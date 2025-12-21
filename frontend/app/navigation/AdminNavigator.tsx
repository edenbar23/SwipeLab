import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";
import AdminDashboard from "../screens/admin/AdminDashboard";
import BottomBar from "./components/BottomBar";

import TopBar from "./components/TopBar";

const Stack = createNativeStackNavigator();

export default function AdminNavigator() {
  return (
    <View style={styles.container}>
      <TopBar />
      {/* Middle Navigator */}
      
      <View style={styles.content}>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          <Stack.Screen name="Management Dashboard" component={AdminDashboard} />
          {/* other admin screens */}
        </Stack.Navigator>
      </View>

      {/* Bottom Bar */}
      <BottomBar
              items={[
                { label: "Users", route: "Users", icon: require("../../assets/images/users.png") },
                { label: "Leaderboard", route: "Leaderboard", icon: require("../../assets/images/leaderboard.png") },
                { label: "Analytics", route: "Analytics", icon: require("../../assets/images/stats.png") },
                { label: "Settings", route: "UserSettings", icon: require("../../assets/images/settings.png") },
              ]}
            />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { flex: 1 },
});
