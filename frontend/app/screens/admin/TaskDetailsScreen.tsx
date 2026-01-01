import { Ionicons } from "@expo/vector-icons";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React, { useEffect, useState } from "react";
import { Image, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";

import { apiFetch } from "../../api/apiFetch";
import { AdminStackParamList } from "../../navigation/adminStack.types";

type Props = NativeStackScreenProps<AdminStackParamList, "TaskDetails">;

type TaskDetails = {
  taskId: number;
  status: "ACTIVE" | "PAUSED" | "ARCHIVED";
  name: string;
  description: string;
  targetSpecies: {
    name: string;
    commonName: string;
    referenceImages: { contentType: string; data: string; caption: string }[];
  }[];
  experiments: number[];
  recipientGroups: number[];
  progress: { totalImages: number; imagesClassified: number };
  minClassificationsPerImage: number;
  consensusThreshold: number;
};

export default function TaskDetailsScreen({ route, navigation }: Props) {
  const { taskId } = route.params;
  const [task, setTask] = useState<TaskDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchTask() {
      try {
        const res = await apiFetch(`/api/v1/dashboard/tasks/${taskId}`, { method: "GET" });
        const data: TaskDetails = await res.json();
        setTask(data);
      } catch (err: any) {
        console.error(err);
        setError(err.message || "Failed to fetch task");
      } finally {
        setLoading(false);
      }
    }
    fetchTask();
  }, [taskId]);

  if (loading) return <Text style={styles.loading}>Loading...</Text>;
  if (error) return <Text style={styles.error}>Error: {error}</Text>;
  if (!task) return <Text style={styles.error}>Task not found</Text>;

  const isActive = task.status === "ACTIVE";

  return (
    <ScrollView contentContainerStyle={styles.container}>
      {/* Task Info */}
      <View style={styles.card}>
        
        <Text style={styles.title}>{task.name}</Text>
        <Text style={styles.description}>{task.description}</Text>
        
        
        {/* Edit / Pause / Archive buttons */}
        <View style={styles.actions}>
          <TouchableOpacity
            onPress={() => navigation.navigate("EditTask", { taskId })}
          >
            <Ionicons name="create-outline" size={26} color="#2563EB" />
          </TouchableOpacity>

          <TouchableOpacity onPress={() => console.log("Toggle Pause/Resume", taskId)}>
            <Ionicons
              name={isActive ? "pause-circle" : "play-circle"}
              size={26}
              color={isActive ? "#F59E0B" : "#10B981"}
            />
          </TouchableOpacity>

          <TouchableOpacity onPress={() => console.log("Archive task", taskId)}>
            <Ionicons name="archive-outline" size={26} color="#EF4444" />
          </TouchableOpacity>
        </View>

        <View style={styles.infoRow}>
          <Text style={styles.label}>Status:</Text>
          <Text style={styles.value}>{task.status}</Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={styles.label}>Progress:</Text>
          <Text style={styles.value}>
            {task.progress.imagesClassified} / {task.progress.totalImages} images classified
          </Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={styles.label}>Min classifications:</Text>
          <Text style={styles.value}>{task.minClassificationsPerImage}</Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={styles.label}>Consensus threshold:</Text>
          <Text style={styles.value}>{task.consensusThreshold}%</Text>
        </View>

      </View>

      {/* Target Species */}
      <Text style={styles.sectionTitle}>Target Species</Text>
      {task.targetSpecies.map((species) => (
        <View key={species.name} style={styles.speciesCard}>
          <Text style={styles.speciesName}>
            {species.commonName} ({species.name})
          </Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginTop: 8 }}>
            {species.referenceImages.map((img, idx) => (
              <View key={idx} style={styles.imageCard}>
                <Image
                  source={{ uri: `data:${img.contentType};base64,${img.data}` }}
                  style={styles.image}
                />
                <Text style={styles.imageCaption}>{img.caption}</Text>
              </View>
            ))}
          </ScrollView>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 40,
    backgroundColor: "#f8f9fa",
  },
  loading: {
    padding: 16,
    fontSize: 16,
    textAlign: "center",
  },
  error: {
    padding: 16,
    fontSize: 16,
    color: "red",
    textAlign: "center",
  },
  card: {
    backgroundColor: "#fff",
    borderRadius: 14,
    padding: 16,
    marginBottom: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  title: {
    fontSize: 20,
    fontWeight: "700",
    marginBottom: 8,
  },
  description: {
    fontSize: 16,
    marginBottom: 12,
    color: "#555",
  },
  infoRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 6,
  },
  label: {
    fontWeight: "500",
    color: "#333",
  },
  value: {
    fontWeight: "400",
    color: "#555",
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "600",
    marginBottom: 8,
  },
  speciesCard: {
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 12,
    marginBottom: 12,
  },
  speciesName: {
    fontSize: 16,
    fontWeight: "500",
  },
  imageCard: {
    marginRight: 12,
    alignItems: "center",
  },
  image: {
    width: 120,
    height: 120,
    borderRadius: 8,
    backgroundColor: "#eee",
  },
  imageCaption: {
    fontSize: 12,
    marginTop: 4,
    color: "#555",
  },
  actions: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 18,
    marginTop: 12,
    marginBottom: 12,
  },
});
