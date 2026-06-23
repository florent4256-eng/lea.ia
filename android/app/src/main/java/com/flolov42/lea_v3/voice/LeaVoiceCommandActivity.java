package com.flolov42.lea_v3.voice;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import java.util.List;

public class LeaVoiceCommandActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    private TextView micButton;
    private TextView statusTv;
    private TextView responseTv;
    private LinearLayout commandHistory;
    private LeaVoiceCommandManager voiceMgr;
    private boolean isListening = false;
    private ObjectAnimator pulseAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        voiceMgr = LeaVoiceCommandManager.get(this);
        setContentView(buildUI());
        loadHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voiceMgr.stopListening();
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeMicSection());
        root.addView(makeResponseCard());
        root.addView(makeCommandsRef());
        root.addView(makeSectionTitle("Historique"));

        commandHistory = new LinearLayout(this);
        commandHistory.setOrientation(LinearLayout.VERTICAL);
        root.addView(commandHistory);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF1A0050, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(20));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("🎤"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Commandes Vocales");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Parlez à Léa, elle vous répond");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeMicSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        section.setLayoutParams(lp);

        micButton = new TextView(this);
        micButton.setText("🎤");
        micButton.setTextSize(56);
        micButton.setGravity(Gravity.CENTER);

        GradientDrawable micBg = new GradientDrawable();
        micBg.setShape(GradientDrawable.OVAL);
        micBg.setColor(CARD);
        micBg.setStroke(dp(3), CYAN);
        micButton.setBackground(micBg);
        micButton.setMinWidth(dp(100)); micButton.setMinHeight(dp(100));
        micButton.setPadding(dp(20), dp(20), dp(20), dp(20));

        micButton.setOnClickListener(v -> toggleListening());
        section.addView(micButton);

        statusTv = new TextView(this);
        statusTv.setText("Appuyez pour parler");
        statusTv.setTextColor(0xFFB0BEC5); statusTv.setTextSize(14); statusTv.setGravity(Gravity.CENTER);
        statusTv.setPadding(0, dp(12), 0, 0);
        section.addView(statusTv);

        return section;
    }

    private LinearLayout makeResponseCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        TextView label = new TextView(this); label.setText("Réponse de Léa:");
        label.setTextColor(CYAN); label.setTextSize(13); label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(label);

        responseTv = new TextView(this);
        responseTv.setText("...");
        responseTv.setTextColor(Color.WHITE); responseTv.setTextSize(15);
        responseTv.setLineSpacing(dp(4), 1f);
        responseTv.setPadding(0, dp(8), 0, 0);
        card.addView(responseTv);

        return card;
    }

    private LinearLayout makeCommandsRef() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF010F1A); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x2200E5FF);
        card.setBackground(bg);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        TextView title = new TextView(this); title.setText("💡 Commandes disponibles:");
        title.setTextColor(CYAN); title.setTextSize(13); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(title);

        String[] commands = {
            "\"Combien de coins?\" → Solde actuel",
            "\"Mon niveau?\" → Niveau et XP",
            "\"Mes habitudes?\" → Streaks et stats",
            "\"Mes quêtes?\" → Quêtes disponibles",
            "\"Mes achievements?\" → Badges débloqués",
            "\"Internet?\" → Statut connexion",
            "\"Mon thème?\" → Thème actuel",
        };
        for (String cmd : commands) {
            TextView tv = new TextView(this);
            tv.setText("• " + cmd);
            tv.setTextColor(0xFF78909C); tv.setTextSize(12);
            tv.setPadding(0, dp(3), 0, dp(3));
            card.addView(tv);
        }
        return card;
    }

    private void toggleListening() {
        if (isListening) {
            isListening = false;
            voiceMgr.stopListening();
            statusTv.setText("Appuyez pour parler");
            micButton.setText("🎤");
            if (pulseAnim != null) pulseAnim.cancel();
            micButton.setAlpha(1f);
        } else {
            isListening = true;
            statusTv.setText("Je vous écoute...");
            micButton.setText("⏹️");
            startPulse();

            voiceMgr.startListening(new LeaVoiceCommandManager.VoiceCallback() {
                @Override public void onResult(String command, String response) {
                    runOnUiThread(() -> {
                        isListening = false;
                        micButton.setText("🎤");
                        statusTv.setText("Appuyez pour parler");
                        if (pulseAnim != null) pulseAnim.cancel();
                        micButton.setAlpha(1f);
                        responseTv.setText("🎤 \"" + command + "\"\n\n🤖 " + response);
                        addToHistory(command, response);
                    });
                }
                @Override public void onError(String error) {
                    runOnUiThread(() -> {
                        isListening = false;
                        micButton.setText("🎤");
                        statusTv.setText("Erreur: " + error);
                        if (pulseAnim != null) pulseAnim.cancel();
                        micButton.setAlpha(1f);
                    });
                }
            });
        }
    }

    private void startPulse() {
        pulseAnim = ObjectAnimator.ofFloat(micButton, "alpha", 1f, 0.4f);
        pulseAnim.setDuration(800);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnim.start();
    }

    private void addToHistory(String cmd, String response) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(10)); bg.setStroke(dp(1), 0x1500E5FF);
        item.setBackground(bg);
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        item.setLayoutParams(lp);

        TextView cmdTv = new TextView(this); cmdTv.setText("🎤 " + cmd);
        cmdTv.setTextColor(Color.WHITE); cmdTv.setTextSize(13);
        item.addView(cmdTv);

        TextView resTv = new TextView(this); resTv.setText("🤖 " + response);
        resTv.setTextColor(0xFFB0BEC5); resTv.setTextSize(12);
        resTv.setPadding(0, dp(2), 0, 0);
        item.addView(resTv);

        commandHistory.addView(item, 0);
    }

    private void loadHistory() {
        commandHistory.removeAllViews();
        List<LeaFeaturesDatabase.VoiceCmd> cmds = LeaFeaturesDatabase.get(this).getRecentCommands(10);
        for (LeaFeaturesDatabase.VoiceCmd c : cmds) {
            addToHistory(c.commandText, c.executionResult != null ? c.executionResult : "");
        }
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
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
