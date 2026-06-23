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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Detects whether the Léa server is reachable on the local WiFi (192.168.1.102:8080)
 * or should fall back to Cloudflare Tunnel. Result is cached for CACHE_TTL_MS.
 *
 * Priority: LOCAL > CLOUDFLARE > OFFLINE
 */
public class LeaNetworkDetector {

    // ── Connection types ──────────────────────────────────────────────────────
    public static final String TYPE_LOCAL      = "LOCAL";
    public static final String TYPE_CLOUDFLARE = "CLOUDFLARE";
    public static final String TYPE_OFFLINE    = "OFFLINE";

    // ── Listener ──────────────────────────────────────────────────────────────
    public interface ConnectionListener {
        void onConnectionChanged(String newType, long latencyMs);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private static final String PREFS_CACHE  = "lea_net_detector_cache";
    private static final String KEY_TYPE     = "cached_type";
    private static final String KEY_LATENCY  = "cached_latency";
    private static final String KEY_TS       = "cached_ts";

    private static final List<ConnectionListener> listeners = new ArrayList<>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static volatile boolean probing = false;

    // ── Public API ────────────────────────────────────────────────────────────
    public static String getCachedType(Context ctx) {
        SharedPreferences p = prefs(ctx);
        long age = System.currentTimeMillis() - p.getLong(KEY_TS, 0);
        if (age > LeaNetworkConfig.CACHE_TTL_MS) return TYPE_CLOUDFLARE; // safe default until probed
        return p.getString(KEY_TYPE, TYPE_CLOUDFLARE);
    }

    public static long getCachedLatency(Context ctx) {
        return prefs(ctx).getLong(KEY_LATENCY, -1);
    }

    public static void addListener(ConnectionListener l) {
        synchronized (listeners) { if (!listeners.contains(l)) listeners.add(l); }
    }
    public static void removeListener(ConnectionListener l) {
        synchronized (listeners) { listeners.remove(l); }
    }

    /**
     * Asynchronously probes both endpoints and updates the cache.
     * Notifies listeners only if the connection type changed.
     */
    public static void probeAsync(Context ctx) {
        if (probing) return;
        probing = true;
        executor.submit(() -> {
            try {
                String prev = getCachedType(ctx);

                // Check internet availability first
                if (!isInternetAvailable(ctx)) {
                    updateCache(ctx, TYPE_OFFLINE, -1);
                    notifyIfChanged(prev, TYPE_OFFLINE, -1);
                    return;
                }

                // Try local first (low timeout = fast fail if not on LAN)
                long localLatency = tcpPing(LeaNetworkConfig.LOCAL_HOST, LeaNetworkConfig.LOCAL_PORT, LeaNetworkConfig.PROBE_TIMEOUT_MS);
                if (localLatency >= 0) {
                    LeaNetworkDatabase.get(ctx).recordConnection(TYPE_LOCAL, localLatency);
                    updateCache(ctx, TYPE_LOCAL, localLatency);
                    notifyIfChanged(prev, TYPE_LOCAL, localLatency);
                    LeaNetworkLogger.get(ctx).info(TYPE_LOCAL,
                        LeaNetworkConfig.LOCAL_HTTP, "PROBE_OK", "latency=" + localLatency + "ms");
                    return;
                }

                // Try Cloudflare
                long cfLatency = tcpPing(LeaNetworkConfig.CLOUDFLARE_HOST, LeaNetworkConfig.CLOUDFLARE_PORT, 5_000);
                if (cfLatency >= 0) {
                    LeaNetworkDatabase.get(ctx).recordConnection(TYPE_CLOUDFLARE, cfLatency);
                    updateCache(ctx, TYPE_CLOUDFLARE, cfLatency);
                    notifyIfChanged(prev, TYPE_CLOUDFLARE, cfLatency);
                    LeaNetworkLogger.get(ctx).info(TYPE_CLOUDFLARE,
                        LeaNetworkConfig.CLOUDFLARE_HTTP, "PROBE_OK", "latency=" + cfLatency + "ms");
                    return;
                }

                LeaNetworkDatabase.get(ctx).recordError(TYPE_CLOUDFLARE);
                updateCache(ctx, TYPE_OFFLINE, -1);
                notifyIfChanged(prev, TYPE_OFFLINE, -1);

            } finally {
                probing = false;
            }
        });
    }

    /** Synchronous probe — only call from a background thread. */
    public static String probeSync(Context ctx) {
        if (!isInternetAvailable(ctx)) return TYPE_OFFLINE;
        long local = tcpPing(LeaNetworkConfig.LOCAL_HOST, LeaNetworkConfig.LOCAL_PORT, LeaNetworkConfig.PROBE_TIMEOUT_MS);
        if (local >= 0) { updateCache(ctx, TYPE_LOCAL, local); return TYPE_LOCAL; }
        long cf = tcpPing(LeaNetworkConfig.CLOUDFLARE_HOST, LeaNetworkConfig.CLOUDFLARE_PORT, 5_000);
        if (cf >= 0) { updateCache(ctx, TYPE_CLOUDFLARE, cf); return TYPE_CLOUDFLARE; }
        updateCache(ctx, TYPE_OFFLINE, -1);
        return TYPE_OFFLINE;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static long tcpPing(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            long t0 = System.currentTimeMillis();
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return System.currentTimeMillis() - t0;
        } catch (Exception e) {
            return -1;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean isInternetAvailable(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
    }

    private static void updateCache(Context ctx, String type, long latencyMs) {
        prefs(ctx).edit()
            .putString(KEY_TYPE, type)
            .putLong(KEY_LATENCY, latencyMs)
            .putLong(KEY_TS, System.currentTimeMillis())
            .apply();
    }

    private static void notifyIfChanged(String prev, String next, long latencyMs) {
        if (!prev.equals(next)) {
            synchronized (listeners) {
                for (ConnectionListener l : listeners) l.onConnectionChanged(next, latencyMs);
            }
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE);
    }
}
