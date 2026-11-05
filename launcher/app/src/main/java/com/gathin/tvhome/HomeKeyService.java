package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends the remote's Home button to this launcher on remotes that emit KEYCODE_ALL_APPS for Home
 * (Xiaomi sticks and boxes). Android TV 9 routes that key only to the system-image launcher, so once
 * the stock launcher is disabled the button does nothing. Every other key passes through untouched.
 *
 * With stock_home_redirect set, it also sends the stock launcher's home screen back here, for firmware that turns
 * that launcher on again after it was disabled (XGIMI's xrmservice does, on its own remote keys).
 *
 * Enable over adb:
 *   settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
 *   settings put secure accessibility_enabled 1
 */
public class HomeKeyService extends AccessibilityService {

    /** restore_key_services was on when the config was read: this helper is noted for ServiceRepair. */
    private boolean repairRecord;
    /** Remote buttons that should open Settings (config.xml, settings_key_scancodes and settings_key_keycodes). */
    private int[] settingsScancodes = new int[0];
    private int[] settingsKeycodes = new int[0];
    private boolean settingsPanel;
    /** app_keys, the same maps InputKeyService reads, for setups that run only this service. */
    private Map<Integer, String> appKeys = Collections.emptyMap();
    private Map<Integer, String> appScancodes = Collections.emptyMap();
    private Map<Integer, String> holdAppKeys = Collections.emptyMap();
    private Map<Integer, String> holdAppScancodes = Collections.emptyMap();

    /** Waits for the user to unlock when Android binds this service at boot; null once the config is read. */
    private BroadcastReceiver unlockReceiver;
    /** The stock launcher's HOME activities (stock_home_redirect), disabled ones included. */
    private final Set<String> stockHome = new HashSet<>();
    private static volatile HomeKeyService sInstance = null;
    private View overlayView = null;

    public static void dismissOverlay() {
        HomeKeyService instance = sInstance;
        if (instance != null) {
            instance.hideOverlay();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new android.accessibilityservice.AccessibilityServiceInfo();
        }
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.notificationTimeout = 50;
        setServiceInfo(info);

        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        // The service is directBootAware: Android 11 skips a service that is not while the user is still
        // locked at boot, writes accessibility_enabled=0 and, on TCL G03 firmware, never binds it after the
        // unlock. Config.load reads credential-protected storage, so wait for the unlock before reading it.
        UserManager users = getSystemService(UserManager.class);
        if (users.isUserUnlocked()) {
            loadConfig();
            return;
        }
        unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopWaitingForUnlock();
                loadConfig();
            }
        };
        registerReceiver(unlockReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        // The unlock may have happened between the check and the registration.
        if (users.isUserUnlocked()) {
            stopWaitingForUnlock();
            loadConfig();
        }
    }

    private void stopWaitingForUnlock() {
        if (unlockReceiver == null) return;
        unregisterReceiver(unlockReceiver);
        unlockReceiver = null;
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        hideOverlay();
        stopWaitingForUnlock();
        super.onDestroy();
    }

    private void loadConfig() {
        // Keycodes and the panel choice can also come from a theme; theme.sh restarts this service.
        Config config = Config.load(this);
        // restore_key_services: note that this helper runs, so Home can put it back after a force stop. Here, not
        // in onServiceConnected: the note is in credential-protected storage too, readable only after the unlock.
        repairRecord = config.restoreKeyServices;
        if (repairRecord) ServiceRepair.connected(this, getClass());
        settingsKeycodes = config.settingsKeycodes;
        settingsPanel = config.settingsKeyPanel;
        appKeys = RemoteKeys.parseAppKeys(config.appKeys);
        appScancodes = RemoteKeys.parseAppScancodes(config.appKeys);
        RemoteKeys.applyKeyFilter(this, config.keyFilter);
        stockHome.clear();
        if (!config.stockHomeRedirect.isEmpty()) {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(config.stockHomeRedirect);
            for (ResolveInfo ri : getPackageManager().queryIntentActivities(home, PackageManager.MATCH_DISABLED_COMPONENTS)) {
                stockHome.add(ri.activityInfo.name);
            }
            if ("com.amazon.tv.launcher".equals(config.stockHomeRedirect)) {
                stockHome.add("com.amazon.tv.launcher.ui.HomeActivity_vNext");
                stockHome.add("com.amazon.tv.launcher.ui.HomeActivity");
                stockHome.add("com.amazon.firehomestarter.HomeStarterActivity");
            }
        }
        holdAppKeys = RemoteKeys.parseHoldAppKeys(config.appKeys);
        holdAppScancodes = RemoteKeys.parseHoldAppScancodes(config.appKeys);
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        for (int code : settingsKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // A scancode entry is more specific than any keycode, so it goes first.
        if (RemoteKeys.handle(this, event, appScancodes.get(event.getScanCode()),
                holdAppScancodes.get(event.getScanCode()))) return true;
        // The stock launcher handled the remote's Settings button; with it disabled the button does nothing.
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = settingsPanel ? new Intent(this, QuickSettingsActivity.class)
                        : SystemSettings.intent(this, Settings.ACTION_SETTINGS);
                try {
                    startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (ActivityNotFoundException | SecurityException unavailable) {
                    Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        if (RemoteKeys.handle(this, event, appKeys.get(event.getKeyCode()),
                holdAppKeys.get(event.getKeyCode()))) return true;
        if (event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS && event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            showOverlayAndReturn();
        }
        return true;
    }

    private synchronized void showOverlayAndReturn() {
        Log.i("TvHome.Key", "showOverlayAndReturn invoked");
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                if (wm != null && overlayView == null) {
                    FrameLayout fl = new FrameLayout(this);
                    fl.setBackgroundColor(0xFF000000);
                    int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            type,
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                            PixelFormat.OPAQUE);
                    wm.addView(fl, lp);
                    overlayView = fl;
                    Log.i("TvHome.Key", "Black overlay added to WindowManager");
                }
            } catch (Exception e) {
                Log.e("TvHome.Key", "Failed to add overlay", e);
            }

            returnToCleanYourTv();
        });
    }

    public synchronized void hideOverlay() {
        if (overlayView != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    if (wm != null && overlayView != null) {
                        wm.removeView(overlayView);
                        overlayView = null;
                        Log.i("TvHome.Key", "Overlay removed");
                    }
                } catch (Exception e) {
                    Log.e("TvHome.Key", "Failed to remove overlay", e);
                }
            });
        }
    }

    private void returnToCleanYourTv() {
        Log.i("TvHome.Key", "Returning to CleanYourTV");

        // 1. Move CleanYourTV task to front
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                int taskId = MainActivity.sTaskId;
                if (taskId != -1) {
                    Log.i("TvHome.Key", "Moving MainActivity.sTaskId=" + taskId + " to front");
                    am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
                } else {
                    List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(20);
                    if (tasks != null) {
                        for (ActivityManager.RunningTaskInfo task : tasks) {
                            if (task.baseActivity != null && "com.gathin.tvhome".equals(task.baseActivity.getPackageName())) {
                                Log.i("TvHome.Key", "Found task id=" + task.id + ", moving to front");
                                am.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TvHome.Key", "moveTaskToFront failed", e);
        }

        // 2. Immediate startActivity
        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(home);
        } catch (Exception e) {
            Log.e("TvHome.Key", "startActivity immediate failed", e);
        }

        // 3. Retries up to past the stopAppSwitches window (5000ms)
        scheduleTaskCheck(150);
        scheduleTaskCheck(350);
        scheduleTaskCheck(750);
        scheduleTaskCheck(1500);
        scheduleTaskCheck(3000);
        scheduleTaskCheck(5100);
        new Handler(Looper.getMainLooper()).postDelayed(this::hideOverlay, 5500);
    }

    private void scheduleTaskCheck(int delayMs) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                    if (tasks != null && !tasks.isEmpty()) {
                        ComponentName top = tasks.get(0).topActivity;
                        if (top != null && "com.gathin.tvhome".equals(top.getPackageName())) {
                            hideOverlay();
                            return; // CleanYourTV is already on top!
                        }
                    }
                    int taskId = MainActivity.sTaskId;
                    if (taskId != -1) {
                        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
                    }
                    startActivity(new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                }
            } catch (Exception ignored) {}
        }, delayMs);
    }

    /** Unbound by Android: the user turned the helper off, so Home doesn't put it back (ServiceRepair). */
    @Override
    public boolean onUnbind(Intent intent) {
        if (repairRecord) ServiceRepair.disconnected(this, getClass());
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cls = event.getClassName();
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String pkgStr = pkg.toString();
        String clsStr = cls != null ? cls.toString() : "";

        // On TIM Box, timvision.launcher is the system priv-app home.
        // If it comes up and was NOT recently launched by the user, return to CleanYourTV.
        if ("timvision.launcher".equals(pkgStr)) {
            long elapsed = SystemClock.uptimeMillis() - AppAdapter.lastLaunchTime;
            boolean recent = elapsed < 4000 && "timvision.launcher".equals(AppAdapter.lastLaunchedPkg);
            Log.i("TvHome.Key", "timvision event: cls=" + clsStr + " elapsed=" + elapsed + " recent=" + recent);
            if (!recent && !clsStr.contains("Player")) {
                Log.i("TvHome.Key", "Intercepting timvision: covering and returning to CleanYourTV");
                showOverlayAndReturn();
                return;
            }
        }

        // stock_home_redirect: the firmware turned the stock launcher back on and its home screen came up.
        if (stockHome.isEmpty()) return;
        boolean match = cls != null && stockHome.contains(clsStr);
        if (!match && "com.amazon.tv.launcher".equals(pkgStr)) {
            if (clsStr.contains("HomeActivity") || clsStr.contains("HomeStarterActivity")) {
                match = true;
            }
        }
        if (match) {
            showOverlayAndReturn();
        }
    }

    @Override
    public void onInterrupt() {
    }
}
