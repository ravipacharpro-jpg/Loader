package com.bgmi.floating;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bgmi.R;
import com.bgmi.utils.FLog;

public class ToggleAim extends Service {
    
    public native void ToggleAim(boolean value);

    static {
            System.loadLibrary("zenin");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        ToggleAim(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToggleAim(false);
    }
}
