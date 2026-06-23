package com.flolov42.lea_v3.core;

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


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import java.util.Calendar;
import java.util.List;

public class LeaAutomationManager {

    private static final String TAG = "LeaAuto";

    private final Context ctx;
    private final LeaNovaDataStore db;

    public LeaAutomationManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.db  = LeaNovaDataStore.get(ctx);
    }

    /**
     * Crée et planifie une routine.
     * @param name  ex: "Routine matin"
     * @param time  ex: "08:00"
     * @param days  ex: "1,2,3,4,5"  (1=lun)
     * @param actions tableau de commandes textuelles
     */
    public long createRoutine(String name, String time, String days, String[] actions) {
        try {
            JSONArray arr = new JSONArray();
            for (String a : actions) if (a != null && !a.trim().isEmpty()) arr.put(a.trim());
            long id = db.saveRoutine(name, time, days, arr.toString());
            scheduleRoutine(id, time);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "createRoutine: " + e.getMessage());
            return -1;
        }
    }

    /** Planifie via AlarmManager (répétition journalière à l'heure donnée). */
    public void scheduleRoutine(long routineId, String time) {
        try {
            if (time == null || time.isEmpty()) return;
            String[] parts = time.split(":");
            if (parts.length == 0 || parts[0].isEmpty()) return;
            int hour   = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(ctx, RoutineTriggerReceiver.class);
            intent.putExtra("routine_id", routineId);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, (int) routineId, intent, flags);

            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
            }
            Log.d(TAG, "Routine #" + routineId + " planifiée à " + time);
        } catch (Exception e) {
            Log.e(TAG, "scheduleRoutine: " + e.getMessage());
        }
    }

    /** Exécute toutes les actions d'une routine. */
    public void executeRoutine(long routineId) {
        LeaNovaDataStore.Routine r = db.getRoutine(routineId);
        if (r == null || !r.active) return;
        try {
            JSONArray actions = new JSONArray(r.actionsJson);
            LeaNovaCommandProcessor proc = new LeaNovaCommandProcessor(ctx, getWebSocket());
            proc.setCallback(msg -> Log.d(TAG, "Routine result: " + msg));
            for (int i = 0; i < actions.length(); i++) {
                proc.process(actions.getString(i));
            }
            Log.d(TAG, "Routine \"" + r.name + "\" exécutée.");
        } catch (Exception e) {
            Log.e(TAG, "executeRoutine: " + e.getMessage());
        }
    }

    public List<LeaNovaDataStore.Routine> getAllRoutines() {
        return db.getAllRoutines();
    }

    public void deleteRoutine(long id) {
        db.deleteRoutine(id);
        cancelAlarm(id);
    }

    public void toggleRoutine(long id, boolean active) {
        db.setRoutineActive(id, active);
        if (!active) cancelAlarm(id);
        else {
            LeaNovaDataStore.Routine r = db.getRoutine(id);
            if (r != null) scheduleRoutine(r.id, r.triggerTime);
        }
    }

    private void cancelAlarm(long id) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, RoutineTriggerReceiver.class);
        int flags = PendingIntent.FLAG_NO_CREATE
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, (int) id, intent, flags);
        if (am != null && pi != null) am.cancel(pi);
    }

    private org.java_websocket.client.WebSocketClient getWebSocket() {
        LeaNovaService svc = LeaNovaService.instance;
        return svc != null ? svc.wsClient : null;
    }

    // Déclenché par AlarmManager
    public static class RoutineTriggerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra("routine_id", -1);
            if (id < 0) return;
            LeaAutomationManager mgr = new LeaAutomationManager(ctx);
            mgr.executeRoutine(id);
            // Re-planifie pour demain
            LeaNovaDataStore.Routine r = LeaNovaDataStore.get(ctx).getRoutine(id);
            if (r != null && r.active) mgr.scheduleRoutine(r.id, r.triggerTime);
        }
    }
}
