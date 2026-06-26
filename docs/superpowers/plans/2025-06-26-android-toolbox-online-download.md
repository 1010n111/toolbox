# Android 工具平台 - 在线下载功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Android 工具平台添加在线下载功能，支持从任意 URL 下载 ZIP/HTML 工具，保存下载链接到 manifest，并支持手动检查更新和覆盖安装。

**Architecture:** 三层架构复用现有设计。新增 DownloadActivity 处理 HTTP 下载，下载完成后直接传递给现有 ImportActivity 复用导入逻辑。新增 ToolDetailActivity 展示工具信息、管理下载链接和检查更新。版本比较逻辑独立为工具类，无第三方依赖。

**Tech Stack:** Kotlin, Android SDK 34, HttpURLConnection, Material Components (已存在), 无新增第三方依赖

## Global Constraints

- minSdkVersion: 34 (Android 14)
- targetSdkVersion: 34
- 语言: Kotlin
- 第三方依赖: 除 Material Components 外无其他依赖
- 严格沙箱隔离: 每个工具只能访问自己目录下的文件
- Ponytail 原则：最简实现，够用就好，不做过度设计

---

## Task 1: 数据模型 ToolInfo 新增 downloadUrl 字段

**Files:**
- Modify: `app/src/main/java/com/toolbox/app/model/ToolInfo.kt`
- Modify: `app/src/main/java/com/toolbox/app/data/ManifestParser.kt`
- Test: `app/src/test/java/com/toolbox/app/model/ToolInfoTest.kt`

**Interfaces:**
- Produces: `ToolInfo.downloadUrl: String = ""` (数据字段，默认空字符串)

---

- [ ] **Step 1: 修改 ToolInfo 数据类，添加 downloadUrl 字段**

```kotlin
// 修改 ToolInfo.kt 第 3-14 行
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
    val permissions: List<String> = emptyList(),
    val downloadUrl: String = ""  // 新增字段，默认空字符串
)
```

- [ ] **Step 2: 修改 ManifestParser，解析 downloadUrl**

在 `parseFromFile` 方法中读取 `downloadUrl` 字段：

```kotlin
// 在 ManifestParser.parseFromFile() 的 ToolInfo 构造处添加
ToolInfo(
    // ... 其他字段保持不变
    iconColor = json.optString("iconColor", "#FF9800"),
    dirPath = manifestFile.parentFile?.absolutePath ?: "",
    installedAt = manifestFile.lastModified(),
    permissions = emptyList(),
    downloadUrl = json.optString("downloadUrl", "")  // 新增
)
```

- [ ] **Step 3: 运行现有测试验证不破坏兼容性**

Run: `./gradlew testDebugUnitTest --tests "com.toolbox.app.model.ToolInfoTest"`
Expected: All tests PASS

- [ ] **Step 4: 验证无 downloadUrl 的 manifest 可正常解析**

在 ToolInfoTest 中添加测试：

```kotlin
@Test
fun testToolInfo_withoutDownloadUrl_parsesCorrectly() {
    val json = """
        {
            "id": "test-tool",
            "name": "Test Tool",
            "version": "1.0.0",
            "description": "Test",
            "author": "Test",
            "icon": "",
            "iconColor": "#FF0000"
        }
    """.trimIndent()
    
    val tempFile = File.createTempFile("manifest", ".json")
    tempFile.writeText(json)
    
    val result = ManifestParser.parseFromFile(tempFile)
    assertTrue(result.isSuccess)
    
    val toolInfo = result.getOrThrow()
    assertEquals("", toolInfo.downloadUrl)  // 默认为空字符串
    
    tempFile.delete()
}
```

- [ ] **Step 5: 运行新增测试**

Run: `./gradlew testDebugUnitTest --tests "com.toolbox.app.model.ToolInfoTest.testToolInfo_withoutDownloadUrl_parsesCorrectly"`
Expected: PASS

- [ ] **Step 6: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 编译通过，测试全部通过，新旧 manifest 都能正确解析

---

## Task 2: 版本比较工具类 VersionComparator

**Files:**
- Create: `app/src/main/java/com/toolbox/app/util/VersionComparator.kt`
- Create: `app/src/test/java/com/toolbox/app/util/VersionComparatorTest.kt`

