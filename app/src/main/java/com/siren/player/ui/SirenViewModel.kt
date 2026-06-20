package com.siren.player.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siren.player.data.api.AlbumInfo
import com.siren.player.data.api.SongDetail
import com.siren.player.data.api.SongInfo
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
    val songs: List<SongInfo> = emptyList()
)

class SirenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val database = SirenDatabase.create(application)

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

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) { repository.getAlbums() }
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

    fun openAlbum(albumCid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val album = _albums.value.find { it.cid == albumCid }
                val songs = withContext(Dispatchers.IO) { repository.getAlbumSongs(albumCid) }
                _currentAlbum.value = UiAlbum(
                    cid = albumCid,
                    name = album?.name ?: "",
                    coverUrl = album?.coverUrl ?: "",
                    artistes = album?.artistes ?: emptyList(),
                    songs = songs
                )
            } catch (e: Exception) {
                _error.value = "加载专辑失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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

    fun clearError() {
        _error.value = null
    }
}
