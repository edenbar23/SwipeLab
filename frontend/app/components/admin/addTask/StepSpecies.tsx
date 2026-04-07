import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { StepProps } from './addTaskTypes';

export default function StepSpecies({ formData, onUpdate, onNext, onBack }: StepProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const [speciesInput, setSpeciesInput] = useState('');
  const canProceed = formData.speciesList.length > 0;

  const addSpecies = () => {
    const trimmed = speciesInput.trim();
    if (trimmed && !formData.speciesList.includes(trimmed)) {
      onUpdate({ speciesList: [...formData.speciesList, trimmed] });
      setSpeciesInput('');
    }
  };

  const removeSpecies = (species: string) => {
    onUpdate({ speciesList: formData.speciesList.filter((s) => s !== species) });
  };

  return (
    <View style={styles.container}>
      <Text style={[styles.heading, { color: themeColors.text }]}>Choose species to label</Text>
      <Text style={[styles.subtitle, { color: themeColors.textSecondary }]}>
        Add the species that classifiers will identify in images.
      </Text>

      {/* Add species input */}
      <View style={styles.addRow}>
        <TextInput
          style={[styles.input, { flex: 1, backgroundColor: themeColors.card, borderColor: themeColors.border, color: themeColors.text }]}
          value={speciesInput}
          onChangeText={setSpeciesInput}
          placeholder="e.g., Asian Giant Hornet"
          placeholderTextColor={themeColors.textSecondary}
          onSubmitEditing={addSpecies}
        />
        <TouchableOpacity
          style={[styles.addButton, !speciesInput.trim() && styles.addButtonDisabled]}
          onPress={addSpecies}
          disabled={!speciesInput.trim()}
        >
          <Text style={styles.addButtonText}>+ Add</Text>
        </TouchableOpacity>
      </View>

      {/* Species list */}
      <ScrollView style={styles.speciesList} showsVerticalScrollIndicator={false}>
        {formData.speciesList.map((species, index) => (
          <View key={index} style={[styles.speciesCard, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            <View style={styles.speciesInfo}>
              <Text style={[styles.speciesName, { color: themeColors.text }]}>{species}</Text>
              <TouchableOpacity style={styles.photoButton}>
                <Text style={styles.photoButtonText}>📷 Add Photo</Text>
              </TouchableOpacity>
            </View>
            <TouchableOpacity style={styles.removeButton} onPress={() => removeSpecies(species)}>
              <Text style={styles.removeText}>✕</Text>
            </TouchableOpacity>
          </View>
        ))}
        {formData.speciesList.length === 0 && (
          <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>
            No species added yet. Type a name above and press "Add".
          </Text>
        )}
      </ScrollView>

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
  addRow: { flexDirection: 'row', gap: 10, marginBottom: 16 },
  input: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    fontSize: 16,
  },
  addButton: {
    backgroundColor: '#10B981',
    paddingHorizontal: 20,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addButtonDisabled: { backgroundColor: '#94D3B3' },
  addButtonText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  speciesList: { flex: 1 },
  speciesCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 10,
  },
  speciesInfo: { flex: 1 },
  speciesName: { fontSize: 16, fontWeight: '600', marginBottom: 6 },
  photoButton: {
    alignSelf: 'flex-start',
    paddingVertical: 4,
    paddingHorizontal: 10,
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
    borderRadius: 8,
  },
  photoButtonText: { color: '#10B981', fontSize: 13, fontWeight: '600' },
  removeButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 10,
  },
  removeText: { color: '#EF4444', fontSize: 14, fontWeight: '700' },
  emptyText: { textAlign: 'center', marginTop: 24, fontSize: 14 },
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
