import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Image,
    Modal,
    RefreshControl,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { apiFetch } from '../../api/apiFetch';
import { AdminStackParamList } from '../../navigation/adminStack.types';
import { RecipientGroup } from '../../mocks/data/recipients.mock';

type NavigationProp = NativeStackNavigationProp<AdminStackParamList, 'RecipientsList'>;

export default function RecipientsListScreen() {
    const navigation = useNavigation<NavigationProp>();
    const [groups, setGroups] = useState<RecipientGroup[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    // Create Modal
    const [createModalVisible, setCreateModalVisible] = useState(false);
    const [newGroupName, setNewGroupName] = useState('');

    const fetchGroups = async () => {
        try {
            const res = await apiFetch('/api/v1/manager/recipient-groups');
            if (res.ok) {
                const data = await res.json();
                setGroups(data);
            }
        } catch (error) {
            console.error('Failed to fetch recipient groups', error);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        // Refresh when navigating back to see updated counts
        const unsubscribe = navigation.addListener('focus', () => {
            fetchGroups();
        });
        return unsubscribe;
    }, [navigation]);

    const onRefresh = () => {
        setRefreshing(true);
        fetchGroups();
    };

    const handleCreateGroup = async () => {
        if (!newGroupName.trim()) {
            Alert.alert("Error", "Please enter a group name");
            return;
        }

        try {
            const res = await apiFetch('/api/v1/manager/recipient-groups', {
                method: 'POST',
                body: JSON.stringify({ name: newGroupName })
            });

            if (res.ok) {
                setCreateModalVisible(false);
                setNewGroupName('');
                fetchGroups();
            } else {
                Alert.alert("Error", "Failed to create group");
            }
        } catch (error) {
            console.error(error);
        }
    };

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#4B7BE5" />
            </View>
        );
    }

    return (
        <ScrollView
            style={styles.container}
            contentContainerStyle={styles.content}
            refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
            <View style={styles.header}>
                <Image
                    source={require('../../../assets/images/users.png')}
                    style={styles.headerIcon}
                />
                <Text style={styles.screenTitle}>Recipients List</Text>

                <TouchableOpacity style={styles.newListButton} onPress={() => setCreateModalVisible(true)}>
                    <View style={styles.newListIconPlaceholder}>
                        <Text style={styles.plusText}>+</Text>
                    </View>
                    <Text style={styles.newListText}>New List</Text>
                </TouchableOpacity>
            </View>

            <View style={styles.grid}>
                {groups.map(group => {
                    // Calculate mini-composition
                    const total = group.users ? group.users.length : 0;
                    const composition = group.users ? (() => {
                        const counts: Record<string, number> = {};
                        group.users.forEach(u => {
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
                            style={styles.card}
                            onPress={() => navigation.navigate('RecipientGroupDetails', { group })}
                        >
                            <Text style={styles.groupName}>{group.name}</Text>

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

                            <Text style={styles.usersCount}>{total} users</Text>
                        </TouchableOpacity>
                    );
                })}
            </View>

            {/* Create Group Modal */}
            <Modal visible={createModalVisible} animationType="fade" transparent>
                <View style={styles.modalContainer}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>Create New List</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="Group Name"
                            value={newGroupName}
                            onChangeText={setNewGroupName}
                        />
                        <View style={styles.modalActions}>
                            <TouchableOpacity onPress={() => setCreateModalVisible(false)} style={styles.cancelBtn}>
                                <Text style={styles.cancelText}>Cancel</Text>
                            </TouchableOpacity>
                            <TouchableOpacity onPress={handleCreateGroup} style={styles.createBtn}>
                                <Text style={styles.createText}>Create</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </ScrollView>
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
    header: {
        alignItems: 'flex-start',
        marginBottom: 20,
    },
    headerIcon: {
        width: 60,
        height: 60,
        marginBottom: 8,
        tintColor: '#4B7BE5' // Adjust color if needed
    },
    screenTitle: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#003366', // Dark Blue
        marginBottom: 16,
    },
    newListButton: {
        position: 'absolute',
        right: 0,
        top: 20,
        alignItems: 'center',
    },
    newListIconPlaceholder: {
        width: 40,
        height: 30,
        borderRadius: 8,
        backgroundColor: '#4B7BE5',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 4,
    },
    plusText: {
        color: '#fff',
        fontSize: 20,
        fontWeight: 'bold',
    },
    newListText: {
        fontSize: 12,
        color: '#888',
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
    modalContainer: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', padding: 30 },
    modalContent: { backgroundColor: '#fff', borderRadius: 12, padding: 20 },
    modalTitle: { fontSize: 20, fontWeight: 'bold', marginBottom: 16, textAlign: 'center' },
    input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 12, marginBottom: 20 },
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
