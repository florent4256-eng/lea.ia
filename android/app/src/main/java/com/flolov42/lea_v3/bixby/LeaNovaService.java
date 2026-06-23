package com.flolov42.lea_v3.bixby;

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
import com.flolov42.lea_v3.R;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

public class LeaNovaService extends Service {

    // Static ref so LeaVoiceActivity can call abortEverything() on tap-outside
    public static volatile LeaNovaService instance;

    // V2 : channel recréé avec IMPORTANCE_LOW pour éviter la bannière au démarrage
    private static final String CHANNEL_ID = "LeaNovaChannelV2";
    private static final int NOTIFICATION_ID = 42;

    private AudioRecord audioRecord;
    private Thread recordingThread = null;
    private boolean isListening = false;
    private boolean isMicrophoneOccupied = false;
    private Model voskModel;
    
    // VARIABLES D'ÉTAT SOUVERAINES
    // NOTE v2.8.3 Fix A : isMicrophoneMuted et isActiveMode comptent >30 usages directs (=true/false, if(...))
    // — migration vers AtomicBoolean trop risquée sans refactoring complet. Laissé en volatile/boolean.
    // La lecture se fait toujours via une variable locale final par itération (voir Fix B, copie locale micMuted/voskReset).
    private boolean isActiveMode = false; // Faux = cherche "Léa", Vrai = traite une commande
    private volatile boolean isMicrophoneMuted = false; // Le sas de sécurité (Priorité audio) — volatile : lu par le thread Vosk, écrit par le thread principal
    private volatile boolean pendingVoskReset = false; // Signale au thread Vosk de vider son buffer interne après une lecture audio
    private android.media.AudioManager audioManager; // 🎧 Contrôle natif du volume Samsung
    private Object audioFocusRequest; // 🎧 Requête de priorité audio
    private boolean wasMusicPlaying = false; // 🎵 Mémorise si la musique tournait avant l'appel
    private int savedRingerMode = -1; // 🔔 Mémorise le mode sonnerie (vibreur/normal/silencieux) avant que Léa s'active
    private int sessionMessageCount = 0;   // 0 = premier msg de la session → joue le salut
    private String cachedSalutAudio = null;     // Phrase de bienvenue pré-générée par le serveur
    private String cachedReflexionAudio = null; // "Attends, je réfléchis à ta question" pré-générée
    public WebSocketClient wsClient;
    private LocationManager locationManager;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private WindowManager windowManager;
    private FrameLayout floatingSiriContainer;
    private TextView floatingSiriText;
    private android.view.View routineFloatingBtn;  // bouton Routines flottant

    // 💬 VARIABLES DU SAS RÉPONSE SMS
    private boolean isReplyMode = false;
    private String replyRecipientName = "";
    private String replyRecipientNumber = "";

    // 📱 VARIABLES DU SAS SMS SOUVERAIN
    private boolean isSmsMode = false;
    private int smsStep = 0; // 1 = Destinataire, 2 = Message
    private String smsRecipientName = "";
    private String smsRecipientNumber = "";
    private String smsMessageContent = "";
    private FrameLayout smsContainer;
    private TextView smsRecipientView;
    private TextView smsMessageView;

    // 📞 VARIABLES DU SAS APPEL SOUVERAIN
    private boolean isCallMode = false;
    private int callStep = 0; // 1 = Qui ?, 2 = Confirmation
    private String callRecipientName = "";
    private String callRecipientNumber = "";
    private FrameLayout callContainer;
    private TextView callRecipientView;

    // 🖼️ VARIABLES DU SAS GÉNÉRATION IMAGE (guidé 2 étapes)
    private boolean isImageMode = false;

    // 🎲 VARIABLES DU SAS GÉNÉRATION 3D (guidé 2 étapes — micro libéré pendant génération)
    private boolean is3DMode = false;

    // 🎵 VARIABLES DU SAS GÉNÉRATION MUSIQUE (guidé 2 étapes : style → structure)
    private boolean isMusicMode = false;
    private int     musicStep = 0; // 1 = style, 2 = structure
    private String  musicStyle = "";
    private String  musicStructure = "";

    // 👁️ VISION CAMÉRA — photo en attente + question vocale
    volatile String pendingVisionPhoto = null;
    volatile boolean isVisionMode = false;

    // 📋 HISTORIQUE — question en cours (sauvegardée quand la réponse arrive)
    private volatile String currentUserQuestion = "";
    private LeaNovaDataStore db;

    private MediaPlayer mediaPlayer;       // La bouche de LÉA (réponse Ollama)
    private MediaPlayer promptPlayer;      // Lecteur des phrases pré-générées (salut / réflexion)

    // ── MODE CONTINU : Léa reste active 45s après une réponse sans re-trigger ──
    private static final long CONTINUOUS_MODE_DURATION_MS = 45_000L;
    private boolean isContinuousMode = false;
    // Quand true : triggerCooldown() termine la session au lieu d'activer le mode continu
    private volatile boolean pendingEndSession = false;
    private final Handler continuousModeHandler = new Handler(Looper.getMainLooper());
    private Runnable continuousModeRunnable;
    private android.media.session.MediaSession leaMediaSession; // L'intercepteur tactile universel

    // ⏱️ CHRONO DE SÉCURITÉ SOUVERAIN
    private Handler silenceHandler = new Handler(Looper.getMainLooper());
    private Runnable silenceRunnable;
    private static final long SILENCE_TIMEOUT_MS = 10000; // 10 secondes

    // ⏱️ COMMIT PARTIEL — envoie le dernier résultat Vosk si pas de final après 2.5s
    volatile String lastPartialText = "";
    private final Handler partialCommitHandler = new Handler(Looper.getMainLooper());
    private Runnable partialCommitRunnable;

    // 🎧 DÉTECTEUR BLUETOOTH SOUVERAIN (Écouteurs, Montre)
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = (device != null && device.getName() != null) ? device.getName() : "Appareil Bluetooth";
                Log.e("LeaNova", "🎧 Bluetooth détecté : " + deviceName);

                if (audioManager == null) audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                // SCO non démarré ici : l'écoute passive Vosk n'en a pas besoin.
                // La musique continue de jouer normalement via A2DP.