**Interfaces:**
- Produces: `VersionComparator.compare(local: String, remote: String): Int`
- Produces: `VersionComparator.isValid(version: String): Boolean`

---

- [ ] **Step 1: 写测试，先写失败的测试用例**

```kotlin
// VersionComparatorTest.kt
package com.toolbox.app.util

import org.junit.Test
import org.junit.Assert.*

class VersionComparatorTest {

    @Test
    fun compare_sameVersions_returnsZero() {
        assertEquals(0, VersionComparator.compare("1.0.0", "1.0.0"))
    }

    @Test
    fun compare_remoteHigher_returnsPositive() {
        assertTrue(VersionComparator.compare("1.0.0", "1.0.1") > 0)
        assertTrue(VersionComparator.compare("1.0.0", "1.1.0") > 0)
        assertTrue(VersionComparator.compare("1.9.9", "2.0.0") > 0)
    }

    @Test
    fun compare_localHigher_returnsNegative() {
        assertTrue(VersionComparator.compare("1.0.1", "1.0.0") < 0)
    }

    @Test
    fun compare_withVPrefix_ignoresPrefix() {
        assertEquals(0, VersionComparator.compare("v1.0.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("V1.0.0", "1.0.0"))
    }

    @Test
    fun compare_differentLengths_padsWithZero() {
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("1", "1.0.0"))
    }

    @Test
    fun isValid_validVersions_returnsTrue() {
        assertTrue(VersionComparator.isValid("1.0.0"))
        assertTrue(VersionComparator.isValid("v1.0.0"))
        assertTrue(VersionComparator.isValid("2.0"))
    }

    @Test
    fun isValid_invalidVersions_returnsFalse() {
        assertFalse(VersionComparator.isValid(""))
        assertFalse(VersionComparator.isValid("unknown"))
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.toolbox.app.util.VersionComparatorTest"`
Expected: FAIL with "Unresolved reference: VersionComparator"

- [ ] **Step 3: 实现 VersionComparator**

```kotlin
// VersionComparator.kt
package com.toolbox.app.util

object VersionComparator {
    /**
     * 比较两个版本号
     * 返回: >0 = 远程版本更新, 0 = 相同, <0 = 本地版本更新
     * Ponytail 简化版：只比较数字部分，忽略 alpha/beta 后缀
     */
    fun compare(localVersion: String, remoteVersion: String): Int {
        val local = cleanVersion(localVersion)
        val remote = cleanVersion(remoteVersion)
        
        val localParts = local.split('.').map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.split('.').map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val l = localParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (l != r) {
                return r - l
            }
        }
        
        return 0
    }
    
    fun isValid(version: String): Boolean {
        val clean = cleanVersion(version)
        return clean.isNotEmpty() && clean.matches(Regex("""^\d+(\.\d+)*$"""))
    }
    
    private fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew testDebugUnitTest --tests "com.toolbox.app.util.VersionComparatorTest"`
Expected: All 7 tests PASS

- [ ] **Step 5: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 所有版本比较测试通过，编译成功

---

## Task 3: ToolManager 支持下载链接和覆盖安装

**Files:**
- Modify: `app/src/main/java/com/toolbox/app/data/ToolManager.kt`
- Test: `app/src/test/java/com/toolbox/app/data/ToolManagerTest.kt` (新建如果不存在)

**Interfaces:**
- Consumes: `ToolInfo.downloadUrl` from Task 1
- Produces: `ToolManager.updateDownloadUrl(toolId: String, url: String): Boolean`
- Produces: `ToolManager.installFromTempDir(..., overwrite: Boolean = false): ToolInfo`

---

- [ ] **Step 1: 修改 installFromTempDir 支持覆盖安装**

修改 `installFromTempDir` 方法签名，增加 `overwrite` 参数：

```kotlin
fun installFromTempDir(tempDir: File, toolId: String, overwrite: Boolean = false): ToolInfo {
    // 修改工具存在检查逻辑
    if (!overwrite && toolDir.toolExists(toolId)) {
        throw Exception("工具已存在")
    }
    
    // 覆盖模式：先删除旧版本
    if (overwrite && toolDir.toolExists(toolId)) {
        deleteTool(toolId)
    }
    
    // 后续原有安装逻辑不变...
}
```

- [ ] **Step 2: 添加 updateDownloadUrl 方法**

