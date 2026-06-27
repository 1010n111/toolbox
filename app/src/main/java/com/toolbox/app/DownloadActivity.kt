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

    // For update flow: when we already have the temp zip downloaded
    private val existingTempZipPath: String? by lazy {
        intent.getStringExtra("temp_zip_path")
    }
    private val overwriteToolId: String? by lazy {
        intent.getStringExtra("overwrite_tool_id")
    }
    // For overwrite download flow: pre-filled URL from tool detail
    private val prefilledDownloadUrl: String? by lazy {
        intent.getStringExtra("download_url")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.online_download)

        inputUrl = findViewById(R.id.input_url)
        btnDownload = findViewById(R.id.btn_download)

        // URL 输入变化监听（先设置监听器再设置文本，确保按钮状态正确更新）
        inputUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString()?.trim() ?: ""
                btnDownload.isEnabled = url.startsWith("http://") || url.startsWith("https://")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Pre-fill URL if coming from overwrite download flow
        if (!prefilledDownloadUrl.isNullOrEmpty()) {
            inputUrl.setText(prefilledDownloadUrl)
        }

        // If we came from an update check with existing temp zip, go straight to import
        if (!existingTempZipPath.isNullOrEmpty()) {
            val tempFile = File(existingTempZipPath!!)
            if (tempFile.exists()) {
                startImportActivity(tempFile, !overwriteToolId.isNullOrEmpty())
                return
            }
        }

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
                    startImportActivity(file, !overwriteToolId.isNullOrEmpty())
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.download_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
        }.start()
    }

    private fun String.normalizeGitUrl(): String {
        // 自动将 Gitee/GitHub 的 blob URL 转换为 raw URL
        var url = this
        android.util.Log.d("DownloadActivity", "Original URL: $url")

        if (url.contains("gitee.com/") && url.contains("/blob/")) {
            // Gitee raw 格式: https://gitee.com/user/repo/raw/branch/path/file
            url = url.replace("/blob/", "/raw/")
            android.util.Log.d("DownloadActivity", "✅ Gitee URL auto-converted to: $url")
        }
        if (url.contains("github.com/") && url.contains("/blob/")) {
            url = url.replace("/blob/", "/raw/")
            android.util.Log.d("DownloadActivity", "✅ GitHub URL auto-converted to: $url")
        }
        return url
    }

    private fun String.isHtmlContentType(): Boolean {
        return this.startsWith("text/html") ||
               this.startsWith("application/xhtml")
    }

    private fun downloadFile(urlString: String): File {
        val normalizedUrl = urlString.normalizeGitUrl()
        android.util.Log.d("DownloadActivity", "Connecting to: $normalizedUrl")

        val url = URL(normalizedUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true  // 自动跟随重定向

        // 设置 User-Agent，避免被某些 Git 平台拦截
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

        // 打印响应头便于调试
        android.util.Log.d("DownloadActivity", "Response code: ${conn.responseCode}")
        for (i in 0..conn.headerFields.size) {
            val key = conn.getHeaderFieldKey(i)
            val value = conn.getHeaderField(i)
            if (key != null && (key.contains("ocation", true) || key.contains("ontent", true))) {
                android.util.Log.d("DownloadActivity", "  Header $key: $value")
            }
        }

        // 检查重定向（手动处理更可靠）
        val redirect = conn.getHeaderField("Location")
        if (redirect != null && conn.responseCode in 300..399) {
            conn.disconnect()
            android.util.Log.d("DownloadActivity", "Following redirect to: $redirect")
            return downloadFile(redirect)
        }

        if (conn.responseCode != 200) {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText()?.take(200) ?: "No error body"
            } catch (e: Exception) {
                "N/A"
            }
            throw Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}\n$errorBody")
        }

        return conn.inputStream.use { input ->
            val contentType = conn.contentType ?: ""

            // 检查是否意外下载了 HTML 页面（Git 平台的 blob 页面）
            val isHtmlPage = contentType.isHtmlContentType()
            val isHtmlFile = urlString.endsWith(".html", ignoreCase = true) ||
                           urlString.endsWith(".htm", ignoreCase = true)

            // 只有当 URL 不是 .html 但实际返回 HTML 时，才判断是误下载
            if (isHtmlPage && !isHtmlFile) {
                throw Exception("下载的是 HTML 页面而非工具文件。请使用 'raw' 链接而非 'blob' 链接。")
            }

            val suffix = if (isHtmlFile) ".html" else ".zip"
            val tempFile = File(cacheDir, "download_${System.currentTimeMillis()}$suffix")

            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }

            // 详细日志：验证下载的文件内容
            android.util.Log.d("DownloadActivity", "Downloaded file: ${tempFile.name}, size: ${tempFile.length()} bytes")

            // 读取文件前 500 字节检测内容类型
            val headerBytes = ByteArray(500)
            val bytesRead = tempFile.inputStream().use { it.read(headerBytes) }

            if (bytesRead > 0) {
                val headerText = String(headerBytes, 0, bytesRead, Charsets.UTF_8)
                android.util.Log.d("DownloadActivity", "File header preview: ${headerText.take(200)}")

                // 如果是 .zip 后缀但内容是 HTML，一定是错的
                if (suffix == ".zip" && headerText.trimStart().startsWith('<')) {
                    // 不是 ZIP 文件头 (PK.. = 0x50, 0x4B)
                    if (!(headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte())) {
                        tempFile.delete()
                        throw Exception("下载的是 HTML 页面而非 ZIP 文件。请使用 'raw' 链接而非 'blob' 链接。\n预览内容: ${headerText.take(100)}")
                    }
                }

                // 如果是 .html 后缀，检查是否是 Git 平台的登录/重定向页面
                // 只有出现明确的平台关键词才拦截，普通 HTML 工具直接通过
                if (suffix == ".html") {
                    val isPlatformPage = headerText.contains("meta http-equiv=\"refresh\"", true) ||
                                        headerText.contains("location.href", true) ||
                                        headerText.contains("gitee.com/login", true) ||
                                        headerText.contains("需要登录", true) ||
                                        headerText.contains("统一登录", true) ||
                                        headerText.contains("location.replace", true) ||
                                        headerText.contains("github.com/login", true)

                    if (isPlatformPage) {
                        tempFile.delete()
                        throw Exception("下载的是登录/重定向页面。Gitee 的 raw 文件链接可能需要登录才能访问，请手动下载后导入。\n预览: ${headerText.take(150)}")
                    }
                }
            }

            android.util.Log.d("DownloadActivity", "File validation passed: $suffix")
            tempFile
        }
    }

    private fun startImportActivity(file: File, isUpdateOverwrite: Boolean) {
        android.util.Log.d("DownloadActivity", "========== STARTING IMPORT ==========")
        android.util.Log.d("DownloadActivity", "Passing file: ${file.name}, exists=${file.exists()}, size=${file.length()} bytes")
        android.util.Log.d("DownloadActivity", "File path: ${file.absolutePath}")

        val intent = Intent(this, ImportActivity::class.java).apply {
            data = Uri.fromFile(file)
            if (isUpdateOverwrite) {
                putExtra("overwrite_tool_id", overwriteToolId)
            }
            // 传递下载URL用于保存到manifest
            inputUrl.text?.toString()?.trim()?.let { url ->
                if (url.isNotEmpty()) {
                    val normalized = url.normalizeGitUrl()
                    putExtra("download_url", normalized)
                    android.util.Log.d("DownloadActivity", "Download URL: $normalized")
                }
            }
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
