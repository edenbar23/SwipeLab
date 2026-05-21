// researcher screen for managing gold images
import React, { useState } from "react";
import {
    FlatList,
    Modal,
    StyleSheet,
    Text,
    TouchableOpacity,
    TouchableWithoutFeedback,
    View,
} from "react-native";
import { useQueryClient } from '@tanstack/react-query';
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
    const queryClient = useQueryClient();
    const { data: goldImages = [], isLoading: loading } = useGoldImages();

    // ID of the gold image pending deletion; null means modal is closed
    const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null);

    const handleDelete = (goldImageId: number) => {
        setPendingDeleteId(goldImageId);
    };

    const confirmDelete = async () => {
        if (pendingDeleteId === null) return;
        const id = pendingDeleteId;
        setPendingDeleteId(null);

        try {
            const response = await apiFetch(
                API_ENDPOINTS.researcher.GOLD_IMAGE_DETAILS(id),
                { method: "DELETE" }
            );
            if (!response.ok) {
                const body = await response.json().catch(() => ({}));
                throw new Error((body as any).message ?? `Server error ${response.status}`);
            }
            queryClient.invalidateQueries({ queryKey: ['researcher', 'goldImages'] });
        } catch (err: any) {
            console.error("Delete error:", err);
        }
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

            {/* Delete Confirmation Modal */}
            <Modal visible={pendingDeleteId !== null} animationType="fade" transparent>
                <TouchableWithoutFeedback onPress={() => setPendingDeleteId(null)}>
                    <View style={styles.modalOverlay}>
                        <TouchableWithoutFeedback onPress={() => {}}>
                            <View style={[styles.modalContent, { backgroundColor: themeColors.card }]}>
                                <Text style={[styles.modalTitle, { color: themeColors.text }]}>
                                    Delete Gold Image
                                </Text>
                                <Text style={[styles.modalMessage, { color: themeColors.textSecondary }]}>
                                    Are you sure you want to delete this gold image? This action cannot be undone.
                                </Text>
                                <View style={styles.modalActions}>
                                    <TouchableOpacity
                                        style={[styles.actionBtn, styles.cancelBtn]}
                                        onPress={() => setPendingDeleteId(null)}
                                    >
                                        <Text style={styles.cancelText}>Cancel</Text>
                                    </TouchableOpacity>
                                    <TouchableOpacity
                                        style={[styles.actionBtn, styles.deleteBtn]}
                                        onPress={confirmDelete}
                                    >
                                        <Text style={styles.deleteText}>Delete</Text>
                                    </TouchableOpacity>
                                </View>
                            </View>
                        </TouchableWithoutFeedback>
                    </View>
                </TouchableWithoutFeedback>
            </Modal>
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
    },
    emptySubtext: {
        fontSize: 14,
        marginTop: 8,
    },

    // Modal
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.5)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    modalContent: {
        width: 320,
        borderRadius: 12,
        padding: 24,
        alignItems: 'center',
    },
    modalTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        textAlign: 'center',
    },
    modalMessage: {
        fontSize: 14,
        textAlign: 'center',
        marginBottom: 24,
        lineHeight: 20,
    },
    modalActions: {
        flexDirection: 'row',
        gap: 12,
        width: '100%',
        justifyContent: 'center',
    },
    actionBtn: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: 'center',
    },
    cancelBtn: {
        backgroundColor: '#F1F5F9',
    },
    cancelText: {
        color: '#374151',
        fontWeight: '600',
    },
    deleteBtn: {
        backgroundColor: '#EF4444',
    },
    deleteText: {
        color: '#fff',
        fontWeight: '600',
    },
});