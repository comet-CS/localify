package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSong)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Long>>
    
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)
}
