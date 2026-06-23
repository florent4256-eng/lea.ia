package com.flolov42.lea_v3.bixby;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity transparente — affiche un popup pour choisir l'app musicale préférée.
 * Lancée depuis LeaNovaService (impossible d'afficher AlertDialog depuis un Service).
 */
public class MusicChooserActivity extends Activity {

    private static final String[] MUSIC_PACKAGES = {
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.sec.android.app.music",
        "com.deezer.android.app",
        "com.apple.android.music",
        "com.tidal.wave",
        "com.soundcloud.android",
        "com.amazon.mp3",
        "com.pandora.android",
        "com.qobuz.music",
        "com.napster.android",
        "com.anghami.android"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager pm = getPackageManager();
        final List<String> names    = new ArrayList<>();
        final List<String> packages = new ArrayList<>();

        for (String pkg : MUSIC_PACKAGES) {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    names.add(pm.getApplicationLabel(info).toString());
                    packages.add(pkg);
                } catch (Exception ignored) {}
            }
        }

        if (names.isEmpty()) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("🎵 Quelle app de musique ?")
            .setItems(names.toArray(new String[0]), (dialog, which) -> {
                String chosenPkg  = packages.get(which);
                String chosenName = names.get(which);
                if (LeaNovaService.instance != null) {
                    LeaNovaService.instance.onMusicAppChosen(chosenPkg, chosenName);
                }
                finish();
            })
            .setOnCancelListener(d -> finish())
            .show();
    }
}
