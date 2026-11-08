package com.bgmi.util;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/** Tiny UI helpers. */
public final class Ui {

    private Ui() {
    }

    public static void toast(Context context, String message) {
        if (context == null || message == null) return;
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void alert(Context context, String title, String message) {
        if (!(context instanceof Activity)) {
            toast(context, message);
            return;
        }
        Activity activity = (Activity) context;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }
}
