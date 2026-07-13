package com.flolov42.lea_v3.maps;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillExtrusionLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LeaMapsActivity extends AppCompatActivity {

    // ── réseau ────────────────────────────────────────────────────
    private OkHttpClient http;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // ── carte (MapLibre Native — tuiles vectorielles OpenFreeMap) ──
    private MapView       mapView;
    private MapLibreMap   mapLibreMap;
    private Style         mapStyle;
    private boolean       styleReady = false;

    private static final String[] OPENFREEMAP_STYLES = {"liberty", "bright", "positron"};
    private static final String[] TILE_NAMES = {"Liberty", "Bright", "Positron"};
    private int mapStyleIndex = 0;

    // ── GPS ───────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedClient;
    private LocationCallback            locationCallback;
    private double  currentLat = 0, currentLon = 0;
    private boolean hasLocation = false;

    // ── UI : commun ───────────────────────────────────────────────
    private View          nightOverlay;
    private TextView       gpsCoords;
    private LinearLayout   routePanel, weatherPanel;
    private LinearLayout   mapsTopBar;
    private LinearLayout   leftControls;
    private Button         tabLocate, tabRoute, tabWeather;
    private Button         btnSOS;
    private boolean        sosActive = false;
    private String         serverHost;

    // ── UI : itinéraire (Waze style) ──────────────────────────────
    private TextView     routeOriginLabel;
    private EditText     routeDestInput;
    private Button       btnModeCar, btnModeWalk, btnModeBike;
    private LinearLayout routeSummaryBlock;
    private TextView     navSummaryMin, navSummaryDist, navSummaryDistUnit, navSummaryETA, routeDestLabel;
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

    // ── Itinéraire tracé (coordonnées conservées pour re-dessiner au chargement du style) ──
    private List<double[]> currentRouteCoords = null;
    private double destLat, destLon;
    private String destLabel = "";
    private boolean hasDest = false;

    // ── Repères posés par appui long (façon Google Maps) ────────────
    private final List<LatLng> longPressWaypoints = new ArrayList<>();

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

    // ── UI : vue 3D ─────────────────────────────────────────────────
    private Button  btn3D;
    private boolean is3D = false;

    // ── UI : signalement communautaire (façon Waze) ────────────────
    private LinearLayout reportMenu;
    private Button        btnReportToggle;
    private boolean        reportMenuOpen = false;
    private final Handler  reportPollHandler = new Handler(Looper.getMainLooper());
    private static final long REPORT_POLL_MS = 20000;
    private String currentUser = "";

    private static final java.util.Map<String, String> REPORT_COLORS = new java.util.HashMap<>();
    private static final java.util.Map<String, String> REPORT_LABELS = new java.util.HashMap<>();
    private static final java.util.Map<String, String> REPORT_EMOJI  = new java.util.HashMap<>();
    static {
        REPORT_COLORS.put("accident", "#ef4444");
        REPORT_COLORS.put("radar",    "#f97316");
        REPORT_COLORS.put("bouchon",  "#eab308");
        REPORT_COLORS.put("obstacle", "#a855f7");
        REPORT_COLORS.put("controle", "#3b82f6");
        REPORT_COLORS.put("travaux",  "#f59e0b");

        REPORT_LABELS.put("accident", "Accident");
        REPORT_LABELS.put("radar",    "Radar");
        REPORT_LABELS.put("bouchon",  "Bouchon");
        REPORT_LABELS.put("obstacle", "Obstacle");
        REPORT_LABELS.put("controle", "Contrôle");
        REPORT_LABELS.put("travaux",  "Travaux");

        REPORT_EMOJI.put("accident", "💥");
        REPORT_EMOJI.put("radar",    "📷");
        REPORT_EMOJI.put("bouchon",  "🚦");
        REPORT_EMOJI.put("obstacle", "⚠️");
        REPORT_EMOJI.put("controle", "👮");
        REPORT_EMOJI.put("travaux",  "🚧");
    }

    // Cache local des signalements actifs (pour l'alerte de proximité, en plus de l'affichage carte)
    private List<JSONObject> cachedReports = new ArrayList<>();
    private final java.util.Set<String> alertedReportIds = new java.util.HashSet<>();
    private static final double PROXIMITY_ALERT_METERS = 150;

    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        MapLibre.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SharedPreferences prefs = getSharedPreferences("lea_prefs", MODE_PRIVATE);
        serverHost  = prefs.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        currentUser = getIntent().getStringExtra("currentUser");
        if (currentUser == null) currentUser = "";

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

    @Override protected void onResume()  {
        super.onResume();
        if (mapView != null) mapView.onResume();
        fetchReports();
        reportPollHandler.postDelayed(reportPollRunnable, REPORT_POLL_MS);
    }
    @Override protected void onPause()   {
        super.onPause();
        if (mapView != null) mapView.onPause();
        stopGPS();
        reportPollHandler.removeCallbacks(reportPollRunnable);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
        stopGPS();
        reportPollHandler.removeCallbacks(reportPollRunnable);
    }
    @Override protected void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override protected void onStop()  { super.onStop();  if (mapView != null) mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    private final Runnable reportPollRunnable = new Runnable() {
        @Override public void run() {
            fetchReports();
            reportPollHandler.postDelayed(this, REPORT_POLL_MS);
        }
    };

    // ── bind ──────────────────────────────────────────────────────
    private void bindViews() {
        mapView          = findViewById(R.id.mapView);
        nightOverlay     = findViewById(R.id.nightOverlay);
        gpsCoords        = findViewById(R.id.gpsCoords);
        mapsTopBar       = findViewById(R.id.mapsTopBar);
        leftControls     = findViewById(R.id.leftControls);
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
        btn3D            = findViewById(R.id.btn3D);

        btnModeCar  = findViewById(R.id.btnModeCar);
        btnModeWalk = findViewById(R.id.btnModeWalk);
        btnModeBike = findViewById(R.id.btnModeBike);

        // Résumé itinéraire Waze
        routeSummaryBlock = findViewById(R.id.routeSummaryBlock);
        navSummaryMin     = findViewById(R.id.navSummaryMin);
        navSummaryDist    = findViewById(R.id.navSummaryDist);
        navSummaryDistUnit = findViewById(R.id.navSummaryDistUnit);
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

        reportMenu      = findViewById(R.id.reportMenu);
        btnReportToggle = findViewById(R.id.btnReportToggle);

        ImageButton back = findViewById(R.id.mapsBack);
        back.setOnClickListener(v -> {
            if (isNavigating) stopNavigation();
            else finish();
        });
    }

    // ── carte ─────────────────────────────────────────────────────
    private void setupMap() {
        mapView.onCreate(null);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapLibreMap map) {
                mapLibreMap = map;
                map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(46.6, 2.0))
                    .zoom(6.0)
                    .build());
                loadStyle(mapStyleIndex);

                // Appui long sur la carte = pose un repère, façon Google Maps
                map.addOnMapLongClickListener(latLng -> {
                    addWaypoint(latLng);
                    return true;
                });

                // Appui simple sur un signalement = voir/confirmer/retirer, façon Waze
                map.addOnMapClickListener(latLng -> {
                    handleMapTap(latLng);
                    return false;
                });
            }
        });
    }

    private void loadStyle(int idx) {
        if (mapLibreMap == null) return;
        String url = "https://tiles.openfreemap.org/styles/" + OPENFREEMAP_STYLES[idx];
        styleReady = false;
        mapLibreMap.setStyle(new Style.Builder().fromUri(url), style -> {
            mapStyle = style;
            styleReady = true;
            setup3DBuildingsLayer(style);
            setupRouteLayer(style);
            setupUserLocationLayer(style);
            setupReportsLayer(style);
            setupWaypointsLayer(style);
            // Re-appliquer l'état courant après un (re)chargement de style
            if (is3D) apply3D(true);
            if (hasLocation) updateLocationLayer();
            if (currentRouteCoords != null) drawRouteLayer(currentRouteCoords);
            if (hasDest) updateDestLayer();
            updateWaypointsLayer();
            fetchReports();
        });
    }

    // Couche bâtiments 3D — masquée par défaut. Dans un try/catch : si le schéma de
    // tuiles d'un style donné ne fournit pas la source-layer 'building', on ne casse
    // pas toute la carte pour autant, on perd juste la 3D sur ce style.
    private void setup3DBuildingsLayer(Style style) {
        try {
            if (style.getLayer("lea-3d-buildings") != null) return;
            FillExtrusionLayer layer = new FillExtrusionLayer("lea-3d-buildings", "openmaptiles");
            layer.setSourceLayer("building");
            layer.setMinZoom(14f);
            layer.setProperties(
                PropertyFactory.fillExtrusionColor("#3b4a6b"),
                PropertyFactory.fillExtrusionHeight(
                    Expression.coalesce(Expression.get("render_height"), Expression.literal(12f))),
                PropertyFactory.fillExtrusionOpacity(0.85f),
                PropertyFactory.visibility(is3D ? "visible" : "none")
            );
            style.addLayer(layer);
        } catch (Exception e) {
            android.util.Log.w("LeaMaps", "Couche 3D indisponible sur ce style : " + e.getMessage());
        }
    }

    // ── Source/couche position utilisateur ─────────────────────────
    private void setupUserLocationLayer(Style style) {
        if (style.getSource("lea-user-location") == null) {
            style.addSource(new GeoJsonSource("lea-user-location", emptyFeatureCollection()));
        }
        if (style.getLayer("lea-user-location-layer") == null) {
            CircleLayer layer = new CircleLayer("lea-user-location-layer", "lea-user-location");
            layer.setProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor("#00ffff"),
                PropertyFactory.circleStrokeColor("#ffffff"),
                PropertyFactory.circleStrokeWidth(3f)
            );
            style.addLayer(layer);
        }
        if (style.getSource("lea-dest") == null) {
            style.addSource(new GeoJsonSource("lea-dest", emptyFeatureCollection()));
        }
        if (style.getLayer("lea-dest-layer") == null) {
            CircleLayer layer = new CircleLayer("lea-dest-layer", "lea-dest");
            layer.setProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor("#ef4444"),
                PropertyFactory.circleStrokeColor("#ffffff"),
                PropertyFactory.circleStrokeWidth(3f)
            );
            style.addLayer(layer);
        }
    }

    private void setupRouteLayer(Style style) {
        if (style.getSource("lea-route") == null) {
            style.addSource(new GeoJsonSource("lea-route", emptyFeatureCollection()));
        }
        if (style.getLayer("lea-route-layer") == null) {
            LineLayer layer = new LineLayer("lea-route-layer", "lea-route");
            layer.setProperties(
                PropertyFactory.lineColor("#33CCFF"),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineCap("round"),
                PropertyFactory.lineJoin("round")
            );
            style.addLayerBelow(layer, "lea-user-location-layer");
        }
    }

    private void setupReportsLayer(Style style) {
        if (style.getSource("lea-reports") == null) {
            style.addSource(new GeoJsonSource("lea-reports", emptyFeatureCollection()));
        }
        if (style.getLayer("lea-reports-layer") == null) {
            CircleLayer layer = new CircleLayer("lea-reports-layer", "lea-reports");
            layer.setProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(Expression.get("color")),
                PropertyFactory.circleStrokeColor("#ffffff"),
                PropertyFactory.circleStrokeWidth(2f)
            );
            style.addLayer(layer);
        }
    }

    private void setupWaypointsLayer(Style style) {
        if (style.getSource("lea-waypoints") == null) {
            style.addSource(new GeoJsonSource("lea-waypoints", emptyFeatureCollection()));
        }
        if (style.getLayer("lea-waypoints-layer") == null) {
            CircleLayer layer = new CircleLayer("lea-waypoints-layer", "lea-waypoints");
            layer.setProperties(
                PropertyFactory.circleRadius(10f),
                PropertyFactory.circleColor("#38BDF8"),
                PropertyFactory.circleStrokeColor("#ffffff"),
                PropertyFactory.circleStrokeWidth(3f)
            );
            style.addLayer(layer);
        }
    }

    // ── Repères par appui long (façon Google Maps) ─────────────────
    // Chaque appui long ajoute un repère et ouvre le panneau itinéraire — l'utilisateur
    // choisit ensuite le mode (voiture/marche/vélo) et appuie sur "Calculer" pour lancer
    // le calcul (pas de calcul automatique immédiat, pour laisser le choix du mode).
    private void addWaypoint(LatLng point) {
        longPressWaypoints.add(point);
        updateWaypointsLayer();
        Toast.makeText(this, "Repère " + longPressWaypoints.size() + " posé — choisis un mode puis Calculer",
            Toast.LENGTH_SHORT).show();

        if (!hasLocation) {
            Toast.makeText(this, "GPS non disponible — impossible de calculer l'itinéraire", Toast.LENGTH_SHORT).show();
            return;
        }
        showPanel(1);
        routeHelpText.setText(longPressWaypoints.size() == 1
            ? "1 repère posé — choisis un mode puis appuie sur Calculer"
            : longPressWaypoints.size() + " repères posés — choisis un mode puis appuie sur Calculer");
        routeHelpText.setVisibility(View.VISIBLE);
    }

    // Lance le calcul pour les repères posés par appui long (GPS → repère 1 → repère 2 → ...)
    private void calculateWaypointRoute() {
        List<LatLng> routePoints = new ArrayList<>();
        routePoints.add(new LatLng(currentLat, currentLon));
        routePoints.addAll(longPressWaypoints);
        String label = longPressWaypoints.size() == 1
            ? "Repère"
            : longPressWaypoints.size() + " repères";
        fetchMultiPointRoute(routePoints, label);
    }

    private void updateWaypointsLayer() {
        if (!styleReady || mapStyle == null) return;
        GeoJsonSource src = mapStyle.getSourceAs("lea-waypoints");
        if (src == null) return;
        StringBuilder features = new StringBuilder();
        for (LatLng p : longPressWaypoints) {
            if (features.length() > 0) features.append(",");
            features.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
                .append(p.getLongitude()).append(",").append(p.getLatitude())
                .append("]},\"properties\":{}}");
        }
        src.setGeoJson("{\"type\":\"FeatureCollection\",\"features\":[" + features + "]}");
    }

    private void clearWaypoints() {
        longPressWaypoints.clear();
        updateWaypointsLayer();
    }

    private String emptyFeatureCollection() {
        return "{\"type\":\"FeatureCollection\",\"features\":[]}";
    }

    private void updateLocationLayer() {
        if (!styleReady || mapStyle == null) return;
        GeoJsonSource src = mapStyle.getSourceAs("lea-user-location");
        if (src == null) return;
        String geojson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
            + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[" + currentLon + "," + currentLat + "]},"
            + "\"properties\":{}}]}";
        src.setGeoJson(geojson);
    }

    private void updateDestLayer() {
        if (!styleReady || mapStyle == null) return;
        GeoJsonSource src = mapStyle.getSourceAs("lea-dest");
        if (src == null) return;
        String geojson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
            + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[" + destLon + "," + destLat + "]},"
            + "\"properties\":{}}]}";
        src.setGeoJson(geojson);
    }

    private void clearDestLayer() {
        hasDest = false;
        if (!styleReady || mapStyle == null) return;
        GeoJsonSource src = mapStyle.getSourceAs("lea-dest");
        if (src != null) src.setGeoJson(emptyFeatureCollection());
    }

    private void drawRouteLayer(List<double[]> coords) {
        if (!styleReady || mapStyle == null) return;
        GeoJsonSource src = mapStyle.getSourceAs("lea-route");
        if (src == null) return;
        StringBuilder coordsStr = new StringBuilder();
        for (int i = 0; i < coords.size(); i++) {
            double[] c = coords.get(i);
            if (i > 0) coordsStr.append(",");
            coordsStr.append("[").append(c[1]).append(",").append(c[0]).append("]"); // [lon,lat]
        }
        String geojson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
            + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[" + coordsStr + "]},"
            + "\"properties\":{}}]}";
        src.setGeoJson(geojson);
    }

    private void clearRouteLayer() {
        currentRouteCoords = null;
        if (!styleReady || mapStyle == null) return;
        GeoJsonSource src = mapStyle.getSourceAs("lea-route");
        if (src != null) src.setGeoJson(emptyFeatureCollection());
    }

    // ── Vue 3D ────────────────────────────────────────────────────
    private void toggle3D() {
        is3D = !is3D;
        apply3D(true);
        btn3D.setBackground(androidx.core.content.ContextCompat.getDrawable(this,
            is3D ? R.drawable.lea_map_btn_active_bg : R.drawable.lea_map_btn_bg));
        btn3D.setTextColor(is3D ? 0xFFFFFFFF : 0xFF93C5FD);
    }

    private void apply3D(boolean animate) {
        if (mapLibreMap == null) return;
        try {
            CameraPosition current = mapLibreMap.getCameraPosition();
            CameraPosition target = new CameraPosition.Builder(current)
                .tilt(is3D ? 60 : 0)
                .build();
            if (animate) mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(target), 600);
            else mapLibreMap.setCameraPosition(target);

            if (mapStyle != null && mapStyle.getLayer("lea-3d-buildings") != null) {
                mapStyle.getLayer("lea-3d-buildings")
                    .setProperties(PropertyFactory.visibility(is3D ? "visible" : "none"));
            }
        } catch (Exception e) {
            android.util.Log.w("LeaMaps", "Bascule 3D échouée : " + e.getMessage());
        }
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
        updateLocationLayer();
        checkReportsProximity();

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
            if (hasLocation) centerCamera(currentLat, currentLon, 15.0);
            else Toast.makeText(this, "GPS en attente...", Toast.LENGTH_SHORT).show();
        });

        tabRoute.setOnClickListener(v -> showPanel(1));

        tabWeather.setOnClickListener(v -> {
            showPanel(2);
            fetchWeatherWithFallback();
        });

        findViewById(R.id.btnCenter).setOnClickListener(v -> {
            if (hasLocation) centerCamera(currentLat, currentLon, 15.0);
            else Toast.makeText(this, "GPS en cours...", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnZoomIn).setOnClickListener(v -> {
            if (mapLibreMap != null) mapLibreMap.animateCamera(CameraUpdateFactory.zoomBy(1));
        });
        findViewById(R.id.btnZoomOut).setOnClickListener(v -> {
            if (mapLibreMap != null) mapLibreMap.animateCamera(CameraUpdateFactory.zoomBy(-1));
        });

        btn3D.setOnClickListener(v -> toggle3D());

        findViewById(R.id.btnStyleCycle).setOnClickListener(v -> {
            mapStyleIndex = (mapStyleIndex + 1) % OPENFREEMAP_STYLES.length;
            loadStyle(mapStyleIndex);
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

        // Calcul itinéraire — priorité aux repères posés par appui long, sinon la destination tapée
        findViewById(R.id.btnCalculateRoute).setOnClickListener(v -> {
            if (!hasLocation) { Toast.makeText(this, "GPS non disponible", Toast.LENGTH_SHORT).show(); return; }
            if (!longPressWaypoints.isEmpty()) {
                calculateWaypointRoute();
                return;
            }
            String dest = routeDestInput.getText().toString().trim();
            if (dest.isEmpty()) { Toast.makeText(this, "Entrez une destination ou appuie longuement sur la carte", Toast.LENGTH_SHORT).show(); return; }
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

        // Signalement communautaire
        btnReportToggle.setOnClickListener(v -> {
            reportMenuOpen = !reportMenuOpen;
            reportMenu.setVisibility(reportMenuOpen ? View.VISIBLE : View.GONE);
        });
        findViewById(R.id.reportAccident).setOnClickListener(v -> submitReport("accident"));
        findViewById(R.id.reportRadar).setOnClickListener(v -> submitReport("radar"));
        findViewById(R.id.reportBouchon).setOnClickListener(v -> submitReport("bouchon"));
        findViewById(R.id.reportObstacle).setOnClickListener(v -> submitReport("obstacle"));
        findViewById(R.id.reportControle).setOnClickListener(v -> submitReport("controle"));
        findViewById(R.id.reportTravaux).setOnClickListener(v -> submitReport("travaux"));
    }

    private void centerCamera(double lat, double lon, double zoom) {
        if (mapLibreMap == null) return;
        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), zoom));
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
        // leftControls (3D/zoom/style/SOS) masqué aussi — sinon il se retrouve mal
        // positionné une fois mapsTopBar caché et peut recouvrir la croix "arrêter".
        mapsTopBar.setVisibility(View.GONE);
        routePanel.setVisibility(View.GONE);
        weatherPanel.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
        btnReportToggle.setVisibility(View.GONE);
        leftControls.setVisibility(View.GONE);

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
        if (hasLocation) centerCamera(currentLat, currentLon, 16.0);
    }

    private void stopNavigation() {
        isNavigating = false;

        navInstructionBar.setVisibility(View.GONE);
        navBottomBar.setVisibility(View.GONE);

        mapsTopBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        btnReportToggle.setVisibility(View.VISIBLE);
        leftControls.setVisibility(View.VISIBLE);

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

    // Le mode nuit est un calque DOM superposé (nightOverlay), jamais un filtre posé
    // sur le rendu GL lui-même — un filtre CSS/GL cassait le rendu sur certains GPU
    // mobiles (bug corrigé sur la version web, appliqué ici dès le départ).
    private void applyTheme(String mode) {
        boolean isNight;
        switch (mode) {
            case "day":   isNight = false; break;
            case "night": isNight = true;  break;
            case "auto":
            default:
                int uiMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                isNight = (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
                break;
        }
        nightOverlay.setVisibility(isNight ? View.VISIBLE : View.GONE);
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
            case "day":   desc = "Carte claire forcée"; break;
            case "night": desc = "Carte sombre forcée"; break;
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
            btnSOS.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFDC2626));
            btnSOS.setText("SOS ●");
            Toast.makeText(this, "SOS activé — envoi position à Léa", Toast.LENGTH_SHORT).show();
            String coords = hasLocation ? String.format("%.6f,%.6f", currentLat, currentLon) : "inconnue";
            http.newCall(new Request.Builder().url(serverHost + "/api/sos?coords=" + coords).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call c, IOException e) {}
                    @Override public void onResponse(Call c, Response r) { r.close(); }
                });
        } else {
            btnSOS.setBackgroundTintList(null);
            btnSOS.setText("SOS");
        }
    }

    // ── Signalement communautaire (façon Waze) ─────────────────────
    private void submitReport(String type) {
        reportMenuOpen = false;
        reportMenu.setVisibility(View.GONE);

        if (!hasLocation) {
            Toast.makeText(this, "Position GPS requise pour signaler.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("username", currentUser);
            body.put("type", type);
            body.put("lat", currentLat);
            body.put("lng", currentLon);

            RequestBody reqBody = RequestBody.create(body.toString(), JSON);
            http.newCall(new Request.Builder()
                .url(serverHost + "/api/maps/report")
                .post(reqBody)
                .build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call c, IOException e) {
                        runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                            "Échec de l'envoi du signalement.", Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onResponse(Call c, Response r) {
                        r.close();
                        runOnUiThread(() -> {
                            Toast.makeText(LeaMapsActivity.this, "Signalé !", Toast.LENGTH_SHORT).show();
                            fetchReports();
                        });
                    }
                });
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchReports() {
        String url = serverHost + "/api/maps/reports?username=" + currentUser;
        http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) { /* silencieux — pas critique */ }
            @Override public void onResponse(Call c, Response r) {
                if (!r.isSuccessful() || r.body() == null) { r.close(); return; }
                try {
                    JSONArray reports = new JSONArray(r.body().string());
                    List<JSONObject> freshList = new ArrayList<>();
                    StringBuilder features = new StringBuilder();
                    for (int i = 0; i < reports.length(); i++) {
                        JSONObject rep = reports.getJSONObject(i);
                        freshList.add(rep);
                        String id    = rep.optString("id", "");
                        String type  = rep.optString("type", "obstacle");
                        String color = REPORT_COLORS.getOrDefault(type, "#a855f7");
                        double lat = rep.getDouble("lat");
                        double lng = rep.getDouble("lng");
                        if (features.length() > 0) features.append(",");
                        features.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
                            .append(lng).append(",").append(lat)
                            .append("]},\"properties\":{\"id\":\"").append(id)
                            .append("\",\"type\":\"").append(type)
                            .append("\",\"color\":\"").append(color).append("\"}}");
                    }
                    String geojson = "{\"type\":\"FeatureCollection\",\"features\":[" + features + "]}";
                    runOnUiThread(() -> {
                        cachedReports = freshList;
                        if (styleReady && mapStyle != null) {
                            GeoJsonSource src = mapStyle.getSourceAs("lea-reports");
                            if (src != null) src.setGeoJson(geojson);
                        }
                        checkReportsProximity();
                    });
                } catch (Exception ignored) {
                } finally { r.close(); }
            }
        });
    }

    // ── Interaction sur un signalement (façon Waze) ─────────────────
    // Toucher un point sur la carte : si ça touche un signalement, propose de confirmer
    // ("toujours là") ou de le retirer ("plus là").
    private void handleMapTap(LatLng tapLatLng) {
        if (mapLibreMap == null || mapStyle == null || mapStyle.getLayer("lea-reports-layer") == null) return;
        try {
            android.graphics.PointF screenPoint = mapLibreMap.getProjection().toScreenLocation(tapLatLng);
            List<org.maplibre.geojson.Feature> hits = mapLibreMap.queryRenderedFeatures(screenPoint, "lea-reports-layer");
            if (hits.isEmpty()) return;
            org.maplibre.geojson.Feature f = hits.get(0);
            String id   = f.hasProperty("id")   ? f.getStringProperty("id")   : null;
            String type = f.hasProperty("type") ? f.getStringProperty("type") : null;
            if (id == null || id.isEmpty()) return;
            showReportDialog(id, type);
        } catch (Exception ignored) {}
    }

    private void showReportDialog(String id, String type) {
        String emoji = REPORT_EMOJI.getOrDefault(type, "⚠️");
        String label = REPORT_LABELS.getOrDefault(type, "Signalement");
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(emoji + "  " + label)
            .setMessage("Ce signalement est-il toujours d'actualité ?")
            .setPositiveButton("✅ Toujours là", (dialog, which) -> confirmReport(id))
            .setNegativeButton("❌ Plus là", (dialog, which) -> dismissReport(id))
            .setNeutralButton("Fermer", null)
            .show();
    }

    private void confirmReport(String id) {
        String url = serverHost + "/api/maps/report/" + id + "/confirm?username="
            + java.net.URLEncoder.encode(currentUser, java.nio.charset.StandardCharsets.UTF_8);
        http.newCall(new Request.Builder()
            .url(url)
            .post(RequestBody.create("{}", JSON))
            .build())
            .enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {
                    runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                        "Échec de la confirmation (réseau).", Toast.LENGTH_SHORT).show());
                }
                @Override public void onResponse(Call c, Response r) {
                    boolean ok = r.isSuccessful();
                    r.close();
                    runOnUiThread(() -> {
                        if (ok) {
                            Toast.makeText(LeaMapsActivity.this, "Merci, confirmé !", Toast.LENGTH_SHORT).show();
                            fetchReports();
                        } else {
                            Toast.makeText(LeaMapsActivity.this, "Échec de la confirmation.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
    }

    private void dismissReport(String id) {
        String url = serverHost + "/api/maps/report/" + id + "?username="
            + java.net.URLEncoder.encode(currentUser, java.nio.charset.StandardCharsets.UTF_8);
        http.newCall(new Request.Builder()
            .url(url)
            .delete()
            .build())
            .enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {
                    runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                        "Échec du retrait (réseau).", Toast.LENGTH_SHORT).show());
                }
                @Override public void onResponse(Call c, Response r) {
                    boolean ok = r.isSuccessful();
                    r.close();
                    runOnUiThread(() -> {
                        if (ok) {
                            alertedReportIds.remove(id);
                            Toast.makeText(LeaMapsActivity.this, "Signalement retiré, merci !", Toast.LENGTH_SHORT).show();
                            fetchReports();
                        } else {
                            Toast.makeText(LeaMapsActivity.this, "Échec du retrait.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
    }

    // ── Alerte de proximité (façon Waze) — prévient quand on approche d'un signalement ──
    private void checkReportsProximity() {
        if (!hasLocation) return;
        for (JSONObject rep : cachedReports) {
            try {
                String id = rep.optString("id", "");
                if (id.isEmpty() || alertedReportIds.contains(id)) continue;
                double lat = rep.getDouble("lat");
                double lng = rep.getDouble("lng");
                double dist = haversine(currentLat, currentLon, lat, lng);
                if (dist <= PROXIMITY_ALERT_METERS) {
                    alertedReportIds.add(id);
                    String type  = rep.optString("type", "obstacle");
                    String emoji = REPORT_EMOJI.getOrDefault(type, "⚠️");
                    String label = REPORT_LABELS.getOrDefault(type, "Signalement");
                    Toast.makeText(this, emoji + " " + label + " signalé à proximité (" + Math.round(dist) + " m)",
                        Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
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
        List<LatLng> pts = new ArrayList<>();
        pts.add(new LatLng(sLat, sLon));
        pts.add(new LatLng(eLat, eLon));
        fetchMultiPointRoute(pts, destName);
    }

    // Itinéraire à travers plusieurs points (départ GPS + repères posés par appui long,
    // façon Google Maps) — OSRM accepte nativement une chaîne de coordonnées, pas
    // seulement deux points.
    private void fetchMultiPointRoute(List<LatLng> points, String destName) {
        if (points.size() < 2) return;
        String baseUrl;
        switch (routeMode) {
            case "walking": baseUrl = "https://routing.openstreetmap.de/routed-foot/route/v1/foot/"; break;
            case "cycling": baseUrl = "https://routing.openstreetmap.de/routed-bike/route/v1/bike/"; break;
            default:        baseUrl = "https://router.project-osrm.org/route/v1/driving/"; break;
        }

        StringBuilder coordsPart = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) coordsPart.append(";");
            coordsPart.append(points.get(i).getLongitude()).append(",").append(points.get(i).getLatitude());
        }
        String url = baseUrl + coordsPart + "?overview=full&geometries=geojson&steps=true";

        LatLng dest = points.get(points.size() - 1);
        double eLat = dest.getLatitude(), eLon = dest.getLongitude();

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
                    List<double[]> routePoints = new ArrayList<>();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i);
                        routePoints.add(new double[]{ c.getDouble(1), c.getDouble(0) }); // [lat, lon]
                    }

                    // Un "leg" par segment entre deux points consécutifs — on les concatène
                    // pour avoir les instructions tour-par-tour sur tout le trajet complet.
                    JSONArray legs = route.getJSONArray("legs");
                    JSONArray allSteps = new JSONArray();
                    for (int i = 0; i < legs.length(); i++) {
                        JSONArray legSteps = legs.getJSONObject(i).getJSONArray("steps");
                        for (int j = 0; j < legSteps.length(); j++) allSteps.put(legSteps.getJSONObject(j));
                    }

                    runOnUiThread(() -> {
                        drawRoute(routePoints, distKm, durMin, eLat, eLon, destName);
                        storeSteps(allSteps);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(LeaMapsActivity.this,
                        "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void drawRoute(List<double[]> points, double distKm, double durMin,
                           double dLat, double dLon, String destName) {
        currentRouteCoords = points;
        drawRouteLayer(points);

        destLat = dLat; destLon = dLon; destLabel = destName; hasDest = true;
        updateDestLayer();

        // Cadrage sur l'itinéraire
        if (!points.isEmpty() && mapLibreMap != null) {
            org.maplibre.android.geometry.LatLngBounds.Builder boundsBuilder =
                new org.maplibre.android.geometry.LatLngBounds.Builder();
            for (double[] p : points) boundsBuilder.include(new LatLng(p[0], p[1]));
            try {
                mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80));
            } catch (Exception ignored) {}
        }

        // Sauvegarder pour la navigation
        navTotalDistKm = distKm;
        navTotalDurMin = durMin;
        navDestName    = destName;

        // Remplir le résumé Waze
        routeDestLabel.setText(destName);
        navSummaryMin.setText(durMin >= 60
            ? String.format("%dh%02d", (int)(durMin / 60), (int)(durMin % 60))
            : String.format("%.0f", durMin));
        if ("walking".equals(routeMode)) {
            // Estimation basée sur une foulée moyenne de 0,75 m — indicatif, pas une mesure exacte
            long steps = Math.round(distKm * 1000 / 0.75);
            navSummaryDist.setText(String.valueOf(steps));
            navSummaryDistUnit.setText("pas");
        } else {
            navSummaryDist.setText(String.format("%.1f", distKm));
            navSummaryDistUnit.setText("km");
        }

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
        clearRouteLayer();
        clearDestLayer();
        clearWaypoints();
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
