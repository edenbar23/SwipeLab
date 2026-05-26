export const API_ENDPOINTS = {
    AUTH: {
        LOGIN: '/api/v1/auth/login',
        REGISTER: '/api/v1/auth/register',
        LOGOUT: '/api/v1/auth/logout',
        REFRESH: '/api/v1/auth/refresh',
        CHANGE_PASSWORD: '/api/v1/auth/password/change',
    },
    USERS: {
        ME: '/api/v1/users/me',
        GET_ALL: '/api/v1/users/get-all',
        INVITE: '/api/v1/manager/users/invite',
        BAN: (username: string) => `/api/v1/users/ban/${username}`,
        UNBAN: (username: string) => `/api/v1/users/unban/${username}`,
    },
    TASKS: {
        MY_TASKS: '/api/v1/tasks/my-tasks',
        AVAILABLE_TASKS: '/api/v1/tasks/available-tasks',
        DASHBOARD: '/api/v1/tasks/dashboard',
        DASHBOARD_DETAILS: (taskId: string | number) => `/api/v1/tasks/dashboard/${taskId}`,
        DASHBOARD_TASK: (taskId: string | number) => `/api/v1/tasks/dashboard/${taskId}`,
        EXPERIMENTS: '/api/v1/tasks/dashboard/experiments',
        PLAY_TASK: (taskId: string | number) => `/api/v1/classifications/tasks/${taskId}/play`,
        CREATE_TASK: '/api/v1/tasks/create',
        UPDATE_TASK: (taskId: string | number) => `/api/v1/tasks/${taskId}`,
        PAUSE_TASK: (taskId: string | number) => `/api/v1/tasks/${taskId}/pause`,
        ARCHIVE_TASK: (taskId: string | number) => `/api/v1/tasks/${taskId}/archive`,
        ACTIVATE_TASK: (taskId: string | number) => `/api/v1/tasks/${taskId}/activate`,
        ASSIGN_TASK: (taskId: string | number) => `/api/v1/tasks/${taskId}/assign`,
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
        // ADD removed — collection entries are created server-side on YES classification
    },
    GAMIFICATION: {
        LEADERBOARD: '/api/v1/gamification/leaderboard',
        CHALLENGES: '/api/v1/gamification/challenges',
        MY_BADGES: '/api/v1/gamification/me/badges',
        RANK: '/api/v1/gamification/rank',
    },
    researcher: {
        //ANALYTICS
        ANALYTICS_OVERVIEW: '/api/v1/analytics/overview',
        ANALYTICS_GLOBAL_STATS: '/api/v1/analytics/global-stats',
        ANALYTICS_TASKS: (taskId: string | number) => `/api/v1/analytics/tasks/${taskId}`,
        ANALYTICS_EXPORTS: '/api/v1/analytics/exports',
        ANALYTICS_USERS: (taskId: string | number) => `/api/v1/analytics/users?taskId=${taskId}`,
        ANALYTICS_TOP: (limit: number = 5) => `/api/v1/analytics/top-performers?limit=${limit}`,
        //GOLD IMAGES
        GOLD_IMAGES: '/api/admin/gold-images',
        GOLD_IMAGES_GET_ALL: '/api/admin/gold-images/get-all',
        GOLD_IMAGES_UPLOAD: '/api/admin/gold-images/upload',
        GOLD_IMAGE_DETAILS: (goldImageId: string | number) => `/api/admin/gold-images/${goldImageId}`,
        //RECIPIENTS
        RECIPIENTS: '/api/v1/dashboard/recipients',
        RECIPIENTS_CREATE: '/api/v1/dashboard/recipients',
        RECIPIENTS_UPDATE: (groupId: string | number) => `/api/v1/dashboard/recipients/${groupId}`,
    },
    STARDBI: {
        LOGIN: 'https://stardbi.cs.bgu.ac.il/auth/get_token/',
        REFRESH: 'https://stardbi.cs.bgu.ac.il/auth/token_refresh/',
    },
    ADMIN: {
        NOTIFICATIONS: '/api/admin/notifications',
        NOTIFICATIONS_UNREAD_COUNT: '/api/admin/notifications/unread-count',
        NOTIFICATION_READ: (id: number) => `/api/admin/notifications/${id}/read`,
        NOTIFICATIONS_READ_ALL: '/api/admin/notifications/read-all',
        SUSPICIOUS_ACTIVITY: '/api/admin/suspicious-activity',
        SUSPICIOUS_ACTIVITY_USER: (username: string) => `/api/admin/suspicious-activity/${username}`,
        SUSPICIOUS_ACTIVITY_RESET: (username: string) => `/api/admin/suspicious-activity/${username}/reset`,
    },
};
