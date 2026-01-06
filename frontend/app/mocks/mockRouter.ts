import { authMock } from './data/auth.mock'
import { classificationMock } from './data/classification.mock'
import { dashboardAdminMock } from './data/dashboard.admin.mock'
import { dashboardUserMock } from './data/dashboard.user.mock'

import { leaderboardMock } from './data/leaderboard.mock'
import { refinedChallengesMock } from './data/challenges.mock'
import { statisticsMock, setUserAccuracy } from './data/statistics.mock'
import { getLeaderboardData, setUserScore } from './data/leaderboard.mock'
import {
  addRecipientGroup,
  addUserToGroup,
  removeUserFromGroup,
  getRecipientGroups
} from './data/recipients.mock'

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

type LoginBody = {
  username?: string
  email?: string
  password?: string
}

type RegisterBody = {
  username: string
  email: string
  password: string
}

// ---------- helper ----------
function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

// ---------- mock router ----------
export async function mockRouter(
  url: string,
  method: Method,
  init?: RequestInit
): Promise<Response> {
  const body = init?.body ? JSON.parse(init.body as string) : {}

  // ---------- AUTH ----------
  if (url.endsWith('/api/v1/auth/login') && method === 'POST') {
    const loginBody = body as LoginBody

    const user = authMock.users.find(
      u =>
        (u.username === loginBody.username || u.email === loginBody.email) &&
        u.password === loginBody.password
    )

    if (!user) {
      return jsonResponse({ message: 'Invalid credentials' }, 401)
    }

    return jsonResponse({
      accessToken: authMock.sessions.accessToken,
      refreshToken: authMock.sessions.refreshToken,
      expiresIn: authMock.sessions.expiresIn,
      user: {
        username: user.username,
        email: user.email,
        role: user.role
      }
    })
  }

  if (url.endsWith('/api/v1/auth/register') && method === 'POST') {
    const registerBody = body as RegisterBody

    const exists = authMock.users.some(
      u => u.username === registerBody.username || u.email === registerBody.email
    )
    if (exists) {
      return jsonResponse({ message: 'User already exists' }, 409)
    }

    // Add new user to mock DB
    const newUser = {
      username: registerBody.username,
      email: registerBody.email,
      password: registerBody.password,
      role: 'USER' // default role
    }
    authMock.users.push(newUser)

    return jsonResponse({
      accessToken: authMock.sessions.accessToken,
      refreshToken: authMock.sessions.refreshToken,
      expiresIn: authMock.sessions.expiresIn,
      user: {
        username: newUser.username,
        email: newUser.email,
        role: newUser.role
      }
    }, 201)
  }

  if (url.endsWith('/api/v1/auth/me') && method === 'GET') {
    const authHeader =
      init?.headers instanceof Headers ? init.headers.get('Authorization') : undefined

    if (!authHeader) return new Response(null, { status: 401 })

    // For simplicity, return first user whose token matches mock token
    const user = authMock.users.find(u => authHeader.includes(u.username)) || authMock.users[0]

    return jsonResponse({
      username: user.username,
      email: user.email,
      role: user.role
    })
  }

  // ---------- DASHBOARD (USER) ----------
  if (method === 'GET' && url.endsWith('/api/v1/dashboard/my-tasks')) {
    return jsonResponse(dashboardUserMock.tasks)
  }

  if (method === 'GET' && url.match(/\/api\/v1\/dashboard\/my-tasks\/\d+$/)) {
    return jsonResponse(dashboardUserMock.taskDetails)
  }

  if (method === 'GET' && url.endsWith('/play')) {
    return jsonResponse(dashboardUserMock.play)
  }

  // ---------- DASHBOARD (ADMIN) ----------
  if (method === 'GET' && url.endsWith('/api/v1/dashboard/tasks')) {
    return jsonResponse(dashboardAdminMock.tasks)
  }

  if (method === 'GET' && url.match(/\/api\/v1\/dashboard\/tasks\/\d+$/)) {
    // Extract task ID from URL
    const idMatch = url.match(/\/api\/v1\/dashboard\/tasks\/(\d+)$/)
    const taskId = idMatch ? parseInt(idMatch[1], 10) : null

    if (!taskId) return jsonResponse({ message: 'Invalid task ID' }, 400)

    // Find task in mock
    const task = dashboardAdminMock.tasks.tasks.find(t => t.taskId === taskId)

    if (!task) return jsonResponse({ message: 'Task not found' }, 404)

    // Construct task details (example: same structure as your API returns)
    const taskDetails = {
      ...task,
      // Add extra fields if needed
      targetSpecies: task.targetSpecies || [],
      experiments: task.experiments || [],
      recipientGroups: task.recipientGroups || [],
      progress: task.progress || { totalImages: 0, imagesClassified: 0 },
      minClassificationsPerImage: task.minClassificationsPerImage || 3,
      consensusThreshold: task.consensusThreshold || 80.0
    }

    return jsonResponse(taskDetails)
  }


  // ---------- CLASSIFICATION ----------
  if (method === 'GET' && url.endsWith('/api/v1/classifications/next-batch')) {
    return jsonResponse(classificationMock.nextBatch)
  }

  if (method === 'POST' && url.includes('/classifications/') && url.endsWith('/submit')) {
    return jsonResponse(classificationMock.submitResult)
  }

  if (method === 'GET' && url.endsWith('/api/v1/classifications/progress')) {
    return jsonResponse(classificationMock.progress)
  }

  // ---------- LEADERBOARD ----------
  if (method === 'GET' && url.includes('/api/v1/leaderboard/')) {
    return jsonResponse(getLeaderboardData())
  }

  // Update user score (for testing dynamic ranking)
  if (method === 'POST' && url.endsWith('/api/v1/leaderboard/update-score')) {
    const { score } = body as { score: number }
    if (typeof score === 'number') {
      setUserScore(score)
      return jsonResponse({ success: true, newRank: getLeaderboardData().currentUser.rank })
    }
    return jsonResponse({ message: 'Invalid score' }, 400)
  }

  // ---------- STATISTICS ----------
  if (method === 'GET' && url.endsWith('/api/v1/statistics/me')) {
    return jsonResponse(statisticsMock.summary)
  }

  if (method === 'GET' && url.endsWith('/vs-experts')) {
    return jsonResponse(statisticsMock.vsExperts)
  }

  if (method === 'GET' && url.endsWith('/vs-users')) {
    return jsonResponse(statisticsMock.vsUsers)
  }

  if (method === 'GET' && url.endsWith('/breakdown')) {
    return jsonResponse(statisticsMock.breakdown)
  }

  if (method === 'GET' && url.endsWith('/timeseries')) {
    return jsonResponse(statisticsMock.timeseries)
  }

  // CHALLENGES
  if (method === 'GET' && url.endsWith('/api/v1/challenges')) {
    return jsonResponse(refinedChallengesMock)
  }

  if (method === 'POST' && url.endsWith('/api/v1/statistics/update-accuracy')) {
    const { accuracy } = body;
    if (typeof accuracy === 'number') {
      setUserAccuracy(accuracy);
      return jsonResponse({ message: 'Accuracy updated', newStats: statisticsMock.summary });
    }
    return jsonResponse({ message: 'Invalid accuracy' }, 400);
  }

  // ---------- MANAGER / RECIPIENTS ----------
  if (method === 'GET' && url.endsWith('/api/v1/manager/recipient-groups')) {
    return jsonResponse(getRecipientGroups())
  }

  // Create Group
  if (method === 'POST' && url.endsWith('/api/v1/manager/recipient-groups')) {
    const { name } = body as { name: string };
    const newGroup = addRecipientGroup(name);
    return jsonResponse(newGroup, 201);
  }

  // Add User(s) to Group
  if (method === 'POST' && url.match(/\/api\/v1\/manager\/recipient-groups\/\d+\/users$/)) {
    const groupId = parseInt(url.split('/recipient-groups/')[1].split('/')[0]);

    // Support batch { userIds: [] } or single { userId }
    const userIds = body.userIds || (body.userId ? [body.userId] : []);

    let updatedGroup;
    userIds.forEach((uid: number) => {
      updatedGroup = addUserToGroup(groupId, uid);
    });

    if (!updatedGroup && userIds.length > 0) return jsonResponse({ message: 'Group or User not found' }, 404);

    // Return the latest state of the group
    return jsonResponse(getRecipientGroups().find(g => g.id === groupId));
  }

  // Remove User from Group
  if (method === 'DELETE' && url.match(/\/api\/v1\/manager\/recipient-groups\/\d+\/users\/\d+$/)) {
    const parts = url.split('/');
    const groupId = parseInt(parts[parts.indexOf('recipient-groups') + 1]);
    const userId = parseInt(parts[parts.indexOf('users') + 1]);

    const updatedGroup = removeUserFromGroup(groupId, userId);
    if (!updatedGroup) return jsonResponse({ message: 'Group not found' }, 404);
    return jsonResponse(updatedGroup);
  }

  // ---------- FALLBACK ----------
  return jsonResponse({ message: 'Mock route not found' }, 404)
}
