import { useNavigation } from "@react-navigation/native";
import React from "react";
import { Image, ImageSourcePropType, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useThemeStore } from "../../stores/themeStore";
import useResponsive from "../../hooks/useResponsive";
import { Colors } from "../../../constants/theme";

interface NavItem {
  label: string;
  route: string;
  icon: ImageSourcePropType;
}

interface Props {
  items: NavItem[];
}

export default function BottomBar({ items }: Props) {
  const navigation = useNavigation<any>();
  const { theme } = useThemeStore();
  const { isDesktop } = useResponsive();
  const themeColors = Colors[theme as keyof typeof Colors];

  return (
    <View style={[styles.container, { backgroundColor: themeColors.card, borderColor: themeColors.border, paddingVertical: isDesktop ? 8 : 10 }]}>
      {items.map((item, idx) => (
        <TouchableOpacity
          key={idx}
          style={[styles.button, { padding: isDesktop ? 4 : 6 }]}
          onPress={() => navigation.navigate(item.route)}
        >
          <Image source={item.icon} style={[styles.icon, { tintColor: themeColors.text }]} />
          <Text style={[styles.label, { color: themeColors.text, fontSize: isDesktop ? 12 : 14 }]}>{item.label}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    borderTopWidth: 1,
    justifyContent: "space-around",
  },
  button: { alignItems: "center" },
  icon: { width: 22, height: 22, marginBottom: 2 },
  label: { fontWeight: "600" },
});
