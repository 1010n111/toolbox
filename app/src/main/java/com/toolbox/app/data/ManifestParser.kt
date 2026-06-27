package com.toolbox.app.data

import com.toolbox.app.model.ToolInfo
import org.json.JSONObject
import java.io.File

object ManifestParser {

    fun parse(jsonString: String, dirPath: String): Result<ToolInfo> {
        return runCatching {
            val json = JSONObject(jsonString)

            val id = json.getString("id")
            val name = json.getString("name")
            val version = json.optString("version", "1.0.0")
            val description = json.optString("description", "")
            val author = json.optString("author", "Unknown")
            val icon = json.optString("icon", "")
            val iconColor = json.optString("iconColor", "#FF5722")
            val downloadUrl = json.optString("downloadUrl", "")

            val permissions = mutableListOf<String>()
            val permArray = json.optJSONArray("permissions")
            if (permArray != null) {
                for (i in 0 until permArray.length()) {
                    permissions.add(permArray.getString(i))
                }
            }

            ToolInfo(
                id = id,
                name = name,
                version = version,
                description = description,
                author = author,
                iconPath = icon,
                iconColor = iconColor,
                dirPath = dirPath,
                installedAt = System.currentTimeMillis(),
                permissions = permissions,
                downloadUrl = downloadUrl
            )
        }
    }

    fun parseFromFile(manifestFile: File): Result<ToolInfo> {
        return runCatching {
            val json = manifestFile.readText()
            parse(json, manifestFile.parentFile!!.absolutePath).getOrThrow()
        }
    }
}
