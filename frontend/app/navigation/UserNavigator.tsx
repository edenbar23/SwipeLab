import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";
import SwipeScreen from "../screens/user/SwipeScreen";
import BottomBar from "./components/BottomBar";
import TopBar from "./components/TopBar";

const Stack = createNativeStackNavigator();

export default function UserNavigator() {
  return (
    <View style={styles.container}>
      {/* Top Bar */}
      <TopBar />

      {/* Middle Navigator */}
      <View style={styles.content}>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          <Stack.Screen name="Swipe" component={SwipeScreen} />
          {/* other screens */}
        </Stack.Navigator>
      </View>

      {/* Bottom Bar */}
      <BottomBar
        items={[
          { label: "My Tasks", route: "Tasks" },
          { label: "Leaderboard", route: "Leaderboard" },
          { label: "Stats", route: "Stats" },
          { label: "Settings", route: "UserSettings" },
        ]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1, // Fill entire screen
  },
  content: {
    flex: 1, // Take remaining space between top and bottom
  },
});
