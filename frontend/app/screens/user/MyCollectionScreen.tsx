import React, { useCallback, useEffect } from 'react';
import {
    ActivityIndicator,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useCollectionStore, CollectionEntry } from '../../stores/collectionStore';
import { useNavigation } from '@react-navigation/native';
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import AuthenticatedImage from '../../components/ui/AuthenticatedImage';

import { parseImageUrl } from '../../utils/imageUtils';

function formatDate(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' });
}

function CollectionCard({ item, themeColors }: { item: CollectionEntry; themeColors: any }) {
    const parsedImageUrl = parseImageUrl(item.imageUrl ?? undefined);

    return (
        <View style={[styles.card, { backgroundColor: themeColors.card }]}>
            {parsedImageUrl ? (
                <AuthenticatedImage
                    uri={parsedImageUrl}
                    style={styles.cardImage}
                    resizeMode="cover"
                />
            ) : (
                <View style={[styles.cardImagePlaceholder, { backgroundColor: themeColors.border }]}>
                    <Text style={styles.placeholderEmoji}>🐛</Text>
                </View>
            )}
            <View style={styles.cardBody}>
                <Text style={[styles.cardSpecies, { color: themeColors.text }]} numberOfLines={1}>
                    {item.species ?? 'Unknown species'}
                </Text>
                <Text style={[styles.cardDate, { color: themeColors.textSecondary }]}>
                    {formatDate(item.taggedAt)}
                </Text>
            </View>
        </View>
    );
}

export default function MyCollectionScreen() {
    const { items, stats, isLoading, fetchCollection } = useCollectionStore();
    const [refreshing, setRefreshing] = React.useState(false);
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    useEffect(() => {
        fetchCollection();
    }, [fetchCollection]);

    const onRefresh = useCallback(async () => {
        setRefreshing(true);
        await fetchCollection();
        setRefreshing(false);
    }, [fetchCollection]);

    if (isLoading && items.length === 0) {
        return (
            <View style={[styles.loadingContainer, { backgroundColor: themeColors.background }]}>
                <ActivityIndicator size="large" color="#4B7BE5" />
            </View>
        );
    }

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/collection.png')}
            leftTitle="My Collection"
            rightIcon={require('../../../assets/images/stats.png')}
            rightTitle="Stats"
            onRightPress={() => navigation.navigate('Stats')}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={[styles.container, { backgroundColor: themeColors.background }]}
                contentContainerStyle={styles.content}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
            >
                {/* Collection Summary */}
                <View style={styles.statsContainer}>
                    <View style={[styles.statCard, { backgroundColor: themeColors.card }]}>
                        <Text style={styles.statNumber}>{stats.total}</Text>
                        <Text style={[styles.statLabel, { color: themeColors.textSecondary }]}>
                            Tags Collected
                        </Text>
                    </View>
                </View>

                {/* Collection Grid */}
                {items.length === 0 ? (
                    <View style={styles.emptyState}>
                        <Text style={styles.emptyEmoji}>📷</Text>
                        <Text style={[styles.emptyTitle, { color: themeColors.text }]}>No items yet</Text>
                        <Text style={[styles.emptySubtitle, { color: themeColors.textSecondary }]}>
                            Swipe YES on images to add them to your notebook!
                        </Text>
                    </View>
                ) : (
                    <View style={styles.grid}>
                        {items.map((item) => (
                            <CollectionCard key={item.id} item={item} themeColors={themeColors} />
                        ))}
                    </View>
                )}

                <View style={{ height: 40 }} />
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    content: {
        padding: 16,
        width: '100%',
        maxWidth: 600,
        alignSelf: 'center',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    statsContainer: {
        marginBottom: 20,
        alignItems: 'center',
    },
    statCard: {
        paddingVertical: 16,
        paddingHorizontal: 32,
        borderRadius: 16,
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    statNumber: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#4B7BE5',
    },
    statLabel: {
        fontSize: 14,
        marginTop: 4,
        fontWeight: '600',
    },
    grid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 10,
    },
    card: {
        width: '48%',
        borderRadius: 12,
        overflow: 'hidden',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 3,
        elevation: 2,
    },
    cardImage: {
        width: '100%',
        height: 110,
    },
    cardImagePlaceholder: {
        width: '100%',
        height: 110,
        justifyContent: 'center',
        alignItems: 'center',
    },
    placeholderEmoji: {
        fontSize: 36,
    },
    cardBody: {
        padding: 8,
    },
    cardSpecies: {
        fontSize: 13,
        fontWeight: '700',
        marginBottom: 2,
    },
    cardDate: {
        fontSize: 11,
    },
    emptyState: {
        alignItems: 'center',
        paddingVertical: 60,
    },
    emptyEmoji: {
        fontSize: 64,
        marginBottom: 16,
    },
    emptyTitle: {
        fontSize: 20,
        fontWeight: 'bold',
        marginBottom: 8,
    },
    emptySubtitle: {
        fontSize: 14,
        textAlign: 'center',
        paddingHorizontal: 40,
    },
});

