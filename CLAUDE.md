# Léa V3 — Design System & Règles du projet

## Stack
- React + TypeScript + Vite
- Tailwind CSS (classes utilitaires uniquement, pas de CSS custom)
- Lucide React pour les icônes
- Capacitor pour le build Android

## Palette de couleurs
- Fond principal : `#020617` (bg-[#020617])
- Surface : `bg-white/5` avec bordure `border-white/10`
- Accent primaire : `purple-500` → `fuchsia-600` (dégradé)
- Accent secondaire : `purple-400` pour les labels
- Texte principal : `text-white` / `text-slate-200`
- Texte secondaire : `text-slate-400` / `text-slate-500`
- Succès : `green-400` / `#22c55e`
- Erreur : `red-400`

## Composants récurrents
- **Boutons principaux** : `bg-gradient-to-r from-purple-600 to-fuchsia-600 rounded-xl font-black uppercase tracking-widest text-xs`
- **Cartes / surfaces** : `bg-white/5 border border-white/10 rounded-2xl`
- **Inputs** : `bg-white/5 border border-white/10 rounded-xl outline-none focus:border-purple-500/50`
- **Labels section** : `text-[9px] font-black uppercase tracking-widest text-slate-500`
- **Badges** : `bg-white/5 px-3 py-1.5 rounded-full border border-white/10 text-[10px]`
- **Boutons ronds (actions)** : `w-10 h-10 rounded-full flex items-center justify-center`

## Effets visuels
- Glassmorphism : `backdrop-blur` + `bg-white/5` ou `bg-black/40`
- Lueur accent : `shadow-[0_0_16px_rgba(168,85,247,0.4)]`
- Grille de fond décorative : pattern SVG base64 en `bg-[url(...)]`
- Halo décoratif : `div` rond avec `blur-3xl` et couleur `purple/15`
- Ping animation sur les points de statut actifs

## Animations & transitions
- Toujours `transition-all` (pas de transition partielle)
- Durée standard : `duration-200`
- Hover scale : `hover:scale-[1.01]` (boutons), `hover:scale-110` (icônes rondes)
- Active : `active:scale-95` ou `active:scale-90`

## Typographie
- Titres modules : `text-base font-black text-white uppercase italic tracking-tight`
- Corps standard : `text-sm text-slate-200`
- Micro-labels : `text-[9px]` ou `text-[10px]` uppercase tracking-widest
- Monospace (seeds, timestamps) : `font-mono`

## Règles UX
- Toujours dark mode — jamais de fond blanc ou clair
- Icônes uniquement depuis `lucide-react`
- Pas de texte explicatif dans les boutons si une icône suffit
- Les modals/popups : petit par défaut + bouton agrandir, fond translucide pour voir l'interface
- Les états de chargement : spinner `border-t-purple-500 rounded-full animate-spin`
- Les états vides : icône + texte `opacity-20`
- Feedback actions (download, save) : couleur → vert + icône ✓ pendant 3s

## Architecture des composants
- Tous dans `src/components/modules/`
- Export nommé (pas default)
- State local uniquement sauf si partagé entre modules
- Les appels serveur utilisent `(window as any).LEA_SERVER_URL || ''`
- L'utilisateur courant : `localStorage.getItem('lea_currentUser') || 'flolov42'`

## Serveur (server.cjs)
- Node.js + Express + WebSocket
- PM2 process name : `LeaMaster`
- Redémarrage : `pm2 restart LeaMaster`
- Users dir : `USERS_DIR` (coffres par utilisateur)
- `MASTER_ADMIN` vient TOUJOURS de `process.env.MASTER_ADMIN` — jamais hardcodé
- Images générées : `/coffres/{username}/studio/nano/`
- ComfyUI : port 8188, RTX 3060 12go, `--lowvram`
 