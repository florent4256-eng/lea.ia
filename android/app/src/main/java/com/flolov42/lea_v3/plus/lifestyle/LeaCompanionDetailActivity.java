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
import java.util.List;

public class LeaCompanionDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG     = 0xFF000D1A;
    private static final int CARD   = 0xFF001A2E;
    private static final int CARD2  = 0xFF00243F;
    private static final int CYAN   = 0xFF00E5FF;
    private static final int PURPLE = 0xFF7C3AED;
    private static final int GREEN  = 0xFF10B981;
    private static final int PINK   = 0xFFEC4899;
    private static final int GOLD   = 0xFFFFD700;
    private static final int WHITE  = 0xFFFFFFFF;
    private static final int DIM    = 0xFF64748B;
    private static final int DIM2   = 0xFF94A3B8;

    @Override protected String getFeatureId() { return LeaPlusDatabase.COMPANION; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        int memoryCount = db.getCompanionMemoryCount();
        List<LeaPlusDatabase.CompanionMemory> recent = db.getRecentMemories(5);

        // Niveau de lien
        String bondLevel, bondEmoji; int bondColor; int bondTarget; float bondPct;
        if (memoryCount < 10)      { bondLevel = "Nouveaux amis";   bondEmoji = "👋"; bondColor = DIM2;   bondTarget = 10;  }
        else if (memoryCount < 50) { bondLevel = "Familiers";       bondEmoji = "🤝"; bondColor = CYAN;   bondTarget = 50;  }
        else if (memoryCount < 200){ bondLevel = "Complices";       bondEmoji = "💙"; bondColor = 0xFF2196F3; bondTarget = 200; }
        else                       { bondLevel = "Âmes sœurs ✨";   bondEmoji = "💜"; bondColor = PURPLE; bondTarget = 200; }
        int prev = memoryCount < 10 ? 0 : memoryCount < 50 ? 10 : memoryCount < 200 ? 50 : 0;
        bondPct = bondTarget > 0 ? Math.min(1f, (float)(memoryCount - prev) / (bondTarget - prev)) : 1f;

        // ── Hero : lien ────────────────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setBackgroundColor(CARD);
        hero.setPadding(dp(20), dp(24), dp(20), dp(20));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        TextView emoTv = tv(bondEmoji, 48, WHITE, Typeface.NORMAL);
        emoTv.setGravity(Gravity.CENTER); hero.addView(emoTv);

        TextView bondTv = tv(bondLevel, 20, bondColor, Typeface.BOLD);
        bondTv.setGravity(Gravity.CENTER); bondTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(bondTv);

        TextView memTv = tv(memoryCount + " souvenirs partagés", 11, DIM2, Typeface.NORMAL);
        memTv.setGravity(Gravity.CENTER); hero.addView(memTv);

        // Barre de lien
        FrameLayout barBg = new FrameLayout(this);
        barBg.setBackgroundColor(0xFF002030);
        LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(-1, dp(6));
        bbLp.setMargins(dp(20), dp(12), dp(20), 0); barBg.setLayoutParams(bbLp);
        View fill = new View(this); fill.setBackgroundColor(bondColor);
        fill.setLayoutParams(new FrameLayout.LayoutParams((int)(bondPct * 10000), -1));
        barBg.addView(fill); hero.addView(barBg);

        String nextLabel = memoryCount < 10 ? (10 - memoryCount) + " souvenirs → Familiers" :
                           memoryCount < 50 ? (50 - memoryCount) + " souvenirs → Complices" :
                           memoryCount < 200 ? (200 - memoryCount) + " souvenirs → Âmes sœurs" : "Niveau maximum ✨";
        TextView nextTv = tv(nextLabel, 10, DIM, Typeface.NORMAL);
        nextTv.setGravity(Gravity.CENTER); nextTv.setPadding(0, dp(6), 0, 0); hero.addView(nextTv);
        parent.addView(hero);

        // ── Souvenirs récents ──────────────────────────────────────────────
        secHeader(parent, "🧠 SOUVENIRS RÉCENTS");
        if (recent.isEmpty()) {
            LinearLayout empty = emptyState("🌟", "Aucun souvenir encore",
                "Parle plus souvent à Léa pour\nqu'elle apprenne à te connaître !");
            parent.addView(empty);
        } else {
            for (LeaPlusDatabase.CompanionMemory mem : recent) {
                parent.addView(memoryCard(mem));
            }
        }

        // ── Message de Léa ─────────────────────────────────────────────────
        secHeader(parent, "💬 LÉA T'ÉCRIT");
        LinearLayout msgCard = new LinearLayout(this);
        msgCard.setOrientation(LinearLayout.HORIZONTAL);
        msgCard.setGravity(Gravity.CENTER_VERTICAL);
        msgCard.setBackgroundColor(CARD);
        msgCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(-1, -2);
        mLp.setMargins(dp(12), dp(4), dp(12), dp(24)); msgCard.setLayoutParams(mLp);

        TextView leaAvatar = tv("🤖", 28, WHITE, Typeface.NORMAL);
        leaAvatar.setPadding(0, 0, dp(12), 0); msgCard.addView(leaAvatar);

        String msg = memoryCount == 0
            ? "Bonjour ! Je suis Léa. Parle-moi de toi, j'apprends vite 💙"
            : "J'ai " + memoryCount + " souvenirs de toi. Chaque échange nous rapproche ✨";
        TextView msgTv = tv(msg, 12, WHITE, Typeface.NORMAL);
        msgTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        msgCard.addView(msgTv);
        parent.addView(msgCard);
    }

    private View memoryCard(LeaPlusDatabase.CompanionMemory mem) {
        String emoji = "joie".equals(mem.emotion) ? "😄" : "tristesse".equals(mem.emotion) ? "😔" :
                       "colere".equals(mem.emotion) ? "😠" : "😊";
        int emotionColor = "joie".equals(mem.emotion) ? GOLD : "tristesse".equals(mem.emotion) ? 0xFF64B5F6 :
                           "colere".equals(mem.emotion) ? 0xFFEF4444 : CYAN;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundColor(CARD);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(3), dp(12), 0); card.setLayoutParams(lp);

        // Dot couleur émotion
        View dot = new View(this);
        dot.setBackgroundColor(emotionColor);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(4), dp(40));
        dLp.setMargins(0, 0, dp(12), 0); dot.setLayoutParams(dLp);
        card.addView(dot);

        TextView emoTv = tv(emoji, 20, WHITE, Typeface.NORMAL);
        emoTv.setPadding(0, 0, dp(10), 0); card.addView(emoTv);

        TextView content = tv(mem.content, 12, WHITE, Typeface.NORMAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(content);
        return card;
    }

    private LinearLayout emptyState(String icon, String title, String sub) {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setBackgroundColor(CARD);
        ll.setPadding(dp(24), dp(36), dp(24), dp(36));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), 0); ll.setLayoutParams(lp);
        TextView iTv = tv(icon, 40, 0xFF1E3A5F, Typeface.NORMAL); iTv.setGravity(Gravity.CENTER); ll.addView(iTv);
        TextView tTv = tv(title, 14, WHITE, Typeface.BOLD); tTv.setGravity(Gravity.CENTER);
        tTv.setPadding(0, dp(8), 0, dp(4)); ll.addView(tTv);
        TextView sTv = tv(sub, 12, DIM, Typeface.NORMAL); sTv.setGravity(Gravity.CENTER); ll.addView(sTv);
        return ll;
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
