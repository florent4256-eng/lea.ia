package com.flolov42.lea_v3.core;

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
import java.util.ArrayList;
import java.util.List;

public class LeaPlusManager {

    // ── Tab constants ─────────────────────────────────────────────────────────
    public static final String TAB_GAMIFICATION = "GAMIFICATION";
    public static final String TAB_LIFESTYLE    = "LIFESTYLE";
    public static final String TAB_LEARNING     = "LEARNING";
    public static final String TAB_PREMIUM      = "PREMIUM";
    public static final String TAB_CONNECT      = "CONNECT";

    // ── Feature metadata ──────────────────────────────────────────────────────
    public static class FeatureInfo {
        public final String id, name, icon, description, tab;
        public final int    color;
        public FeatureInfo(String id, String name, String icon, String description, String tab, int color) {
            this.id=id; this.name=name; this.icon=icon; this.description=description; this.tab=tab; this.color=color;
        }
    }

    public static final FeatureInfo[] ALL_FEATURES = {
        // GAMIFICATION
        new FeatureInfo(LeaPlusDatabase.QUESTS,      "Quêtes & Missions",    "🎯", "Quêtes quotidiennes/hebdomadaires avec XP, badges et récompenses",               TAB_GAMIFICATION, 0xFFFFD700),
        new FeatureInfo(LeaPlusDatabase.ADVENTURE,   "Mode Aventure RPG",    "⚔️", "Chaque tâche complétée = progression dans ton aventure fantasy",                  TAB_GAMIFICATION, 0xFF9C27B0),
        new FeatureInfo(LeaPlusDatabase.COINS,       "Léa Coins",            "💰", "Monnaie interne : gagne et dépense des coins dans la boutique",                    TAB_GAMIFICATION, 0xFFFF9800),
        // LIFESTYLE
        new FeatureInfo(LeaPlusDatabase.HABITS,      "Habit Tracker",        "🔗", "Suivi scientifique des habitudes avec streaks et statistiques",                    TAB_LIFESTYLE,    0xFF4CAF50),
        new FeatureInfo(LeaPlusDatabase.REPORT,      "Rapport Annuel",       "📊", "Ton récap annuel personnalisé (style Spotify Wrapped)",                            TAB_LIFESTYLE,    0xFF2196F3),
        new FeatureInfo(LeaPlusDatabase.COMPANION,   "Virtual Companion",    "🤝", "Léa développe une vraie relation avec toi via la mémoire conversationnelle",       TAB_LIFESTYLE,    0xFFE91E63),
        new FeatureInfo(LeaPlusDatabase.LIFE_OS,     "Life OS",              "🖥️", "Léa gère toute ta journée : réveil, planning, pauses, nutrition, sommeil",         TAB_LIFESTYLE,    0xFF00BCD4),
        // LEARNING
        new FeatureInfo(LeaPlusDatabase.STUDENT,     "Student Assistant",    "📚", "Aide aux devoirs, quiz, planification examen, prédicateur de notes",              TAB_LEARNING,     0xFF3F51B5),
        new FeatureInfo(LeaPlusDatabase.LANGUAGE,    "Language Learner",     "🌐", "Apprentissage des langues avec vocab quotidien et analyse d'accent",               TAB_LEARNING,     0xFF009688),
        new FeatureInfo(LeaPlusDatabase.SMART_NOTIF, "Smart Notifications",  "🔔", "Notifs groupées, filtrées et optimisées selon tes habitudes",                      TAB_LEARNING,     0xFF607D8B),
        // PREMIUM
        new FeatureInfo(LeaPlusDatabase.CLOUD_SYNC,  "Cloud Sync",           "☁️", "Synchronisation chiffrée cross-appareils avec backup quotidien",                   TAB_PREMIUM,      0xFF00E5FF),
        new FeatureInfo(LeaPlusDatabase.MARKETPLACE, "Marketplace Skills",   "🛒", "Boutique de skills créés par la communauté (gratuit + payant)",                    TAB_PREMIUM,      0xFF7B2CBF),
        new FeatureInfo(LeaPlusDatabase.FAMILY,      "Family & Parental",    "👨‍👩‍👧", "Contrôle parental, comptes familiaux, temps d'écran, contenu filtré",           TAB_PREMIUM,      0xFFF44336),
        // CONNECT
        new FeatureInfo(LeaPlusDatabase.OMNICHANNEL, "Omnichannel",          "🌐", "Léa sur PC, montre connectée, domotique, Alexa et Google Home",                   TAB_CONNECT,      0xFF4CAF50),
        new FeatureInfo(LeaPlusDatabase.STREAMING,   "Creator Streaming",    "🎥", "Stream avec Léa sur Twitch/YouTube, interaction live avec le chat",               TAB_CONNECT,      0xFFFF5722),
    };

