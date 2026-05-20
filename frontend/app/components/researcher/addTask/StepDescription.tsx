import React from 'react';
import { StyleSheet, Text, TextInput, TouchableOpacity, View, Platform, ScrollView } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { StepProps } from './addTaskTypes';

export default function StepDescription({ formData, onUpdate, onNext, onBack }: StepProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const trimmedDesc = formData.description.trim();
  const isValidLength = trimmedDesc.length >= 10 && trimmedDesc.length <= 2000;
  const showWarning = formData.description.length > 0 && !isValidLength;
  const canProceed = isValidLength;
  const isWeb = Platform.OS === 'web';

  return (
      <ScrollView contentContainerStyle={[styles.container, isWeb && styles.containerWeb]} keyboardShouldPersistTaps="handled" bounces={false}>
      <Text style={[styles.heading, { color: themeColors.text }, isWeb && styles.headingWeb]}>Describe the task</Text>
      <Text style={[styles.subtitle, { color: themeColors.textSecondary }, isWeb && styles.subtitleWeb]}>
        Provide clear instructions so classifiers know exactly what to look for.
      </Text>

      <TextInput
        style={[styles.input, { backgroundColor: themeColors.card, borderColor: showWarning ? '#EF4444' : themeColors.border, color: themeColors.text }]}
        value={formData.description}
        onChangeText={(text) => onUpdate({ description: text })}
        placeholder="e.g., Identify whether each image contains the target pollinator species..."
        placeholderTextColor={themeColors.textSecondary}
        multiline
        numberOfLines={5}
        textAlignVertical="top"
      />
      <Text style={{ color: showWarning ? '#EF4444' : themeColors.textSecondary, marginTop: 8, fontSize: 13, fontWeight: '500' }}>
        Description must be between 10 and 2000 characters.
      </Text>

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
      </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flexGrow: 1, paddingVertical: 16 },
  containerWeb: { paddingVertical: 8 },
  heading: { fontSize: 22, fontWeight: '700', marginBottom: 8 },
  headingWeb: { fontSize: 20, marginBottom: 6 },
  subtitle: { fontSize: 14, marginBottom: 24, lineHeight: 20 },
  subtitleWeb: { marginBottom: 16 },
  input: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    fontSize: 16,
    minHeight: 120,
  },
  footer: { marginTop: 'auto', paddingTop: 24 },
  footerWeb: { paddingTop: 16 },
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
