package com.flolov42.lea_v3.routines;

import com.flolov42.lea_v3.ui.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sélecteur visuel de condition — style Bixby Routines.
 * Retourne EXTRA_RESULT_JSON avec le JSON de la condition choisie.
 */
public class LeaRoutineConditionPickerActivity extends Activity {

    public static final String EXTRA_RESULT_JSON = "condition_json";

    private static final int BG          = 0xFF011627;
    private static final int CYAN        = 0xFF00E5FF;
    private static final int CARD        = 0xFF012040;
    private static final int REQ_LOCATION = 201;

    // Condition type, emoji label, description
    private static final Object[][] CONDITIONS = {
        // { type, emoji, label, group }
        { "GROUP",            "",   "HEURE & DATE",              null },
        { "TIME_EXACT",       "🕐", "À une heure précise",       "Déclenche la routine à l'heure exacte choisie" },
        { "TIME",             "📅", "Dans une plage horaire",    "Active la routine entre deux heures" },
        { "DAY_OF_WEEK",      "📆", "Jour de la semaine",        "Active uniquement certains jours" },
        { "GROUP",            "",   "LIEU",                      null },
        { "LOCATION",         "📍", "Arrivée à un endroit",      "Détecte la position GPS (nécessite la localisation)" },
        { "GROUP",            "",   "CONNEXION",                  null },
        { "BLUETOOTH_CONNECTED","🔵","Bluetooth connecté",        "Quand un appareil Bluetooth se connecte" },
        { "WIFI_CONNECTED",   "📶", "WiFi connecté",             "Quand tu te connectes à un réseau WiFi" },
        { "HEADPHONES_CONNECTED","🎧","Casque / écouteurs connecté","Quand des écouteurs sont branchés" },
        { "GROUP",            "",   "APPAREIL",                   null },
        { "CHARGING",         "⚡", "En charge",                  "Quand le téléphone est branché au chargeur" },
        { "DISCHARGING",      "🔌", "Débranché du chargeur",     "Quand le chargeur est retiré" },
        { "BATTERY_LEVEL",    "🔋", "Batterie à X%",             "Quand le niveau de batterie atteint un seuil" },
        { "SCREEN_ON",        "💡", "Écran allumé",              "Quand tu allumes l'écran" },
        { "SCREEN_OFF",       "🌑", "Écran éteint",              "Quand l'écran s'éteint" },
        { "GROUP",            "",   "TÉLÉPHONE",                  null },
        { "INCOMING_CALL",    "📞", "Appel entrant",             "Quand tu reçois un appel" },
        { "CALL_ENDED",       "📴", "Appel terminé",             "Quand un appel se termine" },
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
            setContentView(buildLayout());
            LeaFeatureDetailActivity.applyImmersive(this);
        } catch (Throwable e) {
            android.widget.Toast.makeText(this, "Erreur conditions : " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOCATION && resultCode == RESULT_OK && data != null) {
            String json = data.getStringExtra(LeaLocationPickerActivity.EXTRA_RESULT_JSON);
            if (json != null) returnResult(json);
        }
    }

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // Header
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
        title.setText("Choisir une condition");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        header.addView(title, tlp);
        root.addView(header);

        // Séparateur
        View sep = new View(this);
        sep.setBackgroundColor(0xFF0D2137);
        root.addView(sep, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // Liste
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scroll.setBackgroundColor(BG);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(32));

        for (Object[] row : CONDITIONS) {
            String type  = (String) row[0];
            String emoji = (String) row[1];
            String label = (String) row[2];
            String desc  = row[3] != null ? (String) row[3] : null;

            if ("GROUP".equals(type)) {
                list.addView(buildGroupHeader(label));
            } else {
                final String t = type;
                list.addView(buildConditionItem(emoji, label, desc, v -> openConfig(t)));
            }
        }

