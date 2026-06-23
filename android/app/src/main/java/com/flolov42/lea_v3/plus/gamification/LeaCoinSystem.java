package com.flolov42.lea_v3.plus.gamification;

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
import java.util.Locale;

public class LeaCoinSystem extends LeaBasePlusFeature {

    private static final String PREFS = "lea_coin_system";

    // Shop items (id, name, cost, description)
    public static final String[][] SHOP_ITEMS = {
        {"theme_dark",        "Thème Dark Pro",          "100",  "Thème ultra-sombre premium"},
        {"theme_galaxy",      "Thème Galaxie",           "150",  "Fond d'écran animé galaxie"},
        {"notif_emoji",       "Emojis Notifications",    "75",   "Emojis custom dans tes notifs"},
        {"daily_boost",       "Boost Quotidien x3",      "200",  "Triple XP pendant 24h"},
        {"premium_trial",     "Trial Premium 7 jours",   "500",  "Accès complet pendant 1 semaine"},
        {"custom_voice",      "Voix personnalisée",      "300",  "Choisir la voix de LÉA"},
    };

    public LeaCoinSystem(Context ctx) { super(ctx, LeaPlusDatabase.COINS); }

    @Override
    public void execute() {
        checkWeeklyBonus();
        int balance = db.getCoinBalance();
        log("💰 Balance: " + balance + " Léa Coins");
        if (balance > 0 && balance % 500 == 0) notif.notifyCoinMilestone(balance);
    }

    public void addCoins(int amount, String reason) {
        db.addCoins(amount, reason);
        log("+" + amount + " 💰 (" + reason + ") → Total: " + db.getCoinBalance());
    }

    public boolean spendCoins(String itemId, String itemName, int cost) {
        boolean success = db.spendCoins(cost, itemName);
        if (success) log("🛒 Achat: " + itemName + " (-" + cost + " coins)");
        else         log("❌ Solde insuffisant pour: " + itemName + " (" + cost + " requis, " + db.getCoinBalance() + " disponibles)");
        return success;
    }

    public int getBalance() { return db.getCoinBalance(); }

    public String getBalanceDisplay() {
        return "💰 " + db.getCoinBalance() + " Léa Coins";
    }

    private void checkWeeklyBonus() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String thisWeek = new SimpleDateFormat("yyyyWW", Locale.getDefault()).format(new Date());
        if (!thisWeek.equals(prefs.getString("last_weekly_bonus", ""))) {
            // Check habit streak for weekly bonus
            int streakBonus = 50;
            addCoins(streakBonus, "Bonus hebdomadaire");
            prefs.edit().putString("last_weekly_bonus", thisWeek).apply();
            log("🎁 Bonus hebdomadaire: +" + streakBonus + " coins");
        }
    }

    public String getShopDisplay() {
        int balance = db.getCoinBalance();
        StringBuilder sb = new StringBuilder("🛒 BOUTIQUE LÉA COINS\n");
        sb.append("Solde: ").append(balance).append(" 💰\n\n");
        for (String[] item : SHOP_ITEMS) {
            int cost = Integer.parseInt(item[2]);
            sb.append(balance >= cost ? "✅" : "🔒")
              .append(" ").append(item[1]).append(" — ").append(cost).append(" coins\n");
            sb.append("   ").append(item[3]).append("\n\n");
        }
        return sb.toString();
    }
}