                if (wsClient != null && wsClient.isOpen()) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("type", "SYSTEM_EVENT");
                        payload.put("event", "bluetooth_connected");
                        payload.put("deviceName", deviceName);
                        wsClient.send(payload.toString());
                    } catch (Exception e) { Log.w("LeaNova", "⚠️ [bluetoothReceiver] " + e.getMessage()); }
                }
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.e("LeaNova", "🎧 Appareil Bluetooth déconnecté.");

                isListening = false;
                releaseAudioRecord();

                // ⏱️ On laisse 1.5 seconde à Android pour libérer la puce audio
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.e("LeaNova", "🔄 Redémarrage des oreilles...");
                    startPassiveListening();
                }, 1500);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        db = LeaNovaDataStore.get(this);
        Log.e("LeaNova", "⚙️ Initialisation du cerveau tactique...");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
        // Probe before connecting so getWebSocketUrl() returns the best URL immediately
        LeaNetworkDetector.probeAsync(this);
        new Handler(Looper.getMainLooper()).postDelayed(this::initWebSocket, 3_000);
        initVoskModel();
        // Auto-switch: reconnect when the detection worker changes the active type
        networkChangeReceiver = new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                String newType = i.getStringExtra(LeaNetworkDetectionWorker.EXTRA_TYPE);
                Log.e("LeaNova", "🔀 Auto-switch réseau → " + newType);
                LeaNetworkLogger.get(LeaNovaService.this).switchEvent(
                    LeaNetworkDetector.getCachedType(LeaNovaService.this), newType, "broadcast");
                if (wsClient != null) { try { wsClient.closeBlocking(); } catch (Exception e) {} }
                new Handler(Looper.getMainLooper()).postDelayed(() -> initWebSocket(), 1_000);
            }
        };
        registerReceiver(networkChangeReceiver,
            new android.content.IntentFilter(LeaNetworkDetectionWorker.ACTION_NETWORK_CHANGED),
            Context.RECEIVER_NOT_EXPORTED);
        
        // 👆 CRÉATION DE L'INTERCEPTEUR TACTILE (Le tapotement écouteur)
        leaMediaSession = new android.media.session.MediaSession(this, "LeaSovereignSession");
        leaMediaSession.setCallback(new android.media.session.MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                android.view.KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();
                    // On capte le tapotement universel (Play/Pause ou HeadsetHook)
                    if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || 
                        keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK) {
                        
                        Log.e("LeaNova", "🎧 Tapotement intercepté ! Réveil tactique.");
                        
                        if (audioManager == null) audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        
                        // 1. On mémorise et on fige la musique
                        if (audioManager != null) {
                            wasMusicPlaying = audioManager.isMusicActive();
                            if (wasMusicPlaying) {
                                audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
                                audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
                            }
                        }

                        // 2. Éveil de l'interface et du son
                        isActiveMode = true;
                        showSiriLikeOverlay();
                        if (sessionMessageCount == 0 && cachedSalutAudio != null) {
                            isMicrophoneMuted = true;
                            final String salutSnap = cachedSalutAudio;
                            playVoicePrompt(salutSnap, () -> {
                                isMicrophoneMuted = false;
                                pendingVoskReset = true;
                                updateSiriText("✨ LÉA est à l'écoute...");
                            });
                        } else {
                            updateSiriText("✨ LÉA est à l'écoute...");
                            playSystemSound(R.raw.eveil);
                        }
                        return true; // LÉA absorbe la commande matérielle, la musique reste en pause
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });
        leaMediaSession.setFlags(android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        leaMediaSession.setActive(true);

        // 🎧 Enregistrement du radar Bluetooth
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    // 🔄 VARIABLES DU MOTEUR DE RÉSILIENCE NOMADE
    private Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private boolean isReconnecting = false;
    private int reconnectDelay = 3000; // Backoff exponentiel : 3s, 6s, 12s… max 60s
    private android.content.BroadcastReceiver networkChangeReceiver;

    private void initWebSocket() {
        try {
            // 🌐 LE PONT SOUVERAIN NOMADE — LOCAL WIFI (priorité 1) ou CLOUDFLARE (priorité 2)
            String activeWsUrl = LeaNetworkConfig.getWebSocketUrl(LeaNovaService.this);
            if (activeWsUrl == null || activeWsUrl.isEmpty()) {
                Log.e("LeaNova", "❌ URL WebSocket null, retry in 5s");
                new Handler(Looper.getMainLooper()).postDelayed(this::initWebSocket, 5_000);
                return;
            }
            URI serverUri = new URI(activeWsUrl);
            Log.e("LeaNova", "🌐 Connexion WS → " + activeWsUrl);
            
            wsClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    String ctype = LeaNetworkDetector.getCachedType(LeaNovaService.this);
                    Log.e("LeaNova", "🌐 CONNECTÉ — type=" + ctype);
                    isReconnecting = false;
                    reconnectDelay = 3000; // Réinitialisation du backoff
                    LeaNetworkInterceptor.get(LeaNovaService.this).onConnectionSuccess(ctype, 0);
                    LeaNovaService.this.logWs("VOIX-WS", "CONNEXION", "✅ Connecté → " + activeWsUrl + " (" + ctype + ")");

                    // 🔐 Handshake d'identité immédiat dès la connexion
                    try {
                        SharedPreferences prefs = getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
                        String wsToken = prefs.getString("WS_BOSS_TOKEN", "");
                        JSONObject handshake = new JSONObject();
                        handshake.put("type", "AUTH_HANDSHAKE");
                        handshake.put("user", getBossIdentity());
                        if (wsToken != null && !wsToken.isEmpty()) handshake.put("token", wsToken);
                        send(handshake.toString());
                    } catch (Exception e) { Log.w("LeaNova", "⚠️ [AUTH_HANDSHAKE] " + e.getMessage()); }

                    // 📱 Envoi de la liste des apps installées pour que Léa puisse les ouvrir
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        () -> sendInstalledAppsToServer(), 1500
                    );
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject json = new JSONObject(message);
                        
                        // 🛡️ INTERCEPTION DES COMMANDES SYSTÈMES SOUVERAINES
                        if (json.has("type") && json.getString("type").equals("SYSTEM_ACTION")) {
                            String action = json.getString("action");
                            
                            if (action.equals("play_music")) {
                                String pkg = json.optString("package", "");
                                if (!pkg.isEmpty()) {
                                    // Essaie d'abord via MediaSessionManager pour éviter d'ouvrir l'app
                                    boolean playedViaSession = false;
                                    try {
                                        android.media.session.MediaSessionManager msm =
                                            (android.media.session.MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
                                        android.content.ComponentName notifComp =
                                            new android.content.ComponentName(LeaNovaService.this, LeaNotificationService.class);
                                        java.util.List<android.media.session.MediaController> sessions = msm.getActiveSessions(notifComp);
                                        for (android.media.session.MediaController mc : sessions) {
                                            if (pkg.equals(mc.getPackageName())) {
                                                mc.getTransportControls().play();
                                                playedViaSession = true;
                                                break;
                                            }
                                        }
                                    } catch (Exception ignored) {}

                                    if (!playedViaSession) {
                                        // Première fois ou app pas en arrière-plan : envoie PLAY key globale
                                        // (peut ouvrir l'app au 1er lancement, inévitable)
                                        android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                        if (am != null) {
                                            am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
                                            am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
                                        }
                                    }
                                }
                                pendingEndSession = true;
                                return;
                            }

                            if (action.equals("choose_music_app")) {
                                // Popup de sélection de l'app musicale préférée
                                showMusicAppChooser();
                                pendingEndSession = true;
                                return;
                            }

                            if (action.equals("open_app")) {
                                String pkg = json.getString("package");
                                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(launchIntent);
                                }
                                // L'audio de confirmation va jouer — quand il finit, triggerCooldown
                                // verra ce flag et terminera la session sans mode continu
                                pendingEndSession = true;
                                return; // ← skip le triggerCooldown final, l'audio s'en charge
                            } else if (action.equals("navigation")) {
                                String url = json.getString("url");
                                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                pendingEndSession = true;
                                return;
                            }
                            // 🎵 CONTRÔLE MULTIMÉDIA (Lecture / Pause / Suivant)
                            else if (action.equals("media_control")) {
                                String control = json.getString("control");
                                android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                int keyCode = -1;

                                if (control.equals("play_pause")) keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                                else if (control.equals("next")) keyCode = android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
                                else if (control.equals("previous")) keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;

                                if (keyCode != -1 && audioManager != null) {
                                    audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode));
                                    audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode));
                                }
                                // Même logique : fin de session après l'audio
                                pendingEndSession = true;
                                return;
                            }
                            // 🔍 RECHERCHE ET LECTURE D'UN TITRE PRÉCIS
                            else if (action.equals("media_search")) {
                                String query = json.getString("query");
                                Intent searchIntent = new Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                                searchIntent.putExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, android.provider.MediaStore.Audio.Media.ENTRY_CONTENT_TYPE);
                                searchIntent.putExtra(android.app.SearchManager.QUERY, query);
                                searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                if (searchIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(searchIntent);
                                } else {
                                    Intent fallback = getPackageManager().getLaunchIntentForPackage("com.sec.android.app.music");
                                    if (fallback != null) {
                                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(fallback);
                                    }
                                }
                                pendingEndSession = true;
                                return;
                            }
                            // 🖼️ AFFICHAGE D'UNE IMAGE GÉNÉRÉE (mode Bixby — écran verrouillé OK)
                            else if (action.equals("show_image")) {
                                String imageUrl = json.optString("url", "");
                                if (!imageUrl.isEmpty()) {
                                    Intent viewIntent = new Intent(LeaNovaService.this, LeaImageViewerActivity.class);
                                    viewIntent.putExtra(LeaImageViewerActivity.EXTRA_URL, imageUrl);
                                    viewIntent.putExtra("give_mic_on_close", true);
                                    // Android 12+ : démarrer depuis l'overlay (Activity visible) si dispo
                                    LeaNovaModeActivity overlay = LeaNovaModeActivity.instance;
                                    if (overlay != null && !overlay.isFinishing() && !overlay.isDestroyed()) {
                                        try { overlay.startActivity(viewIntent); } catch (Exception ignored) {}
                                    } else {
                                        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        try { startActivity(viewIntent); } catch (Exception ignored) {}
                                    }
                                }
                                return;
                            }
                            // 🎲 AFFICHAGE D'UN OBJET 3D GÉNÉRÉ
                            else if (action.equals("show_3d")) {
                                String modelUrl = json.optString("url", "");
                                if (!modelUrl.isEmpty()) {
                                    // Rend le micro seulement si Bixby était active au moment où la 3D se termine
                                    boolean giveBackMic = isActiveMode || isContinuousMode;
                                    Intent viewIntent = new Intent(LeaNovaService.this, LeaModelViewerActivity.class);
                                    viewIntent.putExtra(LeaModelViewerActivity.EXTRA_URL, modelUrl);
                                    viewIntent.putExtra("give_mic_on_close", giveBackMic);
                                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    try { startActivity(viewIntent); } catch (Exception ignored) {}
                                }
                                return;
                            }
                            // 📄 AFFICHAGE DU TEXTE GÉNÉRÉ (code, livre, paroles de musique)
                            else if (action.equals("show_text")) {
                                String content = json.optString("content", "");
                                String contentType = json.optString("contentType", "text");
                                logWs("VOIX", "SHOW-TEXT", "📄 show_text reçu — type=" + contentType + " len=" + content.length());
                                // Session terminée après TTS → évite que Vosk entende l'écho du TTS
                                if (contentType.equals("code") || contentType.equals("book") || contentType.equals("music")) {
                                    pendingEndSession = true;
                                }
                                if (!content.isEmpty()) {
                                    final String finalContent = content;
                                    final String finalType = contentType;
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        try {
                                            Intent viewIntent = new Intent(LeaNovaService.this, LeaTextViewerActivity.class);
                                            viewIntent.putExtra("content", finalContent);
                                            viewIntent.putExtra("contentType", finalType);
                                            viewIntent.putExtra("give_mic_on_close", true);
                                            // Android 12+ : un Service foreground ne peut pas démarrer d'Activity.
                                            // On démarre depuis l'overlay (Activity visible) si disponible.
                                            LeaNovaModeActivity overlay = LeaNovaModeActivity.instance;
                                            if (overlay != null && !overlay.isFinishing() && !overlay.isDestroyed()) {
                                                overlay.startActivity(viewIntent);
                                                updateSiriText("💻 Code prêt !");
                                                logWs("VOIX", "SHOW-TEXT", "✅ LeaTextViewerActivity lancée via overlay");
                                            } else {
                                                // Fallback : service context (Android ≤11 ou si overlay fermé)
                                                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(viewIntent);
                                                logWs("VOIX", "SHOW-TEXT", "✅ LeaTextViewerActivity lancée via service (fallback)");
                                            }
                                        } catch (Exception e) {
                                            logWs("VOIX", "SHOW-TEXT", "❌ startActivity échoué : " + e.getMessage());
                                        }
                                    });
                                }
                                return;
                            }
                            // ⏰ GESTION DES RÉVEILS ET MINUTEURS
                            else if (action.equals("set_alarm")) {
                                int hour = json.getInt("hour");
                                int minutes = json.getInt("minutes");
                                Intent alarmIntent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
                                alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour);
                                alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minutes);
                                alarmIntent.putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true);
                                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(alarmIntent);
                            }
                            else if (action.equals("set_timer")) {
                                int seconds = json.getInt("seconds");
                                Intent timerIntent = new Intent(android.provider.AlarmClock.ACTION_SET_TIMER);
                                timerIntent.putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds);
                                timerIntent.putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true);
                                timerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(timerIntent);
                            }

                            // 🔊 CONTRÔLE DU VOLUME SOUVERAIN
                            else if (action.equals("set_volume")) {
                                int level = json.getInt("level");
                                if (level < 0) level = 0;
                                if (level > 100) level = 100;
                                
                                android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                if (am != null) {
                                    // 1. On règle le volume de la musique
                                    int maxMusic = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                                    int targetMusic = (int) (maxMusic * (level / 100.0f));
                                    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetMusic, android.media.AudioManager.FLAG_SHOW_UI);
                                    
                                    // 2. On règle AUSSI le volume du canal Appel (car les écouteurs sont en mode SCO !)
                                    if (am.isBluetoothScoOn()) {
                                        int maxCall = am.getStreamMaxVolume(android.media.AudioManager.STREAM_VOICE_CALL);
                                        int targetCall = (int) (maxCall * (level / 100.0f));
                                        am.setStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL, targetCall, 0);
                                    }
                                }
                            }
                            else if (action.equals("volume_up")) {
                                android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                if (am != null) {
                                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI);
                                    if (am.isBluetoothScoOn()) am.adjustStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL, android.media.AudioManager.ADJUST_RAISE, 0);
                                }
                            }
                            else if (action.equals("volume_down")) {
                                android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                if (am != null) {
                                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI);
                                    if (am.isBluetoothScoOn()) am.adjustStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL, android.media.AudioManager.ADJUST_LOWER, 0);
                                }
                            }
                            
                            triggerCooldown(); // Libère instantanément le micro après l'action
                            return;
                        }

                        if (json.has("type") && json.getString("type").equals("CHAT")) {
                            JSONObject msg = json.getJSONObject("message");

                            // 📋 HISTORIQUE — sauvegarde question + réponse
                            String leaResponseText = msg.optString("originalText", "");
                            if (!currentUserQuestion.isEmpty() && !leaResponseText.isEmpty() && db != null) {
                                final String savedQ = currentUserQuestion;
                                final String savedR = leaResponseText;
                                currentUserQuestion = "";
                                new Thread(() -> db.saveConversation(savedQ, savedR, 0)).start();
                            }

                            if (msg.has("audioData") && !msg.isNull("audioData")) {
                                String audioBase64 = msg.getString("audioData");
                                LeaNovaService.this.logWs("VOIX", "AUDIO", "📥 Réponse audio reçue (" + audioBase64.length() + " chars)");
                                playAudioResponse(audioBase64);
                            } else {
                                LeaNovaService.this.logWs("VOIX", "AUDIO", "📥 Réponse sans audio — serveur a répondu mais pas de voix");
                                triggerCooldown();
                            }
                        }

                        // 🖼️ BIXBY_SPEAK : audio d'attente pendant génération image (sans déclencher cooldown)
                        String msgType = json.optString("type", "");
                        if ("BIXBY_SPEAK".equals(msgType)) {
                            String waitAudio = json.optString("audioData", null);
                            if (waitAudio != null && !waitAudio.isEmpty()) {
                                new Handler(Looper.getMainLooper()).post(() -> playAudioWait(waitAudio));
                            }
                            return;
                        }

                        // 🎙️ PREWARM_PHRASES : salut + réflexion pré-générés par le serveur
                        if ("PREWARM_PHRASES".equals(msgType)) {
                            cachedSalutAudio = json.optString("salut", null);
                            cachedReflexionAudio = json.optString("reflexion", null);
                            Log.e("LeaNova", "🎙️ Phrases Bixby pré-chargées");
                            return;
                        }

                        // 🔄 BIXBY_NOM_UPDATED : nom confirmé par le serveur, nouvelle phrase salut
                        if ("BIXBY_NOM_UPDATED".equals(msgType)) {
                            if (json.has("salut")) cachedSalutAudio = json.getString("salut");
                            if (json.has("confirmation")) {
                                final String confAudio = json.getString("confirmation");
                                new Handler(Looper.getMainLooper()).post(() ->
                                    playVoicePrompt(confAudio, () -> {
                                        isMicrophoneMuted = false;
                                        pendingVoskReset = true;
                                        updateSiriText("✨ LÉA est à l'écoute...");
                                        resetSilenceTimer();
                                    })
                                );
                            }
                            return;
                        }

                    } catch (Exception e) {
                        Log.e("LeaNova", "❌ Erreur déballage paquet WebSocket : " + e.getMessage());
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.e("LeaNova", "🔌 Tunnel rompu code=" + code + " — " + reason);
                    if (code == 4001) {
                        Log.e("LeaNova", "🚫 Identité rejetée par le serveur — pas de reconnexion automatique.");
                        return;
                    }
                    LeaNetworkInterceptor.get(LeaNovaService.this).onConnectionClosed(
                        LeaNetworkDetector.getCachedType(LeaNovaService.this), code, reason);
                    LeaNovaService.this.logWs("VOIX-WS", "CONNEXION", "🔌 Coupé code=" + code + " : " + reason);
                    triggerReconnection();
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e("LeaNova", "❌ Interférence réseau : " + ex.getMessage());
                    LeaNovaService.this.logWs("VOIX-WS", "CONNEXION", "❌ Erreur réseau : " + (ex != null ? ex.getMessage() : "?"));
                    LeaNetworkInterceptor.get(LeaNovaService.this).onConnectionError(
                        LeaNetworkDetector.getCachedType(LeaNovaService.this),
                        (newType, newUrl) -> new Handler(Looper.getMainLooper()).post(() -> initWebSocket()));
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            Log.e("LeaNova", "Erreur configuration WS: " + e.getMessage());
            triggerReconnection();
        }
    }

    // 🔄 MOTEUR DE RÉSILIENCE NOMADE (backoff exponentiel)
    private void triggerReconnection() {
        if (isReconnecting) return;
        isReconnecting = true;
        Log.e("LeaNova", "🔄 Reconnexion dans " + (reconnectDelay / 1000) + "s...");
        reconnectHandler.postDelayed(() -> {
            isReconnecting = false;
            reconnectDelay = Math.min(reconnectDelay * 2, 60000);
            initWebSocket();
        }, reconnectDelay);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LÉA active")
                .setContentText("Assistant en arrière-plan")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 🔓 On laisse juste le MICRO pour éviter que le S23 Ultra ne coupe l'appli en fond
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e("LeaNova", "❌ Erreur d'ancrage : " + e.getMessage());
        }
        return START_STICKY;
    }

    private void initVoskModel() {
        try {
            StorageService.unpack(this, "model-fr", "model",
                    (model) -> {
                        this.voskModel = model;
                        logWs("VOIX", "VOSK", "✅ Modèle Vosk prêt — écoute passive démarrée");
                        startPassiveListening();
                    },
                    (exception) -> Log.e("LeaNova", "❌ Échec Vosk : " + exception.getMessage())
            );
        } catch (Throwable e) {
            Log.e("LeaNova", "💥 Crash Vosk : " + e.getMessage());
        }
    }

    private synchronized void startPassiveListening() {
        if (isListening || voskModel == null) return;
        isListening = true;

        recordingThread = new Thread(() -> {
            // 🛡️ CORRECTION SOUVERAINE : Sécurisation de la mémoire du tampon
            int hardwareBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
            // On divise par 2 car on lit des "shorts" (2 octets) et non des "bytes"
            short[] buffer = new short[hardwareBufferSize / 2];
            
            Recognizer recognizer;
            try {
                recognizer = new Recognizer(voskModel, SAMPLE_RATE);
            } catch (java.io.IOException e) {
                isListening = false; return;
            }

            while (isListening) {
                // 🛑 1. ALLUMAGE IMMÉDIAT (Le tuyau est ouvert, le point vert reste fixe)
                if (audioRecord == null) {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Log.e("LeaNova", "❌ Permission RECORD_AUDIO manquante — micro inactif");
                        isListening = false;
                        break;
                    }
                    try {
                        // 🛡️ PARTAGE SOUVERAIN : Retour sur MIC pour permettre au clavier vocal d'utiliser le micro en même temps
                        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, hardwareBufferSize);
                        
                        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                            int audioSessionId = audioRecord.getAudioSessionId();
                            if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                                android.media.audiofx.AcousticEchoCanceler aec = android.media.audiofx.AcousticEchoCanceler.create(audioSessionId);
                                if (aec != null) aec.setEnabled(true);
                            }
                            if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                                android.media.audiofx.NoiseSuppressor ns = android.media.audiofx.NoiseSuppressor.create(audioSessionId);
                                if (ns != null) ns.setEnabled(true);
                            }
                        } else {
                            releaseAudioRecord(); 
                            try { Thread.sleep(1000); } catch (InterruptedException ie1) {} 
                            continue;
                        }
                        audioRecord.startRecording();
                    } catch (Exception mainException) {
                        releaseAudioRecord(); 
                        try { Thread.sleep(1000); } catch (InterruptedException ie2) {}
                        continue;
                    }
                }

                // 🛑 2. LECTURE CONTINUE (On vide le tampon à la vitesse de la lumière)
                AudioRecord ar = audioRecord; // référence locale thread-safe
                if (ar == null || !isListening) break;
                // Fix B v2.8.3 — Timeout sécurité : si le thread est interrompu (onDestroy), on sort proprement
                if (Thread.currentThread().isInterrupted()) break;
                int readSize = ar.read(buffer, 0, buffer.length);
                if (readSize < 0) {
                    Log.w("LeaNova", "⚠️ AudioRecord.read() retourne " + readSize + " — microphone défaillant ?");
                    break;
                }
                if (readSize == 0) continue;

                // Lecture unique des variables volatile par itération (évite la race condition double-lecture)
                final boolean micMuted = isMicrophoneMuted;
                final boolean voskReset = pendingVoskReset;

                // Si le micro est muté (LÉA parle), on vide l'eau du tuyau mais on ne l'analyse pas
                if (micMuted) { Thread.yield(); continue; }

                // Après une lecture audio de Léa : vider le buffer interne de Vosk avant de réécouter
                if (voskReset) {
                    pendingVoskReset = false;
                    if (recognizer != null) { try { recognizer.reset(); } catch (Exception ignored) {} }
                    continue;
                }

                // 🛑 3. LE CERVEAU VOSK
                boolean isFinal = recognizer.acceptWaveForm(buffer, readSize);
                String resultJson = isFinal ? recognizer.getResult() : recognizer.getPartialResult();
                String spokenText = extractTextFromJson(resultJson);

                if (spokenText.isEmpty()) continue;

                // ⏱️ DÉTECTION VOCALE : On remet les chronos à zéro à chaque son capté
                if (isActiveMode) {
                    resetSilenceTimer();
                    // En mode continu, repousser le timer d'extinction
                    if (isContinuousMode) startContinuousModeTimer();
                    // Track le partiel pour le commit de secours (si Vosk n'émet pas de final)
                    if (!isMicrophoneMuted) {
                        lastPartialText = spokenText;
                        if (!isFinal) schedulePartialCommit();
                    }
                }

                if (!isActiveMode) {
                    // MODE VIGILE
                    if (spokenText.contains("léa") || spokenText.contains("lea")) {
                        
                        // 🔐 C'EST ICI LA VRAIE LOGIQUE ! On vérifie si tu es connecté juste avant de réagir.
                        String currentBoss = getBossIdentity();
                        if (currentBoss.equals("invité")) {
                            Log.e("LeaNova", "⚠️ Refus d'activation : Aucun Boss connecté. LÉA ignore l'appel.");
                            LeaNovaService.this.logWs("VOIX", "RÉVEIL", "🚫 Réveil refusé — aucun utilisateur connecté");
                            recognizer.reset();
                            continue; // Elle t'a entendu, mais elle replonge dans le silence
                        }

                        // Si tu es connecté, elle s'active !
                        isActiveMode = true;
                        LeaNovaService.this.logWs("VOIX", "RÉVEIL", "🎤 Réveil par " + currentBoss + " — WS=" + (wsClient != null && wsClient.isOpen() ? "OK" : "COUPÉ"));
                        
                        // 🎧 SOUVERAINETÉ AUDIO ABSOLUE : On FORCE la mise en pause matérielle
                        if (audioManager == null) audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (audioManager != null) {
                            // 🔔 Sauvegarder le mode sonnerie AVANT de toucher à l'audio (Samsung peut le changer)
                            savedRingerMode = audioManager.getRingerMode();

                            // 🎵 INTELLIGENCE : On vérifie si la musique tournait AVANT de l'interrompre
                            wasMusicPlaying = audioManager.isMusicActive();

                            if (wasMusicPlaying) {
                                // 1. Coupure physique instantanée de la musique
                                audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
                                audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
                            }

                            // 2. Prise de contrôle du canal audio en mode EXCLUSIF
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                audioFocusRequest = new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                                        .build();
                                audioManager.requestAudioFocus((android.media.AudioFocusRequest) audioFocusRequest);
                            } else {
                                audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
                            }
                        }

                        showSiriLikeOverlay();
                        if (sessionMessageCount == 0 && cachedSalutAudio != null) {
                            isMicrophoneMuted = true;
                            final String salutSnapshot = cachedSalutAudio;
                            playVoicePrompt(salutSnapshot, () -> {
                                isMicrophoneMuted = false;
                                pendingVoskReset = true;
                                updateSiriText("✨ LÉA est à l'écoute...");
                                resetSilenceTimer();
                            });
                        } else {
                            updateSiriText("✨ LÉA est à l'écoute...");
                            playSystemSound(R.raw.eveil);
                            resetSilenceTimer();
                        }
                        recognizer.reset();
                    }
                } else {
                        // MODE TACTIQUE (INTERCEPTION)
                        if (isFinal) {
                            // Un final est arrivé — annuler le commit partiel de secours
                            partialCommitHandler.removeCallbacks(partialCommitRunnable);
                            lastPartialText = "";
                            // Correction des erreurs Vosk sur les termes techniques
                            spokenText = correctVoskText(spokenText);
                            Log.e("LeaNova", "🗣️ Ordre intercepté : " + spokenText);
                            LeaNovaService.this.logWs("VOIX", "VOSK", "🗣️ Final Vosk : \"" + spokenText + "\"");
                            stopSilenceTimer(); // ⏱️ ARRÊT DU CHRONO
                            
                            // 👁️ VISION : photo capturée, en attente de la question vocale
                            if (isVisionMode && pendingVisionPhoto != null) {
                                isVisionMode = false;
                                final String photo = pendingVisionPhoto;
                                pendingVisionPhoto = null;
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                currentUserQuestion = spokenText;
                                updateSiriText("👁️ Léa analyse...");
                                LeaNovaModeActivity.pushState(LeaNovaModeActivity.STATE_THINK);
                                final String visionQ = spokenText;
                                new Thread(() -> {
                                    try {
                                        if (wsClient != null && wsClient.isOpen()) {
                                            org.json.JSONObject msg = new org.json.JSONObject();
                                            msg.put("type", "VISION_IMAGE");
                                            msg.put("imageBase64", photo);
                                            msg.put("question", visionQ);
                                            msg.put("user", getBossIdentity());
                                            wsClient.send(msg.toString());
                                            logWs("VOIX", "VISION", "✅ Photo + question envoyées : " + visionQ);
                                        }
                                    } catch (Exception e) {
                                        logWs("VOIX", "VISION", "❌ Erreur envoi vision : " + e.getMessage());
                                    }
                                }).start();
                                recognizer.reset();
                                continue;
                            }

                            // 📱 LE SAS SMS
                            if (isSmsMode) {
                                if (smsStep == 1) {
                                    // 1. Recherche du contact
                                    smsRecipientName = spokenText;
                                    smsRecipientNumber = getContactNumber(smsRecipientName);
                                    if (smsRecipientNumber.isEmpty()) {
                                        updateSiriText("❌ Contact introuvable. À qui ?");
                                        playSystemSound(R.raw.relais);
                                        resetSilenceTimer();
                                    } else {
                                        smsStep = 2;
                                        updateSmsUi();
                                        updateSiriText("Que dois-je écrire ?");
                                        playSystemSound(R.raw.relais);
                                        resetSilenceTimer();
                                    }
                                } else if (smsStep == 2) {
                                    // 2. Dictée du message
                                    if (spokenText.equals("annuler") || spokenText.equals("annule")) {
                                        abortSmsMode();
                                        updateSiriText("✨ LÉA est à l'écoute...");
                                        playSystemSound(R.raw.reflexion);
                                        resetSilenceTimer();
                                    } else if (spokenText.equals("envoyer") || spokenText.equals("envoie")) {
                                        if (!smsRecipientNumber.isEmpty() && !smsMessageContent.isEmpty()) {
                                            LeaNovaService.this.logWs("VOIX", "SMS", "📤 SMS envoyé à " + smsRecipientName + " (" + smsRecipientNumber + ") par " + getBossIdentity() + " : \"" + smsMessageContent + "\"");
                                            sendNativeSms(smsRecipientNumber, smsMessageContent);
                                            abortSmsMode();
                                            updateSiriText("✅ Message envoyé.");
                                            playSystemSound(R.raw.reflexion);
                                            resetSilenceTimer();
                                        }
                                    } else if (spokenText.equals("recommencer") || spokenText.equals("effacer")) {
                                        smsMessageContent = "";
                                        updateSmsUi();
                                        updateSiriText("Je t'écoute.");
                                        playSystemSound(R.raw.relais);
                                        resetSilenceTimer();
                                    } else {
                                        // On concatène le texte naturellement
                                        smsMessageContent += (smsMessageContent.isEmpty() ? "" : " ") + spokenText;
                                        updateSmsUi();
                                        updateSiriText("Dire 'envoyer', 'recommencer' ou continuer.");
                                        playSystemSound(R.raw.relais);
                                        resetSilenceTimer();
                                    }
                                }
                                recognizer.reset();
                                continue; // 🛑 ON ABSORBE LE TEXTE, ON NE L'ENVOIE PAS À OLLAMA !
                            }
                            
                            // 🚀 LE DÉCLENCHEUR SMS
                            else if (spokenText.contains("envoie un message") || spokenText.contains("envoyer un message")) {
                                isSmsMode = true;
                                smsStep = 1;
                                LeaNovaService.this.logWs("VOIX", "SMS", "📱 Mode SMS activé par " + getBossIdentity());
                                showSmsOverlay();
                                updateSiriText("À qui dois-je l'envoyer ?");
                                playSystemSound(R.raw.relais);
                                resetSilenceTimer();
                                recognizer.reset();
                                continue; 
                            }
                            
                            // 📞 LA LOGIQUE DU SAS APPEL INTERACTIF
                            if (isCallMode) {
                                if (callStep == 1) {
                                    // Recherche de la personne
                                    if (spokenText.equals("annuler") || spokenText.equals("quitter")) {
                                        abortCallMode();
                                        updateSiriText("✨ LÉA est à l'écoute...");
                                        playSystemSound(R.raw.reflexion);
                                    } else {
                                        callRecipientName = spokenText;
                                        callRecipientNumber = getContactNumber(callRecipientName);
                                        if (callRecipientNumber.isEmpty()) {
                                            updateSiriText("❌ Contact introuvable. Qui dois-je appeler ?");
                                            playSystemSound(R.raw.relais);
                                        } else {
                                            callStep = 2;
                                            showCallOverlay();
                                            updateCallUi();
                                            updateSiriText("J'appelle " + callRecipientName + " ?");
                                            playSystemSound(R.raw.relais);
                                        }
                                    }
                                } else if (callStep == 2) {
                                    // Confirmation de tir
                                    if (spokenText.equals("oui") || spokenText.equals("ouais") || spokenText.equals("ok") || spokenText.equals("appelle") || spokenText.equals("appel") || spokenText.equals("vas-y") || spokenText.startsWith("oui ") || spokenText.endsWith(" oui") || spokenText.contains("oui") || spokenText.equals("go") || spokenText.equals("carrément") || spokenText.equals("affirmatif") || spokenText.equals("bien sûr") || spokenText.equals("c'est bon") || spokenText.equals("bonne idée")) {
                                        LeaNovaService.this.logWs("VOIX", "APPEL", "📞 Appel passé vers " + callRecipientName + " (" + callRecipientNumber + ") par " + getBossIdentity());
                                        makeNativeCall(callRecipientNumber);
                                        abortEverything();
                                    } else if (spokenText.equals("non") || spokenText.equals("nan")) {
                                        callStep = 1;
                                        abortCallMode(); 
                                        isCallMode = true; 
                                        updateSiriText("D'accord. Qui dois-je appeler ?");
                                        playSystemSound(R.raw.relais);
                                    } else if (spokenText.equals("annuler") || spokenText.equals("quitter")) {
                                        abortCallMode();
                                        updateSiriText("✨ LÉA est à l'écoute...");
                                        playSystemSound(R.raw.reflexion);
                                    } else {
                                        updateSiriText("Dire 'oui', 'non' ou 'quitter'.");
                                        playSystemSound(R.raw.relais);
                                    }
                                }
                                resetSilenceTimer();
                                recognizer.reset();
                                continue; 
                            }

                            // 🚀 LES DÉCLENCHEURS APPEL (Entrée dans le Sas)
                            else if (spokenText.equals("appelle") || spokenText.equals("appel") || spokenText.equals("téléphone") || spokenText.equals("passer un appel")) {
                                isCallMode = true;
                                callStep = 1;
                                LeaNovaService.this.logWs("VOIX", "APPEL", "📞 Mode appel activé par " + getBossIdentity());
                                updateSiriText("Qui dois-je appeler ?");
                                playSystemSound(R.raw.relais);
                                resetSilenceTimer();
                                recognizer.reset();
                                continue;
                            }
                            else if (spokenText.startsWith("appelle ") || spokenText.startsWith("téléphone à ")) {
                                String contactName = spokenText.replace("appelle ", "").replace("téléphone à ", "").trim();
                                String number = getContactNumber(contactName);
                                
                                if (!number.isEmpty()) {
                                    isCallMode = true;
                                    callStep = 2;
                                    callRecipientName = contactName;
                                    callRecipientNumber = number;
                                    showCallOverlay();
                                    updateCallUi();
                                    updateSiriText("J'appelle " + contactName + " ?");
                                } else {
                                    isCallMode = true;
                                    callStep = 1;
                                    updateSiriText("❌ Contact introuvable. Qui dois-je appeler ?");
                                }
                                playSystemSound(R.raw.relais);
                                resetSilenceTimer();
                                recognizer.reset();
                                continue;
                            }

                            // 🎙️ DÉTECTION "APPELLE-MOI [NOM] MAINTENANT" (changement du nom Bixby)
                            if ((spokenText.startsWith("appelle-moi ") || spokenText.startsWith("appelle moi ")) &&
                                    (spokenText.endsWith("maintenant") || spokenText.endsWith("désormais") || spokenText.endsWith("dorénavant") || spokenText.endsWith("à partir de maintenant"))) {
                                final String nom = extractBixbyNom(spokenText);
                                if (!nom.isEmpty()) {
                                    try {
                                        JSONObject updateCmd = new JSONObject();
                                        updateCmd.put("type", "UPDATE_BIXBY_NOM");
                                        updateCmd.put("nom", nom);
                                        if (wsClient != null && wsClient.isOpen()) wsClient.send(updateCmd.toString());
                                    } catch (Exception ex) {}
                                    isMicrophoneMuted = true;
                                    recognizer.reset();
                                    continue;
                                }
                            }

                            // 📱 LECTURE SMS + NOTIFICATIONS (WhatsApp, Instagram...)
                            // Vosk transcrit "lis mes messages" en : "lime aimé mes messages", "lit mais message", etc.
                            // → détection élargie sur "mes message(s)" + variantes de "lis/lit/lire"
                            boolean wantsReadMessages2 = spokenText.contains("lis mes notifications")
                                    || spokenText.contains("lis mes messages")
                                    || spokenText.contains("lire mes messages")
                                    || spokenText.contains("regarde mes messages")
                                    || spokenText.contains("nouveaux messages")
                                    || spokenText.contains("j'ai des messages")
                                    || spokenText.contains("qu'est-ce que j'ai reçu")
                                    || spokenText.contains("mes notifications")
                                    || spokenText.contains("mes messages")
                                    || spokenText.contains("mes message")
                                    || spokenText.contains("lire mes derniers")
                                    || spokenText.contains("lire mes dernier")
                                    || spokenText.contains("lis mes dernier")
                                    || (spokenText.contains("notification") && (spokenText.contains("lis") || spokenText.contains("lit") || spokenText.contains("lire") || spokenText.contains("montre") || spokenText.contains("donne") || spokenText.contains("mes") || spokenText.contains("mais") || spokenText.contains("lily")))
                                    || (spokenText.contains("message") && (spokenText.contains("lit") || spokenText.contains("lis") || spokenText.contains("lire") || spokenText.contains("lime") || spokenText.contains("lily")))
                                    || (spokenText.contains("notification") && spokenText.contains("lily"))
                                    || spokenText.contains("lily mes messages")
                                    || spokenText.contains("lily mes notifications");
                            if (wantsReadMessages2) {
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                currentUserQuestion = spokenText;
                                updateSiriText("📱 Lecture des messages...");
                                String digest = buildMessagesDigest();
                                final String msgCmd = "LIS_MESSAGES : " + digest;
                                if (cachedReflexionAudio != null) {
                                    playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(msgCmd));
                                } else {
                                    sendCommandToMaster(msgCmd);
                                }
                                recognizer.reset();
                                continue;
                            }

                            // 🔄 ROUTINES VOCALES — "lance ma routine matin"
                            boolean wantsRoutine = spokenText.contains("lance ma routine")
                                    || spokenText.contains("exécute ma routine")
                                    || spokenText.contains("démarre ma routine")
                                    || spokenText.contains("active ma routine")
                                    || spokenText.contains("lance la routine")
                                    || spokenText.contains("exécute la routine");
                            if (wantsRoutine && db != null) {
                                String routineName = spokenText
                                    .replaceAll("(?i)(lance|exécute|démarre|active)\\s+(ma|la)\\s+routine\\s*", "")
                                    .trim();
                                if (routineName.isEmpty()) routineName = "matin";
                                final String finalRoutineName = routineName;
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                updateSiriText("⚙️ Routine " + finalRoutineName + "...");
                                java.util.List<LeaNovaDataStore.Routine> routines = db.getAllRoutines();
                                boolean found = false;
                                for (LeaNovaDataStore.Routine r : routines) {
                                    if (r.name != null && r.name.toLowerCase().contains(finalRoutineName.toLowerCase())) {
                                        found = true;
                                        new LeaRoutineActionsExecutor(LeaNovaService.this).execute(r.actionsJson);
                                        sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'J\\'ai lancé ta routine " + r.name + ", mon chéri.' de façon naturelle.");
                                        break;
                                    }
                                }
                                if (!found) {
                                    sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'Je n\\'ai pas trouvé de routine appelée " + finalRoutineName + ".' de façon naturelle.");
                                }
                                recognizer.reset();
                                continue;
                            }

                            // ⏰ RAPPELS VOCAUX — "rappelle-moi dans 30 minutes de faire X"
                            boolean wantsRappel = spokenText.contains("rappelle-moi") || spokenText.contains("rappelle moi")
                                    || spokenText.contains("souviens-moi") || spokenText.contains("n'oublie pas de me rappeler");
                            if (wantsRappel) {
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                int delayMs = 0;
                                String rappelMsg = spokenText;
                                java.util.regex.Matcher mMin = java.util.regex.Pattern.compile("dans\\s+(\\d+)\\s*minutes?").matcher(spokenText);
                                java.util.regex.Matcher mHeure = java.util.regex.Pattern.compile("dans\\s+(\\d+)\\s*h(?:eures?)?").matcher(spokenText);
                                java.util.regex.Matcher mSec = java.util.regex.Pattern.compile("dans\\s+(\\d+)\\s*secondes?").matcher(spokenText);
                                if (mMin.find()) { delayMs = Integer.parseInt(mMin.group(1)) * 60 * 1000; rappelMsg = spokenText.replaceAll("(?i)rappelle[- ]moi\\s+dans\\s+\\d+\\s*minutes?\\s*(de|à|pour)?\\s*", "").trim(); }
                                else if (mHeure.find()) { delayMs = Integer.parseInt(mHeure.group(1)) * 60 * 60 * 1000; rappelMsg = spokenText.replaceAll("(?i)rappelle[- ]moi\\s+dans\\s+\\d+\\s*h(?:eures?)?\\s*(de|à|pour)?\\s*", "").trim(); }
                                else if (mSec.find()) { delayMs = Integer.parseInt(mSec.group(1)) * 1000; rappelMsg = spokenText.replaceAll("(?i)rappelle[- ]moi\\s+dans\\s+\\d+\\s*secondes?\\s*(de|à|pour)?\\s*", "").trim(); }
                                if (delayMs > 0) {
                                    final int finalDelay = delayMs;
                                    final String finalMsg = rappelMsg.isEmpty() ? "Rappel" : rappelMsg;
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            NotificationChannel ch = new NotificationChannel("lea_rappels", "Rappels Léa", NotificationManager.IMPORTANCE_HIGH);
                                            nm.createNotificationChannel(ch);
                                        }
                                        android.app.Notification notif = new NotificationCompat.Builder(LeaNovaService.this, "lea_rappels")
                                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                                            .setContentTitle("⏰ Rappel Léa")
                                            .setContentText(finalMsg)
                                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                                            .setAutoCancel(true)
                                            .build();
                                        nm.notify((int)(System.currentTimeMillis() % 10000), notif);
                                        updateSiriText("⏰ " + finalMsg);
                                    }, finalDelay);
                                    String delayStr = (delayMs >= 3600000) ? (delayMs/3600000) + " heure(s)" : (delayMs >= 60000) ? (delayMs/60000) + " minute(s)" : (delayMs/1000) + " seconde(s)";
                                    sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'D\\'accord, je te rappelle dans " + delayStr + " : " + rappelMsg + ".' de façon naturelle.");
                                } else {
                                    sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'Dis-moi dans combien de temps, par exemple dans 30 minutes.' de façon naturelle.");
                                }
                                recognizer.reset();
                                continue;
                            }

                            // 📅 AGENDA — "qu'est-ce que j'ai aujourd'hui / demain ?"
                            boolean wantsAgenda = spokenText.contains("qu'est-ce que j'ai aujourd'hui")
                                    || spokenText.contains("qu est ce que j'ai aujourd")
                                    || spokenText.contains("qu'est-ce que j'ai demain")
                                    || spokenText.contains("mon agenda")
                                    || spokenText.contains("mes rendez-vous")
                                    || spokenText.contains("mes rendez vous")
                                    || spokenText.contains("mes évènements")
                                    || spokenText.contains("mes evenements")
                                    || (spokenText.contains("agenda") && (spokenText.contains("aujourd") || spokenText.contains("demain") || spokenText.contains("semaine")))
                                    || (spokenText.contains("rendez") && (spokenText.contains("aujourd") || spokenText.contains("demain")));
                            if (wantsAgenda) {
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                updateSiriText("📅 Lecture de l'agenda...");
                                if (checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'Je n\\'ai pas accès à ton agenda. Va dans Paramètres > Applications > Accès spéciaux et autorise Léa.' de façon naturelle.");
                                } else {
                                    String agendaDigest = readTodayAgenda(spokenText.contains("demain"));
                                    final String agendaCmd = "AGENDA : " + agendaDigest;
                                    if (cachedReflexionAudio != null) {
                                        playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(agendaCmd));
                                    } else {
                                        sendCommandToMaster(agendaCmd);
                                    }
                                }
                                recognizer.reset();
                                continue;
                            }

                            // 💬 HANDLER RÉPONSE SMS
                            if (isReplyMode) {
                                if (spokenText.equals("annuler") || spokenText.equals("annule")) {
                                    isReplyMode = false;
                                    replyRecipientName = "";
                                    replyRecipientNumber = "";
                                    updateSiriText("✨ LÉA est à l'écoute...");
                                    playSystemSound(R.raw.reflexion);
                                    resetSilenceTimer();
                                    recognizer.reset();
                                    continue;
                                }
                                final String replyContent = spokenText;
                                final String replyTo = replyRecipientNumber;
                                final String replyName = replyRecipientName;
                                isReplyMode = false;
                                replyRecipientName = "";
                                replyRecipientNumber = "";
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sendNativeSms(replyTo, replyContent);
                                sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'J\\'ai répondu à " + replyName + " : " + replyContent + ".' de façon naturelle.");
                                recognizer.reset();
                                continue;
                            }

                            // 💬 DÉCLENCHEUR RÉPONSE SMS — "réponds à [contact]"
                            if (spokenText.startsWith("réponds à ") || spokenText.startsWith("répond à ")
                                    || spokenText.startsWith("réponds au message de ") || spokenText.startsWith("répond au message de ")) {
                                String contactName = spokenText
                                    .replaceAll("(?i)^réponds?\\s+(au\\s+message\\s+de|à)\\s+", "").trim();
                                if (!contactName.isEmpty()) {
                                    String number = getContactNumber(contactName);
                                    if (!number.isEmpty()) {
                                        isReplyMode = true;
                                        replyRecipientName = contactName;
                                        replyRecipientNumber = number;
                                        String lastMsg = getLastSmsFrom(number, contactName);
                                        String prompt = lastMsg.isEmpty()
                                            ? "Que veux-tu répondre à " + contactName + " ?"
                                            : contactName + " t'a dit : \"" + lastMsg + "\". Que veux-tu répondre ?";
                                        updateSiriText("💬 " + prompt);
                                        sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement '" + prompt + "' de façon naturelle.");
                                    } else {
                                        sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'Je n\\'ai pas trouvé " + contactName + " dans tes contacts.' de façon naturelle.");
                                    }
                                }
                                playSystemSound(R.raw.relais);
                                resetSilenceTimer();
                                recognizer.reset();
                                continue;
                            }

                            // 📝 PRISE DE NOTE — "note que..." / "enregistre que..."
                            boolean wantsNote = spokenText.startsWith("note que ")
                                    || spokenText.startsWith("note ça : ")
                                    || spokenText.startsWith("enregistre que ")
                                    || spokenText.startsWith("souviens-toi que ")
                                    || spokenText.startsWith("mémorise que ");
                            if (wantsNote) {
                                String noteContent = spokenText
                                    .replaceAll("(?i)^(note que|note ça :|enregistre que|souviens-toi que|mémorise que)\\s+", "").trim();
                                if (!noteContent.isEmpty()) {
                                    isMicrophoneMuted = true;
                                    stopSilenceTimer();
                                    android.content.SharedPreferences prefs = getSharedPreferences("lea_notes", Context.MODE_PRIVATE);
                                    String existing = prefs.getString("notes_list", "");
                                    String timestamp = new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.FRANCE).format(new java.util.Date());
                                    String newEntry = "[" + timestamp + "] " + noteContent;
                                    String updated = existing.isEmpty() ? newEntry : existing + "\n" + newEntry;
                                    prefs.edit().putString("notes_list", updated).apply();
                                    sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'Note enregistrée : " + noteContent + ".' de façon naturelle.");
                                    recognizer.reset();
                                    continue;
                                }
                            }

                            // 👤 INFOS CONTACT — "c'est qui ce numéro" / "infos sur [contact]"
                            boolean wantsContactInfo = spokenText.startsWith("c'est qui ")
                                    || spokenText.startsWith("c est qui ")
                                    || spokenText.startsWith("qui est ")
                                    || spokenText.startsWith("infos sur ");
                            if (wantsContactInfo) {
                                String query = spokenText
                                    .replaceAll("(?i)^(c'est qui|c est qui|qui est|infos sur)\\s+", "").trim();
                                if (!query.isEmpty()) {
                                    isMicrophoneMuted = true;
                                    stopSilenceTimer();
                                    String contactName = resolveContactName(query);
                                    String info = contactName.isEmpty() ? "Je ne connais pas ce contact." : "Ce contact s'appelle " + contactName + ".";
                                    sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement '" + info + "' de façon naturelle.");
                                    recognizer.reset();
                                    continue;
                                }
                            }

                            // 🖼️ SAS GÉNÉRATION IMAGE — étape 2 : description reçue
                            if (isImageMode) {
                                isImageMode = false;
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                final String imgDesc = spokenText;
                                updateSiriText("🖼️ Génération en cours...");
                                if (cachedReflexionAudio != null) {
                                    playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster("GÉNÉRER_IMAGE : " + imgDesc));
                                } else {
                                    playSystemSound(R.raw.reflexion);
                                    sendCommandToMaster("GÉNÉRER_IMAGE : " + spokenText);
                                }
                                recognizer.reset();
                                continue;
                            }
                            // Déclencheurs image
                            else if (spokenText.contains("génère une image") || spokenText.contains("génère une photo")
                                    || spokenText.contains("crée une image") || spokenText.contains("crée une photo")
                                    || spokenText.contains("fais une image") || spokenText.contains("fais une photo")
                                    || spokenText.contains("génère moi une image") || spokenText.contains("génère-moi une image")
                                    || spokenText.contains("génère moi une photo") || spokenText.contains("génère-moi une photo")) {
                                isImageMode = true;
                                logWs("VOIX", "IMAGE", "🖼️ Sas génération image activé");
                                updateSiriText("Décris ce que tu veux générer.");
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement cette phrase de façon naturelle : 'Dis-moi ce que tu veux que je génère. Décris le sujet, le style et l\\'ambiance.'");
                                recognizer.reset();
                                continue;
                            }

                            // 🎲 SAS GÉNÉRATION 3D — étape 2 : description reçue → micro libéré pendant génération
                            if (is3DMode) {
                                is3DMode = false;
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                final String desc3D = spokenText;
                                updateSiriText("🎲 Génération 3D lancée...");
                                if (cachedReflexionAudio != null) {
                                    playVoicePrompt(cachedReflexionAudio, () -> {
                                        sendCommandToMaster("GÉNÉRER_3D : " + desc3D);
                                        // Libère le micro : génération longue, l'utilisateur peut parler
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> triggerCooldown(), 600);
                                    });
                                } else {
                                    playSystemSound(R.raw.reflexion);
                                    sendCommandToMaster("GÉNÉRER_3D : " + desc3D);
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> triggerCooldown(), 600);
                                }
                                recognizer.reset();
                                continue;
                            }
                            // Déclencheurs 3D
                            else if (spokenText.contains("génère un objet 3d") || spokenText.contains("génère un modèle 3d")
                                    || spokenText.contains("crée un objet 3d") || spokenText.contains("crée un modèle 3d")
                                    || spokenText.contains("génère un objet en 3d") || spokenText.contains("génère un modèle en 3d")
                                    || spokenText.contains("objet 3d") || spokenText.contains("modèle 3d")) {
                                is3DMode = true;
                                logWs("VOIX", "3D", "🎲 Sas génération 3D activé");
                                updateSiriText("Décris l'objet 3D.");
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement cette phrase de façon naturelle : 'Décris-moi l\\'objet 3D que tu veux créer. Je vais lancer la génération et on pourra continuer à parler pendant ce temps.'");
                                recognizer.reset();
                                continue;
                            }

                            // 🎵 SAS GÉNÉRATION MUSIQUE — étape 2 : style reçu
                            if (isMusicMode && musicStep == 1) {
                                musicStyle = spokenText;
                                musicStep = 2;
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement cette phrase de façon naturelle : 'Super ! Maintenant décris-moi la structure : combien de couplets, un refrain, un drop, un outro... c\\'est toi qui décides.'");
                                recognizer.reset();
                                continue;
                            }
                            // 🎵 SAS GÉNÉRATION MUSIQUE — étape 3 : structure reçue → générer
                            else if (isMusicMode && musicStep == 2) {
                                musicStructure = spokenText;
                                isMusicMode = false;
                                musicStep = 0;
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                final String musicStyle2 = musicStyle;
                                final String musicStructure2 = musicStructure;
                                final String musicPrompt = "GÉNÉRER_MUSIQUE : Style : " + musicStyle2 + ". Structure : " + musicStructure2 + ".";
                                currentUserQuestion = "Génère une chanson - Style : " + musicStyle2;
                                updateSiriText("🎵 Génération en cours...");
                                if (cachedReflexionAudio != null) {
                                    playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(musicPrompt));
                                } else {
                                    playSystemSound(R.raw.reflexion);
                                    sendCommandToMaster(musicPrompt);
                                }
                                recognizer.reset();
                                continue;
                            }
                            // Déclencheurs musique
                            else if (spokenText.contains("génère une chanson") || spokenText.contains("crée une chanson")
                                    || spokenText.contains("compose une chanson") || spokenText.contains("génère de la musique")
                                    || spokenText.contains("crée de la musique") || spokenText.contains("écris une chanson")
                                    || spokenText.contains("génère un titre") || spokenText.contains("crée un titre musicale")
                                    || spokenText.contains("fais une chanson") || spokenText.contains("fais de la musique")) {
                                isMusicMode = true;
                                musicStep = 1;
                                musicStyle = "";
                                musicStructure = "";
                                logWs("VOIX", "MUSIQUE", "🎵 Sas génération musique activé");
                                updateSiriText("🎵 Quel style de musique ?");
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement cette phrase de façon naturelle : 'Quel style de musique tu veux ? Pop, hip-hop, électro, rock, jazz... c\\'est toi qui choisis.'");
                                recognizer.reset();
                                continue;
                            }

                            // 💻 GÉNÉRATION DE CODE / PROMPT / TEXTE — envoi direct avec préfixe popup
                            boolean wantsCode = spokenText.contains("prompt de musique")
                                    || spokenText.contains("prompt musical")
                                    || spokenText.contains("promt de musique")
                                    || spokenText.contains("promt musical")
                                    || spokenText.contains("génère un prompt")
                                    || spokenText.contains("génère-moi un prompt")
                                    || spokenText.contains("génère moi un prompt")
                                    || spokenText.contains("génère un promt")
                                    || spokenText.contains("génère des paroles")
                                    || spokenText.contains("écris des paroles")
                                    || spokenText.contains("génère du code")
                                    || spokenText.contains("génère un code")
                                    || spokenText.contains("génère moi un code")
                                    || spokenText.contains("génère-moi un code")
                                    || spokenText.contains("générer un code")
                                    || spokenText.contains("me générer un code")
                                    || spokenText.contains("peux me générer un code")
                                    || spokenText.contains("peux générer un code")
                                    || spokenText.contains("écris du code")
                                    || spokenText.contains("écris un code")
                                    || spokenText.contains("génère un script")
                                    || spokenText.contains("écris un script")
                                    || spokenText.contains("code python") || spokenText.contains("code java")
                                    || spokenText.contains("code javascript") || spokenText.contains("code html")
                                    || spokenText.contains("code css") || spokenText.contains("page html")
                                    || spokenText.contains("page web") || spokenText.contains("une page web")
                                    || spokenText.contains("écris une fonction")
                                    || spokenText.contains("génère une fonction") || spokenText.contains("génère un programme")
                                    || spokenText.contains("écris un programme") || spokenText.contains("code en")
                                    || (spokenText.contains("code") && (spokenText.contains("faire") || spokenText.contains("page") || spokenText.contains("bouton") || spokenText.contains("créer") || spokenText.contains("créé")));
                            if (wantsCode) {
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                final String codeCmd = "GÉNÉRER_CODE : " + spokenText;
                                currentUserQuestion = spokenText;
                                updateSiriText("💻 Génération de code...");
                                if (cachedReflexionAudio != null) {
                                    playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(codeCmd));
                                } else {
                                    playSystemSound(R.raw.reflexion);
                                    sendCommandToMaster(codeCmd);
                                }
                                recognizer.reset();
                                continue;
                            }

                            // 📚 GÉNÉRATION DE LIVRE / TEXTE LONG — envoi direct avec préfixe
                            boolean wantsBook = spokenText.contains("écris un livre")
                                    || spokenText.contains("génère un livre")
                                    || spokenText.contains("rédige un texte")
                                    || spokenText.contains("génère un texte long")
                                    || spokenText.contains("écris un article")
                                    || spokenText.contains("génère un article")
                                    || spokenText.contains("rédige un article")
                                    || spokenText.contains("écris une histoire")
                                    || spokenText.contains("génère une histoire")
                                    || spokenText.contains("rédige un livre");
                            if (wantsBook) {
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                sessionMessageCount++;
                                final String bookCmd = "GÉNÉRER_LIVRE : " + spokenText;
                                currentUserQuestion = spokenText;
                                updateSiriText("📚 Génération en cours...");
                                if (cachedReflexionAudio != null) {
                                    playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(bookCmd));
                                } else {
                                    playSystemSound(R.raw.reflexion);
                                    sendCommandToMaster(bookCmd);
                                }
                                recognizer.reset();
                                continue;
                            }

                            // 👁️ VISION CAMÉRA TEMPS RÉEL
                            boolean wantsVision = spokenText.contains("qu'est-ce que tu vois")
                                    || spokenText.contains("qu est ce que tu vois")
                                    || spokenText.contains("décris ce que tu vois")
                                    || spokenText.contains("décris l'image")
                                    || spokenText.contains("analyse cette image")
                                    || spokenText.contains("analyse ce que tu vois")
                                    || spokenText.contains("regarde devant toi")
                                    || spokenText.contains("qu'est-ce qu'il y a devant")
                                    || spokenText.contains("montre moi ce que tu vois")
                                    || spokenText.contains("lis ce texte")
                                    || spokenText.contains("traduis ce que tu vois")
                                    || spokenText.contains("identifie ce plat")
                                    || spokenText.contains("qu'est-ce que c'est");
                            if (wantsVision) {
                                isMicrophoneMuted = true;
                                stopSilenceTimer();
                                currentUserQuestion = spokenText;
                                updateSiriText("📷 Ouverture de la caméra...");
                                final String visionQuestion = spokenText;
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    LeaNovaModeActivity overlay = LeaNovaModeActivity.instance;
                                    if (overlay != null && !overlay.isFinishing() && !overlay.isDestroyed()) {
                                        overlay.launchCamera(visionQuestion);
                                    } else {
                                        logWs("VOIX", "VISION", "⚠️ Overlay non disponible pour vision");
                                        sendCommandToMaster("INSTRUCTION SYSTÈME ABSOLUE : Dis uniquement 'Tu dois ouvrir l\\'overlay Léa pour utiliser la vision.' de façon naturelle.");
                                    }
                                });
                                recognizer.reset();
                                continue;
                            }

                            // 🧠 MODE OLLAMA CLASSIQUE (Si ce n'est ni un SMS ni un appel ni un changement de nom)
                            LeaNovaService.this.logWs("VOIX", "COMMANDE", "📤 Commande finale : \"" + spokenText + "\" — WS=" + (wsClient != null && wsClient.isOpen() ? "OK" : "COUPÉ"));
                            isMicrophoneMuted = true;
                            stopSilenceTimer(); // Suspend le chrono pendant le traitement Ollama
                            sessionMessageCount++;
                            final String commandToSend = spokenText;
                            currentUserQuestion = spokenText;
                            if (cachedReflexionAudio != null) {
                                updateSiriText("⚙️ LÉA réfléchit...");
                                playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(commandToSend));
                            } else {
                                updateSiriText("⚙️ LÉA réfléchit...");
                                playSystemSound(R.raw.reflexion);
                                sendCommandToMaster(spokenText);
                            }
                            recognizer.reset();
                        }
                    }
                } // 🛡️ L'accolade vitale qui ferme la boucle while(isListening)
                
                recognizer.close();
                releaseAudioRecord();
            }, "LeaVoskThread");
            
            recordingThread.start();
        }

    // ── Log via WebSocket (garanti d'arriver : le WS marche déjà) ────────────────
    // 🎵 Lance MusicChooserActivity (Activity transparente) pour choisir l'app musicale
    private void showMusicAppChooser() {
        Intent intent = new Intent(this, MusicChooserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // 🎵 Appelé par MusicChooserActivity quand l'utilisateur a choisi une app
    public void onMusicAppChosen(String pkg, String appName) {
        if (wsClient == null || !wsClient.isOpen()) return;
        try {
            org.json.JSONObject msg = new org.json.JSONObject();
            msg.put("type", "SET_MUSIC_APP");
            msg.put("package", pkg);
            msg.put("appName", appName);
            wsClient.send(msg.toString());
        } catch (Exception ignored) {}
    }

    // 📱 Envoie la liste des apps installées et lançables au serveur
    private void sendInstalledAppsToServer() {
        if (wsClient == null || !wsClient.isOpen()) return;
        new Thread(() -> {
            try {
                android.content.pm.PackageManager pm = getPackageManager();
                java.util.List<android.content.pm.ApplicationInfo> allApps =
                    pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA);
                org.json.JSONArray appArray = new org.json.JSONArray();
                for (android.content.pm.ApplicationInfo info : allApps) {
                    // On ne garde que les apps avec une icône de lancement (apps utilisateur)
                    if (pm.getLaunchIntentForPackage(info.packageName) == null) continue;
                    String label = pm.getApplicationLabel(info).toString();
                    org.json.JSONObject app = new org.json.JSONObject();
                    app.put("name", label.toLowerCase());
                    app.put("package", info.packageName);
                    appArray.put(app);
                }
                org.json.JSONObject msg = new org.json.JSONObject();
                msg.put("type", "APP_LIST");
                msg.put("apps", appArray);
                wsClient.send(msg.toString());
                Log.e("LeaNova", "📱 APP_LIST envoyée — " + appArray.length() + " apps");
            } catch (Exception e) {
                Log.e("LeaNova", "❌ Erreur APP_LIST : " + e.getMessage());
            }
        }).start();
    }

    private void logWs(String level, String tag, String msg) {
        try {
            JSONObject log = new JSONObject();
            log.put("type", "LOG");
            log.put("level", level);
            log.put("tag", tag);
            log.put("msg", msg);
            log.put("user", getBossIdentity());
            log.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE);
            log.put("ts", System.currentTimeMillis());
            if (wsClient != null && wsClient.isOpen()) {
                try { wsClient.send(log.toString()); } catch (Exception ignored) {}
            } else {
                // WS pas encore prêt : buffer local
                LeaAndroidLogger.voice(this, tag, msg);
            }
        } catch (Exception e) {
            Log.e("LeaNova", "logWs failed: " + e.getMessage());
        }
    }

    // Si Vosk n'émet pas de résultat final dans 5s, on envoie quand même le partiel
    private void schedulePartialCommit() {
        partialCommitHandler.removeCallbacks(partialCommitRunnable);
        partialCommitRunnable = () -> {
            String pending = lastPartialText;
            if (pending.isEmpty() || !isActiveMode || isMicrophoneMuted) return;
            Log.e("LeaNova", "⏱️ Commit partiel 5s : " + pending);
            logWs("VOIX", "COMMANDE", "📤 Commit partiel (5s sans silence) : \"" + pending + "\"");
            lastPartialText = "";
            isMicrophoneMuted = true;
            stopSilenceTimer(); // Suspend le chrono pendant le traitement Ollama
            sessionMessageCount++;
            final String pendingCmd = pending;
            if (cachedReflexionAudio != null) {
                updateSiriText("⚙️ LÉA réfléchit...");
                playVoicePrompt(cachedReflexionAudio, () -> sendCommandToMaster(pendingCmd));
            } else {
                updateSiriText("⚙️ LÉA réfléchit...");
                playSystemSound(R.raw.reflexion);
                sendCommandToMaster(pending);
            }
        };
        partialCommitHandler.postDelayed(partialCommitRunnable, 5000);
    }

    private void playAudioResponse(String base64Audio) {
        isMicrophoneMuted = true; // Garantie absolue : micro coupé pendant toute la lecture
        MediaPlayer localPlayer = null;
        try {
            updateSiriText("🔊 LÉA parle...");
            if (mediaPlayer != null) {
                try { mediaPlayer.stop(); } catch (Exception ignored) {}
                mediaPlayer.release();
                mediaPlayer = null;
            }
            // Décodage du Base64 envoyé par le Zorin OS
            String cleanBase64 = base64Audio;
            if (base64Audio.contains(",")) { cleanBase64 = base64Audio.split(",")[1]; }

            byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
            File tempAudio = File.createTempFile("lea_temp_voice", ".mp3", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempAudio)) {
                fos.write(decodedBytes);
            }

            localPlayer = new MediaPlayer();
            mediaPlayer = localPlayer;

            // Voix de Léa via le canal médias (A2DP pour les écouteurs BT, haut-parleur sinon).
            // Pas de SCO : on ne veut pas couper la musique, juste la mettre en pause via AudioFocus.
            localPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                .build());

            localPlayer.setDataSource(tempAudio.getAbsolutePath());
            localPlayer.prepare();
            localPlayer.start();

            // Dès qu'elle a fini de parler, on déclenche le sas (seulement si session encore active)
            localPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                tempAudio.delete();
                if (!isActiveMode) return; // Session avortée entre-temps — ne pas rouvrir
                triggerCooldown();
            });
            localPlayer.setOnErrorListener((mp, what, extra) -> {
                logWs("VOIX", "AUDIO", "❌ Erreur lecture réponse what=" + what + " extra=" + extra);
                try { mp.release(); } catch (Exception ignored) {}
                mediaPlayer = null;
                tempAudio.delete();
                if (!isActiveMode) return true;
                triggerCooldown(); // Débloquer le micro même en cas d'erreur
                return true;
            });

        } catch (Exception e) {
            Log.e("LeaNova", "❌ Erreur de lecture audio : " + e.getMessage());
            if (localPlayer != null) {
                try { localPlayer.release(); } catch (Exception ignored) {}
                localPlayer = null;
                mediaPlayer = null;
            }
            if (isActiveMode) triggerCooldown();
        }
    }

    /** Appelé par LeaImageViewerActivity / LeaModelViewerActivity à leur fermeture. */
    public void triggerCooldownFromActivity() {
        new Handler(Looper.getMainLooper()).post(this::triggerCooldown);
    }

    /** Appelé par LeaNovaModeActivity après capture photo : active le micro pour la question verbale. */
    public void activateForVisionQuestion(String photoBase64) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (LeaNovaService.instance == null) return; // Service détruit
            pendingVisionPhoto = photoBase64;
            isVisionMode = true;
            isMicrophoneMuted = false;
            isActiveMode = true;
            updateSiriText("🎤 Pose ta question...");
            LeaNovaModeActivity.pushState(LeaNovaModeActivity.STATE_LISTEN);
            resetSilenceTimer();
        });
    }

    // 🖼️ Audio d'attente pendant génération image — joue sans déclencher triggerCooldown
    private void playAudioWait(String base64Audio) {
        isMicrophoneMuted = true;
        stopSilenceTimer();
        stopContinuousModeTimer();
        // Orbe en mode SPEAK pendant la lecture vocale
        LeaVoiceActivity.pushState(LeaVoiceActivity.STATE_SPEAK);
        LeaNovaModeActivity.pushState(LeaNovaModeActivity.STATE_SPEAK);
        try {
            if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
            String cleanBase64 = base64Audio.contains(",") ? base64Audio.split(",")[1] : base64Audio;
            byte[] decoded = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
            File tmp = File.createTempFile("lea_wait_", ".mp3", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(decoded);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                .build());
            mediaPlayer.setDataSource(tmp.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            // Après l'audio, orbe en THINK (génération en cours) — PAS de triggerCooldown
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release(); mediaPlayer = null; tmp.delete();
                LeaVoiceActivity.pushState(LeaVoiceActivity.STATE_THINK);
                LeaNovaModeActivity.pushState(LeaNovaModeActivity.STATE_THINK);
            });
            mediaPlayer.setOnErrorListener((mp, w, e) -> {
                try { mp.release(); } catch (Exception ignored) {}
                mediaPlayer = null; tmp.delete();
                LeaVoiceActivity.pushState(LeaVoiceActivity.STATE_THINK);
                LeaNovaModeActivity.pushState(LeaNovaModeActivity.STATE_THINK);
                return true;
            });
        } catch (Exception e) {
            LeaVoiceActivity.pushState(LeaVoiceActivity.STATE_THINK);
            LeaNovaModeActivity.pushState(LeaNovaModeActivity.STATE_THINK);
        }
    }

    // ⏱️ FONCTIONS DU CHRONOMÈTRE DE SILENCE
    private void resetSilenceTimer() {
        silenceHandler.removeCallbacks(silenceRunnable);
        silenceRunnable = () -> {
            Log.e("LeaNova", "⏱️ 10 secondes de silence absolu. Extinction automatique.");
            abortEverything();
        };
        silenceHandler.postDelayed(silenceRunnable, SILENCE_TIMEOUT_MS);
    }

    private void stopSilenceTimer() {
        if (silenceRunnable != null) {
            silenceHandler.removeCallbacks(silenceRunnable);
        }
    }

    private void triggerCooldown() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pendingVoskReset = true;
            isMicrophoneMuted = false;
            if (pendingEndSession) {
                // Ouverture d'app : on termine la session, pas de mode continu
                pendingEndSession = false;
                abortEverything();
                return;
            }
            // Si la session a été interrompue manuellement (overlay fermé), on n'active pas le mode continu
            if (!isActiveMode) return;
            // Mode continu : Léa reste active 45s sans re-trigger
            isContinuousMode = true;
            isActiveMode = true;
            updateSiriText("✨ LÉA t'écoute encore...");
            playSystemSound(R.raw.relais);
            resetSilenceTimer();
            startContinuousModeTimer();
            Log.e("LeaNova", "🟢 Sas terminé — mode continu actif 45s.");
        }, 2500);
    }

    private void startContinuousModeTimer() {
        if (continuousModeRunnable != null) {
            continuousModeHandler.removeCallbacks(continuousModeRunnable);
        }
        continuousModeRunnable = () -> {
            if (isContinuousMode && !isMicrophoneMuted) {
                logWs("VOIX", "CONTINU", "⏱️ 45s sans activité — fin du mode continu");
                isContinuousMode = false;
                abortEverything();
            }
        };
        continuousModeHandler.postDelayed(continuousModeRunnable, CONTINUOUS_MODE_DURATION_MS);
    }

    private void stopContinuousModeTimer() {
        if (continuousModeRunnable != null) continuousModeHandler.removeCallbacks(continuousModeRunnable);
        isContinuousMode = false;
    }

    public void abortEverything() {
        // KILL-SWITCH ABSOLU TACTILE
        Log.e("LeaNova", "🛑 KILL SWITCH ACTIVÉ !");
        stopSilenceTimer(); // ⏱️ COUPURE DU CHRONO
        stopContinuousModeTimer(); // ⏱️ COUPURE DU MODE CONTINU
        partialCommitHandler.removeCallbacks(partialCommitRunnable); // ⏱️ ANNULER COMMIT PARTIEL
        lastPartialText = "";
        pendingEndSession = false; // 🔄 Réinitialiser le flag de fin de session
        abortSmsMode(); // 📱 FERMETURE DU SAS SMS
        abortCallMode(); // 📞 FERMETURE DU SAS APPEL
        isReplyMode = false; // 💬 FERMETURE DU SAS RÉPONSE SMS
        replyRecipientName = "";
        replyRecipientNumber = "";
        isImageMode = false; // 🖼️ Annuler sas image
        is3DMode = false;    // 🎲 Annuler sas 3D
        isMusicMode = false; musicStep = 0; // 🎵 Annuler sas musique
        currentUserQuestion = ""; // 📋 Effacer la question en cours
        
        // 🎧 RELANCE DE LA MUSIQUE : Uniquement si elle tournait avant l'appel de LÉA !
        if (audioManager != null) {
            if (wasMusicPlaying) {
                audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
                audioManager.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
                wasMusicPlaying = false; // Réinitialisation de la mémoire
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest((android.media.AudioFocusRequest) audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }

            // 🔔 Restaurer le mode sonnerie d'origine (Samsung peut l'avoir changé pendant le focus audio)
            if (savedRingerMode >= 0) {
                try { audioManager.setRingerMode(savedRingerMode); } catch (Exception ignored) {}
                savedRingerMode = -1;
            }
        }
        
        // 1. On ferme le pop-up
        hideSiriLikeOverlay();
        
        // 2. On coupe les lecteurs audio (réponse + prompt pré-généré)
        if (promptPlayer != null) {
            try { if (promptPlayer.isPlaying()) promptPlayer.stop(); promptPlayer.release(); } catch (Exception ignored) {}
            promptPlayer = null;
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
        
        // 3. On envoie l'ordre de tuer Ollama au Zorin OS
        try {
            if (wsClient != null && wsClient.isOpen()) {
                JSONObject stopCmd = new JSONObject();
                stopCmd.put("type", "STOP_GENERATION");
                wsClient.send(stopCmd.toString());
            }
        } catch (Exception e) { Log.w("LeaNova", "⚠️ abortEverything STOP_GENERATION : " + e.getMessage()); }

        // 4. On réinitialise l'écoute
        isActiveMode = false;
        isMicrophoneMuted = false;
        sessionMessageCount = 0; // La prochaine activation rejouera le salut
    }

    private String getBossIdentity() {
        // On lit le nom de la session Capacitor enregistrée dans le S23 Ultra
        SharedPreferences prefs = getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
        String savedName = prefs.getString("lea_session_user", "invité");
        // On nettoie les guillemets que Capacitor ajoute parfois en JSON
        return savedName.replace("\"", ""); 
    }

    @SuppressLint("MissingPermission")
    private void sendCommandToMaster(String command) {
        if (wsClient == null || !wsClient.isOpen()) {
            logWs("VOIX", "COMMANDE", "⚠️ WS coupé au moment d'envoyer \"" + command + "\" — reconnexion");
            initWebSocket(); return;
        }

        double lat = 0.0; double lon = 0.0;
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) { lat = loc.getLatitude(); lon = loc.getLongitude(); }
            }
        } catch (Exception e) { Log.w("LeaNova", "⚠️ GPS location error : " + e.getMessage()); }

        try {
            String bossIdentity = getBossIdentity(); // Remplacement dynamique !

            JSONObject payload = new JSONObject();
            // 🛡️ ALIGNEMENT SYSTÈME : Unification avec la clé attendue par le serveur
            payload.put("user", bossIdentity);
            payload.put("action", "voice_command");
            payload.put("text", command);

            JSONObject gps = new JSONObject();
            gps.put("lat", lat); gps.put("lon", lon);
            payload.put("gps", gps);

            try {
                if (wsClient != null && wsClient.isOpen()) {
                    wsClient.send(payload.toString());
                }
            } catch (Exception e) {
                Log.e("LeaNova", "❌ sendCommandToMaster échoué : " + e.getMessage());
            }
        } catch (Exception e) { Log.w("LeaNova", "⚠️ sendCommandToMaster payload error : " + e.getMessage()); }
    }

    private String extractTextFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("text")) return obj.getString("text").toLowerCase().trim();
            if (obj.has("partial")) return obj.getString("partial").toLowerCase().trim();
        } catch (Exception e) { Log.w("LeaNova", "⚠️ [extractTextFromJson] " + e.getMessage()); }
        return "";
    }

    private String correctVoskText(String text) {
        // HTML : "h t m elle", "h t m el", "acheter et melle", "hache-t-m-elle"
        text = text.replaceAll("(?i)\\bh[\\s.\\-]*t[\\s.\\-]*m[\\s.\\-]*[eè][\\s.\\-]*l+e?\\b", "html");
        text = text.replaceAll("(?i)\\bacheter\\s+(et\\s+)?m[eèê]l+[eé]?s?\\b", "html");
        text = text.replaceAll("(?i)\\bhache[\\s.\\-]?t[\\s.\\-]?m[\\s.\\-]?elle?\\b", "html");
        // JavaScript : "java script"
        text = text.replaceAll("(?i)\\bjava[\\s.\\-]+script\\b", "javascript");
        // TypeScript : "type script"
        text = text.replaceAll("(?i)\\btype[\\s.\\-]+script\\b", "typescript");
        // Python : "p y t h o n", "piton", "piston"
        text = text.replaceAll("(?i)\\bp[\\s.\\-]*y[\\s.\\-]*t[\\s.\\-]*h[\\s.\\-]*o[\\s.\\-]*n\\b", "python");
        text = text.replaceAll("(?i)\\bpiton\\b", "python");
        text = text.replaceAll("(?i)\\bpiston\\b", "python");
        // CSS : "c s s"
        text = text.replaceAll("(?i)\\bc[\\s.\\-]*s[\\s.\\-]*s\\b", "css");
        // PHP : "p h p"
        text = text.replaceAll("(?i)\\bp[\\s.\\-]*h[\\s.\\-]*p\\b", "php");
        // SQL : "s q l", "sequel"
        text = text.replaceAll("(?i)\\bs[\\s.\\-]*q[\\s.\\-]*l\\b", "sql");
        text = text.replaceAll("(?i)\\bsequel\\b", "sql");
        // API : "a p i"
        text = text.replaceAll("(?i)\\ba[\\s.\\-]*p[\\s.\\-]*i\\b", "api");
        return text;
    }

    private synchronized void releaseAudioRecord() {
        if (audioRecord != null) {
            try { if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) audioRecord.stop(); } catch (Exception e) {}
            try { audioRecord.release(); } catch (Exception e) {}
            audioRecord = null;
        }
    }

    private void showSiriLikeOverlay() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(this, LeaNovaModeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void showRoutineButton() {
        if (routineFloatingBtn != null) return;
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        float dp = getResources().getDisplayMetrics().density;

        android.widget.Button btn = new android.widget.Button(this);
        btn.setText("📅 Routines");
        btn.setTextSize(11f);
        btn.setTextColor(Color.parseColor("#00E5FF"));
        btn.setBackgroundColor(Color.parseColor("#CC011627"));
        android.graphics.drawable.GradientDrawable bd = new android.graphics.drawable.GradientDrawable();
        bd.setColor(Color.parseColor("#CC011627"));
        bd.setCornerRadius(dp * 20);
        bd.setStroke((int) dp, Color.parseColor("#00E5FF"));
        btn.setBackground(bd);
        btn.setOnClickListener(v -> {
            Intent i = new Intent(this, LeaRoutinesActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        routineFloatingBtn = btn;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            (int)(150 * dp), (int)(40 * dp),
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        params.y = (int)(110 * dp);  // juste au-dessus de la barre de vague
        windowManager.addView(routineFloatingBtn, params);
    }

    private void hideRoutineButton() {
        if (routineFloatingBtn != null && windowManager != null) {
            try { windowManager.removeView(routineFloatingBtn); } catch (Exception ignored) {}
            routineFloatingBtn = null;
        }
    }

    private void updateSiriText(String newText) {
        int orbState;
        if (newText.contains("réfléchit") || newText.contains("Réfléchit")
                || newText.contains("génér") || newText.contains("Génér")
                || newText.contains("attend") || newText.contains("Attend")
                || newText.contains("⏳")) {
            orbState = LeaVoiceActivity.STATE_THINK;
        } else if (newText.contains("parle") || newText.contains("Parle")) {
            orbState = LeaVoiceActivity.STATE_SPEAK;
        } else {
            orbState = LeaVoiceActivity.STATE_LISTEN;
        }
        LeaVoiceActivity.pushState(orbState);
        LeaNovaModeActivity.pushState(orbState);
    }

    // Joue un audio base64 sans déclencher le cooldown complet ; appelle onComplete quand terminé
    private void playVoicePrompt(String base64Audio, Runnable onComplete) {
        new Handler(Looper.getMainLooper()).post(() -> {
            // Arrêter tout prompt en cours avant d'en lancer un nouveau
            if (promptPlayer != null) {
                try { promptPlayer.stop(); promptPlayer.release(); } catch (Exception ignored) {}
                promptPlayer = null;
            }
            try {
                String cleanBase64 = base64Audio;
                if (base64Audio.contains(",")) cleanBase64 = base64Audio.split(",")[1];
                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                File tempAudio = File.createTempFile("lea_prompt", ".mp3", getCacheDir());
                try (FileOutputStream fos = new FileOutputStream(tempAudio)) {
                    fos.write(decodedBytes);
                }
                MediaPlayer mp = new MediaPlayer();
                promptPlayer = mp;
                mp.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                    .build());
                mp.setDataSource(tempAudio.getAbsolutePath());
                mp.prepare();
                mp.setOnCompletionListener(m -> {
                    try {
                        promptPlayer = null;
                        m.release();
                        tempAudio.delete();
                        pendingVoskReset = true;
                        // Garde : si l'utilisateur a aborté entre-temps, ne pas continuer
                        if (!isActiveMode) {
                            logWs("VOIX", "PROMPT", "⚠️ prompt terminé après abort — callback ignoré");
                            return;
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (onComplete != null) onComplete.run();
                        });
                    } catch (Exception e) {
                        if (onComplete != null) { try { onComplete.run(); } catch (Exception ignored) {} }
                    }
                });
                mp.setOnErrorListener((m, what, extra) -> {
                    try {
                        promptPlayer = null;
                        logWs("VOIX", "PROMPT", "❌ Erreur MediaPlayer prompt what=" + what + " extra=" + extra);
                        try { m.release(); } catch (Exception ignored) {}
                        tempAudio.delete();
                        // Débloquer la session même en cas d'erreur audio
                        if (isActiveMode && onComplete != null) {
                            new Handler(Looper.getMainLooper()).post(onComplete);
                        }
                    } catch (Exception e) {
                        if (onComplete != null) { try { onComplete.run(); } catch (Exception ignored) {} }
                    }
                    return true;
                });
                mp.start();
                logWs("VOIX", "PROMPT", "▶️ Lecture prompt en cours");
            } catch (Exception e) {
                promptPlayer = null;
                logWs("VOIX", "PROMPT", "❌ playVoicePrompt exception: " + e.getMessage());
                pendingVoskReset = true;
                if (isActiveMode && onComplete != null) onComplete.run();
            }
        });
    }

    // Extrait le nom entre "appelle-moi " et le mot de fin (maintenant/désormais/dorénavant)
    private String extractBixbyNom(String text) {
        String[] prefixes = {"appelle-moi ", "appelle moi "};
        String[] suffixes = {" à partir de maintenant", " maintenant", " désormais", " dorénavant"};
        String s = text;
        for (String p : prefixes) { if (s.startsWith(p)) { s = s.substring(p.length()); break; } }
        for (String suf : suffixes) { if (s.endsWith(suf)) { s = s.substring(0, s.length() - suf.length()); break; } }
        return s.trim();
    }

    private void hideSiriLikeOverlay() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent kill1 = new Intent(this, LeaVoiceActivity.class);
            kill1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            kill1.putExtra("ACTION_KILL", true);
            startActivity(kill1);

            Intent kill2 = new Intent(this, LeaNovaModeActivity.class);
            kill2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            kill2.putExtra("ACTION_KILL", true);
            startActivity(kill2);

            hideRoutineButton();
        });
    }

    @Override
    public synchronized void onDestroy() {
        instance = null;
        isListening = false;
        // Fix #2 — RecordingThread join() avant de libérer les ressources
        if (recordingThread != null) {
            recordingThread.interrupt();
            try { recordingThread.join(2000); } catch (InterruptedException ignored) {}
            recordingThread = null;
        }
        releaseAudioRecord();
        hideSiriLikeOverlay();
        // Fix #1 — MediaPlayer release complet avec null check
        if (mediaPlayer != null) { try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {} mediaPlayer = null; }
        if (promptPlayer != null) { try { if (promptPlayer.isPlaying()) promptPlayer.stop(); promptPlayer.release(); } catch (Exception ignored) {} promptPlayer = null; }
        if (wsClient != null) wsClient.close();
        // Fix #5 — MediaSession release avec setActive(false)
        if (leaMediaSession != null) { leaMediaSession.setActive(false); leaMediaSession.release(); leaMediaSession = null; }

        hideRoutineButton();
        // Fix #4 — Bluetooth receiver unregister (déjà présent, maintenu)
        try { unregisterReceiver(bluetoothReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(networkChangeReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // L'app est fermée (swipe recents) → relancer le service immédiatement
        android.app.PendingIntent restartIntent = android.app.PendingIntent.getService(
            this, 1,
            new Intent(this, LeaNovaService.class),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );
        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.set(android.app.AlarmManager.ELAPSED_REALTIME, 1000, restartIntent);
        super.onTaskRemoved(rootIntent);
    }

    // ==========================================
    // 📱 MODULE SMS SOUVERAIN (INTERFACE & LOGIQUE)
    // ==========================================
    
    @SuppressLint("Range")
    private String getContactNumber(String name) {
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        String number = "";
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                    new String[]{"%" + name + "%"}, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (idx >= 0) number = cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e("LeaNova", "getContactNumber error: " + e.getMessage());
        } finally {
            if (cursor != null) { try { cursor.close(); } catch (Exception ignored) {} }
        }
        return number;
    }

    private void sendNativeSms(String number, String message) {
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            // 1. Envoi physique via l'antenne
            android.telephony.SmsManager smsManager = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? getSystemService(android.telephony.SmsManager.class)
                : android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);
            Log.e("LeaNova", "✅ SMS Envoyé à " + number);
            
            // 2. 📝 SAUVEGARDE SOUVERAINE DANS L'HISTORIQUE ANDROID
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("address", number);
            values.put("body", message);
            values.put("type", "2"); // Le code "2" signifie "Message Envoyé"
            values.put("date", System.currentTimeMillis());
            getContentResolver().insert(android.net.Uri.parse("content://sms/sent"), values);
            
        } catch (Exception e) {
            Log.e("LeaNova", "❌ Erreur Envoi/Sauvegarde SMS: " + e.getMessage());
        }
    }

    // 📱 LECTURE SMS + NOTIFICATIONS (WhatsApp etc.)
    private String buildMessagesDigest() {
        StringBuilder sb = new StringBuilder();
        // SMS natifs
        String sms = readLastSmsMessages(5);
        if (!sms.isEmpty()) sb.append("SMS récents :\n").append(sms);
        // Notifications (WhatsApp, etc.)
        java.util.List<String> notifs = LeaNotificationService.getRecentNotifications(5);
        if (!notifs.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Notifications récentes :\n");
            for (String n : notifs) sb.append("- ").append(n).append("\n");
        }
        if (sb.length() == 0) return "Aucun message récent trouvé.";
        return sb.toString();
    }

    private String readTodayAgenda(boolean tomorrow) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            if (tomorrow) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            long endOfDay = cal.getTimeInMillis();

            String[] projection = { "title", "dtstart", "dtend", "description", "eventLocation" };
            String selection = "dtstart >= ? AND dtstart <= ? AND deleted = 0";
            String[] selArgs = { String.valueOf(startOfDay), String.valueOf(endOfDay) };
            android.database.Cursor cursor = null;
            StringBuilder sb = new StringBuilder();
            try {
                cursor = getContentResolver().query(
                    android.provider.CalendarContract.Events.CONTENT_URI,
                    projection, selection, selArgs, "dtstart ASC");
                if (cursor != null && cursor.getCount() > 0) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE);
                    while (cursor.moveToNext()) {
                        String title = cursor.getString(0);
                        long dtStart = cursor.getLong(1);
                        long dtEnd = cursor.getLong(2);
                        String loc = cursor.getString(4);
                        sb.append("- ").append(sdf.format(new java.util.Date(dtStart)));
                        sb.append(" → ").append(sdf.format(new java.util.Date(dtEnd)));
                        sb.append(" : ").append(title != null ? title : "Sans titre");
                        if (loc != null && !loc.isEmpty()) sb.append(" (").append(loc).append(")");
                        sb.append("\n");
                    }
                } else {
                    sb.append("Aucun événement ").append(tomorrow ? "demain" : "aujourd'hui").append(".");
                }
            } finally {
                if (cursor != null) { try { cursor.close(); } catch (Exception ignored) {} }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Impossible de lire l'agenda : " + e.getMessage();
        }
    }

    private String getLastSmsFrom(String number, String name) {
        if (checkSelfPermission(android.Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return "";
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                android.net.Uri.parse("content://sms/inbox"),
                new String[]{"body", "address", "date"},
                "address = ?", new String[]{ number },
                "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            // silent
        } finally {
            if (cursor != null) { try { cursor.close(); } catch (Exception ignored) {} }
        }
        return "";
    }

    @android.annotation.SuppressLint("Range")
    private String readLastSmsMessages(int count) {
        if (checkSelfPermission(android.Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        android.database.Cursor cursor = null;
        try {
            android.content.ContentResolver cr = getContentResolver();
            cursor = cr.query(
                android.net.Uri.parse("content://sms/inbox"),
                new String[]{"address", "body", "date"},
                null, null, "date DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                int i = 0;
                do {
                    String address = cursor.getString(0);
                    String body = cursor.getString(1);
                    String name = resolveContactName(address);
                    sb.append("- De ").append(name).append(" : ").append(body).append("\n");
                    i++;
                } while (cursor.moveToNext() && i < count);
            }
        } catch (Exception e) {
            Log.e("LeaNova", "❌ Lecture SMS : " + e.getMessage());
        } finally {
            if (cursor != null) { try { cursor.close(); } catch (Exception ignored) {} }
        }
        return sb.toString();
    }

    private String resolveContactName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return "Inconnu";
        android.database.Cursor cursor = null;
        try {
            android.net.Uri uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            );
            cursor = getContentResolver().query(uri,
                new String[]{android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e("LeaNova", "❌ Résolution contact : " + e.getMessage());
        } finally {
            if (cursor != null) { try { cursor.close(); } catch (Exception ignored) {} }
        }
        return phoneNumber;
    }

    // 📞 FONCTION D'APPEL SOUVERAINE
    private void makeNativeCall(String number) {
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(android.net.Uri.parse("tel:" + number));
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);
                logWs("VOIX", "APPEL", "📞 Appel direct lancé vers " + number);
            } else {
                // Permission manquante → ouvre le composeur avec le numéro pré-rempli
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(android.net.Uri.parse("tel:" + number));
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialIntent);
                logWs("VOIX", "APPEL", "⚠️ Permission CALL_PHONE manquante — composeur ouvert pour " + number + " (appuie sur le bouton vert)");
            }
        } catch (Exception e) {
            logWs("VOIX", "APPEL", "❌ Échec appel vers " + number + " : " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void showSmsOverlay() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (smsContainer != null) return;
            if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            smsContainer = new FrameLayout(this);
            
            // La carte principale (Fond sombre, bordure cyan)
            android.widget.LinearLayout card = new android.widget.LinearLayout(this);
            card.setOrientation(android.widget.LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E6000814")); 
            bg.setCornerRadius(30f); 
            bg.setStroke(4, Color.parseColor("#00E5FF")); 
            card.setBackground(bg);
            card.setPadding(40, 40, 40, 40);
            
            TextView title = new TextView(this);
            title.setText("📱 PROTOCOLE SMS");
            title.setTextColor(Color.parseColor("#00E5FF"));
            title.setTextSize(18f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            card.addView(title);
            
            smsRecipientView = new TextView(this);
            smsRecipientView.setTextColor(Color.WHITE);
            smsRecipientView.setTextSize(16f);
            smsRecipientView.setPadding(0, 20, 0, 10);
            smsRecipientView.setText("À : ...");
            card.addView(smsRecipientView);
            
            smsMessageView = new TextView(this);
            smsMessageView.setTextColor(Color.LTGRAY);
            smsMessageView.setTextSize(15f);
            smsMessageView.setPadding(0, 10, 0, 30);
            smsMessageView.setText("Message : ...");
            card.addView(smsMessageView);
            
            // Ligne de boutons
            android.widget.LinearLayout buttonRow = new android.widget.LinearLayout(this);
            buttonRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            buttonRow.setGravity(Gravity.CENTER);
            
            // Bouton Annuler (❌)
            android.widget.Button btnCancel = new android.widget.Button(this);
            btnCancel.setText("❌");
            btnCancel.setBackgroundColor(Color.TRANSPARENT);
            btnCancel.setOnClickListener(v -> {
                abortSmsMode();
                updateSiriText("✨ LÉA est à l'écoute...");
            });
            buttonRow.addView(btnCancel);
            
            // Bouton Recommencer (🔄)
            android.widget.Button btnRestart = new android.widget.Button(this);
            btnRestart.setText("🔄");
            btnRestart.setBackgroundColor(Color.TRANSPARENT);
            btnRestart.setOnClickListener(v -> {
                smsMessageContent = "";
                updateSmsUi();
            });
            buttonRow.addView(btnRestart);
            
            // Bouton Envoyer (✅)
            android.widget.Button btnSend = new android.widget.Button(this);
            btnSend.setText("✅");
            btnSend.setBackgroundColor(Color.TRANSPARENT);
            btnSend.setOnClickListener(v -> {
                if (!smsRecipientNumber.isEmpty() && !smsMessageContent.isEmpty()) {
                    sendNativeSms(smsRecipientNumber, smsMessageContent);
                    updateSiriText("✅ Message envoyé !");
                    abortSmsMode();
                }
            });
            buttonRow.addView(btnSend);
            
            card.addView(buttonRow);

            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            );
            cardParams.gravity = Gravity.CENTER;
            cardParams.leftMargin = 50;
            cardParams.rightMargin = 50;
            smsContainer.addView(card, cardParams);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
            );

            windowManager.addView(smsContainer, params);
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateSmsUi() {
        new Handler(Looper.getMainLooper()).post(() -> {
            
            // 🕵️‍♂️ CRYPTAGE DU NUMÉRO (Format: 061***78)
            String displayNum = smsRecipientNumber;
            if (displayNum != null && displayNum.replaceAll("\\s+", "").length() >= 8) {
                String cleanNum = displayNum.replaceAll("\\s+", "");
                int len = cleanNum.length();
                displayNum = cleanNum.substring(0, 4) + "***" + cleanNum.substring(len - 2);
            }

            if (smsRecipientView != null) {
                smsRecipientView.setText("À : " + (smsRecipientName.isEmpty() ? "..." : smsRecipientName + " (" + displayNum + ")"));
            }
            if (smsMessageView != null) {
                smsMessageView.setText("Message : " + (smsMessageContent.isEmpty() ? "..." : smsMessageContent));
            }
        });
    }

    private void abortSmsMode() {
        isSmsMode = false;
        smsStep = 0;
        smsRecipientName = "";
        smsRecipientNumber = "";
        smsMessageContent = "";
        new Handler(Looper.getMainLooper()).post(() -> {
            if (smsContainer != null && windowManager != null) {
                windowManager.removeView(smsContainer);
                smsContainer = null;
            }
        });
    }

    // ==========================================
    // 📞 MODULE APPEL SOUVERAIN (INTERFACE & LOGIQUE)
    // ==========================================
    
    @SuppressLint("SetTextI18n")
    private void showCallOverlay() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callContainer != null) return;
            if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            callContainer = new FrameLayout(this);
            
            // ⬛ Le Ruban (Horizontal, bas, sombre, bordure cyan)
            android.widget.LinearLayout card = new android.widget.LinearLayout(this);
            card.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E6000814"));
            bg.setCornerRadius(30f);
            bg.setStroke(4, Color.parseColor("#00E5FF"));
            card.setBackground(bg);
            card.setPadding(40, 20, 20, 20); 
            
            // 📝 Texte (Nom + Numéro)
            callRecipientView = new TextView(this);
            callRecipientView.setTextColor(Color.WHITE);
            callRecipientView.setTextSize(16f);
            callRecipientView.setTypeface(null, android.graphics.Typeface.BOLD);
            android.widget.LinearLayout.LayoutParams textParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            card.addView(callRecipientView, textParams);
            
            // 📞 Bouton d'Appel Unique
            android.widget.Button btnCall = new android.widget.Button(this);
            btnCall.setText("📞");
            btnCall.setTextSize(22f);
            btnCall.setBackgroundColor(Color.TRANSPARENT);
            btnCall.setPadding(10, 0, 10, 0);
            btnCall.setOnClickListener(v -> {
                if (!callRecipientNumber.isEmpty()) {
                    makeNativeCall(callRecipientNumber);
                    abortEverything(); // On ferme tout et on appelle
                }
            });
            card.addView(btnCall);

            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            );
            cardParams.gravity = Gravity.CENTER;
            cardParams.leftMargin = 50;
            cardParams.rightMargin = 50;
            callContainer.addView(card, cardParams);

            // 🎯 Positionnement : En bas, juste au-dessus de la bulle LÉA
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.BOTTOM;
            params.y = 400; // Ajustement millimétré de la hauteur (en pixels au-dessus du bas)

            windowManager.addView(callContainer, params);
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateCallUi() {
        new Handler(Looper.getMainLooper()).post(() -> {
            // 🕵️‍♂️ CRYPTAGE DU NUMÉRO
            String displayNum = callRecipientNumber;
            if (displayNum != null && displayNum.replaceAll("\\s+", "").length() >= 8) {
                String cleanNum = displayNum.replaceAll("\\s+", "");
                int len = cleanNum.length();
                displayNum = cleanNum.substring(0, 4) + "***" + cleanNum.substring(len - 2);
            }
            if (callRecipientView != null) {
                callRecipientView.setText(callRecipientName + " (" + displayNum + ")");
            }
        });
    }

    private void abortCallMode() {
        isCallMode = false;
        callStep = 0;
        callRecipientName = "";
        callRecipientNumber = "";
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callContainer != null && windowManager != null) {
                windowManager.removeView(callContainer);
                callContainer = null;
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "LÉA Bixby Service", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // 🎵 Sons système de Léa (bip d'éveil, relais, réflexion)
    private void playSystemSound(int soundResId) {
        if (audioManager == null) audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                    .build());
                android.content.res.AssetFileDescriptor afd = getResources().openRawResourceFd(soundResId);
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.prepare();
                mp.start();
            } catch (Exception e) {
                Log.e("LeaNova", "❌ Erreur son système : " + e.getMessage());
            }
        });
    }
}