package com.flolov42.lea_v3.google;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaGoogleDrive {

    private static final String BASE_API    = "https://www.googleapis.com/drive/v3";
    private static final String BASE_UPLOAD = "https://www.googleapis.com/upload/drive/v3/files";

    public interface FilesCallback  { void onSuccess(List<LeaGoogleDatabase.GFile> files); void onError(String err); }
    public interface UploadCallback { void onSuccess(String fileId, String fileName); void onError(String err); }
    public interface DownloadCallback { void onSuccess(String localPath); void onError(String err); }

    private final Context ctx;
    private final LeaGoogleAuth auth;
    private final LeaGoogleDatabase db;

    private static LeaGoogleDrive instance;
    public static LeaGoogleDrive get(Context ctx) {
        if (instance == null) instance = new LeaGoogleDrive(ctx.getApplicationContext());
        return instance;
    }
    private LeaGoogleDrive(Context ctx) {
        this.ctx  = ctx;
        this.auth = LeaGoogleAuth.get(ctx);
        this.db   = LeaGoogleDatabase.get(ctx);
    }

    // ── Lister les fichiers ───────────────────────────────────────────────────
    public void listFiles(FilesCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    JSONObject resp = httpGet(
                        BASE_API + "/files?pageSize=50&fields=files(id,name,mimeType,size,modifiedTime)&orderBy=modifiedTime+desc", token);
                    JSONArray files = resp.optJSONArray("files");
                    if (files == null) { callback.onSuccess(db.getDriveFiles()); return; }

                    for (int i = 0; i < files.length(); i++) {
                        JSONObject f   = files.getJSONObject(i);
                        String id      = f.optString("id");
                        String name    = f.optString("name");
                        String mime    = f.optString("mimeType");
                        long size      = f.optLong("size", 0);
                        long modified  = parseModifiedDate(f.optString("modifiedTime"));
                        db.upsertFile(id, name, mime, size, modified);
                    }
                    callback.onSuccess(db.getDriveFiles());
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Upload fichier (multipart) ────────────────────────────────────────────
    public void uploadFile(String localPath, String mimeType, UploadCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                new Thread(() -> {
                    try {
                        File file = new File(localPath);
                        if (!file.exists()) { callback.onError("Fichier introuvable: " + localPath); return; }

                        String boundary = "lea_boundary_" + System.currentTimeMillis();
                        JSONObject metadata = new JSONObject();
                        metadata.put("name", file.getName());

                        byte[] metaBytes = metadata.toString().getBytes("UTF-8");
                        byte[] fileBytes = readFile(file);

                        String urlStr = BASE_UPLOAD + "?uploadType=multipart";
                        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                        conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
                        conn.setDoOutput(true); conn.setConnectTimeout(30000); conn.setReadTimeout(60000);

                        OutputStream out = conn.getOutputStream();
                        // Part 1: metadata
                        out.write(("--" + boundary + "\r\n").getBytes());
                        out.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes());
                        out.write(metaBytes);
                        out.write("\r\n".getBytes());
                        // Part 2: file content
                        out.write(("--" + boundary + "\r\n").getBytes());
                        out.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes());
                        out.write(fileBytes);
                        out.write(("\r\n--" + boundary + "--").getBytes());
                        out.close();

                        int code = conn.getResponseCode();
                        if (code >= 400) { callback.onError("Upload failed HTTP " + code); return; }

                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder(); String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close(); conn.disconnect();

                        JSONObject respObj = new JSONObject(sb.toString());
                        String fileId   = respObj.optString("id");
                        String fileName = respObj.optString("name");
                        db.upsertFile(fileId, fileName, mimeType, file.length(), System.currentTimeMillis());
                        callback.onSuccess(fileId, fileName);
                    } catch (Exception e) { callback.onError(e.getMessage()); }
                }).start();
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Download fichier ──────────────────────────────────────────────────────
    public void downloadFile(String fileId, String destPath, DownloadCallback callback) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                new Thread(() -> {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(
                            BASE_API + "/files/" + fileId + "?alt=media").openConnection();
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                        conn.setConnectTimeout(15000); conn.setReadTimeout(60000);
                        if (conn.getResponseCode() >= 400) { callback.onError("HTTP " + conn.getResponseCode()); return; }

                        InputStream is = conn.getInputStream();
                        File dest = new File(destPath);
                        dest.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(dest);
                        byte[] buf = new byte[8192]; int n;
                        while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                        fos.close(); is.close(); conn.disconnect();
                        callback.onSuccess(destPath);
                    } catch (Exception e) { callback.onError(e.getMessage()); }
                }).start();
            }
            @Override public void onError(String err) { callback.onError(err); }
        });
    }

    // ── Supprimer un fichier ──────────────────────────────────────────────────
    public void deleteFile(String fileId, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        auth.getAccessToken(new LeaGoogleAuth.TokenCallback() {
            @Override public void onToken(String token) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(BASE_API + "/files/" + fileId).openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
                    conn.getResponseCode(); conn.disconnect();
                    onSuccess.run();
                } catch (Exception e) { onError.accept(e.getMessage()); }
            }
            @Override public void onError(String err) { onError.accept(err); }
        });
    }

    // ── Backup automatique Léa ────────────────────────────────────────────────
    public void backupLeaData(UploadCallback callback) {
        String backupDir = ctx.getFilesDir().getAbsolutePath();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        // Uploader les bases de données
        for (String dbName : new String[]{"lea_plus.db","lea_agents.db","lea_modes.db"}) {
            String dbPath = ctx.getDatabasePath(dbName).getAbsolutePath();
            File f = new File(dbPath);
            if (f.exists()) {
                uploadFile(dbPath, "application/octet-stream", new UploadCallback() {
                    @Override public void onSuccess(String id, String name) {}
                    @Override public void onError(String err) {}
                });
            }
        }
        callback.onSuccess("backup_" + ts, "Backup Léa " + ts);
    }

    // ── Cache local ───────────────────────────────────────────────────────────
    public List<LeaGoogleDatabase.GFile> getCachedFiles() {
        return db.getDriveFiles();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JSONObject httpGet(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
        if (conn.getResponseCode() >= 400) throw new Exception("HTTP " + conn.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close(); conn.disconnect();
        return new JSONObject(sb.toString());
    }
    private byte[] readFile(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = fis.read(buf)) != -1) bos.write(buf, 0, n);
        fis.close(); return bos.toByteArray();
    }
    private long parseModifiedDate(String date) {
        if (date == null || date.isEmpty()) return 0;
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(date).getTime();
        } catch (Exception e) { return 0; }
    }
}
