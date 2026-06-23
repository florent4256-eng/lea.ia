package com.flolov42.lea_v3.ui;

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


import android.app.NotificationManager;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "LeaPhone")
public class LeaPhonePlugin extends Plugin {

    private BroadcastReceiver callStateReceiver;

    @Override
    public void load() {
        super.load();

        callStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("LEA_CALL_STATE_CHANGED".equals(intent.getAction())) {
                    String state = intent.getStringExtra("state");
                    String number = intent.getStringExtra("number");

                    JSObject ret = new JSObject();
                    ret.put("state", state);
                    ret.put("number", number);

                    notifyListeners("onCallStateChanged", ret);
                }
            }
        };

        IntentFilter filter = new IntentFilter("LEA_CALL_STATE_CHANGED");
        if (Build.VERSION.SDK_INT >= 33) {
            getContext().registerReceiver(callStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(callStateReceiver, filter);
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (callStateReceiver != null) {
            getContext().unregisterReceiver(callStateReceiver);
        }
        super.handleOnDestroy();
    }

    @PluginMethod
    public void placeCall(PluginCall call) {
        String number = call.getString("number");
        
        if (number == null || number.isEmpty()) {
            call.reject("Numéro vide.");
            return;
        }

        Context context = getContext();
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            call.reject("Permission refusée.");
            return;
        }

        try {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            Uri uri = Uri.fromParts("tel", number, null);
            Bundle extras = new Bundle();
            telecomManager.placeCall(uri, extras);
            call.resolve();
        } catch (SecurityException e) {
            call.reject("Erreur critique: " + e.getMessage());
        }
    }

    // 📞 OUVERTURE DU CADRAN NATIF SÉCURISÉE
    @PluginMethod
    public void openNativeDialer(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(getActivity(), LeaDialerActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // ⭐ OUVERTURE DES ROUTINES INTELLIGENTES
    @PluginMethod
    public void openRoutines(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(), "🚀 Ouverture Routines...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LeaRoutineActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Throwable e) {
                Toast.makeText(getContext(), "❌ Routines: " + e.getMessage(), Toast.LENGTH_LONG).show();
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 🤖 OUVERTURE DES AGENTS INTELLIGENTS
    @PluginMethod
    public void openAgents(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(), "🚀 Ouverture Agents...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LeaAgentActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Throwable e) {
                Toast.makeText(getContext(), "❌ Agents: " + e.getMessage(), Toast.LENGTH_LONG).show();
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // ✨ LÉA PLUS — 15 features, 5 onglets
    @PluginMethod
    public void openLeaPlus(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(), "🚀 Ouverture Léa Plus...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LeaPlusActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Throwable e) {
                Toast.makeText(getContext(), "❌ LeaPlus: " + e.getMessage(), Toast.LENGTH_LONG).show();
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 🏠 LÉA HOME — Domotique complète
    @PluginMethod
    public void openLeaHome(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(), "🚀 Ouverture Home...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), com.flolov42.lea_v3.home.LeaHomeActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Throwable e) {
                Toast.makeText(getContext(), "❌ Home: " + e.getMessage(), Toast.LENGTH_LONG).show();
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 🟢 ORDRE DE DÉCROCHER
    @PluginMethod
    public void answerCall(PluginCall call) {
        if (LeaCallService.instance != null && LeaCallService.instance.currentCall != null) {
            LeaCallService.instance.currentCall.answer(VideoProfile.STATE_AUDIO_ONLY);
            call.resolve();
        } else {
            call.reject("Aucun appel en cours à décrocher.");
        }
    }

    // 🔴 ORDRE DE RACCROCHER / REJETER
    @PluginMethod
    public void endCall(PluginCall call) {
        if (LeaCallService.instance != null && LeaCallService.instance.currentCall != null) {
            Call currentCall = LeaCallService.instance.currentCall;
            if (currentCall.getState() == Call.STATE_RINGING) {
                currentCall.reject(false, null);
            } else {
                currentCall.disconnect();
            }
            call.resolve();
        } else {
            call.reject("Aucun appel en cours à raccrocher.");
        }
    }

    // 📖 LECTURE DU RÉPERTOIRE SOUVERAIN
    @PluginMethod
    public void getContacts(PluginCall call) {
        com.getcapacitor.JSArray contactsArray = new com.getcapacitor.JSArray();
        Context context = getContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            call.reject("Permission READ_CONTACTS refusée.");
            return;
        }

        try {
            android.content.ContentResolver resolver = context.getContentResolver();
            android.database.Cursor cursor = resolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null,
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                    
                    if(nameIndex >= 0 && numIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        String phoneNumber = cursor.getString(numIndex);
                        
                        // 🛡️ SÉCURITÉ : Blindage des valeurs nulles
                        if (name == null) name = "Inconnu";
                        if (phoneNumber == null) phoneNumber = "Inconnu";
                        
                        JSObject contact = new JSObject();
                        contact.put("name", name);
                        contact.put("number", phoneNumber);
                        contactsArray.put(contact);
                    }
                }
                cursor.close();
            }
            
            JSObject ret = new JSObject();
            ret.put("contacts", contactsArray);
            call.resolve(ret);
            
        } catch (Exception e) {
            call.reject("Erreur de lecture des contacts : " + e.getMessage());
        }
    }
    // 📖 LECTURE DU JOURNAL D'APPELS (RÉCENTS)
    @PluginMethod
    public void getCallLog(PluginCall call) {
        com.getcapacitor.JSArray logArray = new com.getcapacitor.JSArray();
        Context context = getContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            call.reject("Permission READ_CALL_LOG refusée.");
            return;
        }

        try {
            android.database.Cursor cursor = context.getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    null, null, null, android.provider.CallLog.Calls.DATE + " DESC LIMIT 50"
            );

            if (cursor != null) {
                int numberIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
                int typeIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);
                int dateIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE);
                int nameIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME);

                while (cursor.moveToNext()) {
                    JSObject logEntry = new JSObject();
                    logEntry.put("number", cursor.getString(numberIndex));
                    logEntry.put("type", cursor.getInt(typeIndex)); // 1=Reçu, 2=Émis, 3=Manqué
                    logEntry.put("date", cursor.getLong(dateIndex));
                    // 🛡️ SÉCURITÉ ANDROID 16 : Si le nom est null, on envoie une chaîne vide pour ne pas crasher Capacitor
                    String cachedName = cursor.getString(nameIndex);
                    logEntry.put("name", cachedName != null ? cachedName : "");
                    logArray.put(logEntry);
                }
                cursor.close();
            }
            
            JSObject ret = new JSObject();
            ret.put("logs", logArray);
            call.resolve(ret);
            
        } catch (Exception e) {
            call.reject("Erreur historique : " + e.getMessage());
        }
    }

    // 🛡️ BASE DE DONNÉES SOUVERAINE : NUMÉROS BLOQUÉS
    @PluginMethod
    public void getBlockedNumbers(PluginCall call) {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        java.util.Set<String> blockedSet = prefs.getStringSet("blocked_numbers", new java.util.HashSet<>());
        com.getcapacitor.JSArray array = new com.getcapacitor.JSArray();
        for (String num : blockedSet) { array.put(num); }
        JSObject ret = new JSObject();
        ret.put("numbers", array);
        call.resolve(ret);
    }

    @PluginMethod
    public void addBlockedNumber(PluginCall call) {
        String number = call.getString("number");
        if (number == null || number.isEmpty()) { call.reject("Vide"); return; }
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        java.util.Set<String> blockedSet = new java.util.HashSet<>(prefs.getStringSet("blocked_numbers", new java.util.HashSet<>()));
        blockedSet.add(number);
        prefs.edit().putStringSet("blocked_numbers", blockedSet).apply();
        call.resolve();
    }
