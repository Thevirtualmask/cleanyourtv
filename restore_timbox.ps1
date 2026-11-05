# Restore script for TIM_BOX (uzw4010tim)
param (
    [string]$TvHost = "192.168.1.48"
)

$packages = @(
    "com.technicolor.autoperiodicreboot",
    "it.telecomitalia.thefilmclub",
    "it.telecomitalia.games",
    "it.telecomitalia.timmusic_tv",
    "it.telecomitalia.timppapp",
    "com.technicolor.tim.customization",
    "com.technicolor.project.releasenote",
    "com.figc.vivoazzurrotv",
    "it.rds.androidtv.rdssocialtv",
    "com.cbs.ca",
    "com.upst.hayu",
    "tv.pluto.android",
    "it.mediaset.infinitytv",
    "com.dazn",
    "com.wbd.stream",
    "com.apple.atve.androidtv.appletv",
    "com.disney.disneyplus",
    "com.amazon.amazonvideo.livingroom",
    "timvision.telemetry",
    "it.telecomitalia.digitalsupport",
    "it.telecomitalia.linemanager",
    "com.movenda.omadm.tv",
    "com.movenda.p4mcore",
    "tv.broadpeak.nanocdnwrapper",
    "com.technicolor.zta",
    "com.technicolor.android.BootupTraceQR",
    "com.google.android.tvrecommendations",
    "com.google.android.leanbacklauncher.recommendations",
    "com.google.android.feedback",
    "android.autoinstalls.config.technicolor",
    "com.netflix.ninja",
    "com.google.android.youtube.tv",
    "com.google.android.youtube.tvkids",
    "com.google.android.youtube.tvmusic",
    "com.google.android.play.games",
    "com.google.android.videos",
    "com.google.android.tvtutorials",
    "timvision.launcher"
)

Write-Host "Re-enabling all packages on TIM Box ($TvHost)..." -ForegroundColor Cyan
$env:TV_HOST = $TvHost
foreach ($pkg in $packages) {
    Write-Host "Enabling $pkg..."
    python cleanyourtv/tools/tv.py pm enable $pkg
}

Write-Host "Resetting stock launcher as Home..." -ForegroundColor Cyan
python cleanyourtv/tools/tv.py cmd package set-home-activity timvision.launcher/com.canal.box.ui.boot.BoxMainActivity

Write-Host "Restore complete!" -ForegroundColor Green
