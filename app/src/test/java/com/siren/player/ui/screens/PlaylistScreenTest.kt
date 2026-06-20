package com.siren.player.ui.screens

import com.siren.player.player.PlayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistScreenTest {

    @Test
    fun playModeOptions() {
        val modes = PlayMode.entries
        assertEquals(5, modes.size)
        assertTrue(modes.contains(PlayMode.SINGLE_LOOP))
        assertTrue(modes.contains(PlayMode.SINGLE_STOP))
        assertTrue(modes.contains(PlayMode.LIST_LOOP))
        assertTrue(modes.contains(PlayMode.LIST_STOP))
        assertTrue(modes.contains(PlayMode.LIST_SHUFFLE))
    }

    @Test
    fun playModeDisplayNames() {
        assertEquals("单曲循环", PlayMode.SINGLE_LOOP.displayName)
        assertEquals("单曲结束", PlayMode.SINGLE_STOP.displayName)
        assertEquals("列表循环", PlayMode.LIST_LOOP.displayName)
        assertEquals("列表停止", PlayMode.LIST_STOP.displayName)
        assertEquals("列表随机", PlayMode.LIST_SHUFFLE.displayName)
    }
}
