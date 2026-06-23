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
import android.content.SharedPreferences;

public final class LeaNetworkConfig {

    // ── URL constants ─────────────────────────────────────────────────────────
    public static final String LOCAL_HOST         = "192.168.1.102";
    public static final int    LOCAL_PORT          = 3001;
    public static final String LOCAL_HTTP          = "http://192.168.1.102:3001";
    public static final String LOCAL_WS            = "ws://192.168.1.102:3001";

    public static final String CLOUDFLARE_HOST     = "lea-bunker.lea-ia-local.com";
    public static final int    CLOUDFLARE_PORT      = 443;
    public static final String CLOUDFLARE_HTTP      = "https://lea-bunker.lea-ia-local.com";
    public static final String CLOUDFLARE_WS        = "wss://lea-bunker.lea-ia-local.com";

    // ── Detection tuning ──────────────────────────────────────────────────────
    /** Cache validity: active URL is not re-probed more often than this */
    public static final long   CACHE_TTL_MS         = 30_000L;
    /** TCP connect timeout when probing local server */
    public static final int    PROBE_TIMEOUT_MS      = 2_000;
    /** WebSocket reconnect back-off ceiling */
    public static final int    MAX_RECONNECT_DELAY_MS = 30_000;
    /** How many consecutive WS errors before forced failover */
    public static final int    ERROR_THRESHOLD        = 3;

    // ── SharedPreferences helpers ─────────────────────────────────────────────
    private static final String PREFS = "lea_network_prefs";
    private static final String KEY_PREFERRED = "preferred_connection"; // LOCAL | CLOUDFLARE | AUTO
    private static final String KEY_AUTO_SWITCH = "auto_switch_enabled";
    private static final String KEY_SPEED_TEST  = "speed_test_enabled";

    public static String getPreferred(Context ctx) {
        return prefs(ctx).getString(KEY_PREFERRED, "AUTO");
    }
    public static void setPreferred(Context ctx, String mode) {
        prefs(ctx).edit().putString(KEY_PREFERRED, mode).apply();
    }
    public static boolean isAutoSwitch(Context ctx) {
        return prefs(ctx).getBoolean(KEY_AUTO_SWITCH, true);
    }
    public static void setAutoSwitch(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(KEY_AUTO_SWITCH, v).apply();
    }
    public static boolean isSpeedTestEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SPEED_TEST, true);
    }
    public static void setSpeedTestEnabled(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(KEY_SPEED_TEST, v).apply();
    }

    // ── Active URL accessors (used by LeaBixbyService and other services) ─────
    /**
     * Returns the WebSocket URL to use right now.
     * Reads the cached detection result from LeaNetworkDetector.
     */
    public static String getWebSocketUrl(Context ctx) {
        String type = LeaNetworkDetector.getCachedType(ctx);
        if ("LOCAL".equals(type)) return LOCAL_WS;
        return CLOUDFLARE_WS;
    }

    public static String getHttpUrl(Context ctx) {
        String type = LeaNetworkDetector.getCachedType(ctx);
        if ("LOCAL".equals(type)) return LOCAL_HTTP;
        return CLOUDFLARE_HTTP;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private LeaNetworkConfig() {}
}
