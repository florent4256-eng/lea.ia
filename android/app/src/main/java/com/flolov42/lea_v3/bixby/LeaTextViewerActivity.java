package com.flolov42.lea_v3.bixby;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Affiche le texte généré (code, livre, paroles) dans une popup.
 * Rend le micro à la fermeture si give_mic_on_close = true.
 */
public class LeaTextViewerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fenêtre transparente style popup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String content     = getIntent().getStringExtra("content");
        String contentType = getIntent().getStringExtra("contentType");
        if (content == null)     content = "";
        if (contentType == null) contentType = "text";

        boolean isCode = "code".equals(contentType);

        // ── FOND PRINCIPAL ────────────────────────────────────────────────────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24, 24, 24, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F0F1A"));
        bg.setCornerRadius(32f);
        bg.setStroke(2, Color.parseColor("#7C3AED"));
        root.setBackground(bg);

        // ── TITRE ─────────────────────────────────────────────────────────────
        TextView title = new TextView(this);
        title.setText(isCode ? "💻 Code généré" : ("book".equals(contentType) ? "📚 Texte généré" : "🎵 Paroles générées"));
        title.setTextColor(Color.parseColor("#A78BFA"));
        title.setTextSize(16f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        // ── ZONE DE TEXTE ─────────────────────────────────────────────────────
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setTextColor(Color.parseColor("#E2E8F0"));
        textView.setTextSize(13f);
        textView.setPadding(16, 16, 16, 16);
        if (isCode) {
            textView.setTypeface(Typeface.MONOSPACE);
            textView.setBackgroundColor(Color.parseColor("#0D1117"));
        }

        scrollView.addView(textView);
        root.addView(scrollView);

        // ── BOUTONS ───────────────────────────────────────────────────────────
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        btnRow.setPadding(0, 16, 0, 0);

        // Bouton Copier
        Button copyBtn = new Button(this);
        copyBtn.setText("📋 Copier");
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setTextSize(13f);
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setColor(Color.parseColor("#4C1D95"));
        copyBg.setCornerRadius(24f);
        copyBtn.setBackground(copyBg);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        copyLp.setMargins(0, 0, 8, 0);
        copyBtn.setLayoutParams(copyLp);
        final String finalContent = content;
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("lea_text", finalContent));
            Toast.makeText(this, "✅ Copié !", Toast.LENGTH_SHORT).show();
            copyBtn.setText("✅ Copié");
            new Handler(Looper.getMainLooper()).postDelayed(() -> copyBtn.setText("📋 Copier"), 2000);
        });

        // Bouton Fermer
        Button closeBtn = new Button(this);
        closeBtn.setText("✖ Fermer");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(13f);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(Color.parseColor("#7C3AED"));
        closeBg.setCornerRadius(24f);
        closeBtn.setBackground(closeBg);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        closeLp.setMargins(8, 0, 0, 0);
        closeBtn.setLayoutParams(closeLp);
        closeBtn.setOnClickListener(v -> finish());

        btnRow.addView(copyBtn);
        btnRow.addView(closeBtn);
        root.addView(btnRow);

        // Dimensions : 90% largeur, 80% hauteur
        int w, h;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            w = (int) (bounds.width() * 0.90f);
            h = (int) (bounds.height() * 0.80f);
        } else {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            //noinspection deprecation
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            w = (int) (dm.widthPixels * 0.90f);
            h = (int) (dm.heightPixels * 0.80f);
        }

        setContentView(root);
        getWindow().setLayout(w, h);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent == null) return;
        String newContent  = intent.getStringExtra("content");
        String newType     = intent.getStringExtra("contentType");
        if (newContent != null) {
            // Le contenu a changé — recréer l'activité proprement
            finish();
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent i = getIntent();
        if (i != null && i.getBooleanExtra("give_mic_on_close", false)) {
            if (LeaNovaService.instance != null) {
                LeaNovaService.instance.triggerCooldownFromActivity();
            }
        }
    }
}
