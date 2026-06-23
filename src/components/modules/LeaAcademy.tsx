import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  BookOpen, Code2, Map, BarChart3, Trophy, Play, CheckCircle,
  ChevronRight, Lock, Star, Zap, Clock,
  Target, RefreshCw, Terminal, Lightbulb,
  ArrowLeft, Users, Timer
} from 'lucide-react';

// ─── CONFIG ──────────────────────────────────────────────────────────────────
const API_BASE_ACADEMY = '/api/academy';
const STORAGE   = 'lea_academy_v3';

async function api(path: string, body?: object): Promise<any> {
  const currentUser = localStorage.getItem('lea_currentUser') || '';
  const headers: HeadersInit = { 'x-identity': currentUser };
  if (body) headers['Content-Type'] = 'application/json';

  const opts: RequestInit = body
    ? { method: 'POST', headers, body: JSON.stringify(body) }
    : { method: 'GET', headers };
  try {
    const r = await fetch(API_BASE_ACADEMY + path, { ...opts, signal: AbortSignal.timeout(4000) });
    if (r.ok) return r.json();
  } catch {}
  return null;
}

const save = (v: unknown) => { try { localStorage.setItem(STORAGE, JSON.stringify(v)); } catch {} };
const load = <T,>(def: T): T => { try { const r = localStorage.getItem(STORAGE); return r ? JSON.parse(r) : def; } catch { return def; } };

// ─── TYPES ───────────────────────────────────────────────────────────────────
interface TestCase { input: string; expected: string; }
interface Exercise { id: string; title: string; desc: string; level: 'easy'|'medium'|'hard'; lang: string; starter: string; tests: TestCase[]; hint: string; }
interface QuizQ    { q: string; opts: string[]; correct: number; }
interface Lesson   { id: string; title: string; content: string; code: string; lang: string; exercises: Exercise[]; }
interface Chapter  { id: string; title: string; lessons: Lesson[]; quiz: QuizQ[]; }
interface Course   { id: string; name: string; lang: string; level: string; hours: string; icon: string; color: string; desc: string; chapters: Chapter[]; }
interface Challenge{ id: string; title: string; desc: string; difficulty: string; lang: string; starter: string; tests: TestCase[]; xp: number; }
interface Progress { xp: number; done: string[]; badges: string[]; streak: number; lastDate: string; lines: number; tests: number; }

// ─── XP HELPERS ──────────────────────────────────────────────────────────────
const LEVELS = [
  { min: 0,    max: 500,  name: 'Novice',      color: '#94a3b8' },
  { min: 500,  max: 1500, name: 'Apprenti',    color: '#60a5fa' },
  { min: 1500, max: 3500, name: 'Développeur', color: '#a78bfa' },
  { min: 3500, max: 7000, name: 'Avancé',      color: '#34d399' },
  { min: 7000, max: Infinity, name: 'Expert',  color: '#00f2ff' },
];
const getLevel = (xp: number) => LEVELS.findIndex(l => xp < l.max);
const getLvl   = (xp: number) => LEVELS[Math.min(getLevel(xp), 4)];
const xpToNext = (xp: number) => { const l = LEVELS[Math.min(getLevel(xp), 4)]; return l.max === Infinity ? 0 : l.max - l.min; };
const xpInLevel= (xp: number) => { const l = LEVELS[Math.min(getLevel(xp), 4)]; return xp - l.min; };
const pct      = (xp: number) => xpToNext(xp) ? Math.round((xpInLevel(xp) / xpToNext(xp)) * 100) : 100;

// ─── BADGES DATA ──────────────────────────────────────────────────────────────
const ALL_BADGES = [
  { id: 'first_code',   icon: '🚀', name: 'Premier Code',      desc: 'Complète ton 1er exercice', xp: 1   },
  { id: 'quiz_ace',     icon: '🎯', name: 'Quiz Ace',           desc: '100% à un quiz',            xp: 200 },
  { id: 'speed_coder',  icon: '⚡', name: 'Speed Coder',        desc: 'Exercice en < 2 min',       xp: 100 },
  { id: 'bug_hunter',   icon: '🐛', name: 'Bug Hunter',         desc: 'Débogue 10 exercices',      xp: 10  },
  { id: 'py_master',    icon: '🐍', name: 'Python Master',      desc: 'Termine le cours Python',   xp: 1000},
  { id: 'js_master',    icon: '⚙️', name: 'JS Master',          desc: 'Termine le cours JS',       xp: 1000},
  { id: 'java_master',  icon: '☕', name: 'Java Master',        desc: 'Termine le cours Java',     xp: 1000},
  { id: 'web_master',   icon: '🌐', name: 'Web Master',         desc: 'Termine HTML/CSS',          xp: 1000},
  { id: 'polyglot',     icon: '🌍', name: 'Polyglotte',         desc: 'Termine 3+ langages',       xp: 500 },
  { id: 'challenger',   icon: '🏆', name: 'Champion',           desc: 'Gagne 5 défis',             xp: 500 },
  { id: 'streak_7',     icon: '🔥', name: 'Week Streak',        desc: '7 jours consécutifs',       xp: 7   },
  { id: 'streak_30',    icon: '💎', name: 'Month Streak',       desc: '30 jours consécutifs',      xp: 30  },
  { id: 'centurion',    icon: '💯', name: 'Centurion',          desc: '100 exercices complétés',   xp: 100 },
  { id: 'first_cert',   icon: '🎓', name: 'Premier Certif',     desc: 'Obtiens ton 1er certificat',xp: 1000},
  { id: 'mentor',       icon: '👨‍🏫', name: 'Mentor',            desc: 'Aide 10 personnes',         xp: 50  },
];