        scroll.addView(list);
        root.addView(scroll);
        return root;
    }

    private View buildGroupHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF546E7A);
        tv.setTextSize(10f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(18), 0, dp(8));
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildConditionItem(String emoji, String label, String desc, View.OnClickListener onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFF0D2137);
        item.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        item.setLayoutParams(lp);

        // Icône
        TextView emojiV = new TextView(this);
        emojiV.setText(emoji);
        emojiV.setTextSize(24f);
        emojiV.setGravity(Gravity.CENTER);
        GradientDrawable eBg = new GradientDrawable();
        eBg.setColor(0xFF0D2137);
        eBg.setCornerRadius(dp(10));
        emojiV.setBackground(eBg);
        emojiV.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        eLp.setMargins(0, 0, dp(14), 0);
        item.addView(emojiV, eLp);

        // Texte
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        item.addView(col, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameV = new TextView(this);
        nameV.setText(label);
        nameV.setTextColor(Color.WHITE);
        nameV.setTextSize(14f);
        nameV.setTypeface(null, Typeface.BOLD);
        col.addView(nameV);

        if (desc != null && !desc.isEmpty()) {
            TextView descV = new TextView(this);
            descV.setText(desc);
            descV.setTextColor(0xFF546E7A);
            descV.setTextSize(11f);
            descV.setPadding(0, dp(2), 0, 0);
            col.addView(descV);
        }

        // Flèche
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(0xFF1E3A52);
        arrow.setTextSize(22f);
        item.addView(arrow);

        item.setOnClickListener(onClick);
        return item;
    }

    // ── Dialogs de configuration ──────────────────────────────────────────────

    private void openConfig(String type) {
        switch (type) {
            case "TIME_EXACT":        configTimeExact();       break;
            case "TIME":              configTimeRange();       break;
            case "DAY_OF_WEEK":       configDayOfWeek();       break;
            case "LOCATION":          configLocation();        break;
            case "BLUETOOTH_CONNECTED": configBluetooth();    break;
            case "WIFI_CONNECTED":    configWifi();            break;
            case "BATTERY_LEVEL":     configBatteryLevel();   break;
            case "CHARGING":          returnResult("{\"type\":\"CHARGING\"}");   break;
            case "DISCHARGING":       returnResult("{\"type\":\"DISCHARGING\"}"); break;
            case "SCREEN_ON":         returnResult("{\"type\":\"SCREEN_ON\"}");  break;
            case "SCREEN_OFF":        returnResult("{\"type\":\"SCREEN_OFF\"}"); break;
            case "HEADPHONES_CONNECTED": returnResult("{\"type\":\"HEADPHONES_CONNECTED\"}"); break;
            case "INCOMING_CALL":     returnResult("{\"type\":\"INCOMING_CALL\"}"); break;
            case "CALL_ENDED":        returnResult("{\"type\":\"CALL_ENDED\"}"); break;
        }
    }

    private void configTimeExact() {
        TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", "TIME_EXACT");
                o.put("time", String.format("%02d:%02d", h, m));
                returnResult(o.toString());
            } catch (Exception ignored) {}
        }, 8, 0, true);
        tpd.setTitle("À quelle heure ?");
        tpd.show();
    }

    private void configTimeRange() {
        final int[] startH = {8}, startM = {0}, endH = {18}, endM = {0};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(10), dp(20), dp(10));
        form.setBackgroundColor(BG);

        Button pickStart = styleDialogBtn("▶ Heure de début : 08:00");
        Button pickEnd   = styleDialogBtn("⏹ Heure de fin   : 18:00");

        pickStart.setOnClickListener(v -> new TimePickerDialog(this, (tw, h, m) -> {
            startH[0] = h; startM[0] = m;
            pickStart.setText("▶ Heure de début : " + String.format("%02d:%02d", h, m));
        }, startH[0], startM[0], true).show());

        pickEnd.setOnClickListener(v -> new TimePickerDialog(this, (tw, h, m) -> {
            endH[0] = h; endM[0] = m;
            pickEnd.setText("⏹ Heure de fin   : " + String.format("%02d:%02d", h, m));
        }, endH[0], endM[0], true).show());

        form.addView(pickStart);
        form.addView(spacer(8));
        form.addView(pickEnd);

        new AlertDialog.Builder(this)
            .setTitle("Plage horaire")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",  "TIME");
                    o.put("start", String.format("%02d:%02d", startH[0], startM[0]));
                    o.put("end",   String.format("%02d:%02d", endH[0],   endM[0]));
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configDayOfWeek() {
        String[] labels = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};
        boolean[] checked = {true, true, true, true, true, false, false};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(10), dp(20), dp(10));
        form.setBackgroundColor(BG);

        CheckBox[] boxes = new CheckBox[7];
        for (int i = 0; i < 7; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(labels[i]);
            cb.setChecked(checked[i]);
            cb.setTextColor(Color.WHITE);
            cb.setTextSize(14f);
            boxes[i] = cb;
            form.addView(cb);
        }

        new AlertDialog.Builder(this)
            .setTitle("Jours actifs")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    org.json.JSONArray days = new org.json.JSONArray();
                    for (int i = 0; i < 7; i++) {
                        if (boxes[i].isChecked()) days.put(i + 1);
                    }
                    JSONObject o = new JSONObject();
                    o.put("type", "DAY_OF_WEEK");
                    o.put("days", days);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configLocation() {
        startActivityForResult(
            new Intent(this, LeaLocationPickerActivity.class), REQ_LOCATION);
    }

    private void configBluetooth() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        scroll.addView(form);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null || !btAdapter.isEnabled()) {
            TextView warn = new TextView(this);
            warn.setText("⚠ Bluetooth non activé — active-le pour voir tes appareils jumelés.");
            warn.setTextColor(0xFFFF9800);
            warn.setTextSize(12f);
            form.addView(warn);
            form.addView(spacer(10));
        } else {
            Set<BluetoothDevice> bonded = null;
            try { bonded = btAdapter.getBondedDevices(); }
            catch (SecurityException ignored) {}

            if (bonded != null && !bonded.isEmpty()) {
                TextView sectionLbl = sectionLabel("APPAREILS JUMELÉS — appuie pour choisir");
                form.addView(sectionLbl);
                form.addView(spacer(6));

                for (BluetoothDevice device : bonded) {
                    String name;
                    try { name = device.getName(); }
                    catch (SecurityException e) { name = null; }
                    if (name == null || name.isEmpty()) name = "Appareil Bluetooth";

                    final String deviceName = name;
                    Button btn = styleDialogBtn("🔵  " + deviceName);
                    btn.setOnClickListener(v -> {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type",   "BLUETOOTH_CONNECTED");
                            o.put("device", deviceName);
                            returnResult(o.toString());
                        } catch (Exception ignored) {}
                    });
                    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    blp.setMargins(0, 0, 0, dp(6));
                    btn.setLayoutParams(blp);
                    form.addView(btn);
                }
                form.addView(spacer(12));
            }
        }

        // Option "n'importe quel appareil"
        Button anyBtn = styleDialogBtn("✦  N'importe quel appareil Bluetooth");
        anyBtn.setOnClickListener(v -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type",   "BLUETOOTH_CONNECTED");
                o.put("device", "");
                returnResult(o.toString());
            } catch (Exception ignored) {}
        });
        form.addView(anyBtn, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        form.addView(spacer(14));

        // Saisie manuelle
        TextView orLbl = new TextView(this);
        orLbl.setText("— OU ENTRER UN NOM MANUELLEMENT —");
        orLbl.setTextColor(0xFF546E7A);
        orLbl.setTextSize(10f);
        orLbl.setGravity(Gravity.CENTER);
        form.addView(orLbl);
        form.addView(spacer(6));
        EditText etDevice = styledEt("Nom de l'appareil", form);

        new AlertDialog.Builder(this)
            .setTitle("🔵 Bluetooth")
            .setView(scroll)
            .setPositiveButton("Confirmer le nom saisi", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",   "BLUETOOTH_CONNECTED");
                    o.put("device", etDevice.getText().toString().trim());
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configWifi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        scroll.addView(form);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String currentSsid = null;

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    android.net.Network net = cm.getActiveNetwork();
                    android.net.NetworkCapabilities caps = net != null ? cm.getNetworkCapabilities(net) : null;
                    if (caps != null && caps.hasTransport(
                            android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                        android.net.TransportInfo ti = caps.getTransportInfo();
                        if (ti instanceof WifiInfo) {
                            String raw = ((WifiInfo) ti).getSSID();
                            if (raw != null && !raw.equals("<unknown ssid>") && !raw.equals("\"\""))
                                currentSsid = raw.replace("\"", "");
                        }
                    }
                }
            } else if (wm != null && wm.isWifiEnabled()) {
                WifiInfo info = wm.getConnectionInfo();
                if (info != null) {
                    String raw = info.getSSID();
                    if (raw != null && !raw.equals("<unknown ssid>") && !raw.equals("\"\""))
                        currentSsid = raw.replace("\"", "");
                }
            }
        } catch (Exception ignored) {}

        // Réseau actuel
        if (currentSsid != null && !currentSsid.isEmpty()) {
            form.addView(sectionLabel("RÉSEAU ACTUEL"));
            form.addView(spacer(6));
            final String ssidCurrent = currentSsid;
            Button curBtn = styleDialogBtn("📶  " + currentSsid + "  ✔ connecté");
            curBtn.setOnClickListener(v -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "WIFI_CONNECTED");
                    o.put("ssid", ssidCurrent);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            });
            form.addView(curBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            form.addView(spacer(14));
        }

        // Réseaux détectés (scan précédent)
        if (wm != null) {
            try {
                List<ScanResult> results = wm.getScanResults();
                if (results != null && !results.isEmpty()) {
                    results.sort((a, b) -> b.level - a.level);
                    // Dédupliquer
                    List<String> ssids = new ArrayList<>();
                    java.util.Set<String> seen = new java.util.LinkedHashSet<>();
                    for (ScanResult r : results) {
                        if (r.SSID != null && !r.SSID.isEmpty() && seen.add(r.SSID))
                            ssids.add(r.SSID);
                    }
                    if (!ssids.isEmpty()) {
                        form.addView(sectionLabel("RÉSEAUX DÉTECTÉS"));
                        form.addView(spacer(6));
                        int max = Math.min(ssids.size(), 10);
                        for (int i = 0; i < max; i++) {
                            final String ssid = ssids.get(i);
                            if (ssid.equals(currentSsid)) continue;
                            Button btn = styleDialogBtn("📶  " + ssid);
                            btn.setOnClickListener(v -> {
                                try {
                                    JSONObject o = new JSONObject();
                                    o.put("type", "WIFI_CONNECTED");
                                    o.put("ssid", ssid);
                                    returnResult(o.toString());
                                } catch (Exception ignored) {}
                            });
                            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            blp.setMargins(0, 0, 0, dp(6));
                            btn.setLayoutParams(blp);
                            form.addView(btn);
                        }
                        form.addView(spacer(14));
                    }
                }
            } catch (Exception ignored) {}
        }

        // N'importe quel réseau
        Button anyBtn = styleDialogBtn("✦  N'importe quel réseau WiFi");
        anyBtn.setOnClickListener(v -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", "WIFI_CONNECTED");
                o.put("ssid", "");
                returnResult(o.toString());
            } catch (Exception ignored) {}
        });
        form.addView(anyBtn, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        form.addView(spacer(14));

        // Saisie manuelle
        TextView orLbl = new TextView(this);
        orLbl.setText("— OU ENTRER UN NOM MANUELLEMENT —");
        orLbl.setTextColor(0xFF546E7A);
        orLbl.setTextSize(10f);
        orLbl.setGravity(Gravity.CENTER);
        form.addView(orLbl);
        form.addView(spacer(6));
        EditText etSsid = styledEt("Nom du réseau WiFi (SSID)", form);

        new AlertDialog.Builder(this)
            .setTitle("📶 WiFi")
            .setView(scroll)
            .setPositiveButton("Confirmer le nom saisi", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "WIFI_CONNECTED");
                    o.put("ssid", etSsid.getText().toString().trim());
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configBatteryLevel() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(10), dp(20), dp(10));
        form.setBackgroundColor(BG);

        int[] pct = {20};
        TextView label = new TextView(this);
        label.setText("Batterie à : 20%");
        label.setTextColor(Color.WHITE);
        label.setTextSize(15f);
        label.setTypeface(null, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        form.addView(label);
        form.addView(spacer(12));

        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(20);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                pct[0] = p;
                label.setText("Batterie à : " + p + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        form.addView(sb);

        new AlertDialog.Builder(this)
            .setTitle("🔋 Niveau de batterie")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",    "BATTERY_LEVEL");
                    o.put("percent", pct[0]);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void returnResult(String json) {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_JSON, json);
        setResult(RESULT_OK, result);
        finish();
    }

    /** Libellé lisible pour le chip dans l'éditeur. */
    public static String conditionLabel(JSONObject c) {
        try {
            switch (c.optString("type", "")) {
                case "TIME_EXACT":           return "🕐 À " + c.optString("time");
                case "TIME":                 return "📅 " + c.optString("start") + " → " + c.optString("end");
                case "DAY_OF_WEEK":          return "📆 " + daysLabel(c);
                case "LOCATION":             return "📍 " + c.optString("place", "Lieu");
                case "BLUETOOTH_CONNECTED":  {
                    String dev = c.optString("device", "");
                    return "🔵 Bluetooth" + (dev.isEmpty() ? "" : ": " + dev);
                }
                case "WIFI_CONNECTED":       {
                    String ssid = c.optString("ssid", "");
                    return "📶 WiFi" + (ssid.isEmpty() ? "" : ": " + ssid);
                }
                case "HEADPHONES_CONNECTED": return "🎧 Casque connecté";
                case "CHARGING":             return "⚡ En charge";
                case "DISCHARGING":          return "🔌 Débranché";
                case "BATTERY_LEVEL":        return "🔋 Batterie ≤ " + c.optInt("percent") + "%";
                case "SCREEN_ON":            return "💡 Écran allumé";
                case "SCREEN_OFF":           return "🌑 Écran éteint";
                case "INCOMING_CALL":        return "📞 Appel entrant";
                case "CALL_ENDED":           return "📴 Appel terminé";
                default:                     return c.optString("type");
            }
        } catch (Exception e) { return "?"; }
    }

    private static String daysLabel(JSONObject c) {
        try {
            org.json.JSONArray arr = c.getJSONArray("days");
            String[] names = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                int d = arr.getInt(i);
                if (d >= 1 && d <= 7) {
                    if (sb.length() > 0) sb.append("·");
                    sb.append(names[d - 1]);
                }
            }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF546E7A);
        tv.setTextSize(10f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.12f);
        return tv;
    }

    private Button styleDialogBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(CYAN);
        b.setTextSize(13f);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF012040);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0xFF1E3A52);
        b.setBackground(bg);
        return b;
    }

    private EditText styledEt(String hint, LinearLayout parent) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(0xFF37474F);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF012040);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0xFF1E3A52);
        et.setBackground(bg);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        parent.addView(et, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
        return v;
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
