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


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Crée / lit des événements via CalendarProvider + lance l'app Samsung Calendrier.
 */
public class LeaCalendarManager {

    private static final String TAG = "LeaCalendar";
    private final Context ctx;

    public LeaCalendarManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Création d'événement ─────────────────────────────────────────────────

    public String createEvent(String title, long startMillis, long durationMs) {
        try {
            long calendarId = getDefaultCalendarId();
            if (calendarId < 0) {
                // Fallback : ouvre l'app avec pré-remplissage
                openCalendarAppWithEvent(title, startMillis, startMillis + durationMs);
                return "✅ Calendrier ouvert avec ton événement pré-rempli.";
            }

            ContentValues cv = new ContentValues();
            cv.put(CalendarContract.Events.CALENDAR_ID,  calendarId);
            cv.put(CalendarContract.Events.TITLE,        title);
            cv.put(CalendarContract.Events.DTSTART,      startMillis);
            cv.put(CalendarContract.Events.DTEND,        startMillis + durationMs);
            cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            Uri uri = ctx.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, cv);
            if (uri != null) {
                openCalendarApp();
                return "✅ Événement « " + title + " » créé. Calendrier ouvert.";
            }
            openCalendarAppWithEvent(title, startMillis, startMillis + durationMs);
            return "✅ Calendrier ouvert — confirme l'événement.";
        } catch (Exception e) {
            Log.e(TAG, "createEvent: " + e.getMessage());
            openCalendarAppWithEvent(title, startMillis, startMillis + durationMs);
            return "✅ Calendrier ouvert avec l'événement pré-rempli.";
        }
    }

    // ── Lecture calendrier ────────────────────────────────────────────────────

    @SuppressLint("Range")
    public String getEventsToday() {
        try {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);

            Calendar end = Calendar.getInstance();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);

            Cursor c = ctx.getContentResolver().query(
                CalendarContract.Events.CONTENT_URI,
                new String[]{ CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART },
                CalendarContract.Events.DTSTART + " BETWEEN ? AND ?",
                new String[]{ String.valueOf(start.getTimeInMillis()), String.valueOf(end.getTimeInMillis()) },
                CalendarContract.Events.DTSTART + " ASC"
            );

            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return "Aucun événement aujourd'hui dans ton calendrier.";
            }

            List<String> events = new ArrayList<>();
            try {
                do {
                    String title = c.getString(c.getColumnIndex(CalendarContract.Events.TITLE));
                    long   ts    = c.getLong(c.getColumnIndex(CalendarContract.Events.DTSTART));
                    Calendar t   = Calendar.getInstance();
                    t.setTimeInMillis(ts);
                    events.add(String.format("%02d:%02d - %s",
                        t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE), title));
                } while (c.moveToNext());
            } finally {
                c.close();
            }

            if (events.size() == 1) return "Aujourd'hui : " + events.get(0) + ".";
            StringBuilder sb = new StringBuilder("Tu as " + events.size() + " événements : ");
            for (int i = 0; i < events.size(); i++) {
                sb.append(events.get(i));
                if (i < events.size() - 1) sb.append(", ");
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "getEventsToday: " + e.getMessage());
            return "Impossible de lire le calendrier.";
        }
    }

    // ── Parser commande texte ─────────────────────────────────────────────────

    public String parseCreateCommand(String cmd) {
        // Extrait le titre (après "ajoute", "crée", "rdv", "rendez-vous")
        String title = extractTitle(cmd);
        long   start = extractDateTime(cmd);
        long   dur   = 3600_000L; // 1h par défaut

        return createEvent(title, start, dur);
    }

    private String extractTitle(String cmd) {
        String c = cmd.toLowerCase();
        // Supprime les mots-clés
        String t = c
            .replaceAll("(ajoute?|crée?|nouveau|nouvelle|rdv|rendez-vous|réunion|meeting|rappel|note)(\\s+un(e)?)?", "")
            .replaceAll("(demain|aujourd'hui|ce soir|lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)", "")
            .replaceAll("(au calendrier|dans le calendrier|à \\d{1,2}h\\d{0,2}|à \\d{1,2}:\\d{2})", "")
            .trim();
        // Nettoyage
        t = t.replaceAll("\\s{2,}", " ").trim();
        if (t.isEmpty()) t = "Événement Léa";
        // Capitalise
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private long extractDateTime(String cmd) {
        String c = cmd.toLowerCase();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Jour
        if (c.contains("demain"))              cal.add(Calendar.DAY_OF_YEAR, 1);
        else if (c.contains("après-demain"))   cal.add(Calendar.DAY_OF_YEAR, 2);
        else if (c.contains("lundi"))          setNextWeekday(cal, Calendar.MONDAY);
        else if (c.contains("mardi"))          setNextWeekday(cal, Calendar.TUESDAY);
        else if (c.contains("mercredi"))       setNextWeekday(cal, Calendar.WEDNESDAY);
        else if (c.contains("jeudi"))          setNextWeekday(cal, Calendar.THURSDAY);
        else if (c.contains("vendredi"))       setNextWeekday(cal, Calendar.FRIDAY);
        else if (c.contains("samedi"))         setNextWeekday(cal, Calendar.SATURDAY);
        else if (c.contains("dimanche"))       setNextWeekday(cal, Calendar.SUNDAY);

        // Heure
        int hour = 12, minute = 0;
        try {
            int hIdx = c.indexOf('h');
            if (hIdx < 0) hIdx = c.indexOf(':');
            if (hIdx >= 0) {
                String before = c.substring(0, hIdx).replaceAll("[^0-9]", " ").trim();
                String[] parts = before.split("\\s+");
                if (parts.length > 0) hour = Integer.parseInt(parts[parts.length - 1]);
                String after = c.substring(hIdx + 1).replaceAll("[^0-9].*", "").trim();
                if (!after.isEmpty() && after.length() <= 2) minute = Integer.parseInt(after);
            } else if (c.contains("matin"))  { hour = 9; }
            else if (c.contains("midi"))     { hour = 12; }
            else if (c.contains("soir"))     { hour = 19; }
            else if (c.contains("nuit"))     { hour = 22; }
        } catch (Exception ignored) {}

        cal.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, hour)));
        cal.set(Calendar.MINUTE,      Math.max(0, Math.min(59, minute)));

        // Si l'heure est déjà passée aujourd'hui → demain
        if (cal.getTimeInMillis() <= System.currentTimeMillis() && !c.contains("demain")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }

    private void setNextWeekday(Calendar cal, int dayOfWeek) {
        int today = cal.get(Calendar.DAY_OF_WEEK);
        int diff  = dayOfWeek - today;
        if (diff <= 0) diff += 7;
        cal.add(Calendar.DAY_OF_YEAR, diff);
    }

    // ── Apps Samsung calendrier ───────────────────────────────────────────────

    public void openCalendarApp() {
        String[] calApps = {
            "com.sec.android.app.calendar",
            "com.samsung.android.calendar",
            "com.google.android.calendar"
        };
        for (String pkg : calApps) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    private void openCalendarAppWithEvent(String title, long start, long end) {
        try {
            Intent i = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.Events.TITLE,  title);
            i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start);
            i.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,   end);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            openCalendarApp();
        }
    }

    @SuppressLint("Range")
    private long getDefaultCalendarId() {
        try {
            Cursor c = ctx.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{ CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY },
                CalendarContract.Calendars.VISIBLE + "=1",
                null, CalendarContract.Calendars.IS_PRIMARY + " DESC"
            );
            if (c != null && c.moveToFirst()) {
                long id = c.getLong(c.getColumnIndex(CalendarContract.Calendars._ID));
                c.close();
                return id;
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e(TAG, "getDefaultCalendarId: " + e.getMessage());
        }
        return -1;
    }
}
