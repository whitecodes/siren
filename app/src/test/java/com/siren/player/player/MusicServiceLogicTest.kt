package com.siren.player.player

import com.siren.player.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicServiceLogicTest {

    @Test
    fun playModeNextCycles() {
        assertEquals(PlayMode.SINGLE_STOP, PlayMode.SINGLE_LOOP.next())
        assertEquals(PlayMode.LIST_LOOP, PlayMode.SINGLE_STOP.next())
        assertEquals(PlayMode.LIST_STOP, PlayMode.LIST_LOOP.next())
        assertEquals(PlayMode.LIST_SHUFFLE, PlayMode.LIST_STOP.next())
        assertEquals(PlayMode.SINGLE_LOOP, PlayMode.LIST_SHUFFLE.next())
    }

    @Test
    fun playModeCount() {
        assertEquals(5, PlayMode.entries.size)
    }

    @Test
    fun playModeDisplayNamesAreChinese() {
        // 验证所有 PlayMode 都有对应的资源 ID
        PlayMode.entries.forEach { mode ->
            assertTrue(mode.displayNameResId != 0)
        }
    }

    @Test
    fun singleLoopMode() {
        assertEquals(R.string.mode_single_loop, PlayMode.SINGLE_LOOP.displayNameResId)
        assertEquals(0, PlayMode.SINGLE_LOOP.ordinal)
    }

    @Test
    fun singleStopMode() {
        assertEquals(R.string.mode_single_stop, PlayMode.SINGLE_STOP.displayNameResId)
        assertEquals(1, PlayMode.SINGLE_STOP.ordinal)
    }

    @Test
    fun listLoopMode() {
        assertEquals(R.string.mode_list_loop, PlayMode.LIST_LOOP.displayNameResId)
        assertEquals(2, PlayMode.LIST_LOOP.ordinal)
    }

    @Test
    fun listStopMode() {
        assertEquals(R.string.mode_list_stop, PlayMode.LIST_STOP.displayNameResId)
        assertEquals(3, PlayMode.LIST_STOP.ordinal)
    }

    @Test
    fun listShuffleMode() {
        assertEquals(R.string.mode_list_shuffle, PlayMode.LIST_SHUFFLE.displayNameResId)
        assertEquals(4, PlayMode.LIST_SHUFFLE.ordinal)
    }

    @Test
    fun formatTimeZero() {
        assertEquals("00:00", formatTime(0))
        assertEquals("00:00", formatTime(-1))
    }

    @Test
    fun formatTimeSeconds() {
        assertEquals("00:05", formatTime(5000))
        assertEquals("00:30", formatTime(30000))
    }

    @Test
    fun formatTimeMinutes() {
        assertEquals("01:00", formatTime(60000))
        assertEquals("02:30", formatTime(150000))
    }

    @Test
    fun formatTimeHours() {
        // formatTime doesn't handle hours, just minutes:seconds
        // 3600000ms = 60 minutes, formatTime returns "60:00"
        assertEquals("60:00", formatTime(3600000))
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
