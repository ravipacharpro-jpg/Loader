package com.bgmi;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import static com.bgmi.server.ApiServer.mainURL;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.bgmi.server.DownloadZip;
import com.bgmi.utils.AppManager;
import com.bgmi.utils.Downtwo;
import com.bgmi.utils.Prefs;
import com.bgmi.floating.FloatService;
import com.bgmi.floating.Overlay;
import com.bgmi.floating.ToggleAim;
import com.bgmi.floating.ToggleBullet;
import com.bgmi.floating.ToggleSimulation;
import com.topjohnwu.superuser.Shell;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.lang.reflect.Method;
import com.sbox.SboxCore;
import com.sbox.entity.pm.InstallResult;
import com.sbox.core.env.BEnvironment;
import com.featurestic.sdk.FeaturesticBox;

@Obfuscate
public class MAct extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("zenin");
        } catch (UnsatisfiedLinkError ignored) {}
    }

    private static final String PKG_BGMI = "com.pubg.imobile";
    private static final int USER_ID = 0;
    public static String modeselect;
    private ProgressBar progressBar;
    private TextView progressText;
    private boolean isObbCopied = false;
    private AppManager appManager;
    private Prefs prefs;
    public static boolean Ischeck = false;
    public static int gameint = 1;
    public static int bitversi = 64;
    public static boolean modestatus = false;

    private TextView startBgmi;
    private LinearLayout progressCard;
    public static String game = "com.pubg.imobile";
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    public static String socket;
    public static String daemonPath;
    
    // Countdown text views
    private TextView tvD, tvH, tvM, tvS;
    
    // Add this static instance variable
    private static MAct instance;
    
    // Add this get() method
    public static Context get() {
        return instance != null ? instance : null;
    }
    
    // Native methods
    public static native String pp();
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the static instance
        instance = this;
        
        prefs = new Prefs(this);
        Loadssets();

        if (!ischicken()) {
            Toast.makeText(this, "LUND SHAYK KA LELE", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Validate expiry date before creating UI
        if (!isExpiryDateValid()) {
            showToast("Invalid or expired license!");
            prefs.setSt("USER", "");
            startActivity(new Intent(this, LogAct.class));
            finish();
            return;
        }
        
        // Use XML layout
        setContentView(R.layout.activity_main);
        
        appManager = new AppManager(this);
        
        // Bind views
        startBgmi = findViewById(R.id.startBgmi);
        progressCard = findViewById(R.id.progressCard);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        tvD = findViewById(R.id.tvD);
        tvH = findViewById(R.id.tvH);
        tvM = findViewById(R.id.tvM);
        tvS = findViewById(R.id.tvS);

        TextView btnSignOut = findViewById(R.id.btnSignOut);
        TextView btnClearLogin = findViewById(R.id.btnClearLogin);
        Switch hideEspSwitch = findViewById(R.id.hiderecording);
        TextView tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        TextView tvLoaderInfo = findViewById(R.id.tvLoaderInfo);

        // Populate device info from real data
        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;
        tvDeviceInfo.setText(deviceInfo);

        // Loader info: OS version + login type
        String loaderInfo = "Android " + Build.VERSION.RELEASE + " Supported, FB / Twitter Login Direct";
        tvLoaderInfo.setText(loaderInfo);

        // Hide ESP toggle (restore state)
        hideEspSwitch.setChecked(LogAct.hidesp);
        hideEspSwitch.setOnCheckedChangeListener((compoundButton, b) -> LogAct.hidesp = b);

        // Start Game button — MANUAL only, no auto-start
        startBgmi.setOnClickListener(v -> {
            progressCard.setVisibility(View.VISIBLE);
            progressText.setText("Checking game...");
            startGameSequence();
        });

        // Sign Out
        btnSignOut.setOnClickListener(v -> {
            prefs.setSt("USER", "");
            LogAct.hidesp = false;
            startActivity(new Intent(MAct.this, LogAct.class));
            finish();
        });

        // Clear Login
        btnClearLogin.setOnClickListener(v -> {
            prefs.setSt("USER", "");
            showToast("Login data cleared");
            startActivity(new Intent(MAct.this, LogAct.class));
            finish();
        });

        // Start countdown timer from real expiry data
        startCountdownTimer();
    }

    /** Parse pp() expiry and tick countdown every second */
    private void startCountdownTimer() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String expiryStr = pp();
                    if (expiryStr == null || expiryStr.isEmpty()) {
                        setCountdown(0, 0, 0, 0);
                        return;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    Date expiry = sdf.parse(expiryStr);
                    long diff = expiry.getTime() - System.currentTimeMillis();
                    if (diff <= 0) {
                        setCountdown(0, 0, 0, 0);
                        showToast("License expired!");
                        prefs.setSt("USER", "");
                        startActivity(new Intent(MAct.this, LogAct.class));
                        finish();
                        return;
                    }
                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                    long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                    setCountdown(days, hours, minutes, seconds);
                } catch (Exception e) {
                    setCountdown(0, 0, 0, 0);
                }
                countdownHandler.postDelayed(this, 1000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void setCountdown(long d, long h, long m, long s) {
        if (tvD != null) tvD.setText(String.valueOf(d));
        if (tvH != null) tvH.setText(String.format(Locale.US, "%02d", h));
        if (tvM != null) tvM.setText(String.format(Locale.US, "%02d", m));
        if (tvS != null) tvS.setText(String.format(Locale.US, "%02d", s));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        stopService(new Intent(MAct.this, FloatService.class));
        stopService(new Intent(MAct.this, Overlay.class));
        stopService(new Intent(MAct.this, ToggleBullet.class));
        stopService(new Intent(MAct.this, ToggleAim.class));
        stopService(new Intent(MAct.this, ToggleSimulation.class));
        finishAffinity();
        
        instance = null;
    }
    
    /** Check if expiry date is valid and not expired */
    private boolean isExpiryDateValid() {
        try {
            String expiryDateStr = pp();
            if (expiryDateStr == null || expiryDateStr.isEmpty() || !expiryDateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return false;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date expiryDate = dateFormat.parse(expiryDateStr);
            long now = System.currentTimeMillis();
            return expiryDate.getTime() > now;
        } catch (Exception e) {
            return false;
        }
    }

    /** Start game sequence triggered manually by START GAME button */
    private void startGameSequence() {
        progressText.setText("Launching Game...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        // FeaturesticBox SDK: Auto-Init + Auto-Install + Zero-Copy OBB + Splash + Launch
        boolean ok = FeaturesticBox.startGame(this, PKG_BGMI);
        if (ok) {
            startPatcher();
            showToast("Launching BGMI...");
        } else {
            progressBar.setVisibility(View.GONE);
            progressText.setText("Launch failed! Check game install.");
        }
    }

    private void checkAndCopyObb() {
        if (!isObbCopied) {
            copyObbFiles();
        } else {
            launchGameNow();
        }
    }

    private void launchGameNow() {
        progressText.setText("Launching Game...");
        progressBar.setIndeterminate(true);
        new Handler(Looper.getMainLooper()).postDelayed(this::launchGame, 500);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /** Copy OBB with progress */
    private void copyObbFiles() {
        progressText.setText("Checking OBB Files...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";

                File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
                if (!obbDestDir.exists()) obbDestDir.mkdirs();

                File obbSource = null;
                StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);

                for (StorageVolume storageVolume : sm.getStorageVolumes()) {
                    File obbDir;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        obbDir = storageVolume.getDirectory();
                    } else {
                        try {
                            Method getPathFile = StorageVolume.class.getMethod("getPathFile");
                            obbDir = (File) getPathFile.invoke(storageVolume);
                        } catch (Exception e) {
                            obbDir = null;
                        }
                    }

                    if (obbDir != null) {
                        File obbSubDir = new File(obbDir, "Android/obb/" + PKG_BGMI);
                        File obbFile = new File(obbSubDir, gameObb);
                        if (obbFile.exists() && obbFile.canRead()) {
                            obbSource = obbFile;
                            break;
                        }
                    }
                }

                File obbDestFile = new File(obbDestDir, gameObb);
                if (obbDestFile.exists()) {
                    if (obbSource != null && obbDestFile.length() == obbSource.length()) {
                        runOnUiThread(() -> {
                            isObbCopied = true;
                            progressText.setText("OBB Verified.");
                            launchGameNow();
                        });
                        return;
                    } else if (obbSource == null) {
                        runOnUiThread(() -> {
                            isObbCopied = true;
                            progressText.setText("OBB Found.");
                            launchGameNow();
                        });
                        return;
                    }
                }

                if (obbSource == null || !obbSource.exists() || !obbSource.canRead()) {
                    runOnUiThread(() -> progressText.setText("Error: OBB not found in any storage!"));
                    return;
                }

                progressText.setText("Copying OBB Files...");

                FileInputStream fis = new FileInputStream(obbSource);
                FileOutputStream fos = new FileOutputStream(obbDestFile);

                long total = obbSource.length();
                long copied = 0;
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    copied += len;
                    int prog = (int) ((copied * 100) / total);
                    runOnUiThread(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(prog);
                        progressText.setText("Copying OBB: " + prog + "%");
                    });
                }
                fis.close();
                fos.close();

                runOnUiThread(() -> {
                    isObbCopied = true;
                    progressText.setText("Resources Ready.");
                    launchGameNow();
                });

            } catch (Exception e) {
                runOnUiThread(() -> progressText.setText("OBB Error: " + e.getMessage()));
            }
        }).start();
    }

    public void Checking(){
        new DownloadZip(this).execute("1", mainURL());
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

    void RunShell(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void runant(final String nf){
        excpp("/"+nf);
    }

    private void ExecuteElf(String shell) {
        try {
            Runtime.getRuntime().exec(shell);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void excpp(String path) {
        try {
            ExecuteElf("chmod 777 " + getFilesDir() + path);
            ExecuteElf(getFilesDir() + path);
            ExecuteElf("su -c chmod 777 " + getFilesDir() + path);
            ExecuteElf("su -c " + getFilesDir() + path);
        } catch (Exception e) {
            // Handle exception
        }
    }

    private void Loadssets() {
        MoveAssets(getFilesDir() + "/", "sock64");
        MoveAssets(getFilesDir() + "/", "socu64");
        MoveAssets(getFilesDir() + "/", "socs32");
        MoveAssets(getFilesDir() + "/", "socu32");
        MoveAssets(getFilesDir() + "/", "TW");
        MoveAssets(getFilesDir() + "/", "VNG");
        MoveAssets(getFilesDir() + "/", "HakmeDev");
        MoveAssets(getFilesDir() + "/", "kernels64");
    }

    private boolean MoveAssets(String outPath, String fileName) {
        File file = new File(outPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("--Method--", "copyAssetsSingleFile: cannot create directory.");
                return false;
            }
        }
        try {
            InputStream inputStream = getAssets().open(fileName);
            File outFile = new File(file, fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = inputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FloatService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startPatcher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 123);
            } else {
                startFloater();
            }
        }
    }

    private void startFloater() {
        startService(new Intent(MAct.this, FloatService.class));
        loadAssets();
    } 
        
    private void stopPatcher() {
        stopService(new Intent(MAct.this, FloatService.class));
        stopService(new Intent(MAct.this, Overlay.class));
        stopService(new Intent(MAct.this, ToggleAim.class));
        stopService(new Intent(MAct.this, ToggleBullet.class));
        stopService(new Intent(MAct.this, ToggleSimulation.class));
    }

    public void loadAssets() {
        String filepath = Environment.getExternalStorageDirectory() + "/Android/data/.tyb";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filepath);
            byte[] buffer = "DO NOT DELETE".getBytes();
            fos.write(buffer, 0, buffer.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        daemonPath = getFilesDir().toString() + "/sock64";
        if (Shell.rootAccess()) {
            socket = "su -c " + daemonPath;
        } else {
            socket = daemonPath;
        }
        try {
            Runtime.getRuntime().exec("chmod 777 " + daemonPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Launch BGMI */
    private void launchGame() {
        try {
            SboxCore.get().launchApk(PKG_BGMI, USER_ID);
            startPatcher();
            showToast("Launching BGMI...");
        } catch (Exception e) {
            showToast("Launch failed: " + e.getMessage());
        }
    }

    // Kept for backward compatibility / possible native use
    private GradientDrawable createRoundRectDrawable(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(radius);
        shape.setColor(color);
        return shape;
    }
    
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp, 
            getResources().getDisplayMetrics()
        );
    }
}