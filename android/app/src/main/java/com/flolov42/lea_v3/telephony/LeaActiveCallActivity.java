package com.flolov42.lea_v3.telephony;

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


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LeaActiveCallActivity extends Activity {

    private TextView timerText;
    private long startTime = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isOnHold = false;

    private TextView muteIconView;
    private TextView speakerIconView;
    private TextView btToggleIconView;
    private TextView pauseBanner;
    private TextView nameText;

    private String number = "Inconnu";
    private String name = "Inconnu";

    // Conteneurs superposés (grille principale / clavier)
    private RelativeLayout rootLayout;
    private LinearLayout gridLayout;
    private View keypadOverlay;
    private View moreOverlay;

    private final BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if ("ended".equals(state)) {
                // On ne ferme QUE s'il ne reste réellement plus aucun appel
                if (LeaCallService.activeCalls == null || LeaCallService.activeCalls.isEmpty()) {
                    terminerEcran();
                } else {
                    rafraichirTitre();
                }
            } else if ("active".equals(state)) {
                rafraichirTitre();
            }
        }
    };

    private void rafraichirTitre() {
        if (nameText != null) {
            nameText.setText(calculerTitreAppels());
        }
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            timerText.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callStateReceiver, new IntentFilter("LEA_CALL_STATE_CHANGED"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(callStateReceiver, new IntentFilter("LEA_CALL_STATE_CHANGED"));
        }

        String rawNumber = getIntent().getStringExtra("number");
        number = (rawNumber != null) ? rawNumber : "Inconnu";
        String rawName = getIntent().getStringExtra("name");
        name = (rawName != null) ? rawName : "Inconnu";

        rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // 🌌 FOND GALAXIE (identique à l'écran entrant)
        rootLayout.addView(creerFondGalaxie());

        // PARTIE HAUTE : nom + chrono
        LinearLayout topLayout = new LinearLayout(this);
        topLayout.setOrientation(LinearLayout.VERTICAL);
        topLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        RelativeLayout.LayoutParams topParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        topParams.topMargin = 150;
        topLayout.setLayoutParams(topParams);

        nameText = new TextView(this);
        nameText.setText(name.equals("Inconnu") ? number : name);
        nameText.setTextColor(Color.WHITE);
        nameText.setTextSize(42f);
        nameText.setGravity(Gravity.CENTER);
        nameText.setShadowLayer(24f, 0f, 0f, Color.parseColor("#8000E5FF"));

        timerText = new TextView(this);
        timerText.setText("00:00");
        timerText.setTextColor(Color.parseColor("#00E5FF"));
        timerText.setTextSize(20f);
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(0, 20, 0, 0);
        timerText.setShadowLayer(20f, 0f, 0f, Color.parseColor("#00E5FF"));

        topLayout.addView(nameText);
        topLayout.addView(timerText);
        rootLayout.addView(topLayout);

        // GRILLE 3x2
        gridLayout = construireGrille();
        rootLayout.addView(gridLayout);

        // BOUTON RACCROCHER
        rootLayout.addView(construireBoutonRaccrocher());

        // BANDEAU "APPEL EN PAUSE" (caché par défaut)
        pauseBanner = new TextView(this);
        pauseBanner.setText("⏸  Appel en pause");
        pauseBanner.setTextColor(Color.WHITE);
        pauseBanner.setTextSize(15f);
        pauseBanner.setGravity(Gravity.CENTER);
        pauseBanner.setPadding(40, 22, 40, 22);
        GradientDrawable pauseBg = new GradientDrawable();
        pauseBg.setColor(Color.parseColor("#CCF59E0B"));
        pauseBg.setCornerRadius(50f);
        pauseBanner.setBackground(pauseBg);
        RelativeLayout.LayoutParams pauseParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        pauseParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        pauseParams.topMargin = 380;
        pauseBanner.setLayoutParams(pauseParams);
        pauseBanner.setVisibility(View.GONE);
        rootLayout.addView(pauseBanner);

        setContentView(rootLayout);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    // ====================== GRILLE PRINCIPALE ======================

    private LinearLayout construireGrille() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams gridParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        gridParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        grid.setLayoutParams(gridParams);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        row1.setPadding(0, 0, 0, 80);

        LinearLayout btnAdd = createCallButton("Ajouter un appel", "➕", false);
        LinearLayout btnMute = createCallButton("Muet", "🎤", false);
        LinearLayout btnBluetooth = createCallButton("Bluetooth", "᭤", false);
        muteIconView = (TextView) btnMute.getChildAt(0);
        btToggleIconView = (TextView) btnBluetooth.getChildAt(0);

        row1.addView(btnAdd);
        row1.addView(btnMute);
        row1.addView(btnBluetooth);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);

        LinearLayout btnSpeaker = createCallButton("HP", "🔊", false);
        LinearLayout btnKeypad = createCallButton("Clavier", "⌨", false);
        LinearLayout btnMore = createCallButton("Plus", "⋮", false);
        speakerIconView = (TextView) btnSpeaker.getChildAt(0);

        row2.addView(btnSpeaker);
        row2.addView(btnKeypad);
        row2.addView(btnMore);

        grid.addView(row1);
        grid.addView(row2);

        // BRANCHEMENTS
        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            updateButtonState(muteIconView, isMuted);
            if (LeaCallService.instance != null) LeaCallService.instance.setMuted(isMuted);
        });

        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            updateButtonState(speakerIconView, isSpeakerOn);
            if (LeaCallService.instance != null) {
                LeaCallService.instance.setAudioRoute(isSpeakerOn
                        ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE);
            }
        });

        btnBluetooth.setOnClickListener(v -> ouvrirSelecteurBluetooth());
        btnAdd.setOnClickListener(v -> ajouterUnAppel());
        btnKeypad.setOnClickListener(v -> afficherClavier());
        btnMore.setOnClickListener(v -> afficherMenuPlus());

        return grid;
    }

    private LinearLayout createCallButton(String label, String iconSymbol, boolean isActive) {
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnLayout.setLayoutParams(params);

        TextView iconView = new TextView(this);
        iconView.setText(iconSymbol);
        iconView.setTextSize(30f);
        iconView.setTextColor(isActive ? Color.BLACK : Color.WHITE);
        iconView.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(isActive ? Color.WHITE : Color.parseColor("#26FFFFFF"));
        iconView.setBackground(circle);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(180, 180));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(13f);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, 20, 0, 0);

        btnLayout.addView(iconView);
        btnLayout.addView(labelView);
        return btnLayout;
    }

    private void updateButtonState(TextView iconView, boolean isActive) {
        iconView.setTextColor(isActive ? Color.BLACK : Color.WHITE);
        GradientDrawable circle = (GradientDrawable) iconView.getBackground();
        circle.setColor(isActive ? Color.WHITE : Color.parseColor("#26FFFFFF"));
    }

    private RelativeLayout construireBoutonRaccrocher() {
        TextView endBtn = new TextView(this);
        endBtn.setText("☎");
        endBtn.setTextSize(34f);
        endBtn.setTextColor(Color.WHITE);
        endBtn.setGravity(Gravity.CENTER);
        GradientDrawable redBg = new GradientDrawable();
        redBg.setShape(GradientDrawable.OVAL);
        redBg.setColor(Color.parseColor("#EF4444"));
        endBtn.setBackground(redBg);

        RelativeLayout holder = new RelativeLayout(this);
        RelativeLayout.LayoutParams holderParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        holderParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        holderParams.bottomMargin = 150;
        holder.setLayoutParams(holderParams);

        RelativeLayout.LayoutParams endParams = new RelativeLayout.LayoutParams(220, 220);
        endParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        endBtn.setLayoutParams(endParams);
        holder.addView(endBtn);

        endBtn.setOnClickListener(v -> raccrocher());
        return holder;
    }

    // ====================== CLAVIER (DTMF) ======================

    private void afficherClavier() {
        if (keypadOverlay != null) return;

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A2238"));
        bg.setCornerRadius(60f);
        overlay.setBackground(bg);
        overlay.setPadding(40, 50, 40, 50);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        params.leftMargin = 40;
        params.rightMargin = 40;
        overlay.setLayoutParams(params);

        String[][] touches = {
                {"1", "", "2", "ABC", "3", "DEF"},
                {"4", "GHI", "5", "JKL", "6", "MNO"},
                {"7", "PQRS", "8", "TUV", "9", "WXYZ"},
                {"*", "", "0", "+", "#", ""}
        };

        for (String[] ligne : touches) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int i = 0; i < ligne.length; i += 2) {
                row.addView(creerToucheClavier(ligne[i], ligne[i + 1]));
            }
            overlay.addView(row);
        }

        // Bas : HP / Masquer / Muet
        LinearLayout barreBas = new LinearLayout(this);
        barreBas.setOrientation(LinearLayout.HORIZONTAL);
        barreBas.setGravity(Gravity.CENTER);
        barreBas.setPadding(0, 40, 0, 0);

        LinearLayout hp = createCallButton("HP", "🔊", isSpeakerOn);
        LinearLayout masquer = createCallButton("Masquer", "⌨", false);
        LinearLayout muet = createCallButton("Muet", "🎤", isMuted);

        hp.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            updateButtonState((TextView) hp.getChildAt(0), isSpeakerOn);
            updateButtonState(speakerIconView, isSpeakerOn);
            if (LeaCallService.instance != null) {
                LeaCallService.instance.setAudioRoute(isSpeakerOn
                        ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE);
            }
        });
        masquer.setOnClickListener(v -> fermerClavier());
        muet.setOnClickListener(v -> {
            isMuted = !isMuted;
            updateButtonState((TextView) muet.getChildAt(0), isMuted);
            updateButtonState(muteIconView, isMuted);
            if (LeaCallService.instance != null) LeaCallService.instance.setMuted(isMuted);
        });

        barreBas.addView(hp);
        barreBas.addView(masquer);
        barreBas.addView(muet);
        overlay.addView(barreBas);

        keypadOverlay = overlay;
        gridLayout.setVisibility(View.GONE);
        rootLayout.addView(overlay);
    }

    private LinearLayout creerToucheClavier(final String chiffre, String lettres) {
        LinearLayout touche = new LinearLayout(this);
        touche.setOrientation(LinearLayout.VERTICAL);
        touche.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        params.topMargin = 18;
        params.bottomMargin = 18;
        touche.setLayoutParams(params);

        TextView num = new TextView(this);
        num.setText(chiffre);
        num.setTextColor(Color.WHITE);
        num.setTextSize(34f);
        num.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText(lettres);
        sub.setTextColor(Color.parseColor("#99FFFFFF"));
        sub.setTextSize(12f);
        sub.setGravity(Gravity.CENTER);
        sub.setMinHeight(24);

        touche.addView(num);
        touche.addView(sub);

        touche.setOnClickListener(v -> envoyerDtmf(chiffre));
        return touche;
    }

    private void envoyerDtmf(String chiffre) {
        if (LeaCallService.instance == null || LeaCallService.instance.currentCall == null) return;
        if (chiffre == null || chiffre.isEmpty()) return;
        char c = chiffre.charAt(0);
        Call call = LeaCallService.instance.currentCall;
        call.playDtmfTone(c);
        call.stopDtmfTone();
    }

    private void fermerClavier() {
        if (keypadOverlay != null) {
            rootLayout.removeView(keypadOverlay);
            keypadOverlay = null;
            gridLayout.setVisibility(View.VISIBLE);
        }
    }

    // ====================== MENU PLUS ======================

    private void afficherMenuPlus() {
        if (moreOverlay != null) return;

        // Fond cliquable pour fermer
        RelativeLayout scrim = new RelativeLayout(this);
        scrim.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        scrim.setBackgroundColor(Color.parseColor("#66000000"));
        scrim.setOnClickListener(v -> fermerMenuPlus());

        LinearLayout panneau = new LinearLayout(this);
        panneau.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F2202A3D"));
        bg.setCornerRadius(55f);
        panneau.setBackground(bg);
        panneau.setPadding(20, 30, 20, 30);
        RelativeLayout.LayoutParams pParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        pParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        pParams.bottomMargin = 420;
        pParams.leftMargin = 50;
        pParams.rightMargin = 50;
        panneau.setLayoutParams(pParams);

        if (LeaCallService.activeCalls.size() >= 2) {
            panneau.addView(creerLigneMenu("⇄", "Fusionner les appels (conférence)", () -> {
                LeaCallService.instance.fusionnerEnConference();
                Toast.makeText(this, "Appels fusionnés", Toast.LENGTH_SHORT).show();
                fermerMenuPlus();
            }));
        }

        if (trouverConference() != null) {
            panneau.addView(creerLigneMenu("☷", "Gérer la conférence", () -> {
                fermerMenuPlus();
                afficherGestionConference();
            }));
        }

        panneau.addView(creerLigneMenu(isOnHold ? "▶" : "II", isOnHold ? "Reprendre l'appel" : "Mettre l'appel en attente", () -> {
            basculerAttente();
            fermerMenuPlus();
        }));
        panneau.addView(creerLigneMenu("☺", "Afficher contact", () -> {
            afficherContact();
            fermerMenuPlus();
        }));
        panneau.addView(creerLigneMenu("✈", "Envoyer un message", () -> {
            envoyerMessage();
            fermerMenuPlus();
        }));

        scrim.addView(panneau);
        moreOverlay = scrim;
        rootLayout.addView(scrim);
    }

    private LinearLayout creerLigneMenu(String icone, String texte, final Runnable action) {
        LinearLayout ligne = new LinearLayout(this);
        ligne.setOrientation(LinearLayout.HORIZONTAL);
        ligne.setGravity(Gravity.CENTER_VERTICAL);
        ligne.setPadding(30, 40, 30, 40);

        TextView ic = new TextView(this);
        ic.setText(icone);
        ic.setTextColor(Color.WHITE);
        ic.setTextSize(20f);
        ic.setGravity(Gravity.CENTER);
        ic.setWidth(110);

        TextView tx = new TextView(this);
        tx.setText(texte);
        tx.setTextColor(Color.WHITE);
        tx.setTextSize(18f);
        tx.setPadding(30, 0, 0, 0);

        ligne.addView(ic);
        ligne.addView(tx);
        ligne.setOnClickListener(v -> action.run());
        return ligne;
    }

    private void fermerMenuPlus() {
        if (moreOverlay != null) {
            rootLayout.removeView(moreOverlay);
            moreOverlay = null;
        }
    }

    /** Trouve l'appel-conférence (celui qui a des enfants), s'il existe. */
    private Call trouverConference() {
        if (LeaCallService.activeCalls == null) return null;
        for (Call c : LeaCallService.activeCalls) {
            if (c.getChildren() != null && c.getChildren().size() >= 2) {
                return c;
            }
        }
        return null;
    }

    private void afficherGestionConference() {
        if (moreOverlay != null) return;
        final Call conf = trouverConference();
        if (conf == null) {
            Toast.makeText(this, "Aucune conférence active", Toast.LENGTH_SHORT).show();
            return;
        }

        RelativeLayout scrim = new RelativeLayout(this);
        scrim.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        scrim.setBackgroundColor(Color.parseColor("#66000000"));
        scrim.setOnClickListener(v -> fermerMenuPlus());

        ScrollView scroll = new ScrollView(this);
        RelativeLayout.LayoutParams sParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        sParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        sParams.leftMargin = 40;
        sParams.rightMargin = 40;
        scroll.setLayoutParams(sParams);

        LinearLayout panneau = new LinearLayout(this);
        panneau.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F2202A3D"));
        bg.setCornerRadius(55f);
        panneau.setBackground(bg);
        panneau.setPadding(20, 30, 20, 30);

        TextView titre = new TextView(this);
        titre.setText("Gérer la conférence");
        titre.setTextColor(Color.parseColor("#00E5FF"));
        titre.setTextSize(16f);
        titre.setPadding(40, 10, 0, 20);
        panneau.addView(titre);

        for (final Call participant : conf.getChildren()) {
            String nomP = nomDepuisCall(participant);

            TextView nomView = new TextView(this);
            nomView.setText(nomP);
            nomView.setTextColor(Color.WHITE);
            nomView.setTextSize(17f);
            nomView.setPadding(40, 30, 40, 10);
            panneau.addView(nomView);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(30, 0, 30, 30);

            TextView btnAttente = new TextView(this);
            btnAttente.setText("Mettre en attente");
            btnAttente.setTextColor(Color.WHITE);
            btnAttente.setTextSize(14f);
            btnAttente.setGravity(Gravity.CENTER);
            btnAttente.setPadding(30, 26, 30, 26);
            GradientDrawable bgA = new GradientDrawable();
            bgA.setColor(Color.parseColor("#F59E0B"));
            bgA.setCornerRadius(40f);
            btnAttente.setBackground(bgA);
            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpA.rightMargin = 16;
            btnAttente.setLayoutParams(lpA);
            btnAttente.setOnClickListener(v -> {
                LeaCallService.instance.separerEtMettreEnAttente(participant);
                fermerMenuPlus();
                rafraichirTitre();
            });

            TextView btnVirer = new TextView(this);
            btnVirer.setText("Virer");
            btnVirer.setTextColor(Color.WHITE);
            btnVirer.setTextSize(14f);
            btnVirer.setGravity(Gravity.CENTER);
            btnVirer.setPadding(30, 26, 30, 26);
            GradientDrawable bgV = new GradientDrawable();
            bgV.setColor(Color.parseColor("#EF4444"));
            bgV.setCornerRadius(40f);
            btnVirer.setBackground(bgV);
            LinearLayout.LayoutParams lpV = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            btnVirer.setLayoutParams(lpV);
            btnVirer.setOnClickListener(v -> {
                LeaCallService.instance.virerParticipant(participant);
                fermerMenuPlus();
                rafraichirTitre();
            });

            actions.addView(btnAttente);
            actions.addView(btnVirer);
            panneau.addView(actions);
        }

        scroll.addView(panneau);
        scrim.addView(scroll);
        moreOverlay = scrim;
        rootLayout.addView(scrim);
    }

    // ====================== ACTIONS RÉELLES ======================

    private void basculerAttente() {
        if (LeaCallService.instance == null || LeaCallService.instance.currentCall == null) return;
        Call call = LeaCallService.instance.currentCall;
        if (isOnHold) {
            call.unhold();
            isOnHold = false;
            pauseBanner.setVisibility(View.GONE);
            Toast.makeText(this, "Appel repris", Toast.LENGTH_SHORT).show();
        } else {
            call.hold();
            isOnHold = true;
            pauseBanner.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Appel en attente", Toast.LENGTH_SHORT).show();
        }
    }

    private void afficherContact() {
        try {
            Uri lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            android.database.Cursor c = getContentResolver().query(
                    lookupUri,
                    new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY},
                    null, null, null);

            if (c != null && c.moveToFirst()) {
                long id = c.getLong(0);
                String lookupKey = c.getString(1);
                c.close();
                Uri contactUri = ContactsContract.Contacts.getLookupUri(id, lookupKey);
                Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                if (c != null) c.close();
                // Contact inconnu : on propose de l'ajouter
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir le contact", Toast.LENGTH_SHORT).show();
        }
    }

    private void envoyerMessage() {
        try {
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
            sms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sms);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir les messages", Toast.LENGTH_SHORT).show();
        }
    }

    private void ajouterUnAppel() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, CODE_CHOIX_CONTACT);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir les contacts", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int CODE_CHOIX_CONTACT = 7001;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_CHOIX_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri == null) return;
            String numeroChoisi = recupererNumero(contactUri);
            if (numeroChoisi != null) {
                lancerDeuxiemeAppel(numeroChoisi);
            } else {
                Toast.makeText(this, "Numéro introuvable pour ce contact", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @android.annotation.SuppressLint("Range")
    private String recupererNumero(Uri contactUri) {
        try {
            android.database.Cursor c = getContentResolver().query(contactUri, null, null, null, null);
            if (c == null) return null;
            String numero = null;
            if (c.moveToFirst()) {
                int idxHasNumber = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                int idxId = c.getColumnIndex(ContactsContract.Contacts._ID);
                String id = c.getString(idxId);
                if (Integer.parseInt(c.getString(idxHasNumber)) > 0) {
                    android.database.Cursor pc = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    if (pc != null) {
                        if (pc.moveToFirst()) {
                            numero = pc.getString(pc.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                        }
                        pc.close();
                    }
                }
            }
            c.close();
            return numero;
        } catch (Exception e) {
            return null;
        }
    }

    private void lancerDeuxiemeAppel(final String numero) {
        if (LeaCallService.instance == null) return;
        int nbSim = LeaCallService.instance.nombreDeSim();

        if (nbSim >= 2) {
            // Double SIM : on demande quelle SIM utiliser
            RelativeLayout scrim = new RelativeLayout(this);
            scrim.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            scrim.setBackgroundColor(Color.parseColor("#66000000"));
            scrim.setOnClickListener(v -> fermerMenuPlus());

            LinearLayout panneau = new LinearLayout(this);
            panneau.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#F2202A3D"));
            bg.setCornerRadius(55f);
            panneau.setBackground(bg);
            panneau.setPadding(20, 30, 20, 30);
            RelativeLayout.LayoutParams pParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            pParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            pParams.leftMargin = 50;
            pParams.rightMargin = 50;
            panneau.setLayoutParams(pParams);

            TextView titre = new TextView(this);
            titre.setText("Appeler avec quelle SIM ?");
            titre.setTextColor(Color.parseColor("#00E5FF"));
            titre.setTextSize(16f);
            titre.setPadding(40, 10, 0, 20);
            panneau.addView(titre);

            panneau.addView(creerLigneMenu("①", "SIM 1", () -> {
                LeaCallService.instance.placerNouvelAppel(numero, 0);
                fermerMenuPlus();
            }));
            panneau.addView(creerLigneMenu("②", "SIM 2", () -> {
                LeaCallService.instance.placerNouvelAppel(numero, 1);
                fermerMenuPlus();
            }));

            scrim.addView(panneau);
            moreOverlay = scrim;
            rootLayout.addView(scrim);
        } else {
            // Mono SIM : on appelle directement
            LeaCallService.instance.placerNouvelAppel(numero, -1);
        }
    }

    // ====================== BLUETOOTH DYNAMIQUE ======================

    private void ouvrirSelecteurBluetooth() {
        if (LeaCallService.instance == null) {
            Toast.makeText(this, "Service indisponible", Toast.LENGTH_SHORT).show();
            return;
        }
        CallAudioState audioState = LeaCallService.instance.getCallAudioState();
        if (audioState == null) {
            Toast.makeText(this, "Audio indisponible", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();
        int routeMask = audioState.getSupportedRouteMask();

        // Écouteur / oreille
        if ((routeMask & CallAudioState.ROUTE_EARPIECE) != 0) {
            labels.add("Téléphone");
            actions.add(() -> {
                isSpeakerOn = false;
                updateButtonState(speakerIconView, false);
                LeaCallService.instance.setAudioRoute(CallAudioState.ROUTE_EARPIECE);
            });
        }
        // Haut-parleur
        if ((routeMask & CallAudioState.ROUTE_SPEAKER) != 0) {
            labels.add("Haut-parleur");
            actions.add(() -> {
                isSpeakerOn = true;
                updateButtonState(speakerIconView, true);
                LeaCallService.instance.setAudioRoute(CallAudioState.ROUTE_SPEAKER);
            });
        }
        // Casque filaire
        if ((routeMask & CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
            labels.add("Casque filaire");
            actions.add(() -> LeaCallService.instance.setAudioRoute(CallAudioState.ROUTE_WIRED_HEADSET));
        }
        // Appareils Bluetooth réels (écouteurs, montre…)
        if ((routeMask & CallAudioState.ROUTE_BLUETOOTH) != 0) {
            ajouterAppareilsBluetooth(audioState, labels, actions);
        }

        if (labels.isEmpty()) {
            Toast.makeText(this, "Aucune sortie audio disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        afficherListeAudio(labels, actions);
    }

    private void ajouterAppareilsBluetooth(CallAudioState audioState, List<String> labels, List<Runnable> actions) {
        boolean ajouteParDevice = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                java.util.Collection<BluetoothDevice> devices = audioState.getSupportedBluetoothDevices();
                if (devices != null) {
                    for (final BluetoothDevice device : devices) {
                        String nom = nomBluetoothSecurise(device);
                        labels.add(nom);
                        actions.add(() -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                LeaCallService.instance.requestBluetoothAudio(device);
                            } else {
                                LeaCallService.instance.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH);
                            }
                        });
                        ajouteParDevice = true;
                    }
                }
            } catch (SecurityException ignored) {
                // permission BLUETOOTH_CONNECT manquante : on retombe sur la route générique
            }
        }
        if (!ajouteParDevice) {
            labels.add("Bluetooth");
            actions.add(() -> LeaCallService.instance.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH));
        }
    }

    private String nomBluetoothSecurise(BluetoothDevice device) {
        try {
            String n = device.getName();
            if (n != null && !n.isEmpty()) return n;
        } catch (SecurityException ignored) { }
        return "Appareil Bluetooth";
    }

    private void afficherListeAudio(final List<String> labels, final List<Runnable> actions) {
        if (moreOverlay != null) return;

        RelativeLayout scrim = new RelativeLayout(this);
        scrim.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        scrim.setBackgroundColor(Color.parseColor("#66000000"));
        scrim.setOnClickListener(v -> fermerMenuPlus());

        ScrollView scroll = new ScrollView(this);
        RelativeLayout.LayoutParams sParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        sParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        sParams.leftMargin = 50;
        sParams.rightMargin = 50;
        scroll.setLayoutParams(sParams);

        LinearLayout panneau = new LinearLayout(this);
        panneau.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F2202A3D"));
        bg.setCornerRadius(55f);
        panneau.setBackground(bg);
        panneau.setPadding(20, 30, 20, 30);

        TextView titre = new TextView(this);
        titre.setText("Sortie audio");
        titre.setTextColor(Color.parseColor("#00E5FF"));
        titre.setTextSize(16f);
        titre.setPadding(40, 10, 0, 20);
        panneau.addView(titre);

        for (int i = 0; i < labels.size(); i++) {
            final Runnable action = actions.get(i);
            panneau.addView(creerLigneMenu("🔊", labels.get(i), () -> {
                action.run();
                fermerMenuPlus();
            }));
        }

        scroll.addView(panneau);
        scrim.addView(scroll);
        moreOverlay = scrim;
        rootLayout.addView(scrim);
    }

    // ====================== FOND GALAXIE ======================

    private RelativeLayout creerFondGalaxie() {
        RelativeLayout fond = new RelativeLayout(this);
        fond.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        ImageView base = new ImageView(this);
        base.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        base.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#0B1026"), Color.parseColor("#05030F"), Color.parseColor("#000000")}));
        fond.addView(base);

        ImageView haloCyan = new ImageView(this);
        RelativeLayout.LayoutParams cyanParams = new RelativeLayout.LayoutParams(900, 900);
        cyanParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        cyanParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        cyanParams.topMargin = -260;
        haloCyan.setLayoutParams(cyanParams);
        GradientDrawable cyanGlow = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#3300E5FF"), Color.parseColor("#0000E5FF")});
        cyanGlow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        cyanGlow.setGradientRadius(450f);
        cyanGlow.setShape(GradientDrawable.OVAL);
        haloCyan.setBackground(cyanGlow);
        fond.addView(haloCyan);

        ImageView haloViolet = new ImageView(this);
        RelativeLayout.LayoutParams violetParams = new RelativeLayout.LayoutParams(1000, 1000);
        violetParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        violetParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        violetParams.bottomMargin = -300;
        violetParams.rightMargin = -200;
        haloViolet.setLayoutParams(violetParams);
        GradientDrawable violetGlow = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#33B026FF"), Color.parseColor("#00B026FF")});
        violetGlow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        violetGlow.setGradientRadius(500f);
        violetGlow.setShape(GradientDrawable.OVAL);
        haloViolet.setBackground(violetGlow);
        fond.addView(haloViolet);

        return fond;
    }

    // ====================== FIN D'APPEL ======================

    private void raccrocher() {
        if (LeaCallService.activeCalls != null) {
            // Copie pour éviter les soucis pendant qu'on modifie la liste
            for (Call c : new ArrayList<>(LeaCallService.activeCalls)) {
                try { c.disconnect(); } catch (Exception ignored) { }
            }
        }
        terminerEcran();
    }

    private void terminerEcran() {
        timerText.setText("Fin d'appel");
        timerText.setTextColor(Color.parseColor("#EF4444"));
        timerHandler.removeCallbacks(timerRunnable);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1200);
    }

    /** Calcule le texte du haut selon les appels réellement en cours. */
    private String calculerTitreAppels() {
        List<Call> appels = LeaCallService.activeCalls;
        if (appels == null || appels.isEmpty()) {
            return name.equals("Inconnu") ? number : name;
        }
        // Conférence ? (seulement si au moins 2 participants réels)
        for (Call c : appels) {
            if (c.getChildren() != null && c.getChildren().size() >= 2) {
                return "Conférence (" + c.getChildren().size() + ")";
            }
        }
        // Sinon, on liste les noms
        ArrayList<String> noms = new ArrayList<>();
        for (Call c : appels) {
            noms.add(nomDepuisCall(c));
        }
        if (noms.size() == 1) return noms.get(0);
        return android.text.TextUtils.join(" & ", noms);
    }

    private String nomDepuisCall(Call c) {
        try {
            if (c.getDetails() != null && c.getDetails().getHandle() != null) {
                String num = c.getDetails().getHandle().getSchemeSpecificPart();
                String n = LeaCallService.instance != null ? nomContact(num) : num;
                return (n == null || n.isEmpty()) ? num : n;
            }
        } catch (Exception ignored) { }
        return "Inconnu";
    }

    @android.annotation.SuppressLint("Range")
    private String nomContact(String numero) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero));
            android.database.Cursor cur = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cur != null) {
                String r = numero;
                if (cur.moveToFirst()) r = cur.getString(0);
                cur.close();
                return r;
            }
        } catch (Exception ignored) { }
        return numero;
    }

    @Override
    protected void onResume() {
        super.onResume();
        rafraichirTitre();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        try { unregisterReceiver(callStateReceiver); } catch (Exception ignored) { }
    }
}