package com.flolov42.lea_v3.plus.premium.skills;

import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import com.flolov42.lea_v3.ui.LeaFeatureDetailActivity;

public class LeaSkillChefActivity extends Activity {

    private static final int BG   = 0xFF011627;
    private static final int CYAN = 0xFF00E5FF;
    private static final int CARD = 0xFF012040;
    private static final int ORG  = 0xFFFF9800;
    private static final int GRN  = 0xFF4CAF50;
    private static final int GOLD = 0xFFFFD700;
    private static final int DIM  = 0xFF7BB8CC;

    // {nom, ingrédients clés, durée, difficulté, étapes résumées}
    private static final String[][] RECIPES = {
        {"Pasta Carbonara","pâtes,oeuf,lardons,parmesan","20min","Facile",
            "1. Cuire les pâtes al dente. 2. Mélanger œufs + parmesan. 3. Faire revenir les lardons. 4. Mélanger hors du feu pour lier la sauce."},
        {"Omelette aux champignons","oeuf,champignon,beurre,herbes","10min","Très facile",
            "1. Faire revenir les champignons. 2. Battre les œufs avec sel et poivre. 3. Cuire l'omelette baveuse. 4. Garnir et plier."},
        {"Risotto Parmesan","riz arborio,bouillon,parmesan,oignon,beurre","35min","Moyen",
            "1. Faire revenir l'oignon. 2. Ajouter le riz, nacrer 2min. 3. Ajouter bouillon louche par louche en remuant. 4. Mantecare avec beurre + parmesan."},
        {"Soupe de tomates","tomate,oignon,ail,basilic,huile olive","25min","Facile",
            "1. Faire revenir oignon et ail. 2. Ajouter les tomates concassées. 3. Mijoter 20min. 4. Mixer, saler, poivrer, servir avec basilic."},
        {"Poulet rôti citron-herbes","poulet,citron,ail,thym,romarin","90min","Moyen",
            "1. Mariner le poulet avec citron, ail et herbes. 2. Rôtir à 200°C. 3. Arroser toutes les 20min. 4. Reposer 10min avant de découper."},
        {"Salade niçoise","thon,oeuf,tomate,olive,haricots verts,anchois","15min","Très facile",
            "1. Cuire les œufs durs et les haricots verts. 2. Disposer tous les ingrédients sur la salade. 3. Vinaigrette moutarde-vinaigre."},
        {"Curry de légumes","potiron,pois chiche,lait de coco,curry,oignon","30min","Facile",
            "1. Faire revenir oignon + curry. 2. Ajouter les légumes. 3. Verser lait de coco. 4. Mijoter 20min. Servir avec riz basmati."},
        {"Tarte aux poireaux","poireau,crème,oeuf,pâte brisée,gruyère","45min","Moyen",
            "1. Faire fondre les poireaux dans du beurre. 2. Mélanger avec crème et œufs. 3. Verser sur la pâte. 4. Parsemer de gruyère. Cuire 35min à 180°C."},
        {"Taboulé","semoule,tomate,concombre,menthe,persil,citron","20min + repos","Très facile",
            "1. Réhydrater la semoule. 2. Couper finement les légumes et herbes. 3. Mélanger avec jus de citron et huile d'olive. 4. Laisser reposer 30min au frais."},
        {"Quiche lorraine","lardons,crème,oeuf,gruyère,pâte brisée","50min","Facile",
            "1. Précuire la pâte 10min. 2. Faire revenir les lardons. 3. Battre œufs + crème + gruyère. 4. Verser sur la pâte. 5. Cuire 35min à 180°C."},
        {"Guacamole","avocat,citron vert,tomate,oignon,coriandre,piment","10min","Très facile",
            "1. Écraser les avocats à la fourchette. 2. Ajouter citron vert, sel, piment. 3. Incorporer tomate et oignon coupés fin. 4. Garnir de coriandre."},
        {"Soupe miso","bouillon dashi,miso,tofu,algues wakame,oignon vert","15min","Facile",
            "1. Chauffer le bouillon dashi. 2. Dissoudre le miso dans un peu de bouillon. 3. Ajouter le tofu en cubes et les algues. 4. Servir avec oignons verts."},
        {"Riz sauté aux légumes","riz cuit,carotte,oignon,oeuf,sauce soja,ail","20min","Facile",
            "1. Faire revenir ail et légumes. 2. Pousser les légumes sur le côté, brouiller les œufs. 3. Ajouter le riz froid. 4. Assaisonner avec sauce soja."},
        {"Pain perdu","pain rassis,oeuf,lait,sucre,beurre","15min","Très facile",
            "1. Battre œufs, lait, sucre et vanille. 2. Tremper les tranches de pain. 3. Faire dorer dans du beurre mousseux. 4. Servir avec sucre glace ou sirop d'érable."},
        {"Houmous maison","pois chiche,tahini,citron,ail,huile olive","15min","Très facile",
            "1. Égoutter les pois chiches. 2. Mixer avec tahini, citron pressé, ail et sel. 3. Ajouter huile d'olive en filet. 4. Mixer jusqu'à consistance crémeuse."},
        {"Ratatouille","aubergine,courgette,poivron,tomate,ail,herbes","60min","Moyen",
            "1. Couper tous les légumes en cubes. 2. Faire revenir séparément. 3. Assembler et mijoter avec ail et herbes. 4. Cuire à feu doux 40min."},
        {"Oeufs en cocotte","oeuf,crème,gruyère,herbes","20min","Très facile",
            "1. Verser un peu de crème dans des ramequins. 2. Casser un œuf dans chaque. 3. Assaisonner et ajouter gruyère. 4. Cuire au bain-marie 12-15min à 180°C."},
        {"Lentilles au lardons","lentilles,lardons,carotte,oignon,thym,laurier","45min","Facile",
            "1. Faire revenir les lardons. 2. Ajouter oignon et carottes. 3. Ajouter les lentilles et couvrir d'eau. 4. Cuire 30min avec thym et laurier."},
        {"Velouté de butternut","courge butternut,oignon,crème,noix muscade","30min","Facile",
            "1. Faire revenir l'oignon. 2. Ajouter la courge en cubes. 3. Couvrir d'eau et cuire 20min. 4. Mixer, ajouter crème, noix muscade, sel et poivre."},
        {"Brownies","chocolat noir,beurre,oeuf,sucre,farine","40min","Facile",
            "1. Fondre chocolat et beurre. 2. Incorporer sucre et œufs. 3. Ajouter la farine tamisée. 4. Verser dans un moule, cuire 20-25min à 180°C."},
    };

