package com.siren.player.ui.navigation

import com.siren.player.R
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
    fun albumTitleResId() {
        assertEquals(R.string.nav_album, NavigationItem.Album.titleResId)
    }

    @Test
    fun playlistTitleResId() {
        assertEquals(R.string.nav_playlist, NavigationItem.Playlist.titleResId)
    }

    @Test
    fun downloadQueueTitleResId() {
        assertEquals(R.string.nav_download_queue, NavigationItem.DownloadQueue.titleResId)
    }

    @Test
    fun settingsTitleResId() {
        assertEquals(R.string.nav_settings, NavigationItem.Settings.titleResId)
    }

    @Test
    fun aboutTitleResId() {
        assertEquals(R.string.nav_about, NavigationItem.About.titleResId)
    }
}
