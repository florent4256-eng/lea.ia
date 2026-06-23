package com.flolov42.lea_v3.update;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AppUpdateManager {

    public interface DownloadCallback {
        void onProgress(int percent);
        void onComplete(File apkFile);
        void onError(String error);
    }

    private final Context context;
    private Call activeCall;

    public AppUpdateManager(Context context) {
        this.context = context;
    }

    /**
     * Normalise l'URL de téléchargement :
     * - URL relative ("/...") → préfixée avec serverHost
     * - URL https avec :3001 → port supprimé (Cloudflare Tunnel écoute sur 443)
     */
    private String resolveUrl(String url, String serverHost) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("/")) return serverHost + url;
        if (url.startsWith("https://") && url.contains(":3001")) {
            url = url.replace(":3001", "");
        }
        return url;
    }

    public void downloadAPK(String url, String serverHost, String version, DownloadCallback callback) {
        String resolvedUrl = resolveUrl(url, serverHost);
        if (resolvedUrl.isEmpty()) {
            callback.onError("URL de téléchargement invalide.");
            return;
        }

        // Dossier accessible au FileProvider sans permission WRITE_EXTERNAL_STORAGE
        File destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (destDir == null) destDir = context.getCacheDir();
        File destFile = new File(destDir, "lea_update_" + version + ".apk");
        if (destFile.exists()) destFile.delete();

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build();

        Request request = new Request.Builder()
            .url(resolvedUrl)
            .build();

        final File finalDestFile = destFile;
        activeCall = client.newCall(request);
        activeCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Connexion échouée");
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (call.isCanceled()) return;
                if (!response.isSuccessful()) {
                    callback.onError("Serveur : HTTP " + response.code());
                    response.close();
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    callback.onError("Réponse vide du serveur.");
                    return;
                }

                long contentLength = body.contentLength();

                try (InputStream is = body.byteStream();
                     FileOutputStream fos = new FileOutputStream(finalDestFile)) {

                    byte[] buffer = new byte[16384];
                    long downloaded = 0;
                    int read;
                    int lastPercent = -1;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloaded += read;
                        if (contentLength > 0) {
                            int percent = (int) (downloaded * 100L / contentLength);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                callback.onProgress(percent);
                            }
                        }
                    }

                    callback.onComplete(finalDestFile);

                } catch (IOException e) {
                    finalDestFile.delete();
                    callback.onError("Erreur écriture : " + e.getMessage());
                }
            }
        });
    }

    public void installAPK(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                apkFile
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            apkUri = Uri.fromFile(apkFile);
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void cancelDownload() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
    }
}
