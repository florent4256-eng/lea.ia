package com.flolov42.lea_v3.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class LeaFamilyDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "lea_family.db";
    private static final int    DB_VER  = 1;

    // Feature keys — correspond aux constantes LeaPlusDatabase
    public static final String F_QUESTS    = "QUESTS";
    public static final String F_ADVENTURE = "ADVENTURE";
    public static final String F_COINS     = "COINS";
    public static final String F_HABITS    = "HABITS";
    public static final String F_REPORT    = "REPORT";
    public static final String F_COMPANION = "COMPANION";
    public static final String F_LIFE_OS   = "LIFE_OS";
    public static final String F_STUDENT   = "STUDENT";
    public static final String F_LANGUAGE  = "LANGUAGE";
    public static final String F_SMART     = "SMART_NOTIF";
    public static final String F_CLOUD     = "CLOUD_SYNC";
    public static final String F_MARKET    = "MARKETPLACE";
    public static final String F_FAMILY    = "FAMILY";
    public static final String F_OMNI      = "OMNICHANNEL";
    public static final String F_STREAM    = "STREAMING";

    public static class ChildAccount {
        public String pseudo, accountType;
        public int age, screenLimitMin, bedtimeHour, wakeupHour;
    }
    public static class FeatureRule { public String feature; public boolean allowed; }
    public static class ScreenSession { public String dateKey; public int totalMin; }

    private static LeaFamilyDatabase instance;
    public static synchronized LeaFamilyDatabase get(Context ctx) {
        if (instance == null) instance = new LeaFamilyDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LeaFamilyDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VER); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS child_accounts (" +
            "pseudo TEXT PRIMARY KEY, age INTEGER DEFAULT 0, " +
            "account_type TEXT DEFAULT 'child', " +
            "screen_limit_min INTEGER DEFAULT 120, " +
            "bedtime_hour INTEGER DEFAULT 21, " +
            "wakeup_hour INTEGER DEFAULT 8, " +
            "created_ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS feature_access (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "pseudo TEXT, feature_key TEXT, allowed INTEGER DEFAULT 1, " +
            "UNIQUE(pseudo, feature_key))");

        db.execSQL("CREATE TABLE IF NOT EXISTS screen_sessions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "pseudo TEXT, date_key TEXT, duration_min INTEGER DEFAULT 0, " +
            "UNIQUE(pseudo, date_key))");

        db.execSQL("CREATE TABLE IF NOT EXISTS parental_events (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "ts INTEGER, pseudo TEXT, event_type TEXT, detail TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        // Migration douce — aucune table supprimée, aucune donnée perdue
        onCreate(db);
    }

    // ── Child accounts ────────────────────────────────────────────────────────
    public void addChildAccount(String pseudo, int age, String type, int screenLimitMin, int bedtimeHour, int wakeupHour) {
        ContentValues cv = new ContentValues();
        cv.put("pseudo", pseudo); cv.put("age", age); cv.put("account_type", type);
        cv.put("screen_limit_min", screenLimitMin); cv.put("bedtime_hour", bedtimeHour);
        cv.put("wakeup_hour", wakeupHour); cv.put("created_ts", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("child_accounts", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        // Appliquer les permissions par défaut selon le type
        seedDefaultFeatureAccess(pseudo, type);
    }
    public void removeChildAccount(String pseudo) {
        getWritableDatabase().delete("child_accounts", "pseudo=?", new String[]{pseudo});
        getWritableDatabase().delete("feature_access",  "pseudo=?", new String[]{pseudo});
        getWritableDatabase().delete("screen_sessions", "pseudo=?", new String[]{pseudo});
        getWritableDatabase().delete("parental_events", "pseudo=?", new String[]{pseudo});
    }
    public List<ChildAccount> getChildAccounts() {
        List<ChildAccount> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT pseudo,age,account_type,screen_limit_min,bedtime_hour,wakeup_hour FROM child_accounts ORDER BY created_ts", null);
        while (c.moveToNext()) {
            ChildAccount a=new ChildAccount();
            a.pseudo=c.getString(0); a.age=c.getInt(1); a.accountType=c.getString(2);
            a.screenLimitMin=c.getInt(3); a.bedtimeHour=c.getInt(4); a.wakeupHour=c.getInt(5);
            list.add(a);
        }
        c.close(); return list;
    }
    public ChildAccount getChildAccount(String pseudo) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT pseudo,age,account_type,screen_limit_min,bedtime_hour,wakeup_hour FROM child_accounts WHERE pseudo=?", new String[]{pseudo});
        ChildAccount a=null;
        if (c.moveToFirst()) {
            a=new ChildAccount(); a.pseudo=c.getString(0); a.age=c.getInt(1); a.accountType=c.getString(2);
            a.screenLimitMin=c.getInt(3); a.bedtimeHour=c.getInt(4); a.wakeupHour=c.getInt(5);
        }
        c.close(); return a;
    }
    public boolean childExists(String pseudo) {
        Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM child_accounts WHERE pseudo=?", new String[]{pseudo});
        boolean found = c.moveToFirst(); c.close(); return found;
    }
    public void updateScreenLimit(String pseudo, int limitMin) {
        ContentValues cv = new ContentValues(); cv.put("screen_limit_min", limitMin);
        getWritableDatabase().update("child_accounts", cv, "pseudo=?", new String[]{pseudo});
    }
    public void updateBedtime(String pseudo, int bedtimeHour, int wakeupHour) {
        ContentValues cv = new ContentValues(); cv.put("bedtime_hour", bedtimeHour); cv.put("wakeup_hour", wakeupHour);
        getWritableDatabase().update("child_accounts", cv, "pseudo=?", new String[]{pseudo});
    }

    // ── Feature access ────────────────────────────────────────────────────────
    private void seedDefaultFeatureAccess(String pseudo, String type) {
        boolean isChild = "child".equals(type);
        // Permissions par défaut
        String[] allowed  = {F_QUESTS, F_ADVENTURE, F_HABITS, F_COMPANION, F_LIFE_OS, F_LANGUAGE, F_REPORT};
        String[] blocked  = {F_MARKET, F_CLOUD, F_OMNI, F_STREAM, F_SMART, F_FAMILY, F_COINS};
        String[] teenOnly = {F_STUDENT, F_SMART};

        for (String f : allowed)  setFeatureAccess(pseudo, f, true);
        for (String f : blocked)  setFeatureAccess(pseudo, f, false);
        for (String f : teenOnly) setFeatureAccess(pseudo, f, !isChild);
    }
    public void setFeatureAccess(String pseudo, String feature, boolean allowed) {
        ContentValues cv = new ContentValues();
        cv.put("pseudo", pseudo); cv.put("feature_key", feature); cv.put("allowed", allowed ? 1 : 0);
        getWritableDatabase().insertWithOnConflict("feature_access", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public boolean isFeatureAllowed(String pseudo, String feature) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT allowed FROM feature_access WHERE pseudo=? AND feature_key=?", new String[]{pseudo, feature});
        int v = 1;  // par défaut autorisé si aucune règle
        if (c.moveToFirst()) v = c.getInt(0); c.close(); return v == 1;
    }
    public List<FeatureRule> getFeatureRules(String pseudo) {
        List<FeatureRule> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT feature_key,allowed FROM feature_access WHERE pseudo=?", new String[]{pseudo});
        while (c.moveToNext()) {
            FeatureRule r=new FeatureRule(); r.feature=c.getString(0); r.allowed=c.getInt(1)==1; list.add(r);
        }
        c.close(); return list;
    }

    // ── Screen time ───────────────────────────────────────────────────────────
    public void recordScreenTime(String pseudo, int minutes) {
        String dateKey = getTodayKey();
        ContentValues cv = new ContentValues();
        cv.put("pseudo", pseudo); cv.put("date_key", dateKey); cv.put("duration_min", minutes);
        int rows = getWritableDatabase().update("screen_sessions", cv, "pseudo=? AND date_key=?",
            new String[]{pseudo, dateKey});
        if (rows == 0) getWritableDatabase().insert("screen_sessions", null, cv);
        else {
            // Additionner
            getWritableDatabase().execSQL(
                "UPDATE screen_sessions SET duration_min = duration_min + ? WHERE pseudo=? AND date_key=?",
                new Object[]{minutes, pseudo, dateKey});
        }
    }
    public int getScreenUsageToday(String pseudo) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT duration_min FROM screen_sessions WHERE pseudo=? AND date_key=?",
            new String[]{pseudo, getTodayKey()});
        int min = 0; if (c.moveToFirst()) min = c.getInt(0); c.close(); return min;
    }
    public void resetScreenTimeToday(String pseudo) {
        ContentValues cv = new ContentValues(); cv.put("duration_min", 0);
        getWritableDatabase().update("screen_sessions", cv,
            "pseudo=? AND date_key=?", new String[]{pseudo, getTodayKey()});
    }

    // ── Events ────────────────────────────────────────────────────────────────
    public void logEvent(String pseudo, String type, String detail) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis()); cv.put("pseudo", pseudo);
        cv.put("event_type", type); cv.put("detail", detail);
        getWritableDatabase().insert("parental_events", null, cv);
    }
    public List<String> getRecentEvents(String pseudo, int limit) {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT event_type,detail FROM parental_events WHERE pseudo=? ORDER BY ts DESC LIMIT ?",
            new String[]{pseudo, String.valueOf(limit)});
        while (c.moveToNext()) list.add(c.getString(0) + ": " + c.getString(1));
        c.close(); return list;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getTodayKey() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return sdf.format(new java.util.Date());
    }
}
