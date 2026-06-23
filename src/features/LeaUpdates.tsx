import React, { useState, useEffect } from 'react';
import { Download, Check, RefreshCw, AlertTriangle, X } from 'lucide-react';
import { Capacitor } from '@capacitor/core';
import { registerPlugin } from '@capacitor/core';

const LeaPhone = registerPlugin<{ openUpdates: () => Promise<void> }>('LeaPhone');

interface UpdateInfo {
  version: string;
  name: string;
  description: string;
  mandatory: boolean;
  downloadUrl: string;
  fileSize: string;
  releaseDate: string;
  changelog: string[];
}

interface VersionData {
  currentVersion: string;
  latestVersion: string;
  updates: UpdateInfo[];
}

function isNewer(remote: string, installed: string): boolean {
  const r = remote.split('.').map(Number);
  const i = installed.split('.').map(Number);
  const len = Math.max(r.length, i.length);
  for (let k = 0; k < len; k++) {
    const rv = r[k] ?? 0, iv = i[k] ?? 0;
    if (rv > iv) return true;
    if (rv < iv) return false;
  }
  return false;
}

// Animations CSS injectées une seule fois
const HALO_STYLE = `
@keyframes halo-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes halo-pulse { 0%, 100% { opacity: 0.25; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.08); } }
@keyframes halo-spin-slow { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
`;

function injectHaloStyle() {
  if (typeof document === 'undefined') return;
  if (document.getElementById('lea-halo-style')) return;
  const s = document.createElement('style');
  s.id = 'lea-halo-style';
  s.textContent = HALO_STYLE;
  document.head.appendChild(s);
}

// Halos multi-couleur
const COLORS_AVAILABLE = '#00f2ff, #0088ff, #3300cc, #8800ff, #dd00ff, #ff0099, #0088ff, #00f2ff';
const COLORS_UPTODDATE = '#22c55e, #00ffaa, #00ccff, #a855f7, #ff00cc, #22c55e';
const COLORS_ERROR     = '#ff4444, #ff8800, #ff4444';
const COLORS_IDLE      = '#334155, #475569, #334155';

interface HaloProps {
  colors: string;
  spin?: boolean;
  size?: number;
  thickness?: number;
  speed?: string;
  blur?: number;
  glowOpacity?: number;
  children?: React.ReactNode;
}

