package com.siren.player.data

object PlaylistBuilder {
    const val DEFAULT_LIMIT = 8

    /** 按专辑顺序累加整张专辑，直到累计 >= limit（越界专辑整张加入，可略超 limit）。 */
    fun <T> buildAlbumPlaylist(
        albumSongLists: Sequence<List<T>>,
        limit: Int = DEFAULT_LIMIT
    ): List<T> {
        val result = ArrayList<T>(limit)
        val iterator = albumSongLists.iterator()
        // 在拉取下一张专辑前先判断，保证停止点之后的专辑不会被求值
        while (result.size < limit && iterator.hasNext()) {
            result.addAll(iterator.next())
        }
        return result
    }
}
