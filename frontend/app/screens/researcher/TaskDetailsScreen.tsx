import { Ionicons } from "@expo/vector-icons";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React from "react";
import {
  ActivityIndicator,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

import { Colors } from '../../../constants/theme';
import { researcherStackParamList } from "../../navigation/researcherStack.types";
import { useThemeStore } from '../../stores/themeStore';
import { useTaskDetails, useExperiments, useUpdateTaskStatus } from "../../api/queries";

type Props = NativeStackScreenProps<researcherStackParamList, "TaskDetails">;

type TaskDetails = {
  taskId: number;
  status: "ACTIVE" | "PAUSED" | "ARCHIVED" | "PROCESSING";
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

const STATUS_CONFIG = {
  ACTIVE:     { label: 'Active',      color: '#10B981', bg: '#D1FAE5', icon: 'checkmark-circle'       as const },
  PAUSED:     { label: 'Paused',      color: '#F59E0B', bg: '#FEF3C7', icon: 'pause-circle'            as const },
  ARCHIVED:   { label: 'Archived',    color: '#6B7280', bg: '#F3F4F6', icon: 'archive'                 as const },
  PROCESSING: { label: 'Processing',  color: '#3B82F6', bg: '#DBEAFE', icon: 'cloud-download-outline'  as const },
};

export default function TaskDetailsScreen({ route, navigation }: Props) {
  const { taskId } = route.params;
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const { data: task, isLoading: loading, error } = useTaskDetails(taskId);
  const { data: experimentsList } = useExperiments();
  const { mutate: updateStatus } = useUpdateTaskStatus();

  // ── Loading state ──────────────────────────────────────────────────────────
  if (loading) {
    return (
      <View style={[styles.centered, { backgroundColor: themeColors.background }]}>
        <ActivityIndicator size="large" color="#3B82F6" />
        <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>Loading task…</Text>
      </View>
    );
  }

  if (error || !task) {
    return (
      <View style={[styles.centered, { backgroundColor: themeColors.background }]}>
        <Ionicons name="alert-circle-outline" size={48} color="#EF4444" />
        <Text style={[styles.stateText, { color: '#EF4444' }]}>
          {error ? (error as Error).message : 'Task not found'}
        </Text>
      </View>
    );
  }

  const isActive = task.status === 'ACTIVE';
  const status = STATUS_CONFIG[task.status as keyof typeof STATUS_CONFIG] ?? STATUS_CONFIG.PAUSED;

  const total = task.progress.totalImages;
  const classified = task.progress.imagesClassified;
  const progressPct = total > 0 ? Math.min(classified / total, 1) : 0;

  const cardBg    = isDark ? themeColors.card : '#F0F7FF';
  const borderCol = isDark ? themeColors.border : '#BFDBFE';

  return (
    <ScrollView
      style={{ backgroundColor: themeColors.background }}
      contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]}
      showsVerticalScrollIndicator={false}
    >
      {/* ── Back button ─────────────────────────────────────────────────────── */}
      <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
        <Ionicons name="chevron-back" size={18} color="#3B82F6" />
        <Text style={styles.backText}>Back to Tasks</Text>
      </TouchableOpacity>

      {/* ── Hero card ───────────────────────────────────────────────────────── */}
      <View style={[styles.heroCard, { backgroundColor: cardBg, borderColor: borderCol }]}>
        {/* Blue stripe */}
        <View style={styles.heroStripe} />

        <View style={styles.heroBody}>
          {/* Title + status badge */}
          <View style={styles.heroHeader}>
            <Text style={[styles.heroTitle, { color: themeColors.text }]} numberOfLines={2}>
              {task.name}
            </Text>
            <View style={[styles.badge, { backgroundColor: status.bg }]}>
              <Ionicons name={status.icon} size={12} color={status.color} style={{ marginRight: 3 }} />
              <Text style={[styles.badgeText, { color: status.color }]}>{status.label}</Text>
            </View>
          </View>

          {/* Description */}
          {!!task.description && (
            <Text style={[styles.description, { color: themeColors.textSecondary }]}>
              {task.description}
            </Text>
          )}

          {/* ── Progress bar ─────────────────────────────────────────────────── */}
          <View style={styles.progressSection}>
            <View style={styles.progressLabelRow}>
              <Ionicons name="stats-chart-outline" size={13} color="#3B82F6" style={{ marginRight: 4 }} />
              <Text style={[styles.progressLabel, { color: themeColors.textSecondary }]}>
                Classification Progress
              </Text>
              <Text style={styles.progressPct}>{Math.round(progressPct * 100)}%</Text>
            </View>
            <View style={[styles.progressTrack, { backgroundColor: isDark ? '#3a3a5a' : '#DBEAFE' }]}>
              <View style={[styles.progressFill, { width: `${progressPct * 100}%` as any }]} />
            </View>
            <Text style={[styles.progressSub, { color: themeColors.textSecondary }]}>
              {classified} of {total} images classified
            </Text>
          </View>

          {/* ── Stat chips ───────────────────────────────────────────────────── */}
          <View style={styles.statsRow}>
            <StatChip
              icon="layers-outline"
              label="Min. Classifications"
              value={String(task.minClassificationsPerImage)}
              isDark={isDark}
              themeSecondary={themeColors.textSecondary}
            />
            <StatChip
              icon="checkmark-done-outline"
              label="Consensus"
              value={`${task.consensusThreshold}%`}
              isDark={isDark}
              themeSecondary={themeColors.textSecondary}
            />
            <StatChip
              icon="leaf-outline"
              label="Species"
              value={String((task.targetSpecies || []).length)}
              isDark={isDark}
              themeSecondary={themeColors.textSecondary}
            />
          </View>

          {/* ── Action buttons ───────────────────────────────────────────────── */}
          <View style={styles.actionsRow}>
            <TouchableOpacity
              style={[styles.actionBtn, { backgroundColor: isDark ? '#1e3a5f' : '#DBEAFE', borderColor: '#3B82F6' }]}
              onPress={() => navigation.navigate('EditTask', { taskId })}
            >
              <Ionicons name="create-outline" size={16} color="#3B82F6" />
              <Text style={[styles.actionBtnText, { color: '#3B82F6' }]}>Edit</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.actionBtn,
                {
                  backgroundColor: isActive ? (isDark ? '#3d2e0a' : '#FEF3C7') : (isDark ? '#0d2e1c' : '#D1FAE5'),
                  borderColor: isActive ? '#F59E0B' : '#10B981',
                },
              ]}
              onPress={() => updateStatus({ taskId, action: isActive ? 'pause' : 'activate' })}
            >
              <Ionicons
                name={isActive ? 'pause-circle-outline' : 'play-circle-outline'}
                size={16}
                color={isActive ? '#F59E0B' : '#10B981'}
              />
              <Text style={[styles.actionBtnText, { color: isActive ? '#F59E0B' : '#10B981' }]}>
                {isActive ? 'Pause' : 'Resume'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.actionBtn, { backgroundColor: isDark ? '#2d1515' : '#FEE2E2', borderColor: '#EF4444' }]}
              onPress={() => updateStatus({ taskId, action: 'archive' })}
            >
              <Ionicons name="archive-outline" size={16} color="#EF4444" />
              <Text style={[styles.actionBtnText, { color: '#EF4444' }]}>Archive</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* ── Experiments ─────────────────────────────────────────────────────── */}
      {task.experiments?.length > 0 && (
        <>
          <SectionHeader icon="flask-outline" title="Experiments" />
          <View style={[styles.infoCard, { backgroundColor: cardBg, borderColor: borderCol }]}>
            {task.experiments.map((expId: number) => {
              const exp = experimentsList?.find((e: any) => e.id === expId);
              const label = exp ? exp.name : `Experiment #${expId}`;
              return (
                <View key={expId} style={[styles.listRow, { borderBottomColor: borderCol }]}>
                  <Ionicons name="beaker-outline" size={15} color="#3B82F6" style={{ marginRight: 8 }} />
                  <Text style={[styles.listRowText, { color: themeColors.text }]}>{label}</Text>
                </View>
              );
            })}
          </View>
        </>
      )}

      {/* ── Target Species ──────────────────────────────────────────────────── */}
      {(task.targetSpecies || []).length > 0 && (
        <>
          <SectionHeader icon="leaf-outline" title="Target Species" />
          {(task.targetSpecies || []).map((species: any) => (
            <View
              key={species.name || species.commonName}
              style={[styles.speciesCard, { backgroundColor: cardBg, borderColor: borderCol }]}
            >
              {/* Species header */}
              <View style={styles.speciesHeader}>
                <View style={styles.speciesIconWrap}>
                  <Ionicons name="leaf" size={18} color="#3B82F6" />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.speciesCommon, { color: themeColors.text }]}>
                    {species.commonName || species.name}
                  </Text>
                  {species.commonName && species.name && (
                    <Text style={[styles.speciesSci, { color: themeColors.textSecondary }]}>
                      {species.name}
                    </Text>
                  )}
                </View>
                <View style={[styles.imgCountBadge, { backgroundColor: isDark ? '#1e3a5f' : '#DBEAFE' }]}>
                  <Text style={styles.imgCountText}>{(species.referenceImages || []).length} imgs</Text>
                </View>
              </View>

              {/* Reference images */}
              {(species.referenceImages || []).length > 0 ? (
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  style={{ marginTop: 12 }}
                  contentContainerStyle={{ gap: 10 }}
                >
                  {(species.referenceImages || []).map((img: any, idx: number) => (
                    <View key={idx} style={styles.imageCard}>
                      <Image
                        source={{ uri: `data:${img.contentType};base64,${img.data}` }}
                        style={[styles.image, { borderColor: borderCol }]}
                      />
                      {!!img.caption && (
                        <Text style={[styles.imageCaption, { color: themeColors.textSecondary }]} numberOfLines={1}>
                          {img.caption}
                        </Text>
                      )}
                    </View>
                  ))}
                </ScrollView>
              ) : (
                <View style={styles.noImages}>
                  <Ionicons name="image-outline" size={22} color="#BFDBFE" />
                  <Text style={[styles.noImagesText, { color: themeColors.textSecondary }]}>
                    No reference images
                  </Text>
                </View>
              )}
            </View>
          ))}
        </>
      )}
    </ScrollView>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function SectionHeader({ icon, title }: { icon: any; title: string }) {
  return (
    <View style={styles.sectionHeader}>
      <Ionicons name={icon} size={16} color="#3B82F6" style={{ marginRight: 6 }} />
      <Text style={styles.sectionTitle}>{title}</Text>
    </View>
  );
}

