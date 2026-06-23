package com.flolov42.lea_v3.code;

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
import android.content.Intent;
import android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LeaTermuxIntegration {

    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_RUN_ACTION = "com.termux.RUN_COMMAND";

    private final Context ctx;

    public LeaTermuxIntegration(Context ctx) {
        this.ctx = ctx;
    }

    public boolean isTermuxInstalled() {
        try {
            ctx.getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Run command in Termux via broadcast */
    public void runInTermux(String command, String workingDir) {
        Intent intent = new Intent(TERMUX_RUN_ACTION);
        intent.setPackage(TERMUX_PACKAGE);
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", command});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", workingDir);
        intent.putExtra("com.termux.RUN_COMMAND_TERMINAL", true);
        ctx.sendBroadcast(intent);
    }

    /** Execute shell command locally and capture output */
    public CommandResult executeLocal(String command, File workDir, CommandCallback callback) {
        CommandResult result = new CommandResult();
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (callback != null) callback.onLine(line);
                }
            }

            result.exitCode = process.waitFor();
            result.output   = sb.toString();
            result.success  = result.exitCode == 0;

        } catch (IOException | InterruptedException e) {
            result.output  = "Erreur: " + e.getMessage();
            result.success = false;
            result.exitCode = -1;
        }
        return result;
    }

    /** Run gradle build asynchronously */
    public void buildGradle(File projectRoot, BuildCallback callback) {
        new Thread(() -> {
            try {
                // Make gradlew executable
                File gradlew = new File(projectRoot, "gradlew");
                if (gradlew.exists()) gradlew.setExecutable(true);

                // Try local gradlew first
                String buildCmd = gradlew.exists()
                    ? "./gradlew assembleDebug --no-daemon --stacktrace"
                    : "gradle assembleDebug --no-daemon --stacktrace";

                CommandResult r = executeLocal(buildCmd, projectRoot, line -> {
                    if (callback != null) callback.onProgress(line);
                });

                if (r.success) {
                    // Find generated APK
                    File apk = findApk(projectRoot);
                    if (callback != null) callback.onSuccess(apk);
                } else {
                    if (callback != null) callback.onError(r.output);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Erreur build: " + e.getMessage());
            }
        }).start();
    }

    private File findApk(File projectRoot) {
        File debugDir = new File(projectRoot, "app/build/outputs/apk/debug");
        if (debugDir.exists()) {
            File[] apks = debugDir.listFiles(f -> f.getName().endsWith(".apk"));
            if (apks != null && apks.length > 0) return apks[0];
        }
        return null;
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class CommandResult {
        public boolean success;
        public int     exitCode;
        public String  output;
    }

    public interface CommandCallback {
        void onLine(String line);
    }

    public interface BuildCallback {
        void onProgress(String line);
        void onSuccess(File apkFile);
        void onError(String error);
    }
}