const Halo: React.FC<HaloProps> = ({
  colors,
  spin = true,
  size = 140,
  thickness = 4,
  speed = '3s',
  blur = 22,
  glowOpacity = 0.35,
  children,
}) => {
  const gradient = `conic-gradient(from 0deg, ${colors})`;
  const animation = spin ? `halo-spin ${speed} linear infinite` : 'none';
  const inner = size - thickness * 2;

  return (
    <div style={{ position: 'relative', width: size, height: size, flexShrink: 0 }}>
      {/* Halo glow extérieur */}
      <div style={{
        position: 'absolute',
        inset: -blur / 2,
        borderRadius: '50%',
        background: gradient,
        filter: `blur(${blur}px)`,
        opacity: glowOpacity,
        animation: `halo-pulse 2.5s ease-in-out infinite`,
      }} />
      {/* Anneau tournant */}
      <div style={{
        position: 'absolute',
        inset: 0,
        borderRadius: '50%',
        background: gradient,
        animation,
      }} />
      {/* Cercle intérieur sombre */}
      <div style={{
        position: 'absolute',
        inset: thickness,
        borderRadius: '50%',
        background: '#020617',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        {children}
      </div>
    </div>
  );
};

export const LeaUpdates = ({ onClose }: { onClose?: () => void }) => {
  const isAndroid = Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android';

  useEffect(() => { injectHaloStyle(); }, []);

  useEffect(() => {
    if (isAndroid) {
      LeaPhone.openUpdates().catch(() => {});
      if (onClose) onClose();
    }
  }, [isAndroid]);

  const [status, setStatus] = useState<'idle' | 'checking' | 'upToDate' | 'available' | 'error'>('idle');
  const [versionData, setVersionData] = useState<VersionData | null>(null);
  const [installedVersion] = useState(() => {
    try { return localStorage.getItem('lea_installed_version') || '1.0'; } catch { return '1.0'; }
  });
  const [pendingUpdate, setPendingUpdate] = useState<UpdateInfo | null>(null);

  const checkForUpdates = async () => {
    setStatus('checking');
    try {
      const res = await fetch(`/api/app/version`);
      if (!res.ok) throw new Error('Serveur inaccessible');
      const data: VersionData = await res.json();
      setVersionData(data);
      const update = data.updates.find(u => isNewer(u.version, installedVersion));
      if (update) { setPendingUpdate(update); setStatus('available'); }
      else { setStatus('upToDate'); }
    } catch { setStatus('error'); }
  };

  useEffect(() => {
    let lastCheck: string | null = null;
    try { lastCheck = localStorage.getItem('lea_last_update_check'); } catch {}
    const today = new Date().toISOString().slice(0, 10);
    if (lastCheck !== today) {
      try { localStorage.setItem('lea_last_update_check', today); } catch {}
      checkForUpdates();
    } else {
      setStatus('idle');
    }
  }, []);

  return (
    // Overlay plein écran — couvre sidebar + safe-area haut/bas
    <div style={{
      position: 'fixed',
      inset: 0,
      zIndex: 200,
      display: 'flex',
      flexDirection: 'column',
      background: 'linear-gradient(160deg, #000814 0%, #001233 50%, #000814 100%)',
      overflowY: 'auto',
      paddingTop: 'env(safe-area-inset-top, 0px)',
      paddingBottom: 'env(safe-area-inset-bottom, 0px)',
    }}>
      {/* Grille déco fond */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none', opacity: 0.03,
        backgroundImage: 'linear-gradient(#ffffff 1px, transparent 1px), linear-gradient(90deg, #ffffff 1px, transparent 1px)',
        backgroundSize: '40px 40px',
      }} />

      {/* Bouton fermer */}
      {onClose && (
        <button
          onClick={onClose}
          style={{ position: 'absolute', top: 'max(24px, env(safe-area-inset-top, 24px))', right: 24, zIndex: 210 }}
          className="p-2.5 bg-white/5 hover:bg-white/15 backdrop-blur rounded-xl text-white/40 hover:text-white transition-all"
        >
          <X size={22} />
        </button>
      )}

      {/* Contenu centré */}
      <div className="relative z-10 flex flex-col items-center justify-start w-full max-w-md mx-auto px-6 pt-16 pb-12 gap-10">

        {/* Titre */}
        <div className="text-center">
          <h2 className="text-2xl font-black text-transparent bg-clip-text bg-gradient-to-r from-white to-slate-400 tracking-tighter mb-1">
            Mises à jour
          </h2>
          <p className="text-slate-500 text-sm">
            Version installée :{' '}
            <span className="text-[#00f2ff] font-bold">{installedVersion}</span>
          </p>
        </div>

        {/* ═══ HALO CENTRAL ═══ */}
        <div className="flex flex-col items-center gap-6">
          {status === 'idle' && (
            <Halo colors={COLORS_IDLE} spin={false} glowOpacity={0.2}>
              <Download size={32} className="text-slate-500" />
            </Halo>
          )}
          {status === 'checking' && (
            <Halo colors={COLORS_AVAILABLE} spin speed="1.5s" glowOpacity={0.4}>
              <RefreshCw size={30} className="text-[#00f2ff]" style={{ animation: 'halo-spin 1s linear infinite' }} />
            </Halo>
          )}
          {status === 'available' && (
            <Halo colors={COLORS_AVAILABLE} spin speed="3s" thickness={5} blur={28} glowOpacity={0.45}>
              <Download size={34} style={{ color: '#00f2ff' }} />
            </Halo>
          )}
          {status === 'upToDate' && (
            <Halo colors={COLORS_UPTODDATE} spin speed="4s" thickness={5} blur={28} glowOpacity={0.4}>
              <Check size={36} style={{ color: '#22c55e' }} strokeWidth={3} />
            </Halo>
          )}
          {status === 'error' && (
            <Halo colors={COLORS_ERROR} spin={false} glowOpacity={0.3}>
              <AlertTriangle size={30} className="text-red-400" />
            </Halo>
          )}

          {/* Label sous le halo */}
          <div className="text-center">
            {status === 'idle' && (
              <>
                <p className="font-bold text-white text-lg">Vérifier les mises à jour</p>
                <p className="text-slate-500 text-sm mt-1">Appuyez pour chercher une nouvelle version.</p>
              </>
            )}
            {status === 'checking' && (
              <>
                <p className="font-bold text-white text-lg">Vérification en cours…</p>
                <p className="text-slate-500 text-sm mt-1">Connexion au serveur Léa.</p>
              </>
            )}
            {status === 'upToDate' && (
              <>
                <p className="font-bold text-white text-lg">Application à jour</p>
                <p className="text-slate-500 text-sm mt-1">Vous utilisez déjà la dernière version disponible.</p>
              </>
            )}
            {status === 'error' && (
              <>
                <p className="font-bold text-white text-lg">Serveur inaccessible</p>
                <p className="text-slate-500 text-sm mt-1">Impossible de contacter le serveur. Réessayez plus tard.</p>
              </>
            )}
          </div>
        </div>

        {/* ═══ CARTE UPDATE DISPONIBLE ═══ */}
        {status === 'available' && pendingUpdate && (
          <div className="w-full space-y-4">
            <div className="p-5 rounded-2xl border border-[#00f2ff]/20 bg-[#00f2ff]/5"
              style={{ boxShadow: '0 0 32px rgba(0,242,255,0.07)' }}>
              <div className="flex items-center gap-3 mb-3 flex-wrap">
                <p className="font-bold text-white">{pendingUpdate.name}</p>
                {pendingUpdate.mandatory && (
                  <span className="text-[10px] bg-red-500/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded-full font-bold uppercase tracking-widest">
                    Obligatoire
                  </span>
                )}
              </div>
              <p className="text-slate-400 text-sm mb-3">{pendingUpdate.description}</p>
              <div className="flex gap-4 text-xs text-slate-500">
                <span>v{pendingUpdate.version}</span>
                {pendingUpdate.fileSize && <span>{pendingUpdate.fileSize}</span>}
                {pendingUpdate.releaseDate && <span>{pendingUpdate.releaseDate}</span>}
              </div>
            </div>

            {pendingUpdate.changelog?.length > 0 && (
              <div className="bg-white/5 rounded-xl p-4 border border-white/5">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3">Nouveautés</p>
                <ul className="space-y-1.5">
                  {pendingUpdate.changelog.map((item, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-slate-300">
                      <span className="text-[#00f2ff] mt-0.5 shrink-0">•</span>
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div className="flex gap-3 pt-1">
              <a
                href={pendingUpdate.downloadUrl}
                download
                className="flex-1 flex items-center justify-center gap-2 font-black py-3.5 rounded-xl text-sm transition-all"
                style={{
                  background: 'linear-gradient(90deg, #00b4d8, #0077b6, #6600cc)',
                  color: '#fff',
                  boxShadow: '0 0 24px rgba(0,180,216,0.4)',
                }}
              >
                <Download size={16} /> Télécharger et installer
              </a>
              {!pendingUpdate.mandatory && (
                <button
                  onClick={() => setPendingUpdate(null)}
                  className="px-5 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white font-bold text-sm transition-all border border-white/10"
                >
                  Plus tard
                </button>
              )}
            </div>
          </div>
        )}

        {/* Bouton vérifier */}
        {status !== 'checking' && (
          <button
            onClick={checkForUpdates}
            className="w-full flex items-center justify-center gap-2 py-3.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 hover:border-white/20 text-slate-400 hover:text-white text-sm font-medium transition-all"
          >
            <RefreshCw size={15} /> Vérifier maintenant
          </button>
        )}

        <p className="text-[10px] text-slate-600 text-center">
          Les mises à jour obligatoires sont installées automatiquement au démarrage.
        </p>
      </div>
    </div>
  );
};
