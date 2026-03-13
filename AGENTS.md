# Kikoeru Android

## Project Overview

Kikoeru-android is an Android client application for browsing and playing content from [Kikoeru-project](https://github.com/kikoeru-project/kikoeru-express) servers. It is designed to work with Kikoeru-project V0.6.2 API.

The app provides features including:
- Online audio/video streaming and playback
- Local caching for offline playback (audio, video, lyrics)
- Background playback with notification controls
- Lyrics floating window display
- Multi-account switching
- Download management with progress tracking
- RJ number search functionality
- Sleep timer

## Technology Stack

- **Language**: Java
- **Build System**: Gradle 7.3.3
- **Android Gradle Plugin**: 7.2.2
- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 30 (Android 11)
- **Compile SDK**: 34

## Key Dependencies

### Core Android Libraries
- `androidx.appcompat:appcompat:1.2.0` - AppCompat support
- `com.google.android.material:material:1.3.0` - Material Design components
- `androidx.media:media:1.5.0` - Media session support
- `androidx.constraintlayout:constraintlayout:2.0.4` - ConstraintLayout
- `androidx.exifinterface:exifinterface:1.3.2` - EXIF data handling
- `androidx.palette:palette:1.0.0` - Color palette extraction

### Architecture Components
- `androidx.room:room-runtime:2.5.2` - SQLite ORM (replaced GreenDAO)
- `androidx.room:room-compiler:2.5.2` - Room annotation processor
- `androidx.lifecycle:lifecycle-viewmodel:2.5.1` - MVVM ViewModel
- `androidx.lifecycle:lifecycle-livedata:2.5.1` - LiveData for reactive UI
- `androidx.lifecycle:lifecycle-common-java8:2.5.1` - Java 8 support

### Third-party Libraries
- **ExoPlayer 2.16.1** - Media playback engine
  - Core, UI, DASH, Database, Datasource, Decoder, Extractor modules
- **Glide 4.8.0** - Image loading and caching
  - Includes GIF decoder
- **AndroidAsync 3.1.0** - Asynchronous HTTP client
- **Subsampling Scale Image View 3.10.0** - High-resolution image display
- **Guava 32.0.1-jre** - Google core libraries
- **Gson 2.8.9** - JSON parsing

### Bundled in `app/libs/`
- ExoPlayer modules (AAR)
- Glide (AAR/JAR)
- AndroidAsync (AAR)
- Subsampling Scale Image View (AAR)

## Project Structure

```
Kikoeru-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/zinhao/kikoeru/    # Main source code
│   │   │   ├── App.java                # Application class
│   │   │   ├── Api.java                # API client for server communication
│   │   │   ├── AppDatabase.java        # Room database class
│   │   │   ├── AudioService.java       # Background playback service
│   │   │   ├── BaseActivity.java       # Base activity with common functionality
│   │   │   ├── User.java               # User entity (Room)
│   │   │   ├── UserDao.java            # User DAO (Room)
│   │   │   ├── Text.java               # Text/lyrics processing
│   │   │   ├── JSONConst.java          # JSON constants
│   │   │   ├── *Activity.java          # Various UI activities
│   │   │   ├── *Adapter.java           # RecyclerView adapters
│   │   │   └── ...                     # Other utility classes
│   │   ├── res/                        # Android resources
│   │   │   ├── layout/                 # XML layouts
│   │   │   ├── drawable/               # Drawables and vector icons
│   │   │   ├── values/                 # Strings, colors, themes
│   │   │   │   ├── strings.xml         # English strings (default)
│   │   │   │   ├── colors.xml
│   │   │   │   ├── themes.xml
│   │   │   │   └── attrs.xml
│   │   │   ├── values-zh-rCN/          # Simplified Chinese
│   │   │   ├── values-ja-rJP/          # Japanese
│   │   │   └── xml/                    # Configuration XML
│   │   └── AndroidManifest.xml
│   ├── libs/                           # Bundled AAR/JAR libraries
│   ├── build.gradle                    # App-level build config
│   └── proguard-rules.pro              # ProGuard rules
├── build.gradle                        # Project-level build config
├── settings.gradle                     # Project settings
├── gradle.properties                   # Gradle configuration
└── gradle/wrapper/                     # Gradle wrapper
```

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install debug build on connected device
./gradlew installDebug

# Run unit tests
./gradlew test
```

## Architecture

### Application Architecture
The app follows MVVM architecture with:

1. **Application Class** (`App.java`): 
   - Singleton pattern for global state management
   - Room database initialization
   - SharedPreferences wrapper for settings
   - Activity lifecycle tracking

2. **API Layer** (`Api.java`):
   - Static methods for all server communication
   - Uses AndroidAsync for HTTP requests
   - JWT token-based authentication
   - Supports sorting, filtering, and pagination

3. **Service Layer** (`AudioService.java`):
   - Foreground service for background playback
   - ExoPlayer for media playback
   - MediaSession for lock screen controls
   - Notification with playback controls
   - Lyrics floating window management
   - Bluetooth/headset event handling

4. **UI Layer** (`*Activity.java`):
   - Activity-based UI architecture
   - BaseActivity for common functionality
   - RecyclerView with custom adapters for lists
   - ViewBinding enabled

5. **MVVM Components**:
   - ViewModel for UI-related data
   - LiveData for observable data
   - Room for data persistence

### Database Schema

Uses **Room** ORM with a single entity:

**User Table** (`@Entity(tableName = "users")`):
- `id` (Long, PK, auto-generate) - User ID
- `name` (String) - Username
- `password` (String) - Password
- `host` (String) - Server host URL
- `token` (String) - JWT authentication token
- `lastUpdateTime` (long) - Last sync timestamp

**Database Class** (`AppDatabase.java`):
- Version: 1
- Entities: User
- DAO: UserDao

### Key Activities

| Activity | Purpose |
|----------|---------|
| `LauncherActivity` | Entry point, checks auth state |
| `LoginAccountActivity` | User login/registration |
| `WorksActivity` | Browse all works |
| `WorkTreeActivity` | Work detail and track list |
| `AudioPlayerActivity` | Audio playback UI |
| `VideoPlayerActivity` | Video playback UI |
| `TagsActivity` | Browse by tags |
| `VasActivity` | Browse by voice actors |
| `CirclesActivity` | Browse by circles/groups |
| `SearchActivity` | RJ number search |
| `DownLoadMissionActivity` | Download management |
| `UserSwitchActivity` | Multi-account management |
| `MoreActivity` | Settings and options |
| `AboutActivity` | App information |
| `LicenseActivity` | Open source licenses |
| `ImageBrowserActivity` | Image gallery viewer |
| `TextRowActivity` | Text/lyrics row display |
| `LrcFloatWindow` | Lyrics floating window permission handler |

### Utility Classes

| Class | Purpose |
|-------|---------|
| `Api.java` | Server API communication |
| `DownloadUtils.java` | File download utilities |
| `LocalFileCache.java` | Local file caching management |
| `LocalResponse.java` | Local HTTP response handling |
| `Text.java` | Text/lyrics parsing and processing |
| `TimeProgressView.java` | Custom progress view |
| `TagsView.java` | Custom tag display view |
| `ImageIndicator.java` | Image page indicator |

## Configuration

### App-level Configuration (`gradle.properties`)
- `android.useAndroidX=true` - Uses AndroidX libraries
- `android.enableJetifier=true` - Auto-migrates third-party libs to AndroidX
- `android.defaults.buildfeatures.buildconfig=true` - BuildConfig enabled
- `org.gradle.jvmargs=-Xmx2048m` - JVM heap size

### Build Configuration (`app/build.gradle`)
- **Application ID**: `com.zinhao.kikoeru`
- **Version Code**: 12
- **Version Name**: `Release_8.0`
- **ViewBinding**: Enabled
- **Java Version**: 1.8
- **Namespace**: `com.zinhao.kikoeru`

### Room Configuration
```java
@Database(entities = {User.class}, version = 1)
abstract class AppDatabase extends RoomDatabase {
    abstract UserDao userDao();
}
```

## Localization

The app supports three languages:
- **English** (`values/strings.xml`) - Default
- **Simplified Chinese** (`values-zh-rCN/strings.xml`)
- **Japanese** (`values-ja-rJP/strings.xml`)

## Permissions

Required permissions (from `AndroidManifest.xml`):
- `INTERNET` - Network access
- `FOREGROUND_SERVICE` - Background playback service
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Media playback foreground service type (Android 14+)
- `POST_NOTIFICATIONS` - Notification permission (Android 13+)
- `SYSTEM_ALERT_WINDOW` - Lyrics floating window
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` - File caching
- `MANAGE_EXTERNAL_STORAGE` - External storage management (Android 11+)
- `BLUETOOTH_CONNECT` - Bluetooth headset controls

## Testing

The project includes minimal unit testing setup:
- Test framework: JUnit 4
- Test location: `app/src/test/java/`
- Current tests: Basic example unit test only

```bash
# Run unit tests
./gradlew test
```

## Code Style Guidelines

Based on existing code:

1. **Naming Conventions**:
   - Classes: PascalCase (e.g., `AudioService`, `BaseActivity`)
   - Methods: camelCase (e.g., `getCurrentUser()`, `setValue()`)
   - Constants: UPPER_SNAKE_CASE (e.g., `CONFIG_FILE_NAME`, `ID_PLAY_SERVICE`)
   - XML resources: snake_case (e.g., `activity_main.xml`)

2. **Package Structure**:
   - Base package: `com.zinhao.kikoeru`
   - All classes in single package

3. **Comments**: Mixed Chinese and English in codebase

4. **String Resources**: All user-facing strings in `strings.xml` files

## Security Considerations

1. **Token Storage**: JWT tokens stored in Room database
2. **Network**: HTTP/HTTPS communication with server
3. **File Access**: Requests broad storage permissions for caching
4. **ProGuard**: Disabled in release builds (`minifyEnabled false`)

## Development Notes

1. **External Libraries**: Many dependencies are bundled as AAR/JAR files in `app/libs/` rather than fetched from Maven
2. **Database Migration**: Project migrated from GreenDAO to Room
3. **API Compatibility**: Designed for Kikoeru-project V0.6.2 API
4. **Offline Support**: Local caching system allows playback without server connection
5. **Android 14 Support**: Added `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission for Android 14+ compatibility

## License

GPL-3.0 License (see `LICENSE` file)
