import React, { useEffect, useState } from 'react';
import { Image, ImageProps, Platform, StyleSheet, View, ActivityIndicator } from 'react-native';
import { useAuthStore } from '@/stores/authStore';
import { backendUrl } from '@/api/apiFetch';

interface AuthenticatedImageProps extends Omit<ImageProps, 'source'> {
  uri: string | null | undefined;
  fallbackUri?: string;
  loaderColor?: string;
}

export default function AuthenticatedImage({ 
  uri, 
  fallbackUri, 
  loaderColor = '#3B82F6', 
  style, 
  ...rest 
}: AuthenticatedImageProps) {
  const [localUri, setLocalUri] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const token = useAuthStore(state => state.token);

  useEffect(() => {
    if (!uri) {
      setLocalUri(null);
      setError(false);
      return;
    }

    // If it's a data URI, local file, or blob, use it directly
    if (uri.startsWith('data:') || uri.startsWith('file:') || uri.startsWith('blob:')) {
      setLocalUri(uri);
      setError(false);
      return;
    }

    // Resolve relative API paths to full URLs using the centralized backend base
    const resolvedUri = uri.startsWith('http') ? uri : `${backendUrl}${uri}`;

    if (Platform.OS === 'web') {
      let isActive = true;
      let objectUrl: string | null = null;

      const fetchImage = async () => {
        setLoading(true);
        setError(false);
        try {
          const headers: Record<string, string> = {};
          if (token) {
            headers['Authorization'] = `Bearer ${token}`;
          }
          const response = await fetch(resolvedUri, { headers });
          if (!response.ok) {
            throw new Error(`Failed to fetch image: ${response.statusText}`);
          }
          const blob = await response.blob();
          if (isActive) {
            objectUrl = URL.createObjectURL(blob);
            setLocalUri(objectUrl);
          }
        } catch (e) {
          console.error('Failed to load authenticated image:', e);
          if (isActive) {
            setError(true);
            setLocalUri(resolvedUri); // Fallback to resolved uri
          }
        } finally {
          if (isActive) setLoading(false);
        }
      };

      fetchImage();

      return () => {
        isActive = false;
        if (objectUrl) {
          URL.revokeObjectURL(objectUrl);
        }
      };
    } else {
      // On Native, we let React Native's Image component handle headers
      setLocalUri(resolvedUri);
      setError(false);
    }
  }, [uri, token]);

  if (!uri || (error && !fallbackUri)) {
    return (
      <View style={[style as any, styles.placeholder]}>
        <Image 
          source={require('../../../assets/images/taxonomy.png')} 
          style={{ width: '50%', height: '50%', opacity: 0.3 }} 
          resizeMode="contain" 
        />
      </View>
    );
  }

  const finalSource = Platform.OS === 'web' 
    ? { uri: localUri || fallbackUri || uri }
    : { 
        uri: localUri || fallbackUri || uri, 
        ...(token ? { headers: { Authorization: `Bearer ${token}` } } : {}) 
      };

  return (
    <View style={[style as any, styles.container]}>
      {localUri && (
        <Image
          source={finalSource}
          style={[StyleSheet.absoluteFill, { width: '100%', height: '100%' }]}
          {...rest}
        />
      )}
      {loading && (
        <View style={styles.loaderContainer}>
          <ActivityIndicator color={loaderColor} size="small" />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    overflow: 'hidden',
    position: 'relative',
  },
  placeholder: {
    backgroundColor: '#E5E7EB',
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
  },
  loaderContainer: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.3)',
  }
});
