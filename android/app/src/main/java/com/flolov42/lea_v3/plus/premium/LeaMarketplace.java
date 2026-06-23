package com.flolov42.lea_v3.plus.premium;

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
import java.util.List;

public class LeaMarketplace extends LeaBasePlusFeature {

    // Built-in skills catalogue [id, name, author, description, price, category, rating]
    private static final String[][] BUILTIN_SKILLS = {
        {"crypto_tracker",   "Crypto Tracker",      "LéaLabs",  "Prix BTC/ETH/BNB en temps réel",           "0",  "Finance",    "4.8"},
        {"plant_care",       "Plant Care AI",        "LéaLabs",  "Rappels d'arrosage + diagnostic plantes",  "0",  "Lifestyle",  "4.6"},
        {"movie_reco",       "Movie Recommender",    "LéaLabs",  "Films/séries selon ton humeur",            "0",  "Divertissement","4.7"},
        {"recipe_chef",      "Chef LÉA",             "LéaLabs",  "Recettes selon tes ingrédients",           "0",  "Cuisine",    "4.9"},
        {"sleep_coach",      "Sleep Coach",          "Community","Optimise ton sommeil par analyse",         "50", "Santé",      "4.5"},
        {"budget_planner",   "Budget Planner Pro",   "Community","Gestion budget + épargne auto",            "75", "Finance",    "4.7"},
        {"meditation_guide", "Méditation Guidée",    "Community","30 séances de méditation guidées",         "100","Bien-être",   "4.9"},
        {"workout_coach",    "Coach Fitness",        "Community","Plans d'entraînement personnalisés",       "75", "Sport",      "4.6"},
    };

    public LeaMarketplace(Context ctx) { super(ctx, LeaPlusDatabase.MARKETPLACE); }

    @Override
    public void execute() {
        checkForUpdates();
        log("🛒 Marketplace: " + getInstalledSkills().size() + " skill(s) installé(s)");
    }

    private void checkForUpdates() {
        List<LeaPlusDatabase.MarketSkill> installed = db.getInstalledSkills();
        for (LeaPlusDatabase.MarketSkill s : installed) {
            if (s.hasUpdate) {
                notify("🔄 Mise à jour disponible", s.name + " — nouvelle version disponible !");
                log("🔄 Update disponible: " + s.name);
            }
        }
    }

    public boolean installSkill(String skillId) {
        for (String[] s : BUILTIN_SKILLS) {
            if (s[0].equals(skillId)) {
                int price = Integer.parseInt(s[4]);
                if (price > 0) {
                    boolean paid = new LeaCoinSystem(ctx).spendCoins(skillId, s[1], price);
                    if (!paid) { log("❌ Coins insuffisants pour: " + s[1]); return false; }
                }
                db.installSkill(skillId, s[1], s[2], s[3], price, s[5], Float.parseFloat(s[6]));
                notify("✅ Skill installé !", s[1] + " est maintenant disponible dans LÉA !");
                log("📦 Skill installé: " + s[1]);
                LeaPlusManager.get(ctx).onTaskCompleted("Skill installé: " + s[1], 1);
                return true;
            }
        }
        log("❌ Skill introuvable: " + skillId);
        return false;
    }

    public void uninstallSkill(String skillId) {
        LeaPlusDatabase.MarketSkill s = db.getSkill(skillId);
        if (s != null) {
            db.uninstallSkill(skillId);
            log("🗑️ Skill désinstallé: " + s.name);
        }
    }

    public void rateSkill(String skillId, float rating, String review) {
        db.rateSkill(skillId, rating, review);
        log("⭐ Note: " + skillId + " → " + rating + "/5 | " + review.substring(0, Math.min(40, review.length())));
    }

    public List<LeaPlusDatabase.MarketSkill> getInstalledSkills() { return db.getInstalledSkills(); }

    public boolean isInstalled(String skillId) { return db.getSkill(skillId) != null; }

    public String getCatalogDisplay() {
        int balance = new LeaCoinSystem(ctx).getBalance();
        StringBuilder sb = new StringBuilder("🛒 MARKETPLACE LÉA\n");
        sb.append("💰 Ton solde: ").append(balance).append(" coins\n\n");
        String currentCategory = "";
        for (String[] s : BUILTIN_SKILLS) {
            if (!s[5].equals(currentCategory)) {
                currentCategory = s[5];
                sb.append("▶ ").append(currentCategory).append("\n");
            }
            boolean installed = isInstalled(s[0]);
            int price = Integer.parseInt(s[4]);
            sb.append(installed ? "✅" : (price > 0 && balance < price ? "🔒" : "⬇️"))
              .append(" ").append(s[1]).append(" — ")
              .append(price == 0 ? "Gratuit" : price + " coins")
              .append(" ⭐").append(s[6]).append("\n");
            sb.append("   ").append(s[3]).append("\n");
        }
        return sb.toString();
    }

    public String getInstalledDisplay() {
        List<LeaPlusDatabase.MarketSkill> skills = getInstalledSkills();
        if (skills.isEmpty()) return "📦 Aucun skill installé — explore le catalogue !";
        StringBuilder sb = new StringBuilder("📦 SKILLS INSTALLÉS\n\n");
        for (LeaPlusDatabase.MarketSkill s : skills) {
            sb.append("• ").append(s.name).append(" par ").append(s.author).append("\n");
            sb.append("  ⭐ ").append(String.format("%.1f", s.rating)).append(" | ").append(s.category).append("\n\n");
        }
        return sb.toString();
    }
}
