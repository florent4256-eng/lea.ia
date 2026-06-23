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


import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class LeaStudentAssistant extends LeaBasePlusFeature {

    private static final String[][] QUIZ_TEMPLATES = {
        {"Qu'est-ce que %s ?",           "définition"},
        {"Expliquez le concept de %s.",  "explication"},
        {"Quels sont les avantages de %s ?", "analyse"},
        {"Comparez %s et son opposé.",   "comparaison"},
        {"Donnez un exemple de %s.",     "exemple"},
    };

    private static final String[] STUDY_TIPS = {
        "Technique Pomodoro: 25 min de travail + 5 min de pause",
        "Active recall: ferme le cours et tente de résumer",
        "Spaced repetition: relis tes notes à J+1, J+7, J+30",
        "Teach it back: explique la leçon comme si tu l'enseignais",
        "Mind mapping: crée une carte mentale des concepts clés",
    };

    public LeaStudentAssistant(Context ctx) { super(ctx, LeaPlusDatabase.STUDENT); }

    @Override
    public void execute() {
        checkStudyReminders();
        updateAverageGrade();
    }

    private void checkStudyReminders() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour == 18) {
            List<LeaPlusDatabase.SubjectRow> subjects = db.getSubjects();
            if (!subjects.isEmpty()) {
                LeaPlusDatabase.SubjectRow worst = subjects.get(0);
                for (LeaPlusDatabase.SubjectRow s : subjects) if (s.average < worst.average) worst = s;
                String tip = STUDY_TIPS[(int)(System.currentTimeMillis() % STUDY_TIPS.length)];
                notify("📚 Moment d'étudier !", worst.name + " · " + tip);
                log("📚 Rappel étude: " + worst.name);
            }
        }
    }

    private void updateAverageGrade() {
        List<LeaPlusDatabase.SubjectRow> subjects = db.getSubjects();
        if (subjects.isEmpty()) return;
        double totalWeighted = 0, totalCoef = 0;
        for (LeaPlusDatabase.SubjectRow s : subjects) {
            totalWeighted += s.average * s.coef;
            totalCoef += s.coef;
        }
        double avg = totalCoef > 0 ? totalWeighted / totalCoef : 0;
        log(String.format("📊 Moyenne pondérée: %.1f/20", avg));
    }

    public void addSubject(String name, double coefficient) {
        db.insertSubject(name, coefficient);
        log("📚 Matière ajoutée: " + name + " (coef " + coefficient + ")");
    }

    public void addGrade(String subjectName, double grade, double weight) {
        db.insertGrade(subjectName, grade, weight);
        LeaPlusDatabase.SubjectRow s = db.getSubject(subjectName);
        double predicted = predictFinalGrade(s);
        log(String.format("📝 Note: %s → %.1f/20 | Prédict finale: %.1f/20", subjectName, grade, predicted));
        if (predicted >= 16) notify("🌟 " + subjectName, String.format("Excellente note ! Préd. finale: %.1f/20", predicted));
        else if (predicted < 10) notify("⚠️ " + subjectName, String.format("Note en dessous de la moyenne. Préd. finale: %.1f/20", predicted));
    }

    public List<String> generateQuiz(String subject, String topic, int questionCount) {
        List<String> questions = new ArrayList<>();
        for (int i = 0; i < Math.min(questionCount, QUIZ_TEMPLATES.length); i++) {
            String template = QUIZ_TEMPLATES[i % QUIZ_TEMPLATES.length][0];
            questions.add((i + 1) + ". " + String.format(template, topic));
        }
        db.insertQuiz(subject, topic, questions.size());
        log("🧪 Quiz généré: " + questionCount + " questions sur " + topic + " (" + subject + ")");
        return questions;
    }

    public String helpWithHomework(String subject, String question) {
        String lower = question.toLowerCase();
        String advice;
        if (lower.contains("calcul") || lower.contains("équation") || lower.contains("résoudre"))
            advice = "Décompose le problème:\n1. Identifie ce qui est donné\n2. Identifie ce qu'on cherche\n3. Quelle formule relier les deux ?\n4. Applique étape par étape";
        else if (lower.contains("rédige") || lower.contains("explique") || lower.contains("dissertation"))
            advice = "Structure: Introduction (contexte + problématique) → Développement (3 parties) → Conclusion (bilan + ouverture)";
        else if (lower.contains("histoire") || lower.contains("date") || lower.contains("événement"))
            advice = "Méthode: Quoi → Qui → Quand → Où → Pourquoi → Conséquences";
        else
            advice = "Conseil: Relis d'abord le cours, puis tente ta propre réponse. Quelle partie bloque ?";

        log("📖 Aide devoirs: " + subject + " — " + question.substring(0, Math.min(30, question.length())) + "…");
        return "📚 LÉA t'aide en " + subject + ":\n\n" + advice + "\n\n" +
               "💡 Astuce: " + STUDY_TIPS[(int)(System.currentTimeMillis() % STUDY_TIPS.length)];
    }

    public double predictFinalGrade(LeaPlusDatabase.SubjectRow s) {
        if (s == null) return 10.0;
        return s.average;
    }

    public String getStudyReport() {
        List<LeaPlusDatabase.SubjectRow> subjects = db.getSubjects();
        if (subjects.isEmpty()) return "📚 Aucune matière — commence par ajouter tes cours !";
        StringBuilder sb = new StringBuilder("📚 RAPPORT ÉTUDIANT LÉA\n\n");
        double totalWeighted = 0, totalCoef = 0;
        for (LeaPlusDatabase.SubjectRow s : subjects) {
            sb.append("• ").append(s.name).append(": ")
              .append(String.format("%.1f/20", s.average)).append(" (coef ").append(s.coef).append(")\n");
            totalWeighted += s.average * s.coef;
            totalCoef += s.coef;
        }
        double overall = totalCoef > 0 ? totalWeighted / totalCoef : 0;
        sb.append(String.format("\n🎯 Moyenne pondérée: %.2f/20\n", overall));
        sb.append(overall >= 16 ? "👑 Félicitations, excellent niveau !" :
                  overall >= 14 ? "🌟 Très bon niveau !" :
                  overall >= 12 ? "👍 Bon niveau, continue !" :
                  overall >= 10 ? "💪 Passable — tu peux mieux faire !" :
                                  "⚠️ Attention — révisions nécessaires !");
        return sb.toString();
    }
}
