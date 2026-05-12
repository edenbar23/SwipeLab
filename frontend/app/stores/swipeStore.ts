import { create } from 'zustand';

interface SwipeStore {
  dataBatch: any[];
  currentIndex: number;
  // Tracks which task the user has chosen to classify; null = no active session
  activeTaskId: string | number | null;

  setBatch: (items: any[]) => void;
  nextCard: () => void;
  clearBatch: () => void;
  getCurrentImage: () => any | null;
  setActiveTaskId: (id: string | number | null) => void;
}

export const useSwipeStore = create<SwipeStore>((set, get) => ({
  dataBatch: [],
  currentIndex: 0,
  activeTaskId: null,

  setBatch: (items: any[]) => {
    set({ dataBatch: items, currentIndex: 0 });
  },

  nextCard: () => {
    set((state) => ({ currentIndex: state.currentIndex + 1 }));
  },

  clearBatch: () => {
    set({ dataBatch: [], currentIndex: 0 });
  },

  getCurrentImage: () => {
    const { dataBatch, currentIndex } = get();
    return dataBatch[currentIndex] || null;
  },

  setActiveTaskId: (id: string | number | null) => {
    // Also clear any stale batch so the new task starts fresh
    set({ activeTaskId: id, dataBatch: [], currentIndex: 0 });
  },
}));