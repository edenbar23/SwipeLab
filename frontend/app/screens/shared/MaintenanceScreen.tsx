import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, TouchableOpacity, Linking, SafeAreaView, Platform } from 'react-native';
import { Image } from 'expo-image';
import { theme } from '@/theme/theme';
import Animated, { FadeIn, FadeInDown, SlideInDown } from 'react-native-reanimated';
import { useAppStateStore } from '@/stores/appStateStore';
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { backendUrl } from '@/api/apiFetch';

const STATUS_MESSAGES = [
  "Diagnosing system health...",
  "Restoring database connections...",
  "Re-establishing secure links...",
  "Synchronizing data pipelines...",
  "Finalizing system checks..."
];

export function MaintenanceScreen() {
  const [statusIndex, setStatusIndex] = useState(0);
  const setMaintenanceMode = useAppStateStore((state) => state.setMaintenanceMode);

  useEffect(() => {
    const statusInterval = setInterval(() => {
      setStatusIndex((prev) => (prev + 1) % STATUS_MESSAGES.length);
    }, 4000);
    return () => clearInterval(statusInterval);
  }, []);

  useEffect(() => {
    const healthInterval = setInterval(async () => {
      try {
        const res = await fetch(backendUrl + API_ENDPOINTS.SYSTEM.HEALTH, { method: 'GET' });
        if (res.ok) {
          setMaintenanceMode(false);
        }
      } catch (e) {
        // Backend is still down
      }
    }, 10000);
    return () => clearInterval(healthInterval);
  }, [setMaintenanceMode]);

  const handleContactSupport = () => {
    Linking.openURL('mailto:swipelab.developers@gmail.com?subject=SwipeLab%20-%20Server%20is%20down');
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Animated.View entering={FadeInDown.duration(800).delay(100)} pointerEvents="none">
          <Image 
            source={Platform.OS === 'web' ? { uri: '/swipelab.gif' } : require('../../../assets/images/swipelab.gif')} 
            style={styles.logo} 
            contentFit="contain"
          />
        </Animated.View>

        <Animated.View entering={FadeInDown.duration(800).delay(200)} pointerEvents="none">
          <Image 
            source={Platform.OS === 'web' ? { uri: '/maintenance.gif' } : require('../../../assets/images/maintenance.gif')} 
            style={styles.mainImage} 
            contentFit="contain"
          />
        </Animated.View>

        <Animated.Text entering={FadeInDown.duration(800).delay(300)} style={styles.title}>
          We'll be right back!
        </Animated.Text>
        
        <Animated.Text entering={FadeInDown.duration(800).delay(400)} style={styles.message}>
          SwipeLab is currently undergoing maintenance. 
          We're working hard to make things better for you.
        </Animated.Text>
        
        <Animated.View entering={FadeIn.duration(800).delay(500)} style={styles.statusContainer}>
          <ActivityIndicator size="small" color={theme.colors.primary} style={styles.loader} />
          <Text style={styles.statusText}>{STATUS_MESSAGES[statusIndex]}</Text>
        </Animated.View>
        
        <Animated.View entering={SlideInDown.duration(800).delay(700)}>
          <TouchableOpacity style={styles.contactButton} onPress={handleContactSupport}>
            <Text style={styles.contactButtonText}>Contact Support</Text>
          </TouchableOpacity>
        </Animated.View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#ffffff',
    zIndex: 9999,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: theme.spacing.xl,
  },
  logo: {
    width: 100,
    height: 100,
    marginBottom: theme.spacing.xl,
  },
  mainImage: {
    width: 200,
    height: 200,
    marginBottom: theme.spacing.xl,
  },
  title: {
    fontSize: theme.typography.sizes.xl,
    fontWeight: 'bold',
    color: theme.colors.primary,
    marginBottom: theme.spacing.md,
    textAlign: 'center',
  },
  message: {
    fontSize: theme.typography.sizes.md,
    color: '#666666',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: theme.spacing.lg,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.03)',
    paddingVertical: theme.spacing.sm,
    paddingHorizontal: theme.spacing.lg,
    borderRadius: theme.borderRadius.round,
    marginBottom: theme.spacing.xxl,
  },
  loader: {
    marginRight: theme.spacing.sm,
  },
  statusText: {
    color: theme.colors.primary,
    fontSize: theme.typography.sizes.sm,
    fontWeight: '500',
  },
  contactButton: {
    backgroundColor: theme.colors.primary,
    paddingVertical: theme.spacing.md,
    paddingHorizontal: theme.spacing.xl,
    borderRadius: theme.borderRadius.round,
  },
  contactButtonText: {
    color: '#ffffff',
    fontSize: theme.typography.sizes.md,
    fontWeight: 'bold',
  }
});
