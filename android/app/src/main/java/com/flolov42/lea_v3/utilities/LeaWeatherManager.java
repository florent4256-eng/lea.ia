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
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Récupère la météo via OpenWeatherMap (clé configurable) + lance l'app météo Samsung.
 * Configure ta clé API : prefs "weather_api_key" dans SharedPreferences "lea_memory".
 */
public class LeaWeatherManager {

    private static final String TAG      = "LeaWeather";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String PREFS    = "lea_memory";

    private final Context ctx;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WeatherCallback { void onResult(String message); }

    public LeaWeatherManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Méthode principale ────────────────────────────────────────────────────

    public void fetchWeather(WeatherCallback cb) {
        double[] loc = getLastLocation();
        if (loc == null) {
            // Fallback : météo Paris par défaut
            fetchWeatherByCity("Paris", cb);
            return;
        }
        fetchWeatherByCoords(loc[0], loc[1], cb);
    }

    public void fetchWeatherByCity(String city, WeatherCallback cb) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            // Mode démo — pas de clé
            deliver(cb, "Je n'ai pas de clé météo configurée. Lance l'app Samsung pour voir la météo.");
            openSamsungWeatherApp();
            return;
        }
        String endpoint = BASE_URL + "?q=" + city
            + "&appid=" + apiKey + "&units=metric&lang=fr";
        doFetch(endpoint, city, cb);
    }

    private void fetchWeatherByCoords(double lat, double lon, WeatherCallback cb) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            deliver(cb, "Je n'ai pas de clé météo configurée. Lance l'app Samsung pour voir la météo.");
            openSamsungWeatherApp();
            return;
        }
        String endpoint = BASE_URL + "?lat=" + lat + "&lon=" + lon
            + "&appid=" + apiKey + "&units=metric&lang=fr";
        doFetch(endpoint, null, cb);
    }

    private void doFetch(final String endpoint, final String fallbackCity, final WeatherCallback cb) {
        new Thread(() -> {
            try {
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    deliver(cb, "Météo indisponible (code " + code + "). Ouverture de l'app Samsung.");
                    openSamsungWeatherApp();
                    return;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                String city  = json.optString("name", fallbackCity != null ? fallbackCity : "ta ville");
                double temp  = json.getJSONObject("main").optDouble("temp", 0);
                double feels = json.getJSONObject("main").optDouble("feels_like", temp);
                int    humid = json.getJSONObject("main").optInt("humidity", 0);
                String desc  = json.getJSONArray("weather").getJSONObject(0).optString("description", "");
                String icon  = weatherEmoji(json.getJSONArray("weather").getJSONObject(0).optString("icon",""));
                double wind  = json.getJSONObject("wind").optDouble("speed", 0);

                String msg = icon + " Météo à " + city + " : "
                    + (int)temp + "°C, ressenti " + (int)feels + "°C. "
                    + capitalize(desc) + ". "
                    + "Humidité " + humid + "%, vent " + (int)(wind * 3.6) + " km/h.";

                deliver(cb, msg);
                openSamsungWeatherApp();

            } catch (Exception e) {
                Log.e(TAG, "doFetch: " + e.getMessage());
                deliver(cb, "Impossible de récupérer la météo. Vérifiez la connexion.");
                openSamsungWeatherApp();
            }
        }).start();
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    private double[] getLastLocation() {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location loc = null;
            for (String provider : new String[]{
                    LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                try {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l != null && (loc == null || l.getTime() > loc.getTime())) loc = l;
                } catch (SecurityException ignored) {}
            }
            if (loc == null) return null;
            return new double[]{ loc.getLatitude(), loc.getLongitude() };
        } catch (Exception e) {
            Log.e(TAG, "getLastLocation: " + e.getMessage());
            return null;
        }
    }

    // ── App Samsung météo ─────────────────────────────────────────────────────

    public void openSamsungWeatherApp() {
        String[] weatherApps = {
            "com.sec.android.daemonapp.weather",
            "com.samsung.android.weather",
            "com.sec.android.app.weather"
        };
        for (String pkg : weatherApps) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Fallback navigateur
        try {
            Intent i = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://meteo.fr"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getApiKey() {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getString("weather_api_key", "");
    }

    private void deliver(WeatherCallback cb, String msg) {
        mainHandler.post(() -> { if (cb != null) cb.onResult(msg); });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String weatherEmoji(String icon) {
        if (icon.startsWith("01")) return "☀️";
        if (icon.startsWith("02")) return "🌤️";
        if (icon.startsWith("03") || icon.startsWith("04")) return "☁️";
        if (icon.startsWith("09") || icon.startsWith("10")) return "🌧️";
        if (icon.startsWith("11")) return "⛈️";
        if (icon.startsWith("13")) return "❄️";
        if (icon.startsWith("50")) return "🌫️";
        return "🌡️";
    }
}