@PluginMethod
    public void removeBlockedNumber(PluginCall call) {
        String number = call.getString("number");
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        java.util.Set<String> blockedSet = new java.util.HashSet<>(prefs.getStringSet("blocked_numbers", new java.util.HashSet<>()));
        blockedSet.remove(number);
        prefs.edit().putStringSet("blocked_numbers", blockedSet).apply();
        call.resolve();
    }

    // 🔄 REQUÊTE DE SYNCHRONISATION SYNCHRONE (Pour que React lise le cache)
    @PluginMethod
    public void getCurrentCallState(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("state", LeaCallService.cachedState);
        ret.put("number", LeaCallService.cachedNumber);
        ret.put("name", LeaCallService.cachedName);
        call.resolve(ret);
    }
    
    // 🛡️ LE REPLI SOUVERAIN (Pour masquer l'application à la fin de l'appel)
    @PluginMethod
    public void minimizeApp(PluginCall call) {
        // On purge la mémoire pour éviter les boucles
        LeaCallService.cachedState = "none";
        LeaCallService.cachedNumber = "";
        LeaCallService.cachedName = "Inconnu";
        getBridge().getActivity().runOnUiThread(() -> getBridge().getActivity().moveTaskToBack(true));
        call.resolve();
    }

    // 🧊 FORGE 3D — Interface native (SceneView)
    @PluginMethod
    public void openForge3D(PluginCall call) {
        String currentUser = call.getString("currentUser", "");
        getActivity().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(getActivity(), com.flolov42.lea_v3.studio.LeaForge3DActivity.class);
                intent.putExtra("currentUser", currentUser);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erreur Forge3D: " + e.getMessage());
            }
        });
    }

    // 🔄 OUVERTURE DES MISES À JOUR (One UI style)
    @PluginMethod
    public void openUpdates(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(getActivity(), UpdateActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 📝 OUVERTURE DU MODULE OFFICE (Traitement de texte IA)
    @PluginMethod
    public void openOffice(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(getActivity(), com.flolov42.lea_v3.office.LeaOfficeActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 🗺️ OUVERTURE DU MODULE MAPS (OSMDroid natif)
    @PluginMethod
    public void openMaps(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(), "🚀 Ouverture Maps...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), com.flolov42.lea_v3.maps.LeaMapsActivity.class);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Throwable e) {
                Toast.makeText(getContext(), "❌ Maps: " + e.getMessage(), Toast.LENGTH_LONG).show();
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 🚗 OUVERTURE DU MODULE AUTO (OBD2 natif)
    @PluginMethod
    public void openAuto(PluginCall call) {
        String currentUser = call.getString("currentUser", "");
        getActivity().runOnUiThread(() -> {
            try {
                Toast.makeText(getContext(), "🚀 Ouverture Auto...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), com.flolov42.lea_v3.auto.LeaAutoActivity.class);
                intent.putExtra("currentUser", currentUser);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Throwable e) {
                Toast.makeText(getContext(), "❌ Auto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                call.reject("Erreur Java: " + e.getMessage());
            }
        });
    }

    // 🚪 DÉCONNEXION — remet à zéro tout l'état Android lié au compte
    @PluginMethod
    public void logout(PluginCall call) {
        Context ctx = getContext().getApplicationContext();
        new Thread(() -> {
            try {
                // 1. Désactive tous les agents
                LeaAgentDatabase agentDb = LeaAgentDatabase.get(ctx);
                String[] agentIds = {
                    LeaAgentActivationManager.EMAIL, LeaAgentActivationManager.NOTIFICATION,
                    LeaAgentActivationManager.CALENDAR, LeaAgentActivationManager.FINANCE,
                    LeaAgentActivationManager.HEALTH, LeaAgentActivationManager.PRODUCTIVITY,
                    LeaAgentActivationManager.SOCIAL, LeaAgentActivationManager.SMART_HOME,
                    LeaAgentActivationManager.LEARNING, LeaAgentActivationManager.SECURITY,
                    LeaAgentActivationManager.CODE
                };
                for (String id : agentIds) agentDb.setEnabled(id, false);

                // 2. Désactive toutes les features LeaPlus
                LeaPlusDatabase plusDb = LeaPlusDatabase.get(ctx);
                String[] plusIds = {
                    LeaPlusDatabase.QUESTS, LeaPlusDatabase.ADVENTURE, LeaPlusDatabase.COINS,
                    LeaPlusDatabase.HABITS, LeaPlusDatabase.REPORT, LeaPlusDatabase.COMPANION,
                    LeaPlusDatabase.LIFE_OS, LeaPlusDatabase.STUDENT, LeaPlusDatabase.LANGUAGE,
                    LeaPlusDatabase.SMART_NOTIF, LeaPlusDatabase.CLOUD_SYNC,
                    LeaPlusDatabase.MARKETPLACE, LeaPlusDatabase.FAMILY,
                    LeaPlusDatabase.OMNICHANNEL, LeaPlusDatabase.STREAMING
                };
                for (String id : plusIds) plusDb.setEnabled(id, false);

                // 3. Désactive toutes les routines et leur consentement user
                LeaRoutineDatabase routineDb = LeaRoutineDatabase.get(ctx);
                for (LeaRoutineDatabase.RoutineRow r : routineDb.getAllRoutines()) {
                    routineDb.setRoutineActive(r.id, false);
                    routineDb.setRoutineUserEnabled(r.id, false);
                }

                // 4. Arrête les services en arrière-plan
                ctx.stopService(new Intent(ctx, LeaAgentService.class));
                ctx.stopService(new Intent(ctx, LeaRoutineConditionDetector.class));

                // 5. Annule toutes les notifications
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancelAll();

                // 6. Supprime la session utilisateur
                ctx.getSharedPreferences("LeaPrefs", Context.MODE_PRIVATE)
                   .edit().remove("lea_session_user").apply();

                LeaAndroidLogger.info(ctx, "Auth", "Déconnexion — état Android remis à zéro");
                call.resolve();
            } catch (Exception e) {
                call.reject("Erreur logout: " + e.getMessage());
            }
        }).start();
    }
}