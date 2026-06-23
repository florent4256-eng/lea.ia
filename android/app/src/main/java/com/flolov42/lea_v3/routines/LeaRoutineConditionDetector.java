package com.flolov42.lea_v3.routines;

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

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.List;

/**
 * Service de détection des conditions — toutes les conditions supportées :
 * TIME_EXACT · TIME · DAY_OF_WEEK · LOCATION · BLUETOOTH_CONNECTED ·
 * WIFI_CONNECTED · HEADPHONES_CONNECTED · CHARGING · DISCHARGING ·
 * BATTERY_LEVEL · SCREEN_ON · SCREEN_OFF · INCOMING_CALL · CALL_ENDED
 */
public class LeaRoutineConditionDetector extends Service {

    private static final String TAG = "LeaConditionDetector";

    private LeaRoutineDatabase            db;
    private LeaRoutineActionsExecutor     executor;
    private LeaRoutineNotificationManager notifMgr;

    private LocationManager  locationManager;
    private LocationListener locationListener;

    private BroadcastReceiver btReceiver;
    private BroadcastReceiver timeReceiver;
    private BroadcastReceiver powerReceiver;
    private BroadcastReceiver screenReceiver;
    private BroadcastReceiver headsetReceiver;
    private BroadcastReceiver callReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        db       = LeaRoutineDatabase.get(this);
        executor = new LeaRoutineActionsExecutor(this);
        notifMgr = new LeaRoutineNotificationManager(this);

