package com.flolov42.lea_v3.bixby;

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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Dispatcher central des 12 features de Léa.
 * Remplace LeaNovaCommandProcessor comme point d'entrée principal.
 * Si aucune feature ne correspond → route vers le CommandProcessor existant (IA Ollama).
 *
 * Usage dans LeaNovaService :
 *   featuresManager.dispatch(transcribedText, response -> voiceSynthesis(response));
 */
public class LeaNovaFeaturesManager {

    private static final String TAG = "LeaFeatures";

    public interface FeatureCallback { void onResult(String responseText); }

    private final Context                  ctx;
    private final LeaWeatherManager        weather;
    private final LeaAlarmManager          alarm;
    private final LeaCalendarManager       calendar;
    private final LeaCommandExecutor       commands;
    private final LeaSystemControlManager  system;
    private final LeaNotificationSummarizer notifSummary;
    private final LeaTranslationManager    translation;
    private final LeaNetworkIntegration    network;
    private final LeaConversationalProcessor conv;
    private final LeaMemoryManager         memory;
    private final LeaRecommendationEngine  reco;
    private final Handler                  mainHandler = new Handler(Looper.getMainLooper());

    // Fallback vers l'ancienne logique (WebSocket IA)
    private LeaNovaCommandProcessor commandProcessor;

    public LeaNovaFeaturesManager(Context ctx) {
        this.ctx          = ctx.getApplicationContext();
        this.weather      = new LeaWeatherManager(ctx);
        this.alarm        = new LeaAlarmManager(ctx);
        this.calendar     = new LeaCalendarManager(ctx);
        this.commands     = new LeaCommandExecutor(ctx);
        this.system       = new LeaSystemControlManager(ctx);
        this.notifSummary = new LeaNotificationSummarizer(ctx);
        this.translation  = new LeaTranslationManager(ctx);
        this.network      = new LeaNetworkIntegration(ctx);
        this.conv         = new LeaConversationalProcessor(ctx);
        this.memory       = new LeaMemoryManager(ctx);
        this.reco         = new LeaRecommendationEngine(ctx);
    }

    /** Injecte le CommandProcessor existant comme fallback IA. */
    public void setCommandProcessor(LeaNovaCommandProcessor processor) {
        this.commandProcessor = processor;
    }

    // ── Point d'entrée principal ──────────────────────────────────────────────

