# Restore script for Amazon Fire TV Stick Lite
param(
    [string]$TvHost = "192.168.1.9"
)

$env:TV_HOST = $TvHost
$repo = $PSScriptRoot
$py = Join-Path $repo "tools\venv\Scripts\python.exe"
if (-not (Test-Path $py)) {
    $py = "python"
}

Write-Host "Restoring Fire TV packages on $TvHost..." -ForegroundColor Cyan

$pkgs = Get-Content (Join-Path $repo "work\debloat_firetv.txt") |
    ForEach-Object { ($_ -replace '#.*$', '').Trim() } |
    Where-Object { $_ }

foreach ($pkg in $pkgs) {
    Write-Host "Re-enabling $pkg..."
    & $py (Join-Path $repo "tools\tv.py") "pm enable $pkg"
}

Write-Host "Resetting Private DNS to default (opportunistic)..."
& $py (Join-Path $repo "tools\tv.py") "settings put global private_dns_mode opportunistic"
& $py (Join-Path $repo "tools\tv.py") "settings delete global private_dns_specifier"

Write-Host "Restore complete! Rebooting Fire TV..." -ForegroundColor Green
& $py (Join-Path $repo "tools\tv.py") "reboot"
