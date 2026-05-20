import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { authService } from '../services/api';
import { Lock, Phone, KeyRound, ArrowRight, ArrowLeft, Fingerprint, CheckCircle } from 'lucide-react';
import { useToast } from '../components/ToastProvider';
import {
  getApiErrorMessage,
  normalizeMobile,
  validateMobile,
  validatePassword,
} from '../utils/validation';

const STEPS = { MOBILE: 1, OTP: 2, PASSWORD: 3, DONE: 4 };

const ForgotPassword = () => {
  const { showToast } = useToast();
  const [step, setStep] = useState(STEPS.MOBILE);
  const [mobileNumber, setMobileNumber] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const inputClass = (field) =>
    `w-full bg-white/[0.03] border rounded-2xl py-4 pl-14 pr-6 text-white outline-none focus:bg-white/[0.06] transition-all ${
      fieldErrors[field]
        ? 'border-red-500/50 focus:border-red-500/70'
        : 'border-white/10 focus:border-indigo-500/50'
    }`;

  const handleSendOtp = async (e) => {
    e.preventDefault();
    const err = validateMobile(mobileNumber);
    if (err) {
      setFieldErrors({ mobileNumber: err });
      return;
    }
    setFieldErrors({});
    setLoading(true);
    try {
      await authService.forgotPasswordSendOtp(normalizeMobile(mobileNumber));
      showToast('OTP aapke mobile number par bhej diya gaya hai.', 'success');
      setStep(STEPS.OTP);
    } catch (err) {
      showToast(getApiErrorMessage(err, 'OTP bhejne mein masla aaya.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    if (!otpCode || otpCode.length !== 6) {
      setFieldErrors({ otpCode: 'OTP 6 digits ka hona chahiye.' });
      return;
    }
    setFieldErrors({});
    setLoading(true);
    try {
      await authService.forgotPasswordVerifyOtp(normalizeMobile(mobileNumber), otpCode);
      showToast('OTP verify ho gaya!', 'success');
      setStep(STEPS.PASSWORD);
    } catch (err) {
      showToast(getApiErrorMessage(err, 'Galat OTP.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    const err = validatePassword(newPassword);
    if (err) {
      setFieldErrors({ newPassword: err });
      return;
    }
    setFieldErrors({});
    setLoading(true);
    try {
      await authService.forgotPasswordReset(normalizeMobile(mobileNumber), otpCode, newPassword);
      showToast('Password successfully change ho gaya!', 'success');
      setStep(STEPS.DONE);
    } catch (err) {
      showToast(getApiErrorMessage(err, 'Password change mein masla aaya.'), 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050505] flex items-center justify-center p-6">
      <div className="fixed top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-indigo-600/10 blur-[120px] rounded-full" />
      </div>

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
          <p className="text-slate-500 text-xs font-bold uppercase tracking-widest mt-2">
            {step === STEPS.MOBILE && 'Forgot Password'}
            {step === STEPS.OTP && 'Enter OTP'}
            {step === STEPS.PASSWORD && 'New Password'}
            {step === STEPS.DONE && 'Done!'}
          </p>
        </div>

        <AnimatePresence mode="wait">
          {step === STEPS.MOBILE && (
            <motion.form
              key="mobile"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              onSubmit={handleSendOtp}
              className="space-y-6"
            >
              <div className="space-y-2">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">
                  Mobile Number
                </label>
                <div className="relative group">
                  <Phone
                    className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors"
                    size={18}
                  />
                  <input
                    type="tel"
                    inputMode="numeric"
                    required
                    className={inputClass('mobileNumber')}
                    placeholder="03151234567"
                    value={mobileNumber}
                    onChange={(e) =>
                      setMobileNumber(e.target.value.replace(/\D/g, '').slice(0, 11))
                    }
                  />
                </div>
                {fieldErrors.mobileNumber && (
                  <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.mobileNumber}</p>
                )}
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-indigo-600/10 flex items-center justify-center gap-3 transition-all active:scale-95 disabled:opacity-50"
              >
                {loading ? 'Sending...' : 'Send OTP'}
                <ArrowRight size={20} />
              </button>

              <p className="text-center text-slate-500 text-sm mt-8">
                Yaad aaya?{' '}
                <Link to="/login" className="text-indigo-400 font-bold hover:text-indigo-300">
                  Login
                </Link>
              </p>
            </motion.form>
          )}

          {step === STEPS.OTP && (
            <motion.form
              key="otp"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              onSubmit={handleVerifyOtp}
              className="space-y-6"
            >
              <div className="space-y-2">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">
                  6-Digit OTP
                </label>
                <div className="relative group">
                  <KeyRound
                    className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors"
                    size={18}
                  />
                  <input
                    type="text"
                    inputMode="numeric"
                    required
                    maxLength={6}
                    className={inputClass('otpCode')}
                    placeholder="123456"
                    value={otpCode}
                    onChange={(e) =>
                      setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))
                    }
                  />
                </div>
                {fieldErrors.otpCode && (
                  <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.otpCode}</p>
                )}
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setStep(STEPS.MOBILE)}
                  className="w-1/3 bg-white/5 hover:bg-white/10 text-white font-black py-5 rounded-2xl flex items-center justify-center gap-2 transition-all active:scale-95"
                >
                  <ArrowLeft size={20} />
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-2/3 bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-indigo-600/10 flex items-center justify-center gap-3 transition-all active:scale-95 disabled:opacity-50"
                >
                  {loading ? 'Verifying...' : 'Verify OTP'}
                  <ArrowRight size={20} />
                </button>
              </div>
            </motion.form>
          )}

          {step === STEPS.PASSWORD && (
            <motion.form
              key="password"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              onSubmit={handleResetPassword}
              className="space-y-6"
            >
              <div className="space-y-2">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-4">
                  Naya Password
                </label>
                <div className="relative group">
                  <Lock
                    className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-indigo-500 transition-colors"
                    size={18}
                  />
                  <input
                    type="password"
                    required
                    className={inputClass('newPassword')}
                    placeholder="Minimum 6 characters"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />
                </div>
                {fieldErrors.newPassword && (
                  <p className="text-red-400 text-xs font-bold ml-4">{fieldErrors.newPassword}</p>
                )}
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setStep(STEPS.OTP)}
                  className="w-1/3 bg-white/5 hover:bg-white/10 text-white font-black py-5 rounded-2xl flex items-center justify-center gap-2 transition-all active:scale-95"
                >
                  <ArrowLeft size={20} />
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-2/3 bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-indigo-600/10 flex items-center justify-center gap-3 transition-all active:scale-95 disabled:opacity-50"
                >
                  {loading ? 'Resetting...' : 'Reset Password'}
                  <ArrowRight size={20} />
                </button>
              </div>
            </motion.form>
          )}

          {step === STEPS.DONE && (
            <motion.div
              key="done"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="space-y-6 text-center"
            >
              <div className="flex justify-center">
                <CheckCircle size={64} className="text-green-500" />
              </div>
              <p className="text-white text-lg font-bold">Password change ho gaya!</p>
              <p className="text-slate-400 text-sm">Ab aap naye password se login kar sakte hain.</p>
              <Link
                to="/login"
                className="block w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-indigo-600/10 transition-all active:scale-95"
              >
                Login Karein
              </Link>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
};

export default ForgotPassword;
