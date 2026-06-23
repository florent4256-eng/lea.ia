import React, { useState, useEffect, useCallback } from 'react';
import {
  Coins, ShoppingBag, CheckCircle, XCircle, Clock, Copy, Check,
  ExternalLink, Sparkles, Shield, AlertTriangle, RefreshCw, Crown, Star, Zap
} from 'lucide-react';

const SERVER = () => (window as any).LEA_SERVER_URL || '';
const currentUser = () => localStorage.getItem('lea_currentUser') || '';

const PLANS = [
  {
    id: 'free', label: 'Gratuit', icon: <Zap size={14} />,
    monthly: 0, yearly: 0,
    tokens: '50 tokens/mois', color: 'border-slate-600/40', badge: 'text-slate-400',
    features: ['5 générations/jour', 'Accès basique'],
  },
  {
    id: 'ai_plus', label: 'AI Plus', icon: <Star size={14} />,
    monthly: 15.99, yearly: 159.90,
    tokens: '3 000 tokens/mois', color: 'border-cyan-500/40', badge: 'text-cyan-400',
    features: ['10 générations/jour', 'Mémoire étendue', 'Léa Protect Max'],
    popular: true,
  },
  {
    id: 'pro', label: 'Pro', icon: <Crown size={14} />,
    monthly: 24.90, yearly: 249.00,
    tokens: '7 000 tokens/mois', color: 'border-purple-500/40', badge: 'text-purple-400',
    features: ['50 générations/jour', 'Mémoire profonde', 'Serveurs prioritaires'],
  },
];

const TOKEN_PACKS = [
  { id: 'starter',  label: 'Starter',  tokens: 1000,  eur: 5,   badge: 'text-slate-300',   bg: 'bg-slate-500/10  border-slate-500/20',   sel: 'border-slate-400/60  bg-slate-500/15' },
  { id: 'standard', label: 'Standard', tokens: 5000,  eur: 20,  badge: 'text-purple-300',  bg: 'bg-purple-500/10 border-purple-500/20',  sel: 'border-purple-400/60 bg-purple-500/15' },
  { id: 'pro',      label: 'Pro',      tokens: 15000, eur: 50,  badge: 'text-yellow-300',  bg: 'bg-yellow-500/10 border-yellow-500/20',  sel: 'border-yellow-400/60 bg-yellow-500/15' },
  { id: 'mega',     label: 'Mega',     tokens: 50000, eur: 150, badge: 'text-fuchsia-300', bg: 'bg-fuchsia-500/10 border-fuchsia-500/20', sel: 'border-fuchsia-400/60 bg-fuchsia-500/15' },
];

type Payment = {
  id: string; username: string; packId: string; label: string;
  tokens: number; eur: number; method: string; txRef: string;
  status: 'pending' | 'validated' | 'rejected'; submittedAt: string;
  planId?: string; billingCycle?: string;
};

type SelectionType =
  | { kind: 'plan'; plan: typeof PLANS[0]; cycle: 'monthly' | 'yearly' }
  | { kind: 'pack'; pack: typeof TOKEN_PACKS[0] };

