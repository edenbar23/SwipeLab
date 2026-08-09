import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/api/apiFetch';
import { API_ENDPOINTS } from '@/api/apiEndpoints';

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
  
  // Analytics & researcher
  analyticsTasks: (id: string | number) => ['analytics', 'tasks', id],
  analyticsUsers: (id: string | number) => ['analytics', 'users', id],
  analyticsTop: ['analytics', 'top'],
  analyticsOverview: ['analytics', 'overview'],
  analyticsGlobalStats: ['analytics', 'global-stats'],
  
  // Metadata & Misc
  metadata: ['metadata', 'species'],
  statistics: ['statistics', 'me'],
  leaderboard: ['gamification', 'leaderboard'],
  challenges: ['gamification', 'challenges'],
  myBadges: ['gamification', 'my_badges'],
  rank: ['gamification', 'rank'],
  collection: ['collection', 'base'],
  
  // Swipe State
  swipeBatch: (taskId: string | number) => ['classifications', 'batch', taskId],

  // Superadmin: malicious-labeling config
  maliciousLabelingConfig: ['admin', 'malicious-labeling-config'],
  maliciousLabelingAuditLog: ['admin', 'malicious-labeling-config', 'audit-log'],
};

const fetchJson = async (endpoint: string) => {
  const res = await apiFetch(endpoint);
  if (!res.ok) throw new Error(`Failed to fetch ${endpoint}`);
  return res.json();
};

export const useProfile = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: QUERY_KEYS.userProfile,
    queryFn: () => fetchJson(API_ENDPOINTS.USERS.ME),
    staleTime: 5 * 60 * 1000,
    ...options,
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
        const [summary, vsExperts, vsUsers, breakdown, userInfo] = await Promise.all([
            fetchJson(API_ENDPOINTS.STATISTICS.ME),
            fetchJson(API_ENDPOINTS.STATISTICS.VS_EXPERTS),
            fetchJson(API_ENDPOINTS.STATISTICS.VS_USERS),
            fetchJson(API_ENDPOINTS.STATISTICS.BREAKDOWN),
            fetchJson(API_ENDPOINTS.GAMIFICATION.USER_INFO).catch(() => ({ score: 0, badge: null, currentStreak: 0 })),
        ]);
        return { summary, vsExperts, vsUsers, breakdown, userInfo };
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
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_TASKS(taskId)),
    staleTime: 2 * 60 * 1000,
    enabled: taskId !== 0 && taskId !== '',
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
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_USERS(taskId)),
    staleTime: 2 * 60 * 1000,
  });
};

export const useAnalyticsTop = (limit: number = 5) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.analyticsTop, limit],
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_TOP(limit)),
    staleTime: 2 * 60 * 1000,
  });
};

export const useAnalyticsOverview = () => {
  return useQuery({
    queryKey: QUERY_KEYS.analyticsOverview,
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_OVERVIEW),
    staleTime: 2 * 60 * 1000,
  });
};

export const useAnalyticsGlobalStats = () => {
  return useQuery({
    queryKey: QUERY_KEYS.analyticsGlobalStats,
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_GLOBAL_STATS),
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

/** Returns the authenticated user's current rank tier, YES tag count, and progress. */
export const useRank = () => {
  return useQuery({
    queryKey: QUERY_KEYS.rank,
    queryFn: () => fetchJson(API_ENDPOINTS.GAMIFICATION.RANK),
    staleTime: 2 * 60 * 1000,
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

/** Fetches the current malicious-labeling + fraud-detection config (superadmin only). */
export const useMaliciousLabelingConfig = () => {
  return useQuery({
    queryKey: QUERY_KEYS.maliciousLabelingConfig,
    queryFn: () => fetchJson(API_ENDPOINTS.ADMIN.MALICIOUS_LABELING_CONFIG),
    staleTime: 60 * 1000,
  });
};

/** Mutation: partial or full update of malicious-labeling config. Invalidates config cache on success. */
export const useUpdateMaliciousLabelingConfig = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Record<string, unknown>) => {
      const res = await apiFetch(API_ENDPOINTS.ADMIN.MALICIOUS_LABELING_CONFIG, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message ?? 'Failed to update config');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.maliciousLabelingConfig });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.maliciousLabelingAuditLog });
    },
  });
};

/** Fetches paginated audit log for malicious-labeling config changes (superadmin only). */
export const useMaliciousLabelingAuditLog = (page = 0, size = 20) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.maliciousLabelingAuditLog, page, size],
    queryFn: () =>
      fetchJson(`${API_ENDPOINTS.ADMIN.MALICIOUS_LABELING_AUDIT}?page=${page}&size=${size}`),
    staleTime: 30 * 1000,
  });
};

