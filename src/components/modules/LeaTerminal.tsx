import React, { useState, useEffect, useRef } from 'react';
import { 
  Send, Paperclip, Plus, Mic, Copy, Video, Sparkles, ShieldCheck,
  Play, Pause, Download,HelpCircle, Info, Box, Loader2, MapPin, Navigation, Heart, 
  UserCheck, Bot, Cpu, Bitcoin, ChevronDown, TrendingUp, Music, Volume2, X, Square,
  ThumbsUp, ThumbsDown
} from 'lucide-react';
// --- NOUVEAUX COMPOSANTS (Effet d'écriture et Bouton Vocal) ---

const TypewriterText = ({ text }: { text: string }) => {
  const [displayedText, setDisplayedText] = useState('');
  const [votes, setVotes] = useState<Record<string, 'up' | 'down'>>({});

  useEffect(() => {
    setDisplayedText('');
    let i = 0;
    const intervalId = setInterval(() => {
      setDisplayedText(text.slice(0, i + 1));
      i++;
      if (i >= text.length) clearInterval(intervalId);
    }, 15); // Vitesse de frappe de Léa
    return () => clearInterval(intervalId);
  }, [text]);

  return <p className="whitespace-pre-wrap font-light mb-2">{displayedText}</p>;
};

const VoiceButton = ({ text }: { text: string }) => {
      const [status, setStatus] = useState<'idle' | 'loading' | 'playing'>('idle');
      const audioRef = useRef<HTMLAudioElement | null>(null);
      const currentUser = localStorage.getItem('lea_currentUser') || 'Invité';

      const handleVoiceRequest = async (e: React.MouseEvent) => {
        e.stopPropagation(); // Bloque le clic fantôme
        if (status === 'playing') {
          audioRef.current?.pause();
          setStatus('idle');
          return;
        }

        setStatus('loading');
        try {
          // CORRECTION MAJEURE : On utilise un chemin "relatif" (/api/voice/speak). 
          // Comme ça, le navigateur gère tout seul le passage via le tunnel Cloudflare (HTTPS).
          const response = await fetch(`/api/voice/speak`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text, username: currentUser })
          });
          
          if (!response.ok) throw new Error("Le serveur a refusé la connexion");
          const data = await response.json();
          
          if (data.success && data.audio) {
            const audio = new Audio(data.audio);
            audioRef.current = audio;
            audio.volume = 1;
            audio.onended = () => setStatus('idle');
            await audio.play();
            setStatus('playing');
          } else { 
            setStatus('idle'); 
          }
        } catch (err) {
          console.error("❌ Erreur vocale Cloudflare/Serveur:", err);
          setStatus('idle');
        }
      };

      return (
        <button 
          onClick={handleVoiceRequest} 
          className="p-1.5 bg-[#00f2ff]/10 text-[#00f2ff] rounded-full hover:bg-[#00f2ff] hover:text-black transition-all shadow-[0_0_10px_rgba(0,242,255,0.2)] ml-2"
        >
          {status === 'loading' ? <Loader2 size={12} className="animate-spin" /> : status === 'playing' ? <Square size={12} className="fill-current" /> : <Volume2 size={12} />}
        </button>
      );
    };

    // --- 🟢 MOTEUR ÉCONOMIQUE SOUVERAIN ---
  const depenserToken = (cout = 1) => {
    const today = new Date().toDateString();
    let lastReset = localStorage.getItem('lea_last_reset');
    let sub = localStorage.getItem('lea_abonnement') || 'free';
    let dailyLeft = parseInt(localStorage.getItem('lea_daily_left') || '0');
    let tokensLeft = parseInt(localStorage.getItem('lea_tokens') || '0');
    
    // NOUVEAU : Le moteur cherche lui-même le nom du compte (avec "Invité" en roue de secours)
    const userActuel = localStorage.getItem('lea_currentUser') || 'Invité'; 

    // 1. Reset de minuit : on vérifie la date
    if (lastReset !== today) {
      const maxDaily = sub === 'ultra' ? 200 : sub === 'pro' ? 50 : sub === 'ai_plus' ? 10 : 5;
      dailyLeft = maxDaily;
      localStorage.setItem('lea_last_reset', today);
    }

    // 2. Dépense sur le Quota Gratuit Journalier
    if (dailyLeft >= cout) {
      dailyLeft -= cout;
      localStorage.setItem('lea_daily_left', dailyLeft.toString());
      window.dispatchEvent(new Event('storage')); // Met à jour l'affichage ailleurs
      return true;
    }

    // 3. Dépense sur le Solde Mensuel (Tokens)
    if (tokensLeft >= cout) {
      tokensLeft -= cout;
      localStorage.setItem('lea_tokens', tokensLeft.toString());
      
      // On informe discrètement la base de données avec la bonne variable
      fetch(`/api/user/update-tokens`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: userActuel, tokens: tokensLeft })
      }).catch(e => console.error("Sync tokens échouée", e));

      window.dispatchEvent(new Event('storage')); // Met à jour l'affichage ailleurs
      return true;
    }

    // 4. Blocage : Fonds insuffisants
    return false; 
  };

  

// ==========================================
// 1. COMPOSANTS MULTIMÉDIAS (STUDIOS)
// ==========================================

