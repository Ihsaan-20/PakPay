import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { authService } from '../services/api';
import { User, Phone, Lock, CreditCard, ArrowRight, Fingerprint } from 'lucide-react';
import { useToast } from '../components/ToastProvider';
import {
  formatCnicInput,
  getApiErrorField,
  getApiErrorMessage,
  normalizeMobile,
  validateCnic,
  validateFullName,
  validateMobile,
  validatePassword,
} from '../utils/validation';

const Signup = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [formData, setFormData] = useState({
    fullName: '',
    mobileNumber: '',
    password: '',
    confirmPassword: '',
    cnic: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const validateForm = () => {
    const errors = {};
    const nameErr = validateFullName(formData.fullName);
    const mobileErr = validateMobile(formData.mobileNumber);
    const cnicErr = validateCnic(formData.cnic);
    const passwordErr = validatePassword(formData.password);

    if (nameErr) errors.fullName = nameErr;
    if (mobileErr) errors.mobileNumber = mobileErr;
    if (cnicErr) errors.cnic = cnicErr;
    if (passwordErr) errors.password = passwordErr;
    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Password match nahi kar raha.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setLoading(true);
    setFieldErrors({});
    try {
      await authService.signup({
        fullName: formData.fullName.trim(),
        mobileNumber: normalizeMobile(formData.mobileNumber),
        password: formData.password,
        cnic: formData.cnic,
      });
      showToast('Account ban gaya! Ab login karein.', 'success');
      navigate('/login');
    } catch (err) {
      const apiField = getApiErrorField(err);
      const message = getApiErrorMessage(err, 'Signup fail ho gaya.');
      if (apiField) {
        setFieldErrors((prev) => ({ ...prev, [apiField]: message }));
      }
      showToast(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const inputClass = (field) =>
    `w-full bg-white/[0.03] border rounded-2xl py-4 pl-14 pr-6 text-white outline-none focus:bg-white/[0.06] transition-all ${
      fieldErrors[field]
        ? 'border-red-500/50 focus:border-red-500/70'
        : 'border-white/10 focus:border-indigo-500/50'
    }`;

  return (
    <motion.div className="min-h-screen bg-[#050505] flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-[420px] bg-white/[0.02] border border-white/10 backdrop-blur-3xl p-10 rounded-[3rem] shadow-2xl relative z-10"
      >
        <div className="text-center mb-10">
          <div className="inline-flex h-16 w-16 bg-indigo-600 rounded-3xl items-center justify-center mb-6 shadow-lg shadow-indigo-600/20">
            <Fingerprint size={32} className="text-white" />
          </div>
          <h1 className="text-4xl font-black tracking-tighter text-white italic">PakPay</h1>
          <p className="text-slate-500 text-xs font-bold uppercase tracking-widest mt-2">Create Account</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">Full Name</label>
            <div className="relative group">
              <User className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input
                type="text"
                className={inputClass('fullName')}
                placeholder="Ihsan Gohar"
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
              />
            </div>
            {fieldErrors.fullName && <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.fullName}</p>}
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">Mobile Number</label>
            <div className="relative group">
              <Phone className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input
                type="tel"
                inputMode="numeric"
                className={inputClass('mobileNumber')}
                placeholder="03151234567"
                value={formData.mobileNumber}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    mobileNumber: e.target.value.replace(/\D/g, '').slice(0, 11),
                  })
                }
              />
            </div>
            {fieldErrors.mobileNumber && (
              <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.mobileNumber}</p>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">CNIC</label>
            <div className="relative group">
              <CreditCard className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input
                type="text"
                inputMode="numeric"
                className={inputClass('cnic')}
                placeholder="12345-1234567-1"
                value={formData.cnic}
                onChange={(e) => setFormData({ ...formData, cnic: formatCnicInput(e.target.value) })}
              />
            </div>
            {fieldErrors.cnic && <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.cnic}</p>}
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">Password</label>
            <div className="relative group">
              <Lock className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input
                type="password"
                className={inputClass('password')}
                placeholder="••••••••"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>
            {fieldErrors.password && <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.password}</p>}
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">Confirm Password</label>
            <div className="relative group">
              <Lock className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors" size={18} />
              <input
                type="password"
                className={inputClass('confirmPassword')}
                placeholder="••••••••"
                value={formData.confirmPassword}
                onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              />
            </div>
            {fieldErrors.confirmPassword && (
              <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.confirmPassword}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-indigo-600/10 flex items-center justify-center gap-3 transition-all active:scale-95 disabled:opacity-50"
          >
            {loading ? 'Creating account...' : 'Sign Up'}
            <ArrowRight size={20} />
          </button>
        </form>

        <p className="text-center text-slate-500 text-sm mt-8">
          Pehle se account hai?{' '}
          <Link to="/login" className="text-indigo-400 font-bold hover:text-indigo-300">
            Login
          </Link>
        </p>
      </motion.div>
    </motion.div>
  );
};

export default Signup;
