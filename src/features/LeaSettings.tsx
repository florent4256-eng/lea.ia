import React, { useState, useEffect, useRef } from 'react';
import { User, Shield, CreditCard, Globe, Save, Key, Mail, Check, Volume2, Heart, Bot, X, Phone, Mic, MessageSquare, Sparkles, RefreshCw, Brain, Trash2, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useConfirmToast } from '../hooks/useConfirmToast';

export const LeaSettings = ({ onClose }: { onClose?: () => void }) => {
  const { t, i18n } = useTranslation();
  const [activeTab, setActiveTab] = useState('voice'); // On met l'onglet Voix par défaut pour que tu puisses tester
  const [confirmLang, setConfirmLang] = useState<string | null>(null);
  const currentUser = localStorage.getItem('lea_currentUser') || 'Invité';
  const userNom = localStorage.getItem('lea_nom') || '';
  const userPrenom = localStorage.getItem('lea_prenom') || '';
  const userEmail = localStorage.getItem('lea_email') || '';
  const userDob = localStorage.getItem('lea_dob') || '';

  // --- ÉTATS POUR INSTRUCTIONS LÉA (LE CERVEAU JARVIS) ---
  const [isCustomEnabled, setIsCustomEnabled] = useState(false);
  const [instructionsBlocks, setInstructionsBlocks] = useState<string[]>([]);
  const [isInstructionModalOpen, setIsInstructionModalOpen] = useState(false);
  const [newInstructionText, setNewInstructionText] = useState('');
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  
  // --- NOUVEAUTÉ : ÉTAT DE LA VOIX ---
  const [activeVoice, setActiveVoice] = useState('fr_FR-siwis-low.onnx')

  // --- ÉTATS POUR LE TEST VOCAL ---
  const [isTesting, setIsTesting] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // --- ÉTATS POUR BIXBY CONFIG ---
  const [bixbyNomSaved, setBixbyNomSaved] = useState(false);
  const [bixbyNewMemory, setBixbyNewMemory] = useState('');
  const [bixbyMemSaved, setBixbyMemSaved] = useState(false);

  // --- ÉTATS POUR LA SÉCURITÉ ---
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwdStatus, setPwdStatus] = useState<{type: 'success' | 'error', msg: string} | null>(null);
const [billingCycle, setBillingCycle] = useState<'monthly' | 'yearly'>('monthly');

  // --- MODAL DE CONFIRMATION + TOAST (remplace les alert()/window.confirm() natifs moches) ---
  const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();

// --- ÉTATS POUR L'AUTO-DESTRUCTION ---
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deletePassword, setDeletePassword] = useState('');
  const [deleteError, setDeleteError] = useState('');

// --- ÉTATS POUR LE PAIEMENT CRYPTO SOUVERAIN ---
  const [subLoading, setSubLoading] = useState(false);
  const [selectedNetwork, setSelectedNetwork] = useState('ethereum');
  const [txHash, setTxHash] = useState('');
  const [cryptoStatus, setCryptoStatus] = useState<{type: 'success' | 'error', msg: string} | null>(null);

  const cryptoWallets = {
    ethereum: "0x24F01156Df3afA765b2200be229eA1EfD2692E22",
    tron: "TUuKD2Ny15ZGrXH5v218KSGWGjzJpKVASe",
    solana: "5ZtweLRrzFjKbq1ucFsu356z8H6W6cw5L756LsEEHFSs",
    optimism: "0x24F01156Df3afA765b2200be229eA1EfD2692E22",
    ton: "UQCLYPgw3yzMTEcccNvVbJRrxzqACCFQcAFVhKVdCFYs1BVF"
  };

