import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Film, Upload, Sparkles, Download, Clock, Zap, Star,
  RefreshCw, Image as ImageIcon, Type, Layers, Check,
  X, Loader2, Play, Gauge, Coins, Crown, Link2, Trash2
} from 'lucide-react';

const SERVER = () => (window as any).LEA_SERVER_URL || '';
const currentUser = () => localStorage.getItem('lea_currentUser') || 'invité';

type Mode = 'text' | 'image';
type Quality = 'rapide' | 'normal' | 'qualite';

interface HistoryItem {
  filename: string;
  url: string;
  createdAt: number;
}

const BG_GRID = `url("data:image/svg+xml,%3Csvg width='40' height='40' viewBox='0 0 40 40' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23a855f7' fill-opacity='0.03'%3E%3Cpath d='M0 0h40v40H0z'/%3E%3Cpath d='M0 0h1v40H0zM39 0h1v40h-1zM0 0h40v1H0zM0 39h40v1H0z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`;

const QUALITY_MAP: Record<Quality, { label: string; steps: number; minPerClip: number; icon: React.ReactNode; color: string }> = {
  rapide:  { label: 'Rapide',  steps: 15, minPerClip: 1.5, icon: <Zap   size={11} />, color: 'text-yellow-400' },
  normal:  { label: 'Normal',  steps: 25, minPerClip: 3,   icon: <Gauge  size={11} />, color: 'text-blue-400'   },
  qualite: { label: 'Qualité', steps: 30, minPerClip: 4,   icon: <Star   size={11} />, color: 'text-fuchsia-400' },
};

// Motion fixe — valeur calibrée pour un mouvement naturel/réaliste (SVD XT motion bucket)
const MOTION_NATURAL = 127;

// Durées image→vidéo (4 options épurées)
const DURATIONS_IMAGE = [
  { label: '4s',  value: 4  },
  { label: '10s', value: 10 },
  { label: '20s', value: 20 },
  { label: '60s', value: 60 },
];
// Durées texte→vidéo (6 options — FLUX+SVD justifie les grandes durées)
const DURATIONS_TEXT = [
  { label: '4s',   value: 4   },
  { label: '10s',  value: 10  },
  { label: '20s',  value: 20  },
  { label: '30s',  value: 30  },
  { label: '60s',  value: 60  },
  { label: '120s', value: 120 },
];

function calcClips(totalSec: number, fps: number) {
  const secondsPerClip = 25 / fps;
  return Math.max(1, Math.ceil(totalSec / secondsPerClip));
}

