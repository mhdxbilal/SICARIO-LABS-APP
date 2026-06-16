# SICARIO LABS - Media Player Development Guide

## 🏗️ Architecture Overview

The application follows a modern MVVM (Model-View-ViewModel) architecture with offline-first design principles.

### Architecture Layers

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │
│   - Screens, Components, States     │
└────────────────┬────────────────────┘
                 │
┌─────────────────▼────────────────────┐
│      ViewModel Layer (MVVM)         │
│   - State Management                │
│   - Business Logic                  │
└────────────────┬────────────────────┘
                 │
┌─────────────────▼────────────────────┐
│      Repository Layer               │
│   - Data Access Abstraction         │
└────────────────┬────────────────────┘
                 │
┌─────────────────▼────────────────────┐
│      Local Database (Room)          │
│   - Offline Storage                 │
│   - Metadata Cache                  │
└─────────────────────────────────────┘
```

## 📦 Module Structure

### `ui/` - User Interface
- **screens/** - Full screen components
- **components/** - Reusable UI components
- **theme/** - Material Design 3 theme
- **viewmodel/** - MVVM ViewModels

### `data/` - Data Layer
- **database/** - Room database entities and DAOs
- **model/** - Data models
- **repository/** - Repository pattern implementations

### `service/` - Services
- **MediaPlaybackService** - Media3 playback
- **MediaScannerService** - Device media scanning

### `feature/` - Features
- **MediaFilter** - Media filtering and sorting
- **ShuffleManager** - Queue and shuffle management
- **AudioSettings** - EQ and audio configuration

### `util/` - Utilities
- **StorageUtils** - Storage and file operations
- **PermissionUtils** - Permission handling
- **NetworkUtils** - Offline mode verification
- **CacheManager** - Local caching system

## 🔄 Data Flow

### Media Playback Flow

```
User Action (Play)
    ↓
ViewModel (MediaViewModel)
    ↓
Repository (MediaRepository)
    ↓
Database (Room)
    ↓
MediaPlaybackService (ExoPlayer)
    ↓
Local Audio Output
```

### Media Scanning Flow

```
App Startup / Manual Scan
    ↓
MediaScannerService
    ↓
File System Scanning
    ↓
Metadata Extraction
    ↓
Database (Room Insert)
    ↓
UI Update (Flow)
```

## 🗄️ Database Schema

### MediaItem Entity
```sql
CREATE TABLE media_items (
    id INTEGER PRIMARY KEY,
    filePath TEXT NOT NULL,
    fileName TEXT NOT NULL,
    title TEXT,
    artist TEXT,
    album TEXT,
    genre TEXT,
    duration INTEGER,
    fileSize INTEGER,
    mimeType TEXT,
    dateAdded INTEGER,
    dateModified INTEGER,
    isFavorite BOOLEAN,
    playCount INTEGER,
    lastPlayedTime INTEGER
);
```

### Playlist Entity
```sql
CREATE TABLE playlists (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    createdAt INTEGER,
    updatedAt INTEGER,
    coverUri TEXT
);
```

### PlaylistItem Junction Entity
```sql
CREATE TABLE playlist_items (
    playlistId INTEGER,
    mediaItemId INTEGER,
    position INTEGER,
    PRIMARY KEY (playlistId, mediaItemId)
);
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### UI Tests (Instrumented)
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew testDebugUnitTestCoverage
```

## 🔐 Offline-First Implementation

### No Internet Operations
- ✅ All data stored locally in Room database
- ✅ Media files scanned from device storage only
- ✅ No external API calls
- ✅ No cloud sync
- ✅ No telemetry or tracking

### Local Caching
- Metadata cached in database
- Thumbnails cached in device storage
- Playback history stored locally
- User preferences stored locally

## 🎨 UI Components

### Key Composables
- `MainScreen()` - Primary navigation and layout
- `LibraryScreen()` - Media library browse
- `PlaylistsScreen()` - Playlist management
- `SearchScreen()` - Media search
- `SettingsScreen()` - App configuration
- `PlaybackControls()` - Player controls
- `MediaListItem()` - List item component
- `MediaProgressBar()` - Seek bar component

## 🎵 Media Formats

### Supported Audio
- MP3, WAV, FLAC, AAC, OGG, M4A, WMA

### Supported Video
- MP4, MKV, WebM, AVI, 3GP, FLV

### Supported Subtitles
- SRT, ASS, SUB, VTT

## 📊 Performance Optimization

### Database
- Indexing on frequently queried columns
- Pagination for large lists
- Flow-based reactive updates

### Memory
- Efficient bitmap caching
- LazyColumn for list rendering
- Resource cleanup in onDestroy

### Storage
- Automatic cache cleanup
- Configurable cache size (500 MB default)
- Thumbnail optimization (256x256 default)

## 🔧 Build Variants

### Debug
- Full logging enabled
- Debugging allowed
- Minification disabled

### Release
- Proguard minification enabled
- Logging stripped
- Optimized for size

## 📝 Code Style

### Kotlin Style Guide
- Follow official Kotlin style guide
- Use `val` over `var` when possible
- Prefer expressions over statements
- Use `data class` for model classes

### Naming Conventions
- Packages: `com.siciario.labs.feature`
- Classes: `PascalCase`
- Functions: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Private members: `_leadingUnderscore`

## 🚀 Build & Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Install on Device
```bash
./gradlew installDebug
./gradlew installRelease
```

## 🐛 Debugging

### Enable Debug Logging
```kotlin
if (BuildConfig.DEBUG) {
    Log.d("TAG", "Debug message")
}
```

### Database Inspector
Use Android Studio Database Inspector to view Room database

### Logcat Filtering
```bash
adb logcat com.siciario.labs.mediaplayer:V
```

## 📚 Resources

- [Android Documentation](https://developer.android.com)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Media3/ExoPlayer](https://developer.android.com/media/media3/getting-started)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## 📄 License

This project is provided as-is for educational and personal use.

---

**Last Updated:** 2026-06-16
