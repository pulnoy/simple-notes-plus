Add-Type -AssemblyName System.Drawing

$res = Join-Path $PSScriptRoot '../android/app/src/main/res'
$sizes = @{
    mdpi = 48
    hdpi = 72
    xhdpi = 96
    xxhdpi = 144
    xxxhdpi = 192
}

function RoundedPath([float]$x, [float]$y, [float]$width, [float]$height, [float]$radius) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = $radius * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $width - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $width - $d, $y + $height - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $height - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function Render-Icon([int]$size, [string]$kind, [string]$path) {
    $bitmap = [System.Drawing.Bitmap]::new($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.ScaleTransform($size / 432.0, $size / 432.0)

    $blue = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(22, 65, 148))
    $cream = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(249, 233, 200))
    $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    try {
        if ($kind -eq 'round') {
            $graphics.FillEllipse($cream, 0, 0, 432, 432)
        } elseif ($kind -eq 'standard') {
            $graphics.FillRectangle($cream, 0, 0, 432, 432)
        }

        if ($kind -eq 'monochrome') {
            $outline = RoundedPath 113 89 206 254 30
            $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::White, 25)
            $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
            $graphics.DrawPath($pen, $outline)
            $pen.Dispose()
            $outline.Dispose()
            $plusBrush = $white
        } else {
            $notebook = RoundedPath 101 77 230 278 34
            $graphics.FillPath($blue, $notebook)
            $notebook.Dispose()
            $plusBrush = $white
        }

        $plus = RoundedPath 194 151 44 130 13
        $graphics.FillPath($plusBrush, $plus)
        $plus.Dispose()
        $cross = RoundedPath 151 194 130 44 13
        $graphics.FillPath($plusBrush, $cross)
        $cross.Dispose()
    } finally {
        $blue.Dispose()
        $cream.Dispose()
        $white.Dispose()
        $graphics.Dispose()
    }
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

foreach ($density in $sizes.Keys) {
    $dir = Join-Path $res "mipmap-$density"
    $size = $sizes[$density]
    Render-Icon $size standard (Join-Path $dir 'ic_launcher.png')
    Render-Icon $size round (Join-Path $dir 'ic_launcher_round.png')
    Render-Icon ($size * 9 / 4) foreground (Join-Path $dir 'ic_launcher_foreground.png')
    Render-Icon ($size * 9 / 4) monochrome (Join-Path $dir 'ic_launcher_monochrome.png')
}
