# 开发者指南

本文档详细介绍如何为 ToolBox 平台开发工具，以及如何参与平台本身的开发。

---

## 📦 工具开发快速开始

### 方式一：单 HTML 文件（最简单）

1. 创建一个 HTML 文件
2. 在 `<head>` 中添加元数据
3. 编写你的工具逻辑
4. 直接导入 `.html` 文件到 ToolBox

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的工具</title>
    <style>
        /* 你的样式 */
    </style>
</head>
<body>
    <!-- 你的界面 -->
    <h1>Hello ToolBox!</h1>
    
    <script>
        // 使用 ToolBridge API
        $box.toast('工具已加载!');
    </script>
</body>
</html>
```

### 方式二：ZIP 工具包（推荐）

1. 创建以下目录结构：
   ```
   my-tool/
   ├── manifest.json
   ├── index.html
   ├── style.css
   ├── script.js
   └── assets/
       └── icon.png
   ```

2. 打包为 ZIP 文件
3. 导入到 ToolBox

---

## 🎯 API 参考手册

### 文件系统 API

#### `ToolBridge.readFile(path, callback)`

异步读取文件内容。

**参数:**
- `path` (string): 文件相对路径
- `callback` (function): 回调函数，参数为文件内容（失败时为 null）

**示例:**
```javascript
window.ToolBridge.readFile('data/config.json', function(content) {
    if (content) {
        const config = JSON.parse(content);
        console.log(config);
    }
});
```

---

#### `ToolBridge.writeFile(path, content, callback)`

异步写入文件内容。

**参数:**
- `path` (string): 文件相对路径
- `content` (string): 要写入的内容
- `callback` (function): 回调函数，参数为 'true' 或 'false'

**示例:**
```javascript
window.ToolBridge.writeFile('data/notes.txt', 'Hello World', function(success) {
    if (success === 'true') {
        $box.toast('保存成功');
    }
});
```

---

#### `ToolBridge.listFiles(path, callback)`

列出目录下的文件。

**参数:**
- `path` (string): 目录相对路径
- `callback` (function): 回调函数，参数为逗号分隔的文件名列表

**示例:**
```javascript
window.ToolBridge.listFiles('data/', function(files) {
    if (files) {
        console.log('文件列表:', files.split(','));
    }
});
```

---

#### `ToolBridge.deleteFile(path, callback)`

删除文件或目录。

**参数:**
- `path` (string): 文件或目录路径
- `callback` (function): 回调函数，参数为 'true' 或 'false'

**示例:**
```javascript
window.ToolBridge.deleteFile('data/old.txt', function(success) {
    console.log(success === 'true' ? '已删除' : '删除失败');
});
```

---

### 同步 API（简单场景）

```javascript
// 读取
const content = window.ToolBridge.readFileSync('data/file.txt');

// 写入
const success = window.ToolBridge.writeFileSync('data/file.txt', 'content');
```

---

### $box 工具库 API

所有工具自动注入，可直接使用。

#### `$box.toast(message)`

显示 Toast 提示。

```javascript
$box.toast('操作成功');
```

---

#### `$box.alert(title, message, callback?)`

显示 Alert 对话框。

```javascript
$box.alert('提示', '操作已完成', function() {
    // 用户点击确定后的回调（可选）
});
```

---

#### `$box.confirm(title, message, callback?)`

显示 Confirm 确认对话框。

```javascript
$box.confirm('确认删除', '确定要删除这条记录吗？', function(yes) {
    if (yes) {
        // 用户点击确定
    } else {
        // 用户点击取消
    }
});
```

---

#### `$box.getToolInfo()`

获取当前工具的元信息。

```javascript
const info = $box.getToolInfo();
console.log(info.id);      // 工具 ID
console.log(info.name);    // 工具名称
console.log(info.version); // 工具版本号
```

---

## 🎨 界面设计指南

### 移动端适配

**必须设置的视口:**
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
```

**推荐的字体栈:**
```css
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
                 'Helvetica Neue', Arial, sans-serif;
}
```

---

### 触摸友好

- 所有可点击元素最小高度 `48px`
- 按钮之间最小间距 `8dp`
- 使用 `cursor: pointer` 提高可点击感知

```css
button {
    min-height: 48px;
    padding: 12px 24px;
    font-size: 16px;
    border-radius: 20px;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
}
```

---

### 配色方案

建议使用与平台一致的配色：

```css
/* 亮色模式 */
:root {
    --primary: #1976D2;
    --primary-dark: #1565C0;
    --secondary: #26A69A;
    --background: #F5F5F5;
    --surface: #FFFFFF;
    --text-primary: #212121;
    --text-secondary: #757575;
    --divider: #E0E0E0;
    --error: #D32F2F;
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
    :root {
        --primary: #90CAF9;
        --secondary: #80CBC4;
        --background: #121212;
        --surface: #1E1E1E;
        --text-primary: #FFFFFF;
        --text-secondary: #B0B0B0;
        --divider: #424242;
        --error: #EF9A9A;
    }
}
```

---

### 间距系统

使用 8dp 基准网格：

```css
:root {
    --spacing-xs: 4px;
    --spacing-sm: 8px;
    --spacing-md: 16px;
    --spacing-lg: 24px;
    --spacing-xl: 32px;
}
```

---

## 🔧 调试技巧

### Chrome 远程调试

1. 在手机上安装并打开 ToolBox (Debug 版本)
2. 用 USB 连接手机和电脑
3. 在电脑 Chrome 中打开 `chrome://inspect`
4. 找到你的 WebView，点击 "Inspect"
5. 就像调试普通网页一样调试工具！

