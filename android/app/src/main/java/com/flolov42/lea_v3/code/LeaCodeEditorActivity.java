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


import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.List;

public class LeaCodeEditorActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int RED  = 0xFFEF5350;
    private static final int GREEN = 0xFF4CAF50;

    public static final String EXTRA_PROJECT_ID   = "project_id";
    public static final String EXTRA_PROJECT_NAME = "project_name";
    public static final String EXTRA_FILE_PATH    = "file_path";

    private long   projectId;
    private String projectName;
    private String filePath;

    private WebView   editor;
    private TextView  statusBar;
    private TextView  terminalOutput;
    private ScrollView terminalScroll;
    private ProgressBar buildProgress;

    private LeaCodeAgent     codeAgent;
    private LeaCodeCompiler  compiler;
    private LeaAPKInstaller  installer;
    private LeaCodeFileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        projectId   = getIntent().getLongExtra(EXTRA_PROJECT_ID, -1);
        projectName = getIntent().getStringExtra(EXTRA_PROJECT_NAME);
        filePath    = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (projectName == null) projectName = "Projet";

        LeaAgentService svc = LeaAgentService.instance;
        codeAgent   = svc != null ? svc.getCodeAgent() : new LeaCodeAgent(this);
        compiler    = new LeaCodeCompiler(this);
        installer   = new LeaAPKInstaller(this);
        fileManager = new LeaCodeFileManager(this);

        setContentView(buildLayout());

        if (filePath != null) loadFileInEditor(filePath);
        else if (projectName != null) loadProjectDefaultFile();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildHeader());
        root.addView(buildToolbar());

        // Main content: editor + terminal split
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // WebView Editor
        editor = new WebView(this);
        editor.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 2f));
        setupEditor(editor);
        content.addView(editor);

        // Terminal panel
        content.addView(buildTerminalPanel());
        root.addView(content);

        // Status bar
        statusBar = new TextView(this);
        statusBar.setText("📝 Éditeur prêt");
        statusBar.setTextColor(0xFF607D8B);
        statusBar.setTextSize(10f);
        statusBar.setPadding(dp(16), dp(4), dp(16), dp(4));
        statusBar.setBackgroundColor(0xFF010E1A);
        root.addView(statusBar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(0xFF010E1A);
        h.setPadding(dp(8), dp(12), dp(8), dp(12));

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(CYAN);
        back.setBackground(null);
        back.setTextSize(20f);
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = new TextView(this);
        title.setText("💻  " + projectName);
        title.setTextColor(CYAN);
        title.setTextSize(15f);
        title.setTypeface(null, Typeface.BOLD);
        h.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        buildProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        buildProgress.setVisibility(View.INVISIBLE);
        buildProgress.setMax(100);
        buildProgress.setProgress(50);
        h.addView(buildProgress, new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));

        return h;
    }

    private View buildToolbar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xFF011020);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));

        bar.addView(toolBtn("💾 Save",   0xFF00E5FF, v -> saveCurrentFile()));
        bar.addView(toolBtn("🔨 Build",  0xFF4CAF50, v -> buildProject()));
        bar.addView(toolBtn("📂 Files",  0xFF9C27B0, v -> showFileBrowser()));
        bar.addView(toolBtn("🤖 IA",     0xFFFF9800, v -> showAiDialog()));
        bar.addView(toolBtn("📦 Install",0xFF2196F3, v -> installApk()));

        return bar;
    }

    private Button toolBtn(String text, int color, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color);
        b.setTextSize(9f);
        b.setTypeface(null, Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color & 0x00FFFFFF | 0x1A000000);
        gd.setCornerRadius(dp(6));
        gd.setStroke(1, color);
        b.setBackground(gd);
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(listener);
        return b;
    }

    private View buildTerminalPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        panel.setBackgroundColor(0xFF010A12);

        TextView header = new TextView(this);
        header.setText("  ⬛ TERMINAL");
        header.setTextColor(0xFF4CAF50);
        header.setTextSize(10f);
        header.setTypeface(null, Typeface.BOLD);
        header.setBackgroundColor(0xFF010512);
        header.setPadding(dp(8), dp(4), dp(8), dp(4));
        panel.addView(header);

        terminalScroll = new ScrollView(this);
        terminalScroll.setBackgroundColor(0xFF010A12);

        terminalOutput = new TextView(this);
        terminalOutput.setTextColor(0xFF4CAF50);
        terminalOutput.setTextSize(10f);
        terminalOutput.setTypeface(Typeface.MONOSPACE);
        terminalOutput.setPadding(dp(8), dp(4), dp(8), dp(8));
        terminalOutput.setText("$ Léa Terminal prêt\n");
        terminalScroll.addView(terminalOutput);

        panel.addView(terminalScroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return panel;
    }

    // ── WebView Editor ────────────────────────────────────────────────────────

    private void setupEditor(WebView wv) {
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setDomStorageEnabled(true);

        wv.addJavascriptInterface(new EditorBridge(), "LeaEditor");
        wv.loadDataWithBaseURL(null, buildEditorHtml(), "text/html", "UTF-8", null);
    }

    private String buildEditorHtml() {
        return "<!DOCTYPE html>\n<html>\n<head>\n" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
               "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/codemirror.min.css'>\n" +
               "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/theme/dracula.min.css'>\n" +
               "<style>\n" +
               "  body { margin:0; padding:0; background:#011627; }\n" +
               "  .CodeMirror { height:100vh; font-family:monospace; font-size:13px; background:#011627!important; }\n" +
               "  .CodeMirror-scroll { background:#011627; }\n" +
               "  .CodeMirror-gutters { background:#010E1A; border-right:1px solid #1E3A5F; }\n" +
               "  .CodeMirror-linenumber { color:#37474F; }\n" +
               "</style>\n</head>\n<body>\n" +
               "<textarea id='code'></textarea>\n" +
               "<script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/codemirror.min.js'></script>\n" +
               "<script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/clike/clike.min.js'></script>\n" +
               "<script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/addon/edit/closebrackets.min.js'></script>\n" +
               "<script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/addon/edit/matchbrackets.min.js'></script>\n" +
               "<script>\n" +
               "var cm = CodeMirror.fromTextArea(document.getElementById('code'), {\n" +
               "  mode: 'text/x-java',\n" +
               "  theme: 'dracula',\n" +
               "  lineNumbers: true,\n" +
               "  autoCloseBrackets: true,\n" +
               "  matchBrackets: true,\n" +
               "  indentUnit: 4,\n" +
               "  tabSize: 4,\n" +
               "  indentWithTabs: false,\n" +
               "  lineWrapping: false\n" +
               "});\n" +
               "function setCode(code) { cm.setValue(code); cm.refresh(); }\n" +
               "function getCode() { return cm.getValue(); }\n" +
               "cm.on('change', function() { LeaEditor.onChanged(cm.getValue().length); });\n" +
               "</script>\n</body>\n</html>";
    }

    private void setEditorCode(String code) {
        String escaped = code.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
        editor.evaluateJavascript("setCode('" + escaped + "')", null);
    }

    private void getEditorCode(android.webkit.ValueCallback<String> callback) {
        editor.evaluateJavascript("getCode()", callback);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void saveCurrentFile() {
        if (filePath == null) {
            setStatus("⚠️ Aucun fichier ouvert");
            return;
        }
        getEditorCode(code -> {
            if (code == null) return;
            code = code.replace("\\n", "\n").replace("\\'", "'");
            if (code.startsWith("\"") && code.endsWith("\""))
                code = code.substring(1, code.length() - 1);
            try {
                File f = new File(filePath);
                fileManager.writeFile(f.getParentFile(), f.getName(), code);
                setStatus("✅ Sauvegardé: " + f.getName());
            } catch (Exception e) {
                setStatus("❌ Erreur sauvegarde: " + e.getMessage());
            }
        });
    }

    private void buildProject() {
        if (projectId < 0) { toast("Aucun projet sélectionné"); return; }

        saveCurrentFile();
        buildProgress.setVisibility(View.VISIBLE);
        appendTerminal("$ ./gradlew assembleDebug\n");
        setStatus("🔨 Compilation en cours...");

        compiler.compileProject(projectId, projectName, new LeaCodeCompiler.CompileCallback() {
            @Override
            public void onProgress(String msg) {
                runOnUiThread(() -> appendTerminal(msg + "\n"));
            }
            @Override
            public void onSuccess(File apk, String name) {
                runOnUiThread(() -> {
                    buildProgress.setVisibility(View.INVISIBLE);
                    appendTerminal("✅ BUILD SUCCESSFUL → " + (apk != null ? apk.getName() : "APK généré") + "\n");
                    setStatus("✅ Compilation réussie!");
                    toast("🎉 " + name + " compilé avec succès!");
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    buildProgress.setVisibility(View.INVISIBLE);
                    appendTerminal("❌ BUILD FAILED:\n" + error + "\n");
                    setStatus("❌ Erreur de compilation");
                });
            }
        });
    }

    private void showFileBrowser() {
        File root = fileManager.getProjectRoot(projectName);
        List<String> files = fileManager.listFiles(root);

        if (files.isEmpty()) {
            toast("Aucun fichier dans ce projet");
            return;
        }

        String[] items = files.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle("📂 Fichiers — " + projectName)
            .setItems(items, (d, which) -> {
                filePath = new File(root, items[which]).getAbsolutePath();
                loadFileInEditor(filePath);
            })
            .setNegativeButton("Fermer", null)
            .show();
    }

    private void showAiDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        form.setBackgroundColor(BG);

        TextView lbl = new TextView(this);
        lbl.setText("Demande à Léa de générer du code:");
        lbl.setTextColor(CYAN);
        lbl.setTextSize(13f);
        form.addView(lbl);

        EditText input = new EditText(this);
        input.setHint("Ex: Crée une Activity avec un bouton qui envoie un SMS");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF37474F);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(8));
        bg.setStroke(1, 0xFF37474F);
        input.setBackground(bg);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setMinLines(3);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        form.addView(input, lp);

        new AlertDialog.Builder(this)
            .setTitle("🤖 Léa Code Agent")
            .setView(form)
            .setPositiveButton("Générer", (d, w) -> {
                String req = input.getText().toString().trim();
                if (req.isEmpty()) return;
                generateWithAi(req);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void generateWithAi(String request) {
        setStatus("🤖 Génération en cours...");
        appendTerminal("$ Léa génère: " + request + "\n");

        if (projectId < 0) {
            projectId = codeAgent.startProject(projectName, request, (text, streaming) -> {
                runOnUiThread(() -> {
                    handleAiResponse(text);
                });
            });
        } else {
            codeAgent.continueConversation(projectId, request, (text, streaming) -> {
                runOnUiThread(() -> handleAiResponse(text));
            });
        }
    }

    private void handleAiResponse(String text) {
        appendTerminal("Léa: " + text.substring(0, Math.min(100, text.length())) + "...\n");
        setStatus("✅ Code généré par Léa");

        // Extract Java code block if present
        String code = extractCodeBlock(text);
        if (code != null && !code.isEmpty()) {
            setEditorCode(code);
            toast("💻 Code inséré dans l'éditeur!");
        } else {
            // Show full response in dialog
            new AlertDialog.Builder(this)
                .setTitle("🤖 Réponse Léa")
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show();
        }
    }

    private String extractCodeBlock(String text) {
        int start = text.indexOf("```java");
        if (start < 0) start = text.indexOf("```");
        if (start < 0) {
            if (text.contains("package ") && text.contains("class ")) return text;
            return null;
        }
        int end = text.indexOf("```", start + 3);
        if (end < 0) return null;
        String code = text.substring(start, end);
        code = code.replaceFirst("```java?\\s*", "").replace("```", "");
        return code.trim();
    }

    private void installApk() {
        List<LeaAgentDatabase.ProjectRow> projects = codeAgent.getProjects();
        for (LeaAgentDatabase.ProjectRow p : projects) {
            if (p.id == projectId && p.apkPath != null) {
                File apk = new File(p.apkPath);
                installer.installApk(apk, new LeaAPKInstaller.InstallCallback() {
                    @Override public void onProgress(String m) { runOnUiThread(() -> appendTerminal(m + "\n")); }
                    @Override public void onSuccess(String m)  { runOnUiThread(() -> { toast(m); appendTerminal(m + "\n"); }); }
                    @Override public void onError(String e)    { runOnUiThread(() -> toast("❌ " + e)); }
                });
                return;
            }
        }
        toast("Aucun APK trouvé — compilez d'abord le projet");
    }

    // ── File loading ──────────────────────────────────────────────────────────

    private void loadFileInEditor(String path) {
        try {
            String content = fileManager.readFile(new File(path));
            setEditorCode(content);
            setStatus("📄 " + new File(path).getName());
        } catch (Exception e) {
            setStatus("❌ Erreur ouverture: " + e.getMessage());
        }
    }

    private void loadProjectDefaultFile() {
        File root = fileManager.getProjectRoot(projectName);
        List<String> files = fileManager.listFiles(root);
        for (String f : files) {
            if (f.endsWith("MainActivity.java") || f.endsWith(".java")) {
                filePath = new File(root, f).getAbsolutePath();
                loadFileInEditor(filePath);
                return;
            }
        }
        // Create a starter file
        String starter = "package com.lea.generated;\n\n" +
            "import android.app.Activity;\nimport android.os.Bundle;\n\n" +
            "public class " + projectName.replaceAll("[^a-zA-Z]", "") + "MainActivity extends Activity {\n" +
            "    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "    }\n}\n";
        setEditorCode(starter);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendTerminal(String text) {
        terminalOutput.append(text);
        terminalScroll.post(() -> terminalScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void setStatus(String msg) {
        statusBar.setText(msg);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    // ── JavaScript Bridge ─────────────────────────────────────────────────────

    private class EditorBridge {
        @JavascriptInterface
        public void onChanged(int length) {
            runOnUiThread(() -> setStatus("📝 " + length + " caractères"));
        }
    }
}
