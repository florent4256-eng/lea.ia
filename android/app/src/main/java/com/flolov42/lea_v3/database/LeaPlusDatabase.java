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

public class LeaPlusDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_plus.db";
    private static final int    DB_VERSION = 3;

    // ── Feature IDs ──────────────────────────────────────────────────────────
    public static final String QUESTS      = "QUESTS";
    public static final String ADVENTURE   = "ADVENTURE";
    public static final String COINS       = "COINS";
    public static final String HABITS      = "HABITS";
    public static final String REPORT      = "REPORT";
    public static final String COMPANION   = "COMPANION";
    public static final String LIFE_OS     = "LIFE_OS";
    public static final String STUDENT     = "STUDENT";
    public static final String LANGUAGE    = "LANGUAGE";
    public static final String SMART_NOTIF = "SMART_NOTIF";
    public static final String CLOUD_SYNC  = "CLOUD_SYNC";
    public static final String MARKETPLACE = "MARKETPLACE";
    public static final String FAMILY      = "FAMILY";
    public static final String OMNICHANNEL = "OMNICHANNEL";
    public static final String STREAMING   = "STREAMING";

    private static LeaPlusDatabase instance;
    public static synchronized LeaPlusDatabase get(Context ctx) {
        if (instance == null) instance = new LeaPlusDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LeaPlusDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class LogRow       { public long ts; public String msg; }
    public static class QuestRow     { public String id,title,desc,category,difficulty,status; public int xp,coins,target,progress; }
    public static class CharStats    { public int level,xp,xpNext,hp,bossDefeated,totalTasks; public String world; }
    public static class HabitRow     { public String id,name,frequency; public int streak,bestStreak,reminderHour; public long lastCheck; }

    public static class CompanionMemory { public long id; public String content,emotion; }
    public static class ScheduleItem    { public long id; public String title,description; public int hour,minute; }
    public static class SubjectRow      { public String name; public double coef,average; }
    public static class VocabRow        { public String word,translation,phonetic,example; }
    public static class LanguageProgress{ public int wordsLearned; public String fluencyLevel; }
    public static class NotifBatch      { public long id; public String app,content,category; public int priority; }
    public static class MarketSkill     { public String id,name,author,description,category,review; public float rating; public int price; public boolean hasUpdate; }
    public static class FamilyMember    { public String id,name,profile; public int age; public boolean isChild,wasOnline; }
    public static class DeviceRow       { public String id,name,type,ip; public boolean wasOnline; public long lastSeen; }
    public static class StreamSession   { public long id; public String platform,title; public int durationMin; }
    public static class SkillRow        { public String id,name,description,author; public float stars; public int downloads,priceCoins,installed; }
    public static class FamilyMemberRow { public String id,name,role; public int age,screenLimitMin; public long lastSeen; }

    // ── Schema ────────────────────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS features (id TEXT PRIMARY KEY, enabled INTEGER DEFAULT 0, config TEXT, last_run INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS feature_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, feature_id TEXT, ts INTEGER, msg TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS quests (id TEXT PRIMARY KEY, title TEXT, description TEXT, category TEXT, difficulty TEXT, xp_reward INTEGER, coin_reward INTEGER, target INTEGER, progress INTEGER DEFAULT 0, status TEXT DEFAULT 'available', created_ts INTEGER, completed_ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS character_stats (id INTEGER PRIMARY KEY, level INTEGER DEFAULT 1, xp INTEGER DEFAULT 0, xp_to_next INTEGER DEFAULT 100, hp INTEGER DEFAULT 100, world TEXT DEFAULT 'Procrastination Valley', boss_defeated INTEGER DEFAULT 0, total_tasks INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS coins (id INTEGER PRIMARY KEY, balance INTEGER DEFAULT 0, total_earned INTEGER DEFAULT 0, last_daily_login INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS coin_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, reason TEXT, amount INTEGER, balance_after INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS habits (id TEXT PRIMARY KEY, name TEXT, frequency TEXT, reminder_hour INTEGER DEFAULT 9, streak INTEGER DEFAULT 0, best_streak INTEGER DEFAULT 0, last_check INTEGER DEFAULT 0, created_ts INTEGER, active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS habit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, habit_id TEXT, ts INTEGER, completed INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS annual_stats (id INTEGER PRIMARY KEY AUTOINCREMENT, year INTEGER, stat_key TEXT, stat_value TEXT, UNIQUE(year, stat_key))");

        db.execSQL("CREATE TABLE IF NOT EXISTS companion_memories (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, content TEXT, emotion TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS life_schedule (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, hour INTEGER DEFAULT 9, minute INTEGER DEFAULT 0, notified INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS life_feedback (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, activity TEXT, satisfaction INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS student_subjects (name TEXT PRIMARY KEY, coef REAL DEFAULT 1, average REAL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS student_grades (id INTEGER PRIMARY KEY AUTOINCREMENT, subject_name TEXT, grade REAL, weight REAL, ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS student_quizzes (id INTEGER PRIMARY KEY AUTOINCREMENT, subject TEXT, topic TEXT, question_count INTEGER, ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS language_vocab (id INTEGER PRIMARY KEY AUTOINCREMENT, language TEXT, word TEXT, translation TEXT, phonetic TEXT, example TEXT, learned INTEGER DEFAULT 0, ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS language_progress (language TEXT PRIMARY KEY, words_learned INTEGER DEFAULT 0, fluency_level TEXT DEFAULT 'Débutant A1', last_session INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS notif_batch (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, app TEXT, content TEXT, category TEXT, priority INTEGER, delivered INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS notif_prefs (app TEXT PRIMARY KEY, priority INTEGER DEFAULT 5)");

        db.execSQL("CREATE TABLE IF NOT EXISTS sync_history (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, type TEXT, status TEXT, message TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS marketplace_skills (id TEXT PRIMARY KEY, name TEXT, description TEXT, author TEXT, category TEXT, rating REAL DEFAULT 0, price INTEGER DEFAULT 0, installed INTEGER DEFAULT 0, installed_ts INTEGER DEFAULT 0, user_rating REAL DEFAULT 0, review TEXT DEFAULT '', has_update INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS family_members (id TEXT PRIMARY KEY, name TEXT, age INTEGER, is_child INTEGER DEFAULT 0, pin TEXT DEFAULT '', profile TEXT DEFAULT 'adult')");
        db.execSQL("CREATE TABLE IF NOT EXISTS family_usage (id INTEGER PRIMARY KEY AUTOINCREMENT, member_id TEXT, ts INTEGER, minutes INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS devices (id TEXT PRIMARY KEY, name TEXT, type TEXT, ip TEXT DEFAULT '', was_online INTEGER DEFAULT 0, last_seen INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS stream_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, platform TEXT, title TEXT, category TEXT, started_ts INTEGER, ended_ts INTEGER DEFAULT 0, duration_min INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS stream_clips (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER, moment TEXT, timestamp_sec INTEGER, ts INTEGER)");

        seedDefaults(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o >= 2) {
            // Migration douce v2→v3 : aucune table supprimée, aucune donnée perdue.
            // onCreate() est idempotent (CREATE TABLE IF NOT EXISTS) et seedDefaults() utilise CONFLICT_IGNORE.
            onCreate(db);
            return;
        }
        // Migration depuis v1 : schéma incompatible, reconstruction complète inévitable.
        String[] tables = {"features","feature_logs","quests","character_stats","coins","coin_transactions",
            "habits","habit_logs","annual_stats","companion_memories","life_schedule","life_feedback",
            "student_subjects","student_grades","student_quizzes","language_vocab","language_progress",
            "notif_batch","notif_prefs","sync_history","marketplace_skills","family_members","family_usage",
            "devices","stream_sessions","stream_clips"};
        for (String t : tables) db.execSQL("DROP TABLE IF EXISTS " + t);
        onCreate(db);
    }

    private void seedDefaults(SQLiteDatabase db) {
        String[] ids = {QUESTS,ADVENTURE,COINS,HABITS,REPORT,COMPANION,LIFE_OS,
                        STUDENT,LANGUAGE,SMART_NOTIF,CLOUD_SYNC,MARKETPLACE,FAMILY,OMNICHANNEL,STREAMING};
        for (String id : ids) {
            ContentValues cv = new ContentValues();
            cv.put("id", id); cv.put("enabled", 0);
            db.insertWithOnConflict("features", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
        db.execSQL("INSERT OR IGNORE INTO character_stats(id) VALUES(1)");
        db.execSQL("INSERT OR IGNORE INTO coins(id) VALUES(1)");

        String[][] skills = {
            {"crypto_tracker","Crypto Tracker","Prix BTC/ETH/BNB","LéaLabs","Finance","4.8","0"},
            {"plant_care","Plant Care AI","Rappels arrosage + diagnostic","LéaLabs","Lifestyle","4.6","0"},
            {"movie_reco","Movie Recommender","Films selon ton humeur","LéaLabs","Divertissement","4.7","0"},
            {"recipe_chef","Chef LÉA","Recettes selon ingrédients","LéaLabs","Cuisine","4.9","0"},
        };
        for (String[] s : skills) {
            ContentValues cv = new ContentValues();
            cv.put("id",s[0]); cv.put("name",s[1]); cv.put("description",s[2]);
            cv.put("author",s[3]); cv.put("category",s[4]);
            cv.put("rating", Float.parseFloat(s[5]));
            cv.put("price", Integer.parseInt(s[6]));
            db.insertWithOnConflict("marketplace_skills", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ── Feature state ─────────────────────────────────────────────────────────
    public boolean isEnabled(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT enabled FROM features WHERE id=?", new String[]{id});
        boolean r=false; if (c.moveToFirst()) r=c.getInt(0)==1; c.close(); return r;
    }
    public void setEnabled(String id, boolean enabled) {
        ContentValues cv = new ContentValues(); cv.put("enabled", enabled?1:0);
        getWritableDatabase().update("features", cv, "id=?", new String[]{id});
    }

    // ── Logs ─────────────────────────────────────────────────────────────────
    public void log(String featureId, String msg) {
        ContentValues cv = new ContentValues();
        cv.put("feature_id",featureId); cv.put("ts",System.currentTimeMillis()); cv.put("msg",msg);
        getWritableDatabase().insert("feature_logs", null, cv);
        getWritableDatabase().execSQL(
            "DELETE FROM feature_logs WHERE feature_id=? AND id NOT IN (SELECT id FROM feature_logs WHERE feature_id=? ORDER BY ts DESC LIMIT 50)",
            new Object[]{featureId, featureId});
    }
    public List<LogRow> getLogs(String featureId, int limit) {
        List<LogRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT ts,msg FROM feature_logs WHERE feature_id=? ORDER BY ts DESC LIMIT ?",
            new String[]{featureId, String.valueOf(limit)});
        while (c.moveToNext()) { LogRow r=new LogRow(); r.ts=c.getLong(0); r.msg=c.getString(1); list.add(r); }
        c.close(); return list;
    }

    // ── Quests ────────────────────────────────────────────────────────────────
    public void insertQuest(String id, String title, String desc, String category, String difficulty, int xp, int coins, int target) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("title",title); cv.put("description",desc);
        cv.put("category",category); cv.put("difficulty",difficulty);
        cv.put("xp_reward",xp); cv.put("coin_reward",coins); cv.put("target",target);
        cv.put("created_ts",System.currentTimeMillis()); cv.put("status","available");
        getWritableDatabase().insertWithOnConflict("quests", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
    public void updateQuestProgress(String id, int progress, String status) {
        ContentValues cv = new ContentValues(); cv.put("progress",progress); cv.put("status",status);
        if ("completed".equals(status)) cv.put("completed_ts", System.currentTimeMillis());
        getWritableDatabase().update("quests", cv, "id=?", new String[]{id});
    }
    public List<QuestRow> getQuests(String status) {
        List<QuestRow> list = new ArrayList<>();
        String where = status!=null ? "status=?" : "1=1";
        String[] args = status!=null ? new String[]{status} : new String[0];
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,title,description,category,difficulty,status,xp_reward,coin_reward,target,progress FROM quests WHERE "+where+" ORDER BY difficulty", args);
        while (c.moveToNext()) {
            QuestRow r=new QuestRow();
            r.id=c.getString(0); r.title=c.getString(1); r.desc=c.getString(2);
            r.category=c.getString(3); r.difficulty=c.getString(4); r.status=c.getString(5);
            r.xp=c.getInt(6); r.coins=c.getInt(7); r.target=c.getInt(8); r.progress=c.getInt(9);
            list.add(r);
        }
        c.close(); return list;
    }

    // ── Character ─────────────────────────────────────────────────────────────
    public CharStats getCharStats() {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT level,xp,xp_to_next,hp,world,boss_defeated,total_tasks FROM character_stats WHERE id=1", null);
        CharStats s=new CharStats(); s.level=1; s.xp=0; s.xpNext=100; s.hp=100; s.world="Procrastination Valley";
        if (c.moveToFirst()) {
            s.level=c.getInt(0); s.xp=c.getInt(1); s.xpNext=c.getInt(2); s.hp=c.getInt(3);
            s.world=c.getString(4); s.bossDefeated=c.getInt(5); s.totalTasks=c.getInt(6);
        }
        c.close(); return s;
    }
    public void updateChar(int level, int xp, int xpNext, int hp, String world, int bossDefeated, int totalTasks) {
        ContentValues cv = new ContentValues();
        cv.put("level",level); cv.put("xp",xp); cv.put("xp_to_next",xpNext); cv.put("hp",hp);
        cv.put("world",world); cv.put("boss_defeated",bossDefeated); cv.put("total_tasks",totalTasks);
        getWritableDatabase().update("character_stats", cv, "id=1", null);
    }

    // ── Coins ─────────────────────────────────────────────────────────────────
    public int getCoinBalance() {
        Cursor c = getReadableDatabase().rawQuery("SELECT balance FROM coins WHERE id=1", null);
        int b=0; if (c.moveToFirst()) b=c.getInt(0); c.close(); return b;
    }
    public void addCoins(int amount, String reason) {
        int newBal = getCoinBalance() + amount;
        ContentValues cv = new ContentValues(); cv.put("balance",newBal); cv.put("total_earned",newBal);
        getWritableDatabase().update("coins", cv, "id=1", null);
        ContentValues tx = new ContentValues();
        tx.put("ts",System.currentTimeMillis()); tx.put("reason",reason); tx.put("amount",amount); tx.put("balance_after",newBal);
        getWritableDatabase().insert("coin_transactions", null, tx);
    }
    public boolean spendCoins(int amount, String item) {
        int bal = getCoinBalance(); if (bal < amount) return false;
        ContentValues cv = new ContentValues(); cv.put("balance", bal-amount);
        getWritableDatabase().update("coins", cv, "id=1", null);
        ContentValues tx = new ContentValues();
        tx.put("ts",System.currentTimeMillis()); tx.put("reason","Achat: "+item); tx.put("amount",-amount); tx.put("balance_after",bal-amount);
        getWritableDatabase().insert("coin_transactions", null, tx);
        return true;
    }
    public long getLastDailyLogin() {
        Cursor c = getReadableDatabase().rawQuery("SELECT last_daily_login FROM coins WHERE id=1", null);
        long v=0; if (c.moveToFirst()) v=c.getLong(0); c.close(); return v;
    }
    public void setLastDailyLogin(long ts) {
        ContentValues cv = new ContentValues(); cv.put("last_daily_login",ts);
        getWritableDatabase().update("coins", cv, "id=1", null);
    }

    // ── Habits ────────────────────────────────────────────────────────────────
    public long insertHabit(String name, String frequency, int reminderHour) {
        ContentValues cv = new ContentValues();
        cv.put("id","habit_"+System.currentTimeMillis()); cv.put("name",name);
        cv.put("frequency",frequency); cv.put("reminder_hour",reminderHour); cv.put("created_ts",System.currentTimeMillis());
        return getWritableDatabase().insert("habits", null, cv);
    }
    public List<HabitRow> getActiveHabits() {
        List<HabitRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,frequency,streak,best_streak,reminder_hour,last_check FROM habits WHERE active=1", null);
        while (c.moveToNext()) {
            HabitRow r=new HabitRow(); r.id=c.getString(0); r.name=c.getString(1); r.frequency=c.getString(2);
            r.streak=c.getInt(3); r.bestStreak=c.getInt(4); r.reminderHour=c.getInt(5); r.lastCheck=c.getLong(6);
            list.add(r);
        }
        c.close(); return list;
    }
    public void checkInHabit(String habitId) {
        long now = System.currentTimeMillis();
        Cursor c = getReadableDatabase().rawQuery("SELECT streak,best_streak,last_check FROM habits WHERE id=?", new String[]{habitId});
        if (!c.moveToFirst()) { c.close(); return; }
        int streak=c.getInt(0), best=c.getInt(1); long lastCheck=c.getLong(2); c.close();
        if ((now-lastCheck) >= 86400_000L) { streak++; if (streak>best) best=streak; }
        ContentValues cv = new ContentValues(); cv.put("streak",streak); cv.put("best_streak",best); cv.put("last_check",now);
        getWritableDatabase().update("habits", cv, "id=?", new String[]{habitId});
        ContentValues lv = new ContentValues(); lv.put("habit_id",habitId); lv.put("ts",now); lv.put("completed",1);
        getWritableDatabase().insert("habit_logs", null, lv);
    }

    // ── Annual stats ──────────────────────────────────────────────────────────
    public void setStat(int year, String key, String value) {
        ContentValues cv = new ContentValues(); cv.put("year",year); cv.put("stat_key",key); cv.put("stat_value",value);
        getWritableDatabase().insertWithOnConflict("annual_stats", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String getStat(int year, String key) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT stat_value FROM annual_stats WHERE year=? AND stat_key=?", new String[]{String.valueOf(year),key});
        String v=null; if (c.moveToFirst()) v=c.getString(0); c.close(); return v;
    }

    // ── Companion ─────────────────────────────────────────────────────────────
    public void insertCompanionMemory(String content, String emotion) {
        ContentValues cv = new ContentValues();
        cv.put("ts",System.currentTimeMillis()); cv.put("content",content); cv.put("emotion",emotion);
        getWritableDatabase().insert("companion_memories", null, cv);
    }
    public int getCompanionMemoryCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM companion_memories", null);
        int n=0; if (c.moveToFirst()) n=c.getInt(0); c.close(); return n;
    }
    public List<CompanionMemory> getRecentMemories(int limit) {
        List<CompanionMemory> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,content,emotion FROM companion_memories ORDER BY ts DESC LIMIT ?", new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            CompanionMemory m=new CompanionMemory(); m.id=c.getLong(0); m.content=c.getString(1); m.emotion=c.getString(2); list.add(m);
        }
        c.close(); return list;
    }

    // ── Life OS ───────────────────────────────────────────────────────────────
    public void insertScheduleItem(String title, String description, int hour, int minute) {
        ContentValues cv = new ContentValues();
        cv.put("title",title); cv.put("description",description); cv.put("hour",hour); cv.put("minute",minute);
        getWritableDatabase().insert("life_schedule", null, cv);
    }
    public List<ScheduleItem> getScheduleForHour(int hour) {
        List<ScheduleItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,title,description,hour,minute FROM life_schedule WHERE hour=? AND notified=0", new String[]{String.valueOf(hour)});
        while (c.moveToNext()) {
            ScheduleItem s=new ScheduleItem(); s.id=c.getLong(0); s.title=c.getString(1); s.description=c.getString(2); s.hour=c.getInt(3); s.minute=c.getInt(4); list.add(s);
        }
        c.close(); return list;
    }
    public void markScheduleNotified(long id) {
        ContentValues cv = new ContentValues(); cv.put("notified",1);
        getWritableDatabase().update("life_schedule", cv, "id=?", new String[]{String.valueOf(id)});
    }
    public List<ScheduleItem> getTodaySchedule() {
        List<ScheduleItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,title,description,hour,minute FROM life_schedule ORDER BY hour,minute", null);
        while (c.moveToNext()) {
            ScheduleItem s=new ScheduleItem(); s.id=c.getLong(0); s.title=c.getString(1); s.description=c.getString(2); s.hour=c.getInt(3); s.minute=c.getInt(4); list.add(s);
        }
        c.close(); return list;
    }
    public void insertLifeFeedback(String activity, int satisfaction) {
        ContentValues cv = new ContentValues();
        cv.put("ts",System.currentTimeMillis()); cv.put("activity",activity); cv.put("satisfaction",satisfaction);
        getWritableDatabase().insert("life_feedback", null, cv);
    }

    // ── Student ───────────────────────────────────────────────────────────────
    public void insertSubject(String name, double coef) {
        ContentValues cv = new ContentValues(); cv.put("name",name); cv.put("coef",coef);
        getWritableDatabase().insertWithOnConflict("student_subjects", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
    public void insertSubject(String name, double coef, double initialAverage) {
        ContentValues cv = new ContentValues(); cv.put("name",name); cv.put("coef",coef); cv.put("average",initialAverage);
        getWritableDatabase().insertWithOnConflict("student_subjects", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<SubjectRow> getSubjects() {
        List<SubjectRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT name,coef,average FROM student_subjects", null);
        while (c.moveToNext()) {
            SubjectRow r=new SubjectRow(); r.name=c.getString(0); r.coef=c.getDouble(1); r.average=c.getDouble(2); list.add(r);
        }
        c.close(); return list;
    }
    public SubjectRow getSubject(String name) {
        Cursor c = getReadableDatabase().rawQuery("SELECT name,coef,average FROM student_subjects WHERE name=?", new String[]{name});
        SubjectRow r=null;
        if (c.moveToFirst()) { r=new SubjectRow(); r.name=c.getString(0); r.coef=c.getDouble(1); r.average=c.getDouble(2); }
        c.close(); return r;
    }
    public void insertGrade(String subjectName, double grade, double weight) {
        ContentValues cv = new ContentValues();
        cv.put("subject_name",subjectName); cv.put("grade",grade); cv.put("weight",weight); cv.put("ts",System.currentTimeMillis());
        getWritableDatabase().insert("student_grades", null, cv);
        recalcSubjectAverage(subjectName);
    }
    private void recalcSubjectAverage(String subjectName) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT SUM(grade*weight)/SUM(weight) FROM student_grades WHERE subject_name=?", new String[]{subjectName});
        if (c.moveToFirst() && !c.isNull(0)) {
            ContentValues cv = new ContentValues(); cv.put("average", c.getDouble(0));
            getWritableDatabase().update("student_subjects", cv, "name=?", new String[]{subjectName});
        }
        c.close();
    }
    public void insertQuiz(String subject, String topic, int questionCount) {
        ContentValues cv = new ContentValues();
        cv.put("subject",subject); cv.put("topic",topic); cv.put("question_count",questionCount); cv.put("ts",System.currentTimeMillis());
        getWritableDatabase().insert("student_quizzes", null, cv);
    }

    // ── Language ──────────────────────────────────────────────────────────────
    public void insertVocab(String language, String word, String translation, String phonetic, String example) {
        ContentValues cv = new ContentValues();
        cv.put("language",language); cv.put("word",word); cv.put("translation",translation);
        cv.put("phonetic",phonetic); cv.put("example",example); cv.put("ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("language_vocab", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        // Init progress row
        ContentValues p = new ContentValues(); p.put("language",language);
        getWritableDatabase().insertWithOnConflict("language_progress", null, p, SQLiteDatabase.CONFLICT_IGNORE);
    }
    public int getVocabCount(String language) {
        Cursor c;
        if (language == null) {
            c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM language_vocab WHERE learned=1", null);
        } else {
            c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM language_vocab WHERE language=?", new String[]{language});
        }
        int n=0; if (c.moveToFirst()) n=c.getInt(0); c.close(); return n;
    }
    public int getLearnedVocabCount(String language) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM language_vocab WHERE language=? AND learned=1", new String[]{language});
        int n=0; if (c.moveToFirst()) n=c.getInt(0); c.close(); return n;
    }
    public void markVocabLearned(String language, String word) {
        ContentValues cv = new ContentValues(); cv.put("learned",1);
        getWritableDatabase().update("language_vocab", cv, "language=? AND word=?", new String[]{language,word});
        int learned = getLearnedVocabCount(language);
        ContentValues p = new ContentValues(); p.put("words_learned",learned); p.put("last_session",System.currentTimeMillis());
        getWritableDatabase().update("language_progress", p, "language=?", new String[]{language});
    }
    public VocabRow getNextVocab(String language) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT word,translation,phonetic,example FROM language_vocab WHERE language=? AND learned=0 ORDER BY ts LIMIT 1", new String[]{language});
        VocabRow r=null;
        if (c.moveToFirst()) { r=new VocabRow(); r.word=c.getString(0); r.translation=c.getString(1); r.phonetic=c.getString(2); r.example=c.getString(3); }
        c.close(); return r;
    }
    public LanguageProgress getLanguageProgress(String language) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT words_learned,fluency_level FROM language_progress WHERE language=?", new String[]{language});
        LanguageProgress p=null;
        if (c.moveToFirst()) { p=new LanguageProgress(); p.wordsLearned=c.getInt(0); p.fluencyLevel=c.getString(1); }
        c.close(); return p;
    }
    public void updateFluencyLevel(String language, String level) {
        ContentValues cv = new ContentValues(); cv.put("fluency_level",level);
        getWritableDatabase().update("language_progress", cv, "language=?", new String[]{language});
    }

    // ── Smart notifications ────────────────────────────────────────────────────
    public void insertNotifBatch(String app, String content, String category, int priority) {
        ContentValues cv = new ContentValues();
        cv.put("ts",System.currentTimeMillis()); cv.put("app",app); cv.put("content",content);
        cv.put("category",category); cv.put("priority",priority);
        getWritableDatabase().insert("notif_batch", null, cv);
    }
    public List<NotifBatch> getPendingBatch() {
        List<NotifBatch> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,app,content,category,priority FROM notif_batch WHERE delivered=0 ORDER BY priority DESC", null);
        while (c.moveToNext()) {
            NotifBatch n=new NotifBatch(); n.id=c.getLong(0); n.app=c.getString(1); n.content=c.getString(2); n.category=c.getString(3); n.priority=c.getInt(4); list.add(n);
        }
        c.close(); return list;
    }
    public void markNotifDelivered(long id) {
        ContentValues cv = new ContentValues(); cv.put("delivered",1);
        getWritableDatabase().update("notif_batch", cv, "id=?", new String[]{String.valueOf(id)});
    }
    public void setAppPriorityPref(String app, int priority) {
        ContentValues cv = new ContentValues(); cv.put("app",app); cv.put("priority",priority);
        getWritableDatabase().insertWithOnConflict("notif_prefs", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ── Cloud sync ────────────────────────────────────────────────────────────
    public void insertSyncHistory(String type, String status, String message) {
        ContentValues cv = new ContentValues();
        cv.put("ts",System.currentTimeMillis()); cv.put("type",type); cv.put("status",status); cv.put("message",message);
        getWritableDatabase().insert("sync_history", null, cv);
    }

    // ── Marketplace ───────────────────────────────────────────────────────────
    public void installSkill(String skillId, String name, String author, String description, int price, String category, float rating) {
        ContentValues cv = new ContentValues();
        cv.put("id",skillId); cv.put("name",name); cv.put("author",author); cv.put("description",description);
        cv.put("price",price); cv.put("category",category); cv.put("rating",rating);
        cv.put("installed",1); cv.put("installed_ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("marketplace_skills", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<MarketSkill> getInstalledSkills() {
        List<MarketSkill> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,author,description,category,rating,price,has_update FROM marketplace_skills WHERE installed=1", null);
        while (c.moveToNext()) {
            MarketSkill s=new MarketSkill(); s.id=c.getString(0); s.name=c.getString(1); s.author=c.getString(2);
            s.description=c.getString(3); s.category=c.getString(4); s.rating=c.getFloat(5); s.price=c.getInt(6); s.hasUpdate=c.getInt(7)==1;
            list.add(s);
        }
        c.close(); return list;
    }
    public MarketSkill getSkill(String skillId) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,author,description,category,rating,price,has_update FROM marketplace_skills WHERE id=? AND installed=1", new String[]{skillId});
        MarketSkill s=null;
        if (c.moveToFirst()) {
            s=new MarketSkill(); s.id=c.getString(0); s.name=c.getString(1); s.author=c.getString(2);
            s.description=c.getString(3); s.category=c.getString(4); s.rating=c.getFloat(5); s.price=c.getInt(6); s.hasUpdate=c.getInt(7)==1;
        }
        c.close(); return s;
    }
    public void uninstallSkill(String skillId) {
        ContentValues cv = new ContentValues(); cv.put("installed",0);
        getWritableDatabase().update("marketplace_skills", cv, "id=?", new String[]{skillId});
    }
    public void rateSkill(String skillId, float rating, String review) {
        ContentValues cv = new ContentValues(); cv.put("user_rating",rating); cv.put("review",review);
        getWritableDatabase().update("marketplace_skills", cv, "id=?", new String[]{skillId});
    }
    // Legacy — keep for any existing callers
    public List<SkillRow> getMarketplaceSkills() {
        List<SkillRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,description,author,rating,0,price,installed FROM marketplace_skills", null);
        while (c.moveToNext()) {
            SkillRow r=new SkillRow(); r.id=c.getString(0); r.name=c.getString(1); r.description=c.getString(2);
            r.author=c.getString(3); r.stars=c.getFloat(4); r.downloads=c.getInt(5); r.priceCoins=c.getInt(6); r.installed=c.getInt(7);
            list.add(r);
        }
        c.close(); return list;
    }

    // ── Family ────────────────────────────────────────────────────────────────
    public void insertFamilyMember(String name, int age, boolean isChild, String pin, String profile) {
        ContentValues cv = new ContentValues();
        cv.put("id","member_"+System.currentTimeMillis()); cv.put("name",name); cv.put("age",age);
        cv.put("is_child",isChild?1:0); cv.put("pin",pin); cv.put("profile",profile);
        getWritableDatabase().insert("family_members", null, cv);
    }
    public List<FamilyMember> getFamilyMembers() {
        List<FamilyMember> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,age,is_child,profile FROM family_members", null);
        while (c.moveToNext()) {
            FamilyMember m=new FamilyMember(); m.id=c.getString(0); m.name=c.getString(1); m.age=c.getInt(2); m.isChild=c.getInt(3)==1; m.profile=c.getString(4); list.add(m);
        }
        c.close(); return list;
    }
    public FamilyMember getMember(String memberId) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,age,is_child,profile FROM family_members WHERE id=?", new String[]{memberId});
        FamilyMember m=null;
        if (c.moveToFirst()) { m=new FamilyMember(); m.id=c.getString(0); m.name=c.getString(1); m.age=c.getInt(2); m.isChild=c.getInt(3)==1; m.profile=c.getString(4); }
        c.close(); return m;
    }
    public void recordUsage(String memberId, int minutes) {
        ContentValues cv = new ContentValues();
        cv.put("member_id",memberId); cv.put("ts",System.currentTimeMillis()); cv.put("minutes",minutes);
        getWritableDatabase().insert("family_usage", null, cv);
    }
    public int getUsageMinutesToday(String memberId) {
        long dayStart = (System.currentTimeMillis()/86400_000L)*86400_000L;
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT SUM(minutes) FROM family_usage WHERE member_id=? AND ts>=?",
            new String[]{memberId, String.valueOf(dayStart)});
        int n=0; if (c.moveToFirst() && !c.isNull(0)) n=c.getInt(0); c.close(); return n;
    }
    // Legacy
    public void addFamilyMember(String id, String name, int age, String role, int limitMin) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("name",name); cv.put("age",age); cv.put("profile",role);
        cv.put("is_child", age<18?1:0);
        getWritableDatabase().insertWithOnConflict("family_members", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ── Omnichannel devices ───────────────────────────────────────────────────
    public void insertDevice(String name, String type, String ip) {
        ContentValues cv = new ContentValues();
        cv.put("id","dev_"+System.currentTimeMillis()); cv.put("name",name); cv.put("type",type); cv.put("ip",ip); cv.put("last_seen",System.currentTimeMillis());
        getWritableDatabase().insert("devices", null, cv);
    }
    public List<DeviceRow> getDevices() {
        List<DeviceRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,type,ip,was_online,last_seen FROM devices", null);
        while (c.moveToNext()) {
            DeviceRow d=new DeviceRow(); d.id=c.getString(0); d.name=c.getString(1); d.type=c.getString(2); d.ip=c.getString(3); d.wasOnline=c.getInt(4)==1; d.lastSeen=c.getLong(5); list.add(d);
        }
        c.close(); return list;
    }
    public void updateDeviceSeen(String deviceId) {
        ContentValues cv = new ContentValues(); cv.put("last_seen",System.currentTimeMillis()); cv.put("was_online",1);
        getWritableDatabase().update("devices", cv, "id=?", new String[]{deviceId});
    }
    public void setDeviceOnline(String deviceId, boolean online) {
        ContentValues cv = new ContentValues(); cv.put("was_online",online?1:0);
        getWritableDatabase().update("devices", cv, "id=?", new String[]{deviceId});
    }
    // Legacy
    public void upsertDevice(String id, String type, String name, boolean connected) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("type",type); cv.put("name",name); cv.put("was_online",connected?1:0); cv.put("last_seen",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("devices", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ── Streaming ─────────────────────────────────────────────────────────────
    public void insertStreamSession(String platform, String title, String category) {
        ContentValues cv = new ContentValues();
        cv.put("platform",platform); cv.put("title",title); cv.put("category",category); cv.put("started_ts",System.currentTimeMillis());
        getWritableDatabase().insert("stream_sessions", null, cv);
    }
    public void finalizeLastStream(int durationMin) {
        long now = System.currentTimeMillis();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id FROM stream_sessions WHERE ended_ts=0 ORDER BY started_ts DESC LIMIT 1", null);
        if (c.moveToFirst()) {
            long sessionId = c.getLong(0);
            ContentValues cv = new ContentValues(); cv.put("ended_ts",now); cv.put("duration_min",durationMin);
            getWritableDatabase().update("stream_sessions", cv, "id=?", new String[]{String.valueOf(sessionId)});
        }
        c.close();
    }
    public void recordClip(String moment, int timestampSec) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id FROM stream_sessions WHERE ended_ts=0 ORDER BY started_ts DESC LIMIT 1", null);
        long sessionId = -1; if (c.moveToFirst()) sessionId=c.getLong(0); c.close();
        ContentValues cv = new ContentValues();
        cv.put("session_id",sessionId); cv.put("moment",moment); cv.put("timestamp_sec",timestampSec); cv.put("ts",System.currentTimeMillis());
        getWritableDatabase().insert("stream_clips", null, cv);
    }
    public List<StreamSession> getRecentSessions(int count) {
        List<StreamSession> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,platform,title,duration_min FROM stream_sessions ORDER BY started_ts DESC LIMIT ?", new String[]{String.valueOf(count)});
        while (c.moveToNext()) {
            StreamSession s=new StreamSession(); s.id=c.getLong(0); s.platform=c.getString(1); s.title=c.getString(2); s.durationMin=c.getInt(3); list.add(s);
        }
        c.close(); return list;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ADMIN CRUD — Quêtes
    // ════════════════════════════════════════════════════════════════════════════
    public List<QuestRow> getAllQuestsAdmin() {
        List<QuestRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,title,description,category,difficulty,status,xp_reward,coin_reward,target,progress FROM quests ORDER BY created_ts DESC", null);
        while (c.moveToNext()) {
            QuestRow r=new QuestRow();
            r.id=c.getString(0); r.title=c.getString(1); r.desc=c.getString(2);
            r.category=c.getString(3); r.difficulty=c.getString(4); r.status=c.getString(5);
            r.xp=c.getInt(6); r.coins=c.getInt(7); r.target=c.getInt(8); r.progress=c.getInt(9);
            list.add(r);
        }
        c.close(); return list;
    }
    public void updateQuestAdmin(String id, String title, String desc, String category, String difficulty, int xp, int coins, int target) {
        ContentValues cv = new ContentValues();
        cv.put("title",title); cv.put("description",desc); cv.put("category",category);
        cv.put("difficulty",difficulty); cv.put("xp_reward",xp); cv.put("coin_reward",coins); cv.put("target",target);
        getWritableDatabase().update("quests", cv, "id=?", new String[]{id});
    }
    public void deleteQuest(String id) {
        getWritableDatabase().delete("quests", "id=?", new String[]{id});
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ADMIN CRUD — Marketplace (ajouter un skill sans l'installer)
    // ════════════════════════════════════════════════════════════════════════════
    public void addSkillToMarketplace(String skillId, String name, String desc, String author, String category, int price) {
        ContentValues cv = new ContentValues();
        cv.put("id",skillId); cv.put("name",name); cv.put("description",desc); cv.put("author",author);
        cv.put("category",category); cv.put("price",price); cv.put("installed",0); cv.put("rating",0f);
        getWritableDatabase().insertWithOnConflict("marketplace_skills", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
    public void removeMarketplaceSkill(String id) {
        getWritableDatabase().delete("marketplace_skills", "id=?", new String[]{id});
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ADMIN CRUD — Vocabulaire
    // ════════════════════════════════════════════════════════════════════════════
    public static class VocabAdminRow { public long id; public String word,translation,phonetic,example; }
    public List<VocabAdminRow> getAllVocabAdmin(String language) {
        List<VocabAdminRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,word,translation,phonetic,example FROM language_vocab WHERE language=? ORDER BY id DESC", new String[]{language});
        while (c.moveToNext()) {
            VocabAdminRow r=new VocabAdminRow();
            r.id=c.getLong(0); r.word=c.getString(1); r.translation=c.getString(2);
            r.phonetic=c.getString(3); r.example=c.getString(4);
            list.add(r);
        }
        c.close(); return list;
    }
    public void deleteVocab(long rowId) {
        getWritableDatabase().delete("language_vocab", "id=?", new String[]{String.valueOf(rowId)});
    }

    // ════════════════════════════════════════════════════════════════════════════
    // LANGUAGES — liste des langues
    // ════════════════════════════════════════════════════════════════════════════
    public static class LanguageRow {
        public String name, level;
        public int wordsLearned, sessionsCount;
    }
    public List<LanguageRow> getLanguages() {
        List<LanguageRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT language, words_learned, fluency_level FROM language_progress ORDER BY words_learned DESC", null);
        while (c.moveToNext()) {
            LanguageRow r = new LanguageRow();
            r.name = c.getString(0);
            r.wordsLearned = c.getInt(1);
            r.level = c.getString(2);
            if (r.level == null || r.level.isEmpty()) r.level = "A1";
            else if (r.level.length() > 3) r.level = r.level.substring(r.level.length() - 2);
            r.sessionsCount = getVocabCount(r.name);
            list.add(r);
        }
        c.close(); return list;
    }
    public void addLanguage(String name) {
        ContentValues cv = new ContentValues();
        cv.put("language", name);
        cv.put("words_learned", 0);
        cv.put("fluency_level", "A1");
        cv.put("last_session", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("language_progress", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SMART NOTIF — notifications intelligentes
    // ════════════════════════════════════════════════════════════════════════════
    public static class SmartNotif {
        public String title, message, priority;
        public long ts;
    }
    public List<SmartNotif> getSmartNotifs(int limit) {
        List<SmartNotif> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT app, content, priority, ts FROM notif_batch ORDER BY ts DESC LIMIT ?", new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            SmartNotif n = new SmartNotif();
            n.title = c.getString(0);
            n.message = c.getString(1);
            int p = c.getInt(2);
            n.priority = p >= 8 ? "urgent" : p >= 5 ? "normal" : "info";
            n.ts = c.getLong(3);
            list.add(n);
        }
        c.close(); return list;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MARKETPLACE — items achetés
    // ════════════════════════════════════════════════════════════════════════════
    public static class MarketItem { public String name; }
    public List<MarketItem> getOwnedItems() {
        List<MarketItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT stat_value FROM annual_stats WHERE stat_key LIKE 'owned_%' ORDER BY id DESC", null);
        while (c.moveToNext()) {
            MarketItem m = new MarketItem(); m.name = c.getString(0); list.add(m);
        }
        c.close(); return list;
    }
    public void addOwnedItem(String itemName) {
        setStat(2026, "owned_" + itemName.replaceAll("[^a-zA-Z0-9]", "_"), itemName);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // OMNICHANNEL — canaux connectés
    // ════════════════════════════════════════════════════════════════════════════
    public static class ChannelRow { public String name; public boolean connected; }
    public List<ChannelRow> getConnectedChannels() {
        String[] names = {"WhatsApp","Telegram","Facebook Mess.","Email","Slack","Twitter / X","Instagram","TikTok","LinkedIn"};
        List<ChannelRow> list = new ArrayList<>();
        for (String n : names) {
            ChannelRow r = new ChannelRow(); r.name = n; r.connected = false; list.add(r);
        }
        return list;
    }
    public int getTotalOmniMessages() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notif_batch", null);
        int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close(); return count;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STREAMING — état de connexion
    // ════════════════════════════════════════════════════════════════════════════
    public boolean isStreamingConnected() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM stream_sessions", null);
        int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close(); return count > 0;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STUDENT — sessions d'étude
    // ════════════════════════════════════════════════════════════════════════════
    public int getStudySessionsCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM student_quizzes", null);
        int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close(); return count;
    }
}
