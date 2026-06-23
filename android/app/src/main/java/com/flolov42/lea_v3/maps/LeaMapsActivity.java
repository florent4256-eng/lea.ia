package com.flolov42.lea_v3.maps;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.flolov42.lea_v3.R;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LeaMapsActivity extends AppCompatActivity {

    // ── réseau ────────────────────────────────────────────────────
    private OkHttpClient http;

    // ── carte ─────────────────────────────────────────────────────
    private MapView  mapView;
    private Marker   locationMarker;
    private Marker   destMarker;
    private Polyline routeLine;

    // ── styles de carte ───────────────────────────────────────────
    private static final ITileSource[] TILE_SOURCES = new ITileSource[]{
        TileSourceFactory.MAPNIK,
        new XYTileSource("Voyager", 0, 20, 256, ".png", new String[]{
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        }),
        new XYTileSource("DarkMatter", 0, 20, 256, ".png", new String[]{
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/"
        }),
        new XYTileSource("OpenTopo", 0, 17, 256, ".png", new String[]{
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        }),
        TileSourceFactory.PUBLIC_TRANSPORT,
        new XYTileSource("HOT", 0, 19, 256, ".png", new String[]{
            "https://a.tile.openstreetmap.fr/hot/",
            "https://b.tile.openstreetmap.fr/hot/"
        }),
    };
    private static final String[] TILE_NAMES = {
        "Standard", "Voyager", "Nuit", "Topographique", "Transport", "Humanitaire"
    };
    private int mapStyleIndex = 0;

    // ── GPS ───────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedClient;
    private LocationCallback            locationCallback;
    private double  currentLat = 0, currentLon = 0;
    private boolean hasLocation = false;

    // ── UI : commun ───────────────────────────────────────────────
    private TextView     gpsCoords;
    private LinearLayout routePanel, weatherPanel;
    private LinearLayout mapsTopBar;
    private Button       tabLocate, tabRoute, tabWeather;
    private Button       btnSOS;
    private boolean      sosActive = false;
    private String       serverHost;

    // ── UI : itinéraire (Waze style) ──────────────────────────────
    private TextView     routeOriginLabel;
    private EditText     routeDestInput;
    private Button       btnModeCar, btnModeWalk, btnModeBike;
    private LinearLayout routeSummaryBlock;
    private TextView     navSummaryMin, navSummaryDist, navSummaryETA, routeDestLabel;
    private TextView     routeHelpText;
    private Button       btnGo;
    private String       routeMode = "driving";

    // ── UI : navigation active (Waze top card + bottom bar) ───────
    private LinearLayout navInstructionBar, navBottomBar;
    private LinearLayout bottomBar;
    private FrameLayout  navArrowBox;
    private TextView     navArrowText, navDistance, navInstruction, navStreet;
    private TextView     navETA, navDurationLeft, navDistLeft;

    // ── Navigation state ──────────────────────────────────────────
    private boolean          isNavigating  = false;
    private final List<JSONObject> navSteps = new ArrayList<>();
    private int              navStepIdx    = 0;
    private double           navTotalDistKm = 0;
    private double           navTotalDurMin = 0;
    private String           navDestName   = "";

    // ── UI : paramètres ───────────────────────────────────────────
    private View     settingsOverlay;
    private Button   btnThemeDay, btnThemeNight, btnThemeAuto;
    private TextView themeDescription;
    private String   mapThemeMode = "auto";

    // ── UI : météo ────────────────────────────────────────────────
    private TextView weatherTemp, weatherFeelsLike, weatherWind;
    private TextView weatherCode, weatherHumidity, weatherPrecip, weatherUV;
    private TextView forecastDay0, forecastEmoji0, forecastTemp0;
    private TextView forecastDay1, forecastEmoji1, forecastTemp1;
    private TextView forecastDay2, forecastEmoji2, forecastTemp2;

    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue("LeaV3/1.0");
        setContentView(R.layout.activity_maps);

        serverHost = getSharedPreferences("lea_prefs", MODE_PRIVATE)
            .getString("server_host", "https://lea-bunker.lea-ia-local.com:3001");

        http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

        bindViews();
        setupMap();
        setupListeners();
        loadAndApplyTheme();
        startGPS();
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            android.widget.Toast.makeText(this, "❌ Maps: " + e.getMessage() + "\n@ " + loc, android.widget.Toast.LENGTH_LONG).show();
            com.flolov42.lea_v3.utilities.LeaAndroidLogger.crash(this, "Maps onCreate", e);
            finish();
        }
    }

    @Override protected void onResume()  { super.onResume();  if (mapView != null) mapView.onResume(); }
    @Override protected void onPause()   { super.onPause();   if (mapView != null) { mapView.onPause(); stopGPS(); } }
    @Override protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDetach(); stopGPS(); }

    // ── bind ──────────────────────────────────────────────────────
    private void bindViews() {
        mapView          = findViewById(R.id.mapView);
        gpsCoords        = findViewById(R.id.gpsCoords);
        mapsTopBar       = findViewById(R.id.mapsTopBar);
        routePanel       = findViewById(R.id.routePanel);
        weatherPanel     = findViewById(R.id.weatherPanel);
        bottomBar        = findViewById(R.id.bottomBar);
        routeOriginLabel = findViewById(R.id.routeOriginLabel);
        routeDestInput   = findViewById(R.id.routeDestInput);
        routeHelpText    = findViewById(R.id.routeHelpText);
        tabLocate        = findViewById(R.id.tabLocate);
        tabRoute         = findViewById(R.id.tabRoute);
        tabWeather       = findViewById(R.id.tabWeather);
        btnSOS           = findViewById(R.id.btnSOS);

        btnModeCar  = findViewById(R.id.btnModeCar);
        btnModeWalk = findViewById(R.id.btnModeWalk);
        btnModeBike = findViewById(R.id.btnModeBike);

        // Résumé itinéraire Waze
        routeSummaryBlock = findViewById(R.id.routeSummaryBlock);
        navSummaryMin     = findViewById(R.id.navSummaryMin);
        navSummaryDist    = findViewById(R.id.navSummaryDist);
        navSummaryETA     = findViewById(R.id.navSummaryETA);
        routeDestLabel    = findViewById(R.id.routeDestLabel);
        btnGo             = findViewById(R.id.btnGo);

        // Barres de navigation
        navInstructionBar = findViewById(R.id.navInstructionBar);
        navArrowBox       = findViewById(R.id.navArrowBox);
        navArrowText      = findViewById(R.id.navArrowText);
        navDistance       = findViewById(R.id.navDistance);
        navInstruction    = findViewById(R.id.navInstruction);
        navStreet         = findViewById(R.id.navStreet);
        navBottomBar      = findViewById(R.id.navBottomBar);
        navETA            = findViewById(R.id.navETA);
        navDurationLeft   = findViewById(R.id.navDurationLeft);
        navDistLeft       = findViewById(R.id.navDistLeft);

        // Météo
        weatherTemp      = findViewById(R.id.weatherTemp);
        weatherFeelsLike = findViewById(R.id.weatherFeelsLike);
        weatherWind      = findViewById(R.id.weatherWind);
        weatherCode      = findViewById(R.id.weatherCode);
        weatherHumidity  = findViewById(R.id.weatherHumidity);
        weatherPrecip    = findViewById(R.id.weatherPrecip);
        weatherUV        = findViewById(R.id.weatherUV);

        forecastDay0   = findViewById(R.id.forecastDay0);
        forecastEmoji0 = findViewById(R.id.forecastEmoji0);
        forecastTemp0  = findViewById(R.id.forecastTemp0);
        forecastDay1   = findViewById(R.id.forecastDay1);
        forecastEmoji1 = findViewById(R.id.forecastEmoji1);
        forecastTemp1  = findViewById(R.id.forecastTemp1);
        forecastDay2   = findViewById(R.id.forecastDay2);
        forecastEmoji2 = findViewById(R.id.forecastEmoji2);
        forecastTemp2  = findViewById(R.id.forecastTemp2);

        settingsOverlay  = findViewById(R.id.settingsOverlay);
        btnThemeDay      = findViewById(R.id.btnThemeDay);
        btnThemeNight    = findViewById(R.id.btnThemeNight);
        btnThemeAuto     = findViewById(R.id.btnThemeAuto);
        themeDescription = findViewById(R.id.themeDescription);

        ImageButton back = findViewById(R.id.mapsBack);
        back.setOnClickListener(v -> {
            if (isNavigating) stopNavigation();
            else finish();
        });
    }

    // ── carte ─────────────────────────────────────────────────────
    private void setupMap() {
        mapView.setTileSource(TILE_SOURCES[0]);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(new GeoPoint(46.6, 2.0));
    }

    // ── GPS ───────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private void startGPS() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || result.getLastLocation() == null) return;
                currentLat = result.getLastLocation().getLatitude();
                currentLon = result.getLastLocation().getLongitude();
                hasLocation = true;
                updateLocationOnMap();
            }
        };
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());

        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                currentLat = loc.getLatitude();
                currentLon = loc.getLongitude();
                hasLocation = true;
                updateLocationOnMap();
            }
        });
    }

    private void stopGPS() {
        if (fusedClient != null && locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
    }

    private void updateLocationOnMap() {
        GeoPoint pos = new GeoPoint(currentLat, currentLon);
        if (locationMarker == null) {
            locationMarker = new Marker(mapView);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            locationMarker.setTitle("Ma position");
            mapView.getOverlays().add(locationMarker);
        }
        locationMarker.setPosition(pos);
        mapView.invalidate();

        String coordText = String.format("%.4f, %.4f", currentLat, currentLon);
        gpsCoords.setText(coordText);
        routeOriginLabel.setText("Départ : " + coordText);

        // Avancement automatique des étapes de navigation
        if (isNavigating && navStepIdx < navSteps.size()) {
            try {
                JSONObject step = navSteps.get(navStepIdx);
                JSONArray  loc  = step.getJSONObject("maneuver").getJSONArray("location");
                double dist = haversine(currentLat, currentLon, loc.getDouble(1), loc.getDouble(0));
                if (dist < 40) {
                    navStepIdx++;
                    updateNavStep();
                }
            } catch (Exception ignored) {}
        }
    }

    // ── listeners ─────────────────────────────────────────────────
    private void setupListeners() {
        tabLocate.setOnClickListener(v -> {
            showPanel(0);
            if (hasLocation) {
                mapView.getController().animateTo(new GeoPoint(currentLat, currentLon));
                mapView.getController().setZoom(15.0);
            } else Toast.makeText(this, "GPS en attente...", Toast.LENGTH_SHORT).show();
        });

        tabRoute.setOnClickListener(v -> showPanel(1));

        tabWeather.setOnClickListener(v -> {
            showPanel(2);
            fetchWeatherWithFallback();
        });

        findViewById(R.id.btnCenter).setOnClickListener(v -> {
            if (hasLocation) {
                mapView.getController().animateTo(new GeoPoint(currentLat, currentLon));
                mapView.getController().setZoom(15.0);
            } else Toast.makeText(this, "GPS en cours...", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnZoomIn).setOnClickListener(v -> mapView.getController().zoomIn());
        findViewById(R.id.btnZoomOut).setOnClickListener(v -> mapView.getController().zoomOut());

        findViewById(R.id.btnStyleCycle).setOnClickListener(v -> {
            mapStyleIndex = (mapStyleIndex + 1) % TILE_SOURCES.length;
            mapView.setTileSource(TILE_SOURCES[mapStyleIndex]);
            mapView.getTileProvider().clearTileCache();
            mapView.invalidate();
            Toast.makeText(this, "Carte : " + TILE_NAMES[mapStyleIndex], Toast.LENGTH_SHORT).show();
        });

        btnSOS.setOnClickListener(v -> triggerSOS());

        findViewById(R.id.btnSettings).setOnClickListener(v -> openSettings());
        settingsOverlay.setOnClickListener(v -> closeSettings());
        findViewById(R.id.settingsCard).setOnClickListener(v -> {});
        findViewById(R.id.btnCloseSettings).setOnClickListener(v -> closeSettings());

        btnThemeDay.setOnClickListener(v -> selectTheme("day"));
        btnThemeNight.setOnClickListener(v -> selectTheme("night"));
        btnThemeAuto.setOnClickListener(v -> selectTheme("auto"));

        // Modes de transport
        btnModeCar.setOnClickListener(v -> setRouteMode("driving"));
        btnModeWalk.setOnClickListener(v -> setRouteMode("walking"));
        btnModeBike.setOnClickListener(v -> setRouteMode("cycling"));

        // Calcul itinéraire
        findViewById(R.id.btnCalculateRoute).setOnClickListener(v -> {
            String dest = routeDestInput.getText().toString().trim();
            if (dest.isEmpty()) { Toast.makeText(this, "Entrez une destination", Toast.LENGTH_SHORT).show(); return; }
            if (!hasLocation)   { Toast.makeText(this, "GPS non disponible",     Toast.LENGTH_SHORT).show(); return; }
            geocodeAndRoute(dest);
        });
        routeDestInput.setOnEditorActionListener((v, actionId, event) -> {
            String dest = routeDestInput.getText().toString().trim();
            if (!dest.isEmpty() && hasLocation) geocodeAndRoute(dest);
            return true;
        });

        // Bouton GO → démarre la navigation
        btnGo.setOnClickListener(v -> startNavigation());

        // Bouton effacer l'itinéraire
        findViewById(R.id.btnClearRoute).setOnClickListener(v -> clearRoute());

        // Bouton stop navigation (✕ dans la barre de navigation)
        findViewById(R.id.btnStopNav).setOnClickListener(v -> stopNavigation());

        // Actualiser météo
        findViewById(R.id.btnRefreshWeather).setOnClickListener(v -> fetchWeatherWithFallback());
    }

    // ── mode transport ────────────────────────────────────────────
    private void setRouteMode(String mode) {
        routeMode = mode;
        int active = 0xFF1259C3, inactive = 0xFF1E293B;
        btnModeCar.setBackgroundColor(mode.equals("driving")  ? active : inactive);
        btnModeWalk.setBackgroundColor(mode.equals("walking") ? active : inactive);
        btnModeBike.setBackgroundColor(mode.equals("cycling") ? active : inactive);
        btnModeCar.setTextColor(mode.equals("driving")  ? 0xFFFFFFFF : 0xFF94A3B8);
        btnModeWalk.setTextColor(mode.equals("walking") ? 0xFFFFFFFF : 0xFF94A3B8);
        btnModeBike.setTextColor(mode.equals("cycling") ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    // ── tabs ──────────────────────────────────────────────────────
    private void showPanel(int tab) {
        if (isNavigating) return;
        routePanel.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        weatherPanel.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        int active = 0xFF1259C3, inactive = 0xFF1A1A1A;
        int atxt = 0xFF60A5FA, itxt = 0xFF94A3B8;
        tabLocate.setBackgroundColor(tab == 0 ? active : inactive);
        tabRoute.setBackgroundColor(tab == 1 ? active : inactive);
        tabWeather.setBackgroundColor(tab == 2 ? active : inactive);
        tabLocate.setTextColor(tab == 0 ? atxt : itxt);
        tabRoute.setTextColor(tab == 1 ? atxt : itxt);
        tabWeather.setTextColor(tab == 2 ? atxt : itxt);
    }

    // ── Waze Navigation ───────────────────────────────────────────
    private void startNavigation() {
        if (navSteps.isEmpty()) {
            Toast.makeText(this, "Calcul d'itinéraire requis", Toast.LENGTH_SHORT).show();
            return;
        }
        isNavigating  = true;
        navStepIdx    = 0;

        // Masquer top bar et bottom bar, afficher barres nav
        mapsTopBar.setVisibility(View.GONE);
        routePanel.setVisibility(View.GONE);
        weatherPanel.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);

        navInstructionBar.setVisibility(View.VISIBLE);
        navBottomBar.setVisibility(View.VISIBLE);

        // Remplir la barre bas
        String durStr = navTotalDurMin >= 60
            ? String.format("%dh%02d", (int)(navTotalDurMin / 60), (int)(navTotalDurMin % 60))
            : String.format("%.0f min", navTotalDurMin);
        navDurationLeft.setText(durStr);
        navDistLeft.setText(String.format("%.1f km", navTotalDistKm));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, (int) navTotalDurMin);
        navETA.setText(String.format("%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

        // Afficher la première instruction
        updateNavStep();

        // Centrer sur la position
        if (hasLocation) {
            mapView.getController().animateTo(new GeoPoint(currentLat, currentLon));
            mapView.getController().setZoom(16.0);
        }
    }

    private void stopNavigation() {
        isNavigating = false;

        navInstructionBar.setVisibility(View.GONE);
        navBottomBar.setVisibility(View.GONE);

        mapsTopBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);

        clearRoute();
        showPanel(0);
    }

    private void updateNavStep() {
        if (navStepIdx >= navSteps.size()) {
            // Arrivée à destination
            navArrowText.setText("🏁");
            navInstruction.setText("Vous êtes arrivé !");
            navDistance.setText("");
            navStreet.setText(navDestName);
            navArrowBox.setBackgroundColor(0xFF27AE60);
            return;
        }
        try {
            JSONObject step = navSteps.get(navStepIdx);
            JSONObject man  = step.getJSONObject("maneuver");
            String type = man.optString("type", "");
            String mod  = man.optString("modifier", "");
            String name = step.optString("name", "");
            double dist = step.getDouble("distance");

            navArrowText.setText(maneuverArrow(type, mod));
            navInstruction.setText(maneuverInstruction(type, mod));
            navDistance.setText(formatDist(dist));
            navStreet.setText(name);

            // Couleur de la boîte : orange = virage, cyan = tout droit
            boolean isTurn = "turn".equals(type) || "end of road".equals(type)
                          || "left".equals(mod)  || "right".equals(mod)
                          || "sharp left".equals(mod) || "sharp right".equals(mod);
            navArrowBox.setBackgroundColor(isTurn ? 0xFFFF8C00 : 0xFF00C2CB);
        } catch (Exception ignored) {}
    }

    // ── Paramètres / thème ────────────────────────────────────────
    private void openSettings() {
        settingsOverlay.setVisibility(View.VISIBLE);
        updateThemeButtons();
    }

    private void closeSettings() {
        settingsOverlay.setVisibility(View.GONE);
    }

    private void selectTheme(String mode) {
        mapThemeMode = mode;
        getSharedPreferences("lea_prefs", MODE_PRIVATE)
            .edit().putString("map_theme", mode).apply();
        applyTheme(mode);
        updateThemeButtons();
    }

    private void loadAndApplyTheme() {
        mapThemeMode = getSharedPreferences("lea_prefs", MODE_PRIVATE)
            .getString("map_theme", "auto");
        applyTheme(mapThemeMode);
        updateThemeButtons();
    }

    private void applyTheme(String mode) {
        switch (mode) {
            case "day":
                mapView.setTileSource(TILE_SOURCES[1]);
                mapStyleIndex = 1;
                break;
            case "night":
                mapView.setTileSource(TILE_SOURCES[2]);
                mapStyleIndex = 2;
                break;
            case "auto":
            default:
                int uiMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean isNight = (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
                mapView.setTileSource(TILE_SOURCES[isNight ? 2 : 1]);
                mapStyleIndex = isNight ? 2 : 1;
                break;
        }
        mapView.getTileProvider().clearTileCache();
        mapView.invalidate();
    }

    private void updateThemeButtons() {
        int activeBg   = 0xFF1259C3, inactiveBg   = 0xFF0D1B2E;
        int activeText = 0xFFFFFFFF, inactiveText = 0xFF94A3B8;

        btnThemeDay.setBackgroundColor("day".equals(mapThemeMode)   ? activeBg : inactiveBg);
        btnThemeNight.setBackgroundColor("night".equals(mapThemeMode) ? activeBg : inactiveBg);
        btnThemeAuto.setBackgroundColor("auto".equals(mapThemeMode)  ? activeBg : inactiveBg);

        btnThemeDay.setTextColor("day".equals(mapThemeMode)   ? activeText : inactiveText);
        btnThemeNight.setTextColor("night".equals(mapThemeMode) ? activeText : inactiveText);
        btnThemeAuto.setTextColor("auto".equals(mapThemeMode)  ? activeText : inactiveText);

        String desc;
        switch (mapThemeMode) {
            case "day":   desc = "Carte claire — Voyager (CartoDB)"; break;
            case "night": desc = "Carte sombre — Dark Matter (CartoDB)"; break;
            default:
                int uiMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean isNight = (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
                desc = "Automatique — " + (isNight ? "sombre (thème nuit actif)" : "claire (thème jour actif)");
                break;
        }
        themeDescription.setText(desc);
    }

    // ── SOS ───────────────────────────────────────────────────────
    private void triggerSOS() {
        sosActive = !sosActive;
        if (sosActive) {
            btnSOS.setBackgroundColor(0xFFDC2626);
            btnSOS.setText("SOS ●");
            Toast.makeText(this, "SOS activé — envoi position à Léa", Toast.LENGTH_SHORT).show();
            String coords = hasLocation ? String.format("%.6f,%.6f", currentLat, currentLon) : "inconnue";
            http.newCall(new Request.Builder().url(serverHost + "/api/sos?coords=" + coords).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call c, IOException e) {}
                    @Override public void onResponse(Call c, Response r) { r.close(); }
                });
        } else {
            btnSOS.setBackgroundColor(0xCC7F1D1D);
            btnSOS.setText("SOS");
        }
    }

    // ── Météo ─────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private void fetchWeatherWithFallback() {
        if (hasLocation) {
            fetchWeather(currentLat, currentLon);
        } else {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    fetchWeather(loc.getLatitude(), loc.getLongitude());
                } else {
                    runOnUiThread(() -> Toast.makeText(this,
                        "GPS indisponible — active la localisation", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void fetchWeather(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=" + lat
            + "&longitude=" + lon
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,"
            +           "wind_speed_10m,weather_code,precipitation_probability,uv_index"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
            + "&wind_speed_unit=kmh&timezone=auto&forecast_days=3";

        http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                    "Météo indisponible : " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject root    = new JSONObject(response.body().string());
                    JSONObject current = root.getJSONObject("current");

                    double temp     = current.getDouble("temperature_2m");
                    double feels    = current.getDouble("apparent_temperature");
                    double wind     = current.getDouble("wind_speed_10m");
                    int    humidity = current.getInt("relative_humidity_2m");
                    int    code     = current.getInt("weather_code");
                    int    precip   = current.optInt("precipitation_probability", 0);
                    double uv       = current.optDouble("uv_index", 0);

                    JSONObject daily    = root.getJSONObject("daily");
                    JSONArray  dayCodes = daily.getJSONArray("weather_code");
                    JSONArray  dayMax   = daily.getJSONArray("temperature_2m_max");
                    JSONArray  dayMin   = daily.getJSONArray("temperature_2m_min");
                    JSONArray  dayTimes = daily.getJSONArray("time");

                    runOnUiThread(() -> {
                        weatherTemp.setText(String.format("%.0f°C", temp));
                        weatherFeelsLike.setText(String.format("Ressenti : %.0f°C", feels));
                        weatherWind.setText(String.format("%.0f km/h", wind));
                        weatherHumidity.setText(humidity + " %");
                        weatherCode.setText(wmoDesc(code));
                        weatherPrecip.setText("Pluie : " + precip + " %");
                        weatherUV.setText(String.format("%.0f", uv));

                        if (dayTimes.length() > 0) fillForecastDay(0, dayTimes, dayCodes, dayMax, dayMin);
                        if (dayTimes.length() > 1) fillForecastDay(1, dayTimes, dayCodes, dayMax, dayMin);
                        if (dayTimes.length() > 2) fillForecastDay(2, dayTimes, dayCodes, dayMax, dayMin);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                        "Erreur météo : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fillForecastDay(int idx, JSONArray times, JSONArray codes, JSONArray maxT, JSONArray minT) {
        try {
            String dateStr  = times.getString(idx);
            String dayLabel = idx == 0 ? "Auj." : idx == 1 ? "Dem." : dayOfWeek(dateStr);
            int    code     = codes.getInt(idx);
            double tMax     = maxT.getDouble(idx);
            double tMin     = minT.getDouble(idx);

            TextView dayV   = idx == 0 ? forecastDay0   : idx == 1 ? forecastDay1   : forecastDay2;
            TextView emojiV = idx == 0 ? forecastEmoji0 : idx == 1 ? forecastEmoji1 : forecastEmoji2;
            TextView tempV  = idx == 0 ? forecastTemp0  : idx == 1 ? forecastTemp1  : forecastTemp2;

            dayV.setText(dayLabel);
            emojiV.setText(wmoEmoji(code));
            tempV.setText(String.format("%.0f°/%.0f°", tMax, tMin));
        } catch (Exception ignored) {}
    }

    // ── Routing ───────────────────────────────────────────────────
    private void geocodeAndRoute(String destination) {
        clearRoute();
        routeSummaryBlock.setVisibility(View.GONE);
        routeHelpText.setVisibility(View.GONE);
        Toast.makeText(this, "Recherche de " + destination + "...", Toast.LENGTH_SHORT).show();

        String geocodeUrl = "https://nominatim.openstreetmap.org/search"
            + "?q=" + destination.replace(" ", "+")
            + "&format=json&limit=1&accept-language=fr";

        http.newCall(new Request.Builder().url(geocodeUrl)
            .header("User-Agent", "LeaV3/1.0").build())
            .enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(LeaMapsActivity.this, "Adresse introuvable", Toast.LENGTH_SHORT).show();
                        routeHelpText.setVisibility(View.VISIBLE);
                    });
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) return;
                    try {
                        JSONArray results = new JSONArray(response.body().string());
                        if (results.length() == 0) {
                            runOnUiThread(() -> {
                                Toast.makeText(LeaMapsActivity.this, "Adresse introuvable", Toast.LENGTH_SHORT).show();
                                routeHelpText.setVisibility(View.VISIBLE);
                            });
                            return;
                        }
                        JSONObject place = results.getJSONObject(0);
                        double dLat = place.getDouble("lat");
                        double dLon = place.getDouble("lon");
                        String name = place.optString("display_name", destination).split(",")[0];
                        fetchRoute(currentLat, currentLon, dLat, dLon, name);
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                            "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
    }

    private void fetchRoute(double sLat, double sLon, double eLat, double eLon, String destName) {
        String baseUrl;
        switch (routeMode) {
            case "walking": baseUrl = "https://routing.openstreetmap.de/routed-foot/route/v1/foot/"; break;
            case "cycling": baseUrl = "https://routing.openstreetmap.de/routed-bike/route/v1/bike/"; break;
            default:        baseUrl = "https://router.project-osrm.org/route/v1/driving/"; break;
        }

        String url = baseUrl + sLon + "," + sLat + ";" + eLon + "," + eLat
            + "?overview=full&geometries=geojson&steps=true";

        http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(LeaMapsActivity.this, "Calcul d'itinéraire échoué", Toast.LENGTH_SHORT).show();
                    routeHelpText.setVisibility(View.VISIBLE);
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject root   = new JSONObject(response.body().string());
                    JSONArray  routes = root.getJSONArray("routes");
                    if (routes.length() == 0) return;

                    JSONObject route   = routes.getJSONObject(0);
                    double     distKm = route.getDouble("distance") / 1000.0;
                    double     durMin = route.getDouble("duration") / 60.0;

                    JSONArray coords = route.getJSONObject("geometry").getJSONArray("coordinates");
                    List<GeoPoint> points = new ArrayList<>();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i);
                        points.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                    }

                    JSONArray steps = route.getJSONArray("legs")
                        .getJSONObject(0).getJSONArray("steps");

                    runOnUiThread(() -> {
                        drawRoute(points, distKm, durMin, eLat, eLon, destName);
                        storeSteps(steps);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                        "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void drawRoute(List<GeoPoint> points, double distKm, double durMin,
                           double dLat, double dLon, String destName) {
        if (routeLine != null) mapView.getOverlays().remove(routeLine);

        // Tracé Waze cyan
        routeLine = new Polyline(mapView);
        routeLine.setPoints(points);
        routeLine.getOutlinePaint().setColor(Color.parseColor("#33CCFF"));
        routeLine.getOutlinePaint().setStrokeWidth(14f);
        mapView.getOverlays().add(0, routeLine);

        // Marker destination
        if (destMarker != null) mapView.getOverlays().remove(destMarker);
        destMarker = new Marker(mapView);
        destMarker.setPosition(new GeoPoint(dLat, dLon));
        destMarker.setTitle(destName);
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(destMarker);

        mapView.invalidate();
        if (!points.isEmpty()) mapView.zoomToBoundingBox(routeLine.getBounds(), true, 80);

        // Sauvegarder pour la navigation
        navTotalDistKm = distKm;
        navTotalDurMin = durMin;
        navDestName    = destName;

        // Remplir le résumé Waze
        routeDestLabel.setText(destName);
        navSummaryMin.setText(durMin >= 60
            ? String.format("%dh%02d", (int)(durMin / 60), (int)(durMin % 60))
            : String.format("%.0f", durMin));
        navSummaryDist.setText(String.format("%.1f", distKm));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, (int) durMin);
        navSummaryETA.setText(String.format("%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

        routeSummaryBlock.setVisibility(View.VISIBLE);
        routeHelpText.setVisibility(View.GONE);
    }

    private void storeSteps(JSONArray steps) {
        navSteps.clear();
        for (int i = 0; i < steps.length(); i++) {
            try { navSteps.add(steps.getJSONObject(i)); } catch (Exception ignored) {}
        }
    }

    private void clearRoute() {
        if (routeLine  != null) { mapView.getOverlays().remove(routeLine);  routeLine  = null; }
        if (destMarker != null) { mapView.getOverlays().remove(destMarker); destMarker = null; }
        mapView.invalidate();
        navSteps.clear();
        navStepIdx = 0;
        routeSummaryBlock.setVisibility(View.GONE);
        routeHelpText.setVisibility(View.VISIBLE);
        routeDestInput.setText("");
    }

    // ── helpers ───────────────────────────────────────────────────
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R  = 6371000;
        double φ1 = Math.toRadians(lat1), φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);
        double a  = Math.sin(Δφ/2)*Math.sin(Δφ/2)
                  + Math.cos(φ1)*Math.cos(φ2)*Math.sin(Δλ/2)*Math.sin(Δλ/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String maneuverInstruction(String type, String mod) {
        switch (type) {
            case "depart":  return "Départ";
            case "arrive":  return "Arrivée à destination";
            case "turn":
                if (mod.isEmpty()) return "Tournez";
                switch (mod) {
                    case "left":         return "Tournez à gauche";
                    case "sharp left":   return "Tournez fortement à gauche";
                    case "slight left":  return "Légèrement à gauche";
                    case "right":        return "Tournez à droite";
                    case "sharp right":  return "Tournez fortement à droite";
                    case "slight right": return "Légèrement à droite";
                    case "straight":     return "Continuez tout droit";
                    case "uturn":        return "Faites demi-tour";
                    default:             return "Tournez";
                }
            case "continue":        return "straight".equals(mod) ? "Continuez tout droit" : "Continuez";
            case "merge":           return "Rejoignez la voie";
            case "fork":            return "left".equals(mod) ? "Gardez la gauche" : "Gardez la droite";
            case "ramp":            return "left".equals(mod) ? "Bretelle à gauche" : "Bretelle à droite";
            case "roundabout":      return "Prenez le rond-point";
            case "exit roundabout": return "Sortez du rond-point";
            case "end of road":     return "left".equals(mod) ? "Fin de route, à gauche" : "Fin de route, à droite";
            default:                return "Continuez";
        }
    }

    private String maneuverArrow(String type, String mod) {
        if ("depart".equals(type))                                           return "🚀";
        if ("arrive".equals(type))                                           return "🏁";
        if ("roundabout".equals(type) || "exit roundabout".equals(type))    return "↻";
        switch (mod) {
            case "left":         return "←";
            case "sharp left":   return "↰";
            case "slight left":  return "↖";
            case "right":        return "→";
            case "sharp right":  return "↱";
            case "slight right": return "↗";
            case "uturn":        return "↩";
            default:             return "↑";
        }
    }

    private String formatDist(double meters) {
        if (meters < 10)   return "";
        if (meters < 1000) return String.format("%d m", (int) meters);
        return String.format("%.1f km", meters / 1000.0);
    }

    // ── WMO codes ─────────────────────────────────────────────────
    private static String wmoDesc(int code) {
        if (code == 0)                return "Ciel dégagé ☀";
        if (code == 1)                return "Principalement clair";
        if (code == 2)                return "Partiellement nuageux ⛅";
        if (code == 3)                return "Couvert ☁";
        if (code == 45 || code == 48) return "Brouillard 🌫";
        if (code >= 51 && code <= 55) return "Bruine 🌦";
        if (code >= 56 && code <= 57) return "Bruine verglaçante 🌧";
        if (code >= 61 && code <= 63) return "Pluie 🌧";
        if (code == 65)               return "Forte pluie 🌧";
        if (code >= 66 && code <= 67) return "Pluie verglaçante 🌨";
        if (code >= 71 && code <= 75) return "Neige ❄";
        if (code == 77)               return "Grésil 🌨";
        if (code >= 80 && code <= 82) return "Averses 🌦";
        if (code == 85 || code == 86) return "Averses de neige ❄";
        if (code == 95)               return "Orage ⛈";
        if (code == 96 || code == 99) return "Orage avec grêle ⛈";
        return "Inconnu";
    }

    private static String wmoEmoji(int code) {
        if (code == 0 || code == 1)             return "☀";
        if (code == 2 || code == 3)             return "⛅";
        if (code == 45 || code == 48)           return "🌫";
        if (code >= 51 && code <= 67)           return "🌧";
        if (code >= 71 && code <= 77)           return "❄";
        if (code >= 80 && code <= 82)           return "🌦";
        if (code >= 85 && code <= 86)           return "❄";
        if (code >= 95)                         return "⛈";
        return "☁";
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private static String dayOfWeek(String isoDate) {
        try {
            String[] parts = isoDate.split("-");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            String[] days = {"Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"};
            return days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) { return "J+2"; }
    }
}
