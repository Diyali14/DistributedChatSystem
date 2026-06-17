import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { setAuth } from '../../store/chatSlice';
import { apiFetch } from '../../services/api';

export default function Login({ onNavigate }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const dispatch = useDispatch();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          usernameOrEmail: username,
          password,
          deviceName: 'Browser Client (' + navigator.platform + ')',
        }),
      });

      if (response.ok) {
        const data = await response.json();
        dispatch(setAuth({
          user: data.user,
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        }));
        onNavigate('dashboard');
      } else {
        const errorText = await response.text();
        let errJson;
        try {
          errJson = JSON.parse(errorText);
        } catch {
          errJson = { error: errorText };
        }
        setError(errJson.error || 'Invalid username or password');
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
        <p className="auth-subtitle">Distributed Real-Time Enterprise Messaging</p>

        <form onSubmit={handleLogin} className="auth-form">
          {error && <div className="auth-error">{error}</div>}

          <div className="form-group">
            <label>Username or Email</label>
            <input 
              type="text" 
              required 
              placeholder="alice or alice@mail.com" 
              value={username} 
              onChange={e => setUsername(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <div className="label-row">
              <label>Password</label>
              <button 
                type="button" 
                className="link-btn text-sm" 
                onClick={() => onNavigate('forgot-password')}
              >
                Forgot?
              </button>
            </div>
            <input 
              type="password" 
              required 
              placeholder="••••••••" 
              value={password} 
              onChange={e => setPassword(e.target.value)} 
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          Don't have an account?{' '}
          <button className="link-btn" onClick={() => onNavigate('register')}>
            Create Account
          </button>
        </div>
      </div>
    </div>
  );
}
