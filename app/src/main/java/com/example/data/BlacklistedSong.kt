package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklisted_songs")
data class BlacklistedSong(
    @PrimaryKey val id: Long
)
