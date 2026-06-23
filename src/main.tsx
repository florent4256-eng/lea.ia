import React, { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// 🔌 INJECTION DU PONT CAPACITOR POUR LA MÉMOIRE TÉLÉPHONE
import { Preferences } from '@capacitor/preferences';

// Importation stricte des protocoles visuels
import './index.css';
import './app.css';
import './i18n';

// LA CORRECTION EST ICI : on cible exactement ton fichier en minuscules (app.tsx)
import App from './app';

// Configuration de niveau production pour la mémoire cache des cerveaux IA
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      refetchOnWindowFocus: false,
      staleTime: 1000 * 60 * 5, // Conservation des données pendant 5 minutes
    },
  },
});

// Sécurisation du point de montage DOM
const rootElement = document.getElementById('root');

if (!rootElement) {
  console.error(
    "%c [ERREUR SYSTÈME] Point d'ancrage 'root' introuvable. Vérifiez index.html. ", 
    "background: #ff0000; color: #ffffff; padding: 4px;"
  );
  throw new Error("Initialisation du terminal impossible.");
}

// 🔐 OUTIL SOUVERAIN : Fonction exportée pour graver l'identité dynamiquement
export const setLeaIdentity = async (username: string) => {
  try {
    await Preferences.set({
      key: 'lea_session_user',
      value: username // Le nom exact de la personne qui vient de se connecter
    });
    console.log(`✅ [MATRICE] Identité Bixby gravée dans le S23 Ultra pour : ${username}`);
  } catch (e) {
    console.error("❌ Impossible de graver l'identité:", e);
  }
};

// Lancement du moteur React
createRoot(rootElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>
);

// Signature visuelle dans la console de ton navigateur
console.log(
  "%c LÉA V3 INITIALISÉE %c MODE SOUVERAIN MULTI-COMPTES ",
  "color: #ffffff; background: #0047ff; font-weight: bold; padding: 4px 8px; border-radius: 4px 0 0 4px;",
  "color: #00f2ff; background: #000814; font-weight: bold; padding: 4px 8px; border-radius: 0 4px 4px 0;"
);