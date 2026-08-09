// Export Modal — Issue #257
// Full-screen modal for selecting tasks and triggering CSV export.
import React, { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '@/stores/themeStore';
import MultiSelect, { MultiSelectOption } from '@/components/ui/MultiSelect';
import { useAdminTasks, useExportClassificationsCsv } from '@/api/queries';
import { downloadCsvBlob } from '@/services/csvDownload';

interface ExportModalProps {
  visible: boolean;
  onClose: () => void;
}

export default function ExportModal({ visible, onClose }: ExportModalProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const [selectedTaskIds, setSelectedTaskIds] = useState<(string | number)[]>([]);

  const { data: tasks = [], isLoading: tasksLoading } = useAdminTasks();
  const exportMutation = useExportClassificationsCsv();

  // Map tasks to MultiSelect options
  const taskOptions: MultiSelectOption[] = useMemo(() => {
    if (!Array.isArray(tasks)) return [];
    return tasks.map((task: any) => ({
      id: task.taskId ?? task.id,
      label: task.name ?? task.title ?? `Task #${task.taskId ?? task.id}`,
      searchTerms: `${task.status ?? ''} ${task.querySpecies ?? ''}`,
    }));
  }, [tasks]);

  const allSelected = taskOptions.length > 0 && selectedTaskIds.length === taskOptions.length;

  const handleToggleTask = (id: string | number) => {
    setSelectedTaskIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  const handleSelectAll = () => {
    if (allSelected) {
      setSelectedTaskIds([]);
    } else {
      setSelectedTaskIds(taskOptions.map((o) => o.id));
    }
  };

  const handleExport = async () => {
    if (selectedTaskIds.length === 0) return;

    try {
      const numericIds = selectedTaskIds.map((id) => Number(id));
      const blob = await exportMutation.mutateAsync(numericIds);

      const today = new Date().toISOString().slice(0, 10);
      const filename = `swipelab_classifications_export_${today}.csv`;
      await downloadCsvBlob(blob, filename);

      Alert.alert('Success', `Exported ${selectedTaskIds.length} task(s) to CSV.`);
      setSelectedTaskIds([]);
      onClose();
    } catch (err: any) {
      Alert.alert('Export Failed', err?.message ?? 'Something went wrong. Please try again.');
    }
  };

  const handleClose = () => {
    if (!exportMutation.isPending) {
      setSelectedTaskIds([]);
      onClose();
    }
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      onRequestClose={handleClose}
    >
      <View style={styles.overlay}>
        <View
          style={[
            styles.container,
            {
              backgroundColor: isDark ? themeColors.card : '#FFFFFF',
              borderColor: isDark ? themeColors.border : '#DBEAFE',
            },
          ]}
        >
          {/* Header */}
          <View style={styles.header}>
            <View style={{ flex: 1 }}>
              <Text style={[styles.title, { color: themeColors.text }]}>
                📥 Export Classifications to CSV
              </Text>
              <Text style={[styles.subtitle, { color: themeColors.textSecondary }]}>
                Select tasks to include in the export. The CSV will contain all
                classification metadata from the selected tasks.
              </Text>
            </View>
            <TouchableOpacity
              onPress={handleClose}
              disabled={exportMutation.isPending}
              style={styles.closeButton}
            >
              <Ionicons name="close" size={24} color={themeColors.textSecondary} />
            </TouchableOpacity>
          </View>

          {/* Select All toggle */}
          <TouchableOpacity
            style={[
              styles.selectAllButton,
              {
                backgroundColor: allSelected
                  ? isDark ? 'rgba(59,130,246,0.15)' : '#DBEAFE'
                  : isDark ? themeColors.background : '#F0F7FF',
                borderColor: allSelected ? '#3B82F6' : isDark ? themeColors.border : '#BFDBFE',
              },
            ]}
            onPress={handleSelectAll}
            disabled={tasksLoading || taskOptions.length === 0}
          >
            <Ionicons
              name={allSelected ? 'checkbox' : 'square-outline'}
              size={20}
              color={allSelected ? '#3B82F6' : themeColors.textSecondary}
            />
            <Text
              style={[
                styles.selectAllText,
                { color: allSelected ? '#3B82F6' : themeColors.text },
              ]}
            >
              Select All ({taskOptions.length} tasks)
            </Text>
          </TouchableOpacity>

          {/* Task selection */}
          <ScrollView style={styles.scrollArea} nestedScrollEnabled>
            {tasksLoading ? (
              <View style={styles.centered}>
                <ActivityIndicator size="large" color="#3B82F6" />
                <Text style={[styles.loadingText, { color: themeColors.textSecondary }]}>
                  Loading tasks…
                </Text>
              </View>
            ) : taskOptions.length === 0 ? (
              <View style={styles.centered}>
                <Ionicons name="clipboard-outline" size={40} color="#BFDBFE" />
                <Text style={[styles.loadingText, { color: themeColors.textSecondary }]}>
                  No tasks available to export
                </Text>
              </View>
            ) : (
              <MultiSelect
                options={taskOptions}
                selectedIds={selectedTaskIds}
                onToggle={handleToggleTask}
                placeholder="Search tasks..."
              />
            )}
          </ScrollView>

          {/* Bottom bar */}
          <View
            style={[
              styles.bottomBar,
              { borderTopColor: isDark ? themeColors.border : '#DBEAFE' },
            ]}
          >
            <Text style={[styles.selectedCount, { color: themeColors.textSecondary }]}>
              {selectedTaskIds.length} task{selectedTaskIds.length !== 1 ? 's' : ''} selected
            </Text>
            <View style={styles.bottomButtons}>
              <TouchableOpacity
                style={[styles.cancelButton, { borderColor: isDark ? themeColors.border : '#BFDBFE' }]}
                onPress={handleClose}
                disabled={exportMutation.isPending}
              >
                <Text style={[styles.cancelText, { color: themeColors.textSecondary }]}>
                  Cancel
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.exportButton,
                  selectedTaskIds.length === 0 && styles.exportButtonDisabled,
                ]}
                onPress={handleExport}
                disabled={selectedTaskIds.length === 0 || exportMutation.isPending}
              >
                {exportMutation.isPending ? (
                  <ActivityIndicator size="small" color="#FFFFFF" />
                ) : (
                  <>
                    <Ionicons name="download-outline" size={18} color="#FFFFFF" />
                    <Text style={styles.exportText}>Export</Text>
                  </>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  container: {
    width: '100%',
    maxWidth: 520,
    maxHeight: '85%',
    borderRadius: 16,
    borderWidth: 1,
    overflow: 'hidden',
    ...Platform.select({
      web: { boxShadow: '0 8px 32px rgba(0,0,0,0.15)' },
      default: { elevation: 10 },
    }),
  },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: 20,
    paddingBottom: 12,
    gap: 12,
  },
  title: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 6,
  },
  subtitle: {
    fontSize: 13,
    lineHeight: 18,
  },
  closeButton: {
    padding: 4,
    marginTop: -2,
  },
  selectAllButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: 20,
    marginBottom: 8,
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 10,
    borderWidth: 1,
  },
  selectAllText: {
    fontSize: 14,
    fontWeight: '600',
  },
  scrollArea: {
    flex: 1,
    paddingHorizontal: 20,
    minHeight: 150,
  },
  centered: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
    gap: 10,
  },
  loadingText: {
    fontSize: 14,
    textAlign: 'center',
  },
  bottomBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderTopWidth: 1,
  },
  selectedCount: {
    fontSize: 13,
    fontWeight: '600',
  },
  bottomButtons: {
    flexDirection: 'row',
    gap: 10,
  },
  cancelButton: {
    paddingVertical: 10,
    paddingHorizontal: 18,
    borderRadius: 10,
    borderWidth: 1,
  },
  cancelText: {
    fontSize: 14,
    fontWeight: '600',
  },
  exportButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 10,
    backgroundColor: '#3B82F6',
  },
  exportButtonDisabled: {
    opacity: 0.4,
  },
  exportText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
  },
});
