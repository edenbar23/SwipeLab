import React, { useMemo, useState } from 'react';
import {
    ActivityIndicator,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View
} from 'react-native';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '../../stores/themeStore';

export interface MultiSelectOption {
    id: string | number;
    label: string;
}

interface MultiSelectProps {
    options: MultiSelectOption[];
    selectedIds: (string | number)[];
    onToggle: (id: string | number) => void;
    placeholder?: string;
    loading?: boolean;
}

export default function MultiSelect({
    options,
    selectedIds,
    onToggle,
    placeholder = 'Search...',
    loading = false
}: MultiSelectProps) {
    const [search, setSearch] = useState('');
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];

    const filteredOptions = useMemo(() => {
        return options.filter((opt) =>
            opt.label.toLowerCase().includes(search.toLowerCase())
        );
    }, [options, search]);

    const selectedOptions = useMemo(() => {
        return options.filter((opt) => selectedIds.includes(opt.id));
    }, [options, selectedIds]);

    return (
        <View style={styles.container}>
            {/* Selected Tags Horizontal Scroll */}
            {selectedOptions.length > 0 && (
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    style={styles.tagsContainer}
                >
                    {selectedOptions.map((opt) => (
                        <View key={opt.id} style={styles.tag}>
                            <Text style={styles.tagText}>{opt.label}</Text>
                            <TouchableOpacity
                                style={styles.removeButton}
                                onPress={() => onToggle(opt.id)}
                            >
                                <Text style={styles.removeText}>×</Text>
                            </TouchableOpacity>
                        </View>
                    ))}
                </ScrollView>
            )}

            {/* Search Bar */}
            <TextInput
                style={[
                    styles.searchInput,
                    {
                        backgroundColor: themeColors.background,
                        color: themeColors.text,
                        borderColor: themeColors.border,
                    },
                ]}
                value={search}
                onChangeText={setSearch}
                placeholder={placeholder}
                placeholderTextColor={themeColors.textSecondary}
            />

            {/* Dropdown Options */}
            {loading ? (
                <ActivityIndicator
                    size="small"
                    color="#10B981"
                    style={{ marginTop: 10 }}
                />
            ) : (
                <ScrollView
                    style={[
                        styles.optionsContainer,
                        {
                            backgroundColor: themeColors.background,
                            borderColor: themeColors.border,
                        },
                    ]}
                    nestedScrollEnabled
                >
                    {filteredOptions.length === 0 ? (
                        <Text style={[styles.noItems, { color: themeColors.textSecondary }]}>
                            No matches found
                        </Text>
                    ) : (
                        filteredOptions.map((opt) => {
                            const isSelected = selectedIds.includes(opt.id);
                            return (
                                <TouchableOpacity
                                    key={opt.id}
                                    style={[
                                        styles.optionItem,
                                        isSelected && styles.optionItemSelected,
                                    ]}
                                    onPress={() => {
                                        onToggle(opt.id);
                                        setSearch(''); // optionally clear search on select
                                    }}
                                >
                                    <Text
                                        style={[
                                            styles.optionText,
                                            { color: isSelected ? '#10B981' : themeColors.text },
                                        ]}
                                    >
                                        {opt.label}
                                    </Text>
                                </TouchableOpacity>
                            );
                        })
                    )}
                </ScrollView>
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        marginVertical: 4,
    },
    tagsContainer: {
        flexDirection: 'row',
        marginBottom: 8,
    },
    tag: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#10B981', // Green as requested
        paddingVertical: 6,
        paddingHorizontal: 12,
        borderRadius: 20,
        marginRight: 8,
    },
    tagText: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 14,
    },
    removeButton: {
        marginLeft: 6,
        paddingHorizontal: 4,
    },
    removeText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: 'bold',
    },
    searchInput: {
        borderWidth: 1,
        borderRadius: 8,
        padding: 10,
        fontSize: 15,
    },
    optionsContainer: {
        maxHeight: 150,
        borderWidth: 1,
        borderRadius: 8,
        marginTop: 4,
    },
    optionItem: {
        padding: 12,
        borderBottomWidth: 1,
        borderBottomColor: '#eee',
    },
    optionItemSelected: {
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
    },
    optionText: {
        fontSize: 15,
    },
    noItems: {
        padding: 12,
        textAlign: 'center',
    },
});
