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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/**
 * Interface principale Routines Léa — style Bixby Routines.
 * Onglets : ROUTINES (liste avec chips SI/ALORS) | MODES (grille).
 */
public class LeaRoutineActivity extends Activity {

    private static final int BG     = 0xFF020617;
    private static final int CYAN   = 0xFF00E5FF;
    private static final int VIOLET = 0xFF7C3AED;
    private static final int GREEN  = 0xFF4CAF50;
    private static final int CARD   = 0xFF0B1526;
    private static final int GLASS_BORDER = 0x1EFFFFFF;

    private static final int REQ_EDITOR = 200;

    private LeaRoutineManager mgr;

    private Button     btnTabRoutines;
    private Button     btnTabModes;
    private ScrollView scrollRoutines;
    private ScrollView scrollModes;
    private LinearLayout routineListContainer;

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            mgr = new LeaRoutineManager(this);
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);

            setContentView(buildLayout());
            switchTab(true); // onglet ROUTINES par défaut
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            Toast.makeText(this, "❌ " + e.getMessage() + " @ " + loc, Toast.LENGTH_LONG).show();
            LeaAndroidLogger.crash(this, "LeaRoutineActivity", e);
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRoutineList();
        LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        if (reqCode == REQ_EDITOR) refreshRoutineList();
    }

    // ── Layout principal ──────────────────────────────────────────────────────

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildHeader());
        root.addView(buildTabBar());

        // Séparateur
        View sep = new View(this);
        sep.setBackgroundColor(0xFF0D2137);
        root.addView(sep, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // Conteneur onglets
        FrameLayout content = new FrameLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        scrollRoutines = new ScrollView(this);
        scrollRoutines.setBackgroundColor(BG);
        LinearLayout routinesWrap = new LinearLayout(this);
        routinesWrap.setOrientation(LinearLayout.VERTICAL);
        routinesWrap.setPadding(dp(16), dp(12), dp(16), dp(80));
        routinesWrap.addView(buildRoutinesHeader());
        routineListContainer = new LinearLayout(this);
        routineListContainer.setOrientation(LinearLayout.VERTICAL);
        routinesWrap.addView(routineListContainer);
        refreshRoutineList();
        scrollRoutines.addView(routinesWrap);

        scrollModes = new ScrollView(this);
        scrollModes.setBackgroundColor(BG);
        scrollModes.addView(buildModesContent());

        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        content.addView(scrollRoutines, fp);
        content.addView(scrollModes,    fp);
        root.addView(content);

        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(BG);
        h.setPadding(dp(4), dp(22), dp(16), dp(8));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x14FFFFFF);
        backBg.setShape(GradientDrawable.OVAL);
        backBg.setStroke(dp(1), GLASS_BORDER);
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), backBg, null));
        back.setTextSize(20f);
        back.setPadding(0, 0, 0, dp(2));
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText("Léa Routines");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setShadowLayer(dp(14), 0, 0, 0x8000E5FF);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        h.addView(title, tlp);
        return h;
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private View buildTabBar() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setPadding(dp(16), dp(6), dp(16), dp(14));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(0x14FFFFFF);
        barBg.setCornerRadius(dp(16));
        barBg.setStroke(dp(1), GLASS_BORDER);
        bar.setBackground(barBg);
        bar.setPadding(dp(4), dp(4), dp(4), dp(4));

        btnTabRoutines = makeTabBtn("⚡  ROUTINES");
        btnTabModes    = makeTabBtn("🌙  MODES");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        btnTabRoutines.setOnClickListener(v -> switchTab(true));
        btnTabModes.setOnClickListener(v   -> switchTab(false));

        bar.addView(btnTabRoutines, lp);
        bar.addView(btnTabModes,    lp);
        outer.addView(bar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    private Button makeTabBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11f);
        b.setTypeface(null, Typeface.BOLD);
        b.setLetterSpacing(0.04f);
        b.setAllCaps(false);
        b.setPadding(dp(8), dp(11), dp(8), dp(11));
        return b;
    }

    private void switchTab(boolean routines) {
        styleTab(btnTabRoutines, routines);
        styleTab(btnTabModes,    !routines);
        scrollRoutines.setVisibility(routines ? View.VISIBLE : View.GONE);
        scrollModes.setVisibility(routines    ? View.GONE    : View.VISIBLE);
    }

    private void styleTab(Button b, boolean active) {
        if (active) {
            b.setTextColor(Color.WHITE);
            GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{CYAN, VIOLET});
            gd.setCornerRadius(dp(12));
            b.setBackground(gd);
            b.setElevation(dp(2));
        } else {
            b.setTextColor(0xFF64748B);
            b.setBackground(null);
            b.setElevation(0f);
        }
    }

    // ── ONGLET ROUTINES ───────────────────────────────────────────────────────

    private View buildRoutinesHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        row.setLayoutParams(lp);

        // Bouton "+" créer
        Button add = new Button(this);
        add.setText("＋  Nouvelle routine");
        add.setTextColor(Color.WHITE);
        add.setTextSize(13f);
        add.setTypeface(null, Typeface.BOLD);
        add.setAllCaps(false);
        GradientDrawable addBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, new int[]{CYAN, VIOLET});
        addBg.setCornerRadius(dp(14));
        add.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), addBg, null));
        add.setElevation(dp(3));
        add.setOnClickListener(v -> openEditor(-1));
        row.addView(add, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        return row;
    }

    private void refreshRoutineList() {
        if (routineListContainer == null) return;
        routineListContainer.removeAllViews();

        List<LeaRoutineDatabase.RoutineRow> all = mgr.getAll();
        if (all.isEmpty()) {
            routineListContainer.addView(buildEmptyState());
            return;
        }
        for (LeaRoutineDatabase.RoutineRow r : all) {
            routineListContainer.addView(buildRoutineCard(r));
        }
    }

    private View buildEmptyState() {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(32), dp(60), dp(32), dp(32));

        TextView icon = new TextView(this);
        icon.setText("⚡");
        icon.setTextSize(48f);
        icon.setGravity(Gravity.CENTER);
        icon.setAlpha(0.25f);
        empty.addView(icon);

        TextView msg = new TextView(this);
        msg.setText("Aucune routine.\nAppuie sur « Nouvelle routine » pour commencer.");
        msg.setTextColor(0xFF37474F);
        msg.setTextSize(14f);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, dp(12), 0, 0);
        empty.addView(msg);
        return empty;
    }

    private View buildRoutineCard(LeaRoutineDatabase.RoutineRow r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable cardBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        cardBg.setCornerRadius(dp(20));
        cardBg.setStroke(dp(r.active ? 2 : 1), r.active ? r.iconColor : GLASS_BORDER);
        card.setElevation(r.active ? dp(4) : dp(1));
        card.setBackground(new RippleDrawable(
            ColorStateList.valueOf((r.iconColor & 0x00FFFFFF) | 0x33000000), cardBg, null));

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(clp);

        // ── Ligne 1 : icône + nom + switch ──────────────────────────────────
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        // Icône dans badge dégradé
        View iconV = buildIconBadge(r.icon, r.iconColor, 50, 22f);
        ((LinearLayout.LayoutParams) iconV.getLayoutParams()).setMargins(0, 0, dp(14), 0);
        row1.addView(iconV);

        // Nom + indicateur PRESET
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        row1.addView(nameCol, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (r.active) {
            TextView runningBadge = new TextView(this);
            runningBadge.setText("● EN COURS");
            runningBadge.setTextColor(r.iconColor);
            runningBadge.setTextSize(8f);
            runningBadge.setTypeface(null, Typeface.BOLD);
            runningBadge.setLetterSpacing(0.1f);
            nameCol.addView(runningBadge);
        }

        TextView nameV = new TextView(this);
        nameV.setText(r.name);
        nameV.setTextColor(r.active ? r.iconColor : Color.WHITE);
        nameV.setTextSize(15f);
        nameV.setTypeface(null, Typeface.BOLD);
        nameCol.addView(nameV);

        if (r.preset) {
            TextView presetTag = new TextView(this);
            presetTag.setText("PRESET");
            presetTag.setTextColor(0xFF37474F);
            presetTag.setTextSize(9f);
            presetTag.setLetterSpacing(0.08f);
            nameCol.addView(presetTag);
        }

        // Switch
        Switch sw = new Switch(this);
        styleSwitch(sw, r.iconColor);
        sw.setChecked(r.active);
        sw.setOnCheckedChangeListener((btn, checked) -> {
            mgr.toggle(r.id, checked);
            refreshRoutineList();
        });
        row1.addView(sw);
        card.addView(row1);

        // ── Ligne 2 : chips SI ───────────────────────────────────────────────
        String condLabel = conditionsPreview(r.conditionsJson);
        card.addView(buildChipRow("SI", condLabel.isEmpty() ? "Manuelle" : condLabel, CYAN));

        // ── Ligne 3 : chips ALORS ────────────────────────────────────────────
        String actLabel = actionsPreview(r.actionsJson);
        if (!actLabel.isEmpty()) {
            card.addView(buildChipRow("ALORS", actLabel, GREEN));
        }

        // ── Ligne 4 : boutons Modifier / Supprimer ────────────────────────────
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brLp.setMargins(0, dp(12), 0, 0);
        btnRow.setLayoutParams(brLp);

        Button edit = smallBtn("✏️  Modifier",   0xFF90A4AE);
        edit.setOnClickListener(v -> openEditor(r.id));
        Button del = smallBtn("🗑  Supprimer",  0xFFEF5350);
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Supprimer « " + r.name + " » ?")
            .setPositiveButton("Supprimer", (d, w) -> { mgr.delete(r.id); refreshRoutineList(); })
            .setNegativeButton("Annuler", null)
            .show());

        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        eLp.setMargins(0, 0, dp(8), 0);
        btnRow.addView(edit, eLp);
        btnRow.addView(del, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        card.addView(btnRow);
        return card;
    }

    private View buildChipRow(String prefix, String content, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(lp);

        // Étiquette SI / ALORS
        TextView prefixV = new TextView(this);
        prefixV.setText(prefix);
        prefixV.setTextColor(color);
        prefixV.setTextSize(9f);
        prefixV.setTypeface(null, Typeface.BOLD);
        prefixV.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(dp(36),
            ViewGroup.LayoutParams.WRAP_CONTENT);
        row.addView(prefixV, pLp);

        // Contenu
        TextView chip = new TextView(this);
        chip.setText(content);
        chip.setTextColor(0xFFB0BEC5);
        chip.setTextSize(12f);
        chip.setSingleLine(false);
        chip.setMaxLines(2);
        row.addView(chip, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void openEditor(long routineId) {
        Intent i = new Intent(this, LeaRoutineEditorActivity.class);
        i.putExtra(LeaRoutineEditorActivity.EXTRA_ROUTINE_ID, routineId);
        startActivityForResult(i, REQ_EDITOR);
    }

    // ── ONGLET MODES ─────────────────────────────────────────────────────────

    private static final String[][] MODE_DATA = {
        { "SLEEP",      "😴", "Sommeil",   "Muet · Luminosité 0%" },
        { "CINEMA",     "🎬", "Cinéma",    "Muet · Luminosité 40%" },
        { "DRIVING",    "🚗", "Conduite",  "BT ON · Volume fort" },
        { "EXERCISE",   "🏋️","Exercice",  "Volume fort · Lum. max" },
        { "RELAXATION", "🎵", "Détente",   "Volume moyen · Vibreur" },
        { "WORK",       "💼", "Travail",   "Vibreur · Lum. 80%" },
    };
    private static final int[] MODE_COLORS = {
        0xFF9C27B0, 0xFF673AB7, 0xFFFF9800,
        0xFFF44336, 0xFF2196F3, 0xFF3F51B5,
    };
    private static final String[] MODE_ACTIONS = {
        "[{\"type\":\"SOUND_MODE\",\"mode\":\"SILENT\"},{\"type\":\"BRIGHTNESS\",\"value\":0}]",
        "[{\"type\":\"SOUND_MODE\",\"mode\":\"SILENT\"},{\"type\":\"BRIGHTNESS\",\"value\":40}]",
        "[{\"type\":\"BLUETOOTH\",\"enabled\":true},{\"type\":\"VOLUME\",\"level\":\"HIGH\"}]",
        "[{\"type\":\"VOLUME\",\"level\":\"HIGH\"},{\"type\":\"BRIGHTNESS\",\"value\":100}]",
        "[{\"type\":\"VOLUME\",\"level\":\"MEDIUM\"},{\"type\":\"SOUND_MODE\",\"mode\":\"VIBRATE\"}]",
        "[{\"type\":\"SOUND_MODE\",\"mode\":\"VIBRATE\"},{\"type\":\"BRIGHTNESS\",\"value\":80}]",
    };

    private LinearLayout buildModesContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(80));

        // Note explicative
        TextView note = new TextView(this);
        note.setText("Les modes s'appliquent immédiatement et durent jusqu'à ce que tu les désactives.");
        note.setTextColor(0xFF37474F);
        note.setTextSize(11f);
        note.setPadding(0, 0, 0, dp(16));
        root.addView(note);

        LinearLayout row = null;
        for (int i = 0; i < MODE_DATA.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0, 0, 0, dp(12));
                row.setLayoutParams(rlp);
                root.addView(row);
            }
            if (row == null) continue;
            String[] m    = MODE_DATA[i];
            int      col  = MODE_COLORS[i];
            String   acts = MODE_ACTIONS[i];
            boolean  act  = mgr.isModeActive(m[0]);

            View card = buildModeCard(m[0], m[1], m[2], m[3], col, acts, act);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) clp.setMargins(0, 0, dp(6), 0);
            else             clp.setMargins(dp(6), 0, 0, 0);
            card.setLayoutParams(clp);
            row.addView(card);
        }
        return root;
    }

    private View buildModeCard(String type, String emoji, String name,
                                String desc, int color, String actions, boolean active) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(12), dp(16), dp(12), dp(14));
        refreshModeCardBg(card, color, active);

        // Emoji
        TextView emojiV = new TextView(this);
        emojiV.setText(emoji);
        emojiV.setTextSize(32f);
        emojiV.setGravity(Gravity.CENTER);
        card.addView(emojiV);

        // Nom
        TextView nameV = new TextView(this);
        nameV.setText(name);
        nameV.setTextColor(active ? color : Color.WHITE);
        nameV.setTextSize(13f);
        nameV.setTypeface(null, Typeface.BOLD);
        nameV.setGravity(Gravity.CENTER);
        nameV.setPadding(0, dp(6), 0, 0);
        card.addView(nameV);

        // Desc
        TextView descV = new TextView(this);
        descV.setText(desc);
        descV.setTextColor(0xFF546E7A);
        descV.setTextSize(9.5f);
        descV.setGravity(Gravity.CENTER);
        descV.setPadding(0, dp(2), 0, dp(10));
        card.addView(descV);

        // Switch
        Switch sw = new Switch(this);
        styleSwitch(sw, color);
        sw.setChecked(active);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        swLp.gravity = Gravity.CENTER_HORIZONTAL;
        sw.setOnCheckedChangeListener((btn, checked) -> {
            mgr.setModeActive(type, checked, actions);
            refreshModeCardBg(card, color, checked);
            nameV.setTextColor(checked ? color : Color.WHITE);
            Toast.makeText(this,
                "Mode " + name + (checked ? " activé" : " désactivé"),
                Toast.LENGTH_SHORT).show();
        });
        card.addView(sw, swLp);
        return card;
    }

    private void refreshModeCardBg(LinearLayout card, int color, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(active ? ((color & 0x00FFFFFF) | 0x33000000) : 0x0FFFFFFF);
        gd.setCornerRadius(dp(20));
        gd.setStroke(dp(active ? 2 : 1), active ? color : GLASS_BORDER);
        card.setElevation(active ? dp(4) : dp(1));
        card.setBackground(new RippleDrawable(
            ColorStateList.valueOf((color & 0x00FFFFFF) | 0x55000000), gd, null));
    }

    // ── Previews textuels (chips) ─────────────────────────────────────────────

    private String conditionsPreview(String json) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json != null ? json : "[]");
            if (arr.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(arr.length(), 2);
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append("  •  ");
                sb.append(LeaRoutineConditionPickerActivity.conditionLabel(arr.getJSONObject(i)));
            }
            if (arr.length() > 2) sb.append("  +" + (arr.length() - 2));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private String actionsPreview(String json) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json != null ? json : "[]");
            if (arr.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(arr.length(), 3);
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append("  •  ");
                sb.append(LeaRoutineActionPickerActivity.actionLabel(arr.getJSONObject(i)));
            }
            if (arr.length() > 3) sb.append("  +" + (arr.length() - 3));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private Button smallBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color);
        b.setTextSize(11f);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor((color & 0x00FFFFFF) | 0x1F000000);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), (color & 0x00FFFFFF) | 0x55000000);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf((color & 0x00FFFFFF) | 0x55000000), bg, null));
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    /** Badge circulaire glow pour icônes emoji, cohérent avec le style Léa Agents. */
    private View buildIconBadge(String emoji, int color, int sizeDp, float textSizeSp) {
        TextView iconV = new TextView(this);
        iconV.setText(emoji);
        iconV.setTextSize(textSizeSp);
        iconV.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ (color & 0x00FFFFFF) | 0x40000000, (color & 0x00FFFFFF) | 0x18000000 });
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setStroke(dp(1), (color & 0x00FFFFFF) | 0x66000000);
        iconV.setBackground(iconBg);
        iconV.setElevation(dp(2));
        iconV.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return iconV;
    }

    /** Restyle un Switch Android par défaut aux couleurs Léa. */
    private void styleSwitch(Switch sw, int color) {
        sw.setTrackTintList(new ColorStateList(
            new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
            new int[]{ (color & 0x00FFFFFF) | 0x88000000, 0xFF37474F }));
        sw.setThumbTintList(new ColorStateList(
            new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
            new int[]{ color, 0xFFCFD8DC }));
    }

    /** Éclaircit légèrement une couleur (glassmorphism : dégradé subtil au lieu d'un aplat). */
    private int lighten(int color, float amount) {
        int a = (color >>> 24) & 0xFF, r = (color >>> 16) & 0xFF, g = (color >>> 8) & 0xFF, b = color & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount) + 8);
        g = Math.min(255, (int) (g + (255 - g) * amount) + 8);
        b = Math.min(255, (int) (b + (255 - b) * amount) + 8);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
