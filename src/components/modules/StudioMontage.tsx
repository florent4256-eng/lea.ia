import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import {
  Play, Pause, Plus, Trash2, Download, X, Check, Scissors, Zap, Type,
  Layers, Palette, Music, Volume2, VolumeX, Upload, Loader2, Film,
  ZoomIn, ZoomOut, Bold, Italic, AlignCenter, AlignLeft, AlignRight,
  SkipBack, SkipForward, SlidersHorizontal, ChevronUp, ChevronDown,
  Wand2, Lock, Unlock, Eye, EyeOff, FlipHorizontal, RotateCcw,
  Mic, Sun, Settings, Share2, Grid, Star, RefreshCw, Move, Image as ImageIcon
} from 'lucide-react';
import { saveFile, downloadFile } from '../../lib/download';
import { ALL_TRANSITIONS, TRANSITION_CATEGORIES, getTransition, GLTransitionRenderer, requestTransitionThumb, getThumbUrlSync } from '../../lib/glTransitions';
import { useConfirmToast } from '../../hooks/useConfirmToast';

const S = () => (window as any).LEA_SERVER_URL || '';
const cu = () => { try { return localStorage.getItem('lea_currentUser') || 'invite'; } catch { return 'invite'; } };

// ═══ CONFIG ════════════════════════════════════════════════════════════════════
const PX = 80;
const ACCENT = '#fe2c55';   // Rouge CapCut
const BG0 = '#000000';
const BG1 = '#0d0d0d';
const BG2 = '#141414';
const BG3 = '#1c1c1e';
const BG4 = '#252525';

const SUB_ROW_H = 56; // hauteur d'une sous-ligne vidéo ou photo
const TRACK_H = { video: SUB_ROW_H * 2 + 4, audio: 56, text: 44, effect: 44 };

// TRANSITIONS importé depuis glTransitions.ts (ALL_TRANSITIONS)

const TRACK_CFG = {
  video:   { label: '🎬 Vidéo',   clip: '#1a2744', txt: '#93c5fd' },
  audio:   { label: '🎵 Audio',   clip: '#0f2a1e', txt: '#6ee7b7' },
  text:    { label: '💬 Texte',   clip: '#2a1a08', txt: '#fcd34d' },
  effect:  { label: '✨ Effets',  clip: '#2a0a1a', txt: '#f9a8d4' },
};

const FILTERS = [
  { id:'none',        label:'Original',  css:'none' },
  { id:'vivid',       label:'Vivid',     css:'saturate(1.8) contrast(1.1)' },
  { id:'warm',        label:'Warm',      css:'sepia(0.3) saturate(1.4) brightness(1.05)' },
  { id:'cool',        label:'Cool',      css:'hue-rotate(30deg) saturate(0.85)' },
  { id:'noir',        label:'Noir',      css:'grayscale(1) contrast(1.2)' },
  { id:'vintage',     label:'Vintage',   css:'sepia(0.5) contrast(0.9) brightness(0.9)' },
  { id:'fade',        label:'Fade',      css:'brightness(1.15) saturate(0.65)' },
  { id:'chrome',      label:'Chrome',    css:'contrast(1.4) saturate(0.8) brightness(1.1)' },
  { id:'dream',       label:'Dream',     css:'brightness(1.1) saturate(1.3) hue-rotate(-10deg)' },
  { id:'neon',        label:'Néon',      css:'saturate(2.5) contrast(1.2) hue-rotate(10deg)' },
  { id:'shadow',      label:'Shadow',    css:'brightness(0.75) contrast(1.3)' },
  { id:'film',        label:'Film',      css:'sepia(0.2) contrast(1.1) brightness(0.95)' },
  { id:'teal_orange', label:'Teal & Or.',css:'saturate(1.4) hue-rotate(-15deg) contrast(1.15)' },
  { id:'matte',       label:'Matte',     css:'brightness(1.08) saturate(0.75) contrast(0.85)' },
  { id:'cyberpunk',   label:'Cyberpunk', css:'saturate(2.2) contrast(1.3) hue-rotate(-30deg)' },
  { id:'golden',      label:'Golden Hr.',css:'sepia(0.4) saturate(1.6) brightness(1.1)' },
  { id:'bleach',      label:'Bleach',    css:'saturate(0.5) contrast(1.5) brightness(1.1)' },
  { id:'vhs',         label:'VHS',       css:'saturate(1.3) hue-rotate(5deg) contrast(0.95)' },
];

const MASKS = [
  { id:'none',     label:'Aucun',    css:'none' },
  { id:'circle',   label:'Cercle',   css:'circle(45% at 50% 50%)' },
  { id:'ellipse',  label:'Ellipse',  css:'ellipse(45% 30% at 50% 50%)' },
  { id:'rounded',  label:'Arrondi',  css:'inset(5% 8% 5% 8% round 20%)' },
  { id:'diamond',  label:'Losange',  css:'polygon(50% 0%,100% 50%,50% 100%,0% 50%)' },
  { id:'star',     label:'Étoile',   css:'polygon(50% 0%,61% 35%,98% 35%,68% 57%,79% 91%,50% 70%,21% 91%,32% 57%,2% 35%,39% 35%)' },
  { id:'cinema',   label:'Cinéma',   css:'inset(15% 0% 15% 0%)' },
  { id:'portrait', label:'Portrait', css:'inset(0% 22% 0% 22% round 5px)' },
];

const KF_PROPS = [
  { prop:'kfOpacity',  label:'Opacité',    min:0,   max:1,   step:0.01, dflt:1 },
  { prop:'kfPosX',     label:'Position X', min:0,   max:100, step:0.5,  dflt:50 },
  { prop:'kfPosY',     label:'Position Y', min:0,   max:100, step:0.5,  dflt:50 },
  { prop:'kfScaleX',   label:'Échelle X',  min:0.1, max:3,   step:0.01, dflt:1 },
  { prop:'kfScaleY',   label:'Échelle Y',  min:0.1, max:3,   step:0.01, dflt:1 },
  { prop:'kfRotation', label:'Rotation',   min:-180,max:180, step:1,    dflt:0 },
];

const EFFECTS_CAT = {
  'Tendance': ['Glitch','RGB Split','VHS','Grain'],
  'Lumière':  ['Flash','Stroboscope','Bloom','Éblouissement'],
  '3D':       ['Zoom Flou','Rotation','Perspective','Parallaxe'],
  'Basiques': ['Secousse','Rebond','Miroir','Fondu N&B'],
};

const TEXT_FONTS = ['Arial','Impact','Georgia','Courier New','Verdana','Tahoma'];
const TEXT_ANIMS = ['Aucune','Fondu','Glisse Haut','Glisse Bas','Rebond','Machine à Écrire'];
const SPEEDS     = [0.1,0.25,0.5,0.75,1,1.25,1.5,2,3,4,8,16];
const RESOLUTIONS= ['480p','720p','1080p','2K','4K'];
const FRAMERATES = [24,25,30,50,60];
const ASPECT_RATIOS = [
  { id:'16:9', label:'16:9', desc:'YouTube / Desktop', w:16, h:9 },
  { id:'9:16', label:'9:16', desc:'TikTok / Reels',   w:9,  h:16 },
  { id:'1:1',  label:'1:1',  desc:'Instagram Carré',   w:1,  h:1  },
  { id:'4:3',  label:'4:3',  desc:'Classique',          w:4,  h:3  },
];

// SVG sociaux
const TikTokSVG = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M19.59 6.69a4.83 4.83 0 01-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 01-2.88 2.5 2.89 2.89 0 01-2.89-2.89 2.89 2.89 0 012.89-2.89c.28 0 .54.04.79.1V9.01a6.34 6.34 0 106.34 6.34V8.86a8.18 8.18 0 004.77 1.52V6.91a4.85 4.85 0 01-1-.22z"/></svg>;
const IgSVG    = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="2" y="2" width="20" height="20" rx="5"/><path d="M16 11.37A4 4 0 1112.63 8 4 4 0 0116 11.37z"/><line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/></svg>;
const YtSVG    = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M22.54 6.42a2.78 2.78 0 00-1.95-1.96C18.88 4 12 4 12 4s-6.88 0-8.59.46a2.78 2.78 0 00-1.95 1.96A29 29 0 001 12a29 29 0 00.46 5.58A2.78 2.78 0 003.41 19.54C5.12 20 12 20 12 20s6.88 0 8.59-.46a2.78 2.78 0 001.95-1.96A29 29 0 0023 12a29 29 0 00-.46-5.58zM9.75 15.02V8.98L15.5 12l-5.75 3.02z"/></svg>;
const FbSVG    = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z"/></svg>;

// ═══ HELPERS ══════════════════════════════════════════════════════════════════
function genId() { return Math.random().toString(36).slice(2, 9); }

