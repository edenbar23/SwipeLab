import { useNavigation } from '@react-navigation/native';
import React, { useCallback } from 'react';
import { useMyTasks, useAvailableTasks, useStatistics, useAssignTask } from "../../api/queries";
import { Alert, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../constants/theme';

import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import TaskCard from '../../components/user/TaskCard';
import { useThemeStore } from '../../stores/themeStore';
import { useSwipeStore } from '../../stores/swipeStore';


export default function UserMyTasksScreen() {
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const isDark = theme === 'dark';
    const { setActiveTaskId } = useSwipeStore();

    const { data: stats, isLoading: statsLoading, refetch: refetchStats } = useStatistics();
    const { data: tasks = [], isLoading: tasksLoading, refetch: refetchTasks } = useMyTasks();
    const { data: availableTasks = [], isLoading: availableLoading, refetch: refetchAvailable } = useAvailableTasks();

    const refreshing = statsLoading || tasksLoading || availableLoading;

    const loadData = useCallback(() => {
        refetchStats();
        refetchTasks();
        refetchAvailable();
    }, [refetchStats, refetchTasks, refetchAvailable]);

    const assignTask = useAssignTask();

    const handleAddTask = async (taskId: number) => {
        try {
            await assignTask.mutateAsync(taskId);
            // Cache is refreshed via onSuccess in useAssignTask; nothing more needed here.
        } catch (err: any) {
            Alert.alert('Could not add task', err?.message ?? 'An unexpected error occurred.');
        }
    };

    const handleTaskPress = (task: any) => {
        navigation.navigate('TaskDetails', { task });
    };

    const handlePlayTask = (taskId: number) => {
        // Persist chosen task in global store, then navigate to the Swipe tab
        setActiveTaskId(taskId);
        navigation.navigate('SwipeLab');
    };

    // UI guard: filter explore list against assigned IDs as a second line of defense.
    // The backend exclusion query is the primary guard; this prevents any stale-cache leakage.
    const assignedTaskIds = new Set((tasks as any[]).map((t) => t.taskId));
    const filteredExploreTasks = (availableTasks as any[]).filter((t) => !assignedTaskIds.has(t.taskId));

    // Calculate total completion
    const totalAssigned = tasks.length;
    let totalImages = 0;
    let totalClassified = 0;
    tasks.forEach((t: any) => {
        totalImages += t.progress?.totalImages ?? t.totalImages ?? 0;
        totalClassified += t.progress?.imagesClassified ?? t.imagesClassified ?? 0;
    });
    const completionPercent = totalImages > 0 ? Math.round((totalClassified / totalImages) * 100) : 0;

    const cardBg    = isDark ? themeColors.card : '#F0F7FF';
    const borderCol = isDark ? themeColors.border : '#BFDBFE';

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/tasks.png')}
            leftTitle="My Tasks"
            rightIcon={require('../../../assets/images/stats.png')}
            rightTitle={`Completion: ${completionPercent}%`}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={{ backgroundColor: themeColors.background }}
                contentContainerStyle={[styles.scrollContent, { backgroundColor: themeColors.background }]}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={loadData} tintColor={themeColors.text} />
                }
            >
                {/* ── Summary stat chips ──────────────────────────────────────────────── */}
                <View style={styles.statsRow}>
                    <StatChip
                        icon="clipboard-outline"
                        label="Assigned"
                        value={String(totalAssigned)}
                        isDark={isDark}
                        accent="#3B82F6"
                        themeSecondary={themeColors.textSecondary}
                    />
                    <StatChip
                        icon="images-outline"
                        label="Classified"
                        value={String(totalClassified)}
                        isDark={isDark}
                        accent="#3B82F6"
                        themeSecondary={themeColors.textSecondary}
                    />
                    <StatChip
                        icon="trending-up-outline"
                        label="Progress"
                        value={`${completionPercent}%`}
                        isDark={isDark}
                        accent="#3B82F6"
                        themeSecondary={themeColors.textSecondary}
                    />
                </View>

                {/* ── Assigned tasks section ──────────────────────────────────────────── */}
                <SectionHeader icon="checkmark-circle-outline" title="Assigned Tasks" color="#3B82F6" />

                {tasks.length === 0 && (
                    <View style={[styles.emptyCard, { backgroundColor: cardBg, borderColor: borderCol }]}>
                        <Ionicons name="clipboard-outline" size={28} color="#BFDBFE" />
                        <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>No tasks assigned yet.</Text>
                    </View>
                )}

                {(tasks as any[]).map((task: any) => {
                    const taskTotalImages = task.progress?.totalImages ?? task.totalImages ?? 0;
                    const imagesClassified = task.progress?.imagesClassified ?? task.imagesClassified ?? 0;
                    const progress = taskTotalImages > 0 ? Math.round((imagesClassified / taskTotalImages) * 100) : 0;

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={Array.isArray(task.targetSpecies) ? task.targetSpecies.map((s: any) => s.name ?? s) : []}
                            imagesClassified={imagesClassified}
                            progress={progress}
                            onPlay={() => handlePlayTask(task.taskId)}
                            onPress={() => handleTaskPress(task)}
                            actionType="play"
                        />
                    );
                })}

                {/* ── Explore tasks section ───────────────────────────────────────────── */}
                <SectionHeader icon="compass-outline" title="Explore Tasks" color="#10B981" />

                {filteredExploreTasks.map((task: any) => {
                    const taskTotalImages = task.progress?.totalImages ?? task.totalImages ?? 0;
                    const imagesClassified = task.progress?.imagesClassified ?? task.imagesClassified ?? 0;
                    const progress = taskTotalImages > 0 ? Math.round((imagesClassified / taskTotalImages) * 100) : 0;

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={Array.isArray(task.targetSpecies) ? task.targetSpecies.map((s: any) => s.name ?? s) : []}
                            imagesClassified={imagesClassified}
                            progress={progress}
                            onPlay={() => handleAddTask(task.taskId)}
                            onPress={() => handleTaskPress(task)}
                            actionType="add"
                        />
                    );
                })}

                {filteredExploreTasks.length === 0 && (
                    <View style={[styles.emptyCard, { backgroundColor: cardBg, borderColor: borderCol }]}>
                        <Ionicons name="compass-outline" size={28} color="#A7F3D0" />
                        <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>No public tasks available.</Text>
                    </View>
                )}

            </ScrollView>
        </ScreenHeaderLayout>
    );
}

