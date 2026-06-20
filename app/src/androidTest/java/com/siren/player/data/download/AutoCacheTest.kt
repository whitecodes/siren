package com.siren.player.data.download

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.siren.player.data.api.SongDetail
import com.siren.player.db.DownloadStatus
import com.siren.player.db.SirenDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoCacheTest {

    private lateinit var database: SirenDatabase
    private lateinit var downloadManager: DownloadManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SirenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        downloadManager = DownloadManager(context, database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun songWithoutCacheHasNullPath() = runTest {
        database.songDao().insert(
            com.siren.player.db.Song(
                cid = "s1",
                name = "test",
                albumCid = "a1",
                artists = "a",
                status = DownloadStatus.NOT_DOWNLOADED,
                localPath = null
            )
        )
        val path = database.songDao().getLocalPath("s1")
        assertNull(path)
    }

    @Test
    fun songWithCacheHasPath() = runTest {
        database.songDao().insert(
            com.siren.player.db.Song(
                cid = "s1",
                name = "test",
                albumCid = "a1",
                artists = "a",
                status = DownloadStatus.DOWNLOADED,
                localPath = "/path/to/file.mp3"
            )
        )
        val path = database.songDao().getLocalPath("s1")
        assertNotNull(path)
        assertEquals("/path/to/file.mp3", path)
    }

    @Test
    fun enqueueCreatesTask() = runTest {
        val song = SongDetail(
            cid = "s1",
            name = "test",
            albumCid = "a1",
            sourceUrl = "https://example.com/song.mp3",
            artists = listOf("a")
        )
        val taskId = downloadManager.enqueue(song)
        val task = database.downloadTaskDao().get(taskId)
        assertNotNull(task)
        assertEquals("s1", task?.songCid)
    }
}
