package com.siren.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    Album("album", "专辑", Icons.Default.Album),
    Playlist("playlist", "播放列表", Icons.Default.List),
    DownloadQueue("download_queue", "下载队列", Icons.Default.Download),
    Settings("settings", "设置", Icons.Default.Settings),
    About("about", "关于", Icons.Default.Info)
}
