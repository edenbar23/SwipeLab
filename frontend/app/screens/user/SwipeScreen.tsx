import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useQueryClient } from '@tanstack/react-query';
import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import { QUERY_KEYS, useMyTasks, useSwipeBatch } from '../../api/queries';
import ReferenceGallery from '../../components/user/ReferenceGallery';
import SwipeButtons from '../../components/user/SwipeButtons';
import SwipeCard, { SwipeCardHandle } from '../../components/user/SwipeCard';
import WarningToast from '../../components/ui/WarningToast';
import useResponsive from '../../hooks/useResponsive';
import { useSwipeStore } from '../../stores/swipeStore';
import { useThemeStore } from '../../stores/themeStore';
import { SwipeDirection } from '../../types';
import { ClassificationWarning } from '../../types/fraudTypes';


// ─── Accent used across Quick Start UI ──────────────────────────────────────
const ACCENT = '#4B7BE5';

export default function SwipeScreen() {
  const navigation = useNavigation<any>();
  const [showReference, setShowReference] = useState(false);
  const { dataBatch, currentIndex, activeTaskId, setActiveTaskId, setBatch, nextCard, clearBatch } =
    useSwipeStore();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeWarning, setActiveWarning] = useState<ClassificationWarning | null>(null);

  const { isPhone, isDesktop } = useResponsive();
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const size = isDesktop ? 200 : isPhone ? 300 : 250;

  const cardRef = useRef<SwipeCardHandle>(null);
  const queryClient = useQueryClient();

  // Assigned tasks — always fetched so Quick Start can populate immediately
  const { data: myTasks = [], isLoading: tasksLoading } = useMyTasks();

  // Swipe batch — disabled until the user picks a task
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

  const handleSwipe = (direction: SwipeDirection) => {
    const currentImage = dataBatch[currentIndex];

    // Immediately advance UI to the next card
    if (currentIndex + 1 < dataBatch.length) {
      nextCard();
      setShowReference(false);
    } else {
      fetchNextBatch();
    }

    // Process submission in background
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
      .then(async (res) => {
        if (res.ok) {
          const data = await res.json();
          // Show warning toast if the fraud detection system issued one
          if (data?.warning) {
            setActiveWarning(data.warning as ClassificationWarning);
          }
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.challenges });
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.myBadges });
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.userProfile });
        }
      })
      .catch((e) => {
        console.error('Submit error:', e);
      });
    }
  };

  // Keyboard shortcuts (web only, active swipe state)
  useEffect(() => {
    if (Platform.OS !== 'web' || loading || dataBatch.length === 0) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowUp':    cardRef.current?.swipeCard('dont-know'); break;
        case 'ArrowDown':  cardRef.current?.swipeCard('trash');     break;
        case 'ArrowLeft':  cardRef.current?.swipeCard('no');        break;
        case 'ArrowRight': cardRef.current?.swipeCard('yes');       break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [loading, dataBatch, currentIndex]);

  // ─── STATE A: Active task — loading batch ──────────────────────────────────
  if (activeTaskId && (loading || isQueryLoading)) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <ActivityIndicator size="large" color={ACCENT} />
        <Text style={[styles.loadingText, { color: themeColors.textSecondary }]}>
          Loading your next batch…
        </Text>
      </View>
    );
  }

  // ─── STATE A: Active task — batch exhausted or error ──────────────────────
  if (activeTaskId && (error || queryError || dataBatch.length === 0) && !isQueryLoading) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <View style={[styles.batchDoneCard, { backgroundColor: themeColors.card }]}>
          <View style={[styles.iconCircle, { backgroundColor: `${ACCENT}18` }]}>
            <Ionicons name="checkmark-circle-outline" size={48} color={ACCENT} />
          </View>
          <Text style={[styles.batchDoneTitle, { color: themeColors.text }]}>
            {error ? 'Something went wrong' : 'Batch complete!'}
          </Text>
          <Text style={[styles.batchDoneSubtitle, { color: themeColors.textSecondary }]}>
            {error || "You've finished this batch. Keep going!"}
          </Text>
          <View style={styles.batchDoneActions}>
            <TouchableOpacity
              style={[styles.primaryBtn, { backgroundColor: ACCENT }]}
              onPress={fetchNextBatch}
              activeOpacity={0.82}
            >
              <Ionicons name="refresh" size={17} color="#fff" style={{ marginRight: 6 }} />
              <Text style={styles.primaryBtnText}>Load More</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.ghostBtn, { borderColor: themeColors.border }]}
              onPress={() => setActiveTaskId(null)}
              activeOpacity={0.82}
            >
              <Text style={[styles.ghostBtnText, { color: themeColors.textSecondary }]}>
                Switch Task
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    );
  }

  // ─── STATE A: Active swipe UI ───────────────────────────────────────────────
  if (activeTaskId && dataBatch.length > 0) {
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
        imageUrl = rawImageData;
      }
    }
    const referenceImagesUrls =
      currentImage?.referenceImages?.map((ref: any) => {
        if (ref.imageUrl) {
            return ref.imageUrl;
        }
        if (ref.data?.startsWith('http')) return ref.data;
        return `data:${ref.contentType || 'image/jpeg'};base64,${ref.data}`;
      }) || [];

    return (
      <View style={[styles.container, { backgroundColor: themeColors.background }]}>
        {/* Warning toast — rendered above everything, non-blocking */}
        {activeWarning && (
          <WarningToast
            warning={activeWarning}
            onDismiss={() => setActiveWarning(null)}
          />
        )}

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
              onSwipe={(direction) => cardRef.current?.swipeCard(direction)}
              onToggleReference={() => setShowReference(!showReference)}
              showReference={showReference}
            />
          )}
        </View>
      </View>
    );
  }

  // ─── STATES B & C: No active task ──────────────────────────────────────────
  // Show a loading skeleton while my tasks are being fetched on first mount
  if (tasksLoading) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <ActivityIndicator size="large" color={ACCENT} />
      </View>
    );
  }

  // ─── STATE C: True empty — user has no assigned tasks ──────────────────────
  if (myTasks.length === 0) {
    return (
      <View style={[styles.container, styles.centerElements, { backgroundColor: themeColors.background }]}>
        <View style={[styles.emptyCard, { backgroundColor: themeColors.card }]}>
          <View style={[styles.iconCircle, { backgroundColor: `${ACCENT}18` }]}>
            <Ionicons name="telescope-outline" size={48} color={ACCENT} />
          </View>
          <Text style={[styles.emptyTitle, { color: themeColors.text }]}>
            No Tasks Yet
          </Text>
          <Text style={[styles.emptySubtitle, { color: themeColors.textSecondary }]}>
            You have no active tasks.{'\n'}Explore the catalog and start labeling!
          </Text>
          <TouchableOpacity
            style={[styles.primaryBtn, { backgroundColor: ACCENT, alignSelf: 'center', marginTop: 4 }]}
            onPress={() => navigation.navigate('Tasks')}
            activeOpacity={0.82}
          >
            <Ionicons name="search" size={17} color="#fff" style={{ marginRight: 6 }} />
            <Text style={styles.primaryBtnText}>Explore Tasks</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ─── STATE B: Quick Start — user has assigned tasks ────────────────────────
  return (
    <View style={[styles.container, { backgroundColor: themeColors.background }]}>
      {/* Header */}
      <View style={[styles.qsHeader, { borderBottomColor: themeColors.border }]}>
        <View style={[styles.iconCircle, { backgroundColor: `${ACCENT}18`, width: 40, height: 40, borderRadius: 20 }]}>
          <Ionicons name="flash" size={20} color={ACCENT} />
        </View>
        <View style={{ marginLeft: 12, flex: 1 }}>
          <Text style={[styles.qsTitle, { color: themeColors.text }]}>Quick Start</Text>
          <Text style={[styles.qsSubtitle, { color: themeColors.textSecondary }]}>
            Pick a task to continue labeling
          </Text>
        </View>
        <TouchableOpacity
          onPress={() => navigation.navigate('Tasks')}
          style={styles.qsBrowseBtn}
          activeOpacity={0.8}
        >
          <Text style={{ color: ACCENT, fontSize: 13, fontWeight: '600' }}>Browse All</Text>
          <Ionicons name="chevron-forward" size={14} color={ACCENT} />
        </TouchableOpacity>
      </View>

      {/* Task list */}
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={styles.qsList}
        showsVerticalScrollIndicator={false}
      >
        {myTasks.map((task: any) => {
          const totalImages = task.progress?.totalImages ?? task.totalImages ?? 0;
          const classified = task.progress?.imagesClassified ?? task.imagesClassified ?? 0;
          const pct = totalImages > 0 ? Math.round((classified / totalImages) * 100) : 0;

          return (
            <TouchableOpacity
              key={task.taskId}
              style={[styles.qsTaskRow, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}
              onPress={() => setActiveTaskId(task.taskId)}
              activeOpacity={0.86}
            >
              {/* Left icon */}
              <View style={[styles.qsTaskIcon, { backgroundColor: `${ACCENT}18` }]}>
                <Ionicons name="images-outline" size={22} color={ACCENT} />
              </View>

              {/* Info block */}
              <View style={styles.qsTaskInfo}>
                <Text style={[styles.qsTaskName, { color: themeColors.text }]} numberOfLines={1}>
                  {task.name}
                </Text>
                {/* Progress bar */}
                <View style={[styles.qsProgressBg, { backgroundColor: themeColors.border }]}>
                  <View style={[styles.qsProgressFill, { width: `${pct}%` }]} />
                </View>
                <Text style={[styles.qsProgressLabel, { color: themeColors.textSecondary }]}>
                  {classified.toLocaleString()} labeled · {pct}%
                </Text>
              </View>

              {/* CTA */}
              <View style={[styles.qsStartBtn, { backgroundColor: ACCENT }]}>
                <Ionicons name="play" size={14} color="#fff" />
              </View>
            </TouchableOpacity>
          );
        })}

        {/* Bottom spacer so last card clears the BottomBar */}
        <View style={{ height: 24 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  centerElements: {
    justifyContent: 'center',
  },
  loadingText: {
    marginTop: 14,
    fontSize: 14,
  },

  // ─── Shared card shell ────────────────────────────────────────────────────
  emptyCard: {
    width: '85%',
    maxWidth: 360,
    borderRadius: 24,
    padding: 32,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.07,
    shadowRadius: 20,
    elevation: 6,
  },
  batchDoneCard: {
    width: '85%',
    maxWidth: 360,
    borderRadius: 24,
    padding: 32,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.07,
    shadowRadius: 20,
    elevation: 6,
  },
  iconCircle: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 20,
  },

  // ─── Empty state ─────────────────────────────────────────────────────────
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

  // ─── Batch done state ─────────────────────────────────────────────────────
  batchDoneTitle: {
    fontSize: 21,
    fontWeight: '700',
    marginBottom: 8,
    textAlign: 'center',
  },
  batchDoneSubtitle: {
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 21,
    marginBottom: 24,
  },
  batchDoneActions: {
    width: '100%',
    gap: 10,
  },

  // ─── Shared buttons ───────────────────────────────────────────────────────
  primaryBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 13,
    paddingHorizontal: 28,
    borderRadius: 14,
  },
  primaryBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
  ghostBtn: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    borderRadius: 14,
    borderWidth: 1,
  },
  ghostBtnText: {
    fontSize: 14,
    fontWeight: '600',
  },

  // ─── Quick Start header ───────────────────────────────────────────────────
  qsHeader: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
  },
  qsTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  qsSubtitle: {
    fontSize: 13,
    marginTop: 2,
  },
  qsBrowseBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },

  // ─── Quick Start task rows ────────────────────────────────────────────────
  qsList: {
    padding: 16,
    width: '100%',
    maxWidth: 560,   // comfortable reading width on web
    alignSelf: 'center',
  },
  qsTaskRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 16,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    shadowColor: '#000',
    shadowOpacity: 0.04,
    shadowRadius: 8,
    elevation: 2,
  },
  qsTaskIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  qsTaskInfo: {
    flex: 1,
    marginRight: 12,
  },
  qsTaskName: {
    fontSize: 15,
    fontWeight: '600',
    marginBottom: 6,
  },
  qsProgressBg: {
    height: 4,
    borderRadius: 2,
    overflow: 'hidden',
    marginBottom: 4,
  },
  qsProgressFill: {
    height: '100%',
    backgroundColor: ACCENT,
    borderRadius: 2,
  },
  qsProgressLabel: {
    fontSize: 11,
    fontWeight: '500',
  },
  qsStartBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // ─── Active Swipe UI ──────────────────────────────────────────────────────
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
