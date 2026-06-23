package com.flolov42.lea_v3.social;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import android.content.Intent;

public class LeaShareManager {

    private static LeaShareManager instance;
    private final Context ctx;

    public static synchronized LeaShareManager get(Context ctx) {
        if (instance == null) instance = new LeaShareManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaShareManager(Context ctx) { this.ctx = ctx; }

    public void shareAchievement(android.app.Activity activity, String achievementName, String achievementIcon) {
        LeaPlusDatabase plus = LeaPlusDatabase.get(ctx);
        LeaPlusDatabase.CharStats stats = plus.getCharStats();
        int unlocked = LeaFeaturesDatabase.get(ctx).getUnlockedCount();

        String text = achievementIcon + " J'ai débloqué \"" + achievementName + "\" sur Léa!\n\n"
            + "🏆 " + unlocked + " achievements débloqués\n"
            + "⭐ Niveau " + stats.level + " | " + stats.xp + " XP\n\n"
            + "#LéaApp #Achievement #Productivity";

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(share, "Partager mon achievement"));
    }

    public void shareStats(android.app.Activity activity) {
        LeaPlusDatabase plus = LeaPlusDatabase.get(ctx);
        LeaPlusDatabase.CharStats stats = plus.getCharStats();
        int coins = plus.getCoinBalance();
        int habits = plus.getActiveHabits().size();
        int unlocked = LeaFeaturesDatabase.get(ctx).getUnlockedCount();

        String text = "🌟 Mes stats sur Léa:\n\n"
            + "⭐ Niveau " + stats.level + " | " + stats.xp + " XP\n"
            + "🪙 " + coins + " LÉA Coins\n"
            + "🌱 " + habits + " habitudes actives\n"
            + "🏆 " + unlocked + " achievements\n"
            + "🌍 " + stats.world + "\n\n"
            + "#LéaApp #PersonalGrowth #Productivity";

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(share, "Partager mes stats"));
    }

    public void challengeFriend(android.app.Activity activity, String challengeDesc) {
        String text = "⚔️ Défi Léa!\n\n"
            + challengeDesc + "\n\n"
            + "Acceptes-tu le défi? Télécharge Léa!\n"
            + "#LéaApp #Challenge";

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(share, "Défier un ami"));
    }
}
