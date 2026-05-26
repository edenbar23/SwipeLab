import React from 'react';
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuthStore } from '../../stores/authStore';

/**
 * Shown when any API call returns a 403 with a BANNED account code,
 * or when the backend sets accountLocked=true on the user profile.
 *
 * Displayed via RootNavigator when the user's status is detected as BANNED.
 * Invalidates local auth tokens so the user cannot continue without re-auth.
 */
export default function BannedScreen() {
  const { logout } = useAuthStore();

  return (
    <View style={styles.container}>
      <View style={styles.card}>
        {/* Icon */}
        <View style={styles.iconWrap}>
          <Ionicons name="ban" size={52} color="#ef4444" />
        </View>

        {/* Title */}
        <Text style={styles.title}>Account Suspended</Text>

        {/* Message */}
        <Text style={styles.message}>
          Your account has been suspended due to suspicious labeling activity.{'\n\n'}
          If you believe this is a mistake, please contact the research team.
        </Text>

        {/* Contact hint */}
        <View style={styles.hintBox}>
          <Ionicons name="mail-outline" size={16} color="#f59e0b" style={{ marginRight: 6 }} />
          <Text style={styles.hintText}>contact@swipelab.com</Text>
        </View>

        {/* Logout */}
        <TouchableOpacity
          style={styles.logoutBtn}
          onPress={logout}
          activeOpacity={0.8}
          accessibilityRole="button"
          accessibilityLabel="Log out"
        >
          <Ionicons name="log-out-outline" size={18} color="#fff" style={{ marginRight: 8 }} />
          <Text style={styles.logoutText}>Log Out</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  card: {
    width: '100%',
    maxWidth: 380,
    backgroundColor: '#1e2533',
    borderRadius: 24,
    padding: 32,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.4,
    shadowRadius: 24,
    elevation: 10,
    borderWidth: 1,
    borderColor: '#ef444430',
  },
  iconWrap: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: '#ef444418',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  title: {
    color: '#f9fafb',
    fontSize: 24,
    fontWeight: '800',
    marginBottom: 12,
    textAlign: 'center',
  },
  message: {
    color: '#9ca3af',
    fontSize: 14,
    lineHeight: 22,
    textAlign: 'center',
    marginBottom: 20,
  },
  hintBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#78350f30',
    borderRadius: 10,
    paddingVertical: 8,
    paddingHorizontal: 14,
    marginBottom: 28,
  },
  hintText: {
    color: '#f59e0b',
    fontSize: 13,
    fontWeight: '600',
  },
  logoutBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ef4444',
    borderRadius: 14,
    paddingVertical: 13,
    paddingHorizontal: 32,
  },
  logoutText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
});
