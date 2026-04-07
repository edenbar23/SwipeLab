import { useRoute } from '@react-navigation/native';
import React, { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Platform, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import ReferenceGallery from '../../components/user/ReferenceGallery';
import SwipeButtons from '../../components/user/SwipeButtons';
import SwipeCard, { SwipeCardHandle } from '../../components/user/SwipeCard';
import useResponsive from '../../hooks/useResponsive';
import { useThemeStore } from '../../stores/themeStore';
import { SwipeDirection } from '../../types';


export default function SwipeScreen() {
  const [showReference, setShowReference] = useState(false);

  const [dataBatch, setDataBatch] = useState<any[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const route = useRoute<any>();
  const taskId = route?.params?.taskId || 1;

  const { isPhone, isDesktop } = useResponsive();
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const size = isDesktop ? 200 : isPhone ? 300 : 250;

  const cardRef = useRef<SwipeCardHandle>(null);

  const fetchBatch = async (isInitial = false) => {
    setLoading(true);
    setError(null);
    try {
      const endpoint = isInitial
        ? API_ENDPOINTS.TASKS.PLAY_TASK(taskId)
        : API_ENDPOINTS.CLASSIFICATIONS.NEXT_BATCH(taskId, 5);

      const method = isInitial ? 'POST' : 'GET';
      const res = await apiFetch(endpoint, { method });
      if (res.ok) {
        const json = await res.json();
        setDataBatch(json.images || []);
        setCurrentIndex(0);
      } else {
        const errText = await res.text().catch(() => '');
        console.error(`Fetch batch failed. Status: ${res.status}. Body:`, errText);
        setError(`Failed to fetch batch (Status: ${res.status})`);
      }
    } catch (e: any) {
      console.error("Fetch batch exception:", e);
      setError(`Error fetching batch: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBatch(true);
  }, [taskId]);

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
      }).catch(e => console.error("Submit error:", e));
    }

    if (currentIndex + 1 < dataBatch.length) {
      setCurrentIndex(prev => prev + 1);
      setShowReference(false);
    } else {
      fetchBatch();
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

  if (loading && dataBatch.length === 0) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <ActivityIndicator size="large" color={themeColors.tint} />
      </View>
    );
  }

  if (error || dataBatch.length === 0) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <Text style={{ color: themeColors.text }}>{error || 'No more images to classify.'}</Text>
      </View>
    );
  }

  const currentImage = dataBatch[currentIndex];
  let imageUrl = null;
  if (currentImage?.image?.data) {
    if (currentImage.image.data.startsWith('http')) {
      imageUrl = currentImage.image.data;
    } else {
      imageUrl = `data:${currentImage.image.contentType || 'image/jpeg'};base64,${currentImage.image.data}`;
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
