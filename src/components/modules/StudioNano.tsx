import React, { useState, useCallback, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { saveFile } from '../../lib/download';
import {
  Camera, Sparkles, Download, Clock, Image as ImageIcon,
  AlertTriangle, X, Maximize2, Minimize2, Images, RefreshCw,
  Zap, Star, Crown, ChevronDown, ChevronUp, Wand2, Settings2, Coins,
  Paintbrush, Upload, RotateCcw, Trash2, Crosshair
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

// ─── PLACEMENT GUIDÉ ─────────────────────────────────────────────────────────

function hasLocationKeyword(prompt: string): boolean {
  const p = prompt.toLowerCase();
  return ['à gauche','à droite','au centre','en haut','en bas','gauche','droite',
    'centre','milieu','haut','bas','left','right','center','top','bottom',
    'devant','derrière','beside','next to'].some(kw => p.includes(kw));
}
function isAmbiguousAddition(prompt: string): boolean {
  const p = prompt.toLowerCase();
  const hasAdd = /\b(ajoute[rz]?|rajoute[rz]?|mets?|place[rz]?|insères?|positionne[rz]?|inclure|inclus|poser|add|put|insert|include)\b/.test(p)
    || p.includes('une autre') || p.includes('un autre') || p.includes('another');
  return hasAdd && !hasLocationKeyword(prompt);
}

// ─── SNAPSHOT INTER-MONTAGES ─────────────────────────────────────────────────
// Persiste en mémoire JS (survit au démontage du composant, pas à la navigation)
const _snap: {
  tab:          'generate'|'gallery'|'retouche';
  inpaintImage: string|null; inpaintDims:  {w:number;h:number};
  originalImage:string|null; originalDims: {w:number;h:number};
  retoucheMode: 'lea'|'manuel'; inpaintPrompt: string;
} = { tab:'generate', inpaintImage:null, inpaintDims:{w:1024,h:1024},
      originalImage:null, originalDims:{w:1024,h:1024}, retoucheMode:'lea', inpaintPrompt:'' };

export const StudioNano = () => {
  const [tab, setTab]                       = useState<'generate'|'gallery'|'retouche'>(_snap.tab);
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

  // ─── ÉTAT INPAINTING + SAM ────────────────────────────────────────────────
  const maskCanvasRef   = useRef<HTMLCanvasElement|null>(null);
  const fileInputRef    = useRef<HTMLInputElement|null>(null);
  const inpaintTimerRef = useRef<ReturnType<typeof setInterval>|null>(null);

  const [inpaintImage, setInpaintImage]     = useState<string|null>(_snap.inpaintImage);
  const [inpaintDims, setInpaintDims]       = useState(_snap.inpaintDims);
  const [originalImage, setOriginalImage]   = useState<string|null>(_snap.originalImage);
  const [originalDims, setOriginalDims]     = useState(_snap.originalDims);
  const [inpaintPrompt, setInpaintPrompt]   = useState(_snap.inpaintPrompt);
  const [isInpainting, setIsInpainting]     = useState(false);
  const [inpaintResult, setInpaintResult]   = useState<string|null>(null);
  const [inpaintError, setInpaintError]     = useState<string|null>(null);
  const [inpaintElapsed, setInpaintElapsed] = useState(0);

  // Sous-mode du tab Retouche
  const [retoucheMode, setRetoucheMode] = useState<'lea'|'manuel'>(_snap.retoucheMode);

  // Mode de sélection : lasso (tracé libre) ou SAM (clic IA)
  const [maskMode,     setMaskMode]    = useState<'lasso'|'sam'>('lasso');
  const [samAvailable, setSamAvailable] = useState(false);

  // SAM click-to-segment
  const [samPoints, setSamPoints] = useState<{x:number,y:number,label:1|0}[]>([]);
  const [samMask,   setSamMask]   = useState<string|null>(null);
  const [samLoading, setSamLoading] = useState(false);
  const [samError,  setSamError]  = useState<string|null>(null);
  const [addMode,   setAddMode]   = useState<1|0>(1);

  // Lasso freehand + historique undo (1 niveau)
  const lassoPtsRef      = useRef<{x:number,y:number}[]>([]);
  const isLassoRef       = useRef(false);
  const [lassoMask,      setLassoMask]      = useState<string|null>(null);
  const [prevLassoMask,  setPrevLassoMask]  = useState<string|null>(null);
  const [prevCanvasData, setPrevCanvasData] = useState<string|null>(null);

  // Zoom au survol (desktop hover + mobile long-press)
  const [zoomedUrl,  setZoomedUrl]  = useState<string|null>(null);
  const zoomTimerRef = useRef<ReturnType<typeof setTimeout>|null>(null);

  // Lasso Dynamique — addition guidée par tracé libre
  const [placementMode,    setPlacementMode]    = useState(false);
  const [pendingPrompt,    setPendingPrompt]    = useState('');
  const [lassoCentroid,    setLassoCentroid]    = useState<{x:number;y:number}|null>(null);
  const [lastSubject,      setLastSubject]      = useState<string|null>(null);

  // Mission 2 — Droit à l'erreur (3 essais gratuits)
  const [freeRetries,       setFreeRetries]       = useState(0);
  const [isFreeRetryActive, setIsFreeRetryActive] = useState(false);
  const freeRetriesRef        = useRef(0);
  const freeRetryExhaustedRef = useRef(false); // vrai une fois les 3 essais épuisés
  useEffect(() => { freeRetriesRef.current = freeRetries; }, [freeRetries]);

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
      await saveFile(url, `lea_flux_${Date.now()}.png`);
      setDlDone(true);
      setTimeout(() => setDlDone(false), 3000);
    } catch { window.open(url, '_blank'); }
  }, []);

  const handleDownloadCurrent = useCallback(async () => {
    if (!inpaintImage) return;
    try { await saveFile(inpaintImage, `lea_retouche_${Date.now()}.png`); }
    catch { window.open(inpaintImage, '_blank'); }
  }, [inpaintImage]);

  const openModal  = (url: string) => { setDlDone(false); setModalExpanded(false); setModalUrl(url); };
  const closeModal = () => { setModalUrl(null); setModalExpanded(false); };
  const fmt = (s: number) => `${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`;

  // ─── HANDLERS INPAINTING + SAM ───────────────────────────────────────────

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const dataUrl = ev.target?.result as string;
      const img = new Image();
      img.onload = () => {
        const maxDim = 1024;
        let w = img.naturalWidth;
        let h = img.naturalHeight;
        if (w > maxDim || h > maxDim) {
          const ratio = Math.min(maxDim / w, maxDim / h);
          w = Math.round(w * ratio / 8) * 8;
          h = Math.round(h * ratio / 8) * 8;
        } else {
          w = Math.round(w / 8) * 8 || 8;
          h = Math.round(h / 8) * 8 || 8;
        }
        setInpaintDims({ w, h });
        setInpaintImage(dataUrl);
        setOriginalImage(dataUrl);
        setOriginalDims({ w, h });
        setInpaintResult(null);
        setInpaintError(null);
        setFreeRetries(0);
        setIsFreeRetryActive(false);
        freeRetriesRef.current        = 0;
        freeRetryExhaustedRef.current = false; // nouvelle image = nouveau quota
        setSamPoints([]);
        setSamMask(null);
        setSamError(null);
        setLassoMask(null);
        setPrevLassoMask(null);
        setPrevCanvasData(null);
        lassoPtsRef.current = [];
        setTimeout(() => {
          const canvas = maskCanvasRef.current;
          if (canvas) {
            canvas.width = w;
            canvas.height = h;
            canvas.getContext('2d')?.clearRect(0, 0, w, h);
          }
        }, 60);
      };
      img.src = dataUrl;
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  // ─── LASSO FREEHAND ──────────────────────────────────────────────────────

  // Fonctions stables (useCallback sans dépendances) — utilisables dans useEffect
  const drawLassoStroke = useCallback((canvas: HTMLCanvasElement) => {
    const pts = lassoPtsRef.current;
    if (pts.length < 2) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    pts.forEach(pt => ctx.lineTo(pt.x, pt.y));
    ctx.strokeStyle = '#00ffff';
    ctx.lineWidth = 2.5;
    ctx.setLineDash([8, 4]);
    ctx.stroke();
    ctx.setLineDash([]);
  }, []);

  const finalizeLasso = useCallback(() => {
    const pts = lassoPtsRef.current;
    const canvas = maskCanvasRef.current;
    if (!canvas) return;
    const w = canvas.width, h = canvas.height; // toujours sync avec l'image chargée
    if (pts.length < 5) {
      canvas.getContext('2d')?.clearRect(0, 0, w, h);
      return;
    }
    const ctx = canvas.getContext('2d')!;
    // Masque B&W pour ComfyUI inpainting
    const tmp = document.createElement('canvas');
    tmp.width = w; tmp.height = h;
    const tc = tmp.getContext('2d')!;
    tc.fillStyle = 'black'; tc.fillRect(0, 0, w, h);
    tc.fillStyle = 'white';
    tc.beginPath();
    tc.moveTo(pts[0].x, pts[0].y);
    pts.forEach(pt => tc.lineTo(pt.x, pt.y));
    tc.closePath(); tc.fill();
    setLassoMask(tmp.toDataURL('image/png'));
    // Centroïde normalisé pour positionner l'overlay de validation
    const cx = pts.reduce((s, p) => s + p.x, 0) / pts.length / w;
    const cy = pts.reduce((s, p) => s + p.y, 0) / pts.length / h;
    setLassoCentroid({ x: cx, y: cy });
    // Overlay visuel cyan
    ctx.clearRect(0, 0, w, h);
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    pts.forEach(pt => ctx.lineTo(pt.x, pt.y));
    ctx.closePath();
    ctx.fillStyle   = 'rgba(0,200,255,0.15)';
    ctx.fill();
    ctx.strokeStyle = '#00ffff';
    ctx.lineWidth   = 2.5;
    ctx.setLineDash([]);
    ctx.stroke();
  }, []);

  // Check disponibilité SAM au montage
  // ── Sync snapshot (survie au démontage pour navigation shop → retour)
  useEffect(() => { _snap.tab           = tab;           }, [tab]);
  useEffect(() => { _snap.inpaintImage  = inpaintImage;  }, [inpaintImage]);
  useEffect(() => { _snap.inpaintDims   = inpaintDims;   }, [inpaintDims]);
  useEffect(() => { _snap.originalImage = originalImage; }, [originalImage]);
  useEffect(() => { _snap.originalDims  = originalDims;  }, [originalDims]);
  useEffect(() => { _snap.retoucheMode  = retoucheMode;  }, [retoucheMode]);
  useEffect(() => { _snap.inpaintPrompt = inpaintPrompt; }, [inpaintPrompt]);

  useEffect(() => {
    fetch(`${serverUrl}/api/studio/sam/status`)
      .then(r => r.json())
      .then(d => setSamAvailable(!!d.available))
      .catch(() => setSamAvailable(false));
  }, [serverUrl]);

  // Touch non-passif pour le lasso (les événements React sont passifs par défaut)
  useEffect(() => {
    const canvas = maskCanvasRef.current;
    if (!canvas || maskMode !== 'lasso' || !inpaintImage) return;

    const toCanvasXY = (touch: Touch) => {
      const rect = canvas.getBoundingClientRect();
      return {
        x: (touch.clientX - rect.left) * (canvas.width  / rect.width),
        y: (touch.clientY - rect.top)  * (canvas.height / rect.height),
      };
    };

    const onTouchStart = (e: TouchEvent) => {
      e.preventDefault();
      isLassoRef.current = true;
      lassoPtsRef.current = [toCanvasXY(e.touches[0])];
      setLassoMask(null);
      canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
    };
    const onTouchMove = (e: TouchEvent) => {
      e.preventDefault();
      if (!isLassoRef.current) return;
      lassoPtsRef.current.push(toCanvasXY(e.touches[0]));
      drawLassoStroke(canvas);
    };
    const onTouchEnd = () => {
      if (!isLassoRef.current) return;
      isLassoRef.current = false;
      finalizeLasso();
    };

    canvas.addEventListener('touchstart', onTouchStart, { passive: false });
    canvas.addEventListener('touchmove',  onTouchMove,  { passive: false });
    canvas.addEventListener('touchend',   onTouchEnd);
    return () => {
      canvas.removeEventListener('touchstart', onTouchStart);
      canvas.removeEventListener('touchmove',  onTouchMove);
      canvas.removeEventListener('touchend',   onTouchEnd);
    };
  }, [maskMode, inpaintImage, drawLassoStroke, finalizeLasso]);

  // ─── SAM — Dessine le masque retourné par le serveur ─────────────────────
  const drawMaskOnCanvas = (maskBase64: string, points: {x:number,y:number,label:1|0}[]) => {
    const canvas = maskCanvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const img = new Image();
    img.onload = () => {
      // Extraire les pixels du masque binaire dans un canvas temporaire
      const tmp = document.createElement('canvas');
      tmp.width = canvas.width; tmp.height = canvas.height;
      const tc = tmp.getContext('2d')!;
      tc.drawImage(img, 0, 0, canvas.width, canvas.height);
      const srcData = tc.getImageData(0, 0, canvas.width, canvas.height);
      const outData = ctx.createImageData(canvas.width, canvas.height);
      const w = canvas.width, h = canvas.height;
      const src = srcData.data, out = outData.data;
      // Contour cyan + remplissage translucide
      for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
          const i = (y * w + x) * 4;
          const selected = src[i] > 128;
          const edge = selected && (
            x === 0 || x === w-1 || y === 0 || y === h-1 ||
            src[((y-1)*w+x)*4] <= 128 || src[((y+1)*w+x)*4] <= 128 ||
            src[(y*w+x-1)*4] <= 128   || src[(y*w+x+1)*4] <= 128
          );
          if (edge) {
            out[i]=0; out[i+1]=255; out[i+2]=255; out[i+3]=255;
          } else if (selected) {
            out[i]=0; out[i+1]=200; out[i+2]=255; out[i+3]=45;
          } else {
            out[i+3]=0;
          }
        }
      }
      ctx.clearRect(0, 0, w, h);
      ctx.putImageData(outData, 0, 0);
      // Dessiner les points de clic
      points.forEach(pt => {
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 7, 0, Math.PI * 2);
        ctx.fillStyle = pt.label === 1 ? 'rgba(0,255,255,0.95)' : 'rgba(255,60,60,0.95)';
        ctx.fill();
        ctx.strokeStyle = 'white';
        ctx.lineWidth = 2;
        ctx.stroke();
        // Croix au centre
        ctx.strokeStyle = 'rgba(0,0,0,0.7)';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(pt.x - 3, pt.y); ctx.lineTo(pt.x + 3, pt.y);
        ctx.moveTo(pt.x, pt.y - 3); ctx.lineTo(pt.x, pt.y + 3);
        ctx.stroke();
      });
    };
    img.src = maskBase64;
  };

  const resetSelection = () => {
    setSamPoints([]);
    setSamMask(null);
    setSamError(null);
    setLassoMask(null);
    setPrevLassoMask(null);
    setPrevCanvasData(null);
    lassoPtsRef.current = [];
    const canvas = maskCanvasRef.current;
    if (canvas) canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
  };

  // Supprime la sélection active — sauvegarde pour "Annuler"
  const suppressSelection = () => {
    const canvas = maskCanvasRef.current;
    const snapshot = canvas?.toDataURL('image/png') ?? null;
    setPrevLassoMask(lassoMask || samMask);
    setPrevCanvasData(snapshot);
    setSamPoints([]);
    setSamMask(null);
    setSamError(null);
    setLassoMask(null);
    lassoPtsRef.current = [];
    if (canvas) canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
  };

  // Restaure la dernière sélection supprimée
  const undoSelection = () => {
    if (!prevLassoMask) return;
    setLassoMask(prevLassoMask);
    setPrevLassoMask(null);
    // Redessiner le snapshot visuel sur le canvas
    if (prevCanvasData) {
      const canvas = maskCanvasRef.current;
      if (canvas) {
        const img = new Image();
        img.onload = () => canvas.getContext('2d')?.drawImage(img, 0, 0);
        img.src = prevCanvasData;
      }
    }
    setPrevCanvasData(null);
  };

  const handleSAMClick = async (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!inpaintImage || samLoading) return;
    const canvas = maskCanvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const imageX = Math.round((e.clientX - rect.left) * (inpaintDims.w / rect.width));
    const imageY = Math.round((e.clientY - rect.top)  * (inpaintDims.h / rect.height));
    const newPt: {x:number,y:number,label:1|0} = { x: imageX, y: imageY, label: addMode };
    const allPoints = [...samPoints, newPt];
    setSamPoints(allPoints);
    setSamLoading(true);
    setSamError(null);
    try {
      const imageBase64 = await getResizedImageBase64();
      const res = await fetch(`${serverUrl}/api/studio/sam/segment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: currentUser,
          image:    imageBase64,
          points:   allPoints.map(p => [p.x, p.y]),
          labels:   allPoints.map(p => p.label),
        }),
      });
      const data = await res.json();
      if (!res.ok || !data.mask) throw new Error(data.error || 'Segmentation échouée');
      setSamMask(data.mask);
      drawMaskOnCanvas(data.mask, allPoints);
    } catch (err: any) {
      setSamError(err.message);
      // Annuler le dernier point si erreur
      setSamPoints(samPoints);
    } finally {
      setSamLoading(false);
    }
  };

  const getResizedImageBase64 = (): Promise<string> => new Promise(resolve => {
    if (!inpaintImage) { resolve(''); return; }
    const img = new Image();
    img.onload = () => {
      const { w, h } = inpaintDims;
      const c = document.createElement('canvas');
      c.width = w; c.height = h;
      c.getContext('2d')?.drawImage(img, 0, 0, w, h);
      resolve(c.toDataURL('image/png'));
    };
    img.src = inpaintImage;
  });

  // Génère un masque blanc total si aucune sélection (mode Léa sans zone)
  const getMaskOrFull = (): string => {
    if (samMask) return samMask;
    if (lassoMask) return lassoMask;
    const canvas = maskCanvasRef.current;
    const w = canvas?.width || inpaintDims.w;
    const h = canvas?.height || inpaintDims.h;
    const tmp = document.createElement('canvas');
    tmp.width = w; tmp.height = h;
    const ctx = tmp.getContext('2d')!;
    ctx.fillStyle = 'white';
    ctx.fillRect(0, 0, w, h);
    return tmp.toDataURL('image/png');
  };

  // Génère un masque B&W pour une zone de placement prédéfinie
  // Cœur d'inpainting partagé (Léa + Manuel + Lasso Dynamique)
  const executeInpaint = async (promptStr: string, maskBase64: string, denoise: number, isFree: boolean, zoneId?: string) => {
    setIsInpainting(true);
    setInpaintError(null);
    setInpaintResult(null);
    setInpaintElapsed(0);
    inpaintTimerRef.current = setInterval(() => setInpaintElapsed(s => s + 1), 1000);
    try {
      const imageBase64 = await getResizedImageBase64();
      const res = await fetch(`${serverUrl}/api/studio/nano/inpaint`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: currentUser, image_data: imageBase64, mask_data: maskBase64, prompt: promptStr, denoise, free_retry: isFree, zone_id: zoneId }),
      });
      const data = await res.json();
      if (!res.ok || !data.image_url) throw new Error(data.error || 'Retouche échouée');
      setInpaintResult(`${serverUrl}${data.image_url}`);
      if (data.subject) setLastSubject(data.subject);
      if (isFree) {
        const next = Math.max(0, freeRetriesRef.current - 1);
        freeRetriesRef.current = next;
        setFreeRetries(next);
        if (next <= 0) {
          setIsFreeRetryActive(false);
          freeRetryExhaustedRef.current = true; // session épuisée, jamais réactivable
        }
      }
    } catch (e: any) {
      setInpaintError(e.message || 'Erreur de retouche');
    } finally {
      if (inpaintTimerRef.current) clearInterval(inpaintTimerRef.current);
      setIsInpainting(false);
    }
  };

  // Mode Léa — retouche guidée par texte (masque optionnel)
  const handleInpaint = async () => {
    if (!inpaintImage || !inpaintPrompt.trim() || isInpainting) return;
    const isFree = isFreeRetryActive && freeRetriesRef.current > 0;
    if (!isFree && userTokens !== null && userTokens < 30) {
      setInpaintError(`Il te faut 30 🪙 pour retoucher cette image.`);
      return;
    }
    const hasMask = !!(samMask || lassoMask);
    // Addition ambiguë sans masque → activer le Lasso Dynamique
    if (!hasMask && isAmbiguousAddition(inpaintPrompt.trim())) {
      setPendingPrompt(inpaintPrompt.trim());
      setPlacementMode(true);
      setMaskMode('lasso');
      return;
    }
    await executeInpaint(inpaintPrompt.trim(), getMaskOrFull(), hasMask ? 0.88 : 0.60, isFree);
  };

  // Valide le tracé lasso et lance la génération (consomme 1 essai gratuit si actif)
  const handleValidateAdditionLasso = async () => {
    if (!lassoMask || !pendingPrompt || isInpainting) return;
    const isFree = isFreeRetryActive && freeRetriesRef.current > 0;
    setPlacementMode(false);
    setLassoCentroid(null);
    await executeInpaint(pendingPrompt, lassoMask, 0.70, isFree);
    setPendingPrompt('');
  };

  // Efface le tracé lasso pour redessiner — GRATUIT, sans consommation de jeton
  const handleClearAdditionLasso = () => {
    setLassoMask(null);
    setLassoCentroid(null);
    lassoPtsRef.current = [];
    const canvas = maskCanvasRef.current;
    if (canvas) canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
  };

  // Annule le mode addition et revient au prompt
  const handleCancelAdditionMode = () => {
    setPlacementMode(false);
    setPendingPrompt('');
    setLassoMask(null);
    setLassoCentroid(null);
    lassoPtsRef.current = [];
    const canvas = maskCanvasRef.current;
    if (canvas) canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
  };

  // "Continuer sur ce résultat" : le résultat devient la nouvelle source ET le nouvel original
  // → "Revenir à l'original" n'annule qu'une seule étape, pas tout le chemin
  const handleContinueRetouching = () => {
    if (!inpaintResult) return;
    setInpaintImage(inpaintResult);
    setOriginalImage(inpaintResult);   // nouveau point de référence
    setOriginalDims(inpaintDims);      // même résolution
    setInpaintResult(null);
    setInpaintError(null);
    // Réinitialiser toutes les sélections
    setSamPoints([]);
    setSamMask(null);
    setSamError(null);
    setLassoMask(null);
    setPrevLassoMask(null);
    setPrevCanvasData(null);
    lassoPtsRef.current = [];
    setTimeout(() => {
      const canvas = maskCanvasRef.current;
      if (canvas) canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
    }, 60);
  };

  // "Revenir à l'original" : restaure la toute première image importée
  // activateFreeRetry=true depuis l'overlay résultat → active les 3 essais gratuits
  const handleRevertToOriginal = (activateFreeRetry = false) => {
    if (!originalImage) return;
    setInpaintImage(originalImage);
    setInpaintDims(originalDims);
    setInpaintResult(null);
    setInpaintError(null);
    setPlacementMode(false);
    setPendingPrompt('');
    setLassoCentroid(null);
    setSamPoints([]);
    setSamMask(null);
    setSamError(null);
    setLassoMask(null);
    setPrevLassoMask(null);
    setPrevCanvasData(null);
    lassoPtsRef.current = [];
    setTimeout(() => {
      const canvas = maskCanvasRef.current;
      if (canvas) {
        canvas.width  = originalDims.w;
        canvas.height = originalDims.h;
        canvas.getContext('2d')?.clearRect(0, 0, originalDims.w, originalDims.h);
      }
    }, 60);
    // N'active les essais gratuits que si aucune session n'est en cours ET que le quota n'est pas épuisé
    if (activateFreeRetry && !isFreeRetryActive && !freeRetryExhaustedRef.current) {
      fetch(`${serverUrl}/api/studio/nano/free-retry/activate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: currentUser }),
      }).catch(() => {});
      freeRetriesRef.current = 3;
      setFreeRetries(3);
      setIsFreeRetryActive(true);
    }
  };

  // Mode Manuel — gomme magique : efface l'élément et reconstruit le fond
  const handleMagicErase = async () => {
    const activeMask = samMask || lassoMask;
    if (!inpaintImage || !activeMask || isInpainting) return;
    const isFree = isFreeRetryActive && freeRetriesRef.current > 0;
    if (!isFree && userTokens !== null && userTokens < 30) {
      setInpaintError('Il te faut 30 🪙 pour utiliser la gomme magique.');
      return;
    }
    await executeInpaint(
      'empty background, seamless background fill, background reconstruction, clean inpainting, no object',
      activeMask,
      0.95,
      isFree,
    );
  };

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

      {/* ─── ZOOM AU SURVOL ───────────────────────────────────────────── */}
      {zoomedUrl && createPortal(
        <div className="fixed inset-0 z-[9984] flex items-center justify-center pointer-events-none"
          style={{ background: 'rgba(1,3,8,0.82)', backdropFilter: 'blur(8px)' }}>
          <img src={zoomedUrl}
            className="max-w-[92vw] max-h-[92vh] object-contain rounded-2xl"
            style={{ boxShadow: '0 0 80px rgba(0,0,0,0.95), 0 0 0 1px rgba(255,255,255,0.06)' }}
          />
        </div>,
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
              <button
                onClick={() => window.dispatchEvent(new CustomEvent('lea-navigate', { detail: { module: 'shop' } }))}
                title="Recharger mes tokens"
                className={`flex items-center gap-1 px-2.5 py-1 rounded-full border transition-all hover:scale-105 active:scale-95 ${userTokens < nanoCost ? 'bg-red-500/10 border-red-500/20 hover:bg-red-500/20' : 'bg-white/5 border-white/10 hover:bg-yellow-400/10 hover:border-yellow-400/30'}`}>
                <Coins size={10} className={userTokens < nanoCost ? 'text-red-400' : 'text-yellow-400'} />
                <span className={`text-[9px] font-bold ${userTokens < nanoCost ? 'text-red-300' : 'text-slate-300'}`}>{userTokens.toLocaleString()} 🪙</span>
              </button>
            )}
            <div className="flex items-center gap-1 bg-white/5 rounded-xl p-1 border border-white/10">
              {(['generate','gallery','retouche'] as const).map(t => (
                <button key={t} onClick={() => setTab(t)}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all ${tab === t ? 'bg-gradient-to-r from-purple-600 to-fuchsia-600 text-white shadow-md' : 'text-slate-500 hover:text-slate-300'}`}>
                  {t === 'generate' ? <><Sparkles size={9}/> Générer</>
                    : t === 'gallery' ? <><Images size={9}/> Galerie</>
                    : <><Paintbrush size={9}/> Retouche</>}
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

        {/* ═══ RETOUCHE / INPAINTING ════════════════════════════════════════ */}
        {tab === 'retouche' && (
          <div className="flex-1 flex flex-col overflow-hidden">

            {/* Zone principale */}
            <div className="flex-1 flex flex-col overflow-hidden bg-[#010308] relative">
              <div className="absolute inset-0 pointer-events-none" style={{ backgroundImage: 'radial-gradient(rgba(255,255,255,0.2) 1px, transparent 1px)', backgroundSize: '20px 20px', opacity: 0.1 }}/>

              {/* Input fichier caché (unique) */}
              <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handleImageUpload} />

              {/* Bouton Revenir à l'original (flottant, hors overlay résultat) */}
              {originalImage && inpaintImage !== originalImage && !inpaintResult && (
                <button onClick={() => handleRevertToOriginal()}
                  className="absolute top-2 right-2 z-20 flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-black/50 backdrop-blur-sm border border-white/15 text-white/50 text-[9px] font-black uppercase tracking-widest hover:text-white hover:border-white/30 transition-all">
                  <RotateCcw size={10}/> Original
                </button>
              )}

              {!inpaintImage ? (
                /* Zone drop/upload */
                <div className="flex-1 flex flex-col items-center justify-center gap-6 p-8 z-10">
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="flex flex-col items-center gap-4 p-10 rounded-2xl border border-dashed border-white/20 hover:border-[#00ffff]/50 hover:bg-[#00ffff]/5 transition-all duration-300 group"
                  >
                    <div className="w-16 h-16 rounded-2xl border border-white/10 flex items-center justify-center group-hover:border-[#00ffff]/30 group-hover:drop-shadow-[0_0_12px_rgba(0,255,255,0.4)] transition-all">
                      <Upload size={28} className="text-white/30 group-hover:text-[#00ffff] transition-colors"/>
                    </div>
                    <div className="text-center">
                      <p className="text-sm font-black text-white/60 group-hover:text-white uppercase tracking-widest">Importer une photo</p>
                      <p className="text-[9px] text-white/25 font-mono mt-1">PNG · JPG · WEBP · Max 1024px</p>
                    </div>
                  </button>
                  <p className="text-[9px] text-white/15 font-mono uppercase tracking-widest text-center max-w-xs">Importez une photo · tracez le contour de la zone à modifier · ou utilisez le mode SAM IA</p>
                </div>
              ) : (
                /* Canvas SAM — cliquer pour segmenter */
                <div className="flex-1 flex items-center justify-center p-3 relative overflow-hidden z-10">
                  <div
                    className="relative"
                    style={{ maxWidth: '100%', maxHeight: '100%', aspectRatio: `${inpaintDims.w} / ${inpaintDims.h}` }}
                  >
                    {/* Image source */}
                    <img
                      src={inpaintImage} alt="Source"
                      className="w-full h-full object-contain rounded-xl select-none"
                      draggable={false}
                    />
                    {/* Overlay canvas — mode lasso (tracé) ou SAM (clic) */}
                    <canvas
                      ref={maskCanvasRef}
                      width={inpaintDims.w}
                      height={inpaintDims.h}
                      className="absolute inset-0 w-full h-full rounded-xl"
                      style={{ cursor: maskMode === 'sam' && samLoading ? 'wait' : 'crosshair', touchAction: 'none' }}
                      /* SAM — clic simple */
                      onClick={maskMode === 'sam' ? handleSAMClick : undefined}
                      /* Lasso — tracé souris (desktop) */
                      onMouseDown={maskMode === 'lasso' ? e => {
                        if (!inpaintImage) return;
                        const r = e.currentTarget.getBoundingClientRect();
                        isLassoRef.current = true;
                        lassoPtsRef.current = [{ x: (e.clientX - r.left) * (e.currentTarget.width / r.width), y: (e.clientY - r.top) * (e.currentTarget.height / r.height) }];
                        e.currentTarget.getContext('2d')?.clearRect(0, 0, e.currentTarget.width, e.currentTarget.height);
                        setLassoMask(null);
                      } : undefined}
                      onMouseMove={maskMode === 'lasso' ? e => {
                        if (!isLassoRef.current) return;
                        const r = e.currentTarget.getBoundingClientRect();
                        lassoPtsRef.current.push({ x: (e.clientX - r.left) * (e.currentTarget.width / r.width), y: (e.clientY - r.top) * (e.currentTarget.height / r.height) });
                        drawLassoStroke(e.currentTarget);
                      } : undefined}
                      onMouseUp={maskMode === 'lasso' ? () => { if (isLassoRef.current) { isLassoRef.current = false; finalizeLasso(); } } : undefined}
                      onMouseLeave={maskMode === 'lasso' ? () => { if (isLassoRef.current) { isLassoRef.current = false; finalizeLasso(); } } : undefined}
                      /* Touch lasso géré par useEffect (passive:false obligatoire) */
                    />
                    {/* Overlay validation — Lasso Dynamique : apparaît dès que le tracé est fermé */}
                    {placementMode && lassoMask && lassoCentroid && (
                      <div className="absolute z-25 pointer-events-auto transform -translate-x-1/2 -translate-y-full"
                        style={{
                          left: `${Math.min(Math.max(lassoCentroid.x * 100, 18), 82)}%`,
                          top:  `${Math.min(Math.max(lassoCentroid.y * 100, 24), 84)}%`,
                        }}>
                        <div className="bg-[#010308]/90 backdrop-blur-md border border-[#ff00ff]/50 rounded-2xl px-3 py-2 flex items-center gap-2 shadow-[0_0_20px_rgba(255,0,255,0.4)]">
                          <button onClick={handleClearAdditionLasso}
                            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-white/15 text-white/50 text-[9px] font-black uppercase tracking-widest hover:bg-white/10 hover:text-white/80 transition-all active:scale-90">
                            <RotateCcw size={9}/> Refaire
                          </button>
                          <button onClick={handleValidateAdditionLasso}
                            disabled={isInpainting}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-gradient-to-r from-purple-600 to-fuchsia-600 text-white text-[9px] font-black uppercase tracking-widest hover:shadow-[0_0_12px_rgba(168,85,247,0.5)] transition-all active:scale-95 disabled:opacity-40">
                            <Sparkles size={9}/> Générer
                          </button>
                        </div>
                      </div>
                    )}
                    {/* Spinner pendant l'inférence SAM */}
                    {samLoading && (
                      <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-20">
                        <div className="flex flex-col items-center gap-2 bg-black/60 backdrop-blur-sm rounded-2xl px-5 py-4 border border-[#00ffff]/20">
                          <div className="w-5 h-5 border-2 border-[#00ffff]/30 border-t-[#00ffff] rounded-full animate-spin"/>
                          <span className="text-[9px] font-black uppercase tracking-widest text-[#00ffff]">SAM segmente…</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Résultat inpainting */}
              {inpaintResult && (
                <div className="absolute inset-0 z-30 bg-[#010308]/95 backdrop-blur-sm flex flex-col items-center justify-center gap-4 p-4">
                  <p className="text-[9px] font-black uppercase tracking-widest text-[#00ffff]">Retouche IA terminée</p>
                  {lastSubject && (
                    <p className="text-[9px] font-mono text-[#ff00ff]/60 uppercase tracking-widest -mt-2">
                      Sujet : {lastSubject}
                    </p>
                  )}
                  <img src={inpaintResult} alt="Résultat"
                    className="max-h-[55%] max-w-full rounded-xl shadow-[0_0_40px_rgba(0,0,0,0.9)] object-contain cursor-zoom-in"
                    onMouseEnter={() => setZoomedUrl(inpaintResult)}
                    onMouseLeave={() => setZoomedUrl(null)}
                    onTouchStart={() => { zoomTimerRef.current = setTimeout(() => setZoomedUrl(inpaintResult), 600); }}
                    onTouchMove={() => { if (zoomTimerRef.current) { clearTimeout(zoomTimerRef.current); zoomTimerRef.current = null; } }}
                    onTouchEnd={() => { if (zoomTimerRef.current) { clearTimeout(zoomTimerRef.current); zoomTimerRef.current = null; } setZoomedUrl(null); }}
                  />
                  {/* Télécharger */}
                  <button onClick={() => handleDownload(inpaintResult)}
                    className="flex items-center gap-2 px-4 py-2 rounded-xl border border-[#00ffff]/30 text-[#00ffff] text-[10px] font-black uppercase tracking-widest hover:bg-[#00ffff]/10 transition-all">
                    <Download size={12}/> Télécharger
                  </button>
                  <div className="flex items-center gap-2 flex-wrap justify-center">
                    {/* Continuer = résultat → nouvelle source */}
                    <button onClick={handleContinueRetouching}
                      className="flex items-center gap-2 px-3 py-2 rounded-xl border border-[#ff00ff]/40 text-[#ff00ff] text-[10px] font-black uppercase tracking-widest hover:bg-[#ff00ff]/10 transition-all">
                      <Sparkles size={11}/> Continuer sur ce résultat
                    </button>
                    {/* Revenir à l'original → active les 3 essais gratuits */}
                    {originalImage && (
                      <button onClick={() => handleRevertToOriginal(true)}
                        className="flex items-center gap-2 px-3 py-2 rounded-xl border border-white/15 text-white/40 text-[10px] font-black uppercase tracking-widest hover:bg-white/5 hover:text-white/70 transition-all">
                        <RotateCcw size={11}/> Revenir à l'original
                      </button>
                    )}
                    {/* Toujours : annuler et rester sur source actuelle */}
                    <button onClick={() => setInpaintResult(null)}
                      className="flex items-center gap-2 px-3 py-2 rounded-xl border border-white/10 text-white/30 text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">
                      <X size={11}/> Annuler
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* ─ BARRE DE CONTRÔLES ──────────────────────────────────────────── */}
            <div className="shrink-0 border-t border-white/10 bg-[#050A15]/60 backdrop-blur-2xl p-3 flex flex-col gap-2.5">

              {/* Toggle Mode Léa / Mode Manuel */}
              <div className="flex items-center gap-1 bg-white/5 rounded-xl p-0.5 border border-white/10">
                <button onClick={() => setRetoucheMode('lea')}
                  className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${retoucheMode === 'lea' ? 'bg-gradient-to-r from-purple-600/70 to-fuchsia-600/70 text-white shadow-md' : 'text-slate-500 hover:text-slate-300'}`}>
                  <Wand2 size={9}/> Mode Léa
                </button>
                <button onClick={() => setRetoucheMode('manuel')}
                  className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${retoucheMode === 'manuel' ? 'bg-[#00ffff]/15 text-[#00ffff]' : 'text-slate-500 hover:text-slate-300'}`}>
                  <Settings2 size={9}/> Mode Manuel
                </button>
              </div>

              {inpaintImage && (
                <>
                  {/* Outils lasso / SAM (partagés entre les deux modes) */}
                  <div className="flex items-center gap-2 flex-wrap">
                    {/* Lasso / SAM toggle */}
                    <div className="flex items-center rounded-lg border border-white/10 overflow-hidden shrink-0">
                      <button onClick={() => { setMaskMode('lasso'); resetSelection(); }}
                        className={`flex items-center gap-1.5 px-2.5 py-1.5 text-[10px] font-black uppercase tracking-widest transition-all ${maskMode === 'lasso' ? 'bg-[#00ffff]/10 text-[#00ffff]' : 'text-white/30 hover:text-white/60'}`}>
                        <Paintbrush size={10}/> Lasso
                      </button>
                      <div className="w-px self-stretch bg-white/10"/>
                      <button onClick={() => { setMaskMode('sam'); resetSelection(); }}
                        title={!samAvailable ? 'LeaSAM non démarré — pm2 start sam_server.py --interpreter python3' : 'Segmentation automatique IA'}
                        className={`flex items-center gap-1.5 px-2.5 py-1.5 text-[10px] font-black uppercase tracking-widest transition-all ${maskMode === 'sam' ? 'bg-[#00ffff]/10 text-[#00ffff]' : 'text-white/30 hover:text-white/60'}`}>
                        <Sparkles size={10}/> SAM IA
                        {!samAvailable && <span className="ml-0.5 text-[7px] opacity-40">○</span>}
                      </button>
                    </div>
                    {/* Contrôles SAM */}
                    {maskMode === 'sam' && (<>
                      <button onClick={() => setAddMode(1)}
                        className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg border text-[10px] font-black uppercase tracking-widest transition-all ${addMode === 1 ? 'border-[#00ffff]/50 text-[#00ffff] bg-[#00ffff]/5' : 'border-white/10 text-white/30 hover:text-white/60'}`}>
                        + Ajouter
                      </button>
                      <button onClick={() => setAddMode(0)}
                        className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg border text-[10px] font-black uppercase tracking-widest transition-all ${addMode === 0 ? 'border-red-400/50 text-red-400 bg-red-400/5' : 'border-white/10 text-white/30 hover:text-white/60'}`}>
                        − Retirer
                      </button>
                      {samPoints.length > 0 && <span className="text-[9px] text-white/30 font-mono">{samPoints.length} pt</span>}
                    </>)}
                    {/* Undo dernier reset */}
                    {prevLassoMask && (
                      <button onClick={undoSelection}
                        className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-[#00ffff]/40 text-[#00ffff] text-[10px] font-black uppercase tracking-widest hover:bg-[#00ffff]/10 transition-all animate-pulse shrink-0">
                        <RotateCcw size={10}/> Annuler
                      </button>
                    )}
                    {/* Reset sélection */}
                    {(lassoMask || samMask || samPoints.length > 0) && (
                      <button onClick={resetSelection} title="Effacer la sélection"
                        className="p-1.5 rounded-lg border border-white/10 text-white/30 hover:text-red-400 hover:border-red-400/30 transition-all shrink-0">
                        <RotateCcw size={11}/>
                      </button>
                    )}
                    {/* Télécharger l'image de travail courante */}
                    {inpaintImage && (
                      <button onClick={handleDownloadCurrent}
                        className="p-2 rounded-lg border border-white/10 text-white/40 hover:text-green-400 hover:border-green-400/30 transition-all ml-auto shrink-0" title="Télécharger l'image actuelle">
                        <Download size={12}/>
                      </button>
                    )}
                    {/* Changer image */}
                    <button onClick={() => fileInputRef.current?.click()}
                      className={`p-2 rounded-lg border border-white/10 text-white/40 hover:text-[#00ffff] hover:border-[#00ffff]/30 transition-all shrink-0 ${!inpaintImage ? 'ml-auto' : ''}`} title="Changer d'image">
                      <Upload size={12}/>
                    </button>
                  </div>

                  {/* ── MODE LÉA : prompt texte ou placement guidé ───────── */}
                  {retoucheMode === 'lea' && (
                    placementMode ? (
                      /* Lasso Dynamique — guidance tracé libre */
                      <div className="bg-[#ff00ff]/5 border border-[#ff00ff]/25 rounded-xl p-3 flex flex-col gap-2">
                        <div className="flex items-start gap-2">
                          <Crosshair size={13} className="text-[#ff00ff] shrink-0 mt-0.5"/>
                          <p className="text-[11px] text-white/80 font-bold leading-snug">
                            Entoure la zone cible sur l'image
                            <span className="block text-[9px] text-white/30 font-normal mt-0.5 font-mono">{pendingPrompt}</span>
                          </p>
                        </div>
                        {!lassoMask && (
                          <p className="text-[9px] text-[#ff00ff]/40 italic leading-relaxed">
                            Trace un contour libre sur l'image · valide ensuite pour générer
                          </p>
                        )}
                        <button onClick={handleCancelAdditionMode}
                          className="self-start text-[9px] text-white/30 hover:text-white/60 transition-colors mt-0.5">
                          ← Annuler
                        </button>
                      </div>
                    ) : (
                      <div className="relative">
                        <Wand2 size={13} className="absolute top-3 left-3 text-[#ff00ff]/50 pointer-events-none"/>
                        <textarea value={inpaintPrompt} onChange={e => setInpaintPrompt(e.target.value)}
                          placeholder="Décris la retouche à Léa… changer le vêtement, modifier l'arrière-plan, ajouter un élément, supprimer un objet…"
                          rows={2}
                          className="w-full bg-white/5 border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-slate-600 focus:border-[#ff00ff]/40 outline-none resize-none transition-colors"/>
                      </div>
                    )
                  )}
                </>
              )}

              {/* Badge essais gratuits */}
              {isFreeRetryActive && freeRetries > 0 && (
                <div className="flex items-center gap-2 bg-green-500/10 border border-green-500/20 rounded-xl px-3 py-2">
                  <span className="text-base leading-none">🎟️</span>
                  <p className="text-[10px] text-green-300 font-black uppercase tracking-widest">{freeRetries}/3 essais gratuits restants</p>
                </div>
              )}

              {/* Erreurs */}
              {(inpaintError || samError) && (
                <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2">
                  <AlertTriangle size={12} className="text-red-400 shrink-0"/>
                  <p className="text-[10px] text-red-300">{inpaintError || samError}</p>
                </div>
              )}

              {/* ── BOUTON ACTION ────────────────────────────────────────────── */}
              {retoucheMode === 'lea' ? (
                /* Mode Léa — "Valider la retouche" */
                <button
                  onClick={inpaintImage ? handleInpaint : () => fileInputRef.current?.click()}
                  disabled={isInpainting || (!!inpaintImage && !inpaintPrompt.trim())}
                  className={`w-full py-3 rounded-xl font-black uppercase tracking-widest text-xs flex items-center justify-center gap-2 transition-all duration-300 ${
                    isInpainting || (!!inpaintImage && !inpaintPrompt.trim())
                      ? 'bg-transparent border border-white/10 text-white/20 cursor-not-allowed'
                      : !inpaintImage
                      ? 'bg-transparent border border-[#00ffff]/40 text-[#00ffff] hover:bg-[#00ffff]/10 hover:drop-shadow-[0_0_12px_rgba(0,255,255,0.5)]'
                      : 'bg-transparent border border-[#ff00ff] text-[#ff00ff] hover:bg-[#ff00ff]/20 hover:drop-shadow-[0_0_15px_rgba(255,0,255,0.8)]'
                  }`}
                >
                  {isInpainting
                    ? <><div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"/> Léa retouche · {fmt(inpaintElapsed)}</>
                    : !inpaintImage
                    ? <><Upload size={13}/> Importer une photo</>
                    : !inpaintPrompt.trim()
                    ? <><Wand2 size={13}/> Décris la retouche à Léa…</>
                    : isFreeRetryActive && freeRetries > 0
                    ? <><Sparkles size={13}/> Valider la retouche · <span className="text-green-300">Gratuit 🎟️</span></>
                    : <><Sparkles size={13}/> Valider la retouche · 30 🪙</>
                  }
                </button>
              ) : (
                /* Mode Manuel — "Supprimer de l'image" (gomme magique) */
                <button
                  onClick={inpaintImage ? handleMagicErase : () => fileInputRef.current?.click()}
                  disabled={isInpainting || (!!inpaintImage && !(samMask || lassoMask))}
                  className={`w-full py-3 rounded-xl font-black uppercase tracking-widest text-xs flex items-center justify-center gap-2 transition-all duration-300 ${
                    isInpainting || (!!inpaintImage && !(samMask || lassoMask))
                      ? 'bg-transparent border border-white/10 text-white/20 cursor-not-allowed'
                      : !inpaintImage
                      ? 'bg-transparent border border-[#00ffff]/40 text-[#00ffff] hover:bg-[#00ffff]/10 hover:drop-shadow-[0_0_12px_rgba(0,255,255,0.5)]'
                      : 'bg-transparent border border-red-500 text-red-400 hover:bg-red-500/15 hover:drop-shadow-[0_0_15px_rgba(239,68,68,0.7)]'
                  }`}
                >
                  {isInpainting
                    ? <><div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"/> Suppression en cours · {fmt(inpaintElapsed)}</>
                    : !inpaintImage
                    ? <><Upload size={13}/> Importer une photo</>
                    : !(samMask || lassoMask)
                    ? maskMode === 'lasso'
                      ? <><Paintbrush size={13}/> Entourez la zone à supprimer</>
                      : <><Sparkles size={13}/> Cliquez sur l'élément à supprimer</>
                    : isFreeRetryActive && freeRetries > 0
                    ? <><Trash2 size={13}/> Supprimer de l'image · <span className="text-green-300">Gratuit 🎟️</span></>
                    : <><Trash2 size={13}/> Supprimer de l'image · 30 🪙</>
                  }
                </button>
              )}
            </div>
          </div>
        )}

        {/* ═══ GÉNÉRATION ═══════════════════════════════════════════════════ */}
        {tab === 'generate' && (
          <div className="flex flex-1 overflow-hidden">

            {/* SIDEBAR GAUCHE */}
            <div className="w-56 shrink-0 border-r border-white/5 bg-[#020617]/50 backdrop-blur-xl flex flex-col overflow-y-auto p-3 gap-4">

              {/* Modèle */}
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest text-slate-500 mb-2">Moteur IA</p>
                <div className="flex flex-col gap-1">
                  {MODELS.map(m => (
                    <button key={m.id} onClick={() => setModel(m)}
                      className={`flex items-center gap-2 px-2.5 py-2 text-[11px] font-bold transition-all ${model.id === m.id ? 'border-l-2 border-[#00ffff] bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-r-lg rounded-l-none' : 'rounded-xl border border-white/5 bg-transparent text-white/50 hover:text-white hover:border-white/20'}`}>
                      <span className="text-sm">{m.badge}</span>
                      <div className="text-left min-w-0">
                        <div className="truncate">{m.label}</div>
                        <div className="text-[8px] opacity-60 font-mono truncate">{m.sub}</div>
                      </div>
                      {model.id === m.id && <div className="ml-auto w-1.5 h-1.5 rounded-full bg-[#00ffff] shrink-0"/>}
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
                      className={`flex items-center gap-2 px-2.5 py-2 text-[11px] font-bold transition-all ${format.id === f.id ? 'border-l-2 border-[#00ffff] bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-r-lg rounded-l-none' : 'rounded-xl border border-white/5 bg-transparent text-white/50 hover:text-white hover:border-white/20'}`}>
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
                      className={`flex flex-col items-center gap-0.5 px-2 py-2 text-[10px] font-black transition-all ${quality.id === q.id ? 'border-l-2 border-[#00ffff] bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-r-md rounded-l-none' : 'rounded-xl border border-white/5 bg-transparent text-white/50 hover:text-white hover:border-white/20'}`}>
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
                      className={`flex-1 flex flex-col items-center gap-0.5 py-2 text-[10px] font-black transition-all ${upscale.id === u.id ? 'border-l-2 border-[#00ffff] bg-gradient-to-r from-[#00ffff]/10 to-transparent text-[#00ffff] rounded-r-md rounded-l-none' : 'rounded-xl border border-white/5 bg-transparent text-white/50 hover:text-white hover:border-white/20'}`}>
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
              <div className="flex-1 flex items-center justify-center bg-[#010308] relative overflow-hidden min-h-0">

                {/* Grille de fond — points */}
                <div className="absolute inset-0 pointer-events-none" style={{ backgroundImage: 'radial-gradient(rgba(255,255,255,0.2) 1px, transparent 1px)', backgroundSize: '20px 20px', opacity: 0.12 }}/>

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
              <div className="shrink-0 border-t border-white/10 bg-[#050A15]/60 backdrop-blur-2xl">

                {/* Styles */}
                <div className="px-3 pt-3 pb-2 border-b border-white/5">
                  <div className="flex gap-1.5 overflow-x-auto scrollbar-none">
                    {STYLES.map(s => (
                      <button key={s.id} onClick={() => setStyle(s)}
                        className={`px-3 py-1 rounded-full border text-[10px] uppercase tracking-wider whitespace-nowrap transition-all shrink-0 ${style.id === s.id ? 'border-[#00ffff]/50 text-[#00ffff] bg-[#00ffff]/5' : 'border-white/10 text-white/40 hover:border-[#00ffff]/50 hover:text-white/70'}`}>
                        {s.label}
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
                    className={`w-full py-3 rounded-xl font-black uppercase tracking-widest text-xs flex items-center justify-center gap-2 ${!prompt.trim() || isGenerating
                      ? 'bg-transparent border border-white/10 text-white/20 cursor-not-allowed'
                      : 'bg-transparent border border-[#ff00ff] text-[#ff00ff] hover:bg-[#ff00ff]/20 hover:drop-shadow-[0_0_15px_rgba(255,0,255,0.8)] transition-all duration-300'}`}>
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
