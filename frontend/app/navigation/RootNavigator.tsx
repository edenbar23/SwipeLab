// app/navigation/RootNavigator.tsx

import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { ActivityIndicator, View, Text } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

// stores
import { useAuthStore } from "../stores/authStore";
import { useModeStore } from "../stores/modeStore";

// navigators
import ResearcherNavigator from "./ResearcherNavigator";
import UserNavigator from "./UserNavigator";

// screens
import LoginScreen from "../screens/shared/LoginScreen";

export default function RootNavigator() {
  const { token, role, isLoading, sessionExpiredMessage } = useAuthStore();       // "USER" | "researcher" | null
  const { mode } = useModeStore();              // "USER" | "researcher"
  const Stack = createNativeStackNavigator();

  if (sessionExpiredMessage) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center", backgroundColor: '#fff' }}>
        <Text style={{ fontSize: 16, color: '#000', textAlign: 'center', marginHorizontal: 20 }}>
          Session expired, please login again. Redirecting to login...
        </Text>
        <ActivityIndicator size="large" color="#000" style={{ marginTop: 20 }} />
      </View>
    );
  }

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

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

  const isAdmin = role === "RESEARCHER";

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <NavigationContainer key={mode}>
        {isAdmin ? mode === "researcher"
          ? <ResearcherNavigator />
          : <UserNavigator />
          : <UserNavigator />}
      </NavigationContainer>
    </SafeAreaView>
  );
}
