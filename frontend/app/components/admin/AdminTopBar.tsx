import React from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";
import { useThemeStore } from "../../stores/themeStore";
import { Ionicons as VectorIcons } from '@expo/vector-icons';

// Cast to any to accept strict React 19 types
const Ionicons = VectorIcons as any;
import { useNavigation, CommonActions } from '@react-navigation/native';


export default function AdminTopBar() {
  const navigation = useNavigation<any>();
  const { logout, username } = useAuthStore();
  const { setMode } = useModeStore();
  const { theme } = useThemeStore();

  const isDarkMode = theme === 'dark';

  const handleSwitchToPlayer = () => {
    setMode("USER");
    navigation.dispatch(
      CommonActions.reset({
        index: 0,
        routes: [{ name: 'SwipeLab' }],
      })
    );
  };

  const dynamicStyles = {
    container: { backgroundColor: isDarkMode ? '#1f1f2e' : '#fff' },
    username: { color: isDarkMode ? '#9ca3af' : '#888' },
    actionText: { color: isDarkMode ? '#9ca3af' : '#888' },
  };

  return (
    <View style={[styles.container, dynamicStyles.container]}>
      {/* Profile */}
      <TouchableOpacity
        style={styles.profileSection}
        onPress={() => navigation.navigate("Profile")}
      >
        <View style={styles.avatarContainer}>
          <View style={[styles.avatar, { backgroundColor: isDarkMode ? '#374151' : '#e8f0fe' }]}>
            <Ionicons name="person" size={24} color={isDarkMode ? '#9ca3af' : '#666'} />
          </View>
          <Text style={[styles.username, dynamicStyles.username]}>{username || "Admin"}</Text>
        </View>
      </TouchableOpacity>

      <View style={styles.card}>
        <Text style={styles.text}>Manager Dashboard</Text>
        <Text style={styles.subText}>Overview</Text>
      </View>

      <View style={styles.rightSection}>
        <TouchableOpacity onPress={handleSwitchToPlayer} style={styles.switchBtn}>
          <Ionicons name="game-controller-outline" size={20} color="#0EA5E9" />
          <Text style={[styles.logoutText, dynamicStyles.actionText]}>Play</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.logoutBtn} onPress={logout}>
          <Ionicons name="log-out-outline" size={24} color="#EF4444" />
          <Text style={[styles.logoutText, dynamicStyles.actionText]}>Logout</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row", justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 12, alignItems: "center",
    borderBottomWidth: 1, borderBottomColor: '#f0f0f0',
  },
  profileSection: { alignItems: 'center', justifyContent: 'center' },
  avatarContainer: { alignItems: 'center' },
  avatar: {
    width: 40, height: 40, borderRadius: 20, marginBottom: 4, backgroundColor: '#ddd',
    alignItems: 'center', justifyContent: 'center',
  },
  username: { fontSize: 12, fontWeight: '600' },
  card: {
    backgroundColor: "#0EA5E9", paddingHorizontal: 20, paddingVertical: 8,
    borderRadius: 12, alignItems: "center", justifyContent: 'center', height: 70,
  },
  text: { color: "white", fontWeight: "700", fontSize: 13, lineHeight: 18 },
  subText: { color: "white", fontSize: 10, opacity: 0.9 },
  rightSection: { flexDirection: 'row', alignItems: 'center' },
  switchBtn: { alignItems: 'center', marginRight: 16 },
  logoutBtn: { alignItems: "center", justifyContent: "center" },
  logoutText: { fontSize: 10, marginTop: 2 },
});
