package com.flolov42.lea_v3.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.flolov42.lea_v3.R;
import com.flolov42.lea_v3.update.AppUpdate;
import com.flolov42.lea_v3.update.AppUpdateManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// Renommée depuis UpdateActivity (2026-07-12) pour la distinguer de update.UpdatePromptActivity —
// celle-ci est l'écran complet "Vérifier les mises à jour" ouvert manuellement depuis la sidebar
// (halo animé, vérification/téléchargement/installation), pas le popup auto-déclenché en arrière-plan.
public class UpdateCheckActivity extends AppCompatActivity {

    // ═══════════════════════════════════════════════════════════
    //  RÉSEAU — OkHttp IPv4 + retry
    // ═══════════════════════════════════════════════════════════

    /** Force la résolution DNS en IPv4 uniquement pour éviter les timeouts IPv6 Cloudflare. */
    private static class IPv4OnlyDns implements Dns {
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            List<InetAddress> all = Dns.SYSTEM.lookup(hostname);
            List<InetAddress> ipv4 = new ArrayList<>();
            for (InetAddress addr : all) {
                if (addr instanceof Inet4Address) ipv4.add(addr);
            }
            return ipv4.isEmpty() ? all : ipv4; // fallback si aucune IPv4
        }
    }

    /** Retry transparent avec backoff exponentiel (1 s, 2 s, 3 s…). */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetry;

        RetryInterceptor(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt < maxRetry; attempt++) {
                try {
                    Response response = chain.proceed(request);
                    if (response.isSuccessful()) return response;
                    response.close();
                } catch (IOException e) {
                    lastException = e;
                }
                if (attempt < maxRetry - 1) {
                    try {
                        Thread.sleep(1000L * (attempt + 1)); // 1 s, 2 s, 3 s
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (lastException != null) throw lastException;
            throw new IOException("Echec après " + maxRetry + " tentatives");
        }
    }

    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
            .dns(new IPv4OnlyDns())
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(new RetryInterceptor(2))  // 2 tentatives max, pas 3
            .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  CHAMPS
    // ═══════════════════════════════════════════════════════════

    private OkHttpClient httpClient;
    private AppUpdateManager updateManager;
    private final Handler ui = new Handler(Looper.getMainLooper());

    // UI
    private HaloView    haloView;
    private ProgressBar iconSpinner;
    private TextView    iconCheck;
    private TextView    iconUpdate;
    private TextView    statusTitle;
    private TextView    statusSubtext;
    private TextView    currentVersionBadge;
    private LinearLayout changelogSection;
    private TextView    changelogText;
    private LinearLayout downloadSection;
    private ProgressBar downloadProgress;
    private TextView    downloadProgressText;
    private Button      btnCheckUpdate;
    private TextView    btnLater;

    // State
    private static final String PREFS_NAME  = "lea_update_prefs";
    private static final String KEY_VERSION = "installed_version";
    private static final String DEFAULT_VER = "1.0";

    private enum State { CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, DOWNLOADING }
    private State       currentState  = State.CHECKING;
    private AppUpdate   pendingUpdate = null;

    // ═══════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
        hideSystemBars();

        httpClient    = buildHttpClient();
        updateManager = new AppUpdateManager(this);

        bindViews();
        setupListeners();
        animateHaloEntrance();
        checkForUpdates();
    }

    private void hideSystemBars() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController ctrl = getWindow().getInsetsController();
            if (ctrl != null) {
                ctrl.hide(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars());
                ctrl.setSystemBarsBehavior(
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        getWindow().setStatusBarColor(0x00000000);
        getWindow().setNavigationBarColor(0x00000000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        httpClient.dispatcher().cancelAll();
    }

    // ═══════════════════════════════════════════════════════════
    //  BIND + LISTENERS
    // ═══════════════════════════════════════════════════════════

    private void bindViews() {
        haloView            = findViewById(R.id.haloView);
        haloView.setMode(HaloView.Mode.CHECKING);
        iconSpinner         = findViewById(R.id.iconSpinner);
        iconCheck           = findViewById(R.id.iconCheck);
        iconUpdate          = findViewById(R.id.iconUpdate);
        statusTitle         = findViewById(R.id.statusTitle);
        statusSubtext       = findViewById(R.id.statusSubtext);
        currentVersionBadge = findViewById(R.id.currentVersionBadge);
        changelogSection    = findViewById(R.id.changelogSection);
        changelogText       = findViewById(R.id.changelogText);
        downloadSection     = findViewById(R.id.downloadSection);
        downloadProgress    = findViewById(R.id.downloadProgress);
        downloadProgressText = findViewById(R.id.downloadProgressText);
        btnCheckUpdate      = findViewById(R.id.btnCheckUpdate);
        btnLater            = findViewById(R.id.btnLater);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        currentVersionBadge.setText("Version installée : " + getInstalledVersion());
    }

    private void setupListeners() {
        btnCheckUpdate.setOnClickListener(v -> {
            if (currentState == State.UPDATE_AVAILABLE && pendingUpdate != null) {
                startDownload();
            } else {
                // Réessayer ou Rechercher : afficher le spinner immédiatement
                showChecking();
                checkForUpdates();
            }
        });
        btnLater.setOnClickListener(v -> finish());
    }

    // ═══════════════════════════════════════════════════════════
    //  ANIMATION ENTRÉE — halo fade-in
    // ═══════════════════════════════════════════════════════════

    private void animateHaloEntrance() {
        FrameLayout container = findViewById(R.id.haloContainer);
        container.setAlpha(0f);
        container.setScaleX(0.80f);
        container.setScaleY(0.80f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(container, "alpha",  0f, 1f),
            ObjectAnimator.ofFloat(container, "scaleX", 0.80f, 1f),
            ObjectAnimator.ofFloat(container, "scaleY", 0.80f, 1f)
        );
        set.setDuration(480);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
    }

    // ═══════════════════════════════════════════════════════════
    //  CHECK FOR UPDATES — OkHttp async, IPv4, retry 3×
    // ═══════════════════════════════════════════════════════════

    private void checkForUpdates() {
        String host = getServerHost();
        Request request = new Request.Builder()
            .url(host + "/api/app/version")
            .header("Cache-Control", "no-cache")
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return;
                // Affiche le vrai message pour diagnostiquer : SSL / DNS / timeout
                String cause = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                ui.post(() -> showError(cause));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (call.isCanceled()) return;
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        ui.post(() -> showError("Serveur inaccessible (code " + response.code() + ")"));
                        return;
                    }

                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    String latestVersion    = json.getString("latestVersion");
                    String installedVersion = getInstalledVersion();

                    if (!isNewer(latestVersion, installedVersion)) {
                        String now = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                            Locale.FRANCE).format(new Date());
                        ui.post(() -> showUpToDate(now));
                        return;
                    }

                    JSONArray updates = json.getJSONArray("updates");
                    for (int i = 0; i < updates.length(); i++) {
                        JSONObject u = updates.getJSONObject(i);
                        if (u.getString("version").equals(latestVersion)) {
                            AppUpdate update = parseUpdate(u, latestVersion);
                            ui.post(() -> showUpdateAvailable(update));
                            return;
                        }
                    }

                    // latestVersion dans le JSON mais aucun objet update correspondant
                    ui.post(() -> showUpToDate(null));

                } catch (Exception e) {
                    ui.post(() -> showError("Erreur de parsing : " + e.getMessage()));
                }
            }
        });
    }

    private AppUpdate parseUpdate(JSONObject u, String version) throws Exception {
        AppUpdate update = new AppUpdate();
        update.version     = version;
        update.name        = u.optString("name", "Léa " + version);
        update.description = u.optString("description", "");
        update.mandatory   = u.optBoolean("mandatory", false);
        update.downloadUrl = u.optString("downloadUrl", "");
        update.fileSize    = u.optString("fileSize", "");
        update.releaseDate = u.optString("releaseDate", "");

        JSONArray cl = u.optJSONArray("changelog");
        List<String> changelog = new ArrayList<>();
        if (cl != null) {
            for (int j = 0; j < cl.length(); j++) changelog.add(cl.getString(j));
        }
        update.changelog = changelog;
        return update;
    }

    // ═══════════════════════════════════════════════════════════
    //  ÉTATS UI
    // ═══════════════════════════════════════════════════════════

    private void setState(State state) {
        currentState = state;
    }

    private void showUpToDate(String checkedAt) {
        setState(State.UP_TO_DATE);
        pendingUpdate = null;

        haloView.setMode(HaloView.Mode.UPTODATE);
        iconSpinner.setVisibility(View.GONE);
        iconUpdate.setVisibility(View.GONE);
        iconCheck.setVisibility(View.VISIBLE);
        changelogSection.setVisibility(View.GONE);
        downloadSection.setVisibility(View.GONE);
        btnLater.setVisibility(View.GONE);

        iconCheck.setAlpha(0f);
        iconCheck.setScaleX(0.5f);
        iconCheck.setScaleY(0.5f);
        iconCheck.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();

        statusTitle.setText("Votre logiciel est à jour");
        statusSubtext.setText(checkedAt != null ? "Vérifié le : " + checkedAt : "");

        btnCheckUpdate.setEnabled(true);
        btnCheckUpdate.setText("Rechercher les mises à jour");
    }

    private void showUpdateAvailable(AppUpdate update) {
        setState(State.UPDATE_AVAILABLE);
        pendingUpdate = update;

        haloView.setMode(HaloView.Mode.AVAILABLE);
        iconSpinner.setVisibility(View.GONE);
        iconCheck.setVisibility(View.GONE);
        iconUpdate.setVisibility(View.VISIBLE);
        iconUpdate.setAlpha(0f);
        iconUpdate.setScaleX(0.5f);
        iconUpdate.setScaleY(0.5f);
        iconUpdate.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();

        statusTitle.setText("Mise à jour disponible");

        StringBuilder sub = new StringBuilder("v").append(update.version);
        if (!update.name.isEmpty()) sub.append(" — ").append(update.name);
        if (!update.fileSize.isEmpty()) sub.append("\n").append(update.fileSize);
        if (!update.releaseDate.isEmpty()) sub.append("  •  ").append(update.releaseDate);
        statusSubtext.setText(sub.toString());

        if (update.changelog != null && !update.changelog.isEmpty()) {
            StringBuilder cl = new StringBuilder();
            for (String item : update.changelog) cl.append("• ").append(item).append("\n");
            changelogText.setText(cl.toString().trim());
            changelogSection.setVisibility(View.VISIBLE);
        } else {
            changelogSection.setVisibility(View.GONE);
        }

        downloadSection.setVisibility(View.GONE);
        btnCheckUpdate.setEnabled(true);
        btnCheckUpdate.setText("Télécharger et installer");
        btnLater.setVisibility(update.mandatory ? View.GONE : View.VISIBLE);
    }

    private void showChecking() {
        setState(State.CHECKING);
        haloView.setMode(HaloView.Mode.CHECKING);
        iconSpinner.setVisibility(View.VISIBLE);
        iconCheck.setVisibility(View.GONE);
        iconUpdate.setVisibility(View.GONE);
        statusTitle.setText("Vérification en cours...");
        statusSubtext.setText(getServerHost());
        btnCheckUpdate.setEnabled(false);
        btnCheckUpdate.setText("Vérification...");
        btnLater.setVisibility(View.GONE);
    }

    private void showError(String message) {
        setState(State.UP_TO_DATE);
        haloView.setMode(HaloView.Mode.NONE);
        iconSpinner.setVisibility(View.GONE);
        iconUpdate.setVisibility(View.GONE);
        iconCheck.setVisibility(View.GONE);
        statusTitle.setText("Impossible de vérifier");
        // Affiche le vrai message d'erreur pour diagnostiquer (SSL ? DNS ? timeout ?)
        statusSubtext.setText(message != null ? message : "Vérifiez votre connexion.");
        btnCheckUpdate.setEnabled(true);
        btnCheckUpdate.setText("Réessayer");
        btnLater.setVisibility(View.VISIBLE);
    }

    // ═══════════════════════════════════════════════════════════
    //  TÉLÉCHARGEMENT
    // ═══════════════════════════════════════════════════════════

    private void startDownload() {
        if (pendingUpdate == null || pendingUpdate.downloadUrl.isEmpty()) {
            Toast.makeText(this, "URL de téléchargement manquante.", Toast.LENGTH_SHORT).show();
            return;
        }

        setState(State.DOWNLOADING);
        btnCheckUpdate.setEnabled(false);
        btnCheckUpdate.setText("Téléchargement...");
        btnLater.setVisibility(View.GONE);
        downloadSection.setVisibility(View.VISIBLE);
        downloadProgress.setProgress(0);
        downloadProgressText.setText("Connexion au serveur...");

        updateManager.downloadAPK(
            pendingUpdate.downloadUrl,
            getServerHost(),
            pendingUpdate.version,
            new AppUpdateManager.DownloadCallback() {
                @Override
                public void onProgress(int percent) {
                    ui.post(() -> {
                        downloadProgress.setProgress(percent);
                        downloadProgressText.setText("Téléchargement : " + percent + "%");
                    });
                }

                @Override
                public void onComplete(File apkFile) {
                    ui.post(() -> {
                        downloadProgress.setProgress(100);
                        downloadProgressText.setText("Prêt à installer");
                        btnCheckUpdate.setEnabled(true);
                        btnCheckUpdate.setText("Installer maintenant");
                        btnCheckUpdate.setOnClickListener(v -> {
                            updateManager.installAPK(apkFile);
                            finish();
                        });
                    });
                }

                @Override
                public void onError(String error) {
                    ui.post(() -> {
                        downloadSection.setVisibility(View.GONE);
                        showError("Erreur téléchargement : " + error);
                    });
                }
            }
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    private String getServerHost() {
        SharedPreferences prefs = getSharedPreferences("lea_prefs", MODE_PRIVATE);
        String host = prefs.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        // Cloudflare Tunnel expose le serveur sur le port 443 (HTTPS), pas sur le port local 3001
        if (host.startsWith("https://") && host.endsWith(":3001")) {
            host = host.substring(0, host.length() - 5);
        }
        return host;
    }

    private String getInstalledVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return DEFAULT_VER;
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private boolean isNewer(String remote, String installed) {
        try {
            String[] r = remote.split("\\.");
            String[] ins = installed.split("\\.");
            int len = Math.max(r.length, ins.length);
            for (int k = 0; k < len; k++) {
                int rv = k < r.length   ? Integer.parseInt(r[k])   : 0;
                int iv = k < ins.length ? Integer.parseInt(ins[k]) : 0;
                if (rv > iv) return true;
                if (rv < iv) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
