package com.bgmi.sdk;

import android.content.Context;
import android.util.Log;

import com.featurestic.box.entity.pm.InstallResult;
import com.featurestic.sdk.FeaturesticBox;
import com.sbox.core.system.api.BoxActivate;

/** Thin, null-safe wrapper around the FeaturesticBox SDK. */
public final class BoxManager {

    private static final String TAG = "BoxManager";

    /** BGMI (Indian build). */
    public static final String PKG_BGMI = "com.pubg.imobile";
    public static final int USER_ID = 0;

    private BoxManager() {
    }

    public static void init(Context context) {
        try {
            // Do not keep the daemon service alive: the loader is lightweight.
            FeaturesticBox.init(context, false);
        } catch (Throwable t) {
            Log.e(TAG, "init failed", t);
        }
    }

    public static void activate() {
        try {
            BoxActivate.activateSdk("FEATURESTIC");
        } catch (Throwable t) {
            Log.w(TAG, "activate failed", t);
        }
    }

    public static boolean isInstalled() {
        try {
            return FeaturesticBox.isInstalled(PKG_BGMI, USER_ID);
        } catch (Throwable t) {
            Log.e(TAG, "isInstalled failed", t);
            return false;
        }
    }

    /** Installs the host APK into the virtual container (no root required). */
    public static boolean install() {
        try {
            InstallResult result = FeaturesticBox.installPackageAsUser(PKG_BGMI, USER_ID);
            if (result == null) return false;
            if (!result.success) {
                Log.w(TAG, "install failed: " + result.msg);
            }
            return result.success;
        } catch (Throwable t) {
            Log.e(TAG, "install failed", t);
            return false;
        }
    }

    /**
     * Loads the remote payload and launches BGMI inside the virtual container.
     *
     * @param soPath absolute path of the downloaded {@code FeaturesticBox.so}
     */
    public static boolean launch(Context context, String soPath) {
        activate();
        try {
            return FeaturesticBox.startGame(context, PKG_BGMI, USER_ID, soPath);
        } catch (Throwable t) {
            Log.e(TAG, "startGame failed", t);
            return false;
        }
    }

    public static void stop() {
        try {
            FeaturesticBox.stopGame(PKG_BGMI, USER_ID);
        } catch (Throwable ignored) {
        }
    }
}
