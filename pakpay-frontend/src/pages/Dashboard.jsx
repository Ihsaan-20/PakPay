import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { 
  Send, Plus, LogOut, Bell, Loader2, Lock,
  ArrowUpRight, ArrowDownLeft, History, LogIn, QrCode
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { walletService, addMoneyService } from '../services/api';
import { useToast } from '../components/ToastProvider';
import {
  getApiErrorMessage,
  normalizeMobile,
  validateAmount,
  validateMobile,
  validateTransactionPin,
  isCreditTransaction,
} from '../utils/validation';
import AddMoneyModal from '../components/AddMoneyModal';

const isAuthed = () => !!localStorage.getItem('token');

const isTransactionPinSet = () =>
  String(localStorage.getItem('isPinSet') || '').toLowerCase() === 'yes';

/** Normalize wallet history API body (array or wrapped). */
const parseHistoryPayload = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  return payload?.content ?? payload?.transactions ?? [];
};

/** Newest transaction first by `date`. */
const pickLatestTx = (list) => {
  if (!Array.isArray(list) || list.length === 0) return null;
  const sorted = [...list].sort((a, b) => {
    const ta = new Date(a.date || 0).getTime();
    const tb = new Date(b.date || 0).getTime();
    return tb - ta;
  });
  return sorted[0];
};

