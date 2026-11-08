package com.bgmi.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.lsposed.lsparanoid.Obfuscate;
@Obfuscate
public class Downtwo extends AsyncTask<String, String, String> {
    private static final String TAG = "Downtwo";

    public static native String Version();
    public static native String Link();

    public interface Callback {
        void onComplete(boolean success);
    }

    // 🔹 NEW: Progress listener for % updates
    public interface ProgressListener {
        void onProgress(int percent);
    }

    private final Context context;
    private final Callback callback; // may be null
    private ProgressListener progressListener; // 🔹 Added
    private androidx.appcompat.app.AlertDialog progressDialog;
    private String serverVersion = "0.0";
    private static final String PREF_NAME = "com.bgmi.download";
    private static final String PREF_VERSION_KEY = "version";
    private static final String HOSTOP_ZIP = "SANDIP.zip";
    private static final String LOADER_DIR_NAME = "loader";

    // Backwards-compatible constructor (no callback)
    public Downtwo(Context context) {
        this(context, null);
    }

    // New constructor with callback
    public Downtwo(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    // 🔹 Setter for progress listener
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // NOTE: We no longer show a built-in dialog, LogAct will show its own custom one
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
            if (params.length == 0 || params[0] == null) {
                return "Invalid URL provided";
            }

            String downloadUrl = params[0];
            serverVersion = getServerVersion();

            if (serverVersion == null) {
                return "Failed to retrieve server version";
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String localVersion = prefs.getString(PREF_VERSION_KEY, "0.0");

            if (!localVersion.equals(serverVersion)) {
                publishProgress("Downloading update...");
                return downloadAndExtract(downloadUrl);
            } else {
                return "No Update Available";
            }
        } catch (Exception e) {
            Log.e(TAG, "doInBackground error: ", e);
            return "Background error: " + e.getMessage();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        // This was for internal dialog → now handled in LogAct, so keep silent
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // Determine success: null == download applied; "No Update Available" treated as success too
        boolean success = (result == null) || "No Update Available".equals(result);

        // invoke callback if provided
        try {
            if (callback != null) {
                callback.onComplete(success);
            }
        } catch (Exception e) {
            Log.e(TAG, "Callback threw exception: ", e);
        }

        // Show final message only if activity is alive
        if (!success) {
            showFinalMessage("Error: " + result);
        }
    }

    private String getServerVersion() {
        try {
            URL url = new URL(Version());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);  // 8s connect timeout
            connection.setReadTimeout(8000);     // 8s read timeout
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            Scanner scanner = new Scanner(inputStream);
            String version = scanner.nextLine().trim();
            scanner.close();
            inputStream.close();
            connection.disconnect();
            return version;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching server version: ", e);
            return null;
        }
    }

    private String downloadAndExtract(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000); // 15s connect timeout
            connection.setReadTimeout(30000);    // 30s read timeout
            connection.connect();

            int totalSize = connection.getContentLength();
            int downloaded = 0;

            InputStream input = connection.getInputStream();
            File pathBase = new File(context.getFilesDir().getPath());

            if (!pathBase.exists()) {
                pathBase.mkdirs();
            }

            File pathOutput = new File(pathBase, HOSTOP_ZIP);
            FileOutputStream output = new FileOutputStream(pathOutput);
            byte[] data = new byte[65536]; // 64KB buffer (was 1KB — 64x faster I/O)
            int count;

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                downloaded += count;

                // Send progress % to listener
                if (totalSize > 0 && progressListener != null) {
                    int percent = (int) ((downloaded * 100L) / totalSize);
                    progressListener.onProgress(percent);
                }
            }

            output.close();
            input.close();

            File loaderDirectory = new File(pathBase, LOADER_DIR_NAME);
            if (!loaderDirectory.exists()) {
                loaderDirectory.mkdirs();
            }

            // extract with password
            new ZipFile(pathOutput, "ZENINPAPA".toCharArray()).extractAll(loaderDirectory.getAbsolutePath());

            setPermissions(loaderDirectory);

            if (pathOutput.exists()) {
                pathOutput.delete();
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_VERSION_KEY, serverVersion).apply();

            return null; // Success
        } catch (Exception e) {
            Log.e(TAG, "Error downloading or extracting file: ", e);
            return "Download failed: " + e.getMessage();
        }
    }

    private void setPermissions(File directory) {
        if (directory == null) return;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    setPermissions(file);
                }
            }
        } else {
            directory.setReadable(true, false);
            directory.setWritable(true, false);
            directory.setExecutable(true, false);
        }
    }

    private void showFinalMessage(String message) {
        // Only show if activity/context alive
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
