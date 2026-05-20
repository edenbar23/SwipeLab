import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';

interface TaskCardProps {
    title: string;
    species: string[];
    description: string;
    imagesClassified: number;
    progress: number; // 0 to 100
    onPlay: () => void;
    actionType?: 'play' | 'add';
    onPress?: () => void;
    onRemove?: () => void;
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
    onRemove,
}) => {
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const isDark = theme === 'dark';

    const cardBg    = isDark ? themeColors.card : '#F0F7FF';
    const borderCol = isDark ? themeColors.border : '#BFDBFE';
    const stripeColor = actionType === 'add' ? '#10B981' : '#3B82F6';
    const actionColor = actionType === 'add' ? '#10B981' : '#3B82F6';

    return (
        <TouchableOpacity
            style={[styles.card, { backgroundColor: cardBg, borderColor: borderCol }]}
            onPress={onPress}
            activeOpacity={0.85}
        >
            {/* Left accent stripe */}
            <View style={[styles.stripe, { backgroundColor: stripeColor }]} />

            <View style={styles.body}>
                {/* Header row: title + actions */}
                <View style={styles.headerRow}>
                    <Text style={[styles.title, { color: themeColors.text }]} numberOfLines={1}>
                        {title}
                    </Text>
                    <View style={styles.actionsRow}>
                        {onRemove && (
                            <TouchableOpacity style={styles.iconBtn} onPress={onRemove}>
                                <Ionicons name="trash-outline" size={18} color="#EF4444" />
                            </TouchableOpacity>
                        )}
                        <TouchableOpacity style={styles.iconBtn} onPress={onPlay}>
                            <Ionicons
                                name={actionType === 'play' ? 'play-circle' : 'add-circle'}
                                size={34}
                                color={actionColor}
                            />
                        </TouchableOpacity>
                    </View>
                </View>

                {/* Description */}
                {!!description && (
                    <Text style={[styles.description, { color: themeColors.textSecondary }]} numberOfLines={2}>
                        {description}
                    </Text>
                )}

                {/* Species chips */}
                {species.length > 0 && (
                    <View style={styles.speciesRow}>
                        <Ionicons name="leaf-outline" size={12} color={actionColor} style={{ marginRight: 4 }} />
                        {species.map((s, i) => (
                            <View key={i} style={[styles.speciesChip, { backgroundColor: isDark ? '#1e3a5f' : '#DBEAFE', borderColor: borderCol }]}>
                                <Text style={[styles.speciesChipText, { color: actionColor }]}>{s}</Text>
                            </View>
                        ))}
                    </View>
                )}

                {/* Progress section */}
                <View style={styles.progressSection}>
                    <View style={styles.progressLabelRow}>
                        <Ionicons name="stats-chart-outline" size={12} color={actionColor} style={{ marginRight: 4 }} />
                        <Text style={[styles.progressLabel, { color: themeColors.textSecondary }]}>
                            Progress
                        </Text>
                        <Text style={[styles.progressPct, { color: actionColor }]}>{progress}%</Text>
                    </View>
                    <View style={[styles.progressTrack, { backgroundColor: isDark ? '#3a3a5a' : '#DBEAFE' }]}>
                        <View style={[styles.progressFill, { width: `${progress}%` as any, backgroundColor: stripeColor }]} />
                    </View>
                    <Text style={[styles.progressSub, { color: themeColors.textSecondary }]}>
                        {imagesClassified} images classified
                    </Text>
                </View>
            </View>
        </TouchableOpacity>
    );
};

const styles = StyleSheet.create({
    card: {
        flexDirection: 'row',
        borderRadius: 16,
        borderWidth: 1,
        overflow: 'hidden',
        marginBottom: 14,
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.08,
        shadowRadius: 8,
        elevation: 3,
    },
    stripe: {
        width: 5,
    },
    body: {
        flex: 1,
        padding: 14,
    },
    headerRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 6,
    },
    actionsRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
    },
    iconBtn: {
        padding: 2,
    },
    title: {
        fontSize: 17,
        fontWeight: '800',
        flex: 1,
        marginRight: 8,
        lineHeight: 22,
    },
    description: {
        fontSize: 13,
        lineHeight: 18,
        marginBottom: 10,
        fontStyle: 'italic',
    },
    speciesRow: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        alignItems: 'center',
        marginBottom: 10,
        gap: 6,
    },
    speciesChip: {
        paddingHorizontal: 8,
        paddingVertical: 3,
        borderRadius: 12,
        borderWidth: 1,
    },
    speciesChipText: {
        fontSize: 11,
        fontWeight: '600',
    },
    progressSection: {
        marginTop: 2,
    },
    progressLabelRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 5,
    },
    progressLabel: {
        fontSize: 12,
        flex: 1,
    },
    progressPct: {
        fontSize: 12,
        fontWeight: '700',
    },
    progressTrack: {
        height: 7,
        borderRadius: 4,
        overflow: 'hidden',
        marginBottom: 4,
    },
    progressFill: {
        height: '100%',
        borderRadius: 4,
    },
    progressSub: {
        fontSize: 11,
        textAlign: 'right',
    },
});

export default TaskCard;
