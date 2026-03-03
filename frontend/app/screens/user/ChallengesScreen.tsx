import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Image,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { apiFetch } from '../../api/apiFetch';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import { Ionicons } from '@expo/vector-icons';
import { API_ENDPOINTS } from '../../api/apiEndpoints';


interface Challenge {
    id: number;
    title: string;
    description: string;
    type: 'DAILY' | 'WEEKLY' | 'STREAK' | 'MILESTONE';
    progress: number;
    total: number;
    reward: string;
    bgColor: string;
    icon: string;
}

function ChallengeCard({ challenge }: { challenge: Challenge }) {
    const progressPercent = Math.min(100, Math.max(0, (challenge.progress / challenge.total) * 100));

    return (
        <View style={[styles.card, { backgroundColor: challenge.bgColor }]}>
            {/* Header Row: Title */}
            <Text style={styles.cardTitle}>{challenge.title}</Text>

            {/* Description */}
            <Text style={styles.cardDesc}>{challenge.description}</Text>

            {/* Content Row: Progress & Icon */}
            <View style={styles.cardContent}>
                <View style={styles.progressSection}>
                    <Text style={styles.progressText}>
                        Progression: {Math.round(progressPercent)}%
                    </Text>
                    <Text style={styles.progressText}>
                        Completed: {challenge.progress}/{challenge.total}
                    </Text>
                    {/* Progress Bar */}
                    <View style={styles.progressBarTrack}>
                        <View
                            style={[
                                styles.progressBarFill,
                                { width: `${progressPercent}%` }
                            ]}
                        />
                    </View>
                </View>

                {/* Reward/Icon Container */}
                <View style={styles.iconContainer}>
                    <Text style={styles.iconText}>{challenge.icon}</Text>
                </View>
            </View>
        </View>
    );
}

export default function ChallengesScreen() {
    const [challenges, setChallenges] = useState<Challenge[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const fetchChallenges = async () => {
        try {
            const res = await apiFetch(API_ENDPOINTS.GAMIFICATION.CHALLENGES);
            if (res.ok) {
                const data = await res.json();
                setChallenges(data);
            }
        } catch (error) {
            console.error('Failed to fetch challenges', error);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        fetchChallenges();
    }, []);

    const onRefresh = () => {
        setRefreshing(true);
        fetchChallenges();
    };

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#4B7BE5" />
            </View>
        );
    }

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/challenges.png')}
            leftTitle="Challenges"
            centerIcon={require('../../../assets/images/leaderboard.png')}
            centerTitle="Leaderboard"
            onCenterPress={() => navigation.navigate('Leaderboard')}
            rightIcon={<Ionicons name="play" size={28} color={themeColors.text} />}
            rightTitle="Play"
            onRightPress={() => navigation.navigate('SwipeLab')}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={[styles.container, { backgroundColor: themeColors.background }]}
                contentContainerStyle={styles.content}
                showsVerticalScrollIndicator={false}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
            >


                {/* In Progress Section */}
                <Text style={[styles.sectionHeader, { color: themeColors.text }]}>In Progress</Text>
                {challenges.filter(c => c.progress < c.total).map(challenge => (
                    <ChallengeCard key={challenge.id} challenge={challenge} />
                ))}

                {/* Completed Section */}
                <Text style={[styles.sectionHeader, { color: themeColors.text }]}>Completed</Text>
                {challenges.filter(c => c.progress >= c.total).map(challenge => (
                    <ChallengeCard key={challenge.id} challenge={challenge} />
                ))}

                <View style={{ height: 40 }} />
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    content: {
        padding: 16,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    header: {
        alignItems: 'center',
        marginBottom: 20,
        marginTop: 10,
    },
    headerIconContainer: {
        alignItems: 'center',
    },
    headerIconEmoji: {
        fontSize: 48,
        color: '#FFD700', // Gold/Idea color
        marginBottom: 8,
    },
    headerTitle: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#003366', // Dark Blue
    },
    sectionHeader: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#444',
        marginTop: 16,
        marginBottom: 8,
        marginLeft: 4,
    },
    card: {
        borderRadius: 16,
        padding: 16,
        marginBottom: 16,
        // Shadow
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
        borderWidth: 1,
        borderColor: 'rgba(0,0,0,0.05)',
    },
    cardTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#000',
        marginBottom: 4,
        textAlign: 'center',
    },
    cardDesc: {
        fontSize: 14,
        color: '#333',
        textAlign: 'center',
        marginBottom: 12,
    },
    cardContent: {
        flexDirection: 'row',
        alignItems: 'flex-end',
        justifyContent: 'space-between',
    },
    progressSection: {
        flex: 1,
        marginRight: 16,
    },
    progressText: {
        fontSize: 12,
        color: '#444',
        marginBottom: 2,
    },
    progressBarTrack: {
        height: 8,
        backgroundColor: 'rgba(255,255,255,0.5)', // Semi-transparent white
        borderRadius: 4,
        marginTop: 6,
        overflow: 'hidden',
        borderWidth: 0.5,
        borderColor: 'rgba(0,0,0,0.1)',
    },
    progressBarFill: {
        height: '100%',
        backgroundColor: '#4B7BE5', // Default fill, maybe override?
        borderRadius: 4,
    },
    iconContainer: {
        justifyContent: 'center',
        alignItems: 'center',

    },
    iconText: {
        fontSize: 32,
    },
});