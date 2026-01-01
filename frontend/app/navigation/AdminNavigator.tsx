import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";

import BottomBar from "./components/BottomBar";
import TopBar from "./components/TopBar";

import AddTaskScreen from "../screens/admin/AddTaskScreen";
import AdminDashboard from "../screens/admin/AdminDashboard";
import EditTaskScreen from "../screens/admin/EditTaskScreen";
import TaskDetailsScreen from "../screens/admin/TaskDetailsScreen";
import TasksManagementScreen from "../screens/admin/TasksManagementScreen";

import { AdminStackParamList } from "./adminStack.types";

const Stack = createNativeStackNavigator<AdminStackParamList>();

export default function AdminNavigator() {
  return (
    <View style={styles.container}>
      <TopBar />
      {/* Middle Navigator */}
      
      <View style={styles.content}>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
  <Stack.Screen
    name="AdminDashboard"
    component={AdminDashboard}
    options={{ title: "SwipeLab Admin Dashboard" }}
  />

  <Stack.Screen
    name="TasksManagement"
    component={TasksManagementScreen}
    options={{ title: "Tasks Management" }}
  />

  <Stack.Screen
    name="TaskDetails"
    component={TaskDetailsScreen}
    options={{ title: "Task Details" }}
  />

  <Stack.Screen
    name="AddTask"
    component={AddTaskScreen}
    options={{ title: "Add Task" }}
  />

  <Stack.Screen
    name="EditTask"
    component={EditTaskScreen}
    options={{ title: "Edit Task" }}
  />
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
