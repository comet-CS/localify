package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklisted_songs")
    fun getBlacklistedSongs(): Flow<List<BlacklistedSong>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlacklistedSong(song: BlacklistedSong)

    @Query("DELETE FROM blacklisted_songs WHERE id = :id")
    suspend fun deleteBlacklistedSongById(id: Long)
}
