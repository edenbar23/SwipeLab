import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '../../stores/themeStore';

interface StepIndicatorProps {
  steps: string[];
  currentStep: number; // 0-indexed
  layout?: 'horizontal' | 'vertical';
}

const ACTIVE_GREEN = '#10B981';

export default function StepIndicator({ steps, currentStep, layout = 'horizontal' }: StepIndicatorProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  if (layout === 'vertical') {
    return (
      <View style={verticalStyles.container}>
        {steps.map((label, index) => {
          const isCompleted = index < currentStep;
          const isActive = index === currentStep;
          const isLast = index === steps.length - 1;

          return (
            <View key={index} style={verticalStyles.stepWrapper}>
              {/* Circle + Label row */}
              <View style={verticalStyles.stepRow}>
                <View
                  style={[
                    verticalStyles.circle,
                    isCompleted && verticalStyles.circleCompleted,
                    isActive && verticalStyles.circleActive,
                    !isCompleted && !isActive && { borderColor: themeColors.border },
                  ]}
                >
                  {isCompleted ? (
                    <Text style={verticalStyles.checkmark}>✓</Text>
                  ) : (
                    <Text
                      style={[
                        verticalStyles.circleText,
                        isActive && verticalStyles.circleTextActive,
                        !isActive && { color: themeColors.textSecondary },
                      ]}
                    >
                      {index + 1}
                    </Text>
                  )}
                </View>
                <Text
                  style={[
                    verticalStyles.label,
                    isActive && verticalStyles.labelActive,
                    isCompleted && verticalStyles.labelCompleted,
                    !isActive && !isCompleted && { color: themeColors.textSecondary },
                  ]}
                  numberOfLines={1}
                >
                  {label}
                </Text>
              </View>

              {/* Vertical connector */}
              {!isLast && (
                <View style={verticalStyles.connectorWrapper}>
                  <View
                    style={[
                      verticalStyles.connector,
                      isCompleted
                        ? verticalStyles.connectorCompleted
                        : { backgroundColor: themeColors.border },
                    ]}
                  />
                </View>
              )}
            </View>
          );
        })}
      </View>
    );
  }

  // Horizontal layout (default — unchanged)
  return (
    <View style={horizontalStyles.container}>
      {steps.map((label, index) => {
        const isCompleted = index < currentStep;
        const isActive = index === currentStep;
        const isLast = index === steps.length - 1;

        return (
          <View key={index} style={horizontalStyles.stepWrapper}>
            {/* Circle + Label */}
            <View style={horizontalStyles.stepItem}>
              <View
                style={[
                  horizontalStyles.circle,
                  isCompleted && horizontalStyles.circleCompleted,
                  isActive && horizontalStyles.circleActive,
                  !isCompleted && !isActive && { borderColor: themeColors.border },
                ]}
              >
                {isCompleted ? (
                  <Text style={horizontalStyles.checkmark}>✓</Text>
                ) : (
                  <Text
                    style={[
                      horizontalStyles.circleText,
                      isActive && horizontalStyles.circleTextActive,
                      !isActive && { color: themeColors.textSecondary },
                    ]}
                  >
                    {index + 1}
                  </Text>
                )}
              </View>
              <Text
                style={[
                  horizontalStyles.label,
                  isActive && horizontalStyles.labelActive,
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
                  horizontalStyles.connector,
                  isCompleted
                    ? horizontalStyles.connectorCompleted
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

const verticalStyles = StyleSheet.create({
  container: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  stepWrapper: {},
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 4,
  },
  circle: {
    width: 30,
    height: 30,
    borderRadius: 15,
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
    fontSize: 13,
    fontWeight: '700',
    color: '#6b7280',
  },
  circleTextActive: {
    color: '#fff',
  },
  checkmark: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '700',
  },
  label: {
    fontSize: 13,
    fontWeight: '500',
    color: '#6b7280',
  },
  labelActive: {
    color: ACTIVE_GREEN,
    fontWeight: '700',
  },
  labelCompleted: {
    color: ACTIVE_GREEN,
    fontWeight: '600',
  },
  connectorWrapper: {
    paddingLeft: 14, // center under the circle (30/2 - 1)
    height: 16,
  },
  connector: {
    width: 2,
    flex: 1,
    backgroundColor: '#e5e7eb',
  },
  connectorCompleted: {
    backgroundColor: ACTIVE_GREEN,
  },
});

const horizontalStyles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'center',
    alignSelf: 'center',
    width: '100%',
    maxWidth: 600,
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
    width: 76,
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
    marginTop: 15,
    marginHorizontal: -4,
  },
  connectorCompleted: {
    backgroundColor: ACTIVE_GREEN,
  },
});
