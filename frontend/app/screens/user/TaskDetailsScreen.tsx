import React from 'react';
import { View, Text, StyleSheet, ScrollView, Image, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import UserTopBar from '../../components/user/UserTopBar';

export default function TaskDetailsScreen() {
    const navigation = useNavigation<any>();
    const route = useRoute<any>();
    const { task } = route.params;

    const handlePlay = () => {
        navigation.navigate('SwipeLab');
    };

    return (
        <View style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
                    <Ionicons name="arrow-back" size={24} color="#333" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Task Details</Text>
                <View style={{ width: 24 }} />
            </View>

            <ScrollView contentContainerStyle={styles.content}>

                {/* Title & Stats */}
                <Text style={styles.title}>{task.name}</Text>

                <View style={styles.statsRow}>
                    <View style={styles.statItem}>
                        <Text style={styles.statValue}>{task.imagesClassified}</Text>
                        <Text style={styles.statLabel}>Classified</Text>
                    </View>
                    <View style={styles.statItem}>
                        {/* Approximate pending based on total */}
                        <Text style={styles.statValue}>{task.totalImages - task.imagesClassified}</Text>
                        <Text style={styles.statLabel}>Pending</Text>
                    </View>
                    <View style={styles.statItem}>
                        <Text style={styles.statValue}>{Math.round((task.imagesClassified / task.totalImages) * 100)}%</Text>
                        <Text style={styles.statLabel}>Progress</Text>
                    </View>
                </View>

                {/* Motivation / Description */}
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>Motivation</Text>
                    <Text style={styles.bodyText}>
                        {task.motivation || task.description || "No description provided."}
                    </Text>
                </View>

                {/* Species */}
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>Target Species</Text>
                    <View style={styles.speciesList}>
                        {task.species.map((s: any, index: number) => (
                            <View key={index} style={styles.speciesCard}>
                                {/* Placeholder image for species */}
                                <View style={styles.speciesImagePlaceholder}>
                                    <Ionicons name="image" size={30} color="#ccc" />
                                </View>
                                <Text style={styles.speciesName}>{s.name}</Text>
                            </View>
                        ))}
                    </View>
                </View>

                {/* Instructions or Additional Info could go here */}

            </ScrollView>

            <View style={styles.footer}>
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
