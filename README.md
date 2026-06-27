# ToolBox - Android HTML 工具平台

一个轻量级的 Android 工具平台，可以运行打包成 ZIP 的 HTML 单页应用。底座提供沙箱运行环境和原生能力桥接，让你可以用 HTML/CSS/JS 快速开发各种小工具。

---

## ✨ 功能特性

| 特性 | 状态 | 说明 |
|------|------|------|
| 📦 ZIP 工具包导入 | ✅ | 支持导入标准 ZIP 格式的工具包 |
| 📄 单 HTML 导入 | ✅ | 直接导入单个 HTML 文件，自动生成 manifest |
| 🌐 **在线下载** | ✅ | 支持从 URL 直接下载 ZIP/HTML 工具包 |
| 📥 **版本检查更新** | ✅ | 支持在线检查工具版本并一键更新 |
| 🔗 **下载链接持久化** | ✅ | 工具下载链接保存到 manifest，便于后续更新 |
| 📤 工具导出 | ✅ | 已安装的工具可以导出分享 |
| 🖍️ 工具编辑 | ✅ | 可修改工具名称、版本、描述、图标颜色 |
| 🌈 图标颜色系统 | ✅ | 首字母 + 7种彩虹色可选 |
| 🚀 桌面快捷方式 | ✅ | 创建工具桌面快捷方式 |
| 🌐 WebView 沙箱 | ✅ | 独立运行环境，数据隔离 |
| 🔗 JS 原生桥接 | ✅ | 异步 API，不阻塞 UI |
| 🛠️ 内置工具库 | ✅ | `$box` 封装常用交互 |
| 📲 分享导入 | ✅ | 支持从其他应用分享文件导入 |
| 🎨 Material Design | ✅ | 符合 Android 设计规范 |
| 🔧 远程调试 | ✅ | Debug 模式支持 Chrome DevTools |

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- minSdkVersion: 34 (Android 14)
- targetSdkVersion: 34

### 构建项目

1. **克隆或下载项目**
2. **用 Android Studio 打开**
   ```
   File -> Open -> 选择项目根目录
   ```
3. **等待 Gradle 同步完成**
4. **连接设备或启动模拟器**
5. **点击 Run 按钮（▶️）**

### 测试示例工具

**方式一：本地文件导入**
1. 打包 `example/notebook/` 目录为 ZIP 文件
   ```bash
   cd example/notebook
   zip -r ../notebook.zip *
   ```
2. 将 ZIP 文件传输到手机
3. 在应用中点击右上角 "+" 按钮选择文件导入
4. 点击"简易笔记本"卡片打开使用

**方式二：在线下载（推荐）**
1. 点击右上角菜单 → 「在线下载」
2. 输入工具 URL 地址（支持 ZIP 或单 HTML）
3. 自动下载并导入到工具列表

> 💡 **Git 平台智能识别**：
> - 支持 Gitee/GitHub `blob` 链接，自动转换为 `raw` 链接
> - 示例：`https://gitee.com/user/repo/blob/master/tool/index.html`
> - 自动转换为可直接下载的 raw 链接

---

## 🛠️ 开发自己的工具

### 5 分钟快速上手

**方式一：单 HTML 文件（最简单）**

1. 创建一个 `.html` 文件，包含你的所有代码、样式和脚本
2. 直接导入到 ToolBox 即可使用

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的工具</title>
    <style>/* 你的样式 */</style>
</head>
<body>
    <!-- 你的界面 -->
    <h1>Hello ToolBox!</h1>
    <script>
        // 使用 ToolBox API
        $box.toast('工具已加载!');
    </script>
</body>
</html>
```

**方式二：ZIP 工具包（推荐）**

1. 创建目录结构，打包为 ZIP 文件导入

```
my-tool/
├── manifest.json      # 工具元数据
├── index.html         # 主页面
├── style.css          # 样式文件
├── script.js          # 脚本文件
└── assets/            # 资源目录
    └── icon.png       # 工具图标
