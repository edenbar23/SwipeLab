// stores token, role, and user info
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { create } from "zustand";
import { jwtDecode } from "jwt-decode";
import { setTokens, clearTokens, setItem, getItem, removeItem } from "@/utils/tokenUtils";
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { useModeStore } from "@/stores/modeStore";


type Role = "USER" | "RESEARCHER" | null;

interface AuthState {
  token: string | null;
  role: Role;
  isSuperAdmin: boolean;
  isBanned: boolean;
  authProvider: "LOCAL" | "STARDBI" | null;
  isLoading: boolean;
  setAuth: (token: string, role: Role, refreshToken?: string) => Promise<void>;
  setExternalAuth: (token: string, refreshToken: string, username: string) => Promise<void>;
  updateTokens: (token: string, refreshToken: string) => Promise<void>;
  logout: () => Promise<void>;
  initialize: () => Promise<void>;
  sessionExpiredMessage: boolean;
  setSessionExpiredMessage: (show: boolean) => void;
  setIsSuperAdmin: (isSuperAdmin: boolean) => void;
  setIsBanned: (isBanned: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  role: null,
  isSuperAdmin: false,
  isBanned: false,
  authProvider: null,
  isLoading: true,
  sessionExpiredMessage: false,
  setIsBanned: (isBanned) => set({ isBanned }),
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
    await setTokens(token, refreshToken);
    await setItem("authProvider", "LOCAL");
    if (role) await setItem("role", role);

    // Automatically set researcher mode if role is RESEARCHER
    if (role === "RESEARCHER") {
      useModeStore.getState().setMode("researcher");
    } else {
      useModeStore.getState().setMode("USER");
    }
  },

  setExternalAuth: async (token, refreshToken, username) => {
    set({ token, role: "RESEARCHER", authProvider: "STARDBI" });
    await setTokens(token, refreshToken);
    await setItem("role", "RESEARCHER");
    await setItem("authProvider", "STARDBI");
    await setItem("username", username);
    useModeStore.getState().setMode("researcher");
  },

  updateTokens: async (token, refreshToken) => {
    set({ token });
    await setTokens(token, refreshToken);
  },

  logout: async () => {
    // Guard against duplicate logout calls (React Strict Mode / 401 race)
    const state = useAuthStore.getState();
    if (!state.token && !state.role) {
      console.log("[logout] Already logged out, skipping duplicate call.");
      return;
    }

    // 1. Clear frontend state immediately to prevent re-entry
    set({ token: null, role: null, authProvider: null, isSuperAdmin: false, isBanned: false });
    await clearTokens();
    await removeItem("role");
    await removeItem("authProvider");
    await removeItem("isSuperAdmin");
    await removeItem("username");

    // 2. Call the backend to invalidate the refresh token / clear cookies (fire-and-forget)
    // We import apiFetch inline here because we just need to hit the logout endpoint,
    // but we can also just use standard fetch to avoid circular deps if needed.
    const { backendUrl } = require("@/api/apiFetch");
    fetch(backendUrl + API_ENDPOINTS.AUTH.LOGOUT, {
      method: "POST",
      credentials: "omit" // or "include" depending on backend implementation
    })
    .catch(e => console.error("Logout request failed", e));

    // 4. Clear mode and query cache
    useModeStore.getState().resetMode?.();
    const { queryClient } = require("@/queryClient");
    queryClient.clear();
  },

  initialize: async () => {
    try {
      const isAuthFlag = await getItem("isAuthenticated");
      const localToken = await getItem("token"); // Only present on mobile
      
      const role = (await getItem("role")) as Role;
      const authProvider = (await getItem("authProvider")) as "LOCAL" | "STARDBI" | null;
      const isSuperAdmin = (await getItem("isSuperAdmin")) === "true";

      const token = Platform.OS === "web" ? "web-cookie-placeholder" : localToken;
      const isAuthenticated = Platform.OS === "web" ? isAuthFlag === "true" : !!token;

      if (isAuthenticated) {
        let isExpired = false;
        
        if (Platform.OS !== "web" && token) {
          try {
            const decoded = jwtDecode<{ exp?: number }>(token);
            if (decoded.exp && Date.now() >= decoded.exp * 1000) {
              isExpired = true;
            }
          } catch (e) {
            console.error("Invalid token on boot:", e);
            isExpired = true;
          }
        }

        if (isExpired) {
          console.log("[authStore] Token expired on boot. Clearing state.");
          set({ sessionExpiredMessage: true });
          setTimeout(async () => {
            await clearTokens();
            await removeItem("role");
            await removeItem("authProvider");
            await removeItem("isSuperAdmin");
            set({ token: null, role: null, authProvider: null, isSuperAdmin: false, isBanned: false, sessionExpiredMessage: false });
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
