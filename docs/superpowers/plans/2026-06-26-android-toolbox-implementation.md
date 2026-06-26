# Android 工具平台实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个Android插件化工具平台，支持HTML工具的导入、导出和运行，提供JS桥接原生能力

**Architecture:** 三层架构 - Kotlin原生底座 + WebView容器 + 异步JS桥接层。工具以ZIP包或单HTML格式分发，在隔离沙箱中运行。

**Tech Stack:** Kotlin, Android SDK (API 34, Android 14+), System WebView, Material Components, 无第三方依赖

---

## 📊 当前进度

- **v1.0 MVP:** 所有核心功能已实现 ✅
- **v1.1 图标与编辑功能:** 全部完成 ✅
- **当前阶段:** 功能稳定阶段

---

## 全局约束

- minSdkVersion: 34 (Android 14)
- targetSdkVersion: 34 (最新稳定版)
- 语言: Kotlin
- 第三方依赖: 除Material Components外无其他依赖
- 所有JS API必须提供异步回调版本，不阻塞主线程
- WebView默认开启远程调试支持 (Debug模式)
- 严格沙箱隔离：每个工具只能访问自己目录下的文件
- **必须使用 canonicalFile 进行路径规范化检查**

---

## 🎨 UI/UX 设计规范约束

### 1. 配色方案 (Material 3)

| 类别 | 亮色模式 | 暗色模式 | 用途 |
|------|---------|---------|------|
| Primary | `#1976D2` | `#90CAF9` | 主按钮、高亮、选中状态 |
| Secondary | `#26A69A` | `#80CBC4` | 次要操作、成功状态 |
| Danger | `#D32F2F` | `#EF9A9A` | 删除、危险操作 |
| Surface | `#FFFFFF` | `#121212` | 页面背景 |
| Surface 变体 | `#F5F5F5` | `#1E1E1E` | 卡片背景 |
| 主文本 | `#212121` | `#FFFFFF` | 标题、正文 |
| 次文本 | `#757575` | `#B0B0B0B0` | 说明文字 |
| 分割线 | `#E0E0E0` | `#424242` | 边框、分隔线 |

**要求**: 所有文本对比度 ≥ 4.5:1 (WCAG AA 标准)

### 2. 字体排版 (Material 3)

| 样式 | 字号 | 字重 | 行高 |
|------|------|------|------|
| Headline Medium | 28sp | 500 | 36sp |
| Title Medium | 16sp | 500 | 24sp |
| Body Medium | 14sp | 400 | 20sp |
| Label Medium | 12sp | 500 | 16sp |

### 3. 间距系统 (8dp 基准)

- xs: 4dp (图标内边距)
- sm: 8dp (网格间距、卡片内边距)
- md: 16dp (页面水平边距、段落间距)
- lg: 24dp (大区块间距)
- xl: 32dp (底部安全区)

### 4. 触摸目标规范

- 所有可点击元素最小触摸区域: **48dp × 48dp**
- 触摸目标之间间距 ≥ 8dp
- 使用 `android:touchTargetSize` 或 `padding` 扩展点击区域

### 5. 动画与交互

- 微动画时长: 150-300ms
- 页面转场: 250-300ms
- 按下效果: Ripple 波纹 + 透明度变化
- 禁用状态: 不透明度 0.38

### 6. 无障碍规范

- 所有图标按钮必须有 `contentDescription`
- 支持动态字体缩放
- 键盘导航顺序与视觉顺序一致
- 色彩不是唯一的信息传达方式
- 使用 `WindowInsetsCompat` 处理系统栏 inset

### 7. 组件规范

**工具卡片**:
- 圆角: 16dp
- 阴影: elevation = 1dp
- 按下: elevation = 3dp + 轻微缩放
- 图标: 64dp × 64dp 首字母 + 纯色背景

**颜色选择器**:
- 颜色块: 40dp × 40dp
- 圆角: 完整圆形
- 选中状态: 3dp 蓝色边框

**对话框**:
- 圆角: 28dp
- 按钮右对齐
- 危险操作使用红色

**主按钮**:
- 最小高度: 48dp
- 圆角: 20dp (pill shape)
- 内边距: 水平 24dp, 垂直 12dp

---

## ✅ 里程碑 1-6: 已完成 (2026-06-26)

所有核心功能已实现并编译通过：

