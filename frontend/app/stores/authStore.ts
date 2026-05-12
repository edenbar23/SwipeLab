// stores token, role, and user info
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { create } from "zustand";
import { apiFetch } from "../api/apiFetch";
import { useModeStore } from "./modeStore";
import { API_ENDPOINTS } from '../api/apiEndpoints';
import { jwtDecode } from "jwt-decode";


type Role = "USER" | "RESEARCHER" | null;

interface AuthState {
  token: string | null;
  role: Role;
  isSuperAdmin: boolean;
  authProvider: "LOCAL" | "STARDBI" | null;
  isLoading: boolean;
  setAuth: (token: string, role: Role, refreshToken?: string) => Promise<void>;
  setExternalAuth: (token: string, refreshToken: string, lifetime: number, username: string) => Promise<void>;
  updateTokens: (token: string, refreshToken: string) => Promise<void>;
  logout: () => Promise<void>;
  initialize: () => Promise<void>;
  sessionExpiredMessage: boolean;
  setSessionExpiredMessage: (show: boolean) => void;
  setIsSuperAdmin: (isSuperAdmin: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  role: null,
  isSuperAdmin: false,
  authProvider: null,
  isLoading: true,
  sessionExpiredMessage: false,
  setSessionExpiredMessage: (show) => set({ sessionExpiredMessage: show }),
  setIsSuperAdmin: (isSuperAdmin) => {
    set({ isSuperAdmin });
    if (Platform.OS === 'web') {
      localStorage.setItem("isSuperAdmin", isSuperAdmin ? "true" : "false");
    } else {
      SecureStore.setItemAsync("isSuperAdmin", isSuperAdmin ? "true" : "false").catch(console.error);
    }
  },

  setAuth: async (token, role, refreshToken) => {
    set({ token, role, authProvider: "LOCAL" });
    if (Platform.OS === 'web') {
      localStorage.setItem("token", token);
      localStorage.setItem("authProvider", "LOCAL");
      if (role) localStorage.setItem("role", role);
      if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
    } else {
      await SecureStore.setItemAsync("token", token);
      await SecureStore.setItemAsync("authProvider", "LOCAL");
      if (role) await SecureStore.setItemAsync("role", role);
      if (refreshToken) await SecureStore.setItemAsync("refreshToken", refreshToken);
    }

    // Automatically set researcher mode if role is RESEARCHER
    if (role === "RESEARCHER") {
      useModeStore.getState().setMode("researcher"); // keeping mode string same for now if modeStore uses researcher, but we'll update modeStore next
    } else {
      useModeStore.getState().setMode("USER");
    }
  },

  setExternalAuth: async (token, refreshToken, lifetime, username) => {
    set({ token, role: "RESEARCHER", authProvider: "STARDBI" });
    if (Platform.OS === 'web') {
      localStorage.setItem("token", token);
      localStorage.setItem("role", "RESEARCHER");
      localStorage.setItem("authProvider", "STARDBI");
      localStorage.setItem("refreshToken", refreshToken);
      localStorage.setItem("username", username);
      localStorage.setItem("lifetime", lifetime.toString());
    } else {
      await SecureStore.setItemAsync("token", token);
      await SecureStore.setItemAsync("role", "RESEARCHER");
      await SecureStore.setItemAsync("authProvider", "STARDBI");
      await SecureStore.setItemAsync("refreshToken", refreshToken);
      await SecureStore.setItemAsync("username", username);
      await SecureStore.setItemAsync("lifetime", lifetime.toString());
    }
    useModeStore.getState().setMode("researcher");
  },

  updateTokens: async (token, refreshToken) => {
    set({ token });
    if (Platform.OS === 'web') {
      localStorage.setItem("token", token);
      localStorage.setItem("refreshToken", refreshToken);
    } else {
      await SecureStore.setItemAsync("token", token);
      await SecureStore.setItemAsync("refreshToken", refreshToken);
    }
  },

  logout: async () => {
    // Guard against duplicate logout calls (React Strict Mode / 401 race)
    const state = useAuthStore.getState();
    if (!state.token && !state.role) {
      console.log("[logout] Already logged out, skipping duplicate call.");
      return;
    }

    // 1. Get the refresh token before clearing storage
    let refreshToken = null;
    if (Platform.OS === 'web') {
      refreshToken = localStorage.getItem("refreshToken");
    } else {
      refreshToken = await SecureStore.getItemAsync("refreshToken");
    }

    // 2. Clear frontend state immediately to prevent re-entry
    set({ token: null, role: null, authProvider: null, isSuperAdmin: false });
    if (Platform.OS === 'web') {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("authProvider");
      localStorage.removeItem("isSuperAdmin");
    } else {
      await SecureStore.deleteItemAsync("token");
      await SecureStore.deleteItemAsync("role");
      await SecureStore.deleteItemAsync("refreshToken");
      await SecureStore.deleteItemAsync("authProvider");
      await SecureStore.deleteItemAsync("isSuperAdmin");
    }

    // 3. Call the backend to invalidate the refresh token (fire-and-forget)
    if (refreshToken) {
      const backendUrl = process.env.EXPO_PUBLIC_API_URL ||
        (Platform.OS === "web"
          ? "http://localhost:8080"
          : "http://192.168.1.133:8080");

      fetch(backendUrl + API_ENDPOINTS.AUTH.LOGOUT, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${refreshToken}`
        }
      })
      .then(res => {
        if (res.status === 401) {
          console.warn("[logout] Server returned 401 on logout, but local cleanup is already complete.");
        }
      })
      .catch(e => console.error("Logout request failed", e));
    }

    // 4. Clear mode and query cache
    useModeStore.getState().resetMode?.();
    const { queryClient } = require("../queryClient");
    queryClient.clear();
  },

  initialize: async () => {
    try {
      let token, role, authProvider, isSuperAdmin = false;
      if (Platform.OS === 'web') {
        token = localStorage.getItem("token");
        role = localStorage.getItem("role") as Role;
        authProvider = localStorage.getItem("authProvider") as "LOCAL" | "STARDBI" | null;
        isSuperAdmin = localStorage.getItem("isSuperAdmin") === "true";
      } else {
        token = await SecureStore.getItemAsync("token");
        role = (await SecureStore.getItemAsync("role")) as Role;
        authProvider = (await SecureStore.getItemAsync("authProvider")) as "LOCAL" | "STARDBI" | null;
        isSuperAdmin = (await SecureStore.getItemAsync("isSuperAdmin")) === "true";
      }

      if (token) {
        let isExpired = false;
        try {
          const decoded = jwtDecode<{ exp?: number }>(token);
          if (decoded.exp && Date.now() >= decoded.exp * 1000) {
            isExpired = true;
          }
        } catch (e) {
          console.error("Invalid token on boot:", e);
          isExpired = true;
        }

        if (isExpired) {
          console.log("[authStore] Token expired on boot. Clearing state.");
          set({ sessionExpiredMessage: true });
          setTimeout(async () => {
            if (Platform.OS === 'web') {
              localStorage.removeItem("token");
              localStorage.removeItem("role");
              localStorage.removeItem("refreshToken");
              localStorage.removeItem("authProvider");
              localStorage.removeItem("isSuperAdmin");
            } else {
              await SecureStore.deleteItemAsync("token");
              await SecureStore.deleteItemAsync("role");
              await SecureStore.deleteItemAsync("refreshToken");
              await SecureStore.deleteItemAsync("authProvider");
              await SecureStore.deleteItemAsync("isSuperAdmin");
            }
            set({ token: null, role: null, authProvider: null, isSuperAdmin: false, sessionExpiredMessage: false });
          }, 2000);
        } else {
          set({ token, role, authProvider, isSuperAdmin });

          if (role === "RESEARCHER") {
            useModeStore.getState().setMode("researcher");
          } else {
            useModeStore.getState().setMode("USER");
          }
        }
      }
    } catch (e) {
      console.error("Failed to initialize auth", e);
    } finally {
      set({ isLoading: false });
    }
  },
}));
