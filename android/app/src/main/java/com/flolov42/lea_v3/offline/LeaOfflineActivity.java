package com.flolov42.lea_v3.offline;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LeaOfflineActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    private TextView statusBadge;
    private LinearLayout queueList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        setContentView(buildUI());
        refresh();
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeStatusCard());
        root.addView(makeSyncButton());
        root.addView(makeSectionTitle("File d'attente hors ligne"));

        queueList = new LinearLayout(this);
        queueList.setOrientation(LinearLayout.VERTICAL);
        root.addView(queueList);

        scroll.addView(root);
        return scroll;
    }

    private void refresh() {
        LeaOfflineManager mgr = LeaOfflineManager.get(this);
        boolean online = mgr.isOnline();
        if (statusBadge != null) {
            statusBadge.setText(mgr.getStatusText());
            statusBadge.setTextColor(online ? 0xFF4CAF50 : 0xFFFF4444);
        }

        queueList.removeAllViews();
        List<LeaFeaturesDatabase.OfflineAction> pending = LeaFeaturesDatabase.get(this).getPendingActions();
        if (pending.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(online ? "✅ Tout est synchronisé!" : "Aucune action en attente");
            empty.setTextColor(0xFF546E7A); empty.setTextSize(13);
            empty.setPadding(0, dp(16), 0, 0);
            queueList.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
        for (LeaFeaturesDatabase.OfflineAction a : pending) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(CARD); bg.setCornerRadius(dp(12)); bg.setStroke(dp(1), 0x2200E5FF);
            card.setBackground(bg);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(6), 0, dp(6));
            card.setLayoutParams(lp);

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            text.setLayoutParams(tlp);

            TextView typeTv = new TextView(this);
            typeTv.setText("⏳ " + a.actionType);
            typeTv.setTextColor(Color.WHITE); typeTv.setTextSize(14);
            typeTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            text.addView(typeTv);

            TextView dateTv = new TextView(this);
            dateTv.setText(sdf.format(new Date(a.timestamp)) + "  •  " + a.syncStatus);
            dateTv.setTextColor(0xFF546E7A); dateTv.setTextSize(12);
            text.addView(dateTv);

            card.addView(text);

            TextView dot = new TextView(this);
            dot.setText("🕐"); dot.setTextSize(18);
            card.addView(dot);

            queueList.addView(card);
        }
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF01203A, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("📡"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Mode Hors Ligne");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        statusBadge = new TextView(this);
        statusBadge.setText(LeaOfflineManager.get(this).getStatusText());
        statusBadge.setTextSize(18); statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(0, dp(8), 0, 0);
        h.addView(statusBadge);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeStatusCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        LeaOfflineManager mgr = LeaOfflineManager.get(this);
        int pending = mgr.getPendingCount();

        String[][] rows = {
            {"🔄", "Actions en attente", String.valueOf(pending)},
            {"✅", "Fonctionnalités offline", "Habitudes, Quêtes, Stats"},
            {"☁️", "Sync automatique", "Dès retour en ligne"},
        };

        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, dp(4), 0, dp(4));
            r.setLayoutParams(rlp);

            TextView ico = new TextView(this); ico.setText(row[0]); ico.setTextSize(18);
            ico.setMinWidth(dp(36)); r.addView(ico);

            TextView lbl = new TextView(this); lbl.setText(row[1]);
            lbl.setTextColor(0xFFB0BEC5); lbl.setTextSize(13);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lbl.setLayoutParams(llp); r.addView(lbl);

            TextView val = new TextView(this); val.setText(row[2]);
            val.setTextColor(CYAN); val.setTextSize(13);
            val.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            r.addView(val);

            card.addView(r);
        }
        return card;
    }

    private Button makeSyncButton() {
        Button btn = new Button(this);
        btn.setText("🔄 Synchroniser Maintenant");
        btn.setTextColor(Color.BLACK); btn.setTextSize(15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CYAN); bg.setCornerRadius(dp(50));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, 0, 0, dp(20));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> {
            LeaOfflineManager.get(this).syncPending();
            Toast.makeText(this, "Synchronisation lancée!", Toast.LENGTH_SHORT).show();
            refresh();
        });
        return btn;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8)); tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
