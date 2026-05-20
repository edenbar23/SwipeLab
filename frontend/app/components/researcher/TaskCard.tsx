import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';

type AdminTask = {
  taskId: number;
  status: "ACTIVE" | "PAUSED" | "ARCHIVED" | "PROCESSING";
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

const STATUS_CONFIG = {
  ACTIVE:     { label: 'Active',      color: '#10B981', bg: '#D1FAE5', icon: 'checkmark-circle' as const },
  PAUSED:     { label: 'Paused',      color: '#F59E0B', bg: '#FEF3C7', icon: 'pause-circle'     as const },
  ARCHIVED:   { label: 'Archived',    color: '#6B7280', bg: '#F3F4F6', icon: 'archive'           as const },
  PROCESSING: { label: 'Processing',  color: '#3B82F6', bg: '#DBEAFE', icon: 'cloud-download-outline' as const },
};

export default function TaskCard({ task, onPress, onEdit, onToggleStatus, onArchive }: Props) {
  const isActive = task.status === "ACTIVE";
  const isProcessing = task.status === "PROCESSING";
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const status = STATUS_CONFIG[task.status] ?? STATUS_CONFIG.PAUSED;

  const total = task.progress.totalImages;
  const classified = task.progress.imagesClassified;
  const progressPct = total > 0 ? Math.min(classified / total, 1) : 0;
  const progressLabel = total > 0 ? `${classified} / ${total}` : '–';

  // Light blue card tint
  const cardBg = isDark ? themeColors.card : '#F0F7FF';
  const accentBorderColor = '#3B82F6';

  return (
    <TouchableOpacity
      style={[
        styles.card,
        { backgroundColor: cardBg, borderColor: isDark ? themeColors.border : '#BFDBFE' },
        isProcessing && { opacity: 0.75 },
      ]}
      activeOpacity={isProcessing ? 1 : 0.82}
      onPress={isProcessing ? undefined : onPress}
    >
      {/* Left accent stripe */}
      <View style={[styles.accentStripe, { backgroundColor: accentBorderColor }]} />

      <View style={styles.body}>
        {/* Header row */}
        <View style={styles.header}>
          <Text
            style={[styles.title, { color: themeColors.text }]}
            numberOfLines={1}
          >
            {task.name}
          </Text>

          {/* Status badge */}
          <View style={[styles.badge, { backgroundColor: status.bg }]}>
            <Ionicons name={status.icon} size={12} color={status.color} style={{ marginRight: 3 }} />
            <Text style={[styles.badgeText, { color: status.color }]}>{status.label}</Text>
          </View>
        </View>

        {/* Species */}
        <View style={styles.metaRow}>
          <Ionicons name="leaf-outline" size={13} color="#3B82F6" style={{ marginRight: 4 }} />
          <Text style={[styles.meta, { color: themeColors.textSecondary }]} numberOfLines={1}>
            {task.targetSpecies && task.targetSpecies.length > 0
              ? task.targetSpecies.map(s => s.commonName || s.name).join(', ')
              : 'No species assigned'}
          </Text>
        </View>

        {/* Progress bar */}
        <View style={styles.progressSection}>
          <View style={styles.progressLabelRow}>
            <Ionicons name="stats-chart-outline" size={13} color="#3B82F6" style={{ marginRight: 4 }} />
            <Text style={[styles.meta, { color: themeColors.textSecondary }]}>
              Progress: {progressLabel}
            </Text>
            <Text style={[styles.progressPct, { color: '#3B82F6' }]}>
              {Math.round(progressPct * 100)}%
            </Text>
          </View>
          <View style={[styles.progressTrack, { backgroundColor: isDark ? '#3a3a5a' : '#DBEAFE' }]}>
            <View
              style={[
                styles.progressFill,
                { width: `${progressPct * 100}%` as any },
              ]}
            />
          </View>
        </View>

        {/* Divider */}
        <View style={[styles.divider, { backgroundColor: isDark ? themeColors.border : '#DBEAFE' }]} />

        {/* Action row */}
        <View style={styles.actions}>
          {/* Toggle status */}
          {isProcessing ? (
            <View style={styles.actionChip}>
              <Ionicons name="cloud-download-outline" size={14} color="#3B82F6" />
              <Text style={[styles.actionChipText, { color: '#3B82F6' }]}>Importing…</Text>
            </View>
          ) : (
            <TouchableOpacity
              style={[
                styles.actionChip,
                { backgroundColor: isActive ? '#FEF3C7' : '#D1FAE5' },
              ]}
              onPress={(e) => { e.stopPropagation(); onToggleStatus(); }}
            >
              <Ionicons
                name={isActive ? 'pause-circle-outline' : 'play-circle-outline'}
                size={15}
                color={isActive ? '#F59E0B' : '#10B981'}
              />
              <Text style={[styles.actionChipText, { color: isActive ? '#F59E0B' : '#10B981' }]}>
                {isActive ? 'Pause' : 'Resume'}
              </Text>
            </TouchableOpacity>
          )}

          <View style={styles.iconActions}>
            <TouchableOpacity
              disabled={isProcessing}
              style={styles.iconBtn}
              onPress={(e) => { e.stopPropagation(); onEdit(); }}
            >
              <Ionicons name="create-outline" size={20} color="#3B82F6" />
            </TouchableOpacity>

            <TouchableOpacity
              disabled={isProcessing}
              style={styles.iconBtn}
              onPress={(e) => { e.stopPropagation(); onArchive(); }}
            >
              <Ionicons name="archive-outline" size={20} color={themeColors.textSecondary} />
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    borderRadius: 14,
    marginBottom: 12,
    borderWidth: 1,
    overflow: 'hidden',
    shadowColor: '#3B82F6',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  accentStripe: {
    width: 4,
    backgroundColor: '#3B82F6',
  },
  body: {
    flex: 1,
    padding: 14,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
    gap: 8,
  },
  title: {
    fontSize: 16,
    fontWeight: '700',
    flex: 1,
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 20,
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '700',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  meta: {
    fontSize: 13,
    flex: 1,
  },
  progressSection: {
    marginBottom: 10,
  },
  progressLabelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 5,
  },
  progressPct: {
    fontSize: 12,
    fontWeight: '700',
    marginLeft: 'auto',
  },
  progressTrack: {
    height: 6,
    borderRadius: 3,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#3B82F6',
    borderRadius: 3,
  },
  divider: {
    height: 1,
    marginBottom: 10,
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  actionChip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 20,
    gap: 4,
  },
  actionChipText: {
    fontSize: 12,
    fontWeight: '600',
  },
  iconActions: {
    flexDirection: 'row',
    gap: 4,
  },
  iconBtn: {
    padding: 6,
    borderRadius: 8,
  },
});
