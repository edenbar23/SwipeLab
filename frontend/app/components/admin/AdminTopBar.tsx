import React from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import profileImg from "../../../assets/images/profile.png";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";

export default function AdminTopBar() {
  const { logout, username } = useAuthStore();
  const { mode, setMode } = useModeStore();

  return (
    <View style={styles.container}>
      {/* Profile */}
      <View style={styles.profileSection}>
        <Image
          source={profileImg}
          style={styles.avatar}
        />
        <Text style={styles.username}>{username || "Admin"}</Text>
      </View>

      {/* Blue card */}
      <View style={styles.card}>
        <TouchableOpacity onPress={() => setMode("admin")}>
          <Text style={styles.text}>Manager</Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => setMode(mode === "admin" ? "user" : "admin")}
          style={{ marginTop: 10 }}
        >
          <Text style={styles.switchText}>
            Switch to {mode === "admin" ? "Play" : "Admin"}
          </Text>
        </TouchableOpacity>
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
  text: { color: "white", fontWeight: "700", fontSize: 16 },

  switchText: { color: "white", fontWeight: "600" },

  logoutBtn: { flex: 1, alignItems: "flex-end" },
  logoutText: { color: "red", fontWeight: "600" },
});
