package com.flolov42.lea_v3.offline;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

public class LeaOfflineManager {

    private static LeaOfflineManager instance;
    private final Context ctx;

    public static synchronized LeaOfflineManager get(Context ctx) {
        if (instance == null) instance = new LeaOfflineManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaOfflineManager(Context ctx) { this.ctx = ctx; }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    public String getStatusText() {
        return isOnline() ? "🟢 En ligne" : "🔴 Hors ligne";
    }

    public void queueAction(String type, String data) {
        if (!isOnline()) {
            LeaFeaturesDatabase.get(ctx).queueAction(type, data);
        }
    }

    public void syncPending() {
        if (!isOnline()) return;
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        java.util.List<LeaFeaturesDatabase.OfflineAction> pending = db.getPendingActions();
        for (LeaFeaturesDatabase.OfflineAction action : pending) {
            try {
                processAction(action);
                db.markActionSynced(action.id);
            } catch (Exception ignored) {}
        }
    }

    private void processAction(LeaFeaturesDatabase.OfflineAction action) {
        // Process queued actions by type
    }

    public int getPendingCount() {
        return LeaFeaturesDatabase.get(ctx).getPendingCount();
    }
}
