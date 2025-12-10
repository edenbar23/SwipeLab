import { create } from "zustand";

type Mode = "user" | "admin" | null;

interface ModeState {
  mode: Mode;
  setMode: (mode: Exclude<Mode, null>) => void;
  resetMode: () => void;
}

export const useModeStore = create<ModeState>((set) => ({
  mode: null,
  setMode: (mode) => set({ mode }),
  resetMode: () => set({ mode: null }),
}));
