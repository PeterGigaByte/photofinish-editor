# Release Process

This project publishes Windows MSI installers and a simple `latest.json` update manifest.

## Version Files

For every release, update:

- `pom.xml`
- `src/main/resources/application.properties`
- README examples
- `updates/latest.json`

Use semantic versions such as `0.1.4`.

## Build

Use JDK 21.

```powershell
$env:JAVA_HOME='C:\Users\PeterRigo\.jdks\temurin-21.0.11'
.\mvnw.cmd clean package -DskipTests
```

## Build Installer

Preferred helper:

```powershell
.\scripts\Build-Installer.ps1
```

The helper:
- reads the project version from `pom.xml`
- builds the Maven package
- stages `target\jpackage-input`
- generates `target\app-icon.ico`
- downloads portable WiX 3.11.2 when needed
- runs `jpackage`
- falls back to a validation-suppressed WiX link step when local Windows Installer ICE validation is unavailable
- prints the MSI path and SHA-256

Expected output:

```text
target\installer\PhotoFinish Branding Studio-<version>.msi
```

## Publish Update

After building the MSI:

```powershell
.\scripts\Publish-Update.ps1 -Version <version> -InstallerPath "target\installer\PhotoFinish Branding Studio-<version>.msi"
```

The helper:
- copies the MSI to `updates\v<version>\PhotoFinishBrandingStudio-<version>.msi`
- calculates SHA-256
- updates `updates/latest.json`

## Update Manifest

`updates/latest.json` must contain string fields:

```json
{
  "version": "0.1.8",
  "installerUrl": "https://raw.githubusercontent.com/PeterGigaByte/photofinish-editor/master/updates/v0.1.8/PhotoFinishBrandingStudio-0.1.8.msi",
  "sha256": "installer-sha256",
  "notes": "Release notes shown in the Updates tab."
}
```

The `installerUrl` and manifest URL must be public for installed apps to check updates without credentials.

## Installed Version Behavior

- `0.1.0` downloads installers only. The user must run the MSI manually.
- `0.1.1` fixes the placeholder update URL handling.
- `0.1.2+` downloads, verifies, schedules installer launch, and exits the app.

## Commit Checklist

Before committing a release:
- build succeeds
- MSI exists
- SHA-256 in `latest.json` matches the MSI
- README version examples are current
- release notes are meaningful
- update files are staged
- `target/` is not staged

Then:

```powershell
git add README.md pom.xml src/main/resources/application.properties updates/latest.json updates\v<version>\PhotoFinishBrandingStudio-<version>.msi
git commit -m "Release <version>"
git push
```
