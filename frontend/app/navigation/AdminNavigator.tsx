import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";
import WebAppShell from "../components/layout/WebAppShell";

import BottomBar from "./components/BottomBar";
import TopBar from "./components/TopBar";

import AddTaskScreen from "../screens/admin/AddTaskScreen";
import AdminDashboard from "../screens/admin/AdminDashboard";
import EditTaskScreen from "../screens/admin/EditTaskScreen";
import TaskDetailsScreen from "../screens/admin/TaskDetailsScreen";
import TasksManagementScreen from "../screens/admin/TasksManagementScreen";
import GoldImagesManagementScreen from "../screens/admin/GoldImagesManagementScreen";
import AddGoldImageScreen from "../screens/admin/AddGoldImageScreen";
import AddUserScreen from "../screens/admin/AddUserScreen";
import AnalyticsScreen from "../screens/admin/AnalyticsScreen";
import RecipientsListScreen from "../screens/admin/RecipientsListScreen";
import RecipientGroupDetailsScreen from "../screens/admin/RecipientGroupDetailsScreen";
import UsersManagementScreen from "../screens/admin/UsersManagementScreen";
import TaxonomyScreen from "../screens/admin/TaxonomyScreen";
import SettingsScreen from "../screens/shared/SettingsScreen";
import ProfileScreen from "../screens/shared/ProfileScreen";

import { AdminStackParamList } from "./adminStack.types";

const Stack = createNativeStackNavigator<AdminStackParamList>();

export default function AdminNavigator() {
  return (
    <WebAppShell variant="admin">
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

            <Stack.Screen
              name="GoldImagesManagement"
              component={GoldImagesManagementScreen}
              options={{ title: "Gold Images Management" }}
            />

            <Stack.Screen
              name="AddGoldImage"
              component={AddGoldImageScreen}
              options={{ title: "Add Gold Image" }}
            />

            <Stack.Screen
              name="RecipientsList"
              component={RecipientsListScreen}
              options={{ title: "Recipients List" }}
            />

            <Stack.Screen
              name="RecipientGroupDetails"
              component={RecipientGroupDetailsScreen}
              options={{ title: "Recipient Group Details" }}
            />

            <Stack.Screen
              name="Analytics"
              component={AnalyticsScreen}
              options={{ title: "Analytics" }}
            />

            <Stack.Screen
              name="UsersManagement"
              component={UsersManagementScreen}
              options={{ title: "Users Management" }}
            />

            <Stack.Screen
              name="AddUser"
              component={AddUserScreen}
              options={{ title: "Add User" }}
            />

            <Stack.Screen
              name="UserSettings"
              component={SettingsScreen}
              options={{ title: "Settings" }}
            />
            <Stack.Screen
              name="Profile"
              component={ProfileScreen}
              options={{ title: "My Profile" }}
            />

            <Stack.Screen
              name="Taxonomy"
              component={TaxonomyScreen}
              options={{ title: "Taxonomy" }}
            />
          </Stack.Navigator>

        </View>

        {/* Bottom Bar */}
        <BottomBar
          items={[
            { label: "Home", route: "AdminDashboard", icon: require("../../assets/images/home.png") },
            { label: "Users", route: "UsersManagement", icon: require("../../assets/images/users.png") },
            { label: "Tasks", route: "TasksManagement", icon: require("../../assets/images/tasks_mgmt.png") },
            { label: "Analytics", route: "Analytics", icon: require("../../assets/images/stats.png") },
            { label: "Settings", route: "UserSettings", icon: require("../../assets/images/settings.png") },
          ]}
        />
      </View>
    </WebAppShell>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { flex: 1 },
});
