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
    const isDark = theme === 'dark';
    const { setActiveTaskId } = useSwipeStore();

    const totalImages      = task.progress?.totalImages    ?? task.totalImages    ?? 0;
    const imagesClassified = task.progress?.imagesClassified ?? task.imagesClassified ?? 0;
    const pending          = totalImages - imagesClassified;
    const progressPct      = totalImages > 0 ? Math.min(imagesClassified / totalImages, 1) : 0;

    const cardBg    = isDark ? themeColors.card : '#F0F7FF';
    const borderCol = isDark ? themeColors.border : '#BFDBFE';

    const handlePlay = () => {
        setActiveTaskId(task.id ?? task.taskId);
        navigation.navigate('SwipeLab');
    };

    return (
        <View style={[styles.root, { backgroundColor: themeColors.background }]}>
            {/* ── Header ───────────────────────────────────────────────────────── */}
            <View style={[styles.header, { backgroundColor: themeColors.card, borderBottomColor: borderCol }]}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
                    <Ionicons name="chevron-back" size={18} color="#3B82F6" />
                    <Text style={styles.backText}>Back</Text>
                </TouchableOpacity>
                <Text style={[styles.headerTitle, { color: themeColors.text }]} numberOfLines={1}>
                    Task Details
                </Text>
                <View style={{ width: 60 }} />
            </View>

            <ScrollView
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* ── Hero card ────────────────────────────────────────────────── */}
                <View style={[styles.heroCard, { backgroundColor: cardBg, borderColor: borderCol }]}>
                    <View style={styles.heroStripe} />
                    <View style={styles.heroBody}>
                        <Text style={[styles.heroTitle, { color: themeColors.text }]}>
                            {task.name}
                        </Text>

                        {!!(task.motivation || task.description) && (
                            <Text style={[styles.heroDescription, { color: themeColors.textSecondary }]}>
                                {task.motivation || task.description}
                            </Text>
                        )}

                        {/* ── Progress bar ──────────────────────────────────────── */}
                        <View style={styles.progressSection}>
                            <View style={styles.progressLabelRow}>
                                <Ionicons name="stats-chart-outline" size={13} color="#3B82F6" style={{ marginRight: 4 }} />
                                <Text style={[styles.progressLabel, { color: themeColors.textSecondary }]}>
                                    Classification Progress
                                </Text>
                                <Text style={styles.progressPct}>{Math.round(progressPct * 100)}%</Text>
                            </View>
                            <View style={[styles.progressTrack, { backgroundColor: isDark ? '#3a3a5a' : '#DBEAFE' }]}>
                                <View style={[styles.progressFill, { width: `${progressPct * 100}%` as any }]} />
                            </View>
                            <Text style={[styles.progressSub, { color: themeColors.textSecondary }]}>
                                {imagesClassified} of {totalImages} images classified
                            </Text>
                        </View>

                        {/* ── Stat chips ────────────────────────────────────────── */}
                        <View style={styles.statsRow}>
                            <StatChip
                                icon="checkmark-done-outline"
                                label="Classified"
                                value={String(imagesClassified)}
                                isDark={isDark}
                                themeSecondary={themeColors.textSecondary}
                            />
                            <StatChip
                                icon="time-outline"
                                label="Pending"
                                value={String(pending)}
                                isDark={isDark}
                                themeSecondary={themeColors.textSecondary}
                            />
                            <StatChip
                                icon="leaf-outline"
                                label="Species"
                                value={String((task.targetSpecies || []).length)}
                                isDark={isDark}
                                themeSecondary={themeColors.textSecondary}
                            />
                        </View>
                    </View>
                </View>

                {/* ── Target Species ───────────────────────────────────────────── */}
                {(task.targetSpecies || []).length > 0 && (
                    <>
                        <SectionHeader icon="leaf-outline" title="Target Species" />
                        <View style={[styles.infoCard, { backgroundColor: cardBg, borderColor: borderCol }]}>
                            {(task.targetSpecies || []).map((s: any, index: number) => {
                                const name = s.name ?? s;
                                const commonName = s.commonName;
                                return (
                                    <View
                                        key={index}
                                        style={[
                                            styles.speciesRow,
                                            { borderBottomColor: borderCol },
                                            index === (task.targetSpecies || []).length - 1 && { borderBottomWidth: 0 },
                                        ]}
                                    >
                                        <View style={[styles.speciesIconWrap, { backgroundColor: isDark ? '#1e3a5f' : '#DBEAFE' }]}>
                                            <Ionicons name="leaf" size={16} color="#3B82F6" />
                                        </View>
                                        <View style={{ flex: 1 }}>
                                            <Text style={[styles.speciesName, { color: themeColors.text }]}>
                                                {commonName || name}
                                            </Text>
                                            {commonName && name && commonName !== name && (
                                                <Text style={[styles.speciesSci, { color: themeColors.textSecondary }]}>
                                                    {name}
                                                </Text>
                                            )}
                                        </View>
                                        <Ionicons name="chevron-forward" size={14} color={borderCol} />
                                    </View>
                                );
                            })}
                        </View>
                    </>
                )}
            </ScrollView>

            {/* ── Footer CTA ───────────────────────────────────────────────────── */}
            <View style={[styles.footer, { backgroundColor: themeColors.card, borderTopColor: borderCol }]}>
                <TouchableOpacity style={styles.playButton} onPress={handlePlay} activeOpacity={0.85}>
                    <Ionicons name="play-circle-outline" size={22} color="#fff" style={{ marginRight: 8 }} />
                    <Text style={styles.playButtonText}>Start Classifying</Text>
                    <Ionicons name="arrow-forward" size={18} color="#fff" style={{ marginLeft: 6 }} />
                </TouchableOpacity>
            </View>
        </View>
    );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function SectionHeader({ icon, title }: { icon: any; title: string }) {
    return (
        <View style={styles.sectionHeader}>
            <Ionicons name={icon} size={16} color="#3B82F6" style={{ marginRight: 6 }} />
            <Text style={styles.sectionTitle}>{title}</Text>
        </View>
    );
}

