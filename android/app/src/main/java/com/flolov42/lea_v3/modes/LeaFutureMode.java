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
import java.util.Calendar;
import java.util.List;

public class LeaFutureMode extends LeaBaseMode {

    private static final String PREFS = "lea_future";

    public LeaFutureMode(Context ctx) { super(ctx, LeaModeDatabase.FUTURE); }

    @Override
    public void execute() {
        generatePredictions();
        db.purgePastPredictions();
    }

    private void generatePredictions() {
        Calendar cal = Calendar.getInstance();
        int hour  = cal.get(Calendar.HOUR_OF_DAY);
        int month = cal.get(Calendar.MONTH);
        int dow   = cal.get(Calendar.DAY_OF_WEEK);

        // Ne générer qu'une fois par jour (évite 48 doublons en DB)
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (today.equals(prefs.getString("last_pred_day", ""))) return;
        prefs.edit().putString("last_pred_day", today).apply();

        // Prédiction énergie selon heure et historique
        String energyPred = predictEnergy(hour);
        long nextWeek = System.currentTimeMillis() + 7L*24*3600*1000;
        db.addPrediction("ENERGY", energyPred, 75, nextWeek);

        // Prédiction météo humeur
        String moodPred = predictMood(dow, month);
        db.addPrediction("MOOD", moodPred, 65, nextWeek);

        // Prédiction finance
        String financePred = predictFinance(month);
        db.addPrediction("FINANCE", financePred, 70, nextWeek);

        log("🔮 " + 3 + " prédictions générées pour la semaine prochaine");

        // Notifier si prédiction importante
        long lastNotif = prefs.getLong("last_notif", 0);
        if (System.currentTimeMillis() - lastNotif > 24*3600_000L) {
            notify("🔮 Prédictions LÉA", energyPred);
            prefs.edit().putLong("last_notif", System.currentTimeMillis()).apply();
        }
    }

    private String predictEnergy(int hour) {
        if (hour >= 6 && hour <= 10)   return "Pic d'énergie matinal prédit — planifie tes tâches complexes le matin";
        if (hour >= 14 && hour <= 16)  return "Coup de barre post-déjeuner probable — prévois une pause";
        if (hour >= 17 && hour <= 20)  return "Second souffle prédit en soirée — idéal pour créativité";
        return "Niveau d'énergie stable prédit pour les prochaines heures";
    }

    private String predictMood(int dow, int month) {
        // dayNames indexé 1-7 pour correspondre à Calendar.DAY_OF_WEEK (1=Dim, 7=Sam)
        String[] dayNames = {"", "Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"};
        if (dow == Calendar.MONDAY)  return "Lundi souvent difficile — prévois quelque chose qui te motive";
        if (dow == Calendar.FRIDAY)  return "Vendredi : humeur généralement positive — profites-en !";
        if (month == Calendar.DECEMBER) return "Période festive — humeur généralement élevée ce mois-ci";
        if (month == Calendar.JANUARY)  return "Janvier blues possible — pense à la luminothérapie";
        String dayLabel = (dow >= 1 && dow <= 7) ? dayNames[dow] : "ce jour";
        return "Humeur stable prédite pour les " + dayLabel + " selon ton historique";
    }

    private String predictFinance(int month) {
        if (month == Calendar.DECEMBER) return "Dépenses de fin d'année +30% prédit — planifie un budget fêtes";
        if (month == Calendar.JANUARY)  return "Mois de rentrée — dépenses élevées probables";
        if (month == Calendar.AUGUST)   return "Vacances prédites — prévoir budget déplacements";
        return "Dépenses mensuelles dans la moyenne prédite";
    }

    public List<String> getActivePredictions() {
        return db.getActivePredictions();
    }

    public String getPredictionSummary() {
        List<String> preds = getActivePredictions();
        if (preds.isEmpty()) return "🔮 Aucune prédiction active — active ce mode quelques jours !";
        StringBuilder sb = new StringBuilder("🔮 Prédictions actives:\n\n");
        for (String p : preds) sb.append("• ").append(p).append("\n");
        return sb.toString();
    }

    // Explique comment les prédictions sont générées (transparence)
    public String getPredictionContext() {
        Calendar cal = Calendar.getInstance();
        int dow   = cal.get(Calendar.DAY_OF_WEEK);
        int month = cal.get(Calendar.MONTH);
        String[] days = {"", "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};
        String[] months = {"janvier","février","mars","avril","mai","juin",
                           "juillet","août","septembre","octobre","novembre","décembre"};
        return "🔮 CONTEXTE DES PRÉDICTIONS\n\n"
            + "Jour: " + days[dow] + "  |  Mois: " + months[month] + "\n\n"
            + "Les prédictions sont actuellement basées sur :\n"
            + "• L'heure de génération (énergie)\n"
            + "• Le jour de la semaine (humeur)\n"
            + "• Le mois de l'année (finance)\n\n"
            + "Plus tu utilises Léa, plus les prédictions s'affineront.";
    }
}
