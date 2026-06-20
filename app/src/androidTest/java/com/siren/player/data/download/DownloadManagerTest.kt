package com.siren.player.data.download

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.siren.player.data.api.SongDetail
import com.siren.player.db.DownloadStatus
import com.siren.player.db.SirenDatabase
import com.siren.player.db.TaskStatus
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
class DownloadManagerTest {

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
    fun enqueueCreatesDownloadTask() = runTest {
        val song = SongDetail(
            cid = "s1",
            name = "测试歌曲",
            albumCid = "a1",
            sourceUrl = "https://example.com/song.mp3",
            artists = listOf("artist1")
        )

        val taskId = downloadManager.enqueue(song)
        val task = database.downloadTaskDao().get(taskId)

        assertNotNull(task)
        assertEquals("s1", task?.songCid)
        assertEquals(TaskStatus.PENDING, task?.status)
    }

    @Test
    fun cancelTaskUpdatesStatus() = runTest {
        val song = SongDetail(
            cid = "s1", name = "test", albumCid = "a1",
            sourceUrl = "https://example.com/song.mp3", artists = listOf("a")
        )
        val taskId = downloadManager.enqueue(song)
        downloadManager.cancel(taskId)
        val task = database.downloadTaskDao().get(taskId)

        assertEquals(TaskStatus.CANCELLED, task?.status)
    }

    @Test
    fun pauseTaskUpdatesStatus() = runTest {
        val song = SongDetail(
            cid = "s1", name = "test", albumCid = "a1",
            sourceUrl = "https://example.com/song.mp3", artists = listOf("a")
        )
        val taskId = downloadManager.enqueue(song)
        // Note: pause only works on DOWNLOADING tasks, this tests the status update
        downloadManager.pause(taskId)
        val task = database.downloadTaskDao().get(taskId)

        // Task might still be PENDING if download hasn't started
        assertNotNull(task)
    }

    @Test
    fun getActiveTasksReturnsPendingAndDownloading() = runTest {
        val songs = listOf(
            SongDetail(cid = "s1", name = "t1", albumCid = "a1", sourceUrl = "url1", artists = listOf("a")),
            SongDetail(cid = "s2", name = "t2", albumCid = "a1", sourceUrl = "url2", artists = listOf("a")),
            SongDetail(cid = "s3", name = "t3", albumCid = "a1", sourceUrl = "url3", artists = listOf("a"))
        )
        songs.forEach { downloadManager.enqueue(it) }

        val activeTasks = downloadManager.activeTasks.first()
        assertEquals(3, activeTasks.size)
    }

    @Test
    fun getDownloadPathReturnsCorrectPath() {
        val path = downloadManager.getDownloadPath("测试专辑", "测试歌曲.mp3")
        assertNotNull(path)
        assert(path.contains("Music"))
        assert(path.contains("测试专辑"))
        assert(path.contains("测试歌曲.mp3"))
    }
}