export const LeaShop = () => {
  const user = currentUser();
  const server = SERVER();

  const [isAdmin, setIsAdmin]             = useState(false);
  const [userTokens, setUserTokens]       = useState<number | null>(null);
  const [billingCycle, setBillingCycle]   = useState<'monthly' | 'yearly'>('monthly');
  const [selection, setSelection]         = useState<SelectionType | null>(null);

  const [method, setMethod]               = useState<'paypal' | 'solana'>('paypal');
  const [txRef, setTxRef]                 = useState('');
  const [submitting, setSubmitting]       = useState(false);
  const [submitStatus, setSubmitStatus]   = useState<'idle' | 'success' | 'error'>('idle');
  const [submitMsg, setSubmitMsg]         = useState('');

  const [solPrice, setSolPrice]           = useState<number | null>(null);
  const [paypalLink, setPaypalLink]       = useState<string | null>(null);
  const solAddress                        = 'AoffDT318Tp7AS1s2Jin7fDGy22HKZf9mgbYQ4j7siZk';
  const [copiedAddr, setCopiedAddr]       = useState(false);

  const [myPayments, setMyPayments]       = useState<Payment[]>([]);
  const [pendingList, setPendingList]     = useState<Payment[]>([]);
  const [adminTab, setAdminTab]           = useState<'pending' | 'config'>('pending');
  const [paypalInput, setPaypalInput]     = useState('');
  const [savingPaypal, setSavingPaypal]   = useState(false);

  const currentEur = !selection ? 0
    : selection.kind === 'pack' ? selection.pack.eur
    : selection.cycle === 'yearly' ? selection.plan.yearly : selection.plan.monthly;
  const solAmount = solPrice && currentEur > 0 ? (currentEur / solPrice).toFixed(4) : null;

  const select = (s: SelectionType) => { setSelection(s); setSubmitStatus('idle'); setTxRef(''); };
  const clearSelection = () => { setSelection(null); setSubmitStatus('idle'); setTxRef(''); };

  const setCycle = (c: 'monthly' | 'yearly') => {
    setBillingCycle(c);
    if (selection?.kind === 'plan') setSelection({ ...selection, cycle: c });
  };

  const loadConfig = useCallback(async () => {
    try {
      const r = await fetch(`${server}/api/shop/config`);
      if (r.ok) { const d = await r.json(); setSolPrice(d.solPrice); setPaypalLink(d.paypalLink); }
    } catch {}
    if (!user) return;
    try {
      const r = await fetch(`${server}/api/user/profile/${user}`);
      if (r.ok) {
        const d = await r.json();
        if (typeof d.tokens === 'number') setUserTokens(d.tokens);
        if (d.isAdmin || d.abonnement === 'god_mode') setIsAdmin(true);
      }
    } catch {}
  }, [server, user]);

  const loadMyPayments = useCallback(async () => {
    if (!user) return;
    try {
      const r = await fetch(`${server}/api/shop/my-payments/${user}`);
      if (r.ok) setMyPayments(await r.json());
    } catch {}
  }, [server, user]);

  const loadPending = useCallback(async () => {
    if (!isAdmin || !user) return;
    try {
      const r = await fetch(`${server}/api/shop/pending?username=${user}`);
      if (r.ok) setPendingList(await r.json());
    } catch {}
  }, [server, user, isAdmin]);

  useEffect(() => { loadConfig(); loadMyPayments(); }, [loadConfig, loadMyPayments]);
  useEffect(() => { loadPending(); }, [loadPending]);

  const copy = () => {
    navigator.clipboard.writeText(solAddress).then(() => {
      setCopiedAddr(true); setTimeout(() => setCopiedAddr(false), 2000);
    });
  };

  const handleSubmit = async () => {
    if (!txRef.trim() || currentEur === 0 || !selection) return;
    setSubmitting(true); setSubmitStatus('idle');
    try {
      const body: any = { username: user, method, txRef: txRef.trim() };
      if (selection.kind === 'pack') {
        body.packId = selection.pack.id;
      } else {
        body.packId       = `abo_${selection.plan.id}_${selection.cycle}`;
        body.planId       = selection.plan.id;
        body.billingCycle = selection.cycle;
      }
      const r = await fetch(`${server}/api/shop/submit`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
      });
      const d = await r.json();
      if (!r.ok) throw new Error(d.error || 'Erreur serveur');
      setSubmitStatus('success');
      setSubmitMsg(selection.kind === 'pack'
        ? `Paiement soumis ! Tes ${(selection as any).pack.tokens.toLocaleString()} tokens seront crédités dans les 24h.`
        : `Abonnement ${(selection as any).plan.label} soumis ! Ton compte sera mis à jour dans les 24h.`);
      setTxRef('');
      loadMyPayments();
    } catch (e: any) {
      setSubmitStatus('error'); setSubmitMsg(e.message || 'Erreur réseau');
    }
    setSubmitting(false);
  };

  const adminValidate = async (id: string) => {
    try {
      await fetch(`${server}/api/shop/validate/${id}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user }),
      });
      loadPending(); loadConfig();
    } catch {}
  };
  const adminReject = async (id: string) => {
    try {
      await fetch(`${server}/api/shop/reject/${id}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user }),
      });
      loadPending();
    } catch {}
  };
  const savePaypalConfig = async () => {
    if (!paypalInput.trim()) return;
    setSavingPaypal(true);
    try {
      const r = await fetch(`${server}/api/shop/paypal-config`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, link: paypalInput.trim() }),
      });
      if (r.ok) { setPaypalLink(paypalInput.trim()); setPaypalInput(''); }
    } catch {}
    setSavingPaypal(false);
  };

  const statusIcon  = (s: string) => s === 'validated' ? <CheckCircle size={11} className="text-green-400" /> : s === 'rejected' ? <XCircle size={11} className="text-red-400" /> : <Clock size={11} className="text-yellow-400" />;
  const statusLabel = (s: string) => s === 'validated' ? 'Validé' : s === 'rejected' ? 'Rejeté' : 'En attente';
  const statusColor = (s: string) => s === 'validated' ? 'text-green-400' : s === 'rejected' ? 'text-red-400' : 'text-yellow-400';

  const showPayment = !!selection && !(selection.kind === 'plan' && selection.plan.id === 'free');

  return (
    <div className="flex flex-col h-full bg-[#020617] text-slate-200 overflow-y-auto">

      {/* HEADER */}
      <div className="shrink-0 px-5 pt-5 pb-4 border-b border-white/5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-gradient-to-br from-yellow-500 to-orange-500 rounded-xl flex items-center justify-center shadow-[0_0_16px_rgba(234,179,8,0.3)]">
            <ShoppingBag size={16} className="text-white" />
          </div>
          <div>
            <h1 className="font-black text-white uppercase italic tracking-tight text-sm">Léa Shop</h1>
            <p className="text-[9px] uppercase tracking-widest text-slate-500">Abonnements & Tokens</p>
          </div>
        </div>
        {userTokens !== null && (
          <div className="flex items-center gap-1.5 bg-white/5 border border-white/10 px-3 py-1.5 rounded-full">
            <Coins size={11} className="text-yellow-400" />
            <span className="text-yellow-400 font-black text-xs">{userTokens.toLocaleString()} 🪙</span>
          </div>
        )}
      </div>

      <div className="flex-1 px-4 py-4 space-y-6">

        {/* ── ABONNEMENTS ── */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500">⭐ Abonnements</p>
            <div className="flex gap-0.5 p-0.5 bg-white/5 border border-white/10 rounded-lg">
              {(['monthly', 'yearly'] as const).map(c => (
                <button key={c} onClick={() => setCycle(c)}
                  className={`px-3 py-1 rounded-md text-[9px] font-black uppercase tracking-wider transition-all
                    ${billingCycle === c ? 'bg-white/15 text-white' : 'text-slate-600 hover:text-slate-400'}`}>
                  {c === 'monthly' ? 'Mensuel' : 'Annuel −17%'}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2">
            {PLANS.map(plan => {
              const isPlanSelected = selection?.kind === 'plan' && selection.plan.id === plan.id;
              return (
                <button key={plan.id}
                  onClick={() => plan.id === 'free' ? clearSelection() : select({ kind: 'plan', plan, cycle: billingCycle })}
                  className={`relative p-3 rounded-2xl border text-left transition-all
                    ${plan.id !== 'free' ? 'active:scale-95' : 'cursor-default opacity-50'}
                    ${isPlanSelected
                      ? `${plan.color.replace('/40', '/70')} bg-white/6`
                      : `${plan.color} bg-white/3 hover:bg-white/5`}`}>
                  {plan.popular && (
                    <span className="absolute -top-2 left-1/2 -translate-x-1/2 text-[8px] font-black uppercase tracking-widest bg-cyan-500 text-black px-2 py-0.5 rounded-full whitespace-nowrap">Populaire</span>
                  )}
                  <div className={`flex items-center gap-1 mb-1.5 ${plan.badge}`}>
                    {plan.icon}
                    <span className="font-black text-[11px]">{plan.label}</span>
                  </div>
                  <p className="text-base font-black text-white leading-none">
                    {plan.monthly === 0 ? '0€' : `${(billingCycle === 'monthly' ? plan.monthly : plan.yearly).toFixed(2)}€`}
                  </p>
                  <p className="text-[9px] text-slate-600 mb-1">/{billingCycle === 'monthly' ? 'mois' : 'an'}</p>
                  <p className="text-[9px] text-yellow-400 font-bold leading-tight">{plan.tokens}</p>
                  {isPlanSelected && (
                    <div className="absolute top-2 right-2 w-3.5 h-3.5 bg-yellow-500 rounded-full flex items-center justify-center">
                      <Check size={8} className="text-black" />
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* ── SÉPARATEUR ── */}
        <div className="border-t border-white/5" />

        {/* ── PACKS TOKENS ── */}
        <div>
          <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-3">🪙 Recharger des tokens</p>
          <div className="grid grid-cols-2 gap-2.5">
            {TOKEN_PACKS.map(pack => {
              const isPackSelected = selection?.kind === 'pack' && selection.pack.id === pack.id;
              return (
                <button key={pack.id}
                  onClick={() => select({ kind: 'pack', pack })}
                  className={`relative p-4 rounded-2xl border text-left transition-all active:scale-95
                    ${isPackSelected ? pack.sel : `${pack.bg} hover:bg-white/6`}`}>
                  <p className={`text-[9px] font-black uppercase tracking-widest mb-1 ${pack.badge}`}>{pack.label}</p>
                  <p className="text-xl font-black text-white">+{pack.tokens.toLocaleString()}</p>
                  <p className="text-[10px] text-slate-500 mb-1">tokens 🪙</p>
                  <p className="text-base font-black text-yellow-400">{pack.eur} €</p>
                  {userTokens !== null && (
                    <p className="text-[9px] text-slate-500 mt-1.5">
                      Solde : <span className="text-slate-300 font-bold">{(userTokens + pack.tokens).toLocaleString()} 🪙</span>
                    </p>
                  )}
                  {isPackSelected && (
                    <div className="absolute top-2.5 right-2.5 w-4 h-4 bg-yellow-500 rounded-full flex items-center justify-center">
                      <Check size={9} className="text-black" />
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* ── PANNEAU DE PAIEMENT ── */}
        {showPayment && (
          <div className="space-y-4 border-t border-white/5 pt-4">
            <div className="bg-white/3 border border-white/8 rounded-xl px-3 py-2.5 flex items-center justify-between">
              <div>
                <p className="text-white font-black text-sm">
                  {selection?.kind === 'pack'
                    ? `${(selection as any).pack.tokens.toLocaleString()} tokens 🪙`
                    : `Abonnement ${(selection as any).plan?.label}`}
                </p>
                <p className="text-[10px] text-slate-500 mt-0.5">
                  {selection?.kind === 'plan'
                    ? `${billingCycle === 'monthly' ? 'Mensuel' : 'Annuel'} · renouvelable`
                    : 'Achat unique'}
                </p>
              </div>
              <span className="text-yellow-400 font-black text-lg">{currentEur} €</span>
            </div>

            <p className="text-[9px] font-black uppercase tracking-widest text-slate-500">Méthode de paiement</p>
            <div className="flex gap-2 p-1 bg-white/5 border border-white/10 rounded-xl">
              {(['paypal', 'solana'] as const).map(m => (
                <button key={m} onClick={() => { setMethod(m); setSubmitStatus('idle'); }}
                  className={`flex-1 py-2 rounded-lg text-[11px] font-black uppercase tracking-wider transition-all
                    ${method === m ? 'bg-yellow-500 text-black shadow-md' : 'text-slate-500 hover:text-slate-300'}`}>
                  {m === 'paypal' ? '🅿️ PayPal' : '◎ Solana'}
                </button>
              ))}
            </div>

            {method === 'paypal' && (
              <div className="bg-blue-500/8 border border-blue-500/20 rounded-2xl p-4 space-y-3">
                <p className="font-black text-white text-sm">🅿️ Paiement PayPal</p>
                {paypalLink ? (
                  <div className="space-y-2">
                    <p className="text-slate-400 text-xs">
                      Envoie <span className="text-white font-black">{currentEur} €</span> via PayPal, puis entre l'ID de transaction ci-dessous.
                    </p>
                    <div className="flex items-center gap-2">
                      <div className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 font-mono text-[11px] text-blue-300 truncate">{paypalLink}</div>
                      <a href={`${paypalLink}/${currentEur}EUR`} target="_blank" rel="noopener noreferrer"
                        className="w-9 h-9 bg-blue-600 hover:bg-blue-500 rounded-lg flex items-center justify-center transition-all shrink-0">
                        <ExternalLink size={14} className="text-white" />
                      </a>
                    </div>
                    <p className="text-slate-500 text-[10px]">Ajoute ton pseudo (<span className="text-slate-300">{user}</span>) en note de paiement.</p>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-3">
                    <AlertTriangle size={13} className="text-yellow-400 shrink-0" />
                    <p className="text-yellow-300 text-xs">Lien PayPal non configuré. Contacte l'admin.</p>
                  </div>
                )}
              </div>
            )}

            {method === 'solana' && (
              <div className="bg-purple-500/8 border border-purple-500/20 rounded-2xl p-4 space-y-3">
                <p className="font-black text-white text-sm">◎ Paiement Solana</p>
                {solAmount && (
                  <div className="bg-white/5 border border-white/10 rounded-xl p-3 text-center">
                    <p className="text-[10px] text-slate-500 mb-1">Montant à envoyer</p>
                    <p className="text-2xl font-black text-white">{solAmount} <span className="text-purple-400">SOL</span></p>
                    <p className="text-[10px] text-slate-500 mt-1">≈ {currentEur} € · cours {solPrice?.toFixed(2)} €</p>
                  </div>
                )}
                <div>
                  <p className="text-[10px] text-slate-500 mb-1.5">Adresse de réception</p>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 font-mono text-[10px] text-purple-300 break-all">{solAddress}</div>
                    <button onClick={copy}
                      className="w-9 h-9 bg-purple-600 hover:bg-purple-500 rounded-lg flex items-center justify-center transition-all shrink-0 active:scale-90">
                      {copiedAddr ? <Check size={14} className="text-white" /> : <Copy size={14} className="text-white" />}
                    </button>
                  </div>
                </div>
              </div>
            )}

            {(method === 'solana' || (method === 'paypal' && paypalLink)) && (
              <div className="space-y-3">
                <div>
                  <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5">
                    {method === 'solana' ? 'Hash de transaction (Tx Signature)' : 'ID de transaction PayPal'}
                  </p>
                  <input type="text" value={txRef} onChange={e => { setTxRef(e.target.value); setSubmitStatus('idle'); }}
                    placeholder={method === 'solana' ? '5Bt7...xyz' : 'PAYID-xxxx ou référence PayPal'}
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-xs font-mono text-slate-300 outline-none focus:border-yellow-500/50 transition-colors placeholder-slate-600"
                  />
                </div>
                {submitStatus === 'success' && (
                  <div className="bg-green-500/10 border border-green-500/30 rounded-xl p-3 flex items-start gap-2">
                    <CheckCircle size={13} className="text-green-400 shrink-0 mt-0.5" />
                    <p className="text-green-300 text-xs">{submitMsg}</p>
                  </div>
                )}
                {submitStatus === 'error' && (
                  <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2">
                    <XCircle size={13} className="text-red-400 shrink-0 mt-0.5" />
                    <p className="text-red-300 text-xs">{submitMsg}</p>
                  </div>
                )}
                {submitStatus !== 'success' && (
                  <button onClick={handleSubmit} disabled={!txRef.trim() || submitting}
                    className={`w-full py-3.5 rounded-xl font-black uppercase tracking-widest text-xs transition-all flex items-center justify-center gap-2
                      ${!txRef.trim() || submitting
                        ? 'bg-white/5 text-slate-600 cursor-not-allowed'
                        : 'bg-gradient-to-r from-yellow-500 to-orange-500 text-black shadow-[0_0_16px_rgba(234,179,8,0.3)] hover:opacity-90 active:scale-95'}`}>
                    {submitting
                      ? <><div className="w-3 h-3 border-2 border-black/30 border-t-black rounded-full animate-spin" /> Envoi…</>
                      : <><Sparkles size={13} /> Soumettre — {currentEur} €</>}
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* ── MES PAIEMENTS ── */}
        {myPayments.length > 0 && (
          <div className="border-t border-white/5 pt-4">
            <div className="flex items-center justify-between mb-2">
              <p className="text-[9px] font-black uppercase tracking-widest text-slate-500">Mes paiements</p>
              <button onClick={loadMyPayments} className="p-1 text-slate-600 hover:text-white transition-colors"><RefreshCw size={10} /></button>
            </div>
            <div className="space-y-2">
              {myPayments.map(p => (
                <div key={p.id} className="bg-white/3 border border-white/8 rounded-xl px-3 py-2.5 flex items-center gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-white text-xs font-bold">{p.label}</span>
                      {p.tokens > 0 && <span className="text-yellow-400 text-[10px]">+{p.tokens.toLocaleString()} 🪙</span>}
                    </div>
                    <p className="text-slate-600 text-[9px] font-mono truncate mt-0.5">{p.method} · {p.txRef}</p>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {statusIcon(p.status)}
                    <span className={`text-[9px] font-bold ${statusColor(p.status)}`}>{statusLabel(p.status)}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ── PANNEAU ADMIN ── */}
        {isAdmin && (
          <div className="border border-yellow-500/20 rounded-2xl overflow-hidden">
            <div className="bg-yellow-500/8 px-4 py-3 flex items-center gap-2 border-b border-yellow-500/20">
              <Shield size={13} className="text-yellow-400" />
              <span className="text-yellow-400 font-black text-xs uppercase tracking-widest">Panneau Admin</span>
              <div className="flex-1" />
              <div className="flex gap-1 p-0.5 bg-white/5 rounded-lg">
                {(['pending', 'config'] as const).map(t => (
                  <button key={t} onClick={() => { setAdminTab(t); if (t === 'pending') loadPending(); }}
                    className={`px-2.5 py-1 rounded-md text-[9px] font-black uppercase tracking-wider transition-all
                      ${adminTab === t ? 'bg-yellow-500 text-black' : 'text-slate-500 hover:text-white'}`}>
                    {t === 'pending' ? `En attente (${pendingList.length})` : 'Config PayPal'}
                  </button>
                ))}
              </div>
              <button onClick={loadPending} className="p-1 text-slate-500 hover:text-yellow-400 transition-colors ml-1"><RefreshCw size={11} /></button>
            </div>

            {adminTab === 'pending' && (
              <div className="p-3 space-y-2">
                {pendingList.length === 0
                  ? <p className="text-center text-slate-600 text-xs py-4">Aucun paiement en attente</p>
                  : pendingList.map(p => (
                    <div key={p.id} className="bg-white/3 border border-white/8 rounded-xl p-3 space-y-2">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-white text-xs font-bold">{p.username}</span>
                          <span className="text-slate-500 text-[10px] ml-2">· {p.label} · {p.eur}€ · {p.method}</span>
                        </div>
                        {p.tokens > 0 && <span className="text-yellow-400 text-xs font-black">+{p.tokens.toLocaleString()} 🪙</span>}
                      </div>
                      {p.planId && <p className="text-cyan-400 text-[10px] font-bold">Abo : {p.planId} ({p.billingCycle})</p>}
                      <div className="bg-black/30 rounded-lg px-2.5 py-1.5">
                        <p className="text-[9px] text-slate-500 mb-0.5">Référence</p>
                        <p className="font-mono text-[10px] text-slate-300 break-all">{p.txRef}</p>
                      </div>
                      <p className="text-[9px] text-slate-600">{new Date(p.submittedAt).toLocaleString('fr-FR')}</p>
                      <div className="flex gap-2">
                        <button onClick={() => adminValidate(p.id)}
                          className="flex-1 py-2 bg-green-500/20 border border-green-500/30 text-green-400 rounded-lg text-[10px] font-black uppercase tracking-wider hover:bg-green-500/30 transition-all flex items-center justify-center gap-1">
                          <CheckCircle size={11} /> Valider
                        </button>
                        <button onClick={() => adminReject(p.id)}
                          className="flex-1 py-2 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg text-[10px] font-black uppercase tracking-wider hover:bg-red-500/20 transition-all flex items-center justify-center gap-1">
                          <XCircle size={11} /> Rejeter
                        </button>
                      </div>
                    </div>
                  ))
                }
              </div>
            )}

            {adminTab === 'config' && (
              <div className="p-4 space-y-3">
                {paypalLink && (
                  <div className="bg-white/5 border border-white/10 rounded-xl p-3">
                    <p className="text-[9px] text-slate-500 mb-1">Lien PayPal actuel (chiffré en base)</p>
                    <p className="text-blue-300 text-xs font-mono break-all">{paypalLink}</p>
                  </div>
                )}
                <div>
                  <p className="text-[9px] font-black uppercase tracking-widest text-slate-500 mb-1.5">
                    {paypalLink ? 'Mettre à jour' : 'Configurer ton lien PayPal.Me'}
                  </p>
                  <div className="flex gap-2">
                    <input type="text" value={paypalInput} onChange={e => setPaypalInput(e.target.value)}
                      placeholder="https://paypal.me/tonpseudo"
                      className="flex-1 bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-xs text-slate-300 outline-none focus:border-yellow-500/50 transition-colors font-mono"
                    />
                    <button onClick={savePaypalConfig} disabled={!paypalInput.trim() || savingPaypal}
                      className="px-4 py-2 bg-yellow-500 hover:bg-yellow-400 text-black text-xs font-black rounded-xl transition-all disabled:opacity-40 active:scale-95">
                      {savingPaypal ? '…' : 'Sauver'}
                    </button>
                  </div>
                  <p className="text-[9px] text-slate-600 mt-1.5">Chiffré AES-256 · jamais en clair dans la base</p>
                </div>
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
};
