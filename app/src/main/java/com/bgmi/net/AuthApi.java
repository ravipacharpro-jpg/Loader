package com.bgmi.net;

import android.content.Context;

import com.bgmi.util.DeviceId;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** License panel client (same endpoint + token formula as before). */
public final class AuthApi {

    public static final String AUTH_URL = "https://zoro.manishflash.online/connect";
    private static final String AUTH_SALT = "Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E";
    private static final String GAME = "PUBG";

    private AuthApi() {
    }

    public static final class Result {
        public final boolean ok;
        public final String message;
        public final String expiry;

        Result(boolean ok, String message, String expiry) {
            this.ok = ok;
            this.message = message;
            this.expiry = expiry;
        }
    }

    public static Result login(Context context, String userKey) {
        HttpURLConnection conn = null;
        try {
            String key = userKey == null ? "" : userKey.trim();
            String serial = DeviceId.serial(context, key);

            String body = "game=" + GAME
                    + "&user_key=" + URLEncoder.encode(key, "UTF-8")
                    + "&serial=" + URLEncoder.encode(serial, "UTF-8");

            conn = (HttpURLConnection) new URL(AUTH_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "FeaturesticLoader/1.0");

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = readAll(is);

            if (response == null || response.trim().isEmpty()) {
                return new Result(false, "Empty response from server (" + code + ")", "");
            }

            JSONObject root = new JSONObject(response);
            if (!root.optBoolean("status", false)) {
                String reason = root.optString("reason", "Invalid license key");
                return new Result(false, reason, "");
            }

            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                return new Result(false, "Malformed server response", "");
            }

            String expiry = data.optString("EXP", "").trim();
            String token = data.optString("token", "");
            long rng = data.optLong("rng", 0L);

            if (rng > 0L && rng + 30L <= System.currentTimeMillis() / 1000L) {
                return new Result(false, "Server time expired. Please try again.", "");
            }

            String expected = md5(GAME + "-" + key + "-" + serial + "-" + AUTH_SALT);
            if (!expected.equalsIgnoreCase(token)) {
                return new Result(false, "License verification failed", "");
            }

            if (expiry.isEmpty() || "NULL".equalsIgnoreCase(expiry)) {
                return new Result(false, "License has no expiry date", "");
            }

            return new Result(true, "OK", expiry);
        } catch (Exception e) {
            String message = e.getMessage();
            return new Result(false, message == null ? "Network error" : message, "");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private static String md5(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(digest.length * 2);
        for (byte b : digest) out.append(String.format("%02x", b));
        return out.toString();
    }
}
