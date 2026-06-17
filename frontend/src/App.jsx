import React, { useState, useEffect } from 'react';
import { Provider, useSelector } from 'react-redux';
import { store } from './store';
import { ErrorBoundary } from './components/ErrorBoundary';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import VerifyEmail from './components/auth/VerifyEmail';
import ForgotPassword from './components/auth/ForgotPassword';
import ResetPassword from './components/auth/ResetPassword';
import Dashboard from './components/Dashboard';

function AppContent() {
  const [currentScreen, setCurrentScreen] = useState('login');
  const [emailForVerification, setEmailForVerification] = useState('');
  
  const user = useSelector((state) => state.chat.user);

  useEffect(() => {
    // Session restoration
    if (user) {
      setCurrentScreen('dashboard');
    } else {
      // Support password reset landing routes
      if (window.location.pathname === '/reset-password') {
        setCurrentScreen('reset-password');
      } else {
        setCurrentScreen('login');
      }
    }
  }, [user]);

  const navigateTo = (screen) => {
    setCurrentScreen(screen);
  };

  switch (currentScreen) {
    case 'login':
      return <Login onNavigate={navigateTo} />;
    case 'register':
      return (
        <Register 
          onNavigate={navigateTo} 
          setEmailForVerification={setEmailForVerification} 
        />
      );
    case 'verify-email':
      return <VerifyEmail onNavigate={navigateTo} email={emailForVerification} />;
    case 'forgot-password':
      return <ForgotPassword onNavigate={navigateTo} />;
    case 'reset-password':
      return <ResetPassword onNavigate={navigateTo} />;
    case 'dashboard':
      return <Dashboard onNavigate={navigateTo} />;
    default:
      return <Login onNavigate={navigateTo} />;
  }
}

export default function App() {
  return (
    <Provider store={store}>
      <ErrorBoundary>
        <AppContent />
      </ErrorBoundary>
    </Provider>
  );
}
