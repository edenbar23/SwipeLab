import React from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { StepConfirmProps } from './addTaskTypes';

export default function StepConfirm({ formData, onBack, onSubmit, loading, availableOptions }: StepConfirmProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  // Resolve recipient labels
  const recipientLabels = formData.selectedRecipients.map((id) => {
    const opt = availableOptions.find((o) => o.id === id);
    return opt ? opt.label : id;
  });

  return (
    <View style={styles.container}>
      <Text style={[styles.heading, { color: themeColors.text }]}>Review & Confirm</Text>
      <Text style={[styles.subtitle, { color: themeColors.textSecondary }]}>
        Double-check all the details before creating your task.
      </Text>

      <ScrollView style={styles.reviewList} showsVerticalScrollIndicator={false}>
        {/* Name */}
        <View style={[styles.reviewCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
          <Text style={[styles.reviewLabel, { color: themeColors.textSecondary }]}>Task Name</Text>
          <Text style={[styles.reviewValue, { color: themeColors.text }]}>{formData.name}</Text>
        </View>

        {/* Description */}
        <View style={[styles.reviewCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
          <Text style={[styles.reviewLabel, { color: themeColors.textSecondary }]}>Description</Text>
          <Text style={[styles.reviewValue, { color: themeColors.text }]}>{formData.description}</Text>
        </View>

        {/* Species */}
        <View style={[styles.reviewCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
          <Text style={[styles.reviewLabel, { color: themeColors.textSecondary }]}>Species ({formData.speciesList.length})</Text>
          <View style={styles.tagsRow}>
            {formData.speciesList.map((s, i) => (
              <View key={i} style={styles.tag}>
                <Text style={styles.tagText}>{s}</Text>
              </View>
            ))}
          </View>
        </View>

        {/* Recipients */}
        <View style={[styles.reviewCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
          <Text style={[styles.reviewLabel, { color: themeColors.textSecondary }]}>Visibility</Text>
          {formData.isPublic ? (
            <Text style={[styles.reviewValue, { color: themeColors.text }]}>🌍 Public — All users</Text>
          ) : (
            <View>
              <Text style={[styles.reviewValue, { color: themeColors.text, marginBottom: 8 }]}>🔒 Restricted</Text>
              {recipientLabels.length > 0 ? (
                <View style={styles.tagsRow}>
                  {recipientLabels.map((label, i) => (
                    <View key={i} style={[styles.tag, styles.recipientTag]}>
                      <Text style={styles.recipientTagText}>{label}</Text>
                    </View>
                  ))}
                </View>
              ) : (
                <Text style={[styles.noRecipients, { color: themeColors.textSecondary }]}>No recipients selected</Text>
              )}
            </View>
          )}
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <View style={styles.buttonRow}>
          <TouchableOpacity style={[styles.backButton, { borderColor: themeColors.border }]} onPress={onBack}>
            <Text style={[styles.backButtonText, { color: themeColors.text }]}>← Back</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.submitButton, loading && styles.buttonDisabled]}
            onPress={onSubmit}
            disabled={loading}
          >
            <Text style={styles.submitButtonText}>{loading ? 'Creating...' : '✓ Create Task'}</Text>
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
  reviewList: { flex: 1 },
  reviewCard: {
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 12,
  },
  reviewLabel: { fontSize: 12, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 6 },
  reviewValue: { fontSize: 16, fontWeight: '500', lineHeight: 22 },
  tagsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  tag: {
    backgroundColor: '#10B981',
    paddingVertical: 6,
    paddingHorizontal: 14,
    borderRadius: 20,
  },
  tagText: { color: '#fff', fontWeight: '600', fontSize: 13 },
  recipientTag: {
    backgroundColor: 'rgba(16, 185, 129, 0.12)',
  },
  recipientTagText: { color: '#10B981', fontWeight: '600', fontSize: 13 },
  noRecipients: { fontSize: 14, fontStyle: 'italic' },
  footer: { paddingTop: 24 },
  buttonRow: { flexDirection: 'row', gap: 12 },
  backButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
    borderWidth: 1,
  },
  backButtonText: { fontWeight: '700', fontSize: 16 },
  submitButton: {
    flex: 1,
    backgroundColor: '#10B981',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  buttonDisabled: { backgroundColor: '#94D3B3' },
  submitButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