```

---

### 工具包结构

```
my-tool.zip
├── manifest.json      # 【必填】工具元数据
├── index.html         # 【必填】入口页面
├── style.css          # 可选：样式文件
├── script.js          # 可选：脚本文件
└── assets/            # 可选：资源目录
    ├── icon.png       # 工具图标（建议 96×96px）
    └── ...
```

### manifest.json 规范

```json
{
    "id": "my-unique-tool-id",
    "name": "我的工具",
    "version": "1.0.0",
    "description": "一句话描述工具功能",
    "author": "你的名字",
    "icon": "assets/icon.png",
    "iconColor": "#FF9800",
    "permissions": []
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| id | ✅ | 全局唯一标识符，小写字母和横杠 |
| name | ✅ | 显示名称，建议 2-8 个字符 |
| version | ✅ | 语义化版本号（x.y.z） |
| description | ⭕ | 简短描述 |
| author | ⭕ | 作者名称 |
| icon | ⭕ | 图标相对路径 |
| iconColor | ⭕ | 图标背景颜色，默认 #FF9800 |
| permissions | ⭕ | 预留，未来用于权限声明 |
| downloadUrl | ⭕ | 工具下载链接，用于在线更新检查 |

**可用的图标颜色：**

| 颜色 | 值 | 效果 |
|------|----|------|
| 红 | `#F44336` | 🟥 |
| 橙 | `#FF9800` | 🟧 |
| 黄 | `#FFEB3B` | 🟨 |
| 绿 | `#4CAF50` | 🟩 |
| 蓝 | `#2196F3` | 🟦 |
| 靛 | `#3F51B5` | 🟪 |
| 紫 | `#9C27B0` | 🟣 |

---

## 📚 JavaScript API 文档

### ToolBridge - 文件系统 API

所有 API 都提供异步（推荐）和同步版本。

#### 读取文件

```javascript
// 异步（推荐，不阻塞 UI）
window.ToolBridge.readFile('data/notes.txt', function(content) {
    if (content !== null) {
        console.log('内容:', content);
    } else {
        console.log('文件不存在或读取失败');
    }
});

// 同步（简单场景使用）
const content = window.ToolBridge.readFileSync('data/notes.txt');
```

#### 写入文件

```javascript
// 异步
window.ToolBridge.writeFile('data/notes.txt', 'Hello World', function(success) {
    if (success === 'true') {
        console.log('保存成功');
    }
});

// 同步
const success = window.ToolBridge.writeFileSync('data/notes.txt', 'Hello');
```

#### 列出目录

```javascript
window.ToolBridge.listFiles('data/', function(files) {
    if (files) {
        console.log('文件列表:', files.split(','));
    }
});
```

#### 删除文件

```javascript
window.ToolBridge.deleteFile('data/old.txt', function(success) {
    console.log(success === 'true' ? '已删除' : '删除失败');
});
```

#### 扩展 API 入口（预留）

```javascript
window.ToolBridge.callNative('api_name', { param: 'value' }, function(result) {
    console.log(result);
});
```

---

### $box - 内置工具库

所有工具自动注入，无需手动引入。

#### Toast 提示

```javascript
$box.toast('操作成功');          // 短提示
$box.toast('操作失败，请重试');  // 长提示
```

#### 对话框

```javascript
// Alert 对话框
$box.alert('提示', '操作已完成');

// Confirm 确认对话框
$box.confirm('确认删除？', function(yes) {
    if (yes) {
        // 用户点击确定
    } else {
        // 用户点击取消
    }
});
```

#### 获取工具信息

```javascript
const info = $box.getToolInfo();
console.log(info.id);        // 工具 ID
console.log(info.name);      // 工具名称
console.log(info.version);   // 工具版本
console.log(info.iconColor); // 图标颜色
```

---

## 💡 开发最佳实践

### 1. 数据存储

- ✅ **用户数据存放在 `data/` 目录**
- ✅ 导出工具时 `data/` 目录不会被打包
- ❌ 不要在工具根目录存放临时文件

### 2. 性能优化

- ✅ 优先使用异步 API，避免 UI 卡顿
- ✅ 大文件读写使用异步版本
- ✅ 减少频繁的文件 IO 操作

### 3. 兼容性

- ✅ 使用标准 HTML5/CSS3/ES6
- ✅ 不依赖外部 CDN，所有资源打包进 ZIP
- ✅ 适配深色/浅色主题
- ❌ 不要使用浏览器扩展 API

### 4. 安全性

- ✅ 每个工具只能访问自己目录的文件
- ✅ 沙箱隔离防止路径遍历攻击
- ❌ 不要存放敏感明文信息

---

## 📱 界面设计规范

### 推荐的移动端适配

```css
/* 视口设置 */
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">

/* 推荐基础样式 */
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    padding: 16px;
    margin: 0;
    background: #f5f5f5;
}

/* 触摸优化 */
button {
    min-height: 48px;
    padding: 12px 24px;
    font-size: 16px;
}
```

### 配色参考

| 用途 | 亮色 | 暗色 |
|------|------|------|
| 主色 | `#1976D2` | `#90CAF9` |
| 成功 | `#26A69A` | `#80CBC4` |
| 警告 | `#FFA000` | `#FFE082` |
| 危险 | `#D32F2F` | `#EF9A9A` |
| 背景 | `#F5F5F5` | `#121212` |
| 卡片 | `#FFFFFF` | `#1E1E1E` |

---

## 🏗️ 项目架构

```
ToolBox/
├── app/
│   ├── src/main/
│   │   ├── java/com/toolbox/app/
│   │   │   ├── MainActivity.kt          # 主界面 - 工具网格
│   │   │   ├── ToolActivity.kt          # 工具运行界面
│   │   │   ├── ImportActivity.kt        # 导入工具界面
│   │   │   ├── DownloadActivity.kt      # 在线下载界面
│   │   │   ├── ToolDetailActivity.kt    # 工具详情页
│   │   │   ├── EditToolActivity.kt      # 编辑工具界面
│   │   │   ├── model/
│   │   │   │   └── ToolInfo.kt          # 工具数据模型
│   │   │   ├── data/
│   │   │   │   ├── ToolDirectory.kt     # 工具目录管理
│   │   │   │   ├── ToolManager.kt       # 核心业务逻辑
│   │   │   │   └── ManifestParser.kt    # manifest 解析
│   │   │   ├── ui/
│   │   │   │   └── ToolGridAdapter.kt   # 网格适配器
│   │   │   ├── util/
│   │   │   │   ├── VersionComparator.kt # 版本号比较工具
│   │   │   │   └── ZipUtils.kt          # ZIP 压缩解压工具
│   │   │   └── bridge/
│   │   │       ├── ToolBridge.kt        # 文件系统 JS 桥接
│   │   │       └── BoxNative.kt         # 工具库 JS 桥接
│   │   ├── res/
│   │   │   ├── layout/                  # XML 布局文件
│   │   │   ├── drawable/                # 矢量图标
│   │   │   ├── menu/                    # 菜单资源
│   │   │   └── values/                  # 颜色、字符串、主题
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── example/
│   └── notebook/                         # 示例工具
├── docs/superpowers/
│   ├── specs/                            # 设计规格文档
│   └── plans/                            # 实施计划文档
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📚 开发者指南

### 文件系统 API

工具通过 `window.ToolBridge` 访问文件系统：

```javascript
// 异步读取（推荐）
window.ToolBridge.readFile('data/config.json', function(content) {
    if (content) console.log(JSON.parse(content));
});

// 异步写入
window.ToolBridge.writeFile('data/notes.txt', 'Hello', function(success) {
    console.log(success === 'true' ? '已保存' : '失败');
});

// 同步 API（简单场景使用）
const content = window.ToolBridge.readFileSync('data/file.txt');
const success = window.ToolBridge.writeFileSync('data/file.txt', 'content');
```

### $box 工具库

所有工具自动注入，可直接使用：

```javascript
$box.toast('操作成功');                       // Toast 提示

$box.alert('标题', '消息内容', callback);      // Alert 对话框

$box.confirm('确认删除？', function(yes) {     // 确认对话框
    if (yes) { /* 已确认 */ }
});

const info = $box.getToolInfo();               // 获取工具信息
// info.id, info.name, info.version
```

### 界面设计建议

```html
<!-- 必须设置的视口 -->
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">

<!-- 推荐的字体栈 -->
<style>
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
                 'Helvetica Neue', Arial, sans-serif;
}
button {
    min-height: 48px;      /* 触摸友好 */
    cursor: pointer;
}
</style>
```

### Chrome 远程调试

Debug 版本支持远程调试：
1. 手机用 USB 连接电脑
2. 打开 ToolBox 和你的工具
3. 在电脑 Chrome 中打开 `chrome://inspect`
4. 找到 WebView，点击 Inspect

