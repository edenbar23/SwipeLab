import React from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useRoute, useNavigation } from '@react-navigation/native';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { CollectionItem, SwipeDirection } from '../../types';
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import { parseImageUrl } from '../../utils/imageUtils';

export default function CollectionDetailsScreen() {
    const route = useRoute<any>();
    const navigation = useNavigation<any>();
    const { item } = route.params as { item: CollectionItem };
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    if (!item) return null;

    const getLabelColor = (label: SwipeDirection) => {
        switch (label) {
            case 'yes': return '#16a34a'; // Green
            case 'no': return '#dc2626'; // Red
            case 'dont-know': return '#ca8a04'; // Yellow
            case 'trash': return '#6b7280'; // Gray
            default: return '#000';
        }
    };

    const getLabelText = (label: SwipeDirection) => {
        switch (label) {
            case 'yes': return 'Collected';
            case 'no': return 'No';
            case 'dont-know': return "Don't Know";
            case 'trash': return 'Trash';
            default: return label;
        }
    };

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/gold_images.png')}
            leftTitle="Details"
            rightIcon={require('../../../assets/images/collection.png')}
            rightTitle="Collection"
            onRightPress={() => navigation.navigate('Collection')}
        >
            <ScrollView style={[styles.container, { backgroundColor: themeColors.background }]} contentContainerStyle={styles.content}>

                {/* Image Section */}
                <View style={[styles.imageContainer, { backgroundColor: themeColors.card }]}>
                    <Image source={{ uri: parseImageUrl(item.imageUrl) }} style={styles.image} resizeMode="cover" />
                    <View style={[styles.labelBadge, { backgroundColor: getLabelColor(item.label) }]}>
                        <Text style={styles.labelText}>{getLabelText(item.label)}</Text>
                    </View>
                </View>

                {/* Info Section */}
                <View style={styles.infoContainer}>
                    <Text style={[styles.speciesName, { color: themeColors.text }]}>{item.speciesName}</Text>
                    <Text style={[styles.scientificName, { color: themeColors.textSecondary }]}>{item.scientificName}</Text>

                    <View style={[styles.separator, { backgroundColor: themeColors.border }]} />

                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Description</Text>
                    <Text style={[styles.description, { color: themeColors.textSecondary }]}>{item.description}</Text>

                    <View style={[styles.separator, { backgroundColor: themeColors.border }]} />

                    <View style={styles.metaRow}>
                        <View style={styles.metaItem}>
                            <Text style={styles.metaLabel}>Task</Text>
                            <Text style={[styles.metaValue, { color: themeColors.text }]}>{item.taskName}</Text>
                        </View>
                        <View style={styles.metaItem}>
                            <Text style={styles.metaLabel}>Date Labeled</Text>
                            <Text style={[styles.metaValue, { color: themeColors.text }]}>{new Date(item.labeledAt).toLocaleDateString()}</Text>
                        </View>
                    </View>
                </View>

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
        paddingBottom: 40,
    },
    imageContainer: {
        width: '100%',
        height: 300,
        position: 'relative',
        backgroundColor: '#f3f4f6',
        borderRadius: 20,
        overflow: 'hidden',
        marginBottom: 24,
    },
    image: {
        width: '100%',
        height: '100%',
    },
    labelBadge: {
        position: 'absolute',
        bottom: 16,
        right: 16,
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
    },
    labelText: {
        color: '#fff',
        fontWeight: 'bold',
        fontSize: 16,
    },
    infoContainer: {
        paddingHorizontal: 8,
    },
    speciesName: {
        fontSize: 28,
        fontWeight: 'bold',
        color: '#1f2937',
        marginBottom: 4,
    },
    scientificName: {
        fontSize: 18,
        color: '#6b7280',
        fontStyle: 'italic',
        marginBottom: 16,
    },
    separator: {
        height: 1,
        backgroundColor: '#e5e7eb',
        marginVertical: 16,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#374151',
        marginBottom: 8,
    },
    description: {
        fontSize: 16,
        color: '#4b5563',
        lineHeight: 24,
    },
    metaRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
    metaItem: {
        flex: 1,
    },
    metaLabel: {
        fontSize: 12,
        color: '#9ca3af',
        textTransform: 'uppercase',
        marginBottom: 4,
    },
    metaValue: {
        fontSize: 14,
        fontWeight: '500',
        color: '#1f2937',
    },
});
