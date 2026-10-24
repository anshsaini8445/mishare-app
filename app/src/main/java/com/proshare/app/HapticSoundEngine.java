package com.proshare.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticSoundEngine {

    public static void tick(View v) {
        try {
            if (v != null) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception ignored) {}
    }

    public static void popSuccess(Context ctx) {
        try {
            Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(45);
                }
            }
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55);
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 110);
        } catch (Exception ignored) {}
    }
}
