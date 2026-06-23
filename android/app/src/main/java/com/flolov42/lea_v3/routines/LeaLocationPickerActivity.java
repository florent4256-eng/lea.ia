package com.flolov42.lea_v3.routines;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaLocationPickerActivity extends Activity {

    public static final String EXTRA_RESULT_JSON = "location_json";

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;

    private MapView            mapView;
    private Marker             marker;
    private MyLocationNewOverlay locationOverlay;
    private TextView           radiusLabel;
    private EditText           etPlace;
    private int                radius = 200;
    private double             lat = 0, lng = 0;
    private boolean            markerPlaced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Configuration.getInstance().setUserAgentValue(getPackageName());
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
            setContentView(buildLayout());
            LeaFeatureDetailActivity.applyImmersive(this);
            initMap();
        } catch (Throwable e) {
            Toast.makeText(this, "Erreur carte : " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // ── Header ─────────────────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(BG);
        header.setPadding(dp(4), dp(22), dp(16), dp(10));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        back.setBackground(null);
        back.setTextSize(22f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));

        TextView title = new TextView(this);
        title.setText("📍 Choisir un lieu");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        header.addView(title, tlp);

        Button myLocBtn = new Button(this);
        myLocBtn.setText("📡");
        myLocBtn.setTextSize(20f);
        myLocBtn.setTextColor(CYAN);
        myLocBtn.setBackground(null);
        myLocBtn.setOnClickListener(v -> goToMyLocation());
        header.addView(myLocBtn, new LinearLayout.LayoutParams(dp(52), dp(48)));

        root.addView(header);

        // ── Hint ───────────────────────────────────────────────────────────────
        TextView hint = new TextView(this);
        hint.setText("Appuie sur la carte pour placer l'épingle 📍");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(12f);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(2), 0, dp(4));
        root.addView(hint);

        // ── MapView ────────────────────────────────────────────────────────────
        mapView = new MapView(this);
        root.addView(mapView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ── Panneau bas ────────────────────────────────────────────────────────
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(0xFF012040);
        bottom.setPadding(dp(16), dp(12), dp(16), dp(20));

        // Nom du lieu
        etPlace = new EditText(this);
        etPlace.setHint("Nom du lieu (ex: MAISON)");
        etPlace.setTextColor(Color.WHITE);
        etPlace.setHintTextColor(0xFF37474F);
        etPlace.setTextSize(14f);
        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0xFF011627);
        etBg.setCornerRadius(dp(10));
        etBg.setStroke(dp(1), 0xFF1E3A52);
        etPlace.setBackground(etBg);
        etPlace.setPadding(dp(12), dp(10), dp(12), dp(10));
        bottom.addView(etPlace, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Rayon
        LinearLayout radiusRow = new LinearLayout(this);
        radiusRow.setOrientation(LinearLayout.HORIZONTAL);
        radiusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rrLp.setMargins(0, dp(12), 0, dp(4));
        radiusRow.setLayoutParams(rrLp);

        radiusLabel = new TextView(this);
        radiusLabel.setText("Rayon : 200m");
        radiusLabel.setTextColor(CYAN);
        radiusLabel.setTextSize(13f);
        radiusLabel.setTypeface(null, Typeface.BOLD);
        radiusLabel.setMinWidth(dp(120));
        radiusRow.addView(radiusLabel);

        SeekBar sb = new SeekBar(this);
        sb.setMax(39);
        sb.setProgress(3); // default ~200m  (progressToRadius(3) = 50 + 3*50 = 200)
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                radius = progressToRadius(p);
                radiusLabel.setText("Rayon : " + radius + "m");
                updateCircle();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        radiusRow.addView(sb, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bottom.addView(radiusRow);

        // Bouton confirmer
        Button confirm = new Button(this);
        confirm.setText("✔  CONFIRMER CE LIEU");
        confirm.setTextColor(Color.BLACK);
        confirm.setTextSize(14f);
        confirm.setTypeface(null, Typeface.BOLD);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(CYAN);
        cBg.setCornerRadius(dp(12));
        confirm.setBackground(cBg);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        cLp.setMargins(0, dp(10), 0, 0);
        confirm.setLayoutParams(cLp);
        confirm.setOnClickListener(v -> confirmLocation());
        bottom.addView(confirm);

        root.addView(bottom);
        return root;
    }

    private static int progressToRadius(int p) {
        // 0-19: 50m à 1000m (pas de 50m)
        // 20-39: 1000m à 5000m (pas de 200m)
        if (p <= 19) return 50 + p * 50;
        return 1000 + (p - 20) * 200;
    }

    private void initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Rotation
        RotationGestureOverlay rotOvl = new RotationGestureOverlay(mapView);
        rotOvl.setEnabled(true);
        mapView.getOverlays().add(rotOvl);

        // Ma position
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);

        // Tap pour poser l'épingle
        mapView.getOverlays().add(0, new Overlay() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView map) {
                GeoPoint p = (GeoPoint) map.getProjection().fromPixels((int) e.getX(), (int) e.getY());
                placeMarker(p);
                return true;
            }
        });

        goToMyLocation();
    }

    private void placeMarker(GeoPoint p) {
        lat = p.getLatitude();
        lng = p.getLongitude();
        markerPlaced = true;

        if (marker == null) {
            marker = new Marker(mapView);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }
        marker.setPosition(p);
        mapView.getController().animateTo(p);

        // Géocodage inverse en thread bg pour proposer un nom
        final double capLat = lat, capLng = lng;
        new Thread(() -> {
            try {
                Geocoder geo = new Geocoder(this, Locale.FRENCH);
                List<android.location.Address> addrs = geo.getFromLocation(capLat, capLng, 1);
                if (addrs != null && !addrs.isEmpty()) {
                    android.location.Address a = addrs.get(0);
                    String name = a.getFeatureName() != null ? a.getFeatureName()
                                : (a.getThoroughfare() != null ? a.getThoroughfare() : "LIEU");
                    runOnUiThread(() -> {
                        if (etPlace.getText().toString().trim().isEmpty())
                            etPlace.setText(name.toUpperCase(Locale.FRENCH));
                    });
                }
            } catch (Exception ignored) {}
        }).start();

        updateCircle();
    }

    private void updateCircle() {
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay o : mapView.getOverlays()) {
            if (o instanceof Polygon) toRemove.add(o);
        }
        mapView.getOverlays().removeAll(toRemove);

        if (!markerPlaced) { mapView.invalidate(); return; }

        Polygon circle = new Polygon();
        circle.setPoints(Polygon.pointsAsCircle(new GeoPoint(lat, lng), radius));
        circle.getFillPaint().setColor(0x2200E5FF);
        circle.getOutlinePaint().setColor(CYAN);
        circle.getOutlinePaint().setStrokeWidth(3f);
        mapView.getOverlays().add(circle);
        mapView.invalidate();
    }

    private void goToMyLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = null;
            try { loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); }
            catch (SecurityException ignored) {}
            if (loc == null) {
                try { loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); }
                catch (SecurityException ignored) {}
            }
            if (loc != null) {
                GeoPoint gp = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                mapView.getController().animateTo(gp);
                mapView.getController().setZoom(17.0);
            } else {
                mapView.getController().setCenter(new GeoPoint(46.603354, 1.888334));
                mapView.getController().setZoom(6.0);
            }
        } catch (Throwable ignored) {}
    }

    private void confirmLocation() {
        if (!markerPlaced) {
            Toast.makeText(this, "Appuie d'abord sur la carte pour choisir un lieu", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String place = etPlace.getText().toString().trim().toUpperCase(Locale.FRENCH);
            if (place.isEmpty()) place = "LIEU";
            JSONObject o = new JSONObject();
            o.put("type",   "LOCATION");
            o.put("place",  place);
            o.put("lat",    lat);
            o.put("lng",    lng);
            o.put("radius", radius);
            Intent result = new Intent();
            result.putExtra(EXTRA_RESULT_JSON, o.toString());
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause()  { super.onPause();  mapView.onPause();  }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
