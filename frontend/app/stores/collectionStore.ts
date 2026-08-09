import { create } from 'zustand';
import { apiFetch } from '@/api/apiFetch';
import { API_ENDPOINTS } from '@/api/apiEndpoints';

export interface CollectionEntry {
    id: number;
    imageId: number;
    species: string | null;
    imageUrl: string | null;
    taskId: number | null;
    taggedAt: string; // ISO timestamp
}

export interface CollectionStats {
    total: number;
}

interface CollectionStore {
    items: CollectionEntry[];
    stats: CollectionStats;
    isLoading: boolean;

    fetchCollection: () => Promise<void>;
    reset: () => void;
}

const defaultStats: CollectionStats = { total: 0 };

export const useCollectionStore = create<CollectionStore>((set) => ({
    items: [],
    stats: defaultStats,
    isLoading: false,

    fetchCollection: async () => {
        set({ isLoading: true });
        try {
            const [itemsRes, statsRes] = await Promise.all([
                apiFetch(API_ENDPOINTS.COLLECTION.BASE),
                apiFetch(API_ENDPOINTS.COLLECTION.STATS),
            ]);

            if (itemsRes.ok && statsRes.ok) {
                const items: CollectionEntry[] = await itemsRes.json();
                const stats: CollectionStats = await statsRes.json();
                set({ items, stats });
            }
        } catch (error) {
            console.error('Failed to fetch collection:', error);
        } finally {
            set({ isLoading: false });
        }
    },

    reset: () => {
        set({ items: [], stats: defaultStats });
    },
}));

