package com.toolbox.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.toolbox.app.data.ToolManager
import com.toolbox.app.databinding.ActivityMainBinding
import com.toolbox.app.model.ToolInfo
import com.toolbox.app.ui.ToolGridAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolManager: ToolManager
    private lateinit var adapter: ToolGridAdapter

    private val PICK_FILE_REQUEST_CODE = 1001
    private val IMPORT_ACTIVITY_REQUEST_CODE = 1002
    private val EXPORT_REQUEST_CODE = 1003
    private var currentExportingTool: ToolInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        toolManager = ToolManager(this)

        setupRecyclerView()
        loadTools()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                importTool(uri)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ToolGridAdapter(
            onToolClick = { openTool(it) },
            onToolLongClick = { showToolActions(it) }
        )
        binding.toolGrid.layoutManager = GridLayoutManager(this, 3)
        binding.toolGrid.adapter = adapter
    }

    private fun loadTools() {
        val tools = toolManager.listTools()
        adapter.submitList(tools)

        if (tools.isEmpty()) {
            binding.emptyView.visibility = android.view.View.VISIBLE
        } else {
            binding.emptyView.visibility = android.view.View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                startImportPicker()
                true
            }
            R.id.action_online_download -> {
                startActivity(Intent(this, DownloadActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/zip",
                "text/html"
            ))
        }
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                data?.data?.let { uri -> openImportActivity(uri) }
            }
            requestCode == IMPORT_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                loadTools()
            }
            requestCode == EXPORT_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                data?.data?.let { uri ->
                    currentExportingTool?.let { tool ->
                        performExport(tool, uri)
                    }
                }
            }
            requestCode == EDIT_TOOL_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                loadTools()
            }
        }
    }

    private fun openImportActivity(uri: Uri) {
        val intent = Intent(this, ImportActivity::class.java).apply {
            data = uri
        }
        startActivityForResult(intent, IMPORT_ACTIVITY_REQUEST_CODE)
    }

    private fun importTool(uri: Uri) {
        openImportActivity(uri)
    }

    private val EDIT_TOOL_REQUEST_CODE = 1004

    private fun showToolActions(tool: ToolInfo) {
        val items = arrayOf(
            getString(R.string.open),
            getString(R.string.details),
            getString(R.string.export),
            getString(R.string.delete),
            getString(R.string.add_to_desktop),
            "编辑"
        )

        AlertDialog.Builder(this)
            .setTitle(tool.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openTool(tool)
                    1 -> openToolDetail(tool)
                    2 -> exportTool(tool)
                    3 -> confirmDelete(tool)
                    4 -> createShortcut(tool)
                    5 -> openEditTool(tool)
                }
            }
            .show()
    }

    private fun openToolDetail(tool: ToolInfo) {
        val intent = Intent(this, ToolDetailActivity::class.java).apply {
            putExtra("tool_id", tool.id)
        }
        startActivity(intent)
    }

    private fun openEditTool(tool: ToolInfo) {
        val intent = Intent(this, EditToolActivity::class.java).apply {
            putExtra("tool_id", tool.id)
        }
        startActivityForResult(intent, EDIT_TOOL_REQUEST_CODE)
    }

    private fun openTool(tool: ToolInfo) {
        val intent = Intent(this, ToolActivity::class.java).apply {
            putExtra("tool_id", tool.id)
            putExtra("tool_name", tool.name)
        }
        startActivity(intent)
    }

    private fun exportTool(tool: ToolInfo) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "${tool.id}-${tool.version}.zip")
        }
        currentExportingTool = tool
        startActivityForResult(intent, EXPORT_REQUEST_CODE)
    }

    private fun performExport(tool: ToolInfo, outputUri: Uri) {
        Toast.makeText(this, R.string.exporting, Toast.LENGTH_SHORT).show()

        Thread {
            val result = toolManager.exportTool(tool.id, outputUri)
            runOnUiThread {
                result.onSuccess {
                    Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(this, "${getString(R.string.export_failed)}: ${it.message}", Toast.LENGTH_LONG).show()
                }
                currentExportingTool = null
            }
        }.start()
    }

    private fun confirmDelete(tool: ToolInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(String.format(getString(R.string.delete_confirm), tool.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                toolManager.deleteTool(tool.id)
                loadTools()
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createShortcut(tool: ToolInfo) {
        val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val intent = Intent(this, ToolActivity::class.java).apply {
                putExtra("tool_id", tool.id)
                putExtra("tool_name", tool.name)
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // 生成动态图标：首字符 + 圆形彩色背景
            val icon = android.graphics.drawable.Icon.createWithBitmap(createToolIconBitmap(tool))

            val shortcutInfo = android.content.pm.ShortcutInfo.Builder(this, "tool_${tool.id}")
                .setShortLabel(tool.name)
                .setLongLabel(tool.name)
                .setIcon(icon)
                .setIntent(intent)
                .build()

            // 创建快捷方式
            val success = shortcutManager.requestPinShortcut(shortcutInfo, null)
            if (success) {
                Toast.makeText(this, "已创建快捷方式: ${tool.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "创建快捷方式失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "当前启动器不支持快捷方式", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createToolIconBitmap(tool: ToolInfo): android.graphics.Bitmap {
        val size = 192 // 快捷方式图标尺寸
        val cornerRadius = 36f // 圆角，系统会再裁剪
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // 绘制圆角方形背景（留给系统做自适应裁剪）
        val bgColor = try {
            android.graphics.Color.parseColor(tool.iconColor)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#FF5722")
        }

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = android.graphics.Paint.Style.FILL
        }
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // 绘制首字符
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = size * 0.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val firstChar = tool.name.firstOrNull()?.toString() ?: "?"
        val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(firstChar, size / 2f, y, textPaint)

        return bitmap
    }

    override fun onResume() {
        super.onResume()
        loadTools()
    }
}
