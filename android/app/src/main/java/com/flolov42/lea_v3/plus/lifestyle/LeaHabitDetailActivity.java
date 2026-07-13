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
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

public class LeaHabitDetailActivity extends LeaFeatureDetailActivity {

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

    @Override protected String getFeatureId() { return LeaPlusDatabase.HABITS; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        List<LeaPlusDatabase.HabitRow> habits = db.getActiveHabits();

        // ── Hero stats ─────────────────────────────────────────────────────
        int totalStreak = 0;
        int todayDone = 0;
        long now = System.currentTimeMillis();
        for (LeaPlusDatabase.HabitRow h : habits) {
            totalStreak += h.streak;
            if ((now - h.lastCheck) < 86400_000L) todayDone++;
        }
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(10), dp(12), 0); statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("🔥", String.valueOf(totalStreak), "Streak total", ORANGE));
        statsRow.addView(miniStat("✅", todayDone + "/" + habits.size(), "Faites auj.", GREEN));
        statsRow.addView(miniStat("📋", String.valueOf(habits.size()), "Habitudes", CYAN));
        parent.addView(statsRow);

        // ── Liste habitudes ────────────────────────────────────────────────
        secHeader(parent, "🔗 MES HABITUDES");

        if (habits.isEmpty()) {
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
            TextView eIcon = tv("🌱", 40, 0xFF1E3A5F, Typeface.NORMAL); eIcon.setGravity(Gravity.CENTER); empty.addView(eIcon);
            TextView eTitle = tv("Aucune habitude", 14, WHITE, Typeface.BOLD);
            eTitle.setGravity(Gravity.CENTER); eTitle.setPadding(0, dp(8), 0, dp(4)); empty.addView(eTitle);
            TextView eSub = tv("Crée ta première habitude\net construis une routine solide", 12, DIM, Typeface.NORMAL);
            eSub.setGravity(Gravity.CENTER); empty.addView(eSub);
            parent.addView(empty);
        } else {
            for (LeaPlusDatabase.HabitRow h : habits) {
                parent.addView(habitCard(h));
            }
        }

        // ── Bouton ajouter ─────────────────────────────────────────────────
        Button addBtn = new Button(this);
        addBtn.setText("+ Ajouter une habitude");
        addBtn.setTextColor(CYAN);
        GradientDrawable addBtnGd = new GradientDrawable();
        addBtnGd.setColor((CYAN & 0x00FFFFFF) | 0x22000000);
        addBtnGd.setCornerRadius(dp(14));
        addBtnGd.setStroke(dp(1), CYAN);
        addBtn.setBackground(new RippleDrawable(ColorStateList.valueOf((CYAN & 0x00FFFFFF) | 0x55000000), addBtnGd, null));
        addBtn.setElevation(dp(1));
        addBtn.setTextSize(13);
        addBtn.setTypeface(null, Typeface.BOLD);
        addBtn.setAllCaps(false);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(48));
        addLp.setMargins(dp(12), dp(10), dp(12), dp(24));
        addBtn.setLayoutParams(addLp);
        addBtn.setOnClickListener(v -> showAddHabitDialog());
        parent.addView(addBtn);
    }

    private View habitCard(LeaPlusDatabase.HabitRow h) {
        boolean done = (System.currentTimeMillis() - h.lastCheck) < 86400_000L;
        int streakColor = h.streak >= 30 ? GOLD : h.streak >= 7 ? ORANGE : GREEN;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        cardGd.setCornerRadius(dp(18));
        cardGd.setStroke(dp(1), GLASS_BORDER);
        card.setElevation(dp(2));
        card.setBackground(cardGd);
        card.setPadding(dp(14), dp(0), dp(14), dp(12));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(dp(12), dp(4), dp(12), 0); card.setLayoutParams(clp);

        // Barre d'accent
        View accent = new View(this);
        accent.setBackgroundColor(done ? GREEN : streakColor);
        card.addView(accent, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(0, dp(12), 0, 0);

        // Nom + streak
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView nameTv = tv(h.name, 14, WHITE, Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f)); topRow.addView(nameTv);
        TextView streakTv = tv("🔥 " + h.streak + "j", 14, streakColor, Typeface.BOLD);
        topRow.addView(streakTv); inner.addView(topRow);

        // Infos
        inner.addView(tv(h.frequency + "  ·  Rappel " + h.reminderHour + "h  ·  Record " + h.bestStreak + "j",
            10, DIM2, Typeface.NORMAL));

        // Bouton check-in
        Button checkBtn = new Button(this);
        checkBtn.setText(done ? "✅  Fait aujourd'hui !" : "◉  CHECK-IN");
        checkBtn.setTextColor(done ? GREEN : WHITE);
        setCheckBtnBg(checkBtn, done);
        checkBtn.setElevation(dp(1));
        checkBtn.setTextSize(12);
        checkBtn.setTypeface(null, Typeface.BOLD);
        checkBtn.setAllCaps(false);
        checkBtn.setEnabled(!done);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, dp(40));
        btnLp.setMargins(0, dp(10), 0, 0); checkBtn.setLayoutParams(btnLp);
        if (!done) {
            checkBtn.setOnClickListener(v -> {
                db.checkInHabit(h.id);
                db.addCoins(5, "Check-in: " + h.name);
                Toast.makeText(this, "✅ " + h.name + " — +5 💰 !", Toast.LENGTH_SHORT).show();
                checkBtn.setText("✅  Fait aujourd'hui !");
                setCheckBtnBg(checkBtn, true);
                checkBtn.setTextColor(GREEN);
                checkBtn.setEnabled(false);
            });
        }
        inner.addView(checkBtn);
        card.addView(inner);
        return card;
    }

    private void setCheckBtnBg(Button btn, boolean done) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(done ? 0x2210B981 : GREEN);
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), GREEN);
        btn.setBackground(new RippleDrawable(ColorStateList.valueOf((GREEN & 0x00FFFFFF) | 0x55000000), gd, null));
    }

    private void showAddHabitDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setBackgroundColor(BG);
        form.setPadding(dp(20), dp(12), dp(20), dp(8));
        EditText etName = new EditText(this);
        etName.setHint("Nom (ex: Méditation)"); etName.setSingleLine();
        etName.setTextColor(WHITE); etName.setHintTextColor(DIM); etName.setBackgroundColor(CARD);
        etName.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2);
        nlp.setMargins(0, dp(4), 0, dp(8)); etName.setLayoutParams(nlp);
        form.addView(etName);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Quotidien", "Hebdomadaire"}));
        form.addView(sp);
        new AlertDialog.Builder(this).setTitle("Nouvelle habitude").setView(form)
            .setPositiveButton("Créer", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (!name.isEmpty()) {
                    db.insertHabit(name, sp.getSelectedItem().toString(), 9);
                    Toast.makeText(this, "✅ Habitude créée !", Toast.LENGTH_SHORT).show();
                    recreate();
                }
            })
            .setNegativeButton("Annuler", null).show();
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private LinearLayout miniStat(String icon, String val, String lbl, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER);
        GradientDrawable sGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        sGd.setCornerRadius(dp(14));
        sGd.setStroke(dp(1), GLASS_BORDER);
        s.setElevation(dp(2));
        s.setBackground(sGd);
        s.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 18, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(val, 13, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(lbl, 9, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
