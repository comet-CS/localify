package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Song
import com.example.data.Playlist
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.SurfaceVariantColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MusicViewModel, onNavigateToNowPlaying: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val localSongs by viewModel.localSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentSongState by viewModel.audioPlayer.currentSong.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val progressMs by viewModel.audioPlayer.progressMs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val currentSong = currentSongState?.let { song ->
        if (song.durationMs <= 0L) {
            localSongs.find { it.id == song.id } ?: song
        } else song
    }

    var searchQuery by remember { mutableStateOf("") }
    
    // Dialog states
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddPlaylist by remember { mutableStateOf<Song?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    var viewingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var viewingLikedSongs by remember { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SpotifyGreen,
                        cursorColor = SpotifyGreen
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName.trim())
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    }
                }) {
                    Text("Create", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = SurfaceColor
        )
    }

    if (songToAddPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songToAddPlaylist = null },
            title = { Text("Add to Playlist", color = Color.White) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists found.", color = Color.Gray)
                } else {
                    LazyColumn {
                        items(playlists.size) { index ->
                            val playlist = playlists[index]
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, songToAddPlaylist!!.id)
                                        songToAddPlaylist = null
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songToAddPlaylist = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = SurfaceColor
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                if (currentSong != null) {
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        progressMs = progressMs,
                        onPlayPause = { viewModel.audioPlayer.playPause() },
                        onClick = onNavigateToNowPlaying
                    )
                }
                NavigationBar(
                    containerColor = DarkBackground,
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = SurfaceColor,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = SurfaceColor,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = SurfaceColor,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        if (viewingPlaylist != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                PlaylistScreen(
                    playlist = viewingPlaylist!!,
                    viewModel = viewModel,
                    onBack = { viewingPlaylist = null },
                    onPlay = { index, songs -> viewModel.audioPlayer.playPlaylist(songs, index) },
                    onAddSongToPlaylist = { songToAddPlaylist = it }
                )
            }
            return@Scaffold
        }
        if (viewingLikedSongs) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LikedSongsScreen(
                    viewModel = viewModel,
                    onBack = { viewingLikedSongs = false },
                    onPlay = { index, songs -> viewModel.audioPlayer.playPlaylist(songs, index) },
                    onAddSongToPlaylist = { songToAddPlaylist = it }
                )
            }
            return@Scaffold
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.align(Alignment.Center))
            } else {
                when (selectedTab) {
                    0 -> HomeTab(
                        localSongs = localSongs,
                        playlists = playlists,
                        currentSong = currentSong,
                        onPlay = { index, songs -> viewModel.audioPlayer.playPlaylist(songs, index) },
                        onAddSongToPlaylist = { songToAddPlaylist = it },
                        onPlaylistClick = { viewingPlaylist = it },
                        onLikedSongsClick = { viewingLikedSongs = true }
                    )
                    1 -> SearchTab(
                        localSongs = localSongs,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        currentSong = currentSong,
                        onPlay = { index, songs -> viewModel.audioPlayer.playPlaylist(songs, index) },
                        onAddSongToPlaylist = { songToAddPlaylist = it }
                    )
                    2 -> LibraryTab(
                        playlists = playlists,
                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                        onPlaylistClick = { viewingPlaylist = it },
                        onLikedSongsClick = { viewingLikedSongs = true }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTab(
    localSongs: List<Song>,
    playlists: List<Playlist>,
    currentSong: Song?,
    onPlay: (Int, List<Song>) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onLikedSongsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Good evening",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            val topPlaylists = playlists.take(3)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(if (topPlaylists.isEmpty()) 72.dp else 144.dp).padding(horizontal = 8.dp),
                userScrollEnabled = false
            ) {
                item {
                    PlaylistGridItem(name = "Liked Songs", icon = Icons.Filled.Favorite, tint = SpotifyGreen, onClick = onLikedSongsClick)
                }
                items(topPlaylists) { playlist ->
                    PlaylistGridItem(name = playlist.name, icon = Icons.AutoMirrored.Filled.QueueMusic, tint = Color.White, onClick = { onPlaylistClick(playlist) })
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "All Songs",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        itemsIndexed(localSongs) { index, song ->
            SongListItem(
                song = song,
                isCurrent = currentSong?.id == song.id,
                onClick = { onPlay(index, localSongs) },
                onLongClick = { onAddSongToPlaylist(song) }
            )
        }
    }
}

@Composable
fun PlaylistGridItem(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceVariantColor)
            .clickable(onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SurfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    localSongs: List<Song>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSong: Song?,
    onPlay: (Int, List<Song>) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search songs or artists", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceVariantColor,
                unfocusedContainerColor = SurfaceVariantColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = SpotifyGreen
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        
        val filteredSongs = if (searchQuery.isBlank()) {
            emptyList()
        } else {
            localSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
        
        LazyColumn {
            itemsIndexed(filteredSongs) { index, song ->
                SongListItem(
                    song = song,
                    isCurrent = currentSong?.id == song.id,
                    onClick = { onPlay(index, filteredSongs) },
                    onLongClick = { onAddSongToPlaylist(song) }
                )
            }
        }
    }
}

@Composable
fun LibraryTab(
    playlists: List<Playlist>,
    onCreatePlaylist: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onLikedSongsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                IconButton(onClick = onCreatePlaylist) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Playlist", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLikedSongsClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(SurfaceVariantColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = SpotifyGreen)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Liked Songs", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Playlist", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
        items(playlists.size) { index ->
            val playlist = playlists[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(SurfaceVariantColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(playlist.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Playlist", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SongListItem(song: Song, isCurrent: Boolean, isBlacklisted: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit, onToggleBlacklist: () -> Unit = {}) {
    
    @OptIn(ExperimentalFoundationApi::class)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = "Album Art",
            fallback = painterResource(R.drawable.img_album_placeholder_1783437405746),
            error = painterResource(R.drawable.img_album_placeholder_1783437405746),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceVariantColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) SpotifyGreen else if (isBlacklisted) androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = if (isBlacklisted) androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        var expanded by remember { mutableStateOf(false) }
        
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.Gray)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceColor)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = Color.White) },
                    onClick = {
                        expanded = false
                        onLongClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isBlacklisted) "Un-blacklist" else "Blacklist Audio", color = Color.White) },
                    onClick = {
                        expanded = false
                        onToggleBlacklist()
                    }
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, progressMs: Long, onPlayPause: () -> Unit, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantColor)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                fallback = painterResource(R.drawable.img_album_placeholder_1783437405746),
                error = painterResource(R.drawable.img_album_placeholder_1783437405746),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }
        }
        
        val progressPercent = if (song.durationMs > 0) {
            (progressMs.toFloat() / song.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.DarkGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressPercent)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}


@Composable
fun PlaylistScreen(
    playlist: Playlist,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onPlay: (Int, List<Song>) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit
) {
    val songs by viewModel.getPlaylistSongs(playlist.id).collectAsState(initial = emptyList())
    val currentSong by viewModel.audioPlayer.currentSong.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        
        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs in this playlist.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs) { index, song ->
                    SongListItem(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        onClick = { onPlay(index, songs) },
                        onLongClick = { onAddSongToPlaylist(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun LikedSongsScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onPlay: (Int, List<Song>) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit
) {
    val likedSongsList by viewModel.likedSongs.collectAsState(initial = emptyList())
    val localSongs by viewModel.localSongs.collectAsState()
    val currentSong by viewModel.audioPlayer.currentSong.collectAsState()
    
    val songs = remember(likedSongsList, localSongs) {
        val likedIds = likedSongsList.map { it.id }.toSet()
        localSongs.filter { it.id in likedIds }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Liked Songs",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        
        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No liked songs.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs) { index, song ->
                    SongListItem(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        onClick = { onPlay(index, songs) },
                        onLongClick = { onAddSongToPlaylist(song) }
                    )
                }
            }
        }
    }
}
