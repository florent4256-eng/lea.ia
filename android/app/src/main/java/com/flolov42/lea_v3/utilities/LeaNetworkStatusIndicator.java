package com.flolov42.lea_v3.utilities;

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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.flolov42.lea_v3.ui.LeaNetworkStatusActivity;

/**
 * Inline status chip showing the current connection type.
 * Attaches to an existing activity layout.
 *
 * Usage (in an Activity):
 *   LeaNetworkStatusIndicator indicator = new LeaNetworkStatusIndicator(this);
 *   parentLayout.addView(indicator.getView());
 *   // in onResume: indicator.start();
 *   // in onPause:  indicator.stop();
 */
public class LeaNetworkStatusIndicator implements LeaNetworkDetector.ConnectionListener {

    private final Context ctx;
    private final TextView chip;
    private final Handler ui = new Handler(Looper.getMainLooper());

    public LeaNetworkStatusIndicator(Context ctx) {
        this.ctx = ctx;
        chip = new TextView(ctx);
        chip.setTextSize(11);
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        chip.setPadding(dp(10), dp(4), dp(10), dp(4));
        chip.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, LeaNetworkStatusActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        });
        refresh();
    }

    public TextView getView() { return chip; }

    public void start() {
        LeaNetworkDetector.addListener(this);
        LeaNetworkDetector.probeAsync(ctx);
    }

    public void stop() {
        LeaNetworkDetector.removeListener(this);
    }

    @Override
    public void onConnectionChanged(String newType, long latencyMs) {
        ui.post(this::refresh);
    }

    // ── Internal ──────────────────────────────────────────────────────────────
    private void refresh() {
        String type    = LeaNetworkDetector.getCachedType(ctx);
        long   latency = LeaNetworkDetector.getCachedLatency(ctx);
        String label; int color; int textColor = Color.BLACK;
        switch (type) {
            case LeaNetworkDetector.TYPE_LOCAL:
                label = "🟢 LOCAL" + (latency > 0 ? " " + latency + "ms" : "");
                color = 0xFF4CAF50; textColor = Color.WHITE; break;
            case LeaNetworkDetector.TYPE_CLOUDFLARE:
                label = "🔵 CLOUD" + (latency > 0 ? " " + latency + "ms" : "");
                color = 0xFF2196F3; textColor = Color.WHITE; break;
            default:
                label = "🔴 OFFLINE";
                color = 0xFFFF4444; textColor = Color.WHITE; break;
        }
        chip.setText(label);
        chip.setTextColor(textColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(12));
        chip.setBackground(bg);
    }

    private int dp(int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
