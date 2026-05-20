import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View, ScrollView, Platform } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import MultiSelect from '../../ui/MultiSelect';
import { StepRecipientsProps } from './addTaskTypes';

export default function StepRecipients({
  formData, onUpdate, onNext, onBack,
  availableOptions, availableResearchers, optionsLoading,
}: StepRecipientsProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isWeb = Platform.OS === 'web';

  return (
    <View style={[styles.container, isWeb && styles.containerWeb]}>
      <ScrollView style={{ flex: 1 }} contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <Text style={[styles.heading, { color: themeColors.text }, isWeb && styles.headingWeb]}>Who should see this task?</Text>
        <Text style={[styles.subtitle, { color: themeColors.textSecondary }, isWeb && styles.subtitleWeb]}>
          Make the task public for all users, or restrict it to specific groups and users.
        </Text>
        <View style={styles.toggleRow}>
          <TouchableOpacity style={[styles.toggleBtn, { borderColor: themeColors.border }, formData.isPublic && styles.toggleActive]} onPress={() => onUpdate({ isPublic: true })}>
            <Text style={styles.toggleEmoji}>🌍</Text>
            <Text style={[styles.toggleText, formData.isPublic && styles.toggleTextActive]}>Public (All)</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.toggleBtn, { borderColor: themeColors.border }, !formData.isPublic && styles.toggleActive]} onPress={() => onUpdate({ isPublic: false })}>
            <Text style={styles.toggleEmoji}>🔒</Text>
            <Text style={[styles.toggleText, !formData.isPublic && styles.toggleTextActive]}>Restricted</Text>
          </TouchableOpacity>
        </View>
        {!formData.isPublic && (
          <View style={styles.recipientsSection}>
            <Text style={[styles.sectionLabel, { color: themeColors.text }]}>Assign Recipients</Text>
            <MultiSelect options={availableOptions} selectedIds={formData.selectedRecipients}
              onToggle={(id) => { const prev = formData.selectedRecipients; const updated = prev.includes(id as string) ? prev.filter((gid) => gid !== id) : [...prev, id as string]; onUpdate({ selectedRecipients: updated }); }}
              placeholder="Search users or groups..." loading={optionsLoading} />
          </View>
        )}
        <View style={[styles.recipientsSection, { marginTop: 20 }]}>
          <Text style={[styles.sectionLabel, { color: themeColors.text }]}>Share with Co-Managers</Text>
          <Text style={[styles.subtitle, { color: themeColors.textSecondary, marginBottom: 8 }]}>Select other researchers who can view and manage this task.</Text>
          <MultiSelect options={availableResearchers} selectedIds={formData.sharedWithResearchers}
            onToggle={(id) => { const prev = formData.sharedWithResearchers; const updated = prev.includes(id as string) ? prev.filter((rId) => rId !== id) : [...prev, id as string]; onUpdate({ sharedWithResearchers: updated }); }}
            placeholder="Search researchers..." loading={optionsLoading} />
        </View>
      </ScrollView>
      <View style={[styles.footer, isWeb && styles.footerWeb]}>
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
  containerWeb: { paddingVertical: 8 },
  scrollContent: { paddingBottom: 16 },
  heading: { fontSize: 22, fontWeight: '700', marginBottom: 8 },
  headingWeb: { fontSize: 20, marginBottom: 6 },
  subtitle: { fontSize: 14, marginBottom: 24, lineHeight: 20 },
  subtitleWeb: { marginBottom: 16 },
  toggleRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
  toggleBtn: { flex: 1, paddingVertical: 14, backgroundColor: '#f3f4f6', alignItems: 'center', borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb' },
  toggleActive: { backgroundColor: '#d1fae5', borderColor: '#10B981' },
  toggleEmoji: { fontSize: 22, marginBottom: 4 },
  toggleText: { fontWeight: '600', color: '#6b7280', fontSize: 14 },
  toggleTextActive: { color: '#065f46' },
  recipientsSection: { marginTop: 4 },
  sectionLabel: { fontWeight: '600', marginBottom: 8, fontSize: 15 },
  footer: { paddingTop: 16 },
  footerWeb: { paddingTop: 12 },
  buttonRow: { flexDirection: 'row', gap: 12 },
  backButton: { flex: 1, paddingVertical: 14, borderRadius: 12, alignItems: 'center', borderWidth: 1 },
  backButtonText: { fontWeight: '700', fontSize: 16 },
  nextButton: { flex: 1, backgroundColor: '#10B981', paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
  nextButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
