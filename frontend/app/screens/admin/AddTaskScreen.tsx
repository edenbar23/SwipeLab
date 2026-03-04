// admin screen for adding tasks
import React, { useEffect, useState } from "react";
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View
} from "react-native";
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from "../../api/apiEndpoints";
import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import MultiSelect from "../../components/ui/MultiSelect";
import { useThemeStore } from '../../stores/themeStore';


export default function AddTaskScreen({ navigation }: any) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [targetSpecies, setTargetSpecies] = useState(""); // comma-separated
  const [selectedRecipients, setSelectedRecipients] = useState<string[]>([]);
  const [availableOptions, setAvailableOptions] = useState<{ id: string; label: string }[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isPublic, setIsPublic] = useState(false);
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  useEffect(() => {
    const fetchOptions = async () => {
      setOptionsLoading(true);
      try {
        const [groupsRes, usersRes] = await Promise.all([
          apiFetch(API_ENDPOINTS.ADMIN.RECIPIENTS),
          apiFetch(API_ENDPOINTS.USERS.GET_ALL)
        ]);

        let loaded: { id: string, label: string }[] = [];
        if (groupsRes.ok) {
          const groups = await groupsRes.json();
          groups.forEach((g: any) => loaded.push({ id: `G-${g.groupId || g.id}`, label: `(Group) ${g.name}` }));
        }
        if (usersRes.ok) {
          const users = await usersRes.json();
          users.forEach((u: any) => loaded.push({ id: `U-${u.username}`, label: `(User) ${u.username}` }));
        }
        setAvailableOptions(loaded);
      } catch (error) {
        console.error("Failed to load options:", error);
      } finally {
        setOptionsLoading(false);
      }
    };
    fetchOptions();
  }, []);

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
      isPublic,
      recipientGroups: selectedRecipients.filter(id => id.startsWith("G-")).map(id => Number(id.replace("G-", ""))),
      assignedUsernames: selectedRecipients.filter(id => id.startsWith("U-")).map(id => id.replace("U-", "")),
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
      <ScrollView contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]} showsVerticalScrollIndicator={false}>
        <Text style={[styles.label, { color: themeColors.text }]}>Task Name</Text>
        <TextInput
          style={[styles.input, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
          value={name}
          onChangeText={setName}
          placeholder="Enter task name"
        />

        <Text style={[styles.label, { color: themeColors.text }]}>Description</Text>
        <TextInput
          style={[styles.input, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
          value={description}
          onChangeText={setDescription}
          placeholder="Enter description"
          placeholderTextColor={themeColors.textSecondary}
        />

        <Text style={[styles.label, { color: themeColors.text }]}>Target Species (comma-separated)</Text>
        <TextInput
          style={[styles.input, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
          value={targetSpecies}
          onChangeText={setTargetSpecies}
          placeholder="e.g., Asian Giant Hornet, Honey Bee"
          placeholderTextColor={themeColors.textSecondary}
        />

        <Text style={[styles.label, { color: themeColors.text }]}>Task Visibility</Text>
        <View style={{ flexDirection: 'row', marginBottom: 12, marginTop: 4 }}>
          <TouchableOpacity
            style={[styles.toggleBtn, isPublic && styles.toggleActive]}
            onPress={() => setIsPublic(true)}
          >
            <Text style={[styles.toggleText, isPublic && styles.toggleTextActive]}>Public (All)</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.toggleBtn, !isPublic && styles.toggleActive]}
            onPress={() => setIsPublic(false)}
          >
            <Text style={[styles.toggleText, !isPublic && styles.toggleTextActive]}>Restricted</Text>
          </TouchableOpacity>
        </View>

        {!isPublic && (
          <>
            <Text style={[styles.label, { color: themeColors.text }]}>Assign Recipients</Text>
            <MultiSelect
              options={availableOptions}
              selectedIds={selectedRecipients}
              onToggle={(id) => {
                setSelectedRecipients((prev) =>
                  prev.includes(id as string) ? prev.filter((gid) => gid !== id) : [...prev, id as string]
                );
              }}
              placeholder="Search users or groups..."
              loading={optionsLoading}
            />
          </>
        )}

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
  toggleBtn: {
    flex: 1,
    paddingVertical: 10,
    backgroundColor: "#f3f4f6",
    alignItems: "center",
    borderRadius: 8,
    marginHorizontal: 4,
    borderWidth: 1,
    borderColor: "#e5e7eb",
  },
  toggleActive: {
    backgroundColor: "#d1fae5",
    borderColor: "#10B981",
  },
  toggleText: {
    fontWeight: "600",
    color: "#6b7280",
  },
  toggleTextActive: {
    color: "#065f46",
  },
});
