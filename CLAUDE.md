# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

ToolBox is an Android HTML tool platform that runs HTML single-page applications packaged as ZIP files. It provides a sandbox runtime environment and native capability bridging, enabling quick development of various small tools using HTML/CSS/JS.

**Current Version**: 1.2.2

---

## Build & Test Commands

### Build Project
```bash
./gradlew assembleDebug      # Debug build
./gradlew assembleRelease    # Release build
./gradlew compileDebugKotlin # Compile Kotlin only (for quick checks)
```

### Run Tests
```bash
./gradlew testDebugUnitTest              # Unit tests (local JVM)
./gradlew connectedDebugAndroidTest      # Instrumented tests (device/emulator)
```

### Lint & Code Quality
```bash
./gradlew lintDebug          # Run Android lint
```

### Gradle Files
- Root: `build.gradle.kts`
- App module: `app/build.gradle.kts`
- Settings: `settings.gradle.kts`

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    MainActivity                          │
│              (Tool Grid Home Screen)                    │
└──────────────────────────┬──────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
┌───▼──────┐        ┌──────▼───────┐     ┌──────▼───────┐
│ Import   │        │ Download     │     │ Repository   │
│ Activity │        │ Activity     │     │ Activity     │
└──────────┘        └──────────────┘     └──────┬───────┘
                                                 │
    ┌────────────────────────────────────────────┼────────────────────────────────┐
    │                                            │                                │
┌───▼──────┐                               ┌────▼─────┐                    ┌─────▼──────┐
│ EditTool │                               │ Tool     │                    │ ToolDetail │
│ Activity │                               │ Activity │                    │ Activity   │
└──────────┘                               └────┬─────┘                    └────────────┘
                                                 │
                                      ┌──────────▼──────────┐
                                      │     WebView         │
                                      │  (Sandbox Runtime)  │
                                      └──────────┬──────────┘
                                                 │
                                  ┌──────────────▼───────────────┐
                                  │        JS Bridges           │
                                  │  ToolBridge + BoxNative     │
                                  │  (File I/O, Dialogs, etc.)  │
                                  └─────────────────────────────┘
```

### Core Components

| Layer | Key Files | Responsibility |
|-------|-----------|----------------|
| **UI Layer** | `MainActivity.kt`, `ToolActivity.kt`, `ImportActivity.kt`, `DownloadActivity.kt`, `ToolDetailActivity.kt`, `EditToolActivity.kt`, `RepositoryActivity.kt` | Screen navigation and user interaction |
| **Data Layer** | `ToolManager.kt`, `ToolDirectory.kt`, `ManifestParser.kt` | Tool installation, file management, manifest parsing |
| **Model** | `ToolInfo.kt`, `RepositoryTool.kt` | Data models |
| **Bridge Layer** | `ToolBridge.kt`, `BoxNative.kt` | JS ↔ Native communication |
| **Utilities** | `ZipUtils.kt`, `VersionComparator.kt`, `DownloadHistory.kt` | Helper functions |

### Key Design Patterns

1. **Activity per Screen**: Each major feature has its own Activity with XML layouts
2. **WebView Sandbox**: Each tool runs in an isolated WebView with its own data directory
3. **JS-Native Bridge**: Two-way communication using `@JavascriptInterface`
4. **SharedPreferences**: For download history, repository cache, and settings

---

## Important Conventions

### Data Flow Pattern
```kotlin
// Standard pattern for background work + UI updates
Thread {
    try {
        // Background work (file I/O, network)
        val result = doWork()
        runOnUiThread {
            // Update UI
            updateUI(result)
        }
    } catch (e: Exception) {
        runOnUiThread {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }
}.start()
```

### Tool Manifest Format
```json
{
  "id": "tool-id",
  "name": "Tool Name",
  "version": "1.0.0",
  "description": "Description",
  "author": "Author Name",
  "iconColor": "#FF9800",
  "downloadUrl": "https://..."
}
```

### JS API Available to Tools
```javascript
// File System (async recommended)
window.ToolBridge.readFile(path, callback)
window.ToolBridge.writeFile(path, content, callback)
window.ToolBridge.listFiles(path, callback)
window.ToolBridge.deleteFile(path, callback)

// $box Utilities (auto-injected)
$box.toast(message)
$box.alert(title, message, callback)
$box.confirm(title, callback)
$box.getToolInfo()
```

---

## Hot Files (Frequently Modified)

| File | Purpose |
|------|---------|
| `app/src/main/java/com/toolbox/app/data/ToolManager.kt` | Core tool management logic |
| `app/src/main/java/com/toolbox/app/RepositoryActivity.kt` | Online tool repository |
| `app/src/main/java/com/toolbox/app/bridge/ToolBridge.kt` | JS file system bridge |
| `app/src/main/java/com/toolbox/app/bridge/BoxNative.kt` | JS utilities bridge |
| `app/src/main/res/layout/*.xml` | All screen layouts |
| `docs/CHANGELOG.md` | Version history |

---

## Common Tasks

### Add a New JS API
1. Add method to `BoxNative.kt` or `ToolBridge.kt` with `@JavascriptInterface`
2. Inject bridge in `ToolActivity.kt` → `webView.addJavascriptInterface()`
3. Update documentation in `README.md`

### Add a New Screen
1. Create `ActivityName.kt` extending `AppCompatActivity`
2. Create `activity_name.xml` layout
3. Register in `AndroidManifest.xml`
4. Add navigation intent from existing activity

### Add a New Tool to Repository
1. Update the remote `tools.json` in repository server
2. No code changes needed (client dynamically loads)

---

## Versioning

Update version numbers in:
- `app/build.gradle.kts` → `versionCode`, `versionName`
- `docs/CHANGELOG.md` → Add release notes

Follow Semantic Versioning: `MAJOR.MINOR.PATCH`

---

## Repository Caching Strategy

RepositoryActivity uses two caching layers:
1. **SharedPreferences** - For parsed tool list JSON
2. **HTTP Cache** - Bypassed during manual refresh (via timestamp query param + Cache-Control headers)

Force refresh flow: Clear cache → Fetch with `?_t=timestamp` → Update UI immediately

---

## Color System

7 predefined colors for tool icons:
- Red: `#F44336`, Orange: `#FF9800` (default), Yellow: `#FFEB3B`
- Green: `#4CAF50`, Blue: `#2196F3`, Indigo: `#3F51B5`, Purple: `#9C27B0`
