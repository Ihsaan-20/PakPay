const MOBILE_REGEX = /^03\d{9}$/;
const CNIC_DIGITS_REGEX = /^\d{13}$/;

export function normalizeMobile(value) {
  let digits = String(value || '').replace(/\D/g, '');
  if (digits.startsWith('92') && digits.length === 12) {
    digits = `0${digits.slice(2)}`;
  }
  return digits;
}

export function normalizeCnic(value) {
  return String(value || '').replace(/\D/g, '');
}

export function formatCnicInput(value) {
  const digits = normalizeCnic(value).slice(0, 13);
  if (digits.length <= 5) return digits;
  if (digits.length <= 12) return `${digits.slice(0, 5)}-${digits.slice(5)}`;
  return `${digits.slice(0, 5)}-${digits.slice(5, 12)}-${digits.slice(12)}`;
}

export function validateFullName(name) {
  const trimmed = String(name || '').trim();
  if (trimmed.length < 2) return 'Full name kam az kam 2 characters ka ho.';
  if (trimmed.length > 100) return 'Full name 100 characters se zyada nahi ho sakta.';
  return null;
}

export function validateMobile(mobile) {
  const normalized = normalizeMobile(mobile);
  if (!normalized) return 'Mobile number zaroori hai.';
  if (!MOBILE_REGEX.test(normalized)) {
    return 'Mobile 03XXXXXXXXX format mein hona chahiye (11 digits).';
  }
  return null;
}

export function validateCnic(cnic) {
  const normalized = normalizeCnic(cnic);
  if (!normalized) return 'CNIC zaroori hai.';
  if (!CNIC_DIGITS_REGEX.test(normalized)) {
    return 'CNIC 13 digits ka hona chahiye (e.g. 12345-1234567-1).';
  }
  return null;
}

export function validatePassword(password, minLength = 6) {
  const value = String(password || '');
  if (!value) return 'Password zaroori hai.';
  if (value.length < minLength) return `Password kam az kam ${minLength} characters ka ho.`;
  if (value.length > 100) return 'Password 100 characters se zyada nahi ho sakta.';
  return null;
}

export function validateTransactionPin(pin) {
  const digits = String(pin || '').replace(/\D/g, '');
  if (digits.length !== 4) return 'Transaction PIN exactly 4 digits ka hona chahiye.';
  return null;
}

export function validateAmount(amount) {
  const num = Number(amount);
  if (!amount || Number.isNaN(num)) return 'Amount zaroori hai.';
  if (num <= 0) return 'Amount 0 se zyada honi chahiye.';
  if (num > 500000) return 'Amount Rs. 5,00,000 se zyada nahi ho sakti.';
  return null;
}

/** Credit (money in) vs debit for activity/history UI */
export function isCreditTransaction(type) {
  const t = String(type || '').toUpperCase();
  return t === 'RECEIVED' || t === 'ADD_MONEY' || t === 'BANK_DEPOSIT';
}

/** Extract user-facing message from API error response. */
export function getApiErrorMessage(err, fallback = 'Request failed. Dobara try karein.') {
  const data = err?.response?.data;
  if (!data) return fallback;
  if (typeof data === 'string') return data;
  if (data.message) return data.message;
  if (data.errors && typeof data.errors === 'object') {
    const first = Object.values(data.errors)[0];
    if (first) return first;
  }
  return fallback;
}

export function getApiErrorField(err) {
  return err?.response?.data?.field || null;
}