```kotlin
// 在 ToolManager.kt 末尾添加
fun updateDownloadUrl(toolId: String, downloadUrl: String): Boolean {
    val manifestFile = toolDir.getToolManifest(toolId)
    if (!manifestFile.exists()) return false
    
    return try {
        val json = org.json.JSONObject(manifestFile.readText())
        json.put("downloadUrl", downloadUrl)
        manifestFile.writeText(json.toString(2))
        true
    } catch (e: Exception) {
        false
    }
}
```

- [ ] **Step 3: 扩展 updateToolInfo 方法也保存 downloadUrl**

在 `updateToolInfo` 方法参数中添加：

```kotlin
fun updateToolInfo(
    toolId: String,
    name: String,
    version: String,
    author: String,
    description: String,
    iconColor: String,
    downloadUrl: String = ""  // 新增
): Boolean {
    // ...
    json.put("downloadUrl", downloadUrl)  // 在写回 JSON 处添加
    // ...
}
```

- [ ] **Step 4: 写测试验证覆盖安装**

```kotlin
// ToolManagerTest.kt
package com.toolbox.app.data

import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ToolManagerTest {

    @Test
    fun installFromTempDir_overwrite_replacesOldTool() {
        val context = RuntimeEnvironment.getApplication() as Context
        val toolManager = ToolManager(context)
        
        // 1. 创建临时工具目录
        val tempDir = File(context.cacheDir, "test_tool")
        tempDir.mkdirs()
        
        // 2. 写 manifest (v1.0.0)
        val manifest = File(tempDir, "manifest.json")
        manifest.writeText("""
            {"id":"test-overwrite","name":"Test","version":"1.0.0","description":"","author":"","icon":"","iconColor":"#FF0000"}
        """.trimIndent())
        File(tempDir, "index.html").writeText("<html></html>")
        
        // 3. 首次安装
        toolManager.installFromTempDir(tempDir, "test-overwrite")
        val installed1 = toolManager.getTool("test-overwrite")
        assertEquals("1.0.0", installed1?.version)
        
        // 4. 创建 v2.0.0
        val tempDir2 = File(context.cacheDir, "test_tool_v2")
        tempDir2.mkdirs()
        File(tempDir2, "manifest.json").writeText("""
            {"id":"test-overwrite","name":"Test","version":"2.0.0","description":"","author":"","icon":"","iconColor":"#FF0000"}
        """.trimIndent())
        File(tempDir2, "index.html").writeText("<html></html>")
        
        // 5. 覆盖安装
        toolManager.installFromTempDir(tempDir2, "test-overwrite", overwrite = true)
        val installed2 = toolManager.getTool("test-overwrite")
        assertEquals("2.0.0", installed2?.version)
        
        // 清理
        toolManager.deleteTool("test-overwrite")
        tempDir.deleteRecursively()
        tempDir2.deleteRecursively()
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `./gradlew testDebugUnitTest --tests "com.toolbox.app.data.ToolManagerTest"`
Expected: PASS

- [ ] **Step 6: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 覆盖安装测试通过，下载链接保存功能可用

---

## Task 4: 字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

---

- [ ] **Step 1: 添加所有需要的字符串**

在 `strings.xml` 末尾添加：

```xml
<string name="online_download">在线下载</string>
<string name="download_tool">下载并导入</string>
<string name="downloading">正在下载...</string>
<string name="download_failed">下载失败</string>
<string name="enter_url">请输入工具下载地址</string>
<string name="url_hint">https://example.com/tool.zip</string>
<string name="download_support_format">支持 ZIP 工具包或单 HTML 文件</string>
<string name="tool_detail">工具详情</string>
<string name="download_link">下载链接</string>
<string name="save_link">保存链接</string>
<string name="check_update">检查更新</string>
<string name="checking_update">正在检查更新...</string>
<string name="update_available">发现新版本：%1$s\n当前版本：%2$s\n是否更新？</string>
<string name="update_now">立即更新</string>
<string name="no_update">已是最新版本</string>
<string name="version_unknown">无法检测版本差异，是否覆盖安装？</string>
<string name="overwrite">覆盖安装</string>
<string name="installed_at">安装时间：%1$s</string>
<string name="details">详情</string>
<string name="open_tool">打开</string>
<string name="edit_tool">编辑</string>
```

- [ ] **Step 2: 编译验证字符串资源**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 字符串资源添加完成，编译通过

---

## Task 5: 下载页面布局 activity_download.xml

**Files:**
- Create: `app/src/main/res/layout/activity_download.xml`

---

- [ ] **Step 1: 创建下载页面布局**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/enter_url">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/input_url"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textUri"
            android:hint="@string/url_hint"
            android:maxLines="2" />

    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/download_support_format"
        android:textSize="12sp"
        android:textColor="@android:color/darker_gray"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="24dp" />

    <Button
        android:id="@+id/btn_download"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/download_tool"
        android:enabled="false"
        android:backgroundTint="#2196F3"
        android:textColor="@android:color/white" />

</LinearLayout>
```

