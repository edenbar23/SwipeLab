import React from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import profileImg from "../../../assets/images/profile.png";
import { useAuthStore } from "../../stores/authStore";

export default function UserTopBar() {
  const { logout, username } = useAuthStore();

  return (
    <View style={styles.container}>
      {/* Profile section */}
      <View style={styles.profileSection}>
        <Image
          source={profileImg}
          style={styles.avatar}
        />
        <Text style={styles.username}>{username || "Player"}</Text>
      </View>

      {/* Blue card */}
      <View style={styles.card}>
        <Text style={styles.text}>Score: 520</Text>
        <Text style={styles.text}>Rank: #12</Text>
        <Text style={styles.text}>Streak: 36 days</Text>
      </View>

      {/* Logout */}
      <TouchableOpacity style={styles.logoutBtn} onPress={logout}>
        <Text style={styles.logoutText}>Logout</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    padding: 10,
    alignItems: "center",
    backgroundColor: "#f7f7f7",
  },
  profileSection: { flex: 1 },
  avatar: { width: 45, height: 45, borderRadius: 25, marginBottom: 4 },
  username: { fontWeight: "bold", fontSize: 16 },

  card: {
    flex: 2,
    backgroundColor: "#4B7BE5",
    padding: 10,
    borderRadius: 12,
    alignItems: "center",
  },
  text: { color: "white", fontWeight: "600" },

  logoutBtn: { flex: 1, alignItems: "flex-end" },
  logoutText: { color: "red", fontWeight: "600" },
});