const Dashboard = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  // --- 1. States ---
  const [userData, setUserData] = useState({
    name: localStorage.getItem('fullName') || "User",
    mobile: localStorage.getItem('mobileNumber') || "",
    balance: localStorage.getItem('balance') || "0",
    walletID: localStorage.getItem('walletID') || "PK-PAY-XXXXXX"
  });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [step, setStep] = useState(1); 
  const [loading, setLoading] = useState(false);
  const [receiverName, setReceiverName] = useState(""); 
  const [txData, setTxData] = useState({ receiver: '', amount: '', pin: '' });

  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState(null);

  const [loginPromptOpen, setLoginPromptOpen] = useState(false);

  const [lastTx, setLastTx] = useState(null);
  const [lastTxLoading, setLastTxLoading] = useState(false);

  const [pinSetupOpen, setPinSetupOpen] = useState(false);
  const [pinSetup, setPinSetup] = useState({ pin: '', confirm: '' });
  const [pinSetupLoading, setPinSetupLoading] = useState(false);

  const [isQrOpen, setIsQrOpen] = useState(false);
  const [qrLoading, setQrLoading] = useState(false);
  const [qrError, setQrError] = useState(null);
  const [qrString, setQrString] = useState("");

  const [addMoneyOpen, setAddMoneyOpen] = useState(false);

  const fetchProfile = useCallback(async () => {
    if (!isAuthed()) return;
    try {
      const res = await addMoneyService.getProfile();
      if (res.data?.fullName) {
        setUserData((prev) => ({ ...prev, name: res.data.fullName }));
        localStorage.setItem('fullName', res.data.fullName);
      }
    } catch {
      /* keep stored name */
    }
  }, []);

  // --- 2. Real-Time Balance Fetch Logic ---
  const fetchBalance = useCallback(async () => {
    if (!isAuthed()) return;
    try {
      const res = await walletService.getRealTimeBalance();
      
      // Agar API direct value bhej rahi hai (e.g. 335106.00)
      // Ya agar object bhej rahi hai { balance: 335106.00 }
      const freshBalance = res.data.balance !== undefined ? res.data.balance : res.data;
      
      setUserData(prev => ({ 
        ...prev, 
        balance: parseFloat(freshBalance) // Forcefully number mein convert karein
      }));
      
      // Sync ke liye storage update kar dein magar display API wala hi hoga
      localStorage.setItem('balance', freshBalance);
    } catch (err) {
      console.error("API se balance nahi mil saka:", err);
    }
  }, []);

  const fetchLastTransaction = useCallback(async () => {
    if (!isAuthed()) return;
    setLastTxLoading(true);
    try {
      const res = await walletService.getHistory();
      const list = parseHistoryPayload(res.data);
      setLastTx(pickLatestTx(list));
    } catch (err) {
      console.error("Last transaction fetch failed:", err);
      setLastTx(null);
    } finally {
      setLastTxLoading(false);
    }
  }, []);

  const requireAuth = () => {
    if (!isAuthed()) {
      setLoginPromptOpen(true);
      return false;
    }
    return true;
  };

  const openTransferModal = () => {
    setTxData({ receiver: '', amount: '', pin: '' });
    setReceiverName('');
    setStep(1);
    setIsModalOpen(true);
  };

  const handleSendClick = () => {
    if (!requireAuth()) return;
    if (!isTransactionPinSet()) {
      setPinSetup({ pin: '', confirm: '' });
      setPinSetupOpen(true);
      return;
    }
    openTransferModal();
  };

  const handlePinSetupSubmit = async (e) => {
    e.preventDefault();
    if (!requireAuth()) return;
    const p = pinSetup.pin.replace(/\D/g, '');
    const c = pinSetup.confirm.replace(/\D/g, '');
    if (p.length !== 4) {
      return showToast('4-digit transaction PIN enter karein.', 'warning');
    }
    if (p !== c) {
      return showToast('PIN match nahi ho rahi.', 'warning');
    }
    setPinSetupLoading(true);
    try {
      await walletService.setTransactionPin(p);
      localStorage.setItem('isPinSet', 'yes');
      showToast('Transaction PIN set ho gaya.', 'success');
      setPinSetupOpen(false);
      setPinSetup({ pin: '', confirm: '' });
      openTransferModal();
    } catch (err) {
      showToast(
        err.response?.data?.message ||
          err.response?.data?.error ||
          'PIN set nahi ho saka. Dobara try karein.',
        'error'
      );
    } finally {
      setPinSetupLoading(false);
    }
  };

  const goToLogin = () => {
    setLoginPromptOpen(false);
    localStorage.clear();
    navigate('/login', { replace: true });
  };

  const dismissLoginPrompt = () => {
    setLoginPromptOpen(false);
    if (!isAuthed()) {
      navigate('/login', { replace: true });
    }
  };

  // Page load: taaza balance; agar token na ho to login
  useEffect(() => {
    if (!isAuthed()) {
      navigate('/login', { replace: true });
      return;
    }
    fetchBalance();
    fetchLastTransaction();
    fetchProfile();
  }, [fetchBalance, fetchLastTransaction, fetchProfile, navigate]);

  // Doosri device/browser par balance update: focus, tab visible, polling
  // Same machine do tabs: localStorage 'balance' sync
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState === 'visible') {
        fetchBalance();
        fetchLastTransaction();
      }
    };
    const onFocus = () => {
      fetchBalance();
      fetchLastTransaction();
    };
    const onStorage = (e) => {
      if (e.key === 'balance' && e.newValue != null && e.newValue !== '') {
        const n = parseFloat(e.newValue);
        if (!Number.isNaN(n)) {
          setUserData((prev) => ({ ...prev, balance: n }));
        }
      }
    };
    const onSessionExpired = () => setLoginPromptOpen(true);

    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('focus', onFocus);
    window.addEventListener('storage', onStorage);
    window.addEventListener('pakpay:session-expired', onSessionExpired);
    const pollId = setInterval(fetchBalance, 30000);

    return () => {
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('focus', onFocus);
      window.removeEventListener('storage', onStorage);
      window.removeEventListener('pakpay:session-expired', onSessionExpired);
      clearInterval(pollId);
    };
  }, [fetchBalance, fetchLastTransaction, fetchProfile]);

  // --- 3. Handlers ---
  const handleLogout = () => {
    if (!isAuthed()) {
      goToLogin();
      return;
    }
    localStorage.clear();
    navigate('/login', { replace: true });
  };

  const handleVerifyNumber = async () => {
    if (!requireAuth()) return;
    if (!isTransactionPinSet()) {
      setPinSetupOpen(true);
      return;
    }
    const receiverMobile = normalizeMobile(txData.receiver);
    const mobileErr = validateMobile(receiverMobile);
    if (mobileErr) {
      return showToast(mobileErr, 'warning');
    }
    if (receiverMobile === normalizeMobile(userData.mobile)) {
      return showToast('Apne khud ke number par transfer nahi kar sakte.', 'warning');
    }

    setLoading(true);

    try {
      const res = await walletService.checkReceiver(receiverMobile);

      setReceiverName(res.data.fullName);
      setStep(2);

    } catch (err) {
      console.log(err);

      // backend error message
      showToast(getApiErrorMessage(err, 'User nahi mila!'), 'error');

    } finally {
      setLoading(false);
    }
  };

  const openMyQr = async () => {
    if (!requireAuth()) return;
    setIsQrOpen(true);
    setQrLoading(true);
    setQrError(null);
    setQrString("");
    try {
      const mobile = userData.mobile || localStorage.getItem('mobileNumber') || '';
      const name = userData.name || localStorage.getItem('fullName') || '';
      if (!mobile || !name) {
        throw new Error('Mobile/name missing');
      }
      const res = await walletService.getMyQr(mobile, name);
      const d = res.data?.data ?? res.data;
      setQrString(d.qrString || '');
    } catch (err) {
      setQrError(err?.response?.data?.message || err?.message || 'QR load failed');
    } finally {
      setQrLoading(false);
    }
  };

  const openHistory = async () => {
    if (!requireAuth()) return;
    setIsHistoryOpen(true);
    setHistoryError(null);
    setHistoryLoading(true);
    try {
      const res = await walletService.getHistory();
      const list = parseHistoryPayload(res.data);
      setHistory(list);
      setLastTx(pickLatestTx(list));
    } catch (err) {
      setHistoryError(err?.response?.data?.message || err?.message || "History load failed");
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleTransfer = async (e) => {
    e.preventDefault();
    if (!requireAuth()) return;
    if (!isTransactionPinSet()) {
      setIsModalOpen(false);
      setPinSetupOpen(true);
      return;
    }
    const amountErr = validateAmount(txData.amount);
    const pinErr = validateTransactionPin(txData.pin);
    if (amountErr) return showToast(amountErr, 'warning');
    if (pinErr) return showToast(pinErr, 'warning');

    setLoading(true);
    try {
      await walletService.secureTransfer(
        normalizeMobile(txData.receiver),
        txData.amount,
        txData.pin.replace(/\D/g, '')
      );
      showToast(`Rs. ${Number(txData.amount).toLocaleString()} transfer successful.`, 'success');
      
      setIsModalOpen(false);
      setStep(1);
      
      // CRITICAL: Transaction ke foran baad balance refresh karo
      await fetchBalance();
      await fetchLastTransaction();

    } catch (err) {
      showToast(getApiErrorMessage(err, 'Transfer fail ho gaya.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  let activityRow;
  if (lastTxLoading) {
    activityRow = (
      <div className="flex items-center justify-center gap-3 p-8 bg-white/[0.02] border border-white/5 rounded-[2rem]">
        <Loader2 className="animate-spin text-indigo-400" size={22} />
        <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">Loading activity…</span>
      </div>
    );
  } else if (lastTx) {
    const isCredit = isCreditTransaction(lastTx.type);
    const amountPrefix = isCredit ? "+" : "-";
    const statusOk = String(lastTx.status || "").toUpperCase() === "SUCCESS";
    const when = lastTx.date
      ? new Date(lastTx.date).toLocaleString(undefined, {
          dateStyle: "medium",
          timeStyle: "short",
        })
      : "";
    activityRow = (
      <div className="flex items-center justify-between p-5 bg-white/[0.02] border border-white/5 rounded-[2rem]">
        <div className="flex items-center gap-4 min-w-0">
          <div className="h-12 w-12 shrink-0 bg-white/5 rounded-2xl flex items-center justify-center text-slate-500">
            {isCredit ? <ArrowDownLeft className="text-emerald-400" /> : <ArrowUpRight />}
          </div>
          <div className="min-w-0">
            <h4 className="font-bold text-sm truncate">{lastTx.otherPartyMobile || "Transaction"}</h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase truncate">{when || "—"}</p>
          </div>
        </div>
        <div className="text-right shrink-0 pl-3">
          <span className={`font-black text-sm block ${isCredit ? "text-emerald-400" : "text-white"}`}>
            {amountPrefix} Rs. {Number(lastTx.amount).toLocaleString()}
          </span>
          <span className={`text-[10px] font-black uppercase mt-1 inline-block ${statusOk ? "text-emerald-500/90" : "text-amber-400"}`}>
            {lastTx.status || "—"}
          </span>
        </div>
      </div>
    );
  } else {
    activityRow = (
      <div className="flex items-center justify-between p-5 bg-white/[0.02] border border-white/5 rounded-[2rem]">
        <div className="flex items-center gap-4">
          <div className="h-12 w-12 bg-white/5 rounded-2xl flex items-center justify-center text-slate-600"><ArrowUpRight /></div>
          <div>
            <h4 className="font-bold text-sm">Last Transaction</h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase">No activity yet</p>
          </div>
        </div>
        <span className="font-black text-sm text-slate-600">—</span>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050505] text-white selection:bg-indigo-500/30">
      <div className="fixed top-[-10%] right-[-10%] w-[60%] h-[60%] bg-indigo-600/10 blur-[130px] rounded-full pointer-events-none"></div>

      <main className="max-w-lg mx-auto px-6 py-8 relative z-10">
        
        {/* Header */}
        <header className="flex justify-between items-center mb-10">
          <div>
            <p className="text-slate-500 text-[10px] font-black uppercase tracking-[0.2em]">Premium Member</p>
            <h2 className="text-2xl font-black tracking-tight leading-tight">{userData.name} 👋</h2>
          </div>
          <div className="flex gap-3">
             <button
               type="button"
               onClick={() => {
                 if (!requireAuth()) return;
               }}
               className="h-12 w-12 bg-white/5 border border-white/10 rounded-2xl flex items-center justify-center text-slate-300 hover:bg-white/10 transition-all"
             >
               <Bell size={20} />
             </button>
             <button type="button" onClick={handleLogout} className="h-12 w-12 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-center justify-center text-red-500 hover:bg-red-500 transition-all"><LogOut size={20} /></button>
          </div>
        </header>

        {/* Balance Card - Now using REAL-TIME data */}
        <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="bg-gradient-to-br from-indigo-600 to-violet-700 p-8 rounded-[3rem] mb-10 shadow-2xl shadow-indigo-500/20 relative overflow-hidden">
          <div className="relative z-10">
            <p className="text-indigo-100/60 text-[10px] font-black uppercase tracking-widest mb-2">Portfolio Balance</p>
            <h3 className="text-5xl font-black tracking-tighter mb-12">
              Rs. {Number(userData.balance).toLocaleString()}
            </h3>
            <div className="flex justify-between items-center">
              <span className="text-sm font-mono text-indigo-100/80 tracking-widest">{userData.walletID}</span>
              <div className="h-8 w-12 bg-white/10 rounded-lg flex items-center justify-center font-black italic text-[8px] border border-white/10">VISA</div>
            </div>
          </div>
        </motion.div>

        {/* Action Buttons */}
        <div className="space-y-4 mb-12">
          <div className="grid grid-cols-2 gap-4">
            <button
              type="button"
              onClick={handleSendClick}
              className="bg-white/[0.03] border border-white/10 p-8 rounded-[2.5rem] flex flex-col items-center gap-4 hover:bg-white/[0.06] transition-all group"
            >
              <div className="h-14 w-14 bg-indigo-600 rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform text-white"><Send size={24}/></div>
              <span className="text-xs font-black uppercase tracking-widest text-slate-300">Send</span>
            </button>
            <button
              type="button"
              onClick={() => {
                if (!requireAuth()) return;
                setAddMoneyOpen(true);
              }}
              className="bg-white/[0.03] border border-white/10 p-8 rounded-[2.5rem] flex flex-col items-center gap-4 hover:bg-white/[0.06] transition-all group text-white"
            >
              <div className="h-14 w-14 bg-emerald-500 rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform"><Plus size={24}/></div>
              <span className="text-xs font-black uppercase tracking-widest text-slate-300">Add Money</span>
            </button>
          </div>
          <button
            type="button"
            onClick={openMyQr}
            className="w-full bg-white/[0.03] border border-white/10 py-5 px-6 rounded-[2.5rem] flex items-center justify-center gap-3 hover:bg-white/[0.06] transition-all group"
          >
            <div className="h-12 w-12 bg-violet-600 rounded-2xl flex items-center justify-center shadow-lg group-hover:scale-105 transition-transform text-white">
              <QrCode size={22} />
            </div>
            <span className="text-xs font-black uppercase tracking-widest text-slate-300">My QR</span>
          </button>
        </div>

        {/* Recent Activity (Placeholder) */}
        <div className="space-y-6">
          <div className="flex items-end justify-between gap-4">
            <h3 className="text-lg font-black tracking-tight underline decoration-indigo-500 decoration-4 underline-offset-8">Activity</h3>
            <button
              type="button"
              onClick={openHistory}
              className="shrink-0 flex items-center gap-2 px-4 py-3 bg-white/[0.03] border border-white/10 rounded-2xl text-[10px] font-black uppercase tracking-widest text-slate-300 hover:bg-white/[0.06] transition-all"
            >
              <History size={16} className="text-indigo-400" />
              History
            </button>
          </div>
          <div className="space-y-4">
             {activityRow}
          </div>
        </div>

        {/* --- SET TRANSACTION PIN (before first send) --- */}
        <AnimatePresence>
          {pinSetupOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setPinSetupOpen(false)}
                className="fixed inset-0 bg-black/90 backdrop-blur-md z-[72]"
              />
              <motion.div
                initial={{ y: '100%' }}
                animate={{ y: 0 }}
                exit={{ y: '100%' }}
                className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0a0a0a] border-t border-white/10 rounded-t-[3.5rem] p-10 z-[73] shadow-2xl"
              >
                <div className="flex flex-col items-center mb-8">
                  <div className="h-14 w-14 bg-indigo-600/20 border border-indigo-500/30 rounded-3xl flex items-center justify-center text-indigo-400 mb-4">
                    <Lock size={28} />
                  </div>
                  <h3 className="text-xl font-black italic uppercase text-center">Set transaction PIN</h3>
                  <p className="text-sm text-slate-500 font-bold text-center mt-3 leading-relaxed">
                    Pehle 4-digit PIN set karein, phir Send use kar sakte hain.
                  </p>
                </div>
                <form onSubmit={handlePinSetupSubmit} className="space-y-6">
                  <div className="grid grid-cols-2 gap-4">
                    <input
                      type="password"
                      inputMode="numeric"
                      autoComplete="new-password"
                      maxLength={4}
                      required
                      placeholder="PIN"
                      value={pinSetup.pin}
                      onChange={(e) => setPinSetup({ ...pinSetup, pin: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                      className="w-full bg-white/5 border border-white/10 rounded-3xl py-5 px-6 text-center tracking-[0.5em] font-black outline-none focus:border-indigo-500"
                    />
                    <input
                      type="password"
                      inputMode="numeric"
                      autoComplete="new-password"
                      maxLength={4}
                      required
                      placeholder="Confirm"
                      value={pinSetup.confirm}
                      onChange={(e) =>
                        setPinSetup({ ...pinSetup, confirm: e.target.value.replace(/\D/g, '').slice(0, 4) })
                      }
                      className="w-full bg-white/5 border border-white/10 rounded-3xl py-5 px-6 text-center tracking-[0.5em] font-black outline-none focus:border-indigo-500"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={pinSetupLoading}
                    className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black flex items-center justify-center gap-3"
                  >
                    {pinSetupLoading ? <Loader2 className="animate-spin" /> : 'Save PIN & continue'}
                  </button>
                </form>
              </motion.div>
            </>
          )}
        </AnimatePresence>

        {/* --- MODAL --- */}
        <AnimatePresence>
          {isModalOpen && (
            <>
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setIsModalOpen(false)} className="fixed inset-0 bg-black/90 backdrop-blur-md z-[60]" />
              <motion.div initial={{ y: "100%" }} animate={{ y: 0 }} exit={{ y: "100%" }} className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0a0a0a] border-t border-white/10 rounded-t-[3.5rem] p-10 z-[70] shadow-2xl">
                
                <form onSubmit={handleTransfer} className="space-y-8">
                  {step === 1 ? (
                    <div className="space-y-6">
                      <h3 className="text-xl font-black italic uppercase">Verify Receiver</h3>
                      <input type="text" required placeholder="Mobile Number" className="w-full bg-white/5 border border-white/10 rounded-3xl py-6 px-8 outline-none focus:border-indigo-500 text-white text-2xl font-black tracking-widest" onChange={(e) => setTxData({...txData, receiver: e.target.value})} />
                      <button type="button" onClick={handleVerifyNumber} disabled={loading} className="w-full bg-white text-black py-6 rounded-3xl font-black flex items-center justify-center gap-3">
                        {loading ? <Loader2 className="animate-spin" /> : "Verify Account"}
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-6">
                      <div className="bg-indigo-600/10 p-5 rounded-[2rem] border border-indigo-500/20">
                        <p className="text-[10px] font-bold text-indigo-400 uppercase tracking-widest">Sending to</p>
                        <p className="font-black text-lg text-white">{receiverName}</p>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <input type="number" required placeholder="Amount" className="w-full bg-white/5 border border-white/10 rounded-3xl py-5 px-6 font-black" onChange={(e) => setTxData({...txData, amount: e.target.value})} />
                        <input type="password" required maxLength="4" placeholder="PIN" className="w-full bg-white/5 border border-white/10 rounded-3xl py-5 px-6 text-center tracking-[1em]" onChange={(e) => setTxData({...txData, pin: e.target.value})} />
                      </div>
                      <button type="submit" disabled={loading} className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black flex items-center justify-center gap-3">
                        {loading ? <Loader2 className="animate-spin" /> : "Confirm Payment"}
                      </button>
                    </div>
                  )}
                </form>
              </motion.div>
            </>
          )}
        </AnimatePresence>

        {/* --- HISTORY MODAL --- */}
        <AnimatePresence>
          {isHistoryOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setIsHistoryOpen(false)}
                className="fixed inset-0 bg-black/90 backdrop-blur-md z-[60]"
              />
              <motion.div
                initial={{ y: "100%" }}
                animate={{ y: 0 }}
                exit={{ y: "100%" }}
                className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0a0a0a] border-t border-white/10 rounded-t-[3.5rem] p-10 z-[70] shadow-2xl max-h-[85vh] flex flex-col"
              >
                <h3 className="text-xl font-black italic uppercase mb-6 shrink-0">Transaction history</h3>
                <div className="overflow-y-auto flex-1 min-h-0 space-y-4 pr-1 -mr-1">
                  {historyLoading ? (
                    <div className="flex justify-center py-16">
                      <Loader2 className="animate-spin text-indigo-400" size={32} />
                    </div>
                  ) : historyError ? (
                    <p className="text-center text-sm text-red-400 font-bold py-8">{historyError}</p>
                  ) : history.length === 0 ? (
                    <p className="text-center text-sm text-slate-500 font-bold uppercase tracking-widest py-8">No transactions yet</p>
                  ) : (
                    history.map((item) => {
                      const isCredit = isCreditTransaction(item.type);
                      const amountPrefix = isCredit ? "+" : "-";
                      const statusOk = String(item.status || "").toUpperCase() === "SUCCESS";
                      const when = item.date
                        ? new Date(item.date).toLocaleString(undefined, {
                            dateStyle: "medium",
                            timeStyle: "short",
                          })
                        : "—";
                      return (
                        <div
                          key={item.trxId || `${item.date}-${item.amount}`}
                          className="flex items-center justify-between p-5 bg-white/[0.02] border border-white/5 rounded-[2rem]"
                        >
                          <div className="flex items-center gap-4 min-w-0">
                            <div className="h-12 w-12 shrink-0 bg-white/5 rounded-2xl flex items-center justify-center text-slate-500">
                              {isCredit ? <ArrowDownLeft className="text-emerald-400" /> : <ArrowUpRight />}
                            </div>
                            <div className="min-w-0">
                              <h4 className="font-bold text-sm truncate">{item.otherPartyMobile || "Transaction"}</h4>
                              <p className="text-[10px] text-slate-500 font-bold uppercase truncate">{when}</p>
                              <p className="text-[9px] text-slate-600 font-mono mt-1 truncate">{item.trxId}</p>
                            </div>
                          </div>
                          <div className="text-right shrink-0 pl-3">
                            <span className={`font-black text-sm block ${isCredit ? "text-emerald-400" : "text-white"}`}>
                              {amountPrefix} Rs. {Number(item.amount).toLocaleString()}
                            </span>
                            <span
                              className={`text-[10px] font-black uppercase mt-1 inline-block ${
                                statusOk ? "text-emerald-500/90" : "text-amber-400"
                              }`}
                            >
                              {item.status || "—"}
                            </span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </motion.div>
            </>
          )}
        </AnimatePresence>

        {/* --- MY QR MODAL --- */}
        <AnimatePresence>
          {isQrOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setIsQrOpen(false)}
                className="fixed inset-0 bg-black/90 backdrop-blur-md z-[62]"
              />
              <motion.div
                initial={{ y: '100%' }}
                animate={{ y: 0 }}
                exit={{ y: '100%' }}
                className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0a0a0a] border-t border-white/10 rounded-t-[3.5rem] p-10 z-[71] shadow-2xl flex flex-col items-center"
              >
                <h3 className="text-xl font-black italic uppercase mb-2 self-start w-full">Receive via QR</h3>
                <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-8 self-start w-full">
                  Scan to pay this wallet
                </p>
                {qrLoading ? (
                  <div className="flex flex-col items-center gap-4 py-16">
                    <Loader2 className="animate-spin text-indigo-400" size={40} />
                    <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">Loading QR…</span>
                  </div>
                ) : qrError ? (
                  <p className="text-center text-sm text-red-400 font-bold py-8">{qrError}</p>
                ) : qrString ? (
                  <>
                    <div className="p-4 rounded-[2rem] bg-white border border-white/20 shadow-2xl shadow-indigo-500/10 mb-6">
                      <QRCodeSVG value={qrString} size={220} />
                    </div>
                    <p className="text-sm font-mono font-black text-slate-300 tracking-wide mb-2">
                      {userData.mobile || localStorage.getItem('mobileNumber') || ''}
                    </p>
                    <p className="text-[10px] text-slate-500 font-bold uppercase text-center max-w-xs leading-relaxed">
                      SCAN TO PAY
                    </p>
                  </>
                ) : (
                  <p className="text-center text-sm text-slate-500 font-bold py-8">QR data missing</p>
                )}
              </motion.div>
            </>
          )}
        </AnimatePresence>

        {/* --- LOGIN REQUIRED / SESSION --- */}
        <AnimatePresence>
          {loginPromptOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={dismissLoginPrompt}
                className="fixed inset-0 bg-black/90 backdrop-blur-md z-[80]"
              />
              <motion.div
                initial={{ y: "100%" }}
                animate={{ y: 0 }}
                exit={{ y: "100%" }}
                className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0a0a0a] border-t border-white/10 rounded-t-[3.5rem] p-10 z-[90] shadow-2xl"
              >
                <div className="flex flex-col items-center text-center space-y-6">
                  <div className="h-16 w-16 bg-indigo-600/20 border border-indigo-500/30 rounded-3xl flex items-center justify-center text-indigo-400">
                    <LogIn size={32} />
                  </div>
                  <div>
                    <h3 className="text-xl font-black italic uppercase">Login required</h3>
                    <p className="text-sm text-slate-500 font-bold mt-3 leading-relaxed">
                      Please sign in to use wallet features, transfers, and notifications.
                    </p>
                  </div>
                  <div className="flex flex-col w-full gap-3">
                    <button
                      type="button"
                      onClick={goToLogin}
                      className="w-full bg-indigo-600 text-white py-5 rounded-3xl font-black"
                    >
                      Go to login
                    </button>
                    <button
                      type="button"
                      onClick={dismissLoginPrompt}
                      className="w-full bg-white/5 border border-white/10 text-slate-300 py-4 rounded-3xl font-black text-xs uppercase tracking-widest"
                    >
                      Close
                    </button>
                  </div>
                </div>
              </motion.div>
            </>
          )}
        </AnimatePresence>

        <AddMoneyModal
          open={addMoneyOpen}
          onClose={() => setAddMoneyOpen(false)}
          onSuccess={(newBalance) => {
            if (newBalance != null) {
              setUserData((prev) => ({ ...prev, balance: parseFloat(newBalance) }));
              localStorage.setItem('balance', String(newBalance));
            }
            fetchBalance();
            fetchLastTransaction();
          }}
        />
      </main>
    </div>
  );
};

export default Dashboard;