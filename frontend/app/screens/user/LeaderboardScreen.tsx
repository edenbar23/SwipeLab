import { useNavigation } from '@react-navigation/native';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { apiFetch } from '../../api/apiFetch';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useThemeStore } from '../../stores/themeStore';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { useLeaderboard, useRank, useProfile } from '../../api/queries';


interface LeaderboardEntry {
    rank: number;
    username: string;
    score: number;
}

interface LeaderboardData {
    currentUser: {
        rank: number;
        username: string;
        score: number;
    };
    allTime: LeaderboardEntry[];
    monthly: LeaderboardEntry[];
}

const getMedalEmoji = (rank: number): string => {
    switch (rank) {
        case 1: return '🥇';
        case 2: return '🥈';
        case 3: return '🥉';
        default: return '';
    }
};

const getRowStyle = (rank: number, type: 'allTime' | 'monthly', themeColors: any, isDark: boolean) => {
    let bgColor = isDark ? themeColors.card : '#ffffff'; // Default

    // Vibrant medal colors for top 3
    if (rank === 1) bgColor = isDark ? '#B8860B' : '#FFD700'; // Vibrant Gold
    else if (rank === 2) bgColor = isDark ? '#4169E1' : '#AEE2FF'; // Frosty Blue (replacing grey/silver)
    else if (rank === 3) bgColor = isDark ? '#A0522D' : '#F4A460'; // Rich Bronze

    return { backgroundColor: bgColor };
};

interface LeaderboardTableProps {
    title: string;
    data: LeaderboardEntry[];
    type: 'allTime' | 'monthly';
}

function LeaderboardTable({ title, data, type }: LeaderboardTableProps) {
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    // Use a vibrant primary color for the header
    const headerBg = theme === 'dark' ? '#312E81' : '#4B7BE5'; // Indigo in dark, Blue in light
    const headerTextColor = '#ffffff'; 
    const cellColor = theme === 'dark' ? themeColors.text : '#2d3748'; // Richer dark gray for text in light mode

    return (
        <View style={styles.tableContainer}>
            <Text style={[styles.tableTitle, { color: themeColors.text }]}>{title}</Text>
            <View style={[styles.table, { backgroundColor: themeColors.card }]}>
                {/* Header Row */}
                <View style={[styles.headerRow, { backgroundColor: headerBg }]}>
                    <Text style={[styles.headerCell, styles.rankCol, { color: headerTextColor }]}>Rank</Text>
                    <Text style={[styles.headerCell, styles.userCol, { color: headerTextColor }]}>User</Text>
                    <Text style={[styles.headerCell, styles.scoreCol, { color: headerTextColor }]}>Score</Text>
                </View>

                {/* Data Rows */}
                {data.map((entry) => {
                    const isTop3 = entry.rank <= 3;
                    // For top 3, make text color slightly darker in light mode for better contrast
                    const rowTextColor = (isTop3 && theme !== 'dark') ? '#1a202c' : cellColor;
                    return (
                        <View
                            key={entry.rank}
                            style={[styles.dataRow, getRowStyle(entry.rank, type, themeColors, theme === 'dark'), { borderBottomColor: themeColors.border }]}
                        >
                            <Text style={[styles.dataCell, styles.rankCol, { color: rowTextColor, fontWeight: 'bold' }]}>{entry.rank}</Text>
                            <Text style={[styles.dataCell, styles.userCol, { color: rowTextColor, fontWeight: '700' }]}>{entry.username}</Text>
                            <View style={styles.scoreContainer}>
                                <Text style={[styles.dataCell, styles.scoreText, { color: rowTextColor, fontWeight: 'bold' }]}>
                                    {entry.score.toLocaleString()}
                                </Text>
                                <Text style={styles.medal}>{getMedalEmoji(entry.rank)}</Text>
                            </View>
                        </View>
                    );
                })}
            </View>
        </View>
    );
}


const RANK_COLORS: Record<string, string> = {
    UNRANKED: '#818CF8', // Indigo instead of grey
    BEGINNER: '#60a5fa',
    EXPERT: '#34d399',
    PRO: '#a78bfa',
    LEGEND: '#fbbf24',
};

