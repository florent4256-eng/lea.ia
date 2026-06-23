package com.flolov42.lea_v3.agents;

import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent Email — lit les emails non lus via Gmail REST API (OAuth2 Google).
 *
 * Flow d'autorisation :
 *  1. UI appelle getGoogleAccounts() pour lister les comptes sur le téléphone.
 *  2. L'utilisateur choisit un compte.
 *  3. UI appelle getTokenForAuth(email) sur un thread bg.
 *     → Si UserRecoverableAuthException : démarrer e.getIntent() pour le consentement.
 *     → Sinon : token OK → setAccount(email).
 *  4. execute() tourne périodiquement en background, appelle getTokenSilent().
 */
public class LeaEmailAgent {

    public static final String ID          = LeaAgentActivationManager.EMAIL;
    public static final String GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.readonly";

    private static final String GMAIL_API  = "https://gmail.googleapis.com/gmail/v1/users/me";
    private static final String PREFS_NAME = "lea_email_agent";
    private static final String KEY_ACCT   = "gmail_account";

    private final Context                     ctx;
    private final LeaAgentDatabase            db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences           prefs;

    public LeaEmailAgent(Context ctx) {
        this.ctx   = ctx.getApplicationContext();
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Exécution périodique (appelé par LeaAgentService) ────────────────────

    public void execute() {
        String account = getConfiguredAccount();
        if (account.isEmpty()) {
            db.addLog(ID, "⚙️ Gmail non configuré — ouvre l'agent pour connecter");
            return;
        }
        try {
            String token = getTokenSilent(account);
            if (token == null) return; // log déjà fait dans getTokenSilent
            List<EmailInfo> unread = fetchUnreadEmails(token, 20);
            processAndLog(unread);
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Gmail : " + e.getMessage());
        }
    }

    private void processAndLog(List<EmailInfo> unread) {
        if (unread.isEmpty()) {
            String msg = "✅ Boîte vide — aucun email non lu";
            db.addLog(ID, msg);
            db.updateLastAction(ID, msg);
            return;
        }
        int urgent = 0, important = 0, social = 0, newsletters = 0, other = 0;
        for (EmailInfo e : unread) {
            switch (classify(e)) {
                case "URGENT":     urgent++;      break;
                case "IMPORTANT":  important++;   break;
                case "SOCIAL":     social++;      break;
                case "NEWSLETTER": newsletters++; break;
                default:           other++;       break;
            }
        }
        StringBuilder msg = new StringBuilder("📧 " + unread.size() + " non lu(s)");
        if (urgent > 0)      msg.append(" · ").append(urgent).append(" ⚠️ urgent(s)");
        if (important > 0)   msg.append(" · ").append(important).append(" important(s)");
        if (social > 0)      msg.append(" · ").append(social).append(" social(aux)");
        if (newsletters > 0) msg.append(" · ").append(newsletters).append(" newsletter(s)");
        if (other > 0)       msg.append(" · ").append(other).append(" autre(s)");

        String text = msg.toString();
        db.addLog(ID, text);
        db.updateLastAction(ID, text);

        if (urgent > 0) {
            notif.notify(ID, "📧 Email urgent", urgent + " email(s) urgent(s) non lus");
        }
    }

    // ── OAuth2 token ──────────────────────────────────────────────────────────

    /**
     * Retourne le token silencieusement (background thread seulement).
     * Retourne null si une interaction utilisateur est requise.
     */
    public String getTokenSilent(String email) {
        try {
            Account acc = findAccount(email);
            if (acc == null) {
                db.addLog(ID, "⚠️ Compte introuvable sur le téléphone : " + email);
                return null;
            }
            return GoogleAuthUtil.getToken(ctx, acc, GMAIL_SCOPE);
        } catch (UserRecoverableAuthException e) {
            db.addLog(ID, "🔒 Autorisation Gmail requise — ouvre l'agent");
            return null;
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Token error : " + e.getMessage());
            return null;
        }
    }

    /**
     * Retourne le token, ou lève UserRecoverableAuthException si le consentement
     * utilisateur est nécessaire. À appeler sur un thread background depuis l'UI.
     */
    public String getTokenForAuth(String email) throws Exception {
        Account acc = findAccount(email);
        if (acc == null) throw new Exception("Compte Google introuvable : " + email);
        return GoogleAuthUtil.getToken(ctx, acc, GMAIL_SCOPE);
    }

    /** Invalide le token en cache (force un refresh au prochain appel) */
    public void invalidateToken(String token) {
        try { GoogleAuthUtil.clearToken(ctx, token); } catch (Exception ignored) {}
    }

    private Account findAccount(String email) {
        try {
            for (Account acc : AccountManager.get(ctx).getAccountsByType("com.google")) {
                if (acc.name.equalsIgnoreCase(email)) return acc;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Gmail REST API ────────────────────────────────────────────────────────

    /**
     * Récupère les emails non lus des dernières 24h.
     * Doit être appelé depuis un thread background.
     */
    public List<EmailInfo> fetchUnreadEmails(String token, int max) throws Exception {
        List<EmailInfo> result = new ArrayList<>();

        JSONObject listResp = apiGet(
            GMAIL_API + "/messages?q=is:unread+newer_than:1d&maxResults=" + max, token);
        if (listResp == null) return result;

        JSONArray messages = listResp.optJSONArray("messages");
        if (messages == null || messages.length() == 0) return result;

        int limit = Math.min(messages.length(), 10);
        for (int i = 0; i < limit; i++) {
            String msgId = messages.getJSONObject(i).optString("id");
            if (!msgId.isEmpty()) {
                EmailInfo info = fetchEmailDetail(msgId, token);
                if (info != null) result.add(info);
            }
        }
        return result;
    }

    private EmailInfo fetchEmailDetail(String msgId, String token) {
        try {
            JSONObject resp = apiGet(
                GMAIL_API + "/messages/" + msgId
                + "?format=metadata&metadataHeaders=Subject,From,Date", token);
            if (resp == null) return null;

            EmailInfo info = new EmailInfo();
            info.id      = msgId;
            info.snippet = resp.optString("snippet", "");

            JSONObject payload = resp.optJSONObject("payload");
            if (payload != null) {
                JSONArray headers = payload.optJSONArray("headers");
                if (headers != null) {
                    for (int i = 0; i < headers.length(); i++) {
                        JSONObject h = headers.getJSONObject(i);
                        String name = h.optString("name");
                        String val  = h.optString("value");
                        switch (name) {
                            case "Subject": info.subject = val; break;
                            case "From":    info.from    = val; break;
                            case "Date":    info.date    = val; break;
                        }
                    }
                }
            }
            return info;
        } catch (Exception e) { return null; }
    }

    private JSONObject apiGet(String urlStr, String token) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);

            int status = conn.getResponseCode();
            if (status == 401) {
                invalidateToken(token);
                db.addLog(ID, "🔒 Token expiré, reconnecte ton compte");
                return null;
            }
            if (status != 200) {
                db.addLog(ID, "⚠️ Gmail API erreur HTTP " + status);
                return null;
            }

            InputStream is = conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) sb.append(new String(buf, 0, n, "UTF-8"));
            return new JSONObject(sb.toString());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ── Classification & suggestions ─────────────────────────────────────────

    public static String classify(EmailInfo e) {
        if (e == null) return "OTHER";
        String subj    = e.subject != null ? e.subject.toLowerCase() : "";
        String snippet = e.snippet != null ? e.snippet.toLowerCase() : "";
        String from    = e.from    != null ? e.from.toLowerCase()    : "";
        String all     = subj + " " + snippet;

        if (from.contains("noreply") || from.contains("no-reply")
                || all.contains("désabonner") || all.contains("unsubscribe")
                || all.contains("newsletter") || all.contains("promotion")
                || all.contains("offre") || all.contains("promo"))
            return "NEWSLETTER";

        if (all.contains("urgent") || all.contains("asap")
                || all.contains("immédiat") || all.contains("d'urgence")
                || all.contains("impayé") || all.contains("relance"))
            return "URGENT";

        if (all.contains("facture") || all.contains("invoice")
                || all.contains("réunion") || all.contains("meeting")
                || all.contains("contrat") || all.contains("deadline")
                || all.contains("rendez-vous") || all.contains("important")
                || all.contains("confirmation"))
            return "IMPORTANT";

        if (from.contains("linkedin") || from.contains("twitter")
                || from.contains("facebook") || from.contains("instagram")
                || all.contains("a commenté") || all.contains("vous a mentionné"))
            return "SOCIAL";

        return "OTHER";
    }

    public static String suggestReply(EmailInfo e) {
        switch (classify(e)) {
            case "URGENT":
                return "Bonjour, j'ai bien reçu votre message urgent. "
                    + "Je reviens vers vous dans les plus brefs délais. Cordialement.";
            case "IMPORTANT":
                return "Bonjour, merci pour votre message. "
                    + "Je prends note et vous répondrai prochainement. Cordialement.";
            default:
                return null;
        }
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    public boolean isFirstSetup() {
        return !prefs.getBoolean("setup_shown", false);
    }

    public void markSetupShown() {
        prefs.edit().putBoolean("setup_shown", true).apply();
    }

    public void setAccount(String email) {
        prefs.edit().putString(KEY_ACCT, email).apply();
        db.addLog(ID, "✅ Gmail connecté : " + email);
    }

    public String getConfiguredAccount() {
        return prefs.getString(KEY_ACCT, "");
    }

    public boolean isConfigured() {
        return !getConfiguredAccount().isEmpty();
    }

    public void disconnect() {
        String acc = getConfiguredAccount();
        prefs.edit().remove(KEY_ACCT).apply();
        db.addLog(ID, "🔌 Compte Gmail déconnecté : " + acc);
    }

    public List<String> getGoogleAccounts() {
        List<String> list = new ArrayList<>();
        try {
            for (Account acc : AccountManager.get(ctx).getAccountsByType("com.google"))
                list.add(acc.name);
        } catch (Exception ignored) {}
        return list;
    }

    // ── Statut ────────────────────────────────────────────────────────────────

    public String getStatusSummary() {
        List<LeaAgentDatabase.LogRow> logs = db.getLogs(ID, 1);
        return logs.isEmpty() ? "Aucune activité" : logs.get(0).message;
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }

    // ── Modèle ────────────────────────────────────────────────────────────────

    public static class EmailInfo {
        public String id, from, subject, snippet, date;
    }
}
