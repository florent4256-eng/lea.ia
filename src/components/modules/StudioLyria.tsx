import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Music, Play, Pause, Download, Sparkles,
  Clock, Disc3, X, RefreshCw,
  AlertCircle, Zap, Trash2
} from 'lucide-react';

const SERVER = () => (window as any).LEA_SERVER_URL || '';
const currentUser = () => localStorage.getItem('lea_currentUser') || 'invite';
const JOB_KEY = 'lyria_active_job';

type Phase = 'idle' | 'generating' | 'done' | 'error' | 'cancelled';
interface TrackItem { url: string; name: string; date: number; engine?: string; title?: string; }

// ── Langues ────────────────────────────────────────────────────────────────
const LANGUAGES = [
  { id: 'fr', label: '🇫🇷 Français',    tags: 'French lyrics, chanson française, French vocals' },
  { id: 'en', label: '🇬🇧 English',     tags: 'English lyrics, English vocals' },
  { id: 'es', label: '🇪🇸 Español',     tags: 'Spanish lyrics, canción en español, Spanish vocals' },
  { id: 'pt', label: '🇧🇷 Português',   tags: 'Portuguese lyrics, música em português, Brazilian vocals' },
  { id: 'de', label: '🇩🇪 Deutsch',     tags: 'German lyrics, deutsche Musik, German vocals' },
  { id: 'it', label: '🇮🇹 Italiano',    tags: 'Italian lyrics, musica italiana, Italian vocals' },
  { id: 'ja', label: '🇯🇵 日本語',       tags: 'Japanese lyrics, J-pop, Japanese vocals' },
  { id: 'ko', label: '🇰🇷 한국어',       tags: 'Korean lyrics, K-pop, Korean vocals' },
  { id: 'zh', label: '🇨🇳 中文',         tags: 'Chinese lyrics, Mandarin vocals, Chinese pop' },
  { id: 'ar', label: '🇸🇦 العربية',     tags: 'Arabic lyrics, Arabic vocals, Arabic music' },
  { id: 'ru', label: '🇷🇺 Русский',     tags: 'Russian lyrics, Russian vocals, Russian pop' },
  { id: 'hi', label: '🇮🇳 हिन्दी',      tags: 'Hindi lyrics, Bollywood, Hindi vocals' },
  { id: 'tr', label: '🇹🇷 Türkçe',      tags: 'Turkish lyrics, Turkish pop, Turkish vocals' },
  { id: 'pl', label: '🇵🇱 Polski',      tags: 'Polish lyrics, Polish pop, Polish vocals' },
  { id: 'nl', label: '🇳🇱 Nederlands',  tags: 'Dutch lyrics, Dutch pop, Dutch vocals' },
  { id: 'sv', label: '🇸🇪 Svenska',     tags: 'Swedish lyrics, Swedish pop, Swedish vocals' },
  { id: 'no', label: '🇳🇴 Norsk',       tags: 'Norwegian lyrics, Norwegian pop, Norwegian vocals' },
  { id: 'da', label: '🇩🇰 Dansk',       tags: 'Danish lyrics, Danish pop, Danish vocals' },
  { id: 'fi', label: '🇫🇮 Suomi',       tags: 'Finnish lyrics, Finnish pop, Finnish vocals' },
  { id: 'ro', label: '🇷🇴 Română',      tags: 'Romanian lyrics, Romanian pop, Romanian vocals' },
  { id: 'uk', label: '🇺🇦 Українська', tags: 'Ukrainian lyrics, Ukrainian pop, Ukrainian vocals' },
  { id: 'el', label: '🇬🇷 Ελληνικά',   tags: 'Greek lyrics, Greek pop, Greek vocals' },
  { id: 'he', label: '🇮🇱 עברית',       tags: 'Hebrew lyrics, Israeli pop, Hebrew vocals' },
  { id: 'th', label: '🇹🇭 ภาษาไทย',    tags: 'Thai lyrics, Thai pop, Thai vocals' },
  { id: 'vi', label: '🇻🇳 Tiếng Việt', tags: 'Vietnamese lyrics, Vietnamese pop, Vietnamese vocals' },
  { id: 'id', label: '🇮🇩 Indonesia',   tags: 'Indonesian lyrics, Indonesian pop, Indonesian vocals' },
  { id: 'ms', label: '🇲🇾 Melayu',      tags: 'Malay lyrics, Malaysian pop, Malay vocals' },
  { id: 'tl', label: '🇵🇭 Filipino',    tags: 'Filipino lyrics, OPM, Tagalog vocals' },
  { id: 'cs', label: '🇨🇿 Čeština',    tags: 'Czech lyrics, Czech pop, Czech vocals' },
  { id: 'hu', label: '🇭🇺 Magyar',      tags: 'Hungarian lyrics, Hungarian pop, Hungarian vocals' },
];

