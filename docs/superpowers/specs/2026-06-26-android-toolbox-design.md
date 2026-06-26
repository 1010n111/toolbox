# Android 工具平台设计文档

**日期**: 2026-06-26
**作者**: Claude Code
**状态**: 设计中

---

## 1. 项目概述

### 1.1 定位
一个Android工具类应用，采用"底座+HTML小工具"的插件化架构。底座提供运行环境和原生能力，各种实用工具以单HTML页面的形式运行在底座之上。

### 1.2 目标用户
- 个人使用，为主
- 注重简单、可定制的工具用户

### 1.3 核心特性
- ✅ 纯WebView底座，无需跨平台框架
- ✅ 工具以ZIP包或单HTML文件分发，支持导入导出
- ✅ 每个工具在自己的沙箱中运行
- ✅ 异步JS桥接层，不阻塞UI
- ✅ 内置调试支持（Chrome远程调试）
- ✅ 网格工具列表，直观易用
- ✅ 支持创建桌面快捷方式
- ✅ 支持通过系统分享菜单直接导入

---

## 2. 架构设计

### 2.1 三层架构

```
┌─────────────────────────────────┐
│   工具层 (HTML/CSS/JS)          │  每个工具是独立ZIP包
│  ┌───────┐  ┌───────┐  ┌───────┐
│  │ 工具1  │  │ 工具2  │  │ 工具3  │
│  └───────┘  └───────┘  └───────┘
├─────────────────────────────────┤
│   桥接层 (JS Bridge)            │  暴露原生能力给JS
│  ┌───────────────────────────┐  │
│  │  ToolBridge 接口           │  │
│  └───────────────────────────┘  │
├─────────────────────────────────┤
│   底座层 (Kotlin + Android)     │  原生APP容器
│  ┌────────┐  ┌────────────────┐ │
│  │ 主页   │  │  ToolManager   │ │
│  └────────┘  └────────────────┘ │
└─────────────────────────────────┘
```

### 2.2 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | Android官方推荐，简洁 |
| UI框架 | 原生XML + Material | 最简单，无需学习Compose |
| WebView | 系统自带 | 零依赖，自动更新 |
| 工具格式 | ZIP bundle | 支持多文件，易于分发 |

---

## 3. 底座层详细设计

### 3.1 主要组件

#### 3.1.1 MainActivity（主界面）
- **布局**: 顶部工具栏 + RecyclerView网格
- **工具栏**: 导入按钮 + 溢出菜单（设置、关于）
- **网格项**: 工具图标 + 工具名称
- **交互**:
  - 点击工具 → 打开ToolActivity
  - 长按工具 → 弹出操作菜单（打开 / 导出 / 删除）

#### 3.1.2 ToolActivity（工具运行界面）
- **布局**: 全屏WebView + 顶部返回箭头
- **WebView配置**:
  - 启用JavaScript
  - 启用DOM Storage
  - 不允许跳转到外部URL（白名单机制）
  - 注入ToolBridge对象
  - **开启远程调试**: `WebView.setWebContentsDebuggingEnabled(true)`
  - 支持 console 日志输出到 Logcat
- **返回行为**: 先尝试WebView.goBack()，再退出Activity

#### 3.1.3 ShortcutManager（快捷方式管理）
```kotlin
class ShortcutManager(private val context: Context) {
    // 为工具创建桌面快捷方式
    fun createShortcut(tool: ToolInfo): Boolean
    // 更新快捷方式图标和名称
    fun updateShortcut(tool: ToolInfo)
    // 删除快捷方式
    fun removeShortcut(toolId: String)
}
```

#### 3.1.4 ToolManager（工具管理核心）
```kotlin
class ToolManager(private val context: Context) {
    
    // 安装工具：解压ZIP，验证manifest
    fun installTool(zipUri: Uri): Result<ToolInfo>
    
    // 导出工具：打包为ZIP，保存到Downloads
    fun exportTool(toolId: String): Result<Uri>
    
    // 获取已安装工具列表
    fun listTools(): List<ToolInfo>
    
    // 删除工具
    fun deleteTool(toolId: String): Boolean
    
    // 获取工具根目录
    fun getToolDir(toolId: String): File
}
```

