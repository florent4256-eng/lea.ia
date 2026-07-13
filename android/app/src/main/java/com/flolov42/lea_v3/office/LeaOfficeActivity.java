package com.flolov42.lea_v3.office;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.flolov42.lea_v3.R;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LeaOfficeActivity extends AppCompatActivity {

    // ── Document model ────────────────────────────────────────────
    private static class OfficeDoc {
        String id, title, content, modifiedAt;
        File file;
    }

    // ── Fields ────────────────────────────────────────────────────
    private final Handler ui = new Handler(Looper.getMainLooper());
    private OkHttpClient httpClient;
    private File officeDir;

    // Screens
    private LinearLayout screenList, screenEditor;
    private LinearLayout docListContainer, emptyState;

    // Top bar
    private TextView    lblOfficeTitle;
    private EditText    edDocTitle;
    private Button      btnSave, btnDelete;

    // Editor
    private WebView  webEditor;
    private boolean  editorReady = false;

    // Léa panel
    private LinearLayout leaExpanded, leaResult;
    private TextView     leaChevron, leaStatusText, leaResultText;
    private ProgressBar  leaLoading;
    private EditText     leaInput;
    private String       lastInstruction = "";

    // State
    private OfficeDoc   currentDoc       = null;
    private boolean     hasUnsavedChanges = false;
    private final List<OfficeDoc> documents = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);
        // applyImmersive() appelé uniquement depuis onWindowFocusChanged (plus bas) — l'appeler
        // ici, avant setContentView, plantait sur certains appareils (fenêtre pas encore prête
        // pour getInsetsController()). Même pattern que les autres écrans natifs du projet.
        setContentView(R.layout.activity_office);

        httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

        officeDir = new File(getFilesDir(), "lea_office");
        if (!officeDir.exists()) officeDir.mkdirs();

        bindViews();
        setupEditor();
        setupListeners();
        loadDocumentList();
        showListScreen();
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            Toast.makeText(this, "❌ Office: " + e.getMessage() + "\n@ " + loc, Toast.LENGTH_LONG).show();
            com.flolov42.lea_v3.utilities.LeaAndroidLogger.crash(this, "Office onCreate", e);
            finish();
        }
    }

    // ── Bind ──────────────────────────────────────────────────────
    private void bindViews() {
        screenList      = findViewById(R.id.screenList);
        screenEditor    = findViewById(R.id.screenEditor);
        docListContainer = findViewById(R.id.docListContainer);
        emptyState      = findViewById(R.id.emptyState);
        lblOfficeTitle  = findViewById(R.id.lblOfficeTitle);
        edDocTitle      = findViewById(R.id.edDocTitle);
        btnSave         = findViewById(R.id.btnSave);
        btnDelete       = findViewById(R.id.btnDelete);
        webEditor       = findViewById(R.id.webEditor);
        leaExpanded     = findViewById(R.id.leaExpanded);
        leaChevron      = findViewById(R.id.leaChevron);
        leaStatusText   = findViewById(R.id.leaStatusText);
        leaResult       = findViewById(R.id.leaResult);
        leaResultText   = findViewById(R.id.leaResultText);
        leaLoading      = findViewById(R.id.leaLoading);
        leaInput        = findViewById(R.id.leaInput);
    }

    // ── Editor setup ──────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private void setupEditor() {
        WebSettings s = webEditor.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        webEditor.setWebChromeClient(new WebChromeClient());
        webEditor.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                editorReady = true;
                if (currentDoc != null && currentDoc.content != null && !currentDoc.content.isEmpty()) {
                    loadContentIntoEditor(currentDoc.content);
                }
            }
        });
        webEditor.addJavascriptInterface(new AndroidBridge(), "Android");
        webEditor.loadDataWithBaseURL(null, EDITOR_HTML, "text/html", "UTF-8", null);
    }

    // ── Listeners ─────────────────────────────────────────────────
    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> handleBackPress());

        btnSave.setOnClickListener(v -> saveCurrentDoc());

        btnDelete.setOnClickListener(v -> {
            if (currentDoc == null) return;
            new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer « " + currentDoc.title + " » ?")
                .setPositiveButton("Supprimer", (d, w) -> { deleteDoc(currentDoc); showListScreen(); })
                .setNegativeButton("Annuler", null)
                .show();
        });

        Button btnNewDoc = findViewById(R.id.btnNewDoc);
        btnNewDoc.setOnClickListener(v -> createNewDoc());

        // Formatting
        setupFmt(R.id.fmtBold,    "bold",                null);
        setupFmt(R.id.fmtItalic,  "italic",              null);
        setupFmt(R.id.fmtUnder,   "underline",           null);
        setupFmt(R.id.fmtH1,      "formatBlock",         "h1");
        setupFmt(R.id.fmtH2,      "formatBlock",         "h2");
        setupFmt(R.id.fmtPara,    "formatBlock",         "p");
        setupFmt(R.id.fmtUList,   "insertUnorderedList", null);
        setupFmt(R.id.fmtOList,   "insertOrderedList",   null);
        setupFmt(R.id.fmtAlignL,  "justifyLeft",         null);
        setupFmt(R.id.fmtAlignC,  "justifyCenter",       null);
        setupFmt(R.id.fmtClear,   "removeFormat",        null);

        // Léa panel toggle
        LinearLayout leaBar = findViewById(R.id.leaBar);
        leaBar.setOnClickListener(v -> toggleLeaPanel());

        // Quick actions
        setupQA(R.id.qaCorriger,   "Corrige les fautes d'orthographe et de grammaire du texte suivant sans changer le sens :");
        setupQA(R.id.qaAmeliorer,  "Améliore la qualité d'écriture et le style du texte suivant tout en conservant le sens :");
        setupQA(R.id.qaReformuler, "Reformule complètement le texte suivant avec d'autres mots tout en gardant le même sens :");
        setupQA(R.id.qaContinuer,  "Continue naturellement ce texte en ajoutant 2 à 3 paragraphes dans le même style :");

        // Léa send
        Button btnLeaSend = findViewById(R.id.btnLeaSend);
        btnLeaSend.setOnClickListener(v -> {
            String instr = leaInput.getText().toString().trim();
            if (!instr.isEmpty()) sendToLea(instr);
        });

        // Result buttons
        Button btnInsert  = findViewById(R.id.btnInsert);
        Button btnReplace = findViewById(R.id.btnReplace);
        Button btnRetry   = findViewById(R.id.btnRetry);

        btnInsert.setOnClickListener(v -> insertResult(false));
        btnReplace.setOnClickListener(v -> insertResult(true));
        btnRetry.setOnClickListener(v -> { if (!lastInstruction.isEmpty()) sendToLea(lastInstruction); });
    }

    private void setupFmt(int id, String cmd, String val) {
        Button btn = findViewById(id);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            if (!editorReady) return;
            String js = val != null ? "execCmd('" + cmd + "','" + val + "')" : "execCmd('" + cmd + "')";
            webEditor.evaluateJavascript(js, null);
        });
    }

    private void setupQA(int id, String instruction) {
        Button btn = findViewById(id);
        if (btn == null) return;
        btn.setOnClickListener(v -> sendToLea(instruction));
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void handleBackPress() {
        if (screenEditor.getVisibility() == View.VISIBLE) {
            if (hasUnsavedChanges) {
                new AlertDialog.Builder(this)
                    .setTitle("Sauvegarder ?")
                    .setMessage("Vous avez des modifications non sauvegardées.")
                    .setPositiveButton("Sauvegarder", (d, w) -> { saveCurrentDoc(); showListScreen(); })
                    .setNegativeButton("Ignorer",     (d, w) -> showListScreen())
                    .setNeutralButton("Annuler", null)
                    .show();
            } else {
                showListScreen();
            }
        } else {
            finish();
        }
    }

    // ── Screens ───────────────────────────────────────────────────
    private void showListScreen() {
        currentDoc = null;
        hasUnsavedChanges = false;
        screenList.setVisibility(View.VISIBLE);
        screenEditor.setVisibility(View.GONE);
        lblOfficeTitle.setVisibility(View.VISIBLE);
        edDocTitle.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
        loadDocumentList();
    }

    private void showEditorScreen(OfficeDoc doc) {
        currentDoc = doc;
        hasUnsavedChanges = false;
        screenList.setVisibility(View.GONE);
        screenEditor.setVisibility(View.VISIBLE);
        lblOfficeTitle.setVisibility(View.GONE);
        edDocTitle.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
        edDocTitle.setText(doc.title);

        if (editorReady) loadContentIntoEditor(doc.content != null ? doc.content : "");
    }

    private void loadContentIntoEditor(String html) {
        String escaped = html
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "");
        webEditor.evaluateJavascript("setContent('" + escaped + "')", null);
    }

    // ── Documents ─────────────────────────────────────────────────
    private void createNewDoc() {
        OfficeDoc doc = new OfficeDoc();
        doc.id = String.valueOf(System.currentTimeMillis());
        doc.title = "Nouveau document";
        doc.content = "";
        doc.modifiedAt = now();
        doc.file = new File(officeDir, "doc_" + doc.id + ".json");
        saveDoc(doc);
        showEditorScreen(doc);
    }

    private void loadDocumentList() {
        documents.clear();
        File[] files = officeDir.listFiles((d, n) -> n.startsWith("doc_") && n.endsWith(".json"));
        if (files != null) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : files) {
                try {
                    StringBuilder sb = new StringBuilder();
                    try (FileReader r = new FileReader(f)) {
                        char[] buf = new char[8192]; int n;
                        while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
                    }
                    JSONObject j = new JSONObject(sb.toString());
                    OfficeDoc doc = new OfficeDoc();
                    doc.id         = j.optString("id");
                    doc.title      = j.optString("title", "Document");
                    doc.content    = j.optString("content", "");
                    doc.modifiedAt = j.optString("modifiedAt", "");
                    doc.file       = f;
                    documents.add(doc);
                } catch (Exception ignored) {}
            }
        }
        renderDocumentList();
    }

    private void renderDocumentList() {
        docListContainer.removeAllViews();
        if (documents.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            return;
        }
        emptyState.setVisibility(View.GONE);

        for (OfficeDoc doc : documents) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFF0D1117);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            card.setLayoutParams(lp);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));

            TextView tvTitle = new TextView(this);
            tvTitle.setText(doc.title);
            tvTitle.setTextColor(0xFFFFFFFF);
            tvTitle.setTextSize(15);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(tvTitle);

            if (doc.content != null && !doc.content.isEmpty()) {
                String preview = doc.content.replaceAll("<[^>]+>", "").trim();
                if (preview.length() > 90) preview = preview.substring(0, 90) + "…";
                if (!preview.isEmpty()) {
                    TextView tvPrev = new TextView(this);
                    tvPrev.setText(preview);
                    tvPrev.setTextColor(0xFF4A5568);
                    tvPrev.setTextSize(12);
                    tvPrev.setPadding(0, dp(4), 0, 0);
                    card.addView(tvPrev);
                }
            }

            TextView tvDate = new TextView(this);
            tvDate.setText(doc.modifiedAt);
            tvDate.setTextColor(0xFF2A3A50);
            tvDate.setTextSize(11);
            tvDate.setPadding(0, dp(6), 0, 0);
            card.addView(tvDate);

            card.setOnClickListener(v -> showEditorScreen(doc));
            docListContainer.addView(card);
        }
    }

    private void saveCurrentDoc() {
        if (currentDoc == null) return;
        String t = edDocTitle.getText().toString().trim();
        currentDoc.title = t.isEmpty() ? "Document sans titre" : t;
        currentDoc.modifiedAt = now();
        webEditor.evaluateJavascript("getContent()", html -> {
            if (html != null && html.length() > 2) {
                currentDoc.content = html.substring(1, html.length() - 1)
                    .replace("\\/", "/")
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "");
            }
            saveDoc(currentDoc);
            hasUnsavedChanges = false;
            ui.post(() -> Toast.makeText(this, "Sauvegardé ✓", Toast.LENGTH_SHORT).show());
        });
    }

    private void saveDoc(OfficeDoc doc) {
        try {
            JSONObject j = new JSONObject();
            j.put("id",         doc.id);
            j.put("title",      doc.title);
            j.put("content",    doc.content != null ? doc.content : "");
            j.put("modifiedAt", doc.modifiedAt);
            try (FileWriter w = new FileWriter(doc.file)) { w.write(j.toString()); }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDoc(OfficeDoc doc) {
        if (doc.file != null && doc.file.exists()) doc.file.delete();
        documents.remove(doc);
    }

    // ── Léa AI panel ──────────────────────────────────────────────
    private void toggleLeaPanel() {
        boolean open = leaExpanded.getVisibility() == View.VISIBLE;
        leaExpanded.setVisibility(open ? View.GONE : View.VISIBLE);
        leaChevron.setText(open ? "▲" : "▼");
    }

    private void sendToLea(String instruction) {
        if (!editorReady) return;
        lastInstruction = instruction;
        if (leaExpanded.getVisibility() != View.VISIBLE) {
            leaExpanded.setVisibility(View.VISIBLE);
            leaChevron.setText("▼");
        }
        leaLoading.setVisibility(View.VISIBLE);
        leaResult.setVisibility(View.GONE);
        leaStatusText.setText("Léa réfléchit…");

        // Récupère le texte sélectionné ou le contenu complet
        webEditor.evaluateJavascript("getSelectedText()", sel -> {
            String selected = sel != null ? sel.replace("\"", "").trim() : "";
            if (selected.isEmpty()) {
                webEditor.evaluateJavascript("getContent()", full -> {
                    String plain = full != null ? full.replaceAll("<[^>]+>", "").replace("\"", "").trim() : "";
                    callLea(instruction, plain.isEmpty() ? null : plain);
                });
            } else {
                callLea(instruction, selected);
            }
        });
    }

    private void callLea(String instruction, String textContext) {
        SharedPreferences prefs = getSharedPreferences("lea_prefs", MODE_PRIVATE);
        String username = prefs.getString("current_user", "user");

        try {
            JSONObject body = new JSONObject();
            body.put("username",    username);
            body.put("instruction", instruction);
            body.put("text",        textContext != null ? textContext : "");

            Request req = new Request.Builder()
                .url(getServerHost() + "/api/office/assist")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

            httpClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {
                    ui.post(() -> showLeaError("Réseau : " + e.getMessage()));
                }
                @Override public void onResponse(Call call, Response resp) throws java.io.IOException {
                    try (resp) {
                        String s = resp.body() != null ? resp.body().string() : "";
                        if (!resp.isSuccessful()) { ui.post(() -> showLeaError("HTTP " + resp.code())); return; }
                        String result = new JSONObject(s).getString("result");
                        ui.post(() -> showLeaResult(result));
                    } catch (Exception e) {
                        ui.post(() -> showLeaError("Erreur : " + e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            showLeaError("Erreur : " + e.getMessage());
        }
    }

    private void showLeaResult(String result) {
        leaLoading.setVisibility(View.GONE);
        leaResult.setVisibility(View.VISIBLE);
        leaResultText.setText(result);
        leaStatusText.setText("Réponse reçue ✓");
    }

    private void showLeaError(String error) {
        leaLoading.setVisibility(View.GONE);
        leaStatusText.setText("⚠ " + error);
    }

    private void insertResult(boolean replaceSelection) {
        String text = leaResultText.getText().toString();
        if (text.isEmpty() || !editorReady) return;
        String esc = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "<br>\\n");
        if (replaceSelection) {
            webEditor.evaluateJavascript("replaceSelection('<p>" + esc + "</p>')", null);
            leaStatusText.setText("Sélection remplacée ✓");
        } else {
            webEditor.evaluateJavascript("appendText('<p>" + esc + "</p>')", null);
            leaStatusText.setText("Texte inséré ✓");
        }
    }

    // ── WebView bridge ────────────────────────────────────────────
    private class AndroidBridge {
        @JavascriptInterface
        public void onContentChanged() {
            ui.post(() -> hasUnsavedChanges = true);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────
    private String getServerHost() {
        SharedPreferences p = getSharedPreferences("lea_prefs", MODE_PRIVATE);
        String h = p.getString("server_host", "https://lea-bunker.lea-ia-local.com");
        if (h.startsWith("https://") && h.endsWith(":3001")) h = h.substring(0, h.length() - 5);
        return h;
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    // ── HTML de l'éditeur ─────────────────────────────────────────
    private static final String EDITOR_HTML =
        "<!DOCTYPE html><html><head>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>" +
        "<style>" +
        "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}" +
        "html,body{height:100%;margin:0;padding:0;background:#080E1B}" +
        "#ed{padding:20px 16px 80px;min-height:100%;font-family:'Georgia',serif;" +
        "font-size:16px;line-height:1.8;color:#e6edf3;outline:none;word-break:break-word;caret-color:#00f2ff}" +
        "#ed:empty::before{content:attr(data-ph);color:#484f58;pointer-events:none}" +
        "h1{font-size:26px;font-weight:700;margin:20px 0 10px;color:#fff}" +
        "h2{font-size:20px;font-weight:600;margin:16px 0 8px;color:#e0e8f0}" +
        "blockquote{border-left:3px solid #00f2ff;padding:4px 0 4px 12px;margin:8px 0;color:#8b949e}" +
        "ul,ol{padding-left:24px;margin:8px 0}" +
        "b,strong{color:#fff}" +
        "</style></head><body>" +
        "<div id='ed' contenteditable='true' data-ph='Commencez à écrire…'></div>" +
        "<script>" +
        "var e=document.getElementById('ed');" +
        "function getContent(){return e.innerHTML}" +
        "function setContent(h){e.innerHTML=h}" +
        "function execCmd(c,v){document.execCommand(c,false,v||null);e.focus()}" +
        "function getSelectedText(){var s=window.getSelection();return s?s.toString():''}" +
        "function replaceSelection(html){" +
        "  e.focus();var s=window.getSelection();" +
        "  if(!s||!s.rangeCount){appendText(html);return;}" +
        "  var r=s.getRangeAt(0);r.deleteContents();" +
        "  var el=document.createElement('div');el.innerHTML=html;" +
        "  var f=document.createDocumentFragment(),n,last;" +
        "  while((n=el.firstChild)){last=f.appendChild(n)}" +
        "  r.insertNode(f);" +
        "  if(last){r.setStartAfter(last);r.collapse(true);s.removeAllRanges();s.addRange(r)}" +
        "}" +
        "function appendText(html){" +
        "  var p=document.createElement('p');p.innerHTML=html;e.appendChild(p);" +
        "  p.scrollIntoView({behavior:'smooth'})" +
        "}" +
        "e.addEventListener('input',function(){try{Android.onContentChanged()}catch(x){}})" +
        "</script></body></html>";
}
