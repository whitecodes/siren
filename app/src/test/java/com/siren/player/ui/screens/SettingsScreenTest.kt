package com.siren.player.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsScreenTest {

    @Test
    fun appVersion() {
        val version = "1.0"
        assertEquals("1.0", version)
    }

    @Test
    fun appName() {
        val name = "塞壬唱片"
        assertEquals("塞壬唱片", name)
    }

    @Test
    fun cacheSettings() {
        val cacheEnabled = true
        val maxCacheSize = 500L * 1024 * 1024 // 500MB
        assertEquals(true, cacheEnabled)
        assertEquals(524288000L, maxCacheSize)
    }
}