    // ── Observer pattern ──────────────────────────────────────────────────────
    public interface StateListener {
        void onFeatureStateChanged(String featureId, boolean enabled);
    }
    private final List<StateListener> listeners = new ArrayList<>();
    public void registerListener(StateListener l)   { if (!listeners.contains(l)) listeners.add(l); }
    public void unregisterListener(StateListener l) { listeners.remove(l); }
    private void notifyListeners(String id, boolean enabled) {
        for (StateListener l : listeners) l.onFeatureStateChanged(id, enabled);
    }

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static LeaPlusManager instance;
    public static synchronized LeaPlusManager get(Context ctx) {
        if (instance == null) instance = new LeaPlusManager(ctx.getApplicationContext());
        return instance;
    }

    private final Context              ctx;
    private final LeaPlusDatabase      db;
    private final LeaPlusNotifications notif;

    private LeaPlusManager(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaPlusDatabase.get(ctx);
        this.notif = LeaPlusNotifications.get(ctx);
    }

    // ── Feature state ─────────────────────────────────────────────────────────
    public boolean isEnabled(String id) { return db.isEnabled(id); }

    public void enable(String id) {
        db.setEnabled(id, true);
        db.log(id, "✅ Feature activée");
        notifyListeners(id, true);
        FeatureInfo info = getInfo(id);
        if (info != null) notif.notify(id, info.icon + " " + info.name + " activé", info.description);
    }

    public void disable(String id) {
        db.setEnabled(id, false);
        db.log(id, "⏹ Feature désactivée");
        notifyListeners(id, false);
    }

    public FeatureInfo getInfo(String id) {
        for (FeatureInfo f : ALL_FEATURES) { if (f.id.equals(id)) return f; }
        return null;
    }

    public List<FeatureInfo> getFeaturesForTab(String tab) {
        List<FeatureInfo> list = new ArrayList<>();
        for (FeatureInfo f : ALL_FEATURES) { if (tab.equals(f.tab)) list.add(f); }
        return list;
    }

    // ── Execute all enabled features ──────────────────────────────────────────
    public void executeAll() {
        for (FeatureInfo f : ALL_FEATURES) {
            if (db.isEnabled(f.id)) {
                try { getFeature(f.id).execute(); }
                catch (Exception e) { db.log(f.id, "⚠️ " + e.getMessage()); }
            }
        }
    }

    private LeaBasePlusFeature getFeature(String id) {
        switch (id) {
            case LeaPlusDatabase.QUESTS:      return new LeaQuestSystem(ctx);
            case LeaPlusDatabase.ADVENTURE:   return new LeaAdventureMode(ctx);
            case LeaPlusDatabase.COINS:       return new LeaCoinSystem(ctx);
            case LeaPlusDatabase.HABITS:      return new LeaHabitTracker(ctx);
            case LeaPlusDatabase.REPORT:      return new LeaAnnualReport(ctx);
            case LeaPlusDatabase.COMPANION:   return new LeaCompanionMode(ctx);
            case LeaPlusDatabase.LIFE_OS:     return new LeaLifeOS(ctx);
            case LeaPlusDatabase.STUDENT:     return new LeaStudentAssistant(ctx);
            case LeaPlusDatabase.LANGUAGE:    return new LeaLanguageLearner(ctx);
            case LeaPlusDatabase.SMART_NOTIF: return new LeaSmartNotifications(ctx);
            case LeaPlusDatabase.CLOUD_SYNC:  return new LeaCloudSync(ctx);
            case LeaPlusDatabase.MARKETPLACE: return new LeaMarketplace(ctx);
            case LeaPlusDatabase.FAMILY:      return new LeaFamilyMode(ctx);
            case LeaPlusDatabase.OMNICHANNEL: return new LeaOmnichannelIntegration(ctx);
            case LeaPlusDatabase.STREAMING:   return new LeaStreamingMode(ctx);
            default:                          return new LeaBasePlusFeature(ctx, id) { @Override public void execute() {} };
        }
    }

    public List<LeaPlusDatabase.LogRow> getLogs(String featureId) {
        return db.getLogs(featureId, 20);
    }

    // ── Cross-feature coordination ────────────────────────────────────────────

    /** Called when any task is completed — awards coins + XP across all active systems */
    public void onTaskCompleted(String taskName, int difficultyLevel) {
        int xpAward    = difficultyLevel * 25;
        int coinAward  = difficultyLevel * 10;

        if (isEnabled(LeaPlusDatabase.COINS))   { new LeaCoinSystem(ctx).addCoins(coinAward, "Tâche: " + taskName); }
        if (isEnabled(LeaPlusDatabase.ADVENTURE)){ new LeaAdventureMode(ctx).completeTask(taskName, xpAward); }
        db.log("SYSTEM", "🎯 Tâche «" + taskName + "» → +" + xpAward + " XP, +" + coinAward + " coins");
    }
}
