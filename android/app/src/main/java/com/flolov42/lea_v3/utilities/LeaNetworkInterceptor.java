package com.flolov42.lea_v3.utilities;

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

import android.content.Context;

/**
 * Intercepts WebSocket connection errors and orchestrates failover between
 * LOCAL → CLOUDFLARE → OFFLINE.  Called by LeaNovaService on each error/close.
 *
 * Usage:
 *   LeaNetworkInterceptor interceptor = LeaNetworkInterceptor.get(ctx);
 *   interceptor.onConnectionError(currentType, () -> service.reconnect());
 */
public class LeaNetworkInterceptor {

    public interface ReconnectCallback {
        void reconnectWith(String newType, String newWsUrl);
    }

    private static LeaNetworkInterceptor instance;
    public static synchronized LeaNetworkInterceptor get(Context ctx) {
        if (instance == null) instance = new LeaNetworkInterceptor(ctx.getApplicationContext());
        return instance;
    }

    private final Context ctx;
    private int consecutiveErrors = 0;
    private long lastErrorTs = 0;
    private String currentForcedType = null; // null = AUTO

    private LeaNetworkInterceptor(Context ctx) { this.ctx = ctx; }

    // ── Called when WS connect() fails or onError() fires ────────────────────
    public void onConnectionError(String currentType, ReconnectCallback cb) {
        consecutiveErrors++;
        lastErrorTs = System.currentTimeMillis();
        LeaNetworkLogger.get(ctx).error(currentType, LeaNetworkConfig.getWebSocketUrl(ctx),
            "WS_CONNECT_ERROR", "consecutive=" + consecutiveErrors);

        if (consecutiveErrors >= LeaNetworkConfig.ERROR_THRESHOLD) {
            forceFailover(currentType, cb);
        } else {
            // Exponential back-off before retry on same URL
            long delay = Math.min(1_000L * (1L << consecutiveErrors), LeaNetworkConfig.MAX_RECONNECT_DELAY_MS);
            scheduleReconnect(delay, currentType, cb);
        }
    }

    // ── Called when WS connects successfully ─────────────────────────────────
    public void onConnectionSuccess(String type, long latencyMs) {
        consecutiveErrors = 0;
        currentForcedType = null;
        LeaNetworkLogger.get(ctx).wsConnected(type, LeaNetworkConfig.getWebSocketUrl(ctx), latencyMs);
    }

    // ── Called when WS is closed cleanly ─────────────────────────────────────
    public void onConnectionClosed(String type, int code, String reason) {
        LeaNetworkLogger.get(ctx).wsDisconnected(type, LeaNetworkConfig.getWebSocketUrl(ctx), code, reason);
        if (code != 1000) { // not a normal close → treat as error
            onConnectionError(type, null);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────
    private void forceFailover(String fromType, ReconnectCallback cb) {
        consecutiveErrors = 0;
        String newType;
        String newUrl;
        if (LeaNetworkDetector.TYPE_LOCAL.equals(fromType)) {
            // LOCAL failed → try CLOUDFLARE
            newType = LeaNetworkDetector.TYPE_CLOUDFLARE;
            newUrl  = LeaNetworkConfig.CLOUDFLARE_WS;
        } else {
            // CLOUDFLARE also failed → force re-probe
            newType = LeaNetworkDetector.probeSync(ctx);
            newUrl  = LeaNetworkConfig.CLOUDFLARE_WS;
            if (LeaNetworkDetector.TYPE_LOCAL.equals(newType)) newUrl = LeaNetworkConfig.LOCAL_WS;
        }
        currentForcedType = newType;
        LeaNetworkLogger.get(ctx).switchEvent(fromType, newType, "error_threshold");
        // Update cached type so getWebSocketUrl() returns new value immediately
        if (cb != null) cb.reconnectWith(newType, newUrl);
    }

    private void scheduleReconnect(long delayMs, String type, ReconnectCallback cb) {
        if (cb == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            String url = LeaNetworkDetector.TYPE_LOCAL.equals(type)
                ? LeaNetworkConfig.LOCAL_WS : LeaNetworkConfig.CLOUDFLARE_WS;
            cb.reconnectWith(type, url);
        }, delayMs);
    }
}
