# Android 工具平台 - 在线下载功能设计文档

**版本**: 1.0  
**日期**: 2025-06-26  
**状态**: 待实现  
**设计原则**: Ponytail (最简实现，够用就好)

---

## 1. 背景与目标

### 1.1 需求来源
用户需要从在线地址下载工具，支持：
- 任意 HTTP/HTTPS URL 下载
- 支持 ZIP 工具包 和 单 HTML 文件
- 下载链接随工具保存（收藏功能）
- 可检查更新并覆盖安装

### 1.2 设计目标
- 最少代码改动，最大程度复用现有逻辑
- 不引入第三方依赖
- 保持现有沙箱安全机制不变
- 用户体验流畅自然

---

## 2. 整体架构

### 2.1 流程图

```
┌──────────────┐
│ 主界面        │
│ MainActivity │
└──────┬───────┘
       │ 菜单项: 在线下载
       ↓
┌─────────────────┐
│ 在线下载页       │
│ DownloadActivity│───输入URL──┐
└─────────────────┘            │
                               ↓
                        ┌─────────────┐
                        │ HTTP 下载    │───→ 缓存目录
                        └──────┬──────┘
                               │
                               ↓
                    ┌────────────────────┐
                    │ 复用 ImportActivity │←────┐
                    └────────┬───────────┘     │
                             │                  │
                             ↓                  │
                      ┌───────────────┐         │
                      │ 安装到工具目录  │         │
                      └───────┬───────┘         │
                              │                 │
                              ↓                 │
                       ┌──────────────┐          │
                       │ manifest.json│          │
                       │ + downloadUrl│          │
                       └──────┬───────┘          │
                              │                  │
                              ↓                  │
                       ┌──────────────────┐      │
                       │ 详情页             │      │
                       │ ToolDetailActivity│──────┘
                       │ - 查看信息         │
                       │ - 编辑下载链接     │
                       │ - 检查更新         │
                       │ - 覆盖安装         │
                       └──────────────────┘
```

### 2.2 复用说明
- ✅ **100% 复用现有导入逻辑**：下载后直接交给 `ImportActivity`
- ✅ **复用 ToolManager**：安装、更新、删除逻辑不变
- ✅ **复用 manifest 机制**：仅新增一个字段

---

## 3. 数据结构设计

### 3.1 ToolInfo 数据模型变更

**文件**: `app/src/main/java/com/toolbox/app/model/ToolInfo.kt`