### 调试技巧

```javascript
// 检查 API 是否可用
if (typeof window.ToolBridge === 'undefined') {
    console.log('不在 ToolBox 环境中，使用本地存储模拟');
    // 使用 localStorage 模拟
}

// 开发环境模拟
if (!window.ToolBridge) {
    window.ToolBridge = {
        readFile: function(path, cb) {
            cb(localStorage.getItem(path));
        },
        writeFile: function(path, content, cb) {
            localStorage.setItem(path, content);
            cb('true');
        }
    };
}
```

---

## 📝 工具开发最佳实践

### 1. 数据持久化

```javascript
// ✅ 好的做法：data 目录存放用户数据
window.ToolBridge.writeFile('data/user-settings.json', JSON.stringify(settings));

// ❌ 不要在工具根目录存放临时文件
window.ToolBridge.writeFile('temp.txt', '...'); // 不推荐
```

### 2. 错误处理

```javascript
// ✅ 总是处理错误情况
window.ToolBridge.readFile('data/config.json', function(content) {
    if (!content) {
        // 文件不存在，使用默认配置
        initDefaultConfig();
        return;
    }
    try {
        const config = JSON.parse(content);
        // 使用配置
    } catch (e) {
        console.error('配置解析失败', e);
    }
});
```

### 3. 性能考虑

```javascript
// ✅ 防抖：避免频繁写入
let saveTimeout;
function autoSave() {
    clearTimeout(saveTimeout);
    saveTimeout = setTimeout(() => {
        saveData();
    }, 500);
}

// ✅ 大文件使用异步 API
window.ToolBridge.writeFile('data/large.json', bigData, function() {
    // 写入完成
});
```

### 4. 资源管理

- ✅ 所有资源使用相对路径
- ✅ 不要依赖外部 CDN（离线无法使用）
- ✅ 压缩图片和静态资源
- ✅ 图标使用 SVG，自适应分辨率

---

## 🏗️ 平台开发指南

> 这部分是为想要改进 ToolBox 平台本身的开发者准备的。

### 代码规范

#### Kotlin 代码风格

```kotlin
// ✅ 使用标准命名规范
class ToolManager(private val context: Context) {
    
    // 公开方法使用 PascalCase
    fun installTool(uri: Uri): Result<ToolInfo> {
        // 实现
    }
    
    // 私有方法使用 camelCase
    private fun validateManifest(manifest: JSONObject): Boolean {
        // 实现
    }
}

// ✅ 数据类放在单独的 model 包
data class ToolInfo(
    val id: String,
    val name: String,
    val version: String
)
```

#### 架构原则

- **单一职责**: 每个类只做一件事
- **依赖注入**: 依赖通过构造函数传入，便于测试
- **错误处理**: 使用 `Result<T>` 而非抛出异常
- **线程安全**: IO 操作在后台线程执行

---

### 添加新的 JS API

1. 在 `ToolBridge.kt` 中添加新方法：

```kotlin
@JavascriptInterface
fun newApi(param: String, callbackId: String) {
    Thread {
        try {
            val result = doSomething(param)
            callbackHandler(callbackId, result)
        } catch (e: Exception) {
            callbackHandler(callbackId, null)
        }
    }.start()
}
```

2. 在 `ToolActivity.kt` 的 JS wrapper 中添加：

```javascript
window.ToolBridge.newApi = function(param, callback) {
    var id = ++_callbackId;
    _callbacks[id] = callback;
    window.ToolBridgeNative.newApi(param, id);
};
```

---

### 测试

#### 运行单元测试

```bash
./gradlew testDebugUnitTest
```

#### 添加新测试

```kotlin
@Test
fun `test new functionality`() {
    // 准备测试数据
    val input = "test"
    
    // 执行
    val result = classUnderTest.method(input)
    
    // 验证
    assertTrue(result.isSuccess)
    assertEquals("expected", result.getOrNull())
}
```

---

## 📋 工具审核清单

提交工具前请检查：

- [ ] manifest.json 格式正确，必填字段完整
- [ ] index.html 可以正常加载
- [ ] 所有资源使用相对路径
- [ ] 不依赖外部 CDN
- [ ] JS API 使用正确（有错误处理）
- [ ] 适配移动端触摸操作
- [ ] 点击区域不小于 48dp
- [ ] 提供 96×96px 的图标
- [ ] 不包含恶意代码

---

## 🆘 常见问题

### Q: 为什么我的工具导入后打不开？

**可能原因:**
1. manifest.json 格式错误
2. index.html 文件名大小写不匹配
3. ZIP 包包含中文文件名（建议使用英文）
4. 文件路径使用了绝对路径

**解决方法:**
- 使用 JSON 校验工具检查 manifest.json
- 确认 ZIP 根目录下有 index.html
- 所有文件使用英文命名

---

### Q: JS API 调用没有反应？

**检查:**
1. 是否在页面加载完成后调用 API
2. 是否拼写错误（注意大小写）
3. 检查 Chrome DevTools Console 报错
4. 路径是否正确（相对路径，不要以 `/` 开头）

---

### Q: 保存的数据在哪里？

数据存储在应用私有目录：
```
/data/data/com.toolbox.app/tools/[tool-id]/data/
```

只有当前工具可以访问这个目录。

---

## 📚 相关资源

- [Material Design 3 指南](https://m3.material.io/)
- [Android WebView 文档](https://developer.android.com/reference/android/webkit/WebView)
- [Chrome 远程调试](https://developer.chrome.com/docs/devtools/remote-debugging/)
