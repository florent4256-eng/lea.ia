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

/**
 * Analyse de communication textuelle — détecte stress, humeur et patterns de déception
 * à partir de transcriptions ou de messages tapés. Ne fait pas d'analyse audio directe
 * (Android 13+ impose des restrictions sur l'enregistrement en arrière-plan).
 */
public class LeaVoiceBiometricMode extends LeaBaseMode {

    private static final String PREFS = "lea_voice_bio";

    public LeaVoiceBiometricMode(Context ctx) { super(ctx, LeaModeDatabase.VOICE_BIO); }

    @Override
    public void execute() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int totalAnalyses = prefs.getInt("analyses_count", 0);

        // Bilan journalier une fois par jour, seulement si des analyses ont eu lieu
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (!today.equals(prefs.getString("last_summary_day", ""))) {
            int todayCount = prefs.getInt("today_analyses", 0);
            if (todayCount > 0) {
                float avgStress = Float.intBitsToFloat(
                    prefs.getInt("today_avg_stress_bits", Float.floatToIntBits(0f)));
                String stressLabel = avgStress > 70 ? "⚠️ Élevé"
                    : avgStress > 40 ? "😐 Modéré" : "✅ Faible";
                notify("🎙️ Bilan Communication",
                    todayCount + " analyse(s) aujourd'hui · Stress moyen: " + stressLabel
                    + "\nOuvre l'app pour voir ton profil de communication.");
                log("📊 Bilan: " + todayCount + " analyses, stress moy=" + (int) avgStress + "%");
            }
            // Réinitialiser les compteurs journaliers
            prefs.edit()
                .putString("last_summary_day", today)
                .putInt("today_analyses", 0)
                .putInt("today_avg_stress_bits", Float.floatToIntBits(0f))
                .apply();
        }

