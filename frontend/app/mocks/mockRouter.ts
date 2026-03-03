import { authMock } from './data/auth.mock'
import { classificationMock } from './data/classification.mock'
import { addToCollection, getCollection, getCollectionStats } from './data/collection.mock'
import { dashboardAdminMock } from './data/dashboard.admin.mock'
import { dashboardUserMock } from './data/dashboard.user.mock'

import { getTaskAnalytics, getUserPerformance } from './data/analytics.mock'
import { refinedChallengesMock } from './data/challenges.mock'
import { setUserAccuracy, statisticsMock } from './data/statistics.mock'

import { MOCK_GOLD_IMAGES } from './data/goldImages.mock'
import { getLeaderboardData, setUserScore } from './data/leaderboard.mock'
import {
  addRecipientGroup,
  addUserToGroup,
  getRecipientGroups,
  removeUserFromGroup
} from './data/recipients.mock'
import { usersMock } from './data/users.mock'
import { API_ENDPOINTS } from '../api/apiEndpoints';


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
  if (url.endsWith(API_ENDPOINTS.AUTH.LOGIN) && method === 'POST') {
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

  if (url.endsWith(API_ENDPOINTS.AUTH.REGISTER) && method === 'POST') {
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

  if (url.endsWith('/api/v1/auth/profile') && method === 'GET') {
    return jsonResponse(authMock.profile)
  }

  if (url.endsWith(API_ENDPOINTS.AUTH.CHANGE_PASSWORD) && method === 'POST') {
    // Validate that newPassword exists in body
    if (!body.newPassword) {
      return jsonResponse({ message: 'New password is required' }, 400);
    }
    return jsonResponse({ message: 'Password has been reset successfully' });
  }


  // ---------- DASHBOARD (USER) ----------
  if (method === 'GET' && url.split('?')[0].endsWith('/api/v1/dashboard/my-tasks')) {
    return jsonResponse(dashboardUserMock.tasks)
  }

  if (method === 'GET' && url.split('?')[0].endsWith('/api/v1/dashboard/available-tasks')) {
    return jsonResponse(dashboardUserMock.availableTasks)
  }

  if (method === 'GET' && url.split('?')[0].match(/\/api\/v1\/dashboard\/my-tasks\/\d+$/)) {
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
  if (method === 'GET' && url.split('?')[0].endsWith('/api/v1/classifications/next-batch')) {
    return jsonResponse(classificationMock.nextBatch)
  }

  if (method === 'POST' && url.includes('/classifications/') && url.endsWith('/submit')) {
    return jsonResponse(classificationMock.submitResult)
  }

  if (method === 'GET' && url.endsWith('/api/v1/classifications/progress')) {
    return jsonResponse(classificationMock.progress)
  }

  // ---------- LEADERBOARD ----------
  if (method === 'GET' && url.includes(API_ENDPOINTS.GAMIFICATION.LEADERBOARD)) {
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
  if (method === 'GET' && url.endsWith(API_ENDPOINTS.STATISTICS.ME)) {
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
  if (method === 'GET' && url.endsWith(API_ENDPOINTS.GAMIFICATION.CHALLENGES)) {
    return jsonResponse(refinedChallengesMock)
  }

  if (method === 'POST' && url.endsWith(API_ENDPOINTS.STATISTICS.UPDATE_ACCURACY)) {
    const { accuracy } = body;
    if (typeof accuracy === 'number') {
      setUserAccuracy(accuracy);
      return jsonResponse({ message: 'Accuracy updated', newStats: statisticsMock.summary });
    }
    return jsonResponse({ message: 'Invalid accuracy' }, 400);
  }

  // ---------- ANALYTICS ----------
  // Task Analytics
  if (method === 'GET' && url.match(/\/api\/v1\/dashboard\/tasks\/\d+\/analytics$/)) {
    const idMatch = url.match(/\/api\/v1\/dashboard\/tasks\/(\d+)\/analytics$/);
    const taskId = idMatch ? parseInt(idMatch[1], 10) : null;

    if (!taskId) return jsonResponse({ message: 'Invalid task ID' }, 400);

    return jsonResponse(getTaskAnalytics(taskId));
  }

  // User Performance Analytics
  if (method === 'GET' && url.includes('/api/v1/analytics/users')) {
    // Extract taskId from query params if present
    const urlObj = new URL(url, 'http://localhost');
    const taskIdParam = urlObj.searchParams.get('taskId');
    const taskId = taskIdParam ? parseInt(taskIdParam, 10) : undefined;

    return jsonResponse(getUserPerformance(taskId));
  }

  // Top Performers
  if (method === 'GET' && url.includes('/api/v1/analytics/top-performers')) {
    const urlObj = new URL(url, 'http://localhost');
    const limitParam = urlObj.searchParams.get('limit');
    let limit = limitParam ? parseInt(limitParam, 10) : 10;
    if (isNaN(limit)) limit = 10;

    // Sort logic isn't strictly necessary for a mock, but sorting by score or current values is nice.
    // For now we slice the user performance mock logic.
    const performers = getUserPerformance().slice(0, limit);
    return jsonResponse(performers);
  }

  // Exports
  if (method === 'POST' && url.includes(API_ENDPOINTS.ADMIN.ANALYTICS_EXPORTS)) {
    return jsonResponse({
      exportId: "exp_" + Date.now().toString(),
      status: "QUEUED",
      createdAt: new Date().toISOString(),
      estimatedCompletion: new Date(Date.now() + 10 * 60000).toISOString()
    }, 202);
  }

  // CHALLENGES
  if (method === 'GET' && url.endsWith(API_ENDPOINTS.GAMIFICATION.CHALLENGES)) {
    return jsonResponse(refinedChallengesMock)
  }

  // ---------- MANAGER / RECIPIENTS ----------
  if (method === 'GET' && url.endsWith(API_ENDPOINTS.ADMIN.RECIPIENTS)) {
    // Note: User provided API used 'dashboard/recipients/*' but list endpoint wasn't explicitly changed. 
    // Assuming standard REST pattern or keeping existing GET but moving to dashboard/recipients if consistent.
    // For now, let's support both or just the existing GET if not specified. 
    // Wait, let's align with the new path API_ENDPOINTS.ADMIN.RECIPIENTS for consistency.
    return jsonResponse(getRecipientGroups())
  }

  // Also support the old one just in case until full refactor
  if (method === 'GET' && url.endsWith('/api/v1/manager/recipient-groups')) {
    return jsonResponse(getRecipientGroups())
  }

  // 4.3 Create Recipients List
  if (method === 'POST' && url.endsWith(API_ENDPOINTS.ADMIN.RECIPIENTS_CREATE)) {
    const { name, usernames } = body as { name: string, usernames: string[] };
    const newGroup = addRecipientGroup(name);

    // Add users immediately
    if (usernames && Array.isArray(usernames)) {
      usernames.forEach(username => {
        addUserToGroup(newGroup.id, username);
      });
    }

    // Return the updated group structure
    const updatedGroup = getRecipientGroups().find(g => g.id === newGroup.id);
    return jsonResponse(updatedGroup, 201);
  }

  // 4.5 Update Recipients List
  if (method === 'PUT' && url.includes('/api/v1/dashboard/recipients/') && url.endsWith('/update')) {
    const groupId = parseInt(url.split('/recipients/')[1].split('/')[0]);
    const { addUsernames, removeUsernames } = body as { addUsernames?: string[], removeUsernames?: string[] };

    let updatedGroup = getRecipientGroups().find(g => g.id === groupId);
    if (!updatedGroup) return jsonResponse({ message: 'Group not found' }, 404);

    if (addUsernames) {
      addUsernames.forEach(username => {
        addUserToGroup(groupId, username);
      });
    }

    if (removeUsernames) {
      removeUsernames.forEach(username => {
        removeUserFromGroup(groupId, username);
      });
    }

    updatedGroup = getRecipientGroups().find(g => g.id === groupId);
    return jsonResponse(updatedGroup);
  }

  // 4.4 Delete Recipients List
  if (method === 'DELETE' && url.includes('/api/v1/dashboard/recipients/')) {
    // Mock deletion (not fully implemented in mock helper but we can simulate success)
    // Parsing ID just to be sure
    // const groupId = parseInt(url.split('/recipients/')[1]);
    return jsonResponse({
      taskId: 7, // Dummy response from contract
      previousStatus: "ACTIVE",
      currentStatus: "PAUSED",
      pausedAt: new Date().toISOString()
    });
  }

  // ---------- USERS MANAGEMENT ----------
  if (method === 'GET' && url.endsWith('/api/v1/manager/users')) {
    // Return the shared usersMock so IDs match what the recipients logic expects
    return jsonResponse(usersMock);
  }

  // ---------- COLLECTION ----------
  if (method === 'GET' && url.endsWith(API_ENDPOINTS.COLLECTION.BASE)) {
    return jsonResponse(getCollection());
  }

  if (method === 'GET' && url.endsWith(API_ENDPOINTS.COLLECTION.STATS)) {
    return jsonResponse(getCollectionStats());
  }

  if (method === 'POST' && url.endsWith(API_ENDPOINTS.COLLECTION.ADD)) {
    const { imageUrl, label, taskId, taskName, question } = body as {
      imageUrl: string;
      label: string;
      taskId: number;
      taskName: string;
      question: string;
    };
    const newItem = addToCollection(imageUrl, label as any, taskId, taskName, question);
    return jsonResponse(newItem, 201);
  }

  // ---------- GOLD IMAGES ----------
  if (method === 'GET' && url.split('?')[0].endsWith(API_ENDPOINTS.ADMIN.GOLD_IMAGES_GET_ALL)) {
    return jsonResponse(MOCK_GOLD_IMAGES)
  }

  if (method === 'GET' && url.split('?')[0].endsWith(API_ENDPOINTS.ADMIN.GOLD_IMAGES)) {
    const urlObj = new URL(url, 'http://localhost');
    const taskIdParam = urlObj.searchParams.get('taskId');
    if (taskIdParam) {
      const taskId = parseInt(taskIdParam, 10);
      return jsonResponse(MOCK_GOLD_IMAGES.filter(g => g.taskId === taskId));
    }
    return jsonResponse(MOCK_GOLD_IMAGES);
  }

  if (method === 'DELETE' && url.includes('/api/admin/gold-images/')) {
    return jsonResponse({ message: 'Gold image deleted' });
  }

  // ---------- FALLBACK ----------
  return jsonResponse({ message: 'Mock route not found' }, 404)
}
