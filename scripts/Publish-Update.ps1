param(
  [Parameter(Mandatory = $true)]
  [string] $Version,

  [Parameter(Mandatory = $true)]
  [string] $InstallerPath,

  [string] $Notes = "See release notes in the project history.",

  [string] $BaseRawUrl = "https://raw.githubusercontent.com/PeterGigaByte/photofinish-editor/master"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

if (-not (Test-Path -LiteralPath $InstallerPath)) {
  throw "Installer not found: $InstallerPath"
}

$VersionDirectory = Join-Path "updates" "v$Version"
New-Item -ItemType Directory -Force $VersionDirectory | Out-Null

$PublishedName = "PhotoFinishBrandingStudio-$Version.msi"
$PublishedPath = Join-Path $VersionDirectory $PublishedName
Copy-Item -LiteralPath $InstallerPath -Destination $PublishedPath -Force

$Hash = Get-FileHash -Algorithm SHA256 -LiteralPath $PublishedPath
$InstallerUrl = "$BaseRawUrl/updates/v$Version/$PublishedName"

$Manifest = [ordered]@{
  version = $Version
  installerUrl = $InstallerUrl
  sha256 = $Hash.Hash
  notes = $Notes
}

$Json = $Manifest | ConvertTo-Json
Set-Content -LiteralPath "updates\latest.json" -Value $Json -Encoding UTF8

Write-Output "Published: $((Resolve-Path $PublishedPath).Path)"
Write-Output "Manifest: $((Resolve-Path 'updates\latest.json').Path)"
Write-Output "URL: $InstallerUrl"
Write-Output "SHA256: $($Hash.Hash)"

