package com.example.data

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataPath: String,
    val albumArtUri: String
) {
    fun toLikedSong() = LikedSong(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        dataPath = dataPath,
        albumArtUri = albumArtUri
    )
}

fun LikedSong.toSong() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    dataPath = dataPath,
    albumArtUri = albumArtUri ?: ""
)
