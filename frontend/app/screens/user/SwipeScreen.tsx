import { useRoute } from '@react-navigation/native';
import React, { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Platform, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import { useSwipeBatch, QUERY_KEYS } from '../../api/queries';
import { useQueryClient } from '@tanstack/react-query';
import ReferenceGallery from '../../components/user/ReferenceGallery';
import SwipeButtons from '../../components/user/SwipeButtons';
import SwipeCard, { SwipeCardHandle } from '../../components/user/SwipeCard';
import useResponsive from '../../hooks/useResponsive';
import { useThemeStore } from '../../stores/themeStore';
import { useSwipeStore } from '../../stores/swipeStore';
import { SwipeDirection } from '../../types';

const BACKEND_BASE_URL = process.env.EXPO_PUBLIC_API_URL ||
  (Platform.OS === 'web' ? 'http://localhost:8080' : 'http://192.168.1.133:8080');




export default function SwipeScreen() {
  const [showReference, setShowReference] = useState(false);
  const { dataBatch, currentIndex, setBatch, nextCard, clearBatch } = useSwipeStore();
  const [loading, setLoading] = useState(false); // only true during manual fetchNextBatch
  const [error, setError] = useState<string | null>(null);

  const route = useRoute<any>();
  const taskId = route?.params?.taskId || 1;

  const { isPhone, isDesktop } = useResponsive();
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const size = isDesktop ? 200 : isPhone ? 300 : 250;

  const cardRef = useRef<SwipeCardHandle>(null);

  const queryClient = useQueryClient();
  const { data: initialBatch, isLoading: isQueryLoading, error: queryError } = useSwipeBatch(taskId);

  // Clear any stale batch from a previous session on mount
  useEffect(() => {
    clearBatch();
  }, []);

  useEffect(() => {
    if (initialBatch?.images?.length > 0) {
      console.log('[SwipeScreen] Batch loaded, first image keys:', Object.keys(initialBatch.images[0]));
      console.log('[SwipeScreen] First image.image keys:', initialBatch.images[0]?.image ? Object.keys(initialBatch.images[0].image) : 'no image object');
      setBatch(initialBatch.images);
    } else if (Array.isArray(initialBatch) && initialBatch.length > 0) {
      setBatch(initialBatch); // fallback
    }
  }, [initialBatch, setBatch]);

  const fetchNextBatch = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch(API_ENDPOINTS.CLASSIFICATIONS.NEXT_BATCH(taskId, 5), { method: 'GET' });
      if (res.ok) {
        const json = await res.json();
        const newImages = json.images || [];
        setBatch(newImages);
        // Cache the new batch 
        queryClient.setQueryData(QUERY_KEYS.swipeBatch(taskId), { images: newImages });
      } else {
        setError(`Failed to fetch batch (Status: ${res.status})`);
      }
    } catch (e: any) {
      setError(`Error fetching batch: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSwipe = async (direction: SwipeDirection) => {
    console.log(`Swiped: ${direction}`);

    const currentImage = dataBatch[currentIndex];

    if (currentImage) {
      let decision = direction.toUpperCase();
      if (direction === 'dont-know') decision = 'DONT_KNOW';

      apiFetch(`/api/v1/classifications/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageId: currentImage.imageId,
          taskId: currentImage.taskId,
          question: currentImage.question,
          decision: decision,
          responseTimeMs: 0
        })
      })
      .then((res) => {
        if (res.ok) {
          // Invalidate challenges cache so it updates immediately after classification/completion
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.challenges });
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.myBadges });
        }
      })
      .catch(e => console.error("Submit error:", e));
    }

    if (currentIndex + 1 < dataBatch.length) {
      nextCard();
      setShowReference(false);
    } else {
      fetchNextBatch();
    }
  };

  useEffect(() => {
    if (Platform.OS !== 'web' || loading || dataBatch.length === 0) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowUp':
          cardRef.current?.swipeCard('dont-know');
          break;
        case 'ArrowDown':
          cardRef.current?.swipeCard('trash');
          break;
        case 'ArrowLeft':
          cardRef.current?.swipeCard('no');
          break;
        case 'ArrowRight':
          cardRef.current?.swipeCard('yes');
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [loading, dataBatch, currentIndex]);

  if (loading || isQueryLoading) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <ActivityIndicator size="large" color={themeColors.tint} />
      </View>
    );
  }

  if (error || queryError || dataBatch.length === 0) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <Text style={{ color: themeColors.text }}>{error || 'No more images to classify.'}</Text>
      </View>
    );
  }

  const currentImage = dataBatch[currentIndex];
  const rawImageData = currentImage?.image?.data;
  let imageUrl: string | null = null;
  if (rawImageData) {
    if (rawImageData.startsWith('http')) {
      // Already a full HTTP URL
      imageUrl = rawImageData;
    } else if (rawImageData.startsWith('data:image')) {
      // Already a correctly-formed Data URI
      imageUrl = rawImageData;
    } else if (/^[A-Za-z0-9+/]/.test(rawImageData) || rawImageData.startsWith('/9')) {
      // Raw Base64 bytes (including JPEG base64 which starts with /9j/) — build Data URI
      const contentType = currentImage?.image?.contentType || 'image/jpeg';
      imageUrl = `data:${contentType};base64,${rawImageData}`;
    } else if (rawImageData.startsWith('/')) {
      // Relative server path — prepend backend base URL
      imageUrl = `${BACKEND_BASE_URL}${rawImageData}`;
    }
  }

  const referenceImagesUrls = currentImage?.referenceImages?.map((ref: any) => {
    if (ref.data?.startsWith('http')) return ref.data;
    return `data:${ref.contentType || 'image/jpeg'};base64,${ref.data}`;
  }) || [];

  return (
    <View style={[styles.container, { backgroundColor: themeColors.background }]}>
      <View style={[styles.cardSection, { maxWidth: size }]}>
        <SwipeCard
          ref={cardRef}
          question={currentImage?.question || 'Is this a ...'}
          imageUrl={imageUrl}
          onSwipe={handleSwipe}
          key={currentImage?.imageId}
        />
      </View>

      <View style={styles.buttonSection}>
        {showReference ? (
          <ReferenceGallery
            images={referenceImagesUrls.length > 0 ? referenceImagesUrls : ['https://via.placeholder.com/300?text=No+Reference+Images']}
            onClose={() => setShowReference(false)}
          />
        ) : (
          <SwipeButtons
            onSwipe={(direction) => {
              cardRef.current?.swipeCard(direction);
            }}
            onToggleReference={() => setShowReference(!showReference)}
            showReference={showReference}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 0,
    alignItems: 'center',
  },
  centerElements: {
    justifyContent: 'center',
  },
  cardSection: {
    width: '100%',
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  buttonSection: {
    width: '100%',
    paddingHorizontal: 16,
    maxWidth: 300,
  },
});
