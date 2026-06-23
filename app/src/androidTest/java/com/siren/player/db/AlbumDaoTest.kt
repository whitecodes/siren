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
class AlbumDaoTest {

    private lateinit var database: SirenDatabase
    private lateinit var albumDao: AlbumDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SirenDatabase::class.java
        ).allowMainThreadQueries().build()
        albumDao = database.albumDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAlbum() = runTest {
        val album = Album(
            cid = "7761",
            name = "测试专辑",
            coverUrl = "https://example.com/cover.jpg",
            artists = "塞壬唱片-MSR"
        )

        albumDao.insert(album)
        val result = albumDao.get("7761")

        assertNotNull(result)
        assertEquals("7761", result?.cid)
        assertEquals("测试专辑", result?.name)
        assertEquals("https://example.com/cover.jpg", result?.coverUrl)
        assertEquals("塞壬唱片-MSR", result?.artists)
    }

    @Test
    fun getNonExistentAlbumReturnsNull() = runTest {
        val result = albumDao.get("nonexistent")
        assertNull(result)
    }

    @Test
    fun insertAllAndGetAllAlbums() = runTest {
        val albums = listOf(
            Album(cid = "1", name = "专辑1", coverUrl = "url1", artists = "artist1"),
            Album(cid = "2", name = "专辑2", coverUrl = "url2", artists = "artist2"),
            Album(cid = "3", name = "专辑3", coverUrl = "url3", artists = "artist3")
        )

        albumDao.insertAll(albums)
        val result = albumDao.getAll().first()

        assertEquals(3, result.size)
    }

    @Test
    fun replaceAlbumOnConflict() = runTest {
        val album1 = Album(cid = "1", name = "旧名称", coverUrl = "old", artists = "old")
        val album2 = Album(cid = "1", name = "新名称", coverUrl = "new", artists = "new")

        albumDao.insert(album1)
        albumDao.insert(album2)
        val result = albumDao.get("1")

        assertEquals("新名称", result?.name)
    }

    @Test
    fun deleteAlbum() = runTest {
        val album = Album(cid = "1", name = "test", coverUrl = "url", artists = "a")
        albumDao.insert(album)
        albumDao.delete("1")
        val result = albumDao.get("1")

        assertNull(result)
    }
}
