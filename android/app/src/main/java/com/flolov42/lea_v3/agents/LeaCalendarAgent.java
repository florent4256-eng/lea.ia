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


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaCalendarAgent {

    private static final String ID = LeaAgentActivationManager.CALENDAR;

    // Prefer no meetings after this hour (17:00)
    private static final int NO_MEETING_AFTER_HOUR = 17;

    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;

    public LeaCalendarAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
    }

    public void execute() {
        try {
            List<EventInfo> upcoming = getUpcomingEvents(24);
            if (upcoming.isEmpty()) {
                db.addLog(ID, "📅 Aucun événement dans les prochaines 24h");
                return;
            }

            checkConflicts(upcoming);
            checkUpcoming30Min(upcoming);
            checkLateEvents(upcoming);

        } catch (SecurityException e) {
            db.addLog(ID, "🔒 Permission calendrier requise");
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur: " + e.getMessage());
        }
    }

    private static class EventInfo {
        long   id, begin, end;
        String title, location;
    }

    private List<EventInfo> getUpcomingEvents(int hours) {
        List<EventInfo> list = new ArrayList<>();
        try {
            long now   = System.currentTimeMillis();
            long until = now + hours * 3600 * 1000L;

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, now);
            ContentUris.appendId(builder, until);

            ContentResolver cr = ctx.getContentResolver();
            Cursor c = cr.query(builder.build(),
                new String[]{
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.EVENT_LOCATION
                },
                null, null, CalendarContract.Instances.BEGIN + " ASC");

            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        EventInfo e = new EventInfo();
                        e.id       = c.getLong(0);
                        e.begin    = c.getLong(1);
                        e.end      = c.getLong(2);
                        e.title    = c.getString(3);
                        e.location = c.getString(4);
                        if (e.title != null) list.add(e);
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Impossible de lire le calendrier: " + e.getMessage());
        }
        return list;
    }

    private void checkConflicts(List<EventInfo> events) {
        for (int i = 0; i < events.size() - 1; i++) {
            EventInfo a = events.get(i);
            EventInfo b = events.get(i + 1);
            // Conflict: b starts before a ends
            if (b.begin < a.end) {
                String msg = "⚠️ CONFLIT: «" + a.title + "» et «" + b.title + "» se chevauchent!";
                db.addLog(ID, msg);
                notif.notify(ID, "📅 Conflit d'horaire", msg);
            }
        }
    }

    private void checkUpcoming30Min(List<EventInfo> events) {
        long now    = System.currentTimeMillis();
        long in30   = now + 30 * 60 * 1000L;

        for (EventInfo e : events) {
            if (e.begin > now && e.begin <= in30) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String location = e.location != null ? " — " + e.location : "";
                String msg = "⏰ RDV dans 30min: «" + e.title + "» à " + sdf.format(new Date(e.begin)) + location;
                db.addLog(ID, msg);
                notif.notify(ID, "📅 Rappel RDV", msg);
            }
        }
    }

    private void checkLateEvents(List<EventInfo> events) {
        for (EventInfo e : events) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(e.begin);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= NO_MEETING_AFTER_HOUR) {
                String msg = "⚠️ Réunion tardive détectée: «" + e.title + "» après " + NO_MEETING_AFTER_HOUR + "h";
                db.addLog(ID, msg);
            }
        }
    }

    public String getNextEvent() {
        List<EventInfo> events = getUpcomingEvents(2);
        if (events.isEmpty()) return "Aucun événement dans les 2 prochaines heures";
        EventInfo next = events.get(0);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return "Prochain: «" + next.title + "» à " + sdf.format(new Date(next.begin));
    }

    public List<String> getUpcomingEventsSummary(int hours) {
        List<EventInfo> raw = getUpcomingEvents(hours);
        List<String> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd/MM HH:mm", Locale.getDefault());
        for (EventInfo e : raw) {
            StringBuilder sb = new StringBuilder();
            sb.append("📌 ").append(e.title).append("\n");
            sb.append("🕐 ").append(sdf.format(new Date(e.begin)));
            long dur = (e.end - e.begin) / 60000;
            if (dur > 0) sb.append(" (").append(dur).append(" min)");
            if (e.location != null && !e.location.isEmpty())
                sb.append("\n📍 ").append(e.location);
            result.add(sb.toString());
        }
        return result;
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
