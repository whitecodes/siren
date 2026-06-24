# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- "Play Album" button to play all songs in album
- "Add Album to Playlist" button to add entire album to playlist
- Chinese and English string resources for album actions

### Changed
- Updated TopAppBar colors to use surfaceContainer theme
- Improved icon tint consistency across the app

### Fixed
- ANR in settings screen caused by MusicService not calling startForeground() within 5 seconds
  - Moved startForeground() call to the beginning of onCreate() with fallback notification
  - Full media notification is now updated after ExoPlayer and MediaSession initialization

## [1.0.0] - 2026-06-24

### Added
- Core music playback with Media3 ExoPlayer
- Album list and detail screens
- Playlist management
- Download queue
- Settings and about pages
- Light/dark theme support
- Multi-language support (Chinese/English)
