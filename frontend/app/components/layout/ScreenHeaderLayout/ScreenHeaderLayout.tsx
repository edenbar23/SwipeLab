// components/layout/ScreenHeaderLayout.tsx
import React from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { ScreenHeaderLayoutProps } from "./ScreenHeaderLayout.types";
import { useThemeStore } from '../../../stores/themeStore';
import { Colors } from '../../../../constants/theme';

export default function ScreenHeaderLayout({
  leftIcon,
  leftTitle,
  onLeftPress,
  rightIcon,
  rightTitle,
  onRightPress,
  centerIcon,
  centerTitle,
  onCenterPress,
  children,
  contentContainerStyle,
}: ScreenHeaderLayoutProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  return (
    <View style={[styles.container, { backgroundColor: themeColors.background }]}>
      {/* Header */}
      <View style={[styles.header, { borderColor: themeColors.border, backgroundColor: themeColors.card }]}>
        {/* Left (current screen) */}
        <TouchableOpacity 
          style={styles.leftHeaderItem} 
          onPress={onLeftPress} 
          disabled={!onLeftPress}
        >
          {React.isValidElement(leftIcon) ? leftIcon : <Image source={leftIcon as any} style={styles.icon} />}
          <Text style={[styles.title, { color: themeColors.text }]} numberOfLines={1}>{leftTitle}</Text>
        </TouchableOpacity>

        {/* Center (optional navigation action) */}
        {centerTitle && (
          <View style={styles.centerWrapper}>
            <TouchableOpacity
              style={styles.centerHeaderItem}
              onPress={onCenterPress}
              disabled={!onCenterPress}
            >
              {centerIcon && (
                React.isValidElement(centerIcon) ? centerIcon : <Image source={centerIcon as any} style={styles.icon} />
              )}
              <Text style={[styles.button, { color: themeColors.text }]}>{centerTitle}</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Right (navigation action) */}
        <View style={styles.rightHeaderItem}>
          <TouchableOpacity
            style={styles.rightContentWrapper}
            onPress={onRightPress}
            disabled={!onRightPress}
          >
            {React.isValidElement(rightIcon) ? rightIcon : <Image source={rightIcon as any} style={styles.icon} />}
            <Text style={[styles.button, { color: themeColors.text }]} numberOfLines={1}>{rightTitle}</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Screen Content */}
      <View style={[styles.content, contentContainerStyle]}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    padding: 12,
    borderBottomWidth: 1,
    borderColor: "#E0E0E0",
    position: 'relative', // Ensure absolute child is relative to header
  },
  leftHeaderItem: {
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
    flex: 1,
    zIndex: 1,
    pointerEvents: 'box-none',
  },
  centerWrapper: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    alignItems: "center",
    justifyContent: "center",
    zIndex: 0,
    pointerEvents: 'box-none',
  },
  centerHeaderItem: {
    alignItems: "center",
    justifyContent: "center",
  },
  rightHeaderItem: {
    flex: 1,
    justifyContent: "flex-end",
    alignItems: "flex-end", // Align wrapper to the right
    zIndex: 1,
    pointerEvents: 'box-none', // Allow touches to pass through empty space
  },
  rightContentWrapper: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  icon: {
    width: 28,
    height: 28,
    resizeMode: "contain",
  },
  title: {
    fontSize: 18,
    fontWeight: "600",
    flex: 1,
  },
  button: {
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
  },
  content: {
    flex: 1,
    padding: 12,
  },
});
