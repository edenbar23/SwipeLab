import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { API_ENDPOINTS } from './apiEndpoints';

const USE_MOCKS = __DEV__

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
      : "http://172.20.10.8:8080"); //real IP for IOS&ANDROID


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
    let refreshToken;
    let authProvider;
    if (Platform.OS === 'web') {
      refreshToken = localStorage.getItem("refreshToken");
      authProvider = localStorage.getItem("authProvider");
    } else {
      refreshToken = await SecureStore.getItemAsync("refreshToken");
      authProvider = await SecureStore.getItemAsync("authProvider");
    }

  if (refreshToken) {
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
          if (newAccess) {
            if (Platform.OS === 'web') {
              localStorage.setItem("token", newAccess);
            } else {
              await SecureStore.setItemAsync("token", newAccess);
            }
            // Require useAuthStore without top level static import to avoid circular dependency
            const { useAuthStore } = require("../stores/authStore");
            useAuthStore.getState().setAuth(newAccess, "ADMIN", refreshToken);

            return fetch(backendUrl + input, {
              ...init,
              headers: {
                ...(init?.headers ?? {}),
                Authorization: `Bearer ${newAccess}`,
              },
            });
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
            if (Platform.OS === 'web') {
              localStorage.setItem("token", newAccess);
              localStorage.setItem("refreshToken", newRefresh);
            } else {
              await SecureStore.setItemAsync("token", newAccess);
              await SecureStore.setItemAsync("refreshToken", newRefresh);
            }

            const { useAuthStore } = require("../stores/authStore");
            const currentRole = useAuthStore.getState().role;
            useAuthStore.getState().setAuth(newAccess, currentRole, newRefresh);

            return fetch(backendUrl + input, {
              ...init,
              headers: {
                ...(init?.headers ?? {}),
                Authorization: `Bearer ${newAccess}`,
              },
            });
          }
        }
      }
    } catch (e) {
      console.error("Refresh failed", e);
    }
  }

  // If no refresh token or refresh failed, we must logout
  const { useAuthStore } = require("../stores/authStore");
  useAuthStore.getState().logout();
  // Optional: show "Session expired" message via an event dispatcher or local alert
  try {
    if (Platform.OS === 'web') {
      alert("Session expired. Please log in again.");
    }
  } catch (e) { }
}

return response;
}
