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

public class LeaAgentDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_agents.db";
    private static final int    DB_VERSION = 1;

    // ── Tables ────────────────────────────────────────────────────────────────

    private static final String CREATE_AGENTS =
        "CREATE TABLE IF NOT EXISTS agents (" +
        "id TEXT PRIMARY KEY, " +
        "name TEXT, " +
        "enabled INTEGER DEFAULT 0, " +
        "last_action TEXT, " +
        "last_run INTEGER DEFAULT 0)";

    private static final String CREATE_LOGS =
        "CREATE TABLE IF NOT EXISTS agent_logs (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "agent_id TEXT, " +
        "timestamp INTEGER, " +
        "message TEXT)";

    private static final String CREATE_PROJECTS =
        "CREATE TABLE IF NOT EXISTS code_projects (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT, " +
        "description TEXT, " +
        "root_path TEXT, " +
        "apk_path TEXT, " +
        "status TEXT DEFAULT 'draft', " +
        "created_at INTEGER, " +
        "updated_at INTEGER)";

    private static final String CREATE_CONVERSATIONS =
        "CREATE TABLE IF NOT EXISTS code_conversations (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "project_id INTEGER, " +
        "role TEXT, " +
        "content TEXT, " +
        "timestamp INTEGER)";

    private static final String CREATE_FINANCE =
        "CREATE TABLE IF NOT EXISTS finance_entries (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "amount REAL, " +
        "category TEXT, " +
        "description TEXT, " +
        "source TEXT, " +
        "timestamp INTEGER)";

    private static LeaAgentDatabase instance;

    public static synchronized LeaAgentDatabase get(Context ctx) {
        if (instance == null) instance = new LeaAgentDatabase(ctx.getApplicationContext());
        return instance;
    }

    private LeaAgentDatabase(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_AGENTS);
        db.execSQL(CREATE_LOGS);
        db.execSQL(CREATE_PROJECTS);
        db.execSQL(CREATE_CONVERSATIONS);
        db.execSQL(CREATE_FINANCE);
        seedAgents(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Ne JAMAIS supprimer "agents" — contient les états activé/désactivé de l'utilisateur
        // finance_entries, code_projects, code_conversations → données utilisateur, on préserve
        // agent_logs uniquement → reconstruisibles automatiquement, on peut nettoyer
        db.execSQL("DROP TABLE IF EXISTS agent_logs");
        db.execSQL(CREATE_AGENTS);
        db.execSQL(CREATE_LOGS);
        db.execSQL(CREATE_PROJECTS);
        db.execSQL(CREATE_CONVERSATIONS);
        db.execSQL(CREATE_FINANCE);
        // CONFLICT_IGNORE → ajoute les nouveaux agents sans écraser les existants
        seedAgents(db);
    }

    private void seedAgents(SQLiteDatabase db) {
        String[][] agents = {
            {"EMAIL",        "Email Intelligent"},
            {"NOTIFICATION", "Notification Smart"},
            {"CALENDAR",     "Calendrier Intelligent"},
            {"FINANCE",      "Finance / Budget"},
            {"HEALTH",       "Santé / Bien-être"},
            {"PRODUCTIVITY", "Productivité"},
            {"SOCIAL",       "Social"},
            {"SMART_HOME",   "Smart Home"},
            {"LEARNING",     "Apprentissage"},
            {"SECURITY",     "Anti-Spam / Sécurité"},
            {"CODE",         "Code Agent"},
        };
        for (String[] a : agents) {
            ContentValues cv = new ContentValues();
            cv.put("id", a[0]);
            cv.put("name", a[1]);
            cv.put("enabled", 0);
            db.insertWithOnConflict("agents", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ── Agent state ───────────────────────────────────────────────────────────

    public boolean isEnabled(String agentId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT enabled FROM agents WHERE id=?", new String[]{agentId});
        boolean result = false;
        if (c.moveToFirst()) result = c.getInt(0) == 1;
        c.close();
        return result;
    }

    public void setEnabled(String agentId, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put("enabled", enabled ? 1 : 0);
        getWritableDatabase().update("agents", cv, "id=?", new String[]{agentId});
    }

    public void updateLastAction(String agentId, String action) {
        ContentValues cv = new ContentValues();
        cv.put("last_action", action);
        cv.put("last_run", System.currentTimeMillis());
        getWritableDatabase().update("agents", cv, "id=?", new String[]{agentId});
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    public void addLog(String agentId, String message) {
        ContentValues cv = new ContentValues();
        cv.put("agent_id", agentId);
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("message", message);
        getWritableDatabase().insert("agent_logs", null, cv);
        // Keep last 100 logs per agent
        getWritableDatabase().execSQL(
            "DELETE FROM agent_logs WHERE agent_id=? AND id NOT IN " +
            "(SELECT id FROM agent_logs WHERE agent_id=? ORDER BY timestamp DESC LIMIT 100)",
            new Object[]{agentId, agentId});
    }

    public static class LogRow {
        public long   timestamp;
        public String message;
    }

    public List<LogRow> getLogs(String agentId, int limit) {
        List<LogRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT timestamp, message FROM agent_logs WHERE agent_id=? ORDER BY timestamp DESC LIMIT ?",
            new String[]{agentId, String.valueOf(limit)});
        while (c.moveToNext()) {
            LogRow r = new LogRow();
            r.timestamp = c.getLong(0);
            r.message   = c.getString(1);
            list.add(r);
        }
        c.close();
        return list;
    }

    // ── Code Projects ─────────────────────────────────────────────────────────

    public static class ProjectRow {
        public long   id;
        public String name, description, rootPath, apkPath, status;
        public long   createdAt, updatedAt;
    }

    public long createProject(String name, String description, String rootPath) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("description", description);
        cv.put("root_path", rootPath);
        cv.put("status", "draft");
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insert("code_projects", null, cv);
    }

    public void updateProjectStatus(long id, String status, String apkPath) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("updated_at", System.currentTimeMillis());
        if (apkPath != null) cv.put("apk_path", apkPath);
        getWritableDatabase().update("code_projects", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public List<ProjectRow> getProjects() {
        List<ProjectRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,description,root_path,apk_path,status,created_at,updated_at FROM code_projects ORDER BY updated_at DESC", null);
        while (c.moveToNext()) {
            ProjectRow r = new ProjectRow();
            r.id          = c.getLong(0);
            r.name        = c.getString(1);
            r.description = c.getString(2);
            r.rootPath    = c.getString(3);
            r.apkPath     = c.getString(4);
            r.status      = c.getString(5);
            r.createdAt   = c.getLong(6);
            r.updatedAt   = c.getLong(7);
            list.add(r);
        }
        c.close();
        return list;
    }

    public void deleteProject(long id) {
        getWritableDatabase().delete("code_projects", "id=?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete("code_conversations", "project_id=?", new String[]{String.valueOf(id)});
    }

    // ── Conversations ─────────────────────────────────────────────────────────

    public void addMessage(long projectId, String role, String content) {
        ContentValues cv = new ContentValues();
        cv.put("project_id", projectId);
        cv.put("role", role);
        cv.put("content", content);
        cv.put("timestamp", System.currentTimeMillis());
        getWritableDatabase().insert("code_conversations", null, cv);
    }

    public static class MessageRow {
        public String role, content;
        public long   timestamp;
    }

    public List<MessageRow> getMessages(long projectId) {
        List<MessageRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT role,content,timestamp FROM code_conversations WHERE project_id=? ORDER BY timestamp ASC",
            new String[]{String.valueOf(projectId)});
        while (c.moveToNext()) {
            MessageRow r = new MessageRow();
            r.role      = c.getString(0);
            r.content   = c.getString(1);
            r.timestamp = c.getLong(2);
            list.add(r);
        }
        c.close();
        return list;
    }

    // ── Finance ───────────────────────────────────────────────────────────────

    public void addFinanceEntry(double amount, String category, String description, String source) {
        ContentValues cv = new ContentValues();
        cv.put("amount", amount);
        cv.put("category", category);
        cv.put("description", description);
        cv.put("source", source);
        cv.put("timestamp", System.currentTimeMillis());
        getWritableDatabase().insert("finance_entries", null, cv);
    }

    public static class FinanceRow {
        public long   id, timestamp;
        public double amount;
        public String category, description, source;
    }

    public List<FinanceRow> getFinanceEntries(int limit) {
        List<FinanceRow> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,amount,category,description,source,timestamp FROM finance_entries ORDER BY timestamp DESC LIMIT ?",
            new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            FinanceRow r = new FinanceRow();
            r.id          = c.getLong(0);
            r.amount      = c.getDouble(1);
            r.category    = c.getString(2);
            r.description = c.getString(3);
            r.source      = c.getString(4);
            r.timestamp   = c.getLong(5);
            list.add(r);
        }
        c.close();
        return list;
    }

    public double getTotalSpentThisWeek() {
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT SUM(ABS(amount)) FROM finance_entries WHERE amount < 0 AND timestamp > ?",
            new String[]{String.valueOf(weekAgo)});
        double total = 0;
        if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
        c.close();
        return total;
    }
}
