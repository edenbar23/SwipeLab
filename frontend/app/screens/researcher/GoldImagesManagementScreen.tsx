// researcher screen for managing gold images
import React, { useEffect, useState } from "react";
import { Alert, FlatList, StyleSheet, Text, View } from "react-native";
import { Colors } from '../../../constants/theme';
import { apiFetch } from "../../api/apiFetch";
import GoldImageCard from "../../components/researcher/GoldImageCard";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { useThemeStore } from '../../stores/themeStore';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { useGoldImages } from '../../api/queries';


type GoldImageResponse = {
    id: number;
    imageId: number;
    species: string;
    correctAnswer: string;
    imageUrl?: string;
};

export default function GoldImagesManagementScreen({ navigation }: any) {
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const { data: goldImages = [], isLoading: loading, refetch: fetchGoldImages } = useGoldImages();

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
                            await apiFetch(API_ENDPOINTS.researcher.GOLD_IMAGE_DETAILS(goldImageId), {
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
                <View style={[styles.centerContainer, { backgroundColor: themeColors.background }]}>
                    <Text style={{ color: themeColors.text }}>Loading...</Text>
                </View>
            ) : goldImages.length === 0 ? (
                <View style={[styles.centerContainer, { backgroundColor: themeColors.background }]}>
                    <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>No gold images found</Text>
                    <Text style={[styles.emptySubtext, { color: themeColors.textSecondary }]}>
                        Tap "Add Image" to create your first gold image
                    </Text>
                </View>
            ) : (
                <FlatList
                    showsVerticalScrollIndicator={false}
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