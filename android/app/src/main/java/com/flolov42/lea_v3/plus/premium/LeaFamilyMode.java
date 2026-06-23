package com.flolov42.lea_v3.plus.premium;

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
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaFamilyMode extends LeaBasePlusFeature {

    private static final String PREFS = "lea_family";

    // [age_min, age_max, daily_limit_min, restricted_content]
    private static final String[][] AGE_PROFILES = {
        {"0",  "6",  "60",  "violence,adult,gambling,horror"},
        {"7",  "12", "120", "adult,gambling,horror"},
        {"13", "17", "180", "adult,gambling"},
        {"18", "99", "999", ""},
    };

    public LeaFamilyMode(Context ctx) { super(ctx, LeaPlusDatabase.FAMILY); }

    @Override
    public void execute() {
        checkScreenTimeLimits();
        checkBedtimeRules();
    }

    private void checkScreenTimeLimits() {
        List<LeaPlusDatabase.FamilyMember> members = db.getFamilyMembers();
        for (LeaPlusDatabase.FamilyMember m : members) {
            if (m.isChild) {
                int usageToday = getUsageMinutesToday(m.id);
                int limit = getDailyLimitForAge(m.age);
                if (usageToday >= limit) {
                    notify("⏰ Temps d'écran — " + m.name,
                        m.name + " a atteint sa limite quotidienne de " + limit + " min. C'est l'heure de faire une pause !");
                    log("⏰ Limite atteinte: " + m.name + " (" + usageToday + "/" + limit + " min)");
                } else if (usageToday >= limit * 0.8) {
                    notify("⚠️ Bientôt la limite", m.name + " : " + (limit - usageToday) + " min restantes.");
                }
            }
        }
    }

    private void checkBedtimeRules() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int bedtime = prefs.getInt("child_bedtime_hour", 21);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (hour == bedtime && !today.equals(prefs.getString("bedtime_notif", ""))) {
            List<LeaPlusDatabase.FamilyMember> members = db.getFamilyMembers();
            for (LeaPlusDatabase.FamilyMember m : members) {
                if (m.isChild && m.age < 18) {
                    notify("🌙 Heure de dormir — " + m.name,
                        "Il est " + bedtime + "h ! " + m.name + " devrait poser le téléphone.");
                    log("🌙 Couvre-feu numérique: " + m.name);
                }
            }
            prefs.edit().putString("bedtime_notif", today).apply();
        }
    }

    public void addFamilyMember(String name, int age, boolean isChild, String pin) {
        String profile = getProfileForAge(age);
        db.insertFamilyMember(name, age, isChild, pin, profile);
        log("👨‍👩‍👧 Membre ajouté: " + name + " (âge: " + age + ", profil: " + profile + ")");
    }

    public boolean authenticateParent(String pin) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String parentPin = prefs.getString("parent_pin", "");
        return !parentPin.isEmpty() && parentPin.equals(pin);
    }

    public void setParentPin(String pin) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString("parent_pin", pin).apply();
        log("🔐 PIN parental configuré");
    }

    public void setBedtime(int hour) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putInt("child_bedtime_hour", hour).apply();
        log("🌙 Couvre-feu numérique réglé à " + hour + "h00");
    }

    public void recordUsage(String memberId, int minutes) {
        db.recordUsage(memberId, minutes);
    }

    private int getUsageMinutesToday(String memberId) {
        return db.getUsageMinutesToday(memberId);
    }

    private int getDailyLimitForAge(int age) {
        for (String[] p : AGE_PROFILES) {
            int min = Integer.parseInt(p[0]);
            int max = Integer.parseInt(p[1]);
            if (age >= min && age <= max) return Integer.parseInt(p[2]);
        }
        return 120;
    }

    private String getProfileForAge(int age) {
        if (age <= 6)  return "child_6";
        if (age <= 12) return "child_12";
        if (age <= 17) return "teen_17";
        return "adult";
    }

    public String getFamilyReport() {
        List<LeaPlusDatabase.FamilyMember> members = db.getFamilyMembers();
        if (members.isEmpty()) return "👨‍👩‍👧 Aucun membre — ajoutez votre famille !";
        StringBuilder sb = new StringBuilder("👨‍👩‍👧 RAPPORT FAMILLE LÉA\n\n");
        for (LeaPlusDatabase.FamilyMember m : members) {
            int usage = getUsageMinutesToday(m.id);
            int limit = getDailyLimitForAge(m.age);
            sb.append(m.isChild ? "👶" : "👤").append(" ").append(m.name)
              .append(" (").append(m.age).append(" ans)\n");
            if (m.isChild) {
                sb.append("  Écran aujourd'hui: ").append(usage).append("/").append(limit).append(" min\n");
                sb.append("  Profil: ").append(getProfileForAge(m.age)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean isContentAllowed(String memberId, String contentType) {
        LeaPlusDatabase.FamilyMember m = db.getMember(memberId);
        if (m == null || !m.isChild) return true;
        int profileIdx = 3; // adult par défaut
        for (int i = 0; i < AGE_PROFILES.length; i++) {
            if (m.age <= Integer.parseInt(AGE_PROFILES[i][1])) { profileIdx = i; break; }
        }
        String[] profile = AGE_PROFILES[profileIdx];
        return !profile[3].contains(contentType.toLowerCase());
    }
}
