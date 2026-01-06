import React, { useEffect, useState } from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";
import { useThemeStore } from "../../stores/themeStore";
import { Ionicons } from '@expo/vector-icons';
import { statisticsMock } from '../../mocks/data/statistics.mock';
import { useNavigation, CommonActions } from '@react-navigation/native';


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
  const { logout, role, username: authUsername } = useAuthStore();
  const { setMode } = useModeStore();
  const { theme } = useThemeStore();
  const navigation = useNavigation<any>();

  const isDarkMode = theme === 'dark';

  const [stats, setStats] = useState(statisticsMock.summary);

  useEffect(() => {
    setStats(statisticsMock.summary);
  }, []);

  const displayUsername = propUsername || authUsername || stats.username || "Player";
  const displayScore = propScore !== undefined ? propScore : stats.score;
  const displayRank = propRank || `#${stats.rankGlobal}`;
  const displayStreak = propStreak !== undefined ? propStreak : stats.currentStreakDays;
  const handleLogout = onLogout || logout;
  const formattedScore = typeof displayScore === 'number' ? displayScore.toLocaleString() : displayScore;

  const handleSwitchToManager = () => {
    setMode("ADMIN");
    navigation.dispatch(
      CommonActions.reset({
        index: 0,
        routes: [{ name: 'AdminDashboard' }],
      })
    );
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

      <View style={styles.statsBlock}>
        <Text style={styles.statsText}>Score: {formattedScore}</Text>
        <Text style={styles.statsText}>Rank: {displayRank}</Text>
        <Text style={styles.statsText}>{displayStreak} days streak</Text>
      </View>

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
