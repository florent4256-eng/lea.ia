package com.flolov42.lea_v3.google;

import android.content.Context;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class LeaGoogleGmail {

    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me";

    public interface MessagesCallback { void onSuccess(List<LeaGoogleDatabase.GMessage> messages); void onError(String err); }
    public interface ActionCallback   { void onSuccess(); void onError(String err); }

    private final Context ctx;
    private final LeaGoogleAuth auth;
    private final LeaGoogleDatabase db;

    private static LeaGoogleGmail instance;
    public static LeaGoogleGmail get(Context ctx) {
        if (instance == null) instance = new LeaGoogleGmail(ctx.getApplicationContext());
        return instance;
    }
    private LeaGoogleGmail(Context ctx) {
        this.ctx  = ctx;
        this.auth = LeaGoogleAuth.get(ctx);
        this.db   = LeaGoogleDatabase.get(ctx);
    }

    // ── Sync messages récents ─────────────────────────────────────────────────
    public void syncMessages(MessagesCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    // 1. Liste des IDs de messages récents
                    JSONObject listResp = httpGet(BASE + "/messages?maxResults=20&labelIds=INBOX", token);
                    JSONArray  messages = listResp.optJSONArray("messages");
                    if (messages == null) { callback.onSuccess(db.getRecentMessages(20)); return; }

                    // 2. Récupérer le détail de chaque message (metadata seulement)
                    for (int i = 0; i < Math.min(messages.length(), 20); i++) {
                        String msgId = messages.getJSONObject(i).optString("id");
                        try {
                            JSONObject detail = httpGet(BASE + "/messages/" + msgId + "?format=metadata&metadataHeaders=From,Subject,Date", token);
                            parseAndStoreMessage(detail);
                        } catch (Exception ignored) {}
                    }
                    callback.onSuccess(db.getRecentMessages(20));
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    private void parseAndStoreMessage(JSONObject msg) throws Exception {
        String id       = msg.optString("id");
        String threadId = msg.optString("threadId");
        String snippet  = msg.optString("snippet", "");
        long internalTs = msg.optLong("internalDate", 0);
        boolean unread  = false;

        JSONArray labels = msg.optJSONArray("labelIds");
        if (labels != null) {
            for (int i = 0; i < labels.length(); i++) {
                if ("UNREAD".equals(labels.getString(i))) { unread = true; break; }
            }
        }

        String from    = "";
        String subject = "";
        JSONObject payload = msg.optJSONObject("payload");
        if (payload != null) {
            JSONArray headers = payload.optJSONArray("headers");
            if (headers != null) {
                for (int i = 0; i < headers.length(); i++) {
                    JSONObject h = headers.getJSONObject(i);
                    String name = h.optString("name").toLowerCase();
                    if ("from".equals(name))    from    = h.optString("value");
                    if ("subject".equals(name)) subject = h.optString("value");
                }
            }
        }
        db.upsertMessage(id, threadId, from, subject, snippet, internalTs, unread);
    }

    // ── Envoyer un email ──────────────────────────────────────────────────────
    public void sendEmail(String to, String subject, String body, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    String rawEmail = "To: " + to + "\r\n"
                                   + "Subject: " + subject + "\r\n"
                                   + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                                   + body;
                    String encoded = Base64.encodeToString(rawEmail.getBytes("UTF-8"),
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                    JSONObject reqBody = new JSONObject(); reqBody.put("raw", encoded);
                    httpPost(BASE + "/messages/send", token, reqBody);
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Archiver un message ───────────────────────────────────────────────────
    public void archiveMessage(String messageId, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("removeLabelIds", new JSONArray().put("INBOX").put("UNREAD"));
                    httpPost(BASE + "/messages/" + messageId + "/modify", token, body);
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Cache local ───────────────────────────────────────────────────────────
    public List<LeaGoogleDatabase.GMessage> getCachedMessages() {
        return db.getRecentMessages(20);
    }
    public int getUnreadCount() {
        return db.getUnreadCount();
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
        if (conn.getResponseCode() >= 400) throw new Exception("HTTP " + conn.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close(); conn.disconnect();
        return new JSONObject(sb.toString());
    }
}
