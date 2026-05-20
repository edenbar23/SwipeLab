import React from 'react';
import { Platform, View, StyleSheet, ViewStyle } from 'react-native';

interface ResponsiveContainerProps {
  children: React.ReactNode;
  style?: ViewStyle | ViewStyle[];
  maxWidth?: number;
}

/**
 * A container that restricts the maximum width of its content on the web platform,
 * centering it on the screen. On mobile, it takes up 100% of the width.
 * This replaces global App.tsx constraints to allow for full-width web screens if needed.
 */
export default function ResponsiveContainer({ children, style, maxWidth = 1024 }: ResponsiveContainerProps) {
  if (Platform.OS === 'web') {
    return (
      <View style={[styles.webWrapper, style]}>
        <View style={[styles.webInner, { maxWidth }]}>
          {children}
        </View>
      </View>
    );
  }

  // On mobile, just render children directly (or inside a standard flex: 1 view)
  return <View style={[{ flex: 1 }, style]}>{children}</View>;
}

const styles = StyleSheet.create({
  webWrapper: {
    flex: 1,
    width: '100%',
    backgroundColor: 'transparent',
  },
  webInner: {
    flex: 1,
    width: '100%',
    alignSelf: 'center',
    // Optional shadow for web to distinguish the app from background if needed
    boxShadow: '0px 0px 20px rgba(0, 0, 0, 0.05)',
  }
});
