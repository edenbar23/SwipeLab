import { useNavigation } from "@react-navigation/native";
import React from "react";
import { Image, ImageSourcePropType, StyleSheet, Text, TouchableOpacity, View } from "react-native";

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

  return (
    
    <View style={styles.container}>
      {items.map((item) => (
        <TouchableOpacity
          key={item.route}
          style={styles.button}
          onPress={() => navigation.navigate(item.route)}
        >
          <Image
            source={item.icon}
            style={styles.icon}
            resizeMode="contain"
          />
          <Text style={styles.label}>{item.label}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    backgroundColor: "#fff",
    borderTopWidth: 1,
    borderColor: "#ddd",
    paddingVertical: 10,
    justifyContent: "space-around",
  },
  button: {alignItems: "center", padding: 6 },
  icon: {
    width: 22,
    height: 22,
    marginBottom: 2,
  },
  label: { fontSize: 14, fontWeight: "600" },
});
