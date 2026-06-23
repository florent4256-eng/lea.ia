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

public class LeaFeaturesDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_features.db";
    private static final int    DB_VERSION = 1;

    private static LeaFeaturesDatabase instance;
    public static synchronized LeaFeaturesDatabase get(Context ctx) {
        if (instance == null) instance = new LeaFeaturesDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LeaFeaturesDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    // ── Inner data classes ────────────────────────────────────────────────────
    public static class WidgetConfig  { public String id,widgetType; public int enabled,refreshInterval; public long lastUpdate; }
    public static class Achievement   { public String id,name,description,icon,conditionType; public int unlocked,tier,progress,target; public long unlockDate; }
    public static class VaultItem     { public long id; public String title,contentEncrypted; public long createdDate,lastAccess; }
    public static class BiometricCfg  { public int enabled,timeoutMinutes,forceOnLaunch; }
    public static class ThemeCfg      { public String currentTheme,customColors; public int autoSwitch,eyeCare; }
    public static class BackupRecord  { public long id,backupDate,size; public String location,status; public int version; }
    public static class AnalyticsEvt  { public long id,timestamp,duration; public String eventType,featureName; }
    public static class DailyStat     { public String date; public int featuresUsed,totalTime,xpEarned,habitsCompleted; }
    public static class OfflineAction { public long id,timestamp; public String actionType,actionData,syncStatus; }
    public static class VoiceCmd      { public long id,timestamp; public String commandText,executionResult; public float accuracy; }
    public static class A11ySettings  { public int ttsEnabled,highContrast,hapticFeedback; public String fontSize,colorBlindMode; }
    public static class SocialProfile { public String id,pseudo,avatar,friendsList; public int xpTotal,achievementsCount; }
    public static class LeaderEntry   { public long id,timestamp; public String userId; public int rank,xp,streakRecord; }
    public static class Challenge     { public long id; public String challengerPseudo,opponentPseudo,challengeType,status,reward; }
    public static class ForumPost     { public long id,timestamp; public String authorPseudo,title,content; public int repliesCount,likes; }

    // ── Schema ────────────────────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Feature 1: Widgets
        db.execSQL("CREATE TABLE IF NOT EXISTS widgets_config (id TEXT PRIMARY KEY, widget_type TEXT, enabled INTEGER DEFAULT 1, refresh_interval INTEGER DEFAULT 3600, last_update INTEGER DEFAULT 0)");

        // Feature 2: Achievements
        db.execSQL("CREATE TABLE IF NOT EXISTS achievements (id TEXT PRIMARY KEY, name TEXT, description TEXT, icon TEXT, unlocked INTEGER DEFAULT 0, unlock_date INTEGER DEFAULT 0, tier INTEGER DEFAULT 1, condition_type TEXT, progress INTEGER DEFAULT 0, target INTEGER DEFAULT 1)");

        // Feature 3: Biometric / Vault
        db.execSQL("CREATE TABLE IF NOT EXISTS vault_items (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content_encrypted TEXT, created_date INTEGER, last_access INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS biometric_settings (id INTEGER PRIMARY KEY, enabled INTEGER DEFAULT 0, timeout_minutes INTEGER DEFAULT 10, force_on_launch INTEGER DEFAULT 0)");

        // Feature 4: Themes
        db.execSQL("CREATE TABLE IF NOT EXISTS theme_settings (id INTEGER PRIMARY KEY, current_theme TEXT DEFAULT 'galaxie', auto_switch INTEGER DEFAULT 0, eye_care INTEGER DEFAULT 0, custom_colors TEXT DEFAULT '')");

        // Feature 5: Backup
        db.execSQL("CREATE TABLE IF NOT EXISTS backups (id INTEGER PRIMARY KEY AUTOINCREMENT, backup_date INTEGER, size INTEGER DEFAULT 0, location TEXT, version INTEGER DEFAULT 1, status TEXT DEFAULT 'pending')");

        // Feature 6: Analytics
        db.execSQL("CREATE TABLE IF NOT EXISTS analytics_events (id INTEGER PRIMARY KEY AUTOINCREMENT, event_type TEXT, timestamp INTEGER, duration INTEGER DEFAULT 0, feature_name TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_analytics_ts ON analytics_events(timestamp)");
        db.execSQL("CREATE TABLE IF NOT EXISTS daily_stats (date TEXT PRIMARY KEY, features_used INTEGER DEFAULT 0, total_time INTEGER DEFAULT 0, xp_earned INTEGER DEFAULT 0, habits_completed INTEGER DEFAULT 0)");

        // Feature 7: Offline queue
        db.execSQL("CREATE TABLE IF NOT EXISTS offline_queue (id INTEGER PRIMARY KEY AUTOINCREMENT, action_type TEXT, action_data TEXT, sync_status TEXT DEFAULT 'pending', timestamp INTEGER)");

        // Feature 8: Voice commands
        db.execSQL("CREATE TABLE IF NOT EXISTS voice_commands (id INTEGER PRIMARY KEY AUTOINCREMENT, command_text TEXT, execution_result TEXT, timestamp INTEGER, accuracy REAL DEFAULT 0)");

        // Feature 9: Accessibility
        db.execSQL("CREATE TABLE IF NOT EXISTS accessibility_settings (id INTEGER PRIMARY KEY, tts_enabled INTEGER DEFAULT 0, font_size TEXT DEFAULT 'normal', high_contrast INTEGER DEFAULT 0, color_blind_mode TEXT DEFAULT 'none', haptic_feedback INTEGER DEFAULT 1)");

        // Feature 10: Social
        db.execSQL("CREATE TABLE IF NOT EXISTS social_profile (id TEXT PRIMARY KEY, pseudo TEXT, avatar TEXT, xp_total INTEGER DEFAULT 0, achievements_count INTEGER DEFAULT 0, friends_list TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS leaderboard_cache (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, rank INTEGER, xp INTEGER DEFAULT 0, streak_record INTEGER DEFAULT 0, timestamp INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS challenges (id INTEGER PRIMARY KEY AUTOINCREMENT, challenger_pseudo TEXT, opponent_pseudo TEXT, challenge_type TEXT, status TEXT DEFAULT 'pending', reward TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS forum_posts (id INTEGER PRIMARY KEY AUTOINCREMENT, author_pseudo TEXT, title TEXT, content TEXT, replies_count INTEGER DEFAULT 0, likes INTEGER DEFAULT 0, timestamp INTEGER)");

        seedDefaults(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        // Migration douce — aucune table supprimée, aucune donnée perdue
        onCreate(db);
    }

    private void seedDefaults(SQLiteDatabase db) {
        // Seed widgets
        String[][] widgets = {{"widget_habits","HABIT"},{"widget_xp","XP"},{"widget_quests","QUESTS"},{"widget_coins","COINS"}};
        for (String[] w : widgets) {
            ContentValues cv = new ContentValues();
            cv.put("id",w[0]); cv.put("widget_type",w[1]); cv.put("enabled",1); cv.put("refresh_interval",3600);
            db.insertWithOnConflict("widgets_config",null,cv,SQLiteDatabase.CONFLICT_IGNORE);
        }
        // Seed achievements
        String[][] achs = {
            {"ach_first_habit","Premier Pas","Crée ta première habitude","🌱","1","HABIT_CREATE","1"},
            {"ach_first_quest","Première Quête","Complète ta première quête","⚔️","1","QUEST_COMPLETE","1"},
            {"ach_first_skill","Premier Skill","Achète ton premier skill","🛒","1","SKILL_BUY","1"},
            {"ach_7day","Semaine Parfaite","7 jours de streak consécutifs","🔥","2","STREAK_7","7"},
            {"ach_50xp","Montée en Puissance","Gagne 50 XP","⭐","2","XP_50","50"},
            {"ach_10coins","Millionnaire en Herbe","Accumule 10 LÉA Coins","🪙","2","COINS_10","10"},
            {"ach_30day","Mois Légendaire","30 jours de streak","🏆","3","STREAK_30","30"},
            {"ach_500xp","Expert","Atteins 500 XP total","💎","3","XP_500","500"},
            {"ach_level5","Niveau 5","Atteins le niveau 5","🚀","3","LEVEL_5","5"},
            {"ach_100day","Centurion","100 jours de streak","👑","4","STREAK_100","100"},
            {"ach_5000xp","Maître","5000 XP total","🌟","4","XP_5000","5000"},
            {"ach_10skills","Collectionneur","Installe 10 skills","🎒","4","SKILLS_10","10"},
            {"ach_biometric","Fort Knox","Active la sécurité biométrique","🔐","4","BIOMETRIC_ON","1"},
            {"ach_offline","Survivant","Utilise Léa sans internet 1h","📡","4","OFFLINE_HERO","1"},
            {"ach_darkfan","Nuit Étoilée","Active le mode sombre","🌙","1","DARK_MODE","1"},
        };
        for (String[] a : achs) {
            ContentValues cv = new ContentValues();
            cv.put("id",a[0]); cv.put("name",a[1]); cv.put("description",a[2]); cv.put("icon",a[3]);
            cv.put("tier",Integer.parseInt(a[4])); cv.put("condition_type",a[5]); cv.put("target",Integer.parseInt(a[6]));
            db.insertWithOnConflict("achievements",null,cv,SQLiteDatabase.CONFLICT_IGNORE);
        }
        // Seed biometric settings
        ContentValues bio = new ContentValues(); bio.put("id",1);
        db.insertWithOnConflict("biometric_settings",null,bio,SQLiteDatabase.CONFLICT_IGNORE);
        // Seed theme settings
        ContentValues theme = new ContentValues(); theme.put("id",1);
        db.insertWithOnConflict("theme_settings",null,theme,SQLiteDatabase.CONFLICT_IGNORE);
        // Seed a11y settings
        ContentValues a11y = new ContentValues(); a11y.put("id",1);
        db.insertWithOnConflict("accessibility_settings",null,a11y,SQLiteDatabase.CONFLICT_IGNORE);
        // Seed social profile
        ContentValues sp = new ContentValues();
        sp.put("id","local_user"); sp.put("pseudo","LéaUser"); sp.put("avatar","🌟");
        db.insertWithOnConflict("social_profile",null,sp,SQLiteDatabase.CONFLICT_IGNORE);
    }

    // ── Widgets ───────────────────────────────────────────────────────────────
    public List<WidgetConfig> getWidgets() {
        List<WidgetConfig> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,widget_type,enabled,refresh_interval,last_update FROM widgets_config",null);
        while(c.moveToNext()){WidgetConfig w=new WidgetConfig();w.id=c.getString(0);w.widgetType=c.getString(1);w.enabled=c.getInt(2);w.refreshInterval=c.getInt(3);w.lastUpdate=c.getLong(4);list.add(w);}
        c.close(); return list;
    }
    public void setWidgetEnabled(String id, boolean enabled) {
        ContentValues cv=new ContentValues(); cv.put("enabled",enabled?1:0);
        getWritableDatabase().update("widgets_config",cv,"id=?",new String[]{id});
    }
    public void touchWidget(String id) {
        ContentValues cv=new ContentValues(); cv.put("last_update",System.currentTimeMillis());
        getWritableDatabase().update("widgets_config",cv,"id=?",new String[]{id});
    }

    // ── Achievements ──────────────────────────────────────────────────────────
    public List<Achievement> getAllAchievements() {
        List<Achievement> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,name,description,icon,unlocked,unlock_date,tier,condition_type,progress,target FROM achievements ORDER BY tier,name",null);
        while(c.moveToNext()){Achievement a=new Achievement();a.id=c.getString(0);a.name=c.getString(1);a.description=c.getString(2);a.icon=c.getString(3);a.unlocked=c.getInt(4);a.unlockDate=c.getLong(5);a.tier=c.getInt(6);a.conditionType=c.getString(7);a.progress=c.getInt(8);a.target=c.getInt(9);list.add(a);}
        c.close(); return list;
    }
    public int getUnlockedCount() {
        Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM achievements WHERE unlocked=1",null);
        int n=0; if(c.moveToFirst())n=c.getInt(0); c.close(); return n;
    }
    public void updateAchievementProgress(String id, int progress) {
        ContentValues cv=new ContentValues(); cv.put("progress",progress);
        getWritableDatabase().update("achievements",cv,"id=?",new String[]{id});
    }
    public boolean unlockAchievement(String id) {
        Cursor c=getReadableDatabase().rawQuery("SELECT unlocked FROM achievements WHERE id=?",new String[]{id});
        boolean already=false; if(c.moveToFirst())already=c.getInt(0)==1; c.close();
        if(already)return false;
        ContentValues cv=new ContentValues(); cv.put("unlocked",1); cv.put("unlock_date",System.currentTimeMillis());
        getWritableDatabase().update("achievements",cv,"id=?",new String[]{id});
        return true;
    }

    // ── Vault ─────────────────────────────────────────────────────────────────
    public long insertVaultItem(String title, String contentEncrypted) {
        ContentValues cv=new ContentValues(); cv.put("title",title); cv.put("content_encrypted",contentEncrypted); cv.put("created_date",System.currentTimeMillis());
        return getWritableDatabase().insert("vault_items",null,cv);
    }
    public List<VaultItem> getVaultItems() {
        List<VaultItem> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,title,content_encrypted,created_date,last_access FROM vault_items ORDER BY created_date DESC",null);
        while(c.moveToNext()){VaultItem v=new VaultItem();v.id=c.getLong(0);v.title=c.getString(1);v.contentEncrypted=c.getString(2);v.createdDate=c.getLong(3);v.lastAccess=c.getLong(4);list.add(v);}
        c.close(); return list;
    }
    public void touchVaultItem(long id) {
        ContentValues cv=new ContentValues(); cv.put("last_access",System.currentTimeMillis());
        getWritableDatabase().update("vault_items",cv,"id=?",new String[]{String.valueOf(id)});
    }
    public void deleteVaultItem(long id) { getWritableDatabase().delete("vault_items","id=?",new String[]{String.valueOf(id)}); }

    // ── Biometric settings ────────────────────────────────────────────────────
    public BiometricCfg getBiometricConfig() {
        Cursor c=getReadableDatabase().rawQuery("SELECT enabled,timeout_minutes,force_on_launch FROM biometric_settings WHERE id=1",null);
        BiometricCfg cfg=new BiometricCfg(); if(c.moveToFirst()){cfg.enabled=c.getInt(0);cfg.timeoutMinutes=c.getInt(1);cfg.forceOnLaunch=c.getInt(2);}
        c.close(); return cfg;
    }
    public void saveBiometricConfig(int enabled, int timeoutMin, int forceOnLaunch) {
        ContentValues cv=new ContentValues(); cv.put("enabled",enabled); cv.put("timeout_minutes",timeoutMin); cv.put("force_on_launch",forceOnLaunch);
        getWritableDatabase().update("biometric_settings",cv,"id=1",null);
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    public ThemeCfg getThemeConfig() {
        Cursor c=getReadableDatabase().rawQuery("SELECT current_theme,auto_switch,eye_care,custom_colors FROM theme_settings WHERE id=1",null);
        ThemeCfg cfg=new ThemeCfg(); cfg.currentTheme="galaxie";
        if(c.moveToFirst()){cfg.currentTheme=c.getString(0);cfg.autoSwitch=c.getInt(1);cfg.eyeCare=c.getInt(2);cfg.customColors=c.getString(3);}
        c.close(); return cfg;
    }
    public void saveTheme(String theme) {
        ContentValues cv=new ContentValues(); cv.put("current_theme",theme);
        getWritableDatabase().update("theme_settings",cv,"id=1",null);
    }
    public void saveThemeConfig(String theme, int autoSwitch, int eyeCare, String customColors) {
        ContentValues cv=new ContentValues(); cv.put("current_theme",theme); cv.put("auto_switch",autoSwitch); cv.put("eye_care",eyeCare); cv.put("custom_colors",customColors);
        getWritableDatabase().update("theme_settings",cv,"id=1",null);
    }

    // ── Backups ───────────────────────────────────────────────────────────────
    public long insertBackup(String location, long size, int version) {
        ContentValues cv=new ContentValues(); cv.put("backup_date",System.currentTimeMillis()); cv.put("location",location); cv.put("size",size); cv.put("version",version); cv.put("status","completed");
        long id=getWritableDatabase().insert("backups",null,cv);
        getWritableDatabase().execSQL("DELETE FROM backups WHERE id NOT IN (SELECT id FROM backups ORDER BY backup_date DESC LIMIT 10)");
        return id;
    }
    public List<BackupRecord> getBackups() {
        List<BackupRecord> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,backup_date,size,location,version,status FROM backups ORDER BY backup_date DESC",null);
        while(c.moveToNext()){BackupRecord b=new BackupRecord();b.id=c.getLong(0);b.backupDate=c.getLong(1);b.size=c.getLong(2);b.location=c.getString(3);b.version=c.getInt(4);b.status=c.getString(5);list.add(b);}
        c.close(); return list;
    }
    public BackupRecord getLastBackup() {
        Cursor c=getReadableDatabase().rawQuery("SELECT id,backup_date,size,location,version,status FROM backups ORDER BY backup_date DESC LIMIT 1",null);
        BackupRecord b=null; if(c.moveToFirst()){b=new BackupRecord();b.id=c.getLong(0);b.backupDate=c.getLong(1);b.size=c.getLong(2);b.location=c.getString(3);b.version=c.getInt(4);b.status=c.getString(5);}
        c.close(); return b;
    }

    // ── Analytics ─────────────────────────────────────────────────────────────
    public void logEvent(String eventType, long duration, String featureName) {
        ContentValues cv=new ContentValues(); cv.put("event_type",eventType); cv.put("timestamp",System.currentTimeMillis()); cv.put("duration",duration); cv.put("feature_name",featureName);
        getWritableDatabase().insert("analytics_events",null,cv);
        getWritableDatabase().execSQL("DELETE FROM analytics_events WHERE timestamp < ?", new Object[]{System.currentTimeMillis()-30L*86400_000L});
    }
    public int getTodayEventCount(String featureName) {
        long dayStart=(System.currentTimeMillis()/86400_000L)*86400_000L;
        String[] args=featureName!=null?new String[]{featureName,String.valueOf(dayStart)}:new String[]{String.valueOf(dayStart)};
        String where=featureName!=null?"feature_name=? AND timestamp>=?":"timestamp>=?";
        Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM analytics_events WHERE "+where,args);
        int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;
    }
    public List<DailyStat> getWeekStats() {
        List<DailyStat> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT date,features_used,total_time,xp_earned,habits_completed FROM daily_stats ORDER BY date DESC LIMIT 7",null);
        while(c.moveToNext()){DailyStat s=new DailyStat();s.date=c.getString(0);s.featuresUsed=c.getInt(1);s.totalTime=c.getInt(2);s.xpEarned=c.getInt(3);s.habitsCompleted=c.getInt(4);list.add(s);}
        c.close(); return list;
    }
    public void upsertDailyStat(String date, int featuresUsed, int totalTimeMin, int xpEarned, int habitsCompleted) {
        ContentValues cv=new ContentValues(); cv.put("date",date); cv.put("features_used",featuresUsed); cv.put("total_time",totalTimeMin); cv.put("xp_earned",xpEarned); cv.put("habits_completed",habitsCompleted);
        getWritableDatabase().insertWithOnConflict("daily_stats",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String getMostUsedFeature() {
        long dayStart=(System.currentTimeMillis()/86400_000L)*86400_000L - 7L*86400_000L;
        Cursor c=getReadableDatabase().rawQuery("SELECT feature_name,COUNT(*) as cnt FROM analytics_events WHERE timestamp>=? GROUP BY feature_name ORDER BY cnt DESC LIMIT 1",new String[]{String.valueOf(dayStart)});
        String f="—"; if(c.moveToFirst())f=c.getString(0); c.close(); return f;
    }

    // ── Offline queue ─────────────────────────────────────────────────────────
    public long queueAction(String actionType, String actionData) {
        ContentValues cv=new ContentValues(); cv.put("action_type",actionType); cv.put("action_data",actionData); cv.put("timestamp",System.currentTimeMillis()); cv.put("sync_status","pending");
        return getWritableDatabase().insert("offline_queue",null,cv);
    }
    public List<OfflineAction> getPendingActions() {
        List<OfflineAction> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,action_type,action_data,sync_status,timestamp FROM offline_queue WHERE sync_status='pending' ORDER BY timestamp",null);
        while(c.moveToNext()){OfflineAction a=new OfflineAction();a.id=c.getLong(0);a.actionType=c.getString(1);a.actionData=c.getString(2);a.syncStatus=c.getString(3);a.timestamp=c.getLong(4);list.add(a);}
        c.close(); return list;
    }
    public void markActionSynced(long id) {
        ContentValues cv=new ContentValues(); cv.put("sync_status","synced");
        getWritableDatabase().update("offline_queue",cv,"id=?",new String[]{String.valueOf(id)});
        getWritableDatabase().execSQL("DELETE FROM offline_queue WHERE sync_status='synced' AND timestamp < ?",new Object[]{System.currentTimeMillis()-86400_000L});
    }
    public int getPendingCount() {
        Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM offline_queue WHERE sync_status='pending'",null);
        int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;
    }

    // ── Voice commands ────────────────────────────────────────────────────────
    public void logVoiceCommand(String commandText, String result, float accuracy) {
        ContentValues cv=new ContentValues(); cv.put("command_text",commandText); cv.put("execution_result",result); cv.put("timestamp",System.currentTimeMillis()); cv.put("accuracy",accuracy);
        getWritableDatabase().insert("voice_commands",null,cv);
        getWritableDatabase().execSQL("DELETE FROM voice_commands WHERE id NOT IN (SELECT id FROM voice_commands ORDER BY timestamp DESC LIMIT 100)");
    }
    public List<VoiceCmd> getRecentCommands(int limit) {
        List<VoiceCmd> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,command_text,execution_result,timestamp,accuracy FROM voice_commands ORDER BY timestamp DESC LIMIT ?",new String[]{String.valueOf(limit)});
        while(c.moveToNext()){VoiceCmd v=new VoiceCmd();v.id=c.getLong(0);v.commandText=c.getString(1);v.executionResult=c.getString(2);v.timestamp=c.getLong(3);v.accuracy=c.getFloat(4);list.add(v);}
        c.close(); return list;
    }

    // ── Accessibility ─────────────────────────────────────────────────────────
    public A11ySettings getA11ySettings() {
        Cursor c=getReadableDatabase().rawQuery("SELECT tts_enabled,font_size,high_contrast,color_blind_mode,haptic_feedback FROM accessibility_settings WHERE id=1",null);
        A11ySettings s=new A11ySettings(); s.fontSize="normal"; s.colorBlindMode="none"; s.hapticFeedback=1;
        if(c.moveToFirst()){s.ttsEnabled=c.getInt(0);s.fontSize=c.getString(1);s.highContrast=c.getInt(2);s.colorBlindMode=c.getString(3);s.hapticFeedback=c.getInt(4);}
        c.close(); return s;
    }
    public void saveA11ySettings(int tts, String fontSize, int highContrast, String colorBlind, int haptic) {
        ContentValues cv=new ContentValues(); cv.put("tts_enabled",tts); cv.put("font_size",fontSize); cv.put("high_contrast",highContrast); cv.put("color_blind_mode",colorBlind); cv.put("haptic_feedback",haptic);
        getWritableDatabase().update("accessibility_settings",cv,"id=1",null);
    }

    // ── Social ────────────────────────────────────────────────────────────────
    public SocialProfile getMyProfile() {
        Cursor c=getReadableDatabase().rawQuery("SELECT id,pseudo,avatar,xp_total,achievements_count,friends_list FROM social_profile WHERE id='local_user'",null);
        SocialProfile p=new SocialProfile(); p.id="local_user"; p.pseudo="LéaUser"; p.avatar="🌟";
        if(c.moveToFirst()){p.id=c.getString(0);p.pseudo=c.getString(1);p.avatar=c.getString(2);p.xpTotal=c.getInt(3);p.achievementsCount=c.getInt(4);p.friendsList=c.getString(5);}
        c.close(); return p;
    }
    public void updateMyProfile(String pseudo, String avatar) {
        ContentValues cv=new ContentValues(); cv.put("pseudo",pseudo); cv.put("avatar",avatar);
        getWritableDatabase().update("social_profile",cv,"id='local_user'",null);
    }
    public void syncProfileStats(int xpTotal, int achievementsCount) {
        ContentValues cv=new ContentValues(); cv.put("xp_total",xpTotal); cv.put("achievements_count",achievementsCount);
        getWritableDatabase().update("social_profile",cv,"id='local_user'",null);
    }
    public void cacheLeaderboard(List<LeaderEntry> entries) {
        getWritableDatabase().delete("leaderboard_cache",null,null);
        for(LeaderEntry e:entries){ContentValues cv=new ContentValues();cv.put("user_id",e.userId);cv.put("rank",e.rank);cv.put("xp",e.xp);cv.put("streak_record",e.streakRecord);cv.put("timestamp",System.currentTimeMillis());getWritableDatabase().insert("leaderboard_cache",null,cv);}
    }
    public List<LeaderEntry> getLeaderboardCache() {
        List<LeaderEntry> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,user_id,rank,xp,streak_record,timestamp FROM leaderboard_cache ORDER BY rank",null);
        while(c.moveToNext()){LeaderEntry e=new LeaderEntry();e.id=c.getLong(0);e.userId=c.getString(1);e.rank=c.getInt(2);e.xp=c.getInt(3);e.streakRecord=c.getInt(4);e.timestamp=c.getLong(5);list.add(e);}
        c.close(); return list;
    }
    public long insertChallenge(String challenger, String opponent, String type, String reward) {
        ContentValues cv=new ContentValues(); cv.put("challenger_pseudo",challenger); cv.put("opponent_pseudo",opponent); cv.put("challenge_type",type); cv.put("status","pending"); cv.put("reward",reward);
        return getWritableDatabase().insert("challenges",null,cv);
    }
    public List<Challenge> getChallenges() {
        List<Challenge> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,challenger_pseudo,opponent_pseudo,challenge_type,status,reward FROM challenges ORDER BY id DESC",null);
        while(c.moveToNext()){Challenge ch=new Challenge();ch.id=c.getLong(0);ch.challengerPseudo=c.getString(1);ch.opponentPseudo=c.getString(2);ch.challengeType=c.getString(3);ch.status=c.getString(4);ch.reward=c.getString(5);list.add(ch);}
        c.close(); return list;
    }
    public void updateChallengeStatus(long id, String status) {
        ContentValues cv=new ContentValues(); cv.put("status",status);
        getWritableDatabase().update("challenges",cv,"id=?",new String[]{String.valueOf(id)});
    }
    public long insertForumPost(String authorPseudo, String title, String content) {
        ContentValues cv=new ContentValues(); cv.put("author_pseudo",authorPseudo); cv.put("title",title); cv.put("content",content); cv.put("timestamp",System.currentTimeMillis());
        return getWritableDatabase().insert("forum_posts",null,cv);
    }
    public List<ForumPost> getForumPosts(int limit) {
        List<ForumPost> list=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,author_pseudo,title,content,replies_count,likes,timestamp FROM forum_posts ORDER BY timestamp DESC LIMIT ?",new String[]{String.valueOf(limit)});
        while(c.moveToNext()){ForumPost p=new ForumPost();p.id=c.getLong(0);p.authorPseudo=c.getString(1);p.title=c.getString(2);p.content=c.getString(3);p.repliesCount=c.getInt(4);p.likes=c.getInt(5);p.timestamp=c.getLong(6);list.add(p);}
        c.close(); return list;
    }
    public void likePost(long postId) {
        getWritableDatabase().execSQL("UPDATE forum_posts SET likes=likes+1 WHERE id=?",new Object[]{postId});
    }
}
