package com.flolov42.lea_v3.bixby;

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

public class LeaNovaDataStore extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_bixby.db";
    private static final int    DB_VERSION = 2; // v2 : is_favorite + auto-purge 30j

    private static LeaNovaDataStore instance;

    public static synchronized LeaNovaDataStore get(Context ctx) {
        if (instance == null) instance = new LeaNovaDataStore(ctx.getApplicationContext());
        return instance;
    }

    private LeaNovaDataStore(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "ts INTEGER," +
            "user_text TEXT," +
            "lea_text TEXT," +
            "sentiment INTEGER," +
            "is_favorite INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE routines (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT," +
            "trigger_time TEXT," +
            "trigger_days TEXT," +
            "actions TEXT," +
            "active INTEGER DEFAULT 1," +
            "last_run INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE habits (" +
            "key TEXT PRIMARY KEY," +
            "value TEXT," +
            "count INTEGER DEFAULT 1," +
            "last_seen INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migration douce : on ajoute la colonne sans perdre les données existantes
            try { db.execSQL("ALTER TABLE conversations ADD COLUMN is_favorite INTEGER DEFAULT 0"); }
            catch (Exception ignored) {}
        }
    }

    // ── CONVERSATIONS ─────────────────────────────────────────────────────────

    public void saveConversation(String userText, String leaText, int sentiment) {
        ContentValues cv = new ContentValues();
        cv.put("ts",          System.currentTimeMillis());
        cv.put("user_text",   userText);
        cv.put("lea_text",    leaText);
        cv.put("sentiment",   sentiment);
        cv.put("is_favorite", 0);
        getWritableDatabase().insert("conversations", null, cv);
        purgeOldConversations(); // Nettoyage automatique à chaque enregistrement
    }

    /** Toutes les conversations pour l'historique, les plus récentes en premier. */
    public List<ConversationEntry> getAllConversations() {
        List<ConversationEntry> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id, ts, user_text, lea_text, is_favorite FROM conversations ORDER BY ts DESC", null);
        while (c.moveToNext()) {
            ConversationEntry e = new ConversationEntry();
            e.id         = c.getLong(0);
            e.ts         = c.getLong(1);
            e.userText   = c.getString(2);
            e.leaText    = c.getString(3);
            e.isFavorite = c.getInt(4) == 1;
            list.add(e);
        }
        c.close();
        return list;
    }

    /** Pour la rétrocompatibilité avec LeaMemoryManager / LeaRecommendationEngine. */
    public List<String[]> getRecentConversations(int limit) {
        List<String[]> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT user_text, lea_text FROM conversations ORDER BY ts DESC LIMIT ?",
            new String[]{ String.valueOf(limit) });
        while (c.moveToNext()) list.add(new String[]{ c.getString(0), c.getString(1) });
        c.close();
        return list;
    }

    /** Étoile / dés-étoile une conversation. */
    public void setFavorite(long id, boolean favorite) {
        ContentValues cv = new ContentValues();
        cv.put("is_favorite", favorite ? 1 : 0);
        getWritableDatabase().update("conversations", cv, "id=?", new String[]{ String.valueOf(id) });
    }

    /** Supprime une conversation manuellement. */
    public void deleteConversation(long id) {
        getWritableDatabase().delete("conversations", "id=?", new String[]{ String.valueOf(id) });
    }

    /** Supprime les conversations > 30 jours sauf les favoris. */
    private void purgeOldConversations() {
        long cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        getWritableDatabase().delete("conversations",
            "ts < ? AND is_favorite = 0", new String[]{ String.valueOf(cutoff) });
    }

    // ── ROUTINES ──────────────────────────────────────────────────────────────

    public long saveRoutine(String name, String time, String days, String actionsJson) {
        ContentValues cv = new ContentValues();
        cv.put("name",         name);
        cv.put("trigger_time", time);
        cv.put("trigger_days", days);
        cv.put("actions",      actionsJson);
        return getWritableDatabase().insert("routines", null, cv);
    }

    public List<Routine> getAllRoutines() {
        List<Routine> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,trigger_time,trigger_days,actions,active FROM routines ORDER BY id DESC", null);
        while (c.moveToNext()) {
            Routine r = new Routine();
            r.id          = c.getLong(0);
            r.name        = c.getString(1);
            r.triggerTime = c.getString(2);
            r.triggerDays = c.getString(3);
            r.actionsJson = c.getString(4);
            r.active      = c.getInt(5) == 1;
            list.add(r);
        }
        c.close();
        return list;
    }

    public void setRoutineActive(long id, boolean active) {
        ContentValues cv = new ContentValues();
        cv.put("active", active ? 1 : 0);
        getWritableDatabase().update("routines", cv, "id=?", new String[]{ String.valueOf(id) });
    }

    public void deleteRoutine(long id) {
        getWritableDatabase().delete("routines", "id=?", new String[]{ String.valueOf(id) });
    }

    public Routine getRoutine(long id) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,trigger_time,trigger_days,actions,active FROM routines WHERE id=?",
            new String[]{ String.valueOf(id) });
        if (c.moveToFirst()) {
            Routine r = new Routine();
            r.id = c.getLong(0); r.name = c.getString(1);
            r.triggerTime = c.getString(2); r.triggerDays = c.getString(3);
            r.actionsJson = c.getString(4); r.active = c.getInt(5) == 1;
            c.close(); return r;
        }
        c.close(); return null;
    }

    // ── HABITS ────────────────────────────────────────────────────────────────

    public void incrementHabit(String key, String value) {
        Cursor c = getReadableDatabase().rawQuery("SELECT count FROM habits WHERE key=?", new String[]{ key });
        if (c.moveToFirst()) {
            int count = c.getInt(0) + 1; c.close();
            ContentValues cv = new ContentValues();
            cv.put("count", count); cv.put("value", value); cv.put("last_seen", System.currentTimeMillis());
            getWritableDatabase().update("habits", cv, "key=?", new String[]{ key });
        } else {
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("key", key); cv.put("value", value); cv.put("count", 1); cv.put("last_seen", System.currentTimeMillis());
            getWritableDatabase().insert("habits", null, cv);
        }
    }

    public List<String[]> getTopHabits(int limit) {
        List<String[]> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT key,value FROM habits ORDER BY count DESC LIMIT ?", new String[]{ String.valueOf(limit) });
        while (c.moveToNext()) list.add(new String[]{ c.getString(0), c.getString(1) });
        c.close();
        return list;
    }

    // ── DATA CLASSES ──────────────────────────────────────────────────────────

    public static class ConversationEntry {
        public long    id;
        public long    ts;
        public String  userText;
        public String  leaText;
        public boolean isFavorite;

        /** Résumé de 50 caractères max pour l'affichage dans la liste. */
        public String summary() {
            if (userText == null || userText.isEmpty()) return "(vide)";
            return userText.length() > 50 ? userText.substring(0, 50) + "…" : userText;
        }
    }

    public static class Routine {
        public long    id;
        public String  name;
        public String  triggerTime;
        public String  triggerDays;
        public String  actionsJson;
        public boolean active;

        public String daysLabel() {
            if (triggerDays == null) return "";
            if (triggerDays.equals("1,2,3,4,5")) return "Lun–Ven";
            if (triggerDays.equals("1,2,3,4,5,6,7")) return "Tous les jours";
            if (triggerDays.equals("6,7")) return "Week-end";
            return triggerDays;
        }
    }
}
