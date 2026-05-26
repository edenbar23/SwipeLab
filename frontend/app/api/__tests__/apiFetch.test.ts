import { apiFetch } from '../apiFetch';
import { useAuthStore } from '../../stores/authStore';

// Mock dependencies
jest.mock('../../stores/authStore', () => ({
  useAuthStore: {
    getState: jest.fn(() => ({
      setIsBanned: jest.fn(),
      setSessionExpiredMessage: jest.fn(),
      logout: jest.fn(),
    })),
  },
}));

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(() => Promise.resolve('mock_token')),
}));

// Mock global fetch
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('apiFetch', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('intercepts 403 ACCOUNT_BANNED and calls setIsBanned(true)', async () => {
    const mockSetIsBanned = jest.fn();
    (useAuthStore.getState as jest.Mock).mockReturnValue({
      setIsBanned: mockSetIsBanned,
      setSessionExpiredMessage: jest.fn(),
    });

    const mockResponse = {
      status: 403,
      clone: () => ({
        json: () => Promise.resolve({ errorCode: 'ACCOUNT_BANNED' }),
      }),
    };
    mockFetch.mockResolvedValueOnce(mockResponse);

    await apiFetch('/some-endpoint');

    expect(mockSetIsBanned).toHaveBeenCalledWith(true);
  });

  it('does NOT trigger ban for 403 ACCESS_DENIED', async () => {
    const mockSetIsBanned = jest.fn();
    (useAuthStore.getState as jest.Mock).mockReturnValue({
      setIsBanned: mockSetIsBanned,
      setSessionExpiredMessage: jest.fn(),
    });

    const mockResponse = {
      status: 403,
      clone: () => ({
        json: () => Promise.resolve({ errorCode: 'ACCESS_DENIED' }),
      }),
    };
    mockFetch.mockResolvedValueOnce(mockResponse);

    await apiFetch('/some-endpoint');

    expect(mockSetIsBanned).not.toHaveBeenCalled();
  });
});
