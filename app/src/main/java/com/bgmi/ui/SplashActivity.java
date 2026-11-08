package com.bgmi.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bgmi.BuildConfig;
import com.bgmi.R;
import com.bgmi.util.Prefs;

import android.widget.TextView;

/** Branded launch screen; routes to Login or Main. */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_MS = 1100L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable route = new Runnable() {
        @Override
        public void run() {
            Prefs prefs = new Prefs(SplashActivity.this);
            Class<?> next = prefs.isSessionValid() ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, next));
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView version = findViewById(R.id.tvVersion);
        version.setText("v" + BuildConfig.VERSION_NAME);

        handler.postDelayed(route, SPLASH_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(route);
        super.onDestroy();
    }
}
