package com.flolov42.lea_v3.bixby;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaImageViewerActivity extends Activity {

    public static final String EXTRA_URL = "image_url";

    private Bitmap loadedBitmap = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isExpanded = false;
    private FrameLayout card;
    private GradientDrawable cardBg;
    private TextView expandBtn;
    private TextView dlBtn;
    private int screenW, screenH;
    private int dp2, dp4, dp8, dp16, dp36;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Affiche par-dessus l'écran verrouillé
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        String imageUrl = getIntent().getStringExtra(EXTRA_URL);
        if (imageUrl == null || imageUrl.isEmpty()) { finish(); return; }

        // Modèle 3D → déléguer au viewer SceneView natif
        if (imageUrl.endsWith(".glb") || imageUrl.endsWith(".obj") || imageUrl.endsWith(".gltf")) {
            android.content.Intent intent3d = new android.content.Intent(this, LeaModelViewerActivity.class);
            intent3d.putExtra(LeaModelViewerActivity.EXTRA_URL, imageUrl);
            startActivity(intent3d);
            finish();
            return;
        }

        float d = getResources().getDisplayMetrics().density;
        dp2  = (int)(2  * d);
        dp4  = (int)(4  * d);
        dp8  = (int)(8  * d);
        dp16 = (int)(16 * d);
        dp36 = (int)(36 * d);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;

        // ── OVERLAY SOMBRE ────────────────────────────────────────────
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#B3000000")); // noir ~70%
        root.setOnClickListener(v -> finish()); // clic overlay = ferme

        // ── CARD CENTREE ──────────────────────────────────────────────
        card = new FrameLayout(this);
        card.setClickable(true); // bloque la propagation vers l'overlay
        cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setColor(Color.parseColor("#CC0F0A1E")); // rgba(15,10,30,~80%)
        cardBg.setCornerRadius(dp16 * 2f);
        cardBg.setStroke(dp2 / 2, Color.parseColor("#33FFFFFF")); // border white/20
        card.setBackground(cardBg);

        FrameLayout.LayoutParams cardLp = cardNormalLp();
        root.addView(card, cardLp);

        // ── CONTENU DE LA CARD ────────────────────────────────────────
        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        card.addView(cardContent, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        // ── TOOLBAR : boutons à droite ────────────────────────────────
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        toolbar.setBackgroundColor(Color.parseColor("#33000000")); // bg-black/20
        toolbar.setPadding(dp8, dp4, dp8, dp4);
        LinearLayout.LayoutParams toolbarLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp36 + dp4 * 2);
        cardContent.addView(toolbar, toolbarLp);

        // Bouton AGRANDIR
        expandBtn = makeToolbarBtn("⤢");
        toolbar.addView(expandBtn, btnLp());
        expandBtn.setOnClickListener(v -> toggleExpand());

        // Bouton TÉLÉCHARGER
        dlBtn = makeToolbarBtn("↓");
        toolbar.addView(dlBtn, btnLp());
        dlBtn.setOnClickListener(v -> downloadImage());

        // Bouton FERMER
        TextView closeBtn = makeToolbarBtn("✕");
        LinearLayout.LayoutParams closeLp = btnLp();
        closeLp.setMargins(dp4, 0, 0, 0);
        toolbar.addView(closeBtn, closeLp);
        closeBtn.setOnClickListener(v -> finish());

        // Séparateur
        View sep = new View(this);
        sep.setBackgroundColor(Color.parseColor("#1AFFFFFF")); // white/10
        cardContent.addView(sep, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp2 / 2 + 1));

        // ── ZONE IMAGE ────────────────────────────────────────────────
        FrameLayout imgContainer = new FrameLayout(this);
        LinearLayout.LayoutParams imgContainerLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        imgContainerLp.setMargins(dp8, dp8, dp8, dp8);
        cardContent.addView(imgContainer, imgContainerLp);

        // Loader
        ProgressBar progress = new ProgressBar(this);
        imgContainer.addView(progress, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER));

        // Image
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setVisibility(View.GONE);
        imgContainer.addView(imageView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        // ── CHARGEMENT ASYNC ──────────────────────────────────────────
        executor.execute(() -> {
            try {
                InputStream is = new URL(imageUrl).openStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                loadedBitmap = bmp;
                new Handler(Looper.getMainLooper()).post(() -> {
                    progress.setVisibility(View.GONE);
                    imageView.setImageBitmap(bmp);
                    imageView.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // ── HELPERS ───────────────────────────────────────────────────────

    private TextView makeToolbarBtn(String label) {
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTextColor(Color.parseColor("#94A3B8")); // slate-400
        btn.setTextSize(15);
        btn.setGravity(Gravity.CENTER);
        return btn;
    }

    private LinearLayout.LayoutParams btnLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp36, dp36);
        lp.setMargins(dp4, 0, dp4, 0);
        return lp;
    }

    private FrameLayout.LayoutParams cardNormalLp() {
        int w = (int)(screenW * 0.82);
        int h = (int)(screenH * 0.58);
        return new FrameLayout.LayoutParams(w, h, Gravity.CENTER);
    }

    private FrameLayout.LayoutParams cardExpandedLp() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(dp8, dp8, dp8, dp8);
        return lp;
    }

    private void toggleExpand() {
        isExpanded = !isExpanded;
        if (isExpanded) {
            card.setLayoutParams(cardExpandedLp());
            cardBg.setCornerRadius(dp8 * 2f);
            expandBtn.setText("⊟");
        } else {
            card.setLayoutParams(cardNormalLp());
            cardBg.setCornerRadius(dp16 * 2f);
            expandBtn.setText("⤢");
        }
    }

    private void downloadImage() {
        if (loadedBitmap == null) return;
        executor.execute(() -> {
            try {
                File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Léa Studio");
                dir.mkdirs();
                String fileName = "lea_studio_" + System.currentTimeMillis() + ".png";
                File out = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(out);
                loadedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                android.media.MediaScannerConnection.scanFile(
                    this, new String[]{ out.getAbsolutePath() }, null, null);
                new Handler(Looper.getMainLooper()).post(() -> {
                    dlBtn.setText("✓");
                    dlBtn.setTextColor(Color.parseColor("#22C55E")); // vert
                    Toast.makeText(this, "Sauvegardée dans Photos → Léa Studio", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        // Rend le micro à Bixby si demandé (mode génération image guidé)
        if (getIntent().getBooleanExtra("give_mic_on_close", false)) {
            if (LeaNovaService.instance != null) {
                LeaNovaService.instance.triggerCooldownFromActivity();
            }
        }
    }
}
