package com.bgmi.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bgmi.R;
import com.bgmi.sdk.BoxManager;
import com.bgmi.sdk.BoxService;
import com.bgmi.sdk.ObbSetup;
import com.bgmi.sdk.SoUpdater;
import com.bgmi.util.DeviceId;
import com.bgmi.util.Prefs;
import com.bgmi.util.Ui;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Two-state main screen:
 * SETUP (installs the game + copies OBB) then START GAME (downloads the
 * remote payload and launches the BGMI clone inside the virtual container).
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_STORAGE = 101;
    private static final int REQ_OVERLAY = 102;

    private enum Phase {SETUP, READY, BUSY}

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private Prefs prefs;
    private Phase phase = Phase.SETUP;
    private boolean pendingSetup;
    private boolean pendingStart;

    private Button btnAction;
    private ImageButton btnLogout;
    private ImageView ivReady;
    private ProgressBar progressBar;
    private TextView tvExpiry;
    private TextView tvExpiryHint;
    private TextView tvStatus;
    private TextView tvProgress;
    private TextView tvHint;
    private TextView tvDevice;

    private final Runnable countdown = new Runnable() {
        @Override
        public void run() {
            long left = prefs.expiryMillis() - System.currentTimeMillis();
            if (left <= 0L) {
                tvExpiry.setText("Expired");
                signOut(true);
                return;
            }
            tvExpiry.setText(formatRemaining(left));
            ui.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new Prefs(this);
        if (!prefs.isSessionValid()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        bindViews();

        tvDevice.setText(DeviceId.deviceName() + "  ·  Android " + Build.VERSION.RELEASE);
        tvExpiryHint.setText("expires " + prefs.expiry());

        btnAction.setOnClickListener(v -> onAction());
        btnLogout.setOnClickListener(v -> confirmSignOut());

        refreshState();
        ui.post(countdown);
    }

    private void bindViews() {
        btnAction = findViewById(R.id.btnAction);
        btnLogout = findViewById(R.id.btnLogout);
        ivReady = findViewById(R.id.ivReady);
        progressBar = findViewById(R.id.progressBar);
        tvExpiry = findViewById(R.id.tvExpiry);
        tvExpiryHint = findViewById(R.id.tvExpiryHint);
        tvStatus = findViewById(R.id.tvStatus);
        tvProgress = findViewById(R.id.tvProgress);
        tvHint = findViewById(R.id.tvHint);
        tvDevice = findViewById(R.id.tvDevice);
    }

    private void refreshState() {
        boolean ready = prefs.obbReady() && ObbSetup.isReady(this);
        applyPhase(ready ? Phase.READY : Phase.SETUP);
    }

    private void applyPhase(Phase next) {
        phase = next;
        if (next == Phase.BUSY) {
            btnAction.setEnabled(false);
            return;
        }

        btnAction.setEnabled(true);
        if (next == Phase.READY) {
            btnAction.setText(R.string.start_btn);
            tvHint.setText(R.string.hint_start);
            tvStatus.setText(R.string.status_setup_done);
            ivReady.setVisibility(ImageView.VISIBLE);
        } else {
            btnAction.setText(R.string.setup_btn);
            tvHint.setText(R.string.hint_setup);
            tvStatus.setText(R.string.status_not_setup);
            ivReady.setVisibility(ImageView.GONE);
        }
    }

    private void onAction() {
        if (phase == Phase.BUSY) return;
        if (phase == Phase.READY) {
            startGame();
        } else {
            beginSetup();
        }
    }

    // ---------------------------------------------------------------- setup

    private void beginSetup() {
        if (!hasStorage()) {
            pendingSetup = true;
            requestStorage();
            return;
        }
        if (!hasOverlay()) {
            pendingSetup = true;
            requestOverlay();
            return;
        }
        runSetup();
    }

    private void continuePending() {
        if (pendingSetup) {
            pendingSetup = false;
            if (!hasStorage()) {
                Ui.alert(this, "Permission required",
                        "All files access is needed to copy the game OBB files.");
                return;
            }
            if (!hasOverlay()) {
                Ui.alert(this, "Permission required",
                        "Display over other apps is needed for the in-game overlay.");
                return;
            }
            runSetup();
            return;
        }

        if (pendingStart) {
            pendingStart = false;
            if (!hasOverlay()) {
                Ui.alert(this, "Permission required",
                        "Display over other apps is needed for the in-game overlay.");
                return;
            }
            startGame();
        }
    }

    private void runSetup() {
        setBusy("Preparing…");
        io.execute(() -> {
            try {
                if (!BoxManager.isInstalled()) {
                    ui.post(() -> updateProgress(0, "Installing game…"));
                    if (!BoxManager.install()) {
                        fail("Setup failed",
                                "BGMI is not installed on this device. Install BGMI, open it once, then retry.");
                        return;
                    }
                }

                ObbSetup.Result result = ObbSetup.copy(MainActivity.this,
                        (percent, message) -> ui.post(() -> updateProgress(percent, message)));

                if (!result.ok) {
                    fail("Setup failed", result.message);
                    return;
                }

                prefs.setObbReady(true);
                ui.post(() -> {
                    hideProgress();
                    applyPhase(Phase.READY);
                    Ui.toast(MainActivity.this, "Setup complete");
                });
            } catch (Throwable t) {
                fail("Setup failed", safeMessage(t));
            }
        });
    }

    // ---------------------------------------------------------------- launch

    private void startGame() {
        if (!hasOverlay()) {
            pendingStart = true;
            requestOverlay();
            return;
        }

        setBusy("Preparing…");
        BoxService.start(this);

        io.execute(() -> {
            try {
                File payload = SoUpdater.ensure(MainActivity.this,
                        (percent, message) -> ui.post(() -> updateProgress(percent, message)));

                ui.post(() -> updateProgress(100, "Launching BGMI…"));

                boolean launched = BoxManager.launch(MainActivity.this, payload.getAbsolutePath());

                ui.post(() -> {
                    hideProgress();
                    applyPhase(Phase.READY);
                    if (launched) {
                        tvStatus.setText(R.string.status_running);
                        Ui.toast(MainActivity.this, "Starting BGMI…");
                    } else {
                        BoxService.stop(MainActivity.this);
                        Ui.alert(MainActivity.this, "Couldn't start game",
                                "Make sure BGMI is installed on this device, then try again.");
                    }
                });
            } catch (Throwable t) {
                ui.post(() -> {
                    hideProgress();
                    BoxService.stop(MainActivity.this);
                    applyPhase(Phase.READY);
                    Ui.alert(MainActivity.this, "Couldn't start game", safeMessage(t));
                });
            }
        });
    }

    // ------------------------------------------------------------- UI state

    private void setBusy(String label) {
        phase = Phase.BUSY;
        btnAction.setEnabled(false);
        btnAction.setText(label);
        tvStatus.setText(R.string.status_working);
        ivReady.setVisibility(ImageView.GONE);
        progressBar.setProgress(0);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        tvProgress.setText("Please wait…");
        tvProgress.setVisibility(TextView.VISIBLE);
    }

    private void updateProgress(int percent, String message) {
        progressBar.setProgress(Math.max(0, Math.min(100, percent)));
        if (message != null) tvProgress.setText(message);
    }

    private void hideProgress() {
        progressBar.setVisibility(ProgressBar.GONE);
        tvProgress.setVisibility(TextView.GONE);
    }

    private void fail(String title, String message) {
        ui.post(() -> {
            hideProgress();
            prefs.setObbReady(false);
            applyPhase(Phase.SETUP);
            Ui.alert(this, title, message);
        });
    }

    private static String safeMessage(Throwable t) {
        String message = t == null ? null : t.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Something went wrong. Please try again."
                : message;
    }

    // ----------------------------------------------------------- permissions

    private boolean hasStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_STORAGE);
            } catch (Throwable t) {
                try {
                    startActivityForResult(
                            new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                            REQ_STORAGE);
                } catch (Throwable t2) {
                    Ui.alert(this, "Permission required",
                            "Open Settings → Apps → Featurestic Loader → Permissions and allow all files access.");
                }
            }
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQ_STORAGE);
        }
    }

    private boolean hasOverlay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return Settings.canDrawOverlays(this);
    }

    private void requestOverlay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        try {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        } catch (Throwable t) {
            Ui.alert(this, "Permission required",
                    "Open Settings → Apps → Featurestic Loader → Display over other apps and enable it.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) continuePending();
    }

    @Override
    protected void onResume() {
        super.onResume();
        continuePending();
    }

    // --------------------------------------------------------------- session

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setMessage("You will need your license key again.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> signOut(false))
                .show();
    }

    private void signOut(boolean expired) {
        ui.removeCallbacks(countdown);
        prefs.clearSession();
        BoxService.stop(this);
        BoxManager.stop();
        goToLogin();
        if (expired) Ui.toast(this, "License expired");
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String formatRemaining(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0L) {
            return String.format(Locale.US, "%dd %02dh %02dm %02ds",
                    days, hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(countdown);
        io.shutdownNow();
        super.onDestroy();
    }
}
