package com.flolov42.lea_v3.routines;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.utilities.*;

import android.app.Activity;
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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class LeaRoutineEditorActivity extends Activity {

    public static final String EXTRA_ROUTINE_ID = "routine_id";
    static final int REQ_CONDITION = 101;
    static final int REQ_ACTION    = 102;

    private static final int BG    = 0xFF011627;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF4CAF50;
    private static final int CARD  = 0xFF012040;

    private LeaRoutineDatabase            db;
    private LeaRoutineDatabase.RoutineRow routine;
    private boolean isNew;

    private EditText     etName;
    private LinearLayout iconRow;
    private LinearLayout colorRow;
    private LinearLayout conditionsContainer;
    private LinearLayout actionsContainer;

    private String selectedIcon  = "⚡";
    private int    selectedColor = 0xFF00E5FF;

    private final List<JSONObject> conditions = new ArrayList<>();
    private final List<JSONObject> actions    = new ArrayList<>();

    private static final String[] ICONS = {
        "⚡","🏠","💼","🚗","😴","🏋️","🎮","🎬","🎵","📚",
        "🌙","☀️","🍽️","🏃","🧘","📞","🎧","🔔","💡","🚶"
    };
    private static final int[] COLORS = {
        0xFF00E5FF, 0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0,
        0xFFF44336, 0xFFE91E63, 0xFF00BCD4, 0xFF8BC34A, 0xFFFF5722,
        0xFF607D8B, 0xFF795548
    };

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            db = LeaRoutineDatabase.get(this);
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);

            long id = getIntent().getLongExtra(EXTRA_ROUTINE_ID, -1L);
            isNew   = (id < 0);
            routine = isNew ? null : db.getRoutine(id);

            if (!isNew && routine != null) {
                selectedIcon  = routine.icon  != null ? routine.icon  : "⚡";
                selectedColor = routine.iconColor;
                loadFromJson(conditions, routine.conditionsJson);
                loadFromJson(actions,    routine.actionsJson);
            }

            setContentView(buildLayout());
            LeaFeatureDetailActivity.applyImmersive(this);
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            Toast.makeText(this, "Erreur éditeur : " + e.getMessage() + " @ " + loc, Toast.LENGTH_LONG).show();
            LeaAndroidLogger.crash(this, "LeaRoutineEditorActivity", e);
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) return;
        try {
            if (reqCode == REQ_CONDITION) {
                String json = data.getStringExtra(LeaRoutineConditionPickerActivity.EXTRA_RESULT_JSON);
                if (json != null) { conditions.add(new JSONObject(json)); refreshConditionsUI(); }
            } else if (reqCode == REQ_ACTION) {
                String json = data.getStringExtra(LeaRoutineActionPickerActivity.EXTRA_RESULT_JSON);
                if (json != null) { actions.add(new JSONObject(json)); refreshActionsUI(); }
            }
        } catch (Exception ignored) {}
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(buildHeader());

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(slp);
        scroll.setBackgroundColor(BG);
        scroll.addView(buildContent());
        root.addView(scroll);
        return root;
    }

    private LinearLayout buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(BG);
        h.setPadding(dp(4), dp(22), dp(16), dp(10));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        back.setBackground(null);
        back.setTextSize(22f);
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));

        TextView title = new TextView(this);
        title.setText(isNew ? "✨  Nouvelle routine" : "✏️  Modifier");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        h.addView(title, tlp);
        return h;
    }

    private LinearLayout buildContent() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(32));

        // ── Nom ───────────────────────────────────────────────────────────────
        c.addView(sectionLabel("NOM DE LA ROUTINE"));
        etName = new EditText(this);
        etName.setText(routine != null ? routine.name : "");
        etName.setHint("Ex: Réveil matin, Sommeil…");
        etName.setTextColor(Color.WHITE);
        etName.setHintTextColor(0xFF37474F);
        etName.setTextSize(15f);
        etName.setTypeface(null, Typeface.BOLD);
        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(CARD);
        etBg.setCornerRadius(dp(12));
        etBg.setStroke(dp(1), 0xFF1E3A52);
        etName.setBackground(etBg);
        etName.setPadding(dp(14), dp(14), dp(14), dp(14));
        c.addView(etName, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Icône ─────────────────────────────────────────────────────────────
        c.addView(spacer(14));
        c.addView(sectionLabel("ICÔNE"));
        c.addView(buildIconPicker());

        // ── Couleur ───────────────────────────────────────────────────────────
        c.addView(spacer(10));
        c.addView(sectionLabel("COULEUR"));
        c.addView(buildColorPicker());

        // ── Bloc SI ───────────────────────────────────────────────────────────
        c.addView(spacer(24));
        c.addView(buildBlockHeader("SI — QUAND…", CYAN));
        conditionsContainer = new LinearLayout(this);
        conditionsContainer.setOrientation(LinearLayout.VERTICAL);
        c.addView(conditionsContainer);
        refreshConditionsUI();

        Button addCond = buildAddBtn("＋  Ajouter une condition", CYAN);
        addCond.setOnClickListener(v -> startActivityForResult(
            new Intent(this, LeaRoutineConditionPickerActivity.class), REQ_CONDITION));
        LinearLayout.LayoutParams alcp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        alcp.setMargins(0, dp(6), 0, 0);
        c.addView(addCond, alcp);

        // ── Bloc ALORS ────────────────────────────────────────────────────────
        c.addView(spacer(24));
        c.addView(buildBlockHeader("ALORS — FAIRE…", GREEN));
        actionsContainer = new LinearLayout(this);
        actionsContainer.setOrientation(LinearLayout.VERTICAL);
        c.addView(actionsContainer);
        refreshActionsUI();

        Button addAct = buildAddBtn("＋  Ajouter une action", GREEN);
        addAct.setOnClickListener(v -> startActivityForResult(
            new Intent(this, LeaRoutineActionPickerActivity.class), REQ_ACTION));
        LinearLayout.LayoutParams alap = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        alap.setMargins(0, dp(6), 0, 0);
        c.addView(addAct, alap);

        // ── Sauvegarder ───────────────────────────────────────────────────────
        c.addView(spacer(32));
        Button save = new Button(this);
        save.setText("SAUVEGARDER LA ROUTINE");
        save.setTextColor(BG);
        save.setTextSize(13f);
        save.setLetterSpacing(0.06f);
        save.setTypeface(null, Typeface.BOLD);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(CYAN);
        saveBg.setCornerRadius(dp(14));
        save.setBackground(saveBg);
        save.setOnClickListener(v -> saveAndExit());
        c.addView(save, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        return c;
    }

    // ── Sélecteur d'icône ─────────────────────────────────────────────────────

    private View buildIconPicker() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        iconRow = new LinearLayout(this);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setPadding(0, dp(4), 0, dp(4));
        for (String icon : ICONS) {
            iconRow.addView(makeIconDot(icon));
        }
        hsv.addView(iconRow);
        return hsv;
    }

    private TextView makeIconDot(String icon) {
        TextView tv = new TextView(this);
        tv.setText(icon);
        tv.setTextSize(22f);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(50), dp(50));
        lp.setMargins(0, 0, dp(8), 0);
        tv.setLayoutParams(lp);
        styleIconDot(tv, icon);
        tv.setOnClickListener(v -> {
            selectedIcon = icon;
            refreshIconDots();
        });
        return tv;
    }

    private void styleIconDot(TextView tv, String icon) {
        boolean sel = icon.equals(selectedIcon);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(sel ? applyAlpha(selectedColor, 0x33) : CARD);
        gd.setCornerRadius(dp(12));
        gd.setStroke(dp(sel ? 2 : 1), sel ? selectedColor : 0xFF1E3A52);
        tv.setBackground(gd);
    }

    private void refreshIconDots() {
        if (iconRow == null) return;
        for (int i = 0; i < iconRow.getChildCount() && i < ICONS.length; i++) {
            styleIconDot((TextView) iconRow.getChildAt(i), ICONS[i]);
        }
    }

    // ── Sélecteur de couleur ──────────────────────────────────────────────────

    private View buildColorPicker() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setPadding(0, dp(6), 0, dp(6));
        for (int color : COLORS) {
            colorRow.addView(makeColorDot(color));
        }
        hsv.addView(colorRow);
        return hsv;
    }

    private View makeColorDot(int color) {
        View dot = new View(this);
        styleColorDot(dot, color);
        dot.setOnClickListener(v -> {
            selectedColor = color;
            refreshColorDots();
            refreshIconDots();
        });
        return dot;
    }

    private void styleColorDot(View dot, int color) {
        boolean sel = (color == selectedColor);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        if (sel) gd.setStroke(dp(3), Color.WHITE);
        dot.setBackground(gd);
        int size   = sel ? dp(36) : dp(28);
        int margin = sel ? 0 : dp(4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(0, margin, dp(10), margin);
        dot.setLayoutParams(lp);
    }

    private void refreshColorDots() {
        if (colorRow == null) return;
        for (int i = 0; i < colorRow.getChildCount() && i < COLORS.length; i++) {
            styleColorDot(colorRow.getChildAt(i), COLORS[i]);
        }
    }

    // ── Chips conditions / actions ─────────────────────────────────────────────

    private void refreshConditionsUI() {
        if (conditionsContainer == null) return;
        conditionsContainer.removeAllViews();
        for (int i = 0; i < conditions.size(); i++) {
            final int idx = i;
            String label = LeaRoutineConditionPickerActivity.conditionLabel(conditions.get(i));
            conditionsContainer.addView(buildChip(label, CYAN,
                () -> { conditions.remove(idx); refreshConditionsUI(); }));
        }
        if (conditions.isEmpty()) {
            conditionsContainer.addView(buildHint(
                "Aucune condition — déclenchement manuel"));
        }
    }

    private void refreshActionsUI() {
        if (actionsContainer == null) return;
        actionsContainer.removeAllViews();
        for (int i = 0; i < actions.size(); i++) {
            final int idx = i;
            String label = LeaRoutineActionPickerActivity.actionLabel(actions.get(i));
            actionsContainer.addView(buildChip(label, GREEN,
                () -> { actions.remove(idx); refreshActionsUI(); }));
        }
        if (actions.isEmpty()) {
            actionsContainer.addView(buildHint("Aucune action ajoutée"));
        }
    }

    private View buildChip(String label, int color, Runnable onDelete) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(applyAlpha(color, 0x1A));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), applyAlpha(color, 0x55));
        chip.setBackground(bg);
        chip.setPadding(dp(12), dp(10), dp(8), dp(10));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(color);
        lbl.setTextSize(13f);
        chip.addView(lbl, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button del = new Button(this);
        del.setText("✕");
        del.setTextColor(applyAlpha(color, 0x99));
        del.setBackground(null);
        del.setTextSize(14f);
        del.setPadding(dp(8), 0, dp(4), 0);
        del.setOnClickListener(v -> onDelete.run());
        chip.addView(del, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private View buildHint(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF37474F);
        tv.setTextSize(12f);
        tv.setTypeface(null, Typeface.ITALIC);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), dp(8), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private void saveAndExit() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Donne un nom à ta routine.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (actions.isEmpty()) {
            Toast.makeText(this, "Ajoute au moins une action.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String condsJson = toJsonArray(conditions);
            String actsJson  = toJsonArray(actions);

            if (isNew) {
                LeaRoutineDatabase.RoutineRow r = new LeaRoutineDatabase.RoutineRow();
                r.name           = name;
                r.icon           = selectedIcon;
                r.iconColor      = selectedColor;
                r.conditionsJson = condsJson;
                r.actionsJson    = actsJson;
                r.active         = false;
                r.preset         = false;
                r.userEnabled    = false;
                db.insertRoutine(r);
            } else if (routine != null) {
                routine.name           = name;
                routine.icon           = selectedIcon;
                routine.iconColor      = selectedColor;
                routine.conditionsJson = condsJson;
                routine.actionsJson    = actsJson;
                db.updateRoutine(routine);
            }
            Toast.makeText(this, isNew ? "Routine créée !" : "Routine modifiée !", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur sauvegarde : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Helpers UI ─────────────────────────────────────────────────────────────

    private View buildBlockHeader(String text, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(lp);

        View line = new View(this);
        GradientDrawable lineBg = new GradientDrawable();
        lineBg.setColor(color);
        lineBg.setCornerRadius(dp(2));
        line.setBackground(lineBg);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(3), dp(22));
        lineLp.setMargins(0, 0, dp(10), 0);
        line.setLayoutParams(lineLp);
        row.addView(line);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(13f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.06f);
        row.addView(tv);
        return row;
    }

    private Button buildAddBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color);
        b.setTextSize(13f);
        b.setTypeface(null, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(applyAlpha(color, 0x14));
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), applyAlpha(color, 0x44));
        b.setBackground(bg);
        return b;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF546E7A);
        tv.setTextSize(10f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        tv.setLayoutParams(lp);
        return tv;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
        return v;
    }

    /** Applique un canal alpha à une couleur ARGB. */
    private static int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void loadFromJson(List<JSONObject> list, String json) {
        try {
            JSONArray arr = new JSONArray(json != null ? json : "[]");
            for (int i = 0; i < arr.length(); i++) list.add(arr.getJSONObject(i));
        } catch (Exception ignored) {}
    }

    private String toJsonArray(List<JSONObject> list) {
        JSONArray arr = new JSONArray();
        for (JSONObject o : list) arr.put(o);
        return arr.toString();
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
