package com.flolov42.lea_v3.voice;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class LeaVoiceCommandManager {

    public interface VoiceCallback {
        void onResult(String command, String response);
        void onError(String error);
    }

    private static LeaVoiceCommandManager instance;
    private final Context ctx;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private SpeechRecognizer sr;

    public static synchronized LeaVoiceCommandManager get(Context ctx) {
        if (instance == null) instance = new LeaVoiceCommandManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaVoiceCommandManager(Context ctx) {
        this.ctx = ctx;
        initTTS();
    }

    private void initTTS() {
        tts = new TextToSpeech(ctx, status -> ttsReady = (status == TextToSpeech.SUCCESS));
    }

    public void speak(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void startListening(VoiceCallback callback) {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            callback.onError("Reconnaissance vocale non disponible");
            return;
        }
        if (sr != null) sr.destroy();
        sr = SpeechRecognizer.createSpeechRecognizer(ctx);
        sr.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int code) { callback.onError("Erreur " + code); }
            @Override public void onResults(Bundle results) {
                List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String cmd = matches.get(0).toLowerCase(Locale.FRANCE);
                    String response = processCommand(cmd);
                    float[] conf = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                    float accuracy = (conf != null && conf.length > 0) ? conf[0] : 0.8f;
                    LeaFeaturesDatabase.get(ctx).logVoiceCommand(cmd, response, accuracy);
                    callback.onResult(cmd, response);
                    speak(response);
                }
            }
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int type, Bundle b) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Je vous écoute...");
        intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
        sr.startListening(intent);
    }

    public void stopListening() {
        if (sr != null) { sr.stopListening(); sr.destroy(); sr = null; }
    }

    private String processCommand(String cmd) {
        LeaPlusDatabase plus = LeaPlusDatabase.get(ctx);

        if (contains(cmd, "combien", "coins", "solde")) {
            int balance = plus.getCoinBalance();
            return "Tu as " + balance + " LÉA Coins.";
        }
        if (contains(cmd, "niveau", "level", "xp")) {
            LeaPlusDatabase.CharStats stats = plus.getCharStats();
            return "Tu es niveau " + stats.level + " avec " + stats.xp + " XP.";
        }
        if (contains(cmd, "habitude", "streak")) {
            List<LeaPlusDatabase.HabitRow> habits = plus.getActiveHabits();
            if (habits.isEmpty()) return "Tu n'as pas encore d'habitudes.";
            int maxStreak = 0;
            for (LeaPlusDatabase.HabitRow h : habits) if (h.streak > maxStreak) maxStreak = h.streak;
            return "Tu as " + habits.size() + " habitudes actives. Meilleur streak: " + maxStreak + " jours.";
        }
        if (contains(cmd, "quête", "quetes", "quest")) {
            List<LeaPlusDatabase.QuestRow> quests = plus.getQuests("available");
            if (quests.isEmpty()) return "Toutes les quêtes sont complétées, bravo!";
            return "Tu as " + quests.size() + " quêtes disponibles. Prochaine: " + quests.get(0).title;
        }
        if (contains(cmd, "achievements", "badge", "trophy")) {
            String summary = com.flolov42.lea_v3.achievements.LeaAchievementManager.get(ctx).getSummary();
            return "Achievements: " + summary;
        }
        if (contains(cmd, "connexion", "internet", "ligne")) {
            boolean online = com.flolov42.lea_v3.offline.LeaOfflineManager.get(ctx).isOnline();
            return online ? "Tu es connecté à internet." : "Tu es hors ligne.";
        }
        if (contains(cmd, "thème", "theme", "couleur")) {
            String theme = com.flolov42.lea_v3.themes.LeaThemeManager.get(ctx).getCurrentTheme();
            return "Thème actuel: " + theme;
        }
        if (contains(cmd, "monde", "aventure")) {
            LeaPlusDatabase.CharStats stats = plus.getCharStats();
            return "Tu es dans le monde: " + stats.world;
        }
        if (contains(cmd, "bonjour", "salut", "hello")) {
            return "Bonjour! Je suis Léa, ton assistante personnelle. Comment puis-je t'aider?";
        }
        return "Commande non reconnue. Essayez: 'combien de coins', 'mon niveau', 'mes habitudes'.";
    }

    private boolean contains(String input, String... keywords) {
        for (String k : keywords) if (input.contains(k)) return true;
        return false;
    }

    public void destroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (sr != null) { sr.destroy(); }
    }
}
