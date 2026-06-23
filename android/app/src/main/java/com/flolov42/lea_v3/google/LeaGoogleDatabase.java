package com.flolov42.lea_v3.google;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class LeaGoogleDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "lea_google.db";
    private static final int    DB_VER  = 1;

    private static LeaGoogleDatabase instance;
    public static synchronized LeaGoogleDatabase get(Context ctx) {
        if (instance == null) instance = new LeaGoogleDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LeaGoogleDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VER); }

    // ── Data classes ──────────────────────────────────────────────────────────
    public static class CalEvent  { public String id,title,location,description; public long startTs,endTs; }
    public static class GTask     { public String id,listId,title,notes; public boolean completed; public long dueTs; }
    public static class GContact  { public String id,name,email,phone; }
    public static class GMessage  { public String id,from,subject,snippet; public long ts; public boolean unread; }
    public static class GFile     { public String id,name,mimeType; public long size,modifiedTs; }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS google_events (" +
            "id TEXT PRIMARY KEY, title TEXT, location TEXT, description TEXT, " +
            "start_ts INTEGER, end_ts INTEGER, synced_ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS google_tasks (" +
            "id TEXT PRIMARY KEY, list_id TEXT, title TEXT, notes TEXT, " +
            "completed INTEGER DEFAULT 0, due_ts INTEGER, synced_ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS google_contacts (" +
            "id TEXT PRIMARY KEY, name TEXT, email TEXT, phone TEXT, synced_ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS google_messages (" +
            "id TEXT PRIMARY KEY, thread_id TEXT, from_addr TEXT, subject TEXT, " +
            "snippet TEXT, ts INTEGER, unread INTEGER DEFAULT 1, synced_ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS google_files (" +
            "id TEXT PRIMARY KEY, name TEXT, mime_type TEXT, size INTEGER, modified_ts INTEGER, synced_ts INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS google_auth (" +
            "id INTEGER PRIMARY KEY, email TEXT, display_name TEXT, photo_url TEXT, " +
            "access_token TEXT, token_expiry INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        for (String t : new String[]{"google_events","google_tasks","google_contacts","google_messages","google_files","google_auth"})
            db.execSQL("DROP TABLE IF EXISTS " + t);
        onCreate(db);
    }

    // ── Auth storage ──────────────────────────────────────────────────────────
    public void saveAuth(String email, String displayName, String photoUrl) {
        ContentValues cv = new ContentValues();
        cv.put("id",1); cv.put("email",email); cv.put("display_name",displayName); cv.put("photo_url",photoUrl);
        getWritableDatabase().insertWithOnConflict("google_auth", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public void saveToken(String token, long expiryMs) {
        ContentValues cv = new ContentValues(); cv.put("access_token",token); cv.put("token_expiry",expiryMs);
        getWritableDatabase().update("google_auth", cv, "id=1", null);
    }
    public String[] getAuth() {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT email,display_name,photo_url,access_token,token_expiry FROM google_auth WHERE id=1", null);
        String[] r = null;
        if (c.moveToFirst()) r = new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),String.valueOf(c.getLong(4))};
        c.close(); return r;
    }
    public void clearAuth() {
        getWritableDatabase().delete("google_auth", null, null);
    }

    // ── Calendar ──────────────────────────────────────────────────────────────
    public void upsertEvent(String id, String title, String location, String desc, long startTs, long endTs) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("title",title); cv.put("location",location); cv.put("description",desc);
        cv.put("start_ts",startTs); cv.put("end_ts",endTs); cv.put("synced_ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("google_events", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<CalEvent> getUpcomingEvents(int limit) {
        List<CalEvent> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,title,location,description,start_ts,end_ts FROM google_events WHERE start_ts>=? ORDER BY start_ts LIMIT ?",
            new String[]{String.valueOf(now), String.valueOf(limit)});
        while (c.moveToNext()) {
            CalEvent e=new CalEvent(); e.id=c.getString(0); e.title=c.getString(1);
            e.location=c.getString(2); e.description=c.getString(3);
            e.startTs=c.getLong(4); e.endTs=c.getLong(5); list.add(e);
        }
        c.close(); return list;
    }
    public List<CalEvent> getAllEvents() {
        List<CalEvent> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,title,location,description,start_ts,end_ts FROM google_events ORDER BY start_ts DESC LIMIT 50", null);
        while (c.moveToNext()) {
            CalEvent e=new CalEvent(); e.id=c.getString(0); e.title=c.getString(1);
            e.location=c.getString(2); e.description=c.getString(3); e.startTs=c.getLong(4); e.endTs=c.getLong(5); list.add(e);
        }
        c.close(); return list;
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────
    public void upsertTask(String id, String listId, String title, String notes, boolean completed, long dueTs) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("list_id",listId); cv.put("title",title); cv.put("notes",notes);
        cv.put("completed",completed?1:0); cv.put("due_ts",dueTs); cv.put("synced_ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("google_tasks", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<GTask> getPendingTasks() {
        List<GTask> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,list_id,title,notes,completed,due_ts FROM google_tasks WHERE completed=0 ORDER BY due_ts", null);
        while (c.moveToNext()) {
            GTask t=new GTask(); t.id=c.getString(0); t.listId=c.getString(1); t.title=c.getString(2);
            t.notes=c.getString(3); t.completed=c.getInt(4)==1; t.dueTs=c.getLong(5); list.add(t);
        }
        c.close(); return list;
    }
    public void markTaskComplete(String id) {
        ContentValues cv = new ContentValues(); cv.put("completed",1);
        getWritableDatabase().update("google_tasks", cv, "id=?", new String[]{id});
    }

    // ── Contacts ──────────────────────────────────────────────────────────────
    public void upsertContact(String id, String name, String email, String phone) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("name",name); cv.put("email",email); cv.put("phone",phone);
        cv.put("synced_ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("google_contacts", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<GContact> searchContacts(String query) {
        List<GContact> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,name,email,phone FROM google_contacts WHERE name LIKE ? OR email LIKE ? ORDER BY name LIMIT 20",
            new String[]{"%" + query + "%", "%" + query + "%"});
        while (c.moveToNext()) {
            GContact ct=new GContact(); ct.id=c.getString(0); ct.name=c.getString(1); ct.email=c.getString(2); ct.phone=c.getString(3); list.add(ct);
        }
        c.close(); return list;
    }
    public List<GContact> getAllContacts() {
        List<GContact> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,email,phone FROM google_contacts ORDER BY name LIMIT 100", null);
        while (c.moveToNext()) {
            GContact ct=new GContact(); ct.id=c.getString(0); ct.name=c.getString(1); ct.email=c.getString(2); ct.phone=c.getString(3); list.add(ct);
        }
        c.close(); return list;
    }

    // ── Gmail ─────────────────────────────────────────────────────────────────
    public void upsertMessage(String id, String threadId, String from, String subject, String snippet, long ts, boolean unread) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("thread_id",threadId); cv.put("from_addr",from);
        cv.put("subject",subject); cv.put("snippet",snippet); cv.put("ts",ts);
        cv.put("unread",unread?1:0); cv.put("synced_ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("google_messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<GMessage> getRecentMessages(int limit) {
        List<GMessage> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,from_addr,subject,snippet,ts,unread FROM google_messages ORDER BY ts DESC LIMIT ?", new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            GMessage m=new GMessage(); m.id=c.getString(0); m.from=c.getString(1); m.subject=c.getString(2);
            m.snippet=c.getString(3); m.ts=c.getLong(4); m.unread=c.getInt(5)==1; list.add(m);
        }
        c.close(); return list;
    }
    public int getUnreadCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM google_messages WHERE unread=1", null);
        int n=0; if (c.moveToFirst()) n=c.getInt(0); c.close(); return n;
    }

    // ── Drive ─────────────────────────────────────────────────────────────────
    public void upsertFile(String id, String name, String mimeType, long size, long modifiedTs) {
        ContentValues cv = new ContentValues();
        cv.put("id",id); cv.put("name",name); cv.put("mime_type",mimeType);
        cv.put("size",size); cv.put("modified_ts",modifiedTs); cv.put("synced_ts",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("google_files", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<GFile> getDriveFiles() {
        List<GFile> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,mime_type,size,modified_ts FROM google_files ORDER BY modified_ts DESC LIMIT 50", null);
        while (c.moveToNext()) {
            GFile f=new GFile(); f.id=c.getString(0); f.name=c.getString(1); f.mimeType=c.getString(2);
            f.size=c.getLong(3); f.modifiedTs=c.getLong(4); list.add(f);
        }
        c.close(); return list;
    }
}
