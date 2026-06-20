package com.siren.player.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.siren.player.MainActivity
import com.siren.player.R

class MusicService : Service() {

    private val binder = LocalBinder()
    private var exoPlayer: ExoPlayer? = null
    private var currentPlayMode = PlayMode.ALBUM_STOP
    var onPlaybackStateChange: (() -> Unit)? = null
    var onTrackChange: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        handlePlaybackEnd()
                    }
                    updateNotification()
                    onPlaybackStateChange?.invoke()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateNotification()
                    onTrackChange?.invoke()
                }

                override fun onPlayerError(error: PlaybackException) {
                    onPlaybackStateChange?.invoke()
                }
            })
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "塞壬唱片音乐播放控制"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val player = exoPlayer ?: return
        if (!player.isPlaying && player.currentMediaItem == null) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "塞壬唱片"

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("塞壬唱片")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(player.isPlaying)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun handlePlaybackEnd() {
        when (currentPlayMode) {
            PlayMode.SINGLE_LOOP -> {
                exoPlayer?.let { player ->
                    player.seekTo(0)
                    player.play()
                }
            }
            PlayMode.SINGLE_STOP -> {
                exoPlayer?.pause()
            }
            PlayMode.ALBUM_LOOP -> {
                exoPlayer?.let { player ->
                    if (player.currentMediaItemIndex >= player.mediaItemCount - 1) {
                        player.seekTo(0)
                        player.play()
                    }
                }
            }
            PlayMode.ALBUM_STOP -> {
                // Default behavior - stop at end
            }
            PlayMode.ALBUM_SHUFFLE -> {
                exoPlayer?.let { player ->
                    if (player.currentMediaItemIndex >= player.mediaItemCount - 1) {
                        player.seekTo(0)
                        player.play()
                    }
                }
            }
        }
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
        applyPlayMode()
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
        } else if (currentPlayMode == PlayMode.ALBUM_LOOP || currentPlayMode == PlayMode.ALBUM_SHUFFLE) {
            player.seekTo(0)
            player.play()
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

    fun setPlayMode(mode: PlayMode) {
        currentPlayMode = mode
        applyPlayMode()
        onPlaybackStateChange?.invoke()
    }

    private fun applyPlayMode() {
        val player = exoPlayer ?: return
        when (currentPlayMode) {
            PlayMode.SINGLE_LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            PlayMode.SINGLE_STOP -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            PlayMode.ALBUM_LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            PlayMode.ALBUM_STOP -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            PlayMode.ALBUM_SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = true
            }
        }
    }

    fun cyclePlayMode() {
        setPlayMode(currentPlayMode.next())
    }

    fun getPlaylist(): List<Pair<String, String>> {
        val player = exoPlayer ?: return emptyList()
        return (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            val title = item.mediaMetadata.title?.toString() ?: "Unknown"
            val uri = item.localConfiguration?.uri?.toString() ?: ""
            uri to title
        }
    }

    fun skipToIndex(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index.toLong())
            if (!player.isPlaying) {
                player.play()
            }
            onPlaybackStateChange?.invoke()
        }
    }

    val isPlaying: Boolean get() = exoPlayer?.isPlaying == true
    val currentPosition: Long get() = exoPlayer?.currentPosition ?: 0
    val duration: Long get() = exoPlayer?.duration?.takeIf { it > 0 } ?: 0
    val currentMediaItemIndex: Int get() = exoPlayer?.currentMediaItemIndex ?: 0
    val mediaItemCount: Int get() = exoPlayer?.mediaItemCount ?: 0
    val currentTitle: String
        get() = exoPlayer?.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
    val playMode: PlayMode get() = currentPlayMode
    val repeatMode: Int get() = exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
    val shuffleModeEnabled: Boolean get() = exoPlayer?.shuffleModeEnabled == true

    override fun onDestroy() {
        exoPlayer?.release()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "siren_music_channel"
        private const val NOTIFICATION_ID = 1
    }
}
