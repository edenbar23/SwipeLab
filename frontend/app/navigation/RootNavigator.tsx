// app/navigation/RootNavigator.tsx

import { NavigationContainer } from "@react-navigation/native";
import React from "react";
import { SafeAreaView } from "react-native-safe-area-context";

// stores
import { useAuthStore } from "../stores/authStore";
import { useModeStore } from "../stores/modeStore";

// navigators
import AdminNavigator from "./AdminNavigator";
import UserNavigator from "./UserNavigator";

// screens
import LoginScreen from "../screens/shared/LoginScreen";

export default function RootNavigator() {
  const { token, role } = useAuthStore();       // "USER" | "ADMIN" | null
  const { mode } = useModeStore();              // "user" | "admin"

  // Not logged in → go to Login
  if (!token) {
    return (
      <NavigationContainer>
        <LoginScreen />
      </NavigationContainer>
    );
  }

  // Logged in → if user is ADMIN, mode determines flow
  const isAdmin = role === "ADMIN";

return (
  <SafeAreaView style={{ flex: 1 }}>
    <NavigationContainer>
      {isAdmin ? <AdminNavigator /> : <UserNavigator />}
    </NavigationContainer>
  </SafeAreaView>
);

}
