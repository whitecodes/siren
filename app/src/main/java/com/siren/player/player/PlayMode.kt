package com.siren.player.player

enum class PlayMode(val displayName: String) {
    SINGLE_LOOP("单曲循环"),
    SINGLE_STOP("单曲停止"),
    ALBUM_LOOP("专辑循环"),
    ALBUM_STOP("专辑停止"),
    ALBUM_SHUFFLE("专辑内随机");

    fun next(): PlayMode {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }
}