// ─── COURSE DATA ──────────────────────────────────────────────────────────────
const COURSES: Course[] = [
  {
    id: 'py', name: 'Python Débutant', lang: 'python', level: 'Débutant', hours: '40h', icon: '🐍', color: '#3b82f6', desc: 'Maîtrise Python de zéro à héros',
    chapters: [
      { id: 'py-c1', title: 'Syntaxe et Variables', quiz: [
          { q: 'Quel mot-clé affiche du texte en Python ?', opts: ['echo', 'print', 'console.log', 'say'], correct: 1 },
          { q: "x = 5 + '3' lève une :", opts: ['SyntaxError', 'TypeError', 'ValueError', 'NameError'], correct: 1 },
          { q: "type(3.14) retourne :", opts: ['int', 'float', 'double', 'number'], correct: 1 },
          { q: "Python est case-sensitive ?", opts: ['Oui', 'Non', 'Parfois', 'Dépend'], correct: 0 },
          { q: "# en Python c'est un :", opts: ['Opérateur', 'Commentaire', 'String', 'Décorateur'], correct: 1 },
        ],
        lessons: [
          { id: 'py-c1-l1', title: 'Afficher et Variables', lang: 'python', content: "Python est un langage de haut niveau. Utilise `print()` pour afficher du texte. Les variables n'ont pas besoin de déclaration de type.", code: '# Premier programme\nnom = "Alice"\nage = 25\nprint(f"Bonjour {nom}, tu as {age} ans")\n\n# Types de base\nx = 10        # int\ny = 3.14      # float\nbool_val = True  # bool\ntexte = "hello"  # str\n\nprint(type(x))', exercises: [
            { id: 'py-c1-l1-e1', title: 'Hello World', desc: 'Affiche "Bonjour Léa" en Python', level: 'easy', lang: 'python', starter: '# Affiche Bonjour Léa\n', hint: 'Utilise la fonction print()', tests: [{ input: '', expected: 'Bonjour Léa' }] },
            { id: 'py-c1-l1-e2', title: 'Calcul simple', desc: 'Calcule et affiche 42 * 7', level: 'easy', lang: 'python', starter: '# Calcule 42 * 7\n', hint: 'Utilise print(42 * 7)', tests: [{ input: '', expected: '294' }] },
          ]},
          { id: 'py-c1-l2', title: 'Structures Conditionnelles', lang: 'python', content: "Les conditions avec `if/elif/else` permettent de prendre des décisions dans le code selon des valeurs booléennes.", code: 'age = 18\nif age >= 18:\n    print("Majeur")\nelif age >= 13:\n    print("Adolescent")\nelse:\n    print("Enfant")\n\n# Opérateurs logiques\nx = 5\nif x > 0 and x < 10:\n    print("x est entre 0 et 10")', exercises: [
            { id: 'py-c1-l2-e1', title: 'Pair ou Impair', desc: 'Écris une fonction pair_impair(n) qui retourne "pair" ou "impair"', level: 'easy', lang: 'python', starter: 'def pair_impair(n):\n    # Ton code ici\n    pass\n', hint: 'Utilise le modulo %', tests: [{ input: 'pair_impair(4)', expected: 'pair' }, { input: 'pair_impair(7)', expected: 'impair' }] },
          ]},
        ]
      },
      { id: 'py-c2', title: 'Boucles et Listes', quiz: [
          { q: "range(5) génère combien de nombres ?", opts: ['4', '5', '6', '1-5'], correct: 1 },
          { q: "Accéder au dernier élément d'une liste lst :", opts: ['lst[-1]', 'lst[last]', 'lst.last()', 'lst.end()'], correct: 0 },
          { q: "Ajouter un élément à une liste :", opts: ['lst.add()', 'lst.push()', 'lst.append()', 'lst.insert()'], correct: 2 },
          { q: "for i in range(3) : i prend les valeurs ?", opts: ['1,2,3', '0,1,2', '0,1,2,3', '1,2'], correct: 1 },
          { q: "len([1,2,3]) retourne :", opts: ['2', '3', '4', '1'], correct: 1 },
        ],
        lessons: [
          { id: 'py-c2-l1', title: 'Boucles for/while', lang: 'python', content: "Les boucles permettent de répéter des instructions. `for` est idéal pour itérer sur des séquences, `while` pour des conditions arbitraires.", code: '# Boucle for\nfor i in range(5):\n    print(i)  # 0,1,2,3,4\n\n# Boucle while\nx = 0\nwhile x < 3:\n    print(x)\n    x += 1\n\n# Itérer sur une liste\nfruits = ["pomme", "banane", "cerise"]\nfor fruit in fruits:\n    print(fruit)', exercises: [
            { id: 'py-c2-l1-e1', title: 'Somme 1 à N', desc: 'Écris une fonction somme(n) qui retourne la somme de 1 à n', level: 'easy', lang: 'python', starter: 'def somme(n):\n    # Ton code ici\n    pass\n', hint: 'Utilise une boucle for avec range(1, n+1)', tests: [{ input: 'somme(5)', expected: '15' }, { input: 'somme(10)', expected: '55' }] },
            { id: 'py-c2-l1-e2', title: 'Fibonacci', desc: 'Écris une fonction fib(n) qui retourne le Nième nombre de Fibonacci', level: 'medium', lang: 'python', starter: 'def fib(n):\n    # fib(0)=0, fib(1)=1, fib(n)=fib(n-1)+fib(n-2)\n    pass\n', hint: 'Commence par les cas de base n<=1', tests: [{ input: 'fib(0)', expected: '0' }, { input: 'fib(5)', expected: '5' }, { input: 'fib(10)', expected: '55' }] },
          ]},
          { id: 'py-c2-l2', title: 'Listes et Compréhensions', lang: 'python', content: "Les listes Python sont dynamiques. Les list comprehensions permettent de créer des listes de façon concise et pythonique.", code: 'nombres = [1, 2, 3, 4, 5]\ncarres = [x**2 for x in nombres]\nprint(carres)  # [1, 4, 9, 16, 25]\n\n# Filtrage\npairs = [x for x in range(10) if x % 2 == 0]\nprint(pairs)   # [0, 2, 4, 6, 8]\n\n# Tri\nlst = [3, 1, 4, 1, 5]\nlst.sort()\nprint(lst)  # [1, 1, 3, 4, 5]', exercises: [
            { id: 'py-c2-l2-e1', title: 'Nombres pairs', desc: 'Retourne la liste des nombres pairs entre 1 et n (inclus)', level: 'easy', lang: 'python', starter: 'def pairs(n):\n    pass\n', hint: 'List comprehension avec condition if', tests: [{ input: 'pairs(6)', expected: '[2, 4, 6]' }, { input: 'pairs(10)', expected: '[2, 4, 6, 8, 10]' }] },
          ]},
        ]
      },
      { id: 'py-c3', title: 'Fonctions et Modules', quiz: [
          { q: "Définir une fonction en Python :", opts: ['function', 'def', 'fn', 'func'], correct: 1 },
          { q: "Importer le module math :", opts: ['#include math', 'import math', 'use math', 'require math'], correct: 1 },
          { q: "lambda x: x*2 est une :", opts: ['Classe', 'Méthode', 'Fonction anonyme', 'Variable'], correct: 2 },
          { q: "*args permet de passer :", opts: ['Des mots-clés', 'Des arguments positionnels', 'Des dictionnaires', 'Une liste nommée'], correct: 1 },
          { q: "return sans valeur retourne :", opts: ['0', 'False', 'None', 'Erreur'], correct: 2 },
        ],
        lessons: [
          { id: 'py-c3-l1', title: 'Fonctions avancées', lang: 'python', content: "Les fonctions en Python supportent les valeurs par défaut, *args, **kwargs, et les lambdas pour une grande flexibilité.", code: 'def saluer(nom, titre="M."):\n    return f"Bonjour {titre} {nom}"\n\n# Args variables\ndef addition(*nombres):\n    return sum(nombres)\n\nprint(addition(1, 2, 3))  # 6\n\n# Lambda\ncarre = lambda x: x ** 2\nprint(carre(4))  # 16\n\n# Map/Filter\ndoubles = list(map(lambda x: x*2, [1,2,3]))\nprint(doubles)  # [2, 4, 6]', exercises: [
            { id: 'py-c3-l1-e1', title: 'Factorielle', desc: 'Écris une fonction factorielle(n) récursive', level: 'medium', lang: 'python', starter: 'def factorielle(n):\n    # Cas de base: factorielle(0) = 1\n    pass\n', hint: 'n! = n * (n-1)!', tests: [{ input: 'factorielle(0)', expected: '1' }, { input: 'factorielle(5)', expected: '120' }, { input: 'factorielle(10)', expected: '3628800' }] },
            { id: 'py-c3-l1-e2', title: 'Palindrome', desc: 'Vérifie si un mot est un palindrome (ex: "radar")', level: 'medium', lang: 'python', starter: 'def est_palindrome(mot):\n    pass\n', hint: 'Comparer le mot à son inverse mot[::-1]', tests: [{ input: 'est_palindrome("radar")', expected: 'True' }, { input: 'est_palindrome("python")', expected: 'False' }] },
          ]},
        ]
      }
    ]
  },
  {
    id: 'js', name: 'JavaScript Débutant', lang: 'javascript', level: 'Débutant', hours: '35h', icon: '⚙️', color: '#f59e0b', desc: 'Du DOM aux Promises en passant par ES6+',
    chapters: [
      { id: 'js-c1', title: 'Syntaxe JS & ES6+', quiz: [
          { q: "Déclarer une constante :", opts: ['var', 'let', 'const', 'fixed'], correct: 2 },
          { q: "Résultat de typeof null :", opts: ['null', 'undefined', 'object', 'boolean'], correct: 2 },
          { q: "Arrow function correcte :", opts: ['x -> x*2', 'x => x*2', 'fn(x) x*2', '(x): x*2'], correct: 1 },
          { q: "Template literal :", opts: ['${var}', '#{var}', '@{var}', '{{var}}'], correct: 0 },
          { q: "Destructuration array :", opts: ['[a, b] = arr', '{a, b} = arr', 'a,b = arr', 'var[a,b]=arr'], correct: 0 },
        ],
        lessons: [
          { id: 'js-c1-l1', title: 'Variables et Types', lang: 'javascript', content: "JavaScript moderne utilise `const` et `let` (plus `var`). Les types sont dynamiques et incluent string, number, boolean, null, undefined, object, symbol.", code: 'const nom = "Alice";\nlet age = 25;\n\n// Template literals\nconsole.log(`Bonjour ${nom}!`);\n\n// Destructuration\nconst [x, y] = [1, 2];\nconst { a, b } = { a: 10, b: 20 };\n\n// Spread\nconst arr = [...[1,2], ...[3,4]]; // [1,2,3,4]\n\n// Nullish coalescing\nconst val = null ?? "default"; // "default"', exercises: [
            { id: 'js-c1-l1-e1', title: 'Somme Array', desc: 'Écris une fonction somme(arr) qui retourne la somme de tous les éléments', level: 'easy', lang: 'javascript', starter: 'function somme(arr) {\n  // Ton code ici\n}\n', hint: 'Utilise arr.reduce()', tests: [{ input: 'somme([1,2,3,4])', expected: '10' }, { input: 'somme([0,-1,5])', expected: '4' }] },
          ]},
          { id: 'js-c1-l2', title: 'Async/Await et Promises', lang: 'javascript', content: "JavaScript est asynchrone par nature. Les Promises et async/await permettent de gérer les opérations asynchrones de manière propre.", code: '// Promise\nconst fetchData = () => new Promise((resolve, reject) => {\n  setTimeout(() => resolve("data"), 1000);\n});\n\n// Async/Await\nasync function main() {\n  try {\n    const data = await fetchData();\n    console.log(data);\n  } catch (err) {\n    console.error(err);\n  }\n}\n\nmain();', exercises: [
            { id: 'js-c1-l2-e1', title: 'Promise Chain', desc: 'Crée une promesse qui résout avec 42 après 100ms', level: 'medium', lang: 'javascript', starter: 'function maPromesse() {\n  // Retourne une promesse\n}\n', hint: 'new Promise(resolve => setTimeout(() => resolve(42), 100))', tests: [{ input: 'maPromesse().then(v => v)', expected: '42' }] },
          ]},
        ]
      },
      { id: 'js-c2', title: 'DOM & Events', quiz: [
          { q: "Sélectionner un élément par ID :", opts: ['getById()', 'getElementById()', 'select()', 'query()'], correct: 1 },
          { q: "Ajouter un event listener :", opts: ['on()', 'bind()', 'addEventListener()', 'listen()'], correct: 2 },
          { q: "Modifier le texte d'un élément :", opts: ['.html', '.innerText', '.value', '.content'], correct: 1 },
          { q: "event.preventDefault() empêche :", opts: ["L'événement de se propager", 'Le comportement par défaut', 'Les erreurs', "Le code de s'exécuter"], correct: 1 },
          { q: "document.querySelector() sélectionne :", opts: ['Tous les éléments', 'Le premier élément', "L'ID", 'La classe seule'], correct: 1 },
        ],
        lessons: [
          { id: 'js-c2-l1', title: 'Manipulation DOM', lang: 'javascript', content: "Le DOM (Document Object Model) est une interface permettant à JavaScript de modifier dynamiquement le contenu et la structure HTML.", code: '// Sélection\nconst btn = document.getElementById("monBouton");\nconst div = document.querySelector(".maClasse");\n\n// Modification\nbtn.textContent = "Clique moi!";\ndiv.style.color = "red";\n\n// Créer des éléments\nconst p = document.createElement("p");\np.textContent = "Nouveau paragraphe";\ndocument.body.appendChild(p);\n\n// Events\nbtn.addEventListener("click", (e) => {\n  console.log("Cliqué!", e.target);\n});', exercises: [
            { id: 'js-c2-l1-e1', title: 'Compte les clics', desc: "Crée une variable compteur qui s'incrémente à chaque appel de incrément()", level: 'easy', lang: 'javascript', starter: 'let compteur = 0;\nfunction incrément() {\n  // Incrémenter compteur\n}\n', hint: 'compteur++', tests: [{ input: 'incrément(); incrément(); compteur', expected: '2' }] },
          ]},
        ]
      },
    ]
  },
  {
    id: 'java', name: 'Java Débutant', lang: 'java', level: 'Débutant', hours: '45h', icon: '☕', color: '#ef4444', desc: 'POO et programmation orientée objet en Java',
    chapters: [
      { id: 'java-c1', title: 'Syntaxe et Types', quiz: [
          { q: "Point d'entrée d'un programme Java :", opts: ['start()', 'main()', 'run()', 'init()'], correct: 1 },
          { q: "Afficher en Java :", opts: ['print()', 'echo()', 'System.out.println()', 'Console.log()'], correct: 2 },
          { q: "Type entier 64 bits :", opts: ['int', 'long', 'Integer', 'BigInt'], correct: 1 },
          { q: "String en Java est :", opts: ['Mutable', 'Primitif', 'Immuable', 'Nullable par défaut'], correct: 2 },
          { q: "Compiler un fichier Java :", opts: ['java', 'javac', 'jvm', 'compile'], correct: 1 },
        ],
        lessons: [
          { id: 'java-c1-l1', title: 'Hello World & Bases', lang: 'java', content: 'Java est un langage compilé vers bytecode JVM. Tout le code doit être dans une classe. Les types primitifs : int, double, boolean, char, long, etc.', code: 'public class Main {\n    public static void main(String[] args) {\n        // Variables\n        int age = 25;\n        double pi = 3.14;\n        boolean actif = true;\n        String nom = "Alice";\n\n        // Affichage\n        System.out.println("Nom: " + nom);\n        System.out.printf("Age: %d, Pi: %.2f%n", age, pi);\n\n        // Conversion\n        String s = String.valueOf(age); // "25"\n        int n = Integer.parseInt("42"); // 42\n    }\n}', exercises: [
            { id: 'java-c1-l1-e1', title: 'Puissance', desc: 'Calcule et retourne base^exposant (sans Math.pow)', level: 'medium', lang: 'java', starter: 'public static long puissance(int base, int exp) {\n    // Ton code ici\n    return 0;\n}\n', hint: 'Utilise une boucle for et multiplie base exp fois', tests: [{ input: 'puissance(2,10)', expected: '1024' }, { input: 'puissance(3,4)', expected: '81' }] },
          ]},
        ]
      },
      { id: 'java-c2', title: 'Programmation Orientée Objet', quiz: [
          { q: "Héritance en Java :", opts: ['inherits', 'extends', 'implements', 'super'], correct: 1 },
          { q: "Méthode qui ne peut être overridée :", opts: ['static', 'private', 'final', 'abstract'], correct: 2 },
          { q: "Constructeur d'une classe Animal :", opts: ['void Animal()', 'Animal()', 'new Animal()', 'Animal Animal()'], correct: 1 },
          { q: "Interface en Java :", opts: ['abstract class', 'interface', 'trait', 'protocol'], correct: 1 },
          { q: "Encapsulation signifie :", opts: ['Héritage multiple', 'Cacher les détails internes', 'Polymorphisme', 'Récursion'], correct: 1 },
        ],
        lessons: [
          { id: 'java-c2-l1', title: 'Classes et Objets', lang: 'java', content: "La POO organise le code en objets avec des attributs (données) et des méthodes (comportements). Les modificateurs d'accès contrôlent la visibilité.", code: 'public class Animal {\n    private String nom;\n    private int age;\n\n    public Animal(String nom, int age) {\n        this.nom = nom;\n        this.age = age;\n    }\n\n    public String getNom() { return nom; }\n    public void setAge(int age) { this.age = Math.max(0, age); }\n\n    @Override\n    public String toString() {\n        return nom + " (" + age + " ans)";\n    }\n}\n\n// Usage:\nAnimal chat = new Animal("Minou", 3);\nSystem.out.println(chat); // Minou (3 ans)', exercises: [
            { id: 'java-c2-l1-e1', title: 'Classe Cercle', desc: 'Crée une classe Cercle avec rayon et méthode aire() (π*r²)', level: 'medium', lang: 'java', starter: 'public class Cercle {\n    private double rayon;\n    // Constructeur + méthode aire()\n}\n', hint: 'Math.PI * rayon * rayon', tests: [{ input: 'new Cercle(5).aire()', expected: '78.54' }] },
          ]},
        ]
      },
    ]
  },
  {
    id: 'web', name: 'HTML/CSS Débutant', lang: 'html-css', level: 'Débutant', hours: '25h', icon: '🌐', color: '#8b5cf6', desc: 'Structure web, layouts modernes avec Flexbox & Grid',
    chapters: [
      { id: 'web-c1', title: 'HTML5 & Sémantique', quiz: [
          { q: "Balise de titre principal :", opts: ['<title>', '<h1>', '<header>', '<main>'], correct: 1 },
          { q: "Balise sémantique pour navigation :", opts: ['<div>', '<nav>', '<menu>', '<ul>'], correct: 1 },
          { q: "Attribut obligatoire de <img> :", opts: ['src', 'href', 'alt', 'src et alt'], correct: 3 },
          { q: "Fermeture de balise auto-fermante :", opts: ['<br>', '<br/>', 'Les deux', '<break>'], correct: 2 },
          { q: "Lien vers une page externe :", opts: ['<link>', '<a href>', '<url>', '<goto>'], correct: 1 },
        ],
        lessons: [
          { id: 'web-c1-l1', title: 'Structure HTML5', lang: 'html', content: "HTML5 structure le contenu web avec des balises sémantiques. La sémantique améliore l'accessibilité et le SEO. Utilise les bonnes balises pour le bon contenu.", code: '<!DOCTYPE html>\n<html lang="fr">\n<head>\n  <meta charset="UTF-8">\n  <meta name="viewport" content="width=device-width, initial-scale=1.0">\n  <title>Ma Page</title>\n</head>\n<body>\n  <header>\n    <nav>\n      <a href="/">Accueil</a>\n      <a href="/about">À propos</a>\n    </nav>\n  </header>\n  <main>\n    <article>\n      <h1>Mon Article</h1>\n      <p>Contenu...</p>\n    </article>\n  </main>\n  <footer>\n    <p>© 2026 Léa Academy</p>\n  </footer>\n</body>\n</html>', exercises: [
            { id: 'web-c1-l1-e1', title: 'Formulaire Contact', desc: 'Crée un formulaire HTML avec: prénom, email, message, bouton Envoyer', level: 'easy', lang: 'html', starter: '<form>\n  <!-- Ajoute les champs ici -->\n</form>\n', hint: 'Utilise <input type="text">, <input type="email">, <textarea> et <button>', tests: [{ input: '<form>', expected: 'form' }] },
          ]},
        ]
      },
      { id: 'web-c2', title: 'CSS Flexbox & Grid', quiz: [
          { q: "Centrer horizontalement avec Flexbox :", opts: ['align-items: center', 'justify-content: center', 'text-align: center', 'margin: auto'], correct: 1 },
          { q: "Grid : 3 colonnes égales :", opts: ['columns: 3', 'grid-template-columns: 1fr 1fr 1fr', 'flex: 1 1 33%', 'display: 3-column'], correct: 1 },
          { q: "flex-direction: column :", opts: ['Éléments en ligne', 'Éléments en colonne', 'Éléments enroulés', "Pas d'effet"], correct: 1 },
          { q: "gap en CSS Grid :", opts: ['margin entre colonnes', 'Espacement entre cellules', 'Padding interne', 'Border'], correct: 1 },
          { q: "@media query pour mobile :", opts: ['@mobile', '@screen', '@media (max-width: 768px)', '@responsive'], correct: 2 },
        ],
        lessons: [
          { id: 'web-c2-l1', title: 'Flexbox Maîtrise', lang: 'css', content: "Flexbox est un système de layout 1D. Il distribue l'espace le long d'un axe principal (row/column). Indispensable pour les interfaces modernes.", code: '.container {\n  display: flex;\n  justify-content: space-between; /* axe principal */\n  align-items: center;           /* axe croisé */\n  flex-wrap: wrap;               /* retour à la ligne */\n  gap: 1rem;                     /* espacement */\n}\n\n.item {\n  flex: 1 1 200px; /* grow shrink basis */\n}\n\n/* Centrer parfaitement */\n.center {\n  display: flex;\n  justify-content: center;\n  align-items: center;\n}', exercises: [
            { id: 'web-c2-l1-e1', title: 'Navbar Flexbox', desc: 'Crée une navbar avec logo à gauche et liens à droite via Flexbox', level: 'easy', lang: 'css', starter: 'nav {\n  display: flex;\n  /* Ajoute les propriétés */\n}\n', hint: 'justify-content: space-between', tests: [{ input: 'nav style', expected: 'flex' }] },
          ]},
        ]
      },
    ]
  },
  {
    id: 'cpp', name: 'C++ Intermédiaire', lang: 'cpp', level: 'Intermédiaire', hours: '50h', icon: '⚡', color: '#10b981', desc: 'Pointeurs, mémoire, STL et optimisation',
    chapters: [
      { id: 'cpp-c1', title: 'Pointeurs et Mémoire', quiz: [
          { q: "& en C++ signifie :", opts: ['ET logique', 'Adresse de', 'Référence', '& et adresse de'], correct: 3 },
          { q: "Déréférencer un pointeur :", opts: ['*ptr', '&ptr', 'ptr->', 'ptr[]'], correct: 0 },
          { q: "Allocation dynamique :", opts: ['malloc()', 'new', 'alloc()', 'create()'], correct: 1 },
          { q: "Libérer mémoire allouée avec new :", opts: ['free()', 'delete', 'remove()', 'dealloc()'], correct: 1 },
          { q: "nullptr en C++11 remplace :", opts: ['0', 'NULL', 'void*', '0 et NULL'], correct: 3 },
        ],
        lessons: [
          { id: 'cpp-c1-l1', title: 'Pointeurs Essentiels', lang: 'cpp', content: "Les pointeurs stockent des adresses mémoire. Ils permettent la manipulation directe de la mémoire, l'allocation dynamique et le passage par référence efficace.", code: '#include <iostream>\nusing namespace std;\n\nint main() {\n    int x = 42;\n    int* ptr = &x;  // ptr pointe sur x\n\n    cout << x << endl;     // 42\n    cout << &x << endl;    // adresse\n    cout << ptr << endl;   // même adresse\n    cout << *ptr << endl;  // 42 (déréférence)\n\n    *ptr = 100;  // modifie x via le pointeur\n    cout << x << endl;  // 100\n\n    // Allocation dynamique\n    int* arr = new int[5];\n    arr[0] = 1;\n    delete[] arr;  // libérer!\n}', exercises: [
            { id: 'cpp-c1-l1-e1', title: 'Swap avec pointeurs', desc: 'Écris une fonction swap(int* a, int* b) qui échange les valeurs de a et b', level: 'medium', lang: 'cpp', starter: 'void swap(int* a, int* b) {\n    // Ton code ici\n}\n', hint: 'Utilise une variable temporaire int tmp = *a;', tests: [{ input: 'int a=5,b=3; swap(&a,&b); a', expected: '3' }, { input: 'int a=5,b=3; swap(&a,&b); b', expected: '5' }] },
          ]},
        ]
      },
    ]
  }
];

