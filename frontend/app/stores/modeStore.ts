import { create } from "zustand";

type Mode = "USER" | "researcher" | null;

interface ModeState {
  mode: Mode;
  setMode: (mode: "researcher" | "USER") => void;
  resetMode: () => void;
}

export const useModeStore = create<ModeState>((set) => ({
  mode: null,
  setMode: (mode) => set({ mode }),
  resetMode: () => set({ mode: null }),
}));
