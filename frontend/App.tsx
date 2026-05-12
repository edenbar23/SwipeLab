import React from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import RootNavigator from "./app/navigation/RootNavigator";
import { useAuthStore } from "./app/stores/authStore";
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './app/queryClient';
import { useSessionHeartbeat } from "./app/hooks/useSessionHeartbeat";

export default function App() {
  console.log("App.tsx IS LOADING!");

  React.useEffect(() => {
    useAuthStore.getState().initialize();
  }, []);

  useSessionHeartbeat();

  return (
    <QueryClientProvider client={queryClient}>
      <GestureHandlerRootView style={{ flex: 1 }}>
        <SafeAreaProvider style={{ flex: 1 }}>
          <RootNavigator />
        </SafeAreaProvider>
      </GestureHandlerRootView>
    </QueryClientProvider>
  );

}
