// admin screen for managing tasks
import React, { useEffect, useState } from "react";
import { FlatList, View } from "react-native";
import { Colors } from '../../../constants/theme';
import { apiFetch } from "../../api/apiFetch";
import TaskCard from "../../components/admin/TaskCard";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { useThemeStore } from '../../stores/themeStore';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { useAdminTasks } from "../../api/queries";


export default function TasksManagementScreen({ navigation }: any) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const { data: tasks = [], isLoading } = useAdminTasks();

  return (
    <ScreenHeaderLayout
      leftIcon={require("../../../assets/images/tasks_mgmt.png")}
      leftTitle="Tasks"
      rightIcon={require("../../../assets/images/add_task.png")}
      rightTitle="Add Task"
      onRightPress={() => navigation.navigate("AddTask")}
    >
      <View style={{ flex: 1, backgroundColor: themeColors.background }}>
        <FlatList
          showsVerticalScrollIndicator={false}
          data={tasks}
          keyExtractor={(item) => item.taskId.toString()}
          renderItem={({ item }) => (
            <TaskCard
              task={item}
              onPress={() =>
                navigation.navigate("TaskDetails", {
                  taskId: item.taskId,
                })
              }
              onEdit={() =>
                navigation.navigate("EditTask", {
                  taskId: item.taskId,
                })
              }
              onToggleStatus={() => {
                // PATCH /tasks/{id}/status
              }}
              onArchive={() => {
                // confirm + archive
              }}
            />
          )}
        />
      </View>
    </ScreenHeaderLayout>
  );
}
