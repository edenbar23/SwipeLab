/**
 * WebAppShell — Web-only centering wrapper.
 *
 * On mobile (iOS/Android) this renders children directly with no overhead.
 * On web it constrains the app to a readable max-width, centers it, and
 * applies a subtle gutter background so the canvas doesn't look bare.
 *
 * maxWidth prop controls the container width:
 *   - "user"  → 840px  (portrait-ish, game feel)
 *   - "researcher" → 1200px (dashboard, information-dense)
 */

import React from 'react';
import { Platform, StyleSheet, View, ViewStyle } from 'react-native';
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';

interface Props {
  children: React.ReactNode;
  variant?: 'user' | 'researcher';
  style?: ViewStyle;
}

const MAX_WIDTH: Record<'user' | 'researcher', number> = {
  user: 840,
  researcher: 1200,
};

export default function WebAppShell({ children, variant = 'user', style }: Props) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  if (Platform.OS !== 'web') {
    // Mobile: zero-cost passthrough
    return <View style={[{ flex: 1 }, style]}>{children}</View>;
  }

  const maxWidth = MAX_WIDTH[variant];

  // Gutter background differs subtly from the app background to signal the boundary
  const gutterBg = theme === 'dark' ? '#111120' : '#e8eaf0';

  return (
    <View style={[styles.gutter, { backgroundColor: gutterBg }]}>
      <View
        style={[
          styles.shell,
          {
            maxWidth,
            backgroundColor: themeColors.background,
            borderLeftWidth: 1,
            borderRightWidth: 1,
            borderColor: themeColors.border,
          },
          style,
        ]}
      >
        {children}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  gutter: {
    flex: 1,
    // Do NOT use alignItems: 'center' here — it collapses child height to
    // 'auto' in React Native web, breaking all inner flex/scroll chains.
  },
  shell: {
    flex: 1,
    width: '100%',
    // marginHorizontal auto centers the shell horizontally on web
    // without affecting its height (unlike alignItems: 'center' on parent).
    marginHorizontal: 'auto' as any,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
  },
});
