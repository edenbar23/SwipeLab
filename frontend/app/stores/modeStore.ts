import { create } from "zustand";

type Mode = "USER" | "ADMIN" | null;

interface ModeState {
  mode: Mode;
  setMode: (mode: "ADMIN" | "USER") => void;
  resetMode: () => void;
}

export const useModeStore = create<ModeState>((set) => ({
  mode: null,
  setMode: (mode) => set({ mode }),
  resetMode: () => set({ mode: null }),
}));
