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

public class LeaDreamMode extends LeaBaseMode {

    private static final String PREFS = "lea_dream";

    private static final String[][] SYMBOLS = {
        {"eau",      "Émotions, inconscient, transitions"},
        {"chute",    "Peur de l'échec ou perte de contrôle"},
        {"vol",      "Désir de liberté ou ambition"},
        {"maison",   "Ton psyché, ton identité intérieure"},
        {"serpent",  "Transformation, guérison ou menace cachée"},
        {"dents",    "Anxiété sociale ou peur du jugement"},
        {"mort",     "Fin d'un cycle, renouveau, changement"},
        {"enfant",   "Innocence, potentiel non réalisé"},
        {"feu",      "Passion, colère ou purification"},
        {"poursuite","Évitement d'un problème réel"},
        {"nuit",     "Inconscient, mystère ou peur"},
        {"lumière",  "Clarté, insight, espoir"},
    };

    public LeaDreamMode(Context ctx) { super(ctx, LeaModeDatabase.DREAM); }

    @Override
    public void execute() {
        checkMorningSuggestion();
    }

    private void checkMorningSuggestion() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 6 || hour > 10) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (today.equals(prefs.getString("last_reminder", ""))) return;

        List<LeaModeDatabase.DreamRow> dreams = db.getDreams(1);
        String lastDreamDate = dreams.isEmpty() ? "jamais" :
            new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(dreams.get(0).ts));

        notify("🌙 Dream Journal", "Bon réveil ! Enregistre ton rêve de cette nuit avant de l'oublier. Dernier rêve: " + lastDreamDate);
        prefs.edit().putString("last_reminder", today).apply();
        log("⏰ Rappel matinal envoyé");
    }

    public long recordDream(String content) {
        String emotion = detectEmotion(content);
        String interpretation = interpretDream(content);
        long id = db.addDream(content, emotion, interpretation);
        log("🌙 Rêve enregistré: \"" + content.substring(0, Math.min(40, content.length())) + "…\"");
        return id;
    }

    private String detectEmotion(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("peur") || lower.contains("effray") || lower.contains("terreur")) return "😨 Peur";
        if (lower.contains("heureux") || lower.contains("joie") || lower.contains("rire"))   return "😊 Joie";
        if (lower.contains("triste") || lower.contains("pleur") || lower.contains("deuil"))  return "😢 Tristesse";
        if (lower.contains("colère") || lower.contains("furieux") || lower.contains("cri"))  return "😠 Colère";
        if (lower.contains("amour") || lower.contains("tendre") || lower.contains("câlin"))  return "❤️ Amour";
        return "😐 Neutre";
    }

    public String interpretDream(String content) {
        String lower = content.toLowerCase();
        StringBuilder interp = new StringBuilder("🔍 Analyse symbolique:\n");
        int found = 0;
        for (String[] sym : SYMBOLS) {
            if (lower.contains(sym[0])) {
                interp.append("• ").append(sym[0].substring(0,1).toUpperCase())
                      .append(sym[0].substring(1)).append(": ").append(sym[1]).append("\n");
                found++;
            }
        }
        if (found == 0) interp.append("Rêve ordinaire — symboles non répertoriés détectés.\n");

        int dreamCount = db.getDreamCount();
        interp.append("\n💡 Total journalisé: ").append(dreamCount).append(" rêve(s)");
        return interp.toString();
    }

    public List<LeaModeDatabase.DreamRow> getRecentDreams() {
        return db.getDreams(10);
    }
}
