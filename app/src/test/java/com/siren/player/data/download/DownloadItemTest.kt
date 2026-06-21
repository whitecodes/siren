package com.siren.player.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadItemTest {

    @Test
    fun downloadItemDefaults() {
        val item = DownloadItem(id = 1L, songCid = "test")
        assertEquals(0f, item.progress)
        assertFalse(item.isCompleted)
        assertFalse(item.isFailed)
        assertEquals("", item.songName)
        assertEquals("", item.albumName)
    }

    @Test
    fun downloadItemWithProgress() {
        val item = DownloadItem(id = 1L, songCid = "test", progress = 0.75f)
        assertEquals(0.75f, item.progress)
    }

    @Test
    fun downloadItemCompleted() {
        val item = DownloadItem(id = 1L, songCid = "test", isCompleted = true)
        assertTrue(item.isCompleted)
        assertFalse(item.isFailed)
    }

    @Test
    fun downloadItemFailed() {
        val item = DownloadItem(id = 1L, songCid = "test", isFailed = true)
        assertFalse(item.isCompleted)
        assertTrue(item.isFailed)
    }

    @Test
    fun downloadItemWithNames() {
        val item = DownloadItem(
            id = 1L,
            songCid = "test",
            songName = "测试歌曲",
            albumName = "测试专辑"
        )
        assertEquals("测试歌曲", item.songName)
        assertEquals("测试专辑", item.albumName)
    }

    @Test
    fun downloadEventProgress() {
        val event = DownloadEvent.Progress("song1", 0.5f)
        assertEquals("song1", event.songCid)
        assertEquals(0.5f, event.progress)
    }

    @Test
    fun downloadEventCompleted() {
        val event = DownloadEvent.Completed("song1")
        assertEquals("song1", event.songCid)
    }

    @Test
    fun downloadEventFailed() {
        val event = DownloadEvent.Failed("song1")
        assertEquals("song1", event.songCid)
    }

    @Test
    fun downloadEventSealedClass() {
        val events = listOf(
            DownloadEvent.Progress("s1", 0.5f),
            DownloadEvent.Completed("s2"),
            DownloadEvent.Failed("s3")
        )
        assertEquals(3, events.size)
        assertTrue(events[0] is DownloadEvent.Progress)
        assertTrue(events[1] is DownloadEvent.Completed)
        assertTrue(events[2] is DownloadEvent.Failed)
    }
}