```kotlin
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

### 3.2 manifest.json 变更

```json
{
  "id": "my-tool",
  "name": "我的工具",
  "version": "1.0.0",
  "description": "工具描述",
  "author": "作者名",
  "icon": "",
  "iconColor": "#FF5722",
  "downloadUrl": "https://gitee.com/user/repo/raw/master/tool.zip"
}
```

### 3.3 兼容性
- 旧工具没有 `downloadUrl` 字段 → 解析为空字符串，不影响
- 导出的 ZIP 包会包含 `downloadUrl` → 在其他设备导入也能保留

---

## 4. 页面设计

### 4.1 下载页面 (DownloadActivity)

**布局**: `activity_download.xml`

| 元素 | 类型 | 说明 |
|------|------|------|
| 标题 | Toolbar | "在线下载工具"，带返回按钮 |
| URL 输入框 | TextInputEditText | 支持粘贴，默认提示 "https://..." |
| 提示文字 | TextView | "支持 ZIP 工具包或单 HTML 文件下载" |
| 下载按钮 | Button | "下载并导入"，URL 为空时禁用 |

**行为流程**:
1. 用户输入/粘贴 URL
2. 点击"下载并导入"
3. 显示加载状态："正在下载..."
4. 下载完成 → 启动 `ImportActivity` 并传递文件 Uri
5. 下载失败 → Toast 提示错误信息

**技术要点**:
- 使用 `HttpURLConnection` 下载（无第三方依赖）
- 下载在后台线程执行
- 根据 URL 后缀或 Content-Type 决定文件名
- 下载到 `context.cacheDir` 下的临时文件

---

### 4.2 详情页面 (ToolDetailActivity)

**布局**: `activity_tool_detail.xml`

| 区域 | 元素 | 说明 |
|------|------|------|
| **头部** | 工具图标 | 首字符 + 颜色背景（同列表页） |
| | 工具名称 | 大字号显示 |
| | 版本号 | 小字号 |
| **信息区** | 作者 | 作者名 |
| | 描述 | 工具描述 |
| | 安装时间 | 格式化显示 |
| **下载链接** | 输入框 | 可编辑的 downloadUrl |
| | 保存按钮 | 保存修改后的下载链接 |
| **操作区** | 检查更新按钮 | 下载检测版本，提示是否更新 |
| | 打开按钮 | 打开工具（同列表点击） |
| | 编辑按钮 | 打开 EditToolActivity |
| | 导出按钮 | 导出 ZIP |
| | 删除按钮 | 删除工具 |

**入口**:
- 长按工具 → 新增"详情"选项 → 打开详情页
- 后续可改为点击工具直接进详情，再点打开（可选，本次不做）

---

## 5. 核心功能设计

### 5.1 下载功能

**文件识别逻辑**:
```kotlin
fun detectFileType(url: String, contentType: String?): FileType {
    return when {
        url.endsWith(".html", ignoreCase = true) || 
        url.endsWith(".htm", ignoreCase = true) → HTML
        url.endsWith(".zip", ignoreCase = true) → ZIP
        contentType == "text/html" → HTML
        contentType == "application/zip" || 
        contentType == "application/x-zip-compressed" → ZIP
        else → ZIP  // 默认按ZIP处理，解压后检测
    }
}
```

**下载逻辑**:
```kotlin
// DownloadActivity
fun downloadFile(url: String): Result<File> {
    // 1. 创建连接
    val conn = URL(url).openConnection() as HttpURLConnection
    // 2. 下载到缓存目录的临时文件
    val tempFile = File(cacheDir, "download_${System.currentTimeMillis()}")
    // 3. 写入文件
    conn.inputStream.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    // 4. 根据类型重命名（方便 ImportActivity 识别）
    return if (isHtml) {
        val htmlFile = File(cacheDir, "tool_${System.currentTimeMillis()}.html")
        tempFile.renameTo(htmlFile)
        Result.success(htmlFile)
    } else {
        val zipFile = File(cacheDir, "tool_${System.currentTimeMillis()}.zip")
        tempFile.renameTo(zipFile)
        Result.success(zipFile)
    }
}
```

---

### 5.2 版本对比功能

**工具类**: `VersionComparator.kt` (可以作为扩展函数)

```kotlin
object VersionComparator {
    /**
     * 比较两个版本号
     * 返回: >0 = remote 更新, 0 = 相同, <0 = local 更新
     * Ponytail 简化版：只比较数字部分，忽略 alpha/beta 后缀
     */
    fun compare(localVersion: String, remoteVersion: String): Int {
        // 1. 清理版本前缀（v1.2.3 → 1.2.3）
        val local = localVersion.trim().removePrefix("v").removePrefix("V")
        val remote = remoteVersion.trim().removePrefix("v").removePrefix("V")
        
        // 2. 分割为数字数组
        val localParts = local.split('.').map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.split('.').map { it.toIntOrNull() ?: 0 }
        
        // 3. 逐位比较：major → minor → patch
        val maxLength = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val l = localParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (l != r) {
                return r - l  // 正数 = 远程更新
            }
        }
        
