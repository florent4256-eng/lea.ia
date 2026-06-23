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


import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaFinanceAgent {

    private static final String ID    = LeaAgentActivationManager.FINANCE;
    private static final String PREFS = "lea_finance";

    // Budgets par défaut (€/semaine) — ordre d'affichage garanti
    private static final Map<String, Double> DEFAULT_BUDGETS;
    static {
        DEFAULT_BUDGETS = new java.util.LinkedHashMap<>();
        DEFAULT_BUDGETS.put("Restaurant",  80.0);
        DEFAULT_BUDGETS.put("Transport",   50.0);
        DEFAULT_BUDGETS.put("Loisir",     100.0);
        DEFAULT_BUDGETS.put("Shopping",   150.0);
        DEFAULT_BUDGETS.put("Abonnement",  60.0);
        DEFAULT_BUDGETS.put("Autre",      200.0);
    }

    private final Map<String, Double>       budgets;
    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences         prefs;

    // Known subscription keywords (detect unused subs)
    private static final String[] SUBSCRIPTIONS = {
        "netflix", "spotify", "amazon prime", "disney+", "apple",
        "playstation", "xbox", "deezer", "canal+", "orange"
    };

    public LeaFinanceAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.budgets = loadBudgets();
    }

    private Map<String, Double> loadBudgets() {
        Map<String, Double> result = new java.util.LinkedHashMap<>(DEFAULT_BUDGETS);
        for (String cat : DEFAULT_BUDGETS.keySet()) {
            if (prefs.contains("budget_" + cat)) {
                result.put(cat, (double) prefs.getFloat("budget_" + cat,
                    DEFAULT_BUDGETS.get(cat).floatValue()));
            }
        }
        return result;
    }

    public void execute() {
        try {
            parseBankSms();
            checkBudgetAlerts();
            detectUnusedSubscriptions();
            generateWeeklySummary();
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Finance: " + e.getMessage());
        }
    }

    private void parseBankSms() {
        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            ContentResolver cr = ctx.getContentResolver();
            long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;

            Cursor c = cr.query(smsUri,
                new String[]{"address", "body", "date"},
                "date > ?", new String[]{String.valueOf(weekAgo)},
                "date DESC");

            if (c == null) return;

            int parsed = 0;
            try {
                while (c.moveToNext() && parsed < 50) {
                    String body = c.getString(1);
                    if (body == null) continue;

                    TransactionInfo tx = extractTransaction(body);
                    if (tx != null) {
                        db.addFinanceEntry(tx.amount, tx.category, tx.description, "SMS");
                        db.addLog(ID, "💳 Transaction détectée: " + tx.category + " " + String.format("%.2f€", Math.abs(tx.amount)));
                        parsed++;
                    }
                }
            } finally {
                c.close();
            }

            if (parsed > 0) db.addLog(ID, "📊 " + parsed + " transaction(s) importée(s) depuis les SMS");

        } catch (SecurityException e) {
            db.addLog(ID, "🔒 Permission SMS requise pour analyser les transactions");
        }
    }

    private static class TransactionInfo {
        double amount;
        String category, description;
    }

    private TransactionInfo extractTransaction(String sms) {
        String lower = sms.toLowerCase();

        // Patterns SMS bancaires français — accepte "100€", "12,5€", "25.50€"
        Pattern amountPattern = Pattern.compile(
            "([-+]?\\d+(?:[,.]\\d{1,2})?)\\s*(?:eur|€|euros?)",
            Pattern.CASE_INSENSITIVE);
        Matcher m = amountPattern.matcher(sms);
        if (!m.find()) return null;

        double amount;
        try {
            amount = Double.parseDouble(m.group(1).replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }

        // Check it's a debit (spending)
        if (!lower.contains("débit") && !lower.contains("debit") &&
            !lower.contains("paiement") && !lower.contains("virement") &&
            !lower.contains("achat") && !lower.contains("retrait")) {
            return null;
        }

        // Montant négatif si dépense confirmée
        if (lower.contains("débit")   || lower.contains("achat")   || lower.contains("retrait") ||
            lower.contains("virement sortant") || lower.contains("virement émis") ||
            lower.contains("en votre débit")) {
            amount = -Math.abs(amount);
        }

        TransactionInfo tx = new TransactionInfo();
        tx.amount      = amount;
        tx.description = sms.substring(0, Math.min(100, sms.length()));
        tx.category    = categorize(lower);
        return tx;
    }

    private String categorize(String text) {
        if (text.contains("restaurant") || text.contains("mcdonald") || text.contains("burger") ||
            text.contains("sushi") || text.contains("pizza") || text.contains("kebab") || text.contains("uber eat")) {
            return "Restaurant";
        }
        if (text.contains("uber") || text.contains("lyft") || text.contains("sncf") || text.contains("ratp") ||
            text.contains("essence") || text.contains("parking") || text.contains("taxi")) {
            return "Transport";
        }
        if (text.contains("netflix") || text.contains("spotify") || text.contains("disney") ||
            text.contains("amazon prime") || text.contains("apple") || text.contains("deezer")) {
            return "Abonnement";
        }
        if (text.contains("cinema") || text.contains("concert") || text.contains("jeu") ||
            text.contains("steam") || text.contains("playstation")) {
            return "Loisir";
        }
        if (text.contains("zara") || text.contains("h&m") || text.contains("amazon") ||
            text.contains("fnac") || text.contains("leclerc") || text.contains("carrefour")) {
            return "Shopping";
        }
        return "Autre";
    }

    private void checkBudgetAlerts() {
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        List<LeaAgentDatabase.FinanceRow> entries = db.getFinanceEntries(200);

        Map<String, Double> spent = new HashMap<>();
        for (LeaAgentDatabase.FinanceRow row : entries) {
            if (row.timestamp > weekAgo && row.amount < 0) {
                double current = spent.containsKey(row.category) ? spent.get(row.category) : 0;
                spent.put(row.category, current + Math.abs(row.amount));
            }
        }

        for (Map.Entry<String, Double> e : spent.entrySet()) {
            String cat   = e.getKey();
            double total = e.getValue();
            Double budget = budgets.get(cat);
            if (budget != null && total > budget) {
                double over = total - budget;
                String msg = "⚠️ Budget " + cat + " dépassé! +" + String.format("%.2f€", over) + " cette semaine";
                db.addLog(ID, msg);
                notif.notify(ID, "💰 Alerte Budget", msg);
            }
        }
    }

    private void detectUnusedSubscriptions() {
        // Check subscriptions detected in last 90 days but not recently (30 days)
        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
        long ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 3600 * 1000;

        List<LeaAgentDatabase.FinanceRow> entries = db.getFinanceEntries(500);
        Map<String, Long> lastSeen = new HashMap<>();

        for (LeaAgentDatabase.FinanceRow row : entries) {
            if (row.timestamp > ninetyDaysAgo && "Abonnement".equals(row.category)) {
                for (String sub : SUBSCRIPTIONS) {
                    if (row.description != null && row.description.toLowerCase().contains(sub)) {
                        Long last = lastSeen.get(sub);
                        if (last == null || row.timestamp > last) {
                            lastSeen.put(sub, row.timestamp);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Long> e : lastSeen.entrySet()) {
            if (e.getValue() < thirtyDaysAgo) {
                String msg = "💡 Abonnement inutilisé? " + e.getKey() + " pas vu depuis >30 jours";
                db.addLog(ID, msg);
            }
        }
    }

    private void generateWeeklySummary() {
        double total = db.getTotalSpentThisWeek();
        if (total > 0) {
            String msg = "📊 Dépenses semaine: " + String.format("%.2f€", total);
            db.addLog(ID, msg);
            db.updateLastAction(ID, msg);
        }
    }

    public String getWeeklySummary() {
        double total = db.getTotalSpentThisWeek();
        return "Dépenses semaine: " + String.format("%.2f€", total);
    }

    public Map<String, Double> getBudgets() {
        return budgets;
    }

    public void setBudget(String category, double amount) {
        budgets.put(category, amount);
        prefs.edit().putFloat("budget_" + category, (float) amount).apply();
        db.addLog(ID, "⚙️ Budget " + category + " : " + String.format("%.0f€/sem", amount));
    }

    public void addManualTransaction(double amount, String category, String description) {
        db.addFinanceEntry(amount, category, description.isEmpty() ? category : description, "MANUEL");
        db.addLog(ID, "✍️ Manuel: " + category + " " + String.format("%.2f€", Math.abs(amount)));
    }

    public List<LeaAgentDatabase.FinanceRow> getRecentTransactions(int limit) {
        return db.getFinanceEntries(limit);
    }

    public String getMonthlySummary() {
        long monthAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
        List<LeaAgentDatabase.FinanceRow> entries = db.getFinanceEntries(1000);

        double totalOut = 0, totalIn = 0;
        java.util.Map<String, Double> byCategory = new java.util.LinkedHashMap<>();

        for (LeaAgentDatabase.FinanceRow row : entries) {
            if (row.timestamp < monthAgo) continue;
            if (row.amount < 0) {
                totalOut += Math.abs(row.amount);
                double cur = byCategory.containsKey(row.category) ? byCategory.get(row.category) : 0;
                byCategory.put(row.category, cur + Math.abs(row.amount));
            } else {
                totalIn += row.amount;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("💳 Dépenses 30 jours : ").append(String.format("%.2f€", totalOut)).append("\n");
        if (totalIn > 0)
            sb.append("💰 Entrées 30 jours : ").append(String.format("%.2f€", totalIn)).append("\n");
        sb.append("\n📊 Par catégorie :\n");
        for (java.util.Map.Entry<String, Double> e : byCategory.entrySet()) {
            Double budget = budgets.get(e.getKey());
            String budgetStr = budget != null
                ? String.format(" / %.0f€/sem × 4 = %.0f€", budget, budget * 4) : "";
            sb.append("  ").append(e.getKey()).append(" : ")
              .append(String.format("%.2f€", e.getValue()))
              .append(budgetStr).append("\n");
        }
        if (byCategory.isEmpty()) sb.append("  Aucune transaction enregistrée ce mois\n");
        return sb.toString().trim();
    }

    public Map<String, Double> getWeeklySpentByCategory() {
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        List<LeaAgentDatabase.FinanceRow> entries = db.getFinanceEntries(200);
        Map<String, Double> spent = new java.util.LinkedHashMap<>();
        for (LeaAgentDatabase.FinanceRow row : entries) {
            if (row.timestamp > weekAgo && row.amount < 0) {
                double current = spent.containsKey(row.category) ? spent.get(row.category) : 0;
                spent.put(row.category, current + Math.abs(row.amount));
            }
        }
        return spent;
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
