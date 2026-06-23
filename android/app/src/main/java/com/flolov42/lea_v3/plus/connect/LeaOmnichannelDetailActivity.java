package com.flolov42.lea_v3.plus.connect;

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
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

public class LeaOmnichannelDetailActivity extends LeaFeatureDetailActivity {

    private static final int BG    = 0xFF000D1A;
    private static final int CARD  = 0xFF001A2E;
    private static final int CARD2 = 0xFF00243F;
    private static final int CYAN  = 0xFF00E5FF;
    private static final int GREEN = 0xFF10B981;
    private static final int PURPLE= 0xFF7C3AED;
    private static final int ORANGE= 0xFFF59E0B;
    private static final int GOLD  = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM   = 0xFF64748B;
    private static final int DIM2  = 0xFF94A3B8;

    // {emoji, nom, package, url_fallback}
    private static final String[][] MESSAGING_APPS = {
        {"📱", "WhatsApp",           "com.whatsapp",          "https://wa.me"},
        {"📨", "Telegram",           "org.telegram.messenger", "https://t.me"},
        {"💙", "Facebook Messenger", "com.facebook.orca",     "https://messenger.com"},
        {"📧", "Gmail",              "com.google.android.gm", "https://mail.google.com"},
        {"💼", "Slack",              "com.Slack",             "https://slack.com"},
    };

    private static final String[][] SOCIAL_APPS = {
        {"🐦", "Twitter / X",  "com.twitter.android",         "https://x.com"},
        {"📸", "Instagram",    "com.instagram.android",        "https://instagram.com"},
        {"🎵", "TikTok",       "com.zhiliaoapp.musically",     "https://tiktok.com"},
        {"💼", "LinkedIn",     "com.linkedin.android",         "https://linkedin.com"},
    };

    private static final String[] DEVICE_TYPES = {"phone", "tablet", "watch", "pc", "smart_tv", "speaker"};
    private static final String[] DEVICE_TYPES_LABELS = {"📱 Téléphone", "💻 Tablette", "⌚ Montre", "🖥️ PC", "📺 Smart TV", "🔊 Enceinte"};

    @Override protected String getFeatureId() { return LeaPlusDatabase.OMNICHANNEL; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG);
        parent.setPadding(0, 0, 0, 0);

        LeaOmnichannelIntegration omni = new LeaOmnichannelIntegration(this);
        List<LeaPlusDatabase.DeviceRow> devices = db.getDevices();
        long now = System.currentTimeMillis();
        int online = 0;
        for (LeaPlusDatabase.DeviceRow d : devices) if ((now - d.lastSeen) < 300_000L) online++;

        // ── HERO ──────────────────────────────────────────────────────
        LinearLayout hero = card(dp(12), dp(8), dp(12), 0);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        hero.addView(tv("📡", 40, WHITE, Typeface.NORMAL));
        TextView cntTv = tv(online + " / " + Math.max(devices.size(), 1), 26, online > 0 ? GREEN : DIM2, Typeface.BOLD);
        cntTv.setGravity(Gravity.CENTER); cntTv.setPadding(0, dp(6), 0, dp(2)); hero.addView(cntTv);
        hero.addView(tv(devices.isEmpty() ? "aucun appareil enregistré" : "appareil(s) connecté(s)", 11, DIM2, Typeface.NORMAL));
        parent.addView(hero);

        // ── COMMANDE MAISON ────────────────────────────────────────────
        secHeader(parent, "🏠 COMMANDE MAISON INTELLIGENTE");
        LinearLayout cmdCard = card(dp(12), dp(4), dp(12), 0);

        TextView cmdExamples = tv("Ex : \"allume les lumières\", \"règle thermostat à 21°\", \"active le mode nuit\"", 10, DIM, Typeface.ITALIC);
        cmdExamples.setPadding(0, 0, 0, dp(8)); cmdCard.addView(cmdExamples);

        EditText cmdEt = new EditText(this);
        cmdEt.setHint("Tape ta commande...");
        cmdEt.setHintTextColor(DIM); cmdEt.setTextColor(WHITE); cmdEt.setTextSize(13);
        cmdEt.setBackgroundColor(CARD2); cmdEt.setPadding(dp(12), dp(10), dp(12), dp(10));
        cmdCard.addView(cmdEt);

        final TextView resultTv = tv("", 12, GREEN, Typeface.NORMAL);
        resultTv.setPadding(0, dp(6), 0, 0);

