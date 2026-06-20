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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.siren.player.data.api.SongInfo
import com.siren.player.ui.SirenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: SirenViewModel,
    onPlaySong: (songCid: String, songName: String, albumCid: String) -> Unit
) {
    val album by viewModel.currentAlbum.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloading = remember { mutableStateMapOf<String, Float>() }
    val downloadedSongs = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        album != null -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Album cover and info
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
                        // Album intro
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
                            text = "${album!!.songs.size} 首歌曲",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                // Song list - simplified style
                itemsIndexed(album!!.songs) { index, song ->
                    SongItem(
                        song = song,
                        index = index,
                        isDownloading = downloading.containsKey(song.cid),
                        isDownloaded = downloadedSongs[song.cid] == true,
                        onPlay = { onPlaySong(song.cid, song.name, song.albumCid) },
                        onDownload = {
                            scope.launch {
                                downloading[song.cid] = 0f
                                val detail = viewModel.getSongDetail(song.cid)
                                if (detail != null) {
                                    viewModel.downloadAndCache(detail) { progress ->
                                        downloading[song.cid] = progress
                                    }
                                }
                                downloading.remove(song.cid)
                                downloadedSongs[song.cid] = true
                            }
                        }
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
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
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
        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else if (isDownloaded) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已下载",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "下载",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
