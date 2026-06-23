package com.flolov42.lea_v3.studio;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import com.flolov42.lea_v3.utilities.LeaNetworkConfig;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LeaForge3DActivity extends Activity {

    static class HistoryItem {
        String filename, url, format;
        long size;
    }

    // ──────────────────────────────────────────────────────────────────────────
    private String currentUser = "";
    private String serverUrl   = "";
    private String quality     = "high";

    private EditText     promptInput;
    private Button       btnDraft, btnHigh, btnForge, btnDownload;
    private TextView     tokensLabel, statusLabel;
    private ProgressBar  progressBar;
    private View         emptyViewport;
    private LinearLayout forgeResultCard;
    private TextView     resultLabel;
    private LinearLayout downloadBar;
    private LinearLayout historyContainer;
    private TextView     historyEmpty;

    private String lastGlbUrl = null;

    private final List<HistoryItem> history = new ArrayList<>();

    private final OkHttpClient http = new OkHttpClient.Builder()
        .callTimeout(16, java.util.concurrent.TimeUnit.MINUTES).build();
    private final Handler ui = new Handler(Looper.getMainLooper());

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.parseColor("#010a17"));
        getWindow().setNavigationBarColor(Color.parseColor("#010a17"));

        currentUser = getIntent().getStringExtra("currentUser");
        if (currentUser == null) currentUser = "";

        android.content.SharedPreferences prefs = getSharedPreferences("CapacitorStorage", MODE_PRIVATE);
        serverUrl = prefs.getString("server_host", "");
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = LeaNetworkConfig.CLOUDFLARE_HTTP;
        }

        setContentView(buildLayout());
        fetchTokens();
        fetchHistory();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private View buildLayout() {
        float dp = getResources().getDisplayMetrics().density;

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Color.parseColor("#00050b"));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.addView(buildHeader(dp));
        inner.addView(buildPromptSection(dp));
        inner.addView(buildViewport(dp));
        inner.addView(buildActions(dp));
        inner.addView(buildHistory(dp));

        sv.addView(inner, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    private View buildHeader(float dp) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setBackgroundColor(Color.parseColor("#010a17"));
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(px(16,dp), px(10,dp), px(16,dp), px(10,dp));

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setBackground(null);
        back.setColorFilter(Color.parseColor("#22d3ee"));
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(px(36,dp), px(36,dp)));

        TextView title = new TextView(this);
        title.setText("FORGE 3D");
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD_ITALIC);
        title.setTextSize(13f);
        title.setLetterSpacing(0.2f);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMarginStart(px(10,dp));
        h.addView(title, tp);

        TextView cpu = new TextView(this);
        cpu.setText("CPU");
        cpu.setTextColor(Color.parseColor("#22d3ee"));
        cpu.setTextSize(9f);
        cpu.setBackground(roundBg(Color.parseColor("#0e2435"), Color.TRANSPARENT, px(4,dp)));
        cpu.setPadding(px(6,dp), px(3,dp), px(6,dp), px(3,dp));
        h.addView(cpu);

        tokensLabel = new TextView(this);
        tokensLabel.setText("… 🪙");
        tokensLabel.setTextColor(Color.parseColor("#facc15"));
        tokensLabel.setTypeface(null, Typeface.BOLD);
        tokensLabel.setTextSize(12f);
        LinearLayout.LayoutParams tklp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tklp.setMarginStart(px(10,dp));
        h.addView(tokensLabel, tklp);
        return h;
    }

    private View buildPromptSection(float dp) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(px(16,dp), px(12,dp), px(16,dp), px(12,dp));
        s.setBackgroundColor(Color.parseColor("#00050b"));

        promptInput = new EditText(this);
        promptInput.setHint("Ex : Une épée cyberpunk avec des runes lumineuses...");
        promptInput.setHintTextColor(Color.parseColor("#64748b"));
        promptInput.setTextColor(Color.parseColor("#cbd5e1"));
        promptInput.setBackground(roundBg(Color.parseColor("#020d1f"), Color.parseColor("#1e293b"), px(12,dp)));
        promptInput.setPadding(px(14,dp), px(12,dp), px(14,dp), px(12,dp));
        promptInput.setTextSize(13f);
        promptInput.setMinLines(3);
        promptInput.setMaxLines(5);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        s.addView(promptInput, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout qRow = new LinearLayout(this);
        qRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams qrp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qrp.topMargin = px(10,dp);

        btnDraft = qualityBtn("Low-Poly  30🪙", false, dp);
        btnHigh  = qualityBtn("High-Poly 90🪙", true,  dp);
        btnDraft.setOnClickListener(v -> setQuality("draft", dp));
        btnHigh .setOnClickListener(v -> setQuality("high",  dp));

        LinearLayout.LayoutParams qbp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        qbp.setMarginEnd(px(6,dp));
        qRow.addView(btnDraft, qbp);
        qRow.addView(btnHigh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        s.addView(qRow, qrp);
        return s;
    }

    private View buildViewport(float dp) {
        FrameLayout vp = new FrameLayout(this);
        vp.setMinimumHeight(px(220,dp));
        vp.setBackgroundColor(Color.parseColor("#000308"));

        emptyViewport = new TextView(this);
        ((TextView)emptyViewport).setText("VIEWPORT VIDE");
        ((TextView)emptyViewport).setTextColor(Color.parseColor("#334155"));
        ((TextView)emptyViewport).setTextSize(12f);
        ((TextView)emptyViewport).setLetterSpacing(0.2f);
        ((TextView)emptyViewport).setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams fullFp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, px(220,dp));
        fullFp.gravity = Gravity.CENTER;
        vp.addView(emptyViewport, fullFp);

        forgeResultCard = new LinearLayout(this);
        forgeResultCard.setOrientation(LinearLayout.VERTICAL);
        forgeResultCard.setGravity(Gravity.CENTER);
        forgeResultCard.setVisibility(View.GONE);
        resultLabel = new TextView(this);
        resultLabel.setTextColor(Color.parseColor("#22d3ee"));
        resultLabel.setTextSize(13f);
        resultLabel.setGravity(Gravity.CENTER);
        resultLabel.setPadding(px(24,dp), 0, px(24,dp), 0);
        Button btnView3D = new Button(this);
        btnView3D.setText("Voir en 3D");
        btnView3D.setTextColor(Color.WHITE);
        btnView3D.setTextSize(13f);
        btnView3D.setBackground(roundBg(Color.parseColor("#0e2d3d"), Color.parseColor("#22d3ee"), px(12,dp)));
        LinearLayout.LayoutParams vbp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vbp.topMargin = px(16,dp);
        btnView3D.setOnClickListener(v -> { if (lastGlbUrl != null) openModel(lastGlbUrl); });
        forgeResultCard.addView(resultLabel);
        forgeResultCard.addView(btnView3D, vbp);
        FrameLayout.LayoutParams cardFp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, px(220,dp));
        cardFp.gravity = Gravity.CENTER;
        vp.addView(forgeResultCard, cardFp);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setVisibility(View.GONE);
        if (android.os.Build.VERSION.SDK_INT >= 21)
            progressBar.setIndeterminateTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#22d3ee")));
        FrameLayout.LayoutParams pbp = new FrameLayout.LayoutParams(px(64,dp), px(64,dp));
        pbp.gravity = Gravity.CENTER;
        vp.addView(progressBar, pbp);

        statusLabel = new TextView(this);
        statusLabel.setTextColor(Color.parseColor("#22d3ee"));
        statusLabel.setTextSize(11f);
        statusLabel.setGravity(Gravity.CENTER);
        statusLabel.setPadding(px(24,dp), px(80,dp), px(24,dp), 0);
        statusLabel.setVisibility(View.GONE);
        vp.addView(statusLabel, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        return vp;
    }

    private View buildActions(float dp) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(px(16,dp), px(12,dp), px(16,dp), px(12,dp));
        s.setBackgroundColor(Color.parseColor("#010a17"));

        btnForge = new Button(this);
        btnForge.setText("FORGER · 90 🪙");
        btnForge.setTextColor(Color.parseColor("#00050b"));
        btnForge.setTextSize(11f);
        btnForge.setAllCaps(false);
        btnForge.setTypeface(null, Typeface.BOLD);
        btnForge.setBackground(roundBg(Color.parseColor("#0891b2"), Color.TRANSPARENT, px(12,dp)));
        btnForge.setPadding(0, px(14,dp), 0, px(14,dp));
        btnForge.setOnClickListener(v -> startForge());
        s.addView(btnForge, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        downloadBar = new LinearLayout(this);
        downloadBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = px(10,dp);
        btnDownload = new Button(this);
        btnDownload.setText("Télécharger .glb");
        btnDownload.setTextColor(Color.parseColor("#22d3ee"));
        btnDownload.setTextSize(12f);
        btnDownload.setBackground(roundBg(Color.TRANSPARENT, Color.parseColor("#22d3ee"), px(12,dp)));
        btnDownload.setOnClickListener(v -> downloadModel());
        downloadBar.addView(btnDownload, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        s.addView(downloadBar, dlp);
        return s;
    }

    private View buildHistory(float dp) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setBackgroundColor(Color.parseColor("#000308"));

        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setBackgroundColor(Color.parseColor("#010a17"));
        hdr.setPadding(px(16,dp), px(10,dp), px(16,dp), px(10,dp));

        TextView htitle = new TextView(this);
        htitle.setText("HISTORIQUE");
        htitle.setTextColor(Color.parseColor("#22d3ee"));
        htitle.setTextSize(9f);
        htitle.setLetterSpacing(0.2f);
        hdr.addView(htitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnRef = new Button(this);
        btnRef.setText("↻");
        btnRef.setBackground(null);
        btnRef.setTextColor(Color.parseColor("#64748b"));
        btnRef.setOnClickListener(v -> fetchHistory());
        hdr.addView(btnRef, new LinearLayout.LayoutParams(px(36,dp), px(36,dp)));
        s.addView(hdr);

        historyEmpty = new TextView(this);
        historyEmpty.setText("Aucun objet forgé");
        historyEmpty.setTextColor(Color.parseColor("#475569"));
        historyEmpty.setTextSize(10f);
        historyEmpty.setGravity(Gravity.CENTER);
        historyEmpty.setPadding(0, px(16,dp), 0, px(16,dp));
        s.addView(historyEmpty, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        s.addView(historyContainer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return s;
    }

    // ── Forge ─────────────────────────────────────────────────────────────────

    private void startForge() {
        String prompt = promptInput.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Décris l'objet à forger", Toast.LENGTH_SHORT).show();
            return;
        }
        btnForge.setEnabled(false);
        btnForge.setText("Forge en cours…");
        progressBar.setVisibility(View.VISIBLE);
        statusLabel.setText("Traduction · libération VRAM · démarrage Shap-E…");
        statusLabel.setVisibility(View.VISIBLE);
        emptyViewport.setVisibility(View.GONE);
        forgeResultCard.setVisibility(View.GONE);
        downloadBar.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                // Étape 1 : démarrer le job (réponse immédiate avec jobId)
                JSONObject body = new JSONObject();
                body.put("prompt", prompt);
                body.put("username", currentUser);
                body.put("quality", quality);

                Request startReq = new Request.Builder()
                    .url(serverUrl + "/api/forge/generate")
                    .post(RequestBody.create(body.toString(),
                        MediaType.parse("application/json; charset=utf-8")))
                    .build();

                String jobId;
                try (Response resp = http.newCall(startReq).execute()) {
                    String raw = resp.body() != null ? resp.body().string() : "{}";
                    JSONObject data = new JSONObject(raw);
                    if (!resp.isSuccessful()) {
                        String err = data.optString("error", "Erreur serveur");
                        ui.post(() -> onError(err));
                        return;
                    }
                    jobId = data.optString("jobId", "");
                }
                if (jobId.isEmpty()) { ui.post(() -> onError("jobId manquant")); return; }

                // Étape 2 : poll le statut toutes les 3s
                while (true) {
                    Thread.sleep(3000);
                    Request pollReq = new Request.Builder()
                        .url(serverUrl + "/api/forge/status/" + jobId).get().build();
                    try (Response resp = http.newCall(pollReq).execute()) {
                        String raw = resp.body() != null ? resp.body().string() : "{}";
                        JSONObject job = new JSONObject(raw);
                        String status = job.optString("status", "running");
                        if ("done".equals(status)) {
                            lastGlbUrl = serverUrl + job.optString("model_url");
                            int vertices = job.optInt("vertices", 0);
                            ui.post(() -> onSuccess(vertices));
                            break;
                        } else if ("error".equals(status)) {
                            String err = job.optString("error", "Génération échouée");
                            ui.post(() -> onError(err));
                            break;
                        } else if ("cancelled".equals(status)) {
                            ui.post(() -> onError("Annulé"));
                            break;
                        }
                        // "running" → on continue à poller
                        ui.post(() -> statusLabel.setText("Génération en cours…"));
                    }
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                ui.post(() -> onError(msg));
            }
        }).start();
    }

    private void onSuccess(int vertices) {
        progressBar.setVisibility(View.GONE);
        statusLabel.setVisibility(View.GONE);
        btnForge.setEnabled(true);
        btnForge.setText("FORGER · " + (quality.equals("draft") ? "30" : "90") + " 🪙");
        resultLabel.setText("Modèle prêt — " + vertices + " sommets");
        forgeResultCard.setVisibility(View.VISIBLE);
        downloadBar.setVisibility(View.VISIBLE);
        fetchTokens();
        fetchHistory();
    }

    private void onError(String msg) {
        progressBar.setVisibility(View.GONE);
        statusLabel.setVisibility(View.GONE);
        emptyViewport.setVisibility(View.VISIBLE);
        btnForge.setEnabled(true);
        btnForge.setText("FORGER · " + (quality.equals("draft") ? "30" : "90") + " 🪙");
        Toast.makeText(this, "Erreur : " + msg, Toast.LENGTH_LONG).show();
        fetchHistory();
    }

    private void openModel(String url) {
        try {
            Intent intent = new Intent(this, com.flolov42.lea_v3.ui.LeaModelViewerActivity.class);
            intent.putExtra("modelUrl", url);
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    // ── Téléchargement ────────────────────────────────────────────────────────

    private void downloadModel() {
        if (lastGlbUrl == null) return;
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(lastGlbUrl));
        req.setTitle("Forge 3D — modèle.glb");
        req.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "lea_forge3d_" + System.currentTimeMillis() + ".glb");
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (dm != null) dm.enqueue(req);
        btnDownload.setText("Téléchargé ✓");
        btnDownload.setTextColor(Color.parseColor("#4ade80"));
    }

    // ── Réseau ────────────────────────────────────────────────────────────────

    private void fetchTokens() {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                    .url(serverUrl + "/api/user/profile/" + currentUser).get().build();
                try (Response resp = http.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) return;
                    JSONObject p = new JSONObject(resp.body().string());
                    double t = p.optDouble("tokens", -1);
                    String display = t < 0 ? "…" : t == Double.POSITIVE_INFINITY ? "∞" : String.valueOf((long)t);
                    ui.post(() -> tokensLabel.setText(display + " 🪙"));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void fetchHistory() {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                    .url(serverUrl + "/api/forge/history/" + currentUser).get().build();
                try (Response resp = http.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) return;
                    JSONArray arr = new JSONArray(resp.body().string());
                    history.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        HistoryItem item = new HistoryItem();
                        item.filename = o.optString("filename");
                        item.url      = serverUrl + o.optString("url");
                        item.format   = o.optString("format", "GLB");
                        item.size     = o.optLong("size", 0);
                        history.add(item);
                    }
                    ui.post(this::refreshHistoryUI);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void refreshHistoryUI() {
        float dp = getResources().getDisplayMetrics().density;
        historyContainer.removeAllViews();
        historyEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
        for (HistoryItem item : history) {
            historyContainer.addView(buildHistoryRow(item, dp));
        }
    }

    private View buildHistoryRow(HistoryItem item, float dp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(12,dp), px(10,dp), px(12,dp), px(10,dp));
        row.setBackground(roundBg(Color.parseColor("#050d1a"), Color.parseColor("#1e293b"), px(8,dp)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(px(8,dp), px(3,dp), px(8,dp), px(3,dp));
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(item.filename);
        name.setTextColor(Color.WHITE);
        name.setTextSize(11f);
        name.setTypeface(null, Typeface.BOLD);
        TextView meta = new TextView(this);
        meta.setText(String.format("%.1f Mo · .%s", item.size / 1024f / 1024f, item.format.toLowerCase()));
        meta.setTextColor(Color.parseColor("#64748b"));
        meta.setTextSize(9f);
        info.addView(name);
        info.addView(meta);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnOpen = new Button(this);
        btnOpen.setText("3D");
        btnOpen.setTextColor(Color.parseColor("#22d3ee"));
        btnOpen.setTextSize(10f);
        btnOpen.setBackground(roundBg(Color.parseColor("#0e2d3d"), Color.parseColor("#22d3ee"), px(8,dp)));
        btnOpen.setPadding(px(10,dp), px(6,dp), px(10,dp), px(6,dp));
        final String url = item.url;
        btnOpen.setOnClickListener(v -> openModel(url));
        row.addView(btnOpen, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return row;
    }

    // ── Qualité ───────────────────────────────────────────────────────────────

    private void setQuality(String q, float dp) {
        quality = q;
        boolean draft = q.equals("draft");
        btnDraft.setBackground(roundBg(draft ? Color.parseColor("#0e2d3d") : Color.parseColor("#0a0f1a"),
            draft ? Color.parseColor("#22d3ee") : Color.parseColor("#1e293b"), px(12,dp)));
        btnHigh.setBackground(roundBg(draft ? Color.parseColor("#0a0f1a") : Color.parseColor("#0e2d3d"),
            draft ? Color.parseColor("#1e293b") : Color.parseColor("#22d3ee"), px(12,dp)));
        btnForge.setText("FORGER · " + (draft ? "30" : "90") + " 🪙");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Button qualityBtn(String label, boolean selected, float dp) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(11f);
        b.setPadding(px(8,dp), px(10,dp), px(8,dp), px(10,dp));
        b.setBackground(roundBg(
            selected ? Color.parseColor("#0e2d3d") : Color.parseColor("#0a0f1a"),
            selected ? Color.parseColor("#22d3ee") : Color.parseColor("#1e293b"),
            px(12,dp)));
        return b;
    }

    private GradientDrawable roundBg(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        if (stroke != Color.TRANSPARENT) d.setStroke(2, stroke);
        d.setCornerRadius(radius);
        return d;
    }

    private int px(int dpVal, float density) { return Math.round(dpVal * density); }
}
