import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import FacebookLogin from '@greatsumini/react-facebook-login';
import { login, register } from '../services/authService';

/* ─── Google icon SVG ──────────────────────────────────────────────────── */
const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" style={{ flexShrink: 0 }}>
    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
  </svg>
);

/* ─── Facebook icon ────────────────────────────────────────────────────── */
const FbIcon = () => (
  <span style={{
    width: 18, height: 18, borderRadius: '50%', background: '#fff',
    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
    color: '#1877F2', fontSize: 12, fontWeight: 900, flexShrink: 0,
  }}>f</span>
);

/* ══════════════════════════════════════════════════════════════════════════
   LoginPage
═══════════════════════════════════════════════════════════════════════════ */
const LoginPage = () => {
  const navigate = useNavigate();

  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [email,    setEmail]    = useState('');
  const [phone,    setPhone]    = useState('');
  const [password, setPassword] = useState('');
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState('');
  const [status,   setStatus]   = useState('');
  const [focusedField, setFocused] = useState('');

  /* ── submit ── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setStatus('');
    try {
      const response = isRegisterMode
        ? await register(email, phone, password)
        : await login(email, password);

      if (response.status === 'SUCCESS') {
        navigate('/dashboard');
      } else if (response.status === 'LOCKED') {
        setStatus('LOCKED');
        setError(response.message || 'Account locked. Try again in 30 minutes.');
      } else if (response.status === 'CHALLENGED') {
        setStatus('CHALLENGED');
        setError('Suspicious activity detected. Please verify your identity.');
      } else {
        setError(response.message || 'Something went wrong.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Server error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /* ── Google OAuth ── */
  const handleGoogleSuccess = async (credentialResponse) => {
    setError('');
    try {
      const res = await fetch('http://localhost:8080/api/auth/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: credentialResponse.credential }),
      });
      const data = await res.json();
      if (data.token) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('userEmail', data.email || '');
        localStorage.setItem('loginData', JSON.stringify(data));
        navigate('/dashboard');
      } else {
        setError(data.message || 'Google login failed.');
      }
    } catch {
      setError('Google login failed. Please try again.');
    }
  };

  /* ── Facebook OAuth ── */
  const handleFacebookSuccess = async (fbResponse) => {
    setError('');
    try {
      const res = await fetch('http://localhost:8080/api/auth/facebook', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accessToken: fbResponse.accessToken }),
      });
      const data = await res.json();
      if (data.token) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('userEmail', data.email || '');
        localStorage.setItem('loginData', JSON.stringify(data));
        navigate('/dashboard');
      } else {
        setError(data.message || 'Facebook login failed.');
      }
    } catch {
      setError('Facebook login failed. Please try again.');
    }
  };

  /* ── helpers ── */
  const inputStyle = (field) => ({
    ...S.input,
    borderColor: focusedField === field ? '#2E75B6' : '#d1d5db',
    background:  focusedField === field ? '#fff'    : '#f9fafb',
    outline: 'none',
  });

  const toggleMode = () => {
    setIsRegisterMode(!isRegisterMode);
    setError('');
    setStatus('');
  };

  /* ── render ── */
  return (
    <div style={S.page}>
      <div style={S.card}>

        {/* Logo */}
        <div style={S.logoArea}>
          <div style={S.logoBadge}>
            <span style={{ fontSize: 20 }}>⚔</span>
            PROJECT ARES
          </div>
          <p style={S.logoSub}>AI-Driven Resilient Authentication System</p>
        </div>

        {/* Heading */}
        <h2 style={S.heading}>
          {isRegisterMode ? 'Create account' : 'Welcome back'}
        </h2>
        <p style={S.tagline}>
          {isRegisterMode ? 'Join Project Ares' : 'Sign in to your secure account'}
        </p>

        {/* Status banners */}
        {status === 'LOCKED' && (
          <div style={S.bannerLocked}>
            🔒 Account Locked — Too many failed attempts. Auto-unlock in 30 min.
          </div>
        )}
        {status === 'CHALLENGED' && (
          <div style={S.bannerChallenged}>
            ⚠️ Suspicious Activity — Your account has been challenged
          </div>
        )}

        {/* Error */}
        {error && !status && (
          <div style={S.errorBox}>{error}</div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} style={S.form}>

          <div style={S.fieldGroup}>
            <label style={S.label}>Email address</label>
            <input
              id="email-input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onFocus={() => setFocused('email')}
              onBlur={() => setFocused('')}
              placeholder="you@example.com"
              style={inputStyle('email')}
              required
            />
          </div>

          {isRegisterMode && (
            <div style={S.fieldGroup}>
              <label style={S.label}>Phone <span style={{ color: '#9ca3af' }}>(optional)</span></label>
              <input
                id="phone-input"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                onFocus={() => setFocused('phone')}
                onBlur={() => setFocused('')}
                placeholder="+90 555 123 4567"
                style={inputStyle('phone')}
              />
            </div>
          )}

          <div style={S.fieldGroup}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 5 }}>
              <label style={S.label}>Password</label>
              {!isRegisterMode && (
                <span style={S.forgotLink}>Forgot password?</span>
              )}
            </div>
            <input
              id="password-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onFocus={() => setFocused('password')}
              onBlur={() => setFocused('')}
              placeholder="••••••••"
              style={inputStyle('password')}
              required
            />
          </div>

          <button
            id="login-button"
            type="submit"
            disabled={loading}
            style={loading ? S.btnDisabled : S.btnPrimary}
          >
            {loading
              ? '⏳ Please wait...'
              : isRegisterMode
              ? 'Create Account'
              : 'Sign in to Ares'}
          </button>
        </form>

        {/* Divider */}
        <div style={S.divider}>
          <hr style={S.dividerLine} />
          <span style={S.dividerTxt}>or continue with</span>
          <hr style={S.dividerLine} />
        </div>

        {/* Social */}
        <div style={S.socialRow}>
          {/* Google — rendered by @react-oauth/google, wrapped for id targeting */}
          <div id="google-login-btn" style={S.socialWrapper}>
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => setError('Google login failed')}
              useOneTap={false}
              width="200"
              text="signin_with"
              shape="rectangular"
              theme="outline"
            />
          </div>

          {/* Facebook */}
          <FacebookLogin
            appId="1014667905062807"
            onSuccess={handleFacebookSuccess}
            onFail={() => setError('Facebook login failed')}
            render={({ onClick }) => (
              <button
                id="facebook-login-btn"
                onClick={onClick}
                style={S.btnFacebook}
              >
                <FbIcon />
                Sign in with Facebook
              </button>
            )}
          />
        </div>

        {/* Toggle */}
        <p style={S.toggleTxt}>
          {isRegisterMode ? 'Already have an account? ' : "Don't have an account? "}
          <span style={S.toggleLink} onClick={toggleMode}>
            {isRegisterMode ? 'Sign In' : 'Create one free'}
          </span>
        </p>

        {/* Security badge */}
        <div style={S.securityBadge}>
          <span>🛡</span>
          <span style={{ fontSize: 11, color: '#0369a1' }}>
            Protected by Context-Aware Risk Scoring + Claude AI Fraud Detection
          </span>
        </div>

      </div>
    </div>
  );
};

