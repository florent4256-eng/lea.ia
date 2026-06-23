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


import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaDuplicateMode extends LeaBaseMode {

    public LeaDuplicateMode(Context ctx) { super(ctx, LeaModeDatabase.DUPLICATE); }

    @Override
    public void execute() {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            log("🔒 READ_SMS requis pour apprendre le style");
            return;
        }
        // Protection anti-doublon : 1 apprentissage par jour maximum
        android.content.SharedPreferences prefs =
            ctx.getSharedPreferences("lea_duplicate", android.content.Context.MODE_PRIVATE);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (today.equals(prefs.getString("last_learn_day", ""))) return;

        learnStyle();
        prefs.edit().putString("last_learn_day", today).apply();
    }

    private void learnStyle() {
        Cursor c = null;
        try {
            Uri smsUri = Uri.parse("content://sms/sent");
            ContentResolver cr = ctx.getContentResolver();
            // Note: ContentResolver ignore le LIMIT dans le tri — on limite manuellement
            c = cr.query(smsUri, new String[]{"body"}, null, null, "date DESC");

            if (c == null) { log("📭 Aucun SMS envoyé accessible"); return; }

            Map<String, Integer> wordFreq   = new HashMap<>();
            int totalSms = 0, avgLength = 0;
            int emojiCount = 0;
            Pattern emojiPattern = Pattern.compile("[\\x{1F300}-\\x{1F9FF}]", Pattern.UNICODE_CHARACTER_CLASS);

            while (c.moveToNext() && totalSms < 200) {
                String body = c.getString(0);
                if (body == null || body.isEmpty()) continue;
                totalSms++;
                avgLength += body.length();

                Matcher em = emojiPattern.matcher(body);
                while (em.find()) emojiCount++;

                for (String word : body.split("\\s+")) {
                    String w = word.toLowerCase().replaceAll("[^a-zàâçéèêëîïôûùüÿæœ']", "");
                    if (w.length() > 3) wordFreq.merge(w, 1, Integer::sum);
                }
            }

            if (totalSms == 0) { log("📭 Pas assez de SMS pour apprendre"); return; }

            avgLength = avgLength / totalSms;
            String topWord = wordFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("aucun");

            db.upsertStylePattern("avg_length", String.valueOf(avgLength), 1.0);
            db.upsertStylePattern("emoji_rate", String.valueOf((double) emojiCount / totalSms), 1.0);
            db.upsertStylePattern("top_word",   topWord, 1.0);

            int count = db.getStylePatternCount();
            log("✅ Style appris sur " + totalSms + " SMS — mot fréquent: \"" + topWord +
                "\" | longueur moy: " + avgLength + " chars | " + count + " patterns stockés");

        } catch (Exception e) {
            log("⚠️ Erreur apprentissage style: " + e.getMessage());
        } finally {
            if (c != null) c.close();
        }
    }

    // Génère une réponse dans le style appris (appelée depuis l'UI)
    public String generateInMyStyle(String context, String topic) {
        int count = db.getStylePatternCount();
        if (count < 3) return "🪞 Pas encore assez de données — laisse le mode actif quelques jours !";

        String topWord   = db.getStylePattern("top_word");
        String avgLenStr = db.getStylePattern("avg_length");
        String emojiStr  = db.getStylePattern("emoji_rate");

        int    avgLen    = avgLenStr != null ? safeInt(avgLenStr, 60) : 60;
        double emojiRate = emojiStr  != null ? safeDouble(emojiStr, 0.1) : 0.1;

        // Construire une réponse respectant la longueur moyenne et le style emoji
        StringBuilder sb = new StringBuilder();
        if (topic != null && !topic.isEmpty()) sb.append(topic).append(" — ");
        if (context != null && !context.isEmpty()) sb.append(context);
        if (sb.length() == 0) sb.append("Ok, je vois");

        String base = sb.toString();
        if (base.length() > avgLen && avgLen > 20)
            base = base.substring(0, avgLen - 3) + "...";

        if (emojiRate > 0.8)      base += " 😊🙌";
        else if (emojiRate > 0.4) base += " 👍";

        String styleDesc = "longueur ~" + avgLen + " caractères"
            + (emojiRate > 0.3 ? ", avec emojis" : ", sans emoji")
            + (topWord != null ? ", mot fréquent: \"" + topWord + "\"" : "");

        return "🪞 Dans ton style [" + styleDesc + "]:\n\n" + base;
    }

    // Résumé du style appris
    public String getStyleSummary() {
        int count = db.getStylePatternCount();
        if (count == 0) return "🪞 Aucun style appris — active ce mode avec READ_SMS accordé.";

        String topWord   = db.getStylePattern("top_word");
        String avgLenStr = db.getStylePattern("avg_length");
        String emojiStr  = db.getStylePattern("emoji_rate");

        int avgLen = avgLenStr != null ? safeInt(avgLenStr, 0) : 0;
        double emojiRate = emojiStr != null ? safeDouble(emojiStr, 0.0) : 0.0;

        return "🪞 PROFIL DE STYLE (basé sur tes SMS)\n\n"
            + "📏 Longueur moyenne: " + avgLen + " caractères\n"
            + "😊 Taux d'emoji: " + (int)(emojiRate * 100) + "%\n"
            + "📝 Mot le plus fréquent: " + (topWord != null ? "\"" + topWord + "\"" : "—") + "\n\n"
            + (emojiRate > 0.5 ? "Tu écris court et expressif 🎯"
              : avgLen > 80 ? "Tu écris des messages détaillés 📖"
              : "Style équilibré et direct ✅");
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    private double safeDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
