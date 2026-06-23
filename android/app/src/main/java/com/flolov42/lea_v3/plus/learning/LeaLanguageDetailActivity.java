package com.flolov42.lea_v3.plus.learning;

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

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

public class LeaLanguageDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF000D1A;
    private static final int CARD  = 0xFF001A2E;
    private static final int CARD2 = 0xFF00243F;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int PURPLE= 0xFF7C3AED;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;

    @Override protected String getFeatureId() { return LeaPlusDatabase.LANGUAGE; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        List<LeaPlusDatabase.LanguageRow> langs = db.getLanguages();

        // ── Hero ───────────────────────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setBackgroundColor(CARD);
        hero.setPadding(dp(20), dp(24), dp(20), dp(20));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        TextView globeTv = tv("🌍", 48, WHITE, Typeface.NORMAL);
        globeTv.setGravity(Gravity.CENTER); hero.addView(globeTv);

        TextView cntTv = tv(String.valueOf(langs.size()), 36, CYAN, Typeface.BOLD);
        cntTv.setGravity(Gravity.CENTER); cntTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(cntTv);

        TextView subTv = tv("langue" + (langs.size() > 1 ? "s" : "") + " en apprentissage", 12, DIM2, Typeface.NORMAL);
        subTv.setGravity(Gravity.CENTER); hero.addView(subTv);
        parent.addView(hero);

        // ── Stats ──────────────────────────────────────────────────────────
        int totalWords = 0, totalSessions = 0;
        for (LeaPlusDatabase.LanguageRow l : langs) { totalWords += l.wordsLearned; totalSessions += l.sessionsCount; }
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(8), dp(12), 0); statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("💬", String.valueOf(totalWords), "Mots appris", GREEN));
        statsRow.addView(miniStat("📅", String.valueOf(totalSessions), "Sessions", CYAN));
        statsRow.addView(miniStat("🏅", langs.isEmpty() ? "—" : getLevelLabel(langs.get(0).level), "Meilleur niv.", GOLD));
        parent.addView(statsRow);

        // ── Langues ────────────────────────────────────────────────────────
        secHeader(parent, "🌐 MES LANGUES");
        if (langs.isEmpty()) {
            parent.addView(emptyCard("🌍", "Aucune langue", "Ajoute une langue pour commencer\nton apprentissage avec Léa"));
        } else {
            for (LeaPlusDatabase.LanguageRow l : langs) parent.addView(langCard(l));
        }

        // ── Catalogue ─────────────────────────────────────────────────────
        secHeader(parent, "📚 CATALOGUE DISPONIBLE");
        String[][] catalog = {
            {"🇬🇧", "Anglais",    "English"},
            {"🇪🇸", "Espagnol",   "Español"},
            {"🇩🇪", "Allemand",   "Deutsch"},
            {"🇯🇵", "Japonais",   "日本語"},
            {"🇮🇹", "Italien",    "Italiano"},
            {"🇧🇷", "Portugais",  "Português"},
            {"🇨🇳", "Mandarin",   "中文"},
            {"🇷🇺", "Russe",      "Русский"},
        };
        LinearLayout catCard = new LinearLayout(this);
        catCard.setOrientation(LinearLayout.VERTICAL);
        catCard.setBackgroundColor(CARD);
        catCard.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(-1, -2);
        ccLp.setMargins(dp(12), dp(4), dp(12), dp(24)); catCard.setLayoutParams(ccLp);
        for (int i = 0; i < catalog.length; i++) {
            String[] item = catalog[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            row.addView(tv(item[0], 20, WHITE, Typeface.NORMAL));
            LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(12), 0, 0, 0);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            texts.addView(tv(item[1], 13, WHITE, Typeface.BOLD));
            texts.addView(tv(item[2], 10, DIM2, Typeface.NORMAL));
            row.addView(texts);
            Button startBtn = new Button(this);
            startBtn.setText(isLangAdded(langs, item[1]) ? "✓ Ajouté" : "Démarrer");
            startBtn.setTextColor(isLangAdded(langs, item[1]) ? GREEN : CYAN);
            startBtn.setBackgroundColor(CARD2);
            startBtn.setTextSize(10); startBtn.setTypeface(null, Typeface.BOLD); startBtn.setAllCaps(false);
            startBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
            startBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(30)));
            final String langName = item[1];
            startBtn.setOnClickListener(v -> { db.addLanguage(langName); recreate(); });
            row.addView(startBtn);
            catCard.addView(row);
            if (i < catalog.length - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); catCard.addView(sp); }
        }
        parent.addView(catCard);
    }

    private View langCard(LeaPlusDatabase.LanguageRow l) {
        float pct = Math.min(1f, l.wordsLearned / 2000f);
        int levelColor = l.level.equals("A1") || l.level.equals("A2") ? CYAN :
                         l.level.equals("B1") || l.level.equals("B2") ? GREEN : GOLD;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(CARD);
        card.setPadding(dp(14), dp(0), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), 0); card.setLayoutParams(lp);
        View accent = new View(this); accent.setBackgroundColor(levelColor);
        card.addView(accent, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout inner = new LinearLayout(this); inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(0, dp(10), 0, 0);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView nTv = tv(l.name, 14, WHITE, Typeface.BOLD); nTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f)); row.addView(nTv);
        TextView lvlBadge = tv(" " + l.level + " ", 10, levelColor, Typeface.BOLD);
        lvlBadge.setBackgroundColor(0x22000000 | (levelColor & 0x00FFFFFF));
        lvlBadge.setPadding(dp(5), dp(3), dp(5), dp(3)); row.addView(lvlBadge);
        inner.addView(row);
        inner.addView(tv(l.wordsLearned + " mots  ·  " + l.sessionsCount + " sessions", 10, DIM2, Typeface.NORMAL));
        FrameLayout barBg = new FrameLayout(this); barBg.setBackgroundColor(0xFF002030);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-1, dp(5)); bLp.setMargins(0, dp(8), 0, 0); barBg.setLayoutParams(bLp);
        View fill = new View(this); fill.setBackgroundColor(levelColor);
        fill.setLayoutParams(new FrameLayout.LayoutParams((int)(pct * 10000), -1)); barBg.addView(fill); inner.addView(barBg);
        card.addView(inner);
        return card;
    }

    private String getLevelLabel(String level) {
        if (level == null) return "A1";
        switch (level) { case "C1": case "C2": return "Avancé"; case "B1": case "B2": return "Interméd."; default: return "Débutant"; }
    }
    private boolean isLangAdded(List<LeaPlusDatabase.LanguageRow> langs, String name) {
        for (LeaPlusDatabase.LanguageRow l : langs) if (l.name.equals(name)) return true; return false;
    }
    private LinearLayout emptyCard(String icon, String title, String sub) {
        LinearLayout ll = new LinearLayout(this); ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER); ll.setBackgroundColor(CARD);
        ll.setPadding(dp(24), dp(36), dp(24), dp(36));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), 0); ll.setLayoutParams(lp);
        TextView i = tv(icon, 40, 0xFF1E3A5F, Typeface.NORMAL); i.setGravity(Gravity.CENTER); ll.addView(i);
        TextView t = tv(title, 14, WHITE, Typeface.BOLD); t.setGravity(Gravity.CENTER); t.setPadding(0,dp(8),0,dp(4)); ll.addView(t);
        TextView s = tv(sub, 12, DIM, Typeface.NORMAL); s.setGravity(Gravity.CENTER); ll.addView(s);
        return ll;
    }
    private LinearLayout miniStat(String icon, String val, String lbl, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER); s.setBackgroundColor(CARD);
        s.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 18, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(val, 13, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(lbl, 9, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }
    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
