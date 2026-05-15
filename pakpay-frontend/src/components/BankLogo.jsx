import React from 'react';
import { getBankByCode } from '../data/pakistaniBanks';

export default function BankLogo({ bankCode, logoKey, color, size = 40, className = '' }) {
  const bank = bankCode ? getBankByCode(bankCode) : null;
  const key = logoKey || bank?.logoKey || 'default';
  const bg = color || bank?.color || '#4f46e5';
  const label = (bank?.code || bankCode || key).slice(0, 3).toUpperCase();

  return (
    <div
      className={`shrink-0 rounded-xl flex items-center justify-center font-black text-white text-[10px] shadow-inner border border-white/10 ${className}`}
      style={{ width: size, height: size, backgroundColor: bg }}
      title={bank?.name || bankCode}
    >
      {label}
    </div>
  );
}
