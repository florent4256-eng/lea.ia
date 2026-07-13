import React, { useState, Suspense, lazy } from 'react';
import { useTranslation } from 'react-i18next';
import { Capacitor } from '@capacitor/core';
import { useConfirmToast } from '../../hooks/useConfirmToast';
import {
  MessageSquarePlus, Palette, History, Menu, Settings, MoreVertical,
  ChevronRight, ChevronDown, Terminal, Heart, Lock,
  Activity, Globe, Car, ShieldCheck, ShieldAlert, LogOut, User, Coins, Home,
  MapPin, Box, Bot, Star, Sparkles, Download, Zap, Map, Crown, Mic, Trash2, X, Film, Music, ShoppingBag, Brain, FileText, LayoutGrid
} from 'lucide-react';

// Modules chargés immédiatement (toujours utilisés)
import { LeaTerminal } from '../modules/LeaTerminal';
import { LiveModeNative } from '../modules/LiveModeNative';

// Modules chargés à la demande (lazy) — réduit le bundle initial
const LeaAgent     = lazy(() => import('../modules/LeaAgent').then(m => ({ default: m.LeaAgent })));
const LeaSettings  = lazy(() => import('../../features/LeaSettings').then(m => ({ default: m.LeaSettings })));
const LeaChat      = lazy(() => import('../modules/LeaChat').then(m => ({ default: m.LeaChat })));
const LeaEcosystem = lazy(() => import('../modules/LeaEcosystem').then(m => ({ default: m.LeaEcosystem })));
const LeaProtect   = lazy(() => import('../modules/LeaProtect').then(m => ({ default: m.LeaProtect })));
const LeaCrypto    = lazy(() => import('../modules/LeaCrypto').then(m => ({ default: m.LeaCrypto })));
const Languages    = lazy(() => import('../modules/Languages').then(m => ({ default: m.Languages })));
const LeaAcademy   = lazy(() => import('../modules/LeaAcademy').then(m => ({ default: m.LeaAcademy })));
const StudioNano   = lazy(() => import('../modules/StudioNano').then(m => ({ default: m.StudioNano })));
const StudioLyria  = lazy(() => import('../modules/StudioLyria').then(m => ({ default: m.StudioLyria })));
const StudioVeo    = lazy(() => import('../modules/StudioVeo').then(m => ({ default: m.StudioVeo })));
const StudioForge3D  = lazy(() => import('../modules/StudioForge3D').then(m => ({ default: m.StudioForge3D })));
const StudioMontage  = lazy(() => import('../modules/StudioMontage').then(m => ({ default: m.StudioMontage })));
const LeaShop        = lazy(() => import('../modules/LeaShop').then(m => ({ default: m.LeaShop })));
const LeaUpdates     = lazy(() => import('../../features/LeaUpdates').then(m => ({ default: m.LeaUpdates })));

import { registerPlugin } from '@capacitor/core';
const LeaPhone = registerPlugin<any>('LeaPhone');

const LazyFallback = () => (
  <div className="flex items-center justify-center h-full">
    <div className="w-8 h-8 border-2 border-t-[#00f2ff] border-white/10 rounded-full animate-spin" />
  </div>
);

class ErrorBoundary extends React.Component<{children: React.ReactNode}, {hasError: boolean}> {
  constructor(props: any) { super(props); this.state = { hasError: false }; }
  static getDerivedStateFromError() { return { hasError: true }; }
  render() {
    if (this.state.hasError) return <div style={{color:'white',padding:'20px'}}>Module indisponible</div>;
    return this.props.children;
  }
}

