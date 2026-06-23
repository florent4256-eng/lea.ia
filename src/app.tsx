import React, { useState, useEffect, useCallback } from 'react';
import { ChatInterface } from './components/research/ChatInterface';
import { ShieldAlert } from 'lucide-react';
import { LeaAuth } from './components/modules/LeaAuth';

export default function App() {
  const [activeModule, setActiveModule] = useState('terminal');
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const [sessionStatus, setSessionStatus] = useState('online');
  const [searchHistory, setSearchHistory] = useState<any[]>([]);
  const [currentUser, setCurrentUser] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [maintenanceAlert, setMaintenanceAlert] = useState({ active: false, time: 0 });

  // Compte à rebours maintenance
  useEffect(() => {
    let interval: any;
    if (maintenanceAlert.active && maintenanceAlert.time > 0) {
      interval = setInterval(() => {
        setMaintenanceAlert(prev => ({ ...prev, time: prev.time - 1 }));
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [maintenanceAlert]);

  // Vérification de mise à jour au démarrage (une fois par jour)
  useEffect(() => {
    if (!currentUser) return;
    let lastCheck: string | null = null;
    try { lastCheck = localStorage.getItem('lea_last_update_check'); } catch (e) {}
    const today = new Date().toISOString().slice(0, 10);
    if (lastCheck === today) return;
    try { localStorage.setItem('lea_last_update_check', today); } catch (e) {}

    fetch('/api/app/version')
      .then(res => res.json())
      .then((data) => {
        let installedVersion = '1.0';
        try { installedVersion = localStorage.getItem('lea_installed_version') || '1.0'; } catch (e) {}
        const latest = data.latestVersion;
        const isNewer = latest.split('.').reduce((newer: boolean, part: string, i: number, arr: string[]) => {
          if (newer !== null) return newer;
          const r = parseInt(part) || 0;
          const ins = parseInt((installedVersion.split('.')[i]) || '0') || 0;
          if (r > ins) return true;
          if (r < ins) return false;
          return i === arr.length - 1 ? false : null as any;
        }, null as any);
        if (isNewer) {
          const mandatoryUpdate = data.updates?.find((u: any) => u.version === latest && u.mandatory);
          if (mandatoryUpdate) setActiveModule('updates');
        }
      })
      .catch(() => {});
  }, [currentUser]);

  // WebSocket maintenance
  useEffect(() => {
    let isMounted = true;
    fetch('/api/system/status')
      .then(res => res.json())
      .then(data => {
        if (!isMounted) return;
        if (data.maintenanceActive) {
          setMaintenanceAlert({ active: true, time: data.timeRemaining });
        }
      })
      .catch(() => {});

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const ws = new WebSocket(`${protocol}//${window.location.host}`);
    ws.onmessage = (event) => {
      if (!isMounted) return;
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'MAINTENANCE_ALERT') {
          setMaintenanceAlert({ active: data.active, time: data.timeRemaining });
        }
      } catch (e) {}
    };
    return () => {
      isMounted = false;
      ws.close();
    };
  }, []);

  // Restaurer la session utilisateur + vérifier admin
  useEffect(() => {
    let savedUser: string | null = null;
    try { savedUser = localStorage.getItem('lea_currentUser'); } catch (e) {}
    if (savedUser) {
      setCurrentUser(savedUser);
      fetch(`/api/system/is-admin?u=${encodeURIComponent(savedUser)}`)
        .then(r => r.json())
        .then(d => { if (d.isAdmin) setIsAdmin(true); })
        .catch(() => {});
    }
  }, []);

  const handleSearchExecution = useCallback(async (query: string, mode: 'search' | 'research') => {
    setSessionStatus('processing');
    return new Promise((resolve) => {
      setTimeout(() => {
        const newEntry = { id: Date.now(), query, timestamp: new Date().toLocaleTimeString() };
        setSearchHistory(prev => [newEntry, ...prev]);
        setSessionStatus('online');
        resolve("Traitement local achevé.");
      }, 800);
    });
  }, []);

  const handleLogin = (pseudo: string) => {
    setCurrentUser(pseudo);
    fetch(`/api/system/is-admin?u=${encodeURIComponent(pseudo)}`)
      .then(r => r.json())
      .then(d => { if (d.isAdmin) setIsAdmin(true); })
      .catch(() => {});
  };

  return (
    <div className="h-screen w-full bg-[#000814] text-white overflow-hidden font-sans antialiased selection:bg-[#00f2ff]/30">

      {/* Bannière maintenance */}
      {maintenanceAlert.active && maintenanceAlert.time > 0 && (
        <div className="fixed top-2 left-1/2 -translate-x-1/2 w-max max-w-[85%] z-[10000] bg-red-950/40 backdrop-blur-sm border border-red-500/30 shadow-[0_0_15px_rgba(239,68,68,0.2)] px-3 py-1.5 flex items-center justify-center gap-3 rounded-full pointer-events-none animate-in slide-in-from-top duration-500">
          <div className="flex items-center gap-2 overflow-hidden">
            <ShieldAlert size={14} className="text-red-500 shrink-0 animate-pulse" />
            <span className="text-white text-[10px] md:text-xs font-black uppercase tracking-widest truncate drop-shadow-md">
              <span className="text-red-500">MAINTENANCE</span>
              <span className="hidden sm:inline text-gray-300 ml-2">: Arrêtez vos processus</span>
            </span>
          </div>
          <div className="flex items-center shrink-0 bg-black/40 px-2.5 py-0.5 rounded-full border border-red-500/20">
            <div className="text-xs md:text-sm font-mono font-black text-white tracking-widest drop-shadow-[0_0_5px_rgba(255,255,255,0.8)]">
              {Math.floor(maintenanceAlert.time / 60).toString().padStart(2, '0')}:{(maintenanceAlert.time % 60).toString().padStart(2, '0')}
            </div>
          </div>
        </div>
      )}

      {!currentUser ? (
        <LeaAuth
          onLogin={handleLogin}
          isMaintenance={maintenanceAlert.active}
        />
      ) : (
        <ChatInterface
          activeModule={activeModule}
          setActiveModule={setActiveModule}
          isSidebarOpen={isSidebarOpen}
          setSidebarOpen={setSidebarOpen}
          onSearchStart={handleSearchExecution}
          history={searchHistory}
          status={sessionStatus}
          isAdmin={isAdmin}
        />
      )}
    </div>
  );
}
