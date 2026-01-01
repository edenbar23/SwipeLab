// admin screen for managing tasks
import React, { useEffect, useState } from "react";
import { FlatList } from "react-native";
import { apiFetch } from "../../api/apiFetch";
import TaskCard from "../../components/admin/TaskCard";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";

export default function TasksManagementScreen({ navigation }: any) {
  const [tasks, setTasks] = useState<any[]>([]);

useEffect(() => {
  apiFetch("/api/v1/dashboard/tasks")
    .then((res: Response) => res.json()) // parse the JSON body
    .then((data) => {
      console.log("Parsed API data:", data); // now you can see the actual tasks
      setTasks(data.tasks); // use the correct property
    })
    .catch((err) => console.error("API fetch error:", err));
}, []);


  return (
    <ScreenHeaderLayout
      leftIcon={require("../../../assets/images/tasks_mgmt.png")}
      leftTitle="Tasks"
      rightIcon={require("../../../assets/images/add_task.png")}
      rightTitle="Add Task"
      onRightPress={() => navigation.navigate("AddTask")}
    >
      <FlatList
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
    </ScreenHeaderLayout>
  );
}