#### 3.1.5 ToolInfo（工具数据类）
```kotlin
data class ToolInfo(
    val id: String,           // 唯一标识
    val name: String,         // 显示名称
    val version: String,      // 版本号
    val description: String,  // 描述
    val author: String,       // 作者
    val iconPath: String,     // 图标相对路径
    val dirPath: String,      // 安装目录
    val installedAt: Long     // 安装时间
)
```

### 3.2 存储结构

```
/data/data/com.yourname.toolbox/
└── tools/
    ├── <tool-id>/           # 每个工具一个独立目录
    │   ├── manifest.json    # 元数据
    │   ├── index.html       # 入口页面
    │   ├── *.css            # 样式文件
    │   ├── *.js             # 脚本文件
    │   ├── assets/          # 静态资源
    │   └── data/            # 工具私有数据目录
    └── <tool-id>/
        └── ...
```

---

## 4. JS桥接层设计

### 4.1 核心原则
- 第一阶段只实现最小必要接口
- 预留通用扩展接口，未来无需改HTML代码即可加新能力
- 严格沙箱隔离，防止跨工具访问

### 4.2 桥接接口

```kotlin
@JavascriptInterface
class ToolBridge(private val toolId: String) {
    
    // ========== 第一阶段：最小实现 ==========
    
    /** 读取工具私有目录下的文件 */
    @JavascriptInterface
    fun readFile(path: String): String?
    
    /** 写入文件到工具私有目录 */
    @JavascriptInterface
    fun writeFile(path: String, content: String): Boolean
    
    /** 列出目录内容 */
    @JavascriptInterface
    fun listFiles(path: String): Array<String>
    
    /** 删除文件或目录 */
    @JavascriptInterface
    fun deleteFile(path: String): Boolean
    
    // ========== 预留：全能力扩展接口 ==========
    
    /**
     * 通用原生API调用入口
     * 未来新增能力只需要服务端实现，无需改HTML工具代码
     */
    @JavascriptInterface
    fun callNative(apiName: String, paramsJson: String): String?
}
```

### 4.3 JavaScript 使用方式

#### 回调方式（推荐，不阻塞UI）
```javascript
// 读取文件
window.ToolBridge.readFile("data/notes.txt", function(content, error) {
    if (error) console.error(error);
    else console.log(content);
});

// 写入文件
window.ToolBridge.writeFile("data/notes.txt", "Hello World", function(success, error) {
    console.log(success ? "成功" : "失败: " + error);
});
```

#### 同步方式（简单场景用）
```javascript
// 小文件可以同步读
const content = window.ToolBridge.readFileSync("data/notes.txt");
```

#### 内置标准库（自动注入）
```javascript
// 所有工具自动可用的全局方法
$box.toast("提示信息");
$box.alert("标题", "内容", function() { /* 点击确定 */ });
$box.confirm("确认删除？", function(yes) { if (yes) { ... } });
$box.openUrl("https://example.com");
$box.getToolInfo();  // 返回当前工具的manifest信息

// 未来调用扩展API（统一入口）
window.ToolBridge.callNative("clipboard_get", {}, function(result, error) {
    console.log(result);
});
```

### 4.4 安全机制

- **路径校验**: 所有路径先resolve，禁止 `..` 跳出工具目录
- **沙箱隔离**: 每个ToolActivity注入独立的ToolBridge实例，绑定当前toolId
- **权限声明**: 未来扩展API需要在manifest.json中声明权限

---

## 5. 工具层格式规范

### 5.1 工具包格式

**格式A：完整ZIP包（推荐）**
```
my-tool.zip
├── manifest.json      ← 必填，元数据
├── index.html         ← 必填，入口页面
├── style.css          ← 可选
├── script.js          ← 可选
└── assets/            ← 可选，图片、字体等
    └── icon.png
```

**格式B：单HTML文件（简化）**
```
my-tool.html           ← meta标签内嵌入manifest信息
```

自动处理：导入单HTML文件时，系统自动生成manifest.json和基础目录结构。

