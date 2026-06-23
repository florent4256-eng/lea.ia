import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Activity, Mic, X, ShieldAlert, Music, Play, Pause, Download, ChevronRight, Loader2, Check, Edit3 } from 'lucide-react';

const SERVER = () => (window as any).LEA_SERVER_URL || '';

export const LiveModeNative: React.FC<{ onClose?: () => void }> = ({ onClose }) => {
  const currentUser = localStorage.getItem('lea_currentUser') || '';
  const WS_URL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`;

  const [isMicActive, setIsMicActive] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [systemStatus, setSystemStatus] = useState('INITIATION LIAISON...');

  // 📡 Références Réseau & Média
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const peerConnectionRef = useRef<RTCPeerConnection | null>(null);
  const wsRef = useRef<WebSocket | null>(null);

  // 🎙️ Références Audio Natives
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const silenceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isAudioPlayingRef = useRef(false);

  // 🔵 Référence Canvas (Anneau futuriste)
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const haloAnimFrame = useRef<number | null>(null);

  // ── 🎵 Machine d'états Musique Nova ────────────────────────────────────
  type MusicStep = 'idle' | 'title' | 'style' | 'lyrics' | 'generating' | 'playing';
  const [musicStep, setMusicStep] = useState<MusicStep>('idle');
  const [musicTitle, setMusicTitle] = useState('');
  const [musicStyle, setMusicStyle] = useState('');
  const [musicLyrics, setMusicLyrics] = useState('');
  const [musicUrl, setMusicUrl] = useState<string | null>(null);
  const [wizardInput, setWizardInput] = useState('');
  const [lyricsLoading, setLyricsLoading] = useState(false);
  const [genProgress, setGenProgress] = useState(0);
  const [genPhase, setGenPhase] = useState('');
  const pollRef = useRef<any>(null);

  // Lecteur popup musique
  const [playerPlaying, setPlayerPlaying] = useState(false);
  const [playerTime, setPlayerTime] = useState(0);
  const [playerDur, setPlayerDur] = useState(0);
  const playerRef = useRef<HTMLAudioElement>(null);

  const fmt = (s: number) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, '0')}`;

  // Arrête le micro pendant le wizard musique
  const stopMicForWizard = useCallback(() => {
    if (mediaRecorderRef.current?.state === 'recording') mediaRecorderRef.current.stop();
    if (audioContextRef.current) { audioContextRef.current.close(); audioContextRef.current = null; }
    if (haloAnimFrame.current) cancelAnimationFrame(haloAnimFrame.current);
    setIsMicActive(false);
  }, []);

  // Relance le micro après fermeture popup
  const restoreMic = useCallback(() => {
    setMusicStep('idle');
    setMusicUrl(null);
    setMusicTitle(''); setMusicStyle(''); setMusicLyrics('');
    setWizardInput('');
    setPlayerPlaying(false); setPlayerTime(0);
  }, []);

  // ── Étape 3 : génération paroles via IA ────────────────────────────────
  const generateLyrics = useCallback(async (title: string, style: string) => {
    setLyricsLoading(true);
    try {
      const prompt = `Écris des paroles de chanson de style "${style}" avec le titre "${title}". Utilise les marqueurs [verse], [chorus], [bridge]. Donne uniquement les paroles, sans introduction ni explication.`;
      const r = await fetch(`${SERVER()}/api/ai/quick`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: prompt, username: currentUser }),
      });
      if (r.ok) {
        const d = await r.json();
        setMusicLyrics(d.reply || d.text || '');
      }
    } catch {
      setMusicLyrics(`[verse]\nVers 1 de "${title}"\nDans un style ${style}\n\n[chorus]\nRefrain de "${title}"\nMélodie ${style}\n\n[verse]\nDeuxième couplet\nParoles ${style}`);
    }
    setLyricsLoading(false);
  }, [currentUser]);

  // ── Étape 4 : génération ACE-Step ──────────────────────────────────────
  const startGeneration = useCallback(async (title: string, style: string, lyrics: string) => {
    setMusicStep('generating');
    setGenProgress(0);
    setGenPhase('Envoi de la requête...');
    try {
      const r = await fetch(`${SERVER()}/api/studio/lyria/pro/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: style, lyrics, duration: 90, username: currentUser, title }),
      });
      if (!r.ok) throw new Error(`${r.status}`);
      const { job_id } = await r.json();
      if (!job_id) throw new Error('Pas de job_id');

      pollRef.current = setInterval(async () => {
        try {
          const st = await fetch(`${SERVER()}/api/studio/lyria/pro/status/${job_id}`).then(r => r.json());
          setGenProgress(st.progress || 0);
          setGenPhase(st.phase || '');
          if (st.status === 'done' && st.url) {
            clearInterval(pollRef.current);
            setMusicUrl(st.url);
            setMusicStep('playing');
            setPlayerTime(0); setPlayerPlaying(false);
          } else if (st.status === 'error' || st.status === 'cancelled') {
            clearInterval(pollRef.current);
            setMusicStep('lyrics');
          }
        } catch {}
      }, 3000);
    } catch {
      setMusicStep('lyrics');
    }
  }, [currentUser]);

  useEffect(() => () => { clearInterval(pollRef.current); }, []);

  // ── Validation de chaque étape ──────────────────────────────────────────
  const confirmStep = useCallback(() => {
    if (musicStep === 'title') {
      const t = wizardInput.trim();
      if (!t) return;
      setMusicTitle(t);
      setWizardInput('');
      setMusicStep('style');
    } else if (musicStep === 'style') {
      const s = wizardInput.trim();
      if (!s) return;
      setMusicStyle(s);
      setWizardInput('');
      setMusicStep('lyrics');
      generateLyrics(musicTitle, s);
    } else if (musicStep === 'lyrics') {
      startGeneration(musicTitle, musicStyle, musicLyrics);
    }
  }, [musicStep, wizardInput, musicTitle, musicStyle, musicLyrics, generateLyrics, startGeneration]);

  // ==========================================
  // 1. LE PONT NEURONAL WEBRTC & WEBSOCKET
  // ==========================================
  useEffect(() => {
    const DEVICE_ID = 's23_ultra';
    const ws = new WebSocket(WS_URL);
    wsRef.current = ws;

    const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
    peerConnectionRef.current = pc;

    pc.ontrack = (event) => {
      setSystemStatus('🟢 LIAISON VIDÉO STABLE');
      if (remoteVideoRef.current && event.streams && event.streams[0]) {
        remoteVideoRef.current.srcObject = event.streams[0];
      }
    };

    pc.onicecandidate = (event) => {
      if (event.candidate && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'webrtc_ice_candidate', candidate: event.candidate, targetId: 'flip6' }));
      }
    };

    let pingVideo: ReturnType<typeof setInterval> | null = null;

    ws.onopen = () => {
      setIsConnected(true);
      setSystemStatus('RECHERCHE DU FLUX 3D...');
      ws.send(JSON.stringify({ type: 'register_device', deviceId: DEVICE_ID }));
      pingVideo = setInterval(() => {
        if (remoteVideoRef.current && !remoteVideoRef.current.srcObject) {
          ws.send(JSON.stringify({ type: 'request_avatar_stream', requesterId: DEVICE_ID }));
        } else if (pingVideo) { clearInterval(pingVideo); }
      }, 3000);
    };

    let isRemoteDescriptionSet = false;
    let iceQueue: RTCIceCandidateInit[] = [];

    ws.onmessage = async (event) => {
      try {
        const data = JSON.parse(event.data);

        // 🔊 RÉCEPTION AUDIO DE LÉA
        if (data.type === 'CHAT' && data.message && data.message.audioData) {
          isAudioPlayingRef.current = true;
          if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
            mediaRecorderRef.current.stop();
          }
          const audio = new Audio(data.message.audioData);
          audio.onended = () => {
            isAudioPlayingRef.current = false;
            if (isMicActive && mediaRecorderRef.current && mediaRecorderRef.current.state === 'inactive') {
              mediaRecorderRef.current.start();
            }
          };
          audio.play().catch(() => { isAudioPlayingRef.current = false; });
        }

        // 🎵 RÉCEPTION MUSIQUE GÉNÉRÉE (via serveur)
        if (data.type === 'SYSTEM_ACTION' && data.action === 'show_music' && data.url) {
          clearInterval(pollRef.current);
          setMusicUrl(data.url);
          setMusicStep('playing');
          setPlayerTime(0); setPlayerPlaying(false);
          stopMicForWizard();
        }

        // 🎥 RÉCEPTION DU FLUX VIDÉO
        if (data.type === 'webrtc_offer' && (!data.targetId || data.targetId === DEVICE_ID)) {
          let sdpText = data.offer?.sdp || data.offer || data.sdp;
          if (!sdpText) return;
          await pc.setRemoteDescription({ type: 'offer', sdp: sdpText });
          isRemoteDescriptionSet = true;
          const answer = await pc.createAnswer();
          await pc.setLocalDescription(answer);
          ws.send(JSON.stringify({ type: 'webrtc_answer', answer, targetId: 'flip6' }));
          while (iceQueue.length > 0) { const c = iceQueue.shift(); if (c) await pc.addIceCandidate(c); }
        }

        if (data.type === 'webrtc_ice_candidate' && data.candidate) {
          if (isRemoteDescriptionSet) await pc.addIceCandidate(new RTCIceCandidate(data.candidate));
          else iceQueue.push(data.candidate);
        }
      } catch {}
    };

    return () => {
      if (pingVideo) clearInterval(pingVideo);
      pc.close();
      ws.close();
    };
  }, [isMicActive, stopMicForWizard]);

  // ==========================================
  // 2. MICROPHONE INTELLIGENT
  // ==========================================
  const startListening = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } });
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      audioContextRef.current = audioContext;
      const analyser = audioContext.createAnalyser();
      analyserRef.current = analyser;
      analyser.fftSize = 256;
      const source = audioContext.createMediaStreamSource(stream);
      source.connect(analyser);
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunksRef.current.push(e.data); };
      mediaRecorder.onstop = () => {
        if (audioChunksRef.current.length > 0 && wsRef.current?.readyState === WebSocket.OPEN) {
          const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
          const reader = new FileReader();
          reader.readAsDataURL(audioBlob);
          reader.onloadend = () => {
            wsRef.current?.send(JSON.stringify({ type: 'VOICE_CHAT', view: 'LiveModeNative', user: currentUser, sourceDevice: 'S23_Ultra', message: { audioData: reader.result, timestamp: new Date().toISOString() } }));
          };
        }
        audioChunksRef.current = [];
        if (isMicActive && !isAudioPlayingRef.current) mediaRecorder.start();
      };
      mediaRecorder.start();
      detectSilence(analyser, mediaRecorder);
      drawHalo();
    } catch {
      setSystemStatus('❌ ACCÈS MICRO REFUSÉ');
    }
  };

  const detectSilence = (analyser: AnalyserNode, mediaRecorder: MediaRecorder) => {
    if (!isMicActive) return;
    if (isAudioPlayingRef.current) { requestAnimationFrame(() => detectSilence(analyser, mediaRecorder)); return; }
    const dataArray = new Uint8Array(analyser.frequencyBinCount);
    analyser.getByteFrequencyData(dataArray);
    let sum = 0;
    for (let i = 0; i < dataArray.length; i++) sum += dataArray[i];
    if (sum / dataArray.length > 45) {
      if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);
      if (mediaRecorder.state === 'inactive') mediaRecorder.start();
      silenceTimerRef.current = setTimeout(() => { if (mediaRecorder.state === 'recording') mediaRecorder.stop(); }, 1500);
    }
    requestAnimationFrame(() => detectSilence(analyser, mediaRecorder));
  };

  const toggleMic = () => {
    if (musicStep !== 'idle') return;
    if (!isMicActive) { setIsMicActive(true); startListening(); }
    else {
      setIsMicActive(false);
      if (mediaRecorderRef.current?.state === 'recording') mediaRecorderRef.current.stop();
      if (audioContextRef.current) audioContextRef.current.close();
      if (haloAnimFrame.current) cancelAnimationFrame(haloAnimFrame.current);
    }
  };

  // ==========================================
  // 3. MOTEUR GRAPHIQUE CANVAS
  // ==========================================
  const drawHalo = () => {
    if (!canvasRef.current || !analyserRef.current || !isMicActive) return;
    const ctx = canvasRef.current.getContext('2d');
    if (!ctx) return;
    const W = canvasRef.current.width, H = canvasRef.current.height;
    const CX = W / 2, CY = H / 2, R = 150, N = 128;
    ctx.clearRect(0, 0, W, H);
    const dataArray = new Uint8Array(analyserRef.current.frequencyBinCount);
    analyserRef.current.getByteFrequencyData(dataArray);
    ctx.beginPath(); ctx.arc(CX, CY, R, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(0,242,255,0.1)'; ctx.lineWidth = 2; ctx.stroke();
    for (let i = 0; i < N; i++) {
      const angle = (i / N) * Math.PI * 2 - Math.PI / 2;
      const amp = dataArray[i] / 255;
      const barH = 5 + amp * 60;
      ctx.beginPath();
      ctx.moveTo(CX + Math.cos(angle) * R, CY + Math.sin(angle) * R);
      ctx.lineTo(CX + Math.cos(angle) * (R + barH), CY + Math.sin(angle) * (R + barH));
      ctx.strokeStyle = `rgba(0,242,255,${0.3 + amp * 0.7})`; ctx.lineWidth = 2; ctx.lineCap = 'round'; ctx.stroke();
    }
    haloAnimFrame.current = requestAnimationFrame(drawHalo);
  };

  // ── Labels des étapes ──────────────────────────────────────────────────
  const stepMeta: Record<string, { num: number; question: string; hint: string }> = {
    title:      { num: 1, question: 'Quel est le titre de ta musique ?', hint: 'Ex: "Nuit de Paris", "Mon Chemin"...' },
    style:      { num: 2, question: 'Quel style tu veux ?', hint: 'Ex: "Trap 140 BPM", "Pop douce R&B", "Rap français"...' },
    lyrics:     { num: 3, question: 'Paroles — modifie si tu veux', hint: '' },
    generating: { num: 4, question: 'Génération en cours…', hint: '' },
    playing:    { num: 4, question: 'Ta musique est prête !', hint: '' },
  };
  const meta = musicStep !== 'idle' ? stepMeta[musicStep] : null;

  return (
    <div className="fixed inset-0 bg-[#000814] overflow-hidden z-50 flex flex-col items-center justify-center font-sans">

      {/* 📡 HUD */}
      <div className="absolute top-6 left-6 z-20 pointer-events-none">
        <div className="text-[#00f2ff] text-xs font-bold tracking-widest drop-shadow-[0_0_5px_#00f2ff]">
          SYS.OP: {currentUser.toUpperCase()}<br />
          STATUS: <span className={isConnected ? 'text-[#00ff00]' : 'text-[#ff00aa]'}>{systemStatus}</span>
        </div>
      </div>

      <button onClick={() => onClose && onClose()} className="absolute top-6 right-6 z-50 p-3 rounded-full bg-red-950/40 border border-red-500/30 text-red-500 hover:bg-red-500/20 transition-all backdrop-blur-md">
        <X size={24} />
      </button>

      {/* 📺 FLUX 3D */}
      <div className="absolute inset-0 w-full h-full flex items-center justify-center bg-[radial-gradient(circle,rgba(0,242,255,0.05)_0%,rgba(0,0,0,0.8)_70%)]">
        <video ref={remoteVideoRef} autoPlay playsInline muted className="w-full h-full object-cover z-10" />
        <canvas ref={canvasRef} width={420} height={420}
          className={`absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-15 pointer-events-none transition-opacity duration-500 ${isMicActive && musicStep === 'idle' ? 'opacity-100' : 'opacity-0'}`} />
      </div>

      {/* ── 🎵 WIZARD MUSIQUE ──────────────────────────────────────────── */}
      {musicStep !== 'idle' && musicStep !== 'playing' && (
        <div className="absolute inset-0 z-30 bg-black/80 backdrop-blur-sm flex items-center justify-center p-6">
          <div className="w-full max-w-md bg-[#000b1e] border border-fuchsia-500/30 rounded-3xl p-6 shadow-[0_0_40px_rgba(217,70,239,0.2)]">

            {/* Indicateur étapes */}
            <div className="flex items-center gap-1.5 mb-5">
              {[1, 2, 3, 4].map(n => (
                <div key={n} className={`h-1 flex-1 rounded-full transition-all ${(meta?.num ?? 0) >= n ? 'bg-fuchsia-500' : 'bg-white/10'}`} />
              ))}
            </div>

            {/* Icône + question */}
            <div className="flex items-center gap-3 mb-5">
              <div className="w-10 h-10 bg-fuchsia-500/20 rounded-2xl flex items-center justify-center shrink-0">
                <Music size={18} className="text-fuchsia-400" />
              </div>
              <div>
                <p className="text-[9px] uppercase tracking-widest text-fuchsia-400/70 font-black">Étape {meta?.num}/4</p>
                <p className="text-sm font-bold text-white">{meta?.question}</p>
              </div>
            </div>

            {/* Étape 1 : titre */}
            {musicStep === 'title' && (
              <div className="space-y-3">
                <input autoFocus value={wizardInput} onChange={e => setWizardInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && confirmStep()}
                  placeholder={meta?.hint}
                  className="w-full bg-white/5 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white placeholder:text-slate-600 outline-none focus:border-fuchsia-500/50 transition-colors" />
                <button onClick={confirmStep} disabled={!wizardInput.trim()}
                  className="w-full py-3 rounded-2xl bg-gradient-to-r from-fuchsia-600 to-purple-700 text-white text-sm font-black uppercase tracking-widest flex items-center justify-center gap-2 disabled:opacity-40 transition-all hover:scale-[1.01] active:scale-95">
                  Suivant <ChevronRight size={16} />
                </button>
              </div>
            )}

            {/* Étape 2 : style */}
            {musicStep === 'style' && (
              <div className="space-y-3">
                <p className="text-[10px] text-slate-500">Titre : <span className="text-fuchsia-300 font-bold">"{musicTitle}"</span></p>
                <input autoFocus value={wizardInput} onChange={e => setWizardInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && confirmStep()}
                  placeholder={meta?.hint}
                  className="w-full bg-white/5 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white placeholder:text-slate-600 outline-none focus:border-fuchsia-500/50 transition-colors" />
                <button onClick={confirmStep} disabled={!wizardInput.trim()}
                  className="w-full py-3 rounded-2xl bg-gradient-to-r from-fuchsia-600 to-purple-700 text-white text-sm font-black uppercase tracking-widest flex items-center justify-center gap-2 disabled:opacity-40 transition-all hover:scale-[1.01] active:scale-95">
                  Suivant <ChevronRight size={16} />
                </button>
              </div>
            )}

            {/* Étape 3 : paroles */}
            {musicStep === 'lyrics' && (
              <div className="space-y-3">
                <div className="flex gap-2 text-[10px] text-slate-500 flex-wrap">
                  <span>Titre : <span className="text-fuchsia-300 font-bold">"{musicTitle}"</span></span>
                  <span>·</span>
                  <span>Style : <span className="text-fuchsia-300 font-bold">{musicStyle}</span></span>
                </div>
                {lyricsLoading ? (
                  <div className="flex items-center justify-center py-8 gap-3 text-fuchsia-400">
                    <Loader2 size={20} className="animate-spin" />
                    <span className="text-sm font-bold">Léa compose les paroles…</span>
                  </div>
                ) : (
                  <textarea value={musicLyrics} onChange={e => setMusicLyrics(e.target.value)} rows={8}
                    className="w-full bg-white/5 border border-white/10 rounded-2xl px-3 py-2.5 text-[11px] text-slate-200 outline-none focus:border-fuchsia-500/40 transition-colors resize-none font-mono leading-relaxed" />
                )}
                <button onClick={confirmStep} disabled={lyricsLoading || !musicLyrics.trim()}
                  className="w-full py-3 rounded-2xl bg-gradient-to-r from-fuchsia-600 to-purple-700 text-white text-sm font-black uppercase tracking-widest flex items-center justify-center gap-2 disabled:opacity-40 transition-all hover:scale-[1.01] active:scale-95">
                  <Music size={16} /> Générer la musique
                </button>
              </div>
            )}

            {/* Étape 4 : génération */}
            {musicStep === 'generating' && (
              <div className="space-y-4">
                <div className="flex gap-2 text-[10px] text-slate-500 flex-wrap">
                  <span>Titre : <span className="text-fuchsia-300 font-bold">"{musicTitle}"</span></span>
                  <span>·</span>
                  <span>Style : <span className="text-fuchsia-300 font-bold">{musicStyle}</span></span>
                </div>
                <div className="space-y-2">
                  <div className="flex justify-between text-[11px]">
                    <span className="text-fuchsia-300">{genPhase || 'Génération ACE-Step…'}</span>
                    <span className="font-mono text-fuchsia-400">{genProgress}%</span>
                  </div>
                  <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-fuchsia-500 to-purple-500 rounded-full transition-all duration-700" style={{ width: `${genProgress}%` }} />
                  </div>
                </div>
                <p className="text-[10px] text-slate-500 text-center">Patiente, Léa compose ton morceau…</p>
              </div>
            )}

            {/* Annuler */}
            {musicStep !== 'generating' && (
              <button onClick={restoreMic} className="w-full mt-3 py-2 text-[10px] text-slate-500 hover:text-red-400 transition-colors">
                Annuler
              </button>
            )}
          </div>
        </div>
      )}

      {/* ── 🎵 POPUP LECTEUR MUSIQUE ───────────────────────────────────── */}
      {musicStep === 'playing' && musicUrl && (
        <div className="absolute inset-0 z-40 bg-black/90 backdrop-blur-md flex items-center justify-center p-6">
          <div className="w-full max-w-sm bg-[#000b1e] border border-fuchsia-500/40 rounded-3xl p-6 shadow-[0_0_60px_rgba(217,70,239,0.3)]">
            <audio ref={playerRef} src={`${SERVER()}${musicUrl}`}
              onTimeUpdate={() => setPlayerTime(playerRef.current?.currentTime || 0)}
              onLoadedMetadata={() => setPlayerDur(playerRef.current?.duration || 0)}
              onEnded={() => setPlayerPlaying(false)} />

            {/* Header */}
            <div className="flex items-center justify-between mb-5">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-gradient-to-br from-fuchsia-500 to-purple-700 rounded-2xl flex items-center justify-center shadow-[0_0_20px_rgba(217,70,239,0.4)]">
                  <Music size={22} className="text-white" />
                </div>
                <div>
                  <p className="text-sm font-black text-white">{musicTitle || 'Ma musique'}</p>
                  <p className="text-[10px] text-fuchsia-400/70 uppercase tracking-widest">{musicStyle}</p>
                </div>
              </div>
              <div className="w-8 h-8 bg-green-500/20 border border-green-500/40 rounded-full flex items-center justify-center">
                <Check size={14} className="text-green-400" />
              </div>
            </div>

            {/* Progress */}
            <div className="space-y-2 mb-5">
              <div className="h-2 bg-white/10 rounded-full overflow-hidden cursor-pointer" onClick={e => {
                const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
                if (playerRef.current) playerRef.current.currentTime = ((e.clientX - rect.left) / rect.width) * playerDur;
              }}>
                <div className="h-full bg-gradient-to-r from-fuchsia-500 to-purple-500 rounded-full transition-all" style={{ width: playerDur ? `${(playerTime / playerDur) * 100}%` : '0%' }} />
              </div>
              <div className="flex justify-between text-[10px] font-mono text-slate-500">
                <span>{fmt(playerTime)}</span>
                <span>{fmt(playerDur)}</span>
              </div>
            </div>

            {/* Contrôles */}
            <div className="flex items-center gap-3 mb-5">
              <button onClick={() => {
                if (!playerRef.current) return;
                if (playerPlaying) { playerRef.current.pause(); setPlayerPlaying(false); }
                else { playerRef.current.play(); setPlayerPlaying(true); }
              }} className="flex-1 py-3 bg-fuchsia-500 hover:bg-fuchsia-400 text-white rounded-2xl flex items-center justify-center gap-2 font-black text-sm transition-all shadow-[0_0_20px_rgba(217,70,239,0.4)]">
                {playerPlaying ? <Pause size={18} /> : <Play size={18} />}
                {playerPlaying ? 'Pause' : 'Écouter'}
              </button>
              <a href={`${SERVER()}${musicUrl}`}
                download={`${musicTitle || 'musique'}.wav`}
                className="w-12 h-12 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl flex items-center justify-center transition-all">
                <Download size={18} className="text-slate-400" />
              </a>
            </div>

            {/* Fermer → rend le micro */}
            <button onClick={restoreMic}
              className="w-full py-3 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 text-sm font-bold text-slate-300 flex items-center justify-center gap-2 transition-all">
              <Mic size={15} className="text-[#00f2ff]" /> Fermer et reprendre la conversation
            </button>
          </div>
        </div>
      )}

      {/* 🎙️ CONTRÔLES PRINCIPAUX */}
      <div className="absolute bottom-10 left-0 w-full z-20 flex justify-center gap-6">
        {/* Bouton Mode Musique */}
        <button
          onClick={() => { if (musicStep === 'idle') { stopMicForWizard(); setMusicStep('title'); setWizardInput(''); } }}
          className={`w-14 h-14 rounded-full flex items-center justify-center transition-all duration-300 border-2 shadow-xl
            ${musicStep !== 'idle' ? 'bg-fuchsia-500/20 border-fuchsia-500 shadow-[0_0_20px_rgba(217,70,239,0.5)]' : 'bg-[#000b1e] border-fuchsia-900/50 hover:border-fuchsia-500/60'}`}>
          <Music size={22} className={musicStep !== 'idle' ? 'text-fuchsia-400' : 'text-slate-500'} />
        </button>

        {/* Bouton Micro principal */}
        <button
          onClick={toggleMic}
          disabled={musicStep !== 'idle'}
          className={`group relative w-20 h-20 rounded-full flex items-center justify-center transition-all duration-500 border-2 shadow-2xl
            ${isMicActive ? 'bg-[#000b1e] border-[#00f2ff] shadow-[0_0_30px_rgba(0,242,255,0.6)] scale-110' : 'bg-[#000b1e] border-indigo-900/50 hover:border-[#00f2ff]'}
            ${musicStep !== 'idle' ? 'opacity-30 cursor-not-allowed' : ''}`}>
          {isMicActive && <div className="absolute inset-0 rounded-full animate-ping bg-[#00f2ff]/20" />}
          <Mic size={32} className={isMicActive ? 'text-[#00f2ff]' : 'text-slate-500'} />
        </button>
      </div>
    </div>
  );
};
