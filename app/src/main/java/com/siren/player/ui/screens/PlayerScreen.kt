package com.siren.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.siren.player.R
import com.siren.player.player.MusicService
import com.siren.player.player.PlayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    musicService: MusicService?,
    onBack: () -> Unit,
    onPlaylist: () -> Unit = {},
    coverUrl: String? = null
) {
    var isPlaying by remember { mutableStateOf(musicService?.isPlaying == true) }
    var currentPos by remember { mutableLongStateOf(musicService?.currentPosition ?: 0) }
    var duration by remember { mutableLongStateOf(musicService?.duration ?: 0) }
    var title by remember { mutableStateOf(musicService?.currentTitle ?: "") }
    var playMode by remember { mutableStateOf(musicService?.playMode ?: PlayMode.LIST_STOP) }

    DisposableEffect(musicService) {
        val service = musicService ?: return@DisposableEffect onDispose {}
        service.onPlaybackStateChange = {
            isPlaying = service.isPlaying
            currentPos = service.currentPosition
            duration = service.duration
            title = service.currentTitle
            playMode = service.playMode
        }
        val job = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                if (service.isPlaying) {
                    currentPos = service.currentPosition
                    duration = service.duration
                }
                delay(500)
            }
        }
        onDispose {
            service.onPlaybackStateChange = null
            job.cancel()
        }
    }

    // Dynamic colors from album art
    val albumColors = rememberAlbumColors(coverUrl)
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0a0a1a)

    val gradientColors = if (albumColors != null) {
        val dominant = albumColors.dominant
        val vibrant = albumColors.vibrant
        if (dominant != null && vibrant != null) {
            val baseAlpha = if (isDark) 0.35f else 0.15f
            val base = if (isDark) Color(0xFF0a0a1a) else Color(0xFFf5f5f5)
            listOf(
                dominant.copy(alpha = baseAlpha).compositeOver(base),
                vibrant.copy(alpha = baseAlpha * 0.6f).compositeOver(base),
                base
            )
        } else {
            val surface = MaterialTheme.colorScheme.surface
            val background = MaterialTheme.colorScheme.background
            listOf(surface, background)
        }
    } else {
        val surface = MaterialTheme.colorScheme.surface
        val background = MaterialTheme.colorScheme.background
        listOf(surface, background)
    }

    // Dynamic accent color: darkVibrant for dark theme, lightVibrant for light theme
    val accentColor = if (albumColors != null) {
        if (isDark) {
            albumColors.darkVibrant ?: albumColors.vibrant ?: MaterialTheme.colorScheme.primary
        } else {
            albumColors.lightVibrant ?: albumColors.vibrant ?: MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = 1200f
                    )
                )
        ) {
            // Transparent TopAppBar
            TopAppBar(
                title = { Text(stringResource(R.string.now_playing)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Album cover
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = stringResource(R.string.album_cover),
                        modifier = Modifier.size(280.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Song title
                Text(
                    text = title.ifEmpty { stringResource(R.string.not_playing) },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress bar — View-based Material Slider
                var isSeeking by remember { mutableStateOf(false) }
                val durationFloat = duration.toFloat().coerceAtLeast(1f)
                val activeColorInt = accentColor.toArgb()
                val vibrantColorInt = albumColors?.vibrant?.toArgb() ?: activeColorInt
                val inactiveColorInt = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f).toArgb()

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        // 使用自定义主题（包含细轨道+隐藏滑块样式）
                        val themedContext = android.view.ContextThemeWrapper(context, R.style.Theme_Siren_Slider)
                        com.google.android.material.slider.Slider(themedContext).apply {
                            valueFrom = 0f
                            valueTo = durationFloat
                            stepSize = 0f

                            addOnChangeListener { _, _, fromUser ->
                                if (fromUser) {
                                    isSeeking = true
                                }
                            }

                            addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                                override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                                    isSeeking = true
                                }
                                override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                                    isSeeking = false
                                    musicService?.seekTo(slider.value.toLong())
                                }
                            })
                        }
                    },
                    update = { slider ->
                        slider.valueTo = durationFloat
                        if (!isSeeking) {
                            slider.value = currentPos.toFloat().coerceIn(0f, slider.valueTo)
                        }

                        // 颜色设置
                        slider.thumbTintList = android.content.res.ColorStateList.valueOf(vibrantColorInt)
                        slider.trackActiveTintList = android.content.res.ColorStateList.valueOf(activeColorInt)
                        slider.trackInactiveTintList = android.content.res.ColorStateList.valueOf(inactiveColorInt)
                        slider.haloTintList = android.content.res.ColorStateList.valueOf(activeColorInt)
                    }
                )

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPos),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play mode
                    IconButton(onClick = { musicService?.cyclePlayMode() }) {
                        Icon(
                            when (playMode) {
                                PlayMode.SINGLE_LOOP -> Icons.Default.RepeatOne
                                PlayMode.LIST_SHUFFLE -> Icons.Default.Shuffle
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = stringResource(playMode.displayNameResId),
                            tint = if (playMode != PlayMode.LIST_STOP) accentColor
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Previous
                    FilledIconButton(
                        onClick = { musicService?.skipToPrevious() },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous))
                    }

                    // Play/Pause
                    FilledIconButton(
                        onClick = { musicService?.togglePlayPause() },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = accentColor
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next
                    FilledIconButton(
                        onClick = { musicService?.skipToNext() },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next))
                    }

                    // Playlist button
                    IconButton(onClick = onPlaylist) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = stringResource(R.string.playlist),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple alpha compositing: overlay color over background.
 */
private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    return Color(
        red = this.red * alpha + background.red * (1 - alpha),
        green = this.green * alpha + background.green * (1 - alpha),
        blue = this.blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
