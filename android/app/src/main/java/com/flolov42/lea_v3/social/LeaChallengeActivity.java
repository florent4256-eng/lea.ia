package com.flolov42.lea_v3.social;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.List;

public class LeaChallengeActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    private LinearLayout challengeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        setContentView(buildUI());
        refreshList();
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeNewChallengeBtn());
        root.addView(makeSectionTitle("⚔️ Défis actifs & historique"));

        challengeList = new LinearLayout(this);
        challengeList.setOrientation(LinearLayout.VERTICAL);
        root.addView(challengeList);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF200020, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("⚔️"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Défis & Challenges");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Défiez vos amis sur vos performances");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private Button makeNewChallengeBtn() {
        Button btn = new Button(this);
        btn.setText("⚔️ Créer un Défi");
        btn.setTextColor(Color.BLACK); btn.setTextSize(16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CYAN); bg.setCornerRadius(dp(50));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, 0, 0, dp(16));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> showNewChallengeDialog());
        return btn;
    }

    private void showNewChallengeDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        EditText opponentEt = new EditText(this);
        opponentEt.setHint("Pseudo de l'adversaire");
        opponentEt.setTextColor(Color.WHITE); opponentEt.setHintTextColor(0xFF546E7A);
        layout.addView(opponentEt);

        String[] types = {"Streak 7 jours", "100 XP en 3 jours", "5 habitudes/semaine", "Level up avant moi"};
        Spinner typeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        layout.addView(typeSpinner);

        new AlertDialog.Builder(this)
            .setTitle("⚔️ Nouveau Défi")
            .setView(layout)
            .setPositiveButton("Lancer", (d, w) -> {
                String opponent = opponentEt.getText().toString().trim();
                String type = types[typeSpinner.getSelectedItemPosition()];
                if (!opponent.isEmpty()) {
                    LeaFeaturesDatabase.SocialProfile me = LeaFeaturesDatabase.get(this).getMyProfile();
                    LeaFeaturesDatabase.get(this).insertChallenge(me.pseudo, opponent, type, "100 XP");
                    LeaShareManager.get(this).challengeFriend(this, "\"" + type + "\" — Qui gagne?");
                    refreshList();
                    Toast.makeText(this, "Défi créé!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Annuler", null).show();
    }

    private void refreshList() {
        challengeList.removeAllViews();
        List<LeaFeaturesDatabase.Challenge> challenges = LeaFeaturesDatabase.get(this).getChallenges();
        if (challenges.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun défi en cours.\nCréez votre premier défi!");
            empty.setTextColor(0xFF546E7A); empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, 0);
            challengeList.addView(empty);
            return;
        }
        for (LeaFeaturesDatabase.Challenge c : challenges) {
            challengeList.addView(makeChallengeCard(c));
        }
    }

    private LinearLayout makeChallengeCard(LeaFeaturesDatabase.Challenge c) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        boolean active = "pending".equals(c.status);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), active ? 0x6600E5FF : 0x2200E5FF);
        card.setBackground(bg);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView ic = new TextView(this); ic.setText(active ? "⚔️" : "✅"); ic.setTextSize(20);
        ic.setMinWidth(dp(36));
        header.addView(ic);

        TextView vs = new TextView(this);
        vs.setText(c.challengerPseudo + " vs " + c.opponentPseudo);
        vs.setTextColor(Color.WHITE); vs.setTextSize(15);
        vs.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        vs.setLayoutParams(vlp);
        header.addView(vs);

        TextView statusTv = new TextView(this);
        statusTv.setText(c.status);
        statusTv.setTextColor(active ? 0xFFFFC107 : 0xFF4CAF50); statusTv.setTextSize(11);
        header.addView(statusTv);

        card.addView(header);

        TextView typeTv = new TextView(this);
        typeTv.setText("🎯 " + c.challengeType + "  •  🏆 " + c.reward);
        typeTv.setTextColor(0xFFB0BEC5); typeTv.setTextSize(12);
        typeTv.setPadding(dp(36), dp(4), 0, 0);
        card.addView(typeTv);

        if (active) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(dp(36), dp(8), 0, 0);

            Button acceptBtn = new Button(this);
            acceptBtn.setText("✅ Gagné"); acceptBtn.setTextColor(0xFF4CAF50);
            acceptBtn.setBackgroundColor(Color.TRANSPARENT);
            acceptBtn.setOnClickListener(v -> {
                LeaFeaturesDatabase.get(this).updateChallengeStatus(c.id, "won");
                refreshList();
            });
            actions.addView(acceptBtn);

            Button lostBtn = new Button(this);
            lostBtn.setText("❌ Perdu"); lostBtn.setTextColor(0xFFFF4444);
            lostBtn.setBackgroundColor(Color.TRANSPARENT);
            lostBtn.setOnClickListener(v -> {
                LeaFeaturesDatabase.get(this).updateChallengeStatus(c.id, "lost");
                refreshList();
            });
            actions.addView(lostBtn);
            card.addView(actions);
        }
        return card;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15); tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8)); tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
