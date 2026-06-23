package com.flolov42.lea_v3.bixby;

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


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.json.JSONObject;
import java.util.Calendar;

/**
 * Parse les commandes vocales et les route vers Android ou le serveur IA local.
 * Aucune API externe payante — tout passe par WebSocket vers Ollama local.
 */
public class LeaNovaCommandProcessor {

    private static final String TAG = "LeaCmd";

    private final Context ctx;
    private final WebSocketClient ws;
    private final LeaMemoryManager memory;

    public interface ResultCallback { void onResult(String response); }
    private ResultCallback callback;

    public LeaNovaCommandProcessor(Context ctx, WebSocketClient ws) {
        this.ctx    = ctx.getApplicationContext();
        this.ws     = ws;
        this.memory = new LeaMemoryManager(ctx);
    }

    public void setCallback(ResultCallback cb) { this.callback = cb; }

    /**
     * Traite une commande vocale.
     * Retourne true si exécutée nativement, false si routée vers l'IA.
     */
    public boolean process(String command) {
        if (command == null || command.trim().isEmpty()) return false;
        String cmd = command.toLowerCase().trim();

        // Track l'habitude
        memory.setPreference("cmd_" + System.currentTimeMillis(), cmd);

        // --- APPELS ---
        if (cmd.startsWith("appelle ") || cmd.startsWith("téléphone à ")) {
            String name = cmd.replace("appelle ", "").replace("téléphone à ", "").trim();
            makeCall(name); return true;
        }

        // --- MUSIQUE ---
        if (cmd.startsWith("joue ") || cmd.startsWith("mets ") || cmd.contains("musique") || cmd.contains("chanson")) {
            String q = cmd.replace("joue ", "").replace("mets ", "").replace("la musique", "").trim();
            playMusic(q); return true;
        }

        // --- GALERIE ---
        if (cmd.contains("galerie") || cmd.contains("photos")) {
            openGallery(); return true;
        }

        // --- RÉVEIL ---
        if (cmd.contains("réveil") || cmd.contains("réveille")) {
            parseAndSetAlarm(cmd); return true;
        }

        // --- MINUTEUR ---
        if (cmd.contains("minuteur") || cmd.contains("timer") || cmd.contains("chrono")) {
            parseAndSetTimer(cmd); return true;
        }

        // --- VOLUME ---
        if (cmd.contains("monte le son") || cmd.contains("augmente le volume") || cmd.contains("plus fort")) {
            adjustVolume(true); return true;
        }
        if (cmd.contains("baisse le son") || cmd.contains("diminue le volume") || cmd.contains("moins fort")) {
            adjustVolume(false); return true;
        }
        if (cmd.contains("volume")) {
            setVolumePercent(extractFirstNumber(cmd, 70)); return true;
        }

        // --- LUMINOSITÉ ---
        if (cmd.contains("luminosité") || cmd.contains("brightness") || cmd.contains("écran plus")) {
            setBrightness(extractFirstNumber(cmd, 80)); return true;
        }

        // --- MODES SYSTÈME ---
        if (cmd.contains("mode silencieux") || cmd.contains("ne pas déranger") || cmd.contains("vibreur")) {
            setSilentMode(true); return true;
        }
        if (cmd.contains("mode normal") || cmd.contains("son activé") || cmd.contains("désactive silencieux")) {
            setSilentMode(false); return true;
        }

        // --- CALENDRIER LECTURE ---
        if (cmd.contains("calendrier") || cmd.contains("agenda") || cmd.contains("rendez-vous aujourd")) {
            readCalendar(); return true;
        }

        // --- CALENDRIER CRÉATION ---
        if (cmd.contains("crée un événement") || cmd.contains("ajoute au calendrier") || cmd.contains("nouveau rendez-vous")) {
            createCalendarEvent(command); return true;
        }

        // --- ROUTINES ---
        if (cmd.contains("crée une routine") || cmd.contains("nouvelle routine") || cmd.contains("automatise")) {
            parseAndCreateRoutine(command); return true;
        }

        // --- APP SPÉCIFIQUE ---
        if (cmd.contains("ouvre ") || cmd.contains("lance ")) {
            openApp(cmd); return true;
        }

        // --- TOUT LE RESTE → IA LOCALE (OLLAMA via WebSocket) ---
        routeToAI(command);
        return false;
    }

    // =============================================
    //  Implémentations natives
    // =============================================

