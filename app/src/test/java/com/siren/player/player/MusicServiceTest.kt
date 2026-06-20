package com.siren.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MusicServiceTest {

    @Test
    fun playModeValues() {
        val modes = PlayMode.entries
        assertEquals(5, modes.size)
    }

    @Test
    fun nextModeCyclesCorrectly() {
        assertEquals(PlayMode.SINGLE_STOP, PlayMode.SINGLE_LOOP.next())
        assertEquals(PlayMode.ALBUM_LOOP, PlayMode.SINGLE_STOP.next())
        assertEquals(PlayMode.ALBUM_STOP, PlayMode.ALBUM_LOOP.next())
        assertEquals(PlayMode.ALBUM_SHUFFLE, PlayMode.ALBUM_STOP.next())
        assertEquals(PlayMode.SINGLE_LOOP, PlayMode.ALBUM_SHUFFLE.next())
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
