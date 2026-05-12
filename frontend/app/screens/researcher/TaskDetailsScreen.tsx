import { Ionicons } from "@expo/vector-icons";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React from "react";
import { Image, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";

import { Colors } from '../../../constants/theme';
import { apiFetch } from "../../api/apiFetch";
import { researcherStackParamList } from "../../navigation/researcherStack.types";
import { useThemeStore } from '../../stores/themeStore';
import { useTaskDetails, useExperiments, useUpdateTaskStatus } from "../../api/queries";


type Props = NativeStackScreenProps<researcherStackParamList, "TaskDetails">;

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
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  const { data: task, isLoading: loading, error } = useTaskDetails(taskId);
  const { data: experimentsList } = useExperiments();
  const { mutate: updateStatus } = useUpdateTaskStatus();

  if (loading) return <Text style={styles.loading}>Loading...</Text>;
  if (error) return <Text style={styles.error}>Error: {(error as Error).message || "Failed to fetch task"}</Text>;
  if (!task) return <Text style={styles.error}>Task not found</Text>;

  const isActive = task.status === "ACTIVE";

  return (
    <ScrollView
      style={{ backgroundColor: themeColors.background }}
      contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]}
      showsVerticalScrollIndicator={false}
    >
      {/* Task Info */}
      <View style={[styles.card, { backgroundColor: themeColors.card }]}>

        <Text style={[styles.title, { color: themeColors.text }]}>{task.name}</Text>
        <Text style={[styles.description, { color: themeColors.textSecondary }]}>{task.description}</Text>


        {/* Edit / Pause / Archive buttons */}
        <View style={styles.actions}>
          <TouchableOpacity
            onPress={() => navigation.navigate("EditTask", { taskId })}
          >
            <Ionicons name="create-outline" size={26} color="#2563EB" />
          </TouchableOpacity>

          <TouchableOpacity onPress={() => updateStatus({ taskId, action: isActive ? 'pause' : 'activate' })}>
            <Ionicons
              name={isActive ? "pause-circle" : "play-circle"}
              size={26}
              color={isActive ? "#F59E0B" : "#10B981"}
            />
          </TouchableOpacity>

          <TouchableOpacity onPress={() => updateStatus({ taskId, action: 'archive' })}>
            <Ionicons name="archive-outline" size={26} color="#EF4444" />
          </TouchableOpacity>
        </View>

        <View style={styles.infoRow}>
          <Text style={[styles.label, { color: themeColors.text }]}>Status:</Text>
          <Text style={[styles.value, { color: themeColors.textSecondary }]}>{task.status}</Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={[styles.label, { color: themeColors.text }]}>Progress:</Text>
          <Text style={[styles.value, { color: themeColors.textSecondary }]}>
            {task.progress.imagesClassified} / {task.progress.totalImages} images classified
          </Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={[styles.label, { color: themeColors.text }]}>Min classifications:</Text>
          <Text style={[styles.value, { color: themeColors.textSecondary }]}>{task.minClassificationsPerImage}</Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={[styles.label, { color: themeColors.text }]}>Consensus threshold:</Text>
          <Text style={[styles.value, { color: themeColors.textSecondary }]}>{task.consensusThreshold}%</Text>
        </View>

        {task.experiments?.length > 0 && (
          <View style={[styles.infoRow, { marginTop: 8 }]}>
            <Text style={[styles.label, { color: themeColors.text }]}>Experiments:</Text>
            <View style={{ flex: 1, alignItems: 'flex-end' }}>
              {task.experiments.map((expId: number) => {
                const exp = experimentsList?.find((e: any) => e.id === expId);
                const desc = exp ? exp.name : `ID: ${expId}`;
                return <Text key={expId} style={[styles.value, { color: themeColors.textSecondary, marginBottom: 2 }]}>{desc}</Text>;
              })}
            </View>
          </View>
        )}

      </View>

      {/* Target Species */}
      <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Target Species</Text>
      {(task.targetSpecies || []).map((species: any) => (
        <View key={species.name || species.commonName} style={[styles.speciesCard, { backgroundColor: themeColors.card }]}>
          <Text style={[styles.speciesName, { color: themeColors.text }]}>
            {species.commonName || species.name} {species.commonName && species.name ? `(${species.name})` : ''}
          </Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginTop: 8 }}>
            {(species.referenceImages || []).map((img: any, idx: number) => (
              <View key={idx} style={styles.imageCard}>
                <Image
                  source={{ uri: `data:${img.contentType};base64,${img.data}` }}
                  style={styles.image}
                />
                <Text style={[styles.imageCaption, { color: themeColors.textSecondary }]}>{img.caption}</Text>
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
