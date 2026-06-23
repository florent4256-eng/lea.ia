package com.flolov42.lea_v3.bixby;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.flolov42.lea_v3.agents.LeaNotificationAgent;
import com.flolov42.lea_v3.core.LeaAgentActivationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Service qui écoute les notifications Android.
 * Léa peut lire les notifications récentes à la demande vocale.
 */
public class LeaNotificationService extends NotificationListenerService {

    private static volatile LeaNotificationService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();

        // Ignorer les notifications de Léa elle-même
        if (pkg == null || pkg.contains("com.flolov42")) return;

        // N'alimenter l'agent que s'il est activé
        try {
            LeaAgentActivationManager mgr = LeaAgentActivationManager.get(this);
            if (!mgr.isEnabled(LeaAgentActivationManager.NOTIFICATION)) return;
        } catch (Exception ignored) { return; }

        // Extraire le titre
        String title = null;
        try {
            android.app.Notification notif = sbn.getNotification();
            if (notif != null && notif.extras != null) {
                title = notif.extras.getString(android.app.Notification.EXTRA_TITLE, "");
            }
        } catch (Exception ignored) {}

        // Alimenter l'agent (accumule jusqu'au prochain rapport 2h)
        LeaNotificationAgent.onNotificationReceived(pkg, title);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    /** Retourne un résumé des N dernières notifications lisibles (hors Léa elle-même). */
    public static List<String> getRecentNotifications(int maxCount) {
        List<String> result = new ArrayList<>();
        if (instance == null) return result;
        try {
            StatusBarNotification[] active = instance.getActiveNotifications();
            if (active == null) return result;
            int count = 0;
            // Parcours inversé : les plus récentes d'abord
            for (int i = active.length - 1; i >= 0 && count < maxCount; i--) {
                StatusBarNotification sbn = active[i];
                String pkg = sbn.getPackageName();
                // Ignorer les notifications système Léa et les médias (lecture en cours)
                if (pkg.contains("com.flolov42") || pkg.contains("android.music")) continue;

                android.app.Notification notif = sbn.getNotification();
                if (notif == null) continue;
                android.os.Bundle extras = notif.extras;
                String title = extras.getString(android.app.Notification.EXTRA_TITLE, "");
                CharSequence body  = extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
                String text = body != null ? body.toString() : "";

                if (title.isEmpty() && text.isEmpty()) continue;

                String appName = getAppName(pkg);
                String line = appName + (title.isEmpty() ? "" : " — " + title)
                            + (text.isEmpty() ? "" : " : " + text);
                result.add(line);
                count++;
            }
        } catch (Exception e) {
            // Service pas encore lié, retourner liste vide
        }
        return result;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    private static String getAppName(String pkg) {
        if (pkg.contains("whatsapp"))  return "WhatsApp";
        if (pkg.contains("instagram")) return "Instagram";
        if (pkg.contains("messenger")) return "Messenger";
        if (pkg.contains("snapchat"))  return "Snapchat";
        if (pkg.contains("gmail"))     return "Gmail";
        if (pkg.contains("sms") || pkg.contains("messaging") || pkg.contains("mms")) return "SMS";
        if (pkg.contains("tiktok"))    return "TikTok";
        if (pkg.contains("twitter") || pkg.contains("x.com")) return "X/Twitter";
        if (pkg.contains("youtube"))   return "YouTube";
        if (pkg.contains("telegram"))  return "Telegram";
        if (pkg.contains("discord"))   return "Discord";
        // Fallback : prendre la dernière partie du package
        String[] parts = pkg.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : pkg;
    }
}
