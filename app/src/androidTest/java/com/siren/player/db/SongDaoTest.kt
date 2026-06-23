package com.siren.player.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDaoTest {

    private lateinit var database: SirenDatabase
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SirenDatabase::class.java
        ).allowMainThreadQueries().build()
        songDao = database.songDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetSong() = runTest {
        val song = Song(
            cid = "880394",
            name = "测试歌曲",
            albumCid = "7761",
            artists = "塞壬唱片-MSR",
            status = DownloadStatus.NOT_DOWNLOADED,
            localPath = null
        )

        songDao.insert(song)
        val result = songDao.get("880394")

        assertNotNull(result)
        assertEquals("880394", result?.cid)
        assertEquals("测试歌曲", result?.name)
        assertEquals("7761", result?.albumCid)
        assertEquals(DownloadStatus.NOT_DOWNLOADED, result?.status)
    }

    @Test
    fun updateSongStatus() = runTest {
        val song = Song(
            cid = "1", name = "test", albumCid = "a1",
            artists = "artist", status = DownloadStatus.NOT_DOWNLOADED, localPath = null
        )
        songDao.insert(song)
        songDao.updateStatus("1", DownloadStatus.DOWNLOADING)
        val result = songDao.get("1")

        assertEquals(DownloadStatus.DOWNLOADING, result?.status)
    }

    @Test
    fun updateSongLocalPath() = runTest {
        val song = Song(
            cid = "1", name = "test", albumCid = "a1",
            artists = "artist", status = DownloadStatus.NOT_DOWNLOADED, localPath = null
        )
        songDao.insert(song)
        songDao.updateLocalPath("1", "/path/to/file.mp3", DownloadStatus.DOWNLOADED)
        val result = songDao.get("1")

        assertEquals("/path/to/file.mp3", result?.localPath)
        assertEquals(DownloadStatus.DOWNLOADED, result?.status)
    }

    @Test
    fun getAlbumSongs() = runTest {
        val songs = listOf(
            Song(cid = "1", name = "song1", albumCid = "a1", artists = "a", status = DownloadStatus.NOT_DOWNLOADED, localPath = null),
            Song(cid = "2", name = "song2", albumCid = "a1", artists = "a", status = DownloadStatus.NOT_DOWNLOADED, localPath = null),
            Song(cid = "3", name = "song3", albumCid = "a2", artists = "a", status = DownloadStatus.NOT_DOWNLOADED, localPath = null)
        )
        songs.forEach { songDao.insert(it) }

        val result = songDao.getAlbumSongs("a1").first()
        assertEquals(2, result.size)
    }

    @Test
    fun getSongWithLocalPath() = runTest {
        val song = Song(
            cid = "1", name = "test", albumCid = "a1",
            artists = "a", status = DownloadStatus.DOWNLOADED, localPath = "/path/to/file.mp3"
        )
        songDao.insert(song)
        val path = songDao.getLocalPath("1")

        assertEquals("/path/to/file.mp3", path)
    }

    @Test
    fun deleteSong() = runTest {
        val song = Song(
            cid = "1", name = "test", albumCid = "a1",
            artists = "a", status = DownloadStatus.NOT_DOWNLOADED, localPath = null
        )
        songDao.insert(song)
        songDao.delete("1")
        val result = songDao.get("1")

        assertNull(result)
    }
}
