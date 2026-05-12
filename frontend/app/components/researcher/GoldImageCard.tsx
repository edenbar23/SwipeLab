import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
    Image,
    Platform,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from "react-native";
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '../../stores/themeStore';

const BACKEND_BASE_URL = process.env.EXPO_PUBLIC_API_URL ||
  (Platform.OS === 'web' ? 'http://localhost:8080' : 'http://192.168.1.133:8080');

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

    let imageUrl = goldImage.imageUrl;
    if (imageUrl) {
        if (imageUrl.startsWith('/')) {
            imageUrl = `${BACKEND_BASE_URL}${imageUrl}`;
        } else if (/^[A-Za-z0-9+/]/.test(imageUrl) && !imageUrl.startsWith('http') && !imageUrl.startsWith('data:')) {
            imageUrl = `data:image/jpeg;base64,${imageUrl}`;
        }
    }
    const finalUri = imageUrl || `https://via.placeholder.com/300/E2E8F0/64748B?text=${goldImage.species || 'Image'}`;

    return (
        <View style={[styles.card, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            {/* Image Thumbnail */}
            <View style={styles.imageContainer}>
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
                onPress={(e) => {
                    onDelete();
                }}
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
