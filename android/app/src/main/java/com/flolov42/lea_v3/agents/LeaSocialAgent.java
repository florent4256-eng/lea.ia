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
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.CallLog;
import java.util.ArrayList;
import java.util.List;

public class LeaSocialAgent {

    private static final String ID    = LeaAgentActivationManager.SOCIAL;
    private static final String PREFS = "lea_social";
    private static final int    DEFAULT_FORGOTTEN_DAYS = 14;

    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences         prefs;

    public LeaSocialAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getForgottenThresholdDays() {
        return prefs.getInt("forgotten_days", DEFAULT_FORGOTTEN_DAYS);
    }

    public void setForgottenThreshold(int days) {
        prefs.edit().putInt("forgotten_days", days).apply();
        db.addLog(ID, "⚙️ Seuil contacts oubliés : " + days + " jours");
    }

    private long getReminderThreshold() {
        return getForgottenThresholdDays() * 24L * 3600 * 1000;
    }

    public void execute() {
        try {
            checkForgottenContacts();
            checkBirthdays();
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Social: " + e.getMessage());
        }
    }

    private static class ContactInfo {
        String name, phone;
        long   lastContact;
    }

    private void checkForgottenContacts() {
        try {
            List<ContactInfo> contacts = getRecentCallContacts();
            long threshold = System.currentTimeMillis() - getReminderThreshold();

            for (ContactInfo c : contacts) {
                if (c.lastContact < threshold && c.lastContact > 0) {
                    long days = (System.currentTimeMillis() - c.lastContact) / (24 * 3600 * 1000);
                    String msg = "👥 T'as pas contacté " + c.name + " depuis " + days + " jours";
                    db.addLog(ID, msg);
                    // Only notify for first contact found per run
                    notif.notify(ID, "👥 Rappel Social", msg);
                    break;
                }
            }
        } catch (SecurityException e) {
            db.addLog(ID, "🔒 Permission contacts/appels requise");
        }
    }

    private List<ContactInfo> getRecentCallContacts() throws SecurityException {
        List<ContactInfo> list = new ArrayList<>();
        ContentResolver cr = ctx.getContentResolver();
        // SecurityException remontée volontairement pour que checkForgottenContacts() la logue
        Cursor c = cr.query(CallLog.Calls.CONTENT_URI,
            new String[]{
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE
            },
            CallLog.Calls.TYPE + " IN (?,?)",
            new String[]{
                String.valueOf(CallLog.Calls.INCOMING_TYPE),
                String.valueOf(CallLog.Calls.OUTGOING_TYPE)
            },
            CallLog.Calls.DATE + " DESC");

        if (c == null) return list;

        java.util.Map<String, ContactInfo> seen = new java.util.HashMap<>();
        try {
            while (c.moveToNext() && seen.size() < 20) {
                String name  = c.getString(0);
                String phone = c.getString(1);
                long   date  = c.getLong(2);
                if (name == null || name.isEmpty()) continue;
                if (!seen.containsKey(phone)) {
                    ContactInfo info = new ContactInfo();
                    info.name        = name;
                    info.phone       = phone;
                    info.lastContact = date;
                    seen.put(phone, info);
                }
            }
        } finally {
            c.close();
        }
        list.addAll(seen.values());
        return list;
    }

    private void checkBirthdays() {
        try {
            java.util.Map<Integer, Integer> upcomingKeys = buildUpcomingKeys(7);
            List<BirthdayInfo> birthdays = queryBirthdays(upcomingKeys);

            for (BirthdayInfo b : birthdays) {
                db.addLog(ID, b.emoji + " " + b.label + " c'est l'anniversaire de " + b.name + "!");
                if (b.daysAhead == 0) {
                    notif.notify(ID, "🎂 Anniversaire!", "Aujourd'hui : " + b.name);
                } else if (b.daysAhead == 1) {
                    notif.notify(ID, "🎁 Anniversaire demain", b.name);
                }
                // Pas de notification système au-delà de J+1 pour éviter le spam
            }
        } catch (SecurityException e) {
            db.addLog(ID, "🔒 Permission contacts requise pour les anniversaires");
        }
    }

    /** Construit un map (mois*100+jour) → jours restants, pour les N prochains jours. */
    private java.util.Map<Integer, Integer> buildUpcomingKeys(int days) {
        java.util.Map<Integer, Integer> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, i);
            int key = (cal.get(java.util.Calendar.MONTH) + 1) * 100
                    + cal.get(java.util.Calendar.DAY_OF_MONTH);
            map.put(key, i);
        }
        return map;
    }

    private List<BirthdayInfo> queryBirthdays(java.util.Map<Integer, Integer> upcomingKeys) {
        List<BirthdayInfo> result = new ArrayList<>();
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(
            ContactsContract.Data.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Event.START_DATE
            },
            ContactsContract.Data.MIMETYPE + "=? AND " +
            ContactsContract.CommonDataKinds.Event.TYPE + "=?",
            new String[]{
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                String.valueOf(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
            },
            null);

        if (c == null) return result;
        try {
            while (c.moveToNext()) {
                String name = c.getString(0);
                String date = c.getString(1);
                if (name == null || date == null) continue;

                // Format Android : YYYY-MM-DD ou --MM-DD
                String[] parts = date.replace("--", "").split("-");
                if (parts.length < 2) continue;
                try {
                    int month = Integer.parseInt(parts[parts.length - 2]);
                    int day   = Integer.parseInt(parts[parts.length - 1]);
                    int key = month * 100 + day;
                    Integer daysAhead = upcomingKeys.get(key);
                    if (daysAhead != null) {
                        BirthdayInfo b = new BirthdayInfo();
                        b.name      = name;
                        b.daysAhead = daysAhead;
                        b.label     = daysAhead == 0 ? "Aujourd'hui"
                                    : daysAhead == 1 ? "Demain"
                                    : "Dans " + daysAhead + " jours";
                        b.emoji     = daysAhead == 0 ? "🎂" : "🎁";
                        result.add(b);
                    }
                } catch (NumberFormatException ignored) {}
            }
        } finally {
            c.close();
        }
        return result;
    }

    public static class BirthdayInfo {
        public String name;
        public String label;
        public String emoji;
        public int    daysAhead;
    }

    /** Retourne les contacts non appelés depuis plus de 14 jours, pour l'UI. */
    public List<String> getOldContacts() {
        try {
            List<ContactInfo> contacts = getRecentCallContacts();
            long threshold = System.currentTimeMillis() - getReminderThreshold();
            List<String> result = new ArrayList<>();
            for (ContactInfo c : contacts) {
                if (c.lastContact < threshold && c.lastContact > 0) {
                    long days = (System.currentTimeMillis() - c.lastContact)
                              / (24L * 3600 * 1000);
                    result.add(c.name + " — " + days + " jours sans contact");
                }
            }
            return result;
        } catch (SecurityException e) {
            return new ArrayList<>();
        }
    }

    /** Retourne les anniversaires des 7 prochains jours pour l'UI (thread appelant = bg). */
    public List<BirthdayInfo> getUpcomingBirthdays() {
        try {
            return queryBirthdays(buildUpcomingKeys(7));
        } catch (SecurityException e) {
            return new ArrayList<>();
        }
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
