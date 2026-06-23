import React, { useState, useEffect } from 'react';
import { 
  Heart, ShieldCheck, MapPin, Camera, Settings, 
  User, RefreshCw, Zap, Navigation, Flame, UserCheck, CheckCircle2,
  Lock, Activity, MessageCircle, X, Send, PhoneCall, Video, Info
} from 'lucide-react';

export const LeaLove = () => {
  // --- IDENTIFICATION SOUVERAINE ---
  const currentUser = localStorage.getItem('lea_currentUser') || '';
  
  // --- ÉTATS DU MODULE ---
  const [activeTab, setActiveTab] = useState<'profil' | 'decouverte' | 'messages'>('profil');
  const [isProfileActive, setIsProfileActive] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  
  // Paramètres Profil
  const [userBio, setUserBio] = useState("");
  const [searchRadius, setSearchRadius] = useState(50);
  
  // États Messagerie
  const [selectedMatch, setSelectedMatch] = useState<string | null>('elena');
  const [messageInput, setMessageInput] = useState("");

  // Faux Matchs pour la démo
  const matches = [
    { id: 'elena', name: 'Elena', age: 26, lastMsg: 'Coucou ! J\'ai vu que tu aimais la tech ?', time: '10:42', unread: true },
    { id: 'clara', name: 'Clara', age: 24, lastMsg: 'On se capte ce week-end ?', time: 'Hier', unread: false },
  ];

  // Faux Messages
  const [chatMessages, setChatMessages] = useState([
    { id: 1, sender: 'elena', text: 'Coucou ! J\'ai vu que tu aimais la tech ?', time: '10:42' }
  ]);

  const handleSyncProfile = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
      setIsProfileActive(true);
    }, 1500);
  };

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageInput.trim()) return;
    setChatMessages([...chatMessages, { id: Date.now(), sender: currentUser, text: messageInput, time: '10:45' }]);
    setMessageInput("");
  };

  return (
    <div className="w-full h-full pt-20 px-6 md:px-10 pb-10 flex flex-col font-sans relative overflow-hidden bg-[#00050d]">
      
      {/* HEADER LÉA LOVE */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-6 z-20 relative">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <div className="p-3 bg-pink-500/20 rounded-xl border border-pink-500/30 shadow-[0_0_15px_rgba(236,72,153,0.3)]">
              <Heart size={24} className="text-pink-500 animate-pulse" fill="currentColor" />
            </div>
            <h2 className="text-3xl font-black text-white tracking-tighter uppercase">Léa Love</h2>
          </div>
          <p className="text-slate-400 text-xs tracking-widest flex items-center gap-2 uppercase font-bold">
            <ShieldCheck size={14} className="text-pink-500" /> Connexion ultra-sécurisée & cryptée.
          </p>
        </div>

        {/* NAVIGATION SI PROFIL ACTIF */}
        {isProfileActive && (
          <div className="flex bg-[#000b1e]/80 border border-white/10 p-2 rounded-2xl backdrop-blur-xl shadow-xl w-full md:w-auto">
            <button 
              onClick={() => setActiveTab('profil')} 
              className={`flex-1 md:flex-none px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 justify-center ${activeTab === 'profil' ? 'bg-white/10 text-white shadow-inner' : 'text-slate-500 hover:text-white'}`}
            >
              <User size={14} /> Mon Profil
            </button>
            <button 
              onClick={() => setActiveTab('decouverte')} 
              className={`flex-1 md:flex-none px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 justify-center ${activeTab === 'decouverte' ? 'bg-pink-500 text-white shadow-[0_0_15px_rgba(236,72,153,0.4)]' : 'text-slate-500 hover:text-white'}`}
            >
              <Flame size={14} /> Découverte
            </button>
            <button 
              onClick={() => setActiveTab('messages')} 
              className={`flex-1 md:flex-none px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 justify-center ${activeTab === 'messages' ? 'bg-indigo-500 text-white shadow-[0_0_15px_rgba(79,70,229,0.4)]' : 'text-slate-500 hover:text-white'}`}
            >
              <MessageCircle size={14} /> Matchs (2)
            </button>
          </div>
        )}
      </div>

      <div className="flex-1 overflow-hidden relative z-20">
        
        {/* ========================================================= */}
        {/* ÉCRAN 1 : ACTIVATION / SYNCHRONISATION DU COMPTE          */}
        {/* ========================================================= */}
        {!isProfileActive && (
           <div className="h-full flex items-center justify-center animate-in fade-in zoom-in-95 duration-500">
             <div className="max-w-md w-full bg-[#000b1e]/90 border border-pink-500/30 rounded-[2rem] p-8 backdrop-blur-xl shadow-[0_0_50px_rgba(236,72,153,0.1)] flex flex-col items-center text-center">
                <div className="w-24 h-24 bg-gradient-to-br from-pink-500 to-indigo-600 rounded-full flex items-center justify-center mb-6 shadow-2xl">
                  <UserCheck size={40} className="text-white" />
                </div>
                <h3 className="text-2xl font-black text-white uppercase tracking-tighter mb-2">Synchronisation Léa</h3>
                <p className="text-sm text-slate-400 mb-8 leading-relaxed">
                  Bienvenue <span className="text-pink-400 font-bold">{currentUser}</span>. Votre compte système a été détecté. Souhaitez-vous activer votre profil sécurisé Léa Love ?
                </p>
                
                <div className="w-full space-y-4">
                  <div className="bg-black/50 border border-white/5 p-4 rounded-xl flex items-center gap-4 text-left">
                    <ShieldCheck size={20} className="text-emerald-500 shrink-0" />
                    <div>
                      <p className="text-xs font-bold text-white">Vérification de l'identité</p>
                      <p className="text-[10px] text-slate-500">Validée par Léa Protect (Zorin OS)</p>
                    </div>
                  </div>
                  <div className="bg-black/50 border border-white/5 p-4 rounded-xl flex items-center gap-4 text-left">
                    <MapPin size={20} className="text-blue-500 shrink-0" />
                    <div>
                      <p className="text-xs font-bold text-white">Localisation chiffrée (PostGIS)</p>
                      <p className="text-[10px] text-slate-500">Floutage anti-harcèlement activé par défaut</p>
                    </div>
                  </div>
                </div>

                <button 
                  onClick={handleSyncProfile}
                  disabled={isSyncing}
                  className="w-full mt-8 py-5 bg-pink-600 hover:bg-pink-500 text-white rounded-xl font-black uppercase tracking-widest text-xs transition-all shadow-[0_0_30px_rgba(219,39,119,0.4)] disabled:opacity-50 flex justify-center items-center gap-2"
                >
                  {isSyncing ? <RefreshCw className="animate-spin" size={16} /> : <Zap size={16} />}
                  {isSyncing ? "Création du profil crypté..." : "Activer mon profil Léa Love"}
                </button>
             </div>
           </div>
        )}

        {/* ========================================================= */}
        {/* ÉCRAN 2 : GESTION DU PROFIL                               */}
        {/* ========================================================= */}
        {isProfileActive && activeTab === 'profil' && (
          <div className="h-full flex flex-col lg:flex-row gap-6 animate-in slide-in-from-bottom-8">
            <div className="w-full lg:w-1/3 bg-[#000b1e]/80 border border-white/10 rounded-[2rem] p-6 flex flex-col backdrop-blur-md">
              <h4 className="text-xs font-black text-pink-500 uppercase tracking-widest mb-6 flex items-center gap-2">
                <Camera size={16} /> Galerie Sécurisée (MinIO)
              </h4>
              <div className="grid grid-cols-2 gap-4 flex-1">
                {[1, 2, 3, 4].map((slot) => (
                  <button key={slot} className="bg-black/40 border border-dashed border-white/20 hover:border-pink-500/50 rounded-2xl flex flex-col items-center justify-center gap-2 text-slate-500 hover:text-pink-400 transition-colors group">
                    <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center group-hover:scale-110 transition-transform">
                      <Camera size={18} />
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-widest">Ajouter</span>
                  </button>
                ))}
              </div>
              <p className="text-[9px] text-slate-500 text-center mt-4 uppercase tracking-widest">Les photos sont chiffrées sur le serveur local.</p>
            </div>

            <div className="flex-1 bg-gradient-to-b from-[#000b1e]/90 to-[#00050d] border border-white/10 rounded-[2rem] p-8 backdrop-blur-xl shadow-2xl flex flex-col">
              <div className="flex items-center justify-between mb-8 pb-6 border-b border-white/5">
                <div>
                  <h3 className="text-2xl font-black text-white">{currentUser} <span className="text-pink-500 ml-2">28</span></h3>
                  <p className="text-xs text-emerald-400 font-bold flex items-center gap-1 mt-1"><CheckCircle2 size={12}/> Profil vérifié par l'IA</p>
                </div>
                <button className="px-4 py-2 bg-white/5 hover:bg-white/10 rounded-lg text-[10px] font-bold text-white uppercase tracking-widest transition-colors border border-white/10">
                  Aperçu public
                </button>
              </div>

              <div className="space-y-8 flex-1 overflow-y-auto custom-scrollbar pr-2">
                <div>
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-3 block">À propos de moi</label>
                  <textarea 
                    value={userBio}
                    onChange={(e) => setUserBio(e.target.value)}
                    placeholder="Décris-toi en quelques mots..."
                    className="w-full bg-black/40 border border-white/10 rounded-xl p-4 text-sm text-slate-200 outline-none focus:border-pink-500/50 resize-none h-32"
                  />
                </div>

                <div className="bg-black/40 p-6 rounded-2xl border border-white/5">
                  <div className="flex justify-between items-center mb-6">
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest flex items-center gap-2"><Navigation size={14} className="text-blue-400"/> Rayon de recherche (PostGIS)</label>
                    <span className="text-sm font-black text-white">{searchRadius} km</span>
                  </div>
                  <input type="range" min="1" max="150" value={searchRadius} onChange={(e) => setSearchRadius(parseInt(e.target.value))} className="w-full accent-pink-500" />
                  <div className="flex justify-between mt-2 text-[9px] text-slate-600 font-bold"><span>1 km</span><span>150 km</span></div>
                  <div className="mt-4 p-3 bg-blue-500/10 border border-blue-500/20 rounded-lg flex gap-3">
                    <Lock size={14} className="text-blue-400 shrink-0 mt-0.5" />
                    <p className="text-[10px] text-blue-300 leading-relaxed">
                      Léa Protect garantit l'anonymat. Votre position exacte ne sera jamais révélée.
                    </p>
                  </div>
                </div>

                <button className="w-full py-4 bg-white/10 hover:bg-white/20 text-white rounded-xl font-black uppercase tracking-widest text-xs transition-colors">
                  Sauvegarder les modifications
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================= */}
        {/* ÉCRAN 3 : MOTEUR DE DÉCOUVERTE (SWIPE & RADAR)            */}
        {/* ========================================================= */}
        {isProfileActive && activeTab === 'decouverte' && (
          <div className="h-full flex flex-col lg:flex-row gap-6 animate-in slide-in-from-bottom-8">
            <div className="flex-1 flex flex-col items-center justify-center relative">
              <div className="w-full max-w-sm aspect-[3/4] bg-[#000b1e] border border-pink-500/30 rounded-[3rem] shadow-[0_0_50px_rgba(236,72,153,0.15)] overflow-hidden relative group">
                <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-black/90 z-10"></div>
                <div className="absolute inset-0 flex items-center justify-center bg-black/40">
                  <User size={100} className="text-pink-500/20" />
                </div>
                <div className="absolute bottom-0 left-0 right-0 p-8 z-20">
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="text-3xl font-black text-white">Elena</h3>
                    <span className="text-2xl font-light text-pink-400">26</span>
                    <CheckCircle2 size={16} className="text-emerald-400 ml-2" />
                  </div>
                  <p className="text-xs text-slate-300 mb-4 flex items-center gap-2">
                    <MapPin size={12} className="text-blue-400" /> À environ 12 km (Zone sécurisée)
                  </p>
                </div>
                <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-30 flex gap-6">
                  <button className="w-16 h-16 bg-black border-2 border-white/10 rounded-full flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition-all shadow-xl hover:scale-110">
                    <X size={24} />
                  </button>
                  <button className="w-20 h-20 bg-gradient-to-br from-pink-500 to-pink-700 rounded-full flex items-center justify-center text-white transition-all shadow-[0_0_30px_rgba(236,72,153,0.5)] hover:scale-110 border-4 border-[#00050d]">
                    <Heart size={32} className="fill-current" />
                  </button>
                </div>
              </div>
            </div>

            <div className="w-full lg:w-80 bg-[#000b1e]/90 border border-white/10 rounded-[2rem] p-6 flex flex-col shrink-0 shadow-2xl backdrop-blur-xl">
              <h4 className="text-xs font-black text-blue-400 uppercase tracking-widest mb-6 flex items-center gap-2 pb-4 border-b border-white/10">
                <Activity size={16} /> Radar PostGIS
              </h4>
              <div className="aspect-square bg-[#00050d] rounded-full border border-blue-500/30 flex items-center justify-center relative overflow-hidden shadow-[inset_0_0_50px_rgba(59,130,246,0.1)] mb-8">
                <div className="absolute w-full h-full border border-blue-500/10 rounded-full animate-[ping_3s_linear_infinite]"></div>
                <div className="absolute w-2/3 h-2/3 border border-blue-500/20 rounded-full"></div>
                <div className="w-3 h-3 bg-blue-500 rounded-full shadow-[0_0_10px_#3b82f6] z-10 relative"></div>
                <div className="absolute top-1/4 left-1/4 w-3 h-3 bg-pink-500 rounded-full blur-[2px] animate-pulse"></div>
              </div>
              <div className="bg-black/40 p-4 rounded-xl border border-white/5 mb-4">
                <p className="text-[10px] font-black uppercase text-slate-500 tracking-widest mb-1">Membres Actifs ({searchRadius}km)</p>
                <p className="text-xl font-black text-white">42</p>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================= */}
        {/* ÉCRAN 4 (BLOC 3) : MESSAGERIE SÉCURISÉE DES MATCHS        */}
        {/* ========================================================= */}
        {isProfileActive && activeTab === 'messages' && (
          <div className="h-full flex gap-6 animate-in slide-in-from-bottom-8">
            
            {/* Sidebar : Liste des Matchs */}
            <div className="w-80 bg-[#000b1e]/90 border border-white/10 rounded-[2rem] shadow-2xl backdrop-blur-xl flex flex-col shrink-0 overflow-hidden">
              <div className="p-6 border-b border-white/5">
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <Heart size={18} className="text-pink-500" fill="currentColor"/> Nouveaux Matchs
                </h3>
              </div>
              <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1">
                {matches.map(match => (
                  <button 
                    key={match.id}
                    onClick={() => setSelectedMatch(match.id)}
                    className={`w-full p-4 flex items-center gap-4 rounded-2xl transition-all text-left ${selectedMatch === match.id ? 'bg-indigo-500/20 border border-indigo-500/50 shadow-[inset_4px_0_0_#6366f1]' : 'hover:bg-white/5 border border-transparent'}`}
                  >
                    <div className="relative">
                      <div className="w-12 h-12 bg-black border border-white/10 rounded-full flex items-center justify-center text-pink-500">
                        <User size={20} />
                      </div>
                      {match.unread && <div className="absolute top-0 right-0 w-3 h-3 bg-pink-500 rounded-full border-2 border-[#000b1e]"></div>}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-baseline mb-1">
                        <h4 className="text-sm font-bold text-white truncate">{match.name}</h4>
                        <span className="text-[9px] text-slate-500 font-bold">{match.time}</span>
                      </div>
                      <p className={`text-xs truncate ${match.unread ? 'text-white font-bold' : 'text-slate-500'}`}>{match.lastMsg}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Fenêtre de Chat Cryptée */}
            <div className="flex-1 bg-[#00050d]/80 border border-white/10 rounded-[2rem] shadow-2xl backdrop-blur-xl flex flex-col overflow-hidden relative">
              {selectedMatch ? (
                <>
                  <div className="p-6 border-b border-white/5 bg-gradient-to-b from-white/[0.02] to-transparent flex justify-between items-center">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-black border border-white/10 rounded-full flex items-center justify-center text-pink-500">
                        <User size={20} />
                      </div>
                      <div>
                        <h2 className="text-lg font-black text-white flex items-center gap-2">
                          Elena <CheckCircle2 size={14} className="text-emerald-400" />
                        </h2>
                        <p className="text-[10px] text-emerald-400 uppercase tracking-widest font-bold flex items-center gap-1">
                          <Lock size={10} /> Canal Crypté
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button className="p-3 bg-white/5 hover:bg-emerald-500/20 hover:text-emerald-400 rounded-xl transition-all text-slate-400"><PhoneCall size={18}/></button>
                      <button className="p-3 bg-white/5 hover:bg-blue-500/20 hover:text-blue-400 rounded-xl transition-all text-slate-400"><Video size={18}/></button>
                      <button className="p-3 bg-white/5 hover:bg-white/10 rounded-xl transition-all text-slate-400"><Info size={18}/></button>
                    </div>
                  </div>

                  <div className="flex-1 overflow-y-auto custom-scrollbar p-6 space-y-6">
                    <div className="flex justify-center mb-8">
                      <div className="bg-indigo-500/10 border border-indigo-500/20 px-4 py-2 rounded-full flex items-center gap-2">
                        <ShieldCheck size={12} className="text-indigo-400" />
                        <span className="text-[9px] font-black uppercase tracking-widest text-indigo-400">Match vérifié - Connexion locale sécurisée</span>
                      </div>
                    </div>

                    {chatMessages.map((msg) => {
                      const isMe = msg.sender === currentUser;
                      return (
                        <div key={msg.id} className={`flex flex-col ${isMe ? 'items-end' : 'items-start'} animate-in slide-in-from-bottom-2`}>
                          <div className={`max-w-[70%] p-4 rounded-[2rem] text-sm ${isMe ? 'bg-indigo-600 text-white rounded-tr-none shadow-[0_5px_15px_rgba(79,70,229,0.3)]' : 'bg-[#000b1e] border border-white/10 text-slate-200 rounded-tl-none'}`}>
                            {msg.text}
                          </div>
                          <span className="text-[9px] text-slate-600 font-bold mt-1 px-2">{msg.time}</span>
                        </div>
                      );
                    })}
                  </div>

                  <div className="p-6 bg-black/40 border-t border-white/5">
                    <form onSubmit={sendMessage} className="flex gap-3">
                      <input 
                        type="text" 
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        placeholder="Écrire un message crypté..."
                        className="flex-1 bg-[#00050d] border border-white/10 focus:border-indigo-500/50 rounded-2xl px-4 py-3 text-sm text-white outline-none transition-colors"
                      />
                      <button type="submit" disabled={!messageInput.trim()} className="w-12 h-12 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl flex items-center justify-center transition-all disabled:opacity-50 shadow-[0_0_15px_rgba(79,70,229,0.3)] shrink-0">
                        <Send size={18} className="ml-1" />
                      </button>
                    </form>
                  </div>
                </>
              ) : (
                <div className="flex-1 flex items-center justify-center flex-col text-slate-500">
                  <MessageCircle size={48} className="mb-4 opacity-20" />
                  <p className="text-sm font-black uppercase tracking-widest">Sélectionnez un match</p>
                </div>
              )}
            </div>
            
          </div>
        )}
      </div>
    </div>
  );
};