package com.flolov42.lea_v3.analytics;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.List;

public class LeaAnalyticsActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        LeaAnalyticsManager.get(this).trackOpen("ANALYTICS");
        setContentView(buildUI());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LeaAnalyticsManager.get(this).trackClose("ANALYTICS");
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeKPIRow());
        root.addView(makeSectionTitle("Progression XP — 7 derniers jours"));
        root.addView(makeXPChart());
        root.addView(makeSectionTitle("Habitudes complétées"));
        root.addView(makeHabitsChart());
        root.addView(makeSectionTitle("Statistiques Globales"));
        root.addView(makeGlobalStats());

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF013040, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("📊"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Analytics Dashboard");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Tes stats en temps réel");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeKPIRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        row.setLayoutParams(lp);

        LeaPlusDatabase plus = LeaPlusDatabase.get(this);
        LeaPlusDatabase.CharStats stats = plus.getCharStats();
        int coins = plus.getCoinBalance();
        int unlocked = LeaFeaturesDatabase.get(this).getUnlockedCount();
        String topFeature = LeaAnalyticsManager.get(this).getMostUsedFeature();

        row.addView(makeKPIChip("Niv. " + stats.level, "Level", 0xFF7B2CBF));
        row.addView(makeKPIChip(stats.xp + " XP", "Total XP", 0xFF00B0CC));
        row.addView(makeKPIChip(coins + " 🪙", "Coins", 0xFFFFC107));
        row.addView(makeKPIChip(unlocked + " 🏆", "Badges", 0xFF4CAF50));
        return row;
    }

    private LinearLayout makeKPIChip(String value, String label, int accentColor) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(12)); bg.setStroke(dp(2), accentColor);
        chip.setBackground(bg);
        chip.setPadding(dp(4), dp(12), dp(4), dp(12));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        chip.setLayoutParams(lp);

        TextView val = new TextView(this); val.setText(value);
        val.setTextColor(Color.WHITE); val.setTextSize(14);
        val.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); val.setGravity(Gravity.CENTER);
        chip.addView(val);

        TextView lbl = new TextView(this); lbl.setText(label);
        lbl.setTextColor(0xFF78909C); lbl.setTextSize(10); lbl.setGravity(Gravity.CENTER);
        chip.addView(lbl);

        return chip;
    }

    private View makeXPChart() {
        List<LeaFeaturesDatabase.DailyStat> stats = LeaFeaturesDatabase.get(this).getWeekStats();
        BarChartView chart = new BarChartView(this, stats, "xp", CYAN);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        lp.setMargins(0, dp(8), 0, dp(16));
        chart.setLayoutParams(lp);
        return chart;
    }

    private View makeHabitsChart() {
        List<LeaFeaturesDatabase.DailyStat> stats = LeaFeaturesDatabase.get(this).getWeekStats();
        BarChartView chart = new BarChartView(this, stats, "habits", 0xFF4CAF50);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        lp.setMargins(0, dp(8), 0, dp(16));
        chart.setLayoutParams(lp);
        return chart;
    }

    private LinearLayout makeGlobalStats() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        String topFeature = LeaAnalyticsManager.get(this).getMostUsedFeature();
        int todayEvents = LeaAnalyticsManager.get(this).getTodayEventCount();
        LeaPlusDatabase.CharStats charStats = LeaPlusDatabase.get(this).getCharStats();
        int habitsCount = LeaPlusDatabase.get(this).getActiveHabits().size();

        String[][] rows = {
            {"🎯", "Feature la + utilisée", topFeature},
            {"📅", "Actions aujourd'hui", String.valueOf(todayEvents)},
            {"🏅", "Monde actuel", charStats.world},
            {"🌱", "Habitudes actives", String.valueOf(habitsCount) + " habitudes"},
            {"👑", "Boss vaincus", String.valueOf(charStats.bossDefeated)},
            {"✅", "Tâches totales", String.valueOf(charStats.totalTasks)},
        };

        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, dp(6), 0, dp(6));
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
            if (card.getChildCount() < rows.length * 1) {
                View div = new View(this);
                div.setBackgroundColor(0x1500E5FF);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                div.setLayoutParams(dlp);
                card.addView(div);
            }
        }

        return card;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, dp(4));
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    // ── Inline bar chart view ─────────────────────────────────────────────────
    static class BarChartView extends View {
        private final List<LeaFeaturesDatabase.DailyStat> stats;
        private final String mode;
        private final int barColor;
        private final Paint barPaint, textPaint, bgPaint, gridPaint;

        BarChartView(Context ctx, List<LeaFeaturesDatabase.DailyStat> stats, String mode, int barColor) {
            super(ctx);
            this.stats = stats; this.mode = mode; this.barColor = barColor;
            barPaint = new Paint(Paint.ANTI_ALIAS_FLAG); barPaint.setColor(barColor);
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint.setColor(0xFFB0BEC5); textPaint.setTextSize(28);
            bgPaint = new Paint(); bgPaint.setColor(0xFF012040);
            gridPaint = new Paint(); gridPaint.setColor(0x1500E5FF); gridPaint.setStrokeWidth(1);
            setBackgroundColor(0xFF012040);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (stats.isEmpty()) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Pas encore de données", getWidth()/2f, getHeight()/2f, textPaint);
                return;
            }
            int maxVal = 1;
            for (LeaFeaturesDatabase.DailyStat s : stats) {
                int v = "xp".equals(mode) ? s.xpEarned : s.habitsCompleted;
                if (v > maxVal) maxVal = v;
            }
            float padL = 60, padR = 20, padT = 20, padB = 40;
            float w = getWidth() - padL - padR;
            float h = getHeight() - padT - padB;
            int n = stats.size();
            float barW = w / n * 0.6f;
            float gap = w / n;

            // Grid lines
            for (int i = 0; i <= 4; i++) {
                float y = padT + h * (1f - i / 4f);
                canvas.drawLine(padL, y, getWidth()-padR, y, gridPaint);
            }

            // Bars
            for (int i = 0; i < n; i++) {
                LeaFeaturesDatabase.DailyStat s = stats.get(n-1-i);
                int val = "xp".equals(mode) ? s.xpEarned : s.habitsCompleted;
                float barH = h * ((float)val / maxVal);
                float x = padL + i * gap + (gap - barW) / 2;
                float top = padT + h - barH;

                RectF rect = new RectF(x, top, x + barW, padT + h);
                barPaint.setAlpha(200);
                canvas.drawRoundRect(rect, 8, 8, barPaint);

                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(22);
                if (val > 0) canvas.drawText(String.valueOf(val), x + barW/2, top - 8, textPaint);

                String[] parts = s.date.split("-");
                String label = parts.length >= 3 ? parts[2] + "/" + parts[1] : s.date;
                textPaint.setTextSize(20);
                canvas.drawText(label, x + barW/2, getHeight() - 8, textPaint);
            }
        }
    }
}