function StatChip({
  icon, label, value, isDark, themeSecondary,
}: {
  icon: any; label: string; value: string; isDark: boolean; themeSecondary: string;
}) {
  return (
    <View style={[styles.statChip, { backgroundColor: isDark ? '#1e3a5f' : '#EFF6FF', borderColor: isDark ? '#2a4a7f' : '#BFDBFE' }]}>
      <Ionicons name={icon} size={14} color="#3B82F6" style={{ marginBottom: 2 }} />
      <Text style={styles.statValue}>{value}</Text>
      <Text style={[styles.statLabel, { color: themeSecondary }]}>{label}</Text>
    </View>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 48,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  stateText: {
    fontSize: 14,
    textAlign: 'center',
  },

  // Back button
  backBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 14,
    gap: 4,
  },
  backText: {
    fontSize: 14,
    color: '#3B82F6',
    fontWeight: '500',
  },

  // Hero card
  heroCard: {
    flexDirection: 'row',
    borderRadius: 16,
    borderWidth: 1,
    overflow: 'hidden',
    marginBottom: 20,
    shadowColor: '#3B82F6',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  heroStripe: {
    width: 5,
    backgroundColor: '#3B82F6',
  },
  heroBody: {
    flex: 1,
    padding: 16,
  },
  heroHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    marginBottom: 8,
  },
  heroTitle: {
    fontSize: 20,
    fontWeight: '800',
    flex: 1,
    lineHeight: 26,
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 9,
    paddingVertical: 4,
    borderRadius: 20,
    marginTop: 2,
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '700',
  },
  description: {
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 16,
  },

  // Progress
  progressSection: {
    marginBottom: 16,
  },
  progressLabelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  progressLabel: {
    fontSize: 13,
    flex: 1,
  },
  progressPct: {
    fontSize: 13,
    fontWeight: '700',
    color: '#3B82F6',
  },
  progressTrack: {
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
    marginBottom: 4,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#3B82F6',
    borderRadius: 4,
  },
  progressSub: {
    fontSize: 11,
    textAlign: 'right',
  },

  // Stat chips
  statsRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  statChip: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 6,
    borderRadius: 12,
    borderWidth: 1,
    gap: 2,
  },
  statValue: {
    fontSize: 15,
    fontWeight: '800',
    color: '#3B82F6',
  },
  statLabel: {
    fontSize: 9,
    textAlign: 'center',
    fontWeight: '500',
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },

  // Action buttons
  actionsRow: {
    flexDirection: 'row',
    gap: 8,
  },
  actionBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 5,
    paddingVertical: 9,
    borderRadius: 10,
    borderWidth: 1,
  },
  actionBtnText: {
    fontSize: 12,
    fontWeight: '700',
  },

  // Section header
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
    marginTop: 4,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#3B82F6',
  },

  // Generic info card
  infoCard: {
    borderRadius: 14,
    borderWidth: 1,
    overflow: 'hidden',
    marginBottom: 20,
    shadowColor: '#3B82F6',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 5,
    elevation: 2,
  },
  listRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 11,
    paddingHorizontal: 14,
    borderBottomWidth: 1,
  },
  listRowText: {
    fontSize: 14,
    fontWeight: '500',
  },

  // Species card
  speciesCard: {
    borderRadius: 14,
    borderWidth: 1,
    padding: 14,
    marginBottom: 14,
    shadowColor: '#3B82F6',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.07,
    shadowRadius: 5,
    elevation: 2,
  },
  speciesHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  speciesIconWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#DBEAFE',
    alignItems: 'center',
    justifyContent: 'center',
  },
  speciesCommon: {
    fontSize: 15,
    fontWeight: '700',
  },
  speciesSci: {
    fontSize: 12,
    fontStyle: 'italic',
    marginTop: 1,
  },
  imgCountBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 12,
  },
  imgCountText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#3B82F6',
  },
  noImages: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingTop: 10,
  },
  noImagesText: {
    fontSize: 13,
  },
  imageCard: {
    alignItems: 'center',
    width: 110,
  },
  image: {
    width: 110,
    height: 110,
    borderRadius: 10,
    backgroundColor: '#DBEAFE',
    borderWidth: 1,
  },
  imageCaption: {
    fontSize: 11,
    marginTop: 5,
    textAlign: 'center',
    maxWidth: 110,
  },
});
