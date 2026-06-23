package com.flolov42.lea_v3.plus.lifestyle;

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

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Calendar;

public class LeaAnnualReportDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF000D1A;
    private static final int CARD  = 0xFF001A2E;
    private static final int CARD2 = 0xFF00243F;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int PURPLE= 0xFF7C3AED;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;

    @Override protected String getFeatureId() { return LeaPlusDatabase.REPORT; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        int year = Calendar.getInstance().get(Calendar.YEAR);

        // ── Hero bannière ──────────────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setBackgroundColor(CARD);
        hero.setPadding(dp(20), dp(28), dp(20), dp(24));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        TextView starTv = tv("🌟", 48, GOLD, Typeface.NORMAL);
        starTv.setGravity(Gravity.CENTER); hero.addView(starTv);

        TextView titleTv = tv("Rapport Annuel LÉA", 20, GOLD, Typeface.BOLD);
        titleTv.setGravity(Gravity.CENTER); titleTv.setPadding(0, dp(8), 0, dp(2)); hero.addView(titleTv);

        TextView yearTv = tv(String.valueOf(year), 36, CYAN, Typeface.BOLD);
        yearTv.setGravity(Gravity.CENTER); hero.addView(yearTv);

        TextView subTv = tv("Ton aventure en chiffres", 11, DIM2, Typeface.NORMAL);
        subTv.setGravity(Gravity.CENTER); hero.addView(subTv);
        parent.addView(hero);

        // ── Chiffres clés ──────────────────────────────────────────────────
        LeaPlusDatabase.CharStats cs = db.getCharStats();
        int coins = db.getCoinBalance();
        String totalXp   = db.getStat(year, "total_xp");
        String questsDone= db.getStat(year, "quests_done");
        String habitsDone= db.getStat(year, "habits_completed");
        String streakBest= db.getStat(year, "streak_best");

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(-1, -2);
        gLp.setMargins(dp(12), dp(10), dp(12), 0); grid.setLayoutParams(gLp);

        // Ligne 1
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(2);
        LinearLayout.LayoutParams r1Lp = new LinearLayout.LayoutParams(-1, -2);
        r1Lp.setMargins(0, 0, 0, dp(6)); row1.setLayoutParams(r1Lp);
        row1.addView(bigStat("⚔️", "Niveau", String.valueOf(cs.level), GOLD));
        row1.addView(bigStat("✨", "XP Total", totalXp != null ? totalXp : String.valueOf(cs.xp), CYAN));
        grid.addView(row1);

        // Ligne 2
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setWeightSum(2);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(-1, -2);
        r2Lp.setMargins(0, 0, 0, dp(6)); row2.setLayoutParams(r2Lp);
        row2.addView(bigStat("🎯", "Quêtes", questsDone != null ? questsDone : "—", GREEN));
        row2.addView(bigStat("🔥", "Streak", streakBest != null ? streakBest + "j" : "—", 0xFFF59E0B));
        grid.addView(row2);

        // Ligne 3
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setWeightSum(2);
        row3.addView(bigStat("🔗", "Habitudes", habitsDone != null ? habitsDone : "—", PURPLE));
        row3.addView(bigStat("💰", "Coins", String.valueOf(coins), GOLD));
        grid.addView(row3);
        parent.addView(grid);

        // ── Monde atteint ──────────────────────────────────────────────────
        secHeader(parent, "🗺️ TON MONDE");
        LinearLayout worldCard = new LinearLayout(this);
        worldCard.setOrientation(LinearLayout.HORIZONTAL);
        worldCard.setGravity(Gravity.CENTER_VERTICAL);
        worldCard.setBackgroundColor(CARD);
        worldCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams wcLp = new LinearLayout.LayoutParams(-1, -2);
        wcLp.setMargins(dp(12), dp(4), dp(12), 0); worldCard.setLayoutParams(wcLp);
        String worldEmoji = cs.level < 5 ? "🌿" : cs.level < 10 ? "🏰" : cs.level < 20 ? "🌋" : "⭐";
        TextView wEmo = tv(worldEmoji, 32, WHITE, Typeface.NORMAL);
        wEmo.setPadding(0, 0, dp(14), 0); worldCard.addView(wEmo);
        LinearLayout wTexts = new LinearLayout(this); wTexts.setOrientation(LinearLayout.VERTICAL);
        wTexts.addView(tv(cs.world, 14, WHITE, Typeface.BOLD));
        wTexts.addView(tv("Boss vaincus : " + cs.bossDefeated + "  ·  Tâches : " + cs.totalTasks, 11, DIM2, Typeface.NORMAL));
        worldCard.addView(wTexts);
        parent.addView(worldCard);

        // ── Message ────────────────────────────────────────────────────────
        secHeader(parent, "💬 MESSAGE DE LÉA");
        LinearLayout msgCard = new LinearLayout(this);
        msgCard.setOrientation(LinearLayout.VERTICAL);
        msgCard.setBackgroundColor(CARD);
        msgCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(-1, -2);
        mLp.setMargins(dp(12), dp(4), dp(12), dp(24)); msgCard.setLayoutParams(mLp);
        TextView msgTv = tv("« Chaque jour avec toi est une aventure.\nContinue comme ça et les étoiles seront à toi. »", 13, WHITE, Typeface.ITALIC);
        msgTv.setGravity(Gravity.CENTER); msgCard.addView(msgTv);
        TextView sig = tv("— LÉA ✨", 11, DIM2, Typeface.NORMAL);
        sig.setGravity(Gravity.END); sig.setPadding(0, dp(8), 0, 0); msgCard.addView(sig);
        parent.addView(msgCard);
    }

    private View bigStat(String icon, String label, String value, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER); s.setBackgroundColor(CARD);
        s.setPadding(dp(12), dp(16), dp(12), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 22, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(value, 20, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(label, 10, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
