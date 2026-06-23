package com.flolov42.lea_v3.plus.premium.skills;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.app.NotificationCompat;
import org.json.*;
import java.text.*;
import java.util.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

public class LeaSkillPlantCareActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int GRN  = 0xFF4CAF50;
    private static final int RED  = 0xFFF44336;
    private static final int ORG  = 0xFFFF9800;
    private static final int DIM  = 0xFF7BB8CC;

    private static final String PREFS_KEY   = "lea_plants";
    private static final String CHANNEL_ID  = "lea_plant_care";

    private LinearLayout plantContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LeaFeatureDetailActivity.applyImmersive(this);
        prefs = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        sv.addView(root);

        // Header
        LinearLayout header = rowH();
        header.setPadding(dp(16), dp(16), dp(16), dp(8));
        Button back = new Button(this); back.setText("←");
        back.setBackgroundColor(Color.TRANSPARENT); back.setTextColor(CYAN); back.setTextSize(18);
        back.setOnClickListener(v -> finish()); header.addView(back);
        TextView title = new TextView(this); title.setText("🌱 Plant Care");
        title.setTextColor(GRN); title.setTextSize(20); title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(8), dp(8), 0, 0); header.addView(title);
        root.addView(header);

        TextView sub = new TextView(this);
        sub.setText("Suivi de l'arrosage de tes plantes");
        sub.setTextColor(DIM); sub.setTextSize(13);
        sub.setPadding(dp(20), 0, dp(20), dp(12)); root.addView(sub);

        // Bouton ajouter
        Button addBtn = new Button(this); addBtn.setText("+ Ajouter une plante");
        addBtn.setBackgroundColor(GRN); addBtn.setTextColor(0xFF011627);
        addBtn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.setMargins(dp(16), 0, dp(16), dp(8));
        addBtn.setLayoutParams(alp);
        addBtn.setOnClickListener(v -> showAddPlantDialog());
        root.addView(addBtn);

        // Container plantes
        plantContainer = new LinearLayout(this);
        plantContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(plantContainer);

        setContentView(sv);
        createNotifChannel();
        checkOverduePlants();
        refreshPlants();
    }

    private void refreshPlants() {
        plantContainer.removeAllViews();
        JSONObject allPlants = loadPlants();
        if (allPlants.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Aucune plante ajoutée.\nAjoute ta première plante !");
            empty.setTextColor(DIM); empty.setTextSize(14);
            empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            empty.setPadding(dp(20), dp(24), dp(20), dp(16));
            plantContainer.addView(empty); return;
        }
        Iterator<String> keys = allPlants.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject plant = allPlants.optJSONObject(key);
            if (plant != null) plantContainer.addView(buildPlantCard(key, plant));
        }
    }

    private View buildPlantCard(String plantId, JSONObject plant) {
        String name    = plant.optString("name", "Plante");
        String emoji   = plant.optString("emoji", "🌿");
        int freqDays   = plant.optInt("freqDays", 3);
        long lastWater = plant.optLong("lastWatered", 0);

        long nowMs    = System.currentTimeMillis();
        long daysSince = (nowMs - lastWater) / (1000 * 60 * 60 * 24);
        boolean needsWater = daysSince >= freqDays;
        long daysLeft = Math.max(0, freqDays - daysSince);

        LinearLayout card = card();

        LinearLayout row = rowH();
        TextView emojiTv = new TextView(this);
        emojiTv.setText(emoji); emojiTv.setTextSize(36);
        emojiTv.setPadding(0, 0, dp(12), 0); row.addView(emojiTv);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView nameTv = new TextView(this);
        nameTv.setText(name); nameTv.setTextColor(CYAN);
        nameTv.setTextSize(16); nameTv.setTypeface(null, Typeface.BOLD);
        info.addView(nameTv);

        TextView freqTv = new TextView(this);
        freqTv.setText("Arrosage: tous les " + freqDays + " jours");
        freqTv.setTextColor(DIM); freqTv.setTextSize(12);
        info.addView(freqTv);

        TextView statusTv = new TextView(this);
        if (needsWater) {
            statusTv.setText("💧 Arrosage requis! (" + daysSince + " j. sans eau)");
            statusTv.setTextColor(RED);
        } else {
            statusTv.setText("✓ OK — prochain arrosage dans " + daysLeft + " j.");
            statusTv.setTextColor(GRN);
        }
        statusTv.setTextSize(13); info.addView(statusTv);
        row.addView(info);
        card.addView(row);

        // Dernière arrosage
        if (lastWater > 0) {
            TextView lastTv = new TextView(this);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
            lastTv.setText("Dernier arrosage: " + sdf.format(new Date(lastWater)));
            lastTv.setTextColor(DIM); lastTv.setTextSize(12);
            lastTv.setPadding(0, dp(6), 0, 0); card.addView(lastTv);
        }

        // Boutons
        LinearLayout btnRow = rowH();
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(-1, -2);
        btnRowLp.setMargins(0, dp(8), 0, 0); btnRow.setLayoutParams(btnRowLp);

        Button waterBtn = new Button(this); waterBtn.setText("💧 Arrosé!");
        waterBtn.setBackgroundColor(needsWater ? 0xFF1565C0 : 0xFF0D3050);
        waterBtn.setTextColor(needsWater ? Color.WHITE : DIM);
        waterBtn.setTypeface(null, Typeface.BOLD);
        waterBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        waterBtn.setOnClickListener(v -> {
            updateLastWatered(plantId);
            Toast.makeText(this, name + " arrosée! 💧", Toast.LENGTH_SHORT).show();
            refreshPlants();
        });
        btnRow.addView(waterBtn);

        Button delBtn = new Button(this); delBtn.setText("🗑");
        delBtn.setBackgroundColor(Color.TRANSPARENT); delBtn.setTextColor(RED);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(48), -2);
        dlp.setMargins(dp(8), 0, 0, 0); delBtn.setLayoutParams(dlp);
        delBtn.setOnClickListener(v -> {
            deletePlant(plantId);
            Toast.makeText(this, name + " supprimée", Toast.LENGTH_SHORT).show();
            refreshPlants();
        });
        btnRow.addView(delBtn);
        card.addView(btnRow);

        return card;
    }

    private void showAddPlantDialog() {
        String[] plantTypes = {
            "🌵 Cactus (7j)", "🌿 Fougère (2j)", "🌺 Orchidée (5j)",
            "🪴 Pothos (4j)", "🌱 Herbes (1j)", "🌳 Palmier (6j)",
            "🌸 Lavande (4j)", "🍀 Basilic (2j)", "Autre (personnalisé)"
        };
        String[] emojis  = {"🌵","🌿","🌺","🪴","🌱","🌳","🌸","🍀","🌻"};
        int[]    freqs   = {7, 2, 5, 4, 1, 6, 4, 2, 3};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choisir le type de plante");
        builder.setItems(plantTypes, (d, idx) -> {
            if (idx == plantTypes.length - 1) {
                showCustomPlantDialog();
            } else {
                String name  = plantTypes[idx].substring(3);
                int dotIdx   = name.indexOf('(');
                name = dotIdx > 0 ? name.substring(0, dotIdx).trim() : name.trim();
                addPlant(name, emojis[idx], freqs[idx]);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showCustomPlantDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(8));

        EditText nameEt = new EditText(this);
        nameEt.setHint("Nom de la plante"); nameEt.setTextColor(Color.WHITE);
        nameEt.setHintTextColor(DIM); form.addView(nameEt);

        EditText freqEt = new EditText(this);
        freqEt.setHint("Fréquence d'arrosage (jours)");
        freqEt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        freqEt.setTextColor(Color.WHITE); freqEt.setHintTextColor(DIM);
        form.addView(freqEt);

        new AlertDialog.Builder(this).setTitle("Plante personnalisée")
            .setView(form)
            .setPositiveButton("Ajouter", (d, w) -> {
                String name = nameEt.getText().toString().trim();
                if (name.isEmpty()) return;
                int freq = 3;
                try { freq = Integer.parseInt(freqEt.getText().toString().trim()); } catch (Exception ignored) {}
                addPlant(name, "🌻", freq);
            })
            .setNegativeButton("Annuler", null).show();
    }

    // ── Persistance SharedPreferences JSON ───────────────────────────────────
    private JSONObject loadPlants() {
        try { return new JSONObject(prefs.getString("plants", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }
    private void addPlant(String name, String emoji, int freqDays) {
        JSONObject all = loadPlants();
        try {
            JSONObject plant = new JSONObject();
            plant.put("name", name); plant.put("emoji", emoji);
            plant.put("freqDays", freqDays); plant.put("lastWatered", 0);
            all.put("plant_" + System.currentTimeMillis(), plant);
            prefs.edit().putString("plants", all.toString()).apply();
            Toast.makeText(this, name + " ajoutée!", Toast.LENGTH_SHORT).show();
            refreshPlants();
        } catch (Exception ignored) {}
    }
    private void updateLastWatered(String plantId) {
        JSONObject all = loadPlants();
        try {
            JSONObject plant = all.optJSONObject(plantId);
            if (plant != null) { plant.put("lastWatered", System.currentTimeMillis()); }
            prefs.edit().putString("plants", all.toString()).apply();
        } catch (Exception ignored) {}
    }
    private void deletePlant(String plantId) {
        JSONObject all = loadPlants();
        all.remove(plantId);
        prefs.edit().putString("plants", all.toString()).apply();
    }

    private void checkOverduePlants() {
        JSONObject allPlants = loadPlants();
        if (allPlants.length() == 0) return;
        List<String> overdue = new ArrayList<>();
        Iterator<String> keys = allPlants.keys();
        while (keys.hasNext()) {
            JSONObject p = allPlants.optJSONObject(keys.next());
            if (p == null) continue;
            int freqDays  = p.optInt("freqDays", 3);
            long lastWater = p.optLong("lastWatered", 0);
            long daysSince = (System.currentTimeMillis() - lastWater) / (1000 * 60 * 60 * 24);
            if (daysSince >= freqDays) overdue.add(p.optString("name", "Plante"));
        }
        if (overdue.isEmpty()) return;
        String msg = overdue.size() == 1
            ? overdue.get(0) + " a besoin d'eau 💧"
            : overdue.size() + " plantes ont besoin d'eau 💧";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌱 Plant Care — Arrosage requis !")
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        nm.notify(8001, n.build());
    }

    private void createNotifChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Plant Care", NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL); c.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(6), dp(12), dp(6));
        c.setLayoutParams(lp); c.setPadding(dp(16), dp(14), dp(16), dp(14));
        return c;
    }
    private LinearLayout rowH() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        r.setGravity(Gravity.CENTER_VERTICAL); return r;
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
