package com.siren.player.data.repository

import android.content.Context
import com.siren.player.data.api.AlbumInfo
import com.siren.player.data.api.SirenApi
import com.siren.player.data.api.SongDetail
import com.siren.player.data.api.SongInfo
import com.siren.player.db.Album
import com.siren.player.db.DownloadStatus
import com.siren.player.db.SirenDatabase
import com.siren.player.db.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class MusicRepository(context: Context) {

    private val db = SirenDatabase.create(context)
    private val albumDao = db.albumDao()
    private val songDao = db.songDao()
    private val cacheDir = File(context.cacheDir, "music").also { it.mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Get albums from cache first, then network if empty
    suspend fun getAlbums(forceRefresh: Boolean = false): List<AlbumInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = albumDao.getAllList()
            if (cached.isNotEmpty()) {
                return@withContext cached.map { album ->
                    AlbumInfo(
                        cid = album.cid,
                        name = album.name,
                        coverUrl = album.coverUrl,
                        artistes = album.artists.split(","),
                        intro = album.intro
                    )
                }
            }
        }
        // Fetch from network and cache
        val albums = SirenApi.getAlbums()
        saveAlbums(albums)
        albums
    }

    // Get album songs from cache first, then network if empty
    suspend fun getAlbumSongs(albumCid: String, forceRefresh: Boolean = false): List<SongInfo> = withContext(Dispatchers.IO) {
        val songs = if (!forceRefresh) {
            val cachedCount = songDao.countAlbumSongs(albumCid)
            if (cachedCount > 0) {
                val cached = songDao.getAlbumSongsList(albumCid)
                cached.map { song ->
                    SongInfo(
                        cid = song.cid,
                        name = song.name,
                        albumCid = song.albumCid,
                        artists = song.artists.split(",")
                    )
                }
            } else {
                // Fetch from network and cache
                val networkSongs = SirenApi.getAlbumSongs(albumCid)
                saveSongs(networkSongs)
                networkSongs
            }
        } else {
            // Force refresh from network
            val networkSongs = SirenApi.getAlbumSongs(albumCid)
            saveSongs(networkSongs)
            networkSongs
        }
        // Return in reverse order
        songs.reversed()
    }

    // Get album detail with intro
    suspend fun getAlbumDetail(cid: String): com.siren.player.data.api.AlbumDetail? = withContext(Dispatchers.IO) {
        SirenApi.getAlbumDetail(cid)
    }

    suspend fun search(keyword: String): List<AlbumInfo> = withContext(Dispatchers.IO) {
        SirenApi.search(keyword)
    }

    suspend fun getSongDetail(cid: String): SongDetail? = withContext(Dispatchers.IO) {
        SirenApi.getSongDetail(cid)
    }

    suspend fun getCachedPath(cid: String): String? = songDao.getLocalPath(cid)

    suspend fun downloadAndCache(song: SongDetail, onProgress: (Float) -> Unit = {}): String? =
        withContext(Dispatchers.IO) {
            val existing = songDao.getLocalPath(song.cid)
            if (existing != null && File(existing).exists()) return@withContext existing

            // Update status to downloading
            songDao.updateStatus(song.cid, DownloadStatus.DOWNLOADING)

            val ext = if (song.sourceUrl.endsWith(".wav")) ".wav" else ".mp3"
            val file = File(cacheDir, "${song.cid}$ext")

            try {
                val request = Request.Builder().url(song.sourceUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: return@withContext null
                val contentLength = body.contentLength().toFloat()

                file.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                onProgress(totalRead / contentLength)
                            }
                        }
                    }
                }

                // Update song record with local path and downloaded status
                songDao.updateLocalPath(song.cid, file.absolutePath, DownloadStatus.DOWNLOADED)

                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                file.delete()
                songDao.updateStatus(song.cid, DownloadStatus.DOWNLOAD_FAILED)
                null
            }
        }

    suspend fun saveAlbums(albums: List<AlbumInfo>) {
        albumDao.insertAll(albums.map { album ->
            Album(
                cid = album.cid,
                name = album.name,
                coverUrl = album.coverUrl,
                artists = album.artistes.joinToString(","),
                intro = album.intro
            )
        })
    }

    suspend fun saveSongs(songs: List<SongInfo>) {
        songs.forEach { song ->
            songDao.insert(
                Song(
                    cid = song.cid,
                    name = song.name,
                    albumCid = song.albumCid,
                    artists = song.artists.joinToString(",")
                )
            )
        }
    }

    suspend fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
