#!/usr/bin/env bash
# Restore script for Amazon Fire TV Stick Lite
set -euo pipefail

TV_HOST="${1:-${TV_HOST:-192.168.1.9}}"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY="${REPO}/tools/venv/bin/python"
[ -f "${PY}" ] || PY="python3"

echo "Restoring Fire TV packages on ${TV_HOST}..."

while IFS= read -r line; do
    pkg="$(echo "${line}" | sed 's/#.*//' | tr -d '[:space:]')"
    [ -z "${pkg}" ] && continue
    echo "Re-enabling ${pkg}..."
    TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "pm enable ${pkg}" || true
done < "${REPO}/work/debloat_firetv.txt"

echo "Resetting Private DNS to default (opportunistic)..."
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "settings put global private_dns_mode opportunistic"
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "settings delete global private_dns_specifier"

echo "Restore complete! Rebooting Fire TV..."
TV_HOST="${TV_HOST}" "${PY}" "${REPO}/tools/tv.py" "reboot"