- [ ] **Step 2: 编译验证布局**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 布局 XML 无语法错误，编译通过

---

## Task 6: DownloadActivity 下载页面实现

**Files:**
- Create: `app/src/main/java/com/toolbox/app/DownloadActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: 现有 ImportActivity 的导入流程
- Produces: `DownloadActivity` - 可从 MainActivity 启动

---

- [ ] **Step 1: 实现 DownloadActivity**

```kotlin
package com.toolbox.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DownloadActivity : AppCompatActivity() {

    private lateinit var inputUrl: TextInputEditText
    private lateinit var btnDownload: Button
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.online_download)

        inputUrl = findViewById(R.id.input_url)
        btnDownload = findViewById(R.id.btn_download)

        // URL 输入变化监听
        inputUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString()?.trim() ?: ""
                btnDownload.isEnabled = url.startsWith("http://") || url.startsWith("https://")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnDownload.setOnClickListener {
            startDownload()
        }
    }

    private fun startDownload() {
        val url = inputUrl.text?.toString()?.trim() ?: return
        if (isDownloading) return

        isDownloading = true
        btnDownload.isEnabled = false
        btnDownload.text = getString(R.string.downloading)

        Thread {
            try {
                val file = downloadFile(url)
                runOnUiThread {
                    startImportActivity(file)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.download_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
        }.start()
    }

    private fun downloadFile(urlString: String): File {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        return conn.inputStream.use { input ->
            val contentType = conn.contentType
            val isHtml = urlString.endsWith(".html", ignoreCase = true) ||
                          urlString.endsWith(".htm", ignoreCase = true) ||
                          contentType == "text/html"

            val suffix = if (isHtml) ".html" else ".zip"
            val tempFile = File(cacheDir, "download_${System.currentTimeMillis()}$suffix")
            
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
            tempFile
        }
    }

    private fun startImportActivity(file: File) {
        val intent = Intent(this, ImportActivity::class.java).apply {
            data = Uri.fromFile(file)
        }
        startActivity(intent)
        finish()
    }

    private fun resetButton() {
        isDownloading = false
        btnDownload.isEnabled = true
        btnDownload.text = getString(R.string.download_tool)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
```

- [ ] **Step 2: 在 AndroidManifest 中注册 Activity**

在 `<application>` 标签内添加：

```xml
<activity
    android:name=".DownloadActivity"
    android:exported="false"
    android:label="@string/online_download" />
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ DownloadActivity 实现完成，无编译错误

---

## Task 7: 详情页布局 activity_tool_detail.xml

**Files:**
- Create: `app/src/main/res/layout/activity_tool_detail.xml`

---

- [ ] **Step 1: 创建详情页布局**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- 工具图标和名称 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginBottom="16dp">

            <TextView
                android:id="@+id/tv_tool_icon"
                android:layout_width="64dp"
                android:layout_height="64dp"
                android:textSize="28sp"
                android:textColor="@android:color/white"
                android:gravity="center"
                android:background="@drawable/shape_circle" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="16dp"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tv_tool_name"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textSize="20sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tv_version"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textSize="14sp"
                    android:textColor="@android:color/darker_gray" />

            </LinearLayout>

        </LinearLayout>

        <!-- 作者和描述 -->
        <TextView
            android:id="@+id/tv_author"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tv_description"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:lineSpacingExtra="4dp"
            android:layout_marginBottom="16dp" />

        <TextView
            android:id="@+id/tv_installed_at"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="@android:color/darker_gray"
            android:layout_marginBottom="24dp" />

        <!-- 分割线 -->
        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginBottom="16dp" />

        <!-- 下载链接 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/download_link"
            android:textSize="14sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/input_download_url"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textUri"
                android:hint="@string/url_hint"
                android:maxLines="2"
                android:textSize="14sp" />

        </com.google.android.material.textfield.TextInputLayout>

        <Button
            android:id="@+id/btn_save_link"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/save_link"
            android:layout_marginBottom="24dp"
            android:enabled="false" />

        <!-- 检查更新按钮 -->
        <Button
            android:id="@+id/btn_check_update"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/check_update"
            android:backgroundTint="#4CAF50"
            android:textColor="@android:color/white"
            android:layout_marginBottom="24dp" />

        <!-- 分割线 -->
        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginBottom="16dp" />

        <!-- 操作按钮 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <Button
                android:id="@+id/btn_open"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/open_tool"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/btn_edit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/edit_tool" />

        </LinearLayout>

    </LinearLayout>

</ScrollView>
```

- [ ] **Step 2: 创建圆形背景 shape_circle.xml（如果不存在）**

检查 `app/src/main/res/drawable/shape_circle.xml`，如果不存在则创建：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#FF9800" />
</shape>
```

- [ ] **Step 3: 编译验证布局**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 详情页布局无语法错误，编译通过

---

## Task 8: ToolDetailActivity 详情页实现（上半部分：基础UI）

**Files:**
- Create: `app/src/main/java/com/toolbox/app/ToolDetailActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `ToolInfo.downloadUrl`, `ToolManager.getTool()`, `ToolManager.updateDownloadUrl()`

---

- [ ] **Step 1: 实现 ToolDetailActivity 基础 UI 部分**

```kotlin
package com.toolbox.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.toolbox.app.data.ToolManager
import com.toolbox.app.model.ToolInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolDetailActivity : AppCompatActivity() {

    private lateinit var toolManager: ToolManager
    private lateinit var toolId: String
    private var currentTool: ToolInfo? = null

    private lateinit var tvToolIcon: TextView
    private lateinit var tvToolName: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvInstalledAt: TextView
    private lateinit var inputDownloadUrl: TextInputEditText
    private lateinit var btnSaveLink: Button
    private lateinit var btnCheckUpdate: Button
    private lateinit var btnOpen: Button
    private lateinit var btnEdit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.tool_detail)

        toolId = intent.getStringExtra("tool_id") ?: run {
            Toast.makeText(this, "工具ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        toolManager = ToolManager(this)

        initViews()
        loadToolInfo()
        setupListeners()
    }

    private fun initViews() {
        tvToolIcon = findViewById(R.id.tv_tool_icon)
        tvToolName = findViewById(R.id.tv_tool_name)
        tvVersion = findViewById(R.id.tv_version)
        tvAuthor = findViewById(R.id.tv_author)
        tvDescription = findViewById(R.id.tv_description)
        tvInstalledAt = findViewById(R.id.tv_installed_at)
        inputDownloadUrl = findViewById(R.id.input_download_url)
        btnSaveLink = findViewById(R.id.btn_save_link)
        btnCheckUpdate = findViewById(R.id.btn_check_update)
        btnOpen = findViewById(R.id.btn_open)
        btnEdit = findViewById(R.id.btn_edit)
    }

    private fun loadToolInfo() {
        currentTool = toolManager.getTool(toolId)
        val tool = currentTool ?: run {
            Toast.makeText(this, "工具不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置图标背景和文字
        val color = try {
            android.graphics.Color.parseColor(tool.iconColor)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#FF9800")
        }
        (tvToolIcon.background as? android.graphics.drawable.GradientDrawable)?.setColor(color)
        tvToolIcon.text = tool.name.firstOrNull()?.toString() ?: "?"

        tvToolName.text = tool.name
        tvVersion.text = "v${tool.version}"
        tvAuthor.text = "作者: ${tool.author}"
        tvDescription.text = tool.description.ifEmpty { "暂无描述" }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        tvInstalledAt.text = getString(R.string.installed_at, dateFormat.format(Date(tool.installedAt)))
        
        inputDownloadUrl.setText(tool.downloadUrl)
        
        // 如果有下载链接，启用检查更新按钮
        btnCheckUpdate.isEnabled = tool.downloadUrl.isNotEmpty()
    }

    private fun setupListeners() {
        // 下载链接变化监听
        inputDownloadUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString()?.trim() ?: ""
                val originalUrl = currentTool?.downloadUrl ?: ""
                btnSaveLink.isEnabled = url != originalUrl && 
                    (url.isEmpty() || url.startsWith("http://") || url.startsWith("https://"))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 保存下载链接
        btnSaveLink.setOnClickListener {
            val url = inputDownloadUrl.text?.toString()?.trim() ?: ""
            val success = toolManager.updateDownloadUrl(toolId, url)
            if (success) {
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
                btnSaveLink.isEnabled = false
                btnCheckUpdate.isEnabled = url.isNotEmpty()
                currentTool = currentTool?.copy(downloadUrl = url)
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }

        // 打开工具
        btnOpen.setOnClickListener {
            currentTool?.let { tool ->
                val intent = Intent(this, ToolActivity::class.java).apply {
                    putExtra("tool_id", tool.id)
                    putExtra("tool_name", tool.name)
                }
                startActivity(intent)
            }
        }

        // 编辑工具
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditToolActivity::class.java).apply {
                putExtra("tool_id", toolId)
            }
            startActivity(intent)
        }

        // 检查更新按钮在 Task 9 中实现
        btnCheckUpdate.setOnClickListener {
            Toast.makeText(this, "检查更新功能实现中...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadToolInfo()  // 编辑后刷新
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
```

- [ ] **Step 2: 在 AndroidManifest 中注册**

在 `<application>` 内添加：

```xml
<activity
    android:name=".ToolDetailActivity"
    android:exported="false"
    android:label="@string/tool_detail" />
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 详情页基础UI实现完成，可正常显示工具信息

---

## Task 9: ToolDetailActivity 检查更新功能

**Files:**
- Modify: `app/src/main/java/com/toolbox/app/ToolDetailActivity.kt`

**Interfaces:**
- Consumes: `VersionComparator.compare()`, `ToolManager.installFromTempDir(overwrite = true)`

---

- [ ] **Step 1: 检查更新 - 添加下载临时文件和版本提取方法**

在 `ToolDetailActivity` 类内添加以下方法：

```kotlin
// 在类内添加
private var isCheckingUpdate = false

private fun checkForUpdate() {
    val tool = currentTool ?: return
    val downloadUrl = tool.downloadUrl
    if (downloadUrl.isEmpty()) {
        Toast.makeText(this, "请先设置下载链接", Toast.LENGTH_SHORT).show()
        return
    }
    if (isCheckingUpdate) return

    isCheckingUpdate = true
    btnCheckUpdate.isEnabled = false
    btnCheckUpdate.text = getString(R.string.checking_update)

    Thread {
        try {
            // 1. 下载远程文件
            val remoteFile = downloadFileTemp(downloadUrl)
            
            // 2. 尝试提取版本号
            val remoteVersion = extractVersion(remoteFile)
            
            runOnUiThread {
                handleVersionCheckResult(remoteVersion, remoteFile)
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "${getString(R.string.download_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                resetCheckUpdateButton()
            }
        }
    }.start()
}

private fun downloadFileTemp(urlString: String): File {
    val url = URL(urlString)
    val conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 15000
    conn.readTimeout = 30000

    return conn.inputStream.use { input ->
        val contentType = conn.contentType
        val isHtml = urlString.endsWith(".html", ignoreCase = true) ||
                      urlString.endsWith(".htm", ignoreCase = true) ||
                      contentType == "text/html"

        val suffix = if (isHtml) ".html" else ".zip"
        val tempFile = File(cacheDir, "update_${System.currentTimeMillis()}$suffix")
        
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
        tempFile
    }
}

private fun extractVersion(file: File): String? {
    return if (file.name.endsWith(".zip")) {
        // 解压 ZIP 后读取 manifest
        val tempDir = File(cacheDir, "extract_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        com.toolbox.app.util.ZipUtils.unzip(file, tempDir).getOrNull()
        val manifest = File(tempDir, "manifest.json")
        val version = if (manifest.exists()) {
            try {
                org.json.JSONObject(manifest.readText()).optString("version", null)
            } catch (e: Exception) {
                null
            }
        } else null
        tempDir.deleteRecursively()
        version
    } else {
        // HTML: 尝试从注释读取 <!-- version: x.y.z -->
        val content = file.readText()
        val pattern = Regex("""<!--\s*version:\s*([^\s]+)\s*-->""")
        pattern.find(content)?.groupValues?.get(1)
    }
}

private fun handleVersionCheckResult(remoteVersion: String?, downloadedFile: File) {
    val tool = currentTool ?: return

    when {
        // 情况1：双方都有有效版本号 -> 对比
        remoteVersion != null && com.toolbox.app.util.VersionComparator.isValid(tool.version) -> {
            val diff = com.toolbox.app.util.VersionComparator.compare(tool.version, remoteVersion)
            when {
                diff > 0 -> showUpdateConfirm(remoteVersion, downloadedFile)
                diff == 0 -> {
                    Toast.makeText(this, R.string.no_update, Toast.LENGTH_SHORT).show()
                    resetCheckUpdateButton()
                    downloadedFile.delete()
                }
                else -> {
                    Toast.makeText(this, "本地版本比远程更新", Toast.LENGTH_SHORT).show()
                    resetCheckUpdateButton()
                    downloadedFile.delete()
                }
            }
        }
        
        // 情况2：无版本号 -> 提示覆盖安装
        else -> showOverwriteConfirm(downloadedFile)
    }
}

private fun showUpdateConfirm(remoteVersion: String, file: File) {
    AlertDialog.Builder(this)
        .setTitle("发现新版本")
        .setMessage(getString(R.string.update_available, remoteVersion, currentTool?.version))
        .setPositiveButton(R.string.update_now) { _, _ ->
            performOverwriteInstall(file)
        }
        .setNegativeButton(android.R.string.cancel) { _, _ ->
            file.delete()
            resetCheckUpdateButton()
        }
        .show()
}

private fun showOverwriteConfirm(file: File) {
    AlertDialog.Builder(this)
        .setTitle("版本检测")
        .setMessage(R.string.version_unknown)
        .setPositiveButton(R.string.overwrite) { _, _ ->
            performOverwriteInstall(file)
        }
        .setNegativeButton(android.R.string.cancel) { _, _ ->
            file.delete()
            resetCheckUpdateButton()
        }
        .show()
}

private fun performOverwriteInstall(file: File) {
    Toast.makeText(this, "正在更新...", Toast.LENGTH_SHORT).show()
    
    Thread {
        try {
            // 解压到临时目录
            val tempDir = File(cacheDir, "update_install_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            if (file.name.endsWith(".html")) {
                // 单 HTML：复制到临时目录
                file.copyTo(File(tempDir, "index.html"))
                // 生成 manifest
                val manifest = """
                    {
                        "id": "$toolId",
                        "name": "${currentTool?.name ?: "Tool"}",
                        "version": "1.0.0",
                        "description": "",
                        "author": "",
                        "icon": "",
                        "iconColor": "${currentTool?.iconColor ?: "#FF9800"}",
                        "downloadUrl": "${currentTool?.downloadUrl ?: ""}"
                    }
                """.trimIndent()
                File(tempDir, "manifest.json").writeText(manifest)
            } else {
                // ZIP：解压
                com.toolbox.app.util.ZipUtils.unzip(file, tempDir).getOrThrow()
            }
            
            // 覆盖安装
            toolManager.installFromTempDir(tempDir, toolId, overwrite = true)
            
            runOnUiThread {
                Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()
                loadToolInfo()  // 刷新显示
                resetCheckUpdateButton()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "更新失败: ${e.message}", Toast.LENGTH_LONG).show()
                resetCheckUpdateButton()
            }
        }
    }.start()
}

private fun resetCheckUpdateButton() {
    isCheckingUpdate = false
    btnCheckUpdate.isEnabled = true
    btnCheckUpdate.text = getString(R.string.check_update)
}
```

- [ ] **Step 2: 修改 setupListeners 中 btnCheckUpdate 的点击事件**

将：
```kotlin
btnCheckUpdate.setOnClickListener {
    Toast.makeText(this, "检查更新功能实现中...", Toast.LENGTH_SHORT).show()
}
```

改为：
```kotlin
btnCheckUpdate.setOnClickListener {
    checkForUpdate()
}
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 检查更新功能完整实现，版本对比和覆盖安装可用

---

## Task 10: MainActivity 菜单项入口接入

**Files:**
- Modify: `app/src/main/java/com/toolbox/app/MainActivity.kt`
- Modify: `app/src/main/res/menu/main_menu.xml`

**Interfaces:**
- Produces: 主菜单启动 DownloadActivity，长按菜单启动 ToolDetailActivity

---

- [ ] **Step 1: 添加主菜单项**

在 `main_menu.xml` 中添加：

```xml
<item
    android:id="@+id/action_online_download"
    android:title="@string/online_download"
    app:showAsAction="never" />
```

- [ ] **Step 2: 修改 MainActivity 处理菜单项点击**

在 `onOptionsItemSelected` 方法中添加 case：

```kotlin
R.id.action_online_download -> {
    startActivity(Intent(this, DownloadActivity::class.java))
    true
}
```

- [ ] **Step 3: 在长按菜单中添加"详情"选项**

在 `showToolActions` 方法中修改 items 数组：

```kotlin
val items = arrayOf(
    getString(R.string.open),
    getString(R.string.details),  // 新增详情
    getString(R.string.export),
    getString(R.string.delete),
    getString(R.string.add_to_desktop),
    "编辑"
)
```

- [ ] **Step 4: 处理详情菜单点击**

在 click 处理的 when 中添加：

```kotlin
1 -> openToolDetail(tool)  // index=1 是详情
```

- [ ] **Step 5: 添加 openToolDetail 方法**

```kotlin
private fun openToolDetail(tool: ToolInfo) {
    val intent = Intent(this, ToolDetailActivity::class.java).apply {
        putExtra("tool_id", tool.id)
    }
    startActivity(intent)
}
```

- [ ] **Step 6: 编译验证**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**验收标准:** ✅ 主菜单和长按菜单都正确接入，可正常启动下载页和详情页

---

## Task 11: 整体集成测试

**Files:**
- 所有已修改文件

---

- [ ] **Step 1: 完整编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行所有单元测试**

Run: `./gradlew testDebugUnitTest`
Expected: All tests PASS

- [ ] **Step 3: 手动测试用例验证（在设备上运行）**

1. **下载功能测试：**
   - [ ] 主菜单 → 在线下载 → 输入有效的 ZIP 工具 URL
   - [ ] 点击下载 → 成功进入导入页面 → 导入成功
   - [ ] 工具列表中可看到新导入的工具

2. **详情页测试：**
   - [ ] 长按工具 → 详情 → 工具信息正确显示
   - [ ] 在下载链接处输入新的 URL → 点击保存 → 提示"已保存"
   - [ ] 退出详情页再进入 → URL 已持久化

3. **版本对比测试（用测试工具）：**
   - [ ] 准备一个 v1.0 的工具和 v2.0 的同 ID 工具
   - [ ] 在详情页设置 v2.0 的下载链接
   - [ ] 点击检查更新 → 提示发现新版本
   - [ ] 点击更新 → 更新成功，版本号变为 v2.0

4. **无版本号降级测试：**
   - [ ] 设置一个无 manifest 的 HTML 工具下载链接
   - [ ] 点击检查更新 → 提示"无法检测版本差异，是否覆盖安装？"
   - [ ] 确认覆盖 → 安装成功

5. **导出/导入验证：**
   - [ ] 设置了 downloadUrl 的工具导出为 ZIP
   - [ ] 重新导入 → downloadUrl 也被一起导入

**验收标准:** ✅ 所有功能正常工作，无崩溃，测试全部通过

---

## 实施完成检查清单

- [ ] Task 1: 数据模型 ToolInfo 新增 downloadUrl 字段 ✅
- [ ] Task 2: 版本比较工具类 VersionComparator ✅
- [ ] Task 3: ToolManager 支持下载链接和覆盖安装 ✅
- [ ] Task 4: 字符串资源 ✅
- [ ] Task 5: 下载页面布局 ✅
- [ ] Task 6: DownloadActivity 下载页面 ✅
- [ ] Task 7: 详情页布局 ✅
- [ ] Task 8: ToolDetailActivity 基础 UI ✅
- [ ] Task 9: ToolDetailActivity 检查更新功能 ✅
- [ ] Task 10: MainActivity 菜单接入 ✅
- [ ] Task 11: 整体集成测试 ✅

---

*Plan complete and saved to `docs/superpowers/plans/2025-06-26-android-toolbox-online-download.md`*

## Execution Options

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