// ─── CHALLENGES ───────────────────────────────────────────────────────────────
const CHALLENGES: Challenge[] = [
  { id: 'ch1', title: 'Nombres Premiers', desc: 'Écris une fonction est_premier(n) qui retourne True si n est premier', difficulty: 'Facile', lang: 'python', xp: 200, starter: 'def est_premier(n):\n    pass\n', tests: [{ input: 'est_premier(7)', expected: 'True' }, { input: 'est_premier(4)', expected: 'False' }, { input: 'est_premier(2)', expected: 'True' }] },
  { id: 'ch2', title: 'Anagramme', desc: "Vérifie si deux mots sont des anagrammes l'un de l'autre", difficulty: 'Facile', lang: 'python', xp: 200, starter: 'def sont_anagrammes(a, b):\n    pass\n', tests: [{ input: 'sont_anagrammes("listen","silent")', expected: 'True' }, { input: 'sont_anagrammes("hello","world")', expected: 'False' }] },
  { id: 'ch3', title: 'Tri Rapide', desc: 'Implémente le quicksort en Python', difficulty: 'Difficile', lang: 'python', xp: 500, starter: 'def quicksort(lst):\n    if len(lst) <= 1:\n        return lst\n    # Ton code ici\n    pass\n', tests: [{ input: 'quicksort([3,1,4,1,5,9])', expected: '[1, 1, 3, 4, 5, 9]' }, { input: 'quicksort([])', expected: '[]' }] },
  { id: 'ch4', title: 'Inverser une String', desc: 'Inverse une chaîne de caractères sans utiliser [::-1]', difficulty: 'Facile', lang: 'python', xp: 150, starter: 'def inverser(s):\n    pass\n', tests: [{ input: 'inverser("hello")', expected: 'olleh' }, { input: 'inverser("python")', expected: 'nohtyp' }] },
  { id: 'ch5', title: 'FizzBuzz', desc: 'FizzBuzz de 1 à n. Retourne une liste ["Fizz","Buzz","FizzBuzz", chiffre]', difficulty: 'Facile', lang: 'python', xp: 100, starter: 'def fizzbuzz(n):\n    pass\n', tests: [{ input: 'fizzbuzz(15)[2]', expected: 'Fizz' }, { input: 'fizzbuzz(15)[4]', expected: 'Buzz' }, { input: 'fizzbuzz(15)[14]', expected: 'FizzBuzz' }] },
];