    private LinearLayout recipeContainer;
    private EditText ingredientInput;

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
        TextView title = new TextView(this); title.setText("👨‍🍳 Chef Léa");
        title.setTextColor(ORG); title.setTextSize(20); title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(8), dp(8), 0, 0); header.addView(title);
        root.addView(header);

        // Champ ingrédients
        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.VERTICAL);
        searchCard.setBackgroundColor(CARD);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, -2);
        sclp.setMargins(dp(12), dp(4), dp(12), dp(8));
        searchCard.setLayoutParams(sclp);
        searchCard.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView searchLabel = new TextView(this);
        searchLabel.setText("Quels ingrédients as-tu?");
        searchLabel.setTextColor(GOLD); searchLabel.setTextSize(15); searchLabel.setTypeface(null, Typeface.BOLD);
        searchCard.addView(searchLabel);

        ingredientInput = new EditText(this);
        ingredientInput.setHint("Ex: oeuf, tomate, pasta, poulet...");
        ingredientInput.setTextColor(Color.WHITE); ingredientInput.setHintTextColor(DIM);
        ingredientInput.setTextSize(14);
        LinearLayout.LayoutParams ietlp = new LinearLayout.LayoutParams(-1, -2);
        ietlp.setMargins(0, dp(8), 0, dp(8));
        ingredientInput.setLayoutParams(ietlp);
        searchCard.addView(ingredientInput);

        Button searchBtn = new Button(this); searchBtn.setText("Trouver des recettes");
        searchBtn.setBackgroundColor(ORG); searchBtn.setTextColor(0xFF011627);
        searchBtn.setTypeface(null, Typeface.BOLD);
        searchBtn.setOnClickListener(v -> searchRecipes());
        searchCard.addView(searchBtn);

        root.addView(searchCard);

        // Container recettes
        recipeContainer = new LinearLayout(this);
        recipeContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(recipeContainer);

        setContentView(sv);
        showAllRecipes();
    }

    private void searchRecipes() {
        String input = ingredientInput.getText().toString().toLowerCase().trim();
        if (input.isEmpty()) { showAllRecipes(); return; }

        String[] userIngredients = input.split("[,;\\s]+");
        recipeContainer.removeAllViews();

        List<String[]> matched = new ArrayList<>();
        for (String[] recipe : RECIPES) {
            String recipeIngredients = recipe[1].toLowerCase();
            int matchCount = 0;
            for (String ingr : userIngredients) {
                if (!ingr.isEmpty() && recipeIngredients.contains(ingr.trim())) matchCount++;
            }
            if (matchCount > 0) matched.add(recipe);
        }

        // Trier par nombre de matchs (approx: déjà dans l'ordre d'ajout)
        if (matched.isEmpty()) {
            TextView noResult = new TextView(this);
            noResult.setText("Aucune recette trouvée pour ces ingrédients.\nEssaie avec moins d'ingrédients ou cherche séparément.");
            noResult.setTextColor(DIM); noResult.setTextSize(14);
            noResult.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noResult.setPadding(dp(20), dp(24), dp(20), dp(16));
            recipeContainer.addView(noResult);
        } else {
            TextView resultHdr = new TextView(this);
            resultHdr.setText(matched.size() + " recette(s) trouvée(s):");
            resultHdr.setTextColor(GRN); resultHdr.setTextSize(14);
            resultHdr.setPadding(dp(20), dp(8), dp(20), dp(4));
            recipeContainer.addView(resultHdr);
            for (String[] r : matched) recipeContainer.addView(buildRecipeCard(r));
        }
    }

    private void showAllRecipes() {
        recipeContainer.removeAllViews();
        TextView allHdr = new TextView(this);
        allHdr.setText("Toutes nos recettes:");
        allHdr.setTextColor(GOLD); allHdr.setTextSize(15); allHdr.setTypeface(null, Typeface.BOLD);
        allHdr.setPadding(dp(20), dp(8), dp(20), dp(4));
        recipeContainer.addView(allHdr);
        for (String[] r : RECIPES) recipeContainer.addView(buildRecipeCard(r));
    }

    private View buildRecipeCard(String[] recipe) {
        LinearLayout card = card();

        // Ligne 1: titre + durée + difficulté
        LinearLayout row1 = rowH();
        TextView nameTv = new TextView(this);
        nameTv.setText(recipe[0]); nameTv.setTextColor(Color.WHITE);
        nameTv.setTextSize(15); nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row1.addView(nameTv);

        TextView timeTv = new TextView(this);
        timeTv.setText(recipe[2]); timeTv.setTextColor(ORG); timeTv.setTextSize(12);
        row1.addView(timeTv);
        card.addView(row1);

        // Difficulté
        TextView diffTv = new TextView(this);
        int diffColor = recipe[3].contains("Très") ? GRN : (recipe[3].contains("Moyen") ? ORG : CYAN);
        diffTv.setText("● " + recipe[3]); diffTv.setTextColor(diffColor); diffTv.setTextSize(12);
        diffTv.setPadding(0, dp(2), 0, dp(4));
        card.addView(diffTv);

        // Ingrédients
        TextView ingrTv = new TextView(this);
        ingrTv.setText("Ingrédients: " + recipe[1]);
        ingrTv.setTextColor(DIM); ingrTv.setTextSize(12);
        card.addView(ingrTv);

        // Bouton voir recette
        Button detailBtn = new Button(this); detailBtn.setText("Voir la recette →");
        detailBtn.setBackgroundColor(Color.TRANSPARENT); detailBtn.setTextColor(CYAN);
        detailBtn.setTextSize(13);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-2, -2);
        dlp.gravity = Gravity.END;
        detailBtn.setLayoutParams(dlp);
        detailBtn.setOnClickListener(v -> showRecipeDetail(recipe));
        card.addView(detailBtn);

        return card;
    }

    private void showRecipeDetail(String[] recipe) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("👨‍🍳 " + recipe[0])
            .setMessage(
                "⏱ Durée: " + recipe[2] + "\n" +
                "📊 Difficulté: " + recipe[3] + "\n\n" +
                "🥕 Ingrédients:\n" + recipe[1].replace(",", ", ") + "\n\n" +
                "📝 Préparation:\n" + recipe[4].replace(". ", ".\n")
            )
            .setPositiveButton("Fermer", null)
            .show();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL); c.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(4), dp(12), dp(4));
        c.setLayoutParams(lp); c.setPadding(dp(16), dp(12), dp(16), dp(12));
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
