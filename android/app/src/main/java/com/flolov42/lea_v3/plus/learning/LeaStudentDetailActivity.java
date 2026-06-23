package com.flolov42.lea_v3.plus.learning;

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
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

public class LeaStudentDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF000D1A;
    private static final int CARD  = 0xFF001A2E;
    private static final int CARD2 = 0xFF00243F;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int RED   = 0xFFEF4444;
    private static final int GOLD  = 0xFFFFD700;
    private static final int BLUE  = 0xFF2563EB;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;

    @Override protected String getFeatureId() { return LeaPlusDatabase.STUDENT; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);
        List<LeaPlusDatabase.SubjectRow> subjects = db.getSubjects();

        // Calcul moyenne générale
        double totalW = 0, totalC = 0;
        for (LeaPlusDatabase.SubjectRow s : subjects) { totalW += s.average * s.coef; totalC += s.coef; }
        double genAvg = totalC > 0 ? totalW / totalC : 0;

        // ── Hero : moyenne générale ────────────────────────────────────────
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setBackgroundColor(CARD);
        hero.setPadding(dp(20), dp(24), dp(20), dp(20));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(dp(12), dp(8), dp(12), 0); hero.setLayoutParams(hLp);

        TextView bookEmo = tv("📚", 40, WHITE, Typeface.NORMAL);
        bookEmo.setGravity(Gravity.CENTER); hero.addView(bookEmo);

        int avgColor = genAvg >= 14 ? GREEN : genAvg >= 10 ? CYAN : genAvg >= 8 ? ORANGE : RED;
        String avgTxt = subjects.isEmpty() ? "—" : String.format("%.1f", genAvg) + "/20";
        TextView avgTv = tv(avgTxt, 36, avgColor, Typeface.BOLD);
        avgTv.setGravity(Gravity.CENTER); avgTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(avgTv);

        String mention = genAvg >= 16 ? "Très Bien ✨" : genAvg >= 14 ? "Bien 👍" :
                         genAvg >= 12 ? "Assez Bien" : genAvg >= 10 ? "Passable" : "À améliorer";
        TextView mTv = tv(subjects.isEmpty() ? "Aucune matière enregistrée" : mention, 12, DIM2, Typeface.NORMAL);
        mTv.setGravity(Gravity.CENTER); hero.addView(mTv);
        parent.addView(hero);

        // ── Stats rapides ──────────────────────────────────────────────────
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(8), dp(12), 0); statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("📖", String.valueOf(subjects.size()), "Matières", CYAN));
        long bestAvg = 0; String bestName = "—";
        for (LeaPlusDatabase.SubjectRow s : subjects) if (s.average > bestAvg) { bestAvg = (long)s.average; bestName = s.name; }
        statsRow.addView(miniStat("🏆", bestName.length() > 8 ? bestName.substring(0,7) + "…" : bestName, "Meilleure", GOLD));
        statsRow.addView(miniStat("📅", db.getStudySessionsCount() + "h", "Étudiées", GREEN));
        parent.addView(statsRow);

        // ── Matières ──────────────────────────────────────────────────────
        secHeader(parent, "📖 MES MATIÈRES");
        if (subjects.isEmpty()) {
            LinearLayout empty = emptyCard("📚", "Aucune matière", "Ajoute tes matières pour suivre\nta progression scolaire");
            parent.addView(empty);
        } else {
            for (LeaPlusDatabase.SubjectRow s : subjects) {
                parent.addView(subjectCard(s));
            }
        }

        // ── Bouton ajouter ─────────────────────────────────────────────────
        Button addBtn = new Button(this);
        addBtn.setText("+ Ajouter une matière");
        addBtn.setTextColor(CYAN); addBtn.setBackgroundColor(CARD);
        addBtn.setTextSize(13); addBtn.setTypeface(null, Typeface.BOLD); addBtn.setAllCaps(false);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(48));
        addLp.setMargins(dp(12), dp(10), dp(12), dp(24));
        addBtn.setLayoutParams(addLp);
        addBtn.setOnClickListener(v -> showAddSubjectDialog());
        parent.addView(addBtn);
    }

    private View subjectCard(LeaPlusDatabase.SubjectRow s) {
        int avgColor = s.average >= 14 ? GREEN : s.average >= 10 ? CYAN : s.average >= 8 ? ORANGE : RED;
        float frac = (float) s.average / 20f;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(CARD);
        card.setPadding(dp(14), dp(0), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), 0); card.setLayoutParams(lp);

        View accent = new View(this);
        accent.setBackgroundColor(avgColor);
        card.addView(accent, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(0, dp(10), 0, 0);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView nameTv = tv(s.name, 14, WHITE, Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f)); row.addView(nameTv);
        row.addView(tv("coef " + s.coef, 10, DIM2, Typeface.NORMAL));
        TextView avgTv = tv(String.format("  %.1f", s.average) + "/20", 15, avgColor, Typeface.BOLD);
        row.addView(avgTv); inner.addView(row);

        FrameLayout barBg = new FrameLayout(this);
        barBg.setBackgroundColor(0xFF002030);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-1, dp(5));
        bLp.setMargins(0, dp(8), 0, 0); barBg.setLayoutParams(bLp);
        View fill = new View(this); fill.setBackgroundColor(avgColor);
        fill.setLayoutParams(new FrameLayout.LayoutParams((int)(frac * 10000), -1));
        barBg.addView(fill); inner.addView(barBg);
        card.addView(inner);
        return card;
    }

    private void showAddSubjectDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setBackgroundColor(BG); form.setPadding(dp(20), dp(12), dp(20), dp(8));
        EditText etName = fld(form, "Nom de la matière", false);
        EditText etCoef = fld(form, "Coefficient (ex: 3)", true);
        EditText etAvg  = fld(form, "Moyenne actuelle /20", true); etAvg.setInputType(android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("Ajouter une matière").setView(form)
            .setPositiveButton("Ajouter", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) return;
                try {
                    int coef = Integer.parseInt(etCoef.getText().toString().trim());
                    float avg = Float.parseFloat(etAvg.getText().toString().trim());
                    db.insertSubject(name, coef, avg);
                    Toast.makeText(this, "✅ Matière ajoutée !", Toast.LENGTH_SHORT).show();
                    recreate();
                } catch (Exception ignored) { Toast.makeText(this, "Valeurs invalides", Toast.LENGTH_SHORT).show(); }
            })
            .setNegativeButton("Annuler", null).show();
    }

    private EditText fld(LinearLayout p, String hint, boolean num) {
        EditText et = new EditText(this); et.setHint(hint); et.setSingleLine();
        et.setTextColor(WHITE); et.setHintTextColor(DIM); et.setBackgroundColor(CARD);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        if (num) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(8)); et.setLayoutParams(lp); p.addView(et); return et;
    }
    private LinearLayout emptyCard(String icon, String title, String sub) {
        LinearLayout ll = new LinearLayout(this); ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER); ll.setBackgroundColor(CARD);
        ll.setPadding(dp(24), dp(36), dp(24), dp(36));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), 0); ll.setLayoutParams(lp);
        TextView i = tv(icon, 40, 0xFF1E3A5F, Typeface.NORMAL); i.setGravity(Gravity.CENTER); ll.addView(i);
        TextView t = tv(title, 14, WHITE, Typeface.BOLD); t.setGravity(Gravity.CENTER); t.setPadding(0,dp(8),0,dp(4)); ll.addView(t);
        TextView s = tv(sub, 12, DIM, Typeface.NORMAL); s.setGravity(Gravity.CENTER); ll.addView(s);
        return ll;
    }
    private LinearLayout miniStat(String icon, String val, String lbl, int color) {
        LinearLayout s = new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER); s.setBackgroundColor(CARD);
        s.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0); s.setLayoutParams(lp);
        TextView i = tv(icon, 18, WHITE, Typeface.NORMAL); i.setGravity(Gravity.CENTER); s.addView(i);
        TextView v = tv(val, 13, color, Typeface.BOLD); v.setGravity(Gravity.CENTER); s.addView(v);
        TextView l = tv(lbl, 9, DIM, Typeface.NORMAL); l.setGravity(Gravity.CENTER); s.addView(l);
        return s;
    }
    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }
    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD); t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
