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


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LeaSentimentDetector {

    public static final int POSITIVE =  1;
    public static final int NEUTRAL  =  0;
    public static final int NEGATIVE = -1;

    // ── Émotions spécifiques ──────────────────────────────────────────────────
    public static final int EMOTION_HAPPY     = 10;
    public static final int EMOTION_STRESSED  = 11;
    public static final int EMOTION_TIRED     = 12;
    public static final int EMOTION_BORED     = 13;
    public static final int EMOTION_EXCITED   = 14;
    public static final int EMOTION_SAD       = 15;

    private static final Set<String> POS = new HashSet<>(Arrays.asList(
        "bien","super","top","parfait","génial","excellent","merci","bravo",
        "heureux","content","cool","formidable","sympa","beau","magnifique",
        "incroyable","fantastique","joyeux","calme","détendu","motivé","yes",
        "ouais","oui","adore","aime","love","wow","sensass","nickel","impec",
        "enchanté","ravi","satisfait","épanoui"
    ));

    private static final Set<String> NEG = new HashSet<>(Arrays.asList(
        "nul","mauvais","horrible","terrible","triste","stressé","fatigué",
        "énervé","colère","déprimé","angoissé","peur","problème","merde",
        "chiant","galère","naze","bof","pas","jamais","rien","impossible",
        "difficile","lourd","compliqué","perdu","nope","non","arrête",
        "souffre","douleur","inquiet","découragé","démoralisé"
    ));

    private static final Set<String> STRESS_KW = new HashSet<>(Arrays.asList(
        "stressé","angoissé","panique","débordé","pression","urgent","deadline","anxieux","inquiet","overwhelmed"
    ));
    private static final Set<String> TIRED_KW = new HashSet<>(Arrays.asList(
        "fatigué","crevé","épuisé","sommeil","dormir","nuit","sieste","repose","las"
    ));
    private static final Set<String> BORED_KW = new HashSet<>(Arrays.asList(
        "ennuie","ennui","chiant","barbant","rien à faire","occuper","distraction"
    ));
    private static final Set<String> EXCITED_KW = new HashSet<>(Arrays.asList(
        "excité","trop bien","incroyable","fou","ouf","j'adore","hype","top","hyped"
    ));

    // ── Analyse de base ───────────────────────────────────────────────────────

    /** Retourne POSITIVE, NEUTRAL ou NEGATIVE. */
    public static int analyze(String text) {
        if (text == null || text.isEmpty()) return NEUTRAL;
        String[] words = text.toLowerCase().split("\\W+");
        int score = 0;
        for (String w : words) {
            if (POS.contains(w)) score++;
            if (NEG.contains(w)) score--;
        }
        return score > 0 ? POSITIVE : score < 0 ? NEGATIVE : NEUTRAL;
    }

    /** Analyse émotionnelle fine — retourne une constante EMOTION_*. */
    public static int analyzeEmotion(String text) {
        if (text == null || text.isEmpty()) return NEUTRAL;
        String t = text.toLowerCase();
        if (containsAny(t, STRESSED_KW_ARRAY)) return EMOTION_STRESSED;
        if (containsAny(t, TIRED_KW_ARRAY))    return EMOTION_TIRED;
        if (containsAny(t, BORED_KW_ARRAY))    return EMOTION_BORED;
        if (containsAny(t, EXCITED_KW_ARRAY))  return EMOTION_EXCITED;
        int base = analyze(text);
        if (base == POSITIVE) return EMOTION_HAPPY;
        if (base == NEGATIVE) return EMOTION_SAD;
        return NEUTRAL;
    }

    /** Action suggérée selon l'émotion détectée. */
    public static String suggestAction(int emotion) {
        switch (emotion) {
            case EMOTION_STRESSED:
                return "Je sens que tu es stressé. Veux-tu de la musique relaxante, ou que je règle une pause de 10 minutes ?";
            case EMOTION_TIRED:
                return "Tu sembles fatigué. Veux-tu que je règle une alarme pour une sieste de 20 minutes ?";
            case EMOTION_BORED:
                return "Je peux te suggérer un podcast, une musique ou quelque chose d'intéressant. Que préfères-tu ?";
            case EMOTION_EXCITED:
                return "Super énergie ! Veux-tu une playlist qui va avec ton humeur ?";
            case EMOTION_HAPPY:
                return "Ravi que tu sois de bonne humeur ! C'est le bon moment pour une playlist d'hype.";
            case EMOTION_SAD:
                return "Je suis là pour toi. Veux-tu parler, écouter de la musique ou te changer les idées ?";
            default:
                return "";
        }
    }

    public static String label(int s) {
        switch (s) {
            case POSITIVE:       return "positif";
            case NEGATIVE:       return "négatif";
            case EMOTION_STRESSED: return "stressé";
            case EMOTION_TIRED:  return "fatigué";
            case EMOTION_BORED:  return "ennuyé";
            case EMOTION_EXCITED:return "excité";
            case EMOTION_HAPPY:  return "heureux";
            case EMOTION_SAD:    return "triste";
            default:             return "neutre";
        }
    }

    /** Phrase de réaction contextuelle à injecter au début de la réponse. */
    public static String reactionFor(int s) {
        switch (s) {
            case POSITIVE:
            case EMOTION_HAPPY:
            case EMOTION_EXCITED:  return "Content de te l'entendre dire ! ";
            case NEGATIVE:
            case EMOTION_SAD:      return "Je sens que ça ne va pas, je suis là. ";
            case EMOTION_STRESSED: return "Je sens un peu de stress. Respire, je gère. ";
            case EMOTION_TIRED:    return "Tu as l'air fatigué. Je reste doux. ";
            default:               return "";
        }
    }

    /** Couleur de la bulle vague associée au sentiment. */
    public static int orbStateFor(int s) {
        switch (s) {
            case POSITIVE:
            case EMOTION_HAPPY:
            case EMOTION_EXCITED:  return LeaVoiceActivity.STATE_SPEAK;
            case NEGATIVE:
            case EMOTION_SAD:
            case EMOTION_STRESSED: return LeaVoiceActivity.STATE_THINK;
            default:               return LeaVoiceActivity.STATE_LISTEN;
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private static final String[] STRESSED_KW_ARRAY = STRESS_KW.toArray(new String[0]);
    private static final String[] TIRED_KW_ARRAY    = TIRED_KW.toArray(new String[0]);
    private static final String[] BORED_KW_ARRAY    = BORED_KW.toArray(new String[0]);
    private static final String[] EXCITED_KW_ARRAY  = EXCITED_KW.toArray(new String[0]);

    private static boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }
}
