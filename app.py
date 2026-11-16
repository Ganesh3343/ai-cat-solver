import base64
import json
import os
import re
from collections import Counter
from typing import List, Dict, Any

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
import urllib.request
import urllib.error
app = FastAPI(title="AI CAT SOLVER")
app.mount("/static", StaticFiles(directory="static"), name="static")

PROMPT = """
You are reading a screenshot of a grid puzzle called a cat placement puzzle.
Return ONLY valid JSON in this exact shape:
{"grid":[["color_name", ...], ...],"notes":"short note"}

Read ONLY the playable square/rectangular tile grid. Do not include headers,
icons, hints, ads, buttons, or tiles outside the board.
Each grid entry must be the BACKGROUND TILE COLOR, not the symbol printed on it.
Use short consistent lowercase color names (examples: teal, pink, purple,
brown, orange, blue, green, yellow, red, light_blue, grey).
Rows go top-to-bottom; columns go left-to-right.
Every tile has exactly one background color. Include every board cell.
If the board is unclear, make your best careful reading and mention uncertainty
in notes. Do not solve the puzzle; only transcribe the tile background colors.
"""

def extract_json(text: str) -> Dict[str, Any]:
    text = text.strip()
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text, flags=re.I)
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end < start:
        raise ValueError("AI did not return JSON.")
    return json.loads(text[start:end+1])

def solve_grid(grid: List[List[str]]) -> List[Dict[str, int]]:
    if not grid or not grid[0]:
        raise ValueError("The board grid is empty.")
    rows, cols = len(grid), len(grid[0])
    if rows != cols:
        raise ValueError(f"This version expects a square board; detected {rows} rows × {cols} columns.")
    if any(len(row) != cols for row in grid):
        raise ValueError("The detected board has uneven row lengths.")
    colors = sorted({str(v).strip().lower() for row in grid for v in row})
    if len(colors) != rows:
        raise ValueError(
            f"Detected {len(colors)} tile colours on a {rows}×{cols} board. "
            "The puzzle needs one cat per colour and one per row/column. "
            "Please upload a clearer, full-board screenshot."
        )

    candidates = {
        color: [(r, c) for r in range(rows) for c in range(cols)
                if str(grid[r][c]).strip().lower() == color]
        for color in colors
    }
    # Most constrained colors first.
    order = sorted(colors, key=lambda color: len(candidates[color]))
    placed = []
    used_rows, used_cols = set(), set()

    def search(i):
        if i == len(order):
            return True
        color = order[i]
        for r, c in candidates[color]:
            if r in used_rows or c in used_cols:
                continue
            if any(abs(r-pr) <= 1 and abs(c-pc) <= 1 for pr, pc in placed):
                continue
            used_rows.add(r); used_cols.add(c); placed.append((r, c))
            if search(i + 1):
                return True
            placed.pop(); used_rows.remove(r); used_cols.remove(c)
        return False

    if not search(0):
        raise ValueError("No valid arrangement found. The image may have been read incorrectly.")
    return [{"color": color, "row": next(r for r,c in placed if grid[r][c].strip().lower() == color) + 1,
             "column": next(c for r,c in placed if grid[r][c].strip().lower() == color) + 1}
            for color in colors]

@app.get("/")
def home():
    return FileResponse("static/index.html")

@app.post("/solve")
async def solve(file: UploadFile = File(...)):
    if file.content_type not in ("image/png", "image/jpeg", "image/webp"):
        raise HTTPException(400, "Please upload a PNG, JPG, or WEBP screenshot.")
    data = await file.read()
    if len(data) > 12 * 1024 * 1024:
        raise HTTPException(413, "Image is too large. Maximum size is 12 MB.")
    api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise HTTPException(503, "Add GEMINI_API_KEY to the server environment.")

mime = file.content_type
encoded = base64.b64encode(data).decode("utf-8")

try:
    url = (
        "https://generativelanguage.googleapis.com/v1beta/"
        "models/gemini-3.8-flash:generateContent"
        f"?key={api_key}"
    )

    payload = {
        "contents": [{
            "parts": [
                {"text": PROMPT},
                {
                    "inline_data": {
                        "mime_type": mime,
                        "data": encoded
                    }
                }
            ]
        }],
        "generationConfig": {
            "responseMimeType": "application/json"
        }
    }

    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    with urllib.request.urlopen(request, timeout=60) as response:
        result = json.loads(response.read().decode("utf-8"))

    ai_text = result["candidates"][0]["content"]["parts"][0]["text"]
    parsed = extract_json(ai_text)
        grid = parsed.get("grid")
        if not isinstance(grid, list) or not grid:
            raise ValueError("The AI could not identify the board grid.")
        solution = solve_grid(grid)
        return {"grid": grid, "solution": solution, "notes": parsed.get("notes", ""),
                "rows": len(grid), "columns": len(grid[0])}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(422, str(e))
