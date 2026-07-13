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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.*;

/**
 * Base commune pour les 15 écrans détail LÉA PLUS.
 * Chaque sous-classe implémente getFeatureId() et buildContent().
 */
public abstract class LeaFeatureDetailActivity extends Activity {

    protected static final int BG     = 0xFF020617;
    protected static final int CYAN   = 0xFF00E5FF;
    protected static final int VIOLET = 0xFF7C3AED;
    protected static final int CARD   = 0xFF0B1526;
    protected static final int GOLD   = 0xFFFFD700;
    protected static final int GRN    = 0xFF4CAF50;
    protected static final int DIM    = 0xFF7BB8CC;
    protected static final int RED    = 0xFFF44336;
    protected static final int GLASS_BORDER = 0x1EFFFFFF;

    protected LeaPlusManager  plusMgr;
    protected LeaPlusDatabase db;
    protected String          featureId;
    protected LinearLayout    contentArea;

    private TextView statusBadge;
    private TextView toggleSub;

    protected abstract String getFeatureId();
    protected abstract void   buildContent(LinearLayout parent);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            featureId = getFeatureId();
            plusMgr   = LeaPlusManager.get(this);
            db        = LeaPlusDatabase.get(this);

            final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                try { LeaAndroidLogger.error(this, "CRASH/" + featureId, throwable.getClass().getSimpleName() + ": " + throwable.getMessage()); } catch (Throwable ignored) {}
                android.util.Log.e("LeaFeature", "Crash non géré dans " + featureId, throwable);
                if (defaultHandler != null) defaultHandler.uncaughtException(thread, throwable);
            });

            LeaParentalControlManager pcm = LeaParentalControlManager.get(this);
            if (!pcm.checkFeatureAccess(featureId)) {
                showBlockScreen(pcm); return;
            }

            buildUI();
            LeaAndroidLogger.nav(this, featureId);
            LeaAndroidLogger.flushAsync(this);
        } catch (Throwable t) {
            android.util.Log.e("LeaFeature", "CRASH onCreate " + featureId, t);
            try { LeaAndroidLogger.error(this, featureId != null ? featureId : "INIT", t.getClass().getSimpleName() + ": " + t.getMessage()); } catch (Throwable ignored) {}
            showFatalError(t);
        }
    }

    private void buildUI() {
        LeaPlusManager.FeatureInfo info    = plusMgr.getInfo(featureId);
        String  icon    = info != null ? info.icon        : "✨";
        String  name    = info != null ? info.name        : featureId;
        String  desc    = info != null ? info.description : "";
        boolean enabled = db.isEnabled(featureId);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);

        LinearLayout vl = new LinearLayout(this);
        vl.setOrientation(LinearLayout.VERTICAL);
        vl.setBackgroundColor(BG);
        vl.setPadding(0, 0, 0, dp(32));
        sv.addView(vl);

        // ── Header ────────────────────────────────────────────────────────────
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setBackgroundColor(BG);
        hdr.setPadding(dp(8), dp(20), dp(16), dp(12));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x14FFFFFF);
        backBg.setShape(GradientDrawable.OVAL);
        backBg.setStroke(dp(1), GLASS_BORDER);
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), backBg, null));
        back.setTextSize(20);
        back.setPadding(0, 0, 0, dp(2));
        back.setMinWidth(0); back.setMinHeight(0);
        back.setOnClickListener(v -> finish());
        hdr.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(18);
        iconTv.setPadding(dp(10), 0, dp(8), 0);
        hdr.addView(iconTv);

        TextView titleTv = new TextView(this);
        titleTv.setText(name);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(15);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleTv.setShadowLayer(dp(10), 0, 0, 0x8000E5FF);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        hdr.addView(titleTv);

        statusBadge = new TextView(this);
        statusBadge.setText(enabled ? "● ON" : "○ OFF");
        statusBadge.setTextColor(enabled ? GRN : DIM);
        statusBadge.setTextSize(10);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        statusBadge.setBackground(pillBg(enabled ? GRN : DIM, false));
        hdr.addView(statusBadge);

        vl.addView(hdr);

        // ── Toggle card ───────────────────────────────────────────────────────
        LinearLayout tCard = makeCard();
        tCard.setOrientation(LinearLayout.HORIZONTAL);
        tCard.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout tTexts = new LinearLayout(this);
        tTexts.setOrientation(LinearLayout.VERTICAL);
        tTexts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tTitle = new TextView(this);
        tTitle.setText("Activer " + name);
        tTitle.setTextColor(0xFFFFFFFF);
        tTitle.setTextSize(13);
        tTitle.setTypeface(null, Typeface.BOLD);
        tTexts.addView(tTitle);

        toggleSub = new TextView(this);
        toggleSub.setText(enabled ? "✅ Feature active" : "⏹ Feature inactive");
        toggleSub.setTextColor(enabled ? GRN : DIM);
        toggleSub.setTextSize(10);
        tTexts.addView(toggleSub);
        tCard.addView(tTexts);

        Switch sw = new Switch(this);
        styleSwitch(sw, CYAN);
        sw.setChecked(enabled);
        sw.setOnCheckedChangeListener((v, on) -> handleToggle(on, name));
        tCard.addView(sw);
        vl.addView(tCard);

        // ── Description ───────────────────────────────────────────────────────
        if (!desc.isEmpty()) {
            LinearLayout dCard = makeCard();
            TextView dTv = new TextView(this);
            dTv.setText(desc);
            dTv.setTextColor(DIM);
            dTv.setTextSize(12);
            dCard.addView(dTv);
            vl.addView(dCard);
        }

        // ── Content area (remplie par la sous-classe) ─────────────────────────
        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(dp(12), 0, dp(12), 0);
        vl.addView(contentArea);

        setContentView(sv);
        applyImmersive(this);
        try {
            buildContent(contentArea);
        } catch (Throwable t) {
            android.util.Log.e("LeaFeature", "Crash buildContent(" + featureId + ")", t);
            LeaAndroidLogger.error(this, featureId, t.getClass().getSimpleName() + ": " + t.getMessage());
            String where = "";
            for (StackTraceElement el : t.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { where = " @ " + el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            Toast.makeText(this, "❌ " + featureId + " — " + t.getMessage() + where, Toast.LENGTH_LONG).show();
        }
    }

    private void handleToggle(boolean on, String name) {
        if (on) plusMgr.enable(featureId);
        else    plusMgr.disable(featureId);
        statusBadge.setText(on ? "● ON" : "○ OFF");
        statusBadge.setTextColor(on ? GRN : DIM);
        toggleSub.setText(on ? "✅ Feature active" : "⏹ Feature inactive");
        toggleSub.setTextColor(on ? GRN : DIM);
        Toast.makeText(this,
            (on ? "✅ " : "⏹ ") + name + (on ? " activée" : " désactivée"),
            Toast.LENGTH_SHORT).show();
    }

    // ── Helpers protégés disponibles pour toutes les sous-classes ─────────────

    protected LinearLayout makeCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{ lighten(CARD, 0.06f), CARD });
        gd.setCornerRadius(dp(18));
        gd.setStroke(dp(1), GLASS_BORDER);
        c.setElevation(dp(2));
        c.setBackground(gd);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        c.setLayoutParams(lp);
        return c;
    }

    protected TextView sectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(GOLD);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(14), 0, dp(6));
        return tv;
    }

    protected LinearLayout infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(rlp);
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(DIM);
        lbl.setTextSize(11);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lbl);
        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(0xFFFFFFFF);
        val.setTextSize(11);
        val.setTypeface(null, Typeface.BOLD);
        val.setGravity(Gravity.END);
        row.addView(val);
        return row;
    }

    protected View divider() {
        View v = new View(this);
        v.setBackgroundColor(GLASS_BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(0, dp(6), 0, dp(6));
        v.setLayoutParams(lp);
        return v;
    }

    protected Button actionButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(color);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor((color & 0x00FFFFFF) | 0x22000000);
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), color);
        btn.setBackground(new RippleDrawable(ColorStateList.valueOf((color & 0x00FFFFFF) | 0x55000000), gd, null));
        btn.setElevation(dp(1));
        btn.setTextSize(11);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        lp.setMargins(0, dp(8), 0, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    protected TextView badge(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(" " + text + " ");
        tv.setTextColor(color);
        tv.setBackground(pillBg(color, false));
        tv.setTextSize(9);
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    /** Fond pilule translucide bordé, utilisé pour badges/statuts. */
    protected GradientDrawable pillBg(int color, boolean strongFill) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor((color & 0x00FFFFFF) | (strongFill ? 0x44000000 : 0x22000000));
        gd.setCornerRadius(dp(20));
        gd.setStroke(dp(1), color);
        return gd;
    }

    /** Restyle un Switch Android par défaut aux couleurs Léa. */
    protected void styleSwitch(Switch sw, int color) {
        sw.setTrackTintList(new ColorStateList(
            new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
            new int[]{ (color & 0x00FFFFFF) | 0x88000000, 0xFF37474F }));
        sw.setThumbTintList(new ColorStateList(
            new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
            new int[]{ color, 0xFFCFD8DC }));
    }

    /** Éclaircit légèrement une couleur (glassmorphism : dégradé subtil au lieu d'un aplat). */
    protected int lighten(int color, float amount) {
        int a = (color >>> 24) & 0xFF, r = (color >>> 16) & 0xFF, g = (color >>> 8) & 0xFF, b = color & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount) + 8);
        g = Math.min(255, (int) (g + (255 - g) * amount) + 8);
        b = Math.min(255, (int) (b + (255 - b) * amount) + 8);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    protected int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override public void onBackPressed() { finish(); }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive(this);
    }

    /** Cache la barre de navigation Android sur toutes les Activities Léa. */
    @SuppressWarnings("deprecation")
    public static void applyImmersive(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController ctrl = activity.getWindow().getInsetsController();
            if (ctrl != null) {
                ctrl.hide(WindowInsets.Type.navigationBars());
                ctrl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private void showFatalError(Throwable t) {
        String where = "";
        for (StackTraceElement el : t.getStackTrace()) {
            if (el.getClassName().contains("lea_v3")) { where = "\n@ " + el.getFileName() + ":" + el.getLineNumber(); break; }
        }
        try {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(BG);
            root.setGravity(Gravity.CENTER);
            root.setPadding(dp(24), dp(60), dp(24), dp(40));
            setContentView(root);
            applyImmersive(this);

            TextView icon = new TextView(this); icon.setText("❌"); icon.setTextSize(48); icon.setGravity(Gravity.CENTER); root.addView(icon);

            TextView title = new TextView(this);
            title.setText("Erreur — " + (featureId != null ? featureId : "?"));
            title.setTextColor(RED); title.setTextSize(16); title.setTypeface(null, Typeface.BOLD); title.setGravity(Gravity.CENTER); title.setPadding(0, dp(12), 0, dp(8)); root.addView(title);

            TextView msg = new TextView(this);
            msg.setText(t.getClass().getSimpleName() + ": " + t.getMessage() + where);
            msg.setTextColor(DIM); msg.setTextSize(12); msg.setGravity(Gravity.CENTER); root.addView(msg);

            Button back = new Button(this); back.setText("← Retour"); back.setBackgroundColor(CARD); back.setTextColor(CYAN); back.setTypeface(null, Typeface.BOLD); back.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(32), 0, 0); lp.gravity = Gravity.CENTER; back.setLayoutParams(lp);
            back.setOnClickListener(v -> finish()); root.addView(back);
        } catch (Throwable ignored) {
            Toast.makeText(this, "❌ " + (featureId != null ? featureId : "?") + " — " + t.getMessage() + where, Toast.LENGTH_LONG).show();
        }
    }

    private void showBlockScreen(LeaParentalControlManager pcm) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(dp(40), dp(60), dp(40), dp(40));

        TextView icon = new TextView(this);
        icon.setText("🔒");
        icon.setTextSize(64);
        icon.setGravity(android.view.Gravity.CENTER);
        root.addView(icon);

        TextView title = new TextView(this);
        String reason  = pcm.isBedtimeLocked() ? "Couvre-feu actif" :
                         pcm.isScreenTimeLimitReached() ? "Temps d'écran épuisé" :
                         "Accès restreint";
        title.setText(reason);
        title.setTextColor(RED); title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, dp(16), 0, dp(8));
        root.addView(title);

        TextView msg = new TextView(this);
        String pseudo = pcm.getActiveChildPseudo();
        msg.setText("Le compte " + (pseudo != null ? pseudo : "enfant") +
            " n'a pas accès à cette fonctionnalité.\nDemande à un parent de déverrouiller.");
        msg.setTextColor(DIM); msg.setTextSize(14);
        msg.setGravity(android.view.Gravity.CENTER);
        root.addView(msg);

        Button backBtn = new Button(this);
        backBtn.setText("← Retour");
        backBtn.setBackgroundColor(CARD); backBtn.setTextColor(CYAN);
        backBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(32), 0, 0); lp.gravity = android.view.Gravity.CENTER;
        backBtn.setLayoutParams(lp);
        backBtn.setOnClickListener(v -> finish());
        root.addView(backBtn);

        setContentView(root);
        applyImmersive(this);
    }
}
