package com.flolov42.lea_v3.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Halo de fumée multi-couches :
 *  - 5 couches concentriques
 *  - rotations indépendantes (horaire + anti-horaire, vitesses différentes)
 *  → effet fumée organique et tourbillonnant
 */
public class HaloView extends View {

    public enum Mode { NONE, CHECKING, AVAILABLE, UPTODATE }

    // Mise à jour dispo — bleu/violet/magenta
    private static final int[] COLORS_AVAILABLE = {
        0xFF00f2ff, 0xFF0066ff, 0xFF5500ff,
        0xFFcc00ff, 0xFFff0077, 0xFF0066ff, 0xFF00f2ff
    };
    // A jour — vert/teal/violet
    private static final int[] COLORS_UPTODATE = {
        0xFF22c55e, 0xFF00ffaa, 0xFF00bbff,
        0xFF9933ff, 0xFFff00aa, 0xFF22c55e
    };
    // Vérification — cyan simple
    private static final int[] COLORS_CHECKING = {
        0xFF00ccff, 0xFF0044dd, 0xFF00ccff, 0xFF0044dd, 0xFF00ccff
    };

    private final Paint p         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint= new Paint(Paint.ANTI_ALIAS_FLAG);

    // 3 vitesses de rotation indépendantes (fumée) + 1 anneau orbital
    private float rotSlow  = 0f;   // sens horaire, lent
    private float rotMid   = 0f;   // anti-horaire, moyen
    private float rotFast  = 0f;   // sens horaire, rapide
    private float rotRing  = 0f;   // anneau orbital HUD, très lent, sens inverse
    private float pulse    = 0.65f;
    private float breathe  = 0f;   // respiration lente du halo ambiant

    private Mode mode = Mode.NONE;

    private ValueAnimator slowAnim, midAnim, fastAnim, ringAnim, pulseAnim, breatheAnim;

    // Cache
    private SweepGradient shader;
    private BlurMaskFilter blur0, blur1, blur2, blur3, blur4;
    private Mode cachedMode = null;
    private int  cachedW = 0, cachedH = 0;

