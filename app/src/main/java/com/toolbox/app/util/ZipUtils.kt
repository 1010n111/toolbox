package com.toolbox.app.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    fun unzip(zipFile: File, targetDir: File): Result<Unit> {
        return runCatching {
            targetDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val entryFile = File(targetDir, entry!!.name)

                    if (!entryFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Illegal zip entry path: ${entry!!.name}")
                    }

                    if (entry!!.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        FileOutputStream(entryFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                }
            }
        }
    }

    fun zip(sourceDir: File, zipFile: File, excludeDirs: List<String> = emptyList()): Result<Unit> {
        return runCatching {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                sourceDir.walkTopDown()
                    .filter { it.isFile }
                    .filter { file ->
                        excludeDirs.none { exclude ->
                            file.relativeTo(sourceDir).startsWith(exclude)
                        }
                    }
                    .forEach { file ->
                        val entryName = file.relativeTo(sourceDir).path
                        zos.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
            }
        }
    }

    /**
     * Find a specific entry by name in the zip file without extracting everything
     */
    fun findEntry(zipFile: File, entryName: String): ZipEntry? {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name == entryName || entry!!.name == "/$entryName") {
                    return entry
                }
            }
        }
        return null
    }

    /**
     * Read the content of a specific entry as a String
     */
    fun readEntryContent(zipFile: File, entry: ZipEntry): String {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            // Need to position at the correct entry
            var current: ZipEntry?
            while (zis.nextEntry.also { current = it } != null) {
                if (current!!.name == entry.name || current!!.name == "/${entry.name}") {
                    return zis.bufferedReader().readText()
                }
            }
            throw IllegalArgumentException("Entry not found: ${entry.name}")
        }
    }
}
