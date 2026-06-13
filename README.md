# PhotoFinish Branding Studio

Java 21 desktop application for automated branding of photofinish images.

## What is included

- JavaFX desktop window with Dashboard, Folders, Template, History, and Updates tabs.
- SQLite database created in the user's application data directory.
- Configurable input and export folders, including Windows local paths and UNC network paths.
- Background folder watcher using `WatchService`.
- Stable-file detection before processing newly written JPG/PNG images.
- Duplicate protection through the `processed_files.source_path` database constraint.
- Pure Java image processing with `BufferedImage`, `Graphics2D`, and `ImageIO`.
- Logo overlay controls: file, position, size, opacity, and x/y offset.
- Optional bottom text bar with template placeholders.
- Template preview using either a selected sample image or a generated default sample.
- Pending export state when the export destination is unavailable, plus retry action.
- Update check/download service for a configurable `latest.json` URL with semantic version comparison and optional SHA-256 verification.
- Maven package setup suitable for `jpackage`.

## App data

On Windows the application stores data under:

```text
%APPDATA%\PhotoFinish Branding Studio
```

That directory contains:

- `photofinish.db`
- `logs`
- `staging`
- default `input` and `export` folders until changed in the GUI

## Run

```powershell
mvn javafx:run
```

Or build the jar and run it:

```powershell
mvn clean package
java -jar target\photofinish-app-0.1.3.jar
```

## Build a Windows installer

Install JDK 21 and run:

```powershell
mvn clean package
jpackage `
  --type exe `
  --name "PhotoFinish Branding Studio" `
  --app-version 0.1.3 `
  --vendor "Rigo" `
  --input target `
  --main-jar photofinish-app-0.1.3.jar `
  --main-class sk.rigo.photofinish.Launcher `
  --dest target\installer `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut
```

The Maven build copies runtime dependencies to `target\lib`, and the jar manifest points to that folder.

## `latest.json` example

Update checking is disabled until a real `latest.json` URL is configured in the Folders tab.

For the current GitHub-hosted update channel, use:

```text
https://raw.githubusercontent.com/PeterGigaByte/photofinish-editor/master/updates/latest.json
```

```json
{
  "version": "0.2.0",
  "installerUrl": "https://example.com/downloads/PhotoFinishBrandingStudio-0.2.0.exe",
  "sha256": "optional-installer-sha256-hex",
  "notes": "Short release notes shown in the Updates tab."
}
```

The app downloads the installer to a temp directory, launches it, and then exits so the installer can upgrade the app.
