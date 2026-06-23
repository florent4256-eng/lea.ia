# LÉA V3 — Intelligence Artificielle Souveraine

> **Zéro abonnement. Zéro cloud. 100% local.**
> Ton IA tourne sur ton matériel. Tes données ne quittent jamais ta machine.

---

## Pourquoi Léa ?

Les assistants IA comme ChatGPT coûtent 20€/mois et envoient toutes tes données sur des serveurs américains.

Léa tourne entièrement sur **ton propre PC** grâce à [Ollama](https://ollama.com). Une fois installée : **0€/mois, 0 donnée envoyée.**

---

## Ce que Léa sait faire

| Module | Description |
|---|---|
| **Léa Terminal** | Assistant IA conversationnel (voix + texte) |
| **Studio Nano** | Génération d'images IA (FLUX, SDXL, SD1.5) |
| **Studio Lyria** | Génération musicale (MusicGen) |
| **Studio Veo** | Génération vidéo IA |
| **Léa Chat** | Messagerie chiffrée entre utilisateurs |
| **Léa Protect** | Sécurité & surveillance |
| **Léa Academy** | Cours et apprentissage |
| **Léa Crypto** | Trading et portefeuille |
| **Léa Shop** | Abonnements et tokens |
| **Languages** | Traduction locale multi-langues |
| **Léa Love** | Compagnon IA personnalisé |

---

## Application Mobile

Léa est disponible en application Android native.

> Télécharge le dernier APK dans la section [Releases](../../releases)

**Compatible** : Android 8.0+ (API 26)

---

## Configuration minimale recommandée

Pour faire tourner le serveur IA local :

| Composant | Minimum | Recommandé |
|---|---|---|
| GPU | 8 Go VRAM | 12 Go VRAM (RTX 3060) |
| RAM | 16 Go | 32 Go |
| Stockage | 50 Go libre | 100 Go SSD |
| OS | Ubuntu 20.04+ | Ubuntu 22.04+ |

> Sans GPU dédié, Léa tourne en mode CPU (lent mais fonctionnel pour le chat).

---

## Interface uniquement (ce repo)

Ce dépôt contient **l'interface frontend** de Léa (React + TypeScript).

Le serveur backend (moteur IA, gestion utilisateurs, génération) est **propriétaire** et non publié — c'est ce qui fait tourner l'intelligence de Léa.

---

## Stack technique

- **Frontend** : React 18 + TypeScript + Vite + Tailwind CSS
- **Mobile** : Capacitor (Android natif)
- **IA locale** : Ollama (DeepSeek, Qwen, FLUX...)
- **Voix** : Piper TTS (synthèse vocale offline)

---

## Contribuer

Les contributions sur l'interface sont les bienvenues :
- Corriger des bugs UI
- Améliorer l'accessibilité
- Traduire l'interface

Ouvre une **Issue** ou une **Pull Request**.

---

## Licence

Interface publiée sous licence **MIT**.
Le backend et les modèles IA entraînés restent propriétaires.

---

*Projet développé et maintenu par [@flolov42](https://github.com/flolov42)*
