package com.flolov42.lea_v3.accessibility;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.view.View;
import java.util.Locale;

public class LeaAccessibilityManager {

    private static LeaAccessibilityManager instance;
    private final Context ctx;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    public static synchronized LeaAccessibilityManager get(Context ctx) {
        if (instance == null) instance = new LeaAccessibilityManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaAccessibilityManager(Context ctx) {
        this.ctx = ctx;
        initTTS();
    }

    private void initTTS() {
        tts = new TextToSpeech(ctx, status -> {
            ttsReady = (status == TextToSpeech.SUCCESS);
            if (ttsReady) {
                tts.setLanguage(Locale.FRANCE);
                tts.setSpeechRate(0.9f);
            }
        });
    }

    public boolean isTTSEnabled() {
        return LeaFeaturesDatabase.get(ctx).getA11ySettings().ttsEnabled == 1;
    }

    public void speak(String text) {
        if (isTTSEnabled() && ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    public void speakOnFocus(View v, String text) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) speak(text);
        });
    }

    public void haptic() {
        LeaFeaturesDatabase.A11ySettings s = LeaFeaturesDatabase.get(ctx).getA11ySettings();
        if (s.hapticFeedback != 1) return;
        Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vib == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vib.vibrate(50);
        }
    }

    public float getFontScale() {
        String size = LeaFeaturesDatabase.get(ctx).getA11ySettings().fontSize;
        if (size == null) return 1.0f;
        switch (size) {
            case "small":       return 0.85f;
            case "large":       return 1.2f;
            case "extra_large": return 1.5f;
            default:            return 1.0f;
        }
    }

    public boolean isHighContrast() {
        return LeaFeaturesDatabase.get(ctx).getA11ySettings().highContrast == 1;
    }

    public String getColorBlindMode() {
        String mode = LeaFeaturesDatabase.get(ctx).getA11ySettings().colorBlindMode;
        return mode != null ? mode : "none";
    }

    public void saveSettings(int tts, String fontSize, int highContrast, String colorBlind, int haptic) {
        LeaFeaturesDatabase.get(ctx).saveA11ySettings(tts, fontSize, highContrast, colorBlind, haptic);
    }

    public LeaFeaturesDatabase.A11ySettings getSettings() {
        return LeaFeaturesDatabase.get(ctx).getA11ySettings();
    }
}
