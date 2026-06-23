package com.flolov42.lea_v3.database;

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


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class LeaRoutineDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_routines.db";
    private static final int    DB_VERSION = 2;

    private static LeaRoutineDatabase instance;

    public static synchronized LeaRoutineDatabase get(Context ctx) {
        if (instance == null) instance = new LeaRoutineDatabase(ctx.getApplicationContext());
        return instance;
    }

    private LeaRoutineDatabase(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS routines (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL," +
            "icon TEXT DEFAULT '⚡'," +
            "icon_color INTEGER DEFAULT -65281," +
            "conditions_json TEXT DEFAULT '[]'," +
            "actions_json TEXT DEFAULT '[]'," +
            "active INTEGER DEFAULT 0," +
            "preset INTEGER DEFAULT 0," +
            "user_enabled INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS modes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "type TEXT NOT NULL UNIQUE," +
            "active INTEGER DEFAULT 0," +
            "actions_json TEXT DEFAULT '[]')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migration douce : ajout de user_enabled sans perdre les routines existantes
            try { db.execSQL("ALTER TABLE routines ADD COLUMN user_enabled INTEGER DEFAULT 0"); }
            catch (Exception ignored) {}
        }
    }

    // ── ROUTINE CRUD ─────────────────────────────────────────────────────────

    public long insertRoutine(RoutineRow r) {
        ContentValues cv = new ContentValues();
        cv.put("name",            r.name);
        cv.put("icon",            r.icon);
        cv.put("icon_color",      r.iconColor);
        cv.put("conditions_json", r.conditionsJson);
        cv.put("actions_json",    r.actionsJson);
        cv.put("active",          r.active       ? 1 : 0);
        cv.put("preset",          r.preset       ? 1 : 0);
        cv.put("user_enabled",    r.userEnabled  ? 1 : 0);
        return getWritableDatabase().insert("routines", null, cv);
    }

    public boolean updateRoutine(RoutineRow r) {
        ContentValues cv = new ContentValues();
        cv.put("name",            r.name);
        cv.put("icon",            r.icon);
        cv.put("icon_color",      r.iconColor);
        cv.put("conditions_json", r.conditionsJson);
        cv.put("actions_json",    r.actionsJson);
        cv.put("active",          r.active      ? 1 : 0);
        cv.put("user_enabled",    r.userEnabled ? 1 : 0);
        return getWritableDatabase().update("routines", cv, "id=?",
            new String[]{String.valueOf(r.id)}) > 0;
    }

    public void setRoutineActive(long id, boolean active) {
        ContentValues cv = new ContentValues();
        cv.put("active", active ? 1 : 0);
        getWritableDatabase().update("routines", cv, "id=?",
            new String[]{String.valueOf(id)});
    }

    public void setRoutineUserEnabled(long id, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put("user_enabled", enabled ? 1 : 0);
        getWritableDatabase().update("routines", cv, "id=?",
            new String[]{String.valueOf(id)});
    }

    public void deleteRoutine(long id) {
        getWritableDatabase().delete("routines", "id=?",
            new String[]{String.valueOf(id)});
    }

    public List<RoutineRow> getAllRoutines() {
        List<RoutineRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM routines ORDER BY preset DESC, id ASC", null);
        while (c.moveToNext()) list.add(rowFromCursor(c));
        c.close();
        return list;
    }

    public RoutineRow getRoutine(long id) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM routines WHERE id=?", new String[]{String.valueOf(id)});
        RoutineRow r = null;
        if (c.moveToFirst()) r = rowFromCursor(c);
        c.close();
        return r;
    }

    private RoutineRow rowFromCursor(Cursor c) {
        RoutineRow r    = new RoutineRow();
        r.id             = c.getLong(c.getColumnIndexOrThrow("id"));
        r.name           = c.getString(c.getColumnIndexOrThrow("name"));
        r.icon           = c.getString(c.getColumnIndexOrThrow("icon"));
        r.iconColor      = c.getInt(c.getColumnIndexOrThrow("icon_color"));
        r.conditionsJson = c.getString(c.getColumnIndexOrThrow("conditions_json"));
        r.actionsJson    = c.getString(c.getColumnIndexOrThrow("actions_json"));
        r.active         = c.getInt(c.getColumnIndexOrThrow("active")) == 1;
        r.preset         = c.getInt(c.getColumnIndexOrThrow("preset")) == 1;
        int ueIdx        = c.getColumnIndex("user_enabled");
        r.userEnabled    = ueIdx >= 0 && c.getInt(ueIdx) == 1;
        return r;
    }

    // ── MODE CRUD ─────────────────────────────────────────────────────────────

    public void upsertMode(String type, boolean active, String actionsJson) {
        ContentValues cv = new ContentValues();
        cv.put("type",         type);
        cv.put("active",       active ? 1 : 0);
        cv.put("actions_json", actionsJson);
        getWritableDatabase().insertWithOnConflict(
            "modes", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public ModeRow getMode(String type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT * FROM modes WHERE type=?", new String[]{type});
        ModeRow m = null;
        if (c.moveToFirst()) {
            m             = new ModeRow();
            m.id          = c.getLong(c.getColumnIndexOrThrow("id"));
            m.type        = c.getString(c.getColumnIndexOrThrow("type"));
            m.active      = c.getInt(c.getColumnIndexOrThrow("active")) == 1;
            m.actionsJson = c.getString(c.getColumnIndexOrThrow("actions_json"));
        }
        c.close();
        return m;
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class RoutineRow {
        public long    id;
        public String  name;
        public String  icon            = "⚡";
        public int     iconColor       = 0xFF00E5FF;
        public String  conditionsJson  = "[]";
        public String  actionsJson     = "[]";
        public boolean active          = false;
        public boolean preset          = false;
        public boolean userEnabled     = false; // true seulement si l'user a activé explicitement
    }

    public static class ModeRow {
        public long    id;
        public String  type;
        public boolean active;
        public String  actionsJson;
    }
}
