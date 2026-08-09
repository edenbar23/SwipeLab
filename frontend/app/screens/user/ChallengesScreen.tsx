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
import { apiFetch } from '@/api/apiFetch';
import ScreenHeaderLayout from '@/components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useThemeStore } from '@/stores/themeStore';
import { Colors } from '../../../constants/theme';
import { Ionicons } from '@expo/vector-icons';
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { useChallenges } from '@/api/queries';


interface ChallengeDto {
    challengeId: string;
    name: string;
    description: string;
    progress: number;
    target: number;
    completed: boolean;
    badge: {
        title: string;
        iconUrl: string;
    } | null;
}

const CARD_COLORS = [
    '#FFD700', // Gamified Gold/Yellow
    '#00FA9A', // Vibrant Spring Green
    '#00BFFF', // Deep Sky Blue
    '#FF69B4', // Hot Pink
    '#FFA500', // Bright Orange
    '#ADFF2F', // Electric Lime
];

function ChallengeCard({ challenge, index }: { challenge: ChallengeDto, index: number }) {
    const progressPercent = Math.min(100, Math.max(0, (challenge.progress / challenge.target) * 100));
    const bgColor = CARD_COLORS[index % CARD_COLORS.length];

    return (
        <View style={[styles.card, { backgroundColor: bgColor }]}>
            {/* Header Row: Title */}
            <Text style={styles.cardTitle}>{challenge.name}</Text>

            {/* Description */}
            <Text style={styles.cardDesc}>{challenge.description}</Text>

            {/* Reward/Icon Container */}
            <View style={styles.iconContainer}>
                {challenge.badge?.iconUrl ? (
                     <Image 
                        source={{ uri: challenge.badge.iconUrl }}
                        style={{ width: 48, height: 48, resizeMode: 'contain' }}
                     />
                ) : (
                     <Text style={styles.iconText}>🏆</Text>
                )}
            </View>

            {/* Content Row: Progress */}
            <View style={styles.cardContent}>
                <View style={styles.progressSection}>
                    <Text style={styles.progressText}>
                        Progression: {Math.round(progressPercent)}%
                    </Text>
                    <Text style={styles.progressText}>
                        Completed: {challenge.progress}/{challenge.target}
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
            </View>
        </View>
    );
}

export default function ChallengesScreen() {
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const { data: challenges = [], isLoading: loading, refetch, isRefetching } = useChallenges();
    const refreshing = isRefetching;

    const onRefresh = () => {
        refetch();
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
                {challenges.filter((c: ChallengeDto) => !c.completed).map((challenge: ChallengeDto, index: number) => (
                    <ChallengeCard key={challenge.challengeId} challenge={challenge} index={index} />
                ))}

                {/* Completed Section */}
                <Text style={[styles.sectionHeader, { color: themeColors.text }]}>Completed</Text>
                {challenges.filter((c: ChallengeDto) => c.completed).map((challenge: ChallengeDto, index: number) => (
                    <ChallengeCard key={challenge.challengeId} challenge={challenge} index={index} />
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
        padding: 20,
        marginBottom: 16,
        // Shadow
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.2,
        shadowRadius: 8,
        elevation: 5,
    },
    cardTitle: {
        fontSize: 20,
        fontWeight: '900',
        color: '#1a1a2e',
        marginBottom: 8,
        textAlign: 'center',
        letterSpacing: 0.5,
        textTransform: 'uppercase',
    },
    cardDesc: {
        fontSize: 15,
        color: '#333333',
        marginBottom: 16,
        lineHeight: 22,
        textAlign: 'center',
    },
    cardContent: {
        alignItems: 'center',
        width: '100%',
    },
    progressSection: {
        width: '100%',
        alignItems: 'center',
    },
    progressText: {
        fontSize: 13,
        color: '#1a1a2e',
        marginBottom: 4,
        fontWeight: '700',
        textAlign: 'center',
    },
    progressBarTrack: {
        height: 10,
        width: '80%',
        backgroundColor: 'rgba(0,0,0,0.1)', // Subtle dark track
        borderRadius: 5,
        marginTop: 8,
        overflow: 'hidden',
    },
    progressBarFill: {
        height: '100%',
        backgroundColor: '#1a1a2e', // Dark fill for contrast
        borderRadius: 5,
    },
    iconContainer: {
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 16,
    },
    iconText: {
        fontSize: 32,
    },
});