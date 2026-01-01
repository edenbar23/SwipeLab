// app/navigation/RootNavigator.tsx

import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
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
  const Stack = createNativeStackNavigator();


  if (!token) {
    return (
      <NavigationContainer>
        <Stack.Navigator>
          <Stack.Screen 
            name="Login" 
            component={LoginScreen} 
            options={{ headerShown: false }} 
          />
        </Stack.Navigator>
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
