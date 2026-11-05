#!/usr/bin/env bash
# Restore script for Changhong AI PONT (ikebukuro)
set -euo pipefail

TV_HOST="${1:-${TV_HOST:-192.168.1.20}}"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY="${REPO}/tools/venv/bin/python"
[ -f "${PY}" ] || PY="python3"

echo "Restoring packages on ${TV_HOST}..."

PKGS=(
    "tv.anoki.acr.controller"
    "com.homwee.aipont"
    "com.google.android.tvrecommendations"
    "android.autoinstalls.config.homwee"
    "com.homwee.store"
    "com.homwee.help"
    "com.google.android.feedback"
    "com.google.android.play.games"
    "com.google.android.videos"
    "com.google.android.youtube.tvmusic"
    "com.google.android.syncadapters.calendar"
    "com.google.android.katniss"
    "com.homwee.multiscreenshare"
    "com.homwee.mmp.screenshare"
    "com.google.android.backdrop"
    "com.disney.disneyplus"
    "com.tiktok.tv"
    "com.ted.android.tv"
    "com.google.android.tvlauncher"
)

for pkg in "${PKGS[@]}"; do
    echo "Re-enabling ${pkg}..."
    TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "pm enable ${pkg}" || true
done

echo "Re-enabling stock launcher as default..."
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "cmd package set-home-activity com.google.android.tvlauncher/.MainActivity" || true

echo "Resetting Private DNS to default (opportunistic)..."
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "settings put global private_dns_mode opportunistic"
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "settings delete global private_dns_specifier"

echo "Restore complete! Rebooting TV..."
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "reboot"
