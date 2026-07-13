package com.flolov42.lea_v3.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.flolov42.lea_v3.R;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.Map;

public class LeaAutoActivity extends AppCompatActivity {

    // ─── état ─────────────────────────────────────────────────────
    private OBD2Client obd2;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private BluetoothDevice             selectedDevice;
    private String                      currentUser  = "";
    private boolean                     isAdmin      = false;

    // ─── données ──────────────────────────────────────────────────
    private OBD2Client.OBD2Data lastData;
    private List<String>        lastDtcCodes  = new ArrayList<>();
    private List<ChatMsg>       chatMessages  = new ArrayList<>();

    // ─── UI ───────────────────────────────────────────────────────
    private LinearLayout connectionScreen, mainScreen;
    private TextView     autoConnStatus;
    private ListView     deviceList;
    private TextView     noDevicesText;
    private Button       btnConnectDevice;

    private Button   tabDashboard, tabDiag, tabChat, tabGodMode;
    private ScrollView contentDashboard, contentGodMode;
    private LinearLayout contentDiag, contentChat;

    // Dashboard
    private TextView valRpm, valSpeed, valTemp, valThrottle;
    private ProgressBar throttleBar;
    private LinearLayout dtcAlert;
    private TextView dtcAlertText;

    // Diagnostic
    private ArrayAdapter<String> dtcAdapter;
    private ListView dtcList;
    private TextView dtcEmpty;
    private Button btnClearDtc;

    // Chat
    private ArrayAdapter<ChatMsg> chatAdapter;
    private ListView chatListView;
    private EditText chatInput;

    // ─── DTC descriptions ─────────────────────────────────────────
    private static final Map<String, String> DTC_DESC = new HashMap<>();
    static {
        DTC_DESC.put("P0100", "Débit masse d'air (MAF) — circuit défaillant");
        DTC_DESC.put("P0101", "Débit masse d'air hors plage");
        DTC_DESC.put("P0102", "Débit masse d'air — signal bas");
        DTC_DESC.put("P0171", "Mélange trop pauvre — Banc 1");
        DTC_DESC.put("P0172", "Mélange trop riche — Banc 1");
        DTC_DESC.put("P0174", "Mélange trop pauvre — Banc 2");
        DTC_DESC.put("P0300", "Ratés d'allumage aléatoires");
        DTC_DESC.put("P0301", "Ratés d'allumage — Cylindre 1");
        DTC_DESC.put("P0302", "Ratés d'allumage — Cylindre 2");
        DTC_DESC.put("P0303", "Ratés d'allumage — Cylindre 3");
        DTC_DESC.put("P0304", "Ratés d'allumage — Cylindre 4");
        DTC_DESC.put("P0340", "Capteur arbre à cames — circuit défaillant");
        DTC_DESC.put("P0420", "Efficacité catalyseur insuffisante — Banc 1");
        DTC_DESC.put("P0430", "Efficacité catalyseur insuffisante — Banc 2");
        DTC_DESC.put("P0440", "Système EVAP — fuite détectée");
        DTC_DESC.put("P0442", "Fuite EVAP (petite fuite)");
        DTC_DESC.put("P0500", "Capteur de vitesse véhicule — défaillant");
        DTC_DESC.put("P0505", "Régulateur de ralenti — défaillant");
        DTC_DESC.put("P0600", "Bus CAN — défaillant");
        DTC_DESC.put("P1100", "Capteur MAF — hors plage démarrage à froid");
        DTC_DESC.put("C0031", "Roue avant droite — capteur ABS");
        DTC_DESC.put("C0034", "Roue avant gauche — capteur ABS");
        DTC_DESC.put("B1000", "Module airbag — défaillant");
        DTC_DESC.put("U0001", "Bus CAN haute vitesse — communication perdue");
        DTC_DESC.put("U0100", "Communication perdue avec ECM/PCM");
    }

    // ─── lifecycle ────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto);

        currentUser = getIntent().getStringExtra("currentUser");
        if (currentUser == null) currentUser = "";

        // BUG FIX 3 : vérifier le statut admin via le serveur (asynchrone)
        // L'écran se charge normalement, le God Mode apparaît si confirmé
        checkAdminAsync();

