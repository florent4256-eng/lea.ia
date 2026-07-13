package com.flolov42.lea_v3.plus.lifestyle;

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

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Calendar;
import java.util.List;

public class LeaLifeOSDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF020617;
    private static final int CARD  = 0xFF0B1526;
    private static final int CARD2 = 0xFF0F1B2E;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;
    private static final int GLASS_BORDER = 0x1EFFFFFF;

    @Override protected String getFeatureId() { return LeaPlusDatabase.LIFE_OS; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        List<LeaPlusDatabase.ScheduleItem> schedule = db.getTodaySchedule();
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        // ── Hero : heure actuelle ──────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        GradientDrawable heroGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        heroGd.setCornerRadius(dp(20));
        heroGd.setStroke(dp(1), GLASS_BORDER);
        hero.setElevation(dp(2));
        hero.setBackground(heroGd);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        String hour = currentHour < 12 ? "🌅 Matin" : currentHour < 18 ? "☀️ Après-midi" : "🌙 Soirée";
        TextView timeTv = tv(String.format("%02d:00", currentHour), 36, CYAN, Typeface.BOLD);
        timeTv.setGravity(Gravity.CENTER); hero.addView(timeTv);
        TextView dayTv = tv(hour + "  ·  " + schedule.size() + " activité" + (schedule.size() > 1 ? "s" : "") + " aujourd'hui",
            12, DIM2, Typeface.NORMAL);
        dayTv.setGravity(Gravity.CENTER); hero.addView(dayTv);
        parent.addView(hero);

        // ── Bouton ajouter ─────────────────────────────────────────────────
        LinearLayout secRow = new LinearLayout(this);
        secRow.setOrientation(LinearLayout.HORIZONTAL);
        secRow.setGravity(Gravity.CENTER_VERTICAL);
        secRow.setPadding(dp(16), dp(14), dp(12), dp(6));
        TextView secTv = tv("🗓️ PLANNING DU JOUR", 11, GOLD, Typeface.BOLD);
        secTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f)); secRow.addView(secTv);
        Button addBtn = new Button(this);
        addBtn.setText("+ Ajouter"); addBtn.setTextColor(CYAN);
        addBtn.setBackground(pillBg(CYAN, false));
        addBtn.setTextSize(11); addBtn.setTypeface(null, Typeface.BOLD); addBtn.setAllCaps(false);
        addBtn.setPadding(dp(14), dp(6), dp(14), dp(6));
        addBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(32)));
        addBtn.setOnClickListener(v -> showAddScheduleDialog());
        secRow.addView(addBtn);
        parent.addView(secRow);

        // ── Timeline ───────────────────────────────────────────────────────
        if (schedule.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            GradientDrawable emptyGd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ lighten(CARD, 0.06f), CARD });
            emptyGd.setCornerRadius(dp(18));
            emptyGd.setStroke(dp(1), GLASS_BORDER);
            empty.setElevation(dp(2));
            empty.setBackground(emptyGd);
            empty.setPadding(dp(24), dp(36), dp(24), dp(36));
            LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(-1, -2);
            eLp.setMargins(dp(12), dp(4), dp(12), 0); empty.setLayoutParams(eLp);
            TextView eIcon = tv("🗓️", 40, 0xFF1E3A5F, Typeface.NORMAL); eIcon.setGravity(Gravity.CENTER); empty.addView(eIcon);
            TextView eTitle = tv("Planning vide", 14, WHITE, Typeface.BOLD);
            eTitle.setGravity(Gravity.CENTER); eTitle.setPadding(0, dp(8), 0, dp(4)); empty.addView(eTitle);
            TextView eSub = tv("Aucune activité planifiée.\nAppuie sur + Ajouter pour organiser ta journée.", 12, DIM, Typeface.NORMAL);
            eSub.setGravity(Gravity.CENTER); empty.addView(eSub);
            parent.addView(empty);
        } else {
            for (LeaPlusDatabase.ScheduleItem item : schedule) {
                parent.addView(scheduleCard(item, currentHour));
            }
        }

        // ── Routines rapides ───────────────────────────────────────────────
        secHeader(parent, "⚡ ROUTINES RAPIDES");
        String[][] routines = {
            {"🌅", "Réveil zen",        "Étirement + eau + journal"},
            {"💪", "Sport du soir",     "30 min cardio + douche froide"},
            {"🍽️", "Repas équilibré",   "Protéines + légumes + eau"},
            {"😴", "Routine nuit",      "Téléphone off + lecture 20 min"},
        };
        LinearLayout routCard = new LinearLayout(this);
        routCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable routGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        routGd.setCornerRadius(dp(18));
        routGd.setStroke(dp(1), GLASS_BORDER);
        routCard.setElevation(dp(2));
        routCard.setBackground(routGd);
        routCard.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(-1, -2);
        rcLp.setMargins(dp(12), dp(4), dp(12), dp(24)); routCard.setLayoutParams(rcLp);
        for (int i = 0; i < routines.length; i++) {
            String[] r = routines[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            TextView emo = tv(r[0], 20, WHITE, Typeface.NORMAL); emo.setMinWidth(dp(36)); row.addView(emo);
            LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            texts.addView(tv(r[1], 12, WHITE, Typeface.BOLD)); texts.addView(tv(r[2], 10, DIM2, Typeface.NORMAL));
            row.addView(texts);
            routCard.addView(row);
            if (i < routines.length - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); routCard.addView(sp); }
        }
        parent.addView(routCard);
    }

    private View scheduleCard(LeaPlusDatabase.ScheduleItem item, int currentHour) {
        boolean isPast    = item.hour < currentHour;
        boolean isCurrent = item.hour == currentHour;
        int color = isCurrent ? GOLD : (isPast ? DIM : CYAN);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int cardBase = isCurrent ? 0xFF0D1F10 : CARD;
        GradientDrawable cardGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(cardBase, 0.06f), cardBase });
        cardGd.setCornerRadius(dp(16));
        cardGd.setStroke(dp(1), isCurrent ? GREEN : GLASS_BORDER);
        card.setElevation(dp(2));
        card.setBackground(cardGd);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(3), dp(12), 0); card.setLayoutParams(lp);

        // Heure
        String min = item.minute < 10 ? "0" + item.minute : String.valueOf(item.minute);
        LinearLayout timeCol = new LinearLayout(this); timeCol.setOrientation(LinearLayout.VERTICAL);
        timeCol.setGravity(Gravity.CENTER); timeCol.setMinimumWidth(dp(50));
        timeCol.addView(tv(item.hour + "h" + min, 14, color, Typeface.BOLD));
        card.addView(timeCol);

        // Séparateur vertical
        View vBar = new View(this); vBar.setBackgroundColor(isCurrent ? GREEN : (isPast ? CARD2 : 0xFF002233));
        LinearLayout.LayoutParams vbLp = new LinearLayout.LayoutParams(dp(2), dp(40));
        vbLp.setMargins(dp(8), 0, dp(12), 0); vBar.setLayoutParams(vbLp); card.addView(vBar);

        // Titre + description
        LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        texts.addView(tv(item.title, 13, isPast ? DIM2 : WHITE, Typeface.BOLD));
        if (item.description != null && !item.description.isEmpty())
            texts.addView(tv(item.description, 10, DIM, Typeface.NORMAL));
        card.addView(texts);

        if (isCurrent) {
            TextView nowTv = tv("▶ MAINTENANT", 9, GREEN, Typeface.BOLD);
            nowTv.setBackground(pillBg(GREEN, false));
            nowTv.setPadding(dp(8), dp(4), dp(8), dp(4)); card.addView(nowTv);
        }
        return card;
    }

    private void showAddScheduleDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BG);
        layout.setPadding(dp(20), dp(12), dp(20), dp(8));
        EditText etTitle = new EditText(this);
        etTitle.setHint("Titre (ex: Sport)"); etTitle.setSingleLine();
        etTitle.setTextColor(WHITE); etTitle.setHintTextColor(DIM); etTitle.setBackgroundColor(CARD);
        etTitle.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.setMargins(0, dp(4), 0, dp(8));
        etTitle.setLayoutParams(tp); layout.addView(etTitle);
        EditText etDesc = new EditText(this);
        etDesc.setHint("Description (optionnel)"); etDesc.setSingleLine();
        etDesc.setTextColor(WHITE); etDesc.setHintTextColor(DIM); etDesc.setBackgroundColor(CARD);
        etDesc.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(-1, -2); dp2.setMargins(0, dp(4), 0, dp(8));
        etDesc.setLayoutParams(dp2); layout.addView(etDesc);
        NumberPicker hp = new NumberPicker(this);
        hp.setMinValue(0); hp.setMaxValue(23);
        hp.setValue(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        layout.addView(hp);
        new AlertDialog.Builder(this).setTitle("Ajouter au planning").setView(layout)
            .setPositiveButton("Ajouter", (d, w) -> {
                String title = etTitle.getText().toString().trim();
                if (!title.isEmpty()) {
                    db.insertScheduleItem(title, etDesc.getText().toString().trim(), hp.getValue(), 0);
                    Toast.makeText(this, "✅ Activité ajoutée !", Toast.LENGTH_SHORT).show();
                    recreate();
                }
            })
            .setNegativeButton("Annuler", null).show();
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
