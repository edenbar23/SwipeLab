import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";
import AdminDashboard from "../screens/admin/AdminDashboard";
import BottomBar from "./components/BottomBar";

const Stack = createNativeStackNavigator();

export default function AdminNavigator() {
  return (
    <View style={styles.container}>
      {/* Middle Navigator */}
      <View style={styles.content}>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          <Stack.Screen name="AdminDashboard" component={AdminDashboard} />
          {/* other admin screens */}
        </Stack.Navigator>
      </View>

      {/* Bottom Bar */}
      <BottomBar
        items={[
          { label: "Users", route: "Users" },
          { label: "Leaderboard", route: "Leaderboard" },
          { label: "Analytics", route: "Analytics" },
          { label: "Settings", route: "AdminSettings" },
        ]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { flex: 1 },
});
