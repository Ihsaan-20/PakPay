import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

// Request interceptor for JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const reqUrl = error.config?.url || '';
    const isLoginAttempt = reqUrl.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginAttempt) {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('fullName');
      localStorage.removeItem('balance');
      localStorage.removeItem('walletID');
      localStorage.removeItem('isPinSet');
      localStorage.removeItem('email');
      localStorage.removeItem('mobileNumber');
      window.dispatchEvent(new CustomEvent('pakpay:session-expired'));
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: async (mobileNumber, password) => {
    const res = await api.post('/auth/login', { mobileNumber, password });
    return res.data; 
  }
};

export const walletService = {
  // Step 1: Check if receiver exists
  checkReceiver: (mobile) => api.get(`/wallets/check/${mobile}`),

  setTransactionPin: (pin) =>
    api.post(`/wallets/set-pin?pin=${encodeURIComponent(pin)}`, {}),

  // Step 2: Final transfer with Idempotency Key
  secureTransfer: async (toMobile, amount, pin) => {
    const idempotencyKey = `pay-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
    
    return api.post(
      `/wallets/secure-transfer?toMobile=${toMobile}&amount=${amount}&pin=${pin}`,
      {}, 
      {
        headers: { 'X-Idempotency-Key': idempotencyKey }
      }
    );
  }, // <--- Yahan comma missing tha

  // Real-time balance fetch karne ke liye
  getRealTimeBalance: () => api.get('/wallets/balance'),

  getHistory: () => api.get('/wallets/history'),

  getMyQr: (mobile, name) =>
    api.get(`/wallets/my-qr?mobile=${encodeURIComponent(mobile)}&name=${encodeURIComponent(name)}`),
};

export default api;