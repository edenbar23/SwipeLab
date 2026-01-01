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
        <View style={styles.headerItem}>
          <Image source={leftIcon} style={styles.icon} />
          <Text style={styles.title}>{leftTitle}</Text>
        </View>

        {/* Right (navigation action) */}
        <TouchableOpacity
          style={styles.headerItem}
          onPress={onRightPress}
          disabled={!onRightPress}
        >
          <Image source={rightIcon} style={styles.icon} />
          <Text style={styles.button}>{rightTitle}</Text>
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
  headerItem: {
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
  },
  icon: {
    width: 28,
    height: 28,
    resizeMode: "contain",
  },
  title: {
    fontSize: 25,
    fontWeight: "600",
  },
  button: {
    fontSize: 16,
    fontWeight: "600",
  },
  content: {
    flex: 1,
    padding: 12,
  },
});
