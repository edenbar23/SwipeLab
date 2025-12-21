import { authMock } from './data/auth.mock'
import { classificationMock } from './data/classification.mock'
import { dashboardAdminMock } from './data/dashboard.admin.mock'
import { dashboardUserMock } from './data/dashboard.user.mock'
import { leaderboardMock } from './data/leaderboard.mock'
import { statisticsMock } from './data/statistics.mock'

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
    return jsonResponse(leaderboardMock)
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

  // ---------- FALLBACK ----------
  return jsonResponse({ message: 'Mock route not found' }, 404)
}
