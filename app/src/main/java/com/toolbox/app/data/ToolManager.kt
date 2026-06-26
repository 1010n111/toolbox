package com.toolbox.app.data

import android.content.Context
import android.net.Uri
import com.toolbox.app.model.ToolInfo
import com.toolbox.app.util.ZipUtils
import java.io.File

class ToolManager(private val context: Context) {

    private val toolDir = ToolDirectory(context)

    fun listTools(): List<ToolInfo> {
        return toolDir.getAllToolDirs()
            .mapNotNull { dir ->
                val manifest = File(dir, "manifest.json")
                if (manifest.exists()) {
                    ManifestParser.parseFromFile(manifest).getOrNull()
                } else {
                    null
                }
            }
            .sortedByDescending { it.installedAt }
    }

    fun installTool(zipUri: Uri): Result<ToolInfo> {
        return runCatching {
            val tempZip = File(context.cacheDir, "import_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                tempZip.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("无法读取文件")

            val tempDir = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
            ZipUtils.unzip(tempZip, tempDir).getOrThrow()

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                val htmlFiles = tempDir.listFiles { _, name -> name.endsWith(".html") }
                if (htmlFiles?.size == 1) {
                    val htmlFile = htmlFiles[0]
                    val generated = generateManifestFromHtml(htmlFile)
                    manifestFile.writeText(generated)
                } else {
                    throw Exception("找不到 manifest.json 或 index.html")
                }
            }

            val toolInfo = ManifestParser.parseFromFile(manifestFile).getOrThrow()

            if (toolDir.toolExists(toolInfo.id)) {
                throw Exception("工具已存在: ${toolInfo.name}")
            }

            val targetDir = toolDir.getToolDir(toolInfo.id)
            tempDir.renameTo(targetDir)

            tempZip.delete()

            toolInfo.copy(dirPath = targetDir.absolutePath)
        }
    }

    fun installFromTempDir(tempDir: File, toolId: String): ToolInfo {
        android.util.Log.d("ToolManager", "installFromTempDir: ${tempDir.absolutePath}")
        android.util.Log.d("ToolManager", "tempDir exists: ${tempDir.exists()}, isDir: ${tempDir.isDirectory}")
        tempDir.listFiles()?.forEach { f ->
            android.util.Log.d("ToolManager", "  - ${f.name}: ${f.length()} bytes")
        }

        if (toolDir.toolExists(toolId)) {
            throw Exception("工具已存在")
        }

        val manifestFile = File(tempDir, "manifest.json")
        android.util.Log.d("ToolManager", "manifest exists: ${manifestFile.exists()}")
        val toolInfo = ManifestParser.parseFromFile(manifestFile).getOrThrow()
        android.util.Log.d("ToolManager", "toolInfo: id=${toolInfo.id}, name=${toolInfo.name}")

        val targetDir = toolDir.getToolDir(toolId)
        android.util.Log.d("ToolManager", "targetDir: ${targetDir.absolutePath}")
        targetDir.mkdirs()

        // 递归复制（跨目录 renameTo 会静默失败）
        val copyResult = tempDir.copyRecursively(targetDir, overwrite = true)
        android.util.Log.d("ToolManager", "copyResult: $copyResult")
        tempDir.deleteRecursively()

        // 验证复制结果
        android.util.Log.d("ToolManager", "targetDir files:")
        targetDir.listFiles()?.forEach { f ->
            android.util.Log.d("ToolManager", "  - ${f.name}: ${f.length()} bytes")
        }

        return toolInfo.copy(dirPath = targetDir.absolutePath)
    }

    fun analyzeZip(zipUri: Uri): File {
        val tempZip = File(context.cacheDir, "analyze_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            tempZip.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("无法读取文件")

        val tempDir = File(context.cacheDir, "analyze_${System.currentTimeMillis()}")
        ZipUtils.unzip(tempZip, tempDir).getOrThrow()
        tempZip.delete()

        return tempDir
    }

    private fun generateManifestFromHtml(htmlFile: File): String {
        val id = htmlFile.nameWithoutExtension.lowercase().replace("\\W+".toRegex(), "-")
        return """
            {
                "id": "$id",
                "name": "${htmlFile.nameWithoutExtension}",
                "version": "1.0.0",
                "description": "Imported HTML tool",
                "author": "Unknown",
                "icon": ""
            }
        """.trimIndent()
    }

    fun exportTool(toolId: String, outputUri: Uri): Result<Unit> {
        return runCatching {
            val sourceDir = toolDir.getToolDir(toolId)
            if (!sourceDir.exists()) throw Exception("工具不存在")

            val tempZip = File(context.cacheDir, "export_${toolId}.zip")
            ZipUtils.zip(sourceDir, tempZip, excludeDirs = listOf("data/")).getOrThrow()

            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                tempZip.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw Exception("无法写入目标文件")

            tempZip.delete()
        }
    }

    fun deleteTool(toolId: String): Boolean {
        val dir = toolDir.getToolDir(toolId)
        return if (dir.exists()) {
            dir.deleteRecursively()
        } else {
            false
        }
    }

    fun getTool(toolId: String): ToolInfo? {
        val manifest = toolDir.getToolManifest(toolId)
        return if (manifest.exists()) {
            ManifestParser.parseFromFile(manifest).getOrNull()
        } else {
            null
        }
    }

    fun updateToolInfo(
        toolId: String,
        name: String,
        version: String,
        author: String,
        description: String,
        iconColor: String
    ): Boolean {
        val manifestFile = toolDir.getToolManifest(toolId)
        if (!manifestFile.exists()) return false

        return try {
            val json = org.json.JSONObject(manifestFile.readText())
            json.put("name", name)
            json.put("version", version)
            json.put("author", author)
            json.put("description", description)
            json.put("iconColor", iconColor)
            manifestFile.writeText(json.toString(2))
            true
        } catch (e: Exception) {
            false
        }
    }
}
