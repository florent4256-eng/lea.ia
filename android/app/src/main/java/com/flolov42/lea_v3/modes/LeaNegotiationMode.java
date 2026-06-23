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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaNegotiationMode extends LeaBaseMode {

    private static final String[][] NEGOTIABLE_SERVICES = {
        {"internet",     "Orange, SFR, Free",        "15-25€/mois"},
        {"téléphone",    "Bouygues, SFR, Orange",    "5-15€/mois"},
        {"assurance",    "AXA, MAIF, Allianz",       "10-30%"},
        {"banque",       "BNP, CA, LCL",             "frais annuels"},
        {"abonnement",   "Netflix, Spotify, Prime",  "1-3€/mois"},
        {"loyer",        "propriétaire",             "selon marché"},
        {"salaire",      "employeur",                "+5-15%"},
        {"forfait",      "opérateur mobile",         "5-10€/mois"},
    };

    private static final String[] NEGOTIATION_TACTICS = {
        "🎯 Technique de l'ancrage: propose d'abord un chiffre bas pour recentrer la négociation",
        "🤫 Silence stratégique: après ton offre, tais-toi — la pression fait parler l'autre",
        "🚶 BATNA: connais ton alternative — tu négocies mieux quand tu peux partir",
        "📊 Appuie-toi sur des chiffres: \"La concurrence propose X, vous pouvez matcher ?\"",
        "🤝 Win-win: cherche ce que l'autre veut vraiment au-delà du prix",
        "⏰ Timing: les fins de mois/trimestre sont idéales — les vendeurs ont des quotas",
        "😇 Demande le responsable: souvent seuls eux peuvent faire des exceptions",
    };

    public LeaNegotiationMode(Context ctx) { super(ctx, LeaModeDatabase.NEGOTIATION); }

    @Override
    public void execute() {
        double totalSaved = db.getTotalSaved();
        log("🤝 Total économisé via négociations: " + String.format("%.2f", totalSaved) + "€");

        // Notifier au maximum 1 fois par semaine (évite spam toutes les 30min)
        if (totalSaved > 0) {
            android.content.SharedPreferences prefs =
                ctx.getSharedPreferences("lea_negociation", android.content.Context.MODE_PRIVATE);
            long lastNotif = prefs.getLong("last_summary_notif", 0);
            if (System.currentTimeMillis() - lastNotif > 7L * 24 * 3600_000L) {
                notify("🤝 Négociation", "Tu as économisé " + String.format("%.2f", totalSaved) + "€ grâce aux négociations !");
                prefs.edit().putLong("last_summary_notif", System.currentTimeMillis()).apply();
            }
        }
    }

    public String analyzeContract(String contractText) {
        if (contractText == null || contractText.length() < 50)
            return "⚠️ Texte trop court pour une analyse pertinente.";

        StringBuilder analysis = new StringBuilder("📄 ANALYSE DU CONTRAT:\n\n");

        // Détecter montants
        Pattern amountPattern = Pattern.compile("(\\d+[,.]?\\d*)\\s*(?:€|euros?|EUR)", Pattern.CASE_INSENSITIVE);
        Matcher m = amountPattern.matcher(contractText);
        int amountCount = 0;
        while (m.find() && amountCount < 5) {
            analysis.append("💰 Montant détecté: ").append(m.group()).append("\n");
            amountCount++;
        }

        // Détecter clauses critiques
        String lower = contractText.toLowerCase();
        if (lower.contains("reconduction automatique") || lower.contains("renouvellement automatique"))
            analysis.append("⚠️ ALERTE: Reconduction automatique — pense à annuler avant la date\n");
        if (lower.contains("frais de résiliation") || lower.contains("pénalité"))
            analysis.append("⚠️ Frais de résiliation mentionnés — demande exemption si fidèle client\n");
        if (lower.contains("engagement") || lower.contains("durée minimale"))
            analysis.append("📌 Engagement détecté — vérifie la durée exacte\n");
        if (lower.contains("prix peut varier") || lower.contains("modification tarifaire"))
            analysis.append("⚡ Clause de modification de prix — négocie un prix garanti\n");

        if (amountCount == 0) analysis.append("Aucun montant clairement identifié.\n");

        double totalSaved = db.getTotalSaved();
        analysis.append("\n💡 Conseil: ").append(NEGOTIATION_TACTICS[(int)(System.currentTimeMillis() % NEGOTIATION_TACTICS.length)]);
        analysis.append("\n\n📊 Total économisé à ce jour: ").append(String.format("%.2f", totalSaved)).append("€");

        return analysis.toString();
    }

    public String generateNegotiationScript(String service, String currentPrice, String targetPrice) {
        return "📞 SCRIPT DE NÉGOCIATION — " + service.toUpperCase() + "\n\n" +
               "ACCROCHE:\n\"Bonjour, je suis client(e) depuis [X] ans. Je reçois des offres concurrentes à " +
               targetPrice + " pour le même service.\"\n\n" +
               "PRESSION DOUCE:\n\"Je préfère rester chez vous, mais à " + currentPrice +
               " je dois vraiment reconsidérer. Qu'est-ce que vous pouvez faire pour moi ?\"\n\n" +
               "SILENCE (attendre la réponse — NE PAS PARLER)\n\n" +
               "SI REFUS:\n\"Je comprends. Pouvez-vous me passer votre responsable fidélisation ?\"\n\n" +
               "SI OFFRE:\n\"C'est un bon début. Si vous pouvez inclure [avantage], j'accepte.\"\n\n" +
               "CLÔTURE:\n\"Merci. Pouvez-vous m'envoyer ça par écrit ?\"";
    }

    public void recordNegotiationResult(String service, double savedAmount, boolean success) {
        db.addNegotiation(service, savedAmount, success ? "success" : "failed");
        if (success) {
            log("✅ Négociation réussie: " + service + " — économie: " + String.format("%.2f", savedAmount) + "€");
            notify("🤝 Négociation réussie !", "Tu as économisé " + String.format("%.2f", savedAmount) + "€ sur " + service);
        } else {
            log("❌ Négociation échouée: " + service);
        }
    }

    public String[][] getNegotiableServices() { return NEGOTIABLE_SERVICES; }
    public String[] getNegotiationTactics()    { return NEGOTIATION_TACTICS; }
}
