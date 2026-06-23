package com.flolov42.lea_v3.utilities;

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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

/**
 * Intégration réseaux sociaux via Intents Android.
 * Ouvre les apps natives avec pré-remplissage du contenu.
 */
public class LeaNetworkIntegration {

    private static final String TAG = "LeaNetwork";
    private final Context ctx;

    public LeaNetworkIntegration(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Instagram ─────────────────────────────────────────────────────────────

    public String openInstagramPost(String text) {
        // Instagram ne supporte pas l'auto-fill de légende via intent standard
        // On ouvre l'app + copie le texte dans le clipboard pour faciliter
        if (text != null && !text.isEmpty()) copyToClipboard(text);
        return openAppWithFallback(
            "com.instagram.android", null,
            "https://www.instagram.com",
            text != null && !text.isEmpty()
                ? "📸 Instagram ouvert. Ton texte est copié — colle-le dans ta légende."
                : "📸 Instagram ouvert.");
    }

    public String openInstagramStory() {
        try {
            Intent i = new Intent("com.instagram.android");
            i.setPackage("com.instagram.android");
            i.setAction(Intent.ACTION_SEND);
            i.setType("image/*");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "📸 Instagram Stories ouvert.";
        } catch (Exception e) {
            return openInstagramPost(null);
        }
    }

    // ── Twitter / X ───────────────────────────────────────────────────────────

    public String openTwitterPost(String text) {
        // Twitter supporte le deep link avec texte pré-rempli
        try {
            String url = "https://twitter.com/intent/tweet?text=" + Uri.encode(
                text != null ? text : "");
            // Essaie d'abord l'app native
            Intent tweetIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            tweetIntent.setPackage("com.twitter.android");
            tweetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(tweetIntent);
            return "🐦 Twitter ouvert avec ton message.";
        } catch (Exception e) {
            return openAppWithFallback(
                "com.twitter.android", null,
                "https://twitter.com",
                text != null ? "🐦 Twitter ouvert. Ton texte : " + text : "🐦 Twitter ouvert.");
        }
    }

    // ── WhatsApp ──────────────────────────────────────────────────────────────

    public String openWhatsApp(String nameOrNumber, String message) {
        if (nameOrNumber != null && !nameOrNumber.isEmpty()) {
            // Essaie le deep link avec numéro
            String number = nameOrNumber.replaceAll("[^0-9+]", "");
            String url = "https://wa.me/" + number;
            if (message != null && !message.isEmpty()) url += "?text=" + Uri.encode(message);
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.setPackage("com.whatsapp");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return "💬 WhatsApp ouvert pour " + nameOrNumber + ".";
            } catch (Exception ignored) {}
        }
        return openAppWithFallback("com.whatsapp", null, "https://web.whatsapp.com", "💬 WhatsApp ouvert.");
    }

    // ── Telegram ──────────────────────────────────────────────────────────────

    public String openTelegram(String username) {
        if (username != null && !username.isEmpty()) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("tg://resolve?domain=" + username));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return "✈️ Telegram ouvert pour @" + username + ".";
            } catch (Exception ignored) {}
        }
        return openAppWithFallback("org.telegram.messenger", null, "https://telegram.org",
            "✈️ Telegram ouvert.");
    }

    // ── TikTok ────────────────────────────────────────────────────────────────

    public String openTikTok() {
        return openAppWithFallback("com.zhiliaoapp.musically", null,
            "https://www.tiktok.com", "🎵 TikTok ouvert.");
    }

    // ── Snapchat ──────────────────────────────────────────────────────────────

    public String openSnapchat() {
        return openAppWithFallback("com.snapchat.android", null,
            "https://www.snapchat.com", "👻 Snapchat ouvert.");
    }

    // ── Share générique ───────────────────────────────────────────────────────

    public String shareText(String text) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text != null ? text : "");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(Intent.createChooser(i, "Partager via")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return "📤 Menu de partage ouvert.";
        } catch (Exception e) {
            return "Impossible d'ouvrir le menu de partage.";
        }
    }

    // ── Parser commande texte ─────────────────────────────────────────────────

    public String parseCommand(String cmd) {
        String c = cmd.toLowerCase();

        // Extrait le message entre guillemets ou après ":"
        String text = extractMessage(cmd);

        if (c.contains("instagram"))   return openInstagramPost(text);
        if (c.contains("twitter") || c.contains("tweet") || c.contains(" x "))
                                        return openTwitterPost(text);
        if (c.contains("whatsapp"))    return openWhatsApp(extractName(c), text);
        if (c.contains("telegram"))    return openTelegram(extractUsername(c));
        if (c.contains("tiktok"))      return openTikTok();
        if (c.contains("snapchat") || c.contains("snap")) return openSnapchat();
        if (c.contains("partage") || c.contains("share")) return shareText(text);

        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String openAppWithFallback(String pkg, Intent customIntent,
                                        String fallbackUrl, String successMsg) {
        try {
            Intent i = customIntent != null
                ? customIntent
                : ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) throw new Exception("App not installed");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return successMsg;
        } catch (Exception e) {
            try {
                Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(web);
                return successMsg + " (navigateur)";
            } catch (Exception ex) {
                Log.e(TAG, "openAppWithFallback: " + ex.getMessage());
                return "Application introuvable.";
            }
        }
    }

    private void copyToClipboard(String text) {
        try {
            android.content.ClipboardManager cm =
                (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(
                android.content.ClipData.newPlainText("Léa", text));
        } catch (Exception ignored) {}
    }

    private String extractMessage(String cmd) {
        // Entre guillemets
        int q1 = cmd.indexOf('"'), q2 = cmd.lastIndexOf('"');
        if (q1 >= 0 && q2 > q1) return cmd.substring(q1 + 1, q2);
        int q3 = cmd.indexOf("'"), q4 = cmd.lastIndexOf("'");
        if (q3 >= 0 && q4 > q3) return cmd.substring(q3 + 1, q4);
        // Après ":"
        int colon = cmd.indexOf(':');
        if (colon >= 0 && colon < cmd.length() - 1) return cmd.substring(colon + 1).trim();
        // Après le mot-clé réseau social
        String c = cmd.toLowerCase();
        for (String kw : new String[]{ "post sur instagram", "tweet", "poste sur twitter",
                "envoie sur whatsapp", "message sur" }) {
            int idx = c.indexOf(kw);
            if (idx >= 0 && idx + kw.length() < cmd.length())
                return cmd.substring(idx + kw.length()).trim();
        }
        return "";
    }

    private String extractName(String cmd) {
        // Cherche "à [nom]" ou "pour [nom]"
        String c = cmd.toLowerCase();
        for (String prep : new String[]{ "à ", "pour ", "envoie à " }) {
            int idx = c.indexOf(prep);
            if (idx >= 0) {
                String after = c.substring(idx + prep.length());
                String name = after.split("\\s+")[0];
                if (name.length() > 1) return name;
            }
        }
        return "";
    }

    private String extractUsername(String cmd) {
        // Cherche "@username" ou "à username"
        int at = cmd.indexOf('@');
        if (at >= 0) return cmd.substring(at + 1).split("\\s+")[0];
        return extractName(cmd);
    }
}
