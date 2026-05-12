import React, { useEffect, useState, useMemo } from "react";
import { 
    StyleSheet, 
    Text, 
    TouchableOpacity, 
    View, 
    TextInput, 
    FlatList,
    ActivityIndicator,
    Alert
} from "react-native";
import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { researcherStackParamList } from "../../navigation/researcherStack.types";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout";
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';
import { Ionicons } from '@expo/vector-icons';
import { apiFetch } from "../../api/apiFetch";

// Images
import taxonomyImg from "../../../assets/images/taxonomy.png";
import addTaskImg from "../../../assets/images/add_task.png";

type TaxonomyScreenNavigationProp = NativeStackNavigationProp<researcherStackParamList, 'Taxonomy'>;

export default function TaxonomyScreen() {
    const navigation = useNavigation<TaxonomyScreenNavigationProp>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const [searchQuery, setSearchQuery] = useState("");
    const [availableSpecies, setAvailableSpecies] = useState<{ id: string; label: string; searchTerms: string }[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedIds, setSelectedIds] = useState<string[]>([]);

    useEffect(() => {
        const fetchSpecies = async () => {
            try {
                const res = await apiFetch('/api/v1/metadata/species');
                if (res.ok) {
                    const data = await res.json();
                    setAvailableSpecies(data.map((s: any) => ({ 
                        id: String(s.id), 
                        label: String(s.label),
                        searchTerms: String(s.searchTerms || "")
                    })));
                } else {
                    Alert.alert("Error", "Failed to fetch species data");
                }
            } catch (err) {
                console.error("Failed to load species:", err);
                Alert.alert("Error", "Could not connect to the server.");
            } finally {
                setLoading(false);
            }
        };
        fetchSpecies();
    }, []);

    const filteredSpecies = useMemo(() => {
        if (!searchQuery.trim()) return [];
        const queryTerms = searchQuery.toLowerCase().split(/\s+/).filter(Boolean);
        return availableSpecies.filter(s => {
            const lowerLabel = s.label.toLowerCase();
            const lowerTerms = s.searchTerms.toLowerCase();
            return queryTerms.every(term => lowerLabel.includes(term) || lowerTerms.includes(term));
        });
    }, [searchQuery, availableSpecies]);

    const toggleSelection = (id: string) => {
        setSelectedIds(prev => 
            prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]
        );
    };

    const handleCreateSingle = (id: string) => {
        navigation.navigate("AddTask", { initialSpecies: [id] });
    };

    const handleCreateMulti = () => {
        if (selectedIds.length === 0) return;
        navigation.navigate("AddTask", { initialSpecies: selectedIds });
    };

    const renderItem = ({ item }: { item: { id: string, label: string } }) => {
        const isSelected = selectedIds.includes(item.id);

        return (
            <View style={[styles.card, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
                <TouchableOpacity 
                    style={styles.cardSelectArea}
                    onPress={() => toggleSelection(item.id)}
                    activeOpacity={0.7}
                >
                    <Ionicons 
                        name={isSelected ? "checkmark-circle" : "ellipse-outline"} 
                        size={24} 
                        color={isSelected ? "#10B981" : themeColors.textSecondary} 
                    />
                    <Text style={[styles.speciesName, { color: themeColors.text }]} numberOfLines={1}>
                        {item.label}
                    </Text>
                </TouchableOpacity>

                <TouchableOpacity 
                    style={[styles.createBtn, { backgroundColor: '#10B981' }]} 
                    onPress={() => handleCreateSingle(item.id)}
                >
                    <Ionicons name="add-outline" size={18} color="#fff" />
                    <Text style={styles.createBtnText}>Create Task</Text>
                </TouchableOpacity>
            </View>
        );
    };

    return (
        <ScreenHeaderLayout
            leftIcon={taxonomyImg}
            leftTitle="Taxonomy"
            rightIcon={addTaskImg}
            rightTitle="Add Task"
            onRightPress={() => navigation.navigate("AddTask")}
        >
            <View style={[styles.container, { backgroundColor: themeColors.background }]}>
                {/* Search Bar */}
                <View style={[styles.searchContainer, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
                    <Ionicons name="search" size={20} color={themeColors.textSecondary} />
                    <TextInput
                        style={[styles.searchInput, { color: themeColors.text }]}
                        placeholder="Search species..."
                        placeholderTextColor={themeColors.textSecondary}
                        value={searchQuery}
                        onChangeText={setSearchQuery}
                    />
                    {searchQuery.length > 0 && (
                        <TouchableOpacity onPress={() => setSearchQuery("")}>
                            <Ionicons name="close-circle" size={20} color={themeColors.textSecondary} />
                        </TouchableOpacity>
                    )}
                </View>

                {/* List */}
                {loading ? (
                    <View style={styles.centerContainer}>
                        <ActivityIndicator size="large" color="#10B981" />
                        <Text style={[styles.loadingText, { color: themeColors.textSecondary }]}>Loading species...</Text>
                    </View>
                ) : (
                    <FlatList
                        data={filteredSpecies}
                        keyExtractor={item => item.id}
                        renderItem={renderItem}
                        contentContainerStyle={styles.listContainer}
                        showsVerticalScrollIndicator={false}
                        ListEmptyComponent={
                            <View style={styles.emptyContainer}>
                                <Ionicons name={searchQuery.trim() ? "leaf-outline" : "search-outline"} size={48} color={themeColors.textSecondary} />
                                <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>
                                    {searchQuery.trim() ? "No species found" : "Search to explore taxonomy"}
                                </Text>
                            </View>
                        }
                    />
                )}

                {/* Floating Multi-select Button */}
                {selectedIds.length > 0 && (
                    <View style={styles.floatingButtonContainer}>
                        <TouchableOpacity 
                            style={[styles.floatingButton, { backgroundColor: '#10B981' }]}
                            onPress={handleCreateMulti}
                            activeOpacity={0.8}
                        >
                            <Text style={styles.floatingButtonText}>
                                Create on Selected ({selectedIds.length})
                            </Text>
                            <Ionicons name="arrow-forward" size={20} color="#fff" />
                        </TouchableOpacity>
                    </View>
                )}
            </View>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        width: '100%',
        alignItems: 'center',
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        width: '100%',
        maxWidth: 800,
        marginVertical: 16,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 12,
        borderWidth: 1,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 3,
        elevation: 2,
    },
    searchInput: {
        flex: 1,
        marginLeft: 10,
        fontSize: 16,
    },
    centerContainer: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
    },
    listContainer: {
        width: '100%',
        paddingHorizontal: 16,
        paddingBottom: 100, // accommodate bottom button
        maxWidth: 800,
        marginHorizontal: 'auto',
    },
    card: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 12,
        paddingHorizontal: 16,
        marginBottom: 10,
        borderRadius: 12,
        borderWidth: 1,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
        elevation: 1,
    },
    cardSelectArea: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
        marginRight: 10,
    },
    speciesName: {
        marginLeft: 12,
        fontSize: 16,
        fontWeight: '500',
        flexShrink: 1,
    },
    createBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 8,
        paddingHorizontal: 14,
        borderRadius: 20,
    },
    createBtnText: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 14,
        marginLeft: 4,
    },
    emptyContainer: {
        paddingTop: 60,
        alignItems: 'center',
    },
    emptyText: {
        marginTop: 16,
        fontSize: 16,
    },
    floatingButtonContainer: {
        position: 'absolute',
        bottom: 24,
        left: 0,
        right: 0,
        alignItems: 'center',
    },
    floatingButton: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 16,
        paddingHorizontal: 24,
        borderRadius: 30,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.2,
        shadowRadius: 5,
        elevation: 6,
        gap: 8,
    },
    floatingButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '700',
    },
});
