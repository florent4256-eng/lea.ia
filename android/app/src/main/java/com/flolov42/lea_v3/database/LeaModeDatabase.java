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

public class LeaModeDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_modes.db";
    private static final int    DB_VERSION = 1;

    // ── Mode IDs ──────────────────────────────────────────────────────────────
    public static final String DUPLICATE    = "DUPLICATE";
    public static final String MENTAL_HEALTH= "MENTAL_HEALTH";
    public static final String INTERVIEW    = "INTERVIEW";
    public static final String VOICE_BIO    = "VOICE_BIO";
    public static final String FUTURE       = "FUTURE";
    public static final String DREAM        = "DREAM";
    public static final String ALTER_EGO    = "ALTER_EGO";
    public static final String NEGOTIATION  = "NEGOTIATION";
    public static final String RELATIONS    = "RELATIONS";
    public static final String CREATIVE     = "CREATIVE";

    private static LeaModeDatabase instance;
    public static synchronized LeaModeDatabase get(Context ctx) {
        if (instance == null) instance = new LeaModeDatabase(ctx.getApplicationContext());
        return instance;
    }

    private LeaModeDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS modes (" +
            "id TEXT PRIMARY KEY, enabled INTEGER DEFAULT 0, config TEXT, last_run INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS mode_logs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, mode_id TEXT, ts INTEGER, msg TEXT)");
        // Duplicate mode — learned style patterns
        db.execSQL("CREATE TABLE IF NOT EXISTS style_patterns (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, pattern TEXT, weight REAL, ts INTEGER)");
        // Mental health — mood history
        db.execSQL("CREATE TABLE IF NOT EXISTS mood_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, score INTEGER, note TEXT)");
        // Interview sessions
        db.execSQL("CREATE TABLE IF NOT EXISTS interview_sessions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, job TEXT, score INTEGER, ts INTEGER, feedback TEXT)");
        // Dream journal
        db.execSQL("CREATE TABLE IF NOT EXISTS dreams (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, content TEXT, emotion TEXT, interpretation TEXT)");
        // Relations
        db.execSQL("CREATE TABLE IF NOT EXISTS relations (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, last_contact INTEGER, " +
            "frequency_days INTEGER, health_score INTEGER, sentiment TEXT)");
        // Creative works
        db.execSQL("CREATE TABLE IF NOT EXISTS creative_works (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, title TEXT, content TEXT, ts INTEGER)");
        // Negotiations
        db.execSQL("CREATE TABLE IF NOT EXISTS negotiations (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, service TEXT, saved_amount REAL, ts INTEGER, status TEXT)");
        // Future predictions
        db.execSQL("CREATE TABLE IF NOT EXISTS predictions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, prediction TEXT, confidence INTEGER, " +
            "target_date INTEGER, correct INTEGER DEFAULT -1, ts INTEGER)");

        seedModes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        // Ne pas toucher "modes" — contient les états activé/désactivé de l'utilisateur
        // CREATE TABLE IF NOT EXISTS → tables existantes conservées, nouvelles créées
        onCreate(db);
        // seedModes avec CONFLICT_IGNORE → ajoute nouveaux modes sans écraser existants
    }

    private void seedModes(SQLiteDatabase db) {
        String[] ids = { DUPLICATE, MENTAL_HEALTH, INTERVIEW, VOICE_BIO,
                         FUTURE, DREAM, ALTER_EGO, NEGOTIATION, RELATIONS, CREATIVE };
        for (String id : ids) {
            ContentValues cv = new ContentValues();
            cv.put("id", id);
            cv.put("enabled", 0);
            db.insertWithOnConflict("modes", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ── Mode state ────────────────────────────────────────────────────────────
    public boolean isEnabled(String modeId) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT enabled FROM modes WHERE id=?", new String[]{modeId});
        boolean r = false;
        if (c.moveToFirst()) r = c.getInt(0) == 1;
        c.close(); return r;
    }

    public void setEnabled(String modeId, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put("enabled", enabled ? 1 : 0);
        getWritableDatabase().update("modes", cv, "id=?", new String[]{modeId});
    }

    public void setConfig(String modeId, String config) {
        ContentValues cv = new ContentValues();
        cv.put("config", config);
        getWritableDatabase().update("modes", cv, "id=?", new String[]{modeId});
    }

    public String getConfig(String modeId) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT config FROM modes WHERE id=?", new String[]{modeId});
        String r = null;
        if (c.moveToFirst()) r = c.getString(0);
        c.close(); return r;
    }

    // ── Logs ──────────────────────────────────────────────────────────────────
    public void log(String modeId, String msg) {
        ContentValues cv = new ContentValues();
        cv.put("mode_id", modeId);
        cv.put("ts", System.currentTimeMillis());
        cv.put("msg", msg);
        getWritableDatabase().insert("mode_logs", null, cv);
        // Keep last 100 logs per mode
        getWritableDatabase().execSQL(
            "DELETE FROM mode_logs WHERE mode_id=? AND id NOT IN " +
            "(SELECT id FROM mode_logs WHERE mode_id=? ORDER BY ts DESC LIMIT 100)",
            new Object[]{modeId, modeId});
    }

    public static class LogRow { public long ts; public String msg; }

    public List<LogRow> getLogs(String modeId, int limit) {
        List<LogRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT ts,msg FROM mode_logs WHERE mode_id=? ORDER BY ts DESC LIMIT ?",
            new String[]{modeId, String.valueOf(limit)});
        while (c.moveToNext()) {
            LogRow r = new LogRow(); r.ts = c.getLong(0); r.msg = c.getString(1); list.add(r);
        }
        c.close(); return list;
    }

    // ── Mood history ──────────────────────────────────────────────────────────
    public void addMood(int score, String note) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("score", score); cv.put("note", note);
        getWritableDatabase().insert("mood_history", null, cv);
    }

    public int getAverageMoodLastWeek() {
        long weekAgo = System.currentTimeMillis() - 7L*24*3600*1000;
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT AVG(score) FROM mood_history WHERE ts>?", new String[]{String.valueOf(weekAgo)});
        int avg = 50;
        if (c.moveToFirst() && !c.isNull(0)) avg = (int) c.getDouble(0);
        c.close(); return avg;
    }

    // ── Dreams ────────────────────────────────────────────────────────────────
    public long addDream(String content, String emotion, String interpretation) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("content", content); cv.put("emotion", emotion); cv.put("interpretation", interpretation);
        return getWritableDatabase().insert("dreams", null, cv);
    }

    public static class DreamRow { public long id, ts; public String content, emotion, interpretation; }

    public List<DreamRow> getDreams(int limit) {
        List<DreamRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,ts,content,emotion,interpretation FROM dreams ORDER BY ts DESC LIMIT ?",
            new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            DreamRow r = new DreamRow();
            r.id=c.getLong(0); r.ts=c.getLong(1); r.content=c.getString(2);
            r.emotion=c.getString(3); r.interpretation=c.getString(4); list.add(r);
        }
        c.close(); return list;
    }

    public int getDreamCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM dreams", null);
        int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close(); return count;
    }

    // ── Relations ─────────────────────────────────────────────────────────────
    public void upsertRelation(String name, String phone, long lastContact, int freqDays, int health, String sentiment) {
        ContentValues cv = new ContentValues();
        cv.put("name", name); cv.put("phone", phone); cv.put("last_contact", lastContact);
        cv.put("frequency_days", freqDays); cv.put("health_score", health); cv.put("sentiment", sentiment);
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id FROM relations WHERE phone=?", new String[]{phone});
        if (c.moveToFirst()) {
            getWritableDatabase().update("relations", cv, "phone=?", new String[]{phone});
        } else {
            getWritableDatabase().insert("relations", null, cv);
        }
        c.close();
    }

    public static class RelationRow {
        public long id, lastContact;
        public String name, phone, sentiment;
        public int freqDays, healthScore;
    }

    public List<RelationRow> getRelations() {
        List<RelationRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,phone,last_contact,frequency_days,health_score,sentiment FROM relations ORDER BY health_score ASC", null);
        while (c.moveToNext()) {
            RelationRow r = new RelationRow();
            r.id=c.getLong(0); r.name=c.getString(1); r.phone=c.getString(2);
            r.lastContact=c.getLong(3); r.freqDays=c.getInt(4); r.healthScore=c.getInt(5); r.sentiment=c.getString(6);
            list.add(r);
        }
        c.close(); return list;
    }

    // ── Negotiations ──────────────────────────────────────────────────────────
    public void addNegotiation(String service, double savedAmount, String status) {
        ContentValues cv = new ContentValues();
        cv.put("service", service); cv.put("saved_amount", savedAmount);
        cv.put("ts", System.currentTimeMillis()); cv.put("status", status);
        getWritableDatabase().insert("negotiations", null, cv);
    }

    public double getTotalSaved() {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT SUM(saved_amount) FROM negotiations WHERE status='success'", null);
        double total = 0;
        if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
        c.close(); return total;
    }

    // ── Style patterns ────────────────────────────────────────────────────────
    public void upsertStylePattern(String type, String pattern, double weight) {
        ContentValues cv = new ContentValues();
        cv.put("type", type); cv.put("pattern", pattern);
        cv.put("weight", weight); cv.put("ts", System.currentTimeMillis());
        int rows = getWritableDatabase().update("style_patterns", cv, "type=?", new String[]{type});
        if (rows == 0) getWritableDatabase().insert("style_patterns", null, cv);
    }

    public int getStylePatternCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT type) FROM style_patterns", null);
        int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close(); return count;
    }

    public String getStylePattern(String type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT pattern FROM style_patterns WHERE type=? ORDER BY ts DESC LIMIT 1",
            new String[]{type});
        String val = null; if (c.moveToFirst()) val = c.getString(0); c.close(); return val;
    }

    // ── Creative works ────────────────────────────────────────────────────────
    public long addCreativeWork(String type, String title, String content) {
        ContentValues cv = new ContentValues();
        cv.put("type", type); cv.put("title", title);
        cv.put("content", content); cv.put("ts", System.currentTimeMillis());
        return getWritableDatabase().insert("creative_works", null, cv);
    }

    public static class CreativeRow { public long id, ts; public String type, title, content; }

    public List<CreativeRow> getCreativeWorks(String type, int limit) {
        List<CreativeRow> list = new ArrayList<>();
        Cursor c;
        if (type != null) {
            c = getReadableDatabase().rawQuery(
                "SELECT id,type,title,content,ts FROM creative_works WHERE type=? ORDER BY ts DESC LIMIT ?",
                new String[]{type, String.valueOf(limit)});
        } else {
            c = getReadableDatabase().rawQuery(
                "SELECT id,type,title,content,ts FROM creative_works ORDER BY ts DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
        }
        while (c.moveToNext()) {
            CreativeRow r = new CreativeRow();
            r.id=c.getLong(0); r.type=c.getString(1); r.title=c.getString(2);
            r.content=c.getString(3); r.ts=c.getLong(4); list.add(r);
        }
        c.close(); return list;
    }

    // ── Interview sessions ────────────────────────────────────────────────────
    public void addInterviewSession(String job, int avgScore, String feedback) {
        ContentValues cv = new ContentValues();
        cv.put("job", job); cv.put("score", avgScore);
        cv.put("ts", System.currentTimeMillis()); cv.put("feedback", feedback);
        getWritableDatabase().insert("interview_sessions", null, cv);
    }

    public static class InterviewSessionRow {
        public long id, ts; public String job, feedback; public int score;
    }

    public List<InterviewSessionRow> getInterviewSessions(int limit) {
        List<InterviewSessionRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,job,score,ts,feedback FROM interview_sessions ORDER BY ts DESC LIMIT ?",
            new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            InterviewSessionRow r = new InterviewSessionRow();
            r.id=c.getLong(0); r.job=c.getString(1); r.score=c.getInt(2);
            r.ts=c.getLong(3); r.feedback=c.getString(4); list.add(r);
        }
        c.close(); return list;
    }

    // ── Mood history ──────────────────────────────────────────────────────────
    public static class MoodRow { public long ts; public int score; public String note; }

    public List<MoodRow> getMoodHistory(int limit) {
        List<MoodRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT ts,score,note FROM mood_history ORDER BY ts DESC LIMIT ?",
            new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            MoodRow r = new MoodRow();
            r.ts=c.getLong(0); r.score=c.getInt(1); r.note=c.getString(2); list.add(r);
        }
        c.close(); return list;
    }

    // ── Predictions ───────────────────────────────────────────────────────────
    public void addPrediction(String type, String prediction, int confidence, long targetDate) {
        ContentValues cv = new ContentValues();
        cv.put("type", type); cv.put("prediction", prediction);
        cv.put("confidence", confidence); cv.put("target_date", targetDate);
        cv.put("ts", System.currentTimeMillis());
        getWritableDatabase().insert("predictions", null, cv);
    }

    public List<String> getActivePredictions() {
        List<String> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT prediction, confidence FROM predictions WHERE target_date>? AND correct=-1 ORDER BY confidence DESC LIMIT 5",
            new String[]{String.valueOf(now)});
        while (c.moveToNext()) {
            list.add(c.getString(0) + " (" + c.getInt(1) + "% confiance)");
        }
        c.close(); return list;
    }

    public void purgePastPredictions() {
        long now = System.currentTimeMillis();
        getWritableDatabase().delete("predictions", "target_date < ?", new String[]{String.valueOf(now)});
    }
}
