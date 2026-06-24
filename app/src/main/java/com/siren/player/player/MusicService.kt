package com.siren.player.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import com.siren.player.MainActivity
import com.siren.player.R

class MusicService : Service() {

    private val binder = LocalBinder()
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentPlayMode = PlayMode.LIST_STOP
    private var _currentTrackIndex = 0
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

        // 必须在 startForegroundService() 后5秒内调用 startForeground()，否则 ANR
        // 先用简单的 notification 立即调用 startForeground()
        try {
            startForeground(NOTIFICATION_ID, fallbackNotification())
        } catch (e: Exception) {
            // 后台启动时不允许创建前台服务，忽略异常
            android.util.Log.e("MusicService", "Failed to start foreground: ${e.message}")
            stopSelf()
            return
        }

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        handlePlaybackEnd()
                    }
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

        // Create MediaSession — ExoPlayer is the player, session owns the notification
        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(sessionActivity)
            .build()

        // 初始化完成后，更新为完整的 media notification
        updateNotification()
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

    @OptIn(UnstableApi::class)
    private fun buildNotification(): Notification {
        val session = mediaSession ?: return fallbackNotification()

        val title = exoPlayer?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "塞壬唱片"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("塞壬唱片")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(session.sessionActivity)
            .setStyle(MediaStyle(session).setShowActionsInCompactView(0, 1, 2))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(exoPlayer?.isPlaying == true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", mediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            .addAction(
                if (exoPlayer?.isPlaying == true) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                "Play/Pause",
                mediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            )
            .addAction(android.R.drawable.ic_media_next, "Next", mediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_NEXT))
            .build()
    }

    private fun fallbackNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("塞壬唱片")
            .setContentText("音乐播放服务")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun mediaButtonPendingIntent(keyCode: Int): PendingIntent {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        return PendingIntent.getBroadcast(
            this, keyCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MediaButtonReceiver forwards media button events here via onCustomCommand
        // But as a bound service we also handle direct media button intents
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            keyEvent?.let { handleMediaButton(it.keyCode) }
        }
        return START_STICKY
    }

    private fun handleMediaButton(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> togglePlayPause()
            KeyEvent.KEYCODE_MEDIA_NEXT -> skipToNext()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> skipToPrevious()
        }
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
            PlayMode.LIST_LOOP -> {
                exoPlayer?.let { player ->
                    if (player.currentMediaItemIndex >= player.mediaItemCount - 1) {
                        player.seekTo(0)
                        player.play()
                    }
                }
            }
            PlayMode.LIST_STOP -> {
                // Default behavior - stop at end
            }
            PlayMode.LIST_SHUFFLE -> {
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
        android.util.Log.d("SirenPlayer", "MusicService.play: startIndex=$startIndex, urls.size=${urls.size}")
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
        android.util.Log.d("SirenPlayer", "MusicService.play: player started, currentMediaItemIndex=${player.currentMediaItemIndex}")
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
        onPlaybackStateChange?.invoke()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun skipToNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        } else if (currentPlayMode == PlayMode.LIST_LOOP || currentPlayMode == PlayMode.LIST_SHUFFLE) {
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
            PlayMode.LIST_LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            PlayMode.LIST_STOP -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            PlayMode.LIST_SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
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

    fun addToPlaylist(url: String, title: String): Boolean {
        val player = exoPlayer ?: return false
        // Check for duplicates by URL
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val existingUrl = item.localConfiguration?.uri?.toString() ?: ""
            if (existingUrl == url) {
                return false // Already in playlist
            }
        }
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
            .build()

        if (player.mediaItemCount == 0) {
            // First item: need to set, prepare and play
            player.setMediaItem(mediaItem)
            applyPlayMode()
            player.prepare()
            player.play()
        } else {
            player.addMediaItem(mediaItem)
        }
        return true
    }

    fun clearPlaylist() {
        val player = exoPlayer ?: return
        val currentUrl = player.currentMediaItem?.localConfiguration?.uri?.toString()
        val currentTitle = player.currentMediaItem?.mediaMetadata?.title?.toString()

        // Clear all items
        player.clearMediaItems()

        // If there was a playing item, keep it
        if (currentUrl != null && currentTitle != null) {
            val mediaItem = MediaItem.Builder()
                .setUri(currentUrl)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(currentTitle)
                        .build()
                )
                .build()
            player.addMediaItem(mediaItem)
            player.seekTo(0, 0)
            if (player.isPlaying) {
                player.play()
            }
        }
        onPlaybackStateChange?.invoke()
        onTrackChange?.invoke()
    }

    fun skipToIndex(index: Int) {
        val player = exoPlayer ?: return
        android.util.Log.d("SirenPlayer", "skipToIndex: index=$index, mediaItemCount=${player.mediaItemCount}, currentTitle=${player.currentMediaItem?.mediaMetadata?.title}")
        if (index in 0 until player.mediaItemCount) {
            _currentTrackIndex = index
            player.seekTo(index, 0)
            if (!player.isPlaying) {
                player.play()
            }
            onTrackChange?.invoke()
            onPlaybackStateChange?.invoke()
            android.util.Log.d("SirenPlayer", "skipToIndex: after seekTo, newTitle=${player.getMediaItemAt(index).mediaMetadata?.title}")
        }
    }

    val currentMediaItemIndex: Int
        get() {
            val player = exoPlayer ?: return _currentTrackIndex
            val realIndex = player.currentMediaItemIndex
            if (realIndex != _currentTrackIndex && realIndex >= 0) {
                _currentTrackIndex = realIndex
            }
            return _currentTrackIndex
        }

    val isPlaying: Boolean get() = exoPlayer?.isPlaying == true
    val currentPosition: Long get() = exoPlayer?.currentPosition ?: 0
    val duration: Long get() = exoPlayer?.duration?.takeIf { it > 0 } ?: 0
    val mediaItemCount: Int get() = exoPlayer?.mediaItemCount ?: 0
    val currentTitle: String
        get() = exoPlayer?.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
    val playMode: PlayMode get() = currentPlayMode
    val repeatMode: Int get() = exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
    val shuffleModeEnabled: Boolean get() = exoPlayer?.shuffleModeEnabled == true

    override fun onDestroy() {
        mediaSession?.release()
        exoPlayer?.release()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "siren_music_channel"
        private const val NOTIFICATION_ID = 1
    }
}
