package com.flolov42.lea_v3.widgets;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class LeaWidgetActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        setContentView(buildUI());
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeWidgetCard("🌱 Widget Habitudes", "Progression daily + streak actif", "widget_habits"));
        root.addView(makeWidgetCard("⭐ Widget XP & Level", "Niveau actuel + XP vers prochain", "widget_xp"));
        root.addView(makeWidgetCard("⚔️ Widget Quêtes", "Quêtes du jour + accès rapide", "widget_quests"));
        root.addView(makeWidgetCard("🪙 Widget Coins", "Balance LÉA Coins + achievements", "widget_coins"));
        root.addView(makeInfoCard());

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(0, 0, 0, dp(24));

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF013560, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView icon = new TextView(this);
        icon.setText("📱");
        icon.setTextSize(40);
        icon.setGravity(Gravity.CENTER);
        h.addView(icon);

        TextView title = new TextView(this);
        title.setText("Widgets Android");
        title.setTextColor(CYAN);
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        h.addView(title);

        TextView sub = new TextView(this);
        sub.setText("4 widgets disponibles pour l'écran d'accueil");
        sub.setTextColor(0xFFB0BEC5);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, 0);
        h.addView(sub);

        return h;
    }

    private LinearLayout makeWidgetCard(String title, String desc, String widgetId) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(lp);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textBlock.setLayoutParams(tbLp);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textBlock.addView(tv);

        TextView dv = new TextView(this);
        dv.setText(desc);
        dv.setTextColor(0xFFB0BEC5);
        dv.setTextSize(13);
        dv.setPadding(0, dp(2), 0, 0);
        textBlock.addView(dv);

        card.addView(textBlock);

        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(this);
        boolean enabled = false;
        for (LeaFeaturesDatabase.WidgetConfig w : db.getWidgets()) {
            if (w.id.equals(widgetId)) { enabled = w.enabled == 1; break; }
        }

        Switch sw = new Switch(this);
        sw.setChecked(enabled);
        sw.setTag(widgetId);
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            String wid = (String) btn.getTag();
            db.setWidgetEnabled(wid, isChecked);
            Toast.makeText(this, isChecked ? "Widget activé" : "Widget désactivé", Toast.LENGTH_SHORT).show();
        });
        card.addView(sw);

        return card;
    }

    private LinearLayout makeInfoCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF013050);
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, 0);
        card.setLayoutParams(lp);

        TextView info = new TextView(this);
        info.setText("ℹ️ Comment ajouter un widget:\n\n1. Appui long sur l'écran d'accueil\n2. Sélectionner « Widgets »\n3. Trouver « Léa » dans la liste\n4. Glisser le widget souhaité\n\nLes widgets se mettent à jour automatiquement toutes les heures.");
        info.setTextColor(0xFFB0BEC5);
        info.setTextSize(13);
        info.setLineSpacing(dp(4), 1f);
        card.addView(info);

        return card;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
