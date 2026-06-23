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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LeaCloudSync extends LeaBasePlusFeature {

    private static final String PREFS = "lea_cloud_sync";
    private static final long SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L;
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String[] DB_NAMES = {"lea_plus.db", "lea_agents.db", "lea_modes.db"};

    public LeaCloudSync(Context ctx) { super(ctx, LeaPlusDatabase.CLOUD_SYNC); }

    @Override
    public void execute() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lastSync = prefs.getLong("last_sync", 0);
        if (System.currentTimeMillis() - lastSync > SYNC_INTERVAL_MS) {
            syncToCloud();
        }
    }

    // ──────────────────────────────────────────────
    // BACKUP CLOUD (avec fallback local)
    // ──────────────────────────────────────────────

    public void triggerManualBackup() {
        notify("☁️ Cloud Sync LÉA", "Sauvegarde cloud en cours…");
        syncToCloud();
    }

    private void syncToCloud() {
        String username = getUsername();
        String serverUrl = getServerUrl();

        try {
            JSONObject files = buildFilesPayload();
            if (files.length() == 0) {
                log("⚠️ Aucune base de données trouvée");
                return;
            }

            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("files", files);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(serverUrl + "/api/cloud/backup")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                       .edit().putLong("last_sync", System.currentTimeMillis()).apply();
                    db.insertSyncHistory("cloud_backup", "success",
                        files.length() + " BDD sauvegardées sur le cloud");
                    notify("✅ Cloud Sync réussi", files.length() + " bases sauvegardées sur ton disque 4To !");
                    log("☁️ Cloud backup réussi: " + files.length() + " BDD → " + serverUrl);
                    return;
                }
            }
        } catch (Exception e) {
            log("☁️ Cloud injoignable, fallback local: " + e.getMessage());
        }

        // Fallback : sauvegarde locale
        performLocalBackup();
    }

    // ──────────────────────────────────────────────
    // RESTORE CLOUD
    // ──────────────────────────────────────────────

    public void restoreFromCloud() {
        String username = getUsername();
        String serverUrl = getServerUrl();
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(serverUrl + "/api/cloud/restore/" + username)
                .get().build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log("❌ Restore cloud: réponse invalide");
                    return;
                }
                JSONObject json = new JSONObject(response.body().string());
                if (!json.optBoolean("success", false)) {
                    log("❌ Restore cloud: " + json.optString("error"));
                    return;
                }
                JSONObject filesJson = json.getJSONObject("files");
                int restored = 0;
                for (String fileName : DB_NAMES) {
                    if (filesJson.has(fileName)) {
                        byte[] data = Base64.decode(filesJson.getString(fileName), Base64.DEFAULT);
                        File dest = ctx.getDatabasePath(fileName);
                        try (FileOutputStream fos = new FileOutputStream(dest)) {
                            fos.write(data);
                        }
                        restored++;
                    }
                }
                db.insertSyncHistory("cloud_restore", "success",
                    restored + " BDD restaurées depuis " + json.optString("backup"));
                notify("✅ Restauration réussie", restored + " bases LÉA récupérées depuis le cloud !");
                log("♻️ Restore cloud: " + restored + " BDD restaurées");
            }
        } catch (Exception e) {
            db.insertSyncHistory("cloud_restore", "error", e.getMessage());
            log("❌ Restore cloud échoué: " + e.getMessage());
        }
    }

    // Restore depuis un backup local (compatibilité ancienne version)
    public void restoreFromBackup(String backupTimestamp) {
        File backupDir = new File(ctx.getFilesDir(), "lea_backups");
        int restored = 0;
        for (String dbName : DB_NAMES) {
            File backup = new File(backupDir, backupTimestamp + "_" + dbName);
            if (backup.exists()) {
                try {
                    copyFile(backup, ctx.getDatabasePath(dbName));
                    restored++;
                } catch (Exception e) {
                    log("❌ Restore local " + dbName + " échoué: " + e.getMessage());
                }
            }
        }
        log("♻️ Restore local: " + restored + " BDD restaurées depuis " + backupTimestamp);
        db.insertSyncHistory("local_restore", "success",
            restored + " BDD restaurées depuis " + backupTimestamp);
    }

    // ──────────────────────────────────────────────
    // STATUT CLOUD
    // ──────────────────────────────────────────────

    public String getSyncStatus() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lastSync = prefs.getLong("last_sync", 0);
        String lastStr = lastSync > 0
            ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(lastSync))
            : "Jamais";

        String serverInfo = getServerStatus();

        return "☁️ CLOUD SYNC LÉA\n\n" +
               "Dernière sync: " + lastStr + "\n" +
               serverInfo +
               "Sync automatique: Toutes les 24h";
    }

    private String getServerStatus() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(getServerUrl() + "/api/cloud/status/" + getUsername())
                .get().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("available", false)) {
                        boolean diskMounted = json.optBoolean("diskMounted", false);
                        return "Backups cloud: " + json.optInt("totalBackups", 0) + "\n" +
                               "Taille: " + json.optString("totalSizeMB", "0") + " MB\n" +
                               "Disque 4To: " + (diskMounted ? "✅ Connecté" : "⚠️ Non monté (stockage local)") + "\n";
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Serveur: non joignable\n";
    }

    // ──────────────────────────────────────────────
    // BACKUPS LOCAUX DISPONIBLES
    // ──────────────────────────────────────────────

    public String[] getAvailableBackups() {
        File backupDir = new File(ctx.getFilesDir(), "lea_backups");
        if (!backupDir.exists()) return new String[0];
        File[] files = backupDir.listFiles((f, n) -> n.endsWith("lea_plus.db"));
        if (files == null) return new String[0];
        java.util.Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName()));
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++)
            names[i] = files[i].getName().replace("_lea_plus.db", "");
        return names;
    }

    // ──────────────────────────────────────────────
    // FALLBACK : BACKUP LOCAL
    // ──────────────────────────────────────────────

    private void performLocalBackup() {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File backupDir = new File(ctx.getFilesDir(), "lea_backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            int backed = 0;
            for (String dbName : DB_NAMES) {
                File src = ctx.getDatabasePath(dbName);
                if (src.exists()) {
                    copyFile(src, new File(backupDir, ts + "_" + dbName));
                    backed++;
                }
            }
            cleanOldBackups(backupDir);
            db.insertSyncHistory("local_backup", "success",
                backed + " bases sauvegardées localement — " + ts);
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit().putLong("last_sync", System.currentTimeMillis()).apply();
            notify("💾 Backup local", backed + " bases sauvegardées (cloud indisponible)");
            log("💾 Backup local: " + backed + " BDD → " + backupDir.getAbsolutePath());
        } catch (Exception e) {
            db.insertSyncHistory("local_backup", "error", e.getMessage());
            log("❌ Backup local échoué: " + e.getMessage());
        }
    }

    private void cleanOldBackups(File backupDir) {
        File[] files = backupDir.listFiles();
        if (files == null || files.length <= 10) return;
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (int i = 0; i < files.length - 10; i++) files[i].delete();
    }

    // ──────────────────────────────────────────────
    // UTILITAIRES
    // ──────────────────────────────────────────────

    private JSONObject buildFilesPayload() throws Exception {
        JSONObject files = new JSONObject();
        for (String dbName : DB_NAMES) {
            File src = ctx.getDatabasePath(dbName);
            if (src.exists()) {
                byte[] data = readFile(src);
                files.put(dbName, Base64.encodeToString(data, Base64.NO_WRAP));
            }
        }
        return files;
    }

    private byte[] readFile(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] data = new byte[(int) f.length()];
            in.read(data);
            return data;
        }
    }

    private void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private String getUsername() {
        SharedPreferences prefs = ctx.getSharedPreferences("LeaPrefs", Context.MODE_PRIVATE);
        return prefs.getString("lea_session_user", "user").replace("\"", "").toLowerCase().trim();
    }

    private String getServerUrl() {
        SharedPreferences prefs = ctx.getSharedPreferences("LeaPrefs", Context.MODE_PRIVATE);
        String host = prefs.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        return LeaAndroidLogger.resolveServerUrl(host);
    }
}
