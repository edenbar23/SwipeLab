export const API_ENDPOINTS = {
    AUTH: {
        LOGIN: '/api/v1/auth/login',
        REGISTER: '/api/v1/auth/register',
        LOGOUT: '/api/v1/auth/logout',
        CHANGE_PASSWORD: '/api/v1/auth/password/change',
    },
    USERS: {
        ME: '/api/v1/users/me',
        GET_ALL: '/api/v1/users/get-all',
        INVITE: '/api/v1/manager/users/invite',
    },
    TASKS: {
        MY_TASKS: '/api/v1/tasks/my-tasks',
        AVAILABLE_TASKS: '/api/v1/tasks/available-tasks',
        DASHBOARD: '/api/v1/tasks/dashboard',
        DASHBOARD_DETAILS: (taskId: string | number) => `/api/v1/tasks/dashboard/${taskId}`,
        DASHBOARD_TASK: (taskId: string | number) => `/api/v1/tasks/dashboard/${taskId}`,
        PLAY_TASK: (taskId: string | number) => `/api/v1/classifications/tasks/${taskId}/play`,
        UPDATE_TASK: (taskId: string | number) => `/api/v1/tasks/${taskId}`,
    },
    CLASSIFICATIONS: {
        NEXT_BATCH: (taskId: string | number, count: number = 5) =>
            `/api/v1/classifications/next-batch?taskId=${taskId}&count=${count}`,
        SUBMIT: '/api/v1/classifications/submit',
    },
    STATISTICS: {
        ME: '/api/v1/statistics/me',
        VS_EXPERTS: '/api/v1/statistics/me/vs-experts',
        VS_USERS: '/api/v1/statistics/me/vs-users',
        BREAKDOWN: '/api/v1/statistics/me/breakdown',
        TIMESERIES: '/api/v1/statistics/me/timeseries',
        // UPDATE_ACCURACY: '/api/v1/statistics/update-accuracy',
    },
    COLLECTION: {
        BASE: '/api/v1/collection',
        STATS: '/api/v1/collection/stats',
        ADD: '/api/v1/collection/add',
    },
    GAMIFICATION: {
        LEADERBOARD: '/api/v1/gamification/leaderboard',
        CHALLENGES: '/api/v1/challenges',
    },
    ADMIN: {
        //ANALYTICS
        ANALYTICS_TASKS: (taskId: string | number) => `/api/v1/analytics/tasks/${taskId}`,
        ANALYTICS_EXPORTS: '/api/v1/analytics/exports',
        ANALYTICS_USERS: (taskId: string | number) => `/api/v1/analytics/users?taskId=${taskId}`,
        ANALYTICS_TOP: (limit: number = 5) => `/api/v1/analytics/top-performers?limit=${limit}`,
        //GOLD IMAGES
        GOLD_IMAGES: '/api/admin/gold-images',
        GOLD_IMAGES_GET_ALL: '/api/admin/gold-images/get-all',
        GOLD_IMAGE_DETAILS: (goldImageId: string | number) => `/api/admin/gold-images/${goldImageId}`,
        //RECIPIENTS
        RECIPIENTS: '/api/v1/dashboard/recipients',
        RECIPIENTS_CREATE: '/api/v1/dashboard/recipients',
        RECIPIENTS_UPDATE: (groupId: string | number) => `/api/v1/dashboard/recipients/${groupId}/update`,
    },
};
