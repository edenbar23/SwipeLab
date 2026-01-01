import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from "react-native";

type AdminTask = {
  taskId: number;
  status: "ACTIVE" | "PAUSED" | "ARCHIVED";
  name: string;
  targetSpecies: {
    commonName: string;
  }[];
  progress: {
    totalImages: number;
    imagesClassified: number;
  };
};

type Props = {
  task: AdminTask;
  onPress: () => void;
  onEdit: () => void;
  onToggleStatus: () => void;
  onArchive: () => void;
};

export default function TaskCard({
  task,
  onPress,
  onEdit,
  onToggleStatus,
  onArchive,
}: Props) {
  const isActive = task.status === "ACTIVE";

  return (
    <TouchableOpacity
      style={styles.card}
      activeOpacity={0.85}
      onPress={onPress}
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title} numberOfLines={1}>
          {task.name}
        </Text>

        <TouchableOpacity
          onPress={(e) => {
            e.stopPropagation();
            onToggleStatus();
          }}
        >
          <Ionicons
            name={isActive ? "pause-circle" : "play-circle"}
            size={26}
            color={isActive ? "#F59E0B" : "#10B981"}
          />
        </TouchableOpacity>
      </View>

      <Text style={styles.meta}>
        Species: {task.targetSpecies[0]?.commonName}
      </Text>

      <Text style={styles.meta}>
        Progress: {task.progress.imagesClassified} /{" "}
        {task.progress.totalImages}
      </Text>

      {/* Actions */}
      <View style={styles.actions}>
        <TouchableOpacity
          onPress={(e) => {
            e.stopPropagation();
            onEdit();
          }}
        >
          <Ionicons name="create-outline" size={22} />
        </TouchableOpacity>

        <TouchableOpacity
          onPress={(e) => {
            e.stopPropagation();
            onArchive();
          }}
        >
          <Ionicons name="archive-outline" size={22} />
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#F8FAFC",
    borderRadius: 14,
    padding: 14,
    marginBottom: 12,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  title: {
    fontSize: 16,
    fontWeight: "700",
    flex: 1,
    marginRight: 8,
  },
  meta: {
    fontSize: 13,
    color: "#475569",
    marginTop: 4,
  },
  actions: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 18,
    marginTop: 10,
  },
});
