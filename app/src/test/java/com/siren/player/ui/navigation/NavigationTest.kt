package com.siren.player.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {

    @Test
    fun navigationItemsCount() {
        assertEquals(5, NavigationItem.entries.size)
    }

    @Test
    fun albumRoute() {
        assertEquals("album", NavigationItem.Album.route)
    }

    @Test
    fun playlistRoute() {
        assertEquals("playlist", NavigationItem.Playlist.route)
    }

    @Test
    fun downloadQueueRoute() {
        assertEquals("download_queue", NavigationItem.DownloadQueue.route)
    }

    @Test
    fun settingsRoute() {
        assertEquals("settings", NavigationItem.Settings.route)
    }

    @Test
    fun aboutRoute() {
        assertEquals("about", NavigationItem.About.route)
    }

    @Test
    fun albumTitle() {
        assertEquals("专辑", NavigationItem.Album.title)
    }

    @Test
    fun playlistTitle() {
        assertEquals("播放列表", NavigationItem.Playlist.title)
    }

    @Test
    fun downloadQueueTitle() {
        assertEquals("下载队列", NavigationItem.DownloadQueue.title)
    }

    @Test
    fun settingsTitle() {
        assertEquals("设置", NavigationItem.Settings.title)
    }

    @Test
    fun aboutTitle() {
        assertEquals("关于", NavigationItem.About.title)
    }
}
