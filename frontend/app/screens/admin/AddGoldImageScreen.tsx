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
    Image as RNImage,
    Platform,
} from "react-native";
import * as ImagePicker from 'expo-image-picker';
import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';

export default function AddGoldImageScreen({ navigation }: any) {
    const [uploadType, setUploadType] = useState<"url" | "file">("file");
    const [imageUrl, setImageUrl] = useState("");
    const [imageFile, setImageFile] = useState<ImagePicker.ImagePickerAsset | null>(null);
    const [correctAnswer, setCorrectAnswer] = useState<"YES" | "NO">("YES");
    const [classValue, setClassValue] = useState("");
    const [order, setOrder] = useState("");
    const [family, setFamily] = useState("");
    const [genus, setGenus] = useState("");
    const [species, setSpecies] = useState("");
    const [difficultyLevel, setDifficultyLevel] = useState("MEDIUM");
    const [loading, setLoading] = useState(false);
    const [statusMessage, setStatusMessage] = useState<{ type: 'error' | 'success', text: string } | null>(null);
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    // For demo purposes, using taskId = 1. In production, this would come from navigation params
    const taskId = 1;

    const pickImage = async () => {
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ImagePicker.MediaTypeOptions.Images,
            allowsEditing: true,
            quality: 1,
        });

        if (!result.canceled) {
            setImageFile(result.assets[0]);
        }
    };

    const handleSubmit = async () => {
        setStatusMessage(null); // Clear previous status
        
        // Validation
        if (!taskId) {
            setStatusMessage({ type: 'error', text: "Validation Error: Task ID is required" });
            return;
        }
        if (uploadType === "url" && !imageUrl) {
            setStatusMessage({ type: 'error', text: "Validation Error: Image URL is required" });
            return;
        }
        if (uploadType === "file" && !imageFile) {
            setStatusMessage({ type: 'error', text: "Validation Error: An image file is required" });
            return;
        }
        if (!species) {
            setStatusMessage({ type: 'error', text: "Validation Error: Species is required" });
            return;
        }

        const formData = new FormData();
        formData.append("taskId", taskId.toString());
        formData.append("species", species.trim());
        formData.append("correctAnswer", correctAnswer);

        if (uploadType === "file" && imageFile) {
            if (Platform.OS === 'web') {
                // On Web, imageFile.uri is a blob URL. We need to fetch it to get a Blob,
                // or use imageFile.file if it exists in newer expo-image-picker versions.
                if (imageFile.file) {
                    formData.append("file", imageFile.file);
                } else {
                    const response = await fetch(imageFile.uri);
                    const blob = await response.blob();
                    formData.append("file", blob, imageFile.fileName || "image.jpg");
                }
            } else {
                const localUri = Platform.OS === 'ios' ? imageFile.uri.replace('file://', '') : imageFile.uri;
                const filename = imageFile.fileName || localUri.split('/').pop() || 'image.jpg';
                const match = /\.(\w+)$/.exec(filename);
                const type = match ? `image/${match[1]}` : `image`;

                formData.append("file", {
                    uri: localUri,
                    name: filename,
                    type,
                } as any);
            }
        } else if (uploadType === "url") {
            formData.append("imageUrl", imageUrl);
        }

        try {
            setLoading(true);
            const res = await apiFetch(API_ENDPOINTS.ADMIN.GOLD_IMAGES_UPLOAD, {
                method: "POST",
                body: formData,
            });

            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(`HTTP error! status: ${res.status} - ${errorText}`);
            }

            const data = await res.json();
            console.log("AddGoldImage response:", data);

            setStatusMessage({ type: 'success', text: "Gold image created successfully! Redirecting..." });
            
            // Wait 1.5 seconds for the user to read the success message before navigating away
            setTimeout(() => {
                navigation.navigate("AdminDashboard");
            }, 1500);

        } catch (err: any) {
            console.error("Error creating gold image:", err);
            const errMsg = "Failed to create gold image: " + (err.message || "Check fields.");
            setStatusMessage({ type: 'error', text: errMsg });
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
                {/* Upload Mode Selection */}
                <View style={styles.toggleSection}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Upload Type</Text>
                    <View style={styles.toggleButtons}>
                        {(['file', 'url'] as const).map((type) => (
                            <TouchableOpacity
                                key={type}
                                style={[
                                    styles.toggleButton,
                                    { backgroundColor: themeColors.card, borderColor: themeColors.border },
                                    uploadType === type && styles.toggleButtonActive,
                                ]}
                                onPress={() => setUploadType(type)}
                            >
                                <Text
                                    style={[
                                        styles.toggleText,
                                        uploadType === type && styles.toggleTextActive,
                                    ]}
                                >
                                    {type === 'file' ? 'Upload File' : 'Image URL'}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                {/* Upload Section */}
                <View style={styles.uploadSection}>
                    {uploadType === "url" ? (
                        <TextInput
                            style={[styles.input, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                            value={imageUrl}
                            onChangeText={setImageUrl}
                            placeholder="Enter image URL"
                            placeholderTextColor={themeColors.textSecondary}
                        />
                    ) : (
                        <View style={styles.filePickerContainer}>
                            <TouchableOpacity style={styles.pickButton} onPress={pickImage}>
                                <Text style={styles.pickButtonText}>Select Image From Device</Text>
                            </TouchableOpacity>
                            {imageFile && (
                                <RNImage
                                    source={{ uri: imageFile.uri }}
                                    style={styles.previewImage}
                                />
                            )}
                        </View>
                    )}
                </View>

                {/* Is Valid Requested Species? Toggle */}
                <View style={styles.toggleSection}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Is this picture actually the requested species?</Text>
                    <View style={styles.toggleButtons}>
                        {(['YES', 'NO'] as const).map((ans) => (
                            <TouchableOpacity
                                key={ans}
                                style={[
                                    styles.toggleButton,
                                    { backgroundColor: themeColors.card, borderColor: themeColors.border },
                                    correctAnswer === ans && styles.toggleButtonActive,
                                ]}
                                onPress={() => setCorrectAnswer(ans)}
                            >
                                <Text
                                    style={[
                                        styles.toggleText,
                                        correctAnswer === ans && styles.toggleTextActive,
                                    ]}
                                >
                                    {ans}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
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
                    <View style={styles.toggleButtons}>
                        {["EASY", "MEDIUM", "HARD"].map((level) => (
                            <TouchableOpacity
                                key={level}
                                style={[
                                    styles.toggleButton,
                                    { backgroundColor: themeColors.card, borderColor: themeColors.border },
                                    difficultyLevel === level && styles.toggleButtonActive,
                                ]}
                                onPress={() => setDifficultyLevel(level)}
                            >
                                <Text
                                    style={[
                                        styles.toggleText,
                                        difficultyLevel === level && styles.toggleTextActive,
                                    ]}
                                >
                                    {level}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                {/* Status Message Display */}
                {statusMessage && (
                    <View style={[styles.statusContainer, { backgroundColor: statusMessage.type === 'error' ? '#FEE2E2' : '#DCFCE7' }]}>
                        <Text style={[styles.statusText, { color: statusMessage.type === 'error' ? '#B91C1C' : '#15803D' }]}>
                            {statusMessage.text}
                        </Text>
                    </View>
                )}

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
                            {loading ? "..." : "➔"}
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
    toggleSection: {
        marginBottom: 20,
    },
    sectionTitle: {
        fontSize: 14,
        fontWeight: "600",
        marginBottom: 12,
    },
    toggleButtons: {
        flexDirection: "row",
        gap: 12,
    },
    toggleButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: "center",
        borderWidth: 1,
    },
    toggleButtonActive: {
        backgroundColor: "#3B82F6",
        borderColor: "#3B82F6",
    },
    toggleText: {
        fontSize: 14,
        fontWeight: "600",
        color: "#6B7280",
    },
    toggleTextActive: {
        color: "#fff",
    },
    uploadSection: {
        marginBottom: 24,
    },
    input: {
        borderWidth: 1,
        borderRadius: 10,
        padding: 12,
        fontSize: 14,
    },
    filePickerContainer: {
        alignItems: "center",
        gap: 16,
    },
    pickButton: {
        backgroundColor: "#E5E7EB",
        padding: 12,
        borderRadius: 8,
        width: "100%",
        alignItems: "center",
    },
    pickButtonText: {
        color: "#374151",
        fontWeight: "600",
    },
    previewImage: {
        width: 200,
        height: 200,
        borderRadius: 12,
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
        width: 80,
    },
    taxonomyInput: {
        flex: 1,
        borderWidth: 1,
        borderRadius: 8,
        padding: 10,
        fontSize: 14,
    },
    difficultySection: {
        marginBottom: 24,
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
    statusContainer: {
        padding: 12,
        borderRadius: 8,
        marginBottom: 16,
        alignItems: 'center',
    },
    statusText: {
        fontSize: 14,
        fontWeight: '500',
        textAlign: 'center',
    },
});