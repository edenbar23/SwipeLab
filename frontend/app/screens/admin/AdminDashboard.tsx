import React from "react";
import {
  Image,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import useResponsive from "../../hooks/useResponsive";
import { useThemeStore } from '../../stores/themeStore';
import { Colors } from '../../../constants/theme';

// Images
import addGoldImg from "../../../assets/images/add_gold_image.png";
import addTaskImg from "../../../assets/images/add_task.png";
import goldImagesImg from "../../../assets/images/gold_images.png";
import recipientsImg from "../../../assets/images/recipients_lists.png";
import tasksImg from "../../../assets/images/tasks_mgmt.png";
import taxonomyImg from "../../../assets/images/taxonomy.png";

const buttons = [
  { title: "Tasks", image: tasksImg, screen: "TasksManagement" },
  { title: "Add Task", image: addTaskImg, screen: "AddTask" },
  { title: "Taxonomy", image: taxonomyImg, screen: "Taxonomy" },
  { title: "Gold Images", image: goldImagesImg, screen: "GoldImagesManagement" },
  { title: "Add Gold Image", image: addGoldImg, screen: "AddGoldImage" },
  { title: "Recipients List", image: recipientsImg, screen: "RecipientsList" },
];

export default function AdminDashboard({ navigation }: any) {
  const { isPhone, isDesktop } = useResponsive();
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  // 🔹 Tile sizing
  const tileSize = isDesktop ? 180 : isPhone ? 140 : 150;
  const imageSize = tileSize * 0.45;
  const fontSize = 14;

  const Content = (
    <View style={[styles.container, isDesktop && styles.webContainer]}>
      {buttons.map((btn, index) => (
        <TouchableOpacity
          key={index}
          style={[
            styles.tile,
            { width: tileSize, height: tileSize, backgroundColor: themeColors.card, borderColor: themeColors.border },
          ]}
          onPress={() => {
            if (btn.screen) {
              navigation.navigate(btn.screen);
            }
          }}
        >

          <Image
            source={btn.image}
            style={{ width: imageSize, height: imageSize }}
            resizeMode="contain"
          />
          <Text style={[styles.title, { fontSize, color: themeColors.text }]}>
            {btn.title}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );

  return (
    <ScrollView 
      style={{ flex: 1 }}
      contentContainerStyle={[styles.scrollContainer, { backgroundColor: themeColors.background }]} 
      showsVerticalScrollIndicator={false}
    >
      {Content}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  // ✅ FULL-WIDTH BLUE BACKGROUND
  background: {
    width: "100%",
    minHeight: "100%",
    backgroundColor: "#D6EAF8",
  },

  scrollContainer: {
    flexGrow: 1,
    backgroundColor: "#D6EAF8",
  },

  container: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "center",
    gap: 24,
    paddingVertical: 32,
  },

  // ✅ WEB: centered 3×3 grid
  webContainer: {
    maxWidth: 720, // 3 × 180 + gaps
    alignSelf: "center",
  },

  tile: {
    backgroundColor: "#fff",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#ccc",
    justifyContent: "center",
    alignItems: "center",
  },

  title: {
    marginTop: 10,
    fontWeight: "600",
    textAlign: "center",
  },
});
