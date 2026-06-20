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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import com.siren.player.player.PlayMode
import com.siren.player.ui.SirenViewModel
import com.siren.player.ui.navigation.NavigationItem
import com.siren.player.ui.screens.AlbumDetailScreen
import com.siren.player.ui.screens.AlbumListScreen
import com.siren.player.ui.screens.DownloadQueueScreen
import com.siren.player.ui.screens.PlaylistScreen
import com.siren.player.ui.screens.PlayerScreen
import com.siren.player.ui.screens.SettingsScreen
import com.siren.player.ui.screens.AboutScreen
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
        val db = (application as SirenApp).database
        val downloadManager = com.siren.player.data.download.DownloadManager(this, db)
        Thread {
            val songs = SirenApi.getAlbumSongs(albumCid)
            android.util.Log.d("SirenPlayer", "playSong: songCid=$songCid, albumCid=$albumCid, songsCount=${songs.size}")
            val originalIndex = songs.indexOfFirst { it.cid == songCid }
            android.util.Log.d("SirenPlayer", "playSong: originalIndex=$originalIndex")
            var newIndex = 0
            var foundIndex = 0
            val urls = songs.mapNotNull { song ->
                val detail = SirenApi.getSongDetail(song.cid) ?: return@mapNotNull null
                val cachedPath = runBlocking { db.songDao().getLocalPath(song.cid) }
                // Trigger auto-cache if not cached
                if (cachedPath == null) {
                    runBlocking { downloadManager.enqueue(detail) }
                }
                val url = cachedPath ?: detail.sourceUrl
                if (song.cid == songCid) {
                    foundIndex = newIndex
                    android.util.Log.d("SirenPlayer", "playSong: found target song at newIndex=$newIndex")
                }
                newIndex++
                url to detail.name
            }
            android.util.Log.d("SirenPlayer", "playSong: urls.size=${urls.size}, foundIndex=$foundIndex")
            if (urls.isNotEmpty()) {
                runOnUiThread {
                    android.util.Log.d("SirenPlayer", "playSong: calling service.play with startIndex=$foundIndex")
                    service.play(urls, foundIndex)
                }
            }
        }.start()
    }

    private fun playAlbum(albumCid: String) {
        val service = musicService ?: return
        val db = (application as SirenApp).database
        val downloadManager = com.siren.player.data.download.DownloadManager(this, db)
        Thread {
            val songs = SirenApi.getAlbumSongs(albumCid)
            val urls = songs.mapNotNull { song ->
                val detail = SirenApi.getSongDetail(song.cid) ?: return@mapNotNull null
                val cachedPath = runBlocking { db.songDao().getLocalPath(song.cid) }
                // Trigger auto-cache if not cached
                if (cachedPath == null) {
                    runBlocking { downloadManager.enqueue(detail) }
                }
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
        var showSearch by remember { mutableStateOf(false) }
        val searchQuery by viewModel.searchQuery.collectAsState()
        var showPlayModeMenu by remember { mutableStateOf(false) }
        var currentPlayMode by remember { mutableStateOf(musicService?.playMode ?: PlayMode.ALBUM_STOP) }

        // Poll play mode when on playlist screen
        DisposableEffect(musicService, currentNavItem) {
            if (currentNavItem == NavigationItem.Playlist && musicService != null) {
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                val job = scope.launch {
                    while (true) {
                        currentPlayMode = musicService.playMode
                        delay(200)
                    }
                }
                onDispose { job.cancel() }
            }
            onDispose {}
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (currentNavItem == NavigationItem.Album && selectedAlbumCid != null) {
                            Text(
                                text = viewModel.currentAlbum.collectAsState().value?.name ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(currentNavItem.title)
                        }
                    },
                    navigationIcon = {
                        if (currentNavItem == NavigationItem.Album && selectedAlbumCid != null) {
                            IconButton(onClick = { selectedAlbumCid = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        }
                    },
                    actions = {
                        if (currentNavItem == NavigationItem.Album) {
                            if (selectedAlbumCid == null) {
                                // Album list view - show search and refresh
                                IconButton(onClick = { showSearch = !showSearch }) {
                                    Icon(Icons.Default.Search, contentDescription = "搜索")
                                }
                                IconButton(onClick = { viewModel.loadAlbums(forceRefresh = true) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                                }
                            } else {
                                // Album detail view - show refresh only
                                IconButton(onClick = { viewModel.refreshAlbum() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                                }
                            }
                        } else if (currentNavItem == NavigationItem.Playlist) {
                            // Playlist view - show play mode dropdown (single line)
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clickable { showPlayModeMenu = !showPlayModeMenu }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentPlayMode.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                    Icon(
                                        if (showPlayModeMenu) Icons.Default.ArrowDropUp
                                        else Icons.Default.ArrowDropDown,
                                        contentDescription = "选择播放模式",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPlayModeMenu,
                                    onDismissRequest = { showPlayModeMenu = false }
                                ) {
                                    PlayMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = mode.displayName,
                                                    color = if (currentPlayMode == mode) MaterialTheme.colorScheme.primary
                                                           else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                musicService?.setPlayMode(mode)
                                                currentPlayMode = mode
                                                showPlayModeMenu = false
                                            }
                                        )
                                    }
                                }
                            }
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
                // Search field for Album screen
                if (currentNavItem == NavigationItem.Album && selectedAlbumCid == null && showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchAlbums(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("搜索专辑...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                }

                when (currentNavItem) {
                    NavigationItem.Album -> {
                        if (selectedAlbumCid != null) {
                            AlbumDetailScreen(
                                viewModel = viewModel,
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
                        // Pass empty lists for now - download queue is session-only
                        DownloadQueueScreen(
                            activeTasks = emptyList(),
                            completedTasks = emptyList()
                        )
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
