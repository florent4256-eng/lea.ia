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


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.MediaStore;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.List;

/**
 * Exécute les commandes basiques : appels, SMS, musique, galerie, apps.
 * Toutes les méthodes retournent un message vocal.
 */
public class LeaCommandExecutor {

    private static final String TAG = "LeaCmdExec";
    private final Context ctx;

    public LeaCommandExecutor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    // ── Appels téléphoniques ──────────────────────────────────────────────────

    public String makeCall(String nameOrNumber) {
        String number = isPhoneNumber(nameOrNumber)
            ? nameOrNumber : findContactNumber(nameOrNumber);
        if (number.isEmpty()) return "Contact « " + nameOrNumber + " » introuvable.";
        try {
            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number.replaceAll("\\s", "")));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "📞 J'appelle " + nameOrNumber + ".";
        } catch (SecurityException e) {
            return "Permission d'appel manquante.";
        } catch (Exception e) {
            return "Impossible d'appeler.";
        }
    }

    public String openDialer(String nameOrNumber) {
        String number = isPhoneNumber(nameOrNumber)
            ? nameOrNumber : findContactNumber(nameOrNumber);
        Uri uri = number.isEmpty()
            ? Uri.parse("tel:")
            : Uri.parse("tel:" + number.replaceAll("\\s", ""));
        try {
            Intent i = new Intent(Intent.ACTION_DIAL, uri);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "📞 Numéroteur ouvert" + (number.isEmpty() ? "." : " pour " + nameOrNumber + ".");
        } catch (Exception e) {
            return "Impossible d'ouvrir le numéroteur.";
        }
    }

    // ── SMS ───────────────────────────────────────────────────────────────────

    public String openSmsComposer(String nameOrNumber, String text) {
        String number = isPhoneNumber(nameOrNumber)
            ? nameOrNumber : findContactNumber(nameOrNumber);
        try {
            Intent i = new Intent(Intent.ACTION_SENDTO,
                Uri.parse("smsto:" + (number.isEmpty() ? "" : number.replaceAll("\\s", ""))));
            if (text != null && !text.isEmpty()) i.putExtra("sms_body", text);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "💬 Application SMS ouverte" + (nameOrNumber.isEmpty() ? "." : " pour " + nameOrNumber + ".");
        } catch (Exception e) {
            return "Impossible d'ouvrir l'app SMS.";
        }
    }

    public String sendSmsDirectly(String nameOrNumber, String text) {
        String number = isPhoneNumber(nameOrNumber)
            ? nameOrNumber : findContactNumber(nameOrNumber);
        if (number.isEmpty()) return "Numéro introuvable pour « " + nameOrNumber + " ».";
        if (text == null || text.trim().isEmpty()) return openSmsComposer(nameOrNumber, null);
        try {
            SmsManager.getDefault().sendTextMessage(
                number.replaceAll("\\s", ""), null, text, null, null);
            return "✅ SMS envoyé à " + nameOrNumber + ".";
        } catch (Exception e) {
            Log.e(TAG, "sendSms: " + e.getMessage());
            return openSmsComposer(nameOrNumber, text);
        }
    }

    // ── Musique ───────────────────────────────────────────────────────────────

    public String openMusic(String query) {
        // Essaie MediaStore intent générique
        try {
            Intent i = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*");
            if (query != null && !query.isEmpty())
                i.putExtra(android.app.SearchManager.QUERY, query);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "🎵 Lecture de " + (query != null && !query.isEmpty() ? query : "la musique") + ".";
        } catch (Exception ignored) {}

        // Fallback : apps musicales dans l'ordre de préférence
        String[][] musicApps = {
            { "com.spotify.music",               "Spotify"         },
            { "com.google.android.youtube.music","YouTube Music"   },
            { "com.sec.android.app.music",       "Musique Samsung" },
            { "com.amazon.mp3",                  "Amazon Music"    },
            { "com.deezer.android.app",          "Deezer"          },
        };
        for (String[] app : musicApps) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(app[0]);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return "🎵 " + app[1] + " lancé.";
                }
            } catch (Exception ignored) {}
        }
        return "Aucune app musicale trouvée.";
    }

    // ── Galerie ───────────────────────────────────────────────────────────────

    public String openGallery() {
        String[] galleryApps = {
            "com.sec.android.gallery3d",
            "com.samsung.android.gallery",
            "com.google.android.apps.photos"
        };
        for (String pkg : galleryApps) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return "🖼️ Galerie ouverte.";
                }
            } catch (Exception ignored) {}
        }
        // Fallback intent générique
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "🖼️ Galerie ouverte.";
        } catch (Exception e) {
            return "Impossible d'ouvrir la galerie.";
        }
    }

    // ── Caméra ────────────────────────────────────────────────────────────────

    public String openCamera(boolean video) {
        try {
            Intent i = new Intent(video ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return video ? "🎥 Caméra vidéo ouverte." : "📷 Caméra ouverte.";
        } catch (Exception e) {
            return "Impossible d'ouvrir la caméra.";
        }
    }

    // ── Ouvre une app par nom ─────────────────────────────────────────────────

    public String openApp(String appName) {
        if (appName == null || appName.trim().isEmpty()) return "Quel app souhaites-tu ouvrir ?";
        String query = appName.toLowerCase().trim();

        // Correspondances directes connues
        String pkg = resolveKnownApp(query);
        if (pkg != null) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return "✅ " + appName + " lancé.";
                }
            } catch (Exception ignored) {}
        }

        // Recherche par label dans toutes les apps installées
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = ctx.getPackageManager().queryIntentActivities(main, 0);
        for (ResolveInfo ri : apps) {
            String label = ri.loadLabel(ctx.getPackageManager()).toString().toLowerCase();
            if (label.contains(query)) {
                try {
                    Intent launch = ctx.getPackageManager()
                        .getLaunchIntentForPackage(ri.activityInfo.packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(launch);
                        return "✅ " + ri.loadLabel(ctx.getPackageManager()) + " lancé.";
                    }
                } catch (Exception ignored) {}
            }
        }
        return "App « " + appName + " » introuvable.";
    }

    // ── Navigation / Maps ─────────────────────────────────────────────────────

    public String openMaps(String destination) {
        try {
            Intent i;
            if (destination != null && !destination.isEmpty()) {
                i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + Uri.encode(destination)));
            } else {
                i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "🗺️ Navigation vers " + (destination != null ? destination : "ta position") + " lancée.";
        } catch (Exception e) {
            return "Impossible d'ouvrir Maps.";
        }
    }

    // ── Contacts ──────────────────────────────────────────────────────────────

    public String openContacts() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "📒 Contacts ouverts.";
        } catch (Exception e) {
            return "Impossible d'ouvrir les contacts.";
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressLint("Range")
    public String findContactNumber(String name) {
        try {
            Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{ "%" + name + "%" }, null);
            String number = "";
            if (c != null) {
                if (c.moveToFirst())
                    number = c.getString(c.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                c.close();
            }
            return number != null ? number : "";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isPhoneNumber(String s) {
        return s != null && s.matches("[+0-9 \\-()]{5,20}");
    }

    private String resolveKnownApp(String name) {
        if (name.contains("instagram"))    return "com.instagram.android";
        if (name.contains("twitter") || name.contains("x "))
                                           return "com.twitter.android";
        if (name.contains("whatsapp"))     return "com.whatsapp";
        if (name.contains("telegram"))     return "org.telegram.messenger";
        if (name.contains("youtube"))      return "com.google.android.youtube";
        if (name.contains("maps"))         return "com.google.android.apps.maps";
        if (name.contains("gmail"))        return "com.google.android.gm";
        if (name.contains("chrome"))       return "com.android.chrome";
        if (name.contains("samsung internet") || name.contains("internet samsung"))
                                           return "com.sec.android.app.sbrowser";
        if (name.contains("snapchat"))     return "com.snapchat.android";
        if (name.contains("tiktok"))       return "com.zhiliaoapp.musically";
        if (name.contains("netflix"))      return "com.netflix.mediaclient";
        if (name.contains("spotify"))      return "com.spotify.music";
        if (name.contains("paramètres") || name.contains("settings"))
                                           return "com.android.settings";
        return null;
    }
}