// ─── PATHS ────────────────────────────────────────────────────────────────────
const PATHS = [
  { id: 'web-dev',   icon: '🌐', name: 'Web Developer',     courses: ['web','js'],        hours: '60h',  desc: 'HTML → CSS → JS → React, le chemin complet du web frontend' },
  { id: 'backend',   icon: '⚙️', name: 'Backend Developer', courses: ['py','java'],       hours: '85h',  desc: 'Python + Java pour le backend, APIs et bases de données' },
  { id: 'fullstack', icon: '🚀', name: 'Full Stack',        courses: ['web','js','py'],   hours: '100h', desc: 'Frontend + Backend, devenez développeur complet' },
  { id: 'algo',      icon: '🧮', name: 'Algorithm Master',  courses: ['py','cpp'],        hours: '90h',  desc: 'Structures de données et algorithmes pour les entretiens tech' },
];

// ─── HOOK: Progress ───────────────────────────────────────────────────────────
const DEFAULT_PROGRESS: Progress = { xp: 0, done: [], badges: [], streak: 0, lastDate: '', lines: 0, tests: 0 };

function useProgress() {
  const [prog, setProg] = useState<Progress>(() => load(DEFAULT_PROGRESS));

  const addXP = useCallback((amount: number) => {
    setProg(p => { const n = { ...p, xp: p.xp + amount }; save(n); return n; });
  }, []);

  const markDone = useCallback((id: string, xp = 50) => {
    setProg(p => {
      if (p.done.includes(id)) return p;
      const n = { ...p, done: [...p.done, id], xp: p.xp + xp };
      save(n); return n;
    });
  }, []);

  const earnBadge = useCallback((id: string) => {
    setProg(p => {
      if (p.badges.includes(id)) return p;
      const n = { ...p, badges: [...p.badges, id] };
      save(n); return n;
    });
  }, []);

  const addLines = useCallback((n: number) => {
    setProg(p => { const u = { ...p, lines: p.lines + n }; save(u); return u; });
  }, []);

  return { prog, addXP, markDone, earnBadge, addLines };
}