        Button cmdBtn = new Button(this); cmdBtn.setText("▶  ENVOYER");
        cmdBtn.setTextColor(BG); cmdBtn.setBackgroundColor(CYAN);
        cmdBtn.setTextSize(11); cmdBtn.setTypeface(null, Typeface.BOLD); cmdBtn.setAllCaps(false);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(-1, dp(42));
        cbLp.setMargins(0, dp(8), 0, 0); cmdBtn.setLayoutParams(cbLp);
        cmdBtn.setOnClickListener(v -> {
            String cmd = cmdEt.getText().toString().trim();
            if (cmd.isEmpty()) return;
            String result = omni.processVoiceCommand(cmd);
            resultTv.setText(result);
            resultTv.setVisibility(View.VISIBLE);
        });
        cmdCard.addView(cmdBtn);
        cmdCard.addView(resultTv);
        resultTv.setVisibility(View.GONE);
        parent.addView(cmdCard);

        // ── APPAREILS CONNECTÉS ────────────────────────────────────────
        secHeader(parent, "🔗 APPAREILS ENREGISTRÉS");
        LinearLayout devCard = card(dp(12), dp(4), dp(12), 0);

        if (devices.isEmpty()) {
            devCard.addView(tv("Aucun appareil — ajoute ton premier appareil ci-dessous", 11, DIM, Typeface.ITALIC));
        } else {
            for (int i = 0; i < devices.size(); i++) {
                LeaPlusDatabase.DeviceRow d = devices.get(i);
                boolean isOnline = (now - d.lastSeen) < 300_000L;
                LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(7), 0, dp(7));

                View dot = new View(this); dot.setBackgroundColor(isOnline ? GREEN : DIM);
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(8), dp(8));
                dLp.setMargins(0, 0, dp(10), 0); dot.setLayoutParams(dLp); row.addView(dot);

                LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
                texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                texts.addView(tv(d.name, 12, WHITE, Typeface.BOLD));
                texts.addView(tv(d.type + (d.ip.isEmpty() ? "" : "  ·  " + d.ip), 10, DIM2, Typeface.NORMAL));
                row.addView(texts);

