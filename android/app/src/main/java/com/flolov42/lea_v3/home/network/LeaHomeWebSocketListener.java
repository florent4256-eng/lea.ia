package com.flolov42.lea_v3.home.network;

import com.flolov42.lea_v3.home.database.LeaHomeDatabase;
import com.flolov42.lea_v3.home.models.HomeConfig;
import com.flolov42.lea_v3.home.models.SmartDevice;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class LeaHomeWebSocketListener {

    private static final String TAG = "LeaHomeWS";

    // Backoff exponentiel : 5s → 15s → 45s → 2min → 5min → 10min (plafond)
    private static final long   BACKOFF_MIN_MS  = 5_000L;
    private static final long   BACKOFF_MAX_MS  = 10 * 60 * 1000L;  // 10 min
    private static final int    BACKOFF_FACTOR  = 3;

    public interface StateChangeListener {
        void onDeviceStateChanged(SmartDevice device);
        void onConnected();
        void onDisconnected();
    }

    private final Context ctx;
    private final HomeConfig config;
    private final LeaHomeDatabase db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private StateChangeListener listener;
    private WebSocketClient ws;
    private boolean intentionalClose = false;
    private long currentBackoffMs    = BACKOFF_MIN_MS;

    public LeaHomeWebSocketListener(Context ctx) {
        this.ctx    = ctx.getApplicationContext();
        this.config = HomeConfig.get(ctx);
        this.db     = LeaHomeDatabase.get(ctx);
    }

    public void setListener(StateChangeListener l) { this.listener = l; }

    public void connect() {
        intentionalClose  = false;
        currentBackoffMs  = BACKOFF_MIN_MS;  // reset au 1er appel explicite
        doConnect();
    }

    private void doConnect() {
        if (intentionalClose) return;
        String wsUrl = config.getWsUrl();
        if (wsUrl == null || wsUrl.isEmpty()) {
            Log.d(TAG, "URL WebSocket non configurée — connexion ignorée");
            return;
        }
        try {
            URI uri = URI.create(wsUrl);
            ws = new WebSocketClient(uri) {
                @Override public void onOpen(ServerHandshake hs) {
                    Log.d(TAG, "WS connecté");
                    currentBackoffMs = BACKOFF_MIN_MS;  // succès → reset backoff
                    sendIdentify();
                    mainHandler.post(() -> { if (listener != null) listener.onConnected(); });
                }
                @Override public void onMessage(String message) {
                    handleMessage(message);
                }
                @Override public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WS fermé: " + reason);
                    mainHandler.post(() -> { if (listener != null) listener.onDisconnected(); });
                    scheduleReconnect();
                }
                @Override public void onError(Exception e) {
                    Log.w(TAG, "WS erreur: " + e.getMessage());
                    scheduleReconnect();
                }
            };
            ws.connectBlocking();
        } catch (Exception e) {
            Log.w(TAG, "Connexion échouée: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void sendIdentify() {
        try {
            ws.send("{\"type\":\"HOME_LISTENER\",\"client\":\"lea-home\"}");
        } catch (Exception ignored) {}
    }

    private void handleMessage(String msg) {
        try {
            JsonObject obj  = JsonParser.parseString(msg).getAsJsonObject();
            String type     = obj.has("type") ? obj.get("type").getAsString() : "";
            if ("STATE_CHANGED".equals(type) && obj.has("entity_id")) {
                String entityId = obj.get("entity_id").getAsString();
                String state    = obj.has("state") ? obj.get("state").getAsString() : "";
                String attrs    = obj.has("attributes") ? obj.getAsJsonObject("attributes").toString() : null;
                db.updateDeviceState(entityId, state, attrs);
                SmartDevice dummy = new SmartDevice();
                dummy.entityId   = entityId;
                dummy.state      = state;
                dummy.attributes = attrs;
                mainHandler.post(() -> { if (listener != null) listener.onDeviceStateChanged(dummy); });
            }
        } catch (Exception e) {
            Log.w(TAG, "Erreur parse: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (intentionalClose) return;
        Log.d(TAG, "Reconnexion dans " + currentBackoffMs / 1000 + "s");
        mainHandler.postDelayed(this::doConnect, currentBackoffMs);
        // Augmente le délai pour la prochaine tentative (plafond 10 min)
        currentBackoffMs = Math.min(currentBackoffMs * BACKOFF_FACTOR, BACKOFF_MAX_MS);
    }

    public void disconnect() {
        intentionalClose = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (ws != null) { try { ws.close(); } catch (Exception ignored) {} }
    }

    public boolean isConnected() {
        return ws != null && ws.isOpen();
    }
}
