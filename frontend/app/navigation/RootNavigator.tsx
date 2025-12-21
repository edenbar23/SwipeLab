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
  const { mode } = useModeStore();              // "USER" | "ADMIN"

  if (!token) {
    return (
      <NavigationContainer>
        <LoginScreen />
      </NavigationContainer>
    );
  }

  const isAdmin = role === "ADMIN";

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <NavigationContainer>
        {isAdmin ? mode === "ADMIN"
            ? <AdminNavigator />
            : <UserNavigator />
          : <UserNavigator />}
      </NavigationContainer>
    </SafeAreaView>
  );
}
