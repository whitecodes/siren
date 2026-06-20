package com.siren.player.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.siren.player.data.api.SirenApi
import com.siren.player.data.api.SongDetail
import com.siren.player.db.DownloadStatus
import com.siren.player.db.SirenDatabase
import com.siren.player.db.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadItem(
    val id: Long = System.currentTimeMillis(),
    val songCid: String,
    val songName: String = "",
    val albumName: String = "",
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false
)

object DownloadQueue {
    private val _activeTasks = MutableStateFlow<List<DownloadItem>>(emptyList())
    val activeTasks: StateFlow<List<DownloadItem>> = _activeTasks.asStateFlow()

    private val _completedTasks = MutableStateFlow<List<DownloadItem>>(emptyList())
    val completedTasks: StateFlow<List<DownloadItem>> = _completedTasks.asStateFlow()

    private val activeMap = ConcurrentHashMap<Long, DownloadItem>()

    fun updateProgress(id: Long, progress: Float) {
        activeMap[id]?.let { item ->
            activeMap[id] = item.copy(progress = progress)
            _activeTasks.value = activeMap.values.toList()
        }
    }

    fun complete(id: Long) {
        activeMap.remove(id)?.let { item ->
            _completedTasks.value = listOf(item.copy(isCompleted = true)) + _completedTasks.value
            _activeTasks.value = activeMap.values.toList()
        }
    }

    fun fail(id: Long) {
        activeMap.remove(id)?.let { item ->
            _completedTasks.value = listOf(item.copy(isFailed = true)) + _completedTasks.value
            _activeTasks.value = activeMap.values.toList()
        }
    }

    fun addTask(item: DownloadItem) {
        activeMap[item.id] = item
        _activeTasks.value = activeMap.values.toList()
    }

    fun removeTask(id: Long) {
        activeMap.remove(id)
        _activeTasks.value = activeMap.values.toList()
    }
}

class DownloadManager(
    private val context: Context,
    private val database: SirenDatabase
) {
    private val songDao = database.songDao()
    private val albumDao = database.albumDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Use app's internal cache directory for downloads
    private val downloadDir = File(context.cacheDir, "downloads").also { it.mkdirs() }

    fun getDownloadPath(songCid: String, fileName: String): String {
        return File(downloadDir, "${songCid}_$fileName").absolutePath
    }

    suspend fun enqueue(song: SongDetail): Long {
        val taskId = System.currentTimeMillis()
        val album = albumDao.get(song.albumCid)

        val item = DownloadItem(
            id = taskId,
            songCid = song.cid,
            songName = song.name,
            albumName = album?.name ?: "未知专辑"
        )
        DownloadQueue.addTask(item)

        // Ensure song record exists
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
        startDownload(taskId, song, item.albumName)
        return taskId
    }

    private fun startDownload(taskId: Long, song: SongDetail, albumName: String) {
        val job = scope.launch {
            try {
                songDao.updateStatus(song.cid, DownloadStatus.DOWNLOADING)
                Log.d("DownloadManager", "Starting download for ${song.name}")

                // Fetch fresh URL
                val freshSong = SirenApi.getSongDetail(song.cid) ?: song
                val url = freshSong.sourceUrl
                Log.d("DownloadManager", "Download URL: $url")

                val ext = if (url.endsWith(".wav")) ".wav" else ".mp3"
                val fileName = "${song.name}$ext"
                val filePath = getDownloadPath(song.cid, fileName)
                val file = File(filePath)

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                Log.d("DownloadManager", "Content length: $contentLength")

                file.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead = input.read(buffer)
                        var totalRead = 0L
                        while (isActive && bytesRead != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            // Calculate progress - handle unknown content length
                            val progress = if (contentLength > 0) {
                                (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 0.99f)
                            } else {
                                0.5f // Unknown size, show indeterminate
                            }
                            DownloadQueue.updateProgress(taskId, progress)
                            if (totalRead % (8192 * 10) == 0L) {
                                Log.d("DownloadManager", "Downloaded $totalRead bytes, progress: $progress")
                            }
                            bytesRead = input.read(buffer)
                        }
                    }
                }

                Log.d("DownloadManager", "Download complete: $filePath")
                DownloadQueue.complete(taskId)
                songDao.updateLocalPath(song.cid, filePath, DownloadStatus.DOWNLOADED)
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed: ${e.message}", e)
                DownloadQueue.fail(taskId)
                songDao.updateStatus(song.cid, DownloadStatus.DOWNLOAD_FAILED)
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    fun cancel(taskId: Long) {
        activeJobs[taskId]?.cancel()
        DownloadQueue.removeTask(taskId)
    }

    fun cancelAll() {
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeJobs.clear()
    }
}
