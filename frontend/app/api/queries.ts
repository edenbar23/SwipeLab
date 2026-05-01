import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './apiFetch';
import { API_ENDPOINTS } from './apiEndpoints';

export const QUERY_KEYS = {
  // Tasks
  myTasks: ['tasks', 'my'],
  availableTasks: ['tasks', 'available'],
  dashboardTasks: ['tasks', 'dashboard'],
  taskDetails: (id: string | number) => ['tasks', id],
  experiments: ['tasks', 'experiments'],
  
  // User Profile
  userProfile: ['user', 'profile'],
  allUsers: ['user', 'all'],
  
  // Analytics & Admin
  analyticsTasks: (id: string | number) => ['analytics', 'tasks', id],
  analyticsUsers: (id: string | number) => ['analytics', 'users', id],
  analyticsTop: ['analytics', 'top'],
  
  // Metadata & Misc
  metadata: ['metadata', 'species'],
  statistics: ['statistics', 'me'],
  leaderboard: ['gamification', 'leaderboard'],
  challenges: ['gamification', 'challenges'],
  myBadges: ['gamification', 'my_badges'],
  collection: ['collection', 'base'],
  
  // Swipe State
  swipeBatch: (taskId: string | number) => ['classifications', 'batch', taskId],
};

const fetchJson = async (endpoint: string) => {
  const res = await apiFetch(endpoint);
  if (!res.ok) throw new Error(`Failed to fetch ${endpoint}`);
  return res.json();
};

export const useProfile = () => {
  return useQuery({
    queryKey: QUERY_KEYS.userProfile,
    queryFn: () => fetchJson(API_ENDPOINTS.USERS.ME),
    staleTime: 5 * 60 * 1000,
  });
};