const AudioPlayer = ({ src, title }: { src: string, title: string }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [volume, setVolume] = useState(1);
  const audioRef = useRef<HTMLAudioElement>(null);

  const togglePlay = () => {
    if (isPlaying) audioRef.current?.pause();
    else audioRef.current?.play();
  };

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setProgress((audioRef.current.currentTime / audioRef.current.duration) * 100);
    }
  };

  const handleSeek = (e: React.MouseEvent<HTMLDivElement>) => {
    if (audioRef.current) {
      const rect = e.currentTarget.getBoundingClientRect();
      audioRef.current.currentTime = ((e.clientX - rect.left) / rect.width) * audioRef.current.duration;
    }
  };

  const handleVolume = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setVolume(val);
    if (audioRef.current) audioRef.current.volume = val;
  };

  return (
    <div className="flex flex-col gap-3 w-64 sm:w-80 bg-black/40 border border-white/10 p-4 rounded-2xl mt-2">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 bg-[#00f2ff]/20 rounded-full flex items-center justify-center text-[#00f2ff]">
          <Music size={20} />
        </div>
        <div className="flex-1 overflow-hidden">
          <p className="text-sm font-bold text-white truncate">{title}</p>
          <p className="text-[10px] text-[#00f2ff] uppercase tracking-widest">Fichier Audio .MP3</p>
        </div>
      </div>
      <div className="flex items-center gap-3">
        <button onClick={togglePlay} className="p-2 bg-[#00f2ff] text-black rounded-full hover:bg-white transition-colors">
          {isPlaying ? <Pause size={16} /> : <Play size={16} className="ml-0.5" />}
        </button>
        <div className="flex-1 h-2 bg-white/10 rounded-full cursor-pointer relative" onClick={handleSeek}>
          <div className="absolute top-0 left-0 h-full bg-[#00f2ff] rounded-full transition-all duration-75" style={{ width: `${progress}%` }} />
        </div>
        {/* Contrôle du Volume */}
        <div className="flex items-center gap-1 group">
          <Volume2 size={14} className="text-white/50 group-hover:text-white transition-colors" />
          <input type="range" min="0" max="1" step="0.05" value={volume} onChange={handleVolume} className="w-12 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#00f2ff]" />
        </div>
        <a href={src} download={`Léa_Audio_${Date.now()}.mp3`} className="p-2 text-white/50 hover:text-white transition-colors">
          <Download size={18} />
        </a>
      </div>
      <audio 
        ref={audioRef} src={src} autoPlay={false}
        onPlay={() => setIsPlaying(true)} onPause={() => setIsPlaying(false)}
        onTimeUpdate={handleTimeUpdate} onEnded={() => setIsPlaying(false)} 
        className="hidden" 
      />
    </div>
  );
};

const VideoPlayer = ({ src }: { src: string }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  const togglePlay = () => {
    if (isPlaying) videoRef.current?.pause();
    else videoRef.current?.play();
    setIsPlaying(!isPlaying);
  };

  return (
    <div className="w-full max-w-sm mt-2 rounded-2xl overflow-hidden border border-white/10 bg-black relative group">
      <video ref={videoRef} src={src} className="w-full h-auto" onClick={togglePlay} onEnded={() => setIsPlaying(false)} />
      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-end p-4">
        <div className="flex items-center justify-between">
          <button onClick={togglePlay} className="p-3 bg-[#00f2ff] text-black rounded-full hover:bg-white transition-colors">
            {isPlaying ? <Pause size={18} /> : <Play size={18} className="ml-0.5" />}
          </button>
          <a href={src} download={`Léa_Video_${Date.now()}.mp4`} className="flex items-center gap-2 px-4 py-2 bg-white/10 backdrop-blur-md rounded-xl text-white hover:bg-white/20 transition-all font-bold text-xs uppercase tracking-widest">
            <Download size={14} /> MP4
          </a>
        </div>
      </div>
    </div>
  );
};

const ImageDisplay = ({ src }: { src: string }) => (
  <div className="relative group w-full max-w-sm mt-2 rounded-2xl overflow-hidden border border-white/10">
    <img src={src} alt="Génération IA" className="w-full h-auto object-cover" />
    <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
      <a href={src} download={`Léa_Image_${Date.now()}.png`} className="flex items-center gap-2 px-6 py-3 bg-[#00f2ff] text-black rounded-xl font-bold uppercase tracking-widest shadow-[0_0_20px_rgba(0,242,255,0.4)] hover:scale-105 transition-all">
        <Download size={18} /> Sauvegarder
      </a>
    </div>
  </div>
);

const Object3DCard = () => (
  <div className="flex items-center justify-between w-64 sm:w-80 bg-gradient-to-br from-indigo-900/40 to-black/40 border border-indigo-500/30 p-4 rounded-2xl mt-2">
    <div className="flex items-center gap-4">
      <div className="p-3 bg-indigo-500/20 text-indigo-400 rounded-xl"><Box size={24} /></div>
      <div>
        <p className="text-sm font-bold text-white">Modèle 3D Généré</p>
        <p className="text-[10px] text-indigo-400 uppercase tracking-widest">Fichier .STL prêt</p>
      </div>
    </div>
    <button className="p-3 bg-indigo-500 hover:bg-indigo-400 text-white rounded-xl transition-all shadow-[0_0_15px_rgba(99,102,241,0.4)]">
      <Download size={18} />
    </button>
  </div>
);

const MusicCard = ({ src }: { src: string }) => {
  const [playing, setPlaying] = React.useState(false);
  const [progress, setProgress] = React.useState(0);
  const [duration, setDuration] = React.useState(0);
  const [current, setCurrent] = React.useState(0);
  const ref = React.useRef<HTMLAudioElement>(null);
  const toggle = () => { if (!ref.current) return; playing ? ref.current.pause() : ref.current.play(); setPlaying(!playing); };
  const fmt = (s: number) => `${Math.floor(s/60)}:${String(Math.floor(s%60)).padStart(2,'0')}`;
  return (
    <div className="w-full max-w-sm mt-2 bg-gradient-to-br from-fuchsia-950/60 to-black/60 border border-fuchsia-500/30 rounded-2xl p-4">
      <audio ref={ref} src={src}
        onTimeUpdate={() => { const a = ref.current!; setCurrent(a.currentTime); setProgress(a.duration ? a.currentTime/a.duration*100 : 0); }}
        onLoadedMetadata={() => setDuration(ref.current!.duration)}
        onEnded={() => setPlaying(false)} />
      <div className="flex items-center gap-3 mb-3">
        <div className="w-9 h-9 bg-fuchsia-500/20 rounded-xl flex items-center justify-center shrink-0">
          <Music size={16} className="text-fuchsia-400" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[11px] font-black text-fuchsia-300 uppercase tracking-widest">Musique générée</p>
          <p className="text-[10px] text-slate-500 font-mono">{fmt(current)} / {fmt(duration)}</p>
        </div>
        <a href={src} download className="w-8 h-8 bg-white/5 hover:bg-white/10 rounded-lg flex items-center justify-center transition-all">
          <Download size={13} className="text-slate-400" />
        </a>
      </div>
      <div className="flex items-center gap-3">
        <button onClick={toggle} className="w-9 h-9 bg-fuchsia-500 hover:bg-fuchsia-400 text-white rounded-full flex items-center justify-center transition-all shrink-0 shadow-[0_0_12px_rgba(217,70,239,0.4)]">
          {playing ? <Pause size={14} /> : <Play size={14} className="ml-0.5" />}
        </button>
        <div className="flex-1 h-1.5 bg-white/10 rounded-full overflow-hidden cursor-pointer" onClick={e => {
          const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
          if (ref.current) ref.current.currentTime = ((e.clientX - rect.left) / rect.width) * duration;
        }}>
          <div className="h-full bg-gradient-to-r from-fuchsia-500 to-purple-500 rounded-full transition-all" style={{ width: `${progress}%` }} />
        </div>
      </div>
    </div>
  );
};

