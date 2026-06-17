package com.siren.player.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.siren.player.MainActivity
import com.siren.player.SirenApp

class MusicService : Service() {

    private val binder = LocalBinder()
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    var onPlaybackStateChange: (() -> Unit)? = null
    var onTrackChange: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    onPlaybackStateChange?.invoke()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    onTrackChange?.invoke()
                }

                override fun onPlayerError(error: PlaybackException) {
                    onPlaybackStateChange?.invoke()
                }
            })
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun play(urls: List<Pair<String, String>>, startIndex: Int = 0) {
        val player = exoPlayer ?: return
        val mediaItems = urls.map { (url, title) ->
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0)
        player.prepare()
        player.play()
    }

    fun playSingle(url: String, title: String) {
        val player = exoPlayer ?: return
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun skipToNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer ?: return
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun setRepeatMode(mode: Int) {
        exoPlayer?.repeatMode = mode
    }

    fun setShuffleMode(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
    }

    val isPlaying: Boolean get() = exoPlayer?.isPlaying == true
    val currentPosition: Long get() = exoPlayer?.currentPosition ?: 0
    val duration: Long get() = exoPlayer?.duration?.takeIf { it > 0 } ?: 0
    val currentMediaItemIndex: Int get() = exoPlayer?.currentMediaItemIndex ?: 0
    val mediaItemCount: Int get() = exoPlayer?.mediaItemCount ?: 0
    val currentTitle: String
        get() = exoPlayer?.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
    val repeatMode: Int get() = exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
    val shuffleModeEnabled: Boolean get() = exoPlayer?.shuffleModeEnabled == true

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        exoPlayer?.release()
        super.onDestroy()
    }
}
