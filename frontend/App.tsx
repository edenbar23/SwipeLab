import React from "react";
import { Platform, View, StyleSheet } from "react-native";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import RootNavigator from "@/navigation/RootNavigator";
import { QueryClientProvider } from '@tanstack/react-query';
import { useSessionHeartbeat } from "@/hooks/useSessionHeartbeat";
import { GlobalErrorBoundary } from "@/components/GlobalErrorBoundary";
import Toast from 'react-native-toast-message';
import { useAppStateStore } from "@/stores/appStateStore";
import { MaintenanceScreen } from "@/screens/shared/MaintenanceScreen";
import { useHealthCheck } from "@/hooks/useHealthCheck";
import GlobalDownloadToast from "@/components/ui/GlobalDownloadToast";

export default function App() {

  // Use inline imports for stores that might cause circular dependency if imported top-level
  const initialize = require("@/stores/authStore").useAuthStore((state: any) => state.initialize);
  const isMaintenanceMode = useAppStateStore((state) => state.isMaintenanceMode);

  React.useEffect(() => {
    initialize();
  }, [initialize]);

  useSessionHeartbeat();
  useHealthCheck();

  return (
    <GlobalErrorBoundary>
      <QueryClientProvider client={require('@/queryClient').queryClient}>
        <View style={styles.rootBackground}>
          <GestureHandlerRootView style={{ flex: 1 }}>
            <SafeAreaProvider style={{ flex: 1 }}>
              {isMaintenanceMode ? (
                <MaintenanceScreen />
              ) : (
                <RootNavigator />
              )}
            </SafeAreaProvider>
          </GestureHandlerRootView>
        </View>
      </QueryClientProvider>
      <GlobalDownloadToast />
      <Toast />
    </GlobalErrorBoundary>
  );
}

const styles = StyleSheet.create({
  rootBackground: {
    flex: 1,
    backgroundColor: '#fff',
  }
});
