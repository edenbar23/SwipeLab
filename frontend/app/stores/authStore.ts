// stores token & role
import { create } from "zustand";

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
  setAuth: (token, role) => set({ token, role }),
  logout: () => set({ token: null, role: null }),
}));
