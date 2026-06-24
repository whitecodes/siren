package com.siren.player.data.download

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.siren.player.data.api.SirenApi
import com.siren.player.data.api.SongDetail
import com.siren.player.db.DownloadStatus
import com.siren.player.db.SirenDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed class DownloadEvent {
    data class Progress(val songCid: String, val progress: Float) : DownloadEvent()
    data class Completed(val songCid: String) : DownloadEvent()
    data class Failed(val songCid: String) : DownloadEvent()
}

object DownloadQueue {
    private val _activeTasks = MutableSharedFlow<List<DownloadItem>>(replay = 1)
    val activeTasks: SharedFlow<List<DownloadItem>> = _activeTasks.asSharedFlow()

    private val _completedTasks = MutableSharedFlow<List<DownloadItem>>(replay = 1)
    val completedTasks: SharedFlow<List<DownloadItem>> = _completedTasks.asSharedFlow()

    private val _events = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    private val activeMap = ConcurrentHashMap<Long, DownloadItem>()
    private val completedList = mutableListOf<DownloadItem>()

    suspend fun emitEvent(event: DownloadEvent) {
        _events.emit(event)
    }

    fun updateProgress(id: Long, progress: Float) {
        activeMap[id]?.let { item ->
            activeMap[id] = item.copy(progress = progress)
            _activeTasks.tryEmit(activeMap.values.toList())
        }
    }

    fun complete(id: Long) {
        activeMap.remove(id)?.let { item ->
            completedList.add(0, item.copy(isCompleted = true))
            _completedTasks.tryEmit(completedList.toList())
            _activeTasks.tryEmit(activeMap.values.toList())
        }
    }

    fun fail(id: Long) {
        activeMap.remove(id)?.let { item ->
            completedList.add(0, item.copy(isFailed = true))
            _completedTasks.tryEmit(completedList.toList())
            _activeTasks.tryEmit(activeMap.values.toList())
        }
    }

    fun addTask(item: DownloadItem) {
        activeMap[item.id] = item
        _activeTasks.tryEmit(activeMap.values.toList())
    }

    fun removeTask(id: Long) {
        activeMap.remove(id)
        _activeTasks.tryEmit(activeMap.values.toList())
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
    private val prefs: SharedPreferences = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val KEY_DOWNLOAD_PATH = "download_path"
        private const val DEFAULT_DOWNLOAD_PATH = "downloads"
    }

    val downloadPath: String
        get() = prefs.getString(KEY_DOWNLOAD_PATH, null) ?: getDefaultDownloadPath()

    fun isInternalStoragePath(): Boolean {
        return downloadPath.startsWith(context.cacheDir.absolutePath)
    }

    fun setDownloadPath(path: String) {
        prefs.edit().putString(KEY_DOWNLOAD_PATH, path).apply()
    }

    private fun getDefaultDownloadPath(): String {
        return File(context.cacheDir, DEFAULT_DOWNLOAD_PATH).absolutePath
    }

    fun getDownloadDir(): File {
        val dir = File(downloadPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDownloadFilePath(albumName: String, fileName: String): String {
        val albumDir = File(getDownloadDir(), albumName)
        if (!albumDir.exists()) {
            albumDir.mkdirs()
        }
        return File(albumDir, fileName).absolutePath
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

        startDownload(taskId, song, item.albumName)
        return taskId
    }

    private fun startDownload(taskId: Long, song: SongDetail, albumName: String) {
        val job = scope.launch {
            try {
                songDao.updateStatus(song.cid, DownloadStatus.DOWNLOADING)
                DownloadQueue.emitEvent(DownloadEvent.Progress(song.cid, 0f))
                Log.d("DownloadManager", "Starting download for ${song.name}")

                val freshSong = SirenApi.getSongDetail(song.cid) ?: song
                val url = freshSong.sourceUrl

                val ext = if (url.endsWith(".wav")) ".wav" else ".mp3"
                val fileName = "${song.name}$ext"
                val filePath = getDownloadFilePath(albumName, fileName)
                val file = File(filePath)

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()

                file.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead = input.read(buffer)
                        var totalRead = 0L
                        while (isActive && bytesRead != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (contentLength > 0) {
                                (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 0.99f)
                            } else {
                                0.5f
                            }
                            DownloadQueue.updateProgress(taskId, progress)
                            DownloadQueue.emitEvent(DownloadEvent.Progress(song.cid, progress))
                            bytesRead = input.read(buffer)
                        }
                    }
                }

                Log.d("DownloadManager", "Download complete: $filePath")
                songDao.updateLocalPath(song.cid, filePath, DownloadStatus.DOWNLOADED)
                DownloadQueue.emitEvent(DownloadEvent.Completed(song.cid))
                DownloadQueue.complete(taskId)
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed: ${e.message}", e)
                songDao.updateStatus(song.cid, DownloadStatus.DOWNLOAD_FAILED)
                DownloadQueue.emitEvent(DownloadEvent.Failed(song.cid))
                DownloadQueue.fail(taskId)
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
