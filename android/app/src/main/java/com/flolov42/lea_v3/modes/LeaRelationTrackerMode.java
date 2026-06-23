package com.flolov42.lea_v3.modes;

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


import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaRelationTrackerMode extends LeaBaseMode {

    private static final long DAY_MS = 24L * 3600 * 1000;

    public LeaRelationTrackerMode(Context ctx) { super(ctx, LeaModeDatabase.RELATIONS); }

    @Override
    public void execute() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            log("🔒 READ_CALL_LOG requis pour analyser les relations");
            return;
        }
        analyzeCallHistory();
        checkRelationAlerts();
    }

    private void analyzeCallHistory() {
        Cursor c = null;
        try {
            ContentResolver cr = ctx.getContentResolver();
            long monthAgo = System.currentTimeMillis() - 30L * DAY_MS;
            c = cr.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE},
                CallLog.Calls.DATE + " > ?", new String[]{String.valueOf(monthAgo)},
                CallLog.Calls.DATE + " DESC");
            if (c == null) return;

            Map<String, long[]> contacts = new HashMap<>(); // phone → [lastContact, callCount]
            Map<String, String> names    = new HashMap<>();

            while (c.moveToNext()) {
                String phone = c.getString(0);
                String name  = c.getString(1);
                long   date  = c.getLong(2);
                if (phone == null) continue;
                // Initialiser à 1 (ce premier appel compte déjà)
                if (!contacts.containsKey(phone)) contacts.put(phone, new long[]{date, 1});
                else contacts.get(phone)[1]++;
                if (name != null && !name.isEmpty()) names.put(phone, name);
            }

            for (Map.Entry<String, long[]> e : contacts.entrySet()) {
                String phone    = e.getKey();
                long   last     = e.getValue()[0];
                long   count    = e.getValue()[1];
                String name     = names.getOrDefault(phone, phone);
                int    freqDays = (int) Math.max(1, 30 / Math.max(1, count));
                int    health   = computeHealth(last, freqDays);
                String sentiment= health >= 70 ? "Proche" : health >= 40 ? "Distant" : "À risque";
                db.upsertRelation(name, phone, last, freqDays, health, sentiment);
            }

            log("❤️ " + contacts.size() + " relation(s) analysée(s) sur le dernier mois");
        } catch (Exception e) {
            log("⚠️ Erreur analyse relations: " + e.getMessage());
        } finally {
            if (c != null) c.close();
        }
    }

    private int computeHealth(long lastContact, int freqDays) {
        long daysSinceLast = (System.currentTimeMillis() - lastContact) / DAY_MS;
        if (daysSinceLast <= freqDays)         return 90;
        if (daysSinceLast <= freqDays * 2)     return 65;
        if (daysSinceLast <= freqDays * 4)     return 35;
        return 10;
    }

    private void checkRelationAlerts() {
        List<LeaModeDatabase.RelationRow> relations = db.getRelations();
        int atRisk = 0;
        for (LeaModeDatabase.RelationRow r : relations) {
            if (r.healthScore < 30) {
                long daysSince = (System.currentTimeMillis() - r.lastContact) / DAY_MS;
                log("⚠️ Relation à risque: " + r.name + " (dernier contact: " + daysSince + " jours)");
                atRisk++;
            }
        }
        // Notifier au maximum 1 fois par jour (évite spam toutes les 30min)
        if (atRisk > 0) {
            android.content.SharedPreferences prefs =
                ctx.getSharedPreferences("lea_relations", android.content.Context.MODE_PRIVATE);
            String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
            if (!today.equals(prefs.getString("last_alert_day", ""))) {
                notify("❤️ Relation Tracker",
                    atRisk + " relation(s) nécessitent ton attention — tu ne les as pas contactées depuis longtemps");
                prefs.edit().putString("last_alert_day", today).apply();
            }
        }
    }

    public String getRelationReport() {
        List<LeaModeDatabase.RelationRow> relations = db.getRelations();
        if (relations.isEmpty()) return "❤️ Aucune relation analysée — active ce mode pour commencer.";
        StringBuilder sb = new StringBuilder("❤️ RAPPORT RELATIONS:\n\n");
        int good=0, ok=0, risk=0;
        for (LeaModeDatabase.RelationRow r : relations) {
            if      (r.healthScore >= 70) good++;
            else if (r.healthScore >= 40) ok++;
            else                          risk++;
        }
        sb.append("✅ Proches: ").append(good).append("\n");
        sb.append("😐 Distants: ").append(ok).append("\n");
        sb.append("⚠️ À risque: ").append(risk).append("\n\n");
        if (risk > 0) {
            sb.append("Actions suggérées:\n");
            int shown = 0;
            for (LeaModeDatabase.RelationRow r : relations) {
                if (r.healthScore < 40 && shown < 3) {
                    long days = (System.currentTimeMillis() - r.lastContact) / DAY_MS;
                    sb.append("• Appelle ").append(r.name).append(" (").append(days).append(" jours)\n");
                    shown++;
                }
            }
        }
        return sb.toString();
    }
}
