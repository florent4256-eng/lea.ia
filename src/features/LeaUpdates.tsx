import React, { useState, useEffect } from 'react';
import { Download, Check, RefreshCw, AlertTriangle, Loader2 } from 'lucide-react';
import { Capacitor } from '@capacitor/core';
import { registerPlugin } from '@capacitor/core';
import { useConfirmToast } from '../hooks/useConfirmToast';

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

async function downloadAndInstallApk(downloadUrl: string, filename: string, onProgress?: (pct: number) => void): Promise<void> {
  const serverUrl = (window as any).LEA_SERVER_URL || '';
  const fullUrl = downloadUrl.startsWith('http') ? downloadUrl : `${serverUrl}${downloadUrl}`;

  if (!Capacitor.isNativePlatform()) {
    const a = document.createElement('a');
    a.href = fullUrl; a.download = filename;
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
    return;
  }

  const { Filesystem, Directory } = await import('@capacitor/filesystem');
  const { Share } = await import('@capacitor/share');

  const resp = await fetch(fullUrl);
  if (!resp.ok) throw new Error(`Serveur: ${resp.status}`);
  const total = parseInt(resp.headers.get('content-length') || '0');
  const reader = resp.body!.getReader();
  const chunks: Uint8Array[] = [];
  let received = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    chunks.push(value);
    received += value.length;
    if (total && onProgress) onProgress(Math.round(received / total * 100));
  }
  // Cast nécessaire : TS type Uint8Array<ArrayBufferLike> depuis un ReadableStream,
  // alors que BlobPart attend Uint8Array<ArrayBuffer> — sans impact runtime réel.
  const blob = new Blob(chunks as BlobPart[], { type: 'application/vnd.android.package-archive' });

  const base64 = await new Promise<string>((res, rej) => {
    const r = new FileReader();
    r.onloadend = () => res((r.result as string).split(',')[1]);
    r.onerror = rej;
    r.readAsDataURL(blob);
  });

  const saved = await Filesystem.writeFile({ path: filename, data: base64, directory: Directory.Cache });
  await Share.share({ title: 'Installer la mise à jour Léa', url: saved.uri, dialogTitle: 'Ouvrir avec Package Installer' });
}

