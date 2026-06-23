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

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaSmartNotifDetailActivity extends LeaFeatureDetailActivity {

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

    @Override protected String getFeatureId() { return LeaPlusDatabase.SMART_NOTIF; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        List<LeaPlusDatabase.SmartNotif> notifs = db.getSmartNotifs(10);

        // ── Hero ───────────────────────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setBackgroundColor(CARD);
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        TextView bellTv = tv("🔔", 40, WHITE, Typeface.NORMAL);
        bellTv.setGravity(Gravity.CENTER); hero.addView(bellTv);

        TextView cntTv = tv(String.valueOf(notifs.size()), 32, CYAN, Typeface.BOLD);
        cntTv.setGravity(Gravity.CENTER); cntTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(cntTv);

        TextView subTv = tv("notification" + (notifs.size() > 1 ? "s" : "") + " intelligente" + (notifs.size() > 1 ? "s" : ""), 12, DIM2, Typeface.NORMAL);
        subTv.setGravity(Gravity.CENTER); hero.addView(subTv);
        parent.addView(hero);

        // ── Canaux activés ────────────────────────────────────────────────
        secHeader(parent, "📡 CANAUX ACTIFS");
        String[][] channels = {
            {"🎯", "Quêtes",       "Nouvelles quêtes disponibles",       String.valueOf(db.isEnabled(LeaPlusDatabase.QUESTS))},
            {"🔗", "Habitudes",    "Rappels de check-in quotidien",       String.valueOf(db.isEnabled(LeaPlusDatabase.HABITS))},
            {"📚", "Étudiant",     "Rappels de révision et sessions",     String.valueOf(db.isEnabled(LeaPlusDatabase.STUDENT))},
            {"⚔️", "Aventure",     "Événements et boss disponibles",      String.valueOf(db.isEnabled(LeaPlusDatabase.ADVENTURE))},
            {"☁️", "Cloud Sync",   "Backup automatique effectué",         String.valueOf(db.isEnabled(LeaPlusDatabase.CLOUD_SYNC))},
        };
        LinearLayout chanCard = new LinearLayout(this);
        chanCard.setOrientation(LinearLayout.VERTICAL);
        chanCard.setBackgroundColor(CARD);
        chanCard.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(-1, -2);
        ccLp.setMargins(dp(12), dp(4), dp(12), 0); chanCard.setLayoutParams(ccLp);
        for (int i = 0; i < channels.length; i++) {
            String[] ch = channels[i];
            boolean active = "true".equals(ch[3]);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(9), 0, dp(9));
            row.addView(tv(ch[0], 18, WHITE, Typeface.NORMAL));
            LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(10), 0, 0, 0);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            texts.addView(tv(ch[1], 12, WHITE, Typeface.BOLD));
            texts.addView(tv(ch[2], 10, DIM2, Typeface.NORMAL));
            row.addView(texts);
            TextView statusTv = tv(active ? "● ON" : "○ OFF", 10, active ? GREEN : DIM, Typeface.BOLD);
            row.addView(statusTv);
            chanCard.addView(row);
            if (i < channels.length - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); chanCard.addView(sp); }
        }
        parent.addView(chanCard);

        // ── Historique ────────────────────────────────────────────────────
        secHeader(parent, "📋 HISTORIQUE RÉCENT");
        if (notifs.isEmpty()) {
            LinearLayout empty = emptyCard("🔕", "Aucune notification", "Les notifications intelligentes\napparaîtront ici");
            parent.addView(empty);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH);
            for (LeaPlusDatabase.SmartNotif n : notifs) {
                parent.addView(notifCard(n, sdf));
            }
        }

        // ── Légende priorités ──────────────────────────────────────────────
        LinearLayout legendRow = new LinearLayout(this);
        legendRow.setOrientation(LinearLayout.HORIZONTAL);
        legendRow.setGravity(Gravity.CENTER_VERTICAL);
        legendRow.setPadding(dp(16), dp(12), dp(16), dp(24));
        String[][] prios = {{"🔴", "Urgent"}, {"🟡", "Normal"}, {"🟢", "Info"}};
        for (String[] p : prios) {
            LinearLayout pi = new LinearLayout(this); pi.setOrientation(LinearLayout.HORIZONTAL);
            pi.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams piLp = new LinearLayout.LayoutParams(-2, -2); piLp.setMargins(0, 0, dp(16), 0); pi.setLayoutParams(piLp);
            pi.addView(tv(p[0], 12, WHITE, Typeface.NORMAL)); pi.addView(tv("  " + p[1], 10, DIM2, Typeface.NORMAL));
            legendRow.addView(pi);
        }
        parent.addView(legendRow);
    }

    private View notifCard(LeaPlusDatabase.SmartNotif n, SimpleDateFormat sdf) {
        int dotColor = "urgent".equals(n.priority) ? 0xFFEF4444 : "normal".equals(n.priority) ? ORANGE : GREEN;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundColor(CARD);
        card.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(3), dp(12), 0); card.setLayoutParams(lp);

        View dot = new View(this); dot.setBackgroundColor(dotColor);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dLp.setMargins(0, 0, dp(12), 0); dot.setLayoutParams(dLp); card.addView(dot);

        LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        texts.addView(tv(n.title, 12, WHITE, Typeface.BOLD));
        texts.addView(tv(n.message, 10, DIM2, Typeface.NORMAL));
        card.addView(texts);
        card.addView(tv(sdf.format(new Date(n.ts)), 9, DIM, Typeface.NORMAL));
        return card;
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
    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
