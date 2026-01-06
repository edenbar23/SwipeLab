// components/layout/ScreenHeaderLayout.tsx
import React from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { ScreenHeaderLayoutProps } from "./ScreenHeaderLayout.types";

export default function ScreenHeaderLayout({
  leftIcon,
  leftTitle,
  rightIcon,
  rightTitle,
  onRightPress,
  children,
}: ScreenHeaderLayoutProps) {
  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        {/* Left (current screen) */}
        <View style={styles.leftHeaderItem}>
          <Image source={leftIcon} style={styles.icon} />
          <Text style={styles.title} numberOfLines={1}>{leftTitle}</Text>
        </View>

        {/* Right (navigation action) */}
        <TouchableOpacity
          style={styles.rightHeaderItem}
          onPress={onRightPress}
          disabled={!onRightPress}
        >
          <Image source={rightIcon} style={styles.icon} />
          <Text style={styles.button} numberOfLines={1}>{rightTitle}</Text>
        </TouchableOpacity>
      </View>

      {/* Screen Content */}
      <View style={styles.content}>{children}</View>
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
    padding: 12,
    borderBottomWidth: 1,
    borderColor: "#E0E0E0",
  },
  leftHeaderItem: {
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
    flex: 1.5,
  },
  rightHeaderItem: {
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
    flex: 1,
    justifyContent: "flex-end",
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
  },
  content: {
    flex: 1,
    padding: 12,
  },
});
