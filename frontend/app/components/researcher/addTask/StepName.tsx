import React from 'react';
import { StyleSheet, Text, TextInput, TouchableOpacity, View, Platform, ScrollView } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { StepProps } from './addTaskTypes';

export default function StepName({ formData, onUpdate, onNext }: StepProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const trimmedName = formData.name.trim();
  const isValidLength = trimmedName.length >= 3 && trimmedName.length <= 100;
  const showWarning = formData.name.length > 0 && !isValidLength;
  const canProceed = isValidLength;
  const isWeb = Platform.OS === 'web';

  return (
      <ScrollView contentContainerStyle={[styles.container, isWeb && styles.containerWeb]} keyboardShouldPersistTaps="handled" bounces={false}>
      <Text style={[styles.heading, { color: themeColors.text }, isWeb && styles.headingWeb]}>What's the name of your task?</Text>
      <Text style={[styles.subtitle, { color: themeColors.textSecondary }, isWeb && styles.subtitleWeb]}>
        Choose a clear, descriptive name for the classification task.
      </Text>

      <TextInput
        style={[styles.input, { backgroundColor: themeColors.card, borderColor: showWarning ? '#EF4444' : themeColors.border, color: themeColors.text }]}
        value={formData.name}
        onChangeText={(text) => onUpdate({ name: text })}
        placeholder="e.g., Pollinator Identification Survey"
        placeholderTextColor={themeColors.textSecondary}
        autoFocus
      />
      <Text style={{ color: showWarning ? '#EF4444' : themeColors.textSecondary, marginTop: 8, fontSize: 13, fontWeight: '500' }}>
        Task name must be between 3 and 100 characters.
      </Text>

      <View style={[styles.footer, isWeb && styles.footerWeb]}>
        <TouchableOpacity
          style={[styles.nextButton, !canProceed && styles.buttonDisabled]}
          onPress={onNext}
          disabled={!canProceed}
        >
          <Text style={styles.nextButtonText}>Next →</Text>
        </TouchableOpacity>
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
  },
  footer: { marginTop: 'auto', paddingTop: 24 },
  footerWeb: { paddingTop: 16 },
  nextButton: {
    backgroundColor: '#10B981',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  buttonDisabled: { backgroundColor: '#94D3B3' },
  nextButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
