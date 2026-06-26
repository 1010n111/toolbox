package com.toolbox.app

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.toolbox.app.data.ToolManager
import com.toolbox.app.model.ToolInfo

class EditToolActivity : AppCompatActivity() {

    private lateinit var toolManager: ToolManager
    private lateinit var tool: ToolInfo
    private var selectedColor: String = "#FF9800"
    private var selectedColorView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_tool)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "编辑工具"

        toolManager = ToolManager(this)

        val toolId = intent.getStringExtra("tool_id") ?: run {
            Toast.makeText(this, "工具不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tool = toolManager.getTool(toolId) ?: run {
            Toast.makeText(this, "工具不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupForm()
        setupColorPickers()
        setupSaveButton()
    }

    private fun setupForm() {
        findViewById<TextInputEditText>(R.id.input_tool_id).setText(tool.id)
        findViewById<TextInputEditText>(R.id.input_tool_name).setText(tool.name)
        findViewById<TextInputEditText>(R.id.input_tool_version).setText(tool.version)
        findViewById<TextInputEditText>(R.id.input_tool_author).setText(tool.author)
        findViewById<TextInputEditText>(R.id.input_tool_desc).setText(tool.description)
        selectedColor = tool.iconColor
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
            val background = view.background?.mutate() as? android.graphics.drawable.GradientDrawable
            background?.setColor(android.graphics.Color.parseColor(color))

            // 高亮选中的颜色
            if (color == selectedColor) {
                selectedColorView = view
                background?.setStroke(3.dpToPx(), android.graphics.Color.parseColor("#2196F3"))
            }

            view.setOnClickListener {
                // 清除之前选中的
                selectedColorView?.let {
                    val bg = it.background?.mutate() as? android.graphics.drawable.GradientDrawable
                    bg?.setStroke(0, android.graphics.Color.TRANSPARENT)
                }

                // 设置新选中的
                selectedColor = color
                selectedColorView = view
                val bg = view.background?.mutate() as? android.graphics.drawable.GradientDrawable
                bg?.setStroke(3.dpToPx(), android.graphics.Color.parseColor("#2196F3"))
            }
        }
    }

    private fun setupSaveButton() {
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.input_tool_name).text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入工具名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val version = findViewById<TextInputEditText>(R.id.input_tool_version).text.toString().trim().ifEmpty { "1.0.0" }
            val author = findViewById<TextInputEditText>(R.id.input_tool_author).text.toString().trim()
            val desc = findViewById<TextInputEditText>(R.id.input_tool_desc).text.toString().trim()

            val success = toolManager.updateToolInfo(
                toolId = tool.id,
                name = name,
                version = version,
                author = author,
                description = desc,
                iconColor = selectedColor
            )

            if (success) {
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
