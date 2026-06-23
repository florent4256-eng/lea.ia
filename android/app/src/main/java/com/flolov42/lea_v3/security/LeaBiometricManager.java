package com.flolov42.lea_v3.security;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import androidx.annotation.RequiresApi;
import java.util.concurrent.Executor;

public class LeaBiometricManager {

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    private static LeaBiometricManager instance;
    private final Context ctx;

    public static synchronized LeaBiometricManager get(Context ctx) {
        if (instance == null) instance = new LeaBiometricManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaBiometricManager(Context ctx) { this.ctx = ctx; }

    public boolean isBiometricEnabled() {
        return LeaFeaturesDatabase.get(ctx).getBiometricConfig().enabled == 1;
    }

    public boolean isAvailable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        android.hardware.biometrics.BiometricManager bm =
            ctx.getSystemService(android.hardware.biometrics.BiometricManager.class);
        if (bm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return bm.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void authenticate(android.app.Activity activity, String title, String subtitle, AuthCallback callback) {
        Executor executor = activity.getMainExecutor();
        CancellationSignal cancel = new CancellationSignal();

        BiometricPrompt prompt = new BiometricPrompt.Builder(ctx)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Utilisez votre empreinte ou Face ID")
            .setNegativeButton("Annuler", executor, (dialog, which) -> callback.onFailure("cancelled"))
            .build();

        prompt.authenticate(cancel, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                callback.onSuccess();
            }
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                callback.onFailure(errString.toString());
            }
            @Override
            public void onAuthenticationFailed() {
                callback.onFailure("Authentification échouée");
            }
        });
    }

    public void setEnabled(boolean enabled) {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        LeaFeaturesDatabase.BiometricCfg cfg = db.getBiometricConfig();
        db.saveBiometricConfig(enabled ? 1 : 0, cfg.timeoutMinutes, cfg.forceOnLaunch);
        if (enabled) {
            com.flolov42.lea_v3.achievements.LeaAchievementManager.get(ctx).trigger("BIOMETRIC_ON");
        }
    }

    public String simpleEncrypt(String plain) {
        StringBuilder sb = new StringBuilder();
        for (char c : plain.toCharArray()) sb.append((int)c).append(",");
        return sb.toString();
    }

    public String simpleDecrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return "";
        try {
            String[] parts = encrypted.split(",");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) if (!p.isEmpty()) sb.append((char)Integer.parseInt(p.trim()));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
