export const leaderboardMock = {
  global: {
    leaderboard: [
      {
        userId: 2,
        username: 'admin_user',
        rank: 1,
        score: 9850,
        completedTasks: 312,
        accuracy: 0.96,
        streakDays: 42
      },
      {
        userId: 1,
        username: 'john_doe',
        rank: 2,
        score: 7420,
        completedTasks: 198,
        accuracy: 0.91,
        streakDays: 18
      },
      {
        userId: 3,
        username: 'jane_smith',
        rank: 3,
        score: 6890,
        completedTasks: 176,
        accuracy: 0.89,
        streakDays: 12
      }
    ],
    lastUpdated: new Date().toISOString()
  },

  friends: {
    leaderboard: [
      {
        userId: 1,
        username: 'john_doe',
        rank: 1,
        score: 7420,
        completedTasks: 198,
        accuracy: 0.91,
        streakDays: 18
      },
      {
        userId: 4,
        username: 'alex_green',
        rank: 2,
        score: 7010,
        completedTasks: 185,
        accuracy: 0.88,
        streakDays: 9
      }
    ],
    lastUpdated: new Date().toISOString()
  }
}
