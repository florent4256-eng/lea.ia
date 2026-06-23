package com.flolov42.lea_v3.ui;

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
import com.flolov42.lea_v3.code.*;
import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;
import com.flolov42.lea_v3.utilities.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.List;

public class LeaAdminPanel extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int GOLD = 0xFFFFD700;
    private static final int GRN  = 0xFF4CAF50;
    private static final int RED  = 0xFFF44336;
    private static final int DIM  = 0xFF7BB8CC;

    private LeaPlusDatabase  plusDb;
    private LeaAdminDatabase adminDb;

    private String currentTab = "QUETES";
    private final java.util.Map<String, Button>     tabBtns = new java.util.HashMap<>();
    private final java.util.Map<String, ScrollView> tabViews = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);

        plusDb  = LeaPlusDatabase.get(this);
        adminDb = LeaAdminDatabase.get(this);

        // PIN guard
        showPinDialog(() -> buildUI());
    }

    private void showPinDialog(Runnable onSuccess) {
        EditText et = new EditText(this);
        et.setHint("PIN admin");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this)
            .setTitle("🔐 ACCÈS ADMIN LÉA")
            .setMessage("Entrez votre code PIN admin :")
            .setView(et)
            .setPositiveButton("Entrer", (d, w) -> {
                String pin = et.getText().toString().trim();
                if (adminDb.checkPin(pin)) {
                    onSuccess.run();
                } else {
                    Toast.makeText(this, "❌ PIN incorrect", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .setNegativeButton("Annuler", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }

    private void buildUI() {
        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setBackgroundColor(BG);
        rootScroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(0, 0, 0, dp(16));
        rootScroll.addView(root);

        root.addView(buildHeader());
        root.addView(buildTabBar());
        root.addView(buildAllTabs());

        setContentView(rootScroll);
        showTab(currentTab);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private LinearLayout buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(CARD);
        h.setPadding(dp(12), dp(14), dp(16), dp(14));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setTextSize(20);
        back.setOnClickListener(v -> finish());
        h.addView(back);

        TextView title = new TextView(this);
        title.setText("⚙️ PANEL ADMIN LÉA");
        title.setTextColor(GOLD);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(lp);
        h.addView(title);

        Button pinBtn = new Button(this);
        pinBtn.setText("🔑");
        pinBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        pinBtn.setTextColor(DIM);
        pinBtn.setOnClickListener(v -> showChangePinDialog());
        h.addView(pinBtn);

        return h;
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────
    private HorizontalScrollView buildTabBar() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setBackgroundColor(0xFF010E1A);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));

        String[][] tabs = {{"QUETES","⚔️ QUÊTES"},{"SKILLS","🧩 SKILLS"},{"LANGUES","🌐 LANGUES"},{"MONDES","🌍 MONDES"}};
        for (String[] tab : tabs) {
            Button btn = new Button(this);
            btn.setText(tab[1]);
            btn.setTextSize(10);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setAllCaps(false);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
            blp.setMargins(dp(4), 0, dp(4), 0);
            btn.setLayoutParams(blp);
            final String tabId = tab[0];
            btn.setOnClickListener(v -> showTab(tabId));
            tabBtns.put(tabId, btn);
            bar.addView(btn);
        }
        hsv.addView(bar);
        return hsv;
    }

    private void showTab(String tab) {
        currentTab = tab;
        for (java.util.Map.Entry<String, ScrollView> e : tabViews.entrySet())
            e.getValue().setVisibility(tab.equals(e.getKey()) ? View.VISIBLE : View.GONE);
        for (java.util.Map.Entry<String, Button> e : tabBtns.entrySet()) {
            boolean active = tab.equals(e.getKey());
            e.getValue().setBackgroundColor(active ? GOLD : 0xFF022040);
            e.getValue().setTextColor(active ? BG : CYAN);
        }
    }

    private FrameLayout buildAllTabs() {
        FrameLayout frame = new FrameLayout(this);
        for (String tab : new String[]{"QUETES","SKILLS","LANGUES","MONDES"}) {
            ScrollView sv = buildTab(tab);
            tabViews.put(tab, sv);
            frame.addView(sv);
        }
        return frame;
    }

    private ScrollView buildTab(String tab) {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout vl = new LinearLayout(this);
        vl.setOrientation(LinearLayout.VERTICAL);
        vl.setPadding(dp(12), dp(12), dp(12), dp(80));
        vl.setBackgroundColor(BG);
        switch (tab) {
            case "QUETES":  buildQuestTab(vl);    break;
            case "SKILLS":  buildSkillTab(vl);    break;
            case "LANGUES": buildLangTab(vl);     break;
            case "MONDES":  buildWorldTab(vl);    break;
        }
        sv.addView(vl);
        return sv;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB: QUÊTES
    // ═══════════════════════════════════════════════════════════════════════════
    private void buildQuestTab(LinearLayout parent) {
        parent.addView(sectionTitle("➕ CRÉER UNE QUÊTE"));
        parent.addView(buildQuestForm(null));

        parent.addView(sectionTitle("📋 QUÊTES EXISTANTES (" + plusDb.getAllQuestsAdmin().size() + ")"));
        for (LeaPlusDatabase.QuestRow q : plusDb.getAllQuestsAdmin()) {
            parent.addView(buildQuestCard(q, parent));
        }
    }

    private LinearLayout buildQuestForm(LeaPlusDatabase.QuestRow edit) {
        LinearLayout card = makeCard();
        String[] fields = {"Nom de la quête","Description","XP reward","Coin reward","Catégorie (aventure/social/dev...)","Difficulté (easy/medium/hard)","Cible (nb tâches)"};
        int[] inputTypes = {0,0,InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_NUMBER,0,0,InputType.TYPE_CLASS_NUMBER};
        EditText[] ets = new EditText[fields.length];

        for (int i = 0; i < fields.length; i++) {
            EditText et = new EditText(this);
            et.setHint(fields[i]);
            et.setTextColor(0xFFFFFFFF);
            et.setHintTextColor(DIM);
            et.setBackgroundColor(0xFF021830);
            et.setPadding(dp(8), dp(8), dp(8), dp(8));
            if (inputTypes[i] != 0) et.setInputType(inputTypes[i]);
            LinearLayout.LayoutParams etlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            etlp.setMargins(0, dp(4), 0, 0);
            et.setLayoutParams(etlp);
            if (edit != null) {
                switch (i) {
                    case 0: et.setText(edit.title); break;
                    case 1: et.setText(edit.desc); break;
                    case 2: et.setText(String.valueOf(edit.xp)); break;
                    case 3: et.setText(String.valueOf(edit.coins)); break;
                    case 4: et.setText(edit.category); break;
                    case 5: et.setText(edit.difficulty); break;
                    case 6: et.setText(String.valueOf(edit.target)); break;
                }
            }
            ets[i] = et;
            card.addView(et);
        }

        Button saveBtn = new Button(this);
        saveBtn.setText(edit == null ? "💾 CRÉER LA QUÊTE" : "✏️ METTRE À JOUR");
        saveBtn.setTextColor(BG);
        saveBtn.setBackgroundColor(edit == null ? CYAN : GOLD);
        saveBtn.setAllCaps(false);
        saveBtn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        slp.setMargins(0, dp(10), 0, 0);
        saveBtn.setLayoutParams(slp);
        saveBtn.setOnClickListener(v -> {
            String name = ets[0].getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this,"Nom obligatoire",Toast.LENGTH_SHORT).show(); return; }
            int xp = parseIntSafe(ets[2].getText().toString(), 50);
            int coins = parseIntSafe(ets[3].getText().toString(), 10);
            int target = parseIntSafe(ets[6].getText().toString(), 1);
            String cat  = orDefault(ets[4].getText().toString(), "aventure");
            String diff = orDefault(ets[5].getText().toString(), "medium");
            String desc = ets[1].getText().toString();
            if (edit == null) {
                String id = "quest_" + System.currentTimeMillis();
                plusDb.insertQuest(id, name, desc, cat, diff, xp, coins, target);
                adminDb.logAction("CREATE_QUEST", name);
                Toast.makeText(this,"✅ Quête créée !",Toast.LENGTH_SHORT).show();
            } else {
                plusDb.updateQuestAdmin(edit.id, name, desc, cat, diff, xp, coins, target);
                adminDb.logAction("UPDATE_QUEST", name);
                Toast.makeText(this,"✅ Quête mise à jour !",Toast.LENGTH_SHORT).show();
            }
            recreate();
        });
        card.addView(saveBtn);
        return card;
    }

    private LinearLayout buildQuestCard(LeaPlusDatabase.QuestRow q, LinearLayout parent) {
        LinearLayout card = makeCard();
        LinearLayout topRow = makeRow();

        TextView nameTv = new TextView(this);
        nameTv.setText(q.title);
        nameTv.setTextColor(0xFFFFFFFF);
        nameTv.setTextSize(13);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(nameTv);

        String statusColor = "available".equals(q.status) ? "🟢" : ("completed".equals(q.status) ? "🏆" : "🔵");
        TextView statusTv = new TextView(this);
        statusTv.setText(statusColor + " " + q.status);
        statusTv.setTextColor(DIM);
        statusTv.setTextSize(10);
        topRow.addView(statusTv);
        card.addView(topRow);

        card.addView(infoRow("XP", q.xp + " · Coins: " + q.coins + " · Diff: " + q.difficulty));
        card.addView(infoRow("Catégorie", q.category + " · Cible: " + q.target));

        LinearLayout btnRow = makeRow();

        Button editBtn = smallBtn("✏️ Éditer", CYAN);
        editBtn.setOnClickListener(v -> showQuestEditDialog(q));
        btnRow.addView(editBtn);

        Button delBtn = smallBtn("🗑️ Supprimer", RED);
        delBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Supprimer ?")
                .setMessage("Supprimer la quête \"" + q.title + "\" ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    plusDb.deleteQuest(q.id);
                    adminDb.logAction("DELETE_QUEST", q.title);
                    Toast.makeText(this,"🗑️ Quête supprimée",Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .setNegativeButton("Annuler", null).show();
        });
        btnRow.addView(delBtn);
        card.addView(btnRow);
        return card;
    }

    private void showQuestEditDialog(LeaPlusDatabase.QuestRow q) {
        // Rebuild the form in a dialog for editing
        LinearLayout layout = buildQuestForm(q);
        layout.setPadding(dp(8), dp(4), dp(8), dp(4));
        new AlertDialog.Builder(this)
            .setTitle("✏️ Éditer : " + q.title)
            .setView(layout)
            .setNegativeButton("Fermer", null)
            .show();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB: SKILLS
    // ═══════════════════════════════════════════════════════════════════════════
    private void buildSkillTab(LinearLayout parent) {
        parent.addView(sectionTitle("➕ AJOUTER UN SKILL AU MARKETPLACE"));
        parent.addView(buildSkillForm());

        List<LeaPlusDatabase.SkillRow> skills = plusDb.getMarketplaceSkills();
        parent.addView(sectionTitle("🧩 SKILLS MARKETPLACE (" + skills.size() + ")"));
        for (LeaPlusDatabase.SkillRow sk : skills) {
            parent.addView(buildSkillCard(sk));
        }
    }

    private LinearLayout buildSkillForm() {
        LinearLayout card = makeCard();
        String[] hints = {"Nom du skill","Description","Auteur","Catégorie","Prix (coins)"};
        int[]    types  = {0,0,0,0,InputType.TYPE_CLASS_NUMBER};
        EditText[] ets = new EditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            EditText et = new EditText(this);
            et.setHint(hints[i]); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(DIM);
            et.setBackgroundColor(0xFF021830); et.setPadding(dp(8),dp(8),dp(8),dp(8));
            if (types[i] != 0) et.setInputType(types[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, 0);
            et.setLayoutParams(lp);
            ets[i] = et; card.addView(et);
        }
        Button saveBtn = makeSaveBtn("🧩 AJOUTER AU MARKETPLACE", CYAN);
        saveBtn.setOnClickListener(v -> {
            String name = ets[0].getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this,"Nom obligatoire",Toast.LENGTH_SHORT).show(); return; }
            String id    = "skill_" + System.currentTimeMillis();
            String desc  = ets[1].getText().toString();
            String author = orDefault(ets[2].getText().toString(), "Admin Léa");
            String cat   = orDefault(ets[3].getText().toString(), "productivity");
            int price    = parseIntSafe(ets[4].getText().toString(), 50);
            plusDb.addSkillToMarketplace(id, name, desc, author, cat, price);
            adminDb.logAction("CREATE_SKILL", name);
            Toast.makeText(this,"✅ Skill ajouté au marketplace !",Toast.LENGTH_SHORT).show();
            recreate();
        });
        card.addView(saveBtn);
        return card;
    }

    private LinearLayout buildSkillCard(LeaPlusDatabase.SkillRow sk) {
        LinearLayout card = makeCard();
        LinearLayout topRow = makeRow();
        TextView nameTv = new TextView(this);
        nameTv.setText("🧩 " + sk.name);
        nameTv.setTextColor(0xFFFFFFFF); nameTv.setTextSize(13); nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(nameTv);
        TextView priceTv = new TextView(this);
        priceTv.setText(sk.priceCoins + "🪙");
        priceTv.setTextColor(GOLD); priceTv.setTextSize(12);
        topRow.addView(priceTv);
        card.addView(topRow);
        card.addView(infoRow("Auteur", sk.author));
        card.addView(infoRow("Statut", sk.installed == 1 ? "✅ Installé" : "🛒 En vente"));

        Button delBtn = smallBtn("🗑️ Retirer du marketplace", RED);
        delBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Retirer ?")
            .setMessage("Retirer \"" + sk.name + "\" du marketplace ?")
            .setPositiveButton("Retirer", (d, w) -> {
                plusDb.removeMarketplaceSkill(sk.id);
                adminDb.logAction("DELETE_SKILL", sk.name);
                Toast.makeText(this,"🗑️ Skill retiré",Toast.LENGTH_SHORT).show();
                recreate();
            })
            .setNegativeButton("Annuler", null).show());
        card.addView(delBtn);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB: LANGUES
    // ═══════════════════════════════════════════════════════════════════════════
    private String[] activeLangRef = {"EN"};

    private void buildLangTab(LinearLayout parent) {
        // Sélecteur langue
        parent.addView(sectionTitle("🌐 LANGUE"));
        LinearLayout langRow = makeRow();
        for (String lang : new String[]{"EN","FR","ES","DE","PT"}) {
            Button lb = new Button(this);
            lb.setText(lang); lb.setAllCaps(false); lb.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            blp.setMargins(dp(2),0,dp(2),0); lb.setLayoutParams(blp);
            lb.setBackgroundColor(lang.equals(activeLangRef[0]) ? CYAN : 0xFF023050);
            lb.setTextColor(lang.equals(activeLangRef[0]) ? BG : CYAN);
            lb.setTextSize(10);
            lb.setOnClickListener(v -> {
                activeLangRef[0] = lang;
                parent.removeAllViews();
                buildLangTab(parent);
            });
            langRow.addView(lb);
        }
        parent.addView(langRow);

        parent.addView(sectionTitle("➕ AJOUTER UN MOT EN " + activeLangRef[0]));
        parent.addView(buildVocabForm());

        List<LeaPlusDatabase.VocabAdminRow> vocab = plusDb.getAllVocabAdmin(activeLangRef[0]);
        parent.addView(sectionTitle("📚 VOCABULAIRE " + activeLangRef[0] + " (" + vocab.size() + " mots)"));
        for (LeaPlusDatabase.VocabAdminRow vr : vocab) {
            parent.addView(buildVocabCard(vr));
        }
    }

    private LinearLayout buildVocabForm() {
        LinearLayout card = makeCard();
        String[] hints = {"Mot","Traduction","Prononciation (optionnel)","Exemple de phrase (optionnel)"};
        EditText[] ets = new EditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            EditText et = new EditText(this);
            et.setHint(hints[i]); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(DIM);
            et.setBackgroundColor(0xFF021830); et.setPadding(dp(8),dp(8),dp(8),dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, 0); et.setLayoutParams(lp);
            ets[i] = et; card.addView(et);
        }
        Button saveBtn = makeSaveBtn("💾 AJOUTER LE MOT", GRN);
        saveBtn.setOnClickListener(v -> {
            String word = ets[0].getText().toString().trim();
            String trans = ets[1].getText().toString().trim();
            if (word.isEmpty() || trans.isEmpty()) { Toast.makeText(this,"Mot et traduction requis",Toast.LENGTH_SHORT).show(); return; }
            plusDb.insertVocab(activeLangRef[0], word, trans, ets[2].getText().toString(), ets[3].getText().toString());
            adminDb.logAction("ADD_VOCAB_" + activeLangRef[0], word);
            Toast.makeText(this,"✅ Mot ajouté !",Toast.LENGTH_SHORT).show();
            recreate();
        });
        card.addView(saveBtn);
        return card;
    }

    private LinearLayout buildVocabCard(LeaPlusDatabase.VocabAdminRow vr) {
        LinearLayout card = makeCard();
        LinearLayout topRow = makeRow();
        TextView wordTv = new TextView(this);
        wordTv.setText(vr.word);
        wordTv.setTextColor(CYAN); wordTv.setTextSize(14); wordTv.setTypeface(null, Typeface.BOLD);
        wordTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(wordTv);
        TextView transTv = new TextView(this);
        transTv.setText("→ " + vr.translation);
        transTv.setTextColor(DIM); transTv.setTextSize(12);
        topRow.addView(transTv);
        card.addView(topRow);
        if (vr.phonetic != null && !vr.phonetic.isEmpty())
            card.addView(infoRow("Prononciation", vr.phonetic));
        if (vr.example != null && !vr.example.isEmpty())
            card.addView(infoRow("Exemple", vr.example));

        Button delBtn = smallBtn("🗑️", RED);
        delBtn.setOnClickListener(v -> {
            plusDb.deleteVocab(vr.id);
            adminDb.logAction("DELETE_VOCAB", vr.word);
            Toast.makeText(this,"🗑️ Mot supprimé",Toast.LENGTH_SHORT).show();
            recreate();
        });
        card.addView(delBtn);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB: MONDES
    // ═══════════════════════════════════════════════════════════════════════════
    private void buildWorldTab(LinearLayout parent) {
        parent.addView(sectionTitle("➕ CRÉER UN MONDE"));
        parent.addView(buildWorldForm(null));

        List<LeaAdminDatabase.WorldRow> worlds = adminDb.getWorlds();
        parent.addView(sectionTitle("🌍 MONDES (" + worlds.size() + ")"));
        for (LeaAdminDatabase.WorldRow w : worlds) {
            parent.addView(buildWorldCard(w));
        }
    }

    private LinearLayout buildWorldForm(LeaAdminDatabase.WorldRow edit) {
        LinearLayout card = makeCard();
        String[] hints = {"Nom du monde","Nom du Boss","HP du Boss","Dégâts du Boss","XP reward","Niveau requis"};
        int[]    types  = {0,0,InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_NUMBER};
        EditText[] ets = new EditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            EditText et = new EditText(this);
            et.setHint(hints[i]); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(DIM);
            et.setBackgroundColor(0xFF021830); et.setPadding(dp(8),dp(8),dp(8),dp(8));
            if (types[i] != 0) et.setInputType(types[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, 0); et.setLayoutParams(lp);
            if (edit != null) {
                switch (i) {
                    case 0: et.setText(edit.name); break; case 1: et.setText(edit.bossName); break;
                    case 2: et.setText(String.valueOf(edit.bossHp)); break; case 3: et.setText(String.valueOf(edit.bossDmg)); break;
                    case 4: et.setText(String.valueOf(edit.xpReward)); break; case 5: et.setText(String.valueOf(edit.levelRequired)); break;
                }
            }
            ets[i] = et; card.addView(et);
        }
        Button saveBtn = makeSaveBtn(edit == null ? "🌍 CRÉER LE MONDE" : "✏️ METTRE À JOUR", edit == null ? CYAN : GOLD);
        saveBtn.setOnClickListener(v -> {
            String name = ets[0].getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this,"Nom obligatoire",Toast.LENGTH_SHORT).show(); return; }
            String boss = orDefault(ets[1].getText().toString(), "Boss Inconnu");
            int hp  = parseIntSafe(ets[2].getText().toString(), 500);
            int dmg = parseIntSafe(ets[3].getText().toString(), 20);
            int xp  = parseIntSafe(ets[4].getText().toString(), 200);
            int lvl = parseIntSafe(ets[5].getText().toString(), 1);
            if (edit == null) {
                adminDb.insertWorld(name, boss, hp, dmg, xp, lvl);
                Toast.makeText(this,"✅ Monde créé !",Toast.LENGTH_SHORT).show();
            } else {
                adminDb.updateWorld(edit.id, name, boss, hp, dmg, xp, lvl);
                Toast.makeText(this,"✅ Monde mis à jour !",Toast.LENGTH_SHORT).show();
            }
            recreate();
        });
        card.addView(saveBtn);
        return card;
    }

    private LinearLayout buildWorldCard(LeaAdminDatabase.WorldRow w) {
        LinearLayout card = makeCard();
        TextView nameTv = new TextView(this);
        nameTv.setText("🌍 " + w.name);
        nameTv.setTextColor(GOLD); nameTv.setTextSize(15); nameTv.setTypeface(null, Typeface.BOLD);
        card.addView(nameTv);
        card.addView(infoRow("Boss", w.bossName + " · HP: " + w.bossHp + " · DMG: " + w.bossDmg));
        card.addView(infoRow("XP reward", String.valueOf(w.xpReward) + " · Niveau requis: " + w.levelRequired));

        LinearLayout btnRow = makeRow();
        Button editBtn = smallBtn("✏️ Éditer", CYAN);
        editBtn.setOnClickListener(v -> {
            LinearLayout form = buildWorldForm(w);
            new AlertDialog.Builder(this).setTitle("✏️ Éditer monde").setView(form)
                .setNegativeButton("Fermer", null).show();
        });
        btnRow.addView(editBtn);

        Button delBtn = smallBtn("🗑️ Supprimer", RED);
        delBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Supprimer ?")
            .setMessage("Supprimer le monde \"" + w.name + "\" ?")
            .setPositiveButton("Supprimer", (d, ww) -> {
                adminDb.deleteWorld(w.id);
                Toast.makeText(this,"🗑️ Monde supprimé",Toast.LENGTH_SHORT).show();
                recreate();
            })
            .setNegativeButton("Annuler", null).show());
        btnRow.addView(delBtn);
        card.addView(btnRow);
        return card;
    }

    // ── Changer PIN ───────────────────────────────────────────────────────────
    private void showChangePinDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));
        EditText etOld = new EditText(this); etOld.setHint("PIN actuel"); etOld.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        EditText etNew = new EditText(this); etNew.setHint("Nouveau PIN"); etNew.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(etOld); layout.addView(etNew);
        new AlertDialog.Builder(this)
            .setTitle("🔑 Changer PIN Admin")
            .setView(layout)
            .setPositiveButton("Changer", (d, w) -> {
                if (adminDb.checkPin(etOld.getText().toString().trim())) {
                    String newPin = etNew.getText().toString().trim();
                    if (newPin.length() >= 4) {
                        adminDb.setAdminPin(newPin);
                        Toast.makeText(this,"✅ PIN mis à jour !",Toast.LENGTH_SHORT).show();
                    } else { Toast.makeText(this,"PIN trop court (min 4 chiffres)",Toast.LENGTH_SHORT).show(); }
                } else { Toast.makeText(this,"❌ PIN actuel incorrect",Toast.LENGTH_SHORT).show(); }
            })
            .setNegativeButton("Annuler", null).show();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8));
        card.setLayoutParams(lp);
        return card;
    }
    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }
    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(GOLD);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(14), 0, dp(4));
        tv.setLayoutParams(lp);
        return tv;
    }
    private TextView infoRow(String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + ": " + value);
        tv.setTextColor(DIM); tv.setTextSize(11);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(2), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }
    private Button smallBtn(String label, int color) {
        Button btn = new Button(this);
        btn.setText(label); btn.setTextColor(color); btn.setBackgroundColor(0xFF023050);
        btn.setAllCaps(false); btn.setTextSize(10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
        lp.setMargins(0, dp(6), dp(8), 0);
        btn.setLayoutParams(lp);
        return btn;
    }
    private Button makeSaveBtn(String label, int color) {
        Button btn = new Button(this);
        btn.setText(label); btn.setTextColor(BG); btn.setBackgroundColor(color);
        btn.setAllCaps(false); btn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        lp.setMargins(0, dp(10), 0, 0);
        btn.setLayoutParams(lp);
        return btn;
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    protected int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private int parseIntSafe(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }
    private String orDefault(String s, String def) { return s.trim().isEmpty() ? def : s.trim(); }
}
