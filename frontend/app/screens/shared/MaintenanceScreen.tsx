import React from 'react';
import { View, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { Image } from 'expo-image';
import { theme } from '@/theme/theme';

export function MaintenanceScreen() {
  return (
    <View style={styles.container}>
      <Image 
        source={require('../../../assets/images/maintenance.gif')} 
        style={styles.image} 
        contentFit="contain"
      />
      <Text style={styles.title}>Under Maintenance</Text>
      <Text style={styles.message}>
        We are currently experiencing internal server issues or performing maintenance. 
        Please wait while we try to reconnect...
      </Text>
      <ActivityIndicator size="large" color={theme.colors.primary} style={{ marginTop: 20 }} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: theme.colors.background,
    justifyContent: 'center',
    alignItems: 'center',
    padding: theme.spacing.xl,
    zIndex: 9999, // Ensure it overlays everything
  },
  image: {
    width: 250,
    height: 250,
    marginBottom: theme.spacing.xl,
  },
  title: {
    fontSize: theme.typography.sizes.xl,
    fontWeight: 'bold',
    color: theme.colors.text,
    marginBottom: theme.spacing.md,
  },
  message: {
    fontSize: theme.typography.sizes.md,
    color: theme.colors.textSecondary,
    textAlign: 'center',
    lineHeight: 24,
  },
});
