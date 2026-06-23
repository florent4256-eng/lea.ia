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


import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.VideoProfile;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.VideoView;
import android.media.MediaPlayer;

public class LeaIncomingCallActivity extends Activity {

    // Distance (en px) à parcourir avec le doigt pour valider l'action au lâcher.
    private static final float SEUIL_VALIDATION = 220f;

    private String number = "Inconnu";
    private String name = "Inconnu";
    private boolean actionDejaFaite = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if ("active".equals(state) || "ended".equals(state)) {
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
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) keyguardManager.requestDismissKeyguard(this, null);
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

        // 👑 RACINE (Plein écran)
        RelativeLayout rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // 🌌 FOND GALAXIE BLEU NUIT + NÉON
        rootLayout.addView(creerFond());

        // 📱 PARTIE HAUTE (Infos contact centrées en haut)
        LinearLayout topLayout = new LinearLayout(this);
        topLayout.setOrientation(LinearLayout.VERTICAL);
        topLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        RelativeLayout.LayoutParams topParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        topParams.topMargin = 180;
        topLayout.setLayoutParams(topParams);

        TextView titleText = new TextView(this);
        titleText.setText("Appel entrant");
        titleText.setTextColor(Color.parseColor("#00E5FF"));
        titleText.setTextSize(16f);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 40);
        titleText.setShadowLayer(28f, 0f, 0f, Color.parseColor("#00E5FF"));

        TextView nameText = new TextView(this);
        nameText.setText(name.equals("Inconnu") ? number : name);
        nameText.setTextColor(Color.WHITE);
        nameText.setTextSize(46f);
        nameText.setGravity(Gravity.CENTER);
        nameText.setShadowLayer(24f, 0f, 0f, Color.parseColor("#8000E5FF"));

        TextView numberText = new TextView(this);
        numberText.setText("Mobile  " + number);
        numberText.setTextColor(Color.parseColor("#CCDDEE"));
        numberText.setTextSize(18f);
        numberText.setGravity(Gravity.CENTER);
        numberText.setPadding(0, 16, 0, 0);

        topLayout.addView(titleText);
        topLayout.addView(nameText);
        topLayout.addView(numberText);
        rootLayout.addView(topLayout);

        // 📞 PARTIE BASSE : deux boutons ronds (VERT à gauche = répondre, ROUGE à droite = raccrocher)
        LinearLayout bottomLayout = new LinearLayout(this);
        bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
        bottomLayout.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams bottomParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomParams.bottomMargin = 260;
        bottomLayout.setLayoutParams(bottomParams);

        TextView indice = new TextView(this);
        indice.setText("Touche un bouton, ou fais-le glisser puis lâche");
        indice.setTextColor(Color.parseColor("#88AABB"));
        indice.setTextSize(13f);
        indice.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams indiceParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        indiceParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        indiceParams.bottomMargin = 180;
        indice.setLayoutParams(indiceParams);

        LinearLayout acceptBtn = creerBoutonRond("📞", "#22C55E");
        LinearLayout rejectBtn = creerBoutonRond("☎️", "#EF4444");

        // Glisser de partout : on attache le geste sur l'icône ronde (le 1er enfant).
        brancherGlisser(acceptBtn.getChildAt(0), true);
        brancherGlisser(rejectBtn.getChildAt(0), false);

        bottomLayout.addView(acceptBtn);
        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(280, 1));
        bottomLayout.addView(space);
        bottomLayout.addView(rejectBtn);

        rootLayout.addView(bottomLayout);
        rootLayout.addView(indice);

        // ✉️ "Envoyer un message" tout en bas
        TextView messageBtn = new TextView(this);
        messageBtn.setText("Envoyer un message");
        messageBtn.setTextColor(Color.WHITE);
        messageBtn.setTextSize(16f);
        messageBtn.setGravity(Gravity.CENTER);
        messageBtn.setPadding(40, 30, 40, 30);
        RelativeLayout.LayoutParams msgParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        msgParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        msgParams.bottomMargin = 60;
        messageBtn.setLayoutParams(msgParams);
        messageBtn.setOnClickListener(v -> {
            try {
                Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                sms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(sms);
            } catch (Exception ignored) { }
        });
        rootLayout.addView(messageBtn);

        setContentView(rootLayout);
    }

    /** Choisit le fond : photo perso, vidéo perso, ou galaxie par défaut. */
    private View creerFond() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("LeaProtect", MODE_PRIVATE);
            String type = prefs.getString("call_bg_type", "none");
            String uriStr = prefs.getString("call_bg_uri", "");

            if (uriStr != null && !uriStr.isEmpty()) {
                Uri uri = Uri.parse(uriStr);

                if ("photo".equals(type)) {
                    ImageView img = new ImageView(this);
                    img.setLayoutParams(new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setImageURI(uri);
                    // Si l'image n'a pas pu se charger, on retombe sur la galaxie
                    if (img.getDrawable() != null) {
                        return img;
                    }
                } else if ("video".equals(type)) {
                    final VideoView video = new VideoView(this);
                    RelativeLayout.LayoutParams vParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    video.setLayoutParams(vParams);
                    video.setVideoURI(uri);
                    video.setOnPreparedListener(mp -> {
                        mp.setLooping(true);
                        mp.setVolume(0f, 0f); // pas de son sur le fond
                        video.start();
                    });
                    video.setOnErrorListener((mp, what, extra) -> true); // ignore l'erreur silencieusement
                    video.start();

                    // On enveloppe la vidéo dans un conteneur pour garder le même type de retour
                    RelativeLayout wrap = new RelativeLayout(this);
                    wrap.setLayoutParams(new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                    wrap.setBackgroundColor(android.graphics.Color.BLACK);
                    wrap.addView(video);
                    return wrap;
                }
            }
        } catch (Exception ignored) { }

        // Par défaut : le fond galaxie
        return creerFondGalaxie();
    }

    // 🌌 Fond dégradé "galaxie" bleu nuit avec halos néon
    private RelativeLayout creerFondGalaxie() {
        RelativeLayout fond = new RelativeLayout(this);
        fond.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // Dégradé de base : bleu nuit profond -> noir
        ImageView base = new ImageView(this);
        base.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        base.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#0B1026"), Color.parseColor("#05030F"), Color.parseColor("#000000")}));
        fond.addView(base);

        // Halo néon cyan (haut)
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

        // Halo néon violet (bas)
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

    private LinearLayout creerBoutonRond(String icon, String colorHex) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(35f);
        iconView.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor(colorHex));
        iconView.setBackground(circle);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(220, 220));

        layout.addView(iconView);
        return layout;
    }

    // 🎯 Glisser de partout : on bouge l'icône avec le doigt ; au LÂCHER, si on a assez bougé -> action.
    private void brancherGlisser(final View icone, final boolean estAccepter) {
        icone.setOnTouchListener(new View.OnTouchListener() {
            float departX, departY, baseTX, baseTY;
            boolean aBouge = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        departX = event.getRawX();
                        departY = event.getRawY();
                        baseTX = v.getTranslationX();
                        baseTY = v.getTranslationY();
                        aBouge = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - departX;
                        float dy = event.getRawY() - departY;
                        v.setTranslationX(baseTX + dx);
                        v.setTranslationY(baseTY + dy);
                        if (Math.abs(dx) > 24 || Math.abs(dy) > 24) aBouge = true;
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float totalDx = event.getRawX() - departX;
                        float totalDy = event.getRawY() - departY;
                        double distance = Math.hypot(totalDx, totalDy);

                        if (!aBouge) {
                            // Simple tap -> action directe
                            declencher(estAccepter);
                        } else if (distance >= SEUIL_VALIDATION) {
                            // Glissé assez loin puis lâché -> action
                            declencher(estAccepter);
                        } else {
                            // Pas assez loin -> l'icône revient à sa place
                            retourMaison(v);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void retourMaison(View v) {
        ValueAnimator ax = ValueAnimator.ofFloat(v.getTranslationX(), 0f);
        ax.setDuration(220);
        ax.addUpdateListener(a -> v.setTranslationX((float) a.getAnimatedValue()));
        ax.start();
        ValueAnimator ay = ValueAnimator.ofFloat(v.getTranslationY(), 0f);
        ay.setDuration(220);
        ay.addUpdateListener(a -> v.setTranslationY((float) a.getAnimatedValue()));
        ay.start();
    }

    private void declencher(boolean estAccepter) {
        if (actionDejaFaite) return;
        actionDejaFaite = true;
        if (estAccepter) {
            accepterAppel();
        } else {
            refuserAppel();
        }
    }

    private void accepterAppel() {
        if (LeaCallService.instance != null && LeaCallService.instance.currentCall != null) {
            LeaCallService.instance.currentCall.answer(VideoProfile.STATE_AUDIO_ONLY);
            Intent activeIntent = new Intent(this, LeaActiveCallActivity.class);
            activeIntent.putExtra("number", number);
            activeIntent.putExtra("name", name);
            activeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(activeIntent);
        }
        finish();
    }

    private void refuserAppel() {
        if (LeaCallService.instance != null && LeaCallService.instance.currentCall != null) {
            LeaCallService.instance.currentCall.reject(false, null);
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