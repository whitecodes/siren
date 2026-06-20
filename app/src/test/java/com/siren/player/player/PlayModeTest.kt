package com.siren.player.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayModeTest {

    @Test
    fun playModeValues() {
        val modes = PlayMode.entries
        assertEquals(5, modes.size)
    }

    @Test
    fun singleLoopIndex() {
        assertEquals(0, PlayMode.SINGLE_LOOP.ordinal)
    }

    @Test
    fun singleStopIndex() {
        assertEquals(1, PlayMode.SINGLE_STOP.ordinal)
    }

    @Test
    fun albumLoopIndex() {
        assertEquals(2, PlayMode.ALBUM_LOOP.ordinal)
    }

    @Test
    fun albumStopIndex() {
        assertEquals(3, PlayMode.ALBUM_STOP.ordinal)
    }

    @Test
    fun albumShuffleIndex() {
        assertEquals(4, PlayMode.ALBUM_SHUFFLE.ordinal)
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
    fun displayName() {
        assertEquals("单曲循环", PlayMode.SINGLE_LOOP.displayName)
        assertEquals("单曲停止", PlayMode.SINGLE_STOP.displayName)
        assertEquals("专辑循环", PlayMode.ALBUM_LOOP.displayName)
        assertEquals("专辑停止", PlayMode.ALBUM_STOP.displayName)
        assertEquals("专辑内随机", PlayMode.ALBUM_SHUFFLE.displayName)
    }
}
