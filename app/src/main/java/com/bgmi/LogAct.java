package com.bgmi;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.widget.Switch;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.security.MessageDigest;

import com.bgmi.utils.Downtwo;
import com.bgmi.utils.Prefs;

import org.json.JSONObject;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class LogAct extends AppCompatActivity {

    /** License panel endpoint. */
    private static final String AUTH_URL = "https://zoro.manishflash.online/connect";
    /** Shared secret used by the panel to sign the returned token. */
    private static final String AUTH_SALT = "Vm8Lk7Uj2JmsjCPVPVjrLa7zgfx3uz9E";

    /** License expiry (yyyy-MM-dd HH:mm:ss) received from the panel. */
    public static String EXP = "NULL";

    private Prefs prefs;
    private final String USER = "USER";
public static boolean Kooontoool = true;
    private EditText textUsername;
    private TextView btnLogin;
    private ImageView pasteBtn;
    private TextView getKey;
    private File loaderKeyFile;
    private Dialog loadingDialog;
    public static boolean hidesp = false;
    public static String socket;
    public static String daemonPath;

    
    
    // --- Permissions
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200;
    private static final String PREFS_NAME = "com.bgmi.prefs";
    private static final String PREF_PERMISSIONS_GRANTED = "permissions_granted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // VPN check before showing UI
        if (iskokofm()) {
            new AlertDialog.Builder(LogAct.this)
                    .setTitle("VPN Detected")
                    .setMessage("Please disable VPN to continue")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (!ischicken()) {
            Toast.makeText(this, "LUND LELE BHAI", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_login);
        
        

        prefs = new Prefs(this);

        // 🔑 Check permissions
        checkAndRequestPermissions();

        textUsername = findViewById(R.id.userkey);
        btnLogin = findViewById(R.id.login);
        pasteBtn = findViewById(R.id.paste);
        getKey = findViewById(R.id.GetKey);
       Switch hideesp = findViewById(R.id.hiderecording);

        hideesp.setOnCheckedChangeListener((compoundButton, b) -> hidesp = b);
        // Restore saved key
        textUsername.setText(prefs.getSt(USER, ""));

        // Get Key link
        getKey.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(GetKey()));
            startActivity(intent);
        });

        // Login button - NOW COPIES KEY TO CLIPBOARD EVERY TIME
        btnLogin.setOnClickListener(v -> {
            // First copy key to clipboard
            copyKeyToClipboard();
            
            String userKey = textUsername.getText().toString().trim();
            if (!userKey.isEmpty()) {
                if (iskokofm()) {
                    new AlertDialog.Builder(LogAct.this)
                            .setTitle("VPN Detected")
                            .setMessage("Please disable VPN to continue")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                prefs.setSt(USER, userKey);
                Login(this, userKey);
            } else {
                textUsername.setError("Please enter key");
            }
        });

        // Paste button
        pasteBtn.setOnClickListener(v -> {
            pasteFromClipboard();
        });
    }

    /**
     * Copy key to clipboard when Login button is pressed
     */
    /**
 * Copy key from input field to clipboard when Login button is pressed
 */
