import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";
import WebAppShell from "../components/layout/WebAppShell";
import SettingsScreen from "../screens/shared/SettingsScreen";
import LeaderboardScreen from "../screens/user/LeaderboardScreen";
import UserMyTasksScreen from "../screens/user/UserMyTasksScreen";
import SwipeScreen from "../screens/user/SwipeScreen";
import ChallengesScreen from "../screens/user/ChallengesScreen";
import StatsScreen from "../screens/user/StatsScreen";
import MyCollectionScreen from "../screens/user/MyCollectionScreen";
import ProfileScreen from "../screens/shared/ProfileScreen";
import BottomBar from "./components/BottomBar";
import TaskDetailsScreen from "../screens/user/TaskDetailsScreen";
import TopBar from "./components/TopBar";

import CollectionDetailsScreen from "../screens/user/CollectionDetailsScreen";

const Stack = createNativeStackNavigator();

export default function UserNavigator() {
  return (
    <WebAppShell variant="user">
      <View style={styles.container}>
        {/* Global TopBar for all user screens */}
        <TopBar />

        {/* Middle Navigator */}
        <View style={styles.content}>
          <Stack.Navigator screenOptions={{ headerShown: false }}>
            <Stack.Screen name="SwipeLab" component={SwipeScreen} />
            <Stack.Screen name="Tasks" component={UserMyTasksScreen} />
            <Stack.Screen name="TaskDetails" component={TaskDetailsScreen} />
            <Stack.Screen name="Challenges" component={ChallengesScreen} />
            <Stack.Screen name="Leaderboard" component={LeaderboardScreen} />
            <Stack.Screen name="Collection" component={MyCollectionScreen} />
            <Stack.Screen name="CollectionDetails" component={CollectionDetailsScreen} />
            <Stack.Screen name="UserSettings" component={SettingsScreen} />
            <Stack.Screen name="Stats" component={StatsScreen} />
            <Stack.Screen name="Profile" component={ProfileScreen} />
          </Stack.Navigator>
        </View>

        {/* Bottom Bar */}
        <BottomBar
          items={[
            { label: "Home", route: "SwipeLab", icon: require("../../assets/images/home.png") },
            { label: "My Tasks", route: "Tasks", icon: require("../../assets/images/tasks.png") },
            { label: "Leaderboard", route: "Leaderboard", icon: require("../../assets/images/leaderboard.png") },
            { label: "Stats", route: "Stats", icon: require("../../assets/images/stats.png") },
            { label: "Settings", route: "UserSettings", icon: require("../../assets/images/settings.png") },
          ]}
        />
      </View>
    </WebAppShell>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
  },
});
