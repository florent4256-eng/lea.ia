package com.flolov42.lea_v3.agents;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.agents.*;
import com.flolov42.lea_v3.modes.*;
import com.flolov42.lea_v3.plus.gamification.*;
import com.flolov42.lea_v3.plus.lifestyle.*;
import com.flolov42.lea_v3.plus.learning.*;
import com.flolov42.lea_v3.plus.premium.*;
import com.flolov42.lea_v3.plus.connect.*;
import com.flolov42.lea_v3.bixby.*;
import com.flolov42.lea_v3.routines.*;
import com.flolov42.lea_v3.telephony.*;
import com.flolov42.lea_v3.code.*;
import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;
import com.flolov42.lea_v3.utilities.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaNotificationAgent {

    private static final String ID    = LeaAgentActivationManager.NOTIFICATION;
    private static final String PREFS = "lea_notif_agent";

    // Seuil minimum d'importance pour envoyer un rapport
    private static final int MIN_IMPORTANT_TO_NOTIFY = 3;
    private static final int MIN_SOCIAL_TO_NOTIFY    = 10;

    private final Context                    ctx;
    private final LeaAgentDatabase           db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences          prefs;

    // Accumulation thread-safe entre le NotificationListenerService (main) et execute() (bg)
    public static final Map<String, List<String>> groups =
        Collections.synchronizedMap(new HashMap<>());
    public static volatile int blockedCount = 0;

    // Applications considérées comme spam (publicité, réseaux sociaux)
    private static final String[] SPAM_SOURCES = {
        "com.facebook.katana", "com.instagram.android",
        "com.tiktok.android",  "com.zhiliaoapp.musically",
        "com.snapchat.android","com.twitter.android",
        "com.pinterest.android"
    };

    // Applications sociales (moins urgentes)
    private static final String[] SOCIAL_SOURCES = {
        "instagram", "facebook", "tiktok", "snapchat", "twitter", "whatsapp",
        "telegram",  "messenger", "discord", "linkedin", "reddit"
    };

    public LeaNotificationAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Appelé toutes les 2h. Génère un rapport groupé uniquement si le contenu
     * dépasse les seuils d'importance (évite le spam de notifications).
     */
    public void execute() {
        try {
            if (groups.isEmpty() && blockedCount == 0) {
                db.addLog(ID, "🔔 Aucune notification accumulée sur les 2 dernières heures.");
                return;
            }

            int importantCount = 0;
            int socialCount    = 0;
            int blockedTotal   = blockedCount;
            StringBuilder details = new StringBuilder();

            for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                String pkg    = entry.getKey();
                int    count  = entry.getValue().size();

                if (isSpam(pkg)) {
                    blockedTotal += count;
                } else if (isSocial(pkg)) {
                    socialCount += count;
                } else {
                    importantCount += count;
                    details.append("  • ").append(getAppName(pkg))
                           .append(" : ").append(count)
                           .append(" message").append(count > 1 ? "s" : "").append("\n");
                }
            }

            // Log toujours (pour l'historique agent)
            String summary = buildSummary(importantCount, socialCount, blockedTotal, details.toString());
            db.addLog(ID, "🔔 Rapport 2h : " + summary);
            db.updateLastAction(ID, summary);

            // Notification système uniquement si le contenu dépasse les seuils
            boolean shouldNotify = importantCount >= MIN_IMPORTANT_TO_NOTIFY
                                || socialCount    >= MIN_SOCIAL_TO_NOTIFY;

            if (shouldNotify) {
                String title = buildTitle(importantCount, socialCount);
                notif.notify(ID, title, summary);
            }

            // Réinitialise l'accumulation après le rapport
            groups.clear();
            blockedCount = 0;

        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur: " + e.getMessage());
        }
    }

    private String buildTitle(int important, int social) {
        if (important > 0 && social > 0) return "🔔 " + important + " messages · " + social + " sociales";
        if (important > 0)               return "🔔 " + important + " messages importants";
        return "🔔 " + social + " notifications sociales";
    }

    private String buildSummary(int important, int social, int blocked, String details) {
        StringBuilder sb = new StringBuilder();
        if (important > 0) sb.append(important).append(" message").append(important > 1 ? "s" : "").append(" important").append(important > 1 ? "s" : "");
        if (social > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(social).append(" notification").append(social > 1 ? "s" : "").append(" sociale").append(social > 1 ? "s" : "");
        }
        if (blocked > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(blocked).append(" publicité").append(blocked > 1 ? "s" : "").append(" bloquée").append(blocked > 1 ? "s" : "");
        }
        if (sb.length() == 0) sb.append("Aucune notification");
        if (!details.isEmpty()) sb.append("\n").append(details.trim());
        return sb.toString();
    }

    private boolean isSpam(String pkg) {
        if (pkg == null) return false;
        for (String spam : SPAM_SOURCES) {
            if (pkg.startsWith(spam)) return true;
        }
        return false;
    }

    private boolean isSocial(String pkg) {
        if (pkg == null) return false;
        for (String src : SOCIAL_SOURCES) {
            if (pkg.contains(src)) return true;
        }
        return false;
    }

    private String getAppName(String pkg) {
        if (pkg == null) return "App";
        if (pkg.contains("gmail"))      return "Gmail";
        if (pkg.contains("sms")
         || pkg.contains("messaging"))  return "SMS";
        if (pkg.contains("phone")
         || pkg.contains("dialer"))     return "Téléphone";
        if (pkg.contains("calendar"))   return "Calendrier";
        if (pkg.contains("telegram"))   return "Telegram";
        if (pkg.contains("whatsapp"))   return "WhatsApp";
        if (pkg.contains("outlook"))    return "Outlook";
        if (pkg.contains("slack"))      return "Slack";
        if (pkg.contains("teams"))      return "Teams";
        if (pkg.contains("signal"))     return "Signal";
        if (pkg.contains("amazon"))     return "Amazon";
        if (pkg.contains("paypal"))     return "PayPal";
        if (pkg.contains("bank")
         || pkg.contains("credit")
         || pkg.contains("bnp")
         || pkg.contains("credit")
         || pkg.contains("lcl"))        return "Banque";
        // Dernier recours : dernier segment du package
        String[] parts = pkg.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : pkg;
    }

    /**
     * Appelé par LeaNotificationService.onNotificationPosted() — thread main.
     * Accumule sans émettre — le rapport groupé est produit toutes les 2h par execute().
     */
    public static void onNotificationReceived(String packageName, String title) {
        if (packageName == null) return;
        synchronized (groups) {
            List<String> list = groups.get(packageName);
            if (list == null) {
                list = new ArrayList<>();
                groups.put(packageName, list);
            }
            list.add(title != null ? title : "");
        }
    }

    public static void onNotificationReceived(String packageName) {
        onNotificationReceived(packageName, null);
    }

    public static void onNotificationBlocked() {
        blockedCount++;
    }

    // ── Helpers pour l'UI ─────────────────────────────────────────────────────

    /** Vérifie si le NotificationListenerService est autorisé par l'utilisateur. */
    public static boolean isListenerEnabled(Context ctx) {
        String enabled = Settings.Secure.getString(
            ctx.getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(ctx.getPackageName());
    }

    /** Nombre de notifications accumulées depuis le dernier rapport. */
    public static int getAccumulatedCount() {
        synchronized (groups) {
            int total = 0;
            for (List<String> l : groups.values()) total += l.size();
            return total;
        }
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
