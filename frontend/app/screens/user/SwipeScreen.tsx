import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import { QUERY_KEYS, useSwipeBatch } from '../../api/queries';
import ReferenceGallery from '../../components/user/ReferenceGallery';
import SwipeButtons from '../../components/user/SwipeButtons';
import SwipeCard, { SwipeCardHandle } from '../../components/user/SwipeCard';
import useResponsive from '../../hooks/useResponsive';
import { useSwipeStore } from '../../stores/swipeStore';
import { useThemeStore } from '../../stores/themeStore';
import { SwipeDirection } from '../../types';
import { useQueryClient } from '@tanstack/react-query';

const BACKEND_BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ||
  (Platform.OS === 'web' ? 'http://localhost:8080' : 'http://192.168.1.133:8080');

export default function SwipeScreen() {
  const navigation = useNavigation<any>();
  const [showReference, setShowReference] = useState(false);
  const { dataBatch, currentIndex, activeTaskId, setBatch, nextCard, clearBatch } =
    useSwipeStore();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { isPhone, isDesktop } = useResponsive();
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const size = isDesktop ? 200 : isPhone ? 300 : 250;

  const cardRef = useRef<SwipeCardHandle>(null);
  const queryClient = useQueryClient();

  // Only fetch when a task has been selected; prevents spurious network calls
  const {
    data: initialBatch,
    isLoading: isQueryLoading,
    error: queryError,
  } = useSwipeBatch(activeTaskId as string | number, { enabled: !!activeTaskId });

  // Clear stale batch whenever the active task changes
  useEffect(() => {
    clearBatch();
    setError(null);
  }, [activeTaskId]);

  useEffect(() => {
    if (initialBatch?.images?.length > 0) {
      setBatch(initialBatch.images);
    } else if (Array.isArray(initialBatch) && initialBatch.length > 0) {
      setBatch(initialBatch);
    }
  }, [initialBatch, setBatch]);

  const fetchNextBatch = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch(
        API_ENDPOINTS.CLASSIFICATIONS.NEXT_BATCH(activeTaskId as string | number, 5),
        { method: 'GET' }
      );
      if (res.ok) {
        const json = await res.json();
        const newImages = json.images || [];
        setBatch(newImages);
        queryClient.setQueryData(QUERY_KEYS.swipeBatch(activeTaskId as string | number), {
          images: newImages,
        });
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
          decision,
          responseTimeMs: 0,
        }),
      })
        .then((res) => {
          if (res.ok) {
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.challenges });
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.myBadges });
          }
        })
        .catch((e) => console.error('Submit error:', e));
    }

    if (currentIndex + 1 < dataBatch.length) {
      nextCard();
      setShowReference(false);
    } else {
      fetchNextBatch();
    }
  };

  // Keyboard shortcuts (web only)
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

  // ─── Empty State: no task selected ─────────────────────────────────────────
  if (!activeTaskId) {
    return (
      <View
        style={[
          styles.container,
          styles.centerElements,
          { backgroundColor: themeColors.background },
        ]}
      >
        <View style={[styles.emptyCard, { backgroundColor: themeColors.card }]}>
          {/* Icon */}
          <View style={[styles.emptyIconCircle, { backgroundColor: themeColors.background }]}>
            <Ionicons name="images-outline" size={48} color={themeColors.tint ?? '#4B7BE5'} />
          </View>

          <Text style={[styles.emptyTitle, { color: themeColors.text }]}>
            No Active Task
          </Text>
          <Text style={[styles.emptySubtitle, { color: themeColors.textSecondary }]}>
            Select a task and start labeling!
          </Text>

          <TouchableOpacity
            style={[styles.exploreButton, { backgroundColor: themeColors.tint ?? '#4B7BE5' }]}
            onPress={() => navigation.navigate('Tasks')}
            activeOpacity={0.82}
          >
            <Ionicons name="search" size={18} color="#fff" style={{ marginRight: 8 }} />
            <Text style={styles.exploreButtonText}>Explore Tasks</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ─── Loading ────────────────────────────────────────────────────────────────
  if (loading || isQueryLoading) {
    return (
      <View
        style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}
      >
        <ActivityIndicator size="large" color={themeColors.tint} />
      </View>
    );
  }

  // ─── Error / Batch Exhausted ────────────────────────────────────────────────
  if (error || queryError || dataBatch.length === 0) {
    return (
      <View
        style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}
      >
        <Text style={{ color: themeColors.text }}>
          {error || 'No more images to classify.'}
        </Text>
      </View>
    );
  }

  // ─── Active Swipe UI ────────────────────────────────────────────────────────
  const currentImage = dataBatch[currentIndex];
  const rawImageData = currentImage?.image?.data;
  let imageUrl: string | null = null;

  if (rawImageData) {
    if (rawImageData.startsWith('http')) {
      imageUrl = rawImageData;
    } else if (rawImageData.startsWith('data:image')) {
      imageUrl = rawImageData;
    } else if (/^[A-Za-z0-9+/]/.test(rawImageData) || rawImageData.startsWith('/9')) {
      const contentType = currentImage?.image?.contentType || 'image/jpeg';
      imageUrl = `data:${contentType};base64,${rawImageData}`;
    } else if (rawImageData.startsWith('/')) {
      imageUrl = `${BACKEND_BASE_URL}${rawImageData}`;
    }
  }

  const referenceImagesUrls =
    currentImage?.referenceImages?.map((ref: any) => {
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
            images={
              referenceImagesUrls.length > 0
                ? referenceImagesUrls
                : ['https://via.placeholder.com/300?text=No+Reference+Images']
            }
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
  // ─── Empty State ────────────────────────────────────────────────────────────
  emptyCard: {
    width: '85%',
    maxWidth: 360,
    borderRadius: 24,
    padding: 32,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowRadius: 20,
    elevation: 6,
  },
  emptyIconCircle: {
    width: 96,
    height: 96,
    borderRadius: 48,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  emptyTitle: {
    fontSize: 22,
    fontWeight: '700',
    marginBottom: 8,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 15,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 28,
  },
  exploreButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 13,
    paddingHorizontal: 28,
    borderRadius: 14,
  },
  exploreButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
  // ─── Swipe UI ───────────────────────────────────────────────────────────────
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
