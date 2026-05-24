// researcher screen for adding gold images
import React, { useState, useEffect, useRef, useCallback } from "react";
import {
    ActivityIndicator,
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
import { useQueryClient } from '@tanstack/react-query';
import MultiSelect from '../../components/ui/MultiSelect';

type UrlValidationState = 'idle' | 'checking' | 'valid' | 'invalid';

export default function AddGoldImageScreen({ navigation }: any) {
    const queryClient = useQueryClient();
    const [uploadType, setUploadType] = useState<"url" | "file">("file");
    const [imageUrl, setImageUrl] = useState("");
    const [imageFile, setImageFile] = useState<ImagePicker.ImagePickerAsset | null>(null);
    const [correctAnswer, setCorrectAnswer] = useState<"YES" | "NO">("YES");
    const [species, setSpecies] = useState("");
    const [difficultyLevel, setDifficultyLevel] = useState("MEDIUM");
    const [loading, setLoading] = useState(false);
    const [statusMessage, setStatusMessage] = useState<{ type: 'error' | 'success'; text: string } | null>(null);
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const [availableSpecies, setAvailableSpecies] = useState<{ id: string; label: string }[]>([]);
    const [optionsLoading, setOptionsLoading] = useState(false);

    // URL real-time validation state
    const [urlValidation, setUrlValidation] = useState<UrlValidationState>('idle');
    const [urlError, setUrlError] = useState<string>('');
    const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const isDark = theme === 'dark';

    useEffect(() => {
        const fetchOptions = async () => {
            setOptionsLoading(true);
            try {
                const speciesRes = await apiFetch('/api/v1/metadata/species');
                if (speciesRes.ok) {
                    const sps = await speciesRes.json();
                    setAvailableSpecies(sps.map((s: any) => ({
                        id: String(s.id),
                        label: String(s.label),
                        searchTerms: String(s.searchTerms || "")
                    })));
                }
            } catch (error) {
                console.error("Failed to load species:", error);
            } finally {
                setOptionsLoading(false);
            }
        };
        fetchOptions();
    }, []);

    // For demo purposes, using taskId = 1. In production, this would come from navigation params
    const taskId = 1;

    const validateImageUrl = async (url: string): Promise<string | null> => {
        try {
            // Try HEAD first (cheaper), fall back to GET if server doesn't allow HEAD
            let response = await fetch(url, { method: 'HEAD' });
            if (response.status === 405) {
                response = await fetch(url, { method: 'GET' });
            }
            if (!response.ok) {
                return `URL returned HTTP ${response.status}. Check the link is correct and publicly accessible.`;
            }
            const contentType = response.headers.get('content-type') || '';
            if (!contentType.toLowerCase().startsWith('image/')) {
                return `Not an image (Content-Type: "${contentType}"). Only image links are accepted.`;
            }
            return null; // valid
        } catch {
            return 'Could not reach the URL. Make sure it is publicly accessible.';
        }
    };

    // Debounced real-time URL validation — fires 800ms after the user stops typing
    const handleUrlChange = useCallback((text: string) => {
        setImageUrl(text);

        if (debounceTimer.current) clearTimeout(debounceTimer.current);

        if (!text.trim()) {
            setUrlValidation('idle');
            setUrlError('');
            return;
        }

        // Basic format check before even hitting the network
        try {
            new URL(text);
        } catch {
            setUrlValidation('invalid');
            setUrlError('Enter a valid URL (e.g. https://example.com/image.jpg)');
            return;
        }

        setUrlValidation('checking');
        setUrlError('');

        debounceTimer.current = setTimeout(async () => {
            const err = await validateImageUrl(text);
            if (err) {
                setUrlValidation('invalid');
                setUrlError(err);
            } else {
                setUrlValidation('valid');
                setUrlError('');
            }
        }, 800);
    }, []);

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
        setStatusMessage(null);

        if (!taskId) {
            setStatusMessage({ type: 'error', text: "Task ID is required" });
            return;
        }
        if (uploadType === "url" && !imageUrl) {
            setStatusMessage({ type: 'error', text: "Image URL is required" });
            return;
        }
        if (uploadType === "url" && urlValidation === 'invalid') {
            setStatusMessage({ type: 'error', text: urlError || "The image URL is not valid." });
            return;
        }
        if (uploadType === "file" && !imageFile) {
            setStatusMessage({ type: 'error', text: "An image file is required" });
            return;
        }
        if (!species) {
            setStatusMessage({ type: 'error', text: "Species is required" });
            return;
        }

        // If URL hasn't been verified yet (still checking or idle), run validation now
        if (uploadType === "url" && urlValidation !== 'valid') {
            setLoading(true);
            setStatusMessage({ type: 'error', text: 'Validating image URL…' });
            const err = await validateImageUrl(imageUrl);
            setLoading(false);
            if (err) {
                setUrlValidation('invalid');
                setUrlError(err);
                setStatusMessage({ type: 'error', text: err });
                return;
            }
            setUrlValidation('valid');
            setStatusMessage(null);
        }

        const formData = new FormData();
        formData.append("taskId", taskId.toString());
        formData.append("species", species.trim());
        formData.append("correctAnswer", correctAnswer);

        if (uploadType === "file" && imageFile) {
            if (Platform.OS === 'web') {
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
                formData.append("file", { uri: localUri, name: filename, type } as any);
            }
        } else if (uploadType === "url") {
            formData.append("imageUrl", imageUrl);
        }

        try {
            setLoading(true);
            const res = await apiFetch(API_ENDPOINTS.researcher.GOLD_IMAGES_UPLOAD, {
                method: "POST",
                body: formData,
            });

            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(`HTTP ${res.status} – ${errorText}`);
            }

            await res.json();

            queryClient.invalidateQueries({ queryKey: ['researcher', 'goldImages'] });

            setStatusMessage({ type: 'success', text: "Gold image created successfully! Redirecting…" });

            // Navigate to the Gold Images list after a brief success message
            setTimeout(() => {
                navigation.navigate("GoldImagesManagement");
            }, 1500);
        } catch (err: any) {
            console.error("Error creating gold image:", err);
            setStatusMessage({ type: 'error', text: "Failed to create gold image: " + (err.message || "Check fields.") });
        } finally {
            setLoading(false);
        }
    };

    // Derived border color for URL input
    const urlBorderColor =
        urlValidation === 'valid' ? '#10B981' :
        urlValidation === 'invalid' ? '#EF4444' :
        themeColors.border;

    const urlBgColor =
        urlValidation === 'valid' ? (isDark ? 'rgba(16,185,129,0.1)' : '#F0FDF4') :
        urlValidation === 'invalid' ? (isDark ? 'rgba(239,68,68,0.1)' : '#FEF2F2') :
        themeColors.card;

    const cardBg = isDark ? 'rgba(255,255,255,0.05)' : '#FFFFFF';
    const cardBorder = isDark ? 'rgba(255,255,255,0.08)' : '#E5E7EB';

    return (
        <ScreenHeaderLayout
            leftIcon={require("../../../assets/images/add_gold_image.png")}
            leftTitle="Add Gold Image"
            rightIcon={require("../../../assets/images/gold_images.png")}
            rightTitle="Gold Images"
            onRightPress={() => navigation.navigate("GoldImagesManagement")}
        >
            <ScrollView
                contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]}
                showsVerticalScrollIndicator={false}
            >
                {/* ── Info Banner ── */}
                <View style={[styles.infoBanner, { backgroundColor: isDark ? 'rgba(59, 130, 246, 0.1)' : '#EFF6FF', borderColor: isDark ? 'rgba(59, 130, 246, 0.2)' : '#BFDBFE' }]}>
                    <Text style={[styles.infoBannerTitle, { color: isDark ? '#60A5FA' : '#1D4ED8' }]}>
                        ℹ️ Gold Image Guidelines
                    </Text>
                    <Text style={[styles.infoBannerText, { color: themeColors.text }]}>
                        • Used as a hidden quality control mechanism to calculate user credibility.{"\n"}
                        • They contain pre-verified labels and are mixed into regular tasks to test accuracy.{"\n"}
                        • Must look identical to standard experiment photos (e.g., yellow sticky traps) so users cannot tell they are being tested.
                    </Text>
                </View>

                {/* ── Upload Type ── */}
                <View style={[styles.card, { backgroundColor: cardBg, borderColor: cardBorder }]}>
                    <Text style={[styles.cardLabel, { color: themeColors.textSecondary }]}>UPLOAD TYPE</Text>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>How would you like to add the image?</Text>
                    <View style={styles.toggleButtons}>
                        {(['file', 'url'] as const).map((type) => (
                            <TouchableOpacity
                                key={type}
                                style={[
                                    styles.toggleButton,
                                    { borderColor: cardBorder, backgroundColor: themeColors.background },
                                    uploadType === type && styles.toggleButtonActive,
                                ]}
                                onPress={() => {
                                    setUploadType(type);
                                    setUrlValidation('idle');
                                    setUrlError('');
                                }}
                            >
                                <Text style={[styles.toggleIcon]}>
                                    {type === 'file' ? '📁' : '🔗'}
                                </Text>
                                <Text style={[styles.toggleText, uploadType === type && styles.toggleTextActive]}>
                                    {type === 'file' ? 'Upload File' : 'Image URL'}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                {/* ── Image Input ── */}
                <View style={[styles.card, { backgroundColor: cardBg, borderColor: cardBorder }]}>
                    <Text style={[styles.cardLabel, { color: themeColors.textSecondary }]}>IMAGE SOURCE</Text>
                    {uploadType === "url" ? (
                        <View>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Image URL</Text>
                            <View style={styles.urlInputRow}>
                                <TextInput
                                    style={[
                                        styles.input,
                                        styles.urlInput,
                                        {
                                            backgroundColor: urlBgColor,
                                            borderColor: urlBorderColor,
                                            color: themeColors.text,
                                        },
                                    ]}
                                    value={imageUrl}
                                    onChangeText={handleUrlChange}
                                    placeholder="https://example.com/image.jpg"
                                    placeholderTextColor={themeColors.textSecondary}
                                    autoCapitalize="none"
                                    keyboardType="url"
                                />
                                <View style={styles.urlIndicator}>
                                    {urlValidation === 'checking' && (
                                        <ActivityIndicator size="small" color="#3B82F6" />
                                    )}
                                    {urlValidation === 'valid' && (
                                        <Text style={styles.validIcon}>✓</Text>
                                    )}
                                    {urlValidation === 'invalid' && (
                                        <Text style={styles.invalidIcon}>✗</Text>
                                    )}
                                </View>
                            </View>
                            {urlValidation === 'invalid' && urlError ? (
                                <Text style={styles.urlErrorText}>{urlError}</Text>
                            ) : null}
                            {urlValidation === 'valid' ? (
                                <>
                                    <Text style={styles.urlSuccessText}>✓ Valid image URL</Text>
                                    <View style={styles.previewContainer}>
                                        <RNImage
                                            source={{ uri: imageUrl }}
                                            style={styles.previewImage}
                                            resizeMode="cover"
                                        />
                                        <View style={styles.previewBadge}>
                                            <Text style={styles.previewBadgeText}>✓ Preview</Text>
                                        </View>
                                    </View>
                                </>
                            ) : null}
                        </View>
                    ) : (
                        <View>
                            <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Select from Device</Text>
                            <TouchableOpacity
                                style={[styles.pickButton, { backgroundColor: themeColors.background, borderColor: cardBorder }]}
                                onPress={pickImage}
                            >
                                <Text style={styles.pickButtonIcon}>📷</Text>
                                <Text style={[styles.pickButtonText, { color: themeColors.text }]}>
                                    {imageFile ? 'Change Image' : 'Browse & Select Image'}
                                </Text>
                            </TouchableOpacity>
                            {imageFile && (
                                <View style={styles.previewContainer}>
                                    <RNImage source={{ uri: imageFile.uri }} style={styles.previewImage} />
                                    <View style={styles.previewBadge}>
                                        <Text style={styles.previewBadgeText}>✓ Ready</Text>
                                    </View>
                                </View>
                            )}
                        </View>
                    )}
                </View>

                {/* ── Species ── */}
                <View style={[styles.card, { backgroundColor: cardBg, borderColor: cardBorder, zIndex: 10 }]}>
                    <Text style={[styles.cardLabel, { color: themeColors.textSecondary }]}>TAXONOMY</Text>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Species</Text>
                    <Text style={[styles.sectionHint, { color: themeColors.textSecondary }]}>
                        Search and select the relevant species from the taxonomy.
                    </Text>
                    <MultiSelect
                        options={availableSpecies}
                        selectedIds={species ? [species] : []}
                        onToggle={(id) => {
                            setSpecies(species === id ? "" : id as string);
                        }}
                        placeholder="Search for species…"
                        loading={optionsLoading}
                        emptyOnNoSearch={true}
                    />
                </View>

                {/* ── Correct Answer ── */}
                <View style={[styles.card, { backgroundColor: cardBg, borderColor: cardBorder }]}>
                    <Text style={[styles.cardLabel, { color: themeColors.textSecondary }]}>CLASSIFICATION</Text>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>
                        Is this picture actually the requested species?
                    </Text>
                    <View style={styles.toggleButtons}>
                        {(['YES', 'NO'] as const).map((ans) => (
                            <TouchableOpacity
                                key={ans}
                                style={[
                                    styles.toggleButton,
                                    { borderColor: cardBorder, backgroundColor: themeColors.background },
                                    correctAnswer === ans && (ans === 'YES' ? styles.toggleButtonYes : styles.toggleButtonNo),
                                ]}
                                onPress={() => setCorrectAnswer(ans)}
                            >
                                <Text style={styles.toggleIcon}>{ans === 'YES' ? '✓' : '✗'}</Text>
                                <Text style={[
                                    styles.toggleText,
                                    correctAnswer === ans && styles.toggleTextActive,
                                ]}>
                                    {ans === 'YES' ? 'Yes' : 'No'}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                {/* ── Difficulty ── */}
                <View style={[styles.card, { backgroundColor: cardBg, borderColor: cardBorder }]}>
                    <Text style={[styles.cardLabel, { color: themeColors.textSecondary }]}>DIFFICULTY</Text>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Difficulty Level</Text>
                    <View style={styles.toggleButtons}>
                        {(["EASY", "MEDIUM", "HARD"] as const).map((level) => (
                            <TouchableOpacity
                                key={level}
                                style={[
                                    styles.toggleButton,
                                    { borderColor: cardBorder, backgroundColor: themeColors.background },
                                    difficultyLevel === level && difficultyStyles[level],
                                ]}
                                onPress={() => setDifficultyLevel(level)}
                            >
                                <Text style={styles.toggleIcon}>{level === 'EASY' ? '🟢' : level === 'MEDIUM' ? '🟡' : '🔴'}</Text>
                                <Text style={[
                                    styles.toggleText,
                                    difficultyLevel === level && styles.toggleTextActive,
                                ]}>
                                    {level.charAt(0) + level.slice(1).toLowerCase()}
                                </Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                {/* ── Status Message ── */}
                {statusMessage && (
                    <View style={[
                        styles.statusContainer,
                        { backgroundColor: statusMessage.type === 'error' ? '#FEE2E2' : '#DCFCE7' },
                    ]}>
                        <Text style={[
                            styles.statusText,
                            { color: statusMessage.type === 'error' ? '#B91C1C' : '#15803D' },
                        ]}>
                            {statusMessage.text}
                        </Text>
                    </View>
                )}

                {/* ── Action Buttons ── */}
                <View style={styles.actionButtons}>
                    <TouchableOpacity
                        style={styles.cancelButton}
                        onPress={() => navigation.goBack()}
                        disabled={loading}
                    >
                        <Text style={styles.cancelButtonText}>✕</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={[styles.submitButton, loading && styles.buttonDisabled]}
                        onPress={handleSubmit}
                        disabled={loading}
                    >
                        {loading
                            ? <ActivityIndicator size="small" color="#fff" />
                            : <Text style={styles.submitButtonText}>➔</Text>
                        }
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const difficultyStyles: Record<string, object> = {
    EASY:   { backgroundColor: '#10B981', borderColor: '#10B981' },
    MEDIUM: { backgroundColor: '#F59E0B', borderColor: '#F59E0B' },
    HARD:   { backgroundColor: '#EF4444', borderColor: '#EF4444' },
};

const styles = StyleSheet.create({
    container: {
        padding: 16,
        gap: 16,
    },
    // ── Info Banner ──
    infoBanner: {
        padding: 16,
        borderRadius: 12,
        borderWidth: 1,
    },
    infoBannerTitle: {
        fontSize: 16,
        fontWeight: 'bold',
        marginBottom: 8,
    },
    infoBannerText: {
        fontSize: 14,
        lineHeight: 22,
    },
    // ── Card ──
    card: {
        borderRadius: 14,
        borderWidth: 1,
        padding: 18,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.06,
        shadowRadius: 6,
        elevation: 2,
    },
    cardLabel: {
        fontSize: 11,
        fontWeight: '700',
        letterSpacing: 1.2,
        marginBottom: 4,
        textTransform: 'uppercase',
    },
    sectionTitle: {
        fontSize: 15,
        fontWeight: '700',
        marginBottom: 14,
    },
    sectionHint: {
        fontSize: 13,
        marginBottom: 10,
        marginTop: -8,
    },
    // ── Toggle Pills ──
    toggleButtons: {
        flexDirection: "row",
        gap: 10,
    },
    toggleButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        paddingVertical: 11,
        paddingHorizontal: 8,
        borderRadius: 10,
        borderWidth: 1.5,
    },
    toggleButtonActive: {
        backgroundColor: "#3B82F6",
        borderColor: "#3B82F6",
    },
    toggleButtonYes: {
        backgroundColor: "#10B981",
        borderColor: "#10B981",
    },
    toggleButtonNo: {
        backgroundColor: "#EF4444",
        borderColor: "#EF4444",
    },
    toggleIcon: {
        fontSize: 15,
    },
    toggleText: {
        fontSize: 14,
        fontWeight: "600",
        color: "#6B7280",
    },
    toggleTextActive: {
        color: "#fff",
    },
    // ── URL Input ──
    urlInputRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    input: {
        borderWidth: 1.5,
        borderRadius: 10,
        padding: 12,
        fontSize: 14,
    },
    urlInput: {
        flex: 1,
    },
    urlIndicator: {
        width: 28,
        alignItems: 'center',
        justifyContent: 'center',
    },
    validIcon: {
        fontSize: 20,
        color: '#10B981',
        fontWeight: 'bold',
    },
    invalidIcon: {
        fontSize: 20,
        color: '#EF4444',
        fontWeight: 'bold',
    },
    urlErrorText: {
        color: '#DC2626',
        fontSize: 12,
        marginTop: 6,
        lineHeight: 16,
    },
    urlSuccessText: {
        color: '#059669',
        fontSize: 12,
        marginTop: 6,
        fontWeight: '600',
    },
    // ── File Picker ──
    pickButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 10,
        padding: 14,
        borderRadius: 10,
        borderWidth: 1.5,
        borderStyle: 'dashed',
    },
    pickButtonIcon: {
        fontSize: 20,
    },
    pickButtonText: {
        fontWeight: "600",
        fontSize: 14,
    },
    previewContainer: {
        alignItems: 'center',
        marginTop: 14,
        position: 'relative',
    },
    previewImage: {
        width: 200,
        height: 200,
        borderRadius: 12,
    },
    previewBadge: {
        position: 'absolute',
        bottom: 8,
        right: 'auto',
        backgroundColor: '#10B981',
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 20,
    },
    previewBadgeText: {
        color: '#fff',
        fontWeight: '700',
        fontSize: 12,
    },
    // ── Status ──
    statusContainer: {
        padding: 14,
        borderRadius: 10,
        alignItems: 'center',
    },
    statusText: {
        fontSize: 14,
        fontWeight: '500',
        textAlign: 'center',
    },
    // ── Action Buttons ──
    actionButtons: {
        flexDirection: "row",
        justifyContent: "center",
        gap: 24,
        marginTop: 4,
        marginBottom: 8,
    },
    cancelButton: {
        width: 60,
        height: 60,
        borderRadius: 30,
        backgroundColor: "#EF4444",
        justifyContent: "center",
        alignItems: "center",
        shadowColor: '#EF4444',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 4,
    },
    cancelButtonText: {
        fontSize: 22,
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
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 4,
    },
    submitButtonText: {
        fontSize: 26,
        color: "#fff",
        fontWeight: "bold",
    },
    buttonDisabled: {
        backgroundColor: "#94A3B8",
        shadowColor: 'transparent',
    },
});