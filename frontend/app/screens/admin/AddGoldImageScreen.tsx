// admin screen for adding gold images
import React, { useState } from "react";
import {
    Alert,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";
import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';


export default function AddGoldImageScreen({ navigation }: any) {
    const [imageUrl, setImageUrl] = useState("");
    const [classValue, setClassValue] = useState("");
    const [order, setOrder] = useState("");
    const [family, setFamily] = useState("");
    const [genus, setGenus] = useState("");
    const [species, setSpecies] = useState("");
    const [difficultyLevel, setDifficultyLevel] = useState("MEDIUM");
    const [loading, setLoading] = useState(false);
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    // For demo purposes, using taskId = 1. In production, this would come from navigation params
    const taskId = 1;

    const handleSubmit = async () => {
        // Validation
        if (!imageUrl || !species) {
            Alert.alert("Validation Error", "Image URL and Species are required");
            return;
        }

        // Create a label name from the taxonomy fields
        // Using species as the primary label, but could combine with genus if needed
        const labelName = species.trim();
        const caption = [classValue, order, family, genus, species]
            .filter((v) => v.trim())
            .join(" - ");

        // First, we need to create or get the label ID
        // For simplicity, we'll send the label name and let the backend handle it
        // In a real implementation, you might want to search for existing labels first
        const payload = {
            imageUrl,
            caption,
            taskId,
            // Note: The backend expects correctLabelId, but we don't have it yet
            // This is a simplified implementation - you may need to:
            // 1. Call a label creation endpoint first, or
            // 2. Modify backend to accept label name instead of ID
            // For now, using a placeholder labelId = 1
            correctLabelId: 1, // TODO: Replace with actual label lookup/creation
            difficultyLevel,
            explanation: `Taxonomy: ${caption}`,
        };

        try {
            setLoading(true);
            const res = await apiFetch(API_ENDPOINTS.ADMIN.GOLD_IMAGES, {
                method: "POST",
                body: JSON.stringify(payload),
                headers: { "Content-Type": "application/json" },
            });

            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }

            const data = await res.json();
            console.log("AddGoldImage response:", data);

            Alert.alert("Success", "Gold image created successfully", [
                {
                    text: "OK",
                    onPress: () => navigation.navigate("GoldImagesManagement"),
                },
            ]);
        } catch (err) {
            console.error("Error creating gold image:", err);
            Alert.alert(
                "Error",
                "Failed to create gold image. Please check the console for details."
            );
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        navigation.goBack();
    };

    return (
        <ScreenHeaderLayout
            leftIcon={require("../../../assets/images/add_gold_image.png")}
            leftTitle="Add Gold Image"
            rightIcon={require("../../../assets/images/gold_images.png")}
            rightTitle="Gold Images"
            onRightPress={() => navigation.navigate("GoldImagesManagement")}
        >
            <ScrollView contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]} showsVerticalScrollIndicator={false}>
                {/* Upload Section */}
                <View style={styles.uploadSection}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Upload Your Image</Text>
                    <TextInput
                        style={[styles.input, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                        value={imageUrl}
                        onChangeText={setImageUrl}
                        placeholder="Enter image URL"
                        placeholderTextColor={themeColors.textSecondary}
                    />
                </View>

                {/* Taxonomy Fields */}
                <View style={styles.taxonomySection}>
                    <View style={styles.fieldRow}>
                        <Text style={[styles.label, { color: themeColors.textSecondary }]}>Class:</Text>
                        <TextInput
                            style={[styles.taxonomyInput, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                            value={classValue}
                            onChangeText={setClassValue}
                            placeholder="∨"
                            placeholderTextColor={themeColors.textSecondary}
                        />
                    </View>

                    <View style={styles.fieldRow}>
                        <Text style={[styles.label, { color: themeColors.textSecondary }]}>Order:</Text>
                        <TextInput
                            style={[styles.taxonomyInput, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                            value={order}
                            onChangeText={setOrder}
                            placeholder="∨"
                            placeholderTextColor={themeColors.textSecondary}
                        />
                    </View>

                    <View style={styles.fieldRow}>
                        <Text style={[styles.label, { color: themeColors.textSecondary }]}>Family:</Text>
                        <TextInput
                            style={[styles.taxonomyInput, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                            value={family}
                            onChangeText={setFamily}
                            placeholder="∨"
                            placeholderTextColor={themeColors.textSecondary}
                        />
                    </View>

                    <View style={styles.fieldRow}>
                        <Text style={[styles.label, { color: themeColors.textSecondary }]}>Genus:</Text>
                        <TextInput
                            style={[styles.taxonomyInput, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                            value={genus}
                            onChangeText={setGenus}
                            placeholder="∨"
                            placeholderTextColor={themeColors.textSecondary}
                        />
                    </View>

                    <View style={styles.fieldRow}>
                        <Text style={[styles.label, { color: themeColors.textSecondary }]}>Species:</Text>
                        <TextInput
                            style={[styles.taxonomyInput, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                            value={species}
                            onChangeText={setSpecies}
                            placeholder="∨"
                            placeholderTextColor={themeColors.textSecondary}
                        />
                    </View>
                </View>

                {/* Difficulty Level */}
                <View style={styles.difficultySection}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Difficulty Level</Text>
                    <View style={styles.difficultyButtons}>
                        {["EASY", "MEDIUM", "HARD"].map((level) => (
                            <TouchableOpacity
                                key={level}
                                style={[
                                    styles.difficultyButton,
                                    { backgroundColor: themeColors.card, borderColor: themeColors.border },
                                    difficultyLevel === level && styles.difficultyButtonActive,
                                ]}
                                onPress={() => setDifficultyLevel(level)}
                            >
                                <Text
                                    style={[
                                        styles.difficultyText,
                                        difficultyLevel === level && styles.difficultyTextActive,
                                    ]}
                                >
                                    {level}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                {/* Action Buttons */}
                <View style={styles.actionButtons}>
                    <TouchableOpacity
                        style={styles.cancelButton}
                        onPress={handleCancel}
                        disabled={loading}
                    >
                        <Text style={styles.cancelButtonText}>✕</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={[styles.submitButton, loading && styles.buttonDisabled]}
                        onPress={handleSubmit}
                        disabled={loading}
                    >
                        <Text style={styles.submitButtonText}>
                            {loading ? "➔" : "➔"}
                        </Text>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 16,
    },
    uploadSection: {
        marginBottom: 24,
    },
    sectionTitle: {
        fontSize: 14,
        fontWeight: "600",
        color: "#374151",
        marginBottom: 12,
    },
    input: {
        borderWidth: 1,
        borderColor: "#D1D5DB",
        borderRadius: 10,
        padding: 12,
        backgroundColor: "#fff",
        fontSize: 14,
    },
    taxonomySection: {
        marginBottom: 24,
    },
    fieldRow: {
        flexDirection: "row",
        alignItems: "center",
        marginBottom: 12,
    },
    label: {
        fontSize: 14,
        fontWeight: "500",
        color: "#6B7280",
        width: 80,
    },
    taxonomyInput: {
        flex: 1,
        borderWidth: 1,
        borderColor: "#D1D5DB",
        borderRadius: 8,
        padding: 10,
        backgroundColor: "#F9FAFB",
        fontSize: 14,
    },
    difficultySection: {
        marginBottom: 24,
    },
    difficultyButtons: {
        flexDirection: "row",
        gap: 12,
    },
    difficultyButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: "#F3F4F6",
        alignItems: "center",
        borderWidth: 1,
        borderColor: "#E5E7EB",
    },
    difficultyButtonActive: {
        backgroundColor: "#3B82F6",
        borderColor: "#3B82F6",
    },
    difficultyText: {
        fontSize: 14,
        fontWeight: "600",
        color: "#6B7280",
    },
    difficultyTextActive: {
        color: "#fff",
    },
    actionButtons: {
        flexDirection: "row",
        justifyContent: "center",
        gap: 24,
        marginTop: 12,
    },
    cancelButton: {
        width: 60,
        height: 60,
        borderRadius: 30,
        backgroundColor: "#EF4444",
        justifyContent: "center",
        alignItems: "center",
    },
    cancelButtonText: {
        fontSize: 28,
        color: "#fff",
        fontWeight: "bold",
    },
    submitButton: {
        width: 60,
        height: 60,
        borderRadius: 30,
        backgroundColor: "#3B82F6",
        justifyContent: "center",
        alignItems: "center",
    },
    submitButtonText: {
        fontSize: 28,
        color: "#fff",
        fontWeight: "bold",
    },
    buttonDisabled: {
        backgroundColor: "#94A3B8",
    },
});