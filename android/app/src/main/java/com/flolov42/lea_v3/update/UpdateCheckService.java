package com.flolov42.lea_v3.update;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

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

import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateCheckService extends IntentService {

    private static final String PREFS        = "lea_update_prefs";
    private static final String KEY_LAST_CHECK = "last_check_date";
    private static final String KEY_VERSION  = "installed_version";
    private static final String DEFAULT_VER  = "1.0";

    public UpdateCheckService() {
        super("UpdateCheckService");
    }

    // ─── Appel public depuis MainActivity ───────────────────────
    public static void checkOncePerDay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lastCheck = prefs.getString(KEY_LAST_CHECK, "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (!today.equals(lastCheck)) {
            context.startService(new Intent(context, UpdateCheckService.class));
        }
    }

    // ─── OkHttp IPv4-only ───────────────────────────────────────
    private static class IPv4OnlyDns implements Dns {
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            List<InetAddress> all  = Dns.SYSTEM.lookup(hostname);
            List<InetAddress> ipv4 = new ArrayList<>();
            for (InetAddress addr : all) {
                if (addr instanceof Inet4Address) ipv4.add(addr);
            }
            return ipv4.isEmpty() ? all : ipv4;
        }
    }

    // ─── Retry avec backoff ──────────────────────────────────────
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetry;

        RetryInterceptor(int maxRetry) { this.maxRetry = maxRetry; }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request  request       = chain.request();
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
                    try { Thread.sleep(1000L * (attempt + 1)); }
                    catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (lastException != null) throw lastException;
            throw new IOException("Echec après " + maxRetry + " tentatives");
        }
    }

    private OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
            .dns(new IPv4OnlyDns())
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(new RetryInterceptor(2))
            .build();
    }

    // ─── Exécution du service ────────────────────────────────────
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String host = getServerHost();
            Request request = new Request.Builder()
                .url(host + "/api/app/version")
                .header("Cache-Control", "no-cache")
                .build();

            OkHttpClient client = buildClient();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return;

                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                String latestVersion    = json.getString("latestVersion");
                String installedVersion = getInstalledVersion();

                // Sauvegarde date de la vérification
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_LAST_CHECK, today).apply();

                if (!isNewer(latestVersion, installedVersion)) return;

                JSONArray updates = json.getJSONArray("updates");
                for (int i = 0; i < updates.length(); i++) {
                    JSONObject u = updates.getJSONObject(i);
                    if (!u.getString("version").equals(latestVersion)) continue;

                    boolean mandatory   = u.optBoolean("mandatory", false);
                    String  name        = u.optString("name", "Mise à jour");
                    String  description = u.optString("description", "");
                    String  downloadUrl = u.optString("downloadUrl", "");
                    String  fileSize    = u.optString("fileSize", "");
                    String  releaseDate = u.optString("releaseDate", "");

                    JSONArray cl = u.optJSONArray("changelog");
                    StringBuilder changelog = new StringBuilder();
                    if (cl != null) {
                        for (int j = 0; j < cl.length(); j++) {
                            changelog.append("• ").append(cl.getString(j)).append("\n");
                        }
                    }

                    Intent launchIntent = new Intent(this, UpdateActivity.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntent.putExtra(UpdateActivity.EXTRA_VERSION,     latestVersion);
                    launchIntent.putExtra(UpdateActivity.EXTRA_NAME,        name);
                    launchIntent.putExtra(UpdateActivity.EXTRA_DESCRIPTION, description);
                    launchIntent.putExtra(UpdateActivity.EXTRA_MANDATORY,   mandatory);
                    launchIntent.putExtra(UpdateActivity.EXTRA_DOWNLOAD_URL, downloadUrl);
                    launchIntent.putExtra(UpdateActivity.EXTRA_FILE_SIZE,   fileSize);
                    launchIntent.putExtra(UpdateActivity.EXTRA_CHANGELOG,   changelog.toString());
                    startActivity(launchIntent);
                    break;
                }
            }
        } catch (Exception e) {
            // Silencieux : ne pas perturber le démarrage si le serveur est inaccessible
        }
    }

    // ─── Utilitaires ────────────────────────────────────────────
    private String getInstalledVersion() {
        try {
            return getPackageManager()
                .getPackageInfo(getPackageName(), 0)
                .versionName;
        } catch (Exception e) {
            return DEFAULT_VER;
        }
    }

    private String getServerHost() {
        SharedPreferences prefs = getSharedPreferences("lea_prefs", Context.MODE_PRIVATE);
        String host = prefs.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        // Cloudflare Tunnel expose le serveur sur le port 443 (HTTPS), pas sur le port local 3001
        if (host.startsWith("https://") && host.endsWith(":3001")) {
            host = host.substring(0, host.length() - 5);
        }
        return host;
    }

    private boolean isNewer(String remote, String installed) {
        try {
            String[] r   = remote.split("\\.");
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