export const useRecipients = () => {
  return useQuery({
    queryKey: ['researcher', 'recipients'],
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.RECIPIENTS),
    staleTime: 5 * 60 * 1000,
  });
};

export const useGoldImages = () => {
  return useQuery({
    queryKey: ['researcher', 'goldImages'],
    queryFn: () => fetchJson(API_ENDPOINTS.researcher.GOLD_IMAGES_GET_ALL),
    staleTime: 5 * 60 * 1000,
  });
};

export const useSwipeBatch = (
  taskId: string | number,
  options?: { enabled?: boolean }
) => {
  return useQuery({
    queryKey: QUERY_KEYS.swipeBatch(taskId),
    queryFn: async () => {
      const res = await apiFetch(API_ENDPOINTS.TASKS.PLAY_TASK(taskId), { method: 'POST' });
      if (!res.ok) throw new Error("Failed to load batch");
      return res.json();
    },
    staleTime: 5 * 60 * 1000,
    // Caller can opt out (e.g. when no task is selected yet)
    enabled: options?.enabled !== false,
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

export const useAssignTask = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (taskId: string | number) => {
      const res = await apiFetch(API_ENDPOINTS.TASKS.ASSIGN_TASK(taskId), { method: 'POST' });
      if (!res.ok) {
        // 409 Conflict means already assigned — surface a meaningful error
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message ?? `Failed to assign task ${taskId}`);
      }
      return res.json();
    },
    onSuccess: () => {
      // Force an immediate refetch (not just stale-invalidation) so the task
      // moves from Explore → Assigned without waiting for the background cycle.
      queryClient.refetchQueries({ queryKey: QUERY_KEYS.myTasks, type: 'active' });
      queryClient.refetchQueries({ queryKey: QUERY_KEYS.availableTasks, type: 'active' });
      // Also refresh stat chips (Assigned / Classified counters in the header)
      queryClient.refetchQueries({ queryKey: QUERY_KEYS.statistics, type: 'active' });
    },
  });
};

/**
 * Mutation hook for multi-task CSV export (Issue #257).
 * Returns a Blob containing the CSV file streamed from the backend.
 */
export const useExportClassificationsCsv = () => {
  return useMutation({
    mutationFn: async (taskIds: number[]) => {
      const res = await apiFetch(API_ENDPOINTS.researcher.EXPORT_CSV, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ taskIds }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message ?? 'Export failed');
      }
      return res.blob();
    },
  });
};

/**
 * Batch-fetches pool images for multiple species at once.
 * Enabled only when at least one labelId is provided.
 * Returns a map of labelId → list of pool image DTOs.
 */
export const useSpeciesPoolImages = (labelIds: (string | number)[]) => {
  return useQuery({
    queryKey: ['species', 'pool', ...labelIds.map(String).sort()],
    queryFn: () => fetchJson(API_ENDPOINTS.SPECIES.REF_IMAGES_BATCH(labelIds)),
    staleTime: 5 * 60 * 1000,
    enabled: labelIds.length > 0,
  });
};

/** Deletes a reference image from the species pool. */
export const useDeleteSpeciesRefImage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (imageId: number) => {
      const res = await apiFetch(API_ENDPOINTS.SPECIES.REF_IMAGE_DELETE(imageId), { method: 'DELETE' });
      if (!res.ok) throw new Error('Failed to delete reference image');
    },
    onSuccess: () => {
      // Invalidate all species pool queries so the UI refreshes
      queryClient.invalidateQueries({ queryKey: ['species', 'pool'] });
    },
  });
};

import { queryClient } from '@/queryClient';


export const preloadAfterLogin = async (role: string) => {
  try {
    // Step 1: Blocking preloads
    await Promise.all([
      queryClient.prefetchQuery({
        queryKey: QUERY_KEYS.userProfile,
        queryFn: () => fetchJson(API_ENDPOINTS.USERS.ME)
      }),
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

    if (role === 'researcher') {
      const defaultTaskId = 1;
      queryClient.prefetchQuery({
        queryKey: QUERY_KEYS.analyticsTasks(defaultTaskId),
        queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_TASKS(defaultTaskId))
      });
      queryClient.prefetchQuery({
        queryKey: [...QUERY_KEYS.analyticsTop, 5],
        queryFn: () => fetchJson(API_ENDPOINTS.researcher.ANALYTICS_TOP(5))
      });
    }
  } catch (error) {
    console.warn("Preloading encountered an issue:", error);
  }
};
