package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId")
    suspend fun getLyricsForSong(songId: Long): SongLyrics?
    
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId")
    fun observeLyricsForSong(songId: Long): Flow<SongLyrics?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: SongLyrics)
}
