package com.flolov42.lea_v3.utilities;

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
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traitement du langage naturel sans IA externe.
 * Détecte l'intention, extrait les entités, résout le contexte (pronoms, références).
 * Retourne null si non reconnu → passe à l'IA Ollama.
 */
public class LeaConversationalProcessor {

    private static final String TAG   = "LeaConv";
    private static final String PREFS = "lea_context";

    private final Context         ctx;
    private final SharedPreferences prefs;
    private final LeaMemoryManager memory;

    /** Résultat de l'analyse conversationnelle. */
    public static class Intent {
        public String  type;       // WEATHER, ALARM, CALENDAR, CALL, SMS, SYSTEM, TRANSLATE, SOCIAL, OPEN_APP, CONVERSATIONAL, UNKNOWN
        public String  action;     // sous-action spécifique
        public String  subject;    // entité principale (nom, lieu, app)
        public String  content;    // contenu (message SMS, texte à traduire)
        public int     number;     // valeur numérique (heure, volume%)
        public String  lang;       // langue cible pour traduction
        public boolean isQuestion;
        public int     sentiment;  // LeaSentimentDetector.POSITIVE/NEUTRAL/NEGATIVE

        public Intent(String type) { this.type = type; }
    }

    public LeaConversationalProcessor(Context ctx) {
        this.ctx    = ctx.getApplicationContext();
        this.prefs  = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.memory = new LeaMemoryManager(ctx);
    }

    // ── Analyse principale ────────────────────────────────────────────────────