function fmtTimecode(s, fps = 30) {
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = Math.floor(s % 60);
  const fr = Math.floor((s % 1) * fps);
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}:${String(fr).padStart(2,'0')}`;
}

function fmtDur(s) {
  const m = Math.floor(s / 60);
  const sec = (s % 60).toFixed(1).padStart(4,'0');
  return `${m}:${sec}`;
}

function fakeWaveform(n = 120) {
  return Array.from({length:n}, (_,i) => {
    const base = Math.sin(i * 0.3) * 0.3 + 0.5;
    return Math.max(0.05, Math.min(1, base + (Math.random() - 0.5) * 0.4));
  });
}

function getKFValue(kfs, relT, defaultVal) {
  if (!kfs || kfs.length === 0) return defaultVal;
  const sorted = [...kfs].sort((a, b) => a.t - b.t);
  if (sorted.length === 1) return sorted[0].value;
  if (relT <= sorted[0].t) return sorted[0].value;
  if (relT >= sorted[sorted.length - 1].t) return sorted[sorted.length - 1].value;
  for (let i = 0; i < sorted.length - 1; i++) {
    if (relT >= sorted[i].t && relT <= sorted[i + 1].t) {
      const p = (relT - sorted[i].t) / (sorted[i + 1].t - sorted[i].t);
      return sorted[i].value + (sorted[i + 1].value - sorted[i].value) * p;
    }
  }
  return defaultVal;
}

// Réduit les clips audio pour qu'ils ne dépassent pas la durée totale de la piste vidéo
function syncAudioToVideo(nt, allMedia) {
  const vt = nt.find(t => t.type === 'video');
  if (!vt || vt.clips.length === 0) return nt;
  const videoDur = vt.clips.reduce((max, c) => Math.max(max, c.endTime), 0);
  if (videoDur <= 0) return nt;
  return nt.map(t => {
    if (t.type !== 'audio') return t;
    return {
      ...t,
      clips: t.clips.map(c => {
        if (c.endTime <= videoDur) return c;
        const media = allMedia.find(m => m.id === c.mediaId);
        const srcMax = media?.duration || 9999;
        const newEnd = Math.min(videoDur, c.startTime + srcMax - (c.mediaStart || 0));
        if (newEnd <= c.startTime + 0.5) return c;
        return { ...c, endTime: newEnd, mediaEnd: Math.min(srcMax, (c.mediaStart || 0) + (newEnd - c.startTime)) };
      })
    };
  });
}

function makeClip(media, startTime) {
  const dur = Math.max(0.5, media.duration || 5);
  return {
    id: genId(), mediaId: media.id, startTime, endTime: startTime + dur,
    mediaStart: 0, mediaEnd: dur, speed: 1, volume: 1,
    isMuted: false, fadeIn: 0, fadeOut: 0, filters: ['none'],
    colorAdj: { brightness: 0, contrast: 1, saturation: 1, hue: 0 },
    animation: { in: 'none', out: 'none' },
    kenBurns: { enabled: false, type: 'zoom-in' },
    cropLeft: 0, cropRight: 0, cropTop: 0, cropBottom: 0,
    transitionIn: { type: 'none', duration: 0.5 },
    posX: 50, posY: 50, scaleX: 1, scaleY: 1, rotation: 0, flipH: false,
    mask: 'none',
    keyframes: {},
  };
}

function makeTextClip(startTime = 0, endTime = 5) {
  return {
    id: genId(), mediaId: null, startTime, endTime,
    mediaStart: 0, mediaEnd: endTime - startTime, speed: 1, volume: 0,
    isMuted: true, fadeIn: 0, fadeOut: 0, filters: ['none'],
    colorAdj: { brightness: 0, contrast: 1, saturation: 1, hue: 0 },
    animation: { in: 'none', out: 'none' },
    textStyle: {
      content: 'Mon texte', font: 'Arial', fontSize: 40, color: '#ffffff',
      bgColor: '#000000', bgOpacity: 0, bold: false, italic: false,
      align: 'center', outlineColor: '#000000', outlineWidth: 0,
      animation: 'Aucune', x: 50, y: 80,
    },
  };
}

function getFilterCss(filters, adj) {
  const filt = FILTERS.find(f => filters && filters.includes(f.id));
  const parts = [];
  if (filt && filt.css !== 'none') parts.push(filt.css);
  if (adj) {
    if (adj.brightness !== 0)  parts.push(`brightness(${1 + adj.brightness / 100})`);
    if (adj.contrast !== 1)    parts.push(`contrast(${adj.contrast})`);
    if (adj.saturation !== 1)  parts.push(`saturate(${adj.saturation})`);
    if (adj.hue !== 0)         parts.push(`hue-rotate(${adj.hue}deg)`);
  }
  return parts.join(' ') || 'none';
}

function getClipAnimStyle(clip, currentTime) {
  if (!clip) return {};
  const animDur = 0.5;
  const clipDur = clip.endTime - clip.startTime;
  const t = currentTime - clip.startTime;
  const relT = Math.max(0, t);
  let opacity: number | undefined;
  const transforms: string[] = [];

  // Keyframes interpolation
  const kfs = clip.keyframes || {};
  const kfOpacity  = kfs.kfOpacity  && kfs.kfOpacity.length  ? getKFValue(kfs.kfOpacity,  relT, null) : null;
  const kfRotation = kfs.kfRotation && kfs.kfRotation.length ? getKFValue(kfs.kfRotation, relT, null) : null;
  const kfScaleX   = kfs.kfScaleX   && kfs.kfScaleX.length   ? getKFValue(kfs.kfScaleX,   relT, null) : null;
  const kfScaleY   = kfs.kfScaleY   && kfs.kfScaleY.length   ? getKFValue(kfs.kfScaleY,   relT, null) : null;
  const kfPosX     = kfs.kfPosX     && kfs.kfPosX.length     ? getKFValue(kfs.kfPosX,     relT, null) : null;
  const kfPosY     = kfs.kfPosY     && kfs.kfPosY.length     ? getKFValue(kfs.kfPosY,     relT, null) : null;

  if (clip.flipH) transforms.push('scaleX(-1)');

  // Rotation : KF prioritaire
  const effectiveRotation = kfRotation !== null ? kfRotation : (clip.rotation || 0);
  if (effectiveRotation) transforms.push(`rotate(${effectiveRotation}deg)`);

  // Scale : KF prioritaire sur kenBurns
  if (kfScaleX !== null || kfScaleY !== null) {
    const sx = kfScaleX !== null ? kfScaleX : (clip.scaleX || 1);
    const sy = kfScaleY !== null ? kfScaleY : (clip.scaleY || 1);
    if (sx !== 1 || sy !== 1) transforms.push(`scale(${sx},${sy})`);
  } else if (clip.kenBurns?.enabled) {
    const p = Math.max(0, Math.min(1, t / Math.max(0.001, clipDur)));
    const type = clip.kenBurns.type || 'zoom-in';
    if (type === 'zoom-in')    transforms.push(`scale(${1 + 0.2 * p})`);
    else if (type === 'zoom-out') transforms.push(`scale(${1.2 - 0.2 * p})`);
    else if (type === 'left-right') transforms.push(`scale(1.12) translateX(${-5 + 10 * p}%)`);
    else if (type === 'right-left') transforms.push(`scale(1.12) translateX(${5 - 10 * p}%)`);
  }

  // Opacité : KF prioritaire sur animations
  if (kfOpacity !== null) {
    opacity = kfOpacity;
  } else {
    // Anim entrée
    if (clip.animation?.in && clip.animation.in !== 'none' && t >= 0 && t < animDur) {
      const p = t / animDur;
      if (clip.animation.in === 'fade') opacity = p;
      else if (clip.animation.in === 'slide_left')  transforms.push(`translateX(${(1-p)*-100}%)`);
      else if (clip.animation.in === 'slide_right') transforms.push(`translateX(${(1-p)*100}%)`);
      else if (clip.animation.in === 'slide_up')    transforms.push(`translateY(${(1-p)*-100}%)`);
      else if (clip.animation.in === 'zoom_in')     transforms.push(`scale(${0.5 + p * 0.5})`);
      else if (clip.animation.in === 'bounce')      transforms.push(`scale(${1 + Math.sin(p*Math.PI)*0.15})`);
    }
    // Anim sortie
    if (clip.animation?.out && clip.animation.out !== 'none' && t > clipDur - animDur) {
      const p = (clipDur - t) / animDur;
      if (clip.animation.out === 'fade') opacity = p;
      else if (clip.animation.out === 'slide_left')  transforms.push(`translateX(${(1-p)*100}%)`);
      else if (clip.animation.out === 'slide_right') transforms.push(`translateX(${(1-p)*-100}%)`);
      else if (clip.animation.out === 'slide_up')    transforms.push(`translateY(${(1-p)*100}%)`);
      else if (clip.animation.out === 'zoom_out')    transforms.push(`scale(${Math.max(0, p)})`);
    }
  }

  const extra: any = {};
  if (kfPosX !== null) extra['--kf-x'] = kfPosX;
  if (kfPosY !== null) extra['--kf-y'] = kfPosY;

  return {
    opacity: opacity ?? 1,
    transform: transforms.length > 0 ? transforms.join(' ') : 'none',
    transition: 'none',
    ...extra,
  };
}

function rulerMarks(totalDur, zoom) {
  const step = zoom >= 3 ? 0.5 : zoom >= 1.5 ? 1 : zoom >= 0.7 ? 2 : zoom >= 0.4 ? 5 : zoom >= 0.15 ? 10 : zoom >= 0.07 ? 30 : 60;
  const minor = step / 5;
  const marks = [];
  for (let t = 0; t <= totalDur + step; t = Math.round((t + minor) * 10000) / 10000) {
    marks.push({ t, major: Math.abs(Math.round(t / step) * step - t) < 0.0001 });
  }
  return marks;
}

// ═══ WAVEFORM CANVAS ══════════════════════════════════════════════════════════
function WaveformCanvas({ data, width, height, color }) {
  const ref = useRef(null);
  useEffect(() => {
    const c = ref.current; if (!c || !data || !data.length) return;
    const ctx = c.getContext('2d'); if (!ctx) return;
    c.width = Math.max(1, width); c.height = Math.max(1, height);
    ctx.clearRect(0, 0, c.width, c.height);
    ctx.fillStyle = color || 'rgba(255,255,255,0.5)';
    const bw = c.width / data.length;
    data.forEach((amp, i) => {
      const bh = Math.max(2, amp * c.height);
      ctx.fillRect(i * bw, (c.height - bh) / 2, Math.max(0.5, bw - 0.5), bh);
    });
  }, [data, width, height, color]);
  return <canvas ref={ref} style={{ width: Math.max(1,width), height: Math.max(1,height), display:'block' }} />;
}

// ═══ VIDEO THUMB — extraction via FFmpeg serveur (sans CORS) ═════════════════
function VideoClipThumb({ url, style }: { url: string; style?: any }) {
  const [thumb, setThumb] = React.useState('');
  React.useEffect(() => {
    if (!url) return;
    let cancelled = false;
    const srvUrl = (window as any).LEA_SERVER_URL || '';
    const filename = url.split('/').pop() || '';
    const user = (() => { try { return localStorage.getItem('lea_currentUser') || 'invite'; } catch { return 'invite'; } })();
    fetch(`${srvUrl}/api/studio/montage/thumbnail`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ filename, time: 0.5, username: user }),
    })
      .then(r => r.ok ? r.json() : null)
      .then(d => { if (!cancelled && d?.dataUrl) setThumb(d.dataUrl); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [url]);
  if (thumb) return <img src={thumb} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', ...style }} />;
  return <div style={{ width:'100%', height:'100%', display:'flex', alignItems:'center', justifyContent:'center', ...style }}><Film size={14} color="rgba(255,255,255,0.2)"/></div>;
}

// ═══ LOTTIE THUMBNAIL — lecture lottie-web en canvas miniature ════════════════
function LottieThumb({ url }: { url: string }) {
  const divRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const div = divRef.current; if (!div) return;
    let anim: any = null;
    let cancelled = false;
    const srvUrl = (window as any).LEA_SERVER_URL || '';
    import('lottie-web').then((mod: any) => {
      if (cancelled || !divRef.current) return;
      anim = (mod.default || mod).loadAnimation({
        container: divRef.current,
        renderer: 'canvas',
        loop: true,
        autoplay: true,
        path: srvUrl + url,
      });
      anim.setSpeed(0.5);
    }).catch(() => {});
    return () => { cancelled = true; if (anim) { try { anim.destroy(); } catch {} } };
  }, [url]);
  return <div ref={divRef} style={{ width:'100%', height:'100%' }}/>;
}

// ═══ WEBM THUMBNAIL — première frame extraite par canvas ══════════════════════
function WebmThumb({ url }: { url: string }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [thumb, setThumb] = useState('');
  useEffect(() => {
    const v = videoRef.current; if (!v) return;
    const onSeeked = () => {
      try {
        const cv = document.createElement('canvas');
        cv.width = 80; cv.height = 52;
        cv.getContext('2d')?.drawImage(v, 0, 0, 80, 52);
        setThumb(cv.toDataURL('image/jpeg', 0.8));
      } catch {}
    };
    v.addEventListener('seeked', onSeeked);
    v.preload = 'metadata';
    v.muted = true;
    const onMeta = () => { v.currentTime = 0.1; };
    v.addEventListener('loadedmetadata', onMeta);
    return () => { v.removeEventListener('seeked', onSeeked); v.removeEventListener('loadedmetadata', onMeta); };
  }, [url]);
  const srvUrl = (window as any).LEA_SERVER_URL || '';
  return (
    <>
      <video ref={videoRef} src={srvUrl + url} style={{ display:'none' }} preload="metadata" muted crossOrigin="anonymous"/>
      {thumb
        ? <img src={thumb} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
        : <video src={srvUrl + url} autoPlay muted loop playsInline style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
      }
    </>
  );
}

// ═══ LAZY VIDEO THUMB — ne charge le <video> que quand il est visible ═════════
function LazyVideoThumb({ url, duration }: { url: string; duration?: number }) {
  const [visible, setVisible] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = rootRef.current; if (!el) return;
    const obs = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting) { setVisible(true); obs.disconnect(); }
    }, { threshold: 0.1 });
    obs.observe(el);
    return () => obs.disconnect();
  }, []);
  const srvUrl = (window as any).LEA_SERVER_URL || '';
  return (
    <div ref={rootRef} style={{ width:'100%', height:'100%' }}>
      {visible ? (
        <video src={srvUrl + url} preload="metadata" muted playsInline
          style={{ width:'100%', height:'100%', objectFit:'cover' }}
          onLoadedMetadata={(e: any) => { try { e.target.currentTime = 0.5; } catch {} }} />
      ) : (
        <div style={{ width:'100%', height:'100%', display:'flex', alignItems:'center', justifyContent:'center', backgroundColor:'#141414' }}>
          <Film size={14} color="rgba(255,255,255,0.15)" />
        </div>
      )}
    </div>
  );
}

// ═══ TRANSITION PREVIEW — rendu WebGL réel ════════════════════════════════════
function TransitionPreviewCanvas({ transId }: { transId: string }) {
  const cvRef = useRef(null);
  const rfRef = useRef(0);
  const t0Ref = useRef(0);
  useEffect(() => {
    const cv = cvRef.current;
    if (!cv) return;
    const rd = new GLTransitionRenderer();
    if (!rd.init(cv)) return;
    function mkGrad(c1: string, c2: string) {
      const c = document.createElement('canvas');
      c.width = c.height = 128;
      const ctx = c.getContext('2d')!;
      const g = ctx.createLinearGradient(0, 0, 128, 128);
      g.addColorStop(0, c1); g.addColorStop(1, c2);
      ctx.fillStyle = g; ctx.fillRect(0, 0, 128, 128);
      return c;
    }
    rd.uploadFrame('from', mkGrad('#1e3a6e', '#0a1b3b'));
    rd.uploadFrame('to',   mkGrad('#6e1e1e', '#3b0a0a'));
    t0Ref.current = performance.now();
    const animate = (now: number) => {
      const p = ((now - t0Ref.current) % 2400) / 2400;
      const progress = p < 0.5 ? p * 2 : (1 - p) * 2;
      try { rd.render(transId, progress); } catch {}
      rfRef.current = requestAnimationFrame(animate);
    };
    rfRef.current = requestAnimationFrame(animate);
    return () => { cancelAnimationFrame(rfRef.current); rd.destroy(); };
  }, [transId]);
  return <canvas ref={cvRef} width={300} height={100} style={{ width:'100%', height:'100%', display:'block', borderRadius:6 }} />;
}

// ═══ CLIP BLOCK ═══════════════════════════════════════════════════════════════
function ClipBlock({ clip, track, media, zoom, isSelected, onMoveStart, onTrimStart, onSelect, onDelete }) {
  const w = Math.max(16, (clip.endTime - clip.startTime) * PX * zoom);
  const left = clip.startTime * PX * zoom;
  const cfg = TRACK_CFG[track.type] || TRACK_CFG.video;
  const th = TRACK_H[track.type] || 64;

  // Piste vidéo : sous-ligne du haut = vidéos, sous-ligne du bas = photos
  const isPhotoOnVideoTrack = track.type === 'video' && media?.type === 'image';
  const clipTop  = track.type === 'video' ? (isPhotoOnVideoTrack ? SUB_ROW_H + 4 : 4) : 4;
  const clipH    = track.type === 'video' ? SUB_ROW_H - 8 : th - 8;
  const clipBg   = isPhotoOnVideoTrack ? '#1a2d3a' : cfg.clip;
  const clipTxt  = isPhotoOnVideoTrack ? '#7dd3fc' : cfg.txt;

  return (
    <div
      style={{
        position:'absolute', left, width: w, top: clipTop, height: clipH,
        backgroundColor: clipBg, borderRadius:5,
        border: isSelected ? `2px solid ${ACCENT}` : '1px solid rgba(255,255,255,0.15)',
        overflow:'hidden', cursor:'grab', userSelect:'none', touchAction:'none',
        boxShadow: isSelected ? `0 0 0 1px ${ACCENT}, 0 0 14px rgba(254,44,85,0.35)` : 'none',
      }}
      onPointerDown={e => { e.stopPropagation(); onMoveStart(e, clip.id, track.id); }}
      onClick={e => { e.stopPropagation(); onSelect(clip.id, track.id); }}
    >
      {/* Trim left */}
      <div style={{ position:'absolute', left:0, top:0, bottom:0, width:8, cursor:'ew-resize', backgroundColor:'rgba(255,255,255,0.2)', zIndex:3 }}
        onPointerDown={e => { e.stopPropagation(); onTrimStart(e, clip.id, track.id, 'left'); }} />

      {/* Content */}
      <div style={{ position:'absolute', left:8, right:8, top:0, bottom:0, overflow:'hidden' }}>
        {track.type === 'video' && media?.url && (
          media.type === 'image'
            ? <img src={S()+media.url} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', opacity:0.75 }}/>
            : <VideoClipThumb url={S()+media.url} style={{ opacity:0.75 }} />
        )}
        {track.type === 'audio' && (
          <div style={{ width:'100%', height:'100%', display:'flex', alignItems:'center' }}>
            <WaveformCanvas data={media?.waveformData || fakeWaveform(80)} width={Math.max(1, w - 16)} height={th - 16} color="rgba(167,243,208,0.8)" />
          </div>
        )}
        {track.type === 'text' && clip.textStyle && (
          <div style={{ padding:'4px 6px', fontSize:11, color:cfg.txt, fontWeight:'bold', whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis', lineHeight:'1.2' }}>
            {clip.textStyle.content || 'Texte'}
          </div>
        )}
{track.type === 'effect' && (
          <div style={{ padding:'4px 6px', fontSize:10, color:cfg.txt }}>✨ Effet</div>
        )}
      </div>

      {/* Label */}
      {w > 50 && (
        <div style={{ position:'absolute', bottom:2, left:10, right:10, fontSize:9, color:'rgba(255,255,255,0.6)', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', pointerEvents:'none', zIndex:2 }}>
          {clip.textStyle ? clip.textStyle.content : (media?.name?.slice(0,20) || 'Clip')}
        </div>
      )}
      {track.type === 'video' && isPhotoOnVideoTrack && w > 24 && (
        <div style={{ position:'absolute', top:2, left:10, fontSize:9, color:'rgba(255,255,255,0.4)', zIndex:2 }}>🖼</div>
      )}
      {clip.speed !== 1 && w > 40 && (
        <div style={{ position:'absolute', top:2, right:10, fontSize:9, backgroundColor:'rgba(0,0,0,0.6)', padding:'1px 4px', borderRadius:3, color:ACCENT, zIndex:2 }}>{clip.speed}x</div>
      )}
      {clip.isMuted && <div style={{ position:'absolute', top:2, left:10, fontSize:9, color:'#f87171', zIndex:2 }}>🔇</div>}
      {/* KF indicator */}
      {clip.keyframes && Object.values(clip.keyframes).some((kfs:any) => kfs?.length > 0) && (
        <div style={{ position:'absolute', top:2, right: isSelected ? 32 : 10, fontSize:9, color:ACCENT, zIndex:2 }}>◆</div>
      )}
      {/* Mask indicator */}
      {clip.mask && clip.mask !== 'none' && (
        <div style={{ position:'absolute', top:2, right: isSelected ? 46 : 24, fontSize:8, color:'rgba(167,243,208,0.8)', zIndex:2 }}>M</div>
      )}

      {/* Trim right */}
      <div style={{ position:'absolute', right:0, top:0, bottom:0, width:8, cursor:'ew-resize', backgroundColor:'rgba(255,255,255,0.2)', zIndex:3 }}
        onPointerDown={e => { e.stopPropagation(); onTrimStart(e, clip.id, track.id, 'right'); }} />

      {/* Delete button — visible seulement quand sélectionné */}
      {isSelected && (
        <button
          onPointerDown={e => e.stopPropagation()}
          onClick={e => { e.stopPropagation(); onDelete(clip.id, track.id); }}
          style={{ position:'absolute', top:3, right:12, width:18, height:18, borderRadius:'50%', backgroundColor:'#ef4444', border:'none', color:'white', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', fontSize:10, fontWeight:'bold', zIndex:10, lineHeight:1 }}>
          ✕
        </button>
      )}
    </div>
  );
}

// ═══ COMPOSANT PRINCIPAL ══════════════════════════════════════════════════════
export function StudioMontage({ onClose }: { onClose?: () => void } = {}) {
  const [mediaItems, setMediaItems] = useState([]);
  const [tracks, setTracks] = useState([
    { id:'video-0',   type:'video',   clips:[], locked:false, muted:false },
    { id:'audio-0',   type:'audio',   clips:[], locked:false, muted:false },
    { id:'text-0',    type:'text',    clips:[], locked:false, muted:false },
    { id:'effect-0',  type:'effect',  clips:[], locked:false, muted:false },
  ]);
  const [transitionPicker, setTransitionPicker] = useState<{trackId:string, clipId:string, x:number, y:number}|null>(null);
  const [trPopupPos, setTrPopupPos] = useState<{x:number,y:number}|null>(null);
  const [trSearch, setTrSearch] = useState('');
  const [trCat, setTrCat] = useState('Basiques');
  const [selectedClipId, setSelectedClipId]   = useState(null);
  const [selectedTrackId, setSelectedTrackId] = useState(null);
  const [currentTime, setCurrentTime] = useState(0);
  const [isPlaying, setIsPlaying]     = useState(false);
  const [zoom, setZoom]               = useState(1);
  const [project, setProject]         = useState({ name:'Montage sans titre', resolution:'1080p', fps:30, aspectRatio:'16:9', bgColor:'#000000' });
  const [activeMediaTab, setActiveMediaTab] = useState('videos');
  const [stickerAssets, setStickerAssets]   = useState([]);
  const [effectAssets,  setEffectAssets]    = useState([]);
  const [assetsLoading, setAssetsLoading]   = useState(null);
  const [favorites, setFavorites] = useState<any[]>(() => {
    try { return JSON.parse(localStorage.getItem('lea_montage_favorites') || '[]'); } catch { return []; }
  });
  const [uploading, setUploading]     = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [exporting, setExporting]     = useState(false);
  const [exportedUrl, setExportedUrl] = useState(null);
  const [history, setHistory]         = useState([]);
  const [historyIdx, setHistoryIdx]   = useState(-1);
  const [isDesktop, setIsDesktop]     = useState(window.innerWidth >= 768);
  const [mobilePanel, setMobilePanel] = useState(null);
  const [gallery, setGallery]         = useState([]);
  const [showGallery, setShowGallery] = useState(false);
  const [showExport, setShowExport]   = useState(false);
  const [activeInspTab, setActiveInspTab] = useState('properties');
  const [sonSubTool, setSonSubTool]       = useState<string|null>(null);
  const [audioFxBusy, setAudioFxBusy]    = useState(false);
  const [audioFxMsg, setAudioFxMsg]       = useState<string|null>(null);
  // Voix off
  const [isRecording, setIsRecording]     = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  // Auto-cut
  const [beatLoading, setBeatLoading]     = useState(false);
  // Clip busy (stabilise / chromakey)
  const [clipFxBusy, setClipFxBusy]       = useState(false);
  const [clipFxMsg, setClipFxMsg]         = useState<string|null>(null);
  // Templates modal
  const [showTemplates, setShowTemplates] = useState(false);

  const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();

  const videoRef           = useRef(null);
  const videoSrcRef        = useRef('');
  const videoRef2          = useRef(null);
  const videoRef2SrcRef    = useRef('');
  const transImgRef1       = useRef<HTMLImageElement>(null);
  const transImgRef2       = useRef<HTMLImageElement>(null);
  const transImg1SrcRef    = useRef('');
  const transImg2SrcRef    = useRef('');
  const fromFrozenForClipRef  = useRef<string | null>(null); // id du clip c1 dont la texture "from" est gelée en VRAM
  const activeVideoClipRef   = useRef<any>(null);  // ref live → clip vidéo actif (pour RAF, jamais stale)
  const activeClipIdRef      = useRef<string | null>(null); // id du clip que v1 est en train de jouer (pour détecter changement même fichier)
  const activeAudioClipRef   = useRef<any>(null);  // idem audio
  const isPlayingRef         = useRef(false);       // ref live → isPlaying
  const tracksRef            = useRef<any[]>([]);   // ref live → tracks
  const mediaItemsRef        = useRef<any[]>([]);   // ref live → mediaItems
  const currentTimeRef       = useRef(0);           // source de vérité du RAF (60fps, pas de re-render)
  const audioRef        = useRef<HTMLAudioElement>(null);
  const audioSrcRef     = useRef('');
  const glCanvasRef     = useRef(null);
  const glRendererRef   = useRef(null);
  const videoInRef      = useRef(null);
  const photoInRef      = useRef(null);
  const audioInRef      = useRef(null);
  const tlRef           = useRef(null);
  const rafRef          = useRef(0);
  const dragRef         = useRef(null);
  const playStart       = useRef({ rt:0, ct:0 });
  const pinchRef        = useRef<{dist:number, zoom:number}|null>(null);
  const zoomRef              = useRef(1);
  const saveTimerRef         = useRef<any>(null);
  const mediaRecorderRef     = useRef<any>(null);
  const recordedChunksRef    = useRef<Blob[]>([]);
  const recordTimerRef       = useRef<any>(null);

  // ── Computed ──────────────────────────────────────────────────────────────
  const lastClipEnd = useMemo(() => {
    let max = 0;
    tracks.forEach(t => t.clips.forEach(c => { if (c.endTime > max) max = c.endTime; }));
    return max;
  }, [tracks]);

  const totalDur = useMemo(() => Math.max(10, lastClipEnd + 5), [lastClipEnd]);

  const selectedClip = useMemo(() => {
    if (!selectedClipId || !selectedTrackId) return null;
    return tracks.find(t => t.id === selectedTrackId)?.clips.find(c => c.id === selectedClipId) || null;
  }, [tracks, selectedClipId, selectedTrackId]);

  const selectedMedia = useMemo(() => {
    if (!selectedClip?.mediaId) return null;
    return mediaItems.find(m => m.id === selectedClip.mediaId) || null;
  }, [selectedClip, mediaItems]);

  const activeVideoClip = useMemo(() => {
    const vt = tracks.find(t => t.type === 'video');
    return vt?.clips.find(c => c.startTime <= currentTime && c.endTime > currentTime) || null;
  }, [tracks, currentTime]);

  const activeTextClips = useMemo(() => {
    const tt = tracks.find(t => t.type === 'text');
    return tt?.clips.filter(c => c.startTime <= currentTime && c.endTime > currentTime) || [];
  }, [tracks, currentTime]);

  const activeAudioClip = useMemo(() => {
    const at = tracks.find(t => t.type === 'audio');
    return at?.clips.find(c => c.startTime <= currentTime && c.endTime > currentTime) || null;
  }, [tracks, currentTime]);

  const mediaByType = useMemo(() => ({
    videos: mediaItems.filter(m => m.type === 'video'),
    photos: mediaItems.filter(m => m.type === 'image'),
    audio:  mediaItems.filter(m => m.type === 'audio'),
  }), [mediaItems]);

  // ── Effects ───────────────────────────────────────────────────────────────
  useEffect(() => {
    const onResize = () => setIsDesktop(window.innerWidth >= 768);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  // Refs live — mises à jour à chaque changement, accessibles dans le RAF sans closure stale
  useEffect(() => { activeVideoClipRef.current = activeVideoClip; }, [activeVideoClip]);
  useEffect(() => { activeAudioClipRef.current = activeAudioClip; }, [activeAudioClip]);
  useEffect(() => { isPlayingRef.current = isPlaying; }, [isPlaying]);
  useEffect(() => { tracksRef.current = tracks; }, [tracks]);
  useEffect(() => { mediaItemsRef.current = mediaItems; }, [mediaItems]);

  useEffect(() => {
    cancelAnimationFrame(rafRef.current);
    if (!isPlaying) return;

    const stopAt = lastClipEnd > 0 ? lastClipEnd : totalDur;
    let startCt = currentTimeRef.current;
    if (lastClipEnd > 0 && startCt >= stopAt - 0.05) { startCt = 0; currentTimeRef.current = 0; setCurrentTime(0); }
    playStart.current = { rt: performance.now(), ct: startCt };

    // ── Sync vidéo 60fps — Frame-Perfect ──────────────────────────────────────
    const doVideoSync = (nt: number) => {
      const v1 = videoRef.current; if (!v1) return;
      const vt: any = tracksRef.current.find((t: any) => t.type === 'video'); if (!vt) return;
      const sorted = [...vt.clips].sort((a: any, b: any) => a.startTime - b.startTime);
      const ac: any = sorted.find((c: any) => c.startTime <= nt && c.endTime > nt) || null;
      const m: any = ac ? mediaItemsRef.current.find((mi: any) => mi.id === ac.mediaId) : null;
      activeVideoClipRef.current = ac;

      if (!ac || !m || m.type !== 'video') {
        if (!v1.paused) v1.pause();
        // Pré-chargement : sur clip photo, charger la prochaine vidéo dans v1
        if (ac) {
          const idx = sorted.indexOf(ac);
          for (let j = idx + 1; j < sorted.length; j++) {
            const nc: any = sorted[j]; const nm: any = mediaItemsRef.current.find((mi: any) => mi.id === nc.mediaId);
            if (nm?.type === 'video') { const url = S() + nm.url; if (videoSrcRef.current !== url) { videoSrcRef.current = url; v1.src = url; v1.load(); } break; }
          }
        }
        return;
      }

      const url = S() + m.url;
      const clipChanged = activeClipIdRef.current !== ac.id;

      if (videoSrcRef.current !== url) {
        videoSrcRef.current = url; activeClipIdRef.current = ac.id;
        v1.src = url; v1.load(); v1.playbackRate = ac.speed || 1; return;
      }
      if (clipChanged) {
        activeClipIdRef.current = ac.id;
        const target = Math.max(0, (nt - ac.startTime) * (ac.speed || 1) + (ac.mediaStart || 0));
        v1.currentTime = target; v1.playbackRate = ac.speed || 1;
        if (v1.paused) v1.play().catch(() => {});
        return;
      }

      v1.playbackRate = ac.speed || 1;

      // Pré-buffer 2s : si prochain clip est une coupe directe (sans transition), le charger dans v2
      const acIdx = sorted.indexOf(ac);
      if (acIdx >= 0 && acIdx < sorted.length - 1) {
        const nc: any = sorted[acIdx + 1];
        const gap = nc.startTime - nt;
        if (gap > 0 && gap <= 2 && (!nc.transitionIn || nc.transitionIn.type === 'none')) {
          const nm: any = mediaItemsRef.current.find((mi: any) => mi.id === nc.mediaId);
          if (nm?.type === 'video') {
            const v2 = videoRef2.current; const nu = S() + nm.url;
            if (v2 && videoRef2SrcRef.current !== nu) { videoRef2SrcRef.current = nu; v2.src = nu; v2.load(); }
          }
        }
      }

      // Frame-skip : si v1 joue mais a > 500ms de retard → seek immédiat (saute les frames inutiles)
      if (!v1.paused && !v1.seeking && v1.readyState >= 3) {
        const v1Pos = ac.startTime + (v1.currentTime - (ac.mediaStart || 0)) / (ac.speed || 1);
        if (nt - v1Pos > 0.5) {
          v1.currentTime = Math.max(0, (nt - ac.startTime) * (ac.speed || 1) + (ac.mediaStart || 0));
        }
        return; // v1 tourne correctement, ne pas interférer
      }

      // v1 en pause et prête → démarrer depuis la bonne position
      if (v1.paused && !v1.seeking && v1.readyState >= 1) {
        v1.currentTime = Math.max(0, (nt - ac.startTime) * (ac.speed || 1) + (ac.mediaStart || 0));
        v1.play().catch(() => {});
      }
    };

    // ── Sync audio 30fps — hors React ────────────────────────────────────────
    const doAudioSync = (nt: number) => {
      const a = audioRef.current; if (!a) return;
      const audioTrack: any = tracksRef.current.find((t: any) => t.type === 'audio');
      const ac: any = audioTrack?.clips.find((c: any) => c.startTime <= nt && c.endTime > nt) || null;
      if (!ac || audioTrack?.muted) { if (!a.paused) a.pause(); return; }
      const m: any = mediaItemsRef.current.find((mi: any) => mi.id === ac.mediaId);
      if (!m?.url) { if (!a.paused) a.pause(); return; }
      const url = S() + m.url;
      if (audioSrcRef.current !== url) {
        audioSrcRef.current = url; a.src = url; a.load();
        a.addEventListener('canplay', () => {
          const t = Math.max(0, (currentTimeRef.current - ac.startTime) * (ac.speed || 1) + (ac.mediaStart || 0));
          a.currentTime = t; a.play().catch(() => {});
        }, { once: true });
        return;
      }
      a.volume = ac.isMuted ? 0 : Math.min(1, ac.volume ?? 1);
      a.playbackRate = ac.speed || 1;
      const target = Math.max(0, (nt - ac.startTime) * (ac.speed || 1) + (ac.mediaStart || 0));
      if (a.paused) { if (Math.abs(a.currentTime - target) > 0.3) a.currentTime = target; a.play().catch(() => {}); }
      else if (Math.abs(a.currentTime - target) > 2) a.currentTime = target;
    };

    // ── Sync GL transitions 60fps — hors React ───────────────────────────────
    const doGLSync = (nt: number) => {
      const cvs = glCanvasRef.current; if (!cvs || !glRendererRef.current) { if (cvs) cvs.style.display = 'none'; return; }
      const vt: any = tracksRef.current.find((t: any) => t.type === 'video');
      if (!vt) { cvs.style.display = 'none'; return; }
      const sorted = [...vt.clips].sort((a: any, b: any) => a.startTime - b.startTime);
      const gl = glRendererRef.current;
      let active = false;
      for (let i = 0; i < sorted.length - 1; i++) {
        const c1: any = sorted[i]; const c2: any = sorted[i + 1];
        const tr = c2.transitionIn; if (!tr || tr.type === 'none') continue;
        const dur = tr.duration || 0.5; const zoneStart = c1.endTime - dur;
        const rawProgress = (nt - zoneStart) / dur;
        if (nt >= zoneStart - 4 && nt <= c1.endTime + dur) {
          const toM: any = mediaItemsRef.current.find((m: any) => m.id === c2.mediaId);
          if (toM?.type === 'video') { const v2 = videoRef2.current; if (v2) { const u = S() + toM.url; if (videoRef2SrcRef.current !== u) { videoRef2SrcRef.current = u; v2.src = u; v2.load(); } } }
          else if (toM?.type === 'image') { const img2 = transImgRef2.current; if (img2) { const u = S() + toM.url; if (transImg2SrcRef.current !== u) { transImg2SrcRef.current = u; img2.src = u; } } }
          const frM: any = mediaItemsRef.current.find((m: any) => m.id === c1.mediaId);
          if (frM?.type === 'image') { const img1 = transImgRef1.current; if (img1) { const u = S() + frM.url; if (transImg1SrcRef.current !== u) { transImg1SrcRef.current = u; img1.src = u; } } }
        }
        if (rawProgress < 0) continue;
        const progress = Math.max(0, Math.min(1, rawProgress));
        const fromMedia: any = mediaItemsRef.current.find((m: any) => m.id === c1.mediaId);
        const toMedia: any   = mediaItemsRef.current.find((m: any) => m.id === c2.mediaId);
        const v2 = videoRef2.current;
        if (v2 && toMedia?.type === 'video' && rawProgress <= 1) {
          const t2 = Math.max(0, (nt - zoneStart) * (c2.speed || 1) + (c2.mediaStart || 0));
          if (!isPlayingRef.current) { v2.pause(); if (Math.abs(v2.currentTime - t2) > 0.1) v2.currentTime = t2; }
          else if (v2.paused) { v2.currentTime = t2; v2.play().catch(() => {}); }
        }
        let fromOk = false;
        if (fromFrozenForClipRef.current === c1.id) { fromOk = true; }
        else if (rawProgress <= 1) {
          if (fromMedia?.type === 'image') { const img1 = transImgRef1.current; if (img1?.complete && img1.naturalWidth > 0) { try { gl.uploadFrame('from', img1); fromFrozenForClipRef.current = c1.id; fromOk = true; } catch {} } }
          else { const v1 = videoRef.current; if (v1 && v1.readyState >= 2) { try { gl.uploadFrame('from', v1); fromFrozenForClipRef.current = c1.id; fromOk = true; } catch {} } }
        }
        let toOk = false;
        if (toMedia?.type === 'image') { const img2 = transImgRef2.current; if (img2?.complete && img2.naturalWidth > 0) { try { gl.uploadFrame('to', img2); toOk = true; } catch {} } }
        else { if (v2 && v2.readyState >= 2) { try { gl.uploadFrame('to', v2); toOk = true; } catch {} } }
        if (rawProgress <= 1) {
          if (fromOk && toOk)  { try { gl.render(tr.type, progress); active = true; } catch {} }
          else if (fromOk)     { try { gl.render(tr.type, 0); active = true; } catch {} }
          else if (toOk)       { try { gl.render(tr.type, 1); active = true; } catch {} }
          break;
        } else {
          const v1 = videoRef.current;
          const v1Loading = toMedia?.type === 'video' && v1 && v1.readyState < 2;
          if (toOk && v1Loading && nt < c1.endTime + 3) { try { gl.render(tr.type, 1); active = true; } catch {} break; }
        }
      }
      cvs.style.display = active ? 'block' : 'none';
      if (!active) { fromFrozenForClipRef.current = null; if (videoRef2.current) videoRef2.current.pause(); }
    };

    // ── checkSync 500ms : relance v1 si elle stalle pendant la lecture ──────────
    const checkSyncInterval = setInterval(() => {
      if (!isPlayingRef.current) return;
      const v1 = videoRef.current; const avc = activeVideoClipRef.current;
      if (!v1 || !avc) return;
      const m: any = mediaItemsRef.current.find((mi: any) => mi.id === avc.mediaId);
      if (!m || m.type !== 'video') return;
      if (videoSrcRef.current !== S() + m.url) return;
      if (v1.paused && !v1.seeking && v1.readyState >= 2) {
        const target = Math.max(0, (currentTimeRef.current - avc.startTime) * (avc.speed || 1) + (avc.mediaStart || 0));
        v1.currentTime = target;
        v1.play().catch(() => {});
      }
    }, 500);

    // ── RAF loop — audio comme horloge maître ────────────────────────────────────
    let frameN = 0;
    const tick = (now: number) => {
      let nt: number;
      // Lire la position audio si disponible → horloge maître
      const a = audioRef.current;
      const audioTrack = tracksRef.current.find((t: any) => t.type === 'audio');
      const aac: any = audioTrack?.clips.find((c: any) =>
        c.startTime <= currentTimeRef.current + 0.3 && c.endTime > currentTimeRef.current
      ) || null;
      const audioM: any = aac ? mediaItemsRef.current.find((mi: any) => mi.id === aac.mediaId) : null;
      const audioUrlOk = audioM && a && audioSrcRef.current === S() + audioM.url;
      if (a && !a.paused && !a.seeking && a.readyState >= 2 && aac && audioUrlOk && !audioTrack?.muted) {
        // Audio joue : on cale le timecode sur lui
        nt = aac.startTime + (a.currentTime - (aac.mediaStart || 0)) / (aac.speed || 1);
        playStart.current = { rt: now, ct: nt }; // reanchrage de la wall clock
      } else {
        // Pas d'audio (ou photo/clip sans audio) → wall clock
        nt = playStart.current.ct + (now - playStart.current.rt) / 1000;
      }
      const st = lastClipEnd > 0 ? lastClipEnd : totalDur;
      if (nt >= st) { setIsPlaying(false); setCurrentTime(st); currentTimeRef.current = st; return; }
      currentTimeRef.current = nt;
      // 60fps : vidéo + GL (hors React)
      doVideoSync(nt); doGLSync(nt);
      // 30fps : audio + React UI (timecode, timeline)
      frameN++;
      if (frameN % 2 === 0) {
        doAudioSync(nt); setCurrentTime(nt);
        if (tlRef.current) {
          const phX = nt * PX * zoomRef.current;
          const { scrollLeft, clientWidth } = tlRef.current;
          if (phX < scrollLeft + 20 || phX > scrollLeft + clientWidth - 20) tlRef.current.scrollLeft = phX - clientWidth / 2;
        }
      }
      rafRef.current = requestAnimationFrame(tick);
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => { cancelAnimationFrame(rafRef.current); clearInterval(checkSyncInterval); };
  }, [isPlaying, totalDur, lastClipEnd]);

  // ── Vidéo — mode PAUSE uniquement (seek + src) ────────────────────────────
  // Pendant la lecture, le RAF gère tout via doVideoSync (hors React).
  useEffect(() => {
    if (isPlaying) return; // RAF gère la lecture
    const v = videoRef.current; if (!v) return;
    if (!activeVideoClip) { v.pause(); return; }
    const m = mediaItems.find(mi => mi.id === activeVideoClip.mediaId);
    if (!m || m.type !== 'video') { v.pause(); return; }
    const url = S() + m.url;
    if (videoSrcRef.current !== url) { videoSrcRef.current = url; v.src = url; v.load(); return; }
    const target = (currentTime - activeVideoClip.startTime) * (activeVideoClip.speed || 1) + (activeVideoClip.mediaStart || 0);
    if (Math.abs(v.currentTime - target) > 0.15) v.currentTime = Math.max(0, target);
    v.playbackRate = activeVideoClip.speed || 1;
    v.pause();
  }, [activeVideoClip, isPlaying, currentTime, mediaItems]);

  // ── Audio — mode PAUSE uniquement ─────────────────────────────────────────
  // Pendant la lecture, le RAF gère tout via doAudioSync (hors React).
  useEffect(() => {
    if (isPlaying) return; // RAF gère la lecture
    const a = audioRef.current; if (!a) return;
    const audioTrack = tracks.find(t => t.type === 'audio');
    if (!activeAudioClip || audioTrack?.muted) { a.pause(); return; }
    const m = mediaItems.find(mi => mi.id === activeAudioClip.mediaId);
    if (!m?.url) { a.pause(); return; }
    const url = S() + m.url;
    if (audioSrcRef.current !== url) { audioSrcRef.current = url; a.src = url; a.load(); return; }
    a.volume = activeAudioClip.isMuted ? 0 : Math.min(1, activeAudioClip.volume ?? 1);
    a.playbackRate = activeAudioClip.speed || 1;
    const target = Math.max(0, (currentTime - activeAudioClip.startTime) * (activeAudioClip.speed || 1) + (activeAudioClip.mediaStart || 0));
    a.pause();
    if (Math.abs(a.currentTime - target) > 0.2) a.currentTime = target;
  }, [activeAudioClip, isPlaying, currentTime, mediaItems, tracks]);

  // ── Sync zoomRef ─────────────────────────────────────────────────────────────
  useEffect(() => { zoomRef.current = zoom; }, [zoom]);

  // ── Chargement projet (localStorage) ─────────────────────────────────────────
  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(`lea_montage_${cu()}`) || 'null');
      if (saved?.version === 1 && Array.isArray(saved.tracks) && Array.isArray(saved.mediaItems)) {
        setTracks(saved.tracks);
        setMediaItems(saved.mediaItems);
        if (saved.project) setProject(p => ({ ...p, ...saved.project }));
      }
    } catch {}
  }, []); // eslint-disable-line

  // ── Sauvegarde projet (debounced) ─────────────────────────────────────────────
  useEffect(() => {
    clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(() => {
      try {
        localStorage.setItem(`lea_montage_${cu()}`, JSON.stringify({ version: 1, tracks, mediaItems, project }));
      } catch {}
    }, 1500);
  }, [tracks, mediaItems, project]);

  // ── Zoom molette souris sur timeline ─────────────────────────────────────────
  useEffect(() => {
    const tl = tlRef.current; if (!tl) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      if (e.shiftKey) { tl.scrollLeft += e.deltaY * 2; return; }
      const factor = e.deltaY < 0 ? 1.12 : 1 / 1.12;
      const rect = tl.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const timeAtMouse = (tl.scrollLeft + mouseX) / (PX * zoomRef.current);
      setZoom(prev => {
        const next = Math.max(0.05, Math.min(8, prev * factor));
        requestAnimationFrame(() => {
          if (tlRef.current) tlRef.current.scrollLeft = Math.max(0, timeAtMouse * PX * next - mouseX);
        });
        return next;
      });
    };
    tl.addEventListener('wheel', onWheel, { passive: false });
    return () => tl.removeEventListener('wheel', onWheel);
  }, []); // zoomRef évite la dépendance sur zoom

  // ── GL init ──────────────────────────────────────────────────────────────────
  useEffect(() => {
    const c = glCanvasRef.current; if (!c) return;
    c.width = 1280; c.height = 720;
    const r = new GLTransitionRenderer();
    if (r.init(c)) glRendererRef.current = r;
    return () => { if (glRendererRef.current) { glRendererRef.current.destroy(); glRendererRef.current = null; } };
  }, []);

  // ── GL pause-scrub : preview de la transition quand le player est en pause ───
  // Pendant la lecture, le RAF (doGLSync) gère les transitions à 60fps hors React.
  useEffect(() => {
    if (isPlaying) return; // RAF gère en lecture
    fromFrozenForClipRef.current = null; // pas de freeze en scrub : upload frais à chaque position
    const c = glCanvasRef.current;
    if (!c || !glRendererRef.current) { if (c) c.style.display = 'none'; return; }
    const vt = tracks.find(t => t.type === 'video');
    if (!vt) { c.style.display = 'none'; return; }
    const sorted = [...vt.clips].sort((a, b) => a.startTime - b.startTime);
    const gl = glRendererRef.current;
    let active = false;
    for (let i = 0; i < sorted.length - 1; i++) {
      const c1 = sorted[i]; const c2 = sorted[i + 1];
      const tr = c2.transitionIn; if (!tr || tr.type === 'none') continue;
      const dur = tr.duration || 0.5; const zoneStart = c1.endTime - dur;
      const rawProgress = (currentTime - zoneStart) / dur;
      if (rawProgress < 0 || rawProgress > 1) continue;
      const progress = Math.max(0, Math.min(1, rawProgress));
      const fromMedia = mediaItems.find(m => m.id === c1.mediaId);
      const toMedia   = mediaItems.find(m => m.id === c2.mediaId);
      let fromOk = false, toOk = false;
      if (fromMedia?.type === 'image') { const img1 = transImgRef1.current; if (img1?.complete && img1.naturalWidth > 0) { try { gl.uploadFrame('from', img1); fromOk = true; } catch {} } }
      else { const v1 = videoRef.current; if (v1 && v1.readyState >= 2) { try { gl.uploadFrame('from', v1); fromOk = true; } catch {} } }
      if (toMedia?.type === 'image') { const img2 = transImgRef2.current; if (img2?.complete && img2.naturalWidth > 0) { try { gl.uploadFrame('to', img2); toOk = true; } catch {} } }
      else { const v2 = videoRef2.current; if (v2 && v2.readyState >= 2) { try { gl.uploadFrame('to', v2); toOk = true; } catch {} } }
      if (fromOk && toOk) { try { gl.render(tr.type, progress); active = true; } catch {} }
      else if (fromOk) { try { gl.render(tr.type, 0); active = true; } catch {} }
      else if (toOk)   { try { gl.render(tr.type, 1); active = true; } catch {} }
      break;
    }
    c.style.display = active ? 'block' : 'none';
    if (!active && videoRef2.current) videoRef2.current.pause();
  }, [currentTime, tracks, mediaItems, isPlaying]);

  // ── Timeline scroll pendant pause (scrub manuel) ──────────────────────────
  useEffect(() => {
    if (isPlaying) return; // RAF gère le scroll en lecture
    const tl = tlRef.current; if (!tl) return;
    const phX = currentTime * PX * zoom;
    const { scrollLeft, clientWidth } = tl;
    if (phX < scrollLeft + 20 || phX > scrollLeft + clientWidth - 20) tl.scrollLeft = phX - clientWidth / 2;
  }, [currentTime, zoom, isPlaying]);

  // ── History ───────────────────────────────────────────────────────────────
  const pushHistory = useCallback((newTracks) => {
    setHistory(h => {
      const cut = h.slice(0, historyIdx + 1);
      return [...cut, JSON.parse(JSON.stringify(newTracks))].slice(-30);
    });
    setHistoryIdx(i => Math.min(i + 1, 29));
  }, [historyIdx]);

  const undo = useCallback(() => {
    if (historyIdx <= 0) return;
    setTracks(JSON.parse(JSON.stringify(history[historyIdx - 1])));
    setHistoryIdx(i => i - 1);
  }, [history, historyIdx]);

  const redo = useCallback(() => {
    if (historyIdx >= history.length - 1) return;
    setTracks(JSON.parse(JSON.stringify(history[historyIdx + 1])));
    setHistoryIdx(i => i + 1);
  }, [history, historyIdx]);

  // ── Upload ────────────────────────────────────────────────────────────────
  const handleUpload = useCallback(async (files: FileList | File[]) => {
    if (!files.length) return;
    setUploading(true); setUploadProgress(0);
    const fd = new FormData();
    Array.from(files).forEach(f => fd.append('files', f));
    fd.append('username', cu());
    try {
      const uploaded = await new Promise<any[]>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', `${S()}/api/studio/montage/upload`);
        xhr.setRequestHeader('x-lea-user', cu());
        xhr.upload.onprogress = e => { if (e.lengthComputable) setUploadProgress(Math.round(e.loaded / e.total * 100)); };
        xhr.onload = async () => {
          if (xhr.status !== 200) { reject(new Error('Upload échoué')); return; }
          const { files: fls } = JSON.parse(xhr.responseText);
          const items = [];
          for (const f of fls) {
            let duration = 0, width = 1280, height = 720;
            if (f.type === 'video' || f.type === 'audio') {
              // Probe serveur (FFprobe)
              try {
                const r = await fetch(`${S()}/api/studio/montage/probe`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ filename: f.filename, username: cu() }) });
                const d = await r.json();
                if (d.duration > 0) { duration = d.duration; width = d.width || 1280; height = d.height || 720; }
              } catch {}
              // Fallback : détection client via élément <video>/<audio>
              if (!duration) {
                duration = await new Promise<number>(res => {
                  const tmp = f.type === 'audio' ? document.createElement('audio') : document.createElement('video');
                  tmp.preload = 'metadata';
                  (tmp as any).src = f.url;
                  tmp.onloadedmetadata = () => { res(isFinite(tmp.duration) && tmp.duration > 0 ? tmp.duration : 5); (tmp as any).src = ''; };
                  tmp.onerror = () => res(5);
                  setTimeout(() => res(5), 8000);
                });
              }
            }
            if (!duration) duration = 5;
            const item = { id: genId(), name: f.name, type: f.type, url: f.url, serverFilename: f.filename, duration, width, height, waveformData: fakeWaveform() };
            items.push(item);
            // Fetch real waveform async
            if (f.type !== 'image') {
              fetch(`${S()}/api/studio/montage/waveform`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ filename: f.filename, username: cu() }) })
                .then(r => r.json()).then(d => { if (d.waveform) setMediaItems(prev => prev.map(m => m.serverFilename === f.filename ? { ...m, waveformData: d.waveform } : m)); }).catch(()=>{});
            }
          }
          resolve(items);
        };
        xhr.onerror = () => reject(new Error('Erreur réseau'));
        xhr.send(fd);
      });
      setMediaItems(prev => [...prev, ...uploaded]);
      // Auto-add directly to timeline
      setTracks(prev => {
        const nt = JSON.parse(JSON.stringify(prev));
        // Curseur global = fin du dernier clip tous tracks confondus (mis à jour au fil de la boucle)
        let globalEnd = nt.reduce((max, t) => Math.max(max, t.clips.reduce((m, c) => Math.max(m, c.endTime), 0)), 0);
        for (const item of uploaded) {
          const ttype = item.type === 'audio' ? 'audio' : 'video'; // images + vidéos → piste vidéo principale (transitions entre eux)
          const track = nt.find(t => t.type === ttype);
          if (!track) continue;
          const ownEnd = track.clips.reduce((max, c) => Math.max(max, c.endTime), 0);
          // Audio → toujours coller après le dernier clip audio (commence à 0 si vide) ; vidéo/image → après globalEnd si track vide
          const startAt = ttype === 'audio' ? ownEnd : (track.clips.length === 0 ? globalEnd : ownEnd);
          const clip = makeClip(item, startAt);
          track.clips.push(clip);
          globalEnd = Math.max(globalEnd, clip.endTime);
        }
        const synced = syncAudioToVideo(nt, [...mediaItems, ...uploaded]);
        pushHistory(synced);
        return synced;
      });
    } catch (e) {
      console.error('[Montage] Upload:', e);
      showToast('Erreur upload : ' + e.message);
    } finally {
      setUploading(false); setUploadProgress(0);
    }
  }, [pushHistory]);

  // ── Add clip ──────────────────────────────────────────────────────────────
  const addClipToTrack = useCallback((media, targetType?) => {
    const ttype = targetType || (media.type === 'audio' ? 'audio' : 'video');
    setTracks(prev => {
      let nt = prev.map(t => {
        if (t.type !== ttype) return t;
        const lastEnd = t.clips.reduce((max, c) => Math.max(max, c.endTime), 0);
        return { ...t, clips: [...t.clips, makeClip(media, lastEnd)] };
      });
      nt = syncAudioToVideo(nt, [...mediaItems, media]);
      pushHistory(nt);
      return nt;
    });
  }, [pushHistory, mediaItems]);

  const loadCdnAssets = useCallback(async (category) => {
    setAssetsLoading(category);
    try {
      const r = await fetch(`${S()}/api/assets/${category}?username=${encodeURIComponent(cu())}`);
      const d = await r.json();
      if (category === 'stickers') setStickerAssets(d.assets || []);
      else setEffectAssets(d.assets || []);
    } catch {}
    setAssetsLoading(null);
  }, []);

  const toggleFav = useCallback((item: any) => {
    setFavorites(prev => {
      const exists = prev.some(f => f.key === item.key && f.type === item.type);
      const next = exists ? prev.filter(f => !(f.key === item.key && f.type === item.type)) : [...prev, item];
      try { localStorage.setItem('lea_montage_favorites', JSON.stringify(next)); } catch {}
      return next;
    });
  }, []);

  const isFav = useCallback((key: string, type: string) => {
    return favorites.some(f => f.key === key && f.type === type);
  }, [favorites]);

  const addCdnAsset = useCallback((asset) => {
    const ext   = asset.ext || '';
    const type  = ext === '.webm' ? 'effect_video'
                : (ext === '.json' || ext === '.lottie') ? 'lottie'
                : 'sticker';
    const ttype = 'effect';
    const mediaItem = { id: genId(), name: asset.name, type, url: asset.url, duration: type === 'effect_video' ? 3 : 5, waveformData: null };
    setMediaItems(prev => [...prev, mediaItem]);
    setTracks(prev => {
      const nt = prev.map(t => {
        if (t.type !== ttype) return t;
        const lastEnd = t.clips.reduce((max, c) => Math.max(max, c.endTime), 0);
        return { ...t, clips: [...t.clips, makeClip(mediaItem, Math.max(lastEnd, currentTime))] };
      });
      pushHistory(nt);
      return nt;
    });
  }, [currentTime, pushHistory]);

  const addTextClip = useCallback(() => {
    setTracks(prev => {
      const clip = makeTextClip(currentTime, currentTime + 5);
      const nt = prev.map(t => t.type !== 'text' ? t : { ...t, clips: [...t.clips, clip] });
      pushHistory(nt);
      return nt;
    });
  }, [currentTime, pushHistory]);

  // ── Delete ────────────────────────────────────────────────────────────────
  const deleteClip = useCallback((clipId, trackId) => {
    setTracks(prev => {
      const deletedClip = prev.find(t => t.id === trackId)?.clips.find(c => c.id === clipId);
      const nt = prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.filter(c => c.id !== clipId) });
      // GC : si le media du clip supprimé n'est plus référencé nulle part, libérer v1 / audio
      if (deletedClip?.mediaId) {
        const stillUsed = nt.some(t => t.clips.some(c => c.mediaId === deletedClip.mediaId));
        if (!stillUsed) {
          const m = mediaItemsRef.current.find((mi: any) => mi.id === deletedClip.mediaId);
          if (m) {
            const mUrl = ((window as any).LEA_SERVER_URL || '') + m.url;
            if (videoRef.current && videoSrcRef.current === mUrl) {
              videoRef.current.pause(); videoRef.current.removeAttribute('src'); videoRef.current.load();
              videoSrcRef.current = ''; activeClipIdRef.current = null;
            }
            if (audioRef.current && audioSrcRef.current === mUrl) {
              audioRef.current.pause(); audioRef.current.removeAttribute('src'); audioRef.current.load();
              audioSrcRef.current = '';
            }
          }
        }
      }
      pushHistory(nt);
      return nt;
    });
    setSelectedClipId(null); setSelectedTrackId(null);
  }, [pushHistory]);

  // ── Split ─────────────────────────────────────────────────────────────────
  const splitAtPlayhead = useCallback(() => {
    if (!selectedClipId || !selectedTrackId) return;
    setTracks(prev => {
      const nt = prev.map(t => {
        if (t.id !== selectedTrackId) return t;
        const clip = t.clips.find(c => c.id === selectedClipId);
        if (!clip || currentTime <= clip.startTime || currentTime >= clip.endTime) return t;
        const splitRel = currentTime - clip.startTime;
        const left = { ...clip, endTime: currentTime, mediaEnd: clip.mediaStart + splitRel * clip.speed };
        const right = { ...clip, id: genId(), startTime: currentTime, mediaStart: clip.mediaStart + splitRel * clip.speed };
        return { ...t, clips: [...t.clips.filter(c => c.id !== selectedClipId), left, right].sort((a,b) => a.startTime - b.startTime) };
      });
      pushHistory(nt);
      return nt;
    });
  }, [selectedClipId, selectedTrackId, currentTime, pushHistory]);

  // ── Audio tools ───────────────────────────────────────────────────────────
  const duplicateAudioClip = useCallback(() => {
    if (!selectedClipId || !selectedTrackId) return;
    setTracks(prev => {
      const nt = prev.map(t => {
        if (t.id !== selectedTrackId) return t;
        const clip = t.clips.find(c => c.id === selectedClipId);
        if (!clip) return t;
        const dur = clip.endTime - clip.startTime;
        const lastEnd = t.clips.reduce((m, c) => Math.max(m, c.endTime), 0);
        const copy = { ...clip, id: genId(), startTime: lastEnd, endTime: lastEnd + dur };
        return { ...t, clips: [...t.clips, copy] };
      });
      pushHistory(nt);
      return nt;
    });
  }, [selectedClipId, selectedTrackId, pushHistory]);

  const applyAudioFx = useCallback(async (effect: string, params: any = {}) => {
    if (!selectedClipId || !selectedTrackId) return;
    const track = tracks.find(t => t.id === selectedTrackId);
    const clip  = track?.clips.find(c => c.id === selectedClipId);
    const media = clip ? mediaItems.find(m => m.id === clip.mediaId) : null;
    if (!media?.serverFilename) { setAudioFxMsg('Sélectionne un clip audio'); return; }
    setAudioFxBusy(true); setAudioFxMsg(null);
    try {
      const r = await fetch(`${S()}/api/studio/montage/audio-fx`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filename: media.serverFilename, effect, params, username: cu() }),
      });
      const d = await r.json();
      if (!r.ok || !d.url) throw new Error(d.error || 'Erreur serveur');
      // Mettre à jour le media item avec le nouveau fichier
      setMediaItems(prev => prev.map(m => m.id !== media.id ? m : { ...m, url: d.url, serverFilename: d.filename }));
      setAudioFxMsg('✓ Appliqué');
      setTimeout(() => setAudioFxMsg(null), 2500);
    } catch (e: any) {
      setAudioFxMsg('Erreur : ' + (e.message || ''));
    } finally {
      setAudioFxBusy(false);
    }
  }, [selectedClipId, selectedTrackId, tracks, mediaItems]);

  // ── Update clip ───────────────────────────────────────────────────────────
  const updateClip = useCallback((clipId, trackId, updates) => {
    setTracks(prev => prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c => c.id !== clipId ? c : { ...c, ...updates }) }));
  }, []);

  const commitUpdate = useCallback((clipId, trackId, updates) => {
    setTracks(prev => {
      const nt = prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c => c.id !== clipId ? c : { ...c, ...updates }) });
      pushHistory(nt);
      return nt;
    });
  }, [pushHistory]);

  // ── Drag ──────────────────────────────────────────────────────────────────
  const handleMoveStart = useCallback((e, clipId, trackId) => {
    e.preventDefault();
    const clip = tracks.find(t => t.id === trackId)?.clips.find(c => c.id === clipId);
    if (!clip) return;
    const el = e.currentTarget || e.target;
    try { el.setPointerCapture(e.pointerId); } catch {}
    dragRef.current = { mode:'move', clipId, trackId, startX: e.clientX, origStart: clip.startTime, origEnd: clip.endTime, origMediaStart: clip.mediaStart, origMediaEnd: clip.mediaEnd };
    setSelectedClipId(clipId); setSelectedTrackId(trackId);
  }, [tracks]);

  const handleTrimStart = useCallback((e, clipId, trackId, side) => {
    e.preventDefault(); e.stopPropagation();
    const clip = tracks.find(t => t.id === trackId)?.clips.find(c => c.id === clipId);
    if (!clip) return;
    const media = mediaItems.find(m => m.id === clip.mediaId);
    try { e.currentTarget.setPointerCapture(e.pointerId); } catch {}
    const siblings = tracks.find(t => t.id === trackId)?.clips.filter(c => c.id !== clipId).map(c => ({ id: c.id, startTime: c.startTime, endTime: c.endTime })) || [];
    dragRef.current = { mode: side === 'left' ? 'trim-left' : 'trim-right', clipId, trackId, startX: e.clientX, origStart: clip.startTime, origEnd: clip.endTime, origMediaStart: clip.mediaStart, origMediaEnd: clip.mediaEnd, srcMax: media?.duration || 9999, siblingClips: siblings };
    setSelectedClipId(clipId); setSelectedTrackId(trackId);
  }, [tracks, mediaItems]);

  const handlePointerMove = useCallback((e) => {
    const d = dragRef.current; if (!d) return;
    const dt = (e.clientX - d.startX) / (PX * zoom);
    setTracks(prev => prev.map(t => {
      if (t.id !== d.trackId) return t;

      if (d.mode === 'trim-right') {
        const trimmedClip = t.clips.find(c => c.id === d.clipId);
        if (!trimmedClip) return t;
        const ne = Math.max(trimmedClip.startTime + 0.5, d.origEnd + dt);
        const actualDt = ne - d.origEnd;
        return { ...t, clips: t.clips.map(c => {
          if (c.id === d.clipId) {
            const nm = Math.min(d.srcMax, d.origMediaEnd + actualDt * c.speed);
            return { ...c, endTime: ne, mediaEnd: Math.max(c.mediaStart + 0.5, nm) };
          }
          const orig = d.siblingClips?.find(s => s.id === c.id);
          if (orig && orig.startTime >= d.origEnd) {
            return { ...c, startTime: orig.startTime + actualDt, endTime: orig.endTime + actualDt };
          }
          return c;
        })};
      }

      return { ...t, clips: t.clips.map(c => {
        if (c.id !== d.clipId) return c;
        if (d.mode === 'move') {
          const dur = d.origEnd - d.origStart;
          const ns = Math.max(0, d.origStart + dt);
          return { ...c, startTime: ns, endTime: ns + dur };
        }
        if (d.mode === 'trim-left') {
          const ns = Math.max(0, d.origStart + dt);
          const nm = Math.max(0, d.origMediaStart + dt * c.speed);
          if (ns >= c.endTime - 0.5) return c;
          return { ...c, startTime: ns, mediaStart: nm };
        }
        return c;
      })};
    }));
  }, [zoom]);

  const handlePointerUp = useCallback(() => {
    if (!dragRef.current) return;
    dragRef.current = null;
    setTracks(prev => { pushHistory(prev); return prev; });
  }, [pushHistory]);

  const handleRulerClick = useCallback((e) => {
    const tl = tlRef.current; if (!tl) return;
    const rect = tl.getBoundingClientRect();
    const x = e.clientX - rect.left + tl.scrollLeft;
    setCurrentTime(Math.max(0, x / (PX * zoom)));
    setIsPlaying(false);
  }, [zoom]);

  // ── Export ────────────────────────────────────────────────────────────────
  const exportProject = useCallback(async () => {
    setExporting(true); setExportedUrl(null);
    try {
      const payload = {
        tracks: tracks.map(t => ({
          ...t,
          clips: t.clips.map(c => {
            const m = mediaItems.find(mi => mi.id === c.mediaId);
            return { ...c, serverFilename: m?.serverFilename || '', type: m?.type || 'video' };
          })
        })),
        projectSettings: project,
        username: cu(),
      };
      const r = await fetch(`${S()}/api/studio/montage/export`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
      if (!r.ok) throw new Error(await r.text());
      const { url } = await r.json();
      setExportedUrl(S() + url);
    } catch (e) {
      showToast('Erreur export : ' + e.message);
    } finally {
      setExporting(false);
    }
  }, [tracks, project, mediaItems]);

  const loadGallery = useCallback(async () => {
    try {
      const r = await fetch(`${S()}/api/studio/montage/gallery/${cu()}`);
      const { videos } = await r.json();
      setGallery(videos || []);
    } catch {}
  }, []);

  const shareToSocial = useCallback(async (platform) => {
    if (!exportedUrl) { showToast('Exportez d\'abord votre montage'); return; }
    if (platform === 'download') { await downloadFile(exportedUrl, `${project.name}.mp4`); return; }
    try {
      const { Share } = await import('@capacitor/share');
      await Share.share({ title: `Mon montage Léa`, text: `Partagé depuis Léa`, url: exportedUrl, dialogTitle: `Partager sur ${platform}` });
    } catch { window.open(exportedUrl, '_blank'); }
  }, [exportedUrl, project.name]);

  const toggleTrack = useCallback((trackId, field) => {
    setTracks(prev => prev.map(t => t.id !== trackId ? t : { ...t, [field]: !t[field] }));
  }, []);

  // ── Voix off — enregistrement microphone ──────────────────────────
  const startVoiceRecording = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mr = new MediaRecorder(stream, { mimeType: MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : 'audio/webm' });
      recordedChunksRef.current = [];
      mr.ondataavailable = (e: any) => { if (e.data.size > 0) recordedChunksRef.current.push(e.data); };
      mr.onstop = async () => {
        stream.getTracks().forEach(t => t.stop());
        const blob = new Blob(recordedChunksRef.current, { type: 'audio/webm' });
        const file = new File([blob], `voix_off_${Date.now()}.webm`, { type: 'audio/webm' });
        await handleUpload([file] as any);
      };
      mr.start(100);
      mediaRecorderRef.current = mr;
      setIsRecording(true);
      setRecordingTime(0);
      recordTimerRef.current = setInterval(() => setRecordingTime(t => t + 1), 1000);
    } catch (e: any) {
      showToast('Microphone non disponible : ' + (e.message || ''));
    }
  }, [handleUpload]);

  const stopVoiceRecording = useCallback(() => {
    mediaRecorderRef.current?.stop();
    mediaRecorderRef.current = null;
    clearInterval(recordTimerRef.current);
    setIsRecording(false);
  }, []);

  // ── Auto-cut au rythme de la musique ─────────────────────────────
  const autoCutByBeats = useCallback(async () => {
    const audioTrack = tracks.find(t => t.type === 'audio');
    const audioClip  = audioTrack?.clips[0];
    const audioMedia = audioClip ? mediaItems.find(m => m.id === audioClip.mediaId) : null;
    if (!audioMedia?.serverFilename) { showToast('Ajoutez d\'abord une musique sur la piste audio'); return; }
    setBeatLoading(true);
    try {
      const r = await fetch(`${S()}/api/studio/montage/beat-detect`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-lea-user': cu() },
        body: JSON.stringify({ filename: audioMedia.serverFilename, username: cu() }),
      });
      const { beats, error } = await r.json();
      if (error) throw new Error(error);
      if (!beats?.length) { showToast('Aucun beat détecté dans le fichier audio'); return; }

      let cuts = 0;
      setTracks(prev => {
        const nt = JSON.parse(JSON.stringify(prev));
        const vt = nt.find(t => t.type === 'video');
        if (!vt) return prev;
        for (const beat of beats) {
          const c = vt.clips.find(cl => cl.startTime < beat - 0.05 && cl.endTime > beat + 0.05);
          if (!c) continue;
          const rel = beat - c.startTime;
          const left  = { ...c, endTime: beat, mediaEnd: c.mediaStart + rel * (c.speed || 1) };
          const right = { ...c, id: genId(), startTime: beat, mediaStart: c.mediaStart + rel * (c.speed || 1) };
          const idx = vt.clips.findIndex(cl => cl.id === c.id);
          vt.clips.splice(idx, 1, left, right);
          cuts++;
        }
        pushHistory(nt);
        return nt;
      });
      showToast(`${beats.length} beats détectés — ${cuts} coupe(s) appliquée(s)`);
    } catch (e: any) {
      showToast('Erreur beat detection : ' + e.message);
    } finally {
      setBeatLoading(false);
    }
  }, [tracks, mediaItems, pushHistory]);

  // ── Chroma key (fond couleur) ────────────────────────────────────
  const applyChromakey = useCallback(async (color = '#00ff00', similarity = 0.15, blend = 0.05) => {
    if (!selectedClipId || !selectedTrackId) return;
    const track = tracks.find(t => t.id === selectedTrackId);
    const clip  = track?.clips.find(c => c.id === selectedClipId);
    const media = clip ? mediaItems.find(m => m.id === clip.mediaId) : null;
    if (!media?.serverFilename) { setClipFxMsg('Sélectionne un clip'); return; }
    setClipFxBusy(true); setClipFxMsg(null);
    try {
      const r = await fetch(`${S()}/api/studio/montage/chromakey`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-lea-user': cu() },
        body: JSON.stringify({ filename: media.serverFilename, color, similarity, blend, username: cu() }),
      });
      const d = await r.json();
      if (!r.ok || !d.url) throw new Error(d.error || 'Erreur serveur');
      const newMedia = { ...media, id: genId(), url: d.url, serverFilename: d.filename, name: `ck_${media.name}` };
      setMediaItems(prev => [...prev, newMedia]);
      commitUpdate(selectedClipId, selectedTrackId, { mediaId: newMedia.id });
      setClipFxMsg('✓ Chroma key appliqué');
      setTimeout(() => setClipFxMsg(null), 3000);
    } catch (e: any) {
      setClipFxMsg('Erreur : ' + e.message);
    } finally {
      setClipFxBusy(false);
    }
  }, [selectedClipId, selectedTrackId, tracks, mediaItems, commitUpdate]);

  // ── Stabilisation vidéo ──────────────────────────────────────────
  const applyStabilize = useCallback(async () => {
    if (!selectedClipId || !selectedTrackId) return;
    const track = tracks.find(t => t.id === selectedTrackId);
    const clip  = track?.clips.find(c => c.id === selectedClipId);
    const media = clip ? mediaItems.find(m => m.id === clip.mediaId) : null;
    if (!media?.serverFilename || media.type !== 'video') { setClipFxMsg('Sélectionne un clip vidéo'); return; }
    setClipFxBusy(true); setClipFxMsg('Stabilisation en cours (2 passes)…');
    try {
      const r = await fetch(`${S()}/api/studio/montage/stabilize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-lea-user': cu() },
        body: JSON.stringify({ filename: media.serverFilename, username: cu() }),
      });
      const d = await r.json();
      if (!r.ok || !d.url) throw new Error(d.error || 'Erreur serveur');
      const newMedia = { ...media, id: genId(), url: d.url, serverFilename: d.filename, name: `stab_${media.name}` };
      setMediaItems(prev => [...prev, newMedia]);
      commitUpdate(selectedClipId, selectedTrackId, { mediaId: newMedia.id });
      setClipFxMsg('✓ Vidéo stabilisée');
      setTimeout(() => setClipFxMsg(null), 3000);
    } catch (e: any) {
      setClipFxMsg('Erreur : ' + e.message);
    } finally {
      setClipFxBusy(false);
    }
  }, [selectedClipId, selectedTrackId, tracks, mediaItems, commitUpdate]);

  // ── KEYFRAMES ─────────────────────────────────────────────────────────────
  const addKFAtCurrentTime = useCallback((clipId, trackId, prop, value, clip) => {
    const relT = Math.round(Math.max(0, currentTime - clip.startTime) * 100) / 100;
    setTracks(prev => {
      const nt = prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c => {
        if (c.id !== clipId) return c;
        const existing = ((c.keyframes || {})[prop] || []).filter(kf => Math.abs(kf.t - relT) > 0.04);
        const newKFs = [...existing, { t: relT, value }].sort((a, b) => a.t - b.t);
        return { ...c, keyframes: { ...(c.keyframes || {}), [prop]: newKFs } };
      })});
      pushHistory(nt);
      return nt;
    });
  }, [currentTime, pushHistory]);

  const removeKF = useCallback((clipId, trackId, prop, idx) => {
    setTracks(prev => {
      const nt = prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c => {
        if (c.id !== clipId) return c;
        const kfs = ((c.keyframes || {})[prop] || []).filter((_, i) => i !== idx);
        return { ...c, keyframes: { ...(c.keyframes || {}), [prop]: kfs } };
      })});
      pushHistory(nt);
      return nt;
    });
  }, [pushHistory]);

  const clearKF = useCallback((clipId, trackId, prop) => {
    setTracks(prev => {
      const nt = prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c =>
        c.id !== clipId ? c : { ...c, keyframes: { ...(c.keyframes || {}), [prop]: [] } }
      )});
      pushHistory(nt);
      return nt;
    });
  }, [pushHistory]);

  // ── SOUS-TITRES IA ────────────────────────────────────────────────────────
  const autoSubtitles = useCallback(async () => {
    if (!selectedClip || !selectedMedia?.serverFilename || selectedMedia.type !== 'video') {
      setClipFxMsg('Sélectionne un clip vidéo pour la transcription'); return;
    }
    setClipFxBusy(true); setClipFxMsg('Transcription IA en cours… (peut prendre 1-2 min)');
    try {
      const r = await fetch(`${S()}/api/studio/montage/transcribe`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-lea-user': cu() },
        body: JSON.stringify({ filename: selectedMedia.serverFilename, username: cu(), language: 'fr' }),
      });
      const d = await r.json();
      if (!r.ok || !d.segments) throw new Error(d.error || 'Erreur serveur');
      setTracks(prev => {
        const nt = JSON.parse(JSON.stringify(prev));
        const tt = nt.find(t => t.type === 'text');
        if (!tt) return prev;
        const base = selectedClip.startTime || 0;
        for (const seg of d.segments) {
          const tc = makeTextClip(base + seg.start, base + seg.end);
          tc.textStyle.content = seg.text;
          tc.textStyle.fontSize = 32;
          tc.textStyle.y = 85;
          tc.textStyle.outlineWidth = 2;
          tc.textStyle.outlineColor = '#000000';
          tt.clips.push(tc);
        }
        pushHistory(nt);
        return nt;
      });
      setClipFxMsg(`✓ ${d.count} sous-titres créés`);
      setTimeout(() => setClipFxMsg(null), 4000);
    } catch (e: any) {
      setClipFxMsg('Erreur : ' + e.message);
    } finally {
      setClipFxBusy(false);
    }
  }, [selectedClip, selectedMedia, pushHistory]);

  // ══════════════════════════════════════════════════════════════════════════
  // RENDER HELPERS
  // ══════════════════════════════════════════════════════════════════════════

  const TEMPLATES = [
    { id:'tiktok',   icon:'🎵', label:'TikTok Story', desc:'9:16 • 30fps • coupes rapides', settings:{ aspectRatio:'9:16', fps:30, resolution:'1080p', bgColor:'#000000' }, trType:'fade', trDur:0.3, hint:'Importe tes clips vidéo, l\'auto-cut appliquera les coupes au rythme.' },
    { id:'voyage',   icon:'✈️', label:'Voyage',        desc:'16:9 • 24fps • Ken Burns',     settings:{ aspectRatio:'16:9', fps:24, resolution:'1080p', bgColor:'#0a0a0a' }, trType:'dissolve', trDur:0.8, hint:'Ajoute tes photos + vidéos. Active Ken Burns sur chaque photo pour donner du mouvement.' },
    { id:'anniv',    icon:'🎂', label:'Anniversaire',  desc:'1:1 • 30fps • dynamique',       settings:{ aspectRatio:'1:1',  fps:30, resolution:'1080p', bgColor:'#1a0a2e' }, trType:'wipeRight', trDur:0.5, hint:'Format carré Instagram. Ajoute ta musique, puis Auto-cut pour les coupes.' },
    { id:'cinema',   icon:'🎬', label:'Cinéma',        desc:'16:9 • 24fps • transitions lentes', settings:{ aspectRatio:'16:9', fps:24, resolution:'2K', bgColor:'#000000' }, trType:'crosszoom', trDur:1.2, hint:'Qualité 2K. Transitions lentes crosszoom. Filtres "Film" ou "Matte" recommandés.' },
    { id:'reels',    icon:'📱', label:'Reels',         desc:'9:16 • 30fps • énergie',        settings:{ aspectRatio:'9:16', fps:30, resolution:'1080p', bgColor:'#000000' }, trType:'wipeLeft', trDur:0.25, hint:'Format Reels Instagram. Transitions rapides. Ajoute ta music et utilise Auto-cut.' },
    { id:'vlog',     icon:'🎙️', label:'Vlog',          desc:'16:9 • 30fps • naturel',        settings:{ aspectRatio:'16:9', fps:30, resolution:'1080p', bgColor:'#111111' }, trType:'fade', trDur:0.6, hint:'Format YouTube. Alterne clips voix + images B-roll. Stabilise tes shaky clips.' },
  ];

  function renderMediaPanel() {
    const tabs = [
      { id:'favoris',   label:'★ Favs',    items: [],                 ref: null },
      { id:'videos',    label:'Vidéos',    items: mediaByType.videos, ref: videoInRef },
      { id:'photos',    label:'Photos',    items: mediaByType.photos, ref: photoInRef },
      { id:'audio',     label:'Audio',     items: mediaByType.audio,  ref: audioInRef },
      { id:'text',      label:'Texte',     items: [],                 ref: null },
      { id:'templates', label:'Template',  items: [],                 ref: null },
      { id:'stickers',  label:'Stickers',  items: [],                 ref: null },
      { id:'effets',    label:'Effets',    items: [],                 ref: null },
    ];
    const curTab = tabs.find(t => t.id === activeMediaTab);

    function handleTabClick(tab) {
      setActiveMediaTab(tab.id);
      if (tab.id === 'text') { addTextClip(); return; }
      if (tab.id === 'stickers') { if (stickerAssets.length === 0) loadCdnAssets('stickers'); return; }
      if (tab.id === 'effets')   { if (effectAssets.length === 0)  loadCdnAssets('effets');   return; }
      if (tab.ref?.current) { try { tab.ref.current.value = ''; tab.ref.current.click(); } catch {} }
    }

    return (
      <div style={{ display:'flex', flexDirection:'column', height:'100%', backgroundColor:BG1, borderRight:`1px solid rgba(255,255,255,0.08)` }}>
        <div style={{ padding:'10px 8px 6px' }}>
          {uploading ? (
            <div style={{ width:'100%', padding:'8px', backgroundColor:BG3, borderRadius:8, display:'flex', alignItems:'center', justifyContent:'center', gap:6, fontSize:12, color:'rgba(255,255,255,0.6)' }}>
              <Loader2 size={14} style={{ animation:'spin 1s linear infinite' }}/> {uploadProgress}%
            </div>
          ) : (
            <label style={{ width:'100%', display:'flex', alignItems:'center', justifyContent:'center', gap:6, padding:'8px', backgroundColor:ACCENT, color:'#000', borderRadius:8, cursor:'pointer', fontWeight:'bold', fontSize:12 }}
              onClick={() => { try { videoInRef.current.value=''; videoInRef.current.click(); photoInRef.current.value=''; } catch {} }}>
              <Plus size={14}/> Ajouter (vidéo / photo)
            </label>
          )}
        </div>
        <div style={{ display:'flex', borderBottom:`1px solid rgba(255,255,255,0.08)`, padding:'0 2px', flexShrink:0 }}>
          {tabs.map(tab => (
            <button key={tab.id} onClick={() => handleTabClick(tab)}
              style={{ flex:1, padding:'5px 1px', fontSize:8, color: activeMediaTab === tab.id ? ACCENT : 'rgba(255,255,255,0.35)', background:'none', border:'none', cursor:'pointer', borderBottom: activeMediaTab === tab.id ? `2px solid ${ACCENT}` : '2px solid transparent', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.04em' }}>
              {tab.label}
            </button>
          ))}
        </div>
        <div style={{ flex:1, overflowY:'auto', padding:6 }}>
          {activeMediaTab === 'text' && (
            <div style={{ padding:4 }}>
              <button onClick={addTextClip}
                style={{ width:'100%', padding:'10px', backgroundColor:BG3, color:'white', border:`1px solid rgba(255,255,255,0.1)`, borderRadius:8, cursor:'pointer', fontSize:12, display:'flex', alignItems:'center', justifyContent:'center', gap:6, marginBottom:8 }}>
                <Type size={14}/> Ajouter du texte
              </button>
              {[['Impact','TITRE PERCUTANT','#ffffff'],['Arial','Sous-titre','#cccccc'],['Georgia','Texte élégant','#f0e6d0']].map(([font, ex, color]) => (
                <button key={font} onClick={() => {
                  const clip = makeTextClip(currentTime, currentTime + 5);
                  clip.textStyle.font = font; clip.textStyle.content = ex; clip.textStyle.color = color;
                  setTracks(prev => { const nt = prev.map(t => t.type !== 'text' ? t : { ...t, clips:[...t.clips,clip] }); pushHistory(nt); return nt; });
                  setSelectedClipId(clip.id); setSelectedTrackId('text-0'); setActiveInspTab('properties');
                }}
                  style={{ width:'100%', marginBottom:6, padding:'8px 10px', backgroundColor:BG3, color, border:`1px solid rgba(255,255,255,0.08)`, borderRadius:6, cursor:'pointer', fontFamily:font, fontSize:12, textAlign:'left' }}>
                  {ex}
                </button>
              ))}
            </div>
          )}
          {activeMediaTab === 'favoris' && (() => {
            if (favorites.length === 0) return (
              <div style={{ padding:24, textAlign:'center', color:'rgba(255,255,255,0.15)', fontSize:12 }}>
                <Star size={28} style={{ display:'block', margin:'0 auto 8px', opacity:0.2 }}/>
                Pas encore de favoris<br/>
                <span style={{ fontSize:9, opacity:0.6 }}>Appuyez sur ★ sur n'importe quelle ressource</span>
              </div>
            );
            return (
              <div style={{ padding:4 }}>
                {(['media','sticker','effet','transition'] as const).map(type => {
                  const items = favorites.filter(f => f.type === type);
                  if (items.length === 0) return null;
                  const typeLabel = type === 'media' ? 'Médias' : type === 'sticker' ? 'Stickers' : type === 'effet' ? 'Effets' : 'Transitions';
                  return (
                    <div key={type} style={{ marginBottom:10 }}>
                      <div style={{ fontSize:9, color:'rgba(255,255,255,0.3)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.08em', padding:'2px 2px 5px' }}>{typeLabel}</div>
                      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:4 }}>
                        {items.map(fav => (
                          <div key={fav.key} style={{ position:'relative', backgroundColor:BG3, borderRadius:8, overflow:'hidden', border:'1px solid rgba(245,158,11,0.25)', cursor:'pointer' }}
                            onClick={() => {
                              if (fav.type === 'media') { const m = mediaItems.find(mi => mi.id === fav.key); if (m) addClipToTrack(m); return; }
                              if (fav.type !== 'transition') addCdnAsset({ filename:fav.key, name:fav.label, url:fav.url, ext:fav.ext||'' });
                            }}>
                            <div style={{ height:68, backgroundColor:BG4, display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden' }}>
                              {fav.type === 'transition' && (fav.thumbUrl
                                ? <img src={fav.thumbUrl} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                                : <div style={{ width:'100%', height:'100%', background:'linear-gradient(135deg,rgba(30,58,110,0.6),rgba(110,30,26,0.6))' }}/>)}
                              {fav.type === 'media' && fav.mediaType === 'image' && <img src={S()+fav.url} alt="" loading="lazy" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>}
                              {fav.type === 'media' && fav.mediaType === 'video' && <LazyVideoThumb url={fav.url}/>}
                              {fav.type === 'media' && fav.mediaType === 'audio' && <div style={{ width:'100%', height:'100%', display:'flex', alignItems:'center', justifyContent:'center' }}><Music size={20} color="rgba(52,211,153,0.5)"/></div>}
                              {fav.type !== 'transition' && fav.type !== 'media' && (fav.ext === '.png' || fav.ext === '.webp' || fav.ext === '.gif') && <img src={S()+fav.url} alt="" style={{ width:'100%', height:'100%', objectFit:'contain' }}/>}
                              {fav.type !== 'transition' && fav.type !== 'media' && fav.ext === '.webm' && <WebmThumb url={fav.url}/>}
                              {fav.type !== 'transition' && fav.type !== 'media' && (fav.ext === '.json' || fav.ext === '.lottie') && <LottieThumb url={fav.url}/>}
                            </div>
                            <div style={{ padding:'3px 6px', fontSize:9, color:'rgba(255,255,255,0.4)', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{fav.label.slice(0,20)}</div>
                            <button onClick={e => { e.stopPropagation(); toggleFav(fav); }}
                              style={{ position:'absolute', top:3, right:3, background:'none', border:'none', cursor:'pointer', padding:2, color:'#f59e0b', lineHeight:1 }}>
                              <Star size={12} fill="#f59e0b"/>
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            );
          })()}
          {(activeMediaTab === 'stickers' || activeMediaTab === 'effets') && (() => {
            const isSt   = activeMediaTab === 'stickers';
            const assets = isSt ? stickerAssets : effectAssets;
            const cat    = isSt ? 'stickers' : 'effets';
            const favType = isSt ? 'sticker' : 'effet';
            if (assetsLoading === cat) return (
              <div style={{ display:'flex', alignItems:'center', justifyContent:'center', height:80, gap:8, color:'rgba(255,255,255,0.4)', fontSize:12 }}>
                <Loader2 size={14} style={{ animation:'spin 1s linear infinite' }}/> Chargement…
              </div>
            );
            return (
              <div style={{ padding:4 }}>
                <div style={{ display:'flex', justifyContent:'flex-end', marginBottom:4 }}>
                  <button onClick={() => loadCdnAssets(cat)}
                    style={{ padding:'2px 8px', backgroundColor:'transparent', border:'none', color:'rgba(255,255,255,0.3)', cursor:'pointer', fontSize:10 }}>
                    ↺ Actualiser
                  </button>
                </div>
                {assets.length === 0 ? (
                  <div style={{ padding:'20px 8px', textAlign:'center', color:'rgba(255,255,255,0.15)', fontSize:11 }}>
                    {isSt ? '🎨' : '✨'} Dossier vide<br/>
                    <span style={{ fontSize:9, opacity:0.7 }}>/public/assets/{isSt ? 'stickers' : 'effets_video'}/</span>
                  </div>
                ) : (
                  <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:4 }}>
                    {assets.map(asset => {
                      const favd = isFav(asset.filename, favType);
                      return (
                        <div key={asset.filename} style={{ position:'relative', backgroundColor:BG3, borderRadius:8, overflow:'hidden', border:`1px solid ${favd ? 'rgba(245,158,11,0.35)' : 'rgba(255,255,255,0.06)'}`, cursor:'pointer' }}
                          onClick={() => addCdnAsset(asset)}>
                          <div style={{ height:68, backgroundColor:BG4, display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden' }}>
                            {(asset.ext === '.png' || asset.ext === '.webp' || asset.ext === '.gif') && <img src={S()+asset.url} alt="" style={{ width:'100%', height:'100%', objectFit:'contain' }}/>}
                            {asset.ext === '.webm' && <WebmThumb url={asset.url}/>}
                            {(asset.ext === '.json' || asset.ext === '.lottie') && <LottieThumb url={asset.url}/>}
                          </div>
                          <div style={{ padding:'3px 6px', fontSize:9, color:'rgba(255,255,255,0.45)', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{asset.name.slice(0,20)}</div>
                          <button onClick={e => { e.stopPropagation(); toggleFav({ key:asset.filename, type:favType, label:asset.name, url:asset.url, ext:asset.ext }); }}
                            style={{ position:'absolute', top:3, right:3, background:'none', border:'none', cursor:'pointer', padding:2, color: favd ? '#f59e0b' : 'rgba(255,255,255,0.25)', lineHeight:1 }}>
                            <Star size={12} fill={favd ? '#f59e0b' : 'none'}/>
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })()}
          {activeMediaTab === 'templates' && (
            <div style={{ padding:4 }}>
              <div style={{ fontSize:9, color:'rgba(255,255,255,0.3)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.08em', padding:'4px 2px 8px' }}>Choisir un style de montage</div>
              {TEMPLATES.map(tpl => (
                <div key={tpl.id}
                  style={{ backgroundColor:BG3, borderRadius:10, padding:'10px 12px', marginBottom:6, border:'1px solid rgba(255,255,255,0.07)', cursor:'pointer' }}
                  onClick={() => {
                    askConfirm(`Appliquer le template "${tpl.label}" ? (Les paramètres projet seront modifiés)`, () => {
                      setProject(p => ({ ...p, ...tpl.settings }));
                      showToast(`✓ Template "${tpl.label}" appliqué !\n\n${tpl.hint}`);
                    });
                  }}>
                  <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:4 }}>
                    <span style={{ fontSize:22 }}>{tpl.icon}</span>
                    <div>
                      <div style={{ fontSize:12, fontWeight:'bold', color:'white' }}>{tpl.label}</div>
                      <div style={{ fontSize:9, color:'rgba(255,255,255,0.35)' }}>{tpl.desc}</div>
                    </div>
                    <button
                      style={{ marginLeft:'auto', padding:'4px 10px', backgroundColor:ACCENT, border:'none', borderRadius:6, color:'#000', fontSize:9, fontWeight:'bold', cursor:'pointer' }}
                      onClick={e => { e.stopPropagation(); setProject(p => ({ ...p, ...tpl.settings })); showToast(`✓ Template "${tpl.label}" appliqué !\n\n${tpl.hint}`); }}>
                      Appliquer
                    </button>
                  </div>
                  <div style={{ fontSize:9, color:'rgba(255,255,255,0.25)', lineHeight:1.4 }}>{tpl.hint}</div>
                </div>
              ))}
            </div>
          )}

          {(activeMediaTab === 'videos' || activeMediaTab === 'photos' || activeMediaTab === 'audio') && (
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:4 }}>
              {(curTab?.items || []).map(item => {
                const favd = isFav(item.id, 'media');
                return (
                  <div key={item.id}
                    style={{ backgroundColor:BG3, borderRadius:8, overflow:'hidden', cursor:'pointer', border:`1px solid ${favd ? 'rgba(245,158,11,0.3)' : 'rgba(255,255,255,0.06)'}`, position:'relative' }}
                    onDoubleClick={() => addClipToTrack(item)}
                    draggable onDragStart={e => e.dataTransfer.setData('mediaId', item.id)}
                    title={`Double-clic pour ajouter — ${item.name}`}
                  >
                    <div style={{ height:60, backgroundColor:BG4, position:'relative', overflow:'hidden' }}>
                      {item.type === 'image' && item.url && (
                        <img src={S()+item.url} alt="" loading="lazy" style={{ width:'100%', height:'100%', objectFit:'cover' }} />
                      )}
                      {item.type === 'video' && item.url && (
                        <LazyVideoThumb url={item.url} duration={item.duration} />
                      )}
                      {item.type === 'audio' && (
                        <div style={{ width:'100%', height:'100%', display:'flex', alignItems:'center', justifyContent:'center' }}>
                          <WaveformCanvas data={item.waveformData || fakeWaveform(60)} width={100} height={44} color="#34d399"/>
                        </div>
                      )}
                      {item.type === 'video' && (
                        <div style={{ position:'absolute', bottom:3, right:3, backgroundColor:'rgba(0,0,0,0.7)', padding:'1px 4px', borderRadius:3, fontSize:9, color:'white' }}>{fmtDur(item.duration)}</div>
                      )}
                    </div>
                    <div style={{ padding:'4px 6px', fontSize:9, color:'rgba(255,255,255,0.45)', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{item.name.slice(0,22)}</div>
                    <button onClick={() => addClipToTrack(item)}
                      style={{ position:'absolute', top:3, left:3, width:18, height:18, borderRadius:'50%', backgroundColor:ACCENT, border:'none', color:'#000', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', fontSize:10, fontWeight:'bold', zIndex:2 }}>+</button>
                    <button onClick={e => { e.stopPropagation(); toggleFav({ key:item.id, type:'media', label:item.name, url:item.url, ext:'.'+item.type, mediaType:item.type }); }}
                      style={{ position:'absolute', top:3, right:3, background:'none', border:'none', cursor:'pointer', padding:2, color: favd ? '#f59e0b' : 'rgba(255,255,255,0.25)', lineHeight:1, zIndex:2 }}>
                      <Star size={12} fill={favd ? '#f59e0b' : 'none'}/>
                    </button>
                  </div>
                );
              })}
              {(curTab?.items || []).length === 0 && (
                <div style={{ gridColumn:'span 2', padding:24, textAlign:'center', color:'rgba(255,255,255,0.15)', fontSize:12 }}>
                  <Upload size={28} style={{ display:'block', margin:'0 auto 8px' }}/>
                  Importez des fichiers
                </div>
              )}
            </div>
          )}
        </div>
        <div style={{ padding:'6px 8px', borderTop:`1px solid rgba(255,255,255,0.06)`, display:'flex', gap:4 }}>
          <button onClick={() => { setActiveMediaTab('audio'); try { audioInRef.current.value=''; audioInRef.current.click(); } catch {} }}
            style={{ flex:1, padding:'6px', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.08)`, borderRadius:6, color:'rgba(255,255,255,0.5)', cursor:'pointer', fontSize:10, display:'flex', alignItems:'center', justifyContent:'center', gap:4 }}>
            <Mic size={12}/> Audio
          </button>
          <button onClick={() => { loadGallery(); setShowGallery(true); }}
            style={{ flex:1, padding:'6px', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.08)`, borderRadius:6, color:'rgba(255,255,255,0.5)', cursor:'pointer', fontSize:10, display:'flex', alignItems:'center', justifyContent:'center', gap:4 }}>
            <Grid size={12}/> Galerie
          </button>
        </div>
      </div>
    );
  }

  function renderVideoPreview() {
    const ar = ASPECT_RATIOS.find(a => a.id === project.aspectRatio) || ASPECT_RATIOS[0];
    const activeM = activeVideoClip ? mediaItems.find(m => m.id === activeVideoClip.mediaId) : null;
    return (
      <div style={{ display:'flex', flexDirection:'column', width:'100%', height:'100%', backgroundColor:BG0, alignItems:'center', minHeight:0 }}>
        <div style={{ flex:1, minHeight:0, display:'flex', alignItems:'center', justifyContent:'center', padding:'8px', width:'100%' }}>
          {(() => {
            const clipAnim = getClipAnimStyle(activeVideoClip, currentTime);
            const filterCss = activeVideoClip ? getFilterCss(activeVideoClip.filters, activeVideoClip.colorAdj) : 'none';
            const cropStyle = activeVideoClip && (activeVideoClip.cropTop||activeVideoClip.cropRight||activeVideoClip.cropBottom||activeVideoClip.cropLeft)
              ? { clipPath:`inset(${(activeVideoClip.cropTop||0)*100}% ${(activeVideoClip.cropRight||0)*100}% ${(activeVideoClip.cropBottom||0)*100}% ${(activeVideoClip.cropLeft||0)*100}%)` }
              : {};
            const maskCss = activeVideoClip?.mask && activeVideoClip.mask !== 'none'
              ? MASKS.find(m => m.id === activeVideoClip.mask)?.css || 'none'
              : 'none';
            return (
              <div style={{ position:'relative', maxWidth:'100%', maxHeight:'100%', aspectRatio:`${ar.w}/${ar.h}`, backgroundColor:project.bgColor, borderRadius:4, overflow:'hidden', boxShadow:'0 4px 32px rgba(0,0,0,0.9)', ...cropStyle }}>
                {/* v1 toujours en flux normal (donne sa hauteur au conteneur aspect-ratio)
                    opacity:0 pendant les clips photo → browser continue de décoder en arrière-plan */}
                <video ref={videoRef}
                  style={{ width:'100%', height:'100%', objectFit:'contain', filter:filterCss, transform:clipAnim.transform, opacity: activeM?.type === 'video' ? clipAnim.opacity : 0, clipPath:maskCss }}
                  muted={activeVideoClip?.isMuted} playsInline />
                <video ref={videoRef2} style={{ position:'absolute', width:1, height:1, opacity:0, pointerEvents:'none', top:0, left:0 }} muted playsInline />
                <img ref={transImgRef1} style={{ display:'none' }} alt=""/>
                <img ref={transImgRef2} style={{ display:'none' }} alt=""/>
                <canvas ref={glCanvasRef} style={{ position:'absolute', inset:0, width:'100%', height:'100%', display:'none', zIndex:4, borderRadius:4 }} />
                {/* Photo : se superpose à v1 en position absolute */}
                {activeM?.type === 'image' && (
                  <img src={S() + activeM.url} alt=""
                    style={{ position:'absolute', inset:0, width:'100%', height:'100%', objectFit:'contain', filter:filterCss, transform:clipAnim.transform, opacity:clipAnim.opacity, clipPath:maskCss }} />
                )}

                {activeTextClips.map(tc => tc.textStyle && (
                  <div key={tc.id} style={{ position:'absolute', left:`${tc.textStyle.x}%`, top:`${tc.textStyle.y}%`, transform:'translate(-50%,-50%)', textAlign: tc.textStyle.align, fontSize: tc.textStyle.fontSize, color: tc.textStyle.color, fontFamily: tc.textStyle.font, fontWeight: tc.textStyle.bold ? 'bold' : 'normal', fontStyle: tc.textStyle.italic ? 'italic' : 'normal', backgroundColor: tc.textStyle.bgOpacity > 0 ? `${tc.textStyle.bgColor}${Math.round(tc.textStyle.bgOpacity*255).toString(16).padStart(2,'0')}` : 'transparent', padding: tc.textStyle.bgOpacity > 0 ? '2px 8px' : 0, WebkitTextStroke: tc.textStyle.outlineWidth > 0 ? `${tc.textStyle.outlineWidth}px ${tc.textStyle.outlineColor}` : 'none', whiteSpace:'nowrap', userSelect:'none', pointerEvents:'none', zIndex:5 }}>
                    {tc.textStyle.content}
                  </div>
                ))}
                {!activeVideoClip && !activeTextClips.length && (
                  tracks.some(t => t.clips.length > 0)
                    ? <div style={{ position:'absolute', inset:0, backgroundColor: project.bgColor }}/>
                    : <div style={{ position:'absolute', inset:0, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', color:'rgba(255,255,255,0.08)' }}>
                        <Film size={36}/><div style={{ fontSize:11, marginTop:8 }}>Ajoutez des médias à la timeline</div>
                      </div>
                )}
              </div>
            );
          })()}
        </div>
        <div style={{ fontFamily:'monospace', fontSize:13, color:ACCENT, padding:'4px 0', letterSpacing:'0.1em', fontWeight:'bold' }}>
          {fmtTimecode(currentTime, project.fps)} <span style={{ color:'rgba(255,255,255,0.25)', fontSize:10 }}>/ {fmtTimecode(totalDur - 5, project.fps)}</span>
        </div>
        <div style={{ display:'flex', alignItems:'center', gap:8, padding:'6px 12px', borderTop:`1px solid rgba(255,255,255,0.06)`, width:'100%', justifyContent:'center' }}>
          <button onClick={undo} disabled={historyIdx <= 0}
            style={{ padding:'6px', backgroundColor:'transparent', border:'none', color: historyIdx > 0 ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.1)', cursor:'pointer', borderRadius:6 }}>
            <RotateCcw size={15}/>
          </button>
          <button onClick={() => setCurrentTime(t => Math.max(0, t - 1/project.fps))}
            style={{ padding:'6px', backgroundColor:BG3, border:'none', color:'white', cursor:'pointer', borderRadius:6 }}><SkipBack size={15}/></button>
          <button onClick={() => { cancelAnimationFrame(rafRef.current); setIsPlaying(p => !p); }}
            style={{ width:42, height:42, borderRadius:'50%', backgroundColor:ACCENT, border:'none', color:'#000', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center' }}>
            {isPlaying ? <Pause size={19}/> : <Play size={19} style={{ marginLeft:2 }}/>}
          </button>
          <button onClick={() => setCurrentTime(t => Math.min(totalDur - 0.1, t + 1/project.fps))}
            style={{ padding:'6px', backgroundColor:BG3, border:'none', color:'white', cursor:'pointer', borderRadius:6 }}><SkipForward size={15}/></button>
          <button onClick={redo} disabled={historyIdx >= history.length - 1}
            style={{ padding:'6px', backgroundColor:'transparent', border:'none', color: historyIdx < history.length - 1 ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.1)', cursor:'pointer', borderRadius:6 }}>
            <RefreshCw size={15}/>
          </button>
        </div>
      </div>
    );
  }

  function renderInspector() {
    if (!selectedClip || !selectedTrackId) return renderProjectSettings();
    const track = tracks.find(t => t.id === selectedTrackId);
    if (!track) return renderProjectSettings();
    const ttype = track.type;

    const sl = { width:'100%', accentColor:ACCENT };
    const lbl = { fontSize:10, color:'rgba(255,255,255,0.4)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.08em', display:'block', marginBottom:4 };
    const inp: React.CSSProperties = { width:'100%', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:6, padding:'6px 8px', color:'white', fontSize:12, boxSizing:'border-box' };
    const row = { marginBottom:12 };

    const insTabs = ttype === 'video'
      ? [['properties','Propriétés'],['color','Couleur'],['filters','Filtres'],['animation','Anim.'],['keyframes','KF'],['mask','Masque']]
      : [];

    return (
      <div style={{ display:'flex', flexDirection:'column', height:'100%', backgroundColor:BG1, borderLeft:`1px solid rgba(255,255,255,0.08)`, overflow:'hidden' }}>
        <div style={{ padding:'8px 10px', borderBottom:`1px solid rgba(255,255,255,0.08)`, display:'flex', alignItems:'center', justifyContent:'space-between', flexShrink:0 }}>
          <span style={{ fontSize:11, color:'white', fontWeight:'bold' }}>{TRACK_CFG[ttype]?.label || 'Inspecteur'}</span>
          <button onClick={() => deleteClip(selectedClip.id, selectedTrackId)}
            style={{ padding:'4px 6px', backgroundColor:'rgba(239,68,68,0.15)', border:'1px solid rgba(239,68,68,0.3)', borderRadius:6, color:'#ef4444', cursor:'pointer', fontSize:10 }}>
            <Trash2 size={12}/>
          </button>
        </div>
        {insTabs.length > 0 && (
          <div style={{ display:'flex', borderBottom:`1px solid rgba(255,255,255,0.06)`, flexShrink:0 }}>
            {insTabs.map(([id,l]) => (
              <button key={id} onClick={() => setActiveInspTab(id)}
                style={{ flex:1, padding:'5px 2px', fontSize:9, fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.05em', background:'none', border:'none', cursor:'pointer', borderBottom: activeInspTab === id ? `2px solid ${ACCENT}` : '2px solid transparent', color: activeInspTab === id ? ACCENT : 'rgba(255,255,255,0.35)' }}>
                {l}
              </button>
            ))}
          </div>
        )}
        <div style={{ flex:1, overflowY:'auto', padding:10 }}>

          {/* VIDEO — Properties */}
          {(ttype === 'video') && activeInspTab === 'properties' && (<>
            <div style={row}>
              <label style={lbl}>Vitesse</label>
              <div style={{ display:'flex', flexWrap:'wrap', gap:4 }}>
                {SPEEDS.map(sp => (
                  <button key={sp} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { speed:sp })}
                    style={{ padding:'4px 8px', borderRadius:5, fontSize:11, border:'none', cursor:'pointer', backgroundColor: selectedClip.speed===sp ? ACCENT : BG3, color: selectedClip.speed===sp ? '#000' : 'white', fontWeight: selectedClip.speed===sp ? 'bold' : 'normal' }}>
                    {sp}x
                  </button>
                ))}
              </div>
            </div>
            <div style={row}>
              <label style={lbl}>Volume — {Math.round((selectedClip.volume||1)*100)}%</label>
              <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                <button onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { isMuted: !selectedClip.isMuted })}
                  style={{ background:'none', border:'none', cursor:'pointer', color: selectedClip.isMuted ? '#f87171' : 'rgba(255,255,255,0.4)', padding:4 }}>
                  {selectedClip.isMuted ? <VolumeX size={15}/> : <Volume2 size={15}/>}
                </button>
                <input type="range" min={0} max={2} step={0.05} value={selectedClip.volume||1} style={sl}
                  onChange={e => updateClip(selectedClip.id, selectedTrackId, { volume:+e.target.value })}
                  onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { volume:selectedClip.volume })} />
              </div>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:12 }}>
              <div><label style={lbl}>Fondu début (s)</label><input type="number" min={0} max={5} step={0.1} style={inp} value={selectedClip.fadeIn||0} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { fadeIn:+e.target.value })}/></div>
              <div><label style={lbl}>Fondu fin (s)</label><input type="number" min={0} max={5} step={0.1} style={inp} value={selectedClip.fadeOut||0} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { fadeOut:+e.target.value })}/></div>
            </div>
            <div style={{ display:'flex', gap:6, marginBottom:12 }}>
              <button onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { flipH: !selectedClip.flipH })}
                style={{ flex:1, padding:'7px', backgroundColor: selectedClip.flipH ? ACCENT : BG3, color: selectedClip.flipH ? '#000' : 'white', border:'none', borderRadius:6, cursor:'pointer', fontSize:11, display:'flex', alignItems:'center', justifyContent:'center', gap:4 }}>
                <FlipHorizontal size={13}/> Miroir
              </button>
              <button onClick={splitAtPlayhead}
                style={{ flex:1, padding:'7px', backgroundColor:BG3, color:'white', border:'none', borderRadius:6, cursor:'pointer', fontSize:11, display:'flex', alignItems:'center', justifyContent:'center', gap:4 }}>
                <Scissors size={13}/> Couper ici
              </button>
            </div>
            <div style={row}>
              <label style={lbl}>Rotation</label>
              <div style={{ display:'flex', gap:4 }}>
                {[0,90,180,270].map(r => (
                  <button key={r} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { rotation:r })}
                    style={{ flex:1, padding:'5px', fontSize:11, border:'none', borderRadius:5, cursor:'pointer', backgroundColor:(selectedClip.rotation||0)===r ? ACCENT : BG3, color:(selectedClip.rotation||0)===r ? '#000' : 'white' }}>
                    {r}°
                  </button>
                ))}
              </div>
            </div>
            {/* Ken Burns — photos uniquement */}
            {selectedMedia?.type === 'image' && (
              <div style={row}>
                <label style={lbl}>Ken Burns</label>
                <div style={{ display:'flex', gap:6, marginBottom:6 }}>
                  <button onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { kenBurns: { ...(selectedClip.kenBurns||{}), enabled: !selectedClip.kenBurns?.enabled } })}
                    style={{ flex:1, padding:'7px', backgroundColor: selectedClip.kenBurns?.enabled ? ACCENT : BG3, color: selectedClip.kenBurns?.enabled ? '#000' : 'white', border:'none', borderRadius:6, cursor:'pointer', fontSize:11, fontWeight:'bold' }}>
                    {selectedClip.kenBurns?.enabled ? '✓ Actif' : 'Désactivé'}
                  </button>
                </div>
                {selectedClip.kenBurns?.enabled && (
                  <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:4 }}>
                    {[['zoom-in','Zoom ▶'],['zoom-out','Zoom ◀'],['left-right','→ Glisse'],['right-left','← Glisse']].map(([type,label]) => (
                      <button key={type} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { kenBurns: { ...(selectedClip.kenBurns||{}), type } })}
                        style={{ padding:'6px', backgroundColor:(selectedClip.kenBurns?.type||'zoom-in')===type ? ACCENT : BG3, color:(selectedClip.kenBurns?.type||'zoom-in')===type ? '#000' : 'white', border:'none', borderRadius:6, cursor:'pointer', fontSize:10, fontWeight:'bold' }}>
                        {label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            {/* Recadrage (Crop) */}
            <div style={row}>
              <label style={lbl}>Recadrage</label>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:6 }}>
                {[['cropLeft','Gauche'],['cropRight','Droite'],['cropTop','Haut'],['cropBottom','Bas']].map(([key,label]) => {
                  const val = Math.round((selectedClip[key]||0)*100);
                  return (
                    <div key={key}>
                      <div style={{ fontSize:9, color:'rgba(255,255,255,0.35)', marginBottom:2 }}>{label} {val}%</div>
                      <input type="range" min={0} max={45} step={1} value={val} style={sl}
                        onChange={e => updateClip(selectedClip.id, selectedTrackId, { [key]: +e.target.value/100 })}
                        onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { [key]: selectedClip[key]||0 })}/>
                    </div>
                  );
                })}
              </div>
              {(selectedClip.cropLeft||selectedClip.cropRight||selectedClip.cropTop||selectedClip.cropBottom) ? (
                <button onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { cropLeft:0, cropRight:0, cropTop:0, cropBottom:0 })}
                  style={{ marginTop:4, fontSize:9, color:'rgba(255,255,255,0.3)', background:'none', border:'none', cursor:'pointer' }}>Reset crop</button>
              ) : null}
            </div>

            {/* Chroma Key + Stabilisation */}
            {ttype === 'video' && (
              <div style={row}>
                <label style={lbl}>Outils avancés</label>
                {clipFxMsg && (
                  <div style={{ padding:'6px 10px', borderRadius:7, marginBottom:8, backgroundColor: clipFxMsg.startsWith('✓') ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)', border:`1px solid ${clipFxMsg.startsWith('✓') ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`, fontSize:11, color: clipFxMsg.startsWith('✓') ? '#4ade80' : '#f87171' }}>{clipFxMsg}</div>
                )}
                <div style={{ display:'flex', gap:6 }}>
                  <button onClick={() => applyChromakey()} disabled={clipFxBusy}
                    style={{ flex:1, padding:'8px 4px', backgroundColor:BG3, border:'1px solid rgba(255,255,255,0.1)', borderRadius:7, color: clipFxBusy ? 'rgba(255,255,255,0.3)' : 'white', cursor: clipFxBusy ? 'wait' : 'pointer', fontSize:10, fontWeight:'bold', display:'flex', alignItems:'center', justifyContent:'center', gap:4 }}>
                    🟢 Fond vert
                  </button>
                  {selectedMedia?.type === 'video' && (
                    <button onClick={applyStabilize} disabled={clipFxBusy}
                      style={{ flex:1, padding:'8px 4px', backgroundColor:BG3, border:'1px solid rgba(255,255,255,0.1)', borderRadius:7, color: clipFxBusy ? 'rgba(255,255,255,0.3)' : 'white', cursor: clipFxBusy ? 'wait' : 'pointer', fontSize:10, fontWeight:'bold', display:'flex', alignItems:'center', justifyContent:'center', gap:4 }}>
                      📐 Stabiliser
                    </button>
                  )}
                </div>
                {selectedMedia?.type === 'image' && (
                  <button onClick={async () => {
                    if (!selectedMedia?.serverFilename) return;
                    setClipFxBusy(true); setClipFxMsg('Suppression arrière-plan…');
                    try {
                      const r = await fetch(`${S()}/api/studio/montage/rembg`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ filename: selectedMedia.serverFilename, username: cu() }) });
                      const d = await r.json();
                      if (!r.ok || !d.url) throw new Error(d.error || 'Erreur');
                      const nm = { ...selectedMedia, id: genId(), url: d.url, serverFilename: d.filename, name: `rembg_${selectedMedia.name}` };
                      setMediaItems(prev => [...prev, nm]);
                      commitUpdate(selectedClipId, selectedTrackId, { mediaId: nm.id });
                      setClipFxMsg('✓ Fond supprimé');
                      setTimeout(() => setClipFxMsg(null), 3000);
                    } catch (e: any) { setClipFxMsg('Erreur : ' + e.message); }
                    finally { setClipFxBusy(false); }
                  }} disabled={clipFxBusy}
                    style={{ marginTop:6, width:'100%', padding:'8px', backgroundColor:BG3, border:'1px solid rgba(255,255,255,0.1)', borderRadius:7, color: clipFxBusy ? 'rgba(255,255,255,0.3)' : 'white', cursor: clipFxBusy ? 'wait' : 'pointer', fontSize:10, fontWeight:'bold' }}>
                    🤖 Supprimer fond (IA)
                  </button>
                )}
              </div>
            )}
          </>)}

          {/* VIDEO — Color */}
          {(ttype === 'video') && activeInspTab === 'color' && (<>
            {[{key:'brightness',label:'Luminosité',min:-100,max:100,step:1,dflt:0},{key:'contrast',label:'Contraste',min:0.1,max:3,step:0.05,dflt:1},{key:'saturation',label:'Saturation',min:0,max:3,step:0.05,dflt:1},{key:'hue',label:'Teinte',min:-180,max:180,step:1,dflt:0}].map(({key,label,min,max,step,dflt}) => {
              const val = selectedClip.colorAdj?.[key] ?? dflt;
              return (
                <div key={key} style={row}>
                  <label style={lbl}>{label} — {typeof val==='number' ? (key==='brightness'?val.toFixed(0):val.toFixed(2)) : val}</label>
                  <input type="range" min={min} max={max} step={step} value={val} style={sl}
                    onChange={e => updateClip(selectedClip.id, selectedTrackId, { colorAdj:{...selectedClip.colorAdj,[key]:+e.target.value} })}
                    onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { colorAdj:selectedClip.colorAdj })} />
                  <button onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { colorAdj:{...selectedClip.colorAdj,[key]:dflt} })}
                    style={{ float:'right', fontSize:9, color:'rgba(255,255,255,0.25)', background:'none', border:'none', cursor:'pointer' }}>Reset</button>
                </div>
              );
            })}
          </>)}

          {/* VIDEO — Filters */}
          {(ttype === 'video') && activeInspTab === 'filters' && (
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:5 }}>
              {FILTERS.map(f => {
                const active = selectedClip.filters?.includes(f.id);
                return (
                  <div key={f.id} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { filters:[f.id] })}
                    style={{ cursor:'pointer', borderRadius:7, overflow:'hidden', border: active ? `2px solid ${ACCENT}` : '2px solid transparent' }}>
                    <div style={{ height:48, backgroundColor:BG3, display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden' }}>
                      {selectedMedia?.thumbnail ? <img src={selectedMedia.thumbnail} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', filter:f.css }}/> : <div style={{ width:'100%', height:'100%', background:'linear-gradient(135deg,#334,#112)', filter:f.css }}/>}
                    </div>
                    <div style={{ padding:'3px 0', textAlign:'center', fontSize:9, color: active ? ACCENT : 'rgba(255,255,255,0.4)', fontWeight: active ? 'bold' : 'normal' }}>{f.label}</div>
                  </div>
                );
              })}
            </div>
          )}

          {/* VIDEO — Animation */}
          {(ttype === 'video') && activeInspTab === 'animation' && (<>
            <div style={row}>
              <label style={lbl}>Entrée</label>
              <select style={{ ...inp, appearance:'none' }} value={selectedClip.animation?.in||'none'} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { animation:{...selectedClip.animation, in:e.target.value} })}>
                {['none','fade','slide_left','slide_right','slide_up','zoom_in','bounce'].map(a => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
            <div style={row}>
              <label style={lbl}>Sortie</label>
              <select style={{ ...inp, appearance:'none' }} value={selectedClip.animation?.out||'none'} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { animation:{...selectedClip.animation, out:e.target.value} })}>
                {['none','fade','slide_left','slide_right','slide_up','zoom_out'].map(a => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
          </>)}

          {/* VIDEO — Keyframes */}
          {(ttype === 'video') && activeInspTab === 'keyframes' && (
            <div>
              <div style={{ fontSize:9, color:'rgba(255,255,255,0.25)', marginBottom:10, lineHeight:1.5 }}>
                Positionne la tête de lecture, règle la valeur, clique ◇ pour ancrer un keyframe.
              </div>
              {KF_PROPS.map(({ prop, label, min, max, step, dflt }) => {
                const kfs: any[] = ((selectedClip.keyframes || {})[prop] || []).sort((a:any, b:any) => a.t - b.t);
                const relT = Math.max(0, currentTime - selectedClip.startTime);
                const actualDflt =
                  prop === 'kfPosX'     ? (selectedClip.posX     ?? dflt) :
                  prop === 'kfPosY'     ? (selectedClip.posY     ?? dflt) :
                  prop === 'kfScaleX'   ? (selectedClip.scaleX   ?? dflt) :
                  prop === 'kfScaleY'   ? (selectedClip.scaleY   ?? dflt) :
                  prop === 'kfRotation' ? (selectedClip.rotation ?? dflt) :
                  dflt;
                const curVal = kfs.length ? getKFValue(kfs, relT, actualDflt) : actualDflt;
                const hasKFs = kfs.length > 0;
                return (
                  <div key={prop} style={{ marginBottom:10, paddingBottom:10, borderBottom:'1px solid rgba(255,255,255,0.05)' }}>
                    <div style={{ display:'flex', alignItems:'center', gap:6, marginBottom:4 }}>
                      <span style={{ flex:1, fontSize:9, fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.08em', color: hasKFs ? ACCENT : 'rgba(255,255,255,0.4)' }}>
                        {label} — {curVal.toFixed(2)}
                      </span>
                      <button onClick={() => addKFAtCurrentTime(selectedClip.id, selectedTrackId, prop, curVal, selectedClip)}
                        title="Ajouter keyframe à la position actuelle"
                        style={{ width:20, height:20, backgroundColor: hasKFs ? ACCENT : BG4, color: hasKFs ? '#000' : 'rgba(255,255,255,0.4)', border:`1px solid ${hasKFs ? ACCENT : 'rgba(255,255,255,0.15)'}`, borderRadius:4, cursor:'pointer', fontSize:11, display:'flex', alignItems:'center', justifyContent:'center', fontWeight:'bold', flexShrink:0 }}>
                        ◇
                      </button>
                      {hasKFs && (
                        <button onClick={() => clearKF(selectedClip.id, selectedTrackId, prop)}
                          style={{ fontSize:9, color:'rgba(255,255,255,0.2)', background:'none', border:'none', cursor:'pointer', padding:'0 2px' }}>✕</button>
                      )}
                    </div>
                    <input type="range" min={min} max={max} step={step} value={curVal} style={{ width:'100%', accentColor:ACCENT }}
                      onChange={e => addKFAtCurrentTime(selectedClip.id, selectedTrackId, prop, +e.target.value, selectedClip)}/>
                    {kfs.length > 0 && (
                      <div style={{ display:'flex', flexWrap:'wrap', gap:3, marginTop:5 }}>
                        {kfs.map((kf:any, ki:number) => (
                          <div key={ki} onClick={() => setCurrentTime(selectedClip.startTime + kf.t)}
                            style={{ display:'flex', alignItems:'center', gap:3, backgroundColor:BG4, borderRadius:4, padding:'2px 5px', border:`1px solid rgba(255,255,255,0.1)`, cursor:'pointer', fontSize:9 }}>
                            <span style={{ color:ACCENT }}>◆</span>
                            <span style={{ color:'rgba(255,255,255,0.5)' }}>{kf.t.toFixed(1)}s</span>
                            <span style={{ color:'white' }}>{(+kf.value).toFixed(2)}</span>
                            <button onClick={e => { e.stopPropagation(); removeKF(selectedClip.id, selectedTrackId, prop, ki); }}
                              style={{ background:'none', border:'none', color:'rgba(255,255,255,0.25)', cursor:'pointer', padding:0, fontSize:9 }}>✕</button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
              <div style={{ fontSize:9, color:'rgba(255,255,255,0.2)', marginTop:4 }}>
                Position/Échelle/Rotation : prévisualisation. Opacité : incluse à l'export.
              </div>
            </div>
          )}

          {/* VIDEO — Masque */}
          {(ttype === 'video') && activeInspTab === 'mask' && (
            <div>
              <div style={{ fontSize:9, color:'rgba(255,255,255,0.25)', marginBottom:10 }}>
                Applique un masque de forme au clip.
              </div>
              <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:6 }}>
                {MASKS.map(m => {
                  const active = (selectedClip.mask || 'none') === m.id;
                  return (
                    <button key={m.id} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { mask: m.id })}
                      style={{ padding:'8px 4px', backgroundColor: active ? ACCENT : BG3, border:`1px solid ${active ? ACCENT : 'rgba(255,255,255,0.1)'}`, borderRadius:7, color: active ? '#000' : 'white', cursor:'pointer', fontSize:10, fontWeight:'bold', textAlign:'center' }}>
                      {m.label}
                    </button>
                  );
                })}
              </div>
              {/* Prévisualisation du masque */}
              <div style={{ marginTop:12, display:'flex', justifyContent:'center' }}>
                <div style={{ width:80, height:80, backgroundColor:'rgba(254,44,85,0.3)', clipPath: MASKS.find(m => m.id === (selectedClip.mask||'none'))?.css || 'none', border:'1px solid rgba(254,44,85,0.4)', borderRadius:4 }}/>
              </div>
              {/* Sous-titres IA — accessible depuis masque car lié aux clips vidéo */}
              {ttype === 'video' && selectedMedia?.type === 'video' && (
                <div style={{ marginTop:14, borderTop:'1px solid rgba(255,255,255,0.07)', paddingTop:12 }}>
                  <label style={{ fontSize:9, fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.08em', color:'rgba(255,255,255,0.4)', display:'block', marginBottom:6 }}>Sous-titres automatiques IA</label>
                  {clipFxMsg && (
                    <div style={{ padding:'6px 10px', borderRadius:7, marginBottom:8, backgroundColor: clipFxMsg.startsWith('✓') ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)', border:`1px solid ${clipFxMsg.startsWith('✓') ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`, fontSize:10, color: clipFxMsg.startsWith('✓') ? '#4ade80' : '#f87171' }}>{clipFxMsg}</div>
                  )}
                  <button onClick={autoSubtitles} disabled={clipFxBusy}
                    style={{ width:'100%', padding:'9px', backgroundColor: clipFxBusy ? BG3 : 'rgba(254,44,85,0.15)', border:`1px solid ${clipFxBusy ? 'rgba(255,255,255,0.08)' : 'rgba(254,44,85,0.4)'}`, borderRadius:7, color: clipFxBusy ? 'rgba(255,255,255,0.3)' : 'white', cursor: clipFxBusy ? 'wait' : 'pointer', fontSize:11, fontWeight:'bold' }}>
                    {clipFxBusy ? '⏳ Transcription…' : '🎙️ Générer les sous-titres'}
                  </button>
                  <div style={{ fontSize:9, color:'rgba(255,255,255,0.2)', marginTop:5, lineHeight:1.4 }}>Requiert Whisper (pip install openai-whisper)</div>
                </div>
              )}
            </div>
          )}

          {/* TEXT */}
          {ttype === 'text' && selectedClip.textStyle && (<>
            <div style={row}>
              <label style={lbl}>Contenu</label>
              <textarea style={{ ...inp, resize:'vertical', minHeight:56 }} value={selectedClip.textStyle.content}
                onChange={e => updateClip(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, content:e.target.value} })}
                onBlur={() => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:selectedClip.textStyle })}/>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:12 }}>
              <div>
                <label style={lbl}>Police</label>
                <select style={{ ...inp, appearance:'none' }} value={selectedClip.textStyle.font} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, font:e.target.value} })}>
                  {TEXT_FONTS.map(f => <option key={f} value={f}>{f}</option>)}
                </select>
              </div>
              <div>
                <label style={lbl}>Taille — {selectedClip.textStyle.fontSize}px</label>
                <input type="range" min={10} max={200} step={2} value={selectedClip.textStyle.fontSize} style={sl}
                  onChange={e => updateClip(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, fontSize:+e.target.value} })}
                  onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:selectedClip.textStyle })}/>
              </div>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:12 }}>
              <div><label style={lbl}>Couleur texte</label><input type="color" value={selectedClip.textStyle.color} style={{ ...inp, height:34, padding:2, cursor:'pointer' }} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, color:e.target.value} })}/></div>
              <div>
                <label style={lbl}>Fond — {Math.round(selectedClip.textStyle.bgOpacity*100)}%</label>
                <div style={{ display:'flex', gap:4 }}>
                  <input type="color" value={selectedClip.textStyle.bgColor} style={{ flex:'0 0 34px', height:34, backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:5, padding:2, cursor:'pointer' }} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, bgColor:e.target.value} })}/>
                  <input type="range" min={0} max={1} step={0.05} value={selectedClip.textStyle.bgOpacity} style={{ flex:1, accentColor:ACCENT }} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, bgOpacity:+e.target.value} })}/>
                </div>
              </div>
            </div>
            <div style={{ display:'flex', gap:4, marginBottom:12 }}>
              {[{field:'bold',icon:<Bold size={13}/>},{field:'italic',icon:<Italic size={13}/>}].map(({field,icon}) => (
                <button key={field} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, [field]:!selectedClip.textStyle[field]} })}
                  style={{ flex:1, padding:'6px', backgroundColor: selectedClip.textStyle[field] ? ACCENT : BG3, color: selectedClip.textStyle[field] ? '#000' : 'white', border:'none', borderRadius:6, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center' }}>
                  {icon}
                </button>
              ))}
              {[{a:'left',i:<AlignLeft size={13}/>},{a:'center',i:<AlignCenter size={13}/>},{a:'right',i:<AlignRight size={13}/>}].map(({a,i}) => (
                <button key={a} onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, align:a} })}
                  style={{ flex:1, padding:'6px', backgroundColor: selectedClip.textStyle.align===a ? ACCENT : BG3, color: selectedClip.textStyle.align===a ? '#000' : 'white', border:'none', borderRadius:6, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center' }}>
                  {i}
                </button>
              ))}
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:12 }}>
              <div>
                <label style={lbl}>Contour — {selectedClip.textStyle.outlineWidth}px</label>
                <input type="range" min={0} max={10} step={1} value={selectedClip.textStyle.outlineWidth} style={sl}
                  onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, outlineWidth:+e.target.value} })}/>
              </div>
              <div><label style={lbl}>Couleur contour</label><input type="color" value={selectedClip.textStyle.outlineColor} style={{ ...inp, height:34, padding:2, cursor:'pointer' }} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, outlineColor:e.target.value} })}/></div>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:12 }}>
              <div>
                <label style={lbl}>Position X — {selectedClip.textStyle.x}%</label>
                <input type="range" min={0} max={100} value={selectedClip.textStyle.x} style={sl}
                  onChange={e => updateClip(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, x:+e.target.value} })}
                  onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:selectedClip.textStyle })}/>
              </div>
              <div>
                <label style={lbl}>Position Y — {selectedClip.textStyle.y}%</label>
                <input type="range" min={0} max={100} value={selectedClip.textStyle.y} style={sl}
                  onChange={e => updateClip(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, y:+e.target.value} })}
                  onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:selectedClip.textStyle })}/>
              </div>
            </div>
            <div style={row}>
              <label style={lbl}>Animation</label>
              <select style={{ ...inp, appearance:'none' }} value={selectedClip.textStyle.animation} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { textStyle:{...selectedClip.textStyle, animation:e.target.value} })}>
                {TEXT_ANIMS.map(a => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
          </>)}

          {/* AUDIO */}
          {ttype === 'audio' && (<>
            <div style={row}>
              <label style={lbl}>Volume — {Math.round((selectedClip.volume||1)*100)}%</label>
              <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                <button onClick={() => commitUpdate(selectedClip.id, selectedTrackId, { isMuted:!selectedClip.isMuted })}
                  style={{ background:'none', border:'none', cursor:'pointer', color: selectedClip.isMuted ? '#f87171' : 'rgba(255,255,255,0.4)', padding:4 }}>
                  {selectedClip.isMuted ? <VolumeX size={15}/> : <Volume2 size={15}/>}
                </button>
                <input type="range" min={0} max={2} step={0.05} value={selectedClip.volume||1} style={sl}
                  onChange={e => updateClip(selectedClip.id, selectedTrackId, { volume:+e.target.value })}
                  onMouseUp={() => commitUpdate(selectedClip.id, selectedTrackId, { volume:selectedClip.volume })}/>
              </div>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:12 }}>
              <div><label style={lbl}>Fondu début (s)</label><input type="number" min={0} max={10} step={0.1} style={inp} value={selectedClip.fadeIn||0} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { fadeIn:+e.target.value })}/></div>
              <div><label style={lbl}>Fondu fin (s)</label><input type="number" min={0} max={10} step={0.1} style={inp} value={selectedClip.fadeOut||0} onChange={e => commitUpdate(selectedClip.id, selectedTrackId, { fadeOut:+e.target.value })}/></div>
            </div>
            {selectedMedia && (
              <button onClick={async () => {
                try {
                  const r = await fetch(`${S()}/api/studio/montage/extract-audio`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ filename:selectedMedia.serverFilename, username:cu() }) });
                  const d = await r.json();
                  if (d.url) await downloadFile(S()+d.url, `audio_${selectedMedia.name}.mp3`);
                } catch { showToast('Erreur extraction audio'); }
              }}
                style={{ width:'100%', padding:'8px', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:8, color:'white', cursor:'pointer', fontSize:12, display:'flex', alignItems:'center', justifyContent:'center', gap:6 }}>
                <Mic size={14}/> Extraire l'audio (MP3)
              </button>
            )}
          </>)}
        </div>
      </div>
    );
  }

  function renderProjectSettings() {
    return (
      <div style={{ display:'flex', flexDirection:'column', height:'100%', backgroundColor:BG1, borderLeft:`1px solid rgba(255,255,255,0.08)`, padding:12, overflowY:'auto' }}>
        <div style={{ fontSize:10, color:'rgba(255,255,255,0.35)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.08em', marginBottom:10 }}>Paramètres du projet</div>
        <div style={{ marginBottom:10 }}>
          <label style={{ display:'block', fontSize:10, color:'rgba(255,255,255,0.4)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:4 }}>Nom</label>
          <input value={project.name} onChange={e => setProject(p => ({...p, name:e.target.value}))} style={{ width:'100%', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:6, padding:'6px 8px', color:'white', fontSize:12, boxSizing:'border-box' }}/>
        </div>
        <div style={{ marginBottom:10 }}>
          <label style={{ display:'block', fontSize:10, color:'rgba(255,255,255,0.4)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:4 }}>Résolution</label>
          <div style={{ display:'flex', flexWrap:'wrap', gap:4 }}>
            {RESOLUTIONS.map(r => <button key={r} onClick={() => setProject(p => ({...p, resolution:r}))} style={{ padding:'4px 8px', borderRadius:5, fontSize:11, border:'none', cursor:'pointer', backgroundColor: project.resolution===r ? ACCENT : BG3, color: project.resolution===r ? '#000' : 'white', fontWeight: project.resolution===r ? 'bold' : 'normal' }}>{r}</button>)}
          </div>
        </div>
        <div style={{ marginBottom:10 }}>
          <label style={{ display:'block', fontSize:10, color:'rgba(255,255,255,0.4)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:4 }}>FPS</label>
          <div style={{ display:'flex', gap:4 }}>
            {FRAMERATES.map(f => <button key={f} onClick={() => setProject(p => ({...p, fps:f}))} style={{ flex:1, padding:'5px 2px', borderRadius:5, fontSize:11, border:'none', cursor:'pointer', backgroundColor: project.fps===f ? ACCENT : BG3, color: project.fps===f ? '#000' : 'white' }}>{f}</button>)}
          </div>
        </div>
        <div style={{ marginBottom:10 }}>
          <label style={{ display:'block', fontSize:10, color:'rgba(255,255,255,0.4)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:4 }}>Format</label>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:4 }}>
            {ASPECT_RATIOS.map(ar => <button key={ar.id} onClick={() => setProject(p => ({...p, aspectRatio:ar.id}))} style={{ padding:'7px 4px', borderRadius:7, border: project.aspectRatio===ar.id ? `2px solid ${ACCENT}` : '1px solid rgba(255,255,255,0.08)', cursor:'pointer', backgroundColor: project.aspectRatio===ar.id ? 'rgba(34,211,238,0.08)' : BG3 }}><div style={{ fontSize:12, color: project.aspectRatio===ar.id ? ACCENT : 'white', fontWeight:'bold' }}>{ar.label}</div><div style={{ fontSize:9, color:'rgba(255,255,255,0.35)', marginTop:1 }}>{ar.desc}</div></button>)}
          </div>
        </div>
        <div style={{ marginBottom:12 }}>
          <label style={{ display:'block', fontSize:10, color:'rgba(255,255,255,0.4)', fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:4 }}>Fond</label>
          <input type="color" value={project.bgColor} onChange={e => setProject(p => ({...p, bgColor:e.target.value}))} style={{ width:'100%', height:34, backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:6, padding:2, cursor:'pointer' }}/>
        </div>
        <button onClick={() => { loadGallery(); setShowGallery(true); }}
          style={{ width:'100%', padding:'8px', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:8, color:'white', cursor:'pointer', fontSize:12, display:'flex', alignItems:'center', justifyContent:'center', gap:6 }}>
          <Grid size={14}/> Voir la galerie des montages
        </button>
      </div>
    );
  }

  function renderTimeline() {
    const LABEL_W = 120;
    const marks = rulerMarks(totalDur, zoom);
    const tlWidth = totalDur * PX * zoom;
    const phLeft = currentTime * PX * zoom;

    return (
      <div style={{ display:'flex', flexDirection:'column', height:'100%', backgroundColor:BG1, borderTop:`1px solid rgba(255,255,255,0.08)`, userSelect:'none' }}>
        {/* Toolbar */}
        <div style={{ height:32, display:'flex', alignItems:'center', padding:'0 8px', gap:6, borderBottom:`1px solid rgba(255,255,255,0.06)`, flexShrink:0 }}>
          <button onClick={() => setZoom(z => Math.max(0.05, z * 0.7))} style={{ padding:'3px', backgroundColor:BG3, border:'none', borderRadius:4, color:'white', cursor:'pointer', flexShrink:0 }}><ZoomOut size={12}/></button>
          <input type="range" min={0.05} max={8} step={0.01} value={zoom}
            onChange={e => setZoom(+e.target.value)}
            style={{ width:90, accentColor:ACCENT, cursor:'pointer', flexShrink:0 }}/>
          <div style={{ fontSize:10, color:'rgba(255,255,255,0.3)', minWidth:30, textAlign:'center', flexShrink:0 }}>{Math.round(zoom*100)}%</div>
          <button onClick={() => setZoom(z => Math.min(8, z * 1.4))} style={{ padding:'3px', backgroundColor:BG3, border:'none', borderRadius:4, color:'white', cursor:'pointer', flexShrink:0 }}><ZoomIn size={12}/></button>
          <button onClick={() => setZoom(1)} style={{ padding:'2px 5px', backgroundColor:BG3, border:'none', borderRadius:4, color:'rgba(255,255,255,0.35)', cursor:'pointer', fontSize:9, flexShrink:0 }}>1:1</button>
          <div style={{ flex:1 }}/>
          <button onClick={splitAtPlayhead} style={{ padding:'3px 7px', backgroundColor:BG3, border:'none', borderRadius:4, color:'rgba(255,255,255,0.5)', cursor:'pointer', fontSize:10, display:'flex', alignItems:'center', gap:3 }}><Scissors size={11}/> Couper</button>
          <button onClick={addTextClip} style={{ padding:'3px 7px', backgroundColor:BG3, border:'none', borderRadius:4, color:'rgba(255,255,255,0.5)', cursor:'pointer', fontSize:10, display:'flex', alignItems:'center', gap:3 }}><Type size={11}/> Texte</button>
          <button onClick={autoCutByBeats} disabled={beatLoading}
            title="Auto-découpe au rythme de la musique"
            style={{ padding:'3px 7px', backgroundColor: beatLoading ? 'rgba(254,44,85,0.2)' : BG3, border:'none', borderRadius:4, color: beatLoading ? ACCENT : 'rgba(255,255,255,0.5)', cursor: beatLoading ? 'wait' : 'pointer', fontSize:10, display:'flex', alignItems:'center', gap:3 }}>
            {beatLoading ? <><Loader2 size={11} style={{ animation:'spin 1s linear infinite' }}/> Beats…</> : <><Zap size={11}/> Auto-cut</>}
          </button>
          <button onClick={() => { askConfirm('Nouveau projet ? Tout sera effacé.', () => { const fresh = [ {id:'video-0',type:'video',clips:[],locked:false,muted:false},{id:'audio-0',type:'audio',clips:[],locked:false,muted:false},{id:'text-0',type:'text',clips:[],locked:false,muted:false},{id:'effect-0',type:'effect',clips:[],locked:false,muted:false} ]; setTracks(fresh); setMediaItems([]); setCurrentTime(0); setSelectedClipId(null); setSelectedTrackId(null); setHistory([]); setHistoryIdx(-1); localStorage.removeItem(`lea_montage_${cu()}`); }); }} style={{ padding:'3px 7px', backgroundColor:'rgba(239,68,68,0.12)', border:'1px solid rgba(239,68,68,0.25)', borderRadius:4, color:'rgba(239,68,68,0.7)', cursor:'pointer', fontSize:10 }}>✕ Nouveau</button>
        </div>

        {/* Body */}
        <div style={{ flex:1, display:'flex', minHeight:0, overflowX:'hidden', overflowY:'auto' }}>
          {/* Labels */}
          <div style={{ width:LABEL_W, flexShrink:0, backgroundColor:BG0, borderRight:`1px solid rgba(255,255,255,0.06)` }}>
            <div style={{ height:24, borderBottom:`1px solid rgba(255,255,255,0.05)` }}/>
            {tracks.map(track => (
              <div key={track.id} style={{ height:TRACK_H[track.type]||64, borderBottom:`1px solid rgba(255,255,255,0.04)`, display:'flex', alignItems:'center', padding:'0 5px', gap:3 }}>
                <div style={{ flex:1, fontSize:10, color: track.muted ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.55)', fontWeight:'bold', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{TRACK_CFG[track.type]?.label}</div>
                {track.type === 'audio' && (
                  <button
                    onClick={() => setTracks(prev => { const s = syncAudioToVideo(prev, mediaItems); pushHistory(s); return s; })}
                    title="Sync musique → durée vidéo"
                    style={{ padding:2, background:'none', border:'none', cursor:'pointer', color:'rgba(110,231,183,0.6)', borderRadius:3, fontSize:11, lineHeight:1 }}>
                    ⟳
                  </button>
                )}
                <button onClick={() => toggleTrack(track.id, 'muted')} style={{ padding:2, background:'none', border:'none', cursor:'pointer', color: track.muted ? '#f87171' : 'rgba(255,255,255,0.25)', borderRadius:3 }}>
                  {track.muted ? <VolumeX size={10}/> : <Volume2 size={10}/>}
                </button>
                <button onClick={() => toggleTrack(track.id, 'locked')} style={{ padding:2, background:'none', border:'none', cursor:'pointer', color: track.locked ? ACCENT : 'rgba(255,255,255,0.25)', borderRadius:3 }}>
                  {track.locked ? <Lock size={10}/> : <Unlock size={10}/>}
                </button>
              </div>
            ))}
          </div>

          {/* Scroll area */}
          <div ref={tlRef} style={{ flex:1, overflowX:'auto', overflowY:'hidden', position:'relative', touchAction:'pan-x' }}
            onPointerMove={handlePointerMove} onPointerUp={handlePointerUp} onPointerLeave={handlePointerUp}
            onTouchStart={e => {
              if (e.touches.length === 2) {
                const dx = e.touches[0].clientX - e.touches[1].clientX;
                const dy = e.touches[0].clientY - e.touches[1].clientY;
                pinchRef.current = { dist: Math.hypot(dx, dy), zoom };
              }
            }}
            onTouchMove={e => {
              if (e.touches.length === 2 && pinchRef.current) {
                e.preventDefault();
                const dx = e.touches[0].clientX - e.touches[1].clientX;
                const dy = e.touches[0].clientY - e.touches[1].clientY;
                const newDist = Math.hypot(dx, dy);
                const ratio = newDist / pinchRef.current.dist;
                setZoom(Math.min(8, Math.max(0.05, pinchRef.current.zoom * ratio)));
              }
            }}
            onTouchEnd={() => { pinchRef.current = null; }}>
            <div style={{ width: Math.max(tlWidth + 200, 1000), position:'relative', minHeight:'100%' }}>
              {/* Ruler */}
              <div style={{ height:24, borderBottom:`1px solid rgba(255,255,255,0.05)`, cursor:'crosshair', position:'relative', backgroundColor:BG0 }}
                onClick={handleRulerClick}>
                {marks.filter(m => m.major).map((m,i) => (
                  <div key={i} style={{ position:'absolute', left: m.t * PX * zoom, top:0, bottom:0 }}>
                    <div style={{ position:'absolute', top:0, bottom:0, left:0, width:1, backgroundColor:'rgba(255,255,255,0.08)' }}/>
                    <span style={{ position:'absolute', top:6, left:3, fontSize:9, color:'rgba(255,255,255,0.35)', whiteSpace:'nowrap' }}>{fmtDur(m.t)}</span>
                  </div>
                ))}
                {marks.filter(m => !m.major).map((m,i) => (
                  <div key={`min${i}`} style={{ position:'absolute', left: m.t * PX * zoom, top:'60%', width:1, height:'40%', backgroundColor:'rgba(255,255,255,0.05)' }}/>
                ))}
                <div style={{ position:'absolute', left:phLeft, top:0, bottom:0, width:2, backgroundColor:ACCENT, zIndex:5, pointerEvents:'none' }}>
                  <div style={{ position:'absolute', top:0, left:-5, width:12, height:12, clipPath:'polygon(50% 100%, 0 0, 100% 0)', backgroundColor:ACCENT }}/>
                </div>
              </div>

              {/* Tracks */}
              {tracks.map(track => (
                <div key={track.id}
                  style={{ position:'relative', height:TRACK_H[track.type]||64, borderBottom:`1px solid rgba(255,255,255,0.04)`, backgroundColor: track.locked ? 'rgba(255,200,100,0.01)' : 'transparent' }}
                  onPointerMove={handlePointerMove} onPointerUp={handlePointerUp}
                  onDragOver={e => e.preventDefault()}
                  onDrop={e => {
                    e.preventDefault();
                    if (track.locked) return;
                    const mediaId = e.dataTransfer.getData('mediaId');
                    const media = mediaItems.find(m => m.id === mediaId);
                    if (!media) return;
                    const rect = tlRef.current?.getBoundingClientRect() || { left:0 };
                    const x = e.clientX - rect.left + (tlRef.current?.scrollLeft || 0);
                    const t = Math.max(0, x / (PX * zoom));
                    setTracks(prev => { const nt = prev.map(tr => tr.id !== track.id ? tr : { ...tr, clips:[...tr.clips, { ...makeClip(media, t) }] }); pushHistory(nt); return nt; });
                  }}
                >
                  {/* Séparateur sous-lignes + labels pour la piste vidéo */}
                  {track.type === 'video' && <>
                    <div style={{ position:'absolute', left:0, right:0, top:SUB_ROW_H, height:1, backgroundColor:'rgba(255,255,255,0.07)', pointerEvents:'none', zIndex:1 }}/>
                    <span style={{ position:'absolute', right:4, top:3, fontSize:7, color:'rgba(255,255,255,0.18)', pointerEvents:'none', userSelect:'none' }}>🎬 vidéo</span>
                    <span style={{ position:'absolute', right:4, top:SUB_ROW_H+3, fontSize:7, color:'rgba(124,205,252,0.4)', pointerEvents:'none', userSelect:'none' }}>🖼 photos</span>
                  </>}
                  {(() => {
                    const sorted = [...track.clips].sort((a,b) => a.startTime - b.startTime);
                    const th = TRACK_H[track.type] || 64;
                    return sorted.map((clip, idx) => {
                      const media = mediaItems.find(m => m.id === clip.mediaId);
                      const nextClip = sorted[idx + 1];
                      const hasTransition = nextClip?.transitionIn?.type && nextClip.transitionIn.type !== 'none';
                      return (
                        <React.Fragment key={clip.id}>
                          <ClipBlock clip={clip} track={track} media={media} zoom={zoom}
                            isSelected={selectedClipId === clip.id}
                            onMoveStart={track.locked ? ()=>{} : handleMoveStart}
                            onTrimStart={track.locked ? ()=>{} : handleTrimStart}
                            onSelect={(cid, tid) => { setSelectedClipId(cid); setSelectedTrackId(tid); setActiveInspTab('properties'); }}
                            onDelete={deleteClip}
                          />
                          {/* Carré de transition entre ce clip et le suivant */}
                          {nextClip && (track.type === 'video') && (() => {
                            const junctionX = clip.endTime * PX * zoom;
                            const btnSize = 22;
                            const nextMedia = mediaItems.find(m => m.id === nextClip.mediaId);
                            // Sur la piste vidéo : carré au bord des sous-lignes si type différent, sinon centré dans la sous-ligne
                            const crossType = track.type === 'video' && media?.type !== nextMedia?.type;
                            const btnY = track.type === 'video'
                              ? (crossType ? SUB_ROW_H - btnSize/2 : media?.type === 'image' ? SUB_ROW_H + (SUB_ROW_H - btnSize)/2 : (SUB_ROW_H - btnSize)/2)
                              : th/2 - btnSize/2;
                            return (
                              <button
                                key={`tr-${clip.id}`}
                                onPointerDown={e => e.stopPropagation()}
                                onClick={e => {
                                  e.stopPropagation();
                                  const rect = e.currentTarget.getBoundingClientRect();
                                  setTransitionPicker({ trackId: track.id, clipId: nextClip.id, x: rect.left, y: rect.top }); setTrPopupPos(null);
                                }}
                                style={{
                                  position:'absolute',
                                  left: junctionX - btnSize/2,
                                  top: btnY,
                                  width: btnSize, height: btnSize,
                                  borderRadius:5,
                                  backgroundColor: hasTransition ? ACCENT : 'rgba(255,255,255,0.12)',
                                  border: hasTransition ? `1.5px solid ${ACCENT}` : '1.5px solid rgba(255,255,255,0.25)',
                                  color: hasTransition ? '#000' : 'rgba(255,255,255,0.55)',
                                  cursor:'pointer', fontSize:10, fontWeight:'bold',
                                  display:'flex', alignItems:'center', justifyContent:'center',
                                  zIndex:6, lineHeight:1, padding:0,
                                }}>
                                {hasTransition
                                  ? (getTransition(nextClip.transitionIn?.type)?.icon || 'T')
                                  : '⧖'}
                              </button>
                            );
                          })()}
                        </React.Fragment>
                      );
                    });
                  })()}
                </div>
              ))}

              {/* Playhead */}
              <div style={{ position:'absolute', left:phLeft, top:0, bottom:0, width:2, backgroundColor:ACCENT, zIndex:8, pointerEvents:'none', opacity:0.85 }}/>
            </div>
          </div>
        </div>
      </div>
    );
  }

  function renderTransitionPicker() {
    if (!transitionPicker) return null;
    const { trackId, clipId, x, y } = transitionPicker;
    const clip = tracks.find(t => t.id === trackId)?.clips.find(c => c.id === clipId);
    const current = clip?.transitionIn?.type || 'none';
    const isMobile = window.innerWidth < 520;
    const POPUP_W = isMobile
      ? Math.min(500, window.innerWidth - 2)
      : Math.min(580, window.innerWidth - 20);
    const POPUP_H = isMobile
      ? Math.min(720, window.innerHeight - 30)
      : Math.min(720, window.innerHeight - 40);
    const px = Math.floor((window.innerWidth - POPUP_W) / 2);
    const py = isMobile
      ? window.innerHeight - POPUP_H - 52
      : (y - POPUP_H - 8 < 8 ? y + 32 : y - POPUP_H - 8);

    const finalX = trPopupPos ? trPopupPos.x : px;
    const finalY = trPopupPos ? trPopupPos.y : py;

    function startDrag(e: any) {
      e.preventDefault();
      const sx = e.touches ? e.touches[0].clientX : e.clientX;
      const sy = e.touches ? e.touches[0].clientY : e.clientY;
      const ox = finalX; const oy = finalY;
      function onMove(me: any) {
        const cx = me.touches ? me.touches[0].clientX : me.clientX;
        const cy = me.touches ? me.touches[0].clientY : me.clientY;
        setTrPopupPos({
          x: Math.max(0, Math.min(ox + cx - sx, window.innerWidth  - POPUP_W)),
          y: Math.max(0, Math.min(oy + cy - sy, window.innerHeight - POPUP_H)),
        });
      }
      function onEnd() {
        window.removeEventListener('mousemove', onMove);
        window.removeEventListener('mouseup',   onEnd);
        window.removeEventListener('touchmove', onMove as any);
        window.removeEventListener('touchend',  onEnd);
      }
      window.addEventListener('mousemove', onMove);
      window.addEventListener('mouseup',   onEnd);
      window.addEventListener('touchmove', onMove as any, { passive: false });
      window.addEventListener('touchend',  onEnd);
    }

    const filtered = ALL_TRANSITIONS.filter(tr =>
      trSearch ? tr.label.toLowerCase().includes(trSearch.toLowerCase()) || tr.id.toLowerCase().includes(trSearch.toLowerCase())
               : tr.category === trCat
    );

    function applyTransition(type) {
      setTracks(prev => {
        const nt = prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c => c.id !== clipId ? c : { ...c, transitionIn: { ...c.transitionIn, type } }) });
        pushHistory(nt);
        return nt;
      });
    }

    // Couleurs des previews CSS
    const FROM_COLOR = '#1a2744';
    const TO_COLOR   = '#3b1f1a';

    function TransitionThumb({ tr }) {
      const active = current === tr.id;
      const favd   = isFav(tr.id, 'transition');
      const [thumbUrl, setThumbUrl] = React.useState<string>(() => getThumbUrlSync(tr.id));
      React.useEffect(() => {
        if (thumbUrl) return;
        requestTransitionThumb(tr.id, setThumbUrl);
      }, [tr.id]);
      return (
        <div style={{ position:'relative' }}>
          <button onClick={() => applyTransition(tr.id)}
            style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:2, padding:'4px', borderRadius:7, backgroundColor: active ? 'rgba(254,44,85,0.15)' : 'transparent', border: active ? `2px solid ${ACCENT}` : '2px solid rgba(255,255,255,0.05)', cursor:'pointer', width:'100%' }}>
            <div style={{ width:'100%', height:38, borderRadius:4, overflow:'hidden', position:'relative', backgroundColor:BG4 }}>
              {thumbUrl
                ? <img src={thumbUrl} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', borderRadius:3 }}/>
                : <div style={{ width:'100%', height:'100%', background:'linear-gradient(135deg,rgba(30,58,110,0.5),rgba(110,30,26,0.5))' }}/>
              }
              {active && <div style={{ position:'absolute', top:2, left:2, width:6, height:6, borderRadius:'50%', backgroundColor:ACCENT }}/>}
            </div>
            <span style={{ fontSize:7, fontWeight:'bold', color: active ? ACCENT : 'rgba(255,255,255,0.55)', textAlign:'center', lineHeight:1.2, width:'100%', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{tr.label}</span>
          </button>
          <button onClick={e => { e.stopPropagation(); toggleFav({ key:tr.id, type:'transition', label:tr.label, thumbUrl }); }}
            style={{ position:'absolute', top:5, right:5, background:'none', border:'none', cursor:'pointer', padding:1, color: favd ? '#f59e0b' : 'rgba(255,255,255,0.2)', lineHeight:1, zIndex:2 }}>
            <Star size={9} fill={favd ? '#f59e0b' : 'none'}/>
          </button>
        </div>
      );
    }

    return (
      <>
        <div style={{ position:'fixed', inset:0, zIndex:199 }} onClick={() => { setTransitionPicker(null); setTrSearch(''); }}/>
        <div style={{ position:'fixed', left:finalX, top:finalY, width:POPUP_W, height:POPUP_H, backgroundColor:BG1, borderRadius:isMobile ? '16px 16px 0 0' : 16, border:`1px solid rgba(255,255,255,0.1)`, boxShadow:'0 -8px 60px rgba(0,0,0,0.9)', zIndex:200, display:'flex', flexDirection:'column', overflow:'hidden' }}
          onClick={e => e.stopPropagation()}>

          {/* Header — draggable */}
          <div onMouseDown={startDrag} onTouchStart={startDrag}
            style={{ display:'flex', alignItems:'center', padding:'12px 14px 8px', borderBottom:`1px solid rgba(255,255,255,0.07)`, flexShrink:0, cursor:'grab', userSelect:'none' }}>
            <span style={{ fontSize:11, color:'rgba(255,255,255,0.2)', marginRight:6 }}>⠿</span>
            <span style={{ fontSize:13, fontWeight:'bold', color:'white', flex:1 }}>Transitions</span>
            <span style={{ fontSize:10, color:'rgba(255,255,255,0.35)', marginRight:8 }}>{ALL_TRANSITIONS.length}</span>
            <button onMouseDown={e => e.stopPropagation()} onClick={() => { setTransitionPicker(null); setTrSearch(''); }} style={{ background:'none', border:'none', color:'rgba(255,255,255,0.4)', cursor:'pointer', padding:2, fontSize:15, lineHeight:1 }}>✕</button>
          </div>

          {/* Durée */}
          <div style={{ display:'flex', alignItems:'center', gap:8, padding:'8px 14px 6px', borderBottom:`1px solid rgba(255,255,255,0.05)`, flexShrink:0 }}>
            <span style={{ fontSize:10, color:'rgba(255,255,255,0.45)' }}>Durée</span>
            <input type="range" min={0.1} max={2} step={0.1} value={clip?.transitionIn?.duration||0.5}
              onChange={e => setTracks(prev => prev.map(t => t.id !== trackId ? t : { ...t, clips: t.clips.map(c => c.id !== clipId ? c : { ...c, transitionIn: { ...c.transitionIn, duration: +e.target.value } }) }))}
              style={{ flex:1, accentColor:ACCENT }}/>
            <span style={{ fontSize:10, color:ACCENT, fontWeight:'bold', minWidth:24 }}>{clip?.transitionIn?.duration?.toFixed(1)||'0.5'}s</span>
          </div>

          {/* Preview WebGL temps réel */}
          {!isMobile && (
            <div style={{ padding:'6px 12px 4px', flexShrink:0 }}>
              <div style={{ height:76, borderRadius:8, overflow:'hidden', backgroundColor:BG4, border:`1px solid rgba(255,255,255,0.07)` }}>
                {current !== 'none'
                  ? <TransitionPreviewCanvas transId={current} />
                  : <div style={{ width:'100%', height:'100%', display:'flex', alignItems:'center', justifyContent:'center', color:'rgba(255,255,255,0.15)', fontSize:10 }}>Choisissez une transition</div>
                }
              </div>
              {current !== 'none' && <div style={{ fontSize:9, color:ACCENT, textAlign:'center', marginTop:3, fontWeight:'bold' }}>{getTransition(current)?.label}</div>}
            </div>
          )}

          {/* Recherche */}
          <div style={{ padding:'6px 10px', flexShrink:0 }}>
            <input value={trSearch} onChange={e => setTrSearch(e.target.value)} placeholder="🔍 Rechercher..."
              style={{ width:'100%', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.1)`, borderRadius:8, color:'white', fontSize:11, padding:'6px 10px', outline:'none', boxSizing:'border-box' }}/>
          </div>

          {/* Menu déroulant catégories */}
          {!trSearch && (
            <div style={{ padding:'4px 10px 6px', flexShrink:0 }}>
              <select value={trCat} onChange={e => setTrCat(e.target.value)}
                style={{ width:'100%', backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.12)`, borderRadius:8, color:'white', fontSize:12, padding:'7px 10px', outline:'none', cursor:'pointer', appearance:'none', WebkitAppearance:'none' }}>
                {TRANSITION_CATEGORIES.map(cat => {
                  const cnt = ALL_TRANSITIONS.filter(t => t.category === cat).length;
                  return <option key={cat} value={cat} style={{ backgroundColor:BG1 }}>{cat} ({cnt})</option>;
                })}
              </select>
            </div>
          )}

          {/* Grille responsive */}
          <div style={{ flex:1, overflowY:'auto', overflowX:'hidden', padding:'4px 8px 12px', display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:4, alignContent:'start' }}>
            {/* Aucune transition */}
            {(!trSearch || 'aucun'.includes(trSearch.toLowerCase())) && (
              <button onClick={() => applyTransition('none')}
                style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:2, padding:'4px', borderRadius:8, backgroundColor: current==='none' ? 'rgba(254,44,85,0.15)' : 'transparent', border: current==='none' ? `2px solid ${ACCENT}` : '2px solid transparent', cursor:'pointer', width:'100%' }}>
                <div style={{ width:'100%', height:38, borderRadius:4, overflow:'hidden', position:'relative', backgroundColor:BG4, display:'flex', alignItems:'center', justifyContent:'center' }}>
                  <X size={14} color="rgba(255,255,255,0.4)"/>
                </div>
                <span style={{ fontSize:7, fontWeight:'bold', color: current==='none' ? ACCENT : 'rgba(255,255,255,0.5)' }}>Aucune</span>
              </button>
            )}
            {filtered.map(tr => <TransitionThumb key={tr.id} tr={tr}/>)}
          </div>
        </div>
      </>
    );
  }

  function renderSonPanel() {
    const audioTrack  = tracks.find(t => t.type === 'audio');
    const clip        = audioTrack?.clips.find(c => c.id === selectedClipId) || audioTrack?.clips[0] || null;
    const media       = clip ? mediaItems.find(m => m.id === clip.mediaId) : null;
    const noClip      = !clip;

    const TOOLS = [
      { id:'volume',    icon:'🔊', label:'Volume'       },
      { id:'diviser',   icon:'✂️',  label:'Diviser'      },
      { id:'vitesse',   icon:'⚡',  label:'Vitesse'      },
      { id:'dupliquer', icon:'📋', label:'Dupliquer'    },
      { id:'effets',    icon:'🎛️', label:'Effets'       },
      { id:'voix',      icon:'🎤', label:'Voix+'        },
      { id:'isolation', icon:'🎙️', label:'Isolation'    },
      { id:'bruit',     icon:'🔇', label:'Sans bruit'   },
    ];

    const btn = (label: string, onClick: ()=>void, disabled = false, active = false) => (
      <button onClick={onClick} disabled={disabled || audioFxBusy}
        style={{ flex:1, padding:'10px 4px', backgroundColor: active ? ACCENT : BG3, border:`1px solid rgba(255,255,255,${active?'0.3':'0.1'})`, borderRadius:8, color: active ? '#000' : disabled?'rgba(255,255,255,0.3)':'white', cursor: disabled?'default':'pointer', fontSize:12, fontWeight:'bold' }}>
        {label}
      </button>
    );

    return (
      <div style={{ display:'flex', flexDirection:'column', height:'100%', backgroundColor:BG1, padding:12, gap:8, overflowY:'auto' }}>
        <div style={{ fontSize:11, fontWeight:'bold', color:'rgba(255,255,255,0.5)', marginBottom:4 }}>🎵 Outils son{noClip ? ' — aucun clip audio sélectionné' : ''}</div>

        {/* 8 boutons outils */}
        <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:6 }}>
          {TOOLS.map(t => (
            <button key={t.id} onClick={() => setSonSubTool(prev => prev === t.id ? null : t.id)}
              style={{ padding:'10px 4px', backgroundColor: sonSubTool===t.id ? 'rgba(254,44,85,0.18)' : BG3, border:`1.5px solid ${sonSubTool===t.id ? ACCENT : 'rgba(255,255,255,0.1)'}`, borderRadius:8, color: sonSubTool===t.id ? ACCENT : 'rgba(255,255,255,0.7)', cursor:'pointer', fontSize:10, display:'flex', flexDirection:'column', alignItems:'center', gap:3 }}>
              <span style={{ fontSize:18 }}>{t.icon}</span>
              <span style={{ fontWeight:'bold', fontSize:9, lineHeight:1.2, textAlign:'center' }}>{t.label}</span>
            </button>
          ))}
        </div>

        {/* Message status */}
        {audioFxMsg && <div style={{ padding:'7px 10px', borderRadius:8, backgroundColor: audioFxMsg.startsWith('✓') ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)', border:`1px solid ${audioFxMsg.startsWith('✓') ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`, fontSize:12, color: audioFxMsg.startsWith('✓') ? '#4ade80' : '#f87171', textAlign:'center' }}>{audioFxMsg}</div>}
        {audioFxBusy && <div style={{ textAlign:'center', fontSize:11, color:ACCENT }}>⏳ Traitement en cours…</div>}

        {/* Sous-panneau Volume */}
        {sonSubTool === 'volume' && clip && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Volume — {Math.round((clip.volume||1)*100)}%</div>
            <input type="range" min={0} max={2} step={0.05} value={clip.volume||1} style={{ width:'100%', accentColor:ACCENT }}
              onChange={e => updateClip(clip.id, audioTrack!.id, { volume:+e.target.value })}
              onPointerUp={() => commitUpdate(clip.id, audioTrack!.id, { volume:clip.volume })}/>
            <div style={{ display:'flex', gap:6, marginTop:8 }}>
              {btn('Muet', () => commitUpdate(clip.id, audioTrack!.id, { isMuted:!clip.isMuted }), false, clip.isMuted)}
              {btn('Max (200%)', () => commitUpdate(clip.id, audioTrack!.id, { volume:2 }))}
              {btn('Normal', () => commitUpdate(clip.id, audioTrack!.id, { volume:1 }))}
            </div>
          </div>
        )}

        {/* Sous-panneau Diviser */}
        {sonSubTool === 'diviser' && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Divise le clip à la position du curseur ({fmtTimecode(currentTime, project.fps)})</div>
            <button onClick={() => { splitAtPlayhead(); setSonSubTool(null); }} disabled={noClip}
              style={{ width:'100%', padding:'10px', backgroundColor: noClip ? BG3 : ACCENT, border:'none', borderRadius:8, color: noClip ? 'rgba(255,255,255,0.3)' : '#000', cursor: noClip ? 'default' : 'pointer', fontWeight:'bold', fontSize:13 }}>
              ✂️ Diviser ici
            </button>
          </div>
        )}

        {/* Sous-panneau Vitesse */}
        {sonSubTool === 'vitesse' && clip && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Vitesse — {clip.speed||1}x</div>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:6 }}>
              {[0.5, 0.75, 1, 1.25, 1.5, 2, 3, 4].map(s => (
                <button key={s} onClick={() => commitUpdate(clip.id, audioTrack!.id, { speed:s })}
                  style={{ padding:'8px', backgroundColor:(clip.speed||1)===s ? ACCENT : BG3, border:'none', borderRadius:6, color:(clip.speed||1)===s ? '#000' : 'white', cursor:'pointer', fontWeight:'bold', fontSize:12 }}>
                  {s}x
                </button>
              ))}
            </div>
            <div style={{ fontSize:10, color:'rgba(255,255,255,0.3)', marginTop:8 }}>Applique aussi via Serveur pour un rendu permanent →</div>
            {btn('🚀 Appliquer (permanent)', () => applyAudioFx('speed', { speed: clip.speed||1 }))}
          </div>
        )}

        {/* Sous-panneau Dupliquer */}
        {sonSubTool === 'dupliquer' && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Duplique le clip à la suite</div>
            <button onClick={() => { duplicateAudioClip(); setSonSubTool(null); }} disabled={noClip}
              style={{ width:'100%', padding:'10px', backgroundColor: noClip ? BG3 : ACCENT, border:'none', borderRadius:8, color: noClip ? 'rgba(255,255,255,0.3)' : '#000', cursor: noClip ? 'default' : 'pointer', fontWeight:'bold', fontSize:13 }}>
              📋 Dupliquer
            </button>
          </div>
        )}

        {/* Sous-panneau Effets */}
        {sonSubTool === 'effets' && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Effets audio (traitement FFmpeg permanent)</div>
            <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
              {[
                { id:'reverb',  label:'🌊 Réverbération', desc:'Espace / salle' },
                { id:'echo',    label:'📡 Écho',           desc:'Répétition sonore' },
                { id:'chorus',  label:'🎶 Chorus',         desc:'Effet chœur / richesse' },
              ].map(fx => (
                <button key={fx.id} onClick={() => applyAudioFx(fx.id)} disabled={noClip || audioFxBusy}
                  style={{ padding:'10px 12px', backgroundColor: noClip ? BG3 : 'rgba(255,255,255,0.06)', border:'1px solid rgba(255,255,255,0.12)', borderRadius:8, color: noClip ? 'rgba(255,255,255,0.3)' : 'white', cursor: noClip ? 'default' : 'pointer', textAlign:'left' }}>
                  <div style={{ fontWeight:'bold', fontSize:12 }}>{fx.label}</div>
                  <div style={{ fontSize:10, color:'rgba(255,255,255,0.4)', marginTop:2 }}>{fx.desc}</div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Sous-panneau Voix off / Améliorer */}
        {sonSubTool === 'voix' && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, fontWeight:'bold', color:'rgba(255,255,255,0.6)', marginBottom:10 }}>🎤 Enregistrement voix off</div>
            {!isRecording ? (
              <button onClick={startVoiceRecording}
                style={{ width:'100%', padding:'14px', backgroundColor:ACCENT, border:'none', borderRadius:8, color:'#000', fontWeight:'bold', fontSize:14, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
                <Mic size={16}/> Enregistrer
              </button>
            ) : (
              <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:10 }}>
                <div style={{ display:'flex', alignItems:'center', gap:8, color:'#f87171', fontSize:14, fontWeight:'bold' }}>
                  <div style={{ width:10, height:10, borderRadius:'50%', backgroundColor:'#f87171', animation:'spin 1s linear infinite' }}/>
                  {Math.floor(recordingTime/60).toString().padStart(2,'0')}:{(recordingTime%60).toString().padStart(2,'0')}
                </div>
                <button onClick={stopVoiceRecording}
                  style={{ padding:'10px 24px', backgroundColor:'rgba(239,68,68,0.15)', border:'1px solid rgba(239,68,68,0.4)', borderRadius:8, color:'#ef4444', fontWeight:'bold', cursor:'pointer', fontSize:13 }}>
                  ⬛ Arrêter &amp; Importer
                </button>
              </div>
            )}
            <div style={{ marginTop:12, borderTop:'1px solid rgba(255,255,255,0.06)', paddingTop:10, fontSize:10, color:'rgba(255,255,255,0.3)' }}>Améliorer la voix (FFmpeg)</div>
            <button onClick={() => applyAudioFx('enhance_voice')} disabled={noClip || audioFxBusy}
              style={{ marginTop:6, width:'100%', padding:'9px', backgroundColor: noClip ? BG3 : 'rgba(255,255,255,0.07)', border:'1px solid rgba(255,255,255,0.12)', borderRadius:8, color: noClip ? 'rgba(255,255,255,0.3)' : 'white', cursor: noClip ? 'default' : 'pointer', fontSize:12 }}>
              🎙️ Filtre voix / podcast
            </button>
          </div>
        )}

        {/* Sous-panneau Isolation voix */}
        {sonSubTool === 'isolation' && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Isole les fréquences vocales (250Hz – 4kHz)</div>
            <div style={{ fontSize:10, color:'rgba(255,255,255,0.3)', marginBottom:10 }}>Filtre fréquentiel : atténue la musique, garde la voix. Pour une séparation parfaite, utilise le moteur IA (lourd).</div>
            <button onClick={() => applyAudioFx('isolate_voice')} disabled={noClip || audioFxBusy}
              style={{ width:'100%', padding:'12px', backgroundColor: noClip ? BG3 : ACCENT, border:'none', borderRadius:8, color: noClip ? 'rgba(255,255,255,0.3)' : '#000', cursor: noClip ? 'default' : 'pointer', fontWeight:'bold', fontSize:13 }}>
              🎙️ Isoler la voix
            </button>
          </div>
        )}

        {/* Sous-panneau Réduire le bruit */}
        {sonSubTool === 'bruit' && (
          <div style={{ backgroundColor:BG2, borderRadius:10, padding:12 }}>
            <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:8 }}>Réduction du bruit de fond (FFT)</div>
            <div style={{ fontSize:10, color:'rgba(255,255,255,0.3)', marginBottom:10 }}>Filtre afftdn : supprime le souffle, le bruit de ventilateur, les parasites constants.</div>
            <button onClick={() => applyAudioFx('denoise')} disabled={noClip || audioFxBusy}
              style={{ width:'100%', padding:'12px', backgroundColor: noClip ? BG3 : ACCENT, border:'none', borderRadius:8, color: noClip ? 'rgba(255,255,255,0.3)' : '#000', cursor: noClip ? 'default' : 'pointer', fontWeight:'bold', fontSize:13 }}>
              🔇 Réduire le bruit
            </button>
          </div>
        )}
      </div>
    );
  }

  function renderExportModal() {
    if (!showExport) return null;
    return (
      <div style={{ position:'fixed', inset:0, backgroundColor:'rgba(0,0,0,0.85)', zIndex:100, display:'flex', alignItems:'center', justifyContent:'center' }} onClick={() => !exporting && setShowExport(false)}>
        <div onClick={e => e.stopPropagation()} style={{ backgroundColor:BG2, borderRadius:16, padding:24, width:Math.min(380, window.innerWidth-32), border:`1px solid rgba(255,255,255,0.1)`, boxShadow:'0 16px 64px rgba(0,0,0,0.8)' }}>
          <div style={{ fontSize:15, fontWeight:'bold', color:'white', marginBottom:4 }}>🎬 Exporter le montage</div>
          <div style={{ fontSize:11, color:'rgba(255,255,255,0.4)', marginBottom:18 }}>Rendu serveur via FFmpeg — {project.resolution} • {project.fps}fps • {project.aspectRatio}</div>
          <div style={{ marginBottom:16, display:'flex', flexDirection:'column', gap:7 }}>
            {[['Clips vidéo',tracks.find(t=>t.type==='video')?.clips.length||0],['Clips texte',tracks.find(t=>t.type==='text')?.clips.length||0],['Clips audio',tracks.find(t=>t.type==='audio')?.clips.length||0]].map(([k,v]) => (
              <div key={k} style={{ display:'flex', justifyContent:'space-between', fontSize:12 }}>
                <span style={{ color:'rgba(255,255,255,0.5)' }}>{k}</span>
                <span style={{ color:'white' }}>{v}</span>
              </div>
            ))}
          </div>
          {exportedUrl ? (<>
            <div style={{ padding:'10px', backgroundColor:'rgba(34,197,94,0.08)', border:'1px solid rgba(34,197,94,0.25)', borderRadius:8, marginBottom:14, fontSize:12, color:'#4ade80', textAlign:'center' }}>✅ Montage exporté avec succès !</div>
            <button onClick={() => downloadFile(exportedUrl, `${project.name}.mp4`)}
              style={{ width:'100%', padding:'10px', backgroundColor:ACCENT, border:'none', borderRadius:8, color:'#000', fontWeight:'bold', cursor:'pointer', fontSize:13, display:'flex', alignItems:'center', justifyContent:'center', gap:6, marginBottom:10 }}>
              <Download size={15}/> Télécharger
            </button>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr 1fr', gap:6 }}>
              {[{id:'tiktok',icon:<TikTokSVG/>,label:'TikTok',bg:'#111'},{id:'instagram',icon:<IgSVG/>,label:'Insta',bg:'linear-gradient(135deg,#833ab4,#fd1d1d,#fcb045)'},{id:'youtube',icon:<YtSVG/>,label:'YouTube',bg:'#ff0000'},{id:'facebook',icon:<FbSVG/>,label:'Facebook',bg:'#1877f2'}].map(soc => (
                <button key={soc.id} onClick={() => shareToSocial(soc.id)}
                  style={{ padding:'8px 3px', background:soc.bg, border:'none', borderRadius:8, cursor:'pointer', color:'white', fontSize:9, display:'flex', flexDirection:'column', alignItems:'center', gap:3 }}>
                  {soc.icon} {soc.label}
                </button>
              ))}
            </div>
          </>) : (
            <button onClick={exportProject} disabled={exporting}
              style={{ width:'100%', padding:'12px', backgroundColor: exporting ? 'rgba(34,211,238,0.25)' : ACCENT, border:'none', borderRadius:8, color:'#000', fontWeight:'bold', cursor: exporting ? 'wait' : 'pointer', fontSize:14, display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
              {exporting ? <><Loader2 size={15} style={{ animation:'spin 1s linear infinite' }}/> Rendu en cours...</> : '🚀 Lancer l\'export'}
            </button>
          )}
        </div>
      </div>
    );
  }

  function renderGalleryModal() {
    if (!showGallery) return null;
    return (
      <div style={{ position:'fixed', inset:0, backgroundColor:'rgba(0,0,0,0.95)', zIndex:100, display:'flex', flexDirection:'column' }}>
        <div style={{ display:'flex', alignItems:'center', padding:'12px 16px', borderBottom:`1px solid rgba(255,255,255,0.08)` }}>
          <div style={{ fontSize:14, fontWeight:'bold', color:'white', flex:1 }}>🎬 Mes montages</div>
          <button onClick={() => { loadGallery(); }} style={{ padding:'5px 10px', backgroundColor:BG3, border:'none', borderRadius:6, color:'rgba(255,255,255,0.5)', cursor:'pointer', fontSize:11, marginRight:8 }}>↻ Actualiser</button>
          <button onClick={() => setShowGallery(false)} style={{ background:'none', border:'none', color:'white', cursor:'pointer', padding:4 }}><X size={18}/></button>
        </div>
        <div style={{ flex:1, overflowY:'auto', padding:12 }}>
          {gallery.length === 0 ? (
            <div style={{ textAlign:'center', color:'rgba(255,255,255,0.15)', padding:40, fontSize:13 }}>
              <Film size={36} style={{ display:'block', margin:'0 auto 8px' }}/> Aucun montage exporté
            </div>
          ) : (
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(200px, 1fr))', gap:12 }}>
              {gallery.map(v => (
                <div key={v.filename} style={{ backgroundColor:BG2, borderRadius:10, overflow:'hidden', border:`1px solid rgba(255,255,255,0.07)` }}>
                  <video src={S()+v.url} style={{ width:'100%', height:110, objectFit:'cover', cursor:'pointer' }} muted playsInline onClick={e => { const vid = e.currentTarget; vid.paused ? vid.play() : vid.pause(); }} />
                  <div style={{ padding:'8px 10px' }}>
                    <div style={{ fontSize:11, color:'white', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{v.filename}</div>
                    <div style={{ fontSize:9, color:'rgba(255,255,255,0.3)', marginTop:2 }}>{(v.size/1024/1024).toFixed(1)} Mo</div>
                    <div style={{ display:'flex', gap:6, marginTop:8 }}>
                      <button onClick={() => downloadFile(S()+v.url, v.filename)}
                        style={{ flex:1, padding:'5px', backgroundColor:ACCENT, border:'none', borderRadius:6, cursor:'pointer', color:'#000', fontSize:11, fontWeight:'bold', display:'flex', alignItems:'center', justifyContent:'center', gap:3 }}>
                        <Download size={11}/> DL
                      </button>
                      <button onClick={async () => { await fetch(`${S()}/api/studio/montage/${cu()}/${v.filename}`,{method:'DELETE'}); setGallery(p => p.filter(g => g.filename !== v.filename)); }}
                        style={{ flex:1, padding:'5px', backgroundColor:'rgba(239,68,68,0.1)', border:'1px solid rgba(239,68,68,0.25)', borderRadius:6, cursor:'pointer', color:'#ef4444', fontSize:11, display:'flex', alignItems:'center', justifyContent:'center' }}>
                        <Trash2 size={11}/>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  // ══════════════════════════════════════════════════════════════════════════
  // MAIN RENDER
  // ══════════════════════════════════════════════════════════════════════════
  const topBar = (
    <div style={{ height:46, display:'flex', alignItems:'center', padding:'0 10px', gap:8, backgroundColor:BG0, borderBottom:`1px solid rgba(255,255,255,0.06)`, flexShrink:0 }}>
      <Film size={15} style={{ color:ACCENT, flexShrink:0 }}/>
      <input value={project.name} onChange={e => setProject(p => ({...p, name:e.target.value}))}
        style={{ backgroundColor:'transparent', border:'none', color:'white', fontWeight:'bold', fontSize:13, outline:'none', minWidth:0, flex:1, maxWidth:160, cursor:'text' }}/>
      <div style={{ flex:1 }}/>
      {isRecording && (
        <div style={{ display:'flex', alignItems:'center', gap:5, padding:'4px 10px', backgroundColor:'rgba(239,68,68,0.15)', border:'1px solid rgba(239,68,68,0.35)', borderRadius:8 }}>
          <div style={{ width:7, height:7, borderRadius:'50%', backgroundColor:'#ef4444', animation:'spin 1s linear infinite' }}/>
          <span style={{ fontSize:10, color:'#f87171', fontWeight:'bold' }}>
            REC {Math.floor(recordingTime/60).toString().padStart(2,'0')}:{(recordingTime%60).toString().padStart(2,'0')}
          </span>
          <button onClick={stopVoiceRecording} style={{ background:'none', border:'none', color:'#f87171', cursor:'pointer', fontSize:9, padding:'0 2px' }}>■</button>
        </div>
      )}
      <select value={project.resolution} onChange={e => setProject(p => ({...p, resolution:e.target.value}))}
        style={{ backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.08)`, borderRadius:6, color:'white', fontSize:11, padding:'3px 5px', cursor:'pointer' }}>
        {RESOLUTIONS.map(r => <option key={r} value={r}>{r}</option>)}
      </select>
      <select value={project.fps} onChange={e => setProject(p => ({...p, fps:+e.target.value}))}
        style={{ backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.08)`, borderRadius:6, color:'white', fontSize:11, padding:'3px 5px', cursor:'pointer' }}>
        {FRAMERATES.map(f => <option key={f} value={f}>{f}fps</option>)}
      </select>
      <select value={project.aspectRatio} onChange={e => setProject(p => ({...p, aspectRatio:e.target.value}))}
        style={{ backgroundColor:BG3, border:`1px solid rgba(255,255,255,0.08)`, borderRadius:6, color:'white', fontSize:11, padding:'3px 5px', cursor:'pointer' }}>
        {ASPECT_RATIOS.map(a => <option key={a.id} value={a.id}>{a.label} — {a.desc}</option>)}
      </select>
      <button onClick={() => { setShowExport(true); setExportedUrl(null); }}
        style={{ padding:'6px 14px', backgroundColor:ACCENT, border:'none', borderRadius:7, color:'#000', fontWeight:'bold', cursor:'pointer', fontSize:12, display:'flex', alignItems:'center', gap:5, flexShrink:0 }}>
        {exporting ? <><Loader2 size={12} style={{ animation:'spin 1s linear infinite' }}/> Rendu...</> : <><Film size={12}/> Exporter</>}
      </button>
      {onClose && (
        <button
          onClick={onClose}
          title="Fermer le Studio"
          style={{ width:30, height:30, display:'flex', alignItems:'center', justifyContent:'center', backgroundColor:'rgba(255,255,255,0.06)', border:'1px solid rgba(255,255,255,0.10)', borderRadius:7, color:'rgba(255,255,255,0.55)', cursor:'pointer', flexShrink:0, transition:'background 0.15s, color 0.15s' }}
          onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'rgba(239,68,68,0.18)'; e.currentTarget.style.color = '#ef4444'; }}
          onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.06)'; e.currentTarget.style.color = 'rgba(255,255,255,0.55)'; }}
        >
          <X size={14}/>
        </button>
      )}
    </div>
  );

  // Hauteur exacte de la timeline : toolbar(32) + ruler(24) + somme des pistes actives
  const TL_H = 32 + 24 + tracks.reduce((s, t) => s + (TRACK_H[t.type] || 64), 0);

  return (
    <div style={{ position:'fixed', top:0, left:0, right:0, bottom:0, display:'flex', flexDirection:'column', backgroundColor:BG0, overflow:'hidden', zIndex:50, fontFamily:'system-ui, -apple-system, sans-serif' }}>
      <ConfirmToastHost />
      <style>{`
        @keyframes spin { from{transform:rotate(0deg)}to{transform:rotate(360deg)} }
        ::-webkit-scrollbar{width:4px;height:4px}
        ::-webkit-scrollbar-track{background:transparent}
        ::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.12);border-radius:2px}
        input[type=range]::-webkit-slider-thumb{background:${ACCENT}}
        select option{background:#1a1a1a;color:white}

        /* ── Keyframes transitions picker ── */
        @keyframes tr-fade-out{0%,20%{opacity:1}80%,100%{opacity:0}}
        @keyframes tr-fade-in{0%,20%{opacity:0}80%,100%{opacity:1}}
        @keyframes tr-flash{0%,40%{opacity:1}50%{opacity:1;filter:brightness(10)}60%,100%{opacity:0}}
        @keyframes tr-slide-left-out{0%,20%{transform:translateX(0)}80%,100%{transform:translateX(-100%)}}
        @keyframes tr-slide-left-in{0%,20%{transform:translateX(100%)}80%,100%{transform:translateX(0)}}
        @keyframes tr-slide-right-out{0%,20%{transform:translateX(0)}80%,100%{transform:translateX(100%)}}
        @keyframes tr-slide-right-in{0%,20%{transform:translateX(-100%)}80%,100%{transform:translateX(0)}}
        @keyframes tr-slide-up-out{0%,20%{transform:translateY(0)}80%,100%{transform:translateY(-100%)}}
        @keyframes tr-slide-up-in{0%,20%{transform:translateY(100%)}80%,100%{transform:translateY(0)}}
        @keyframes tr-slide-down-out{0%,20%{transform:translateY(0)}80%,100%{transform:translateY(100%)}}
        @keyframes tr-slide-down-in{0%,20%{transform:translateY(-100%)}80%,100%{transform:translateY(0)}}
        @keyframes tr-wipe-left{0%,20%{clip-path:inset(0 0% 0 0)}80%,100%{clip-path:inset(0 100% 0 0)}}
        @keyframes tr-wipe-right{0%,20%{clip-path:inset(0 0 0 0%)}80%,100%{clip-path:inset(0 0 0 100%)}}
        @keyframes tr-wipe-up{0%,20%{clip-path:inset(0 0 0% 0)}80%,100%{clip-path:inset(100% 0 0 0)}}
        @keyframes tr-wipe-down{0%,20%{clip-path:inset(0% 0 0 0)}80%,100%{clip-path:inset(0 0 100% 0)}}
        @keyframes tr-horz-open{0%,20%{clip-path:inset(0 0 0 0)}80%,100%{clip-path:inset(0 50% 0 50%)}}
        @keyframes tr-horz-close{0%,20%{clip-path:inset(0 50% 0 50%)}80%,100%{clip-path:inset(0 0 0 0)}}
        @keyframes tr-vert-open{0%,20%{clip-path:inset(0 0 0 0)}80%,100%{clip-path:inset(50% 0 50% 0)}}
        @keyframes tr-vert-close{0%,20%{clip-path:inset(50% 0 50% 0)}80%,100%{clip-path:inset(0 0 0 0)}}
        @keyframes tr-diag-tl{0%,20%{clip-path:polygon(0 0,100% 0,100% 100%,0 100%)}80%,100%{clip-path:polygon(0 0,0 0,0 0,0 0)}}
        @keyframes tr-diag-tr{0%,20%{clip-path:polygon(0 0,100% 0,100% 100%,0 100%)}80%,100%{clip-path:polygon(100% 0,100% 0,100% 0,100% 0)}}
        @keyframes tr-diag-bl{0%,20%{clip-path:polygon(0 0,100% 0,100% 100%,0 100%)}80%,100%{clip-path:polygon(0 100%,0 100%,0 100%,0 100%)}}
        @keyframes tr-diag-br{0%,20%{clip-path:polygon(0 0,100% 0,100% 100%,0 100%)}80%,100%{clip-path:polygon(100% 100%,100% 100%,100% 100%,100% 100%)}}
        @keyframes tr-zoom-in-out{0%,20%{transform:scale(1);opacity:1}80%,100%{transform:scale(2);opacity:0}}
        @keyframes tr-squeeze-h{0%,20%{transform:scaleX(1)}80%,100%{transform:scaleX(0)}}
        @keyframes tr-squeeze-v{0%,20%{transform:scaleY(1)}80%,100%{transform:scaleY(0)}}
        @keyframes tr-circle-open{0%,20%{clip-path:circle(70% at 50% 50%)}80%,100%{clip-path:circle(0% at 50% 50%)}}
        @keyframes tr-circle-close{0%,20%{clip-path:circle(0% at 50% 50%)}80%,100%{clip-path:circle(70% at 50% 50%)}}
        @keyframes tr-radial{0%{clip-path:polygon(50% 50%,50% 0,50% 0,50% 0,50% 0,50% 0)}100%{clip-path:polygon(50% 50%,50% 0,100% 0,100% 100%,0 100%,0 0)}}
        @keyframes tr-rotate{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}
        @keyframes tr-polkadots{0%,20%{clip-path:circle(0% at 25% 25%)}50%{clip-path:circle(30% at 50% 50%)}80%,100%{clip-path:circle(80% at 50% 50%)}}
        @keyframes tr-cube-out{0%,20%{transform:perspective(200px) rotateY(0deg)}80%,100%{transform:perspective(200px) rotateY(-90deg);opacity:0}}
        @keyframes tr-cube-in{0%,20%{transform:perspective(200px) rotateY(90deg);opacity:0}80%,100%{transform:perspective(200px) rotateY(0deg);opacity:1}}
        @keyframes tr-page-flip{0%,20%{transform:perspective(300px) rotateY(0)}80%,100%{transform:perspective(300px) rotateY(-160deg)}}
        @keyframes tr-glitch{0%{transform:translateX(0);filter:none}33%{transform:translateX(-6px);filter:hue-rotate(90deg)}66%{transform:translateX(4px);filter:saturate(5)}100%{transform:translateX(0);filter:none}}
        @keyframes tr-swirl{0%,20%{transform:rotate(0deg) scale(1);opacity:1}80%,100%{transform:rotate(360deg) scale(0);opacity:0}}
        @keyframes tr-ripple{0%,20%{transform:scale(1);opacity:1}50%{transform:scale(1.05)}80%,100%{transform:scale(1);opacity:0}}
        @keyframes tr-burn{0%,20%{filter:brightness(1)}50%{filter:brightness(2) saturate(3) sepia(1)}80%,100%{filter:brightness(0);opacity:0}}
        @keyframes tr-blur-out{0%,20%{filter:blur(0px);opacity:1}80%,100%{filter:blur(8px);opacity:0}}
      `}</style>

      <input ref={videoInRef} id="lea-video-input" type="file" accept="video/*" multiple style={{ display:'none' }} onChange={e => { if (e.target.files?.length) handleUpload(e.target.files); }}/>
      <input ref={photoInRef} id="lea-photo-input" type="file" accept="image/*" multiple style={{ display:'none' }} onChange={e => { if (e.target.files?.length) handleUpload(e.target.files); }}/>
      <input ref={audioInRef} id="lea-audio-input" type="file" accept="audio/*" multiple style={{ display:'none' }} onChange={e => { if (e.target.files?.length) handleUpload(e.target.files); }}/>
      <audio ref={audioRef} preload="auto" style={{ display:'none' }}/>

      {topBar}

      {isDesktop ? (
        /* ── Layout desktop : position:absolute pure, zéro flex ── */
        <div style={{ position:'absolute', top:46, left:0, right:0, bottom:0, overflow:'hidden' }}>

          {/* PANNEAU MÉDIA — colonne gauche */}
          <div style={{ position:'absolute', top:0, left:0, width:220, bottom:TL_H, overflow:'hidden', borderRight:`1px solid rgba(255,255,255,0.07)` }}>
            {renderMediaPanel()}
          </div>

          {/* PRÉVISUALISATION VIDÉO — centre */}
          <div style={{ position:'absolute', top:0, left:220, right:240, bottom:TL_H, overflow:'hidden' }}>
            {renderVideoPreview()}
          </div>

          {/* INSPECTEUR — colonne droite */}
          <div style={{ position:'absolute', top:0, right:0, width:240, bottom:TL_H, overflow:'hidden', borderLeft:`1px solid rgba(255,255,255,0.07)` }}>
            {mobilePanel === 'son' ? renderSonPanel() : renderInspector()}
          </div>

          {/* TIMELINE — hauteur exacte calculée, zéro espace vide */}
          <div style={{ position:'absolute', bottom:0, left:0, right:0, height:TL_H, overflow:'hidden', borderTop:`1px solid rgba(255,255,255,0.06)` }}>
            {renderTimeline()}
          </div>

        </div>
      ) : (
        /* ── Layout mobile : position:absolute pure comme le desktop ── */
        <div style={{ position:'absolute', top:46, left:0, right:0, bottom:0, overflow:'hidden' }}>
          {/* VIDEO — haut fixe 40vh */}
          <div style={{ position:'absolute', top:0, left:0, right:0, height:'40vh', overflow:'hidden' }}>
            {renderVideoPreview()}
          </div>
          {/* TIMELINE — entre la vidéo et la toolbar */}
          <div style={{ position:'absolute', top:'40vh', left:0, right:0, bottom:52, overflow:'hidden' }}>
            {renderTimeline()}
          </div>
          {/* TOOLBAR MOBILE — bande basse 52px */}
          <div style={{ position:'absolute', bottom:0, left:0, right:0, height:52, display:'flex', alignItems:'center', gap:1, padding:'0 4px', backgroundColor:BG0, borderTop:`1px solid rgba(255,255,255,0.06)`, overflowX:'auto' }}>
            {[
              {id:'favoris',  icon:<Star size={17}/>,      label:'Favoris'},
              {id:'video',    icon:<Film size={17}/>,      label:'Vidéo'},
              {id:'photo',    icon:<ImageIcon size={17}/>, label:'Photo'},
              {id:'audio',    icon:<Music size={17}/>,     label:'Audio'},
              {id:'text',     icon:<Type size={17}/>,      label:'Texte'},
              {id:'filters',  icon:<Palette size={17}/>,   label:'Filtres'},
              {id:'stickers', icon:<Layers size={17}/>,    label:'Stickers'},
              {id:'effets',   icon:<Zap size={17}/>,       label:'Effets'},
              {id:'son',      icon:<Volume2 size={17}/>,    label:'Son'},
              {id:'inspector',icon:<Settings size={17}/>,  label:'Clip'},
              {id:'gallery',  icon:<Grid size={17}/>,      label:'Galerie'},
            ].map(tool => (
              <button key={tool.id}
                onClick={() => {
                  if (tool.id==='gallery') { loadGallery(); setShowGallery(true); return; }
                  if (tool.id==='text') { addTextClip(); setMobilePanel('inspector'); return; }
                  if (tool.id==='video') { try { videoInRef.current.value=''; videoInRef.current.click(); } catch {} setMobilePanel('media'); return; }
                  if (tool.id==='photo') { try { photoInRef.current.value=''; photoInRef.current.click(); } catch {} setMobilePanel('media'); return; }
                  if (tool.id==='audio') { try { audioInRef.current.value=''; audioInRef.current.click(); } catch {} setMobilePanel('media'); return; }
                  if (tool.id==='favoris')  { setActiveMediaTab('favoris');  setMobilePanel('media'); return; }
                  if (tool.id==='stickers') { setActiveMediaTab('stickers'); if (stickerAssets.length === 0) loadCdnAssets('stickers'); setMobilePanel('media'); return; }
                  if (tool.id==='effets')   { setActiveMediaTab('effets');   if (effectAssets.length === 0)  loadCdnAssets('effets');   setMobilePanel('media'); return; }
                  setMobilePanel(prev=>prev===tool.id?null:tool.id);
                }}
                style={{ minWidth:48, display:'flex', flexDirection:'column', alignItems:'center', gap:2, padding:'4px 3px', backgroundColor: mobilePanel===tool.id ? `rgba(254,44,85,0.12)` : 'transparent', border:'none', cursor:'pointer', borderRadius:7, color: mobilePanel===tool.id ? ACCENT : 'rgba(255,255,255,0.45)', flexShrink:0 }}>
                {tool.icon}
                <span style={{ fontSize:8, fontWeight:'bold', textTransform:'uppercase', letterSpacing:'0.04em' }}>{tool.label}</span>
              </button>
            ))}
          </div>
          {/* Bottom sheet */}
          {mobilePanel && (
            <div style={{ position:'fixed', left:0, right:0, bottom:52, height:'52vh', backgroundColor:BG2, borderTop:`1px solid rgba(255,255,255,0.1)`, borderRadius:'14px 14px 0 0', zIndex:30, display:'flex', flexDirection:'column', overflow:'hidden', boxShadow:'0 -8px 32px rgba(0,0,0,0.6)' }}>
              <div style={{ display:'flex', alignItems:'center', padding:'8px 14px 5px', borderBottom:`1px solid rgba(255,255,255,0.06)`, flexShrink:0 }}>
                <div style={{ width:32, height:4, backgroundColor:'rgba(255,255,255,0.15)', borderRadius:2 }}/>
                <div style={{ flex:1 }}/>
                <button onClick={() => setMobilePanel(null)} style={{ background:'none', border:'none', color:'rgba(255,255,255,0.4)', cursor:'pointer', padding:3 }}><X size={15}/></button>
              </div>
              <div style={{ flex:1, overflow:'auto' }}>
                {mobilePanel === 'media' && renderMediaPanel()}
                {mobilePanel === 'inspector' && renderInspector()}
                {mobilePanel === 'son' && renderSonPanel()}
                {mobilePanel === 'filters' && selectedClip && (
                  <div style={{ padding:10 }}>
                    <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:8 }}>
                      {FILTERS.map(f => {
                        const act = selectedClip?.filters?.includes(f.id);
                        return (
                          <div key={f.id} onClick={() => selectedClip && commitUpdate(selectedClip.id, selectedTrackId, { filters:[f.id] })}
                            style={{ borderRadius:7, overflow:'hidden', border: act ? `2px solid ${ACCENT}` : '2px solid transparent', cursor:'pointer' }}>
                            <div style={{ height:56, backgroundColor:BG3, filter:f.css }}/>
                            <div style={{ padding:'3px 0', textAlign:'center', fontSize:10, color: act ? ACCENT : 'rgba(255,255,255,0.45)' }}>{f.label}</div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {renderExportModal()}
      {renderGalleryModal()}
      {renderTransitionPicker()}
    </div>
  );
}
