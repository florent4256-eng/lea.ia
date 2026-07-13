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

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

public class LeaFamilyDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG     = 0xFF020617;
    private static final int CARD   = 0xFF0B1526;
    private static final int CARD2  = 0xFF0F1B2E;
    private static final int CYAN_C = 0xFF00E5FF;
    private static final int PURPLE = 0xFF7C3AED;
    private static final int GREEN_C= 0xFF10B981;
    private static final int ORANGE = 0xFFF59E0B;
    private static final int RED_C  = 0xFFEF4444;
    private static final int WHITE  = 0xFFFFFFFF;
    private static final int DIM    = 0xFF64748B;
    private static final int DIM2   = 0xFF94A3B8;
    private static final int GLASS_BORDER = 0x1EFFFFFF;

    private LeaFamilyDatabase          familyDb;
    private LeaParentalControlManager  parentCtrl;
    private LinearLayout               childListContainer;
    private LinearLayout               heroCard;
    private Button                     sessionBtn;

    @Override protected String getFeatureId() { return LeaPlusDatabase.FAMILY; }

    @Override
    protected void buildContent(LinearLayout container) {
        familyDb   = LeaFamilyDatabase.get(this);
        parentCtrl = LeaParentalControlManager.get(this);
        container.setBackgroundColor(BG);
        container.setPadding(0, 0, 0, 0);

        // ── Hero statut session ────────────────────────────────────────────
        heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER);
        heroCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable heroGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        heroGd.setCornerRadius(dp(20));
        heroGd.setStroke(dp(1), GLASS_BORDER);
        heroCard.setElevation(dp(2));
        heroCard.setBackground(heroGd);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.setMargins(dp(12), dp(8), dp(12), 0);
        heroCard.setLayoutParams(heroLp);
        container.addView(heroCard);
        buildHeroContent();

        // ── Bouton session ─────────────────────────────────────────────────
        sessionBtn = new Button(this);
        applySessionBtn();
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(-1, dp(48));
        sbLp.setMargins(dp(12), dp(8), dp(12), 0);
        sessionBtn.setLayoutParams(sbLp);
        sessionBtn.setAllCaps(false);
        sessionBtn.setOnClickListener(v -> {
            if (parentCtrl.isChildSessionActive()) {
                parentCtrl.endChildSession();
                toast("Session enfant terminée");
                buildHeroContent();
                applySessionBtn();
                refreshChildList();
            } else {
                showStartSessionDialog();
            }
        });
        container.addView(sessionBtn);

        // ── Statistiques rapides ───────────────────────────────────────────
        List<LeaFamilyDatabase.ChildAccount> accounts = familyDb.getChildAccounts();
        int totalToday = 0;
        for (LeaFamilyDatabase.ChildAccount a : accounts) totalToday += familyDb.getScreenUsageToday(a.pseudo);

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.setMargins(dp(12), dp(10), dp(12), 0);
        statsRow.setLayoutParams(srLp);
        statsRow.addView(miniStat("👨‍👩‍👧", String.valueOf(accounts.size()), "Comptes", CYAN_C));
        String sessionVal = parentCtrl.isChildSessionActive() ? parentCtrl.getSessionMinutes() + " min" : "—";
        statsRow.addView(miniStat("⏱️", sessionVal, "Session", GREEN_C));
        statsRow.addView(miniStat("📱", totalToday + " min", "Écran total", ORANGE));
        container.addView(statsRow);

        // ── Section header + bouton ajouter ───────────────────────────────
        LinearLayout secRow = new LinearLayout(this);
        secRow.setOrientation(LinearLayout.HORIZONTAL);
        secRow.setGravity(Gravity.CENTER_VERTICAL);
        secRow.setPadding(dp(16), dp(16), dp(12), dp(8));

        TextView secTv = tv("👧 PROFILS ENFANTS", 11, CYAN_C, Typeface.BOLD);
        secTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        secRow.addView(secTv);

        Button addBtn = new Button(this);
        addBtn.setText("+ Ajouter");
        addBtn.setTextColor(GREEN_C);
        addBtn.setBackground(new RippleDrawable(
            ColorStateList.valueOf(0x5510B981), pillBg(GREEN_C, false), null));
        addBtn.setTextSize(11);
        addBtn.setTypeface(null, Typeface.BOLD);
        addBtn.setAllCaps(false);
        addBtn.setPadding(dp(14), dp(6), dp(14), dp(6));
        addBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(32)));
        addBtn.setOnClickListener(v -> showAddChildDialog());
        secRow.addView(addBtn);
        container.addView(secRow);

        // ── Liste des enfants ──────────────────────────────────────────────
        childListContainer = new LinearLayout(this);
        childListContainer.setOrientation(LinearLayout.VERTICAL);
        childListContainer.setPadding(dp(12), 0, dp(12), dp(32));
        container.addView(childListContainer);
        refreshChildList();
    }

    // ──────────────────────────────────────────────────────────────────────
    // HERO
    // ──────────────────────────────────────────────────────────────────────

    private void buildHeroContent() {
        heroCard.removeAllViews();
        boolean active = parentCtrl.isChildSessionActive();
        String pseudo  = active ? parentCtrl.getActiveChildPseudo() : null;

        TextView iconTv = tv(active ? "👶" : "👨‍👩‍👧", 40, WHITE, Typeface.NORMAL);
        iconTv.setGravity(Gravity.CENTER);
        heroCard.addView(iconTv);

        TextView statusTv = tv(
            active ? "Session active : " + pseudo : "Mode parent",
            15, active ? GREEN_C : DIM2, Typeface.BOLD);
        statusTv.setGravity(Gravity.CENTER);
        statusTv.setPadding(0, dp(6), 0, dp(2));
        heroCard.addView(statusTv);

        String sub = active
            ? parentCtrl.getSessionMinutes() + " min en cours"
            : "Aucune restriction active";
        TextView subTv = tv(sub, 11, DIM, Typeface.NORMAL);
        subTv.setGravity(Gravity.CENTER);
        heroCard.addView(subTv);
    }

    private void applySessionBtn() {
        boolean active = parentCtrl.isChildSessionActive();
        int btnColor = active ? RED_C : GREEN_C;
        sessionBtn.setText(active ? "⏹  Terminer la session enfant" : "▶  Activer une session enfant");
        GradientDrawable sessionGd = new GradientDrawable();
        sessionGd.setColor((btnColor & 0x00FFFFFF) | 0x33000000);
        sessionGd.setCornerRadius(dp(14));
        sessionGd.setStroke(dp(1), btnColor);
        sessionBtn.setBackground(new RippleDrawable(
            ColorStateList.valueOf((btnColor & 0x00FFFFFF) | 0x55000000), sessionGd, null));
        sessionBtn.setElevation(dp(1));
        sessionBtn.setTextColor(WHITE);
        sessionBtn.setTypeface(null, Typeface.BOLD);
        sessionBtn.setTextSize(13);
    }

    // ──────────────────────────────────────────────────────────────────────
    // LISTE ENFANTS
    // ──────────────────────────────────────────────────────────────────────

    private void refreshChildList() {
        childListContainer.removeAllViews();
        List<LeaFamilyDatabase.ChildAccount> accounts = familyDb.getChildAccounts();

        if (accounts.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(36), dp(24), dp(36));
            GradientDrawable emptyGd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ lighten(CARD, 0.06f), CARD });
            emptyGd.setCornerRadius(dp(18));
            emptyGd.setStroke(dp(1), GLASS_BORDER);
            empty.setElevation(dp(2));
            empty.setBackground(emptyGd);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, -2);
            elp.setMargins(0, dp(4), 0, 0);
            empty.setLayoutParams(elp);

            TextView eIcon = tv("👨‍👩‍👧", 48, 0xFF1E3A5F, Typeface.NORMAL);
            eIcon.setGravity(Gravity.CENTER);
            empty.addView(eIcon);

            TextView eTitle = tv("Aucun profil enfant", 14, WHITE, Typeface.BOLD);
            eTitle.setGravity(Gravity.CENTER);
            eTitle.setPadding(0, dp(8), 0, dp(4));
            empty.addView(eTitle);

            TextView eSub = tv("Appuie sur + Ajouter pour créer\nun compte enfant ou ado", 12, DIM, Typeface.NORMAL);
            eSub.setGravity(Gravity.CENTER);
            empty.addView(eSub);

            childListContainer.addView(empty);
            return;
        }
        for (LeaFamilyDatabase.ChildAccount acc : accounts) {
            childListContainer.addView(buildChildCard(acc));
        }
    }

    private View buildChildCard(LeaFamilyDatabase.ChildAccount acc) {
        boolean isActive = acc.pseudo.equals(parentCtrl.getActiveChildPseudo());
        boolean isChild  = "child".equals(acc.accountType);
        int usedMin  = familyDb.getScreenUsageToday(acc.pseudo);
        int limitMin = acc.screenLimitMin;
        float frac   = limitMin > 0 ? Math.min(1f, (float) usedMin / limitMin) : 0;
        int barColor = frac >= 1f ? RED_C : (frac >= 0.75f ? ORANGE : GREEN_C);

        int accentColor = isActive ? GREEN_C : (isChild ? CYAN_C : PURPLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(0), dp(14), dp(12));
        GradientDrawable cardGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        cardGd.setCornerRadius(dp(18));
        cardGd.setStroke(dp(1), accentColor);
        card.setElevation(dp(2));
        card.setBackground(cardGd);
        card.setClipToOutline(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(6), 0, 0);
        card.setLayoutParams(clp);

        // Barre d'accent top
        View accent = new View(this);
        accent.setBackgroundColor(accentColor);
        card.addView(accent, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, 0);

        // ── Ligne avatar + nom ─────────────────────────────────────────────
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout avatarFrame = new FrameLayout(this);
        avatarFrame.setBackgroundColor(isChild ? 0xFF002244 : 0xFF1A0040);
        int avSz = dp(44);
        LinearLayout.LayoutParams afLp = new LinearLayout.LayoutParams(avSz, avSz);
        afLp.setMargins(0, 0, dp(12), 0);
        avatarFrame.setLayoutParams(afLp);
        TextView avatarTv = tv(isChild ? "👦" : "🧒", 22, WHITE, Typeface.NORMAL);
        avatarTv.setGravity(Gravity.CENTER);
        avatarTv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        avatarFrame.addView(avatarTv);
        topRow.addView(avatarFrame);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.addView(tv(acc.pseudo, 15, WHITE, Typeface.BOLD));
        if (isActive) {
            TextView badge = tv(" ACTIF ", 9, GREEN_C, Typeface.BOLD);
            badge.setBackground(pillBg(GREEN_C, false));
            badge.setPadding(dp(4), dp(2), dp(4), dp(2));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-2, -2);
            bLp.setMargins(dp(6), 0, 0, 0);
            badge.setLayoutParams(bLp);
            nameRow.addView(badge);
        }
        nameCol.addView(nameRow);
        nameCol.addView(tv(acc.age + " ans  ·  " + (isChild ? "Enfant" : "Ado") +
            "  ·  Coucher " + acc.bedtimeHour + "h", 11, DIM2, Typeface.NORMAL));
        topRow.addView(nameCol);
        content.addView(topRow);

        // ── Temps écran ────────────────────────────────────────────────────
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(10)));
        content.addView(spacer);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView timeTv = tv("📱  " + usedMin + " / " + limitMin + " min", 11, frac >= 1f ? RED_C : DIM2, Typeface.NORMAL);
        timeTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        timeRow.addView(timeTv);
        timeRow.addView(tv((int)(frac * 100) + "%", 11, barColor, Typeface.BOLD));
        content.addView(timeRow);

        FrameLayout barBg = new FrameLayout(this);
        GradientDrawable barBgGd = new GradientDrawable();
        barBgGd.setColor(0xFF002030);
        barBgGd.setCornerRadius(dp(4));
        barBg.setBackground(barBgGd);
        barBg.setClipToOutline(true);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(5));
        barLp.setMargins(0, dp(4), 0, dp(10));
        barBg.setLayoutParams(barLp);
        View fill = new View(this);
        fill.setBackgroundColor(barColor);
        fill.setLayoutParams(new FrameLayout.LayoutParams((int)(frac * 10000), -1));
        barBg.addView(fill);
        content.addView(barBg);

        // ── Boutons ────────────────────────────────────────────────────────
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setWeightSum(3);

        Button permBtn = tinyBtn("Permissions", CYAN_C);
        permBtn.setOnClickListener(v -> showPermissionsDialog(acc));
        btnRow.addView(permBtn);

        Button editBtn = tinyBtn("Modifier", ORANGE);
        editBtn.setOnClickListener(v -> showEditChildDialog(acc));
        btnRow.addView(editBtn);

        Button delBtn = tinyBtn("Supprimer", RED_C);
        delBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Supprimer " + acc.pseudo + " ?")
            .setMessage("Toutes les données seront effacées.")
            .setPositiveButton("Supprimer", (d, w) -> {
                familyDb.removeChildAccount(acc.pseudo);
                if (acc.pseudo.equals(parentCtrl.getActiveChildPseudo())) {
                    parentCtrl.endChildSession();
                    buildHeroContent();
                    applySessionBtn();
                }
                refreshChildList();
            })
            .setNegativeButton("Annuler", null).show());
        btnRow.addView(delBtn);
        content.addView(btnRow);
        card.addView(content);
        return card;
    }

    // ──────────────────────────────────────────────────────────────────────
    // DIALOGUES
    // ──────────────────────────────────────────────────────────────────────

    private void showStartSessionDialog() {
        List<LeaFamilyDatabase.ChildAccount> accounts = familyDb.getChildAccounts();
        if (accounts.isEmpty()) { toast("Aucun profil enfant. Crées-en un d'abord."); return; }
        String[] names = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) names[i] = accounts.get(i).pseudo;
        new AlertDialog.Builder(this).setTitle("Sélectionner le profil")
            .setItems(names, (d, idx) -> {
                LeaFamilyDatabase.ChildAccount acc = accounts.get(idx);
                LeaFamilyDatabase.ChildAccount full = familyDb.getChildAccount(acc.pseudo);
                if (full != null && parentCtrl.isBedtimeLocked(full)) {
                    toast("Couvre-feu actif pour " + acc.pseudo); return;
                }
                parentCtrl.startChildSession(acc.pseudo);
                toast("Session démarrée : " + acc.pseudo);
                buildHeroContent(); applySessionBtn(); refreshChildList();
            })
            .setNegativeButton("Annuler", null).show();
    }

    private void showAddChildDialog() {
        LinearLayout form = form();
        EditText pseudoEt = field(form, "Pseudo (ex: Luca, Camille…)", false);
        EditText ageEt    = field(form, "Âge", true);
        Spinner typeSpin  = new Spinner(this);
        typeSpin.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"Enfant (< 13 ans)", "Ado (13–17 ans)"}));
        form.addView(lbl("Type de compte")); form.addView(typeSpin);
        EditText limitEt = field(form, "Limite écran (min/jour)", true); limitEt.setText("120");
        EditText bedEt   = field(form, "Heure de coucher (0–23)", true);  bedEt.setText("21");
        EditText wakeEt  = field(form, "Heure de réveil (0–23)", true);   wakeEt.setText("7");
        new AlertDialog.Builder(this).setTitle("Nouveau profil enfant").setView(form)
            .setPositiveButton("Créer", (d, w) -> {
                String pseudo = pseudoEt.getText().toString().trim();
                if (pseudo.isEmpty()) { toast("Pseudo requis"); return; }
                if (familyDb.childExists(pseudo)) { toast("Pseudo déjà utilisé"); return; }
                familyDb.addChildAccount(pseudo, pi(ageEt, 10),
                    typeSpin.getSelectedItemPosition() == 0 ? "child" : "teen",
                    pi(limitEt, 120), pi(bedEt, 21), pi(wakeEt, 7));
                toast("Profil créé : " + pseudo);
                refreshChildList();
            })
            .setNegativeButton("Annuler", null).show();
    }

    private void showEditChildDialog(LeaFamilyDatabase.ChildAccount acc) {
        LinearLayout form = form();
        EditText limitEt = field(form, "Limite écran (min/jour)", true);
        limitEt.setText(String.valueOf(acc.screenLimitMin));
        EditText bedEt = field(form, "Heure de coucher", true); bedEt.setText(String.valueOf(acc.bedtimeHour));
        EditText wakeEt = field(form, "Heure de réveil", true);  wakeEt.setText(String.valueOf(acc.wakeupHour));
        new AlertDialog.Builder(this).setTitle("Modifier : " + acc.pseudo).setView(form)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                familyDb.updateScreenLimit(acc.pseudo, pi(limitEt, acc.screenLimitMin));
                familyDb.updateBedtime(acc.pseudo, pi(bedEt, acc.bedtimeHour), pi(wakeEt, acc.wakeupHour));
                refreshChildList();
            })
            .setNegativeButton("Annuler", null).show();
    }

    private void showPermissionsDialog(LeaFamilyDatabase.ChildAccount acc) {
        String[] features = { LeaFamilyDatabase.F_QUESTS, LeaFamilyDatabase.F_ADVENTURE,
            LeaFamilyDatabase.F_HABITS, LeaFamilyDatabase.F_COMPANION, LeaFamilyDatabase.F_LIFE_OS,
            LeaFamilyDatabase.F_LANGUAGE, LeaFamilyDatabase.F_STUDENT, LeaFamilyDatabase.F_REPORT,
            LeaFamilyDatabase.F_SMART, LeaFamilyDatabase.F_MARKET, LeaFamilyDatabase.F_CLOUD,
            LeaFamilyDatabase.F_OMNI, LeaFamilyDatabase.F_STREAM, LeaFamilyDatabase.F_COINS };
        String[] labels = { "Quêtes", "Aventure", "Habitudes", "Compagnon", "Life OS",
            "Langues", "Étudiant", "Rapport", "Notifs Smart", "Marketplace",
            "Cloud Sync", "Omnicanal", "Streaming", "Coins" };
        boolean[] checked = new boolean[features.length];
        for (int i = 0; i < features.length; i++) checked[i] = familyDb.isFeatureAllowed(acc.pseudo, features[i]);
        new AlertDialog.Builder(this).setTitle("Permissions — " + acc.pseudo)
            .setMultiChoiceItems(labels, checked, (d, idx, v) -> checked[idx] = v)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                for (int i = 0; i < features.length; i++) familyDb.setFeatureAccess(acc.pseudo, features[i], checked[i]);
                toast("Permissions mises à jour");
            })
            .setNegativeButton("Annuler", null).show();
    }

    // ──────────────────────────────────────────────────────────────────────
    // HELPERS UI
    // ──────────────────────────────────────────────────────────────────────

    private TextView tv(String text, float sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(color); t.setTextSize(sp); t.setTypeface(null, style);
        return t;
    }

    private LinearLayout miniStat(String icon, String value, String label, int color) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER);
        GradientDrawable statGd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        statGd.setCornerRadius(dp(14));
        statGd.setStroke(dp(1), GLASS_BORDER);
        s.setElevation(dp(2));
        s.setBackground(statGd);
        s.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        s.setLayoutParams(lp);
        TextView iconTv = tv(icon, 18, WHITE, Typeface.NORMAL);
        iconTv.setGravity(Gravity.CENTER); s.addView(iconTv);
        TextView valTv = tv(value, 14, color, Typeface.BOLD);
        valTv.setGravity(Gravity.CENTER); s.addView(valTv);
        TextView lblTv = tv(label, 9, DIM, Typeface.NORMAL);
        lblTv.setGravity(Gravity.CENTER); s.addView(lblTv);
        return s;
    }

    private Button tinyBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text); b.setTextColor(color);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor((color & 0x00FFFFFF) | 0x1A000000);
        gd.setCornerRadius(dp(10));
        gd.setStroke(dp(1), (color & 0x00FFFFFF) | 0x66000000);
        b.setBackground(new RippleDrawable(
            ColorStateList.valueOf((color & 0x00FFFFFF) | 0x55000000), gd, null));
        b.setTextSize(10); b.setTypeface(null, Typeface.BOLD); b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1f);
        lp.setMargins(dp(2), dp(6), dp(2), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout form() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setBackgroundColor(BG);
        f.setPadding(dp(20), dp(12), dp(20), dp(8));
        return f;
    }

    private EditText field(LinearLayout parent, String hint, boolean numeric) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setTextColor(WHITE); et.setHintTextColor(DIM);
        et.setBackgroundColor(CARD); et.setPadding(dp(10), dp(8), dp(10), dp(8));
        if (numeric) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(8));
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }

    private TextView lbl(String text) {
        TextView t = tv(text, 11, DIM2, Typeface.NORMAL);
        t.setPadding(0, dp(6), 0, dp(2));
        return t;
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
    private int pi(EditText et, int def) {
        try { return Integer.parseInt(et.getText().toString().trim()); } catch (Exception e) { return def; }
    }
}
