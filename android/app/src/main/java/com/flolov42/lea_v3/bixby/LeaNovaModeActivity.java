package com.flolov42.lea_v3.bixby;

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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Activité overlay enrichie pour le mode Nova de Léa.
 * Intègre : barre de vague, carte résultat, historique des requêtes (déroulant),
 * étoile favoris, corbeille suppression, rétention 30 jours.
 */
public class LeaNovaModeActivity extends Activity {

    public static final int STATE_LISTEN = LeaVoiceActivity.STATE_LISTEN;
    public static final int STATE_THINK  = LeaVoiceActivity.STATE_THINK;
    public static final int STATE_SPEAK  = LeaVoiceActivity.STATE_SPEAK;

    static volatile LeaNovaModeActivity instance;

    private static final int REQ_CAMERA = 88;
    private Uri   cameraPhotoUri;
    private String cameraQuestion;

    private LeaVoiceActivity.WaveBarView waveBar;
    private TextView                      resultCard;
    private TextView                      hintText;
    private final Handler hintCancelHandler = new Handler(Looper.getMainLooper());
    private Runnable hintRunnable;

    private LeaMemoryManager        memory;
    private LeaRecommendationEngine  recommendations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getBooleanExtra("ACTION_KILL", false)) {
            finish(); return;
        }

        instance        = this;
        memory          = new LeaMemoryManager(this);
        recommendations = new LeaRecommendationEngine(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(buildLayout());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) return;
        if (intent.getBooleanExtra("ACTION_KILL", false)) { finish(); return; }
        if (intent.hasExtra("ORB_STATE") && waveBar != null)
            waveBar.setState(intent.getIntExtra("ORB_STATE", STATE_LISTEN));
        if (intent.hasExtra("RESULT_TEXT"))
            showResult(intent.getStringExtra("RESULT_TEXT"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hintCancelHandler.removeCallbacks(hintRunnable);
        if (instance == this) instance = null;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private View buildLayout() {
        float dp = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOnClickListener(v -> closeAndKill());

        // ── BARRE DE VAGUE (ajoutée EN PREMIER = Z le plus bas) ──────────────
        waveBar = new LeaVoiceActivity.WaveBarView(this);
        waveBar.setCloseCallback(this::closeAndKill);
        root.addView(waveBar, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // ── CARTE RÉSULTAT (centre-bas) ───────────────────────────────────────
        resultCard = new TextView(this);
        resultCard.setTextColor(Color.WHITE);
        resultCard.setTextSize(15f);
        resultCard.setTypeface(null, Typeface.BOLD);
        resultCard.setGravity(Gravity.CENTER);
        resultCard.setPadding((int)(20*dp),(int)(14*dp),(int)(20*dp),(int)(14*dp));
        resultCard.setBackgroundColor(0xCC001530);
        resultCard.setVisibility(View.GONE);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity      = Gravity.BOTTOM;
        cardParams.bottomMargin = (int)(165 * dp);
        cardParams.leftMargin   = (int)(20 * dp);
        cardParams.rightMargin  = (int)(20 * dp);
        root.addView(resultCard, cardParams);

        // ── HINT PROACTIF ─────────────────────────────────────────────────────
        hintText = new TextView(this);
        hintText.setTextColor(0xAA00E5FF);
        hintText.setTextSize(12f);
        hintText.setGravity(Gravity.CENTER);
        hintText.setVisibility(View.GONE);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        hintParams.gravity      = Gravity.BOTTOM;
        hintParams.bottomMargin = (int)(140 * dp);
        root.addView(hintText, hintParams);

        // ── BOUTON CAMÉRA (coin haut-droit) ──────────────────────────────────
        Button cameraBtn = new Button(this);
        cameraBtn.setText("📷");
        cameraBtn.setTextSize(22f);
        cameraBtn.setBackgroundColor(0x00000000);
        cameraBtn.setPadding(0, 0, 0, 0);
        cameraBtn.setOnClickListener(v -> launchCamera("Décris ce que tu vois de façon détaillée."));
        FrameLayout.LayoutParams camBtnParams = new FrameLayout.LayoutParams(
            (int)(56 * dp), (int)(56 * dp));
        camBtnParams.gravity     = Gravity.TOP | Gravity.END;
        camBtnParams.topMargin   = (int)(48 * dp);
        camBtnParams.rightMargin = (int)(16 * dp);
        root.addView(cameraBtn, camBtnParams);

        return root;
    }

    // ── API statique appelée par LeaNovaService ────────────────────────────────

    public static void pushState(int state) {
        LeaNovaModeActivity a = instance;
        if (a != null) a.runOnUiThread(() -> {
            if (a.waveBar != null) a.waveBar.setState(state);
        });
    }

    public static void updateResult(String text) {
        LeaNovaModeActivity a = instance;
        if (a != null) a.runOnUiThread(() -> a.showResult(text));
    }

    private void showResult(String text) {
        if (text == null || text.isEmpty()) { resultCard.setVisibility(View.GONE); return; }
        resultCard.setText(text);
        resultCard.setVisibility(View.VISIBLE);
        int sentiment = LeaSentimentDetector.analyze(text);
        memory.remember("", text, sentiment);
        hintCancelHandler.removeCallbacks(hintRunnable);
        hintRunnable = () -> {
            if (isDestroyed() || isFinishing()) return;
            String hint = recommendations.getQuickHint();
            if (!hint.isEmpty() && hintText != null) {
                hintText.setText(hint);
                hintText.setVisibility(View.VISIBLE);
            }
        };
        hintCancelHandler.postDelayed(hintRunnable, 3000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    // ── CAMÉRA VISION (LLaVA) ──────────────────────────────────────────────

    public void launchCamera(String question) {
        try {
            cameraQuestion = question;
            File photoFile = new File(getCacheDir(), "lea_vision_" + System.currentTimeMillis() + ".jpg");
            cameraPhotoUri = FileProvider.getUriForFile(this, "com.flolov42.lea_v3.fileprovider", photoFile);
            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            startActivityForResult(cam, REQ_CAMERA);
        } catch (Exception e) {
            Log.e("LeaNova", "❌ Caméra : " + e.getMessage());
            Toast.makeText(this, "Caméra indisponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CAMERA || resultCode != RESULT_OK || cameraPhotoUri == null) {
            cameraPhotoUri = null;
            return;
        }
        final Uri uri = cameraPhotoUri;
        cameraPhotoUri = null;
        updateResult("📷 Photo prise — pose ta question !");
        // Encode la photo en arrière-plan, puis active le micro pour la question vocale
        new Thread(() -> {
            try {
                Bitmap bmp;
                try (java.io.InputStream stream = getContentResolver().openInputStream(uri)) {
                    if (stream == null) throw new Exception("Stream null pour URI: " + uri);
                    bmp = BitmapFactory.decodeStream(stream);
                } // stream fermé automatiquement
                if (bmp == null) throw new Exception("Bitmap null");
                if (bmp.getWidth() > 640) {
                    int newH = (int)(bmp.getHeight() * (640f / bmp.getWidth()));
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, 640, newH, true);
                    bmp.recycle();
                    bmp = scaled;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                final String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                Log.i("LeaNova", "✅ Photo encodée (" + b64.length() + " chars) — attente question vocale");
                LeaNovaService svc = LeaNovaService.instance;
                if (svc != null) {
                    svc.activateForVisionQuestion(b64);
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "Léa n'est pas connectée", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("LeaNova", "❌ Vision encode : " + e.getMessage());
            }
        }).start();
    }

    private void closeAndKill() {
        LeaNovaService svc = LeaNovaService.instance;
        if (svc != null) svc.abortEverything();
        finish();
        overridePendingTransition(0, 0);
    }
}
