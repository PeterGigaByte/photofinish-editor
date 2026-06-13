param(
  [string] $Version,
  [string] $JavaHome = $env:JAVA_HOME,
  [switch] $SkipMaven
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

if ([string]::IsNullOrWhiteSpace($Version)) {
  [xml] $Pom = Get-Content -Raw "pom.xml"
  $Version = $Pom.project.version
}

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
  $JavaHome = "C:\Users\PeterRigo\.jdks\temurin-21.0.11"
}

$JPackage = Join-Path $JavaHome "bin\jpackage.exe"
if (-not (Test-Path -LiteralPath $JPackage)) {
  throw "jpackage.exe not found at $JPackage"
}

if (-not $SkipMaven) {
  $env:JAVA_HOME = $JavaHome
  & ".\mvnw.cmd" clean package -DskipTests
}

$JarName = "photofinish-app-$Version.jar"
$JarPath = Join-Path "target" $JarName
if (-not (Test-Path -LiteralPath $JarPath)) {
  throw "Jar not found: $JarPath"
}

$InputDir = Join-Path "target" "jpackage-input"
if (Test-Path -LiteralPath $InputDir) {
  Remove-Item -LiteralPath $InputDir -Recurse -Force
}
New-Item -ItemType Directory -Force $InputDir | Out-Null
Copy-Item -LiteralPath $JarPath -Destination $InputDir -Force
Copy-Item -LiteralPath "target\lib" -Destination $InputDir -Recurse -Force

$WixRoot = Join-Path "target" "wix"
$Candle = Join-Path $WixRoot "candle.exe"
$Light = Join-Path $WixRoot "light.exe"
if (-not (Test-Path -LiteralPath $Candle) -or -not (Test-Path -LiteralPath $Light)) {
  New-Item -ItemType Directory -Force $WixRoot | Out-Null
  $ZipPath = Join-Path $WixRoot "wix311-binaries.zip"
  Invoke-WebRequest -Uri "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip" -OutFile $ZipPath
  Expand-Archive -LiteralPath $ZipPath -DestinationPath $WixRoot -Force
}

$InstallerDir = Join-Path "target" "installer"
New-Item -ItemType Directory -Force $InstallerDir | Out-Null
$IconPath = Join-Path "target" "app-icon.ico"
& (Join-Path $PSScriptRoot "Generate-AppIcon.ps1") -OutputPath $IconPath
$ResolvedIconPath = (Resolve-Path $IconPath).Path

$TempDir = Join-Path "target" ("jpackage-temp-msi-" + (Get-Date -Format "yyyyMMddHHmmss"))
New-Item -ItemType Directory -Force $TempDir | Out-Null

$env:PATH = (Resolve-Path $WixRoot).Path + ";" + $env:PATH
$InstallerPath = Join-Path $InstallerDir "PhotoFinish Branding Studio-$Version.msi"

& $JPackage `
  --temp $TempDir `
  --type msi `
  --name "PhotoFinish Branding Studio" `
  --app-version $Version `
  --vendor "Rigo" `
  --input $InputDir `
  --main-jar $JarName `
  --main-class sk.rigo.photofinish.Launcher `
  --dest $InstallerDir `
  --icon $ResolvedIconPath `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut

if (-not (Test-Path -LiteralPath $InstallerPath)) {
  $NewestTemp = Get-ChildItem -LiteralPath "target" -Directory -Filter "jpackage-temp-msi-*" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

  if ($null -eq $NewestTemp) {
    throw "jpackage did not create an MSI and no temp directory was found."
  }

  $TempDir = $NewestTemp.FullName
  & $Light -nologo -spdb `
    -ext WixUtilExtension `
    -out $InstallerPath `
    -ext WixUIExtension `
    -b (Join-Path $TempDir "config") `
    -sval `
    -loc (Join-Path $TempDir "config\MsiInstallerStrings_de.wxl") `
    -loc (Join-Path $TempDir "config\MsiInstallerStrings_en.wxl") `
    -loc (Join-Path $TempDir "config\MsiInstallerStrings_ja.wxl") `
    -loc (Join-Path $TempDir "config\MsiInstallerStrings_zh_CN.wxl") `
    -cultures:en-us `
    (Join-Path $TempDir "wixobj\main.wixobj") `
    (Join-Path $TempDir "wixobj\bundle.wixobj") `
    (Join-Path $TempDir "wixobj\ui.wixobj") `
    (Join-Path $TempDir "wixobj\InstallDirNotEmptyDlg.wixobj")
}

$Hash = Get-FileHash -Algorithm SHA256 -LiteralPath $InstallerPath
Write-Output "Installer: $((Resolve-Path $InstallerPath).Path)"
Write-Output "SHA256: $($Hash.Hash)"