// --- Widgets (Conservés intacts) ---
const MapWidget = () => (<div className="w-64 sm:w-80 bg-gradient-to-br from-emerald-900/30 to-black/40 border border-emerald-500/30 p-4 rounded-2xl mt-2"><div className="flex items-center gap-3 mb-4"><div className="w-8 h-8 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400"><MapPin size={16} /></div><div><p className="text-xs font-bold text-emerald-400 uppercase tracking-widest">Léa Maps</p><p className="text-sm font-bold text-white">Veauche ➔ Paris</p></div></div><div className="flex justify-between items-center text-xs text-slate-300 mb-4 bg-black/40 p-2 rounded-lg"><span>Distance : 465 km</span><span>Temps : 4h 32m</span></div><button className="w-full py-2.5 bg-emerald-500/20 hover:bg-emerald-500/40 border border-emerald-500/50 text-emerald-400 rounded-xl flex items-center justify-center gap-2 transition-all font-bold text-xs uppercase tracking-widest"><Navigation size={14} /> Démarrer la navigation</button></div>);
const LoveWidget = () => (<div className="w-64 sm:w-80 bg-gradient-to-br from-pink-900/30 to-black/40 border border-pink-500/30 p-4 rounded-2xl mt-2"><div className="flex items-center gap-3 mb-4"><div className="w-8 h-8 rounded-full bg-pink-500/20 flex items-center justify-center text-pink-400"><Heart size={16} /></div><div><p className="text-xs font-bold text-pink-400 uppercase tracking-widest">Léa Love</p><p className="text-sm font-bold text-white">Activité du profil</p></div></div><div className="flex items-center gap-4 bg-black/40 p-3 rounded-xl mb-4"><div className="w-12 h-12 rounded-full bg-pink-500/20 flex items-center justify-center blur-[2px] overflow-hidden border border-pink-500/50"><UserCheck size={24} className="text-pink-400" /></div><div><p className="text-lg font-black text-white">3 Matchs</p><p className="text-[10px] text-pink-400 uppercase">Aujourd'hui</p></div></div><button className="w-full py-2.5 bg-pink-500/20 hover:bg-pink-500/40 border border-pink-500/50 text-pink-400 rounded-xl flex items-center justify-center gap-2 transition-all font-bold text-xs uppercase tracking-widest">Ouvrir Léa Love</button></div>);
const AgentWidget = () => (<div className="w-64 sm:w-80 bg-gradient-to-br from-amber-900/30 to-black/40 border border-amber-500/30 p-4 rounded-2xl mt-2"><div className="flex items-center gap-3 mb-4"><div className="w-8 h-8 rounded-full bg-amber-500/20 flex items-center justify-center text-amber-400"><Bot size={16} /></div><div><p className="text-xs font-bold text-amber-400 uppercase tracking-widest">Léa Agent</p><p className="text-sm font-bold text-white">Avatar IA Configuré</p></div></div><button className="w-full py-2.5 bg-amber-500/20 hover:bg-amber-500/40 border border-amber-500/50 text-amber-400 rounded-xl flex items-center justify-center gap-2 transition-all font-bold text-xs uppercase tracking-widest"><Cpu size={14} /> Lancer l'Agent</button></div>);
const CryptoWidget = () => (<div className="w-64 sm:w-80 bg-gradient-to-br from-orange-900/30 to-black/40 border border-orange-500/30 p-4 rounded-2xl mt-2"><div className="flex items-center gap-3 mb-4"><div className="w-8 h-8 rounded-full bg-orange-500/20 flex items-center justify-center text-orange-400"><Bitcoin size={16} /></div><div><p className="text-xs font-bold text-orange-400 uppercase tracking-widest">Léa Crypto</p><p className="text-sm font-bold text-white">Analyse du Marché</p></div></div><div className="flex justify-between items-center bg-black/40 p-3 rounded-xl mb-4"><span className="font-bold text-white">BTC / EUR</span><div className="flex items-center gap-1 text-emerald-400"><TrendingUp size={14} /><span className="text-sm font-bold">+2.4%</span></div></div><button className="w-full py-2.5 bg-orange-500/20 hover:bg-orange-500/40 border border-orange-500/50 text-orange-400 rounded-xl flex items-center justify-center gap-2 transition-all font-bold text-xs uppercase tracking-widest">Ouvrir Portefeuille</button></div>);

// ==========================================
// 3. COMPOSANT PRINCIPAL : LÉA TERMINAL
// ==========================================

export const LeaTerminal = () => {
  const [messages, setMessages] = useState<any[]>([]);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [inputText, setInputText] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [socket, setSocket] = useState<WebSocket | null>(null);
  
  // États d'analyse universelle
  const [isGenerating, setIsGenerating] = useState(false);
  const [genData, setGenData] = useState({ text: 'Analyse en profondeur...', progress: 0 });

  // États pour les périphériques réels
  const [isRecording, setIsRecording] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [attachedFile, setAttachedFile] = useState<File | null>(null);
  const [isCameraActive, setIsCameraActive] = useState(false); // Pour le live

  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<BlobPart[]>([]);
  const recordingTimerRef = useRef<number | null>(null);

  const currentUser = localStorage.getItem('lea_currentUser') || 'Invité';

  // --- 🛡️ GARDE-FRONTIÈRE : ANNIVERSAIRE & DOUANE ---
  useEffect(() => {
    const checkStatus = () => {
      const dob = localStorage.getItem('lea_dob');
      const isKyc = localStorage.getItem('lea_kyc_verified') === 'true';
      const giftClaimed = localStorage.getItem('lea_18_gift_claimed') === 'true';
      
      if (!dob) return;

      // Calcul de l'âge précis
      const birthDate = new Date(dob);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const m = today.getMonth() - birthDate.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;

      // 🎂 SCÉNARIO 18 ANS + CADEAU AI+
      if (age >= 18 && !giftClaimed) {
        localStorage.setItem('lea_isAdult', 'true');
        localStorage.setItem('lea_abonnement', 'ai_plus');
        localStorage.setItem('lea_tokens', '3000');
        localStorage.setItem('lea_18_gift_claimed', 'true'); // LE VERROU ANTI-TRICHE
        
        setMessages(prev => [...prev, {
          id: 'bday-' + Date.now(),
          sender: 'Léa',
          originalText: `🎂 JOYEUX 18ème ANNIVERSAIRE ! 🥂\n\nLe monde des adultes s'ouvre à toi sur la Forge. Pour fêter ça, je t'offre 1 mois d'abonnement AI+ et 3000 Tokens ! \n\n⚠️ ATTENTION : Pour débloquer Léa Crypto et les modules sensibles, n'oublie pas de valider ta Douane (KYC).`,
          isMine: false,
          mediaType: 'text'
        }]);
        window.dispatchEvent(new Event('storage'));
      }
      
      // 🛂 RAPPEL DOUANE POUR LES MAJEURS
      if (age >= 18 && !isKyc) {
        // Petit rappel discret dans le terminal au lancement
        console.log("Douane : KYC en attente pour débloquer les modules.");
      }
    };

    checkStatus();
  }, []);

  
  // 🛡️ GESTION DES SESSIONS : Mémoire absolue (Persistance au F5 et aux changements d'onglets)
  const [sessionId, setSessionId] = useState(() => {
    // Au démarrage, on regarde si on a pas déjà une discussion en cours dans le coffre-fort
    const activeSession = localStorage.getItem('lea_active_session_id');
    if (activeSession) return activeSession;
    
    // Sinon, on en crée une nouvelle et on la verrouille
    const newId = Date.now().toString();
    localStorage.setItem('lea_active_session_id', newId);
    return newId;
  });
  const [historySessions, setHistorySessions] = useState<{id: string, name: string}[]>([]);
  const [votes, setVotes] = useState<Record<string, 'up' | 'down'>>({});
  const [isAdult, setIsAdult] = useState(false);
  const [douaneLoading, setDouaneLoading] = useState(false);

// --- ÉTATS DE DOUANE (QUARANTAINE) ---
  const [isDouanePassed, setIsDouanePassed] = useState(true);
  const [quarantineCode, setQuarantineCode] = useState('');
  const [showExemple, setShowExemple] = useState(false);

// --- NOUVEAU : FONCTION RLHF (DRESSAGE MAISON) ---
  const handleFeedback = async (msgIndex: number, isPositive: boolean, msgId: string) => {
    const userMessage = messages[msgIndex - 1]?.originalText || "Action spéciale";
    const leaResponse = messages[msgIndex].originalText;

    // On marque le bouton localement pour qu'il reste allumé
    setVotes(prev => ({ ...prev, [msgId]: isPositive ? 'up' : 'down' }));

    try {
      await fetch(`/api/rlhf/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: currentUser,
          prompt: userMessage,
          response: leaResponse,
          isPositive: isPositive
        })
      });
    } catch (err) {
      console.error("Erreur RLHF:", err);
    }
  };

  // 🛡️ DOUANE SOUVERAINE : Vérification du statut du profil
  useEffect(() => {
    fetch(`https://${window.location.host}/api/user/profile/${currentUser}`)
      .then(res => res.json())
      .then(data => {
        setIsDouanePassed(data.isDouanePassed || false);
        setIsAdult(data.isAdult || false);
        setQuarantineCode(data.quarantineCode || '');
      })
      .catch(err => console.error("Erreur vérification Douane :", err));
  }, [currentUser]);

  // 🛡️ SYNC & CHARGEMENT : On charge la liste ET le contenu de la session (VERSION PRO)
  useEffect(() => {
    // 1. On actualise la liste de la barre latérale
    fetch(`/api/history/${currentUser}`)
      .then(res => res.json())
      .then(data => setHistorySessions(data))
      .catch(err => console.error("Erreur liste:", err));

    // 2. On charge les messages de la discussion actuelle
    fetch(`/api/history/${currentUser}/${sessionId}`)
      .then(res => res.json())
      .then(data => {
        const formattedMessages = data.map((item: any) => {
          let msg = item.message || item;
          // Correction dynamique des URL pour l'audio/image
          if (msg.mediaUrl) {
            const base = `${window.location.protocol}//${window.location.host}`;
            msg.mediaUrl = msg.mediaUrl.replace(/https?:\/\/localhost:3001/g, base);
            msg.mediaUrl = msg.mediaUrl.replace(/https?:\/\/192\.168\.\d+\.\d+:3001/g, base);
          }
          return msg;
        });
        setMessages(formattedMessages);
      })
      .catch(err => console.error("Erreur messages:", err));
  }, [currentUser, sessionId]);

  // 🛡️ DÉTECTEUR DE CLIC SIDEBAR : Pour charger la session choisie dans le menu
  useEffect(() => {
    const checkSavedSession = setInterval(() => {
      const savedSession = localStorage.getItem('lea_currentSession');
      if (savedSession && savedSession !== sessionId) {
        setSessionId(savedSession);
        localStorage.setItem('lea_active_session_id', savedSession); // 🔒 On verrouille la nouvelle session
        setMessages([]); 
        localStorage.removeItem('lea_currentSession'); 
      }
    }, 500);
    return () => clearInterval(checkSavedSession);
  }, [sessionId]);

  // Fonction pour lancer une page blanche (Nouveau Chat)
  const startNewChat = () => {
    const newSession = Date.now().toString();
    localStorage.setItem('lea_active_session_id', newSession); // 🔒 On verrouille la session vierge
    setSessionId(newSession);
    setMessages([]);
  };

  // Fonction pour charger une ancienne discussion
  const loadSession = (id: string) => {
    localStorage.setItem('lea_active_session_id', id); // 🔒 On verrouille la session chargée
    setSessionId(id);
    setMessages([]); 
  };

  // Autoscroll fluide
  useEffect(() => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
    }
  }, [messages, isGenerating]);

  // WebSocket Connexion (VERSION PRO)
  useEffect(() => {
    // Détection auto : wss si on est sur le domaine Cloudflare, ws si on est en local
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}`;
    const ws = new WebSocket(wsUrl);
    
    ws.onopen = () => { setSocket(ws); setIsConnected(true); };
    ws.onclose = () => setIsConnected(false);
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'CHAT' && data.message.sender !== currentUser) {
        
        // Correction dynamique de la voix de Léa pour la 5G
        if (data.message.mediaUrl) {
            const base = `${window.location.protocol}//${window.location.host}`;
            data.message.mediaUrl = data.message.mediaUrl.replace(/https?:\/\/localhost:3001/g, base);
            data.message.mediaUrl = data.message.mediaUrl.replace(/https?:\/\/192\.168\.\d+\.\d+:3001/g, base);
        }
        
        setIsGenerating(false);
        setMessages(prev => [...prev, data.message]);
      }
    };
    return () => { ws.close(); };
  }, [currentUser]);

  // --- LE FREIN D'URGENCE (BOUTON STOP) ---
  const handleStopGeneration = () => {
    if (isConnected && socket) {
      // On envoie le signal d'arrêt immédiat au serveur
      socket.send(JSON.stringify({ type: 'STOP_GENERATION', sessionId: sessionId }));
    }
    setGenData({ text: 'Interruption demandée par l\'Admin...', progress: 100 });
    setTimeout(() => {
      setIsGenerating(false);
    }, 1000);
  };

  // --- NOUVEAU PIPELINE AUDIO HAUTE FIDÉLITÉ (MediaRecorder & Chunks) ---
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaRecorderRef.current = new MediaRecorder(stream);
      audioChunksRef.current = [];

      mediaRecorderRef.current.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
          // Le "Pipe" : on stocke le flux audio par tranches de 250ms
        }
      };

      mediaRecorderRef.current.onstop = () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        
        // Conversion du flux compressé en Base64 pour l'envoi blindé via WebSocket
        const reader = new FileReader();
        reader.readAsDataURL(audioBlob);
        reader.onloadend = () => {
          const base64Audio = reader.result;
          
          if (isConnected && socket) {
            // Envoi direct au serveur sans passer par le texte
            socket.send(JSON.stringify({
              type: 'VOICE_CHAT',
              sessionId: sessionId,
              message: {
                id: Date.now().toString(),
                sender: currentUser,
                audioData: base64Audio, // Le serveur va récupérer ça pour le TTS/Whisper
                isMine: true
              }
            }));
            
            setIsGenerating(true);
            setGenData({ text: 'Léa analyse ton flux vocal...', progress: 0 });
          }
        };
        // On coupe le micro pour des raisons de sécurité
        stream.getTracks().forEach(track => track.stop());
      };

      // Démarrage du pipeline avec des "slices" de 250ms
      mediaRecorderRef.current.start(250);
      setIsRecording(true);
      setRecordingTime(0);
      recordingTimerRef.current = window.setInterval(() => {
        setRecordingTime(prev => prev + 1);
      }, 1000);

    } catch (err) {
      console.error("Accès micro refusé ou introuvable", err);
      alert("Léa Protect : Accès au microphone refusé ou périphérique introuvable.");
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop(); // Déclenche l'événement onstop ci-dessus
    }
    setIsRecording(false);
    if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
  };

  const toggleCamera = () => {
    setIsCameraActive(!isCameraActive);
  };
  
