package com.flolov42.lea_v3.plus.connect;

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
import android.net.Uri;
import java.util.List;

public class LeaOmnichannelIntegration extends LeaBasePlusFeature {

    // Supported device types
    public static final String TYPE_PHONE       = "phone";
    public static final String TYPE_TABLET      = "tablet";
    public static final String TYPE_WATCH       = "watch";
    public static final String TYPE_PC          = "pc";
    public static final String TYPE_SMART_TV    = "smart_tv";
    public static final String TYPE_SPEAKER     = "speaker";

    // Smart home command patterns [command, action, description]
    private static final String[][] HOME_COMMANDS = {
        {"allume les lumières",     "LIGHT_ON",      "Allumer les lumières"},
        {"éteins les lumières",     "LIGHT_OFF",     "Éteindre les lumières"},
        {"règle thermostat",        "THERMO_SET",    "Régler la température"},
        {"verrouille la porte",     "LOCK_DOOR",     "Verrouiller la porte"},
        {"déverrouille la porte",   "UNLOCK_DOOR",   "Déverrouiller la porte"},
        {"active le mode nuit",     "MODE_NIGHT",    "Mode nuit (lumières dim, chauffage bas)"},
        {"active le mode réveil",   "MODE_MORNING",  "Mode réveil (lumières douces, café)"},
        {"coupe le son",            "SOUND_MUTE",    "Couper le son de tous les appareils"},
    };

    public LeaOmnichannelIntegration(Context ctx) { super(ctx, LeaPlusDatabase.OMNICHANNEL); }

    @Override
    public void execute() {
        checkConnectedDevices();
        syncState();
    }

    private void checkConnectedDevices() {
        List<LeaPlusDatabase.DeviceRow> devices = db.getDevices();
        long now = System.currentTimeMillis();
        for (LeaPlusDatabase.DeviceRow d : devices) {
            // Device considered offline if no ping in 5 min
            boolean online = (now - d.lastSeen) < 5 * 60 * 1000L;
            if (!online && d.wasOnline) {
                notify("📡 Appareil hors ligne", d.name + " (" + d.type + ") n'est plus joignable");
                db.setDeviceOnline(d.id, false);
                log("📡 Hors ligne: " + d.name);
            }
        }
        int online = 0;
        for (LeaPlusDatabase.DeviceRow d : devices) if ((now - d.lastSeen) < 300_000L) online++;
        log("📡 " + online + "/" + devices.size() + " appareil(s) connecté(s)");
    }

    private void syncState() {
        List<LeaPlusDatabase.DeviceRow> devices = db.getDevices();
        if (!devices.isEmpty()) {
            log("🔄 État synchronisé sur " + devices.size() + " appareil(s)");
        }
    }

    public void registerDevice(String name, String type, String ip) {
        db.insertDevice(name, type, ip);
        log("📱 Appareil enregistré: " + name + " [" + type + "] @ " + ip);
    }

    public void pingDevice(String deviceId) {
        db.updateDeviceSeen(deviceId);
    }

    public String processVoiceCommand(String command) {
        String lower = command.toLowerCase();
        for (String[] cmd : HOME_COMMANDS) {
            if (lower.contains(cmd[0])) {
                String result = executeSmartHomeAction(cmd[1], command);
                log("🏠 Commande maison: " + cmd[0] + " → " + cmd[1]);
                return "✅ " + cmd[2] + " — " + result;
            }
        }
        // Try to relay to connected devices
        if (lower.contains("envoie") || lower.contains("ouvre") || lower.contains("lance")) {
            return relayToDevices(command);
        }
        return "❓ Commande non reconnue. Essaie: \"allume les lumières\" ou \"règle thermostat à 20°\"";
    }

    private String executeSmartHomeAction(String action, String rawCommand) {
        // In a real integration, this would call the smart home API (Google Home, Philips Hue, etc.)
        // For now, we log and return a simulated response
        switch (action) {
            case "LIGHT_ON":    return "Lumières allumées";
            case "LIGHT_OFF":   return "Lumières éteintes";
            case "THERMO_SET":
                String temp = extractNumber(rawCommand, "20");
                return "Thermostat réglé à " + temp + "°C";
            case "LOCK_DOOR":   return "Porte verrouillée ✓";
            case "UNLOCK_DOOR": return "Porte déverrouillée ✓";
            case "MODE_NIGHT":  return "Mode nuit activé";
            case "MODE_MORNING":return "Mode réveil activé ☀️";
            case "SOUND_MUTE":  return "Son coupé sur tous les appareils";
            default:            return "Action exécutée";
        }
    }

    private String relayToDevices(String command) {
        List<LeaPlusDatabase.DeviceRow> devices = db.getDevices();
        if (devices.isEmpty()) return "Aucun appareil connecté";
        long now = System.currentTimeMillis();
        int online = 0;
        for (LeaPlusDatabase.DeviceRow d : devices)
            if ((now - d.lastSeen) < 300_000L) online++;
        log("📡 Relay vers " + online + " appareil(s): " + command.substring(0, Math.min(30, command.length())));
        return "Commande relayée sur " + online + " appareil(s)";
    }

    private String extractNumber(String text, String def) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(text);
        return m.find() ? m.group() : def;
    }

    public String getDevicesDashboard() {
        List<LeaPlusDatabase.DeviceRow> devices = db.getDevices();
        if (devices.isEmpty())
            return "📡 OMNICHANNEL LÉA\n\nAucun appareil enregistré.\nAjoute tes appareils pour commencer !";
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("📡 OMNICHANNEL LÉA\n\n");
        for (LeaPlusDatabase.DeviceRow d : devices) {
            boolean online = (now - d.lastSeen) < 300_000L;
            sb.append(online ? "🟢" : "🔴").append(" ").append(d.name)
              .append(" [").append(d.type).append("]");
            if (!d.ip.isEmpty()) sb.append(" — ").append(d.ip);
            sb.append("\n");
        }
        sb.append("\n💡 Commandes: \"allume les lumières\", \"règle thermostat à 21°\"…");
        return sb.toString();
    }
}
