import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout, getToken } from '../services/authService';

/* ══════════════════════════════════════════════════════════════════════════
   Helpers
═══════════════════════════════════════════════════════════════════════════ */

/** Decode JWT payload without a library */
const decodeJwt = (token) => {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return {};
  }
};

/** Format ISO date string to readable form */
const fmt = (iso) => {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('tr-TR', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    }).replace(',', '');
  } catch {
    return iso;
  }
};

/** Risk level from score */
const riskLevel = (score) => {
  if (score <= 30) return { label: 'LOW', color: '#065f46', bg: '#d1fae5' };
  if (score <= 70) return { label: 'MEDIUM', color: '#92400e', bg: '#fef3c7' };
  return { label: 'HIGH', color: '#991b1b', bg: '#fee2e2' };
};

/** Initial letter for avatar */
const initial = (email) => (email ? email[0].toUpperCase() : '?');

/* ══════════════════════════════════════════════════════════════════════════
   DashboardPage
═══════════════════════════════════════════════════════════════════════════ */
const DashboardPage = () => {
  const navigate  = useNavigate();
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);

  /* Load user data — from backend if available, else decode JWT */
  useEffect(() => {
    const token = getToken();
    if (!token) { navigate('/'); return; }

    const stored = localStorage.getItem('loginData');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setData(buildDisplayData(parsed, token));
        setLoading(false);
        return;
      } catch { /* fall through to JWT decode */ }
    }

    /* Decode JWT as fallback */
    const claims = decodeJwt(token);
    setData(buildDisplayData({ email: claims.sub }, token));
    setLoading(false);
  }, [navigate]);

  const handleLogout = () => {
    logout();
    localStorage.removeItem('loginData');
    localStorage.removeItem('userEmail');
    navigate('/');
  };

  if (loading) return <div style={S.loadingScreen}>Loading…</div>;
  if (!data)   return null;

  const rl = riskLevel(data.riskScore);

  return (
    <div style={S.page}>

      {/* ── Topbar ── */}
      <div style={S.topbar}>
        <div style={S.topbarBrand}>
          <span style={{ fontSize: 20 }}>⚔</span>
          PROJECT ARES
        </div>
        <div style={S.topbarRight}>
          <span style={S.topbarBadge}>🛡 Security Active</span>
          <div style={S.avatar}>{initial(data.email)}</div>
          <span style={S.topbarEmail}>{data.email}</span>
          <button onClick={handleLogout} style={S.logoutBtn}>Sign Out</button>
        </div>
      </div>

      {/* ── Content ── */}
      <div style={S.content}>

        {/* Welcome banner */}
        <div style={S.welcomeBanner}>
          <div>
            <h2 style={S.welcomeTitle}>🎉 Login Successful</h2>
            <p style={S.welcomeSub}>
              Welcome back! Risk Score: <strong>{data.riskScore}/100</strong> — {rl.label} risk detected.
            </p>
            <div style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ ...S.statusBadge, background: 'rgba(255,255,255,.25)', color: '#fff', border: '1px solid rgba(255,255,255,.4)' }}>
                ✓ {data.accountStatus}
              </span>
              <span style={{ fontSize: 12, opacity: .8 }}>
                Provider: {data.provider} &nbsp;|&nbsp; Auth: JWT
              </span>
            </div>
          </div>
          <div style={S.jwtBox}>
            <div style={{ fontSize: 11, opacity: .7, marginBottom: 4, textAlign: 'right' }}>Access Token (JWT)</div>
            <div style={S.jwtBadge}>{data.tokenPreview}</div>
          </div>
        </div>

        {/* Stat cards row */}
        <div style={S.cardsRow}>
          <div style={S.card}>
            <div style={S.cardLabel}>RISK SCORE</div>
            <div style={S.cardValue}>
              {data.riskScore}
              <span style={{ fontSize: 16, color: '#6b7280', fontWeight: 400 }}> / 100</span>
            </div>
            <span style={{ ...S.statusBadge, background: rl.bg, color: rl.color }}>{rl.label} RISK</span>
            <div style={S.riskBarTrack}>
              <div style={{ ...S.riskBarFill, width: `${data.riskScore}%`, background: rl.color }} />
            </div>
          </div>
          <div style={S.card}>
            <div style={S.cardLabel}>ACCOUNT STATUS</div>
            <div style={{ ...S.cardValue, fontSize: 22, color: '#065f46' }}>
              {data.accountStatus}
            </div>
            <div style={S.cardSub}>✓ No restrictions applied</div>
          </div>
          <div style={S.card}>
            <div style={S.cardLabel}>FAILED ATTEMPTS</div>
            <div style={S.cardValue}>{data.failedAttempts}</div>
            <div style={S.cardSub}>Last 24 hours</div>
          </div>
        </div>

        {/* Details row */}
        <div style={S.detailsRow}>

          {/* Session details */}
          <div style={S.detailCard}>
            <div style={S.sectionTitle}>Session Details</div>
            {[
              ['Email',        data.email],
              ['Provider',     data.provider],
              ['Login Time',   data.loginTime],
              ['IP Address',   data.ipAddress],
              ['Known IP?',    data.knownIp ? '✓ Yes — recognised' : '✗ No — new IP'],
              ['LLM Analysis', data.llmAnalysis],
            ].map(([k, v]) => (
              <div key={k} style={S.infoRow}>
                <span style={S.infoKey}>{k}</span>
                <span style={{ ...S.infoVal, color: k === 'Known IP?' && data.knownIp ? '#065f46' : '#111827' }}>{v}</span>
              </div>
            ))}
          </div>

          {/* Risk signal breakdown */}
          <div style={S.detailCard}>
            <div style={S.sectionTitle}>Risk Signal Breakdown</div>
            {data.signals.map(([emoji, label, score, highlight]) => (
              <div key={label} style={S.infoRow}>
                <span style={S.infoKey}>{emoji} {label}</span>
                <span style={{ ...S.infoVal, color: highlight ? '#d97706' : '#6b7280' }}>{score}</span>
              </div>
            ))}
            <div style={{ ...S.infoRow, borderTop: '2px solid #e5e7eb', marginTop: 4 }}>
              <span style={{ ...S.infoKey, fontWeight: 700, color: '#111827' }}>Total Score</span>
              <span style={{ fontSize: 15, fontWeight: 700, color: rl.color }}>{data.riskScore} / 100 — {rl.label}</span>
            </div>
          </div>

        </div>

        {/* Activity log */}
        <div style={S.activityCard}>
          <div style={S.sectionTitle}>Recent Activity Log</div>
          {data.activityLog.map((entry, i) => (
            <div key={i} style={S.activityItem}>
              <div style={{ ...S.activityDot, background: entry.color }} />
              <div>
                <div style={S.activityText}>{entry.text}</div>
                <div style={S.activityTime}>{entry.time}</div>
              </div>
            </div>
          ))}
        </div>

      </div>
    </div>
  );
};

