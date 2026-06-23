package com.flolov42.lea_v3.routines;

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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Locale;

/**
 * Exécuteur d'actions pour les routines Léa.
 * Actions : BRIGHTNESS · VOLUME · WIFI · BLUETOOTH · SOUND_MODE ·
 *           LAUNCH_APP · OPEN_CONTACT · NETWORK_MODE ·
 *           DO_NOT_DISTURB · FLASHLIGHT · ALARM · SPEAK_TEXT · SHOW_NOTIFICATION
 */
public class LeaRoutineActionsExecutor {

    private static final String TAG        = "LeaActionsExec";
    private static final String NOTIF_CHAN = "LEA_ROUTINE_ACTION";
    private final Context ctx;
    private TextToSpeech tts;

    public LeaRoutineActionsExecutor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        initTts();
        initNotifChannel();
    }

    public void execute(String actionsJson) {
        try {
            JSONArray arr = new JSONArray(actionsJson != null ? actionsJson : "[]");
            for (int i = 0; i < arr.length(); i++) executeOne(arr.getJSONObject(i));
        } catch (Exception e) { Log.e(TAG, "execute: " + e.getMessage()); }
    }

    private void executeOne(JSONObject action) {
        try {
            switch (action.getString("type")) {
                case "BRIGHTNESS":        setBrightness(action.optInt("value", 50));                break;
                case "VOLUME":            setVolume(action.optString("level", "MEDIUM"));           break;
                case "WIFI":              setWifi(action.optBoolean("enabled", true));              break;
                case "BLUETOOTH":         setBluetooth(action.optBoolean("enabled", true));         break;
                case "SOUND_MODE":        setSoundMode(action.optString("mode", "NORMAL"));         break;
                case "LAUNCH_APP":        launchApp(action.optString("package", ""));              break;
                case "NETWORK_MODE":      openNetworkSettings();                                    break;
                case "OPEN_CONTACT":      openContact(action.optString("number", ""));             break;
                case "DO_NOT_DISTURB":    setDoNotDisturb(action.optBoolean("enabled", true));      break;
                case "FLASHLIGHT":        setFlashlight(action.optBoolean("enabled", false));       break;
                case "ALARM":             setAlarm(action.optInt("hour", 7), action.optInt("minute", 0)); break;
                case "SPEAK_TEXT":        speakText(action.optString("text", ""));                 break;
                case "SHOW_NOTIFICATION": showNotification(
                        action.optString("title", "Léa"),
                        action.optString("text",  "Routine active"));                              break;
                default: Log.w(TAG, "Unknown action: " + action.optString("type"));               break;
            }
        } catch (Exception e) { Log.e(TAG, "executeOne: " + e.getMessage()); }
    }

    // ── Luminosité ───────────────────────────────────────────────────────────

    private void setBrightness(int percent) {
        try {
            if (!Settings.System.canWrite(ctx)) return;
            int value = Math.max(0, Math.min(255, (int)(percent / 100.0 * 255)));
            Settings.System.putInt(ctx.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(ctx.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, value);
        } catch (Exception e) { Log.e(TAG, "setBrightness: " + e.getMessage()); }
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    private void setVolume(String level) {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            int maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int maxRing  = am.getStreamMaxVolume(AudioManager.STREAM_RING);
            int music, ring;
            switch (level) {
                case "MUTE":   music = 0;            ring = 0;            break;
                case "LOW":    music = maxMusic / 4;  ring = maxRing / 4;  break;
                case "HIGH":   music = maxMusic;       ring = maxRing;      break;
                default:       music = maxMusic / 2;  ring = maxRing / 2;  break;
            }
            am.setStreamVolume(AudioManager.STREAM_MUSIC, music, 0);
            am.setStreamVolume(AudioManager.STREAM_RING,  ring,  0);
        } catch (Exception e) { Log.e(TAG, "setVolume: " + e.getMessage()); }
    }

    // ── WiFi ──────────────────────────────────────────────────────────────────

    private void setWifi(boolean enabled) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                if (wm != null) wm.setWifiEnabled(enabled);
            } else {
                Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }
        } catch (Exception e) { Log.e(TAG, "setWifi: " + e.getMessage()); }
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────

    private void setBluetooth(boolean enabled) {
        try {
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            if (bt == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (enabled && !bt.isEnabled()) bt.enable();
                else if (!enabled && bt.isEnabled()) bt.disable();
            } else {
                String action = enabled ? "activer" : "désactiver";
                LeaAgentNotificationManager.get(ctx).notify(
                    "ROUTINE",
                    "⚡ Bluetooth — action requise",
                    "Ta routine demande d'" + action + " le Bluetooth. Glisse depuis le haut.");
            }
        } catch (Exception e) { Log.e(TAG, "setBluetooth: " + e.getMessage()); }
    }

    // ── Mode son ──────────────────────────────────────────────────────────────

    private void setSoundMode(String mode) {
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            switch (mode) {
                case "SILENT":  am.setRingerMode(AudioManager.RINGER_MODE_SILENT);  break;
                case "VIBRATE": am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE); break;
                default:        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);  break;
            }
        } catch (Exception e) { Log.e(TAG, "setSoundMode: " + e.getMessage()); }
    }

    // ── Ne pas déranger ───────────────────────────────────────────────────────

    private void setDoNotDisturb(boolean enabled) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (!nm.isNotificationPolicyAccessGranted()) {
                LeaAgentNotificationManager.get(ctx).notify(
                    "ROUTINE",
                    "🚫 DND — permission requise",
                    "Autorise Léa à gérer le mode Ne pas déranger dans les Paramètres > Applications spéciales.");
                return;
            }
            nm.setInterruptionFilter(enabled
                ? NotificationManager.INTERRUPTION_FILTER_NONE
                : NotificationManager.INTERRUPTION_FILTER_ALL);
        } catch (Exception e) { Log.e(TAG, "setDND: " + e.getMessage()); }
    }

    // ── Lampe torche ──────────────────────────────────────────────────────────

    private void setFlashlight(boolean enabled) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return;
            String[] ids = cm.getCameraIdList();
            if (ids.length > 0) cm.setTorchMode(ids[0], enabled);
        } catch (Exception e) { Log.e(TAG, "setFlashlight: " + e.getMessage()); }
    }

    // ── Alarme ───────────────────────────────────────────────────────────────

    private void setAlarm(int hour, int minute) {
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(AlarmClock.EXTRA_HOUR,    hour);
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception e) { Log.e(TAG, "setAlarm: " + e.getMessage()); }
    }

    // ── TTS — Léa parle ───────────────────────────────────────────────────────

    private void initTts() {
        try {
            tts = new TextToSpeech(ctx, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.FRENCH);
                }
            });
        } catch (Exception e) { Log.e(TAG, "initTts: " + e.getMessage()); }
    }

    private void speakText(String text) {
        if (text == null || text.isEmpty()) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (tts != null) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LEA_ROUTINE");
                }
            } catch (Exception e) { Log.e(TAG, "speakText: " + e.getMessage()); }
        }, 500);
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private void initNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(
                    NOTIF_CHAN, "Actions Routines Léa", NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private void showNotification(String title, String text) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b = new Notification.Builder(ctx, NOTIF_CHAN);
            } else {
                b = new Notification.Builder(ctx);
            }
            Notification notif = b
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build();
            nm.notify((int)(System.currentTimeMillis() % 10000), notif);
        } catch (Exception e) { Log.e(TAG, "showNotification: " + e.getMessage()); }
    }

    // ── App / Contact / Réseau ────────────────────────────────────────────────

    private void launchApp(String packageName) {
        try {
            if (packageName.isEmpty()) return;
            Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(launch);
            }
        } catch (Exception e) { Log.e(TAG, "launchApp: " + e.getMessage()); }
    }

    private void openNetworkSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            try {
                Intent fb = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                fb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(fb);
            } catch (Exception ex) { Log.e(TAG, "openNetworkSettings: " + ex.getMessage()); }
        }
    }

    private void openContact(String number) {
        try {
            if (number.isEmpty()) return;
            Intent i = new Intent(Intent.ACTION_DIAL,
                android.net.Uri.fromParts("tel", number, null));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) { Log.e(TAG, "openContact: " + e.getMessage()); }
    }
}
