package com.flolov42.lea_v3.google;

import android.content.Context;
import com.flolov42.lea_v3.database.LeaPlusDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaGoogleTasks {

    private static final String BASE = "https://tasks.googleapis.com/tasks/v1";
    private static final int    XP_PER_TASK = 25;

    public interface TasksCallback  { void onSuccess(List<LeaGoogleDatabase.GTask> tasks); void onError(String err); }
    public interface ActionCallback { void onSuccess(); void onError(String err); }

    private final Context ctx;
    private final LeaGoogleAuth auth;
    private final LeaGoogleDatabase db;
    private final LeaPlusDatabase plusDb;

    private static LeaGoogleTasks instance;
    public static LeaGoogleTasks get(Context ctx) {
        if (instance == null) instance = new LeaGoogleTasks(ctx.getApplicationContext());
        return instance;
    }
    private LeaGoogleTasks(Context ctx) {
        this.ctx    = ctx;
        this.auth   = LeaGoogleAuth.get(ctx);
        this.db     = LeaGoogleDatabase.get(ctx);
        this.plusDb = LeaPlusDatabase.get(ctx);
    }

    // ── Sync depuis Google ────────────────────────────────────────────────────
    public void syncTasks(TasksCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    // 1. Récupérer les listes de tâches
                    JSONObject listsResp = httpGet(BASE + "/users/@me/lists", token);
                    JSONArray  lists     = listsResp.optJSONArray("items");
                    if (lists == null) { callback.onSuccess(db.getPendingTasks()); return; }

                    for (int i = 0; i < lists.length(); i++) {
                        JSONObject list   = lists.getJSONObject(i);
                        String listId    = list.optString("id");
                        // 2. Récupérer les tâches de chaque liste
                        JSONObject tasksResp = httpGet(BASE + "/lists/" + listId + "/tasks?maxResults=50&showCompleted=false", token);
                        JSONArray  items     = tasksResp.optJSONArray("items");
                        if (items == null) continue;
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject t    = items.getJSONObject(j);
                            String taskId   = t.optString("id");
                            String title    = t.optString("title", "(Sans titre)");
                            String notes    = t.optString("notes", "");
                            boolean done    = "completed".equals(t.optString("status"));
                            long dueTs      = parseDueDate(t.optString("due"));
                            db.upsertTask(taskId, listId, title, notes, done, dueTs);
                        }
                    }
                    callback.onSuccess(db.getPendingTasks());
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Compléter une tâche → +XP ─────────────────────────────────────────────
    public void completeTask(String taskId, String listId, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    // PATCH sur Google Tasks
                    JSONObject body = new JSONObject();
                    body.put("status", "completed");
                    httpPatch(BASE + "/lists/" + listId + "/tasks/" + taskId, token, body);

                    // Mise à jour locale + XP
                    db.markTaskComplete(taskId);
                    plusDb.addCoins(XP_PER_TASK, "Tâche Google complétée");
                    // Mettre à jour les stats de l'aventure
                    LeaPlusDatabase.CharStats stats = plusDb.getCharStats();
                    plusDb.updateChar(stats.level, stats.xp + XP_PER_TASK, stats.xpNext,
                        stats.hp, stats.world, stats.bossDefeated, stats.totalTasks + 1);
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Créer une tâche ───────────────────────────────────────────────────────
    public void createTask(String title, String notes, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    // Utiliser la liste par défaut "@default"
                    JSONObject body = new JSONObject();
                    body.put("title", title);
                    if (notes != null && !notes.isEmpty()) body.put("notes", notes);
                    JSONObject resp = httpPost(BASE + "/lists/@default/tasks", token, body);
                    String newId = resp.optString("id");
                    if (!newId.isEmpty()) {
                        db.upsertTask(newId, "@default", title, notes, false, 0);
                    }
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Supprimer une tâche ───────────────────────────────────────────────────
    public void deleteTask(String taskId, String listId, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    httpDelete(BASE + "/lists/" + listId + "/tasks/" + taskId, token);
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Cache local ───────────────────────────────────────────────────────────
    public List<LeaGoogleDatabase.GTask> getCachedTasks() {
        return db.getPendingTasks();
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────
    private JSONObject httpGet(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        if (conn.getResponseCode() >= 400) throw new Exception("HTTP " + conn.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close(); conn.disconnect();
        return new JSONObject(sb.toString());
    }
    private JSONObject httpPost(String urlStr, String token, JSONObject body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true); conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        OutputStream os = conn.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close(); conn.disconnect();
        return new JSONObject(sb.toString());
    }
    private void httpPatch(String urlStr, String token, JSONObject body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("PATCH");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true); conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        OutputStream os = conn.getOutputStream(); os.write(body.toString().getBytes("UTF-8")); os.close();
        conn.getResponseCode(); conn.disconnect();
    }
    private void httpDelete(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        conn.getResponseCode(); conn.disconnect();
    }

    private long parseDueDate(String due) {
        if (due == null || due.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            return sdf.parse(due).getTime();
        } catch (Exception e) { return 0; }
    }
}
