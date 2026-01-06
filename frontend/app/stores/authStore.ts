// stores token, role, and user info
import { create } from "zustand";
import { useModeStore } from "./modeStore";

type Role = "USER" | "ADMIN" | null;

interface AuthState {
  token: string | null;
  role: Role;
  username: string | null;
  email: string | null;
  setAuth: (token: string, role: Role, username?: string, email?: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  role: null,
  username: null,
  email: null,

  setAuth: (token, role, username = "User", email = "") => {
    set({ token, role, username, email });

    // Automatically set admin mode if role is ADMIN
    if (role === "ADMIN") {
      useModeStore.getState().setMode("ADMIN");
    } else {
      useModeStore.getState().setMode("USER");
    }
  },

  logout: () => {
    set({ token: null, role: null, username: null, email: null });

    // Clear mode on logout
    useModeStore.getState().resetMode?.();
  },
}));
