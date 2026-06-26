package com.toolbox.app.model

data class ToolInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val iconPath: String,
    val iconColor: String,
    val dirPath: String,
    val installedAt: Long,
    val permissions: List<String> = emptyList()
)
