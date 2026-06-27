// VersionComparator.kt
package com.toolbox.app.util

object VersionComparator {
    /**
     * 比较两个版本号
     * 返回: >0 = 远程版本更新, 0 = 相同, <0 = 本地版本更新
     * Ponytail 简化版：只比较数字部分，忽略 alpha/beta 后缀
     */
    fun compare(localVersion: String, remoteVersion: String): Int {
        val local = cleanVersion(localVersion)
        val remote = cleanVersion(remoteVersion)

        val localParts = local.split('.').map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.split('.').map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val l = localParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (l != r) {
                return r - l
            }
        }

        return 0
    }

    fun isValid(version: String): Boolean {
        val clean = cleanVersion(version)
        return clean.isNotEmpty() && clean.matches(Regex("""^\d+(\.\d+)*$"""))
    }

    private fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }
}