// ── Styles musicaux ────────────────────────────────────────────────────────
const STYLES = [
  { id: 'pop',       label: 'Pop',        tags: 'pop, catchy, female vocal, upbeat, radio-friendly' },
  { id: 'electro',   label: 'Électro',    tags: 'electronic, edm, synthesizer, energetic, club beat, modern' },
  { id: 'acoustic',  label: 'Acoustique', tags: 'acoustic guitar, warm, folk, singer-songwriter, intimate' },
  { id: 'epic',      label: 'Épique',     tags: 'cinematic, orchestral, strings, epic, powerful, trailer' },
  { id: 'rb',        label: 'R&B',        tags: 'rnb, soul, smooth, groovy, emotional, soulful, female vocal' },
  { id: 'hiphop',    label: 'Hip-Hop',    tags: 'hip hop, trap, 808 bass, modern beat, urban, rap' },
  { id: 'rock',      label: 'Rock',       tags: 'rock, electric guitar, drums, energetic, band, distortion' },
  { id: 'jazz',      label: 'Jazz',       tags: 'jazz, piano, saxophone, swing, cool, improvisation, smooth' },
  { id: 'metal',     label: 'Metal',      tags: 'heavy metal, distorted guitar, aggressive, double kick, powerful' },
  { id: 'classical', label: 'Classique',  tags: 'classical, piano, orchestra, strings, elegant, sophisticated' },
  { id: 'lofi',      label: 'Lo-fi',      tags: 'lofi, chill, relaxing, lo-fi hip hop, mellow, study music' },
  { id: 'latin',     label: 'Latin',      tags: 'latin, salsa, reggaeton, dancehall, tropical, percussion' },
];

const LYRICS_PLACEHOLDER = `[verse]
Écris tes paroles de couplet ici
Deuxième ligne du couplet

[chorus]
Le refrain ici — plus fort et accrocheur
Mélodie principale
Laisse ta voix briller

[verse]
Deuxième couplet
Développe le thème

[chorus]
Le refrain ici — plus fort et accrocheur
Mélodie principale
Laisse ta voix briller`;

