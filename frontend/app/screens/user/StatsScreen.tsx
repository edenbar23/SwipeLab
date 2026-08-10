import { useNavigation } from '@react-navigation/native';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { apiFetch } from '@/api/apiFetch';
import ScreenHeaderLayout from '@/components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useThemeStore } from '@/stores/themeStore';
import { useAllStatistics } from '@/api/queries';


const DEBUG_MODE = false; // Set to true for testing controls

interface SummaryData {
    summary: {
        totalClassifications: number;
        correctClassifications: number;
        accuracy: number;
        contributionPercentage: number;
        rank: {
            daily: number;
            weekly: number;
            monthly: number;
            allTime: number;
        };
        rankPercentile: number;
        currentStreak: number;
        longestStreak: number;
    };
    trend: {
        byDay: Array<{ date: string; accuracy: number }>;
    };
}

interface VsExpertsData {
    user: { accuracy: number };
    experts: { accuracy: number };
    difference: { accuracy: number };
}

interface VsUsersData {
    percentile: number;
    averageUserAccuracy: number;
}

interface SpeciesBreakdown {
    category: string;
    total: number;
    accuracy: number;
}

interface BreakdownData {
    byCategory: SpeciesBreakdown[];
}

interface UserInfoData {
    score: number;
    badge: string | null;
    currentStreak: number;
}

interface StatsData {
    summary: SummaryData;
    vsExperts: VsExpertsData;
    vsUsers: VsUsersData;
    breakdown: BreakdownData;
    userInfo: UserInfoData;
}

function ProgressBar({ value, color, label, maxValue = 1 }: { value: number; color: string; label: string; maxValue?: number }) {
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const percentage = Math.min(100, Math.max(0, (value / maxValue) * 100));
    return (
        <View style={styles.progressContainer}>
            <View style={styles.progressLabelRow}>
                <Text style={[styles.progressLabel, { color: themeColors.textSecondary }]}>{label}</Text>
                <Text style={[styles.progressValue, { color: themeColors.text }]}>{(value * 100).toFixed(1)}%</Text>
            </View>
            <View style={styles.progressBarTrack}>
                <View style={[styles.progressBarFill, { width: `${percentage}%`, backgroundColor: color }]} />
            </View>
        </View>
    );
}

function SummaryCard({ title, value, subtext, icon }: { title: string; value: string | number; subtext?: string, icon?: string }) {
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    return (
        <View style={[styles.summaryCard, { backgroundColor: themeColors.card }]}>
            <View style={styles.cardHeader}>
                <Text style={[styles.cardTitle, { color: themeColors.textSecondary }]}>{title}</Text>
                {icon && <Text style={styles.cardIcon}>{icon}</Text>}
            </View>
            <Text style={[styles.cardValue, { color: themeColors.text }]}>{value}</Text>
            {subtext && (
                <View style={styles.badgeContainer}>
                    <Text style={styles.badgeText}>{subtext}</Text>
                </View>
            )}
        </View>
    );
}

