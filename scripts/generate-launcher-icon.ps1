Add-Type -AssemblyName System.Drawing

$res = Join-Path $PSScriptRoot '../android/app/src/main/res'
$sourceDir = Join-Path $PSScriptRoot 'icon-source'
$sizes = [ordered]@{ mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }
$background = [System.Drawing.Color]::FromArgb(249, 233, 200)
$oldInk = [System.Drawing.Color]::FromArgb(115, 117, 120)
$ink = [System.Drawing.Color]::FromArgb(70, 78, 94)
$plusBlue = [System.Drawing.Color]::FromArgb(22, 65, 148)

function Recolor-Icon([System.Drawing.Bitmap]$bitmap, [string]$kind) {
    if ($kind -eq 'monochrome') { return }
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            $pixel = $bitmap.GetPixel($x, $y)
            if ($pixel.A -eq 0) { continue }
            if ($kind -eq 'foreground') {
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $ink))
                continue
            }
            # Recolor the calendar strokes while preserving the cream background.
            $dr = $background.R - $oldInk.R
            $dg = $background.G - $oldInk.G
            $db = $background.B - $oldInk.B
            $t = (($background.R - $pixel.R) * $dr +
                ($background.G - $pixel.G) * $dg +
                ($background.B - $pixel.B) * $db) / ($dr * $dr + $dg * $dg + $db * $db)
            $t = [Math]::Max(0, [Math]::Min(1, $t))
            $r = [int][Math]::Round($background.R + $t * ($ink.R - $background.R))
            $g = [int][Math]::Round($background.G + $t * ($ink.G - $background.G))
            $b = [int][Math]::Round($background.B + $t * ($ink.B - $background.B))
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $r, $g, $b))
        }
    }
}

function Add-Plus([System.Drawing.Bitmap]$bitmap, [string]$kind) {
    $adaptive = $kind -eq 'foreground' -or $kind -eq 'monochrome'
    $cx = if ($adaptive) { 310 } else { 148 }
    $cy = if ($adaptive) { 130 } else { 49 }
    $radius = if ($adaptive) { 41 } else { 20 }
    $stroke = if ($adaptive) { 17 } else { 8 }
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    try {
        if ($kind -ne 'monochrome') {
            $badge = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
            $graphics.FillEllipse($badge, $cx - $radius, $cy - $radius, 2 * $radius, 2 * $radius)
            $badge.Dispose()
            $outline = [System.Drawing.Pen]::new($plusBlue, $(if ($adaptive) { 4 } else { 2 }))
            $graphics.DrawEllipse($outline, $cx - $radius, $cy - $radius, 2 * $radius, 2 * $radius)
            $outline.Dispose()
        } else {
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $clear = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::Transparent)
            $graphics.FillEllipse($clear, $cx - $radius, $cy - $radius, 2 * $radius, 2 * $radius)
            $clear.Dispose()
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
        }
        $plusColor = if ($kind -eq 'monochrome') { [System.Drawing.Color]::White } else { $plusBlue }
        $pen = [System.Drawing.Pen]::new($plusColor, $stroke)
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
        $halfArm = [int]($radius * 0.58)
        $graphics.DrawLine($pen, $cx - $halfArm, $cy, $cx + $halfArm, $cy)
        $graphics.DrawLine($pen, $cx, $cy - $halfArm, $cx, $cy + $halfArm)
        $pen.Dispose()
    } finally {
        $graphics.Dispose()
    }
}

foreach ($name in @('ic_launcher', 'ic_launcher_round', 'ic_launcher_foreground', 'ic_launcher_monochrome')) {
    $kind = if ($name -eq 'ic_launcher_foreground') { 'foreground' } elseif ($name -eq 'ic_launcher_monochrome') { 'monochrome' } else { 'standard' }
    $source = [System.Drawing.Bitmap]::new((Join-Path $sourceDir "$name.png"))
    try {
        Recolor-Icon $source $kind
        Add-Plus $source $kind
        foreach ($density in $sizes.Keys) {
            $size = $sizes[$density]
            if ($kind -eq 'foreground' -or $kind -eq 'monochrome') { $size = [int]($size * 9 / 4) }
            $output = [System.Drawing.Bitmap]::new($size, $size)
            $graphics = [System.Drawing.Graphics]::FromImage($output)
            try {
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.DrawImage($source, 0, 0, $size, $size)
            } finally {
                $graphics.Dispose()
            }
            try {
                $output.Save((Join-Path $res "mipmap-$density/$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $output.Dispose()
            }
        }
    } finally {
        $source.Dispose()
    }
}
