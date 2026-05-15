/** Pakistani banks — logoKey matches /banks/{logoKey}.svg in public */
export const PAKISTANI_BANKS = [
  { code: 'HBL', name: 'Habib Bank Limited', logoKey: 'hbl', color: '#008269' },
  { code: 'UBL', name: 'United Bank Limited', logoKey: 'ubl', color: '#007DC5' },
  { code: 'MCB', name: 'MCB Bank Limited', logoKey: 'mcb', color: '#C9A227' },
  { code: 'MEEZAN', name: 'Meezan Bank', logoKey: 'meezan', color: '#006747' },
  { code: 'ABL', name: 'Allied Bank Limited', logoKey: 'abl', color: '#E31837' },
  { code: 'ALFALAH', name: 'Bank Alfalah', logoKey: 'alfalah', color: '#E2231A' },
  { code: 'FAYSAL', name: 'Faysal Bank', logoKey: 'faysal', color: '#003B71' },
  { code: 'SCB', name: 'Standard Chartered', logoKey: 'scb', color: '#0473EA' },
  { code: 'BAH', name: 'Bank Al Habib', logoKey: 'bah', color: '#00A651' },
  { code: 'ASKARI', name: 'Askari Bank', logoKey: 'askari', color: '#006633' },
  { code: 'JS', name: 'JS Bank', logoKey: 'js', color: '#003DA5' },
  { code: 'SONERI', name: 'Soneri Bank', logoKey: 'soneri', color: '#F7941D' },
  { code: 'SILK', name: 'Silkbank', logoKey: 'silk', color: '#6B2C91' },
  { code: 'SUMMIT', name: 'Summit Bank', logoKey: 'summit', color: '#1E4D8C' },
  { code: 'NBP', name: 'National Bank of Pakistan', logoKey: 'nbp', color: '#006B3F' },
  { code: 'SINDH', name: 'Sindh Bank', logoKey: 'sindh', color: '#0054A6' },
  { code: 'BOP', name: 'Bank of Punjab', logoKey: 'bop', color: '#00529B' },
  { code: 'DIB', name: 'Dubai Islamic Bank Pakistan', logoKey: 'dib', color: '#006341' },
  { code: 'SAMBA', name: 'Samba Bank', logoKey: 'samba', color: '#004B87' },
  { code: 'BIPL', name: 'BankIslami Pakistan', logoKey: 'bipl', color: '#00A651' },
];

export function getBankByCode(code) {
  return PAKISTANI_BANKS.find((b) => b.code === code);
}
