import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

/**
 * For Web: We rely on HttpOnly cookies, so we don't store tokens in localStorage.
 * For Mobile: We use SecureStore to store tokens securely.
 */

export async function getAccessToken(): Promise<string | null> {
  if (Platform.OS === 'web') {
    return null; // Handled by HttpOnly cookies
  }
  return await SecureStore.getItemAsync('token');
}

export async function getRefreshToken(): Promise<string | null> {
  if (Platform.OS === 'web') {
    return null; // Handled by HttpOnly cookies
  }
  return await SecureStore.getItemAsync('refreshToken');
}

export async function setTokens(accessToken: string, refreshToken?: string): Promise<void> {
  if (Platform.OS === 'web') {
    // Backend sets HttpOnly cookies, nothing to store securely in JS.
    // We can store a flag that user is logged in.
    localStorage.setItem('isAuthenticated', 'true');
  } else {
    await SecureStore.setItemAsync('token', accessToken);
    if (refreshToken) {
      await SecureStore.setItemAsync('refreshToken', refreshToken);
    }
  }
}

export async function clearTokens(): Promise<void> {
  if (Platform.OS === 'web') {
    localStorage.removeItem('isAuthenticated');
  } else {
    await SecureStore.deleteItemAsync('token');
    await SecureStore.deleteItemAsync('refreshToken');
  }
}

// Other non-sensitive data
export async function setItem(key: string, value: string): Promise<void> {
  if (Platform.OS === 'web') {
    localStorage.setItem(key, value);
  } else {
    await SecureStore.setItemAsync(key, value);
  }
}

export async function getItem(key: string): Promise<string | null> {
  if (Platform.OS === 'web') {
    return localStorage.getItem(key);
  } else {
    return await SecureStore.getItemAsync(key);
  }
}

export async function removeItem(key: string): Promise<void> {
  if (Platform.OS === 'web') {
    localStorage.removeItem(key);
  } else {
    await SecureStore.deleteItemAsync(key);
  }
}
