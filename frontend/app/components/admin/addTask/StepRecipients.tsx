import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import MultiSelect from '../../ui/MultiSelect';
import { StepRecipientsProps } from './addTaskTypes';

export default function StepRecipients({
  formData,
  onUpdate,
  onNext,
  onBack,
  availableOptions,
  optionsLoading,
}: StepRecipientsProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  return (
    <View style={styles.container}>
      <Text style={[styles.heading, { color: themeColors.text }]}>Who should see this task?</Text>
      <Text style={[styles.subtitle, { color: themeColors.textSecondary }]}>
        Make the task public for all users, or restrict it to specific groups and users.
      </Text>

      {/* Public/Restricted toggle */}
      <View style={styles.toggleRow}>
        <TouchableOpacity
          style={[
            styles.toggleBtn,
            { borderColor: themeColors.border },
            formData.isPublic && styles.toggleActive,
          ]}
          onPress={() => onUpdate({ isPublic: true })}
        >
          <Text style={[styles.toggleEmoji]}>🌍</Text>
          <Text style={[styles.toggleText, formData.isPublic && styles.toggleTextActive]}>
            Public (All)
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.toggleBtn,
            { borderColor: themeColors.border },
            !formData.isPublic && styles.toggleActive,
          ]}
          onPress={() => onUpdate({ isPublic: false })}
        >
          <Text style={[styles.toggleEmoji]}>🔒</Text>
          <Text style={[styles.toggleText, !formData.isPublic && styles.toggleTextActive]}>
            Restricted
          </Text>
        </TouchableOpacity>
      </View>

      {/* Recipient selection (only when restricted) */}
      {!formData.isPublic && (
        <View style={styles.recipientsSection}>
          <Text style={[styles.sectionLabel, { color: themeColors.text }]}>Assign Recipients</Text>
          <MultiSelect
            options={availableOptions}
            selectedIds={formData.selectedRecipients}
            onToggle={(id) => {
              const prev = formData.selectedRecipients;
              const updated = prev.includes(id as string)
                ? prev.filter((gid) => gid !== id)
                : [...prev, id as string];
              onUpdate({ selectedRecipients: updated });
            }}
            placeholder="Search users or groups..."
            loading={optionsLoading}
          />
        </View>
      )}

      <View style={styles.footer}>
        <View style={styles.buttonRow}>
          <TouchableOpacity style={[styles.backButton, { borderColor: themeColors.border }]} onPress={onBack}>
            <Text style={[styles.backButtonText, { color: themeColors.text }]}>← Back</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.nextButton} onPress={onNext}>
            <Text style={styles.nextButtonText}>Next →</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingVertical: 16 },
  heading: { fontSize: 22, fontWeight: '700', marginBottom: 8 },
  subtitle: { fontSize: 14, marginBottom: 24, lineHeight: 20 },
  toggleRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  toggleBtn: {
    flex: 1,
    paddingVertical: 16,
    backgroundColor: '#f3f4f6',
    alignItems: 'center',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  toggleActive: {
    backgroundColor: '#d1fae5',
    borderColor: '#10B981',
  },
  toggleEmoji: { fontSize: 24, marginBottom: 6 },
  toggleText: { fontWeight: '600', color: '#6b7280', fontSize: 14 },
  toggleTextActive: { color: '#065f46' },
  recipientsSection: { marginTop: 4 },
  sectionLabel: { fontWeight: '600', marginBottom: 8, fontSize: 15 },
  footer: { marginTop: 'auto', paddingTop: 24 },
  buttonRow: { flexDirection: 'row', gap: 12 },
  backButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
    borderWidth: 1,
  },
  backButtonText: { fontWeight: '700', fontSize: 16 },
  nextButton: {
    flex: 1,
    backgroundColor: '#10B981',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  nextButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
