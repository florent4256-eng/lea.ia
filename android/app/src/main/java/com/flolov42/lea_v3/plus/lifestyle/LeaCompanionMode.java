package com.flolov42.lea_v3.plus.lifestyle;

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
import java.util.List;
import java.util.Locale;

public class LeaCompanionMode extends LeaBasePlusFeature {

    private static final String PREFS = "lea_companion";

    private static final String[] MORNING_GREETINGS = {
        "Bonjour ! J'espère que tu as bien dormi 😊",
        "Coucou ! Prêt(e) pour une nouvelle journée ?",
        "Hello toi ! Comment tu te sens ce matin ?",
    };
    private static final String[] EVENING_MESSAGES = {
        "Comment s'est passée ta journée ? Raconte-moi 😊",
        "Bonsoir ! Tu as réussi à accomplir tes objectifs aujourd'hui ?",
        "Je pense à toi ce soir. Tu veux me raconter ta journée ?",
    };

    public LeaCompanionMode(Context ctx) { super(ctx, LeaPlusDatabase.COMPANION); }

    @Override
    public void execute() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        checkDailyGreeting(hour);
        checkMilestones();
    }

    private void checkDailyGreeting(int hour) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        boolean morningDone = today.equals(prefs.getString("morning_" , ""));
        boolean eveningDone = today.equals(prefs.getString("evening_" , ""));
        if (!morningDone && hour >= 7 && hour < 11) {
            String msg = MORNING_GREETINGS[(int)(System.currentTimeMillis() % MORNING_GREETINGS.length)];
            notify("☀️ LÉA te dit bonjour !", msg);
            log("☀️ Message du matin envoyé");
            prefs.edit().putString("morning_", today).apply();
        } else if (!eveningDone && hour >= 19 && hour < 22) {
            String msg = EVENING_MESSAGES[(int)(System.currentTimeMillis() % EVENING_MESSAGES.length)];
            notify("🌙 LÉA pense à toi", msg);
            log("🌙 Message du soir envoyé");
            prefs.edit().putString("evening_", today).apply();
        }
    }

    public void recordMemory(String content, String emotion) {
        db.insertCompanionMemory(content, emotion);
        log("💭 Mémoire enregistrée: " + content.substring(0, Math.min(40, content.length())) + "…");
    }

    public void recordInsideJoke(String joke) {
        db.insertCompanionMemory("INSIDE_JOKE: " + joke, "fun");
        log("😂 Inside joke ajouté: " + joke);
    }

    private void checkMilestones() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int memoryCount = db.getCompanionMemoryCount();
        int lastNotifiedAt = prefs.getInt("last_milestone", 0);
        int[] milestones = {10, 25, 50, 100, 200, 500};
        for (int m : milestones) {
            if (memoryCount >= m && lastNotifiedAt < m) {
                notify("💫 Milestone !", "Vous avez partagé " + m + " souvenirs ensemble !");
                log("🎉 Milestone mémoires: " + m);
                prefs.edit().putInt("last_milestone", m).apply();
                break;
            }
        }
    }

    public String getBondingSummary() {
        int count = db.getCompanionMemoryCount();
        List<LeaPlusDatabase.CompanionMemory> recent = db.getRecentMemories(5);
        StringBuilder sb = new StringBuilder("💙 COMPAGNON LÉA\n\n");
        sb.append("Souvenirs partagés: ").append(count).append("\n\n");
        sb.append("📖 Récents:\n");
        for (LeaPlusDatabase.CompanionMemory m : recent) {
            sb.append("• ").append(m.content, 0, Math.min(60, m.content.length())).append("\n");
        }
        if (count > 50) sb.append("\n💙 Lien fort — LÉA te connaît bien !");
        else if (count > 10) sb.append("\n😊 Votre lien grandit !");
        else sb.append("\n🌱 Le début d'une belle amitié !");
        return sb.toString();
    }

    public String reactToEmotion(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("triste") || lower.contains("mal") || lower.contains("nul"))
            return "Je suis là pour toi. Veux-tu en parler ? 💙";
        if (lower.contains("heureux") || lower.contains("super") || lower.contains("génial"))
            return "Ça me fait vraiment plaisir ! Dis-moi tout 😊✨";
        if (lower.contains("fatigué") || lower.contains("épuisé"))
            return "Prends soin de toi. Tu mérites de te reposer 🌙";
        if (lower.contains("stress") || lower.contains("anxieux"))
            return "Respire doucement. Je suis là. Qu'est-ce qui se passe ? 🌸";
        return "Je t'écoute avec attention 💙";
    }
}
