import React from 'react';

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[Error Boundary] Uncaught error: ', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={styles.errorContainer}>
          <div style={styles.errorBox}>
            <h1 style={styles.errorTitle}>Something Went Wrong</h1>
            <p style={styles.errorMessage}>
              {this.state.error?.message || 'An unexpected runtime error crashed the view.'}
            </p>
            <button 
              style={styles.errorButton} 
              onClick={() => window.location.reload()}
            >
              Reload ChatSphere X
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

const styles = {
  errorContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100vh',
    width: '100vw',
    backgroundColor: '#0a0b0d',
    color: '#ffffff',
    fontFamily: 'Inter, system-ui, sans-serif',
  },
  errorBox: {
    padding: '40px',
    borderRadius: '16px',
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    backdropFilter: 'blur(10px)',
    textAlign: 'center',
    maxWidth: '480px',
    boxShadow: '0 20px 40px rgba(0, 0, 0, 0.5)',
  },
  errorTitle: {
    fontSize: '24px',
    fontWeight: '700',
    marginBottom: '16px',
    color: '#ff4d4d',
  },
  errorMessage: {
    fontSize: '15px',
    color: '#a0a5b5',
    lineHeight: '1.6',
    marginBottom: '28px',
  },
  errorButton: {
    padding: '12px 24px',
    borderRadius: '8px',
    backgroundColor: '#3b82f6',
    color: '#ffffff',
    border: 'none',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  }
};
