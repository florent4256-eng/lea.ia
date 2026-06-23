/// <reference lib="dom" />
import React, { useState, useEffect, useRef } from 'react';
import { 
  Languages as LangIcon, Check, Sparkles, 
  Loader2, Server, Globe2, ArrowRight, Copy, Trash2, CheckCircle2, Mic, Volume2
} from 'lucide-react';

export const Languages = () => {
  const currentUser = localStorage.getItem('lea_currentUser') || '';
  const [socket, setSocket] = useState<WebSocket | null>(null);

  // --- ÉTATS DU MODULE ---
  const [activeLang, setActiveLang] = useState<string>('EN');
  const [inputText, setInputText] = useState('');
  const [outputText, setOutputText] = useState('');
  const [isTranslating, setIsTranslating] = useState(false);
  const [copied, setCopied] = useState(false);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Les langues sont désormais directement gérées par le cerveau local (Ollama)
  type Lang = { n: string; code: string; langName: string; desc: string };
  const availableLangs: Lang[] = [
    { n: 'Français', code: 'FR', langName: 'French', desc: 'Réseau Neuronal Actif' },
    { n: 'English', code: 'EN', langName: 'English', desc: 'Réseau Neuronal Actif' },
    { n: 'Español', code: 'ES', langName: 'Spanish', desc: 'Réseau Neuronal Actif' },
    { n: 'Português', code: 'PT', langName: 'Portuguese', desc: 'Réseau Neuronal Actif' },
    { n: 'Deutsch', code: 'DE', langName: 'German', desc: 'Réseau Neuronal Actif' },
    { n: 'Italiano', code: 'IT', langName: 'Italian', desc: 'Réseau Neuronal Actif' }
  ];

  // --- CONNEXION AU SERVEUR LINUX ---
  useEffect(() => {
    const wsUrl = `ws://${window.location.hostname}:3001`;
    const ws = new WebSocket(wsUrl);
    
    ws.onopen = () => setSocket(ws);
    
    ws.onmessage = (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      
      // Réception de la traduction depuis le serveur
      if (data.type === 'TRANSLATION_RESULT' && data.user === currentUser) {
        setOutputText(data.translatedText);
        if (data.audioUrl) {
          setAudioUrl(data.audioUrl);
        }
        setIsTranslating(false);
      }
    };

    return () => ws.close();
  }, [currentUser]);

  // --- MOTEUR DE TRADUCTION RÉEL ---
  const handleTranslate = () => {
    if (!inputText.trim()) return;
    setIsTranslating(true);
    setAudioUrl(null);
    setOutputText('');

    const targetLang = availableLangs.find(l => l.code === activeLang)?.langName || 'English';

    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({
        type: 'TRANSLATE_REQUEST',
        user: currentUser,
        text: inputText,
        targetLanguage: targetLang
      }));
    } else {
      // Fallback si le serveur est coupé (pour éviter le crash de l'interface)
      setTimeout(() => {
        setOutputText("[ERREUR] Connexion au cerveau local (port 3001) interrompue.");
        setIsTranslating(false);
      }, 1000);
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(outputText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const clearAll = () => {
    setInputText('');
    setOutputText('');
    setAudioUrl(null);
  };

  const playAudio = () => {
    if (audioUrl && audioRef.current) {
      if (isPlaying) {
        audioRef.current.pause();
        setIsPlaying(false);
      } else {
        audioRef.current.play();
        setIsPlaying(true);
      }
    }
  };

  return (
    <div className="w-full h-full p-6 md:p-10 flex flex-col pt-24 overflow-y-auto custom-scrollbar">
      
      {/* HEADER DU MODULE */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <div className="p-3 bg-[#00f2ff]/20 rounded-xl border border-[#00f2ff]/30 shadow-[0_0_15px_rgba(0,242,255,0.2)]">
              <Globe2 size={24} className="text-[#00f2ff]" />
            </div>
            <h2 className="text-3xl font-black text-white tracking-tighter uppercase">Traduction Souveraine</h2>
          </div>
          <p className="text-slate-400 text-sm tracking-wide flex items-center gap-2">
            <Server size={14} className="text-[#00f2ff]" /> Moteur local actif. Zéro donnée envoyée sur le cloud.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* SÉLECTEUR DE LANGUES (Moteur Local) */}
        <div className="lg:col-span-1 bg-[#000b1e]/80 border border-white/5 rounded-[2rem] p-6 shadow-2xl">
          <div className="flex items-center justify-between mb-6 border-b border-white/5 pb-4">
            <h3 className="font-bold text-white uppercase tracking-widest text-sm flex items-center gap-2">
              <LangIcon size={16} className="text-[#00f2ff]" /> Réseaux Disponibles
            </h3>
          </div>
          
          <div className="space-y-3">
            {availableLangs.map(l => {
              const isActive = activeLang === l.code;
              return (
                <div 
                  key={l.code}
                  onClick={() => setActiveLang(l.code)}
                  className={`flex items-center justify-between p-4 rounded-2xl border transition-all cursor-pointer group ${
                    isActive ? 'bg-[#00f2ff]/10 border-[#00f2ff]/50 shadow-[0_0_15px_rgba(0,242,255,0.1)]' : 'bg-black/40 border-white/5 hover:border-white/20'
                  }`}
                >
                  <div className="flex items-center gap-4">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-black text-sm transition-colors ${
                      isActive ? 'bg-[#00f2ff] text-black shadow-[0_0_10px_#00f2ff]' : 'bg-white/5 text-slate-400'
                    }`}>
                      {l.code}
                    </div>
                    <div className="flex flex-col">
                      <span className={`font-bold text-sm tracking-wide ${isActive ? 'text-white' : 'text-slate-300'}`}>{l.n}</span>
                      <span className="text-[10px] text-slate-500 mt-0.5">{l.desc}</span>
                    </div>
                  </div>

                  {isActive && <CheckCircle2 size={18} className="text-[#00f2ff] drop-shadow-[0_0_5px_rgba(0,242,255,0.5)]" />}
                </div>
              );
            })}
          </div>
        </div>

        {/* ZONE DE TRADUCTION */}
        <div className="lg:col-span-2 flex flex-col gap-4">
          
          {/* Bloc Source */}
          <div className="bg-black/40 border border-white/10 rounded-[2rem] p-1 flex flex-col shadow-inner transition-all focus-within:border-[#0047ff]/50">
            <div className="flex justify-between items-center p-4 border-b border-white/5 bg-white/[0.02] rounded-t-[2rem]">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">Français (Auto-détection)</span>
              <div className="flex gap-2">
                <button className="p-2 text-slate-500 hover:text-[#00f2ff] transition-colors rounded-lg hover:bg-white/5">
                  <Mic size={16} />
                </button>
                <button onClick={clearAll} className="p-2 text-slate-500 hover:text-red-400 transition-colors rounded-lg hover:bg-white/5">
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
            <textarea 
              value={inputText}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setInputText(e.target.value)}
              placeholder="Saisissez le texte à traduire via le réseau local..."
              className="w-full h-40 bg-transparent text-white p-6 outline-none resize-none custom-scrollbar font-light text-lg placeholder-slate-600"
            />
          </div>

          {/* Bouton d'Action */}
          <div className="flex justify-center -my-6 z-10">
            <button 
              onClick={handleTranslate}
              disabled={!inputText.trim() || isTranslating}
              className={`w-16 h-16 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                !inputText.trim() || isTranslating 
                ? 'bg-[#000b1e] border-2 border-white/10 text-slate-600' 
                : 'bg-gradient-to-br from-[#0047ff] to-[#00f2ff] text-white shadow-[0_0_30px_rgba(0,242,255,0.4)] hover:scale-110'
              }`}
            >
              {isTranslating ? <Loader2 size={24} className="animate-spin" /> : <ArrowRight size={24} />}
            </button>
          </div>

          {/* Bloc Résultat */}
          <div className="bg-black/40 border border-[#00f2ff]/30 rounded-[2rem] p-1 flex flex-col shadow-[0_0_30px_rgba(0,242,255,0.05)] relative overflow-hidden">
            {isTranslating && (
              <div className="absolute inset-0 bg-[#000b1e]/80 backdrop-blur-sm flex flex-col items-center justify-center z-20">
                <div className="relative w-16 h-16 mb-4">
                  <div className="absolute inset-0 border-4 border-t-[#00f2ff] border-r-transparent border-b-transparent border-l-transparent rounded-full animate-spin"></div>
                  <div className="absolute inset-2 border-4 border-l-[#0047ff] border-r-transparent border-b-transparent border-t-transparent rounded-full animate-[spin_1.5s_reverse_infinite]"></div>
                  <div className="absolute inset-0 flex items-center justify-center"><Sparkles size={20} className="text-[#00f2ff] animate-pulse" /></div>
                </div>
                <p className="text-xs font-black text-[#00f2ff] uppercase tracking-widest animate-pulse">Inférence locale en cours...</p>
              </div>
            )}

            <div className="flex justify-between items-center p-4 border-b border-[#00f2ff]/20 bg-[#00f2ff]/5 rounded-t-[2rem]">
              <span className="text-xs font-bold text-[#00f2ff] uppercase tracking-widest">
                {availableLangs.find(l => l.code === activeLang)?.n || 'Traduction'}
              </span>
              <div className="flex gap-2">
                {audioUrl && (
                  <button onClick={playAudio} className="p-2 text-[#00f2ff] hover:text-white transition-colors rounded-lg hover:bg-[#00f2ff]/20">
                    <Volume2 size={16} className={isPlaying ? "animate-pulse" : ""} />
                  </button>
                )}
                <button onClick={handleCopy} disabled={!outputText} className="p-2 text-[#00f2ff] hover:text-white transition-colors rounded-lg hover:bg-[#00f2ff]/20 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-[#00f2ff]">
                  {copied ? <Check size={16} /> : <Copy size={16} />}
                </button>
              </div>
            </div>
            <div className="w-full h-40 bg-transparent text-white p-6 overflow-y-auto custom-scrollbar font-light text-lg">
              {outputText ? (
                <p className="whitespace-pre-wrap">{outputText}</p>
              ) : (
                <p className="text-slate-600 italic">La traduction s'affichera ici...</p>
              )}
            </div>

            {/* Lecteur Audio invisible utilisé par la fonction playAudio */}
            {audioUrl && (
              <audio 
                ref={audioRef} 
                src={audioUrl} 
                onEnded={() => setIsPlaying(false)} 
                className="hidden" 
              />
            )}
          </div>

        </div>
      </div>
    </div>
  );
};