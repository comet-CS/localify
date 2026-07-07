package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM liked_songs")
    fun getLikedSongs(): Flow<List<LikedSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedSong(song: LikedSong)

    @Query("DELETE FROM liked_songs WHERE id = :id")
    suspend fun deleteLikedSongById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE id = :id)")
    fun isSongLiked(id: Long): Flow<Boolean>
}
