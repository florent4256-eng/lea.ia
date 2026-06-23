import React from 'react';
import { X, Shield, Cpu, Lock, EyeOff, Radio, Smartphone } from 'lucide-react';

interface LeaTermsProps {
  onClose: () => void;
}

export const LeaTerms: React.FC<LeaTermsProps> = ({ onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#000814]/90 backdrop-blur-md animate-in fade-in duration-300">
      <div className="bg-[#000b1e] border border-[#00f2ff]/30 rounded-3xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-[0_0_50px_rgba(0,242,255,0.15)] relative animate-in zoom-in-95 duration-300">
        
        {/* En-tête du document */}
        <div className="flex items-center justify-between p-6 border-b border-white/10 shrink-0">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-[#00f2ff]/10 rounded-xl text-[#00f2ff]">
              <Shield size={24} />
            </div>
            <div>
              <h2 className="text-xl font-black text-white uppercase tracking-widest">Charte d'Opération LÉA V3</h2>
              <p className="text-[10px] text-[#00f2ff] font-mono tracking-[0.2em] uppercase">Manifeste de Sécurité & Capacités</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-red-500 hover:bg-white/5 rounded-xl transition-all">
            <X size={24} />
          </button>
        </div>

        {/* Contenu défilant */}
        <div className="p-6 overflow-y-auto custom-scrollbar space-y-8 text-slate-300">
          
          <section>
            <h3 className="text-sm font-black text-white uppercase tracking-widest flex items-center gap-2 mb-3">
              <Cpu size={16} className="text-[#0047ff]" /> 1. Capacités du Système
            </h3>
            <p className="text-sm leading-relaxed mb-2">L'intelligence LÉA V3 est une architecture modulaire souveraine. En acceptant ces termes, vous accédez aux capacités suivantes :</p>
            <ul className="list-disc list-inside text-sm space-y-1 ml-2 text-slate-400">
              <li>Communication P2P chiffrée (Texte, Audio, Vidéo WebRTC).</li>
              <li>Génération et traitement de médias locaux (Léa Studio).</li>
              <li>Traduction hors-ligne par réseaux neuronaux embarqués.</li>
              <li>Gestion de portefeuilles et transactions cryptographiques (Léa Crypto).</li>
            </ul>
          </section>

          <section>
            <h3 className="text-sm font-black text-white uppercase tracking-widest flex items-center gap-2 mb-3">
              <Smartphone size={16} className="text-[#0047ff]" /> 2. Accès Matériel Requis
            </h3>
            <p className="text-sm leading-relaxed mb-2">Pour un fonctionnement optimal sur terminaux mobiles et postes fixes, LÉA requiert l'accès aux composants suivants (uniquement lors de l'activation explicite des modules) :</p>
            <ul className="list-disc list-inside text-sm space-y-1 ml-2 text-slate-400">
              <li><strong className="text-slate-200">Microphone :</strong> Pour les mémos vocaux et appels VoIP.</li>
              <li><strong className="text-slate-200">Caméra :</strong> Pour les transmissions vidéo sécurisées (FaceTime) et la reconnaissance spatiale.</li>
              <li><strong className="text-slate-200">Stockage Local :</strong> Pour la sauvegarde des clés de chiffrement et l'historique temporaire.</li>
            </ul>
          </section>

          <section>
            <h3 className="text-sm font-black text-white uppercase tracking-widest flex items-center gap-2 mb-3">
              <EyeOff size={16} className="text-[#00f2ff]" /> 3. Confidentialité & Zéro-Cloud
            </h3>
            <div className="bg-white/5 p-4 rounded-xl border border-white/10">
              <p className="text-sm leading-relaxed">
                Contrairement aux services commerciaux, LÉA opère sur un protocole **100% Zéro-Cloud**. 
                Aucune donnée personnelle, aucun message, et aucun média n'est stocké sur des serveurs externes. 
                Toutes les transmissions transitent via votre propre tour serveur sécurisée. Les données éphémères (Mode Burn) sont définitivement détruites après expiration, sans aucune trace résiduelle.
              </p>
            </div>
          </section>

          <section>
            <h3 className="text-sm font-black text-white uppercase tracking-widest flex items-center gap-2 mb-3">
              <Lock size={16} className="text-red-400" /> 4. Responsabilité de l'Opérateur
            </h3>
            <p className="text-sm leading-relaxed">
              Le réseau est strictement privé. L'opérateur s'engage à ne pas utiliser l'infrastructure de LÉA pour des activités compromettant l'intégrité du système de flolov42. La perte de vos clés d'accès entraînera la perte irréversible de vos données locales.
            </p>
          </section>

        </div>

        {/* Footer avec validation */}
        <div className="p-4 border-t border-white/10 bg-black/40 shrink-0 flex justify-end rounded-b-3xl">
          <button onClick={onClose} className="px-6 py-3 bg-white/10 hover:bg-[#00f2ff]/20 text-white hover:text-[#00f2ff] font-bold uppercase tracking-widest rounded-xl transition-all text-xs">
            J'ai compris et j'accepte
          </button>
        </div>

      </div>
    </div>
  );
};