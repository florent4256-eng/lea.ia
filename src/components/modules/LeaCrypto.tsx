import React, { useState, useEffect, useRef } from 'react';
import { 
    Wallet, Pickaxe, TrendingUp, BarChart2, MessageCircle, 
    Send, Cpu, Play, Square, Settings, Activity, Terminal,
    Search, BarChart3 
} from 'lucide-react';
import { createChart, ColorType } from 'lightweight-charts';

export const LeaCrypto = () => {
    const [activeTab, setActiveTab] = useState('bot');
    const [isLeaChatOpen, setIsLeaChatOpen] = useState(false);

    // --- 🛡️ IDENTITÉ DU NAVIGATEUR ---
    const currentUser = localStorage.getItem('lea_currentUser') || '';

    // --- 🛡️ MUR DE FER : ÉTATS DU VIGILE ---
    const [isConfigured, setIsConfigured] = useState<boolean | null>(null);
    const [exchangeInput, setExchangeInput] = useState('binance'); // Par défaut
    const [apiKeyInput, setApiKeyInput] = useState('');
    const [apiSecretInput, setApiSecretInput] = useState('');
    const [isSavingKeys, setIsSavingKeys] = useState(false);
    const [keyError, setKeyError] = useState('');
    
    // --- ÉTATS RÉELS DU SYSTÈME ---
    // --- ÉTATS RÉELS DU SYSTÈME (DOUBLE MOTEUR) ---
    const [isSniperRunning, setIsSniperRunning] = useState(false);
    const [sniperBudget, setSniperBudget] = useState<number | string>(25);
    const [sniperLeverage, setSniperLeverage] = useState(20);

    const [isEclairRunning, setIsEclairRunning] = useState(false);
    const [eclairBudget, setEclairBudget] = useState<number | string>(20);
    const [eclairLeverage, setEclairLeverage] = useState(20);

    const [logs, setLogs] = useState<{bot: string, text: string, time: string}[]>([
        { bot: 'SYSTÈME', text: 'Interface connectée à la Matrice. Prêts pour le tir.', time: new Date().toLocaleTimeString() }
    ]);
    const [logFilter, setLogFilter] = useState<'ALL' | 'SNIPER' | 'ECLAIR'>('ALL');
    const [selectedCrypto, setSelectedCrypto] = useState<string | null>(null);
    const [layoutMode, setLayoutMode] = useState<'simple' | 'advanced' | null>(null);
    const [favorites, setFavorites] = useState<string[]>([]);
    const [showOnlyFavorites, setShowOnlyFavorites] = useState(false);
    const [chartType, setChartType] = useState('candle'); 
    const [searchTerm, setSearchTerm] = useState(''); 
    const [orderSide, setOrderSide] = useState<'buy' | 'sell'>('buy');
    const [orderType, setOrderType] = useState<'market' | 'limit'>('market');
    const [orderAmount, setOrderAmount] = useState('');
    const [orderPrice, setOrderPrice] = useState('');
    const [portfolio, setPortfolio] = useState<{totalEUR: number, usdt: number, assets: any[], history: any[]}>({
        totalEUR: 0,
        usdt: 0,
        assets: [],
        history: []
    
    });

    const fetchPortfolioData = async () => {
        try {
            const response = await fetch('/api/crypto/portfolio');
            const result = await response.json();
            if (result.success) {
                setPortfolio({
                    totalEUR: result.totalEUR,
                    usdt: result.usdt,
                    assets: result.assets,
                    history: result.history
                });
            }
        } catch (error) {
            console.error("Erreur récupération portfolio");
        }
    };// <--- AJOUT ICI// Si tu ne l'avais pas déjà

    const fetchFavorites = async () => {
        try {
            // On envoie le pseudo dans l'URL
            const response = await fetch(`/api/crypto/favorites?username=${currentUser}`);
            const result = await response.json();
            if (result.success) setFavorites(result.favorites);
        } catch (error) {}
    };

    // 3.7 LE RADAR PM2 (Vérifie si les bots sont immortels)
    const fetchBotStatus = async () => {
        if (!currentUser) return;
        try {
            const resSniper = await fetch(`/api/crypto/bot/status?username=${currentUser}&botType=sniper`);
            const dataSniper = await resSniper.json();
            if (dataSniper.success && dataSniper.isRunning) {
                setIsSniperRunning(true);
                setLogs(prev => [...prev, { bot: 'SNIPER', text: `🛡️ Analyse : Sniper détecté en arrière-plan.`, time: new Date().toLocaleTimeString() }]);
            }
            
            const resEclair = await fetch(`/api/crypto/bot/status?username=${currentUser}&botType=eclair`);
            const dataEclair = await resEclair.json();
            if (dataEclair.success && dataEclair.isRunning) {
                setIsEclairRunning(true);
                setLogs(prev => [...prev, { bot: 'ECLAIR', text: `🛡️ Analyse : Bot Éclair détecté en arrière-plan.`, time: new Date().toLocaleTimeString() }]);
            }
        } catch (error) { console.error("Erreur radar PM2"); }
    };

    const toggleFavorite = async (symbol: string, e: React.MouseEvent) => {
        e.stopPropagation();
        const action = favorites.includes(symbol) ? 'remove' : 'add';
        try {
            const response = await fetch('/api/crypto/favorites', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                // On glisse le pseudo dans le colis
                body: JSON.stringify({ favorite: symbol, action, username: currentUser })
            });
            const result = await response.json();
            if (result.success) setFavorites(result.favorites);
        } catch (error) {}
    };


    const [wallet, setWallet] = useState({ balance: 0.00, usdt: 0.00 });
    const [marketData, setMarketData] = useState<any[]>([]);

    const scrollRef = useRef<HTMLDivElement>(null);

    // 1. AUTO-SCROLL DU TERMINAL
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [logs]);

    // 2. WEBSOCKET (LE VRAI FLUX DU TERMINAL) ET MARCHÉ
    useEffect(() => {
        // Chargement initial du marché et du wallet
        fetchMarketData();
        fetchWalletData();
        fetchFavorites();
        fetchBotStatus();

        // 🛡️ ACTUALISATION DU SOLDE EN SILENCE (Toutes les 5 secondes)
        const walletInterval = setInterval(() => { fetchWalletData(); }, 5000);

        // Câblage direct au serveur pour les logs du bot
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${wsProtocol}//${window.location.host}`;
        const ws = new WebSocket(wsUrl);

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                
                // Bloc existant pour les logs
                if (data.type === 'BOT_LOG') {
                    let botSource = 'SYSTÈME';
                    if (data.message.includes('ÉCLAIR') || data.message.includes('SCALPING') || data.message.includes('ECLAIR')) botSource = 'ECLAIR';
                    else if (data.message.includes('SNIPER') || data.message.includes('ACHAT') || data.message.includes('PROFIT')) botSource = 'SNIPER';
                    setLogs(prev => [...prev, { bot: botSource, text: data.message, time: new Date().toLocaleTimeString() }]);
                }

            } catch (e) {}
        };

        return () => { clearInterval(walletInterval); ws.close(); };
    }, []);

    // 3. FONCTION DE LECTURE DU MARCHÉ EN DIRECT (Via ta route Kraken)
    const fetchMarketData = async () => {
        try {
            const response = await fetch('/api/crypto/market');
            const result = await response.json();
            if (result.success) {
                setMarketData(result.data);
            }
        } catch (error) {
            console.error("Erreur de récupération du marché");
        }
    };

    // 3.5 LECTURE DU WALLET RÉEL (AVEC LE VRAI VIGILE)
    const fetchWalletData = async (userOverride = '') => {
        try {
            // On utilise le nom qu'on vient de taper, sinon on cherche dans la mémoire
            const activeUser = userOverride || localStorage.getItem('lea_currentUser') || currentUser;
            
            // On demande le coffre du bon utilisateur
            const response = await fetch(`/api/crypto/wallet?username=${activeUser}`);
            const result = await response.json();
            
            if (result.success && result.isConfigured === true) {
                setWallet({ balance: result.balance, usdt: result.usdt });
                setIsConfigured(true);
            } else {
                setIsConfigured(false);
            }
        } catch (error) {
            console.error("Erreur de récupération du portefeuille");
            setIsConfigured(false);
        }
    };

    // --- 3.8 LE MOTEUR GRAPHIQUE PRO (TRADINGVIEW) ---
    useEffect(() => {
        if (activeTab !== 'pro') return;

        // Délai de sécurité pour laisser Zorin OS dessiner la page avant de dessiner
        const timer = setTimeout(() => {
            const chartContainer = document.getElementById('crypto-chart');
            if (!chartContainer) return;

            chartContainer.innerHTML = ''; // Nettoyage de la zone

            try {
                // Création blindée : On force la hauteur à 400px pour éviter le crash
                const chart: any = createChart(chartContainer as any, {
                    layout: { background: { type: ColorType.Solid, color: 'transparent' }, textColor: '#94a3b8' },
                    grid: { vertLines: { color: 'rgba(30, 41, 59, 0.2)' }, horzLines: { color: 'rgba(30, 41, 59, 0.2)' } },
                    width: chartContainer.clientWidth || 800,
                    height: 400, 
                    timeScale: { timeVisible: true, secondsVisible: false },
                } as any);

                let series: any;
                if (chartType === 'candle') {
                    series = chart.addCandlestickSeries({
                        upColor: '#22c55e', downColor: '#ef4444', borderVisible: false,
                        wickUpColor: '#22c55e', wickDownColor: '#ef4444'
                    } as any);
                } else {
                    series = chart.addLineSeries({ color: '#00f2ff', lineWidth: 2 } as any);
                }

                // Aspiration des bougies
                fetch('/api/crypto/ohlcv/BTC/1h')
                    .then(res => res.json())
                    .then(result => {
                        if (result.success && result.data) {
                            const formattedData = chartType === 'line' 
                                ? result.data.map((d: any) => ({ time: d.time, value: d.close }))
                                : result.data;
                            series.setData(formattedData);
                            chart.timeScale().fitContent();
                        }
                    });

            } catch (error) {
                console.error("Le moteur TradingView a été intercepté :", error);
            }
        }, 100); // 100 millisecondes de délai

        // Nettoyage si on change d'onglet avant la fin du timer
        return () => clearTimeout(timer);
    }, [activeTab, chartType]);

    // --- 3.9 LE SYSTÈME DE TIR (EXECUTION ORDRE) ---
    const executeTrade = async () => {
        if (!selectedCrypto || !orderAmount) return;
        
        try {
            const response = await fetch('/api/crypto/trade', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    symbol: selectedCrypto,
                    side: orderSide,
                    type: orderType,
                    amount: parseFloat(orderAmount),
                    price: orderType === 'limit' ? parseFloat(orderPrice) : undefined
                })
            });
            const result = await response.json();
            if (result.success) {
                alert(`🎯 TIR RÉUSSI : ${result.message}`);
                fetchWalletData(); // Actualise tes sous immédiatement
            } else {
                alert(`⚠️ ÉCHEC DU TIR : ${result.error}`);
            }
        } catch (error) {
            console.error("Erreur de communication avec le serveur");
        }
    };

    // 4. LES MOTEURS D'ACTION (LANCEMENT / STOP)
    const handleToggleBot = async (botType: 'sniper' | 'eclair') => {
        const isRunning = botType === 'sniper' ? isSniperRunning : isEclairRunning;
        const action = !isRunning ? 'start' : 'stop';
        const currentBudget = Number(botType === 'sniper' ? sniperBudget : eclairBudget);
        const currentLeverage = botType === 'sniper' ? sniperLeverage : eclairLeverage;

        // 🛡️ SÉCURITÉ : VERROU DU BUDGET MINIMUM
        if (action === 'start' && currentBudget < 10) {
            alert(`⚠️ TIR ANNULÉ : Le budget minimum pour lancer le ${botType.toUpperCase()} est de 10 $ afin d'absorber les frais.`);
            return;
        }
        
        try {
            if (action === 'start') {
                await fetch('/api/crypto/bot/config', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: currentUser, budgetActif: currentBudget, levier: currentLeverage, botType })
                });
                setLogs(prev => [...prev, { bot: botType.toUpperCase(), text: `[CONFIG] Budget validé : ${currentBudget} $ (x${currentLeverage})`, time: new Date().toLocaleTimeString() }]);
            }

            const response = await fetch('/api/crypto/bot/toggle', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action, username: currentUser, botType })
            });
            const result = await response.json();
            
            if (result.success) {
                if (botType === 'sniper') setIsSniperRunning(action === 'start');
                else setIsEclairRunning(action === 'start');
                setLogs(prev => [...prev, { bot: botType.toUpperCase(), text: `Ordre de ${action === 'start' ? 'DÉMARRAGE' : 'ARRÊT'} exécuté.`, time: new Date().toLocaleTimeString() }]);
            } else {
                setLogs(prev => [...prev, { bot: 'SYSTÈME', text: `[ERREUR] ${result.message}`, time: new Date().toLocaleTimeString() }]);
            }
        } catch (error) {
            setLogs(prev => [...prev, { bot: 'SYSTÈME', text: `[FATAL] Perte de connexion.`, time: new Date().toLocaleTimeString() }]);
        }
    };

    // 🚨 LE BOUTON D'URGENCE (KILL SWITCH)
    const handleEmergencyStop = async () => {
        if(window.confirm("🚨 URGENCE ABSOLUE : Es-tu sûr de vouloir COUPER tous les bots de trading instantanément ?")) {
            try {
                if (isSniperRunning) await fetch('/api/crypto/bot/toggle', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: 'stop', username: currentUser, botType: 'sniper' }) });
                if (isEclairRunning) await fetch('/api/crypto/bot/toggle', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: 'stop', username: currentUser, botType: 'eclair' }) });
                
                setIsSniperRunning(false);
                setIsEclairRunning(false);
                setLogs(prev => [...prev, { bot: 'SYSTÈME', text: `🛑 [ARRÊT D'URGENCE] Tous les moteurs ont été coupés.`, time: new Date().toLocaleTimeString() }]);
            } catch (e) {
                setLogs(prev => [...prev, { bot: 'SYSTÈME', text: `❌ Erreur lors de l'arrêt d'urgence.`, time: new Date().toLocaleTimeString() }]);
            }
        }
    };

    // --- MOTEUR D'ENREGISTREMENT DES CLÉS ---
    const handleSaveKeys = async () => {
        if (!apiKeyInput || !apiSecretInput) {
            setKeyError('Veuillez remplir toutes les clés.');
            return;
        }
        setIsSavingKeys(true);
        setKeyError('');
        try {
            const response = await fetch('/api/crypto/keys', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    exchange: exchangeInput,
                    apiKey: apiKeyInput,
                    apiSecret: apiSecretInput,
                    username: currentUser 
                })
            });
            const result = await response.json();
            if (result.success) {
                setIsConfigured(true); 
                fetchWalletData();     
            } else {
                setKeyError(result.message || 'Erreur lors de la sauvegarde.');
            }
        } catch (error) {
            setKeyError('Erreur de connexion au serveur.');
        } finally {
            setIsSavingKeys(false);
        }
    };

    // --- [MOTEUR LÉA V3] SYSTÈME D'AUTHENTIFICATION INTELLIGENT ---
    const [showLogin, setShowLogin] = useState(false);
    const [loginInput, setLoginInput] = useState('');
    const [passwordInput, setPasswordInput] = useState('');
    const [loginError, setLoginError] = useState('');

    useEffect(() => {
        const verifyAuth = async () => {
            const storedPseudo = localStorage.getItem('lea_currentUser');
            const storedPassword = localStorage.getItem('lea_token'); // Nom discret pour la mémoire
            
            if (!storedPseudo || !storedPassword) {
                setShowLogin(true);
                return;
            }

            try {
                const response = await fetch('/api/auth/verify-session', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: storedPseudo, password: storedPassword })
                });
                const result = await response.json();
                if (!result.success) {
                    setShowLogin(true);
                }
            } catch (error) {
                console.error("Liaison Master rompue");
            }
        };
        verifyAuth();
    }, []);

    const handleLogin = async () => {
        setLoginError('');
        if (loginInput.trim() && passwordInput.trim()) {
            try {
                // On toque à la porte du serveur avec les identifiants
                const response = await fetch('/api/auth/verify-session', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: loginInput.toLowerCase(), password: passwordInput })
                });
                const result = await response.json();
                
                if (result.success) {
                    localStorage.setItem('lea_currentUser', loginInput.toLowerCase());
                    localStorage.setItem('lea_token', passwordInput);
                    
                    // 🛡️ CORRECTION : On ne recharge plus la page !
                    // On ferme juste l'écran de sécurité et on réveille tous les modules
                    setShowLogin(false);
                    
                    // On lance l'aspiration des données en direct
                    fetchWalletData(loginInput.toLowerCase());
                    fetchMarketData();
                    fetchFavorites();
                    if (typeof fetchBotStatus === 'function') fetchBotStatus();
                    
                    console.log("🔓 [Matrice] Accès validé. Bienvenue dans ton centre de tir.");
                } else {
                    setLoginError(result.error || "Identifiants invalides");
                }
            } catch (error) {
                setLoginError("Serveur injoignable");
            }
        } else {
            setLoginError("Veuillez remplir tous les champs");
        }
    };

    if (showLogin) {
        return (
            <div className="flex flex-col items-center justify-center h-full w-full bg-[#0a0e14] p-8 text-center">
                <div className="bg-[#0b0e14] p-10 rounded-[40px] border border-teal-500/30 shadow-[0_0_50px_rgba(20,184,166,0.1)] max-w-md w-full">
                    <div className="w-20 h-20 bg-teal-500/10 rounded-3xl flex items-center justify-center border border-teal-500/30 mx-auto mb-6">
                        <Cpu className="text-teal-400" size={40} />
                    </div>
                    <h2 className="text-2xl font-black text-white uppercase tracking-tighter mb-2">Sécurité Matrice</h2>
                    <p className="text-gray-500 text-sm mb-6">Identifie-toi pour déverrouiller ton Sniper Bot.</p>
                    
                    {loginError && (
                        <div className="bg-red-500/10 border border-red-500/50 text-red-500 text-xs font-bold p-3 rounded-xl mb-4 uppercase tracking-wider">
                            {loginError}
                        </div>
                    )}

                    <div className="space-y-4 mb-6">
                        <input 
                            type="text" 
                            placeholder="Ton Pseudo"
                            value={loginInput}
                            onChange={(e) => setLoginInput(e.target.value)}
                            className="w-full bg-[#11141b] border border-gray-800 p-4 rounded-xl text-white outline-none focus:border-teal-500 transition-colors text-center font-bold"
                        />
                        
                        <input 
                            type="password" 
                            placeholder="Mot de passe"
                            value={passwordInput}
                            onChange={(e) => setPasswordInput(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
                            className="w-full bg-[#11141b] border border-gray-800 p-4 rounded-xl text-white outline-none focus:border-teal-500 transition-colors text-center font-bold"
                        />
                    </div>
                    
                    <button 
                        onClick={handleLogin}
                        className="w-full py-4 bg-teal-600 hover:bg-teal-500 text-black font-black rounded-xl uppercase tracking-widest transition-all shadow-lg"
                    >
                        Ouvrir le coffre
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="flex h-full w-full bg-[#0a0e14] text-white overflow-hidden rounded-xl border border-gray-800 shadow-2xl relative">
            
            {/* MENU LATÉRAL */}
            <div className="w-64 bg-[#111827] p-4 border-r border-teal-500/20 flex flex-col z-10">
                <h2 className="text-xl font-bold text-teal-400 mb-8 tracking-wider flex items-center space-x-2">
                    <div className="w-8 h-8 bg-teal-500/10 rounded-lg flex items-center justify-center border border-teal-500/30">
                        <Wallet className="text-teal-400" size={18} />
                    </div>
                    <span className="bg-gradient-to-r from-teal-400 to-blue-400 bg-clip-text text-transparent">Léa Crypto</span>
                </h2>
                
                <nav className="space-y-2">
                    <button onClick={() => setActiveTab('wallet')} className={`flex items-center space-x-3 w-full p-3 rounded-xl transition-all ${activeTab === 'wallet' ? 'bg-teal-500/10 text-teal-400 shadow-lg border-l-4 border-teal-400' : 'hover:bg-gray-800 text-gray-500'}`}>
    <Wallet size={18} /><span>Wallet</span>
</button>
                    <button onClick={() => setActiveTab('bot')} className={`flex items-center space-x-3 w-full p-3 rounded-xl transition-all ${activeTab === 'bot' ? 'bg-teal-500/10 text-teal-400 shadow-lg border-l-4 border-teal-400' : 'hover:bg-gray-800 text-gray-500'}`}>
                        <Cpu size={18} /><span>Sniper Bot V3</span>
                    </button>
                    <button onClick={() => { setActiveTab('pro'); fetchMarketData(); }} className={`flex items-center space-x-3 w-full p-3 rounded-xl transition-all ${activeTab === 'pro' ? 'bg-teal-500/10 text-teal-400 shadow-lg border-l-4 border-teal-400' : 'hover:bg-gray-800 text-gray-500'}`}>
                        <BarChart2 size={18} /><span>Marché Pro</span>
                    </button>
                    <button onClick={() => setActiveTab('mining')} className={`flex items-center space-x-3 w-full p-3 rounded-xl transition-all ${activeTab === 'mining' ? 'bg-teal-500/10 text-teal-400 shadow-lg border-l-4 border-teal-400' : 'hover:bg-gray-800 text-gray-500'}`}>
                        <Pickaxe size={18} /><span>Minage</span>
                    </button>
                </nav>

                <div className="mt-auto bg-gray-900/80 p-4 rounded-2xl border border-teal-500/10">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-[10px] text-gray-500 uppercase tracking-widest">Liaison Kraken</span>
                        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse shadow-[0_0_8px_rgba(34,197,94,0.6)]"></div>
                    </div>
                    <p className="text-sm font-mono text-teal-400">CONNECTÉ : {currentUser.toUpperCase()}</p>
                </div>
            </div>

            {/* ZONE CENTRALE */}
            <div className="flex-1 p-8 overflow-y-auto relative flex flex-col bg-[radial-gradient(circle_at_top_right,_var(--tw-gradient-stops))] from-teal-900/10 via-transparent to-transparent">
                
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h3 className="text-3xl font-bold text-gray-100 tracking-tight">
                            {activeTab === 'bot' ? 'Sniper Command Center' : activeTab.toUpperCase()}
                        </h3>
                    </div>
                </div>

                {/* ONGLET WALLET (PORTEFEUILLE PRO) */}
                {activeTab === 'wallet' && (
                    <div className="flex flex-col h-full space-y-6 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-800">
                        
                        {/* 1. VUE D'ENSEMBLE & GRAPHIQUE BLEU */}
                        <div className="bg-[#0b0e14] p-8 rounded-[40px] border border-gray-800 shadow-inner shadow-black">
                            <div className="flex justify-between items-start mb-8">
                                <div>
                                    <p className="text-gray-500 text-xs font-black uppercase tracking-[0.2em] mb-1">Valeur Totale</p>
                                    <h2 className="text-5xl font-black text-white tracking-tighter">
                                        {(portfolio.totalEUR || 0).toLocaleString(undefined, {minimumFractionDigits: 2})} <span className="text-gray-600">USD</span>
                                    </h2>
                                    <p className="text-green-500 text-sm font-bold mt-2">▲ +452.75% <span className="text-gray-600 font-medium ml-1">derniers mois</span></p>
                                </div>
                                
                                {/* Menu d'actions rapides (Transfert, Convertir...) */}
                                <div className="flex gap-3">
                                    {['Transférer', 'Retirer', 'Dépôt', 'Convertir'].map((action) => (
                                        <button key={action} className="px-5 py-2.5 bg-[#181d26] hover:bg-teal-500 hover:text-black border border-gray-800 rounded-xl text-xs font-black text-gray-400 transition-all uppercase tracking-wider">
                                            {action}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* LE GRAPHIQUE D'ÉVOLUTION BLEU (Standard Kraken Pro) */}
                            <div className="w-full h-64 bg-[#11141b] rounded-3xl border border-gray-800/50 relative overflow-hidden">
                                <div className="absolute top-4 right-6 flex gap-4 z-10">
                                    {['1S', '1M', '3M', '6M', '1A', 'Tout'].map(t => (
                                        <button key={t} className={`text-[10px] font-black ${t === 'Tout' ? 'text-teal-400' : 'text-gray-600 hover:text-white'}`}>{t}</button>
                                    ))}
                                </div>
                                {/* Intégration du graphique temporel réel */}
                                <iframe
    src="https://s.tradingview.com/widgetembed/?symbol=KRAKEN%3ABTCUSD&interval=D&hidesidetoolbar=1&theme=dark&style=3&timezone=Etc%2FUTC&locale=fr"
    style={{ width: '100%', height: '110%', marginTop: '-5%', border: 'none' }}
    title="Portfolio Growth"
/>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                            {/* 2. RÉPARTITION DES ACTIFS (TES CRYPTOS) */}
                            <div className="bg-[#0b0e14] p-6 rounded-[40px] border border-gray-800">
                                <h4 className="text-lg font-black text-white mb-6 px-2 uppercase tracking-widest flex items-center gap-2">
                                    <Wallet size={18} className="text-teal-500" /> Tes Actifs
                                </h4>
                                <div className="space-y-2">
                                    {portfolio.assets.map((asset, idx) => (
                                        <div key={idx} className="flex justify-between items-center p-4 bg-[#181d26]/50 rounded-2xl border border-transparent hover:border-gray-700 transition-all">
                                            <div className="flex items-center gap-4">
                                                <div className="w-10 h-10 bg-black/40 rounded-full flex items-center justify-center font-black text-teal-400 text-xs border border-gray-800">{asset.name[0]}</div>
                                                <div>
                                                    <p className="font-bold text-white">{asset.name}</p>
                                                    <p className="text-[10px] text-gray-500 font-mono">{asset.amount.toFixed(8)} {asset.name}</p>
                                                </div>
                                            </div>
                                            <div className="text-right">
                                                <p className="font-mono text-white text-sm">-- EUR</p>
                                                <p className="text-[10px] text-gray-600">Prix Live Kraken</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* 3. HISTORIQUE DES TRANSACTIONS */}
                            <div className="bg-[#0b0e14] p-6 rounded-[40px] border border-gray-800">
                                <h4 className="text-lg font-black text-white mb-6 px-2 uppercase tracking-widest flex items-center gap-2">
                                    <Activity size={18} className="text-teal-500" /> Historique
                                </h4>
                                <div className="space-y-3">
                                    {portfolio.history.map((item, idx) => (
                                        <div key={idx} className="flex justify-between items-center px-4 py-3 bg-black/20 rounded-xl border border-gray-800/50">
                                            <div className="flex flex-col">
                                                <span className="text-[10px] text-gray-600 font-bold">{new Date(item.date).toLocaleDateString()}</span>
                                                <span className="text-sm font-bold text-gray-200">{item.type} {item.asset}</span>
                                            </div>
                                            <div className="text-right">
                                                <div className="bg-green-500/10 text-green-500 text-[9px] font-black px-2 py-0.5 rounded uppercase mb-1 border border-green-500/20">{item.status}</div>
                                                <p className="text-xs font-mono text-gray-400">{item.amount > 0 ? '+' : ''}{item.amount}</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* ONGLET BOT SNIPER */}
                {activeTab === 'bot' && (
                    <div className="flex flex-col space-y-6 h-full">
                        
                        {/* CAS 1 : Chargement (Le Vigile interroge le serveur) */}
                        {isConfigured === null ? (
                            <div className="flex flex-col items-center justify-center h-full text-teal-500 animate-pulse space-y-4">
                                <div className="w-12 h-12 border-4 border-teal-500 border-t-transparent rounded-full animate-spin"></div>
                                <p className="tracking-widest text-sm uppercase font-bold text-gray-400">Analyse de la matrice en cours...</p>
                            </div>
                        ) 
                        
                        /* CAS 2 : Pas de clés = LE SAS DE SÉCURITÉ */
                        : isConfigured === false ? (
                            <div className="flex items-center justify-center h-full">
                                <div className="bg-[#0b0e14] p-10 rounded-[40px] border border-red-500/30 shadow-[0_0_50px_rgba(239,68,68,0.1)] max-w-lg w-full text-center relative overflow-hidden">
                                    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-red-500 via-orange-500 to-red-500"></div>
                                    
                                    <Activity size={48} className="text-red-500 mx-auto mb-6" />
                                    <h3 className="text-3xl font-black text-white mb-2 uppercase tracking-tight">Accès Restreint</h3>
                                    <p className="text-gray-400 text-sm mb-8">Vous devez lier une plateforme d'échange pour déverrouiller le Sniper Bot V3.</p>

                                    {keyError && (
                                        <div className="bg-red-500/10 border border-red-500/50 text-red-500 text-xs font-bold p-3 rounded-xl mb-6 uppercase tracking-wider">
                                            {keyError}
                                        </div>
                                    )}

                                    <div className="space-y-5 text-left">
                                        <div>
                                            <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Plateforme</label>
                                            <select 
                                                value={exchangeInput}
                                                onChange={(e) => setExchangeInput(e.target.value)}
                                                className="w-full bg-[#11141b] border border-gray-800 p-4 rounded-xl text-white outline-none focus:border-red-500 transition-colors"
                                            >
                                                <option value="krakenfutures">Kraken Futures (Pro)</option>
                                                <option value="binance">Binance</option>
                                                <option value="coinbase">Coinbase</option>
                                            </select>
                                        </div>

                                        <div>
                                            <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Clé API (API Key)</label>
                                            <input 
                                                type="password"
                                                value={apiKeyInput}
                                                onChange={(e) => setApiKeyInput(e.target.value)}
                                                className="w-full bg-[#11141b] border border-gray-800 p-4 rounded-xl text-white outline-none focus:border-red-500 transition-colors font-mono text-sm"
                                                placeholder="Collez votre clé API ici..."
                                            />
                                        </div>

                                        <div>
                                            <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Clé Secrète (API Secret)</label>
                                            <input 
                                                type="password"
                                                value={apiSecretInput}
                                                onChange={(e) => setApiSecretInput(e.target.value)}
                                                className="w-full bg-[#11141b] border border-gray-800 p-4 rounded-xl text-white outline-none focus:border-red-500 transition-colors font-mono text-sm"
                                                placeholder="Collez votre clé secrète ici..."
                                            />
                                        </div>

                                        <button 
                                            onClick={handleSaveKeys}
                                            disabled={isSavingKeys}
                                            className={`w-full py-4 rounded-xl font-black text-white tracking-widest uppercase transition-all mt-4 flex justify-center items-center gap-2 ${isSavingKeys ? 'bg-gray-700 cursor-not-allowed' : 'bg-red-600 hover:bg-red-500 shadow-[0_0_20px_rgba(220,38,38,0.4)]'}`}
                                        >
                                            {isSavingKeys ? <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div> : <><Settings size={18}/> Verrouiller & Activer</>}
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ) 
                        
                        /* CAS 3 : Clés trouvées = LE VRAI COMMAND CENTER */
                        : (
                            <>
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                    <div className="bg-gray-800/40 p-6 rounded-3xl border border-gray-700 flex items-center space-x-4">
                                        <div className="p-3 bg-teal-500/10 rounded-2xl text-teal-400"><Wallet size={24}/></div>
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Solde Base <span className="text-[9px] text-teal-500 ml-1">(Live 5s)</span></p>
                                            <p className="text-2xl font-bold">{wallet.balance.toFixed(2)} $</p>
                                        </div>
                                    </div>
                                    <div className="bg-gray-800/40 p-6 rounded-3xl border border-gray-700 flex items-center space-x-4">
                                        <div className="p-3 bg-orange-500/10 rounded-2xl text-orange-400"><Activity size={24}/></div>
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">État Moteur</p>
                                            <p className={`text-xl font-bold ${(isSniperRunning || isEclairRunning) ? 'text-teal-400' : 'text-gray-400'}`}>
                                                {(isSniperRunning || isEclairRunning) ? 'ACTIF (En chasse)' : 'VEILLE'}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="bg-gray-800/40 p-6 rounded-3xl border border-gray-700 flex items-center space-x-4">
                                        <div className="p-3 bg-blue-500/10 rounded-2xl text-blue-400"><TrendingUp size={24}/></div>
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Cibles Radar</p>
                                            <p className="text-2xl font-bold">10 Cryptos</p>
                                        </div>
                                    </div>
                                </div>

                                {/* BOUTON D'URGENCE GLOBAL */}
                                <button onClick={handleEmergencyStop} className="w-full py-4 mt-6 mb-2 bg-red-600 hover:bg-red-500 text-white font-black text-lg tracking-widest rounded-2xl shadow-[0_0_20px_rgba(220,38,38,0.5)] border border-red-400 transition-all flex justify-center items-center gap-3 uppercase">
                                    <Square size={20} fill="currentColor" /> ARRÊT D'URGENCE GÉNÉRAL (KILL SWITCH)
                                </button>

                                <div className="grid grid-cols-1 xl:grid-cols-2 gap-6 mt-6">
                                    
                                    {/* COLONNE GAUCHE : LES DEUX BOTS */}
                                    <div className="flex flex-col space-y-6">
                                        {/* PANNEAU SNIPER LONGUE PORTÉE */}
                                        <div className="bg-gray-900/60 p-6 rounded-[30px] border border-teal-500/30 backdrop-blur-xl shadow-inner shadow-black relative overflow-hidden">
                                            <div className="absolute top-0 right-0 w-32 h-32 bg-teal-500/10 rounded-full blur-3xl"></div>
                                            <div className="flex justify-between items-center mb-6 relative z-10">
                                                <div>
                                                    <h4 className="text-lg font-black text-white flex items-center gap-2">
                                                        <div className="w-3 h-3 rounded-full bg-teal-400 shadow-[0_0_10px_rgba(45,212,191,0.8)]"></div>
                                                        Sniper Longue Portée
                                                    </h4>
                                                    <p className="text-xs text-gray-400 font-mono mt-1">Chasseur de gros mouvements</p>
                                                </div>
                                                <button 
                                                    onClick={() => handleToggleBot('sniper')}
                                                    className={`flex items-center space-x-2 px-5 py-3 rounded-2xl font-black transition-all ${isSniperRunning ? 'bg-red-500 hover:bg-red-400 text-white shadow-[0_0_20px_rgba(239,68,68,0.4)]' : 'bg-teal-500 hover:bg-teal-400 text-gray-900 shadow-[0_0_20px_rgba(20,184,166,0.4)]'}`}
                                                >
                                                    {isSniperRunning ? <><Square size={16}/><span>STOP</span></> : <><Play size={16}/><span>LANCER</span></>}
                                                </button>
                                            </div>

                                            <div className="space-y-4 relative z-10">
                                                <div>
                                                    <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Budget Engagé ($) - Min 10$</label>
                                                    <input 
                                                        type="number" step="0.01" min="10" value={sniperBudget}
                                                        onChange={(e) => setSniperBudget(e.target.value)}
                                                        disabled={isSniperRunning}
                                                        className="w-full bg-[#11141b] border border-gray-800 p-3 rounded-xl text-teal-400 font-mono text-lg outline-none focus:border-teal-500 transition-colors placeholder-gray-700 disabled:opacity-50"
                                                    />
                                                </div>
                                                <div>
                                                    <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Multiplicateur Levier</label>
                                                    <select 
                                                        value={sniperLeverage} onChange={(e) => setSniperLeverage(parseInt(e.target.value))}
                                                        disabled={isSniperRunning}
                                                        className="w-full bg-gray-800 border border-gray-700 rounded-xl p-3 text-white font-bold outline-none focus:border-teal-500 disabled:opacity-50"
                                                    >
                                                        <option value="5">x5</option><option value="10">x10</option><option value="20">x20</option><option value="50">x50 (GOD MODE)</option>
                                                    </select>
                                                </div>
                                            </div>
                                        </div>

                                        {/* PANNEAU BOT ÉCLAIR */}
                                        <div className="bg-gray-900/60 p-6 rounded-[30px] border border-orange-500/30 backdrop-blur-xl shadow-inner shadow-black relative overflow-hidden">
                                            <div className="absolute top-0 right-0 w-32 h-32 bg-orange-500/10 rounded-full blur-3xl"></div>
                                            <div className="flex justify-between items-center mb-6 relative z-10">
                                                <div>
                                                    <h4 className="text-lg font-black text-white flex items-center gap-2">
                                                        <div className="w-3 h-3 rounded-full bg-orange-400 shadow-[0_0_10px_rgba(251,146,60,0.8)]"></div>
                                                        Bot Éclair
                                                    </h4>
                                                    <p className="text-xs text-gray-400 font-mono mt-1">Mitraillette Scalping (1 Min)</p>
                                                </div>
                                                <button 
                                                    onClick={() => handleToggleBot('eclair')}
                                                    className={`flex items-center space-x-2 px-5 py-3 rounded-2xl font-black transition-all ${isEclairRunning ? 'bg-red-500 hover:bg-red-400 text-white shadow-[0_0_20px_rgba(239,68,68,0.4)]' : 'bg-orange-500 hover:bg-orange-400 text-gray-900 shadow-[0_0_20px_rgba(249,115,22,0.4)]'}`}
                                                >
                                                    {isEclairRunning ? <><Square size={16}/><span>STOP</span></> : <><Play size={16}/><span>LANCER</span></>}
                                                </button>
                                            </div>

                                            <div className="space-y-4 relative z-10">
                                                <div>
                                                    <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Budget Engagé ($) - Min 10$</label>
                                                    <input 
                                                        type="number" step="0.01" min="10" value={eclairBudget}
                                                        onChange={(e) => setEclairBudget(e.target.value)}
                                                        disabled={isEclairRunning}
                                                        className="w-full bg-[#11141b] border border-gray-800 p-3 rounded-xl text-orange-400 font-mono text-lg outline-none focus:border-orange-500 transition-colors placeholder-gray-700 disabled:opacity-50"
                                                    />
                                                </div>
                                                <div>
                                                    <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Multiplicateur Levier</label>
                                                    <select 
                                                        value={eclairLeverage} onChange={(e) => setEclairLeverage(parseInt(e.target.value))}
                                                        disabled={isEclairRunning}
                                                        className="w-full bg-gray-800 border border-gray-700 rounded-xl p-3 text-white font-bold outline-none focus:border-orange-500 disabled:opacity-50"
                                                    >
                                                        <option value="5">x5</option><option value="10">x10</option><option value="20">x20</option>
                                                    </select>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* COLONNE DROITE : TERMINAL FUSIONNÉ */}
                                    <div className="bg-black/90 rounded-[30px] border border-gray-800 flex flex-col overflow-hidden shadow-[inset_0_0_40px_rgba(0,0,0,1)] h-[550px]">
                                        <div className="bg-[#11141b] p-4 flex items-center justify-between border-b border-gray-800">
                                            <div className="flex items-center space-x-2">
                                                <Terminal size={18} className="text-gray-400"/>
                                                <span className="text-xs font-bold text-gray-300 tracking-widest uppercase">LÉA_PROTECT_TERMINAL</span>
                                            </div>
                                            {/* Filtres du terminal */}
                                            <div className="flex bg-black/50 rounded-lg p-1 border border-gray-800">
                                                <button onClick={() => setLogFilter('ALL')} className={`px-3 py-1 text-[10px] font-black rounded transition-all ${logFilter === 'ALL' ? 'bg-gray-700 text-white' : 'text-gray-500 hover:text-white'}`}>TOUS</button>
                                                <button onClick={() => setLogFilter('SNIPER')} className={`px-3 py-1 text-[10px] font-black rounded transition-all ${logFilter === 'SNIPER' ? 'bg-teal-500 text-black shadow-[0_0_10px_rgba(20,184,166,0.3)]' : 'text-gray-500 hover:text-white'}`}>SNIPER</button>
                                                <button onClick={() => setLogFilter('ECLAIR')} className={`px-3 py-1 text-[10px] font-black rounded transition-all ${logFilter === 'ECLAIR' ? 'bg-orange-500 text-black shadow-[0_0_10px_rgba(249,115,22,0.3)]' : 'text-gray-500 hover:text-white'}`}>ÉCLAIR</button>
                                            </div>
                                        </div>
                                        <div ref={scrollRef} className="flex-1 p-6 font-mono text-[11px] overflow-y-auto space-y-2 scrollbar-thin scrollbar-thumb-gray-800">
                                            {logs.filter(log => logFilter === 'ALL' || log.bot === logFilter || log.bot === 'SYSTÈME').map((log, i) => (
                                                <div key={i} className={`flex items-start gap-3 border-b border-gray-900/50 pb-2
                                                    ${log.text.includes('🔫') || log.text.includes('🚨') ? 'bg-red-500/10 p-2 rounded border-red-500/20' : ''}
                                                    ${log.text.includes('🤑') ? 'bg-green-500/10 p-2 rounded border-green-500/20' : ''}
                                                `}>
                                                    <span className="text-gray-600 flex-shrink-0 mt-0.5">[{log.time}]</span>
                                                    <span className={`flex-shrink-0 font-bold px-1.5 py-0.5 rounded text-[9px] mt-0.5
                                                        ${log.bot === 'SNIPER' ? 'bg-teal-500/20 text-teal-400' : 
                                                          log.bot === 'ECLAIR' ? 'bg-orange-500/20 text-orange-400' : 
                                                          'bg-gray-800 text-gray-400'}`}>
                                                        {log.bot}
                                                    </span>
                                                    <span className={`break-words ${log.text.includes('ERREUR') || log.text.includes('FATAL') ? 'text-red-500 font-bold' : log.text.includes('🤑') ? 'text-green-400 font-bold' : 'text-gray-300'}`}>
                                                        {log.text}
                                                    </span>
                                                </div>
                                            ))}
                                            {(isSniperRunning || isEclairRunning) && <div className="animate-pulse inline-block w-2 h-4 bg-gray-500 ml-1"></div>}
                                        </div>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                )}

                {/* ONGLET MARCHÉ PRO */}
                {activeTab === 'pro' && (
                    <div className="flex flex-col h-full overflow-hidden">
                        
                        {/* 1. LE HUB MARCHÉ (Liste façon Kraken Pro finalisée) */}
                        {!selectedCrypto && (
                            <div className="bg-[#0b0e14] p-6 rounded-[40px] border border-gray-800 h-full flex flex-col shadow-inner shadow-black">
                                
                                {/* 🎯 AJOUT 1 : GRAPHIQUE PAR DÉFAUT (Overview BTC en ligne fluide) */}
                                <div className="bg-[#11141b] p-4 rounded-3xl border border-gray-800 mb-6 flex-shrink-0 shadow-lg">
                                    <h5 className="text-sm font-bold text-gray-500 mb-3 tracking-wider flex items-center gap-2 px-2">
                                        <BarChart3 size={16} className="text-orange-400"/> Aperçu Marché Réel (KRAKEN:BTC/USD)
                                    </h5>
                                    <div className="w-full h-48 rounded-xl overflow-hidden relative border border-gray-800/50">
                                        {/* Widget TradingView Lite pour l'overview rapide */}
                                        <iframe
                                            src="https://s.tradingview.com/widgetembed/?frameElementId=tradingview_b7f0c&symbol=KRAKEN%3ABTCUSD&interval=H&hidesidetoolbar=1&symboledit=1&saveimage=0&toolbarbg=f1f3f6&theme=dark&style=3&timezone=Etc%2FUTC&studies=%5B%5D&locale=fr&utm_source=localhost&utm_medium=widget&utm_campaign=chart&utm_term=KRAKEN%3ABTCUSD"
                                            style={{ width: '100%', height: '100%', border: 'none' }}
                                            title="BTC Overview Chart"
                                        />
                                        <div className="absolute top-2 right-2 bg-black/50 px-2 py-0.5 rounded text-[10px] text-green-400 font-mono">LIVE</div>
                                    </div>
                                </div>

                                {/* En-tête du Hub avec Recherche et Filtres */}
                                <div className="flex justify-between items-center mb-6 px-4 gap-6">
                                    <h4 className="text-2xl font-bold text-white flex items-center gap-3 tracking-wide flex-shrink-0">
                                        <TrendingUp className="text-teal-500" /> Marchés Spot
                                    </h4>

                                    {/* 🎯 AJOUT 2 : BARRE DE RECHERCHE DYNAMIQUE */}
                                    <div className="relative flex-1 max-w-xl">
                                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-600" size={18} />
                                        <input
                                            type="text"
                                            value={searchTerm}
                                            onChange={(e) => setSearchTerm(e.target.value.toUpperCase())} // Force majuscule pour le symbole
                                            placeholder="Rechercher une crypto (ex: SOL, DOGE...)"
                                            className="w-full pl-12 pr-4 py-3 bg-[#181d26] border border-gray-800 rounded-xl text-white font-mono text-sm outline-none focus:border-teal-500 transition shadow-inner shadow-black"
                                        />
                                        {searchTerm && (
                                            <button onClick={() => setSearchTerm('')} className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-600 hover:text-white text-xs">✕</button>
                                        )}
                                    </div>

                                    {/* Boutons Tous/Favoris (déjà là) */}
                                    <div className="flex bg-[#181d26] p-1 rounded-xl border border-gray-800 flex-shrink-0 shadow-inner shadow-black">
                                        <button onClick={() => setShowOnlyFavorites(false)} className={`px-5 py-2 rounded-lg text-xs font-black tracking-wider transition-all ${!showOnlyFavorites ? 'bg-teal-500 text-black shadow-md' : 'text-gray-400 hover:text-white'}`}>TOUTES</button>
                                        <button onClick={() => setShowOnlyFavorites(true)} className={`px-5 py-2 rounded-lg text-xs font-black tracking-wider transition-all flex items-center gap-2 ${showOnlyFavorites ? 'bg-teal-500 text-black shadow-md' : 'text-gray-400 hover:text-white'}`}>
                                            ★ FAVORIS
                                        </button>
                                    </div>
                                </div>
                                
                                {/* EN-TÊTE DU TABLEAU */}
                                <div className="grid grid-cols-4 gap-4 px-8 py-3 text-xs font-bold text-gray-500 uppercase border-b border-gray-800/50 mb-2">
                                    <div>Marché</div>
                                    <div className="text-right">Prix (USD)</div>
                                    <div className="text-right">Variation 24h</div>
                                    <div className="text-right">Volume 24h</div>
                                </div>

                                {/* LISTE DES CRYPTOS AVEC DOUBLE FILTRAGE (Favoris + Recherche) */}
                                <div className="flex-1 overflow-y-auto space-y-1 scrollbar-thin scrollbar-thumb-gray-800 scrollbar-track-transparent pr-2 min-h-0">
                                    {marketData
                                        // 1. Filtrage par Favoris
                                        .filter(c => showOnlyFavorites ? favorites.includes(c.symbol) : true)
                                        // 🎯 2. AJOUT 3 : Filtrage par Recherche
                                        .filter(c => searchTerm ? c.symbol.includes(searchTerm) : true)
                                        .map((crypto, idx) => (
                                        <div 
                                            key={idx} 
                                            onClick={() => setSelectedCrypto(crypto.symbol)}
                                            className="grid grid-cols-4 gap-4 px-8 py-4 items-center bg-transparent hover:bg-[#181d26] border border-transparent hover:border-gray-700/50 rounded-2xl cursor-pointer transition-all group"
                                        >
                                            <div className="flex items-center gap-4">
                                                <button 
                                                    onClick={(e) => toggleFavorite(crypto.symbol, e)}
                                                    className={`text-xl transition-all ${favorites.includes(crypto.symbol) ? 'text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.8)]' : 'text-gray-700 group-hover:text-gray-400'}`}
                                                >★</button>
                                                <span className="font-bold text-gray-200 text-lg group-hover:text-white">{crypto.symbol}</span>
                                            </div>
                                            <div className="text-right font-mono text-gray-300 group-hover:text-white">${(crypto.price || 0).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 6})}</div>
                                            <div className={`text-right font-bold ${crypto.change24h >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                                                {crypto.change24h >= 0 ? '+' : ''}{crypto.change24h.toFixed(2)}%
                                            </div>
                                            <div className="text-right font-mono text-gray-500 group-hover:text-gray-400">${(crypto.volume * crypto.price).toLocaleString(undefined, {maximumFractionDigits: 0})}</div>
                                        </div>
                                    ))}
                                    
                                    {/* Gestion si aucun résultat */}
                                    {marketData.length > 0 && marketData.filter(c => showOnlyFavorites ? favorites.includes(c.symbol) : true).filter(c => searchTerm ? c.symbol.includes(searchTerm) : true).length === 0 && (
                                        <div className="flex flex-col items-center justify-center h-40 text-gray-700 border-2 border-dashed border-gray-800 rounded-3xl">
                                            <Search size={40} className="mb-3 text-gray-800" />
                                            <p className="tracking-widest text-xs uppercase">Aucune crypto trouvée pour "{searchTerm}"</p>
                                        </div>
                                    )}

                                    {/* Chargement initial */}
                                    {marketData.length === 0 && (
                                        <div className="flex flex-col items-center justify-center h-40 text-gray-500 animate-pulse space-y-4">
                                            <div className="w-8 h-8 border-4 border-teal-500 border-t-transparent rounded-full animate-spin"></div>
                                            <p className="tracking-widest text-xs uppercase">Connexion au réseau Kraken Spot...</p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* 2. LE TERMINAL DE TRADING PRO COMPLET */}
                        {selectedCrypto && (
                            <div className="flex flex-col h-full bg-[#0b0e14] rounded-[40px] border border-gray-800 overflow-hidden shadow-inner shadow-black relative">
                                
                                {/* Header du Terminal */}
                                <div className="flex items-center justify-between px-6 py-4 bg-[#11141b] border-b border-gray-800/50">
                                    <div className="flex items-center gap-6">
                                        <button onClick={() => setSelectedCrypto(null)} className="text-gray-500 hover:text-white font-bold flex items-center gap-2 transition-colors px-4 py-2 bg-[#181d26] rounded-xl border border-gray-800">
                                            ← MARCHÉS
                                        </button>
                                        <h2 className="text-2xl font-black text-white flex items-center gap-2">
                                            {selectedCrypto}<span className="text-gray-600 text-sm">/USD</span>
                                        </h2>
                                    </div>
                                    <div className="text-gray-500 text-sm font-mono flex gap-4">
                                        <span>Terminal Sécurisé</span>
                                        <span className="text-green-500 flex items-center gap-1"><div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div> KRAKEN LIVE</span>
                                    </div>
                                </div>

                                {/* Zone des 3 Colonnes */}
                                <div className="flex flex-1 overflow-hidden">
                                    
                                    {/* COLONNE GAUCHE : LE VRAI MOTEUR TRADINGVIEW */}
                                    {/* J'utilise l'outil avancé officiel pour avoir nativement les zooms, les bougies et les temps (1m, 15m, 1h...) */}
                                    <div className="flex-1 flex flex-col border-r border-gray-800/50 relative">
                                        <iframe
                                            src={`https://s.tradingview.com/widgetembed/?frameElementId=tradingview_chart&symbol=COINBASE%3A${selectedCrypto}USD&interval=20&hidesidetoolbar=0&symboledit=1&saveimage=0&toolbarbg=11141b&theme=dark&style=1&timezone=Etc%2FUTC&studies=%5B%5D&locale=fr`}
                                            style={{ width: '100%', height: '100%', border: 'none' }}
                                            title="Crypto Chart Advanced"
                                        />
                                    </div>

                                    {/* COLONNE MILIEU : CARNET D'ORDRES (Order Book) */}
                                    <div className="w-64 bg-[#0b0e14] border-r border-gray-800/50 flex flex-col hidden lg:flex">
                                        <div className="flex justify-between items-center p-3 border-b border-gray-800/50 bg-[#11141b]">
                                            <h3 className="text-xs font-bold text-gray-500 uppercase tracking-widest">Carnet d'ordres</h3>
                                        </div>
                                        <div className="grid grid-cols-2 px-3 py-2 text-[10px] text-gray-600 uppercase font-bold border-b border-gray-800/50">
                                            <div>Prix (USD)</div>
                                            <div className="text-right">Montant</div>
                                        </div>
                                        <div className="flex-1 overflow-y-auto p-1 space-y-[2px] scrollbar-none">
                                            {/* Flux Vendeurs (Rouge) */}
                                            {[...Array(12)].map((_, i) => <div key={`ask-${i}`} className="flex justify-between text-xs font-mono px-2 py-0.5 hover:bg-gray-800/30 cursor-pointer"><span className="text-red-500/90">{(Math.random() * 1000 + 50000).toFixed(1)}</span><span className="text-gray-400">{(Math.random() * 2).toFixed(4)}</span></div>)}
                                            
                                            {/* Séparateur / Prix Actuel */}
                                            <div className="text-center text-lg font-black text-white my-2 border-y border-gray-800/80 py-2 bg-[#11141b]">
                                                --- USD
                                            </div>
                                            
                                            {/* Flux Acheteurs (Vert) */}
                                            {[...Array(12)].map((_, i) => <div key={`bid-${i}`} className="flex justify-between text-xs font-mono px-2 py-0.5 hover:bg-gray-800/30 cursor-pointer"><span className="text-green-500/90">{(Math.random() * 1000 + 49000).toFixed(1)}</span><span className="text-gray-400">{(Math.random() * 2).toFixed(4)}</span></div>)}
                                        </div>
                                    </div>

                                    {/* COLONNE DROITE : LE PANNEAU ACHAT / VENTE PRO */}
                                    <div className="w-[340px] bg-[#0b0e14] flex flex-col p-5">
                                        {/* Aiguillage Achat/Vente */}
                                        <div className="flex bg-[#11141b] p-1 rounded-xl mb-6 border border-gray-800/80 shadow-inner">
                                            <button onClick={() => setOrderSide('buy')} className={`flex-1 py-3 rounded-lg text-sm font-black tracking-wider transition-all ${orderSide === 'buy' ? 'bg-green-600 text-white shadow-md shadow-green-900/20' : 'text-gray-500 hover:text-white'}`}>ACHETER</button>
                                            <button onClick={() => setOrderSide('sell')} className={`flex-1 py-3 rounded-lg text-sm font-black tracking-wider transition-all ${orderSide === 'sell' ? 'bg-red-600 text-white shadow-md shadow-red-900/20' : 'text-gray-500 hover:text-white'}`}>VENDRE</button>
                                        </div>
                                        
                                        {/* Type d'ordre (Marché/Limite) */}
                                        <div className="flex gap-4 mb-6 border-b border-gray-800/80 pb-3 px-1">
                                            <button onClick={() => setOrderType('market')} className={`text-xs font-bold uppercase tracking-widest transition-all ${orderType === 'market' ? (orderSide === 'buy' ? 'text-green-500' : 'text-red-500') : 'text-gray-600 hover:text-gray-300'}`}>Au Marché</button>
                                            <button onClick={() => setOrderType('limit')} className={`text-xs font-bold uppercase tracking-widest transition-all ${orderType === 'limit' ? (orderSide === 'buy' ? 'text-green-500' : 'text-red-500') : 'text-gray-600 hover:text-gray-300'}`}>Limite</button>
                                        </div>

                                        {/* Formulaire */}
                                        <div className="space-y-5 flex-1">
                                            {orderType === 'limit' && (
                                                <div>
                                                    <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 block">Prix cible (USD)</label>
                                                    <div className="relative">
                                                        <input type="number" value={orderPrice} onChange={(e) => setOrderPrice(e.target.value)} className="w-full bg-[#11141b] border border-gray-800/80 p-4 rounded-xl text-white font-mono text-lg outline-none focus:border-teal-500 transition-colors placeholder-gray-700" style={{ appearance: 'none', WebkitAppearance: 'none', MozAppearance: 'textfield' }} placeholder="0.00" />
                                                        <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-600 font-bold">USD</span>
                                                    </div>
                                                </div>
                                            )}
                                            
                                            <div>
                                                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2 flex justify-between">
                                                 <span>Montant ({selectedCrypto})</span>
                                                 <span className="text-teal-500 font-mono">Dispo: {portfolio.usdt.toFixed(2)} USDT</span>
                                                </label>
                                                <div className="relative">
                                                    {/* Champ propre, fini les flèches dégueulasses */}
                                                    <input type="number" value={orderAmount} onChange={(e) => setOrderAmount(e.target.value)} className="w-full bg-[#11141b] border border-gray-800/80 p-4 rounded-xl text-white font-mono text-lg outline-none focus:border-teal-500 transition-colors placeholder-gray-700 m-0" style={{ appearance: 'none', WebkitAppearance: 'none', MozAppearance: 'textfield' }} placeholder="0.00" />
                                                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-600 font-bold">{selectedCrypto}</span>
                                                </div>
                                            </div>
                                            
                                            {/* Boutons de raccourcis (25%, 50%...) */}
                                            <div className="flex gap-2">
                                                {['25%', '50%', '75%', '100%'].map(pct => (
                                                    <button key={pct} className="flex-1 bg-[#11141b] border border-gray-800 hover:border-gray-500 text-gray-400 hover:text-white py-2 rounded-lg text-xs font-mono font-bold transition-all">{pct}</button>
                                                ))}
                                            </div>
                                        </div>

                                        {/* Gros Bouton d'Action */}
                                        <button 
                                          onClick={executeTrade} // <--- AJOUTE ÇA ICI
                                          className={`w-full py-5 rounded-2xl font-black text-white text-lg tracking-widest uppercase transition-transform active:scale-95 mt-4 ${orderSide === 'buy' ? 'bg-green-600 hover:bg-green-500 shadow-[0_0_20px_rgba(22,163,74,0.3)]' : 'bg-red-600 hover:bg-red-500 shadow-[0_0_20px_rgba(220,38,38,0.3)]'}`}
                                        >
                                           {orderSide === 'buy' ? `Acheter ${selectedCrypto}` : `Vendre ${selectedCrypto}`}
                                        </button>
                                    </div>

                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>

    );
};