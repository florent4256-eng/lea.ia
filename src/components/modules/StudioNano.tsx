import React, { useState, useCallback, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  Camera, Sparkles, Download, Clock, Image as ImageIcon,
  AlertTriangle, X, Maximize2, Minimize2, Images, RefreshCw,
  Zap, Star, Crown, ChevronDown, ChevronUp, Wand2, Settings2, Coins
} from 'lucide-react';

// ─── MODÈLES ────────────────────────────────────────────────────────────────
const MODELS = [
  { id: 'flux',         label: 'FLUX Dev',      sub: 'Q5_K_S · Meilleur',   badge: '👑', engine: 'flux' },
  { id: 'juggernaut',  label: 'JuggernautXL',   sub: 'SDXL · Photoréaliste', badge: '📷', engine: 'sdxl', ckpt: 'JuggernautXL_v9.safetensors' },
  { id: 'realvis',     label: 'RealVisXL V5',   sub: 'SDXL · Portraits',    badge: '🎭', engine: 'sdxl', ckpt: 'RealVisXL_V5_fp16.safetensors' },
  { id: 'dreamshaper', label: 'DreamShaper 8',  sub: 'SD1.5 · Créatif',     badge: '🎨', engine: 'sd15', ckpt: 'dreamshaper_8.safetensors' },
];

// ─── FORMATS ────────────────────────────────────────────────────────────────
const FORMATS = [
  { id: 'square',     label: 'Carré',       ratio: '1:1',   w: 1024, h: 1024, icon: '⬛', sdW: 512,  sdH: 512  },
  { id: 'portrait',   label: 'Portrait',    ratio: '3:4',   w: 768,  h: 1024, icon: '📱', sdW: 384,  sdH: 512  },
  { id: 'landscape',  label: 'Paysage',     ratio: '16:9',  w: 1280, h: 720,  icon: '🖼️', sdW: 512,  sdH: 288  },
  { id: 'cinematic',  label: 'Cinéma',      ratio: '21:9',  w: 1344, h: 576,  icon: '🎬', sdW: 512,  sdH: 224  },
  { id: 'portrait_xl',label: 'Portrait XL', ratio: '2:3',   w: 832,  h: 1216, icon: '🧍', sdW: 416,  sdH: 608  },
  { id: 'wallpaper',  label: 'Wallpaper',   ratio: '9:16',  w: 720,  h: 1280, icon: '📲', sdW: 288,  sdH: 512  },
];

// ─── QUALITÉS ────────────────────────────────────────────────────────────────
const QUALITIES = [
  { id: 'turbo',    label: 'Turbo',    sub: '~20 sec',  steps: 10, icon: <Zap size={11}/> },
  { id: 'standard',label: 'Standard', sub: '~45 sec',  steps: 20, icon: <Star size={11}/> },
  { id: 'hd',      label: 'HD',       sub: '~80 sec',  steps: 35, icon: <Crown size={11}/> },
  { id: 'ultra',   label: 'Ultra',    sub: '~2 min',   steps: 50, icon: <Sparkles size={11}/> },
];

// ─── STYLES ──────────────────────────────────────────────────────────────────
const STYLES = [
  { id: 'photo',        label: 'Photo',         suffix: 'photorealistic, RAW photo, 8k, sharp focus, natural lighting',                    emoji: '📷' },
  { id: 'cinematic',    label: 'Cinématique',   suffix: 'cinematic shot, dramatic lighting, anamorphic lens, movie still, depth of field',  emoji: '🎬' },
  { id: 'portrait',     label: 'Portrait',      suffix: 'professional portrait, soft bokeh, studio lighting, sharp details, 85mm lens',     emoji: '🧑' },
  { id: 'paysage',      label: 'Paysage',       suffix: 'landscape photography, golden hour, wide angle, ultra detailed, atmospheric',       emoji: '🌅' },
  { id: 'illustration', label: 'Illustration',  suffix: 'digital illustration, vibrant colors, concept art, artstation, detailed',           emoji: '🎨' },
  { id: 'anime',        label: 'Anime',         suffix: 'anime style, cel shading, vibrant, studio ghibli quality, detailed',               emoji: '✨' },
  { id: 'peinture',     label: 'Peinture',      suffix: 'oil painting, impasto technique, museum quality, masterful brushwork',             emoji: '🖌️' },
  { id: 'scifi',        label: 'Sci-Fi',        suffix: 'science fiction, futuristic, neon lights, cyberpunk aesthetic, hyperdetailed',     emoji: '🚀' },
];

