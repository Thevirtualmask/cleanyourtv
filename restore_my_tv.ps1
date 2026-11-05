# Restore script for Changhong AI PONT (ikebukuro)
param(
    [string]$TvHost = "192.168.1.20"
)

$env:TV_HOST = $TvHost
$repo = $PSScriptRoot
$py = Join-Path $repo "tools\venv\Scripts\python.exe"
if (-not (Test-Path $py)) {
    $py = "python"
}

Write-Host "Restoring packages on $TvHost..." -ForegroundColor Cyan

$pkgs = @(
    "tv.anoki.acr.controller",
    "com.homwee.aipont",
    "com.google.android.tvrecommendations",
    "android.autoinstalls.config.homwee",
    "com.homwee.store",
    "com.homwee.help",
    "com.google.android.feedback",
    "com.google.android.play.games",
    "com.google.android.videos",
    "com.google.android.youtube.tvmusic",
    "com.google.android.syncadapters.calendar",
    "com.google.android.katniss",
    "com.homwee.multiscreenshare",
    "com.homwee.mmp.screenshare",
    "com.google.android.backdrop",
    "com.disney.disneyplus",
    "com.tiktok.tv",
    "com.ted.android.tv",
    "com.google.android.tvlauncher"
)

foreach ($pkg in $pkgs) {
    Write-Host "Re-enabling $pkg..."
    & $py (Join-Path $repo "tools\tv.py") "pm enable $pkg"
}

Write-Host "Re-enabling stock launcher as default..."
& $py (Join-Path $repo "tools\tv.py") "cmd package set-home-activity com.google.android.tvlauncher/.MainActivity"

Write-Host "Resetting Private DNS to default (opportunistic)..."
& $py (Join-Path $repo "tools\tv.py") "settings put global private_dns_mode opportunistic"
& $py (Join-Path $repo "tools\tv.py") "settings delete global private_dns_specifier"

Write-Host "Restore complete! Rebooting TV to clean cache..." -ForegroundColor Green
& $py (Join-Path $repo "tools\tv.py") "reboot"