function StatChip({
    icon, label, value, isDark, themeSecondary,
}: {
    icon: any; label: string; value: string; isDark: boolean; themeSecondary: string;
}) {
    return (
        <View style={[
            styles.statChip,
            { backgroundColor: isDark ? '#1e3a5f' : '#EFF6FF', borderColor: isDark ? '#2a4a7f' : '#BFDBFE' },
        ]}>
            <Ionicons name={icon} size={14} color="#3B82F6" style={{ marginBottom: 2 }} />
            <Text style={styles.statValue}>{value}</Text>
            <Text style={[styles.statLabel, { color: themeSecondary }]}>{label}</Text>
        </View>
    );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
    root: {
        flex: 1,
    },

    // Header
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderBottomWidth: 1,
    },
    backBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 3,
        width: 60,
    },
    backText: {
        fontSize: 14,
        color: '#3B82F6',
        fontWeight: '500',
    },
    headerTitle: {
        fontSize: 17,
        fontWeight: '700',
        flex: 1,
        textAlign: 'center',
    },

    // Scroll
    scrollContent: {
        padding: 16,
        paddingBottom: 24,
    },

    // Hero card
    heroCard: {
        flexDirection: 'row',
        borderRadius: 16,
        borderWidth: 1,
        overflow: 'hidden',
        marginBottom: 20,
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.1,
        shadowRadius: 8,
        elevation: 4,
    },
    heroStripe: {
        width: 5,
        backgroundColor: '#3B82F6',
    },
    heroBody: {
        flex: 1,
        padding: 16,
    },
    heroTitle: {
        fontSize: 22,
        fontWeight: '800',
        lineHeight: 28,
        marginBottom: 8,
    },
    heroDescription: {
        fontSize: 14,
        lineHeight: 20,
        marginBottom: 16,
        fontStyle: 'italic',
    },

    // Progress
    progressSection: {
        marginBottom: 16,
    },
    progressLabelRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 6,
    },
    progressLabel: {
        fontSize: 13,
        flex: 1,
    },
    progressPct: {
        fontSize: 13,
        fontWeight: '700',
        color: '#3B82F6',
    },
    progressTrack: {
        height: 8,
        borderRadius: 4,
        overflow: 'hidden',
        marginBottom: 4,
    },
    progressFill: {
        height: '100%',
        backgroundColor: '#3B82F6',
        borderRadius: 4,
    },
    progressSub: {
        fontSize: 11,
        textAlign: 'right',
    },

    // Stat chips
    statsRow: {
        flexDirection: 'row',
        gap: 8,
    },
    statChip: {
        flex: 1,
        alignItems: 'center',
        paddingVertical: 10,
        paddingHorizontal: 6,
        borderRadius: 12,
        borderWidth: 1,
        gap: 2,
    },
    statValue: {
        fontSize: 15,
        fontWeight: '800',
        color: '#3B82F6',
    },
    statLabel: {
        fontSize: 9,
        textAlign: 'center',
        fontWeight: '500',
        textTransform: 'uppercase',
        letterSpacing: 0.3,
    },

    // Section header
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 10,
        marginTop: 4,
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#3B82F6',
    },

    // Info card (species list)
    infoCard: {
        borderRadius: 14,
        borderWidth: 1,
        overflow: 'hidden',
        marginBottom: 20,
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.06,
        shadowRadius: 5,
        elevation: 2,
    },
    speciesRow: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
        paddingHorizontal: 14,
        borderBottomWidth: 1,
        gap: 12,
    },
    speciesIconWrap: {
        width: 34,
        height: 34,
        borderRadius: 17,
        alignItems: 'center',
        justifyContent: 'center',
    },
    speciesName: {
        fontSize: 14,
        fontWeight: '600',
    },
    speciesSci: {
        fontSize: 12,
        fontStyle: 'italic',
        marginTop: 1,
    },

    // Footer
    footer: {
        padding: 16,
        borderTopWidth: 1,
    },
    playButton: {
        backgroundColor: '#3B82F6',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 15,
        borderRadius: 14,
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    playButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '700',
    },
});
