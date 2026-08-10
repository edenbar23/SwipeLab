import React, { useEffect, useRef, useState } from 'react';
import { Animated, StyleSheet, Text, ActivityIndicator, Platform } from 'react-native';
import { useDownloadStore } from '@/stores/downloadStore';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '@/constants/theme';
import { useThemeStore } from '@/stores/themeStore';

export default function GlobalDownloadToast() {
  const activeExports = useDownloadStore((state) => state.activeExports);
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const [visible, setVisible] = useState(false);
  const [completed, setCompleted] = useState(false);
  const opacity = useRef(new Animated.Value(0)).current;
  const translateY = useRef(new Animated.Value(20)).current;

  // Keep track of previous length to detect completion
  const prevLengthRef = useRef(activeExports.length);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const currentLength = activeExports.length;
    
    if (currentLength > 0) {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      setVisible(true);
      setCompleted(false);
      
      Animated.parallel([
        Animated.timing(opacity, {
          toValue: 1,
          duration: 300,
          useNativeDriver: true,
        }),
        Animated.spring(translateY, {
          toValue: 0,
          useNativeDriver: true,
        })
      ]).start();
    } else if (currentLength === 0 && prevLengthRef.current > 0) {
      // Just finished
      setCompleted(true);
      
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => {
        Animated.parallel([
          Animated.timing(opacity, {
            toValue: 0,
            duration: 300,
            useNativeDriver: true,
          }),
          Animated.timing(translateY, {
            toValue: 20,
            duration: 300,
            useNativeDriver: true,
          })
        ]).start(() => {
          setVisible(false);
          setCompleted(false);
        });
      }, 3000);
    }
    
    prevLengthRef.current = currentLength;
  }, [activeExports.length, opacity, translateY]);

  if (!visible) return null;

  let text = '';
  if (completed) {
    text = 'Download Complete';
  } else {
    const names = activeExports.map(t => t.taskName);
    if (names.length === 1) {
      text = `Downloading: ${names[0]}`;
    } else if (names.length === 2) {
      text = `Downloading: ${names[0]}, ${names[1]}`;
    } else if (names.length > 2) {
      text = `Downloading: ${names[0]}, ${names[1]} +${names.length - 2} more`;
    }
  }

  return (
    <Animated.View
      style={[
        styles.container,
        {
          opacity,
          transform: [{ translateY }],
          backgroundColor: isDark ? themeColors.card : '#FFFFFF',
          borderColor: isDark ? themeColors.border : '#E5E7EB',
          ...Platform.select({
            web: { boxShadow: '0 4px 12px rgba(0,0,0,0.1)' },
            default: { elevation: 6, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 12 },
          })
        }
      ]}
    >
      {completed ? (
        <Ionicons name="checkmark-circle" size={20} color="#10B981" />
      ) : (
        <ActivityIndicator size="small" color="#3B82F6" />
      )}
      <Text style={[styles.text, { color: themeColors.text }]} numberOfLines={1}>
        {text}
      </Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 90, // Above bottom toolbar
    right: 20,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
    gap: 10,
    maxWidth: 300,
    zIndex: 9999,
  },
  text: {
    fontSize: 14,
    fontWeight: '500',
    flexShrink: 1,
  }
});
