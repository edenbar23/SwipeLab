// admin screen for viewing analytics
import React, { useEffect, useState } from "react";
import {
    Alert,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from "react-native";
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from "../../api/apiFetch";
import MetricCard from "../../components/admin/MetricCard";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { useQueryClient } from "@tanstack/react-query";
import { useThemeStore } from '../../stores/themeStore';
import { useAnalyticsTask, useAnalyticsUsers, useAnalyticsTop, QUERY_KEYS } from "../../api/queries";


type TaskAnalytics = {
    taskId: number;
    taskName?: string;
    status: string;
    progress?: {
        totalImages: number;
        imagesClassified: number;
        percentComplete: number;
        completedImages: number;
    };
    consensus?: {
        overallAverage: number | null;
        lowConsensusImages: number | null;
        threshold: number | null;
    };
    participation?: {
        activeUsers: number | null;
        totalClassifications: number | null;
        averageClassificationsPerUser: number | null;
        medianResponseTimeMs: number | null;
    };
};

export type UserPerformance = {
    username: string;
    displayName: string;
    totalClassifications: number;
    goldImageClassifications: number;
    correctGoldClassifications: number;
    goldAccuracy: number;
    credibilityScore: number;
    currentStreak: number;
    points: number;
};

export default function AnalyticsScreen({ navigation }: any) {
    const selectedTaskId = 1;
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const queryClient = useQueryClient();

    const { data: analytics, isLoading: analyticsLoading, refetch: refetchAnalytics } = useAnalyticsTask(selectedTaskId);
    const { data: users, isLoading: usersLoading, refetch: refetchUsers } = useAnalyticsUsers(selectedTaskId);
    const { data: topPerformers, isLoading: topLoading, refetch: refetchTop } = useAnalyticsTop(5);

    const loading = analyticsLoading || usersLoading || topLoading;
    const [refreshing, setRefreshing] = useState(false);

    const handleRefresh = async () => {
        setRefreshing(true);
        await Promise.all([
            refetchAnalytics(),
            refetchUsers(),
            refetchTop(),
        ]).catch(() => Alert.alert("Error", "Failed to reach server"));
        setRefreshing(false);
    };

    const handleStateChange = async (action: "pause" | "activate" | "archive") => {
        const actionMessages = {
            pause: "Pause this task?",
            activate: "Activate this task?",
            archive: "Archive this task? This cannot be undone.",
        };

        Alert.alert(
            `${action.charAt(0).toUpperCase() + action.slice(1)} Task`,
            actionMessages[action],
            [
                { text: "Cancel", style: "cancel" },
                {
                    text: action.charAt(0).toUpperCase() + action.slice(1),
                    style: action === "archive" ? "destructive" : "default",
                    onPress: async () => {
                        try {
                            const endpoint =
                                action === "archive"
                                    ? `/api/v1/dashboard/tasks/archive/${selectedTaskId}`
                                    : `/api/v1/dashboard/tasks/${selectedTaskId}/${action}`;

                            await apiFetch(endpoint, { method: "POST" });
                            Alert.alert("Success", `Task ${action}d successfully`);
                            queryClient.invalidateQueries({ queryKey: ['tasks'] });
                            queryClient.invalidateQueries({ queryKey: ['analytics'] });
                            handleRefresh();
                        } catch (err) {
                            console.error(`${action} error:`, err);
                            Alert.alert("Error", `Failed to ${action} task`);
                        }
                    },
                },
            ]
        );
    };

    const handleExport = async () => {
        try {
            const res = await apiFetch(`/api/v1/analytics/exports`, {
                method: "POST",
                body: JSON.stringify({ taskId: selectedTaskId })
            });
            const data = await res.json();
            Alert.alert("Export Started", `Status: ${data.status}\nEstimated completion: ${new Date(data.estimatedCompletion).toLocaleTimeString()}`);
        } catch (error) {
            console.error("Export error:", error);
            Alert.alert("Error", "Failed to start export");
        }
    };



    return (
        <ScreenHeaderLayout
            leftIcon={require("../../../assets/images/stats.png")}
            leftTitle="Analytics"
            rightIcon={require("../../../assets/images/tasks_mgmt.png")}
            rightTitle="Tasks"
            onRightPress={() => navigation.navigate("TasksManagement")}
        >
            <ScrollView
                contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
                }
            >
                {loading && !analytics ? (
                    <View style={styles.loadingContainer}>
                        <Text style={[styles.loadingText, { color: themeColors.textSecondary }]}>Loading analytics...</Text>
                    </View>
                ) : !analytics ? (
                    <View style={styles.emptyContainer}>
                        <Text style={styles.emptyText}>No analytics data available</Text>
                    </View>
                ) : (
                    <>
                        {/* Task Info */}
                        <View style={styles.section}>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Task</Text>
                            <View style={[styles.taskInfo, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
                                <Text style={[styles.taskName, { color: themeColors.text }]}>{analytics?.taskName ?? `Task #${analytics?.taskId ?? ''}`}</Text>
                                <View
                                    style={[
                                        styles.statusBadge,
                                        analytics?.status === "ACTIVE"
                                            ? styles.statusActive
                                            : analytics?.status === "PAUSED"
                                                ? styles.statusPaused
                                                : styles.statusArchived,
                                    ]}
                                >
                                    <Text style={styles.statusText}>{analytics?.status ?? "UNKNOWN"}</Text>
                                </View>
                            </View>
                        </View>

                        {/* Progress Section */}
                        <View style={styles.section}>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>📊 Progress</Text>
                            <View style={styles.row}>
                                <View style={styles.halfWidth}>
                                    <MetricCard
                                        label="Completion"
                                        value={`${(analytics?.progress?.percentComplete ?? 0).toFixed(1)}%`}
                                        variant="primary"
                                    />
                                </View>
                                <View style={styles.halfWidth}>
                                    <MetricCard
                                        label="Total Images"
                                        value={analytics?.progress?.totalImages ?? 0}
                                        variant="primary"
                                    />
                                </View>
                            </View>
                            <MetricCard
                                label="Classified Images"
                                value={analytics?.progress?.imagesClassified ?? 0}
                                subtitle={`${(analytics?.progress?.totalImages ?? 0) - (analytics?.progress?.imagesClassified ?? 0)} remaining`}
                                variant="success"
                            />
                        </View>

                        {/* Quality Section */}
                        <View style={styles.section}>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>✨ Data Quality</Text>
                            <MetricCard
                                label="Average Consensus"
                                value={`${(analytics?.consensus?.overallAverage ?? 0).toFixed(1)}%`}
                                variant="success"
                            />
                            <View style={styles.row}>
                                <View style={styles.halfWidth}>
                                    <MetricCard
                                        label="Completed Images"
                                        value={analytics?.progress?.completedImages ?? 0}
                                        subtitle="Reached consensus"
                                        variant="success"
                                    />
                                </View>
                                <View style={styles.halfWidth}>
                                    <MetricCard
                                        label="Low Consensus"
                                        value={analytics?.consensus?.lowConsensusImages ?? 0}
                                        subtitle="Below threshold"
                                        variant="warning"
                                    />
                                </View>
                            </View>
                        </View>

                        {/* Users Section */}
                        <View style={styles.section}>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>👥 Users</Text>
                            <MetricCard
                                label="Active Users"
                                value={analytics?.participation?.activeUsers ?? 0}
                                subtitle={`${analytics?.participation?.totalClassifications ?? 0} total classifications`}
                                variant="primary"
                            />

                            <Text style={[styles.subsectionTitle, { color: themeColors.textSecondary }]}>Top Performers</Text>
                            {topPerformers?.map((user: UserPerformance, index: number) => (
                                <View key={user.username} style={[styles.userCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
                                    <View style={styles.userRank}>
                                        <Text style={styles.rankText}>#{index + 1}</Text>
                                    </View>
                                    <View style={styles.userInfo}>
                                        <Text style={[styles.userName, { color: themeColors.text }]}>{user.displayName ?? user.username ?? "Unknown User"}</Text>
                                        <Text style={styles.userStats}>
                                            {user.totalClassifications ?? 0} classifications •{" "}
                                            {(user.goldAccuracy ?? 0).toFixed(1)}% accuracy
                                        </Text>
                                    </View>
                                    <View style={styles.userScore}>
                                        <Text style={styles.credibilityScore}>
                                            {((user.credibilityScore ?? 0) * 100).toFixed(0)}
                                        </Text>
                                        <Text style={styles.credibilityLabel}>score</Text>
                                    </View>
                                </View>
                            ))}
                        </View>

                        {/* Actions Section */}
                        <View style={styles.section}>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>⚙️ Actions</Text>

                            <TouchableOpacity
                                style={styles.exportButton}
                                onPress={handleExport}
                            >
                                <Text style={styles.exportButtonText}>📥 Export Results</Text>
                            </TouchableOpacity>

                            <Text style={[styles.subsectionTitle, { color: themeColors.textSecondary }]}>Task Controls</Text>
                            <View style={styles.controlButtons}>
                                {analytics?.status !== "ACTIVE" && (
                                    <TouchableOpacity
                                        style={[styles.controlButton, styles.activateButton]}
                                        onPress={() => handleStateChange("activate")}
                                    >
                                        <Text style={styles.controlButtonText}>▶️ Activate</Text>
                                    </TouchableOpacity>
                                )}
                                {analytics?.status === "ACTIVE" && (
                                    <TouchableOpacity
                                        style={[styles.controlButton, styles.pauseButton]}
                                        onPress={() => handleStateChange("pause")}
                                    >
                                        <Text style={styles.controlButtonText}>⏸️ Pause</Text>
                                    </TouchableOpacity>
                                )}
                                <TouchableOpacity
                                    style={[styles.controlButton, styles.archiveButton]}
                                    onPress={() => handleStateChange("archive")}
                                >
                                    <Text style={styles.controlButtonText}>🗄️ Archive</Text>
                                </TouchableOpacity>
                            </View>
                        </View>
                    </>
                )}
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 16,
    },
    section: {
        marginBottom: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: "700",
        color: "#1F2937",
        marginBottom: 12,
    },
    subsectionTitle: {
        fontSize: 14,
        fontWeight: "600",
        color: "#6B7280",
        marginTop: 8,
        marginBottom: 8,
    },
    taskInfo: {
        flexDirection: "row",
        alignItems: "center",
        backgroundColor: "#F9FAFB",
        padding: 16,
        borderRadius: 10,
        borderWidth: 1,
        borderColor: "#D1D5DB",
    },
    taskName: {
        flex: 1,
        fontSize: 16,
        fontWeight: "600",
        color: "#1F2937",
    },
    statusBadge: {
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 12,
    },
    statusActive: {
        backgroundColor: "#10B981",
    },
    statusPaused: {
        backgroundColor: "#F59E0B",
    },
    statusArchived: {
        backgroundColor: "#6B7280",
    },
    statusText: {
        color: "#fff",
        fontSize: 12,
        fontWeight: "600",
    },
    row: {
        flexDirection: "row",
        gap: 12,
    },
    halfWidth: {
        flex: 1,
    },
    userCard: {
        flexDirection: "row",
        alignItems: "center",
        backgroundColor: "#F9FAFB",
        padding: 12,
        borderRadius: 10,
        marginBottom: 8,
        borderWidth: 1,
        borderColor: "#E5E7EB",
    },
    userRank: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: "#3B82F6",
        justifyContent: "center",
        alignItems: "center",
        marginRight: 12,
    },
    rankText: {
        color: "#fff",
        fontWeight: "700",
        fontSize: 14,
    },
    userInfo: {
        flex: 1,
    },
    userName: {
        fontSize: 15,
        fontWeight: "600",
        color: "#1F2937",
        marginBottom: 2,
    },
    userStats: {
        fontSize: 12,
        color: "#6B7280",
    },
    userScore: {
        alignItems: "center",
    },
    credibilityScore: {
        fontSize: 20,
        fontWeight: "700",
        color: "#10B981",
    },
    credibilityLabel: {
        fontSize: 10,
        color: "#6B7280",
        textTransform: "uppercase",
    },
    exportButton: {
        backgroundColor: "#3B82F6",
        padding: 16,
        borderRadius: 10,
        alignItems: "center",
        marginBottom: 16,
    },
    exportButtonText: {
        color: "#fff",
        fontSize: 16,
        fontWeight: "600",
    },
    controlButtons: {
        flexDirection: "row",
        gap: 12,
    },
    controlButton: {
        flex: 1,
        padding: 14,
        borderRadius: 10,
        alignItems: "center",
    },
    activateButton: {
        backgroundColor: "#10B981",
    },
    pauseButton: {
        backgroundColor: "#F59E0B",
    },
    archiveButton: {
        backgroundColor: "#EF4444",
    },
    controlButtonText: {
        color: "#fff",
        fontSize: 14,
        fontWeight: "600",
    },
    loadingContainer: {
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
        paddingTop: 100,
    },
    loadingText: {
        fontSize: 16,
        color: "#6B7280",
    },
    emptyContainer: {
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
        paddingTop: 100,
    },
    emptyText: {
        fontSize: 16,
        color: "#6B7280",
    },
});