    public HaloView(Context ctx)                           { super(ctx);         init(); }
    public HaloView(Context ctx, AttributeSet a)           { super(ctx, a);      init(); }
    public HaloView(Context ctx, AttributeSet a, int def)  { super(ctx, a, def); init(); }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        p.setStyle(Paint.Style.FILL);
        ringPaint.setStyle(Paint.Style.STROKE);
        sparkPaint.setStyle(Paint.Style.FILL);
    }

    // ── API ──────────────────────────────────────────────────

    public void setMode(Mode m) {
        if (mode == m) return;
        mode = m;
        cachedMode = null;
        stopAll();
        if (mode != Mode.NONE) startAll();
        invalidate();
    }

    // ── Animations ──────────────────────────────────────────

    private void startAll() {
        boolean checking = (mode == Mode.CHECKING);

        // Couche 1 — horaire lent (4.5s / 2s si checking)
        slowAnim = rot(0f, 360f, checking ? 2000L : 4500L, a -> { rotSlow = (float) a.getAnimatedValue(); invalidate(); });

        // Couche 2 — anti-horaire moyen (3.2s / 1.5s)
        midAnim  = rot(0f, 360f, checking ? 1500L : 3200L, a -> rotMid  = (float) a.getAnimatedValue());

        // Couche 3 — horaire rapide (2s / 1s)
        fastAnim = rot(0f, 360f, checking ? 1000L : 2000L, a -> rotFast = (float) a.getAnimatedValue());

        // Anneau orbital HUD — très lent, sens inverse (6s / 3s)
        ringAnim = rot(0f, -360f, checking ? 3000L : 6000L, a -> rotRing = (float) a.getAnimatedValue());

        // Pulse alpha
        pulseAnim = ValueAnimator.ofFloat(0.50f, 0.92f);
        pulseAnim.setDuration(1800);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnim.addUpdateListener(a -> pulse = (float) a.getAnimatedValue());
        pulseAnim.start();

        // Respiration lente du halo ambiant en arrière-plan
        breatheAnim = ValueAnimator.ofFloat(0f, 1f);
        breatheAnim.setDuration(3400);
        breatheAnim.setRepeatCount(ValueAnimator.INFINITE);
        breatheAnim.setRepeatMode(ValueAnimator.REVERSE);
        breatheAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        breatheAnim.addUpdateListener(a -> breathe = (float) a.getAnimatedValue());
        breatheAnim.start();
    }

    private ValueAnimator rot(float from, float to, long dur, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator va = ValueAnimator.ofFloat(from, to);
        va.setDuration(dur);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(l);
        va.start();
        return va;
    }

    private void stopAll() {
        if (slowAnim    != null) { slowAnim.cancel();    slowAnim    = null; }
        if (midAnim     != null) { midAnim.cancel();     midAnim     = null; }
        if (fastAnim    != null) { fastAnim.cancel();    fastAnim    = null; }
        if (ringAnim    != null) { ringAnim.cancel();    ringAnim    = null; }
        if (pulseAnim   != null) { pulseAnim.cancel();   pulseAnim   = null; }
        if (breatheAnim != null) { breatheAnim.cancel(); breatheAnim = null; }
    }

    @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); stopAll(); }

    // ── Dessin ──────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        if (mode == Mode.NONE) return;

        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f, cy = h / 2f;
        float R  = Math.min(w, h) / 2f;
        if (R <= 2f) return;

        // Recréer les filtres si taille change
        if (w != cachedW || h != cachedH) {
            blur0 = new BlurMaskFilter(R * 0.34f, BlurMaskFilter.Blur.NORMAL); // halo ambiant, ultra diffus
            blur1 = new BlurMaskFilter(R * 0.36f, BlurMaskFilter.Blur.NORMAL); // très diffus
            blur2 = new BlurMaskFilter(R * 0.26f, BlurMaskFilter.Blur.NORMAL); // diffus
            blur3 = new BlurMaskFilter(R * 0.16f, BlurMaskFilter.Blur.NORMAL); // moyen
            blur4 = new BlurMaskFilter(R * 0.08f, BlurMaskFilter.Blur.NORMAL); // précis
            cachedW = w; cachedH = h;
        }

        // Recréer le shader si mode/taille change
        if (mode != cachedMode) {
            shader = new SweepGradient(cx, cy, getColors(), null);
            cachedMode = mode;
        }

        p.setShader(shader);

        // Rayon max de fumée : 58% → avec blur1 (36%) → 58+36 = 94% < 100% → pas de carré
        float S = R * 0.58f;

        // ── Halo ambiant : glow large et discret, respire lentement en arrière-plan
        // (rayon + flou doit rester < 100% de R, sinon le flou est tronqué net au bord de la vue → carré visible)
        draw(canvas, cx, cy, 0f, R * 0.50f, blur0, (int)(40 + breathe * 35));

        // ── Couche 1 : halo extérieur (horaire lent, très diffus)
        draw(canvas, cx, cy, rotSlow,    S,        blur1, (int)(pulse * 130));

        // ── Couche 2 : nappe de fumée (anti-horaire moyen, diffus)
        draw(canvas, cx, cy, -rotMid,   S * 0.85f, blur2, (int)(pulse * 170));

        // ── Couche 3 : corps de fumée (horaire rapide, moyen)
        draw(canvas, cx, cy, rotFast,   S * 0.68f, blur3, (int)(pulse * 210));

        // ── Couche 4 : cœur brillant (anti-horaire rapide, précis)
        draw(canvas, cx, cy, -rotFast * 1.4f, S * 0.45f, blur4, (int)(pulse * 255));

        // ── Couche 5 : étincelle centrale (horaire ultra-rapide, sans blur)
        draw(canvas, cx, cy, rotFast * 2.1f,  S * 0.22f, null,  (int)(pulse * 110));

        // ── Cœur énergétique blanc-chaud : point brillant net au centre
        sparkPaint.setShader(null);
        sparkPaint.setMaskFilter(blur4);
        sparkPaint.setColor(0xFFFFFFFF);
        sparkPaint.setAlpha((int) (pulse * 200));
        canvas.drawCircle(cx, cy, S * 0.09f, sparkPaint);

        // ── Anneau orbital HUD : fin cercle lumineux qui tourne lentement en sens inverse
        ringPaint.setShader(shader);
        ringPaint.setMaskFilter(blur3);
        ringPaint.setStrokeWidth(Math.max(2f, R * 0.018f));
        ringPaint.setAlpha((int) (pulse * 190));
        canvas.save();
        canvas.rotate(rotRing, cx, cy);
        canvas.drawCircle(cx, cy, R * 0.72f, ringPaint);
        canvas.restore();

        // ── Particules d'énergie : petits points scintillants en orbite
        drawSparks(canvas, cx, cy, S);
    }

    private void drawSparks(Canvas c, float cx, float cy, float S) {
        int[] cols = getColors();
        int   n    = 5;
        float orbitR = S * 0.95f;
        sparkPaint.setShader(null);
        sparkPaint.setMaskFilter(blur4);
        for (int i = 0; i < n; i++) {
            double angle = Math.toRadians(rotMid * (i % 2 == 0 ? 1 : -1) + i * (360f / n));
            float sx = cx + (float) (Math.cos(angle) * orbitR);
            float sy = cy + (float) (Math.sin(angle) * orbitR);
            float twinkle = (float) (Math.sin(Math.toRadians(rotFast * 2f + i * 71f)) * 0.5 + 0.5);
            sparkPaint.setColor(cols[i % cols.length]);
            sparkPaint.setAlpha((int) (twinkle * 220));
            c.drawCircle(sx, sy, S * 0.035f, sparkPaint);
        }
    }

    private void draw(Canvas c, float cx, float cy,
                      float angle, float radius,
                      BlurMaskFilter blur, int alpha) {
        if (radius <= 0) return;
        c.save();
        c.rotate(angle, cx, cy);
        p.setMaskFilter(blur);
        p.setAlpha(alpha);
        c.drawCircle(cx, cy, radius, p);
        c.restore();
    }

    private int[] getColors() {
        switch (mode) {
            case AVAILABLE: return COLORS_AVAILABLE;
            case UPTODATE:  return COLORS_UPTODATE;
            case CHECKING:  return COLORS_CHECKING;
            default:        return new int[]{ 0xFF334155, 0xFF334155 };
        }
    }
}
