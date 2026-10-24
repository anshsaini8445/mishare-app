package com.proshare.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
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
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50);
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 100);
        } catch (Exception ignored) {}
    }
}
