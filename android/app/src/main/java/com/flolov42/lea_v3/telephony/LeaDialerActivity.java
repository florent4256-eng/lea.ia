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


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LeaDialerActivity extends Activity {

    private String currentNumber = "";
    private TextView numberDisplay;

    // Les conteneurs des différents onglets
    private LinearLayout keypadContainer;
    private ScrollView recentsContainer;
    private ScrollView contactsContainer;
    private ScrollView blockedContainer;
    private ScrollView voicemailContainer;
    private android.media.MediaPlayer vmPlayer;
    private final android.os.Handler vmHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeaFeatureDetailActivity.applyImmersive(this);
        purgerCorbeille();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 👑 RACINE (Plein écran)
        RelativeLayout rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.parseColor("#000814"));
        rootLayout.setTag("dialer_root");

        // 🌌 EN-TÊTE
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(50, 60, 30, 40);
        topBar.setId(View.generateViewId());
        topBar.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        
        TextView title = new TextView(this);
        title.setText("Téléphone LÉA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        title.getPaint().setFakeBoldText(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        title.setLayoutParams(titleParams);
        
        // ⋮ Bouton menu paramètres
        TextView menuBtn = new TextView(this);
        menuBtn.setText("⋮");
        menuBtn.setTextColor(Color.WHITE);
        menuBtn.setTextSize(28f);
        menuBtn.setPadding(30, 10, 30, 10);
        menuBtn.setOnClickListener(v -> ouvrirMenuParametres());

        // ✕ Bouton fermer (revient à Léa)
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(26f);
        closeBtn.setPadding(30, 10, 20, 10);
        closeBtn.setOnClickListener(v -> finish());

        TextView addBtn = new TextView(this);
        addBtn.setText("+");
        addBtn.setTextColor(Color.WHITE);
        addBtn.setTextSize(30f);
        addBtn.setPadding(30, 6, 30, 6);
        addBtn.setOnClickListener(v -> afficherNouveauContact());

        topBar.addView(title);
        topBar.addView(addBtn);
        topBar.addView(menuBtn);
        topBar.addView(closeBtn);
        rootLayout.addView(topBar);

        // 📱 CONTENEUR PRINCIPAL (Prend la place entre l'en-tête et la barre du bas)
        FrameLayout mainContent = new FrameLayout(this);
        RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        contentParams.addRule(RelativeLayout.BELOW, topBar.getId());
        contentParams.bottomMargin = 200; // Place pour la barre de navigation
        mainContent.setLayoutParams(contentParams);

        // Initialisation des vues
        keypadContainer = buildKeypad();
        recentsContainer = buildRecents();
        contactsContainer = buildContacts();
        blockedContainer = buildBlockedNumbers();
        voicemailContainer = buildVoicemail();

        mainContent.addView(keypadContainer);
        mainContent.addView(recentsContainer);
        mainContent.addView(contactsContainer);
        mainContent.addView(blockedContainer);
        //mainContent.addView(voicemailContainer);
        rootLayout.addView(mainContent);

        // 🧭 BARRE DE NAVIGATION INFÉRIEURE
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER);
        navBar.setBackgroundColor(Color.parseColor("#0A1020"));
        RelativeLayout.LayoutParams navParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 200);
        navParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        navBar.setLayoutParams(navParams);

        navBar.addView(createNavButton("Clavier", "⌨️", () -> showTab("keypad")));
        navBar.addView(createNavButton("Récents", "🕒", () -> showTab("recents")));
        navBar.addView(createNavButton("Contacts", "👥", () -> showTab("contacts")));
        navBar.addView(createNavButton("Mess. vocal", "📼", () -> ouvrirRepondeurNatif()));

        rootLayout.addView(navBar);
        setContentView(rootLayout);

        // Afficher le clavier par défaut
        showTab("keypad");
    }

    // ==========================================
    // 🎛️ 1. LE PAVÉ NUMÉRIQUE (KEYPAD)
    // ==========================================
    private LinearLayout buildKeypad() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Affichage du numéro
        RelativeLayout displayArea = new RelativeLayout(this);
        displayArea.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 250));
        
        numberDisplay = new TextView(this);
        numberDisplay.setTextColor(Color.WHITE);
        numberDisplay.setTextSize(40f);
        numberDisplay.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams numParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        numParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        numberDisplay.setLayoutParams(numParams);
        
        // Bouton effacer
        TextView deleteBtn = new TextView(this);
        deleteBtn.setText("⌫");
        deleteBtn.setTextColor(Color.parseColor("#888888"));
        deleteBtn.setTextSize(30f);
        deleteBtn.setPadding(40, 40, 40, 40);
        RelativeLayout.LayoutParams delParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        delParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        delParams.addRule(RelativeLayout.CENTER_VERTICAL);
        deleteBtn.setLayoutParams(delParams);
        deleteBtn.setOnClickListener(v -> {
            if (currentNumber.length() > 0) {
                currentNumber = currentNumber.substring(0, currentNumber.length() - 1);
                numberDisplay.setText(currentNumber);
            }
        });

        displayArea.addView(numberDisplay);
        displayArea.addView(deleteBtn);
        layout.addView(displayArea);

        // Grille des touches
        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"*", "0", "#"}
        };

        for (String[] row : keys) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            rowLayout.setPadding(0, 10, 0, 10);
            
            for (String key : row) {
                TextView btn = new TextView(this);
                btn.setText(key);
                btn.setTextColor(Color.WHITE);
                btn.setTextSize(36f);
                btn.setGravity(Gravity.CENTER);
                
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.parseColor("#1AFFFFFF"));
                btn.setBackground(bg);
                
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(200, 200);
                btnParams.setMargins(20, 20, 20, 20);
                btn.setLayoutParams(btnParams);
                
                btn.setOnClickListener(v -> {
                    currentNumber += key;
                    numberDisplay.setText(currentNumber);
                });
                rowLayout.addView(btn);
            }
            layout.addView(rowLayout);
        }

        // Bouton d'appel vert
        TextView callBtn = new TextView(this);
        callBtn.setText("📞");
        callBtn.setTextSize(36f);
        callBtn.setGravity(Gravity.CENTER);
        GradientDrawable callBg = new GradientDrawable();
        callBg.setShape(GradientDrawable.OVAL);
        callBg.setColor(Color.parseColor("#22C55E"));
        callBtn.setBackground(callBg);
        LinearLayout.LayoutParams callParams = new LinearLayout.LayoutParams(220, 220);
        callParams.setMargins(0, 50, 0, 0);
        callBtn.setLayoutParams(callParams);
        
        callBtn.setOnClickListener(v -> {
            if (!currentNumber.isEmpty()) {
                placeNativeCall(currentNumber);
            }
        });

        layout.addView(callBtn);
        return layout;
    }

    // ==========================================
    // 🕒 2. LE JOURNAL D'APPELS (RÉCENTS)
    // ==========================================
    @SuppressLint("Range")
    private ScrollView buildRecents() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40, 20, 40, 20);

        // Structure pour trier tous les appels par date
        java.util.List<long[]> ordre = new java.util.ArrayList<>(); // [index, date]
        final java.util.List<String[]> entrees = new java.util.ArrayList<>(); // [nom/num, sousTexte, couleur, numero]

        // 1) JOURNAL SYSTÈME
        try {
            Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, null, null, null,
                    CallLog.Calls.DATE + " DESC LIMIT 50");
            if (cursor != null) {
                int numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                while (cursor.moveToNext()) {
                    String num = cursor.getString(numIdx);
                    int type = cursor.getInt(typeIdx);
                    long date = cursor.getLong(dateIdx);
                    String cached = cursor.getString(nameIdx);
                    String titre;
                    if (cached != null && !cached.isEmpty()) {
                        titre = cached;
                    } else if (num != null && !num.isEmpty()) {
                        titre = num;
                    } else {
                        titre = "Numéro inconnu";
                    }

                    String typeTxt;
                    String couleur = "#FFFFFF";
                    if (type == CallLog.Calls.INCOMING_TYPE) typeTxt = "Entrant";
                    else if (type == CallLog.Calls.OUTGOING_TYPE) typeTxt = "Sortant";
                    else if (type == CallLog.Calls.MISSED_TYPE) { typeTxt = "Manqué"; couleur = "#EF4444"; }
                    else typeTxt = "Appel";

                    String heure = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(new Date(date));
                    entrees.add(new String[]{titre, typeTxt + " · " + heure, couleur, num});
                    ordre.add(new long[]{entrees.size() - 1, date});
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            TextView err = new TextView(this);
            err.setText("Permission journal d'appels manquante.");
            err.setTextColor(Color.RED);
            list.addView(err);
        }

        // 2) NOTRE JOURNAL DES BLOQUÉS
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> journalBloque = prefs.getStringSet("blocked_call_log", new HashSet<>());
        for (String e : journalBloque) {
            String[] p = e.split("\\|");
            if (p.length >= 4) {
                String num = p[0];
                String nom = p[1];
                long date = Long.parseLong(p[2]);
                String raison = p[3];

                String titre;
                if (nom != null && !nom.isEmpty() && !nom.equals("Numéro inconnu")) {
                    titre = nom;
                } else if ("inconnu".equals(raison)) {
                    titre = "Numéro inconnu";
                } else {
                    titre = num;
                }
                String heure = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(new Date(date));
                entrees.add(new String[]{titre, "🚫 Bloqué · " + heure, "#F59E0B", num});
                ordre.add(new long[]{entrees.size() - 1, date});
            }
        }

        // 3) TRI PAR DATE (du plus récent au plus ancien)
        ordre.sort((a, b) -> Long.compare(b[1], a[1]));

        if (ordre.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun appel récent.");
            empty.setTextColor(Color.parseColor("#888888"));
            list.addView(empty);
        } else {
            for (long[] o : ordre) {
                String[] info = entrees.get((int) o[0]);
                list.addView(createListItem(info[0], info[1], info[2], info[3]));
            }
        }

        scroll.addView(list);
        return scroll;
    }

    // ==========================================
    // 👥 3. LE RÉPERTOIRE (CONTACTS)
    // ==========================================
    @SuppressLint("Range")
    private ScrollView buildContacts() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40, 20, 40, 20);

        // Deux groupes : contacts normaux et services opérateur
        java.util.List<String[]> contactsNormaux = new java.util.ArrayList<>(); // [nom, numero]
        java.util.List<String[]> services = new java.util.ArrayList<>();

        try {
            Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                String lastContactName = "";
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if (name == null) name = "";
                    if (number == null) number = "";

                    if (!name.equals(lastContactName)) {
                        if (estServiceOperateur(name, number)) {
                            services.add(new String[]{name, number});
                        } else {
                            contactsNormaux.add(new String[]{name, number});
                        }
                        lastContactName = name;
                    }
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            TextView err = new TextView(this);
            err.setText("Permission contacts manquante.");
            err.setTextColor(Color.RED);
            list.addView(err);
            scroll.addView(list);
            return scroll;
        }

        // === CONTACTS NORMAUX avec séparateurs de lettres ===
        String lettreActuelle = "";
        for (String[] c : contactsNormaux) {
            String nom = c[0];
            String num = c[1];
            String premiereLettre = (nom != null && !nom.isEmpty())
                    ? nom.substring(0, 1).toUpperCase() : "#";
            // Si ce n'est pas une lettre A-Z, on regroupe sous "#"
            if (!premiereLettre.matches("[A-Z]")) premiereLettre = "#";

            if (!premiereLettre.equals(lettreActuelle)) {
                lettreActuelle = premiereLettre;
                list.addView(creerSeparateurLettre(premiereLettre));
            }

            final String fNom = nom;
            final String fNum = num;
            LinearLayout item = createListItem(nom, num, "#FFFFFF", num);
            item.setOnClickListener(v -> afficherFicheContact(fNom, fNum));
            list.addView(item);
        }

        // === SERVICES OPÉRATEUR tout en bas ===
        if (!services.isEmpty()) {
            list.addView(creerSeparateurLettre("Services"));
            for (String[] s : services) {
                final String fNom = s[0];
                final String fNum = s[1];
                LinearLayout item = createListItem(
                        (fNom != null && !fNom.isEmpty()) ? fNom : fNum, fNum, "#00E5FF", fNum);
                item.setOnClickListener(v -> afficherFicheContact(fNom, fNum));
                list.addView(item);
            }
        }

        scroll.addView(list);
        return scroll;
    }

    /** Vrai si le numéro ressemble à un service opérateur (numéro court). */
    private boolean estServiceOperateur(String nom, String numero) {
        // Convention : tout contact dont le nom commence par "." est un service
        if (nom != null && nom.trim().startsWith(".")) {
            return true;
        }
        // Sinon, on détecte aussi les numéros courts (vrais services opérateur)
        if (numero == null) return false;
        String chiffres = numero.replaceAll("[^0-9]", "");
        return chiffres.length() >= 3 && chiffres.length() <= 6;
    }

    /** Crée un en-tête de section (lettre ou "Services"). */
    private TextView creerSeparateurLettre(String texte) {
        TextView sep = new TextView(this);
        sep.setText(texte);
        sep.setTextColor(Color.parseColor("#00E5FF"));
        sep.setTextSize(15f);
        sep.getPaint().setFakeBoldText(true);
        sep.setPadding(20, 40, 0, 16);
        return sep;
    }

    // ==========================================
    // 🛡️ 4. LA LISTE NOIRE (LÉA PROTECT)
    // ==========================================
    private ScrollView buildBlockedNumbers() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40, 20, 40, 20);

        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> blockedSet = prefs.getStringSet("blocked_numbers", new HashSet<>());

        TextView info = new TextView(this);
        info.setText("Numéros bloqués par le pare-feu :");
        info.setTextColor(Color.parseColor("#00E5FF"));
        info.setPadding(0, 0, 0, 40);
        list.addView(info);

        if (blockedSet.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun numéro bloqué.");
            empty.setTextColor(Color.parseColor("#888888"));
            list.addView(empty);
        } else {
            for (String num : blockedSet) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(20, 30, 20, 30);
                row.setBackgroundColor(Color.parseColor("#11FFFFFF"));
                
                String nomContact = chercherNomContact(num);
                TextView numText = new TextView(this);
                if (nomContact != null && !nomContact.isEmpty()) {
                    numText.setText(nomContact + "\n" + num);
                } else {
                    numText.setText(num);
                }
                numText.setTextColor(Color.WHITE);
                numText.setTextSize(18f);
                numText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView unblockBtn = new TextView(this);
                unblockBtn.setText("Débloquer");
                unblockBtn.setTextColor(Color.parseColor("#EF4444"));
                unblockBtn.setOnClickListener(v -> {
                    blockedSet.remove(num);
                    prefs.edit().putStringSet("blocked_numbers", blockedSet).apply();
                    Toast.makeText(this, "Numéro débloqué", Toast.LENGTH_SHORT).show();
                    // Relancer l'activité pour rafraîchir
                    recreate();
                });

                row.addView(numText);
                row.addView(unblockBtn);
                list.addView(row);
            }
        }
        scroll.addView(list);
        return scroll;
    }

    @SuppressLint("Range")
    private String chercherNomContact(String numero) {
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero));
            Cursor c = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (c != null) {
                String nom = null;
                if (c.moveToFirst()) {
                    nom = c.getString(0);
                }
                c.close();
                return nom;
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ==========================================
    // ⚙️ OUTILS ET LOGIQUE COMMUNE
    // ==========================================

    // ==========================================
    // ⚙️ MENU PARAMÈTRES (croix + 3 points)
    // ==========================================

    private void ouvrirMenuParametres() {
        String[] options = {
                "🛡️ Numéros bloqués",
                "🚫 Bloquer les inconnus",
                "🖼️ Arrière-plan téléphonique",
                "🔔 Sonnerie téléphone",
                "🗑️ Corbeille des contacts",
                "⬇️ Importer les numéros bloqués du système"
        };
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Paramètres");
        b.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showTab("blocked");
            } else if (which == 1) {
                reglageBloquerInconnus();
            } else if (which == 2) {
                reglageArrierePlan();
            } else if (which == 3) {
                reglageSonnerie();
            } else if (which == 4) {
                afficherCorbeille();
            } else if (which == 5) {
                importerBloquesSysteme();
            }
        });
        b.show();
    }

    @SuppressLint("Range")
    private void importerBloquesSysteme() {
        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle("Importer les numéros bloqués");
        confirm.setMessage("Léa va lire la liste de blocage du système (celle de l'app téléphone Samsung) et l'ajouter à sa propre liste. Continuer ?");
        confirm.setPositiveButton("Importer", (d, w) -> {
            int nbImportes = 0;
            try {
                SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
                Set<String> blockedSet = new HashSet<>(prefs.getStringSet("blocked_numbers", new HashSet<>()));
                int avant = blockedSet.size();

                Uri uri = android.provider.BlockedNumberContract.BlockedNumbers.CONTENT_URI;
                Cursor c = getContentResolver().query(uri,
                        new String[]{android.provider.BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                        null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String num = c.getString(0);
                        if (num != null && !num.isEmpty()) {
                            blockedSet.add(num);
                        }
                    }
                    c.close();
                }

                prefs.edit().putStringSet("blocked_numbers", blockedSet).apply();
                nbImportes = blockedSet.size() - avant;

                Toast.makeText(this,
                        nbImportes + " numéro(s) importé(s) dans Léa",
                        Toast.LENGTH_LONG).show();

                // Reconstruire et rafraîchir l'onglet bloqués
                rafraichirOngletBloques();

            } catch (SecurityException e) {
                Toast.makeText(this, "Léa doit être l'app téléphone par défaut pour lire la liste système.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Import impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        confirm.setNegativeButton("Annuler", null);
        confirm.show();
    }

    private void rafraichirOngletBloques() {
        // On retire l'ancien conteneur bloqués et on le reconstruit à jour
        FrameLayout parent = (FrameLayout) blockedContainer.getParent();
        if (parent != null) {
            parent.removeView(blockedContainer);
        }
        blockedContainer = buildBlockedNumbers();
        if (parent != null) {
            parent.addView(blockedContainer);
        }
        showTab("blocked");
    }

    private void rafraichirOngletContacts() {
        FrameLayout parent = (FrameLayout) contactsContainer.getParent();
        if (parent != null) {
            parent.removeView(contactsContainer);
        }
        contactsContainer = buildContacts();
        if (parent != null) {
            parent.addView(contactsContainer);
        }
        showTab("contacts");
    }

    private void reglageBloquerInconnus() {
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        boolean actif = prefs.getBoolean("block_unknown", false);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Bloquer les inconnus");
        b.setMessage(actif
                ? "Le blocage des numéros inconnus est ACTIVÉ.\n\nLes appels de numéros absents de tes contacts seront rejetés automatiquement."
                : "Le blocage des numéros inconnus est DÉSACTIVÉ.\n\nTous les appels passent normalement.");
        b.setPositiveButton(actif ? "Désactiver" : "Activer", (d, w) -> {
            prefs.edit().putBoolean("block_unknown", !actif).apply();
            Toast.makeText(this, !actif ? "Blocage des inconnus activé" : "Blocage des inconnus désactivé", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Annuler", null);
        b.show();
    }

    private void reglageArrierePlan() {
        String[] options = {
                "Choisir une photo ou une vidéo",
                "Remettre le fond galaxie par défaut"
        };
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Arrière-plan téléphonique");
        b.setItems(options, (dialog, which) -> {
            if (which == 0) {
                choisirMediaFond();
            } else {
                SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
                prefs.edit().putString("call_bg_type", "none").putString("call_bg_uri", "").apply();
                Toast.makeText(this, "Fond galaxie rétabli", Toast.LENGTH_SHORT).show();
            }
        });
        b.show();
    }

    private void reglageSonnerie() {
        String[] options = {
                "Sonneries du téléphone",
                "Ma musique perso (fichier audio)",
                "Remettre la sonnerie par défaut"
        };
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Sonnerie téléphone");
        b.setItems(options, (dialog, which) -> {
            if (which == 0) {
                choisirSonnerieSysteme();
            } else if (which == 1) {
                choisirMusiquePerso();
            } else {
                SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
                prefs.edit().putString("ringtone_uri", "").apply();
                Toast.makeText(this, "Sonnerie par défaut rétablie", Toast.LENGTH_SHORT).show();
            }
        });
        b.show();
    }

    private static final int CODE_SONNERIE_SYSTEME = 9200;
    private static final int CODE_MUSIQUE_PERSO = 9201;

    private void choisirSonnerieSysteme() {
        Intent intent = new Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
                android.media.RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Choisir une sonnerie");
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        // Pré-sélectionner la sonnerie actuelle si déjà définie
        try {
            SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
            String actuelle = prefs.getString("ringtone_uri", "");
            if (actuelle != null && !actuelle.isEmpty()) {
                intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(actuelle));
            }
        } catch (Exception ignored) { }
        try {
            startActivityForResult(intent, CODE_SONNERIE_SYSTEME);
        } catch (Exception e) {
            Toast.makeText(this, "Sélecteur de sonnerie indisponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void choisirMusiquePerso() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        try {
            startActivityForResult(intent, CODE_MUSIQUE_PERSO);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir les fichiers audio", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int CODE_CHOIX_FOND = 9100;

    private void choisirMediaFond() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        try {
            startActivityForResult(intent, CODE_CHOIX_FOND);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir le sélecteur", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_CHOIX_FOND && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            // Garder l'accès au fichier dans le temps
            try {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }

            // Déterminer si c'est une photo ou une vidéo
            String type = getContentResolver().getType(uri);
            String bgType = "photo";
            if (type != null && type.startsWith("video")) {
                bgType = "video";
            }

            SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("call_bg_type", bgType)
                    .putString("call_bg_uri", uri.toString())
                    .apply();

            Toast.makeText(this, "Arrière-plan enregistré ✔", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == CODE_SONNERIE_SYSTEME && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
            if (uri != null) {
                prefs.edit().putString("ringtone_uri", uri.toString()).apply();
                Toast.makeText(this, "Sonnerie enregistrée ✔", Toast.LENGTH_SHORT).show();
            } else {
                // L'utilisateur a choisi "Par défaut"
                prefs.edit().putString("ringtone_uri", "").apply();
                Toast.makeText(this, "Sonnerie par défaut", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == CODE_MUSIQUE_PERSO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) { }
                SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
                prefs.edit().putString("ringtone_uri", uri.toString()).apply();
                Toast.makeText(this, "Musique perso enregistrée ✔", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==========================================
    // 📼 MESSAGERIE VOCALE
    // ==========================================

    /** Dossier où vivent les messages vocaux (le serveur Léa déposera ici). */
    private java.io.File dossierVoicemails() {
        java.io.File dir = new java.io.File(getFilesDir(), "voicemails");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void rafraichirOngletVoicemail() {
        FrameLayout parent = (FrameLayout) voicemailContainer.getParent();
        if (parent != null) {
            parent.removeView(voicemailContainer);
        }
        voicemailContainer = buildVoicemail();
        if (parent != null) {
            parent.addView(voicemailContainer);
        }
        showTab("voicemail");
    }

    @SuppressLint("Range")
    private ScrollView buildVoicemail() {
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40, 20, 40, 20);

        // Bouton TEST (à retirer plus tard) : crée un faux message pour tester le lecteur
        TextView testBtn = new TextView(this);
        testBtn.setText("➕ Créer un message test");
        testBtn.setTextColor(Color.parseColor("#00E5FF"));
        testBtn.setTextSize(14f);
        testBtn.setPadding(20, 20, 20, 30);
        testBtn.setOnClickListener(v -> creerMessageTest());
        list.addView(testBtn);

        java.io.File[] fichiers = dossierVoicemails().listFiles((d, n) ->
                n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".wav") || n.endsWith(".3gp") || n.endsWith(".amr"));

        if (fichiers == null || fichiers.length == 0) {
            TextView vide = new TextView(this);
            vide.setText("Aucun message vocal.");
            vide.setTextColor(Color.parseColor("#888888"));
            vide.setPadding(20, 40, 20, 0);
            list.addView(vide);
            scroll.addView(list);
            return scroll;
        }

        // Trier du plus récent au plus ancien
        java.util.Arrays.sort(fichiers, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        for (java.io.File f : fichiers) {
            list.addView(creerCarteMessage(f));
        }

        scroll.addView(list);
        return scroll;
    }

    /**
     * Convention de nom de fichier : numero_nom_timestamp.ext
     * Exemple : 0762600453_Alexis_1717000000000.mp3
     * (le serveur Léa nommera ses fichiers comme ça)
     */
    private LinearLayout creerCarteMessage(final java.io.File fichier) {
        // Décoder le nom du fichier
        String base = fichier.getName();
        int point = base.lastIndexOf('.');
        if (point > 0) base = base.substring(0, point);
        String[] parts = base.split("_");
        final String numero = parts.length > 0 ? parts[0] : "Inconnu";
        String nom = parts.length > 1 ? parts[1] : "";
        long ts = fichier.lastModified();
        if (parts.length > 2) {
            try { ts = Long.parseLong(parts[2]); } catch (Exception ignored) { }
        }
        final String nomAffiche = (nom != null && !nom.isEmpty()) ? nom : numero;

        // Durée du fichier
        final int dureeMs = dureeFichier(fichier);
        String dureeTxt = formaterDuree(dureeMs);
        String heureTxt = new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date(ts));

        // Carte
        LinearLayout carte = new LinearLayout(this);
        carte.setOrientation(LinearLayout.VERTICAL);
        carte.setPadding(40, 40, 40, 40);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#10FFFFFF"));
        bg.setCornerRadius(40f);
        carte.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = 30;
        carte.setLayoutParams(cp);

        TextView nomView = new TextView(this);
        nomView.setText(nomAffiche);
        nomView.setTextColor(Color.WHITE);
        nomView.setTextSize(20f);
        nomView.getPaint().setFakeBoldText(true);
        carte.addView(nomView);

        TextView infoView = new TextView(this);
        infoView.setText("📼 Message vocal, " + dureeTxt + "  ·  " + heureTxt);
        infoView.setTextColor(Color.parseColor("#AABBCC"));
        infoView.setTextSize(14f);
        infoView.setPadding(0, 8, 0, 20);
        carte.addView(infoView);

        // --- Ligne lecteur : play/pause | temps | barre | durée | HP | poubelle ---
        LinearLayout lecteur = new LinearLayout(this);
        lecteur.setOrientation(LinearLayout.HORIZONTAL);
        lecteur.setGravity(Gravity.CENTER_VERTICAL);

        final TextView playBtn = new TextView(this);
        playBtn.setText("▶");
        playBtn.setTextColor(Color.WHITE);
        playBtn.setTextSize(24f);
        playBtn.setPadding(0, 0, 24, 0);

        final TextView tempsActuel = new TextView(this);
        tempsActuel.setText("00:00");
        tempsActuel.setTextColor(Color.parseColor("#AABBCC"));
        tempsActuel.setTextSize(12f);

        final android.widget.SeekBar seek = new android.widget.SeekBar(this);
        seek.setMax(dureeMs > 0 ? dureeMs : 1);
        LinearLayout.LayoutParams seekP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        seekP.leftMargin = 16; seekP.rightMargin = 16;
        seek.setLayoutParams(seekP);

        final TextView dureeView = new TextView(this);
        dureeView.setText(dureeTxt);
        dureeView.setTextColor(Color.parseColor("#AABBCC"));
        dureeView.setTextSize(12f);

        TextView hpBtn = new TextView(this);
        hpBtn.setText("🔊");
        hpBtn.setTextSize(20f);
        hpBtn.setPadding(24, 0, 16, 0);
        hpBtn.setOnClickListener(v -> basculerHautParleurVm());

        TextView poubelle = new TextView(this);
        poubelle.setText("🗑️");
        poubelle.setTextSize(20f);
        poubelle.setOnClickListener(v -> {
            arreterLecture();
            if (fichier.delete()) {
                Toast.makeText(this, "Message supprimé", Toast.LENGTH_SHORT).show();
                rafraichirOngletVoicemail();
            }
        });

        lecteur.addView(playBtn);
        lecteur.addView(tempsActuel);
        lecteur.addView(seek);
        lecteur.addView(dureeView);
        lecteur.addView(hpBtn);
        lecteur.addView(poubelle);
        carte.addView(lecteur);

        // Logique play/pause
        playBtn.setOnClickListener(v -> lireOuPause(fichier, playBtn, seek, tempsActuel));

        // Glisser la barre = déplacer la lecture
        seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean fromUser) {
                if (fromUser && vmPlayer != null) vmPlayer.seekTo(p);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });

        // --- Rangée d'actions : Appeler / Message / (liste) ---
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, 30, 0, 0);

        actions.addView(boutonFiche("📞", "Appeler", () -> placeNativeCall(numero)));
        actions.addView(boutonFiche("💬", "Message", () -> {
            try {
                Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numero));
                startActivity(sms);
            } catch (Exception e) {
                Toast.makeText(this, "Impossible d'ouvrir les messages", Toast.LENGTH_SHORT).show();
            }
        }));
        actions.addView(boutonFiche("👤", "Contact", () -> afficherFicheContact(nomAffiche, numero)));
        carte.addView(actions);

        return carte;
    }

    private void lireOuPause(java.io.File fichier, final TextView playBtn,
                             final android.widget.SeekBar seek, final TextView tempsActuel) {
        try {
            // Si on appuie sur le bouton du message déjà en lecture -> pause/reprise
            if (vmPlayer != null && vmEnCours != null && vmEnCours.equals(fichier.getAbsolutePath())) {
                if (vmPlayer.isPlaying()) {
                    vmPlayer.pause();
                    playBtn.setText("▶");
                } else {
                    vmPlayer.start();
                    playBtn.setText("⏸");
                    suivreProgression(seek, tempsActuel, playBtn);
                }
                return;
            }

            // Sinon on arrête l'ancien et on lance le nouveau
            arreterLecture();
            vmPlayer = new android.media.MediaPlayer();
            vmPlayer.setDataSource(fichier.getAbsolutePath());
            vmPlayer.setAudioStreamType(vmHautParleur
                    ? android.media.AudioManager.STREAM_MUSIC
                    : android.media.AudioManager.STREAM_VOICE_CALL);
            vmPlayer.prepare();
            vmPlayer.start();
            vmEnCours = fichier.getAbsolutePath();
            vmPlayBtnActuel = playBtn;
            playBtn.setText("⏸");
            seek.setMax(vmPlayer.getDuration());
            suivreProgression(seek, tempsActuel, playBtn);

            vmPlayer.setOnCompletionListener(mp -> {
                playBtn.setText("▶");
                seek.setProgress(0);
                tempsActuel.setText("00:00");
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lecture impossible", Toast.LENGTH_SHORT).show();
        }
    }

    private String vmEnCours = null;
    private TextView vmPlayBtnActuel = null;
    private boolean vmHautParleur = false;

    private void suivreProgression(final android.widget.SeekBar seek,
                                   final TextView tempsActuel, final TextView playBtn) {
        vmHandler.post(new Runnable() {
            @Override public void run() {
                if (vmPlayer != null && vmPlayer.isPlaying()) {
                    int pos = vmPlayer.getCurrentPosition();
                    seek.setProgress(pos);
                    tempsActuel.setText(formaterDuree(pos));
                    vmHandler.postDelayed(this, 300);
                }
            }
        });
    }

    private void basculerHautParleurVm() {
        vmHautParleur = !vmHautParleur;
        Toast.makeText(this, vmHautParleur ? "Haut-parleur activé" : "Écouteur", Toast.LENGTH_SHORT).show();
        // Le changement s'appliquera à la prochaine lecture
    }

    private void arreterLecture() {
        try {
            if (vmPlayer != null) {
                if (vmPlayer.isPlaying()) vmPlayer.stop();
                vmPlayer.release();
            }
        } catch (Exception ignored) { }
        vmPlayer = null;
        vmEnCours = null;
        if (vmPlayBtnActuel != null) vmPlayBtnActuel.setText("▶");
    }

    private int dureeFichier(java.io.File f) {
        try {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            mmr.setDataSource(f.getAbsolutePath());
            String d = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            mmr.release();
            return d != null ? Integer.parseInt(d) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formaterDuree(int ms) {
        int totalSec = ms / 1000;
        return String.format(Locale.FRANCE, "%02d:%02d", totalSec / 60, totalSec % 60);
    }

    /** TEST : crée un petit fichier audio bidon pour vérifier l'interface. À retirer plus tard. */
    private void creerMessageTest() {
        try {
            // On enregistre 3 secondes depuis le micro pour avoir un vrai fichier lisible
            final String chemin = new java.io.File(dossierVoicemails(),
                    "0762600453_Alexis_" + System.currentTimeMillis() + ".m4a").getAbsolutePath();
            final android.media.MediaRecorder rec = new android.media.MediaRecorder();
            rec.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            rec.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            rec.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            rec.setOutputFile(chemin);
            rec.prepare();
            rec.start();
            Toast.makeText(this, "Enregistrement test... (3 s, parle !)", Toast.LENGTH_SHORT).show();
            vmHandler.postDelayed(() -> {
                try { rec.stop(); rec.release(); } catch (Exception ignored) { }
                Toast.makeText(this, "Message test créé ✔", Toast.LENGTH_SHORT).show();
                rafraichirOngletVoicemail();
            }, 3000);
        } catch (Exception e) {
            Toast.makeText(this, "Test impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showTab(String tabName) {
        keypadContainer.setVisibility(View.GONE);
        recentsContainer.setVisibility(View.GONE);
        contactsContainer.setVisibility(View.GONE);
        blockedContainer.setVisibility(View.GONE);
        voicemailContainer.setVisibility(View.GONE);

        if (tabName.equals("keypad")) keypadContainer.setVisibility(View.VISIBLE);
        if (tabName.equals("recents")) {
            // Rafraîchissement dynamique des récents
            ((FrameLayout) recentsContainer.getParent()).removeView(recentsContainer);
            recentsContainer = buildRecents();
            ((FrameLayout) keypadContainer.getParent()).addView(recentsContainer);
            recentsContainer.setVisibility(View.VISIBLE);
        }
        if (tabName.equals("contacts")) contactsContainer.setVisibility(View.VISIBLE);
        if (tabName.equals("blocked")) blockedContainer.setVisibility(View.VISIBLE);
        if (tabName.equals("voicemail")) voicemailContainer.setVisibility(View.VISIBLE);
    }

    private LinearLayout createNavButton(String label, String icon, Runnable action) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
        
        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(24f);
        iconView.setGravity(Gravity.CENTER);
        
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.parseColor("#888888"));
        labelView.setTextSize(12f);
        labelView.setGravity(Gravity.CENTER);

        btn.addView(iconView);
        btn.addView(labelView);
        btn.setOnClickListener(v -> action.run());
        return btn;
    }

    private LinearLayout createListItem(String titleText, String subText, String titleColor, String targetNumber) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 30, 20, 30);
        
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(Color.parseColor(titleColor));
        title.setTextSize(18f);
        title.getPaint().setFakeBoldText(true);

        TextView sub = new TextView(this);
        sub.setText(subText);
        sub.setTextColor(Color.parseColor("#888888"));
        sub.setTextSize(14f);

        textLayout.addView(title);
        textLayout.addView(sub);

        TextView callIcon = new TextView(this);
        callIcon.setText("📞");
        callIcon.setTextSize(24f);
        callIcon.setPadding(30, 0, 10, 0);
        callIcon.setOnClickListener(v -> placeNativeCall(targetNumber));

        row.addView(textLayout);
        row.addView(callIcon);

        // Ligne de séparation
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setStroke(1, Color.parseColor("#1AFFFFFF"));
        row.setBackground(border);

        return row;
    }

    // ==========================================
    // 👤 FICHE CONTACT (version simple)
    // ==========================================
    private void afficherFicheContact(final String nom, final String numero) {
        RelativeLayout overlay = new RelativeLayout(this);
        overlay.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(Color.parseColor("#000814"));

        // Bouton retour
        TextView back = new TextView(this);
        back.setText("‹  Retour");
        back.setTextColor(Color.WHITE);
        back.setTextSize(18f);
        back.setPadding(50, 60, 50, 40);
        back.setId(View.generateViewId());
        back.setOnClickListener(v -> {
            RelativeLayout root = (RelativeLayout) findViewById(android.R.id.content).getRootView()
                    .findViewWithTag("dialer_root");
            // on retire simplement l'overlay
            ((RelativeLayout) overlay.getParent()).removeView(overlay);
        });
        overlay.addView(back);

        // Carte du haut (avatar + nom + numéro)
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(40, 80, 40, 60);
        RelativeLayout.LayoutParams cardParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        cardParams.addRule(RelativeLayout.BELOW, back.getId());
        cardParams.topMargin = 60;
        card.setLayoutParams(cardParams);

        // Avatar rond avec l'initiale
        TextView avatar = new TextView(this);
        String initiale = (nom != null && !nom.isEmpty()) ? nom.substring(0, 1).toUpperCase() : "?";
        avatar.setText(initiale);
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(48f);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable avBg = new GradientDrawable();
        avBg.setShape(GradientDrawable.OVAL);
        avBg.setColor(Color.parseColor("#00E5FF"));
        avatar.setBackground(avBg);
        LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(220, 220);
        avatar.setLayoutParams(avParams);
        card.addView(avatar);

        TextView nameView = new TextView(this);
        nameView.setText(nom);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(30f);
        nameView.getPaint().setFakeBoldText(true);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, 40, 0, 10);
        card.addView(nameView);

        TextView numView = new TextView(this);
        numView.setText("Mobile  " + numero);
        numView.setTextColor(Color.parseColor("#AABBCC"));
        numView.setTextSize(18f);
        numView.setGravity(Gravity.CENTER);
        card.addView(numView);

        overlay.addView(card);

        // Rangée d'actions : Appeler / Message
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams actParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        actParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        actions.setLayoutParams(actParams);

        actions.addView(boutonFiche("📞", "Appeler", () -> placeNativeCall(numero)));
        actions.addView(boutonFiche("💬", "Message", () -> {
            try {
                Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numero));
                startActivity(sms);
            } catch (Exception e) {
                Toast.makeText(this, "Impossible d'ouvrir les messages", Toast.LENGTH_SHORT).show();
            }
        }));
        actions.addView(boutonFiche("✏️", "Modifier", () -> {
            try {
                Uri lookupUri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero));
                Cursor c = getContentResolver().query(lookupUri,
                        new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY},
                        null, null, null);
                if (c != null && c.moveToFirst()) {
                    long id = c.getLong(0);
                    String key = c.getString(1);
                    c.close();
                    Uri contactUri = ContactsContract.Contacts.getLookupUri(id, key);
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    intent.setData(contactUri);
                    startActivity(intent);
                } else {
                    if (c != null) c.close();
                    Toast.makeText(this, "Contact introuvable", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Impossible de modifier", Toast.LENGTH_SHORT).show();
            }
        }));
        actions.addView(boutonFiche("🚫", "Bloquer", () -> bloquerContact(numero)));
        actions.addView(boutonFiche("🗑️", "Supprimer", () -> confirmerSuppression(numero, overlay)));

        overlay.addView(actions);

        // On affiche l'overlay par-dessus tout
        ((RelativeLayout) findViewById(android.R.id.content).getRootView()
                .findViewWithTag("dialer_root")).addView(overlay);
    }

    // ==========================================
    // ➕ NOUVEAU CONTACT (enregistré dans le téléphone)
    // ==========================================
    private void afficherNouveauContact() {
        RelativeLayout overlay = new RelativeLayout(this);
        overlay.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(Color.parseColor("#000814"));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(60, 80, 60, 60);
        RelativeLayout.LayoutParams colP = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        col.setLayoutParams(colP);

        TextView titre = new TextView(this);
        titre.setText("Nouveau contact");
        titre.setTextColor(Color.WHITE);
        titre.setTextSize(24f);
        titre.getPaint().setFakeBoldText(true);
        titre.setPadding(0, 0, 0, 50);
        col.addView(titre);

        final android.widget.EditText champNom = champTexte("Nom", android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final android.widget.EditText champTel = champTexte("Téléphone", android.text.InputType.TYPE_CLASS_PHONE);
        final android.widget.EditText champMail = champTexte("E-mail", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                | android.text.InputType.TYPE_CLASS_TEXT);

        col.addView(champNom);
        col.addView(champTel);
        col.addView(champMail);

        // Boutons Annuler / Enregistrer
        LinearLayout boutons = new LinearLayout(this);
        boutons.setOrientation(LinearLayout.HORIZONTAL);
        boutons.setGravity(Gravity.CENTER);
        boutons.setPadding(0, 60, 0, 0);

        TextView annuler = boutonTexte("Annuler", "#33FFFFFF");
        annuler.setOnClickListener(v -> {
            if (overlay.getParent() != null) ((RelativeLayout) overlay.getParent()).removeView(overlay);
        });

        TextView enregistrer = boutonTexte("Enregistrer", "#22C55E");
        enregistrer.setOnClickListener(v -> {
            String nom = champNom.getText().toString().trim();
            String tel = champTel.getText().toString().trim();
            String mail = champMail.getText().toString().trim();
            if (tel.isEmpty() && nom.isEmpty()) {
                Toast.makeText(this, "Indique au moins un nom ou un numéro", Toast.LENGTH_SHORT).show();
                return;
            }
            if (enregistrerNouveauContact(nom, tel, mail)) {
                Toast.makeText(this, "Contact enregistré ✔", Toast.LENGTH_SHORT).show();
                if (overlay.getParent() != null) ((RelativeLayout) overlay.getParent()).removeView(overlay);
                rafraichirOngletContacts();
            }
        });

        boutons.addView(annuler);
        boutons.addView(enregistrer);
        col.addView(boutons);

        overlay.addView(col);
        ((RelativeLayout) findViewById(android.R.id.content).getRootView()
                .findViewWithTag("dialer_root")).addView(overlay);
    }

    private android.widget.EditText champTexte(String hint, int inputType) {
        android.widget.EditText e = new android.widget.EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#7788AA"));
        e.setTextColor(Color.WHITE);
        e.setTextSize(18f);
        e.setInputType(inputType);
        e.setPadding(20, 30, 20, 30);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = 24;
        e.setLayoutParams(p);
        return e;
    }

    private TextView boutonTexte(String texte, String couleurHex) {
        TextView b = new TextView(this);
        b.setText(texte);
        b.setTextColor(Color.WHITE);
        b.setTextSize(17f);
        b.setGravity(Gravity.CENTER);
        b.setPadding(60, 36, 60, 36);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(couleurHex));
        bg.setCornerRadius(50f);
        b.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        p.leftMargin = 12; p.rightMargin = 12;
        b.setLayoutParams(p);
        return b;
    }

    private boolean enregistrerNouveauContact(String nom, String tel, String mail) {
        try {
            java.util.ArrayList<android.content.ContentProviderOperation> ops = new java.util.ArrayList<>();
            int raw = ops.size();
            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());
            if (!nom.isEmpty()) {
                ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, nom)
                        .build());
            }
            if (!tel.isEmpty()) {
                ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, tel)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build());
            }
            if (!mail.isEmpty()) {
                ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, mail)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE,
                                ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build());
            }
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Enregistrement impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private LinearLayout boutonFiche(String icon, String label, final Runnable action) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(40, 0, 40, 0);

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(28f);
        ic.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#1AFFFFFF"));
        ic.setBackground(bg);
        ic.setLayoutParams(new LinearLayout.LayoutParams(150, 150));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.parseColor("#AABBCC"));
        lbl.setTextSize(13f);
        lbl.setGravity(Gravity.CENTER);
        lbl.setPadding(0, 16, 0, 0);

        col.addView(ic);
        col.addView(lbl);
        col.setOnClickListener(v -> action.run());
        return col;
    }

    private void confirmerSuppression(final String numero, final View overlay) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Supprimer le contact");
        b.setMessage("Voulez-vous vraiment supprimer ce contact ?");
        b.setPositiveButton("Supprimer", (d, w) -> {
            try {
                Uri lookupUri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero));
                Cursor c = getContentResolver().query(lookupUri,
                        new String[]{ContactsContract.PhoneLookup.LOOKUP_KEY}, null, null, null);
                if (c != null && c.moveToFirst()) {
                    String key = c.getString(0);
                    c.close();
                    // On sauvegarde dans la corbeille avant de supprimer
                    String nomContact = chercherNomContact(numero);
                    mettreEnCorbeille(nomContact, numero);
                    Uri deleteUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key);
                    getContentResolver().delete(deleteUri, null, null);
                    Toast.makeText(this, "Contact déplacé dans la corbeille", Toast.LENGTH_SHORT).show();
                    if (overlay.getParent() != null) {
                        ((RelativeLayout) overlay.getParent()).removeView(overlay);
                    }
                    // Rafraîchir la liste des contacts (reconstruction)
                    rafraichirOngletContacts();
                } else {
                    if (c != null) c.close();
                    Toast.makeText(this, "Contact introuvable", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Suppression impossible", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Annuler", null);
        b.show();
    }

    // ==========================================
    // 🗑️ CORBEILLE DES CONTACTS (30 jours)
    // ==========================================
    private static final long DUREE_CORBEILLE_MS = 30L * 24 * 60 * 60 * 1000; // 30 jours

    private void mettreEnCorbeille(String nom, String numero) {
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> corbeille = new HashSet<>(prefs.getStringSet("contacts_corbeille", new HashSet<>()));
        // Format : nom|numero|timestamp
        String entree = (nom != null ? nom : "") + "|" + numero + "|" + System.currentTimeMillis();
        corbeille.add(entree);
        prefs.edit().putStringSet("contacts_corbeille", corbeille).apply();
    }

    /** Purge les éléments de plus de 30 jours. À appeler au démarrage. */
    private void purgerCorbeille() {
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> corbeille = new HashSet<>(prefs.getStringSet("contacts_corbeille", new HashSet<>()));
        Set<String> restants = new HashSet<>();
        long maintenant = System.currentTimeMillis();
        for (String e : corbeille) {
            String[] p = e.split("\\|");
            if (p.length >= 3) {
                try {
                    long t = Long.parseLong(p[2]);
                    if (maintenant - t < DUREE_CORBEILLE_MS) {
                        restants.add(e); // encore valide
                    }
                } catch (Exception ignored) { }
            }
        }
        prefs.edit().putStringSet("contacts_corbeille", restants).apply();
    }

    private void afficherCorbeille() {
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> corbeille = prefs.getStringSet("contacts_corbeille", new HashSet<>());

        if (corbeille.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Corbeille")
                    .setMessage("La corbeille est vide.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Construire la liste affichable
        final java.util.List<String> entrees = new java.util.ArrayList<>(corbeille);
        final String[] labels = new String[entrees.size()];
        for (int i = 0; i < entrees.size(); i++) {
            String[] p = entrees.get(i).split("\\|");
            String nom = p.length > 0 ? p[0] : "";
            String num = p.length > 1 ? p[1] : "";
            long t = p.length > 2 ? Long.parseLong(p[2]) : 0;
            long joursRestants = (DUREE_CORBEILLE_MS - (System.currentTimeMillis() - t)) / (24L * 60 * 60 * 1000);
            String titre = (nom != null && !nom.isEmpty()) ? nom : num;
            labels[i] = titre + "\n" + num + "  (supprimé dans " + joursRestants + "j)";
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Corbeille (" + entrees.size() + ")");
        b.setItems(labels, (dialog, which) -> actionSurElementCorbeille(entrees.get(which)));
        b.setNegativeButton("Fermer", null);
        b.show();
    }

    private void actionSurElementCorbeille(final String entree) {
        String[] p = entree.split("\\|");
        final String nom = p.length > 0 ? p[0] : "";
        final String numero = p.length > 1 ? p[1] : "";

        String[] actions = {"♻️ Restaurer le contact", "❌ Supprimer définitivement"};
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle((nom != null && !nom.isEmpty()) ? nom : numero);
        b.setItems(actions, (dialog, which) -> {
            if (which == 0) {
                restaurerContact(nom, numero, entree);
            } else {
                supprimerDeCorbeille(entree);
                Toast.makeText(this, "Supprimé définitivement", Toast.LENGTH_SHORT).show();
            }
        });
        b.show();
    }

    private void restaurerContact(String nom, String numero, String entree) {
        try {
            java.util.ArrayList<android.content.ContentProviderOperation> ops = new java.util.ArrayList<>();
            int rawIndex = ops.size();
            ops.add(android.content.ContentProviderOperation.newInsert(
                    ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());
            // Nom
            ops.add(android.content.ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                            (nom != null && !nom.isEmpty()) ? nom : numero)
                    .build());
            // Numéro
            ops.add(android.content.ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, numero)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            supprimerDeCorbeille(entree);
            Toast.makeText(this, "Contact restauré ✔", Toast.LENGTH_SHORT).show();
            rafraichirOngletContacts();
        } catch (Exception e) {
            Toast.makeText(this, "Restauration impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void supprimerDeCorbeille(String entree) {
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> corbeille = new HashSet<>(prefs.getStringSet("contacts_corbeille", new HashSet<>()));
        corbeille.remove(entree);
        prefs.edit().putStringSet("contacts_corbeille", corbeille).apply();
    }

    private void bloquerContact(final String numero) {
        SharedPreferences prefs = getSharedPreferences("LeaProtect", Context.MODE_PRIVATE);
        Set<String> blockedSet = new HashSet<>(prefs.getStringSet("blocked_numbers", new HashSet<>()));

        if (blockedSet.contains(numero)) {
            Toast.makeText(this, "Ce numéro est déjà bloqué", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Bloquer ce contact");
        b.setMessage("Une fois bloqué, ses appels seront rejetés automatiquement : le téléphone ne sonnera pas.");
        b.setPositiveButton("Bloquer", (d, w) -> {
            blockedSet.add(numero);
            prefs.edit().putStringSet("blocked_numbers", blockedSet).apply();
            Toast.makeText(this, "Contact bloqué 🚫", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Annuler", null);
        b.show();
    }

    private void placeNativeCall(String number) {
        try {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            Uri uri = Uri.fromParts("tel", number, null);
            telecomManager.placeCall(uri, null);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission d'appel refusée par le système.", Toast.LENGTH_LONG).show();
        }
    }

    // 📼 APPEL DIRECT AU RÉPONDEUR (CONTOURNE LE BLOCAGE SAMSUNG)
    private void ouvrirRepondeurNatif() {
        try {
            // ACTION_CALL force l'appel immédiat au numéro de messagerie de la SIM (ex: 888, 666)
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("voicemail:"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'appeler le répondeur", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) LeaFeatureDetailActivity.applyImmersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arreterLecture();
    }
}