package com.flolov42.lea_v3.agents;

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


import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class LeaSecurityAgent {

    private static final String ID = LeaAgentActivationManager.SECURITY;

    // ── Codes de demande de permission (uniques à cet agent) ─────────────────
    public static final int REQ_SMS      = 3001;
    public static final int REQ_CONTACTS = 3002;

    // ── Permissions déclarées ─────────────────────────────────────────────────
    public static final String[] PERMS_SMS      = { Manifest.permission.READ_SMS };
    public static final String[] PERMS_CONTACTS = { Manifest.permission.READ_CONTACTS };

    // ── Mots-clés phishing ────────────────────────────────────────────────────
    private static final String[] PHISHING_KEYWORDS = {
        "cliquez ici", "click here", "verify your account",
        "vérifiez votre compte", "suspended", "suspendu",
        "urgent: votre", "compte bloqué", "mise à jour requise",
        "gagnez", "vous avez gagné", "prix", "cadeau gratuit",
        "bit.ly", "tinyurl", "ow.ly", "t.co/spam"
    };

    // ── Combinaisons de permissions dangereuses ───────────────────────────────
    private static final String[] DANGEROUS_PERM_COMBOS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    };

    // ── Whitelist apps de confiance ───────────────────────────────────────────
    private static final String[] TRUSTED_PACKAGES = {
        "com.google", "com.samsung", "com.android",
        "com.facebook", "com.instagram", "com.whatsapp",
        "com.microsoft", "com.apple", "com.spotify"
    };

    private final Context                     ctx;
    private final LeaAgentDatabase            db;
    private final LeaAgentNotificationManager notif;

    public LeaSecurityAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
    }

    // ── Vérifications de permission ───────────────────────────────────────────

    /** Vérifie si READ_SMS est accordée. */
    public boolean hasSmsPerm() {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS)
               == PackageManager.PERMISSION_GRANTED;
    }

    /** Vérifie si READ_CONTACTS est accordée. */
    public boolean hasContactsPerm() {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
               == PackageManager.PERMISSION_GRANTED;
    }

    /** Retourne true si TOUTES les permissions requises sont accordées. */
    public boolean hasAllPermissions() {
        return hasSmsPerm() && hasContactsPerm();
    }

    // ── Demandes de permission (doivent être appelées depuis une Activity) ─────

    /** Demande READ_SMS à l'utilisateur. */
    public void requestSmsPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMS_SMS, REQ_SMS);
        db.addLog(ID, "📋 Demande de permission READ_SMS envoyée…");
    }

    /** Demande READ_CONTACTS à l'utilisateur. */
    public void requestContactsPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMS_CONTACTS, REQ_CONTACTS);
        db.addLog(ID, "📋 Demande de permission READ_CONTACTS envoyée…");
    }

    /**
     * Demande toutes les permissions manquantes en une seule fois.
     * Retourne true si au moins une demande a été lancée.
     */
    public boolean requestMissingPermissions(Activity activity) {
        List<String> missing = new ArrayList<>();
        if (!hasSmsPerm())      missing.add(Manifest.permission.READ_SMS);
        if (!hasContactsPerm()) missing.add(Manifest.permission.READ_CONTACTS);

        if (missing.isEmpty()) return false;

        ActivityCompat.requestPermissions(
            activity,
            missing.toArray(new String[0]),
            REQ_SMS  // code générique — onRequestPermissionsResult trie par permission
        );
        db.addLog(ID, "📋 " + missing.size() + " permission(s) demandée(s)…");
        return true;
    }

    // ── Callback depuis Activity.onRequestPermissionsResult ───────────────────

    /**
     * À appeler depuis LeaAgentActivity.onRequestPermissionsResult().
     * Gère les réponses pour REQ_SMS et REQ_CONTACTS.
     * Lance automatiquement le scan si la permission vient d'être accordée.
     */
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) return;

        for (int i = 0; i < permissions.length; i++) {
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            String  perm    = permissions[i];

            if (Manifest.permission.READ_SMS.equals(perm)) {
                if (granted) {
                    db.addLog(ID, "✅ Permission READ_SMS accordée — scan SMS lancé");
                    new Thread(this::scanPhishingSms).start();
                } else {
                    db.addLog(ID, "❌ Permission READ_SMS refusée — scan SMS désactivé");
                }
            }

            if (Manifest.permission.READ_CONTACTS.equals(perm)) {
                if (granted) {
                    db.addLog(ID, "✅ Permission READ_CONTACTS accordée");
                } else {
                    db.addLog(ID, "❌ Permission READ_CONTACTS refusée");
                }
            }
        }
    }

    // ── Exécution périodique (appelée par LeaAgentService) ────────────────────

    /**
     * Point d'entrée principal.
     * Chaque opération vérifie sa permission avant d'agir.
     */
    public void execute() {
        try {
            if (hasSmsPerm()) {
                scanPhishingSms();
            } else {
                db.addLog(ID, "🔒 READ_SMS manquante — scan SMS ignoré (accorder dans Paramètres)");
            }

            // scanSuspiciousApps n'a pas besoin de permission runtime
            scanSuspiciousApps();

            db.updateLastAction(ID, "🔐 Scan sécurité effectué à " +
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date()));

        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Sécurité: " + e.getMessage());
        }
    }

    // ── Opérations internes ───────────────────────────────────────────────────

    private void scanPhishingSms() {
        // Permission READ_SMS déjà vérifiée par l'appelant
        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            ContentResolver cr  = ctx.getContentResolver();
            long dayAgo = System.currentTimeMillis() - 24L * 3600 * 1000;

            Cursor c = cr.query(
                smsUri,
                new String[]{"address", "body", "date"},
                "date > ?",
                new String[]{String.valueOf(dayAgo)},
                "date DESC"
            );

            if (c == null) {
                db.addLog(ID, "📭 Aucun SMS accessible");
                return;
            }

            int phishCount = 0;
            try {
                while (c.moveToNext()) {
                    String body = c.getString(1);
                    if (body == null) continue;
                    String lower = body.toLowerCase();

                    for (String keyword : PHISHING_KEYWORDS) {
                        if (lower.contains(keyword.toLowerCase())) {
                            String sender = c.getString(0);
                            String msg = "🚨 SMS phishing détecté de " + sender + ": \""
                                + body.substring(0, Math.min(50, body.length())) + "…\"";
                            db.addLog(ID, msg);
                            notif.notify(ID, "🚨 ALERTE Phishing!", msg);
                            phishCount++;
                            break;
                        }
                    }
                }
            } finally {
                c.close();
            }

            if (phishCount == 0) {
                db.addLog(ID, "✅ Aucun SMS suspect dans les dernières 24h");
            } else {
                db.addLog(ID, "⚠️ " + phishCount + " SMS suspect(s) détecté(s)!");
            }

        } catch (SecurityException e) {
            // Peut arriver si la permission est révoquée entre la vérification et l'accès
            db.addLog(ID, "🔒 Permission SMS révoquée pendant le scan — réessai annulé");
        }
    }

    private void scanSuspiciousApps() {
        try {
            PackageManager    pm   = ctx.getPackageManager();
            List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            List<String>      suspicious = new ArrayList<>();

            for (PackageInfo pkg : apps) {
                if (isTrusted(pkg.packageName)) continue;
                if (pkg.applicationInfo == null) continue;
                if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

                String[] perms = pkg.requestedPermissions;
                if (perms == null) continue;

                int dangerCount = 0;
                for (String dangerPerm : DANGEROUS_PERM_COMBOS) {
                    for (String appPerm : perms) {
                        if (dangerPerm.equals(appPerm)) { dangerCount++; break; }
                    }
                }

                if (dangerCount >= 4) suspicious.add(pkg.packageName);
            }

            if (!suspicious.isEmpty()) {
                String msg = "⚠️ Apps suspectes (" + suspicious.size() + "): "
                    + suspicious.get(0)
                    + (suspicious.size() > 1 ? " +" + (suspicious.size() - 1) : "");
                db.addLog(ID, msg);
                notif.notify(ID, "🔐 App suspecte", msg);
            } else {
                db.addLog(ID, "✅ Toutes les apps semblent normales (" + apps.size() + " vérifiées)");
            }

        } catch (Exception e) {
            db.addLog(ID, "⚠️ Scan apps échoué: " + e.getMessage());
        }
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Vérifie si un corps de SMS contient des mots-clés phishing. */
    public boolean isPhishingSms(String body) {
        if (body == null) return false;
        String lower = body.toLowerCase();
        for (String keyword : PHISHING_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }

    private boolean isTrusted(String pkg) {
        if (pkg == null) return false;
        for (String t : TRUSTED_PACKAGES) {
            if (pkg.startsWith(t)) return true;
        }
        return false;
    }
}
