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
  { title: "Taxonomy", image: taxonomyImg },
  { title: "Recipients List", image: recipientsImg },
  { title: "Gold Images", image: goldImagesImg },
  { title: "Add Gold Image", image: addGoldImg },
];

export default function AdminDashboard({ navigation }: any) {
  const { isPhone } = useResponsive();
  const isWeb = Platform.OS === "web";

  // 🔹 Tile sizing
  const tileSize = isWeb ? 180 : isPhone ? 140 : 150;
  const imageSize = tileSize * 0.45;
  const fontSize = 14;

  const Content = (
    <View style={[styles.container, isWeb && styles.webContainer]}>
      {buttons.map((btn, index) => (
        <TouchableOpacity
  key={index}
  style={[
    styles.tile,
    { width: tileSize, height: tileSize },
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
          <Text style={[styles.title, { fontSize }]}>
            {btn.title}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );

  // 🌐 WEB — full-width background, no scroll
  if (isWeb) {
    return <View style={styles.background}>{Content}</View>;
  }

  // 📱 MOBILE — scroll enabled
  return (
    <ScrollView contentContainerStyle={styles.scrollContainer}>
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
