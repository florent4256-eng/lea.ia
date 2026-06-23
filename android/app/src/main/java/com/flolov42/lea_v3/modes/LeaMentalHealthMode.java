package com.flolov42.lea_v3.modes;

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

public class LeaMentalHealthMode extends LeaBaseMode {

    private static final String PREFS = "lea_mental_health";

    private static final String[] STRESS_KEYWORDS = {
        "épuisé", "fatigué", "stressé", "débordé", "angoisse",
        "panique", "je n'en peux plus", "ça va pas", "déprimé", "anxieux",
        "exhausted", "stressed", "overwhelmed", "anxious", "depressed"
    };

    private static final String[] POSITIVE_KEYWORDS = {
        "super", "génial", "content", "heureux", "motivé",
        "enthousiaste", "fier", "accompli", "bien", "great", "happy"
    };

    private static final String[][] EXERCISES = {
        {"Respiration 4-7-8",  "Inspire 4s → retiens 7s → expire 8s. Répète 4 fois."},
        {"Ancrage 5-4-3-2-1",  "Nomme 5 choses vues, 4 entendues, 3 touchées, 2 senties, 1 goûtée."},
        {"Body Scan",          "Ferme les yeux. Scanne ton corps de la tête aux pieds. Relâche les tensions."},
        {"Gratitude x3",       "Écris 3 choses pour lesquelles tu es reconnaissant(e) aujourd'hui."},
        {"Mouvement 5min",     "Lève-toi, étire-toi, marche 5 minutes. Le corps aide le mental."}
    };

    public LeaMentalHealthMode(Context ctx) { super(ctx, LeaModeDatabase.MENTAL_HEALTH); }

    @Override
    public void execute() {
        checkMoodPattern();
        proposeExercise();
    }

    private void checkMoodPattern() {
        int avg = db.getAverageMoodLastWeek();
        String trend;
        if (avg >= 70)      trend = "😊 Excellente semaine (score moy: " + avg + "/100)";
        else if (avg >= 50) trend = "😐 Semaine correcte (score moy: " + avg + "/100)";
        else                trend = "😔 Semaine difficile (score moy: " + avg + "/100) — pense à te reposer";
        log(trend);

        if (avg < 40) {
            notify("🧠 Thérapie Vocale — attention à toi",
                "Ton humeur semble basse cette semaine. Un exercice de respiration ?");
        }
    }

    private void proposeExercise() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lastEx = prefs.getLong("last_exercise", 0);
        long now = System.currentTimeMillis();
        if (now - lastEx < 4 * 3600_000L) return; // max 1 exercise every 4h

        // Rotation par tranche de 4h du jour (0-4h=ex0, 4-8h=ex1, etc.)
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int idx  = (hour / 4) % EXERCISES.length;
        String[] ex = EXERCISES[idx];
        log("💆 Exercice proposé: " + ex[0]);
        notify("💆 " + ex[0], ex[1]);
        prefs.edit().putLong("last_exercise", now).apply();
    }

    // Analyse le texte entré pour détecter l'état émotionnel
    public String analyzeText(String text) {
        if (text == null || text.isEmpty()) return "Raconte-moi comment tu te sens…";
        String lower = text.toLowerCase();
        int stressScore = 0, positiveScore = 0;
        for (String k : STRESS_KEYWORDS)   if (lower.contains(k)) stressScore++;
        for (String k : POSITIVE_KEYWORDS) if (lower.contains(k)) positiveScore++;

        int moodScore;
        String response;
        if (stressScore > positiveScore) {
            moodScore = Math.max(20, 50 - stressScore * 10);
            response  = "Je sens que tu traverses quelque chose de difficile. " +
                        EXERCISES[stressScore % EXERCISES.length][1];
        } else if (positiveScore > 0) {
            moodScore = Math.min(90, 60 + positiveScore * 10);
            response  = "Tu sembles dans un bon état d'esprit ! Continue comme ça 🌟";
        } else {
            moodScore = 55;
            response  = "Comment puis-je t'aider aujourd'hui ?";
        }

        db.addMood(moodScore, text.substring(0, Math.min(50, text.length())));
        log("🎭 Analyse: stress=" + stressScore + " positif=" + positiveScore + " score=" + moodScore);
        return response;
    }

    public void recordMood(int score, String note) {
        db.addMood(score, note);
        log("📊 Humeur enregistrée: " + score + "/100 — " + note);
    }

    // Historique des humeurs enregistrées — consultable depuis l'UI
    public String getMoodHistorySummary() {
        List<LeaModeDatabase.MoodRow> history = db.getMoodHistory(14);
        if (history.isEmpty()) return "😐 Aucun enregistrement — analyse un texte ou utilise 'Enregistrer l'humeur'.";

        int total = 0;
        for (LeaModeDatabase.MoodRow r : history) total += r.score;
        int avg = total / history.size();
        String trend = avg >= 70 ? "😊 Bonne période" : avg >= 50 ? "😐 Période correcte" : "😔 Période difficile";

        StringBuilder sb = new StringBuilder("📊 HISTORIQUE HUMEUR (" + history.size() + " entrées)\n\n");
        sb.append("Moyenne: ").append(avg).append("/100 — ").append(trend).append("\n\n");
        int shown = 0;
        for (LeaModeDatabase.MoodRow r : history) {
            if (shown++ >= 7) break;
            String icon = r.score >= 70 ? "😊" : r.score >= 50 ? "😐" : "😔";
            sb.append(icon).append(" ").append(r.score).append("/100");
            if (r.note != null && !r.note.isEmpty())
                sb.append("  ·  ").append(r.note.length() > 30 ? r.note.substring(0, 30) + "…" : r.note);
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    // Exercice ciblé selon le niveau de stress actuel
    public String getExerciseForLevel(int stressLevel) {
        int idx = stressLevel > 70 ? 0 : stressLevel > 50 ? 2 : 3;
        return "💆 " + EXERCISES[idx][0] + "\n\n" + EXERCISES[idx][1];
    }
}
