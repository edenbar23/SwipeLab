// Researcher screen for managing tasks
import { Ionicons } from '@expo/vector-icons';
import React, { useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { useAdminTasks, useUpdateTaskStatus } from '../../api/queries';
import TaskCard from '../../components/researcher/TaskCard';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout';
import { useThemeStore } from '../../stores/themeStore';

type StatusFilter = 'ALL' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED' | 'PROCESSING';

const FILTERS: { label: string; value: StatusFilter }[] = [
  { label: 'All',        value: 'ALL'        },
  { label: 'Active',     value: 'ACTIVE'     },
  { label: 'Paused',     value: 'PAUSED'     },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Archived',   value: 'ARCHIVED'   },
];

export default function TasksManagementScreen({ navigation }: any) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const { data: tasks = [], isLoading } = useAdminTasks();
  const { mutate: updateStatus } = useUpdateTaskStatus();

  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<StatusFilter>('ALL');

  const filtered = tasks.filter((t: any) => {
    const matchesSearch = t.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = activeFilter === 'ALL' || t.status === activeFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <ScreenHeaderLayout
      leftIcon={require('../../../assets/images/tasks_mgmt.png')}
      leftTitle="Tasks"
      rightIcon={require('../../../assets/images/add_task.png')}
      rightTitle="Add Task"
      onRightPress={() => navigation.navigate('AddTask')}
    >
      <View style={[styles.container, { backgroundColor: themeColors.background }]}>

        {/* Search bar */}
        <View style={[styles.searchWrapper, { backgroundColor: isDark ? themeColors.card : '#EFF6FF', borderColor: isDark ? themeColors.border : '#BFDBFE' }]}>
          <Ionicons name="search-outline" size={16} color="#3B82F6" style={{ marginRight: 6 }} />
          <TextInput
            style={[styles.searchInput, { color: themeColors.text }]}
            placeholder="Search tasks…"
            placeholderTextColor={themeColors.textSecondary}
            value={searchQuery}
            onChangeText={setSearchQuery}
          />
          {searchQuery.length > 0 && (
            <TouchableOpacity onPress={() => setSearchQuery('')}>
              <Ionicons name="close-circle" size={16} color={themeColors.textSecondary} />
            </TouchableOpacity>
          )}
        </View>

        {/* Status filter chips */}
        <View style={styles.filtersRow}>
          {FILTERS.map(f => {
            const isActive = activeFilter === f.value;
            return (
              <TouchableOpacity
                key={f.value}
                style={[
                  styles.filterChip,
                  isActive
                    ? { backgroundColor: '#3B82F6', borderColor: '#3B82F6' }
                    : { backgroundColor: isDark ? themeColors.card : '#F0F7FF', borderColor: isDark ? themeColors.border : '#BFDBFE' },
                ]}
                onPress={() => setActiveFilter(f.value)}
              >
                <Text style={[styles.filterChipText, { color: isActive ? '#fff' : themeColors.textSecondary }]}>
                  {f.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Content */}
        {isLoading ? (
          <View style={styles.centered}>
            <ActivityIndicator size="large" color="#3B82F6" />
            <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>Loading tasks…</Text>
          </View>
        ) : filtered.length === 0 ? (
          <View style={styles.centered}>
            <Ionicons name="clipboard-outline" size={48} color="#BFDBFE" />
            <Text style={[styles.stateText, { color: themeColors.textSecondary }]}>
              {searchQuery || activeFilter !== 'ALL' ? 'No tasks match your filters.' : 'No tasks yet. Create one!'}
            </Text>
          </View>
        ) : (
          <FlatList
            showsVerticalScrollIndicator={false}
            data={filtered}
            keyExtractor={(item) => item.taskId.toString()}
            contentContainerStyle={styles.listContent}
            renderItem={({ item }) => (
              <TaskCard
                task={item}
                onPress={() => navigation.navigate('TaskDetails', { taskId: item.taskId })}
                onEdit={() => navigation.navigate('EditTask', { taskId: item.taskId })}
                onToggleStatus={() => {
                  updateStatus({ taskId: item.taskId, action: item.status === 'ACTIVE' ? 'pause' : 'activate' });
                }}
                onArchive={() => {
                  updateStatus({ taskId: item.taskId, action: 'archive' });
                }}
              />
            )}
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
  searchWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 8,
    marginTop: 14,
    marginBottom: 10,
  },
  searchInput: {
    flex: 1,
    fontSize: 14,
    padding: 0,
  },
  filtersRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 14,
  },
  filterChip: {
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 20,
    borderWidth: 1,
  },
  filterChipText: {
    fontSize: 12,
    fontWeight: '600',
  },
  listContent: {
    paddingBottom: 32,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    paddingBottom: 40,
  },
  stateText: {
    fontSize: 14,
    textAlign: 'center',
  },
});
