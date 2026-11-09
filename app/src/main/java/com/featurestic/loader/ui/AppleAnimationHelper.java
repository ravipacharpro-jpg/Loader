package com.featurestic.loader.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Provides Apple iOS / Cupertino style spring physics, tactile press scaling,
 * and smooth staggered entrance animations.
 */
public final class AppleAnimationHelper {

    private AppleAnimationHelper() {}

    /**
     * Attaches iOS-style subtle press-down scale (0.96x) and spring release (1.0x)
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void attachSpringPress(final View view) {
        if (view == null) return;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(110)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(240)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                }
                return false;
            }
        });
    }

    /**
     * Plays staggered Apple card entrance animation (translateY + alpha + subtle scale)
     */
    public static void playEntranceAnimation(View... views) {
        if (views == null) return;
        for (int i = 0; i < views.length; i++) {
            final View v = views[i];
            if (v == null) continue;
            v.setAlpha(0f);
            v.setTranslationY(42f);
            v.setScaleX(0.97f);
            v.setScaleY(0.97f);
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(i * 75L)
                    .setDuration(460)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .start();
        }
    }

    /**
     * Plays a celebratory Apple spring bounce when a state transitions (e.g. Setup -> Start Game)
     */
    public static void playMorphPop(View view) {
        if (view == null) return;
        try {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        } catch (Throwable ignored) {}
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, "scaleX", 0.92f, 1.04f, 1.0f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, "scaleY", 0.92f, 1.04f, 1.0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.setDuration(380);
        set.setInterpolator(new OvershootInterpolator(2.2f));
        set.start();
    }
}
