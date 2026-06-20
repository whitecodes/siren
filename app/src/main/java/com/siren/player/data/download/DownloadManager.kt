package com.siren.player.data.download

import android.content.Context
import android.os.Environment
import com.siren.player.data.api.SirenApi
import com.siren.player.data.api.SongDetail
import com.siren.player.db.DownloadStatus
import com.siren.player.db.SirenDatabase
import com.siren.player.db.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val database: SirenDatabase
) {
    private val songDao = database.songDao()
    private val taskDao = database.downloadTaskDao()
    private val albumDao = database.albumDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val activeTasks: Flow<List<com.siren.player.db.DownloadTask>> = taskDao.getActiveTasks()

    fun getDownloadPath(albumName: String, fileName: String): String {
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            albumName
        )
        return File(musicDir, fileName).absolutePath
    }

    suspend fun enqueue(song: SongDetail): Long {
        val task = com.siren.player.db.DownloadTask(
            songCid = song.cid,
            status = TaskStatus.PENDING,
            progress = 0f,
            pausePoint = 0L
        )
        val taskId = taskDao.insert(task)

        // Also ensure song record exists
        songDao.insert(
            com.siren.player.db.Song(
                cid = song.cid,
                name = song.name,
                albumCid = song.albumCid,
                artists = song.artists.joinToString(","),
                status = DownloadStatus.NOT_DOWNLOADED
            )
        )

        // Start download
        startDownload(taskId, song)
        return taskId
    }

    private fun startDownload(taskId: Long, song: SongDetail, resumeFrom: Long = 0L) {
        val job = scope.launch {
            taskDao.updateStatus(taskId, TaskStatus.DOWNLOADING)
            songDao.updateStatus(song.cid, DownloadStatus.DOWNLOADING)
            var lastProgress = resumeFrom

            try {
                // Fetch fresh URL (URLs expire)
                val freshSong = SirenApi.getSongDetail(song.cid) ?: song
                val url = freshSong.sourceUrl

                // Determine file path
                val album = albumDao.get(song.albumCid)
                val albumName = album?.name ?: "Unknown"
                val ext = if (url.endsWith(".wav")) ".wav" else ".mp3"
                val fileName = "${song.name}$ext"
                val filePath = getDownloadPath(albumName, fileName)
                val file = File(filePath)

                // Create parent directory
                file.parentFile?.mkdirs()

                val requestBuilder = Request.Builder().url(url)
                if (resumeFrom > 0) {
                    requestBuilder.addHeader("Range", "bytes=$resumeFrom-")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength().toFloat()

                if (resumeFrom > 0 && file.exists()) {
                    // Append to existing file
                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(resumeFrom)
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead = input.read(buffer)
                            while (isActive && bytesRead != -1) {
                                raf.write(buffer, 0, bytesRead)
                                lastProgress += bytesRead
                                val progress = if (contentLength > 0) {
                                    lastProgress / (resumeFrom + contentLength)
                                } else {
                                    0f
                                }
                                taskDao.updateProgress(taskId, progress.coerceAtMost(1f))
                                bytesRead = input.read(buffer)
                            }
                        }
                    }
                } else {
                    // Write new file
                    file.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead = input.read(buffer)
                            while (isActive && bytesRead != -1) {
                                output.write(buffer, 0, bytesRead)
                                lastProgress += bytesRead
                                val progress = if (contentLength > 0) {
                                    lastProgress / contentLength
                                } else {
                                    0f
                                }
                                taskDao.updateProgress(taskId, progress.coerceAtMost(1f))
                                bytesRead = input.read(buffer)
                            }
                        }
                    }
                }

                if (isActive) {
                    // Download complete
                    taskDao.updateStatus(taskId, TaskStatus.COMPLETED)
                    taskDao.updateProgress(taskId, 1f)
                    songDao.updateLocalPath(song.cid, filePath, DownloadStatus.DOWNLOADED)
                } else {
                    // Cancelled - save pause point
                    taskDao.pauseTask(taskId, TaskStatus.CANCELLED, lastProgress)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isActive) {
                    taskDao.updateStatus(taskId, TaskStatus.FAILED)
                    songDao.updateStatus(song.cid, DownloadStatus.DOWNLOAD_FAILED)
                }
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    fun pause(taskId: Long) {
        activeJobs[taskId]?.let { job ->
            job.cancel()
            scope.launch {
                val task = taskDao.get(taskId)
                if (task != null && task.status == TaskStatus.DOWNLOADING) {
                    taskDao.pauseTask(taskId, TaskStatus.PAUSED, task.pausePoint)
                    songDao.updateStatus(task.songCid, DownloadStatus.NOT_DOWNLOADED)
                }
            }
        }
    }

    fun resume(taskId: Long) {
        scope.launch {
            val task = taskDao.get(taskId) ?: return@launch
            if (task.status == TaskStatus.PAUSED) {
                val song = SirenApi.getSongDetail(task.songCid) ?: return@launch
                startDownload(taskId, song, task.pausePoint)
            }
        }
    }

    fun cancel(taskId: Long) {
        activeJobs[taskId]?.let { job ->
            job.cancel()
        }
        scope.launch {
            taskDao.updateStatus(taskId, TaskStatus.CANCELLED)
            val task = taskDao.get(taskId)
            if (task != null) {
                songDao.updateStatus(task.songCid, DownloadStatus.NOT_DOWNLOADED)
            }
        }
    }

    fun cancelAll() {
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeJobs.clear()
        scope.launch {
            taskDao.getActiveTasks().collect { tasks ->
                tasks.forEach { task ->
                    taskDao.updateStatus(task.id, TaskStatus.CANCELLED)
                }
            }
        }
    }
}
