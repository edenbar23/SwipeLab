import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
    Image,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from "react-native";

type GoldImageData = {
    id: number;
    imageUrl: string;
    correctLabelName: string;
    difficultyLevel?: string;
};

type Props = {
    goldImage: GoldImageData;
    onDelete: () => void;
};

export default function GoldImageCard({ goldImage, onDelete }: Props) {
    return (
        <View style={styles.card}>
            {/* Image Thumbnail */}
            <View style={styles.imageContainer}>
                <Image
                    source={{ uri: goldImage.imageUrl }}
                    style={styles.thumbnail}
                    resizeMode="cover"
                />
            </View>

            {/* Content */}
            <View style={styles.content}>
                <Text style={styles.species} numberOfLines={1}>
                    {goldImage.correctLabelName}
                </Text>
                {goldImage.difficultyLevel && (
                    <Text style={styles.difficulty}>
                        Level: {goldImage.difficultyLevel}
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
