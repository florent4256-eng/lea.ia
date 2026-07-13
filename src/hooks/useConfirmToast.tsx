import React, { useState } from 'react';
import { ShieldAlert } from 'lucide-react';

/**
 * Remplace les alert()/window.confirm() natifs du navigateur par une modal glass
 * et un toast flottant, cohérents avec le reste de l'interface Léa.
 *
 * Usage :
 *   const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();
 *   askConfirm("Supprimer ?", () => doDelete());
 *   showToast("Sauvegardé !");
 *   return <div>...<ConfirmToastHost /></div>;
 */
export function useConfirmToast() {
  const [confirmDialog, setConfirmDialog] = useState<{ message: string; onConfirm: () => void } | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  const askConfirm = (message: string, onConfirm: () => void) => {
    setConfirmDialog({ message, onConfirm });
  };

  const ConfirmToastHost = () => (
    <>
      {confirmDialog && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/60 backdrop-blur-md animate-in fade-in">
          <div className="bg-[#000b1e] border border-[#00f2ff]/30 p-8 rounded-[2.5rem] max-w-sm w-full shadow-[0_0_50px_rgba(0,242,255,0.2)] text-center">
            <div className="w-16 h-16 bg-[#00f2ff]/10 rounded-full flex items-center justify-center mx-auto mb-6 text-[#00f2ff]">
              <ShieldAlert size={32} />
            </div>
            <p className="text-slate-200 text-sm mb-8 leading-relaxed">{confirmDialog.message}</p>
            <div className="flex gap-4">
              <button
                onClick={() => setConfirmDialog(null)}
                className="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold transition-all"
              >
                Annuler
              </button>
              <button
                onClick={() => { confirmDialog.onConfirm(); setConfirmDialog(null); }}
                className="flex-1 py-3 rounded-xl bg-[#00f2ff] hover:bg-white text-[#000814] font-bold transition-all shadow-[0_0_20px_rgba(0,242,255,0.4)]"
              >
                Confirmer
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[220] px-6 py-3 rounded-2xl bg-[#000b1e] border border-[#00f2ff]/30 shadow-[0_0_30px_rgba(0,242,255,0.25)] text-sm text-white font-medium animate-in fade-in slide-in-from-top-4 duration-300">
          {toast}
        </div>
      )}
    </>
  );

  return { askConfirm, showToast, ConfirmToastHost };
}
