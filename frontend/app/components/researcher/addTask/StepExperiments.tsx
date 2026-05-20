import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View, ScrollView, Platform } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import MultiSelect from '../../ui/MultiSelect';
import { StepExperimentsProps } from './addTaskTypes';

export default function StepExperiments({
  formData,
  onUpdate,
  onNext,
  onBack,
  availableExperiments,
  optionsLoading,
}: StepExperimentsProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isWeb = Platform.OS === 'web';

  return (
    <View style={[styles.container, isWeb && styles.containerWeb]}>
      <ScrollView style={{ flex: 1 }} contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <Text style={[styles.heading, { color: themeColors.text }, isWeb && styles.headingWeb]}>Select Experiments</Text>
        <Text style={[styles.subtitle, { color: themeColors.textSecondary }, isWeb && styles.subtitleWeb]}>
          Choose one or more experiments that this task will classify images for.
        </Text>

      <View style={styles.recipientsSection}>
        <Text style={[styles.sectionLabel, { color: themeColors.text }]}>Experiments</Text>
        <MultiSelect
          options={availableExperiments}
          selectedIds={formData.selectedExperiments || []}
          onToggle={(id) => {
            const prev = formData.selectedExperiments || [];
            const updated = prev.includes(id as string)
              ? prev.filter((eid) => eid !== id)
              : [...prev, id as string];
            onUpdate({ selectedExperiments: updated });
          }}
          placeholder="Search experiments..."
          loading={optionsLoading}
        />
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
  recipientsSection: { marginTop: 4 },
  sectionLabel: { fontWeight: '600', marginBottom: 8, fontSize: 15 },
  footer: { paddingTop: 16 },
  footerWeb: { paddingTop: 12 },
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
