import React, { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Building2, ChevronDown, Loader2, Link2, Wallet, X } from 'lucide-react';
import { addMoneyService } from '../services/api';
import { useToast } from './ToastProvider';
import { getApiErrorMessage, validateAmount } from '../utils/validation';
import BankLogo from './BankLogo';
import { PAKISTANI_BANKS, getBankByCode } from '../data/pakistaniBanks';

const VIEWS = {
  ACCOUNTS: 'accounts',
  LINK: 'link',
  OTP: 'otp',
  DEPOSIT: 'deposit',
};

export default function AddMoneyModal({ open, onClose, onSuccess }) {
  const { showToast } = useToast();
  const [view, setView] = useState(VIEWS.ACCOUNTS);
  const [loading, setLoading] = useState(false);
  const [banks, setBanks] = useState(PAKISTANI_BANKS);
  const [linkedAccounts, setLinkedAccounts] = useState([]);
  const [bankDropdownOpen, setBankDropdownOpen] = useState(false);

  const [linkForm, setLinkForm] = useState({
    bankCode: '',
    accountNumber: '',
    accountTitle: '',
  });
  const [otpRequestId, setOtpRequestId] = useState(null);
  const [demoOtp, setDemoOtp] = useState('');
  const [otp, setOtp] = useState('');

  const [selectedAccount, setSelectedAccount] = useState(null);
  const [depositAmount, setDepositAmount] = useState('');

  const loadLinked = useCallback(async () => {
    try {
      const res = await addMoneyService.getLinkedAccounts();
      setLinkedAccounts(Array.isArray(res.data) ? res.data : []);
    } catch {
      setLinkedAccounts([]);
    }
  }, []);

  useEffect(() => {
    if (!open) return;
    setView(VIEWS.ACCOUNTS);
    setLinkForm({ bankCode: '', accountNumber: '', accountTitle: '' });
    setOtp('');
    setOtpRequestId(null);
    setDemoOtp('');
    setSelectedAccount(null);
    setDepositAmount('');
    loadLinked();
    addMoneyService.getBanks().then((res) => {
      if (Array.isArray(res.data) && res.data.length > 0) setBanks(res.data);
    }).catch(() => {});
  }, [open, loadLinked]);

  const selectedBank = getBankByCode(linkForm.bankCode) || banks.find((b) => b.code === linkForm.bankCode);

  const handleSendOtp = async (e) => {
    e.preventDefault();
    if (!linkForm.bankCode) return showToast('Bank select karein.', 'warning');
    if (!/^\d{10,20}$/.test(linkForm.accountNumber.replace(/\D/g, ''))) {
      return showToast('Account number 10-20 digits hona chahiye.', 'warning');
    }
    if (linkForm.accountTitle.trim().length < 3) {
      return showToast('Account title likhein.', 'warning');
    }
    setLoading(true);
    try {
      const res = await addMoneyService.sendLinkOtp({
        bankCode: linkForm.bankCode,
        accountNumber: linkForm.accountNumber.replace(/\D/g, ''),
        accountTitle: linkForm.accountTitle.trim(),
      });
      setOtpRequestId(res.data.otpRequestId);
      if (res.data.demoOtp) setDemoOtp(res.data.demoOtp);
      showToast(res.data.message || 'OTP bhej diya gaya.', 'success');
      setView(VIEWS.OTP);
    } catch (err) {
      showToast(getApiErrorMessage(err, 'OTP send fail.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    if (!/^\d{6}$/.test(otp)) return showToast('6-digit OTP enter karein.', 'warning');
    setLoading(true);
    try {
      const res = await addMoneyService.verifyLinkOtp({ otpRequestId, otp });
      showToast(res.data.message || 'Account link ho gaya!', 'success');
      await loadLinked();
      setView(VIEWS.ACCOUNTS);
      setOtp('');
    } catch (err) {
      showToast(getApiErrorMessage(err, 'OTP verify fail.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleDeposit = async (e) => {
    e.preventDefault();
    const amountErr = validateAmount(depositAmount);
    if (amountErr) return showToast(amountErr, 'warning');
    if (!selectedAccount?.id) return showToast('Bank account select karein.', 'warning');
    setLoading(true);
    try {
      const res = await addMoneyService.deposit(selectedAccount.id, depositAmount);
      showToast(res.data.message || 'Add money successful!', 'success');
      onSuccess?.(res.data.newBalance);
      onClose();
    } catch (err) {
      showToast(getApiErrorMessage(err, 'Deposit fail.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/90 backdrop-blur-md z-[80]"
        onClick={onClose}
      />
      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0a0a0a] border-t border-white/10 rounded-t-[3.5rem] p-8 pb-10 z-[81] shadow-2xl max-h-[92vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-6">
          <div>
            <p className="text-[10px] font-black uppercase tracking-widest text-emerald-400">Add Money</p>
            <h3 className="text-xl font-black italic">
              {view === VIEWS.LINK && 'Link bank account'}
              {view === VIEWS.OTP && 'Verify OTP'}
              {view === VIEWS.DEPOSIT && 'Enter amount'}
              {view === VIEWS.ACCOUNTS && 'Linked banks'}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="h-10 w-10 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-slate-400"
          >
            <X size={18} />
          </button>
        </div>

        {view === VIEWS.ACCOUNTS && (
          <div className="space-y-4">
            {linkedAccounts.length === 0 ? (
              <p className="text-sm text-slate-500 text-center py-6">
                Pehle apna bank account link karein, phir wallet mein paise add karein.
              </p>
            ) : (
              <ul className="space-y-3">
                {linkedAccounts.map((acc) => (
                  <li key={acc.id}>
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedAccount(acc);
                        setDepositAmount('');
                        setView(VIEWS.DEPOSIT);
                      }}
                      className="w-full flex items-center gap-4 p-4 rounded-2xl bg-white/[0.03] border border-white/10 hover:border-emerald-500/40 transition-all text-left"
                    >
                      <BankLogo bankCode={acc.bankCode} logoKey={acc.logoKey} size={44} />
                      <div className="flex-1 min-w-0">
                        <p className="font-black text-sm truncate">{acc.bankName}</p>
                        <p className="text-xs text-slate-500 truncate">{acc.accountTitle}</p>
                        <p className="text-[10px] font-mono text-slate-600 mt-1">{acc.accountNumberMasked}</p>
                      </div>
                      <Wallet size={18} className="text-emerald-400 shrink-0" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <button
              type="button"
              onClick={() => setView(VIEWS.LINK)}
              className="w-full flex items-center justify-center gap-2 py-4 rounded-2xl border border-dashed border-white/20 text-sm font-black uppercase tracking-widest text-slate-400 hover:text-white hover:border-emerald-500/50"
            >
              <Link2 size={18} />
              Link new bank
            </button>
          </div>
        )}

        {view === VIEWS.LINK && (
          <form onSubmit={handleSendOtp} className="space-y-5">
            <div className="space-y-2 relative">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">
                Pakistani bank
              </label>
              <button
                type="button"
                onClick={() => setBankDropdownOpen((o) => !o)}
                className="w-full flex items-center gap-3 p-4 rounded-2xl bg-white/[0.03] border border-white/10 text-left"
              >
                {selectedBank ? (
                  <>
                    <BankLogo bankCode={selectedBank.code} logoKey={selectedBank.logoKey} color={selectedBank.color} size={40} />
                    <span className="font-bold text-sm flex-1">{selectedBank.name}</span>
                  </>
                ) : (
                  <>
                    <Building2 className="text-slate-500" size={22} />
                    <span className="text-slate-500 text-sm flex-1">Bank select karein</span>
                  </>
                )}
                <ChevronDown size={18} className={`text-slate-500 transition-transform ${bankDropdownOpen ? 'rotate-180' : ''}`} />
              </button>
              {bankDropdownOpen && (
                <ul className="absolute z-20 left-0 right-0 mt-1 max-h-52 overflow-y-auto rounded-2xl bg-[#111] border border-white/10 shadow-2xl">
                  {banks.map((b) => (
                    <li key={b.code}>
                      <button
                        type="button"
                        onClick={() => {
                          setLinkForm((f) => ({ ...f, bankCode: b.code }));
                          setBankDropdownOpen(false);
                        }}
                        className="w-full flex items-center gap-3 px-4 py-3 hover:bg-white/5 text-left"
                      >
                        <BankLogo bankCode={b.code} logoKey={b.logoKey} color={b.color} size={36} />
                        <span className="text-sm font-bold truncate">{b.name}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <div className="space-y-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">
                Account number
              </label>
              <input
                type="text"
                inputMode="numeric"
                className="w-full bg-white/5 border border-white/10 rounded-2xl py-4 px-5 text-white font-mono"
                placeholder="IBAN / account number"
                value={linkForm.accountNumber}
                onChange={(e) =>
                  setLinkForm({ ...linkForm, accountNumber: e.target.value.replace(/\D/g, '').slice(0, 20) })
                }
              />
            </div>
            <div className="space-y-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">
                Account title
              </label>
              <input
                type="text"
                className="w-full bg-white/5 border border-white/10 rounded-2xl py-4 px-5 text-white"
                placeholder="Name on account"
                value={linkForm.accountTitle}
                onChange={(e) => setLinkForm({ ...linkForm, accountTitle: e.target.value })}
              />
            </div>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setView(VIEWS.ACCOUNTS)}
                className="flex-1 py-4 rounded-2xl border border-white/10 text-sm font-black uppercase text-slate-400"
              >
                Back
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-[2] py-4 rounded-2xl bg-emerald-600 font-black uppercase text-sm disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {loading ? <Loader2 className="animate-spin" size={18} /> : 'Send OTP'}
              </button>
            </div>
          </form>
        )}

        {view === VIEWS.OTP && (
          <form onSubmit={handleVerifyOtp} className="space-y-5">
            {demoOtp && (
              <p className="text-center text-xs text-amber-400/90 bg-amber-500/10 border border-amber-500/20 rounded-2xl py-3 px-4">
                Demo OTP (testing): <span className="font-mono font-black">{demoOtp}</span>
              </p>
            )}
            <p className="text-sm text-slate-500 text-center">
              6-digit OTP enter karein. Verify hone ke baad account link ho jayega.
            </p>
            <input
              type="text"
              inputMode="numeric"
              maxLength={6}
              className="w-full bg-white/5 border border-white/10 rounded-2xl py-5 px-6 text-white text-center text-2xl font-black tracking-[0.5em]"
              placeholder="000000"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
            />
            <button
              type="submit"
              disabled={loading || otp.length !== 6}
              className="w-full py-4 rounded-2xl bg-emerald-600 font-black uppercase text-sm disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? <Loader2 className="animate-spin" size={18} /> : 'Verify & link account'}
            </button>
          </form>
        )}

        {view === VIEWS.DEPOSIT && selectedAccount && (
          <form onSubmit={handleDeposit} className="space-y-5">
            <div className="flex items-center gap-4 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20">
              <BankLogo bankCode={selectedAccount.bankCode} logoKey={selectedAccount.logoKey} size={44} />
              <div>
                <p className="font-black text-sm">{selectedAccount.bankName}</p>
                <p className="text-xs text-slate-400">{selectedAccount.accountNumberMasked}</p>
              </div>
            </div>
            <p className="text-xs text-slate-500 text-center">
              Dummy transfer — entered amount wallet balance mein add ho jayegi.
            </p>
            <input
              type="number"
              min="1"
              step="1"
              className="w-full bg-white/5 border border-white/10 rounded-2xl py-5 px-6 text-white text-2xl font-black text-center"
              placeholder="Amount (PKR)"
              value={depositAmount}
              onChange={(e) => setDepositAmount(e.target.value)}
            />
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setView(VIEWS.ACCOUNTS)}
                className="flex-1 py-4 rounded-2xl border border-white/10 text-sm font-black uppercase text-slate-400"
              >
                Back
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-[2] py-4 rounded-2xl bg-emerald-600 font-black uppercase text-sm disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {loading ? <Loader2 className="animate-spin" size={18} /> : 'Add to wallet'}
              </button>
            </div>
          </form>
        )}
      </motion.div>
    </AnimatePresence>
  );
}
