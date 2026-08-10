import { Platform } from 'react-native';
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { getAccessToken, getRefreshToken, getItem } from '@/utils/tokenUtils';
import Toast from 'react-native-toast-message';

const USE_MOCKS = __DEV__

let isRefreshing = false;
let refreshSubscribers: ((token: string | null) => void)[] = [];

const onRefreshed = (token: string | null) => {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
};

const addRefreshSubscriber = (cb: (token: string | null) => void) => {
  refreshSubscribers.push(cb);
};
export const backendUrl = process.env.EXPO_PUBLIC_API_URL ||
  (Platform.OS === "web"
    ? "http://localhost:8080"
    : "http://192.168.1.133:8080"); //real IP for IOS&ANDROID

export async function forceTokenRefresh(): Promise<boolean> {
  if (isRefreshing) return new Promise((resolve) => {
    addRefreshSubscriber((newToken) => resolve(!!newToken));
  });

  isRefreshing = true;

  let refreshToken = await getRefreshToken();
  let authProvider = await getItem("authProvider");

  if (!refreshToken && Platform.OS !== 'web') {
    isRefreshing = false;
    onRefreshed(null);
    return false;
  }

  try {
    // SwipeLab backend refresh (works for both local users and Stardbi researchers via BFF)
    const refreshResponse = await fetch(backendUrl + API_ENDPOINTS.AUTH.REFRESH, {
      method: "POST",
      credentials: "include", // Important for sending the refresh cookie on web
      headers: refreshToken ? { "Authorization": `Bearer ${refreshToken}` } : {},
    });

    if (refreshResponse.ok) {
      const data = await refreshResponse.json();
      const newAccess = data.accessToken;
      const newRefresh = data.refreshToken || refreshToken;

      if (newAccess) {
        const { useAuthStore } = require("@/stores/authStore");
        await useAuthStore.getState().updateTokens(newAccess, newRefresh);

        isRefreshing = false;
        onRefreshed(newAccess);
        return true;
      }
    }
  } catch (e) {
    console.error("Force refresh failed", e);
  }

  isRefreshing = false;
  onRefreshed(null);
  return false;
}

