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


import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.Telephony;
import android.util.Log;
import java.util.Calendar;

/**
 * Résume les SMS, appels manqués et événements du jour sous forme de message vocal.
 */
public class LeaNotificationSummarizer {

    private static final String TAG = "LeaNotifSum";
    private final Context ctx;
    private final LeaCalendarManager calMgr;

    public LeaNotificationSummarizer(Context ctx) {
        this.ctx    = ctx.getApplicationContext();
        this.calMgr = new LeaCalendarManager(ctx);
    }

    // ── Résumé complet ────────────────────────────────────────────────────────

    public String getSummary() {
        StringBuilder sb = new StringBuilder("📊 Voilà ton résumé du jour. ");

        // SMS non lus
        int sms = countUnreadSms();
        if (sms > 0)
            sb.append(sms == 1 ? "1 SMS non lu. " : sms + " SMS non lus. ");
        else
            sb.append("Aucun SMS non lu. ");

        // Appels manqués
        int missed = countMissedCallsToday();
        if (missed > 0)
            sb.append(missed == 1 ? "1 appel manqué aujourd'hui. " : missed + " appels manqués. ");

        // Événements calendrier
        String events = calMgr.getEventsToday();
        if (!events.startsWith("Aucun")) sb.append(events);

        // Conclusion
        if (sms == 0 && missed == 0) sb.append("Tout est calme, rien d'urgent.");

        return sb.toString();
    }

    // ── SMS non lus ───────────────────────────────────────────────────────────

    @SuppressLint("Range")
    private int countUnreadSms() {
        try {
            Cursor c = ctx.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{ Telephony.Sms._ID },
                Telephony.Sms.READ + "=0 AND " + Telephony.Sms.TYPE + "=" + Telephony.Sms.MESSAGE_TYPE_INBOX,
                null, null);
            int count = 0;
            if (c != null) { count = c.getCount(); c.close(); }
            return count;
        } catch (Exception e) {
            Log.w(TAG, "countUnreadSms: " + e.getMessage());
            return 0;
        }
    }

    // ── Appels manqués aujourd'hui ────────────────────────────────────────────

    @SuppressLint("Range")
    private int countMissedCallsToday() {
        try {
            Calendar startOfDay = Calendar.getInstance();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            Cursor c = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{ CallLog.Calls._ID },
                CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE
                    + " AND " + CallLog.Calls.DATE + ">?",
                new String[]{ String.valueOf(startOfDay.getTimeInMillis()) },
                null);
            int count = 0;
            if (c != null) { count = c.getCount(); c.close(); }
            return count;
        } catch (Exception e) {
            Log.w(TAG, "countMissedCalls: " + e.getMessage());
            return 0;
        }
    }

    // ── Derniers expéditeurs SMS ──────────────────────────────────────────────

    @SuppressLint("Range")
    public String getLatestSenders(int max) {
        try {
            Cursor c = ctx.getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{ Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE },
                Telephony.Sms.READ + "=0",
                null, Telephony.Sms.DATE + " DESC LIMIT " + max);
            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return "Aucun message non lu.";
            }
            StringBuilder sb = new StringBuilder("Derniers messages : ");
            try {
                do {
                    String addr = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS));
                    String body = c.getString(c.getColumnIndex(Telephony.Sms.BODY));
                    if (body != null && body.length() > 40) body = body.substring(0, 40) + "…";
                    sb.append("De ").append(addr).append(" : ").append(body).append(". ");
                } while (c.moveToNext());
            } finally {
                c.close();
            }
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "getLatestSenders: " + e.getMessage());
            return "Impossible de lire les SMS.";
        }
    }
}
