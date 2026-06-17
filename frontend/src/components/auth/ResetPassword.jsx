import React, { useState, useEffect } from 'react';

export default function ResetPassword({ onNavigate }) {
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const emailParam = params.get('email');
    const tokenParam = params.get('token');
    if (emailParam) setEmail(emailParam);
    if (tokenParam) setToken(tokenParam);
  }, []);

  const handleReset = async (e) => {
    e.preventDefault();
    setError('');
    setMsg('');
    setLoading(true);

    try {
      const response = await fetch(
        `/api/auth/reset-password?email=${encodeURIComponent(email)}&token=${encodeURIComponent(token)}&newPassword=${encodeURIComponent(password)}`,
        { method: 'POST' }
      );
      const text = await response.text();

      if (response.ok) {
        setMsg(text || 'Password reset successful!');
        setTimeout(() => {
          onNavigate('login');
        }, 2000);
      } else {
        setError(text || 'Reset failed.');
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
        <h1 className="auth-logo">Set New Password</h1>
        <p className="auth-subtitle">Fill in the fields to complete the credentials update.</p>

        <form onSubmit={handleReset} className="auth-form">
          {error && <div className="auth-error">{error}</div>}
          {msg && <div className="auth-success">{msg}</div>}

          <div className="form-group">
            <label>Email Address</label>
            <input 
              type="email" 
              required 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label>Verification Token</label>
            <input 
              type="text" 
              required 
              value={token} 
              onChange={e => setToken(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label>New Password</label>
            <input 
              type="password" 
              required 
              placeholder="••••••••" 
              value={password} 
              onChange={e => setPassword(e.target.value)} 
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Updating...' : 'Reset Password'}
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
