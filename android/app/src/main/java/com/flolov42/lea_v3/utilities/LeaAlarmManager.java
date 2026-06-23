package com.flolov42.lea_v3.utilities;

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
import android.content.Intent;
import android.provider.AlarmClock;
import android.util.Log;

/**
 * Crée des alarmes et minuteurs via AlarmClock (Intent standard Android).
 * Lance l'app Samsung Horloge pour PROUVER que c'est fait.
 */
public class LeaAlarmManager {

    private static final String TAG = "LeaAlarm";
    private final Context ctx;

    public LeaAlarmManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Réveil ────────────────────────────────────────────────────────────────

    public String setAlarm(int hour, int minute, String label) {
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR,    hour);
            i.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, label != null ? label : "Réveil Léa");
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, false); // ouvre l'app pour confirmer
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            String timeStr = String.format("%02d:%02d", hour, minute);
            return "✅ Réveil créé à " + timeStr + ". L'app s'ouvre pour confirmer.";
        } catch (Exception e) {
            Log.e(TAG, "setAlarm: " + e.getMessage());
            openClockApp();
            return "J'ai ouvert l'app horloge — crée le réveil manuellement.";
        }
    }

    // ── Minuteur ──────────────────────────────────────────────────────────────

    public String setTimer(int seconds, String label) {
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_TIMER);
            i.putExtra(AlarmClock.EXTRA_LENGTH,  seconds);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, label != null ? label : "Minuteur Léa");
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            String dur = seconds >= 3600
                ? (seconds / 3600) + "h" + ((seconds % 3600) / 60 > 0 ? (seconds % 3600) / 60 + "min" : "")
                : seconds >= 60
                    ? (seconds / 60) + " minute" + (seconds / 60 > 1 ? "s" : "")
                    : seconds + " secondes";
            return "✅ Minuteur de " + dur + " lancé.";
        } catch (Exception e) {
            Log.e(TAG, "setTimer: " + e.getMessage());
            openClockApp();
            return "J'ai ouvert l'app horloge — crée le minuteur manuellement.";
        }
    }

    // ── App Samsung horloge ───────────────────────────────────────────────────

    public void openClockApp() {
        String[] clockApps = {
            "com.samsung.android.app.alarmmanager",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock"
        };
        for (String pkg : clockApps) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Parser texte → heure/minutes ─────────────────────────────────────────

    public String parseAlarmCommand(String cmd) {
        String c = cmd.toLowerCase();
        int hour = 7, minute = 0;

        // Détecte "à 5h30", "à 8:45", "8 heures", "à 5h"
        try {
            int hIdx = c.indexOf('h');
            if (hIdx < 0) hIdx = c.indexOf(':');

            if (hIdx >= 0) {
                // Extract hour before the 'h' or ':'
                String beforeH = c.substring(0, hIdx).replaceAll("[^0-9]", " ").trim();
                String[] parts = beforeH.split("\\s+");
                if (parts.length > 0) {
                    hour = Integer.parseInt(parts[parts.length - 1]);
                }
                // Extract minutes after
                String afterH = c.substring(hIdx + 1).replaceAll("[^0-9].*", "").trim();
                if (!afterH.isEmpty() && afterH.length() <= 2) {
                    minute = Integer.parseInt(afterH);
                }
            } else {
                // Just a number
                for (String p : c.split("[^0-9]+")) {
                    if (!p.isEmpty()) { hour = Integer.parseInt(p); break; }
                }
            }
        } catch (Exception ignored) {}

        hour   = Math.max(0, Math.min(23, hour));
        minute = Math.max(0, Math.min(59, minute));

        String label = c.contains("réveil") ? "Réveil Léa" : "Alarme Léa";
        return setAlarm(hour, minute, label);
    }

    public String parseTimerCommand(String cmd) {
        String c = cmd.toLowerCase();
        int n = extractFirstNumber(c, 5);
        int seconds;
        if      (c.contains("heure"))                    seconds = n * 3600;
        else if (c.contains("minute") || c.contains("min")) seconds = n * 60;
        else if (c.contains("seconde") || c.contains("sec")) seconds = n;
        else                                               seconds = n * 60; // défaut minutes

        String label = "Minuteur Léa";
        return setTimer(seconds, label);
    }

    private int extractFirstNumber(String text, int def) {
        for (String p : text.split("[^0-9]+")) {
            if (!p.isEmpty()) try { return Integer.parseInt(p); } catch (Exception ignored) {}
        }
        return def;
    }
}
