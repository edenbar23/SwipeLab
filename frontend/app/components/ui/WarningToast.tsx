import React, { useEffect, useRef } from 'react';
import {
  Animated,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { ClassificationWarning } from '../../types/fraudTypes';

interface WarningToastProps {
  warning: ClassificationWarning;
  onDismiss: () => void;
}

/**
 * Non-blocking inline toast shown after a classification submission when the
 * fraud detection system has issued a warning to the current user.
 *
 * WARNING_1 → amber styling, 5 s auto-dismiss
 * WARNING_2 → red styling, 8 s auto-dismiss (more prominent = more reading time)
 */
export default function WarningToast({ warning, onDismiss }: WarningToastProps) {
  const opacity = useRef(new Animated.Value(0)).current;
  const translateY = useRef(new Animated.Value(-20)).current;
  const isFinal = warning.level === 'WARNING_2';

  const bgColor = isFinal ? '#7f1d1d' : '#78350f';
  const borderColor = isFinal ? '#ef4444' : '#f59e0b';
  const iconName: any = isFinal ? 'alert-circle' : 'warning';
  const autoDismissMs = isFinal ? 8000 : 5000;

  useEffect(() => {
    // Slide in
    Animated.parallel([
      Animated.timing(opacity, { toValue: 1, duration: 300, useNativeDriver: true }),
      Animated.timing(translateY, { toValue: 0, duration: 300, useNativeDriver: true }),
    ]).start();

    // Auto-dismiss
    const timer = setTimeout(() => dismiss(), autoDismissMs);
    return () => clearTimeout(timer);
  }, []);

  const dismiss = () => {
    Animated.parallel([
      Animated.timing(opacity, { toValue: 0, duration: 250, useNativeDriver: true }),
      Animated.timing(translateY, { toValue: -20, duration: 250, useNativeDriver: true }),
    ]).start(onDismiss);
  };

  return (
    <Animated.View
      style={[
        styles.container,
        { backgroundColor: bgColor, borderLeftColor: borderColor, opacity, transform: [{ translateY }] },
      ]}
      accessibilityRole="alert"
      accessibilityLabel={`Fraud warning: ${warning.message}`}
    >
      {/* Icon */}
      <Ionicons name={iconName} size={22} color={borderColor} style={styles.icon} />

      {/* Text block */}
      <View style={styles.textBlock}>
        <Text style={styles.title}>
          {isFinal ? '🚨 Final Warning' : '⚠️ Warning'}
        </Text>
        <Text style={styles.message}>{warning.message}</Text>
        <Text style={styles.subtitle}>
          {warning.strikesUntilBan > 0
            ? `${warning.strikesUntilBan} more violation${warning.strikesUntilBan > 1 ? 's' : ''} will result in an automatic ban.`
            : 'Next violation will result in an automatic ban.'}
        </Text>
      </View>

      {/* Dismiss */}
      <TouchableOpacity onPress={dismiss} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
        <Ionicons name="close" size={18} color="#fff9" />
      </TouchableOpacity>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 16,
    left: 16,
    right: 16,
    zIndex: 999,
    flexDirection: 'row',
    alignItems: 'flex-start',
    borderRadius: 12,
    borderLeftWidth: 4,
    padding: 14,
    shadowColor: '#000',
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 8,
  },
  icon: {
    marginRight: 10,
    marginTop: 2,
  },
  textBlock: {
    flex: 1,
  },
  title: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 14,
    marginBottom: 3,
  },
  message: {
    color: '#fef3c7',
    fontSize: 13,
    lineHeight: 18,
    marginBottom: 4,
  },
  subtitle: {
    color: '#fde68a',
    fontSize: 11,
    fontStyle: 'italic',
  },
});
