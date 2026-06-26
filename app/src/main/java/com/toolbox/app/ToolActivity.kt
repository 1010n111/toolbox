package com.toolbox.app

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.toolbox.app.bridge.BoxNative
import com.toolbox.app.bridge.ToolBridge
import com.toolbox.app.data.ToolDirectory
import com.toolbox.app.databinding.ActivityToolBinding

class ToolActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolBinding
    private lateinit var toolId: String
    private lateinit var toolName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolId = intent.getStringExtra("tool_id") ?: throw IllegalArgumentException("tool_id required")
        toolName = intent.getStringExtra("tool_name") ?: "Tool"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = toolName

        binding.toolbar.setNavigationOnClickListener {
            if (binding.webview.canGoBack()) {
                binding.webview.goBack()
            } else {
                finish()
            }
        }

        setupWebView()
        loadTool()
    }

    private fun setupWebView() {
        val webView = binding.webview
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        WebView.setWebContentsDebuggingEnabled(true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return url?.startsWith("file://") != true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("ToolActivity", "onPageFinished: $url")
                injectJsWrapper()
            }

            override fun onReceivedError(view: WebView?, req: android.webkit.WebResourceRequest, err: android.webkit.WebResourceError) {
                super.onReceivedError(view, req, err)
                android.util.Log.e("ToolActivity", "WebView error: ${err.errorCode} ${err.description} at ${req.url}")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsConfirm(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                android.app.AlertDialog.Builder(this@ToolActivity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                android.app.AlertDialog.Builder(this@ToolActivity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onConsoleMessage(cm: android.webkit.ConsoleMessage): Boolean {
                android.util.Log.d("ToolJS", "${cm.message()} at ${cm.sourceId()}:${cm.lineNumber()}")
                return super.onConsoleMessage(cm)
            }
        }
        injectJavascriptInterfaces()
    }

    private fun injectJavascriptInterfaces() {
        val webView = binding.webview

        webView.addJavascriptInterface(ToolBridge(this, toolId) { callbackId, result ->
            runOnUiThread {
                val escapedResult = result?.replace("\\", "\\\\")?.replace("'", "\\'") ?: "null"
                val jsValue = if (result == null) "null" else "'$escapedResult'"
                webView.evaluateJavascript("window._toolBridgeCallback('$callbackId', $jsValue)", null)
            }
        }, "ToolBridgeNative")

        val jsCallbackHandler = { callbackId: String, result: String ->
            runOnUiThread {
                webView.evaluateJavascript("window._boxCallback('$callbackId', '$result')", null)
            }
        }
        webView.addJavascriptInterface(BoxNative(this, toolId, ::runOnUiThread, jsCallbackHandler), "BoxNative")
    }

    private fun injectJsWrapper() {
        val jsWrapper = """
            (function() {
                var _callbacks = {};
                var _callbackId = 0;

                window.ToolBridge = {
                    readFile: function(path, callback) {
                        var id = ++_callbackId;
                        _callbacks[id] = callback;
                        window.ToolBridgeNative.readFile(path, id);
                    },
                    readFileSync: function(path) {
                        return window.ToolBridgeNative.readFileSync(path);
                    },
                    writeFile: function(path, content, callback) {
                        var id = ++_callbackId;
                        _callbacks[id] = callback;
                        window.ToolBridgeNative.writeFile(path, content, id);
                    },
                    writeFileSync: function(path, content) {
                        return window.ToolBridgeNative.writeFileSync(path, content);
                    },
                    listFiles: function(path, callback) {
                        var id = ++_callbackId;
                        _callbacks[id] = callback;
                        window.ToolBridgeNative.listFiles(path, id);
                    },
                    deleteFile: function(path, callback) {
                        var id = ++_callbackId;
                        _callbacks[id] = callback;
                        window.ToolBridgeNative.deleteFile(path, id);
                    },
                    callNative: function(api, params, callback) {
                        var id = ++_callbackId;
                        _callbacks[id] = callback;
                        window.ToolBridgeNative.callNative(api, JSON.stringify(params), id);
                    }
                };

                window._toolBridgeCallback = function(id, result) {
                    var cb = _callbacks[id];
                    if (cb) {
                        cb(result);
                        delete _callbacks[id];
                    }
                };

                var _boxCallbacks = {};
                var _boxCallbackId = 0;

                window._boxCallback = function(id, result) {
                    var cb = _boxCallbacks[id];
                    if (cb) {
                        cb(result === 'true');
                        delete _boxCallbacks[id];
                    }
                };

                window.${"$"}box = {
                    toast: function(msg) { window.BoxNative.toast(msg); },
                    alert: function(title, msg, cb) {
                        var id = ++_boxCallbackId;
                        if (cb) _boxCallbacks[id] = cb;
                        window.BoxNative.alert(title, msg, id.toString());
                    },
                    confirm: function(title, msg, cb) {
                        var id = ++_boxCallbackId;
                        if (cb) _boxCallbacks[id] = cb;
                        window.BoxNative.confirm(title, msg, id.toString());
                    },
                    getToolInfo: function() { return JSON.parse(window.BoxNative.getToolInfo()); }
                };
            })();
        """.trimIndent()

        binding.webview.evaluateJavascript(jsWrapper, null)
    }

    private fun loadTool() {
        val indexFile = ToolDirectory(this).getToolIndexHtml(toolId)
        android.util.Log.d("ToolActivity", "Loading tool: $toolId")
        android.util.Log.d("ToolActivity", "Index path: ${indexFile.absolutePath}")
        android.util.Log.d("ToolActivity", "File exists: ${indexFile.exists()}")

        if (indexFile.exists()) {
            android.util.Log.d("ToolActivity", "File size: ${indexFile.length()} bytes")
            binding.webview.loadUrl("file://${indexFile.absolutePath}")
        } else {
            // 列出目录内容帮助调试
            val dir = indexFile.parentFile
            if (dir != null && dir.exists()) {
                android.util.Log.d("ToolActivity", "Dir contents:")
                dir.listFiles()?.forEach { f ->
                    android.util.Log.d("ToolActivity", "  - ${f.name} (${f.length()} bytes)")
                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
