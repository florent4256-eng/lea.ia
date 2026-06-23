package com.flolov42.lea_v3.google;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class LeaGoogleCalendar {

    private static final String BASE = "https://www.googleapis.com/calendar/v3";

    public interface CalendarCallback { void onSuccess(List<LeaGoogleDatabase.CalEvent> events); void onError(String err); }
    public interface ActionCallback   { void onSuccess(); void onError(String err); }
    public interface FreeSlotsCallback { void onSlots(List<long[]> slots); void onError(String err); }

    private final Context ctx;
    private final LeaGoogleAuth auth;
    private final LeaGoogleDatabase db;

    private static LeaGoogleCalendar instance;
    public static LeaGoogleCalendar get(Context ctx) {
        if (instance == null) instance = new LeaGoogleCalendar(ctx.getApplicationContext());
        return instance;
    }
    private LeaGoogleCalendar(Context ctx) {
        this.ctx  = ctx;
        this.auth = LeaGoogleAuth.get(ctx);
        this.db   = LeaGoogleDatabase.get(ctx);
    }

    // ── Sync: fetch events from Google ────────────────────────────────────────
    public void syncEvents(CalendarCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    // Plage: maintenant → +30 jours
                    long now     = System.currentTimeMillis();
                    long endTime = now + 30L * 24 * 3600 * 1000;
                    SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    rfc3339.setTimeZone(TimeZone.getTimeZone("UTC"));

                    String url = BASE + "/calendars/primary/events"
                               + "?maxResults=50&orderBy=startTime&singleEvents=true"
                               + "&timeMin=" + rfc3339.format(new Date(now))
                               + "&timeMax=" + rfc3339.format(new Date(endTime));

                    JSONObject resp = httpGet(url, token);
                    JSONArray items = resp.optJSONArray("items");
                    if (items == null) { callback.onError("Aucun événement"); return; }

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject ev = items.getJSONObject(i);
                        String id    = ev.optString("id");
                        String title = ev.optString("summary", "(Sans titre)");
                        String loc   = ev.optString("location", "");
                        String desc  = ev.optString("description", "");

                        JSONObject start = ev.optJSONObject("start");
                        JSONObject end   = ev.optJSONObject("end");
                        long startTs = parseDateTime(start);
                        long endTs   = parseDateTime(end);

                        db.upsertEvent(id, title, loc, desc, startTs, endTs);
                    }
                    callback.onSuccess(db.getUpcomingEvents(50));
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Créer un événement sur Google ─────────────────────────────────────────
    public void createEvent(String title, long startMs, long endMs, String description, String location, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    rfc3339.setTimeZone(TimeZone.getTimeZone("UTC"));

                    JSONObject body = new JSONObject();
                    body.put("summary", title);
                    body.put("description", description);
                    body.put("location", location);

                    JSONObject startObj = new JSONObject(); startObj.put("dateTime", rfc3339.format(new Date(startMs))); startObj.put("timeZone","UTC");
                    JSONObject endObj   = new JSONObject(); endObj.put("dateTime",   rfc3339.format(new Date(endMs)));   endObj.put("timeZone","UTC");
                    body.put("start", startObj); body.put("end", endObj);

                    JSONObject resp = httpPost(BASE + "/calendars/primary/events", token, body);
                    String newId = resp.optString("id");
                    if (!newId.isEmpty()) {
                        db.upsertEvent(newId, title, location, description, startMs, endMs);
                    }
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Supprimer un événement ────────────────────────────────────────────────
    public void deleteEvent(String eventId, ActionCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    httpDelete(BASE + "/calendars/primary/events/" + eventId, token);
                    callback.onSuccess();
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Créneaux libres (pour proposer des quêtes) ───────────────────────────
    public void detectFreeSlots(FreeSlotsCallback callback) {
        syncEvents(new CalendarCallback() {
            @Override public void onSuccess(List<LeaGoogleDatabase.CalEvent> events) {
                List<long[]> freeSlots = new ArrayList<>();
                long now = System.currentTimeMillis();
                // Trouver des créneaux de 60min+ dans les prochaines 48h
                for (int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); h < 48; h++) {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.HOUR_OF_DAY, h);
                    c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
                    long slotStart = c.getTimeInMillis();
                    long slotEnd   = slotStart + 60 * 60 * 1000L;
                    boolean busy = false;
                    for (LeaGoogleDatabase.CalEvent ev : events) {
                        if (ev.startTs < slotEnd && ev.endTs > slotStart) { busy = true; break; }
                    }
                    if (!busy) freeSlots.add(new long[]{slotStart, slotEnd});
                    if (freeSlots.size() >= 5) break;
                }
                callback.onSlots(freeSlots);
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Local cache ───────────────────────────────────────────────────────────
    public List<LeaGoogleDatabase.CalEvent> getCachedEvents() {
        return db.getAllEvents();
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────
    private JSONObject httpGet(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        int code = conn.getResponseCode();
        if (code >= 400) throw new Exception("HTTP " + code);
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
        byte[] input = body.toString().getBytes("UTF-8");
        OutputStream os = conn.getOutputStream(); os.write(input); os.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close(); conn.disconnect();
        return new JSONObject(sb.toString());
    }
    private void httpDelete(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        conn.getResponseCode(); conn.disconnect();
    }

    private long parseDateTime(JSONObject obj) {
        if (obj == null) return 0;
        try {
            String dt = obj.optString("dateTime");
            if (!dt.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
                return sdf.parse(dt).getTime();
            }
            String d = obj.optString("date");
            if (!d.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(d).getTime();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
