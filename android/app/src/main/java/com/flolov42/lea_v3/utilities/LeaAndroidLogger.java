package com.flolov42.lea_v3.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class LeaAndroidLogger {

    private static final String PREFS       = "LeaPrefs";
    private static final String KEY_BUFFER  = "lea_log_buffer";
    private static final int    MAX_BUFFER  = 200;
    private static final OkHttpClient http  = new OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    // ── API publique ─────────────────────────────────────────────────────────

    /** Log dédié aux échanges vocaux avec Léa — level VOIX, flush immédiat. */
    public static void voice(Context ctx, String tag, String msg) {
        append(ctx, "VOIX", tag, msg);
        flushAsync(ctx);
    }

    public static void nav(Context ctx, String screen) {
        append(ctx, "NAV", screen, "Ouverture : " + screen);
    }

    public static void feature(Context ctx, String featureId, String action) {
        append(ctx, "FEAT", featureId, action);
    }

    public static void info(Context ctx, String tag, String msg) {
        append(ctx, "INFO", tag, msg);
    }

    public static void warn(Context ctx, String tag, String msg) {
        append(ctx, "WARN", tag, msg);
    }

    public static void net(Context ctx, String tag, String msg) {
        append(ctx, "NET", tag, msg);
    }

    public static void error(Context ctx, String tag, String msg) {
        append(ctx, "ERROR", tag, msg);
        flushAsync(ctx);
    }

    public static void error(Context ctx, String tag, String msg, Throwable t) {
        String full = msg;
        if (t != null) full += " | " + t.getClass().getSimpleName() + ": " + t.getMessage();
        append(ctx, "ERROR", tag, full);
        flushAsync(ctx);
    }

    public static void crash(Context ctx, String msg, Throwable t) {
        String full = msg;
        if (t != null) {
            full += " | " + t.getClass().getSimpleName() + ": " + t.getMessage();
            // Ajoute les 3 premières lignes du stack trace
            StackTraceElement[] stack = t.getStackTrace();
            for (int i = 0; i < Math.min(3, stack.length); i++) {
                full += "\n  at " + stack[i].toString();
            }
        }
        append(ctx, "CRASH", "UncaughtException", full);
        flush(ctx); // synchrone pour s'assurer que ça part avant le crash
    }

    // ── Envoi asynchrone ─────────────────────────────────────────────────────

    public static void flushAsync(Context ctx) {
        new Thread(() -> flush(ctx)).start();
    }

    public static void flush(Context ctx) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_BUFFER, "[]");
            JSONArray arr = new JSONArray(raw);
            if (arr.length() == 0) return;

            // Utilise la même URL que le WebSocket (réseau local ou Cloudflare)
            String serverUrl = LeaNetworkConfig.getHttpUrl(ctx);
            JSONObject payload = new JSONObject();
            payload.put("logs", arr);
            payload.put("appVersion", "2.0.0");

            Request req = new Request.Builder()
                .url(serverUrl + "/api/android-logs")
                .post(RequestBody.create(payload.toString(), MediaType.parse("application/json")))
                .build();

            try (Response resp = http.newCall(req).execute()) {
                if (resp.isSuccessful()) {
                    prefs.edit().putString(KEY_BUFFER, "[]").apply();
                }
            }
        } catch (Exception ignored) {
            // Silencieux — on ne veut pas crasher l'app à cause du logger
        }
    }

    // ── URL helper (partagé avec le reste de l'app) ──────────────────────────

    public static String resolveServerUrl(String host) {
        if (host == null || host.isEmpty()) host = "http://lea-bunker.lea-ia-local.com";
        // HTTPS = Cloudflare tunnel → port 443 géré automatiquement, pas de :3001
        if (host.startsWith("https://")) return host;
        // HTTP → ajouter :3001 si aucun port spécifié
        if (host.startsWith("http://")) {
            if (!host.matches(".*:\\d+/?$")) host += ":3001";
            return host;
        }
        // Pas de schéma → IP locale ou hostname brut
        return "http://" + host + ":3001";
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private static synchronized void append(Context ctx, String level, String tag, String msg) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_BUFFER, "[]");
            JSONArray arr = new JSONArray(raw);

            JSONObject entry = new JSONObject();
            entry.put("ts",     System.currentTimeMillis());
            entry.put("level",  level);
            entry.put("tag",    tag);
            entry.put("msg",    msg);
            entry.put("user",   prefs.getString("lea_session_user", "?").replace("\"", "").trim());
            entry.put("device", Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");
            arr.put(entry);

            // Roulement — on garde au max MAX_BUFFER entrées
            if (arr.length() > MAX_BUFFER) {
                JSONArray trimmed = new JSONArray();
                for (int i = arr.length() - MAX_BUFFER; i < arr.length(); i++) trimmed.put(arr.get(i));
                arr = trimmed;
            }

            prefs.edit().putString(KEY_BUFFER, arr.toString()).apply();
            Log.d("LÉA[" + level + "][" + tag + "]", msg);
        } catch (Exception ignored) {}
    }
}
