package com.toolbox.app.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.toolbox.app.data.ToolManager

class BoxNative(
    private val context: Context,
    private val toolId: String,
    private val runOnUiThread: (Runnable) -> Unit,
    private val jsCallback: (String, String) -> Unit = { _, _ -> }
) {
    @JavascriptInterface
    fun toast(message: String) {
        runOnUiThread(Runnable {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        })
    }

    @JavascriptInterface
    fun alert(title: String, message: String, callbackId: String? = null) {
        runOnUiThread(Runnable {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    callbackId?.let { jsCallback(it, "true") }
                }
                .show()
        })
    }

    @JavascriptInterface
    fun confirm(title: String, message: String, callbackId: String) {
        runOnUiThread(Runnable {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    jsCallback(callbackId, "true")
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    jsCallback(callbackId, "false")
                }
                .show()
        })
    }

    @JavascriptInterface
    fun getToolInfo(): String {
        val tool = ToolManager(context).getTool(toolId)
        return tool?.let {
            """{"id":"${it.id}","name":"${it.name}","version":"${it.version}"}"""
        } ?: "{}"
    }
}
