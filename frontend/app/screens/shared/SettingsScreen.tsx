import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Switch } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuthStore } from '../../stores/authStore';
import { useThemeStore } from '../../stores/themeStore';

export default function SettingsScreen() {
  const { logout } = useAuthStore();
  const { theme, toggleTheme } = useThemeStore();
  const [notifications, setNotifications] = useState(true);

  const isDarkMode = theme === 'dark';

  // Dynamic styles based on theme
  const dynamicStyles = {
    container: {
      backgroundColor: isDarkMode ? '#1a1a2e' : '#f8f9fa',
    },
    pageTitle: {
      color: isDarkMode ? '#fff' : '#1a1a2e',
    },
    sectionTitle: {
      color: isDarkMode ? '#9ca3af' : '#666',
    },
    settingRow: {
      backgroundColor: isDarkMode ? '#2d2d44' : '#fff',
    },
    settingLabel: {
      color: isDarkMode ? '#fff' : '#333',
    },
    iconColor: isDarkMode ? '#9ca3af' : '#666',
    chevronColor: isDarkMode ? '#6b7280' : '#999',
    versionText: {
      color: isDarkMode ? '#6b7280' : '#999',
    },
  };

  return (
    <View style={[styles.container, dynamicStyles.container]}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <Text style={[styles.pageTitle, dynamicStyles.pageTitle]}>Settings</Text>

        {/* Account Section */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, dynamicStyles.sectionTitle]}>Account</Text>

          <View style={[styles.settingRow, dynamicStyles.settingRow]}>
            <View style={styles.settingLeft}>
              <Ionicons name="person-outline" size={24} color={dynamicStyles.iconColor} />
              <Text style={[styles.settingLabel, dynamicStyles.settingLabel]}>Profile</Text>
            </View>
            <Ionicons name="chevron-forward" size={20} color={dynamicStyles.chevronColor} />
          </View>
        </View>

        {/* Preferences Section */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, dynamicStyles.sectionTitle]}>Preferences</Text>

          <View style={[styles.settingRow, dynamicStyles.settingRow]}>
            <View style={styles.settingLeft}>
              <Ionicons name="notifications-outline" size={24} color={dynamicStyles.iconColor} />
              <Text style={[styles.settingLabel, dynamicStyles.settingLabel]}>Notifications</Text>
            </View>
            <Switch
              value={notifications}
              onValueChange={setNotifications}
              trackColor={{ false: '#ccc', true: '#4B7BE5' }}
              thumbColor={notifications ? '#fff' : '#f4f3f4'}
            />
          </View>

          <View style={[styles.settingRow, dynamicStyles.settingRow]}>
            <View style={styles.settingLeft}>
              <Ionicons name="moon-outline" size={24} color={dynamicStyles.iconColor} />
              <Text style={[styles.settingLabel, dynamicStyles.settingLabel]}>Dark Mode</Text>
            </View>
            <Switch
              value={isDarkMode}
              onValueChange={toggleTheme}
              trackColor={{ false: '#ccc', true: '#4B7BE5' }}
              thumbColor={isDarkMode ? '#fff' : '#f4f3f4'}
            />
          </View>
        </View>

        {/* Support Section */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, dynamicStyles.sectionTitle]}>Support</Text>

          <TouchableOpacity style={[styles.settingRow, dynamicStyles.settingRow]}>
            <View style={styles.settingLeft}>
              <Ionicons name="help-circle-outline" size={24} color={dynamicStyles.iconColor} />
              <Text style={[styles.settingLabel, dynamicStyles.settingLabel]}>Help Center</Text>
            </View>
            <Ionicons name="open-outline" size={20} color={dynamicStyles.chevronColor} />
          </TouchableOpacity>

          <TouchableOpacity style={[styles.settingRow, dynamicStyles.settingRow]}>
            <View style={styles.settingLeft}>
              <Ionicons name="information-circle-outline" size={24} color={dynamicStyles.iconColor} />
              <Text style={[styles.settingLabel, dynamicStyles.settingLabel]}>About</Text>
            </View>
            <Ionicons name="chevron-forward" size={20} color={dynamicStyles.chevronColor} />
          </TouchableOpacity>
        </View>

        {/* Logout Button */}
        <TouchableOpacity style={styles.logoutButton} onPress={logout}>
          <Ionicons name="log-out-outline" size={20} color="#fff" style={{ marginRight: 8 }} />
          <Text style={styles.logoutText}>Log Out</Text>
        </TouchableOpacity>

        <Text style={[styles.versionText, dynamicStyles.versionText]}>SwipeLab v1.0.0-alpha</Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
    paddingBottom: 100,
  },
  pageTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 24,
  },
  section: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  settingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    borderRadius: 12,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  settingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  settingLabel: {
    marginLeft: 12,
    fontSize: 16,
    fontWeight: '500',
  },
  logoutButton: {
    flexDirection: 'row',
    backgroundColor: '#EF4444',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 20,
    shadowColor: '#EF4444',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 3,
  },
  logoutText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  versionText: {
    textAlign: 'center',
    marginTop: 24,
    fontSize: 12,
  },
});