import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute } from '@react-navigation/native';
import React from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { Colors } from '../../../constants/theme';
import { useThemeStore } from '../../stores/themeStore';
import { useSwipeStore } from '../../stores/swipeStore';

export default function TaskDetailsScreen() {
    const navigation = useNavigation<any>();
    const route = useRoute<any>();
    const { task } = route.params;
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const { setActiveTaskId } = useSwipeStore();

    const handlePlay = () => {
        // Persist chosen task in global store so SwipeScreen can read it without nav params
        setActiveTaskId(task.id);
        navigation.navigate('SwipeLab');
    };

    return (
        <View style={[styles.container, { backgroundColor: themeColors.background }]}>
            <View style={[styles.header, { backgroundColor: themeColors.card, borderBottomColor: themeColors.border }]}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={24} color={themeColors.text} />
                </TouchableOpacity>
                <Text style={[styles.headerTitle, { color: themeColors.text }]}>Task Details</Text>
                <View style={{ width: 24 }} />
            </View>

            <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>

                {/* Title & Stats */}
                <Text style={[styles.title, { color: themeColors.text }]}>{task.name}</Text>

                <View style={[styles.statsRow, { backgroundColor: themeColors.card, shadowColor: themeColors.text }]}>
                    <View style={styles.statItem}>
                        <Text style={styles.statValue}>{task.progress?.imagesClassified ?? task.imagesClassified ?? 0}</Text>
                        <Text style={[styles.statLabel, { color: themeColors.textSecondary }]}>Classified</Text>
                    </View>
                    <View style={styles.statItem}>
                        {/* Approximate pending based on total */}
                        <Text style={styles.statValue}>
                            {(task.progress?.totalImages ?? task.totalImages ?? 0) - (task.progress?.imagesClassified ?? task.imagesClassified ?? 0)}
                        </Text>
                        <Text style={[styles.statLabel, { color: themeColors.textSecondary }]}>Pending</Text>
                    </View>
                    <View style={styles.statItem}>
                        <Text style={styles.statValue}>
                            {(task.progress?.totalImages ?? task.totalImages)
                                ? Math.round(((task.progress?.imagesClassified ?? task.imagesClassified ?? 0) / (task.progress?.totalImages ?? task.totalImages)) * 100)
                                : 0}%
                        </Text>
                        <Text style={[styles.statLabel, { color: themeColors.textSecondary }]}>Progress</Text>
                    </View>
                </View>

                {/* Motivation / Description */}
                <View style={styles.section}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Motivation</Text>
                    <Text style={[styles.bodyText, { color: themeColors.textSecondary }]}>
                        {task.motivation || task.description || "No description provided."}
                    </Text>
                </View>

                {/* Species */}
                <View style={styles.section}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Target Species</Text>
                    <View style={styles.speciesList}>
                        {(task.targetSpecies || []).map((s: any, index: number) => (
                            <View key={index} style={[styles.speciesCard, { backgroundColor: themeColors.card }]}>
                                {/* Placeholder image for species */}
                                <View style={[styles.speciesImagePlaceholder, { backgroundColor: themeColors.background }]}>
                                    <Ionicons name="image" size={30} color={themeColors.icon} />
                                </View>
                                <Text style={[styles.speciesName, { color: themeColors.text }]}>{s.name ?? s}</Text>
                            </View>
                        ))}
                    </View>
                </View>

                {/* Instructions or Additional Info could go here */}

            </ScrollView>

            <View style={[styles.footer, { backgroundColor: themeColors.card, borderTopColor: themeColors.border }]}>
                <TouchableOpacity style={styles.playButton} onPress={handlePlay}>
                    <Text style={styles.playButtonText}>Start Classifying</Text>
                    <Ionicons name="arrow-forward" size={20} color="#fff" style={{ marginLeft: 8 }} />
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f8f9fa',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 12,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#eee',
    },
    backButton: {
        padding: 4,
    },
    headerTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#333',
    },
    content: {
        padding: 20,
    },
    title: {
        fontSize: 26,
        fontWeight: 'bold',
        color: '#111',
        marginBottom: 20,
    },
    statsRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 24,
        backgroundColor: '#fff',
        padding: 16,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOpacity: 0.05,
        shadowRadius: 5,
        elevation: 2,
    },
    statItem: {
        alignItems: 'center',
    },
    statValue: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#4B7BE5',
    },
    statLabel: {
        fontSize: 12,
        color: '#666',
        marginTop: 4,
    },
    section: {
        marginBottom: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#333',
        marginBottom: 12,
    },
    bodyText: {
        fontSize: 15,
        color: '#444',
        lineHeight: 22,
    },
    speciesList: {
        flexDirection: 'row',
        flexWrap: 'wrap',
    },
    speciesCard: {
        backgroundColor: '#fff',
        borderRadius: 8,
        padding: 12,
        marginRight: 12,
        marginBottom: 12,
        alignItems: 'center',
        width: 100,
        shadowColor: '#000',
        shadowOpacity: 0.05,
        shadowRadius: 3,
        elevation: 1,
    },
    speciesImagePlaceholder: {
        width: 50,
        height: 50,
        backgroundColor: '#f0f0f0',
        borderRadius: 25,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 8,
    },
    speciesName: {
        fontSize: 12,
        textAlign: 'center',
        fontWeight: '500',
        color: '#333',
    },
    footer: {
        padding: 16,
        backgroundColor: '#fff',
        borderTopWidth: 1,
        borderTopColor: '#eee',
    },
    playButton: {
        backgroundColor: '#4B7BE5',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 14,
        borderRadius: 12,
    },
    playButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: 'bold',
    },
});