// ─── COMPOSANTS ──────────────────────────────────────────────────────────────
const XpBar: React.FC<{ xp: number }> = ({ xp }) => {
  const l = getLvl(xp); const p = pct(xp);
  return (
    <div className="flex items-center gap-3 bg-black/30 rounded-2xl px-4 py-2.5 border border-white/10">
      <Zap size={14} className="shrink-0" style={{ color: l.color }}/>
      <div className="flex-1">
        <div className="flex justify-between items-baseline mb-1">
          <span className="text-[10px] font-black uppercase tracking-widest" style={{ color: l.color }}>Niv. {getLevel(xp)+1} · {l.name}</span>
          <span className="text-[9px] text-slate-500">{xp.toLocaleString()} XP</span>
        </div>
        <div className="h-1.5 bg-white/5 rounded-full"><div className="h-full rounded-full transition-all duration-500" style={{ width: `${p}%`, background: l.color }}/></div>
      </div>
    </div>
  );
};

const DiffBadge: React.FC<{ level: string }> = ({ level }) => {
  const cfg: Record<string, string> = { easy: 'bg-emerald-500/20 text-emerald-400', medium: 'bg-amber-500/20 text-amber-400', hard: 'bg-red-500/20 text-red-400', Facile: 'bg-emerald-500/20 text-emerald-400', Difficile: 'bg-red-500/20 text-red-400', Intermédiaire: 'bg-amber-500/20 text-amber-400' };
  return <span className={`text-[9px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full ${cfg[level] ?? 'bg-slate-500/20 text-slate-400'}`}>{level}</span>;
};

const CodeEditor: React.FC<{ code: string; onChange: (v: string) => void; language: string; height?: string }> = ({ code, onChange, language, height = 'h-52' }) => {
  const ta = useRef<HTMLTextAreaElement>(null);
  const handleTab = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key !== 'Tab') return;
    e.preventDefault();
    const s = e.currentTarget.selectionStart;
    const newVal = code.slice(0, s) + '    ' + code.slice(e.currentTarget.selectionEnd);
    onChange(newVal);
    requestAnimationFrame(() => { if (ta.current) { ta.current.selectionStart = ta.current.selectionEnd = s + 4; } });
  };
  return (
    <div className={`relative ${height} rounded-xl overflow-hidden border border-white/10 bg-[#1e1e1e]`}>
      <div className="absolute top-0 left-0 right-0 flex items-center gap-1.5 px-3 py-1.5 bg-[#2d2d2d] border-b border-white/5 z-10">
        <div className="w-2.5 h-2.5 rounded-full bg-red-500/60"/><div className="w-2.5 h-2.5 rounded-full bg-yellow-500/60"/><div className="w-2.5 h-2.5 rounded-full bg-green-500/60"/>
        <span className="text-[9px] text-slate-500 ml-2 uppercase tracking-widest">{language}</span>
      </div>
      <textarea
        ref={ta} value={code} onChange={e => onChange(e.target.value)} onKeyDown={handleTab}
        className="absolute inset-0 top-7 w-full h-[calc(100%-1.75rem)] font-mono text-sm text-[#d4d4d4] bg-transparent p-4 outline-none resize-none leading-6 z-20"
        spellCheck={false} autoComplete="off" autoCapitalize="off"
      />
    </div>
  );
};

