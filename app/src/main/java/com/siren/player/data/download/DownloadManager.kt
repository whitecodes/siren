package com.siren.player.data.download

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
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
import kotlinx.coroutines.withContext
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
        private const val KEY_DOWNLOAD_URI = "download_uri"
        private const val DEFAULT_DOWNLOAD_PATH = "downloads"
    }

    val downloadUri: Uri?
        get() {
            val uriString = prefs.getString(KEY_DOWNLOAD_URI, null)
            return uriString?.let { Uri.parse(it) }
        }

    val downloadPath: String
        get() {
            val uri = downloadUri
            return if (uri != null) {
                uriToPath(uri)
            } else {
                prefs.getString(KEY_DOWNLOAD_PATH, null) ?: getDefaultDownloadPath()
            }
        }

    fun isInternalStoragePath(): Boolean {
        val uri = downloadUri
        if (uri != null) {
            return uri.toString().startsWith(context.cacheDir.absolutePath)
        }
        return downloadPath.startsWith(context.cacheDir.absolutePath)
    }

    fun setDownloadPath(path: String) {
        prefs.edit().putString(KEY_DOWNLOAD_PATH, path).apply()
    }

    fun setDownloadUri(uri: Uri) {
        // 持久化 SAF 权限，确保 app 重启后 ExoPlayer 仍能读取 content:// URI
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Failed to persist SAF permission: ${e.message}")
        }
        prefs.edit()
            .putString(KEY_DOWNLOAD_URI, uri.toString())
            .putString(KEY_DOWNLOAD_PATH, uriToPath(uri))
            .apply()
    }

    private fun uriToPath(uri: Uri): String {
        try {
            // 解析 URI 的 path segment，获取完整路径
            // path 格式可能是 "/tree/primary:Music" 或 "/tree/primary%3AMusic"
            val path = uri.path ?: return "unknown"

            // 移除 /tree/ 前缀
            val cleanPath = if (path.startsWith("/tree/")) {
                path.removePrefix("/tree/")
            } else {
                path.removePrefix("/")
            }

            // 处理 primary: 或 primary%3A 格式
            val decodedPath = java.net.URLDecoder.decode(cleanPath, "UTF-8")
            if (decodedPath.startsWith("primary:")) {
                val relativePath = decodedPath.removePrefix("primary:")
                return "/sdcard/$relativePath"
            }

            // 处理其他格式，如 external_files:Music
            val colonIndex = decodedPath.indexOf(':')
            if (colonIndex > 0) {
                val volumeId = decodedPath.substring(0, colonIndex)
                val relativePath = decodedPath.substring(colonIndex + 1)
                return "/storage/$volumeId/$relativePath"
            }

            // 如果没有冒号，直接返回路径
            return "/$decodedPath"
        } catch (e: Exception) {
            android.util.Log.e("DownloadManager", "Failed to parse URI path: ${e.message}")
            return "unknown"
        }
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

    private fun getDownloadDocumentFile(): DocumentFile? {
        val uri = downloadUri ?: return null
        return DocumentFile.fromTreeUri(context, uri)
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

                // Use DocumentFile if URI is available
                val docDir = getDownloadDocumentFile()
                if (docDir != null) {
                    downloadWithDocumentFile(taskId, song, docDir, albumName, fileName, url, this@launch)
                } else {
                    downloadWithFilePath(taskId, song, albumName, fileName, url, this@launch)
                }
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

    private suspend fun downloadWithDocumentFile(
        taskId: Long,
        song: SongDetail,
        docDir: DocumentFile,
        albumName: String,
        fileName: String,
        url: String,
        coroutineScope: CoroutineScope
    ) {
        // Find or create album directory
        var albumDoc = docDir.findFile(albumName)
        if (albumDoc == null || !albumDoc.isDirectory) {
            albumDoc = docDir.createDirectory(albumName)
        }
        if (albumDoc == null) {
            throw Exception("Failed to create album directory: $albumName")
        }

        // Check if file already exists
        val existingFile = albumDoc.findFile(fileName)
        if (existingFile != null) {
            Log.d("DownloadManager", "File already exists: $fileName")
            songDao.updateLocalPath(song.cid, existingFile.uri.toString(), DownloadStatus.DOWNLOADED)
            DownloadQueue.emitEvent(DownloadEvent.Completed(song.cid))
            DownloadQueue.complete(taskId)
            return
        }

        // Create file
        val file = albumDoc.createFile("audio/*", fileName)
            ?: throw Exception("Failed to create file: $fileName")

        // Download and write to file
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val contentLength = body.contentLength()

        context.contentResolver.openOutputStream(file.uri)?.use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                var totalRead = 0L
                while (coroutineScope.isActive && bytesRead != -1) {
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

        Log.d("DownloadManager", "Download complete: ${file.uri}")
        songDao.updateLocalPath(song.cid, file.uri.toString(), DownloadStatus.DOWNLOADED)
        DownloadQueue.emitEvent(DownloadEvent.Completed(song.cid))
        DownloadQueue.complete(taskId)
    }

    private suspend fun downloadWithFilePath(
        taskId: Long,
        song: SongDetail,
        albumName: String,
        fileName: String,
        url: String,
        coroutineScope: CoroutineScope
    ) {
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
                while (coroutineScope.isActive && bytesRead != -1) {
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
    }

    fun cancel(taskId: Long) {
        activeJobs[taskId]?.cancel()
        DownloadQueue.removeTask(taskId)
    }

    fun cancelAll() {
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeJobs.clear()
    }

    /**
     * 重建数据库：扫描下载目录，根据文件更新数据库中歌曲的下载状态
     * @return 更新的歌曲数量
     */
    suspend fun rebuildDatabase(): Int = kotlinx.coroutines.withContext(Dispatchers.IO) {
        var updatedCount = 0

        // Use DocumentFile if URI is available
        val docDir = getDownloadDocumentFile()
        if (docDir != null) {
            return@withContext rebuildDatabaseWithDocumentFile(docDir)
        }

        // Fallback to file path
        val downloadDir = File(downloadPath)

        Log.d("DownloadManager", "Rebuild database: scan $downloadDir")
        Log.d("DownloadManager", "Download dir exists: ${downloadDir.exists()}, isDir: ${downloadDir.isDirectory}")

        if (!downloadDir.exists() || !downloadDir.isDirectory) {
            Log.d("DownloadManager", "Download directory does not exist")
            return@withContext 0
        }

        // Step 1: 确保数据库有专辑数据
        if (albumDao.getAllList().isEmpty()) {
            Log.d("DownloadManager", "Albums table is empty, fetching from network...")
            try {
                val albums = SirenApi.getAlbums()
                albumDao.insertAll(albums.map { album ->
                    com.siren.player.db.Album(
                        cid = album.cid,
                        name = album.name,
                        coverUrl = album.coverUrl,
                        artists = album.artistes.joinToString(","),
                        intro = album.intro
                    )
                })
                Log.d("DownloadManager", "Fetched ${albums.size} albums from network")
            } catch (e: Exception) {
                Log.e("DownloadManager", "Failed to fetch albums: ${e.message}", e)
                return@withContext 0
            }
        }

        // Step 2: 建立专辑名 -> albumCid 的索引
        val allAlbums = albumDao.getAllList()
        val albumNameToId = allAlbums.associate { it.name to it.cid }
        Log.d("DownloadManager", "Album index size: ${albumNameToId.size}")
        Log.d("DownloadManager", "Album names in DB: ${albumNameToId.keys.take(10)}...")

        // Step 3: 遍历下载目录，匹配并更新状态
        val allDirs = downloadDir.listFiles()
        Log.d("DownloadManager", "listFiles returned ${allDirs?.size ?: "null"} entries")
        allDirs?.forEach { Log.d("DownloadManager", "  dir entry: '${it.name}' isDir=${it.isDirectory}") }

        for (albumDir in allDirs ?: emptyArray()) {
            if (!albumDir.isDirectory) continue

            val albumCid = albumNameToId[albumDir.name]
            if (albumCid == null) {
                Log.d("DownloadManager", "Album folder '${albumDir.name}' not found in database, skipping")
                continue
            }

            Log.d("DownloadManager", "Processing album '${albumDir.name}' (cid=$albumCid)")

            // 懒加载：只在需要时拉取歌曲
            var albumSongs = songDao.getAlbumSongsList(albumCid)
            Log.d("DownloadManager", "  DB has ${albumSongs.size} songs for this album")
            if (albumSongs.isEmpty()) {
                Log.d("DownloadManager", "Album '${albumDir.name}' has no songs in DB, fetching from network...")
                try {
                    val networkSongs = SirenApi.getAlbumSongs(albumCid)
                    // 保存歌曲到数据库
                    networkSongs.forEachIndexed { index, song ->
                        songDao.insert(
                            com.siren.player.db.Song(
                                cid = song.cid,
                                name = song.name,
                                albumCid = song.albumCid,
                                artists = song.artists.joinToString(","),
                                order = index
                            )
                        )
                    }
                    albumSongs = songDao.getAlbumSongsList(albumCid)
                    Log.d("DownloadManager", "Fetched ${albumSongs.size} songs for album '${albumDir.name}'")
                } catch (e: Exception) {
                    Log.e("DownloadManager", "Failed to fetch songs for album '${albumDir.name}': ${e.message}", e)
                    continue
                }
            }

            // 建立歌曲名 -> Song 的索引
            val songNameToSong = albumSongs.associateBy { it.name }
            Log.d("DownloadManager", "  Song names in DB: ${songNameToSong.keys}")

            // 不使用 listFiles()（可能因 scoped storage 权限问题返回空）
            // 改为遍历数据库中的歌曲，直接检查文件是否存在
            for ((songName, song) in songNameToSong) {
                if (song.status == DownloadStatus.DOWNLOADED) {
                    Log.d("DownloadManager", "  - Song '${song.name}' already DOWNLOADED")
                    continue
                }

                // 尝试 mp3 和 wav 两种扩展名
                val mp3File = File(albumDir, "$songName.mp3")
                val wavFile = File(albumDir, "$songName.wav")
                val file = when {
                    mp3File.exists() -> mp3File
                    wavFile.exists() -> wavFile
                    else -> null
                }

                if (file != null) {
                    songDao.updateStatus(song.cid, DownloadStatus.DOWNLOADED)
                    songDao.updateLocalPath(song.cid, file.absolutePath, DownloadStatus.DOWNLOADED)
                    updatedCount++
                    Log.d("DownloadManager", "    ✓ Song '${song.name}' status updated to DOWNLOADED (${file.name})")
                } else {
                    Log.d("DownloadManager", "    ✗ Song '${song.name}' file not found in '${albumDir.name}'")
                }
            }
        }

        // Step 4: 反向清理——检查数据库中标记为 DOWNLOADED 但文件不存在的
        val downloadedSongs = songDao.getDownloadedSongs()
        for (song in downloadedSongs) {
            val localPath = song.localPath
            if (localPath != null && !File(localPath).exists()) {
                songDao.updateStatus(song.cid, DownloadStatus.NOT_DOWNLOADED)
                updatedCount++
                Log.d("DownloadManager", "Song '${song.name}' file not found, status updated to NOT_DOWNLOADED")
            }
        }

        Log.d("DownloadManager", "Rebuild database completed, updated $updatedCount songs")
        updatedCount
    }

    private suspend fun rebuildDatabaseWithDocumentFile(docDir: DocumentFile): Int {
        var updatedCount = 0

        Log.d("DownloadManager", "Rebuild database with DocumentFile: ${docDir.uri}")

        // Step 1: 确保数据库有专辑数据
        if (albumDao.getAllList().isEmpty()) {
            Log.d("DownloadManager", "Albums table is empty, fetching from network...")
            try {
                val albums = SirenApi.getAlbums()
                albumDao.insertAll(albums.map { album ->
                    com.siren.player.db.Album(
                        cid = album.cid,
                        name = album.name,
                        coverUrl = album.coverUrl,
                        artists = album.artistes.joinToString(","),
                        intro = album.intro
                    )
                })
                Log.d("DownloadManager", "Fetched ${albums.size} albums from network")
            } catch (e: Exception) {
                Log.e("DownloadManager", "Failed to fetch albums: ${e.message}", e)
                return 0
            }
        }

        // Step 2: 建立专辑名 -> albumCid 的索引
        val allAlbums = albumDao.getAllList()
        val albumNameToId = allAlbums.associate { it.name to it.cid }
        Log.d("DownloadManager", "Album index size: ${albumNameToId.size}")

        // Step 3: 遍历 DocumentFile 目录
        for (albumDoc in docDir.listFiles()) {
            if (!albumDoc.isDirectory) continue

            val albumName = albumDoc.name ?: continue
            val albumCid = albumNameToId[albumName] ?: continue

            Log.d("DownloadManager", "Processing album '$albumName' (cid=$albumCid)")

            // 懒加载：只在需要时拉取歌曲
            var albumSongs = songDao.getAlbumSongsList(albumCid)
            if (albumSongs.isEmpty()) {
                Log.d("DownloadManager", "Album '$albumName' has no songs in DB, fetching from network...")
                try {
                    val networkSongs = SirenApi.getAlbumSongs(albumCid)
                    networkSongs.forEachIndexed { index, song ->
                        songDao.insert(
                            com.siren.player.db.Song(
                                cid = song.cid,
                                name = song.name,
                                albumCid = song.albumCid,
                                artists = song.artists.joinToString(","),
                                order = index
                            )
                        )
                    }
                    albumSongs = songDao.getAlbumSongsList(albumCid)
                    Log.d("DownloadManager", "Fetched ${albumSongs.size} songs for album '$albumName'")
                } catch (e: Exception) {
                    Log.e("DownloadManager", "Failed to fetch songs for album '$albumName': ${e.message}", e)
                    continue
                }
            }

            // 建立歌曲名 -> Song 的索引
            val songNameToSong = albumSongs.associateBy { it.name }

            // 遍历专辑中的文件
            for (fileDoc in albumDoc.listFiles()) {
                if (!fileDoc.isFile) continue

                val fileName = fileDoc.name ?: continue
                val songName = fileName.substringBeforeLast(".")
                val song = songNameToSong[songName] ?: continue

                if (song.status == DownloadStatus.DOWNLOADED) continue

                songDao.updateStatus(song.cid, DownloadStatus.DOWNLOADED)
                songDao.updateLocalPath(song.cid, fileDoc.uri.toString(), DownloadStatus.DOWNLOADED)
                updatedCount++
                Log.d("DownloadManager", "    ✓ Song '${song.name}' status updated to DOWNLOADED")
            }
        }

        Log.d("DownloadManager", "Rebuild database completed, updated $updatedCount songs")
        return updatedCount
    }
}