const fmtTime = (s: number) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, '0')}`;

function addTagsToText(existing: string, tags: string): string {
  const base = existing.trim();
  if (!base) return tags;
  if (base.includes(tags)) return base;
  return base + ', ' + tags;
}
function removeTagsFromText(existing: string, tags: string): string {
  let result = existing;
  result = result.replace(new RegExp(',?\\s*' + tags.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ',?\\s*', 'g'), ', ');
  return result.replace(/^,\s*/, '').replace(/,\s*$/, '').replace(/,\s*,/g, ',').trim();
}
function detectLang(text: string): string {
  if (!text.trim()) return 'fr';
  const scores: Record<string, number> = {};
  const t = text.toLowerCase();
  scores['fr'] = ((t.match(/[àâçéèêëîïôùûüœæ]/g) || []).length * 3)
    + ((t.match(/\b(le|la|les|de|du|et|en|un|une|pour|avec|sur|dans|je|tu|il|elle|nous|vous|mon|ma|mes|son|sa|ses|ce|qui|que|tout|plus|mais|bien|comme|si|très|ça)\b/g) || []).length);
  scores['en'] = (t.match(/\b(the|and|is|are|was|you|your|my|me|we|it|he|she|they|our|this|that|with|for|in|on|to|of|be|have|do|not|but|from|will|so|what|when|how|just|can|like)\b/g) || []).length;
  scores['es'] = ((t.match(/[áéíóúüñ¿¡]/g) || []).length * 2)
    + ((t.match(/\b(el|la|los|las|de|del|en|que|es|un|una|con|por|para|como|pero|más|muy|yo|tú|él|ella)\b/g) || []).length);
  scores['pt'] = ((t.match(/[ãõàáéíóúâêôç]/g) || []).length * 2)
    + ((t.match(/\b(eu|tu|ele|ela|nós|você|que|de|do|da|em|um|uma|para|com|mas|não|mais|muito)\b/g) || []).length);
  scores['de'] = ((t.match(/[äöüß]/g) || []).length * 3)
    + ((t.match(/\b(ich|du|er|sie|wir|ihr|und|ist|das|die|der|ein|eine|nicht|mit|für|auf|in|von|zu|es|aber|wie|auch|wenn)\b/g) || []).length);
  scores['it'] = (t.match(/\b(io|tu|lui|lei|noi|voi|che|di|il|la|le|lo|gli|un|una|per|con|ma|non|più|come|anche|sono|sei|è|essere)\b/g) || []).length;
  scores['ja'] = (t.match(/[぀-ゟ゠-ヿ一-龯]/g) || []).length * 5;
  scores['ko'] = (t.match(/[가-힯ᄀ-ᇿ]/g) || []).length * 5;
  scores['zh'] = (t.match(/[一-鿿]/g) || []).length;
  scores['ar'] = (t.match(/[؀-ۿ]/g) || []).length * 5;
  scores['ru'] = (t.match(/[а-яё]/g) || []).length * 2;
  scores['uk'] = (t.match(/[іїєґ]/g) || []).length * 5 + (scores['ru'] || 0);
  scores['hi'] = (t.match(/[ऀ-ॿ]/g) || []).length * 5;
  scores['th'] = (t.match(/[฀-๿]/g) || []).length * 5;
  scores['vi'] = (t.match(/[đắằặẳẵấầậẩẫảẹẻẽếềệểễỉịọỏốồộổỗớờợởỡụủứừựửữỳỷỹỵ]/g) || []).length * 3;
  const best = Object.entries(scores).sort((a, b) => b[1] - a[1])[0];
  if (!best || best[1] === 0) return 'fr';
  return best[0];
}

// ── Durées ─────────────────────────────────────────────────────────────────
const DURATIONS = [
  { label: '30s',     min: 30,  max: 30,  cost: 30  },
  { label: '1~2 min', min: 60,  max: 120, cost: 60  },
  { label: '2~3 min', min: 120, max: 180, cost: 120 },
  { label: '3~4 min', min: 180, max: 240, cost: 180 },
  { label: '5~6 min', min: 300, max: 360, cost: 250 },
];

// ── BPM presets ────────────────────────────────────────────────────────────
const BPM_PRESETS = [
  { label: 'Très lent',  bpm: 60,  desc: 'Ballade, ambiant' },
  { label: 'Lent',       bpm: 75,  desc: 'Soul, slow' },
  { label: 'Doux',       bpm: 90,  desc: 'R&B, pop douce' },
  { label: 'Pop',        bpm: 110, desc: 'Standard pop' },
  { label: 'Dance',      bpm: 128, desc: 'Club, électro' },
  { label: 'Trap',       bpm: 140, desc: 'Trap, hip-hop' },
  { label: 'Hard',       bpm: 155, desc: 'Rock, metal' },
  { label: 'Drum&Bass',  bpm: 174, desc: 'DnB, jungle' },
];

// ── Composant principal ────────────────────────────────────────────────────
export const StudioLyria = () => {
  const [tab, setTab] = useState<'create' | 'library'>('create');

  const [selectedLang, setSelectedLang] = useState<string>('fr');
  const [selectedStyles, setSelectedStyles] = useState<Set<string>>(new Set());
  const [genreText, setGenreText] = useState('French lyrics, chanson française, French vocals');
  const [lyricsMode, setLyricsMode] = useState<'lyrics' | 'instrumental'>('lyrics');
  const [lyrics, setLyrics] = useState('');
  const [durIdx, setDurIdx] = useState(1);
  const [title, setTitle] = useState('');
  const [bpm, setBpm] = useState<number | null>(null);
  const [bpmInput, setBpmInput] = useState('');

  const [phase, setPhase] = useState<Phase>('idle');
  const [progress, setProgress] = useState(0);
  const [phaseLabel, setPhaseLabel] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [jobId, setJobId] = useState<string | null>(null);

  const [currentTrack, setCurrentTrack] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [audioTime, setAudioTime] = useState(0);
  const [audioDuration, setAudioDuration] = useState(0);
  const audioRef = useRef<HTMLAudioElement>(null);

  const [library, setLibrary] = useState<TrackItem[]>([]);
  const [loadingLib, setLoadingLib] = useState(false);
  const [userTokens, setUserTokens] = useState<number | null>(null);
  const [tokenError, setTokenError] = useState<string | null>(null);
  const pollRef = useRef<any>(null);

  // ── Langue / styles ────────────────────────────────────────────────────
  const changeLang = useCallback((langId: string, prevId?: string) => {
    const oldLang = LANGUAGES.find(l => l.id === (prevId ?? selectedLang));
    const newLang = LANGUAGES.find(l => l.id === langId);
    if (!newLang || langId === (prevId ?? selectedLang)) return;
    setSelectedLang(langId);
    setGenreText(t => addTagsToText(oldLang ? removeTagsFromText(t, oldLang.tags) : t, newLang.tags));
  }, [selectedLang]);

  const handleLyricsChange = useCallback((text: string) => {
    setLyrics(text);
    if (text.trim().length < 20) return;
    const detected = detectLang(text);
    if (detected !== selectedLang) changeLang(detected, selectedLang);
  }, [selectedLang, changeLang]);

  const toggleStyle = (styleId: string) => {
    const style = STYLES.find(s => s.id === styleId)!;
    setSelectedStyles(prev => {
      const next = new Set(prev);
      if (next.has(styleId)) { next.delete(styleId); setGenreText(t => removeTagsFromText(t, style.tags)); }
      else { next.add(styleId); setGenreText(t => addTagsToText(t, style.tags)); }
      return next;
    });
  };

  const durCfg = DURATIONS[durIdx];

  // ── Bibliothèque ───────────────────────────────────────────────────────
  const fetchLibrary = useCallback(async () => {
    setLoadingLib(true);
    try {
      const r = await fetch(`${SERVER()}/api/studio/lyria/gallery/${currentUser()}`);
      if (r.ok) {
        const d = await r.json();
        setLibrary((d.files || []).map((f: any) => ({
          url: f.url, name: f.name, date: f.date, engine: f.engine, title: f.title,
        })));
      }
    } catch {}
    setLoadingLib(false);
  }, []);

  useEffect(() => { fetchLibrary(); }, [fetchLibrary]);

  // Fetch tokens au montage + polling 10s
  useEffect(() => {
    const user = currentUser();
    if (!user) return;
    const server = SERVER();
    const fetchTk = () => fetch(`${server}/api/user/profile/${user}`)
      .then(r => r.ok ? r.json() : null)
      .then(p => { if (p && typeof p.tokens === 'number') setUserTokens(p.tokens); })
      .catch(() => {});
    fetchTk();
    const iv = setInterval(fetchTk, 10000);
    return () => clearInterval(iv);
  }, []);

  // ── Job lifecycle ──────────────────────────────────────────────────────
  const finishJob = useCallback((status: Phase, trackUrl?: string, errMsg?: string) => {
    clearInterval(pollRef.current);
    localStorage.removeItem(JOB_KEY);
    setPhase(status);
    if (trackUrl) {
      setCurrentTrack(`${SERVER()}${trackUrl}`);
      setAudioTime(0);
      setIsPlaying(false);
      if (audioRef.current) audioRef.current.currentTime = 0;
      fetchLibrary();
    }
    if (errMsg) setError(errMsg);
  }, [fetchLibrary]);

  const startPoll = useCallback((jid: string) => {
    if (pollRef.current) clearInterval(pollRef.current);
    const statusUrl = `${SERVER()}/api/studio/lyria/pro/status/${jid}`;
    pollRef.current = setInterval(async () => {
      try {
        const r = await fetch(statusUrl);
        if (!r.ok) return;
        const d = await r.json();
        setProgress(d.progress || 0);
        setPhaseLabel(d.phase || '');
        if (d.status === 'done' && d.url)     finishJob('done', d.url);
        else if (d.status === 'error')         finishJob('error', undefined, d.error || 'Erreur inconnue');
        else if (d.status === 'cancelled')     finishJob('cancelled');
        else if (d.status === 'unknown')       finishJob('error', undefined, 'Job introuvable');
      } catch {}
    }, 3000);
  }, [finishJob]);

  useEffect(() => () => { clearInterval(pollRef.current); }, []);

  // Reprise sur retour
  useEffect(() => {
    const saved = localStorage.getItem(JOB_KEY);
    if (!saved) return;
    try {
      const { jid, startedAt } = JSON.parse(saved);
      if (Date.now() - startedAt > 3600_000) { localStorage.removeItem(JOB_KEY); return; }
      setJobId(jid);
      setPhase('generating');
      setPhaseLabel('Reconnexion au job en cours...');
      startPoll(jid);
    } catch { localStorage.removeItem(JOB_KEY); }
  }, []); // eslint-disable-line

  const handleCancel = async () => {
    if (!jobId) return;
    try { await fetch(`${SERVER()}/api/studio/lyria/pro/cancel/${jobId}`, { method: 'POST' }); } catch {}
    finishJob('cancelled');
  };

  // ── Générer ────────────────────────────────────────────────────────────
  const handleGenerate = async () => {
    setTokenError(null);
    // Vérification tokens avant génération
    const cost = DURATIONS[durIdx].cost;
    if (userTokens !== null && userTokens < cost) {
      setTokenError(`Il te faut ${cost} 🪙 pour générer ce morceau, tu en as ${userTokens.toLocaleString()}.`);
      return;
    }
    setPhase('generating'); setProgress(0); setError(null);
    setPhaseLabel('Envoi de la requête...');
    try {
      const base = genreText.trim() || 'pop, energetic, female vocal, upbeat';
      const prompt = bpm ? `${bpm} bpm, ${base}` : base;
      const lyricsText = lyricsMode === 'instrumental' ? '' : (lyrics.trim() || '');
      const { min, max } = DURATIONS[durIdx];
      const dur = min === max ? min : Math.floor(Math.random() * (max - min) + min);
      const body = { prompt, lyrics: lyricsText, duration: dur, username: currentUser(), title: title.trim() || undefined };
      const r = await fetch(`${SERVER()}/api/studio/lyria/pro/generate`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
      });
      if (!r.ok) throw new Error(`Serveur: ${r.status}`);
      const d = await r.json();
      if (!d.job_id) throw new Error('Pas de job_id reçu');
      setJobId(d.job_id);
      localStorage.setItem(JOB_KEY, JSON.stringify({ jid: d.job_id, startedAt: Date.now() }));
      startPoll(d.job_id);
    } catch (e: any) {
      setPhase('error'); setError(e.message || 'Erreur réseau');
    }
  };

  // ── Lecteur ────────────────────────────────────────────────────────────
  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) { audioRef.current.pause(); setIsPlaying(false); }
    else { audioRef.current.play(); setIsPlaying(true); }
  };
  const playTrack = (url: string) => {
    const full = url.startsWith('http') ? url : `${SERVER()}${url}`;
    setCurrentTrack(full);
    setAudioTime(0);
    setIsPlaying(false);
    setTab('create');
    setTimeout(() => {
      if (audioRef.current) { audioRef.current.currentTime = 0; audioRef.current.play().catch(() => {}); setIsPlaying(true); }
    }, 150);
  };

  const deleteTrack = async (track: TrackItem) => {
    if (!confirm(`Supprimer "${track.title || track.name}" ?`)) return;
    try {
      const r = await fetch(`${SERVER()}/api/studio/lyria/file/${currentUser()}/${encodeURIComponent(track.name)}`, { method: 'DELETE' });
      if (r.ok) {
        setLibrary(prev => prev.filter(t => t.name !== track.name));
        if (currentTrack?.endsWith(track.name)) { setCurrentTrack(null); setIsPlaying(false); }
      }
    } catch {}
  };

  return (
    <div className="flex flex-col h-full bg-[#020617] text-slate-200 overflow-hidden relative">
      <div className="absolute bottom-0 left-0 w-96 h-96 bg-fuchsia-900/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute top-0 right-0 w-72 h-72 bg-purple-900/10 rounded-full blur-[100px] pointer-events-none" />

      {/* ── Header ──────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-white/5 relative z-10 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-gradient-to-br from-fuchsia-500 to-purple-700 rounded-xl flex items-center justify-center shadow-[0_0_14px_rgba(217,70,239,0.3)]">
            <Music size={17} className="text-white" />
          </div>
          <div>
            <h2 className="text-sm font-black text-white uppercase italic tracking-tight">Studio Lyria</h2>
            <p className="text-[9px] uppercase tracking-widest text-fuchsia-400/70 font-bold">ACE-Step v1 · 3.5B · 44.1kHz</p>
          </div>
        </div>
        <div className="flex gap-1">
          {(['create', 'library'] as const).map(t => (
            <button key={t} onClick={() => { setTab(t); if (t === 'library') fetchLibrary(); }}
              className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${tab === t ? 'bg-fuchsia-500/20 text-fuchsia-300 border border-fuchsia-500/30' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
              {t === 'create' ? 'Créer' : 'Bibliothèque'}
            </button>
          ))}
        </div>
      </div>

      {/* ── Lecteur ─────────────────────────────────────────────────────── */}
      {currentTrack && (
        <div className="shrink-0 px-6 py-3 bg-fuchsia-950/30 border-b border-fuchsia-500/10 relative z-10">
          <audio ref={audioRef} src={currentTrack}
            onTimeUpdate={() => setAudioTime(audioRef.current?.currentTime || 0)}
            onLoadedMetadata={() => setAudioDuration(audioRef.current?.duration || 0)}
            onEnded={() => setIsPlaying(false)} />
          <div className="flex items-center gap-3">
            <button onClick={togglePlay} className="w-8 h-8 bg-fuchsia-500/20 hover:bg-fuchsia-500/40 border border-fuchsia-500/30 rounded-full flex items-center justify-center transition-all">
              {isPlaying ? <Pause size={13} className="text-fuchsia-300" /> : <Play size={13} className="text-fuchsia-300" />}
            </button>
            <span className="text-[10px] font-mono text-fuchsia-300 w-8 shrink-0">{fmtTime(audioTime)}</span>
            <input type="range" min={0} max={audioDuration || 1} step={0.1} value={audioTime}
              onChange={e => { if (audioRef.current) audioRef.current.currentTime = parseFloat(e.target.value); }}
              className="flex-1 h-1 accent-fuchsia-500 cursor-pointer" />
            <span className="text-[10px] font-mono text-slate-500 w-8 shrink-0">{fmtTime(audioDuration)}</span>
            <a href={currentTrack} download className="w-7 h-7 bg-white/5 hover:bg-white/10 rounded-lg flex items-center justify-center transition-all">
              <Download size={12} className="text-slate-400" />
            </a>
            <button onClick={() => { setCurrentTrack(null); setIsPlaying(false); }}
              className="w-7 h-7 bg-white/5 hover:bg-white/10 rounded-lg flex items-center justify-center transition-all">
              <X size={12} className="text-slate-400" />
            </button>
          </div>
        </div>
      )}

      {/* ── Contenu ─────────────────────────────────────────────────────── */}
      <div className="flex-1 overflow-y-auto relative z-10">

        {/* ════ CRÉER ════ */}
        {tab === 'create' && (
          <div className="p-5 space-y-5 max-w-2xl mx-auto">

            {/* Titre */}
            <div>
              <label className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-2 block">Titre de la musique</label>
              <input type="text" value={title} onChange={e => setTitle(e.target.value)}
                placeholder="Mon Hit 2025, La Nuit de Paris..."
                maxLength={60}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-[13px] text-slate-200 placeholder:text-slate-600 outline-none focus:border-fuchsia-500/50 transition-colors" />
            </div>

            {/* Langue */}
            <div>
              <label className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-2 block">Langue des paroles</label>
              <div className="grid grid-cols-3 gap-1.5 max-h-48 overflow-y-auto pr-1 scrollbar-thin">
                {LANGUAGES.map(l => (
                  <button key={l.id} onClick={() => changeLang(l.id, selectedLang)}
                    className={`px-2 py-1.5 rounded-xl border text-[10px] font-bold transition-all text-left truncate ${selectedLang === l.id ? 'bg-fuchsia-500/25 border-fuchsia-500/50 text-fuchsia-200' : 'bg-white/3 border-white/10 text-slate-400 hover:text-white hover:border-white/25'}`}>
                    {l.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Style */}
            <div>
              <label className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-2 block">Style musical</label>
              <textarea value={genreText} onChange={e => setGenreText(e.target.value)}
                placeholder={"Ex: Trap, Heavy 808 Bass, Dark Cinematic, Auto-tune vocals, 140 BPM,\nDistorted 808, Hard-hitting drums, Deep sub bass, Studio quality,\nMelancholic Piano, Aggressive drums, Male lead vocals singing..."}
                rows={4}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-[12px] text-slate-200 placeholder:text-slate-600 outline-none focus:border-fuchsia-500/40 transition-colors resize-none leading-relaxed mb-2.5" />
              <div className="flex flex-wrap gap-1.5">
                {STYLES.map(s => {
                  const active = selectedStyles.has(s.id);
                  return (
                    <button key={s.id} onClick={() => toggleStyle(s.id)}
                      className={`px-2.5 py-1 rounded-full border text-[10px] font-bold transition-all ${active ? 'bg-fuchsia-500/25 border-fuchsia-500/50 text-fuchsia-200' : 'bg-white/3 border-white/10 text-slate-500 hover:text-white hover:border-white/25'}`}>
                      {active && <span className="mr-1 opacity-60">✓</span>}{s.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* BPM */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-[9px] font-black uppercase tracking-widest text-slate-500">BPM</label>
                {bpm && (
                  <button onClick={() => { setBpm(null); setBpmInput(''); }}
                    className="text-[9px] text-slate-500 hover:text-red-400 transition-colors">✕ Libre</button>
                )}
              </div>
              <div className="flex flex-wrap gap-1.5 mb-2">
                {BPM_PRESETS.map(p => (
                  <button key={p.bpm} onClick={() => { setBpm(p.bpm); setBpmInput(String(p.bpm)); }}
                    title={p.desc}
                    className={`px-2.5 py-1.5 rounded-xl border text-[10px] font-bold transition-all ${bpm === p.bpm ? 'bg-fuchsia-500/25 border-fuchsia-500/50 text-fuchsia-200' : 'bg-white/3 border-white/10 text-slate-400 hover:text-white hover:border-white/25'}`}>
                    {p.label}
                    <span className="ml-1 opacity-50 font-mono">{p.bpm}</span>
                  </button>
                ))}
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="number" min={40} max={220} value={bpmInput}
                  onChange={e => {
                    setBpmInput(e.target.value);
                    const v = parseInt(e.target.value);
                    if (v >= 40 && v <= 220) setBpm(v); else if (!e.target.value) setBpm(null);
                  }}
                  placeholder="BPM personnalisé (40-220)"
                  className="flex-1 bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-[12px] text-slate-200 placeholder:text-slate-600 outline-none focus:border-fuchsia-500/40 transition-colors font-mono" />
                {bpm && (
                  <div className="shrink-0 px-3 py-2 bg-fuchsia-500/15 border border-fuchsia-500/30 rounded-xl text-[12px] font-black text-fuchsia-300 font-mono">
                    {bpm} BPM
                  </div>
                )}
              </div>
            </div>

            {/* Mode paroles / instrumental */}
            <div>
              <label className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-2 block">Mode</label>
              <div className="flex gap-2 mb-3">
                {(['lyrics', 'instrumental'] as const).map(m => (
                  <button key={m} onClick={() => setLyricsMode(m)}
                    className={`flex-1 py-2 rounded-xl border text-[11px] font-bold transition-all ${lyricsMode === m ? 'bg-fuchsia-500/20 border-fuchsia-500/40 text-fuchsia-300' : 'bg-white/3 border-white/8 text-slate-400 hover:text-white'}`}>
                    {m === 'lyrics' ? '🎤 Avec paroles' : '🎸 Instrumental'}
                  </button>
                ))}
              </div>
              {lyricsMode === 'lyrics' && (
                <textarea value={lyrics} onChange={e => handleLyricsChange(e.target.value)}
                  placeholder={LYRICS_PLACEHOLDER} rows={10}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-[12px] text-white placeholder:text-slate-700 outline-none focus:border-fuchsia-500/40 transition-colors resize-none font-mono leading-relaxed" />
              )}
            </div>

            {/* Durée */}
            <div>
              <label className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-2 block">Durée</label>
              <div className="flex gap-2 flex-wrap">
                {DURATIONS.map((d, i) => (
                  <button key={i} onClick={() => setDurIdx(i)}
                    className={`flex-1 py-2 px-1 rounded-xl border text-center transition-all ${durIdx === i ? 'bg-fuchsia-500/25 border-fuchsia-500/50 text-fuchsia-200' : 'bg-white/3 border-white/10 text-slate-400 hover:text-white hover:border-white/25'}`}>
                    <span className="block text-[11px] font-bold">{d.label}</span>
                    <span className="block text-[9px] text-yellow-400/80">{d.cost} 🪙</span>
                  </button>
                ))}
              </div>
              <p className="text-[10px] text-slate-600 mt-1.5 flex items-center gap-1">
                <Clock size={10} />
                {durCfg.min === durCfg.max
                  ? <><span className="text-slate-400 font-bold ml-1">~30s</span> · ACE-Step INT8</>
                  : <><span className="text-slate-400 font-bold ml-1">Durée aléatoire {durCfg.label}</span> · ACE-Step INT8</>
                }
              </p>
            </div>

            {/* Progression */}
            {phase === 'generating' && (
              <div className="bg-fuchsia-950/30 border border-fuchsia-500/20 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold text-fuchsia-300">{phaseLabel || 'Génération en cours...'}</span>
                  <span className="text-[11px] font-mono text-fuchsia-400">{progress}%</span>
                </div>
                <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                  <div className="h-full transition-all duration-700 rounded-full bg-gradient-to-r from-fuchsia-500 to-purple-500" style={{ width: `${progress}%` }} />
                </div>
                <button onClick={handleCancel}
                  className="w-full py-2 rounded-xl bg-white/5 hover:bg-red-900/20 border border-white/10 hover:border-red-500/30 text-[11px] text-slate-400 hover:text-red-400 transition-all flex items-center justify-center gap-2">
                  Annuler
                </button>
              </div>
            )}

            {/* Erreur */}
            {phase === 'error' && error && (
              <div className="flex items-start gap-2 bg-red-950/30 border border-red-500/20 rounded-xl px-4 py-3">
                <AlertCircle size={16} className="text-red-400 shrink-0" />
                <p className="text-[11px] text-red-300">{error}</p>
              </div>
            )}

            {/* Bouton générer */}
            {tokenError && (
              <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 text-center">
                <p className="text-red-300 text-xs font-bold mb-1">{tokenError}</p>
                <p className="text-slate-500 text-[10px] mb-2">Recharge tes tokens pour créer ce morceau.</p>
                <button
                  onClick={() => window.dispatchEvent(new CustomEvent('lea-navigate', { detail: { module: 'shop' } }))}
                  className="bg-gradient-to-r from-yellow-500 to-orange-500 text-white text-[10px] font-black uppercase tracking-widest px-4 py-2 rounded-lg hover:opacity-90 active:scale-95 transition-all"
                >
                  Recharger mes tokens 🪙
                </button>
              </div>
            )}
            {phase !== 'generating' && (
              <button onClick={handleGenerate}
                className="w-full py-4 rounded-2xl font-black uppercase tracking-widest text-sm flex items-center justify-center gap-2 transition-all bg-gradient-to-r from-fuchsia-600 to-purple-700 text-white shadow-[0_0_20px_rgba(217,70,239,0.3)] hover:scale-[1.01] active:scale-95">
                <Sparkles size={16} /> Générer · {DURATIONS[durIdx].cost} 🪙
              </button>
            )}
          </div>
        )}

        {/* ════ BIBLIOTHÈQUE ════ */}
        {tab === 'library' && (
          <div className="p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[11px] font-black uppercase tracking-widest text-slate-400">Mes morceaux</h3>
              <button onClick={fetchLibrary} className="w-7 h-7 bg-white/5 hover:bg-white/10 rounded-lg flex items-center justify-center transition-all">
                <RefreshCw size={12} className={`text-slate-400 ${loadingLib ? 'animate-spin' : ''}`} />
              </button>
            </div>
            {library.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 opacity-30">
                <Music size={48} className="text-slate-600 mb-3" />
                <p className="text-sm font-bold uppercase tracking-widest text-slate-600">Aucun morceau</p>
              </div>
            ) : (
              <div className="space-y-2">
                {library.map(track => (
                  <div key={track.url} className="bg-white/3 border border-white/8 hover:border-fuchsia-500/20 rounded-xl p-3 flex items-center gap-3 transition-all group">
                    <div className="w-9 h-9 bg-fuchsia-500/10 rounded-lg flex items-center justify-center shrink-0">
                      {track.engine === 'rapide'
                        ? <Zap size={15} className="text-amber-400/60" />
                        : <Disc3 size={16} className="text-fuchsia-400/60" />
                      }
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-[11px] font-bold text-white truncate">{track.title || track.name}</p>
                        {track.engine === 'rapide' && (
                          <span className="text-[9px] font-black uppercase px-1.5 py-0.5 rounded-full shrink-0 bg-amber-500/15 text-amber-400">rapide</span>
                        )}
                      </div>
                      <p className="text-[10px] text-slate-500">
                        {new Date(track.date * 1000).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>
                    <div className="flex gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={() => playTrack(track.url)}
                        className="w-7 h-7 bg-fuchsia-500/20 hover:bg-fuchsia-500/40 rounded-lg flex items-center justify-center transition-all">
                        <Play size={11} className="text-fuchsia-300" />
                      </button>
                      <a href={`${SERVER()}${track.url}`}
                        download={track.title ? `${track.title}.wav` : track.name}
                        className="w-7 h-7 bg-white/5 hover:bg-white/10 rounded-lg flex items-center justify-center transition-all">
                        <Download size={11} className="text-slate-400" />
                      </a>
                      <button onClick={() => deleteTrack(track)}
                        className="w-7 h-7 bg-red-950/50 hover:bg-red-900/60 rounded-lg flex items-center justify-center transition-all">
                        <Trash2 size={11} className="text-red-400" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
