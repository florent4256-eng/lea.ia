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

public class LeaAdventureMode extends LeaBasePlusFeature {

    private static final String[][] WORLDS = {
        {"Procrastination Valley",  "Le monde de l'inaction — chaque tâche te rapproche de la sortie"},
        {"Focus Forest",            "Forêt enchantée où la concentration est une magie"},
        {"Social Summit",           "Sommet où les liens humains forgent ta légende"},
        {"Learning Labyrinth",      "Labyrinthe de la connaissance — chaque mot appris ouvre une porte"},
        {"Discipline Dungeon",      "Donjon de l'autodiscipline — les boss sont tes mauvaises habitudes"},
        {"Mastery Mountain",        "La montagne des maîtres — seuls les plus constants arrivent au sommet"},
    };

    private static final String[][] BOSSES = {
        {"Procrastination Dragon",  "100", "Beat it: Complete 30 tasks", "200"},
        {"Distraction Demon",       "150", "Beat it: 7-day focus streak", "300"},
        {"Laziness Lich",           "200", "Beat it: 30-day habit streak", "500"},
    };

    public LeaAdventureMode(Context ctx) { super(ctx, LeaPlusDatabase.ADVENTURE); }

    @Override
    public void execute() {
        LeaPlusDatabase.CharStats stats = db.getCharStats();
        log("⚔️ Niveau " + stats.level + " — " + stats.xp + "/" + stats.xpNext + " XP | Monde: " + stats.world);
    }

    public LeaPlusDatabase.CharStats completeTask(String taskName, int xpAmount) {
        LeaPlusDatabase.CharStats stats = db.getCharStats();
        stats.xp        += xpAmount;
        stats.totalTasks++;
        boolean leveledUp = false;

        // Level-up loop
        while (stats.xp >= stats.xpNext) {
            stats.xp     -= stats.xpNext;
            stats.level++;
            stats.xpNext  = (int)(stats.xpNext * 1.5);
            leveledUp     = true;
            updateWorld(stats);
        }

        db.updateChar(stats.level, stats.xp, stats.xpNext, stats.hp, stats.world, stats.bossDefeated, stats.totalTasks);
        log("⚔️ «" + taskName + "» → +" + xpAmount + " XP | Total: " + (stats.xp + (leveledUp?stats.xpNext:0)));

        if (leveledUp) {
            notif.notifyLevelUp(stats.level, stats.world);
            log("🎉 LEVEL UP ! → Niveau " + stats.level + " — " + stats.world);
        }
        return stats;
    }

    private void updateWorld(LeaPlusDatabase.CharStats stats) {
        int worldIdx = Math.min((stats.level - 1) / 5, WORLDS.length - 1);
        stats.world = WORLDS[worldIdx][0];
    }

    public String getCharacterSheet() {
        LeaPlusDatabase.CharStats s = db.getCharStats();
        return "⚔️ FICHE PERSONNAGE\n\n" +
               "Niveau: " + s.level + "\n" +
               "XP: " + s.xp + " / " + s.xpNext + "\n" +
               "HP: " + s.hp + "/100\n" +
               "Monde actuel: " + s.world + "\n" +
               "Boss vaincus: " + s.bossDefeated + "\n" +
               "Total tâches: " + s.totalTasks + "\n\n" +
               "🏆 Prochain niveau: " + (s.xpNext - s.xp) + " XP restants";
    }

    public String getStoryNarration() {
        LeaPlusDatabase.CharStats s = db.getCharStats();
        return "📖 Ton héros de niveau " + s.level + " avance dans " + s.world + ".\n" +
               s.totalTasks + " défis vaincus, " + s.bossDefeated + " boss éliminés.\n" +
               "Continue l'aventure — la légende s'écrit par l'action !";
    }
}
