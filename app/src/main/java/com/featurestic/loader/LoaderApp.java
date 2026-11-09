package com.featurestic.loader;

import android.app.Application;
import android.content.Context;

import com.featurestic.loader.sdk.FeaturesticSdkHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Clean 2-Phase Application Integration with FeaturesticBox-release.aar:
 *   1. attachBaseContext(base) -> unseals Hidden APIs & attaches base context
 *   2. onCreate() -> initializes virtual services after ContentProviders are ready
 */
public class LoaderApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        installCrashRecorder(base);
        FeaturesticSdkHelper.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FeaturesticSdkHelper.initEngine(this);
    }

    private void installCrashRecorder(final Context context) {
        try {
            final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    try {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        File f = new File(context.getFilesDir(), "last_crash.txt");
                        try (FileOutputStream fos = new FileOutputStream(f)) {
                            fos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Throwable ignored) {
                    }
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(t, e);
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }
}
