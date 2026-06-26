package com.toolbox.app.data

import android.content.Context
import java.io.File

class ToolDirectory(private val context: Context) {

    private val toolsRoot: File
        get() = File(context.filesDir, "tools").apply { mkdirs() }

    fun getToolDir(toolId: String): File = File(toolsRoot, toolId)

    fun getToolIndexHtml(toolId: String): File = File(getToolDir(toolId), "index.html")

    fun getToolManifest(toolId: String): File = File(getToolDir(toolId), "manifest.json")

    fun getToolDataDir(toolId: String): File = File(getToolDir(toolId), "data").apply { mkdirs() }

    fun getAllToolDirs(): List<File> = toolsRoot.listFiles()?.filter { it.isDirectory() } ?: emptyList()

    fun toolExists(toolId: String): Boolean = getToolDir(toolId).exists()
}
