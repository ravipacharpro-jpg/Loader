package com.bgmi.sdk;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads the remote native payload.
 *
 * <p>The .so is never bundled in the APK: it always comes from GitHub and is
 * cached under {@code files/FeaturesticBox.so}.</p>
 */
public final class SoUpdater {

    private static final String TAG = "SoUpdater";

    public static final String SO_URL =
            "https://github.com/ravipacharpro-jpg/Fbox/releases/download/Floader/FeaturesticBox.so";
    public static final String SO_NAME = "FeaturesticBox.so";

    private static final long MIN_VALID_SIZE = 1024L * 1024L; // 1 MB

    public interface Progress {
        void onProgress(int percent, String message);
    }

    private SoUpdater() {
    }

    public static File cached(Context context) {
        return new File(context.getFilesDir(), SO_NAME);
    }

    /**
     * Returns a ready-to-load .so, downloading it if needed.
     *
     * @throws IOException with a user readable message on failure
     */
    public static File ensure(Context context, Progress progress) throws IOException {
        File target = cached(context);
        if (target.isFile() && target.length() > MIN_VALID_SIZE) {
            if (progress != null) progress.onProgress(100, "Payload ready");
            return target;
        }

        File tmp = new File(context.getFilesDir(), SO_NAME + ".part");
        HttpURLConnection conn = null;
        boolean success = false;
        try {
            if (progress != null) progress.onProgress(0, "Downloading payload…");

            conn = (HttpURLConnection) new URL(SO_URL).openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "FeaturesticLoader/1.0");
            conn.setRequestProperty("Accept", "application/octet-stream");
            conn.connect();

            int code = conn.getResponseCode();
            if (code >= 400) {
                throw new IOException("Download failed (HTTP " + code + ")");
            }

            long total = conn.getContentLengthLong();
            long downloaded = 0L;
            int lastPercent = -1;

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(tmp);
            byte[] buffer = new byte[64 * 1024];
            int read;
            try {
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;
                    if (total > 0 && progress != null) {
                        int percent = (int) Math.min(99L, (downloaded * 100L) / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            progress.onProgress(percent,
                                    "Downloading payload… " + percent + "%");
                        }
                    }
                }
                out.flush();
            } finally {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }

            if (downloaded < MIN_VALID_SIZE) {
                throw new IOException("Downloaded payload is incomplete");
            }

            if (target.exists() && !target.delete()) {
                throw new IOException("Could not replace cached payload");
            }
            if (!tmp.renameTo(target)) {
                throw new IOException("Could not save payload");
            }

            //noinspection ResultOfMethodCallIgnored
            target.setReadable(true, false);
            //noinspection ResultOfMethodCallIgnored
            target.setExecutable(true, false);

            success = true;
            if (progress != null) progress.onProgress(100, "Payload ready");
            Log.i(TAG, "payload downloaded: " + target.length() + " bytes");
            return target;
        } finally {
            if (conn != null) conn.disconnect();
            if (!success && tmp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }
}
