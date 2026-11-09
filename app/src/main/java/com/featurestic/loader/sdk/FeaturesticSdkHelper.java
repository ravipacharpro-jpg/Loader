package com.featurestic.loader.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import com.featurestic.loader.AppConfig;
import com.featurestic.sdk.FeaturesticBox;

/**
 * Clean SDK Controller that separates FeaturesticBox Virtual Engine calls from UI code.
 * Handles:
 *   1. Display Over Other Apps (SYSTEM_ALERT_WINDOW / Floating Permission)
 *   2. Android 11–16 All Files Access (MANAGE_EXTERNAL_STORAGE) for /SdCard/0/Android/obb
 *   3. Android 13+ Notification Permission (POST_NOTIFICATIONS)
 *   4. 4-Layer Host Game Detection & 4-Layer OBB Sync into /storage/emulated/0/SdCard/0/Android/obb/<pkg>
 *   5. One-Click Game Launch inside FeaturesticBox with automatic .so plugin staging
 */
public final class FeaturesticSdkHelper {

    private static final String PREF_SETUP = "featurestic_setup_state";

    private FeaturesticSdkHelper() {}

    public static void attachBaseContext(Context base) {
        try {
            FeaturesticBox.doAttachBaseContext(base);
        } catch (Throwable ignored) {
        }
    }

    public static void initEngine(Context context) {
        try {
            FeaturesticBox.doCreate(context);
            FeaturesticBox.setFeature("hide_root", AppConfig.ENABLE_HIDE_ROOT);
            FeaturesticBox.setFeature("hide_xposed", AppConfig.ENABLE_HIDE_XPOSED);
            FeaturesticBox.setFeature("low_latency_network", AppConfig.ENABLE_LOW_LATENCY);
            if (AppConfig.ENABLE_AUTO_120_FPS) {
                FeaturesticBox.get().setTargetGameFps(120);
            }
            // Plugin .so is staged after login / during game launch so App Open stays 100% instant
        } catch (Throwable ignored) {
        }
    }

    public static void requestRuntimePermissionsOnStart(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                java.util.List<String> perms = new java.util.ArrayList<>();
                if (Build.VERSION.SDK_INT >= 33) {
                    if (activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                        perms.add("android.permission.POST_NOTIFICATIONS");
                    }
                    if (activity.checkSelfPermission("android.permission.READ_MEDIA_IMAGES") != PackageManager.PERMISSION_GRANTED) {
                        perms.add("android.permission.READ_MEDIA_IMAGES");
                        perms.add("android.permission.READ_MEDIA_VIDEO");
                        perms.add("android.permission.READ_MEDIA_AUDIO");
                    }
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    if (activity.checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                        perms.add("android.permission.BLUETOOTH_CONNECT");
                    }
                }
                if (activity.checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                    perms.add("android.permission.RECORD_AUDIO");
                }
                if (activity.checkSelfPermission("android.permission.READ_PHONE_STATE") != PackageManager.PERMISSION_GRANTED) {
                    perms.add("android.permission.READ_PHONE_STATE");
                }
                if (Build.VERSION.SDK_INT < 33) {
                    if (activity.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                        perms.add("android.permission.READ_EXTERNAL_STORAGE");
                        perms.add("android.permission.WRITE_EXTERNAL_STORAGE");
                    }
                }
                if (!perms.isEmpty()) {
                    activity.requestPermissions(perms.toArray(new String[0]), 1002);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Checks if "Display over other apps" (Float / Overlay) permission is granted.
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 23 && context != null) {
            try {
                return Settings.canDrawOverlays(context);
            } catch (Throwable ignored) {
                return true;
            }
        }
        return true;
    }

