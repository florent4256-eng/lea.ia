package com.flolov42.lea_v3.plus.premium;

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
import java.util.List;

public class LeaMarketplaceDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF020617;
    private static final int CARD  = 0xFF0B1526;
    private static final int CARD2 = 0xFF0F1B2E;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int PURPLE= 0xFF7C3AED;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int RED   = 0xFFEF4444;
    private static final int GOLD  = 0xFFFFD700;
    private static final int PINK  = 0xFFEC4899;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;
    private static final int GLASS_BORDER = 0x1EFFFFFF;

    @Override protected String getFeatureId() { return LeaPlusDatabase.MARKETPLACE; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        int balance = db.getCoinBalance();
        List<LeaPlusDatabase.MarketItem> owned = db.getOwnedItems();

        // ── Hero : solde + possédés ────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));
        GradientDrawable heroGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        heroGd.setCornerRadius(dp(20));
        heroGd.setStroke(dp(1), GLASS_BORDER);
        hero.setElevation(dp(2));
        hero.setBackground(heroGd);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        TextView storeTv = tv("🛒", 40, WHITE, Typeface.NORMAL);
        storeTv.setGravity(Gravity.CENTER); hero.addView(storeTv);

        TextView balTv = tv(balance + " 💰", 28, GOLD, Typeface.BOLD);
        balTv.setGravity(Gravity.CENTER); balTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(balTv);

        TextView ownTv = tv(owned.size() + " item" + (owned.size() != 1 ? "s" : "") + " possédé" + (owned.size() != 1 ? "s" : ""), 11, DIM2, Typeface.NORMAL);
        ownTv.setGravity(Gravity.CENTER); hero.addView(ownTv);
        parent.addView(hero);

        // ── Catégories ────────────────────────────────────────────────────
        Object[][][] catalog = {
            {
                {"🎨", "Thèmes", null, null},
                {"🌌 Galaxie Pro",  "200 💰", "Thème nuit cosmique",          PURPLE},
                {"🌊 Océan Néon",   "150 💰", "Thème bleu aquatique",         CYAN},
                {"🔥 Inferno",      "250 💰", "Thème rouge volcanique",       RED},
                {"🌿 Nature Zen",   "100 💰", "Thème vert apaisant",          GREEN},
            },
            {
                {"🔊", "Sons & Effets", null, null},
                {"🏆 Son de victoire",  "50 💰",  "Effet sonore succès",         GOLD},
                {"⚡ Alerte éclair",    "30 💰",  "Notification rapide",         ORANGE},
                {"🎵 Mélodie Léa",      "80 💰",  "Thème musical exclusif",      PINK},
            },
            {
                {"🏅", "Badges & Prestige", null, null},
                {"👑 Badge Légendaire",  "500 💰", "Badge exclusive rang S",     GOLD},
                {"💎 Badge Diamant",     "300 💰", "Badge rang A+",              CYAN},
                {"🔥 Badge Inferno",     "200 💰", "Badge domination",           RED},
            },
            {
                {"⚡", "Boosts", null, null},
                {"⚡ XP ×2 (1h)",     "150 💰", "Double XP pendant 1 heure",  GREEN},
                {"🪙 Coins ×3 (1h)",  "200 💰", "Triple coins pendant 1 heure",GOLD},
                {"🛡️ Shield Pro",     "100 💰", "Streak protégé 1 jour",      PURPLE},
            },
        };

        for (Object[][] category : catalog) {
            Object[] header = category[0];
            secHeader(parent, header[0] + " " + header[1]);
            LinearLayout catCard = new LinearLayout(this);
            catCard.setOrientation(LinearLayout.VERTICAL);
            catCard.setPadding(dp(14), dp(8), dp(14), dp(8));
            GradientDrawable catGd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ lighten(CARD, 0.06f), CARD });
            catGd.setCornerRadius(dp(18));
            catGd.setStroke(dp(1), GLASS_BORDER);
            catCard.setElevation(dp(2));
            catCard.setBackground(catGd);
            LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(-1, -2);
            ccLp.setMargins(dp(12), dp(4), dp(12), 0); catCard.setLayoutParams(ccLp);

            for (int i = 1; i < category.length; i++) {
                Object[] item = category[i];
                String itemName = (String) item[0];
                String price    = (String) item[1];
                String desc     = (String) item[2];
                int color       = (int) item[3];
                boolean isOwned = isOwned(owned, itemName);
                int priceVal = Integer.parseInt(price.split(" ")[0]);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(8), 0, dp(8));

                View dot = new View(this); dot.setBackgroundColor(color);
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(4), dp(36));
                dLp.setMargins(0, 0, dp(12), 0); dot.setLayoutParams(dLp); row.addView(dot);

                LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
                texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                texts.addView(tv(itemName, 12, isOwned ? DIM2 : WHITE, isOwned ? Typeface.NORMAL : Typeface.BOLD));
                texts.addView(tv(desc, 10, DIM, Typeface.NORMAL));
                row.addView(texts);

                if (isOwned) {
                    row.addView(tv("✓ Acheté", 10, GREEN, Typeface.BOLD));
                } else {
                    Button buyBtn = new Button(this);
                    buyBtn.setText(price);
                    int buyColor = balance >= priceVal ? GOLD : DIM;
                    buyBtn.setTextColor(buyColor);
                    GradientDrawable buyGd = new GradientDrawable();
                    buyGd.setColor((buyColor & 0x00FFFFFF) | 0x22000000);
                    buyGd.setCornerRadius(dp(10));
                    buyGd.setStroke(dp(1), buyColor);
                    buyBtn.setBackground(new RippleDrawable(
                        ColorStateList.valueOf((buyColor & 0x00FFFFFF) | 0x55000000), buyGd, null));
                    buyBtn.setTextSize(10); buyBtn.setTypeface(null, Typeface.BOLD); buyBtn.setAllCaps(false);
                    buyBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
                    buyBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(30)));
                    if (balance >= priceVal) {
                        buyBtn.setOnClickListener(v -> {
                            db.addCoins(-priceVal, "Achat: " + itemName);
                            db.addOwnedItem(itemName);
                            Toast.makeText(this, "✅ " + itemName + " acheté !", Toast.LENGTH_SHORT).show();
                            recreate();
                        });
                    }
                    row.addView(buyBtn);
                }
                catCard.addView(row);
                if (i < category.length - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                    sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); catCard.addView(sp); }
            }
            parent.addView(catCard);
        }

        // Padding bas
        View pad = new View(this);
        pad.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(24)));
        parent.addView(pad);
    }

    private boolean isOwned(List<LeaPlusDatabase.MarketItem> owned, String name) {
        for (LeaPlusDatabase.MarketItem m : owned) if (m.name.equals(name)) return true; return false;
    }
    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
