package com.siren.player.data.repository

import android.content.Context
import com.siren.player.data.api.AlbumInfo
import com.siren.player.data.api.SirenApi
import com.siren.player.data.api.SongDetail
import com.siren.player.db.CachedSong
import com.siren.player.db.SirenDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class MusicRepository(context: Context) {

    private val db = SirenDatabase.create(context)
    private val dao = db.songCacheDao()
    private val cacheDir = File(context.cacheDir, "music").also { it.mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getAlbums(): List<AlbumInfo> = withContext(Dispatchers.IO) {
        SirenApi.getAlbums()
    }

    suspend fun getAlbumSongs(albumCid: String) = withContext(Dispatchers.IO) {
        SirenApi.getAlbumSongs(albumCid)
    }

    suspend fun search(keyword: String): List<AlbumInfo> = withContext(Dispatchers.IO) {
        SirenApi.search(keyword)
    }

    suspend fun getSongDetail(cid: String): SongDetail? = withContext(Dispatchers.IO) {
        SirenApi.getSongDetail(cid)
    }

    suspend fun getCachedPath(cid: String): String? = dao.getLocalPath(cid)

    suspend fun downloadAndCache(song: SongDetail, onProgress: (Float) -> Unit = {}): String? =
        withContext(Dispatchers.IO) {
            val existing = dao.getLocalPath(song.cid)
            if (existing != null && File(existing).exists()) return@withContext existing

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

                dao.insert(
                    CachedSong(
                        cid = song.cid,
                        name = song.name,
                        albumCid = song.albumCid,
                        artists = song.artists.joinToString(","),
                        sourceUrl = song.sourceUrl,
                        localPath = file.absolutePath
                    )
                )

                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                file.delete()
                null
            }
        }

    suspend fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
