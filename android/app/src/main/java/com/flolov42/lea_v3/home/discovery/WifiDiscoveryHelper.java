package com.flolov42.lea_v3.home.discovery;

import com.flolov42.lea_v3.home.database.LeaHomeDatabase;
import com.flolov42.lea_v3.home.models.*;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class WifiDiscoveryHelper {

    private static final String TAG          = "WifiDiscovery";
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final long   TIMEOUT_MS   = 10_000;

    public interface WifiDiscoveryCallback {
        void onFound(SmartDevice device);
        void onFinished(List<SmartDevice> devices);
    }

    private final NsdManager nsdManager;
    private final LeaHomeDatabase db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private NsdManager.DiscoveryListener listener;
    private final List<SmartDevice> found = new ArrayList<>();
    private boolean running = false;

    public WifiDiscoveryHelper(Context ctx) {
        nsdManager = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
        db = LeaHomeDatabase.get(ctx);
    }

    public void startDiscovery(WifiDiscoveryCallback cb) {
        if (running) return;
        running = true;
        found.clear();

        listener = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                running = false;
                Log.e(TAG, "Discovery start failed: " + errorCode);
            }
            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: " + errorCode);
            }
            @Override public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "NSD discovery started");
            }
            @Override public void onDiscoveryStopped(String serviceType) {
                running = false;
            }
            @Override public void onServiceFound(NsdServiceInfo serviceInfo) {
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override public void onResolveFailed(NsdServiceInfo info, int errorCode) {}
                    @Override public void onServiceResolved(NsdServiceInfo info) {
                        SmartDevice d = new SmartDevice();
                        d.entityId     = "wifi." + info.getServiceName().replaceAll("\\s+", "_").toLowerCase();
                        d.friendlyName = info.getServiceName();
                        d.ipAddress    = info.getHost() != null ? info.getHost().getHostAddress() : "";
                        d.protocol     = Protocol.WIFI_DIRECT;
                        d.type         = DeviceType.SWITCH;
                        d.room         = "Général";
                        d.state        = "unknown";

                        synchronized (found) { found.add(d); }
                        db.upsertDevice(d);
                        mainHandler.post(() -> { if (cb != null) cb.onFound(d); });
                    }
                });
            }
            @Override public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener);

        mainHandler.postDelayed(() -> {
            stopDiscovery();
            if (cb != null) cb.onFinished(new ArrayList<>(found));
        }, TIMEOUT_MS);
    }

    public void stopDiscovery() {
        if (listener != null && running) {
            try { nsdManager.stopServiceDiscovery(listener); } catch (Exception ignored) {}
            listener = null;
            running = false;
        }
    }
}