// ─── UPSCALE ─────────────────────────────────────────────────────────────────
const UPSCALES = [
  { id: 'none', label: 'Aucun',  sub: 'Original',    mult: 1 },
  { id: 'x2',   label: '×2',    sub: '2048×2048',    mult: 2 },
  { id: '4k',   label: '4K',    sub: '4096×4096',    mult: 4 },
];

type GalleryImage = { filename: string; url: string; date: number };
type Model  = typeof MODELS[0];
type Format = typeof FORMATS[0];
type Quality= typeof QUALITIES[0];
type Style  = typeof STYLES[0];
type Upscale= typeof UPSCALES[0];

export const StudioNano = () => {
  const [tab, setTab]                       = useState<'generate'|'gallery'>('generate');
  const [prompt, setPrompt]                 = useState('');
  const [negPrompt, setNegPrompt]           = useState('');
  const [showNeg, setShowNeg]               = useState(false);
  const [showAdvanced, setShowAdvanced]     = useState(false);
  const [model, setModel]                   = useState<Model>(MODELS[0]);
  const [format, setFormat]                 = useState<Format>(FORMATS[0]);
  const [quality, setQuality]               = useState<Quality>(QUALITIES[1]);
  const [style, setStyle]                   = useState<Style>(STYLES[0]);
  const [upscale, setUpscale]               = useState<Upscale>(UPSCALES[0]);
  const [seed, setSeed]                     = useState('');
  const [isGenerating, setIsGenerating]     = useState(false);
  const [generatedImage, setGeneratedImage] = useState<string|null>(null);
  const [error, setError]                   = useState<string|null>(null);
  const [elapsed, setElapsed]               = useState(0);
  const [modalUrl, setModalUrl]             = useState<string|null>(null);
  const [modalExpanded, setModalExpanded]   = useState(false);
  const [dlDone, setDlDone]                 = useState(false);
  const [gallery, setGallery]               = useState<GalleryImage[]>([]);
  const [galleryLoading, setGalleryLoading] = useState(false);
  const [lastPromptEnriched, setLastPromptEnriched] = useState('');
  const [userTokens, setUserTokens]         = useState<number | null>(null);
  const [tokenError, setTokenError]         = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval>|null>(null);

  const serverUrl   = (window as any).LEA_SERVER_URL || '';
  const currentUser = localStorage.getItem('lea_currentUser') || '';

  // Résolution effective selon le modèle
  const effectiveW = model.engine === 'sd15' ? format.sdW : format.w;
  const effectiveH = model.engine === 'sd15' ? format.sdH : format.h;
  const outputW    = effectiveW * upscale.mult;
  const outputH    = effectiveH * upscale.mult;

  const loadGallery = useCallback(async () => {
    setGalleryLoading(true);
    try {
      const res = await fetch(`${serverUrl}/api/studio/nano/gallery/${currentUser}`);
      const data = await res.json();
      setGallery(data.images || []);
    } catch { setGallery([]); }
    finally { setGalleryLoading(false); }
  }, [serverUrl, currentUser]);

  useEffect(() => { if (tab === 'gallery') loadGallery(); }, [tab, loadGallery]);

  // Fetch tokens au montage + polling 10s
  useEffect(() => {
    if (!currentUser) return;
    const fetchTk = () => fetch(`${serverUrl}/api/user/profile/${currentUser}`)
      .then(r => r.ok ? r.json() : null)
      .then(p => { if (p && typeof p.tokens === 'number') setUserTokens(p.tokens); })
      .catch(() => {});
    fetchTk();
    const iv = setInterval(fetchTk, 10000);
    return () => clearInterval(iv);
  }, [serverUrl, currentUser]);

  // Auto-sélection du bon moteur selon le modèle
  useEffect(() => {
    if (model.engine !== 'flux') setShowNeg(true);
    else setShowNeg(false);
  }, [model]);

  const nanoCost = upscale.id === '4k' ? 70 : 30;
  const isAdmin = userTokens === null; // null = pas encore chargé ou admin (on laisse passer)

  const handleGenerate = async () => {
    if (!prompt.trim() || isGenerating) return;
    setTokenError(null);
    // Vérification tokens côté frontend
    if (userTokens !== null && userTokens < nanoCost) {
      setTokenError(`Il te faut ${nanoCost} 🪙 pour générer cette image, tu en as ${userTokens.toLocaleString()}.`);
      return;
    }
    setIsGenerating(true);
    setError(null);
    setGeneratedImage(null);
    setModalUrl(null);
    setDlDone(false);
    setElapsed(0);
    setLastPromptEnriched('');

    timerRef.current = setInterval(() => setElapsed(s => s + 1), 1000);

    try {
      const res = await fetch(`${serverUrl}/api/studio/nano/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username:        currentUser,
          prompt:          prompt.trim(),
          negative_prompt: negPrompt.trim(),
          style:           style.id,
          style_suffix:    style.suffix,
          model_engine:    model.engine,
          model_ckpt:      model.ckpt || null,
          width:           effectiveW,
          height:          effectiveH,
          steps:           quality.steps,
          seed:            seed ? Number(seed) : -1,
          upscale:         upscale.id,
        }),
      });

      const data = await res.json();
      if (!res.ok || !data.image_url) throw new Error(data.error || 'Erreur de génération');
      const fullUrl = `${serverUrl}${data.image_url}`;
      if (data.enriched_prompt) setLastPromptEnriched(data.enriched_prompt);
      setGeneratedImage(fullUrl);
      setModalUrl(fullUrl);
    } catch (e: any) {
      setError(e.message || 'ComfyUI est inaccessible.');
    } finally {
      if (timerRef.current) clearInterval(timerRef.current);
      setIsGenerating(false);
    }
  };

  const handleDownload = useCallback(async (url: string) => {
    try {
      const res = await fetch(url);
      const blob = await res.blob();
      const objUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = objUrl;
      a.download = `lea_flux_${Date.now()}.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(objUrl), 2000);
      setDlDone(true);
      setTimeout(() => setDlDone(false), 3000);
    } catch { window.open(url, '_blank'); }
  }, []);

  const openModal  = (url: string) => { setDlDone(false); setModalExpanded(false); setModalUrl(url); };
  const closeModal = () => { setModalUrl(null); setModalExpanded(false); };
  const fmt = (s: number) => `${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`;

  // ─── Badge moteur header ───────────────────────────────────────────────────
  const engineLabel = model.engine === 'flux'
    ? 'FLUX Dev · RTX 3060 · 12 Go'
    : model.engine === 'sdxl'
    ? 'SDXL · RTX 3060 · 12 Go'
    : 'SD 1.5 · RTX 3060 · 12 Go';

  return (
    <>
      {/* ─── MODAL PLEIN ÉCRAN ─────────────────────────────────────────── */}
      {modalUrl && createPortal(
        <>
          <div className="fixed inset-0 z-[9998] bg-black/60 backdrop-blur-sm" onClick={closeModal} />
          <div
            role="dialog" aria-modal="true"
            className={`fixed z-[9999] flex flex-col overflow-hidden rounded-2xl border border-white/15 shadow-2xl backdrop-blur-xl transition-all duration-300 ${
              modalExpanded ? 'inset-2' : 'left-1/2 top-1/2 h-[70%] w-[90%] max-w-sm -translate-x-1/2 -translate-y-1/2'
            }`}
            style={{ background: 'rgba(8,4,20,0.85)' }}
          >
            <div className="flex items-center justify-between border-b border-white/10 bg-black/40 px-3 py-2 shrink-0">
              <span className="text-[10px] font-black uppercase tracking-widest text-purple-400">Studio Nano · Léa</span>
              <div className="flex items-center gap-1">
                <button onClick={() => setModalExpanded(v => !v)}
                  className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-white/10 hover:text-white transition-colors">
                  {modalExpanded ? <Minimize2 size={13}/> : <Maximize2 size={13}/>}
                </button>
                <button onClick={() => handleDownload(modalUrl)}
                  className="flex h-7 w-7 items-center justify-center rounded-lg transition-colors hover:bg-white/10"
                  style={{ color: dlDone ? '#22c55e' : '#94a3b8' }}>
                  {dlDone
                    ? <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                    : <Download size={13}/>}
                </button>
                <button onClick={closeModal}
                  className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-red-600/70 hover:text-white transition-colors">
                  <X size={13}/>
                </button>
              </div>
            </div>
            <div className="flex-1 p-2 min-h-0">
              <img src={modalUrl} alt="Image générée"
                className="h-full w-full object-contain rounded-xl" />
            </div>
            {lastPromptEnriched && (
              <div className="shrink-0 px-3 py-2 border-t border-white/5 bg-black/20">
                <p className="text-[9px] font-black uppercase tracking-widest text-purple-400 mb-0.5">Prompt enrichi par Léa</p>
                <p className="text-[9px] text-slate-400 font-mono leading-relaxed line-clamp-2">{lastPromptEnriched}</p>
              </div>
            )}
          </div>
        </>,
        document.body
      )}

      {/* ─── INTERFACE PRINCIPALE ──────────────────────────────────────── */}
      <div className="flex flex-col h-full w-full bg-[#020617] text-slate-200 overflow-hidden">

        {/* HEADER */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/5 shrink-0">
          <div className="flex items-center gap-3">
            {/* Halo animé */}
            <div className="relative">
              <div className="absolute inset-0 bg-purple-500/30 rounded-xl blur-md animate-pulse" />
              <div className="relative w-9 h-9 bg-gradient-to-br from-purple-500 via-fuchsia-500 to-pink-600 rounded-xl flex items-center justify-center shadow-[0_0_20px_rgba(168,85,247,0.5)]">
                <Camera size={15} className="text-white" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-sm font-black text-white uppercase italic tracking-tight">Studio Nano</h2>
                <span className="text-[8px] font-black uppercase tracking-widest bg-gradient-to-r from-purple-500 to-fuchsia-500 text-white px-1.5 py-0.5 rounded-md">PRO</span>
              </div>
              <p className="text-[8px] uppercase tracking-widest text-purple-400 font-bold">{engineLabel}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {userTokens !== null && (
              <div className={`flex items-center gap-1 px-2.5 py-1 rounded-full border ${userTokens < nanoCost ? 'bg-red-500/10 border-red-500/20' : 'bg-white/5 border-white/10'}`}>
                <Coins size={10} className={userTokens < nanoCost ? 'text-red-400' : 'text-yellow-400'} />
                <span className={`text-[9px] font-bold ${userTokens < nanoCost ? 'text-red-300' : 'text-slate-300'}`}>{userTokens.toLocaleString()} 🪙</span>
              </div>
            )}
            <div className="flex items-center gap-1 bg-white/5 rounded-xl p-1 border border-white/10">
              {(['generate','gallery'] as const).map(t => (
                <button key={t} onClick={() => setTab(t)}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all ${tab === t ? 'bg-gradient-to-r from-purple-600 to-fuchsia-600 text-white shadow-md' : 'text-slate-500 hover:text-slate-300'}`}>
                  {t === 'generate' ? <><Sparkles size={9}/> Générer</> : <><Images size={9}/> Galerie</>}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* ═══ GALERIE ══════════════════════════════════════════════════════ */}
        {tab === 'gallery' && (
          <div className="flex-1 overflow-y-auto p-4">
            <div className="flex items-center justify-between mb-4">
              <p className="text-[9px] font-black uppercase tracking-widest text-slate-500">
                {gallery.length} image{gallery.length !== 1 ? 's' : ''}
              </p>
              <button onClick={loadGallery}
                className="flex items-center gap-1.5 text-[9px] font-bold uppercase tracking-widest text-slate-500 hover:text-purple-400 transition-colors">
                <RefreshCw size={9} className={galleryLoading ? 'animate-spin' : ''}/> Actualiser
              </button>
            </div>
            {galleryLoading ? (
              <div className="flex items-center justify-center h-40">
                <div className="w-8 h-8 border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin"/>
              </div>
            ) : gallery.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-40 gap-3 opacity-20">
                <Images size={36} className="text-slate-500"/>
                <p className="text-xs font-black uppercase tracking-widest text-slate-400">Aucune image générée</p>
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-2">
                {gallery.map(img => (
                  <button key={img.filename} onClick={() => openModal(`${serverUrl}${img.url}`)}
                    className="group relative aspect-square rounded-xl overflow-hidden bg-white/5 border border-white/10 hover:border-purple-500/50 transition-all hover:scale-[1.02]">
                    <img src={`${serverUrl}${img.url}`} alt={img.filename}
                      className="w-full h-full object-cover" loading="lazy"/>
                    <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                      <Maximize2 size={16} className="text-white"/>
                    </div>
                    <div className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/70 to-transparent p-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <p className="text-[8px] text-slate-300 font-mono">
                        {new Date(img.date).toLocaleDateString('fr-FR', { day:'2-digit', month:'short', hour:'2-digit', minute:'2-digit' })}
                      </p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ═══ GÉNÉRATION ═══════════════════════════════════════════════════ */}
        {tab === 'generate' && (
          <div className="flex flex-1 overflow-hidden">

            {/* SIDEBAR GAUCHE */}
            <div className="w-56 shrink-0 border-r border-white/5 flex flex-col overflow-y-auto p-3 gap-4">

              {/* Modèle */}
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest text-slate-500 mb-2">Moteur IA</p>
                <div className="flex flex-col gap-1">
                  {MODELS.map(m => (
                    <button key={m.id} onClick={() => setModel(m)}
                      className={`flex items-center gap-2 px-2.5 py-2 rounded-xl border text-[11px] font-bold transition-all ${model.id === m.id ? 'bg-purple-500/20 border-purple-500/60 text-purple-200' : 'bg-white/5 border-white/8 text-slate-400 hover:bg-white/10'}`}>
                      <span className="text-sm">{m.badge}</span>
                      <div className="text-left min-w-0">
                        <div className="truncate">{m.label}</div>
                        <div className="text-[8px] opacity-60 font-mono truncate">{m.sub}</div>
                      </div>
                      {model.id === m.id && <div className="ml-auto w-1.5 h-1.5 rounded-full bg-purple-400 shrink-0"/>}
                    </button>
                  ))}
                </div>
              </div>

              {/* Format */}
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest text-slate-500 mb-2">Format</p>
                <div className="flex flex-col gap-1">
                  {FORMATS.map(f => (
                    <button key={f.id} onClick={() => setFormat(f)}
                      className={`flex items-center gap-2 px-2.5 py-2 rounded-xl border text-[11px] font-bold transition-all ${format.id === f.id ? 'bg-purple-500/20 border-purple-500/60 text-purple-200' : 'bg-white/5 border-white/8 text-slate-400 hover:bg-white/10'}`}>
                      <span className="text-sm w-4 text-center">{f.icon}</span>
                      <div className="text-left">
                        <div>{f.label}</div>
                        <div className="text-[8px] font-mono opacity-60">{f.ratio} · {model.engine==='sd15'?`${f.sdW}×${f.sdH}`:`${f.w}×${f.h}`}</div>
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              {/* Qualité */}
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest text-slate-500 mb-2">Qualité</p>
                <div className="grid grid-cols-2 gap-1">
                  {QUALITIES.map(q => (
                    <button key={q.id} onClick={() => setQuality(q)}
                      className={`flex flex-col items-center gap-0.5 px-2 py-2 rounded-xl border text-[10px] font-black transition-all ${quality.id === q.id ? 'bg-purple-500/20 border-purple-500/60 text-purple-200' : 'bg-white/5 border-white/8 text-slate-400 hover:bg-white/10'}`}>
                      <span>{q.icon}</span>
                      <span>{q.label}</span>
                      <span className="text-[8px] font-mono opacity-60 flex items-center gap-0.5"><Clock size={7}/>{q.sub}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Upscale */}
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest text-slate-500 mb-2">Upscale</p>
                <div className="flex gap-1">
                  {UPSCALES.map(u => (
                    <button key={u.id} onClick={() => setUpscale(u)}
                      className={`flex-1 flex flex-col items-center gap-0.5 py-2 rounded-xl border text-[10px] font-black transition-all ${upscale.id === u.id ? 'bg-fuchsia-500/20 border-fuchsia-500/60 text-fuchsia-200' : 'bg-white/5 border-white/8 text-slate-400 hover:bg-white/10'}`}>
                      <span>{u.label}</span>
                      <span className="text-[8px] font-mono opacity-60">{u.mult > 1 ? `${outputW}×${outputH}` : u.sub}</span>
                    </button>
                  ))}
                </div>
              </div>

            </div>

            {/* ZONE CENTRALE */}
            <div className="flex-1 flex flex-col overflow-hidden">

              {/* Aperçu / Canvas */}
              <div className="flex-1 flex items-center justify-center bg-black/50 relative overflow-hidden min-h-0">

                {/* Grille de fond */}
                <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAwIDEwIEwgNDAgMTAgTSAxMCAwIEwgMTAgNDAiIGZpbGw9Im5vbmUiIHN0cm9rZT0icmdiYSgyNTUsIDI1NSwgMjU1LCAwLjAyKSIgc3Ryb2tlLXdpZHRoPSIxIi8+PC9wYXR0ZXJuPjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI2dyaWQpIiAvPjwvc3ZnPg==')] pointer-events-none opacity-50"/>

                {isGenerating ? (
                  <div className="flex flex-col items-center gap-5 z-10">
                    {/* Spinner premium */}
                    <div className="w-20 h-20 relative flex items-center justify-center">
                      <div className="absolute inset-0 border-4 border-purple-500/10 rounded-full animate-ping"/>
                      <div className="absolute inset-0 border-4 border-transparent border-t-purple-500 border-r-fuchsia-500 rounded-full animate-spin"/>
                      <div className="absolute inset-3 border-2 border-transparent border-t-pink-400 border-l-fuchsia-400 rounded-full animate-spin" style={{animationDirection:'reverse', animationDuration:'1.5s'}}/>
                      <ImageIcon size={18} className="text-purple-400"/>
                    </div>
                    <div className="text-center">
                      <p className="text-sm font-black text-white uppercase tracking-widest">Génération en cours</p>
                      <p className="text-[10px] text-purple-400 font-mono mt-1">
                        {model.label} · {effectiveW}×{effectiveH} · {quality.steps} steps · {fmt(elapsed)}
                      </p>
                      {upscale.id !== 'none' && <p className="text-[9px] text-fuchsia-400 font-mono mt-0.5">↑ Upscale {upscale.label} → {outputW}×{outputH}</p>}
                    </div>
                    <div className="w-52 h-1 bg-white/5 rounded-full overflow-hidden">
                      <div className="h-full bg-gradient-to-r from-purple-500 via-fuchsia-500 to-pink-500 animate-pulse w-full"/>
                    </div>
                    <p className="text-[9px] text-slate-600 font-mono">Léa enrichit et forge ton image...</p>
                  </div>
                ) : error ? (
                  <div className="flex flex-col items-center gap-3 text-center px-8 z-10">
                    <AlertTriangle size={30} className="text-red-400"/>
                    <p className="text-sm font-bold text-red-300">Erreur de génération</p>
                    <p className="text-xs text-slate-500">{error}</p>
                  </div>
                ) : generatedImage ? (
                  <button onClick={() => openModal(generatedImage)}
                    className="group relative w-full h-full flex items-center justify-center p-4 cursor-pointer">
                    <img src={generatedImage} alt="Résultat"
                      className="max-h-full max-w-full rounded-2xl object-contain shadow-[0_0_60px_rgba(0,0,0,0.9)] opacity-80 group-hover:opacity-100 transition-all duration-300 group-hover:shadow-[0_0_40px_rgba(168,85,247,0.3)]"/>
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                      <div className="flex items-center gap-2 bg-black/70 backdrop-blur px-4 py-2 rounded-full border border-purple-500/30">
                        <Maximize2 size={12} className="text-purple-300"/>
                        <span className="text-[10px] font-black uppercase tracking-widest text-white">Voir en grand</span>
                      </div>
                    </div>
                  </button>
                ) : (
                  <div className="flex flex-col items-center gap-3 opacity-15 z-10">
                    <div className="relative">
                      <div className="w-16 h-16 bg-gradient-to-br from-purple-500/20 to-fuchsia-500/20 rounded-2xl flex items-center justify-center border border-white/5">
                        <Camera size={28} className="text-slate-400"/>
                      </div>
                    </div>
                    <p className="text-xs font-black uppercase tracking-widest text-slate-400">Studio prêt · {model.label}</p>
                    <p className="text-[9px] text-slate-600 font-mono">{effectiveW}×{effectiveH}{upscale.id!=='none'?` → ${outputW}×${outputH}`:''}</p>
                  </div>
                )}
              </div>

              {/* Zone prompt + boutons */}
              <div className="shrink-0 border-t border-white/5 bg-[#020617]">

                {/* Styles */}
                <div className="px-3 pt-3 pb-2 border-b border-white/5">
                  <div className="flex gap-1.5 overflow-x-auto scrollbar-none">
                    {STYLES.map(s => (
                      <button key={s.id} onClick={() => setStyle(s)}
                        className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg border text-[10px] font-bold whitespace-nowrap transition-all shrink-0 ${style.id === s.id ? 'bg-purple-500/25 border-purple-500/60 text-purple-200' : 'bg-white/5 border-white/8 text-slate-500 hover:text-slate-300 hover:bg-white/10'}`}>
                        <span>{s.emoji}</span> {s.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="p-3 flex flex-col gap-2">
                  {/* Prompt principal */}
                  <div className="relative">
                    <Wand2 size={13} className="absolute top-3 left-3 text-purple-400 pointer-events-none"/>
                    <textarea value={prompt} onChange={e => setPrompt(e.target.value)}
                      onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleGenerate(); }}}
                      placeholder="Décris ton image... Léa enrichira automatiquement le prompt ✨"
                      rows={2}
                      className="w-full bg-white/5 border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-slate-600 focus:border-purple-500/50 outline-none resize-none transition-colors"/>
                  </div>

                  {/* Options avancées */}
                  <div>
                    <button onClick={() => setShowAdvanced(v => !v)}
                      className="flex items-center gap-1 text-[9px] font-bold uppercase tracking-widest text-slate-600 hover:text-slate-400 transition-colors">
                      <Settings2 size={9}/> Options avancées {showAdvanced ? <ChevronUp size={9}/> : <ChevronDown size={9}/>}
                    </button>
                    {showAdvanced && (
                      <div className="mt-2 flex flex-col gap-2">
                        {model.engine !== 'flux' && (
                          <input value={negPrompt} onChange={e => setNegPrompt(e.target.value)}
                            placeholder="Prompt négatif : flou, déformé, watermark..."
                            className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-[11px] text-slate-400 placeholder-slate-600 focus:border-red-500/30 outline-none transition-colors"/>
                        )}
                        <div className="flex items-center gap-2">
                          <span className="text-[9px] font-black uppercase tracking-widest text-slate-600 shrink-0">Seed</span>
                          <input type="number" value={seed} onChange={e => setSeed(e.target.value)}
                            placeholder="Aléatoire"
                            className="flex-1 bg-white/5 border border-white/10 rounded-xl px-3 py-1.5 text-[11px] font-mono text-slate-400 outline-none focus:border-purple-500/50 transition-colors"/>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Bloc erreur tokens */}
                  {tokenError && (
                    <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 text-center">
                      <p className="text-red-300 text-xs font-bold mb-1">{tokenError}</p>
                      <p className="text-slate-500 text-[10px] mb-2">Recharge tes tokens pour générer une image.</p>
                      <button
                        onClick={() => window.dispatchEvent(new CustomEvent('lea-navigate', { detail: { module: 'shop' } }))}
                        className="bg-gradient-to-r from-yellow-500 to-orange-500 text-white text-[10px] font-black uppercase tracking-widest px-4 py-2 rounded-lg hover:opacity-90 active:scale-95 transition-all"
                      >
                        Recharger mes tokens 🪙
                      </button>
                    </div>
                  )}
                  {/* Bouton Générer */}
                  <button onClick={handleGenerate} disabled={!prompt.trim() || isGenerating}
                    className={`w-full py-3 rounded-xl font-black uppercase tracking-widest text-xs transition-all flex items-center justify-center gap-2 ${!prompt.trim() || isGenerating
                      ? 'bg-white/5 text-slate-600 cursor-not-allowed'
                      : 'bg-gradient-to-r from-purple-600 via-fuchsia-600 to-pink-600 text-white shadow-[0_0_24px_rgba(168,85,247,0.4)] hover:shadow-[0_0_36px_rgba(168,85,247,0.6)] hover:scale-[1.01] active:scale-95'}`}>
                    {isGenerating
                      ? <><div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"/> Génération · {fmt(elapsed)}</>
                      : <><Sparkles size={13}/> Générer · {nanoCost} 🪙 · {model.badge} {effectiveW}×{effectiveH}{upscale.id!=='none'?` → ${upscale.label}`:''}</>
                    }
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
};