    public void dispatch(String input, FeatureCallback callback) {
        if (input == null || input.trim().isEmpty()) return;

        // Analyse conversationnelle
        LeaConversationalProcessor.Intent intent = conv.analyze(input);
        if (intent == null) { routeToAI(input, callback); return; }

        // Sauvegarde dans la mémoire
        int sentiment = LeaSentimentDetector.analyze(input);
        memory.remember(input, "", sentiment);

        // Enrichissement émotionnel
        String prefix = LeaSentimentDetector.reactionFor(sentiment);

        switch (intent.type) {

            // ── MÉTÉO ──────────────────────────────────────────────────────────
            case "WEATHER":
                weather.fetchWeather(msg ->
                    deliver(callback, prefix + msg));
                break;

            // ── RÉVEIL / MINUTEUR ──────────────────────────────────────────────
            case "ALARM":
                if ("SET_ALARM".equals(intent.action)) {
                    String alarmMsg = alarm.parseAlarmCommand(input);
                    deliver(callback, prefix + alarmMsg);
                } else {
                    String timerMsg = alarm.parseTimerCommand(input);
                    deliver(callback, prefix + timerMsg);
                }
                break;

            // ── CALENDRIER ────────────────────────────────────────────────────
            case "CALENDAR":
                if ("CREATE".equals(intent.action)) {
                    String calMsg = calendar.parseCreateCommand(input);
                    deliver(callback, prefix + calMsg);
                } else {
                    String calRead = calendar.getEventsToday();
                    deliver(callback, prefix + calRead);
                }
                break;

            // ── APPEL ─────────────────────────────────────────────────────────
            case "CALL": {
                String name = intent.subject.isEmpty()
                    ? extractAfter(input.toLowerCase(), new String[]{"appelle ","téléphone à "})
                    : intent.subject;
                String callMsg = commands.makeCall(name);
                deliver(callback, prefix + callMsg);
                break;
            }

            // ── SMS ───────────────────────────────────────────────────────────
            case "SMS": {
                String name = intent.subject.isEmpty()
                    ? extractAfter(input.toLowerCase(), new String[]{"message à ","sms à ","écris à "})
                    : intent.subject;
                String smsMsg = commands.openSmsComposer(name, intent.content);
                deliver(callback, prefix + smsMsg);
                break;
            }

            // ── CONTRÔLE SYSTÈME ──────────────────────────────────────────────
            case "SYSTEM": {
                String sysResult = system.processCommand(intent.action.isEmpty() ? input : intent.action);
                if (sysResult != null) {
                    deliver(callback, prefix + sysResult);
                } else {
                    routeToAI(input, callback);
                }
                break;
            }

            // ── TRADUCTION / DEVISE ────────────────────────────────────────────
            case "TRANSLATE":
                translation.parseTranslateCommand(input, msg ->
                    deliver(callback, prefix + msg));
                break;

            // ── RÉSEAUX SOCIAUX ────────────────────────────────────────────────
            case "SOCIAL": {
                String socialResult = network.parseCommand(input);
                if (socialResult != null) deliver(callback, prefix + socialResult);
                else routeToAI(input, callback);
                break;
            }

            // ── OUVRE APP ─────────────────────────────────────────────────────
            case "OPEN_APP": {
                String appResult = commands.openApp(intent.subject);
                if (appResult.contains("introuvable")) {
                    // Essaie aussi music/gallery/maps
                    String c = input.toLowerCase();
                    if (c.contains("musique") || c.contains("spotify") || c.contains("music"))
                        deliver(callback, prefix + commands.openMusic(extractQuery(c)));
                    else if (c.contains("galerie") || c.contains("photos"))
                        deliver(callback, prefix + commands.openGallery());
                    else if (c.contains("maps") || c.contains("navigation") || c.contains("itinéraire"))
                        deliver(callback, prefix + commands.openMaps(extractAfter(c, new String[]{"aller à ","vers ","pour "})));
                    else
                        deliver(callback, prefix + appResult);
                } else {
                    deliver(callback, prefix + appResult);
                }
                break;
            }

            // ── RÉSUMÉ NOTIFICATIONS ───────────────────────────────────────────
            case "SUMMARY":
                deliver(callback, prefix + notifSummary.getSummary());
                break;

            // ── CONVERSATIONNEL / ÉTATS ────────────────────────────────────────
            case "CONVERSATIONAL":
                if (intent.content != null && !intent.content.isEmpty()) {
                    // Ajoute la recommandation proactive si disponible
                    String hint = reco.getQuickHint();
                    String full = intent.content + (hint.isEmpty() ? "" : " " + hint);
                    deliver(callback, full);
                } else {
                    routeToAI(input, callback);
                }
                break;

            // ── INCONNU → IA ──────────────────────────────────────────────────
            default:
                routeToAI(input, callback);
                break;
        }
    }

    // ── Fallback IA ───────────────────────────────────────────────────────────

    private void routeToAI(String input, FeatureCallback callback) {
        if (commandProcessor != null) {
            commandProcessor.setCallback(msg -> deliver(callback, msg));
            boolean handled = commandProcessor.process(input);
            if (!handled) {
                // Enrichit avec le contexte et envoie à Ollama
                commandProcessor.routeToAI(input);
            }
        } else {
            deliver(callback, "Connexion au serveur Léa en attente…");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void deliver(FeatureCallback cb, String msg) {
        mainHandler.post(() -> { if (cb != null) cb.onResult(msg); });
    }

    private String extractAfter(String cmd, String[] keywords) {
        for (String kw : keywords) {
            int idx = cmd.indexOf(kw);
            if (idx >= 0) return cmd.substring(idx + kw.length()).trim();
        }
        return "";
    }

    private String extractQuery(String cmd) {
        return cmd.replaceAll("(joue|mets|lance|écoute|la musique|une chanson|ouvre)", "").trim();
    }

    // ── Analyse proactive (appelée au démarrage) ──────────────────────────────

    public String getProactiveGreeting() {
        String hint = reco.buildProactiveMessage();
        if (!hint.isEmpty()) return hint;
        return "";
    }
}
