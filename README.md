<div align="center">
  <img width="1200" height="475" alt="SICARIO-LABS-APP" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# 🎬 SICARIO LABS - Advanced Offline Media Player

**A powerful, AI-enhanced offline-first Android media player with advanced features, built with Kotlin and Jetpack Compose.**

> ⚠️ **OFFLINE-FIRST**: This application is strictly offline-based. All features work without internet connectivity.

## 🌟 Features

### Core Media Playback
- ✅ Full-featured media player powered by Media3/ExoPlayer
- ✅ Support for multiple audio/video formats
- ✅ Playback controls: play, pause, seek, repeat, shuffle
- ✅ Playlist management
- ✅ Subtitle support
- ✅ Advanced audio controls

### Offline-First Architecture
- ✅ Zero internet dependency
- ✅ Local media library management
- ✅ Cached metadata and thumbnails
- ✅ Offline playback statistics
- ✅ Device storage optimization

### Smart Features
- ✅ AI-powered media organization (on-device)
- ✅ Advanced search and filtering
- ✅ Custom playlists and favorites
- ✅ Recently played tracking
- ✅ Media statistics and analytics (local)
- ✅ Dark/Light theme support

### User Experience
- ✅ Modern UI built with Jetpack Compose
- ✅ Material Design 3
- ✅ Smooth animations
- ✅ Responsive layouts
- ✅ Accessibility support

## 📋 Requirements

- **Android Studio** (Flamingo or later): [Download](https://developer.android.com/studio)
- **Android SDK**: API 24 (Android 7.0) or higher
- **Kotlin**: 1.9.0+
- **Gradle**: 8.0+
- **JDK**: Java 11+

## 🚀 Quick Start

### 1. Clone & Open Project
```bash
git clone https://github.com/mhdxbilal/SICARIO-LABS-APP.git
cd SICARIO-LABS-APP
```

### 2. Open in Android Studio
- Launch Android Studio
- Select **File → Open** and choose the project directory
- Allow Gradle sync and dependency resolution

### 3. Environment Setup (Optional)
```bash
# Create .env file for optional AI features (offline-only)
echo "GEMINI_API_KEY=your_key_here" > .env
```

### 4. Build Configuration
- Remove the debug signing config requirement if testing locally:
  ```gradle
  // In app/build.gradle.kts, comment out or remove:
  // signingConfig = signingConfigs.getByName("debugConfig")
  ```

### 5. Run Application
- **Emulator**: Select a virtual device and click **Run**
- **Physical Device**: Connect via USB and click **Run**
- Grant permissions when prompted

## 📁 Project Structure

```
SICARIO-LABS-APP/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/siciario/labs/
│   │   │   │   ├── ui/                 # Jetpack Compose UI screens
│   │   │   │   ├── viewmodel/          # MVVM ViewModels
│   │   │   │   ├── repository/         # Data layer
│   │   │   │   ├── model/              # Data models
│   │   │   │   ├── database/           # Room database
│   │   │   │   ├── service/            # Background services
│   │   │   │   ├── util/               # Utility functions
│   │   │   │   └── MainActivity.kt      # Entry point
│   │   │   ├── res/
│   │   │   │   ├── drawable/           # Icons & graphics
│   │   │   │   ├── values/             # Colors, strings, themes
│   │   │   │   └── layout/             # Legacy layouts (if any)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                       # Unit tests
│   │   └── androidTest/                # UI/Integration tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
├── .env.example
├── .gitignore
├── metadata.json
└── README.md
```

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|----------|
| Language | Kotlin | 1.9+ |
| UI Framework | Jetpack Compose | Latest BOM |
| Design System | Material Design 3 | - |
| Media | Media3/ExoPlayer | Latest |
| Database | Room | Latest |
| Networking | OkHttp + Retrofit | Latest |
| JSON | Moshi | Latest |
| Image Loading | Coil | Latest |
| Concurrency | Coroutines | Latest |
| Lifecycle | Lifecycle (MVVM) | Latest |
| Camera | CameraX | 1.3.1 |
| ML | MediaPipe | 0.20230731 |

## 🔧 Development

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run UI tests
./gradlew connectedAndroidTest

# Check code quality
./gradlew lint
```

### Gradle Properties
Optimized for:
- Parallel builds
- Incremental compilation
- Configuration caching
- Memory efficiency

## 📱 Supported Formats

### Audio
- MP3, WAV, FLAC, AAC, OGG, M4A, WMA

### Video
- MP4, MKV, WebM, AVI, 3GP, FLV

### Subtitles
- SRT, ASS, SUB, VTT

## 🔐 Privacy & Security

✅ **100% Offline** - No data transmission to external servers  
✅ **Local Storage Only** - All media and metadata stored on device  
✅ **No Tracking** - Zero analytics or telemetry  
✅ **No Ads** - Clean, ad-free experience  
✅ **Open Architecture** - Review the code anytime  

## 🎨 Customization

### Themes
Customize app appearance in `res/values/themes/`:
- Light theme
- Dark theme
- Custom color palettes

### Features Flags
Configure features in `gradle.properties` or feature config files

## 🐛 Troubleshooting

### Build Issues
```bash
# Clean build
./gradlew clean build

# Update dependencies
./gradlew dependencies --refresh-dependencies

# Clear cache
rm -rf .gradle build
```

### Runtime Issues
- **Permissions denied**: Check `AndroidManifest.xml` permissions
- **Media not loading**: Verify file paths and formats
- **Performance slow**: Check device storage and RAM

## 📊 Performance Metrics

- **APK Size**: ~15-20 MB (depends on architecture)
- **Min RAM**: 2 GB
- **Min Storage**: 100 MB free
- **Startup Time**: <2 seconds
- **Battery Impact**: Minimal (optimized playback)

## 🔄 Offline Sync Strategy

- Local database (Room) for metadata
- File system scanning for media discovery
- Cached thumbnails and artwork
- Playback history stored locally
- No cloud sync (offline-first)

## 📝 Contribution Guidelines

1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes and test thoroughly
3. Commit with descriptive messages
4. Push and create Pull Request
5. Ensure all tests pass

## 📄 License

This project is provided as-is for educational and personal use.

## 🤝 Support

For issues, questions, or feature requests:
- Open an [Issue](https://github.com/mhdxbilal/SICARIO-LABS-APP/issues)
- Check existing issues first
- Provide detailed reproduction steps

## 🗺️ Roadmap

- [ ] Gapless playback
- [ ] Advanced equalizer
- [ ] Visualizer effects
- [ ] Batch media operations
- [ ] Cloud import (with offline storage)
- [ ] Plugin system
- [ ] Extended media library

---

**Made with ❤️ by SICARIO LABS**

*Last Updated: 2026-06-16*
