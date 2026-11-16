# AI CAT SOLVER

A mobile-friendly cloud web app prototype. Open it in Chrome on Android and use **Add to Home screen** to make it feel like a lightweight app.

## What it does
1. Uploads a screenshot of the full cat puzzle board.
2. Sends the image to a vision-capable OpenAI model from the server.
3. Reads the background colour of each tile.
4. Finds one position per colour, row and column, with no cats touching (including diagonally).
5. Shows row/column answers and highlights the selected cells.

## Important
This is a working starter prototype, not a guaranteed solver for every game variation. Image recognition can misread similar colours, overlays, or cropped boards. Check the detected board if an answer looks wrong. This version assumes a square board with the same number of distinct tile colours as rows/columns and the rules shown above.

## Run locally
Requires Python 3.11+.

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
# Android/ macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
```

Set your API key on the server (never put it in the HTML or share it):
- Windows PowerShell: `$env:OPENAI_API_KEY="your_key_here"`
- macOS/Linux: `export OPENAI_API_KEY="your_key_here"`

Then run:
```bash
uvicorn app:app --reload
```
Open `http://127.0.0.1:8000`.

## Deploy as a cloud app
1. Create a private GitHub repository and upload these project files.
2. Create a Web Service on Render and connect the repository. The included `render.yaml` and `Dockerfile` configure it.
3. In the service's Environment settings, add `OPENAI_API_KEY` with your own OpenAI API key. Keep it secret.
4. Deploy. Open the Render URL on your Android phone.
5. In Chrome, menu → **Add to Home screen**.

OpenAI API usage is billed separately according to your account/model pricing. The app does not include an API key or free AI credits.

## Files
- `app.py` — cloud API, image analysis and puzzle solver
- `static/index.html` — mobile app interface
- `requirements.txt` — Python dependencies
- `Dockerfile`, `render.yaml` — deployment configuration
