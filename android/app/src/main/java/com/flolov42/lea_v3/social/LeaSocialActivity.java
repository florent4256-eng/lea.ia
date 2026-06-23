package com.flolov42.lea_v3.social;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.achievements.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LeaSocialActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    private LeaFeaturesDatabase db;
    private LinearLayout forumList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        db = LeaFeaturesDatabase.get(this);
        setContentView(buildUI());
        loadForum();
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeProfileCard());
        root.addView(makeNavRow());
        root.addView(makeShareCard());
        root.addView(makeSectionTitle("💬 Forum Communauté"));
        root.addView(makeNewPostBtn());

        forumList = new LinearLayout(this);
        forumList.setOrientation(LinearLayout.VERTICAL);
        root.addView(forumList);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF001A2E, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("🌐"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Social & Communauté");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Classements • Défis • Partages • Forum");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeProfileCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(2), CYAN);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        LeaFeaturesDatabase.SocialProfile profile = db.getMyProfile();
        LeaPlusDatabase.CharStats stats = LeaPlusDatabase.get(this).getCharStats();
        db.syncProfileStats(stats.xp, db.getUnlockedCount());

        TextView avatar = new TextView(this); avatar.setText(profile.avatar);
        avatar.setTextSize(36); avatar.setMinWidth(dp(60)); avatar.setGravity(Gravity.CENTER);
        card.addView(avatar);

        LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(dp(12), 0, 0, 0); text.setLayoutParams(tlp);

        TextView nameTv = new TextView(this); nameTv.setText(profile.pseudo);
        nameTv.setTextColor(CYAN); nameTv.setTextSize(18);
        nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.addView(nameTv);

        TextView statsTv = new TextView(this);
        statsTv.setText("Niv." + stats.level + "  •  " + stats.xp + " XP  •  🏆" + db.getUnlockedCount());
        statsTv.setTextColor(0xFFB0BEC5); statsTv.setTextSize(12);
        text.addView(statsTv);

        card.addView(text);

        Button editBtn = new Button(this);
        editBtn.setText("✏️"); editBtn.setTextColor(CYAN); editBtn.setBackgroundColor(Color.TRANSPARENT);
        editBtn.setOnClickListener(v -> showEditProfile(profile));
        card.addView(editBtn);

        return card;
    }

    private LinearLayout makeNavRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        row.setLayoutParams(lp);

        row.addView(makeNavBtn("🏆 Classement", () -> startActivity(new Intent(this, LeaLeaderboardActivity.class))));
        row.addView(makeNavBtn("⚔️ Défis", () -> startActivity(new Intent(this, LeaChallengeActivity.class))));
        return row;
    }

    private LinearLayout makeNavBtn(String label, Runnable action) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x4400E5FF);
        btn.setBackground(bg);
        btn.setPadding(dp(12), dp(16), dp(12), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(72), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        btn.setLayoutParams(lp);

        TextView tv = new TextView(this); tv.setText(label);
        tv.setTextColor(Color.WHITE); tv.setTextSize(14);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); tv.setGravity(Gravity.CENTER);
        btn.addView(tv);

        btn.setOnClickListener(v -> action.run());
        return btn;
    }

    private LinearLayout makeShareCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF010F1A); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        TextView title = new TextView(this); title.setText("📢 Partager mes stats");
        title.setTextColor(CYAN); title.setTextSize(15); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(title);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, dp(8), 0, 0);

        Button shareStatsBtn = makeSmallBtn("📊 Stats", CYAN);
        shareStatsBtn.setOnClickListener(v -> LeaShareManager.get(this).shareStats(this));
        btns.addView(shareStatsBtn);

        Button shareAchBtn = makeSmallBtn("🏆 Achievement", 0xFF7B2CBF);
        shareAchBtn.setOnClickListener(v -> {
            List<LeaFeaturesDatabase.Achievement> all = db.getAllAchievements();
            for (LeaFeaturesDatabase.Achievement a : all) {
                if (a.unlocked == 1) {
                    LeaShareManager.get(this).shareAchievement(this, a.name, a.icon);
                    return;
                }
            }
            Toast.makeText(this, "Débloquez d'abord un achievement!", Toast.LENGTH_SHORT).show();
        });
        btns.addView(shareAchBtn);

        card.addView(btns);
        return card;
    }

    private Button makeNewPostBtn() {
        Button btn = new Button(this);
        btn.setText("✍️ Nouveau Post");
        btn.setTextColor(CYAN); btn.setTextSize(14);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(50)); bg.setStroke(dp(1), CYAN);
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lp.setMargins(0, 0, 0, dp(12));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> showNewPostDialog());
        return btn;
    }

    private void loadForum() {
        forumList.removeAllViews();
        List<LeaFeaturesDatabase.ForumPost> posts = db.getForumPosts(20);
        if (posts.isEmpty()) {
            // Seed sample posts
            LeaPlusDatabase.CharStats stats = LeaPlusDatabase.get(this).getCharStats();
            db.insertForumPost("LéaTeam", "Bienvenue sur le forum!", "Partagez vos tips, astuces et encouragements. Ensemble on est plus forts! 🚀");
            db.insertForumPost("GalaxyMaster", "Astuce streak 30 jours", "La clé: checkin tous les matins dès le réveil, avant même le café ☕");
            db.insertForumPost(db.getMyProfile().pseudo, "Mon premier post 🎉", "Niveau " + stats.level + " atteint! Qui pour m'aider à aller plus loin?");
            posts = db.getForumPosts(20);
        }
        for (LeaFeaturesDatabase.ForumPost post : posts) {
            forumList.addView(makePostCard(post));
        }
    }

    private LinearLayout makePostCard(LeaFeaturesDatabase.ForumPost post) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x1500E5FF);
        card.setBackground(bg);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView authorTv = new TextView(this); authorTv.setText("👤 " + post.authorPseudo);
        authorTv.setTextColor(CYAN); authorTv.setTextSize(13);
        authorTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        authorTv.setLayoutParams(alp);
        header.addView(authorTv);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
        TextView dateTv = new TextView(this); dateTv.setText(sdf.format(new Date(post.timestamp)));
        dateTv.setTextColor(0xFF546E7A); dateTv.setTextSize(11);
        header.addView(dateTv);

        card.addView(header);

        TextView titleTv = new TextView(this);
        titleTv.setText(post.title);
        titleTv.setTextColor(Color.WHITE); titleTv.setTextSize(15);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, dp(6), 0, 0);
        card.addView(titleTv);

        TextView contentTv = new TextView(this);
        contentTv.setText(post.content);
        contentTv.setTextColor(0xFFB0BEC5); contentTv.setTextSize(13);
        contentTv.setLineSpacing(dp(3), 1f);
        contentTv.setPadding(0, dp(4), 0, dp(8));
        card.addView(contentTv);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView likes = new TextView(this); likes.setText("❤️ " + post.likes);
        likes.setTextColor(0xFF78909C); likes.setTextSize(13);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        likes.setLayoutParams(llp);
        actions.addView(likes);

        Button likeBtn = new Button(this);
        likeBtn.setText("👍 J'aime"); likeBtn.setTextColor(CYAN);
        likeBtn.setBackgroundColor(Color.TRANSPARENT);
        likeBtn.setOnClickListener(v -> {
            db.likePost(post.id);
            likes.setText("❤️ " + (post.likes + 1));
            Toast.makeText(this, "❤️ Aimé!", Toast.LENGTH_SHORT).show();
        });
        actions.addView(likeBtn);

        card.addView(actions);
        return card;
    }

    private void showNewPostDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        EditText titleEt = new EditText(this);
        titleEt.setHint("Titre du post");
        titleEt.setTextColor(Color.WHITE); titleEt.setHintTextColor(0xFF546E7A);
        layout.addView(titleEt);

        EditText contentEt = new EditText(this);
        contentEt.setHint("Contenu...");
        contentEt.setTextColor(Color.WHITE); contentEt.setHintTextColor(0xFF546E7A);
        contentEt.setMinLines(4);
        layout.addView(contentEt);

        new AlertDialog.Builder(this)
            .setTitle("✍️ Nouveau Post")
            .setView(layout)
            .setPositiveButton("Publier", (d, w) -> {
                String title = titleEt.getText().toString().trim();
                String content = contentEt.getText().toString().trim();
                if (!title.isEmpty() && !content.isEmpty()) {
                    String pseudo = db.getMyProfile().pseudo;
                    db.insertForumPost(pseudo, title, content);
                    loadForum();
                    Toast.makeText(this, "Post publié!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Annuler", null).show();
    }

    private void showEditProfile(LeaFeaturesDatabase.SocialProfile profile) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        EditText pseudoEt = new EditText(this);
        pseudoEt.setHint("Pseudo"); pseudoEt.setText(profile.pseudo);
        pseudoEt.setTextColor(Color.WHITE);
        layout.addView(pseudoEt);

        String[] avatars = {"🌟", "🚀", "🦁", "🐉", "🌙", "⚡", "🔥", "💎"};
        LinearLayout avatarRow = new LinearLayout(this);
        avatarRow.setOrientation(LinearLayout.HORIZONTAL);
        final String[] selectedAvatar = {profile.avatar};
        for (String av : avatars) {
            TextView avTv = new TextView(this); avTv.setText(av); avTv.setTextSize(24);
            avTv.setPadding(dp(8), dp(4), dp(8), dp(4));
            avTv.setOnClickListener(v -> { selectedAvatar[0] = av; Toast.makeText(this, av + " sélectionné", Toast.LENGTH_SHORT).show(); });
            avatarRow.addView(avTv);
        }
        layout.addView(avatarRow);

        new AlertDialog.Builder(this)
            .setTitle("✏️ Modifier le profil")
            .setView(layout)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                String pseudo = pseudoEt.getText().toString().trim();
                if (!pseudo.isEmpty()) {
                    db.updateMyProfile(pseudo, selectedAvatar[0]);
                    recreate();
                }
            })
            .setNegativeButton("Annuler", null).show();
    }

    private Button makeSmallBtn(String label, int color) {
        Button btn = new Button(this);
        btn.setText(label); btn.setTextColor(Color.BLACK); btn.setTextSize(12);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(50));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        lp.setMargins(0, 0, dp(8), 0);
        btn.setLayoutParams(lp);
        btn.setPadding(dp(16), dp(4), dp(16), dp(4));
        return btn;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15); tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8)); tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
