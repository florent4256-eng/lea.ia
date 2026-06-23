package com.flolov42.lea_v3.plus.learning;

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
import java.util.ArrayList;
import java.util.List;

public class LeaSmartNotifications extends LeaBasePlusFeature {

    private static final String PREFS = "lea_smart_notif";

    // Priority keywords → score
    private static final String[][] PRIORITY_RULES = {
        {"urgent",       "10"}, {"important",  "8"},  {"rappel",    "7"},
        {"deadline",     "9"},  {"rdv",        "8"},  {"paiement",  "9"},
        {"livraison",    "6"},  {"sécurité",   "10"}, {"mot de passe","9"},
        {"promo",        "2"},  {"newsletter", "1"},  {"pub",       "1"},
    };

    public LeaSmartNotifications(Context ctx) { super(ctx, LeaPlusDatabase.SMART_NOTIF); }

    @Override
    public void execute() {
        processBatchedNotifications();
        sendDigestIfScheduled();
    }

    private void processBatchedNotifications() {
        List<LeaPlusDatabase.NotifBatch> batch = db.getPendingBatch();
        if (batch.isEmpty()) return;

        // Group by priority
        List<LeaPlusDatabase.NotifBatch> high = new ArrayList<>();
        List<LeaPlusDatabase.NotifBatch> low  = new ArrayList<>();
        for (LeaPlusDatabase.NotifBatch n : batch) {
            if (n.priority >= 7) high.add(n);
            else                 low.add(n);
        }

        // Deliver high-priority immediately
        for (LeaPlusDatabase.NotifBatch n : high) {
            notify("🔴 " + n.app + " (priorité haute)", n.content);
            db.markNotifDelivered(n.id);
            log("🔴 Notif prioritaire: " + n.app);
        }

        // Batch low-priority
        if (!low.isEmpty()) {
            log("📦 " + low.size() + " notif(s) low-priority en attente de digest");
        }
    }

    private void sendDigestIfScheduled() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int digestHour = prefs.getInt("digest_hour", 12);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (hour == digestHour && !today.equals(prefs.getString("digest_sent", ""))) {
            List<LeaPlusDatabase.NotifBatch> pending = db.getPendingBatch();
            if (!pending.isEmpty()) {
                String digest = buildDigest(pending);
                notify("📬 Digest LÉA (" + pending.size() + " notifs)", digest);
                for (LeaPlusDatabase.NotifBatch n : pending) db.markNotifDelivered(n.id);
                log("📬 Digest envoyé: " + pending.size() + " notifications");
                prefs.edit().putString("digest_sent", today).apply();
            }
        }
    }

    public int scoreNotification(String app, String content) {
        String lower = content.toLowerCase();
        int score = 3; // default
        for (String[] rule : PRIORITY_RULES) {
            if (lower.contains(rule[0])) {
                score = Math.max(score, Integer.parseInt(rule[1]));
            }
        }
        return score;
    }

    public void addToBatch(String app, String content, String category) {
        int priority = scoreNotification(app, content);
        db.insertNotifBatch(app, content, category, priority);
        log("📥 Notif reçue: " + app + " [priorité " + priority + "/10]");
        if (priority >= 9) {
            notify("🚨 " + app, content);
            log("🚨 Notif critique transmise immédiatement: " + app);
        }
    }

    public void setDigestHour(int hour) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putInt("digest_hour", hour).apply();
        log("⏰ Digest configuré à " + hour + "h00");
    }

    public void setAppPriority(String app, int priority) {
        db.setAppPriorityPref(app, priority);
        log("⚙️ Priorité de " + app + " définie à " + priority + "/10");
    }

    public String getStats() {
        int pending = db.getPendingBatch().size();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int digestHour = prefs.getInt("digest_hour", 12);
        return "📬 SMART NOTIFICATIONS\n\n" +
               "Notifs en attente: " + pending + "\n" +
               "Digest quotidien: " + digestHour + "h00\n" +
               "Filtrage: Actif (seuil priorité: 7/10)\n\n" +
               "💡 Les notifs critiques (9-10) passent toujours immédiatement.";
    }

    private String buildDigest(List<LeaPlusDatabase.NotifBatch> items) {
        if (items.size() == 1) return items.get(0).app + ": " + items.get(0).content;
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(items.size(), 3);
        for (int i = 0; i < shown; i++)
            sb.append("• ").append(items.get(i).app).append("\n");
        if (items.size() > 3) sb.append("+ ").append(items.size() - 3).append(" autres…");
        return sb.toString().trim();
    }
}