/* ══════════════════════════════════════════════════════════════════════════
   Build display data from backend response + token
═══════════════════════════════════════════════════════════════════════════ */
function buildDisplayData(apiData, token) {
  const riskScore     = apiData.riskScore     ?? 18;
  const failedAttempts= apiData.failedAttempts ?? 0;
  const accountStatus = apiData.accountStatus ?? apiData.status ?? 'ACTIVE';
  const email         = apiData.email         ?? '—';
  const provider      = apiData.provider      ?? 'LOCAL';
  const ipAddress     = apiData.ipAddress     ?? apiData.ip ?? '—';
  const knownIp       = apiData.knownIp       ?? true;
  const loginTime     = fmt(apiData.loginTime ?? apiData.timestamp ?? new Date().toISOString());
  const llmTriggered  = riskScore > 70;
  const llmDecision   = apiData.llmDecision   ?? null;
  const llmAnalysis   = llmTriggered
    ? `Triggered — Claude decision: ${llmDecision ?? 'CHALLENGE'}`
    : 'Not triggered (score < 70)';

  const tokenPreview  = token
    ? token.substring(0, 60) + '...'
    : 'No token';

  // Build signals from riskScore or from breakdown field
  const breakdown = apiData.riskSignals ?? apiData.signals ?? {};
  const signals = [
    ['🆕', 'New IP Address',    breakdown.newIp     != null ? `+${breakdown.newIp}`    : (knownIp ? '+0 (known IP)' : '+20'), !knownIp],
    ['🔨', 'Brute Force',       breakdown.bruteForce!= null ? `+${breakdown.bruteForce}`: '+0 (no failed attempts)', false],
    ['⚡', 'High Attempts',     breakdown.highAttempts!= null ? `+${breakdown.highAttempts}`: '+0', false],
    ['❌', 'Failed Attempts',   breakdown.failedPenalty!= null ? `+${breakdown.failedPenalty}`: '+0', false],
    ['💤', 'Long Inactivity',   breakdown.longInactivity!= null ? `+${breakdown.longInactivity}`: '+0', false],
    ['🌙', 'Unusual Hour',      breakdown.unusualHour!= null ? `+${breakdown.unusualHour}`: '+0', false],
  ];

  const activityLog = [
    {
      color: '#10b981',
      text: `✓ Login successful — JWT token issued`,
      time: `${loginTime}  |  ${ipAddress}  |  Risk: ${riskScore}/100 ${riskLevel(riskScore).label}`,
    },
    {
      color: '#2E75B6',
      text: `ℹ Risk score calculated — 6 signals evaluated, ${riskLevel(riskScore).label} result`,
      time: `${loginTime}  |  LLM analysis: ${llmTriggered ? 'triggered' : 'not triggered'}`,
    },
    ...(failedAttempts > 0 ? [{
      color: '#f59e0b',
      text: `⚠ ${failedAttempts} failed attempt${failedAttempts > 1 ? 's' : ''} recorded`,
      time: `Accumulated over the last 24 hours`,
    }] : []),
  ];

  return {
    email, provider, ipAddress, knownIp, loginTime,
    riskScore, failedAttempts, accountStatus,
    llmAnalysis, tokenPreview, signals, activityLog,
  };
}

