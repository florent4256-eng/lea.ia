package com.flolov42.lea_v3.achievements;

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

public class LeaAchievementActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        LeaAchievementManager.get(this).checkAll();
        setContentView(buildUI());
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeStatsBar());

        String[] tiers = {"Tier 1 — Débutant", "Tier 2 — Intermédiaire", "Tier 3 — Avancé", "Tier 4 — Légendaire"};
        for (int tier = 1; tier <= 4; tier++) {
            root.addView(makeSectionTitle(tiers[tier - 1]));
            List<LeaFeaturesDatabase.Achievement> list = getAchievementsForTier(tier);
            for (LeaFeaturesDatabase.Achievement a : list) {
                root.addView(makeAchievementCard(a));
            }
        }

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF2D1B69, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("🏆"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);

        TextView title = new TextView(this);
        title.setText("Achievements");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);

        TextView sub = new TextView(this);
        sub.setText(LeaAchievementManager.get(this).getSummary());
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(14); sub.setGravity(Gravity.CENTER);
        h.addView(sub);

        // Back button
        TextView back = new TextView(this);
        back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14);
        back.setPadding(0, dp(12), 0, 0);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);

        return h;
    }

    private LinearLayout makeStatsBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        bar.setLayoutParams(lp);

        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(this);
        int unlocked = db.getUnlockedCount();
        int total = db.getAllAchievements().size();

        bar.addView(makeStatChip("🔓 " + unlocked, "Débloqués"));
        bar.addView(makeStatChip("🔒 " + (total - unlocked), "Verrouillés"));
        bar.addView(makeStatChip("📊 " + (total > 0 ? unlocked * 100 / total : 0) + "%", "Progression"));
        return bar;
    }

    private LinearLayout makeStatChip(String value, String label) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(12)); bg.setStroke(dp(1), 0x3300E5FF);
        chip.setBackground(bg);
        chip.setPadding(dp(8), dp(12), dp(8), dp(12));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        chip.setLayoutParams(lp);

        TextView val = new TextView(this); val.setText(value); val.setTextColor(CYAN);
        val.setTextSize(18); val.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); val.setGravity(Gravity.CENTER);
        chip.addView(val);

        TextView lbl = new TextView(this); lbl.setText(label); lbl.setTextColor(0xFFB0BEC5);
        lbl.setTextSize(11); lbl.setGravity(Gravity.CENTER);
        chip.addView(lbl);

        return chip;
    }

    private TextView makeSectionTitle(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(CYAN); tv.setTextSize(15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, dp(8));
        tv.setLayoutParams(lp);
        return tv;
    }

    private LinearLayout makeAchievementCard(LeaFeaturesDatabase.Achievement a) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        boolean locked = a.unlocked == 0;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(locked ? 0xFF010F1A : CARD);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), locked ? 0x1A00E5FF : 0x6600E5FF);
        card.setBackground(bg);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(lp);

        TextView iconTv = new TextView(this);
        iconTv.setText(locked ? "🔒" : a.icon);
        iconTv.setTextSize(28); iconTv.setGravity(Gravity.CENTER);
        iconTv.setMinWidth(dp(48));
        if (locked) iconTv.setAlpha(0.4f);
        card.addView(iconTv);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tbLp.setMargins(dp(12), 0, 0, 0);
        textBlock.setLayoutParams(tbLp);

        TextView nameTv = new TextView(this);
        nameTv.setText(a.name);
        nameTv.setTextColor(locked ? 0xFF546E7A : Color.WHITE);
        nameTv.setTextSize(15); nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textBlock.addView(nameTv);

        TextView descTv = new TextView(this);
        descTv.setText(a.description);
        descTv.setTextColor(0xFF78909C); descTv.setTextSize(12);
        textBlock.addView(descTv);

        if (!locked && a.unlockDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
            TextView dateTv = new TextView(this);
            dateTv.setText("✓ " + sdf.format(new Date(a.unlockDate)));
            dateTv.setTextColor(0xFF4CAF50); dateTv.setTextSize(11);
            textBlock.addView(dateTv);
        } else if (locked && a.target > 1) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(a.target); pb.setProgress(a.progress);
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            pbLp.setMargins(0, dp(4), 0, 0);
            pb.setLayoutParams(pbLp);
            textBlock.addView(pb);
            TextView prog = new TextView(this);
            prog.setText(a.progress + "/" + a.target);
            prog.setTextColor(0xFF546E7A); prog.setTextSize(11);
            textBlock.addView(prog);
        }

        card.addView(textBlock);

        if (!locked) {
            TextView star = new TextView(this);
            star.setText("⭐");
            star.setTextSize(20);
            card.addView(star);
        }

        return card;
    }

    private List<LeaFeaturesDatabase.Achievement> getAchievementsForTier(int tier) {
        List<LeaFeaturesDatabase.Achievement> all = LeaFeaturesDatabase.get(this).getAllAchievements();
        List<LeaFeaturesDatabase.Achievement> result = new ArrayList<>();
        for (LeaFeaturesDatabase.Achievement a : all) if (a.tier == tier) result.add(a);
        return result;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
