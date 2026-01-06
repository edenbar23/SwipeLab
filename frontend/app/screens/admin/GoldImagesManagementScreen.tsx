// admin screen for managing gold images
import React, { useEffect, useState } from "react";
import { Alert, FlatList, StyleSheet, Text, View } from "react-native";
import { apiFetch } from "../../api/apiFetch";
import GoldImageCard from "../../components/admin/GoldImageCard";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";

type GoldImageResponse = {
    id: number;
    imageId: number;
    imageUrl: string;
    caption: string;
    taskId: number;
    correctLabelId: number;
    correctLabelName: string;
    difficultyLevel: string;
    explanation: string;
    createdAt: string;
};

// Mock data for visualization
const MOCK_GOLD_IMAGES: GoldImageResponse[] = [
    {
        id: 1,
        imageId: 101,
        imageUrl: "https://via.placeholder.com/300/FFB6C1/000000?text=Bee",
        caption: "Insecta - Hymenoptera - Apidae - Apis - mellifera",
        taskId: 1,
        correctLabelId: 1,
        correctLabelName: "Honey Bee (Apis mellifera)",
        difficultyLevel: "EASY",
        explanation: "Common honey bee",
        createdAt: "2026-01-06T10:00:00",
    },
    {
        id: 2,
        imageId: 102,
        imageUrl: "https://via.placeholder.com/300/98FB98/000000?text=Wasp",
        caption: "Insecta - Hymenoptera - Vespidae - Vespa - crabro",
        taskId: 1,
        correctLabelId: 2,
        correctLabelName: "European Hornet (Vespa crabro)",
        difficultyLevel: "MEDIUM",
        explanation: "Large wasp species",
        createdAt: "2026-01-06T10:15:00",
    },
    {
        id: 3,
        imageId: 103,
        imageUrl: "https://via.placeholder.com/300/87CEEB/000000?text=Butterfly",
        caption: "Insecta - Lepidoptera - Nymphalidae - Danaus - plexippus",
        taskId: 1,
        correctLabelId: 3,
        correctLabelName: "Monarch Butterfly (Danaus plexippus)",
        difficultyLevel: "EASY",
        explanation: "Orange and black butterfly",
        createdAt: "2026-01-06T10:30:00",
    },
    {
        id: 4,
        imageId: 104,
        imageUrl: "https://via.placeholder.com/300/DDA0DD/000000?text=Beetle",
        caption: "Insecta - Coleoptera - Coccinellidae - Coccinella - septempunctata",
        taskId: 1,
        correctLabelId: 4,
        correctLabelName: "Ladybug (Coccinella septempunctata)",
        difficultyLevel: "EASY",
        explanation: "Seven-spotted ladybug",
        createdAt: "2026-01-06T10:45:00",
    },
    {
        id: 5,
        imageId: 105,
        imageUrl: "https://via.placeholder.com/300/F0E68C/000000?text=Ant",
        caption: "Insecta - Hymenoptera - Formicidae - Camponotus - pennsylvanicus",
        taskId: 1,
        correctLabelId: 5,
        correctLabelName: "Carpenter Ant (Camponotus pennsylvanicus)",
        difficultyLevel: "HARD",
        explanation: "Large black ant",
        createdAt: "2026-01-06T11:00:00",
    },
];

export default function GoldImagesManagementScreen({ navigation }: any) {
    const [goldImages, setGoldImages] = useState<GoldImageResponse[]>(MOCK_GOLD_IMAGES);
    const [loading, setLoading] = useState(false);

    // For demo purposes, using taskId = 1. In production, this would come from navigation params or context
    const taskId = 1;

    useEffect(() => {
        // Using mock data for now. Uncomment below to fetch real data from API
        // fetchGoldImages();
    }, []);

    const fetchGoldImages = async () => {
        try {
            setLoading(true);
            const res = await apiFetch(`/api/admin/gold-images?taskId=${taskId}`);
            const data = await res.json();
            console.log("Gold Images data:", data);
            setGoldImages(data);
        } catch (err) {
            console.error("API fetch error:", err);
            Alert.alert("Error", "Failed to fetch gold images");
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = (goldImageId: number) => {
        Alert.alert(
            "Delete Gold Image",
            "Are you sure you want to delete this gold image?",
            [
                { text: "Cancel", style: "cancel" },
                {
                    text: "Delete",
                    style: "destructive",
                    onPress: async () => {
                        try {
                            await apiFetch(`/api/admin/gold-images/${goldImageId}`, {
                                method: "DELETE",
                            });
                            // Refresh the list
                            fetchGoldImages();
                            Alert.alert("Success", "Gold image deleted successfully");
                        } catch (err) {
                            console.error("Delete error:", err);
                            Alert.alert("Error", "Failed to delete gold image");
                        }
                    },
                },
            ]
        );
    };

    return (
        <ScreenHeaderLayout
            leftIcon={require("../../../assets/images/gold_images.png")}
            leftTitle="Gold Images"
            rightIcon={require("../../../assets/images/add_gold_image.png")}
            rightTitle="Add Image"
            onRightPress={() => navigation.navigate("AddGoldImage")}
        >
            {loading ? (
                <View style={styles.centerContainer}>
                    <Text>Loading...</Text>
                </View>
            ) : goldImages.length === 0 ? (
                <View style={styles.centerContainer}>
                    <Text style={styles.emptyText}>No gold images found</Text>
                    <Text style={styles.emptySubtext}>
                        Tap "Add Image" to create your first gold image
                    </Text>
                </View>
            ) : (
                <FlatList
                    data={goldImages}
                    keyExtractor={(item) => item.id.toString()}
                    renderItem={({ item }) => (
                        <GoldImageCard
                            goldImage={item}
                            onDelete={() => handleDelete(item.id)}
                        />
                    )}
                />
            )}
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    centerContainer: {
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
    },
    emptyText: {
        fontSize: 16,
        fontWeight: "600",
        color: "#6B7280",
    },
    emptySubtext: {
        fontSize: 14,
        color: "#9CA3AF",
        marginTop: 8,
    },
});