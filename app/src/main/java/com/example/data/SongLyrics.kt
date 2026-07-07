package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_lyrics")
data class SongLyrics(
    @PrimaryKey val songId: Long,
    val lyrics: String?,
    val isNotFound: Boolean
)
