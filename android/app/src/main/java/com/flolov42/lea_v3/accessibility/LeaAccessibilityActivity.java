package com.flolov42.lea_v3.accessibility;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class LeaAccessibilityActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;

    private LeaAccessibilityManager a11yMgr;
    private LeaFeaturesDatabase.A11ySettings current;

    private Switch ttsSw, contrastSw, hapticSw;
    private RadioGroup fontSizeGroup, colorBlindGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        a11yMgr = LeaAccessibilityManager.get(this);
        current = a11yMgr.getSettings();
        setContentView(buildUI());
    }

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeSectionTitle("Lecture d'écran"));
        root.addView(makeTTSCard());
        root.addView(makeSectionTitle("Taille du texte"));
        root.addView(makeFontSizeCard());
        root.addView(makeSectionTitle("Contraste & Couleurs"));
        root.addView(makeContrastCard());
        root.addView(makeSectionTitle("Mode daltonien"));
        root.addView(makeColorBlindCard());
        root.addView(makeSectionTitle("Retour tactile"));
        root.addView(makeHapticCard());
        root.addView(makeSaveButton());

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF012030, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("♿"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Accessibilité");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Personnalisez Léa pour vos besoins");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeTTSCard() {
        LinearLayout card = makeCard();
        LinearLayout row = makeRow();

        LinearLayout text = makeTextBlock("🔊 Synthèse vocale", "Lire le contenu automatiquement");
        row.addView(text);

        ttsSw = new Switch(this);
        ttsSw.setChecked(current.ttsEnabled == 1);
        row.addView(ttsSw);

        card.addView(row);
        Button testBtn = new Button(this);
        testBtn.setText("Tester la voix");
        testBtn.setTextColor(CYAN); testBtn.setBackgroundColor(Color.TRANSPARENT);
        testBtn.setOnClickListener(v -> {
            a11yMgr.speak("Bonjour, je suis Léa, votre assistante!");
            Toast.makeText(this, "Test vocal lancé", Toast.LENGTH_SHORT).show();
        });
        card.addView(testBtn);
        return card;
    }

    private LinearLayout makeFontSizeCard() {
        LinearLayout card = makeCard();
        TextView label = new TextView(this); label.setText("📏 Taille actuelle: " + current.fontSize);
        label.setTextColor(0xFFB0BEC5); label.setTextSize(12);
        card.addView(label);

        fontSizeGroup = new RadioGroup(this);
        fontSizeGroup.setOrientation(RadioGroup.HORIZONTAL);
        String[] sizes = {"small", "normal", "large", "extra_large"};
        String[] labels = {"Petit", "Normal", "Grand", "Très grand"};
        for (int i = 0; i < sizes.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i]); rb.setTag(sizes[i]);
            rb.setTextColor(Color.WHITE);
            if (sizes[i].equals(current.fontSize)) rb.setChecked(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            rb.setLayoutParams(lp);
            fontSizeGroup.addView(rb);
        }
        card.addView(fontSizeGroup);
        return card;
    }

    private LinearLayout makeContrastCard() {
        LinearLayout card = makeCard();
        LinearLayout row = makeRow();
        row.addView(makeTextBlock("🌓 Contraste élevé", "Améliore la lisibilité"));
        contrastSw = new Switch(this);
        contrastSw.setChecked(current.highContrast == 1);
        row.addView(contrastSw);
        card.addView(row);
        return card;
    }

    private LinearLayout makeColorBlindCard() {
        LinearLayout card = makeCard();
        String[] modes = {"none", "deuteranopia", "protanopia", "tritanopia"};
        String[] modeLabels = {"Normal", "Deuteranopie (vert/rouge)", "Protanopie (rouge)", "Tritanopie (bleu)"};
        colorBlindGroup = new RadioGroup(this);
        for (int i = 0; i < modes.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(modeLabels[i]); rb.setTag(modes[i]);
            rb.setTextColor(Color.WHITE);
            if (modes[i].equals(current.colorBlindMode)) rb.setChecked(true);
            colorBlindGroup.addView(rb);
        }
        card.addView(colorBlindGroup);
        return card;
    }

    private LinearLayout makeHapticCard() {
        LinearLayout card = makeCard();
        LinearLayout row = makeRow();
        row.addView(makeTextBlock("📳 Retour haptique", "Vibration sur les interactions"));
        hapticSw = new Switch(this);
        hapticSw.setChecked(current.hapticFeedback == 1);
        row.addView(hapticSw);
        card.addView(row);
        return card;
    }

    private Button makeSaveButton() {
        Button btn = new Button(this);
        btn.setText("💾 Sauvegarder");
        btn.setTextColor(Color.BLACK); btn.setTextSize(16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CYAN); bg.setCornerRadius(dp(50));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(20), 0, 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> saveSettings());
        return btn;
    }

    private void saveSettings() {
        String fontSize = "normal";
        RadioButton checkedFont = fontSizeGroup != null ? fontSizeGroup.findViewById(fontSizeGroup.getCheckedRadioButtonId()) : null;
        if (checkedFont != null) fontSize = (String) checkedFont.getTag();

        String colorBlind = "none";
        RadioButton checkedColor = colorBlindGroup != null ? colorBlindGroup.findViewById(colorBlindGroup.getCheckedRadioButtonId()) : null;
        if (checkedColor != null) colorBlind = (String) checkedColor.getTag();

        int tts = ttsSw != null && ttsSw.isChecked() ? 1 : 0;
        int contrast = contrastSw != null && contrastSw.isChecked() ? 1 : 0;
        int haptic = hapticSw != null && hapticSw.isChecked() ? 1 : 0;

        a11yMgr.saveSettings(tts, fontSize, contrast, colorBlind, haptic);
        a11yMgr.haptic();
        Toast.makeText(this, "✅ Paramètres sauvegardés!", Toast.LENGTH_SHORT).show();
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x3300E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(lp);
        return card;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private LinearLayout makeTextBlock(String title, String desc) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        block.setLayoutParams(lp);
        TextView t1 = new TextView(this); t1.setText(title); t1.setTextColor(Color.WHITE); t1.setTextSize(15);
        t1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        block.addView(t1);
        TextView t2 = new TextView(this); t2.setText(desc); t2.setTextColor(0xFFB0BEC5); t2.setTextSize(12);
        block.addView(t2);
        return block;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextColor(CYAN); tv.setTextSize(15); tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, dp(6)); tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
