import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  MessageSquare, Users, Lock, ShieldCheck, Search, Plus, X, Send,
  Mic, Paperclip, Image as ImageIcon, PhoneCall, Video, Crown, Globe,
  CheckCheck, Check, Edit3, Trash2, UserPlus, ChevronUp,
  Play, Pause, Smile, Menu, ArrowLeft, UserCircle2, Copy
} from 'lucide-react';
import { useConfirmToast } from '../../hooks/useConfirmToast';

// ═══════════════════════════════════════════════════════════════════
// TYPES
// ═══════════════════════════════════════════════════════════════════
interface ChatMessage {
  id: string;
  chatId: string;
  sender: string;
  text: string;
  timestamp: string;
  type: 'text' | 'audio' | 'image' | 'file';
  fileUrl?: string;
  fileName?: string;
  duration?: number;
  isRead: boolean;
  isEdited?: boolean;
  isDeleted?: boolean;
  reactions: Record<string, string[]>;
}

interface ChatGroup {
  id: string;
  name: string;
  type: 'global' | 'private' | 'group';
  admin?: string | null;
  members: string[];
  unreadCount: number;
}

interface UserPresence {
  username: string;
  status: 'online' | 'away' | 'offline' | 'in_call';
  lastSeen: number;
}

interface CallSession {
  chatId: string;
  callType: 'voice' | 'video';
  roomId: string;
  initiator: string;
  status: 'ringing' | 'active';
}

// ═══════════════════════════════════════════════════════════════════
// CONFIG
// ═══════════════════════════════════════════════════════════════════
const WS_URL   = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`;
const API_BASE = '';
const JITSI    = 'meet.jit.si';
const EMOJIS   = ['👍', '❤️', '😂', '😡', '🔥', '🎉'];
const PAGE     = 50;

// ═══════════════════════════════════════════════════════════════════
// API SERVICE
// ═══════════════════════════════════════════════════════════════════
const api = {
  async fetchConversations(username: string): Promise<ChatGroup[]> {
    try {
      const r = await fetch(`${API_BASE}/api/chat/conversations?username=${encodeURIComponent(username)}`);
      const data = await r.json();
      return Array.isArray(data) ? data.map((c: any) => ({ ...c, unreadCount: 0 })) : [];
    } catch { return []; }
  },
  async fetchMessages(chatId: string, limit = 50, offset = 0): Promise<{ messages: ChatMessage[]; total: number; hasMore: boolean }> {
    try {
      const r = await fetch(`${API_BASE}/api/chat/conversations/${chatId}/messages?limit=${limit}&offset=${offset}`);
      return await r.json();
    } catch { return { messages: [], total: 0, hasMore: false }; }
  },
  async fetchOnlineUsers(): Promise<UserPresence[]> {
    try {
      const r = await fetch(`${API_BASE}/api/chat/users/online`);
      const data = await r.json();
      return Array.isArray(data) ? data : [];
    } catch { return []; }
  },
  async createConversation(participantIds: string[], type: 'private' | 'group', name?: string): Promise<ChatGroup | null> {
    try {
      const r = await fetch(`${API_BASE}/api/chat/conversations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ participantIds, type, name })
      });
      const data = await r.json();
      return data.id ? { ...data, unreadCount: 0 } : null;
    } catch { return null; }
  },
  async deleteConversation(chatId: string): Promise<boolean> {
    try {
      const r = await fetch(`${API_BASE}/api/chat/conversations/${chatId}`, { method: 'DELETE' });
      const data = await r.json();
      return !!data.success;
    } catch { return false; }
  },
  async addMember(chatId: string, userId: string): Promise<boolean> {
    try {
      const r = await fetch(`${API_BASE}/api/chat/conversations/${chatId}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId })
      });
      const data = await r.json();
      return !!data.success;
    } catch { return false; }
  }
};

// ═══════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════
const uid   = () => `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
const hhmm  = () => new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
const fmtS  = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
const b64   = (blob: Blob): Promise<string> =>
  new Promise(r => { const fr = new FileReader(); fr.onload = () => r(fr.result as string); fr.readAsDataURL(blob); });

// ═══════════════════════════════════════════════════════════════════
// HOOK: WebSocket robuste
// ═══════════════════════════════════════════════════════════════════
function useLeaWS(onMsg: (d: any) => void) {
  const wsRef  = useRef<WebSocket | null>(null);
  const queue  = useRef<any[]>([]);
  const ping   = useRef<ReturnType<typeof setInterval> | undefined>(undefined);
  const retry  = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const onRef  = useRef(onMsg);
  onRef.current = onMsg;
  const [conn, setConn] = useState<'local' | 'cloud' | 'offline'>('offline');

  const connect = useCallback(() => {
    try {
      const ws = new WebSocket(WS_URL);
      ws.onopen = () => {
        wsRef.current = ws;
        setConn(window.location.protocol === 'https:' ? 'cloud' : 'local');
        queue.current.forEach(m => ws.send(JSON.stringify(m)));
        queue.current = [];
        clearInterval(ping.current);
        ping.current = setInterval(() => {
          if (ws.readyState === 1) ws.send(JSON.stringify({ type: 'PING', ts: Date.now() }));
        }, 25000);
      };
      ws.onmessage = e => { try { onRef.current(JSON.parse(e.data)); } catch {} };
      ws.onerror   = () => ws.close();
      ws.onclose   = () => {
        clearInterval(ping.current);
        wsRef.current = null;
        setConn('offline');
        clearTimeout(retry.current);
        retry.current = setTimeout(connect, 3000);
      };
    } catch { setConn('offline'); }
  }, []);

  const send = useCallback((data: any) => {
    if (wsRef.current?.readyState === 1) wsRef.current.send(JSON.stringify(data));
    else queue.current.push(data);
  }, []);

  useEffect(() => {
    connect();
    return () => { clearInterval(ping.current); clearTimeout(retry.current); wsRef.current?.close(1000); };
  }, [connect]);

  return { conn, send };
}

// ═══════════════════════════════════════════════════════════════════
// HOOK: Enregistreur vocal
// ═══════════════════════════════════════════════════════════════════
function useAudioRec() {
  const [rec,  setRec]  = useState(false);
  const [secs, setSecs] = useState(0);
  const [blob, setBlob] = useState<Blob | null>(null);
  const mr   = useRef<MediaRecorder | null>(null);
  const bufs = useRef<Blob[]>([]);
  const tick = useRef<ReturnType<typeof setInterval> | undefined>(undefined);

  const start = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const type   = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4';
      const rec    = new MediaRecorder(stream, { mimeType: type });
      mr.current   = rec; bufs.current = [];
      rec.ondataavailable = e => { if (e.data.size > 0) bufs.current.push(e.data); };
      rec.onstop = () => { setBlob(new Blob(bufs.current, { type })); stream.getTracks().forEach(t => t.stop()); };
      rec.start(100); setRec(true); setSecs(0);
      tick.current = setInterval(() => setSecs(s => s + 1), 1000);
    } catch (e) { console.warn('Mic refusé', e); }
  }, []);

  const stop   = useCallback(() => { mr.current?.stop(); clearInterval(tick.current); setRec(false); }, []);
  const cancel = useCallback(() => { mr.current?.stop(); clearInterval(tick.current); setRec(false); setBlob(null); setSecs(0); }, []);
  const clear  = useCallback(() => { setBlob(null); setSecs(0); }, []);

  return { rec, secs, blob, start, stop, cancel, clear };
}

