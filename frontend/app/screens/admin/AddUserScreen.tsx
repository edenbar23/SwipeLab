import React, { useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from 'react-native';
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useNavigation } from '@react-navigation/native';
import { apiFetch } from '../../api/apiFetch';
import { AdminStackParamList } from '../../navigation/adminStack.types';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { API_ENDPOINTS } from '../../api/apiEndpoints';


type NavigationProp = NativeStackNavigationProp<AdminStackParamList, 'AddUser'>;

export default function AddUserScreen() {
    const navigation = useNavigation<NavigationProp>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [role, setRole] = useState<'USER' | 'RESEARCHER' | 'ADMIN'>('USER');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async () => {
        if (!username.trim() || !email.trim()) {
            Alert.alert("Error", "Please fill in all fields");
            return;
        }

        // Email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            Alert.alert("Error", "Please enter a valid email address");
            return;
        }

        try {
            setLoading(true);
            const res = await apiFetch(API_ENDPOINTS.USERS.INVITE, {
                method: 'POST',
                body: JSON.stringify({
                    username,
                    email,
                    role
                }),
                headers: { 'Content-Type': 'application/json' }
            });

            if (res.ok) {
                Alert.alert("Success", "User invitation sent successfully", [
                    { text: "OK", onPress: () => navigation.goBack() }
                ]);
            } else {
                const data = await res.json();
                Alert.alert("Error", data.message || "Failed to invite user");
            }
        } catch (error) {
            console.error("Invite user error:", error);
            Alert.alert("Error", "An unexpected error occurred");
        } finally {
            setLoading(false);
        }
    };

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/users.png')}
            leftTitle="Add User"
            rightIcon={require('../../../assets/images/users.png')}
            rightTitle="Users"
            onRightPress={() => navigation.goBack()}
        >
            <ScrollView
                style={{ backgroundColor: themeColors.background }}
                contentContainerStyle={[styles.container, { backgroundColor: themeColors.background }]}
                showsVerticalScrollIndicator={false}
            >
                <View style={[styles.formCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>

                    {/* Username */}
                    <View style={styles.inputGroup}>
                        <Text style={[styles.label, { color: themeColors.text }]}>Username</Text>
                        <TextInput
                            style={[styles.input, {
                                backgroundColor: themeColors.background,
                                borderColor: themeColors.border,
                                color: themeColors.text
                            }]}
                            placeholder="e.g. jdoe23"
                            placeholderTextColor={themeColors.textSecondary}
                            value={username}
                            onChangeText={setUsername}
                            autoCapitalize="none"
                        />
                    </View>

                    {/* Email */}
                    <View style={styles.inputGroup}>
                        <Text style={[styles.label, { color: themeColors.text }]}>Email</Text>
                        <TextInput
                            style={[styles.input, {
                                backgroundColor: themeColors.background,
                                borderColor: themeColors.border,
                                color: themeColors.text
                            }]}
                            placeholder="e.g. john.doe@example.com"
                            placeholderTextColor={themeColors.textSecondary}
                            value={email}
                            onChangeText={setEmail}
                            keyboardType="email-address"
                            autoCapitalize="none"
                        />
                        <Text style={[styles.hint, { color: themeColors.textSecondary }]}>
                            An invitation link will be sent to this email.
                        </Text>
                    </View>

                    {/* Role Selection */}
                    <View style={styles.inputGroup}>
                        <Text style={[styles.label, { color: themeColors.text }]}>Role</Text>
                        <View style={styles.roleContainer}>
                            {(['USER', 'RESEARCHER', 'ADMIN'] as const).map((r) => (
                                <TouchableOpacity
                                    key={r}
                                    style={[
                                        styles.roleButton,
                                        { borderColor: themeColors.border },
                                        role === r && { backgroundColor: '#4B7BE5', borderColor: '#4B7BE5' }
                                    ]}
                                    onPress={() => setRole(r)}
                                >
                                    <Text style={[
                                        styles.roleText,
                                        { color: themeColors.text },
                                        role === r && { color: '#fff', fontWeight: 'bold' }
                                    ]}>{r}</Text>
                                </TouchableOpacity>
                            ))}
                        </View>
                    </View>

                    {/* Submit Button */}
                    <TouchableOpacity
                        style={[styles.submitButton, loading && styles.disabledButton]}
                        onPress={handleSubmit}
                        disabled={loading}
                    >
                        {loading ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <Text style={styles.submitText}>Send Invite</Text>
                        )}
                    </TouchableOpacity>

                </View>
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 16,
        paddingBottom: 40,
    },
    formCard: {
        borderRadius: 16,
        padding: 24,
        borderWidth: 1,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    inputGroup: {
        marginBottom: 20,
    },
    label: {
        fontSize: 16,
        fontWeight: '600',
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderRadius: 10,
        padding: 12,
        fontSize: 16,
    },
    hint: {
        fontSize: 13,
        marginTop: 6,
    },
    roleContainer: {
        flexDirection: 'row',
        gap: 10,
    },
    roleButton: {
        flex: 1,
        paddingVertical: 12,
        borderWidth: 1,
        borderRadius: 8,
        alignItems: 'center',
        justifyContent: 'center',
    },
    roleText: {
        fontSize: 14,
    },
    submitButton: {
        backgroundColor: '#4B7BE5',
        paddingVertical: 16,
        borderRadius: 12,
        alignItems: 'center',
        marginTop: 10,
    },
    disabledButton: {
        opacity: 0.7,
    },
    submitText: {
        color: '#fff',
        fontWeight: 'bold',
        fontSize: 16,
    },
});