function RankBadge({ themeColors, isDark }: { themeColors: any; isDark: boolean }) {
    const { data: rankData, isLoading } = useRank();
    if (isLoading || !rankData) return null;

    const color = RANK_COLORS[rankData.tier] ?? '#9ca3af';
    const nextLabel = rankData.nextTierAt === -1
        ? 'MAX'
        : `${rankData.yesTagCount} / ${rankData.nextTierAt} tags`;

    return (
        <View style={[rankStyles.container, { backgroundColor: isDark ? themeColors.card : '#fff' }]}>
            <View style={[rankStyles.pill, { backgroundColor: color }]}>
                <Text style={rankStyles.pillText}>{rankData.tier}</Text>
            </View>
            <View style={rankStyles.info}>
                <Text style={[rankStyles.label, { color: themeColors.text }]}>Your Rank</Text>
                <Text style={[rankStyles.sub, { color: themeColors.textSecondary }]}>{nextLabel}</Text>
            </View>
            {rankData.nextTierAt !== -1 && (
                <View style={[rankStyles.barBg, { backgroundColor: isDark ? '#374151' : '#e5e7eb' }]}>
                    <View style={[rankStyles.barFill, { width: `${rankData.progressPercent}%` as any, backgroundColor: color }]} />
                </View>
            )}
        </View>
    );
}

const rankStyles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        borderRadius: 12,
        padding: 12,
        marginBottom: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 3,
        elevation: 2,
        flexWrap: 'wrap',
        gap: 8,
    },
    pill: {
        paddingHorizontal: 12,
        paddingVertical: 4,
        borderRadius: 20,
    },
    pillText: {
        color: '#fff',
        fontWeight: '800',
        fontSize: 13,
        letterSpacing: 0.5,
    },
    info: {
        flex: 1,
    },
    label: {
        fontWeight: '700',
        fontSize: 13,
    },
    sub: {
        fontSize: 11,
        marginTop: 1,
    },
    barBg: {
        height: 4,
        borderRadius: 2,
        width: '100%',
        marginTop: 4,
    },
    barFill: {
        height: 4,
        borderRadius: 2,
    },
});

const DEBUG_MODE = false;

