package com.bgmi.utils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.lsposed.lsparanoid.Obfuscate;

/**
 * Downloads the FeaturesticBox native payload directly (no ZIP / password step).
 * The file is stored as {@code files/FeaturesticBox.so} and handed to
 * {@code FeaturesticBox.startGame(...)}.
 */
@Obfuscate
public class Downtwo extends AsyncTask<String, String, String> {
    private static final String TAG = "Downtwo";

    /** Remote payload (replaces the old libzenin.so). */
    public static final String SO_URL =
            "https://github.com/ravipacharpro-jpg/Fbox/releases/download/Floader/FeaturesticBox.so";

    public static final String SO_NAME = "FeaturesticBox.so";
    private static final long MIN_VALID_SIZE = 1024L * 1024L; // 1 MB sanity check

    public static String Version() {
        return "1";
    }

    public static String Link() {
        return SO_URL;
    }

    public interface Callback {
        void onComplete(boolean success);
    }

    public interface ProgressListener {
        void onProgress(int percent);
    }

    private final Context context;
    private final Callback callback;
    private ProgressListener progressListener;

    public Downtwo(Context context) {
        this(context, null);
    }

    public Downtwo(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            String downloadUrl = (params.length > 0 && params[0] != null) ? params[0] : SO_URL;

            File target = new File(context.getFilesDir(), SO_NAME);
            if (target.exists() && target.length() > MIN_VALID_SIZE) {
                return "No Update Available";
            }

            publishProgress("Downloading payload...");
            return downloadSo(downloadUrl, target);
        } catch (Exception e) {
            Log.e(TAG, "doInBackground error: ", e);
            return "Background error: " + e.getMessage();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        boolean success = (result == null) || "No Update Available".equals(result);

        try {
            if (callback != null) {
                callback.onComplete(success);
            }
        } catch (Exception e) {
            Log.e(TAG, "Callback threw exception: ", e);
        }

        if (!success) {
            showFinalMessage("Error: " + result);
        }
    }

    private String downloadSo(String urlString, File target) {
        HttpURLConnection connection = null;
        File tmp = new File(target.getParentFile(), SO_NAME + ".part");
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int code = connection.getResponseCode();
            if (code >= 400) {
                return "HTTP " + code;
            }

            int totalSize = connection.getContentLength();
            long downloaded = 0;

            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(tmp);
            byte[] data = new byte[65536];
            int count;

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                downloaded += count;

                if (totalSize > 0 && progressListener != null) {
                    int percent = (int) ((downloaded * 100L) / totalSize);
                    progressListener.onProgress(percent);
                }
            }

            output.flush();
            output.close();
            input.close();

            if (downloaded < MIN_VALID_SIZE) {
                tmp.delete();
                return "Downloaded file too small";
            }

            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) {
                tmp.delete();
                return "Could not save payload";
            }

            target.setReadable(true, false);
            return null; // Success
        } catch (Exception e) {
            Log.e(TAG, "Error downloading payload: ", e);
            return "Download failed: " + e.getMessage();
        } finally {
            if (connection != null) connection.disconnect();
            if (tmp.exists()) tmp.delete();
        }
    }

    private void showFinalMessage(String message) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        try {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Update Status")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        try {
                            dialog.dismiss();
                        } catch (Exception ignored) {}
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show final message dialog: ", e);
        }
    }
}
