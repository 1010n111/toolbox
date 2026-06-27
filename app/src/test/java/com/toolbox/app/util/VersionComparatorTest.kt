// VersionComparatorTest.kt
package com.toolbox.app.util

import org.junit.Test
import org.junit.Assert.*

class VersionComparatorTest {

    @Test
    fun compare_sameVersions_returnsZero() {
        assertEquals(0, VersionComparator.compare("1.0.0", "1.0.0"))
    }

    @Test
    fun compare_remoteHigher_returnsPositive() {
        assertTrue(VersionComparator.compare("1.0.0", "1.0.1") > 0)
        assertTrue(VersionComparator.compare("1.0.0", "1.1.0") > 0)
        assertTrue(VersionComparator.compare("1.9.9", "2.0.0") > 0)
    }

    @Test
    fun compare_localHigher_returnsNegative() {
        assertTrue(VersionComparator.compare("1.0.1", "1.0.0") < 0)
    }

    @Test
    fun compare_withVPrefix_ignoresPrefix() {
        assertEquals(0, VersionComparator.compare("v1.0.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("V1.0.0", "1.0.0"))
    }

    @Test
    fun compare_differentLengths_padsWithZero() {
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("1", "1.0.0"))
    }

    @Test
    fun isValid_validVersions_returnsTrue() {
        assertTrue(VersionComparator.isValid("1.0.0"))
        assertTrue(VersionComparator.isValid("v1.0.0"))
        assertTrue(VersionComparator.isValid("2.0"))
    }

    @Test
    fun isValid_invalidVersions_returnsFalse() {
        assertFalse(VersionComparator.isValid(""))
        assertFalse(VersionComparator.isValid("unknown"))
    }
}
