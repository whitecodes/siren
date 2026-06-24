package com.siren.player.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siren.player.data.api.AlbumInfo
import com.siren.player.data.api.SirenApi
import com.siren.player.data.api.SongDetail
import com.siren.player.data.api.SongInfo
import com.siren.player.data.download.DownloadManager
import com.siren.player.data.repository.MusicRepository
import com.siren.player.db.SirenDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiAlbum(
    val cid: String,
    val name: String,
    val coverUrl: String,
    val artistes: List<String>,
    val intro: String = "",
    val songs: List<SongInfo> = emptyList()
)

class SirenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val database = SirenDatabase.create(application)
    val downloadManager = DownloadManager(application, database)

    private val _albums = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val albums: StateFlow<List<AlbumInfo>> = _albums

    private val _currentAlbum = MutableStateFlow<UiAlbum?>(null)
    val currentAlbum: StateFlow<UiAlbum?> = _currentAlbum

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val searchResults: StateFlow<List<AlbumInfo>> = _searchResults

    private val _downloadPath = MutableStateFlow(downloadManager.downloadPath)
    val downloadPath: StateFlow<String> = _downloadPath

    private val _downloadUri = MutableStateFlow(downloadManager.downloadUri)
    val downloadUri: StateFlow<Uri?> = _downloadUri

    init {
        loadAlbums(forceRefresh = false)
    }

    fun loadAlbums(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) { repository.getAlbums(forceRefresh) }
                Log.d("SirenViewModel", "Loaded ${result.size} albums")
                _albums.value = result
            } catch (e: Exception) {
                Log.e("SirenViewModel", "loadAlbums failed", e)
                _error.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openAlbum(albumCid: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get songs from cache or network
                val songs = withContext(Dispatchers.IO) { repository.getAlbumSongs(albumCid, forceRefresh) }
                // Get album detail with intro (always from network for intro)
                val albumDetail = if (forceRefresh || _currentAlbum.value?.cid != albumCid) {
                    withContext(Dispatchers.IO) { repository.getAlbumDetail(albumCid) }
                } else {
                    null
                }
                val album = _albums.value.find { it.cid == albumCid }
                _currentAlbum.value = UiAlbum(
                    cid = albumCid,
                    name = albumDetail?.name ?: album?.name ?: "",
                    coverUrl = albumDetail?.coverUrl ?: album?.coverUrl ?: "",
                    artistes = albumDetail?.artistes ?: album?.artistes ?: emptyList(),
                    intro = albumDetail?.intro ?: _currentAlbum.value?.intro ?: "",
                    songs = songs
                )
            } catch (e: Exception) {
                _error.value = "加载专辑失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAlbum() {
        val albumCid = _currentAlbum.value?.cid ?: return
        openAlbum(albumCid, forceRefresh = true)
    }

    fun getSongDetail(cid: String): SongDetail? {
        return try {
            kotlinx.coroutines.runBlocking {
                withContext(Dispatchers.IO) { repository.getSongDetail(cid) }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun searchAlbums(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _searchResults.value = withContext(Dispatchers.IO) { repository.search(query) }
            } catch (e: Exception) {
                _error.value = "搜索失败: ${e.message}"
            }
        }
    }

    suspend fun downloadAndCache(song: SongDetail, onProgress: (Float) -> Unit = {}): String? {
        return repository.downloadAndCache(song, onProgress)
    }

    suspend fun getCachedPath(cid: String): String? = repository.getCachedPath(cid)

    suspend fun clearCache() {
        val shouldClearDownloads = downloadManager.isInternalStoragePath()
        repository.clearCache(clearDownloads = shouldClearDownloads)
    }

    suspend fun rebuildDatabase(): Int {
        return downloadManager.rebuildDatabase()
    }

    fun clearError() {
        _error.value = null
    }

    fun setDownloadPath(path: String) {
        downloadManager.setDownloadPath(path)
        _downloadPath.value = downloadManager.downloadPath
    }

    fun setDownloadUri(uri: Uri) {
        downloadManager.setDownloadUri(uri)
        _downloadUri.value = uri
        _downloadPath.value = downloadManager.downloadPath
    }
}
