package com.flolov42.lea_v3.update;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.File;

// Renommée depuis UpdateActivity (2026-07-12) pour la distinguer de ui.UpdateCheckActivity —
// celle-ci est le popup léger déclenché automatiquement par UpdateCheckService quand une
// nouvelle version est détectée en arrière-plan, pas l'écran complet ouvert depuis la sidebar.
public class UpdatePromptActivity extends Activity {

    public static final String EXTRA_VERSION = "version";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_MANDATORY = "mandatory";
    public static final String EXTRA_DOWNLOAD_URL = "download_url";
    public static final String EXTRA_FILE_SIZE = "file_size";
    public static final String EXTRA_CHANGELOG = "changelog";

    private AppUpdateManager updateManager;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button installButton;
    private Button laterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        updateManager = new AppUpdateManager(this);

        String version = getIntent().getStringExtra(EXTRA_VERSION);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        boolean mandatory = getIntent().getBooleanExtra(EXTRA_MANDATORY, false);
        String downloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);
        String fileSize = getIntent().getStringExtra(EXTRA_FILE_SIZE);
        String changelog = getIntent().getStringExtra(EXTRA_CHANGELOG);

        // Block back button for mandatory updates
        if (mandatory) {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            );
        }

        buildUI(version, name, description, mandatory, downloadUrl, fileSize, changelog);
    }

    private void buildUI(String version, String name, String description,
                         boolean mandatory, String downloadUrl, String fileSize, String changelog) {

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF000814);
        scroll.setPadding(48, 80, 48, 48);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Badge obligatoire
        if (mandatory) {
            TextView badge = new TextView(this);
            badge.setText("⚠ MISE À JOUR OBLIGATOIRE");
            badge.setTextColor(0xFFFF5555);
            badge.setTextSize(10);
            badge.setPadding(0, 0, 0, 16);
            layout.addView(badge);
        }

        // Titre
        TextView title = new TextView(this);
        title.setText("🆕 " + name + " (v" + version + ")");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 12);
        layout.addView(title);

        // Description
        if (description != null && !description.isEmpty()) {
            TextView desc = new TextView(this);
            desc.setText(description);
            desc.setTextColor(0xFF9EAAB8);
            desc.setTextSize(13);
            desc.setPadding(0, 0, 0, 16);
            layout.addView(desc);
        }

        // Taille
        if (fileSize != null && !fileSize.isEmpty()) {
            TextView size = new TextView(this);
            size.setText("Taille : " + fileSize);
            size.setTextColor(0xFF00F2FF);
            size.setTextSize(12);
            size.setPadding(0, 0, 0, 16);
            layout.addView(size);
        }

        // Changelog
        if (changelog != null && !changelog.isEmpty()) {
            TextView cl = new TextView(this);
            cl.setText("Nouveautés :\n" + changelog);
            cl.setTextColor(0xFFCCCCCC);
            cl.setTextSize(12);
            cl.setPadding(0, 0, 0, 24);
            layout.addView(cl);
        }

        // Progress bar (caché au départ)
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        progressBar.setPadding(0, 0, 0, 8);
        layout.addView(progressBar);

        statusText = new TextView(this);
        statusText.setTextColor(0xFF9EAAB8);
        statusText.setTextSize(12);
        statusText.setVisibility(View.GONE);
        statusText.setPadding(0, 0, 0, 16);
        layout.addView(statusText);

        // Bouton installer
        installButton = new Button(this);
        installButton.setText("⬇ Installer la mise à jour");
        installButton.setTextColor(0xFF000814);
        installButton.setBackgroundColor(0xFF00F2FF);
        installButton.setPadding(0, 24, 0, 24);
        installButton.setOnClickListener(v -> startDownload(downloadUrl, version));
        layout.addView(installButton);

        // Bouton "Plus tard" (masqué si obligatoire)
        laterButton = new Button(this);
        laterButton.setText("Plus tard");
        laterButton.setTextColor(0xFF9EAAB8);
        laterButton.setBackgroundColor(0x00000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 16, 0, 0);
        laterButton.setLayoutParams(lp);
        laterButton.setOnClickListener(v -> finish());
        if (mandatory) laterButton.setVisibility(View.GONE);
        layout.addView(laterButton);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void startDownload(String url, String version) {
        installButton.setEnabled(false);
        installButton.setText("Téléchargement...");
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Connexion au serveur...");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        updateManager.downloadAPK(url, getServerHost(), version, new AppUpdateManager.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    progressBar.setProgress(percent);
                    statusText.setText("Téléchargement : " + percent + "%");
                });
            }

            @Override
            public void onComplete(File apkFile) {
                runOnUiThread(() -> {
                    progressBar.setProgress(100);
                    statusText.setText("Prêt à installer");
                    installButton.setText("✅ Installer maintenant");
                    installButton.setEnabled(true);
                    installButton.setOnClickListener(v -> {
                        updateManager.installAPK(apkFile);
                        finish();
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    statusText.setText("Erreur : " + error);
                    installButton.setText("⬇ Réessayer");
                    installButton.setEnabled(true);
                    installButton.setOnClickListener(v -> startDownload(url, version));
                });
            }
        });
    }

    private String getServerHost() {
        SharedPreferences prefs = getSharedPreferences("lea_prefs", MODE_PRIVATE);
        String host = prefs.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        if (host.startsWith("https://") && host.endsWith(":3001")) {
            host = host.substring(0, host.length() - 5);
        }
        return host;
    }

    @Override
    public void onBackPressed() {
        // Prevent back if coming from mandatory check — laterButton handles dismiss
    }
}
