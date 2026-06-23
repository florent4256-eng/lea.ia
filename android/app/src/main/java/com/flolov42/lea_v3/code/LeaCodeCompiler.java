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
import java.io.File;
import java.util.List;

public class LeaCodeCompiler {

    private final Context               ctx;
    private final LeaCodeFileManager    fileManager;
    private final LeaTermuxIntegration  termux;
    private final LeaCodeProjectStorage storage;
    private final LeaAgentDatabase      db;

    public LeaCodeCompiler(Context ctx) {
        this.ctx         = ctx;
        this.fileManager = new LeaCodeFileManager(ctx);
        this.termux      = new LeaTermuxIntegration(ctx);
        this.storage     = new LeaCodeProjectStorage(ctx);
        this.db          = LeaAgentDatabase.get(ctx);
    }

    public interface CompileCallback {
        void onProgress(String message);
        void onSuccess(File apkFile, String projectName);
        void onError(String error);
    }

    public void compileProject(long projectId, String projectName, CompileCallback callback) {
        File root = fileManager.getProjectRoot(projectName);

        if (!root.exists()) {
            if (callback != null) callback.onError("Dossier projet introuvable: " + root.getAbsolutePath());
            return;
        }

        if (callback != null) callback.onProgress("🔨 Démarrage de la compilation...");
        storage.updateStatus(projectId, "building", null);
        db.addLog(LeaAgentActivationManager.CODE, "🔨 Compilation démarrée: " + projectName);

        termux.buildGradle(root, new LeaTermuxIntegration.BuildCallback() {
            @Override
            public void onProgress(String line) {
                if (callback != null) callback.onProgress(line);
                // Log important lines
                if (line.contains("BUILD") || line.contains("ERROR") || line.contains("FAILED")) {
                    db.addLog(LeaAgentActivationManager.CODE, line);
                }
            }

            @Override
            public void onSuccess(File apkFile) {
                String apkPath = apkFile != null ? apkFile.getAbsolutePath() : null;
                storage.updateStatus(projectId, "built", apkPath);
                db.addLog(LeaAgentActivationManager.CODE, "✅ Compilation réussie: " + projectName);
                if (callback != null) callback.onSuccess(apkFile, projectName);
            }

            @Override
            public void onError(String error) {
                storage.updateStatus(projectId, "error", null);
                db.addLog(LeaAgentActivationManager.CODE, "❌ Erreur compilation: " + error.substring(0, Math.min(200, error.length())));
                if (callback != null) callback.onError(error);
            }
        });
    }

    /** Run a quick syntax check without full build */
    public void quickCheck(String javaCode, CompileCallback callback) {
        new Thread(() -> {
            // Basic syntax validation
            List<String> errors = checkBasicSyntax(javaCode);
            if (errors.isEmpty()) {
                if (callback != null) callback.onSuccess(null, "syntax_check");
            } else {
                if (callback != null) callback.onError(String.join("\n", errors));
            }
        }).start();
    }

    private java.util.List<String> checkBasicSyntax(String code) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        if (code == null || code.trim().isEmpty()) {
            errors.add("Code vide");
            return errors;
        }

        int braces = 0;
        int parens = 0;

        for (char c : code.toCharArray()) {
            if (c == '{') braces++;
            else if (c == '}') braces--;
            else if (c == '(') parens++;
            else if (c == ')') parens--;
        }

        if (braces != 0) errors.add("Accolades déséquilibrées: " + (braces > 0 ? "manque }" : "trop de }"));
        if (parens != 0) errors.add("Parenthèses déséquilibrées: " + (parens > 0 ? "manque )" : "trop de )"));

        if (!code.contains("class ")) errors.add("⚠️ Aucune déclaration de classe trouvée");

        return errors;
    }
}