        return 0  // 版本相同
    }
    
    // 检查版本字符串是否有效
    fun isValid(version: String): Boolean {
        val clean = version.trim().removePrefix("v").removePrefix("V")
        return clean.matches(Regex("""^\d+(\.\d+)*$"""))
    }
}
```

**支持的版本格式**:
- ✅ `1.0.0`
- ✅ `v1.2.3`
- ✅ `V2.0`
- ✅ `1.5` (自动补 0 → 1.5.0)

**不支持（但不报错）**:
- `1.0.0-beta` → 截断为 `1.0.0` 比较
- `2024.06` → 按数字比较
- 任意非数字 → 视为 0

---

### 5.3 更新检查流程

```kotlin
// ToolDetailActivity
fun checkForUpdate() {
    val downloadUrl = currentTool.downloadUrl
    if (downloadUrl.isEmpty()) {
        Toast.makeText(this, "请先设置下载链接", Toast.LENGTH_SHORT).show()
        return
    }
    
    // 1. 下载远程文件（后台线程）
    showProgress("正在检查更新...")
    Thread {
        val remoteFile = downloadToTemp(downloadUrl).getOrElse {
            runOnUiThread { showError("下载失败: ${it.message}") }
            return@Thread
        }
        
        // 2. 解析远程版本
        val remoteVersion = extractVersion(remoteFile)
        
        runOnUiThread {
            when {
                // 情况1：双方都有版本号 → 对比
                remoteVersion != null && 
                VersionComparator.isValid(currentTool.version) -> {
                    val diff = VersionComparator.compare(
                        currentTool.version, 
                        remoteVersion
                    )
                    when {
                        diff > 0 -> showUpdateConfirm(remoteVersion)  // 有更新
                        diff == 0 -> showToast("已是最新版本")
                        else -> showToast("本地版本比远程更新")
                    }
                }
                
                // 情况2：无版本号 → 提示覆盖
                else -> showOverwriteConfirm()
            }
        }
    }.start()
}

// 提取版本号（从 ZIP 或 HTML）
fun extractVersion(file: File): String? {
    return if (file.name.endsWith(".zip")) {
        // 解压后从 manifest.json 读取
        unzipToTemp(file)?.let { tempDir ->
            val manifest = File(tempDir, "manifest.json")
            if (manifest.exists()) {
                readVersionFromManifest(manifest)
            } else {
                null  // ZIP 内无 manifest
            }
        }
    } else {
        // 单 HTML：尝试从注释读取 <!-- version: x.y.z -->
        readVersionFromHtmlComment(file)
    }
}
```

**版本提取失败的降级处理**:
- 提取不到版本号 → 不对比，直接问"是否覆盖安装？"
- 用户确认后 → 覆盖安装（同导入逻辑，工具ID保持不变）

---

### 5.4 覆盖安装

**复用现有逻辑**:
- 下载新文件 → 解压分析 → 用相同的 toolId 安装
- `ToolManager.installFromTempDir()` 会检查 ID 是否已存在
- 需要修改 `ToolManager` 支持覆盖安装：

```kotlin
// ToolManager.kt 改动
fun installFromTempDir(tempDir: File, toolId: String, overwrite: Boolean = false): ToolInfo {
    if (!overwrite && toolDir.toolExists(toolId)) {
        throw Exception("工具已存在")
    }
    
    // 如果覆盖模式，先删除旧的
    if (overwrite && toolDir.toolExists(toolId)) {
        deleteTool(toolId)
    }
    
    // 后续安装逻辑不变...
}
```

---

## 6. Manifest 解析与保存

### 6.1 ManifestParser 支持 downloadUrl

```kotlin
// ManifestParser.kt
fun parseFromFile(manifestFile: File): Result<ToolInfo> {
    return runCatching {
        val json = JSONObject(manifestFile.readText())
        ToolInfo(
            id = json.getString("id"),
            name = json.getString("name"),
            version = json.optString("version", "1.0.0"),
            description = json.optString("description", ""),
            author = json.optString("author", "Unknown"),
            iconPath = json.optString("icon", ""),
            iconColor = json.optString("iconColor", "#FF9800"),
            dirPath = manifestFile.parentFile?.absolutePath ?: "",
            installedAt = manifestFile.lastModified(),
            permissions = emptyList(),
            downloadUrl = json.optString("downloadUrl", "")  // 新增
        )
    }
}
```

### 6.2 保存 downloadUrl

```kotlin
// ToolManager.kt
fun updateDownloadUrl(toolId: String, downloadUrl: String): Boolean {
    val manifestFile = toolDir.getToolManifest(toolId)
    if (!manifestFile.exists()) return false
    
    return try {
        val json = JSONObject(manifestFile.readText())
        json.put("downloadUrl", downloadUrl)
        manifestFile.writeText(json.toString(2))
        true
    } catch (e: Exception) {
        false
    }
}
```

或者直接扩展现有的 `updateToolInfo()` 方法。

---

## 7. 字符串资源 (strings.xml)

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
```

---

## 8. 菜单项变更

### 8.1 主菜单新增