export const LeaUpdates = ({ onClose }: { onClose?: () => void }) => {
  const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();
  const isAndroid = Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android';

  // Sur Android : déléguer entièrement à la modale native, ne rien rendre
  useEffect(() => {
    if (isAndroid) {
      LeaPhone.openUpdates().catch(() => {});
      if (onClose) onClose();
    }
  }, [isAndroid]);

  if (isAndroid) return null;

  // ── Vue web ──────────────────────────────────────────────────────────────────
  const [status, setStatus] = useState<'idle' | 'checking' | 'upToDate' | 'available' | 'error'>('idle');
  const [versionData, setVersionData] = useState<VersionData | null>(null);
  const [pendingUpdate, setPendingUpdate] = useState<UpdateInfo | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const [dlProgress, setDlProgress] = useState(0);

  const installedVersion = (() => {
    try { return localStorage.getItem('lea_installed_version') || '0'; } catch { return '0'; }
  })();

  const checkForUpdates = async () => {
    setStatus('checking');
    try {
      const res = await fetch('/api/app/version');
      if (!res.ok) throw new Error();
      const data: VersionData = await res.json();
      setVersionData(data);
      const update = data.updates.find(u => isNewer(u.version, installedVersion));
      if (update) { setPendingUpdate(update); setStatus('available'); }
      else setStatus('upToDate');
    } catch { setStatus('error'); }
  };

  useEffect(() => {
    let lastCheck: string | null = null;
    try { lastCheck = localStorage.getItem('lea_last_update_check'); } catch {}
    const today = new Date().toISOString().slice(0, 10);
    if (lastCheck !== today) {
      try { localStorage.setItem('lea_last_update_check', today); } catch {}
      checkForUpdates();
    }
  }, []);

  return (
    <div className="flex flex-col gap-6 p-6 max-w-lg mx-auto w-full">
      <ConfirmToastHost />

      <h2 className="text-xl font-black text-white tracking-tight">Mises à jour</h2>

      {/* Statut */}
      <div className="flex items-center gap-3 p-4 rounded-2xl bg-white/5 border border-white/10">
        {status === 'checking' && <RefreshCw size={20} className="text-[#00f2ff] animate-spin shrink-0" />}
        {status === 'upToDate' && <Check size={20} className="text-green-400 shrink-0" />}
        {status === 'available' && <Download size={20} className="text-[#00f2ff] shrink-0" />}
        {status === 'error'     && <AlertTriangle size={20} className="text-red-400 shrink-0" />}
        {status === 'idle'      && <Download size={20} className="text-slate-500 shrink-0" />}
        <div>
          {status === 'idle'     && <p className="text-white font-bold text-sm">Vérifier les mises à jour</p>}
          {status === 'checking' && <p className="text-white font-bold text-sm">Vérification en cours…</p>}
          {status === 'upToDate' && <p className="text-white font-bold text-sm">Application à jour</p>}
          {status === 'available'&& <p className="text-[#00f2ff] font-bold text-sm">Mise à jour disponible</p>}
          {status === 'error'    && <p className="text-red-400 font-bold text-sm">Serveur inaccessible</p>}
        </div>
      </div>

      {/* Carte mise à jour disponible */}
      {status === 'available' && pendingUpdate && (
        <div className="space-y-3">
          <div className="p-4 rounded-2xl border border-[#00f2ff]/20 bg-[#00f2ff]/5">
            <div className="flex items-center gap-2 mb-2 flex-wrap">
              <p className="font-bold text-white text-sm">{pendingUpdate.name}</p>
              {pendingUpdate.mandatory && (
                <span className="text-[10px] bg-red-500/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded-full font-bold uppercase tracking-widest">
                  Obligatoire
                </span>
              )}
            </div>
            <p className="text-slate-400 text-sm mb-2">{pendingUpdate.description}</p>
            <div className="flex gap-3 text-xs text-slate-500">
              <span>v{pendingUpdate.version}</span>
              {pendingUpdate.fileSize && <span>{pendingUpdate.fileSize}</span>}
              {pendingUpdate.releaseDate && <span>{pendingUpdate.releaseDate}</span>}
            </div>
          </div>

          {pendingUpdate.changelog?.length > 0 && (
            <div className="bg-white/5 rounded-xl p-4 border border-white/5">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">Nouveautés</p>
              <ul className="space-y-1">
                {pendingUpdate.changelog.map((item, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm text-slate-300">
                    <span className="text-[#00f2ff] mt-0.5 shrink-0">•</span>{item}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex gap-3">
            <button
              onClick={async () => {
                if (isDownloading) return;
                setIsDownloading(true); setDlProgress(0);
                try {
                  const fname = pendingUpdate.downloadUrl.split('/').pop() || 'lea-update.apk';
                  await downloadAndInstallApk(pendingUpdate.downloadUrl, fname, setDlProgress);
                } catch (e: any) {
                  showToast(`Erreur : ${e.message}`);
                } finally { setIsDownloading(false); setDlProgress(0); }
              }}
              disabled={isDownloading}
              className="flex-1 flex items-center justify-center gap-2 font-black py-3 rounded-xl text-sm transition-all disabled:opacity-70 bg-gradient-to-r from-[#00b4d8] to-[#6600cc] text-white"
            >
              {isDownloading
                ? <><Loader2 size={16} className="animate-spin" /> {dlProgress > 0 ? `${dlProgress}%` : 'Préparation…'}</>
                : <><Download size={16} /> Télécharger et installer</>}
            </button>
            {!pendingUpdate.mandatory && !isDownloading && (
              <button
                onClick={() => setPendingUpdate(null)}
                className="px-4 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white font-bold text-sm transition-all border border-white/10"
              >
                Plus tard
              </button>
            )}
          </div>
        </div>
      )}

      {/* Lien téléchargement direct quand à jour */}
      {status === 'upToDate' && versionData && (
        <a
          href={versionData.updates?.[0]?.downloadUrl || `/apk/lea-v${versionData.latestVersion}.apk`}
          download
          className="flex items-center justify-center gap-2 py-3 rounded-xl text-sm font-bold text-[#00f2ff] border border-[#00f2ff]/30 bg-[#00f2ff]/5 hover:bg-[#00f2ff]/15 transition-all"
        >
          <Download size={15} /> Télécharger l'APK v{versionData.latestVersion}
        </a>
      )}

      {/* Bouton vérifier */}
      {status !== 'checking' && (
        <button
          onClick={checkForUpdates}
          className="flex items-center justify-center gap-2 py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-slate-400 hover:text-white text-sm font-medium transition-all"
        >
          <RefreshCw size={15} /> Vérifier maintenant
        </button>
      )}
    </div>
  );
};
