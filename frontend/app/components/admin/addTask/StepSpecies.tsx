import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { StepSpeciesProps } from './addTaskTypes';
import MultiSelect from '../../../components/ui/MultiSelect';

export default function StepSpecies({ formData, onUpdate, onNext, onBack, availableSpecies = [], optionsLoading = false }: StepSpeciesProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const canProceed = formData.speciesList.length > 0;

  return (
    <View style={styles.container}>
      <Text style={[styles.heading, { color: themeColors.text }]}>Choose species to label</Text>
      <Text style={[styles.subtitle, { color: themeColors.textSecondary }]}>
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

      <View style={styles.footer}>
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
  heading: { fontSize: 22, fontWeight: '700', marginBottom: 8 },
  subtitle: { fontSize: 14, marginBottom: 24, lineHeight: 20 },
  footer: { paddingTop: 24, flex: 1, justifyContent: 'flex-end' },
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