```html
<!-- 单文件工具的 meta 标签示例 -->
<meta name="toolbox-id" content="my-notebook">
<meta name="toolbox-name" content="简易笔记本">
<meta name="toolbox-version" content="1.0.0">
<meta name="toolbox-description" content="一个简单的本地笔记工具">
```

### 5.2 manifest.json 规范

```json
{
  "id": "my-notebook",
  "name": "简易笔记本",
  "version": "1.0.0",
  "description": "一个简单的本地笔记工具",
  "author": "Your Name",
  "icon": "assets/icon.png",
  "permissions": []
}
```

**字段说明**:
- `id`: 全局唯一标识符，建议用横线分隔的小写英文
- `name`: 用户可见的工具名称
- `version`: 语义化版本号
- `icon`: ZIP包内图标文件的相对路径，建议 96x96 PNG
- `permissions`: 预留，未来声明需要的原生API权限

### 5.3 工具开发注意事项

- 所有资源用相对路径引用
- 数据存在 `data/` 目录下，不要硬编码绝对路径
- 不要依赖外部CDN，所有JS/CSS打包进ZIP
- ToolBridge对象在页面加载时注入

---

## 6. 导入导出流程

### 6.1 导入流程

```
用户点击"导入"
    ↓
打开系统文件选择器（过滤*.zip）
    ↓
复制ZIP到临时目录
    ↓
验证manifest.json存在且格式正确
    ↓
检查toolId是否已存在
    ↓
解压到工具目录
    ↓
刷新工具列表
    ↓
提示"导入成功"
```

### 6.2 导出流程

```
用户长按工具 → 选择"导出"
    ↓
打包工具目录为ZIP（不含data/目录？可配置）
    ↓
保存到公共Download目录
    ↓
通知媒体扫描器，让文件立即可见
    ↓
提示"已导出到 Download/工具名.zip"
```

---

## 7. 实现计划

### 阶段一：MVP最小可用版本
- [ ] 新建Android项目，配置基础依赖
- [ ] 实现ToolManager（ZIP解压/打包）
- [ ] 主页网格布局（RecyclerView）
- [ ] ToolActivity + WebView基础配置
- [ ] 开启WebView远程调试支持
- [ ] 异步JS Bridge（文件I/O + 内置$box标准库）
- [ ] 导入导出功能（ZIP + 单HTML）
- [ ] 支持系统分享菜单导入

### 阶段二：体验优化
- [ ] 工具图标加载和显示
- [ ] 导入时的进度提示
- [ ] 删除确认对话框
- [ ] 错误处理和友好提示
- [ ] 夜间模式适配
- [ ] 创建桌面快捷方式功能
- [ ] 长按菜单增加"添加到桌面"

### 阶段三：扩展能力
- [ ] 完善 `callNative` 通用接口
- [ ] 逐步添加原生能力（剪贴板、相机、存储、通知等）
- [ ] 权限声明和校验机制
- [ ] 工具内置模板/示例工具
- [ ] 工具市场/分享机制

---

## 8. 边界和限制

### 明确不做的（第一版）
- ❌ 不做跨平台（只Android）
- ❌ 不做工具市场（手动导入导出）
- ❌ 不做工具更新检查
- ❌ 不做多标签页（一次开一个工具）
- ❌ 不做云端同步（导出到本地文件）

### 性能考虑
- WebView启动开销：可接受，个人使用不在乎那几百ms
- 每个工具独立WebView：内存占用合理，一次只开一个
- ZIP解压/打包：用系统自带ZipFile，不引入第三方库

---

## 9. 示例工具构想

为了验证平台可用性，可以先做几个示例工具：

1. **简易笔记本** - 用readFile/writeFile存取文本
2. **JSON格式化工具** - 纯前端，不需要桥接
3. **密码生成器** - 纯前端
4. **单位换算器** - 纯前端
5. **Markdown预览器** - 纯前端 + 读取文件

---

## 附录：开发环境要求

- Android Studio Hedgehog 或更高
- minSdkVersion: 26 (Android 8.0)
- targetSdkVersion: 最新稳定版
- 不需要额外第三方依赖