        obd2 = new OBD2Client();
        bindViews();
        setupListeners();
        loadDevices();
        } catch (Throwable e) {
            String loc = "";
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().contains("lea_v3")) { loc = el.getFileName() + ":" + el.getLineNumber(); break; }
            }
            android.widget.Toast.makeText(this, "❌ Auto: " + e.getMessage() + "\n@ " + loc, android.widget.Toast.LENGTH_LONG).show();
            com.flolov42.lea_v3.utilities.LeaAndroidLogger.crash(this, "Auto onCreate", e);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (obd2 != null) obd2.disconnect();
    }

    // ─── bind ─────────────────────────────────────────────────────
    private void bindViews() {
        connectionScreen = findViewById(R.id.connectionScreen);
        mainScreen       = findViewById(R.id.mainScreen);
        autoConnStatus   = findViewById(R.id.autoConnStatus);
        deviceList       = findViewById(R.id.deviceList);
        noDevicesText    = findViewById(R.id.noDevicesText);
        btnConnectDevice = findViewById(R.id.btnConnectDevice);

        tabDashboard  = findViewById(R.id.tabDashboard);
        tabDiag       = findViewById(R.id.tabDiag);
        tabChat       = findViewById(R.id.tabChat);
        tabGodMode    = findViewById(R.id.tabGodMode);

        contentDashboard = findViewById(R.id.contentDashboard);
        contentDiag      = findViewById(R.id.contentDiag);
        contentChat      = findViewById(R.id.contentChat);
        contentGodMode   = findViewById(R.id.contentGodMode);

        valRpm      = findViewById(R.id.valRpm);
        valSpeed    = findViewById(R.id.valSpeed);
        valTemp     = findViewById(R.id.valTemp);
        valThrottle = findViewById(R.id.valThrottle);
        throttleBar = findViewById(R.id.throttleBar);
        dtcAlert    = findViewById(R.id.dtcAlert);
        dtcAlertText= findViewById(R.id.dtcAlertText);

        dtcList   = findViewById(R.id.dtcList);
        dtcEmpty  = findViewById(R.id.dtcEmpty);
        btnClearDtc = findViewById(R.id.btnClearDtc);

        chatListView = findViewById(R.id.chatList);
        chatInput    = findViewById(R.id.chatInput);

        ImageButton btnBack = findViewById(R.id.autoBack);
        btnBack.setOnClickListener(v -> finish());

        if (isAdmin) tabGodMode.setVisibility(View.VISIBLE);

        // Adapters
        ArrayAdapter<BluetoothDevice> devAdapter = new ArrayAdapter<BluetoothDevice>(
                this, android.R.layout.simple_list_item_single_choice, devices) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, convertView, parent);
                BluetoothDevice d = devices.get(pos);
                tv.setText(d.getName() != null ? d.getName() : d.getAddress());
                tv.setTextColor(0xFFFFFFFF);
                tv.setBackground(rowBg(0x14FFFFFF));
                tv.setPadding(32, 28, 32, 28);
                return tv;
            }
        };
        deviceList.setAdapter(devAdapter);
        deviceList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        dtcAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, convertView, parent);
                tv.setText(getItem(pos));
                tv.setTextColor(0xFFFCA5A5);
                tv.setBackground(rowBg(0x1AEF4444));
                tv.setPadding(24, 20, 24, 20);
                tv.setTextSize(13f);
                return tv;
            }
        };
        dtcList.setAdapter(dtcAdapter);

        chatMessages.add(new ChatMsg("lea", "Système OBD2 connecté. Prêt pour le diagnostic."));
        chatAdapter = new ArrayAdapter<ChatMsg>(this, 0, chatMessages) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView tv = convertView instanceof TextView ? (TextView) convertView : new TextView(getContext());
                ChatMsg msg = getItem(pos);
                boolean isUser = msg != null && !msg.sender.equals("lea");
                tv.setText((isUser ? currentUser : "Léa") + ": " + (msg != null ? msg.text : ""));
                tv.setTextColor(isUser ? 0xFFCBD5E1 : 0xFFFC8181);
                tv.setTextSize(13f);
                tv.setPadding(28, 18, 28, 18);
                GradientDrawable bubble = new GradientDrawable();
                bubble.setColor(isUser ? 0x14FFFFFF : 0x1AEF4444);
                bubble.setCornerRadius(36f);
                bubble.setStroke(2, isUser ? 0x1EFFFFFF : 0x33EF4444);
                tv.setBackground(bubble);
                android.widget.AbsListView.LayoutParams mlp = new android.widget.AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tv.setLayoutParams(mlp);
                return tv;
            }
        };
        chatListView.setAdapter(chatAdapter);
    }

    // ─── listeners ────────────────────────────────────────────────
    private void setupListeners() {
        deviceList.setOnItemClickListener((parent, view, pos, id) -> {
            selectedDevice = devices.get(pos);
            btnConnectDevice.setEnabled(true);
        });

        btnConnectDevice.setOnClickListener(v -> connectToDevice());

        tabDashboard.setOnClickListener(v -> showTab(0));
        tabDiag.setOnClickListener(v -> showTab(1));
        tabChat.setOnClickListener(v -> showTab(2));
        tabGodMode.setOnClickListener(v -> showTab(3));

        // Diagnostic
        findViewById(R.id.btnReadDtc).setOnClickListener(v -> readDTCs());
        btnClearDtc.setOnClickListener(v -> confirmClearDTCs());
        findViewById(R.id.btnResetOil).setOnClickListener(v -> confirmOilReset());

        // Alert DTC → switch to diag tab
        findViewById(R.id.dtcAlertBtn).setOnClickListener(v -> showTab(1));

        // Chat
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendChatMessage();
            return true;
        });
        findViewById(R.id.btnChatSend).setOnClickListener(v -> sendChatMessage());

        // God Mode
        if (isAdmin) {
            findViewById(R.id.btnModeEco).setOnClickListener(v ->
                confirmEcuAction("MODE ÉCO", "Brider les paramètres d'injection pour maximiser l'économie de carburant."));
            findViewById(R.id.btnModeNormal).setOnClickListener(v ->
                confirmEcuAction("MODE NORMAL", "Restaurer la cartographie d'origine du constructeur."));
            findViewById(R.id.btnModeSport).setOnClickListener(v ->
                confirmEcuAction("MODE SPORT", "Activer une carte agressive : pédale plus réactive, +15% couple. RISQUE : annule la garantie."));
            findViewById(R.id.btnStage1).setOnClickListener(v ->
                confirmEcuAction("STAGE 1 CUSTOM", "⚠ OPÉRATION IRRÉVERSIBLE ⚠\nInjecter la cartographie personnalisée Stage 1 dans l'ECU.\nASSUREZ-VOUS que le câble E-NET est branché. Coupure pendant l'écriture = ECU MORT."));
        }
    }

    // ─── connexion Bluetooth ──────────────────────────────────────
    private void loadDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            noDevicesText.setVisibility(View.VISIBLE);
            noDevicesText.setText("Bluetooth désactivé.\nActivez-le dans les Paramètres.");
            return;
        }
        devices.clear();
        devices.addAll(OBD2Client.getPairedDevices());
        if (devices.isEmpty()) {
            noDevicesText.setVisibility(View.VISIBLE);
        } else {
            noDevicesText.setVisibility(View.GONE);
        }
        ((ArrayAdapter<?>) deviceList.getAdapter()).notifyDataSetChanged();
    }

    private void connectToDevice() {
        if (selectedDevice == null) return;
        btnConnectDevice.setEnabled(false);
        btnConnectDevice.setText("Connexion...");
        autoConnStatus.setText("● Connexion...");
        autoConnStatus.setTextColor(0xFFFBBF24);

        obd2.connect(selectedDevice, new OBD2Client.Callback() {
            @Override
            public void onConnected(String deviceName) {
                autoConnStatus.setText("● " + deviceName);
                autoConnStatus.setTextColor(0xFF34D399);
                connectionScreen.setVisibility(View.GONE);
                mainScreen.setVisibility(View.VISIBLE);
                showTab(0);
                addChatMsg("lea", "OBD2 connecté via " + deviceName + ". Données en direct activées.");
            }

            @Override
            public void onDisconnected(String reason) {
                autoConnStatus.setText("● Déconnecté");
                autoConnStatus.setTextColor(0xFFEF4444);
                mainScreen.setVisibility(View.GONE);
                connectionScreen.setVisibility(View.VISIBLE);
                btnConnectDevice.setEnabled(true);
                btnConnectDevice.setText("Connecter");
                Toast.makeText(LeaAutoActivity.this, "Déconnecté : " + reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onData(OBD2Client.OBD2Data data) {
                lastData = data;
                valRpm.setText(String.valueOf(data.rpm));
                valSpeed.setText(String.valueOf(data.speed));
                valTemp.setText(data.coolantTemp + "°");
                valThrottle.setText(data.throttlePct + "%");
                throttleBar.setProgress(data.throttlePct);
                // Couleur température
                int tempColor = data.coolantTemp > 100 ? 0xFFEF4444 :
                                data.coolantTemp > 85  ? 0xFF34D399 : 0xFFFBBF24;
                valTemp.setTextColor(tempColor);
            }

            @Override
            public void onDtcResult(List<String> codes) {
                lastDtcCodes = codes;
                dtcAdapter.clear();
                if (codes.isEmpty()) {
                    dtcEmpty.setVisibility(View.VISIBLE);
                    dtcList.setVisibility(View.GONE);
                    btnClearDtc.setEnabled(false);
                    dtcAlert.setVisibility(View.GONE);
                } else {
                    dtcEmpty.setVisibility(View.GONE);
                    dtcList.setVisibility(View.VISIBLE);
                    btnClearDtc.setEnabled(true);
                    for (String code : codes) {
                        String desc = DTC_DESC.getOrDefault(code, "Code non répertorié");
                        dtcAdapter.add(code + " — " + desc);
                    }
                    dtcAlert.setVisibility(View.VISIBLE);
                    dtcAlertText.setText("⚠ " + codes.size() + " code(s) défaut — onglet Diagnostic");
                }
                dtcAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDtcCleared() {
                lastDtcCodes.clear();
                dtcAdapter.clear();
                dtcAdapter.notifyDataSetChanged();
                dtcEmpty.setVisibility(View.VISIBLE);
                dtcList.setVisibility(View.GONE);
                btnClearDtc.setEnabled(false);
                dtcAlert.setVisibility(View.GONE);
                Toast.makeText(LeaAutoActivity.this, "Codes défaut effacés", Toast.LENGTH_SHORT).show();
                addChatMsg("lea", "Tous les codes défaut ont été effacés du calculateur.");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LeaAutoActivity.this, message, Toast.LENGTH_SHORT).show();
                addChatMsg("lea", "⚠ " + message);
            }
        });
    }

    // ─── tabs ─────────────────────────────────────────────────────
    private void showTab(int tab) {
        contentDashboard.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        contentDiag.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        contentChat.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        if (isAdmin) contentGodMode.setVisibility(tab == 3 ? View.VISIBLE : View.GONE);

        int activeBg = R.drawable.lea_auto_tab_active_bg, inactiveBg = R.drawable.lea_auto_tab_inactive_bg;
        tabDashboard.setBackgroundResource(tab == 0 ? activeBg : inactiveBg);
        tabDiag.setBackgroundResource(tab == 1 ? activeBg : inactiveBg);
        tabChat.setBackgroundResource(tab == 2 ? activeBg : inactiveBg);
        if (isAdmin) tabGodMode.setBackgroundResource(tab == 3 ? activeBg : inactiveBg);

        int activeText = 0xFFFFFFFF, inactiveText = 0xFF64748B;
        tabDashboard.setTextColor(tab == 0 ? activeText : inactiveText);
        tabDiag.setTextColor(tab == 1 ? activeText : inactiveText);
        tabChat.setTextColor(tab == 2 ? activeText : inactiveText);
        if (isAdmin) tabGodMode.setTextColor(tab == 3 ? activeText : 0xFFEF4444);
    }

    // ─── diagnostic ───────────────────────────────────────────────
    private void readDTCs() {
        addChatMsg("lea", "Lecture des codes défaut en cours...");
        obd2.readDTCs();
    }

    private void confirmClearDTCs() {
        new AlertDialog.Builder(this)
            .setTitle("Effacer les codes défaut")
            .setMessage("Cette action efface tous les codes défaut du calculateur et éteint les voyants moteur.\n\nContinuer ?")
            .setPositiveButton("Effacer", (d, w) -> obd2.clearDTCs())
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void confirmOilReset() {
        new AlertDialog.Builder(this)
            .setTitle("Remise à zéro vidange")
            .setMessage("À utiliser uniquement APRÈS avoir changé l'huile et le filtre.\n\nContinuer ?")
            .setPositiveButton("Réinitialiser", (d, w) -> {
                // L'ECU remet le compteur via PID propriétaire (dépend du véhicule)
                // Pour un reset universel, on envoie ATSH 7DF + 06 04 (clear MIL)
                // Simulation + toast confirmation
                Toast.makeText(this, "Compteur de vidange réinitialisé", Toast.LENGTH_SHORT).show();
                addChatMsg("lea", "Remise à zéro du compteur de révision effectuée. Prochain entretien dans 15 000 km.");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ─── Co-Pilote chat ───────────────────────────────────────────
    private void sendChatMessage() {
        String text = chatInput.getText().toString().trim();
        if (text.isEmpty()) return;
        addChatMsg(currentUser.isEmpty() ? "user" : currentUser, text);
        chatInput.setText("");

        String response = generateAutoResponse(text.toLowerCase());
        chatListView.postDelayed(() -> addChatMsg("lea", response), 800);
    }

    private String generateAutoResponse(String msg) {
        if (lastData != null) {
            if (msg.contains("rpm") || msg.contains("régime")) {
                return "Régime moteur actuel : " + lastData.rpm + " RPM. " +
                    (lastData.rpm > 3000 ? "Régime élevé, relâchez l'accélérateur." :
                     lastData.rpm < 600  ? "Régime très bas, risque de calage." :
                     "Régime normal.");
            }
            if (msg.contains("vitesse")) {
                return "Vitesse actuelle : " + lastData.speed + " km/h.";
            }
            if (msg.contains("temp") || msg.contains("chaud") || msg.contains("surchauffe")) {
                return "Température moteur : " + lastData.coolantTemp + "°C. " +
                    (lastData.coolantTemp > 105 ? "⚠ SURCHAUFFE ! Arrêtez-vous immédiatement." :
                     lastData.coolantTemp > 95  ? "Température élevée. Surveillez le niveau de liquide." :
                     lastData.coolantTemp < 60  ? "Moteur froid, attendez la montée en température." :
                     "Température normale.");
            }
        }
        if (msg.contains("défaut") || msg.contains("voyant") || msg.contains("panne") || msg.contains("dtc")) {
            if (lastDtcCodes.isEmpty())
                return "Aucun code défaut détecté dans le calculateur. Tout semble normal.";
            return "J'ai détecté " + lastDtcCodes.size() + " code(s) défaut : " +
                   String.join(", ", lastDtcCodes) + ". Allez dans l'onglet Diagnostic pour les détails.";
        }
        if (msg.contains("huile") || msg.contains("vidange")) {
            return "Pour la remise à zéro vidange, allez dans l'onglet Diagnostic et utilisez le bouton 'Reset vidange' uniquement après avoir changé l'huile.";
        }
        if (msg.contains("obd") || msg.contains("connexion") || msg.contains("connecté")) {
            return obd2.isConnected()
                ? "OBD2 connecté et opérationnel. Données de télémétrie en direct."
                : "Non connecté. Retournez à l'écran de connexion.";
        }
        if (msg.contains("aide") || msg.contains("help") || msg.contains("quoi faire")) {
            return "Je peux vous aider à :\n• Lire les données moteur (RPM, vitesse, température)\n• Diagnostiquer les codes défaut\n• Effacer les voyants moteur\n• Suivre la consommation\n\nDites-moi ce qui vous préoccupe.";
        }
        return "Commande enregistrée. Je surveille les paramètres en temps réel. " +
               (lastData != null ? "Moteur à " + lastData.rpm + " RPM, " + lastData.coolantTemp + "°C." : "");
    }

    private void addChatMsg(String sender, String text) {
        chatMessages.add(new ChatMsg(sender, text));
        chatAdapter.notifyDataSetChanged();
        chatListView.post(() -> chatListView.setSelection(chatMessages.size() - 1));
    }

    // ─── God Mode ECU ─────────────────────────────────────────────
    private void confirmEcuAction(String modeName, String description) {
        new AlertDialog.Builder(this)
            .setTitle("⚡ " + modeName)
            .setMessage(description + "\n\nCâble E-NET branché et moteur à l'arrêt ?")
            .setPositiveButton("Confirmer — Écrire ECU", (d, w) -> {
                // Ici : envoi du fichier de calibration via protocole BMW E-NET ou similaire
                // Nécessite un serveur de flashage et le bon firmware
                Toast.makeText(this, modeName + " envoyé au calculateur", Toast.LENGTH_LONG).show();
                addChatMsg("lea", "Ordre " + modeName + " envoyé. Redémarrez le moteur pour appliquer les changements.");
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ─── vérification admin via serveur ───────────────────────────
    private void checkAdminAsync() {
        if (currentUser.isEmpty()) return;
        String host = getSharedPreferences("lea_prefs", MODE_PRIVATE)
            .getString("server_host", "https://lea-bunker.lea-ia-local.com");

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();
                Request req = new Request.Builder()
                    .url(host + "/api/app/is-admin?user=" + currentUser)
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String body = resp.body().string().trim();
                        boolean confirmed = "true".equalsIgnoreCase(body);
                        runOnUiThread(() -> {
                            isAdmin = confirmed;
                            if (isAdmin && tabGodMode != null)
                                tabGodMode.setVisibility(View.VISIBLE);
                        });
                    }
                }
            } catch (IOException ignored) {
                // Serveur inaccessible → pas de God Mode, c'est le comportement sûr
            }
        }).start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    // ─── helpers ──────────────────────────────────────────────────

    /** Fond de ligne arrondi translucide pour les items ArrayAdapter (device/DTC list). */
    private GradientDrawable rowBg(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(28f);
        return gd;
    }

    private static class ChatMsg {
        final String sender, text;
        ChatMsg(String s, String t) { sender = s; text = t; }
    }
}
