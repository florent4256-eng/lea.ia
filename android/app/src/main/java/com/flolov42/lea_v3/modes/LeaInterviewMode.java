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
import java.util.ArrayList;
import java.util.List;

public class LeaInterviewMode extends LeaBaseMode {

    private static final String PREFS = "lea_interview";

    private static final String[][] QUESTIONS = {
        {"Général",       "Parlez-moi de vous en 2 minutes."},
        {"Général",       "Quelles sont vos plus grandes forces ?"},
        {"Général",       "Décrivez une situation difficile et comment vous l'avez gérée."},
        {"Général",       "Pourquoi voulez-vous ce poste ?"},
        {"Général",       "Où vous voyez-vous dans 5 ans ?"},
        {"Tech",          "Expliquez-moi un projet récent dont vous êtes fier(e)."},
        {"Tech",          "Comment gérez-vous les délais serrés et la pression ?"},
        {"Tech",          "Donnez un exemple de résolution d'un problème complexe."},
        {"Comportemental","Décrivez un conflit avec un collègue et sa résolution."},
        {"Comportemental","Comment priorisez-vous vos tâches en situation de surcharge ?"},
        {"Salarial",      "Quelles sont vos prétentions salariales ?"},
        {"Salarial",      "Êtes-vous ouvert(e) à la négociation ?"},
    };

    private static final String[] FEEDBACK_TEMPLATES = {
        "✅ Bonne réponse STAR (Situation-Tâche-Action-Résultat). Score: %d/10",
        "💡 Conseil: soyez plus concret avec des chiffres et exemples précis. Score: %d/10",
        "⭐ Excellente structure! Vous avez bien mis en valeur vos compétences. Score: %d/10",
        "🔧 À améliorer: manque de lien direct avec le poste visé. Score: %d/10",
    };

    public LeaInterviewMode(Context ctx) { super(ctx, LeaModeDatabase.INTERVIEW); }

