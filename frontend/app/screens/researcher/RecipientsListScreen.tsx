import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Modal,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    TouchableWithoutFeedback,
    View
} from 'react-native';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import useResponsive from '../../hooks/useResponsive';
import { RecipientGroup } from '../../mocks/data/recipients.mock';
import { User } from '../../mocks/data/users.mock';
import { researcherStackParamList } from '../../navigation/researcherStack.types';
import { useThemeStore } from '../../stores/themeStore';
import { useAdminUsers, useRecipients } from '../../api/queries';


type NavigationProp = NativeStackNavigationProp<researcherStackParamList, 'RecipientsList'>;

export default function RecipientsListScreen() {
    const navigation = useNavigation<NavigationProp>();

    // Responsive layout
    const { isDesktop, isTablet, width } = useResponsive();
    const modalWidth = isDesktop ? 600 : isTablet ? 500 : width * 0.9;

    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const { data: rawUsers = [] } = useAdminUsers();
    const { data: rawGroups = [], isLoading: groupsLoading, refetch: refetchGroups, isRefetching } = useRecipients();

    // Create Modal
    const [createModalVisible, setCreateModalVisible] = useState(false);
    const [newGroupName, setNewGroupName] = useState('');
    const [createErrorMsg, setCreateErrorMsg] = useState('');

    // User Selection for New Group
    const [activeTab, setActiveTab] = useState<'USERS' | 'GROUPS'>('USERS');
    const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
    const [selectedGroupIds, setSelectedGroupIds] = useState<number[]>([]);
    const [searchQuery, setSearchQuery] = useState('');

    const allUsers = React.useMemo(() => rawUsers.map((u: any) => ({ ...u, id: u.username || u.id })), [rawUsers]);

    const groups = React.useMemo(() => {
        return rawGroups.map((g: any) => ({
            id: g.groupId,
            name: g.name,
            usersCount: g.userCount || g.usernames?.length || 0,
            users: (g.usernames || []).map((uname: string) => {
                const u = allUsers.find((user: any) => (user.username || user.id) === uname);
                return u || { id: uname, username: uname, description: 'User' };
            })
        }));
    }, [rawGroups, allUsers]);

    const loading = groupsLoading;
    const refreshing = isRefetching;

    const onRefresh = () => {
        refetchGroups();
    };

    const handleCreateGroup = async () => {
        setCreateErrorMsg('');
        const trimmedName = newGroupName.trim();
        if (!trimmedName || trimmedName.length < 3 || trimmedName.length > 100) {
            setCreateErrorMsg("Group name must be between 3 and 100 characters");
            return;
        }

        // Merge selected users + users from selected groups
        let finalUsernames: string[] = [...selectedUserIds];
        const groupsToAdd = groups.filter((g: any) => selectedGroupIds.includes(g.id));
        groupsToAdd.forEach((g: any) => {
            g.users.forEach((u: any) => {
                finalUsernames.push(u.id); // id is now username (string)
            });
        });
        // Deduplicate
        finalUsernames = Array.from(new Set(finalUsernames));

        try {
            // New Endpoint: /api/v1/dashboard/recipients/create
            // Payload: { name, usernames: [...] }
            const res = await apiFetch(API_ENDPOINTS.researcher.RECIPIENTS_CREATE, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    name: newGroupName,
                    usernames: finalUsernames
                })
            });

            if (res.ok) {
                setCreateModalVisible(false);
                setNewGroupName('');
                setSearchQuery('');
                setSelectedUserIds([]);
                setSelectedGroupIds([]);
                setCreateErrorMsg('');
                refetchGroups();
            } else {
                let errorMsg = "Failed to create group";
                try {
                    const errorText = await res.text();
                    try {
                        const errorData = JSON.parse(errorText);
                        errorMsg = errorData.message || errorMsg;
                    } catch (e) {
                        errorMsg = errorText || errorMsg;
                    }
                } catch (e) {
                    console.error("Failed to read error response", e);
                }

                setCreateErrorMsg(errorMsg);
            }
        } catch (error) {
            console.error(error);
            setCreateErrorMsg("Network error occurred");
        }
    };

    if (loading) {
        return (
            <View style={[styles.loadingContainer, { backgroundColor: themeColors.background }]}>
                <ActivityIndicator size="large" color="#4B7BE5" />
            </View>
        );
    }

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/recipients_lists.png')}
            leftTitle="Recipients List"
            rightIcon={require('../../../assets/images/add_list.png')}
            rightTitle="New List"
            onRightPress={() => {
                setCreateErrorMsg('');
                setCreateModalVisible(true);
            }}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={[styles.container, { backgroundColor: themeColors.background }]}
                contentContainerStyle={styles.content}
                showsVerticalScrollIndicator={false}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
            >

                <View style={styles.grid}>
                    {groups.map((group: any) => {
                        // Calculate mini-composition
                        const total = group.users ? group.users.length : 0;
                        const composition = group.users ? (() => {
                            const counts: Record<string, number> = {};
                            group.users.forEach((u: any) => {
                                const type = u.description || 'Other';
                                counts[type] = (counts[type] || 0) + 1;
                            });
                            return Object.keys(counts).map(type => ({
                                type,
                                percent: total > 0 ? (counts[type] / total) * 100 : 0,
                                color: getColorForType(type)
                            }));
                        })() : [];

                        return (
                            <TouchableOpacity
                                key={group.id}
                                style={[styles.card, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}
                                onPress={() => navigation.navigate('RecipientGroupDetails', { group })}
                            >
                                <Text style={[styles.groupName, { color: themeColors.text }]}>{group.name}</Text>

                                {/* Detailed Composition Bar */}
                                {total > 0 && (
                                    <View style={styles.miniBarContainer}>
                                        {composition.map((c, i) => (
                                            <View key={i} style={[styles.barSegment, { flex: c.percent, backgroundColor: c.color }]}>
                                                {c.percent > 15 && (
                                                    <Text style={styles.barLabel} numberOfLines={1}>
                                                        {c.type} {Math.round(c.percent)}%
                                                    </Text>
                                                )}
                                            </View>
                                        ))}
                                    </View>
                                )}

                                <Text style={[styles.usersCount, { color: themeColors.textSecondary }]}>{total} users</Text>
                            </TouchableOpacity>
                        );
                    })}
                </View>

                {/* Create Group Modal */}
                <Modal visible={createModalVisible} animationType="fade" transparent>
                    <TouchableWithoutFeedback onPress={() => setCreateModalVisible(false)}>
                        <View style={styles.modalOverlay}>
                            <TouchableWithoutFeedback onPress={() => { }}>
                                <View style={[styles.modalContent, { width: modalWidth, maxHeight: '80%', backgroundColor: themeColors.card }]}>
                                    <Text style={[styles.modalTitle, { color: themeColors.text }]}>Create New List</Text>

                                    {!!createErrorMsg && (
                                        <Text style={{ color: '#FF6B6B', marginBottom: 12, textAlign: 'center', fontWeight: '500' }}>{createErrorMsg}</Text>
                                    )}

                                    <TextInput
                                        style={[styles.input, { color: themeColors.text, borderColor: themeColors.border, backgroundColor: themeColors.background, marginBottom: 4 }]}
                                        placeholder="Enter a new group name"
                                        placeholderTextColor={themeColors.textSecondary}
                                        value={newGroupName}
                                        onChangeText={setNewGroupName}
                                    />
                                    <Text style={{ fontSize: 12, color: themeColors.textSecondary, marginBottom: 16, marginLeft: 4 }}>
                                        Group name must be between 3 and 100 characters.
                                    </Text>

                                    <View style={styles.tabsContainer}>
                                        <TouchableOpacity
                                            style={[styles.tab, activeTab === 'USERS' && styles.activeTab]}
                                            onPress={() => setActiveTab('USERS')}
                                        >
                                            <Text style={[styles.tabText, activeTab === 'USERS' && styles.activeTabText]}>Users</Text>
                                        </TouchableOpacity>
                                        <TouchableOpacity
                                            style={[styles.tab, activeTab === 'GROUPS' && styles.activeTab]}
                                            onPress={() => setActiveTab('GROUPS')}
                                        >
                                            <Text style={[styles.tabText, activeTab === 'GROUPS' && styles.activeTabText]}>Groups</Text>
                                        </TouchableOpacity>
                                    </View>

                                    {/* Search Bar */}
                                    <TextInput
                                        style={[styles.searchInput, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
                                        placeholder="Search..."
                                        placeholderTextColor={themeColors.textSecondary}
                                        value={searchQuery}
                                        onChangeText={setSearchQuery}
                                    />

                                    <Text style={[styles.subLabel, { color: themeColors.textSecondary }]}>{activeTab === 'USERS' ? 'Select Users' : 'Select Groups to Merge'}</Text>
                                    <ScrollView style={[styles.userList, { borderColor: themeColors.border }]} nestedScrollEnabled showsVerticalScrollIndicator={false}>
                                        {activeTab === 'USERS' ? (
                                            allUsers
                                                .filter((u: any) => u.username.toLowerCase().includes(searchQuery.toLowerCase()))
                                                .map((u: any) => {
                                                    const isSelected = selectedUserIds.includes(u.id);
                                                    return (
                                                        <TouchableOpacity
                                                            key={u.id}
                                                            style={[styles.userRow, isSelected && styles.userRowSelected, { borderBottomColor: themeColors.border }]}
                                                            onPress={() => {
                                                                if (isSelected) {
                                                                    setSelectedUserIds(prev => prev.filter(id => id !== u.id));
                                                                } else {
                                                                    setSelectedUserIds(prev => [...prev, u.id]);
                                                                }
                                                            }}
                                                        >
                                                            <Text style={[styles.userRowText, isSelected && styles.userRowTextSelected, { color: isSelected ? '#4B7BE5' : themeColors.text }]}>
                                                                {isSelected ? '☑' : '☐'} {u.username}
                                                            </Text>
                                                            <Text style={styles.userRowDesc}>{u.description}</Text>
                                                        </TouchableOpacity>
                                                    );
                                                })
                                        ) : (
                                            groups
                                                .filter((g: any) => g.name.toLowerCase().includes(searchQuery.toLowerCase()))
                                                .map((g: any) => {
                                                    const isSelected = selectedGroupIds.includes(g.id);
                                                    return (
                                                        <TouchableOpacity
                                                            key={g.id}
                                                            style={[styles.userRow, isSelected && styles.userRowSelected, { borderBottomColor: themeColors.border }]}
                                                            onPress={() => {
                                                                if (isSelected) {
                                                                    setSelectedGroupIds(prev => prev.filter(id => id !== g.id));
                                                                } else {
                                                                    setSelectedGroupIds(prev => [...prev, g.id]);
                                                                }
                                                            }}
                                                        >
                                                            <Text style={[styles.userRowText, isSelected && styles.userRowTextSelected, { color: isSelected ? '#4B7BE5' : themeColors.text }]}>
                                                                {isSelected ? '☑' : '☐'} {g.name}
                                                            </Text>
                                                            <Text style={styles.userRowDesc}>{g.users ? g.users.length : 0} users</Text>
                                                        </TouchableOpacity>
                                                    );
                                                })
                                        )}
                                    </ScrollView>

                                    <View style={styles.modalActions}>
                                        <TouchableOpacity onPress={() => setCreateModalVisible(false)} style={styles.cancelBtn}>
                                            <Text style={styles.cancelText}>Cancel</Text>
                                        </TouchableOpacity>
                                        <TouchableOpacity onPress={handleCreateGroup} style={styles.createBtn}>
                                            <Text style={styles.createText}>
                                                Create ({selectedUserIds.length + selectedGroupIds.length > 0 ? 'Selected' : ''})
                                            </Text>
                                        </TouchableOpacity>
                                    </View>
                                </View>
                            </TouchableWithoutFeedback>
                        </View>
                    </TouchableWithoutFeedback>
                </Modal>
            </ScrollView >
        </ScreenHeaderLayout >
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    content: {
        padding: 16,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    // Grid Layout
    grid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
    },
    card: {
        backgroundColor: '#F0F0F0', // Light Gray
        borderRadius: 16,
        padding: 16,
        marginBottom: 16,
        alignItems: 'center',
        // Shadow
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
        borderWidth: 1,
        borderColor: '#E0E0E0',
        width: '48%', // tile width ~half screen
    },
    groupName: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#333',
        marginBottom: 8,
        textAlign: 'center',
    },
    usersCount: {
        fontSize: 14,
        color: '#666',
        textAlign: 'center',
    },
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#fff', borderRadius: 12, padding: 20 },
    modalTitle: { fontSize: 20, fontWeight: 'bold', marginBottom: 16, textAlign: 'center' },
    subLabel: { fontSize: 14, fontWeight: '600', color: '#666', marginBottom: 8 },
    userList: { maxHeight: 200, marginBottom: 20, borderWidth: 1, borderColor: '#eee', borderRadius: 8 },
    userRow: { flexDirection: 'row', justifyContent: 'space-between', padding: 10, borderBottomWidth: 1, borderBottomColor: '#f9f9f9' },
    userRowSelected: { backgroundColor: '#eef2ff' },
    userRowText: { fontSize: 14, color: '#333' },
    userRowTextSelected: { fontWeight: 'bold', color: '#4B7BE5' },
    tabsContainer: { flexDirection: 'row', marginBottom: 12, borderBottomWidth: 1, borderColor: '#eee' },
    tab: { flex: 1, paddingVertical: 12, alignItems: 'center' },
    activeTab: { borderBottomWidth: 2, borderColor: '#4B7BE5' },
    tabText: { fontSize: 14, color: '#888' },
    activeTabText: { color: '#4B7BE5', fontWeight: 'bold' },
    userRowDesc: { fontSize: 12, color: '#999' },
    input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 12, marginBottom: 12 },
    searchInput: { borderWidth: 1, borderColor: '#eee', borderRadius: 8, padding: 8, marginBottom: 12, backgroundColor: '#f9f9f9', fontSize: 14 },
    modalActions: { flexDirection: 'row', justifyContent: 'space-between' },
    cancelBtn: { flex: 1, marginRight: 8, alignItems: 'center', padding: 12 },
    cancelText: { color: 'red', fontWeight: 'bold' },
    createBtn: { flex: 1, marginLeft: 8, backgroundColor: '#4B7BE5', borderRadius: 8, alignItems: 'center', padding: 12 },
    createText: { color: '#fff', fontWeight: 'bold' },

    // Mini Bar Styles
    miniBarContainer: {
        flexDirection: 'row',
        height: 22, // Increased height
        width: '100%', // Full width of card padding area
        borderRadius: 6,
        overflow: 'hidden',
        marginBottom: 8,
        backgroundColor: '#eee'
    },
    barSegment: {
        justifyContent: 'center',
        alignItems: 'center',
        overflow: 'hidden'
    },
    barLabel: {
        color: '#fff',
        fontSize: 10,
        fontWeight: 'bold',
        textAlign: 'center'
    }
});

function getColorForType(type: string) {
    switch (type) {
        case 'Student': return '#4B7BE5'; // Blue
        case 'Expert': return '#FFD700'; // Gold
        case 'Volunteer': return '#50C878'; // Green
        case 'Researcher': return '#FF6B6B'; // Red
        default: return '#ccc';
    }
}
