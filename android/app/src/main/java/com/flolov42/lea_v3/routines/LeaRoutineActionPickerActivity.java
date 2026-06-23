package com.flolov42.lea_v3.routines;

import com.flolov42.lea_v3.ui.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import org.json.JSONObject;

/**
 * Sélecteur visuel d'action — style Bixby Routines.
 * Retourne EXTRA_RESULT_JSON avec le JSON de l'action choisie.
 */
public class LeaRoutineActionPickerActivity extends Activity {

    public static final String EXTRA_RESULT_JSON = "action_json";

    private static final int BG    = 0xFF011627;
    private static final int GREEN = 0xFF4CAF50;
    private static final int CARD  = 0xFF012040;

    private static final Object[][] ACTIONS = {
        { "GROUP",        "",   "SON",             null },
        { "VOLUME",       "🔊", "Volume",          "Régler le volume musique et sonnerie" },
        { "SOUND_MODE",   "🔔", "Mode son",        "Normal · Vibreur · Silencieux" },
        { "GROUP",        "",   "ÉCRAN",           null },
        { "BRIGHTNESS",   "☀️", "Luminosité",      "Régler la luminosité de l'écran" },
        { "GROUP",        "",   "CONNECTIVITÉ",    null },
        { "WIFI",         "📶", "WiFi",            "Activer ou désactiver le WiFi" },
        { "BLUETOOTH",    "🔵", "Bluetooth",       "Activer ou désactiver le Bluetooth" },
        { "GROUP",        "",   "SYSTÈME",         null },
        { "DO_NOT_DISTURB","🚫","Ne pas déranger", "Mode silencieux total — bloque toutes les alertes" },
        { "FLASHLIGHT",   "🔦", "Lampe torche",    "Activer ou désactiver la lampe torche" },
        { "ALARM",        "⏰", "Alarme",          "Programmer une alarme" },
        { "GROUP",        "",   "LÉA",             null },
        { "SPEAK_TEXT",   "💬", "Léa dit…",        "Léa prononce un message à voix haute" },
        { "SHOW_NOTIFICATION","🔔","Notification",  "Afficher une notification personnalisée" },
        { "GROUP",        "",   "APPLICATIONS",    null },
        { "LAUNCH_APP",   "📱", "Ouvrir une app",  "Lancer une application installée" },
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
            android.widget.Toast.makeText(this, "Erreur actions : " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
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
        back.setTextColor(GREEN);
        back.setBackground(null);
        back.setTextSize(22f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));

        TextView title = new TextView(this);
        title.setText("Choisir une action");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        header.addView(title, tlp);
        root.addView(header);

        View sep = new View(this);
        sep.setBackgroundColor(0xFF0D2137);
        root.addView(sep, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scroll.setBackgroundColor(BG);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(32));

        for (Object[] row : ACTIONS) {
            String type  = (String) row[0];
            String emoji = (String) row[1];
            String label = (String) row[2];
            String desc  = row[3] != null ? (String) row[3] : null;

            if ("GROUP".equals(type)) {
                list.addView(buildGroupHeader(label));
            } else {
                final String t = type;
                list.addView(buildActionItem(emoji, label, desc, v -> openConfig(t)));
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

    private View buildActionItem(String emoji, String label, String desc, View.OnClickListener onClick) {
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

        TextView emojiV = new TextView(this);
        emojiV.setText(emoji);
        emojiV.setTextSize(24f);
        emojiV.setGravity(Gravity.CENTER);
        GradientDrawable eBg = new GradientDrawable();
        eBg.setColor(GREEN & 0x00FFFFFF | 0x1A000000);
        eBg.setCornerRadius(dp(10));
        emojiV.setBackground(eBg);
        emojiV.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        eLp.setMargins(0, 0, dp(14), 0);
        item.addView(emojiV, eLp);

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
            case "VOLUME":           configVolume();          break;
            case "SOUND_MODE":       configSoundMode();       break;
            case "BRIGHTNESS":       configBrightness();      break;
            case "WIFI":             configToggle("WIFI",      "📶 WiFi",        "WiFi");      break;
            case "BLUETOOTH":        configToggle("BLUETOOTH", "🔵 Bluetooth",   "Bluetooth"); break;
            case "DO_NOT_DISTURB":   configToggle("DO_NOT_DISTURB","🚫 Ne pas déranger","DND"); break;
            case "FLASHLIGHT":       configToggle("FLASHLIGHT","🔦 Lampe torche","la lampe torche"); break;
            case "ALARM":            configAlarm();           break;
            case "SPEAK_TEXT":       configSpeakText();       break;
            case "SHOW_NOTIFICATION":configNotification();    break;
            case "LAUNCH_APP":       configLaunchApp();       break;
        }
    }

    private void configVolume() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(10));
        form.setBackgroundColor(BG);

        String[] levels = {"MUTE","LOW","MEDIUM","HIGH"};
        String[] labels = {"🔇 Muet","🔉 Faible","🔊 Moyen","📣 Fort"};
        final int[] sel = {2};

        for (int i = 0; i < levels.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextColor(Color.WHITE);
            b.setTextSize(14f);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(i == sel[0] ? (GREEN & 0x00FFFFFF | 0x33000000) : CARD);
            bg.setCornerRadius(dp(10));
            bg.setStroke(dp(1), i == sel[0] ? GREEN : 0xFF1E3A52);
            b.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            lp.setMargins(0, 0, 0, dp(8));
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                sel[0] = idx;
                for (int j = 0; j < form.getChildCount(); j++) {
                    View child = form.getChildAt(j);
                    GradientDrawable cbd = new GradientDrawable();
                    cbd.setColor(j == idx ? (GREEN & 0x00FFFFFF | 0x33000000) : CARD);
                    cbd.setCornerRadius(dp(10));
                    cbd.setStroke(dp(1), j == idx ? GREEN : 0xFF1E3A52);
                    child.setBackground(cbd);
                }
            });
            form.addView(b);
        }

        final String[] levs = levels;
        new AlertDialog.Builder(this)
            .setTitle("🔊 Volume")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",  "VOLUME");
                    o.put("level", levs[sel[0]]);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configSoundMode() {
        String[] modes  = {"NORMAL","VIBRATE","SILENT"};
        String[] labels = {"🔔 Normal","📳 Vibreur","🔕 Silencieux"};
        final int[] sel = {0};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(10));
        form.setBackgroundColor(BG);

        for (int i = 0; i < modes.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextColor(Color.WHITE);
            b.setTextSize(14f);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(i == 0 ? (GREEN & 0x00FFFFFF | 0x33000000) : CARD);
            bg.setCornerRadius(dp(10));
            bg.setStroke(dp(1), i == 0 ? GREEN : 0xFF1E3A52);
            b.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            lp.setMargins(0, 0, 0, dp(8));
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                sel[0] = idx;
                for (int j = 0; j < form.getChildCount(); j++) {
                    GradientDrawable gd = new GradientDrawable();
                    gd.setColor(j == idx ? (GREEN & 0x00FFFFFF | 0x33000000) : CARD);
                    gd.setCornerRadius(dp(10));
                    gd.setStroke(dp(1), j == idx ? GREEN : 0xFF1E3A52);
                    form.getChildAt(j).setBackground(gd);
                }
            });
            form.addView(b);
        }

        final String[] ms = modes;
        new AlertDialog.Builder(this)
            .setTitle("🔔 Mode son")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "SOUND_MODE");
                    o.put("mode", ms[sel[0]]);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configBrightness() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(10));
        form.setBackgroundColor(BG);

        int[] pct = {70};
        TextView label = new TextView(this);
        label.setText("Luminosité : 70%");
        label.setTextColor(Color.WHITE);
        label.setTextSize(15f);
        label.setTypeface(null, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        form.addView(label);
        form.addView(spacer(12));

        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(70);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                pct[0] = p;
                label.setText("Luminosité : " + p + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        form.addView(sb);

        new AlertDialog.Builder(this)
            .setTitle("☀️ Luminosité")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",  "BRIGHTNESS");
                    o.put("value", pct[0]);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configToggle(String type, String title, String name) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(new String[]{"✅ Activer " + name, "❌ Désactiver " + name}, (d, which) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",    type);
                    o.put("enabled", which == 0);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .show();
    }

    private void configAlarm() {
        int[] h = {7}, m = {0};
        new TimePickerDialog(this, (view, hour, min) -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", "ALARM");
                o.put("hour", hour);
                o.put("minute", min);
                returnResult(o.toString());
            } catch (Exception ignored) {}
        }, 7, 0, true).show();
    }

    private void configSpeakText() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(10));
        form.setBackgroundColor(BG);

        TextView hint = new TextView(this);
        hint.setText("Léa dira ce texte à voix haute.");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(12f);
        form.addView(hint);
        form.addView(spacer(10));
        EditText et = styledEt("Ex: Bonne nuit ! Passe une bonne nuit.", form);

        new AlertDialog.Builder(this)
            .setTitle("💬 Léa dit…")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    String text = et.getText().toString().trim();
                    if (text.isEmpty()) text = "Salut !";
                    JSONObject o = new JSONObject();
                    o.put("type", "SPEAK_TEXT");
                    o.put("text", text);
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configNotification() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(10));
        form.setBackgroundColor(BG);

        EditText etTitle = styledEt("Titre de la notification", form);
        form.addView(spacer(8));
        EditText etText  = styledEt("Texte de la notification", form);

        new AlertDialog.Builder(this)
            .setTitle("🔔 Notification")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",  "SHOW_NOTIFICATION");
                    o.put("title", etTitle.getText().toString().trim());
                    o.put("text",  etText.getText().toString().trim());
                    returnResult(o.toString());
                } catch (Exception ignored) {}
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void configLaunchApp() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(10));
        form.setBackgroundColor(BG);

        TextView hint = new TextView(this);
        hint.setText("Package Android de l'app.\nEx: com.spotify.music · com.google.android.youtube");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(12f);
        form.addView(hint);
        form.addView(spacer(10));
        EditText et = styledEt("Package (ex: com.spotify.music)", form);

        new AlertDialog.Builder(this)
            .setTitle("📱 Ouvrir une app")
            .setView(form)
            .setPositiveButton("Confirmer", (d, w) -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type",    "LAUNCH_APP");
                    o.put("package", et.getText().toString().trim());
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
    public static String actionLabel(JSONObject a) {
        try {
            switch (a.optString("type", "")) {
                case "VOLUME":            return "🔊 Volume " + a.optString("level");
                case "SOUND_MODE":        {
                    String m = a.optString("mode","");
                    return "🔔 " + ("SILENT".equals(m) ? "Silencieux" : "VIBRATE".equals(m) ? "Vibreur" : "Normal");
                }
                case "BRIGHTNESS":        return "☀️ Luminosité " + a.optInt("value") + "%";
                case "WIFI":              return "📶 WiFi " + (a.optBoolean("enabled") ? "ON" : "OFF");
                case "BLUETOOTH":         return "🔵 Bluetooth " + (a.optBoolean("enabled") ? "ON" : "OFF");
                case "DO_NOT_DISTURB":    return "🚫 DND " + (a.optBoolean("enabled") ? "ON" : "OFF");
                case "FLASHLIGHT":        return "🔦 Lampe " + (a.optBoolean("enabled") ? "ON" : "OFF");
                case "ALARM":             return "⏰ Alarme " + String.format("%02d:%02d", a.optInt("hour"), a.optInt("minute"));
                case "SPEAK_TEXT":        return "💬 Léa : \"" + truncate(a.optString("text"), 20) + "\"";
                case "SHOW_NOTIFICATION": return "🔔 Notif : " + truncate(a.optString("title"), 18);
                case "LAUNCH_APP":        return "📱 " + truncate(a.optString("package"), 24);
                default:                  return a.optString("type");
            }
        } catch (Exception e) { return "?"; }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
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
