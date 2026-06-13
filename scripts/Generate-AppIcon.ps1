param(
  [Parameter(Mandatory = $true)]
  [string] $OutputPath
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

Add-Type -AssemblyName System.Drawing

$ResolvedOutputDirectory = Split-Path -Parent $OutputPath
if (-not [string]::IsNullOrWhiteSpace($ResolvedOutputDirectory)) {
  New-Item -ItemType Directory -Force $ResolvedOutputDirectory | Out-Null
}

$Bitmap = New-Object System.Drawing.Bitmap 256, 256
$Graphics = [System.Drawing.Graphics]::FromImage($Bitmap)
$Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$Graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

try {
  $DarkBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 17, 21, 29))
  $BlueBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 63, 182, 255))
  $WhiteBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
  $GreenBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 168, 240, 122))
  $LanePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(190, 231, 238, 247), 5)
  $RedPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 242, 67, 67), 8)
  $Font = [System.Drawing.Font]::new("Segoe UI", 58, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)

  $Graphics.FillRectangle($DarkBrush, 0, 0, 256, 256)
  $Graphics.FillRectangle($BlueBrush, 22, 24, 212, 70)
  $Graphics.DrawString("PF", $Font, $WhiteBrush, 52, 24)

  foreach ($Y in 126, 150, 174, 198) {
    $Graphics.DrawLine($LanePen, 34, $Y, 222, $Y)
  }

  $Graphics.FillRectangle($GreenBrush, 42, 146, 172, 17)
  $Graphics.DrawLine($RedPen, 150, 106, 150, 222)

  $PngStream = New-Object System.IO.MemoryStream
  $Bitmap.Save($PngStream, [System.Drawing.Imaging.ImageFormat]::Png)
  $PngBytes = $PngStream.ToArray()

  $FileStream = [System.IO.File]::Create($OutputPath)
  $Writer = [System.IO.BinaryWriter]::new($FileStream)
  try {
    $Writer.Write([UInt16] 0)
    $Writer.Write([UInt16] 1)
    $Writer.Write([UInt16] 1)
    $Writer.Write([byte] 0)
    $Writer.Write([byte] 0)
    $Writer.Write([byte] 0)
    $Writer.Write([byte] 0)
    $Writer.Write([UInt16] 1)
    $Writer.Write([UInt16] 32)
    $Writer.Write([UInt32] $PngBytes.Length)
    $Writer.Write([UInt32] 22)
    $Writer.Write($PngBytes)
  } finally {
    $Writer.Dispose()
    $FileStream.Dispose()
  }
} finally {
  $Graphics.Dispose()
  $Bitmap.Dispose()
}

Write-Output "Icon: $((Resolve-Path $OutputPath).Path)"
