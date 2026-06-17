import React, { useState } from 'react';

export default function VerifyEmail({ onNavigate, email }) {
  const [token, setToken] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setMsg('');
    setLoading(true);

    try {
      const response = await fetch(`/api/auth/verify?email=${encodeURIComponent(email)}&token=${encodeURIComponent(token)}`);
      const text = await response.text();

      if (response.ok) {
        setMsg(text || 'Verification successful!');
        setTimeout(() => {
          onNavigate('login');
        }, 2000);
      } else {
        setError(text || 'Verification failed');
      }
    } catch (err) {
      setError('Connection failure.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h1 className="auth-logo">Verify Email</h1>
        <p className="auth-subtitle">Verification token sent to {email || 'your email'}.</p>
        <p className="hint-text">Check the auth-service console logs for the verification token link.</p>

        <form onSubmit={handleVerify} className="auth-form">
          {error && <div className="auth-error">{error}</div>}
          {msg && <div className="auth-success">{msg}</div>}

          <div className="form-group">
            <label>Verification Token</label>
            <input 
              type="text" 
              required 
              placeholder="Paste token here" 
              value={token} 
              onChange={e => setToken(e.target.value)} 
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Verifying...' : 'Verify Email'}
          </button>
        </form>

        <div className="auth-footer">
          Back to{' '}
          <button className="link-btn" onClick={() => onNavigate('login')}>
            Sign In
          </button>
        </div>
      </div>
    </div>
  );
}
