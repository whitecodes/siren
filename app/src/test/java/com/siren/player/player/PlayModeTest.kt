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
    fun listLoopIndex() {
        assertEquals(2, PlayMode.LIST_LOOP.ordinal)
    }

    @Test
    fun listStopIndex() {
        assertEquals(3, PlayMode.LIST_STOP.ordinal)
    }

    @Test
    fun listShuffleIndex() {
        assertEquals(4, PlayMode.LIST_SHUFFLE.ordinal)
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
    fun displayName() {
        assertEquals("单曲循环", PlayMode.SINGLE_LOOP.displayName)
        assertEquals("单曲结束", PlayMode.SINGLE_STOP.displayName)
        assertEquals("列表循环", PlayMode.LIST_LOOP.displayName)
        assertEquals("列表停止", PlayMode.LIST_STOP.displayName)
        assertEquals("列表随机", PlayMode.LIST_SHUFFLE.displayName)
    }
}
