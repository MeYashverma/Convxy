package com.convx.music.ui.screens.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convx.music.db.MusicDatabase
import com.convx.music.db.entities.Album
import com.convx.music.db.entities.Artist
import com.convx.music.db.entities.Song
import com.convx.music.utils.LocalAudioScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = database.localSongsByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = database.albumsLocalByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = database.artistsLocalByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<LocalAudioScanner.ScanResult?>(null)
    val scanResult: StateFlow<LocalAudioScanner.ScanResult?> = _scanResult.asStateFlow()

    fun scanDevice(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = LocalAudioScanner.scanAndInsert(context, database)
                _scanResult.value = result
                Timber.tag("LocalMusicViewModel").i("Scan complete: $result")
            } catch (e: Exception) {
                Timber.tag("LocalMusicViewModel").e(e, "Scan failed")
            } finally {
                _isScanning.value = false
            }
        }
    }
}
