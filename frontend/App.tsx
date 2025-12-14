import React from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import RootNavigator from "./app/navigation/RootNavigator";

export default function App() {
  console.log("App.tsx IS LOADING!");
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
    <RootNavigator />
      </SafeAreaProvider>
      </GestureHandlerRootView>
  );
  
}
