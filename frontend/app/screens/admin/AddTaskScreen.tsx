// admin screen for adding tasks
import React, { useState } from "react";
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity
} from "react-native";
import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";

export default function AddTaskScreen({ navigation }: any) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [targetSpecies, setTargetSpecies] = useState(""); // comma-separated
  const [recipientGroups, setRecipientGroups] = useState(""); // comma-separated
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!name || !description) {
      Alert.alert("Validation Error", "Task name and description are required");
      return;
    }

    const payload = {
      name,
      description,
      targetSpecies: targetSpecies
        .split(",")
        .map((s) => ({ commonName: s.trim() })),
      recipientGroups: recipientGroups
        .split(",")
        .map((id) => Number(id.trim())),
    };

    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/dashboard/tasks", {
        method: "POST",
        body: JSON.stringify(payload),
        headers: { "Content-Type": "application/json" },
      });

      const data = await res.json();
      console.log("AddTask response:", data);

      Alert.alert("Success", "Task created successfully");
      navigation.navigate("Tasks Management"); // go back to tasks list
    } catch (err) {
      console.error("Error creating task:", err);
      Alert.alert("Error", "Failed to create task");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScreenHeaderLayout
        leftIcon={require("../../../assets/images/add_task.png")}
        leftTitle="Add Task"
        rightIcon={require("../../../assets/images/tasks_mgmt.png")}
        rightTitle="Tasks"
        onRightPress={() => navigation.navigate("TasksManagement")}
    >
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.label}>Task Name</Text>
        <TextInput
          style={styles.input}
          value={name}
          onChangeText={setName}
          placeholder="Enter task name"
        />

        <Text style={styles.label}>Description</Text>
        <TextInput
          style={styles.input}
          value={description}
          onChangeText={setDescription}
          placeholder="Enter description"
        />

        <Text style={styles.label}>Target Species (comma-separated)</Text>
        <TextInput
          style={styles.input}
          value={targetSpecies}
          onChangeText={setTargetSpecies}
          placeholder="e.g., Asian Giant Hornet, Honey Bee"
        />

        <Text style={styles.label}>Recipient Groups (IDs, comma-separated)</Text>
        <TextInput
          style={styles.input}
          value={recipientGroups}
          onChangeText={setRecipientGroups}
          placeholder="e.g., 1, 2, 3"
        />

        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleSubmit}
          disabled={loading}
        >
          <Text style={styles.buttonText}>{loading ? "Creating..." : "Create Task"}</Text>
        </TouchableOpacity>
      </ScrollView>
    </ScreenHeaderLayout>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  label: {
    fontWeight: "600",
    marginTop: 12,
    marginBottom: 4,
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 10,
    padding: 10,
    backgroundColor: "#fff",
  },
  button: {
    marginTop: 24,
    backgroundColor: "#10B981",
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
  },
  buttonDisabled: {
    backgroundColor: "#94D3B3",
  },
  buttonText: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 16,
  },
});
