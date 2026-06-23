package com.flolov42.lea_v3.telephony;

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


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class LeaCallService extends InCallService {

    // 👑 LE PONT MÉMOIRE SOUVERAIN
    public static LeaCallService instance;

    // 🧠 currentCall : conservé pour compatibilité avec ton code existant.
    // Il pointe désormais sur "l'appel au premier plan" (le dernier pertinent).
    public Call currentCall;
    private MediaPlayer sonnerie;
    private Vibrator vibreur;

    // 📋 NOUVEAU : la liste de TOUS les appels en cours (pour le double appel / conférence)
    public static final List<Call> activeCalls = new ArrayList<>();

    // 🧠 CACHE D'ÉTAT SOUVERAIN
    public static String cachedState = "none";
    public static String cachedNumber = "";
    public static String cachedName = "Inconnu";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        instance = this;

        // On ajoute à la liste sans écraser bêtement l'ancien
        if (!activeCalls.contains(call)) {
            activeCalls.add(call);
        }

        // Détecte s'il y avait DÉJÀ un appel en cours avant celui-ci (= double appel)
        boolean ilYAvaitDejaUnAppel = activeCalls.size() > 1;

        currentCall = call;

        Log.e("LeaCallService", "📞 [LÉA PROTECT] Appel intercepté ! Total: " + activeCalls.size());

        String number = "Inconnu";
        if (call.getDetails() != null && call.getDetails().getHandle() != null) {
            String raw = call.getDetails().getHandle().getSchemeSpecificPart();
            if (raw != null && !raw.isEmpty()) number = raw;
        }
        final String callerNumber = number;
        final String callerName = getContactName(callerNumber);

        // 🛡️ LE BOUCLIER LÉA PROTECT (inchangé)
        android.content.SharedPreferences prefs = getSharedPreferences("LeaProtect", android.content.Context.MODE_PRIVATE);
        java.util.Set<String> blockedSet = prefs.getStringSet("blocked_numbers", new java.util.HashSet<>());
        String callerNorm = normaliserNumero(callerNumber);
        boolean estBloque = false;
        for (String bloque : blockedSet) {
            if (normaliserNumero(bloque).equals(callerNorm)) {
                estBloque = true;
                break;
            }
        }
        if (estBloque) {
            android.util.Log.e("LeaCallService", "🛑 [LÉA PROTECT] Numéro bloqué exécuté : " + callerNumber);
            enregistrerAppelBloque(callerNumber, callerName, "bloque");
            call.reject(false, null);
            activeCalls.remove(call);
            return;
        }

        // 🚫 BLOCAGE DES INCONNUS (si l'option est activée dans les paramètres)
        boolean bloquerInconnus = prefs.getBoolean("block_unknown", false);
        if (bloquerInconnus && "Inconnu".equals(callerName)) {
            android.util.Log.e("LeaCallService", "🚫 [LÉA PROTECT] Inconnu rejeté : " + callerNumber);
            enregistrerAppelBloque(callerNumber, "Numéro inconnu", "inconnu");
            call.reject(false, null);
            activeCalls.remove(call);
            return;
        }

        int state = call.getState();
        if (state == Call.STATE_RINGING) {

            if (ilYAvaitDejaUnAppel) {
                // 🔔 DOUBLE APPEL ENTRANT : un appel arrive alors qu'on est déjà en ligne.
                // On ouvre l'écran de décision (attente / terminer) au lieu du plein écran classique.
                Intent waitingIntent = new Intent(this, LeaWaitingCallActivity.class);
                waitingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                waitingIntent.putExtra("number", callerNumber);
                waitingIntent.putExtra("name", callerName);
                startActivity(waitingIntent);
                demarrerSonnerie();
                broadcastState("waiting", callerNumber);
            } else {
                // Appel entrant simple : comportement d'origine
                Intent forceIntent = new Intent(this, LeaIncomingCallActivity.class);
                forceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                forceIntent.putExtra("number", callerNumber);
                forceIntent.putExtra("name", callerName);
                startActivity(forceIntent);

                NotificationManager manager = getSystemService(NotificationManager.class);
                NotificationChannel channel = new NotificationChannel("lea_call", "Appels Léa Protect", NotificationManager.IMPORTANCE_MIN);
                manager.createNotificationChannel(channel);

                Intent intent = new Intent(this, LeaIncomingCallActivity.class);
                intent.putExtra("number", callerNumber);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Notification notification = new Notification.Builder(this, "lea_call")
                        .setSmallIcon(android.R.drawable.sym_call_incoming)
                        .setContentTitle("Léa Protect : Appel entrant")
                        .setContentText(callerNumber)
                        .setFullScreenIntent(pendingIntent, true)
                        .setCategory(Notification.CATEGORY_CALL)
                        .setOngoing(true)
                        .build();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    manager.notify(4242, notification);
                }
                demarrerSonnerie();
                broadcastState("incoming", callerNumber);
            }

        } else if (state == Call.STATE_CONNECTING || state == Call.STATE_DIALING || state == Call.STATE_ACTIVE) {
            broadcastState("active", callerNumber);
        }

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                if (state == Call.STATE_RINGING) {
                    broadcastState("incoming", callerNumber);
                } else if (state == Call.STATE_DIALING || state == Call.STATE_ACTIVE) {
                    arreterSonnerie();
                    currentCall = call;
                    // On enlève la notif full-screen entrante (sinon elle redemande répondre/raccrocher)
                    NotificationManager m = getSystemService(NotificationManager.class);
                    if (m != null) m.cancel(4242);
                    // Lancement de l'écran communication (sauf si c'est un 2e appel : l'écran existe déjà)
                    if (activeCalls.size() <= 1) {
                        Intent activeIntent = new Intent(LeaCallService.this, LeaActiveCallActivity.class);
                        activeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activeIntent.putExtra("number", callerNumber);
                        activeIntent.putExtra("name", callerName);
                        startActivity(activeIntent);
                    }
                    afficherNotifEnCours(callerName.equals("Inconnu") ? callerNumber : callerName);
                    broadcastState("active", callerNumber);
                } else if (state == Call.STATE_DISCONNECTED) {
                    arreterSonnerie();
                    activeCalls.remove(call);
                    if (currentCall == call) {
                        currentCall = activeCalls.isEmpty() ? null : activeCalls.get(activeCalls.size() - 1);
                    }
                    // On ne signale "ended" QUE s'il ne reste plus aucun appel
                    if (activeCalls.isEmpty()) {
                        broadcastState("ended", callerNumber);
                    } else {
                        broadcastState("active", "");
                    }
                }
            }
        });
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        arreterSonnerie();
        activeCalls.remove(call);
        if (currentCall == call) {
            currentCall = activeCalls.isEmpty() ? null : activeCalls.get(activeCalls.size() - 1);
        }
        if (activeCalls.isEmpty()) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.cancel(4242);
            }
            retirerNotifEnCours();
            broadcastState("ended", "");
        }
    }

    // ====================== NOUVEAU : OUTILS MULTI-APPELS ======================

    /**
     * Place un 2e appel. Comme Léa est dialer par défaut, Android met
     * automatiquement le 1er appel en attente.
     * @param numero le numéro à appeler
     * @param simSlot 0 = SIM1, 1 = SIM2, -1 = laisser le système choisir
     */
    public void placerNouvelAppel(String numero, int simSlot) {
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            Uri uri = Uri.fromParts("tel", numero, null);
            Bundle extras = new Bundle();

            if (simSlot >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    List<PhoneAccountHandle> comptes = tm.getCallCapablePhoneAccounts();
                    if (comptes != null && simSlot < comptes.size()) {
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, comptes.get(simSlot));
                    }
                } catch (SecurityException ignored) { }
            }

            // Permission CALL_PHONE requise
            tm.placeCall(uri, extras);
        } catch (SecurityException e) {
            Log.e("LeaCallService", "❌ Permission manquante pour placer l'appel", e);
        } catch (Exception e) {
            Log.e("LeaCallService", "❌ Erreur placeCall", e);
        }
    }

    /** Combien de SIM appelantes sont disponibles (pour proposer le choix SIM1/SIM2). */
    public int nombreDeSim() {
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            List<PhoneAccountHandle> comptes = tm.getCallCapablePhoneAccounts();
            return comptes != null ? comptes.size() : 0;
        } catch (SecurityException e) {
            return 0;
        }
    }

    /** Fusionne tous les appels en une conférence (à 3, 4, 5… selon l'opérateur). */
    public void fusionnerEnConference() {
        if (activeCalls.size() < 2) return;
        Call premier = activeCalls.get(0);
        for (int i = 1; i < activeCalls.size(); i++) {
            try {
                premier.conference(activeCalls.get(i));
            } catch (Exception e) {
                Log.e("LeaCallService", "❌ Erreur conférence", e);
            }
        }
    }

    /** Sépare un participant de la conférence et le met en attente (tu restes avec les autres). */
    public void separerEtMettreEnAttente(Call participant) {
        if (participant == null) return;
        try {
            participant.splitFromConference(); // sort de la conférence
            participant.hold();                 // le met en attente
        } catch (Exception e) {
            Log.e("LeaCallService", "❌ Erreur séparation", e);
        }
    }

    /** Vire (raccroche) un participant précis. */
    public void virerParticipant(Call participant) {
        if (participant == null) return;
        try {
            participant.disconnect();
        } catch (Exception e) {
            Log.e("LeaCallService", "❌ Erreur virer participant", e);
        }
    }

    /** L'appel "en attente" (celui qui n'est pas au premier plan), s'il existe. */
    public Call appelEnAttente() {
        for (Call c : activeCalls) {
            if (c != currentCall && c.getState() == Call.STATE_HOLDING) {
                return c;
            }
        }
        return null;
    }

    // ====================== EXISTANT (inchangé) ======================
    
    /** Notification persistante "appel en cours" : tapable pour revenir à l'écran d'appel. */
    public void afficherNotifEnCours(String titre) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                "lea_ongoing", "Appel Léa en cours", NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);

        Intent retour = new Intent(this, LeaActiveCallActivity.class);
        retour.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        retour.putExtra("number", cachedNumber);
        PendingIntent pi = PendingIntent.getActivity(this, 1, retour,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new Notification.Builder(this, "lea_ongoing")
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("Appel en cours")
                .setContentText(titre)
                .setContentIntent(pi)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            manager.notify(4243, notif);
        }
    }

    /** Retire la notification d'appel en cours. */
    public void retirerNotifEnCours() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(4243);
    }

    /** Démarre la sonnerie sur le bon canal (flux sonnerie + haut-parleur). */
    private void demarrerSonnerie() {
        try {
            // Si le téléphone est en mode silencieux/vibreur, on ne joue pas le son
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                // mode silencieux ou vibreur : pas de son (la vibration plus bas s'en charge)
            } else {
                Uri uri = null;
                android.content.SharedPreferences prefsSon = getSharedPreferences("LeaProtect", MODE_PRIVATE);
                String ringtonePerso = prefsSon.getString("ringtone_uri", "");
                if (ringtonePerso != null && !ringtonePerso.isEmpty()) {
                    try { uri = Uri.parse(ringtonePerso); } catch (Exception ignored) { }
                }
                if (uri == null) {
                    uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
                }
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
                sonnerie = new MediaPlayer();
                sonnerie.setDataSource(this, uri);
                sonnerie.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build());
                sonnerie.setLooping(true);
                sonnerie.prepare();
                sonnerie.start();
            }
        } catch (Exception e) {
            Log.e("LeaCallService", "❌ Erreur sonnerie", e);
        }

        try {
            vibreur = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibreur != null && vibreur.hasVibrator()) {
                long[] motif = {0, 800, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibreur.vibrate(VibrationEffect.createWaveform(motif, 0));
                } else {
                    vibreur.vibrate(motif, 0);
                }
            }
        } catch (Exception e) {
            Log.e("LeaCallService", "❌ Erreur vibration", e);
        }
    }

    /** Arrête la sonnerie + la vibration. */
    private void arreterSonnerie() {
        try {
            if (sonnerie != null) {
                if (sonnerie.isPlaying()) sonnerie.stop();
                sonnerie.release();
            }
            sonnerie = null;
        } catch (Exception ignored) { }
        try {
            if (vibreur != null) {
                vibreur.cancel();
            }
            vibreur = null;
        } catch (Exception ignored) { }
    }

    private void broadcastState(String state, String number) {
        cachedState = state;
        cachedNumber = number;

        Intent intent = new Intent("LEA_CALL_STATE_CHANGED");
        intent.setPackage(getPackageName());
        intent.putExtra("state", state);
        intent.putExtra("number", number);
        sendBroadcast(intent);
    }

    /** Enregistre un appel bloqué dans notre journal maison (numéro|nom|date|raison). */
    private void enregistrerAppelBloque(String numero, String nom, String raison) {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("LeaProtect", MODE_PRIVATE);
            java.util.Set<String> journal = new java.util.HashSet<>(
                    prefs.getStringSet("blocked_call_log", new java.util.HashSet<>()));
            // Format : numero|nom|timestamp|raison
            String entree = numero + "|" + (nom != null ? nom : "") + "|"
                    + System.currentTimeMillis() + "|" + raison;
            journal.add(entree);
            // On limite à 100 entrées pour ne pas gonfler à l'infini
            if (journal.size() > 100) {
                // on retire la plus ancienne
                String plusAncienne = null;
                long minTime = Long.MAX_VALUE;
                for (String e : journal) {
                    String[] p = e.split("\\|");
                    if (p.length >= 3) {
                        long t = Long.parseLong(p[2]);
                        if (t < minTime) { minTime = t; plusAncienne = e; }
                    }
                }
                if (plusAncienne != null) journal.remove(plusAncienne);
            }
            prefs.edit().putStringSet("blocked_call_log", journal).apply();
        } catch (Exception e) {
            Log.e("LeaCallService", "Erreur journal bloqué", e);
        }
    }

    /** Normalise un numéro pour comparaison : enlève espaces/points, convertit +33 → 0, etc. */
    public static String normaliserNumero(String numero) {
        if (numero == null) return "";
        // On garde seulement les chiffres et le +
        String n = numero.replaceAll("[^0-9+]", "");
        // Conversion des indicatifs France vers format national 0X
        if (n.startsWith("+33")) {
            n = "0" + n.substring(3);
        } else if (n.startsWith("0033")) {
            n = "0" + n.substring(4);
        } else if (n.startsWith("33") && n.length() == 11) {
            n = "0" + n.substring(2);
        }
        return n;
    }

    @android.annotation.SuppressLint("Range")
    private String getContactName(String phoneNumber) {
        android.content.ContentResolver cr = getContentResolver();
        android.net.Uri uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber));
        android.database.Cursor cursor = cr.query(uri, new String[]{android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) return "Inconnu";
        String contactName = "Inconnu";
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        cursor.close();
        return contactName;
    }
}