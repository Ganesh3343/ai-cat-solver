package com.aicatsolver.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var resultBox: LinearLayout
    private lateinit var solveButton: Button
    private var selectedUri: Uri? = null
    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            status.text = "Screenshot selected. Tap FIND ALL CATS."
            resultBox.removeAllViews()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(0xFF17142B.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "🐱\\nAI CAT SOLVER"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 18, 0, 8)
        }
        root.addView(title, matchWrap())
        val subtitle = TextView(this).apply {
            text = "Upload your cat puzzle screenshot and find every cat."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFFC4BFD9.toInt())
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle, matchWrap())

        val choose = Button(this).apply {
            text = "📸  CHOOSE PUZZLE IMAGE"
            setOnClickListener { picker.launch("image/*") }
        }
        root.addView(choose, matchWrap())

        solveButton = Button(this).apply {
            text = "✨  FIND ALL CATS"
            setOnClickListener { uploadAndSolve() }
        }
        val solveParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        solveParams.topMargin = 12
        root.addView(solveButton, solveParams)

        status = TextView(this).apply {
            text = "Choose a clear screenshot showing the complete board."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFFC4BFD9.toInt())
            setPadding(0, 18, 0, 18)
        }
        root.addView(status, matchWrap())

        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(resultBox, matchWrap())

        val footer = TextView(this).apply {
            text = "Cloud connection required. Your API key stays on the server."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFFAAA3C4.toInt())
            setPadding(0, 28, 0, 8)
        }
        root.addView(footer, matchWrap())
    }

    private fun uploadAndSolve() {
        val uri = selectedUri
        if (uri == null) {
            status.text = "Please choose a puzzle screenshot first."
            return
        }
        val base = BuildConfig.API_BASE_URL
        if (base.contains("YOUR-CLOUD-SERVICE")) {
            status.text = "Cloud server is not connected yet. Deploy the backend and set API_BASE_URL in app/build.gradle.kts."
            return
        }
        solveButton.isEnabled = false
        status.text = "Uploading image and analysing the puzzle…"
        Thread {
            try {
                val input = contentResolver.openInputStream(uri) ?: error("Cannot open selected image.")
                val temp = File(cacheDir, "puzzle_upload.jpg")
                input.use { source -> FileOutputStream(temp).use { target -> source.copyTo(target) } }
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", "puzzle.jpg", temp.asRequestBody("image/jpeg".toMediaType()))
                    .build()
                val request = Request.Builder().url(base.trimEnd('/') + "/solve").post(body).build()
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = try { JSONObject(text).optString("detail", text) } catch (_: Exception) { text }
                        error(message.ifBlank { "Server error ${response.code}" })
                    }
                    val json = JSONObject(text)
                    val solution = json.getJSONArray("solution")
                    runOnUiThread {
                        resultBox.removeAllViews()
                        val heading = TextView(this).apply {
                            text = "🎯  CAT POSITIONS"
                            textSize = 20f
                            setTextColor(0xFFFFFFFF.toInt())
                            setPadding(0, 18, 0, 12)
                        }
                        resultBox.addView(heading)
                        for (i in 0 until solution.length()) {
                            val item = solution.getJSONObject(i)
                            val line = TextView(this).apply {
                                text = "🐱  ${item.getString("color").replaceFirstChar { it.uppercase() }} — Row ${item.getInt("row")}, Column ${item.getInt("column")}"
                                textSize = 16f
                                setTextColor(0xFF71E2DC.toInt())
                                setPadding(14, 14, 14, 14)
                                setBackgroundColor(0xFF292544.toInt())
                            }
                            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            params.bottomMargin = 8
                            resultBox.addView(line, params)
                        }
                        status.text = "Done! Rows are counted top-to-bottom; columns left-to-right."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not solve: ${e.message}" }
            } finally {
                runOnUiThread { solveButton.isEnabled = true }
            }
        }.start()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