    public Intent analyze(String input) {
        if (input == null || input.trim().isEmpty()) return null;

        // Résolution contextuelle : remplace "là", "lui", "elle", "ça", etc.
        String resolved = resolvePronouns(input);
        String cmd      = resolved.toLowerCase().trim();

        // Analyse sentiment
        int sentiment = LeaSentimentDetector.analyze(cmd);

        // Sauvegarde dans le contexte
        saveLastInput(cmd);

        Intent intent;

        // ── MÉTÉO ──
        if (matchesAny(cmd, "météo","temps","température","il fait","quel temps","quelle météo","pluie","soleil","neige","vent")) {
            intent = new Intent("WEATHER");
            intent.subject = extractLocation(cmd);
            saveContext("last_location", intent.subject);
        }
        // ── RÉVEIL / MINUTEUR ──
        else if (matchesAny(cmd, "réveil","réveille-moi","mets une alarme","crée un réveil")) {
            intent = new Intent("ALARM");
            intent.action = "SET_ALARM";
            intent.number = extractHour(cmd);
        }
        else if (matchesAny(cmd, "minuteur","timer","chrono","compte à rebours","dans x minutes")) {
            intent = new Intent("ALARM");
            intent.action = "SET_TIMER";
            intent.number = extractDuration(cmd);
        }
        // ── CALENDRIER ──
        else if (matchesAny(cmd, "ajoute","crée un rdv","nouveau rdv","rendez-vous","réunion","meeting","rappelle-moi","événement au calendrier")) {
            intent = new Intent("CALENDAR");
            intent.action = "CREATE";
            intent.subject = cmd;
        }
        else if (matchesAny(cmd, "calendrier","agenda","qu'est-ce que j'ai","mes rdv","mes rendez-vous")) {
            intent = new Intent("CALENDAR");
            intent.action = "READ";
        }
        // ── APPEL ──
        else if (matchesAny(cmd, "appelle","téléphone à","passe un coup de fil","appel à")) {
            intent = new Intent("CALL");
            intent.subject = extractPersonName(cmd);
            saveContext("last_person", intent.subject);
        }
        // ── SMS ──
        else if (matchesAny(cmd, "envoie un sms","envoie un message","message à","sms à","écris à")) {
            intent = new Intent("SMS");
            intent.subject = extractPersonName(cmd);
            intent.content = extractMessageContent(cmd);
            saveContext("last_person", intent.subject);
        }
        // ── CONTRÔLE SYSTÈME ──
        else if (matchesAny(cmd, "luminosité","brightness","volume","son","vibreur","silencieux","wifi","bluetooth","batterie","mode réseau","5g","4g","ne pas déranger","occupé")) {
            intent = new Intent("SYSTEM");
            intent.action = cmd;
        }
        // ── TRADUCTION / DEVISE ──
        else if (matchesAny(cmd, "traduis","traduction","comment dit-on","en anglais","en espagnol","en allemand","convertis","euros en","dollars en","parle-moi en")) {
            intent = new Intent("TRANSLATE");
            intent.subject = cmd;
            intent.lang    = extractTargetLang(cmd);
        }
        // ── RÉSEAUX SOCIAUX ──
        else if (matchesAny(cmd, "instagram","twitter","tweet","whatsapp","telegram","tiktok","snapchat","partage","post sur","lis les comments")) {
            intent = new Intent("SOCIAL");
            intent.action  = cmd;
        }
        // ── OUVRE UNE APP ──
        else if (matchesAny(cmd, "ouvre ","lance ","démarre ","va sur ")) {
            intent = new Intent("OPEN_APP");
            intent.subject = extractAppName(cmd);
        }
        // ── NOTIFICATIONS / RÉSUMÉ ──
        else if (matchesAny(cmd, "résume","résumé","notifications","messages non lus","appels manqués","quoi de neuf","quoi de nouveau","j'ai des messages")) {
            intent = new Intent("SUMMARY");
        }
        // ── QUESTIONS SUR LÉA / SMALLTALK ──
        else if (matchesAny(cmd, "comment tu vas","ça va","tu vas bien","t'es là","tu m'écoutes")) {
            intent = new Intent("CONVERSATIONAL");
            intent.action  = "SMALLTALK";
            intent.content = buildSmallTalkReply(sentiment);
        }
        // ── ÉTATS / CONTEXTE IMPLICITE ──
        else if (matchesAny(cmd, "je suis occupé","ne me dérange pas","pas disponible","réunion")) {
            intent = new Intent("SYSTEM");
            intent.action = "ne pas déranger activé";
        }
        else if (matchesAny(cmd, "j'ai froid","il fait froid")) {
            intent = new Intent("CONVERSATIONAL");
            intent.action  = "CONTEXT_COLD";
            intent.content = "Je suggère de mettre un vêtement chaud ou d'augmenter le chauffage. Veux-tu que j'ouvre les réglages de ta maison connectée ?";
        }
        else if (matchesAny(cmd, "je suis fatigué","j'suis crevé","épuisé")) {
            intent = new Intent("CONVERSATIONAL");
            intent.action  = "CONTEXT_TIRED";
            intent.content = "Tu as besoin de repos. Veux-tu que je mette une musique douce ou que je règle un réveil pour une sieste de 20 minutes ?";
        }
        else if (matchesAny(cmd, "j'ai faim","j'ai soif")) {
            intent = new Intent("CONVERSATIONAL");
            intent.action  = "CONTEXT_HUNGRY";
            intent.content = "Veux-tu que j'ouvre une app de livraison ou que je cherche une recette ?";
        }
        // ── Inconnu → IA ──
        else {
            intent = new Intent("UNKNOWN");
        }

        intent.isQuestion = cmd.contains("?") || cmd.startsWith("quel") || cmd.startsWith("comment")
            || cmd.startsWith("est-ce") || cmd.startsWith("peux-tu") || cmd.startsWith("qu'est");
        intent.sentiment  = sentiment;
        return intent;
    }

    // ── Résolution de pronoms ─────────────────────────────────────────────────

    private String resolvePronouns(String input) {
        String c = input.toLowerCase();
        String lastPerson   = prefs.getString("last_person",   "");
        String lastLocation = prefs.getString("last_location", "");

        // "appelle-le / appelle-la" → dernier contact mentionné
        if (!lastPerson.isEmpty()) {
            c = c.replace("appelle-le", "appelle " + lastPerson)
                 .replace("appelle-la", "appelle " + lastPerson)
                 .replace("envoie-lui", "envoie à " + lastPerson)
                 .replace("lui", lastPerson)
                 .replace("elle", lastPerson);
        }
        // "là-bas", "là" → dernier lieu mentionné
        if (!lastLocation.isEmpty()) {
            c = c.replace("là-bas", lastLocation)
                 .replace("là", lastLocation);
        }
        return c;
    }

    // ── Extraction d'entités ──────────────────────────────────────────────────

