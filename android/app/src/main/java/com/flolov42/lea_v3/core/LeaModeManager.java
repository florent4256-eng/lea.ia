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

public class LeaModeManager {

    // ── Real-time state observer ──────────────────────────────────────────────

    public interface StateListener {
        void onModeStateChanged(String modeId, boolean enabled);
    }

    private final List<StateListener> listeners = new ArrayList<>();

    public void registerListener(StateListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void unregisterListener(StateListener l) {
        listeners.remove(l);
    }

    private void notifyListeners(String modeId, boolean enabled) {
        for (StateListener l : listeners) l.onModeStateChanged(modeId, enabled);
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static LeaModeManager instance;
    public static synchronized LeaModeManager get(Context ctx) {
        if (instance == null) instance = new LeaModeManager(ctx.getApplicationContext());
        return instance;
    }

    // ── Métadonnées de tous les modes ─────────────────────────────────────────
    public static class ModeInfo {
        public final String id, name, icon, description;
        public final int    color;
        public ModeInfo(String id, String name, String icon, String description, int color) {
            this.id=id; this.name=name; this.icon=icon; this.description=description; this.color=color;
        }
    }

    public static final ModeInfo[] ALL_MODES = {
        new ModeInfo(LeaModeDatabase.DUPLICATE,
            "Duplicate de Toi", "🪞",
            "Apprend ton style d'écriture et clone tes réponses",
            0xFF7C4DFF),
        new ModeInfo(LeaModeDatabase.MENTAL_HEALTH,
            "Thérapie Vocale", "🧠",
            "Détecte stress et émotions, guide des exercices de bien-être",
            0xFF00BCD4),
        new ModeInfo(LeaModeDatabase.INTERVIEW,
            "Simulation Entrevues", "💼",
            "Simule des entretiens d'embauche avec feedback intelligent",
            0xFF4CAF50),
        new ModeInfo(LeaModeDatabase.VOICE_BIO,
            "Biométrie Vocale", "🎙️",
            "Analyse la voix pour détecter stress, mensonge et humeur",
            0xFFFF5722),
        new ModeInfo(LeaModeDatabase.FUTURE,
            "Prédiction Futur", "🔮",
            "Prédit tes patterns futurs basé sur l'historique",
            0xFF9C27B0),
        new ModeInfo(LeaModeDatabase.DREAM,
            "Dream Journal", "🌙",
            "Journal de rêves avec analyse psychologique",
            0xFF3F51B5),
        new ModeInfo(LeaModeDatabase.ALTER_EGO,
            "Alter Ego", "🎭",
            "Transforme LÉA en coach, thérapeute ou ami selon tes besoins",
            0xFFE91E63),
        new ModeInfo(LeaModeDatabase.NEGOTIATION,
            "Négociation", "🤝",
            "Analyse contrats et génère des scripts de négociation",
            0xFF795548),
        new ModeInfo(LeaModeDatabase.RELATIONS,
            "Relation Tracker", "❤️",
            "Suit la santé de tes relations et suggère des actions",
            0xFFF44336),
        new ModeInfo(LeaModeDatabase.CREATIVE,
            "Mode Créatif", "✨",
            "Génère histoires, poèmes et chansons personnalisés",
            0xFFFF9800),
    };

    private final Context               ctx;
    private final LeaModeDatabase       db;
    private final LeaModeNotifications  notif;

    private LeaModeManager(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaModeDatabase.get(ctx);
        this.notif = LeaModeNotifications.get(ctx);
    }

    // ── État ──────────────────────────────────────────────────────────────────

    public boolean isEnabled(String modeId) { return db.isEnabled(modeId); }

    public void enable(String modeId) {
        db.setEnabled(modeId, true);
        db.log(modeId, "✅ Mode activé");
        notifyListeners(modeId, true);
        ModeInfo info = getInfo(modeId);
        if (info != null) notif.notify(modeId, info.icon + " " + info.name + " activé",
            info.description);
    }

    public void disable(String modeId) {
        db.setEnabled(modeId, false);
        db.log(modeId, "⏹ Mode désactivé");
        notifyListeners(modeId, false);
        notif.cancel(modeId);
    }

    public ModeInfo getInfo(String modeId) {
        for (ModeInfo m : ALL_MODES) { if (m.id.equals(modeId)) return m; }
        return null;
    }

    // ── Exécution ─────────────────────────────────────────────────────────────

    public void executeAll() {
        for (ModeInfo m : ALL_MODES) {
            if (db.isEnabled(m.id)) {
                try { getMode(m.id).execute(); }
                catch (Exception e) { db.log(m.id, "⚠️ Erreur: " + e.getMessage()); }
            }
        }
    }

    public void executeMode(String modeId) {
        if (!db.isEnabled(modeId)) return;
        try { getMode(modeId).execute(); }
        catch (Exception e) { db.log(modeId, "⚠️ Erreur: " + e.getMessage()); }
    }

    private LeaBaseMode getMode(String modeId) {
        switch (modeId) {
            case LeaModeDatabase.DUPLICATE:     return new LeaDuplicateMode(ctx);
            case LeaModeDatabase.MENTAL_HEALTH: return new LeaMentalHealthMode(ctx);
            case LeaModeDatabase.INTERVIEW:     return new LeaInterviewMode(ctx);
            case LeaModeDatabase.VOICE_BIO:     return new LeaVoiceBiometricMode(ctx);
            case LeaModeDatabase.FUTURE:        return new LeaFutureMode(ctx);
            case LeaModeDatabase.DREAM:         return new LeaDreamMode(ctx);
            case LeaModeDatabase.ALTER_EGO:     return new LeaAlterEgoMode(ctx);
            case LeaModeDatabase.NEGOTIATION:   return new LeaNegotiationMode(ctx);
            case LeaModeDatabase.RELATIONS:     return new LeaRelationTrackerMode(ctx);
            case LeaModeDatabase.CREATIVE:      return new LeaCreativeMode(ctx);
            default: return new LeaBaseMode(ctx, modeId) {
                @Override public void execute() { db.log(modeId, "Mode inconnu"); }
            };
        }
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    public List<LeaModeDatabase.LogRow> getLogs(String modeId) {
        return db.getLogs(modeId, 20);
    }
}
