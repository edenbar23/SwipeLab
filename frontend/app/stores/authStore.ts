// stores token & role
import { create } from "zustand";
import { useModeStore } from "./modeStore";

type Role = "USER" | "ADMIN" | null;

interface AuthState {
  token: string | null;
  role: Role;
  setAuth: (token: string, role: Role) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  role: null,
setAuth: (token, role) => {
    set({ token, role });

    // Automatically set admin mode if role is ADMIN
    if (role === "ADMIN") {
      useModeStore.getState().setMode("ADMIN");
    }
  },

  logout: () => {
    set({ token: null, role: null });

    // Clear mode on logout
    useModeStore.getState().resetMode?.();
  },
}));
