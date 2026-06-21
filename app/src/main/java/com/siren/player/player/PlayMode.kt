package com.siren.player.player

import com.siren.player.R

enum class PlayMode(val displayNameResId: Int) {
    SINGLE_LOOP(R.string.mode_single_loop),
    SINGLE_STOP(R.string.mode_single_stop),
    LIST_LOOP(R.string.mode_list_loop),
    LIST_STOP(R.string.mode_list_stop),
    LIST_SHUFFLE(R.string.mode_list_shuffle);

    fun next(): PlayMode {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }
}