---

## 🧪 运行测试

### 单元测试

```bash
./gradlew testDebugUnitTest
```

### Instrumented 测试

```bash
./gradlew connectedDebugAndroidTest
```

### 手动测试清单

- [ ] 应用启动不崩溃
- [ ] 空状态显示正确
- [ ] ZIP 工具包导入成功
- [ ] 单 HTML 文件导入成功
- [ ] 工具可以正常打开
- [ ] JS 文件读写 API 正常
- [ ] Toast/Alert 弹窗正常
- [ ] 工具导出功能正常
- [ ] 删除工具功能正常
- [ ] 编辑工具功能正常
- [ ] 桌面快捷方式正常
- [ ] 横竖屏切换不崩溃

---

## 🔮 未来计划

### v1.2 目标

- [ ] 深色模式适配
- [ ] 工具分类和搜索
- [ ] 导出时可选是否包含数据
- [ ] 工具排序

### v1.2 已完成 ✅

- [x] 在线下载功能 - 支持从 URL 下载 ZIP/HTML 工具
- [x] 工具详情页 - 查看信息展示、下载链接管理
- [x] 版本检查更新 - 一键检测并更新工具
- [x] Git 平台智能识别 - Gitee/GitHub blob 自动转 raw

### v1.3 目标

- [ ] 深色模式适配
- [ ] 工具分类和搜索
- [ ] 导出时可选是否包含数据
- [ ] 工具排序

