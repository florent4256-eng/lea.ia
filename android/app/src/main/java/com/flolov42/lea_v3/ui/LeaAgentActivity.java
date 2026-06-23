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
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Typeface;

import com.google.android.gms.auth.UserRecoverableAuthException;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LeaAgentActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    private static final int REQ_GMAIL_AUTH = 901;

    private LeaAgentManager          manager;
    private LeaModeManager           modeManager;
    private LeaAgentPermissionHelper permHelper;
    private String                   focusedAgentId;
    private String                   pendingGmailAccount = null;

    // Panels
    private ScrollView   panelGrid, panelModes, panelDetail;
    private LinearLayout detailContent;
    private boolean      isModesTab = false;
    private TextView     tabAgentsLabel, tabModesLabel;

    // Card reference maps for real-time UI updates
    private final Map<String, LinearLayout> agentCardMap  = new HashMap<>();
    private final Map<String, TextView>     agentNameMap  = new HashMap<>();
    private final Map<String, TextView>     agentStatusMap= new HashMap<>();
    private final Map<String, Switch>       agentSwitchMap= new HashMap<>();
    private final Map<String, LinearLayout> modeCardMap   = new HashMap<>();
    private final Map<String, TextView>     modeNameMap   = new HashMap<>();
    private final Map<String, TextView>     modeStatusMap = new HashMap<>();
    private final Map<String, Switch>       modeSwitchMap = new HashMap<>();

    // Observers — registered in onResume, removed in onPause
    private final LeaAgentManager.StateListener agentListener = (id, enabled) ->
        runOnUiThread(() -> refreshAgentCard(id, enabled));

    private final LeaModeManager.StateListener modeListener = (id, enabled) ->
        runOnUiThread(() -> refreshModeCard(id, enabled));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);
        manager     = LeaAgentManager.get(this);
        modeManager = LeaModeManager.get(this);
        permHelper  = new LeaAgentPermissionHelper(this);
        manager.ensureServiceRunning();

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        // Check if we should open a specific agent
        focusedAgentId = getIntent().getStringExtra("agent_id");

        setContentView(buildRoot());

        if (focusedAgentId != null) {
            for (LeaAgentManager.AgentInfo ai : LeaAgentManager.ALL_AGENTS) {
                if (ai.id.equals(focusedAgentId)) {
                    openAgentDetail(ai);
                    break;
                }
            }
        }
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            android.widget.Toast.makeText(this, "❌ Agents: " + e.getMessage() + "\n@ " + loc, android.widget.Toast.LENGTH_LONG).show();
            LeaAndroidLogger.crash(this, "Agents onCreate", e);
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
        manager.registerListener(agentListener);
        modeManager.registerListener(modeListener);
        // Sync all card states on return (catches external state changes)
        refreshAllCards();
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(agentListener);
        modeManager.unregisterListener(modeListener);
    }

    // ── Root layout ───────────────────────────────────────────────────────────

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildHeader());
        root.addView(buildTabBar());

        FrameLayout container = new FrameLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        panelGrid   = buildGridPanel();
        panelModes  = buildModesPanel();
        panelDetail = new ScrollView(this);
        panelDetail.setBackgroundColor(BG);
        detailContent = new LinearLayout(this);
        detailContent.setOrientation(LinearLayout.VERTICAL);
        panelDetail.addView(detailContent);

        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(panelGrid,   fp);
        container.addView(panelModes,  fp);
        container.addView(panelDetail, fp);
        panelDetail.setVisibility(View.GONE);
        panelModes.setVisibility(View.GONE);

        root.addView(container);
        return root;
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private View buildTabBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(CARD);

        tabAgentsLabel = makeTabLabel("🤖  AGENTS (11)", true);
        tabModesLabel  = makeTabLabel("✨  MODES (10)",  false);

        tabAgentsLabel.setOnClickListener(v -> switchTab(false));
        tabModesLabel.setOnClickListener(v  -> switchTab(true));

        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(tabAgentsLabel, tlp);
        bar.addView(tabModesLabel,  tlp);
        return bar;
    }

    private TextView makeTabLabel(String text, boolean active) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(13), 0, dp(11));
        tv.setTextSize(11f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.06f);
        applyTabStyle(tv, active);
        return tv;
    }

    private void applyTabStyle(TextView tv, boolean active) {
        tv.setTextColor(active ? CYAN : 0xFF546E7A);
        if (active) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.TRANSPARENT);
            gd.setStroke(0, Color.TRANSPARENT);
            tv.setBackground(gd);
            tv.setPadding(0, dp(13), 0, dp(9));
        } else {
            tv.setBackground(null);
            tv.setPadding(0, dp(13), 0, dp(11));
        }
    }

    private void switchTab(boolean modesTab) {
        isModesTab = modesTab;
        applyTabStyle(tabAgentsLabel, !modesTab);
        applyTabStyle(tabModesLabel,   modesTab);
        panelDetail.setVisibility(View.GONE);
        if (modesTab) {
            panelGrid.setVisibility(View.GONE);
            panelModes.setVisibility(View.VISIBLE);
        } else {
            panelModes.setVisibility(View.GONE);
            panelGrid.setVisibility(View.VISIBLE);
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(12), dp(20), dp(16), dp(12));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        back.setBackground(null);
        back.setTextSize(22f);
        back.setOnClickListener(v -> {
            if (panelDetail.getVisibility() == View.VISIBLE) {
                panelDetail.setVisibility(View.GONE);
                if (isModesTab) panelModes.setVisibility(View.VISIBLE);
                else             panelGrid.setVisibility(View.VISIBLE);
            } else {
                finish();
            }
        });
        h.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText("🤖  LÉA AGENTS");
        title.setTextColor(CYAN);
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.gravity = Gravity.CENTER_VERTICAL;
        h.addView(title, lp);

        // Global ON/OFF badge
        TextView badge = new TextView(this);
        badge.setText("⚡ ACTIFS");
        badge.setTextColor(0xFF4CAF50);
        badge.setTextSize(9f);
        badge.setTypeface(null, Typeface.BOLD);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x224CAF50);
        badgeBg.setCornerRadius(dp(20));
        badgeBg.setStroke(dp(1), 0xFF4CAF50);
        badge.setBackground(badgeBg);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        h.addView(badge);
        return h;
    }

    // ── Grid Panel ────────────────────────────────────────────────────────────

    private ScrollView buildGridPanel() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(80));

        // Subtitle
        TextView sub = new TextView(this);
        sub.setText("11 agents intelligents — cliquez pour configurer");
        sub.setTextColor(0xFF37474F);
        sub.setTextSize(11f);
        sub.setPadding(dp(4), 0, 0, dp(16));
        content.addView(sub);

        // Build grid (2 columns)
        LinearLayout row = null;
        for (int i = 0; i < LeaAgentManager.ALL_AGENTS.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0, 0, 0, dp(12));
                row.setLayoutParams(rlp);
                content.addView(row);
            }

            LeaAgentManager.AgentInfo agent = LeaAgentManager.ALL_AGENTS[i];
            View card = buildAgentCard(agent);

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) clp.setMargins(0, 0, dp(6), 0);
            else             clp.setMargins(dp(6), 0, 0, 0);
            card.setLayoutParams(clp);
            if (row != null) row.addView(card);
        }

        sv.addView(content);
        return sv;
    }

    private View buildAgentCard(LeaAgentManager.AgentInfo agent) {
        boolean enabled = manager.isEnabled(agent.id);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(12), dp(16), dp(12), dp(14));
        refreshCardBg(card, agent.color, enabled);

        // Emoji icon
        TextView iconV = new TextView(this);
        iconV.setText(agent.icon);
        iconV.setTextSize(28f);
        iconV.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(agent.color & 0x00FFFFFF | 0x22000000);
        iconBg.setShape(GradientDrawable.OVAL);
        iconV.setBackground(iconBg);
        iconV.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(56), dp(52));
        card.addView(iconV, iconLp);

        // Name
        TextView nameV = new TextView(this);
        nameV.setText(agent.name);
        nameV.setTextColor(enabled ? agent.color : Color.WHITE);
        nameV.setTextSize(11f);
        nameV.setTypeface(null, Typeface.BOLD);
        nameV.setGravity(Gravity.CENTER);
        nameV.setPadding(0, dp(6), 0, dp(2));
        nameV.setMaxLines(2);
        card.addView(nameV);

        // Status
        TextView statusV = new TextView(this);
        statusV.setText(enabled ? "● ACTIF" : "○ INACTIF");
        statusV.setTextColor(enabled ? 0xFF4CAF50 : 0xFF546E7A);
        statusV.setTextSize(9f);
        statusV.setGravity(Gravity.CENTER);
        statusV.setLetterSpacing(0.1f);
        card.addView(statusV);

        // Switch
        Switch sw = new Switch(this);
        sw.setChecked(enabled);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        swLp.gravity = Gravity.CENTER_HORIZONTAL;
        swLp.setMargins(0, dp(6), 0, 0);
        sw.setOnCheckedChangeListener((btn, checked) ->
            onToggleAgent(agent, checked, sw,
                // onEnabled: met à jour la carte
                () -> {
                    refreshCardBg(card, agent.color, true);
                    nameV.setTextColor(agent.color);
                    statusV.setText("● ACTIF");
                    statusV.setTextColor(0xFF4CAF50);
                },
                // onDisabled
                () -> {
                    refreshCardBg(card, agent.color, false);
                    nameV.setTextColor(Color.WHITE);
                    statusV.setText("○ INACTIF");
                    statusV.setTextColor(0xFF546E7A);
                }
            )
        );
        card.addView(sw, swLp);

        // Store refs for real-time refresh
        agentCardMap.put(agent.id, card);
        agentNameMap.put(agent.id, nameV);
        agentStatusMap.put(agent.id, statusV);
        agentSwitchMap.put(agent.id, sw);

        // Click = detail
        card.setOnClickListener(v -> openAgentDetail(agent));
        return card;
    }

    private void refreshCardBg(LinearLayout card, int color, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(active ? (color & 0x00FFFFFF | 0x1A000000) : CARD);
        gd.setCornerRadius(dp(18));
        gd.setStroke(dp(1), active ? color : 0xFF1E2D3D);
        card.setBackground(gd);
    }

    // ── Modes Panel ───────────────────────────────────────────────────────────

    private ScrollView buildModesPanel() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(80));

        TextView sub = new TextView(this);
        sub.setText("10 modes innovants — cliquez pour explorer");
        sub.setTextColor(0xFF37474F);
        sub.setTextSize(11f);
        sub.setPadding(dp(4), 0, 0, dp(16));
        content.addView(sub);

        LinearLayout row = null;
        for (int i = 0; i < LeaModeManager.ALL_MODES.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0, 0, 0, dp(12));
                row.setLayoutParams(rlp);
                content.addView(row);
            }
            LeaModeManager.ModeInfo mode = LeaModeManager.ALL_MODES[i];
            View card = buildModeCard(mode);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) clp.setMargins(0, 0, dp(6), 0);
            else             clp.setMargins(dp(6), 0, 0, 0);
            card.setLayoutParams(clp);
            if (row != null) row.addView(card);
        }

        sv.addView(content);
        return sv;
    }

    private View buildModeCard(LeaModeManager.ModeInfo mode) {
        LeaModeManager mm = LeaModeManager.get(this);
        boolean enabled   = mm.isEnabled(mode.id);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(12), dp(16), dp(12), dp(14));
        refreshCardBg(card, mode.color, enabled);

        TextView iconV = new TextView(this);
        iconV.setText(mode.icon);
        iconV.setTextSize(28f);
        iconV.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(mode.color & 0x00FFFFFF | 0x22000000);
        iconBg.setShape(GradientDrawable.OVAL);
        iconV.setBackground(iconBg);
        iconV.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.addView(iconV, new LinearLayout.LayoutParams(dp(56), dp(52)));

        TextView nameV = new TextView(this);
        nameV.setText(mode.name);
        nameV.setTextColor(enabled ? mode.color : Color.WHITE);
        nameV.setTextSize(11f);
        nameV.setTypeface(null, Typeface.BOLD);
        nameV.setGravity(Gravity.CENTER);
        nameV.setPadding(0, dp(6), 0, dp(2));
        nameV.setMaxLines(2);
        card.addView(nameV);

        TextView statusV = new TextView(this);
        statusV.setText(enabled ? "● ACTIF" : "○ INACTIF");
        statusV.setTextColor(enabled ? 0xFF4CAF50 : 0xFF546E7A);
        statusV.setTextSize(9f);
        statusV.setGravity(Gravity.CENTER);
        statusV.setLetterSpacing(0.1f);
        card.addView(statusV);

        Switch sw = new Switch(this);
        sw.setChecked(enabled);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        swLp.gravity = Gravity.CENTER_HORIZONTAL;
        swLp.setMargins(0, dp(6), 0, 0);
        sw.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                mm.enable(mode.id);
                refreshCardBg(card, mode.color, true);
                nameV.setTextColor(mode.color);
                statusV.setText("● ACTIF");
                statusV.setTextColor(0xFF4CAF50);
                toast("✨ " + mode.name + " activé");
            } else {
                mm.disable(mode.id);
                refreshCardBg(card, mode.color, false);
                nameV.setTextColor(Color.WHITE);
                statusV.setText("○ INACTIF");
                statusV.setTextColor(0xFF546E7A);
                toast("⏹ " + mode.name + " désactivé");
            }
        });
        card.addView(sw, swLp);

        // Store refs for real-time refresh
        modeCardMap.put(mode.id, card);
        modeNameMap.put(mode.id, nameV);
        modeStatusMap.put(mode.id, statusV);
        modeSwitchMap.put(mode.id, sw);

        card.setOnClickListener(v -> openModeDetail(mode));
        return card;
    }

    private void openModeDetail(LeaModeManager.ModeInfo mode) {
        LeaModeManager mm = LeaModeManager.get(this);
        boolean enabled   = mm.isEnabled(mode.id);

        detailContent.removeAllViews();
        detailContent.setPadding(dp(20), dp(12), dp(20), dp(80));

        // Header
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, dp(8), 0, dp(16));

        TextView iconV = new TextView(this);
        iconV.setText(mode.icon);
        iconV.setTextSize(36f);
        iconV.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(mode.color & 0x00FFFFFF | 0x33000000);
        iconBg.setShape(GradientDrawable.OVAL);
        iconV.setBackground(iconBg);
        iconV.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(68), dp(64));
        iconLp.setMargins(0, 0, dp(16), 0);
        topRow.addView(iconV, iconLp);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        topRow.addView(nameCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameV = new TextView(this);
        nameV.setText(mode.name);
        nameV.setTextColor(mode.color);
        nameV.setTextSize(20f);
        nameV.setTypeface(null, Typeface.BOLD);
        nameCol.addView(nameV);

        TextView statusV = new TextView(this);
        statusV.setText(enabled ? "● MODE ACTIF" : "○ MODE INACTIF");
        statusV.setTextColor(enabled ? 0xFF4CAF50 : 0xFF546E7A);
        statusV.setTextSize(11f);
        statusV.setLetterSpacing(0.08f);
        nameCol.addView(statusV);

        detailContent.addView(topRow);

        // Description
        LinearLayout descCard = makeCard(CARD, true);
        TextView desc = new TextView(this);
        desc.setText(mode.description);
        desc.setTextColor(0xFFCFD8DC);
        desc.setTextSize(13f);
        desc.setLineSpacing(dp(2), 1f);
        descCard.addView(desc);
        detailContent.addView(descCard);
        detailContent.addView(spacer(dp(12)));

        // ON/OFF switch
        LinearLayout switchCard = makeCard(0xFF1E3A5F, true);
        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView switchLabel = new TextView(this);
        switchLabel.setText("Activer ce mode");
        switchLabel.setTextColor(Color.WHITE);
        switchLabel.setTextSize(15f);
        switchLabel.setTypeface(null, Typeface.BOLD);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch bigSwitch = new Switch(this);
        bigSwitch.setChecked(enabled);
        bigSwitch.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) { mm.enable(mode.id);  statusV.setText("● MODE ACTIF");   statusV.setTextColor(0xFF4CAF50); toast("✨ " + mode.name + " activé"); }
            else         { mm.disable(mode.id); statusV.setText("○ MODE INACTIF"); statusV.setTextColor(0xFF546E7A); toast("⏹ " + mode.name + " désactivé"); }
        });
        switchRow.addView(bigSwitch);
        switchCard.addView(switchRow);
        detailContent.addView(switchCard);
        detailContent.addView(spacer(dp(12)));

        // Logs
        TextView logsTitle = new TextView(this);
        logsTitle.setText("📋  LOGS D'ACTIVITÉ");
        logsTitle.setTextColor(0xFF90A4AE);
        logsTitle.setTextSize(11f);
        logsTitle.setTypeface(null, Typeface.BOLD);
        logsTitle.setLetterSpacing(0.12f);
        logsTitle.setPadding(0, dp(8), 0, dp(8));
        detailContent.addView(logsTitle);

        LinearLayout logsCard = makeCard(0xFF011020, false);
        logsCard.setPadding(dp(12), dp(10), dp(12), dp(10));

        List<LeaModeDatabase.LogRow> modeLogs = mm.getLogs(mode.id);
        if (modeLogs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun log — activez le mode pour commencer");
            empty.setTextColor(0xFF37474F);
            empty.setTextSize(12f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(20));
            logsCard.addView(empty);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            for (LeaModeDatabase.LogRow mLog : modeLogs) {
                LinearLayout logRow = new LinearLayout(this);
                logRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams logLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                logLp.setMargins(0, 0, 0, dp(6));
                logRow.setLayoutParams(logLp);

                TextView time = new TextView(this);
                time.setText(sdf.format(new Date(mLog.ts)));
                time.setTextColor(0xFF546E7A);
                time.setTextSize(10f);
                time.setTypeface(Typeface.MONOSPACE);
                time.setPadding(0, 0, dp(10), 0);
                LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT);
                timeLp.gravity = Gravity.TOP;
                logRow.addView(time, timeLp);

                TextView msgV = new TextView(this);
                msgV.setText(mLog.msg);
                msgV.setTextColor(0xFFB0BEC5);
                msgV.setTextSize(11f);
                msgV.setLineSpacing(dp(1), 1f);
                logRow.addView(msgV, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                logsCard.addView(logRow);
            }
        }
        detailContent.addView(logsCard);

        panelGrid.setVisibility(View.GONE);
        panelModes.setVisibility(View.GONE);
        panelDetail.setVisibility(View.VISIBLE);
        panelDetail.scrollTo(0, 0);
    }

    // ── Detail Panel ──────────────────────────────────────────────────────────

    private void openAgentDetail(LeaAgentManager.AgentInfo agent) {
        detailContent.removeAllViews();
        detailContent.setPadding(dp(20), dp(12), dp(20), dp(80));

        boolean enabled = manager.isEnabled(agent.id);

        // Icon + Name header
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, dp(8), 0, dp(16));

        TextView iconV = new TextView(this);
        iconV.setText(agent.icon);
        iconV.setTextSize(36f);
        iconV.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(agent.color & 0x00FFFFFF | 0x33000000);
        iconBg.setShape(GradientDrawable.OVAL);
        iconV.setBackground(iconBg);
        iconV.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(68), dp(64));
        iconLp.setMargins(0, 0, dp(16), 0);
        topRow.addView(iconV, iconLp);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        topRow.addView(nameCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameV = new TextView(this);
        nameV.setText(agent.name);
        nameV.setTextColor(agent.color);
        nameV.setTextSize(20f);
        nameV.setTypeface(null, Typeface.BOLD);
        nameCol.addView(nameV);

        TextView statusV = new TextView(this);
        statusV.setText(enabled ? "● AGENT ACTIF" : "○ AGENT INACTIF");
        statusV.setTextColor(enabled ? 0xFF4CAF50 : 0xFF546E7A);
        statusV.setTextSize(11f);
        statusV.setLetterSpacing(0.08f);
        nameCol.addView(statusV);

        detailContent.addView(topRow);

        // Description card
        LinearLayout descCard = makeCard(agent.color, enabled);
        TextView desc = new TextView(this);
        desc.setText(agent.description);
        desc.setTextColor(0xFFCFD8DC);
        desc.setTextSize(13f);
        desc.setLineSpacing(dp(2), 1f);
        descCard.addView(desc);
        detailContent.addView(descCard);
        detailContent.addView(spacer(dp(12)));

        // ON/OFF switch card
        LinearLayout switchCard = makeCard(0xFF1E3A5F, true);
        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView switchLabel = new TextView(this);
        switchLabel.setText("Activer l'agent");
        switchLabel.setTextColor(Color.WHITE);
        switchLabel.setTextSize(15f);
        switchLabel.setTypeface(null, Typeface.BOLD);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch bigSwitch = new Switch(this);
        bigSwitch.setChecked(enabled);
        bigSwitch.setOnCheckedChangeListener((btn, checked) ->
            onToggleAgent(agent, checked, bigSwitch,
                // onEnabled
                () -> {
                    statusV.setText("● AGENT ACTIF");
                    statusV.setTextColor(0xFF4CAF50);
                },
                // onDisabled
                () -> {
                    statusV.setText("○ AGENT INACTIF");
                    statusV.setTextColor(0xFF546E7A);
                }
            )
        );
        switchRow.addView(bigSwitch);
        switchCard.addView(switchRow);
        detailContent.addView(switchCard);
        detailContent.addView(spacer(dp(12)));

        // Special actions per agent
        addSpecialActions(agent);

        // Logs section
        TextView logsTitle = new TextView(this);
        logsTitle.setText("📋  LOGS D'ACTIVITÉ");
        logsTitle.setTextColor(0xFF90A4AE);
        logsTitle.setTextSize(11f);
        logsTitle.setTypeface(null, Typeface.BOLD);
        logsTitle.setLetterSpacing(0.12f);
        logsTitle.setPadding(0, dp(8), 0, dp(8));
        detailContent.addView(logsTitle);

        LinearLayout logsCard = makeCard(0xFF011020, false);
        logsCard.setPadding(dp(12), dp(10), dp(12), dp(10));

        List<LeaAgentDatabase.LogRow> logs = manager.getLogs(agent.id);
        if (logs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun log — activez l'agent pour commencer");
            empty.setTextColor(0xFF37474F);
            empty.setTextSize(12f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(20));
            logsCard.addView(empty);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            for (LeaAgentDatabase.LogRow log : logs) {
                LinearLayout logRow = new LinearLayout(this);
                logRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams logLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                logLp.setMargins(0, 0, 0, dp(6));
                logRow.setLayoutParams(logLp);

                TextView time = new TextView(this);
                time.setText(sdf.format(new Date(log.timestamp)));
                time.setTextColor(0xFF546E7A);
                time.setTextSize(10f);
                time.setTypeface(Typeface.MONOSPACE);
                time.setPadding(0, 0, dp(10), 0);
                LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT);
                timeLp.gravity = Gravity.TOP;
                logRow.addView(time, timeLp);

                TextView msg = new TextView(this);
                msg.setText(log.message);
                msg.setTextColor(0xFFB0BEC5);
                msg.setTextSize(11f);
                msg.setLineSpacing(dp(1), 1f);
                logRow.addView(msg, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                logsCard.addView(logRow);
            }
        }
        detailContent.addView(logsCard);

        // Show panel
        panelGrid.setVisibility(View.GONE);
        panelDetail.setVisibility(View.VISIBLE);
        panelDetail.scrollTo(0, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_GMAIL_AUTH && resultCode == RESULT_OK
                && pendingGmailAccount != null) {
            // L'utilisateur a accordé l'accès Gmail — on re-tente la connexion
            String account = pendingGmailAccount;
            pendingGmailAccount = null;
            LeaEmailAgent ea = LeaAgentService.instance != null
                ? LeaAgentService.instance.getEmailAgent()
                : new LeaEmailAgent(this);
            connectGmailAccount(account, ea);
        }
    }

    private void addSpecialActions(LeaAgentManager.AgentInfo agent) {
        switch (agent.id) {
            case LeaAgentActivationManager.EMAIL:
                addEmailActions();
                break;
            case LeaAgentActivationManager.NOTIFICATION:
                addNotificationActions();
                break;
            case LeaAgentActivationManager.SOCIAL:
                addSocialActions();
                break;
            case LeaAgentActivationManager.CODE:
                addCodeAgentActions();
                break;
            case LeaAgentActivationManager.PRODUCTIVITY:
                addProductivityActions();
                break;
            case LeaAgentActivationManager.SMART_HOME:
                addSmartHomeActions();
                break;
            case LeaAgentActivationManager.LEARNING:
                addLearningActions();
                break;
            case LeaAgentActivationManager.HEALTH:
                addHealthActions();
                break;
            case LeaAgentActivationManager.FINANCE:
                addFinanceActions();
                break;
            case LeaAgentActivationManager.SECURITY:
                addSecurityActions();
                break;
            case LeaAgentActivationManager.CALENDAR:
                addCalendarActions();
                break;
        }
    }

    // ── Agent Notifications ───────────────────────────────────────────────────

    private void addNotificationActions() {
        boolean listenerOk = LeaNotificationAgent.isListenerEnabled(this);
        boolean serviceOk  = com.flolov42.lea_v3.bixby.LeaNotificationService.isAvailable();

        LinearLayout card = makeCard(0xFF1A001A, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("🔔 Accès aux notifications", 0xFFE91E63));
        card.addView(spacer(dp(10)));

        if (!listenerOk) {
            // ── Permission non accordée ─────────────────────────────────────
            TextView statusV = new TextView(this);
            statusV.setText("❌ Accès aux notifications non autorisé");
            statusV.setTextColor(0xFFF44336);
            statusV.setTextSize(13f);
            statusV.setTypeface(null, Typeface.BOLD);
            card.addView(statusV);
            card.addView(spacer(dp(8)));

            TextView explainV = new TextView(this);
            explainV.setText(
                "Pour que l'agent groupe et résume tes notifications, "
                + "Léa doit être autorisée dans les paramètres Android.\n\n"
                + "1. Appuie sur le bouton ci-dessous\n"
                + "2. Trouve \"Léa\" dans la liste\n"
                + "3. Active le switch\n"
                + "4. Reviens ici — l'agent sera opérationnel");
            explainV.setTextColor(0xFF90A4AE);
            explainV.setTextSize(12f);
            explainV.setLineSpacing(dp(3), 1f);
            card.addView(explainV);
            card.addView(spacer(dp(12)));

            Button btnSettings = actionBtn(
                "⚙️  Ouvrir l'accès aux notifications", 0xFFE91E63, v -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception e) {
                        toast("Ouvre Paramètres → Applications → Accès spéciaux → Notifications");
                    }
                });
            card.addView(btnSettings);

        } else {
            // ── Permission accordée ─────────────────────────────────────────
            TextView statusV = new TextView(this);
            statusV.setText("✅ Accès aux notifications accordé");
            statusV.setTextColor(0xFF4CAF50);
            statusV.setTextSize(13f);
            statusV.setTypeface(null, Typeface.BOLD);
            card.addView(statusV);
            card.addView(spacer(dp(6)));

            // Service actif ?
            TextView svcV = new TextView(this);
            svcV.setText(serviceOk
                ? "🟢 Service d'écoute actif"
                : "🟡 Service en attente (active l'agent)");
            svcV.setTextColor(serviceOk ? 0xFF4CAF50 : 0xFFFF9800);
            svcV.setTextSize(12f);
            card.addView(svcV);
            card.addView(spacer(dp(10)));

            // Notifications accumulées
            int accumulated = LeaNotificationAgent.getAccumulatedCount();
            TextView accumV = new TextView(this);
            accumV.setText("📊 " + accumulated + " notification(s) en attente de rapport");
            accumV.setTextColor(0xFF78909C);
            accumV.setTextSize(12f);
            card.addView(accumV);
            card.addView(spacer(dp(4)));

            TextView modeV = new TextView(this);
            modeV.setText("ℹ️  Rapport groupé automatique toutes les 2h.\n"
                + "Spam (réseaux sociaux publicitaires) filtré en silence.\n"
                + "Notification uniquement si ≥3 messages importants.");
            modeV.setTextColor(0xFF546E7A);
            modeV.setTextSize(11f);
            modeV.setLineSpacing(dp(2), 1f);
            card.addView(modeV);
            card.addView(spacer(dp(12)));

            // Forcer un rapport maintenant
            LeaNotificationAgent notifAgentRef = LeaAgentService.instance != null
                ? LeaAgentService.instance.getNotifAgent()
                : new LeaNotificationAgent(this);

            Button btnNow = actionBtn("📋 Générer le rapport maintenant", 0xFFE91E63, v -> {
                new Thread(notifAgentRef::execute).start();
                toast("🔔 Rapport en cours de génération…");
            });
            card.addView(btnNow);
        }

        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    // ── Agent Email ───────────────────────────────────────────────────────────

    private void addEmailActions() {
        LeaEmailAgent ea = LeaAgentService.instance != null
            ? LeaAgentService.instance.getEmailAgent()
            : new LeaEmailAgent(this);

        // Premier accès non configuré → popup d'explication
        if (!ea.isConfigured() && ea.isFirstSetup()) {
            ea.markSetupShown();
            showEmailSetupGuide(ea);
        }

        LinearLayout card = makeCard(0xFF001A2E, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("📧 Configuration Gmail", 0xFF2196F3));
        card.addView(spacer(dp(10)));

        if (ea.isConfigured()) {
            String account = ea.getConfiguredAccount();

            TextView accountView = new TextView(this);
            accountView.setText("✅ Connecté : " + account);
            accountView.setTextColor(0xFF4CAF50);
            accountView.setTextSize(13f);
            accountView.setTypeface(null, Typeface.BOLD);
            card.addView(accountView);
            card.addView(spacer(dp(12)));

            Button btnRefresh = actionBtn("🔄 Actualiser maintenant", 0xFF2196F3, v -> {
                toast("🔄 Récupération des emails…");
                new Thread(() -> ea.execute()).start();
            });
            Button btnView = actionBtn("📋 Voir les emails non lus", 0xFF03A9F4, v ->
                showUnreadEmailsDialog(ea));
            Button btnDisconnect = actionBtn("🔌 Déconnecter", 0xFF607D8B, v -> {
                ea.disconnect();
                toast("Gmail déconnecté");
                reopenEmailDetail();
            });

            card.addView(btnRefresh);
            card.addView(spacer(dp(8)));
            card.addView(btnView);
            card.addView(spacer(dp(8)));
            card.addView(btnDisconnect);

        } else {
            List<String> accounts = ea.getGoogleAccounts();

            if (accounts.isEmpty()) {
                TextView msg = new TextView(this);
                msg.setText("Aucun compte Google trouvé sur cet appareil.\n"
                    + "Ajoute un compte Google dans les paramètres Android.");
                msg.setTextColor(0xFFFF9800);
                msg.setTextSize(12f);
                msg.setLineSpacing(dp(2), 1f);
                card.addView(msg);
            } else {
                TextView hint = new TextView(this);
                hint.setText("Choisis un compte Google pour connecter Gmail :");
                hint.setTextColor(0xFF78909C);
                hint.setTextSize(12f);
                card.addView(hint);
                card.addView(spacer(dp(8)));

                for (String accountEmail : accounts) {
                    Button btn = actionBtn("📧  " + accountEmail, 0xFF2196F3,
                        v -> connectGmailAccount(accountEmail, ea));
                    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    blp.setMargins(0, 0, 0, dp(6));
                    btn.setLayoutParams(blp);
                    card.addView(btn);
                }

                card.addView(spacer(dp(10)));
                TextView note = new TextView(this);
                note.setText("ℹ️ Gmail API doit être activée dans la Google Cloud Console. "
                    + "Une fenêtre d'autorisation s'ouvrira au premier connect.");
                note.setTextColor(0xFF546E7A);
                note.setTextSize(10f);
                note.setLineSpacing(dp(2), 1f);
                card.addView(note);
            }
        }

        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void showEmailSetupGuide(LeaEmailAgent ea) {
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(14), dp(20), dp(14));
        content.setBackgroundColor(0xFF011627);
        scroll.addView(content);

        // Intro
        addGuideText(content, 0xFF2196F3, 15f, true,
            "L'agent Email lit tes emails non lus, les classe (urgent, important, newsletter…) "
            + "et te notifie si quelque chose nécessite ton attention.");
        content.addView(guideSpace());

        // Étape 1
        addGuideStep(content, "ÉTAPE 1", "Activer Gmail API", 0xFF00BCD4,
            "1.  Va sur console.cloud.google.com\n"
            + "2.  Ouvre ton projet (ou crée-en un)\n"
            + "3.  Menu → \"API et services\" → \"Bibliothèque\"\n"
            + "4.  Recherche \"Gmail API\"\n"
            + "5.  Clique sur Gmail API → \"Activer\"");

        // Bouton Google Cloud Console
        Button btnConsole = new Button(this);
        btnConsole.setText("🌐  Ouvrir Google Cloud Console");
        btnConsole.setTextColor(0xFF00BCD4);
        btnConsole.setTextSize(13f);
        GradientDrawable consoleBg = new GradientDrawable();
        consoleBg.setColor(0xFF012040);
        consoleBg.setCornerRadius(dp(10));
        consoleBg.setStroke(dp(1), 0xFF00BCD4);
        btnConsole.setBackground(consoleBg);
        LinearLayout.LayoutParams consoleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        consoleLp.setMargins(0, dp(8), 0, dp(16));
        btnConsole.setLayoutParams(consoleLp);
        btnConsole.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://console.cloud.google.com/apis/library/gmail.googleapis.com"));
            startActivity(i);
        });
        content.addView(btnConsole);

        // Étape 2
        addGuideStep(content, "ÉTAPE 2", "Connecter ton compte", 0xFF4CAF50,
            "1.  Ferme ce guide\n"
            + "2.  Sélectionne ton compte Google dans la liste\n"
            + "3.  Une page de consentement Google s'ouvre\n"
            + "4.  Accepte l'accès → Gmail est connecté ✅");
        content.addView(guideSpace());

        // Note sécurité
        addGuideText(content, 0xFF546E7A, 11f, false,
            "🔒  Léa n'accède qu'en lecture seule (scope gmail.readonly). "
            + "Aucun email n'est envoyé, modifié ou supprimé. "
            + "Le token OAuth expire automatiquement et se renouvelle en arrière-plan.");
        content.addView(guideSpace());

        // Note configuration unique
        addGuideText(content, 0xFF78909C, 11f, false,
            "ℹ️  Cette configuration est à faire une seule fois. "
            + "Ce guide ne s'affichera plus après.");

        new AlertDialog.Builder(this)
            .setTitle("📧 Configurer l'agent Email")
            .setView(scroll)
            .setPositiveButton("J'ai compris →", (d, w) -> { /* le card est déjà affiché */ })
            .setNeutralButton("Plus tard", null)
            .show();
    }

    private void addGuideStep(LinearLayout parent, String tag, String title, int color, String body) {
        // Tag coloré
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(rowLp);

        TextView tagV = new TextView(this);
        tagV.setText(tag);
        tagV.setTextColor(color);
        tagV.setTextSize(9f);
        tagV.setTypeface(null, Typeface.BOLD);
        tagV.setLetterSpacing(0.12f);
        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setColor((color & 0x00FFFFFF) | 0x22000000);
        tagBg.setCornerRadius(dp(6));
        tagV.setBackground(tagBg);
        tagV.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.setMargins(0, 0, dp(10), 0);
        row.addView(tagV, tagLp);

        TextView titleV = new TextView(this);
        titleV.setText(title);
        titleV.setTextColor(Color.WHITE);
        titleV.setTextSize(14f);
        titleV.setTypeface(null, Typeface.BOLD);
        row.addView(titleV, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);

        // Corps
        LinearLayout bodyCard = new LinearLayout(this);
        bodyCard.setOrientation(LinearLayout.VERTICAL);
        bodyCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bodyBg = new GradientDrawable();
        bodyBg.setColor(0xFF012040);
        bodyBg.setCornerRadius(dp(10));
        bodyBg.setStroke(dp(1), (color & 0x00FFFFFF) | 0x44000000);
        bodyCard.setBackground(bodyBg);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.setMargins(0, dp(4), 0, dp(12));
        bodyCard.setLayoutParams(bodyLp);

        TextView bodyV = new TextView(this);
        bodyV.setText(body);
        bodyV.setTextColor(0xFFB0BEC5);
        bodyV.setTextSize(12f);
        bodyV.setLineSpacing(dp(3), 1f);
        bodyCard.addView(bodyV);
        parent.addView(bodyCard);
    }

    private void addGuideText(LinearLayout parent, int color, float size,
                               boolean bold, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(size);
        tv.setLineSpacing(dp(2), 1f);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        parent.addView(tv, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View guideSpace() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        return v;
    }

    private void connectGmailAccount(String accountEmail, LeaEmailAgent ea) {
        toast("🔄 Connexion à " + accountEmail + "…");
        new Thread(() -> {
            try {
                String token = ea.getTokenForAuth(accountEmail);
                // Token OK sans consentement nécessaire
                ea.setAccount(accountEmail);
                runOnUiThread(() -> {
                    toast("✅ Gmail connecté !");
                    reopenEmailDetail();
                });
            } catch (UserRecoverableAuthException e) {
                // L'utilisateur doit accorder l'accès via la page de consentement Google
                pendingGmailAccount = accountEmail;
                runOnUiThread(() -> startActivityForResult(e.getIntent(), REQ_GMAIL_AUTH));
            } catch (Exception e) {
                runOnUiThread(() -> toast("❌ " + e.getMessage()));
            }
        }).start();
    }

    private void reopenEmailDetail() {
        for (LeaAgentManager.AgentInfo ai : LeaAgentManager.ALL_AGENTS) {
            if (ai.id.equals(LeaAgentActivationManager.EMAIL)) {
                openAgentDetail(ai);
                break;
            }
        }
    }

    private void showUnreadEmailsDialog(LeaEmailAgent ea) {
        toast("🔄 Récupération…");
        new Thread(() -> {
            try {
                String token = ea.getTokenSilent(ea.getConfiguredAccount());
                if (token == null) {
                    runOnUiThread(() -> toast("❌ Token invalide — reconnecte ton compte"));
                    return;
                }
                List<LeaEmailAgent.EmailInfo> emails = ea.fetchUnreadEmails(token, 15);
                runOnUiThread(() -> displayEmailList(emails));
            } catch (Exception e) {
                runOnUiThread(() -> toast("❌ Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private void displayEmailList(List<LeaEmailAgent.EmailInfo> emails) {
        if (emails.isEmpty()) {
            toast("✅ Aucun email non lu !");
            return;
        }

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(10), dp(16), dp(10));
        list.setBackgroundColor(0xFF011627);
        scroll.addView(list);

        for (LeaEmailAgent.EmailInfo e : emails) {
            String cat = LeaEmailAgent.classify(e);
            int catColor;
            switch (cat) {
                case "URGENT":     catColor = 0xFFF44336; break;
                case "IMPORTANT":  catColor = 0xFFFF9800; break;
                case "NEWSLETTER": catColor = 0xFF9E9E9E; break;
                case "SOCIAL":     catColor = 0xFF2196F3; break;
                default:           catColor = 0xFF546E7A; break;
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(0xFF012040);
            rowBg.setCornerRadius(dp(10));
            rowBg.setStroke(dp(1), catColor & 0x33FFFFFF | (catColor & 0xFF000000));
            row.setBackground(rowBg);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rowLp);

            // Catégorie + Expéditeur
            LinearLayout topLine = new LinearLayout(this);
            topLine.setOrientation(LinearLayout.HORIZONTAL);
            topLine.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView catTag = new TextView(this);
            catTag.setText(cat);
            catTag.setTextColor(catColor);
            catTag.setTextSize(9f);
            catTag.setTypeface(null, Typeface.BOLD);
            catTag.setLetterSpacing(0.1f);
            catTag.setPadding(dp(6), dp(2), dp(6), dp(2));
            GradientDrawable tagBg = new GradientDrawable();
            tagBg.setColor((catColor & 0x00FFFFFF) | 0x22000000);
            tagBg.setCornerRadius(dp(6));
            catTag.setBackground(tagBg);
            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tagLp.setMargins(0, 0, dp(8), 0);
            topLine.addView(catTag, tagLp);

            TextView from = new TextView(this);
            String fromText = e.from != null ? e.from : "Expéditeur inconnu";
            if (fromText.length() > 35) fromText = fromText.substring(0, 35) + "…";
            from.setText(fromText);
            from.setTextColor(0xFF90A4AE);
            from.setTextSize(11f);
            topLine.addView(from, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(topLine);

            // Sujet
            TextView subj = new TextView(this);
            subj.setText(e.subject != null ? e.subject : "(sans sujet)");
            subj.setTextColor(Color.WHITE);
            subj.setTextSize(13f);
            subj.setTypeface(null, Typeface.BOLD);
            subj.setPadding(0, dp(4), 0, dp(2));
            row.addView(subj);

            // Snippet
            if (e.snippet != null && !e.snippet.isEmpty()) {
                TextView snip = new TextView(this);
                String s = e.snippet.length() > 120 ? e.snippet.substring(0, 120) + "…" : e.snippet;
                snip.setText(s);
                snip.setTextColor(0xFF546E7A);
                snip.setTextSize(11f);
                row.addView(snip);
            }

            // Suggestion de réponse
            String reply = LeaEmailAgent.suggestReply(e);
            if (reply != null) {
                Button replyBtn = new Button(this);
                replyBtn.setText("💬 Réponse suggérée");
                replyBtn.setTextColor(catColor);
                replyBtn.setTextSize(11f);
                replyBtn.setBackground(null);
                final String replyFinal = reply;
                replyBtn.setOnClickListener(v -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("reply", replyFinal));
                        toast("📋 Réponse copiée !");
                    }
                });
                row.addView(replyBtn);
            }

            list.addView(row);
        }

        new AlertDialog.Builder(this)
            .setTitle("📧 Emails non lus (" + emails.size() + ")")
            .setView(scroll)
            .setPositiveButton("Fermer", null)
            .show();
    }

    // ── Agent Social ──────────────────────────────────────────────────────────

    private void addSocialActions() {
        LeaSocialAgent sa0 = new LeaSocialAgent(this);
        int seuil = sa0.getForgottenThresholdDays();

        LinearLayout card = makeCard(0xFF001530, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("👥 Social", 0xFF00BCD4));
        card.addView(spacer(dp(10)));

        Button btnBirthdays = actionBtn("🎂 Anniversaires (7 prochains jours)",        0xFFE91E63,
            v -> showBirthdaysDialog());
        Button btnForgotten = actionBtn("👥 Contacts oubliés (+" + seuil + " jours)",  0xFF00BCD4,
            v -> showForgottenContactsDialog());
        Button btnSeuil     = actionBtn("⚙️ Modifier le seuil (actuellement " + seuil + "j)", 0xFF607D8B,
            v -> showForgottenThresholdDialog());
        Button btnScan      = actionBtn("🔄 Scanner maintenant",                         0xFF4CAF50, v -> {
            new Thread(new LeaSocialAgent(this)::execute).start();
            toast("👥 Scan social en cours…");
        });

        card.addView(btnBirthdays); card.addView(spacer(dp(8)));
        card.addView(btnForgotten); card.addView(spacer(dp(8)));
        card.addView(btnSeuil);     card.addView(spacer(dp(8)));
        card.addView(btnScan);
        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void showForgottenThresholdDialog() {
        LeaSocialAgent sa = new LeaSocialAgent(this);
        int current = sa.getForgottenThresholdDays();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        EditText field = makeEditText(form, "Nombre de jours", String.valueOf(current));
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
            .setTitle("⚙️ Seuil contacts oubliés")
            .setMessage("Un contact non appelé depuis X jours déclenche un rappel.")
            .setView(form)
            .setPositiveButton("Enregistrer", (d, w) -> {
                try {
                    int days = Integer.parseInt(field.getText().toString().trim());
                    if (days < 1) days = 1;
                    sa.setForgottenThreshold(days);
                    toast("✅ Seuil mis à jour : " + days + " jours");
                } catch (NumberFormatException e) {
                    toast("Valeur invalide");
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showBirthdaysDialog() {
        toast("📅 Récupération…");
        new Thread(() -> {
            LeaSocialAgent sa = new LeaSocialAgent(this);
            List<LeaSocialAgent.BirthdayInfo> list = sa.getUpcomingBirthdays();
            runOnUiThread(() -> {
                if (list.isEmpty()) {
                    toast("🎂 Aucun anniversaire dans les 7 prochains jours");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (LeaSocialAgent.BirthdayInfo b : list) {
                    sb.append(b.emoji).append("  ").append(b.name)
                      .append("  —  ").append(b.label).append("\n");
                }
                new AlertDialog.Builder(this)
                    .setTitle("🎂 Anniversaires à venir")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
            });
        }).start();
    }

    private void showForgottenContactsDialog() {
        toast("👥 Analyse…");
        new Thread(() -> {
            LeaSocialAgent sa = new LeaSocialAgent(this);
            List<String> list = sa.getOldContacts();
            runOnUiThread(() -> {
                if (list.isEmpty()) {
                    toast("✅ Tous tes contacts ont été contactés récemment !");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (String s : list) sb.append("•  ").append(s).append("\n");
                new AlertDialog.Builder(this)
                    .setTitle("👥 Contacts oubliés")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
            });
        }).start();
    }

    private void addCodeAgentActions() {
        LinearLayout actionsCard = makeCard(0xFF1B0033, true);
        actionsCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = sectionTitle("💻 Actions Code Agent", 0xFF673AB7);
        actionsCard.addView(title);
        actionsCard.addView(spacer(dp(10)));

        Button btnNewProject = actionBtn("🚀 Nouveau Projet", 0xFF673AB7, v -> showNewProjectDialog());
        Button btnMyProjects = actionBtn("📂 Mes Projets", 0xFF9C27B0, v -> showProjectList());
        Button btnApiKey    = actionBtn("🔑 Configurer Clé API", 0xFF00E5FF, v -> showApiKeyDialog());

        actionsCard.addView(btnNewProject);
        actionsCard.addView(spacer(dp(8)));
        actionsCard.addView(btnMyProjects);
        actionsCard.addView(spacer(dp(8)));
        actionsCard.addView(btnApiKey);

        detailContent.addView(actionsCard);
        detailContent.addView(spacer(dp(12)));
    }

    private void addProductivityActions() {
        // ── Section USAGE_STATS ─────────────────────────────────────────────────
        boolean usageOk = LeaProductivityAgent.isUsageStatsGranted(this);
        LinearLayout usageCard = makeCard(0xFF1A1700, true);
        usageCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        usageCard.addView(sectionTitle("📊 Détection procrastination", 0xFFFFEB3B));
        usageCard.addView(spacer(dp(8)));

        if (usageOk) {
            TextView usageStatusV = new TextView(this);
            usageStatusV.setText("✅ Accès aux stats d'usage accordé — TikTok, YouTube, Instagram surveillés");
            usageStatusV.setTextColor(0xFF4CAF50);
            usageStatusV.setTextSize(12f);
            usageStatusV.setLineSpacing(dp(2), 1f);
            usageCard.addView(usageStatusV);
        } else {
            TextView usageStatusV = new TextView(this);
            usageStatusV.setText("❌ Accès aux stats d'usage non accordé\nLéa ne peut pas détecter TikTok, YouTube…");
            usageStatusV.setTextColor(0xFFF44336);
            usageStatusV.setTextSize(12f);
            usageStatusV.setLineSpacing(dp(2), 1f);
            usageCard.addView(usageStatusV);
            usageCard.addView(spacer(dp(6)));

            TextView usageHint = new TextView(this);
            usageHint.setText("1. Appuie sur le bouton ci-dessous\n2. Trouve \"Léa\" dans la liste\n3. Active l'accès");
            usageHint.setTextColor(0xFF90A4AE);
            usageHint.setTextSize(11f);
            usageHint.setLineSpacing(dp(2), 1f);
            usageCard.addView(usageHint);
            usageCard.addView(spacer(dp(8)));

            Button btnUsage = actionBtn("⚙️ Ouvrir l'accès aux statistiques", 0xFFFFEB3B, v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (Exception ex) {
                    toast("Paramètres → Apps → Accès spéciaux → Usage des données");
                }
            });
            usageCard.addView(btnUsage);
        }
        detailContent.addView(usageCard);
        detailContent.addView(spacer(dp(12)));

        // ── Section FOCUS ───────────────────────────────────────────────────────
        LinearLayout focusCard = makeCard(0xFF1A1700, true);
        focusCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        focusCard.addView(sectionTitle("⚡ Mode Focus", 0xFFFFEB3B));
        focusCard.addView(spacer(dp(10)));

        Button btn25 = actionBtn("🍅 Pomodoro 25min",   0xFFFF5722, v -> startFocus(25));
        Button btn50 = actionBtn("⏱️ Focus 50min",      0xFFFF9800, v -> startFocus(50));
        Button btn90 = actionBtn("🔥 Deep Work 90min",  0xFFF44336, v -> startFocus(90));

        focusCard.addView(btn25); focusCard.addView(spacer(dp(8)));
        focusCard.addView(btn50); focusCard.addView(spacer(dp(8)));
        focusCard.addView(btn90);
        detailContent.addView(focusCard);
        detailContent.addView(spacer(dp(12)));

        // ── Section TÂCHES ──────────────────────────────────────────────────────
        LinearLayout taskCard = makeCard(0xFF1A1700, true);
        taskCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        taskCard.addView(sectionTitle("✅ Tâches", 0xFF4CAF50));
        taskCard.addView(spacer(dp(10)));

        LeaProductivityAgent pa2 = new LeaProductivityAgent(this);
        int doneToday = pa2.getTasksCompletedToday();
        TextView doneV = new TextView(this);
        doneV.setText("🏆 Tâches complétées aujourd'hui : " + doneToday);
        doneV.setTextColor(doneToday > 0 ? 0xFF4CAF50 : 0xFF78909C);
        doneV.setTextSize(13f);
        doneV.setTypeface(null, Typeface.BOLD);
        taskCard.addView(doneV);
        taskCard.addView(spacer(dp(10)));

        Button btnTask = actionBtn("✅ Marquer une tâche comme complète", 0xFF4CAF50,
            v -> showCompleteTaskDialog());
        taskCard.addView(btnTask);
        detailContent.addView(taskCard);
        detailContent.addView(spacer(dp(12)));
    }

    private void showCompleteTaskDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        EditText field = makeEditText(form, "Nom de la tâche", "Ex : rapport hebdomadaire");
        new AlertDialog.Builder(this)
            .setTitle("✅ Tâche complétée")
            .setView(form)
            .setPositiveButton("Valider", (d, w) -> {
                String name = field.getText().toString().trim();
                if (name.isEmpty()) name = "Tâche";
                new LeaProductivityAgent(this).celebrateTaskCompletion(name);
                toast("🎉 Super ! Tâche « " + name + " » enregistrée !");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void addSmartHomeActions() {
        LeaSmartHomeAgent sha = new LeaSmartHomeAgent(this);
        boolean atHome = sha.getStatus().startsWith("🏠");
        String ssid = sha.getCurrentSsid();

        LinearLayout card = makeCard(0xFF0A1520, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("🏠 Smart Home", 0xFF607D8B));
        card.addView(spacer(dp(8)));

        // Statut présence
        TextView statusV = new TextView(this);
        statusV.setText(sha.getStatus());
        statusV.setTextColor(atHome ? 0xFF4CAF50 : 0xFF607D8B);
        statusV.setTextSize(14f);
        statusV.setTypeface(null, Typeface.BOLD);
        card.addView(statusV);

        // WiFi actuel
        if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
            TextView wifiV = new TextView(this);
            wifiV.setText("📶 Connecté : " + ssid);
            wifiV.setTextColor(0xFF78909C);
            wifiV.setTextSize(12f);
            card.addView(wifiV);
        }
        card.addView(spacer(dp(10)));

        LeaSmartHomeAgent shaConf = new LeaSmartHomeAgent(this);
        String stStatus = shaConf.hasSmartThingsToken()
            ? "✅ Token SmartThings configuré" : "⚠️ SmartThings non configuré";
        TextView stV = new TextView(this);
        stV.setText(stStatus);
        stV.setTextColor(shaConf.hasSmartThingsToken() ? 0xFF4CAF50 : 0xFFFF9800);
        stV.setTextSize(12f);
        card.addView(stV);
        card.addView(spacer(dp(10)));

        Button btnWifi      = actionBtn("📶 Configurer WiFi Maison",           0xFF607D8B, v -> showWifiDialog());
        Button btnSmartToken = actionBtn("🔑 Configurer token SmartThings",    0xFF1565C0, v -> showSmartThingsTokenDialog());
        Button btnSmartScene = actionBtn("🎬 Configurer les scènes",            0xFF0D47A1, v -> showSmartThingsScenesDialog());
        Button btnTrigger   = actionBtn("🔄 Analyser l'environnement",          0xFF455A64, v -> {
            new Thread(() -> {
                new LeaSmartHomeAgent(this).execute();
                runOnUiThread(() -> toast("🏠 Analyse Smart Home lancée"));
            }).start();
        });
        card.addView(btnWifi);        card.addView(spacer(dp(8)));
        card.addView(btnSmartToken);  card.addView(spacer(dp(8)));
        card.addView(btnSmartScene);  card.addView(spacer(dp(8)));
        card.addView(btnTrigger);
        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void addLearningActions() {
        LeaLearningAgent la = new LeaLearningAgent(this);
        String[] topic = la.getCurrentTopic();

        LinearLayout card = makeCard(0xFF0A1500, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("📚 Apprentissage", 0xFF8BC34A));
        card.addView(spacer(dp(8)));

        // Progression
        TextView progressV = new TextView(this);
        progressV.setText("📈 " + la.getProgressSummary());
        progressV.setTextColor(0xFF8BC34A);
        progressV.setTextSize(13f);
        progressV.setTypeface(null, Typeface.BOLD);
        card.addView(progressV);
        card.addView(spacer(dp(6)));

        // Ressource du jour
        if (topic != null) {
            TextView topicV = new TextView(this);
            topicV.setText("💡 Aujourd'hui : " + topic[1]);
            topicV.setTextColor(0xFFA5D6A7);
            topicV.setTextSize(12f);
            topicV.setLineSpacing(dp(1), 1f);
            card.addView(topicV);
        }
        card.addView(spacer(dp(10)));

        Button btnOpen = actionBtn("🔗 Ouvrir la ressource du jour", 0xFF8BC34A, v -> {
            if (topic != null && topic.length > 2) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(topic[2])));
                } catch (Exception e) {
                    toast("Impossible d'ouvrir le navigateur");
                }
            }
        });
        Button btnInterests = actionBtn("✏️ Modifier les intérêts", 0xFF558B2F, v -> showInterestsDialog());
        Button btnNow = actionBtn("📚 Déclencher une session maintenant", 0xFF4CAF50, v -> {
            new Thread(() -> {
                new LeaLearningAgent(this).execute();
                runOnUiThread(() -> toast("📚 Session d'apprentissage proposée !"));
            }).start();
        });
        card.addView(btnOpen);      card.addView(spacer(dp(8)));
        card.addView(btnInterests); card.addView(spacer(dp(8)));
        card.addView(btnNow);
        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void addHealthActions() {
        LeaHealthAgent ha = new LeaHealthAgent(this);
        int drinksToday = ha.getDrinksToday();
        final int DRINK_GOAL = 8;

        LinearLayout card = makeCard(0xFF1A0000, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("💪 Suivi Santé", 0xFFF44336));
        card.addView(spacer(dp(8)));

        // Résumé compact
        TextView summaryV = new TextView(this);
        summaryV.setText("📊 " + ha.getHealthSummary());
        summaryV.setTextColor(0xFFFF5722);
        summaryV.setTextSize(12f);
        summaryV.setLineSpacing(dp(1), 1f);
        card.addView(summaryV);
        card.addView(spacer(dp(6)));

        // Barre hydratation textuelle
        StringBuilder barSb = new StringBuilder("💧 ");
        for (int i = 0; i < DRINK_GOAL; i++) barSb.append(i < drinksToday ? "█" : "░");
        barSb.append("  ").append(drinksToday).append("/").append(DRINK_GOAL);
        if (drinksToday >= DRINK_GOAL) barSb.append(" ✅");
        TextView barV = new TextView(this);
        barV.setText(barSb.toString());
        barV.setTextColor(drinksToday >= DRINK_GOAL ? 0xFF4CAF50 : 0xFF2196F3);
        barV.setTextSize(13f);
        barV.setTypeface(android.graphics.Typeface.MONOSPACE);
        card.addView(barV);
        card.addView(spacer(dp(12)));

        Button btnSleep = actionBtn("😴 Enregistrer le sommeil",        0xFFF44336, v -> showSleepDialog());
        Button btnSteps = actionBtn("🚶 Enregistrer mes pas",            0xFFFF5722, v -> showStepsDialog());
        Button btnDrink = actionBtn("💧 J'ai bu un verre d'eau (+1)",    0xFF2196F3, v -> {
            recordDrink();
            toast("💧 Verre enregistré !");
        });
        Button btnMove  = actionBtn("🏃 Je me suis levé / Activité",     0xFF4CAF50, v -> {
            new LeaHealthAgent(this).recordMovement();
            toast("🏃 Activité enregistrée — minuteur remis à zéro");
        });
        Button btnWeek  = actionBtn("📊 Bilan de la semaine",            0xFF9C27B0, v -> showHealthWeeklyDialog());

        card.addView(btnSleep); card.addView(spacer(dp(8)));
        card.addView(btnSteps); card.addView(spacer(dp(8)));
        card.addView(btnDrink); card.addView(spacer(dp(8)));
        card.addView(btnMove);  card.addView(spacer(dp(8)));
        card.addView(btnWeek);
        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void showHealthWeeklyDialog() {
        new Thread(() -> {
            LeaHealthAgent ha = new LeaHealthAgent(this);
            String stats = ha.getWeeklyStats();
            runOnUiThread(() ->
                new AlertDialog.Builder(this)
                    .setTitle("📊 Bilan Santé — 7 jours")
                    .setMessage(stats)
                    .setPositiveButton("OK", null)
                    .show());
        }).start();
    }

    private void addFinanceActions() {
        LeaFinanceAgent fa = LeaAgentService.instance != null
            ? LeaAgentService.instance.getFinanceAgent()
            : new LeaFinanceAgent(this);
        double weekly = 0;
        for (double v : fa.getWeeklySpentByCategory().values()) weekly += v;

        LinearLayout card = makeCard(0xFF1A1000, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("💰 Finance", 0xFFFF9800));
        card.addView(spacer(dp(8)));

        TextView weekV = new TextView(this);
        weekV.setText(String.format("💳 Cette semaine : %.2f€", weekly));
        weekV.setTextColor(weekly > 0 ? 0xFFFF9800 : 0xFF78909C);
        weekV.setTextSize(13f);
        weekV.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(weekV);
        card.addView(spacer(dp(10)));

        Button btnScan    = actionBtn("📊 Analyser les SMS maintenant",   0xFFFF9800, v -> scanFinanceNow());
        Button btnHistory = actionBtn("📋 Voir les transactions",          0xFFFFB74D, v -> showTransactionsDialog());
        Button btnAdd     = actionBtn("✍️ Ajouter une dépense",            0xFF4CAF50, v -> showAddTransactionDialog());
        Button btnBudget  = actionBtn("💼 Budgets de la semaine",          0xFFFF9800, v -> showBudgetDialog());
        Button btnMonth   = actionBtn("📅 Bilan du mois (30 jours)",       0xFF26C6DA, v -> showMonthlyFinanceDialog());
        Button btnEdit    = actionBtn("⚙️ Modifier les budgets",           0xFF607D8B, v -> showEditBudgetsDialog());

        card.addView(btnScan);    card.addView(spacer(dp(8)));
        card.addView(btnHistory); card.addView(spacer(dp(8)));
        card.addView(btnAdd);     card.addView(spacer(dp(8)));
        card.addView(btnBudget);  card.addView(spacer(dp(8)));
        card.addView(btnMonth);   card.addView(spacer(dp(8)));
        card.addView(btnEdit);
        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void addCalendarActions() {
        LinearLayout card = makeCard(0xFF001020, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("📅 Calendrier", 0xFF2196F3));
        card.addView(spacer(dp(8)));

        // Prochain événement
        boolean hasPermission = checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;

        if (hasPermission) {
            LeaCalendarAgent ca = new LeaCalendarAgent(this);
            String nextEvent = ca.getNextEvent();
            TextView nextV = new TextView(this);
            nextV.setText("⏭ " + nextEvent);
            nextV.setTextColor(0xFF90CAF9);
            nextV.setTextSize(12f);
            nextV.setLineSpacing(dp(1), 1f);
            card.addView(nextV);
            card.addView(spacer(dp(10)));

            Button btnScan   = actionBtn("🔍 Analyser maintenant",         0xFF2196F3, v -> {
                new Thread(() -> {
                    new LeaCalendarAgent(this).execute();
                    runOnUiThread(() -> toast("📅 Calendrier analysé !"));
                }).start();
            });
            Button btnEvents = actionBtn("📋 Voir les événements (24h)",   0xFF1976D2, v -> showCalendarEventsDialog());
            card.addView(btnScan);   card.addView(spacer(dp(8)));
            card.addView(btnEvents);
        } else {
            TextView permV = new TextView(this);
            permV.setText("❌ Permission calendrier non accordée\n"
                + "Paramètres → Applications → Léa → Autorisations → Calendrier");
            permV.setTextColor(0xFFF44336);
            permV.setTextSize(12f);
            permV.setLineSpacing(dp(2), 1f);
            card.addView(permV);
            card.addView(spacer(dp(8)));

            Button btnPerm = actionBtn("⚙️ Demander la permission",        0xFF2196F3, v ->
                requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR}, 203));
            card.addView(btnPerm);
        }

        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    private void showCalendarEventsDialog() {
        new Thread(() -> {
            LeaCalendarAgent ca = new LeaCalendarAgent(this);
            java.util.List<String> events = ca.getUpcomingEventsSummary(24);
            StringBuilder sb = new StringBuilder();
            if (events.isEmpty()) {
                sb.append("Aucun événement dans les 24 prochaines heures.");
            } else {
                for (String e : events) sb.append(e).append("\n\n");
            }
            String msg = sb.toString().trim();
            runOnUiThread(() ->
                new AlertDialog.Builder(this)
                    .setTitle("📅 Événements — 24h")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show());
        }).start();
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void showNewProjectDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);

        EditText nameField = makeEditText(form, "Nom du projet", "MonApp");
        EditText descField = makeEditText(form, "Description", "Que doit faire cette app?");

        new AlertDialog.Builder(this)
            .setTitle("🚀 Nouveau Projet")
            .setView(form)
            .setPositiveButton("Créer", (d, w) -> {
                String name = nameField.getText().toString().trim();
                String desc = descField.getText().toString().trim();
                if (name.isEmpty()) { toast("Nom requis"); return; }
                createNewProject(name, desc);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void createNewProject(String name, String description) {
        LeaCodeAgent codeAgent = LeaAgentService.instance != null ?
            LeaAgentService.instance.getCodeAgent() : new LeaCodeAgent(this);

        codeAgent.startProject(name, description, (text, streaming) -> {
            runOnUiThread(() -> {
                toast("✅ Projet «" + name + "» créé!");
                // Open editor
                Intent intent = new Intent(this, LeaCodeEditorActivity.class);
                intent.putExtra(LeaCodeEditorActivity.EXTRA_PROJECT_NAME, name);
                intent.putExtra(LeaCodeEditorActivity.EXTRA_PROJECT_ID, codeAgent.getCurrentProjectId());
                startActivity(intent);
            });
        });
    }

    private void showProjectList() {
        LeaCodeAgent codeAgent = LeaAgentService.instance != null ?
            LeaAgentService.instance.getCodeAgent() : new LeaCodeAgent(this);

        List<LeaAgentDatabase.ProjectRow> projects = codeAgent.getProjects();
        if (projects.isEmpty()) {
            toast("Aucun projet — créez-en un!");
            return;
        }

        String[] names = new String[projects.size()];
        for (int i = 0; i < projects.size(); i++) {
            names[i] = "💻 " + projects.get(i).name + " [" + projects.get(i).status + "]";
        }

        new AlertDialog.Builder(this)
            .setTitle("📂 Mes Projets")
            .setItems(names, (d, which) -> {
                LeaAgentDatabase.ProjectRow p = projects.get(which);
                Intent intent = new Intent(this, LeaCodeEditorActivity.class);
                intent.putExtra(LeaCodeEditorActivity.EXTRA_PROJECT_ID, p.id);
                intent.putExtra(LeaCodeEditorActivity.EXTRA_PROJECT_NAME, p.name);
                startActivity(intent);
            })
            .setNegativeButton("Fermer", null)
            .show();
    }

    private void showApiKeyDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);

        TextView hint = new TextView(this);
        hint.setText("Entrez votre clé API Claude (Anthropic).\nObtenir: console.anthropic.com");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(11f);
        form.addView(hint);

        EditText keyField = makeEditText(form, "sk-ant-...", "");
        keyField.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD | android.text.InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
            .setTitle("🔑 Clé API Claude")
            .setView(form)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                String key = keyField.getText().toString().trim();
                if (key.isEmpty()) return;
                LeaCodeAgent codeAgent = LeaAgentService.instance != null ?
                    LeaAgentService.instance.getCodeAgent() : new LeaCodeAgent(this);
                codeAgent.setApiKey(key);
                toast("🔑 Clé API sauvegardée!");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void startFocus(int minutes) {
        LeaProductivityAgent pa = LeaAgentService.instance != null
            ? new LeaProductivityAgent(this) : new LeaProductivityAgent(this);
        pa.startFocusMode(minutes);
        toast("🎯 Mode Focus " + minutes + "min activé!");
    }

    private void showWifiDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        EditText ssidField = makeEditText(form, "Nom WiFi maison (SSID)", "");
        new AlertDialog.Builder(this)
            .setTitle("📶 WiFi Maison")
            .setView(form)
            .setPositiveButton("Configurer", (d, w) -> {
                String ssid = ssidField.getText().toString().trim();
                if (ssid.isEmpty()) return;
                new LeaSmartHomeAgent(this).setHomeWifi(ssid);
                toast("✅ WiFi maison configuré: " + ssid);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showSmartThingsTokenDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        TextView hint = new TextView(this);
        hint.setText("Personal Access Token Samsung SmartThings.\n"
            + "Obtenir : app.smartthings.com → Account → Personal access tokens");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(11f);
        hint.setLineSpacing(dp(2), 1f);
        form.addView(hint);
        form.addView(spacer(dp(8)));
        EditText field = makeEditText(form, "Bearer token SmartThings", "");
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
            .setTitle("🔑 Token SmartThings")
            .setView(form)
            .setPositiveButton("Enregistrer", (d, w) -> {
                String token = field.getText().toString().trim();
                new LeaSmartHomeAgent(this).setSmartThingsToken(token);
                toast(token.isEmpty() ? "🔑 Token supprimé" : "✅ Token SmartThings enregistré");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showSmartThingsScenesDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        TextView hint = new TextView(this);
        hint.setText("IDs des scènes SmartThings (trouvez-les via l'API ou l'app).\n"
            + "Arrive Home : scène exécutée à l'arrivée.\n"
            + "Leave Home : scène exécutée au départ.");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(11f);
        hint.setLineSpacing(dp(2), 1f);
        form.addView(hint);
        form.addView(spacer(dp(8)));
        EditText arriveField = makeEditText(form, "Scene ID 'Arrive Home'", "");
        EditText leaveField  = makeEditText(form, "Scene ID 'Leave Home'",  "");
        new AlertDialog.Builder(this)
            .setTitle("🎬 Scènes SmartThings")
            .setView(form)
            .setPositiveButton("Enregistrer", (d, w) -> {
                LeaSmartHomeAgent sha = new LeaSmartHomeAgent(this);
                String arrive = arriveField.getText().toString().trim();
                String leave  = leaveField.getText().toString().trim();
                if (!arrive.isEmpty()) sha.setSmartThingsSceneId("arrive_home", arrive);
                if (!leave.isEmpty())  sha.setSmartThingsSceneId("leave_home",  leave);
                toast("✅ Scènes SmartThings enregistrées");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showInterestsDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        TextView hint = new TextView(this);
        hint.setText("Séparez par virgule.\nSujets: programmation, ia, finance, langue, sante, musique, productivite");
        hint.setTextColor(0xFF546E7A);
        hint.setTextSize(11f);
        form.addView(hint);
        EditText field = makeEditText(form, "programmation,ia,finance", "");
        new AlertDialog.Builder(this)
            .setTitle("📚 Mes Intérêts")
            .setView(form)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                String v = field.getText().toString().trim();
                if (!v.isEmpty()) { new LeaLearningAgent(this).setInterests(v); toast("✅ Intérêts mis à jour!"); }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showSleepDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        EditText field = makeEditText(form, "Heures de sommeil (ex: 7)", "7");
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
            .setTitle("😴 Sommeil")
            .setView(form)
            .setPositiveButton("Enregistrer", (d, w) -> {
                try {
                    int h = Integer.parseInt(field.getText().toString().trim());
                    new LeaHealthAgent(this).recordSleep(h);
                    toast("✅ " + h + "h de sommeil enregistrées");
                } catch (NumberFormatException e) { toast("Valeur invalide"); }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showStepsDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);
        EditText field = makeEditText(form, "Nombre de pas", "5000");
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
            .setTitle("🚶 Pas")
            .setView(form)
            .setPositiveButton("Enregistrer", (d, w) -> {
                try {
                    int steps = Integer.parseInt(field.getText().toString().trim());
                    new LeaHealthAgent(this).recordSteps(steps);
                    toast("✅ " + steps + " pas enregistrés");
                } catch (NumberFormatException e) { toast("Valeur invalide"); }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void recordDrink() {
        new LeaHealthAgent(this).recordDrink();
    }

    private void scanFinanceNow() {
        new Thread(() -> {
            new LeaFinanceAgent(this).execute();
            runOnUiThread(() -> toast("📊 Analyse Finance terminée — vérifiez les logs"));
        }).start();
    }

    private void showMonthlyFinanceDialog() {
        new Thread(() -> {
            LeaFinanceAgent fa = LeaAgentService.instance != null
                ? LeaAgentService.instance.getFinanceAgent()
                : new LeaFinanceAgent(this);
            String summary = fa.getMonthlySummary();
            runOnUiThread(() ->
                new AlertDialog.Builder(this)
                    .setTitle("📅 Bilan Finance — 30 jours")
                    .setMessage(summary)
                    .setPositiveButton("OK", null)
                    .show());
        }).start();
    }

    private void showBudgetDialog() {
        toast("💼 Chargement…");
        new Thread(() -> {
            LeaFinanceAgent fa = LeaAgentService.instance != null
                ? LeaAgentService.instance.getFinanceAgent()
                : new LeaFinanceAgent(this);
            Map<String, Double> spent   = fa.getWeeklySpentByCategory();
            Map<String, Double> budgets = fa.getBudgets();
            runOnUiThread(() -> {
                android.widget.ScrollView scroll = new android.widget.ScrollView(this);
                LinearLayout list = new LinearLayout(this);
                list.setOrientation(LinearLayout.VERTICAL);
                list.setPadding(dp(16), dp(10), dp(16), dp(10));
                list.setBackgroundColor(0xFF011627);
                scroll.addView(list);

                double totalSpent = 0;
                for (Map.Entry<String, Double> e : budgets.entrySet()) {
                    String cat    = e.getKey();
                    double budget = e.getValue();
                    double s      = spent.containsKey(cat) ? spent.get(cat) : 0;
                    totalSpent   += s;
                    double pct    = budget > 0 ? s / budget : 0;
                    int color     = pct >= 1.0 ? 0xFFF44336 : pct >= 0.75 ? 0xFFFF9800 : 0xFF4CAF50;

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(dp(12), dp(10), dp(12), dp(10));
                    GradientDrawable rowBg = new GradientDrawable();
                    rowBg.setColor(0xFF012040);
                    rowBg.setCornerRadius(dp(10));
                    rowBg.setStroke(dp(1), (color & 0x00FFFFFF) | 0x55000000);
                    row.setBackground(rowBg);
                    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rowLp.setMargins(0, 0, 0, dp(8));
                    row.setLayoutParams(rowLp);

                    TextView catV = new TextView(this);
                    catV.setText(cat);
                    catV.setTextColor(android.graphics.Color.WHITE);
                    catV.setTextSize(13f);
                    catV.setTypeface(null, Typeface.BOLD);
                    row.addView(catV, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    TextView amtV = new TextView(this);
                    amtV.setText(String.format("%.0f€ / %.0f€", s, budget));
                    amtV.setTextColor(color);
                    amtV.setTextSize(12f);
                    amtV.setTypeface(null, Typeface.BOLD);
                    row.addView(amtV);

                    list.addView(row);
                }

                TextView totalV = new TextView(this);
                totalV.setText(String.format("💰 Total semaine : %.2f€", totalSpent));
                totalV.setTextColor(0xFFFF9800);
                totalV.setTextSize(14f);
                totalV.setTypeface(null, Typeface.BOLD);
                totalV.setPadding(dp(8), dp(12), dp(8), dp(4));
                list.addView(totalV);

                new AlertDialog.Builder(this)
                    .setTitle("💰 Budgets cette semaine")
                    .setView(scroll)
                    .setPositiveButton("Fermer", null)
                    .show();
            });
        }).start();
    }

    // ── Finance — dialogs ─────────────────────────────────────────────────────

    private void showTransactionsDialog() {
        toast("📋 Chargement…");
        new Thread(() -> {
            LeaFinanceAgent fa = LeaAgentService.instance != null
                ? LeaAgentService.instance.getFinanceAgent()
                : new LeaFinanceAgent(this);
            List<LeaAgentDatabase.FinanceRow> rows = fa.getRecentTransactions(30);
            runOnUiThread(() -> {
                if (rows.isEmpty()) {
                    toast("Aucune transaction — analyser les SMS ou ajouter manuellement");
                    return;
                }
                android.widget.ScrollView scroll = new android.widget.ScrollView(this);
                LinearLayout list = new LinearLayout(this);
                list.setOrientation(LinearLayout.VERTICAL);
                list.setPadding(dp(16), dp(10), dp(16), dp(10));
                list.setBackgroundColor(0xFF011627);
                scroll.addView(list);

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "dd/MM HH:mm", java.util.Locale.getDefault());

                for (LeaAgentDatabase.FinanceRow row : rows) {
                    boolean isDebit = row.amount < 0;
                    int color = isDebit ? 0xFFF44336 : 0xFF4CAF50;

                    LinearLayout rowV = new LinearLayout(this);
                    rowV.setOrientation(LinearLayout.HORIZONTAL);
                    rowV.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    rowV.setPadding(dp(10), dp(9), dp(10), dp(9));
                    GradientDrawable rowBg = new GradientDrawable();
                    rowBg.setColor(0xFF012040);
                    rowBg.setCornerRadius(dp(8));
                    rowV.setBackground(rowBg);
                    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rowLp.setMargins(0, 0, 0, dp(6));
                    rowV.setLayoutParams(rowLp);

                    LinearLayout textCol = new LinearLayout(this);
                    textCol.setOrientation(LinearLayout.VERTICAL);

                    boolean isManual = "MANUEL".equals(row.source);
                    TextView catV = new TextView(this);
                    catV.setText(row.category + (isManual ? " ✍️" : ""));
                    catV.setTextColor(android.graphics.Color.WHITE);
                    catV.setTextSize(12f);
                    catV.setTypeface(null, android.graphics.Typeface.BOLD);
                    textCol.addView(catV);

                    String desc = row.description != null && row.description.length() > 45
                        ? row.description.substring(0, 45) + "…" : row.description;
                    TextView metaV = new TextView(this);
                    metaV.setText(sdf.format(new java.util.Date(row.timestamp))
                        + (desc != null && !desc.isEmpty() ? "  ·  " + desc : ""));
                    metaV.setTextColor(0xFF546E7A);
                    metaV.setTextSize(10f);
                    textCol.addView(metaV);

                    rowV.addView(textCol, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    TextView amtV = new TextView(this);
                    amtV.setText((isDebit ? "-" : "+") + String.format("%.2f€", Math.abs(row.amount)));
                    amtV.setTextColor(color);
                    amtV.setTextSize(13f);
                    amtV.setTypeface(null, android.graphics.Typeface.BOLD);
                    rowV.addView(amtV);

                    list.addView(rowV);
                }

                new AlertDialog.Builder(this)
                    .setTitle("📋 Transactions récentes (" + rows.size() + ")")
                    .setView(scroll)
                    .setPositiveButton("Fermer", null)
                    .show();
            });
        }).start();
    }

    private void showAddTransactionDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(16));
        form.setBackgroundColor(BG);

        EditText amountField = makeEditText(form, "Montant (ex: 25.50)", "");
        amountField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
            | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText descField = makeEditText(form, "Description (ex: McDonald's)", "");

        String[] categories = {"Restaurant", "Transport", "Loisir", "Shopping", "Abonnement", "Autre"};
        android.widget.Spinner spinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, dp(10), 0, 0);
        form.addView(spinner, slp);

        new AlertDialog.Builder(this)
            .setTitle("✍️ Ajouter une dépense")
            .setView(form)
            .setPositiveButton("Ajouter", (d, w) -> {
                try {
                    String amtStr = amountField.getText().toString().trim().replace(",", ".");
                    if (amtStr.isEmpty()) { toast("Montant requis"); return; }
                    double amount = -Math.abs(Double.parseDouble(amtStr));
                    String desc   = descField.getText().toString().trim();
                    String cat    = (String) spinner.getSelectedItem();
                    LeaFinanceAgent fa = LeaAgentService.instance != null
                        ? LeaAgentService.instance.getFinanceAgent()
                        : new LeaFinanceAgent(this);
                    fa.addManualTransaction(amount, cat, desc);
                    toast("✅ Dépense ajoutée : " + String.format("%.2f€", Math.abs(amount)));
                } catch (NumberFormatException e) {
                    toast("Montant invalide");
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void showEditBudgetsDialog() {
        LeaFinanceAgent fa = LeaAgentService.instance != null
            ? LeaAgentService.instance.getFinanceAgent()
            : new LeaFinanceAgent(this);
        Map<String, Double> budgets = fa.getBudgets();
        String[] categories = budgets.keySet().toArray(new String[0]);
        String[] labels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            labels[i] = categories[i] + "  —  " + String.format("%.0f€/sem", budgets.get(categories[i]));
        }
        final LeaFinanceAgent faRef = fa;
        new AlertDialog.Builder(this)
            .setTitle("⚙️ Modifier les budgets")
            .setItems(labels, (d, which) ->
                showSingleBudgetEdit(faRef, categories[which], budgets.get(categories[which])))
            .setNegativeButton("Fermer", null)
            .show();
    }

    private void showSingleBudgetEdit(LeaFinanceAgent fa, String category, double current) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);

        TextView hint = new TextView(this);
        hint.setText("Budget hebdomadaire pour « " + category + " » (en €)");
        hint.setTextColor(0xFF78909C);
        hint.setTextSize(12f);
        form.addView(hint);

        EditText field = makeEditText(form, "Montant €/semaine", String.valueOf((int) current));
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
            | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
            .setTitle("⚙️ Budget " + category)
            .setView(form)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                try {
                    String val = field.getText().toString().trim().replace(",", ".");
                    if (val.isEmpty()) return;
                    fa.setBudget(category, Double.parseDouble(val));
                    toast("✅ Budget " + category + " mis à jour");
                } catch (NumberFormatException e) {
                    toast("Montant invalide");
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ── Agent Sécurité ────────────────────────────────────────────────────────

    private void addSecurityActions() {
        LeaSecurityAgent sa = new LeaSecurityAgent(this);
        boolean smsOk = sa.hasSmsPerm();

        LinearLayout card = makeCard(0xFF0A0A1A, true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(sectionTitle("🔐 Sécurité", 0xFF607D8B));
        card.addView(spacer(dp(8)));

        TextView smsStatus = new TextView(this);
        smsStatus.setText(smsOk
            ? "✅ SMS — scan phishing actif"
            : "⚠️ SMS — READ_SMS manquante (scan phishing désactivé)");
        smsStatus.setTextColor(smsOk ? 0xFF4CAF50 : 0xFFFF9800);
        smsStatus.setTextSize(12f);
        card.addView(smsStatus);
        card.addView(spacer(dp(4)));

        TextView appsStatus = new TextView(this);
        appsStatus.setText("✅ Scan des apps installées — actif (no permission needed)");
        appsStatus.setTextColor(0xFF4CAF50);
        appsStatus.setTextSize(12f);
        card.addView(appsStatus);
        card.addView(spacer(dp(10)));

        Button btnScan = actionBtn("🔍 Lancer le scan maintenant", 0xFF607D8B, v -> {
            new Thread(() -> {
                new LeaSecurityAgent(this).execute();
                runOnUiThread(() -> toast("🔐 Scan sécurité terminé — vérifiez les logs"));
            }).start();
        });
        card.addView(btnScan);

        if (!smsOk) {
            card.addView(spacer(dp(8)));
            Button btnPerm = actionBtn("📋 Accorder READ_SMS", 0xFFFF9800, v ->
                new LeaSecurityAgent(this).requestSmsPermission(this));
            card.addView(btnPerm);
        }

        detailContent.addView(card);
        detailContent.addView(spacer(dp(12)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LinearLayout makeCard(int color, boolean hasBorder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(16));
        if (hasBorder) gd.setStroke(dp(1), 0xFF1E3A5F);
        card.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView sectionTitle(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(13f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.05f);
        return tv;
    }

    private Button actionBtn(String text, int color, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color);
        b.setTextSize(12f);
        b.setTypeface(null, Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color & 0x00FFFFFF | 0x1A000000);
        gd.setCornerRadius(dp(10));
        gd.setStroke(dp(1), color);
        b.setBackground(gd);
        b.setOnClickListener(listener);
        b.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        return b;
    }

    private EditText makeEditText(LinearLayout parent, String hint, String defVal) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(defVal);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(0xFF37474F);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), 0xFF37474F);
        et.setBackground(bg);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        parent.addView(et, lp);
        return et;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    // ── Gestion des permissions ───────────────────────────────────────────────

    /**
     * Gère le toggle ON/OFF d'un agent avec vérification des permissions.
     *
     * @param agent      métadonnées de l'agent
     * @param checked    état souhaité du switch
     * @param sw         référence au switch (pour revert si refus)
     * @param onEnabled  callback UI si activation confirmée
     * @param onDisabled callback UI si désactivation
     */
    private void onToggleAgent(LeaAgentManager.AgentInfo agent,
                               boolean checked,
                               Switch sw,
                               Runnable onEnabled,
                               Runnable onDisabled) {
        if (!checked) {
            // Désactivation immédiate — pas besoin de permissions
            manager.disableAgent(agent.id);
            onDisabled.run();
            toast("❌ " + agent.name + " désactivé");
            return;
        }

        // Activation : vérifier les permissions d'abord
        if (permHelper.hasPermissionsFor(agent.id)) {
            // Tout est accordé — on active directement
            manager.enableAgent(agent.id);
            onEnabled.run();
            toast("✅ " + agent.name + " activé");
        } else {
            // Des permissions manquent — on les demande
            toast("📋 Permissions requises pour " + agent.name + "…");
            permHelper.requestFor(agent.id, new LeaAgentPermissionHelper.PermissionCallback() {
                @Override
                public void onGranted(String id) {
                    runOnUiThread(() -> {
                        manager.enableAgent(id);
                        onEnabled.run();
                        toast("✅ Permissions accordées — " + agent.name + " activé!");
                    });
                }

                @Override
                public void onDenied(String id, String[] deniedPerms) {
                    runOnUiThread(() -> {
                        // Revert le switch visuellement
                        sw.setOnCheckedChangeListener(null); // évite récursion
                        sw.setChecked(false);
                        sw.setOnCheckedChangeListener((btn, c) ->
                            onToggleAgent(agent, c, sw, onEnabled, onDisabled));

                        String labels = buildDeniedLabel(deniedPerms);
                        toast("🔒 Permissions refusées (" + labels + ") — " + agent.name + " non activé");
                        showPermissionDeniedHint(agent.name, deniedPerms);
                    });
                }
            });
        }
    }

    /**
     * Obligatoire : callback Android quand l'utilisateur répond à la popup de permission.
     * Redirige vers LeaAgentPermissionHelper qui déclenche les callbacks onGranted/onDenied.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Délégation complète au helper — il appelle onGranted ou onDenied
        permHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Cas spécifique LeaSecurityAgent (codes directs de l'agent)
        if (requestCode == LeaSecurityAgent.REQ_SMS ||
            requestCode == LeaSecurityAgent.REQ_CONTACTS) {
            LeaSecurityAgent secAgent = new LeaSecurityAgent(this);
            secAgent.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /** Affiche un hint expliquant comment accorder la permission manuellement. */
    private void showPermissionDeniedHint(String agentName, String[] denied) {
        String msg = "Pour activer «" + agentName + "», accorde les permissions dans :\n"
            + "Paramètres → Applications → Léa → Permissions\n\n"
            + "Refusées : " + buildDeniedLabel(denied);

        new AlertDialog.Builder(this)
            .setTitle("🔒 Permissions requises")
            .setMessage(msg)
            .setPositiveButton("Ouvrir Paramètres", (d, w) -> {
                Intent i = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
            })
            .setNegativeButton("Plus tard", null)
            .show();
    }

    private String buildDeniedLabel(String[] perms) {
        if (perms == null || perms.length == 0) return "?";
        StringBuilder sb = new StringBuilder();
        for (String p : perms) {
            if (sb.length() > 0) sb.append(", ");
            String[] parts = p.split("\\.");
            sb.append(parts[parts.length - 1]);
        }
        return sb.toString();
    }

    // ── Real-time card refresh ────────────────────────────────────────────────

    private void refreshAgentCard(String agentId, boolean enabled) {
        LinearLayout card   = agentCardMap.get(agentId);
        TextView     nameV  = agentNameMap.get(agentId);
        TextView     statusV= agentStatusMap.get(agentId);
        Switch       sw     = agentSwitchMap.get(agentId);
        if (card == null) return;

        LeaAgentManager.AgentInfo info = null;
        for (LeaAgentManager.AgentInfo a : LeaAgentManager.ALL_AGENTS) {
            if (a.id.equals(agentId)) { info = a; break; }
        }
        if (info == null) return;

        int color = info.color;
        refreshCardBg(card, color, enabled);
        if (nameV  != null) nameV.setTextColor(enabled ? color : Color.WHITE);
        if (statusV != null) {
            statusV.setText(enabled ? "● ACTIF" : "○ INACTIF");
            statusV.setTextColor(enabled ? 0xFF4CAF50 : 0xFF546E7A);
        }
        if (sw != null) {
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(enabled);
            LeaAgentManager.AgentInfo finalInfo = info;
            sw.setOnCheckedChangeListener((btn, checked) ->
                onToggleAgent(finalInfo, checked, sw,
                    () -> refreshAgentCard(agentId, true),
                    () -> refreshAgentCard(agentId, false)));
        }
    }

    private void refreshModeCard(String modeId, boolean enabled) {
        LinearLayout card   = modeCardMap.get(modeId);
        TextView     nameV  = modeNameMap.get(modeId);
        TextView     statusV= modeStatusMap.get(modeId);
        Switch       sw     = modeSwitchMap.get(modeId);
        if (card == null) return;

        LeaModeManager.ModeInfo info = modeManager.getInfo(modeId);
        if (info == null) return;

        int color = info.color;
        refreshCardBg(card, color, enabled);
        if (nameV  != null) nameV.setTextColor(enabled ? color : Color.WHITE);
        if (statusV != null) {
            statusV.setText(enabled ? "● ACTIF" : "○ INACTIF");
            statusV.setTextColor(enabled ? 0xFF4CAF50 : 0xFF546E7A);
        }
        if (sw != null) {
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(enabled);
            sw.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) modeManager.enable(modeId);
                else         modeManager.disable(modeId);
            });
        }
    }

    private void refreshAllCards() {
        for (LeaAgentManager.AgentInfo a : LeaAgentManager.ALL_AGENTS) {
            if (agentCardMap.containsKey(a.id))
                refreshAgentCard(a.id, manager.isEnabled(a.id));
        }
        for (LeaModeManager.ModeInfo m : LeaModeManager.ALL_MODES) {
            if (modeCardMap.containsKey(m.id))
                refreshModeCard(m.id, modeManager.isEnabled(m.id));
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
