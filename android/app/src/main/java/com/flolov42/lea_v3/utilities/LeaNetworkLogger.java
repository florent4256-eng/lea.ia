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
import android.util.Log;

/**
 * Thin facade over LeaNetworkDatabase for structured network event logging.
 * All writes are fire-and-forget (caller thread); DB retains 7-day window.
 */
public class LeaNetworkLogger {

    private static final String TAG = "LeaNetwork";

    private static LeaNetworkLogger instance;
    public static synchronized LeaNetworkLogger get(Context ctx) {
        if (instance == null) instance = new LeaNetworkLogger(ctx.getApplicationContext());
        return instance;
    }

    private final LeaNetworkDatabase db;
    private LeaNetworkLogger(Context ctx) { db = LeaNetworkDatabase.get(ctx); }

    // ── Log helpers ───────────────────────────────────────────────────────────
    public void info(String type, String url, String event, String details) {
        Log.i(TAG, "[" + type + "] " + event + " — " + details);
        db.log(type, url, event, details, -1);
    }

    public void latency(String type, String url, String event, long latencyMs) {
        Log.d(TAG, "[" + type + "] " + event + " " + latencyMs + "ms");
        db.log(type, url, event, "latency=" + latencyMs + "ms", latencyMs);
    }

    public void error(String type, String url, String event, String error) {
        Log.e(TAG, "[" + type + "] " + event + " ERROR: " + error);
        db.log(type, url, event, "ERROR: " + error, -1);
        db.recordError(type);
    }

    public void switchEvent(String fromType, String toType, String reason) {
        String msg = fromType + " → " + toType + " (" + reason + ")";
        Log.i(TAG, "SWITCH " + msg);
        db.log(toType, "", "SWITCH", msg, -1);
    }

    public void wsConnected(String type, String url, long latencyMs) {
        db.log(type, url, "WS_CONNECTED", "WebSocket connected", latencyMs);
        db.recordConnection(type, latencyMs);
        Log.i(TAG, "[" + type + "] WS connected to " + url + " (" + latencyMs + "ms)");
    }

    public void wsDisconnected(String type, String url, int code, String reason) {
        db.log(type, url, "WS_DISCONNECTED", "code=" + code + " reason=" + reason, -1);
        Log.w(TAG, "[" + type + "] WS disconnected code=" + code + " " + reason);
    }

    public void wsError(String type, String url, String error) {
        db.log(type, url, "WS_ERROR", error, -1);
        db.recordError(type);
        Log.e(TAG, "[" + type + "] WS error: " + error);
    }
}
