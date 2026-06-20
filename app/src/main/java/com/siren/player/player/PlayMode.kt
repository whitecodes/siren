package com.siren.player.player

enum class PlayMode(val displayName: String) {
    SINGLE_LOOP("单曲循环"),
    SINGLE_STOP("单曲结束"),
    LIST_LOOP("列表循环"),
    LIST_STOP("列表停止"),
    LIST_SHUFFLE("列表随机");

    fun next(): PlayMode {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }
}
