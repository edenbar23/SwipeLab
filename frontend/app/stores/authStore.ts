// stores token, role, and user info
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { create } from "zustand";
import { apiFetch } from "../api/apiFetch";
import { useModeStore } from "./modeStore";

type Role = "USER" | "ADMIN" | null;

interface AuthState {
  token: string | null;
  role: Role;
  isLoading: boolean;
  setAuth: (token: string, role: Role) => Promise<void>;
  logout: () => Promise<void>;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  role: null,
  isLoading: true,

  setAuth: async (token, role) => {
    set({ token, role });
    if (Platform.OS === 'web') {
      localStorage.setItem("token", token);
      if (role) localStorage.setItem("role", role);
    } else {
      await SecureStore.setItemAsync("token", token);
      if (role) await SecureStore.setItemAsync("role", role);
    }

    // Automatically set admin mode if role is ADMIN
    if (role === "ADMIN") {
      useModeStore.getState().setMode("ADMIN");
    } else {
      useModeStore.getState().setMode("USER");
    }
  },

  logout: async () => {
    set({ token: null, role: null });
    if (Platform.OS === 'web') {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
    } else {
      await SecureStore.deleteItemAsync("token");
      await SecureStore.deleteItemAsync("role");
    }
    apiFetch("/api/v1/auth/logout", {
      method: "POST",
    });
    // Clear mode on logout
    useModeStore.getState().resetMode?.();
  },

  initialize: async () => {
    try {
      let token, role;
      if (Platform.OS === 'web') {
        token = localStorage.getItem("token");
        role = localStorage.getItem("role") as Role;
      } else {
        token = await SecureStore.getItemAsync("token");
        role = (await SecureStore.getItemAsync("role")) as Role;
      }

      if (token) {
        set({ token, role });

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
