import React, { useState } from 'react';

export default function ForgotPassword({ onNavigate }) {
  const [email, setEmail] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleForgot = async (e) => {
    e.preventDefault();
    setError('');
    setMsg('');
    setLoading(true);

    try {
      const response = await fetch(`/api/auth/forgot-password?email=${encodeURIComponent(email)}`, {
        method: 'POST',
      });
      const text = await response.text();

      if (response.ok) {
        setMsg(text || 'Reset link printed in console logs.');
      } else {
        setError(text || 'Request failed.');
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
        <h1 className="auth-logo">Reset Password</h1>
        <p className="auth-subtitle">Enter your email to generate a reset link in service logs.</p>

        <form onSubmit={handleForgot} className="auth-form">
          {error && <div className="auth-error">{error}</div>}
          {msg && <div className="auth-success">{msg}</div>}

          <div className="form-group">
            <label>Email Address</label>
            <input 
              type="email" 
              required 
              placeholder="alice@mail.com" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Sending...' : 'Send Reset Link'}
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
