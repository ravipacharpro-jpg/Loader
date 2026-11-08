package com.bgmi.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bgmi.R;
import com.bgmi.net.AuthApi;
import com.bgmi.util.Prefs;
import com.bgmi.util.Ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** License key login. */
public class LoginActivity extends AppCompatActivity {

    private static final String GET_KEY_URL = "https://t.me/";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private Prefs prefs;
    private EditText etKey;
    private Button btnLogin;
    private boolean busy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new Prefs(this);
        if (prefs.isSessionValid()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        etKey = findViewById(R.id.etKey);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvGetKey = findViewById(R.id.tvGetKey);

        etKey.setText(prefs.key());
        if (!TextUtils.isEmpty(prefs.key())) {
            etKey.setSelection(etKey.getText().length());
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        etKey.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        tvGetKey.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GET_KEY_URL)));
            } catch (Throwable t) {
                Ui.toast(this, "No browser available");
            }
        });
    }

    private void attemptLogin() {
        if (busy) return;
        final String key = etKey.getText().toString().trim();
        if (key.isEmpty()) {
            etKey.setError("Enter your license key");
            etKey.requestFocus();
            return;
        }

        hideKeyboard();
        busy = true;
        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.checking);

        io.execute(() -> {
            AuthApi.Result result = AuthApi.login(LoginActivity.this, key);
            ui.post(() -> {
                busy = false;
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.continue_btn);
                if (result.ok) {
                    prefs.setKey(key);
                    prefs.setExpiry(result.expiry);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Ui.alert(LoginActivity.this, "Login failed", result.message);
                }
            });
        });
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
