package com.flolov42.lea_v3.plus.learning;

import android.content.Context;
import com.flolov42.lea_v3.database.LeaPlusDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LeaLanguageContent {

    // Format: {mot_EN, traduction_FR, phonétique, exemple_EN}
    private static final String[][] EN_VOCAB = {
        {"serendipity","sérendipité","ˌsɛrənˈdɪpɪti","A happy accident of serendipity led to the discovery."},
        {"resilience","résilience","rɪˈzɪliəns","Her resilience helped her overcome every obstacle."},
        {"ephemeral","éphémère","ɪˈfɛmərəl","The beauty of cherry blossoms is ephemeral."},
        {"eloquent","éloquent","ˈɛləkwənt","He gave an eloquent speech that moved the audience."},
        {"perseverance","persévérance","ˌpɜːsɪˈvɪərəns","Success comes through perseverance and hard work."},
        {"ambiguous","ambigu","æmˈbɪɡjuəs","The message was ambiguous and hard to understand."},
        {"benevolent","bienveillant","bɪˈnɛvələnt","She is a benevolent leader who cares for her team."},
        {"candid","franc","ˈkændɪd","I appreciate your candid feedback on my work."},
        {"diligent","diligent","ˈdɪlɪdʒənt","She is a diligent student who never misses class."},
        {"empathy","empathie","ˈɛmpəθi","Empathy is the ability to understand others' feelings."},
        {"frugal","frugal","ˈfruːɡəl","A frugal lifestyle helps save money for the future."},
        {"gregarious","grégaire","ɡrɪˈɡɛəriəs","He is gregarious and loves meeting new people."},
        {"humble","humble","ˈhʌmbəl","Despite her success, she remained humble and kind."},
        {"innovative","innovant","ˈɪnəveɪtɪv","We need innovative solutions to solve this problem."},
        {"jubilant","exultant","ˈdʒuːbɪlənt","The team was jubilant after winning the championship."},
        {"keen","enthousiaste","kiːn","She has a keen interest in environmental science."},
        {"luminous","lumineux","ˈluːmɪnəs","The moon cast a luminous glow over the lake."},
        {"meticulous","méticuleux","mɪˈtɪkjʊləs","The painter was meticulous in his attention to detail."},
        {"nuanced","nuancé","ˈnjuːɑːnst","Her analysis of the problem was nuanced and thorough."},
        {"optimistic","optimiste","ˌɒptɪˈmɪstɪk","Stay optimistic even when things seem difficult."},
        {"pragmatic","pragmatique","præɡˈmætɪk","We need a pragmatic approach to solve this issue."},
        {"quintessential","par excellence","kwɪnˈtɛsənʃəl","Paris is the quintessential romantic city."},
        {"radiant","rayonnant","ˈreɪdiənt","She had a radiant smile that lit up the room."},
        {"steadfast","inébranlable","ˈstɛdfɑːst","He remained steadfast in his commitment to justice."},
        {"tenacious","tenace","tɪˈneɪʃəs","Her tenacious spirit helped her climb to the top."},
        {"ubiquitous","omniprésent","juːˈbɪkwɪtəs","Smartphones have become ubiquitous in modern life."},
        {"vivid","vif","ˈvɪvɪd","She has a vivid imagination and loves storytelling."},
        {"whimsical","fantaisiste","ˈwɪmzɪkəl","The garden was filled with whimsical sculptures."},
        {"xenophile","xénophile","ˈzɛnəfaɪl","As a xenophile, she collects art from around the world."},
        {"yearning","désir ardent","ˈjɜːnɪŋ","He had a yearning to travel the world."},
        {"zealous","zélé","ˈzɛləs","She is a zealous advocate for human rights."},
        {"abundant","abondant","əˈbʌndənt","The river valley has abundant natural resources."},
        {"bliss","félicité","blɪs","Walking in nature brings me a sense of bliss."},
        {"clarity","clarté","ˈklærɪti","Meditation brings clarity of mind and purpose."},
        {"dazzling","éblouissant","ˈdæzlɪŋ","The fireworks display was absolutely dazzling."},
        {"earnest","sincère","ˈɜːnɪst","She made an earnest effort to improve her skills."},
        {"flourish","s'épanouir","ˈflʌrɪʃ","Plants flourish when given proper care and sunlight."},
        {"gracious","gracieux","ˈɡreɪʃəs","She was gracious in both victory and defeat."},
        {"harmonious","harmonieux","hɑːˈmoʊniəs","The team worked in a harmonious and productive way."},
        {"introspective","introspectif","ˌɪntrəˈspɛktɪv","He became introspective after reading the philosophy book."},
        {"joyful","joyeux","ˈdʒɔɪfəl","The children were joyful playing in the park."},
        {"kinship","parenté","ˈkɪnʃɪp","She felt a deep kinship with the local community."},
        {"lively","animé","ˈlaɪvli","The market was lively with vendors and shoppers."},
        {"mindful","conscient","ˈmaɪndfəl","Being mindful helps reduce stress and anxiety."},
        {"noble","noble","ˈnoʊbəl","It was a noble gesture to donate to the charity."},
        {"open-minded","ouvert d'esprit","ˈoʊpən maɪndɪd","An open-minded person welcomes new perspectives."},
        {"passionate","passionné","ˈpæʃənɪt","She is passionate about environmental conservation."},
        {"quirky","original","ˈkwɜːki","His quirky sense of humor always made people laugh."},
        {"refreshing","rafraîchissant","rɪˈfrɛʃɪŋ","It was refreshing to hear an honest opinion."},
        {"sincere","sincère","sɪnˈsɪər","A sincere apology can heal many wounds."},
        {"thoughtful","attentionné","ˈθɔːtfəl","She left a thoughtful note to cheer up her friend."},
    };

    // Format: {mot_ES, traduction_FR, phonétique, exemple_ES}
    private static final String[][] ES_VOCAB = {
        {"madrugada","aube / petit matin","maðɾuˈɣaða","Me despierto en la madrugada para estudiar."},
        {"sobremesa","conversation après repas","soβɾeˈmesa","La sobremesa duró más que la cena."},
        {"añoranza","nostalgie","aɲoˈɾansa","Siento añoranza por los veranos de mi infancia."},
        {"resiliencia","résilience","resiljenθja","Su resiliencia la ayudó a superar los obstáculos."},
        {"entusiasmo","enthousiasme","entuzjaˈsmo","Trabaja con entusiasmo en cada proyecto."},
        {"confianza","confiance","konˈfjansa","La confianza es la base de toda relación."},
        {"tranquilidad","tranquillité","tɾankiliˈðað","La meditación me trae tranquilidad interior."},
        {"gratitud","gratitude","ɡɾatiˈtuð","La gratitud transforma nuestra perspectiva de vida."},
        {"amabilidad","amabilité","amabiliˈðað","Su amabilidad conquistó a todos en la reunión."},
        {"perseverancia","persévérance","peɾseβeˈɾansja","La perseverancia es clave para el éxito."},
        {"valentía","courage","balenˈtia","Hace falta valentía para seguir tus sueños."},
        {"sabiduría","sagesse","saβiðuˈɾia","La sabiduría viene con la experiencia."},
        {"humildad","humilité","umilˈdað","La humildad es una virtud muy preciada."},
        {"libertad","liberté","liβeɾˈtað","La libertad es el bien más preciado."},
        {"esperanza","espoir","espeˈɾansa","La esperanza mantiene viva la llama interior."},
        {"bienestar","bien-être","bjeneˈstaɾ","El bienestar mental es tan importante como el físico."},
        {"compromiso","engagement","kompɾoˈmiso","Tengo un compromiso con mis valores."},
        {"alegría","joie","aleˈɣɾia","La alegría es contagiosa y eleva el ánimo."},
        {"paciencia","patience","paˈsjensja","La paciencia es una virtud esencial."},
        {"fortaleza","force intérieure","foɾtaˈlesa","La fortaleza interior nos sostiene en tiempos difíciles."},
        {"bondad","bonté","bonˈdað","La bondad es el lenguaje universal del corazón."},
        {"creatividad","créativité","kɾeatiβiˈðað","La creatividad no tiene límites cuando eres libre."},
        {"curiosidad","curiosité","kuɾjosiˈðað","La curiosidad es el motor del aprendizaje."},
        {"determinación","détermination","deteɾminaˈsjon","Con determinación, todo es posible."},
        {"empatía","empathie","empaˈtia","La empatía nos conecta profundamente con los demás."},
        {"felicidad","bonheur","feliθiˈðað","La felicidad está en las pequeñas cosas."},
        {"generosidad","générosité","xeneɾosiˈðað","Su generosidad no conoce límites."},
        {"honestidad","honnêteté","onestiˈðað","La honestidad construye relaciones duraderas."},
        {"inspiración","inspiration","inspiɾaˈsjon","Busco inspiración en la naturaleza."},
        {"justicia","justice","xusˈtisja","La justicia es el fundamento de la sociedad."},
    };

    // Format: {mot_DE, traduction_FR, phonétique, exemple_DE}
    private static final String[][] DE_VOCAB = {
        {"Fernweh","nostalgie des pays lointains","ˈfɛʁnveː","Ich habe Fernweh und möchte die Welt bereisen."},
        {"Wanderlust","désir de voyager","ˈvandɐlʊst","Wanderlust treibt mich immer zu neuen Abenteuern."},
        {"Gemütlichkeit","confort et convivialité","ɡəˈmyːtlɪçkaɪt","Die Gemütlichkeit des Hauses macht es zu einem Zuhause."},
        {"Schadenfreude","joie maligne","ˈʃaːdnˌfʁɔʏdə","Schadenfreude ist keine schöne Eigenschaft."},
        {"Weltanschauung","vision du monde","ˈvɛltʔanˌʃaʊʊŋ","Seine Weltanschauung beeinflusst seine Entscheidungen."},
        {"Fingerspitzengefühl","doigté / tact","ˈfɪŋɐˌʃpɪtsn̩ɡəˌfyːl","Er hat Fingerspitzengefühl im Umgang mit Menschen."},
        {"Dankbarkeit","gratitude","ˈdaŋkbaːɐ̯kaɪt","Dankbarkeit verändert die Perspektive."},
        {"Aufmerksamkeit","attention / bienveillance","ˈaʊ̯fmɛʁkzaːmkaɪt","Aufmerksamkeit ist die wichtigste Form der Fürsorge."},
        {"Gelassenheit","sérénité","ɡəˈlasn̩haɪt","Gelassenheit hilft uns, Stress zu bewältigen."},
        {"Selbstbewusstsein","conscience de soi","ˈzɛlpstbəˌvʊstzaɪn","Ein gesundes Selbstbewusstsein ist wichtig."},
        {"Neugier","curiosité","ˈnɔɪ̯ɡiːɐ̯","Neugier ist der Schlüssel zum Lernen."},
        {"Mitgefühl","compassion","ˈmɪtɡəˌfyːl","Mitgefühl verbindet uns mit anderen Menschen."},
        {"Ehrlichkeit","honnêteté","ˈeːɐ̯lɪçkaɪt","Ehrlichkeit ist die Grundlage jeder Freundschaft."},
        {"Begeisterung","enthousiasme","bəˈɡaɪ̯stəʁʊŋ","Ihre Begeisterung für Musik ist ansteckend."},
        {"Geduld","patience","ɡəˈdʊlt","Geduld ist eine Tugend, die man üben muss."},
        {"Freundlichkeit","amabilité","ˈfʁɔʏntlɪçkaɪt","Freundlichkeit öffnet viele Türen im Leben."},
        {"Tapferkeit","courage","ˈtapfɐkaɪt","Tapferkeit bedeutet, trotz Angst weiterzumachen."},
        {"Hoffnung","espoir","ˈhɔfnʊŋ","Hoffnung ist das Licht in dunklen Zeiten."},
        {"Stärke","force","ˈʃtɛʁkə","Innere Stärke wächst durch Überwindung von Hindernissen."},
        {"Weisheit","sagesse","ˈvaɪ̯shaɪt","Weisheit kommt mit Erfahrung und Reflexion."},
        {"Freude","joie","ˈfʁɔʏdə","Freude teilen bedeutet, sie zu verdoppeln."},
        {"Mut","courage","muːt","Mut ist nicht die Abwesenheit von Angst."},
        {"Frieden","paix","ˈfʁiːdn̩","Innerer Frieden beginnt mit Selbstakzeptanz."},
        {"Liebe","amour","ˈliːbə","Liebe ist die stärkste Kraft im Universum."},
        {"Vertrauen","confiance","fɛʁˈtʁaʊ̯ən","Vertrauen ist die Basis jeder guten Beziehung."},
    };

    // Format: {mot_PT, traduction_FR, phonétique, exemple_PT}
    private static final String[][] PT_VOCAB = {
        {"saudade","mélancolie nostalgique","sawˈdadɨ","Sinto saudade dos dias de infância."},
        {"resiliência","résilience","ʁɛziˈljẽsjɐ","A resiliência nos ajuda a superar os desafios."},
        {"alegria","joie","ɐlɛˈɡɾiɐ","A alegria está nas pequenas coisas do dia a dia."},
        {"gratidão","gratitude","ɡɾɐtiˈdɐ̃w","A gratidão transforma a nossa perspectiva de vida."},
        {"esperança","espoir","ɨʃpɛˈɾɐ̃sɐ","A esperança é a luz que guia nos momentos difíceis."},
        {"bondade","bonté","bõˈdadɨ","A bondade é a linguagem universal do coração."},
        {"coragem","courage","kuˈɾaʒɐ̃j","A coragem é continuar mesmo com medo."},
        {"criatividade","créativité","kɾjɐtiviˈdadɨ","A criatividade não tem limites."},
        {"determinação","détermination","dɨtɨɾmiˈnɐsɐ̃w","Com determinação, tudo é possível."},
        {"empatia","empathie","ẽˈpɐtjɐ","A empatia conecta as pessoas de forma profunda."},
        {"felicidade","bonheur","fɨlisiˈdadɨ","A felicidade está dentro de nós mesmos."},
        {"generosidade","générosité","ʒɨnɨɾuziˈdadɨ","A generosidade enriquece quem dá e quem recebe."},
        {"honestidade","honnêteté","ɔnɨʃtiˈdadɨ","A honestidade constrói relações duradouras."},
        {"inspiração","inspiration","ĩʃpiɾɐˈsɐ̃w","Busco inspiração na natureza e na arte."},
        {"justiça","justice","ʒuʃˈtisɐ","A justiça é o fundamento de uma sociedade justa."},
        {"liberdade","liberté","libɨɾˈdadɨ","A liberdade é o bem mais precioso."},
        {"maravilha","merveille","mɐɾɐˈviʎɐ","O mundo é cheio de maravilhas para descobrir."},
        {"paciência","patience","pɐˈsjẽsjɐ","A paciência é uma virtude essencial na vida."},
        {"serenidade","sérénité","sɨɾɨniˈdadɨ","A meditação traz serenidade à mente."},
        {"tranquilidade","tranquillité","tɾɐ̃kilidˈadɨ","A tranquilidade interior é um testemunho de paz."},
        {"amizade","amitié","ɐmiˈzadɨ","A amizade verdadeira é um tesouro inestimável."},
        {"sabedoria","sagesse","sɐbɨˈdoɾjɐ","A sabedoria vem da experiência e da reflexão."},
        {"humildade","humilité","uˈmildadɨ","A humildade é sinal de verdadeira grandeza."},
        {"perseverança","persévérance","pɨɾsɨvɨˈɾɐ̃sɐ","A perseverança é a chave do sucesso."},
        {"confiança","confiance","kõfiˈɐ̃sɐ","A confiança é a base de toda relação saudável."},
    };

    private final Context ctx;
    private final LeaPlusDatabase db;

    private static LeaLanguageContent instance;
    public static synchronized LeaLanguageContent get(Context ctx) {
        if (instance == null) instance = new LeaLanguageContent(ctx.getApplicationContext());
        return instance;
    }
    private LeaLanguageContent(Context ctx) {
        this.ctx = ctx;
        this.db  = LeaPlusDatabase.get(ctx);
    }

    // ── Seeder principal ──────────────────────────────────────────────────────
    public void seedIfEmpty(String languageCode) {
        if (db.getVocabCount(languageCode) >= 10) return;

        String[][] vocab = getVocabFor(languageCode);
        if (vocab == null) return;

        for (String[] entry : vocab) {
            db.insertVocab(languageCode, entry[0], entry[1], entry[2], entry[3]);
        }
    }

    // ── Quiz: 4 choix aléatoires ──────────────────────────────────────────────
    public static class QuizQuestion {
        public String word, phonetic, example;
        public String correctAnswer;
        public String[] choices = new String[4];
    }

    public QuizQuestion generateQuiz(String languageCode) {
        String[][] vocab = getVocabFor(languageCode);
        if (vocab == null || vocab.length < 4) return null;

        Random rnd   = new Random();
        int correct  = rnd.nextInt(vocab.length);
        String[] tgt = vocab[correct];

        QuizQuestion q = new QuizQuestion();
        q.word          = tgt[0];
        q.phonetic      = tgt[2];
        q.example       = tgt[3];
        q.correctAnswer = tgt[1];

        // 3 mauvaises réponses
        List<String> wrongs = new ArrayList<>();
        for (int i = 0; i < vocab.length; i++) {
            if (i != correct) wrongs.add(vocab[i][1]);
        }
        Collections.shuffle(wrongs, rnd);

        int pos = rnd.nextInt(4);
        int w = 0;
        for (int i = 0; i < 4; i++) {
            if (i == pos) q.choices[i] = q.correctAnswer;
            else          q.choices[i] = wrongs.get(w++);
        }
        return q;
    }

    // ── Accesseurs par langue ─────────────────────────────────────────────────
    public String[][] getVocabFor(String langCode) {
        switch (langCode.toUpperCase()) {
            case "EN": return EN_VOCAB;
            case "ES": return ES_VOCAB;
            case "DE": return DE_VOCAB;
            case "PT": return PT_VOCAB;
            case "FR": return frenchVocabMirrorEN();
            default:   return null;
        }
    }

    // Pour FR: enseigne l'anglais à un francophone
    private String[][] frenchVocabMirrorEN() {
        // Pivot: mot FR d'abord, traduction EN
        String[][] fr = new String[EN_VOCAB.length][4];
        for (int i = 0; i < EN_VOCAB.length; i++) {
            fr[i][0] = EN_VOCAB[i][1];  // mot français
            fr[i][1] = EN_VOCAB[i][0];  // traduction anglaise
            fr[i][2] = EN_VOCAB[i][2];  // phonétique anglaise
            fr[i][3] = EN_VOCAB[i][3];  // exemple anglais
        }
        return fr;
    }
}
