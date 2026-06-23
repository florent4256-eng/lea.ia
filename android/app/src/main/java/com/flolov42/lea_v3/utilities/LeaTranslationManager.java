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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduction avec double moteur :
 *   1. MyMemory API (en ligne, rapide, gratuite)
 *   2. ML Kit Translate (hors-ligne, modèles ~30 Mo téléchargés sur l'appareil)
 * Conversion de devises via ExchangeRate-API (en ligne).
 */
public class LeaTranslationManager {

    private static final String TAG      = "LeaTranslation";
    private static final String MYMEMORY = "https://api.mymemory.translated.net/get";
    private static final String EXCHANGE = "https://open.er-api.com/v6/latest/";
    private static final String PREFS    = "lea_memory";
    private static final String KEY_LANG = "lea_response_language";

    private final Context ctx;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface TranslationCallback { void onResult(String result); }

    public LeaTranslationManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Traduction (online → offline fallback) ────────────────────────────────

    public void translate(String text, String targetLang, TranslationCallback cb) {
        if (text == null || text.trim().isEmpty()) {
            deliver(cb, "Quel texte veux-tu traduire ?");
            return;
        }
        String langCode = normalizeLangCode(targetLang);
        String source   = detectSourceLang();
        String pair     = source + "|" + langCode;

        new Thread(() -> {
            try {
                String urlStr = MYMEMORY + "?q=" + Uri.encode(text) + "&langpair=" + Uri.encode(pair);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("User-Agent", "LeaApp/1.0");

                int code = conn.getResponseCode();
                if (code != 200) {
                    Log.e(TAG, "MyMemory HTTP " + code + " → bascule ML Kit offline");
                    translateOffline(text, source, langCode, cb);
                    return;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                String translated = json.getJSONObject("responseData")
                    .optString("translatedText", "");

                if (translated.isEmpty()) {
                    translateOffline(text, source, langCode, cb);
                    return;
                }

                String langName = langNameFor(langCode);
                deliver(cb, "🌍 « " + text + " » en " + langName + " : « " + translated + " ».");

            } catch (Exception e) {
                Log.e(TAG, "MyMemory exception : " + e.getMessage() + " → bascule ML Kit offline");
                translateOffline(text, source, langCode, cb);
            }
        }).start();
    }

    // Traduction 100% hors-ligne via ML Kit (modèles téléchargés sur l'appareil)
    private void translateOffline(String text, String sourceLang, String targetLang,
                                  TranslationCallback cb) {
        Log.e(TAG, "📴 Traduction ML Kit offline : " + sourceLang + " → " + targetLang);

        TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(mlKitLang(sourceLang))
            .setTargetLanguage(mlKitLang(targetLang))
            .build();
        Translator translator = Translation.getClient(options);

        // Télécharge le modèle si absent (nécessite réseau au 1er appel seulement)
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(v -> {
                translator.translate(text)
                    .addOnSuccessListener(result -> {
                        String langName = langNameFor(targetLang);
                        deliver(cb, "🌍 « " + text + " » en " + langName
                            + " : « " + result + " » (hors-ligne).");
                        translator.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "ML Kit translate : " + e.getMessage());
                        deliver(cb, "Traduction indisponible hors-ligne.");
                        translator.close();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "ML Kit download : " + e.getMessage());
                deliver(cb, "📥 Modèle de traduction pas encore téléchargé. "
                    + "Connecte-toi une fois au Wi-Fi, Léa le téléchargera automatiquement.");
                translator.close();
            });
    }

    // Mappe nos codes BCP-47 vers les constantes ML Kit
    private String mlKitLang(String bcp47) {
        switch (bcp47) {
            case "fr": return TranslateLanguage.FRENCH;
            case "en": return TranslateLanguage.ENGLISH;
            case "es": return TranslateLanguage.SPANISH;
            case "de": return TranslateLanguage.GERMAN;
            case "it": return TranslateLanguage.ITALIAN;
            case "pt": return TranslateLanguage.PORTUGUESE;
            case "ar": return TranslateLanguage.ARABIC;
            case "zh": return TranslateLanguage.CHINESE;
            case "ja": return TranslateLanguage.JAPANESE;
            case "ru": return TranslateLanguage.RUSSIAN;
            case "nl": return TranslateLanguage.DUTCH;
            default:   return TranslateLanguage.ENGLISH;
        }
    }

    // ── Conversion de devises ─────────────────────────────────────────────────

    public void convertCurrency(double amount, String fromCurrency, String toCurrency,
                                 TranslationCallback cb) {
        String from = fromCurrency.toUpperCase().trim();
        String to   = toCurrency.toUpperCase().trim();

        new Thread(() -> {
            try {
                URL url = new URL(EXCHANGE + from);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    deliver(cb, "Conversion indisponible. Vérifie la connexion.");
                    return;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json  = new JSONObject(sb.toString());
                JSONObject rates = json.getJSONObject("rates");
                if (!rates.has(to)) {
                    deliver(cb, "Devise « " + to + " » non trouvée.");
                    return;
                }
                double rate   = rates.getDouble(to);
                double result = amount * rate;
                String msg = String.format("💱 %.2f %s = %.2f %s", amount, from, result, to);
                deliver(cb, msg);

            } catch (Exception e) {
                Log.e(TAG, "convertCurrency: " + e.getMessage());
                deliver(cb, "Conversion indisponible en ce moment.");
            }
        }).start();
    }

    // ── Langue de réponse ─────────────────────────────────────────────────────

    public String setResponseLanguage(String lang) {
        String code = normalizeLangCode(lang);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, code).apply();
        return "✅ Je parlerai maintenant en " + langNameFor(code) + ".";
    }

    public String getResponseLanguage() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "fr");
    }

    // ── Parser commande texte ─────────────────────────────────────────────────

    public void parseTranslateCommand(String cmd, TranslationCallback cb) {
        String c = cmd.toLowerCase();

        if (c.contains("euro") || c.contains("dollar") || c.contains("livre")
                || c.contains("yen") || c.contains("franc") || c.contains("devise")
                || c.contains("convertis") || c.contains("converti ")) {
            double amount = extractAmount(c);
            String from   = extractCurrencyCode(c, true);
            String to     = extractCurrencyCode(c, false);
            if (!from.equals(to)) {
                convertCurrency(amount, from, to, cb);
                return;
            }
        }

        if (c.contains("parle") && (c.contains("anglais") || c.contains("espagnol")
                || c.contains("allemand") || c.contains("italien") || c.contains("arabe")
                || c.contains("chinois") || c.contains("japonais"))) {
            String lang = extractTargetLang(c);
            deliver(cb, setResponseLanguage(lang));
            return;
        }

        String text       = extractTextToTranslate(c);
        String targetLang = extractTargetLang(c);
        translate(text.isEmpty() ? cmd : text, targetLang, cb);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String normalizeLangCode(String lang) {
        if (lang == null) return "en";
        switch (lang.toLowerCase().trim()) {
            case "anglais": case "english":    case "en": return "en";
            case "espagnol": case "spanish":   case "es": return "es";
            case "allemand": case "german":    case "de": return "de";
            case "italien":  case "italian":   case "it": return "it";
            case "portugais": case "portuguese": case "pt": return "pt";
            case "arabe":    case "arabic":    case "ar": return "ar";
            case "chinois":  case "chinese":   case "zh": return "zh";
            case "japonais": case "japanese":  case "ja": return "ja";
            case "russe":    case "russian":   case "ru": return "ru";
            case "néerlandais": case "dutch":  case "nl": return "nl";
            default: return "en";
        }
    }

    private String langNameFor(String code) {
        Map<String, String> names = new HashMap<>();
        names.put("en","anglais"); names.put("es","espagnol");
        names.put("de","allemand"); names.put("it","italien");
        names.put("pt","portugais"); names.put("ar","arabe");
        names.put("zh","chinois"); names.put("ja","japonais");
        names.put("ru","russe"); names.put("nl","néerlandais");
        return names.getOrDefault(code, code);
    }

    private String detectSourceLang() {
        String lang = getResponseLanguage();
        return lang.isEmpty() ? "fr" : lang;
    }

    private String extractTargetLang(String cmd) {
        if (cmd.contains("anglais"))   return "en";
        if (cmd.contains("espagnol"))  return "es";
        if (cmd.contains("allemand"))  return "de";
        if (cmd.contains("italien"))   return "it";
        if (cmd.contains("portugais")) return "pt";
        if (cmd.contains("arabe"))     return "ar";
        if (cmd.contains("chinois"))   return "zh";
        if (cmd.contains("japonais"))  return "ja";
        if (cmd.contains("russe"))     return "ru";
        return "en";
    }

    private String extractTextToTranslate(String cmd) {
        int q1 = cmd.indexOf("'"), q2 = cmd.lastIndexOf("'");
        if (q1 >= 0 && q2 > q1) return cmd.substring(q1 + 1, q2);
        int q3 = cmd.indexOf("\""), q4 = cmd.lastIndexOf("\"");
        if (q3 >= 0 && q4 > q3) return cmd.substring(q3 + 1, q4);
        return cmd.replaceAll("(traduis?|comment dit-on|en anglais|en espagnol|en allemand|en .+)", "").trim();
    }

    private String extractCurrencyCode(String cmd, boolean first) {
        String[] currencies = { "eur", "euro", "euros", "usd", "dollar", "dollars",
            "gbp", "livre", "livres", "jpy", "yen", "chf", "franc" };
        String[] codes = { "EUR","EUR","EUR","USD","USD","USD",
            "GBP","GBP","GBP","JPY","JPY","CHF","CHF" };
        int firstIdx = Integer.MAX_VALUE, lastIdx = -1;
        String firstCode = "EUR", lastCode = "USD";
        for (int i = 0; i < currencies.length; i++) {
            int idx = cmd.indexOf(currencies[i]);
            if (idx >= 0) {
                if (idx < firstIdx) { firstIdx = idx; firstCode = codes[i]; }
                if (idx > lastIdx)  { lastIdx  = idx; lastCode  = codes[i]; }
            }
        }
        return first ? firstCode : lastCode;
    }

    private double extractAmount(String text) {
        for (String p : text.split("[^0-9.,]+")) {
            if (!p.isEmpty()) {
                try { return Double.parseDouble(p.replace(',', '.')); } catch (Exception ignored) {}
            }
        }
        return 1.0;
    }

    private void deliver(TranslationCallback cb, String msg) {
        mainHandler.post(() -> { if (cb != null) cb.onResult(msg); });
    }
}
