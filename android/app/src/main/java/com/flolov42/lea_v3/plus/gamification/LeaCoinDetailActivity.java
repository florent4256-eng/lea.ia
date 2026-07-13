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

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class LeaCoinDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF020617;
    private static final int CARD  = 0xFF0D1526;
    private static final int CARD2 = 0xFF16233C;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;
    private static final int GLASS_BORDER = 0x1EFFFFFF;

    @Override protected String getFeatureId() { return LeaPlusDatabase.COINS; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        int balance = db.getCoinBalance();

        // ── Hero : solde ───────────────────────────────────────────────────
        LinearLayout hero = card(-1, dp(8), dp(12), 0, dp(12));
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(20), dp(24), dp(20), dp(24));

        TextView coinIcon = tv("💰", 52, GOLD, Typeface.NORMAL);
        coinIcon.setGravity(Gravity.CENTER);
        hero.addView(coinIcon);

        TextView balTv = tv(String.valueOf(balance), 42, GOLD, Typeface.BOLD);
        balTv.setGravity(Gravity.CENTER);
        hero.addView(balTv);

        TextView subTv = tv("Léa Coins disponibles", 12, DIM2, Typeface.NORMAL);
        subTv.setGravity(Gravity.CENTER);
        hero.addView(subTv);
        parent.addView(hero);

        // ── Stats rapides ──────────────────────────────────────────────────
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(8), dp(12), 0);
        statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("🏆", String.valueOf(db.getQuests("completed").size()), "Quêtes", GOLD));
        statsRow.addView(miniStat("📅", "Jour " + (db.getCompanionMemoryCount() % 365 + 1), "Série", CYAN));
        statsRow.addView(miniStat("🛒", "4", "Items shop", GREEN));
        parent.addView(statsRow);

        // ── Bonus quotidien ────────────────────────────────────────────────
        secHeader(parent, "🎁 BONUS QUOTIDIEN");
        LinearLayout bonusCard = card(-1, dp(4), dp(12), dp(8), dp(12));
        long lastLogin = db.getLastDailyLogin();
        boolean canClaim = (System.currentTimeMillis() - lastLogin) >= 86400_000L;

        LinearLayout bonusRow = new LinearLayout(this);
        bonusRow.setOrientation(LinearLayout.HORIZONTAL);
        bonusRow.setGravity(Gravity.CENTER_VERTICAL);
        bonusRow.setPadding(0, 0, 0, dp(10));

        View dot = new View(this);
        dot.setBackgroundColor(canClaim ? GREEN : DIM);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotLp.setMargins(0, 0, dp(10), 0); dot.setLayoutParams(dotLp);
        bonusRow.addView(dot);

        TextView bonusTv = tv(canClaim ? "🟢  Bonus disponible ! +50 coins" : "⏳  Déjà réclamé aujourd'hui", 13,
            canClaim ? GREEN : DIM, Typeface.BOLD);
        bonusTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        bonusRow.addView(bonusTv);
        bonusRow.addView(tv("+50 💰", 14, GOLD, Typeface.BOLD));
        bonusCard.addView(bonusRow);

        if (canClaim) {
            Button claimBtn = wideBtn("🎁  RÉCLAMER MON BONUS", GOLD, 0xFF1A1000);
            claimBtn.setOnClickListener(v -> {
                db.addCoins(50, "Bonus quotidien");
                db.setLastDailyLogin(System.currentTimeMillis());
                Toast.makeText(this, "🎉 +50 coins réclamés !", Toast.LENGTH_SHORT).show();
                bonusTv.setText("✅  Bonus réclamé !");
                bonusTv.setTextColor(GREEN);
                claimBtn.setEnabled(false);
                claimBtn.setAlpha(0.4f);
                balTv.setText(String.valueOf(db.getCoinBalance()));
            });
            bonusCard.addView(claimBtn);
        }
        parent.addView(bonusCard);

        // ── Boutique ───────────────────────────────────────────────────────
        secHeader(parent, "🛒 BOUTIQUE");
        String[][] items = {
            {"🌌", "Thème Galaxie Pro",  "200 💰", "Cosmétique"},
            {"🔊", "Son de victoire",    "50 💰",  "Audio"},
            {"🏅", "Badge Légendaire",   "500 💰", "Prestige"},
            {"⚡", "Boost XP ×2 (1h)",  "150 💰", "Boost"},
        };
        LinearLayout shopCard = card(-1, dp(4), dp(12), dp(8), dp(12));
        for (int i = 0; i < items.length; i++) {
            String[] it = items[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            TextView emo = tv(it[0], 22, WHITE, Typeface.NORMAL);
            emo.setMinWidth(dp(36)); row.addView(emo);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            texts.addView(tv(it[1], 13, WHITE, Typeface.BOLD));
            texts.addView(tv(it[3], 10, DIM2, Typeface.NORMAL));
            row.addView(texts);
            row.addView(tv(it[2], 12, GOLD, Typeface.BOLD));
            shopCard.addView(row);
            if (i < items.length - 1) shopCard.addView(sep());
        }
        TextView shopNote = tv("Boutique complète via Léa Marketplace ✨", 10, DIM, Typeface.ITALIC);
        shopNote.setPadding(0, dp(8), 0, 0);
        shopCard.addView(shopNote);
        parent.addView(shopCard);
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private LinearLayout card(int w, int mt, int ml, int mb, int mr) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(glassCard(dp(18), GLASS_BORDER));
        c.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w == -1 ? -1 : w, -2);
        lp.setMargins(ml, mt, mr, mb); c.setLayoutParams(lp);
        c.setPadding(dp(14), dp(12), dp(14), dp(12)); return c;
    }
    private LinearLayout miniStat(String icon, String val, String lbl, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER); s.setBackground(glassCard(dp(14), GLASS_BORDER));
        s.setElevation(dp(2));
        s.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 18, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(val, 13, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(lbl, 9, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }
    private Button wideBtn(String text, int tc, int bg) {
        Button b = new Button(this); b.setText(text); b.setTextColor(tc);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bg);
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), tc);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf((tc & 0x00FFFFFF) | 0x55000000), gd, null));
        b.setElevation(dp(1));
        b.setTextSize(12); b.setTypeface(null, Typeface.BOLD); b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
        lp.setMargins(0, dp(6), 0, 0); b.setLayoutParams(lp); return b;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD);
        t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
    private View sep() {
        View v = new View(this); v.setBackgroundColor(CARD2);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); return v;
    }
    /** Fond glassmorphism arrondi pour cartes locales (dégradé subtil + bordure translucide). */
    private GradientDrawable glassCard(int radius, int borderColor) {
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        gd.setCornerRadius(radius);
        gd.setStroke(dp(1), borderColor);
        return gd;
    }
}
