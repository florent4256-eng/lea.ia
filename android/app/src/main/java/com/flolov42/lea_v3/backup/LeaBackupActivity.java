package com.flolov42.lea_v3.backup;

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

public class LeaBackupActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int GREEN = 0xFF4CAF50;

    private LinearLayout historyList;
    private TextView statusTv;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        setContentView(buildUI());
        refreshHistory();
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeStatusCard());
        root.addView(makeActionButtons());
        root.addView(makeSectionTitle("Historique des sauvegardes"));

        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyList);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF013050, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("☁️"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Backup & Restore");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Sauvegarde automatique quotidienne + manuel");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
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
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        statusTv = new TextView(this);
        statusTv.setText(LeaBackupManager.get(this).getLastBackupSummary());
        statusTv.setTextColor(Color.WHITE); statusTv.setTextSize(14);
        card.addView(statusTv);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100); progressBar.setProgress(0);
        progressBar.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        pbLp.setMargins(0, dp(8), 0, 0);
        progressBar.setLayoutParams(pbLp);
        card.addView(progressBar);
        return card;
    }

    private LinearLayout makeActionButtons() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(20));
        row.setLayoutParams(lp);

        Button backupBtn = makeBtn("💾 Sauvegarder", CYAN, Color.BLACK);
        backupBtn.setOnClickListener(v -> startBackup());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        btnLp.setMargins(0, 0, dp(8), 0);
        backupBtn.setLayoutParams(btnLp);
        row.addView(backupBtn);

        Button autoBtn = makeBtn("⚙️ Auto-backup", 0xFF012040, CYAN);
        autoBtn.setOnClickListener(v -> Toast.makeText(this, "Auto-backup: quotidien à 02:00", Toast.LENGTH_LONG).show());
        LinearLayout.LayoutParams autoLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        autoLp.setMargins(dp(8), 0, 0, 0);
        autoBtn.setLayoutParams(autoLp);
        row.addView(autoBtn);

        return row;
    }

    private void startBackup() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        statusTv.setText("Sauvegarde en cours...");

        LeaBackupManager.get(this).createBackup(new LeaBackupManager.BackupListener() {
            @Override public void onProgress(int percent, String status) {
                runOnUiThread(() -> { progressBar.setProgress(percent); statusTv.setText(status); });
            }
            @Override public void onComplete(String path, long size) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    statusTv.setText("✅ Sauvegardé! (" + size/1024 + " Ko)");
                    Toast.makeText(LeaBackupActivity.this, "Sauvegarde complète!", Toast.LENGTH_SHORT).show();
                    refreshHistory();
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    statusTv.setText("❌ Erreur: " + error);
                    Toast.makeText(LeaBackupActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void refreshHistory() {
        historyList.removeAllViews();
        List<LeaFeaturesDatabase.BackupRecord> records = LeaFeaturesDatabase.get(this).getBackups();
        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucune sauvegarde. Cliquez 'Sauvegarder'.");
            empty.setTextColor(0xFF546E7A); empty.setTextSize(13);
            empty.setPadding(0, dp(16), 0, 0);
            historyList.addView(empty);
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
        for (LeaFeaturesDatabase.BackupRecord r : records) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(CARD); bg.setCornerRadius(dp(12)); bg.setStroke(dp(1), 0x2200E5FF);
            card.setBackground(bg);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(6), 0, dp(6));
            card.setLayoutParams(lp);

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            text.setLayoutParams(tlp);

            TextView dateTv = new TextView(this);
            dateTv.setText("☁️ " + sdf.format(new Date(r.backupDate)));
            dateTv.setTextColor(Color.WHITE); dateTv.setTextSize(14);
            dateTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            text.addView(dateTv);

            TextView sizeTv = new TextView(this);
            sizeTv.setText(r.size/1024 + " Ko  •  v" + r.version + "  •  " + r.status);
            sizeTv.setTextColor(0xFFB0BEC5); sizeTv.setTextSize(12);
            text.addView(sizeTv);

            card.addView(text);

            TextView statusDot = new TextView(this);
            statusDot.setText("✅"); statusDot.setTextSize(18);
            card.addView(statusDot);

            historyList.addView(card);
        }
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        tv.setLayoutParams(lp);
        return tv;
    }

    private Button makeBtn(String label, int bgColor, int textColor) {
        Button btn = new Button(this);
        btn.setText(label); btn.setTextColor(textColor); btn.setTextSize(13);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(50));
        if (bgColor == 0xFF012040) bg.setStroke(dp(1), CYAN);
        btn.setBackground(bg);
        return btn;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
