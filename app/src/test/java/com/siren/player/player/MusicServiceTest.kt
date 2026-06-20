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
        assertEquals(PlayMode.LIST_LOOP, PlayMode.SINGLE_STOP.next())
        assertEquals(PlayMode.LIST_STOP, PlayMode.LIST_LOOP.next())
        assertEquals(PlayMode.LIST_SHUFFLE, PlayMode.LIST_STOP.next())
        assertEquals(PlayMode.SINGLE_LOOP, PlayMode.LIST_SHUFFLE.next())
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
