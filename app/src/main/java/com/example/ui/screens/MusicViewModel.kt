package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MusicRepository
import com.example.data.Playlist
import com.example.data.Song
import com.example.data.toSong
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(application, database.songDao(), database.lyricsDao(), database.playlistDao(), database.blacklistDao())
    val audioPlayer = AudioPlayer(application)

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()
    
    val blacklistedSongIds: StateFlow<Set<Long>> = repository.blacklistedSongs
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
        
    fun toggleBlacklist(song: Song) {
        viewModelScope.launch {
            val isBlacklisted = blacklistedSongIds.value.contains(song.id)
            repository.toggleBlacklist(song.id, isBlacklisted)
            if (!isBlacklisted) {
                audioPlayer.removeSongFromQueue(song.id)
            }
        }
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val currentLyrics = audioPlayer.currentSong.flatMapLatest { song ->
        if (song != null) repository.observeLyrics(song.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val songs = repository.loadLocalSongs()
            _localSongs.value = songs
            _isLoading.value = false
            
            // Fetch lyrics in background
            repository.fetchLyricsForSongs(songs)
        }
    }

    val likedSongs: StateFlow<List<Song>> = repository.likedSongs
        .map { likedList -> likedList.map { it.toSong() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val currentlyLiked = likedSongs.value.any { it.id == song.id }
            repository.toggleLike(song, currentlyLiked)
        }
    }

    fun isLiked(song: Song?): StateFlow<Boolean> {
        if (song == null) return MutableStateFlow(false)
        return repository.isSongLiked(song.id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> {
        return repository.getSongsInPlaylist(playlistId).map { songIds ->
            val allSongs = _localSongs.value
            songIds.mapNotNull { id -> allSongs.find { it.id == id } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
