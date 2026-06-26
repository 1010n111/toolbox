# 快速开始指南

5 分钟内创建你的第一个 ToolBox 工具！

---

## 🚀 5 分钟快速上手

### 第 1 步：创建 HTML 文件

新建一个 `hello-tool.html` 文件：

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的第一个工具</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            padding: 24px;
            background: #f5f5f5;
        }
        .card {
            background: white;
            padding: 24px;
            border-radius: 16px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        h1 {
            color: #1976D2;
            margin-bottom: 16px;
            font-size: 24px;
        }
        input {
            width: 100%;
            padding: 12px 16px;
            font-size: 16px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            margin-bottom: 16px;
            outline: none;
        }
        input:focus {
            border-color: #1976D2;
        }
        button {
            width: 100%;
            padding: 14px;
            font-size: 16px;
            font-weight: 500;
            color: white;
            background: #1976D2;
            border: none;
            border-radius: 20px;
            cursor: pointer;
            transition: background 0.2s;
        }
        button:hover {
            background: #1565C0;
        }
        .message {
            margin-top: 16px;
            padding: 12px;
            background: #E3F2FD;
            border-radius: 8px;
            color: #1565C0;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="card">
        <h1>👋 你好，ToolBox!</h1>
        <input type="text" id="nameInput" placeholder="输入你的名字">
        <button onclick="sayHello()">打个招呼</button>
        <div id="message" class="message" style="display: none;"></div>
    </div>

    <script>
        function sayHello() {
            const name = document.getElementById('nameInput').value.trim();
            const messageEl = document.getElementById('message');
            
            if (name) {
                messageEl.textContent = `你好，${name}！🎉`;
                messageEl.style.display = 'block';
                
                // 使用 ToolBox API 保存数据
                if (window.ToolBridge) {
                    window.ToolBridge.writeFile('data/last_user.txt', name, function(success) {
                        if (success === 'true') {
                            $box.toast('已记住你的名字！');
                        }
                    });
                }
            } else {
                $box.toast('请输入你的名字！');
            }
        }

        // 页面加载时读取上次保存的名字
        window.onload = function() {
            if (window.ToolBridge) {
                window.ToolBridge.readFile('data/last_user.txt', function(content) {
                    if (content) {
                        document.getElementById('nameInput').value = content;
                    }
                });
            }
        };
    </script>
</body>
</html>
```

### 第 2 步：导入到 ToolBox

1. 将 `hello-tool.html` 传输到手机
2. 打开 ToolBox 应用
3. 点击右上角 "+" 按钮
4. 选择 `hello-tool.html` 文件
5. 导入成功！工具会出现在网格中

### 第 3 步：运行测试

1. 点击刚导入的工具卡片
2. 在输入框中输入你的名字
3. 点击"打个招呼"按钮
4. 看到问候消息！
5. 关闭工具再重新打开——你的名字被记住了！🎉

---

## 📦 创建 ZIP 工具包（进阶）

当你的工具变得更复杂时，可以打包成 ZIP 格式：

### 目录结构

```
hello-tool/
├── manifest.json      # 工具元数据
├── index.html         # 主页面
├── style.css          # 样式文件
├── script.js          # 脚本文件
└── assets/
    └── icon.png       # 工具图标
```

### manifest.json

```json
{
    "id": "hello-tool",
    "name": "你好工具",
    "version": "1.0.0",
    "description": "我的第一个 ToolBox 工具",
    "author": "你的名字",
    "icon": "assets/icon.png",
    "permissions": []
}
```

### 打包命令

```bash
cd hello-tool
zip -r ../hello-tool.zip *
```

---

## 💡 更多示例

### 示例 1：简单的计数器

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>计数器</title>
    <style>
        body {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 80vh;
            background: #f5f5f5;
            font-family: sans-serif;
        }
        .count {
            font-size: 72px;
            font-weight: bold;
            color: #1976D2;
        }
        .buttons {
            display: flex;
            gap: 16px;
            margin-top: 24px;
        }
        button {
            width: 64px;
            height: 64px;
            font-size: 24px;
            border: none;
            border-radius: 50%;
            cursor: pointer;
        }
        .btn-add { background: #4CAF50; color: white; }
        .btn-sub { background: #F44336; color: white; }
    </style>
</head>
<body>
    <div class="count" id="count">0</div>
    <div class="buttons">
        <button class="btn-sub" onclick="update(-1)">-</button>
        <button class="btn-add" onclick="update(1)">+</button>
    </div>

    <script>
        let count = 0;
        
        function update(delta) {
            count += delta;
            document.getElementById('count').textContent = count;
            
            // 自动保存
            if (window.ToolBridge) {
                window.ToolBridge.writeFileSync('data/count.txt', count.toString());
            }
        }
        
        // 加载保存的数值
        window.onload = function() {
            if (window.ToolBridge) {
                const saved = window.ToolBridge.readFileSync('data/count.txt');
                if (saved) count = parseInt(saved);
                document.getElementById('count').textContent = count;
            }
        };
    </script>
</body>
</html>
```

---

### 示例 2：随机密码生成器

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>密码生成器</title>
    <style>
        body {
            font-family: sans-serif;
            padding: 24px;
            background: #f5f5f5;
        }
        .result {
            background: white;
            padding: 16px;
            border-radius: 8px;
            font-family: monospace;
            font-size: 18px;
            word-break: break-all;
            margin-bottom: 16px;
            border: 2px solid #e0e0e0;
        }
        button {
            width: 100%;
            padding: 14px;
            background: #1976D2;
            color: white;
            border: none;
            border-radius: 20px;
            font-size: 16px;
            cursor: pointer;
        }
    </style>
</head>
<body>
    <div class="result" id="password">点击生成</div>
    <button onclick="generate()">🔐 生成密码</button>

    <script>
        function generatePassword(length = 16) {
            const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*';
            let result = '';
            for (let i = 0; i < length; i++) {
                result += chars.charAt(Math.floor(Math.random() * chars.length));
            }
            return result;
        }
        
        function generate() {
            const password = generatePassword();
            document.getElementById('password').textContent = password;
            $box.toast('密码已生成！');
        }
    </script>
</body>
</html>
```

---

## 🎯 下一步

1. ✅ 完成第一个工具
2. 📚 阅读 [API 参考手册](DEVELOPER_GUIDE.md#api-参考手册)
3. 🎨 学习 [界面设计指南](DEVELOPER_GUIDE.md#界面设计指南)
4. 🛠️ 尝试更复杂的工具
5. 💡 分享你的工具给朋友！

---

## ❓ 需要帮助？

- 查看 [README.md](../README.md) 了解平台功能
- 阅读 [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) 获取详细文档
- 参考 `example/notebook/` 目录下的示例工具
