import { ref } from 'vue';

const STORAGE_KEY = 's14p21a506.cognito.access_token';
const browserAvailable = typeof window !== 'undefined';
const authToken = ref(browserAvailable ? window.localStorage.getItem(STORAGE_KEY) ?? '' : '');

function clean(value) {
  return (value ?? '').trim();
}

function normalizeHostedUiDomain(value) {
  return clean(value).replace(/^https?:\/\//, '').replace(/\/+$/, '');
}

export function useAuthToken() {
  return authToken;
}

export function getAuthToken() {
  return authToken.value;
}

export function setAuthToken(value) {
  authToken.value = clean(value);

  if (!browserAvailable) {
    return;
  }

  if (authToken.value) {
    window.localStorage.setItem(STORAGE_KEY, authToken.value);
    return;
  }

  window.localStorage.removeItem(STORAGE_KEY);
}

export function clearAuthToken() {
  setAuthToken('');
}

export function decodeJwtPayload(token) {
  const value = clean(token);
  if (!value) {
    return null;
  }

  const parts = value.split('.');
  if (parts.length < 2) {
    return null;
  }

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    return JSON.parse(atob(padded));
  } catch (error) {
    return null;
  }
}

export function getCognitoConfig() {
  const redirectFallback = browserAvailable ? `${window.location.origin}/test` : '';

  return {
    region: clean(import.meta.env.VITE_COGNITO_AWS_REGION),
    userPoolId: clean(import.meta.env.VITE_COGNITO_USER_POOL_ID),
    clientId: clean(import.meta.env.VITE_COGNITO_CLIENT_ID),
    hostedUiDomain: normalizeHostedUiDomain(import.meta.env.VITE_COGNITO_HOSTED_UI_DOMAIN),
    redirectUri: clean(import.meta.env.VITE_COGNITO_REDIRECT_URI) || redirectFallback,
  };
}

export function getCognitoHostedUiLoginUrl() {
  const { hostedUiDomain, clientId, redirectUri } = getCognitoConfig();
  if (!hostedUiDomain || !clientId || !redirectUri) {
    return '';
  }

  const params = new URLSearchParams({
    client_id: clientId,
    response_type: 'token',
    scope: 'openid email profile',
    redirect_uri: redirectUri,
  });

  return `https://${hostedUiDomain}/login?${params.toString()}`;
}

export function consumeHostedUiAccessToken() {
  if (!browserAvailable || !window.location.hash) {
    return '';
  }

  const params = new URLSearchParams(window.location.hash.slice(1));
  const accessToken = clean(params.get('access_token'));

  if (!accessToken) {
    return '';
  }

  setAuthToken(accessToken);
  window.history.replaceState({}, document.title, `${window.location.pathname}${window.location.search}`);
  return accessToken;
}
