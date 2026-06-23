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
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class LeaCodeAgent {

    private static final String ID    = LeaAgentActivationManager.CODE;
    private static final String PREFS = "lea_code_agent";

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL   = "claude-haiku-4-5-20251001";

    private final Context               ctx;
    private final LeaAgentDatabase      db;
    private final LeaCodeProjectStorage storage;
    private final LeaCodeFileManager    fileManager;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences     prefs;

    private long   currentProjectId = -1;

    public LeaCodeAgent(Context ctx) {
        this.ctx         = ctx;
        this.db          = LeaAgentDatabase.get(ctx);
        this.storage     = new LeaCodeProjectStorage(ctx);
        this.fileManager = new LeaCodeFileManager(ctx);
        this.notif       = LeaAgentNotificationManager.get(ctx);
        this.prefs       = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Called periodically by service when idle */
    public void runIdle() {
        db.addLog(ID, "💻 Code Agent en veille — prêt à coder");
    }

    /** Start a new coding project conversation */
    public long startProject(String projectName, String initialRequest, ResponseCallback callback) {
        File root = fileManager.getProjectRoot(projectName);
        currentProjectId = storage.createProject(projectName, initialRequest, root.getAbsolutePath());

        db.addLog(ID, "🚀 Nouveau projet: " + projectName);

        // Initial AI response
        String systemPrompt = buildSystemPrompt();
        String userMsg = "Je veux créer une application Android appelée \"" + projectName + "\".\n\n" + initialRequest;

        storage.addConversationMessage(currentProjectId, "user", userMsg);

        callClaudeApi(systemPrompt, currentProjectId, callback);
        return currentProjectId;
    }

    /** Continue conversation on existing project */
    public void continueConversation(long projectId, String userMessage, ResponseCallback callback) {
        currentProjectId = projectId;
        storage.addConversationMessage(projectId, "user", userMessage);
        callClaudeApi(buildSystemPrompt(), projectId, callback);
    }

    /** Generate complete Activity code for a project */
    public void generateCode(long projectId, String className, String description, ResponseCallback callback) {
        String prompt = "Génère le code Java COMPLET et COMPILABLE pour une classe Android appelée \"" +
            className + "\".\n\nDescription: " + description + "\n\n" +
            "RÈGLES:\n" +
            "- Package: com.lea.generated\n" +
            "- Extends Activity (pas AppCompatActivity)\n" +
            "- Thème sombre (BG: #011627, Accent: #00E5FF)\n" +
            "- UI construite programmatiquement (pas XML)\n" +
            "- Code 100% compilable\n" +
            "- Commentaires en français\n" +
            "- Retourne UNIQUEMENT le code Java, sans markdown";

        storage.addConversationMessage(projectId, "user", prompt);
        callClaudeApi(buildSystemPrompt(), projectId, callback);
    }

    private void callClaudeApi(String systemPrompt, long projectId, ResponseCallback callback) {
        new Thread(() -> {
            try {
                String apiKey = prefs.getString("claude_api_key", "");
                if (apiKey.isEmpty()) {
                    String fallback = generateLocalCode(projectId);
                    if (callback != null) callback.onResponse(fallback, false);
                    storage.addConversationMessage(projectId, "assistant", fallback);
                    return;
                }

                List<LeaAgentDatabase.MessageRow> history = storage.getConversation(projectId);

                // Build messages array for Claude API
                JSONArray messages = new JSONArray();
                for (LeaAgentDatabase.MessageRow msg : history) {
                    JSONObject m = new JSONObject();
                    m.put("role", msg.role);
                    m.put("content", msg.content);
                    messages.put(m);
                }

                JSONObject body = new JSONObject();
                body.put("model", CLAUDE_MODEL);
                body.put("max_tokens", 4096);
                body.put("system", systemPrompt);
                body.put("messages", messages);

                URL url = new URL(CLAUDE_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int status = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (status == 200) {
                    JSONObject response = new JSONObject(sb.toString());
                    String text = response.getJSONArray("content")
                                          .getJSONObject(0)
                                          .getString("text");

                    storage.addConversationMessage(projectId, "assistant", text);
                    db.addLog(ID, "💬 Réponse Claude reçue (" + text.length() + " chars)");

                    if (callback != null) callback.onResponse(text, false);

                    // Auto-save generated code if it looks like Java
                    if (text.contains("class ") && text.contains("package ")) {
                        autoSaveCode(projectId, text);
                    }
                } else {
                    String errText = "API Error " + status + ": " + sb.toString().substring(0, Math.min(200, sb.length()));
                    db.addLog(ID, "❌ " + errText);
                    String fallback = generateLocalCode(projectId);
                    storage.addConversationMessage(projectId, "assistant", fallback);
                    if (callback != null) callback.onResponse(fallback, false);
                }

            } catch (Exception e) {
                db.addLog(ID, "⚠️ Erreur API Claude: " + e.getMessage());
                String fallback = generateLocalCode(projectId);
                if (callback != null) callback.onResponse(fallback, false);
            }
        }).start();
    }

    private void autoSaveCode(long projectId, String code) {
        try {
            List<LeaAgentDatabase.ProjectRow> projects = storage.getAllProjects();
            for (LeaAgentDatabase.ProjectRow p : projects) {
                if (p.id == projectId) {
                    File root = new File(p.rootPath);
                    // Extract class name from code
                    String className = extractClassName(code);
                    if (className != null && !className.isEmpty()) {
                        fileManager.writeSourceFile(root, "com.lea.generated", className, code);
                        db.addLog(ID, "💾 Code sauvegardé: " + className + ".java");
                    }
                    break;
                }
            }
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Auto-save échoué: " + e.getMessage());
        }
    }

    private String extractClassName(String code) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)");
        java.util.regex.Matcher m = p.matcher(code);
        return m.find() ? m.group(1) : null;
    }

    /** Generate basic code locally without API */
    private String generateLocalCode(long projectId) {
        List<LeaAgentDatabase.MessageRow> msgs = storage.getConversation(projectId);
        String lastUser = msgs.isEmpty() ? "projet" : msgs.get(msgs.size() - 1).content;

        String projectName = "MonApp";
        List<LeaAgentDatabase.ProjectRow> projects = storage.getAllProjects();
        for (LeaAgentDatabase.ProjectRow p : projects) {
            if (p.id == projectId) { projectName = p.name; break; }
        }

        String className = toCamelCase(projectName.replaceAll("[^a-zA-Z0-9]", "")) + "MainActivity";

        return "package com.lea.generated;\n\n" +
               "import android.app.Activity;\n" +
               "import android.graphics.Color;\n" +
               "import android.graphics.Typeface;\n" +
               "import android.graphics.drawable.GradientDrawable;\n" +
               "import android.os.Bundle;\n" +
               "import android.view.Gravity;\n" +
               "import android.view.ViewGroup;\n" +
               "import android.widget.LinearLayout;\n" +
               "import android.widget.ScrollView;\n" +
               "import android.widget.TextView;\n\n" +
               "// Généré par Léa Code Agent\n" +
               "// Projet: " + projectName + "\n" +
               "public class " + className + " extends Activity {\n\n" +
               "    private static final int BG   = 0xFF011627;\n" +
               "    private static final int CYAN = 0xFF00E5FF;\n" +
               "    private static final int CARD = 0xFF012040;\n\n" +
               "    @Override\n" +
               "    protected void onCreate(Bundle savedInstanceState) {\n" +
               "        super.onCreate(savedInstanceState);\n" +
               "        getWindow().setStatusBarColor(BG);\n" +
               "        getWindow().setNavigationBarColor(BG);\n\n" +
               "        LinearLayout root = new LinearLayout(this);\n" +
               "        root.setOrientation(LinearLayout.VERTICAL);\n" +
               "        root.setBackgroundColor(BG);\n" +
               "        root.setPadding(dp(24), dp(40), dp(24), dp(24));\n\n" +
               "        // Titre\n" +
               "        TextView title = new TextView(this);\n" +
               "        title.setText(\"🚀 " + projectName + "\");\n" +
               "        title.setTextColor(CYAN);\n" +
               "        title.setTextSize(24f);\n" +
               "        title.setTypeface(null, Typeface.BOLD);\n" +
               "        title.setGravity(Gravity.CENTER);\n" +
               "        root.addView(title);\n\n" +
               "        // Description\n" +
               "        TextView desc = new TextView(this);\n" +
               "        desc.setText(\"Application générée par Léa Code Agent\");\n" +
               "        desc.setTextColor(0xFF607D8B);\n" +
               "        desc.setTextSize(14f);\n" +
               "        desc.setGravity(Gravity.CENTER);\n" +
               "        desc.setPadding(0, dp(16), 0, 0);\n" +
               "        root.addView(desc);\n\n" +
               "        setContentView(root);\n" +
               "    }\n\n" +
               "    private int dp(int v) {\n" +
               "        return (int)(v * getResources().getDisplayMetrics().density);\n" +
               "    }\n" +
               "}\n";
    }

    private String buildSystemPrompt() {
        return "Tu es Léa, une IA spécialisée dans le développement Android.\n" +
               "Tu génères du code Java Android COMPILABLE et FONCTIONNEL.\n\n" +
               "RÈGLES STRICTES:\n" +
               "1. Toujours utiliser Activity (pas AppCompatActivity)\n" +
               "2. Thème galaxie: BG=0xFF011627, CYAN=0xFF00E5FF, CARD=0xFF012040\n" +
               "3. UI construite programmatiquement (pas XML)\n" +
               "4. Code 100% compilable sans dépendances externes\n" +
               "5. Commentaires en français\n" +
               "6. Zéro placeholder — du vrai code fonctionnel\n" +
               "7. Quand tu génères du code Java, mets-le dans un bloc ```java ... ```\n" +
               "8. Pose des questions si la demande est ambiguë\n" +
               "9. Propose des architectures claires\n\n" +
               "Aujourd'hui: " + new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    private String toCamelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public void setApiKey(String key) {
        prefs.edit().putString("claude_api_key", key).apply();
        db.addLog(ID, "🔑 Clé API Claude configurée");
    }

    public long getCurrentProjectId() { return currentProjectId; }

    public List<LeaAgentDatabase.ProjectRow> getProjects() {
        return storage.getAllProjects();
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }

    public interface ResponseCallback {
        void onResponse(String text, boolean isStreaming);
    }
}
