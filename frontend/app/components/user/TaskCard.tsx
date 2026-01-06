import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

interface TaskCardProps {
    title: string;
    species: string[];
    description: string;
    imagesClassified: number;
    progress: number; // 0 to 100
    onPlay: () => void;
    actionType?: 'play' | 'add';
    onPress?: () => void; // For card detail navigation
    onRemove?: () => void; // For removing task
}

const TaskCard: React.FC<TaskCardProps> = ({
    title,
    species,
    description,
    imagesClassified,
    progress,
    onPlay,
    actionType = 'play',
    onPress,
    onRemove
}) => {
    return (
        <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.9}>
            <View style={styles.headerRow}>
                <Text style={styles.title} numberOfLines={1}>{title}</Text>

                <View style={styles.actionsRow}>
                    {/* Remove Button (Only if onRemove provided) */}
                    {onRemove && (
                        <TouchableOpacity style={styles.removeButton} onPress={onRemove}>
                            <Ionicons name="trash-outline" size={20} color="#EF4444" />
                        </TouchableOpacity>
                    )}

                    {/* Main Action Button */}
                    <TouchableOpacity style={styles.playButton} onPress={onPlay}>
                        {actionType === 'play' ? (
                            <Ionicons name="play-circle" size={32} color="#4B7BE5" />
                        ) : (
                            <Ionicons name="add-circle" size={32} color="#10B981" />
                        )}
                    </TouchableOpacity>
                </View>
            </View>

            <View style={styles.contentRow}>
                <Text style={styles.label}>Species: </Text>
                <View style={styles.speciesContainer}>
                    {species.map((s, index) => (
                        <View key={index} style={styles.speciesItem}>
                            <Text style={styles.speciesText}>{s}</Text>
                            <Ionicons name="image-outline" size={14} color="#555" />
                        </View>
                    ))}
                </View>
            </View>

            <Text style={styles.description} numberOfLines={2}>
                Description: {description}
            </Text>

            <Text style={styles.statsText}>Images Classified: {imagesClassified}</Text>

            <View style={styles.progressContainer}>
                <Text style={styles.statsText}>Progression: {progress}%</Text>
                {/* Simple Progress Bar */}
                <View style={styles.progressBarBg}>
                    <View style={[styles.progressBarFill, { width: `${progress}%` }]} />
                </View>
            </View>
        </TouchableOpacity>
    );
};

const styles = StyleSheet.create({
    card: {
        backgroundColor: '#fff',
        borderRadius: 16,
        padding: 16,
        marginBottom: 16,
        borderWidth: 1,
        borderColor: '#e0e0e0',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    headerRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 8,
    },
    actionsRow: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    title: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#000',
        flex: 1,
    },
    playButton: {
        marginLeft: 8,
    },
    removeButton: {
        padding: 4,
        marginRight: 4,
    },
    contentRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        marginBottom: 8,
    },
    label: {
        fontSize: 14,
        color: '#333',
    },
    speciesContainer: {
        flex: 1,
        flexDirection: 'row',
        flexWrap: 'wrap',
    },
    speciesItem: {
        flexDirection: 'row',
        alignItems: 'center',
        marginRight: 12,
        marginBottom: 4,
    },
    speciesText: {
        fontSize: 14,
        fontWeight: '600',
        marginRight: 4,
    },
    description: {
        fontSize: 13,
        color: '#444',
        marginBottom: 12,
        fontStyle: 'italic',
    },
    statsText: {
        fontSize: 13,
        color: '#333',
        marginBottom: 4,
    },
    progressContainer: {
        marginTop: 4,
    },
    progressBarBg: {
        height: 6,
        backgroundColor: '#E0E0E0',
        borderRadius: 3,
        marginTop: 4,
        overflow: 'hidden',
    },
    progressBarFill: {
        height: '100%',
        backgroundColor: '#4CAF50', // Green for progress
    }
});

export default TaskCard;
