package com.toolbox.app.model

data class RepositoryTool(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val downloadUrl: String,
    val dirName: String
)