// ═══════════════════════════════════════════════════════════════════
// COMPOSANT: Lecteur audio
// ═══════════════════════════════════════════════════════════════════
const AudioPlayer: React.FC<{ src: string; mine: boolean }> = ({ src, mine }) => {
  const [play, setPlay] = useState(false);
  const [pct,  setPct]  = useState(0);
  const [dur,  setDur]  = useState(0);
  const audio = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    const a = new Audio(src); audio.current = a;
    a.onloadedmetadata = () => setDur(Math.floor(a.duration) || 0);
    a.ontimeupdate  = () => setPct(a.currentTime / (a.duration || 1));
    a.onended       = () => { setPlay(false); setPct(0); };
    return () => a.pause();
  }, [src]);

  const toggle = () => {
    if (!audio.current) return;
    if (play) { audio.current.pause(); setPlay(false); }
    else { audio.current.play(); setPlay(true); }
  };

  return (
    <div className="flex items-center gap-3 min-w-[150px]">
      <button onClick={toggle} className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${mine ? 'bg-white/25 text-white' : 'bg-[#00f2ff]/20 text-[#00f2ff]'}`}>
        {play ? <Pause size={13}/> : <Play size={13}/>}
      </button>
      <div className="flex-1">
        <div className={`h-1 rounded-full ${mine ? 'bg-white/20' : 'bg-white/10'}`}>
          <div className={`h-full rounded-full transition-all ${mine ? 'bg-white' : 'bg-[#00f2ff]'}`} style={{ width: `${pct * 100}%` }}/>
        </div>
        <span className="text-[9px] mt-0.5 opacity-50 block">{fmtS(dur)}</span>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════
// COMPOSANT: Bulle de message
// ═══════════════════════════════════════════════════════════════════
const Bubble: React.FC<{
  msg: ChatMessage; mine: boolean; currentUser: string;
  onReact: (id: string, emoji: string) => void;
  onEdit: (msg: ChatMessage) => void;
  onDelete: (id: string) => void;
}> = ({ msg, mine, currentUser, onReact, onEdit, onDelete }) => {
  const [menu,    setMenu]    = useState(false);
  const [emojiPk, setEmojiPk] = useState(false);
  const lp = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const startLP = () => { lp.current = setTimeout(() => { mine ? setMenu(true) : setEmojiPk(true); }, 600); };
  const endLP   = () => clearTimeout(lp.current);

  if (msg.isDeleted) return (
    <div className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
      <span className="italic text-[10px] text-slate-600 px-3 py-1.5 border border-white/5 rounded-2xl">Message supprimé</span>
    </div>
  );

  const rxEntries = Object.entries(msg.reactions || {}).filter(([, u]) => u.length > 0);

  return (
    <div className={`flex flex-col ${mine ? 'items-end' : 'items-start'} relative`}>
      <div className="flex items-baseline gap-1.5 mb-1 px-2">
        <span className={`text-[9px] font-black uppercase tracking-widest ${mine ? 'text-[#00f2ff]' : 'text-slate-500'}`}>{msg.sender}</span>
        <span className="text-[8px] text-slate-600">{msg.timestamp}</span>
        {msg.isEdited && <span className="text-[8px] text-slate-600 italic">(édité)</span>}
      </div>

      <div
        onMouseDown={startLP} onMouseUp={endLP}
        onTouchStart={startLP} onTouchEnd={endLP}
        className={`relative max-w-[80%] md:max-w-[65%] p-4 rounded-[1.75rem] text-sm shadow-lg select-none cursor-pointer ${
          mine
            ? 'bg-gradient-to-br from-[#0047ff] to-[#00f2ff] text-white rounded-tr-none shadow-[0_4px_18px_rgba(0,242,255,0.2)]'
            : 'bg-[#000b1e] border border-white/10 text-slate-200 rounded-tl-none'
        }`}
      >
        {msg.type === 'audio' && msg.fileUrl ? (
          <AudioPlayer src={msg.fileUrl} mine={mine}/>
        ) : msg.type === 'image' && msg.fileUrl ? (
          <img src={msg.fileUrl} alt="" className="max-w-[200px] rounded-xl cursor-pointer" onClick={() => window.open(msg.fileUrl)}/>
        ) : msg.type === 'file' && msg.fileUrl ? (
          <a href={msg.fileUrl} download={msg.fileName} className="flex items-center gap-2 text-xs underline">
            <Paperclip size={13}/> {msg.fileName || 'Fichier'}
          </a>
        ) : (
          <span className="leading-relaxed font-light">{msg.text}</span>
        )}
        {mine && (
          <div className={`absolute -bottom-4 right-1 ${msg.isRead ? 'text-[#00f2ff]' : 'text-slate-600'}`}>
            {msg.isRead ? <CheckCheck size={11}/> : <Check size={11}/>}
          </div>
        )}
      </div>

      {rxEntries.length > 0 && (
        <div className="flex gap-1 mt-1.5 px-2 flex-wrap">
          {rxEntries.map(([emoji, users]) => (
            <button key={emoji} onClick={() => onReact(msg.id, emoji)}
              className={`text-xs px-2 py-0.5 rounded-full border transition-all ${
                users.includes(currentUser)
                  ? 'bg-[#00f2ff]/20 border-[#00f2ff]/40 text-white'
                  : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'
              }`}>
              {emoji} {users.length}
            </button>
          ))}
        </div>
      )}

      {emojiPk && (
        <div className="absolute z-50 bottom-full mb-2 left-0 flex flex-col gap-1 bg-[#000b1e] border border-white/10 p-2 rounded-2xl shadow-2xl animate-in fade-in">
          <div className="flex gap-1.5">
            {EMOJIS.map(e => (
              <button key={e} onClick={() => { onReact(msg.id, e); setEmojiPk(false); }}
                className="text-xl hover:scale-125 transition-transform">{e}</button>
            ))}
            <button onClick={() => setEmojiPk(false)} className="text-slate-500 ml-1 self-center"><X size={11}/></button>
          </div>
          {msg.type === 'text' && msg.text && (
            <button onClick={() => {
              navigator.clipboard.writeText(msg.text);
              setEmojiPk(false);
            }} className="flex items-center gap-2 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/5 rounded-xl w-full border-t border-white/5 mt-1 pt-2">
              <Copy size={12}/> Copier le message
            </button>
          )}
        </div>
      )}

      {menu && (
        <div className="absolute z-50 right-0 top-full mt-1 bg-[#000b1e] border border-white/10 rounded-2xl shadow-2xl overflow-hidden animate-in fade-in">
          <button onClick={() => { setMenu(false); setEmojiPk(true); }}
            className="flex items-center gap-2 px-4 py-2.5 text-xs text-slate-300 hover:bg-white/5 w-full">
            <Smile size={13}/> Réagir
          </button>
          {msg.type === 'text' && msg.text && (
            <button onClick={() => { navigator.clipboard.writeText(msg.text); setMenu(false); }}
              className="flex items-center gap-2 px-4 py-2.5 text-xs text-slate-300 hover:bg-white/5 w-full">
              <Copy size={13}/> Copier
            </button>
          )}
          {msg.type === 'text' && (
            <button onClick={() => { onEdit(msg); setMenu(false); }}
              className="flex items-center gap-2 px-4 py-2.5 text-xs text-[#00f2ff] hover:bg-[#00f2ff]/10 w-full">
              <Edit3 size={13}/> Modifier
            </button>
          )}
          <button onClick={() => { onDelete(msg.id); setMenu(false); }}
            className="flex items-center gap-2 px-4 py-2.5 text-xs text-red-400 hover:bg-red-500/10 w-full">
            <Trash2 size={13}/> Supprimer
          </button>
          <button onClick={() => setMenu(false)}
            className="flex items-center gap-2 px-4 py-2.5 text-xs text-slate-600 hover:bg-white/5 w-full border-t border-white/5">
            <X size={13}/> Annuler
          </button>
        </div>
      )}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════
// COMPOSANT: Modal appel Jitsi
// ═══════════════════════════════════════════════════════════════════
const CallModal: React.FC<{ call: CallSession; onEnd: () => void }> = ({ call, onEnd }) => (
  <div className="fixed inset-0 z-[100] bg-black/95 flex flex-col">
    <div className="flex items-center justify-between p-4 bg-black border-b border-white/10">
      <div className="flex items-center gap-3">
        <div className="w-2.5 h-2.5 bg-emerald-500 rounded-full animate-pulse"/>
        <span className="text-white font-bold">
          {call.callType === 'video' ? '📹 Appel vidéo' : '📞 Appel vocal'} — {call.initiator}
        </span>
      </div>
      <button onClick={onEnd} className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl font-bold text-sm transition-all">
        Raccrocher
      </button>
    </div>
    <iframe
      src={`https://${JITSI}/${call.roomId}#config.startWithVideoMuted=${call.callType === 'voice'}&config.startWithAudioMuted=false`}
      allow="camera; microphone; fullscreen; display-capture"
      className="flex-1 w-full border-0"
    />
  </div>
);

