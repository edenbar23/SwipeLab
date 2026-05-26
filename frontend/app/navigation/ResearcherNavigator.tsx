import { createNativeStackNavigator } from "@react-navigation/native-stack";
import React from "react";
import { StyleSheet, View } from "react-native";

import BottomBar from "./components/BottomBar";
import TopBar from "./components/TopBar";

import { useAuthStore } from "../stores/authStore";

import AddTaskScreen from "../screens/researcher/AddTaskScreen";
import ResearcherDashboard from "../screens/researcher/ResearcherDashboard";
import EditTaskScreen from "../screens/researcher/EditTaskScreen";
import TaskDetailsScreen from "../screens/researcher/TaskDetailsScreen";
import TasksManagementScreen from "../screens/researcher/TasksManagementScreen";
import GoldImagesManagementScreen from "../screens/researcher/GoldImagesManagementScreen";
import AddGoldImageScreen from "../screens/researcher/AddGoldImageScreen";
import AddUserScreen from "../screens/researcher/AddUserScreen";
import AnalyticsScreen from "../screens/researcher/AnalyticsScreen";
import RecipientsListScreen from "../screens/researcher/RecipientsListScreen";
import RecipientGroupDetailsScreen from "../screens/researcher/RecipientGroupDetailsScreen";
import UsersManagementScreen from "../screens/researcher/UsersManagementScreen";
import TaxonomyScreen from "../screens/researcher/TaxonomyScreen";
import SettingsScreen from "../screens/shared/SettingsScreen";
import ProfileScreen from "../screens/shared/ProfileScreen";

import { researcherStackParamList } from "./researcherStack.types";

const Stack = createNativeStackNavigator<researcherStackParamList>();

export default function ResearcherNavigator() {
  const isSuperAdmin = useAuthStore((state) => state.isSuperAdmin);
  
  const bottomBarItems = [
    { label: "Home", route: "ResearcherDashboard", icon: require("../../assets/images/home.png") },
    ...(isSuperAdmin ? [{ label: "Users", route: "UsersManagement", icon: require("../../assets/images/users.png") }] : []),
    { label: "Tasks", route: "TasksManagement", icon: require("../../assets/images/tasks_mgmt.png") },
    { label: "Analytics", route: "Analytics", icon: require("../../assets/images/stats.png") },
    { label: "Settings", route: "UserSettings", icon: require("../../assets/images/settings.png") },
  ];

  return (
    <View style={styles.container}>
        <TopBar />
        {/* Middle Navigator */}

        <View style={styles.content}>
          <Stack.Navigator screenOptions={{ headerShown: false }}>
            <Stack.Screen
              name="ResearcherDashboard"
              component={ResearcherDashboard}
              options={{ title: "SwipeLab researcher Dashboard" }}
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

            {isSuperAdmin && (
              <>
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
              </>
            )}

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
          items={bottomBarItems}
        />
      </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { flex: 1 },

});
