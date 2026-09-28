Add-Type -AssemblyName System.Drawing
$resourceRoot = Join-Path $PSScriptRoot '../android/app/src/main/res'
$selected = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'icon-source-selected.png'))
$sizes = [ordered]@{ mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }
function Save-SelectedIcon([string]$path, [int]$size, [bool]$adaptive = $false, [bool]$round = $false) {
    $bitmap = [System.Drawing.Bitmap]::new($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        if ($round) {
            $clip = [System.Drawing.Drawing2D.GraphicsPath]::new()
            $clip.AddEllipse(0, 0, $size, $size)
            $graphics.SetClip($clip)
            $clip.Dispose()
        }
        $artSize = if ($adaptive) { [int]($size * 2 / 3) } else { $size }
        $offset = [int](($size - $artSize) / 2)
        $graphics.DrawImage($selected, $offset, $offset, $artSize, $artSize)
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $bitmap.Dispose() }
}
try {
    foreach ($density in $sizes.Keys) {
        $size = $sizes[$density]
        $folder = Join-Path $resourceRoot "mipmap-$density"
        Save-SelectedIcon (Join-Path $folder 'ic_launcher.png') $size
        Save-SelectedIcon (Join-Path $folder 'ic_launcher_round.png') $size $false $true
        Save-SelectedIcon (Join-Path $folder 'ic_launcher_foreground.png') ([int]($size * 9 / 4)) $true
    }
    Save-SelectedIcon (Join-Path $PSScriptRoot '../play-store-assets/icon-512.png') 512
} finally { $selected.Dispose() }
