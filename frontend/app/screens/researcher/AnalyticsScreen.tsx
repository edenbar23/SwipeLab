// Researcher Analytics Screen — Issue #221, #257
// Overview + per-task drill-down + CSV export.
import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '@/stores/themeStore';
import { useQueryClient } from '@tanstack/react-query';
import ScreenHeaderLayout from '@/components/layout/ScreenHeaderLayout';
import MetricCard from '@/components/researcher/MetricCard';
import TimeWindowCards from '@/components/researcher/TimeWindowCards';
import ConfidenceTrendChart from '@/components/researcher/ConfidenceTrendChart';
import LabelDistributionBar from '@/components/researcher/LabelDistributionBar';
import {
  useAnalyticsOverview,
  useAnalyticsTop,
  useAnalyticsTask,
  useAdminTasks,
  QUERY_KEYS,
} from '@/api/queries';
import type { UserPerformance, TaskAnalytics } from '@/types/analyticsTypes';
import ExportModal from '@/components/researcher/ExportModal';

type Tab = 'overview' | 'tasks';

export default function AnalyticsScreen({ navigation }: any) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [exportModalVisible, setExportModalVisible] = useState(false);

  // ─── Data ─────────────────────────────────────────────────────────────────
  const { data: overview, isLoading: overviewLoading, refetch: refetchOverview } =
    useAnalyticsOverview();

  const { data: topPerformers = [], isLoading: topLoading, refetch: refetchTop } =
    useAnalyticsTop(5);

  const { data: tasks = [], isLoading: tasksLoading, refetch: refetchTasks } =
    useAdminTasks();

  // Fetch selected task analytics lazily (only when a task is selected)
  const { data: taskAnalytics, isLoading: taskAnalyticsLoading } =
    useAnalyticsTask(selectedTaskId ?? 0);

  // ─── Refresh ──────────────────────────────────────────────────────────────
  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      if (activeTab === 'overview') {
        await Promise.all([refetchOverview(), refetchTop()]);
      } else {
        await refetchTasks();
        if (selectedTaskId) {
          await queryClient.invalidateQueries({
            queryKey: QUERY_KEYS.analyticsTasks(selectedTaskId),
          });
        }
      }
    } catch {
      Alert.alert('Error', 'Failed to refresh analytics');
    }
    setRefreshing(false);
  };

  // ─── Helpers ──────────────────────────────────────────────────────────────
  const formatMs = (ms: number | null | undefined): string => {
    if (!ms) return '–';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  };

  // ─── Tabs ─────────────────────────────────────────────────────────────────
  const tabBar = (
    <View style={[styles.tabBar, { borderColor: isDark ? themeColors.border : '#DBEAFE' }]}>
      {(['overview', 'tasks'] as Tab[]).map((tab) => {
        const isActive = activeTab === tab;
        return (
          <TouchableOpacity
            key={tab}
            style={[
              styles.tab,
              isActive && { borderBottomColor: '#3B82F6', borderBottomWidth: 2 },
            ]}
            onPress={() => {
              setActiveTab(tab);
              if (tab === 'overview') setSelectedTaskId(null);
            }}
          >
            <Text
              style={[
                styles.tabText,
                { color: isActive ? '#3B82F6' : themeColors.textSecondary },
              ]}
            >
              {tab === 'overview' ? '🌐 Overview' : '📋 Tasks'}
            </Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );

  // ─── Overview Tab ─────────────────────────────────────────────────────────
  const renderOverview = () => {
    if (overviewLoading) {
      return (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color="#3B82F6" />
          <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
            Loading overview…
          </Text>
        </View>
      );
    }

    if (!overview) {
      return (
        <View style={styles.centered}>
          <Ionicons name="bar-chart-outline" size={48} color="#BFDBFE" />
          <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
            No overview data available
          </Text>
        </View>
      );
    }

    return (
      <>
        {/* ── Activity windows ─────────────────────────────── */}
        <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
          📊 Activity
        </Text>
        <TimeWindowCards
          today={overview.today}
          thisWeek={overview.thisWeek}
          thisMonth={overview.thisMonth}
        />

        {/* ── Platform Totals ───────────────────────────────── */}
        <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
          🔢 Platform Totals
        </Text>
        <View style={styles.row}>
          <View style={styles.halfWidth}>
            <MetricCard
              label="Total Users"
              value={overview.totals.totalUsers.toLocaleString()}
              variant="primary"
            />
          </View>
          <View style={styles.halfWidth}>
            <MetricCard
              label="Active Tasks"
              value={overview.totals.activeTasks.toLocaleString()}
              variant="success"
            />
          </View>
        </View>
        <View style={styles.row}>
          <View style={styles.halfWidth}>
            <MetricCard
              label="Total Classifications"
              value={overview.totals.totalClassifications.toLocaleString()}
              variant="primary"
            />
          </View>
          <View style={styles.halfWidth}>
            <MetricCard
              label="Total Images"
              value={overview.totals.totalImages.toLocaleString()}
              variant="primary"
            />
          </View>
        </View>

        {/* ── Confidence Trend ─────────────────────────────── */}
        <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
          📈 Credibility Trend
        </Text>
        <ConfidenceTrendChart data={overview.confidenceTrend} />

        {/* ── Label Distribution ────────────────────────────── */}
        <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
          🏷️ Label Distribution (30d)
        </Text>
        <LabelDistributionBar data={overview.labelDistribution} />

        {/* ── Top Performers ───────────────────────────────── */}
        <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
          👥 Top Performers
        </Text>
        {topLoading ? (
          <ActivityIndicator color="#3B82F6" style={{ marginVertical: 12 }} />
        ) : topPerformers.length === 0 ? (
          <View style={styles.emptyCard}>
            <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
              No performers data yet
            </Text>
          </View>
        ) : (
          (topPerformers as UserPerformance[]).map((user, index) => (
            <View
              key={user.username}
              style={[
                styles.userCard,
                {
                  backgroundColor: isDark ? themeColors.card : '#F8FAFF',
                  borderColor: isDark ? themeColors.border : '#DBEAFE',
                },
              ]}
            >
              <View style={styles.rank}>
                <Text style={styles.rankText}>#{index + 1}</Text>
              </View>
              <View style={styles.userInfo}>
                <Text style={[styles.userName, { color: themeColors.text }]}>
                  {user.displayName ?? user.username}
                </Text>
                <Text style={[styles.userStats, { color: themeColors.textSecondary }]}>
                  {user.totalClassifications} classifications •{' '}
                  {(user.goldAccuracy ?? 0).toFixed(1)}% accuracy
                </Text>
              </View>
              <View style={styles.credBox}>
                <Text style={[styles.credScore, { color: '#10B981' }]}>
                  {/* credibilityScore is 0–100 from the backend — no conversion needed */}
                  {Math.round(user.credibilityScore ?? 0)}
                </Text>
                <Text style={[styles.credLabel, { color: themeColors.textSecondary }]}>
                  / 100
                </Text>
              </View>
            </View>
          ))
        )}
      </>
    );
  };

  // ─── Tasks Tab ────────────────────────────────────────────────────────────
  const renderTasks = () => {
    if (tasksLoading) {
      return (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color="#3B82F6" />
          <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
            Loading tasks…
          </Text>
        </View>
      );
    }

    if (tasks.length === 0) {
      return (
        <View style={styles.centered}>
          <Ionicons name="clipboard-outline" size={48} color="#BFDBFE" />
          <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
            No tasks yet
          </Text>
        </View>
      );
    }

    return (
      <>
        <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
          Select a task to view its analytics
        </Text>

        {/* Task list */}
        {tasks.map((task: any) => {
          const isSelected = selectedTaskId === task.taskId;
          return (
            <View key={task.taskId}>
              <TouchableOpacity
                style={[
                  styles.taskRow,
                  {
                    backgroundColor: isSelected
                      ? isDark ? '#1e3a5f' : '#DBEAFE'
                      : isDark ? themeColors.card : '#F0F7FF',
                    borderColor: isSelected ? '#3B82F6' : isDark ? themeColors.border : '#BFDBFE',
                  },
                ]}
                onPress={() =>
                  setSelectedTaskId(isSelected ? null : task.taskId)
                }
              >
                <View style={styles.taskRowLeft}>
                  <View
                    style={[
                      styles.statusDot,
                      {
                        backgroundColor:
                          task.status === 'ACTIVE'
                            ? '#10B981'
                            : task.status === 'PAUSED'
                            ? '#F59E0B'
                            : '#9CA3AF',
                      },
                    ]}
                  />
                  <View>
                    <Text
                      style={[styles.taskName, { color: themeColors.text }]}
                      numberOfLines={1}
                    >
                      {task.name ?? task.title ?? `Task #${task.taskId}`}
                    </Text>
                    <Text style={[styles.taskMeta, { color: themeColors.textSecondary }]}>
                      {task.status} · {task.progress?.imagesClassified ?? 0}/
                      {task.progress?.totalImages ?? 0} images
                    </Text>
                  </View>
                </View>
                <Ionicons
                  name={isSelected ? 'chevron-up' : 'chevron-down'}
                  size={18}
                  color="#3B82F6"
                />
              </TouchableOpacity>

              {/* Expanded analytics for selected task */}
              {isSelected && (
                <View
                  style={[
                    styles.taskDetail,
                    {
                      backgroundColor: isDark ? themeColors.background : '#FAFCFF',
                      borderColor: '#3B82F6',
                    },
                  ]}
                >
                  {taskAnalyticsLoading ? (
                    <ActivityIndicator color="#3B82F6" style={{ marginVertical: 16 }} />
                  ) : !taskAnalytics ? (
                    <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
                      No analytics data for this task
                    </Text>
                  ) : (
                    <TaskAnalyticsDetail
                      analytics={taskAnalytics as TaskAnalytics}
                      themeColors={themeColors}
                      isDark={isDark}
                      formatMs={formatMs}
                    />
                  )}
                </View>
              )}
            </View>
          );
        })}
      </>
    );
  };

  // ─── Render ───────────────────────────────────────────────────────────────
  return (
    <ScreenHeaderLayout
      leftIcon={require('../../../assets/images/stats.png')}
      leftTitle="Analytics"
      rightIcon={require('../../../assets/images/tasks_mgmt.png')}
      rightTitle="Tasks"
      onRightPress={() => navigation.navigate('TasksManagement')}
    >
      <ScrollView
        contentContainerStyle={[
          styles.container,
          { backgroundColor: themeColors.background },
        ]}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
      >
        {tabBar}

        {/* Export to CSV button */}
        <TouchableOpacity
          style={[
            styles.exportCsvButton,
            {
              backgroundColor: isDark ? '#1e3a5f' : '#EFF6FF',
              borderColor: '#3B82F6',
            },
          ]}
          onPress={() => setExportModalVisible(true)}
        >
          <Ionicons name="download-outline" size={18} color="#3B82F6" />
          <Text style={styles.exportCsvText}>Export to CSV</Text>
        </TouchableOpacity>

        {activeTab === 'overview' ? renderOverview() : renderTasks()}
      </ScrollView>

      <ExportModal
        visible={exportModalVisible}
        onClose={() => setExportModalVisible(false)}
      />
    </ScreenHeaderLayout>
  );
}

// ─── Task Analytics Detail sub-component ─────────────────────────────────────

function TaskAnalyticsDetail({
  analytics,
  themeColors,
  isDark,
  formatMs,
}: {
  analytics: TaskAnalytics;
  themeColors: any;
  isDark: boolean;
  formatMs: (ms: number | null | undefined) => string;
}) {
  const p = analytics.progress;
  const c = analytics.consensus;
  const part = analytics.participation;
  const q = analytics.quality;

  return (
    <View style={{ gap: 8 }}>
      {/* Progress */}
      <Text style={[styles.detailSection, { color: themeColors.textSecondary }]}>
        📊 Progress
      </Text>
      <View style={styles.row}>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Completion"
            value={`${(p?.percentComplete ?? 0).toFixed(1)}%`}
            variant="primary"
          />
        </View>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Total Images"
            value={p?.totalImages ?? 0}
            variant="primary"
          />
        </View>
      </View>
      <View style={styles.row}>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Classified"
            value={p?.imagesClassified ?? 0}
            subtitle={`${(p?.totalImages ?? 0) - (p?.imagesClassified ?? 0)} remaining`}
            variant="success"
          />
        </View>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Completed"
            value={p?.completedImages ?? 0}
            subtitle="Reached consensus"
            variant="success"
          />
        </View>
      </View>

      {/* Data Quality */}
      <Text style={[styles.detailSection, { color: themeColors.textSecondary }]}>
        ✨ Data Quality
      </Text>
      <View style={styles.row}>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Avg Consensus"
            value={`${(c?.overallAverage ?? 0).toFixed(1)}%`}
            variant="success"
          />
        </View>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Low Consensus"
            value={c?.lowConsensusImages ?? 0}
            subtitle="Below threshold"
            variant="warning"
          />
        </View>
      </View>
      {q && (
        <View style={styles.row}>
          <View style={styles.halfWidth}>
            <MetricCard
              label="Avg Credibility"
              value={`${(q.averageCredibility ?? 0).toFixed(0)}/100`}
              variant="success"
            />
          </View>
          <View style={styles.halfWidth}>
            <MetricCard
              label="Low Quality Users"
              value={q.lowQualityUsers ?? 0}
              variant={q.lowQualityUsers && q.lowQualityUsers > 0 ? 'warning' : 'success'}
            />
          </View>
        </View>
      )}

      {/* Participation */}
      <Text style={[styles.detailSection, { color: themeColors.textSecondary }]}>
        👥 Participation
      </Text>
      <MetricCard
        label="Active Users"
        value={part?.activeUsers ?? 0}
        subtitle={`${part?.totalClassifications ?? 0} total classifications`}
        variant="primary"
      />
      <View style={styles.row}>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Avg / User"
            value={part?.averageClassificationsPerUser ?? 0}
            variant="primary"
          />
        </View>
        <View style={styles.halfWidth}>
          <MetricCard
            label="Median Response"
            value={formatMs(part?.medianResponseTimeMs)}
            variant="primary"
          />
        </View>
      </View>
    </View>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 40,
  },
  tabBar: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    marginBottom: 20,
  },
  tab: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
  },
  tabText: {
    fontSize: 14,
    fontWeight: '700',
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginTop: 4,
    marginBottom: 10,
  },
  detailSection: {
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
    marginTop: 4,
    marginBottom: 2,
  },
  row: {
    flexDirection: 'row',
    gap: 12,
  },
  halfWidth: {
    flex: 1,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    paddingTop: 60,
    paddingBottom: 40,
  },
  stateText: {
    fontSize: 14,
    textAlign: 'center',
  },
  emptyCard: {
    padding: 20,
    alignItems: 'center',
    borderRadius: 12,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: '#DBEAFE',
    marginBottom: 12,
  },
  // Top performers
  userCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 8,
  },
  rank: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: '#3B82F6',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  rankText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 13,
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 14,
    fontWeight: '700',
    marginBottom: 2,
  },
  userStats: {
    fontSize: 12,
  },
  credBox: {
    alignItems: 'center',
  },
  credScore: {
    fontSize: 20,
    fontWeight: '800',
  },
  credLabel: {
    fontSize: 9,
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },
  // Tasks tab
  taskRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 14,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 8,
  },
  taskRowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  statusDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  taskName: {
    fontSize: 14,
    fontWeight: '700',
    maxWidth: 220,
  },
  taskMeta: {
    fontSize: 11,
    marginTop: 2,
  },
  taskDetail: {
    borderWidth: 1,
    borderTopWidth: 0,
    borderRadius: 12,
    borderTopLeftRadius: 0,
    borderTopRightRadius: 0,
    padding: 14,
    marginBottom: 12,
    marginTop: -8,
  },
  // Export button
  exportCsvButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 16,
  },
  exportCsvText: {
    color: '#3B82F6',
    fontSize: 14,
    fontWeight: '700',
  },
});