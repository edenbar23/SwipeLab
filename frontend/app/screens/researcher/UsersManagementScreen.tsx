import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import React, { useMemo, useState } from 'react';
import {
    FlatList,
    Image,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from 'react-native';
import useResponsive from '../../hooks/useResponsive';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import { QUERY_KEYS, useAdminUsers, useProfile } from "../../api/queries";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout";
import { researcherStackParamList } from "../../navigation/researcherStack.types";
import { useThemeStore } from '../../stores/themeStore';
import { useMutation, useQueryClient } from '@tanstack/react-query';

// Images
import addTaskImg from "../../../assets/images/add_task.png";
import profileImg from "../../../assets/images/profile.png";
import usersImg from "../../../assets/images/users.png";

type SortOrder = 'default' | 'asc' | 'desc';

type UsersManagementScreenNavigationProp = NativeStackNavigationProp<researcherStackParamList, 'UsersManagement'>;

interface User {
    id: string;
    username: string;
    score: number;
    active: boolean;
}

const SORT_ICONS: Record<SortOrder, string> = {
    default: '⇅',
    asc: '↑',
    desc: '↓',
};

const NEXT_SORT: Record<SortOrder, SortOrder> = {
    default: 'asc',
    asc: 'desc',
    desc: 'default',
};

const SORT_LABELS: Record<SortOrder, string> = {
    default: 'Default',
    asc: 'Score ↑',
    desc: 'Score ↓',
};



export default function UsersManagementScreen() {
    // Use screen width — not Platform.OS — so mobile browsers get the card layout too
    const { isPhone } = useResponsive();
    const navigation = useNavigation<UsersManagementScreenNavigationProp>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const queryClient = useQueryClient();

    const { data: users = [], isLoading: loading } = useAdminUsers();
    const { data: profile } = useProfile();
    const [searchQuery, setSearchQuery] = useState('');
    const [sortOrder, setSortOrder] = useState<SortOrder>('default');

    const displayedUsers = useMemo(() => {
        const filtered = users.filter((user: User) =>
            user.username.toLowerCase().includes(searchQuery.toLowerCase())
        );
        if (sortOrder === 'default') return filtered;
        return [...filtered].sort((a: User, b: User) =>
            sortOrder === 'asc' ? a.score - b.score : b.score - a.score
        );
    }, [users, searchQuery, sortOrder]);

    const toggleBanStatus = useMutation({
        mutationFn: async ({ username, isBanned }: { username: string; isBanned: boolean }) => {
            const endpoint = isBanned
                ? API_ENDPOINTS.USERS.UNBAN(username)
                : API_ENDPOINTS.USERS.BAN(username);
            const res = await apiFetch(endpoint, { method: 'POST' });
            if (!res.ok) throw new Error('Failed to toggle ban status');
            return res.json();
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.allUsers });
        },
    });

    // ─── Web: table layout ────────────────────────────────────────────────────
    const renderWebTable = () => (
        <View style={[webStyles.tableContainer, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            {/* Table Header */}
            <View style={[webStyles.tableHeader, { borderBottomColor: themeColors.border }]}>
                <Text style={[webStyles.headerCell, webStyles.avatarCol, { color: themeColors.textSecondary }]}>Avatar</Text>
                <TouchableOpacity
                    style={[webStyles.headerCellBtn, webStyles.usernameCol]}
                    onPress={() => setSortOrder(NEXT_SORT[sortOrder])}
                    accessibilityLabel={`Sort by score, current: ${SORT_LABELS[sortOrder]}`}
                >
                    <Text style={[webStyles.headerCellText, { color: themeColors.textSecondary }]}>Username</Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[webStyles.headerCellBtn, webStyles.scoreCol]}
                    onPress={() => setSortOrder(NEXT_SORT[sortOrder])}
                    accessibilityLabel={`Sort by score, current: ${SORT_LABELS[sortOrder]}`}
                >
                    <Text style={[webStyles.headerCellText, { color: themeColors.textSecondary }]}>
                        Score {SORT_ICONS[sortOrder]}
                    </Text>
                </TouchableOpacity>
                <Text style={[webStyles.headerCell, webStyles.statusCol, { color: themeColors.textSecondary }]}>Status</Text>
                <Text style={[webStyles.headerCell, webStyles.actionCol, { color: themeColors.textSecondary }]}>Actions</Text>
            </View>

            {/* Table Body */}
            <ScrollView style={webStyles.tableBody}>
                {displayedUsers.map((item: User, index: number) => (
                    <View
                        key={item.id}
                        style={[
                            webStyles.tableRow,
                            { borderBottomColor: themeColors.border },
                            index % 2 === 0 ? { backgroundColor: theme === 'dark' ? '#1e1e1e' : '#f9f9f9' } : null,
                        ]}
                    >
                        <View style={[webStyles.cell, webStyles.avatarCol]}>
                            <View style={webStyles.avatarWrapper}>
                                <Image source={profileImg} style={webStyles.avatar} />
                            </View>
                        </View>
                        <View style={[webStyles.cell, webStyles.usernameCol]}>
                            <Text style={[webStyles.usernameText, { color: themeColors.text }]}>{item.username}</Text>
                        </View>
                        <View style={[webStyles.cell, webStyles.scoreCol]}>
                            <Text style={[webStyles.scoreText, { color: themeColors.text }]}>{item.score}</Text>
                        </View>
                        <View style={[webStyles.cell, webStyles.statusCol]}>
                            <View style={[webStyles.statusBadge, { backgroundColor: item.active ? '#e6f7ff' : '#fff1f0' }]}>
                                <Text style={[webStyles.statusText, { color: item.active ? '#1890ff' : '#f5222d' }]}>
                                    {item.active ? 'Active' : 'Banned'}
                                </Text>
                            </View>
                        </View>
                        <View style={[webStyles.cell, webStyles.actionCol]}>
                            {profile?.username !== item.username ? (
                                <TouchableOpacity
                                    style={[webStyles.actionButton, { backgroundColor: item.active ? '#ff4d4f' : '#52c41a' }]}
                                    onPress={() => toggleBanStatus.mutate({ username: item.username, isBanned: !item.active })}
                                >
                                    <Text style={webStyles.actionButtonText}>{item.active ? 'Ban' : 'Unban'}</Text>
                                </TouchableOpacity>
                            ) : (
                                <Text style={{ color: themeColors.textSecondary, fontSize: 12, fontStyle: 'italic' }}>
                                    Current User
                                </Text>
                            )}
                        </View>
                    </View>
                ))}
            </ScrollView>
        </View>
    );

    // ─── Mobile: card grid layout ─────────────────────────────────────────────
    const renderMobileCard = ({ item }: { item: User }) => (
        <View style={[mobileStyles.card, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            {/* Avatar */}
            <View style={mobileStyles.avatarContainer}>
                <Image source={profileImg} style={mobileStyles.avatar} />
            </View>

            {/* Username */}
            <Text style={[mobileStyles.username, { color: themeColors.text }]} numberOfLines={1}>
                {item.username}
            </Text>

            {/* Score row */}
            <View style={mobileStyles.scoreRow}>
                <Text style={[mobileStyles.scoreLabel, { color: themeColors.textSecondary }]}>Score </Text>
                <Text style={[mobileStyles.scoreValue, { color: themeColors.text }]}>{item.score}</Text>
            </View>

            {/* Status badge */}
            <View style={[mobileStyles.statusBadge, { backgroundColor: item.active ? '#e6f7ff' : '#fff1f0' }]}>
                <Text style={[mobileStyles.statusText, { color: item.active ? '#1890ff' : '#f5222d' }]}>
                    {item.active ? 'Active' : 'Banned'}
                </Text>
            </View>

            {/* Action */}
            {profile?.username !== item.username && (
                <TouchableOpacity
                    style={[mobileStyles.actionButton, { backgroundColor: item.active ? '#ff4d4f' : '#52c41a' }]}
                    onPress={() => toggleBanStatus.mutate({ username: item.username, isBanned: !item.active })}
                >
                    <Text style={mobileStyles.actionButtonText}>{item.active ? 'Ban' : 'Unban'}</Text>
                </TouchableOpacity>
            )}
        </View>
    );

    return (
        <ScreenHeaderLayout
            leftIcon={usersImg}
            leftTitle="Users"
            rightIcon={addTaskImg}
            rightTitle="Add User"
            onRightPress={() => navigation.navigate('AddUser')}
        >
            <View style={[sharedStyles.container, { backgroundColor: themeColors.background }]}>

                {/* Search + Sort row */}
                <View style={sharedStyles.controlsRow}>
                    <TextInput
                        style={[sharedStyles.searchInput, {
                            color: themeColors.text,
                            borderColor: themeColors.border,
                            backgroundColor: themeColors.card,
                        }]}
                        placeholder="Search users..."
                        placeholderTextColor={themeColors.textSecondary}
                        value={searchQuery}
                        onChangeText={setSearchQuery}
                    />
                    {/* Sort button — on desktop it's on the Score column header; on mobile keep it here */}
                    {isPhone && (
                        <TouchableOpacity
                            style={[sharedStyles.sortButton, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}
                            onPress={() => setSortOrder(prev => NEXT_SORT[prev])}
                            accessibilityLabel={`Sort by score, current: ${SORT_LABELS[sortOrder]}`}
                        >
                            <Text style={[sharedStyles.sortIcon, { color: themeColors.text }]}>
                                {SORT_ICONS[sortOrder]}
                            </Text>
                        </TouchableOpacity>
                    )}
                </View>

                {loading ? (
                    <Text style={{ textAlign: 'center', marginTop: 20, color: themeColors.text }}>Loading...</Text>
                ) : displayedUsers.length === 0 ? (
                    <Text style={{ textAlign: 'center', marginTop: 20, color: themeColors.textSecondary }}>No users found.</Text>
                ) : !isPhone ? (
                    renderWebTable()
                ) : (
                    <FlatList
                        showsVerticalScrollIndicator={false}
                        data={displayedUsers}
                        renderItem={renderMobileCard}
                        keyExtractor={item => item.id}
                        numColumns={2}
                        contentContainerStyle={mobileStyles.listContent}
                        columnWrapperStyle={mobileStyles.columnWrapper}
                    />
                )}
            </View>
        </ScreenHeaderLayout>
    );
}

// ─── Shared styles ────────────────────────────────────────────────────────────
const sharedStyles = StyleSheet.create({
    container: {
        flex: 1,
        paddingHorizontal: 16,
        paddingTop: 8,
    },
    controlsRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 12,
        marginBottom: 12,
        gap: 8,
    },
    searchInput: {
        flex: 1,
        height: 44,
        borderWidth: 1,
        borderRadius: 10,
        paddingHorizontal: 14,
        fontSize: 15,
    },
    sortButton: {
        width: 44,
        height: 44,
        borderWidth: 1,
        borderRadius: 10,
        justifyContent: 'center',
        alignItems: 'center',
    },
    sortIcon: {
        fontSize: 18,
        fontWeight: '700',
    },
});

// ─── Mobile card styles ───────────────────────────────────────────────────────
const mobileStyles = StyleSheet.create({
    listContent: {
        paddingBottom: 32,
    },
    columnWrapper: {
        justifyContent: 'space-between',
        marginBottom: 12,
    },
    card: {
        borderRadius: 14,
        padding: 14,
        alignItems: 'center',
        // 2-column grid: ~48% width so there's a gap between cards
        width: '48%',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 3,
        borderWidth: 1,
    },
    avatarContainer: {
        width: 52,
        height: 52,
        borderRadius: 26,
        backgroundColor: '#D8BFD8',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 10,
    },
    avatar: {
        width: 28,
        height: 28,
        tintColor: 'white',
    },
    username: {
        fontSize: 13,
        fontWeight: '700',
        marginBottom: 6,
        textAlign: 'center',
        // Prevent overflow
        maxWidth: '100%',
    },
    scoreRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 8,
    },
    scoreLabel: {
        fontSize: 12,
        fontWeight: '500',
    },
    scoreValue: {
        fontSize: 13,
        fontWeight: '700',
    },
    statusBadge: {
        paddingHorizontal: 10,
        paddingVertical: 3,
        borderRadius: 10,
        marginBottom: 10,
    },
    statusText: {
        fontSize: 11,
        fontWeight: '700',
    },
    actionButton: {
        width: '100%',
        paddingVertical: 7,
        borderRadius: 8,
        alignItems: 'center',
    },
    actionButtonText: {
        color: 'white',
        fontWeight: '700',
        fontSize: 12,
    },
});

// ─── Web table styles ─────────────────────────────────────────────────────────
const webStyles = StyleSheet.create({
    tableContainer: {
        flex: 1,
        borderWidth: 1,
        borderRadius: 12,
        overflow: 'hidden',
    },
    tableHeader: {
        flexDirection: 'row',
        paddingVertical: 14,
        paddingHorizontal: 20,
        borderBottomWidth: 1,
        backgroundColor: 'rgba(0,0,0,0.02)',
        alignItems: 'center',
    },
    headerCell: {
        fontWeight: 'bold',
        fontSize: 13,
        textTransform: 'uppercase',
        letterSpacing: 0.5,
    },
    headerCellText: {
        fontWeight: 'bold',
        fontSize: 13,
        textTransform: 'uppercase',
        letterSpacing: 0.5,
    },
    headerCellBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
    },
    tableBody: { flex: 1 },
    tableRow: {
        flexDirection: 'row',
        paddingVertical: 12,
        paddingHorizontal: 20,
        borderBottomWidth: 1,
        alignItems: 'center',
    },
    cell: { justifyContent: 'center' },

    // Column widths
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
