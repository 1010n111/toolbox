package com.toolbox.app.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import com.toolbox.app.data.ToolDirectory
import java.io.File

class ToolBridge(
    private val context: Context,
    private val toolId: String,
    private val callbackHandler: (String, String?) -> Unit
) {
    private val toolDir = ToolDirectory(context).getToolDir(toolId)

    private fun resolvePath(path: String): File {
        val toolDirCanonical = toolDir.canonicalFile
        val file = File(toolDir, path).canonicalFile
        val toolDirPath = toolDirCanonical.absolutePath + File.separator
        val filePath = file.absolutePath
        if (!filePath.startsWith(toolDirPath) && filePath != toolDirCanonical.absolutePath) {
            throw SecurityException("Access denied: $path")
        }
        return file
    }

    @JavascriptInterface
    fun readFile(path: String, callbackId: String) {
        Thread {
            try {
                android.util.Log.d("ToolBridge", "readFile: $path")
                val file = resolvePath(path)
                val content = file.readText()
                android.util.Log.d("ToolBridge", "readFile success: ${content.length} chars")
                callbackHandler(callbackId, content)
            } catch (e: Exception) {
                android.util.Log.e("ToolBridge", "readFile failed: $path", e)
                callbackHandler(callbackId, null)
            }
        }.start()
    }

    @JavascriptInterface
    fun readFileSync(path: String): String? {
        return try {
            val file = resolvePath(path)
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    @JavascriptInterface
    fun writeFile(path: String, content: String, callbackId: String) {
        Thread {
            try {
                android.util.Log.d("ToolBridge", "writeFile: $path, content length: ${content.length}")
                val file = resolvePath(path)
                android.util.Log.d("ToolBridge", "resolved path: ${file.absolutePath}")
                file.parentFile?.mkdirs()
                file.writeText(content)
                android.util.Log.d("ToolBridge", "writeFile success")
                callbackHandler(callbackId, "true")
            } catch (e: Exception) {
                android.util.Log.e("ToolBridge", "writeFile failed: $path", e)
                callbackHandler(callbackId, "false")
            }
        }.start()
    }

    @JavascriptInterface
    fun writeFileSync(path: String, content: String): Boolean {
        return try {
            val file = resolvePath(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun listFiles(path: String, callbackId: String) {
        Thread {
            try {
                val dir = resolvePath(path)
                val files = dir.listFiles()?.map { it.name }?.joinToString(",")
                callbackHandler(callbackId, files)
            } catch (e: Exception) {
                callbackHandler(callbackId, null)
            }
        }.start()
    }

    @JavascriptInterface
    fun deleteFile(path: String, callbackId: String) {
        Thread {
            try {
                val file = resolvePath(path)
                val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                callbackHandler(callbackId, success.toString())
            } catch (e: Exception) {
                callbackHandler(callbackId, "false")
            }
        }.start()
    }

    @JavascriptInterface
    fun callNative(api: String, paramsJson: String, callbackId: String) {
        callbackHandler(callbackId, null)
    }
}
