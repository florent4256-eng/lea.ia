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

public class LeaNetworkDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "lea_network.db";
    private static final int    DB_VERSION = 1;

    private static LeaNetworkDatabase instance;
    public static synchronized LeaNetworkDatabase get(Context ctx) {
        if (instance == null) instance = new LeaNetworkDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LeaNetworkDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    // ── Inner data classes ────────────────────────────────────────────────────
    public static class NetworkLog {
        public long id, timestamp, latencyMs;
        public String connectionType, serverUrl, event, details;
    }

    // ── Schema ────────────────────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS network_logs ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "timestamp INTEGER, "
            + "connection_type TEXT, "
            + "server_url TEXT, "
            + "event TEXT, "
            + "details TEXT, "
            + "latency_ms INTEGER DEFAULT -1)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_net_ts ON network_logs(timestamp)");

        db.execSQL("CREATE TABLE IF NOT EXISTS connection_stats ("
            + "connection_type TEXT PRIMARY KEY, "
            + "total_connections INTEGER DEFAULT 0, "
            + "total_errors INTEGER DEFAULT 0, "
            + "avg_latency_ms INTEGER DEFAULT 0, "
            + "last_connected INTEGER DEFAULT 0, "
            + "last_error INTEGER DEFAULT 0)");

        // Seed connection types
        for (String t : new String[]{"LOCAL", "CLOUDFLARE", "OFFLINE"}) {
            ContentValues cv = new ContentValues(); cv.put("connection_type", t);
            db.insertWithOnConflict("connection_stats", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        // Migration douce — aucune table supprimée, aucune donnée perdue
        onCreate(db);
    }

    // ── Logging ───────────────────────────────────────────────────────────────
    public void log(String connectionType, String serverUrl, String event, String details, long latencyMs) {
        ContentValues cv = new ContentValues();
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("connection_type", connectionType);
        cv.put("server_url", serverUrl);
        cv.put("event", event);
        cv.put("details", details);
        cv.put("latency_ms", latencyMs);
        getWritableDatabase().insert("network_logs", null, cv);
        // Keep only 7 days
        getWritableDatabase().execSQL(
            "DELETE FROM network_logs WHERE timestamp < ?",
            new Object[]{System.currentTimeMillis() - 7L * 86400_000L});
    }

    public List<NetworkLog> getRecentLogs(int limit) {
        List<NetworkLog> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,timestamp,connection_type,server_url,event,details,latency_ms "
            + "FROM network_logs ORDER BY timestamp DESC LIMIT ?",
            new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            NetworkLog l = new NetworkLog();
            l.id = c.getLong(0); l.timestamp = c.getLong(1);
            l.connectionType = c.getString(2); l.serverUrl = c.getString(3);
            l.event = c.getString(4); l.details = c.getString(5); l.latencyMs = c.getLong(6);
            list.add(l);
        }
        c.close(); return list;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    public void recordConnection(String type, long latencyMs) {
        getWritableDatabase().execSQL(
            "UPDATE connection_stats SET "
            + "total_connections=total_connections+1, "
            + "avg_latency_ms=((avg_latency_ms*total_connections)+" + latencyMs + ")/(total_connections+1), "
            + "last_connected=? WHERE connection_type=?",
            new Object[]{System.currentTimeMillis(), type});
    }

    public void recordError(String type) {
        getWritableDatabase().execSQL(
            "UPDATE connection_stats SET total_errors=total_errors+1, last_error=? WHERE connection_type=?",
            new Object[]{System.currentTimeMillis(), type});
    }

    public long getAvgLatency(String type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT avg_latency_ms FROM connection_stats WHERE connection_type=?", new String[]{type});
        long v = -1; if (c.moveToFirst()) v = c.getLong(0); c.close(); return v;
    }

    public int getTotalConnections(String type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT total_connections FROM connection_stats WHERE connection_type=?", new String[]{type});
        int v = 0; if (c.moveToFirst()) v = c.getInt(0); c.close(); return v;
    }

    public int getTotalErrors(String type) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT total_errors FROM connection_stats WHERE connection_type=?", new String[]{type});
        int v = 0; if (c.moveToFirst()) v = c.getInt(0); c.close(); return v;
    }
}
