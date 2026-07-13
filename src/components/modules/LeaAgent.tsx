import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Brain, Send, FolderOpen, FileText, Terminal, Code2, CheckCircle,
  XCircle, ChevronDown, ChevronRight, Image as ImageIcon, X,
  Sparkles, Bot, User, RefreshCw, Trash2, Eye, EyeOff, Loader,
  Mic, MicOff, Volume2, VolumeX
} from 'lucide-react';

const SERVER = () => (window as any).LEA_SERVER_URL || '';
const currentUser = () => localStorage.getItem('lea_currentUser') || '';
const SESSION_KEY = 'lea_agent_session_id';

// ── Types ─────────────────────────────────────────────────────────────────────

type MsgKind =
  | { kind: 'user';    text: string }
  | { kind: 'text';    text: string }
  | { kind: 'thinking'; text: string }
  | { kind: 'compact'; text: string }
  | { kind: 'tool_call'; tool: string; args: any }
  | { kind: 'tool_result'; tool: string; result: any }
  | { kind: 'confirm'; editId: string; tool: string; args: any; status: 'pending' | 'accepted' | 'rejected' }
  | { kind: 'turn_summary'; reads: number; commands: number; searches: number; edits: number; creates: number; code: number }
  | { kind: 'error';   text: string }
  | { kind: 'syntax_error'; file: string; errors: string }
  | { kind: 'image_analysis'; analysis: string };

type TurnStats = { reads: number; commands: number; searches: number; edits: number; creates: number; code: number };

// ── Helpers ────────────────────────────────────────────────────────────────────

const toolLabel: Record<string, { icon: React.ReactNode; label: string; color: string }> = {
  read_file:                   { icon: <FileText size={12}/>,   label: 'Lecture fichier',    color: 'text-cyan-400 border-cyan-500/20 bg-cyan-500/5' },
  list_files:                  { icon: <FolderOpen size={12}/>, label: 'Liste dossier',      color: 'text-blue-400 border-blue-500/20 bg-blue-500/5' },
  search_in_files:             { icon: <FileText size={12}/>,   label: 'Recherche',           color: 'text-indigo-400 border-indigo-500/20 bg-indigo-500/5' },
  run_command:                 { icon: <Terminal size={12}/>,   label: 'Commande shell',     color: 'text-yellow-400 border-yellow-500/20 bg-yellow-500/5' },
  generate_code_with_deepseek: { icon: <Code2 size={12}/>,     label: 'DeepSeek génère',    color: 'text-purple-400 border-purple-500/20 bg-purple-500/5' },
  propose_edit:                { icon: <FileText size={12}/>,   label: 'Modification fichier', color: 'text-orange-400 border-orange-500/20 bg-orange-500/5' },
  propose_create_file:         { icon: <FileText size={12}/>,   label: 'Création fichier',   color: 'text-green-400 border-green-500/20 bg-green-500/5' },
  write_file:                  { icon: <FileText size={12}/>,   label: 'Réécriture fichier', color: 'text-orange-400 border-orange-500/20 bg-orange-500/5' },
  update_memory:               { icon: <Brain size={12}/>,      label: 'Mise à jour mémoire', color: 'text-fuchsia-400 border-fuchsia-500/20 bg-fuchsia-500/5' },
};

const argSummary = (tool: string, args: any): string => {
  if (tool === 'read_file')                   return args.path?.split('/').slice(-2).join('/') || '';
  if (tool === 'list_files')                  return args.path?.split('/').slice(-2).join('/') || '';
  if (tool === 'search_in_files')             return `"${args.pattern}" dans ${args.directory?.split('/').pop()}`;
  if (tool === 'run_command')                 return args.command?.slice(0, 60) || '';
  if (tool === 'generate_code_with_deepseek') return args.task?.slice(0, 60) || '';
  if (tool === 'propose_edit')                return args.file_path?.split('/').slice(-2).join('/') || '';
  if (tool === 'propose_create_file')         return args.file_path?.split('/').slice(-2).join('/') || '';
  if (tool === 'write_file')                  return args.file_path?.split('/').slice(-2).join('/') || '';
  if (tool === 'update_memory')               return args.file || '';
  return '';
};

