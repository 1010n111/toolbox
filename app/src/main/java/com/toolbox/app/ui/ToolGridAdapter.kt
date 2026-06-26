package com.toolbox.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolbox.app.R
import com.toolbox.app.model.ToolInfo

class ToolGridAdapter(
    private val onToolClick: (ToolInfo) -> Unit,
    private val onToolLongClick: (ToolInfo) -> Unit
) : RecyclerView.Adapter<ToolGridAdapter.ViewHolder>() {

    private var tools: List<ToolInfo> = emptyList()

    fun submitList(newTools: List<ToolInfo>) {
        tools = newTools
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tools[position])
    }

    override fun getItemCount(): Int = tools.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tool_name)
        private val iconText: TextView = itemView.findViewById(R.id.tool_icon_text)
        private val iconContainer: FrameLayout = itemView.findViewById(R.id.icon_container)

        fun bind(tool: ToolInfo) {
            name.text = tool.name

            // 设置首字母
            iconText.text = tool.name.firstOrNull()?.toString() ?: "?"

            // 设置背景颜色
            try {
                val color = Color.parseColor(tool.iconColor)
                val background = iconContainer.background?.mutate() as? GradientDrawable
                background?.setColor(color)
            } catch (e: Exception) {
                val background = iconContainer.background?.mutate() as? GradientDrawable
                background?.setColor(Color.parseColor("#FF5722"))
            }

            itemView.setOnClickListener { onToolClick(tool) }
            itemView.setOnLongClickListener {
                onToolLongClick(tool)
                true
            }
        }
    }
}