/* ══════════════════════════════════════════════════════════════════════════
   Styles
═══════════════════════════════════════════════════════════════════════════ */
const S = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, #0f1923 0%, #1a2a3a 50%, #0f1923 100%)',
    fontFamily: "'Outfit', -apple-system, BlinkMacSystemFont, sans-serif",
    padding: '24px 16px',
  },
  card: {
    background: '#ffffff',
    borderRadius: 16,
    padding: '40px 44px 36px',
    width: '100%',
    maxWidth: 440,
    boxShadow: '0 24px 80px rgba(0,0,0,0.45)',
  },
  logoArea: { textAlign: 'center', marginBottom: 28 },
  logoBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 10,
    background: '#1F4E79',
    color: '#fff',
    padding: '10px 20px',
    borderRadius: 30,
    fontSize: 17,
    fontWeight: 700,
    letterSpacing: 1,
  },
  logoSub: { color: '#6b7280', fontSize: 13, marginTop: 6, marginBottom: 0 },
  heading: { fontSize: 22, fontWeight: 700, color: '#111827', marginBottom: 4, marginTop: 0 },
  tagline: { color: '#6b7280', fontSize: 13, marginBottom: 22, marginTop: 0 },

  bannerLocked: {
    background: '#fef2f2',
    border: '1.5px solid #fca5a5',
    color: '#b91c1c',
    padding: '10px 14px',
    borderRadius: 8,
    fontSize: 13,
    fontWeight: 600,
    marginBottom: 14,
    textAlign: 'center',
  },
  bannerChallenged: {
    background: '#fff7ed',
    border: '1.5px solid #fed7aa',
    color: '#c2410c',
    padding: '10px 14px',
    borderRadius: 8,
    fontSize: 13,
    fontWeight: 600,
    marginBottom: 14,
    textAlign: 'center',
  },
  errorBox: {
    background: '#fef2f2',
    border: '1px solid #fca5a5',
    color: '#b91c1c',
    padding: '10px 14px',
    borderRadius: 8,
    fontSize: 13,
    marginBottom: 14,
  },

  form: { display: 'flex', flexDirection: 'column', gap: 0 },
  fieldGroup: { marginBottom: 16 },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginBottom: 5 },
  forgotLink: { fontSize: 12, color: '#2E75B6', cursor: 'pointer' },
  input: {
    width: '100%',
    padding: '11px 14px',
    border: '1.5px solid #d1d5db',
    borderRadius: 8,
    fontSize: 14,
    color: '#111827',
    background: '#f9fafb',
    boxSizing: 'border-box',
    transition: 'border-color .15s, background .15s',
    fontFamily: 'inherit',
  },

  btnPrimary: {
    width: '100%',
    padding: 12,
    background: '#1F4E79',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 700,
    cursor: 'pointer',
    marginTop: 4,
    fontFamily: 'inherit',
    letterSpacing: .3,
    transition: 'background .2s',
  },
  btnDisabled: {
    width: '100%',
    padding: 12,
    background: '#9ca3af',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 700,
    cursor: 'not-allowed',
    marginTop: 4,
    fontFamily: 'inherit',
  },

  divider: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    margin: '20px 0 14px',
  },
  dividerLine: { flex: 1, border: 'none', borderTop: '1px solid #e5e7eb', margin: 0 },
  dividerTxt: { fontSize: 12, color: '#9ca3af', whiteSpace: 'nowrap' },

  socialRow: {
    display: 'flex',
    flexDirection: 'column',
    gap: 10,
    alignItems: 'stretch',
    marginBottom: 20,
  },
  socialWrapper: {
    display: 'flex',
    justifyContent: 'center',
  },
  btnFacebook: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    padding: '10px 16px',
    background: '#1877F2',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 700,
    cursor: 'pointer',
    fontFamily: 'inherit',
    width: '100%',
  },

  toggleTxt: { textAlign: 'center', fontSize: 13, color: '#6b7280', marginBottom: 14, marginTop: 0 },
  toggleLink: { color: '#2E75B6', cursor: 'pointer', fontWeight: 700 },

  securityBadge: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: '8px 12px',
    background: '#f0f9ff',
    borderRadius: 8,
    border: '1px solid #bae6fd',
  },
};

export default LoginPage;