    private String extractLocation(String cmd) {
        // "à Paris", "pour Lyon", "sur Nice", "de Marseille"
        Pattern p = Pattern.compile("(?:à|sur|de|pour|en)\\s+([A-ZÀÉÈÊË][a-zàéèêëîïôùûü]+(?:\\s+[A-ZÀÉÈÊË][a-zàéèêëîïôùûü]+)*)");
        Matcher m = p.matcher(cmd);
        if (m.find()) return m.group(1);
        // Dernier lieu connu
        String last = prefs.getString("last_location", "");
        return last.isEmpty() ? "" : last;
    }

    private String extractPersonName(String cmd) {
        // "appelle Marie", "message à Pierre", "à Jean-Pierre"
        Pattern p = Pattern.compile("(?:appelle|à|envoie à|message à|sms à|écris à|pour)\\s+([A-ZÀÉÈÊË][a-zàéèêëîïôùûü]+(?:[- ][A-ZÀÉÈÊË][a-zàéèêëîïôùûü]+)*)");
        Matcher m = p.matcher(cmd);
        if (m.find()) return m.group(1);
        return "";
    }

    private String extractMessageContent(String cmd) {
        // Après ":" ou entre guillemets ou après "pour dire"
        int colon = cmd.indexOf(':');
        if (colon >= 0) return cmd.substring(colon + 1).trim();
        int q1 = cmd.indexOf('"'), q2 = cmd.lastIndexOf('"');
        if (q1 >= 0 && q2 > q1) return cmd.substring(q1 + 1, q2);
        int idx = cmd.indexOf("pour dire ");
        if (idx >= 0) return cmd.substring(idx + 10).trim();
        return "";
    }

    private int extractHour(String cmd) {
        Pattern p = Pattern.compile("(\\d{1,2})\\s*h");
        Matcher m = p.matcher(cmd);
        if (m.find()) return Integer.parseInt(m.group(1));
        Pattern p2 = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher m2 = p2.matcher(cmd);
        if (m2.find()) return Integer.parseInt(m2.group(1));
        for (String part : cmd.split("[^0-9]+")) {
            if (!part.isEmpty()) try { return Integer.parseInt(part); } catch (Exception ignored) {}
        }
        return 7;
    }

    private int extractDuration(String cmd) {
        int n = 5;
        for (String part : cmd.split("[^0-9]+")) {
            if (!part.isEmpty()) try { n = Integer.parseInt(part); break; } catch (Exception ignored) {}
        }
        if (cmd.contains("heure"))                       return n * 3600;
        if (cmd.contains("minute") || cmd.contains("min")) return n * 60;
        return n * 60; // défaut minutes
    }

    private String extractAppName(String cmd) {
        for (String kw : new String[]{ "ouvre ", "lance ", "démarre ", "va sur " }) {
            int idx = cmd.indexOf(kw);
            if (idx >= 0) return cmd.substring(idx + kw.length()).trim().split("\\s+")[0];
        }
        return "";
    }

    private String extractTargetLang(String cmd) {
        if (cmd.contains("anglais"))   return "en";
        if (cmd.contains("espagnol"))  return "es";
        if (cmd.contains("allemand"))  return "de";
        if (cmd.contains("italien"))   return "it";
        if (cmd.contains("arabe"))     return "ar";
        if (cmd.contains("chinois"))   return "zh";
        if (cmd.contains("japonais"))  return "ja";
        return "en";
    }

    // ── Contexte persistant ───────────────────────────────────────────────────

    public void saveContext(String key, String value) {
        if (value != null && !value.isEmpty())
            prefs.edit().putString(key, value).apply();
    }

    public String getContext(String key) {
        return prefs.getString(key, "");
    }

    private void saveLastInput(String input) {
        prefs.edit().putString("last_input", input).apply();
    }

    // ── Small talk ────────────────────────────────────────────────────────────

    private String buildSmallTalkReply(int sentiment) {
        if (sentiment == LeaSentimentDetector.POSITIVE)
            return "Très bien merci ! Content de t'entendre en forme. Que puis-je faire pour toi ?";
        if (sentiment == LeaSentimentDetector.NEGATIVE)
            return "Je suis là pour toi. Dis-moi ce qui ne va pas, je ferai de mon mieux pour t'aider.";
        return "Je suis opérationnel et à ton service ! Que veux-tu faire ?";
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private boolean matchesAny(String cmd, String... keywords) {
        for (String kw : keywords) {
            if (cmd.contains(kw)) return true;
        }
        return false;
    }
}
