package com.toolbox.app.model

import com.toolbox.app.data.ManifestParser
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class ToolInfoTest {
    @Test
    fun `tool info fields are correctly stored`() {
        val tool = ToolInfo(
            id = "test-tool",
            name = "Test Tool",
            version = "1.0.0",
            description = "Test",
            author = "Tester",
            iconPath = "icon.png",
            iconColor = "#FF5722",
            dirPath = "/tmp/test",
            installedAt = 1234567890L
        )

        assertEquals("test-tool", tool.id)
        assertEquals("Test Tool", tool.name)
        assertEquals("1.0.0", tool.version)
        assertEquals("", tool.downloadUrl)
    }

    @Test
    fun `tool info with downloadUrl stores correctly`() {
        val tool = ToolInfo(
            id = "test-tool",
            name = "Test Tool",
            version = "1.0.0",
            description = "Test",
            author = "Tester",
            iconPath = "icon.png",
            iconColor = "#FF5722",
            dirPath = "/tmp/test",
            installedAt = 1234567890L,
            downloadUrl = "https://example.com/tool.zip"
        )

        assertEquals("https://example.com/tool.zip", tool.downloadUrl)
    }

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
        assertEquals("", toolInfo.downloadUrl)

        tempFile.delete()
    }

    @Test
    fun testToolInfo_withDownloadUrl_parsesCorrectly() {
        val json = """
            {
                "id": "test-tool",
                "name": "Test Tool",
                "version": "1.0.0",
                "description": "Test",
                "author": "Test",
                "icon": "",
                "iconColor": "#FF0000",
                "downloadUrl": "https://example.com/test-tool.zip"
            }
        """.trimIndent()

        val tempFile = File.createTempFile("manifest", ".json")
        tempFile.writeText(json)

        val result = ManifestParser.parseFromFile(tempFile)
        assertTrue(result.isSuccess)

        val toolInfo = result.getOrThrow()
        assertEquals("https://example.com/test-tool.zip", toolInfo.downloadUrl)

        tempFile.delete()
    }
}
