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


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralise les vérifications et demandes de permission pour tous les agents.
 * onRequestPermissionsResult() de LeaAgentActivity délègue ici.
 */
public class LeaAgentPermissionHelper {

    // ── Codes de requête par agent ────────────────────────────────────────────
    public static final int REQ_EMAIL        = 3010;
    public static final int REQ_CALENDAR     = 3011;
    public static final int REQ_FINANCE      = 3012;
    public static final int REQ_SOCIAL       = 3013;
    public static final int REQ_SECURITY     = 3014;
    public static final int REQ_PRODUCTIVITY = 3015;
    public static final int REQ_HEALTH       = 3016;
    public static final int REQ_ALL_AGENTS   = 3020;

    // ── Permissions par agent ─────────────────────────────────────────────────
    private static final String[] PERMS_EMAIL = {
        // Gmail API nécessite un account, ici on utilise le contenu local
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.INTERNET
    };

    private static final String[] PERMS_CALENDAR = {
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    };

    private static final String[] PERMS_FINANCE = {
        Manifest.permission.READ_SMS
    };

    private static final String[] PERMS_SOCIAL = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG
    };

    private static final String[] PERMS_SECURITY = {
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS
    };

    private static final String[] PERMS_PRODUCTIVITY = {
        // PACKAGE_USAGE_STATS n'est pas une permission runtime — elle se demande
        // via Settings.ACTION_USAGE_ACCESS_SETTINGS (gérée séparément)
    };

    private static final String[] PERMS_HEALTH = {
        // Samsung Health utilise son propre SDK — pas de permission standard
    };

    // ── Toutes les permissions runtime combinées ───────────────────────────────
    private static final String[] ALL_RUNTIME_PERMS = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    };

    private final Activity            activity;
    private final LeaAgentDatabase    db;
    private       PermissionCallback  pendingCallback;
    private       String              pendingAgentId;

    public interface PermissionCallback {
        void onGranted(String agentId);
        void onDenied(String agentId, String[] deniedPerms);
    }

    public LeaAgentPermissionHelper(Activity activity) {
        this.activity = activity;
        this.db       = LeaAgentDatabase.get(activity);
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Retourne true si toutes les permissions de cet agent sont accordées. */
    public boolean hasPermissionsFor(String agentId) {
        String[] needed = getPermsFor(agentId);
        for (String p : needed) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Demande les permissions manquantes pour un agent donné.
     * Le callback est déclenché depuis onRequestPermissionsResult().
     */
    public void requestFor(String agentId, PermissionCallback callback) {
        String[] needed  = getPermsFor(agentId);
        String[] missing = getMissing(needed);

        if (missing.length == 0) {
            callback.onGranted(agentId);
            return;
        }

        pendingCallback = callback;
        pendingAgentId  = agentId;

        ActivityCompat.requestPermissions(activity, missing, getRequestCodeFor(agentId));
        db.addLog(agentId, "📋 " + missing.length + " permission(s) demandée(s)…");
    }

    /**
     * Demande toutes les permissions de tous les agents en une seule popup.
     * À appeler à l'activation de LeaAgentActivity.
     */
    public void requestAllAgentPermissions(PermissionCallback callback) {
        String[] missing = getMissing(ALL_RUNTIME_PERMS);
        if (missing.length == 0) {
            if (callback != null) callback.onGranted("ALL");
            return;
        }
        pendingCallback = callback;
        pendingAgentId  = "ALL";
        ActivityCompat.requestPermissions(activity, missing, REQ_ALL_AGENTS);
    }

    /**
     * À appeler depuis Activity.onRequestPermissionsResult().
     * Gère les réponses pour tous les agents et déclenche le callback.
     */
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (pendingCallback == null) return;

        List<String> denied = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            boolean granted = grantResults.length > i
                              && grantResults[i] == PackageManager.PERMISSION_GRANTED;

            logPermissionResult(permissions[i], granted);
            if (!granted) denied.add(permissions[i]);
        }

        String agentId = pendingAgentId != null ? pendingAgentId : "?";
        if (denied.isEmpty()) {
            pendingCallback.onGranted(agentId);
        } else {
            pendingCallback.onDenied(agentId, denied.toArray(new String[0]));
        }

        // On déroule aussi le callback spécifique à LeaSecurityAgent si concerné
        if (requestCode == REQ_SECURITY || requestCode == LeaSecurityAgent.REQ_SMS
                                        || requestCode == REQ_ALL_AGENTS) {
            LeaAgentService svc = LeaAgentService.instance;
            // Le service recrée l'agent au prochain cycle — pas besoin d'appel direct
        }

        pendingCallback = null;
        pendingAgentId  = null;
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private String[] getPermsFor(String agentId) {
        switch (agentId) {
            case LeaAgentActivationManager.EMAIL:        return PERMS_EMAIL;
            case LeaAgentActivationManager.CALENDAR:     return PERMS_CALENDAR;
            case LeaAgentActivationManager.FINANCE:      return PERMS_FINANCE;
            case LeaAgentActivationManager.SOCIAL:       return PERMS_SOCIAL;
            case LeaAgentActivationManager.SECURITY:     return PERMS_SECURITY;
            case LeaAgentActivationManager.PRODUCTIVITY: return PERMS_PRODUCTIVITY;
            case LeaAgentActivationManager.HEALTH:       return PERMS_HEALTH;
            default:                                     return new String[0];
        }
    }

    private int getRequestCodeFor(String agentId) {
        switch (agentId) {
            case LeaAgentActivationManager.EMAIL:        return REQ_EMAIL;
            case LeaAgentActivationManager.CALENDAR:     return REQ_CALENDAR;
            case LeaAgentActivationManager.FINANCE:      return REQ_FINANCE;
            case LeaAgentActivationManager.SOCIAL:       return REQ_SOCIAL;
            case LeaAgentActivationManager.SECURITY:     return REQ_SECURITY;
            case LeaAgentActivationManager.PRODUCTIVITY: return REQ_PRODUCTIVITY;
            case LeaAgentActivationManager.HEALTH:       return REQ_HEALTH;
            default:                                     return REQ_ALL_AGENTS;
        }
    }

    private String[] getMissing(String[] perms) {
        List<String> missing = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        return missing.toArray(new String[0]);
    }

    private void logPermissionResult(String perm, boolean granted) {
        String label = permLabel(perm);
        if (granted) {
            db.addLog("SECURITY", "✅ " + label + " accordée");
        } else {
            db.addLog("SECURITY", "❌ " + label + " refusée");
        }
    }

    private String permLabel(String perm) {
        if (perm == null) return "?";
        switch (perm) {
            case Manifest.permission.READ_SMS:       return "READ_SMS";
            case Manifest.permission.READ_CONTACTS:  return "READ_CONTACTS";
            case Manifest.permission.READ_CALL_LOG:  return "READ_CALL_LOG";
            case Manifest.permission.READ_CALENDAR:  return "READ_CALENDAR";
            case Manifest.permission.WRITE_CALENDAR: return "WRITE_CALENDAR";
            default:
                String[] parts = perm.split("\\.");
                return parts[parts.length - 1];
        }
    }
}
