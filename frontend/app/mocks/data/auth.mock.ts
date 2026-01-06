export const authMock = {
  sessions: {
    accessToken: 'mock-access-token-123',
    refreshToken: 'mock-refresh-token-456',
    expiresIn: 2592000
  },

  users: [
    {
      username: 'john_doe',
      email: 'user@example.com',
      password: '1234', //not part of the contract only for mock
      role: 'USER'
    },
    {
      username: 'admin_user',
      email: 'admin@swipelab.com',
      password: '1234', //not part of the contract only for mock
      role: 'ADMIN'
    }
  ],
  profile: {
    username: "SwipeMaster2024",
    email: "user@swipelab.com",
    rank: "Gold Tier",
    score: 1250,
    badges: ["🏆", "🔥", "⭐", "💎", "🛡️"]
  }
}
