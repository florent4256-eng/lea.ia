package com.flolov42.lea_v3.plus.premium.skills;

import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

public class LeaSkillMovieActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int GRN  = 0xFF4CAF50;
    private static final int GOLD = 0xFFFFD700;
    private static final int DIM  = 0xFF7BB8CC;

    // Humeur → liste de films {titre, réalisateur, année, description courte}
    private static final String[][] JOYFUL = {
        {"La La Land", "D. Chazelle", "2016", "Un rêve de jazz et d'amour à Los Angeles."},
        {"Intouchables", "Toledano & Nakache", "2011", "Amitié improbable, humour et émotion."},
        {"Le Fabuleux Destin d'Amélie Poulain", "J.-P. Jeunet", "2001", "La magie du quotidien à Montmartre."},
        {"Mamma Mia!", "P. Lloyd", "2008", "ABBA, soleil grec et rires garantis."},
        {"Soul", "P. Docter", "2020", "Jazz, passion et sens de la vie."},
    };
    private static final String[][] SAD = {
        {"Her", "S. Jonze", "2013", "Amour, solitude et connexion à l'ère numérique."},
        {"Eternal Sunshine of the Spotless Mind", "M. Gondry", "2004", "Oublier pour mieux aimer."},
        {"Manchester by the Sea", "K. Lonergan", "2016", "Le poids du deuil et de la culpabilité."},
        {"Moonlight", "B. Jenkins", "2016", "Portrait d'une identité qui se cherche."},
        {"The Shawshank Redemption", "F. Darabont", "1994", "Espoir indestructible derrière les barreaux."},
    };
    private static final String[][] ROMANTIC = {
        {"Before Sunset", "R. Linklater", "2004", "Retrouvailles à Paris, dialogue exquis."},
        {"Notting Hill", "R. Michell", "1999", "Humour britannique et amour improbable."},
        {"Pride & Prejudice", "J. Wright", "2005", "Orgueil, préjugés et passion à l'anglaise."},
        {"Call Me by Your Name", "L. Guadagnino", "2017", "Un été italien d'amour absolu."},
        {"Portrait de la jeune fille en feu", "C. Sciamma", "2019", "Passion, art et regard au 18e siècle."},
    };
    private static final String[][] THRILLER = {
        {"Parasite", "B. Joon-ho", "2019", "Critique sociale explosive. Palme d'Or."},
        {"Gone Girl", "D. Fincher", "2014", "Manipulation et médias dans un mariage toxique."},
        {"Prisoners", "D. Villeneuve", "2013", "Jusqu'où iriez-vous pour sauver votre enfant?"},
        {"Knives Out", "R. Johnson", "2019", "Whodunit moderne et très malin."},
        {"The Prestige", "C. Nolan", "2006", "Rivalité et secrets entre illusionnistes."},
    };
    private static final String[][] SCIFI = {
        {"Interstellar", "C. Nolan", "2014", "Amour, gravité et temps qui s'effondre."},
        {"Arrival", "D. Villeneuve", "2016", "Premier contact avec une intelligence radicalement autre."},
        {"Ex Machina", "A. Garland", "2014", "L'IA et la question de la conscience."},
        {"Blade Runner 2049", "D. Villeneuve", "2017", "Humanité et mémoire dans un monde dystopique."},
        {"The Matrix", "Wachowskis", "1999", "Réalité, libre arbitre et pilule rouge."},
    };
    private static final String[][] COMEDY = {
        {"The Grand Budapest Hotel", "W. Anderson", "2014", "Folie visuelle, esprit et nostalgie."},
        {"What We Do in the Shadows", "Waititi & Clement", "2014", "Vampires en coloc — mockumentaire culte."},
        {"Game Night", "Daley & Goldstein", "2018", "Soirée jeux qui déraille avec brio."},
        {"Superbad", "G. Mottola", "2007", "Teen comedy sincère et hilarante."},
        {"L'Arnacœur", "P. Chesnais", "2010", "Comédie française légère et attachante."},
    };
    private static final String[][] ACTION = {
        {"Mad Max: Fury Road", "G. Miller", "2015", "Poursuite infernale, féminisme et adrénaline pure."},
        {"John Wick", "Leitch & Stahelski", "2014", "Chorégraphie gun-fu impeccable."},
        {"The Dark Knight", "C. Nolan", "2008", "Superhéros, philosophie et Joker légendaire."},
        {"Mission: Impossible — Fallout", "C. McQuarrie", "2018", "Cascade après cascade, sans trucage."},
        {"Casino Royale", "M. Campbell", "2006", "Bond réinventé, sobre et brutal."},
    };
    private static final String[][] DOCUMENTARY = {
        {"Free Solo", "Chin & Vasarhelyi", "2018", "Alex Honnold escalade El Capitan sans corde."},
        {"The Social Dilemma", "J. Orlowski", "2020", "Les réseaux sociaux et notre manipulation."},
        {"Seaspiracy", "A. Tabrizi", "2021", "L'impact de la pêche sur les océans."},
        {"13th", "A. DuVernay", "2016", "Esclavage, incarcération de masse et race aux USA."},
        {"My Octopus Teacher", "P. Ehrlich", "2020", "Amitié improbable avec une pieuvre."},
    };

    private LinearLayout movieContainer;
    private final String[] moodLabels = {"Joyeux","Triste","Romantique","Thriller","Sci-Fi","Comédie","Action","Documentaire"};
    private final String[][][] moodData = {JOYFUL, SAD, ROMANTIC, THRILLER, SCIFI, COMEDY, ACTION, DOCUMENTARY};
    private final String[] moodEmojis  = {"😄","😢","💕","😱","🚀","😂","💥","🎬"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LeaFeatureDetailActivity.applyImmersive(this);

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
        TextView title = new TextView(this); title.setText("🎬 Movie Recommender");
        title.setTextColor(CYAN); title.setTextSize(20); title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(8), dp(8), 0, 0); header.addView(title);
        root.addView(header);

        TextView sub = new TextView(this);
        sub.setText("Comment te sens-tu aujourd'hui?");
        sub.setTextColor(DIM); sub.setTextSize(14); sub.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sub.setPadding(dp(20), dp(4), dp(20), dp(16)); root.addView(sub);

        // Grille d'humeurs (2 par ligne)
        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < moodLabels.length; i += 2) {
            LinearLayout rowL = rowH();
            rowL.setPadding(dp(8), 0, dp(8), dp(8));
            rowL.addView(moodButton(i));
            if (i + 1 < moodLabels.length) rowL.addView(moodButton(i + 1));
            grid.addView(rowL);
        }
        root.addView(grid);

        movieContainer = new LinearLayout(this);
        movieContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(movieContainer);

        setContentView(sv);
    }

    private Button moodButton(int idx) {
        Button btn = new Button(this);
        btn.setText(moodEmojis[idx] + " " + moodLabels[idx]);
        btn.setBackgroundColor(CARD); btn.setTextColor(CYAN);
        btn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> showMoviesFor(idx));
        return btn;
    }

    private void showMoviesFor(int moodIdx) {
        movieContainer.removeAllViews();

        TextView header = new TextView(this);
        header.setText(moodEmojis[moodIdx] + " Films pour " + moodLabels[moodIdx]);
        header.setTextColor(GOLD); header.setTextSize(16); header.setTypeface(null, Typeface.BOLD);
        header.setPadding(dp(20), dp(8), dp(20), dp(12));
        movieContainer.addView(header);

        String[][] movies = moodData[moodIdx];
        // Ordre aléatoire
        List<String[]> list = new ArrayList<>(Arrays.asList(movies));
        Collections.shuffle(list);

        for (String[] m : list) {
            LinearLayout card = card();

            LinearLayout row = rowH();
            TextView numTv = new TextView(this);
            numTv.setText("🎬"); numTv.setTextSize(24); numTv.setPadding(0, 0, dp(12), 0);
            row.addView(numTv);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView titleTv = new TextView(this);
            titleTv.setText(m[0]); titleTv.setTextColor(Color.WHITE);
            titleTv.setTextSize(15); titleTv.setTypeface(null, Typeface.BOLD);
            info.addView(titleTv);

            TextView metaTv = new TextView(this);
            metaTv.setText(m[1] + " · " + m[2]);
            metaTv.setTextColor(DIM); metaTv.setTextSize(12);
            info.addView(metaTv);

            TextView descTv = new TextView(this);
            descTv.setText(m[3]); descTv.setTextColor(0xFFB0C8D8); descTv.setTextSize(13);
            descTv.setPadding(0, dp(4), 0, 0);
            info.addView(descTv);

            row.addView(info);
            card.addView(row);
            movieContainer.addView(card);
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL); c.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), dp(4));
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
