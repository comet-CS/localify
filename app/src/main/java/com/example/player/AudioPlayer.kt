package com.example.player

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayer(private val context: Context) {
    private var controller: MediaController? = null
    
    private var currentPlaylist: List<Song> = emptyList()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()
    
    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled: StateFlow<Boolean> = _isShuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            { 
                controller = controllerFuture.get() 
                setupController()
            },
            ContextCompat.getMainExecutor(context)
        )
    }
    
    private fun setupController() {
        controller?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSong(mediaItem)
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }
            
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleModeEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
        
        updateCurrentSong(controller?.currentMediaItem)
        _isPlaying.value = controller?.isPlaying == true
        if (_isPlaying.value) {
            startProgressTracking()
        }
        _isShuffleModeEnabled.value = controller?.shuffleModeEnabled == true
        _repeatMode.value = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
    }
    
    fun toggleShuffle() {
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        val songId = mediaItem?.mediaId?.toLongOrNull()
        if (songId != null) {
            val songInPlaylist = currentPlaylist.find { it.id == songId }
            if (songInPlaylist != null) {
                _currentSong.value = songInPlaylist
            } else {
                val metadata = mediaItem.mediaMetadata
                _currentSong.value = Song(
                    id = songId,
                    title = metadata.title?.toString() ?: "",
                    artist = metadata.artist?.toString() ?: "",
                    album = metadata.albumTitle?.toString() ?: "",
                    durationMs = controller?.duration?.takeIf { it > 0 } ?: 0L,
                    dataPath = "",
                    albumArtUri = metadata.artworkUri?.toString() ?: ""
                )
            }
        } else {
            _currentSong.value = null
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty() || startIndex !in songs.indices) return
        currentPlaylist = songs
        
        val mediaItems = songs.map { song ->
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            val artworkUri = if (song.albumArtUri.isNotEmpty()) Uri.parse(song.albumArtUri) else null
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(artworkUri)
                        .build()
                )
                .build()
        }
        
        controller?.setMediaItems(mediaItems, startIndex, 0)
        controller?.prepare()
        controller?.play()
    }

    fun playPause() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun playNext() {
        controller?.seekToNext()
    }
    
    fun removeSongFromQueue(songId: Long) {
        controller?.let {
            val count = it.mediaItemCount
            for (i in count - 1 downTo 0) {
                if (it.getMediaItemAt(i).mediaId == songId.toString()) {
                    it.removeMediaItem(i)
                }
            }
        }
        currentPlaylist = currentPlaylist.filter { it.id != songId }
    }

    fun playPrevious() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _progressMs.value = positionMs
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                controller?.let {
                    if (it.isPlaying) {
                        _progressMs.value = it.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun release() {
        controller?.release()
        controller = null
        stopProgressTracking()
    }
}
