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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Date;
import java.util.List;

/**
 * Full-screen network diagnostics. Shows:
 * - Live status badge (🟢 LOCAL / 🔵 CLOUDFLARE / 🔴 OFFLINE)
 * - Latency for both endpoints
 * - Connection stats (total connections, errors, avg latency per type)
 * - Speed-test trigger
 * - Recent log tail (last 30 entries)
 * - Settings toggle for auto-switch
 */
public class LeaNetworkStatusActivity extends Activity implements LeaNetworkDetector.ConnectionListener {

    private static final int BG     = 0xFF011627;
    private static final int CARD   = 0xFF012040;
    private static final int CYAN   = 0xFF00E5FF;
    private static final int GREEN  = 0xFF4CAF50;
    private static final int BLUE   = 0xFF2196F3;
    private static final int AMBER  = 0xFFFFB300;
    private static final int RED    = 0xFFFF4444;
    private static final int TEXT   = 0xFFE0F7FA;
    private static final int MUTED  = 0xFF7B97AA;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView    tvStatus, tvStatusLabel;
    private TextView    tvLocalLatency, tvCloudflareLatency;
    private TextView    tvLocalStats, tvCloudflareStats;
    private LinearLayout logContainer;
    private Button      btnRefresh, btnSpeedTest;
    private Switch      swAutoSwitch;
    private ProgressBar pbSpeedTest;
    private TextView    tvSpeedResult;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private BroadcastReceiver networkReceiver;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        LeaFeatureDetailActivity.applyImmersive(this);
        LeaNetworkDetector.addListener(this);
        setContentView(buildLayout());
        refreshAll();
        registerNetworkReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LeaNetworkDetector.removeListener(this);
        if (networkReceiver != null) unregisterReceiver(networkReceiver);
    }

    @Override
    public void onConnectionChanged(String newType, long latencyMs) {
        ui.post(this::refreshAll);
    }

    // ── UI builder ────────────────────────────────────────────────────────────
    private View buildLayout() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(32));
        sv.addView(root);

        // Back
        Button btnBack = new Button(this);
        btnBack.setText("← Retour");
        btnBack.setTextColor(CYAN); btnBack.setBackground(null);
        btnBack.setOnClickListener(v -> finish());
        root.addView(btnBack);

        // Title
        TextView title = new TextView(this);
        title.setText("📡 Réseau Léa");
        title.setTextColor(CYAN); title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(8), 0, dp(16));
        root.addView(title);

        // ── Status card ───────────────────────────────────────────────────────
        LinearLayout statusCard = card(root);
        tvStatus = textView("🟢", 40, Color.WHITE); tvStatus.setGravity(Gravity.CENTER);
        tvStatusLabel = textView("LOCAL WiFi", 16, Color.WHITE); tvStatusLabel.setGravity(Gravity.CENTER);
        TextView tvStatusSub = textView("192.168.1.102:8080", 12, MUTED); tvStatusSub.setGravity(Gravity.CENTER);
        statusCard.addView(tvStatus);
        statusCard.addView(tvStatusLabel);
        statusCard.addView(tvStatusSub);

        // Latency row
        LinearLayout latRow = new LinearLayout(this);
        latRow.setOrientation(LinearLayout.HORIZONTAL);
        latRow.setPadding(0, dp(12), 0, 0);
        tvLocalLatency = textView("Local: —", 13, GREEN);
        tvLocalLatency.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        tvCloudflareLatency = textView("Cloudflare: —", 13, BLUE);
        tvCloudflareLatency.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        tvCloudflareLatency.setGravity(Gravity.END);
        latRow.addView(tvLocalLatency); latRow.addView(tvCloudflareLatency);
        statusCard.addView(latRow);

        // Refresh + Speed test buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(12), 0, 0);
        btnRefresh = makeButton("🔄 Sonder", CYAN, Color.BLACK);
        btnRefresh.setOnClickListener(v -> {
            btnRefresh.setEnabled(false);
            btnRefresh.setText("⏳ Sondage...");
            LeaNetworkDetector.probeAsync(this);
            ui.postDelayed(() -> {
                btnRefresh.setEnabled(true); btnRefresh.setText("🔄 Sonder");
                refreshAll();
            }, 3_000);
        });
        btnRow.addView(btnRefresh);

        btnSpeedTest = makeButton("⚡ Speed Test", 0xFF7B2CBF, Color.WHITE);
        btnSpeedTest.setOnClickListener(v -> runSpeedTest());
        btnRow.addView(btnSpeedTest);
        statusCard.addView(btnRow);

        // Speed test progress + result
        pbSpeedTest = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pbSpeedTest.setIndeterminate(true);
        pbSpeedTest.setVisibility(View.GONE);
        statusCard.addView(pbSpeedTest);
        tvSpeedResult = textView("", 12, MUTED);
        tvSpeedResult.setPadding(0, dp(4), 0, 0);
        statusCard.addView(tvSpeedResult);

        // ── Settings card ─────────────────────────────────────────────────────
        LinearLayout settingsCard = card(root);
        settingsCard.addView(textView("Paramètres", 14, CYAN));

        LinearLayout autoRow = new LinearLayout(this);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        autoRow.setGravity(Gravity.CENTER_VERTICAL);
        autoRow.setPadding(0, dp(8), 0, 0);
        TextView tvAutoLabel = textView("Auto-switch (30 min)", 13, TEXT);
        tvAutoLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        swAutoSwitch = new Switch(this);
        swAutoSwitch.setChecked(LeaNetworkConfig.isAutoSwitch(this));
        swAutoSwitch.setOnCheckedChangeListener((b, checked) -> LeaNetworkConfig.setAutoSwitch(this, checked));
        autoRow.addView(tvAutoLabel); autoRow.addView(swAutoSwitch);
        settingsCard.addView(autoRow);

        // URL info
        settingsCard.addView(pad(dp(8)));
        settingsCard.addView(textView("Priorité 1 (LOCAL):", 11, MUTED));
        settingsCard.addView(textView(LeaNetworkConfig.LOCAL_WS, 11, GREEN));
        settingsCard.addView(pad(dp(4)));
        settingsCard.addView(textView("Priorité 2 (CLOUDFLARE):", 11, MUTED));
        settingsCard.addView(textView(LeaNetworkConfig.CLOUDFLARE_WS, 11, BLUE));

        // ── Stats card ────────────────────────────────────────────────────────
        LinearLayout statsCard = card(root);
        statsCard.addView(textView("Statistiques", 14, CYAN));
        tvLocalStats = textView("—", 12, TEXT); tvLocalStats.setPadding(0, dp(6), 0, 0);
        tvCloudflareStats = textView("—", 12, TEXT);
        statsCard.addView(tvLocalStats);
        statsCard.addView(tvCloudflareStats);

        // ── Log card ──────────────────────────────────────────────────────────
        LinearLayout logCard = card(root);
        logCard.addView(textView("Journal réseau (30 derniers)", 14, CYAN));
        logContainer = new LinearLayout(this);
        logContainer.setOrientation(LinearLayout.VERTICAL);
        logContainer.setPadding(0, dp(8), 0, 0);
        logCard.addView(logContainer);

        return sv;
    }

    // ── Refresh data ──────────────────────────────────────────────────────────
    private void refreshAll() {
        String type    = LeaNetworkDetector.getCachedType(this);
        long latency   = LeaNetworkDetector.getCachedLatency(this);

        // Badge
        switch (type) {
            case LeaNetworkDetector.TYPE_LOCAL:
                tvStatus.setText("🟢"); tvStatusLabel.setText("LOCAL WiFi");
                tvStatusLabel.setTextColor(GREEN); break;
            case LeaNetworkDetector.TYPE_CLOUDFLARE:
                tvStatus.setText("🔵"); tvStatusLabel.setText("Cloudflare Tunnel");
                tvStatusLabel.setTextColor(BLUE); break;
            default:
                tvStatus.setText("🔴"); tvStatusLabel.setText("Hors ligne");
                tvStatusLabel.setTextColor(RED); break;
        }

        // Latency
        tvLocalLatency.setText("Local: " + (type.equals(LeaNetworkDetector.TYPE_LOCAL) && latency > 0
            ? latency + "ms" : "N/A"));
        tvCloudflareLatency.setText("Cloudflare: " + (type.equals(LeaNetworkDetector.TYPE_CLOUDFLARE) && latency > 0
            ? latency + "ms" : "N/A"));

        // Stats
        LeaNetworkDatabase db = LeaNetworkDatabase.get(this);
        tvLocalStats.setText("🟢 LOCAL — " + db.getTotalConnections("LOCAL") + " connexions, "
            + db.getTotalErrors("LOCAL") + " erreurs, moy. " + db.getAvgLatency("LOCAL") + "ms");
        tvCloudflareStats.setText("🔵 CLOUDFLARE — " + db.getTotalConnections("CLOUDFLARE") + " connexions, "
            + db.getTotalErrors("CLOUDFLARE") + " erreurs, moy. " + db.getAvgLatency("CLOUDFLARE") + "ms");

        // Logs
        refreshLogs();
    }

    private void refreshLogs() {
        logContainer.removeAllViews();
        List<LeaNetworkDatabase.NetworkLog> logs = LeaNetworkDatabase.get(this).getRecentLogs(30);
        if (logs.isEmpty()) {
            logContainer.addView(textView("Aucun événement.", 11, MUTED));
            return;
        }
        for (LeaNetworkDatabase.NetworkLog log : logs) {
            String ts = DateFormat.format("HH:mm:ss", new Date(log.timestamp)).toString();
            String typeIcon = "LOCAL".equals(log.connectionType) ? "🟢"
                : "CLOUDFLARE".equals(log.connectionType) ? "🔵" : "🔴";
            String line = ts + " " + typeIcon + " " + log.event + " — " + log.details
                + (log.latencyMs > 0 ? " (" + log.latencyMs + "ms)" : "");
            TextView tv = textView(line, 11, MUTED);
            tv.setPadding(0, dp(2), 0, 0);
            logContainer.addView(tv);
        }
    }

    // ── Speed test ────────────────────────────────────────────────────────────
    private void runSpeedTest() {
        btnSpeedTest.setEnabled(false);
        pbSpeedTest.setVisibility(View.VISIBLE);
        tvSpeedResult.setText("⏳ Test en cours...");
        LeaNetworkSpeedTest.runBothAsync(this, new LeaNetworkSpeedTest.SpeedTestCallback() {
            String localText = "Local: —", cfText = "Cloudflare: —";
            @Override public void onLocalResult(LeaNetworkSpeedTest.SpeedResult r) {
                localText = "🟢 LOCAL " + (r.reachable ? r.medianMs + "ms (min=" + r.minMs + "ms)" : "injoignable");
                ui.post(() -> tvSpeedResult.setText(localText + "\n" + cfText));
            }
            @Override public void onCloudflareResult(LeaNetworkSpeedTest.SpeedResult r) {
                cfText = "🔵 CLOUDFLARE " + (r.reachable ? r.medianMs + "ms (min=" + r.minMs + "ms)" : "injoignable");
                ui.post(() -> tvSpeedResult.setText(localText + "\n" + cfText));
            }
            @Override public void onDone() {
                ui.post(() -> {
                    pbSpeedTest.setVisibility(View.GONE);
                    btnSpeedTest.setEnabled(true);
                    refreshLogs();
                });
            }
        });
    }

    // ── Broadcast receiver ────────────────────────────────────────────────────
    private void registerNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (LeaNetworkDetectionWorker.ACTION_NETWORK_CHANGED.equals(i.getAction())) {
                    ui.post(() -> refreshAll());
                }
            }
        };
        IntentFilter f = new IntentFilter(LeaNetworkDetectionWorker.ACTION_NETWORK_CHANGED);
        registerReceiver(networkReceiver, f);
    }

    // ── Layout helpers ─────────────────────────────────────────────────────────
    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        parent.addView(card, lp);
        return card;
    }

    private TextView textView(String text, float size, int color) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextSize(size); tv.setTextColor(color);
        return tv;
    }

    private Button makeButton(String text, int bg, int fg) {
        Button b = new Button(this);
        b.setText(text); b.setTextColor(fg);
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg); d.setCornerRadius(dp(8));
        b.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private View pad(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, height));
        return v;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
