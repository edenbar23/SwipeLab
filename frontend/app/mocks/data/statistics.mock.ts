// Dynamic user stats
let userStats = {
  accuracy: 0.91,
  totalClassifications: 1980,
  score: 7420,
  rankGlobal: 2
};

// Function to update user accuracy (for testing)
export function setUserAccuracy(newAccuracy: number) {
  userStats.accuracy = newAccuracy;
}

export function getStatisticsData() {
  return {
    summary: {
      userId: 1,
      username: 'john_doe',
      totalClassifications: userStats.totalClassifications,
      completedTasks: 12,
      accuracy: userStats.accuracy,
      averageTimePerImageSeconds: 4.7,
      currentStreakDays: 18,
      longestStreakDays: 42,
      score: userStats.score,
      rankGlobal: userStats.rankGlobal
    },
    vsExperts: {
      userAccuracy: userStats.accuracy,
      expertAccuracy: 0.95,
      difference: parseFloat((userStats.accuracy - 0.95).toFixed(2)),
      comparedSamples: 860
    },
    vsUsers: {
      userAccuracy: userStats.accuracy,
      averageUserAccuracy: 0.87,
      percentile: userStats.accuracy > 0.9 ? 95 : (userStats.accuracy > 0.85 ? 80 : 50)
    },
    breakdown: {
      byTask: [
        {
          taskId: 7,
          taskName: 'Asian Giant Hornet Identification',
          classifications: 320,
          accuracy: Math.min(1, userStats.accuracy + 0.02),
          averageTimeSeconds: 4.2
        },
        {
          taskId: 9,
          taskName: 'Invasive Beetle Detection',
          classifications: 210,
          accuracy: Math.max(0, userStats.accuracy - 0.02),
          averageTimeSeconds: 5.1
        }
      ],
      bySpecies: [
        {
          species: 'Vespa mandarinia',
          classifications: 280,
          accuracy: Math.min(1, userStats.accuracy + 0.03)
        },
        {
          species: 'Anoplophora glabripennis',
          classifications: 140,
          accuracy: Math.max(0, userStats.accuracy - 0.03)
        }
      ]
    },
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
  };
}

export const statisticsMock = {
  get summary() { return getStatisticsData().summary; },
  get vsExperts() { return getStatisticsData().vsExperts; },
  get vsUsers() { return getStatisticsData().vsUsers; },
  get breakdown() { return getStatisticsData().breakdown; },
  get timeseries() { return getStatisticsData().timeseries; }
};
