import React, { useState, useEffect, useRef } from 'react';
import { 
  Radio, Tv, UserCircle, ScanFace, Video, Mic, 
  MessageCircle, Settings, Play, Square, Eye, 
  Focus, Cpu, Activity, Maximize, Scan, Wand2,
  Lightbulb, Power, MonitorPlay, Fingerprint, Lock, Loader2
} from 'lucide-react';
import { useConfirmToast } from '../../hooks/useConfirmToast';

export const LiveMode = () => {
  const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();
  const currentUser = localStorage.getItem('lea_currentUser') || '';
  const [socket, setSocket] = useState<WebSocket | null>(null);

  // --- ÉTATS GLOBAUX ---
  const [activeTab, setActiveTab] = useState<'vision' | 'avatar' | 'studio'>('vision');
  // --- ÉTATS CRÉATEUR AVATAR ---
  const [selectedStyle, setSelectedStyle] = useState('Cyberpunk');
  const [selectedVoice, setSelectedVoice] = useState('fr_FR-siwis-low.onnx');
  const [isCompiling, setIsCompiling] = useState(false);
 const [avatarData, setAvatarData] = useState<{imageUrl: string, audioUrl: string, videoUrl?: string | null} | null>(null);

  // --- FONCTION DE COMPILATION ---
  const handleCompileAvatar = async () => {
    setIsCompiling(true);
    try {
      const response = await fetch('/api/studio/avatar/compile', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: currentUser,
          style: selectedStyle,
          voice: selectedVoice
        })
      });
      const data = await response.json();
      
      if (data.success) {
        setAvatarData({ 
          imageUrl: data.imageUrl, 
          audioUrl: data.audioUrl,
          videoUrl: data.videoUrl 
        });
        
        // On ne lance l'audio séparé que si la vidéo n'est pas là
        if (data.audioUrl && !data.videoUrl) {
          const audio = new Audio(data.audioUrl);
          audio.play();
        }
      } else {
        showToast("Erreur de compilation : " + data.error);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setIsCompiling(false);
    }
  };

  // --- ÉTATS LÉA TV VISION & DOMOTIQUE ---
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [homeConnectSync, setHomeConnectSync] = useState(false);
  const [visionLogs, setVisionLogs] = useState<string[]>([]);
  const videoRef = useRef<HTMLVideoElement>(null);

  // --- CONNEXION WEBSOCKET (Serveur Central Linux) ---
  useEffect(() => {
    const wsUrl = `ws://${window.location.hostname}:3001`;
    const ws = new WebSocket(wsUrl);
    ws.onopen = () => setSocket(ws);
    
    // Écoute des retours d'analyse IA en direct
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'VISION_ANALYSIS' && activeTab === 'vision') {
        setVisionLogs(prev => [data.log, ...prev].slice(0, 8)); // Garde les 8 derniers logs
      }
    };
    return () => ws.close();
  }, [activeTab]);

  // --- LOGIQUE LÉA TV VISION ---
  const toggleCamera = async () => {
    if (!isCameraActive) {
      try {
        // Demande l'accès réel à la webcam du Zorin OS
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
        setIsCameraActive(true);
        setVisionLogs(["[SYS] Flux optique initialisé. Connecté au moteur IA local."]);
        
        // Simulation d'envoi de la frame au serveur pour analyse
        if (socket && socket.readyState === WebSocket.OPEN) {
          socket.send(JSON.stringify({ type: 'START_VISION_STREAM', user: currentUser }));
        }
      } catch (err) {
        setVisionLogs(["[ERREUR] Accès caméra refusé ou périphérique non détecté."]);
      }
    } else {
      // Coupe le flux
      const stream = videoRef.current?.srcObject as MediaStream;
      stream?.getTracks().forEach(track => track.stop());
      if (videoRef.current) videoRef.current.srcObject = null;
      setIsCameraActive(false);
      
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: 'STOP_VISION_STREAM', user: currentUser }));
      }
    }
  };

  const toggleHomeConnect = () => {
    const newState = !homeConnectSync;
    setHomeConnectSync(newState);
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ 
        type: 'HOME_CONNECT_CMD', 
        action: newState ? 'CINEMA_MODE_ON' : 'CINEMA_MODE_OFF' 
      }));
    }
    setVisionLogs(prev => [
      `[DOMOTIQUE] Ambiance Cinéma ${newState ? 'ACTIVÉE (Lumières off)' : 'DÉSACTIVÉE'}`, ...prev
    ]);
  };

  // =========================================================
  // FONCTION DE RENDU : LÉA TV VISION
  // =========================================================
  const renderVision = () => (
    <div className="flex flex-col lg:flex-row h-full gap-6 animate-in fade-in duration-300">
      
      {/* Lecteur Principal (Film ou Webcam) */}
      <div className="flex-1 bg-black/80 border border-white/10 rounded-[2rem] overflow-hidden flex flex-col relative shadow-[0_0_30px_rgba(0,242,255,0.05)]">
        
        {/* En-tête Vidéo */}
        <div className="absolute top-0 w-full p-6 flex justify-between items-center z-10 bg-gradient-to-b from-black/80 to-transparent">
          <div className="flex items-center gap-3">
            <div className="w-3 h-3 rounded-full bg-[#00f2ff] shadow-[0_0_10px_#00f2ff] animate-pulse" />
            <span className="text-xs font-black text-white uppercase tracking-widest">Flux Principal</span>
          </div>
          <button className="p-2 bg-black/50 hover:bg-white/10 text-white rounded-lg transition-all backdrop-blur-md">
            <Maximize size={16} />
          </button>
        </div>

        {/* Zone Vidéo Réelle */}
        <div className="flex-1 relative flex items-center justify-center bg-[#00050d]">
          <video 
            ref={videoRef}
            autoPlay 
            playsInline 
            muted 
            className={`w-full h-full object-cover ${isCameraActive ? 'opacity-100' : 'opacity-0'} transition-opacity duration-500`}
          />
          
          {/* Overlay si inactif */}
          {!isCameraActive && (
            <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-500">
              <MonitorPlay size={64} className="mb-6 opacity-30" />
              <p className="text-sm font-black uppercase tracking-[0.2em]">Aucun signal optique</p>
              <p className="text-xs mt-2 opacity-50">En attente de connexion caméra ou fichier vidéo...</p>
            </div>
          )}

          {/* Calque de ciblage IA (Simulation visuelle de Bounding Boxes) */}
          {isCameraActive && (
            <div className="absolute inset-0 pointer-events-none border-[4px] border-[#00f2ff]/20 m-8 rounded-3xl flex items-center justify-center">
              <ScanFace size={120} className="text-[#00f2ff]/30 animate-pulse" />
              <div className="absolute top-4 left-4 border-t-2 border-l-2 border-[#00f2ff] w-12 h-12" />
              <div className="absolute top-4 right-4 border-t-2 border-r-2 border-[#00f2ff] w-12 h-12" />
              <div className="absolute bottom-4 left-4 border-b-2 border-l-2 border-[#00f2ff] w-12 h-12" />
              <div className="absolute bottom-4 right-4 border-b-2 border-r-2 border-[#00f2ff] w-12 h-12" />
            </div>
          )}
        </div>

        {/* Panneau de Contrôle Inférieur */}
        <div className="p-6 bg-[#000b1e]/90 border-t border-white/10 backdrop-blur-xl flex flex-wrap items-center justify-between gap-4">
          <div className="flex gap-4">
            <button 
              onClick={toggleCamera}
              className={`px-6 py-3 rounded-xl font-black uppercase tracking-widest text-xs flex items-center gap-2 transition-all shadow-lg ${
                isCameraActive 
                ? 'bg-red-500/20 text-red-400 border border-red-500/50 hover:bg-red-500/30' 
                : 'bg-[#00f2ff] text-black hover:bg-white shadow-[0_0_20px_rgba(0,242,255,0.4)]'
              }`}
            >
              {isCameraActive ? <Square size={14} fill="currentColor" /> : <Play size={14} fill="currentColor" />}
              {isCameraActive ? 'Couper Flux' : 'Initialiser'}
            </button>
            <button className="px-6 py-3 bg-white/5 hover:bg-white/10 text-white border border-white/10 rounded-xl font-black uppercase tracking-widest text-xs transition-all flex items-center gap-2">
              <MonitorPlay size={14} /> Charger Film
            </button>
          </div>

          {/* Le bouton Magique : Home Connect */}
          <button 
            onClick={toggleHomeConnect}
            className={`px-6 py-3 rounded-xl font-black uppercase tracking-widest text-xs flex items-center gap-2 transition-all ${
              homeConnectSync 
              ? 'bg-indigo-500 text-white shadow-[0_0_20px_rgba(79,70,229,0.5)] border border-indigo-400' 
              : 'bg-black/50 text-slate-400 border border-white/10 hover:border-indigo-500/50'
            }`}
          >
            <Lightbulb size={14} className={homeConnectSync ? 'text-yellow-300' : ''} />
            Home Connect : Ambiance
          </button>
        </div>
      </div>

      {/* Panneau d'Analyse IA Côté Droit */}
      <div className="w-full lg:w-80 bg-[#000b1e] border border-white/10 rounded-[2rem] p-6 flex flex-col shadow-2xl shrink-0">
        <h3 className="flex items-center gap-3 text-[#00f2ff] font-black uppercase tracking-widest text-sm mb-6 pb-4 border-b border-white/10">
          <Cpu size={18} /> Moteur d'Analyse
        </h3>

        <div className="space-y-4 mb-6">
          <div className="bg-black/50 p-4 rounded-xl border border-white/5">
            <p className="text-[10px] text-slate-500 uppercase font-bold tracking-widest mb-1 flex items-center gap-2">
              <Fingerprint size={12} /> Identification
            </p>
            <p className="text-white font-bold text-sm">{isCameraActive ? `${currentUser} (Validé)` : 'En attente...'}</p>
          </div>
          <div className="bg-black/50 p-4 rounded-xl border border-white/5">
            <p className="text-[10px] text-slate-500 uppercase font-bold tracking-widest mb-1 flex items-center gap-2">
              <Activity size={12} /> Charge GPU (Local)
            </p>
            <div className="w-full h-2 bg-white/10 rounded-full mt-2 overflow-hidden">
              <div className={`h-full ${isCameraActive ? 'bg-[#00f2ff] w-3/4 animate-pulse' : 'bg-slate-600 w-0'} transition-all duration-1000`} />
            </div>
          </div>
        </div>

        <div className="flex-1 bg-black/80 rounded-xl border border-white/5 p-4 flex flex-col">
          <p className="text-[10px] text-[#00f2ff] font-black uppercase tracking-widest mb-3 border-b border-[#00f2ff]/20 pb-2">
            Terminal Vision
          </p>
          <div className="flex-1 overflow-y-auto custom-scrollbar font-mono text-[10px] space-y-2 pr-1">
            {visionLogs.map((log, index) => (
              <div key={index} className="text-slate-300 break-words leading-relaxed">
                <span className="text-[#00f2ff] opacity-50 mr-2">{'>'}</span> {log}
              </div>
            ))}
            {!isCameraActive && visionLogs.length === 0 && (
               <div className="text-slate-600 italic">En attente d'initialisation du flux optique...</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="w-full h-full pt-20 px-6 md:px-10 pb-10 flex flex-col font-sans">
      <ConfirmToastHost />

      {/* HEADER & NAVIGATION */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <div className="p-3 bg-[#00e5ff]/20 rounded-xl border border-[#00e5ff]/30 shadow-[0_0_15px_rgba(0,229,255,0.2)]">
              <Radio size={24} className="text-[#00e5ff]" />
            </div>
            <h2 className="text-3xl font-black text-white tracking-tighter uppercase">Live Mode</h2>
          </div>
          <p className="text-slate-400 text-sm tracking-wide flex items-center gap-2">
            <Lock size={14} className="text-[#00e5ff]" /> Protocole temps réel souverain.
          </p>
        </div>

        <div className="flex bg-[#000b1e]/80 border border-white/10 p-2 rounded-2xl backdrop-blur-xl shadow-xl w-full md:w-auto">
          <button 
            onClick={() => setActiveTab('vision')} 
            className={`flex-1 md:flex-none px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 justify-center ${activeTab === 'vision' ? 'bg-[#00f2ff] text-black shadow-[0_0_15px_rgba(0,242,255,0.4)]' : 'text-slate-500 hover:text-white'}`}
          >
            <Tv size={14} /> Léa TV Vision
          </button>
          <button 
            onClick={() => setActiveTab('studio')} 
            className={`flex-1 md:flex-none px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 justify-center ${activeTab === 'studio' ? 'bg-pink-500 text-white shadow-[0_0_15px_rgba(236,72,153,0.4)]' : 'text-slate-500 hover:text-white'}`}
          >
            <Video size={14} /> Studio Live
          </button>
          <button 
            onClick={() => setActiveTab('avatar')} 
            className={`flex-1 md:flex-none px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 justify-center ${activeTab === 'avatar' ? 'bg-indigo-500 text-white shadow-[0_0_15px_rgba(79,70,229,0.4)]' : 'text-slate-500 hover:text-white'}`}
          >
            <UserCircle size={14} /> Créateur Avatar
          </button>
        </div>
      </div>

      {/* ZONE D'AFFICHAGE DYNAMIQUE (La balise fermante du fichier arrivera dans le Bloc 2) */}
      <div className="flex-1 overflow-hidden relative">
        {activeTab === 'vision' && renderVision()}
        {/* ========================================================= */}
        {/* ONGLET 2 : STUDIO LIVE (Discussion & Streaming)             */}
        {/* ========================================================= */}
        {activeTab === 'studio' && (
          <div className="flex flex-col lg:flex-row h-full gap-6 animate-in fade-in duration-300">
             
             {/* Zone de Diffusion Principale */}
             <div className="flex-1 bg-black/60 border border-white/10 rounded-[2rem] flex flex-col overflow-hidden relative shadow-xl">
                
                {/* Header du Studio */}
                <div className="p-4 md:p-6 border-b border-white/10 bg-white/5 flex justify-between items-center backdrop-blur-md">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 rounded-full bg-pink-500 animate-pulse shadow-[0_0_10px_#ec4899]" />
                    <span className="text-xs font-black text-white uppercase tracking-widest">En Direct : Studio d'Échange</span>
                  </div>
                  <div className="flex gap-3">
                    <button className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded-xl text-[10px] font-bold text-white uppercase transition-colors border border-white/5">
                      Inviter des membres
                    </button>
                    <button className="px-4 py-2 bg-pink-500/20 hover:bg-pink-500/30 text-pink-400 border border-pink-500/50 rounded-xl text-[10px] font-bold uppercase transition-colors">
                      Démarrer l'enregistrement
                    </button>
                  </div>
                </div>

                {/* Flux Vidéo / Avatar Numérique */}
                <div className="flex-1 bg-gradient-to-b from-[#00050d] to-black relative flex items-center justify-center overflow-hidden">
                  <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-pink-500/5 via-transparent to-transparent"></div>
                  
                  {/* Avatar Placeholder */}
                  <div className="flex flex-col items-center">
                    <UserCircle size={120} className="text-pink-500/20 mb-6 drop-shadow-2xl" />
                    <p className="text-xs font-bold text-slate-500 uppercase tracking-widest border border-white/10 px-4 py-2 rounded-full bg-black/50">
                      Flux Avatar Hors Ligne
                    </p>
                  </div>
                </div>

                {/* Panneau de contrôle des outils */}
                <div className="p-4 bg-[#000b1e] border-t border-white/10 flex justify-center gap-4">
                  <button className="p-4 bg-white/5 hover:bg-white/10 rounded-xl text-slate-400 hover:text-white transition-colors"><Mic size={20} /></button>
                  <button className="p-4 bg-white/5 hover:bg-white/10 rounded-xl text-slate-400 hover:text-white transition-colors"><Video size={20} /></button>
                  <button className="p-4 bg-white/5 hover:bg-white/10 rounded-xl text-slate-400 hover:text-white transition-colors"><MonitorPlay size={20} /></button>
                </div>
             </div>
             
             {/* Colonne Chat & Modération */}
             <div className="w-full lg:w-96 bg-[#000b1e]/90 border border-white/10 rounded-[2rem] flex flex-col shrink-0 shadow-2xl">
               <div className="p-6 border-b border-white/10 flex items-center justify-between bg-gradient-to-r from-transparent to-white/[0.02]">
                 <h3 className="text-xs font-black text-pink-500 uppercase tracking-widest flex items-center gap-2">
                   <MessageCircle size={16}/> Chat & Événements
                 </h3>
                 <span className="text-[10px] bg-pink-500/10 text-pink-400 px-2 py-1 rounded-md border border-pink-500/20 font-bold">Modération IA Actives</span>
               </div>
               
               {/* Flux de messages */}
               <div className="flex-1 p-6 overflow-y-auto custom-scrollbar space-y-4 font-sans">
                 <div className="bg-pink-500/10 border border-pink-500/20 p-4 rounded-2xl relative overflow-hidden">
                   <div className="absolute left-0 top-0 bottom-0 w-1 bg-pink-500"></div>
                   <p className="text-[10px] font-black uppercase text-pink-400 mb-1 flex items-center gap-2">
                     <Wand2 size={12} /> Léa Agent (Modératrice)
                   </p>
                   <p className="text-xs text-slate-200 leading-relaxed">Bienvenue dans le salon live. Le cryptage de bout en bout est assuré par Léa Protect. Je surveille les échanges.</p>
                 </div>
               </div>
               
               {/* Input Chat */}
               <div className="p-4 border-t border-white/10 bg-black/40 rounded-b-[2rem]">
                 <div className="bg-[#00050d] border border-white/10 focus-within:border-pink-500/50 transition-colors rounded-xl flex items-center p-2">
                   <input type="text" placeholder="Interagir avec le studio..." className="flex-1 bg-transparent outline-none text-xs text-white px-3" />
                   <button className="p-3 bg-pink-600 hover:bg-pink-500 rounded-lg text-white transition-all shadow-[0_0_15px_rgba(219,39,119,0.4)]">
                     <Play size={14} fill="currentColor" />
                   </button>
                 </div>
               </div>
             </div>
          </div>
        )}

        {/* ========================================================= */}
        {/* ONGLET 3 : CRÉATEUR D'AVATAR (Modélisation Numérique)       */}
        {/* ========================================================= */}
        {activeTab === 'avatar' && (
           <div className="flex flex-col lg:flex-row h-full gap-6 animate-in fade-in duration-300">
             
             {/* Visualiseur 3D */}
             <div className="flex-1 bg-gradient-to-br from-[#000b1e] to-black border border-white/10 rounded-[2rem] flex flex-col items-center justify-center relative overflow-hidden shadow-2xl">
               <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-10 mix-blend-overlay"></div>
               <div className="relative w-full h-full flex flex-col items-center justify-center">
                 <div className="absolute inset-0 bg-indigo-500 blur-[100px] opacity-20 rounded-full"></div>
                 
                 {/* Affichage de l'Avatar (Vidéo animée ou Image fixe) */}
                 {avatarData?.videoUrl ? (
                   <video 
                     src={avatarData.videoUrl} 
                     autoPlay 
                     className="w-80 h-80 object-cover rounded-3xl shadow-[0_0_40px_rgba(79,70,229,0.5)] z-10 border-2 border-indigo-500/50" 
                     onEnded={() => console.log("Fin de l'animation.")}
                   />
                 ) : avatarData?.imageUrl ? (
                   <img 
                     src={avatarData.imageUrl} 
                     alt="Avatar" 
                     className="w-80 h-80 object-cover rounded-3xl shadow-[0_0_40px_rgba(79,70,229,0.5)] z-10 border-2 border-indigo-500/50" 
                   />
                 ) : (
                   <UserCircle size={180} className="text-indigo-500/60 mb-8 relative z-10" strokeWidth={1} />
                 )}
                 
               </div>
               <h3 className="text-2xl font-black text-white uppercase tracking-[0.2em] relative z-10 mt-6">Avatar Numérique</h3>
               <p className="text-xs font-bold uppercase tracking-widest text-indigo-400 mt-3 relative z-10 bg-indigo-500/10 px-4 py-2 rounded-full border border-indigo-500/20">
                 {isCompiling ? "Génération par la Matrice en cours..." : (avatarData ? "Entité Opérationnelle" : "En attente de compilation...")}
               </p>
             </div>

             {/* Panneau de Configuration */}
             <div className="w-full lg:w-96 bg-[#000b1e]/90 border border-white/10 rounded-[2rem] p-8 flex flex-col shrink-0 overflow-y-auto custom-scrollbar shadow-xl">
                <h3 className="text-xs font-black text-indigo-400 uppercase tracking-widest flex items-center gap-2 mb-8 border-b border-white/10 pb-4">
                  <Settings size={16}/> Paramètres de Synthèse
                </h3>
                
                <div className="space-y-8">
                  <div>
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-3 block">Identité Visuelle</label>
                    <div className="grid grid-cols-2 gap-3">
                      {['Cyberpunk', 'Pro Réaliste', 'Hologramme', 'Custom IA'].map(style => (
                        <button 
                          key={style} 
                          onClick={() => setSelectedStyle(style)}
                          className={`p-3 border rounded-xl text-xs transition-colors text-center ${selectedStyle === style ? 'bg-indigo-500/20 border-indigo-500 text-indigo-300 shadow-[0_0_15px_rgba(79,70,229,0.3)]' : 'bg-black/50 border-white/10 hover:border-indigo-500/50 text-slate-300'}`}
                        >
                          {style}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-3 block">Moteur Vocal (TTS)</label>
                    <select 
                      value={selectedVoice}
                      onChange={(e) => setSelectedVoice(e.target.value)}
                      className="w-full bg-black/50 border border-white/10 rounded-xl p-4 text-xs font-bold text-white outline-none focus:border-indigo-500/50 appearance-none"
                    >
                      <option value="fr_FR-siwis-low.onnx">Voix Léa (Neuronale Officielle)</option>
                      <option value="fr_FR-gilles-low.onnx">Voix Léo (Masculine)</option>
                      <option value="fr_FR-upmc-medium.onnx">Voix Léa (Éloquente)</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-3 block">Animation Faciale</label>
                    <div className="flex items-center justify-between p-4 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
                       <span className="text-xs font-bold text-indigo-300">Lip-Sync (Phase Suivante)</span>
                       <div className="w-10 h-5 bg-indigo-500/30 rounded-full relative">
                         <div className="absolute left-1 top-1 bottom-1 w-3 bg-white/50 rounded-full"></div>
                       </div>
                    </div>
                  </div>

                  <button 
                    onClick={handleCompileAvatar}
                    disabled={isCompiling}
                    className={`w-full py-5 rounded-xl font-black uppercase tracking-widest text-xs transition-all mt-4 ${isCompiling ? 'bg-indigo-900 text-indigo-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-[0_0_30px_rgba(79,70,229,0.3)]'}`}
                  >
                    {isCompiling ? <Loader2 size={16} className="animate-spin inline-block mr-2" /> : null}
                    {isCompiling ? "Forge en cours..." : "Compiler l'Entité"}
                  </button>
                </div>
             </div>
           </div>
        )}
      </div>
    </div>
  );
};