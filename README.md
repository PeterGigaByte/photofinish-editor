# PhotoFinish Branding Studio

Java 21 desktop application for automated branding of photofinish images.

## What is included

- JavaFX desktop window with Dashboard, Folders, Template, History, and Updates tabs.
- SQLite database created in the user's application data directory.
- Configurable input and export folders, including Windows local paths and UNC network paths.
- Background folder watcher using `WatchService`.
- Stable-file detection before processing newly written JPG/PNG images.
- Content-aware re-export: an unchanged file that was already exported is skipped, but new content arriving under the same name is re-exported (de-duplication uses the source size + last-modified fingerprint, not just the path).
- Exported images keep the same base name as the input (only the extension follows the chosen output format).
- Pure Java image processing with `BufferedImage`, `Graphics2D`, and `ImageIO`.
- Auto-crop of the empty (no-participant) stretches from long photofinish strips, keeping the original pixels (only the width changes).
- Poster-style canvas controls for portrait/social output, including canvas size, background color, and image fit mode. The `Keep original size` fit mode builds the poster (header, results, logos, text bar) around the native-size photo so the photo is never scaled — the poster width follows the (cropped) photo width.
- Top header controls with event title, subtitle, header colors, and left/right logo images.
- Logo overlay controls: file, position, size, opacity, and x/y offset.
- Optional bottom text bar with template placeholders.
- Optional bottom results table with editable rows and colors.
- Template preview using either a selected sample image or a generated default sample.
- Dark JavaFX UI styling with branded button icons and generated app/installer icon.
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
java -jar target\photofinish-app-0.1.5.jar
```

## Build a Windows installer

Install JDK 21 and run:

```powershell
mvn clean package
.\scripts\Generate-AppIcon.ps1 -OutputPath target\app-icon.ico
jpackage `
  --type exe `
  --name "PhotoFinish Branding Studio" `
  --app-version 0.1.5 `
  --vendor "Rigo" `
  --input target `
  --main-jar photofinish-app-0.1.5.jar `
  --main-class sk.rigo.photofinish.Launcher `
  --dest target\installer `
  --icon target\app-icon.ico `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut
```

The Maven build copies runtime dependencies to `target\lib`, and the jar manifest points to that folder.

The preferred release helper is:

```powershell
.\scripts\Build-Installer.ps1
```

The helper builds the Java package, generates a branded Windows icon, runs `jpackage`, and prints the MSI path and SHA-256.

## Project documentation

- `docs/PROJECT_GUIDE.md` describes architecture and runtime behavior.
- `docs/RELEASE_PROCESS.md` describes versioning, MSI packaging, and update publishing.
- `docs/AI_ASSISTANT_GUIDE.md` describes how Codex, Claude, and other agents should work in this repo.
- `AGENTS.md` is the shared durable instruction file for coding agents.
- `CLAUDE.md` imports `AGENTS.md` for Claude Code.

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
