package com.featurestic.loader.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.featurestic.loader.AppConfig;
import com.featurestic.loader.R;
import com.featurestic.loader.auth.PhpAuthManager;
import com.featurestic.loader.sdk.FeaturesticSdkHelper;

/**
 * Screen 1 — Apple Cupertino Glassmorphism Login Screen
 * 1. Requests required permissions on start (Overlay / Float over other apps, All Files Access for OBB, Notifications)
 * 2. Connects to Zoro PHP Panel (https://zoro.manishflash.online/connect) to verify license key
 * 3. Transitions smoothly to MainActivity
 */
public class LoginActivity extends Activity {

    private EditText etLicenseKey;
    private TextView btnPaste;
    private View btnSignIn;
    private TextView tvSignInLabel;
    private ProgressBar progressSignIn;
    private TextView tvStatusBanner;
    private TextView tvHwidBadge;
    private TextView btnSupport;

    private boolean isVerifying = false;
    private AlertDialog permissionDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdgeWindow();
        setContentView(R.layout.activity_login);

        View heroHeader = findViewById(R.id.heroHeader);
        View loginCard = findViewById(R.id.loginGlassCard);
        View footerRow = findViewById(R.id.footerContainer);

        etLicenseKey = findViewById(R.id.etLicenseKey);
        btnPaste = findViewById(R.id.btnPasteKey);
        btnSignIn = findViewById(R.id.btnSignInContainer);
        tvSignInLabel = findViewById(R.id.tvSignInLabel);
        progressSignIn = findViewById(R.id.progressSignIn);
        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvHwidBadge = findViewById(R.id.tvHwidBadge);
        btnSupport = findViewById(R.id.btnGetKeySupport);

        // Pre-fill saved license key if available
        String savedKey = PhpAuthManager.getSavedKey(this);
        if (!savedKey.isEmpty()) {
            etLicenseKey.setText(savedKey);
            etLicenseKey.setSelection(savedKey.length());
        }

        // Display Device HWID in subtle Apple monospace pill
        String hwid = PhpAuthManager.getDeviceHwid(this);
        tvHwidBadge.setText("HWID • " + hwid);

        // Attach Apple spring touch physics
        AppleAnimationHelper.attachSpringPress(btnPaste);
        AppleAnimationHelper.attachSpringPress(btnSignIn);
        AppleAnimationHelper.attachSpringPress(btnSupport);

        // Entrance animation
        AppleAnimationHelper.playEntranceAnimation(heroHeader, loginCard, footerRow);

        // Request standard runtime permissions (Notifications / Legacy Storage)
        FeaturesticSdkHelper.requestRuntimePermissionsOnStart(this);

