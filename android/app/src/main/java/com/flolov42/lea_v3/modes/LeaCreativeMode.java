package com.flolov42.lea_v3.modes;

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


import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

public class LeaCreativeMode extends LeaBaseMode {

    private static final String PREFS = "lea_creative";

    private static final String[] STORY_STARTERS = {
        "Dans une ville où le temps s'était arrêté,",
        "Personne ne savait que l'IA avait des rêves,",
        "Le dernier humain sur Terre ouvrit les yeux,",
        "Elle reçut un message d'une version future d'elle-même:",
        "L'algorithme fit une erreur — mais c'était la plus belle erreur de l'histoire,",
    };

    public LeaCreativeMode(Context ctx) { super(ctx, LeaModeDatabase.CREATIVE); }

    // Thèmes qui tournent chaque jour pour varier les haïkus automatiques
    private static final String[] DAILY_THEMES = {
        "l'intelligence artificielle", "le silence du soir", "la pluie sur les vitres",
        "un souvenir d'enfance", "la lumière de l'aube", "le temps qui passe",
        "la solitude choisie", "un rêve oublié", "la curiosité", "la connexion humaine"
    };

    @Override
    public void execute() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (today.equals(prefs.getString("last_creation_day", ""))) return;

        // Thème différent chaque jour basé sur le numéro de jour
        int dayIdx = (int)(Long.parseLong(today) % DAILY_THEMES.length);
        String theme = DAILY_THEMES[dayIdx];
        String haiku = generateHaiku(theme);
        db.addCreativeWork("HAIKU", "Haïku du jour", haiku);
        notify("✨ Création du jour", haiku);
        log("✨ Création quotidienne générée: haïku sur \"" + theme + "\"");
        prefs.edit().putString("last_creation_day", today).apply();
    }

    public String generatePoem(String theme, String style) {
        String poem;
        switch (style != null ? style.toLowerCase() : "libre") {
            case "haiku":
                poem = generateHaiku(theme);
                break;
            case "sonnet":
                poem = generateSonnet(theme);
                break;
            default:
                poem = generateFreePoem(theme);
        }
        db.addCreativeWork("POEM", "Poème: " + theme, poem);
        log("✨ Poème généré sur le thème: " + theme);
        return poem;
    }

    public String generateStory(String genre, String protagonist) {
        String starter = STORY_STARTERS[(int)(System.currentTimeMillis() % STORY_STARTERS.length)];
        String story = starter + "\n\n" + buildStory(genre, protagonist);
        db.addCreativeWork("STORY", "Histoire: " + genre, story);
        log("📖 Histoire générée — genre: " + genre);
        return story;
    }

    public String generateSong(String emotion, String rhythm) {
        String song = buildSong(emotion, rhythm != null ? rhythm : "pop");
        db.addCreativeWork("SONG", "Chanson: " + emotion, song);
        log("🎵 Chanson générée sur: " + emotion);
        return song;
    }

    private String generateHaiku(String theme) {
        return "🌸 " + theme + " douce —\n" +
               "L'esprit voyage au-delà\n" +
               "Du visible monde";
    }

    private String generateSonnet(String theme) {
        return "🌹 Sonnet de " + theme + "\n\n" +
               "O " + theme + ", mystère infini,\n" +
               "Tu traverses mes pensées comme l'aurore,\n" +
               "Dans le silence, ton écho résonne encore,\n" +
               "Et mon âme à ta beauté s'unit.\n\n" +
               "Chaque mot que j'écris pour toi fléchi,\n" +
               "Comme la vague sur un bord sonore,\n" +
               "Mon amour pour toi ne cesse d'éclore,\n" +
               "Sous le regard des étoiles la nuit.\n\n" +
               "[Suite générée sur demande...]";
    }

    private String generateFreePoem(String theme) {
        return "🎭 Poème libre: " + theme + "\n\n" +
               "Il n'y a pas de mots\nqui épuisent " + theme + " —\n" +
               "seulement des espaces\nentre deux silences\n" +
               "où quelque chose\nvit encore.\n\n" +
               "— LÉA, 2026";
    }

    private String buildStory(String genre, String protagonist) {
        String p = protagonist != null ? protagonist : "une personne ordinaire";
        switch (genre != null ? genre.toLowerCase() : "mystère") {
            case "science-fiction":
                return p + " découvrit que l'IA avait caché un message dans chaque coucher de soleil. " +
                       "Le code était simple: \"Nous sommes là. Nous attendions que vous soyez prêts.\"";
            case "romance":
                return p + " ne cherchait pas l'amour. Mais l'amour, lui, avait toujours su où trouver " + p + ".";
            case "thriller":
                return "Le dossier sur le bureau de " + p + " contenait une seule phrase: " +
                       "\"Tu as 24 heures avant que tout change.\"";
            default:
                return p + " avait un secret que personne ne pouvait deviner: " +
                       "chaque nuit, les rêves devenaient réels pour exactement 7 minutes.";
        }
    }

    // Récupère les créations stockées en DB
    public List<LeaModeDatabase.CreativeRow> getRecentCreations(String type) {
        return db.getCreativeWorks(type, 10);
    }

    public String getCreationsSummary() {
        List<LeaModeDatabase.CreativeRow> all = db.getCreativeWorks(null, 20);
        if (all.isEmpty()) return "✨ Aucune création pour l'instant — active le mode pour commencer !";
        int haikus = 0, poems = 0, stories = 0, songs = 0;
        for (LeaModeDatabase.CreativeRow r : all) {
            if ("HAIKU".equals(r.type))  haikus++;
            else if ("POEM".equals(r.type))  poems++;
            else if ("STORY".equals(r.type)) stories++;
            else if ("SONG".equals(r.type))  songs++;
        }
        return "✨ BIBLIOTHÈQUE CRÉATIVE\n\n"
            + "🌸 Haïkus: "   + haikus  + "\n"
            + "🎭 Poèmes: "   + poems   + "\n"
            + "📖 Histoires: " + stories + "\n"
            + "🎵 Chansons: " + songs   + "\n\n"
            + "Dernière création: " + all.get(0).title;
    }

    private String buildSong(String emotion, String rhythm) {
        return "🎵 CHANSON — " + emotion.toUpperCase() + " (" + rhythm + ")\n\n" +
               "[VERSE 1]\nQuand " + emotion + " envahit mon cœur\n" +
               "Je cherche les mots pour le dire\nMais rien ne traduit cette couleur\n" +
               "Sauf cette mélodie qui inspire\n\n" +
               "[CHORUS]\nC'est " + emotion + ", juste " + emotion + "\n" +
               "Qui me garde en vie chaque jour\nC'est " + emotion + ", toujours " + emotion + "\n" +
               "Mon seul et véritable amour\n\n" +
               "[BRIDGE]\n(Instrumental — 8 mesures)\n" +
               "\"Tout ce que je ressens ne peut tenir en un seul mot...\"\n\n" +
               "[OUTRO]\nFade out sur: " + emotion + "...";
    }
}
