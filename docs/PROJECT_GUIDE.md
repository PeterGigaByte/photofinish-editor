# Project Guide

PhotoFinish Branding Studio is a JavaFX desktop app that watches an input folder, applies branding overlays to photofinish images, exports processed files, stores processing history locally, and checks for updates.

## Technology

- Java 21
- Maven wrapper
- JavaFX desktop UI
- SQLite via JDBC
- Java `WatchService`
- Java image APIs: `BufferedImage`, `Graphics2D`, `ImageIO`
- `jpackage` plus WiX for Windows MSI packaging

## Runtime Data

On Windows, app data is stored under:

```text
%APPDATA%\PhotoFinish Branding Studio
```

Contents:
- `photofinish.db`
- `logs`
- `staging`
- default `input`
- default `export`

Do not store mutable app data inside the installation directory.

## Package Layout

```text
src/main/java/sk/rigo/photofinish
  MainApp.java
  Launcher.java
  config
  db
  image
  model
  repository
  service
  ui
  update
  watcher
```

## Application Startup

`Launcher` delegates to `MainApp`.

`MainApp`:
- creates app-data paths
- configures file logging
- initializes SQLite schema
- loads settings
- ensures the default branding template exists
- wires repositories and services
- starts JavaFX `MainView`

## Database

Schema is created in `Database.initialize()`.

Tables:
- `app_settings`
- `branding_templates`
- `processed_files`
- `processing_errors`

Repository classes own SQL access. Keep SQL out of UI classes.

`Database.initialize()` also migrates older `branding_templates` rows by adding poster canvas, header, and results-table columns when missing. Do not require users to delete their local database for template feature updates.

## Folder Watching

`FolderWatcherService` watches the configured input folder for JPG/PNG files.

Rules:
- create the input folder if missing
- enqueue existing supported images on watcher start
- use `StableFileDetector` before processing
- de-duplicate by content fingerprint: skip a re-detected file only when its size and last-modified match the stored values; re-queue when the content changed
- never process on the JavaFX UI thread

## Processing Pipeline

`FileProcessingService` handles queueing and processing.

Flow:
1. Queue the file (`queueForProcessing`): insert a `QUEUED` row for a new path, re-queue when the content at a known path changed, or skip when unchanged.
2. Mark `PROCESSING`.
3. Wait for source file stability, then refresh the stored size + last-modified fingerprint.
4. Render branding with `BrandingRenderer`.
5. Write staged output with `ImageExporter`.
6. Copy to export path.
7. Mark `EXPORTED`, `PENDING_EXPORT`, or `FAILED`.

`PENDING_EXPORT` is valid when the export path is offline or inaccessible.

Exported files keep the input's base name (e.g. `race1.jpg` → `race1.<output-extension>`). Re-exporting changed content under the same name overwrites the previous export.

## Image Rendering

`BrandingRenderer` is independent of JavaFX.

Supported features:
- optional fixed-size poster canvas for portrait/social output
- image fit mode: keep original size, cover, contain, or stretch
  - `Keep original size` (`ImageFitMode.ORIGINAL`) draws the photo at native 1:1 pixels and builds the poster around it; the output width follows the (cropped) photo width and the header/results bands are sized relative to the photo height
  - `cover` / `contain` / `stretch` fit the photo into the fixed canvas as before
- auto-crop of empty (no-participant) areas via `PhotofinishAutoCropper`, applied before layout when enabled; only engages for long strips (width ≥ 2.5× height) and never scales pixels
- canvas background color
- top header with title, subtitle, colors, and left/right logo images
- logo overlay
- logo position
- logo size as percent of image width
- opacity
- x/y offsets
- optional bottom text bar
- text template placeholders
- optional bottom results table with editable rows and colors

Preview rendering uses the same renderer, but it does not write output files or create history rows.

## UI

`MainView` is currently code-built JavaFX UI with tabs:
- Dashboard
- Folders
- Template
- History
- Updates

Rules:
- use background workers for I/O, database, network, rendering, and update work
- keep controls responsive
- use `Platform.runLater` only for UI updates
- keep image processing logic outside `ui`
- keep template preview controls wired to the same `BrandingTemplate` fields used by folder processing
- keep the dark UI stylesheet in `src/main/resources/styles/app.css`

## Updates

`UpdateService`:
- fetches a configured `latest.json`
- compares semantic versions
- downloads installer to temp directory
- verifies SHA-256 when provided
- launches installer after app exit for versions `0.1.2+`

The app does not overwrite its own installation files.