        // Paste button action
        btnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip()) {
                    ClipData clip = cm.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0 && clip.getItemAt(0).getText() != null) {
                        String pasted = clip.getItemAt(0).getText().toString().trim();
                        etLicenseKey.setText(pasted);
                        etLicenseKey.setSelection(pasted.length());
                        showBanner("Key pasted from clipboard", false);
                        return;
                    }
                }
                showBanner("Clipboard is empty", true);
            }
        });

        // Sign In button action
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isVerifying) return;
                if (!checkAndGuidePermissions(true)) {
                    return;
                }
                performLogin();
            }
        });

        // Support / Get Key action
        btnSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.SUPPORT_URL));
                    startActivity(browser);
                } catch (Throwable ignored) {
                    Toast.makeText(LoginActivity.this, "Support link not configured", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndGuidePermissions(false);
    }

    /**
     * Guides the user to grant:
     *   1. Display Over Other Apps (Floating Overlay permission for ESP / plugin .so)
     *   2. All Files Access (MANAGE_EXTERNAL_STORAGE for /SdCard/0/Android/obb)
     */
    private boolean checkAndGuidePermissions(boolean triggeredByLoginClick) {
        final boolean hasOverlay = FeaturesticSdkHelper.hasOverlayPermission(this);
        final boolean hasStorage = FeaturesticSdkHelper.hasAllFilesPermission(this);

        if (hasOverlay && hasStorage) {
            if (permissionDialog != null && permissionDialog.isShowing()) {
                try {
                    permissionDialog.dismiss();
                } catch (Throwable ignored) {}
            }
            return true;
        }

        if (!hasOverlay) {
            showBanner("Tap here to allow 'Display Over Other Apps' (Required for Overlay)", false);
            tvStatusBanner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FeaturesticSdkHelper.requestOverlayPermission(LoginActivity.this);
                }
            });
        } else {
            showBanner("Tap here to allow 'All Files Access' (Required for OBB & Virtual Box)", false);
            tvStatusBanner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FeaturesticSdkHelper.requestAllFilesPermission(LoginActivity.this);
                }
            });
        }

        if (permissionDialog != null && permissionDialog.isShowing()) {
            return false;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (!hasOverlay) {
                builder.setTitle("Allow Floating Overlay Permission");
                builder.setMessage("Please enable 'Display over other apps' (Float on top) so the Loader and Virtual Engine overlay can run smoothly.");
                builder.setCancelable(false);
                builder.setPositiveButton("Grant Overlay Permission", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FeaturesticSdkHelper.requestOverlayPermission(LoginActivity.this);
                    }
                });
                builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            } else {
                builder.setTitle("Allow Storage / OBB Access");
                builder.setMessage("Please enable 'All Files Access' so the Loader can copy and mount game OBB files in /SdCard/0/Android/obb.");
                builder.setCancelable(false);
                builder.setPositiveButton("Grant Storage Access", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FeaturesticSdkHelper.requestAllFilesPermission(LoginActivity.this);
                    }
                });
                builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            }
            permissionDialog = builder.create();
            permissionDialog.show();
        } catch (Throwable ignored) {
            if (triggeredByLoginClick) {
                if (!hasOverlay) {
                    FeaturesticSdkHelper.requestOverlayPermission(this);
                } else {
                    FeaturesticSdkHelper.requestAllFilesPermission(this);
                }
            }
        }
        return false;
    }

    private void performLogin() {
        String key = etLicenseKey.getText().toString().trim();
        if (key.isEmpty()) {
            showBanner("Enter your license key to continue", true);
            AppleAnimationHelper.playMorphPop(etLicenseKey);
            return;
        }

        setLoadingState(true);
        showBanner("Authenticating with Zoro Panel...", false);

        PhpAuthManager.verifyKey(this, key, new PhpAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String licenseKey, String expiryText, String serverMessage) {
                setLoadingState(false);
                showBanner(serverMessage, false);

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("license_key", licenseKey);
                intent.putExtra("expiry_text", expiryText);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoadingState(false);
                showBanner(errorMessage, true);
                AppleAnimationHelper.playMorphPop(btnSignIn);
            }
        });
    }

    private void setLoadingState(boolean loading) {
        isVerifying = loading;
        etLicenseKey.setEnabled(!loading);
        btnPaste.setEnabled(!loading);
        if (loading) {
            tvSignInLabel.setText("Verifying Key...");
            progressSignIn.setVisibility(View.VISIBLE);
            btnSignIn.setAlpha(0.85f);
        } else {
            tvSignInLabel.setText("Continue to Virtual Box");
            progressSignIn.setVisibility(View.GONE);
            btnSignIn.setAlpha(1.0f);
        }
    }

    private void showBanner(String message, boolean isError) {
        tvStatusBanner.setVisibility(View.VISIBLE);
        tvStatusBanner.setText(message);
        if (isError) {
            tvStatusBanner.setTextColor(Color.parseColor("#FF453A"));
            tvStatusBanner.setBackgroundResource(R.drawable.bg_apple_pill_error);
        } else {
            tvStatusBanner.setTextColor(Color.parseColor("#64D2FF"));
            tvStatusBanner.setBackgroundResource(R.drawable.bg_apple_pill_info);
        }
    }

    private void setupEdgeToEdgeWindow() {
        try {
            Window window = getWindow();
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.parseColor("#07090E"));
                window.setNavigationBarColor(Color.parseColor("#07090E"));
            }
        } catch (Throwable ignored) {
        }
    }
}