```xml
<!-- main_menu.xml -->
<item
    android:id="@+id/action_online_download"
    android:title="@string/online_download"
    android:icon="@drawable/ic_download"
    app:showAsAction="never" />
```

### 8.2 长按菜单新增

```kotlin
// MainActivity.showToolActions()
val items = arrayOf(
    getString(R.string.open),
    getString(R.string.details),  // 新增
    getString(R.string.export),
    getString(R.string.delete),
    getString(R.string.add_to_desktop),
    "编辑"
)
```

---

## 9. AndroidManifest 变更

```xml
<!-- 已有 INTERNET 权限，确认存在 -->
<uses-permission android:name="android.permission.INTERNET" />

<application>
    <!-- 新增 Activity -->
    <activity
        android:name=".DownloadActivity"
        android:exported="false"
        android:label="在线下载工具" />
    
    <activity
        android:name=".ToolDetailActivity"
        android:exported="false"
        android:label="工具详情" />
</application>
```

---

## 10. Ponytail 简化清单

本设计刻意做了以下简化（够用就好）：

| 简化项 | 说明 | 何时升级 |
|--------|------|----------|
| ❌ 无自动更新检查 | 仅用户手动触发检查 | 有需求时加 AlarmManager |
| ❌ 无下载进度条 | 只显示转圈，不显示百分比 | 需要时加 ProgressListener |
| ❌ 无断点续传 | 下载失败重新下 | 大文件多了再考虑 |
| ❌ 版本对比只支持数字 | 忽略 alpha/beta 后缀 | 有复杂版本需求时扩展 |
| ❌ 无下载历史/收藏列表 | downloadUrl 在 manifest 里就是收藏 | 需要单独收藏夹时再加 |
| ❌ 无批量更新 | 只能单个检查更新 | 工具多了再考虑 |
| ❌ 无多源管理 | 用户手动输入完整URL | 有市场源需求时再加 |

---

## 11. 文件改动总览

| 文件 | 操作 | 估计行数 |
|------|------|---------|
| `app/src/main/java/com/toolbox/app/model/ToolInfo.kt` | ✏️ 修改 | +1 |
| `app/src/main/java/com/toolbox/app/data/ManifestParser.kt` | ✏️ 修改 | +2 |
| `app/src/main/java/com/toolbox/app/data/ToolManager.kt` | ✏️ 修改 | +15 |
| `app/src/main/java/com/toolbox/app/MainActivity.kt` | ✏️ 修改 | +10 |
| `app/src/main/java/com/toolbox/app/util/VersionComparator.kt` | ➕ 新增 | ~30 |
| `app/src/main/java/com/toolbox/app/DownloadActivity.kt` | ➕ 新增 | ~150 |
| `app/src/main/java/com/toolbox/app/ToolDetailActivity.kt` | ➕ 新增 | ~200 |
| `app/src/main/res/layout/activity_download.xml` | ➕ 新增 | ~50 |
| `app/src/main/res/layout/activity_tool_detail.xml` | ➕ 新增 | ~100 |
| `app/src/main/res/values/strings.xml` | ✏️ 修改 | +15 |
| `app/src/main/res/menu/main_menu.xml` | ✏️ 修改 | +5 |
| `app/src/main/AndroidManifest.xml` | ✏️ 修改 | +8 |

**总计**: ~600 行代码改动，新增 4 个文件，修改 6 个文件。

---

## 12. 测试要点

1. **下载功能**:
   - ✅ HTTP/HTTPS URL 下载
   - ✅ 单 HTML 文件下载导入
   - ✅ ZIP 包下载导入
   - ✅ 下载错误处理（网络错误、404 等）

2. **版本对比**:
   - ✅ 正常语义化版本对比
   - ✅ 带 v 前缀的版本
   - ✅ 无版本号的降级处理
   - ✅ 远程版本更旧的情况

3. **更新流程**:
   - ✅ 有版本号时正确提示更新
   - ✅ 无版本号时提示覆盖
   - ✅ 覆盖安装后数据保留（data 目录）
   - ✅ 下载链接编辑和保存

4. **边界情况**:
   - ✅ 工具无 downloadUrl 的情况
   - ✅ 工具 ID 冲突处理
   - ✅ 下载中断的清理

---

*设计文档结束*
