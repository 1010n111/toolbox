package com.toolbox.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File

data class DownloadRecord(
    val url: String,
    val toolId: String,
    val toolName: String,
    val downloadTime: Long,
    val version: String
)

class DownloadHistory(private val context: Context) {

    private val historyFile = File(context.filesDir, "downloads.json")
    private val json = loadHistory()

    private fun loadHistory(): JSONObject {
        return if (historyFile.exists()) {
            try {
                JSONObject(historyFile.readText())
            } catch (e: Exception) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
    }

    private fun saveHistory() {
        historyFile.writeText(json.toString(2))
    }

    /**
     * 从URL提取相对路径作为key
     * 例如: https://github.com/user/repo/blob/main/cal/cal.zip → /cal/cal.zip
     *       https://raw.githubusercontent.com/user/repo/master/cal/cal.zip → /cal/cal.zip
     */
    private fun extractRelativePath(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val path = uri.path
            // 移除 /user/repo/blob/main 或 /user/repo/master 等前缀
            // 匹配 GitHub/Gitee/GitLab 的路径模式
            val patterns = listOf(
                "/blob/main/", "/blob/master/",
                "/raw/main/", "/raw/master/",
                "/-/raw/main/", "/-/raw/master/",
                "@main/", "@master/"
            )
            for (pattern in patterns) {
                val index = path.indexOf(pattern)
                if (index >= 0) {
                    return path.substring(index + pattern.length - 1) // 保留开头的 /
                }
            }
            // 如果没有匹配到模式，返回完整路径
            path
        } catch (e: Exception) {
            url // 解析失败则返回原始URL
        }
    }

    /**
     * 添加下载记录
     */
    fun addRecord(url: String, toolId: String, toolName: String, version: String) {
        val key = extractRelativePath(url)
        val record = JSONObject()
        record.put("url", url)
        record.put("toolId", toolId)
        record.put("toolName", toolName)
        record.put("downloadTime", System.currentTimeMillis())
        record.put("version", version)
        record.put("relativePath", key) // 额外保存相对路径
        json.put(key, record)
        saveHistory()
    }

    /**
     * 根据URL获取下载记录
     */
    fun getRecord(url: String): DownloadRecord? {
        val key = extractRelativePath(url)
        return if (json.has(key)) {
            val obj = json.getJSONObject(key)
            DownloadRecord(
                url = obj.getString("url"),
                toolId = obj.getString("toolId"),
                toolName = obj.getString("toolName"),
                downloadTime = obj.getLong("downloadTime"),
                version = obj.optString("version", "1.0.0")
            )
        } else {
            null
        }
    }

    /**
     * 检查URL是否已下载安装
     */
    fun isDownloaded(url: String): Boolean {
        val key = extractRelativePath(url)
        return json.has(key)
    }

    /**
     * 根据toolId查找下载记录
     */
    fun findByToolId(toolId: String): DownloadRecord? {
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = json.getJSONObject(key)
            if (obj.optString("toolId") == toolId) {
                return DownloadRecord(
                    url = obj.getString("url"),
                    toolId = obj.getString("toolId"),
                    toolName = obj.getString("toolName"),
                    downloadTime = obj.getLong("downloadTime"),
                    version = obj.optString("version", "1.0.0")
                )
            }
        }
        return null
    }

    /**
     * 删除下载记录（根据URL）
     */
    fun removeByUrl(url: String) {
        json.remove(url)
        saveHistory()
    }

    /**
     * 删除下载记录（根据toolId）
     */
    fun removeByToolId(toolId: String) {
        val keys = json.keys()
        val keysToRemove = mutableListOf<String>()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = json.getJSONObject(key)
            if (obj.optString("toolId") == toolId) {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach {
            json.remove(it)
        }
        saveHistory()
    }

    /**
     * 获取所有下载记录
     */
    fun getAllRecords(): List<DownloadRecord> {
        val records = mutableListOf<DownloadRecord>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = json.getJSONObject(key)
            records.add(
                DownloadRecord(
                    url = obj.getString("url"),
                    toolId = obj.getString("toolId"),
                    toolName = obj.getString("toolName"),
                    downloadTime = obj.getLong("downloadTime"),
                    version = obj.optString("version", "1.0.0")
                )
            )
        }
        return records
    }
}
