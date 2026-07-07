package com.example.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.SongLyrics
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlinx.coroutines.flow.emptyFlow

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao,
    private val lyricsDao: LyricsDao,
    private val playlistDao: PlaylistDao,
    private val blacklistDao: BlacklistDao
) {
    private val okHttpClient = OkHttpClient()

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    suspend fun fetchLyricsForSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext

        for (song in songs) {
            val existingLyrics = lyricsDao.getLyricsForSong(song.id)
            if (existingLyrics != null) continue // Already fetched or marked not found

            try {
                val artist = URLEncoder.encode(song.artist, "UTF-8")
                val track = URLEncoder.encode(song.title, "UTF-8")
                val url = "https://lrclib.net/api/get?artist_name=$artist&track_name=$track"
                
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val syncedLyrics = json.optString("syncedLyrics", "")
                        if (syncedLyrics.isNotBlank()) {
                            lyricsDao.insertLyrics(SongLyrics(song.id, syncedLyrics, false))
                        } else {
                            lyricsDao.insertLyrics(SongLyrics(song.id, null, true))
                        }
                    } else {
                        lyricsDao.insertLyrics(SongLyrics(song.id, null, true))
                    }
                } else {
                    lyricsDao.insertLyrics(SongLyrics(song.id, null, true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Don't mark as not found on network error, so we can retry next time
            }
            delay(500) // Rate limiting for LRCLib
        }
    }

    fun observeLyrics(songId: Long): Flow<SongLyrics?> = lyricsDao.observeLyricsForSong(songId)

    val likedSongs: Flow<List<LikedSong>> = songDao.getLikedSongs()
    val blacklistedSongs: Flow<List<BlacklistedSong>> = blacklistDao.getBlacklistedSongs()
    
    suspend fun toggleBlacklist(songId: Long, isBlacklisted: Boolean) {
        if (isBlacklisted) {
            blacklistDao.deleteBlacklistedSongById(songId)
        } else {
            blacklistDao.insertBlacklistedSong(BlacklistedSong(songId))
        }
    }
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(PlaylistSong(playlistId, songId))
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Long>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    suspend fun loadLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val dataPath = cursor.getString(dataColumn) ?: ""
                val albumId = cursor.getLong(albumIdColumn)
                val albumArtUri = "content://media/external/audio/albumart/$albumId"

                songs.add(
                    Song(id, title, artist, album, duration, dataPath, albumArtUri)
                )
            }
        }
        songs
    }

    suspend fun toggleLike(song: Song, isLiked: Boolean) {
        if (isLiked) {
            songDao.deleteLikedSongById(song.id)
        } else {
            songDao.insertLikedSong(song.toLikedSong())
        }
    }

    fun isSongLiked(id: Long): Flow<Boolean> = songDao.isSongLiked(id)
}
