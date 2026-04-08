// stores token, role, and user info
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { create } from "zustand";
import { apiFetch } from "../api/apiFetch";
import { useModeStore } from "./modeStore";
import { API_ENDPOINTS } from '../api/apiEndpoints';


type Role = "USER" | "ADMIN" | null;

interface AuthState {
  token: string | null;
  role: Role;
  authProvider: "LOCAL" | "STARDBI" | null;
  isLoading: boolean;
  setAuth: (token: string, role: Role, refreshToken?: string) => Promise<void>;
  setExternalAuth: (token: string, refreshToken: string, lifetime: number, username: string) => Promise<void>;
  logout: () => Promise<void>;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  role: null,
  authProvider: null,
  isLoading: true,

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

    // Automatically set admin mode if role is ADMIN
    if (role === "ADMIN") {
      useModeStore.getState().setMode("ADMIN");
    } else {
      useModeStore.getState().setMode("USER");
    }
  },

  setExternalAuth: async (token, refreshToken, lifetime, username) => {
    set({ token, role: "ADMIN", authProvider: "STARDBI" });
    if (Platform.OS === 'web') {
      localStorage.setItem("token", token);
      localStorage.setItem("role", "ADMIN");
      localStorage.setItem("authProvider", "STARDBI");
      localStorage.setItem("refreshToken", refreshToken);
      localStorage.setItem("username", username);
      localStorage.setItem("lifetime", lifetime.toString());
    } else {
      await SecureStore.setItemAsync("token", token);
      await SecureStore.setItemAsync("role", "ADMIN");
      await SecureStore.setItemAsync("authProvider", "STARDBI");
      await SecureStore.setItemAsync("refreshToken", refreshToken);
      await SecureStore.setItemAsync("username", username);
      await SecureStore.setItemAsync("lifetime", lifetime.toString());
    }
    useModeStore.getState().setMode("ADMIN");
  },

  logout: async () => {
    set({ token: null, role: null, authProvider: null });
    if (Platform.OS === 'web') {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("authProvider");
    } else {
      await SecureStore.deleteItemAsync("token");
      await SecureStore.deleteItemAsync("role");
      await SecureStore.deleteItemAsync("refreshToken");
      await SecureStore.deleteItemAsync("authProvider");
    }
    apiFetch(API_ENDPOINTS.AUTH.LOGOUT, {
      method: "POST",
    });
    // Clear mode on logout
    useModeStore.getState().resetMode?.();
    const { queryClient } = require("../queryClient");
    queryClient.clear();
  },

  initialize: async () => {
    try {
      let token, role, authProvider;
      if (Platform.OS === 'web') {
        token = localStorage.getItem("token");
        role = localStorage.getItem("role") as Role;
        authProvider = localStorage.getItem("authProvider") as "LOCAL" | "STARDBI" | null;
      } else {
        token = await SecureStore.getItemAsync("token");
        role = (await SecureStore.getItemAsync("role")) as Role;
        authProvider = (await SecureStore.getItemAsync("authProvider")) as "LOCAL" | "STARDBI" | null;
      }

      if (token) {
        set({ token, role, authProvider });

        if (role === "ADMIN") {
          useModeStore.getState().setMode("ADMIN");
        } else {
          useModeStore.getState().setMode("USER");
        }
      }
    } catch (e) {
      console.error("Failed to initialize auth", e);
    } finally {
      set({ isLoading: false });
    }
  },
}));
