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


import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.util.List;

/**
 * Activité de gestion des routines automatisées de Léa.
 * Affiche la liste des routines, permet d'en créer / activer / supprimer.
 */
public class LeaRoutinesActivity extends Activity {

    private LinearLayout        routineList;
    private LeaAutomationManager mgr;

    private static final int BG    = 0xFF011627;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int CARD  = 0xFF012040;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        mgr = new LeaAutomationManager(this);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(80));

        // Titre
        TextView title = new TextView(this);
        title.setText("Routines Léa");
        title.setTextColor(CYAN);
        title.setTextSize(22f);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(20));
        root.addView(title);

        // Bouton "+" créer
        Button btnAdd = makeButton("+ Nouvelle routine", CYAN, BG);
        btnAdd.setOnClickListener(v -> showCreateDialog());
        root.addView(btnAdd, makeLp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        // Spacer
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16)));

        // Liste des routines
        routineList = new LinearLayout(this);
        routineList.setOrientation(LinearLayout.VERTICAL);
        root.addView(routineList);

        scroll.addView(root);
        setContentView(scroll);

        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        routineList.removeAllViews();
        List<LeaNovaDataStore.Routine> routines = mgr.getAllRoutines();
        if (routines.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucune routine. Crée-en une !");
            empty.setTextColor(0xFF607D8B);
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            routineList.addView(empty);
            return;
        }
        for (LeaNovaDataStore.Routine r : routines) {
            routineList.addView(buildRoutineCard(r));
        }
    }

    private View buildRoutineCard(LeaNovaDataStore.Routine r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(12));
        bg.setStroke(r.active ? 2 : 1, r.active ? CYAN : 0xFF37474F);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        // Ligne 1 : nom + toggle actif
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(this);
        nameView.setText(r.name);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(16f);
        nameView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row1.addView(nameView, nameLp);

        CheckBox toggle = new CheckBox(this);
        toggle.setChecked(r.active);
        toggle.setText(r.active ? "Actif" : "Inactif");
        toggle.setTextColor(r.active ? CYAN : 0xFF607D8B);
        toggle.setOnCheckedChangeListener((cb, checked) -> {
            mgr.toggleRoutine(r.id, checked);
            cb.setText(checked ? "Actif" : "Inactif");
            cb.setTextColor(checked ? CYAN : 0xFF607D8B);
            refreshList();
        });
        row1.addView(toggle);
        card.addView(row1);

        // Ligne 2 : heure + jours
        String actionsPreview = "";
        try {
            JSONArray arr = new JSONArray(r.actionsJson != null ? r.actionsJson : "[]");
            actionsPreview = arr.length() > 0 ? arr.getString(0) : "";
            if (arr.length() > 1) actionsPreview += " +" + (arr.length() - 1) + " action(s)";
        } catch (Exception ignored) {}

        TextView infoView = new TextView(this);
        infoView.setText("⏰ " + (r.triggerTime != null ? r.triggerTime : "?")
            + "  |  " + r.daysLabel()
            + "\n" + actionsPreview);
        infoView.setTextColor(0xFFB0BEC5);
        infoView.setTextSize(13f);
        infoView.setPadding(0, dp(6), 0, dp(6));
        card.addView(infoView);

        // Bouton supprimer
        Button btnDel = makeButton("Supprimer", 0xFFE57373, BG);
        btnDel.setTextSize(12f);
        btnDel.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Supprimer \"" + r.name + "\" ?")
            .setPositiveButton("Oui", (d, w) -> { mgr.deleteRoutine(r.id); refreshList(); })
            .setNegativeButton("Annuler", null)
            .show());
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(120), dp(34));
        delLp.gravity = Gravity.END;
        card.addView(btnDel, delLp);

        return card;
    }

    private void showCreateDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(10), dp(20), dp(10));

        EditText etName    = makeEditText("Nom de la routine", form);
        EditText etTime    = makeEditText("Heure déclencheur (ex: 08:00)", form);
        EditText etDays    = makeEditText("Jours (1=lun … 7=dim, ex: 1,2,3,4,5)", form);
        EditText etActions = makeEditText("Actions (séparées par ;)", form);

        new AlertDialog.Builder(this)
            .setTitle("Nouvelle routine")
            .setView(form)
            .setPositiveButton("Créer", (d, w) -> {
                String name    = etName.getText().toString().trim();
                String time    = etTime.getText().toString().trim();
                String days    = etDays.getText().toString().trim();
                String actRaw  = etActions.getText().toString().trim();
                if (name.isEmpty() || time.isEmpty()) {
                    Toast.makeText(this, "Nom et heure requis.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] actions = actRaw.isEmpty() ? new String[0] : actRaw.split(";");
                long id = mgr.createRoutine(name, time, days.isEmpty() ? "1,2,3,4,5" : days, actions);
                Toast.makeText(this, id >= 0 ? "Routine créée !" : "Erreur.", Toast.LENGTH_SHORT).show();
                refreshList();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // --- Helpers UI ---

    private EditText makeEditText(String hint, LinearLayout parent) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(0xFF607D8B);
        et.setBackgroundColor(0xFF012030);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        parent.addView(et, lp);
        return et;
    }

    private Button makeButton(String text, int textColor, int bgColor) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(textColor);
        b.setBackgroundColor(bgColor);
        GradientDrawable bd = new GradientDrawable();
        bd.setColor(bgColor);
        bd.setCornerRadius(dp(8));
        bd.setStroke(1, textColor);
        b.setBackground(bd);
        return b;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams makeLp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }
}
