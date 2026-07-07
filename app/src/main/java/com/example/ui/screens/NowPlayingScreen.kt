package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Song
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.SurfaceVariantColor
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(viewModel: MusicViewModel, onBack: () -> Unit) {
    val currentSongState by viewModel.audioPlayer.currentSong.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val progressMs by viewModel.audioPlayer.progressMs.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    val isShuffleEnabled by viewModel.audioPlayer.isShuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.audioPlayer.repeatMode.collectAsState()
    val blacklistedSongIds by viewModel.blacklistedSongIds.collectAsState()
    
    val currentSong = currentSongState?.let { song ->
        if (song.durationMs <= 0L) {
            localSongs.find { it.id == song.id } ?: song
        } else song
    }

    if (currentSong == null) {
        onBack()
        return
    }

    val song = currentSong!!
    val isLiked by viewModel.isLiked(song).collectAsState(initial = false)

    var isFullScreenLyrics by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())

    if (isFullScreenLyrics) {
        BackHandler { isFullScreenLyrics = false }
        FullScreenLyricsScreen(
            viewModel = viewModel,
            song = song,
            progressMs = progressMs,
            isPlaying = isPlaying,
            onClose = { isFullScreenLyrics = false }
        )
        return
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Now Playing",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(SurfaceColor)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = Color.White) },
                                onClick = {
                                    expandedMenu = false
                                    showPlaylistDialog = true
                                }
                            )
                            val isBlacklisted = blacklistedSongIds.contains(song.id)
                            DropdownMenuItem(
                                text = { Text(if (isBlacklisted) "Un-blacklist" else "Blacklist Audio", color = Color.White) },
                                onClick = {
                                    expandedMenu = false
                                    viewModel.toggleBlacklist(song)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                fallback = painterResource(R.drawable.img_album_placeholder_1783437405746),
                error = painterResource(R.drawable.img_album_placeholder_1783437405746),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceColor)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.Gray,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.toggleLike(song) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) SpotifyGreen else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            val progressPercent = if (song.durationMs > 0) progressMs.toFloat() / song.durationMs.toFloat() else 0f
            Slider(
                value = progressPercent,
                onValueChange = { percent ->
                    viewModel.audioPlayer.seekTo((percent * song.durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = SurfaceVariantColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(progressMs), color = Color.Gray, fontSize = 12.sp)
                Text(text = formatTime(song.durationMs), color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.audioPlayer.toggleShuffle() }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (isShuffleEnabled) SpotifyGreen else Color.Gray)
                }
                IconButton(onClick = { viewModel.audioPlayer.playPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(
                    onClick = { viewModel.audioPlayer.playPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { viewModel.audioPlayer.playNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.audioPlayer.toggleRepeat() }) {
                    val repeatIcon = when (repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    val repeatTint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) Color.Gray else SpotifyGreen
                    Icon(repeatIcon, contentDescription = "Repeat", tint = repeatTint)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val currentLyricsObj by viewModel.currentLyrics.collectAsState()
            
            if (currentLyricsObj?.isNotFound != true) {
                // Lyrics Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF28543B))
                        .clickable { isFullScreenLyrics = true }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Lyrics",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    
                    IconButton(
                        onClick = { isFullScreenLyrics = true },
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 16.dp, y = (-16).dp)
                    ) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = "Full Screen", tint = Color.White)
                    }
                    
                    Box(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
                        SyncedLyrics(lyricsText = currentLyricsObj?.lyrics, progressMs = progressMs, isFullScreen = false)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenLyricsScreen(
    viewModel: MusicViewModel,
    song: Song,
    progressMs: Long,
    isPlaying: Boolean,
    onClose: () -> Unit
) {
    val currentLyricsObj by viewModel.currentLyrics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF28543B)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                SyncedLyrics(lyricsText = currentLyricsObj?.lyrics, progressMs = progressMs, isFullScreen = true)
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = { viewModel.audioPlayer.playPause() },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black
                        )
                    }
                }
                val progressPercent = if (song.durationMs > 0) progressMs.toFloat() / song.durationMs.toFloat() else 0f
                Slider(
                    value = progressPercent,
                    onValueChange = { percent -> viewModel.audioPlayer.seekTo((percent * song.durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

data class ParsedLyric(val timeMs: Long, val text: String)

fun parseLrc(lrcContent: String?): List<ParsedLyric> {
    if (lrcContent.isNullOrBlank()) return emptyList()
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
    val parsed = mutableListOf<ParsedLyric>()
    
    lrcContent.lines().forEach { line ->
        val match = regex.find(line)
        if (match != null) {
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val msPart = match.groupValues[3]
            val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
            val text = match.groupValues[4].trim()
            val timeMs = minutes * 60 * 1000 + seconds * 1000 + ms
            if (text.isNotEmpty()) {
                parsed.add(ParsedLyric(timeMs, text))
            }
        }
    }
    return parsed
}

@Composable
fun SyncedLyrics(lyricsText: String?, progressMs: Long, isFullScreen: Boolean) {
    val lyrics = remember(lyricsText) { parseLrc(lyricsText) }
    
    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lyrics not available", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    val listState = rememberLazyListState()
    
    val currentIndex = remember(progressMs, lyrics) {
        var index = lyrics.indexOfLast { it.timeMs <= progressMs }
        if (index == -1) index = 0
        index
    }

    val density = LocalDensity.current
    val offsetPx = with(density) { if (isFullScreen) 300.dp.toPx().toInt() else 120.dp.toPx().toInt() }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lyrics.size) {
            listState.animateScrollToItem(currentIndex, scrollOffset = -offsetPx)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = if (isFullScreen) 400.dp else 200.dp)
    ) {
        items(lyrics.size) { index ->
            val isCurrent = index == currentIndex
            
            val alpha by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.4f,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                label = "alpha"
            )
            
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.05f else 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                label = "scale"
            )
            
            Text(
                text = lyrics[index].text,
                color = Color.White.copy(alpha = alpha),
                fontSize = if (isFullScreen) 32.sp else 24.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                style = TextStyle(
                    shadow = if (isCurrent) Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 12f) else null
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isFullScreen) 16.dp else 12.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
