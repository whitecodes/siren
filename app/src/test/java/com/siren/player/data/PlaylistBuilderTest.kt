package com.siren.player.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistBuilderTest {

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals(emptyList<String>(), PlaylistBuilder.buildAlbumPlaylist(emptySequence<List<String>>()))
    }

    @Test
    fun totalUnderLimitReturnsAll() {
        val albums = sequenceOf(listOf("a", "b"), listOf("c"), listOf("d", "e", "f"))
        assertEquals(
            listOf("a", "b", "c", "d", "e", "f"),
            PlaylistBuilder.buildAlbumPlaylist(albums, limit = 10)
        )
    }

    @Test
    fun exactlyAtLimitStopsAfterThatAlbum() {
        val albums = sequenceOf(listOf("a", "b"), listOf("c", "d"), listOf("e", "f"))
        // after 2 albums total = 4 == limit, third album not consumed
        assertEquals(
            listOf("a", "b", "c", "d"),
            PlaylistBuilder.buildAlbumPlaylist(albums, limit = 4)
        )
    }

    @Test
    fun overshootingAlbumIsAddedInFull() {
        val albums = sequenceOf(listOf("a", "b"), listOf("c", "d", "e"))
        // second album pushes total past 3, but is added whole
        assertEquals(
            listOf("a", "b", "c", "d", "e"),
            PlaylistBuilder.buildAlbumPlaylist(albums, limit = 3)
        )
    }

    @Test
    fun stopsAcrossMultipleAlbums() {
        val albums = sequenceOf(listOf("a"), listOf("b"), listOf("c", "d"), listOf("e"))
        assertEquals(
            listOf("a", "b", "c", "d"),
            PlaylistBuilder.buildAlbumPlaylist(albums, limit = 4)
        )
    }

    @Test
    fun sequenceIsLazyBeyondStopPoint() {
        var evaluated = 0
        val albums = sequence {
            for (albumSongs in listOf(listOf("a", "b"), listOf("c"), listOf("d", "e", "f"), listOf("g"))) {
                evaluated++
                yield(albumSongs)
            }
        }
        val result = PlaylistBuilder.buildAlbumPlaylist(albums, limit = 4)
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), result)
        // third album brings total to 6 >= 4, fourth album never evaluated
        assertEquals(3, evaluated)
    }

    @Test
    fun defaultLimitIsEight() {
        assertEquals(8, PlaylistBuilder.DEFAULT_LIMIT)
    }
}
