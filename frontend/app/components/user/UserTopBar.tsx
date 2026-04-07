import { Ionicons as VectorIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from "../../api/apiFetch";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";
import { useThemeStore } from "../../stores/themeStore";

// Cast to any to accept strict React 19 types
const Ionicons = VectorIcons as any;

interface UserTopBarProps {
  username?: string;
  score?: number;
  rank?: string;
  streak?: number;
  onLogout?: () => void;
}

export default function UserTopBar({
  username: propUsername,
  score: propScore,
  rank: propRank,
  streak: propStreak,
  onLogout
}: UserTopBarProps) {
  const { logout, role } = useAuthStore();
  const { setMode } = useModeStore();
  const { theme } = useThemeStore();
  const navigation = useNavigation<any>();

  const isDarkMode = theme === 'dark';

  const [stats, setStats] = useState<any>(null);

  useEffect(() => {
    let isMounted = true;
    const fetchStats = async () => {
      try {
        const res = await apiFetch(API_ENDPOINTS.USERS.ME);
        if (res.ok) {
          const data = await res.json();
          if (isMounted) setStats(data);
        }
      } catch (err) {
        console.error("Failed to fetch user top bar stats", err);
      }
    };
    fetchStats();
    return () => {
      isMounted = false;
    };
  }, []);

  const displayUsername = stats?.displayName || stats?.username || propUsername || "Player";
  const displayScore = propScore !== undefined ? propScore : (stats?.score || 0);
  const displayRank = propRank || (stats?.rank ? `${stats.rank}` : `#${stats?.rankGlobal || '--'}`);
  const displayStreak = propStreak !== undefined ? propStreak : (stats?.currentStreakDays || 0);
  const handleLogout = onLogout || logout;
  const formattedScore = typeof displayScore === 'number' ? displayScore.toLocaleString() : displayScore;

  const handleSwitchToManager = () => {
    setMode("ADMIN");
  };

  // Dynamic styles for dark mode
  const dynamicStyles = {
    container: { backgroundColor: isDarkMode ? '#1f1f2e' : '#fff' },
    username: { color: isDarkMode ? '#9ca3af' : '#666' },
    actionText: { color: isDarkMode ? '#9ca3af' : '#888' },
  };

  return (
    <View style={[styles.container, dynamicStyles.container]}>
      {/* User Profile */}
      <TouchableOpacity
        style={styles.profileSection}
        onPress={() => navigation.navigate("Profile")}
      >
        <View style={styles.avatarContainer}>
          <View style={[styles.avatar, { backgroundColor: isDarkMode ? '#374151' : '#e8f0fe' }]}>
            <Ionicons name="person" size={24} color={isDarkMode ? '#9ca3af' : '#666'} />
          </View>
          <Text style={[styles.username, dynamicStyles.username]}>{displayUsername}</Text>
        </View>
      </TouchableOpacity>

      <TouchableOpacity style={styles.statsBlock} onPress={() => navigation.navigate("Challenges")}>
        <Text style={styles.statsText}>Score: {formattedScore}</Text>
        <Text style={styles.statsText}>Rank: {displayRank}</Text>
        <Text style={styles.statsText}>{displayStreak} days streak</Text>
      </TouchableOpacity>

      <View style={styles.rightSection}>
        {role === 'ADMIN' && (
          <TouchableOpacity onPress={handleSwitchToManager} style={styles.switchBtn}>
            <Ionicons name="briefcase-outline" size={20} color="#0EA5E9" />
            <Text style={[styles.actionText, dynamicStyles.actionText]}>Manager</Text>
          </TouchableOpacity>
        )}
        <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
          <Ionicons name="log-out-outline" size={24} color="#EF4444" />
          <Text style={[styles.actionText, dynamicStyles.actionText]}>Logout</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  profileSection: { alignItems: 'center', justifyContent: 'center' },
  avatarContainer: { alignItems: 'center' },
  avatar: {
    width: 40, height: 40, borderRadius: 20,
    alignItems: 'center', justifyContent: 'center', marginBottom: 4,
  },
  username: { fontSize: 12, fontWeight: '600' },
  statsBlock: {
    backgroundColor: '#4B7BE5',
    paddingHorizontal: 20, paddingVertical: 8,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center', height: 70,
  },
  statsText: { color: '#fff', fontSize: 12, fontWeight: '600', lineHeight: 18 },
  rightSection: { flexDirection: 'row', alignItems: 'center' },
  switchBtn: { alignItems: 'center', marginRight: 16 },
  logoutBtn: { alignItems: 'center' },
  actionText: { fontSize: 10, marginTop: 2 },
});
