package com.flolov42.lea_v3.google;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class LeaGoogleContacts {

    private static final String BASE = "https://people.googleapis.com/v1";

    public interface ContactsCallback { void onSuccess(List<LeaGoogleDatabase.GContact> contacts); void onError(String err); }

    private final Context ctx;
    private final LeaGoogleAuth auth;
    private final LeaGoogleDatabase db;

    private static LeaGoogleContacts instance;
    public static LeaGoogleContacts get(Context ctx) {
        if (instance == null) instance = new LeaGoogleContacts(ctx.getApplicationContext());
        return instance;
    }
    private LeaGoogleContacts(Context ctx) {
        this.ctx  = ctx;
        this.auth = LeaGoogleAuth.get(ctx);
        this.db   = LeaGoogleDatabase.get(ctx);
    }

    // ── Importer les contacts Google ──────────────────────────────────────────
    public void syncContacts(ContactsCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    String url = BASE + "/people/me/connections"
                               + "?personFields=names,emailAddresses,phoneNumbers"
                               + "&pageSize=200&sortOrder=FIRST_NAME_ASCENDING";

                    JSONObject resp = httpGet(url, token);
                    JSONArray connections = resp.optJSONArray("connections");
                    if (connections == null) { callback.onSuccess(db.getAllContacts()); return; }

                    for (int i = 0; i < connections.length(); i++) {
                        JSONObject person = connections.getJSONObject(i);
                        String resourceName = person.optString("resourceName");

                        // Nom
                        String name = "(Sans nom)";
                        JSONArray names = person.optJSONArray("names");
                        if (names != null && names.length() > 0) {
                            name = names.getJSONObject(0).optString("displayName", "(Sans nom)");
                        }
                        // Email
                        String email = "";
                        JSONArray emails = person.optJSONArray("emailAddresses");
                        if (emails != null && emails.length() > 0) {
                            email = emails.getJSONObject(0).optString("value", "");
                        }
                        // Téléphone
                        String phone = "";
                        JSONArray phones = person.optJSONArray("phoneNumbers");
                        if (phones != null && phones.length() > 0) {
                            phone = phones.getJSONObject(0).optString("value", "");
                        }

                        db.upsertContact(resourceName, name, email, phone);
                    }
                    callback.onSuccess(db.getAllContacts());
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Recherche ─────────────────────────────────────────────────────────────
    public List<LeaGoogleDatabase.GContact> searchContacts(String query) {
        return db.searchContacts(query);
    }

    // ── Cache ─────────────────────────────────────────────────────────────────
    public List<LeaGoogleDatabase.GContact> getCachedContacts() {
        return db.getAllContacts();
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────
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
}