// ── Diff ligne par ligne (style git) ─────────────────────────────────────────
const DiffView: React.FC<{ oldStr: string; newStr: string }> = ({ oldStr, newStr }) => {
  const oldLines = (oldStr || '').split('\n');
  const newLines = (newStr || '').split('\n');
  return (
    <div className="rounded-xl overflow-hidden border border-white/10 font-mono text-[9px] bg-black/30">
      <div className="bg-white/3 px-3 py-1 text-slate-600 border-b border-white/5 flex items-center gap-2">
        <span className="text-red-400/60">−{oldLines.length}</span>
        <span className="text-green-400/60">+{newLines.length}</span>
        <span className="text-slate-700">lignes</span>
      </div>
      <div className="max-h-52 overflow-y-auto">
        {oldLines.map((line, i) => (
          <div key={`r${i}`} className="flex items-start border-b border-white/3 bg-red-500/8 hover:bg-red-500/12">
            <span className="w-7 text-right pr-2 text-red-500/30 shrink-0 select-none py-0.5 border-r border-white/5 text-[8px] leading-5">{i + 1}</span>
            <span className="text-red-400 w-5 text-center shrink-0 py-0.5 leading-5 select-none">−</span>
            <span className="text-red-300/80 py-0.5 px-1.5 whitespace-pre-wrap break-all flex-1 leading-5">{line || ' '}</span>
          </div>
        ))}
        {newLines.map((line, i) => (
          <div key={`a${i}`} className="flex items-start border-b border-white/3 bg-green-500/8 hover:bg-green-500/12">
            <span className="w-7 text-right pr-2 text-green-500/30 shrink-0 select-none py-0.5 border-r border-white/5 text-[8px] leading-5">{i + 1}</span>
            <span className="text-green-400 w-5 text-center shrink-0 py-0.5 leading-5 select-none">+</span>
            <span className="text-green-300/80 py-0.5 px-1.5 whitespace-pre-wrap break-all flex-1 leading-5">{line || ' '}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

// ── Résumé de tour (fin de tour) ─────────────────────────────────────────────
const STAT_ICONS: Record<string, string> = {
  reads: '📄', searches: '🔍', commands: '⚡', code: '🤖', edits: '✏️', creates: '📁',
};
const TurnSummaryLine: React.FC<TurnStats> = ({ reads, commands, searches, edits, creates, code }) => {
  const items: { key: string; label: string }[] = [];
  if (reads    > 0) items.push({ key: 'reads',    label: `${reads} fichier${reads > 1 ? 's' : ''} lu${reads > 1 ? 's' : ''}` });
  if (searches > 0) items.push({ key: 'searches', label: `${searches} recherche${searches > 1 ? 's' : ''}` });
  if (commands > 0) items.push({ key: 'commands', label: `${commands} commande${commands > 1 ? 's' : ''}` });
  if (code     > 0) items.push({ key: 'code',     label: `${code} génération${code > 1 ? 's' : ''} IA` });
  if (edits    > 0) items.push({ key: 'edits',    label: `${edits} modification${edits > 1 ? 's' : ''}` });
  if (creates  > 0) items.push({ key: 'creates',  label: `${creates} création${creates > 1 ? 's' : ''}` });
  if (items.length === 0) return null;
  return (
    <div className="flex flex-wrap items-center gap-1.5 pl-9 py-1.5">
      <span className="text-slate-600 text-[10px] font-mono mr-0.5">└─</span>
      {items.map(({ key, label }) => (
        <span key={key} className="inline-flex items-center gap-1 text-[10px] text-slate-400 bg-white/5 border border-white/8 rounded-full px-2 py-0.5 font-mono">
          <span>{STAT_ICONS[key]}</span>{label}
        </span>
      ))}
    </div>
  );
};

// ── Live stats bar (pendant le chargement) ────────────────────────────────────
const LiveStatsBar: React.FC<TurnStats & { visible: boolean }> = ({ visible, reads, commands, searches, edits, creates, code }) => {
  if (!visible) return null;
  const total = reads + commands + searches + edits + creates + code;
  if (total === 0) return null;
  const parts: string[] = [];
  if (reads    > 0) parts.push(`${reads} lu`);
  if (searches > 0) parts.push(`${searches} rech.`);
  if (commands > 0) parts.push(`${commands} cmd`);
  if (code     > 0) parts.push(`${code} IA`);
  if (edits    > 0) parts.push(`${edits} modif.`);
  if (creates  > 0) parts.push(`${creates} créé`);
  return (
    <div className="flex items-center gap-2 px-3 py-1.5 mx-1 mb-1 bg-purple-500/8 border border-purple-500/15 rounded-xl">
      <Loader size={10} className="animate-spin text-purple-400 shrink-0"/>
      <span className="text-[10px] text-purple-300/70 font-mono">{parts.join(' · ')}</span>
    </div>
  );
};

// ── Sous-composant : bulle outil ──────────────────────────────────────────────
const ToolBubble: React.FC<{ msg: Extract<MsgKind, { kind: 'tool_call' | 'tool_result' }> }> = ({ msg }) => {
  const [open, setOpen] = useState(false);
  const info = toolLabel[msg.tool] || { icon: <Code2 size={12}/>, label: msg.tool, color: 'text-slate-400 border-white/10 bg-white/3' };

  if (msg.kind === 'tool_call') {
    return (
      <div className={`flex items-center gap-2 px-3 py-1.5 rounded-xl border text-[10px] font-bold w-fit ${info.color}`}>
        <span className="animate-pulse">{info.icon}</span>
        <span>{info.label}</span>
        {argSummary(msg.tool, msg.args) && <span className="opacity-60 font-mono">— {argSummary(msg.tool, msg.args)}</span>}
      </div>
    );
  }

  // tool_result
  const hasContent = msg.result && Object.keys(msg.result).length > 0 && !msg.result.pending_confirmation;
  const preview = msg.result?.content?.slice(0, 120) || msg.result?.output?.slice(0, 120) || msg.result?.listing?.slice(0, 120) || msg.result?.results?.slice(0, 80) || msg.result?.generated_code?.slice(0, 120) || (msg.result?.error ? `❌ ${msg.result.error}` : '');

  return (
    <button onClick={() => setOpen(o => !o)}
      className={`text-left px-3 py-1.5 rounded-xl border text-[10px] w-fit max-w-full ${info.color} ${hasContent ? 'cursor-pointer hover:opacity-80' : 'cursor-default'} transition-all`}>
      <div className="flex items-center gap-2">
        {msg.result?.error ? <XCircle size={11} className="text-red-400 shrink-0"/> : <CheckCircle size={11} className="shrink-0"/>}
        <span className="font-bold">{info.label} — {msg.result?.error ? 'Erreur' : 'OK'}</span>
        {hasContent && (open ? <ChevronDown size={10}/> : <ChevronRight size={10}/>)}
      </div>
      {!open && preview && <p className="font-mono opacity-50 truncate mt-0.5">{preview}</p>}
      {open && hasContent && (
        <pre className="mt-2 text-[9px] font-mono whitespace-pre-wrap break-all max-h-48 overflow-y-auto opacity-80 border-t border-white/10 pt-2">
          {JSON.stringify(msg.result, null, 2).slice(0, 3000)}
        </pre>
      )}
    </button>
  );
};

// ── Sous-composant : carte erreur syntaxe ────────────────────────────────────
const SyntaxErrorCard: React.FC<{ file: string; errors: string }> = ({ file, errors }) => {
  const shortFile = file.split('/').slice(-2).join('/');
  return (
    <div className="rounded-xl border border-red-500/40 bg-red-500/8 overflow-hidden">
      <div className="flex items-center gap-2 px-3 py-2 border-b border-red-500/20 bg-red-500/10">
        <span className="text-red-400 text-sm">⚠️</span>
        <span className="text-red-300 text-[11px] font-black uppercase tracking-widest">Erreur syntaxe détectée</span>
        <span className="text-red-500/60 text-[9px] font-mono ml-auto">{shortFile}</span>
      </div>
      <pre className="text-red-300/80 text-[9px] font-mono px-3 py-2 whitespace-pre-wrap break-all max-h-40 overflow-y-auto leading-4">{errors}</pre>
      <div className="px-3 py-2 border-t border-red-500/20 text-red-400/60 text-[9px]">
        Dis à Léa : "Corrige l'erreur de syntaxe dans {shortFile}"
      </div>
    </div>
  );
};

// ── Sous-composant : carte confirmation ───────────────────────────────────────
const ConfirmCard: React.FC<{
  msg: Extract<MsgKind, { kind: 'confirm' }>;
  onConfirm: (editId: string, confirmed: boolean) => void;
}> = ({ msg, onConfirm }) => {
  const [showDiff, setShowDiff] = useState(true);
  const { args, tool, status } = msg;

  const isEdit   = tool === 'propose_edit';
  const isCreate = tool === 'propose_create_file' || tool === 'write_file';

  return (
    <div className={`rounded-2xl border overflow-hidden transition-all ${
      status === 'accepted' ? 'border-green-500/30 bg-green-500/5' :
      status === 'rejected' ? 'border-red-500/20 bg-red-500/5 opacity-60' :
      'border-orange-500/30 bg-orange-500/5'
    }`}>
      <div className="px-4 py-3 border-b border-white/8 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <FileText size={13} className={status === 'accepted' ? 'text-green-400' : status === 'rejected' ? 'text-red-400' : 'text-orange-400'} />
          <span className="text-xs font-black text-white">
            {isCreate ? 'Créer fichier' : 'Modifier fichier'}
          </span>
          <span className="font-mono text-[10px] text-slate-400">{args.file_path?.split('/').slice(-3).join('/')}</span>
        </div>
        {status === 'pending' && (
          <button onClick={() => setShowDiff(v => !v)} className="p-1 text-slate-500 hover:text-white transition-colors">
            {showDiff ? <EyeOff size={13}/> : <Eye size={13}/>}
          </button>
        )}
        {status === 'accepted' && <span className="text-[9px] font-black text-green-400 uppercase tracking-widest">Appliqué ✓</span>}
        {status === 'rejected' && <span className="text-[9px] font-black text-red-400 uppercase tracking-widest">Annulé</span>}
      </div>

      {args.explanation && (
        <p className="px-4 pt-3 text-xs text-slate-300 leading-relaxed">{args.explanation}</p>
      )}

      {showDiff && status === 'pending' && (
        <div className="px-4 py-3 space-y-2">
          {isEdit && (
            <DiffView oldStr={args.old_string || ''} newStr={args.new_string || ''} />
          )}
          {isCreate && (
            <div className="rounded-xl overflow-hidden border border-green-500/20">
              <div className="bg-green-500/10 px-3 py-1 text-[9px] font-black uppercase tracking-widest text-green-400">Nouveau fichier</div>
              <pre className="px-3 py-2 text-[10px] font-mono text-green-300/80 whitespace-pre-wrap break-all max-h-48 overflow-y-auto bg-green-500/3">{args.content?.slice(0, 2000)}{args.content?.length > 2000 ? '\n...[tronqué]' : ''}</pre>
            </div>
          )}
        </div>
      )}

      {status === 'pending' && (
        <div className="px-4 pb-4 flex gap-2">
          <button onClick={() => onConfirm(msg.editId, true)}
            className="flex-1 py-2.5 bg-green-500/20 border border-green-500/40 text-green-400 rounded-xl text-xs font-black uppercase tracking-wider hover:bg-green-500/30 active:scale-95 transition-all flex items-center justify-center gap-1.5">
            <CheckCircle size={13}/> Confirmer
          </button>
          <button onClick={() => onConfirm(msg.editId, false)}
            className="flex-1 py-2.5 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-xs font-black uppercase tracking-wider hover:bg-red-500/20 active:scale-95 transition-all flex items-center justify-center gap-1.5">
            <XCircle size={13}/> Annuler
          </button>
        </div>
      )}
    </div>
  );
};

// ── Composant principal ────────────────────────────────────────────────────────
export const LeaAgent = () => {
  const user   = currentUser();
  const server = SERVER();

  const [isAdmin, setIsAdmin]         = useState(false);
  const [adminChecked, setAdminChecked] = useState(false);
  const [messages, setMessages]       = useState<MsgKind[]>([]);
  const [input, setInput]             = useState('');
  const [loading, setLoading]         = useState(false);
  const [sessionId, setSessionId]     = useState<string>(() => localStorage.getItem(SESSION_KEY) || '');
  const [imageFile, setImageFile]     = useState<File | null>(null);
  const [imageB64, setImageB64]       = useState<string | null>(null);

  const [isListening, setIsListening] = useState(false);
  const [speakingIdx, setSpeakingIdx] = useState<number | null>(null);
  const [qwenStatus, setQwenStatus] = useState<'loading' | 'ready' | 'error'>('loading');

  const bottomRef      = useRef<HTMLDivElement>(null);
  const inputRef       = useRef<HTMLTextAreaElement>(null);
  const imgInputRef    = useRef<HTMLInputElement>(null);
  const abortRef       = useRef<AbortController | null>(null);
  const recognitionRef = useRef<any>(null);
  const audioRef       = useRef<HTMLAudioElement | null>(null);
  const turnStatsRef   = useRef<TurnStats>({ reads: 0, commands: 0, searches: 0, edits: 0, creates: 0, code: 0 });
  const [liveTurnStats, setLiveTurnStats] = useState<TurnStats>({ reads: 0, commands: 0, searches: 0, edits: 0, creates: 0, code: 0 });
  const autoVerifyDoneRef = useRef(false);

  // Vérification admin via profil serveur
  useEffect(() => {
    if (!user) { setAdminChecked(true); return; }
    fetch(`${server}/api/user/profile/${user}`)
      .then(r => r.ok ? r.json() : null)
      .then(d => {
        if (d?.isAdmin || d?.abonnement === 'god_mode') setIsAdmin(true);
        setAdminChecked(true);
      })
      .catch(() => setAdminChecked(true));
  }, [user, server]);

  // Warmup à l'ouverture, unload à la fermeture — gestion VRAM automatique
  useEffect(() => {
    if (!user) return;
    setQwenStatus('loading');
    fetch(`${server}/api/agent/warmup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: user }),
    })
      .then(r => r.json())
      .then(d => setQwenStatus(d.ready ? 'ready' : 'error'))
      .catch(() => setQwenStatus('error'));

    // Cleanup : libère Qwen quand l'utilisateur quitte le module
    return () => {
      fetch(`${server}/api/agent/unload`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user }),
      }).catch(() => {});
    };
  }, [user, server]);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  // Persister sessionId
  useEffect(() => {
    if (sessionId) localStorage.setItem(SESSION_KEY, sessionId);
  }, [sessionId]);

  // ── Charger image ────────────────────────────────────────────────────────────
  const handleImagePick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setImageFile(f);
    const reader = new FileReader();
    reader.onload = () => {
      const b64 = (reader.result as string).split(',')[1];
      setImageB64(b64);
    };
    reader.readAsDataURL(f);
    e.target.value = '';
  };

  // ── Analyser image avec LLaVA ───────────────────────────────────────────────
  const analyzeImage = async () => {
    if (!imageB64) return;
    setLoading(true);
    setMessages(m => [...m, { kind: 'user', text: `[Image envoyée : ${imageFile?.name}]` }]);
    setMessages(m => [...m, { kind: 'thinking', text: 'LLaVA analyse le design...' }]);
    try {
      const r = await fetch(`${server}/api/agent/analyze-image`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageBase64: imageB64, username: user }),
      });
      const d = await r.json();
      setMessages(m => m.filter(x => x.kind !== 'thinking').concat({ kind: 'image_analysis', analysis: d.analysis || 'Pas de réponse' }));
    } catch (e: any) {
      setMessages(m => m.filter(x => x.kind !== 'thinking').concat({ kind: 'error', text: e.message }));
    }
    setImageFile(null); setImageB64(null);
    setLoading(false);
  };

  // ── Envoyer message à l'agent ─────────────────────────────────────────────
  const sendMessage = useCallback(async (text: string) => {
    if (!text.trim() || loading) return;
    const userMsg = text.trim();
    setInput('');
    setLoading(true);
    setMessages(m => [...m, { kind: 'user', text: userMsg }]);
    const zeroStats = { reads: 0, commands: 0, searches: 0, edits: 0, creates: 0, code: 0 };
    turnStatsRef.current = { ...zeroStats };
    setLiveTurnStats(zeroStats);
    // Réinitialiser le flag SEULEMENT si c'est un vrai message utilisateur (pas l'auto-verify)
    if (!text.startsWith('Relis maintenant')) {
      autoVerifyDoneRef.current = false;
    }

    abortRef.current = new AbortController();

    try {
      const r = await fetch(`${server}/api/agent`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMsg, sessionId: sessionId || undefined, username: user }),
        signal: abortRef.current.signal,
      });

      if (!r.ok) { setMessages(m => [...m, { kind: 'error', text: `Erreur serveur ${r.status}` }]); setLoading(false); return; }

      const reader = r.body?.getReader();
      const dec    = new TextDecoder();
      if (!reader) { setLoading(false); return; }

      let buffer = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += dec.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (!line.startsWith('data: ')) continue;
          const raw = line.slice(6).trim();
          if (raw === '[DONE]') { setLoading(false); break; }
          try {
            const ev = JSON.parse(raw);
            handleEvent(ev);
          } catch {}
        }
      }
    } catch (e: any) {
      if (e.name !== 'AbortError') setMessages(m => [...m, { kind: 'error', text: e.message }]);
    }
    setLoading(false);
  }, [server, user, sessionId, loading]);

  const handleEvent = (ev: any) => {
    switch (ev.type) {
      case 'session_id':
        setSessionId(ev.sessionId);
        break;

      case 'thinking':
        setMessages(m => {
          const last = m[m.length - 1];
          if (last?.kind === 'thinking') return [...m.slice(0, -1), { kind: 'thinking', text: ev.text }];
          return [...m, { kind: 'thinking', text: ev.text }];
        });
        break;

      case 'tool_call': {
        const s = turnStatsRef.current;
        if (ev.tool === 'read_file')                                          s.reads++;
        else if (ev.tool === 'run_command')                                   s.commands++;
        else if (ev.tool === 'search_in_files' || ev.tool === 'list_files')  s.searches++;
        else if (ev.tool === 'propose_edit' || ev.tool === 'write_file')     s.edits++;
        else if (ev.tool === 'propose_create_file')                           s.creates++;
        else if (ev.tool === 'generate_code_with_deepseek')                   s.code++;
        setLiveTurnStats({ ...s });
        setMessages(m => {
          const filtered = m.filter(x => x.kind !== 'thinking');
          return [...filtered, { kind: 'tool_call', tool: ev.tool, args: ev.args }];
        });
        break;
      }

      case 'tool_result':
        setMessages(m => [...m, { kind: 'tool_result', tool: ev.tool, result: ev.result }]);
        break;

      case 'confirmation_required': {
        const stats = { ...turnStatsRef.current };
        setMessages(m => {
          const filtered = m.filter(x => x.kind !== 'thinking');
          const withSummary = stats.reads + stats.commands + stats.searches + stats.code + stats.edits + stats.creates > 0
            ? [...filtered, { kind: 'turn_summary' as const, ...stats }]
            : filtered;
          return [...withSummary, { kind: 'confirm', editId: ev.editId, tool: ev.tool, args: ev.args, status: 'pending' }];
        });
        setLoading(false);
        break;
      }

      case 'compact':
        setMessages(m => [...m.filter(x => x.kind !== 'thinking'), { kind: 'compact', text: ev.text }]);
        break;

      case 'response': {
        const stats = { ...turnStatsRef.current };
        setMessages(m => {
          const filtered = m.filter(x => x.kind !== 'thinking');
          const withSummary = stats.reads + stats.commands + stats.searches + stats.code + stats.edits + stats.creates > 0
            ? [...filtered, { kind: 'turn_summary' as const, ...stats }]
            : filtered;
          if (ev.text?.trim()) return [...withSummary, { kind: 'text', text: ev.text }];
          return withSummary;
        });
        break;
      }

      case 'error':
        setMessages(m => m.filter(x => x.kind !== 'thinking').concat({ kind: 'error', text: ev.text }));
        break;
    }
  };

  // ── Confirmer / rejeter une modification ───────────────────────────────────
  const handleConfirm = async (editId: string, confirmed: boolean) => {
    // Mettre à jour l'UI immédiatement
    setMessages(m => m.map(msg =>
      msg.kind === 'confirm' && msg.editId === editId
        ? { ...msg, status: confirmed ? 'accepted' : 'rejected' }
        : msg
    ));

    try {
      const r = await fetch(`${server}/api/agent/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, editId, confirmed, username: user }),
      });
      const d = await r.json();
      if (confirmed && d.applied) {
        const shortPath = d.file?.split('/').slice(-2).join('/');
        setMessages(m => [...m, { kind: 'text', text: `✅ Fichier modifié : \`${shortPath}\`` }]);
        if (d.syntaxErrors) {
          setMessages(m => [...m, { kind: 'syntax_error', file: d.file, errors: d.syntaxErrors }]);
        }
      } else if (confirmed && !d.applied) {
        // Échec silencieux → remettre le statut en pending + afficher l'erreur
        setMessages(m => m.map(msg =>
          msg.kind === 'confirm' && msg.editId === editId ? { ...msg, status: 'pending' } : msg
        ));
        setMessages(m => [...m, { kind: 'error', text: `❌ Modification échouée : ${d.error || 'texte original introuvable'}` }]);
      } else if (!confirmed) {
        setMessages(m => [...m, { kind: 'text', text: '↩️ Modification annulée.' }]);
      }
    } catch (e: any) {
      setMessages(m => [...m, { kind: 'error', text: e.message }]);
    }
  };

  // ── Micro → texte (Web Speech API) ─────────────────────────────────────────
  const toggleListening = () => {
    if (isListening) {
      recognitionRef.current?.stop();
      setIsListening(false);
      return;
    }
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) return;
    const rec = new SR();
    rec.lang = 'fr-FR';
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onresult = (e: any) => {
      const t = e.results[0][0].transcript;
      setInput(prev => prev ? prev + ' ' + t : t);
    };
    rec.onerror  = () => setIsListening(false);
    rec.onend    = () => setIsListening(false);
    recognitionRef.current = rec;
    rec.start();
    setIsListening(true);
  };

  // ── Haut-parleur (Piper TTS — voix configurée dans paramètres) ──────────────
  const toggleSpeak = async (text: string, idx: number) => {
    if (audioRef.current) { audioRef.current.pause(); audioRef.current = null; }
    if (speakingIdx === idx) { setSpeakingIdx(null); return; }
    setSpeakingIdx(idx);
    try {
      const res = await fetch(`${server}/api/voice/speak`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, username: user }),
      });
      const data = await res.json();
      if (!data.audio) { setSpeakingIdx(null); return; }
      const audio = new Audio(data.audio);
      audioRef.current = audio;
      audio.onended = () => { audioRef.current = null; setSpeakingIdx(null); };
      audio.onerror = () => { audioRef.current = null; setSpeakingIdx(null); };
      audio.play();
    } catch { setSpeakingIdx(null); }
  };

  const clearSession = () => {
    localStorage.removeItem(SESSION_KEY);
    setSessionId('');
    setMessages([]);
    setInput('');
  };

  const abort = () => { abortRef.current?.abort(); setLoading(false); };

  const onKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(input); }
  };

  // ── Rendu d'un message ───────────────────────────────────────────────────────
  const renderMsg = (msg: MsgKind, i: number) => {
    switch (msg.kind) {

      case 'user':
        return (
          <div key={i} className="flex justify-end">
            <div className="max-w-[80%] bg-gradient-to-br from-purple-600 to-fuchsia-600 text-white rounded-[1.5rem] rounded-tr-md px-4 py-3 text-sm shadow-[0_4px_18px_rgba(168,85,247,0.25)]">
              <div className="flex items-center gap-1.5 mb-1 opacity-60">
                <User size={9}/><span className="text-[9px] font-black uppercase tracking-widest">{user}</span>
              </div>
              <p className="leading-relaxed">{msg.text}</p>
            </div>
          </div>
        );

      case 'thinking':
        return (
          <div key={i} className="flex items-center gap-2 px-1">
            <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-purple-500 to-fuchsia-600 flex items-center justify-center shrink-0 shadow-[0_0_8px_rgba(168,85,247,0.4)]">
              <Brain size={12} className="text-white animate-pulse"/>
            </div>
            <div className="flex items-center gap-1.5 text-slate-500 text-xs">
              <Loader size={11} className="animate-spin"/>
              <span>{msg.text}</span>
            </div>
          </div>
        );

      case 'compact':
        return (
          <div key={i} className="flex items-center gap-2 py-2 px-1">
            <div className="flex-1 h-px bg-fuchsia-500/20"/>
            <div className="flex items-center gap-1.5 text-fuchsia-400/50 text-[9px] font-black uppercase tracking-widest shrink-0">
              <RefreshCw size={9} className="animate-spin"/>
              <span>Compression de la conversation</span>
              <RefreshCw size={9} className="animate-spin"/>
            </div>
            <div className="flex-1 h-px bg-fuchsia-500/20"/>
          </div>
        );

      case 'tool_call':
      case 'tool_result':
        return (
          <div key={i} className="flex justify-start pl-8">
            <ToolBubble msg={msg}/>
          </div>
        );

      case 'confirm':
        return (
          <div key={i} className="px-1">
            <ConfirmCard msg={msg} onConfirm={handleConfirm}/>
          </div>
        );

      case 'text':
        return (
          <div key={i} className="flex justify-start gap-2.5">
            <div className="w-7 h-7 rounded-xl bg-gradient-to-br from-purple-500 to-fuchsia-600 flex items-center justify-center shrink-0 mt-1 shadow-[0_0_10px_rgba(168,85,247,0.35)]">
              <Bot size={13} className="text-white"/>
            </div>
            <div className="max-w-[85%] bg-[#000b1e] border border-white/10 rounded-[1.5rem] rounded-tl-md px-4 py-3">
              <div className="flex items-center gap-1.5 mb-1">
                <Brain size={9} className="opacity-50"/><span className="text-[9px] font-black uppercase tracking-widest text-purple-400 opacity-50">Léa</span>
                <button onClick={() => toggleSpeak(msg.text, i)}
                  className={`ml-auto p-1 rounded-lg transition-all ${speakingIdx === i ? 'text-fuchsia-400 bg-fuchsia-500/20' : 'text-slate-600 hover:text-fuchsia-400 hover:bg-fuchsia-500/10'}`}
                  title={speakingIdx === i ? 'Arrêter' : 'Écouter'}>
                  {speakingIdx === i ? <VolumeX size={12}/> : <Volume2 size={12}/>}
                </button>
              </div>
              <p className="text-slate-200 text-sm leading-relaxed whitespace-pre-wrap">{msg.text}</p>
            </div>
          </div>
        );

      case 'image_analysis':
        return (
          <div key={i} className="flex justify-start gap-2.5">
            <div className="w-7 h-7 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shrink-0 mt-1 shadow-[0_0_10px_rgba(6,182,212,0.35)]">
              <ImageIcon size={13} className="text-white"/>
            </div>
            <div className="max-w-[85%] bg-[#000b1e] border border-cyan-500/20 rounded-[1.5rem] rounded-tl-md px-4 py-3">
              <div className="flex items-center gap-1.5 mb-1">
                <span className="text-[9px] font-black uppercase tracking-widest text-cyan-400 opacity-60">LLaVA · Analyse design</span>
                <button onClick={() => toggleSpeak(msg.analysis, i)}
                  className={`ml-auto p-1 rounded-lg transition-all ${speakingIdx === i ? 'text-cyan-400 bg-cyan-500/20' : 'text-slate-600 hover:text-cyan-400 hover:bg-cyan-500/10'}`}
                  title={speakingIdx === i ? 'Arrêter' : 'Écouter'}>
                  {speakingIdx === i ? <VolumeX size={12}/> : <Volume2 size={12}/>}
                </button>
              </div>
              <p className="text-slate-200 text-sm leading-relaxed whitespace-pre-wrap">{msg.analysis}</p>
            </div>
          </div>
        );

      case 'turn_summary':
        return <div key={i}><TurnSummaryLine {...msg}/></div>;

      case 'syntax_error':
        return <div key={i} className="px-1"><SyntaxErrorCard file={msg.file} errors={msg.errors}/></div>;

      case 'error':
        return (
          <div key={i} className="flex items-start gap-2 px-1">
            <XCircle size={14} className="text-red-400 shrink-0 mt-0.5"/>
            <p className="text-red-300 text-xs bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2">{msg.text}</p>
          </div>
        );

      default: return null;
    }
  };

  // Guard — chargement
  if (!adminChecked) return (
    <div className="flex items-center justify-center h-full bg-[#020617]">
      <div className="w-6 h-6 border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin"/>
    </div>
  );

  // Guard — accès refusé
  if (!isAdmin) return (
    <div className="flex flex-col items-center justify-center h-full bg-[#020617] gap-4 text-center px-8">
      <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center">
        <XCircle size={32} className="text-red-400"/>
      </div>
      <div>
        <p className="text-white font-black uppercase tracking-widest text-sm">Accès restreint</p>
        <p className="text-slate-500 text-xs mt-2">Léa Agent est réservé à l'administrateur.</p>
      </div>
    </div>
  );

  return (
    <div className="flex flex-col h-full bg-[#020617] text-slate-200">

      {/* HEADER */}
      <div className="shrink-0 px-5 pt-5 pb-4 border-b border-white/5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-gradient-to-br from-purple-500 to-fuchsia-600 rounded-xl flex items-center justify-center shadow-[0_0_16px_rgba(168,85,247,0.4)]">
            <Brain size={16} className="text-white"/>
          </div>
          <div>
            <h1 className="font-black text-white uppercase italic tracking-tight text-sm">Léa Agent</h1>
            <div className="flex items-center gap-1.5 mt-0.5">
              <div className={`w-1.5 h-1.5 rounded-full shrink-0 ${qwenStatus === 'ready' ? 'bg-green-400 shadow-[0_0_4px_rgba(74,222,128,0.8)]' : qwenStatus === 'error' ? 'bg-red-400' : 'bg-yellow-400 animate-pulse'}`}/>
              <p className="text-[9px] uppercase tracking-widest text-slate-500">
                {qwenStatus === 'ready' ? 'Qwen prêt · 60 min' : qwenStatus === 'error' ? 'Qwen indisponible' : 'Chargement Qwen...'}
              </p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {messages.length > 0 && (() => {
            const reads   = messages.filter(m => m.kind === 'tool_call' && (m as any).tool === 'read_file').length;
            const cmds    = messages.filter(m => m.kind === 'tool_call' && (m as any).tool === 'run_command').length;
            const mods    = messages.filter(m => m.kind === 'confirm' && (m as any).status === 'accepted').length;
            const statParts = [
              reads > 0 ? `${reads} lu` : '',
              cmds  > 0 ? `${cmds} cmd` : '',
              mods  > 0 ? `${mods} modif` : '',
            ].filter(Boolean);
            return statParts.length > 0 ? (
              <span className="text-[9px] text-slate-700 font-mono hidden sm:block">{statParts.join(' · ')}</span>
            ) : null;
          })()}
          {messages.length > 0 && (
            <button onClick={clearSession}
              className="p-2 text-slate-600 hover:text-red-400 transition-colors rounded-lg hover:bg-red-500/10"
              title="Nouvelle session">
              <Trash2 size={14}/>
            </button>
          )}
          {loading && (
            <button onClick={abort}
              className="p-2 text-slate-500 hover:text-yellow-400 transition-colors rounded-lg hover:bg-yellow-500/10"
              title="Arrêter">
              <XCircle size={14}/>
            </button>
          )}
        </div>
      </div>

      {/* MESSAGES */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">

        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full gap-4 opacity-40">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-purple-500/20 to-fuchsia-600/20 border border-purple-500/20 flex items-center justify-center">
              <Brain size={32} className="text-purple-400"/>
            </div>
            <div className="text-center">
              <p className="text-white font-black uppercase tracking-widest text-sm">Léa Agent</p>
              <p className="text-slate-500 text-xs mt-1">Dis-moi ce que tu veux coder ou modifier.</p>
              <p className="text-slate-600 text-[10px] mt-0.5">Elle lit les fichiers, génère du code et confirme avant d'agir.</p>
            </div>

            {/* Suggestions */}
            <div className="grid grid-cols-1 gap-2 w-full max-w-xs mt-2">
              {[
                'Montre-moi la structure du projet',
                'Lis le fichier server.cjs et résume-le',
                'Quels modules React existent dans src/ ?',
              ].map(s => (
                <button key={s} onClick={() => sendMessage(s)}
                  className="text-left px-3 py-2.5 bg-white/3 border border-white/8 rounded-xl text-xs text-slate-400 hover:bg-white/6 hover:text-white hover:border-purple-500/30 transition-all active:scale-95">
                  <Sparkles size={10} className="inline mr-1.5 text-purple-400"/>
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg, i) => renderMsg(msg, i))}
        <LiveStatsBar visible={loading} {...liveTurnStats}/>
        <div ref={bottomRef}/>
      </div>

      {/* PREVIEW IMAGE */}
      {imageFile && (
        <div className="mx-4 mb-2 bg-cyan-500/8 border border-cyan-500/20 rounded-xl px-3 py-2.5 flex items-center gap-3">
          <ImageIcon size={13} className="text-cyan-400 shrink-0"/>
          <span className="text-xs text-cyan-300 flex-1 truncate">{imageFile.name}</span>
          <button onClick={analyzeImage} disabled={loading}
            className="px-3 py-1 bg-cyan-500 hover:bg-cyan-400 text-black text-[10px] font-black rounded-lg transition-all active:scale-95 disabled:opacity-40">
            Analyser
          </button>
          <button onClick={() => { setImageFile(null); setImageB64(null); }} className="text-slate-500 hover:text-red-400 transition-colors">
            <X size={13}/>
          </button>
        </div>
      )}

      {/* INPUT */}
      <div className="shrink-0 px-4 pb-4 pt-2 border-t border-white/5">
        <div className="flex items-end gap-2">
          <div className="flex-1 bg-[#000b1e] border border-white/10 focus-within:border-purple-500/40 rounded-2xl flex items-end p-1.5 transition-colors">
            <button onClick={() => imgInputRef.current?.click()}
              className="p-2 text-slate-600 hover:text-cyan-400 transition-colors shrink-0"
              title="Analyser une image avec LLaVA">
              <ImageIcon size={16}/>
            </button>
            <textarea ref={inputRef} value={input} onChange={e => setInput(e.target.value)} onKeyDown={onKeyDown}
              placeholder={qwenStatus === 'loading' ? 'Chargement de Qwen en VRAM...' : qwenStatus === 'error' ? 'Qwen indisponible — vérifie Ollama' : 'Dis à Léa ce que tu veux faire... (Entrée pour envoyer)'}
              disabled={loading || qwenStatus !== 'ready'}
              rows={1}
              className="flex-1 bg-transparent text-white outline-none resize-none max-h-36 py-2.5 px-2 text-sm placeholder:text-slate-600 disabled:opacity-50"
            />
            <button onClick={toggleListening} disabled={loading}
              className={`p-2 transition-all shrink-0 rounded-xl ${isListening ? 'text-red-400 bg-red-500/20 animate-pulse' : 'text-slate-600 hover:text-fuchsia-400 hover:bg-fuchsia-500/10'} disabled:opacity-30`}
              title={isListening ? 'Arrêter l\'écoute' : 'Dicter un message'}>
              {isListening ? <MicOff size={16}/> : <Mic size={16}/>}
            </button>
          </div>
          <button onClick={() => sendMessage(input)} disabled={!input.trim() || loading || qwenStatus !== 'ready'}
            className={`p-3.5 rounded-2xl transition-all shrink-0 ${input.trim() && !loading ? 'bg-gradient-to-br from-purple-600 to-fuchsia-600 text-white shadow-[0_0_16px_rgba(168,85,247,0.35)] hover:opacity-90 active:scale-95' : 'bg-white/5 text-slate-600 cursor-not-allowed'}`}>
            {loading ? <Loader size={17} className="animate-spin"/> : <Send size={17}/>}
          </button>
        </div>
        <p className="text-[9px] text-slate-700 mt-1.5 text-center">Maj+Entrée pour nouvelle ligne · Elle confirme avant toute modification</p>
      </div>

      <input ref={imgInputRef} type="file" accept="image/*" className="hidden" onChange={handleImagePick}/>
    </div>
  );
};
