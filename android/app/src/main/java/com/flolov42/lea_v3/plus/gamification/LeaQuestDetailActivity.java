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

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

public class LeaQuestDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF020617;
    private static final int CARD  = 0xFF0D1526;
    private static final int CARD2 = 0xFF16233C;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int RED   = 0xFFEF4444;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;
    private static final int GLASS_BORDER = 0x1EFFFFFF;

    @Override protected String getFeatureId() { return LeaPlusDatabase.QUESTS; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);

        List<LeaPlusDatabase.QuestRow> all       = db.getQuests(null);
        List<LeaPlusDatabase.QuestRow> available = db.getQuests("available");
        List<LeaPlusDatabase.QuestRow> completed = db.getQuests("completed");

        // ── Hero stats ─────────────────────────────────────────────────────
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(10), dp(12), 0);
        statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("🎯", String.valueOf(available.size()), "Disponibles", CYAN));
        statsRow.addView(miniStat("✅", String.valueOf(completed.size()), "Terminées", GREEN));
        statsRow.addView(miniStat("📋", String.valueOf(all.size()), "Total", DIM2));
        parent.addView(statsRow);

        // ── Quêtes disponibles ─────────────────────────────────────────────
        secHeader(parent, "🎯 QUÊTES DISPONIBLES");

        if (available.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(glassCard(dp(18), GLASS_BORDER));
            empty.setElevation(dp(2));
            empty.setPadding(dp(24), dp(32), dp(24), dp(32));
            LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(-1, -2);
            eLp.setMargins(dp(12), dp(4), dp(12), 0); empty.setLayoutParams(eLp);
            TextView eIcon = tv("🎯", 40, 0xFF1E3A5F, Typeface.NORMAL);
            eIcon.setGravity(Gravity.CENTER); empty.addView(eIcon);
            TextView eMsg = tv("Active la feature pour\ngénérer des quêtes quotidiennes !", 12, DIM, Typeface.NORMAL);
            eMsg.setGravity(Gravity.CENTER); eMsg.setPadding(0, dp(8), 0, 0); empty.addView(eMsg);
            parent.addView(empty);
        } else {
            int shown = Math.min(available.size(), 5);
            for (int i = 0; i < shown; i++) {
                parent.addView(questCard(available.get(i)));
            }
        }

        // ── Terminées ──────────────────────────────────────────────────────
        if (!completed.isEmpty()) {
            secHeader(parent, "✅ RÉCEMMENT TERMINÉES");
            LinearLayout doneCard = new LinearLayout(this);
            doneCard.setOrientation(LinearLayout.VERTICAL);
            doneCard.setBackground(glassCard(dp(18), GLASS_BORDER));
            doneCard.setElevation(dp(2));
            doneCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams dcLp = new LinearLayout.LayoutParams(-1, -2);
            dcLp.setMargins(dp(12), dp(4), dp(12), dp(8)); doneCard.setLayoutParams(dcLp);
            int shown = Math.min(completed.size(), 5);
            for (int i = 0; i < shown; i++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(5), 0, dp(5));
                row.addView(tv("✅", 14, GREEN, Typeface.NORMAL));
                TextView t = tv(completed.get(i).title, 12, DIM2, Typeface.NORMAL);
                t.setPadding(dp(8), 0, 0, 0);
                t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f)); row.addView(t);
                row.addView(tv("+" + completed.get(i).xp + " XP", 11, GOLD, Typeface.BOLD));
                doneCard.addView(row);
                if (i < shown - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                    sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); doneCard.addView(sp); }
            }
            parent.addView(doneCard);
        }
    }

    private View questCard(LeaPlusDatabase.QuestRow q) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(glassCard(dp(18), GLASS_BORDER));
        card.setElevation(dp(2));
        card.setPadding(dp(14), dp(0), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), 0); card.setLayoutParams(lp);

        // Accent color selon difficulté
        int accentColor = "hard".equals(q.difficulty) ? RED : "medium".equals(q.difficulty) ? ORANGE : GREEN;
        View accent = new View(this);
        accent.setBackgroundColor(accentColor);
        card.addView(accent, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(0, dp(10), 0, 0);

        // Titre + XP
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleTv = tv(q.title, 13, WHITE, Typeface.BOLD);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        topRow.addView(titleTv);

        // Badge difficulté
        String diff = "easy".equals(q.difficulty) ? "FACILE" : "medium".equals(q.difficulty) ? "MOYEN" : "DIFFICILE";
        TextView diffBadge = tv(" " + diff + " ", 8, accentColor, Typeface.BOLD);
        diffBadge.setBackground(pillBg(accentColor, false));
        diffBadge.setPadding(dp(4), dp(2), dp(4), dp(2));
        LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(-2, -2);
        dbLp.setMargins(dp(6), 0, 0, 0); diffBadge.setLayoutParams(dbLp);
        topRow.addView(diffBadge);
        inner.addView(topRow);

        if (q.desc != null && !q.desc.isEmpty()) {
            TextView desc = tv(q.desc, 11, DIM2, Typeface.NORMAL);
            desc.setPadding(0, dp(4), 0, dp(4)); inner.addView(desc);
        }

        // Récompenses
        LinearLayout rewardsRow = new LinearLayout(this);
        rewardsRow.setOrientation(LinearLayout.HORIZONTAL);
        rewardsRow.setGravity(Gravity.CENTER_VERTICAL);
        rewardsRow.setPadding(0, dp(6), 0, 0);
        rewardsRow.addView(tv("+" + q.xp + " XP", 11, GOLD, Typeface.BOLD));
        if (q.coins > 0) {
            TextView coinTv = tv("  ·  +" + q.coins + " 💰", 11, GOLD, Typeface.NORMAL);
            rewardsRow.addView(coinTv);
        }
        rewardsRow.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        if (q.target > 0) {
            LinearLayout progRow = new LinearLayout(this);
            progRow.setOrientation(LinearLayout.HORIZONTAL);
            progRow.setGravity(Gravity.CENTER_VERTICAL);
            progRow.addView(rewardsRow);
            TextView progTv = tv(q.progress + "/" + q.target, 11, CYAN, Typeface.BOLD);
            progRow.addView(progTv);
            inner.addView(progRow);

            float frac = q.target > 0 ? Math.min(1f, (float) q.progress / q.target) : 0;
            FrameLayout bg = new FrameLayout(this);
            bg.setBackgroundColor(0xFF002030);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-1, dp(4));
            bLp.setMargins(0, dp(5), 0, 0); bg.setLayoutParams(bLp);
            View fill = new View(this); fill.setBackgroundColor(accentColor);
            fill.setLayoutParams(new FrameLayout.LayoutParams((int)(frac * 10000), -1));
            bg.addView(fill); inner.addView(bg);
        } else {
            inner.addView(rewardsRow);
        }

        card.addView(inner);
        return card;
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private LinearLayout miniStat(String icon, String val, String lbl, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER); s.setBackground(glassCard(dp(14), GLASS_BORDER));
        s.setElevation(dp(2));
        s.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 18, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(val, 14, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(lbl, 9, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD);
        t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
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