export const useMyTasks = () => {
  return useQuery({
    queryKey: QUERY_KEYS.myTasks,
    queryFn: async () => {
      const data = await fetchJson(API_ENDPOINTS.TASKS.MY_TASKS);
      return data.tasks || [];
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useAvailableTasks = () => {
  return useQuery({
    queryKey: QUERY_KEYS.availableTasks,
    queryFn: async () => {
      const data = await fetchJson(API_ENDPOINTS.TASKS.AVAILABLE_TASKS).catch(() => ({ tasks: [] }));
      return data.tasks || [];
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useStatistics = () => {
  return useQuery({
    queryKey: QUERY_KEYS.statistics,
    queryFn: () => fetchJson(API_ENDPOINTS.STATISTICS.ME).catch(() => ({})),
    staleTime: 5 * 60 * 1000,
  });
};

export const useAllStatistics = () => {
  return useQuery({
    queryKey: ['statistics', 'all'],
    queryFn: async () => {
        const [summary, vsExperts, vsUsers, breakdown] = await Promise.all([
            fetchJson(API_ENDPOINTS.STATISTICS.ME),
            fetchJson(API_ENDPOINTS.STATISTICS.VS_EXPERTS),
            fetchJson(API_ENDPOINTS.STATISTICS.VS_USERS),
            fetchJson(API_ENDPOINTS.STATISTICS.BREAKDOWN),
        ]);
        return { summary, vsExperts, vsUsers, breakdown };
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useTaskDetails = (taskId: string | number) => {
  return useQuery({
    queryKey: QUERY_KEYS.taskDetails(taskId),
    queryFn: () => fetchJson(API_ENDPOINTS.TASKS.DASHBOARD_TASK(taskId)),
    staleTime: 5 * 60 * 1000,
  });
};

export const useAnalyticsTask = (taskId: string | number) => {
  return useQuery({
    queryKey: QUERY_KEYS.analyticsTasks(taskId),
    queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.ANALYTICS_TASKS(taskId)),
    staleTime: 2 * 60 * 1000,
  });
};

export const useExperiments = () => {
  return useQuery({
    queryKey: QUERY_KEYS.experiments,
    queryFn: () => fetchJson(API_ENDPOINTS.TASKS.EXPERIMENTS),
    staleTime: 5 * 60 * 1000,
  });
};

export const useAnalyticsUsers = (taskId: string | number) => {
  return useQuery({
    queryKey: QUERY_KEYS.analyticsUsers(taskId),
    queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.ANALYTICS_USERS(taskId)),
    staleTime: 2 * 60 * 1000,
  });
};

export const useAnalyticsTop = (limit: number = 5) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.analyticsTop, limit],
    queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.ANALYTICS_TOP(limit)),
    staleTime: 2 * 60 * 1000,
  });
};

export const useSpeciesMetadata = () => {
  return useQuery({
    queryKey: QUERY_KEYS.metadata,
    // Provide a mocked or empty response since it's not in API_ENDPOINTS right now
    queryFn: () => fetchJson('/api/v1/metadata/species').catch(() => []),
    staleTime: 24 * 60 * 60 * 1000, // 24 hours
  });
};

export const useLeaderboard = () => {
  return useQuery({
    queryKey: QUERY_KEYS.leaderboard,
    queryFn: () => fetchJson(API_ENDPOINTS.GAMIFICATION.LEADERBOARD),
    staleTime: 5 * 60 * 1000,
  });
};

export const useChallenges = () => {
  return useQuery({
    queryKey: QUERY_KEYS.challenges,
    queryFn: () => fetchJson(API_ENDPOINTS.GAMIFICATION.CHALLENGES),
    staleTime: 5 * 60 * 1000,
  });
};

export const useMyBadges = () => {
  return useQuery({
    queryKey: QUERY_KEYS.myBadges,
    queryFn: () => fetchJson(API_ENDPOINTS.GAMIFICATION.MY_BADGES).catch(() => []),
    staleTime: 5 * 60 * 1000,
  });
};

export const useAdminTasks = () => {
  return useQuery({
    queryKey: QUERY_KEYS.dashboardTasks,
    queryFn: () => fetchJson(API_ENDPOINTS.TASKS.DASHBOARD),
    staleTime: 2 * 60 * 1000,
    refetchInterval: (query: any) => {
      const data = query?.state?.data;
      if (Array.isArray(data) && data.some((t: any) => t.status === 'PROCESSING')) {
        return 3000;
      }
      return false;
    },
  });
};

export const useAdminUsers = () => {
  return useQuery({
    queryKey: QUERY_KEYS.allUsers,
    queryFn: () => fetchJson(API_ENDPOINTS.USERS.GET_ALL),
    staleTime: 2 * 60 * 1000,
  });
};

export const useRecipients = () => {
  return useQuery({
    queryKey: ['admin', 'recipients'],
    queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.RECIPIENTS),
    staleTime: 5 * 60 * 1000,
  });
};

export const useGoldImages = () => {
  return useQuery({
    queryKey: ['admin', 'goldImages'],
    queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.GOLD_IMAGES_GET_ALL),
    staleTime: 5 * 60 * 1000,
  });
};

export const useSwipeBatch = (taskId: string | number) => {
  return useQuery({
    queryKey: QUERY_KEYS.swipeBatch(taskId),
    queryFn: async () => {
      const res = await apiFetch(API_ENDPOINTS.TASKS.PLAY_TASK(taskId), { method: 'POST' });
      if (!res.ok) throw new Error("Failed to load batch");
      return res.json();
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useUpdateTaskStatus = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ taskId, action }: { taskId: string | number, action: 'pause' | 'archive' | 'activate' }) => {
      const endpoint = action === 'pause' ? API_ENDPOINTS.TASKS.PAUSE_TASK(taskId) :
                       action === 'archive' ? API_ENDPOINTS.TASKS.ARCHIVE_TASK(taskId) :
                       API_ENDPOINTS.TASKS.ACTIVATE_TASK(taskId);
      const res = await apiFetch(endpoint, { method: 'POST' });
      if (!res.ok) throw new Error(`Failed to ${action} task`);
      return res.json();
    },
    onSuccess: (updatedTask, { taskId }) => {
      queryClient.setQueryData(QUERY_KEYS.taskDetails(taskId), updatedTask);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.dashboardTasks });
    }
  });
};

import { queryClient } from '../queryClient';

export const preloadAfterLogin = async (role: string) => {
  try {
    // Step 1: Blocking preloads
    await Promise.all([
      queryClient.prefetchQuery({
        queryKey: QUERY_KEYS.userProfile,
        queryFn: () => fetchJson(API_ENDPOINTS.USERS.ME)
      }),
      queryClient.prefetchQuery({
        queryKey: QUERY_KEYS.swipeBatch(1),
        queryFn: async () => {
          const res = await apiFetch(API_ENDPOINTS.TASKS.PLAY_TASK(1), { method: 'POST' });
          if (!res.ok) throw new Error("Failed to load batch");
          return res.json();
        }
      })
    ]);

    // Step 2: Non-blocking background preloads
    queryClient.prefetchQuery({
      queryKey: QUERY_KEYS.myTasks,
      queryFn: async () => {
        const data = await fetchJson(API_ENDPOINTS.TASKS.MY_TASKS);
        return data.tasks || [];
      }
    });
    
    queryClient.prefetchQuery({
      queryKey: QUERY_KEYS.metadata,
      queryFn: () => fetchJson('/api/v1/metadata/species').catch(() => [])
    });

    if (role === 'ADMIN') {
      const defaultTaskId = 1;
      queryClient.prefetchQuery({
        queryKey: QUERY_KEYS.analyticsTasks(defaultTaskId),
        queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.ANALYTICS_TASKS(defaultTaskId))
      });
      queryClient.prefetchQuery({
        queryKey: [...QUERY_KEYS.analyticsTop, 5],
        queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.ANALYTICS_TOP(5))
      });
    }
  } catch (error) {
    console.warn("Preloading encountered an issue:", error);
  }
};
