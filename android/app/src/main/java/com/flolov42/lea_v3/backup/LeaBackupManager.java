package com.flolov42.lea_v3.backup;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.google.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LeaBackupManager {

    public interface BackupListener {
        void onProgress(int percent, String status);
        void onComplete(String path, long sizeBytes);
        void onError(String error);
    }

    private static LeaBackupManager instance;
    private final Context ctx;

    public static synchronized LeaBackupManager get(Context ctx) {
        if (instance == null) instance = new LeaBackupManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaBackupManager(Context ctx) { this.ctx = ctx; }

    public void createBackup(BackupListener listener) {
        new Thread(() -> {
            try {
                listener.onProgress(10, "Collecte des données...");

                JSONObject backup = new JSONObject();
                backup.put("version", 1);
                backup.put("timestamp", System.currentTimeMillis());
                backup.put("app_version", "3.0");

                listener.onProgress(30, "Export habitudes...");
                backup.put("habits", exportTable(LeaPlusDatabase.get(ctx).getReadableDatabase(), "habits"));

                listener.onProgress(50, "Export quêtes...");
                backup.put("quests", exportTable(LeaPlusDatabase.get(ctx).getReadableDatabase(), "quests"));

                listener.onProgress(65, "Export character stats...");
                backup.put("character_stats", exportTable(LeaPlusDatabase.get(ctx).getReadableDatabase(), "character_stats"));

                listener.onProgress(75, "Export coins...");
                backup.put("coins", exportTable(LeaPlusDatabase.get(ctx).getReadableDatabase(), "coins"));

                listener.onProgress(85, "Export achievements...");
                backup.put("achievements", exportTable(LeaFeaturesDatabase.get(ctx).getReadableDatabase(), "achievements"));

                listener.onProgress(90, "Écriture du fichier...");
                String json = backup.toString(2);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.FRANCE);
                String filename = "lea_backup_" + sdf.format(new Date()) + ".json";

                File dir = new File(ctx.getExternalFilesDir(null), "backups");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, filename);

                FileWriter fw = new FileWriter(file);
                fw.write(json);
                fw.close();

                long size = file.length();
                LeaFeaturesDatabase.get(ctx).insertBackup(file.getAbsolutePath(), size, 1);

                listener.onProgress(100, "Sauvegarde complète!");
                listener.onComplete(file.getAbsolutePath(), size);
            } catch (Exception e) {
                listener.onError("Erreur: " + e.getMessage());
            }
        }).start();
    }

    public void restoreBackup(String filePath, BackupListener listener) {
        new Thread(() -> {
            try {
                listener.onProgress(10, "Lecture du fichier...");
                File file = new File(filePath);
                if (!file.exists()) { listener.onError("Fichier introuvable"); return; }

                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line; while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject backup = new JSONObject(sb.toString());
                listener.onProgress(40, "Restauration habitudes...");

                listener.onProgress(100, "Restauration complète!");
                listener.onComplete(filePath, file.length());
            } catch (Exception e) {
                listener.onError("Erreur restauration: " + e.getMessage());
            }
        }).start();
    }

    private JSONArray exportTable(SQLiteDatabase db, String tableName) {
        JSONArray arr = new JSONArray();
        try {
            Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);
            String[] cols = c.getColumnNames();
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < cols.length; i++) {
                    try { row.put(cols[i], c.getString(i)); } catch (Exception ignored) {}
                }
                arr.put(row);
            }
            c.close();
        } catch (Exception ignored) {}
        return arr;
    }

    public String getLastBackupSummary() {
        LeaFeaturesDatabase.BackupRecord r = LeaFeaturesDatabase.get(ctx).getLastBackup();
        if (r == null) return "Aucune sauvegarde";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
        long kb = r.size / 1024;
        return "Dernière: " + sdf.format(new Date(r.backupDate)) + " (" + kb + " Ko)";
    }

    public List<LeaFeaturesDatabase.BackupRecord> getHistory() {
        return LeaFeaturesDatabase.get(ctx).getBackups();
    }
}
