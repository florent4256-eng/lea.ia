package com.flolov42.lea_v3.themes;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

public class LeaThemeActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;

    private LeaThemeManager themeManager;
    private LinearLayout themeGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        themeManager = LeaThemeManager.get(this);
        setContentView(buildUI());
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("Choisir un Thème");
        sectionTitle.setTextColor(CYAN); sectionTitle.setTextSize(15);
        sectionTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, 0, 0, dp(12));
        sectionTitle.setLayoutParams(slp);
        root.addView(sectionTitle);

        themeGrid = new LinearLayout(this);
        themeGrid.setOrientation(LinearLayout.VERTICAL);
        root.addView(themeGrid);

        refreshThemeGrid();

        root.addView(makeEyeCareCard());

        scroll.addView(root);
        return scroll;
    }

    private void refreshThemeGrid() {
        themeGrid.removeAllViews();
        String current = themeManager.getCurrentTheme();
        String[] themes = LeaThemeManager.allThemes();
        for (int i = 0; i < themes.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, dp(6), 0, dp(6));
            row.setLayoutParams(rlp);
            row.addView(makeThemeCard(themes[i], current));
            if (i + 1 < themes.length) row.addView(makeThemeCard(themes[i+1], current));
            themeGrid.addView(row);
        }
    }

    private LinearLayout makeThemeCard(String themeId, String current) {
        LeaThemeManager.ThemeColors colors = themeManager.getColors(themeId);
        boolean isActive = themeId.equals(current);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colors.card);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(isActive ? 3 : 1), isActive ? colors.primary : 0x2200E5FF);
        card.setBackground(bg);
        card.setPadding(dp(12), dp(16), dp(12), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(lp);

        // Color preview circles
        LinearLayout circles = new LinearLayout(this);
        circles.setOrientation(LinearLayout.HORIZONTAL);
        circles.setGravity(Gravity.CENTER);
        circles.setPadding(0, 0, 0, dp(8));
        circles.addView(makeCircle(colors.bg));
        circles.addView(makeCircle(colors.primary));
        circles.addView(makeCircle(colors.card));
        circles.addView(makeCircle(colors.accent));
        card.addView(circles);

        TextView emoji = new TextView(this);
        emoji.setText(colors.emoji); emoji.setTextSize(28); emoji.setGravity(Gravity.CENTER);
        card.addView(emoji);

        TextView name = new TextView(this);
        name.setText(colors.name);
        name.setTextColor(isActive ? colors.primary : Color.WHITE);
        name.setTextSize(13); name.setGravity(Gravity.CENTER);
        if (isActive) name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(name);

        if (isActive) {
            TextView check = new TextView(this);
            check.setText("✓ Actif"); check.setTextColor(colors.primary); check.setTextSize(11);
            check.setGravity(Gravity.CENTER);
            card.addView(check);
        }

        card.setOnClickListener(v -> {
            themeManager.applyTheme(themeId);
            Toast.makeText(this, colors.emoji + " Thème " + colors.name + " appliqué!", Toast.LENGTH_SHORT).show();
            refreshThemeGrid();
        });

        return card;
    }

    private View makeCircle(int color) {
        View v = new View(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL); gd.setColor(color);
        gd.setStroke(dp(1), 0x44FFFFFF);
        v.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
        lp.setMargins(dp(2), 0, dp(2), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout makeEyeCareCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF012040); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(20), 0, 0);
        card.setLayoutParams(lp);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        text.setLayoutParams(tlp);

        TextView t1 = new TextView(this); t1.setText("👁️ Mode Eye Care");
        t1.setTextColor(Color.WHITE); t1.setTextSize(15); t1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.addView(t1);
        TextView t2 = new TextView(this); t2.setText("Filtre lumière bleue — protège vos yeux la nuit");
        t2.setTextColor(0xFFB0BEC5); t2.setTextSize(12);
        text.addView(t2);
        card.addView(text);

        Switch sw = new Switch(this);
        sw.setChecked(themeManager.isEyeCareEnabled());
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            themeManager.setEyeCare(isChecked);
            Toast.makeText(this, isChecked ? "Eye Care activé 🌙" : "Eye Care désactivé", Toast.LENGTH_SHORT).show();
        });
        card.addView(sw);
        return card;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF1A0533, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(20));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("🎨"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Thèmes & Apparence");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("6 thèmes disponibles");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
