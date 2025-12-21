export const statisticsMock = {

  /**
   * GET /api/v1/statistics/me
   */
  summary: {
    userId: 1,
    username: 'john_doe',
    totalClassifications: 1980,
    completedTasks: 12,
    accuracy: 0.91,
    averageTimePerImageSeconds: 4.7,
    currentStreakDays: 18,
    longestStreakDays: 42,
    score: 7420,
    rankGlobal: 2
  },

  /**
   * GET /api/v1/statistics/me/vs-experts
   */
  vsExperts: {
    userAccuracy: 0.91,
    expertAccuracy: 0.95,
    difference: -0.04,
    comparedSamples: 860
  },

  /**
   * GET /api/v1/statistics/me/vs-users
   */
  vsUsers: {
    userAccuracy: 0.91,
    averageUserAccuracy: 0.87,
    percentile: 88
  },

  /**
   * GET /api/v1/statistics/me/breakdown
   */
  breakdown: {
    byTask: [
      {
        taskId: 7,
        taskName: 'Asian Giant Hornet Identification',
        classifications: 320,
        accuracy: 0.93,
        averageTimeSeconds: 4.2
      },
      {
        taskId: 9,
        taskName: 'Invasive Beetle Detection',
        classifications: 210,
        accuracy: 0.89,
        averageTimeSeconds: 5.1
      }
    ],
    bySpecies: [
      {
        species: 'Vespa mandarinia',
        classifications: 280,
        accuracy: 0.94
      },
      {
        species: 'Anoplophora glabripennis',
        classifications: 140,
        accuracy: 0.88
      }
    ]
  },

  /**
   * GET /api/v1/statistics/me/timeseries
   */
  timeseries: {
    interval: 'DAY',
    data: [
      {
        date: '2025-12-01',
        classifications: 120,
        accuracy: 0.90,
        scoreGained: 420
      },
      {
        date: '2025-12-02',
        classifications: 140,
        accuracy: 0.92,
        scoreGained: 480
      },
      {
        date: '2025-12-03',
        classifications: 160,
        accuracy: 0.93,
        scoreGained: 520
      },
      {
        date: '2025-12-04',
        classifications: 90,
        accuracy: 0.89,
        scoreGained: 300
      }
    ]
  }
}
