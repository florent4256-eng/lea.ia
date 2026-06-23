package com.flolov42.lea_v3.ui;

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
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class LeaVoiceActivity extends Activity {

    public static final int STATE_LISTEN = 0;
    public static final int STATE_THINK  = 1;
    public static final int STATE_SPEAK  = 2;

    static LeaVoiceActivity instance;
    private WaveBarView waveBarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getBooleanExtra("ACTION_KILL", false)) {
            finish();
            return;
        }

        instance = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        waveBarView = new WaveBarView(this);
        waveBarView.setCloseCallback(this::closeAndKill);
        setContentView(waveBarView);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) return;
        if (intent.getBooleanExtra("ACTION_KILL", false)) { finish(); return; }
        if (intent.hasExtra("ORB_STATE") && waveBarView != null) {
            waveBarView.setState(intent.getIntExtra("ORB_STATE", STATE_LISTEN));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance == this) instance = null;
    }

    /** Appelé par LeaNovaService pour changer l'état visuel. */
    public static void pushState(int state) {
        LeaVoiceActivity a = instance;
        if (a != null) a.runOnUiThread(() -> {
            if (a.waveBarView != null) a.waveBarView.setState(state);
        });
    }

    private void closeAndKill() {
        LeaNovaService svc = LeaNovaService.instance;
        if (svc != null) svc.abortEverything();
        finish();
        overridePendingTransition(0, 0);
    }

    // =================================================================
    //  WaveBarView — ovale vivant type Siri, ondes FILL + halo + souffle
    // =================================================================
    public static class WaveBarView extends View {

        public interface CloseCallback { void close(); }
        private CloseCallback closeCallback;

        // ── PALETTE [onde1, onde2, onde3, onde4, fond] par état ───────────
        private static final int[][] STATE_COLORS = {
            // ÉCOUTE — vert émeraude / turquoise / menthe : "je t'écoute, je suis là"
            { 0xFF00E676, 0xFF1DE9B6, 0xFF69F0AE, 0xFF00BFA5, 0xFF001A0E },
            // RÉFLEXION — orange doré / ambre / corail : "j'y réfléchis, je cherche"
            { 0xFFFFAB40, 0xFFFF6D00, 0xFFFFD740, 0xFFFF4081, 0xFF1A0800 },
            // PAROLE — rose / violet / cyan / ambre : Siri-like, explosif
            { 0xFFFF4081, 0xFF7C4DFF, 0xFF00E5FF, 0xFFFF6D00, 0xFF0A0015 },
        };

        private static final float[] TARGET_AMP   = { 0.38f, 0.54f, 0.68f };
        private static final float[] TARGET_SPEED = { 0.026f, 0.062f, 0.085f };

        private int   state         = STATE_LISTEN;
        private int   prevState     = -1;
        private float currentAmp   = TARGET_AMP[STATE_LISTEN];
        private float currentSpeed = TARGET_SPEED[STATE_LISTEN];

        // 4 phases décalées de π/2
        private float phase1 = 0f;
        private float phase2 = 1.571f;
        private float phase3 = 3.141f;
        private float phase4 = 4.712f;
        private float breathPhase = 0f; // cycle de "souffle" ~2.5s

        private long    lastFrameNanos = 0;
        private boolean isAttached     = false;
        private volatile boolean soundEnabled = true;
        private float   dp             = 1f;

        // ── GÉOMÉTRIE pré-allouée ─────────────────────────────────────────
        private final RectF bar      = new RectF();
        private final RectF glow1    = new RectF(); // halo proche
        private final RectF glow2    = new RectF(); // halo moyen
        private final RectF glow3    = new RectF(); // halo lointain
        private final Path  clipPath  = new Path(); // forme œil — clip des ondes
        private final Path  glow1Path = new Path();
        private final Path  glow2Path = new Path();
        private final Path  glow3Path = new Path();
        private final Path  wavePath  = new Path();

        // ── PINCEAUX pré-alloués ──────────────────────────────────────────
        private final Paint bgPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint w1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint w2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint w3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint w4 = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean shadersDirty = true;

        private final Choreographer.FrameCallback frameCallback =
            new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    if (!isAttached) return;
                    float dt = lastFrameNanos == 0 ? 0.016f
                             : Math.min((frameTimeNanos - lastFrameNanos) / 1_000_000_000f, 0.032f);
                    lastFrameNanos = frameTimeNanos;

                    float lerpA = Math.min(dt * 8f, 1f);
                    float lerpS = Math.min(dt * 6f, 1f);
                    currentAmp   += (TARGET_AMP[state]   - currentAmp)   * lerpA;
                    currentSpeed += (TARGET_SPEED[state] - currentSpeed) * lerpS;

                    phase1 = wrapPhase(phase1 + currentSpeed);
                    phase2 = wrapPhase(phase2 + currentSpeed * 0.73f);
                    phase3 = wrapPhase(phase3 + currentSpeed * 0.57f);
                    phase4 = wrapPhase(phase4 + currentSpeed * 0.41f);
                    breathPhase = wrapPhase(breathPhase + 0.010f);

                    invalidate();
                    Choreographer.getInstance().postFrameCallback(this);
                }
            };

        public WaveBarView(Context ctx) {
            super(ctx);
            dp = ctx.getResources().getDisplayMetrics().density;
            setLayerType(LAYER_TYPE_SOFTWARE, null); // nécessaire pour BlurMaskFilter
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2.0f * dp);
            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(30 * dp, android.graphics.BlurMaskFilter.Blur.NORMAL));
            w1.setStyle(Paint.Style.FILL);
            w2.setStyle(Paint.Style.FILL);
            w3.setStyle(Paint.Style.FILL);
            w4.setStyle(Paint.Style.FILL);
        }

        public void setCloseCallback(CloseCallback cb) { closeCallback = cb; }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            isAttached = true; soundEnabled = true; lastFrameNanos = 0;
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            isAttached = false; soundEnabled = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            float barW  = w * 0.65f;
            float barH  = 68 * dp;
            float botMg = 80 * dp;
            float left  = (w - barW) / 2f;
            bar.set(left, h - botMg - barH, left + barW, h - botMg);

            buildEyePath(clipPath, bar);

            // Halos concentriques en forme d'œil
            float e1 = 7 * dp, e2 = 15 * dp, e3 = 26 * dp;
            glow1.set(bar.left-e1, bar.top-e1*0.6f, bar.right+e1, bar.bottom+e1*0.6f);
            glow2.set(bar.left-e2, bar.top-e2*0.6f, bar.right+e2, bar.bottom+e2*0.6f);
            glow3.set(bar.left-e3, bar.top-e3*0.6f, bar.right+e3, bar.bottom+e3*0.6f);
            buildEyePath(glow1Path, glow1);
            buildEyePath(glow2Path, glow2);
            buildEyePath(glow3Path, glow3);
            shadersDirty = true;
        }

        // Œil humain : paupière supérieure bien ouverte, inférieure légèrement plus plate
        private static void buildEyePath(Path out, RectF r) {
            out.reset();
            float cx = r.centerX(), cy = r.centerY();
            float w = r.width();
            // Paupière supérieure : arc ample, les deux points de contrôle tirés vers r.top
            out.moveTo(r.left, cy);
            out.cubicTo(cx - w * 0.18f, r.top,
                        cx + w * 0.18f, r.top,
                        r.right, cy);
            // Paupière inférieure : légèrement plus plate que le haut
            out.cubicTo(cx + w * 0.18f, r.bottom,
                        cx - w * 0.18f, r.bottom,
                        r.left, cy);
            out.close();
        }

        public void setState(int newState) {
            int s = Math.max(0, Math.min(2, newState));
            if (s == state) return;
            state = s; shadersDirty = true;
            playStateSound(state);
        }

        // Reconstruit les shaders (alloués UNE FOIS par changement d'état ou de taille)
        private void rebuildShaders() {
            if (bar.isEmpty()) return;
            int[] c = STATE_COLORS[state];
            float top = bar.top, bot = bar.bottom;

            // Fond : gradient horizontal sombre sur les côtés, légèrement plus clair au centre
            bgPaint.setShader(new LinearGradient(
                bar.left, 0, bar.right, 0,
                new int[]{ darken(c[4]), c[4], darken(c[4]) },
                new float[]{ 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));

            // 4 ondes : gradient vertical, couleur saturée en haut → transparent en bas
            w1.setShader(new LinearGradient(bar.left, top, bar.left, bot,
                new int[]{ makeA(c[0], 0xCC), makeA(c[0], 0x66), 0x00000000 },
                new float[]{ 0f, 0.50f, 1f }, Shader.TileMode.CLAMP));
            w2.setShader(new LinearGradient(bar.left, top, bar.left, bot,
                new int[]{ makeA(c[1], 0xB4), makeA(c[1], 0x55), 0x00000000 },
                new float[]{ 0.05f, 0.58f, 1f }, Shader.TileMode.CLAMP));
            w3.setShader(new LinearGradient(bar.left, top, bar.left, bot,
                new int[]{ makeA(c[2], 0x99), makeA(c[2], 0x44), 0x00000000 },
                new float[]{ 0.10f, 0.65f, 1f }, Shader.TileMode.CLAMP));
            w4.setShader(new LinearGradient(bar.left, top, bar.left, bot,
                new int[]{ makeA(c[3], 0x80), makeA(c[3], 0x33), 0x00000000 },
                new float[]{ 0.15f, 0.72f, 1f }, Shader.TileMode.CLAMP));

            borderPaint.setColor(c[0]);
            borderPaint.setAlpha(140);
            shadersDirty = false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bar.isEmpty()) return;
            if (shadersDirty) rebuildShaders();

            int[] c = STATE_COLORS[state];

            // Souffle : amplitude qui respire, plus prononcé en écoute
            float breathFactor;
            if (state == STATE_LISTEN) {
                // respiration ample + flutter secondaire pour donner de la vie
                breathFactor = 1f + 0.22f * (float) Math.sin(breathPhase)
                                  + 0.06f * (float) Math.sin(breathPhase * 2.7f);
            } else if (state == STATE_THINK) {
                // pulsation rapide et nette — cherchant/processing
                breathFactor = 1f + 0.20f * (float) Math.sin(breathPhase * 2.5f);
            } else {
                breathFactor = 1f + 0.03f * (float) Math.sin(breathPhase * 2.5f);
            }

            float halfH = bar.height() / 2f;
            float amp   = currentAmp * halfH * breathFactor;
            float midY  = bar.centerY();

            // 1. Halo flou — visible au centre, bords estompés vers transparent
            glowPaint.setColor(c[0]);
            glowPaint.setAlpha(110);
            canvas.drawPath(glow1Path, glowPaint);

            // 3. Ondes FILL empilées dans le clip œil
            canvas.save();
            canvas.clipPath(clipPath);
            drawWaveFill(canvas, w4, midY,                 amp * 0.46f, phase4, 1.15f);
            drawWaveFill(canvas, w3, midY - halfH * 0.07f, amp * 0.63f, phase3, 2.35f);
            drawWaveFill(canvas, w2, midY + halfH * 0.09f, amp * 0.81f, phase2, 3.20f);
            drawWaveFill(canvas, w1, midY,                 amp,          phase1, 1.85f);
            canvas.restore();

        }

        // Onde sine REMPLIE — fermée vers le bas de la barre (effet couche colorée)
        private void drawWaveFill(Canvas canvas, Paint paint,
                                   float baseY, float amplitude,
                                   float phase, float freq) {
            wavePath.reset();
            int steps = (int) bar.width();
            float w = bar.width();
            boolean first = true;
            for (int i = 0; i <= steps; i++) {
                float x = bar.left + i;
                float t = i / w;
                float env = (float) Math.sin(t * Math.PI); // fond aux extrémités
                float y   = baseY + amplitude * env
                          * (float) Math.sin(phase + t * freq * 2 * Math.PI);
                if (first) { wavePath.moveTo(x, y); first = false; }
                else        { wavePath.lineTo(x, y); }
            }
            wavePath.lineTo(bar.right, bar.bottom);
            wavePath.lineTo(bar.left,  bar.bottom);
            wavePath.close();
            canvas.drawPath(wavePath, paint);
        }

        private float wrapPhase(float p) {
            float tp = (float)(2 * Math.PI);
            return p > tp ? p - tp : p;
        }

        private int darken(int c) {
            return Color.rgb(
                (int)(Color.red(c)   * 0.55f),
                (int)(Color.green(c) * 0.55f),
                (int)(Color.blue(c)  * 0.55f));
        }

        private int makeA(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (closeCallback != null) closeCallback.close();
            }
            return true;
        }

        // =============================================================
        //  Sons synthétisés via AudioTrack (pas de fichier ressource)
        // =============================================================

        private void playStateSound(int s) {
            if (!soundEnabled) return;
            new Thread(() -> {
                try {
                    final int SR = 44100;
                    short[] buf;
                    switch (s) {
                        case STATE_LISTEN: buf = genDing(SR);   break;
                        case STATE_THINK:  buf = genBuzz(SR);   break;
                        case STATE_SPEAK:  buf = genWhoosh(SR); break;
                        default: return;
                    }
                    AudioTrack track = buildTrack(SR, buf.length * 2);
                    if (track == null) return;
                    track.write(buf, 0, buf.length);
                    track.play();
                    int durationMs = buf.length * 1000 / SR;
                    Thread.sleep(durationMs + 60);
                    track.stop();
                    track.release();
                } catch (Exception e) {
                    android.util.Log.e("LeaWave", "Son: " + e.getMessage());
                }
            }).start();
        }

        private AudioTrack buildTrack(int sampleRate, int bufferBytes) {
            try {
                int minBuf = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufSize = Math.max(minBuf, bufferBytes);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                        .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                        .setBufferSizeInBytes(bufSize)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                } else {
                    return new AudioTrack(android.media.AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STATIC);
                }
            } catch (Exception e) { return null; }
        }

        /**
         * Ding doux — montée 880→1060 Hz, 170ms, enveloppe en cloche asymétrique.
         * Simule un carillon léger.
         */
        private short[] genDing(int sr) {
            int n = sr * 170 / 1000;
            short[] s = new short[n];
            for (int i = 0; i < n; i++) {
                float t  = (float) i / sr;
                float p  = (float) i / n;
                float hz = 880f + 180f * p;                              // glide
                float env = (float) Math.exp(-9.0 * Math.pow(p - 0.12, 2)); // cloche
                s[i] = pcm(0.28f * env * (float) Math.sin(2 * Math.PI * hz * t));
            }
            return s;
        }

        /**
         * Bzzz subtil — 320 Hz modulé en amplitude à 11 Hz, 210ms.
         * Simule un bourdonnement doux de réflexion.
         */
        private short[] genBuzz(int sr) {
            int n = sr * 210 / 1000;
            short[] s = new short[n];
            for (int i = 0; i < n; i++) {
                float t  = (float) i / sr;
                float p  = (float) i / n;
                float env = p < 0.07f ? p / 0.07f : (p > 0.72f ? (1f - p) / 0.28f : 1f);
                float am  = 0.55f + 0.45f * (float) Math.sin(2 * Math.PI * 11 * t);
                s[i] = pcm(0.26f * env * am * (float) Math.sin(2 * Math.PI * 320 * t));
            }
            return s;
        }

        /**
         * Whoosh fluide — sweep exponentiel 200→850 Hz, 240ms, enveloppe arc de sinus.
         * Simule un souffle de départ fluide.
         */
        private short[] genWhoosh(int sr) {
            int n = sr * 240 / 1000;
            short[] s = new short[n];
            double phase = 0;
            for (int i = 0; i < n; i++) {
                float p    = (float) i / n;
                float freq = 200f + 650f * p * p;              // accélération quadratique
                float env  = (float)(Math.sin(p * Math.PI)     // arc de sinus
                           * Math.exp(-2.2 * p));              // extinction rapide en fin
                phase += 2 * Math.PI * freq / sr;
                s[i] = pcm(0.33f * env * (float) Math.sin(phase));
            }
            return s;
        }

        /** Convertit un float [-1,1] en short PCM 16 bits avec saturation. */
        private short pcm(float f) {
            int v = (int)(f * Short.MAX_VALUE);
            return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
        }
    }
}
