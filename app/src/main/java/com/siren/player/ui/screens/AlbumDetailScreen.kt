package com.siren.player.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.siren.player.R
import com.siren.player.data.api.SongInfo
import com.siren.player.data.download.DownloadEvent
import com.siren.player.data.download.DownloadQueue
import com.siren.player.db.DownloadStatus
import com.siren.player.player.MusicService
import com.siren.player.ui.SirenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: SirenViewModel,
    musicService: MusicService?,
    onPlaySong: (songCid: String, songName: String, albumCid: String) -> Unit,
) {
    val album by viewModel.currentAlbum.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadStatus = remember { mutableStateMapOf<String, DownloadStatus>() }
    val downloadProgress = remember { mutableStateMapOf<String, Float>() }
    val scope = rememberCoroutineScope()

    // Initialize download status from database
    DisposableEffect(album) {
        val db = viewModel.database
        val job = scope.launch {
            album?.songs?.forEach { song ->
                val songEntity = withContext(Dispatchers.IO) { db.songDao().get(song.cid) }
                if (songEntity != null) {
                    downloadStatus[song.cid] = songEntity.status
                }
            }
        }
        onDispose { job.cancel() }
    }

    // Observe download events
    DisposableEffect(Unit) {
        val job = scope.launch {
            DownloadQueue.events.collect { event ->
                when (event) {
                    is DownloadEvent.Progress -> {
                        downloadStatus[event.songCid] = DownloadStatus.DOWNLOADING
                        downloadProgress[event.songCid] = event.progress
                    }
                    is DownloadEvent.Completed -> {
                        downloadStatus[event.songCid] = DownloadStatus.DOWNLOADED
                        downloadProgress.remove(event.songCid)
                    }
                    is DownloadEvent.Failed -> {
                        downloadStatus[event.songCid] = DownloadStatus.DOWNLOAD_FAILED
                        downloadProgress.remove(event.songCid)
                    }
                }
            }
        }
        onDispose { job.cancel() }
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        album != null -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = album!!.coverUrl,
                            contentDescription = album!!.name,
                            modifier = Modifier.size(180.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = album!!.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = album!!.artistes.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (album!!.intro.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album!!.intro,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.songs_count, album!!.songs.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                itemsIndexed(album!!.songs) { index, song ->
                    val status = downloadStatus[song.cid] ?: DownloadStatus.NOT_DOWNLOADED
                    val progress = downloadProgress[song.cid]
                    SongItem(
                        song = song,
                        index = index,
                        downloadStatus = status,
                        downloadProgress = progress,
                        onPlay = { onPlaySong(song.cid, song.name, song.albumCid) },
                        onAddToPlaylist = {
                            scope.launch {
                                val detail = viewModel.getSongDetail(song.cid)
                                if (detail != null) {
                                    withContext(Dispatchers.Main) {
                                        musicService?.addToPlaylist(detail.sourceUrl, detail.name)
                                    }
                                }
                            }
                        },
                        onDownload = { }
                    )
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: SongInfo,
    index: Int,
    downloadStatus: DownloadStatus,
    downloadProgress: Float?,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = song.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_to_playlist),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        when (downloadStatus) {
            DownloadStatus.DOWNLOADED -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            DownloadStatus.DOWNLOADING -> {
                CircularProgressIndicator(
                    progress = { downloadProgress ?: 0f },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            else -> {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = stringResource(R.string.download),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
