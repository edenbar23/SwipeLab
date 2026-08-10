import type {
  PlatformOverview,
  GlobalStats,
  UserPerformance,
} from '@/types/analyticsTypes';

// ─── Platform Overview mock ───────────────────────────────────────────────────

export const MOCK_PLATFORM_OVERVIEW: PlatformOverview = {
  today: {
    classifications: 42,
    uniqueImages: 18,
    uniqueUsers: 5,
    uniqueTasks: 2,
    uniqueExperiments: 1,
  },
  thisWeek: {
    classifications: 278,
    uniqueImages: 95,
    uniqueUsers: 12,
    uniqueTasks: 3,
    uniqueExperiments: 2,
  },
  thisMonth: {
    classifications: 1043,
    uniqueImages: 310,
    uniqueUsers: 20,
    uniqueTasks: 4,
    uniqueExperiments: 3,
  },
  confidenceTrend: Array.from({ length: 30 }, (_, i) => {
    const date = new Date();
    date.setDate(date.getDate() - (29 - i));
    return {
      date: date.toISOString().slice(0, 10),
      // Backend returns AVG(credibilityAtTime) which is stored on the 0–100 scale
      averageCredibility: 65 + Math.random() * 30,
      classificationCount: Math.floor(20 + Math.random() * 60),
    };
  }),
  labelDistribution: [
    { label: 'YES',       count: 620, percentage: 59.5 },
    { label: 'NO',        count: 280, percentage: 26.9 },
    { label: 'DONT_KNOW', count: 110, percentage: 10.6 },
    { label: 'TRASH',     count: 33,  percentage: 3.2  },
  ],
  totals: {
    totalUsers: 24,
    totalClassifications: 1043,
    totalImages: 310,
    activeTasks: 4,
  },
};

// ─── Global Stats mock ────────────────────────────────────────────────────────

export const MOCK_GLOBAL_STATS: GlobalStats = {
  totalUsers: 24,
  totalSwipes: 1043,
  activeTasks: 4,
  totalImages: 310,
};

// ─── User Performance mock ────────────────────────────────────────────────────

const MOCK_USER_PERFORMANCE: UserPerformance[] = [
  {
    username: 'user1',
    displayName: 'Alice Johnson',
    totalClassifications: 89,
    goldImageClassifications: 12,
    correctGoldClassifications: 11,
    goldAccuracy: 91.7,
    // Backend composite score — 0 (bad) to 100 (perfect)
    credibilityScore: 92,
    currentStreak: 5,
    points: 450,
  },
  {
    username: 'user2',
    displayName: 'Bob Smith',
    totalClassifications: 76,
    goldImageClassifications: 10,
    correctGoldClassifications: 9,
    goldAccuracy: 90.0,
    credibilityScore: 89,
    currentStreak: 3,
    points: 380,
  },
  {
    username: 'user3',
    displayName: 'Carol Davis',
    totalClassifications: 65,
    goldImageClassifications: 8,
    correctGoldClassifications: 7,
    goldAccuracy: 87.5,
    credibilityScore: 86,
    currentStreak: 2,
    points: 325,
  },
  {
    username: 'user4',
    displayName: 'David Lee',
    totalClassifications: 58,
    goldImageClassifications: 7,
    correctGoldClassifications: 6,
    goldAccuracy: 85.7,
    credibilityScore: 84,
    currentStreak: 1,
    points: 290,
  },
  {
    username: 'user5',
    displayName: 'Emma Wilson',
    totalClassifications: 52,
    goldImageClassifications: 6,
    correctGoldClassifications: 5,
    goldAccuracy: 83.3,
    credibilityScore: 81,
    currentStreak: 0,
    points: 260,
  },
];

// ─── Task analytics mock (kept for backward compat with mockRouter) ───────────

export function getTaskAnalytics(taskId: number) {
  return {
    taskId,
    taskName: `Task #${taskId}`,
    status: 'ACTIVE',
    progress: {
      totalImages: 150,
      imagesClassified: 98,
      completedImages: 68,
      percentComplete: 65.3,
    },
    consensus: {
      overallAverage: 78.5,
      lowConsensusImages: 12,
      threshold: 80.0,
    },
    participation: {
      activeUsers: 15,
      totalClassifications: 456,
      averageClassificationsPerUser: 30,
      medianResponseTimeMs: 1800,
    },
    quality: {
      averageCredibility: 0.87,
      expertAgreement: 0.91,
      lowQualityUsers: 2,
    },
  };
}

export function getUserPerformance(taskId?: number): UserPerformance[] {
  return MOCK_USER_PERFORMANCE;
}

export const analyticsMock = {
  platformOverview: MOCK_PLATFORM_OVERVIEW,
  globalStats: MOCK_GLOBAL_STATS,
  userPerformance: MOCK_USER_PERFORMANCE,
  getTaskAnalytics,
  getUserPerformance,
};
