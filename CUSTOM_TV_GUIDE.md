# Custom Android TV Setup & Recovery Guide
**Device:** Changhong "AI PONT" (Codename: `ikebukuro`)  
**OS Version:** Android TV 11 (API level 30)  
**Firmware Build:** `RTO2.250804.001`  
**IP Address:** `192.168.1.20`  

---

## 1. What Was Done

1. **Full Device Backup:**
   - Saved pre-modification state in `cleanyourtv/backup/`:
     - `packages_all.txt`: Complete package catalog.
     - `packages_disabled.txt`: Factory disabled packages (`com.google.android.marvin.talkback`, `com.cltv.fast`).
     - `getprop.txt`: Full system properties.
     - `settings_global.txt`, `settings_secure.txt`, `settings_system.txt`: System settings tables.

2. **System Debloat (Safe User-Level Disables):**
   - **Telemetry & Sponsored Ads:**
     - `tv.anoki.acr.controller` (Anoki ACR telemetry)
     - `com.homwee.aipont` (Vendor portal & ad recommendation channels)
     - `com.google.android.tvrecommendations`
     - `android.autoinstalls.config.homwee` (Silent app installer)
   - **Preloaded Unused Apps & Vendor Junk:**
     - `com.homwee.store`, `com.homwee.help`
     - `com.google.android.feedback`, `com.google.android.play.games`
     - `com.google.android.videos`, `com.google.android.youtube.tvmusic`
     - `com.google.android.syncadapters.calendar`
     - `com.disney.disneyplus`, `com.tiktok.tv`, `com.ted.android.tv`
   - **Unused System Features (Per Your Selection):**
     - `com.google.android.katniss` (Voice Assistant)
     - `com.homwee.multiscreenshare`, `com.homwee.mmp.screenshare` (Miracast)
     - `com.google.android.backdrop` (Ambient screensaver)
   - **Preserved & Untouched:**
     - Chromecast built-in (`com.google.android.apps.mediashell`)
     - Firmware updates (`com.homwee.dmt.upgrade`)
     - Tuner / Live TV & Hardware inputs (`com.mediatek.*`)
     - Bluetooth remote pairing, audio, display, keyboard (Latin IME)
     - Google Play Store & Play Services
     - Streaming & player apps: Netflix, YouTube, Prime Video, RaiPlay, Stremio, Moonlight, imPlayer, NordVPN, Downloader

3. **Private DNS Ad-Blocking:**
   - Configured AdGuard Private DNS (`dns.adguard-dns.com`) for system-wide blocking of ad servers and telemetry trackers.
   - Note: DNS-level ad blocking does not block YouTube in-video ads (which are served directly from YouTube video stream CDNs), but protects your privacy across the TV.

4. **Apple TV Style Native Launcher:**
   - Built native lightweight Android TV launcher (`com.gathin.tvhome`) matching SDK 30.
   - Dock pinned apps (5 columns): **Live TV**, **YouTube**, **Stremio**, **NordVPN**, **iMPlayer TV**.
   - Connected network pill directly to **NordVPN** (shows real-time VPN status in top bar).
   - Top bar glass menu for **Inputs** (HDMI 1, HDMI 2, AV, Live TV).
   - Set as default home activity and disabled stock `com.google.android.tvlauncher`.

---

## 2. How to Re-enable Any App Individually

To restore any specific app, run this command from PowerShell inside `cleanyourtv`:

```powershell
$env:TV_HOST = "192.168.1.20"
python tools\tv.py "pm enable <package_name>"
```

**Common package names to restore if you ever want them back:**
- Google Assistant (Voice button): `com.google.android.katniss`
- Disney+: `com.disney.disneyplus`
- TikTok TV: `com.tiktok.tv`
- TED TV: `com.ted.android.tv`
- YouTube Music: `com.google.android.youtube.tvmusic`
- Miracast Screen Sharing: `com.homwee.multiscreenshare`

---

## 3. How to Restore Everything to Factory Defaults

To revert all debloating, restore the stock Google TV launcher, and reset Private DNS back to default:

**PowerShell (Windows):**
```powershell
.\restore_my_tv.ps1 -TvHost 192.168.1.20
```

**Bash / Linux / macOS:**
```bash
./restore_my_tv.sh 192.168.1.20
```

To undo Private DNS manually without full restore:
```powershell
$env:TV_HOST = "192.168.1.20"
python tools\tv.py "settings put global private_dns_mode opportunistic"
python tools\tv.py "settings delete global private_dns_specifier"
```

---

## 4. Customizing the Launcher (Dock Order, Themes & Wallpapers)

### Option A: Change Dock Order or Apps via Theme Config (Instant, No Rebuild)
You can push any layout configuration or wallpaper without compiling:
Create or edit a JSON file (e.g., in `themes/mytheme/config.json`):
```json
{
  "dock_apps": [
    "com.mediatek.wwtv.tvcenter",
    "com.google.android.youtube.tv",
    "com.stremio.one",
    "com.nordvpn.android"
  ],
  "columns": 4,
  "background_color": "#000000"
}
```
Push it to the TV:
```powershell
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.20 themes\mytheme
```

### Option B: Rebuild & Install
Edit `launcher\app\src\main\res\values\config.xml`, then run:
```powershell
$env:JAVA_HOME = "C:\Users\alexi\.jdks\ms-17.0.20"
$env:ANDROID_HOME = "C:\Users\alexi\AppData\Local\Android\Sdk"
.\launcher\build-and-install.ps1 -TvHost 192.168.1.20
```

---

## 5. Final Step: Securing Your TV (Closing ADB)

Network ADB on port 5555 is plaintext. When you are done configuring, close it:
1. On your TV: go to **Settings → Device Preferences → Developer options**.
2. Turn **USB debugging** OFF (and **Network debugging** OFF if separate).
3. Select **Revoke USB debugging authorizations**.
4. Reboot the TV.
5. Verify from your PC:
   ```powershell
   python tools\adb-closed.py 192.168.1.20
   ```
   It should confirm that the port connection is refused.
