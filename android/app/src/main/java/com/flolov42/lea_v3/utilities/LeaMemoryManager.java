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
import java.util.List;

public class LeaMemoryManager {

    private static final String PREFS = "lea_memory";
    private final LeaNovaDataStore db;
    private final SharedPreferences prefs;

    public LeaMemoryManager(Context ctx) {
        db    = LeaNovaDataStore.get(ctx);
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Sauvegarde un échange dans la mémoire conversationnelle. */
    public void remember(String userText, String leaText, int sentiment) {
        db.saveConversation(userText, leaText, sentiment);
    }

    /** Construit un bloc de contexte des N derniers échanges pour enrichir un prompt IA. */
    public String buildContext(int lastN) {
        List<String[]> history = db.getRecentConversations(lastN);
        if (history.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[Contexte de la conversation]\n");
        for (int i = history.size() - 1; i >= 0; i--) {
            String[] p = history.get(i);
            sb.append("Utilisateur: ").append(p[0]).append("\n");
            sb.append("Léa: ").append(p[1]).append("\n");
        }
        sb.append("[Fin du contexte]\n");
        return sb.toString();
    }

    /** Mémorise une préférence utilisateur + incrémente l'habitude. */
    public void setPreference(String key, String value) {
        prefs.edit().putString(key, value).apply();
        db.incrementHabit(key, value);
    }

    public String getPreference(String key, String def) {
        return prefs.getString(key, def);
    }

    /** Retourne les habitudes les plus fréquentes. */
    public List<String[]> getTopHabits(int limit) {
        return db.getTopHabits(limit);
    }

    /** Lit l'identité du patron depuis CapacitorStorage. */
    public String getBossName() {
        SharedPreferences cap = prefs.getAll().containsKey("lea_session_user") ? prefs : null;
        if (cap != null) return cap.getString("lea_session_user", "patron").replace("\"", "");
        return getPreference("lea_session_user", "patron").replace("\"", "");
    }

    // ── Contexte conversationnel (entités) ────────────────────────────────────

    /** Sauvegarde une entité contextuelle (dernière personne, lieu, sujet). */
    public void saveContext(String key, String value) {
        if (value != null && !value.isEmpty())
            prefs.edit().putString("ctx_" + key, value).apply();
    }

    /** Récupère une entité contextuelle. */
    public String getContext(String key) {
        return prefs.getString("ctx_" + key, "");
    }

    /** Efface tout le contexte conversationnel. */
    public void clearContext() {
        SharedPreferences.Editor ed = prefs.edit();
        for (String k : prefs.getAll().keySet()) {
            if (k.startsWith("ctx_")) ed.remove(k);
        }
        ed.apply();
    }

    // ── Tracking des habitudes temporelles ───────────────────────────────────

    /** Enregistre qu'une action a été faite maintenant (pour le moteur de recommandation). */
    public void recordTimedAction(String action) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String key = "timed_" + action + "_h" + hour;
        int count = prefs.getInt(key, 0) + 1;
        prefs.edit().putInt(key, count).apply();
        db.incrementHabit(key, action);
    }

    /** Retourne le nombre de fois qu'une action a été faite à une heure donnée. */
    public int getTimedActionCount(String action, int hour) {
        String key = "timed_" + action + "_h" + hour;
        return prefs.getInt(key, 0);
    }
}
