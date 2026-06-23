package com.flolov42.lea_v3.security;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LeaVaultActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int RED  = 0xFFFF4444;

    private LinearLayout vaultList;
    private LeaFeaturesDatabase db;
    private LeaBiometricManager bio;
    private boolean authenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        db = LeaFeaturesDatabase.get(this);
        bio = LeaBiometricManager.get(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        root.addView(makeHeader());
        root.addView(makeSecurityCard());

        vaultList = new LinearLayout(this);
        vaultList.setOrientation(LinearLayout.VERTICAL);
        root.addView(vaultList);

        root.addView(makeAddButton());

        scroll.addView(root);
        setContentView(scroll);

        if (bio.isBiometricEnabled() && bio.isAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bio.authenticate(this, "🔐 Léa Vault", "Authentifiez-vous pour accéder à vos secrets",
                new LeaBiometricManager.AuthCallback() {
                    @Override public void onSuccess() { authenticated = true; refreshVaultList(); }
                    @Override public void onFailure(String reason) {
                        Toast.makeText(LeaVaultActivity.this, "Accès refusé: " + reason, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
        } else {
            authenticated = true;
            refreshVaultList();
        }
    }

    private void refreshVaultList() {
        vaultList.removeAllViews();
        if (!authenticated) {
            TextView lock = new TextView(this);
            lock.setText("🔒 Authentification requise");
            lock.setTextColor(0xFFB0BEC5); lock.setGravity(Gravity.CENTER);
            lock.setPadding(0, dp(32), 0, 0);
            vaultList.addView(lock);
            return;
        }
        List<LeaFeaturesDatabase.VaultItem> items = db.getVaultItems();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("🔐 Vault vide\nAjoute ton premier secret!");
            empty.setTextColor(0xFF546E7A); empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(32), 0, 0);
            vaultList.addView(empty);
            return;
        }
        for (LeaFeaturesDatabase.VaultItem item : items) {
            vaultList.addView(makeVaultCard(item));
        }
    }

    private LinearLayout makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xFF1A0033, 0xFF011627});
        gd.setCornerRadius(dp(16));
        h.setBackground(gd);
        h.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        h.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("🔐"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        h.addView(icon);
        TextView title = new TextView(this); title.setText("Vault Sécurisé");
        title.setTextColor(CYAN); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        h.addView(title);
        TextView sub = new TextView(this); sub.setText("Stockez vos mots de passe & secrets chiffrés");
        sub.setTextColor(0xFFB0BEC5); sub.setTextSize(13); sub.setGravity(Gravity.CENTER);
        h.addView(sub);
        TextView back = new TextView(this); back.setText("← Retour");
        back.setTextColor(CYAN); back.setTextSize(14); back.setPadding(0, dp(12), 0, 0); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back);
        return h;
    }

    private LinearLayout makeSecurityCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF010F1A); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x4400E5FF);
        card.setBackground(bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        text.setLayoutParams(tlp);

        TextView t1 = new TextView(this); t1.setText("🔒 Biométrie");
        t1.setTextColor(Color.WHITE); t1.setTextSize(15); t1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.addView(t1);
        TextView t2 = new TextView(this);
        t2.setText(bio.isAvailable() ? "Face ID / Empreinte disponible" : "Non disponible sur cet appareil");
        t2.setTextColor(0xFFB0BEC5); t2.setTextSize(12);
        text.addView(t2);
        card.addView(text);

        Switch sw = new Switch(this);
        LeaFeaturesDatabase.BiometricCfg cfg = db.getBiometricConfig();
        sw.setChecked(cfg.enabled == 1);
        sw.setEnabled(bio.isAvailable());
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            bio.setEnabled(isChecked);
            Toast.makeText(this, isChecked ? "Biométrie activée" : "Biométrie désactivée", Toast.LENGTH_SHORT).show();
        });
        card.addView(sw);
        return card;
    }

    private LinearLayout makeVaultCard(LeaFeaturesDatabase.VaultItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x2200E5FF);
        card.setBackground(bg);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textBlock.setLayoutParams(tlp);

        TextView nameTv = new TextView(this);
        nameTv.setText("🔑 " + item.title);
        nameTv.setTextColor(Color.WHITE); nameTv.setTextSize(15);
        nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textBlock.addView(nameTv);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        TextView dateTv = new TextView(this);
        dateTv.setText("Créé le " + sdf.format(new Date(item.createdDate)));
        dateTv.setTextColor(0xFF546E7A); dateTv.setTextSize(11);
        textBlock.addView(dateTv);

        card.addView(textBlock);

        Button viewBtn = new Button(this);
        viewBtn.setText("Voir");
        viewBtn.setTextColor(CYAN); viewBtn.setBackgroundColor(Color.TRANSPARENT);
        viewBtn.setOnClickListener(v -> showVaultItem(item));
        card.addView(viewBtn);

        Button delBtn = new Button(this);
        delBtn.setText("🗑");
        delBtn.setTextColor(RED); delBtn.setBackgroundColor(Color.TRANSPARENT);
        delBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer \"" + item.title + "\"?")
                .setPositiveButton("Oui", (d, w) -> { db.deleteVaultItem(item.id); refreshVaultList(); })
                .setNegativeButton("Non", null).show();
        });
        card.addView(delBtn);
        return card;
    }

    private void showVaultItem(LeaFeaturesDatabase.VaultItem item) {
        db.touchVaultItem(item.id);
        String plain = bio.simpleDecrypt(item.contentEncrypted);
        new AlertDialog.Builder(this)
            .setTitle("🔑 " + item.title)
            .setMessage(plain)
            .setPositiveButton("Fermer", null).show();
    }

    private View makeAddButton() {
        Button btn = new Button(this);
        btn.setText("+ Ajouter un Secret");
        btn.setTextColor(Color.BLACK); btn.setTextSize(16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CYAN); bg.setCornerRadius(dp(50));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(16), 0, 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> showAddDialog());
        return btn;
    }

    private void showAddDialog() {
        if (!authenticated) { Toast.makeText(this, "Authentification requise", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        EditText titleEt = new EditText(this);
        titleEt.setHint("Titre (ex: Gmail)");
        titleEt.setTextColor(Color.WHITE); titleEt.setHintTextColor(0xFF546E7A);
        layout.addView(titleEt);

        EditText contentEt = new EditText(this);
        contentEt.setHint("Secret / Mot de passe");
        contentEt.setTextColor(Color.WHITE); contentEt.setHintTextColor(0xFF546E7A);
        contentEt.setMinLines(3);
        layout.addView(contentEt);

        new AlertDialog.Builder(this)
            .setTitle("Nouveau Secret")
            .setView(layout)
            .setPositiveButton("Sauvegarder", (d, w) -> {
                String title = titleEt.getText().toString().trim();
                String content = contentEt.getText().toString().trim();
                if (!title.isEmpty() && !content.isEmpty()) {
                    String encrypted = bio.simpleEncrypt(content);
                    db.insertVaultItem(title, encrypted);
                    refreshVaultList();
                    Toast.makeText(this, "Secret sauvegardé!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Annuler", null).show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
