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


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Applique DIRECTEMENT les réglages système (luminosité, volume, WiFi, BT, etc.)
 * sans ouvrir d'interface.  Toutes les méthodes retournent un message vocal.
 */
public class LeaSystemControlManager {

    private static final String TAG = "LeaSysCtrl";
    private final Context ctx;

    public LeaSystemControlManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Luminosité ────────────────────────────────────────────────────────────

    public String setBrightness(int percent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(ctx)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    android.net.Uri.parse("package:" + ctx.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return "J'ai besoin de la permission d'écriture pour changer la luminosité.";
            }
            int val = Math.max(1, Math.min(255, (int)(percent / 100.0 * 255)));
            Settings.System.putInt(ctx.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(ctx.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, val);
            return "✅ Luminosité à " + percent + "%.";
        } catch (Exception e) {
            Log.e(TAG, "setBrightness: " + e.getMessage());
            return "Impossible de changer la luminosité.";
        }
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    public String setVolume(int percent) {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return "AudioManager indisponible.";
            int maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int maxRing  = am.getStreamMaxVolume(AudioManager.STREAM_RING);
            int music = Math.max(0, Math.min(maxMusic, (int)(percent / 100.0 * maxMusic)));
            int ring  = Math.max(0, Math.min(maxRing,  (int)(percent / 100.0 * maxRing)));
            am.setStreamVolume(AudioManager.STREAM_MUSIC, music, 0);
            am.setStreamVolume(AudioManager.STREAM_RING,  ring,  0);
            return "✅ Volume à " + percent + "%.";
        } catch (Exception e) {
            Log.e(TAG, "setVolume: " + e.getMessage());
            return "Impossible de changer le volume.";
        }
    }

    public String setVolumeLevel(String level) {
        int pct;
        switch (level.toLowerCase()) {
            case "mute": case "muet": case "silencieux": pct = 0;   break;
            case "bas":  case "low":                     pct = 20;  break;
            case "fort": case "high": case "max":        pct = 100; break;
            default:                                      pct = 50;  break;
        }
        return setVolume(pct);
    }

    // ── Mode sonore ───────────────────────────────────────────────────────────

