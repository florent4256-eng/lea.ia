package com.flolov42.lea_v3.bixby;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Activité transparente qui prend une photo puis l'envoie au serveur via WebSocket (LLaVA).
 */
public class LeaCameraActivity extends Activity {

    private static final int REQ_CAMERA = 77;
    private Uri photoUri;
    private String question;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        question = getIntent().getStringExtra("question");
        if (question == null) question = "Décris ce que tu vois.";

        // Crée un fichier temporaire pour la photo
        try {
            File photoFile = new File(getCacheDir(), "lea_vision_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(this, "com.flolov42.lea_v3.fileprovider", photoFile);

            Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(camIntent, REQ_CAMERA);
        } catch (Exception e) {
            Log.e("LeaCamera", "❌ Impossible d'ouvrir la caméra : " + e.getMessage());
            Toast.makeText(this, "Caméra indisponible", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CAMERA) { finish(); return; }

        if (resultCode != RESULT_OK || photoUri == null) {
            finish();
            return;
        }

        new Thread(() -> {
            try {
                // Charger et compresser la photo
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                if (bmp == null) throw new Exception("Bitmap null");

                // Redimensionner pour économiser la VRAM (max 640px de largeur)
                int maxW = 640;
                if (bmp.getWidth() > maxW) {
                    int newH = (int) (bmp.getHeight() * ((float) maxW / bmp.getWidth()));
                    bmp = Bitmap.createScaledBitmap(bmp, maxW, newH, true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                // Envoi via WebSocket au serveur
                LeaNovaService svc = LeaNovaService.instance;
                if (svc != null && svc.wsClient != null && svc.wsClient.isOpen()) {
                    JSONObject msg = new JSONObject();
                    msg.put("type", "VISION_IMAGE");
                    msg.put("imageBase64", base64Image);
                    msg.put("question", question);
                    msg.put("user", "");
                    svc.wsClient.send(msg.toString());
                    Log.i("LeaCamera", "✅ Image envoyée au serveur (" + base64Image.length() + " chars)");

                    new Handler(Looper.getMainLooper()).post(() ->
                        LeaNovaModeActivity.updateResult("👁️ Léa analyse l'image...")
                    );
                } else {
                    Log.w("LeaCamera", "⚠️ WebSocket non disponible");
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "Connexion Léa indisponible", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("LeaCamera", "❌ Erreur traitement image : " + e.getMessage());
            } finally {
                // Supprimer le fichier temporaire
                try {
                    new File(photoUri.getPath()).delete();
                } catch (Exception ignored) {}
            }
            new Handler(Looper.getMainLooper()).post(this::finish);
        }).start();
    }
}
