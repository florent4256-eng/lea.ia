package com.flolov42.lea_v3.plus.gamification;

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

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class LeaAdventureDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF000D1A;
    private static final int CARD  = 0xFF001A2E;
    private static final int CARD2 = 0xFF00243F;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int PURPLE= 0xFF7C3AED;
    private static final int GREEN = 0xFF10B981;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int RED   = 0xFFEF4444;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;

    @Override protected String getFeatureId() { return LeaPlusDatabase.ADVENTURE; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        LeaPlusDatabase.CharStats s = db.getCharStats();

        // ── Hero : niveau & monde ──────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setBackgroundColor(CARD);
        hero.setPadding(dp(20), dp(24), dp(20), dp(20));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0);
        hero.setLayoutParams(hLp);

        String worldEmoji = s.level < 5 ? "🌿" : s.level < 10 ? "🏰" : s.level < 20 ? "🌋" : "⭐";
        TextView worldEmo = tv(worldEmoji, 48, WHITE, Typeface.NORMAL);
        worldEmo.setGravity(Gravity.CENTER); hero.addView(worldEmo);

        TextView lvlTv = tv("Niveau " + s.level, 28, GOLD, Typeface.BOLD);
        lvlTv.setGravity(Gravity.CENTER);
        lvlTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(lvlTv);

        TextView worldTv = tv(s.world, 13, DIM2, Typeface.NORMAL);
        worldTv.setGravity(Gravity.CENTER); hero.addView(worldTv);
        parent.addView(hero);

        // ── XP bar ────────────────────────────────────────────────────────
        LinearLayout xpCard = new LinearLayout(this);
        xpCard.setOrientation(LinearLayout.VERTICAL);
        xpCard.setBackgroundColor(CARD);
        xpCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams xpLp = new LinearLayout.LayoutParams(-1, -2);
        xpLp.setMargins(dp(12), dp(8), dp(12), 0);
        xpCard.setLayoutParams(xpLp);

        LinearLayout xpRow = new LinearLayout(this);
        xpRow.setOrientation(LinearLayout.HORIZONTAL);
        xpRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView xpLabel = tv("✨  Expérience", 12, DIM2, Typeface.NORMAL);
        xpLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        xpRow.addView(xpLabel);
        int pct = s.xpNext > 0 ? (int)((s.xp * 100f) / s.xpNext) : 0;
        xpRow.addView(tv(s.xp + " / " + s.xpNext + "  (" + pct + "%)", 11, CYAN, Typeface.BOLD));
        xpCard.addView(xpRow);

        FrameLayout xpBg = new FrameLayout(this);
        xpBg.setBackgroundColor(0xFF002030);
        LinearLayout.LayoutParams xpBgLp = new LinearLayout.LayoutParams(-1, dp(8));
        xpBgLp.setMargins(0, dp(8), 0, 0); xpBg.setLayoutParams(xpBgLp);
        View xpFill = new View(this);
        xpFill.setBackgroundColor(GOLD);
        xpFill.setLayoutParams(new FrameLayout.LayoutParams(pct * 100, -1));
        xpBg.addView(xpFill); xpCard.addView(xpBg);
        parent.addView(xpCard);

        // ── Stats ──────────────────────────────────────────────────────────
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(4);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(8), dp(12), 0);
        statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("❤️",  String.valueOf(s.hp),           "HP",         RED));
        statsRow.addView(miniStat("⚔️",  String.valueOf(s.bossDefeated), "Boss",       PURPLE));
        statsRow.addView(miniStat("✅",  String.valueOf(s.totalTasks),   "Tâches",     GREEN));
        statsRow.addView(miniStat("💰",  String.valueOf(db.getCoinBalance()), "Coins", GOLD));
        parent.addView(statsRow);

        // ── Prochaine étape ────────────────────────────────────────────────
        secHeader(parent, "🗺️ PROCHAINE ÉTAPE");
        LinearLayout nextCard = new LinearLayout(this);
        nextCard.setOrientation(LinearLayout.VERTICAL);
        nextCard.setBackgroundColor(CARD);
        nextCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(-1, -2);
        nLp.setMargins(dp(12), dp(4), dp(12), 0); nextCard.setLayoutParams(nLp);

        String nextMsg = s.level < 5  ? "🌿 Forêt Sombre — Vaincre 3 boss" :
                         s.level < 10 ? "🏰 Château Brisé — 50 tâches accomplies" :
                         s.level < 20 ? "🌋 Mont Inferno — Dépasser 1000 XP" :
                                        "⭐ Légende accomplie — Tu es au sommet !";
        String nextSub = s.level < 5  ? (3 - Math.min(3, s.bossDefeated)) + " boss restants" :
                         s.level < 10 ? (50 - Math.min(50, s.totalTasks)) + " tâches restantes" :
                         s.level < 20 ? (1000 - Math.min(1000, s.xp)) + " XP restants" : "MAX";

        LinearLayout nextRow = new LinearLayout(this);
        nextRow.setOrientation(LinearLayout.HORIZONTAL);
        nextRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView nextEmo = tv(s.level < 5 ? "🌿" : s.level < 10 ? "🏰" : s.level < 20 ? "🌋" : "⭐", 28, WHITE, Typeface.NORMAL);
        nextEmo.setPadding(0, 0, dp(14), 0); nextRow.addView(nextEmo);
        LinearLayout nextTexts = new LinearLayout(this);
        nextTexts.setOrientation(LinearLayout.VERTICAL);
        nextTexts.addView(tv(nextMsg, 13, WHITE, Typeface.BOLD));
        nextTexts.addView(tv(nextSub, 11, DIM2, Typeface.NORMAL));
        nextRow.addView(nextTexts);
        nextCard.addView(nextRow);

        // Barre de progression vers prochain niveau
        int progPct = s.level < 5 ? (Math.min(3, s.bossDefeated) * 100 / 3) :
                      s.level < 10 ? (Math.min(50, s.totalTasks) * 100 / 50) :
                      s.level < 20 ? (Math.min(1000, s.xp) * 100 / 1000) : 100;
        FrameLayout progBg = new FrameLayout(this);
        progBg.setBackgroundColor(0xFF002030);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(-1, dp(5));
        pbLp.setMargins(0, dp(10), 0, 0); progBg.setLayoutParams(pbLp);
        View progFill = new View(this);
        progFill.setBackgroundColor(CYAN);
        progFill.setLayoutParams(new FrameLayout.LayoutParams(progPct * 100, -1));
        progBg.addView(progFill); nextCard.addView(progBg);
        parent.addView(nextCard);
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private LinearLayout miniStat(String icon, String val, String lbl, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER); s.setBackgroundColor(CARD);
        s.setPadding(dp(6), dp(10), dp(6), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 16, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(val, 14, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(lbl, 9, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD);
        t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