// 📁 Fonction pour sélectionner un fichier (Trombone)
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setAttachedFile(e.target.files[0]);
    }
  };

  // ==========================================
  // L'AIGUILLEUR CENTRAL (Envoi au serveur avec Fichiers)
  // ==========================================
  const handleSend = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim() && !attachedFile) return;

    // 🔴 LE PÉAGE INTELLIGENT : Immunité Master Admin & Chat Illimité
    // 1. On vérifie si c'est toi (currentUser est une simple string ici)
    const isMasterAdmin = localStorage.getItem('lea_userRole') === 'admin';
    
    // 2. On détecte s'il s'agit d'une génération (mots-clés ou fichier joint)
    const isGenerationRequest = /(photo|image|dessin|portrait|illustration|avatar|pochette|musique|vidéo|3d|génère)/i.test(inputText) || attachedFile !== null;

    // Si ce n'est PAS le grand patron ET que c'est une demande de module lourd, on vérifie le solde
    if (!isMasterAdmin && isGenerationRequest) {
        if (!depenserToken(1)) {
            setMessages(prev => [...prev, {
                id: Date.now().toString(),
                sender: 'Léa System',
                originalText: "❌ ALERTE MATRICE : Quota de génération épuisé pour votre abonnement. (Le chat reste gratuit et illimité !)",
                isMine: false,
                mediaType: 'text'
            }]);
            return; // On bloque uniquement l'accès aux générateurs
        }
    }
    
    let textToSend = inputText.trim();
    let base64File = null;
    let fileName = null;
    

    // Si on a une pièce jointe, on la convertit en Base64
    if (attachedFile) {
      textToSend = `[Pièce jointe : ${attachedFile.name}]\n` + textToSend;
      fileName = attachedFile.name;
      
      const reader = new FileReader();
      reader.readAsDataURL(attachedFile);
      await new Promise(resolve => {
          reader.onloadend = () => {
              base64File = reader.result;
              resolve(true);
          };
      });
    }

    const userMsg = { 
      id: Date.now().toString(), 
      sender: currentUser, 
      originalText: textToSend, 
      isMine: true,
      fileData: base64File,
      fileName: fileName
    };
    
    setMessages(prev => [...prev, userMsg]);
    setInputText('');
    setAttachedFile(null);

    setIsGenerating(true);
    setGenData({ text: attachedFile ? 'Léa lit ton document...' : 'Léa analyse la demande...', progress: 0 });

    // Envoi au vrai cerveau (server.cjs) avec les données du fichier
    if (isConnected && socket) {
      socket.send(JSON.stringify({ type: 'CHAT', view: 'terminal_lea', sessionId: sessionId, message: userMsg }));
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };
  // Formatage du temps d'enregistrement (00:00)
  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  // 🛡️ LOGIQUE DOUANE : On vérifie si c'est un adulte et s'il est vérifié
  useEffect(() => {
    fetch(`/api/user/profile/${currentUser}`)
      .then(res => res.json())
      .then(data => {
        setIsDouanePassed(data.isDouanePassed);
        setIsAdult(data.isAdult);
        setQuarantineCode(data.quarantineCode);
      });
  }, [currentUser]);

  const handleDouaneUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setDouaneLoading(true);
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onloadend = async () => {
      const res = await fetch(`/api/auth/douane`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: currentUser, imageBase64: reader.result })
      });
      const data = await res.json();
      if (data.success) { alert("🎉 " + data.message); setIsDouanePassed(true); }
      else { alert("❌ " + data.error); }
      setDouaneLoading(false);
    };
  };

  return (
    <div className="flex h-screen w-full bg-transparent overflow-hidden">

      {/* 💬 ZONE DE CHAT */}
      <div className="flex-1 flex flex-col relative w-full h-full bg-transparent overflow-hidden">

        {/* 🛑 BANNIÈRE DE QUARANTAINE (UNIQUEMENT POUR ADULTES NON VÉRIFIÉS) */}
        {(isAdult && !isDouanePassed) && (
          <div className="absolute top-0 left-0 w-full z-50 bg-red-600/90 backdrop-blur-md text-white px-4 py-3 shadow-[0_0_30px_rgba(239,68,68,0.4)] border-b border-red-400/50 flex flex-col sm:flex-row items-center justify-center gap-2 sm:gap-6 animate-in slide-in-from-top-4">
            <div className="flex items-center gap-2 font-bold text-sm tracking-wide">
              <ShieldCheck size={20} className="text-white animate-pulse" />
              <span>DOUANE OBLIGATOIRE (+18 ANS)</span>
            </div>
            <div className="flex flex-col sm:flex-row items-center gap-3 text-xs sm:text-sm font-medium">
              <span>Prends un selfie de toi normal.</span>
              <button 
  onClick={() => setShowExemple(true)}
  className="ml-4 flex items-center gap-2 bg-white/10 hover:bg-white/20 px-3 py-1 rounded-full text-[10px] font-bold transition-all border border-white/10"
>
  <HelpCircle size={14} /> VOIR L'EXEMPLE
</button>
              <label className={`cursor-pointer bg-white text-red-600 px-4 py-1.5 rounded-lg font-bold hover:bg-gray-200 transition-colors flex items-center gap-2 ${douaneLoading ? 'opacity-50' : ''}`}>
                <Video size={14} />
                {douaneLoading ? 'ANALYSE...' : 'VÉRIFIER MON PROFIL'}
                <input type="file" accept="image/*" capture="user" className="hidden" onChange={handleDouaneUpload} />
              </label>
            </div>
          </div>
        )}

      {/* ZONE DES MESSAGES */}
      <div ref={scrollContainerRef} className="flex-1 overflow-y-auto min-h-0 px-4 md:px-10 lg:px-64 pt-24 pb-8 flex flex-col custom-scrollbar scroll-smooth">
        {messages.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-end pb-16 animate-in fade-in duration-1000">
             <div className="relative inline-block mb-10">
                <div className="absolute inset-0 bg-[#0047ff] blur-[120px] opacity-30 animate-pulse" />
                <div className="relative w-24 h-24 sm:w-28 sm:h-28 rounded-[2rem] bg-gradient-to-br from-[#0047ff] to-[#00f2ff] flex items-center justify-center shadow-[0_0_40px_rgba(0,242,255,0.4)] border border-white/20">
                  <Sparkles size={48} className="text-white drop-shadow-lg" />
                </div>
             </div>
             <h1 className="text-5xl sm:text-7xl md:text-8xl font-black mb-6 tracking-tighter text-transparent bg-clip-text bg-gradient-to-b from-white to-slate-400">
               LÉA <span className="text-[#00f2ff] drop-shadow-[0_0_15px_rgba(0,242,255,0.5)]">V3</span>
             </h1>
             <div className="flex items-center justify-center gap-3 opacity-60">
               <ShieldCheck size={16} className="text-[#00f2ff]" /> 
               <span className="text-[10px] sm:text-xs md:text-sm uppercase tracking-[0.6em] font-bold text-slate-300 text-center">
                 Terminal {currentUser}<br className="sm:hidden" /> Instance Privée
               </span>
             </div>
          </div>
        ) : (
          <div className="space-y-8 w-full pb-10">
            {messages.map((msg, index) => (
              <div key={msg.id + index} className={`flex flex-col ${msg.isMine ? 'items-end' : 'items-start'} group animate-in fade-in slide-in-from-bottom-4 duration-500`}>
                
                {/* EN-TÊTE DU MESSAGE ÉPURÉ */}
        <div className="flex items-center mb-2 px-2">
          <span className="text-[10px] sm:text-xs md:text-sm uppercase tracking-[0.6em] font-bold text-slate-300 opacity-0 group-hover:opacity-100 transition-opacity">
            {/* L'ancien bouton vocal a été retiré d'ici */}
          </span>
        </div>

        <div className={`relative w-max max-w-[90%] md:max-w-[85%] p-4 sm:p-5 rounded-3xl text-sm leading-relaxed ${msg.isMine ? 'bg-[#0047ff]/10 border border-[#0047ff]/30 text-white rounded-tr-none' : 'bg-white/0.05 border border-white/10 text-white rounded-tl-none'}`}>
          
          {/* TEXTE (En machine à écrire pour Léa) */}
          {msg.originalText && (
            msg.isMine 
              ? <p className="whitespace-pre-wrap font-light mb-2">{msg.originalText}</p>
              : <TypewriterText text={msg.originalText} />
          )}
                  
                  {/* AFFICHAGE DES MÉDIAS */}
                  {msg.mediaType === 'audio' && <AudioPlayer src={msg.mediaUrl} title="Léa Audio" />}
                  {msg.mediaType === 'video' && <VideoPlayer src={msg.mediaUrl} />}
                  {msg.mediaType === 'image' && <ImageDisplay src={msg.mediaUrl} />}
                  {msg.mediaType === '3d' && <Object3DCard />}
                  {msg.mediaType === 'music' && <MusicCard src={msg.mediaUrl} />}
                  
                  {msg.widgetType === 'maps' && <MapWidget />}
                  {msg.widgetType === 'love' && <LoveWidget />}
                  {msg.widgetType === 'agent' && <AgentWidget />}
                  {msg.widgetType === 'crypto' && <CryptoWidget />}

                  {/* FOOTER : DRESSAGE, VOCAL & COPIER (Toujours visibles) */}
                  {!msg.isMine && (
                    <div className="flex justify-between items-center mt-3 pt-2 border-t border-white/5 w-full">
                      
                      {/* TOUT À GAUCHE : LES OUTILS */}
                      <div className="flex items-center gap-1">
                        {/* POUCE HAUT */}
                        <button 
                          onClick={() => handleFeedback(index, true, msg.id)} 
                          className={`p-1.5 rounded-md transition-all ${votes[msg.id] === 'up' ? 'text-emerald-400 bg-emerald-400/20 shadow-[0_0_10px_rgba(52,211,153,0.3)]' : 'text-white/20 hover:text-emerald-400 hover:bg-emerald-400/10'}`}
                        >
                          <ThumbsUp className="w-3.5 h-3.5" />
                        </button>

                        {/* POUCE BAS */}
                        <button 
                          onClick={() => handleFeedback(index, false, msg.id)} 
                          className={`p-1.5 rounded-md transition-all ${votes[msg.id] === 'down' ? 'text-red-400 bg-red-400/20 shadow-[0_0_10px_rgba(248,113,113,0.3)]' : 'text-white/20 hover:text-red-400 hover:bg-red-400/10'}`}
                        >
                          <ThumbsDown className="w-3.5 h-3.5" />
                        </button>
                        
                        <div className="w-px h-3 bg-white/10 mx-1"></div>
                        
                        {/* VOCAL */}
                        <div className="scale-90 origin-left">
                          <VoiceButton text={msg.originalText} />
                        </div>

                        <div className="w-px h-3 bg-white/10 mx-1"></div>

                        {/* COPIER (Version icône uniquement) */}
                        <button 
                          onClick={() => {
                            navigator.clipboard.writeText(msg.originalText);
                            // Petit effet visuel au clic
                          }} 
                          className="p-1.5 text-white/20 hover:text-[#00f2ff] hover:bg-[#00f2ff]/10 rounded-md transition-all"
                          title="Copier le texte"
                        >
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {/* PETIT INDICATEUR DE TEMPS À DROITE */}
                      <span className="text-[9px] font-mono text-white/10 uppercase tracking-tighter">
                        {new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* LE BLOC D'ANALYSE EN DIRECT (Style Discret & Raisonnement) */}
            {isGenerating && (
              <div className="flex flex-col items-start mb-4 animate-in fade-in slide-in-from-bottom-2 w-full max-w-2xl">
                <details className="group w-full rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 overflow-hidden cursor-pointer transition-colors">
                  <summary className="flex items-center gap-3 px-4 py-3 list-none outline-none">
                    <Sparkles size={16} className="text-[#00f2ff] animate-pulse" />
                    <span className="text-sm font-semibold text-slate-300 group-open:text-[#00f2ff] transition-colors">{genData.text}</span>
                    <div className="ml-auto flex items-center">
                      <span className="w-4 h-4 rounded-full border-2 border-t-[#00f2ff] border-r-transparent border-b-transparent border-l-transparent animate-spin" />
                    </div>
                  </summary>
                  <div className="px-4 pb-4 pt-2 text-xs text-slate-400 border-t border-white/5 bg-black/20">
                    <ul className="space-y-2 mt-1 font-mono">
                      <li className="flex items-center gap-2"><Cpu size={12} className="text-emerald-400" /> Aiguillage : Recherche du meilleur cerveau dans la matrice...</li>
                      <li className="flex items-center gap-2"><Navigation size={12} className="text-blue-400" /> Contexte : Chargement de la session et des mémoires d'or...</li>
                      <li className="flex items-center gap-2"><Sparkles size={12} className="text-purple-400" /> Inférence : Génération de la réponse en cours...</li>
                    </ul>
                  </div>
                </details>
              </div>
            )}
          </div>
        )}
      </div>

      {/* INPUT ZONE OPÉRATIONNELLE */}
      <div className="px-4 sm:px-10 md:px-32 lg:px-64 pb-2 sm:pb-3 shrink-0 w-full relative z-10">
        {/* Aperçu de la pièce jointe */}
        {attachedFile && (
          <div className="max-w-5xl mx-auto mb-2 flex items-center justify-between bg-white/10 border border-white/20 p-3 rounded-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-2">
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="p-2 bg-[#00f2ff]/20 text-[#00f2ff] rounded-xl"><Paperclip size={16} /></div>
              <span className="text-sm text-white font-medium truncate">{attachedFile.name}</span>
              <span className="text-xs text-slate-400">({(attachedFile.size / 1024 / 1024).toFixed(2)} MB)</span>
            </div>
            <button type="button" onClick={() => setAttachedFile(null)} className="p-1.5 hover:bg-white/10 text-white/50 hover:text-white rounded-lg transition-colors">
              <X size={16} />
            </button>
          </div>
        )}

        <form onSubmit={handleSend} className="max-w-5xl mx-auto relative group">
          <div className="absolute -inset-1 bg-gradient-to-r from-[#0047ff] to-[#00f2ff] rounded-[32px] opacity-10 blur-lg group-hover:opacity-20 transition-opacity duration-500" />
          <div className="relative bg-[#000b1e]/90 backdrop-blur-xl border border-white/10 rounded-[32px] p-1.5 sm:p-2 flex items-end gap-1 sm:gap-2 focus-within:border-[#00f2ff]/50 shadow-2xl transition-all duration-300">
            
            {/* Bouton + : ouvre le sélecteur de fichiers (tous types, sans limite de taille) */}
            <input type="file" ref={fileInputRef} onChange={handleFileSelect} className="hidden" />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="flex-shrink-0 w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-2xl text-white/50 hover:text-[#00f2ff] hover:bg-[#00f2ff]/10 transition-all duration-200 mb-1"
              title="Joindre un fichier"
            >
              <Plus className="w-5 h-5" strokeWidth={2.5} />
            </button>
            
            {isRecording ? (
              <div className="flex-1 flex items-center justify-center gap-4 py-3 sm:py-4">
                <span className="w-3 h-3 bg-red-500 rounded-full animate-pulse shadow-[0_0_10px_rgba(239,68,68,0.6)]" />
                <span className="text-red-400 font-mono font-bold tracking-widest">{formatTime(recordingTime)}</span>
              </div>
            ) : (
              <textarea
                value={inputText}
                onChange={(e) => {
                  setInputText(e.target.value);
                  // Auto-resize : grandit avec le texte
                  e.target.style.height = 'auto';
                  e.target.style.height = Math.min(e.target.scrollHeight, 200) + 'px';
                  // Auto-scroll : curseur toujours visible
                  e.target.scrollTop = e.target.scrollHeight;
                }}
                onKeyDown={handleKeyDown}
                placeholder="Écrivez à Léa (ex: itinéraire Veauche Paris, génère photo, crypto...)"
                className="flex-1 bg-transparent border-none outline-none text-white py-3 sm:py-4 px-3 sm:px-2 resize-none font-light text-sm sm:text-base placeholder-white/30 overflow-y-auto custom-scrollbar"
                style={{ maxHeight: '200px', minHeight: '44px' }}
                rows={1}
              />
            )}
            
            <div className="flex items-center gap-0.5 sm:gap-1 mb-1 mr-1">
              {/* Bouton Micro Opérationnel */}
              {isRecording ? (
                <button type="button" onClick={stopRecording} className="p-2 sm:p-3 text-red-500 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-colors">
                  <Square className="w-5 h-5 fill-current" />
                </button>
              ) : (
                <button type="button" onClick={startRecording} className="p-2 sm:p-3 text-white/40 hover:text-red-400 transition-colors">
                  <Mic className="w-5 h-5" />
                </button>
              )}
              
              {/* Bouton Caméra */}
              <button type="button" onClick={toggleCamera} className={`p-2 sm:p-3 transition-colors hidden sm:block ${isCameraActive ? 'text-[#00f2ff] drop-shadow-[0_0_5px_#00f2ff]' : 'text-white/40 hover:text-yellow-400'}`}>
                <Video className="w-5 h-5" />
              </button>
              
              {/* Bouton Envoi ou STOP D'URGENCE */}
              {isGenerating && !isRecording ? (
                <button 
                  type="button" 
                  onClick={handleStopGeneration} 
                  className="p-3 sm:p-4 bg-red-500 hover:bg-red-400 text-white rounded-2xl transition-all shadow-[0_0_20px_rgba(239,68,68,0.5)] hover:scale-105"
                  title="Forcer l'arrêt de la génération"
                >
                  <Square className="w-5 h-5 fill-current" />
                </button>
              ) : (
                <button 
                  type="submit" 
                  disabled={(!inputText.trim() && !attachedFile) && !isRecording} 
                  className={`p-3 sm:p-4 rounded-2xl transition-all duration-300 ${((inputText.trim() || attachedFile) && !isGenerating) || isRecording ? 'bg-[#0047ff] hover:bg-[#00f2ff] text-white font-bold shadow-[0_0_20px_rgba(0,242,255,0.4)] scale-100' : 'bg-white/5 text-white/20 scale-95 cursor-not-allowed'}`}
                >
                  <Send className="w-5 h-5" />
                </button>
              )}
            </div>
          </div>
        </form>
      </div>

      {showExemple && (
  <div className="fixed inset-0 z-[999] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
    <div className="bg-[#0a0c10] border border-white/10 rounded-3xl max-w-2xl w-full overflow-hidden shadow-2xl">
      {/* Header */}
      <div className="p-6 border-b border-white/5 flex justify-between items-center bg-white/5">
        <h3 className="text-xl font-bold text-[#00f2ff] uppercase tracking-widest flex items-center gap-2">
          <Info size={24} /> Protocole d'Identification
        </h3>
        <button onClick={() => setShowExemple(false)} className="text-slate-500 hover:text-white">
          <X size={24} />
        </button>
      </div>

      {/* Contenu */}
      <div className="p-8 space-y-6">
        <p className="text-slate-300 text-sm leading-relaxed">
          Pour que l'IA puisse valider votre accès, le code doit être écrit de manière <span className="text-white font-bold">parfaitement lisible</span>. 
          Utilisez un feutre noir sur une feuille blanche sans carreaux.
        </p>

          <div className="flex justify-center w-full">
          <div className="space-y-2 text-center max-w-sm">
            <img src="/image/exemple.png" alt="Modèle d'écriture" className="rounded-xl border border-white/10 w-full shadow-lg" />
            <span className="text-[10px] text-slate-500 uppercase tracking-widest">Modèle d'écriture</span>
          </div>
        </div>

        <div className="bg-blue-500/10 border border-blue-500/20 p-4 rounded-2xl">
          <ul className="text-xs text-blue-300 space-y-2">
            <li>• Écrivez en <span className="font-bold underline">BÂTONS DROITS</span> (pas d'écriture attachée).</li>
            <li>• Ne mettez pas de barre au milieu du chiffre 7.</li>
            <li>• Assurez-vous que le tiret "-" est bien visible.</li>
          </ul>
        </div>
      </div>

      {/* Footer */}
      <div className="p-6 bg-white/5 flex justify-center">
        <button 
          onClick={() => setShowExemple(false)}
          className="bg-[#00f2ff] text-black font-black py-3 px-8 rounded-full hover:scale-105 transition-transform uppercase tracking-tighter"
        >
          J'ai compris, je prends mon selfie
        </button>
      </div>
    </div>
  </div>
)}

    </div>
   </div> 
  );
};