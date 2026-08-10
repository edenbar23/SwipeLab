import React from 'react';
import { Image, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { CollectionItem, SwipeDirection } from '@/types';
import { useThemeStore } from '@/stores/themeStore';
import { Colors } from '../../../constants/theme';
import { parseImageUrl } from '@/utils/imageUtils';

interface CollectionCardProps {
    item: CollectionItem;
}

const LABEL_CONFIG: Record<SwipeDirection, { color: string; bgColor: string; text: string; emoji: string }> = {
    yes: { color: '#16a34a', bgColor: '#dcfce7', text: 'Collected', emoji: '' },
    no: { color: '#dc2626', bgColor: '#fee2e2', text: 'No', emoji: '✗' },
    'dont-know': { color: '#ca8a04', bgColor: '#fef9c3', text: "Don't Know", emoji: '?' },
    trash: { color: '#6b7280', bgColor: '#f3f4f6', text: 'Trash', emoji: '🗑' },
};

export default function CollectionCard({ item }: CollectionCardProps) {
    const navigation = useNavigation<any>();
    const labelConfig = LABEL_CONFIG[item.label];
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffTime = Math.abs(now.getTime() - date.getTime());
        const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 0) return 'Today';
        if (diffDays === 1) return 'Yesterday';
        if (diffDays < 7) return `${diffDays} days ago`;
        return date.toLocaleDateString();
    };

    const handlePress = () => {
        navigation.navigate('CollectionDetails', { item });
    };

    return (
        <TouchableOpacity
            style={[styles.card, { backgroundColor: themeColors.card }]}
            onPress={handlePress}
            activeOpacity={0.8}
            accessibilityLabel={item.speciesName || item.taskName}
            testID={`collection-item-${item.id}`} // Helpful for testing too
        >
            <Image
                source={{ uri: parseImageUrl(item.imageUrl) }}
                style={styles.image}
                resizeMode="cover"
            />
            <View style={styles.overlay}>
                <View style={[styles.labelBadge, { backgroundColor: labelConfig.bgColor }]}>
                    <Text style={[styles.labelText, { color: labelConfig.color }]}>
                        {labelConfig.emoji}{labelConfig.emoji ? ' ' : ''}{labelConfig.text}
                    </Text>
                </View>
            </View>
            <View style={styles.nameOverlay}>
                <Text style={styles.nameText} numberOfLines={2}>
                    {item.speciesName || item.taskName}
                </Text>
            </View>
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    card: {
        width: '32%', // 3 columns
        backgroundColor: '#fff',
        borderRadius: 12,
        marginBottom: 8, // Smaller margin for tighter grid
        overflow: 'hidden',
        // Square aspect ratio
        aspectRatio: 1,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
    },
    image: {
        width: '100%',
        height: '100%', // Full height
        backgroundColor: '#e5e7eb',
    },
    overlay: {
        position: 'absolute',
        top: 4,
        right: 4,
    },
    nameOverlay: {
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        backgroundColor: 'rgba(0,0,0,0.6)',
        paddingVertical: 2,
        paddingHorizontal: 4,
        alignItems: 'center',
    },
    nameText: {
        fontSize: 12,
        fontWeight: '700',
        color: '#fff',
        textAlign: 'center',
        textShadowColor: 'rgba(0, 0, 0, 0.5)',
        textShadowOffset: { width: 0, height: 1 },
        textShadowRadius: 2,
    },
    labelBadge: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 8,
        backgroundColor: 'rgba(255,255,255,0.9)', // Slight transparency or solid? Using config bg
    },
    labelText: {
        fontSize: 10,
        fontWeight: '700',
    },
    // Info styles removed
});
