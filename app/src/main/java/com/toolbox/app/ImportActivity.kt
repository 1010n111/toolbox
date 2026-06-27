package com.toolbox.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.toolbox.app.data.ToolManager
import com.toolbox.app.util.ZipUtils
import java.io.File

class ImportActivity : AppCompatActivity() {

    private lateinit var toolManager: ToolManager
    private var zipUri: Uri? = null
    private var tempDir: File? = null
    private var hasManifest = false
    private var selectedColor: String = "#FF5722"
    private var selectedColorView: View? = null
    private val overwriteToolId: String? by lazy {
        intent.getStringExtra("overwrite_tool_id")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (overwriteToolId != null) "更新工具" else "导入工具"

        toolManager = ToolManager(this)

        setupColorPickers()

        zipUri = intent.data
        if (zipUri == null) {
            Toast.makeText(this, "无效的文件", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupForm()
        setupImportButton()

        // 异步解压分析ZIP
        Thread {
            try {
                analyzeZipFile()
                // 回到主线程更新UI
                runOnUiThread {
                    // 自动填充后启用按钮
                    val name = findViewById<TextInputEditText>(R.id.input_tool_name).text
                    findViewById<Button>(R.id.btn_import).isEnabled = !name.isNullOrBlank()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "文件解析失败: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }

    private fun analyzeZipFile() {
        val uri = zipUri ?: return

        // 从ContentResolver获取真实文件名
        var fileName = "import"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex) ?: "import"
            }
        }

        android.util.Log.d("ImportActivity", "Importing: $fileName")

        // 判断是单HTML文件还是ZIP包
        if (fileName.endsWith(".html", ignoreCase = true) ||
            fileName.endsWith(".htm", ignoreCase = true)) {
            // 单HTML文件：直接复制到临时目录
            tempDir = File(cacheDir, "html_${System.currentTimeMillis()}").apply { mkdir() }
            val htmlFile = File(tempDir, "index.html")

            contentResolver.openInputStream(uri)?.use { input ->
                htmlFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d("ImportActivity", "HTML copied: ${htmlFile.length()} bytes to ${htmlFile.absolutePath}")

            hasManifest = false
            val fileSize = htmlFile.length()

            runOnUiThread {
                val fileInfo = findViewById<TextView>(R.id.file_info)
                val manifestStatus = findViewById<TextView>(R.id.manifest_status)

                fileInfo.text = "$fileName · 1 个文件 · ${formatSize(fileSize)}"
                manifestStatus.text = "✓ 单HTML工具"
                manifestStatus.setTextColor(0xFF388E3C.toInt())
                autoFillFromFileName()
            }
        } else {
            // ZIP包：正常解压
            val tempZip = File(cacheDir, "analyze_${System.currentTimeMillis()}.zip")

            contentResolver.openInputStream(uri)?.use { input ->
                tempZip.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            tempDir = File(cacheDir, "analyze_${System.currentTimeMillis()}")
            ZipUtils.unzip(tempZip, tempDir!!).getOrThrow()
            tempZip.delete()

            val manifestFile = File(tempDir!!, "manifest.json")
            hasManifest = manifestFile.exists()

            val fileSize = tempDir!!.walkTopDown().sumOf { it.length() }
            val fileCount = tempDir!!.walkTopDown().count { it.isFile }
            val fileInfoText = "${tempDir!!.name} · $fileCount 个文件 · ${formatSize(fileSize)}"

            runOnUiThread {
                val fileInfo = findViewById<TextView>(R.id.file_info)
                val manifestStatus = findViewById<TextView>(R.id.manifest_status)

                fileInfo.text = fileInfoText

                if (hasManifest) {
                    manifestStatus.text = "✓ 包含 manifest.json，将自动读取信息"
                    manifestStatus.setTextColor(0xFF388E3C.toInt())
                    autoFillFromManifest(manifestFile)
                } else {
                    manifestStatus.text = "⚠ 未找到 manifest.json，请手动填写信息"
                    manifestStatus.setTextColor(0xFFFFA000.toInt())
                    autoFillFromFileName()
                }
            }
        }
    }

    private fun autoFillFromManifest(manifestFile: File) {
        val json = manifestFile.readText()
        try {
            val obj = org.json.JSONObject(json)
            // For overwrite/update: use the existing tool ID, don't generate a new one
            val id = overwriteToolId ?: run {
                // 用短UUID确保唯一，不读取manifest里的id
                java.util.UUID.randomUUID().toString().substring(0, 8)
            }
            val name = obj.optString("name", "")
            val version = obj.optString("version", "1.0.0")
            val author = obj.optString("author", "")
            val desc = obj.optString("description", "")
            val iconColor = obj.optString("iconColor", "#FF5722")

            runOnUiThread {
                findViewById<TextInputEditText>(R.id.input_tool_id).setText(id)
                findViewById<TextInputEditText>(R.id.input_tool_name).setText(name)
                findViewById<TextInputEditText>(R.id.input_tool_version).setText(version)
                findViewById<TextInputEditText>(R.id.input_tool_author).setText(author)
                findViewById<TextInputEditText>(R.id.input_tool_desc).setText(desc)
                // Try to use the existing icon color from manifest
                try {
                    selectedColor = iconColor
                    // We can't easily update the selection highlight here without refactoring, but it will be written correctly
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            autoFillFromFileName()
        }
    }

    private fun autoFillFromFileName() {
        val htmlFiles = tempDir?.listFiles { _, name -> name.endsWith(".html") }
        var name = ""

        if (!htmlFiles.isNullOrEmpty()) {
            name = htmlFiles[0].nameWithoutExtension
        }

        // 短UUID：8位，工具唯一标识
        val shortUuid = java.util.UUID.randomUUID().toString().substring(0, 8)

        // 确保在主线程更新UI
        runOnUiThread {
            findViewById<TextInputEditText>(R.id.input_tool_id).setText(shortUuid)
            findViewById<TextInputEditText>(R.id.input_tool_name).setText(name)
            findViewById<TextInputEditText>(R.id.input_tool_version).setText("1.0.0")
        }
    }

    private fun setupForm() {
        val nameInput = findViewById<TextInputEditText>(R.id.input_tool_name)
        nameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                findViewById<Button>(R.id.btn_import).isEnabled = !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupImportButton() {
        findViewById<Button>(R.id.btn_import).setOnClickListener {
            performImport()
        }
    }

    private fun performImport() {
        val btn = findViewById<Button>(R.id.btn_import)
        btn.isEnabled = false
        btn.text = "正在导入..."

        Thread {
            try {
                val id = findViewById<TextInputEditText>(R.id.input_tool_id).text.toString().trim()
                    .ifEmpty { findViewById<TextInputEditText>(R.id.input_tool_name).text.toString()
                        .lowercase().replace("\\W+".toRegex(), "-") }
                val name = findViewById<TextInputEditText>(R.id.input_tool_name).text.toString().trim()
                val version = findViewById<TextInputEditText>(R.id.input_tool_version).text.toString().trim().ifEmpty { "1.0.0" }
                val author = findViewById<TextInputEditText>(R.id.input_tool_author).text.toString().trim()
                val desc = findViewById<TextInputEditText>(R.id.input_tool_desc).text.toString().trim()

                // 生成 manifest.json
                val manifest = """
                    {
                        "id": "$id",
                        "name": "$name",
                        "version": "$version",
                        "description": "$desc",
                        "author": "$author",
                        "icon": "",
                        "iconColor": "$selectedColor"
                    }
                """.trimIndent()

                File(tempDir!!, "manifest.json").writeText(manifest)

                // 确保有 index.html
                val htmlFiles = tempDir!!.listFiles { _, name -> name.endsWith(".html") }
                if (!htmlFiles.isNullOrEmpty()) {
                    if (!File(tempDir!!, "index.html").exists()) {
                        htmlFiles[0].renameTo(File(tempDir!!, "index.html"))
                    }
                }

                // 移动到工具目录
                val overwrite = !overwriteToolId.isNullOrEmpty()
                ToolManager(this).installFromTempDir(tempDir!!, id, overwrite)

                runOnUiThread {
                    val message = if (overwrite) "更新成功: $name" else "导入成功: $name"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                    btn.isEnabled = true
                    btn.text = "导入"
                }
            }
        }.start()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / 1024.0 / 1024.0)} MB"
        }
    }

    private fun setupColorPickers() {
        val colorViews = listOf(
            findViewById<View>(R.id.color_red),
            findViewById<View>(R.id.color_orange),
            findViewById<View>(R.id.color_yellow),
            findViewById<View>(R.id.color_green),
            findViewById<View>(R.id.color_blue),
            findViewById<View>(R.id.color_indigo),
            findViewById<View>(R.id.color_purple)
        )

        colorViews.forEach { view ->
            val color = view.tag as? String ?: return@forEach

            // 设置颜色
            val background = view.background as? android.graphics.drawable.GradientDrawable
            background?.setColor(android.graphics.Color.parseColor(color))

            view.setOnClickListener {
                // 清除之前选中的
                selectedColorView?.let {
                    val bg = it.background as? android.graphics.drawable.GradientDrawable
                    bg?.setStroke(0, android.graphics.Color.TRANSPARENT)
                }

                // 设置新选中的
                selectedColor = color
                selectedColorView = view
                val bg = view.background as? android.graphics.drawable.GradientDrawable
                bg?.setStroke(3, android.graphics.Color.parseColor("#2196F3"))
            }
        }

        // 默认选中第一个（橙色）
        selectedColor = "#FF9800"
        selectedColorView = colorViews[1]
        val bg = colorViews[1].background as? android.graphics.drawable.GradientDrawable
        bg?.setStroke(3, android.graphics.Color.parseColor("#2196F3"))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        tempDir?.deleteRecursively()
    }
}
