package com.siren.player.player

import com.siren.player.R
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
    fun displayNameResIds() {
        // 测试资源 ID 是否正确映射
        assertEquals(R.string.mode_single_loop, PlayMode.SINGLE_LOOP.displayNameResId)
        assertEquals(R.string.mode_single_stop, PlayMode.SINGLE_STOP.displayNameResId)
        assertEquals(R.string.mode_list_loop, PlayMode.LIST_LOOP.displayNameResId)
        assertEquals(R.string.mode_list_stop, PlayMode.LIST_STOP.displayNameResId)
        assertEquals(R.string.mode_list_shuffle, PlayMode.LIST_SHUFFLE.displayNameResId)
    }
}
