package com.flolov42.lea_v3.ui;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.agents.*;
import com.flolov42.lea_v3.modes.*;
import com.flolov42.lea_v3.update.UpdateCheckService;
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


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;


public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Enregistrement de notre plugin de communication
        registerPlugin(LeaPhonePlugin.class);
        
        super.onCreate(savedInstanceState);
        
        // DÉVERROUILLAGE SOUVERAIN : Autorise Léa à parler et à jouer du son sans blocage
        WebView webView = this.bridge.getWebView();
        if (webView != null) {
            WebSettings settings = webView.getSettings();
            settings.setMediaPlaybackRequiresUserGesture(false);
        }

        // Status bar transparente — le contenu passe derrière
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);

        // Appel immédiat après attachement du décor view
        getWindow().getDecorView().post(this::setImmersiveMode);
        // Second appel 500 ms plus tard pour couvrir l'init async du WebView Capacitor
        getWindow().getDecorView().postDelayed(this::setImmersiveMode, 500);

        // 👁️ VÉRIFICATION GLOBALE (OVERLAY + TOUTES LES PERMISSIONS S20 ULTRA)
        checkAllPermissionsAndStart();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setImmersiveMode();
    }

    @SuppressWarnings("deprecation")
    private void setImmersiveMode() {
        // FLAG_TRANSLUCENT_STATUS/NAVIGATION bloquent hide() sur Android 11+ — on les purge avant tout
        getWindow().clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController ctrl = getWindow().getInsetsController();
            if (ctrl != null) {
                ctrl.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                ctrl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

private void checkAllPermissionsAndStart() {
        // 1. ÉTAPE 1 : Vérification de l'Overlay (La bulle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("✅ Permission nécessaire — Superposition")
                .setMessage(
                    "Android affiche un avertissement de sécurité sur la page suivante — c'est normal.\n\n" +
                    "LÉA a besoin de cette permission pour :\n" +
                    "• Afficher l'assistant vocal par-dessus les autres apps\n" +
                    "• Afficher les appels entrants sur l'écran de verrouillage\n" +
                    "• Afficher les commandes rapides Bixby\n\n" +
                    "⚠️ Sans cette permission, le mode vocal Bixby sera complètement désactivé.\n" +
                    "Tu pourras l'activer plus tard en accordant la permission dans les réglages.\n\n" +
                    "LÉA n'accède jamais à tes données bancaires.\n" +
                    "Sur la page suivante, appuie sur le bouton pour autoriser."
                )
                .setPositiveButton("Continuer vers les réglages", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 4242);
                })
                .setNegativeButton("Désactiver Bixby", (d, w) -> {
                    // Sans overlay : le mode vocal Bixby est désactivé. L'app fonctionne mais sans écoute vocale.
                    proceedAfterOverlayCheck();
                })
                .setCancelable(false)
                .show();
            return;
        }

        // 2. ÉTAPE 2 : Vérification de l'accès total aux fichiers (Pour "Mes Fichiers" / Musique)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 4343);
            return;
        }

        // 3. ÉTAPE 3 : Collecte des permissions d'exécution (Runtime Permissions)
        List<String> missingPerms = new ArrayList<>();
        
        // 📍 AJOUT SOUVERAIN : Le GPS est maintenant demandé dès l'ouverture
        String[] basePerms = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        };

        for (String perm : basePerms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(perm);
            }
        }

        // Stockage classique pour les anciennes versions d'Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Notifications Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        }

        // Bluetooth Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPerms.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        // Envoi de la rafale de pop-ups s'il en manque
        if (!missingPerms.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPerms.toArray(new String[0]), 112);
        } else {
            // Tout est blindé et validé, lancement de tous les services !
            startAllServices();
        }
    } // 👑 L'ACCOLADE SOUVERAINE QUI FERMAIT MAL EST ICI !

    // Appelé quand l'utilisateur choisit "Plus tard" sur la demande d'overlay
    // → saute directement à l'étape 2 pour ne pas boucler
    private void proceedAfterOverlayCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 4343);
            return;
        }
        List<String> missingPerms = new ArrayList<>();
        String[] basePerms = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        };
        for (String perm : basePerms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(perm);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.POST_NOTIFICATIONS);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.READ_MEDIA_AUDIO);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.READ_MEDIA_VIDEO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                missingPerms.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!missingPerms.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPerms.toArray(new String[0]), 112);
        } else {
            startAllServices();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4242 || requestCode == 4343) {
            // Re-vérification après le retour des fenêtres de paramètres systèmes
            checkAllPermissionsAndStart();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 112) {
            // Lancement des services quoi qu'il arrive pour ne pas bloquer l'application
            startAllServices();
        }
    }

    private void startAllServices() {
        // 1. Pré-initialise toutes les databases SQLite (cache warm-up)
        LeaAgentDatabase.get(this);
        LeaPlusDatabase.get(this);
        LeaModeDatabase.get(this);
        LeaRoutineDatabase.get(this);
        LeaNetworkDatabase.get(this);
        // Probe réseau immédiat — met à jour le cache avant que LeaNovaService démarre
        LeaNetworkDetector.probeAsync(this);

        // 2. Init notification managers — crée tous les channels Android 8+
        LeaAgentNotificationManager.get(this);
        LeaPlusNotifications.get(this);
        LeaModeNotifications.get(this);

        // 3. Démarre LeaNovaService uniquement si la permission overlay est accordée
        //    Sans elle, la bulle vocale ne peut pas s'afficher → le mode Bixby reste désactivé.
        //    Si l'utilisateur accorde la permission plus tard, le service démarrera automatiquement.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            Intent bixbyIntent = new Intent(this, LeaNovaService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(bixbyIntent);
            } else {
                startService(bixbyIntent);
            }
        }

        // 4. Démarre LeaAgentService (foreground — 11 agents + modes + LÉA PLUS)
        LeaAgentManager.get(this).ensureServiceRunning();

        // 5. Démarre LeaRoutineConditionDetector (GPS + BT + heure)
        startService(new Intent(this, LeaRoutineConditionDetector.class));

        // 6. Planifie les tâches WorkManager périodiques (survie aux kills)
        LeaWorkScheduler.schedule(this);

        // 7. Vérifie les mises à jour (une fois par jour, en arrière-plan)
        UpdateCheckService.checkOncePerDay(this);
    }
}
