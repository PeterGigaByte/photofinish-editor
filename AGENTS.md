# AGENTS.md

This file is the primary project guidance for Codex and other coding agents.

## Project

PhotoFinish Branding Studio is a Windows-first Java 21 desktop app for automated photofinish image branding.

Core stack:
- Java 21
- Maven
- JavaFX
- SQLite through JDBC
- `BufferedImage`, `Graphics2D`, and `ImageIO` for image processing
- `jpackage`/WiX for Windows MSI packaging

## Hard Rules

- Preserve existing user work. Do not reset, checkout, or delete unrelated changes.
- Keep the app simple and maintainable. No Spring Boot unless there is a clear, documented reason.
- Never block the JavaFX UI thread with filesystem, network, database, image rendering, update, or packaging work.
- Keep image processing independent from JavaFX UI classes.
- Store app data under the user app-data directory, not beside the installed executable.
- Treat update behavior as production-sensitive. Always verify installer SHA-256 and never self-overwrite the running app.
- Do not commit `target/`, temp packaging directories, local AppData files, databases, logs, or personal assistant notes.

## Documentation Rule

Every behavior change must update documentation in the same commit when relevant.

Update at least one of these when behavior, build, packaging, update, database, UI, or workflow changes:
- `README.md`
- `docs/PROJECT_GUIDE.md`
- `docs/RELEASE_PROCESS.md`
- `docs/AI_ASSISTANT_GUIDE.md`
- `updates/latest.json` when publishing an installable release

Before finishing a task, check whether the docs still match:
- current app version
- run/build commands
- installer filename and SHA-256
- update manifest URL and schema
- Java package layout
- app-data paths
- UI features
- known packaging caveats

## Common Commands

Use JDK 21. On this machine the known local JDK path has been:

```powershell
$env:JAVA_HOME='C:\Users\PeterRigo\.jdks\temurin-21.0.11'
```

Build:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Run from Maven:

```powershell
.\mvnw.cmd javafx:run
```

Build MSI helper:

```powershell
.\scripts\Build-Installer.ps1
```

Publish update manifest helper:

```powershell
.\scripts\Publish-Update.ps1 -Version 0.1.4 -InstallerPath "target\installer\PhotoFinish Branding Studio-0.1.4.msi"
```

## Architecture Map

Main packages under `src/main/java/sk/rigo/photofinish`:
- `config`: app metadata, paths, settings, logging
- `db`: SQLite connection and schema initialization
- `model`: templates, status, history, errors, update data
- `repository`: JDBC persistence
- `service`: app context and background processing orchestration
- `watcher`: folder watcher and stable-file detection
- `image`: renderer, text template engine, export logic
- `update`: semantic versioning, update checks, download/install launch
- `ui`: JavaFX screens and controls

## Implementation Notes

- Watcher and processing services must use background executors.
- Preview rendering must not create history rows or export files.
- `PENDING_EXPORT` is the expected state when export output cannot be copied.
- Retry export should copy from staged output, not reprocess the source image.
- Update checks read `latest.json` with string fields: `version`, `installerUrl`, `sha256`, `notes`.
- Installed app versions before `0.1.2` download installers but do not auto-launch them.
- App versions `0.1.2+` launch the downloaded installer after the app exits.

## Release Invariants

For any installable release:
- bump `pom.xml`
- bump `src/main/resources/application.properties`
- update README examples
- build the MSI
- copy the MSI to `updates/vX.Y.Z/PhotoFinishBrandingStudio-X.Y.Z.msi`
- update `updates/latest.json`
- verify SHA-256
- commit code, docs, manifest, and installer together
- push to GitHub

The update channel only works for installed apps if the `latest.json` URL and installer URL are publicly reachable.

## Verification Expectations

For Java code changes, run:

```powershell
.\mvnw.cmd clean package -DskipTests
```

For packaging/update changes, additionally run:

```powershell
.\scripts\Build-Installer.ps1
.\scripts\Publish-Update.ps1 -Version <version> -InstallerPath "target\installer\PhotoFinish Branding Studio-<version>.msi"
```

Report any command that cannot be run and why.

