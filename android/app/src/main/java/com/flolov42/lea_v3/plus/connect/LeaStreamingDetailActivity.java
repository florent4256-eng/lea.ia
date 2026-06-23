package com.flolov42.lea_v3.plus.connect;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;
import java.util.Locale;

public class LeaStreamingDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF000D1A;
    private static final int CARD  = 0xFF001A2E;
    private static final int CARD2 = 0xFF00243F;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int PURPLE= 0xFF7C3AED;
    private static final int RED   = 0xFFEF4444;
    private static final int PINK  = 0xFFEC4899;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;

    // {emoji, nom, key, package, url}
    private static final String[][] PLATFORMS = {
        {"▶️", "YouTube Live",   "YouTube Live",   "com.google.android.youtube", "https://studio.youtube.com"},
        {"🟣", "Twitch",         "Twitch",          "tv.twitch.android.app",      "https://twitch.tv"},
        {"📸", "Instagram Live", "Instagram Live",  "com.instagram.android",      "https://instagram.com"},
        {"🎵", "TikTok Live",    "TikTok Live",     "com.zhiliaoapp.musically",   "https://tiktok.com"},
        {"🔵", "Facebook Live",  "Facebook Live",   "com.facebook.katana",        "https://facebook.com/live/producer"},
    };

    private static final String[] STREAM_TIPS = {
        "Varie le rythme — fais une pause après les moments intenses",
        "Engage le chat : pose des questions ouvertes en direct",
        "Annonce tes prochains streams en fin de session",
        "Mercie les abonnés nominativement — ça fidélise",
        "Utilise des transitions fluides entre les sections",
        "Fais des pauses hors-caméra toutes les 90 minutes",
    };

    private LeaStreamingMode streamMode;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private TextView timerTv;

    @Override protected String getFeatureId() { return LeaPlusDatabase.STREAMING; }

    @Override
    protected void buildContent(LinearLayout parent) {
        streamMode   = new LeaStreamingMode(this);
        timerHandler = new Handler(Looper.getMainLooper());

        SharedPreferences prefs = getSharedPreferences("lea_streaming", MODE_PRIVATE);
        boolean isLive       = prefs.getBoolean("is_live", false);
        String  livePlatform = prefs.getString("platform", "");
        String  liveTitle    = prefs.getString("title", "");
        long    streamStart  = prefs.getLong("stream_start", 0L);

        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);

        // ── STATUT LIVE ───────────────────────────────────────────────
        secHeader(parent, "📡 STATUT");
        LinearLayout hero = card(dp(12), dp(4), dp(12), 0);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(16), dp(18), dp(16), dp(18));

        View dot = new View(this);
        dot.setBackgroundColor(isLive ? RED : DIM);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(12), dp(12));
        dotLp.setMargins(0, 0, dp(14), 0); dot.setLayoutParams(dotLp);
        hero.addView(dot);

        LinearLayout heroTexts = new LinearLayout(this);
        heroTexts.setOrientation(LinearLayout.VERTICAL);
        heroTexts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        heroTexts.addView(tv(isLive ? "🔴 EN LIVE" : "⭕ Hors ligne", 15, isLive ? RED : DIM2, Typeface.BOLD));
        if (isLive) {
            heroTexts.addView(tv(livePlatform + (liveTitle.isEmpty() ? "" : "  ·  " + liveTitle), 11, DIM2, Typeface.NORMAL));
            timerTv = tv("00:00:00", 14, CYAN, Typeface.BOLD);
            heroTexts.addView(timerTv);
            startTimer(streamStart);
        } else {
            heroTexts.addView(tv("Prêt à démarrer un stream", 11, DIM, Typeface.NORMAL));
        }
        hero.addView(heroTexts);

        if (isLive) {
            Button stopBtn = new Button(this);
            stopBtn.setText("⏹ TERMINER");
            stopBtn.setTextColor(RED); stopBtn.setBackgroundColor(CARD2);
            stopBtn.setTextSize(10); stopBtn.setTypeface(null, Typeface.BOLD); stopBtn.setAllCaps(false);
            stopBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
            stopBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(36)));
            stopBtn.setOnClickListener(v -> {
                streamMode.endStream();
                stopTimerIfRunning();
                contentArea.removeAllViews();
                buildContent(contentArea);
            });
            hero.addView(stopBtn);
        }
        parent.addView(hero);

        // ── DÉMARRER (hors ligne) ─────────────────────────────────────
        if (!isLive) {
            secHeader(parent, "▶️ DÉMARRER UN STREAM");
            LinearLayout startCard = card(dp(12), dp(4), dp(12), 0);

            final String[] selectedPlatform = {PLATFORMS[0][2]};
            final Button[] platBtns = new Button[PLATFORMS.length];

            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            LinearLayout platRow = new LinearLayout(this);
            platRow.setOrientation(LinearLayout.HORIZONTAL);
            platRow.setPadding(0, 0, 0, dp(10));

            for (int i = 0; i < PLATFORMS.length; i++) {
                final int idx = i;
                Button pb = new Button(this);
                pb.setText(PLATFORMS[i][0] + " " + PLATFORMS[i][1]);
                pb.setTextSize(9); pb.setTypeface(null, Typeface.BOLD); pb.setAllCaps(false);
                pb.setBackgroundColor(i == 0 ? CYAN : CARD2);
                pb.setTextColor(i == 0 ? BG : DIM2);
                pb.setPadding(dp(10), dp(4), dp(10), dp(4));
                LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(-2, dp(30));
                pbLp.setMargins(0, 0, dp(4), 0); pb.setLayoutParams(pbLp);
                platBtns[i] = pb;
                pb.setOnClickListener(v -> {
                    selectedPlatform[0] = PLATFORMS[idx][2];
                    for (int j = 0; j < platBtns.length; j++) {
                        platBtns[j].setBackgroundColor(j == idx ? CYAN : CARD2);
                        platBtns[j].setTextColor(j == idx ? BG : DIM2);
                    }
                });
                platRow.addView(pb);
            }
            hsv.addView(platRow);
            startCard.addView(hsv);

            EditText titleEt = new EditText(this);
            titleEt.setHint("Titre du stream...");
            titleEt.setHintTextColor(DIM);
            titleEt.setTextColor(WHITE);
            titleEt.setTextSize(13);
            titleEt.setBackgroundColor(CARD2);
            titleEt.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(-1, -2);
            etLp.setMargins(0, dp(8), 0, dp(10)); titleEt.setLayoutParams(etLp);
            startCard.addView(titleEt);

            Button startBtn = new Button(this);
            startBtn.setText("▶  DÉMARRER LE STREAM");
            startBtn.setTextColor(BG); startBtn.setBackgroundColor(CYAN);
            startBtn.setTextSize(12); startBtn.setTypeface(null, Typeface.BOLD); startBtn.setAllCaps(false);
            LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(-1, dp(44));
            sbLp.setMargins(0, dp(4), 0, 0); startBtn.setLayoutParams(sbLp);
            startBtn.setOnClickListener(v -> {
                String t = titleEt.getText().toString().trim();
                if (t.isEmpty()) t = "Stream LÉA";
                streamMode.startStream(selectedPlatform[0], t, "Gaming");
                contentArea.removeAllViews();
                buildContent(contentArea);
            });
            startCard.addView(startBtn);
            parent.addView(startCard);
        }

        // ── MARQUER UN CLIP (live) ─────────────────────────────────────
        if (isLive) {
            secHeader(parent, "✂️ CLIP");
            LinearLayout clipCard = card(dp(12), dp(4), dp(12), 0);
            Button clipBtn = new Button(this);
            clipBtn.setText("✂️  MARQUER UN CLIP");
            clipBtn.setTextColor(GOLD); clipBtn.setBackgroundColor(CARD2);
            clipBtn.setTextSize(12); clipBtn.setTypeface(null, Typeface.BOLD); clipBtn.setAllCaps(false);
            clipBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(44)));
            clipBtn.setOnClickListener(v -> {
                EditText et = new EditText(this); et.setHint("ex: Victoire épique !");
                new AlertDialog.Builder(this)
                    .setTitle("✂️ Décris le moment")
                    .setView(et)
                    .setPositiveButton("Marquer", (d, w) -> {
                        String moment = et.getText().toString().trim();
                        if (moment.isEmpty()) moment = "Moment fort";
                        streamMode.generateClip(moment);
                        Toast.makeText(this, "✂️ Clip marqué !", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Annuler", null).show();
            });
            clipCard.addView(clipBtn);
            parent.addView(clipCard);
        }

        // ── OUVRIR UNE PLATEFORME ──────────────────────────────────────
        secHeader(parent, "🎥 OUVRIR UNE PLATEFORME");
        LinearLayout platCard = card(dp(12), dp(4), dp(12), 0);
        for (int i = 0; i < PLATFORMS.length; i++) {
            final String[] p = PLATFORMS[i];
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(8), 0, dp(8));
            row.addView(tv(p[0], 20, WHITE, Typeface.NORMAL));
            LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(10), 0, 0, 0);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            texts.addView(tv(p[1], 12, WHITE, Typeface.BOLD));
            row.addView(texts);
            Button btn = new Button(this); btn.setText("Ouvrir");
            btn.setTextColor(CYAN); btn.setBackgroundColor(CARD2);
            btn.setTextSize(10); btn.setTypeface(null, Typeface.BOLD); btn.setAllCaps(false);
            btn.setPadding(dp(10), dp(4), dp(10), dp(4));
            btn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(30)));
            final String pkg = p[3], url = p[4];
            btn.setOnClickListener(v -> launchApp(pkg, url));
            row.addView(btn);
            platCard.addView(row);
            if (i < PLATFORMS.length - 1) { View sep = new View(this); sep.setBackgroundColor(CARD2);
                sep.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); platCard.addView(sep); }
        }
        parent.addView(platCard);

        // ── HISTORIQUE ─────────────────────────────────────────────────
        List<LeaPlusDatabase.StreamSession> sessions = db.getRecentSessions(5);
        if (!sessions.isEmpty()) {
            secHeader(parent, "📋 SESSIONS RÉCENTES");
            LinearLayout histCard = card(dp(12), dp(4), dp(12), 0);
            for (int i = 0; i < sessions.size(); i++) {
                LeaPlusDatabase.StreamSession s = sessions.get(i);
                LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(6), 0, dp(6));
                LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
                texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                texts.addView(tv(s.platform + (s.title.isEmpty() ? "" : "  ·  " + s.title), 12, WHITE, Typeface.BOLD));
                row.addView(texts);
                int h = s.durationMin / 60, m = s.durationMin % 60;
                String dur = h > 0 ? h + "h" + m + "m" : m + " min";
                row.addView(tv(dur, 11, DIM2, Typeface.NORMAL));
                histCard.addView(row);
                if (i < sessions.size() - 1) { View sep = new View(this); sep.setBackgroundColor(CARD2);
                    sep.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); histCard.addView(sep); }
            }
            parent.addView(histCard);
        }

        // ── CONSEIL ────────────────────────────────────────────────────
        secHeader(parent, "💡 CONSEIL DU MOMENT");
        LinearLayout tipCard = card(dp(12), dp(4), dp(12), dp(24));
        int tipIdx = (int)(System.currentTimeMillis() / 3600_000L) % STREAM_TIPS.length;
        tipCard.addView(tv("💡 " + STREAM_TIPS[tipIdx], 12, DIM2, Typeface.ITALIC));
        parent.addView(tipCard);
    }

    private void startTimer(long streamStart) {
        timerRunnable = new Runnable() {
            @Override public void run() {
                if (timerTv == null) return;
                long elapsed = (System.currentTimeMillis() - streamStart) / 1000L;
                int h = (int)(elapsed / 3600);
                int m = (int)((elapsed % 3600) / 60);
                int s = (int)(elapsed % 60);
                timerTv.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimerIfRunning() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void launchApp(String packageName, String fallbackUrl) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
            if (i != null) { startActivity(i); return; }
        } catch (Exception ignored) {}
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
    }

    private LinearLayout card(int ml, int mt, int mr, int mb) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(CARD); c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(ml, mt, mr, mb); c.setLayoutParams(lp); return c;
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }

    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD);
        t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimerIfRunning();
    }
}
