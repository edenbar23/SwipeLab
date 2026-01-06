export interface Challenge {
    id: number;
    title: string;
    description: string;
    type: 'DAILY' | 'WEEKLY' | 'STREAK' | 'MILESTONE';
    progress: number;
    total: number;
    reward: string;
    bgColor: string;
    icon: string;
}

export const challengesMock: Challenge[] = [
    {
        id: 1,
        title: 'Classify 20 images today',
        description: 'Complete your daily streak!',
        type: 'DAILY',
        progress: 10,
        total: 20,
        reward: 'Silver Badge',
        bgColor: '#CCE5FF', // Light Blue
        icon: '🥈'
    },
    {
        id: 2,
        title: 'Classify 100 images this week',
        description: 'Complete your daily streak!',
        type: 'WEEKLY',
        progress: 38,
        total: 100,
        reward: 'Purple Badge',
        bgColor: '#FOC1E3', // Pinkish
        icon: '👾'
    },
    {
        id: 3,
        title: 'Complete 5 tasks in a row',
        description: 'Complete your daily streak!',
        type: 'STREAK',
        progress: 1,
        total: 5,
        reward: 'Red Badge',
        bgColor: '#FFDAC1', // Light Orange
        icon: '🔥'
    },
    {
        id: 4,
        title: 'Reach 500 total classifications',
        description: 'Become a "LabSwiper Legend"',
        type: 'MILESTONE',
        progress: 500, 
        total: 500,
        reward: 'Gold Medal',
        bgColor: '#C1E1C1', // Light Green
        icon: '🏆'
    }
];

// Adjusting colors to match the image better based on hex estimates
// Blue: #B3E5FC
// Pink: #F8BBD0
// Orange: #FFCCBC
// Green: #C8E6C9

export const refinedChallengesMock: Challenge[] = [
    {
        id: 1,
        title: 'Classify 20 images today',
        description: 'Complete your daily streak!',
        type: 'DAILY',
        progress: 10,
        total: 20,
        reward: 'Silver Star',
        bgColor: '#ADD8E6', // LightBlue
        icon: '⭐'
    },
    {
        id: 2,
        title: 'Classify 100 images this week',
        description: 'Complete your daily streak!',
        type: 'WEEKLY',
        progress: 38,
        total: 100,
        reward: 'Purple Shield',
        bgColor: '#E6E6FA', // Lavender/Pinkish
        icon: '🛡️'
    },
    {
        id: 3,
        title: 'Complete 5 tasks in a row',
        description: 'Complete your daily streak!',
        type: 'STREAK',
        progress: 1,
        total: 5,
        reward: 'Red Shield',
        bgColor: '#FFE4B5', // Moccasin/Light Orange
        icon: '🛡️'
    },
    {
        id: 4,
        title: 'Reach 500 total classifications',
        description: 'Become a "LabSwiper Legend"',
        type: 'MILESTONE',
        progress: 500,
        total: 500,
        reward: 'Master Badge',
        bgColor: '#90EE90', // LightGreen
        icon: '🔰'
    }
];
