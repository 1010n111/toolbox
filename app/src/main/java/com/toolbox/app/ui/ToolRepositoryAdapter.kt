package com.toolbox.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toolbox.app.R
import com.toolbox.app.data.DownloadHistory
import com.toolbox.app.data.ToolManager
import com.toolbox.app.model.RepositoryTool

class ToolRepositoryAdapter(
    private val onInstallClick: (RepositoryTool) -> Unit,
    private val toolManager: ToolManager,
    private val downloadHistory: DownloadHistory
) : ListAdapter<RepositoryTool, ToolRepositoryAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tv_icon)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvVersion: TextView = view.findViewById(R.id.tv_version)
        val tvDescription: TextView = view.findViewById(R.id.tv_description)
        val tvAuthor: TextView = view.findViewById(R.id.tv_author)
        val btnInstall: Button = view.findViewById(R.id.btn_install)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repository_tool, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tool = getItem(position)
        holder.tvIcon.text = tool.name.firstOrNull()?.toString() ?: "?"
        holder.tvName.text = tool.name
        holder.tvVersion.text = "v${tool.version}"
        holder.tvDescription.text = tool.description
        holder.tvAuthor.text = "作者: ${tool.author}"

        // 根据下载URL检查是否已安装（从downloads.json判断）
        val isInstalled = downloadHistory.isDownloaded(tool.downloadUrl)
        if (isInstalled) {
            // 已安装，显示重新安装（覆盖）
            holder.btnInstall.text = holder.itemView.context.getString(R.string.reinstall)
            holder.btnInstall.isEnabled = true
            holder.btnInstall.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt()) // 蓝色
        } else {
            // 未安装
            holder.btnInstall.text = holder.itemView.context.getString(R.string.install)
            holder.btnInstall.isEnabled = true
            holder.btnInstall.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt()) // 绿色
        }
        holder.btnInstall.setOnClickListener { onInstallClick(tool) }
    }

    class DiffCallback : DiffUtil.ItemCallback<RepositoryTool>() {
        override fun areItemsTheSame(oldItem: RepositoryTool, newItem: RepositoryTool): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RepositoryTool, newItem: RepositoryTool): Boolean {
            return oldItem == newItem
        }
    }
}