export function StudioVeo() {
  const [mode, setMode]                   = useState<Mode>('image');
  const [prompt, setPrompt]               = useState('');
  const [imageFile, setImageFile]         = useState<File | null>(null);
  const [imagePreview, setImagePreview]   = useState<string | null>(null);
  const [imagePayload, setImagePayload]   = useState<{ type: 'base64' | 'url'; data: string } | null>(null);
  const [totalSeconds, setTotalSeconds]   = useState(4);
  const [fps, setFps]                     = useState(6);
  const [quality, setQuality]             = useState<Quality>('normal');
  const [isGenerating, setIsGenerating]   = useState(false);
  const [generatedVideo, setGeneratedVideo] = useState<string | null>(null);
  const [history, setHistory]             = useState<HistoryItem[]>([]);
  const [showHistory, setShowHistory]     = useState(false);
  const [error, setError]                 = useState<string | null>(null);
  const [downloadedId, setDownloadedId]   = useState<string | null>(null);
  const [deletingId, setDeletingId]       = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [pollCount, setPollCount]         = useState(0);
  const [userTokens, setUserTokens]       = useState<number | null>(null);
  const [isAdmin, setIsAdmin]             = useState(false);
  const [veoProgress, setVeoProgress]     = useState<{ clipDone: number; clipTotal: number; phase: string; percent: number; isCurrentOwner?: boolean; positionInQueue?: number; estimatedWaitMin?: number } | null>(null);
  const [isQueued, setIsQueued]           = useState(false);

  const fileInputRef      = useRef<HTMLInputElement>(null);
  const pollRef           = useRef<ReturnType<typeof setInterval> | null>(null);
  const prevGalleryLen    = useRef(0);

  const clipCount = calcClips(totalSeconds, fps);
  const tokenCost = clipCount * 100; // 100 tokens par clip (~4s)
  // Temps estimé : ~2-4 min par clip selon qualité
  const minPerClip = QUALITY_MAP[quality].minPerClip;
  const estMin = Math.round(clipCount * minPerClip + (mode === 'text' ? 2 : 0));
  // Polls nécessaires pour couvrir le temps estimé + 50% de marge
  const MAX_POLLS = Math.max(240, Math.ceil(estMin * 60 / 5 * 1.5));

  // ── Charger profil (tokens + admin) ──────────────────────────────────────
  useEffect(() => {
    const user = currentUser();
    if (!user || user === 'invité') return;
    fetch(`${SERVER()}/api/user/profile/${user}`)
      .then(r => r.ok ? r.json() : null)
      .then(p => {
        if (!p) return;
        setUserTokens(typeof p.tokens === 'number' ? p.tokens : null);
        setIsAdmin(p.abonnement === 'god_mode' || p.role === 'admin');
      })
      .catch(() => {});
    fetch(`${SERVER()}/api/auth/is-admin`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: user })
    }).then(r => r.ok ? r.json() : null).then(d => { if (d?.isAdmin) setIsAdmin(true); }).catch(() => {});
  }, []);

  // ── Charger galerie ───────────────────────────────────────────────────────
  const fetchGallery = useCallback(async () => {
    try {
      const r = await fetch(`${SERVER()}/api/studio/veo/gallery/${currentUser()}`);
      if (!r.ok) return;
      const data = await r.json();
      const items: HistoryItem[] = (data.files || [])
        .map((f: any) => ({ filename: f.filename, url: f.url, createdAt: f.createdAt || 0 }))
        .sort((a: HistoryItem, b: HistoryItem) => b.createdAt - a.createdAt);
      setHistory(items);
      if (!isGenerating) {
        // Pas en génération : on synchronise le compteur (évite false-positive au prochain poll)
        prevGalleryLen.current = items.length;
        return;
      }
      if (items.length > prevGalleryLen.current) {
        prevGalleryLen.current = items.length;
        setGeneratedVideo(items[0].url);
        setIsGenerating(false);
        setIsQueued(false);
        setVeoProgress(null);
        setError(null);
        stopPoll();
        stopStatusPoll();
        // Rafraîchir les tokens
        fetch(`${SERVER()}/api/user/profile/${currentUser()}`)
          .then(r => r.ok ? r.json() : null)
          .then(p => { if (p && typeof p.tokens === 'number') setUserTokens(p.tokens); })
          .catch(() => {});
      }
    } catch {}
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isGenerating]);

  const stopPoll = () => {
    if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null; }
  };

  useEffect(() => { fetchGallery(); }, []);

  // Reprise après reload : détecter si une génération est en cours
  useEffect(() => {
    const checkOngoing = async () => {
      try {
        // Récupérer la galerie AVANT de set isGenerating pour initialiser prevGalleryLen correctement
        const gr = await fetch(`${SERVER()}/api/studio/veo/gallery/${currentUser()}`);
        const gd = gr.ok ? await gr.json() : { files: [] };
        prevGalleryLen.current = (gd.files || []).length;

        const r = await fetch(`${SERVER()}/api/studio/veo/status/${currentUser()}`);
        if (!r.ok) return;
        const d = await r.json();
        if (d.isCurrentOwner && d.busy) {
          setIsGenerating(true);
          setVeoProgress(d);
        } else if (d.positionInQueue > 0) {
          setIsQueued(true);
          setIsGenerating(true);
          setVeoProgress(d);
        }
      } catch {}
    };
    checkOngoing();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Init image depuis Forge3D
  useEffect(() => {
    const initImg = localStorage.getItem('lea_veo_init_image');
    if (initImg) {
      localStorage.removeItem('lea_veo_init_image');
      setMode('image');
      setImagePayload({ type: 'url', data: initImg });
      setImagePreview(`${SERVER()}${initImg}`);
    }
  }, []);

  // Poll status clip (toutes les 3s pendant génération)
  const fetchVeoStatus = useCallback(async () => {
    try {
      const r = await fetch(`${SERVER()}/api/studio/veo/status/${currentUser()}`);
      if (!r.ok) return;
      const d = await r.json();
      setVeoProgress(d);
      // Mise à jour état file
      if (d.positionInQueue > 0) {
        setIsQueued(true);
      } else if (d.isCurrentOwner) {
        setIsQueued(false);
      } else if (d.phase === 'idle' && !d.busy) {
        setIsQueued(false);
      }
    } catch {}
  }, []);

  // Poll pendant génération — bascule en mode lent après MAX_POLLS (ne s'arrête pas)
  const statusPollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const stopStatusPoll = () => { if (statusPollRef.current) { clearInterval(statusPollRef.current); statusPollRef.current = null; } };

  useEffect(() => {
    if (isGenerating) {
      setPollCount(0);
      setVeoProgress(null);
      // Poll gallery toutes les 5s
      pollRef.current = setInterval(() => {
        setPollCount(c => {
          const next = c + 1;
          if (next === MAX_POLLS) {
            stopPoll();
            setError(`Génération longue (~${estMin} min estimées). La vidéo apparaîtra dans la bibliothèque dès qu'elle sera prête.`);
            pollRef.current = setInterval(() => { fetchGallery(); }, 30000);
          }
          return next;
        });
        fetchGallery();
      }, 5000);
      // Poll status clip toutes les 3s
      statusPollRef.current = setInterval(fetchVeoStatus, 3000);
    } else {
      stopPoll();
      stopStatusPoll();
      setVeoProgress(null);
    }
    return () => { stopPoll(); stopStatusPoll(); };
  }, [isGenerating, fetchGallery, fetchVeoStatus, MAX_POLLS, estMin]);

  // ── Upload image ──────────────────────────────────────────────────────────
  const handleFile = (file: File) => {
    setImageFile(file);
    const reader = new FileReader();
    reader.onload = e => {
      const b64 = e.target?.result as string;
      setImagePreview(b64);
      setImagePayload({ type: 'base64', data: b64 });
    };
    reader.readAsDataURL(file);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) handleFile(file);
  };

  const clearImage = () => {
    setImageFile(null);
    setImagePreview(null);
    setImagePayload(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // ── Générer ───────────────────────────────────────────────────────────────
  const generate = async () => {
    if (mode === 'image' && !imagePayload) return;
    if (mode === 'text' && !prompt.trim()) return;
    setError(null);
    setIsGenerating(true);
    setGeneratedVideo(null);
    prevGalleryLen.current = history.length;

    const body: Record<string, any> = {
      username: currentUser(),
      totalSeconds,
      fps,
      motion: MOTION_NATURAL,
      steps: QUALITY_MAP[quality].steps,
    };
    if (mode === 'text') {
      body.prompt = prompt.trim();
    } else if (imagePayload?.type === 'base64') {
      body.image_base64 = imagePayload.data;
    } else if (imagePayload?.type === 'url') {
      body.image_url = imagePayload.data;
    }

    try {
      const r = await fetch(`${SERVER()}/api/studio/veo/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await r.json();
      if (data.queued) {
        // En file d'attente — pas d'erreur, juste attendre
        setIsQueued(true);
        setVeoProgress({ clipDone: 0, clipTotal: 0, phase: 'queued', percent: 0, positionInQueue: data.position, estimatedWaitMin: data.estimatedWaitMin });
        return;
      }
      if (!r.ok || !data.success) {
        setError(data.error || 'Erreur génération');
        setIsGenerating(false);
        return;
      }
      setIsQueued(false);
      // Déduire tokens localement pour affichage immédiat
      if (!isAdmin && userTokens !== null) {
        setUserTokens(prev => prev !== null ? Math.max(0, prev - (data.tokenCost || 0)) : null);
      }
    } catch (e: any) {
      setError(e.message || 'Erreur réseau');
      setIsGenerating(false);
    }
  };

  // ── Télécharger ───────────────────────────────────────────────────────────
  const downloadVideo = async (item: HistoryItem) => {
    try {
      const r = await fetch(`${SERVER()}${item.url}`);
      const blob = await r.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = item.filename;
      a.click();
      setDownloadedId(item.filename);
      setTimeout(() => setDownloadedId(null), 2000);
    } catch {}
  };

  // ── Supprimer ─────────────────────────────────────────────────────────────
  const deleteVideo = async (item: HistoryItem) => {
    setDeletingId(item.filename);
    try {
      const r = await fetch(`${SERVER()}/api/studio/veo/gallery/${currentUser()}/${encodeURIComponent(item.filename)}`, {
        method: 'DELETE',
      });
      if (r.ok) {
        setHistory(prev => prev.filter(h => h.filename !== item.filename));
        if (generatedVideo === item.url) setGeneratedVideo(null);
        prevGalleryLen.current = Math.max(0, prevGalleryLen.current - 1);
      }
    } catch {}
    setDeletingId(null);
    setConfirmDeleteId(null);
  };

  const canGenerate = mode === 'image' ? !!imagePayload : !!prompt.trim();
  const hasEnoughTokens = isAdmin || userTokens === null || userTokens >= tokenCost;

  return (
    <div className="flex flex-col h-full bg-[#020617] overflow-hidden" style={{ backgroundImage: BG_GRID }}>
      {/* Halo */}
      <div className="pointer-events-none absolute top-0 left-1/2 -translate-x-1/2 w-96 h-48 bg-purple-600/10 blur-3xl rounded-full" />

      {/* ── HEADER ── */}
      <div className="relative flex-none px-4 pt-5 pb-4 border-b border-white/5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-2xl bg-gradient-to-br from-purple-600 to-fuchsia-600 flex items-center justify-center shadow-[0_0_16px_rgba(168,85,247,0.4)]">
              <Film size={17} className="text-white" />
            </div>
            <div>
              <h1 className="text-sm font-black text-white uppercase italic tracking-tight">Studio Veo</h1>
              <p className="text-[9px] font-bold tracking-widest uppercase text-slate-500">SVD XT · Chaînage clips</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* Badge tokens */}
            {isAdmin ? (
              <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-yellow-500/10 border border-yellow-500/20">
                <Crown size={10} className="text-yellow-400" />
                <span className="text-[9px] font-black uppercase text-yellow-400">Admin · Gratuit</span>
              </div>
            ) : userTokens !== null ? (
              <div className={`flex items-center gap-1 px-2.5 py-1 rounded-full border ${userTokens < tokenCost ? 'bg-red-500/10 border-red-500/20' : 'bg-white/5 border-white/10'}`}>
                <Coins size={10} className={userTokens < tokenCost ? 'text-red-400' : 'text-purple-400'} />
                <span className={`text-[9px] font-bold ${userTokens < tokenCost ? 'text-red-300' : 'text-slate-300'}`}>{userTokens.toLocaleString()} 🪙</span>
              </div>
            ) : null}
            <button
              onClick={() => { setShowHistory(v => !v); if (!showHistory) fetchGallery(); }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-[10px] font-bold transition-all ${showHistory ? 'bg-purple-500/20 border-purple-500/30 text-purple-300' : 'border-white/10 text-slate-400 hover:text-white hover:border-white/20'}`}
            >
              <Layers size={11} />
              {history.length > 0 && <span className="bg-purple-500/30 text-purple-300 px-1.5 rounded-full text-[9px]">{history.length}</span>}
            </button>
          </div>
        </div>

        {/* Mode */}
        <div className="mt-3 flex gap-1.5 p-1 bg-white/5 border border-white/10 rounded-xl">
          {([['image', ImageIcon, 'Image → Vidéo'], ['text', Type, 'Texte → Vidéo']] as const).map(([m, Icon, label]) => (
            <button
              key={m}
              onClick={() => {
                const newMode = m as Mode;
                setMode(newMode);
                // Réinitialiser la durée si elle n'existe pas dans le nouveau mode
                const validDurations = newMode === 'image' ? DURATIONS_IMAGE : DURATIONS_TEXT;
                if (!validDurations.find(d => d.value === totalSeconds)) {
                  setTotalSeconds(validDurations[0].value);
                }
              }}
              className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${mode === m ? 'bg-gradient-to-r from-purple-600 to-fuchsia-600 text-white shadow-[0_0_12px_rgba(168,85,247,0.3)]' : 'text-slate-500 hover:text-slate-300'}`}
            >
              <Icon size={11} />
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* ── SCROLL ── */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">

        {/* Bibliothèque */}
        {showHistory && (
          <div className="bg-white/5 border border-white/10 rounded-2xl overflow-hidden">
            <div className="flex items-center justify-between px-3 py-2.5 border-b border-white/5">
              <span className="text-[9px] font-black uppercase tracking-widest text-slate-500">Bibliothèque</span>
              <button onClick={fetchGallery} className="text-slate-500 hover:text-purple-400 transition-all active:scale-90">
                <RefreshCw size={12} />
              </button>
            </div>
            {history.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 opacity-30">
                <Film size={28} className="mb-2" />
                <p className="text-[10px] font-bold tracking-widest uppercase text-slate-500">Aucune vidéo</p>
              </div>
            ) : (
              <div className="divide-y divide-white/5 max-h-64 overflow-y-auto">
                {history.map(item => (
                  <div key={item.filename} className="flex items-center gap-3 px-3 py-2.5 hover:bg-white/5 transition-all group">
                    <div className="w-14 h-10 rounded-lg overflow-hidden bg-black/40 flex-none border border-white/10">
                      {item.filename.endsWith('.mp4') || item.filename.endsWith('.webm')
                        ? <video src={`${SERVER()}${item.url}`} className="w-full h-full object-cover" muted playsInline />
                        : <img src={`${SERVER()}${item.url}`} alt="" className="w-full h-full object-cover" loading="lazy" />
                      }
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[10px] font-bold text-slate-300 truncate">{item.filename}</p>
                      <p className="text-[9px] text-slate-600 font-mono">
                        {item.createdAt ? new Date(item.createdAt).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—'}
                      </p>
                    </div>
                    <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-all">
                      <button
                        onClick={() => { setGeneratedVideo(item.url); setShowHistory(false); }}
                        className="w-7 h-7 rounded-lg bg-purple-500/20 flex items-center justify-center text-purple-400 hover:bg-purple-500/30 transition-all"
                        title="Afficher"
                      >
                        <Play size={11} />
                      </button>
                      <button
                        onClick={() => downloadVideo(item)}
                        className={`w-7 h-7 rounded-lg flex items-center justify-center transition-all ${downloadedId === item.filename ? 'bg-green-500/20 text-green-400' : 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white'}`}
                        title="Télécharger"
                      >
                        {downloadedId === item.filename ? <Check size={11} /> : <Download size={11} />}
                      </button>
                      {confirmDeleteId === item.filename ? (
                        <>
                          <button
                            onClick={() => deleteVideo(item)}
                            disabled={deletingId === item.filename}
                            className="w-7 h-7 rounded-lg bg-red-500/30 flex items-center justify-center text-red-300 hover:bg-red-500/50 transition-all"
                            title="Confirmer suppression"
                          >
                            {deletingId === item.filename ? <Loader2 size={11} className="animate-spin" /> : <Check size={11} />}
                          </button>
                          <button
                            onClick={() => setConfirmDeleteId(null)}
                            className="w-7 h-7 rounded-lg bg-white/5 flex items-center justify-center text-slate-400 hover:bg-white/10 transition-all"
                            title="Annuler"
                          >
                            <X size={11} />
                          </button>
                        </>
                      ) : (
                        <button
                          onClick={() => setConfirmDeleteId(item.filename)}
                          className="w-7 h-7 rounded-lg bg-white/5 flex items-center justify-center text-slate-500 hover:bg-red-500/20 hover:text-red-400 transition-all"
                          title="Supprimer"
                        >
                          <Trash2 size={11} />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Résultat */}
        {generatedVideo && (
          <div className="bg-black/40 border border-purple-500/20 rounded-2xl overflow-hidden shadow-[0_0_24px_rgba(168,85,247,0.12)]">
            <div className="flex items-center justify-between px-3 py-2 border-b border-white/5">
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
                <span className="text-[9px] font-black uppercase tracking-widest text-green-400">Vidéo prête</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => {
                    const found = history.find(h => h.url === generatedVideo);
                    downloadVideo(found || { filename: 'video.webp', url: generatedVideo!, createdAt: 0 });
                  }}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-purple-500/20 border border-purple-500/30 text-purple-300 text-[10px] font-bold hover:bg-purple-500/30 transition-all active:scale-95"
                >
                  <Download size={11} />
                  Télécharger
                </button>
                <button
                  onClick={() => setGeneratedVideo(null)}
                  className="w-6 h-6 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-slate-500 hover:text-red-400 hover:border-red-500/20 transition-all"
                >
                  <X size={11} />
                </button>
              </div>
            </div>
            {generatedVideo.endsWith('.mp4') || generatedVideo.endsWith('.webm')
              ? <video src={`${SERVER()}${generatedVideo}`} className="w-full max-h-64 object-contain" controls autoPlay loop muted playsInline />
              : <img src={`${SERVER()}${generatedVideo}`} alt="Vidéo générée" className="w-full object-contain max-h-64" />
            }
          </div>
        )}

        {/* Zone IMAGE */}
        {mode === 'image' && (
          <div>
            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5">Image source</p>
            {!imagePreview ? (
              <div
                onDrop={handleDrop}
                onDragOver={e => e.preventDefault()}
                onClick={() => fileInputRef.current?.click()}
                className="relative border-2 border-dashed border-white/10 hover:border-purple-500/40 rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all group"
              >
                <div className="w-12 h-12 rounded-2xl bg-white/5 group-hover:bg-purple-500/10 flex items-center justify-center mb-3 transition-all">
                  <Upload size={20} className="text-slate-600 group-hover:text-purple-400 transition-all" />
                </div>
                <p className="text-xs font-bold text-slate-500 group-hover:text-slate-300 transition-all">Glissez ou cliquez pour choisir</p>
                <p className="text-[9px] text-slate-600 mt-1">PNG · JPG · WEBP · max 10 Mo</p>
                <input ref={fileInputRef} type="file" accept="image/*" className="hidden"
                  onChange={e => { if (e.target.files?.[0]) handleFile(e.target.files[0]); }} />
              </div>
            ) : (
              <div className="relative rounded-2xl overflow-hidden border border-white/10 bg-black/40 group">
                <img src={imagePreview} alt="Source" className="w-full max-h-48 object-contain" />
                <button
                  onClick={clearImage}
                  className="absolute top-2 right-2 w-7 h-7 rounded-full bg-black/70 border border-white/10 flex items-center justify-center text-slate-400 hover:text-red-400 hover:border-red-500/30 opacity-0 group-hover:opacity-100 transition-all"
                >
                  <X size={13} />
                </button>
                {imageFile && (
                  <div className="absolute bottom-2 left-2 bg-black/60 backdrop-blur px-2 py-1 rounded-lg border border-white/10">
                    <p className="text-[9px] text-slate-400 font-bold truncate max-w-[160px]">{imageFile.name}</p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Zone TEXTE */}
        {mode === 'text' && (
          <div>
            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5">Description de la vidéo</p>
            <div className="relative">
              <textarea
                value={prompt}
                onChange={e => setPrompt(e.target.value)}
                placeholder="Une aurore boréale dansant sur un lac de montagne…"
                rows={4}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white placeholder:text-slate-600 outline-none focus:border-purple-500/40 resize-none transition-all"
              />
              {prompt && (
                <button onClick={() => setPrompt('')} className="absolute top-2 right-2 w-6 h-6 rounded-full bg-white/5 flex items-center justify-center text-slate-500 hover:text-red-400 transition-all">
                  <X size={11} />
                </button>
              )}
            </div>
            <p className="text-[9px] text-slate-600 mt-1.5">FLUX génère d'abord une image, puis SVD XT l'anime clip par clip.</p>
          </div>
        )}

        {/* ── Paramètres ── */}
        <div className="space-y-3">
          {/* Durée totale */}
          <div>
            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5 flex items-center gap-1.5">
              <Clock size={9} /> Durée totale
            </p>
            <div className={`grid gap-1.5 ${mode === 'image' ? 'grid-cols-4' : 'grid-cols-3'}`}>
              {(mode === 'image' ? DURATIONS_IMAGE : DURATIONS_TEXT).map(d => {
                const clips = calcClips(d.value, fps);
                const isActive = totalSeconds === d.value;
                // Si changement de mode et durée invalide, reset
                return (
                  <button
                    key={d.value}
                    onClick={() => setTotalSeconds(d.value)}
                    className={`flex flex-col items-center py-2 rounded-xl border text-[10px] font-black transition-all active:scale-95 ${isActive ? 'bg-purple-500/20 border-purple-500/40 text-purple-300' : 'border-white/10 text-slate-500 hover:text-slate-300 hover:border-white/20'}`}
                  >
                    <span className="text-xs font-black">{d.label}</span>
                    <span className="opacity-50 font-normal text-[9px] mt-0.5">{clips} clip{clips > 1 ? 's' : ''}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* FPS */}
          <div>
            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5">Fluidité — {fps} fps</p>
            <div className="flex gap-2">
              {[6, 8].map(f => (
                <button key={f} onClick={() => setFps(f)}
                  className={`flex-1 py-2 rounded-xl border text-[10px] font-black transition-all active:scale-95 ${fps === f ? 'bg-purple-500/20 border-purple-500/40 text-purple-300' : 'border-white/10 text-slate-500 hover:text-slate-300 hover:border-white/20'}`}
                >
                  {f} fps
                </button>
              ))}
            </div>
          </div>

          {/* Qualité */}
          <div>
            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5">Qualité</p>
            <div className="flex gap-2">
              {(Object.entries(QUALITY_MAP) as [Quality, typeof QUALITY_MAP[Quality]][]).map(([k, v]) => (
                <button key={k} onClick={() => setQuality(k)}
                  className={`flex-1 flex flex-col items-center py-2.5 rounded-xl border text-[10px] font-black transition-all active:scale-95 ${quality === k ? 'bg-purple-500/20 border-purple-500/40 text-purple-300' : 'border-white/10 text-slate-500 hover:text-slate-300 hover:border-white/20'}`}
                >
                  <span className={quality === k ? 'text-purple-400' : v.color}>{v.icon}</span>
                  <span className="mt-1">{v.label}</span>
                  <span className="opacity-50 font-normal text-[9px]">{v.steps} steps</span>
                </button>
              ))}
            </div>
          </div>

        </div>

        {/* ── Récap génération ── */}
        <div className="flex gap-2 flex-wrap">
          <div className="flex items-center gap-1.5 px-2.5 py-1.5 bg-white/5 border border-white/10 rounded-xl">
            <Link2 size={10} className="text-slate-500" />
            <span className="text-[10px] text-slate-400 font-bold">{clipCount} clip{clipCount > 1 ? 's' : ''} SVD XT</span>
          </div>
          <div className="flex items-center gap-1.5 px-2.5 py-1.5 bg-white/5 border border-white/10 rounded-xl">
            <Clock size={10} className="text-slate-500" />
            <span className="text-[10px] text-slate-400 font-bold">~{estMin} min</span>
          </div>
          {isAdmin ? (
            <div className="flex items-center gap-1.5 px-2.5 py-1.5 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
              <Crown size={10} className="text-yellow-400" />
              <span className="text-[10px] text-yellow-400 font-bold">Gratuit</span>
            </div>
          ) : (
            <div className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border ${hasEnoughTokens ? 'bg-white/5 border-white/10' : 'bg-red-500/10 border-red-500/20'}`}>
              <Coins size={10} className={hasEnoughTokens ? 'text-purple-400' : 'text-red-400'} />
              <span className={`text-[10px] font-bold ${hasEnoughTokens ? 'text-slate-400' : 'text-red-300'}`}>{tokenCost} token{tokenCost > 1 ? 's' : ''}</span>
            </div>
          )}
        </div>

        {/* Erreur / Avertissement long */}
        {error && (
          <div className={`flex flex-col gap-2 px-3 py-2.5 rounded-xl border ${error.includes('longue') ? 'bg-yellow-500/10 border-yellow-500/20' : 'bg-red-500/10 border-red-500/20'}`}>
            <div className="flex items-start gap-2">
              <Clock size={13} className={`mt-0.5 flex-none ${error.includes('longue') ? 'text-yellow-400' : 'text-red-400'}`} />
              <p className={`text-[11px] ${error.includes('longue') ? 'text-yellow-300' : 'text-red-300'}`}>{error}</p>
            </div>
            {error.includes('longue') && (
              <button
                onClick={() => { fetchGallery(); setShowHistory(true); }}
                className="w-full py-2 rounded-lg bg-purple-500/20 border border-purple-500/30 text-purple-300 text-[10px] font-bold hover:bg-purple-500/30 transition-all"
              >
                Voir la bibliothèque (vérification auto toutes les 30s)
              </button>
            )}
          </div>
        )}

        {/* Progression / File d'attente */}
        {isGenerating && (
          <div className={`border rounded-2xl px-4 py-4 ${isQueued && veoProgress?.phase === 'queued' ? 'bg-amber-500/5 border-amber-500/20' : 'bg-purple-500/5 border-purple-500/20'}`}>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2.5">
                <Loader2 size={16} className={`${isQueued && veoProgress?.phase === 'queued' ? 'text-amber-400' : 'text-purple-400'} animate-spin flex-none`} />
                <div>
                  {/* Titre */}
                  <p className={`text-xs font-bold ${isQueued && veoProgress?.phase === 'queued' ? 'text-amber-300' : 'text-purple-300'}`}>
                    {isQueued && veoProgress?.phase === 'queued'
                      ? `File d'attente — position ${veoProgress.positionInQueue}`
                      : veoProgress?.phase === 'converting'
                        ? 'Conversion MP4 en cours…'
                        : clipCount > 1
                          ? `Chaînage SVD XT${mode === 'text' ? ' (FLUX+)' : ''}…`
                          : `SVD XT${mode === 'text' ? ' (FLUX+)' : ''} en cours…`}
                  </p>
                  {/* Clip X / Y — uniquement si génération active */}
                  {!isQueued && veoProgress && veoProgress.clipTotal > 0 && (
                    <p className="text-[10px] text-purple-400 font-mono font-bold mt-0.5">
                      Clip {veoProgress.phase === 'generating'
                        ? Math.min(veoProgress.clipDone + 1, veoProgress.clipTotal)
                        : veoProgress.clipDone} / {veoProgress.clipTotal}
                      {` — ${veoProgress.percent}%`}
                    </p>
                  )}
                  {/* Estimation attente */}
                  <p className="text-[9px] text-slate-500 mt-0.5">
                    {isQueued && veoProgress?.phase === 'queued'
                      ? `Temps estimé avant ta génération : ~${veoProgress.estimatedWaitMin ?? estMin} min`
                      : `~${veoProgress?.estimatedWaitMin ?? estMin} min restantes`}
                  </p>
                </div>
              </div>
              <button
                onClick={() => { stopPoll(); stopStatusPoll(); setIsGenerating(false); setIsQueued(false); setVeoProgress(null); setError('Génération annulée. La vidéo continue en arrière-plan — vérifiez la bibliothèque.'); }}
                className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-slate-500 hover:text-red-400 hover:border-red-500/20 text-[10px] font-bold transition-all"
              >
                Annuler
              </button>
            </div>

            {/* Barre de progression */}
            {!isQueued && (
              <>
                <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-purple-600 to-fuchsia-600 rounded-full transition-all duration-700"
                    style={{ width: veoProgress && veoProgress.clipTotal > 0 ? `${veoProgress.percent}%` : '2%' }}
                  />
                </div>
                <div className="flex items-center justify-between mt-1.5">
                  <p className="text-[9px] text-slate-500 font-mono font-bold">
                    {veoProgress && veoProgress.clipTotal > 0 ? `${veoProgress.percent}%` : 'En attente GPU…'}
                  </p>
                  <p className="text-[9px] text-slate-600 font-mono">Scan #{pollCount + 1}</p>
                </div>
              </>
            )}

            {/* Barre d'attente file (indéterminée) */}
            {isQueued && veoProgress?.phase === 'queued' && (
              <div className="w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
                <div className="h-full w-1/3 bg-gradient-to-r from-amber-500 to-orange-500 rounded-full animate-[pulse_2s_ease-in-out_infinite]" />
              </div>
            )}
          </div>
        )}

        <div className="h-4" />
      </div>

      {/* ── BOUTON GÉNÉRER ── */}
      <div className="flex-none px-4 pb-5 pt-3 border-t border-white/5 bg-[#020617]/90 backdrop-blur">
        {!hasEnoughTokens && !isAdmin && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 mb-3 text-center">
            <p className="text-red-300 text-xs font-bold mb-1">
              Tokens insuffisants — {(userTokens ?? 0).toLocaleString()} 🪙 disponibles, {tokenCost.toLocaleString()} 🪙 requis
            </p>
            <p className="text-slate-500 text-[10px] mb-2">Il te faut plus de tokens pour générer cette vidéo.</p>
            <button
              onClick={() => window.dispatchEvent(new CustomEvent('lea-navigate', { detail: { module: 'shop' } }))}
              className="bg-gradient-to-r from-yellow-500 to-orange-500 text-white text-[10px] font-black uppercase tracking-widest px-4 py-2 rounded-lg hover:opacity-90 active:scale-95 transition-all"
            >
              Recharger mes tokens 🪙
            </button>
          </div>
        )}
        <button
          onClick={generate}
          disabled={isGenerating || !canGenerate || (!hasEnoughTokens && !isAdmin)}
          className={`w-full py-4 rounded-2xl font-black uppercase tracking-widest text-sm transition-all flex items-center justify-center gap-2 ${
            isGenerating || !canGenerate || (!hasEnoughTokens && !isAdmin)
              ? 'bg-white/5 border border-white/10 text-slate-600 cursor-not-allowed'
              : 'bg-gradient-to-r from-purple-600 to-fuchsia-600 text-white hover:opacity-90 active:scale-95 shadow-[0_0_20px_rgba(168,85,247,0.4)]'
          }`}
        >
          {isGenerating
            ? <><Loader2 size={16} className="animate-spin" /> Génération en cours…</>
            : <><Sparkles size={16} /> Générer {totalSeconds}s · {clipCount} clip{clipCount > 1 ? 's' : ''} · {tokenCost.toLocaleString()} 🪙</>
          }
        </button>
      </div>
    </div>
  );
}
