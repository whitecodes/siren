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
        assertTrue(modes.contains(PlayMode.ALBUM_LOOP))
        assertTrue(modes.contains(PlayMode.ALBUM_STOP))
        assertTrue(modes.contains(PlayMode.ALBUM_SHUFFLE))
    }

    @Test
    fun playModeDisplayNames() {
        assertEquals("单曲循环", PlayMode.SINGLE_LOOP.displayName)
        assertEquals("单曲停止", PlayMode.SINGLE_STOP.displayName)
        assertEquals("专辑循环", PlayMode.ALBUM_LOOP.displayName)
        assertEquals("专辑停止", PlayMode.ALBUM_STOP.displayName)
        assertEquals("专辑内随机", PlayMode.ALBUM_SHUFFLE.displayName)
    }
}
