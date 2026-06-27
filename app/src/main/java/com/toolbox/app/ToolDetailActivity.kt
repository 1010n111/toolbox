package com.toolbox.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.toolbox.app.data.ToolManager
import com.toolbox.app.model.ToolInfo
import com.toolbox.app.util.VersionComparator
import com.toolbox.app.util.ZipUtils
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class ToolDetailActivity : AppCompatActivity() {

    private lateinit var toolManager: ToolManager
    private lateinit var toolId: String
    private var currentTool: ToolInfo? = null

    private lateinit var tvToolIcon: TextView
    private lateinit var tvToolName: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvInstalledAt: TextView
    private lateinit var inputDownloadUrl: TextInputEditText
    private lateinit var btnSaveLink: Button
    private lateinit var btnCheckUpdate: Button
    private lateinit var btnOpen: Button
    private lateinit var btnEdit: Button
    private lateinit var btnOverwriteDownload: Button

    private var isCheckingUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.tool_detail)

        toolId = intent.getStringExtra("tool_id") ?: run {
            Toast.makeText(this, "工具ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        toolManager = ToolManager(this)

        initViews()
        loadToolInfo()
        setupListeners()
    }

    private fun initViews() {
        tvToolIcon = findViewById(R.id.tv_tool_icon)
        tvToolName = findViewById(R.id.tv_tool_name)
        tvVersion = findViewById(R.id.tv_version)
        tvAuthor = findViewById(R.id.tv_author)
        tvDescription = findViewById(R.id.tv_description)
        tvInstalledAt = findViewById(R.id.tv_installed_at)
        inputDownloadUrl = findViewById(R.id.input_download_url)
        btnSaveLink = findViewById(R.id.btn_save_link)
        btnCheckUpdate = findViewById(R.id.btn_check_update)
        btnOverwriteDownload = findViewById(R.id.btn_overwrite_download)
        btnOpen = findViewById(R.id.btn_open)
        btnEdit = findViewById(R.id.btn_edit)
    }

    private fun loadToolInfo() {
        currentTool = toolManager.getTool(toolId)
        val tool = currentTool ?: run {
            Toast.makeText(this, "工具不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置图标背景和文字
        val color = try {
            android.graphics.Color.parseColor(tool.iconColor)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#FF9800")
        }
        (tvToolIcon.background as? android.graphics.drawable.GradientDrawable)?.setColor(color)
        tvToolIcon.text = tool.name.firstOrNull()?.toString() ?: "?"

        tvToolName.text = tool.name
        tvVersion.text = "v${tool.version}"
        tvAuthor.text = "作者: ${tool.author}"
        tvDescription.text = tool.description.ifEmpty { "暂无描述" }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        tvInstalledAt.text = getString(R.string.installed_at, dateFormat.format(Date(tool.installedAt)))

        inputDownloadUrl.setText(tool.downloadUrl)

        // 如果有下载链接，启用检查更新和覆盖下载按钮
        btnCheckUpdate.isEnabled = tool.downloadUrl.isNotEmpty()
        btnOverwriteDownload.isEnabled = tool.downloadUrl.isNotEmpty()
    }

    private fun setupListeners() {
        // 下载链接变化监听
        inputDownloadUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString()?.trim() ?: ""
                val originalUrl = currentTool?.downloadUrl ?: ""
                btnSaveLink.isEnabled = url != originalUrl &&
                    (url.isEmpty() || url.startsWith("http://") || url.startsWith("https://"))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 保存下载链接
        btnSaveLink.setOnClickListener {
            val url = inputDownloadUrl.text?.toString()?.trim() ?: ""
            val success = toolManager.updateDownloadUrl(toolId, url)
            if (success) {
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
                btnSaveLink.isEnabled = false
                btnCheckUpdate.isEnabled = url.isNotEmpty()
                currentTool = currentTool?.copy(downloadUrl = url)
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }

        // 打开工具
        btnOpen.setOnClickListener {
            currentTool?.let { tool ->
                val intent = Intent(this, ToolActivity::class.java).apply {
                    putExtra("tool_id", tool.id)
                    putExtra("tool_name", tool.name)
                }
                startActivity(intent)
            }
        }

        // 编辑工具
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditToolActivity::class.java).apply {
                putExtra("tool_id", toolId)
            }
            startActivity(intent)
        }

        // 检查更新
        btnCheckUpdate.setOnClickListener {
            checkForUpdate()
        }

        // 覆盖下载
        btnOverwriteDownload.setOnClickListener {
            startOverwriteDownload()
        }
    }

    private fun checkForUpdate() {
        val tool = currentTool ?: return
        val url = tool.downloadUrl.trim()
        if (url.isEmpty() || isCheckingUpdate) return

        isCheckingUpdate = true
        btnCheckUpdate.isEnabled = false
        btnCheckUpdate.text = getString(R.string.checking_update)

        Thread {
            try {
                val result = checkUpdateInternal(url, tool.version)
                runOnUiThread {
                    handleUpdateResult(result, tool.version)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "检查更新失败: ${e.message}", Toast.LENGTH_LONG).show()
                    resetCheckUpdateButton()
                }
            }
        }.start()
    }

    private data class UpdateCheckResult(
        val success: Boolean,
        val remoteVersion: String? = null,
        val tempZipFile: File? = null,
        val errorMessage: String? = null
    )

    private fun checkUpdateInternal(urlString: String, localVersion: String): UpdateCheckResult {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        return conn.inputStream.use { input ->
            val tempZip = File(cacheDir, "update_${System.currentTimeMillis()}.zip")
            tempZip.outputStream().use { output ->
                input.copyTo(output)
            }

            // Unzip only manifest.json to temp dir
            val tempDir = File(cacheDir, "update_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // Extract just manifest.json
                val manifestEntry = ZipUtils.findEntry(tempZip, "manifest.json")
                    ?: return@use UpdateCheckResult(
                        success = false,
                        errorMessage = "压缩包中未找到 manifest.json"
                    )

                val manifestContent = ZipUtils.readEntryContent(tempZip, manifestEntry)
                val json = JSONObject(manifestContent)
                val remoteVersion = json.optString("version", "")

                if (remoteVersion.isEmpty()) {
                    return@use UpdateCheckResult(success = false, errorMessage = "无法获取版本信息")
                }

                UpdateCheckResult(
                    success = true,
                    remoteVersion = remoteVersion,
                    tempZipFile = tempZip
                )
            } catch (e: Exception) {
                tempDir.deleteRecursively()
                tempZip.delete()
                UpdateCheckResult(success = false, errorMessage = e.message)
            }
        }
    }

    private fun handleUpdateResult(result: UpdateCheckResult, localVersion: String) {
        resetCheckUpdateButton()

        if (!result.success) {
            Toast.makeText(this, result.errorMessage ?: "检查更新失败", Toast.LENGTH_LONG).show()
            return
        }

        val remoteVersion = result.remoteVersion!!
        val comparison = if (VersionComparator.isValid(localVersion) && VersionComparator.isValid(remoteVersion)) {
            VersionComparator.compare(localVersion, remoteVersion)
        } else {
            // 版本格式无法解析，询问用户是否覆盖
            showUnknownVersionDialog(result.tempZipFile!!)
            return
        }

        when {
            comparison > 0 -> {
                // Remote is newer
                showUpdateDialog(localVersion, remoteVersion, result.tempZipFile!!)
            }
            comparison == 0 -> {
                // Same version
                result.tempZipFile?.delete()
                Toast.makeText(this, getString(R.string.no_update), Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Local is newer (user has dev version)
                result.tempZipFile?.delete()
                Toast.makeText(this, getString(R.string.no_update), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(localVersion: String, remoteVersion: String, tempZip: File) {
        val message = getString(R.string.update_available, remoteVersion, localVersion)
        AlertDialog.Builder(this)
            .setTitle(R.string.update_available)
            .setMessage(message)
            .setPositiveButton(R.string.update_now) { _, _ ->
                startUpdateInstall(tempZip)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                tempZip.delete()
            }
            .show()
    }

    private fun showUnknownVersionDialog(tempZip: File) {
        AlertDialog.Builder(this)
            .setMessage(R.string.version_unknown)
            .setPositiveButton(R.string.overwrite) { _, _ ->
                startUpdateInstall(tempZip)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                tempZip.delete()
            }
            .show()
    }

    private fun startUpdateInstall(tempZip: File) {
        // Start DownloadActivity with the already downloaded file
        val intent = Intent(this, DownloadActivity::class.java).apply {
            putExtra("temp_zip_path", tempZip.absolutePath)
            putExtra("overwrite_tool_id", toolId)
        }
        startActivity(intent)
        finish()
    }

    private fun resetCheckUpdateButton() {
        isCheckingUpdate = false
        btnCheckUpdate.isEnabled = currentTool?.downloadUrl?.isNotEmpty() == true
        btnCheckUpdate.text = getString(R.string.check_update)
    }

    private fun startOverwriteDownload() {
        val url = currentTool?.downloadUrl?.trim() ?: return
        if (url.isEmpty() || isCheckingUpdate) return

        isCheckingUpdate = true
        btnOverwriteDownload.isEnabled = false
        btnOverwriteDownload.text = getString(R.string.downloading)
        btnCheckUpdate.isEnabled = false

        Thread {
            try {
                val file = downloadFileDirect(url)
                runOnUiThread {
                    startImportActivity(file, true)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.download_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                    resetOverwriteButton()
                }
            }
        }.start()
    }

    private fun downloadFileDirect(urlString: String): File {
        // 自动将 Gitee/GitHub 的 blob URL 转换为 raw URL
        var normalizedUrl = urlString
        if (normalizedUrl.contains("gitee.com/") && normalizedUrl.contains("/blob/")) {
            normalizedUrl = normalizedUrl.replace("/blob/", "/raw/")
        }
        if (normalizedUrl.contains("github.com/") && normalizedUrl.contains("/blob/")) {
            normalizedUrl = normalizedUrl.replace("/blob/", "/raw/")
        }

        val url = URL(normalizedUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

        // 手动处理重定向
        val redirect = conn.getHeaderField("Location")
        if (redirect != null && conn.responseCode in 300..399) {
            conn.disconnect()
            return downloadFileDirect(redirect)
        }

        if (conn.responseCode != 200) {
            throw Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}")
        }

        return conn.inputStream.use { input ->
            val contentType = conn.contentType ?: ""
            val isHtmlFile = urlString.endsWith(".html", ignoreCase = true) ||
                           urlString.endsWith(".htm", ignoreCase = true)
            val suffix = if (isHtmlFile) ".html" else ".zip"
            val tempFile = File(cacheDir, "download_${System.currentTimeMillis()}$suffix")

            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }

            // 验证下载内容
            val headerBytes = ByteArray(500)
            val bytesRead = tempFile.inputStream().use { it.read(headerBytes) }

            if (bytesRead > 0) {
                val headerText = String(headerBytes, 0, bytesRead, Charsets.UTF_8)

                // 如果是 .zip 后缀但内容是 HTML，报错
                if (suffix == ".zip" && headerText.trimStart().startsWith('<')) {
                    if (!(headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte())) {
                        tempFile.delete()
                        throw Exception("下载的是 HTML 页面而非工具文件。请使用 raw 链接。")
                    }
                }

                // 如果是 .html 后缀，检查是否是登录页面
                if (suffix == ".html") {
                    val isLoginPage = headerText.contains("meta http-equiv=\"refresh\"", true) ||
                                    headerText.contains("location.href", true) ||
                                    headerText.contains("gitee.com/login", true) ||
                                    headerText.contains("需要登录", true) ||
                                    headerText.contains("github.com/login", true)
                    if (isLoginPage) {
                        tempFile.delete()
                        throw Exception("下载的是登录页面。Gitee raw 链接可能需要登录，请手动下载后导入。")
                    }
                }
            }

            tempFile
        }
    }

    private fun startImportActivity(file: File, isOverwrite: Boolean) {
        val intent = Intent(this, ImportActivity::class.java).apply {
            data = android.net.Uri.fromFile(file)
            if (isOverwrite) {
                putExtra("overwrite_tool_id", toolId)
            }
            // 传递下载URL用于保存到manifest
            currentTool?.downloadUrl?.let { url ->
                if (url.isNotEmpty()) {
                    putExtra("download_url", url)
                }
            }
        }
        startActivity(intent)
        finish()
    }

    private fun resetOverwriteButton() {
        isCheckingUpdate = false
        btnOverwriteDownload.isEnabled = currentTool?.downloadUrl?.isNotEmpty() == true
        btnOverwriteDownload.text = getString(R.string.overwrite_download)
        btnCheckUpdate.isEnabled = currentTool?.downloadUrl?.isNotEmpty() == true
    }

    override fun onResume() {
        super.onResume()
        loadToolInfo()  // 编辑后刷新
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
