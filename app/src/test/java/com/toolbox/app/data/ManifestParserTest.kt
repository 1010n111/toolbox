package com.toolbox.app.data

import org.junit.Test
import org.junit.Assert.*

class ManifestParserTest {

    @Test
    fun `parse valid manifest returns ToolInfo`() {
        val json = """
            {
                "id": "test-tool",
                "name": "Test Tool",
                "version": "1.0.0",
                "description": "A test tool",
                "author": "Tester",
                "icon": "icon.png",
                "permissions": []
            }
        """.trimIndent()

        val result = ManifestParser.parse(json, "/test/path")

        assertTrue(result.isSuccess)
        val tool = result.getOrNull()!!
        assertEquals("test-tool", tool.id)
        assertEquals("Test Tool", tool.name)
        assertEquals("/test/path", tool.dirPath)
    }

    @Test
    fun `parse invalid manifest returns failure`() {
        val json = "invalid json"
        val result = ManifestParser.parse(json, "/test/path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `parse manifest missing id returns failure`() {
        val json = """{"name": "Test", "version": "1.0"}"""
        val result = ManifestParser.parse(json, "/test/path")
        assertTrue(result.isFailure)
    }
}
