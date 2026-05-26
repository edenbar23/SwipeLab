import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { API_ENDPOINTS } from './apiEndpoints';

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
const backendUrl = process.env.EXPO_PUBLIC_API_URL ||
  (Platform.OS === "web"
    ? "http://localhost:8080"
    : "http://192.168.1.133:8080"); //real IP for IOS&ANDROID

export async function forceTokenRefresh(): Promise<boolean> {
  if (isRefreshing) return new Promise((resolve) => {
    addRefreshSubscriber((newToken) => resolve(!!newToken));
  });

  isRefreshing = true;

  let refreshToken;
  let authProvider;
  if (Platform.OS === 'web') {
    refreshToken = localStorage.getItem("refreshToken");
    authProvider = localStorage.getItem("authProvider");
  } else {
    refreshToken = await SecureStore.getItemAsync("refreshToken");
    authProvider = await SecureStore.getItemAsync("authProvider");
  }

  if (!refreshToken) {
    isRefreshing = false;
    onRefreshed(null);
    return false;
  }

  try {
    if (authProvider === "STARDBI") {
      const refreshResponse = await fetch(API_ENDPOINTS.STARDBI.REFRESH, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refresh: refreshToken }),
      });

      if (refreshResponse.ok) {
        const data = await refreshResponse.json();
        const newAccess = data.access;
        const newRefresh = data.refresh || refreshToken;

        if (newAccess) {
          const { useAuthStore } = require("../stores/authStore");
          await useAuthStore.getState().updateTokens(newAccess, newRefresh);

          isRefreshing = false;
          onRefreshed(newAccess);
          return true;
        }
      }
    } else {
      // SwipeLab backend refresh
      const refreshResponse = await fetch(backendUrl + API_ENDPOINTS.AUTH.REFRESH, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${refreshToken}`,
        },
      });

      if (refreshResponse.ok) {
        const data = await refreshResponse.json();
        const newAccess = data.accessToken;
        const newRefresh = data.refreshToken || refreshToken;

        if (newAccess) {
          const { useAuthStore } = require("../stores/authStore");
          await useAuthStore.getState().updateTokens(newAccess, newRefresh);

          isRefreshing = false;
          onRefreshed(newAccess);
          return true;
        }
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


  const backendUrl = process.env.EXPO_PUBLIC_API_URL ||
    (Platform.OS === "web"
      ? "http://localhost:8080"
      : "http://192.168.1.133:8080"); //real IP for IOS&ANDROID


  // Get token from storage
  let token;
  if (Platform.OS === 'web') {
    token = localStorage.getItem("token");
  } else {
    token = await SecureStore.getItemAsync("token");
  }

  const fullUrl = backendUrl + input;
  console.log("[apiFetch] Full exact URL being fetch'ed:", fullUrl);
  console.log("[apiFetch] Header Token Present:", token ? "YES (length " + token.length + ")" : "NO (null/undefined)");

  const response = await fetch(fullUrl, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (response.status === 401) {
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
      let newToken;
      if (Platform.OS === 'web') {
        newToken = localStorage.getItem("token");
      } else {
        newToken = await SecureStore.getItemAsync("token");
      }
      
      if (newToken) {
        return fetch(fullUrl, {
          ...init,
          headers: {
            ...(init?.headers ?? {}),
            Authorization: `Bearer ${newToken}`,
          },
        });
      }
    }

    // If no refresh token or refresh failed, we must logout
    
    const { useAuthStore } = require("../stores/authStore");
    useAuthStore.getState().setSessionExpiredMessage(true);
    setTimeout(() => {
      useAuthStore.getState().logout();
      useAuthStore.getState().setSessionExpiredMessage(false);
    }, 2000);
  }

  // Global ban detection — any 403 with ACCOUNT_BANNED triggers BannedScreen
  if (response.status === 403) {
    try {
      const cloned = response.clone();
      const body = await cloned.json();
      if (body?.errorCode === 'ACCOUNT_BANNED') {
        const { useAuthStore } = require("../stores/authStore");
        useAuthStore.getState().setIsBanned(true);
      }
    } catch {
      // If body parsing fails, it's a regular 403 — pass through
    }
  }

  return response;
}
