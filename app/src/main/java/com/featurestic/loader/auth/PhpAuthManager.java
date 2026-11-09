package com.featurestic.loader.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.featurestic.loader.AppConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Universal & Zoro/Box PHP Panel Authenticator:
 * Sends POST game=PUBG&user_key=<key>&serial=<uuid>
 * Parses JSON responses:
 *   - {"status": true, "data": {"token": "...", "rng": ..., "EXP": "2026-12-31"}}
 *   - {"status": false, "reason": "..."}
 */
public class PhpAuthManager {

    private static final String PREF_NAME = "featurestic_apple_auth";
    private static final String KEY_SAVED_LICENSE = "saved_license_key";
    private static final String KEY_SAVED_EXPIRY = "saved_expiry";
    private static final String KEY_REMEMBER = "remember_key";
    private static final String KEY_FALLBACK_HWID = "fallback_hwid";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface AuthCallback {
        void onSuccess(String licenseKey, String expiryText, String serverMessage);
        void onError(String errorMessage);
    }

    private static String normalizeUrl(String rawUrl) {
        if (rawUrl == null) return "";
        String u = rawUrl.trim();
        if (u.regionMatches(true, 0, "https://", 0, 8)) {
            u = "https://" + u.substring(8);
        } else if (u.regionMatches(true, 0, "http://", 0, 7)) {
            u = "http://" + u.substring(7);
        } else if (!u.contains("://") && !u.isEmpty()) {
            u = "https://" + u;
        }
        if (u.startsWith("https://github.com/") && u.contains("/blob/")) {
            u = u.replace("https://github.com/", "https://raw.githubusercontent.com/")
                 .replaceFirst("/blob/", "/");
        }
        return u;
    }

    public static void verifyKey(final Context context, final String rawKey, final AuthCallback callback) {
        final String key = rawKey != null ? rawKey.trim() : "";
        if (key.isEmpty()) {
            callback.onError("Please enter your license key");
            return;
        }

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String serialUuid = computePanelSerialUuid(context, key);

                    if (key.equalsIgnoreCase(AppConfig.MASTER_TEST_KEY)) {
                        Thread.sleep(500);
                        final String expiry = "VIP Lifetime • Active";
                        saveSession(context, key, expiry, true);
                        MAIN_HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(key, expiry, "Verified with Master Key");
                            }
                        });
                        return;
                    }

                    String endpoint = normalizeUrl(AppConfig.PHP_PANEL_URL);
                    boolean isStaticRawUrl = endpoint.contains("raw.githubusercontent.com")
                            || endpoint.endsWith(".json")
                            || endpoint.endsWith(".txt");

                    URL url = new URL(endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setDoInput(true);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Charset", "UTF-8");
                    conn.setRequestProperty("User-Agent", "Dalvik/2.1.0 (Linux; U; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ")");

                    if (isStaticRawUrl) {
                        conn.setRequestMethod("GET");
                    } else {
                        conn.setRequestMethod("POST");
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        String postData = "game=" + URLEncoder.encode(AppConfig.PHP_GAME_TAG, "UTF-8")
                                + "&user_key=" + URLEncoder.encode(key, "UTF-8")
                                + "&serial=" + URLEncoder.encode(serialUuid, "UTF-8");
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(postData.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    }

                    int code = conn.getResponseCode();
                    InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                    StringBuilder sb = new StringBuilder();
                    if (is != null) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
                        }
                    }

                    String body = sb.toString().trim();
                    // Strip UTF-8 BOM or PHP warnings before '{' if any
                    int jsonStart = body.indexOf('{');
                    if (jsonStart > 0) {
                        body = body.substring(jsonStart);
                    }

                    if (body.isEmpty()) {
                        postError(callback, "Empty response from panel (HTTP " + code + ")");
                        return;
                    }

                    if (body.startsWith("{")) {
                        JSONObject json = new JSONObject(body);
                        boolean ok = false;
                        if (json.has("status")) {
                            boolean stBool = json.optBoolean("status", false);
                            String stStr = json.optString("status", "").trim();
                            ok = stBool
                                    || "true".equalsIgnoreCase(stStr)
                                    || "success".equalsIgnoreCase(stStr)
                                    || "1".equalsIgnoreCase(stStr)
                                    || "ok".equalsIgnoreCase(stStr);
                        } else if (json.optBoolean("success", false)) {
                            ok = true;
                        }

                        if (ok) {
                            String expiry = "Active License";
                            if (json.has("data") && json.optJSONObject("data") != null) {
                                JSONObject data = json.optJSONObject("data");
                                expiry = data.optString("EXP",
                                        data.optString("expiry",
                                        data.optString("expired_date",
                                        data.optString("exp", "Active License"))));
                            } else {
                                expiry = json.optString("EXP",
                                        json.optString("expiry",
                                        json.optString("expired_date",
                                        json.optString("exp", "Active License"))));
                            }
                            final String finalExpiry = expiry;
                            final String msg = json.optString("message", "License authenticated");
                            saveSession(context, key, finalExpiry, true);
                            MAIN_HANDLER.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(key, finalExpiry, msg);
                                }
                            });
                        } else {
                            String reason = json.optString("reason",
                                    json.optString("message",
                                    json.optString("error", "Invalid or expired license key")));
                            postError(callback, reason);
                        }
                    } else if (body.equalsIgnoreCase("true") || body.equalsIgnoreCase("success") || body.startsWith("OK")) {
                        final String finalExpiry = "Active License";
                        saveSession(context, key, finalExpiry, true);
                        MAIN_HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(key, finalExpiry, "License authenticated");
                            }
                        });
                    } else {
                        postError(callback, body.length() > 120 ? "Invalid response from panel (HTTP " + code + ")" : body);
                    }

                } catch (final Exception e) {
                    postError(callback, "Connection error: " + e.getMessage());
                }
            }
        });
    }

    private static void postError(final AuthCallback callback, final String msg) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(msg);
            }
        });
    }

    /**
     * Computes the exact UUID serial expected by Zoro / Box PHP Panels (Login.h):
     * UUID.nameUUIDFromBytes((userKey + androidId + Build.MODEL + Build.BRAND).getBytes())
     */
    public static String computePanelSerialUuid(Context context, String userKey) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null) androidId = "";
            String seed = (userKey != null ? userKey : "") + androidId + Build.MODEL + Build.BRAND;
            return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
        } catch (Throwable t) {
            return getDeviceHwid(context);
        }
    }

    public static String getDeviceHwid(Context context) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null) androidId = "";
            String seed = androidId + Build.MODEL + Build.BRAND;
            return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString().toUpperCase();
        } catch (Throwable t) {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String id = sp.getString(KEY_FALLBACK_HWID, null);
            if (id == null) {
                id = UUID.randomUUID().toString().toUpperCase();
                sp.edit().putString(KEY_FALLBACK_HWID, id).apply();
            }
            return id;
        }
    }

    public static void saveSession(Context context, String key, String expiry, boolean remember) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_SAVED_LICENSE, key)
                .putString(KEY_SAVED_EXPIRY, expiry)
                .putBoolean(KEY_REMEMBER, remember)
                .apply();
    }

    public static String getSavedKey(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SAVED_LICENSE, "");
    }

    public static String getSavedExpiry(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SAVED_EXPIRY, "Active Session");
    }

    public static void clearSession(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_SAVED_LICENSE).remove(KEY_SAVED_EXPIRY).apply();
    }
}