| 功能模块 | 状态 | 文件位置 |
|---------|------|--------|
| 项目脚手架 & 基础结构 | ✅ 完成 | 全部 build.gradle.kts, AndroidManifest |
| ToolManager 核心功能 | ✅ 完成 | app/src/main/java/com/toolbox/app/data/ |
| 主界面 & 工具列表 | ✅ 完成 | MainActivity.kt, ToolGridAdapter.kt |
| WebView & JS桥接层 | ✅ 完成 | ToolActivity.kt, bridge/ToolBridge.kt, bridge/BoxNative.kt |
| 导出功能 | ✅ 完成 | MainActivity.exportTool() |
| ProGuard 规则 | ✅ 完成 | proguard-rules.pro |
| 桌面快捷方式 | ✅ 完成 | MainActivity.createShortcut() |
| JS 回调转义修复 | ✅ 完成 | ToolActivity.kt: 修复单引号/反斜杠转义 |
| 沙箱路径规范化修复 | ✅ 完成 | ToolBridge.resolvePath(): 使用 canonicalFile 比较 |

---

## ✅ 里程碑 7: 图标与编辑功能 (已完成)

### Task 7.1: 图标颜色系统 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/model/ToolInfo.kt` (新增 iconColor 字段)
- ✅ Update: `app/src/main/java/com/toolbox/app/data/ManifestParser.kt` (解析 iconColor)
- ✅ Update: `app/src/main/java/com/toolbox/app/data/ToolManager.kt` (updateToolInfo 方法)
- ✅ Update: `app/src/main/java/com/toolbox/app/ui/ToolGridAdapter.kt` (渲染颜色图标)

**实现功能:**
- ✅ ToolInfo 数据模型新增 `iconColor: String` 字段
- ✅ ManifestParser 支持解析 iconColor 字段（默认 #FF9800）
- ✅ 网格列表显示首字母 + 纯色圆角背景
- ✅ 7 种彩虹色：红/橙/黄/绿/蓝/靛/紫

### Task 7.2: 导入页面颜色选择器 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/res/layout/activity_import.xml`
- ✅ Update: `app/src/main/java/com/toolbox/app/ImportActivity.kt`

**实现功能:**
- ✅ 导入页面顶部显示 7 个颜色选择块
- ✅ 点击颜色块设置工具图标颜色
- ✅ 选中的颜色有蓝色边框高亮
- ✅ 颜色保存到 manifest.json 的 iconColor 字段

### Task 7.3: 工具编辑页面 ✅ 完成

**Files:**
- ✅ Create: `app/src/main/java/com/toolbox/app/EditToolActivity.kt`
- ✅ Create: `app/src/main/res/layout/activity_edit_tool.xml`
- ✅ Update: `app/src/main/AndroidManifest.xml`

**实现功能:**
- ✅ 加载现有工具信息到表单
- ✅ 支持修改工具名称
- ✅ 支持修改版本号
- ✅ 支持修改作者
- ✅ 支持修改描述
- ✅ 支持重新选择图标颜色
- ✅ 保存后自动刷新主界面列表

### Task 7.4: 长按菜单新增编辑选项 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/MainActivity.kt`

**实现功能:**
- ✅ 长按菜单新增"编辑"选项
- ✅ 点击打开 EditToolActivity
- ✅ 使用 ActivityResult 处理编辑结果

### Task 7.5: 桌面快捷方式动态图标 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/MainActivity.kt`

**实现功能:**
- ✅ `createToolIconBitmap()` 方法动态生成快捷方式图标
- ✅ 图标包含首字母和工具对应颜色背景
- ✅ 支持 Adaptive Icon 规范

### Task 7.6: JS 对话框优化 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/ToolActivity.kt`

**实现功能:**
- ✅ 自定义 WebChromeClient
- ✅ 重写 `onJsAlert()` 移除默认标题
- ✅ 重写 `onJsConfirm()` 移除默认标题
- ✅ 不再显示 "The page at file:// says:"
- ✅ 保留原生对话框样式

---

## ✅ 里程碑 8: 导入页面重构 (已完成)

### Task 8.1: 自动生成工具 ID ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/ImportActivity.kt`

**实现功能:**
- ✅ 使用 UUID 前 8 位自动生成工具 ID
- ✅ ID 输入框设置为不可编辑
- ✅ 用户无需手动输入 ID

### Task 8.2: 真实文件名提取 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/ImportActivity.kt`

**实现功能:**
- ✅ 使用 ContentResolver 查询 OpenableColumns.DISPLAY_NAME
- ✅ 不再使用 Uri.lastPathSegment（返回内部路径）
- ✅ 正确显示导入的文件名

### Task 8.3: 跨目录文件复制修复 ✅ 完成

**Files:**
- ✅ Update: `app/src/main/java/com/toolbox/app/data/ToolManager.kt`

**修复问题:**
- ✅ `renameTo()` 在不同目录间静默失败
- ✅ 改用 `copyRecursively()` 复制文件
- ✅ 复制完成后删除临时目录
- ✅ 添加详细日志便于调试