    public String setRingerMode(String mode) {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return "AudioManager indisponible.";
            switch (mode.toLowerCase()) {
                case "silencieux": case "muet": case "silent":
                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    return "✅ Mode silencieux activé.";
                case "vibreur": case "vibrate":
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    return "✅ Mode vibreur activé.";
                default:
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    return "✅ Mode son normal activé.";
            }
        } catch (SecurityException e) {
            return "Permission Do Not Disturb requise pour changer le mode son.";
        }
    }

    // ── WiFi ──────────────────────────────────────────────────────────────────

    public String setWifi(boolean enabled) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    wm.setWifiEnabled(enabled);
                    return "✅ WiFi " + (enabled ? "activé" : "désactivé") + ".";
                }
            }
            // Android 10+ — open panel
            Intent i = new Intent(Settings.Panel.ACTION_WIFI);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "Panneau WiFi ouvert. Active ou désactive manuellement.";
        } catch (Exception e) {
            return "Impossible de changer le WiFi.";
        }
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────

    public String setBluetooth(boolean enabled) {
        try {
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            if (bt == null) return "Bluetooth non disponible sur cet appareil.";
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (enabled && !bt.isEnabled())  bt.enable();
                if (!enabled && bt.isEnabled())  bt.disable();
                return "✅ Bluetooth " + (enabled ? "activé" : "désactivé") + ".";
            }
            // Android 13+ : bt.enable()/disable() supprimé pour les apps tierces.
            // Ne pas ouvrir ACTION_BLUETOOTH_SETTINGS depuis un service — cela s'affiche
            // sur l'écran du téléphone pour tout le monde sans avertissement.
            String action = enabled ? "activer" : "désactiver";
            return "⚠️ Android 13+ ne permet pas l'" + action + " automatique du Bluetooth. "
                + "Utilise les Réglages rapides (glisse depuis le haut de l'écran).";
        } catch (Exception e) {
            return "Impossible de changer le Bluetooth.";
        }
    }

    // ── Économie de batterie ──────────────────────────────────────────────────

    public String togglePowerSaving(boolean enabled) {
        try {
            Intent i;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                i = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            } else {
                i = new Intent(Settings.ACTION_SETTINGS);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "Paramètres batterie ouverts — active le mode économie.";
        } catch (Exception e) {
            return "Impossible d'accéder aux paramètres batterie.";
        }
    }

    // ── Mode réseau ───────────────────────────────────────────────────────────

    public String openNetworkMode() {
        try {
            Intent i = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "Paramètres réseau ouverts.";
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return "Paramètres réseau ouverts.";
            } catch (Exception ex) {
                return "Impossible d'accéder aux paramètres réseau.";
            }
        }
    }

    // ── Ne Pas Déranger ───────────────────────────────────────────────────────

    public String setDoNotDisturb(boolean enabled) {
        try {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return enabled
                ? "Panneau Ne Pas Déranger ouvert. Active-le pour être tranquille."
                : "Panneau Ne Pas Déranger ouvert.";
        } catch (Exception e) {
            return setRingerMode(enabled ? "silencieux" : "normal");
        }
    }

    // ── Dispatcher par commande texte ─────────────────────────────────────────

    public String processCommand(String cmd) {
        String c = cmd.toLowerCase();

        // Luminosité
        if (c.contains("luminosité") || c.contains("brightness") || c.contains("écran")) {
            int val = extractNumber(c, 80);
            return setBrightness(val);
        }

        // Volume
        if (c.contains("volume") || c.contains("son")) {
            if (c.contains("muet") || c.contains("mute") || c.contains("couper"))
                return setVolume(0);
            if (c.contains("fort") || c.contains("max") || c.contains("augmente") || c.contains("monte"))
                return setVolume(100);
            if (c.contains("bas") || c.contains("baisse") || c.contains("diminue"))
                return setVolume(20);
            int val = extractNumber(c, 50);
            return setVolume(val);
        }

        // Mode sonore
        if (c.contains("mode silencieux") || (c.contains("silencieux") && !c.contains("wifi")))
            return setRingerMode("silencieux");
        if (c.contains("vibreur")) return setRingerMode("vibreur");
        if (c.contains("mode normal") || c.contains("son normal")) return setRingerMode("normal");

        // Ne Pas Déranger
        if (c.contains("ne pas déranger") || c.contains("occupé") || c.contains("pas disponible"))
            return setDoNotDisturb(true);

        // WiFi
        if (c.contains("wifi") || c.contains("wi-fi")) {
            boolean on = c.contains(" on") || c.contains("active") || c.contains("allume");
            boolean off = c.contains(" off") || c.contains("désactive") || c.contains("éteins");
            if (on)  return setWifi(true);
            if (off) return setWifi(false);
        }

        // Bluetooth
        if (c.contains("bluetooth") || c.contains("bt ")) {
            boolean on  = c.contains(" on") || c.contains("active") || c.contains("allume");
            boolean off = c.contains(" off") || c.contains("désactive") || c.contains("éteins");
            if (on)  return setBluetooth(true);
            if (off) return setBluetooth(false);
        }

        // Batterie
        if (c.contains("économie") || c.contains("batterie") || c.contains("power sav"))
            return togglePowerSaving(true);

        // Réseau
        if (c.contains("5g") || c.contains("4g") || c.contains("3g") || c.contains("réseau mobile"))
            return openNetworkMode();

        return null;
    }

    private int extractNumber(String text, int def) {
        for (String p : text.split("[^0-9]+")) {
            if (!p.isEmpty()) try { return Integer.parseInt(p); } catch (Exception ignored) {}
        }
        return def;
    }
}