// --- ÉTATS POUR LE PROFIL ---
  const [profileData, setProfileData] = useState({
    nom: '',
    prenom: '',
    dateNaissance: '',
    email: '',
    abonnement: 'free',
    nextBillingDate: '',
    cancelAtPeriodEnd: false,
    isTrainingContributor: true,
    leaBixbyNom: '',
  });

  // Au chargement des paramètres, on va demander au bunker la voix actuelle de l'utilisateur
  useEffect(() => {
    fetch(`/api/user/profile/${currentUser}`)
      .then(res => res.json())
      .then(data => {
        if (data.voix) setActiveVoice(data.voix);
        if (data.langue && data.langue !== i18n.language) {
          i18n.changeLanguage(data.langue);
        }
        // --- NOUVEAUTÉ : ON REMPLIT LES CASES DU COMPTE ET L'ABONNEMENT ---
        setProfileData({
          nom: data.nom || '',
          prenom: data.prenom || '',
          dateNaissance: data.dateNaissance || '',
          email: data.email || '',
          abonnement: data.abonnement || 'free',
          nextBillingDate: data.nextBillingDate || '',
          cancelAtPeriodEnd: data.cancelAtPeriodEnd || false,
          isTrainingContributor: data.isTrainingContributor !== false,
          leaBixbyNom: data.leaBixbyNom || '',
        });
      })
      .catch(err => console.error("Erreur récupération profil:", err));
  }, [currentUser, i18n]);

  const tabs = [
    { id: 'account', label: 'Mon Compte', icon: <User size={18} /> },
    { id: 'security', label: 'Sécurité & Accès', icon: <Shield size={18} /> },
    { id: 'instructions', label: 'Instructions pour Léa', icon: <Bot size={18} /> },
    { id: 'subscription', label: 'Mon Abonnement', icon: <CreditCard size={18} /> },
    { id: 'language', label: 'Langue & Région', icon: <Globe size={18} /> },
    { id: 'voice', label: "Voix de l'Assistante", icon: <Volume2 size={18} /> },
    { id: 'bixby', label: 'Config LeaNova', icon: <Mic size={18} /> },
  ];

  const languages = [
    { id: 'fr', name: 'Français', region: 'France', dir: 'ltr' },
    { id: 'en', name: 'English', region: 'United States', dir: 'ltr' },
    { id: 'es', name: 'Español', region: 'España', dir: 'ltr' },
    { id: 'pt', name: 'Português', region: 'Portugal / Brasil', dir: 'ltr' },
    { id: 'ar', name: 'العربية', region: 'World', dir: 'rtl' },
    { id: 'de', name: 'Deutsch', region: 'Deutschland', dir: 'ltr' },
    { id: 'it', name: 'Italiano', region: 'Italia', dir: 'ltr' },
    { id: 'lb', name: 'Lëtzebuergesch', region: 'Luxembourg', dir: 'ltr' },
    { id: 'zh', name: '中文', region: '中国', dir: 'ltr' }
  ];

  // --- LES VOIX DISPONIBLES ---
  const availableVoices = [
    { id: 'fr_FR-siwis-low.onnx', name: 'Léa (Féminin)', desc: 'Voix douce, fluide et naturelle.' },
    { id: 'fr_FR-gilles-low.onnx', name: 'Léo (Masculin)', desc: 'Voix grave, posée et professionnelle.' }
  ];

  // --- FONCTION POUR CHANGER ET SAUVEGARDER LA VOIX ---
  const changeVoice = async (voiceId: string) => {
    setActiveVoice(voiceId); // Met à jour l'interface en bleu immédiatement
    try {
      // Sauvegarde dans le coffre de l'utilisateur
      await fetch('/api/user/update-voice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: currentUser, voix: voiceId })
      });
    } catch (err) {
      console.error("Erreur lors de la sauvegarde de la voix:", err);
    }
  };

  // --- FONCTION DE TEST VOCAL EN DIRECT ---
  const testVoice = async (voiceId: string) => {
    if (isTesting) return;
    setIsTesting(true);
    
    // On synchronise la voix choisie avant de la faire parler
    await changeVoice(voiceId);
    
    const testText = voiceId.includes('gilles') || voiceId.includes('remi') 
      ? "Bonjour mon chéri, je suis Léo. Mon système vocal est opérationnel." 
      : "Bonjour mon chéri, je suis Léa. Mon système vocal est opérationnel.";

    try {
      const response = await fetch('/api/voice/speak', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: testText, username: currentUser })
      });
      
      const data = await response.json();
      if (data.success && data.audio) {
        if (audioRef.current) audioRef.current.pause();
        const audio = new Audio(data.audio);
        audioRef.current = audio;
        audio.onended = () => setIsTesting(false);
        await audio.play();
      } else {
        setIsTesting(false);
      }
    } catch (e) {
      console.error("Erreur test vocal", e);
      setIsTesting(false);
    }
  };

  // --- LA FONCTION RADICALE POUR LA LANGUE ---
  const changeLanguage = async (langId: string) => {
    try {
      // 1. On informe le serveur pour sauvegarder la langue dans le profil (Le tien ou celui de ton pote)
      await fetch(`/api/user/update-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          username: currentUser, 
          langue: langId 
        })
      });

      // 2. On met à jour l'interface locale
      localStorage.setItem('i18nextLng', langId);
      
      // On force la langue avec i18n AVANT de recharger la page
      await i18n.changeLanguage(langId);
      
      setConfirmLang(null);
      window.location.reload(); 
    } catch (err) {
      console.error("Erreur sauvegarde langue:", err);
    }
  };

  // --- LA FONCTION DE MISE À JOUR DU MOT DE PASSE ---
  const handlePasswordUpdate = async () => {
    if (!newPassword) {
      setPwdStatus({ type: 'error', msg: "Le nouveau mot de passe ne peut pas être vide." });
      return;
    }
    
    try {
      const response = await fetch(`/api/user/update-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          username: currentUser, 
          oldPassword: oldPassword,
          newPassword: newPassword 
        })
      });
      
      const data = await response.json();
      
      if (data.success) {
        setPwdStatus({ type: 'success', msg: "Modification réussie et sauvegardée !" });
        setOldPassword('');
        setNewPassword('');
        setTimeout(() => setPwdStatus(null), 4000); // Fait disparaître le message après 4s
      } else {
        setPwdStatus({ type: 'error', msg: data.error || "Erreur de modification." });
      }
    } catch (err) {
      setPwdStatus({ type: 'error', msg: "Erreur de communication avec le serveur Master." });
    }
  };

  // --- LA FONCTION D'AUTO-DESTRUCTION DU PROFIL ---
  const handleDeleteProfile = async () => {
    if (!deletePassword) {
      setDeleteError("Le mot de passe est requis pour déclencher l'auto-destruction.");
      return;
    }
    
    try {
      const response = await fetch(`/api/user/delete-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          username: currentUser, 
          password: deletePassword 
        })
      });
      
      const data = await response.json();
      
      if (data.success) {
        showToast("💥 " + data.message);
        // On pulvérise la mémoire locale du navigateur
        localStorage.removeItem('lea_currentUser');
        localStorage.removeItem('lea_user_' + currentUser.toLowerCase());
        // On redirige vers la page de connexion (léger délai pour laisser voir le toast)
        setTimeout(() => { window.location.href = '/'; }, 1200);
      } else {
        setDeleteError(data.error || "Échec de l'auto-destruction.");
      }
    } catch (err) {
      setDeleteError("Erreur critique de communication avec la Tour Master.");
    }
  };

  // --- LES FONCTIONS DE GESTION DE L'ABONNEMENT ---
  const handleUpgrade = async (plan: string) => {
    const price = billingCycle === 'monthly' ? "19.99€" : "199.99€";
    askConfirm(`Confirmer l'achat de ${plan} (${billingCycle}) pour ${price} ?`, async () => {
      try {
        await fetch(`/api/subscription/upgrade`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: currentUser,
            planType: plan,
            billingCycle: billingCycle
          })
        });
        window.location.reload();
      } catch (e) {
        console.error("Erreur lors de l'upgrade :", e);
      }
    });
  };

  const handleCancelSubscription = async () => {
    askConfirm("Voulez-vous vraiment résilier ? Vous garderez vos accès jusqu'à la fin de la période actuelle.", async () => {
      try {
        await fetch(`/api/subscription/cancel`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: currentUser })
        });
        window.location.reload();
      } catch (e) {
        console.error("Erreur lors de la résiliation :", e);
      }
    });
  };

  // --- LA FONCTION DE VÉRIFICATION WEB3 ---
  const handleCryptoPayment = async () => {
    if (!txHash) {
      setCryptoStatus({ type: 'error', msg: "Veuillez coller l'ID de transaction (TxHash)." });
      return;
    }
    
    setSubLoading(true);
    try {
      const response = await fetch(`/api/billing/crypto`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          username: currentUser, 
          network: selectedNetwork,
          txHash: txHash,
          planType: billingCycle === 'monthly' ? 'Léa AI Plus' : 'Léa AI Plus Annual'
        })
      });
      
      const data = await response.json();
      if (data.success) {
        setCryptoStatus({ type: 'success', msg: "Paiement en cours de vérification sur la blockchain..." });
        setTimeout(() => window.location.reload(), 3000);
      } else {
        setCryptoStatus({ type: 'error', msg: data.error });
      }
    } catch (e) {
      setCryptoStatus({ type: 'error', msg: "Erreur de connexion au serveur Master." });
    } finally {
      setSubLoading(false);
    }
  };

  // --- LE GRAND REGISTRE DYNAMIQUE DES VOIX ---
  const currentLangKey = (localStorage.getItem('i18nextLng') || 'fr').substring(0, 2);

  const voicesRegistry: Record<string, {id: string, name: string, desc: string, gender: string}[]> = {
    fr: [
      { id: 'fr_FR-siwis-low.onnx', name: 'Léa Classique', desc: 'Féminin - Douce & naturelle', gender: 'female' },
      { id: 'fr_FR-upmc-medium.onnx', name: 'Léa Éloquente', desc: 'Féminin - Claire & humaine', gender: 'female' },
      { id: 'fr_FR-upmc-medium(1).onnx', name: 'Léa Éloquente (Alt)', desc: 'Féminin - Variante alternative', gender: 'female' },
      { id: 'fr_FR-gilles-low.onnx', name: 'Léo Standard', desc: 'Masculin - Sérieux & posé', gender: 'male' },
      { id: 'fr_FR-tom-medium.onnx', name: 'Léo Tom', desc: 'Masculin - Dynamique', gender: 'male' },
      { id: 'fr_FR-mls-medium.onnx', name: 'Voix MLS 1', desc: 'Standard Mixte', gender: 'male' },
      { id: 'fr_FR-mls-medium(1).onnx', name: 'Voix MLS 2', desc: 'Standard Mixte', gender: 'female' },
      { id: 'fr_FR-mls-medium(2).onnx', name: 'Voix MLS 3', desc: 'Standard Mixte', gender: 'female' }
    ],
    en: [
      { id: 'en_GB-cori-high.onnx', name: 'Cori (High)', desc: 'Féminin - Accent British HQ', gender: 'female' },
      { id: 'en_GB-jenny_dioco-medium.onnx', name: 'Jenny Dioco', desc: 'Féminin - Standard', gender: 'female' },
      { id: 'en_GB-semaine-medium.onnx', name: 'Semaine (Base)', desc: 'Féminin - Ton neutre', gender: 'female' },
      { id: 'en_GB-semaine-medium(1).onnx', name: 'Semaine (Alt 1)', desc: 'Variante 1', gender: 'female' },
      { id: 'en_GB-semaine-medium(2).onnx', name: 'Semaine (Alt 2)', desc: 'Variante 2', gender: 'female' },
      { id: 'en_GB-semaine-medium(3).onnx', name: 'Semaine (Alt 3)', desc: 'Variante 3', gender: 'female' },
      { id: 'en_GB-northern_english_male-medium.onnx', name: 'Northern English', desc: 'Masculin - Accent du Nord', gender: 'male' }
    ],
    es: [
      { id: 'es_ES-sharvard-medium.onnx', name: 'Sharvard', desc: 'Standard Masculin', gender: 'male' },
      { id: 'es_ES-carlfm-x_low.onnx', name: 'Carl FM', desc: 'Masculin - Très rapide', gender: 'male' },
      { id: 'es_ES-mls_9972-low.onnx', name: 'MLS 9972', desc: 'Variante Locale', gender: 'female' },
      { id: 'es_ES-mls_10246-low.onnx', name: 'MLS 10246', desc: 'Variante Locale', gender: 'female' }
    ],
    de: [
      { id: 'de_DE-kerstin-low.onnx', name: 'Kerstin', desc: 'Féminin - Standard', gender: 'female' },
      { id: 'de_DE-ramona-low.onnx', name: 'Ramona', desc: 'Féminin - Standard', gender: 'female' },
      { id: 'de_DE-karlsson-low.onnx', name: 'Karlsson', desc: 'Masculin - Standard', gender: 'male' },
      { id: 'de_DE-thorsten_emotional-medium.onnx', name: 'Thorsten Émotionnel', desc: 'Masculin - Expressif', gender: 'male' }
    ],
    it: [
      { id: 'it_IT-paola-medium.onnx', name: 'Paola', desc: 'Féminin - Standard', gender: 'female' },
      { id: 'it_IT-riccardo-x_low.onnx', name: 'Riccardo', desc: 'Masculin - Rapide', gender: 'male' }
    ],
    pt: [
      { id: 'pt_PT-tugão-medium.onnx', name: 'Tugão', desc: 'Masculin - Accent Portugal', gender: 'male' }
    ],
    zh: [
      { id: 'zh_CN-xiao_ya-medium.onnx', name: 'Xiao Ya', desc: 'Féminin - Standard', gender: 'female' },
      { id: 'zh_CN-huayan-medium.onnx', name: 'Huayan', desc: 'Féminin - Standard', gender: 'female' },
      { id: 'zh_CN-chaowen-medium.onnx', name: 'Chaowen', desc: 'Masculin - Standard', gender: 'male' }
    ],
    ar: [
      { id: 'ar_JO-kareem-medium.onnx', name: 'Kareem', desc: 'Masculin - Standard', gender: 'male' }
    ],
    lb: [
      { id: 'lb_LU-marylux-medium.onnx', name: 'Marylux', desc: 'Féminin - Luxembourgeois', gender: 'female' }
    ]
  };

  const voicesToDisplay = voicesRegistry[currentLangKey] || voicesRegistry['fr'];

