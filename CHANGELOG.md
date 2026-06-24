# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- "Play Album" button to play all songs in album
- "Add Album to Playlist" button to add entire album to playlist
- Chinese and English string resources for album actions
- "Rebuild Database" button in settings to sync download status with files
- SAF (Storage Access Framework) DocumentFile API for download path management
  - UI shows converted path for readability
  - Actual file operations use URI for proper scoped storage support

### Changed
- Updated TopAppBar colors to use surfaceContainer theme
- Improved icon tint consistency across the app
- Cache clearing now behaves differently based on download storage type
  - Internal storage: clears everything (database, config, downloaded files)
  - External storage: clears only database and config, preserves downloaded files
- Download operations now use DocumentFile API when URI is available
- Rebuild database now uses DocumentFile API for directory traversal

### Fixed
- ANR in settings screen caused by MusicService not calling startForeground() within 5 seconds
  - Moved startForeground() call to the beginning of onCreate() with fallback notification
  - Full media notification is now updated after ExoPlayer and MediaSession initialization
- Unit tests that referenced removed displayName/title properties
  - Updated PlayModeTest, MusicServiceLogicTest, and NavigationTest to use resource IDs
- Clear cache not properly resetting settings
  - Changed SharedPreferences.clear() from apply() to commit() for synchronous execution
- Download path display showing URI format instead of folder name
  - Improved uriToPath() to properly extract folder name from DocumentFile or URI path

## [1.0.0] - 2026-06-24

### Added
- Core music playback with Media3 ExoPlayer
- Album list and detail screens
- Playlist management
- Download queue
- Settings and about pages
- Light/dark theme support
- Multi-language support (Chinese/English)