    private void makeCall(String name) {
        String number = findContactNumber(name);
        if (number.isEmpty()) { notify("Contact introuvable : " + name); return; }
        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(i); notify("J'appelle " + name + "."); }
        catch (SecurityException e) { notify("Permission d'appel manquante."); }
    }

    private void playMusic(String query) {
        Intent i = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE);
        i.putExtra(android.app.SearchManager.QUERY, query);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(i);
            notify(query.isEmpty() ? "Lecture aléatoire." : "Je lance " + query + ".");
        } catch (Exception e) {
            for (String pkg : new String[]{ "com.spotify.music", "com.google.android.music", "com.sec.android.app.music" }) {
                Intent fallback = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(fallback);
                    return;
                }
            }
        }
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(i); }
        catch (Exception e) { notify("Impossible d'ouvrir la galerie."); }
    }

    private void parseAndSetAlarm(String cmd) {
        int hour = extractFirstNumber(cmd, 7), minute = 0;
        // Détecte "8h30" ou "8:30"
        try {
            int idx = cmd.indexOf('h');
            if (idx < 0) idx = cmd.indexOf(':');
            if (idx >= 0) {
                String rest = cmd.substring(idx + 1).replaceAll("[^0-9].*", "");
                if (!rest.isEmpty()) minute = Math.min(59, Integer.parseInt(rest));
            }
        } catch (Exception ignored) {}
        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
        i.putExtra(AlarmClock.EXTRA_HOUR, hour);
        i.putExtra(AlarmClock.EXTRA_MINUTES, minute);
        i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        notify("Réveil à " + hour + "h" + (minute > 0 ? String.format("%02d", minute) : "") + ".");
    }

    private void parseAndSetTimer(String cmd) {
        int n = extractFirstNumber(cmd, 1);
        int seconds = n;
        if (cmd.contains("heure"))  seconds = n * 3600;
        else if (cmd.contains("minute") || cmd.contains("min")) seconds = n * 60;
        Intent i = new Intent(AlarmClock.ACTION_SET_TIMER);
        i.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
        i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        notify("Minuteur de " + (seconds >= 60 ? seconds / 60 + " min" : seconds + " sec") + ".");
    }

    private void adjustVolume(boolean up) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI);
        notify(up ? "Volume augmenté." : "Volume baissé.");
    }

    private void setVolumePercent(int pct) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(max * pct / 100f), AudioManager.FLAG_SHOW_UI);
        notify("Volume à " + pct + "%.");
    }

    private void setBrightness(int pct) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(ctx)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                notify("Autorise d'abord la modification des paramètres.");
                return;
            }
            int value = Math.max(0, Math.min(255, (int)(pct / 100f * 255)));
            Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
            Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            notify("Luminosité à " + pct + "%.");
        } catch (Exception e) { notify("Impossible de modifier la luminosité."); }
    }

    private void setSilentMode(boolean silent) {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) am.setRingerMode(silent ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
            notify(silent ? "Mode silencieux activé." : "Son réactivé.");
        } catch (SecurityException e) { notify("Permission Do Not Disturb requise."); }
    }

    @SuppressLint("Range")
    private void readCalendar() {
        try {
            Calendar now = Calendar.getInstance();
            Calendar end = Calendar.getInstance(); end.add(Calendar.HOUR_OF_DAY, 24);
            Cursor c = ctx.getContentResolver().query(
                CalendarContract.Events.CONTENT_URI,
                new String[]{ CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART },
                CalendarContract.Events.DTSTART + " BETWEEN ? AND ?",
                new String[]{ String.valueOf(now.getTimeInMillis()), String.valueOf(end.getTimeInMillis()) },
                CalendarContract.Events.DTSTART + " ASC"
            );
            if (c != null && c.moveToFirst()) {
                StringBuilder sb = new StringBuilder("Aujourd'hui : ");
                do { sb.append(c.getString(c.getColumnIndex(CalendarContract.Events.TITLE))).append(". "); }
                while (c.moveToNext());
                c.close(); notify(sb.toString());
            } else { notify("Aucun événement aujourd'hui."); if (c != null) c.close(); }
        } catch (Exception e) { notify("Impossible de lire le calendrier."); }
    }

    private void createCalendarEvent(String title) {
        Intent i = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
        i.putExtra(CalendarContract.Events.TITLE, title);
        i.putExtra(CalendarContract.Events.DTSTART, System.currentTimeMillis() + 3600000L);
        i.putExtra(CalendarContract.Events.DTEND,   System.currentTimeMillis() + 7200000L);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    private void parseAndCreateRoutine(String cmd) {
        String name = "Routine " + new java.util.Date().toString().substring(0, 10);
        String time = "07:00";
        int n = extractFirstNumber(cmd, 7);
        if (n >= 0 && n <= 23) time = String.format("%02d:00", n);
        new LeaAutomationManager(ctx).createRoutine(name, time, "1,2,3,4,5", new String[]{ cmd });
        notify("Routine \"" + name + "\" créée à " + time + ".");
    }

    private void openApp(String cmd) {
        String app = cmd.replace("ouvre ", "").replace("lance ", "").trim();
        // Essaie de trouver l'appli par nom partiel
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        for (android.content.pm.ResolveInfo ri : ctx.getPackageManager().queryIntentActivities(main, 0)) {
            String label = ri.loadLabel(ctx.getPackageManager()).toString().toLowerCase();
            if (label.contains(app)) {
                Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(launch);
                    notify("J'ouvre " + ri.loadLabel(ctx.getPackageManager()) + ".");
                    return;
                }
            }
        }
        notify("Application \"" + app + "\" introuvable.");
    }

    /** Route la commande vers l'IA Ollama locale via WebSocket. */
    public void routeToAI(String text) {
        if (ws == null || !ws.isOpen()) { notify("Non connecté au serveur Léa."); return; }
        try {
            String context = memory.buildContext(4);
            int sentiment  = LeaSentimentDetector.analyze(text);
            String prefix  = LeaSentimentDetector.reactionFor(sentiment);

            JSONObject payload = new JSONObject();
            payload.put("user",   memory.getBossName());
            payload.put("action", "voice_command");
            payload.put("text",   context + prefix + text);
            ws.send(payload.toString());
        } catch (Exception e) { Log.e(TAG, "routeToAI: " + e.getMessage()); }
    }

    @SuppressLint("Range")
    private String findContactNumber(String name) {
        Cursor c = ctx.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            new String[]{ "%" + name + "%" }, null);
        String number = "";
        if (c != null) {
            if (c.moveToFirst()) number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            c.close();
        }
        return number;
    }

    private int extractFirstNumber(String text, int def) {
        for (String p : text.split("[^0-9]+")) {
            if (!p.isEmpty()) try { return Integer.parseInt(p); } catch (Exception ignored) {}
        }
        return def;
    }

    private void notify(String msg) {
        if (callback != null) new Handler(Looper.getMainLooper()).post(() -> callback.onResult(msg));
    }
}
