import { mockRouter } from '../mocks/mockRouter'

const USE_MOCKS = __DEV__

export async function apiFetch(
  input: RequestInfo,
  init?: RequestInit
): Promise<Response> {
  const url = typeof input === 'string' ? input : input.url
  const method = (init?.method ?? 'GET').toUpperCase() as any

  if (USE_MOCKS) {
    const mockResponse = await mockRouter(url, method, init)
    if (mockResponse) {
      console.log('[MOCK]', method, url)
      return mockResponse
    }
  }

  return fetch(input, init)
}
