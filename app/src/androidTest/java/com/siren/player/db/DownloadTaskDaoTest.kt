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
class DownloadTaskDaoTest {

    private lateinit var database: SirenDatabase
    private lateinit var downloadTaskDao: DownloadTaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SirenDatabase::class.java
        ).allowMainThreadQueries().build()
        downloadTaskDao = database.downloadTaskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetTask() = runTest {
        val task = DownloadTask(
            id = 1,
            songCid = "880394",
            status = TaskStatus.PENDING,
            progress = 0f,
            pausePoint = 0L,
            createdAt = System.currentTimeMillis()
        )

        downloadTaskDao.insert(task)
        val result = downloadTaskDao.get(1)

        assertNotNull(result)
        assertEquals("880394", result?.songCid)
        assertEquals(TaskStatus.PENDING, result?.status)
    }

    @Test
    fun updateTaskProgress() = runTest {
        val task = DownloadTask(
            id = 1, songCid = "s1", status = TaskStatus.DOWNLOADING,
            progress = 0f, pausePoint = 0L, createdAt = System.currentTimeMillis()
        )
        downloadTaskDao.insert(task)
        downloadTaskDao.updateProgress(1, 0.5f)
        val result = downloadTaskDao.get(1)

        assertEquals(0.5f, result?.progress)
    }

    @Test
    fun updateTaskStatus() = runTest {
        val task = DownloadTask(
            id = 1, songCid = "s1", status = TaskStatus.PENDING,
            progress = 0f, pausePoint = 0L, createdAt = System.currentTimeMillis()
        )
        downloadTaskDao.insert(task)
        downloadTaskDao.updateStatus(1, TaskStatus.DOWNLOADING)
        val result = downloadTaskDao.get(1)

        assertEquals(TaskStatus.DOWNLOADING, result?.status)
    }

    @Test
    fun pauseTask() = runTest {
        val task = DownloadTask(
            id = 1, songCid = "s1", status = TaskStatus.DOWNLOADING,
            progress = 0.5f, pausePoint = 0L, createdAt = System.currentTimeMillis()
        )
        downloadTaskDao.insert(task)
        downloadTaskDao.pauseTask(1, 5000L)
        val result = downloadTaskDao.get(1)

        assertEquals(TaskStatus.PAUSED, result?.status)
        assertEquals(5000L, result?.pausePoint)
    }

    @Test
    fun getActiveTasks() = runTest {
        val tasks = listOf(
            DownloadTask(id = 1, songCid = "s1", status = TaskStatus.DOWNLOADING, progress = 0f, pausePoint = 0L, createdAt = 1L),
            DownloadTask(id = 2, songCid = "s2", status = TaskStatus.PENDING, progress = 0f, pausePoint = 0L, createdAt = 2L),
            DownloadTask(id = 3, songCid = "s3", status = TaskStatus.COMPLETED, progress = 1f, pausePoint = 0L, createdAt = 3L)
        )
        tasks.forEach { downloadTaskDao.insert(it) }

        val result = downloadTaskDao.getActiveTasks().first()
        assertEquals(2, result.size)
    }

    @Test
    fun getTaskBySongCid() = runTest {
        val task = DownloadTask(
            id = 1, songCid = "s1", status = TaskStatus.DOWNLOADING,
            progress = 0.5f, pausePoint = 0L, createdAt = System.currentTimeMillis()
        )
        downloadTaskDao.insert(task)
        val result = downloadTaskDao.getBySongCid("s1")

        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun deleteTask() = runTest {
        val task = DownloadTask(
            id = 1, songCid = "s1", status = TaskStatus.COMPLETED,
            progress = 1f, pausePoint = 0L, createdAt = System.currentTimeMillis()
        )
        downloadTaskDao.insert(task)
        downloadTaskDao.delete(1)
        val result = downloadTaskDao.get(1)

        assertNull(result)
    }
}
