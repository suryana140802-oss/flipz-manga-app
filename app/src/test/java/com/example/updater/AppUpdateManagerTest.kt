package com.example.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsNewerVersion_detectsMajorUpdate() {
        assertTrue(AppUpdateManager.isNewerVersion("2.1", "3.0"))
        assertTrue(AppUpdateManager.isNewerVersion("2.1.0", "3.0.0"))
    }

    @Test
    fun testIsNewerVersion_detectsMinorUpdate() {
        assertTrue(AppUpdateManager.isNewerVersion("2.1", "2.2"))
        assertTrue(AppUpdateManager.isNewerVersion("2.1.0", "2.2.0"))
    }

    @Test
    fun testIsNewerVersion_detectsPatchUpdate() {
        assertTrue(AppUpdateManager.isNewerVersion("2.1.0", "2.1.1"))
    }

    @Test
    fun testIsNewerVersion_sameVersion_returnsFalse() {
        assertFalse(AppUpdateManager.isNewerVersion("2.1", "2.1"))
        assertFalse(AppUpdateManager.isNewerVersion("2.1.0", "2.1.0"))
    }

    @Test
    fun testIsNewerVersion_olderVersion_returnsFalse() {
        assertFalse(AppUpdateManager.isNewerVersion("2.2", "2.1"))
        assertFalse(AppUpdateManager.isNewerVersion("3.0", "2.9"))
    }

    @Test
    fun testIsNewerVersion_emptyOrBlank_returnsFalse() {
        assertFalse(AppUpdateManager.isNewerVersion("", "2.1"))
        assertFalse(AppUpdateManager.isNewerVersion("2.1", ""))
        assertFalse(AppUpdateManager.isNewerVersion("", ""))
    }
}
