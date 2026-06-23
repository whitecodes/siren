package com.siren.player.data.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SirenApiParsingTest {

    @Test
    fun parseAlbumInfo() {
        val json = JSONObject().apply {
            put("cid", "7761")
            put("name", "测试专辑")
            put("coverUrl", "https://example.com/cover.jpg")
            put("artistes", JSONArray().apply {
                put("塞壬唱片-MSR")
            })
        }

        val album = AlbumInfo(
            cid = json.optString("cid"),
            name = json.optString("name"),
            coverUrl = json.optString("coverUrl"),
            artistes = json.optJSONArray("artistes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )

        assertEquals("7761", album.cid)
        assertEquals("测试专辑", album.name)
        assertEquals("https://example.com/cover.jpg", album.coverUrl)
        assertEquals(1, album.artistes.size)
        assertEquals("塞壬唱片-MSR", album.artistes[0])
    }

    @Test
    fun parseAlbumInfoWithEmptyArtistes() {
        val json = JSONObject().apply {
            put("cid", "1")
            put("name", "test")
            put("coverUrl", "url")
            put("artistes", JSONArray())
        }

        val album = AlbumInfo(
            cid = json.optString("cid"),
            name = json.optString("name"),
            coverUrl = json.optString("coverUrl"),
            artistes = json.optJSONArray("artistes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )

        assertEquals(0, album.artistes.size)
    }

    @Test
    fun parseAlbumDetail() {
        val json = JSONObject().apply {
            put("cid", "7761")
            put("name", "测试专辑")
            put("intro", "这是一张专辑简介")
            put("coverUrl", "https://example.com/cover.jpg")
            put("artistes", JSONArray().apply {
                put("艺术家1")
                put("艺术家2")
            })
        }

        val detail = AlbumDetail(
            cid = json.optString("cid"),
            name = json.optString("name"),
            intro = json.optString("intro"),
            coverUrl = json.optString("coverUrl"),
            artistes = json.optJSONArray("artistes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )

        assertEquals("7761", detail.cid)
        assertEquals("测试专辑", detail.name)
        assertEquals("这是一张专辑简介", detail.intro)
        assertEquals(2, detail.artistes.size)
    }

    @Test
    fun parseSongInfo() {
        val json = JSONObject().apply {
            put("cid", "880394")
            put("name", "测试歌曲")
            put("albumCid", "7761")
            put("artists", JSONArray().apply {
                put("塞壬唱片-MSR")
            })
        }

        val song = SongInfo(
            cid = json.optString("cid"),
            name = json.optString("name"),
            albumCid = json.optString("albumCid"),
            artists = json.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )

        assertEquals("880394", song.cid)
        assertEquals("测试歌曲", song.name)
        assertEquals("7761", song.albumCid)
        assertEquals(1, song.artists.size)
    }

    @Test
    fun parseSongDetail() {
        val json = JSONObject().apply {
            put("cid", "880394")
            put("name", "测试歌曲")
            put("albumCid", "7761")
            put("sourceUrl", "https://example.com/song.wav")
            put("lyricUrl", "https://example.com/lyric.lrc")
            put("artists", JSONArray().apply {
                put("塞壬唱片-MSR")
            })
        }

        val detail = SongDetail(
            cid = json.optString("cid"),
            name = json.optString("name"),
            albumCid = json.optString("albumCid"),
            sourceUrl = json.optString("sourceUrl"),
            lyricUrl = json.optString("lyricUrl").takeIf { it != "null" && it.isNotEmpty() },
            artists = json.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )

        assertEquals("880394", detail.cid)
        assertEquals("测试歌曲", detail.name)
        assertEquals("https://example.com/song.wav", detail.sourceUrl)
        assertEquals("https://example.com/lyric.lrc", detail.lyricUrl)
    }

    @Test
    fun parseSongDetailWithNullLyric() {
        val json = JSONObject().apply {
            put("cid", "1")
            put("name", "test")
            put("albumCid", "a1")
            put("sourceUrl", "url")
            put("lyricUrl", "null")
            put("artists", JSONArray())
        }

        val lyricUrl = json.optString("lyricUrl").takeIf { it != "null" && it.isNotEmpty() }
        assertNull(lyricUrl)
    }

    @Test
    fun parseAlbumsList() {
        val json = JSONObject().apply {
            put("data", JSONArray().apply {
                put(JSONObject().apply {
                    put("cid", "1")
                    put("name", "album1")
                    put("coverUrl", "url1")
                    put("artistes", JSONArray().put("artist1"))
                })
                put(JSONObject().apply {
                    put("cid", "2")
                    put("name", "album2")
                    put("coverUrl", "url2")
                    put("artistes", JSONArray().put("artist2"))
                })
            })
        }

        val list = json.optJSONArray("data") ?: return
        val albums = (0 until list.length()).mapNotNull { i ->
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

        assertEquals(2, albums.size)
        assertEquals("1", albums[0].cid)
        assertEquals("2", albums[1].cid)
    }

    @Test
    fun parseSongsList() {
        val json = JSONObject().apply {
            put("data", JSONObject().apply {
                put("list", JSONArray().apply {
                    put(JSONObject().apply {
                        put("cid", "s1")
                        put("name", "song1")
                        put("albumCid", "a1")
                        put("artists", JSONArray().put("artist1"))
                    })
                })
            })
        }

        val data = json.optJSONObject("data") ?: return
        val list = data.optJSONArray("list") ?: return
        val songs = (0 until list.length()).mapNotNull { i ->
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

        assertEquals(1, songs.size)
        assertEquals("s1", songs[0].cid)
    }
}
