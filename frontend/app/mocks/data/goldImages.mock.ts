export type GoldImageResponse = {
    id: number;
    imageId: number;
    species: string;
    correctAnswer: string;
    imageUrl?: string; // Optional for rendering in the card right now
};

export const MOCK_GOLD_IMAGES: GoldImageResponse[] = [
    {
        id: 1,
        imageId: 101,
        species: "BEE",
        correctAnswer: "YES",
        imageUrl: "https://via.placeholder.com/300/FFB6C1/000000?text=Bee",
    },
    {
        id: 2,
        imageId: 102,
        species: "WASP",
        correctAnswer: "NO",
        imageUrl: "https://via.placeholder.com/300/98FB98/000000?text=Wasp",
    },
    {
        id: 3,
        imageId: 103,
        species: "BUTTERFLY",
        correctAnswer: "YES",
        imageUrl: "https://via.placeholder.com/300/87CEEB/000000?text=Butterfly",
    },
    {
        id: 4,
        imageId: 104,
        species: "BEETLE",
        correctAnswer: "NO",
        imageUrl: "https://via.placeholder.com/300/DDA0DD/000000?text=Beetle",
    },
    {
        id: 5,
        imageId: 105,
        species: "ANT",
        correctAnswer: "YES",
        imageUrl: "https://via.placeholder.com/300/F0E68C/000000?text=Ant",
    },
];