---

## ✅ 验收标准 (v1.1)

| 功能 | 状态 | 验证方式 |
|------|------|---------|
| 项目编译 | ✅ | `./gradlew assembleDebug` 成功 |
| 应用启动 | ✅ | 在设备上正常启动，无崩溃 |
| 单元测试 | ✅ | 全部通过 |
| 图标颜色系统 | ✅ | 工具显示首字母 + 纯色背景 |
| 颜色选择器 | ✅ | 导入/编辑页面可选择7种颜色 |
| 工具编辑 | ✅ | 可修改名称、版本、描述、颜色 |
| 桌面快捷方式 | ✅ | 动态生成正确颜色的图标 |
| JS 对话框 | ✅ | 无多余的默认标题 |
| 无Manifest导入 | ✅ | 用户可手动填写工具信息 |
| HTML/ZIP导入 | ✅ | 可导入单HTML或ZIP格式 |
| 工具运行 | ✅ | WebView正常渲染HTML页面 |
| 文件读写 | ✅ | JS桥接API可读写文件 (已修复沙箱问题) |
| $box 工具库 | ✅ | toast/alert/confirm 可用 |
| 工具导出 | ✅ | 可导出工具为ZIP文件 |
| 工具删除 | ✅ | 可删除已安装工具 |
| 空状态 | ✅ | 无工具时显示友好提示 |
| 沙箱隔离 | ✅ | 工具间数据隔离 |

---

## 🐛 已修复 Bug 汇总 (2026-06-26)

| # | 问题描述 | 根因 | 修复方案 | 状态 |
|---|---------|------|---------|------|
| 1 | mipmap/ic_launcher 资源未找到 | Adaptive Icon 资源缺失 | 创建 ic_launcher_simple 矢量图标 | ✅ 已修复 |
| 2 | layout_constraintCenter_toCenterOf 属性不存在 | ConstraintLayout 不支持此属性 | 改为设置 top/bottom/start/end 约束 | ✅ 已修复 |
| 3 | JS 回调参数特殊字符导致语法错误 | 内容包含 `'` 或 `\` 破坏JS字符串 | 添加转义处理 | ✅ 已修复 |
| 4 | 沙箱路径比较失败 (SecurityException) | Android多用户导致 `/data/data/` 与 `/data/user/0/` 不匹配 | 将 toolDir 也转为 canonicalFile 再比较 | ✅ 已修复 |
| 5 | minSdk 版本太低 | 原 minSdk = 26 | 提升到 34 (Android 14) | ✅ 已修复 |
| 6 | 跨目录 renameTo 静默失败 | File.renameTo() 不同挂载点间不工作 | 改用 copyRecursively() + deleteRecursively() | ✅ 已修复 |
| 7 | 导入文件名显示错误 | 使用 lastPathSegment 返回内部路径 | 使用 OpenableColumns.DISPLAY_NAME | ✅ 已修复 |
| 8 | JS 对话框显示多余标题 | WebView 默认行为 | 自定义 WebChromeClient 重写 onJsAlert/onJsConfirm | ✅ 已修复 |
| 9 | 桌面快捷方式无效 | 旧版本 INSTALL_SHORTCUT 广播已废弃 | 使用 ShortcutManager.requestPinShortcut() | ✅ 已修复 |

---

## 🎯 后续版本规划 (v1.2+)

### 待实现功能优先级

**P0 - 高优先级:**
- [ ] 深色模式适配
- [ ] 导出时可选是否包含数据
- [ ] 首次启动自动安装示例工具

**P1 - 中优先级:**
- [ ] 工具分类/文件夹
- [ ] 搜索功能
- [ ] 批量导入/导出
- [ ] 工具排序
- [ ] 工具信息查看页

**P2 - 低优先级:**
- [ ] 剪贴板访问API
- [ ] 相机访问API
- [ ] 通知API
- [ ] 工具市场/在线下载
- [ ] 备份与恢复功能

---

## 📝 开发日志

### 2026-06-26 (v1.1)
- ✅ 实现图标颜色系统（首字母 + 7种彩虹色）
- ✅ 创建工具编辑页面 (EditToolActivity)
- ✅ 导入页面添加颜色选择器
- ✅ 长按菜单新增"编辑"选项
- ✅ 桌面快捷方式动态生成对应颜色图标
- ✅ 修复 JS 对话框默认标题问题
- ✅ 工具 ID 自动生成（UUID 前8位）
- ✅ 修复跨目录文件复制问题
- ✅ 修复导入文件名显示问题

---

*文档最后更新: 2026-06-26*