### v2.0 目标

- [ ] 更多原生 API 桥接（相机、剪贴板、通知）
- [ ] 工具市场和在线更新
- [ ] 插件化扩展系统
- [ ] 备份与恢复功能

---

## 📄 许可证

MIT License

---

## 📋 更新日志

详细的版本变更记录请查看 [docs/CHANGELOG.md](docs/CHANGELOG.md)

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## ❓ 常见问题

### Q: 支持哪些 Android 版本？
A: 最低支持 Android 14 (API 34)，因为使用了最新的 API 和安全特性。

### Q: 可以使用 Vue/React 等框架开发工具吗？
A: 可以！只要打包成单 HTML 文件或 ZIP 包即可运行。

### Q: 工具数据会丢失吗？
A: 数据存储在应用私有目录，只要不卸载应用或清除数据就不会丢失。

### Q: 如何调试工具中的 JavaScript？
A: Debug 版本开启了远程调试，用 Chrome 访问 `chrome://inspect` 即可连接调试。

### Q: 如何修改工具的图标颜色？
A: 长按工具卡片，选择"编辑"，在编辑页面选择喜欢的颜色即可。

### Q: 在线下载支持哪些链接？
A: 支持：
- 直接的 ZIP/HTML 文件链接
- Gitee blob 链接（自动转 raw）
- GitHub blob 链接（自动转 raw）
- 所有返回 ZIP 或 HTML 内容的 HTTP/HTTPS 链接

### Q: 下载的文件保存在哪里？
A: 下载的临时文件在导入成功后会自动删除，工具最终安装在应用的私有目录下，数据沙箱隔离。

### Q: 如何为已有的工具设置下载链接？
A: 长按工具 → 选择「详情」→ 在下载链接输入框中填写 URL → 点击保存按钮。之后就可以点击「检查更新」来更新工具。

### Q: 在线更新的原理是什么？
A: 检查更新时，系统会下载远程文件，解压后读取 manifest.json 中的 version 字段，与本地版本比较。如果远程版本更新，会提示更新。
