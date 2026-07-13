import React from 'react';
import { Capacitor, registerPlugin } from '@capacitor/core';
import { Heart, ExternalLink, LayoutGrid } from 'lucide-react';

const LeaPhone = registerPlugin<{ openLeaLoveApp: () => Promise<void> }>('LeaPhone');

interface EcosystemApp {
  id: string;
  name: string;
  description: string;
  icon: React.ReactNode;
  accent: string; // classes Tailwind pour le badge d'icône
  webFallback: string;
  open: () => Promise<void>;
}

const apps: EcosystemApp[] = [
  {
    id: 'lealove',
    name: 'Léa Love',
    description: "L'application de rencontre connectée à Léa — même compte, IA embarquée pour t'accompagner dans tes conversations.",
    icon: <Heart size={22} />,
    accent: 'bg-pink-500/10 text-pink-400 border-pink-500/20',
    webFallback: 'https://lealove.lea-ia-local.com',
    open: async () => {
      if (Capacitor.isNativePlatform()) {
        await LeaPhone.openLeaLoveApp();
      } else {
        window.open('https://lealove.lea-ia-local.com', '_blank');
      }
    },
  },
];

// Écosystème Léa — liste les applications séparées mais connectées au même compte Léa.
// Pour l'instant seule Léa Love existe ; conçu pour accueillir de futures applications
// sans changer la structure (voir tableau `apps` ci-dessus).
export const LeaEcosystem = () => {
  const openApp = async (app: EcosystemApp) => {
    try {
      await app.open();
    } catch {
      window.open(app.webFallback, '_blank');
    }
  };

  return (
    <div className="w-full h-full flex flex-col items-center px-6 py-10 overflow-y-auto">
      <div className="w-full max-w-lg flex flex-col gap-6">
        <div className="flex flex-col items-center text-center gap-3">
          <div className="relative">
            <div className="absolute inset-[-14px] rounded-full bg-gradient-to-br from-purple-500 to-fuchsia-600 opacity-20 blur-xl" />
            <div className="relative w-16 h-16 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-purple-400">
              <LayoutGrid size={28} />
            </div>
          </div>
          <div>
            <h2 className="text-xl font-black text-white uppercase italic tracking-tight">Écosystème Léa</h2>
            <p className="text-sm text-slate-400 max-w-sm mt-1">
              Toutes les applications connectées à ton compte Léa, au même endroit.
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          {apps.map(app => (
            <button
              key={app.id}
              onClick={() => openApp(app)}
              className="w-full flex items-center gap-4 p-4 rounded-2xl bg-white/5 border border-white/10 hover:border-white/20 hover:scale-[1.01] transition-all text-left"
            >
              <div className={`w-12 h-12 rounded-xl border flex items-center justify-center shrink-0 ${app.accent}`}>
                {app.icon}
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-black text-white">{app.name}</p>
                <p className="text-xs text-slate-500 mt-0.5">{app.description}</p>
              </div>
              <ExternalLink size={16} className="text-slate-600 shrink-0" />
            </button>
          ))}
        </div>

        <p className="text-[10px] text-slate-600 text-center uppercase tracking-widest">
          D'autres applications rejoindront l'écosystème ici
        </p>
      </div>
    </div>
  );
};
