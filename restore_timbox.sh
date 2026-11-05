#!/usr/bin/env bash
TV_HOST="${1:-192.168.1.48}"
export TV_HOST

packages=(
    "com.technicolor.autoperiodicreboot"
    "it.telecomitalia.thefilmclub"
    "it.telecomitalia.games"
    "it.telecomitalia.timmusic_tv"
    "it.telecomitalia.timppapp"
    "com.technicolor.tim.customization"
    "com.technicolor.project.releasenote"
    "com.figc.vivoazzurrotv"
    "it.rds.androidtv.rdssocialtv"
    "com.cbs.ca"
    "com.upst.hayu"
    "tv.pluto.android"
    "it.mediaset.infinitytv"
    "com.dazn"
    "com.wbd.stream"
    "com.apple.atve.androidtv.appletv"
    "com.disney.disneyplus"
    "com.amazon.amazonvideo.livingroom"
    "timvision.telemetry"
    "it.telecomitalia.digitalsupport"
    "it.telecomitalia.linemanager"
    "com.movenda.omadm.tv"
    "com.movenda.p4mcore"
    "tv.broadpeak.nanocdnwrapper"
    "com.technicolor.zta"
    "com.technicolor.android.BootupTraceQR"
    "com.google.android.tvrecommendations"
    "com.google.android.leanbacklauncher.recommendations"
    "com.google.android.feedback"
    "android.autoinstalls.config.technicolor"
    "com.netflix.ninja"
    "com.google.android.youtube.tv"
    "com.google.android.youtube.tvkids"
    "com.google.android.youtube.tvmusic"
    "com.google.android.play.games"
    "com.google.android.videos"
    "com.google.android.tvtutorials"
    "timvision.launcher"
)

echo "Re-enabling packages on TIM Box ($TV_HOST)..."
for pkg in "${packages[@]}"; do
    python3 cleanyourtv/tools/tv.py pm enable "$pkg"
done

echo "Setting stock launcher as home..."
python3 cleanyourtv/tools/tv.py cmd package set-home-activity timvision.launcher/com.canal.box.ui.boot.BoxMainActivity

echo "Restore complete!"
