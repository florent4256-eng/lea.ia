package com.flolov42.lea_v3.home.voice;

import com.flolov42.lea_v3.home.database.LeaHomeDatabase;
import com.flolov42.lea_v3.home.models.SmartDevice;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    public enum Action { TURN_ON, TURN_OFF, TOGGLE, SET_BRIGHTNESS, SET_TEMPERATURE, SCENE, UNKNOWN }

    public static class ParsedCommand {
        public Action  action       = Action.UNKNOWN;
        public String  targetName   = null;   // device friendly name fragment
        public String  room         = null;
        public int     brightness   = -1;
        public float   temperature  = -1;
        public String  sceneName    = null;
        public SmartDevice resolved = null;
    }

    private final LeaHomeDatabase db;

    public CommandParser(LeaHomeDatabase db) {
        this.db = db;
    }

    public ParsedCommand parse(String text) {
        ParsedCommand cmd = new ParsedCommand();
        String t = text.toLowerCase().trim();

        // Scenes — "la scène" obligatoire pour ne pas capturer "active la lumière"
        Matcher scene = Pattern.compile("(?:active|lance|démarre)\\s+(?:la scène|le mode)\\s+(.+)").matcher(t);
        if (scene.find()) { cmd.action = Action.SCENE; cmd.sceneName = scene.group(1).trim(); return cmd; }

        // Brightness — accepte "%" ou "pour cent"
        Matcher bright = Pattern.compile("(?:mets|règle|baisse|monte)\\s+(?:la lumière|les lumières)?\\s*(?:à|a)?\\s*(\\d+)\\s*(?:%|pour cent)").matcher(t);
        if (bright.find()) { cmd.action = Action.SET_BRIGHTNESS; cmd.brightness = Integer.parseInt(bright.group(1)); }

        // Temperature
        Matcher temp = Pattern.compile("(?:règle|mets)\\s+(?:le chauffage|la température)\\s+(?:à|a)\\s*(\\d+(?:[.,]\\d+)?)").matcher(t);
        if (temp.find()) { cmd.action = Action.SET_TEMPERATURE; cmd.temperature = Float.parseFloat(temp.group(1).replace(',', '.')); }

        // On/Off/Toggle
        if (cmd.action == Action.UNKNOWN) {
            if (t.matches(".*(allume|active|ouvre|démarre).*"))  cmd.action = Action.TURN_ON;
            else if (t.matches(".*(éteins|éteint|désactive|ferme|stop|arrête).*")) cmd.action = Action.TURN_OFF;
            else if (t.matches(".*(bascule|inverse|switch).*")) cmd.action = Action.TOGGLE;
        }

        // Room extraction
        String[] rooms = {"salon", "chambre", "cuisine", "salle de bain", "bureau", "couloir", "jardin", "entrée"};
        for (String r : rooms) { if (t.contains(r)) { cmd.room = capitalize(r); break; } }

        // Device name extraction (remove command words)
        cmd.targetName = extractTarget(t);

        // Resolve device
        if (cmd.targetName != null || cmd.room != null) {
            cmd.resolved = resolveDevice(cmd.targetName, cmd.room);
        }

        return cmd;
    }

    private String extractTarget(String t) {
        String[] removeWords = {
            "allume", "éteins", "active", "désactive", "bascule", "mets", "règle",
            "la lumière", "les lumières", "le chauffage", "la température",
            "le ventilateur", "le volet", "les volets",
            "dans le", "dans la", "du", "de la", "le", "la", "les", "l'"
        };
        String result = t;
        for (String w : removeWords) result = result.replace(w, " ");
        result = result.replaceAll("\\s+", " ").trim();
        return result.isEmpty() ? null : result;
    }

    private SmartDevice resolveDevice(String nameFrag, String room) {
        List<SmartDevice> devices = (room != null)
            ? db.getDevicesByRoom(room) : db.getAllDevices();
        if (devices.isEmpty()) return null;
        SmartDevice best = null;
        int bestScore = 0;
        for (SmartDevice d : devices) {
            int score = 0;
            if (nameFrag != null) {
                String fn = d.friendlyName.toLowerCase();
                for (String word : nameFrag.split("\\s+")) {
                    if (!word.isEmpty() && fn.contains(word)) score++;
                }
            }
            if (room != null && room.equalsIgnoreCase(d.room)) score += 2;
            if (score > bestScore) { bestScore = score; best = d; }
        }
        return bestScore > 0 ? best : null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
