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
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import java.io.File;

public class LeaAPKInstaller {

    private final Context ctx;

    public LeaAPKInstaller(Context ctx) {
        this.ctx = ctx;
    }

    public void installApk(File apkFile, InstallCallback callback) {
        if (apkFile == null || !apkFile.exists()) {
            if (callback != null) callback.onError("APK introuvable: " + (apkFile != null ? apkFile.getAbsolutePath() : "null"));
            return;
        }

        try {
            if (callback != null) callback.onProgress("📦 Lancement de l'installation de " + apkFile.getName());

            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7+
                apkUri = FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".fileprovider", apkFile);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            ctx.startActivity(intent);

            if (callback != null) callback.onSuccess("✅ Installation lancée pour " + apkFile.getName());

        } catch (Exception e) {
            if (callback != null) callback.onError("Erreur installation: " + e.getMessage());
        }
    }

    public interface InstallCallback {
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String error);
    }
}
