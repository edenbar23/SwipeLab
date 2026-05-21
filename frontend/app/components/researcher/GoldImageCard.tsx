import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
    Image,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from "react-native";
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '../../stores/themeStore';

type GoldImageData = {
    id: number;
    imageUrl?: string;
    species?: string;
    correctAnswer?: string;
};

type Props = {
    goldImage: GoldImageData;
    onDelete: () => void;
};

export default function GoldImageCard({ goldImage, onDelete }: Props) {
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const finalUri = goldImage.imageUrl
        || `https://via.placeholder.com/300/E2E8F0/64748B?text=${goldImage.species || 'Image'}`;

    return (
        <View style={[styles.card, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            {/* Image Thumbnail */}
            {/* pointerEvents on View, not Image — Image doesn't accept this prop */}
            <View style={styles.imageContainer} pointerEvents="none">
                <Image
                    source={{ uri: finalUri }}
                    style={styles.thumbnail}
                    resizeMode="cover"
                />
            </View>

            {/* Content */}
            <View style={styles.content}>
                <Text style={[styles.species, { color: themeColors.text }]} numberOfLines={1}>
                    {goldImage.species || "Unknown Species"}
                </Text>
                {goldImage.correctAnswer && (
                    <Text style={[styles.difficulty, { color: themeColors.textSecondary }]}>
                        Answer: {goldImage.correctAnswer}
                    </Text>
                )}
            </View>

            {/* Delete Button */}
            <TouchableOpacity
                style={styles.deleteButton}
                onPress={onDelete}
                activeOpacity={0.6}
            >
                <Ionicons name="trash-outline" size={22} color="#EF4444" />
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    card: {
        backgroundColor: "#F8FAFC",
        borderRadius: 14,
        padding: 12,
        marginBottom: 12,
        flexDirection: "row",
        alignItems: "center",
        borderWidth: 1,
        borderColor: "#E2E8F0",
    },
    imageContainer: {
        width: 60,
        height: 60,
        borderRadius: 8,
        overflow: "hidden",
        backgroundColor: "#E2E8F0",
    },
    thumbnail: {
        width: "100%",
        height: "100%",
    },
    content: {
        flex: 1,
        marginLeft: 12,
    },
    species: {
        fontSize: 16,
        fontWeight: "600",
        color: "#1F2937",
    },
    difficulty: {
        fontSize: 13,
        color: "#6B7280",
        marginTop: 4,
    },
    deleteButton: {
        padding: 8,
    },
});
