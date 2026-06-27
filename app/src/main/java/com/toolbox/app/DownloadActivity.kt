package com.toolbox.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DownloadActivity : AppCompatActivity() {

    private lateinit var inputUrl: TextInputEditText
    private lateinit var btnDownload: Button
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.online_download)

        inputUrl = findViewById(R.id.input_url)
        btnDownload = findViewById(R.id.btn_download)

        // URL 输入变化监听
        inputUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString()?.trim() ?: ""
                btnDownload.isEnabled = url.startsWith("http://") || url.startsWith("https://")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnDownload.setOnClickListener {
            startDownload()
        }
    }

    private fun startDownload() {
        val url = inputUrl.text?.toString()?.trim() ?: return
        if (isDownloading) return

        isDownloading = true
        btnDownload.isEnabled = false
        btnDownload.text = getString(R.string.downloading)

        Thread {
            try {
                val file = downloadFile(url)
                runOnUiThread {
                    startImportActivity(file)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.download_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
        }.start()
    }

    private fun downloadFile(urlString: String): File {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        return conn.inputStream.use { input ->
            val contentType = conn.contentType
            val isHtml = urlString.endsWith(".html", ignoreCase = true) ||
                          urlString.endsWith(".htm", ignoreCase = true) ||
                          contentType == "text/html"

            val suffix = if (isHtml) ".html" else ".zip"
            val tempFile = File(cacheDir, "download_${System.currentTimeMillis()}$suffix")

            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
            tempFile
        }
    }

    private fun startImportActivity(file: File) {
        val intent = Intent(this, ImportActivity::class.java).apply {
            data = Uri.fromFile(file)
        }
        startActivity(intent)
        finish()
    }

    private fun resetButton() {
        isDownloading = false
        btnDownload.isEnabled = true
        btnDownload.text = getString(R.string.download_tool)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
