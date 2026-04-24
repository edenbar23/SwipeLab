import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
  StyleSheet,
  Text,
  TouchableOpacity,

  View
} from "react-native";
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';

type AdminTask = {
  taskId: number;
  status: "ACTIVE" | "PAUSED" | "ARCHIVED";
  name: string;
  targetSpecies: {
    name: string;
    commonName?: string;
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
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  return (
    <TouchableOpacity
      style={[styles.card, { backgroundColor: themeColors.card }]}
      activeOpacity={0.85}
      onPress={onPress}
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={[styles.title, { color: themeColors.text }]} numberOfLines={1}>
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

      <Text style={[styles.meta, { color: themeColors.textSecondary }]}>
        Species: {task.targetSpecies && task.targetSpecies.length > 0 
          ? task.targetSpecies.map(s => s.commonName || s.name).join(", ") 
          : "None"}
      </Text>

      <Text style={[styles.meta, { color: themeColors.textSecondary }]}>
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
          <Ionicons name="create-outline" size={22} color={themeColors.text} />
        </TouchableOpacity>

        <TouchableOpacity
          onPress={(e) => {
            e.stopPropagation();
            onArchive();
          }}
        >
          <Ionicons name="archive-outline" size={22} color={themeColors.text} />
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
