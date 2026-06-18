# AI Assistant Guide

This guide explains how Codex, Claude Code, and similar assistants should work in this repository.

## Instruction Files

- `AGENTS.md` is the shared project instruction file.
- `CLAUDE.md` imports `AGENTS.md` so Claude Code receives the same project rules.
- `CLAUDE.local.md` is for personal local notes and must not be committed.

Keep `AGENTS.md` concise enough to load reliably. Put longer reference material in `docs/`.

## Operating Principles

1. Read the local context first.
2. Preserve unrelated user changes.
3. Make narrow, maintainable changes.
4. Run the most relevant verification command.
5. Update documentation in the same change when behavior changes.
6. Commit and push only when the user asks, or when the ongoing workflow has already established publishing as part of the task.

## Documentation Must Stay Current

Update docs when changing:
- UI screens, controls, labels, or workflows
- folder watcher behavior
- image rendering/export behavior
- external API integrations or API-backed workflows
- update check, download, or installer launch behavior
- database schema
- app-data/log/staging paths
- build commands
- release packaging
- public update manifest layout

Documentation targets:
- `README.md`: user-facing quick start and high-level features
- `docs/PROJECT_GUIDE.md`: architecture and development guide
- `docs/RELEASE_PROCESS.md`: versioning, MSI, and update publishing
- `AGENTS.md`: durable agent rules
- `updates/latest.json`: live update channel

## Standard Workflow

For code changes:

```powershell
$env:JAVA_HOME='C:\Users\PeterRigo\.jdks\temurin-21.0.11'
.\mvnw.cmd clean package -DskipTests
```

For release changes:

```powershell
.\scripts\Build-Installer.ps1
.\scripts\Publish-Update.ps1 -Version <version> -InstallerPath "target\installer\PhotoFinish Branding Studio-<version>.msi"
```

For update-channel changes, verify:
- `updates/latest.json` version matches the intended release
- `installerUrl` points to the committed installer
- `sha256` matches the installer file
- the repository or hosting location is public if installed clients need anonymous access

## Review Checklist

Before final response:
- working tree state is known
- build/test/package command result is known
- installer path and hash are known for releases
- docs were updated or consciously not needed
- no `target/` files are staged
- no local databases/logs/AppData files are staged
- no personal secrets or local paths were added except documented examples
