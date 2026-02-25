import { useNavigation } from '@react-navigation/native';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { apiFetch } from '../../api/apiFetch';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useThemeStore } from '../../stores/themeStore';

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
    // Relaxed Pastel Colors
    // Rank 1: Soft Gold
    // Rank 2: Soft Silver/Gray
    // Rank 3: Soft Bronze/Orange
    // Rank 4+: Default/Transparent

    // Light Mode Pastels
    const lightColors = ['#FFF9C4', '#F5F5F5', '#FFCCBC'];
    // Dark Mode Muted Colors (Darker pastels so text is readable)
    const darkColors = ['#5D4037', '#424242', '#3E2723']; // Just examples, better to use semi-transparent

    // Better approach: Use opacity on base colors?
    // Let's use relaxed solid colors that work for both or switch based on theme

    let bgColor = isDark ? themeColors.card : '#fff'; // Default

    if (rank === 1) bgColor = isDark ? '#4A3B00' : '#FFF59D'; // Gold-ish
    else if (rank === 2) bgColor = isDark ? '#37474F' : '#CFD8DC'; // Silver-ish
    else if (rank === 3) bgColor = isDark ? '#3E2723' : '#FFAB91'; // Bronze-ish

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
    const headerBg = theme === 'dark' ? themeColors.card : '#e0e0e0';
    const cellColor = theme === 'dark' ? themeColors.text : '#333';

    return (
        <View style={styles.tableContainer}>
            <Text style={[styles.tableTitle, { color: themeColors.text }]}>{title}</Text>
            <View style={[styles.table, { backgroundColor: themeColors.card }]}>
                {/* Header Row */}
                <View style={[styles.headerRow, { backgroundColor: headerBg }]}>
                    <Text style={[styles.headerCell, styles.rankCol, { color: cellColor }]}>Rank</Text>
                    <Text style={[styles.headerCell, styles.userCol, { color: cellColor }]}>User</Text>
                    <Text style={[styles.headerCell, styles.scoreCol, { color: cellColor }]}>Score</Text>
                </View>

                {/* Data Rows */}
                {data.map((entry) => (
                    <View
                        key={entry.rank}
                        style={[styles.dataRow, getRowStyle(entry.rank, type, themeColors, theme === 'dark'), { borderBottomColor: themeColors.border }]}
                    >
                        <Text style={[styles.dataCell, styles.rankCol, { color: cellColor }]}>{entry.rank}</Text>
                        <Text style={[styles.dataCell, styles.userCol, { color: cellColor }]}>{entry.username}</Text>
                        <View style={styles.scoreContainer}>
                            <Text style={[styles.dataCell, styles.scoreText, { color: cellColor }]}>
                                {entry.score.toLocaleString()}
                            </Text>
                            <Text style={styles.medal}>{getMedalEmoji(entry.rank)}</Text>
                        </View>
                    </View>
                ))}
            </View>
        </View>
    );
}


const DEBUG_MODE = false;

export default function LeaderboardScreen() {
    const [data, setData] = useState<LeaderboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const fetchLeaderboard = useCallback(async () => {
        try {
            setLoading(true);
            const response = await apiFetch('/api/v1/gamification/leaderboard');

            if (!response.ok) {
                throw new Error('Failed to fetch leaderboard');
            }

            const leaderboardData = await response.json();

            // Transform data to match UI requirements
            const allTime = leaderboardData.map((item: any, index: number) => ({
                rank: index + 1,
                username: item.username,
                score: item.score
            }));

            // Mock monthly data for now as backend doesn't provide it yet
            const monthly = [...allTime].sort((a, b) => b.score - a.score).slice(0, 5);

            // Find current user in the list or mock it if not found
            // In a real app, we should fetch current user rank from backend
            const currentUser = allTime.find((item: any) => item.username === "You") || {
                rank: 0,
                username: "You",
                score: 0
            };

            setData({
                currentUser,
                allTime,
                monthly
            });
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Unknown error');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchLeaderboard();
    }, [fetchLeaderboard]);

    const updateScore = async (newScore: number) => {
        // API endpoint for updating score is likely different too, 
        // based on GamificationController it might not even exist yet or be different.
        // verified GamificationController has no update-score endpoint.
        // disabling for now to prevent errors.
        console.warn("Update score not implemented in backend yet");
        // await apiFetch('/api/v1/leaderboard/update-score', { ... });
        // fetchLeaderboard(); 
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
                <Text style={styles.errorText}>⚠️ {error || 'Failed to load leaderboard'}</Text>
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

                {/* All Time Leaderboard */}
                <LeaderboardTable
                    title="Greatest Of All Time"
                    data={data.allTime}
                    type="allTime"
                />

                {/* Monthly Leaderboard */}
                <LeaderboardTable
                    title="Greatest Of The Month"
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
        fontWeight: 'bold',
        fontStyle: 'italic',
        color: '#1a1a2e',
        marginBottom: 12,
        textAlign: 'center',
    },
    table: {
        borderRadius: 12,
        overflow: 'hidden',
        backgroundColor: '#fff',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    headerRow: {
        flexDirection: 'row',
        backgroundColor: '#e0e0e0',
        paddingVertical: 10,
        paddingHorizontal: 8,
        borderBottomWidth: 2,
        borderBottomColor: '#ccc',
    },
    headerCell: {
        fontWeight: 'bold',
        fontSize: 14,
        color: '#333',
        textAlign: 'center',
    },
    dataRow: {
        flexDirection: 'row',
        paddingVertical: 12,
        paddingHorizontal: 8,
        borderBottomWidth: 1,
        borderBottomColor: 'rgba(255,255,255,0.3)',
    },
    dataCell: {
        fontSize: 14,
        fontWeight: '600',
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