export default function LeaderboardScreen() {
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const { data: profile } = useProfile();
    const { data: rawData, isLoading: loading, error } = useLeaderboard();

    const data = React.useMemo(() => {
        if (!rawData) return null;
        const allTime = rawData.map((item: any, index: number) => ({
            rank: index + 1,
            username: item.username === profile?.username ? "You" : item.username,
            score: item.score,
            isCurrentUser: item.username === profile?.username
        }));
        const monthly = [...allTime].sort((a, b) => b.score - a.score).slice(0, 5);
        const currentUser = allTime.find((item: any) => item.isCurrentUser) || {
            rank: 0,
            username: "You",
            score: 0
        };
        return { currentUser, allTime, monthly };
    }, [rawData, profile]);

    const updateScore = async (newScore: number) => {
        console.warn("Update score not implemented in backend yet");
    };

    if (loading) {
        return (
            <View style={[styles.loadingContainer, { backgroundColor: themeColors.background }]}>
                <ActivityIndicator size="large" color="#4B7BE5" />
                <Text style={styles.loadingText}>Loading leaderboard...</Text>
            </View>
        );
    }

    if (error || !data) {
        return (
            <View style={styles.errorContainer}>
                <Text style={styles.errorText}>⚠️ {(error as Error)?.message || 'Failed to load leaderboard'}</Text>
            </View>
        );
    }

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/leaderboard.png')}
            leftTitle="Leaderboard"
            rightIcon={require('../../../assets/images/profile.png')}
            rightTitle={data ? `Your Spot: #${data.currentUser.rank}` : 'Your Spot: -'}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={[styles.container, { backgroundColor: themeColors.background }]}
                contentContainerStyle={styles.content}
                showsVerticalScrollIndicator={false}
            >
                {/* Header info (Your Spot) - Removed as it is now in the top header */}

                {/* Test Controls (Debug Only) */}
                {DEBUG_MODE && (
                    <View style={styles.testControls}>
                        <Text style={styles.testLabel}>⚡ Test: Change Your Score</Text>
                        <View style={styles.testButtons}>
                            <TouchableOpacity style={styles.testBtn} onPress={() => updateScore(15000)}>
                                <Text style={styles.testBtnText}>🥇 15,000</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.testBtn} onPress={() => updateScore(11900)}>
                                <Text style={styles.testBtnText}>🥈 11,900</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.testBtn} onPress={() => updateScore(9400)}>
                                <Text style={styles.testBtnText}>📅 9,400</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.testBtn} onPress={() => updateScore(7542)}>
                                <Text style={styles.testBtnText}>Reset</Text>
                            </TouchableOpacity>
                        </View>
                        <Text style={styles.currentScore}>Current: {data.currentUser.score.toLocaleString()} pts</Text>
                    </View>
                )}

                {/* Rank Badge for current user */}
                <RankBadge themeColors={themeColors} isDark={theme === 'dark'} />

                {/* All Time Leaderboard */}
                <LeaderboardTable
                    title="🏆 Greatest Of All Time"
                    data={data.allTime}
                    type="allTime"
                />

                {/* Monthly Leaderboard */}
                <LeaderboardTable
                    title="🔥 Greatest Of The Month"
                    data={data.monthly}
                    type="monthly"
                />
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#daddffff',
    },
    content: {
        padding: 16,
        paddingBottom: 32,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#daddffff',
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
        color: '#4B7BE5',
    },
    errorContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#daddffff',
        padding: 20,
    },
    errorText: {
        fontSize: 16,
        color: '#dc3545',
        textAlign: 'center',
    },
    headerInfo: {
        marginBottom: 20,
        alignItems: 'center',
    },
    yourSpot: {
        fontSize: 18,
        fontWeight: '600',
        color: '#4B7BE5',
    },
    tableContainer: {
        marginBottom: 24,
    },
    tableTitle: {
        fontSize: 22,
        fontWeight: '900',
        color: '#1a1a2e',
        marginBottom: 12,
        textAlign: 'center',
        letterSpacing: 0.5,
        textTransform: 'uppercase',
    },
    table: {
        borderRadius: 16,
        overflow: 'hidden',
        backgroundColor: '#fff',
        shadowColor: '#4B7BE5',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.15,
        shadowRadius: 8,
        elevation: 5,
        borderWidth: 1,
        borderColor: 'rgba(75, 123, 229, 0.1)',
    },
    headerRow: {
        flexDirection: 'row',
        backgroundColor: '#4B7BE5',
        paddingVertical: 14,
        paddingHorizontal: 8,
    },
    headerCell: {
        fontWeight: '800',
        fontSize: 15,
        color: '#fff',
        textAlign: 'center',
        letterSpacing: 0.5,
        textTransform: 'uppercase',
    },
    dataRow: {
        flexDirection: 'row',
        paddingVertical: 14,
        paddingHorizontal: 8,
        borderBottomWidth: 1,
        borderBottomColor: 'rgba(0,0,0,0.05)',
        alignItems: 'center',
    },
    dataCell: {
        fontSize: 15,
        textAlign: 'center',
    },
    rankCol: {
        flex: 1,
    },
    userCol: {
        flex: 2,
    },
    scoreCol: {
        flex: 1.5,
    },
    scoreContainer: {
        flex: 1.5,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
    },
    scoreText: {
        marginRight: 4,
    },
    medal: {
        fontSize: 20,
    },
    testControls: {
        backgroundColor: '#f0f4ff',
        borderRadius: 12,
        padding: 12,
        marginBottom: 20,
        borderWidth: 2,
        borderColor: '#4B7BE5',
        borderStyle: 'dashed',
    },
    testLabel: {
        fontSize: 14,
        fontWeight: 'bold',
        color: '#4B7BE5',
        marginBottom: 8,
        textAlign: 'center',
    },
    testButtons: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        marginBottom: 8,
    },
    testBtn: {
        backgroundColor: '#4B7BE5',
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
    },
    testBtnText: {
        color: '#fff',
        fontWeight: 'bold',
        fontSize: 12,
    },
    currentScore: {
        textAlign: 'center',
        fontSize: 12,
        color: '#666',
    },
});
