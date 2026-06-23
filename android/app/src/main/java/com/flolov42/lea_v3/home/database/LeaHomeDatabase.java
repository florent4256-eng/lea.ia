package com.flolov42.lea_v3.home.database;

import com.flolov42.lea_v3.home.models.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class LeaHomeDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_home.db";
    private static final int    DB_VERSION = 2;

    private static final String CREATE_DEVICES =
        "CREATE TABLE IF NOT EXISTS devices (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "entity_id TEXT UNIQUE, " +
        "friendly_name TEXT, " +
        "type TEXT, " +
        "protocol TEXT, " +
        "room TEXT, " +
        "state TEXT DEFAULT 'unknown', " +
        "attributes TEXT, " +
        "ip_address TEXT, " +
        "is_favorite INTEGER DEFAULT 0, " +
        "last_seen INTEGER DEFAULT 0)";

    private static final String CREATE_SCENES =
        "CREATE TABLE IF NOT EXISTS scenes (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT, " +
        "room TEXT, " +
        "icon TEXT, " +
        "commands TEXT, " +   // JSON array of {entityId, state, attributes}
        "created_at INTEGER)";

    private static final String CREATE_LOGS =
        "CREATE TABLE IF NOT EXISTS home_logs (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "entity_id TEXT, " +
        "action TEXT, " +
        "result TEXT, " +
        "timestamp INTEGER)";

    private static LeaHomeDatabase instance;

    public static synchronized LeaHomeDatabase get(Context ctx) {
        if (instance == null) instance = new LeaHomeDatabase(ctx.getApplicationContext());
        return instance;
    }

    private LeaHomeDatabase(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DEVICES);
        db.execSQL(CREATE_SCENES);
        db.execSQL(CREATE_LOGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            // v2: ajout scenes + logs + colonnes manquantes dans devices (sans perte de données)
            db.execSQL(CREATE_SCENES);
            db.execSQL(CREATE_LOGS);
            try { db.execSQL("ALTER TABLE devices ADD COLUMN is_favorite INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE devices ADD COLUMN attributes TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE devices ADD COLUMN ip_address TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE devices ADD COLUMN last_seen INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        }
    }

    // ── Devices ───────────────────────────────────────────────────────────────

    public void upsertDevice(SmartDevice d) {
        ContentValues cv = new ContentValues();
        cv.put("entity_id",     d.entityId);
        cv.put("friendly_name", d.friendlyName);
        cv.put("type",          d.type != null ? d.type.name() : "UNKNOWN");
        cv.put("protocol",      d.protocol != null ? d.protocol.name() : "UNKNOWN");
        cv.put("room",          d.room);
        cv.put("state",         d.state);
        cv.put("attributes",    d.attributes);
        cv.put("ip_address",    d.ipAddress);
        cv.put("is_favorite",   d.isFavorite ? 1 : 0);
        cv.put("last_seen",     System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("devices", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateDeviceState(String entityId, String state, String attributes) {
        ContentValues cv = new ContentValues();
        cv.put("state", state);
        if (attributes != null) cv.put("attributes", attributes);
        cv.put("last_seen", System.currentTimeMillis());
        getWritableDatabase().update("devices", cv, "entity_id=?", new String[]{entityId});
    }

    public void setFavorite(String entityId, boolean fav) {
        ContentValues cv = new ContentValues();
        cv.put("is_favorite", fav ? 1 : 0);
        getWritableDatabase().update("devices", cv, "entity_id=?", new String[]{entityId});
    }

    public List<SmartDevice> getAllDevices() {
        return queryDevices("SELECT * FROM devices ORDER BY room, friendly_name", null);
    }

    public List<SmartDevice> getDevicesByRoom(String room) {
        return queryDevices("SELECT * FROM devices WHERE room=? ORDER BY friendly_name",
                new String[]{room});
    }

    public List<SmartDevice> getFavorites() {
        return queryDevices("SELECT * FROM devices WHERE is_favorite=1 ORDER BY friendly_name", null);
    }

    public List<String> getRooms() {
        List<String> rooms = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT DISTINCT room FROM devices WHERE room IS NOT NULL ORDER BY room", null);
        try {
            while (c.moveToNext()) rooms.add(c.getString(0));
        } finally {
            c.close();
        }
        return rooms;
    }

    private List<SmartDevice> queryDevices(String sql, String[] args) {
        List<SmartDevice> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) {
                SmartDevice d = new SmartDevice();
                d.id           = c.getLong(c.getColumnIndexOrThrow("id"));
                d.entityId     = c.getString(c.getColumnIndexOrThrow("entity_id"));
                d.friendlyName = c.getString(c.getColumnIndexOrThrow("friendly_name"));
                try { d.type = DeviceType.valueOf(c.getString(c.getColumnIndexOrThrow("type"))); }
                catch (Exception e) { d.type = DeviceType.UNKNOWN; }
                try { d.protocol = Protocol.valueOf(c.getString(c.getColumnIndexOrThrow("protocol"))); }
                catch (Exception e) { d.protocol = Protocol.UNKNOWN; }
                d.room        = c.getString(c.getColumnIndexOrThrow("room"));
                d.state       = c.getString(c.getColumnIndexOrThrow("state"));
                d.attributes  = c.getString(c.getColumnIndexOrThrow("attributes"));
                d.ipAddress   = c.getString(c.getColumnIndexOrThrow("ip_address"));
                d.isFavorite  = c.getInt(c.getColumnIndexOrThrow("is_favorite")) == 1;
                d.lastSeen    = c.getLong(c.getColumnIndexOrThrow("last_seen"));
                list.add(d);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public void deleteDevice(String entityId) {
        getWritableDatabase().delete("devices", "entity_id=?", new String[]{entityId});
    }

    // ── Scenes ────────────────────────────────────────────────────────────────

    public static class Scene {
        public long   id;
        public String name, room, icon, commands;
        public long   createdAt;
    }

    public long saveScene(String name, String room, String icon, String commandsJson) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("room", room);
        cv.put("icon", icon);
        cv.put("commands", commandsJson);
        cv.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("scenes", null, cv);
    }

    public List<Scene> getScenes(String room) {
        List<Scene> list = new ArrayList<>();
        String sql = (room == null)
            ? "SELECT * FROM scenes ORDER BY name"
            : "SELECT * FROM scenes WHERE room=? ORDER BY name";
        Cursor c = getReadableDatabase().rawQuery(sql, room == null ? null : new String[]{room});
        try {
            while (c.moveToNext()) {
                Scene s = new Scene();
                s.id        = c.getLong(0);
                s.name      = c.getString(1);
                s.room      = c.getString(2);
                s.icon      = c.getString(3);
                s.commands  = c.getString(4);
                s.createdAt = c.getLong(5);
                list.add(s);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public void deleteScene(long id) {
        getWritableDatabase().delete("scenes", "id=?", new String[]{String.valueOf(id)});
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    public void log(String entityId, String action, String result) {
        ContentValues cv = new ContentValues();
        cv.put("entity_id", entityId);
        cv.put("action", action);
        cv.put("result", result);
        cv.put("timestamp", System.currentTimeMillis());
        getWritableDatabase().insert("home_logs", null, cv);
        getWritableDatabase().execSQL(
            "DELETE FROM home_logs WHERE id NOT IN (SELECT id FROM home_logs ORDER BY timestamp DESC LIMIT 500)");
    }
}
