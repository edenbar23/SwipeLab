import { useEffect, useRef } from 'react';
import { useAppStateStore } from '@/stores/appStateStore';
import { backendUrl } from '@/api/apiFetch';

export function useHealthCheck() {
  const isMaintenanceMode = useAppStateStore((state) => state.isMaintenanceMode);
  const setMaintenanceMode = useAppStateStore((state) => state.setMaintenanceMode);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (!isMaintenanceMode) {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
      return;
    }

    const checkHealth = async () => {
      try {
        const response = await fetch(`${backendUrl}/health`);
        if (response.ok) {
          setMaintenanceMode(false);
        }
      } catch (e) {
        // Still down
      }
    };

    // Poll every 10 seconds
    timerRef.current = setInterval(checkHealth, 10000);

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [isMaintenanceMode, setMaintenanceMode]);
}