    @Override
    public void execute() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int sessions = prefs.getInt("session_count", 0);
        log("📊 " + sessions + " session(s) d'entretien effectuée(s) — Mode actif");
        if (sessions == 0 && !prefs.getBoolean("intro_notif_sent", false)) {
            notify("💼 Simulation Entrevues prête",
                "Ouvre le mode pour simuler ton premier entretien !");
            prefs.edit().putBoolean("intro_notif_sent", true).apply();
        }
    }

    // Démarre une nouvelle session et incrémente le compteur
    public int startSession(String jobType) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int sessions = prefs.getInt("session_count", 0) + 1;
        prefs.edit()
            .putInt("session_count", sessions)
            .putString("current_job", jobType != null ? jobType : "Général")
            .putInt("current_total_score", 0)
            .putInt("current_q_count", 0)
            .putInt("q_index", 0)
            .apply();
        log("💼 Session #" + sessions + " démarrée — poste: " + (jobType != null ? jobType : "Général"));
        return sessions;
    }

    // Évalue une réponse ET accumule le score de la session en cours
    public String evaluateAndAccumulate(String question, String answer) {
        String feedback = evaluateAnswer(question, answer);
        try {
            int score = Integer.parseInt(feedback.split("Score: ")[1].split("/10")[0].trim());
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                .putInt("current_total_score", prefs.getInt("current_total_score", 0) + score)
                .putInt("current_q_count", prefs.getInt("current_q_count", 0) + 1)
                .apply();
        } catch (Exception ignored) {}
        return feedback;
    }

    // Termine la session, la sauvegarde en DB et retourne le bilan
    public String finishSession() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String job     = prefs.getString("current_job", "Général");
        int totalScore = prefs.getInt("current_total_score", 0);
        int qCount     = prefs.getInt("current_q_count", 0);
        if (qCount == 0) return "Aucune question répondue dans cette session.";

        int avgScore = totalScore / qCount;
        String feedback = avgScore >= 8 ? "Excellent niveau ! Tu es prêt(e)."
            : avgScore >= 6 ? "Bon niveau. Travaille la méthode STAR pour les exemples concrets."
            : avgScore >= 4 ? "Niveau correct. Entraîne-toi à structurer tes réponses."
            : "À améliorer. Prépare des exemples précis avec chiffres à l'appui.";

        db.addInterviewSession(job, avgScore, feedback);
        notify("💼 Session terminée", job + " — Score: " + avgScore + "/10");
        log("✅ Session " + job + " terminée — " + qCount + " Q, score moy: " + avgScore + "/10");

        return "🎯 Résultat — " + job + "\n\n"
            + "Questions: " + qCount + "  |  Score moyen: " + avgScore + "/10\n\n"
            + (avgScore >= 8 ? "⭐⭐⭐ " : avgScore >= 6 ? "⭐⭐ " : "⭐ ") + feedback;
    }

    // Historique des sessions sauvegardées en DB
    public String getSessionHistory() {
        List<LeaModeDatabase.InterviewSessionRow> sessions = db.getInterviewSessions(10);
        if (sessions.isEmpty()) return "💼 Aucune session terminée — lance une simulation !";
        StringBuilder sb = new StringBuilder("💼 HISTORIQUE DES SESSIONS:\n\n");
        for (LeaModeDatabase.InterviewSessionRow s : sessions) {
            String stars = s.score >= 8 ? "⭐⭐⭐" : s.score >= 6 ? "⭐⭐" : "⭐";
            sb.append(stars).append(" ").append(s.job)
              .append(" — ").append(s.score).append("/10\n")
              .append("  ").append(s.feedback).append("\n\n");
        }
        return sb.toString().trim();
    }

    public String getNextQuestion(String jobType) {
        List<String> relevant = new ArrayList<>();
        for (String[] q : QUESTIONS) {
            if ("Général".equals(q[0]) || jobType == null || q[0].toLowerCase().contains(jobType.toLowerCase())) {
                relevant.add(q[1]);
            }
        }
        if (relevant.isEmpty()) return "Parlez-moi d'un projet dont vous êtes fier(e).";
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idx = prefs.getInt("q_index", 0);
        String question = relevant.get(idx % relevant.size());
        prefs.edit().putInt("q_index", (idx + 1) % relevant.size()).apply();
        return question;
    }

    public String evaluateAnswer(String question, String answer) {
        if (answer == null || answer.length() < 20) {
            return "❌ Réponse trop courte. Développez avec la méthode STAR. Score: 2/10";
        }

        int score = 5;
        String lower = answer.toLowerCase();
        if (lower.contains("résultat") || lower.contains("résolu") || lower.contains("réussi")) score += 2;
        if (lower.contains("équipe")   || lower.contains("collabor"))                            score += 1;
        if (answer.length() > 100)                                                               score += 1;
        if (lower.contains("%") || lower.contains("€") || lower.contains("jours"))              score += 1;
        score = Math.min(10, score);

        String template = FEEDBACK_TEMPLATES[score >= 8 ? 2 : score >= 6 ? 0 : score >= 4 ? 1 : 3];
        String feedback = String.format(template, score);
        log("💼 Q: \"" + question.substring(0, Math.min(30, question.length())) + "\" | Score: " + score + "/10");
        return feedback;
    }

    public String generateInterviewScript(String job, String company) {
        return "🎯 Script entretien pour: " + job + " @ " + company + "\n\n" +
               "INTRODUCTION (30s):\n" +
               "\"Bonjour, je m'appelle [Nom]. J'ai [X] ans d'expérience en [domaine]...\"\n\n" +
               "FORCE CLÉ:\n\"Ma plus grande force est [compétence] — prouvé par [exemple concret].\"\n\n" +
               "QUESTION RETOURNÉE:\n" +
               "\"Qu'est-ce qui distingue les collaborateurs qui réussissent chez " + company + " ?\"\n\n" +
               "SALAIRE:\n\"J'envisage entre [X]€ et [Y]€ selon les avantages globaux.\"";
    }
}
