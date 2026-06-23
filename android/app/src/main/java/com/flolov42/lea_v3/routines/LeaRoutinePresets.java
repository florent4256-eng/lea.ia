package com.flolov42.lea_v3.routines;

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
import java.util.List;

public class LeaRoutinePresets {

    public static void loadIfEmpty(Context ctx) {
        LeaRoutineDatabase db  = LeaRoutineDatabase.get(ctx);
        List<LeaRoutineDatabase.RoutineRow> all = db.getAllRoutines();
        for (LeaRoutineDatabase.RoutineRow r : all) {
            if (r.preset) return; // already loaded
        }

        // 🏠 Maison — WiFi ON, BT ON, son normal, luminosité 60%
        insert(db, "Maison", "🏠", 0xFF4CAF50,
            "[{\"type\":\"LOCATION\",\"place\":\"MAISON\",\"lat\":0,\"lng\":0,\"radius\":100}]",
            "[{\"type\":\"WIFI\",\"enabled\":true}," +
             "{\"type\":\"BLUETOOTH\",\"enabled\":true}," +
             "{\"type\":\"SOUND_MODE\",\"mode\":\"NORMAL\"}," +
             "{\"type\":\"BRIGHTNESS\",\"value\":60}]");

        // 💼 Travail — Vibreur, WiFi ON, BT OFF, luminosité 80%
        insert(db, "Travail", "💼", 0xFF2196F3,
            "[{\"type\":\"LOCATION\",\"place\":\"TRAVAIL\",\"lat\":0,\"lng\":0,\"radius\":100}]",
            "[{\"type\":\"SOUND_MODE\",\"mode\":\"VIBRATE\"}," +
             "{\"type\":\"WIFI\",\"enabled\":true}," +
             "{\"type\":\"BLUETOOTH\",\"enabled\":false}," +
             "{\"type\":\"BRIGHTNESS\",\"value\":80}]");

        // 🚗 Voiture — BT ON (auto-car), volume fort, luminosité max
        insert(db, "Voiture", "🚗", 0xFFFF9800,
            "[{\"type\":\"BLUETOOTH_CONNECTED\",\"device\":\"\"}]",
            "[{\"type\":\"BLUETOOTH\",\"enabled\":true}," +
             "{\"type\":\"VOLUME\",\"level\":\"HIGH\"}," +
             "{\"type\":\"BRIGHTNESS\",\"value\":100}," +
             "{\"type\":\"SOUND_MODE\",\"mode\":\"NORMAL\"}]");

        // 😴 Sommeil — 22h30, muet, luminosité 0%, WiFi/BT OFF
        insert(db, "Sommeil", "😴", 0xFF9C27B0,
            "[{\"type\":\"TIME_EXACT\",\"time\":\"22:30\"}]",
            "[{\"type\":\"SOUND_MODE\",\"mode\":\"SILENT\"}," +
             "{\"type\":\"BRIGHTNESS\",\"value\":0}," +
             "{\"type\":\"WIFI\",\"enabled\":false}," +
             "{\"type\":\"BLUETOOTH\",\"enabled\":false}]");

        // 🏋️ Exercice — 6h-8h, volume fort, luminosité max, BT ON
        insert(db, "Exercice", "🏋️", 0xFFF44336,
            "[{\"type\":\"TIME\",\"start\":\"06:00\",\"end\":\"08:00\"}]",
            "[{\"type\":\"VOLUME\",\"level\":\"HIGH\"}," +
             "{\"type\":\"BRIGHTNESS\",\"value\":100}," +
             "{\"type\":\"BLUETOOTH\",\"enabled\":true}," +
             "{\"type\":\"SOUND_MODE\",\"mode\":\"NORMAL\"}]");

        // 🎮 Gaming — volume fort, luminosité max, WiFi ON, son normal
        insert(db, "Gaming", "🎮", 0xFFE91E63,
            "[]",
            "[{\"type\":\"VOLUME\",\"level\":\"HIGH\"}," +
             "{\"type\":\"BRIGHTNESS\",\"value\":100}," +
             "{\"type\":\"WIFI\",\"enabled\":true}," +
             "{\"type\":\"SOUND_MODE\",\"mode\":\"NORMAL\"}]");
    }

    private static void insert(LeaRoutineDatabase db, String name, String icon, int color,
                                String conditions, String actions) {
        LeaRoutineDatabase.RoutineRow r = new LeaRoutineDatabase.RoutineRow();
        r.name           = name;
        r.icon           = icon;
        r.iconColor      = color;
        r.conditionsJson = conditions;
        r.actionsJson    = actions;
        r.active         = false;
        r.preset         = true;
        db.insertRoutine(r);
    }
}
