import { CollectionItem, CollectionStats, SwipeDirection } from '@/types';

// Sample images for collection mock
const sampleImages = [
    'https://images.unsplash.com/photo-1558642452-9d2a7deb7f62?w=300',
    'https://images.unsplash.com/photo-1560807707-8cc77767d783?w=300',
    'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=300',
    'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=300',
    'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=300',
    'https://images.unsplash.com/photo-1425082661705-1834bfd09dca?w=300',
    'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=300',
    'https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=300',
];

// Create date strings for past dates
function getDateString(daysAgo: number): string {
    const date = new Date();
    date.setDate(date.getDate() - daysAgo);
    return date.toISOString();
}

// Mutable collection data
let collectionItems: CollectionItem[] = [
    {
        id: 'col-1',
        imageUrl: sampleImages[0],
        label: 'yes',
        taskId: 7,
        taskName: 'Asian Giant Hornet Identification',
        taskStatus: 'COMPLETED',
        speciesName: 'Asian Giant Hornet',
        scientificName: 'Vespa mandarinia',
        description: 'The world\'s largest hornet, native to temperate and tropical East Asia.',
        question: 'Is this a Vespa mandarinia?',
        labeledAt: getDateString(0),
    },
    {
        id: 'col-2',
        imageUrl: sampleImages[1],
        label: 'no',
        taskId: 7,
        taskName: 'Asian Giant Hornet Identification',
        taskStatus: 'COMPLETED',
        speciesName: 'Asian Giant Hornet',
        scientificName: 'Vespa mandarinia',
        description: 'The world\'s largest hornet, native to temperate and tropical East Asia.',
        question: 'Is this a Vespa mandarinia?',
        labeledAt: getDateString(1),
    },
    {
        id: 'col-3',
        imageUrl: sampleImages[2],
        label: 'yes',
        taskId: 101,
        taskName: 'Red Fire Ant Control',
        taskStatus: 'IN_PROGRESS',
        speciesName: 'Red Imported Fire Ant',
        scientificName: 'Solenopsis invicta',
        description: 'One of over 280 species in the widespread genus Solenopsis.',
        question: 'Is this a Solenopsis invicta?',
        labeledAt: getDateString(2),
    },
    {
        id: 'col-4',
        imageUrl: sampleImages[3],
        label: 'dont-know',
        taskId: 7,
        taskName: 'Asian Giant Hornet Identification',
        taskStatus: 'COMPLETED',
        speciesName: 'Asian Giant Hornet',
        scientificName: 'Vespa mandarinia',
        description: 'The world\'s largest hornet, native to temperate and tropical East Asia.',
        question: 'Is this a Vespa mandarinia?',
        labeledAt: getDateString(3),
    },
    {
        id: 'col-5',
        imageUrl: sampleImages[4],
        label: 'trash',
        taskId: 102,
        taskName: 'Urban Butterfly Watch',
        taskStatus: 'IN_PROGRESS',
        speciesName: 'Eastern Tiger Swallowtail',
        scientificName: 'Papilio glaucus',
        description: 'A species of swallowtail butterfly native to eastern North America.',
        question: 'Is this a Papilio glaucus?',
        labeledAt: getDateString(4),
    },
    {
        id: 'col-6',
        imageUrl: sampleImages[5],
        label: 'yes',
        taskId: 7,
        taskName: 'Asian Giant Hornet Identification',
        taskStatus: 'COMPLETED',
        speciesName: 'Asian Giant Hornet',
        scientificName: 'Vespa mandarinia',
        description: 'The world\'s largest hornet, native to temperate and tropical East Asia.',
        question: 'Is this a Vespa mandarinia?',
        labeledAt: getDateString(5),
    },
    {
        id: 'col-7',
        imageUrl: sampleImages[6],
        label: 'no',
        taskId: 101,
        taskName: 'Red Fire Ant Control',
        taskStatus: 'IN_PROGRESS',
        speciesName: 'Red Imported Fire Ant',
        scientificName: 'Solenopsis invicta',
        description: 'One of over 280 species in the widespread genus Solenopsis.',
        question: 'Is this a Solenopsis invicta?',
        labeledAt: getDateString(6),
    },
    {
        id: 'col-8',
        imageUrl: sampleImages[7],
        label: 'yes',
        taskId: 102,
        taskName: 'Urban Butterfly Watch',
        taskStatus: 'IN_PROGRESS',
        speciesName: 'Monarch',
        scientificName: 'Danaus plexippus',
        description: 'A milkweed butterfly in the family Nymphalidae.',
        question: 'Is this a Danaus plexippus?',
        labeledAt: getDateString(7),
    },
];

export function getCollection(): CollectionItem[] {
    // Filter only items from COMPLETED tasks AND where label is 'yes' (bugs found)
    const completedItems = collectionItems.filter(
        item => item.taskStatus === 'COMPLETED' && item.label === 'yes'
    );

    return [...completedItems].sort(
        (a, b) => new Date(b.labeledAt).getTime() - new Date(a.labeledAt).getTime()
    );
}

export function getCollectionStats(): CollectionStats {
    // Stats should match the collection view (only bugs found in completed tasks)
    const completedItems = collectionItems.filter(
        item => item.taskStatus === 'COMPLETED' && item.label === 'yes'
    );

    const stats: CollectionStats = {
        total: completedItems.length,
        yes: 0,
        no: 0,
        dontKnow: 0,
        trash: 0,
    };

    completedItems.forEach((item) => {
        switch (item.label) {
            case 'yes':
                stats.yes++;
                break;
            case 'no':
                stats.no++;
                break;
            case 'dont-know':
                stats.dontKnow++;
                break;
            case 'trash':
                stats.trash++;
                break;
        }
    });

    return stats;
}

export function addToCollection(
    imageUrl: string,
    label: SwipeDirection,
    taskId: number,
    taskName: string,
    question: string
): CollectionItem {
    // Logic to determine if task is completed would go here in real backend.
    // For mock, we'll assume new items are from IN_PROGRESS tasks unless specified.
    const newItem: CollectionItem = {
        id: `col-${Date.now()}`,
        imageUrl,
        label,
        taskId,
        taskName,
        taskStatus: 'IN_PROGRESS', // Default to in_progress for new ones in mock
        speciesName: 'Unknown Species',
        scientificName: 'Unknown',
        description: 'Description waiting for completion.',
        question,
        labeledAt: new Date().toISOString(),
    };

    collectionItems.push(newItem);
    return newItem;
}

export function resetCollection(): void {
    // Reset to initial state for testing
    // (We're not re-initializing the array here to keep the const simple, but in real app we might)
}

export const collectionMock = {
    getCollection,
    getCollectionStats,
    addToCollection,
    resetCollection,
};
