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
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaLanguageLearner extends LeaBasePlusFeature {

    private static final String PREFS = "lea_language";

    // [word, translation, phonetic, example]
    private static final String[][] STARTER_VOCAB_EN = {
        {"Hello",       "Bonjour",   "hɛˈloʊ",   "Hello, how are you?"},
        {"Thank you",   "Merci",     "θæŋk juː",  "Thank you very much!"},
        {"Please",      "S'il vous plaît","pliːz","Please help me."},
        {"Sorry",       "Pardon",    "ˈsɒri",     "Sorry, I made a mistake."},
        {"Goodbye",     "Au revoir", "ɡʊdˈbaɪ",  "Goodbye! See you tomorrow."},
        {"Yes",         "Oui",       "jɛs",       "Yes, that's correct."},
        {"No",          "Non",       "noʊ",       "No, I don't think so."},
        {"Water",       "Eau",       "ˈwɔːtər",  "Can I have some water?"},
        {"Help",        "Aide",      "hɛlp",      "I need help!"},
        {"Friend",      "Ami(e)",    "frɛnd",     "You are my best friend."},
    };

    private static final String[][] STARTER_VOCAB_ES = {
        {"Hola",        "Bonjour",   "ˈola",      "Hola, ¿cómo estás?"},
        {"Gracias",     "Merci",     "ˈɡɾasjas",  "Muchas gracias!"},
        {"Por favor",   "S'il vous plaît","poɾ faˈβoɾ","Por favor, ayúdame."},
        {"Lo siento",   "Pardon",    "lo ˈsjento","Lo siento mucho."},
        {"Adiós",       "Au revoir", "aˈðjos",    "Adiós amigo!"},
        {"Amigo",       "Ami",       "aˈmiɣo",    "Eres mi amigo."},
        {"Casa",        "Maison",    "ˈkasa",     "Mi casa es tu casa."},
        {"Agua",        "Eau",       "ˈaɣwa",     "Quiero agua, por favor."},
        {"Tiempo",      "Temps",     "ˈtjempo",   "¿Qué tiempo hace hoy?"},
        {"Comida",      "Nourriture","koˈmiða",   "La comida está deliciosa."},
    };

    private static final String[] FLUENCY_LEVELS = {
        "Débutant A1", "Élémentaire A2", "Intermédiaire B1",
        "Intermédiaire+ B2", "Avancé C1", "Maîtrise C2"
    };

    public LeaLanguageLearner(Context ctx) { super(ctx, LeaPlusDatabase.LANGUAGE); }

    @Override
    public void execute() {
        triggerDailySession();
        updateFluencyLevel();
    }

    private void triggerDailySession() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!today.equals(prefs.getString("last_session", ""))) {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 9 && hour < 20) {
                String lang = prefs.getString("active_lang", "EN");
                int count = db.getVocabCount(lang);
                notify("🌐 Session LÉA Languages", "Révise " + Math.min(count, 10) + " mots en " + lang + " — 5 min suffisent !");
                log("🌐 Session quotidienne déclenchée: " + lang);
                prefs.edit().putString("last_session", today).apply();
            }
        }
    }

    private void updateFluencyLevel() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lang = prefs.getString("active_lang", "EN");
        LeaPlusDatabase.LanguageProgress prog = db.getLanguageProgress(lang);
        if (prog == null) return;
        int levelIdx = Math.min(prog.wordsLearned / 50, FLUENCY_LEVELS.length - 1);
        String level = FLUENCY_LEVELS[levelIdx];
        if (!level.equals(prog.fluencyLevel)) {
            db.updateFluencyLevel(lang, level);
            notify("🏆 Nouveau niveau !", "Tu es maintenant " + level + " en " + getLanguageName(lang) + " !");
            log("🎯 Niveau de langue: " + level + " (" + lang + ")");
        }
    }

    public void seedStarterVocab(String language) {
        String[][] vocab = "ES".equals(language) ? STARTER_VOCAB_ES : STARTER_VOCAB_EN;
        for (String[] w : vocab) {
            db.insertVocab(language, w[0], w[1], w[2], w[3]);
        }
        log("🌐 Vocabulaire de base chargé: " + vocab.length + " mots (" + language + ")");
    }

    public void addVocab(String language, String word, String translation, String phonetic, String example) {
        db.insertVocab(language, word, translation, phonetic, example);
        int total = db.getVocabCount(language);
        log("➕ Mot ajouté: \"" + word + "\" → \"" + translation + "\" | Total: " + total);
        LeaPlusManager.get(ctx).onTaskCompleted("Mot appris: " + word, 1);
    }

    public void markWordLearned(String language, String word) {
        db.markVocabLearned(language, word);
        log("✅ Mot maîtrisé: " + word + " (" + language + ")");
        updateFluencyLevel();
    }

    public String getDailyFlashcard(String language) {
        LeaPlusDatabase.VocabRow next = db.getNextVocab(language);
        if (next == null) return "🌐 Aucun mot à réviser ! Ajoute du vocabulaire.";
        return "🌐 FLASHCARD DU JOUR\n\n" +
               "Mot: " + next.word + "\n" +
               "Prononciation: /" + next.phonetic + "/\n\n" +
               "Traduis en français…\n\n" +
               "💡 Exemple: " + next.example + "\n" +
               "✅ Réponse: " + next.translation;
    }

    public String getProgressReport(String language) {
        LeaPlusDatabase.LanguageProgress prog = db.getLanguageProgress(language);
        int total = db.getVocabCount(language);
        int learned = db.getLearnedVocabCount(language);
        StringBuilder sb = new StringBuilder("🌐 PROGRÈS " + getLanguageName(language) + "\n\n");
        sb.append("Niveau: ").append(prog != null ? prog.fluencyLevel : FLUENCY_LEVELS[0]).append("\n");
        sb.append("Mots: ").append(learned).append(" / ").append(total).append(" appris\n");
        if (total > 0) {
            int pct = (learned * 100) / total;
            sb.append("Progression: ").append(buildBar(pct)).append(" ").append(pct).append("%\n");
        }
        sb.append("\nProchain palier: ").append(learned < 50 ? (50 - learned) + " mots → Élémentaire A2" :
                  learned < 100 ? (100 - learned) + " mots → Intermédiaire B1" : "Continue, tu es sur la bonne voie !");
        return sb.toString();
    }

    private String buildBar(int pct) {
        int filled = pct / 10;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) bar.append(i < filled ? "█" : "░");
        return bar.append("]").toString();
    }

    private String getLanguageName(String code) {
        switch (code) {
            case "EN": return "Anglais";
            case "ES": return "Espagnol";
            case "DE": return "Allemand";
            case "IT": return "Italien";
            case "PT": return "Portugais";
            case "ZH": return "Chinois";
            case "JA": return "Japonais";
            default:   return code;
        }
    }

    public void setActiveLanguage(String language) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString("active_lang", language).apply();
        log("🌐 Langue active: " + getLanguageName(language));
    }
}
