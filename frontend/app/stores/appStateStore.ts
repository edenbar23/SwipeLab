import { create } from "zustand";

interface AppState {
  isMaintenanceMode: boolean;
  setMaintenanceMode: (isMaintenance: boolean) => void;
}

export const useAppStateStore = create<AppState>((set) => ({
  isMaintenanceMode: false,
  setMaintenanceMode: (isMaintenanceMode) => set({ isMaintenanceMode }),
}));