interface ChatInterfaceProps {
  activeModule: string;
  setActiveModule: (module: string) => void;
  isSidebarOpen: boolean;
  setSidebarOpen: (state: boolean) => void;
  onSearchStart: (query: string, mode: 'search' | 'research') => Promise<any>;
  history: any[];
  status: string;
  children?: React.ReactNode;
  isAdmin?: boolean;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({
  activeModule, setActiveModule, isSidebarOpen, setSidebarOpen, status, children, isAdmin = false
}) => {
  const { t, i18n } = useTranslation();
  const { askConfirm, ConfirmToastHost } = useConfirmToast();
  const [showModules, setShowModules] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  let currentUser = '';
  try { currentUser = localStorage.getItem('lea_currentUser') || ''; } catch (e) { console.warn('localStorage indisponible'); }
  const [showHistory, setShowHistory] = useState(false);
  const [historySessions, setHistorySessions] = useState<{id: string, name: string}[]>([]);
  const [showNovaHistory, setShowNovaHistory] = useState(false);
  type NovaItem = { ts: number; userText: string; leaText: string; isFavorite?: boolean };
  const [novaHistory, setNovaHistory] = useState<NovaItem[]>([]);
  const [novaModal, setNovaModal] = useState<NovaItem | null>(null);
  const [userTokens, setUserTokens] = useState<number | string>('...');
  // Initialisation immédiate depuis localStorage (évite le flash "pas admin" au chargement)
  const [userRole, setUserRole] = useState<string>(() => {
    try { return localStorage.getItem('lea_userRole') === 'admin' ? 'ROOT ACCESS' : 'USER'; } catch { return 'USER'; }
  });
  const [isAdult, setIsAdult] = useState(true);
  const [userDob, setUserDob] = useState<string | null>(null);
  const [isAdminUser, setIsAdminUser] = useState(false);

  // Vérification admin au montage — indépendante de userRole
  React.useEffect(() => {
    if (!currentUser) return;
    fetch(`/api/system/is-admin?u=${encodeURIComponent(currentUser)}`)
      .then(r => r.json())
      .then(d => { if (d.isAdmin) setIsAdminUser(true); })
      .catch(() => {});
  }, []);

  // Navigation inter-modules via event (ex: Forge3D → Studio Veo)
  React.useEffect(() => {
    const handler = (e: Event) => {
      const mod = (e as CustomEvent<{ module: string }>).detail?.module;
      if (mod) setActiveModule(mod);
    };
    window.addEventListener('lea-navigate', handler);
    return () => window.removeEventListener('lea-navigate', handler);
  }, [setActiveModule]);

  // --- FORÇAGE DE LA LANGUE SOUVERAINE EN TEMPS RÉEL ---
  React.useEffect(() => {
    const applyLanguage = () => {
      // Le plugin i18next sauvegarde généralement la langue sous 'i18nextLng'
      // On vérifie aussi 'lea_language' au cas où tes paramètres utilisent cette clé.
      let savedLang = 'fr';
      try { savedLang = localStorage.getItem('i18nextLng') || localStorage.getItem('lea_language') || 'fr'; } catch (e) {}
      
      // Si la langue du moteur est différente de la sauvegarde, on force la traduction !
      if (i18n && i18n.language !== savedLang) {
        i18n.changeLanguage(savedLang);
        console.log(`🌍 [Traducteur] Langue de l'interface basculée en : ${savedLang.toUpperCase()}`);
      }
    };

    // 1. Déclenchement à chaque changement d'écran (ex: quand tu quittes les paramètres)
    applyLanguage();

    // 2. Déclenchement automatique si la mémoire locale change
    window.addEventListener('storage', applyLanguage);

    return () => window.removeEventListener('storage', applyLanguage);
  }, [activeModule, i18n]);

// --- NOUVEAU : FORCER LA PAGE BLANCHE A L'OUVERTURE (SANS FLASH VISUEL) ---
  const [isPurged] = React.useState(() => {
    const freshSession = Date.now().toString();
    try { localStorage.setItem('lea_active_session_id', freshSession); } catch (e) {}
    try { localStorage.setItem('lea_currentSession', freshSession); } catch (e) {}
    return true;
  });
  
  React.useEffect(() => {
    // 1. Récupération du statut Adulte et Date de Naissance
    let storedIsAdult: string | null = null;
    try { storedIsAdult = localStorage.getItem('lea_isAdult'); } catch (e) { console.warn('localStorage indisponible'); }
    setIsAdult(storedIsAdult !== 'false');

    if (currentUser) {
        try {
            const userStr = localStorage.getItem('lea_user_' + currentUser.toLowerCase());
            if (userStr) {
                const user = JSON.parse(userStr);
                if (user.dob) setUserDob(user.dob);
            }
        } catch (e) { console.warn('localStorage indisponible'); }
    }

    // 2. Synchronisation de l'historique (avec AbortController)
    const controller = new AbortController();
    fetch(`/api/history/${currentUser}`, { signal: controller.signal })
      .then(res => res.json())
      .then(data => setHistorySessions(data))
      .catch(err => { if (err.name !== 'AbortError') console.error("Erreur historique:", err); });

    // 3. Synchronisation des TOKENS et du RÔLE
    fetch(`/api/user/profile/${currentUser}`)
      .then(res => res.json())
      .then(data => {
          if(data.tokens !== undefined) {
              const isAdmin = data.role === 'admin';
              setUserTokens(isAdmin ? '∞' : data.tokens);
              setUserRole(isAdmin ? 'ROOT ACCESS' : data.abonnement.toUpperCase());
              try { localStorage.setItem('lea_userRole', isAdmin ? 'admin' : 'user'); } catch (e) {}
          }
      })
      .catch(err => console.error("Erreur profil:", err));

    // Vérification admin directe (fiable même si le profil est incomplet)
    if (currentUser) {
      fetch(`/api/system/is-admin?u=${encodeURIComponent(currentUser)}`)
        .then(r => r.json())
        .then(d => {
          if (d.isAdmin) {
            setUserRole('ROOT ACCESS');
            try { localStorage.setItem('lea_userRole', 'admin'); } catch (e) {}
          }
        })
        .catch(() => {});
    }

    // 4. LE HEARTBEAT (Réveil du serveur toutes les 30 secondes)
    const heartbeat = setInterval(() => {
        fetch(`/api/heartbeat`).catch(() => {});
    }, 30000);

    return () => {
      controller.abort();
      clearInterval(heartbeat);
    };

  }, [currentUser, showHistory, activeModule]);

  // --- FONCTION COMPTE À REBOURS 18 ANS ---
  const getCountdownTo18 = (birthDateString: string | null) => {
    if (!birthDateString) return "Interdit";
    const birthDate = new Date(birthDateString);
    const targetDate = new Date(birthDate.getFullYear() + 18, birthDate.getMonth(), birthDate.getDate());
    const today = new Date();
    const diffTime = Math.abs(targetDate.getTime() - today.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays > 365) return `Dans ${Math.floor(diffDays / 365)} ans`;
    if (diffDays > 30) return `Dans ${Math.floor(diffDays / 30)} mois`;
    return `Dans ${diffDays} jours`;
  };

  const sidebarModules = [
    { id: 'studio', name: t('mod_studio'), icon: <Palette size={16} /> },
    { id: 'chat', name: t('mod_chat'), icon: <Terminal size={16} /> },
    { id: 'home', name: 'Léa Home', icon: <Home size={16} /> },
    { id: 'love', name: t('mod_love'), icon: <Heart size={16} /> },
    { id: 'protect', name: t('mod_protect'), icon: <ShieldCheck size={16} /> },
    { id: 'crypto', name: t('mod_crypto'), icon: <Lock size={16} /> },
    { id: 'turbo', name: t('mod_turbo'), icon: <Zap size={16} /> },
    { id: 'maps', name: t('mod_maps'), icon: <Map size={16} /> },
    { id: 'languages', name: t('mod_lang'), icon: <Globe size={16} /> },
    { id: 'auto', name: t('mod_auto'), icon: <Car size={16} /> }
  ];

  const handleLogout = () => {
    try { localStorage.removeItem('lea_currentUser'); } catch (e) {}
    window.location.reload();
  };
  
  // --- [GOD-MODE] DÉCLENCHEMENT DE LA SIRÈNE ---
  const triggerMaintenance = () => {
    askConfirm("🚨 BOSS : Es-tu sûr de vouloir déclencher l'alerte de maintenance (15 min) sur tous les terminaux ?", async () => {
      try {
        let token = ''; try { token = localStorage.getItem('lea_session_token') || ''; } catch (e) {}
        await fetch('/api/system/maintenance', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...(token ? { 'x-lea-session': token } : {}) },
          body: JSON.stringify({ username: currentUser, duration: 15 })
        });
      } catch (error) {
        console.error("Liaison avec le Master interrompue");
      }
    });
  };

  return (
    <div className="fixed inset-0 w-screen h-screen overflow-hidden bg-[#000814]">
      <ConfirmToastHost />

      {/* LA ZONE MAGIQUE A 80% + BLOCAGE ANTI-SAUT DE PAGE */}
      <div
        className="absolute top-0 left-0 flex text-white transition-all duration-300"
        style={{
          width: activeModule === 'studio_montage' ? '100%' : '125%',
          height: activeModule === 'studio_montage' ? '100%' : '125%',
          transform: activeModule === 'studio_montage' ? 'none' : 'scale(0.80)',
          transformOrigin: 'top left'
        }}
      >
        
        {/* SIDEBAR */}
        <aside className={`
          ${isSidebarOpen && activeModule !== 'studio_montage' ? 'w-[320px]' : 'w-0'}
          transition-all duration-500 ease-in-out h-full
          bg-[#020617]/60 backdrop-blur-2xl border-r border-white/10
          flex flex-col overflow-hidden z-50 shadow-[5px_0_30px_rgba(0,0,0,0.5)] shrink-0
        `}>
          <div className="p-5 flex flex-col h-full min-w-[320px]">
            
            <button 
  onClick={() => {
    const newSession = Date.now().toString();
    try { localStorage.setItem('lea_active_session_id', newSession); } catch (e) {}
    try { localStorage.setItem('lea_currentSession', newSession); } catch (e) {}
    setActiveModule('terminal');
  }}
  className="group flex items-center gap-4 px-6 py-4 bg-[#00ffff]/5 border border-[#00ffff]/20 rounded-2xl text-[#00ffff] hover:bg-[#00ffff]/20 hover:drop-shadow-[0_0_10px_rgba(0,255,255,0.5)] transition-all duration-300 mb-4"
>
  <MessageSquarePlus size={22} className="group-hover:scale-110 transition-transform" /> 
  <span className="font-bold tracking-wide">{t('sidebar_new_chat')}</span>
</button>
            
            {/* BOUTON HISTORIQUE LEANOVA */}
            <div className="flex flex-col mb-2">
              <button
                onClick={() => {
                  const next = !showNovaHistory;
                  setShowNovaHistory(next);
                  if (next) {
                    fetch(`/api/nova/history?username=${encodeURIComponent(currentUser)}`)
                      .then(r => r.json()).then(setNovaHistory).catch(() => {});
                  }
                }}
                className="group flex items-center justify-between px-6 py-3 text-slate-400 hover:text-white hover:bg-white/5 rounded-xl transition-all duration-200"
              >
                <div className="flex items-center gap-4">
                  <Mic size={20} className={`transition-all duration-300 ${showNovaHistory ? 'text-purple-400' : 'group-hover:text-purple-300'}`} />
                  <span className={`font-medium ${showNovaHistory ? 'text-white' : ''}`}>Historique LeaNova</span>
                </div>
                {showNovaHistory ? <ChevronDown size={16} className="text-purple-400" /> : <ChevronRight size={16} />}
              </button>
              <div className={`space-y-0.5 transition-all duration-500 overflow-y-auto custom-scrollbar ${showNovaHistory ? 'opacity-100 mt-1 max-h-48' : 'opacity-0 max-h-0 pointer-events-none'}`}>
                {novaHistory.length === 0 ? (
                  <p className="text-[10px] text-white/20 italic text-center py-2 px-6">Aucun échange vocal</p>
                ) : (
                  novaHistory.map((item, i) => (
                    <div key={i}
                      className="group/item relative flex items-center gap-1 pl-6 pr-2 py-2 rounded-lg hover:bg-purple-500/10 transition-colors cursor-pointer"
                      onClick={() => setNovaModal(item)}
                    >
                      <div className="flex-1 min-w-0">
                        <span className="text-xs font-bold text-slate-300 group-hover/item:text-purple-300 truncate block">{item.userText.slice(0, 38)}{item.userText.length > 38 ? '…' : ''}</span>
                        <span className="text-[9px] text-slate-500 font-mono">{new Date(item.ts).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                      </div>
                      <button
                        title={item.isFavorite ? 'Retirer des favoris' : 'Ajouter aux favoris'}
                        className="shrink-0 opacity-0 group-hover/item:opacity-100 text-sm leading-none px-1 transition-all"
                        onClick={e => {
                          e.stopPropagation();
                          const newFav = !item.isFavorite;
                          fetch('/api/nova/favorite', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: currentUser, ts: item.ts, isFavorite: newFav }) })
                            .then(() => setNovaHistory(prev => prev.map(h => h.ts === item.ts ? { ...h, isFavorite: newFav } : h)));
                        }}
                      >{item.isFavorite ? '⭐' : '☆'}</button>
                      <button
                        title="Supprimer"
                        className="shrink-0 opacity-0 group-hover/item:opacity-100 text-red-400 hover:text-red-300 transition-all p-0.5"
                        onClick={e => {
                          e.stopPropagation();
                          fetch('/api/nova/item', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: currentUser, ts: item.ts }) })
                            .then(() => setNovaHistory(prev => prev.filter(h => h.ts !== item.ts)));
                        }}
                      ><Trash2 size={12}/></button>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* BOUTON HISTORIQUE DÉROULANT */}
            <div className="flex flex-col mb-6">
              <button
                onClick={() => setShowHistory(!showHistory)}
                className="group flex items-center justify-between px-6 py-3 text-slate-400 hover:text-white hover:bg-white/5 rounded-xl transition-all duration-200"
              >
                <div className="flex items-center gap-4">
                  <History size={20} className={`transition-transform duration-300 ${showHistory ? '-rotate-45 text-[#00f2ff]' : 'group-hover:-rotate-45'}`} /> 
                  <span className={`font-medium ${showHistory ? 'text-white' : ''}`}>{t('sidebar_history')}</span>
                </div>
                {showHistory ? <ChevronDown size={16} className="text-[#00f2ff]" /> : <ChevronRight size={16} />}
              </button>

              {/* LISTE DES SESSIONS */}
              <div className={`
                flex-1 space-y-1 transition-all duration-500 overflow-y-auto custom-scrollbar
                ${showHistory ? 'opacity-100 mt-2 max-h-48' : 'opacity-0 max-h-0 pointer-events-none'}
              `}>
                {historySessions.length === 0 ? (
                  <p className="text-[10px] text-white/20 italic text-center py-2 px-6">Aucune mémoire trouvée</p>
                ) : (
                  historySessions.map((session) => (
                    <div
                      key={session.id}
                      className="group/session relative flex items-center gap-1 pl-6 pr-2 py-2 rounded-lg hover:bg-[#00ffff]/15 transition-all cursor-pointer"
                      onClick={() => {
                        try { localStorage.setItem('lea_currentSession', session.id); } catch (e) {}
                        setActiveModule('terminal');
                      }}
                    >
                      <div className="flex-1 min-w-0">
                        <span className="text-xs font-bold text-slate-300 group-hover/session:text-white truncate block">{session.name}</span>
                        <span className="text-[9px] text-slate-500 font-mono mt-0.5 block">{new Date(parseInt(session.id)).toLocaleDateString()}</span>
                      </div>
                      <div className="flex items-center gap-1 opacity-0 group-hover/session:opacity-100 transition-all shrink-0">
                        <button
                          title="Favori"
                          className="p-1 text-white/40 hover:text-yellow-400 hover:scale-110 transition-all"
                          onClick={e => e.stopPropagation()}
                        ><Star size={12} /></button>
                        <button
                          title="Supprimer"
                          className="p-1 text-white/40 hover:text-red-500 hover:scale-110 transition-all"
                          onClick={e => {
                            e.stopPropagation();
                            let token = ''; try { token = localStorage.getItem('lea_session_token') || ''; } catch {}
                            fetch(`/api/history/${currentUser}/${session.id}`, {
                              method: 'DELETE',
                              headers: token ? { 'x-lea-session': token } : {}
                            }).then(() => setHistorySessions(prev => prev.filter(s => s.id !== session.id)));
                          }}
                        ><Trash2 size={12} /></button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* ═══════════════════════════════════════════════════════ */}
            {/* ZONE SCROLLABLE — tous les boutons + hubs + studio      */}
            {/* flex-1 + min-h-0 indispensables pour le scroll dans     */}
            {/* un flex-col avec footer fixe                            */}
            {/* ═══════════════════════════════════════════════════════ */}
            <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden custom-scrollbar pr-1">

            <div className="h-px w-full bg-gradient-to-r from-transparent via-white/10 to-transparent mb-4" />

            {/* ACCÈS SUPRÊME & OUTILS UNIVERSELS */}
          <div className="mb-4 space-y-2">
            {/* TERMINAL — bleu nuit galaxie */}
            <button
              onClick={() => setActiveModule('terminal')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 text-sm rounded-lg transition-all duration-300 border ${
                activeModule === 'terminal'
                  ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none'
                  : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'
              }`}
            >
              <Terminal size={18} className="shrink-0" />
              <span>Léa Terminal</span>
            </button>

            {/* ÉCOSYSTÈME — liste des applications connectées au compte Léa (Léa Love, etc.) —
                couleur volontairement voyante (dégradé violet/fuchsia + lueur), pour se
                démarquer du reste de la sidebar et donner envie de cliquer dessus. */}
            <button
              onClick={() => setActiveModule('ecosystem')}
              className={`w-full flex items-center gap-3 px-4 py-2.5 text-sm rounded-lg font-bold transition-all duration-300 border shadow-[0_0_15px_rgba(168,85,247,0.35)] hover:scale-[1.02] ${
                activeModule === 'ecosystem'
                  ? 'bg-gradient-to-r from-purple-600 to-fuchsia-600 border-transparent text-white'
                  : 'bg-gradient-to-r from-purple-600/20 to-fuchsia-600/20 border-purple-500/40 text-purple-300 hover:from-purple-600 hover:to-fuchsia-600 hover:text-white hover:border-transparent'
              }`}
            >
              <LayoutGrid size={18} className="shrink-0" />
              <span>Écosystème Léa</span>
            </button>

          </div>

          {/* LÉA ROUTINE & AGENT — disponibles sur mobile pour tous */}
          {Capacitor.isNativePlatform() && (
            <>
              <div className="mb-4">
                <button
                  onClick={async () => { try { await LeaPhone.openRoutines(); } catch (e) { console.error('Erreur routines', e); } }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_15px_rgba(0,229,255,0.2)] bg-cyan-600/10 hover:bg-cyan-600 hover:scale-[1.02] border-cyan-500/40 text-cyan-400 hover:text-white"
                >
                  <Star size={18} className="shrink-0" />
                  <span>Léa Routine</span>
                </button>
              </div>

              {/* 🤖 LÉA AGENT — 11 agents + 10 modes innovants */}
              <div className="mb-4">
                <button
                  onClick={async () => { try { await LeaPhone.openAgents(); } catch (e) { console.error('Erreur agents', e); } }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_18px_rgba(103,58,183,0.35)] bg-violet-700/10 hover:bg-violet-700 hover:scale-[1.02] border-violet-500/40 text-violet-400 hover:text-white"
                >
                  <Bot size={18} className="shrink-0 animate-pulse" />
                  <span>Léa Agent</span>
                </button>
              </div>


              {/* ✨ LÉA PLUS — 15 features, 5 onglets */}
              <div className="mb-4">
                <button
                  onClick={async () => { try { await LeaPhone.openLeaPlus(); } catch (e) { console.error('Erreur LeaPlus', e); } }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_18px_rgba(255,215,0,0.25)] bg-yellow-500/8 hover:bg-yellow-500/20 hover:scale-[1.02] border-yellow-400/35 hover:border-yellow-400/70 text-yellow-400 hover:text-white"
                >
                  <Sparkles size={18} className="shrink-0" />
                  <span>Léa Plus ✨</span>
                </button>
              </div>

              {/* 🏠 LÉA HOME — Domotique complète */}
              <div className="mb-4">
                <button
                  onClick={async () => { try { await LeaPhone.openLeaHome(); } catch (e) { console.error('Erreur LeaHome', e); } }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_18px_rgba(0,229,255,0.25)] bg-[#00E5FF]/8 hover:bg-[#00E5FF]/20 hover:scale-[1.02] border-[#00E5FF]/35 hover:border-[#00E5FF]/70 text-[#00E5FF] hover:text-white"
                >
                  <Home size={18} className="shrink-0" />
                  <span>Léa Home 🏠</span>
                </button>
              </div>

              {/* 🗺️ LÉA MAPS — Navigation OSMDroid natif */}
              <div className="mb-4">
                <button
                  onClick={async () => {
                    try { await LeaPhone.openMaps({ currentUser }); } catch (e) { console.error('Erreur Maps', e); }
                  }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_18px_rgba(34,197,94,0.25)] bg-green-500/8 hover:bg-green-500/20 hover:scale-[1.02] border-green-500/35 hover:border-green-500/70 text-green-400 hover:text-white"
                >
                  <MapPin size={18} className="shrink-0" />
                  <span>Léa Maps 🗺️</span>
                </button>
              </div>

              {/* 🚗 LÉA AUTO — Diagnostic OBD2 natif */}
              <div className="mb-4">
                <button
                  onClick={async () => {
                    let currentUser = '';
                    try { currentUser = localStorage.getItem('lea_currentUser') || ''; } catch (e) {}
                    try {
                      // openAuto ouvre l'Activity native Android; await garantit
                      // que l'activité est terminée avant de continuer.
                      // closeAuto() dans le finally assure la fermeture BT
                      // même en cas de crash ou retour brutal.
                      await LeaPhone.openAuto({ currentUser });
                    } catch (e) {
                      console.error('Erreur Auto', e);
                    } finally {
                      // Fermeture hermétique du socket Bluetooth OBD2
                      try { await LeaPhone.closeAuto(); } catch {}
                    }
                  }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_18px_rgba(239,68,68,0.25)] bg-red-500/8 hover:bg-red-500/20 hover:scale-[1.02] border-red-500/35 hover:border-red-500/70 text-red-400 hover:text-white"
                >
                  <Car size={18} className="shrink-0" />
                  <span>Léa Auto 🚗</span>
                </button>
              </div>

              {/* 📝 LÉA OFFICE — Traitement de texte IA natif */}
              <div className="mb-4">
                <button
                  onClick={async () => {
                    try { await LeaPhone.openOffice(); } catch (e) { console.error('Erreur Office', e); }
                  }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_18px_rgba(0,229,255,0.25)] bg-[#00E5FF]/8 hover:bg-[#00E5FF]/20 hover:scale-[1.02] border-[#00E5FF]/35 hover:border-[#00E5FF]/70 text-[#00E5FF] hover:text-white"
                >
                  <FileText size={18} className="shrink-0" />
                  <span>Léa Office 📝</span>
                </button>
              </div>
            </>
          )}

          {/* BOUTONS EXCLUSIFS ADMIN */}
          {(isAdmin || userRole === 'ROOT ACCESS' || isAdminUser) && (
            <>
              {/* 🧠 LÉA AGENT DEV — visible web + Android */}
              <div className="mb-4">
                <button
                  onClick={() => setActiveModule('agent')}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 text-sm rounded-lg transition-all duration-300 border ${activeModule === 'agent' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}
                >
                  <Brain size={18} className="shrink-0" />
                  <span>Léa Agent Dev</span>
                </button>
              </div>

              <div className="mb-4">
                <button
                  onClick={triggerMaintenance}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-sm rounded-lg bg-red-500/5 hover:bg-red-500/20 border border-red-500/20 text-red-400 hover:drop-shadow-[0_0_10px_rgba(239,68,68,0.5)] transition-all duration-300"
                >
                  <ShieldAlert size={18} className="shrink-0" />
                  <span>Alerte Maintenance</span>
                </button>
              </div>

              {/* 👁️ LÉA LIVE : AIGUILLAGE HYBRIDE (LINUX NATIVE vs ANDROID S23 ULTRA) */}
              <div className="mb-4">
                <button 
                  onClick={() => {
                    if (Capacitor.isNativePlatform()) {
                      console.log("📱 [S23 Ultra] Bascule sur le module radar natif Capacitor...");
                      setActiveModule('live');
                    } else {
                      console.log("🚀 [Zorin OS] Lancement de l'application native bureau...");
                      window.location.href = 'lealive://start';
                    }
                  }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl transition-all font-black uppercase tracking-widest text-[11px] border shadow-[0_0_15px_rgba(99,102,241,0.2)] bg-indigo-600/10 hover:bg-indigo-600 hover:scale-[1.02] border-indigo-500/40 text-indigo-400 hover:text-white"
                >
                  <Activity size={18} className="shrink-0 animate-pulse text-[#00f2ff]" />
                  <span>Léa Live (APP NATIVE)</span>
                </button>
              </div>
            </>
          )}

          {/* HUB : COMMUNICATION */}
          <details className="group mb-2">
            <summary className="flex items-center justify-between p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 cursor-pointer outline-none transition-colors">
              <span className="text-[10px] font-bold tracking-widest uppercase">{t('hub_communication')}</span>
              <ChevronDown size={14} className="group-open:rotate-180 transition-transform" />
            </summary>
            <div className="pl-3 mt-1 space-y-1 border-l border-white/10 ml-3">
              <button onClick={() => setActiveModule('chat')} className={`w-full flex items-center gap-3 p-2 rounded-lg transition-all text-sm ${activeModule === 'chat' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}><MessageSquarePlus size={16} /> Léa Chat</button>
              <button onClick={() => setActiveModule('languages')} className={`w-full flex items-center gap-3 p-2 rounded-lg transition-all text-sm ${activeModule === 'languages' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}><Globe size={16} /> Léa Langues</button>
            </div>
          </details>

          {/* HUB : ACADÉMIE & TRAVAIL */}
          <details className="group mb-2">
            <summary className="flex items-center justify-between p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 cursor-pointer outline-none transition-colors">
              <span className="text-[10px] font-bold tracking-widest uppercase">Académie & Pro</span>
              <ChevronDown size={14} className="group-open:rotate-180 transition-transform" />
            </summary>
            <div className="pl-3 mt-1 space-y-1 border-l border-white/10 ml-3">
              <button onClick={() => setActiveModule('academy')} className={`w-full flex items-center gap-3 p-2 rounded-lg transition-all text-sm ${activeModule === 'academy' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}><Bot size={16} /> Léa Academy</button>
            </div>
          </details>

          {/* HUB : LA FORTERESSE */}
          <details className="group mb-2">
            <summary className="flex items-center justify-between p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 cursor-pointer outline-none transition-colors">
              <span className="text-[10px] font-bold tracking-widest uppercase">La Forteresse</span>
              <ChevronDown size={14} className="group-open:rotate-180 transition-transform" />
            </summary>
            <div className="pl-3 mt-1 space-y-1 border-l border-white/10 ml-3">
              <button onClick={() => setActiveModule('protect')} className={`w-full flex items-center gap-3 p-2 rounded-lg transition-all text-sm ${activeModule === 'protect' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}><ShieldCheck size={16} /> Léa Protect</button>
              <button onClick={() => setActiveModule('crypto')} className={`w-full flex items-center gap-3 p-2 rounded-lg transition-all text-sm ${activeModule === 'crypto' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}><Coins size={16} /> Léa Crypto</button>
            </div>
          </details>

          {/* ACCÈS DIRECT : STUDIO NANO */}
          <div className="mb-1">
            <button
              onClick={() => setActiveModule('studio_nano')}
              className={`w-full flex items-center gap-3 p-2 rounded-xl transition-all outline-none ${activeModule === 'studio_nano' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}
            >
              <Palette size={16} className={activeModule === 'studio_nano' ? 'text-[#00f2ff]' : 'text-slate-400'} />
              <span className="text-[10px] font-bold tracking-widest uppercase">Studio Nano</span>
            </button>
          </div>

          {/* ACCÈS DIRECT : FORGE 3D */}
          <div className="mb-2">
            <button
              onClick={async () => {
                if (Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android') {
                  try {
                    let currentUser = '';
                    try { currentUser = localStorage.getItem('lea_currentUser') || ''; } catch (e) {}
                    await LeaPhone.openForge3D({ currentUser });
                  } catch (e) { console.error('Erreur Forge3D natif', e); }
                } else {
                  setActiveModule('studio_forge');
                }
              }}
              className={`w-full flex items-center gap-3 p-2 rounded-xl transition-all outline-none ${activeModule === 'studio_forge' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}
            >
              <Box size={16} className={activeModule === 'studio_forge' ? 'text-cyan-400' : 'text-slate-400'} />
              <span className="text-[10px] font-bold tracking-widest uppercase">Forge 3D</span>
            </button>

            <button
              onClick={() => setActiveModule('studio_veo')}
              className={`w-full flex items-center gap-3 p-2 rounded-xl transition-all outline-none ${activeModule === 'studio_veo' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}
            >
              <Film size={16} className={activeModule === 'studio_veo' ? 'text-purple-400' : 'text-slate-400'} />
              <span className="text-[10px] font-bold tracking-widest uppercase">Studio Veo</span>
            </button>

            <button
              onClick={() => setActiveModule('studio_lyria')}
              className={`w-full flex items-center gap-3 p-2 rounded-xl transition-all outline-none ${activeModule === 'studio_lyria' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}
            >
              <Music size={16} className={activeModule === 'studio_lyria' ? 'text-fuchsia-400' : 'text-slate-400'} />
              <span className="text-[10px] font-bold tracking-widest uppercase">Studio Lyria</span>
            </button>

            <button
              onClick={() => setActiveModule('studio_montage')}
              className={`w-full flex items-center gap-3 p-2 rounded-xl transition-all outline-none ${activeModule === 'studio_montage' ? 'border-l-2 border-[#00ffff] border-r-0 border-t-0 border-b-0 bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-l-none' : 'text-white/60 bg-transparent border-transparent hover:text-white hover:bg-white/5 hover:border-white/10'}`}
            >
              <Film size={16} className={activeModule === 'studio_montage' ? 'text-pink-400' : 'text-slate-400'} />
              <span className="text-[10px] font-bold tracking-widest uppercase">Studio Montage</span>
            </button>
          </div>

            </div>
            {/* ═══ FIN ZONE SCROLLABLE ═══════════════════════════════ */}

            {/* BAS : Abonnements et Paramètres — fixe, toujours visible */}
            <div className="pt-4 border-t border-white/10 flex flex-col gap-3 relative shrink-0">
              
              <button
                onClick={() => setActiveModule('shop')}
                style={{ backgroundImage: 'none' }}
                className="w-full flex items-center gap-2 px-4 py-2.5 rounded-[8px] bg-transparent border border-[#00ffff]/30 text-[#00ffff] transition-all hover:bg-[#00ffff]/10 hover:drop-shadow-[0_0_12px_rgba(0,255,255,0.6)]"
              >
                <Coins size={15} />
                <span className="font-bold text-xs uppercase tracking-wider flex-1 text-left">{t('sidebar_tokens')}</span>
                <span className="font-mono font-black text-sm">{userTokens}</span>
              </button>

              {showSettings && (
                <div className="absolute bottom-full mb-4 left-0 w-full bg-[#000b1e]/95 backdrop-blur-2xl border border-white/10 rounded-2xl p-4 shadow-2xl z-50 animate-in fade-in slide-in-from-bottom-2">
                  <div className="flex items-center gap-3 border-b border-white/10 pb-3 mb-3">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#0047ff] to-[#00f2ff] flex items-center justify-center">
                      <User size={16} className="text-white" />
                    </div>
                    <div>
                      <p className="text-white font-bold text-sm leading-tight">{currentUser}</p>
                      <p className="text-[#00f2ff] text-[10px] uppercase tracking-widest font-black">{userRole}</p>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <button
                      onClick={() => {
                        setActiveModule('settings');
                        setShowSettings(false);
                      }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-slate-300 hover:text-white hover:bg-white/5 rounded-lg text-sm transition-colors"
                    >
                      <Settings size={16} /> {t('settings_general')}
                    </button>
                    <button
                      onClick={() => { setShowSettings(false); setActiveModule('updates'); }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-slate-300 hover:text-white hover:bg-white/5 rounded-lg text-sm transition-colors"
                    >
                      <Download size={16} /> {t('settings_updates', 'Mises à jour')}
                    </button>
                    <button
                      onClick={handleLogout}
                      className="w-full flex items-center gap-2 px-3 py-2 text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg text-sm transition-colors border border-red-500/10 mt-2"
                    >
                      <LogOut size={16} /> {t('settings_logout')}
                    </button>
                  </div>
                </div>
              )}

              <button
                onClick={() => setActiveModule('shop')}
                style={{ backgroundImage: 'none' }}
                className="w-full flex items-center gap-2 px-4 py-2.5 rounded-[8px] bg-transparent border border-yellow-500/30 text-yellow-500 transition-all hover:bg-yellow-500/10 hover:drop-shadow-[0_0_12px_rgba(234,179,8,0.6)]"
              >
                <ShoppingBag size={15} />
                <span className="text-[10px] font-black uppercase tracking-widest">Shop · Tokens & Abos</span>
              </button>

              <button
                onClick={() => setShowSettings(!showSettings)}
                className={`flex items-center gap-3 px-5 py-3.5 rounded-xl transition-all ${showSettings ? 'bg-white/10 text-white' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}
              >
                <Settings size={18} className={`${showSettings ? 'rotate-90' : ''} transition-transform duration-300`} />
                <span className="font-medium text-sm">{t('sidebar_settings')}</span>
              </button>
            </div>
          </div>
        </aside>

        {/* ZONE CENTRALE - AVEC RENDU OPÉRATIONNEL DES MODULES */}
        <main className="flex-1 flex flex-col relative overflow-hidden bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-[#001233] via-[#000814] to-[#000814]">
          <button onClick={() => setSidebarOpen(!isSidebarOpen)} className="absolute top-6 left-6 p-3 rounded-xl z-50 transition-all duration-300 text-white/60 hover:text-[#00ffff] hover:scale-110 hover:drop-shadow-[0_0_12px_rgba(0,255,255,1)]">
            <MoreVertical size={22} />
          </button>


          <div className="w-full h-full flex flex-col">
            <ErrorBoundary>
            <Suspense fallback={<LazyFallback />}>
              {activeModule === 'settings' && <LeaSettings />}
              {activeModule === 'terminal' && <LeaTerminal />}
              {activeModule === 'chat' && <LeaChat />}
              {activeModule === 'agent' && <LeaAgent />}
              {activeModule === 'ecosystem' && <LeaEcosystem />}
              {activeModule === 'protect' && <LeaProtect />}
              {activeModule === 'crypto' && <LeaCrypto />}
              {activeModule === 'shop' && <LeaShop />}
              {activeModule === 'updates' && <LeaUpdates />}
              {activeModule === 'live' && <LiveModeNative />}
              {activeModule === 'languages' && <Languages />}
              {activeModule === 'auto' && (
                <div className="flex flex-col items-center justify-center h-full text-slate-400 gap-4">
                  <Car size={48} className="opacity-30" />
                  <p className="text-sm">Léa Auto est disponible uniquement sur Android.</p>
                </div>
              )}
              {activeModule === 'academy' && <LeaAcademy />}
              {activeModule === 'office' && (
                <div className="flex flex-col items-center justify-center h-full text-slate-400 gap-4">
                  <FileText size={48} className="opacity-30" />
                  <p className="text-sm">Léa Office est disponible uniquement sur Android.</p>
                </div>
              )}
              {activeModule === 'home' && (
                <div className="flex flex-col items-center justify-center h-full text-slate-400 gap-4">
                  <Home size={48} className="opacity-30" />
                  <p className="text-sm">Léa Home est disponible uniquement sur Android.</p>
                </div>
              )}
              {activeModule === 'studio_nano' && <StudioNano />}
              {activeModule === 'studio_lyria' && <StudioLyria />}
              {activeModule === 'studio_veo' && <StudioVeo />}
              {activeModule === 'studio_forge' && <StudioForge3D />}
              {activeModule === 'studio_montage' && <StudioMontage onClose={() => setActiveModule('terminal')} />}
            </Suspense>
            </ErrorBoundary>
          </div>
        </main>
        
      </div>

      {/* ── MODAL CONVERSATION LEANOVA ────────────────────────────────────── */}
      {novaModal && (
        <div className="fixed inset-0 z-[300] flex items-center justify-center bg-black/75 backdrop-blur-sm"
          onClick={() => setNovaModal(null)}>
          <div className="bg-[#000b1e] border border-purple-500/30 rounded-2xl p-5 w-[90%] max-w-lg max-h-[80vh] flex flex-col shadow-[0_0_40px_rgba(139,92,246,0.2)]"
            onClick={e => e.stopPropagation()}>

            {/* En-tête */}
            <div className="flex items-center justify-between mb-1 shrink-0">
              <span className="flex items-center gap-2 text-purple-400 font-black text-[11px] uppercase tracking-widest">
                <Mic size={13}/> Échange LeaNova
              </span>
              <div className="flex items-center gap-2">
                <button
                  title={novaModal.isFavorite ? 'Retirer des favoris' : 'Mettre en favori'}
                  className="text-base leading-none hover:scale-110 transition-transform"
                  onClick={() => {
                    const newFav = !novaModal.isFavorite;
                    fetch('/api/nova/favorite', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: currentUser, ts: novaModal.ts, isFavorite: newFav }) })
                      .then(() => {
                        const updated = { ...novaModal, isFavorite: newFav };
                        setNovaModal(updated);
                        setNovaHistory(prev => prev.map(h => h.ts === novaModal.ts ? updated : h));
                      });
                  }}
                >{novaModal.isFavorite ? '⭐' : '☆'}</button>
                <button
                  title="Supprimer cette conversation"
                  className="text-red-400 hover:text-red-300 transition-colors"
                  onClick={() => {
                    fetch('/api/nova/item', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: currentUser, ts: novaModal.ts }) })
                      .then(() => {
                        setNovaHistory(prev => prev.filter(h => h.ts !== novaModal.ts));
                        setNovaModal(null);
                      });
                  }}
                ><Trash2 size={15}/></button>
                <button className="text-slate-400 hover:text-white transition-colors" onClick={() => setNovaModal(null)}><X size={18}/></button>
              </div>
            </div>
            <span className="text-[9px] text-slate-600 font-mono mb-4 shrink-0">
              {new Date(novaModal.ts).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
              {novaModal.isFavorite && <span className="ml-2 text-yellow-500">· favori</span>}
            </span>

            {/* Conversation scrollable */}
            <div className="flex-1 overflow-y-auto space-y-3 pr-1 custom-scrollbar">
              {/* Bulle utilisateur */}
              <div className="flex justify-end">
                <div className="bg-purple-600/20 border border-purple-500/30 rounded-2xl rounded-tr-sm px-4 py-3 max-w-[85%]">
                  <p className="text-white text-sm leading-relaxed">{novaModal.userText}</p>
                </div>
              </div>
              {/* Bulle Léa */}
              <div className="flex justify-start">
                <div className="bg-white/5 border border-white/10 rounded-2xl rounded-tl-sm px-4 py-3 max-w-[85%]">
                  <p className="text-[10px] font-black text-purple-400 mb-1.5 uppercase tracking-widest">LÉA</p>
                  <p className="text-slate-200 text-sm leading-relaxed whitespace-pre-wrap">{novaModal.leaText}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}