export async function apiFetch(
  input: RequestInfo,
  init?: RequestInit
): Promise<Response> {
  const url = typeof input === 'string' ? input : input.url
  const method = (init?.method ?? 'GET').toUpperCase() as any

  // if (USE_MOCKS) {
  //   const mockResponse = await mockRouter(url, method, init)
  //   if (mockResponse) {
  //     console.log('[MOCK]', method, url)
  //     return mockResponse
  //   }
  // }


  // Get token from storage (null on web if cookies are used)
  const token = await getAccessToken();

  const fullUrl = url.startsWith('http') ? url : backendUrl + url;
  if (__DEV__) {
    console.log("[apiFetch] Full exact URL being fetch'ed:", fullUrl);
  }

  let response: Response;
  try {
    response = await fetch(fullUrl, {
      ...init,
      credentials: "include", // Required for HttpOnly cookies on web
      headers: {
        ...(init?.headers ?? {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
  } catch (err) {
    // Network error (e.g., ERR_CONNECTION_REFUSED, offline)
    (async () => {
      try {
        const healthRes = await fetch(backendUrl + API_ENDPOINTS.SYSTEM.HEALTH, { method: 'GET' });
        if (!healthRes.ok) {
          const { useAppStateStore } = require('@/stores/appStateStore');
          useAppStateStore.getState().setMaintenanceMode(true);
        }
      } catch (e) {
        // Backend is completely unreachable
        const { useAppStateStore } = require('@/stores/appStateStore');
        useAppStateStore.getState().setMaintenanceMode(true);
      }
    })();
    throw err;
  }

  if (response.status === 401) {
    // Check for Stardbi session expiration before attempting refresh
    try {
      const cloned = response.clone();
      const body = await cloned.json();
      if (body?.errorCode === 'STARDBI_SESSION_EXPIRED') {
        const { useAuthStore } = require("@/stores/authStore");
        useAuthStore.getState().setSessionExpiredMessage(true);
        setTimeout(() => {
          useAuthStore.getState().logout();
          useAuthStore.getState().setSessionExpiredMessage(false);
        }, 2000);
        return response; // Exit early, do not try to refresh SwipeLab JWT
      }
    } catch {}

    // Do not intercept 401s for login, refresh, or logout endpoints
    const urlString = input.toString();
    if (
      urlString.includes('/login') ||
      urlString.includes('/refresh') ||
      urlString.includes('/logout')
    ) {
      return response;
    }

    if (isRefreshing) {
      return new Promise((resolve) => {
        addRefreshSubscriber((newToken) => {
          if (newToken) {
            resolve(
              fetch(fullUrl, {
                ...init,
                headers: {
                  ...(init?.headers ?? {}),
                  Authorization: `Bearer ${newToken}`,
                },
              })
            );
          } else {
            resolve(response); // Return original 401 if refresh failed
          }
        });
      });
    }

    const refreshSuccess = await forceTokenRefresh();
    
    if (refreshSuccess) {
      const newToken = await getAccessToken();
      
      return fetch(fullUrl, {
        ...init,
        credentials: "include",
        headers: {
          ...(init?.headers ?? {}),
          ...(newToken ? { Authorization: `Bearer ${newToken}` } : {}),
        },
      });
    }

    // If no refresh token or refresh failed, we must logout
    const { useAuthStore } = require("@/stores/authStore");
    
    const isAuthenticatedFlag = await getItem("isAuthenticated");
    const localToken = await getAccessToken();
    const hadRefreshToken = Platform.OS === 'web' ? isAuthenticatedFlag === 'true' : !!localToken;

    if (hadRefreshToken) {
      useAuthStore.getState().setSessionExpiredMessage(true);
    }
    
    setTimeout(() => {
      useAuthStore.getState().logout();
      if (hadRefreshToken) {
        useAuthStore.getState().setSessionExpiredMessage(false);
      }
    }, 2000);
  }

  // Global ban detection — any 403 with ACCOUNT_BANNED triggers BannedScreen
  if (response.status === 403) {
    try {
      const cloned = response.clone();
      const body = await cloned.json();
      if (body?.errorCode === 'ACCOUNT_BANNED') {
        const { useAuthStore } = require("@/stores/authStore");
        useAuthStore.getState().setIsBanned(true);
      }
    } catch {
      // If body parsing fails, it's a regular 403 — pass through
    }
  }

  // Handle generic errors (non-401, non-403) with a Toast, excluding 5xx errors which are handled below
  if (!response.ok && response.status !== 401 && response.status !== 403 && response.status < 500) {
    const urlString = input.toString();
    if (!urlString.includes('/login') && !urlString.includes('/refresh')) {
      Toast.show({
        type: 'error',
        text1: 'API Error',
        text2: `Request failed with status ${response.status}`,
      });
    }
  }

  // 500 Maintenance Mode handling
  if (response.status >= 500) {
    (async () => {
      try {
        const healthRes = await fetch(backendUrl + API_ENDPOINTS.SYSTEM.HEALTH, { method: 'GET' });
        if (!healthRes.ok) {
          const { useAppStateStore } = require('@/stores/appStateStore');
          useAppStateStore.getState().setMaintenanceMode(true);
        } else {
          // The backend is alive, it was just an isolated 500 error.
          Toast.show({
            type: 'error',
            text1: 'Server Error',
            text2: 'An unexpected internal error occurred.',
          });
        }
      } catch (e) {
        // Fetch failed entirely
        const { useAppStateStore } = require('@/stores/appStateStore');
        useAppStateStore.getState().setMaintenanceMode(true);
      }
    })();
  }
  
  return response;
}
