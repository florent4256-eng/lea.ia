package com.flolov42.lea_v3.security;

import com.flolov42.lea_v3.database.*;

import android.content.Context;

public class LeaSessionManager {

    private static LeaSessionManager instance;
    private final Context ctx;
    private long lastActivityTs = System.currentTimeMillis();

    public static synchronized LeaSessionManager get(Context ctx) {
        if (instance == null) instance = new LeaSessionManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaSessionManager(Context ctx) { this.ctx = ctx; }

    public void touch() { lastActivityTs = System.currentTimeMillis(); }

    public boolean isSessionExpired() {
        LeaFeaturesDatabase.BiometricCfg cfg = LeaFeaturesDatabase.get(ctx).getBiometricConfig();
        if (cfg.enabled == 0) return false;
        long timeoutMs = cfg.timeoutMinutes * 60_000L;
        return (System.currentTimeMillis() - lastActivityTs) > timeoutMs;
    }

    public void reset() { lastActivityTs = System.currentTimeMillis(); }

    public int getTimeoutMinutes() {
        return LeaFeaturesDatabase.get(ctx).getBiometricConfig().timeoutMinutes;
    }

    public void setTimeoutMinutes(int minutes) {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        LeaFeaturesDatabase.BiometricCfg cfg = db.getBiometricConfig();
        db.saveBiometricConfig(cfg.enabled, minutes, cfg.forceOnLaunch);
    }
}