    /**
     * Opens the system "Display over other apps" settings screen for this Loader.
     */
    public static void requestOverlayPermission(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName())
                );
                activity.startActivityForResult(intent, 1003);
            } catch (Throwable ignored) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    activity.startActivityForResult(intent, 1003);
                } catch (Throwable ignored2) {}
            }
        }
    }

    /**
     * Checks if All Files Access (Android 11+) or Legacy Storage Permission (Android 10) is granted.
     */
    public static boolean hasAllFilesPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                return Environment.isExternalStorageManager();
            } catch (Throwable ignored) {
                return true;
            }
        }
        if (Build.VERSION.SDK_INT >= 23 && context != null) {
            return context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Opens the Android 11+ "Allow access to manage all files" settings screen for this Loader,
     * or requests READ/WRITE_EXTERNAL_STORAGE on Android 10.
     */
    public static void requestAllFilesPermission(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, 1001);
                return;
            } catch (Throwable ignored) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, 1001);
                    return;
                } catch (Throwable ignored2) {}
            }
        }
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                activity.requestPermissions(new String[]{
                        "android.permission.READ_EXTERNAL_STORAGE",
                        "android.permission.WRITE_EXTERNAL_STORAGE"
                }, 1001);
            } catch (Throwable ignored) {}
        }
    }

    public static PackageInfo findHostGameInfo(Context context, String packageName) {
        return FeaturesticBox.findHostGameInfo(context, packageName);
    }

    public static boolean isInstalledOnHost(Context context, String packageName) {
        return FeaturesticBox.isGameInstalledOnHost(context, packageName);
    }

    public static boolean hasHostObbFiles(Context context, String packageName) {
        return FeaturesticBox.hasHostObb(context, packageName);
    }

    public static boolean hasVirtualObbFiles(String packageName) {
        return FeaturesticBox.hasVirtualObb(packageName);
    }

    public static boolean isGameSetupReady(final Context context, final String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) return false;
        SharedPreferences sp = context.getSharedPreferences(PREF_SETUP, Context.MODE_PRIVATE);
        boolean markedDone = sp.getBoolean("setup_done_" + packageName, false);
        boolean installedInBox = false;
        try {
            installedInBox = FeaturesticBox.isInstalled(packageName, 0);
        } catch (Throwable ignored) {}
        if (markedDone && isInstalledOnHost(context, packageName)) {
            if (!installedInBox) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!FeaturesticBox.isInstalled(packageName, 0)) {
                                FeaturesticBox.installGame(packageName, 0);
                            }
                        } catch (Throwable ignored) {}
                    }
                }, "Featurestic-AutoHeal-Pkg").start();
            }
            return true;
        }
        return installedInBox && (markedDone || hasVirtualObbFiles(packageName));
    }

    public static void markGameSetupState(Context context, String packageName, boolean completed) {
        SharedPreferences sp = context.getSharedPreferences(PREF_SETUP, Context.MODE_PRIVATE);
        sp.edit().putBoolean("setup_done_" + packageName, completed).apply();
    }

    public static void setupGameEnvironment(final Context context, final String packageName, final FeaturesticBox.SetupCallback callback) {
        FeaturesticBox.setupGameAsync(context, packageName, 0, new FeaturesticBox.SetupCallback() {
            @Override
            public void onProgress(int percent, String statusTitle, String statusDetail) {
                if (callback != null) {
                    callback.onProgress(percent, statusTitle, statusDetail);
                }
            }

            @Override
            public void onComplete(boolean success, String message) {
                boolean installed = FeaturesticBox.isInstalled(packageName, 0);
                if (!installed) {
                    boolean retryOk = FeaturesticBox.installGame(packageName, 0);
                    installed = FeaturesticBox.isInstalled(packageName, 0) || retryOk;
                }
                boolean finalSuccess = success || installed;
                markGameSetupState(context, packageName, finalSuccess);
                if (callback != null) {
                    callback.onComplete(finalSuccess, message);
                }
            }
        });
    }

    public static boolean launchVirtualGame(Context context, String packageName) {
        return FeaturesticBox.startGame(
                context,
                packageName,
                0,
                AppConfig.PLUGIN_SO_URL_OR_PATH
        );
    }
}