export default function StatsScreen() {
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    
    const { data, isLoading: loading, refetch, isRefetching } = useAllStatistics();
    const refreshing = isRefetching;
    
    const onRefresh = useCallback(() => {
        refetch();
    }, [refetch]);

    if (loading && !refreshing && !data) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#4B7BE5" />
            </View>
        );
    }

    if (!data) return null;

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/stats.png')}
            leftTitle="Stats"
            centerIcon={require('../../../assets/images/collection.png')}
            centerTitle="Collection"
            onCenterPress={() => navigation.navigate('Collection')}
            rightIcon={require('../../../assets/images/my-profile.png')}
            rightTitle="My Profile"
            onRightPress={() => navigation.navigate('Profile')}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={[styles.container, { backgroundColor: themeColors.background }]}
                contentContainerStyle={styles.content}
                showsVerticalScrollIndicator={false}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
            >


                {/* User Profile Summary */}
                <View style={styles.grid}>
                    <SummaryCard title="Global Rank" value={`#${data.summary?.summary?.rank?.allTime ?? '-'}`} subtext="Top 1%" icon="🌍" />
                    <SummaryCard title="Score" value={data.userInfo?.score?.toLocaleString() ?? '0'} icon="🌟" />
                    <SummaryCard title="Tasks Done" value={data.summary?.summary?.totalClassifications ?? 0} icon="✅" />
                    <SummaryCard title="Accuracy" value={`${((data.summary?.summary?.accuracy ?? 0) * 100).toFixed(1)}%`} subtext="Overall" icon="🎯" />
                </View>

                {/* Streak */}
                <View style={styles.section}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>🔥 Streak</Text>
                    <View style={[styles.streakContainer, { backgroundColor: themeColors.card }]}>
                        <Text style={[styles.streakText, { color: themeColors.text }]}>Current: <Text style={styles.streakBold}>{data.userInfo?.currentStreak ?? 0} days</Text></Text>
                        <Text style={[styles.streakText, { color: themeColors.text }]}>Longest: <Text style={styles.streakBold}>{data.userInfo?.longestStreak ?? 0} days</Text></Text>
                    </View>
                </View>

                {/* Comparisons */}
                <View style={styles.section}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>⚖️ Comparisons</Text>
                    <View style={[styles.card, { backgroundColor: themeColors.card }]}>
                        <Text style={[styles.chartTitle, { color: themeColors.text }]}>Vs Experts</Text>
                        <ProgressBar
                            label="Your Accuracy"
                            value={data.vsExperts?.user?.accuracy ?? 0}
                            color="#4B7BE5"
                        />
                        <ProgressBar
                            label="Expert Benchmark"
                            value={data.vsExperts?.experts?.accuracy ?? 0}
                            color="#8B008B"
                        />
                        <Text style={[styles.insightText, { color: themeColors.textSecondary }]}>
                            You are {Math.abs((data.vsExperts?.difference?.accuracy ?? 0) * 100).toFixed(1)}%
                            {(data.vsExperts?.difference?.accuracy ?? 0) >= 0 ? ' above ' : ' below '}
                            expert level.
                        </Text>

                        <View style={styles.separator} />

                        <Text style={[styles.chartTitle, { color: themeColors.text }]}>Vs Community</Text>
                        <ProgressBar
                            label="Average User"
                            value={data.vsUsers?.averageUserAccuracy ?? 0}
                            color="#FFA500"
                        />
                        <Text style={[styles.insightText, { color: themeColors.textSecondary }]}>
                            You're in the top {Math.max(0, 100 - (data.vsUsers?.percentile ?? 0)).toFixed(0)}% of contributors!
                        </Text>
                    </View>
                </View>

                {/* Breakdown */}
                <View style={styles.section}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>📝 Species Breakdown</Text>
                    {data.breakdown?.byCategory?.map((item: any, index: number) => (
                        <View key={index} style={[styles.taskRow, { backgroundColor: themeColors.card }]}>
                            <View style={styles.taskInfo}>
                                <Text style={[styles.taskName, { color: themeColors.text }]}>{item.category}</Text>
                                <Text style={styles.taskCount}>{item.total} classifications</Text>
                            </View>
                            <View style={styles.taskStat}>
                                <Text style={styles.taskAccuracy}>{((item.accuracy ?? 0) * 100).toFixed(1)}%</Text>
                            </View>
                        </View>
                    ))}
                </View>

                {/* Debug Controls */}
                {/* {DEBUG_MODE && (
                    <View style={styles.debugControls}>
                        <Text style={styles.debugTitle}>⚡ Debug: Adjust Accuracy</Text>
                        <View style={styles.debugButtons}>
                            <TouchableOpacity style={styles.debugBtn} onPress={() => updateAccuracy(0.65)}>
                                <Text style={styles.debugBtnText}>Low (65%)</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.debugBtn} onPress={() => updateAccuracy(0.88)}>
                                <Text style={styles.debugBtnText}>Avg (88%)</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.debugBtn} onPress={() => updateAccuracy(0.98)}>
                                <Text style={styles.debugBtnText}>Expert (98%)</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                )} */}

                <View style={{ height: 40 }} />
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f5f7fa',
    },
    content: {
        padding: 16,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    headerTitle: {
        fontSize: 28,
        fontWeight: 'bold',
        color: '#1a1a2e',
        marginBottom: 20,
    },
    grid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        marginBottom: 24,
    },
    summaryCard: {
        width: '48%',
        backgroundColor: '#fff',
        padding: 20,
        borderRadius: 16,
        marginBottom: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.08,
        shadowRadius: 8,
        elevation: 4,
        borderWidth: 1,
        borderColor: '#f0f0f0',
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    cardIcon: {
        fontSize: 20,
    },
    cardTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: '#666',
        textTransform: 'uppercase',
        letterSpacing: 0.5,
    },
    cardValue: {
        fontSize: 32,
        fontWeight: '900',
        color: '#1a1a2e',
    },
    badgeContainer: {
        marginTop: 8,
        alignSelf: 'flex-start',
        backgroundColor: '#e8f5e9', // Light green
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 12,
    },
    badgeText: {
        fontSize: 12,
        fontWeight: 'bold',
        color: '#2e7d32', // Dark green
    },
    section: {
        marginBottom: 24,
    },
    sectionTitle: {
        fontSize: 20,
        fontWeight: '900',
        color: '#1a1a2e',
        marginBottom: 12,
        textTransform: 'uppercase',
        letterSpacing: 0.5,
    },
    card: {
        backgroundColor: '#fff',
        padding: 16,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    streakContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        backgroundColor: '#fff',
        padding: 16,
        borderRadius: 12,
        elevation: 2,
    },
    streakText: {
        fontSize: 16,
        color: '#333',
    },
    streakBold: {
        fontWeight: 'bold',
        color: '#FF6347',
    },
    chartTitle: {
        fontSize: 16,
        fontWeight: '600',
        marginBottom: 12,
        marginTop: 8,
        color: '#333',
    },
    progressContainer: {
        marginBottom: 12,
    },
    progressLabelRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 4,
    },
    progressLabel: {
        fontSize: 14,
        color: '#666',
    },
    progressValue: {
        fontSize: 14,
        fontWeight: 'bold',
        color: '#333',
    },
    progressBarTrack: {
        height: 10,
        backgroundColor: '#e0e0e0',
        borderRadius: 5,
        overflow: 'hidden',
    },
    progressBarFill: {
        height: '100%',
        borderRadius: 5,
    },
    insightText: {
        fontSize: 14,
        color: '#666',
        fontStyle: 'italic',
        marginTop: 4,
        marginBottom: 8,
    },
    separator: {
        height: 1,
        backgroundColor: '#eee',
        marginVertical: 12,
    },
    taskRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        backgroundColor: '#fff',
        padding: 16,
        borderRadius: 12,
        marginBottom: 8,
        elevation: 1,
    },
    taskInfo: {
        flex: 1,
    },
    taskName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 4,
    },
    taskCount: {
        fontSize: 12,
        color: '#888',
    },
    taskStat: {
        backgroundColor: '#e8f0fe',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 16,
    },
    taskAccuracy: {
        color: '#4B7BE5',
        fontWeight: 'bold',
    },
    debugControls: {
        padding: 16,
        backgroundColor: '#f0f4ff',
        borderRadius: 12,
        borderWidth: 2,
        borderColor: '#4B7BE5',
        borderStyle: 'dashed',
        marginTop: 20,
    },
    debugTitle: {
        textAlign: 'center',
        color: '#4B7BE5',
        fontWeight: 'bold',
        marginBottom: 10,
    },
    debugButtons: {
        flexDirection: 'row',
        justifyContent: 'space-around',
    },
    debugBtn: {
        backgroundColor: '#4B7BE5',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 8,
    },
    debugBtnText: {
        color: '#fff',
        fontWeight: 'bold',
    },
});