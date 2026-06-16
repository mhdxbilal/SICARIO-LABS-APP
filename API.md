# API Reference - SICARIO LABS Media Player

## Database APIs

### MediaDao

```kotlin
// Query operations
getAllMedia(): Flow<List<MediaItem>>
getMediaById(id: Long): MediaItem?
getFavoriteMedia(): Flow<List<MediaItem>>
getRecentlyPlayedMedia(): Flow<List<MediaItem>>
searchMedia(query: String): Flow<List<MediaItem>>

// Mutation operations
insertMedia(mediaItem: MediaItem)
insertAllMedia(mediaItems: List<MediaItem>)
updateMedia(mediaItem: MediaItem)
deleteMedia(mediaItem: MediaItem)
toggleFavorite(id: Long, isFavorite: Boolean)
recordMediaPlay(id: Long, timestamp: Long)
```

### PlaylistDao

```kotlin
// Query operations
getAllPlaylists(): Flow<List<Playlist>>
getPlaylistById(id: Long): Playlist?
getPlaylistItemCount(playlistId: Long): Flow<Int>

// Mutation operations
insertPlaylist(playlist: Playlist): Long
updatePlaylist(playlist: Playlist)
deletePlaylist(playlist: Playlist)
insertPlaylistItem(playlistItem: PlaylistItem)
removeFromPlaylist(playlistId: Long, mediaItemId: Long)
clearPlaylist(playlistId: Long)
```

## Repository APIs

### MediaRepository

```kotlin
fun getAllMedia(): Flow<List<MediaItem>>
fun getFavoriteMedia(): Flow<List<MediaItem>>
fun getMostPlayedMedia(): Flow<List<MediaItem>>
fun getRecentlyPlayedMedia(): Flow<List<MediaItem>>
fun searchMedia(query: String): Flow<List<MediaItem>>
suspend fun insertMedia(mediaItem: MediaItem)
suspend fun insertAllMedia(mediaItems: List<MediaItem>)
suspend fun updateMedia(mediaItem: MediaItem)
suspend fun deleteMedia(mediaItem: MediaItem)
suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
suspend fun recordMediaPlay(id: Long, timestamp: Long)
fun getMediaCount(): Flow<Int>
```

### PlaylistRepository

```kotlin
fun getAllPlaylists(): Flow<List<Playlist>>
suspend fun createPlaylist(name: String, description: String = ""): Long
suspend fun updatePlaylist(playlist: Playlist)
suspend fun deletePlaylist(playlist: Playlist)
suspend fun addToPlaylist(playlistId: Long, mediaItemId: Long, position: Int)
suspend fun removeFromPlaylist(playlistId: Long, mediaItemId: Long)
fun getPlaylistItemCount(playlistId: Long): Flow<Int>
```

## ViewModel APIs

### MediaViewModel

```kotlin
val uiState: StateFlow<MediaUiState>
val searchQuery: StateFlow<String>

fun loadAllMedia()
fun searchMedia(query: String)
fun toggleFavorite(mediaItem: MediaItem)
fun recordMediaPlay(mediaItem: MediaItem)
fun loadFavoriteMedia()
fun loadRecentlyPlayedMedia()
```

### PlaylistViewModel

```kotlin
val uiState: StateFlow<PlaylistUiState>

fun loadPlaylists()
fun createPlaylist(name: String, description: String = "")
fun deletePlaylist(playlist: Playlist)
fun addToPlaylist(playlistId: Long, mediaItemId: Long)
```

## Utility APIs

### StorageUtils

```kotlin
fun getCacheDir(context: Context): File
fun getThumbnailCacheDir(context: Context): File
fun getMetadataCacheDir(context: Context): File
fun getAvailableStorageSpace(): Long
fun formatBytes(bytes: Long): String
fun formatDuration(milliseconds: Long): String
```

### PermissionUtils

```kotlin
fun hasPermission(context: Context, permission: String): Boolean
fun hasRequiredPermissions(context: Context): Boolean
```

### NetworkUtils

```kotlin
fun isInternetAvailable(context: Context): Boolean
fun verifyOfflineMode(context: Context)
```

### CacheManager

```kotlin
fun get(key: String): ByteArray?
fun put(key: String, data: ByteArray, ttl: Long = CACHE_EXPIRY_MS)
fun remove(key: String)
fun clear()
fun getCacheSize(): Long
```

## Feature APIs

### MediaFilter

```kotlin
fun filterByGenre(mediaItems: List<MediaItem>, genre: String): List<MediaItem>
fun filterByArtist(mediaItems: List<MediaItem>, artist: String): List<MediaItem>
fun filterByAlbum(mediaItems: List<MediaItem>, album: String): List<MediaItem>
fun filterByType(mediaItems: List<MediaItem>, mimeTypePrefix: String): List<MediaItem>
fun sortByTitle(mediaItems: List<MediaItem>): List<MediaItem>
fun sortByArtist(mediaItems: List<MediaItem>): List<MediaItem>
fun sortByDateAdded(mediaItems: List<MediaItem>, descending: Boolean = true): List<MediaItem>
fun sortByPlayCount(mediaItems: List<MediaItem>, descending: Boolean = true): List<MediaItem>
fun getGenres(mediaItems: List<MediaItem>): Set<String>
fun getArtists(mediaItems: List<MediaItem>): Set<String>
fun getAlbums(mediaItems: List<MediaItem>): Set<String>
```

### ShuffleManager

```kotlin
fun setQueue(mediaItems: List<MediaItem>)
fun enableShuffle()
fun disableShuffle()
fun getCurrentQueue(): List<MediaItem>
fun getCurrentItem(): MediaItem?
fun getNextItem(): MediaItem?
fun getPreviousItem(): MediaItem?
fun next()
fun previous()
fun seekTo(index: Int)
fun getCurrentIndex(): Int
fun getQueueSize(): Int
```

### AudioSettingsManager

```kotlin
fun setEqualizerSettings(settings: EqualizerSettings)
fun getEqualizerSettings(): EqualizerSettings
fun setVolume(vol: Float)
fun getVolume(): Float
fun setBassBoost(boost: Int)
fun getBassBoost(): Int
fun applyPreset(preset: EqualizerSettings.EqualizerPreset)
```

## Data Models

### MediaItem

```kotlin
data class MediaItem(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val duration: Long,
    val fileSize: Long,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTime: Long = 0
)
```

### Playlist

```kotlin
data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val coverUri: String? = null
)
```

---

**Last Updated:** 2026-06-16