// ═══════════════════════════════════════════════════════════════════
// COMPOSANT PRINCIPAL
// ═══════════════════════════════════════════════════════════════════
export const LeaChat = () => {
  const currentUser = localStorage.getItem('lea_currentUser') || '';

  // --- MODAL DE CONFIRMATION + TOAST (remplace les alert()/window.confirm() natifs moches) ---
  const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();

  // ── Core state ─────────────────────────────────────────────────────────────
  const [chats,    setChats]    = useState<ChatGroup[]>([]);
  const [messages, setMessages] = useState<Record<string, ChatMessage[]>>({});
  const [selId,    setSelId]    = useState('global-1');
  const [tab,      setTab]      = useState<'all' | 'private' | 'groups'>('all');
  const [query,    setQuery]    = useState('');
  const [input,    setInput]    = useState('');
  const [editMsg,  setEditMsg]  = useState<ChatMessage | null>(null);
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [hasMore,  setHasMore]  = useState(false);
  const [msgOffset, setMsgOffset] = useState(0);

  // ── Presence & typing ──────────────────────────────────────────────────────
  const [presence,    setPresence]    = useState<Record<string, UserPresence>>({});
  const [onlineList,  setOnlineList]  = useState<UserPresence[]>([]);
  const [typing,      setTyping]      = useState<Record<string, string[]>>({});
  const lastTypeSent  = useRef(0);
  const typingClears  = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

  // ── Mobile layout ──────────────────────────────────────────────────────────
  const [mobileView, setMobileView] = useState<'sidebar' | 'chat' | 'users'>('sidebar');

  // ── Modals ─────────────────────────────────────────────────────────────────
  const [groupModal,   setGroupModal]   = useState(false);
  const [groupName,    setGroupName]    = useState('');
  const [inviteModal,  setInviteModal]  = useState(false);
  const [invitee,      setInvitee]      = useState('');
  const [activeCall,   setActiveCall]   = useState<CallSession | null>(null);
  const [incomingCall, setIncomingCall] = useState<CallSession | null>(null);
  const [onlineSearch, setOnlineSearch] = useState('');

  // ── Upload / audio ─────────────────────────────────────────────────────────
  const [uploadPct, setUploadPct] = useState<number | null>(null);
  const fileRef   = useRef<HTMLInputElement>(null);
  const imgRef    = useRef<HTMLInputElement>(null);
  const inputRef  = useRef<HTMLTextAreaElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const selIdRef  = useRef(selId);
  selIdRef.current = selId;

  const audio = useAudioRec();

  // ── WebSocket handler ──────────────────────────────────────────────────────
  const handleMsg = useCallback((data: any) => {
    const cid = selIdRef.current;
    switch (data.type) {
      case 'PONG': case 'PING': break;
      case 'CHAT_MESSAGE': case 'CHAT_AUDIO': case 'CHAT_IMAGE': case 'CHAT_FILE':
        if (!data.message || !data.chatId) break;
        setMessages(prev => ({ ...prev, [data.chatId]: [...(prev[data.chatId] || []), data.message] }));
        if (data.chatId !== cid) setChats(p => p.map(c => c.id === data.chatId ? { ...c, unreadCount: c.unreadCount + 1 } : c));
        if (data.message.sender !== currentUser && 'Notification' in window && Notification.permission === 'granted')
          new Notification(`${data.message.sender} · Léa Chat`, { body: data.message.text || '📎 Fichier', icon: '/icon.png' });
        break;
      case 'USER_TYPING':
        if (data.username === currentUser) break;
        setTyping(p => {
          const prev = p[data.chatId] || [];
          const next = prev.includes(data.username) ? prev : [...prev, data.username];
          clearTimeout(typingClears.current[data.username]);
          typingClears.current[data.username] = setTimeout(() =>
            setTyping(pp => ({ ...pp, [data.chatId]: (pp[data.chatId] || []).filter(u => u !== data.username) })), 5000);
          return { ...p, [data.chatId]: next };
        });
        break;
      case 'USER_READ':
        if (data.chatId) setMessages(p => ({ ...p, [data.chatId]: (p[data.chatId] || []).map(m => m.sender === currentUser ? { ...m, isRead: true } : m) }));
        break;
      case 'USER_REACTION':
        setMessages(p => ({ ...p, [data.chatId]: (p[data.chatId] || []).map(m => {
          if (m.id !== data.messageId) return m;
          const r = { ...m.reactions }; if (!r[data.emoji]) r[data.emoji] = [];
          const i = r[data.emoji].indexOf(data.username);
          if (i >= 0) r[data.emoji] = r[data.emoji].filter((_, j) => j !== i); else r[data.emoji] = [...r[data.emoji], data.username];
          return { ...m, reactions: r };
        }) }));
        break;
      case 'USER_ONLINE':
        setPresence(p => ({ ...p, [data.username]: { username: data.username, status: data.status, lastSeen: data.ts } }));
        break;
      case 'CALL_INVITE':
        setIncomingCall({ chatId: data.chatId, callType: data.callType, roomId: data.roomId, initiator: data.initiator, status: 'ringing' });
        break;
      case 'MSG_EDITED':
        setMessages(p => ({ ...p, [data.chatId]: (p[data.chatId] || []).map(m => m.id === data.messageId ? { ...m, text: data.newText, isEdited: true } : m) }));
        break;
      case 'MSG_DELETED':
        setMessages(p => ({ ...p, [data.chatId]: (p[data.chatId] || []).map(m => m.id === data.messageId ? { ...m, isDeleted: true } : m) }));
        break;
      case 'GROUP_CREATED':
        if (!data.conversation) break;
        setChats(p => p.find(c => c.id === data.conversation.id) ? p : [{ ...data.conversation, unreadCount: 0 }, ...p]);
        // Notification si c'est un DM entrant
        if (data.conversation.type === 'private' && 'Notification' in window && Notification.permission === 'granted') {
          const other = (data.conversation.members || []).find((m: string) => m !== currentUser);
          if (other) new Notification('Léa Chat', { body: `${other} a démarré une conversation privée avec vous`, icon: '/icon.png' });
        }
        break;
      case 'GROUP_MEMBER_ADDED':
        if (!data.conversationId) break;
        if (data.conversation) {
          // Mettre à jour ou ajouter la conv complète
          setChats(p => {
            const exists = p.find(c => c.id === data.conversationId);
            if (exists) return p.map(c => c.id === data.conversationId ? { ...c, ...data.conversation, unreadCount: c.unreadCount } : c);
            return [{ ...data.conversation, unreadCount: 0 }, ...p];
          });
        } else {
          setChats(p => p.map(c => c.id === data.conversationId ? { ...c, members: [...new Set([...c.members, data.userId])] } : c));
        }
        break;
      case 'CONVERSATION_DELETED':
        if (!data.conversationId) break;
        setChats(p => p.filter(c => c.id !== data.conversationId));
        if (selIdRef.current === data.conversationId) setSelId('global-1');
        break;
    }
  }, [currentUser]);

  const { conn, send } = useLeaWS(handleMsg);

  // ── Chargement initial depuis le serveur ───────────────────────────────────
  useEffect(() => {
    if (!currentUser) return;
    api.fetchConversations(currentUser).then(convs => {
      if (convs.length > 0) setChats(convs);
    });
    api.fetchOnlineUsers().then(users => setOnlineList(users));

    if ('Notification' in window && Notification.permission === 'default') Notification.requestPermission();
    send({ type: 'USER_ONLINE', username: currentUser, status: 'online', ts: Date.now() });
    const t1 = setInterval(() => send({ type: 'USER_ONLINE', username: currentUser, status: 'online', ts: Date.now() }), 60000);
    const t2 = setInterval(() => api.fetchOnlineUsers().then(users => setOnlineList(users)), 30000);
    return () => { clearInterval(t1); clearInterval(t2); };
  }, [currentUser]);

  // ── Chargement messages depuis serveur au changement de conv ──────────────
  useEffect(() => {
    if (!selId) return;
    setChats(p => p.map(c => c.id === selId ? { ...c, unreadCount: 0 } : c));
    send({ type: 'USER_READ', chatId: selId, username: currentUser });
    setMsgOffset(0);

    setLoadingMsgs(true);
    api.fetchMessages(selId, PAGE, 0).then(({ messages: msgs, hasMore: more }) => {
      if (msgs.length > 0) setMessages(prev => ({ ...prev, [selId]: msgs }));
      setHasMore(more);
      setLoadingMsgs(false);
    });
  }, [selId]);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages[selId]?.length, selId]);

  // ── Charger plus de messages ───────────────────────────────────────────────
  const loadMore = async () => {
    const newOffset = msgOffset + PAGE;
    const { messages: older, hasMore: more } = await api.fetchMessages(selId, PAGE, newOffset);
    if (older.length > 0) {
      setMessages(prev => ({ ...prev, [selId]: [...older, ...(prev[selId] || [])] }));
    }
    setMsgOffset(newOffset);
    setHasMore(more);
  };

  // ── Handlers envoi ─────────────────────────────────────────────────────────
  const addMsg = (msg: ChatMessage) =>
    setMessages(p => ({ ...p, [selId]: [...(p[selId] || []), msg] }));

  const sendText = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() && !editMsg) return;
    if (editMsg) {
      setMessages(p => ({ ...p, [selId]: (p[selId] || []).map(m => m.id === editMsg.id ? { ...m, text: input, isEdited: true } : m) }));
      send({ type: 'MSG_EDIT', chatId: selId, messageId: editMsg.id, newText: input });
      setEditMsg(null);
    } else {
      const msg: ChatMessage = { id: uid(), chatId: selId, sender: currentUser, text: input.trim(), timestamp: hhmm(), type: 'text', isRead: false, reactions: {} };
      addMsg(msg);
      send({ type: 'CHAT_MESSAGE', chatId: selId, message: msg });
    }
    setInput(''); inputRef.current?.focus();
  };

  const onTyping = (val: string) => {
    setInput(val);
    if (Date.now() - lastTypeSent.current > 3000) {
      send({ type: 'USER_TYPING', chatId: selId, username: currentUser });
      lastTypeSent.current = Date.now();
    }
  };

  const sendAudio = async () => {
    if (!audio.blob) return;
    setUploadPct(10);
    const data = await b64(audio.blob);
    setUploadPct(100);
    const msg: ChatMessage = { id: uid(), chatId: selId, sender: currentUser, text: '', timestamp: hhmm(), type: 'audio', fileUrl: data, duration: audio.secs, isRead: false, reactions: {} };
    addMsg(msg); send({ type: 'CHAT_AUDIO', chatId: selId, message: msg });
    audio.clear(); setUploadPct(null);
  };

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>, isImg: boolean) => {
    const f = e.target.files?.[0]; if (!f) return;
    if (f.size > 10 * 1024 * 1024) { showToast('Max 10 Mo'); return; }
    setUploadPct(20);
    const data = await b64(f);
    setUploadPct(100);
    const msg: ChatMessage = { id: uid(), chatId: selId, sender: currentUser, text: '', timestamp: hhmm(), type: isImg ? 'image' : 'file', fileUrl: data, fileName: f.name, isRead: false, reactions: {} };
    addMsg(msg); send({ type: isImg ? 'CHAT_IMAGE' : 'CHAT_FILE', chatId: selId, message: msg });
    e.target.value = ''; setUploadPct(null);
  };

  const handleReact = (msgId: string, emoji: string) => {
    setMessages(p => ({ ...p, [selId]: (p[selId] || []).map(m => {
      if (m.id !== msgId) return m;
      const r = { ...m.reactions }; if (!r[emoji]) r[emoji] = [];
      const i = r[emoji].indexOf(currentUser);
      if (i >= 0) r[emoji] = r[emoji].filter((_, j) => j !== i); else r[emoji] = [...r[emoji], currentUser];
      return { ...m, reactions: r };
    }) }));
    send({ type: 'USER_REACTION', chatId: selId, messageId: msgId, emoji, username: currentUser });
  };

  const handleDeleteMsg = (msgId: string) => {
    setMessages(p => ({ ...p, [selId]: (p[selId] || []).map(m => m.id === msgId ? { ...m, isDeleted: true } : m) }));
    send({ type: 'MSG_DELETE', chatId: selId, messageId: msgId });
  };

  const handleDeleteConv = (chatId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    askConfirm('Supprimer cette conversation ?', async () => {
      const ok = await api.deleteConversation(chatId);
      if (ok) {
        setChats(p => p.filter(c => c.id !== chatId));
        if (selId === chatId) setSelId('global-1');
      }
    });
  };

  const startCall = (callType: 'voice' | 'video') => {
    const roomId = `lea-${selId}-${Date.now()}`;
    const call: CallSession = { chatId: selId, callType, roomId, initiator: currentUser, status: 'active' };
    setActiveCall(call);
    send({ type: 'CALL_INVITE', chatId: selId, callType, roomId, initiator: currentUser });
  };

  const createGroup = async () => {
    if (!groupName.trim()) return;
    const conv = await api.createConversation([currentUser], 'group', groupName.trim());
    if (conv) {
      setChats(p => [conv, ...p]);
      setSelId(conv.id);
      setMobileView('chat');
    }
    setGroupModal(false); setGroupName('');
  };

  const doInvite = async () => {
    if (!invitee.trim()) return;
    const newMember = invitee.trim().toLowerCase();
    await api.addMember(selId, newMember);
    setChats(p => p.map(c => {
      if (c.id !== selId) return c;
      const newMembers = [...new Set([...c.members, newMember])];
      // Transformer privé → groupe si 3+ membres
      const newType = (c.type === 'private' && newMembers.length >= 3) ? 'group' : c.type;
      const newName = newType === 'group' && c.type === 'private' ? newMembers.join(', ') : c.name;
      return { ...c, members: newMembers, type: newType, name: newName };
    }));
    const sys: ChatMessage = { id: uid(), chatId: selId, sender: 'Système', text: `📨 ${currentUser} a invité ${invitee}`, timestamp: hhmm(), type: 'text', isRead: true, reactions: {} };
    addMsg(sys); send({ type: 'GROUP_INVITE', chatId: selId, invitee, inviter: currentUser });
    setInvitee(''); setInviteModal(false);
  };

  // ── Ouvrir/créer un DM depuis la liste des users en ligne ─────────────────
  const openDM = async (targetUser: string) => {
    if (targetUser === currentUser) return;
    // Chercher conv DM existante dans la liste
    const sorted = [currentUser, targetUser].map(u => u.toLowerCase()).sort();
    const existingId = `dm-${sorted.join('-')}`;
    const existing = chats.find(c => c.id === existingId);
    if (existing) {
      setSelId(existing.id);
      setMobileView('chat');
      return;
    }
    // Créer via serveur
    const conv = await api.createConversation([currentUser, targetUser], 'private');
    if (conv) {
      setChats(p => [conv, ...p.filter(c => c.id !== conv.id)]);
      setSelId(conv.id);
      setMobileView('chat');
    }
  };

  // ── Computed ───────────────────────────────────────────────────────────────
  const activeChat  = chats.find(c => c.id === selId);
  const allMsgs     = messages[selId] || [];
  const typingNow   = (typing[selId] || []).filter(u => u !== currentUser);
  const totalUnread = chats.reduce((s, c) => s + c.unreadCount, 0);
  const onlineCount = Object.values(presence).filter(p => p.status === 'online').length +
    onlineList.filter(u => !presence[u.username] && u.username !== currentUser).length;

  const connBadge = conn === 'local' ? { label: '🟢 LOCAL',  cls: 'bg-emerald-500/20 text-emerald-400' }
                  : conn === 'cloud' ? { label: '🔵 CLOUD',  cls: 'bg-blue-500/20 text-blue-400' }
                  :                    { label: '🔴 HORS LIGNE', cls: 'bg-red-500/20 text-red-400' };

  const filteredChats = chats
    .filter(c => tab === 'all' || (tab === 'private' && c.type === 'private') || (tab === 'groups' && c.type === 'group'))
    .filter(c => c.name.toLowerCase().includes(query.toLowerCase()));

  const allOnlineUsers = [
    ...Object.values(presence).filter(p => p.status === 'online' && p.username !== currentUser),
    ...onlineList.filter(u => !presence[u.username] && u.username !== currentUser && u.status === 'online')
  ];
  const filteredOnlineUsers = allOnlineUsers.filter(u =>
    u.username.toLowerCase().includes(onlineSearch.toLowerCase())
  );

  // ── Sidebar content (JSX variable, pas composant — évite remount + perte de focus) ──
  const sidebarContent = (
    <aside className="w-full h-full bg-[#000b1e]/90 backdrop-blur-xl border border-white/10 rounded-[2rem] flex flex-col shadow-2xl overflow-hidden relative">
      {/* Header */}
      <div className="p-5 border-b border-white/5">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-black text-white uppercase tracking-tighter flex items-center gap-2">
            <MessageSquare size={18} className="text-[#00f2ff]"/> Chat
            {totalUnread > 0 && (
              <span className="bg-red-500 text-white text-[9px] font-black rounded-full w-5 h-5 flex items-center justify-center">{totalUnread > 9 ? '9+' : totalUnread}</span>
            )}
          </h2>
          <div className="flex items-center gap-2">
            <span className={`text-[8px] font-black px-2 py-0.5 rounded-full ${connBadge.cls}`}>{connBadge.label}</span>
            <button onClick={() => setGroupModal(true)} className="p-1.5 bg-[#00f2ff]/10 text-[#00f2ff] hover:bg-[#00f2ff]/20 rounded-xl transition-all"><Plus size={16}/></button>
          </div>
        </div>
        <div className="relative">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-600"/>
          <input type="text" placeholder="Rechercher..." value={query} onChange={e => setQuery(e.target.value)}
            className="w-full bg-black/40 border border-white/5 rounded-xl py-2.5 pl-9 pr-3 text-xs text-white focus:outline-none focus:border-[#00f2ff]/30 transition-all"/>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex p-1.5 bg-black/20 gap-1">
        {([{ id: 'all', label: 'Tout' }, { id: 'private', label: 'Privé' }, { id: 'groups', label: 'Groupes' }] as const).map(t => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={`flex-1 py-1.5 rounded-xl text-[9px] font-black uppercase tracking-widest transition-all ${tab === t.id ? 'bg-white/10 text-white' : 'text-slate-500 hover:text-slate-300'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {/* Chat list */}
      <div className="flex-1 overflow-y-auto p-2 space-y-0.5">
        {filteredChats.map(chat => {
          const msgs = messages[chat.id] || [];
          const last = msgs.length ? msgs[msgs.length - 1] : undefined;
          const peer = chat.members?.find(m => m !== currentUser) || '';
          const pres = presence[peer];
          return (
            <button key={chat.id} onClick={() => { setSelId(chat.id); setMobileView('chat'); }}
              className={`w-full p-3 flex items-center gap-3 rounded-2xl transition-all text-left group ${selId === chat.id ? 'bg-[#00f2ff]/10 border border-[#00f2ff]/30' : 'hover:bg-white/5 border border-transparent'}`}>
              <div className={`relative w-10 h-10 rounded-xl flex items-center justify-center border shrink-0 ${chat.type === 'global' ? 'bg-[#0047ff]/20 text-[#0047ff] border-[#0047ff]/40' : chat.type === 'group' ? 'bg-indigo-500/20 text-indigo-400 border-indigo-500/40' : 'bg-black/60 text-slate-300 border-white/10'}`}>
                {chat.type === 'global' ? <Globe size={16}/> : chat.type === 'group' ? <Users size={16}/> : <Lock size={16}/>}
                {chat.type === 'private' && pres && (
                  <div className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-[#000b1e] ${pres.status === 'online' ? 'bg-emerald-500' : pres.status === 'away' ? 'bg-amber-500' : 'bg-slate-600'}`}/>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex justify-between items-center mb-0.5">
                  <span className="text-xs font-bold text-white truncate">{chat.name}</span>
                  {chat.unreadCount > 0 && (
                    <span className="bg-[#00f2ff] text-black text-[8px] font-black rounded-full w-4 h-4 flex items-center justify-center shrink-0 ml-1">{chat.unreadCount > 9 ? '9+' : chat.unreadCount}</span>
                  )}
                </div>
                <p className="text-[10px] text-slate-500 truncate">
                  {last?.isDeleted ? 'Message supprimé' : last?.type === 'audio' ? '🎤 Vocal' : last?.type === 'image' ? '🖼️ Image' : last?.text || 'Aucun message'}
                </p>
              </div>
              {chat.type !== 'global' && (
                <button onClick={e => handleDeleteConv(chat.id, e)}
                  className="opacity-0 group-hover:opacity-100 p-1 text-red-400/60 hover:text-red-400 transition-all rounded-lg hover:bg-red-500/10 shrink-0">
                  <Trash2 size={12}/>
                </button>
              )}
            </button>
          );
        })}
      </div>

      {/* New Group modal */}
      {groupModal && (
        <div className="absolute inset-0 bg-[#000b1e]/95 backdrop-blur-md z-50 flex flex-col p-6 animate-in fade-in">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-base font-black text-white uppercase tracking-widest">Nouveau Groupe</h3>
            <button onClick={() => setGroupModal(false)} className="text-slate-500 hover:text-red-400"><X size={18}/></button>
          </div>
          <label className="text-[9px] font-black uppercase tracking-widest text-[#00f2ff] mb-2 block">Nom du groupe</label>
          <input type="text" value={groupName} onChange={e => setGroupName(e.target.value)} onKeyDown={e => e.key === 'Enter' && createGroup()}
            placeholder="Ex: Projet V4..." className="w-full bg-black/40 border border-white/10 rounded-xl p-3 text-sm text-white outline-none focus:border-[#00f2ff]/50 mb-4 transition-colors"/>
          <button onClick={createGroup} disabled={!groupName.trim()} className="w-full py-3 bg-[#00f2ff] text-black rounded-xl font-black uppercase tracking-widest text-sm transition-all disabled:opacity-50">
            Créer le groupe
          </button>
        </div>
      )}

      {/* Invite modal */}
      {inviteModal && (
        <div className="absolute inset-0 bg-[#000b1e]/95 backdrop-blur-md z-50 flex flex-col p-6 animate-in fade-in">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-base font-black text-white uppercase">Ajouter un membre</h3>
            <button onClick={() => setInviteModal(false)} className="text-slate-500 hover:text-red-400"><X size={18}/></button>
          </div>
          <label className="text-[9px] font-black uppercase tracking-widest text-[#00f2ff] mb-2 block">Pseudo</label>
          <input type="text" value={invitee} onChange={e => setInvitee(e.target.value)} onKeyDown={e => e.key === 'Enter' && doInvite()}
            placeholder="@pseudo..." className="w-full bg-black/40 border border-white/10 rounded-xl p-3 text-sm text-white outline-none focus:border-[#00f2ff]/50 mb-4"/>
          <button onClick={doInvite} disabled={!invitee.trim()} className="w-full py-3 bg-indigo-500 hover:bg-indigo-400 text-white rounded-xl font-black text-sm transition-all disabled:opacity-50">
            Ajouter
          </button>
        </div>
      )}
    </aside>
  );

  // ── Online users panel ─────────────────────────────────────────────────────
  const onlinePanel = (
    <aside className="w-full h-full bg-[#000b1e]/90 backdrop-blur-xl border border-white/10 rounded-[2rem] flex flex-col shadow-2xl overflow-hidden">
      <div className="p-4 border-b border-white/5">
        <h3 className="text-sm font-black text-white uppercase tracking-widest flex items-center gap-2 mb-3">
          <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"/>
          En ligne ({filteredOnlineUsers.length})
        </h3>
        <div className="relative">
          <Search size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-600"/>
          <input type="text" placeholder="Filtrer..." value={onlineSearch} onChange={e => setOnlineSearch(e.target.value)}
            className="w-full bg-black/40 border border-white/5 rounded-xl py-2 pl-9 pr-3 text-xs text-white focus:outline-none focus:border-[#00f2ff]/30 transition-all"/>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto p-3 space-y-1">
        {filteredOnlineUsers.length === 0 ? (
          <div className="text-center py-8 text-slate-600">
            <UserCircle2 size={28} className="mx-auto mb-2 opacity-30"/>
            <p className="text-[10px] uppercase tracking-widest">Aucun utilisateur en ligne</p>
          </div>
        ) : filteredOnlineUsers.map(u => (
          <button key={u.username} onClick={() => { openDM(u.username); }}
            className="w-full flex items-center gap-3 p-3 rounded-2xl hover:bg-[#00f2ff]/10 hover:border-[#00f2ff]/20 border border-transparent transition-all text-left group">
            <div className="relative w-9 h-9 rounded-xl bg-gradient-to-br from-[#0047ff]/30 to-[#00f2ff]/20 border border-white/10 flex items-center justify-center shrink-0">
              <span className="text-xs font-black text-white">{u.username.slice(0, 2).toUpperCase()}</span>
              <div className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-[#000b1e]"/>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-bold text-white truncate">{u.username}</p>
              <p className="text-[9px] text-emerald-400">En ligne</p>
            </div>
            <MessageSquare size={13} className="text-slate-600 group-hover:text-[#00f2ff] transition-colors shrink-0"/>
          </button>
        ))}
      </div>
    </aside>
  );

  // ── Chat area ──────────────────────────────────────────────────────────────
  const chatArea = (
    <main className="w-full h-full bg-[#00050d]/80 backdrop-blur-xl border border-white/10 rounded-[2rem] flex flex-col shadow-2xl relative overflow-hidden min-w-0">
      {activeChat ? (
        <>
          {/* Header */}
          <header className="p-4 border-b border-white/5 flex justify-between items-center shrink-0">
            <div className="flex items-center gap-3">
              {/* Bouton retour mobile */}
              <button onClick={() => setMobileView('sidebar')} className="lg:hidden p-2 text-slate-500 hover:text-white rounded-xl hover:bg-white/5 transition-all">
                <ArrowLeft size={16}/>
              </button>
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center border shrink-0 ${activeChat.type === 'global' ? 'bg-[#0047ff]/20 text-[#0047ff] border-[#0047ff]/40' : activeChat.type === 'group' ? 'bg-indigo-500/20 text-indigo-400 border-indigo-500/40' : 'bg-black/60 text-slate-300 border-white/10'}`}>
                {activeChat.type === 'global' ? <Globe size={17}/> : activeChat.type === 'group' ? <Users size={17}/> : <Lock size={17}/>}
              </div>
              <div>
                <h2 className="text-sm font-black text-white">{activeChat.name}</h2>
                <p className="text-[9px] text-[#00f2ff] uppercase tracking-widest font-bold flex items-center gap-1">
                  {typingNow.length > 0
                    ? <><span className="animate-pulse">●</span> {typingNow[0]} écrit...</>
                    : activeChat.type === 'private' ? 'E2E · Léa Protect'
                    : activeChat.type === 'global' ? `Canal public · ${onlineCount} en ligne`
                    : `${activeChat.members?.length || 0} membres`
                  }
                </p>
              </div>
            </div>

            <div className="flex items-center gap-1.5">
              {activeChat.type !== 'global' && (
                <>
                  <button onClick={() => startCall('voice')} className="p-2.5 bg-white/5 text-slate-400 hover:text-emerald-400 hover:bg-emerald-500/10 rounded-xl transition-all"><PhoneCall size={15}/></button>
                  <button onClick={() => startCall('video')} className="p-2.5 bg-white/5 text-slate-400 hover:text-[#00f2ff] hover:bg-[#00f2ff]/10 rounded-xl transition-all"><Video size={15}/></button>
                </>
              )}
              {(activeChat.type === 'group' || activeChat.type === 'global') && (
                <button onClick={() => setInviteModal(true)} className="p-2.5 bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 rounded-xl hover:bg-indigo-500/20 transition-all"><UserPlus size={15}/></button>
              )}
              {activeChat.type === 'group' && activeChat.admin === currentUser && (
                <div className="relative group/admin">
                  <button className="p-2.5 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded-xl hover:bg-amber-500/20 transition-all"><Crown size={15}/></button>
                  <div className="absolute right-0 top-full mt-1 w-52 bg-[#000b1e] border border-amber-500/20 rounded-2xl p-3 shadow-2xl opacity-0 invisible group-hover/admin:opacity-100 group-hover/admin:visible transition-all z-50">
                    <p className="text-[8px] font-black uppercase text-amber-400 mb-2 tracking-widest">Membres ({activeChat.members?.length || 0})</p>
                    {(activeChat.members || []).map(m => (
                      <div key={m} className="flex justify-between items-center py-1.5 border-b border-white/5 last:border-0">
                        <span className={`text-xs ${m === currentUser ? 'text-amber-300 font-bold' : 'text-slate-300'}`}>{m} {m === currentUser && '👑'}</span>
                        {m !== currentUser && (
                          <button onClick={async () => { await api.addMember(selId, m); setChats(p => p.map(c => c.id === activeChat.id ? { ...c, members: c.members.filter(x => x !== m) } : c)); }}
                            className="text-red-400 hover:text-red-300 transition-colors"><X size={12}/></button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </header>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-5 space-y-4">
            <div className="flex justify-center">
              <div className="bg-emerald-500/10 border border-emerald-500/20 px-3 py-1 rounded-full flex items-center gap-1.5">
                <Lock size={9} className="text-emerald-500"/>
                <span className="text-[8px] font-black uppercase tracking-widest text-emerald-500">Léa Protect Actif · Chiffré</span>
              </div>
            </div>

            {hasMore && (
              <div className="flex justify-center">
                <button onClick={loadMore} disabled={loadingMsgs}
                  className="flex items-center gap-1.5 text-[10px] text-slate-500 hover:text-[#00f2ff] py-1.5 px-3 rounded-xl border border-white/5 hover:border-[#00f2ff]/20 transition-all disabled:opacity-50">
                  <ChevronUp size={12}/> Charger plus
                </button>
              </div>
            )}

            {loadingMsgs && allMsgs.length === 0 && (
              <div className="flex justify-center py-8">
                <div className="w-6 h-6 border-2 border-[#00f2ff]/30 border-t-[#00f2ff] rounded-full animate-spin"/>
              </div>
            )}

            {allMsgs.map((msg, i) => (
              <Bubble key={msg.id || i} msg={msg} mine={msg.sender === currentUser}
                currentUser={currentUser} onReact={handleReact}
                onEdit={m => { setEditMsg(m); setInput(m.text); inputRef.current?.focus(); }}
                onDelete={handleDeleteMsg}/>
            ))}

            {typingNow.length > 0 && (
              <div className="flex items-center gap-2 px-2">
                <div className="flex gap-0.5">{[0, 150, 300].map(d => (
                  <div key={d} className="w-1.5 h-1.5 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: `${d}ms` }}/>
                ))}</div>
                <span className="text-[10px] text-slate-500">{typingNow[0]} écrit...</span>
              </div>
            )}
            <div ref={bottomRef}/>
          </div>

          {/* Upload progress */}
          {uploadPct !== null && (
            <div className="mx-5 mb-2 bg-[#00f2ff]/10 border border-[#00f2ff]/20 rounded-xl px-3 py-2">
              <div className="h-1 bg-white/10 rounded-full"><div className="h-full bg-[#00f2ff] rounded-full transition-all" style={{ width: `${uploadPct}%` }}/></div>
              <span className="text-[9px] text-[#00f2ff] mt-1 block">Envoi {uploadPct}%</span>
            </div>
          )}

          {/* Edit banner */}
          {editMsg && (
            <div className="mx-5 mb-2 bg-amber-500/10 border border-amber-500/20 rounded-xl px-3 py-2 flex justify-between items-center">
              <span className="text-[10px] text-amber-400 font-bold flex items-center gap-1.5"><Edit3 size={11}/> Modification : "{editMsg.text.slice(0, 35)}..."</span>
              <button onClick={() => { setEditMsg(null); setInput(''); }} className="text-slate-500 hover:text-red-400"><X size={13}/></button>
            </div>
          )}

          {/* Audio recording */}
          {audio.rec && (
            <div className="mx-5 mb-2 bg-red-500/10 border border-red-500/30 rounded-2xl px-4 py-3 flex items-center gap-3">
              <div className="w-2.5 h-2.5 bg-red-500 rounded-full animate-pulse"/>
              <span className="text-red-400 text-sm font-bold flex-1">{fmtS(audio.secs)} · Enregistrement...</span>
              <button onClick={audio.stop}   className="px-3 py-1 bg-[#00f2ff] text-black rounded-lg text-xs font-bold">Stop</button>
              <button onClick={audio.cancel} className="px-3 py-1 bg-red-500/20 text-red-400 rounded-lg text-xs font-bold">Annuler</button>
            </div>
          )}

          {/* Audio preview */}
          {audio.blob && !audio.rec && (
            <div className="mx-5 mb-2 bg-indigo-500/10 border border-indigo-500/30 rounded-2xl px-4 py-3 flex items-center gap-3">
              <AudioPlayer src={URL.createObjectURL(audio.blob)} mine={false}/>
              <button onClick={sendAudio}   className="px-3 py-1 bg-[#00f2ff] text-black rounded-lg text-xs font-bold">Envoyer</button>
              <button onClick={audio.clear} className="px-3 py-1 bg-red-500/20 text-red-400 rounded-lg text-xs font-bold">Supprimer</button>
            </div>
          )}

          {/* Input */}
          <div className="p-4 bg-black/40 border-t border-white/5 shrink-0">
            <form onSubmit={sendText} className="flex items-end gap-2">
              <div className="flex-1 bg-[#00050d] border border-white/10 focus-within:border-[#00f2ff]/40 rounded-2xl flex items-end p-1.5 transition-colors">
                <button type="button" onClick={() => fileRef.current?.click()} className="p-2 text-slate-500 hover:text-[#00f2ff] transition-colors shrink-0"><Paperclip size={17}/></button>
                <button type="button" onClick={() => imgRef.current?.click()} className="p-2 text-slate-500 hover:text-indigo-400 transition-colors hidden sm:block shrink-0"><ImageIcon size={17}/></button>
                <textarea
                  ref={inputRef} value={input} onChange={e => onTyping(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); e.currentTarget.form?.requestSubmit(); } }}
                  placeholder={editMsg ? 'Modifier...' : 'Message sécurisé... (Entrée pour envoyer)'}
                  className="flex-1 bg-transparent text-white outline-none resize-none max-h-32 py-2.5 px-2 text-sm placeholder:text-slate-600"
                  rows={1}
                />
                <button type="button" onClick={audio.rec ? audio.stop : audio.start}
                  className={`p-2 transition-colors shrink-0 ${audio.rec ? 'text-red-400 animate-pulse' : 'text-slate-500 hover:text-red-400'}`}>
                  {audio.rec ? <div className="w-4 h-4 bg-red-500 rounded-sm"/> : <Mic size={17}/>}
                </button>
              </div>
              <button type="submit" disabled={!input.trim() && !editMsg}
                className={`p-3.5 rounded-2xl transition-all shrink-0 ${input.trim() || editMsg ? 'bg-[#00f2ff] text-black hover:bg-white shadow-[0_0_18px_rgba(0,242,255,0.25)]' : 'bg-white/5 text-slate-600 cursor-not-allowed'}`}>
                <Send size={17}/>
              </button>
            </form>
          </div>
        </>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center text-slate-500">
          <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center mb-4">
            <ShieldCheck size={38} className="text-[#00f2ff] opacity-40"/>
          </div>
          <h3 className="text-lg font-black text-white uppercase tracking-widest mb-2">Canal Sécurisé</h3>
          <p className="text-sm">Sélectionnez une conversation.</p>
        </div>
      )}
    </main>
  );

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <>
      <ConfirmToastHost />
      <input ref={fileRef} type="file" accept=".pdf,.txt,.docx,.mp4,.webm,.mp3,.wav,.m4a" className="hidden" onChange={e => handleFile(e, false)}/>
      <input ref={imgRef}  type="file" accept="image/*" className="hidden" onChange={e => handleFile(e, true)}/>

      {activeCall && <CallModal call={activeCall} onEnd={() => setActiveCall(null)}/>}

      {incomingCall && !activeCall && (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[90] bg-[#000b1e] border border-[#00f2ff]/40 rounded-2xl p-4 shadow-2xl flex items-center gap-4 animate-in slide-in-from-top-4">
          <div className="text-2xl">{incomingCall.callType === 'video' ? '📹' : '📞'}</div>
          <div>
            <p className="text-white font-bold text-sm">Appel de {incomingCall.initiator}</p>
            <p className="text-[#00f2ff] text-[10px] uppercase tracking-widest">{incomingCall.callType === 'video' ? 'Vidéo' : 'Vocal'}</p>
          </div>
          <button onClick={() => { setActiveCall({ ...incomingCall, status: 'active' }); setIncomingCall(null); }}
            className="px-3 py-1.5 bg-emerald-500 text-white rounded-xl text-xs font-bold">Répondre</button>
          <button onClick={() => setIncomingCall(null)}
            className="px-3 py-1.5 bg-red-500 text-white rounded-xl text-xs font-bold">Refuser</button>
        </div>
      )}

      {/* ── LAYOUT DESKTOP large (3 colonnes) ─────────────────────────────── */}
      <div className="hidden xl:flex w-full h-full pt-20 px-4 pb-8 gap-4">
        <div className="w-[300px] 2xl:w-[340px] shrink-0 h-full">{sidebarContent}</div>
        <div className="flex-1 min-w-0 h-full">{chatArea}</div>
        <div className="w-[220px] 2xl:w-[260px] shrink-0 h-full">{onlinePanel}</div>
      </div>

      {/* ── LAYOUT TABLETTE/LAPTOP (3 colonnes compactes 768–1280px) ──────── */}
      <div className="hidden md:flex xl:hidden w-full h-full pt-20 px-3 pb-8 gap-3">
        <div className="w-[240px] lg:w-[280px] shrink-0 h-full">{sidebarContent}</div>
        <div className="flex-1 min-w-0 h-full">{chatArea}</div>
        <div className="w-[180px] lg:w-[200px] shrink-0 h-full">{onlinePanel}</div>
      </div>

      {/* ── LAYOUT MOBILE (1 colonne avec nav) ────────────────────────────── */}
      <div className="flex md:hidden flex-col w-full h-full pt-16 pb-0">
        <div className="flex-1 min-h-0 px-2 pt-2">
          {mobileView === 'sidebar' && sidebarContent}
          {mobileView === 'chat'    && chatArea}
          {mobileView === 'users'   && onlinePanel}
        </div>
        {/* Bottom nav mobile */}
        <div className="flex bg-[#000b1e]/95 border-t border-white/10 safe-bottom">
          {[
            { view: 'sidebar' as const, icon: <Menu size={20}/>, label: 'Chats', badge: totalUnread },
            { view: 'chat'    as const, icon: <MessageSquare size={20}/>, label: activeChat?.name.slice(0,10) || 'Chat', badge: 0 },
            { view: 'users'   as const, icon: <Users size={20}/>, label: 'En ligne', badge: filteredOnlineUsers.length },
          ].map(({ view, icon, label, badge }) => (
            <button key={view} onClick={() => setMobileView(view)}
              className={`flex-1 flex flex-col items-center gap-1 py-3 relative transition-all ${mobileView === view ? 'text-[#00f2ff]' : 'text-slate-500'}`}>
              <div className="relative">
                {icon}
                {badge > 0 && <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[8px] font-black rounded-full w-4 h-4 flex items-center justify-center">{badge > 9 ? '9+' : badge}</span>}
              </div>
              <span className="text-[9px] font-bold uppercase tracking-wide truncate max-w-[60px]">{label}</span>
              {mobileView === view && <div className="absolute top-0 left-1/2 -translate-x-1/2 w-6 h-0.5 bg-[#00f2ff] rounded-full"/>}
            </button>
          ))}
        </div>
      </div>
    </>
  );
};
