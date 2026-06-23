package com.flolov42.lea_v3.agents;

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
import android.net.wifi.WifiManager;
import java.util.List;

public class LeaSmartHomeAgent {

    private static final String ID    = LeaAgentActivationManager.SMART_HOME;
    private static final String PREFS = "lea_smarthome";

    // Known home WiFi SSID
    private static final String HOME_WIFI_PREFIX = "home_wifi";

    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences         prefs;

    private String configuredHomeWifi = "";

    public LeaSmartHomeAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        configuredHomeWifi = prefs.getString("home_wifi_ssid", "");
    }

    public void execute() {
        try {
            detectPresenceChange();
            learnPatterns();
            suggestEnergyOptimization();
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Smart Home: " + e.getMessage());
        }
    }

    private void detectPresenceChange() {
        boolean atHome    = isAtHome();
        boolean wasAtHome = prefs.getBoolean("was_at_home", false);

        if (atHome != wasAtHome) {
            prefs.edit().putBoolean("was_at_home", atHome).apply();

            if (atHome) {
                onArrival();
            } else {
                onDeparture();
            }
        }
    }

    private void onArrival() {
        String msg = "🏠 Bienvenue! Activation des préférences maison (luminosité auto, musique...)";
        db.addLog(ID, msg);
        notif.notify(ID, "🏠 Arrivée détectée", "Léa active tes préférences maison");
        // In a real SmartThings integration, we'd call the Samsung SmartThings API here
        sendSmartThingsCommand("scene", "ARRIVE_HOME");
    }

    private void onDeparture() {
        String msg = "🚗 Départ détecté — Economie d'énergie activée";
        db.addLog(ID, msg);
        notif.notify(ID, "🚗 Départ détecté", "Veux-tu couper le chauffage? (Auto dans 5min)");
        sendSmartThingsCommand("scene", "LEAVE_HOME");
    }

    private void learnPatterns() {
        java.util.Calendar cal  = java.util.Calendar.getInstance();
        int hour   = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int dow    = cal.get(java.util.Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY);

        // Log patterns for learning
        String pattern = "hour_" + hour + "_weekday_" + (isWeekend ? "end" : "day");
        int count = prefs.getInt(pattern + "_count", 0) + 1;
        prefs.edit().putInt(pattern + "_count", count).apply();

        // After 5 observations, suggest automation
        if (count == 5) {
            String suggestion = buildPatternSuggestion(hour, isWeekend);
            if (suggestion != null) {
                db.addLog(ID, "💡 Automatisation suggérée: " + suggestion);
            }
        }
    }

    private String buildPatternSuggestion(int hour, boolean isWeekend) {
        if (hour >= 22 || hour <= 6) return "Mode nuit automatique à " + hour + "h";
        if (hour == 7 && !isWeekend)  return "Routine matin semaine à 7h";
        if (hour >= 18 && hour <= 20) return "Mode soirée automatique à " + hour + "h";
        return null;
    }

    private void suggestEnergyOptimization() {
        java.util.Calendar cal  = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

        if (hour >= 2 && hour <= 6) {
            boolean atHome = isAtHome();
            if (!atHome) {
                String msg = "💡 Tu es absent et des appareils sont peut-être allumés. Éco-mode?";
                db.addLog(ID, msg);
            }
        }
    }

    private boolean isAtHome() {
        if (configuredHomeWifi.isEmpty()) return true;
        try {
            String ssid = getCurrentSsid();
            return ssid != null && ssid.equals(configuredHomeWifi);
        } catch (Exception e) {
            return true;
        }
    }

    public String getCurrentSsid() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Android 12+ — WifiInfo via NetworkCapabilities.getTransportInfo()
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm == null) return null;
                android.net.Network net = cm.getActiveNetwork();
                if (net == null) return null;
                android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                if (caps == null || !caps.hasTransport(
                        android.net.NetworkCapabilities.TRANSPORT_WIFI)) return null;
                android.net.TransportInfo ti = caps.getTransportInfo();
                if (!(ti instanceof android.net.wifi.WifiInfo)) return null;
                String raw = ((android.net.wifi.WifiInfo) ti).getSSID();
                return raw != null ? raw.replace("\"", "") : null;
            } else {
                WifiManager wm = (WifiManager) ctx.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
                if (wm == null || !wm.isWifiEnabled()) return null;
                android.net.wifi.WifiInfo info = wm.getConnectionInfo();
                if (info == null) return null;
                return info.getSSID().replace("\"", "");
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void sendSmartThingsCommand(String type, String value) {
        String token = prefs.getString("smartthings_token", "");
        if (token.isEmpty()) {
            db.addLog(ID, "📡 SmartThings: token non configuré — commande ignorée (" + type + "=" + value + ")");
            return;
        }
        // Appel API SmartThings en arrière-plan
        new Thread(() -> {
            try {
                String sceneKey = "scene_" + value.toLowerCase();
                String sceneId  = prefs.getString(sceneKey, "");
                if (sceneId.isEmpty()) {
                    db.addLog(ID, "📡 SmartThings: scène '" + value + "' non configurée");
                    return;
                }
                java.net.URL url = new java.net.URL(
                    "https://api.smartthings.com/v1/scenes/" + sceneId + "/execute");
                java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                int status = conn.getResponseCode();
                conn.disconnect();
                if (status == 200) {
                    db.addLog(ID, "✅ SmartThings: scène '" + value + "' exécutée");
                } else {
                    db.addLog(ID, "⚠️ SmartThings: HTTP " + status + " pour scène '" + value + "'");
                }
            } catch (Exception e) {
                db.addLog(ID, "⚠️ SmartThings: " + e.getMessage());
            }
        }).start();
    }

    public void setSmartThingsToken(String token) {
        prefs.edit().putString("smartthings_token", token).apply();
        db.addLog(ID, token.isEmpty() ? "🔑 Token SmartThings supprimé" : "🔑 Token SmartThings configuré");
    }

    public void setSmartThingsSceneId(String sceneName, String sceneId) {
        prefs.edit().putString("scene_" + sceneName.toLowerCase(), sceneId).apply();
        db.addLog(ID, "📋 Scène SmartThings '" + sceneName + "' → " + sceneId);
    }

    public boolean hasSmartThingsToken() {
        return !prefs.getString("smartthings_token", "").isEmpty();
    }

    public void setHomeWifi(String ssid) {
        configuredHomeWifi = ssid;
        prefs.edit().putString("home_wifi_ssid", ssid).apply();
        db.addLog(ID, "📶 WiFi maison configuré: " + ssid);
    }

    public String getStatus() {
        return isAtHome() ? "🏠 À la maison" : "🚗 Absent";
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