                row.addView(tv(isOnline ? "🟢 En ligne" : "🔴 Hors ligne", 10, isOnline ? GREEN : DIM, Typeface.NORMAL));
                devCard.addView(row);
                if (i < devices.size() - 1) { View sep = new View(this); sep.setBackgroundColor(CARD2);
                    sep.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); devCard.addView(sep); }
            }
        }

        Button addDevBtn = new Button(this); addDevBtn.setText("➕  AJOUTER UN APPAREIL");
        addDevBtn.setTextColor(CYAN); addDevBtn.setBackgroundColor(CARD2);
        addDevBtn.setTextSize(10); addDevBtn.setTypeface(null, Typeface.BOLD); addDevBtn.setAllCaps(false);
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(-1, dp(38));
        adLp.setMargins(0, dp(10), 0, 0); addDevBtn.setLayoutParams(adLp);
        addDevBtn.setOnClickListener(v -> showAddDeviceDialog(omni));
        devCard.addView(addDevBtn);
        parent.addView(devCard);

        // ── MESSAGERIES ────────────────────────────────────────────────
        secHeader(parent, "💬 MESSAGERIES");
        boolean notifEnabled = isNotificationListenerEnabled();
        if (!notifEnabled) {
            LinearLayout notifBanner = new LinearLayout(this);
            notifBanner.setOrientation(LinearLayout.HORIZONTAL);
            notifBanner.setGravity(Gravity.CENTER_VERTICAL);
            notifBanner.setBackgroundColor(0xFF1A0A00);
            notifBanner.setPadding(dp(14), dp(10), dp(14), dp(10));
            LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(-1, -2);
            nbLp.setMargins(dp(12), dp(4), dp(12), 0); notifBanner.setLayoutParams(nbLp);
            LinearLayout nbTexts = new LinearLayout(this); nbTexts.setOrientation(LinearLayout.VERTICAL);
            nbTexts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            nbTexts.addView(tv("🔔 Accès aux notifications requis", 12, ORANGE, Typeface.BOLD));
            nbTexts.addView(tv("Léa lira tes messages pour les unifier", 10, DIM2, Typeface.NORMAL));
            notifBanner.addView(nbTexts);
            Button activateBtn = new Button(this); activateBtn.setText("Activer");
            activateBtn.setTextColor(ORANGE); activateBtn.setBackgroundColor(CARD2);
            activateBtn.setTextSize(10); activateBtn.setTypeface(null, Typeface.BOLD); activateBtn.setAllCaps(false);
            activateBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
            activateBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(32)));
            activateBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
            notifBanner.addView(activateBtn);
            parent.addView(notifBanner);
        }
        parent.addView(buildAppList(MESSAGING_APPS, notifEnabled ? "✅ Via Léa" : "Ouvrir", GREEN));

        // ── RÉSEAUX SOCIAUX ────────────────────────────────────────────
        secHeader(parent, "📡 RÉSEAUX SOCIAUX");
        parent.addView(buildAppList(SOCIAL_APPS, "Ouvrir", CYAN));

        // ── CE QUE LÉA FAIT ───────────────────────────────────────────
        secHeader(parent, "⚙️ FONCTIONNALITÉS");
        String[][] feats = {
            {"🏠", "Commandes maison",   "Contrôle domotique par voix ou texte"},
            {"🔗", "Gestion d'appareils","Suivi de tous tes appareils connectés"},
            {"📊", "Hub unifié",          "Une seule interface pour tout gérer"},
            {"🔔", "Alertes unifiées",   "Une seule notif Léa pour tous les canaux"},
            {"🤖", "Réponse intelligente","Léa analyse le contexte de tes messages"},
        };
        LinearLayout featCard = card(dp(12), dp(4), dp(12), dp(24));
        for (int i = 0; i < feats.length; i++) {
            String[] f = feats[i];
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(8), 0, dp(8));
            row.addView(tv(f[0], 18, WHITE, Typeface.NORMAL));
            LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
            t.setPadding(dp(10), 0, 0, 0); t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            t.addView(tv(f[1], 12, CYAN, Typeface.BOLD)); t.addView(tv(f[2], 10, DIM2, Typeface.NORMAL));
            row.addView(t);
            featCard.addView(row);
            if (i < feats.length - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); featCard.addView(sp); }
        }
        parent.addView(featCard);
    }

    private View buildAppList(String[][] apps, String btnLabel, int btnColor) {
        LinearLayout card = card(dp(12), dp(4), dp(12), 0);
        for (int i = 0; i < apps.length; i++) {
            final String[] a = apps[i];
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(8), 0, dp(8));
            row.addView(tv(a[0], 20, WHITE, Typeface.NORMAL));
            LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
            t.setPadding(dp(10), 0, 0, 0); t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            t.addView(tv(a[1], 12, WHITE, Typeface.BOLD));
            row.addView(t);
            Button btn = new Button(this); btn.setText(btnLabel);
            btn.setTextColor(btnColor); btn.setBackgroundColor(CARD2);
            btn.setTextSize(10); btn.setTypeface(null, Typeface.BOLD); btn.setAllCaps(false);
            btn.setPadding(dp(10), dp(4), dp(10), dp(4));
            btn.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(30)));
            btn.setOnClickListener(v -> launchApp(a[2], a[3]));
            row.addView(btn);
            card.addView(row);
            if (i < apps.length - 1) { View sp = new View(this); sp.setBackgroundColor(CARD2);
                sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); card.addView(sp); }
        }
        return card;
    }

    private void showAddDeviceDialog(LeaOmnichannelIntegration omni) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(10), dp(20), dp(10));

        EditText nameEt = new EditText(this); nameEt.setHint("Nom (ex: Mon PC, Galaxy Watch)");
        form.addView(nameEt);

        Spinner typeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, DEVICE_TYPES_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        form.addView(typeSpinner);

        EditText ipEt = new EditText(this); ipEt.setHint("IP (optionnel, ex: 192.168.1.10)");
        form.addView(ipEt);

        new AlertDialog.Builder(this)
            .setTitle("➕ Ajouter un appareil")
            .setView(form)
            .setPositiveButton("Ajouter", (d, w) -> {
                String name = nameEt.getText().toString().trim();
                if (name.isEmpty()) { Toast.makeText(this, "Nom requis", Toast.LENGTH_SHORT).show(); return; }
                String type = DEVICE_TYPES[typeSpinner.getSelectedItemPosition()];
                String ip   = ipEt.getText().toString().trim();
                omni.registerDevice(name, type, ip);
                Toast.makeText(this, "✅ " + name + " enregistré !", Toast.LENGTH_SHORT).show();
                contentArea.removeAllViews();
                buildContent(contentArea);
            })
            .setNegativeButton("Annuler", null).show();
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private void launchApp(String packageName, String fallbackUrl) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
            if (i != null) { startActivity(i); return; }
        } catch (Exception ignored) {}
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
    }

    private LinearLayout card(int ml, int mt, int mr, int mb) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(CARD); c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(ml, mt, mr, mb); c.setLayoutParams(lp); return c;
    }

    private TextView tv(String t, float sp, int c, int s) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(c); v.setTextSize(sp); v.setTypeface(null, s); return v;
    }

    private void secHeader(LinearLayout p, String text) {
        TextView t = tv(text, 11, GOLD, Typeface.BOLD);
        t.setPadding(dp(16), dp(14), dp(16), dp(6)); p.addView(t);
    }
}
