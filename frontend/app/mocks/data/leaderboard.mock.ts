// Mock data matching backend Gamification entity
export interface Gamification {
  username: string;
  score: number;
  currentStreak: number;
  longestStreak: number;
  badge: string | null;
}

const allPlayers: Gamification[] = [
  { username: 'GoldWolf', score: 12430, currentStreak: 5, longestStreak: 12, badge: 'Gold' },
  { username: 'MoonNinja', score: 11980, currentStreak: 3, longestStreak: 8, badge: 'Silver' },
  { username: 'LunaStar', score: 11820, currentStreak: 7, longestStreak: 15, badge: 'Silver' },
  { username: 'IceBlade', score: 10905, currentStreak: 2, longestStreak: 5, badge: 'Bronze' },
  { username: 'FlashSoda', score: 9800, currentStreak: 1, longestStreak: 4, badge: null },
  { username: 'AutoWiz', score: 9200, currentStreak: 0, longestStreak: 3, badge: null },
  { username: 'EmberGuy', score: 8500, currentStreak: 4, longestStreak: 10, badge: 'Bronze' },
  { username: 'BluePhoenix', score: 8000, currentStreak: 2, longestStreak: 6, badge: null },
];

export let currentUserScore = 7542;

export function setUserScore(newScore: number) {
  currentUserScore = newScore;
}

export function getLeaderboardData(): Gamification[] {
  const currentUser: Gamification = {
    username: 'You',
    score: currentUserScore,
    currentStreak: 4,
    longestStreak: 9,
    badge: 'Bronze'
  };

  // Combine and sort
  return [...allPlayers, currentUser].sort((a, b) => b.score - a.score);
}

// Legacy export if needed, but mainly for the router
export const leaderboardMock = getLeaderboardData();
