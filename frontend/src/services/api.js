import { store } from '../store';
import { setAuth, clearAuth } from '../store/chatSlice';

export async function apiFetch(url, options = {}) {
  let token = localStorage.getItem('cs_token');

  options.headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    options.headers['Authorization'] = `Bearer ${token}`;
  }

  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  const finalUrl = url.startsWith('/') ? `${baseUrl}${url}` : url;

  let response = await fetch(finalUrl, options);

  // Expired token interceptor
  if (response.status === 401) {
    const refreshToken = localStorage.getItem('cs_refresh');
    if (refreshToken) {
      try {
        log('Attempting refresh token rotation...');
        const refreshUrl = `${baseUrl}/api/auth/refresh`;
        const refreshResponse = await fetch(refreshUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            refreshToken,
            deviceName: navigator.userAgent.substring(0, 30),
          }),
        });

        if (refreshResponse.ok) {
          const data = await refreshResponse.json();
          // Dispatch new auth pair
          store.dispatch(setAuth({
            user: data.user,
            accessToken: data.accessToken,
            refreshToken: data.refreshToken,
          }));

          // Retry the original failed request carrying the new access token
          options.headers['Authorization'] = `Bearer ${data.accessToken}`;
          response = await fetch(finalUrl, options);
        } else {
          // Refresh token expired or reuse compromised
          log('Refresh token rotation failed. Logging out user...');
          store.dispatch(clearAuth());
        }
      } catch (err) {
        log('Token refresh error: ' + err);
        store.dispatch(clearAuth());
      }
    } else {
      store.dispatch(clearAuth());
    }
  }

  return response;
}

function log(msg) {
  console.log('[Api Service] ' + msg);
}
