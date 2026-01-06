// All players with their start of month score for dynamic calculation
const allPlayers = [
  { username: 'GoldWolf', score: 12430, startOfMonthScore: 11000 },
  { username: 'MoonNinja', score: 11980, startOfMonthScore: 9800 }, // Monthly: 2180
  { username: 'LunaStar', score: 11820, startOfMonthScore: 11000 },
  { username: 'IceBlade', score: 10905, startOfMonthScore: 10000 },
  { username: 'FlashSoda', score: 9800, startOfMonthScore: 7320 }, // Monthly: 2480 (Top 1)
  { username: 'AutoWiz', score: 9200, startOfMonthScore: 6890 },   // Monthly: 2310 (Top 2)
  { username: 'EmberGuy', score: 8500, startOfMonthScore: 6250 },  // Monthly: 2250 (Top 3)
  { username: 'BluePhoenix', score: 8000, startOfMonthScore: 7000 },
];

// Current user data
export let currentUserScore = 7542;
export let currentUserStartOfMonthScore = 7000; // Monthly: 542

// Function to update user score (for testing)
export function setUserScore(newScore: number) {
  currentUserScore = newScore;
}

// Function to get leaderboard with dynamic ranking
export function getLeaderboardData() {
  const currentUser = {
    username: 'User123',
    score: currentUserScore,
    startOfMonthScore: currentUserStartOfMonthScore
  };

  // Combine all players with current user
  const allPlayersWithUser = [...allPlayers, currentUser];

  // 1. Calculate All Time Rankings
  const sortedAllTime = [...allPlayersWithUser].sort((a, b) => b.score - a.score);
  const userAllTimeRank = sortedAllTime.findIndex(p => p.username === 'User123') + 1;

  const allTimeLeaderboard = sortedAllTime.slice(0, 4).map((player, index) => ({
    rank: index + 1,
    username: player.username,
    score: player.score,
  }));

  // 2. Calculate Monthly Rankings (Score - StartOfMonthScore)
  const playersWithMonthly = allPlayersWithUser.map(p => ({
    ...p,
    monthlyScore: p.score - p.startOfMonthScore
  }));

  const sortedMonthly = playersWithMonthly.sort((a, b) => b.monthlyScore - a.monthlyScore);

  const monthlyLeaderboard = sortedMonthly.slice(0, 4).map((player, index) => ({
    rank: index + 1,
    username: player.username,
    score: player.monthlyScore,
  }));

  return {
    currentUser: {
      rank: userAllTimeRank,
      username: 'User123',
      score: currentUserScore,
    },
    allTime: allTimeLeaderboard,
    monthly: monthlyLeaderboard,
    lastUpdated: new Date().toISOString(),
  };
}

// Legacy export for backward compatibility
export const leaderboardMock = {
  get currentUser() {
    return getLeaderboardData().currentUser;
  },
  get allTime() {
    return getLeaderboardData().allTime;
  },
  get monthly() {
    return getLeaderboardData().monthly;
  },
  get lastUpdated() {
    return getLeaderboardData().lastUpdated;
  },
};
