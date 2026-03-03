import { create } from 'zustand';
import { apiFetch } from '../api/apiFetch';
import { CollectionItem, CollectionStats, SwipeDirection } from '../types';
import { API_ENDPOINTS } from '../api/apiEndpoints';


interface CollectionStore {
    items: CollectionItem[];
    stats: CollectionStats;
    isLoading: boolean;
    filter: SwipeDirection | 'all';

    fetchCollection: () => Promise<void>;
    addItem: (
        imageUrl: string,
        label: SwipeDirection,
        taskId: number,
        taskName: string,
        question: string
    ) => Promise<void>;
    setFilter: (filter: SwipeDirection | 'all') => void;
    getFilteredItems: () => CollectionItem[];
    reset: () => void;
}

const defaultStats: CollectionStats = {
    total: 0,
    yes: 0,
    no: 0,
    dontKnow: 0,
    trash: 0,
};

export const useCollectionStore = create<CollectionStore>((set, get) => ({
    items: [],
    stats: defaultStats,
    isLoading: false,
    filter: 'all',

    fetchCollection: async () => {
        set({ isLoading: true });
        try {
            const [itemsRes, statsRes] = await Promise.all([
                apiFetch(API_ENDPOINTS.COLLECTION.BASE),
                apiFetch(API_ENDPOINTS.COLLECTION.STATS),
            ]);

            if (itemsRes.ok && statsRes.ok) {
                const items = await itemsRes.json();
                const stats = await statsRes.json();
                set({ items, stats });
            }
        } catch (error) {
            console.error('Failed to fetch collection:', error);
        } finally {
            set({ isLoading: false });
        }
    },

    addItem: async (imageUrl, label, taskId, taskName, question) => {
        try {
            const response = await apiFetch(API_ENDPOINTS.COLLECTION.ADD, {
                method: 'POST',
                body: JSON.stringify({ imageUrl, label, taskId, taskName, question }),
            });

            if (response.ok) {
                const newItem = await response.json();
                set((state) => {
                    const newStats = { ...state.stats };
                    newStats.total++;
                    switch (label) {
                        case 'yes':
                            newStats.yes++;
                            break;
                        case 'no':
                            newStats.no++;
                            break;
                        case 'dont-know':
                            newStats.dontKnow++;
                            break;
                        case 'trash':
                            newStats.trash++;
                            break;
                    }
                    return {
                        items: [newItem, ...state.items],
                        stats: newStats,
                    };
                });
            }
        } catch (error) {
            console.error('Failed to add to collection:', error);
        }
    },

    setFilter: (filter) => {
        set({ filter });
    },

    getFilteredItems: () => {
        const { items, filter } = get();
        if (filter === 'all') return items;
        return items.filter((item) => item.label === filter);
    },

    reset: () => {
        set({ items: [], stats: defaultStats, filter: 'all' });
    },
}));
