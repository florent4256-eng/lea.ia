package com.flolov42.lea_v3.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class LeaAdminDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "lea_admin.db";
    private static final int    DB_VER  = 1;

    private static LeaAdminDatabase instance;
    public static synchronized LeaAdminDatabase get(Context ctx) {
        if (instance == null) instance = new LeaAdminDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LeaAdminDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VER); }

    // ── Data classes ──────────────────────────────────────────────────────────
    public static class WorldRow  { public String id,name,bossName; public int bossHp,bossDmg,xpReward,levelRequired,orderIndex; }
    public static class AdminLog  { public long ts; public String action, detail; }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS worlds (" +
            "id TEXT PRIMARY KEY, name TEXT, boss_name TEXT, boss_hp INTEGER DEFAULT 500, " +
            "boss_damage INTEGER DEFAULT 20, xp_reward INTEGER DEFAULT 200, " +
            "level_required INTEGER DEFAULT 1, order_index INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS admin_settings (" +
            "key TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS admin_logs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, action TEXT, detail TEXT)");

        // Mondes par défaut
        seedWorlds(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        // Migration douce — aucune table supprimée, aucune donnée perdue
        onCreate(db);
    }

    private void seedWorlds(SQLiteDatabase db) {
        String[][] worlds = {
            {"world_1","Procrastination Valley","Boss Procrastinus","500","20","200","1","1"},
            {"world_2","Distraction Jungle","Boss Scrollius","800","30","350","5","2"},
            {"world_3","Chaos Mountains","Boss Lazimoth","1200","45","500","10","3"},
            {"world_4","Void Desert","Boss Emptiness","2000","60","800","15","4"},
            {"world_5","Clarity Peaks","Boss Perfectus","3000","80","1200","20","5"},
        };
        for (String[] w : worlds) {
            ContentValues cv = new ContentValues();
            cv.put("id",w[0]); cv.put("name",w[1]); cv.put("boss_name",w[2]);
            cv.put("boss_hp",Integer.parseInt(w[3])); cv.put("boss_damage",Integer.parseInt(w[4]));
            cv.put("xp_reward",Integer.parseInt(w[5])); cv.put("level_required",Integer.parseInt(w[6]));
            cv.put("order_index",Integer.parseInt(w[7]));
            db.insertWithOnConflict("worlds", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
        // PIN admin par défaut
        ContentValues pin = new ContentValues();
        pin.put("key","admin_pin"); pin.put("value","1234");
        db.insertWithOnConflict("admin_settings", null, pin, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // ── Admin auth ────────────────────────────────────────────────────────────
    public String getAdminPin() {
        Cursor c = getReadableDatabase().rawQuery("SELECT value FROM admin_settings WHERE key='admin_pin'", null);
        String pin = "1234"; if (c.moveToFirst()) pin = c.getString(0); c.close(); return pin;
    }
    public void setAdminPin(String newPin) {
        ContentValues cv = new ContentValues(); cv.put("key","admin_pin"); cv.put("value",newPin);
        getWritableDatabase().insertWithOnConflict("admin_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public boolean checkPin(String pin) { return getAdminPin().equals(pin); }

    // ── Worlds CRUD ───────────────────────────────────────────────────────────
    public List<WorldRow> getWorlds() {
        List<WorldRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,boss_name,boss_hp,boss_damage,xp_reward,level_required,order_index FROM worlds ORDER BY order_index", null);
        while (c.moveToNext()) {
            WorldRow w=new WorldRow();
            w.id=c.getString(0); w.name=c.getString(1); w.bossName=c.getString(2);
            w.bossHp=c.getInt(3); w.bossDmg=c.getInt(4); w.xpReward=c.getInt(5);
            w.levelRequired=c.getInt(6); w.orderIndex=c.getInt(7);
            list.add(w);
        }
        c.close(); return list;
    }
    public void insertWorld(String name, String bossName, int bossHp, int bossDmg, int xpReward, int levelRequired) {
        int maxOrder = 0;
        Cursor c = getReadableDatabase().rawQuery("SELECT MAX(order_index) FROM worlds", null);
        if (c.moveToFirst() && !c.isNull(0)) maxOrder = c.getInt(0)+1; c.close();
        ContentValues cv = new ContentValues();
        cv.put("id","world_"+System.currentTimeMillis()); cv.put("name",name); cv.put("boss_name",bossName);
        cv.put("boss_hp",bossHp); cv.put("boss_damage",bossDmg); cv.put("xp_reward",xpReward);
        cv.put("level_required",levelRequired); cv.put("order_index",maxOrder);
        getWritableDatabase().insert("worlds", null, cv);
        logAction("CREATE_WORLD", name);
    }
    public void updateWorld(String id, String name, String bossName, int bossHp, int bossDmg, int xpReward, int levelRequired) {
        ContentValues cv = new ContentValues();
        cv.put("name",name); cv.put("boss_name",bossName); cv.put("boss_hp",bossHp);
        cv.put("boss_damage",bossDmg); cv.put("xp_reward",xpReward); cv.put("level_required",levelRequired);
        getWritableDatabase().update("worlds", cv, "id=?", new String[]{id});
        logAction("UPDATE_WORLD", name);
    }
    public void deleteWorld(String id) {
        getWritableDatabase().delete("worlds", "id=?", new String[]{id});
        logAction("DELETE_WORLD", id);
    }

    // ── Admin logs ────────────────────────────────────────────────────────────
    public void logAction(String action, String detail) {
        ContentValues cv = new ContentValues();
        cv.put("ts",System.currentTimeMillis()); cv.put("action",action); cv.put("detail",detail);
        getWritableDatabase().insert("admin_logs", null, cv);
    }
    public List<AdminLog> getRecentLogs(int limit) {
        List<AdminLog> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT ts,action,detail FROM admin_logs ORDER BY ts DESC LIMIT ?", new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            AdminLog l=new AdminLog(); l.ts=c.getLong(0); l.action=c.getString(1); l.detail=c.getString(2); list.add(l);
        }
        c.close(); return list;
    }

    // ── Generic setting ───────────────────────────────────────────────────────
    public String getSetting(String key, String def) {
        Cursor c = getReadableDatabase().rawQuery("SELECT value FROM admin_settings WHERE key=?", new String[]{key});
        String v=def; if (c.moveToFirst()) v=c.getString(0); c.close(); return v;
    }
    public void setSetting(String key, String value) {
        ContentValues cv = new ContentValues(); cv.put("key",key); cv.put("value",value);
        getWritableDatabase().insertWithOnConflict("admin_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
