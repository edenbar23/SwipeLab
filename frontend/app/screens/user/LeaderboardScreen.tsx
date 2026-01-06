import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { apiFetch } from '../../api/apiFetch';

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

const getRowStyle = (rank: number, type: 'allTime' | 'monthly') => {
    const allTimeColors = ['#FFD700', '#FFA500', '#FF6347', '#DC143C'];
    const monthlyColors = ['#FFD700', '#FFA500', '#9370DB', '#8B008B'];

    const colors = type === 'allTime' ? allTimeColors : monthlyColors;
    return { backgroundColor: colors[rank - 1] || '#ccc' };
};

interface LeaderboardTableProps {
    title: string;
    data: LeaderboardEntry[];
    type: 'allTime' | 'monthly';
}

function LeaderboardTable({ title, data, type }: LeaderboardTableProps) {
    return (
        <View style={styles.tableContainer}>
            <Text style={styles.tableTitle}>{title}</Text>
            <View style={styles.table}>
                {/* Header Row */}
                <View style={styles.headerRow}>
                    <Text style={[styles.headerCell, styles.rankCol]}>Rank</Text>
                    <Text style={[styles.headerCell, styles.userCol]}>User</Text>
                    <Text style={[styles.headerCell, styles.scoreCol]}>Score</Text>
                </View>

                {/* Data Rows */}
                {data.map((entry) => (
                    <View
                        key={entry.rank}
                        style={[styles.dataRow, getRowStyle(entry.rank, type)]}
                    >
                        <Text style={[styles.dataCell, styles.rankCol]}>{entry.rank}</Text>
                        <Text style={[styles.dataCell, styles.userCol]}>{entry.username}</Text>
                        <View style={styles.scoreContainer}>
                            <Text style={[styles.dataCell, styles.scoreText]}>
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

    const fetchLeaderboard = useCallback(async () => {
        try {
            setLoading(true);
            const response = await apiFetch('/api/v1/leaderboard/all');

            if (!response.ok) {
                throw new Error('Failed to fetch leaderboard');
            }

            const leaderboardData = await response.json();
            setData(leaderboardData);
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
        await apiFetch('/api/v1/leaderboard/update-score', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ score: newScore }),
        });
        fetchLeaderboard(); // Refresh to see new rank
    };

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
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
        <ScrollView style={styles.container} contentContainerStyle={styles.content}>
            {/* Header */}
            <View style={styles.header}>
                <View style={styles.titleRow}>
                    <Text style={styles.trophyIcon}>🏆</Text>
                    <Text style={styles.title}>Leaderboard</Text>
                </View>
                <Text style={styles.yourSpot}>Your Spot: #{data.currentUser.rank}</Text>
            </View>

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
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 20,
    },
    titleRow: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    trophyIcon: {
        fontSize: 28,
        marginRight: 8,
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#1a1a2e',
    },
    yourSpot: {
        fontSize: 16,
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
        color: '#fff',
        textAlign: 'center',
        textShadowColor: 'rgba(0,0,0,0.3)',
        textShadowOffset: { width: 1, height: 1 },
        textShadowRadius: 2,
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
