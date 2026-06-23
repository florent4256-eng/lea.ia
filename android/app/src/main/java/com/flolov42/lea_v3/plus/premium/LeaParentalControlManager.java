package com.flolov42.lea_v3.plus.premium;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import com.flolov42.lea_v3.database.LeaFamilyDatabase;
import java.util.Calendar;

public class LeaParentalControlManager {

    private static final String PREFS          = "lea_parental_control";
    private static final String KEY_ACTIVE_PSEUDO = "active_child_pseudo";
    private static final String KEY_SESSION_START = "session_start_ts";
    private static final String CHANNEL_ID     = "lea_parental";

    private final Context ctx;
    private final LeaFamilyDatabase db;
    private final SharedPreferences prefs;

    private static LeaParentalControlManager instance;
    public static synchronized LeaParentalControlManager get(Context ctx) {
        if (instance == null) instance = new LeaParentalControlManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaParentalControlManager(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaFamilyDatabase.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        createNotifChannel();
    }

    // ── Session enfant ────────────────────────────────────────────────────────
    public void startChildSession(String pseudo) {
        prefs.edit()
            .putString(KEY_ACTIVE_PSEUDO, pseudo)
            .putLong(KEY_SESSION_START, System.currentTimeMillis())
            .apply();
        db.logEvent(pseudo, "SESSION_START", "Début de session");
    }
    public void endChildSession() {
        String pseudo = getActiveChildPseudo();
        if (pseudo == null) return;

        long start = prefs.getLong(KEY_SESSION_START, System.currentTimeMillis());
        int minutes = (int) ((System.currentTimeMillis() - start) / 60000);
        if (minutes > 0) {
            db.recordScreenTime(pseudo, minutes);
            db.logEvent(pseudo, "SESSION_END", minutes + " min utilisées");
        }
        prefs.edit().remove(KEY_ACTIVE_PSEUDO).remove(KEY_SESSION_START).apply();
    }
    public boolean isChildSessionActive() {
        return prefs.contains(KEY_ACTIVE_PSEUDO);
    }
    public String getActiveChildPseudo() {
        return prefs.getString(KEY_ACTIVE_PSEUDO, null);
    }

    // ── Vérification d'accès ──────────────────────────────────────────────────
    public boolean checkFeatureAccess(String featureId) {
        String pseudo = getActiveChildPseudo();
        if (pseudo == null) return true;  // mode parent = tout autorisé

        LeaFamilyDatabase.ChildAccount account = db.getChildAccount(pseudo);
        if (account == null) return true;

        // Vérifier couvre-feu
        if (isBedtimeLocked(account)) return false;

        // Vérifier temps d'écran
        if (isScreenTimeLimitReached(account)) return false;

        // Vérifier whitelist de fonctionnalités
        return db.isFeatureAllowed(pseudo, featureId);
    }

    public boolean isBedtimeLocked(LeaFamilyDatabase.ChildAccount account) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        // Bloqué si heure >= bedtime OU heure < wakeup
        if (account.bedtimeHour > account.wakeupHour) {
            // Ex: coucher 21h, lever 7h → bloquer de 21h à 7h
            return hour >= account.bedtimeHour || hour < account.wakeupHour;
        } else {
            // Cas inhabituel (bedtime < wakeup)
            return hour >= account.bedtimeHour && hour < account.wakeupHour;
        }
    }
    public boolean isBedtimeLocked() {
        String pseudo = getActiveChildPseudo();
        if (pseudo == null) return false;
        LeaFamilyDatabase.ChildAccount a = db.getChildAccount(pseudo);
        return a != null && isBedtimeLocked(a);
    }

    public boolean isScreenTimeLimitReached(LeaFamilyDatabase.ChildAccount account) {
        int used    = db.getScreenUsageToday(account.pseudo);
        long start  = prefs.getLong(KEY_SESSION_START, System.currentTimeMillis());
        int currentSessionMin = (int) ((System.currentTimeMillis() - start) / 60000);
        return (used + currentSessionMin) >= account.screenLimitMin;
    }
    public boolean isScreenTimeLimitReached() {
        String pseudo = getActiveChildPseudo();
        if (pseudo == null) return false;
        LeaFamilyDatabase.ChildAccount a = db.getChildAccount(pseudo);
        return a != null && isScreenTimeLimitReached(a);
    }

    // ── Infos session ─────────────────────────────────────────────────────────
    public int getSessionMinutes() {
        long start = prefs.getLong(KEY_SESSION_START, System.currentTimeMillis());
        return (int) ((System.currentTimeMillis() - start) / 60000);
    }
    public int getTodayTotalMinutes() {
        String pseudo = getActiveChildPseudo();
        if (pseudo == null) return 0;
        return db.getScreenUsageToday(pseudo) + getSessionMinutes();
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    public void sendParentNotification(String title, String message) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        nm.notify((int)(System.currentTimeMillis() % 90000) + 99000, n.build());
    }
    public void checkAndNotifyScreenTime() {
        String pseudo = getActiveChildPseudo();
        if (pseudo == null) return;
        LeaFamilyDatabase.ChildAccount a = db.getChildAccount(pseudo);
        if (a == null) return;
        int total = getTodayTotalMinutes();
        int limit = a.screenLimitMin;
        // Avertissement à 80%
        if (total >= (int)(limit * 0.8) && total < limit) {
            int remaining = limit - total;
            sendParentNotification("⏰ " + pseudo + " — Temps écran",
                "Il reste " + remaining + " minutes. Limite: " + limit + " min/jour.");
            db.logEvent(pseudo, "SCREEN_WARNING", remaining + " min restantes");
        }
        // Limite atteinte
        if (total >= limit) {
            sendParentNotification("🚫 " + pseudo + " — Limite atteinte",
                "Temps d'écran quotidien épuisé (" + limit + " min).");
            db.logEvent(pseudo, "SCREEN_LIMIT", "Limite atteinte: " + total + " min");
        }
    }

    // ── Canal ─────────────────────────────────────────────────────────────────
    private void createNotifChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Contrôle Parental", NotificationManager.IMPORTANCE_HIGH);
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }
}
