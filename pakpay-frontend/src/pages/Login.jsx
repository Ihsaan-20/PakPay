import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { authService } from '../services/api';
import { Lock, Phone, ArrowRight, Fingerprint } from 'lucide-react';
import { useToast } from '../components/ToastProvider';

const Login = () => {
  const { showToast } = useToast();
  const [formData, setFormData] = useState({ mobileNumber: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await authService.login(formData.mobileNumber, formData.password);
      // Backend response mapping
      localStorage.setItem('token', data.token);
      if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
      if (data.email != null) localStorage.setItem('email', data.email);
      if (data.mobileNumber != null) localStorage.setItem('mobileNumber', data.mobileNumber);
      localStorage.setItem('fullName', data.fullName);
      localStorage.setItem('balance', data.currentBalance);
      localStorage.setItem('walletID', data.walletAccountNumber);
      const pinOk = String(data.isPinSet ?? '').toLowerCase() === 'yes';
      localStorage.setItem('isPinSet', pinOk ? 'yes' : 'no');
      
      window.location.href = '/dashboard';
    } catch (err) {
      showToast('Invalid credentials. Dobara try karein.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050505] flex items-center justify-center p-6">
      <div className="fixed top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-indigo-600/10 blur-[120px] rounded-full"></div>
      </div>

      <motion.div 
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-[420px] bg-white/[0.02] border border-white/10 backdrop-blur-3xl p-10 rounded-[3rem] shadow-2xl relative z-10"
      >
        <div className="text-center mb-10">
          <div className="inline-flex h-16 w-16 bg-indigo-600 rounded-3xl items-center justify-center mb-6 shadow-lg shadow-indigo-600/20">
            <Fingerprint size={32} className="text-white" />
          </div>
          <h1 className="text-4xl font-black tracking-tighter text-white italic">PakPay</h1>
          <p className="text-slate-500 text-xs font-bold uppercase tracking-widest mt-2">Secure Gateway</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">Mobile Number</label>
            <div className="relative group">
              <Phone className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input 
                type="text" required
                className="w-full bg-white/[0.03] border border-white/10 rounded-2xl py-4 pl-14 pr-6 text-white outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] transition-all"
                placeholder="0315XXXXXXX"
                onChange={(e) => setFormData({...formData, mobileNumber: e.target.value})}
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">Security Pin</label>
            <div className="relative group">
              <Lock className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input 
                type="password" required
                className="w-full bg-white/[0.03] border border-white/10 rounded-2xl py-4 pl-14 pr-6 text-white outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] transition-all"
                placeholder="••••••••"
                onChange={(e) => setFormData({...formData, password: e.target.value})}
              />
            </div>
          </div>

          <button 
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-indigo-600/10 flex items-center justify-center gap-3 transition-all active:scale-95 disabled:opacity-50"
          >
            {loading ? "Verifying..." : "Access Wallet"}
            <ArrowRight size={20} />
          </button>
        </form>
      </motion.div>
    </div>
  );
};

export default Login;