        startLocationMonitoring();
        startBluetoothMonitoring();
        startTimeMonitoring();
        startPowerMonitoring();
        startScreenMonitoring();
        startHeadsetMonitoring();
        startCallMonitoring();
    }

    // ── LOCALISATION ─────────────────────────────────────────────────────────

    private void startLocationMonitoring() {
        try {
            locationManager  = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override public void onLocationChanged(Location loc) { checkLocationConditions(loc); }
                @Override public void onStatusChanged(String p, int s, Bundle e) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 60_000L, 50f, locationListener);
            }
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 60_000L, 50f, locationListener);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "startLocationMonitoring: " + e.getMessage());
        }
    }

    private void checkLocationConditions(Location loc) {
        for (LeaRoutineDatabase.RoutineRow r : db.getAllRoutines()) {
            try {
                JSONArray conds = new JSONArray(r.conditionsJson != null ? r.conditionsJson : "[]");
                for (int i = 0; i < conds.length(); i++) {
                    JSONObject c = conds.getJSONObject(i);
                    if (!"LOCATION".equals(c.optString("type"))) continue;
                    double lat    = c.optDouble("lat",    0);
                    double lng    = c.optDouble("lng",    0);
                    float  radius = (float) c.optDouble("radius", 100);
                    if (lat == 0 && lng == 0) continue;
                    float[] dist = new float[1];
                    Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), lat, lng, dist);
                    boolean inZone = dist[0] <= radius;
                    if (inZone  && !r.active && r.userEnabled) activateRoutine(r);
                    if (!inZone && r.active  && !r.userEnabled) deactivateRoutine(r);
                }
            } catch (Exception e) { Log.e(TAG, "checkLocation: " + e.getMessage()); }
        }
    }

    // ── BLUETOOTH ────────────────────────────────────────────────────────────

    private void startBluetoothMonitoring() {
        btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = "";
                try { if (device != null) name = device.getName(); } catch (Exception ignored) {}
                boolean connected = BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction());
                checkBluetoothConditions(name != null ? name : "", connected);
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        f.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btReceiver, f);
    }

    private void checkBluetoothConditions(String deviceName, boolean connected) {
        for (LeaRoutineDatabase.RoutineRow r : db.getAllRoutines()) {
            try {
                JSONArray conds = new JSONArray(r.conditionsJson != null ? r.conditionsJson : "[]");
                for (int i = 0; i < conds.length(); i++) {
                    JSONObject c = conds.getJSONObject(i);
                    if (!"BLUETOOTH_CONNECTED".equals(c.optString("type"))) continue;
                    String target = c.optString("device", "");
                    if (!target.isEmpty() && !target.equalsIgnoreCase(deviceName)) continue;
                    if (connected  && !r.active && r.userEnabled) activateRoutine(r);
                    if (!connected && r.active  && !r.userEnabled) deactivateRoutine(r);
                }
            } catch (Exception e) { Log.e(TAG, "checkBluetooth: " + e.getMessage()); }
        }
    }

    // ── HEURE / JOUR ─────────────────────────────────────────────────────────

    private void startTimeMonitoring() {
        timeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) checkTimeConditions();
            }
        };
        registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    private void checkTimeConditions() {
        Calendar now      = Calendar.getInstance();
        int      nowH     = now.get(Calendar.HOUR_OF_DAY);
        int      nowM     = now.get(Calendar.MINUTE);
        int      nowTotal = nowH * 60 + nowM;
        int      nowDay   = now.get(Calendar.DAY_OF_WEEK); // 1=dim, 2=lun … 7=sam

        for (LeaRoutineDatabase.RoutineRow r : db.getAllRoutines()) {
            try {
                JSONArray conds = new JSONArray(r.conditionsJson != null ? r.conditionsJson : "[]");
                for (int i = 0; i < conds.length(); i++) {
                    JSONObject c    = conds.getJSONObject(i);
                    String     type = c.optString("type", "");

                    switch (type) {
                        case "TIME_EXACT": {
                            String[] p = c.optString("time", "00:00").split(":");
                            int h = Integer.parseInt(p[0]);
                            int m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
                            if (nowH == h && nowM == m && !r.active && r.userEnabled) activateRoutine(r);
                            break;
                        }
                        case "TIME": {
                            String[] sp = c.optString("start", "00:00").split(":");
                            String[] ep = c.optString("end",   "00:00").split(":");
                            int s = Integer.parseInt(sp[0]) * 60 + (sp.length > 1 ? Integer.parseInt(sp[1]) : 0);
                            int e = Integer.parseInt(ep[0]) * 60 + (ep.length > 1 ? Integer.parseInt(ep[1]) : 0);
                            boolean inRange = (s <= e) ? (nowTotal >= s && nowTotal <= e)
                                                       : (nowTotal >= s || nowTotal <= e);
                            if (inRange  && !r.active && r.userEnabled) activateRoutine(r);
                            if (!inRange && r.active  && !r.userEnabled) deactivateRoutine(r);
                            break;
                        }
                        case "DAY_OF_WEEK": {
                            // Conversion : Calendar.SUNDAY=1, MONDAY=2 … → notre format 1=lun, 7=dim
                            int leaDay = (nowDay == Calendar.SUNDAY) ? 7 : nowDay - 1;
                            JSONArray days = c.optJSONArray("days");
                            boolean match = false;
                            if (days != null) {
                                for (int d = 0; d < days.length(); d++) {
                                    if (days.getInt(d) == leaDay) { match = true; break; }
                                }
                            }
                            if (match  && !r.active && r.userEnabled) activateRoutine(r);
                            if (!match && r.active  && !r.userEnabled) deactivateRoutine(r);
                            break;
                        }
                    }
                }
            } catch (Exception e) { Log.e(TAG, "checkTime: " + e.getMessage()); }
        }
        refreshNotification();
    }

    // ── CHARGE / BATTERIE ─────────────────────────────────────────────────────

    private void startPowerMonitoring() {
        powerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    checkSimpleCondition("CHARGING");
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    checkSimpleCondition("DISCHARGING");
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
                    if (level >= 0 && scale > 0) {
                        int pct = (int)(level * 100f / scale);
                        checkBatteryLevel(pct);
                    }
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(powerReceiver, f);
    }

    private void checkBatteryLevel(int currentPct) {
        for (LeaRoutineDatabase.RoutineRow r : db.getAllRoutines()) {
            try {
                JSONArray conds = new JSONArray(r.conditionsJson != null ? r.conditionsJson : "[]");
                for (int i = 0; i < conds.length(); i++) {
                    JSONObject c = conds.getJSONObject(i);
                    if (!"BATTERY_LEVEL".equals(c.optString("type"))) continue;
                    int threshold = c.optInt("percent", 20);
                    if (currentPct <= threshold && !r.active && r.userEnabled) activateRoutine(r);
                }
            } catch (Exception e) { Log.e(TAG, "checkBattery: " + e.getMessage()); }
        }
    }

    // ── ÉCRAN ─────────────────────────────────────────────────────────────────

    private void startScreenMonitoring() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action))  checkSimpleCondition("SCREEN_ON");
                if (Intent.ACTION_SCREEN_OFF.equals(action)) checkSimpleCondition("SCREEN_OFF");
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, f);
    }

    // ── CASQUE / ÉCOUTEURS ────────────────────────────────────────────────────

    private void startHeadsetMonitoring() {
        headsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) return;
                int state = intent.getIntExtra("state", 0);
                if (state == 1) checkSimpleCondition("HEADPHONES_CONNECTED");
            }
        };
        registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    // ── APPELS ────────────────────────────────────────────────────────────────

    private void startCallMonitoring() {
        callReceiver = new BroadcastReceiver() {
            private boolean wasRinging = false;
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                    wasRinging = true;
                    checkSimpleCondition("INCOMING_CALL");
                } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state) && wasRinging) {
                    wasRinging = false;
                    checkSimpleCondition("CALL_ENDED");
                }
            }
        };
        registerReceiver(callReceiver,
            new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
    }

    // ── Vérification générique (condition sans paramètre) ─────────────────────

    private void checkSimpleCondition(String condType) {
        for (LeaRoutineDatabase.RoutineRow r : db.getAllRoutines()) {
            try {
                JSONArray conds = new JSONArray(r.conditionsJson != null ? r.conditionsJson : "[]");
                for (int i = 0; i < conds.length(); i++) {
                    JSONObject c = conds.getJSONObject(i);
                    if (condType.equals(c.optString("type")) && !r.active && r.userEnabled) {
                        activateRoutine(r);
                    }
                }
            } catch (Exception e) { Log.e(TAG, "checkSimple(" + condType + "): " + e.getMessage()); }
        }
    }

    // ── Activation / désactivation ────────────────────────────────────────────

    private void activateRoutine(LeaRoutineDatabase.RoutineRow r) {
        db.setRoutineActive(r.id, true);
        executor.execute(r.actionsJson);
        notifMgr.showActiveRoutine(r.name, r.icon);
        Log.d(TAG, "Activée: " + r.name);
    }

    private void deactivateRoutine(LeaRoutineDatabase.RoutineRow r) {
        db.setRoutineActive(r.id, false);
        Log.d(TAG, "Désactivée: " + r.name);
        refreshNotification();
    }

    private void refreshNotification() {
        List<LeaRoutineDatabase.RoutineRow> list = db.getAllRoutines();
        int count = 0;
        LeaRoutineDatabase.RoutineRow last = null;
        for (LeaRoutineDatabase.RoutineRow r : list) { if (r.active) { count++; last = r; } }
        if (count == 0)                      notifMgr.cancel();
        else if (count == 1 && last != null) notifMgr.showActiveRoutine(last.name, last.icon);
        else                                 notifMgr.showMultipleActive(count);
    }

    // ── Cycle de vie du service ───────────────────────────────────────────────

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) {}
        }
        safeUnregister(btReceiver);
        safeUnregister(timeReceiver);
        safeUnregister(powerReceiver);
        safeUnregister(screenReceiver);
        safeUnregister(headsetReceiver);
        safeUnregister(callReceiver);
    }

    private void safeUnregister(BroadcastReceiver r) {
        if (r != null) try { unregisterReceiver(r); } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
