import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React, { useEffect, useState } from "react";
import {
    Alert,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
} from "react-native";

import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { AdminStackParamList } from "../../navigation/adminStack.types";

type Props = NativeStackScreenProps<
  AdminStackParamList,
  "EditTask"
>;

export default function EditTaskScreen({ route, navigation }: Props) {
  const { taskId } = route.params;

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [targetSpecies, setTargetSpecies] = useState("");
  const [recipientGroups, setRecipientGroups] = useState("");
  const [loading, setLoading] = useState(false);

  // 🔹 Load task details
  useEffect(() => {
    apiFetch(`/api/v1/dashboard/tasks/${taskId}`)
      .then((res) => res.json())
      .then((data) => {
        setName(data.name);
        setDescription(data.description);

        setTargetSpecies(
          data.targetSpecies
            ?.map((s: any) => s.name)
            .join(", ")
        );

        setRecipientGroups(
          data.recipientGroups?.join(", ")
        );
      })
      .catch(() => {
        Alert.alert("Error", "Failed to load task data");
      });
  }, [taskId]);

  const handleSubmit = async () => {
    if (!name || !description) {
      Alert.alert("Validation Error", "Task name and description are required");
      return;
    }

    const payload = {
      status: "ACTIVE",
      name,
      description,
      targetSpecies: targetSpecies
        .split(",")
        .map((s) => ({
          name: s.trim(),
          referenceImages: [],
        })),
      recipientGroups: recipientGroups
        .split(",")
        .map((id) => Number(id.trim())),
    };

    try {
      setLoading(true);

      const res = await apiFetch(
        `/api/v1/dashboard/tasks/archive/${taskId}`,
        {
          method: "POST",
          body: JSON.stringify(payload),
          headers: { "Content-Type": "application/json" },
        }
      );

      await res.json();

      Alert.alert("Success", "Task updated successfully");
      navigation.navigate("TasksManagement");
    } catch (err) {
      console.error("Update task error:", err);
      Alert.alert("Error", "Failed to update task");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScreenHeaderLayout
      leftIcon={require("../../../assets/images/tasks_mgmt.png")}
      leftTitle="Edit Task"
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
        />

        <Text style={styles.label}>Description</Text>
        <TextInput
          style={styles.input}
          value={description}
          onChangeText={setDescription}
        />

        <Text style={styles.label}>
          Target Species (comma-separated)
        </Text>
        <TextInput
          style={styles.input}
          value={targetSpecies}
          onChangeText={setTargetSpecies}
        />

        <Text style={styles.label}>
          Recipient Groups (IDs, comma-separated)
        </Text>
        <TextInput
          style={styles.input}
          value={recipientGroups}
          onChangeText={setRecipientGroups}
        />

        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleSubmit}
          disabled={loading}
        >
          <Text style={styles.buttonText}>
            {loading ? "Updating..." : "Update Task"}
          </Text>
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
    backgroundColor: "#2563EB",
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
  },
  buttonDisabled: {
    backgroundColor: "#93C5FD",
  },
  buttonText: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 16,
  },
});
