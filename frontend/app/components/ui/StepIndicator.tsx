import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '../../stores/themeStore';

interface StepIndicatorProps {
  steps: string[];
  currentStep: number; // 0-indexed
}

export default function StepIndicator({ steps, currentStep }: StepIndicatorProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  return (
    <View style={styles.container}>
      {steps.map((label, index) => {
        const isCompleted = index < currentStep;
        const isActive = index === currentStep;
        const isLast = index === steps.length - 1;

        return (
          <View key={index} style={styles.stepWrapper}>
            {/* Circle + Label */}
            <View style={styles.stepItem}>
              <View
                style={[
                  styles.circle,
                  isCompleted && styles.circleCompleted,
                  isActive && styles.circleActive,
                  !isCompleted && !isActive && { borderColor: themeColors.border },
                ]}
              >
                {isCompleted ? (
                  <Text style={styles.checkmark}>✓</Text>
                ) : (
                  <Text
                    style={[
                      styles.circleText,
                      isActive && styles.circleTextActive,
                      !isActive && { color: themeColors.textSecondary },
                    ]}
                  >
                    {index + 1}
                  </Text>
                )}
              </View>
              <Text
                style={[
                  styles.label,
                  isActive && styles.labelActive,
                  !isActive && { color: themeColors.textSecondary },
                ]}
              >
                {label}
              </Text>
            </View>

            {/* Connector line */}
            {!isLast && (
              <View
                style={[
                  styles.connector,
                  isCompleted
                    ? styles.connectorCompleted
                    : { backgroundColor: themeColors.border },
                ]}
              />
            )}
          </View>
        );
      })}
    </View>
  );
}

const ACTIVE_GREEN = '#10B981';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'center',
    alignSelf: 'center', // Centers the whole bar
    width: '100%',
    maxWidth: 600, // Prevents it from stretching infinitely on wide screens
    paddingVertical: 16,
    paddingHorizontal: 8,
  },
  stepWrapper: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  stepItem: {
    alignItems: 'center',
    width: 76, // Increased to allow "Description" and "Recipients" to fit without wrapping
  },
  circle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 2,
    borderColor: '#e5e7eb',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'transparent',
  },
  circleActive: {
    borderColor: ACTIVE_GREEN,
    backgroundColor: ACTIVE_GREEN,
  },
  circleCompleted: {
    borderColor: ACTIVE_GREEN,
    backgroundColor: ACTIVE_GREEN,
  },
  circleText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#6b7280',
  },
  circleTextActive: {
    color: '#fff',
  },
  checkmark: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  label: {
    fontSize: 10,
    fontWeight: '500',
    marginTop: 4,
    textAlign: 'center',
    color: '#6b7280',
    width: 76,
  },
  labelActive: {
    color: ACTIVE_GREEN,
    fontWeight: '700',
  },
  connector: {
    flex: 1,
    height: 2,
    backgroundColor: '#e5e7eb',
    marginTop: 15, // vertically center with circle
    marginHorizontal: -4,
  },
  connectorCompleted: {
    backgroundColor: ACTIVE_GREEN,
  },
});
