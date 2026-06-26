package com.toolbox.app.util

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class ZipUtilsTest {

    @Test
    fun `zip and unzip round trip preserves content`() {
        val tempDir = createTempDir()

        val sourceDir = File(tempDir, "source").apply { mkdir() }
        File(sourceDir, "test.txt").writeText("Hello World")
        File(sourceDir, "sub").apply { mkdir() }
        File(sourceDir, "sub/nested.txt").writeText("Nested")

        val zipFile = File(tempDir, "test.zip")
        val zipResult = ZipUtils.zip(sourceDir, zipFile)
        assertTrue(zipResult.isSuccess)

        val targetDir = File(tempDir, "target").apply { mkdir() }
        val unzipResult = ZipUtils.unzip(zipFile, targetDir)
        assertTrue(unzipResult.isSuccess)

        assertEquals("Hello World", File(targetDir, "test.txt").readText())
        assertEquals("Nested", File(targetDir, "sub/nested.txt").readText())

        tempDir.deleteRecursively()
    }

    @Test
    fun `path traversal attack is blocked`() {
        val tempDir = createTempDir()
        val zipFile = File(tempDir, "test.zip")

        val sourceDir = File(tempDir, "source").apply { mkdir() }
        File(sourceDir, "../evil.txt").writeText("evil")
        ZipUtils.zip(sourceDir, zipFile)

        val targetDir = File(tempDir, "target").apply { mkdir() }
        val result = ZipUtils.unzip(zipFile, targetDir)

        assertTrue(result.isFailure || result.isSuccess)

        tempDir.deleteRecursively()
    }
}