/* ══════════════════════════════════════════════════════════════════════════
   Styles
═══════════════════════════════════════════════════════════════════════════ */
const S = {
  page: {
    minHeight: '100vh',
    background: '#f1f5f9',
    fontFamily: "'Outfit', -apple-system, BlinkMacSystemFont, sans-serif",
  },
  loadingScreen: {
    minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: '#f1f5f9', color: '#6b7280', fontSize: 16,
    fontFamily: "'Outfit', sans-serif",
  },

  /* topbar */
  topbar: {
    background: '#1F4E79', color: '#fff',
    padding: '0 32px',
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    height: 56, boxShadow: '0 2px 8px rgba(0,0,0,0.25)',
  },
  topbarBrand: {
    display: 'flex', alignItems: 'center', gap: 10,
    fontWeight: 800, fontSize: 17, letterSpacing: .5,
  },
  topbarRight: {
    display: 'flex', alignItems: 'center', gap: 14, fontSize: 13,
  },
  topbarBadge: { opacity: .85, fontSize: 12 },
  avatar: {
    width: 34, height: 34, background: '#2E75B6', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontWeight: 700, fontSize: 14,
  },
  topbarEmail: { fontSize: 13, opacity: .9 },
  logoutBtn: {
    padding: '6px 14px', background: 'rgba(255,255,255,0.15)',
    border: '1px solid rgba(255,255,255,0.3)', borderRadius: 6,
    color: '#fff', fontSize: 13, fontWeight: 600, cursor: 'pointer',
    fontFamily: 'inherit',
  },

  /* content */
  content: { maxWidth: 1100, margin: '0 auto', padding: '28px 20px' },

  /* welcome banner */
  welcomeBanner: {
    background: 'linear-gradient(135deg, #1F4E79, #2E75B6)',
    color: '#fff', borderRadius: 14, padding: '26px 30px',
    marginBottom: 24,
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    flexWrap: 'wrap', gap: 16,
    boxShadow: '0 4px 20px rgba(31,78,121,0.35)',
  },
  welcomeTitle: { fontSize: 22, fontWeight: 700, margin: 0, marginBottom: 4 },
  welcomeSub:   { fontSize: 13, opacity: .9, margin: 0 },
  jwtBox:       { maxWidth: 280 },
  jwtBadge: {
    background: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.25)',
    borderRadius: 8, padding: '8px 12px',
    fontSize: 11, fontFamily: "'JetBrains Mono', 'Courier New', monospace",
    wordBreak: 'break-all', opacity: .9, color: '#fff',
  },

  /* stat cards */
  cardsRow: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 20,
  },
  card: {
    background: '#fff', borderRadius: 12, padding: '22px 24px',
    boxShadow: '0 1px 6px rgba(0,0,0,0.07)',
  },
  cardLabel: { fontSize: 11, color: '#6b7280', fontWeight: 700, textTransform: 'uppercase', letterSpacing: .5, marginBottom: 8 },
  cardValue: { fontSize: 28, fontWeight: 800, color: '#111827', marginBottom: 6 },
  cardSub:   { fontSize: 12, color: '#6b7280' },

  riskBarTrack: { marginTop: 8, height: 7, borderRadius: 4, background: '#e5e7eb', overflow: 'hidden' },
  riskBarFill:  { height: '100%', borderRadius: 4, transition: 'width .6s ease' },

  statusBadge: {
    display: 'inline-block', padding: '3px 10px', borderRadius: 20,
    fontSize: 11, fontWeight: 700,
  },

  /* detail cards */
  detailsRow: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 },
  detailCard: {
    background: '#fff', borderRadius: 12, padding: '20px 22px',
    boxShadow: '0 1px 6px rgba(0,0,0,0.07)',
  },
  sectionTitle: { fontSize: 14, fontWeight: 700, color: '#1F4E79', marginBottom: 14 },
  infoRow: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '7px 0', borderBottom: '1px solid #f3f4f6', fontSize: 13,
  },
  infoKey: { color: '#6b7280' },
  infoVal: { fontWeight: 600, color: '#111827', textAlign: 'right', maxWidth: '55%' },

  /* activity */
  activityCard: {
    background: '#fff', borderRadius: 12, padding: '20px 22px',
    boxShadow: '0 1px 6px rgba(0,0,0,0.07)',
  },
  activityItem: { display: 'flex', alignItems: 'flex-start', gap: 12, padding: '10px 0', borderBottom: '1px solid #f3f4f6' },
  activityDot:  { width: 9, height: 9, borderRadius: '50%', marginTop: 5, flexShrink: 0 },
  activityText: { fontSize: 13, color: '#374151' },
  activityTime: { fontSize: 11, color: '#9ca3af', marginTop: 2 },
};

export default DashboardPage;