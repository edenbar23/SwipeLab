import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View, ScrollView, Platform } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { StepSpeciesProps } from './addTaskTypes';
import MultiSelect from '../../../components/ui/MultiSelect';

export default function StepSpecies({ formData, onUpdate, onNext, onBack, availableSpecies = [], optionsLoading = false }: StepSpeciesProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const canProceed = formData.speciesList.length > 0;
  const isWeb = Platform.OS === 'web';

  return (
    <View style={[styles.container, isWeb && styles.containerWeb]}>
      <ScrollView style={{ flex: 1 }} contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <Text style={[styles.heading, { color: themeColors.text }, isWeb && styles.headingWeb]}>Choose species to label</Text>
        <Text style={[styles.subtitle, { color: themeColors.textSecondary }, isWeb && styles.subtitleWeb]}>
          Add the species that classifiers will identify in images.
        </Text>

      <MultiSelect
        options={availableSpecies}
        selectedIds={formData.speciesList}
        onToggle={(id) => {
          const newSpeciesList = formData.speciesList.includes(id as string)
            ? formData.speciesList.filter((s) => s !== id)
            : [...formData.speciesList, id as string];
          onUpdate({ speciesList: newSpeciesList });
        }}
        placeholder="Search species..."
        loading={optionsLoading}
        emptyOnNoSearch={true}
      />
      </ScrollView>

      <View style={[styles.footer, isWeb && styles.footerWeb]}>
        <View style={styles.buttonRow}>
          <TouchableOpacity style={[styles.backButton, { borderColor: themeColors.border }]} onPress={onBack}>
            <Text style={[styles.backButtonText, { color: themeColors.text }]}>← Back</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.nextButton, !canProceed && styles.buttonDisabled]}
            onPress={onNext}
            disabled={!canProceed}
          >
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
  buttonDisabled: { backgroundColor: '#94D3B3' },
  nextButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
