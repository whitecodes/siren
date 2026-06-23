package com.siren.player.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.siren.player.R

enum class NavigationItem(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector
) {
    Album("album", R.string.nav_album, Icons.Default.Album),
    Playlist("playlist", R.string.nav_playlist, Icons.Default.List),
    DownloadQueue("download_queue", R.string.nav_download_queue, Icons.Default.Download),
    Settings("settings", R.string.nav_settings, Icons.Default.Settings),
    About("about", R.string.nav_about, Icons.Default.Info)
}
