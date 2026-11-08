package com.bgmi;

import android.app.Application;
import android.util.Log;

import com.bgmi.sdk.BoxManager;

/**
 * Featurestic Loader application entry point.
 *
 * <p>The SDK ships its own auto-init provider, so all we do here is make sure
 * the virtual container core is ready before any activity starts.</p>
 */
public class App extends Application {

    private static final String TAG = "FeaturesticApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            BoxManager.init(this);
            BoxManager.activate();
        } catch (Throwable t) {
            Log.e(TAG, "SDK init failed", t);
        }
    }
}
