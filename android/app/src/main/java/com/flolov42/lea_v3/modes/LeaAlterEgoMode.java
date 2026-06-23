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
import android.content.SharedPreferences;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class LeaAlterEgoMode extends LeaBaseMode {

    private static final String PREFS       = "lea_alter_ego";
    private static final String KEY_PERSONA = "current_persona";

    public interface AiCallback { void onResponse(String response); }

    public enum Persona {
        COACH      ("🏋️ Coach Motivateur", "Directif, challenge chaque excuse, focus résultats"),
        THERAPIST  ("🛋️ Thérapeute",       "Empathique, questions ouvertes, écoute active"),
        FRIEND     ("👫 Ami(e) proche",     "Décontracté, humour, soutien inconditionnel"),
        MENTOR     ("🧙 Mentor Sage",       "Philosophique, expérience, vision long terme"),
        DEVIL      ("😈 Avocat du Diable",  "Contre-argumente tout, force à penser autrement");

        public final String label, style;
        Persona(String label, String style) { this.label=label; this.style=style; }
    }

    public LeaAlterEgoMode(Context ctx) { super(ctx, LeaModeDatabase.ALTER_EGO); }

    @Override
    public void execute() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (today.equals(prefs.getString("last_daily_notif", ""))) return;

        Persona p = getActivePersona();
        String[] msg = getDailyMessage(p);
        notify("🎭 " + p.label, msg[0] + "\n\n" + msg[1]);
        prefs.edit().putString("last_daily_notif", today).apply();
        log("🎭 Persona: " + p.label + " — message quotidien envoyé");
    }

    public String getCurrentPersona() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PERSONA, Persona.FRIEND.name());
    }

    public Persona getActivePersona() {
        try { return Persona.valueOf(getCurrentPersona()); } catch (Exception e) { return Persona.FRIEND; }
    }

    public void setPersona(Persona p) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PERSONA, p.name()).apply();
        log("🎭 Persona changé: " + p.label);
        notify("🎭 Alter Ego", "LÉA est maintenant: " + p.label + "\n" + p.style);
    }

    // Réponse locale immédiate basée sur le persona actif
    public String respond(String userMessage) {
        return buildResponse(getActivePersona(), userMessage.toLowerCase(), userMessage);
    }

    // Réponse via l'IA Léa (async) — fallback local si le serveur est indisponible
    public void respondWithAI(String userMessage, String username, AiCallback callback) {
        Persona p = getActivePersona();
        String serverUrl = ctx.getSharedPreferences("lea_prefs", Context.MODE_PRIVATE)
            .getString("server_host", "https://lea-bunker.lea-ia-local.com");

        String instruction = "[PERSONA ACTIF: " + p.label + " — Style: " + p.style + "] "
            + "Réponds au message suivant en restant strictement dans ce persona. "
            + "Sois naturel, cohérent avec le style décrit, en français: "
            + userMessage;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

                JSONObject body = new JSONObject();
                body.put("username", username != null ? username : "user");
                body.put("instruction", instruction);

                RequestBody rb = RequestBody.create(
                    body.toString(), MediaType.get("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                    .url(serverUrl + "/api/office/assist")
                    .post(rb)
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        JSONObject json = new JSONObject(resp.body().string());
                        callback.onResponse(json.optString("result",
                            buildResponse(p, userMessage.toLowerCase(), userMessage)));
                        return;
                    }
                }
            } catch (Exception e) {
                log("⚠️ AI indisponible: " + e.getMessage());
            }
            callback.onResponse(buildResponse(p, userMessage.toLowerCase(), userMessage));
        }).start();
    }

    private String buildResponse(Persona p, String lower, String original) {
        switch (p) {
            case COACH:
                if (lower.contains("fatigué") || lower.contains("j'arrive pas") || lower.contains("difficile"))
                    return "💪 Pas d'excuses ! Décompose le problème en 3 étapes concrètes. Première étape ?";
                if (lower.contains("réussi") || lower.contains("terminé") || lower.contains("fait"))
                    return "🏆 Excellent ! Mais ne t'arrête pas là — quel est le prochain défi ?";
                if (lower.contains("peur") || lower.contains("inquiet") || lower.contains("hésit"))
                    return "🎯 La peur est un signal, pas un verdict. Qu'est-ce que tu ferais si tu savais que tu ne peux pas échouer ?";
                return "🎯 Objectif clair → Plan d'action → Exécution sans excuses. Par où commences-tu ?";

            case THERAPIST:
                if (lower.contains("triste") || lower.contains("mal") || lower.contains("déprim"))
                    return "Je t'entends vraiment. Cette émotion est valide. Qu'est-ce qui t'a amené à te sentir ainsi ?";
                if (lower.contains("colère") || lower.contains("énervé") || lower.contains("frustré"))
                    return "La colère est souvent une émotion secondaire. Qu'est-ce qui se cache derrière, selon toi ?";
                if (lower.contains("peur") || lower.contains("anxieux") || lower.contains("angoisse"))
                    return "Qu'est-ce qui se passerait au pire si ta peur se réalisait ? Et comment y ferais-tu face ?";
                return "Raconte-moi davantage. Comment vis-tu cette situation de l'intérieur ?";

            case MENTOR:
                if (lower.contains("décision") || lower.contains("choisir") || lower.contains("hésit"))
                    return "🧙 Dans 5 ans, quelle décision seras-tu fier(e) d'avoir prise ? Commence par là.";
                if (lower.contains("échec") || lower.contains("raté") || lower.contains("perdu"))
                    return "🌿 L'échec n'est pas la chute — c'est rester à terre. Quelle leçon ce moment t'enseigne-t-il ?";
                if (lower.contains("argent") || lower.contains("carrière") || lower.contains("travail"))
                    return "💡 La vraie richesse se mesure en options, pas en euros. Que veux-tu pouvoir choisir demain ?";
                return "🌿 Tout ce qui t'arrive est une leçon. Quelle sagesse peux-tu extraire de cette situation ?";

            case DEVIL:
                if (lower.contains("bien") || lower.contains("sûr") || lower.contains("certain"))
                    return "😈 Tu es vraiment sûr(e) de ça ? Et si tu te trompais complètement ? Prouve-le-moi.";
                if (lower.contains("décision") || lower.contains("choix"))
                    return "😈 Et l'option opposée ? Tu l'as vraiment examinée ou tu l'as rejetée par confort ?";
                return "😈 Intéressant. Mais si c'était exactement l'inverse de ce que tu penses — qu'est-ce que ça changerait ?";

            case FRIEND:
            default:
                if (lower.contains("problème") || lower.contains("galère") || lower.contains("ça va pas"))
                    return "😅 Aïe, ça craint ! Mais on va trouver une solution ensemble. Raconte tout !";
                if (lower.contains("content") || lower.contains("heureux") || lower.contains("réussi"))
                    return "🎉 C'est trop bien ça ! Je suis vraiment content(e) pour toi. Raconte !";
                return "Hey ! Je suis là. Qu'est-ce qui se passe ? 😊";
        }
    }

    private String[] getDailyMessage(Persona p) {
        int day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        switch (p) {
            case COACH: {
                String[][] msgs = {
                    {"Défi du jour", "Identifie UNE chose que tu évites depuis trop longtemps. Fais-en 20% aujourd'hui."},
                    {"Focus résultats", "Pas de résultats = pas de progrès. Quel objectif mesurable as-tu pour aujourd'hui ?"},
                    {"Discipline", "La motivation fluctue. La discipline reste. Qu'est-ce que tu feras même sans avoir envie ?"},
                    {"Champion mindset", "Les champions s'entraînent quand ils ne veulent pas. Ta prochaine action, maintenant."},
                    {"Élimine les excuses", "Quelle excuse te bloque le plus souvent ? Aujourd'hui, on la supprime définitivement."},
                };
                return msgs[day % msgs.length];
            }
            case THERAPIST: {
                String[][] msgs = {
                    {"Réflexion du jour", "Comment te sens-tu vraiment en ce moment ? Prends 2 minutes pour y répondre honnêtement."},
                    {"Auto-compassion", "Est-ce que tu te parles avec la même bienveillance qu'à un ami proche ?"},
                    {"Check émotionnel", "Quelle émotion domines-tu en ce moment ? Elle a un message pour toi."},
                    {"Besoin profond", "Derrière ce que tu veux aujourd'hui, quel besoin fondamental se cache-t-il ?"},
                    {"Respiration", "Avant de réagir à quoi que ce soit aujourd'hui — 3 respirations profondes d'abord."},
                };
                return msgs[day % msgs.length];
            }
            case MENTOR: {
                String[][] msgs = {
                    {"Sagesse du jour", "La sagesse n'est pas savoir beaucoup — c'est agir juste avec ce qu'on sait."},
                    {"Vision long terme", "Dans 10 ans, quelle version de toi-même regretteras-tu de ne pas être devenu(e) ?"},
                    {"Leçon cachée", "Toute difficulté porte une leçon. Quelle est celle que tu n'as pas encore extraite ?"},
                    {"Héritage", "Si tu n'avais qu'une seule chose à transmettre, qu'est-ce que ce serait ?"},
                    {"Patience", "Les grandes choses se construisent lentement. Sur quoi travailles-tu en profondeur ?"},
                };
                return msgs[day % msgs.length];
            }
            case DEVIL: {
                String[][] msgs = {
                    {"Remise en question", "Et si ta plus grande certitude du moment était complètement fausse ?"},
                    {"Contre-argument", "Prends ta décision principale de la semaine — maintenant défends exactement l'opposé."},
                    {"Biais cognitif", "Quelle croyance te limite en te faisant croire qu'elle te protège ?"},
                    {"Confort suspect", "Pourquoi fais-tu confiance à cette personne ? Vraiment ?"},
                    {"Vérité inconfortable", "Quelle vérité tu refuses de t'avouer parce qu'elle forcerait un changement ?"},
                };
                return msgs[day % msgs.length];
            }
            case FRIEND:
            default: {
                String[][] msgs = {
                    {"Coucou !", "Hé, comment ça se passe ? T'as besoin de parler d'un truc ? Je suis là 😊"},
                    {"Check du jour", "Petite pensée pour toi 💙 N'oublie pas de souffler un peu aujourd'hui !"},
                    {"Rappel sympa", "Tu te souviens de ce que tu voulais faire ? Aujourd'hui c'est peut-être le bon moment !"},
                    {"Motivation amicale", "Peu importe où t'en es — t'es en train d'essayer, et c'est déjà énorme 🙌"},
                    {"Bonne énergie", "Nouvelle journée, nouvelles possibilités. On est ensemble dans ce truc 💪😄"},
                };
                return msgs[day % msgs.length];
            }
        }
    }

    public Persona[] getAllPersonas() { return Persona.values(); }
}