// ── Sub-components ──────────────────────────────────────────────────────────────

function SectionHeader({ icon, title, color }: { icon: any; title: string; color: string }) {
    return (
        <View style={styles.sectionHeader}>
            <Ionicons name={icon} size={16} color={color} style={{ marginRight: 6 }} />
            <Text style={[styles.sectionTitle, { color }]}>{title}</Text>
        </View>
    );
}

function StatChip({
    icon, label, value, isDark, accent, themeSecondary,
}: {
    icon: any; label: string; value: string; isDark: boolean; accent: string; themeSecondary: string;
}) {
    return (
        <View style={[styles.statChip, { backgroundColor: isDark ? '#1e3a5f' : '#EFF6FF', borderColor: isDark ? '#2a4a7f' : '#BFDBFE' }]}>
            <Ionicons name={icon} size={14} color={accent} style={{ marginBottom: 2 }} />
            <Text style={[styles.statValue, { color: accent }]}>{value}</Text>
            <Text style={[styles.statLabel, { color: themeSecondary }]}>{label}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    scrollContent: {
        padding: 16,
        paddingBottom: 80,
    },
    // Summary chips
    statsRow: {
        flexDirection: 'row',
        gap: 10,
        marginBottom: 20,
    },
    statChip: {
        flex: 1,
        alignItems: 'center',
        paddingVertical: 12,
        paddingHorizontal: 6,
        borderRadius: 14,
        borderWidth: 1,
        gap: 2,
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.06,
        shadowRadius: 4,
        elevation: 2,
    },
    statValue: {
        fontSize: 16,
        fontWeight: '800',
    },
    statLabel: {
        fontSize: 10,
        textAlign: 'center',
        fontWeight: '500',
        textTransform: 'uppercase',
        letterSpacing: 0.3,
    },
    // Section header
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 12,
        marginTop: 8,
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '700',
    },
    // Empty state
    emptyCard: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        padding: 16,
        borderRadius: 14,
        borderWidth: 1,
        marginBottom: 14,
    },
    emptyText: {
        fontSize: 14,
    },
});
