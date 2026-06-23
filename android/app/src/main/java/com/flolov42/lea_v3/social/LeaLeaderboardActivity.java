package com.flolov42.lea_v3.social;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class LeaLeaderboardActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int GOLD = 0xFFFFD700;

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
        root.addView(makeMyRank());
        root.addView(makeSectionTitle("🏆 Classement Mondial — Top 10"));
        root.addView(makeLeaderboard());

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF2A1A00, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("🏆"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Classement");
        title.setTextColor(GOLD); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeMyRank() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1A0D00); bg.setCornerRadius(dp(14)); bg.setStroke(dp(2), GOLD);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(this);
        LeaFeaturesDatabase.SocialProfile profile = db.getMyProfile();
        LeaPlusDatabase plus = LeaPlusDatabase.get(this);
        LeaPlusDatabase.CharStats stats = plus.getCharStats();
        db.syncProfileStats(stats.xp, db.getUnlockedCount());

        TextView avatarTv = new TextView(this); avatarTv.setText(profile.avatar);
        avatarTv.setTextSize(32); avatarTv.setMinWidth(dp(52)); avatarTv.setGravity(Gravity.CENTER);
        card.addView(avatarTv);

        LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(dp(12), 0, 0, 0); text.setLayoutParams(tlp);

        TextView nameTv = new TextView(this); nameTv.setText(profile.pseudo + " (Moi)");
        nameTv.setTextColor(GOLD); nameTv.setTextSize(16);
        nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.addView(nameTv);

        TextView statTv = new TextView(this); statTv.setText(stats.xp + " XP  •  Niv." + stats.level);
        statTv.setTextColor(0xFFB0BEC5); statTv.setTextSize(13);
        text.addView(statTv);

        card.addView(text);

        TextView rankTv = new TextView(this); rankTv.setText("👑");
        rankTv.setTextSize(28);
        card.addView(rankTv);

        return card;
    }

    private LinearLayout makeLeaderboard() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        List<LeaFeaturesDatabase.LeaderEntry> cached = LeaFeaturesDatabase.get(this).getLeaderboardCache();
        if (cached.isEmpty()) {
            // Populate with demo data for local mode
            cached = generateDemoLeaderboard();
        }

        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < cached.size(); i++) {
            LeaFeaturesDatabase.LeaderEntry entry = cached.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);

            boolean isTop3 = i < 3;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(isTop3 ? 0xFF1A1200 : CARD);
            bg.setCornerRadius(dp(12));
            bg.setStroke(dp(1), isTop3 ? GOLD : 0x1500E5FF);
            card.setBackground(bg);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(5), 0, dp(5));
            card.setLayoutParams(lp);

            TextView rankTv = new TextView(this);
            rankTv.setText(isTop3 ? medals[i] : String.valueOf(i + 1));
            rankTv.setTextSize(isTop3 ? 22 : 16); rankTv.setMinWidth(dp(44)); rankTv.setGravity(Gravity.CENTER);
            rankTv.setTextColor(isTop3 ? GOLD : 0xFF546E7A);
            card.addView(rankTv);

            LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tlp.setMargins(dp(8), 0, 0, 0); text.setLayoutParams(tlp);

            TextView uname = new TextView(this); uname.setText(entry.userId);
            uname.setTextColor(isTop3 ? GOLD : Color.WHITE); uname.setTextSize(14);
            if (isTop3) uname.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            text.addView(uname);

            TextView xpTv = new TextView(this); xpTv.setText(entry.xp + " XP  •  🔥" + entry.streakRecord + "j");
            xpTv.setTextColor(0xFF78909C); xpTv.setTextSize(12);
            text.addView(xpTv);

            card.addView(text);

            list.addView(card);
        }
        return list;
    }

    private List<LeaFeaturesDatabase.LeaderEntry> generateDemoLeaderboard() {
        String[][] data = {
            {"GalaxyMaster", "4820", "89"}, {"LéaHero", "4200", "65"}, {"ZenCoder", "3980", "72"},
            {"DataWizard", "3540", "55"}, {"MindfulDev", "3200", "48"}, {"QuantumLeap", "2980", "41"},
            {"Productive42", "2750", "38"}, {"HabitKing", "2500", "61"}, {"FlowState", "2200", "29"}, {"NightOwl", "1980", "33"},
        };
        List<LeaFeaturesDatabase.LeaderEntry> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            LeaFeaturesDatabase.LeaderEntry e = new LeaFeaturesDatabase.LeaderEntry();
            e.rank = i+1; e.userId = data[i][0];
            e.xp = Integer.parseInt(data[i][1]);
            e.streakRecord = Integer.parseInt(data[i][2]);
            list.add(e);
        }
        return list;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextColor(GOLD); tv.setTextSize(15); tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
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