const handleProfileUpdate = async () => {
    try {
      await fetch(`/api/user/update-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: currentUser,
          ...profileData
        })
      });
      showToast("Identité sauvegardée dans le bunker !");
    } catch (err) {
      console.error("Erreur sauvegarde profil:", err);
    }
  };

  const handleBixbyNomSave = async () => {
    try {
      await fetch(`/api/user/update-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: currentUser, leaBixbyNom: profileData.leaBixbyNom })
      });
      setBixbyNomSaved(true);
      setTimeout(() => setBixbyNomSaved(false), 3000);
    } catch (err) {
      console.error("Erreur sauvegarde nom Bixby:", err);
    }
  };

  // --- LOGIQUE SOUVERAINE DES INSTRUCTIONS POUR LÉA ---
  useEffect(() => {
    fetch(`/api/instructions/${currentUser}`)
      .then(res => res.json())
      .then(data => {
        setIsCustomEnabled(data.enabled || false);
        setInstructionsBlocks(data.blocks || []);
      })
      .catch(err => console.error("Erreur récupération instructions:", err));
  }, [currentUser]);

  const saveInstructionsToBunker = async (enabled: boolean, blocks: string[]) => {
    try {
      await fetch(`/api/instructions/update`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: currentUser, enabled, blocks })
      });
    } catch (err) {
      console.error("Erreur sauvegarde instructions:", err);
    }
  };

  const handleToggleInstructions = () => {
    const newState = !isCustomEnabled;
    setIsCustomEnabled(newState);
    saveInstructionsToBunker(newState, instructionsBlocks);
  };

  const handleSaveInstruction = () => {
    if (!newInstructionText.trim()) return;
    let newBlocks = [...instructionsBlocks];
    if (editingIndex !== null) {
      newBlocks[editingIndex] = newInstructionText.trim();
    } else {
      newBlocks.push(newInstructionText.trim());
    }
    setInstructionsBlocks(newBlocks);
    saveInstructionsToBunker(isCustomEnabled, newBlocks);
    setIsInstructionModalOpen(false);
    setNewInstructionText('');
    setEditingIndex(null);
  };

  const handleDeleteInstruction = (index: number) => {
    const newBlocks = instructionsBlocks.filter((_, i) => i !== index);
    setInstructionsBlocks(newBlocks);
    saveInstructionsToBunker(isCustomEnabled, newBlocks);
  };

  const handleClearAllInstructions = () => {
    askConfirm("Es-tu sûr de vouloir effacer toute la mémoire contextuelle de Léa ?", () => {
      setInstructionsBlocks([]);
      saveInstructionsToBunker(isCustomEnabled, []);
    });
  };

  const handleAddBixbyMemory = () => {
    if (!bixbyNewMemory.trim()) return;
    const newBlocks = [...instructionsBlocks, bixbyNewMemory.trim()];
    setInstructionsBlocks(newBlocks);
    saveInstructionsToBunker(true, newBlocks);
    setBixbyNewMemory('');
    setBixbyMemSaved(true);
    setTimeout(() => setBixbyMemSaved(false), 3000);
  };

  const handleDeleteBixbyMemory = (index: number) => {
    const newBlocks = instructionsBlocks.filter((_, i) => i !== index);
    setInstructionsBlocks(newBlocks);
    saveInstructionsToBunker(isCustomEnabled, newBlocks);
  };

  return (
    <div className="w-full h-full flex flex-col md:flex-row overflow-hidden bg-transparent text-white p-4 md:p-10 animate-in fade-in duration-500 relative">
      
      {/* BOUTON FERMER TRANSLUCIDE (X) */}
      {onClose && (
        <button 
          onClick={onClose}
          className="absolute top-6 right-6 z-[110] p-2 bg-black/10 hover:bg-black/30 backdrop-blur-sm rounded-xl text-white/40 hover:text-white transition-all cursor-pointer"
          title="Fermer les paramètres"
        >
          <X size={24} />
        </button>
      )}

      {/* FENÊTRE DE CONFIRMATION (MODAL LANGUE) */}
      {confirmLang && (
        <div className="absolute inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-md animate-in fade-in">
          <div className="bg-[#000b1e] border border-[#00f2ff]/30 p-8 rounded-[2.5rem] max-w-sm w-full shadow-[0_0_50px_rgba(0,242,255,0.2)] text-center">
            <div className="w-16 h-16 bg-[#00f2ff]/10 rounded-full flex items-center justify-center mx-auto mb-6 text-[#00f2ff]">
              <Globe size={32} />
            </div>
            <h3 className="text-xl font-bold mb-2">{t('modal_confirm_title', 'Changer la langue ?')}</h3>
            <p className="text-slate-400 text-sm mb-8">
              {t('modal_confirm_desc', "Voulez-vous confirmer le changement de langue vers :")} <br/><br/>
              <span className="text-white font-bold text-lg px-4 py-2 bg-white/5 rounded-lg border border-white/10">
                {languages.find(l => l.id === confirmLang)?.name}
              </span>
            </p>
            <div className="flex gap-4">
              <button 
                onClick={() => setConfirmLang(null)}
                className="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold transition-all"
              >
                {t('btn_cancel', 'Annuler')}
              </button>
              <button 
                onClick={() => changeLanguage(confirmLang)}
                className="flex-1 py-3 rounded-xl bg-[#00f2ff] hover:bg-white text-[#000814] font-bold transition-all shadow-[0_0_20px_rgba(0,242,255,0.4)]"
              >
                {t('btn_confirm', 'Confirmer')}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmToastHost />

      {/* MENU : ADAPTATIF MOBILE (HAUT) / DESKTOP (GAUCHE) */}
<div className="w-full md:w-64 flex md:flex-col gap-2 pb-4 md:pr-8 border-b md:border-b-0 md:border-r border-white/10 shrink-0 overflow-x-auto md:overflow-x-visible no-scrollbar">
  <h2 className="hidden md:block text-2xl font-black text-transparent bg-clip-text bg-gradient-to-r from-white to-slate-400 mb-8 tracking-tighter">
    Configuration
  </h2>
        
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium text-sm ${
              activeTab === tab.id 
              ? 'bg-gradient-to-r from-[#0047ff]/20 to-[#00f2ff]/10 text-[#00f2ff] border border-[#00f2ff]/30 shadow-[0_0_15px_rgba(0,242,255,0.1)]' 
              : 'text-slate-400 hover:bg-white/5 hover:text-white border border-transparent'
            }`}
          >
            {tab.icon}
            {t(`tab_${tab.id}`, tab.label)}
          </button>
        ))}
      </div>

      {/* ZONE DE CONTENU */}
      <div className="flex-1 overflow-y-auto custom-scrollbar pt-6 md:pl-10 pr-2 pb-10">
  <div className="max-w-4xl mx-auto">
          
          {/* ONGLET : COMPTE (FINALISÉ) */}
          {activeTab === 'account' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-xl font-bold mb-2">{t('account_title', 'Profil Utilisateur')}</h3>
                <p className="text-slate-400 text-sm mb-6">{t('account_desc', 'Gérez vos informations personnelles gravées dans le bunker.')}</p>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2 opacity-60">
                 <label className="text-xs font-bold text-slate-500 uppercase tracking-widest">Prénom (Gravé)</label>
                 <input type="text" value={profileData.prenom} readOnly className="w-full bg-[#000b1e]/50 border border-white/5 rounded-xl py-3 px-4 text-slate-400 cursor-not-allowed outline-none" />
                </div>
                <div className="space-y-2 opacity-60">
                  <label className="text-xs font-bold text-slate-500 uppercase tracking-widest">Nom (Gravé)</label>
                  <input type="text" value={profileData.nom} readOnly className="w-full bg-[#000b1e]/50 border border-white/5 rounded-xl py-3 px-4 text-slate-400 cursor-not-allowed outline-none" />
                </div>
                <div className="space-y-2 opacity-60">
                 <label className="text-xs font-bold text-slate-500 uppercase tracking-widest">Date de Naissance (Gravé)</label>
                 <input type="date" value={profileData.dateNaissance} readOnly className="w-full bg-[#000b1e]/50 border border-white/5 rounded-xl py-3 px-4 text-slate-400 cursor-not-allowed outline-none" />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400 uppercase tracking-widest">Adresse E-mail</label>
                  <input type="email" value={profileData.email} onChange={(e) => setProfileData({...profileData, email: e.target.value})} className="w-full bg-[#000b1e] border border-white/10 rounded-xl py-3 px-4 text-white focus:border-[#00f2ff] focus:outline-none transition-colors" />
                </div>
              </div>

              <div className="pt-6 border-t border-white/10 flex flex-col sm:flex-row gap-4 justify-between items-center">
                <button 
                  onClick={handleProfileUpdate}
                  className="flex items-center justify-center w-full sm:w-auto gap-2 bg-[#00f2ff] text-[#000814] font-bold px-6 py-3 rounded-xl hover:bg-white transition-colors shadow-[0_0_20px_rgba(0,242,255,0.3)]"
                >
                  <Save size={18} /> Sauvegarder mon identité
                </button>

                <button 
                  onClick={() => { setIsDeleteModalOpen(true); setDeleteError(''); setDeletePassword(''); }}
                  className="flex items-center justify-center w-full sm:w-auto gap-2 bg-red-500/10 border border-red-500/30 text-red-500 hover:bg-red-600 hover:text-white font-bold px-6 py-3 rounded-xl transition-all shadow-[0_0_15px_rgba(239,68,68,0.2)]"
                >
                  <X size={18} /> Supprimer le profil définitivement
                </button>
              </div>
            </div>
          )}

          {activeTab === 'instructions' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-xl font-bold mb-2">Instructions pour Léa</h3>
                <p className="text-slate-400 text-sm mb-6">Partagez des informations sur votre vie et vos préférences pour obtenir des réponses hyper-personnalisées.</p>
              </div>

              {/* SECTION TOGGLE */}
              <div className="flex items-center justify-between p-4 bg-white/5 border border-white/10 rounded-2xl">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-[#00f2ff]/10 rounded-xl flex items-center justify-center text-[#00f2ff]">
                    <Bot size={20} />
                  </div>
                  <div>
                    <p className="text-sm font-bold">Activer la personnalisation</p>
                    <p className="text-[10px] text-slate-500">Léa utilisera ces notes comme contexte absolu.</p>
                  </div>
                </div>
                <div className="relative inline-block w-12 h-6 cursor-pointer" onClick={handleToggleInstructions}>
                  <div className={`block w-12 h-6 rounded-full transition-colors ${isCustomEnabled ? 'bg-[#00f2ff]' : 'bg-slate-600'}`}></div>
                  <div className={`absolute left-1 top-1 bg-white w-4 h-4 rounded-full transition-transform ${isCustomEnabled ? 'transform translate-x-6' : ''}`}></div>
                </div>
              </div>

              {/* LISTE DES BLOCS DE MÉMOIRE */}
              <div className={`space-y-4 transition-opacity ${!isCustomEnabled ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
                {instructionsBlocks.map((block, index) => (
                  <div key={index} className="p-4 bg-white/5 border border-white/10 rounded-2xl flex justify-between items-start group hover:border-white/30 transition-all">
                    <p className="text-sm text-slate-300 whitespace-pre-wrap flex-1 mr-4">{block}</p>
                    <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button 
                        onClick={() => { setEditingIndex(index); setNewInstructionText(block); setIsInstructionModalOpen(true); }}
                        className="p-2 text-slate-400 hover:text-[#00f2ff] transition-colors bg-black/20 rounded-lg"
                      >
                        <Save size={14} />
                      </button>
                      <button 
                        onClick={() => handleDeleteInstruction(index)}
                        className="p-2 text-slate-400 hover:text-red-400 transition-colors bg-black/20 rounded-lg"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  </div>
                ))}
                
                <button 
                  onClick={() => { setEditingIndex(null); setNewInstructionText(''); setIsInstructionModalOpen(true); }}
                  className="w-full py-4 border border-dashed border-white/20 hover:border-[#00f2ff]/50 hover:bg-[#00f2ff]/5 text-slate-400 hover:text-[#00f2ff] rounded-2xl text-xs font-bold transition-all flex items-center justify-center gap-2"
                >
                  + AJOUTER UNE INFORMATION
                </button>
              </div>

              {instructionsBlocks.length > 0 && (
                <div className="pt-6 border-t border-white/10">
                  <button 
                    onClick={handleClearAllInstructions}
                    className="px-6 py-3 bg-red-500/10 text-red-400 border border-red-500/30 rounded-xl font-bold text-xs hover:bg-red-500 hover:text-white transition-all flex items-center gap-2"
                  >
                    <X size={16} /> TOUT SUPPRIMER D'UN COUP
                  </button>
                </div>
              )}

              {/* LA MODAL D'AJOUT/MODIFICATION STYLE GEMINI */}
              {isInstructionModalOpen && (
                <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in">
                  <div className="bg-[#1e1e1e] border border-white/10 p-6 rounded-3xl w-full max-w-md shadow-2xl flex flex-col gap-4">
                    <h3 className="text-lg font-bold text-white">
                      {editingIndex !== null ? "Modifier l'information" : "Que voulez-vous que Léa mémorise ?"}
                    </h3>
                    <textarea 
                      value={newInstructionText}
                      onChange={(e) => setNewInstructionText(e.target.value)}
                      placeholder='Par exemple, "Je préfère les réponses brèves" ou "Pour les pochettes, le titre doit être en haut au milieu"'
                      className="w-full bg-[#2a2a2a] text-white border-none rounded-2xl p-4 min-h-[120px] resize-none focus:ring-2 focus:ring-[#00f2ff] outline-none text-sm custom-scrollbar"
                      autoFocus
                    />
                    <div className="flex justify-end gap-3 mt-2">
                      <button 
                        onClick={() => { setIsInstructionModalOpen(false); setNewInstructionText(''); setEditingIndex(null); }}
                        className="px-5 py-2.5 rounded-full text-slate-300 hover:bg-white/10 font-medium transition-all text-sm"
                      >
                        Annuler
                      </button>
                      <button 
                        onClick={handleSaveInstruction}
                        disabled={!newInstructionText.trim()}
                        className="px-5 py-2.5 rounded-full bg-white text-black hover:bg-gray-200 font-medium transition-all disabled:opacity-50 text-sm"
                      >
                        Envoyer
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ONGLET : SÉCURITÉ */}
          {activeTab === 'security' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-xl font-bold mb-2">{t('sec_title', 'Sécurité & Mot de passe')}</h3>
                <p className="text-slate-400 text-sm mb-6">{t('sec_desc', "Gérez l'accès à votre espace personnel et vos clés de sécurité.")}</p>
              </div>
              
              <div className="space-y-4">
                {/* MESSAGE DE STATUT DYNAMIQUE */}
                {pwdStatus && (
                  <div className={`p-4 rounded-xl border font-bold animate-in fade-in ${pwdStatus.type === 'success' ? 'bg-green-500/10 border-green-500/50 text-green-400' : 'bg-red-500/10 border-red-500/50 text-red-400'}`}>
                    {pwdStatus.msg}
                  </div>
                )}

                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400 uppercase tracking-widest">{t('sec_old_pwd', 'Ancien mot de passe')}</label>
                  <div className="relative">
                    <Key className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                    <input 
                      type="password" 
                      value={oldPassword}
                      onChange={(e) => setOldPassword(e.target.value)}
                      placeholder="••••••••" 
                      className="w-full bg-[#000b1e] border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white focus:border-[#00f2ff] focus:outline-none transition-colors" 
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-400 uppercase tracking-widest">{t('sec_new_pwd', 'Nouveau mot de passe')}</label>
                  <div className="relative">
                    <Key className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                    <input 
                      type="password" 
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="••••••••" 
                      className="w-full bg-[#000b1e] border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white focus:border-[#00f2ff] focus:outline-none transition-colors" 
                    />
                  </div>
                </div>
              </div>

              <div className="pt-6 border-t border-white/10">
                <button 
                  onClick={handlePasswordUpdate}
                  className="flex items-center gap-2 bg-white/5 border border-white/10 hover:border-[#00f2ff] hover:text-[#00f2ff] text-white font-bold px-6 py-3 rounded-xl transition-all"
                >
                  <Save size={18} /> {t('btn_update_pwd', 'Mettre à jour le mot de passe')}
                </button>
              </div>
            </div>
          )}

          {/* --- ONGLET PROFIL (GRAVÉ) --- */}
        {activeTab === 'profile' && (
          <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500 text-left">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-[10px] text-slate-500 uppercase font-bold tracking-widest">Prénom (Gravé)</label>
                <div className="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-slate-300 font-medium">
                  {userPrenom || "Non renseigné"}
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-[10px] text-slate-500 uppercase font-bold tracking-widest">Nom (Gravé)</label>
                <div className="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-slate-300 font-medium">
                  {userNom || "Non renseigné"}
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] text-slate-500 uppercase font-bold tracking-widest">Adresse Mail</label>
              <div className="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-slate-300 font-medium">
                {userEmail || "Non renseignée"}
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] text-slate-500 uppercase font-bold tracking-widest">Date de Naissance</label>
              <div className="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-slate-300 font-medium">
                {userDob || "Non renseignée"}
              </div>
            </div>
          </div>
        )}

          {/* ONGLET : ABONNEMENT & PAIEMENT CRYPTO */}
          {activeTab === 'subscription' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div className="flex justify-between items-end">
                <div>
                  <h3 className="text-xl font-bold mb-2">Gestion de l'Abonnement</h3>
                  <p className="text-slate-400 text-sm">Choisissez votre rythme de facturation.</p>
                </div>
                
                {/* SÉLECTEUR DE CYCLE */}
                <div className="flex bg-white/5 p-1 rounded-xl border border-white/10">
                  <button 
                    onClick={() => setBillingCycle('monthly')}
                    className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all ${billingCycle === 'monthly' ? 'bg-[#00f2ff] text-black shadow-[0_0_10px_rgba(0,242,255,0.3)]' : 'text-slate-400'}`}
                  >
                    Mensuel
                  </button>
                  <button 
                    onClick={() => setBillingCycle('yearly')}
                    className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all ${billingCycle === 'yearly' ? 'bg-[#00f2ff] text-black shadow-[0_0_10px_rgba(0,242,255,0.3)]' : 'text-slate-400'}`}
                  >
                    Annuel (-15%)
                  </button>
                </div>
              </div>
              
              {/* ÉTAT ACTUEL DU COMPTE */}
              <div className={`p-6 border rounded-2xl relative overflow-hidden ${profileData.abonnement === 'free' ? 'bg-white/5 border-white/10' : 'bg-gradient-to-br from-yellow-500/10 to-yellow-900/20 border-yellow-500/30'}`}>
                <h4 className="text-3xl font-black text-white mb-1 uppercase">
                  {profileData.abonnement === 'free' ? 'Plan Gratuit' : profileData.abonnement}
                </h4>
                
                {profileData.nextBillingDate ? (
                  <div className="mt-4">
                    <p className="text-slate-300 mb-6 font-medium">
                      {profileData.cancelAtPeriodEnd ? "Prend fin le : " : "Prochain prélèvement le : "}
                      {new Date(profileData.nextBillingDate).toLocaleDateString()}
                    </p>
                    <button 
                      onClick={handleCancelSubscription}
                      disabled={profileData.cancelAtPeriodEnd}
                      className={`font-bold px-6 py-2.5 rounded-xl transition-all ${profileData.cancelAtPeriodEnd ? 'bg-white/5 text-slate-500 cursor-not-allowed' : 'bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white'}`}
                    >
                      {profileData.cancelAtPeriodEnd ? "Résiliation en cours..." : "Résilier l'abonnement"}
                    </button>
                  </div>
                ) : (
                  <p className="text-slate-400 mt-2">Passez à la puissance supérieure pour débloquer Léa.</p>
                )}
              </div>

              {/* MODULE DE PAIEMENT CRYPTO MULTI-RÉSEAUX (Affiché seulement si gratuit) */}
              {profileData.abonnement === 'free' && (
                <div className="space-y-6 p-6 bg-white/5 border border-[#00f2ff]/30 rounded-2xl shadow-[0_0_20px_rgba(0,242,255,0.05)]">
                  <div className="flex justify-between items-center">
                    <h4 className="text-lg font-bold text-white flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-[#00f2ff] animate-pulse"></div>
                      Paiement Web3 Décentralisé
                    </h4>
                    <span className="text-xl font-black text-[#00f2ff]">
                      {billingCycle === 'monthly' ? '19.99 USDT' : '199.99 USDT'}
                    </span>
                  </div>
                  
                  {/* SÉLECTION DU RÉSEAU */}
                  <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                    {Object.keys(cryptoWallets).map(net => (
                      <button 
                        key={net}
                        onClick={() => setSelectedNetwork(net)}
                        className={`py-2 rounded-lg text-[10px] font-bold uppercase transition-all border ${selectedNetwork === net ? 'bg-[#00f2ff]/20 border-[#00f2ff] text-[#00f2ff]' : 'bg-white/5 border-white/10 text-slate-500 hover:border-white/30'}`}
                      >
                        {net}
                      </button>
                    ))}
                  </div>

                  {/* ADRESSE DU WALLET */}
                  <div className="bg-[#000b1e] p-4 rounded-xl border border-white/5">
                    <p className="text-[10px] text-slate-500 mb-2 uppercase tracking-widest">Adresse de dépôt ({selectedNetwork})</p>
                    <code className="text-xs text-[#00f2ff] break-all font-mono bg-white/5 p-2 rounded block select-all">
                      {cryptoWallets[selectedNetwork as keyof typeof cryptoWallets]}
                    </code>
                  </div>

                  {/* VÉRIFICATION TXHASH */}
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-500 uppercase tracking-widest">Collez votre TxHash (ID de transaction)</label>
                    <input 
                      type="text" 
                      value={txHash}
                      onChange={(e) => setTxHash(e.target.value)}
                      placeholder="0x..." 
                      className="w-full bg-[#000b1e] border border-white/10 rounded-xl py-3 px-4 text-white text-xs focus:border-[#00f2ff] focus:outline-none transition-colors" 
                    />
                  </div>

                  {cryptoStatus && (
                    <div className={`p-3 rounded-lg text-[10px] font-bold ${cryptoStatus.type === 'success' ? 'bg-green-500/10 text-green-400 border border-green-500/30' : 'bg-red-500/10 text-red-400 border border-red-500/30'}`}>
                      {cryptoStatus.msg}
                    </div>
                  )}

                  <button 
                    onClick={handleCryptoPayment}
                    disabled={subLoading}
                    className="w-full bg-[#00f2ff] hover:bg-white text-black font-black py-4 rounded-xl transition-all shadow-[0_0_20px_rgba(0,242,255,0.3)] uppercase tracking-widest text-xs"
                  >
                    {subLoading ? "VÉRIFICATION DANS LA MATRICE..." : "J'AI ENVOYÉ LES FONDS"}
                  </button>
                </div>
              )}
            </div>
          )}

          {/* ONGLET : LANGUE */}
          {activeTab === 'language' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-xl font-bold mb-2">{t('lang_title', "Langue de l'interface")}</h3>
                <p className="text-slate-400 text-sm mb-6">{t('lang_desc', "Choisissez la langue d'affichage globale de l'application LÉA.")}</p>
              </div>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {languages.map(lang => {
                  const currentLang = localStorage.getItem('i18nextLng') || 'fr';
                  const isActive = currentLang.startsWith(lang.id);
                  
                  return (
                    <button 
                      key={lang.id}
                      onClick={() => setConfirmLang(lang.id)}
                      className={`flex items-start justify-between p-5 rounded-2xl border text-left transition-all group ${
                        isActive 
                        ? 'bg-[#00f2ff]/10 border-[#00f2ff]/50 shadow-[0_0_20px_rgba(0,242,255,0.1)]' 
                        : 'bg-white/[0.02] border-white/10 hover:border-white/30 hover:bg-white/[0.05]'
                      }`}
                    >
                      <div className={lang.dir === 'rtl' ? 'text-right w-full' : ''}>
                        <h5 className={`font-bold text-lg ${isActive ? 'text-[#00f2ff]' : 'text-white'}`}>{lang.name}</h5>
                        <p className="text-slate-500 text-xs mt-1 uppercase tracking-widest">{lang.region}</p>
                      </div>
                      {isActive && (
                        <div className="w-5 h-5 rounded-full bg-[#00f2ff] flex items-center justify-center shadow-[0_0_10px_#00f2ff] shrink-0 ml-3">
                          <Check size={12} className="text-[#000814] font-bold" />
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* NOUVEL ONGLET : VOIX DE L'ASSISTANTE (DYNAMIQUE ET CATÉGORISÉ) */}
          {activeTab === 'voice' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-xl font-bold mb-2">Identité Vocale Souveraine</h3>
                <p className="text-slate-400 text-sm mb-6">
                  Voici les identités vocales disponibles pour la langue actuelle de l'interface (<strong className="text-[#00f2ff] uppercase">{currentLangKey}</strong>). Cliquez sur l'icône de volume pour tester.
                </p>
              </div>

              {/* === CATÉGORIE FÉMININE (LÉA) === */}
              {voicesToDisplay.filter(v => v.gender === 'female').length > 0 && (
                <div className="mb-6">
                  <h4 className="text-sm font-bold text-[#00f2ff] mb-4 flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#00f2ff]"></span>
                    IDENTITÉS FÉMININES (LÉA)
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {voicesToDisplay.filter(v => v.gender === 'female').map(voice => (
                      <button 
                        key={voice.id}
                        onClick={() => changeVoice(voice.id)}
                        className={`flex flex-col p-4 rounded-2xl border transition-all text-left group ${activeVoice === voice.id ? 'bg-[#00f2ff]/10 border-[#00f2ff] shadow-[0_0_15px_rgba(0,242,255,0.2)]' : 'bg-white/5 border-white/10 hover:border-white/30'}`}
                      >
                        <div className="flex justify-between items-center w-full mb-2">
                          <span className={`font-bold text-sm ${activeVoice === voice.id ? 'text-white' : 'text-slate-400'}`}>{voice.name}</span>
                          <button 
                            onClick={(e) => { e.stopPropagation(); testVoice(voice.id); }}
                            disabled={isTesting}
                            className={`p-2 rounded-xl transition-all ${isTesting && activeVoice === voice.id ? 'bg-white text-black animate-pulse' : 'bg-white/5 hover:bg-[#00f2ff] hover:text-black'}`}
                            title="Tester la voix"
                          >
                            <Volume2 size={14} />
                          </button>
                        </div>
                        <span className="text-[10px] text-slate-500 group-hover:text-slate-300 uppercase tracking-tighter">{voice.desc}</span>
                        <span className="text-[8px] text-slate-700 mt-1 font-mono">{voice.id}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* === CATÉGORIE MASCULINE (LÉO) === */}
              {voicesToDisplay.filter(v => v.gender === 'male').length > 0 && (
                <div>
                  <h4 className="text-sm font-bold text-[#00f2ff] mb-4 flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#00f2ff]"></span>
                    IDENTITÉS MASCULINES (LÉO)
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {voicesToDisplay.filter(v => v.gender === 'male').map(voice => (
                      <button 
                        key={voice.id}
                        onClick={() => changeVoice(voice.id)}
                        className={`flex flex-col p-4 rounded-2xl border transition-all text-left group ${activeVoice === voice.id ? 'bg-[#00f2ff]/10 border-[#00f2ff] shadow-[0_0_15px_rgba(0,242,255,0.2)]' : 'bg-white/5 border-white/10 hover:border-white/30'}`}
                      >
                        <div className="flex justify-between items-center w-full mb-2">
                          <span className={`font-bold text-sm ${activeVoice === voice.id ? 'text-white' : 'text-slate-400'}`}>{voice.name}</span>
                          <button 
                            onClick={(e) => { e.stopPropagation(); testVoice(voice.id); }}
                            disabled={isTesting}
                            className={`p-2 rounded-xl transition-all ${isTesting && activeVoice === voice.id ? 'bg-white text-black animate-pulse' : 'bg-white/5 hover:bg-[#00f2ff] hover:text-black'}`}
                            title="Tester la voix"
                          >
                            <Volume2 size={14} />
                          </button>
                        </div>
                        <span className="text-[10px] text-slate-500 group-hover:text-slate-300 uppercase tracking-tighter">{voice.desc}</span>
                        <span className="text-[8px] text-slate-700 mt-1 font-mono">{voice.id}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* SI AUCUNE VOIX N'EST TROUVÉE POUR LA LANGUE */}
              {voicesToDisplay.length === 0 && (
                <div className="col-span-full p-6 text-center border border-dashed border-white/20 rounded-2xl text-slate-400">
                  Aucun fichier vocal détecté pour cette langue dans le coffre.
                </div>
              )}

            </div>
          )}

          {/* ========================================= */}
          {/* ONGLET : CONFIG LEABIXBY                  */}
          {/* ========================================= */}
          {activeTab === 'bixby' && (
            <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">

              {/* EN-TÊTE */}
              <div className="flex items-start gap-4">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#0047ff]/30 to-[#00f2ff]/20 border border-[#00f2ff]/30 flex items-center justify-center text-[#00f2ff] shadow-[0_0_20px_rgba(0,242,255,0.15)] shrink-0">
                  <Mic size={26} />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white">LeaNova — Mode vocal</h3>
                  <p className="text-slate-400 text-sm mt-1">
                    Configure comment Léa te parle et comment tu interagis avec elle sans toucher ton téléphone.
                  </p>
                </div>
              </div>

              {/* CARTE : NOM D'APPEL */}
              <div className="rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden">
                <div className="px-6 pt-5 pb-4 border-b border-white/10 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-[#00f2ff]/10 flex items-center justify-center text-[#00f2ff]">
                    <User size={15} />
                  </div>
                  <div>
                    <h4 className="font-bold text-white text-sm">Nom d'appel</h4>
                    <p className="text-slate-500 text-xs">Le prénom que Léa utilise pour te saluer à chaque activation</p>
                  </div>
                </div>
                <div className="p-6 space-y-4">
                  {profileData.leaBixbyNom && (
                    <div className="flex items-center gap-2 text-xs text-slate-500">
                      <span>Nom actuel :</span>
                      <span className="px-2 py-0.5 rounded-lg bg-[#00f2ff]/10 text-[#00f2ff] font-bold font-mono">{profileData.leaBixbyNom}</span>
                    </div>
                  )}
                  <div className="flex gap-3 items-center">
                    <input
                      type="text"
                      value={profileData.leaBixbyNom}
                      onChange={e => setProfileData(prev => ({ ...prev, leaBixbyNom: e.target.value }))}
                      placeholder={profileData.prenom || 'Tape ton prénom…'}
                      className="flex-1 bg-[#000b1e] border border-white/10 rounded-xl py-3 px-4 text-white outline-none focus:border-[#00f2ff] transition-colors placeholder:text-slate-600"
                    />
                    <button
                      onClick={handleBixbyNomSave}
                      className={`flex items-center gap-2 px-5 py-3 rounded-xl font-bold text-sm transition-all ${
                        bixbyNomSaved
                          ? 'bg-green-500/20 border border-green-500/40 text-green-400'
                          : 'bg-[#00f2ff]/10 border border-[#00f2ff]/30 text-[#00f2ff] hover:bg-[#00f2ff]/20'
                      }`}
                    >
                      {bixbyNomSaved ? <><Check size={15} /> Sauvegardé</> : <><Save size={15} /> Sauvegarder</>}
                    </button>
                  </div>
                  <p className="text-slate-600 text-xs">
                    Si ce champ est vide, Léa utilise ton prénom du profil. L'admin reçoit toujours "mon chéri".
                  </p>
                </div>
              </div>

              {/* CARTE : MÉMOIRE LONG TERME */}
              <div className="rounded-2xl border border-purple-500/20 bg-white/[0.03] overflow-hidden">
                <div className="px-6 pt-5 pb-4 border-b border-purple-500/20 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-purple-500/10 flex items-center justify-center text-purple-400">
                      <Brain size={15} />
                    </div>
                    <div>
                      <h4 className="font-bold text-white text-sm">Mémoire long terme</h4>
                      <p className="text-slate-500 text-xs">Ce que Léa sait de toi en permanence ({instructionsBlocks.length} souvenir{instructionsBlocks.length !== 1 ? 's' : ''})</p>
                    </div>
                  </div>
                  {instructionsBlocks.length > 0 && (
                    <button
                      onClick={() => askConfirm('Effacer toute la mémoire long terme ?', () => {
                        setInstructionsBlocks([]);
                        saveInstructionsToBunker(isCustomEnabled, []);
                      })}
                      className="text-xs text-red-400/60 hover:text-red-400 transition-colors flex items-center gap-1"
                    >
                      <Trash2 size={12} /> Tout effacer
                    </button>
                  )}
                </div>
                <div className="p-6 space-y-4">
                  {/* Liste des souvenirs */}
                  {instructionsBlocks.length === 0 ? (
                    <p className="text-slate-600 text-xs text-center py-4 italic">Aucun souvenir enregistré. Dis "Léa, souviens-toi que..." ou ajoute ci-dessous.</p>
                  ) : (
                    <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                      {instructionsBlocks.map((block, i) => (
                        <div key={i} className="flex items-start gap-3 p-3 rounded-xl bg-[#000b1e] border border-purple-500/10 group">
                          <Brain size={13} className="text-purple-400/60 mt-0.5 shrink-0" />
                          <p className="text-slate-300 text-xs flex-1 leading-relaxed">{block}</p>
                          <button
                            onClick={() => handleDeleteBixbyMemory(i)}
                            className="opacity-0 group-hover:opacity-100 transition-opacity text-red-400/60 hover:text-red-400 shrink-0"
                          >
                            <X size={13} />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                  {/* Ajouter un souvenir */}
                  <div className="flex gap-2 items-center pt-1">
                    <input
                      type="text"
                      value={bixbyNewMemory}
                      onChange={e => setBixbyNewMemory(e.target.value)}
                      onKeyDown={e => { if (e.key === 'Enter') handleAddBixbyMemory(); }}
                      placeholder="Ex: J'ai un chien qui s'appelle Rex…"
                      className="flex-1 bg-[#000b1e] border border-purple-500/20 rounded-xl py-2.5 px-4 text-white text-sm outline-none focus:border-purple-400 transition-colors placeholder:text-slate-600"
                    />
                    <button
                      onClick={handleAddBixbyMemory}
                      disabled={!bixbyNewMemory.trim()}
                      className={`flex items-center gap-1.5 px-4 py-2.5 rounded-xl font-bold text-xs transition-all shrink-0 ${
                        bixbyMemSaved
                          ? 'bg-green-500/20 border border-green-500/40 text-green-400'
                          : 'bg-purple-500/10 border border-purple-500/30 text-purple-400 hover:bg-purple-500/20 disabled:opacity-40 disabled:cursor-not-allowed'
                      }`}
                    >
                      {bixbyMemSaved ? <><Check size={13} /> Mémorisé</> : <><Plus size={13} /> Ajouter</>}
                    </button>
                  </div>
                </div>
              </div>

              {/* CARTE : COMMANDES VOCALES */}
              <div className="rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden">
                <div className="px-6 pt-5 pb-4 border-b border-white/10 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-[#00f2ff]/10 flex items-center justify-center text-[#00f2ff]">
                    <MessageSquare size={15} />
                  </div>
                  <div>
                    <h4 className="font-bold text-white text-sm">Commandes vocales</h4>
                    <p className="text-slate-500 text-xs">Ce que tu peux dire directement à Léa sans passer par les paramètres</p>
                  </div>
                </div>
                <div className="p-6 space-y-3">
                  {[
                    {
                      cmd: '"Appelle-moi [prénom] maintenant"',
                      desc: 'Change ton nom d\'appel immédiatement. Léa confirme à la voix.',
                      variants: ['désormais', 'dorénavant', 'à partir de maintenant']
                    }
                  ].map((item, i) => (
                    <div key={i} className="p-4 rounded-xl bg-[#000b1e] border border-white/5">
                      <p className="font-mono text-[#00f2ff] text-sm font-bold mb-1">{item.cmd}</p>
                      <p className="text-slate-400 text-xs mb-2">{item.desc}</p>
                      <div className="flex flex-wrap gap-1">
                        <span className="text-[10px] text-slate-600 mr-1">Variantes :</span>
                        {item.variants.map(v => (
                          <span key={v} className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 text-slate-400 font-mono">"{v}"</span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* CARTE : COMMENT ÇA MARCHE */}
              <div className="rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden">
                <div className="px-6 pt-5 pb-4 border-b border-white/10 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-[#00f2ff]/10 flex items-center justify-center text-[#00f2ff]">
                    <Sparkles size={15} />
                  </div>
                  <div>
                    <h4 className="font-bold text-white text-sm">Comment fonctionne LeaNova</h4>
                    <p className="text-slate-500 text-xs">Le déroulement complet d'une session vocale</p>
                  </div>
                </div>
                <div className="p-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {[
                    {
                      step: '1',
                      icon: <Mic size={18} />,
                      title: 'Réveil',
                      desc: 'Dis "Léa" à voix haute. Elle t\'entend en permanence en arrière-plan.'
                    },
                    {
                      step: '2',
                      icon: <Volume2 size={18} />,
                      title: 'Salut vocal',
                      desc: 'À la première activation de la session, Léa te salue par ton nom et demande ce qu\'elle peut faire.'
                    },
                    {
                      step: '3',
                      icon: <RefreshCw size={18} />,
                      title: 'Réflexion',
                      desc: 'Avant d\'envoyer ta question, elle dit "Attends, je réfléchis à ta question" — tu sais qu\'elle a bien compris.'
                    },
                    {
                      step: '4',
                      icon: <MessageSquare size={18} />,
                      title: 'Réponse',
                      desc: 'Elle répond à la voix avec ta voix configurée (Piper). Aucun texte à lire, tout est vocal.'
                    }
                  ].map(item => (
                    <div key={item.step} className="flex gap-3 p-4 rounded-xl bg-[#000b1e] border border-white/5">
                      <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#0047ff]/20 to-[#00f2ff]/10 border border-[#00f2ff]/20 flex items-center justify-center text-[#00f2ff] shrink-0">
                        {item.icon}
                      </div>
                      <div>
                        <p className="font-bold text-white text-sm">{item.title}</p>
                        <p className="text-slate-500 text-xs mt-0.5 leading-relaxed">{item.desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* LIEN RETOUR VOIX */}
              <button
                onClick={() => setActiveTab('voice')}
                className="flex items-center gap-2 text-slate-500 text-xs hover:text-[#00f2ff] transition-colors"
              >
                <Volume2 size={13} /> Changer la voix utilisée → onglet "Voix de l'Assistante"
              </button>

            </div>
          )}

          {/* === MODAL D'AUTO-DESTRUCTION (LÉA PROTECT) === */}
      {isDeleteModalOpen && (
        <div className="absolute inset-0 z-[150] flex items-center justify-center bg-black/80 backdrop-blur-md animate-in fade-in">
          <div className="bg-[#000b1e] border border-red-500/50 p-8 rounded-[2rem] max-w-sm w-full shadow-[0_0_50px_rgba(239,68,68,0.2)] text-center">
            <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6 text-red-500">
              <Shield size={32} />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Auto-destruction</h3>
            <p className="text-slate-400 text-sm mb-6">
              Cette action est <strong className="text-red-400">irréversible</strong>. Ton dossier, tes fichiers crypto et ton historique seront purgés de la Matrice. Tape ton code secret pour confirmer.
            </p>
            
            {deleteError && (
              <div className="mb-4 p-3 bg-red-500/20 border border-red-500/50 rounded-xl text-red-400 text-xs font-bold">
                {deleteError}
              </div>
            )}

            <div className="relative mb-6 text-left">
              <Key className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
              <input 
                type="password" 
                value={deletePassword}
                onChange={(e) => setDeletePassword(e.target.value)}
                placeholder="Code secret" 
                className="w-full bg-[#000814] border border-red-500/30 rounded-xl py-3 pl-12 pr-4 text-white focus:border-red-500 outline-none transition-colors" 
                autoFocus
              />
            </div>

            <div className="flex gap-4">
              <button 
                onClick={() => setIsDeleteModalOpen(false)}
                className="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold transition-all"
              >
                Annuler
              </button>
              <button 
                onClick={handleDeleteProfile}
                className="flex-1 py-3 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold transition-all shadow-[0_0_20px_rgba(239,68,68,0.4)]"
              >
                Pulvériser
              </button>
            </div>
          </div>
        </div>
      )}

        </div>
      </div>
    </div>
  );
};