import React from 'react';
import { View, Text, StyleSheet, ActivityIndicator, TouchableOpacity, Linking, SafeAreaView } from 'react-native';
import { Image } from 'expo-image';
import { theme } from '@/theme/theme';

export function MaintenanceScreen() {
  const handleContactSupport = () => {
    Linking.openURL('mailto:swipelab.developers@gmail.com?subject=SwipeLab%20-%20Server%20is%20down');
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Image 
          source={require('../../../assets/images/swipelab.gif')} 
          style={styles.logo} 
          contentFit="contain"
        />
        <Image 
          source={require('../../../assets/images/maintenance.gif')} 
          style={styles.mainImage} 
          contentFit="contain"
        />
        <Text style={styles.title}>We&apos;ll be right back!</Text>
        <Text style={styles.message}>
          SwipeLab is currently undergoing maintenance. 
          We&apos;re working hard to make things better for you.
        </Text>
        
        <ActivityIndicator size="large" color="#ffffff" style={styles.loader} />
        
        <TouchableOpacity style={styles.contactButton} onPress={handleContactSupport}>
          <Text style={styles.contactButtonText}>Contact Support</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: theme.colors.primary,
    zIndex: 9999, // Ensure it overlays everything
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
    color: '#ffffff',
    marginBottom: theme.spacing.md,
    textAlign: 'center',
  },
  message: {
    fontSize: theme.typography.sizes.md,
    color: 'rgba(255, 255, 255, 0.8)',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: theme.spacing.xl,
  },
  loader: {
    marginBottom: theme.spacing.xxl,
  },
  contactButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    paddingVertical: theme.spacing.md,
    paddingHorizontal: theme.spacing.xl,
    borderRadius: theme.borderRadius.round,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.5)',
  },
  contactButtonText: {
    color: '#ffffff',
    fontSize: theme.typography.sizes.md,
    fontWeight: 'bold',
  }
});
