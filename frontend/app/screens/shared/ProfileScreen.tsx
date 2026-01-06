import React, { useEffect, useState } from "react";
import { Alert, StyleSheet, Text, View, Image, TouchableOpacity, ScrollView, ActivityIndicator, Platform, TextInput, Modal } from "react-native";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout";
import { useNavigation } from "@react-navigation/native";
import { apiFetch } from "../../api/apiFetch";
import useResponsive from "../../hooks/useResponsive";

interface UserProfile {
    username: string;
    email: string;
    rank: string;
    score: number;
    badges: string[];
}

export default function ProfileScreen() {
    const navigation = useNavigation<any>();
    const { isDesktop } = useResponsive();
    const [user, setUser] = useState<UserProfile | null>(null);
    const [loading, setLoading] = useState(true);

    // Change Password State
    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordError, setPasswordError] = useState("");
    const [isSavingPassword, setIsSavingPassword] = useState(false);

    useEffect(() => {
        fetchProfile();
    }, []);

    const fetchProfile = async () => {
        try {
            const response = await apiFetch("/api/v1/auth/profile");
            if (!response.ok) throw new Error("Failed to fetch");
            const data = await response.json();
            setUser(data);
        } catch (error) {
            console.error("Failed to fetch profile:", error);
            Alert.alert("Error", "Failed to load profile data.");
        } finally {
            setLoading(false);
        }
    };

    const handleStartChangePassword = () => {
        setIsChangingPassword(true);
        setNewPassword("");
        setConfirmPassword("");
        setPasswordError("");
    };

    const handleCancelChangePassword = () => {
        setIsChangingPassword(false);
        setNewPassword("");
        setConfirmPassword("");
        setPasswordError("");
    };

    const handleSavePassword = async () => {
        setPasswordError("");

        if (!newPassword || !confirmPassword) {
            setPasswordError("Please fill in both fields.");
            return;
        }

        if (newPassword !== confirmPassword) {
            setPasswordError("Passwords do not match.");
            return;
        }

        if (newPassword.length < 6) {
            setPasswordError("Password must be at least 6 characters.");
            return;
        }

        setIsSavingPassword(true);

        try {
            const response = await apiFetch("/api/v1/auth/password/change", {
                method: "POST",
                body: JSON.stringify({ newPassword }),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "Failed to change password");
            }

            const data = await response.json();
            Alert.alert("Success", data.message);
            setIsChangingPassword(false);
        } catch (error: any) {
            console.error("Change password error:", error);
            setPasswordError(error.message || "An error occurred.");
        } finally {
            setIsSavingPassword(false);
        }
    };

    const handleNavigateToChallenges = () => {
        navigation.navigate("Challenges");
    };

    // Render Logic
    const renderContent = () => {
        if (loading) {
            return (
                <View style={styles.center}>
                    <ActivityIndicator size="large" color="#007AFF" />
                </View>
            );
        }

        if (!user) {
            return (
                <View style={styles.center}>
                    <Text>Failed to load user data.</Text>
                </View>
            );
        }

        return (
            <View style={[styles.contentWrapper, isDesktop && styles.desktopCard]}>
                {/* User Info Section */}
                <View style={styles.section}>
                    <View style={styles.avatarContainer}>
                    </View>

                    <View style={styles.infoRow}>
                        <Text style={styles.label}>Username:</Text>
                        <Text style={styles.value}>{user.username}</Text>
                    </View>
                    <View style={styles.infoRow}>
                        <Text style={styles.label}>Email:</Text>
                        <Text style={styles.value}>{user.email}</Text>
                    </View>
                </View>

                <View style={styles.divider} />

                {/* Stats Section */}
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>Stats</Text>
                    <View style={styles.statsRow}>
                        <View style={styles.statItem}>
                            <Text style={styles.statValue}>{user.score}</Text>
                            <Text style={styles.statLabel}>Score</Text>
                        </View>
                        <View style={styles.statItem}>
                            <Text style={styles.statValue}>{user.rank}</Text>
                            <Text style={styles.statLabel}>Rank</Text>
                        </View>
                    </View>
                </View>

                <View style={styles.divider} />

                {/* Badges Section */}
                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>Badges</Text>
                    <View style={styles.badgesContainer}>
                        {user.badges.map((badge, index) => (
                            <View key={index} style={styles.badge}>
                                <Text style={styles.badgeText}>{badge}</Text>
                            </View>
                        ))}
                    </View>
                </View>

                <View style={styles.divider} />

                {/* Actions Section */}
                <View style={styles.section}>
                    <TouchableOpacity style={styles.button} onPress={handleStartChangePassword}>
                        <Text style={styles.buttonText}>Change Password</Text>
                    </TouchableOpacity>
                </View>

                {/* Change Password Modal */}
                <Modal
                    visible={isChangingPassword}
                    transparent={true}
                    animationType="fade"
                    onRequestClose={handleCancelChangePassword}
                >
                    <View style={styles.modalOverlay}>
                        <View style={[styles.modalContent, isDesktop && styles.modalContentDesktop]}>
                            <Text style={styles.modalTitle}>Change Password</Text>

                            <TextInput
                                style={styles.input}
                                placeholder="New Password"
                                value={newPassword}
                                onChangeText={setNewPassword}
                                secureTextEntry
                            />
                            <TextInput
                                style={styles.input}
                                placeholder="Confirm New Password"
                                value={confirmPassword}
                                onChangeText={setConfirmPassword}
                                secureTextEntry
                            />

                            {passwordError ? <Text style={styles.errorText}>{passwordError}</Text> : null}

                            <View style={styles.actionButtons}>
                                <TouchableOpacity
                                    style={[styles.smallButton, styles.cancelButton]}
                                    onPress={handleCancelChangePassword}
                                    disabled={isSavingPassword}
                                >
                                    <Text style={[styles.smallButtonText, styles.cancelButtonText]}>Cancel</Text>
                                </TouchableOpacity>

                                <TouchableOpacity
                                    style={[styles.smallButton, styles.saveButton]}
                                    onPress={handleSavePassword}
                                    disabled={isSavingPassword}
                                >
                                    {isSavingPassword ? (
                                        <ActivityIndicator size="small" color="#fff" />
                                    ) : (
                                        <Text style={styles.smallButtonText}>Save</Text>
                                    )}
                                </TouchableOpacity>
                            </View>
                        </View>
                    </View>
                </Modal>
            </View>
        );
    };

    return (
        <ScreenHeaderLayout
            leftIcon={require("../../../assets/images/my-profile.png")}
            leftTitle="My Profile"
            rightIcon={require("../../../assets/images/tasks.png")}
            rightTitle="Challenges"
            onRightPress={handleNavigateToChallenges}
        >
            <ScrollView
                contentContainerStyle={[
                    styles.scrollContainer,
                    isDesktop && styles.scrollContainerDesktop
                ]}
            >
                {renderContent()}
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    scrollContainer: {
        paddingBottom: 20,
        width: "100%",
    },
    scrollContainerDesktop: {
        alignItems: 'center', // Centers the card within the scroll view
        paddingTop: 20,
    },
    contentWrapper: {
        width: "100%",
        padding: 10,
    },
    desktopCard: {
        width: 600,
        backgroundColor: "white",
        borderRadius: 16,
        padding: 24,
        // Shadow for iOS
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 12,
        // Shadow for Android
        elevation: 5,
        // Optional border for clarity
        borderWidth: 1,
        borderColor: "#eee",
    },
    center: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
    },
    section: {
        marginVertical: 5,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: "bold",
        marginBottom: 8,
        color: "#333",
    },
    infoRow: {
        flexDirection: "row",
        justifyContent: "space-between",
        marginBottom: 6,
        alignItems: "center",
    },
    label: {
        fontSize: 16,
        color: "#666",
        fontWeight: "500",
    },
    value: {
        fontSize: 16,
        color: "#000",
        fontWeight: "bold",
    },
    divider: {
        height: 1,
        backgroundColor: "#E0E0E0",
        marginVertical: 8,
    },
    avatarContainer: {
        alignItems: 'center',
        marginBottom: 12,
    },
    avatar: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: '#f0f0f0',
    },
    statsRow: {
        flexDirection: 'row',
        justifyContent: 'space-around',
    },
    statItem: {
        alignItems: 'center',
    },
    statValue: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#007AFF', // Example primary color
    },
    statLabel: {
        fontSize: 14,
        color: '#666',
    },
    badgesContainer: {
        flexDirection: "row",
        flexWrap: "wrap",
        gap: 10,
    },
    badge: {
        backgroundColor: "#F5F5F5",
        padding: 10,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: "#E0E0E0",
    },
    badgeText: {
        fontSize: 20,
    },
    button: {
        backgroundColor: "#007AFF",
        padding: 12,
        borderRadius: 8,
        alignItems: "center",
    },
    buttonText: {
        color: "#fff",
        fontSize: 16,
        fontWeight: "600",
    },
    changePasswordContainer: {
        // Removed as now using Modal
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: "rgba(0,0,0,0.5)",
        justifyContent: "center",
        alignItems: "center",
        padding: 20,
    },
    modalContent: {
        backgroundColor: "#fff",
        borderRadius: 16,
        padding: 24,
        width: "100%",
        maxWidth: 400,
        // Shadows
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.2,
        shadowRadius: 12,
        elevation: 10,
    },
    modalContentDesktop: {
        width: 400,
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: "bold",
        marginBottom: 20,
        textAlign: "center",
        color: "#333",
    },
    input: {
        backgroundColor: "#fff",
        borderWidth: 1,
        borderColor: "#ddd",
        borderRadius: 8,
        padding: 10,
        marginBottom: 10,
        fontSize: 16,
    },
    errorText: {
        color: "red",
        marginBottom: 10,
        fontSize: 14,
    },
    actionButtons: {
        flexDirection: "row",
        justifyContent: "space-between",
        marginTop: 5,
    },
    smallButton: {
        flex: 1,
        padding: 10,
        borderRadius: 8,
        alignItems: "center",
        justifyContent: "center",
    },
    saveButton: {
        backgroundColor: "#007AFF",
        marginLeft: 5,
    },
    cancelButton: {
        backgroundColor: "#E0E0E0",
        marginRight: 5,
    },
    smallButtonText: {
        color: "#fff",
        fontWeight: "600",
        fontSize: 14,
    },
    cancelButtonText: {
        color: "#333",
    },
});