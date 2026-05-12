import { useEffect } from 'react';
import { AppState, Platform } from 'react-native';
import { useAuthStore } from '../stores/authStore';
import { jwtDecode } from 'jwt-decode';
import { forceTokenRefresh } from '../api/apiFetch';

const THRESHOLD_SECONDS = 180; // 3 minutes
const INTERVAL_MS = 60 * 1000; // Check every minute

export function useSessionHeartbeat() {
  const checkTokenAndRefresh = async () => {
    try {
      const state = useAuthStore.getState();
      if (!state.token) {
        return;
      }

      const decoded = jwtDecode<{ exp?: number }>(state.token);
      if (!decoded.exp) return;

      const timeRemaining = decoded.exp - Math.floor(Date.now() / 1000);
      
      // If expired or about to expire, force a silent background refresh
      if (timeRemaining <= THRESHOLD_SECONDS) {
        console.log(`[Heartbeat] Token near expiration (${timeRemaining}s left), forcing refresh...`);
        await forceTokenRefresh();
      }
    } catch (error) {
      console.error('[Heartbeat] Error checking token:', error);
    }
  };

  useEffect(() => {
    // 1. AppState listener (React Native)
    const subscription = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'active') {
        checkTokenAndRefresh();
      }
    });

    // 2. Web visibility listener (for frozen tabs in iOS Safari / Chrome Desktop)
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        checkTokenAndRefresh();
      }
    };
    
    const handleFocus = () => {
      checkTokenAndRefresh();
    };

    if (Platform.OS === 'web') {
      document.addEventListener('visibilitychange', handleVisibilityChange);
      window.addEventListener('focus', handleFocus);
    }

    // 3. Periodic interval while actively staring at a cached screen
    const interval = setInterval(() => {
      if (Platform.OS === 'web') {
        if (document.visibilityState === 'visible') {
          checkTokenAndRefresh();
        }
      } else {
        if (AppState.currentState === 'active') {
          checkTokenAndRefresh();
        }
      }
    }, INTERVAL_MS);

    // Initial check on mount
    checkTokenAndRefresh();

    return () => {
      subscription.remove();
      if (Platform.OS === 'web') {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
        window.removeEventListener('focus', handleFocus);
      }
      clearInterval(interval);
    };
  }, []);
}
