package com.siren.player

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.siren.player.data.api.SirenApi
import com.siren.player.player.MusicService
import com.siren.player.ui.SirenViewModel
import com.siren.player.ui.navigation.NavigationItem
import com.siren.player.ui.screens.AlbumDetailScreen
import com.siren.player.ui.screens.AlbumListScreen
import com.siren.player.ui.screens.DownloadQueueScreen
import com.siren.player.ui.screens.PlaylistScreen
import com.siren.player.ui.screens.PlayerScreen
import com.siren.player.ui.theme.SirenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var musicService by mutableStateOf<MusicService?>(null)
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Intent(this, MusicService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, BIND_AUTO_CREATE)
        }

        requestPermissions()

        setContent {
            SirenTheme {
                SirenApp(
                    musicService = musicService,
                    onPlaySong = ::playSong,
                    onPlayAlbum = ::playAlbum
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf<String>()
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (permissions.isNotEmpty()) {
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
                    .launch(permissions.toTypedArray())
            }
        }
    }

    private fun playSong(songCid: String, songName: String, albumCid: String) {
        val service = musicService ?: return
        Thread {
            val songs = SirenApi.getAlbumSongs(albumCid)
            val startIndex = songs.indexOfFirst { it.cid == songCid }.coerceAtLeast(0)
            val urls = songs.mapNotNull { song ->
                val detail = SirenApi.getSongDetail(song.cid) ?: return@mapNotNull null
                val cachedPath = runBlocking { (application as SirenApp).database.songDao().getLocalPath(song.cid) }
                val url = cachedPath ?: detail.sourceUrl
                url to detail.name
            }
            if (urls.isNotEmpty()) {
                runOnUiThread {
                    service.play(urls, startIndex)
                }
            }
        }.start()
    }

    private fun playAlbum(albumCid: String) {
        val service = musicService ?: return
        Thread {
            val songs = SirenApi.getAlbumSongs(albumCid)
            val urls = songs.mapNotNull { song ->
                val detail = SirenApi.getSongDetail(song.cid) ?: return@mapNotNull null
                val cachedPath = runBlocking { (application as SirenApp).database.songDao().getLocalPath(song.cid) }
                val url = cachedPath ?: detail.sourceUrl
                url to detail.name
            }
            if (urls.isNotEmpty()) {
                runOnUiThread {
                    service.play(urls)
                }
            }
        }.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SirenApp(
    musicService: MusicService?,
    onPlaySong: (String, String, String) -> Unit,
    onPlayAlbum: (String) -> Unit
) {
    val viewModel: SirenViewModel = viewModel()
    var selectedAlbumCid by remember { mutableStateOf<String?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    var currentNavItem by remember { mutableStateOf(NavigationItem.Album) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showPlayer && musicService != null) {
        PlayerScreen(
            musicService = musicService,
            onBack = { showPlayer = false }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "塞壬唱片",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationItem.entries.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentNavItem == item,
                        onClick = {
                            currentNavItem = item
                            selectedAlbumCid = null
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentNavItem.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                MiniPlayerBar(
                    musicService = musicService,
                    onClick = { showPlayer = true }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentNavItem) {
                    NavigationItem.Album -> {
                        if (selectedAlbumCid != null) {
                            AlbumDetailScreen(
                                viewModel = viewModel,
                                onBack = { selectedAlbumCid = null },
                                onPlaySong = onPlaySong
                            )
                        } else {
                            AlbumListScreen(
                                viewModel = viewModel,
                                onAlbumClick = { albumCid ->
                                    selectedAlbumCid = albumCid
                                    viewModel.openAlbum(albumCid)
                                }
                            )
                        }
                    }
                    NavigationItem.Playlist -> {
                        PlaylistScreen(musicService = musicService)
                    }
                    NavigationItem.DownloadQueue -> {
                        DownloadQueueScreen(viewModel = viewModel)
                    }
                    NavigationItem.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                    NavigationItem.About -> {
                        AboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SirenViewModel) {
    // Placeholder - will be implemented in T1.7
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Text("功能开发中...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AboutScreen() {
    // Placeholder - will be implemented in T1.7
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("关于", style = MaterialTheme.typography.headlineMedium)
        Text("塞壬唱片 v1.0", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MiniPlayerBar(
    musicService: MusicService?,
    onClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(musicService) {
        val svc = musicService ?: return@DisposableEffect onDispose {}
        val scope = CoroutineScope(Dispatchers.Main)
        val callback = {
            isPlaying = svc.isPlaying
            title = svc.currentTitle
        }
        svc.onPlaybackStateChange = callback
        callback()
        val job = scope.launch {
            while (true) {
                isPlaying = svc.isPlaying
                title = svc.currentTitle
                val dur = svc.duration
                progress = if (dur > 0) svc.currentPosition.toFloat() / dur.toFloat() else 0f
                delay(300)
            }
        }
        onDispose {
            svc.onPlaybackStateChange = null
            job.cancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title.ifEmpty { "塞壬唱片" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { musicService?.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = { musicService?.skipToNext() }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "下一曲",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
