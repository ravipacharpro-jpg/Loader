package com.bgmi.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small typed wrapper around the loader's preferences. */
public final class Prefs {

    private static final String FILE = "featurestic_loader";
    private static final String K_KEY = "license_key";
    private static final String K_EXPIRY = "license_expiry";
    private static final String K_OBB = "obb_ready";
    private static final String EXPIRY_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        this.sp = context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String key() {
        return sp.getString(K_KEY, "");
    }

    public void setKey(String key) {
        sp.edit().putString(K_KEY, key == null ? "" : key.trim()).apply();
    }

    public String expiry() {
        return sp.getString(K_EXPIRY, "");
    }

    public void setExpiry(String expiry) {
        sp.edit().putString(K_EXPIRY, expiry == null ? "" : expiry.trim()).apply();
    }

    public boolean obbReady() {
        return sp.getBoolean(K_OBB, false);
    }

    public void setObbReady(boolean ready) {
        sp.edit().putBoolean(K_OBB, ready).apply();
    }

    public void clearSession() {
        sp.edit()
                .remove(K_KEY)
                .remove(K_EXPIRY)
                .remove(K_OBB)
                .apply();
    }

    /** Parsed expiry in millis, or 0 when the stored value is missing/invalid. */
    public long expiryMillis() {
        String value = expiry();
        if (value.isEmpty()) return 0L;
        try {
            SimpleDateFormat f = new SimpleDateFormat(EXPIRY_PATTERN, Locale.US);
            f.setLenient(false);
            Date date = f.parse(value);
            return date == null ? 0L : date.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    public boolean isSessionValid() {
        return !key().isEmpty() && expiryMillis() > System.currentTimeMillis();
    }
}
