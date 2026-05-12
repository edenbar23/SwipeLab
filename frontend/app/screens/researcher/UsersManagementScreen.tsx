import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import React, { useState } from 'react';
import { FlatList, Image, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout";
import { researcherStackParamList } from "../../navigation/researcherStack.types";
import { useThemeStore } from '../../stores/themeStore';

// Images
import addTaskImg from "../../../assets/images/add_task.png";
import profileImg from "../../../assets/images/profile.png";
import usersImg from "../../../assets/images/users.png";
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { useAdminUsers, QUERY_KEYS } from "../../api/queries";
import { useQueryClient, useMutation } from '@tanstack/react-query';


type UsersManagementScreenNavigationProp = NativeStackNavigationProp<researcherStackParamList, 'UsersManagement'>;

interface User {
    id: string;
    username: string;
    score: number;
    active: boolean;
}

export default function UsersManagementScreen() {
    const navigation = useNavigation<UsersManagementScreenNavigationProp>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const queryClient = useQueryClient();

    const { data: users = [], isLoading: loading } = useAdminUsers();
    const [searchQuery, setSearchQuery] = useState('');

    const filteredUsers = users.filter(user => 
        user.username.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const toggleBanStatus = useMutation({
        mutationFn: async ({ username, isBanned }: { username: string, isBanned: boolean }) => {
            const endpoint = isBanned ? `/api/v1/users/unban/${username}` : `/api/v1/users/ban/${username}`;
            const res = await apiFetch(endpoint, { method: 'POST' });
            if (!res.ok) throw new Error('Failed to toggle ban status');
            return res.json();
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.allUsers });
        }
    });

    const renderItem = ({ item }: { item: User }) => (
        <TouchableOpacity style={[styles.card, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            <View style={styles.avatarContainer}>
                <Image source={profileImg} style={styles.avatar} />
            </View>
            <Text style={[styles.username, { color: themeColors.text }]}>{item.username}</Text>
            <Text style={[styles.score, { color: themeColors.textSecondary }]}>{item.score}</Text>
            
            <TouchableOpacity 
                style={[styles.actionButton, { backgroundColor: item.active ? '#ff4d4f' : '#52c41a' }]}
                onPress={() => toggleBanStatus.mutate({ username: item.username, isBanned: !item.active })}
            >
                <Text style={styles.actionButtonText}>{item.active ? 'Ban' : 'Unban'}</Text>
            </TouchableOpacity>
        </TouchableOpacity>
    );

    return (
        <ScreenHeaderLayout
            leftIcon={usersImg}
            leftTitle="Users"
            rightIcon={addTaskImg}
            rightTitle="Add User"
            onRightPress={() => navigation.navigate('AddUser')}
        >
            <View style={[styles.container, { backgroundColor: themeColors.background }]}>
                <TextInput
                    style={[styles.searchInput, { color: themeColors.text, borderColor: themeColors.border, backgroundColor: themeColors.card }]}
                    placeholder="Search users..."
                    placeholderTextColor={themeColors.textSecondary}
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
                {loading ? (
                    <Text style={{ textAlign: 'center', marginTop: 20, color: themeColors.text }}>Loading...</Text>
                ) : filteredUsers.length === 0 ? (
                    <Text style={{ textAlign: 'center', marginTop: 20, color: themeColors.textSecondary }}>No users found.</Text>
                ) : (
                    <FlatList
                        showsVerticalScrollIndicator={false}
                        data={filteredUsers}
                        renderItem={renderItem}
                        keyExtractor={item => item.id}
                        numColumns={3}
                        contentContainerStyle={styles.listContent}
                        columnWrapperStyle={styles.columnWrapper}
                    />
                )}
            </View>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        paddingHorizontal: 16,
    },
    listContent: {
        paddingTop: 16,
        paddingBottom: 32,
    },
    searchInput: {
        height: 40,
        borderWidth: 1,
        borderRadius: 8,
        paddingHorizontal: 12,
        marginTop: 16,
        marginBottom: 8,
        fontSize: 16,
    },
    columnWrapper: {
        justifyContent: 'space-between',
        marginBottom: 16,
    },
    card: {
        backgroundColor: 'white',
        borderRadius: 12,
        padding: 12,
        alignItems: 'center',
        width: '30%', // Approx 1/3 minus spacing
        shadowColor: "#000",
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.1,
        shadowRadius: 3.84,
        elevation: 3,
        borderWidth: 1,
        borderColor: '#e0e0e0',
    },
    avatarContainer: {
        width: 50,
        height: 50,
        borderRadius: 25,
        backgroundColor: '#D8BFD8', // Light purple background placeholder
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 8,
    },
    avatar: {
        width: 30,
        height: 30,
        tintColor: 'white',
    },
    username: {
        fontSize: 14,
        fontWeight: '600',
        color: '#555',
        marginBottom: 4,
        textAlign: 'center',
    },
    score: {
        fontSize: 14,
        fontWeight: 'bold',
        color: '#333',
    },
    actionButton: {
        marginTop: 10,
        paddingVertical: 6,
        paddingHorizontal: 16,
        borderRadius: 16,
    },
    actionButtonText: {
        color: 'white',
        fontWeight: 'bold',
        fontSize: 12,
    },
});
