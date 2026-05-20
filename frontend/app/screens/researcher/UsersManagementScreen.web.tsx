import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import React, { useState } from 'react';
import { Image, StyleSheet, Text, TextInput, TouchableOpacity, View, ScrollView } from 'react-native';
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
import { useAdminUsers, QUERY_KEYS, useProfile } from "../../api/queries";
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
    const { data: profile } = useProfile();
    const [searchQuery, setSearchQuery] = useState('');

    const filteredUsers = users.filter((user: User) => 
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
                    <View style={[styles.tableContainer, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
                        {/* Table Header */}
                        <View style={[styles.tableHeader, { borderBottomColor: themeColors.border }]}>
                            <Text style={[styles.headerCell, styles.avatarCol, { color: themeColors.textSecondary }]}>Avatar</Text>
                            <Text style={[styles.headerCell, styles.usernameCol, { color: themeColors.textSecondary }]}>Username</Text>
                            <Text style={[styles.headerCell, styles.scoreCol, { color: themeColors.textSecondary }]}>Score</Text>
                            <Text style={[styles.headerCell, styles.statusCol, { color: themeColors.textSecondary }]}>Status</Text>
                            <Text style={[styles.headerCell, styles.actionCol, { color: themeColors.textSecondary }]}>Actions</Text>
                        </View>

                        {/* Table Body */}
                        <ScrollView style={styles.tableBody}>
                            {filteredUsers.map((item: User, index: number) => (
                                <View 
                                    key={item.id} 
                                    style={[
                                        styles.tableRow, 
                                        { borderBottomColor: themeColors.border },
                                        index % 2 === 0 ? { backgroundColor: theme === 'dark' ? '#1e1e1e' : '#f9f9f9' } : null
                                    ]}
                                >
                                    <View style={[styles.cell, styles.avatarCol]}>
                                        <View style={styles.avatarWrapper}>
                                            <Image source={profileImg} style={styles.avatar} />
                                        </View>
                                    </View>
                                    <View style={[styles.cell, styles.usernameCol]}>
                                        <Text style={[styles.usernameText, { color: themeColors.text }]}>{item.username}</Text>
                                    </View>
                                    <View style={[styles.cell, styles.scoreCol]}>
                                        <Text style={[styles.scoreText, { color: themeColors.text }]}>{item.score}</Text>
                                    </View>
                                    <View style={[styles.cell, styles.statusCol]}>
                                        <View style={[styles.statusBadge, { backgroundColor: item.active ? '#e6f7ff' : '#fff1f0' }]}>
                                            <Text style={[styles.statusText, { color: item.active ? '#1890ff' : '#f5222d' }]}>
                                                {item.active ? 'Active' : 'Banned'}
                                            </Text>
                                        </View>
                                    </View>
                                    <View style={[styles.cell, styles.actionCol]}>
                                        {profile?.username !== item.username ? (
                                            <TouchableOpacity 
                                                style={[styles.actionButton, { backgroundColor: item.active ? '#ff4d4f' : '#52c41a' }]}
                                                onPress={() => toggleBanStatus.mutate({ username: item.username, isBanned: !item.active })}
                                            >
                                                <Text style={styles.actionButtonText}>{item.active ? 'Ban' : 'Unban'}</Text>
                                            </TouchableOpacity>
                                        ) : (
                                            <Text style={{ color: themeColors.textSecondary, fontSize: 12, fontStyle: 'italic' }}>Current User</Text>
                                        )}
                                    </View>
                                </View>
                            ))}
                        </ScrollView>
                    </View>
                )}
            </View>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, paddingHorizontal: 24, paddingVertical: 16 },
    searchInput: { height: 44, borderWidth: 1, borderRadius: 8, paddingHorizontal: 16, marginBottom: 20, fontSize: 16 },
    
    tableContainer: {
        flex: 1,
        borderWidth: 1,
        borderRadius: 12,
        overflow: 'hidden',
    },
    tableHeader: {
        flexDirection: 'row',
        paddingVertical: 16,
        paddingHorizontal: 20,
        borderBottomWidth: 1,
        backgroundColor: 'rgba(0,0,0,0.02)',
    },
    headerCell: { fontWeight: 'bold', fontSize: 14, textTransform: 'uppercase' },
    
    tableBody: { flex: 1 },
    tableRow: {
        flexDirection: 'row',
        paddingVertical: 12,
        paddingHorizontal: 20,
        borderBottomWidth: 1,
        alignItems: 'center',
    },
    
    cell: { justifyContent: 'center' },
    
    // Column Widths
    avatarCol: { width: 60, alignItems: 'center' },
    usernameCol: { flex: 2, paddingLeft: 16 },
    scoreCol: { flex: 1 },
    statusCol: { flex: 1 },
    actionCol: { flex: 1.5, alignItems: 'flex-end' },
    
    avatarWrapper: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#D8BFD8', justifyContent: 'center', alignItems: 'center' },
    avatar: { width: 20, height: 20, tintColor: 'white' },
    
    usernameText: { fontSize: 15, fontWeight: '600' },
    scoreText: { fontSize: 15 },
    
    statusBadge: { paddingHorizontal: 12, paddingVertical: 4, borderRadius: 12 },
    statusText: { fontSize: 12, fontWeight: 'bold' },
    
    actionButton: { paddingVertical: 8, paddingHorizontal: 16, borderRadius: 6 },
    actionButtonText: { color: 'white', fontWeight: 'bold', fontSize: 13 },
});
