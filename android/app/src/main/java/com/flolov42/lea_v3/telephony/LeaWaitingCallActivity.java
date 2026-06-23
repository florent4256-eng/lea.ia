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
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class LeaWaitingCallActivity extends Activity {

    private String number = "Inconnu";
    private String name = "Inconnu";
    private boolean actionFaite = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            // Si tout se termine, on ferme ce pop-up
            if ("ended".equals(state)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, new IntentFilter("LEA_CALL_STATE_CHANGED"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, new IntentFilter("LEA_CALL_STATE_CHANGED"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        String rawNumber = getIntent().getStringExtra("number");
        number = (rawNumber != null) ? rawNumber : "Inconnu";
        String rawName = getIntent().getStringExtra("name");
        name = (rawName != null) ? rawName : "Inconnu";

        // Fond sombre semi-transparent (on voit qu'on est par-dessus l'appel en cours)
        RelativeLayout root = new RelativeLayout(this);
        root.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.parseColor("#CC000814"));

        // Panneau central
        LinearLayout panneau = new LinearLayout(this);
        panneau.setOrientation(LinearLayout.VERTICAL);
        panneau.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F21A2238"));
        bg.setCornerRadius(60f);
        panneau.setBackground(bg);
        panneau.setPadding(60, 70, 60, 70);
        RelativeLayout.LayoutParams pParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        pParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        pParams.leftMargin = 60;
        pParams.rightMargin = 60;
        panneau.setLayoutParams(pParams);

        TextView titre = new TextView(this);
        titre.setText("Deuxième appel entrant");
        titre.setTextColor(Color.parseColor("#00E5FF"));
        titre.setTextSize(15f);
        titre.setGravity(Gravity.CENTER);
        titre.setShadowLayer(20f, 0f, 0f, Color.parseColor("#00E5FF"));

        TextView nameText = new TextView(this);
        nameText.setText(name.equals("Inconnu") ? number : name);
        nameText.setTextColor(Color.WHITE);
        nameText.setTextSize(32f);
        nameText.setGravity(Gravity.CENTER);
        nameText.setPadding(0, 24, 0, 4);

        TextView numText = new TextView(this);
        numText.setText(number);
        numText.setTextColor(Color.parseColor("#CCDDEE"));
        numText.setTextSize(16f);
        numText.setGravity(Gravity.CENTER);
        numText.setPadding(0, 0, 0, 50);

        panneau.addView(titre);
        panneau.addView(nameText);
        panneau.addView(numText);

        // Bouton 1 : répondre ET mettre l'ancien en attente
        panneau.addView(creerBouton(
                "Répondre + mettre l'autre en attente", "#22C55E",
                () -> repondre(true)));

        // Bouton 2 : répondre ET terminer l'ancien
        panneau.addView(creerBouton(
                "Répondre + terminer l'autre", "#F59E0B",
                () -> repondre(false)));

        // Bouton 3 : refuser le nouvel appel
        panneau.addView(creerBouton(
                "Refuser", "#EF4444",
                this::refuser));

        root.addView(panneau);
        setContentView(root);
    }

    private TextView creerBouton(String texte, String couleur, final Runnable action) {
        TextView btn = new TextView(this);
        btn.setText(texte);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(17f);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(30, 44, 30, 44);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(couleur));
        bg.setCornerRadius(50f);
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 24;
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> action.run());
        return btn;
    }

    /** Trouve le nouvel appel entrant (celui qui sonne). */
    private Call trouverAppelQuiSonne() {
        for (Call c : LeaCallService.activeCalls) {
            if (c.getState() == Call.STATE_RINGING) {
                return c;
            }
        }
        return null;
    }

    /** Trouve l'appel actif en cours (l'ancien). */
    private Call trouverAppelEnCours() {
        for (Call c : LeaCallService.activeCalls) {
            if (c.getState() == Call.STATE_ACTIVE) {
                return c;
            }
        }
        return null;
    }

    private void repondre(boolean mettreEnAttenteLAncien) {
        if (actionFaite) return;
        actionFaite = true;

        Call nouveau = trouverAppelQuiSonne();
        Call ancien = trouverAppelEnCours();

        if (ancien != null) {
            if (mettreEnAttenteLAncien) {
                ancien.hold();
            } else {
                ancien.disconnect();
            }
        }

        if (nouveau != null) {
            nouveau.answer(VideoProfile.STATE_AUDIO_ONLY);
            // On bascule l'écran communication sur le nouvel appel
            Intent activeIntent = new Intent(this, LeaActiveCallActivity.class);
            activeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activeIntent.putExtra("number", number);
            activeIntent.putExtra("name", name);
            startActivity(activeIntent);
        }
        finish();
    }

    private void refuser() {
        if (actionFaite) return;
        actionFaite = true;
        Call nouveau = trouverAppelQuiSonne();
        if (nouveau != null) {
            nouveau.reject(false, null);
        }
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(stateReceiver); } catch (Exception ignored) { }
    }
}