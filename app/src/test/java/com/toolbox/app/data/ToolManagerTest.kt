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

        // 5. 尝试不覆盖安装应该抛出异常
        var exceptionThrown = false
        try {
            toolManager.installFromTempDir(tempDir2, "test-overwrite")
        } catch (e: Exception) {
            exceptionThrown = true
        }
        assertTrue("Should throw exception when overwrite=false and tool exists", exceptionThrown)

        // 6. 覆盖安装
        toolManager.installFromTempDir(tempDir2, "test-overwrite", overwrite = true)
        val installed2 = toolManager.getTool("test-overwrite")
        assertEquals("2.0.0", installed2?.version)

        // 清理
        toolManager.deleteTool("test-overwrite")
        tempDir.deleteRecursively()
        tempDir2.deleteRecursively()
    }

    @Test
    fun updateDownloadUrl_updatesExistingUrl() {
        val context = RuntimeEnvironment.getApplication() as Context
        val toolManager = ToolManager(context)

        // 创建测试工具
        val tempDir = File(context.cacheDir, "test_download")
        tempDir.mkdirs()
        File(tempDir, "manifest.json").writeText("""
            {"id":"test-download","name":"Test","version":"1.0.0","description":"","author":"","icon":"","iconColor":"#FF0000"}
        """.trimIndent())
        File(tempDir, "index.html").writeText("<html></html>")

        // 安装
        toolManager.installFromTempDir(tempDir, "test-download")

        // 更新下载链接
        val newUrl = "https://example.com/tool.zip"
        val result = toolManager.updateDownloadUrl("test-download", newUrl)
        assertTrue("updateDownloadUrl should succeed", result)

        // 验证保存成功
        val tool = toolManager.getTool("test-download")
        assertEquals(newUrl, tool?.downloadUrl)

        // 清理
        toolManager.deleteTool("test-download")
        tempDir.deleteRecursively()
    }

    @Test
    fun updateDownloadUrl_returnsFalseForNonExistentTool() {
        val context = RuntimeEnvironment.getApplication() as Context
        val toolManager = ToolManager(context)

        val result = toolManager.updateDownloadUrl("non-existent", "https://example.com")
        assertFalse("Should return false for non-existent tool", result)
    }

    @Test
    fun updateToolInfo_withDownloadUrl_savesDownloadUrl() {
        val context = RuntimeEnvironment.getApplication() as Context
        val toolManager = ToolManager(context)

        // 创建测试工具
        val tempDir = File(context.cacheDir, "test_update_info")
        tempDir.mkdirs()
        File(tempDir, "manifest.json").writeText("""
            {"id":"test-update-info","name":"Test","version":"1.0.0","description":"","author":"","icon":"","iconColor":"#FF0000"}
        """.trimIndent())
        File(tempDir, "index.html").writeText("<html></html>")

        // 安装
        toolManager.installFromTempDir(tempDir, "test-update-info")

        // 更新信息，包含 downloadUrl
        val downloadUrl = "https://example.com/new.zip"
        val result = toolManager.updateToolInfo(
            toolId = "test-update-info",
            name = "Updated Test",
            version = "1.0.1",
            author = "Test Author",
            description = "Updated description",
            iconColor = "#00FF00",
            downloadUrl = downloadUrl
        )
        assertTrue("updateToolInfo should succeed", result)

        // 验证所有字段包括 downloadUrl
        val tool = toolManager.getTool("test-update-info")
        assertEquals("Updated Test", tool?.name)
        assertEquals("1.0.1", tool?.version)
        assertEquals("Test Author", tool?.author)
        assertEquals("Updated description", tool?.description)
        assertEquals("#00FF00", tool?.iconColor)
        assertEquals(downloadUrl, tool?.downloadUrl)

        // 清理
        toolManager.deleteTool("test-update-info")
        tempDir.deleteRecursively()
    }
}
