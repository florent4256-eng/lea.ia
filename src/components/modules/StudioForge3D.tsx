import React, { useState, useRef, useEffect, useCallback } from 'react';
import { saveFile } from '../../lib/download';
import {
  Hexagon, Rotate3D, Download, Activity, Coins,
  Terminal, Sparkles, ChevronDown, Clock, RefreshCw, Film,
  Trash2, X, Cpu, Zap, Image, AlertCircle, RotateCcw,
} from 'lucide-react';

type ForgeModel = {
  url: string; preview_url: string | null; vertices: number;
  faces: number; format: string; device: string; prompt: string;
};

type HistoryItem = {
  filename: string; url: string; preview_url: string | null;
  size: number; date: string; format: string;
  prompt: string; vertices: number; faces: number;
  device: string; quality: string;
};

const SERVER   = () => (window as any).LEA_SERVER_URL || '';
const ME       = () => localStorage.getItem('lea_currentUser') || 'invité';
const fmtTime  = (s: number) => `${Math.floor(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}`;

export const StudioForge3D = () => {
  const [prompt, setPrompt]             = useState('');
  const [quality, setQuality]           = useState<'draft' | 'high'>('high');
  const [isGenerating, setIsGenerating] = useState(false);
  const [model, setModel]               = useState<ForgeModel | null>(null);
  const [userTokens, setUserTokens]     = useState<number | null>(Infinity);
  const [logs, setLogs]                 = useState<string[]>(['[Système] Forge 3D initialisée.']);
  const [logsOpen, setLogsOpen]         = useState(false);
  const [downloaded, setDownloaded]     = useState(false);
  const [history, setHistory]           = useState<HistoryItem[]>([]);
  const [historyOpen, setHistoryOpen]   = useState(false);
  const [tokenError, setTokenError]     = useState<string | null>(null);
  const [inlineError, setInlineError]   = useState<string | null>(null);
  const [elapsed, setElapsed]           = useState(0);
  const [glbLoading, setGlbLoading]     = useState(false);
  const [deleting, setDeleting]         = useState<string | null>(null);

  const logsEndRef  = useRef<HTMLDivElement>(null);
  const isMounted   = useRef(true);
  const abortRef    = useRef<AbortController | null>(null);
  const timerRef    = useRef<any>(null);
  const viewerRef   = useRef<any>(null);
  const jobIdRef    = useRef<string | null>(null);

  const cost = quality === 'draft' ? 500 : 1500;

  useEffect(() => {
    if (logsOpen) logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs, logsOpen]);

  const addLog = (msg: string) =>
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${msg}`]);

  const fetchTokens = useCallback(async () => {
    try {
      const r = await fetch(`${SERVER()}/api/user/profile/${ME()}`);
      if (r.ok && isMounted.current) {
        const p = await r.json();
        setUserTokens(typeof p.tokens === 'number' ? p.tokens : null);
      }
    } catch {}
  }, []);

  const fetchHistory = useCallback(async () => {
    try {
      const r = await fetch(`${SERVER()}/api/forge/history/${ME()}`);
      if (r.ok && isMounted.current) setHistory(await r.json());
    } catch { addLog('[Réseau] Impossible de charger l\'historique.'); }
  }, []);

  useEffect(() => {
    isMounted.current = true;
    fetchHistory();
    fetchTokens();
    return () => {
      isMounted.current = false;
      clearInterval(timerRef.current);
      abortRef.current?.abort();
    };
  }, []);

  // Écoute le chargement du GLB dans model-viewer
  useEffect(() => {
    const el = viewerRef.current;
    if (!el) return;
    const onLoad  = () => setGlbLoading(false);
    const onError = () => setGlbLoading(false);
    el.addEventListener('load', onLoad);
    el.addEventListener('error', onError);
    return () => { el.removeEventListener('load', onLoad); el.removeEventListener('error', onError); };
  }, [model]);

  const startTimer = () => {
    setElapsed(0);
    clearInterval(timerRef.current);
    timerRef.current = setInterval(() => setElapsed(p => p + 1), 1000);
  };
  const stopTimer = () => clearInterval(timerRef.current);

  const handleGenerate = async (overridePrompt?: string, overrideQuality?: 'draft' | 'high') => {
    const p = overridePrompt ?? prompt;
    const q = overrideQuality ?? quality;
    if (!p.trim()) return;

    setTokenError(null);
    setInlineError(null);
    if (userTokens !== null && userTokens !== Infinity && userTokens < cost) {
      setTokenError(`Il te faut ${cost.toLocaleString()} 🪙 pour forger, tu en as ${(userTokens ?? 0).toLocaleString()}.`);
      return;
    }

    abortRef.current?.abort();
    abortRef.current = new AbortController();

    setIsGenerating(true);
    setModel(null);
    setDownloaded(false);
    startTimer();
    addLog(`Génération "${p}" (${q === 'high' ? 'High-Poly' : 'Low-Poly'})…`);
    addLog('Traduction · libération VRAM · démarrage Shap-E…');

    try {
      // Démarre le job — le serveur répond immédiatement avec jobId
      const startRes = await fetch(`${SERVER()}/api/forge/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: p, username: ME(), quality: q }),
        signal: abortRef.current.signal,
      });
      const startData = await startRes.json();
      if (!startRes.ok) throw new Error(startData.error || `Erreur ${startRes.status}`);
      const { jobId } = startData;
      jobIdRef.current = jobId;

      // Poll le statut toutes les 3s jusqu'à done/error/cancelled
      let data: any = null;
      while (true) {
        await new Promise(r => setTimeout(r, 3000));
        if (abortRef.current?.signal.aborted) throw Object.assign(new Error('Annulé'), { name: 'AbortError' });
        const statusRes = await fetch(`${SERVER()}/api/forge/status/${jobId}`);
        const job = await statusRes.json();
        if (job.status === 'done')      { data = job; break; }
        if (job.status === 'error')     throw new Error(job.error || 'Génération échouée');
        if (job.status === 'cancelled') throw Object.assign(new Error('Annulé'), { name: 'AbortError' });
      }

      if (!isMounted.current) return;
      const device = data.device || 'cpu';
      setModel({
        url:         `${SERVER()}${data.model_url}`,
        preview_url: data.preview_url ? `${SERVER()}${data.preview_url}` : null,
        vertices:    data.vertices ?? 0,
        faces:       data.faces    ?? 0,
        format:      (data.format || 'glb').toUpperCase(),
        device,
        prompt:      p,
      });
      setGlbLoading(true);
      addLog(`✅ Modèle .${data.format || 'glb'} prêt — ${data.vertices?.toLocaleString()} sommets · ${device.toUpperCase()}`);
      fetchTokens();
      fetchHistory();
    } catch (err: any) {
      if (!isMounted.current || err.name === 'AbortError') return;
      addLog(`ERREUR : ${err.message}`);
      addLog("💡 Consulte l'historique — la génération a peut-être quand même abouti.");
      setInlineError(err.message);
    } finally {
      jobIdRef.current = null;
      stopTimer();
      if (isMounted.current) setIsGenerating(false);
    }
  };

  const handleCancel = async () => {
    abortRef.current?.abort();
    stopTimer();
    try {
      await fetch(`${SERVER()}/api/forge/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: ME(), jobId: jobIdRef.current }),
      });
    } catch {}
    jobIdRef.current = null;
    if (isMounted.current) { setIsGenerating(false); addLog('⛔ Génération annulée.'); }
  };

  const handleDelete = async (item: HistoryItem, e: React.MouseEvent) => {
    e.stopPropagation();
    setDeleting(item.filename);
    try {
      await fetch(`${SERVER()}/api/forge/file/${ME()}/${item.filename}`, { method: 'DELETE' });
      setHistory(prev => prev.filter(h => h.filename !== item.filename));
      if (model?.url.includes(item.filename)) setModel(null);
    } catch { addLog('[Erreur] Suppression échouée.'); }
    finally { setDeleting(null); }
  };

  const loadFromHistory = (item: HistoryItem) => {
    setModel({
      url:         `${SERVER()}${item.url}`,
      preview_url: item.preview_url ? `${SERVER()}${item.preview_url}` : null,
      vertices:    item.vertices,
      faces:       item.faces,
      format:      item.format,
      device:      item.device,
      prompt:      item.prompt,
    });
    setGlbLoading(true);
    setHistoryOpen(false);
    setDownloaded(false);
  };

  const navigateToAnimate = () => {
    if (!model?.preview_url) {
      setInlineError('Génère d\'abord un modèle avec preview pour pouvoir l\'animer.');
      return;
    }
    const rel = model.preview_url.replace(SERVER(), '');
    localStorage.setItem('lea_veo_init_image', rel);
    window.dispatchEvent(new CustomEvent('lea-navigate', { detail: { module: 'studio_veo' } }));
  };

  const isCuda  = model?.device?.includes('cuda') ?? false;
  const DeviceIcon = isCuda ? Zap : Cpu;

  return (
    <div className="flex flex-col h-full w-full bg-[#00050b] text-slate-200 overflow-hidden rounded-[2rem] border border-cyan-500/20 shadow-[0_0_40px_rgba(6,182,212,0.08)] relative">

      {/* Halo ambiance */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[500px] h-[500px] bg-cyan-600/4 rounded-full blur-[120px]" />
      </div>

      {/* ── HEADER ─────────────────────────────────────────────────── */}
      <header className="shrink-0 h-14 bg-[#010a17] border-b border-white/5 flex items-center justify-between px-4 z-10">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 bg-cyan-500/10 border border-cyan-500/30 rounded-lg flex items-center justify-center">
            <Hexagon size={14} className="text-cyan-400" />
          </div>
          <span className="font-black text-white uppercase tracking-widest text-xs italic">Forge 3D</span>
          {model ? (
            <span className={`flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-mono border ${isCuda ? 'bg-yellow-500/10 border-yellow-500/20 text-yellow-400' : 'bg-white/5 border-white/10 text-slate-400'}`}>
              <DeviceIcon size={9} />{isCuda ? 'CUDA' : 'CPU'}
            </span>
          ) : (
            <span className="px-1.5 py-0.5 bg-white/5 border border-white/10 rounded text-[9px] text-slate-400 font-mono">Shap-E</span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {isGenerating && (
            <span className="font-mono text-xs text-cyan-400 tabular-nums">{fmtTime(elapsed)}</span>
          )}
          <div className="flex items-center gap-1.5 bg-[#020d1f] px-3 py-1 rounded-full border border-white/5">
            <Coins size={12} className="text-yellow-400" />
            <span className="text-yellow-400 font-black text-xs">
              {userTokens === null ? '…' : userTokens === Infinity ? '∞' : userTokens.toLocaleString()}
            </span>
          </div>
        </div>
      </header>

      {/* ── PROMPT + QUALITÉ ────────────────────────────────────────── */}
      <div className="shrink-0 p-4 border-b border-white/5 z-10 space-y-3">
        <textarea
          value={prompt}
          onChange={e => { setPrompt(e.target.value); setInlineError(null); }}
          rows={3}
          placeholder="Ex : Une épée cyberpunk avec des runes lumineuses…"
          disabled={isGenerating}
          className="w-full bg-[#020d1f] border border-white/10 rounded-xl px-4 py-3 text-sm focus:border-cyan-500 resize-none text-slate-300 outline-none disabled:opacity-50"
        />
        <div className="grid grid-cols-2 gap-2">
          {([['draft', 'Low-Poly rapide', 500], ['high', 'High-Poly master', 1500]] as const).map(([q, label, c]) => (
            <button
              key={q}
              onClick={() => setQuality(q)}
              disabled={isGenerating}
              className={`p-3 rounded-xl text-left border transition-all disabled:opacity-50 ${quality === q ? 'bg-cyan-500/10 border-cyan-500/40' : 'bg-white/5 border-white/5 hover:bg-white/10'}`}
            >
              <span className="font-bold text-xs text-white block">{label}</span>
              <span className="text-yellow-400 text-xs font-black">{c} 🪙</span>
            </button>
          ))}
        </div>
      </div>

      {/* ── VIEWPORT 3D ─────────────────────────────────────────────── */}
      <div className="flex-1 relative min-h-0 z-10">

        {/* Stats overlay */}
        {model && !isGenerating && (
          <div className="absolute top-3 right-3 bg-[#010a17]/90 backdrop-blur border border-white/5 px-3 py-2 rounded-lg z-20 font-mono text-[9px] space-y-0.5">
            {model.vertices > 0 && <div className="flex gap-2"><span className="text-slate-500">Vtx</span><span className="text-cyan-400">{model.vertices.toLocaleString()}</span></div>}
            {model.faces > 0    && <div className="flex gap-2"><span className="text-slate-500">Faces</span><span className="text-cyan-400">{model.faces.toLocaleString()}</span></div>}
            <div className="flex gap-2"><span className="text-slate-500">Fmt</span><span className="text-cyan-400">{model.format}</span></div>
            {model.prompt && <div className="flex gap-2 max-w-[160px]"><span className="text-slate-500">↳</span><span className="text-slate-400 truncate">{model.prompt}</span></div>}
          </div>
        )}

        {isGenerating ? (
          /* Animation génération */
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-6">
            <div className="relative">
              <div className="w-28 h-28 border border-cyan-500/30 rounded-full animate-[spin_3s_linear_infinite]" />
              <div className="absolute inset-4 border border-blue-500/40 rounded-full animate-[spin_2s_linear_infinite_reverse]" />
              <div className="absolute inset-0 flex items-center justify-center">
                <Hexagon size={32} className="text-cyan-400 animate-pulse" />
              </div>
            </div>
            <div className="text-center">
              <p className="font-black uppercase tracking-[0.2em] text-cyan-400 text-sm">Forgeage en cours…</p>
              <p className="text-slate-500 text-[11px] mt-1">Shap-E · libération VRAM en cours</p>
              <p className="text-cyan-600 font-mono text-sm mt-2">{fmtTime(elapsed)}</p>
            </div>
          </div>

        ) : model ? (
          <>
            {/* Preview PNG pendant le chargement du GLB */}
            {glbLoading && model.preview_url && (
              <div className="absolute inset-0 flex items-center justify-center bg-[#00050b] z-10">
                <img src={model.preview_url} className="max-h-full max-w-full object-contain opacity-60" alt="preview" />
                <div className="absolute bottom-4 flex items-center gap-2 text-cyan-400 text-[10px]">
                  <Activity size={12} className="animate-spin" />
                  <span>Chargement du modèle 3D…</span>
                </div>
              </div>
            )}
            <model-viewer
              ref={viewerRef}
              src={model.url}
              auto-rotate
              camera-controls
              shadow-intensity="1"
              environment-image="neutral"
              exposure="0.8"
              style={{ width: '100%', height: '100%', backgroundColor: 'transparent' }}
            />
          </>
        ) : (
          <div className="absolute inset-0 flex flex-col items-center justify-center opacity-15 pointer-events-none">
            <Rotate3D size={72} className="text-slate-400 mb-4" />
            <p className="font-black uppercase tracking-[0.2em] text-slate-400">Viewport vide</p>
          </div>
        )}
      </div>

      {/* ── ACTIONS BAS ─────────────────────────────────────────────── */}
      <div className="shrink-0 p-4 border-t border-white/5 z-10 space-y-2">

        {/* Erreur tokens */}
        {tokenError && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 text-center">
            <p className="text-red-300 text-xs font-bold mb-1">{tokenError}</p>
            <button
              onClick={() => window.dispatchEvent(new CustomEvent('lea-navigate', { detail: { module: 'shop' } }))}
              className="bg-gradient-to-r from-yellow-500 to-orange-500 text-white text-[10px] font-black uppercase tracking-widest px-4 py-2 rounded-lg hover:opacity-90 active:scale-95 transition-all"
            >Recharger mes tokens 🪙</button>
          </div>
        )}

        {/* Erreur inline */}
        {inlineError && !tokenError && (
          <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-xl p-3">
            <AlertCircle size={14} className="text-red-400 shrink-0 mt-0.5" />
            <p className="text-red-300 text-xs flex-1">{inlineError}</p>
            <button onClick={() => setInlineError(null)} className="text-slate-600 hover:text-slate-400"><X size={12} /></button>
          </div>
        )}

        {/* Bouton principal : Forger ou Annuler */}
        {isGenerating ? (
          <button
            onClick={handleCancel}
            className="w-full py-4 rounded-xl font-black uppercase tracking-[0.1em] text-[11px] transition-all flex items-center justify-center gap-2 bg-red-600/20 border border-red-500/40 text-red-400 hover:bg-red-600/30 active:scale-95"
          >
            <X size={16} />Annuler la génération
          </button>
        ) : (
          <button
            onClick={() => handleGenerate()}
            disabled={!prompt.trim()}
            className={`w-full py-4 rounded-xl font-black uppercase tracking-[0.1em] text-[11px] transition-all flex items-center justify-center gap-2
              ${!prompt.trim()
                ? 'bg-white/5 text-slate-600 cursor-not-allowed'
                : 'bg-cyan-600 hover:bg-cyan-500 text-[#00050b] shadow-[0_0_20px_rgba(6,182,212,0.3)] active:scale-95'}`}
          >
            <Sparkles size={16} />Forger · {cost.toLocaleString()} 🪙
          </button>
        )}

        {/* Actions post-génération */}
        {model && !isGenerating && (
          <div className="grid grid-cols-2 gap-2">
            {/* Télécharger GLB */}
            <button
              onClick={async () => { await saveFile(model.url, `lea_3d_${Date.now()}.${model.format.toLowerCase()}`); setDownloaded(true); }}
              className={`py-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-center gap-2
                ${downloaded ? 'border-green-500/40 text-green-400 bg-green-500/5' : 'border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/10 active:scale-95'}`}
            >
              <Download size={13} />
              {downloaded ? '✓ Téléchargé' : `.${model.format.toLowerCase()}`}
            </button>

            {/* Télécharger preview PNG */}
            {model.preview_url ? (
              <button
                onClick={() => saveFile(model.preview_url, `lea_3d_preview_${Date.now()}.png`)}
                className="py-3 rounded-xl border border-white/10 text-white/50 text-xs font-bold hover:bg-white/5 active:scale-95 transition-all flex items-center justify-center gap-2"
              >
                <Image size={13} />Preview .png
              </button>
            ) : <div />}

            {/* Re-générer High-Poly */}
            {quality === 'draft' && (
              <button
                onClick={() => handleGenerate(model.prompt, 'high')}
                className="py-3 rounded-xl border border-cyan-500/20 text-cyan-500/70 text-xs font-bold hover:bg-cyan-500/10 active:scale-95 transition-all flex items-center justify-center gap-2"
              >
                <RotateCcw size={13} />Upgrade High-Poly
              </button>
            )}

            {/* Animer en vidéo */}
            <button
              onClick={navigateToAnimate}
              className={`py-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-center gap-2
                ${model.preview_url ? 'border-purple-500/30 text-purple-400 hover:bg-purple-500/10 active:scale-95' : 'border-white/5 text-slate-600 cursor-not-allowed'}`}
            >
              <Film size={13} />Animer en vidéo
            </button>
          </div>
        )}
      </div>

      {/* ── HISTORIQUE ──────────────────────────────────────────────── */}
      <div className="shrink-0 border-t border-white/5 z-10">
        <button
          onClick={() => setHistoryOpen(o => !o)}
          className="w-full flex items-center gap-2 px-4 py-2 bg-[#010a17] text-left"
        >
          <Clock size={11} className="text-cyan-500" />
          <span className="text-[9px] uppercase tracking-widest text-cyan-500/70 flex-1">Historique ({history.length})</span>
          <button onClick={e => { e.stopPropagation(); fetchHistory(); }} className="p-0.5 hover:text-cyan-400 text-slate-600">
            <RefreshCw size={10} />
          </button>
          <ChevronDown size={12} className={`text-slate-500 transition-transform ml-1 ${historyOpen ? 'rotate-180' : ''}`} />
        </button>

        {historyOpen && (
          <div className="max-h-48 overflow-y-auto bg-[#000308] p-2 space-y-1">
            {history.length === 0 ? (
              <p className="text-[10px] text-slate-600 text-center py-4">Aucun objet forgé</p>
            ) : history.map(item => (
              <button
                key={item.filename}
                onClick={() => loadFromHistory(item)}
                className="w-full flex items-center gap-3 p-2 rounded-lg bg-white/3 hover:bg-cyan-500/10 border border-white/5 text-left transition-all group"
              >
                {item.preview_url ? (
                  <img src={`${SERVER()}${item.preview_url}`} className="w-10 h-10 rounded object-cover border border-white/10 shrink-0" alt="" />
                ) : (
                  <div className="w-10 h-10 rounded border border-cyan-500/20 bg-cyan-500/5 flex items-center justify-center shrink-0">
                    <Hexagon size={16} className="text-cyan-500/60" />
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-[11px] text-white font-bold truncate">{item.prompt || item.filename}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <p className="text-[9px] text-slate-500">{new Date(item.date).toLocaleDateString('fr-FR')}</p>
                    {item.vertices > 0 && <p className="text-[9px] text-cyan-700">{item.vertices.toLocaleString()} vtx</p>}
                    {item.device && <p className={`text-[9px] font-mono ${item.device.includes('cuda') ? 'text-yellow-700' : 'text-slate-600'}`}>{item.device.includes('cuda') ? 'CUDA' : 'CPU'}</p>}
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <span className="text-[9px] text-cyan-400 font-mono">.{item.format.toLowerCase()}</span>
                  <button
                    onClick={e => handleDelete(item, e)}
                    disabled={deleting === item.filename}
                    className="p-1 rounded opacity-0 group-hover:opacity-100 hover:bg-red-500/20 hover:text-red-400 text-slate-600 transition-all ml-1"
                  >
                    {deleting === item.filename ? <Activity size={10} className="animate-spin" /> : <Trash2 size={10} />}
                  </button>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* ── CONSOLE ─────────────────────────────────────────────────── */}
      <div className="shrink-0 border-t border-white/5 z-10">
        <button
          onClick={() => setLogsOpen(o => !o)}
          className="w-full flex items-center gap-2 px-4 py-2 bg-[#010a17] text-left"
        >
          <Terminal size={11} className="text-cyan-500" />
          <span className="text-[9px] uppercase tracking-widest text-cyan-500/70 flex-1">Console ({logs.length})</span>
          <ChevronDown size={12} className={`text-slate-500 transition-transform ${logsOpen ? 'rotate-180' : ''}`} />
        </button>
        {logsOpen && (
          <div className="h-28 overflow-y-auto bg-[#000308] p-3 space-y-1 font-mono text-[10px] text-slate-400">
            {logs.map((log, i) => (
              <div key={i} className="flex gap-2">
                <span className="text-cyan-800 shrink-0">~</span>
                <span className={log.includes('ERREUR') || log.includes('⛔') ? 'text-red-400' : log.includes('✅') ? 'text-green-400' : log.includes('[Réseau]') ? 'text-orange-400' : ''}>{log}</span>
              </div>
            ))}
            <div ref={logsEndRef} />
          </div>
        )}
      </div>
    </div>
  );
};
