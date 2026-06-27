package com.toolbox.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.toolbox.app.data.DownloadHistory
import com.toolbox.app.data.ToolManager
import com.toolbox.app.model.RepositoryTool
import com.toolbox.app.ui.ToolRepositoryAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class RepositoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRepoUrl: TextView
    private lateinit var tvCacheHint: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var adapter: ToolRepositoryAdapter
    private lateinit var toolManager: ToolManager
    private lateinit var downloadHistory: DownloadHistory
    private var isLoading = false

    // 预设仓库列表
    private val PRESET_REPOS = listOf(
        RepositoryInfo(
            name = "GitHub (默认)",
            apiUrl = "https://raw.githubusercontent.com/1010n111/toolbox-rep/master/tools.json",
            rawBase = "https://raw.githubusercontent.com/1010n111/toolbox-rep/master"
        ),
        RepositoryInfo(
            name = "jsDelivr CDN",
            apiUrl = "https://cdn.jsdelivr.net/gh/1010n111/toolbox-rep@master/tools.json",
            rawBase = "https://cdn.jsdelivr.net/gh/1010n111/toolbox-rep@master"
        ),
        RepositoryInfo(
            name = "Gitee Raw",
            apiUrl = "https://gitee.com/1010n111/toolbox-rep/raw/master/tools.json",
            rawBase = "https://gitee.com/1010n111/toolbox-rep/raw/master"
        ),
        RepositoryInfo(
            name = "GitLab Raw",
            apiUrl = "https://gitlab.com/1010n111/toolbox-rep/-/raw/master/tools.json",
            rawBase = "https://gitlab.com/1010n111/toolbox-rep/-/raw/master"
        )
    )

    data class RepositoryInfo(
        val name: String,
        val apiUrl: String,
        val rawBase: String
    )

    // 默认使用第一个 (GitHub)
    private val DEFAULT_REPO_API_URL = PRESET_REPOS[0].apiUrl
    private val DEFAULT_REPO_RAW_BASE = PRESET_REPOS[0].rawBase

    // 备用仓库地址 (jsDelivr CDN)
    private val FALLBACK_REPO_API_URL = PRESET_REPOS[1].apiUrl
    private val FALLBACK_REPO_RAW_BASE = PRESET_REPOS[1].rawBase

    // 内置默认工具列表，完全离线可用（空列表，表示只从服务器获取）
    private val BUILTIN_TOOLS = """
    {
        "tools": []
    }
    """.trimIndent()

    private val PREFS_NAME = "RepositoryPrefs"
    private val KEY_REPO_API_URL = "repo_api_url"
    private val KEY_REPO_RAW_BASE = "repo_raw_base"
    private val KEY_CACHE_DATA = "repo_cache_data"
    private val KEY_CACHE_TIME = "repo_cache_time"

    private var repoApiUrl: String = DEFAULT_REPO_API_URL
    private var repoRawBase: String = DEFAULT_REPO_RAW_BASE
    private var cachedTools: List<RepositoryTool> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.tool_repository)

        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        tvRepoUrl = findViewById(R.id.tv_repo_url)
        tvCacheHint = findViewById(R.id.tv_cache_hint)
        btnRefresh = findViewById(R.id.btn_refresh)

        toolManager = ToolManager(this)
        downloadHistory = DownloadHistory(this)

        // 加载保存的仓库地址
        loadRepositorySettings()

        adapter = ToolRepositoryAdapter(
            onInstallClick = { tool -> installTool(tool) },
            toolManager = toolManager,
            downloadHistory = downloadHistory
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 点击仓库地址可以修改
        tvRepoUrl.setOnClickListener {
            showEditRepositoryDialog()
        }

        // 刷新按钮
        btnRefresh.setOnClickListener {
            loadRepository(forceRefresh = true)
        }

        // 只加载缓存，不自动刷新网络
        loadFromCache()

        // 如果没有缓存数据，才请求网络
        if (cachedTools.isEmpty()) {
            loadRepository(forceRefresh = false)
        }
    }

    private fun loadRepositorySettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        repoApiUrl = prefs.getString(KEY_REPO_API_URL, DEFAULT_REPO_API_URL) ?: DEFAULT_REPO_API_URL
        repoRawBase = prefs.getString(KEY_REPO_RAW_BASE, DEFAULT_REPO_RAW_BASE) ?: DEFAULT_REPO_RAW_BASE
        tvRepoUrl.text = repoApiUrl
    }

    private fun saveRepositorySettings(apiUrl: String, rawBase: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_REPO_API_URL, apiUrl)
            .putString(KEY_REPO_RAW_BASE, rawBase)
            .apply()
        repoApiUrl = apiUrl
        repoRawBase = rawBase
        tvRepoUrl.text = apiUrl
        // 清除缓存，强制刷新
        clearCache()
        loadRepository(forceRefresh = true)
    }

    private fun loadFromCache() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val cacheData = prefs.getString(KEY_CACHE_DATA, null)
        val cacheTime = prefs.getLong(KEY_CACHE_TIME, 0)

        if (cacheData != null && cacheData.isNotEmpty()) {
            try {
                cachedTools = parseToolsFromJson(cacheData)
                adapter.submitList(cachedTools)
                val timeDiff = (System.currentTimeMillis() - cacheTime) / 1000 / 60
                tvCacheHint.visibility = View.VISIBLE
                tvCacheHint.text = "缓存数据 (${timeDiff}分钟前)"
                return
            } catch (e: Exception) {
                Log.e("RepositoryActivity", "Cache parse error: ${e.message}")
            }
        }
        tvCacheHint.visibility = View.GONE
    }

    private fun saveToCache(tools: List<RepositoryTool>) {
        val jsonArray = JSONArray()
        tools.forEach { tool ->
            val json = JSONObject()
            json.put("id", tool.id)
            json.put("name", tool.name)
            json.put("version", tool.version)
            json.put("description", tool.description)
            json.put("author", tool.author)
            json.put("downloadUrl", tool.downloadUrl)
            json.put("dirName", tool.dirName)
            jsonArray.put(json)
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CACHE_DATA, jsonArray.toString())
            .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun clearCache() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_CACHE_DATA)
            .remove(KEY_CACHE_TIME)
            .apply()
        cachedTools = emptyList()
    }

    private fun parseToolsFromJson(jsonString: String): List<RepositoryTool> {
        val tools = mutableListOf<RepositoryTool>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            tools.add(
                RepositoryTool(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    version = json.getString("version"),
                    description = json.optString("description", ""),
                    author = json.optString("author", "Unknown"),
                    downloadUrl = json.getString("downloadUrl"),
                    dirName = json.optString("dirName", "")
                )
            )
        }
        return tools
    }

    private fun showEditRepositoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_repository, null)
        val inputRepoUrl = dialogView.findViewById<TextInputEditText>(R.id.input_repo_url)
        val inputApiUrl = dialogView.findViewById<TextInputEditText>(R.id.input_api_url)
        val inputRawBase = dialogView.findViewById<TextInputEditText>(R.id.input_raw_base)

        // 从当前 API URL 反推仓库地址
        inputRepoUrl.setText(extractRepoUrl(repoApiUrl))
        inputApiUrl.setText(repoApiUrl)
        inputRawBase.setText(repoRawBase)

        // 监听仓库地址变化，自动生成 API 和 Raw 地址
        inputRepoUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val repoUrl = s?.toString()?.trim() ?: ""
                if (repoUrl.isNotEmpty()) {
                    val generated = generateRepoUrls(repoUrl)
                    inputApiUrl.setText(generated.first)
                    inputRawBase.setText(generated.second)
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_repository)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newApiUrl = inputApiUrl.text?.toString()?.trim() ?: ""
                val newRawBase = inputRawBase.text?.toString()?.trim() ?: ""

                if (newApiUrl.isNotEmpty() && newRawBase.isNotEmpty()) {
                    saveRepositorySettings(newApiUrl, newRawBase)
                    Toast.makeText(this, R.string.repository_updated, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.url_cannot_be_empty, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.reset_default) { _, _ ->
                saveRepositorySettings(DEFAULT_REPO_API_URL, DEFAULT_REPO_RAW_BASE)
                Toast.makeText(this, R.string.reset_to_default, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 从仓库地址生成 API 和 Raw 基础地址
     * 支持 GitHub、Gitee、GitLab
     */
    private fun generateRepoUrls(repoUrl: String): Pair<String, String> {
        val url = repoUrl.trimEnd('/')

        return when {
            // GitHub: https://github.com/user/repo
            url.contains("github.com") -> {
                val path = url.substringAfter("github.com").trimStart('/')
                val rawBase = "https://raw.githubusercontent.com/$path/master"
                val apiUrl = "$rawBase/tools.json"
                Pair(apiUrl, rawBase)
            }
            // Gitee: https://gitee.com/user/repo
            url.contains("gitee.com") -> {
                val path = url.substringAfter("gitee.com").trimStart('/')
                val rawBase = "https://gitee.com/$path/raw/master"
                val apiUrl = "$rawBase/tools.json"
                Pair(apiUrl, rawBase)
            }
            // GitLab: https://gitlab.com/user/repo
            url.contains("gitlab.com") -> {
                val path = url.substringAfter("gitlab.com").trimStart('/')
                val rawBase = "https://gitlab.com/$path/-/raw/master"
                val apiUrl = "$rawBase/tools.json"
                Pair(apiUrl, rawBase)
            }
            // 其他地址，保持原样
            else -> Pair(url, url)
        }
    }

    /**
     * 从 API URL 反推仓库地址
     */
    private fun extractRepoUrl(apiUrl: String): String {
        return when {
            apiUrl.contains("raw.githubusercontent.com") -> {
                val path = apiUrl.substringAfter("raw.githubusercontent.com").trimStart('/')
                    .replace("/master/tools.json", "").replace("/master/", "")
                "https://github.com/$path"
            }
            apiUrl.contains("gitee.com") && apiUrl.contains("/raw/") -> {
                val path = apiUrl.substringAfter("gitee.com").trimStart('/')
                    .replace("/raw/master/tools.json", "").replace("/raw/master/", "")
                "https://gitee.com/$path"
            }
            apiUrl.contains("gitlab.com") && apiUrl.contains("/-/raw/") -> {
                val path = apiUrl.substringAfter("gitlab.com").trimStart('/')
                    .replace("/-/raw/master/tools.json", "").replace("/-/raw/master/", "")
                "https://gitlab.com/$path"
            }
            apiUrl.contains("cdn.jsdelivr.net") -> {
                val path = apiUrl.substringAfter("cdn.jsdelivr.net/gh/").trimStart('/')
                    .replace("@master/tools.json", "").replace("@master/", "")
                "https://github.com/$path"
            }
            else -> ""
        }
    }

    private fun loadRepository(forceRefresh: Boolean = false) {
        if (isLoading) return
        isLoading = true
        progressBar.visibility = View.VISIBLE
        if (forceRefresh) {
            Toast.makeText(this, "正在刷新...", Toast.LENGTH_SHORT).show()
        }

        Thread {
            try {
                val tools = fetchRepositoryTools()
                runOnUiThread {
                    cachedTools = tools
                    saveToCache(tools)
                    adapter.submitList(tools)
                    progressBar.visibility = View.GONE
                    tvCacheHint.visibility = View.GONE
                    if (forceRefresh) {
                        Toast.makeText(this, "刷新完成", Toast.LENGTH_SHORT).show()
                    }
                    if (tools.isEmpty()) {
                        Toast.makeText(this, R.string.no_tools, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (cachedTools.isNotEmpty()) {
                        Toast.makeText(this, "使用缓存数据: ${e.message}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                isLoading = false
            }
        }.start()
    }

    private fun fetchRepositoryTools(): List<RepositoryTool> {
        val tools = mutableListOf<RepositoryTool>()

        // 方案1: 尝试从配置的 URL 获取工具列表 JSON
        var jsonStr: String? = null
        var useFallback = false

        try {
            jsonStr = fetchText(repoApiUrl)
        } catch (e: Exception) {
            Log.w("RepositoryActivity", "主仓库访问失败: ${e.message}，尝试备用地址")
            useFallback = true
        }

        // 方案2: 主仓库失败，尝试 GitHub 备用地址
        if (useFallback && repoApiUrl == DEFAULT_REPO_API_URL) {
            try {
                jsonStr = fetchText(FALLBACK_REPO_API_URL)
                repoRawBase = FALLBACK_REPO_RAW_BASE
            } catch (e: Exception) {
                Log.w("RepositoryActivity", "备用仓库也失败: ${e.message}，使用内置工具")
            }
        }

        // 方案3: 网络都失败，使用内置工具列表
        if (jsonStr.isNullOrEmpty()) {
            jsonStr = BUILTIN_TOOLS
        }

        // 解析工具列表 JSON
        try {
            val json = JSONObject(jsonStr)
            val toolsArray = json.getJSONArray("tools")

            for (i in 0 until toolsArray.length()) {
                val toolObj = toolsArray.getJSONObject(i)
                val id = toolObj.getString("id")
                val name = toolObj.getString("name")
                val version = toolObj.optString("version", "1.0.0")
                val description = toolObj.optString("description", "")
                val author = toolObj.optString("author", "Unknown")
                val path = toolObj.getString("path")

                // 组装下载地址：rawBase + path
                val downloadUrl = if (path.startsWith("http")) {
                    path // 绝对地址
                } else {
                    "$repoRawBase/$path" // 相对地址
                }

                tools.add(
                    RepositoryTool(
                        id = id,
                        name = name,
                        version = version,
                        description = description,
                        author = author,
                        downloadUrl = downloadUrl,
                        dirName = id
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("RepositoryActivity", "解析工具列表失败: ${e.message}")
            throw Exception("工具列表格式错误: ${e.message}")
        }

        return tools
    }

    private fun fetchJson(urlString: String): String {
        return fetchText(urlString)
    }

    private fun fetchText(urlString: String): String {
        Log.d("RepositoryActivity", "请求URL: $urlString")
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true  // 自动跟随重定向

        // 添加完整的浏览器请求头，避免Gitee 403
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.setRequestProperty("Accept", "application/json, text/plain, */*")
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        conn.setRequestProperty("Referer", "https://gitee.com/")

        val responseCode = conn.responseCode
        Log.d("RepositoryActivity", "响应码: $responseCode")

        // 处理301/302重定向（手动处理，解决CDN问题）
        if (responseCode in 300..399) {
            val location = conn.getHeaderField("Location")
            Log.d("RepositoryActivity", "重定向到: $location")
            if (!location.isNullOrEmpty()) {
                return fetchText(location)  // 递归请求重定向地址
            }
        }

        if (responseCode != 200) {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText()?.take(200) ?: ""
            } catch (e: Exception) {
                ""
            }
            Log.e("RepositoryActivity", "请求失败: $errorBody")
            throw Exception("HTTP $responseCode: ${conn.responseMessage}")
        }

        val content = conn.inputStream.use { input ->
            input.bufferedReader().readText()
        }
        Log.d("RepositoryActivity", "响应内容长度: ${content.length}")
        Log.d("RepositoryActivity", "响应内容前200字: ${content.take(200)}")
        return content
    }

    private fun installTool(tool: RepositoryTool) {
        Toast.makeText(this, "正在下载: ${tool.name}", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val file = downloadFile(tool.downloadUrl)
                runOnUiThread {
                    startImportActivity(file, tool.downloadUrl)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun downloadFile(urlString: String): File {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")

        // 处理重定向
        val redirect = conn.getHeaderField("Location")
        if (redirect != null && conn.responseCode in 300..399) {
            conn.disconnect()
            return downloadFile(redirect)
        }

        if (conn.responseCode != 200) {
            throw Exception("HTTP ${conn.responseCode}")
        }

        return conn.inputStream.use { input ->
            val suffix = if (urlString.endsWith(".html", ignoreCase = true)) ".html" else ".zip"
            val tempFile = File(cacheDir, "repo_${System.currentTimeMillis()}$suffix")

            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
            tempFile
        }
    }

    private fun startImportActivity(file: File, downloadUrl: String) {
        val intent = Intent(this, ImportActivity::class.java).apply {
            data = Uri.fromFile(file)
            putExtra("download_url", downloadUrl)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // 返回页面时刷新安装状态
        if (cachedTools.isNotEmpty()) {
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.repository_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                loadRepository(forceRefresh = true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
