package com.bgmi.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Device identity helpers used by the license panel. */
public final class DeviceId {

    private DeviceId() {
    }

    public static String androidId(Context context) {
        try {
            String id = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "" : id;
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Same formula the license panel expects:
     * UUIDv3(KEY + ANDROID_ID + MODEL + BRAND).
     */
    public static String serial(Context context, String userKey) {
        String raw = (userKey == null ? "" : userKey)
                + androidId(context)
                + Build.MODEL
                + Build.BRAND;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String deviceName() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        String name = (manufacturer + " " + model).trim();
        if (name.isEmpty()) name = "Android device";
        return name;
    }
}
