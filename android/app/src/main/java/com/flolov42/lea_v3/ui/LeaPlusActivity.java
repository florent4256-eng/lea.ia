package com.flolov42.lea_v3.ui;

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
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.view.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class LeaPlusActivity extends Activity implements LeaPlusManager.StateListener {

    // ── Galaxy palette ────────────────────────────────────────────────────────
    private static final int BG    = 0xFF011627;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int CARD  = 0xFF012040;
    private static final int GOLD  = 0xFFFFD700;

    // ── Manager ───────────────────────────────────────────────────────────────
    private LeaPlusManager manager;

    // ── Tab state ─────────────────────────────────────────────────────────────
    private String currentTab = LeaPlusManager.TAB_GAMIFICATION;

    // ── Tab buttons ───────────────────────────────────────────────────────────
    private final Map<String, Button>       tabButtons  = new HashMap<>();
    // ── Tab content scroll views (one per tab) ────────────────────────────────
    private final Map<String, ScrollView>   tabContent  = new HashMap<>();
    // ── Card reference maps ───────────────────────────────────────────────────
    private final Map<String, LinearLayout> cardMap     = new HashMap<>();
    private final Map<String, TextView>     statusMap   = new HashMap<>();
    private final Map<String, Switch>       switchMap   = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);
        manager = LeaPlusManager.get(this);

        // Root scroll + vertical layout
        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setBackgroundColor(BG);
        rootScroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(0, 0, 0, dp(16));
        rootScroll.addView(root);

        root.addView(buildHeader());
        root.addView(buildTabBar());
        root.addView(buildAllTabContents());

        setContentView(rootScroll);
        showTab(currentTab);
        handleIntent();
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            android.widget.Toast.makeText(this, "❌ LeaPlus: " + e.getMessage() + "\n@ " + loc, android.widget.Toast.LENGTH_LONG).show();
            LeaAndroidLogger.crash(this, "LeaPlus onCreate", e);
            finish();
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private LinearLayout buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(CARD);
        h.setPadding(dp(16), dp(14), dp(16), dp(14));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setTextSize(20);
        back.setPadding(0, 0, dp(8), 0);
        back.setOnClickListener(v -> finish());
        h.addView(back);

        TextView title = new TextView(this);
        title.setText("LÉA PLUS ✨");
        title.setTextColor(CYAN);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(lp);
        h.addView(title);

        TextView sub = new TextView(this);
        sub.setText("15 FEATURES");
        sub.setTextColor(0xFFFFD700);
        sub.setTextSize(10);
        sub.setGravity(Gravity.CENTER);
        h.addView(sub);

        return h;
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────
    private HorizontalScrollView buildTabBar() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setBackgroundColor(0xFF010E1A);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));

        String[][] tabs = {
            {LeaPlusManager.TAB_GAMIFICATION, "🎮 GAMIF"},
            {LeaPlusManager.TAB_LIFESTYLE,    "🌟 STYLE"},
            {LeaPlusManager.TAB_LEARNING,     "📚 LEARN"},
            {LeaPlusManager.TAB_PREMIUM,      "👑 PREMIUM"},
            {LeaPlusManager.TAB_CONNECT,      "🔗 CONNECT"},
        };

        for (String[] tab : tabs) {
            Button btn = new Button(this);
            btn.setText(tab[1]);
            btn.setTextSize(10);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
            lp.setMargins(dp(4), 0, dp(4), 0);
            btn.setLayoutParams(lp);
            final String tabId = tab[0];
            btn.setOnClickListener(v -> showTab(tabId));
            tabButtons.put(tabId, btn);
            bar.addView(btn);
        }
        hsv.addView(bar);
        return hsv;
    }

    private void showTab(String tab) {
        currentTab = tab;
        for (Map.Entry<String, ScrollView> e : tabContent.entrySet())
            e.getValue().setVisibility(tab.equals(e.getKey()) ? View.VISIBLE : View.GONE);
        for (Map.Entry<String, Button> e : tabButtons.entrySet()) {
            boolean active = tab.equals(e.getKey());
            e.getValue().setBackgroundColor(active ? CYAN : 0xFF022040);
            e.getValue().setTextColor(active ? BG : CYAN);
        }
    }

    // ── Tab contents ──────────────────────────────────────────────────────────
    private FrameLayout buildAllTabContents() {
        FrameLayout frame = new FrameLayout(this);
        String[] tabs = {
            LeaPlusManager.TAB_GAMIFICATION,
            LeaPlusManager.TAB_LIFESTYLE,
            LeaPlusManager.TAB_LEARNING,
            LeaPlusManager.TAB_PREMIUM,
            LeaPlusManager.TAB_CONNECT,
        };
        for (String tab : tabs) {
            ScrollView sv = buildTabContent(tab);
            tabContent.put(tab, sv);
            frame.addView(sv);
        }
        return frame;
    }

    private ScrollView buildTabContent(String tab) {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout vl = new LinearLayout(this);
        vl.setOrientation(LinearLayout.VERTICAL);
        vl.setPadding(dp(12), dp(12), dp(12), dp(12));
        vl.setBackgroundColor(BG);

        // Tab title banner
        TextView tabTitle = new TextView(this);
        tabTitle.setText(getTabTitle(tab));
        tabTitle.setTextColor(GOLD);
        tabTitle.setTextSize(13);
        tabTitle.setTypeface(null, Typeface.BOLD);
        tabTitle.setPadding(dp(8), dp(4), dp(8), dp(12));
        vl.addView(tabTitle);

        List<LeaPlusManager.FeatureInfo> features = manager.getFeaturesForTab(tab);
        for (LeaPlusManager.FeatureInfo f : features) vl.addView(buildFeatureCard(f));

        sv.addView(vl);
        return sv;
    }

    private LinearLayout buildFeatureCard(LeaPlusManager.FeatureInfo f) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackgroundColor(CARD);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardLp);

        // Top row: icon + name + switch
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText(f.icon);
        icon.setTextSize(22);
        icon.setPadding(0, 0, dp(10), 0);
        topRow.addView(icon);

        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameBlock.setLayoutParams(nbLp);

        TextView name = new TextView(this);
        name.setText(f.name);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(13);
        name.setTypeface(null, Typeface.BOLD);
        nameBlock.addView(name);

        TextView desc = new TextView(this);
        desc.setText(f.description);
        desc.setTextColor(0xFF7BB8CC);
        desc.setTextSize(10);
        nameBlock.addView(desc);
        topRow.addView(nameBlock);

        Switch sw = new Switch(this);
        boolean enabled = manager.isEnabled(f.id);
        sw.setChecked(enabled);
        sw.setOnCheckedChangeListener((v, checked) -> {
            if (checked) manager.enable(f.id);
            else         manager.disable(f.id);
        });
        topRow.addView(sw);
        card.addView(topRow);

        // Status row
        View divider = new View(this);
        divider.setBackgroundColor(0xFF023050);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, dp(8), 0, dp(8));
        divider.setLayoutParams(divLp);
        card.addView(divider);

        TextView status = new TextView(this);
        status.setText(enabled ? "✅ Actif" : "⏹ Inactif");
        status.setTextColor(enabled ? 0xFF4CAF50 : 0xFF7BB8CC);
        status.setTextSize(11);
        card.addView(status);

        // Last log
        List<LeaPlusDatabase.LogRow> logs = manager.getLogs(f.id);
        if (!logs.isEmpty()) {
            TextView lastLog = new TextView(this);
            lastLog.setText(logs.get(0).msg);
            lastLog.setTextColor(0xFF4A8FA8);
            lastLog.setTextSize(10);
            lastLog.setPadding(0, dp(4), 0, 0);
            card.addView(lastLog);
        }

        // Action button
        Button actionBtn = new Button(this);
        actionBtn.setText("▶ OUVRIR");
        actionBtn.setTextColor(f.color == 0 ? CYAN : f.color);
        actionBtn.setBackgroundColor(0xFF023050);
        actionBtn.setTextSize(10);
        actionBtn.setTypeface(null, Typeface.BOLD);
        actionBtn.setAllCaps(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
        btnLp.setMargins(0, dp(8), 0, 0);
        actionBtn.setLayoutParams(btnLp);
        actionBtn.setOnClickListener(v -> openFeatureDetail(f));
        card.addView(actionBtn);

        // Store references in maps
        cardMap.put(f.id, card);
        statusMap.put(f.id, status);
        switchMap.put(f.id, sw);

        return card;
    }

    private void openFeatureDetail(LeaPlusManager.FeatureInfo f) {
        Class<?> cls = getDetailClass(f.id);
        if (cls == null) {
            Toast.makeText(this, f.icon + " " + f.name + " — bientôt disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("feature_id", f.id);
        startActivity(intent);
    }

    private Class<?> getDetailClass(String featureId) {
        switch (featureId) {
            case LeaPlusDatabase.QUESTS:      return LeaQuestDetailActivity.class;
            case LeaPlusDatabase.ADVENTURE:   return LeaAdventureDetailActivity.class;
            case LeaPlusDatabase.COINS:       return LeaCoinDetailActivity.class;
            case LeaPlusDatabase.HABITS:      return LeaHabitDetailActivity.class;
            case LeaPlusDatabase.REPORT:      return LeaAnnualReportDetailActivity.class;
            case LeaPlusDatabase.COMPANION:   return LeaCompanionDetailActivity.class;
            case LeaPlusDatabase.LIFE_OS:     return LeaLifeOSDetailActivity.class;
            case LeaPlusDatabase.STUDENT:     return LeaStudentDetailActivity.class;
            case LeaPlusDatabase.LANGUAGE:    return LeaLanguageDetailActivity.class;
            case LeaPlusDatabase.SMART_NOTIF: return LeaSmartNotifDetailActivity.class;
            case LeaPlusDatabase.CLOUD_SYNC:  return LeaCloudSyncDetailActivity.class;
            case LeaPlusDatabase.MARKETPLACE: return LeaMarketplaceDetailActivity.class;
            case LeaPlusDatabase.FAMILY:      return LeaFamilyDetailActivity.class;
            case LeaPlusDatabase.OMNICHANNEL: return LeaOmnichannelDetailActivity.class;
            case LeaPlusDatabase.STREAMING:   return LeaStreamingDetailActivity.class;
            default:                          return null;
        }
    }

    // ── Observer pattern ──────────────────────────────────────────────────────
    @Override
    public void onFeatureStateChanged(String featureId, boolean enabled) {
        runOnUiThread(() -> refreshFeatureCard(featureId, enabled));
    }

    private void refreshFeatureCard(String featureId, boolean enabled) {
        Switch sw = switchMap.get(featureId);
        if (sw != null && sw.isChecked() != enabled) sw.setChecked(enabled);
        TextView status = statusMap.get(featureId);
        if (status != null) {
            status.setText(enabled ? "✅ Actif" : "⏹ Inactif");
            status.setTextColor(enabled ? 0xFF4CAF50 : 0xFF7BB8CC);
        }
        LinearLayout card = cardMap.get(featureId);
        if (card != null) card.setAlpha(enabled ? 1.0f : 0.7f);
    }

    private void refreshAllCards() {
        for (LeaPlusManager.FeatureInfo f : LeaPlusManager.ALL_FEATURES)
            refreshFeatureCard(f.id, manager.isEnabled(f.id));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override protected void onResume() {
        super.onResume();
        manager.registerListener(this);
        refreshAllCards();
        showTab(currentTab);
    }
    @Override protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    private String getActiveLanguage() {
        return getSharedPreferences("lea_language", MODE_PRIVATE).getString("active_lang", "EN");
    }

    private String getTabTitle(String tab) {
        switch (tab) {
            case LeaPlusManager.TAB_GAMIFICATION: return "🎮 GAMIFICATION — Quêtes, Aventure, Coins";
            case LeaPlusManager.TAB_LIFESTYLE:    return "🌟 LIFESTYLE — Habitudes, Rapport, Compagnon, LifeOS";
            case LeaPlusManager.TAB_LEARNING:     return "📚 LEARNING — Étudiant, Langues, Notifications";
            case LeaPlusManager.TAB_PREMIUM:      return "👑 PREMIUM — Sync, Marketplace, Famille";
            case LeaPlusManager.TAB_CONNECT:      return "🔗 CONNECT — Omnichannel, Streaming";
            default:                              return tab;
        }
    }

    private void handleIntent() {
        if (getIntent() != null) {
            String featureId = getIntent().getStringExtra("feature_id");
            if (featureId != null) {
                LeaPlusManager.FeatureInfo info = manager.getInfo(featureId);
                if (info != null) showTab(info.tab);
            }
        }
    }
}