private void copyKeyToClipboard() {
    try {
        // Get the key from the input field, not from GetKey()
        String keyToCopy = textUsername.getText().toString().trim();
        
        if (keyToCopy.isEmpty()) {
            Toast.makeText(this, "No key to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("App Key", keyToCopy);
            clipboard.setPrimaryClip(clip);
            
            // Show success message
            Toast.makeText(this, "Key copied to clipboard!", Toast.LENGTH_SHORT).show();
            
            Log.d("LogAct", "Key copied to clipboard: " + keyToCopy);
        } else {
            Toast.makeText(this, "Failed to access clipboard", Toast.LENGTH_SHORT).show();
        }
    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error copying key", Toast.LENGTH_SHORT).show();
    }
}

   

    /**
     * Enhanced paste functionality
     */
    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence clipText = clip.getItemAt(0).coerceToText(this);
                String pasted = clipText != null ? clipText.toString() : "";
                if (pasted.length() > 5) {
                    textUsername.setText(pasted);
                    Toast.makeText(this, "Text pasted from clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid key in clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Clipboard empty", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean ischicken() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Signature signature : signatures) {
                byte[] digest = md.digest(signature.toByteArray());
                StringBuilder hash = new StringBuilder();
                for (byte b : digest) {
                    hash.append(String.format("%02X", b));
                }
                if (hash.toString().equals("E545B00E31AF8FC008566DE0B87C0B17FC467A90AEED71B00755B98698AE58BC")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 🔧 Permissions handling
    private void checkAndRequestPermissions() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean permissionsGranted = prefs.getBoolean(PREF_PERMISSIONS_GRANTED, false);

        if (!isStoragePermissionGranted()) {
            requestStoragePermissionDirect();
        } else if (!canRequestPackageInstalls()) {
            requestUnknownAppPermissionsDirect();
        } else if (!canDrawOverlays()) {
            requestOverlayPermission();
        } else {
            prefs.edit().putBoolean(PREF_PERMISSIONS_GRANTED, true).apply();
        }
    }

    private boolean isStoragePermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private void requestStoragePermissionDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_MANAGE_STORAGE_PERMISSION);
        }
    }

    private boolean canRequestPackageInstalls() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls();
    }

    private void requestUnknownAppPermissionsDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES);
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 300);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            // Re-check permissions no matter what
            checkAndRequestPermissions();
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            checkAndRequestPermissions();
        } else if (requestCode == 300) { // Overlay Permission
            checkAndRequestPermissions();
        }
    }

    private boolean iskokofm() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true;
                    }
                }
            } else {
                // Fallback for older devices: check network interface names for common VPN interfaces
                try {
                    java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                    while (interfaces != null && interfaces.hasMoreElements()) {
                        java.net.NetworkInterface intf = interfaces.nextElement();
                        String name = intf.getName();
                        if (name != null) {
                            if (name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("tap")) {
                                return true;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return false;
    }

    // 🔑 Login
    private void Login(final Context m_Context, final String userKey) {
        showLoadingDialog("Checking key...", false);

        Handler loginHandler = new Handler(msg -> {
            dismissLoadingDialog();
            if (msg.what == 0) {
                startDownload(m_Context);
            } else if (msg.what == 1) {
                showLoadingDialog((String) msg.obj, true);
            }
            return true;
        });

        new Thread(() -> {
            String result = Check(m_Context, userKey); // native
            if ("OK".equals(result)) {
                loginHandler.sendEmptyMessage(0);
            } else {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = result;
                loginHandler.sendMessage(msg);
            }
        }).start();
    }

    private void startDownload(Context m_Context) {
        showLoadingDialog("Downloading files...", false);

        Downtwo task = new Downtwo(LogAct.this, success -> {
            dismissLoadingDialog();
            if (success) {
                
                Intent i = new Intent(m_Context.getApplicationContext(), MAct.class);
                m_Context.startActivity(i);
            } else {
                Toast.makeText(LogAct.this, "Download failed!", Toast.LENGTH_SHORT).show();
            }
        });

        task.setProgressListener(progress -> runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                ProgressBar progressBar = loadingDialog.findViewById(R.id.progressBar);
                TextView progressText = loadingDialog.findViewById(R.id.progressText);

                progressBar.setIndeterminate(false);
                progressBar.setMax(100);
                progressBar.setProgress(progress);

                progressText.setText("Downloading... " + progress + "%");
            }
        }));

        task.execute(Downtwo.Link());
    }

    /**
     * Keep an AlertDialog-style helper (legacy) for places that might use it.
     * Uses ios_loading to avoid missing layout resource errors.
     */
    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Use ios_loading (existing layout) to avoid compilation error when dialog_loading is absent
        builder.setView(R.layout.ios_loading);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    // 🔧 Dialog (main)
    private void showLoadingDialog(String message, boolean isError) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.ios_loading);
            loadingDialog.setCancelable(false);
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
        ProgressBar progressBar = loadingDialog.findViewById(R.id.progressBar);
        Button okButton = loadingDialog.findViewById(R.id.okButton);

        if (isError) {
            progressBar.setVisibility(View.GONE);
            okButton.setVisibility(View.VISIBLE);
            loadingText.setText("Login failed: " + message);
            okButton.setOnClickListener(v -> dismissLoadingDialog());
        } else {
            progressBar.setVisibility(View.VISIBLE);
            okButton.setVisibility(View.GONE);
            loadingText.setText(message != null ? message : "Loading...");
        }

        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // --- License check (pure Java replacement for the old libzenin.so JNI) ---
    private static String Check(Context mContext, String userKey) {
        HttpURLConnection conn = null;
        try {
            String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            String raw = userKey + (androidId == null ? "" : androidId) + Build.MODEL + Build.BRAND;
            String serial = UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();

            String body = "game=PUBG"
                    + "&user_key=" + URLEncoder.encode(userKey, "UTF-8")
                    + "&serial=" + URLEncoder.encode(serial, "UTF-8");

            conn = (HttpURLConnection) new URL(AUTH_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            String response = readAll(is);
            Log.d("LogAct", "auth response (" + code + "): " + response);

            JSONObject root = new JSONObject(response);
            if (!root.optBoolean("status", false)) {
                return root.optString("reason", "Invalid key");
            }

            JSONObject data = root.getJSONObject("data");
            String exp = data.optString("EXP", "NULL");
            String token = data.optString("token", "");
            long rng = data.optLong("rng", 0L);

            if (rng + 30L <= System.currentTimeMillis() / 1000L) {
                return "Server time expired, retry";
            }

            String auth = md5("PUBG-" + userKey + "-" + serial + "-" + AUTH_SALT);
            if (!auth.equalsIgnoreCase(token)) {
                return "Token mismatch";
            }

            EXP = exp;
            mContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit().putString("EXP", exp).apply();
            return "OK";
        } catch (Exception e) {
            Log.e("LogAct", "login error", e);
            return e.getMessage() == null ? "Network error" : e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private static String md5(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format("%02x", b));
        return out.toString();
    }

    private String GetKey() {
        return "https://t.me/";
    }
}