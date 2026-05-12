import React, { createContext, useCallback, useContext, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react';

const ToastContext = createContext(null);

const variantStyles = {
  success:
    'border-emerald-500/30 bg-emerald-950/80 shadow-emerald-500/10 text-emerald-50',
  error: 'border-red-500/30 bg-red-950/80 shadow-red-500/10 text-red-50',
  warning: 'border-amber-500/30 bg-amber-950/80 shadow-amber-500/10 text-amber-50',
  info: 'border-indigo-500/35 bg-[#0a0a0a]/95 shadow-indigo-500/15 text-white',
};

const variantIcons = {
  success: CheckCircle2,
  error: AlertCircle,
  warning: AlertTriangle,
  info: Info,
};

const variantIconClass = {
  success: 'text-emerald-400',
  error: 'text-red-400',
  warning: 'text-amber-400',
  info: 'text-indigo-400',
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message, variant = 'info', duration = 4200) => {
      const id =
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      setToasts((prev) => [...prev, { id, message, variant }]);
      if (duration > 0) {
        window.setTimeout(() => dismiss(id), duration);
      }
      return id;
    },
    [dismiss]
  );

  return (
    <ToastContext.Provider value={{ showToast, dismiss }}>
      {children}
      <div
        className="fixed inset-x-0 bottom-0 z-[200] flex flex-col-reverse items-stretch gap-3 p-6 pt-0 pointer-events-none max-w-lg mx-auto w-full"
        aria-live="polite"
      >
        <AnimatePresence mode="popLayout">
          {toasts.map((t) => {
            const Icon = variantIcons[t.variant] || Info;
            return (
              <motion.div
                key={t.id}
                layout
                initial={{ opacity: 0, y: 28, scale: 0.94 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 16, scale: 0.96 }}
                transition={{ type: 'spring', damping: 28, stiffness: 380 }}
                className={`pointer-events-auto flex gap-3 items-start rounded-[1.75rem] border px-4 py-4 shadow-2xl backdrop-blur-xl ${variantStyles[t.variant] || variantStyles.info}`}
              >
                <Icon className={`shrink-0 mt-0.5 ${variantIconClass[t.variant] || variantIconClass.info}`} size={22} />
                <p className="text-sm font-bold leading-snug flex-1 min-w-0 pt-0.5">{t.message}</p>
                <button
                  type="button"
                  onClick={() => dismiss(t.id)}
                  className="shrink-0 -mr-1 -mt-1 h-9 w-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white/60 hover:text-white hover:bg-white/10 transition-colors"
                  aria-label="Dismiss"
                >
                  <X size={16} />
                </button>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return ctx;
}
