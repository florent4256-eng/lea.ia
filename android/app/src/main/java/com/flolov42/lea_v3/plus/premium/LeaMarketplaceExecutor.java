package com.flolov42.lea_v3.plus.premium;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.flolov42.lea_v3.database.LeaPlusDatabase;
import com.flolov42.lea_v3.plus.premium.skills.LeaSkillCryptoActivity;
import com.flolov42.lea_v3.plus.premium.skills.LeaSkillPlantCareActivity;
import com.flolov42.lea_v3.plus.premium.skills.LeaSkillMovieActivity;
import com.flolov42.lea_v3.plus.premium.skills.LeaSkillChefActivity;

public class LeaMarketplaceExecutor {

    private static final String SKILL_CRYPTO = "skill_crypto";
    private static final String SKILL_PLANT  = "skill_plant";
    private static final String SKILL_MOVIE  = "skill_movie";
    private static final String SKILL_CHEF   = "skill_chef";

    private final Context ctx;
    private final LeaPlusDatabase db;

    private static LeaMarketplaceExecutor instance;
    public static synchronized LeaMarketplaceExecutor get(Context ctx) {
        if (instance == null) instance = new LeaMarketplaceExecutor(ctx.getApplicationContext());
        return instance;
    }
    private LeaMarketplaceExecutor(Context ctx) {
        this.ctx = ctx;
        this.db  = LeaPlusDatabase.get(ctx);
    }

    // ── Seeding des skills par défaut ─────────────────────────────────────────
    public void seedDefaultSkillsIfEmpty() {
        // On vérifie si le skill existe déjà avant d'insérer
        if (!isSkillInMarket(SKILL_CRYPTO)) {
            db.addSkillToMarketplace(SKILL_CRYPTO, "Crypto Tracker",
                "Suis le cours de Bitcoin, Ethereum et BNB en temps réel via CoinGecko.",
                "Léa AI", "Finance", 50);
        }
        if (!isSkillInMarket(SKILL_PLANT)) {
            db.addSkillToMarketplace(SKILL_PLANT, "Plant Care",
                "Suivi de tes plantes avec calendrier d'arrosage intelligent.",
                "Léa AI", "Nature", 30);
        }
        if (!isSkillInMarket(SKILL_MOVIE)) {
            db.addSkillToMarketplace(SKILL_MOVIE, "Movie Recommender",
                "Suggestions de films basées sur ton humeur du moment.",
                "Léa AI", "Divertissement", 20);
        }
        if (!isSkillInMarket(SKILL_CHEF)) {
            db.addSkillToMarketplace(SKILL_CHEF, "Chef Léa",
                "Recettes personnalisées selon les ingrédients que tu as à disposition.",
                "Léa AI", "Cuisine", 25);
        }
    }

    // ── Lancer un skill installé ──────────────────────────────────────────────
    public void launchSkill(Context activityCtx, String skillId) {
        Class<?> cls = getSkillActivity(skillId);
        if (cls == null) {
            Toast.makeText(activityCtx, "Skill non disponible: " + skillId, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(activityCtx, cls);
        intent.putExtra("skill_id", skillId);
        activityCtx.startActivity(intent);
    }

    // ── Achat d'un skill ──────────────────────────────────────────────────────
    public boolean purchaseAndInstall(String skillId, String name, String author,
                                      String description, int priceCoins, String category, float stars) {
        int balance = db.getCoinBalance();
        if (balance < priceCoins) return false;

        db.spendCoins(priceCoins, "Skill acheté: " + name);
        db.installSkill(skillId, name, author, description, priceCoins, category, stars);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean isSkillInMarket(String skillId) {
        // On utilise getMarketplaceSkills() et on filtre par id
        for (LeaPlusDatabase.SkillRow sk : db.getMarketplaceSkills()) {
            if (skillId.equals(sk.id)) return true;
        }
        return false;
    }

    private Class<?> getSkillActivity(String skillId) {
        switch (skillId) {
            case SKILL_CRYPTO: return LeaSkillCryptoActivity.class;
            case SKILL_PLANT:  return LeaSkillPlantCareActivity.class;
            case SKILL_MOVIE:  return LeaSkillMovieActivity.class;
            case SKILL_CHEF:   return LeaSkillChefActivity.class;
            default:           return null;
        }
    }
}