// ─── QUIZ MODAL ───────────────────────────────────────────────────────────────
const QuizModal: React.FC<{ questions: QuizQ[]; onClose: (passed: boolean) => void }> = ({ questions, onClose }) => {
  const [qi, setQi] = useState(0);
  const [score, setScore] = useState(0);
  const [chosen, setChosen] = useState<number | null>(null);
  const [done, setDone] = useState(false);
  const [secs, setSecs] = useState(20);

  useEffect(() => {
    if (done) return;
    const t = setInterval(() => setSecs(s => { if (s <= 1) { next(null); return 20; } return s - 1; }), 1000);
    return () => clearInterval(t);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [qi, done]);

  const next = (sel: number | null) => {
    const correct = questions[qi].correct;
    const pts = sel === correct ? 1 : 0;
    const newScore = score + pts;
    if (qi + 1 >= questions.length) { setScore(newScore); setDone(true); }
    else { setScore(newScore); setQi(qi + 1); setChosen(null); setSecs(20); }
  };

  const passed = score / questions.length >= 0.7;

  return (
    <div className="fixed inset-0 z-[80] bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#000b1e] border border-white/10 rounded-3xl p-8 w-full max-w-lg shadow-2xl">
        {!done ? (
          <>
            <div className="flex justify-between items-center mb-6">
              <span className="text-[10px] font-black uppercase tracking-widest text-[#00f2ff]">Question {qi + 1}/{questions.length}</span>
              <div className={`flex items-center gap-1.5 text-sm font-black ${secs <= 5 ? 'text-red-400 animate-pulse' : 'text-slate-400'}`}>
                <Timer size={14}/> {secs}s
              </div>
            </div>
            <div className="h-1 bg-white/5 rounded-full mb-6"><div className="h-full bg-[#00f2ff] rounded-full transition-all" style={{ width: `${(qi / questions.length) * 100}%` }}/></div>
            <p className="text-white font-bold text-lg mb-6">{questions[qi].q}</p>
            <div className="space-y-3">
              {questions[qi].opts.map((opt, i) => (
                <button key={i} onClick={() => { setChosen(i); setTimeout(() => next(i), 600); }}
                  className={`w-full text-left p-4 rounded-xl border text-sm font-medium transition-all ${
                    chosen === null ? 'border-white/10 hover:border-[#00f2ff]/50 hover:bg-[#00f2ff]/5 text-slate-200' :
                    chosen === i && i === questions[qi].correct ? 'border-emerald-500 bg-emerald-500/20 text-emerald-400' :
                    chosen === i ? 'border-red-500 bg-red-500/20 text-red-400' :
                    i === questions[qi].correct ? 'border-emerald-500/50 text-emerald-400' : 'border-white/5 text-slate-600'
                  }`}>{String.fromCharCode(65 + i)}. {opt}
                </button>
              ))}
            </div>
          </>
        ) : (
          <div className="text-center">
            <div className="text-6xl mb-4">{passed ? '🎉' : '😅'}</div>
            <h3 className="text-2xl font-black text-white mb-2">{passed ? 'Réussi !' : 'Pas tout à fait…'}</h3>
            <p className="text-slate-400 mb-4">{score}/{questions.length} bonnes réponses ({Math.round(score/questions.length*100)}%)</p>
            <p className={`text-sm font-bold mb-8 ${passed ? 'text-emerald-400' : 'text-amber-400'}`}>
              {passed ? '✅ Chapitre débloqué ! +200 XP' : `⚠️ Score minimum: 70%. Encore ${Math.ceil(questions.length * 0.7) - score} bonne(s) réponse(s).`}
            </p>
            <button onClick={() => onClose(passed)}
              className={`w-full py-3 rounded-xl font-black text-sm ${passed ? 'bg-[#00f2ff] text-black' : 'bg-white/10 text-white hover:bg-white/20'} transition-all`}>
              {passed ? 'Continuer →' : 'Réessayer'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

// ─── MAIN COMPONENT ───────────────────────────────────────────────────────────
export const LeaAcademy = () => {
  const { prog, addXP, markDone, earnBadge, addLines } = useProgress();

  const [tab, setTab]  = useState<'courses'|'challenges'|'paths'|'stats'|'leaderboard'>('courses');
  const [course, setCourse] = useState<Course | null>(null);
  const [chIdx,  setChIdx]  = useState(0);
  const [lesIdx, setLesIdx] = useState(0);
  const [exIdx,  setExIdx]  = useState<number | null>(null);
  const [code, setCode]     = useState('');
  const [output, setOutput] = useState('');
  const [running, setRunning] = useState(false);
  const [quizOpen, setQuizOpen] = useState(false);
  const [unlockBadge, setUnlockBadge] = useState<string | null>(null);
  const [challenge, setChallenge] = useState<Challenge | null>(null);
  const [board, setBoard] = useState<any[]>([]);
  const [hint, setHint] = useState(false);

  const lesson   = course?.chapters[chIdx]?.lessons[lesIdx];
  const exercise = exIdx !== null ? lesson?.exercises[exIdx] : null;
  const chapter  = course?.chapters[chIdx];

  useEffect(() => {
    api('/leaderboard').then(d => d && setBoard(d));
  }, []);

  const runCode = async (src: string, lang: string, tests: TestCase[]) => {
    setRunning(true); setOutput('⏳ Exécution...');
    addLines(src.split('\n').length);
    const result = await api('/run-code', { code: src, language: lang, testCases: tests });
    if (result) {
      setOutput(result.output || '');
      return result;
    }
    setOutput('⚠️ Serveur inaccessible. Exécution bloquée : connexion au Master requise.');
    setRunning(false); return null;
  };

  const submitExercise = async () => {
    if (!exercise || !lesson) return;
    const result = await runCode(code, exercise.lang, exercise.tests);
    setRunning(false);
    if (!result) return;
    const passed = result.passed ?? result.results?.filter((r: { passed: boolean }) => r.passed).length ?? 0;
    const total  = exercise.tests.length;
    if (passed === total) {
      markDone(exercise.id, 100);
      addXP(100);
      if (prog.tests + total >= 10) earnBadge('bug_hunter');
      if (!prog.done.length) { earnBadge('first_code'); setUnlockBadge('first_code'); }
      setOutput(prev => prev + `\n\n✅ ${passed}/${total} tests réussis ! +100 XP`);
    } else {
      setOutput(prev => prev + `\n\n❌ ${passed}/${total} tests. Réessaie !`);
    }
  };

  const submitChallenge = async () => {
    if (!challenge) return;
    const result = await runCode(code, challenge.lang, challenge.tests);
    setRunning(false);
    if (!result) return;
    const passed = result.passed ?? challenge.tests.length;
    if (passed === challenge.tests.length) {
      markDone(challenge.id, challenge.xp);
      earnBadge('challenger');
      setOutput(prev => prev + `\n\n🏆 Défi réussi ! +${challenge.xp} XP`);
    }
  };

  const startChallenge = (ch: Challenge) => { setChallenge(ch); setCode(ch.starter); setOutput(''); };

  const openLesson = (c: Course, ci: number, li: number) => {
    setCourse(c); setChIdx(ci); setLesIdx(li); setExIdx(null);
    setCode(c.chapters[ci].lessons[li].code); setOutput(''); setHint(false);
  };

  const openExercise = (i: number) => {
    if (!lesson) return;
    setExIdx(i); setCode(lesson.exercises[i].starter); setOutput(''); setHint(false);
  };

  const earnBadgeVisible = (id: string) => {
    earnBadge(id);
    setUnlockBadge(id);
    setTimeout(() => setUnlockBadge(null), 3000);
  };

  const TABS = [
    { id: 'courses',     icon: <BookOpen size={15}/>,  label: 'Cours'      },
    { id: 'challenges',  icon: <Code2 size={15}/>,     label: 'Défis'      },
    { id: 'paths',       icon: <Map size={15}/>,       label: 'Parcours'   },
    { id: 'stats',       icon: <BarChart3 size={15}/>, label: 'Stats'      },
    { id: 'leaderboard', icon: <Trophy size={15}/>,    label: 'Classement' },
  ];

  return (
    <div className="w-full h-full pt-20 px-4 md:px-8 pb-8 flex flex-col gap-4 min-h-0">

      {/* Badge unlock toast */}
      {unlockBadge && (() => {
        const b = ALL_BADGES.find(x => x.id === unlockBadge);
        return b ? (
          <div className="fixed top-6 right-6 z-[90] bg-[#000b1e] border border-[#00f2ff]/40 rounded-2xl p-4 shadow-2xl flex items-center gap-3">
            <span className="text-3xl">{b.icon}</span>
            <div><p className="text-white font-black text-sm">Badge débloqué !</p><p className="text-[#00f2ff] text-xs">{b.name}</p></div>
          </div>
        ) : null;
      })()}

      {/* Header */}
      <div className="flex flex-col md:flex-row gap-3 items-start md:items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          {(course || challenge) && (
            <button onClick={() => { setCourse(null); setChallenge(null); }}
              className="p-2 text-slate-500 hover:text-white transition-colors"><ArrowLeft size={18}/></button>
          )}
          <div>
            <h1 className="text-2xl font-black text-white uppercase tracking-tighter">
              {course ? course.name : challenge ? '⚡ ' + challenge.title : '🎓 Léa Academy'}
            </h1>
            <p className="text-[10px] text-[#00f2ff] uppercase tracking-widest">
              {course ? chapter?.title : "Plateforme d'apprentissage souveraine"}
            </p>
          </div>
        </div>
        <XpBar xp={prog.xp}/>
      </div>

      {/* Tabs */}
      {!course && !challenge && (
        <div className="flex gap-1 bg-[#000b1e]/80 border border-white/10 rounded-2xl p-1.5 shrink-0">
          {TABS.map(t => (
            <button key={t.id} onClick={() => setTab(t.id as typeof tab)}
              className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${tab === t.id ? 'bg-[#00f2ff]/15 text-[#00f2ff] border border-[#00f2ff]/30' : 'text-slate-500 hover:text-slate-300'}`}>
              {t.icon} <span className="hidden sm:block">{t.label}</span>
            </button>
          ))}
        </div>
      )}

      {/* ── LESSON VIEW ── */}
      {course && lesson && (
        <div className="flex-1 flex flex-col lg:flex-row gap-4 min-h-0 overflow-hidden">
          {/* Sidebar */}
          <div className="w-full lg:w-[260px] bg-[#000b1e]/80 border border-white/10 rounded-2xl p-4 shrink-0 overflow-y-auto">
            <p className="text-[9px] font-black uppercase text-slate-500 tracking-widest mb-3">Chapitres</p>
            {course.chapters.map((ch, ci) => (
              <div key={ch.id} className="mb-3">
                <button onClick={() => openLesson(course, ci, 0)}
                  className={`w-full text-left text-xs font-bold px-3 py-2 rounded-xl mb-1 transition-all ${chIdx === ci ? 'bg-[#00f2ff]/15 text-[#00f2ff]' : 'text-slate-400 hover:text-white'}`}>
                  {ci + 1}. {ch.title}
                </button>
                {chIdx === ci && ch.lessons.map((ls, li) => (
                  <button key={ls.id} onClick={() => openLesson(course, ci, li)}
                    className={`w-full text-left text-[10px] pl-6 py-1.5 rounded-lg transition-all flex items-center gap-1.5 ${lesIdx === li ? 'text-white font-bold' : 'text-slate-500 hover:text-slate-300'}`}>
                    {prog.done.includes(ls.id)
                      ? <CheckCircle size={10} className="text-emerald-500 shrink-0"/>
                      : <div className="w-2.5 h-2.5 rounded-full border border-slate-600 shrink-0"/>}
                    {ls.title}
                  </button>
                ))}
              </div>
            ))}
          </div>

          {/* Main */}
          <div className="flex-1 flex flex-col gap-3 min-h-0 overflow-y-auto">
            {exIdx === null ? (
              <>
                <div className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-5 shrink-0">
                  <h2 className="text-lg font-black text-white mb-2">{lesson.title}</h2>
                  <p className="text-sm text-slate-300 leading-relaxed mb-4">{lesson.content}</p>
                  <p className="text-[9px] font-black uppercase text-[#00f2ff] mb-2 tracking-widest flex items-center gap-1.5"><Terminal size={11}/> Exemple</p>
                  <CodeEditor code={lesson.code} onChange={() => {}} language={lesson.lang} height="h-48"/>
                </div>

                <div className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-5 shrink-0">
                  <p className="text-[9px] font-black uppercase text-slate-500 tracking-widest mb-3">Exercices</p>
                  <div className="space-y-2">
                    {lesson.exercises.map((ex, i) => (
                      <button key={ex.id} onClick={() => openExercise(i)}
                        className="w-full flex items-center gap-3 p-3 bg-black/40 border border-white/5 hover:border-[#00f2ff]/30 rounded-xl transition-all text-left">
                        {prog.done.includes(ex.id)
                          ? <CheckCircle size={16} className="text-emerald-500 shrink-0"/>
                          : <Target size={16} className="text-slate-500 shrink-0"/>}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-bold text-white">{ex.title}</p>
                          <p className="text-[10px] text-slate-500 truncate">{ex.desc}</p>
                        </div>
                        <DiffBadge level={ex.level}/>
                        <ChevronRight size={14} className="text-slate-600 shrink-0"/>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="flex gap-2 shrink-0">
                  <button onClick={() => setQuizOpen(true)}
                    className="flex items-center gap-2 px-4 py-2.5 bg-indigo-500/15 border border-indigo-500/30 text-indigo-400 rounded-xl font-bold text-sm hover:bg-indigo-500/25 transition-all">
                    <Star size={15}/> Quiz du chapitre
                  </button>
                  <button onClick={() => { markDone(lesson.id, 50); addXP(50); }}
                    disabled={prog.done.includes(lesson.id)}
                    className="flex items-center gap-2 px-4 py-2.5 bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 rounded-xl font-bold text-sm hover:bg-emerald-500/25 transition-all disabled:opacity-50">
                    <CheckCircle size={15}/> {prog.done.includes(lesson.id) ? 'Terminé ✓' : 'Marquer terminé +50 XP'}
                  </button>
                </div>
              </>
            ) : exercise ? (
              <div className="flex flex-col gap-3">
                <div className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-5">
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h2 className="text-base font-black text-white">{exercise.title}</h2>
                      <p className="text-sm text-slate-300 mt-1">{exercise.desc}</p>
                    </div>
                    <DiffBadge level={exercise.level}/>
                  </div>
                  <div className="grid grid-cols-2 gap-2 mt-3">
                    {exercise.tests.map((t, i) => (
                      <div key={i} className="bg-black/40 border border-white/5 rounded-lg px-3 py-2">
                        <p className="text-[9px] text-slate-600 uppercase tracking-widest">Test {i+1}</p>
                        <p className="text-[10px] text-slate-400 font-mono">{t.input} → <span className="text-[#00f2ff]">{t.expected}</span></p>
                      </div>
                    ))}
                  </div>
                </div>

                <CodeEditor code={code} onChange={setCode} language={exercise.lang} height="h-56"/>

                <div className="flex gap-2">
                  <button onClick={() => { setRunning(true); runCode(code, exercise.lang, []).then(() => setRunning(false)); }}
                    disabled={running}
                    className="flex items-center gap-2 px-4 py-2.5 bg-[#00f2ff]/15 border border-[#00f2ff]/30 text-[#00f2ff] rounded-xl font-bold text-sm hover:bg-[#00f2ff]/25 transition-all disabled:opacity-50">
                    {running ? <RefreshCw size={14} className="animate-spin"/> : <Play size={14}/>} Exécuter
                  </button>
                  <button onClick={submitExercise} disabled={running}
                    className="flex items-center gap-2 px-4 py-2.5 bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 rounded-xl font-bold text-sm hover:bg-emerald-500/25 transition-all disabled:opacity-50">
                    <CheckCircle size={14}/> Soumettre
                  </button>
                  <button onClick={() => setHint(!hint)}
                    className="flex items-center gap-2 px-4 py-2.5 bg-amber-500/10 border border-amber-500/20 text-amber-400 rounded-xl font-bold text-sm hover:bg-amber-500/20 transition-all">
                    <Lightbulb size={14}/> Indice
                  </button>
                  <button onClick={() => { setCode(exercise.starter); setOutput(''); }}
                    className="flex items-center gap-2 px-4 py-2.5 bg-white/5 border border-white/10 text-slate-400 rounded-xl font-bold text-sm hover:bg-white/10 transition-all ml-auto">
                    <RefreshCw size={14}/> Reset
                  </button>
                </div>

                {hint && (
                  <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl px-4 py-3 text-amber-300 text-sm">
                    <span className="font-bold">💡 Indice : </span>{exercise.hint}
                  </div>
                )}

                {output && (
                  <div className="bg-black rounded-xl border border-white/10 p-4 font-mono text-sm">
                    <p className="text-[9px] text-slate-600 uppercase tracking-widest mb-2 flex items-center gap-1.5"><Terminal size={10}/> Output</p>
                    <pre className={`whitespace-pre-wrap text-xs leading-5 ${output.includes('✅') ? 'text-emerald-400' : output.includes('❌') ? 'text-red-400' : 'text-slate-300'}`}>{output}</pre>
                  </div>
                )}
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* ── CHALLENGE VIEW ── */}
      {challenge && !course && (
        <div className="flex-1 flex flex-col gap-3 min-h-0">
          <div className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-5 shrink-0">
            <div className="flex items-start justify-between mb-2">
              <div>
                <h2 className="text-base font-black text-white">{challenge.title}</h2>
                <p className="text-sm text-slate-300 mt-1">{challenge.desc}</p>
              </div>
              <div className="text-right">
                <DiffBadge level={challenge.difficulty}/>
                <p className="text-[#00f2ff] text-xs font-bold mt-1">+{challenge.xp} XP</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2 mt-3">
              {challenge.tests.map((t, i) => (
                <div key={i} className="bg-black/40 border border-white/5 rounded-lg px-3 py-2">
                  <p className="text-[9px] text-slate-600 uppercase">Test {i+1}</p>
                  <p className="text-[10px] font-mono text-slate-400">{t.input} → <span className="text-[#00f2ff]">{t.expected}</span></p>
                </div>
              ))}
            </div>
          </div>

          <CodeEditor code={code} onChange={setCode} language={challenge.lang} height="h-56"/>

          <div className="flex gap-2 shrink-0">
            <button onClick={() => { setRunning(true); runCode(code, challenge.lang, []).then(() => setRunning(false)); }} disabled={running}
              className="flex items-center gap-2 px-4 py-2.5 bg-[#00f2ff]/15 border border-[#00f2ff]/30 text-[#00f2ff] rounded-xl font-bold text-sm disabled:opacity-50 hover:bg-[#00f2ff]/25 transition-all">
              {running ? <RefreshCw size={14} className="animate-spin"/> : <Play size={14}/>} Tester
            </button>
            <button onClick={submitChallenge} disabled={running}
              className="flex items-center gap-2 px-4 py-2.5 bg-amber-500/15 border border-amber-500/30 text-amber-400 rounded-xl font-bold text-sm disabled:opacity-50 hover:bg-amber-500/25 transition-all">
              <Trophy size={14}/> Soumettre pour +{challenge.xp} XP
            </button>
          </div>

          {output && (
            <div className="bg-black rounded-xl border border-white/10 p-4 font-mono">
              <pre className={`whitespace-pre-wrap text-xs leading-5 ${output.includes('🏆') ? 'text-emerald-400' : 'text-slate-300'}`}>{output}</pre>
            </div>
          )}
        </div>
      )}

      {/* ── COURSES TAB ── */}
      {!course && !challenge && tab === 'courses' && (
        <div className="flex-1 overflow-y-auto grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 content-start">
          {COURSES.map(c => (
            <button key={c.id} onClick={() => openLesson(c, 0, 0)}
              className="bg-[#000b1e]/80 border border-white/10 hover:border-white/20 rounded-2xl p-5 text-left transition-all hover:shadow-[0_0_20px_rgba(0,242,255,0.08)] group">
              <div className="flex items-start justify-between mb-4">
                <div className="text-4xl">{c.icon}</div>
                <div className="text-right">
                  <DiffBadge level={c.level}/>
                  <p className="text-[9px] text-slate-500 mt-1 flex items-center gap-1 justify-end"><Clock size={9}/> {c.hours}</p>
                </div>
              </div>
              <h3 className="text-base font-black text-white mb-1 group-hover:text-[#00f2ff] transition-colors">{c.name}</h3>
              <p className="text-xs text-slate-500 mb-4">{c.desc}</p>
              <div className="flex items-center justify-between">
                <div className="flex gap-1">
                  {c.chapters.map((ch, i) => (
                    <div key={i} className={`w-2 h-2 rounded-full ${prog.done.some(d => ch.lessons.some(l => l.id === d)) ? 'bg-[#00f2ff]' : 'bg-white/10'}`}/>
                  ))}
                </div>
                <div className="flex items-center gap-1 text-[10px] font-bold" style={{ color: c.color }}>
                  <Users size={10}/> {c.chapters.length} chapitres
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* ── CHALLENGES TAB ── */}
      {!course && !challenge && tab === 'challenges' && (
        <div className="flex-1 overflow-y-auto space-y-3">
          {CHALLENGES.map(ch => (
            <div key={ch.id} className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-5 flex items-center gap-4">
              <div className="text-3xl">💪</div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="text-sm font-black text-white">{ch.title}</h3>
                  <DiffBadge level={ch.difficulty}/>
                </div>
                <p className="text-xs text-slate-500 truncate">{ch.desc}</p>
                <p className="text-[9px] text-[#00f2ff] mt-1">{ch.lang} · {ch.tests.length} tests · +{ch.xp} XP</p>
              </div>
              <div className="flex flex-col items-end gap-2">
                {prog.done.includes(ch.id) ? (
                  <span className="text-emerald-400 text-xs font-bold flex items-center gap-1"><CheckCircle size={12}/> Réussi</span>
                ) : (
                  <button onClick={() => startChallenge(ch)}
                    className="flex items-center gap-1.5 px-3 py-2 bg-[#00f2ff]/15 border border-[#00f2ff]/30 text-[#00f2ff] rounded-xl text-xs font-bold hover:bg-[#00f2ff]/25 transition-all">
                    <Play size={12}/> Relever le défi
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── PATHS TAB ── */}
      {!course && !challenge && tab === 'paths' && (
        <div className="flex-1 overflow-y-auto grid grid-cols-1 md:grid-cols-2 gap-4 content-start">
          {PATHS.map(p => {
            const pathCourses = p.courses.map(id => COURSES.find(c => c.id === id)).filter((c): c is Course => c !== undefined);
            const donePct = pathCourses.length
              ? Math.round((pathCourses.filter(c => prog.done.some(d => c.chapters.some(ch => ch.lessons.some(l => l.id === d)))).length / pathCourses.length) * 100)
              : 0;
            return (
              <div key={p.id} className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-6">
                <div className="text-4xl mb-3">{p.icon}</div>
                <h3 className="text-lg font-black text-white mb-1">{p.name}</h3>
                <p className="text-xs text-slate-400 mb-4">{p.desc}</p>
                <div className="flex flex-wrap gap-2 mb-4">
                  {pathCourses.map(c => (
                    <button key={c.id} onClick={() => openLesson(c, 0, 0)}
                      className="flex items-center gap-1.5 text-[10px] font-bold px-2.5 py-1 rounded-lg border border-white/10 hover:border-[#00f2ff]/40 text-slate-300 hover:text-[#00f2ff] transition-all">
                      {c.icon} {c.name}
                    </button>
                  ))}
                </div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[9px] text-slate-500 flex items-center gap-1"><Clock size={9}/> {p.hours}</span>
                  <span className="text-[9px] font-bold text-[#00f2ff]">{donePct}% complété</span>
                </div>
                <div className="h-1.5 bg-white/5 rounded-full">
                  <div className="h-full bg-[#00f2ff] rounded-full transition-all" style={{ width: `${donePct}%` }}/>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── STATS TAB ── */}
      {!course && !challenge && tab === 'stats' && (() => {
        const lvl = getLvl(prog.xp);
        const completedCourses = COURSES.filter(c => c.chapters.every(ch => ch.lessons.every(l => prog.done.includes(l.id)))).length;
        return (
          <div className="flex-1 overflow-y-auto grid grid-cols-2 md:grid-cols-4 gap-3 content-start">
            {[
              { icon: '⚡', label: 'Total XP',        value: prog.xp.toLocaleString(),                              color: lvl.color   },
              { icon: '🎓', label: 'Niveau',           value: `Niv. ${getLevel(prog.xp)+1} ${lvl.name}`,            color: lvl.color   },
              { icon: '✅', label: 'Leçons terminées', value: String(prog.done.filter(d => d.includes('-l')).length), color: '#00f2ff'   },
              { icon: '💪', label: 'Exercices',        value: String(prog.done.filter(d => d.includes('-e')).length), color: '#a78bfa'   },
              { icon: '📚', label: 'Cours terminés',   value: String(completedCourses),                              color: '#34d399'   },
              { icon: '🔥', label: 'Série actuelle',   value: `${prog.streak} jours`,                                color: '#f97316'   },
              { icon: '💻', label: 'Lignes de code',   value: prog.lines.toLocaleString(),                           color: '#60a5fa'   },
              { icon: '🏅', label: 'Badges',           value: `${prog.badges.length}/${ALL_BADGES.length}`,          color: '#fbbf24'   },
            ].map(s => (
              <div key={s.label} className="bg-[#000b1e]/80 border border-white/10 rounded-2xl p-4 text-center">
                <div className="text-2xl mb-2">{s.icon}</div>
                <p className="text-lg font-black" style={{ color: s.color }}>{s.value}</p>
                <p className="text-[9px] text-slate-500 uppercase tracking-widest mt-1">{s.label}</p>
              </div>
            ))}
            <div className="col-span-2 md:col-span-4 bg-[#000b1e]/80 border border-white/10 rounded-2xl p-5">
              <p className="text-[9px] font-black uppercase text-slate-500 tracking-widest mb-4">Badges</p>
              <div className="grid grid-cols-5 md:grid-cols-8 gap-3">
                {ALL_BADGES.map(b => {
                  const earned = prog.badges.includes(b.id);
                  return (
                    <div key={b.id} title={`${b.name}: ${b.desc}`}
                      className={`flex flex-col items-center gap-1 transition-all ${earned ? '' : 'opacity-30 grayscale'}`}>
                      <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-2xl border ${earned ? 'border-[#00f2ff]/40 bg-[#00f2ff]/10' : 'border-white/5 bg-white/5'}`}>{b.icon}</div>
                      <p className="text-[8px] text-center text-slate-400 leading-tight">{b.name}</p>
                      {!earned && <Lock size={8} className="text-slate-700"/>}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        );
      })()}

      {/* ── LEADERBOARD TAB ── */}
      {!course && !challenge && tab === 'leaderboard' && (
        <div className="flex-1 overflow-y-auto space-y-2">
          <div className="flex justify-between items-center mb-2">
            <p className="text-[9px] text-slate-500 uppercase tracking-widest">Top 10 — Toutes les langues</p>
            <button onClick={() => api('/leaderboard').then(d => d && setBoard(d))}
              className="flex items-center gap-1.5 text-[10px] text-slate-500 hover:text-[#00f2ff] transition-colors">
              <RefreshCw size={11}/> Actualiser
            </button>
          </div>
          {board.map(entry => {
            const isMe = entry.user === localStorage.getItem('lea_currentUser');
            return (
              <div key={entry.rank} className={`flex items-center gap-4 p-4 rounded-2xl border transition-all ${isMe ? 'bg-[#00f2ff]/10 border-[#00f2ff]/30' : 'bg-[#000b1e]/80 border-white/5'}`}>
                <div className="w-8 text-center">
                  {entry.rank === 1 ? <span className="text-xl">🥇</span>
                   : entry.rank === 2 ? <span className="text-xl">🥈</span>
                   : entry.rank === 3 ? <span className="text-xl">🥉</span>
                   : <span className="text-sm font-black text-slate-500">#{entry.rank}</span>}
                </div>
                <div className="w-10 h-10 rounded-xl bg-[#0047ff]/20 border border-[#0047ff]/30 flex items-center justify-center text-xl">
                  {(['🌌','🚀','⚡','💡','🐍','☕','🌐','🧮','🔥','💎'])[entry.rank - 1]}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className={`text-sm font-black ${isMe ? 'text-[#00f2ff]' : 'text-white'}`}>{entry.user}</span>
                    {isMe && <span className="text-[8px] bg-[#00f2ff]/20 text-[#00f2ff] px-1.5 py-0.5 rounded font-bold">MOI</span>}
                  </div>
                  <p className="text-[9px] text-slate-500">Niv. {getLevel(entry.xp)+1} · {getLvl(entry.xp).name} · {entry.courses} cours</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-black" style={{ color: getLvl(entry.xp).color }}>{entry.xp.toLocaleString()}</p>
                  <p className="text-[9px] text-slate-600">XP</p>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── QUIZ MODAL ── */}
      {quizOpen && chapter && (
        <QuizModal questions={chapter.quiz} onClose={(passed) => {
          setQuizOpen(false);
          if (passed) { addXP(200); earnBadgeVisible('quiz_ace'); }
        }}/>
      )}
    </div>
  );
};
