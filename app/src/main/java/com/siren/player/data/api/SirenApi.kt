package com.siren.player.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log
import java.util.concurrent.TimeUnit

object SirenApi {

    private const val BASE_URL = "https://monster-siren.hypergryph.com/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun get(path: String): JSONObject? {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .build()
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            Log.d("SirenApi", "GET $path -> ${response.code}, body length=${body.length}")
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("SirenApi", "GET $path failed", e)
            null
        }
    }

    fun getAlbums(): List<AlbumInfo> {
        val json = get("/albums") ?: return emptyList()
        val list = json.optJSONArray("data") ?: return emptyList()
        return (0 until list.length()).mapNotNull { i ->
            val obj = list.getJSONObject(i)
            AlbumInfo(
                cid = obj.optString("cid"),
                name = obj.optString("name"),
                coverUrl = obj.optString("coverUrl"),
                artistes = obj.optJSONArray("artistes")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
            )
        }
    }

    fun getAlbumDetail(cid: String): AlbumDetail? {
        val json = get("/album/$cid/data") ?: return null
        val data = json.optJSONObject("data") ?: return null
        return AlbumDetail(
            cid = data.optString("cid"),
            name = data.optString("name"),
            intro = data.optString("intro"),
            coverUrl = data.optString("coverUrl"),
            artistes = data.optJSONArray("artistes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )
    }

    fun getSongs(): List<SongInfo> {
        val json = get("/songs") ?: return emptyList()
        val data = json.optJSONObject("data") ?: return emptyList()
        val list = data.optJSONArray("list") ?: return emptyList()
        return (0 until list.length()).mapNotNull { i ->
            val obj = list.getJSONObject(i)
            SongInfo(
                cid = obj.optString("cid"),
                name = obj.optString("name"),
                albumCid = obj.optString("albumCid"),
                artists = obj.optJSONArray("artists")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
            )
        }
    }

    fun getAlbumSongs(albumCid: String): List<SongInfo> {
        return getSongs().filter { it.albumCid == albumCid }
    }

    fun getSongDetail(cid: String): SongDetail? {
        val json = get("/song/$cid") ?: return null
        val data = json.optJSONObject("data") ?: return null
        return SongDetail(
            cid = data.optString("cid"),
            name = data.optString("name"),
            albumCid = data.optString("albumCid"),
            sourceUrl = data.optString("sourceUrl"),
            lyricUrl = data.optString("lyricUrl").takeIf { it != "null" && it.isNotEmpty() },
            artists = data.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )
    }

    fun search(keyword: String): List<AlbumInfo> {
        val json = get("/search?keyword=$keyword") ?: return emptyList()
        val data = json.optJSONObject("data") ?: return emptyList()
        val albums = data.optJSONObject("albums") ?: return emptyList()
        val list = albums.optJSONArray("list") ?: return emptyList()
        return (0 until list.length()).mapNotNull { i ->
            val obj = list.getJSONObject(i)
            AlbumInfo(
                cid = obj.optString("cid"),
                name = obj.optString("name"),
                coverUrl = obj.optString("coverUrl"),
                artistes = obj.optJSONArray("artistes")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class AlbumInfo(
    val cid: String,
    val name: String,
    val coverUrl: String,
    val artistes: List<String>,
    val intro: String = ""
)

data class AlbumDetail(
    val cid: String,
    val name: String,
    val intro: String,
    val coverUrl: String,
    val artistes: List<String>
)

data class SongInfo(
    val cid: String,
    val name: String,
    val albumCid: String,
    val artists: List<String>
)

data class SongDetail(
    val cid: String,
    val name: String,
    val albumCid: String,
    val sourceUrl: String,
    val lyricUrl: String?,
    val artists: List<String>
)
