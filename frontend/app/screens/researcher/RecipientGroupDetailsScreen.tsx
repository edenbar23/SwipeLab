import { useNavigation, useRoute } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import {
    Alert,
    Image,
    Modal,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    TouchableWithoutFeedback,
    View,
    Platform
} from 'react-native';
import useResponsive from '../../hooks/useResponsive';
import { Colors } from '../../../constants/theme';
import { apiFetch } from '../../api/apiFetch';
import { RecipientGroup } from '../../mocks/data/recipients.mock';
import { User } from '../../mocks/data/users.mock';
import { useThemeStore } from '../../stores/themeStore';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { useAdminUsers, useRecipients } from '../../api/queries';
import { useQueryClient } from '@tanstack/react-query';


// Utility for colors
function getColorForType(type: string) {
    switch (type) {
        case 'Student': return '#4B7BE5'; // Blue
        case 'Expert': return '#FFD700'; // Gold
        case 'Volunteer': return '#50C878'; // Green
        case 'Researcher': return '#FF6B6B'; // Red
        default: return '#ccc';
    }
}

export default function RecipientGroupDetailsScreen() {
    const navigation = useNavigation();
    const route = useRoute();
    const { group } = route.params as { group: RecipientGroup };
    const { isDesktop, isTablet, width } = useResponsive();
    const modalWidth = isDesktop ? 600 : isTablet ? 500 : width * 0.9;

    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const queryClient = useQueryClient();

    const { data: rawUsers = [] } = useAdminUsers();
    const { data: rawGroups = [], refetch: refetchGroups } = useRecipients();

    // Add Member Modal State
    const [addMemberModalVisible, setAddMemberModalVisible] = useState(false);
    const [userToRemove, setUserToRemove] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<'USERS' | 'GROUPS'>('USERS');
    const [searchQuery, setSearchQuery] = useState('');

    // Selection State
    const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
    const [selectedGroupIds, setSelectedGroupIds] = useState<number[]>([]);

    const allUsers = React.useMemo(() => rawUsers.map((u: any) => ({ ...u, id: u.username || u.id })), [rawUsers]);

    const allGroups = React.useMemo(() => {
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

    const currentGroup = allGroups.find((g: any) => g.id === group.id) || group;

    const refreshGroup = async () => {
        await refetchGroups();
    };

    const handleAddSelected = async () => {
        let finalUsernamesToAdd: string[] = [...selectedUserIds];

        // Merge users from selected groups
        const groupsToAdd = allGroups.filter((g: any) => selectedGroupIds.includes(g.id));
        groupsToAdd.forEach((g: any) => {
            g.users.forEach((u: any) => {
                if (!currentGroup.users.some((existing: any) => existing.id === u.id)) {
                    finalUsernamesToAdd.push(u.id);
                }
            });
        });
        // Unique IDs only
        finalUsernamesToAdd = Array.from(new Set(finalUsernamesToAdd));

        if (finalUsernamesToAdd.length === 0) {
            setAddMemberModalVisible(false);
            return;
        }

        try {
            // New Endpoint: /api/v1/dashboard/recipients/{id}/update
            // Payload: { addUsernames: [...] }
            const res = await apiFetch(API_ENDPOINTS.researcher.RECIPIENTS_UPDATE(currentGroup.id), {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ addUsernames: finalUsernamesToAdd })
            });

            if (res.ok) {
                setAddMemberModalVisible(false);
                setSearchQuery('');
                setSelectedUserIds([]);
                setSelectedGroupIds([]);
                refreshGroup();
            } else {
                Alert.alert("Error", "Failed to add members");
            }
        } catch (error) {
            console.error(error);
        }
    };

    const toggleUserSelection = (userId: string) => {
        if (selectedUserIds.includes(userId)) {
            setSelectedUserIds(selectedUserIds.filter(id => id !== userId));
        } else {
            setSelectedUserIds([...selectedUserIds, userId]);
        }
    };

    const toggleGroupSelection = (groupId: number) => {
        if (selectedGroupIds.includes(groupId)) {
            setSelectedGroupIds(selectedGroupIds.filter(id => id !== groupId));
        } else {
            setSelectedGroupIds([...selectedGroupIds, groupId]);
        }
    };

    const handleRemoveUser = (userId: string) => {
        setUserToRemove(userId);
    };

    const confirmRemoveUser = async () => {
        if (!userToRemove) return;
        const userId = userToRemove;
        
        try {
            const url = API_ENDPOINTS.researcher.RECIPIENTS_UPDATE(currentGroup.id);
            const res = await apiFetch(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ removeUsernames: [userId] })
            });
            
            if (res.ok) {
                refreshGroup();
            } else {
                Alert.alert("Error", "Failed to remove user");
            }
        } catch (e) {
            console.error("[confirmRemoveUser] Exception during fetch:", e);
            Alert.alert("Error", "Network error occurred while removing user.");
        } finally {
            setUserToRemove(null);
        }
    };

    // --- Composition Logic ---
    const getComposition = () => {
        if (!currentGroup || !currentGroup.users) return [];
        const total = currentGroup.users.length;
        if (total === 0) return [];
        const counts: Record<string, number> = {};
        currentGroup.users.forEach((u: any) => {
            const type = u.description || 'Other';
            counts[type] = (counts[type] || 0) + 1;
        });
        return Object.keys(counts).map(type => ({
            type,
            count: counts[type],
            percent: Math.round((counts[type] / total) * 100),
            color: getColorForType(type)
        }));
    };

    const composition = getComposition();

    return (
        <View style={[styles.container, { backgroundColor: themeColors.background }]}>
            <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                <Text style={[styles.backButtonText, { color: '#4B7BE5' }]}>← Back to Lists</Text>
            </TouchableOpacity>

            <View style={styles.header}>
                <Text style={[styles.title, { color: themeColors.text }]}>{currentGroup.name}</Text>
                <TouchableOpacity onPress={() => setAddMemberModalVisible(true)} style={styles.addBtn}>
                    <Image source={require('../../../assets/images/users.png')} style={{ width: 24, height: 24, tintColor: '#fff' }} />
                    <Text style={styles.addBtnText}>+</Text>
                </TouchableOpacity>
            </View>

            {/* Composition Bar */}
            {composition.length > 0 && (
                <View style={styles.compositionContainer}>
                    <Text style={styles.compTitle}>Group Composition:</Text>
                    <View style={styles.barContainer}>
                        {composition.map((comp) => (
                            <View key={comp.type} style={[styles.barSegment, { flex: comp.percent, backgroundColor: comp.color }]}>
                                {comp.percent > 10 && <Text style={styles.barText}>{comp.percent}%</Text>}
                            </View>
                        ))}
                    </View>
                    <View style={styles.legendContainer}>
                        {composition.map(comp => (
                            <View key={comp.type} style={styles.legendItem}>
                                <View style={[styles.dot, { backgroundColor: comp.color }]} />
                                <Text style={styles.legendText}>{comp.type} ({comp.count})</Text>
                            </View>
                        ))}
                    </View>
                </View>
            )}

            <ScrollView contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
                {!currentGroup || !currentGroup.users || currentGroup.users.length === 0 ? (
                    <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>No users in this group.</Text>
                ) : (
                    currentGroup.users.map((user: any) => (
                        <View key={user.id} style={[styles.userRow, { backgroundColor: themeColors.card }]}>
                            <View>
                                <Text style={[styles.username, { color: themeColors.text }]}>{user.username}</Text>
                                <Text style={[styles.userDesc, { color: themeColors.textSecondary }]}>{user.description || 'User'}</Text>
                            </View>
                            <TouchableOpacity onPress={() => handleRemoveUser(user.id)} style={styles.deleteBtn}>
                                <Text style={styles.deleteText}>🗑️</Text>
                            </TouchableOpacity>
                        </View>
                    ))
                )}
            </ScrollView>

            {/* Remove User Confirmation Modal */}
            <Modal visible={!!userToRemove} animationType="fade" transparent>
                <TouchableWithoutFeedback onPress={() => setUserToRemove(null)}>
                    <View style={styles.modalOverlay}>
                        <TouchableWithoutFeedback onPress={() => {}}>
                            <View style={[styles.modalContent, { width: modalWidth, backgroundColor: themeColors.card, alignItems: 'center' }]}>
                                <Text style={[styles.modalTitle, { color: themeColors.text, marginBottom: 8 }]}>Remove User</Text>
                                <Text style={{ color: themeColors.textSecondary, marginBottom: 20, textAlign: 'center' }}>
                                    Are you sure you want to remove this user from the group?
                                </Text>
                                
                                <View style={{ flexDirection: 'row', justifyContent: 'center', width: '100%', gap: 16 }}>
                                    <TouchableOpacity 
                                        style={[styles.closeBtn, { backgroundColor: '#f0f0f0', borderRadius: 8, paddingHorizontal: 24 }]} 
                                        onPress={() => setUserToRemove(null)}
                                    >
                                        <Text style={{ color: '#333', fontWeight: 'bold' }}>Cancel</Text>
                                    </TouchableOpacity>
                                    <TouchableOpacity 
                                        style={[styles.confirmBtn, { backgroundColor: '#FF6B6B' }]} 
                                        onPress={confirmRemoveUser}
                                    >
                                        <Text style={styles.confirmText}>Remove</Text>
                                    </TouchableOpacity>
                                </View>
                            </View>
                        </TouchableWithoutFeedback>
                    </View>
                </TouchableWithoutFeedback>
            </Modal>

            {/* Advanced Add Member Modal */}
            <Modal visible={addMemberModalVisible} animationType="fade" transparent>
                <TouchableWithoutFeedback onPress={() => setAddMemberModalVisible(false)}>
                    <View style={styles.modalOverlay}>
                        <TouchableWithoutFeedback onPress={() => { }}>
                            <View style={[styles.modalContent, { width: modalWidth, maxHeight: '80%', backgroundColor: themeColors.card }]}>
                                <Text style={[styles.modalTitle, { color: themeColors.text }]}>Add Members</Text>

                                {/* Tabs */}
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

                                <ScrollView style={[styles.userList, { borderColor: themeColors.border }]} nestedScrollEnabled showsVerticalScrollIndicator={false}>
                                    {activeTab === 'USERS' ? (
                                        // Users List
                                        allUsers
                                            .filter((u: any) => u.username.toLowerCase().includes(searchQuery.toLowerCase()))
                                            .map((u: any) => {
                                                const isAlreadyIn = currentGroup && currentGroup.users ? currentGroup.users.some((existing: any) => existing.id === u.id) : false;
                                                if (isAlreadyIn) return null;
                                                const isSelected = selectedUserIds.includes(u.id);
                                                return (
                                                    <TouchableOpacity key={u.id} style={[styles.userRowModal, isSelected && styles.userRowSelected, { borderBottomColor: themeColors.border }]} onPress={() => toggleUserSelection(u.id)}>
                                                        <Text style={[styles.userRowText, isSelected && styles.userRowTextSelected, { color: isSelected ? '#4B7BE5' : themeColors.text }]}>{isSelected ? '☑' : '☐'} {u.username}</Text>
                                                        <Text style={[styles.userRowDesc, { color: themeColors.textSecondary }]}>{u.description}</Text>
                                                    </TouchableOpacity>
                                                );
                                            })
                                    ) : (
                                        // Groups List
                                        allGroups
                                            .filter((g: any) => g.name.toLowerCase().includes(searchQuery.toLowerCase()))
                                            .map((g: any) => {
                                                if (currentGroup && g.id === currentGroup.id) return null; // Don't add self
                                                const isSelected = selectedGroupIds.includes(g.id);
                                                return (
                                                    <TouchableOpacity key={g.id} style={[styles.userRowModal, isSelected && styles.userRowSelected, { borderBottomColor: themeColors.border }]} onPress={() => toggleGroupSelection(g.id)}>
                                                        <Text style={[styles.userRowText, isSelected && styles.userRowTextSelected, { color: isSelected ? '#4B7BE5' : themeColors.text }]}>{isSelected ? '☑' : '☐'} {g.name}</Text>
                                                        <Text style={[styles.userRowDesc, { color: themeColors.textSecondary }]}>{g.usersCount} users</Text>
                                                    </TouchableOpacity>
                                                );
                                            })
                                    )}
                                </ScrollView>

                                <View style={styles.modalActions}>
                                    <TouchableOpacity style={styles.cancelBtn} onPress={() => setAddMemberModalVisible(false)}>
                                        <Text style={styles.cancelText}>Cancel</Text>
                                    </TouchableOpacity>
                                    <TouchableOpacity style={styles.createBtn} onPress={handleAddSelected}>
                                        <Text style={styles.createText}>
                                            Add ({activeTab === 'USERS' ? selectedUserIds.length : selectedGroupIds.length})
                                        </Text>
                                    </TouchableOpacity>
                                </View>
                            </View>
                        </TouchableWithoutFeedback>
                    </View>
                </TouchableWithoutFeedback>
            </Modal>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#fff', padding: 16 },
    backButton: { marginBottom: 16, paddingVertical: 4 },
    backButtonText: { fontSize: 16, color: '#003366', fontWeight: '500' },

    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
    title: { fontSize: 24, fontWeight: 'bold', color: '#003366' },
    addBtn: { flexDirection: 'row', backgroundColor: '#4B7BE5', padding: 8, borderRadius: 8, alignItems: 'center' },
    addBtnText: { color: '#fff', fontWeight: 'bold', marginLeft: 4, fontSize: 18 },

    // Composition Styles
    compositionContainer: { marginBottom: 20 },
    compTitle: { fontSize: 14, fontWeight: 'bold', color: '#666', marginBottom: 6 },
    barContainer: { flexDirection: 'row', height: 24, borderRadius: 12, overflow: 'hidden', marginBottom: 8 },
    barSegment: { justifyContent: 'center', alignItems: 'center' },
    barText: { fontSize: 10, color: '#fff', fontWeight: 'bold' },
    legendContainer: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
    legendItem: { flexDirection: 'row', alignItems: 'center' },
    dot: { width: 10, height: 10, borderRadius: 5, marginRight: 4 },
    legendText: { fontSize: 12, color: '#555' },

    list: {
        ...Platform.select({
            web: {
                flexDirection: 'row',
                flexWrap: 'wrap',
                justifyContent: 'space-between',
            },
            default: {},
        }),
    },
    userRow: { 
        flexDirection: 'row', 
        justifyContent: 'space-between', 
        padding: 12, 
        backgroundColor: '#f9f9f9', 
        borderRadius: 8, 
        marginBottom: 8, 
        alignItems: 'center',
        ...Platform.select({
            web: {
                width: '49%', // Grid view on web (2 columns)
            },
            default: {
                width: '100%',
            }
        }),
    },
    username: { fontSize: 16, fontWeight: '600', color: '#333' },
    userDesc: { fontSize: 12, color: '#666' },
    emptyText: { textAlign: 'center', color: '#888', marginTop: 20, width: '100%' },
    deleteBtn: { padding: 4 },
    deleteText: { fontSize: 18 },

    // Modal Styles
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#fff', borderRadius: 12, padding: 20 },
    modalTitle: { fontSize: 20, fontWeight: 'bold', marginBottom: 16, textAlign: 'center' },

    tabsContainer: { flexDirection: 'row', marginBottom: 12, borderBottomWidth: 1, borderColor: '#eee' },
    tab: { flex: 1, paddingVertical: 12, alignItems: 'center' },
    activeTab: { borderBottomWidth: 2, borderColor: '#4B7BE5' },
    tabText: { fontSize: 14, color: '#888' },
    activeTabText: { color: '#4B7BE5', fontWeight: 'bold' },

    userList: { maxHeight: 200, marginBottom: 20, borderWidth: 1, borderColor: '#eee', borderRadius: 8 },
    userRowModal: { flexDirection: 'row', justifyContent: 'space-between', padding: 10, borderBottomWidth: 1, borderBottomColor: '#f9f9f9' },
    userRowSelected: { backgroundColor: '#eef2ff' },
    userRowText: { fontSize: 14, color: '#333' },
    userRowTextSelected: { fontWeight: 'bold', color: '#4B7BE5' },
    userRowDesc: { fontSize: 12, color: '#999' },
    
    searchInput: { borderWidth: 1, borderColor: '#eee', borderRadius: 8, padding: 8, marginBottom: 12, backgroundColor: '#f9f9f9', fontSize: 14 },
    
    modalActions: { flexDirection: 'row', justifyContent: 'space-between' },
    cancelBtn: { flex: 1, marginRight: 8, alignItems: 'center', padding: 12 },
    cancelText: { color: 'red', fontWeight: 'bold' },
    createBtn: { flex: 1, marginLeft: 8, backgroundColor: '#4B7BE5', borderRadius: 8, alignItems: 'center', padding: 12 },
    createText: { color: '#fff', fontWeight: 'bold' },
    closeBtn: { padding: 12 },
    confirmBtn: { backgroundColor: '#4B7BE5', paddingVertical: 12, paddingHorizontal: 24, borderRadius: 8 },
    confirmText: { color: '#fff', fontWeight: 'bold' }
});
