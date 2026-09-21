Add-Type -AssemblyName System.Drawing

$assets = Join-Path $PSScriptRoot '../play-store-assets'
New-Item -ItemType Directory -Force -Path $assets | Out-Null
$launcherPath = Join-Path $PSScriptRoot '../android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png'
$launcher = [System.Drawing.Image]::FromFile((Resolve-Path $launcherPath))
try {
    $icon = [System.Drawing.Bitmap]::new(512, 512)
    $g = [System.Drawing.Graphics]::FromImage($icon)
    try {
        $g.Clear([System.Drawing.Color]::FromArgb(249, 233, 200))
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.DrawImage($launcher, 0, 0, 512, 512)
    } finally { $g.Dispose() }
    try { $icon.Save((Join-Path $assets 'icon-512.png'), [System.Drawing.Imaging.ImageFormat]::Png) }
    finally { $icon.Dispose() }

    $graphic = [System.Drawing.Bitmap]::new(1024, 500)
    $g = [System.Drawing.Graphics]::FromImage($graphic)
    try {
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
        $background = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
            [System.Drawing.Rectangle]::new(0, 0, 1024, 500),
            [System.Drawing.Color]::FromArgb(11, 36, 87),
            [System.Drawing.Color]::FromArgb(22, 65, 148), 0.0)
        $g.FillRectangle($background, 0, 0, 1024, 500)
        $background.Dispose()
        $g.DrawImage($launcher, 72, 86, 328, 328)
        $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
        $soft = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(224, 233, 249))
        $title = [System.Drawing.Font]::new('Segoe UI', 55, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
        $subtitle = [System.Drawing.Font]::new('Segoe UI', 30, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
        try {
            $g.DrawString('Simple Notes+', $title, $white, 453, 164)
            $g.DrawString('Notes, listes, dessins', $subtitle, $soft, 457, 259)
            $g.DrawString('et mémos audio', $subtitle, $soft, 457, 303)
        } finally {
            $title.Dispose(); $subtitle.Dispose(); $white.Dispose(); $soft.Dispose()
        }
    } finally { $g.Dispose() }
    try { $graphic.Save((Join-Path $assets 'feature-graphic-1024x500.png'), [System.Drawing.Imaging.ImageFormat]::Png) }
    finally { $graphic.Dispose() }
} finally { $launcher.Dispose() }
