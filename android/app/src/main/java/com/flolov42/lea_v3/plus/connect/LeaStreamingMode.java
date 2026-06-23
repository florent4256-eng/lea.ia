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


import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class LeaStreamingMode extends LeaBasePlusFeature {

    private static final String PREFS = "lea_streaming";

    // Chat reactions to generate for simulated chat
    private static final String[] CHAT_REACTIONS = {
        "LUL", "PogChamp", "KEKW", "Pog", "GG", "EZ",
        "HeyGuys", "VoHiYo", "PauseChamp", "OMEGALUL"
    };

    private static final String[] STREAM_TIPS = {
        "Varie le rythme — pause après les moments intenses",
        "Engage le chat avec des questions ouvertes",
        "Annonce les prochains streams en fin de session",
        "Mercie les abonnés nominativement — ça fidélise",
        "Utilise des transitions fluides entre les sections",
        "Fais des pauses hors-caméra toutes les 90 minutes",
    };

    public LeaStreamingMode(Context ctx) { super(ctx, LeaPlusDatabase.STREAMING); }

    @Override
    public void execute() {
        checkActiveStream();
        log("📺 Module streaming prêt");
    }

    private void checkActiveStream() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean isLive = prefs.getBoolean("is_live", false);
        if (isLive) {
            long startTime = prefs.getLong("stream_start", System.currentTimeMillis());
            long duration = (System.currentTimeMillis() - startTime) / 60000L;
            // Fatigue warning every 90 min
            if (duration > 0 && duration % 90 == 0) {
                notify("⚠️ Stream LÉA", "Tu stream depuis " + duration + " min — pense à faire une pause !");
            }
            // Stream tip every 30 min
            if (duration > 0 && duration % 30 == 0) {
                String tip = STREAM_TIPS[(int)(duration / 30) % STREAM_TIPS.length];
                log("💡 Tip stream: " + tip);
            }
            log("📺 En live depuis " + duration + " min");
        }
    }

    public void startStream(String platform, String title, String category) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean("is_live", true)
             .putLong("stream_start", System.currentTimeMillis())
             .putString("platform", platform)
             .putString("title", title)
             .apply();
        db.insertStreamSession(platform, title, category);
        notify("🔴 Stream démarré !", platform + " — " + title);
        log("🔴 Stream démarré: " + title + " sur " + platform);
        LeaPlusManager.get(ctx).onTaskCompleted("Stream démarré: " + title, 2);
    }

    public void endStream() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("is_live", false)) return;
        long startTime = prefs.getLong("stream_start", System.currentTimeMillis());
        int durationMin = (int)((System.currentTimeMillis() - startTime) / 60000L);
        String platform = prefs.getString("platform", "");
        String title    = prefs.getString("title", "");

        db.finalizeLastStream(durationMin);
        prefs.edit().putBoolean("is_live", false).apply();

        String recap = generateStreamRecap(title, platform, durationMin);
        notify("⭕ Stream terminé", durationMin + " min de live sur " + platform);
        log("⭕ Stream terminé: " + durationMin + " min");
        LeaPlusManager.get(ctx).onTaskCompleted("Stream terminé: " + durationMin + " min", 3);
    }

    public void generateClip(String moment) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("is_live", false)) {
            log("❌ Clip: aucun stream actif");
            return;
        }
        long streamStart = prefs.getLong("stream_start", System.currentTimeMillis());
        long timestamp = (System.currentTimeMillis() - streamStart) / 1000L;
        db.recordClip(moment, (int) timestamp);
        notify("✂️ Clip marqué !", "Moment sauvegardé à " + formatTime((int)timestamp) + " — \"" + moment + "\"");
        log("✂️ Clip: \"" + moment + "\" @ " + formatTime((int)timestamp));
    }

    public String simulateChatMessage() {
        String[] viewers = {"xX_Gamer_Xx", "TwitchFan42", "ProViewer", "CasualStream", "LéaFan2024"};
        Random rnd = new Random();
        String viewer = viewers[rnd.nextInt(viewers.length)];
        String reaction = CHAT_REACTIONS[rnd.nextInt(CHAT_REACTIONS.length)];
        return viewer + ": " + reaction;
    }

    public String readChatHighlights(int count) {
        List<LeaPlusDatabase.StreamSession> sessions = db.getRecentSessions(1);
        if (sessions.isEmpty()) return "💬 Aucun stream récent";
        StringBuilder sb = new StringBuilder("💬 HIGHLIGHTS CHAT\n\n");
        for (int i = 0; i < count; i++) sb.append("• ").append(simulateChatMessage()).append("\n");
        return sb.toString();
    }

    private String generateStreamRecap(String title, String platform, int durationMin) {
        return "📊 RÉCAP STREAM\n" +
               "Titre: " + title + "\n" +
               "Plateforme: " + platform + "\n" +
               "Durée: " + durationMin + " min\n" +
               "Conseil: " + STREAM_TIPS[durationMin % STREAM_TIPS.length];
    }

    public String getStreamStats() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean live = prefs.getBoolean("is_live", false);
        List<LeaPlusDatabase.StreamSession> sessions = db.getRecentSessions(10);
        int totalMin = 0;
        for (LeaPlusDatabase.StreamSession s : sessions) totalMin += s.durationMin;

        StringBuilder sb = new StringBuilder("📺 STATS STREAMING LÉA\n\n");
        sb.append("Statut: ").append(live ? "🔴 EN LIVE" : "⭕ Hors ligne").append("\n");
        if (live) {
            long start = prefs.getLong("stream_start", System.currentTimeMillis());
            int cur = (int)((System.currentTimeMillis() - start) / 60000L);
            sb.append("Durée en cours: ").append(cur).append(" min\n");
        }
        sb.append("Sessions récentes: ").append(sessions.size()).append("\n");
        sb.append("Temps total: ").append(totalMin / 60).append("h ").append(totalMin % 60).append("min\n\n");
        sb.append("💡 Conseil du moment:\n").append(STREAM_TIPS[sessions.size() % STREAM_TIPS.length]);
        return sb.toString();
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}
