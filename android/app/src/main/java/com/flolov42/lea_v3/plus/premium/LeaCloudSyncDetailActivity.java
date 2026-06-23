package com.flolov42.lea_v3.plus.premium;

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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LeaCloudSyncDetailActivity extends LeaFeatureDetailActivity {

    private static final int PICK_FILE = 101;
    private static final int BG_DARK   = 0xFF000D1A;
    private static final int CARD_CLR  = 0xFF001A2E;
    private static final int CYAN_C    = 0xFF00E5FF;
    private static final int PURPLE    = 0xFF7C3AED;
    private static final int GREEN_C   = 0xFF10B981;
    private static final int ORANGE    = 0xFFF59E0B;
    private static final int RED_C     = 0xFFEF4444;
    private static final int DIM_C     = 0xFF64748B;
    private static final int WHITE     = 0xFFFFFFFF;

    private LinearLayout fileListLayout;
    private TextView storageText;
    private TextView fileCountText;
    private View storageProgressFill;
    private ScrollView mainScrollView;
    private List<JSONObject> currentFiles = new ArrayList<>();
    private String currentFolder = "";
    private TextView breadcrumbTv;

    @Override protected String getFeatureId() { return LeaPlusDatabase.CLOUD_SYNC; }

    @Override
    protected void buildContent(LinearLayout parent) {
        parent.setBackgroundColor(BG_DARK);
        parent.setPadding(0, 0, 0, 0);

        // ── Barre de stockage ──────────────────────────────────────────────
        LinearLayout storageBar = new LinearLayout(this);
        storageBar.setOrientation(LinearLayout.VERTICAL);
        storageBar.setBackgroundColor(CARD_CLR);
        storageBar.setPadding(dp(16), dp(12), dp(16), dp(14));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(-1, -2);
        sbLp.setMargins(dp(12), dp(8), dp(12), 0);
        storageBar.setLayoutParams(sbLp);

        LinearLayout storageRow = new LinearLayout(this);
        storageRow.setOrientation(LinearLayout.HORIZONTAL);
        storageRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView cloudIcon = makeText("☁️", 22, WHITE, Typeface.BOLD);
        cloudIcon.setPadding(0, 0, dp(8), 0);
        storageRow.addView(cloudIcon);

        LinearLayout storageTexts = new LinearLayout(this);
        storageTexts.setOrientation(LinearLayout.VERTICAL);
        storageTexts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        storageText = makeText("Chargement…", 13, WHITE, Typeface.BOLD);
        fileCountText = makeText("", 10, DIM_C, Typeface.NORMAL);
        storageTexts.addView(storageText);
        storageTexts.addView(fileCountText);
        storageRow.addView(storageTexts);

        // Bouton upload
        Button uploadBtn = roundButton("+ Ajouter", CYAN_C, 0xFF002233);
        uploadBtn.setOnClickListener(v -> pickFile());
        storageRow.addView(uploadBtn);

        storageBar.addView(storageRow);

        // Barre de progression stockage
        FrameLayout progressTrack = new FrameLayout(this);
        progressTrack.setBackgroundColor(0xFF002033);
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(-1, dp(8));
        ptLp.setMargins(0, dp(10), 0, 0);
        progressTrack.setLayoutParams(ptLp);
        storageProgressFill = new View(this);
        storageProgressFill.setBackgroundColor(CYAN_C);
        storageProgressFill.setLayoutParams(new FrameLayout.LayoutParams(0, -1));
        progressTrack.addView(storageProgressFill);
        storageBar.addView(progressTrack);

        parent.addView(storageBar);

        // ── Breadcrumb + dossier courant ───────────────────────────────────
        LinearLayout bcRow = new LinearLayout(this);
        bcRow.setOrientation(LinearLayout.HORIZONTAL);
        bcRow.setGravity(Gravity.CENTER_VERTICAL);
        bcRow.setPadding(dp(16), dp(10), dp(16), dp(6));

        breadcrumbTv = makeText("📁 Mes fichiers", 12, CYAN_C, Typeface.BOLD);
        breadcrumbTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        breadcrumbTv.setOnClickListener(v -> { currentFolder = ""; refreshFiles(); });
        bcRow.addView(breadcrumbTv);

        TextView newFolderBtn = makeText("＋ Dossier", 10, PURPLE, Typeface.BOLD);
        newFolderBtn.setOnClickListener(v -> showNewFolderDialog());
        LinearLayout.LayoutParams nfLp = new LinearLayout.LayoutParams(-2, dp(28));
        newFolderBtn.setLayoutParams(nfLp);
        bcRow.addView(newFolderBtn);
        parent.addView(bcRow);

        // Séparateur
        View sep = new View(this);
        sep.setBackgroundColor(0xFF001A2E);
        sep.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        parent.addView(sep);

        // ── Liste des fichiers ─────────────────────────────────────────────
        mainScrollView = new ScrollView(this);
        mainScrollView.setBackgroundColor(BG_DARK);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(-1, -1);
        svLp.setMargins(0, 0, 0, 0);
        mainScrollView.setLayoutParams(svLp);
        ScrollView sv = mainScrollView;

        fileListLayout = new LinearLayout(this);
        fileListLayout.setOrientation(LinearLayout.VERTICAL);
        fileListLayout.setPadding(dp(10), dp(4), dp(10), dp(80));
        sv.addView(fileListLayout);
        parent.addView(sv);

        // Chargement initial
        refreshFiles();
    }

    // ──────────────────────────────────────────────────────────────────────
    // CHARGEMENT DE LA LISTE
    // ──────────────────────────────────────────────────────────────────────

    private void refreshFiles() {
        fileListLayout.removeAllViews();
        showLoading();
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder()
                    .url(getServerUrl() + "/api/cloud/files/" + getUsername())
                    .get().build();
                try (Response response = client.newCall(req).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray arr = json.getJSONArray("files");
                        long totalSize  = json.optLong("totalSize", 0);
                        long quotaBytes = json.optLong("quotaBytes", 0);
                        boolean hasQuota = json.optBoolean("hasQuota", false);
                        int total = arr.length();

                        currentFiles.clear();
                        List<String> folders = new ArrayList<>();
                        List<JSONObject> files = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject f = arr.getJSONObject(i);
                            String fileFolder = f.optString("folder", "");
                            if (!fileFolder.equals(currentFolder)) {
                                if (fileFolder.startsWith(currentFolder.isEmpty() ? "" : currentFolder + "/") || (!currentFolder.isEmpty() && fileFolder.startsWith(currentFolder))) {
                                    String rel = currentFolder.isEmpty() ? fileFolder : fileFolder.substring(currentFolder.length()).replaceAll("^/", "");
                                    String topFolder = rel.split("/")[0];
                                    if (!topFolder.isEmpty() && !folders.contains(topFolder))
                                        folders.add(topFolder);
                                }
                                continue;
                            }
                            currentFiles.add(f);
                            files.add(f);
                        }

                        final float storagePct;
                        final String storageLabel;
                        if (hasQuota && quotaBytes > 0) {
                            storagePct = Math.min(1f, (float) totalSize / quotaBytes);
                            String pctStr = storagePct < 0.0001f
                                ? String.format(Locale.FRENCH, "%.4f%%", storagePct * 100)
                                : storagePct < 0.01f
                                    ? String.format(Locale.FRENCH, "%.3f%%", storagePct * 100)
                                    : String.format(Locale.FRENCH, "%.1f%%", storagePct * 100);
                            storageLabel = formatSize(totalSize) + " / 300 Go  ·  " + pctStr;
                        } else {
                            storagePct = 0f;
                            storageLabel = formatSize(totalSize) + " — Sans abonnement";
                        }
                        final String countLabel = total + " fichier" + (total > 1 ? "s" : "") + " dans le cloud";
                        final List<String> fFolders = folders;
                        final List<JSONObject> fFiles = files;

                        runOnUiThread(() -> {
                            storageText.setText(storageLabel);
                            fileCountText.setText(countLabel);
                            int fillColor = storagePct > 0.85f ? RED_C : storagePct > 0.6f ? ORANGE : CYAN_C;
                            storageProgressFill.setBackgroundColor(fillColor);
                            // post() : attend le layout pour avoir la vraie largeur du parent
                            storageProgressFill.post(() -> {
                                ViewGroup track = (ViewGroup) storageProgressFill.getParent();
                                if (track != null && track.getWidth() > 0) {
                                    FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(
                                        Math.max(1, (int)(storagePct * track.getWidth())), -1);
                                    storageProgressFill.setLayoutParams(fillLp);
                                }
                            });
                            updateBreadcrumb();
                            renderFileList(fFolders, fFiles);
                            if (mainScrollView != null) mainScrollView.scrollTo(0, 0);
                        });
                    } else {
                        runOnUiThread(() -> showError("Serveur non joignable"));
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void renderFileList(List<String> folders, List<JSONObject> files) {
        fileListLayout.removeAllViews();

        if (folders.isEmpty() && files.isEmpty()) {
            showEmpty();
            return;
        }

        // Dossiers en premier
        for (String folder : folders) {
            fileListLayout.addView(makeFolderRow(folder));
        }

        // Séparateur si les deux existent
        if (!folders.isEmpty() && !files.isEmpty()) {
            View sep = new View(this);
            sep.setBackgroundColor(0xFF001A2E);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
            lp.setMargins(dp(40), dp(4), 0, dp(4));
            sep.setLayoutParams(lp);
            fileListLayout.addView(sep);
        }

        // Fichiers
        for (JSONObject f : files) {
            fileListLayout.addView(makeFileRow(f));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // LIGNES DOSSIER / FICHIER
    // ──────────────────────────────────────────────────────────────────────

    private View makeFolderRow(String folderName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(2), 0, dp(2));
        row.setLayoutParams(lp);
        row.setBackgroundColor(CARD_CLR);

        TextView icon = makeText("📁", 20, WHITE, Typeface.NORMAL);
        icon.setMinWidth(dp(36));
        row.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        texts.addView(makeText(folderName, 13, WHITE, Typeface.BOLD));
        texts.addView(makeText("Dossier", 10, DIM_C, Typeface.NORMAL));
        row.addView(texts);

        TextView arrow = makeText("›", 22, DIM_C, Typeface.BOLD);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            currentFolder = currentFolder.isEmpty() ? folderName : currentFolder + "/" + folderName;
            refreshFiles();
        });
        return row;
    }

    private View makeFileRow(JSONObject f) {
        String name = f.optString("name", "");
        String ext  = f.optString("ext", "").toLowerCase();
        long size   = f.optLong("size", 0);
        long modMs  = f.optLong("modifiedAt", 0);
        String path = f.optString("path", name);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(2), 0, dp(2));
        row.setLayoutParams(lp);
        row.setBackgroundColor(CARD_CLR);

        // Icône par type
        String emoji = fileEmoji(ext);
        int iconColor = fileColor(ext);
        TextView icon = makeText(emoji, 20, iconColor, Typeface.NORMAL);
        icon.setMinWidth(dp(36));
        row.addView(icon);

        // Nom + infos
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        texts.addView(makeText(name, 13, WHITE, Typeface.NORMAL));
        String info = formatSize(size);
        if (modMs > 0) info += "  ·  " + new SimpleDateFormat("dd/MM/yy", Locale.FRENCH).format(new Date(modMs));
        texts.addView(makeText(info, 10, DIM_C, Typeface.NORMAL));
        row.addView(texts);

        // Bouton menu (⋮)
        Button menu = new Button(this);
        menu.setText("⋮");
        menu.setTextColor(DIM_C);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setTextSize(18);
        menu.setMinWidth(dp(36));
        menu.setMinHeight(dp(36));
        menu.setPadding(dp(4), 0, dp(4), 0);
        menu.setOnClickListener(v -> showFileMenu(name, path, ext));
        row.addView(menu);

        row.setOnClickListener(v -> showFileMenu(name, path, ext));
        return row;
    }

    // ──────────────────────────────────────────────────────────────────────
    // MENU FICHIER
    // ──────────────────────────────────────────────────────────────────────

    private void showFileMenu(String name, String filePath, String ext) {
        new AlertDialog.Builder(this)
            .setTitle(fileEmoji(ext) + " " + name)
            .setItems(new String[]{ "⬇️  Télécharger", "🗑️  Supprimer", "✖  Annuler" }, (d, which) -> {
                if (which == 0) downloadFile(filePath, name);
                else if (which == 1) confirmDelete(filePath, name);
            })
            .show();
    }

    private void downloadFile(String filePath, String name) {
        showToast("⬇️ Téléchargement en cours…");
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder()
                    .url(getServerUrl() + "/api/cloud/files/" + getUsername() + "/base64/" + Uri.encode(filePath, "/"))
                    .get().build();
                try (Response response = client.newCall(req).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> showToast("❌ Téléchargement échoué"));
                        return;
                    }
                    JSONObject json = new JSONObject(response.body().string());
                    byte[] data = Base64.decode(json.getString("content"), Base64.DEFAULT);
                    File dl = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), name);
                    try (FileOutputStream fos = new FileOutputStream(dl)) { fos.write(data); }
                    runOnUiThread(() -> showToast("✅ Enregistré dans Téléchargements : " + name));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showToast("❌ Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private void confirmDelete(String filePath, String name) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer " + name + " ?")
            .setMessage("Ce fichier sera supprimé du cloud.")
            .setPositiveButton("Supprimer", (d, w) -> deleteCloudFile(filePath))
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deleteCloudFile(String filePath) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder()
                    .url(getServerUrl() + "/api/cloud/files/" + getUsername() + "/" + Uri.encode(filePath, "/"))
                    .delete().build();
                try (Response response = client.newCall(req).execute()) {
                    runOnUiThread(() -> {
                        showToast(response.isSuccessful() ? "🗑️ Fichier supprimé" : "❌ Erreur suppression");
                        if (response.isSuccessful()) refreshFiles();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> showToast("❌ " + e.getMessage()));
            }
        }).start();
    }

    // ──────────────────────────────────────────────────────────────────────
    // UPLOAD
    // ──────────────────────────────────────────────────────────────────────

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        String name = getFileName(uri);
        showToast("☁️ Envoi de " + name + " en cours…");
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) { runOnUiThread(() -> showToast("❌ Impossible de lire le fichier")); return; }
                byte[] bytes = is.readAllBytes();
                is.close();
                String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                JSONObject body = new JSONObject();
                body.put("username", getUsername());
                body.put("filename", name);
                body.put("content", b64);
                body.put("folder", currentFolder);

                OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                Request req = new Request.Builder()
                    .url(getServerUrl() + "/api/cloud/files/upload")
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                    .build();
                try (Response response = client.newCall(req).execute()) {
                    if (response.isSuccessful()) {
                        String sizeTxt = formatSize(bytes.length);
                        runOnUiThread(() -> {
                            showToast("✅ " + name + " (" + sizeTxt + ") envoyé !");
                            refreshFiles();
                        });
                    } else {
                        runOnUiThread(() -> showToast("❌ Erreur serveur"));
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> showToast("❌ " + e.getMessage()));
            }
        }).start();
    }

    // ──────────────────────────────────────────────────────────────────────
    // NOUVEAU DOSSIER
    // ──────────────────────────────────────────────────────────────────────

    private void showNewFolderDialog() {
        EditText et = new EditText(this);
        et.setHint("Nom du dossier");
        et.setTextColor(WHITE);
        et.setHintTextColor(DIM_C);
        et.setPadding(dp(16), dp(12), dp(16), dp(12));
        et.setBackgroundColor(CARD_CLR);
        new AlertDialog.Builder(this)
            .setTitle("Nouveau dossier")
            .setView(et)
            .setPositiveButton("Créer", (d, w) -> {
                String name = et.getText().toString().trim();
                if (name.isEmpty()) return;
                String full = currentFolder.isEmpty() ? name : currentFolder + "/" + name;
                new Thread(() -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("folder", full);
                        OkHttpClient client = new OkHttpClient();
                        Request req = new Request.Builder()
                            .url(getServerUrl() + "/api/cloud/files/" + getUsername() + "/mkdir")
                            .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                            .build();
                        try (Response response = client.newCall(req).execute()) {
                            runOnUiThread(() -> {
                                if (response.isSuccessful()) { showToast("📁 Dossier créé"); refreshFiles(); }
                                else showToast("❌ Erreur");
                            });
                        }
                    } catch (Exception e) { runOnUiThread(() -> showToast("❌ " + e.getMessage())); }
                }).start();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ──────────────────────────────────────────────────────────────────────
    // ÉTATS UI (chargement / vide / erreur)
    // ──────────────────────────────────────────────────────────────────────

    private void showLoading() {
        fileListLayout.removeAllViews();
        LinearLayout center = centered();
        center.addView(makeText("☁️", 40, CYAN_C, Typeface.NORMAL));
        center.addView(makeText("Connexion au cloud…", 13, DIM_C, Typeface.NORMAL));
        fileListLayout.addView(center);
    }

    private void showEmpty() {
        fileListLayout.removeAllViews();
        LinearLayout center = centered();
        center.addView(makeText("☁️", 56, 0xFF1E3A5F, Typeface.NORMAL));
        center.addView(makeText("Cloud vide", 16, WHITE, Typeface.BOLD));
        center.addView(makeText("Appuie sur \"+ Ajouter\" pour envoyer\nton premier fichier dans le cloud", 12, DIM_C, Typeface.NORMAL));
        fileListLayout.addView(center);
    }

    private void showError(String msg) {
        fileListLayout.removeAllViews();
        LinearLayout center = centered();
        center.addView(makeText("⚠️", 40, ORANGE, Typeface.NORMAL));
        center.addView(makeText("Serveur non joignable", 14, WHITE, Typeface.BOLD));
        center.addView(makeText(msg != null ? msg : "Vérifiez votre réseau", 11, DIM_C, Typeface.NORMAL));

        // Affiche l'URL utilisée pour diagnostic
        TextView urlTv = makeText("→ " + getServerUrl(), 9, DIM_C, Typeface.ITALIC);
        urlTv.setPadding(dp(8), dp(8), dp(8), dp(4));
        center.addView(urlTv);

        Button retry = roundButton("Réessayer", CYAN_C, 0xFF001A2E);
        retry.setOnClickListener(v -> refreshFiles());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
        rlp.setMargins(0, dp(12), 0, 0);
        rlp.gravity = Gravity.CENTER;
        retry.setLayoutParams(rlp);
        center.addView(retry);

        LeaAndroidLogger.error(this, "CloudSync", "Connexion échouée vers " + getServerUrl() + " : " + msg);
        fileListLayout.addView(center);
    }

    private void updateBreadcrumb() {
        if (currentFolder.isEmpty()) {
            breadcrumbTv.setText("📁 Mes fichiers");
        } else {
            breadcrumbTv.setText("📁 Mes fichiers / " + currentFolder.replace("/", " / "));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // UTILITAIRES UI
    // ──────────────────────────────────────────────────────────────────────

    private TextView makeText(String txt, float sp, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        tv.setTypeface(null, style);
        return tv;
    }

    private Button roundButton(String text, int textColor, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setBackgroundColor(bgColor);
        btn.setTextSize(11);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(34));
        btn.setLayoutParams(lp);
        return btn;
    }

    private LinearLayout centered() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setPadding(dp(32), dp(48), dp(32), dp(48));
        ll.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        for (int i = 0; i < 10; i++) ll.addView(new View(this)); // placeholder pour espacement
        ll.removeAllViews();
        return ll;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "fichier_" + System.currentTimeMillis();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return String.format(Locale.FRENCH, "%.1f Ko", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format(Locale.FRENCH, "%.1f Mo", bytes / (1024.0 * 1024));
        return String.format(Locale.FRENCH, "%.2f Go", bytes / (1024.0 * 1024 * 1024));
    }

    private String fileEmoji(String ext) {
        switch (ext) {
            case "pdf": return "📄";
            case "doc": case "docx": return "📝";
            case "xls": case "xlsx": case "csv": return "📊";
            case "ppt": case "pptx": return "📋";
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "heic": return "🖼️";
            case "mp4": case "mkv": case "avi": case "mov": case "webm": return "🎬";
            case "mp3": case "aac": case "flac": case "wav": case "ogg": return "🎵";
            case "zip": case "rar": case "7z": case "tar": case "gz": return "🗜️";
            case "apk": return "📱";
            case "json": case "xml": case "yaml": case "yml": return "⚙️";
            case "js": case "ts": case "py": case "java": case "kt": case "cpp": case "c": return "💻";
            case "txt": case "md": return "📃";
            case "db": case "sqlite": return "🗄️";
            default: return "📎";
        }
    }

    private int fileColor(String ext) {
        switch (ext) {
            case "pdf": return RED_C;
            case "doc": case "docx": return 0xFF2563EB;
            case "xls": case "xlsx": case "csv": return GREEN_C;
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "heic": return PURPLE;
            case "mp4": case "mkv": case "avi": case "mov": return ORANGE;
            case "mp3": case "aac": case "flac": case "wav": return 0xFFEC4899;
            case "zip": case "rar": case "7z": return 0xFFF97316;
            case "apk": return GREEN_C;
            case "js": case "ts": case "py": case "java": case "kt": return CYAN_C;
            default: return DIM_C;
        }
    }

    private String getUsername() {
        SharedPreferences p = getSharedPreferences("LeaPrefs", Context.MODE_PRIVATE);
        return p.getString("lea_session_user", "user").replace("\"", "").toLowerCase().trim();
    }

    private String getServerUrl() {
        SharedPreferences p = getSharedPreferences("LeaPrefs", Context.MODE_PRIVATE);
        String host = p.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        return LeaAndroidLogger.resolveServerUrl(host);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
