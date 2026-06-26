# 更新日志

本项目的所有重要变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范。

---

## [Unreleased]

### 已计划
- [ ] 深色模式切换
- [ ] 工具分类和搜索
- [ ] 剪贴板访问 API
- [ ] 相机访问 API
- [ ] 通知 API
- [ ] 批量导出/导入
- [ ] 工具排序
- [ ] 示例工具内置

---

## [1.1.0] - 2026-06-26

### ✨ 新增
- 🌈 **图标颜色系统** - 工具首字母 + 纯色圆形背景，支持7种彩虹色
  - 红 🟥 #F44336
  - 橙 🟧 #FF9800 (默认)
  - 黄 🟨 #FFEB3B
  - 绿 🟩 #4CAF50
  - 蓝 🟦 #2196F3
  - 靛 🟪 #3F51B5
  - 紫 🟣 #9C27B0
- 📝 **工具编辑功能** - 长按工具选择"编辑"，可修改所有信息：
  - 修改工具名称
  - 修改版本号
  - 修改作者信息
  - 修改描述
  - 重新选择图标颜色
- 🚀 **桌面快捷方式**
  - 支持创建工具桌面快捷方式
  - 使用 Android 8.0+ ShortcutManager API
  - 动态生成快捷方式图标（首字母 + 颜色）
- 💬 **JS 对话框优化**
  - `$box.alert()` 和 `$box.confirm()` 对话框去掉默认标题
  - 不再显示 "The page at file:// says:"
  - 更纯净的原生对话框体验

### 🔧 改进
- 导入页面自动生成工具 ID（UUID 前8位），用户无需手动输入
- ToolInfo 数据模型新增 `iconColor` 字段
- ManifestParser 支持解析和写入 `iconColor` 字段
- ToolManager 新增 `updateToolInfo()` 方法
- WebView 加载时添加详细的日志输出，便于调试
- 所有 UI 更新强制在主线程执行，避免崩溃

### 🐛 修复
- ✅ JS 回调参数特殊字符转义问题（单引号、反斜杠）
- ✅ 沙箱路径比较失败问题（Android 多用户 `/data/data/` vs `/data/user/0/`）
- ✅ `renameTo()` 跨目录静默失败问题（改用 `copyRecursively()`）
- ✅ 导入大文件时的内存溢出风险
- ✅ 网格列表刷新不及时问题

### 🎨 设计
- 工具图标：64dp × 64dp 首字母 + 纯色圆角背景
- 颜色选择器：圆形色块，选中后有蓝色边框
- 导入页面：颜色选择区域在表单顶部
- Material Design 3 风格的圆角卡片（16dp）

---

## [1.0.0] - 2024-06-26

### ✨ 新增
- 完整的 Android 项目脚手架
- 工具网格主页（3列布局）
- 长按操作菜单
- ZIP 工具包导入支持
- 单 HTML 文件导入支持
- 系统分享导入支持
- 工具导出功能
- WebView 沙箱运行环境
- Chrome 远程调试支持
- ToolBridge 文件系统 API
  - `readFile(path, callback)` - 异步读取
  - `writeFile(path, content, callback)` - 异步写入
  - `listFiles(path, callback)` - 列出目录
  - `deleteFile(path, callback)` - 删除文件
  - `readFileSync(path)` - 同步读取
  - `writeFileSync(path, content)` - 同步写入
- $box 内置工具库
  - `$box.toast(message)` - Toast 提示
  - `$box.alert(title, message)` - 对话框
  - `$box.confirm(title, message, callback)` - 确认对话框
  - `$box.getToolInfo()` - 获取工具信息
- 沙箱安全机制（路径遍历防护）
- Material Design 3 界面风格
- 空状态引导界面
- 单元测试框架配置
- 示例工具：简易笔记本

### 🎨 设计
- 48dp 最小点击区域
- 8dp 基准间距系统
- 16dp 卡片圆角
- 20dp 按钮圆角
- 1dp-3dp 阴影层级
- 蓝色主题配色方案

### 📚 文档
- 完整的 README.md
- 详细的开发者指南 (DEVELOPER_GUIDE.md)
- 设计规格文档
- 实施计划文档

---

## 版本说明

### 版本号格式
`主版本号.次版本号.修订号`

- **主版本号**: 不兼容的 API 变更
- **次版本号**: 向下兼容的功能性新增
- **修订号**: 向下兼容的问题修正

### 版本发布节奏
- 小版本：1-2 周
- 中版本：1-2 月
- 大版本：按需发布

---

## 贡献指南

### 提交信息格式

```
<类型>(<范围>): <简短描述>

<详细描述>

<关联的 issue 或 PR>
```

**类型:**
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构（既不是新增功能，也不是修 bug）
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

**示例:**
```
feat(bridge): 添加剪贴板访问 API

新增 $box.getClipboard() 和 $box.setClipboard() 方法

Closes #123
```

---

## 兼容性承诺

### JS API 兼容性

- 补丁版本变更：100% 向后兼容
- 次版本变更：向后兼容，新增 API
- 主版本变更：可能有不兼容变更，提供迁移指南

### 工具格式兼容性

- v1.0 的工具包格式在 v2.0 仍然可用
- 旧格式自动升级到新格式

---

## 已知问题

### v1.1.0
- [ ] 不支持嵌套目录的 `listFiles`
- [ ] 导入大文件时没有进度提示
- [ ] 横屏布局需要优化
- [ ] 删除工具后不显示空状态

---

## 历史版本

本项目从 v1.0.0 开始，之前的版本为原型设计阶段。
