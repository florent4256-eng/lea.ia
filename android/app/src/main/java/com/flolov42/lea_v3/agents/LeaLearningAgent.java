package com.flolov42.lea_v3.agents;

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
import java.util.List;

public class LeaLearningAgent {

    private static final String ID    = LeaAgentActivationManager.LEARNING;
    private static final String PREFS = "lea_learning";

    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences         prefs;

    // Topic → resource pairs
    private static final String[][] LEARNING_TOPICS = {
        {"programmation", "🐍 Python avancé: decorators et generators", "https://docs.python.org"},
        {"programmation", "☕ Java: design patterns GoF en pratique", "https://refactoring.guru"},
        {"programmation", "🤖 Android: WorkManager et tâches background", "https://developer.android.com"},
        {"ia",            "🧠 Machine Learning: réseaux de neurones CNN", "https://cs231n.github.io"},
        {"ia",            "🤖 LLM: comprendre les transformers", "https://arxiv.org"},
        {"finance",       "💰 Investissement: ETF et diversification", "https://investisseur.fr"},
        {"langue",        "🇬🇧 English: 10 mots business essentiels", "https://learnenglish.org"},
        {"productivite",  "⏱️ Technique Pomodoro: guide complet", "https://francescocirillo.com"},
        {"sante",         "🧬 Nutrition: protéines et performance", "https://examine.com"},
        {"musique",       "🎵 Théorie musicale: accords et progressions", "https://musictheory.net"},
    };

    public LeaLearningAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void execute() {
        try {
            proposeLearningSession();
            trackProgress();
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Apprentissage: " + e.getMessage());
        }
    }

    private void proposeLearningSession() {
        java.util.Calendar cal  = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

        // Only propose during good learning times (8h-10h, 14h-16h, 20h-22h)
        boolean goodTime = (hour >= 8 && hour <= 10) ||
                           (hour >= 14 && hour <= 16) ||
                           (hour >= 20 && hour <= 22);

        if (!goodTime) return;

        // Check not proposed in last 2h
        long lastProposal = prefs.getLong("last_proposal", 0);
        if (System.currentTimeMillis() - lastProposal < 2 * 3600 * 1000L) return;

        String[] interests = getInterests();
        String[] content   = pickContent(interests);

        if (content != null) {
            String msg = "📚 Session 5min: " + content[1];
            db.addLog(ID, msg);
            db.updateLastAction(ID, msg);
            notif.notify(ID, "📚 Micro-apprentissage", msg + "\n" + content[2]);
            prefs.edit().putLong("last_proposal", System.currentTimeMillis()).apply();

            // Increment total sessions
            int total = prefs.getInt("total_sessions", 0) + 1;
            prefs.edit().putInt("total_sessions", total).apply();
        }
    }

    private void trackProgress() {
        int sessions = prefs.getInt("total_sessions", 0);
        int streak   = prefs.getInt("learning_streak", 0);

        long lastStreakDay = prefs.getLong("last_streak_day", 0);
        long todayStart   = getDayStart();
        long dayMs        = 24 * 3600 * 1000L;

        if (todayStart > lastStreakDay) {
            // Consécutif uniquement si le dernier jour de streak était hier
            boolean consecutive = lastStreakDay > 0 && (todayStart - lastStreakDay) <= dayMs + 60_000L;
            streak = consecutive ? streak + 1 : 1;
            prefs.edit()
                .putInt("learning_streak", streak)
                .putLong("last_streak_day", todayStart)
                .apply();
        }

        if (sessions > 0 && sessions % 10 == 0) {
            String msg = "🏆 Bravo! " + sessions + " sessions d'apprentissage complétées!";
            db.addLog(ID, msg);
            notif.notify(ID, "🏆 Milestone!", msg);
        }

        db.updateLastAction(ID, sessions + " sessions • streak: " + streak + " jours");
    }

    private long getDayStart() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String[] getInterests() {
        String saved = prefs.getString("interests", "programmation,ia");
        return saved.split(",");
    }

    public void setInterests(String interests) {
        prefs.edit().putString("interests", interests).apply();
        db.addLog(ID, "📝 Intérêts mis à jour: " + interests);
    }

    private String[] pickContent(String[] interests) {
        // Rotation réelle basée sur le jour de l'année pour varier le contenu
        int dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);

        // Collecter tous les sujets correspondant aux intérêts de l'utilisateur
        java.util.List<String[]> matches = new java.util.ArrayList<>();
        for (String interest : interests) {
            String trimmed = interest.trim().toLowerCase();
            for (String[] topic : LEARNING_TOPICS) {
                if (topic[0].equals(trimmed)) matches.add(topic);
            }
        }

        if (!matches.isEmpty()) return matches.get(dayOfYear % matches.size());

        // Fallback : rotation sur tout le catalogue
        return LEARNING_TOPICS[dayOfYear % LEARNING_TOPICS.length];
    }

    public String getProgressSummary() {
        int sessions = prefs.getInt("total_sessions", 0);
        int streak   = prefs.getInt("learning_streak", 0);
        return sessions + " sessions • " + streak + " jours de streak";
    }

    /** Retourne {topic, titre, url} du contenu du jour selon les intérêts configurés. */
    public String[] getCurrentTopic() {
        return pickContent(getInterests());
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
