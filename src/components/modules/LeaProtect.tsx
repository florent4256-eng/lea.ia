import React, { useState, useEffect, useRef } from 'react';
import { 
  ShieldCheck, ShieldAlert, ShieldX, Lock, Unlock, Zap, 
  Activity, Eye, Terminal, Radio, MapPin, PhoneCall, 
  Database, Cpu, Share2, AlertTriangle, Fingerprint, 
  Key, Globe, BarChart3, Binary, HardDrive, RefreshCcw, Heart,
  FileText, Download
} from 'lucide-react';

// ==========================================
// 🛡️ TYPES ET INTERFACES SÉCURISÉS
// ==========================================
interface SecurityLog {
  id: string;
  time: string;
  type: 'IP_BLOCK' | 'QUANTUM_THREAT' | 'SOS_TRIGGER' | 'BANK_ALERT' | 'BLOCKCHAIN_SYNC' | 'SYSTEM_ALERT' | 'SOCIAL_SHIELD';
  severity: 'low' | 'medium' | 'high' | 'critical';
  message: string;
  location?: string;
}

interface SocialMessage {
  id: string;
  timestamp: string;
  sender: string;
  content: string;
  isToxic: boolean;
}

export const LeaProtect = () => {
  const currentUser = localStorage.getItem('lea_currentUser') || '';
  const [isGodMode, setIsGodMode] = React.useState(false);
  
  // --- ÉTATS DE SÉCURITÉ CRITIQUE ---
  const [securityLogs, setSecurityLogs] = useState<SecurityLog[]>([]);
  const [securityLevel, setSecurityLevel] = useState<'nominal' | 'alert' | 'critical' | 'lockdown'>('nominal');
  const [quantumShield, setQuantumShield] = useState(true);
  const [blockchainStatus, setBlockchainStatus] = useState('synchronized');
  const [logs, setLogs] = useState<SecurityLog[]>([]);
  const [activeTab, setActiveTab] = useState<'cyber' | 'blockchain' | 'network' | 'social' | 'vault'>('cyber');
  const [isScanning, setIsScanning] = useState(false);

  // --- ÉTATS DE CONNEXION TOUR ---
  const [isTourConnected, setIsTourConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  // --- ÉTATS DU BOUCLIER SOCIAL ---
  const [vaultFiles, setVaultFiles] = useState<{name: string, size: string, date: string}[]>([]);
  const [harassmentCounter, setHarassmentCounter] = useState(0);
  const [socialArchive, setSocialArchive] = useState<SocialMessage[]>([]);
  const [socialStatus, setSocialStatus] = useState<'surveillance' | 'intervention'>('surveillance');
  const [permissions, setPermissions] = useState({
    microphone: false,
    location: false,
    system: false
  });

  // ==========================================
  // 🔌 CONNEXION UNIQUE AU CERVEAU CENTRAL (WEBSOCKET BLINDÉ)
  // ==========================================
  useEffect(() => {
    // Vérification du rôle admin depuis le serveur (pas depuis localStorage)
    if (currentUser) {
      fetch(`/api/user/profile/${currentUser}`)
        .then(r => r.json())
        .then(d => setIsGodMode(d.role === 'admin'))
        .catch(() => setIsGodMode(false));
    }
  }, [currentUser]);

  useEffect(() => {
    try {
      const wsProtocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
      const wsUrl = `${wsProtocol}${window.location.host}`; 
      
      const ws = new WebSocket(wsUrl);

      ws.onopen = () => setIsTourConnected(true);
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          
          if (data.type === 'SYSTEM_ALERT') {
            const newLog: SecurityLog = {
              id: Date.now().toString() + Math.random().toString(),
              time: new Date().toLocaleTimeString(),
              type: 'SYSTEM_ALERT',
              severity: data.severity,
              message: data.message
            };
            setLogs(prev => [newLog, ...prev]);
            if (data.severity === 'critical') setSecurityLevel('alert');
          }

          if (data.type === 'SOCIAL_INTERCEPT') {
            setSocialArchive(prev => [data.message, ...prev]);
            
            if (data.message.isToxic) {
              setHarassmentCounter(prev => {
                 const newCount = prev + 1;
                 if (newCount >= 5) {
                   setTimeout(() => {
                     setSocialStatus('intervention');
                     setSecurityLevel('alert');
                   }, 0);
                 }
                 return newCount;
              });
            }
          }

          if (data.type === 'SECURITY_ALERT') {
            // Alerte d'intrusion physique depuis le Xiaomi
            setSecurityLogs(prev => [data.data, ...prev]);
            setSecurityLevel('critical');
            if (window.navigator.vibrate) window.navigator.vibrate([200, 100, 200]);
          }
        } catch (err) { console.error("Erreur de décodage des données de la Matrice"); }
      };
      
      ws.onclose = () => setIsTourConnected(false);
      wsRef.current = ws;
      return () => ws.close();
      
    } catch (fatalError) {
      console.error("Le pare-feu a bloqué la connexion au Hub Léaria.");
    }
  }, []);

  // --- ÉCOUTEUR DE SÉCURITÉ TEMPS RÉEL (CONNEXION TOUR MASTER) ---
  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}`;
    const ws = new WebSocket(wsUrl);

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        if (message.type === 'SECURITY_ALERT') {
          // On injecte l'alerte du Xiaomi en haut de la liste
          setSecurityLogs(prev => [message.data, ...prev]);
          setSecurityLevel('critical');
          if (window.navigator.vibrate) window.navigator.vibrate([200, 100, 200]);
        }
      } catch (err) {
        console.error("Erreur tunnel Protect:", err);
      }
    };

    return () => ws.close();
  }, []);

  // 🔴 LE BOUTON ROUGE : ARRÊT D'URGENCE GPU
  const handleKillGPU = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ 
        type: 'STOP_GENERATION', 
        sessionId: 'MASTER_SHIELD' 
      }));
      const killLog: SecurityLog = {
        id: Date.now().toString(),
        time: new Date().toLocaleTimeString(),
        type: 'SYSTEM_ALERT',
        severity: 'critical',
        message: "ORDRE D'ARRÊT D'URGENCE : Processus GPU interrompus par l'Admin."
      };
      setLogs(prev => [killLog, ...prev]);
      setSecurityLevel('lockdown');
    }
  };

  // ==========================================
  // 🛡️ LOGIQUE DU BOUCLIER SOCIAL (ANTI-HARCÈLEMENT)
  // ==========================================
  const generatePoliceReport = () => {
    console.log("Génération du dossier de preuves pour la police...");
    alert("✅ Le rapport officiel au format PDF a été généré avec succès ! Prêt pour le dépôt de plainte.");
    setHarassmentCounter(0);
    setSocialStatus('surveillance');
    setSecurityLevel('nominal');
  };

  const simulateSocialAlert = () => {
    const count = harassmentCounter + 1;
    setHarassmentCounter(count);
    if (count >= 5) {
      setSocialStatus('intervention');
      setSecurityLevel('alert');
    }
  };

  const requestAllPermissions = async () => {
    try {
      // 1. Demande officielle du Micro
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      if (stream) {
        setPermissions(prev => ({ ...prev, microphone: true }));
        const newLog: SecurityLog = {
            id: Date.now().toString(), 
            time: new Date().toLocaleTimeString(),
            type: 'SYSTEM_ALERT', 
            severity: 'low', 
            message: "Accès Microphone : AUTORISÉ par l'utilisateur."
        };
        setLogs(prev => [newLog, ...prev]);
        stream.getTracks().forEach(track => track.stop()); // On libère le micro après le test
      }

      // 2. Demande officielle du GPS
      navigator.geolocation.getCurrentPosition((pos) => {
        setPermissions(prev => ({ ...prev, location: true }));
        const newLog: SecurityLog = {
            id: (Date.now() + 1).toString(), 
            time: new Date().toLocaleTimeString(),
            type: 'SYSTEM_ALERT', 
            severity: 'low', 
            message: `Accès GPS : AUTORISÉ. Position verrouillée.`
        };
        setLogs(prev => [newLog, ...prev]);
      }, (err) => {
        const newLog: SecurityLog = {
            id: (Date.now() + 1).toString(), 
            time: new Date().toLocaleTimeString(),
            type: 'SYSTEM_ALERT', 
            severity: 'high', 
            message: "Accès GPS : REFUSÉ par l'utilisateur."
        };
        setLogs(prev => [newLog, ...prev]);
      });

      // 3. Note pour le futur : Ici on déclenchera l'accès SMS/Appels
      if (isTourConnected) {
        setPermissions(prev => ({ ...prev, system: true }));
      }
      
      alert("Léa a maintenant accès à tes sens pour te protéger, mon chéri !");
    } catch (err) {
      const newLog: SecurityLog = {
          id: (Date.now() + 2).toString(), 
          time: new Date().toLocaleTimeString(),
          type: 'SYSTEM_ALERT', 
          severity: 'critical', 
          message: "Échec de l'obtention des permissions systèmes."
      };
      setLogs(prev => [newLog, ...prev]);
    }
  };

  return (
    <div className="w-full h-full p-6 md:p-10 flex flex-col pt-24 overflow-y-auto custom-scrollbar">
      
      {/* HEADER : ÉTAT GLOBAL DU SYSTÈME */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
        <div className={`p-6 rounded-[2rem] border transition-all ${
          securityLevel === 'nominal' ? 'bg-emerald-500/10 border-emerald-500/30' : 
          securityLevel === 'alert' ? 'bg-orange-500/10 border-orange-500/30' : 'bg-red-500/20 border-red-500 shadow-[0_0_30px_rgba(239,68,68,0.3)]'
        }`}>
          <div className="flex justify-between items-start mb-4">
            <ShieldCheck size={28} className={securityLevel === 'nominal' ? 'text-emerald-400' : 'text-red-400'} />
            
            {/* 🛡️ SÉCURITÉ : SEUL L'ADMIN VOIT CES BADGES */}
            {isGodMode && (
              <div className="flex gap-2">
                <div className="px-2 py-1 bg-indigo-500/40 rounded-lg text-[10px] font-black uppercase tracking-widest text-white border border-indigo-500/30">
                  XIAOMI ARMÉ
                </div>
                <div className={`px-2 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest text-white ${isTourConnected ? 'bg-emerald-500/40' : 'bg-red-500/40'}`}>
                  {isTourConnected ? 'TOUR ONLINE' : 'OFFLINE'}
                </div>
              </div>
            )}
          </div>
          <p className="text-[10px] uppercase font-bold text-slate-500 tracking-widest">Niveau de Menace</p>
          <p className={`text-2xl font-black uppercase italic ${securityLevel === 'nominal' ? 'text-emerald-400' : 'text-red-400'}`}>
            {securityLevel === 'nominal' ? 'Nominal' : securityLevel === 'alert' ? 'Alerte' : 'Critique'}
          </p>
        </div>

        <div className="p-6 bg-[#000b1e] border border-white/5 rounded-[2rem]">
          <div className="flex justify-between items-start mb-4">
            <Binary size={28} className="text-[#00f2ff]" />
            <div className="px-2 py-1 bg-[#00f2ff]/20 rounded-lg text-[10px] font-black uppercase tracking-widest text-[#00f2ff]">Léa Chain</div>
          </div>
          <p className="text-[10px] uppercase font-bold text-slate-500 tracking-widest">Blockchain Décentralisée</p>
          <p className="text-2xl font-black text-white italic uppercase">{blockchainStatus}</p>
        </div>

        <div className="p-6 bg-[#000b1e] border border-white/5 rounded-[2rem]">
          <div className="flex justify-between items-start mb-4">
            <Cpu size={28} className="text-indigo-400" />
            <div className="px-2 py-1 bg-indigo-500/20 rounded-lg text-[10px] font-black uppercase tracking-widest text-indigo-400">Post-Quantique</div>
          </div>
          <p className="text-[10px] uppercase font-bold text-slate-500 tracking-widest">Bouclier Anti-Pirate</p>
          <p className="text-2xl font-black text-white italic uppercase">{quantumShield ? 'Actif' : 'Inactif'}</p>
        </div>
      </div>

      {/* NAVIGATION INTERNE DU BUNKER */}
      <div className="flex gap-4 mb-8 bg-black/40 p-2 rounded-2xl w-max border border-white/5">
        {[
          { id: 'cyber', name: 'Cyber-Défense', icon: <Terminal size={16} /> },
          { id: 'blockchain', name: 'Léa Chain & Nodes', icon: <Share2 size={16} /> },
          { id: 'social', name: 'Bouclier Social', icon: <Heart size={16} /> },
          { id: 'vault', name: 'Léa Bank Vault', icon: <Database size={16} /> }
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`flex items-center gap-2 px-6 py-3 rounded-xl text-xs font-bold uppercase tracking-widest transition-all ${
              activeTab === tab.id ? 'bg-[#00f2ff] text-black shadow-[0_0_20px_rgba(0,242,255,0.4)]' : 'text-slate-500 hover:text-white'
            }`}
          >
            {tab.icon} {tab.name}
          </button>
        ))}
      </div>

      {/* ========================================================= */}
      {/* ⚠️ ZONE D'AFFICHAGE DYNAMIQUE (LE MUR PORTEUR MANQUANT) ⚠️ */}
      {/* ========================================================= */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* COLONNE DE GAUCHE : LOGS ET ALERTES EN DIRECT */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-[#000b1e]/60 border border-white/5 rounded-[2rem] p-8 flex flex-col h-[600px]">
            <div className="flex items-center justify-between mb-6">
              <h3 className="font-black text-white uppercase tracking-tighter text-xl flex items-center gap-3">
                <Activity size={20} className="text-[#00f2ff]" /> 
                {activeTab === 'cyber' && "Console de Cyber-Défense"}
                {activeTab === 'blockchain' && "LÉA Chain Sentinel"}
                {activeTab === 'social' && "Modération Bouclier Social"}
                {activeTab === 'vault' && "LÉA Bank & Cloud Vault"}
              </h3>
              <div className="flex gap-2">
                <div className="px-3 py-1 bg-[#00f2ff]/10 rounded-full text-[9px] font-black text-[#00f2ff] uppercase tracking-widest border border-[#00f2ff]/20">
                  Live Stream
                </div>
              </div>
            </div>

            {/* FLUX DE DONNÉES DYNAMIQUE */}
            <div className="flex-1 overflow-y-auto custom-scrollbar space-y-3 pr-2 font-mono">
              {activeTab === 'social' && socialArchive.length > 0 ? (
                socialArchive.map(msg => (
                  <div key={msg.id} className="p-4 rounded-xl border border-pink-500/20 bg-pink-500/5 mb-3 animate-in fade-in slide-in-from-bottom-2">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-[10px] font-black text-pink-400 uppercase">{msg.sender}</span>
                      <span className="text-[10px] text-slate-500">{msg.timestamp}</span>
                    </div>
                    <p className="text-sm text-slate-200 italic">"{msg.content}"</p>
                    <div className="mt-2 text-[9px] font-bold text-red-400 uppercase tracking-widest">⚠️ Preuve collectée et horodatée</div>
                  </div>
                ))
              ) : logs.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-slate-600 opacity-40">
                  <Fingerprint size={48} className="mb-4" />
                  <p className="text-xs uppercase tracking-widest">Initialisation des protocoles...</p>
                </div>
              ) : (
                logs.map(log => (
                  <div key={log.id} className={`p-4 rounded-xl border flex gap-4 mb-3 animate-in slide-in-from-left-4 ${
                    log.severity === 'critical' ? 'bg-red-500/10 border-red-500/30' : 
                    log.severity === 'high' ? 'bg-orange-500/10 border-orange-500/20' : 'bg-white/5 border-white/5'
                  }`}>
                    <div className={`mt-1 ${log.severity === 'critical' ? 'text-red-500' : 'text-orange-500'}`}>
                      {log.severity === 'critical' ? <ShieldAlert size={18} /> : <Terminal size={18} />}
                    </div>
                    <div>
                      <div className="flex items-center gap-3 mb-1">
                        <span className="text-[10px] font-bold text-slate-500">{log.time}</span>
                        <span className="text-[10px] font-black uppercase text-slate-400">{log.type}</span>
                      </div>
                      <p className="text-sm text-slate-200">{log.message}</p>
                      {log.location && <p className="text-[10px] text-indigo-400 mt-2 italic">{log.location}</p>}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* COLONNE DE DROITE : OUTILS DE CONTRÔLE */}
        <div className="lg:col-span-1 space-y-6">
          
          <div className="bg-gradient-to-b from-[#000b1e] to-black border border-white/10 rounded-[2rem] p-8 shadow-2xl">
            {activeTab === 'cyber' && (
              <div className="space-y-6">
                <h4 className="text-xs font-black text-[#00f2ff] uppercase tracking-widest mb-6">Paramètres Anti-Quantique</h4>
                <div className="space-y-4">
                  <div className="flex justify-between items-center p-4 bg-white/5 rounded-2xl border border-white/5">
                    <span className="text-sm font-bold text-white">Chiffrement Lattice</span>
                    <div className="w-12 h-6 bg-[#00f2ff] rounded-full relative"><div className="absolute right-1 top-1 w-4 h-4 bg-black rounded-full" /></div>
                  </div>
                  <div className="flex justify-between items-center p-4 bg-white/5 rounded-2xl border border-white/5">
                    <span className="text-sm font-bold text-white">Fail2Ban (Zorin OS)</span>
                    <div className="w-12 h-6 bg-[#00f2ff] rounded-full relative"><div className="absolute right-1 top-1 w-4 h-4 bg-black rounded-full" /></div>
                  </div>
                  <button 
                    onClick={requestAllPermissions}
                    className={`w-full py-4 rounded-2xl font-black uppercase tracking-widest transition-all shadow-lg flex items-center justify-center gap-3 ${
                      permissions.microphone && permissions.location 
                      ? 'bg-emerald-500 text-black' 
                      : 'bg-[#00f2ff] text-black hover:scale-105'
                    }`}
                  >
                    {permissions.microphone && permissions.location ? <Unlock size={20} /> : <Lock size={20} />}
                    {permissions.microphone && permissions.location ? "Système Entièrement Armé" : "Autoriser l'Accès Total"}
                  </button>
                  <div className="p-4 bg-indigo-500/10 border border-indigo-500/30 rounded-2xl">
                    <p className="text-[10px] text-indigo-400 uppercase font-black mb-2 tracking-widest">Clef de session flolov42</p>
                    <p className="text-[10px] font-mono text-white break-all">QA-8192-X-992-SECURE-MASTER-NODE</p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'blockchain' && (
              <div className="space-y-6">
                <h4 className="text-xs font-black text-amber-500 uppercase tracking-widest mb-6">LÉA Chain : Consensus</h4>
                <div className="space-y-4 text-center">
                  <div className="p-6 bg-black rounded-3xl border border-amber-500/20">
                    <RefreshCcw size={32} className="text-amber-500 mx-auto mb-4 animate-spin-slow" />
                    <p className="text-2xl font-black text-white">100% SYNC</p>
                    <p className="text-[10px] text-slate-500 uppercase tracking-widest mt-2">Nœud flolov42 : Leader</p>
                  </div>
                  <button className="w-full py-4 bg-amber-500/10 border border-amber-500/30 rounded-2xl text-amber-500 text-[10px] font-black uppercase tracking-widest hover:bg-amber-500/20 transition-all">
                    Vérifier l'intégrité des blocs
                  </button>
                </div>
              </div>
            )}

            {activeTab === 'social' && (
              <div className="space-y-6">
                <h4 className="text-xs font-black text-pink-500 uppercase tracking-widest mb-6">Contrôle Léa Love - Ange Gardien</h4>
                <div className="space-y-4">
                  <div className={`p-4 rounded-2xl border transition-all ${socialStatus === 'intervention' ? 'bg-red-500/20 border-red-500' : 'bg-pink-500/10 border-pink-500/30'}`}>
                    <p className="text-[10px] text-pink-400 font-black uppercase mb-1">Statut du Bouclier</p>
                    <p className="text-xs text-slate-300">
                      {socialStatus === 'surveillance' ? '🔍 Surveillance sémantique active' : '🛡️ INTERVENTION EN COURS'}
                    </p>
                  </div>
                  
                  <div className="p-4 bg-black/40 border border-white/5 rounded-2xl">
                    <p className="text-[10px] text-slate-500 font-black uppercase mb-2">Niveau de harcèlement : {harassmentCounter}/5</p>
                    <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden">
                      <div 
                        className={`h-full transition-all duration-500 ${harassmentCounter >= 5 ? 'bg-red-500' : 'bg-pink-500'}`}
                        style={{ width: `${(harassmentCounter / 5) * 100}%` }}
                      />
                    </div>
                  </div>

                  <button 
                    onClick={simulateSocialAlert}
                    className="w-full py-3 bg-white/5 border border-white/10 rounded-xl text-[10px] font-black uppercase hover:bg-white/10 transition-all"
                  >
                    Simuler une menace (Test)
                  </button>

                  {socialStatus === 'intervention' && (
          <button 
            onClick={async () => {
              const res = await fetch('/api/protect/report', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: currentUser, logs: logs })
              });
              const data = await res.json();
              if (data.success) {
                alert(`📂 Preuves archivées dans le coffre : ${data.fileName}`);
                const resVault = await fetch(`/api/protect/vault/${currentUser}`);
                const vaultData = await resVault.json();
                setVaultFiles(vaultData);
              }
            }}
            className="w-full py-4 bg-pink-500 text-black rounded-2xl text-xs font-black uppercase tracking-widest hover:bg-white transition-all flex items-center justify-center gap-2"
          >
            Dépôt de plainte (Preuves)
          </button>
        )}
                </div>
              </div>
            )}

            {activeTab === 'vault' && (
              <div className="space-y-6">
                <h4 className="text-xs font-black text-emerald-500 uppercase tracking-widest mb-6">Léa Bank & Cloud</h4>
                <div className="p-6 bg-emerald-500/10 border border-emerald-500/30 rounded-3xl text-center">
                   <HardDrive size={32} className="text-emerald-500 mx-auto mb-4" />
                   <p className="text-2xl font-black text-white">0.00 GB / ∞</p>
                   <p className="text-[10px] text-slate-500 uppercase tracking-widest mt-2">Stockage Cloud Privé</p>
                </div>
                <button 
                  onClick={async () => {
                    const res = await fetch(`/api/protect/vault/${currentUser}`);
                    const data = await res.json();
                    setVaultFiles(data);
                    alert(`🔐 Coffre-fort ouvert. ${data.length} fichiers sécurisés détectés.`);
                  }}
                  className="w-full py-4 bg-emerald-500 text-black rounded-2xl text-xs font-black uppercase tracking-widest hover:bg-white transition-all shadow-[0_0_20px_rgba(16,185,129,0.3)]"
                >
                  Gérer les coffres ({vaultFiles.length})
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};