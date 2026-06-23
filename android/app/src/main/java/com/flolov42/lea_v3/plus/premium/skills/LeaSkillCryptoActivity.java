package com.flolov42.lea_v3.plus.premium.skills;

import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

public class LeaSkillCryptoActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int GRN  = 0xFF4CAF50;
    private static final int RED  = 0xFFF44336;
    private static final int GOLD = 0xFFFFD700;
    private static final int DIM  = 0xFF7BB8CC;

    private LinearLayout priceContainer;
    private TextView statusTv;
    private Button refreshBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LeaFeatureDetailActivity.applyImmersive(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        sv.addView(root);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(16), dp(16), dp(16), dp(8));
        Button backBtn = new Button(this);
        backBtn.setText("←"); backBtn.setBackgroundColor(Color.TRANSPARENT);
        backBtn.setTextColor(CYAN); backBtn.setTextSize(18);
        backBtn.setOnClickListener(v -> finish());
        header.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("🪙 Crypto Tracker");
        title.setTextColor(GOLD); title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(8), dp(8), 0, 0);
        header.addView(title);
        root.addView(header);

        // Status
        statusTv = new TextView(this);
        statusTv.setText("Chargement des prix...");
        statusTv.setTextColor(DIM); statusTv.setTextSize(13);
        statusTv.setPadding(dp(20), dp(4), dp(20), dp(8));
        root.addView(statusTv);

        // Prix container
        priceContainer = new LinearLayout(this);
        priceContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(priceContainer);

        // Refresh button
        refreshBtn = new Button(this);
        refreshBtn.setText("Actualiser");
        refreshBtn.setBackgroundColor(CYAN); refreshBtn.setTextColor(0xFF011627);
        refreshBtn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(dp(16), dp(8), dp(16), dp(16));
        refreshBtn.setLayoutParams(rlp);
        refreshBtn.setOnClickListener(v -> fetchPrices());
        root.addView(refreshBtn);

        // Note
        TextView note = new TextView(this);
        note.setText("Source: CoinGecko (données en temps réel)");
        note.setTextColor(DIM); note.setTextSize(11);
        note.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        note.setPadding(dp(16), 0, dp(16), dp(20));
        root.addView(note);

        setContentView(sv);
        fetchPrices();
    }

    private void fetchPrices() {
        refreshBtn.setEnabled(false);
        statusTv.setText("Récupération des prix...");
        priceContainer.removeAllViews();

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String url = "https://api.coingecko.com/api/v3/simple/price" +
                    "?ids=bitcoin,ethereum,binancecoin&vs_currencies=usd,eur&include_24hr_change=true";
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());

                StringBuilder sb = new StringBuilder(); String line;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    while ((line = reader.readLine()) != null) sb.append(line);
                }

                JSONObject json = new JSONObject(sb.toString());
                if (!isDestroyed()) runOnUiThread(() -> renderPrices(json));
            } catch (Exception e) {
                if (!isDestroyed()) runOnUiThread(() -> {
                    statusTv.setText("Erreur: " + e.getMessage() + "\n(Vérifiez votre connexion)");
                    refreshBtn.setEnabled(true);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void renderPrices(JSONObject json) {
        priceContainer.removeAllViews();
        String[] ids     = {"bitcoin",   "ethereum",  "binancecoin"};
        String[] symbols = {"BTC",       "ETH",       "BNB"};
        String[] icons   = {"₿",         "Ξ",         "🅱"};

        for (int i = 0; i < ids.length; i++) {
            JSONObject data = json.optJSONObject(ids[i]);
            if (data == null) continue;

            double usd    = data.optDouble("usd", 0);
            double eur    = data.optDouble("eur", 0);
            double change = data.optDouble("usd_24h_change", 0);

            LinearLayout card = card();

            // Ligne: icône + nom + prix USD
            LinearLayout topRow = rowH();
            TextView iconTv = new TextView(this);
            iconTv.setText(icons[i]); iconTv.setTextSize(28); iconTv.setTextColor(GOLD);
            iconTv.setPadding(0, 0, dp(12), 0);
            topRow.addView(iconTv);

            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView nameTv = new TextView(this);
            nameTv.setText(symbols[i]); nameTv.setTextColor(CYAN);
            nameTv.setTextSize(16); nameTv.setTypeface(null, Typeface.BOLD);
            infoCol.addView(nameTv);

            TextView eurTv = new TextView(this);
            eurTv.setText(String.format("€ %,.2f EUR", eur));
            eurTv.setTextColor(DIM); eurTv.setTextSize(13);
            infoCol.addView(eurTv);
            topRow.addView(infoCol);

            TextView usdTv = new TextView(this);
            usdTv.setText(String.format("$ %,.2f", usd));
            usdTv.setTextColor(Color.WHITE); usdTv.setTextSize(16);
            usdTv.setTypeface(null, Typeface.BOLD);
            topRow.addView(usdTv);
            card.addView(topRow);

            // Variation 24h
            TextView changeTv = new TextView(this);
            boolean positive = change >= 0;
            changeTv.setText((positive ? "▲ +" : "▼ ") + String.format("%.2f", change) + "% (24h)");
            changeTv.setTextColor(positive ? GRN : RED);
            changeTv.setTextSize(13);
            changeTv.setPadding(0, dp(4), 0, 0);
            card.addView(changeTv);

            priceContainer.addView(card);
        }

        statusTv.setText("Mis à jour: " + new java.text.SimpleDateFormat("HH:mm:ss",
            java.util.Locale.FRANCE).format(new java.util.Date()));
        refreshBtn.setEnabled(true);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(6), dp(12), dp(6));
        c.setLayoutParams(lp); c.setPadding(dp(16), dp(14), dp(16), dp(14));
        return c;
    }
    private LinearLayout rowH() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
