package com.toolbox.app.model

import org.junit.Test
import org.junit.Assert.*

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
    }
}
