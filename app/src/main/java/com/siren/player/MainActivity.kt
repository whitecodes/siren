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
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
import com.siren.player.ui.theme.LanguageManager
import com.siren.player.ui.theme.LanguageMode
import com.siren.player.ui.theme.SirenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

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

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = LanguageManager.getLocale()
        if (locale != null) {
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: android.content.res.Configuration?) {
        val locale = LanguageManager.getLocale()
        if (locale != null && overrideConfiguration != null) {
            overrideConfiguration.setLocale(locale)
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            SirenTheme {
                SirenApp(
                    musicService = musicService,
                    onPlaySong = ::playSong,
                    onPlayAlbum = ::playAlbum,
                    onLanguageChange = ::showLanguageChangeDialog
                )
            }
        }

        // Start service after activity is visible
        lifecycleScope.launch {
            delay(500)
            Intent(this@MainActivity, MusicService::class.java).also { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                bindService(intent, connection, BIND_AUTO_CREATE)
            }
        }
    }

    fun showLanguageChangeDialog(mode: LanguageMode) {
        android.app.AlertDialog.Builder(this)
            .setTitle("切换语言")
            .setMessage("切换语言需要重启应用，是否立即重启？")
            .setPositiveButton("确定") { _, _ ->
                LanguageManager.setLanguageMode(mode)
                // 重启应用
                val packageManager = packageManager
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Runtime.getRuntime().exit(0)
            }
            .setNegativeButton("取消", null)
            .show()
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
        Thread {
            val detail = SirenApi.getSongDetail(songCid) ?: return@Thread
            val cachedPath = runBlocking { db.songDao().getLocalPath(songCid) }
            val url = cachedPath ?: detail.sourceUrl
            
            runOnUiThread {
                val added = service.addToPlaylist(url, detail.name)
                if (!added) {
                    // Song already in playlist, skip to it
                    val playlist = service.getPlaylist()
                    val index = playlist.indexOfFirst { it.first == url }
                    if (index >= 0) {
                        service.skipToIndex(index)
                    }
                } else if (service.mediaItemCount > 1) {
                    // addToPlaylist handles play when playlist was empty
                    // For non-empty playlists, skip to the newly added song
                    service.skipToIndex(service.mediaItemCount - 1)
                }
            }
        }.start()
    }

    private fun playAlbum(albumCid: String) {
        val service = musicService ?: return
        val db = (application as SirenApp).database
        val repository = com.siren.player.data.repository.MusicRepository(application)
        Thread {
            val songs = kotlinx.coroutines.runBlocking { repository.getAlbumSongs(albumCid) }
            val urls = songs.mapNotNull { song ->
                val detail = SirenApi.getSongDetail(song.cid) ?: return@mapNotNull null
                val cachedPath = runBlocking { db.songDao().getLocalPath(song.cid) }
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
    onPlayAlbum: (String) -> Unit,
    onLanguageChange: (LanguageMode) -> Unit
) {
    val viewModel: SirenViewModel = viewModel()
    var selectedAlbumCid by remember { mutableStateOf<String?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    var currentNavItem by remember { mutableStateOf(NavigationItem.Album) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Handle back navigation
    BackHandler {
        when {
            showPlayer -> showPlayer = false
            selectedAlbumCid != null -> selectedAlbumCid = null
            drawerState.isOpen -> scope.launch { drawerState.close() }
            else -> {
                (context as? ComponentActivity)?.moveTaskToBack(true)
            }
        }
    }

    if (showPlayer && musicService != null) {
        val currentCoverUrl = viewModel.currentAlbum.collectAsState().value?.coverUrl
        PlayerScreen(
            musicService = musicService,
            onBack = { showPlayer = false },
            onPlaylist = {
                showPlayer = false
                currentNavItem = NavigationItem.Playlist
            },
            coverUrl = currentCoverUrl
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(260.dp),
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                // Brand Header with cropped logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Use local drawable for the Monster Siren logo
                    coil.compose.AsyncImage(
                        model = R.drawable.siren_logo,
                        contentDescription = "Monster Siren Logo",
                        modifier = Modifier
                            .height(100.dp)
                            .aspectRatio(400f/243f),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                
                NavigationItem.entries.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId)) },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = currentNavItem == item,
                        onClick = {
                            currentNavItem = item
                            selectedAlbumCid = null
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        shape = RoundedCornerShape(0.dp)
                    )
                }
            }
        }
    ) {
        var showSearch by remember { mutableStateOf(false) }
        val searchQuery by viewModel.searchQuery.collectAsState()
        var showPlayModeMenu by remember { mutableStateOf(false) }
        var currentPlayMode by remember { mutableStateOf(musicService?.playMode ?: PlayMode.LIST_STOP) }

        // Poll play mode when on playlist screen
        DisposableEffect(musicService, currentNavItem) {
            if (currentNavItem == NavigationItem.Playlist && musicService != null) {
                val scope = CoroutineScope(Dispatchers.Main)
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
                            Text(stringResource(currentNavItem.titleResId))
                        }
                    },
                    navigationIcon = {
                        if (currentNavItem == NavigationItem.Album && selectedAlbumCid != null) {
                            IconButton(onClick = { selectedAlbumCid = null }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentNavItem == NavigationItem.Album) {
                            if (selectedAlbumCid == null) {
                                IconButton(onClick = { showSearch = !showSearch }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { viewModel.loadAlbums(forceRefresh = true) }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.refresh),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Row {
                                    IconButton(onClick = { viewModel.refreshAlbum() }) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.refresh),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(onClick = {
                                        musicService?.let { svc ->
                                            viewModel.currentAlbum.value?.songs?.forEach { song ->
                                                val detail = viewModel.getSongDetail(song.cid)
                                                if (detail != null) {
                                                    val cachedPath = runBlocking { viewModel.database.songDao().getLocalPath(song.cid) }
                                                    val url = cachedPath ?: detail.sourceUrl
                                                    svc.addToPlaylist(url, detail.name)
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.play_album),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(onClick = {
                                        musicService?.let { svc ->
                                            viewModel.currentAlbum.value?.songs?.forEach { song ->
                                                val detail = viewModel.getSongDetail(song.cid)
                                                if (detail != null) {
                                                    val cachedPath = runBlocking { viewModel.database.songDao().getLocalPath(song.cid) }
                                                    val url = cachedPath ?: detail.sourceUrl
                                                    svc.addToPlaylist(url, detail.name)
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = stringResource(R.string.add_album_to_playlist),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else if (currentNavItem == NavigationItem.Playlist) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clickable { showPlayModeMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(currentPlayMode.displayNameResId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        if (showPlayModeMenu) Icons.Default.ArrowDropUp
                                        else Icons.Default.ArrowDropDown,
                                        contentDescription = stringResource(R.string.select_play_mode),
                                        tint = MaterialTheme.colorScheme.onSurface,
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
                                                    text = stringResource(mode.displayNameResId),
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
                            IconButton(onClick = { musicService?.clearPlaylist() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.clear_playlist),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                if (currentNavItem == NavigationItem.Album && selectedAlbumCid == null && showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchAlbums(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.search_album)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                }

                when (currentNavItem) {
                    NavigationItem.Album -> {
                        if (selectedAlbumCid != null) {
                            AlbumDetailScreen(
                                viewModel = viewModel,
                                musicService = musicService,
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
                        DownloadQueueScreen()
                    }
                    NavigationItem.Settings -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onLanguageChange = onLanguageChange
                        )
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
        val callback = {
            isPlaying = svc.isPlaying
            title = svc.currentTitle
        }
        svc.onPlaybackStateChange = callback
        callback()
        val scope = CoroutineScope(Dispatchers.Main)
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
                text = title.ifEmpty { stringResource(R.string.album_name) },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { musicService?.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = { musicService?.skipToNext() }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.next),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
