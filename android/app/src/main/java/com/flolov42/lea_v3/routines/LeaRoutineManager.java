package com.flolov42.lea_v3.routines;

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
import java.util.List;

public class LeaRoutineManager {

    private final Context                     ctx;
    private final LeaRoutineDatabase          db;
    private final LeaRoutineActionsExecutor   executor;
    private final LeaRoutineNotificationManager notifMgr;

    public LeaRoutineManager(Context ctx) {
        this.ctx      = ctx.getApplicationContext();
        this.db       = LeaRoutineDatabase.get(ctx);
        this.executor = new LeaRoutineActionsExecutor(ctx);
        this.notifMgr = new LeaRoutineNotificationManager(ctx);

        LeaRoutinePresets.loadIfEmpty(ctx);
        startConditionDetector();
    }

    private void startConditionDetector() {
        ctx.startService(new Intent(ctx, LeaRoutineConditionDetector.class));
    }

    // ── Routine CRUD ─────────────────────────────────────────────────────────

    public List<LeaRoutineDatabase.RoutineRow> getAll() {
        return db.getAllRoutines();
    }

    public long create(String name, String icon, int iconColor,
                       String conditionsJson, String actionsJson, boolean preset) {
        LeaRoutineDatabase.RoutineRow r = new LeaRoutineDatabase.RoutineRow();
        r.name           = name;
        r.icon           = icon;
        r.iconColor      = iconColor;
        r.conditionsJson = conditionsJson;
        r.actionsJson    = actionsJson;
        r.active         = false;
        r.preset         = preset;
        return db.insertRoutine(r);
    }

    public void update(LeaRoutineDatabase.RoutineRow r) {
        db.updateRoutine(r);
    }

    public void delete(long id) {
        db.setRoutineActive(id, false);
        db.deleteRoutine(id);
        refreshNotification();
    }

    public void activate(long id) {
        LeaRoutineDatabase.RoutineRow r = db.getRoutine(id);
        if (r == null) return;
        db.setRoutineActive(id, true);
        db.setRoutineUserEnabled(id, true); // consentement explicite de l'utilisateur
        executor.execute(r.actionsJson);
        notifMgr.showActiveRoutine(r.name, r.icon);
    }

    public void deactivate(long id) {
        db.setRoutineActive(id, false);
        db.setRoutineUserEnabled(id, false); // l'user désactive → plus d'auto-activation
        refreshNotification();
    }

    public void toggle(long id, boolean active) {
        if (active) activate(id); else deactivate(id);
    }

    // ── Mode methods ─────────────────────────────────────────────────────────

    public boolean isModeActive(String type) {
        LeaRoutineDatabase.ModeRow m = db.getMode(type);
        return m != null && m.active;
    }

    public void setModeActive(String type, boolean active, String defaultActionsJson) {
        db.upsertMode(type, active, defaultActionsJson);
        if (active) executor.execute(defaultActionsJson);
    }

    // ── Notification sync ────────────────────────────────────────────────────

    private void refreshNotification() {
        List<LeaRoutineDatabase.RoutineRow> list = db.getAllRoutines();
        int count = 0;
        LeaRoutineDatabase.RoutineRow last = null;
        for (LeaRoutineDatabase.RoutineRow r : list) {
            if (r.active) { count++; last = r; }
        }
        if (count == 0)                      notifMgr.cancel();
        else if (count == 1 && last != null) notifMgr.showActiveRoutine(last.name, last.icon);
        else                                 notifMgr.showMultipleActive(count);
    }
}
