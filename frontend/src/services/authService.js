import axios from 'axios';

const API_URL = 'http://localhost:8080/api/auth';

/* ─── token helpers ──────────────────────────────────────────────────────── */
export const saveToken  = (token) => localStorage.setItem('token', token);
export const getToken   = ()      => localStorage.getItem('token');
export const logout     = ()      => {
  localStorage.removeItem('token');
  localStorage.removeItem('loginData');
  localStorage.removeItem('userEmail');
};

/* ─── register ───────────────────────────────────────────────────────────── */
export const register = async (email, phone, password) => {
  const response = await axios.post(`${API_URL}/register`, {
    email,
    phone,
    password,
  });
  if (response.data.token) {
    saveToken(response.data.token);
    // Persist full response so Dashboard can read risk data immediately
    localStorage.setItem('loginData', JSON.stringify(response.data));
    localStorage.setItem('userEmail', email);
  }
  return response.data;
};

/* ─── login ──────────────────────────────────────────────────────────────── */
export const login = async (email, password) => {
  // Fetch real client IP for risk scoring — silently falls back to 'unknown'
  const ipResponse = await axios
    .get('https://api.ipify.org?format=json')
    .catch(() => ({ data: { ip: 'unknown' } }));

  const response = await axios.post(`${API_URL}/login`, {
    email,
    password,
    ipAddress: ipResponse.data.ip,
  });

  if (response.data.token) {
    saveToken(response.data.token);
    // Persist full response so Dashboard can read risk score, signals, etc.
    localStorage.setItem('loginData', JSON.stringify({
      ...response.data,
      email,
      ipAddress: ipResponse.data.ip,
    }));
    localStorage.setItem('userEmail', email);
  }
  return response.data;
};