package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_songs")
data class LikedSong(
    @PrimaryKey val id: Long, // MediaStore Audio ID
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataPath: String, // Path to file if needed
    val albumArtUri: String?
)
