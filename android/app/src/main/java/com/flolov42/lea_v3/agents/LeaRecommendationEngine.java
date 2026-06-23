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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LeaRecommendationEngine {

    private final LeaMemoryManager memory;
    private final Context ctx;

    public LeaRecommendationEngine(Context ctx) {
        this.ctx    = ctx.getApplicationContext();
        this.memory = new LeaMemoryManager(ctx);
    }

    /** Retourne une liste de suggestions contextuelles (heure + habitudes). */
    public List<String> getSuggestions() {
        List<String> list = new ArrayList<>();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int day  = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1=dim, 2=lun...

        // Suggestions selon l'heure
        if (hour >= 6 && hour < 9) {
            list.add("Météo du matin ?");
            list.add("Consulter ton calendrier du jour ?");
            list.add("Activer la routine matinale ?");
        } else if (hour >= 9 && hour < 12) {
            list.add("Résumé des notifications ?");
            list.add("Créer un rappel pour cet après-midi ?");
        } else if (hour >= 12 && hour < 14) {
            list.add("Pause musicale ?");
            list.add("Rappels de l'après-midi ?");
        } else if (hour >= 17 && hour < 20) {
            list.add("Résumé des messages du jour ?");
            list.add("Routine du soir ?");
        } else if (hour >= 20) {
            list.add("Régler un réveil pour demain ?");
            list.add("Bilan de la journée ?");
            list.add("Mode silencieux pour la nuit ?");
        }

        // Suggestions selon le jour
        if (day == 2) list.add("C'est lundi, activer la routine semaine ?"); // lundi
        if (day == 7) list.add("C'est samedi, mode détente ?");
        if (day == 1) list.add("C'est dimanche, préparer la semaine ?");

        // Habitudes récurrentes
        List<String[]> habits = memory.getTopHabits(2);
        for (String[] h : habits) {
            String val = h[1];
            if (val != null && val.length() > 3 && !val.startsWith("last_cmd_")) {
                list.add("Répéter : \"" + val + "\" ?");
            }
        }

        return list;
    }

    /** Une phrase vocale courte de suggestion proactive. */
    public String buildProactiveMessage() {
        List<String> s = getSuggestions();
        if (s.isEmpty()) return "";
        return "Au fait, je peux aussi : " + s.get(0);
    }

    /** Suggestion rapide pour enrichir la réponse IA. */
    public String getQuickHint() {
        List<String> s = getSuggestions();
        if (s.isEmpty()) return "";
        return s.get(0);
    }

    // ── Apprentissage des patterns ────────────────────────────────────────────

    /** Enregistre qu'une action a été exécutée (pour apprendre les habitudes). */
    public void recordAction(String action) {
        memory.recordTimedAction(action);
        memory.setPreference("last_action", action);
    }

    /**
     * Retourne une recommandation proactive basée sur les habitudes apprises.
     * Ex: "Tu écoutes souvent de la musique à cette heure. Veux-tu lancer Spotify ?"
     */
    public String getProactiveRecommendation() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        // Vérifie les habitudes temporelles connues
        String[] actions = { "music", "alarm", "weather", "calendar", "call" };
        String[] labels  = {
            "de la musique",
            "un réveil",
            "la météo",
            "ton calendrier",
            "quelqu'un"
        };
        int bestCount = 1; // seuil minimum
        String bestLabel = null;
        for (int i = 0; i < actions.length; i++) {
            int count = memory.getTimedActionCount(actions[i], hour);
            if (count > bestCount) { bestCount = count; bestLabel = labels[i]; }
        }
        if (bestLabel != null)
            return "D'habitude à cette heure tu demandes " + bestLabel + ". Veux-tu le faire ?";

        // Fallback sur les suggestions contextuelles
        List<String> s = getSuggestions();
        return s.isEmpty() ? "" : s.get(0);
    }
}