        log("🎙️ " + totalAnalyses + " analyse(s) au total — Mode actif");
    }

    // Analyse un texte (transcription vocale ou message tapé) et retourne un profil de communication
    public VoiceAnalysis analyzeVoiceText(String transcription) {
        if (transcription == null || transcription.isEmpty()) {
            return new VoiceAnalysis(50, 50, "Neutre", "Parle pour que je t'analyse !");
        }

        String lower = transcription.toLowerCase();
        VoiceAnalysis result = new VoiceAnalysis();

        // Stress : hésitations, surcharge de points d'exclamation, ellipses
        int stressWords = 0;
        String[] stressIndicators = {"pas sûr", "je sais pas", "euh", "hm", "peut-être", "hésit",
            "je sais plus", "j'arrive pas", "débordé", "pas le temps", "stressé", "angoisse"};
        for (String s : stressIndicators) if (lower.contains(s)) stressWords++;
        int exclamations = countChar(transcription, '!');
        int ellipses    = transcription.split("\\.{3}").length - 1;
        int questionOverload = transcription.split("\\?").length > 4 ? 2 : 0;
        result.stressLevel = Math.min(100, 25 + stressWords * 10 + exclamations * 5
            + ellipses * 5 + questionOverload * 5);

        // Déception : sur-justification — souvent signe d'inconfort ou de recherche de validation
        String[] lieMarkers = {"franchement", "sincèrement", "je te jure", "crois-moi",
            "honnêtement", "je promets", "vraiment", "sans mentir"};
        int lieCount = 0;
        for (String m : lieMarkers) if (lower.contains(m)) lieCount++;
        result.deceptionScore = Math.min(100, lieCount * 20);

        // Humeur dominante
        String[] joyWords   = {"super", "génial", "content", "heureux", "excellent", "trop bien",
            "j'adore", "cool", "parfait", "incroyable"};
        String[] sadWords   = {"triste", "déprimé", "pas bien", "mal", "nul", "ça va pas",
            "fatiguée", "épuisé", "découragé"};
        String[] angryWords = {"énervé", "furieux", "insupportable", "ras-le-bol", "en colère",
            "agacé", "frustré", "je supporte plus"};
        int joy=0, sad=0, angry=0;
        for (String w : joyWords)   if (lower.contains(w)) joy++;
        for (String w : sadWords)   if (lower.contains(w)) sad++;
        for (String w : angryWords) if (lower.contains(w)) angry++;

        if      (joy > sad && joy > angry)   result.mood = "😊 Positif";
        else if (sad > joy && sad > angry)   result.mood = "😔 Mélancolique";
        else if (angry > 0)                  result.mood = "😠 Frustré";
        else                                 result.mood = "😐 Neutre";

        // Conseil personnalisé
        if (result.stressLevel > 70)
            result.advice = "⚠️ Stress élevé détecté. Essaie la respiration 4-7-8 (inspire 4s, retiens 7s, expire 8s).";
        else if (result.deceptionScore > 60)
            result.advice = "🔍 Sur-justification détectée — sois plus direct(e) et confiant(e) dans ton discours.";
        else if ("😔 Mélancolique".equals(result.mood))
            result.advice = "💙 Tu sembles un peu bas(se). Une petite marche de 10 min peut changer ton état.";
        else if ("😠 Frustré".equals(result.mood))
            result.advice = "😤 Frustration détectée. Identifie ce qui ne dépend PAS de toi — lâche ça.";
        else
            result.advice = "✅ Communication claire et équilibrée. Bonne énergie !";

        // Mise à jour des statistiques journalières
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int newTotal = prefs.getInt("analyses_count", 0) + 1;
        int todayCount = prefs.getInt("today_analyses", 0) + 1;
        float prevAvg = Float.intBitsToFloat(
            prefs.getInt("today_avg_stress_bits", Float.floatToIntBits(0f)));
        float newAvg = (prevAvg * (todayCount - 1) + result.stressLevel) / todayCount;

        prefs.edit()
            .putInt("analyses_count", newTotal)
            .putInt("today_analyses", todayCount)
            .putInt("today_avg_stress_bits", Float.floatToIntBits(newAvg))
            .apply();

        log("🎙️ Analyse #" + newTotal + ": stress=" + result.stressLevel
            + "% humeur=" + result.mood + " déception=" + result.deceptionScore + "%");
        return result;
    }

    // Résumé des 7 derniers jours basé sur les logs
    public String getWeeklyReport() {
        java.util.List<LeaModeDatabase.LogRow> logs = db.getLogs(LeaModeDatabase.VOICE_BIO, 100);
        if (logs.isEmpty()) return "Aucune analyse enregistrée pour l'instant.";

        int count = 0, stressSum = 0;
        for (LeaModeDatabase.LogRow row : logs) {
            if (row.msg != null && row.msg.contains("stress=")) {
                try {
                    String part = row.msg.split("stress=")[1].split("%")[0];
                    stressSum += Integer.parseInt(part);
                    count++;
                } catch (Exception ignored) {}
            }
        }

        if (count == 0) return "Pas encore assez de données pour un rapport.";
        int avgStress = stressSum / count;
        String trend = avgStress > 65 ? "⚠️ Semaine stressante"
            : avgStress > 40 ? "😐 Semaine normale"
            : "✅ Bonne semaine — stress maîtrisé";

        return "📊 Rapport 7 jours (" + count + " analyses)\n"
            + "Stress moyen: " + avgStress + "%\n"
            + trend + "\n"
            + (avgStress > 65 ? "Conseil: identifie la principale source de stress et délègue ou reporte." : "Continue comme ça !");
    }

    // Profil de communication agrégé sur toutes les analyses passées
    public String getCommunicationProfile() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int totalAnalyses = prefs.getInt("analyses_count", 0);
        if (totalAnalyses < 5)
            return "🎙️ Pas encore assez d'analyses (" + totalAnalyses + "/5 min).\nEnvoie du texte à analyser pour construire ton profil !";

        // Extraire stress et humeur depuis les logs
        java.util.List<LeaModeDatabase.LogRow> logs = db.getLogs(LeaModeDatabase.VOICE_BIO, 50);
        int stressSum = 0, stressCount = 0;
        int joyCount = 0, sadCount = 0, angryCount = 0;

        for (LeaModeDatabase.LogRow row : logs) {
            if (row.msg == null) continue;
            if (row.msg.contains("stress=")) {
                try {
                    int s = Integer.parseInt(row.msg.split("stress=")[1].split("%")[0].trim());
                    stressSum += s; stressCount++;
                } catch (Exception ignored) {}
            }
            if (row.msg.contains("humeur=")) {
                if (row.msg.contains("Positif"))     joyCount++;
                else if (row.msg.contains("Mélancolique")) sadCount++;
                else if (row.msg.contains("Frustré"))     angryCount++;
            }
        }

        String stressLabel, moodLabel, advice;
        if (stressCount > 0) {
            int avg = stressSum / stressCount;
            stressLabel = avg > 65 ? "Élevé (" + avg + "%) ⚠️"
                : avg > 40 ? "Modéré (" + avg + "%) 😐"
                : "Maîtrisé (" + avg + "%) ✅";
            advice = avg > 65 ? "Conseil: pratique la respiration 4-7-8 chaque matin."
                : avg > 40 ? "Bien. Maintiens une routine de récupération."
                : "Excellent équilibre ! Continue.";
        } else {
            stressLabel = "Données insuffisantes";
            advice = "Continue à analyser pour obtenir un profil précis.";
        }

        int total = joyCount + sadCount + angryCount;
        if (total > 0) {
            if (joyCount >= sadCount && joyCount >= angryCount)   moodLabel = "Positif 😊 (" + pct(joyCount, total) + "%)";
            else if (sadCount >= joyCount && sadCount >= angryCount) moodLabel = "Mélancolique 😔 (" + pct(sadCount, total) + "%)";
            else                                                       moodLabel = "Frustré 😠 (" + pct(angryCount, total) + "%)";
        } else {
            moodLabel = "Neutre 😐";
        }

        return "🎙️ TON PROFIL DE COMMUNICATION\n\n"
            + "📊 Analyses effectuées: " + totalAnalyses + "\n"
            + "🧠 Stress moyen: " + stressLabel + "\n"
            + "😊 Humeur dominante: " + moodLabel + "\n\n"
            + advice;
    }

    private int pct(int part, int total) {
        return total == 0 ? 0 : (int)(100.0 * part / total);
    }

    private int countChar(String s, char ch) {
        int count = 0; for (char c : s.toCharArray()) if (c == ch) count++; return count;
    }

    public static class VoiceAnalysis {
        public int    stressLevel, deceptionScore;
        public String mood, advice;
        VoiceAnalysis() {}
        VoiceAnalysis(int s, int d, String m, String a) {
            stressLevel=s; deceptionScore=d; mood=m; advice=a;
        }
    }
}
