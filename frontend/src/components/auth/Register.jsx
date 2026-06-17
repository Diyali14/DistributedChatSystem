import React, { useState } from 'react';

export default function Register({ onNavigate, setEmailForVerification }) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password }),
      });

      const text = await response.text();

      if (response.ok) {
        setEmailForVerification(email);
        onNavigate('verify-email');
      } else {
        let errJson;
        try {
          errJson = JSON.parse(text);
        } catch {
          errJson = { error: text };
        }
        setError(errJson.error || 'Registration failed');
      }
    } catch (err) {
      setError('Connection failure. Check backend service status.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h1 className="auth-logo">ChatSphere <span className="logo-badge">X</span></h1>
        <p className="auth-subtitle">Create your secure distributed credentials</p>

        <form onSubmit={handleRegister} className="auth-form">
          {error && <div className="auth-error">{error}</div>}

          <div className="form-group">
            <label>Username</label>
            <input 
              type="text" 
              required 
              placeholder="e.g. bob" 
              value={username} 
              onChange={e => setUsername(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label>Email Address</label>
            <input 
              type="email" 
              required 
              placeholder="e.g. bob@mail.com" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input 
              type="password" 
              required 
              placeholder="••••••••" 
              value={password} 
              onChange={e => setPassword(e.target.value)} 
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Creating Account...' : 'Register'}
          </button>
        </form>

        <div className="auth-footer">
          Already have an account?{' '}
          <button className="link-btn" onClick={() => onNavigate('login')}>
            Sign In
          </button>
        </div>
      </div>
    </div>
  );
}
