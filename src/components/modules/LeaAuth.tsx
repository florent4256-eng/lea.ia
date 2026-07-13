import React, { useState, useEffect } from 'react';
import { Key, User, ArrowRight, Sparkles, CheckCircle2, Info, Mail, Calendar, Type, Eye, EyeOff, ArrowLeft, ShieldCheck, Lock } from 'lucide-react';

// 🔌 INJECTION DU PONT POUR BIXBY
import { setLeaIdentity } from '../../main';
import { useConfirmToast } from '../../hooks/useConfirmToast';

const API_BASE = '';

export const LeaAuth = ({ onLogin, isMaintenance }: { onLogin: (pseudo: string) => void, isMaintenance: boolean }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [isForgotPassword, setIsForgotPassword] = useState(false);
  const [showTerms, setShowTerms] = useState(false); // NOUVEAU : État pour afficher la charte
  const [resetToken, setResetToken] = useState('');
  const [resetUser, setResetUser] = useState('');
  
  // États des champs
  const [pseudo, setPseudo] = useState('');
  const [password, setPassword] = useState('');
  const [nom, setNom] = useState('');
  const [prenom, setPrenom] = useState('');
  const [dob, setDob] = useState('');
  const [email, setEmail] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [genre, setGenre] = useState('');
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  
  // Options
  const [acceptTerms, setAcceptTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isKycVerified, setIsKycVerified] = useState(false);
  const [showWeakPwdWarning, setShowWeakPwdWarning] = useState(false);
  const [forceWeakPwd, setForceWeakPwd] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  // Compte créé ailleurs (ex. Léa Love) avec juste pseudo+mot de passe — il manque les
  // informations propres à Léa. On les demande une fois, ici, avant d'entrer dans l'app.
  const [needsCompletion, setNeedsCompletion] = useState(false);
  const [pendingPseudo, setPendingPseudo] = useState('');
  const [pendingData, setPendingData] = useState<any>(null);

  // --- MODAL DE CONFIRMATION + TOAST (remplace les alert()/window.confirm() natifs moches) ---
  const { askConfirm, showToast, ConfirmToastHost } = useConfirmToast();

  // ── Calcul de la robustesse du mot de passe ──
  const getPwdStrength = (pwd: string): { score: number; label: string; color: string; width: string } => {
    if (!pwd) return { score: 0, label: '', color: 'bg-slate-700', width: '0%' };
    let score = 0;
    if (pwd.length >= 8)  score++;
    if (pwd.length >= 12) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    if (score <= 2) return { score, label: 'Faible', color: 'bg-red-500',    width: '25%'  };
    if (score <= 4) return { score, label: 'Moyen',  color: 'bg-orange-400',  width: '60%'  };
    return               { score, label: 'Fort',   color: 'bg-green-400',   width: '100%' };
  };
  const pwdStrength = getPwdStrength(password);

  // 1. DÉTECTION DU LIEN PAR MAIL
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('reset');
    const user = params.get('user');
    
    if (token && user) {
      setResetToken(token);
      setResetUser(user);
      setPseudo(user);
      setIsForgotPassword(true);
      setError("🔐 Mode Récupération : Entrez votre nouveau code secret.");
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // SCÉNARIO A : L'utilisateur tape son NOUVEAU mot de passe via le lien reçu
    if (resetToken) {
        if (!password.trim() || !confirmPassword.trim()) { 
            setError("Remplis les deux champs pour ton nouveau code secret."); 
            return; 
        }
        if (!isLogin && !genre) {
        setError("Veuillez sélectionner votre genre pour la Matrice.");
        return;
      }
        if (password !== confirmPassword) {
            setError("Les deux codes secrets ne correspondent pas !");
            return;
        }
        setIsLoading(true);
        setError('');
        
        try {
            // Utilisation d'un chemin relatif pour être compatible partout
            const res = await fetch(`${API_BASE}/api/auth/reset-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pseudo: resetUser, token: resetToken, newPassword: password })
            });
            const data = await res.json();
            
            if (data.success) {
                // M-04 : on ne stocke JAMAIS le mot de passe en clair dans localStorage
                showToast("✅ Mission accomplie : Code secret mis à jour avec succès dans le bunker !");
                window.location.href = '/';
            } else {
                setError(data.error);
            }
        } catch (err) {
            setError("Erreur critique de communication avec le bunker.");
        }
        setIsLoading(false);
        return;
    }

    // SCÉNARIO B : L'utilisateur DEMANDE un lien de réinitialisation
    if (isForgotPassword && !resetToken) {
        if (!pseudo.trim() || !email.trim()) {
            setError("Pseudo et Email requis pour demander l'accès.");
            return;
        }
        setIsLoading(true);
        setError('');
        
        try {
            // Simplification ici aussi
            const res = await fetch(`${API_BASE}/api/auth/forgot-password`, {
               method: 'POST',
               headers: { 'Content-Type': 'application/json' },
               body: JSON.stringify({ pseudo: pseudo.trim(), email: email.trim() })
            });
            const data = await res.json();
            
            if (data.success) {
                showToast("📧 " + data.message);
                setIsForgotPassword(false);
            } else {
                setError(data.error);
            }
        } catch(err) {
            setError("Erreur critique de communication avec le bunker.");
        }
        setIsLoading(false);
        return;
    }

    // SCÉNARIO C : LOGIQUE NORMALE (Connexion ou Inscription)
    if (isLogin) {
      if (!pseudo.trim() || !password.trim()) { setError("Pseudo et mot de passe requis."); return; }
    } else {
      if (!pseudo.trim() || !password.trim() || !nom.trim() || !prenom.trim() || !dob || !email.trim()) {
          setError("Remplis tous les champs du dossier, mon chéri."); return;
      }
      if (!acceptTerms) { setError("L'acceptation des CGU est obligatoire."); return; }
      // Avertissement mot de passe faible (peut être ignoré)
      if (pwdStrength.score <= 2 && !forceWeakPwd) {
        setShowWeakPwdWarning(true);
        return;
      }
    }
    
    setIsLoading(true);
    setError('');
    const formattedPseudo = pseudo.trim();

    try {
        if (!isLogin) {
            // 🟢 1. INSCRIPTION : Câblage direct vers la nouvelle route du Serveur Master
            const res = await fetch(`${API_BASE}/api/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    nom: nom.trim(),
                    prenom: prenom.trim(),
                    pseudo: formattedPseudo,
                    email: email.trim(),
                    dateNaissance: dob,
                    password: password.trim()
                })
            });
            const data = await res.json();
            
            if (!data.success) {
                setError(data.error || "Erreur lors de la création dans la Matrice.");
                setIsLoading(false);
                return;
            }
            
            // 🟢 MATRICE ÉCONOMIQUE & SÉCURITÉ : Synchronisation complète à la connexion
            const userSub = data.abonnement || 'free';
            const defaultTokens = userSub === 'ultra' ? '10000' : userSub === 'pro' ? '7000' : userSub === 'ai_plus' ? '3000' : '50';
            const defaultDaily = userSub === 'ultra' ? '200' : userSub === 'pro' ? '100' : userSub === 'ai_plus' ? '50' : '20';

            localStorage.setItem('lea_currentUser', formattedPseudo);
            
            // On récupère le vrai statut majeur depuis le serveur
            localStorage.setItem('lea_isAdult', data.isAdult !== undefined ? data.isAdult.toString() : 'true');
            
            // On synchronise le statut de la Douane
            if (data.isDouanePassed !== undefined) {
                localStorage.setItem('lea_kyc_verified', data.isDouanePassed.toString());
            }

            // On réhydrate les infos personnelles
            if (data.nom) localStorage.setItem('lea_nom', data.nom);
            if (data.prenom) localStorage.setItem('lea_prenom', data.prenom);
            if (data.email) localStorage.setItem('lea_email', data.email);

            localStorage.setItem('lea_abonnement', userSub);
            localStorage.setItem('lea_tokens', data.tokens?.toString() || defaultTokens);
            // On lit 'dailyQuota' qui est la variable renvoyée par le serveur
            // On lit 'dailyQuota' qui est la variable renvoyée par le serveur
            localStorage.setItem('lea_daily_left', data.dailyQuota?.toString() || defaultDaily); 
            localStorage.setItem('lea_last_reset', data.last_reset || new Date().toDateString());
            
            // 🎯 FRAPPE 1 : Gravure de l'identité dans le S23 Ultra
            await setLeaIdentity(formattedPseudo);

            onLogin(formattedPseudo);
            
        } else {
            // 🟢 2. CONNEXION : Le Vigile de la Tour Master prend le relais (Fini le bug de la tablette !)
            const res = await fetch(`${API_BASE}/api/auth/verify-session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: formattedPseudo, password: password.trim() })
            });
            const data = await res.json();

            if (!data.success) {
                setError(data.error || "Identifiants incorrects.");
                setIsLoading(false);
                return;
            }

            // Compte créé via Léa Love (ou tout autre point d'entrée minimal) : il manque
            // les informations propres à Léa. On les demande avant d'entrer dans l'app.
            if (!data.nom || !data.prenom || !data.dateNaissance || !data.genre) {
                setNom(data.nom || '');
                setPrenom(data.prenom || '');
                setDob(data.dateNaissance || '');
                setEmail(data.email || '');
                setGenre(data.genre || '');
                setPendingPseudo(formattedPseudo);
                setPendingData(data);
                setNeedsCompletion(true);
                setIsLoading(false);
                return;
            }

            finalizeLogin(formattedPseudo, data);
        }
    } catch (err) {
        setError("Erreur critique de communication avec la Tour Master.");
        setIsLoading(false);
    }
  };

  // Écriture localStorage + entrée dans l'app — extrait pour être appelé soit directement
  // (compte déjà complet), soit après le formulaire de complétion (compte créé ailleurs).
  const finalizeLogin = (formattedPseudo: string, data: any) => {
    const userSub = data.abonnement || 'free';
    const defaultTokens = userSub === 'ultra' ? '10000' : userSub === 'pro' ? '7000' : userSub === 'ai_plus' ? '3000' : '50';
    const defaultDaily = userSub === 'ultra' ? '200' : userSub === 'pro' ? '100' : userSub === 'ai_plus' ? '50' : '20';

    localStorage.setItem('lea_currentUser', formattedPseudo);
    // M-03/I-02 : isAdult depuis la réponse serveur uniquement, jamais recalculé côté client
    localStorage.setItem('lea_isAdult', data.isAdult !== undefined ? data.isAdult.toString() : 'true');
    // M-01 : stocker le token de session pour les requêtes authentifiées
    if (data.sessionToken) localStorage.setItem('lea_session_token', data.sessionToken);
    localStorage.setItem('lea_abonnement', userSub);
    localStorage.setItem('lea_tokens', data.tokens?.toString() || defaultTokens);
    localStorage.setItem('lea_daily_left', data.dailyQuota?.toString() || data.daily_left?.toString() || defaultDaily);
    localStorage.setItem('lea_last_reset', data.last_reset || new Date().toDateString());

    // 🎯 Gravure de l'identité dans le S23 Ultra
    setLeaIdentity(formattedPseudo);

    onLogin(formattedPseudo);
  };

  const submitCompletion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nom.trim() || !prenom.trim() || !dob || !email.trim() || !genre) {
        setError("Remplis tous les champs pour finir ton inscription Léa.");
        return;
    }
    setIsLoading(true);
    setError('');
    try {
        const res = await fetch(`${API_BASE}/api/user/update-profile`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: pendingPseudo,
                nom: nom.trim(),
                prenom: prenom.trim(),
                dateNaissance: dob,
                email: email.trim(),
                genre,
            }),
        });
        const data = await res.json();
        if (!data.success) {
            setError(data.error || "Erreur lors de la mise à jour du profil.");
            setIsLoading(false);
            return;
        }
        // Re-vérification : récupère isAdult recalculé côté serveur plutôt que de
        // réutiliser la valeur périmée de la première réponse (mineur/majeur dépend
        // de la date de naissance qu'on vient tout juste de renseigner).
        const freshRes = await fetch(`${API_BASE}/api/auth/verify-session`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: pendingPseudo, password: password.trim() }),
        });
        const freshData = await freshRes.json();
        finalizeLogin(pendingPseudo, freshData.success ? freshData : { ...pendingData, nom: nom.trim(), prenom: prenom.trim(), dateNaissance: dob, email: email.trim(), genre });
    } catch (err) {
        setError("Erreur critique de communication avec la Tour Master.");
        setIsLoading(false);
    }
  };

  // =========================================
  // VUE DE LA CHARTE DE CONFIDENTIALITÉ
  // =========================================
  if (showTerms) {
    return (
      <div className="flex h-screen w-full bg-[#050505] items-center justify-center font-sans relative overflow-hidden p-4">
        <ConfirmToastHost />
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHdpZHRoPSczMicgaGVpZ2h0PSczMic+PGNpcmNsZSBjeD0nMS41JyBjeT0nMS41JyByPScwLjc1JyBmaWxsPSd3aGl0ZScvPjwvc3ZnPg==')] opacity-[0.06] pointer-events-none" />
        <div className="absolute top-[-15%] left-[-10%] w-[55%] h-[55%] bg-[#0047ff] blur-[180px] opacity-25 pointer-events-none" />
        <div className="absolute bottom-[-15%] right-[-10%] w-[55%] h-[55%] bg-[#00d4ff] blur-[180px] opacity-[0.15] pointer-events-none" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[30%] h-[30%] bg-[#6600ff] blur-[130px] opacity-[0.08] pointer-events-none" />
        
        <div className="relative z-10 w-full max-w-2xl bg-[#020C1E]/80 backdrop-blur-2xl border border-[#00d4ff]/10 rounded-[2rem] shadow-[0_0_80px_rgba(0,71,255,0.18),0_0_0_1px_rgba(0,212,255,0.07)] flex flex-col h-[90vh] animate-in fade-in slide-in-from-bottom-6 duration-700">
          
          <div className="flex items-center justify-between p-6 border-b border-white/10 shrink-0">
            <button onClick={() => setShowTerms(false)} className="text-slate-400 hover:text-white transition-colors flex items-center gap-2">
              <ArrowLeft size={20} /> Retour
            </button>
            <div className="flex items-center gap-3">
              <ShieldCheck className="text-[#00f2ff]" size={24} />
              <h2 className="text-white font-bold tracking-wide">Pacte de Souveraineté</h2>
            </div>
            <div className="w-20"></div> {/* Spacer pour centrer le titre */}
          </div>

          <div className="p-6 overflow-y-auto custom-scrollbar text-slate-300 text-sm space-y-8 flex-1">
            <div className="space-y-4">
              <p><strong className="text-white text-base">Bienvenue dans le Réseau LÉA.</strong> En rejoignant cet écosystème, vous n'utilisez pas un simple service, vous devenez membre d'un réseau décentralisé, sécurisé et souverain. Vos données vous appartiennent. Voici nos engagements pour protéger votre identité, votre sécurité et vos droits.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">1. LÉA PROTECT : Votre Garde du Corps Numérique</h3>
              <p>La sécurité de nos utilisateurs est notre priorité absolue. L'application mobile LÉA intègre le module de sécurité avancée Léa Protect.</p>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong className="text-white">Écoute Bienveillante :</strong> Avec votre accord, Léa reste en veille active pour être invoquée à tout moment, à la manière d'un assistant vocal naturel.</li>
                <li><strong className="text-white">Coffre-Fort de Preuves :</strong> En cas de détection de menaces, de cyberharcèlement ou d'appels malveillants, Léa archive automatiquement ces preuves cryptées dans votre coffre-fort local, prêtes à être transmises aux autorités compétentes.</li>
                <li><strong className="text-white">Assistance d'Urgence :</strong> Si Léa détecte une situation de danger critique, elle est autorisée à contacter les services de secours, à synthétiser la situation et à transmettre vos informations vitales (nom, prénom, date de naissance) ainsi que votre position GPS en temps réel.</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">2. DÉCENTRALISATION & RÉSEAU LÉARIA</h3>
              <p>LÉA V3 refuse la centralisation des géants du web. Notre puissance est communautaire.</p>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong className="text-white">Partage de Ressources :</strong> En utilisant nos services, vous acceptez d'allouer une fraction de la puissance de calcul de votre appareil pour faire fonctionner l'intelligence collective du réseau.</li>
                <li><strong className="text-white">Programme de Récompense (Blockchain LÉA I) :</strong> Pour vous remercier de votre participation, vous serez éligible à des distributions gratuites (Airdrops) de notre future cryptomonnaie. <em>(Note : Les utilisateurs pratiquant du minage intensif sur nos serveurs sont exclus de ce programme)</em>.</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">3. LA MATRICE LÉA : Confidentialité par Module</h3>
              <p>Votre écosystème intègre des outils puissants nécessitant des accès spécifiques, toujours chiffrés de bout en bout :</p>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong className="text-white">Pôle Créatif (Studios) :</strong> La génération de médias garantit vos droits d'auteur sur les créations.</li>
                <li><strong className="text-white">Pôle Social & Communication :</strong> Le Chat mondial, les salons privés et l'application Maps utilisent un chiffrement inviolable.</li>
                <li><strong className="text-white">Léa Crypto :</strong> Vos transactions, le trading avec effet de levier et le minage sont gérés via des protocoles blockchain sécurisés.</li>
                <li><strong className="text-white">Léa Auto & Home :</strong> L'accès au diagnostic de votre véhicule et à votre domotique locale ne quitte jamais votre réseau personnel.</li>
                <li><strong className="text-white">Léa Biohack :</strong> Vos données de santé et de coaching sont strictement confidentielles et chiffrées localement.</li>
                <li><strong className="text-white">Léa Apprentissage & Outils Avancés :</strong> Les modules d'éducation, Léa Archive, Léa Vision, Léa Buttel, Léa Colba et Léa Ghost tournent dans des environnements isolés pour protéger vos données.</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">4. ABONNEMENTS ET LIBERTÉ FINANCIÈRE</h3>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong className="text-white">Zéro Engagement :</strong> Nos offres sont sans engagement. Vous pouvez annuler à tout moment.</li>
                <li><strong className="text-white">Paiements Sécurisés :</strong> L'ensemble des flux financiers (Fiat ou Crypto) est anonymisé et sécurisé par nos partenaires de paiement Web3.</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">5. LIMITATION DE RESPONSABILITÉ & GARANTIE</h3>
              <p>Le réseau LÉA est fourni "en l'état" sans aucune garantie de résultat ou de disponibilité. <strong>L'utilisateur décharge expressément l'Administrateur</strong> de toute responsabilité en cas de dommages, pertes de données, ou conséquences légales découlant de l'utilisation des contenus générés par l'IA.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">6. USAGES PROHIBÉS & TOLÉRANCE ZÉRO</h3>
              <p>Il est strictement interdit d'utiliser l'infrastructure pour :</p>
              <ul className="list-disc pl-5 space-y-2">
                <li>La création de contenus illégaux, haineux, violents ou diffamatoires.</li>
                <li>Toute tentative de piratage, d'ingénierie inverse ou d'attaque par déni de service sur le réseau.</li>
                <li>La sollicitation de méthodes criminelles ou la violation de la vie privée d'autrui.</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">7. DROIT DE RÉVOCATION (GLACE LÉTALE)</h3>
              <p>L'Administrateur se réserve le droit souverain de <strong>bannir définitivement et sans préavis</strong> tout utilisateur dont le comportement est jugé suspect ou malveillant par le module Léa Protect. L'accès au réseau est un privilège révocable à tout moment pour garantir la sécurité de l'intelligence collective.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">8. PROPRIÉTÉ DES RÉSULTATS</h3>
              <p>Bien que l'utilisateur soit propriétaire des contenus qu'il génère, il accepte que l'Administrateur ne puisse être tenu pour responsable en cas de violation de droits de propriété intellectuelle tiers par le moteur d'intelligence artificielle.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">9. AVERTISSEMENT SUR LES ERREURS (HALLUCINATIONS)</h3>
              <p className="italic text-red-400">LÉA V3 est une intelligence artificielle qui peut commettre des erreurs ou donner des informations inexactes, y compris sur des faits réels.</p>
              <p>L'utilisateur est tenu de vérifier systématiquement les informations fournies. L'Administrateur ne pourra être tenu responsable des conséquences d'une information erronée.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">10. CLAUSE DE NON-CONSEIL (FINANCE & SANTÉ)</h3>
              <p>Les modules <strong>Léa Crypto</strong> et <strong>Léa Biohack</strong> sont fournis à titre informatif. L'IA ne remplace pas un conseiller financier ou un médecin. Tout investissement ou décision de santé est pris sous la seule responsabilité de l'utilisateur.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">11. SÉCURITÉ PHYSIQUE & DOMOTIQUE</h3>
              <p>Pour les modules <strong>Léa Auto</strong> et <strong>Léa Home</strong>, l'utilisateur accepte les risques liés à l'interconnectivité. L'Administrateur ne pourra être tenu responsable des pannes ou accidents découlant d'une commande système.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">12. PROTECTION DES MINEURS & MAJORITÉ LÉGALE</h3>
              <p>L'accès à l'écosystème LÉA est <strong>strictement interdit aux mineurs</strong>. Le système utilise la date de naissance déclarée pour verrouiller les modules sensibles (Crypto, Love, Biohack).</p>
              <p><strong>Responsabilité :</strong> À l'instant précis de sa majorité légale (18 ans), l'utilisateur devient l'unique responsable juridique de ses actes sur le réseau. Toute tentative de contournement par un mineur (fausse déclaration) entraînera un bannissement immédiat et dégage l'Administrateur de toute responsabilité légale en cas de problème.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">13. CLAUSE D'INDEMNISATION</h3>
              <p>L'utilisateur s'engage à indemniser, défendre et dégager de toute responsabilité l'Administrateur contre toute réclamation, poursuite, perte ou dépense (y compris les frais d'avocat) résultant de son utilisation du réseau LÉA, de la violation de ces CGU ou de la violation des droits d'un tiers.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">14. MODIFICATION DES CONDITIONS</h3>
              <p>L'Administrateur se réserve le droit de modifier le présent manifeste à tout moment pour s'adapter aux évolutions technologiques et légales de l'intelligence artificielle. La poursuite de l'utilisation du réseau après modification vaut acceptation sans réserve des nouvelles conditions.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">15. SÉCURITÉ DES ACTIFS & CYBERATTAQUES</h3>
              <p>L'utilisateur reconnaît que le réseau LÉA opère sur des protocoles décentralisés. <strong>L'Administrateur n'a jamais accès à vos clés privées</strong> et ne peut pas restaurer un accès perdu. En cas de piratage, de phishing ou de vulnérabilité technique (bug), l'utilisateur accepte que l'Administrateur ne peut être tenu pour responsable de la perte ou du vol de ses actifs numériques.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">16. EXCLUSION DE RESPONSABILITÉ FINANCIÈRE</h3>
              <p>L'utilisation des outils de trading, de bots ou de portefeuilles via Léa s'effectue aux risques et périls de l'utilisateur. Aucune perte financière, qu'elle soit due à la volatilité du marché, à une défaillance du code ou à une erreur système, ne pourra donner lieu à un remboursement ou à des poursuites contre l'Administrateur.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">17. FORCE MAJEURE</h3>
              <p>L'Administrateur ne pourra être tenu responsable d'aucun retard ou manquement lié à un cas de force majeure, incluant, mais sans s'y limiter, les pannes de réseau, coupures de courant, cyberattaques massives ou défaillances matérielles.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">18. INDÉPENDANCE DES CLAUSES</h3>
              <p>Si l'une des dispositions de ce manifeste devait être déclarée nulle ou inapplicable par une juridiction compétente, cette nullité n'affectera en rien la validité des autres clauses, qui resteront pleinement en vigueur et applicables.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">19. JURIDICTION ET LOI APPLICABLE</h3>
              <p>Le réseau LÉA est opéré sous juridiction française. En cas de litige, et à défaut de résolution à l'amiable, les lois françaises s'appliqueront et les tribunaux compétents seront exclusivement ceux du lieu de résidence de l'Administrateur.</p>
            </div>

            <div className="space-y-3">
              <h3 className="text-[#00f2ff] font-bold text-lg">20. CONSENTEMENT TOTAL</h3>
              <p>En cliquant sur le bouton ci-dessous, vous validez la création de votre coffre-fort numérique personnel et autorisez le système LÉA à déployer ses modules de protection, d'assistance et de partage de ressources selon les termes définis dans ce pacte.</p>
              <p className="text-[10px] italic opacity-50">Dernière mise à jour du protocole : 17 Avril 2026.</p>
            </div>

          </div>

          <div className="p-6 border-t border-[#00d4ff]/10 shrink-0 bg-[#010A1A]">
             <button
                type="button"
                onClick={() => { setAcceptTerms(true); setShowTerms(false); }}
                className="w-full py-4 bg-transparent border border-[#00d4ff]/70 text-[#00d4ff] font-bold tracking-wide rounded-xl flex items-center justify-center gap-2 transition-all duration-200 [transition-timing-function:cubic-bezier(0.34,1.56,0.64,1)] hover:bg-[#00d4ff] hover:text-[#020617] hover:border-[#00d4ff] hover:shadow-[0_0_25px_rgba(0,212,255,0.7),0_0_60px_rgba(0,212,255,0.3)] hover:scale-105 active:scale-[0.98]"
             >
                J'accepte et je rejoins le réseau <CheckCircle2 size={18} />
             </button>
          </div>

        </div>
      </div>
    );
  }

  // =========================================
  // VUE DE COMPLÉTION (compte créé via Léa Love — infos Léa manquantes)
  // =========================================
  if (needsCompletion) {
    return (
      <div className="min-h-screen w-full bg-[#050505] flex items-center justify-center font-sans px-4 py-8">
        <ConfirmToastHost />
        <div className="w-full max-w-md bg-[#0f172a] rounded-[18px] border border-[#00d4ff]/10 p-8">
          <div className="flex flex-col items-center mb-6 text-center">
            <Sparkles size={28} className="text-[#00f2ff] mb-3" />
            <h1 className="text-xl font-black text-white tracking-tight">Finis ton inscription Léa</h1>
            <p className="text-slate-400 text-xs mt-2">
              Ton compte {pendingPseudo} vient d'un autre point d'entrée (Léa Love). Il manque quelques informations propres à Léa pour continuer.
            </p>
          </div>

          <form onSubmit={submitCompletion} className="flex flex-col gap-3">
            <div className="relative group">
              <Type className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
              <input type="text" placeholder="Prénom" value={prenom} onChange={(e) => setPrenom(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
            </div>
            <div className="relative group">
              <Type className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
              <input type="text" placeholder="Nom" value={nom} onChange={(e) => setNom(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
            </div>
            <div className="relative group">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
              <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
            </div>
            <div className="relative group">
              <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
              <input type="date" value={dob} onChange={(e) => setDob(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white/70 rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm [&::-webkit-calendar-picker-indicator]:filter [&::-webkit-calendar-picker-indicator]:invert" />
            </div>

            <div>
              <label className="text-slate-400 text-xs font-medium mb-2 flex items-center gap-2">
                <Sparkles size={14} className="text-[#00f2ff]" /> Nature de l'identité
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'homme', label: 'Homme' },
                  { id: 'femme', label: 'Femme' },
                  { id: 'enfant', label: 'Enfant' }
                ].map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setGenre(item.id)}
                    className={`py-2 px-3 rounded-lg border text-xs font-medium transition-all ${
                      genre === item.id
                        ? 'bg-[#00f2ff]/10 border-[#00f2ff] text-[#00f2ff] shadow-[0_0_15px_rgba(0,242,255,0.2)]'
                        : 'bg-slate-900/50 border-slate-800 text-slate-500 hover:border-slate-700'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {error && <p className="text-red-400 text-xs text-center">{error}</p>}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full mt-2 py-3 bg-[#00d4ff] text-[#020617] font-bold rounded-xl disabled:opacity-50"
            >
              {isLoading ? 'Enregistrement…' : 'Continuer'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // =========================================
  // VUE PRINCIPALE (Connexion / Inscription)
  // =========================================
  return (
    <div className="min-h-screen w-full bg-[#050505] flex items-center justify-center font-sans px-4 py-8">
      <ConfirmToastHost />

      <div
        className={`relative group overflow-hidden transition-all duration-500 ease-in-out p-[2px] w-full max-w-md rounded-[20px] ${isExpanded ? 'max-h-[900px]' : 'max-h-[60px] hover:max-h-[900px]'}`}
        onClick={() => setIsExpanded(true)}
      >
        <div
          className="absolute top-[-50%] left-[-50%] w-[200%] h-[200%] animate-spin"
          style={{ background: 'conic-gradient(from 0deg, transparent 0deg 36deg, #00ffff 36deg 136deg, transparent 136deg 216deg, #ff00ff 216deg 316deg, transparent 316deg 360deg)', animationDuration: '4s' }}
        />
        <div className="relative z-10 bg-[#0f172a] w-full h-full rounded-[18px] flex flex-col">

          {/* Capsule toujours visible */}
          <div className="h-[60px] flex items-center justify-center gap-3 shrink-0 cursor-pointer select-none">
            <Lock size={16} className="text-[#00ffff]" />
            <span className="text-white font-bold uppercase tracking-[0.25em] text-sm">
              {resetToken ? 'Récupération' : isForgotPassword ? 'Récupération' : isLogin ? 'Connexion' : 'Inscription'}
            </span>
          </div>

          {/* Corps — apparaît après expansion */}
          <div className={`transition-all duration-500 delay-150 overflow-y-auto px-8 pb-8 custom-scrollbar ${isExpanded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4 group-hover:opacity-100 group-hover:translate-y-0 pointer-events-none'}`}>

        <div className="flex flex-col items-center mb-8">
          <div className="relative mb-4">
            <div className="absolute inset-[-10px] rounded-[1.75rem] bg-gradient-to-br from-[#0047ff] to-[#00d4ff] opacity-[0.22] blur-xl animate-pulse" />
            <img
              src="/lea-logo.png"
              alt="Léa"
              className="relative w-16 h-16 rounded-2xl shadow-[0_0_45px_rgba(0,212,255,0.45)] object-cover"
            />
          </div>
          <h1 className="text-3xl font-black text-white tracking-tighter">
            LÉA <span className="text-[#00f2ff]">V3</span>
          </h1>
          <p className="text-slate-400 text-[10px] mt-1 font-black uppercase tracking-[0.3em] opacity-60">
            {resetToken ? 'Nouveau Code Secret' : isForgotPassword ? 'Récupération Sécurisée' : isLogin ? 'Identification Requise' : 'Création de Coffre-Fort'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className={`p-3 border rounded-xl text-xs font-bold flex items-center gap-2 animate-in fade-in zoom-in ${error.includes('🔐') || error.includes('✅') ? 'bg-[#00f2ff]/10 border-[#00f2ff]/50 text-[#00f2ff]' : 'bg-red-500/10 border-red-500/50 text-red-400'}`}>
              <Info size={16} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {(!isLogin && !isForgotPassword && !resetToken) && (
            <>
              <div className="flex gap-3">
                <div className="relative group flex-1">
                  <Type className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
                  <input type="text" placeholder="Prénom" value={prenom} onChange={(e) => setPrenom(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
                </div>
                <div className="relative group flex-1">
                  <Type className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
                  <input type="text" placeholder="Nom" value={nom} onChange={(e) => setNom(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
                </div>
              </div>

              <div className="relative group">
                <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
                <input type="date" value={dob} onChange={(e) => setDob(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white/70 rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm [&::-webkit-calendar-picker-indicator]:filter [&::-webkit-calendar-picker-indicator]:invert" />
              </div>
            </>
          )}

          {/* --- SÉLECTION DU GENRE --- */}
                {!isLogin && (
                  <div className="mt-4">
                    <label className="text-slate-400 text-xs font-medium mb-2 flex items-center gap-2">
                      <Sparkles size={14} className="text-[#00f2ff]" /> Nature de l'identité
                    </label>
                    <div className="grid grid-cols-3 gap-2">
                      {[
                        { id: 'homme', label: 'Homme' },
                        { id: 'femme', label: 'Femme' },
                        { id: 'enfant', label: 'Enfant' }
                      ].map((item) => (
                        <button
                          key={item.id}
                          type="button"
                          onClick={() => setGenre(item.id)}
                          className={`py-2 px-3 rounded-lg border text-xs font-medium transition-all ${
                            genre === item.id
                              ? 'bg-[#00f2ff]/10 border-[#00f2ff] text-[#00f2ff] shadow-[0_0_15px_rgba(0,242,255,0.2)]'
                              : 'bg-slate-900/50 border-slate-800 text-slate-500 hover:border-slate-700'
                          }`}
                        >
                          {item.label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

          {(!resetToken) && (
             <div className="relative group">
               <User className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
               <input type="text" placeholder="Pseudo Opérateur" value={pseudo} onChange={(e) => setPseudo(e.target.value)} disabled={!!resetToken} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm disabled:opacity-50" />
             </div>
          )}

          {(!isLogin || isForgotPassword) && !resetToken && (
             <div className="relative group">
               <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
               <input type="email" placeholder="Adresse Email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-3 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
             </div>
          )}

          {(!isForgotPassword || resetToken) && (
            <div className="space-y-4">
              <div className="relative group">
                <Key className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
                <input type={showPassword ? "text" : "password"} placeholder={resetToken ? "Nouveau Code Secret" : "Code Secret"} value={password} onChange={(e) => { setPassword(e.target.value); setForceWeakPwd(false); }} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-12 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-[#00f2ff] transition-colors">
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>

              {/* Jauge de robustesse — visible uniquement à l'inscription */}
              {!isLogin && !isForgotPassword && !resetToken && password.length > 0 && (
                <div className="space-y-1">
                  <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div className={`h-full rounded-full transition-all duration-300 ${pwdStrength.color}`} style={{ width: pwdStrength.width }} />
                  </div>
                  <div className="flex justify-between items-center">
                    <span className={`text-[10px] font-bold uppercase tracking-widest ${pwdStrength.score <= 2 ? 'text-red-400' : pwdStrength.score <= 4 ? 'text-orange-400' : 'text-green-400'}`}>
                      {pwdStrength.label}
                    </span>
                    <span className="text-[9px] text-slate-500">Maj · Min · Chiffre · Symbole recommandés</span>
                  </div>
                </div>
              )}

              {/* Le champ de CONFIRMATION, visible uniquement lors d'une réinitialisation */}
              {resetToken && (
                <div className="relative group animate-in fade-in slide-in-from-top-2">
                  <Key className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-[#00f2ff] transition-colors" size={18} />
                  <input type={showConfirmPassword ? "text" : "password"} placeholder="Confirmer le Code Secret" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className="w-full bg-[#050505] border border-white/10 text-white rounded-lg py-3 pl-11 pr-12 focus:outline-none focus:border-[#00ffff] focus:ring-1 focus:ring-[#00ffff] transition-all text-sm" />
                  <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-[#00f2ff] transition-colors">
                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              )}
            </div>
          )}

          {(!isLogin && !isForgotPassword && !resetToken) && (
            <label className="flex items-start gap-3 px-1 py-1 cursor-pointer group">
              <div className="relative mt-0.5">
                <input type="checkbox" className="sr-only" checked={acceptTerms} onChange={() => setAcceptTerms(!acceptTerms)} />
                <div className={`w-4 h-4 rounded-md border transition-all flex items-center justify-center ${acceptTerms ? 'bg-[#00f2ff] border-[#00f2ff]' : 'border-white/20 bg-white/5'}`}>
                  {acceptTerms && <CheckCircle2 size={14} className="text-[#000814]" />}
                </div>
              </div>
              <span className="text-[11px] text-slate-400 leading-tight group-hover:text-slate-200 transition-colors">
                J'accepte la <span className="text-[#00f2ff] underline cursor-pointer" onClick={(e) => { e.preventDefault(); setShowTerms(true); }}>Charte de Confidentialité</span> de Léa.
              </span>
            </label>
          )}

          {/* BOUTON D'ACCÈS AVEC SÉCURITÉ MAINTENANCE */}
          <button 
            type="submit" 
            disabled={isLoading || (isMaintenance && !resetToken)} 
            className={`w-full mt-4 py-4 font-bold tracking-wide rounded-xl flex items-center justify-center gap-2 transition-all duration-200 [transition-timing-function:cubic-bezier(0.34,1.56,0.64,1)] shadow-2xl
              ${isMaintenance && !resetToken
                ? 'bg-red-600/20 border border-red-500/50 text-red-500 cursor-not-allowed'
                : 'bg-transparent border border-[#00ffff] text-[#00ffff] font-bold hover:bg-[#00ffff] hover:text-black hover:drop-shadow-[0_0_15px_rgba(0,255,255,0.8)] hover:scale-105 active:scale-[0.98] disabled:opacity-40 disabled:scale-100 disabled:hover:bg-transparent disabled:hover:text-[#00ffff]'
              }`}
          >
            {isLoading ? (
              'Transmission...'
            ) : (isMaintenance && !resetToken) ? (
              <>MAINTENANCE EN COURS <ShieldCheck size={18} /></>
            ) : resetToken ? (
              <>Sauvegarder le code <ArrowRight size={18} /></>
            ) : isForgotPassword ? (
              <>Envoyer le pigeon <ArrowRight size={18} /></>
            ) : isLogin ? (
              <>Accéder <ArrowRight size={18} /></>
            ) : (
              <>Créer mon identité <ArrowRight size={18} /></>
            )}
          </button>
        </form>

        <div className="mt-6 flex flex-col gap-3 text-center">
          {(!isForgotPassword && isLogin && !resetToken) && (
            <button type="button" onClick={() => { setIsForgotPassword(true); setError(''); }} className="text-slate-400 text-xs hover:text-[#00f2ff] transition-colors font-medium">
              Mot de passe oublié ?
            </button>
          )}
          
          {!resetToken && (
            <button type="button" onClick={() => { setIsLogin(!isLogin); setIsForgotPassword(false); setError(''); setShowPassword(false); }} className="text-slate-500 text-xs hover:text-white transition-colors font-medium">
              {isForgotPassword ? "Retour à l'identification" : isLogin ? "Nouveau ? Créer un coffre-fort" : "Déjà un profil ? S'identifier"}
            </button>
          )}
        </div>
          </div>
        </div>
      </div>

      {/* ── Modal avertissement mot de passe faible ── */}
      {showWeakPwdWarning && (
        <div className="fixed inset-0 z-[500] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-[#060D1E] border border-red-500/40 rounded-2xl p-6 max-w-sm w-full shadow-[0_0_40px_rgba(239,68,68,0.2)] space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-red-500/15 flex items-center justify-center flex-shrink-0">
                <ShieldCheck size={20} className="text-red-400" />
              </div>
              <div>
                <p className="font-black text-white text-sm uppercase tracking-widest">Mot de passe faible</p>
                <p className="text-[10px] text-red-400 uppercase tracking-widest font-bold">Risque de piratage élevé</p>
              </div>
            </div>
            <p className="text-slate-300 text-xs leading-relaxed">
              Ton mot de passe ne respecte pas les critères de sécurité minimaux. Un pirate pourrait forcer l'accès à ton coffre-fort en quelques secondes.<br/><br/>
              <span className="text-slate-400">Recommandé :</span> <span className="text-[#00f2ff]">12+ caractères, majuscule, chiffre, symbole.</span>
            </p>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => { setShowWeakPwdWarning(false); setPassword(''); }}
                className="w-full py-2.5 bg-[#00f2ff] hover:bg-[#00c8d4] text-black font-black text-xs rounded-xl transition-all uppercase tracking-widest"
              >
                Améliorer mon mot de passe
              </button>
              <button
                onClick={() => { setShowWeakPwdWarning(false); setForceWeakPwd(true); setTimeout(() => document.querySelector<HTMLFormElement>('form')?.requestSubmit(), 0); }}
                className="w-full py-2 text-slate-500 hover:text-red-400 text-[10px] transition-colors font-medium"
              >
                Ignorer et créer quand même (à mes risques et périls)
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};