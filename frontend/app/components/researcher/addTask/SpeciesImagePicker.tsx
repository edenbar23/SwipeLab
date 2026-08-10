import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useQueryClient } from '@tanstack/react-query';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '@/stores/themeStore';
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { apiFetch } from '@/api/apiFetch';
import { SpeciesRefImage } from '@/components/researcher/addTask/addTaskTypes';
import AuthenticatedImage from '@/components/ui/AuthenticatedImage';

const MAX_IMAGES = 3;
const MIN_IMAGES = 1;
const THUMB_SIZE = 80;

interface PoolImage {
  id: number;
  thumbnailUrl: string;
  imageUrl: string;
  caption?: string;
}

interface SpeciesImagePickerProps {
  speciesId: string;
  speciesLabel: string;
  selectedImages: SpeciesRefImage[];
  poolImages: PoolImage[];
  poolLoading?: boolean;
  onImagesChange: (speciesId: string, images: SpeciesRefImage[]) => void;
}

export default function SpeciesImagePicker({
  speciesId,
  speciesLabel,
  selectedImages,
  poolImages,
  poolLoading = false,
  onImagesChange,
}: SpeciesImagePickerProps) {
  const { theme } = useThemeStore();
  const c = Colors[theme as keyof typeof Colors];
  const queryClient = useQueryClient();
  const [modalVisible, setModalVisible] = useState(false);
  const [previewUri, setPreviewUri]     = useState<string | null>(null);
  const [uploading, setUploading]       = useState(false);

  const isAtMax  = selectedImages.length >= MAX_IMAGES;
  const isValid  = selectedImages.length >= MIN_IMAGES;

  // ── Helpers ────────────────────────────────────────────────────────────────

  const isPoolImageSelected = (poolId: number) =>
    selectedImages.some((img) => img.fromPool && img.poolId === poolId);

  // ── Toggle a pool image (select / deselect) ────────────────────────────────

  const togglePoolImage = (pool: PoolImage) => {
    if (isPoolImageSelected(pool.id)) {
      onImagesChange(speciesId, selectedImages.filter((img) => img.poolId !== pool.id));
    } else {
      if (isAtMax) {
        Alert.alert('Limit reached', `Maximum ${MAX_IMAGES} reference images per species.`);
        return;
      }
      onImagesChange(speciesId, [
        ...selectedImages,
        { poolId: pool.id, uri: pool.thumbnailUrl, fromPool: true, caption: pool.caption },
      ]);
    }
  };

  // ── Upload new image(s) from device ────────────────────────────────────────
  // Images are uploaded to the species pool *immediately* (not deferred to task
  // submit). This makes the new image instantly selectable from the pool — and
  // we invalidate the cached pool query so the grid refreshes without a reload.

  /**
   * Uploads picked files to the species pool, adds the returned pool images as
   * selected (fromPool) entries, and refreshes the pool cache.
   */
  const uploadToPool = async (
    files: { file?: File; uri?: string; name?: string }[],
  ) => {
    if (files.length === 0) return;
    const remaining = MAX_IMAGES - selectedImages.length;
    const toUpload = files.slice(0, remaining);

    setUploading(true);
    try {
      const fd = new FormData();
      toUpload.forEach((f) => {
        if (Platform.OS === 'web') {
          if (f.file) fd.append('files', f.file);
        } else {
          fd.append('files', {
            uri: f.uri,
            name: f.name || 'ref.jpg',
            type: 'image/jpeg',
          } as any);
        }
      });

      const res = await apiFetch(API_ENDPOINTS.SPECIES.REF_IMAGES(speciesId), {
        method: 'POST',
        body: fd,
      });
      if (!res.ok) throw new Error(`Upload failed (${res.status})`);

      const saved: { id: number; thumbnailUrl: string; imageUrl: string; caption?: string }[] =
        await res.json();

      // Add the freshly-uploaded images as selected pool entries.
      const newSelected: SpeciesRefImage[] = saved.map((s) => ({
        poolId: s.id,
        uri: s.thumbnailUrl,
        fromPool: true,
        caption: s.caption,
      }));
      onImagesChange(speciesId, [...selectedImages, ...newSelected]);

      // Make the new image appear in the pool grid right away.
      // 1) Optimistically merge it into every cached pool query that includes
      //    this species, so it shows instantly without waiting for a network round-trip.
      queryClient.setQueriesData<Record<string, PoolImage[]>>(
        { queryKey: ['species', 'pool'] },
        (old) => {
          if (!old) return old;
          const existing = old[speciesId] ?? [];
          const merged = [
            ...existing,
            ...saved
              .filter((s) => !existing.some((p) => p.id === s.id))
              .map((s) => ({
                id: s.id,
                thumbnailUrl: s.thumbnailUrl,
                imageUrl: s.imageUrl,
                caption: s.caption,
              })),
          ];
          return { ...old, [speciesId]: merged };
        },
      );
      // 2) Force an immediate refetch (not lazy invalidation) so the cache is
      //    reconciled with the server right away — fixes the stale-cache delay.
      queryClient.refetchQueries({ queryKey: ['species', 'pool'] });
    } catch (err) {
      console.error('Reference image upload failed:', err);
      Alert.alert('Upload failed', 'Could not upload the image. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const handleUpload = async () => {
    if (isAtMax) {
      Alert.alert('Limit reached', `Maximum ${MAX_IMAGES} reference images per species.`);
      return;
    }
    if (uploading) return;

    if (Platform.OS === 'web') {
      // Web: invisible file input
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/jpeg,image/png,image/webp,image/gif,image/bmp';
      input.multiple = true;
      input.onchange = (e: Event) => {
        const files = (e.target as HTMLInputElement).files;
        if (!files) return;
        void uploadToPool(Array.from(files).map((file) => ({ file, name: file.name })));
      };
      input.click();
    } else {
      const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission required', 'Camera roll access is needed to pick images.');
        return;
      }
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsMultipleSelection: true,
        selectionLimit: MAX_IMAGES - selectedImages.length,
        quality: 1, // Backend does compression; send full quality
      });
      if (!result.canceled) {
        await uploadToPool(
          result.assets.map((asset) => ({ uri: asset.uri, name: asset.fileName ?? undefined })),
        );
      }
    }
  };

  // ── Remove a locally-selected image ───────────────────────────────────────

  const removeImage = (index: number) => {
    const updated = selectedImages.filter((_, i) => i !== index);
    onImagesChange(speciesId, updated);
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <View style={[styles.container, { borderColor: c.border }]}>
      {/* Header row */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Text style={[styles.label, { color: c.text }]}>
            Reference Images
          </Text>
          <Text style={[styles.count, { color: isValid ? '#10B981' : '#f59e0b' }]}>
            {selectedImages.length}/{MAX_IMAGES}
            {!isValid && '  ⚠ Required'}
          </Text>
        </View>
        <TouchableOpacity
          style={[styles.addBtn, isAtMax && styles.addBtnDisabled]}
          onPress={() => setModalVisible(true)}
          disabled={isAtMax}
        >
          <Ionicons name="images-outline" size={14} color={isAtMax ? '#94a3b8' : '#10B981'} />
          <Text style={[styles.addBtnText, { color: isAtMax ? '#94a3b8' : '#10B981' }]}>
            {poolImages.length > 0 ? 'From pool / Upload' : 'Upload'}
          </Text>
        </TouchableOpacity>
      </View>

      {/* Selected thumbnails row */}
      {selectedImages.length > 0 && (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.thumbRow}>
          {selectedImages.map((img, idx) => (
            <TouchableOpacity
              key={idx}
              style={styles.thumbWrap}
              onPress={() => setPreviewUri(img.fromPool ? img.uri : img.uri)}
              onLongPress={() => removeImage(idx)}
            >
              {img.fromPool ? (
                <AuthenticatedImage uri={img.uri} style={styles.thumb} />
              ) : (
                <Image source={{ uri: img.uri }} style={styles.thumb} />
              )}
              <TouchableOpacity style={styles.removeBtn} onPress={() => removeImage(idx)}>
                <Ionicons name="close-circle" size={18} color="#ef4444" />
              </TouchableOpacity>
              {img.fromPool && (
                <View style={styles.poolBadge}>
                  <Ionicons name="checkmark" size={10} color="#fff" />
                </View>
              )}
            </TouchableOpacity>
          ))}
        </ScrollView>
      )}

      {/* Pool picker modal */}
      <Modal
        visible={modalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalSheet, { backgroundColor: c.card, borderColor: c.border }]}>
            {/* Modal header */}
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: c.text }]}>{speciesLabel}</Text>
              <TouchableOpacity onPress={() => setModalVisible(false)}>
                <Ionicons name="close" size={22} color={c.textSecondary} />
              </TouchableOpacity>
            </View>

            <ScrollView showsVerticalScrollIndicator={false}>
              {/* Pool section */}
              {poolLoading ? (
                <ActivityIndicator color="#10B981" style={{ marginVertical: 20 }} />
              ) : poolImages.length > 0 ? (
                <View>
                  <Text style={[styles.sectionTitle, { color: c.textSecondary }]}>
                    EXISTING POOL IMAGES
                  </Text>
                  <View style={styles.poolGrid}>
                    {poolImages.map((pool) => {
                      const selected = isPoolImageSelected(pool.id);
                      return (
                        <TouchableOpacity
                          key={pool.id}
                          style={[styles.poolThumbWrap, selected && styles.poolThumbSelected]}
                          onPress={() => togglePoolImage(pool)}
                          onLongPress={() => setPreviewUri(pool.imageUrl)}
                        >
                          <AuthenticatedImage
                            uri={pool.thumbnailUrl}
                            style={styles.poolThumb}
                          />
                          {selected && (
                            <View style={styles.selectedOverlay}>
                              <Ionicons name="checkmark-circle" size={22} color="#10B981" />
                            </View>
                          )}
                        </TouchableOpacity>
                      );
                    })}
                  </View>
                  <Text style={[styles.hint, { color: c.textSecondary }]}>
                    Long-press a thumbnail to preview full image. Tap to select/deselect.
                  </Text>
                </View>
              ) : (
                <Text style={[styles.emptyPool, { color: c.textSecondary }]}>
                  No images in pool yet.
                </Text>
              )}

              {/* Upload new */}
              <Text style={[styles.sectionTitle, { color: c.textSecondary, marginTop: 16 }]}>
                UPLOAD NEW
              </Text>
              <TouchableOpacity
                style={[styles.uploadRow, { borderColor: c.border }, (isAtMax || uploading) && styles.uploadRowDisabled]}
                onPress={handleUpload}
                disabled={isAtMax || uploading}
              >
                {uploading ? (
                  <ActivityIndicator size="small" color="#10B981" />
                ) : (
                  <Ionicons name="cloud-upload-outline" size={20} color={isAtMax ? '#94a3b8' : '#10B981'} />
                )}
                <Text style={[styles.uploadText, { color: isAtMax ? '#94a3b8' : c.text }]}>
                  {uploading
                    ? 'Uploading…'
                    : isAtMax
                    ? `Limit reached (${MAX_IMAGES} max)`
                    : `Choose image${MAX_IMAGES - selectedImages.length > 1 ? 's' : ''} from device`}
                </Text>
              </TouchableOpacity>
              <Text style={[styles.hint, { color: c.textSecondary }]}>
                JPEG · PNG · WEBP · GIF · BMP — max 5 MB each. Images are compressed server-side.
              </Text>
            </ScrollView>

            <TouchableOpacity
              style={styles.doneBtn}
              onPress={() => setModalVisible(false)}
            >
              <Text style={styles.doneBtnText}>Done</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Full-image preview modal */}
      {previewUri && (
        <Modal
          visible={!!previewUri}
          transparent
          animationType="fade"
          onRequestClose={() => setPreviewUri(null)}
        >
          <TouchableOpacity
            style={styles.previewOverlay}
            activeOpacity={1}
            onPress={() => setPreviewUri(null)}
          >
            <AuthenticatedImage uri={previewUri} style={styles.previewImage} resizeMode="contain" />
            <Text style={styles.previewHint}>Tap anywhere to close</Text>
          </TouchableOpacity>
        </Modal>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    marginTop: 8,
    marginBottom: 4,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  label: { fontSize: 13, fontWeight: '600' },
  count: { fontSize: 12, fontWeight: '600' },
  addBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#10B981',
  },
  addBtnDisabled: { borderColor: '#94a3b8' },
  addBtnText: { fontSize: 12, fontWeight: '600' },
  thumbRow: { flexDirection: 'row', marginTop: 10 },
  thumbWrap: { marginRight: 8, position: 'relative' },
  thumb: { width: THUMB_SIZE, height: THUMB_SIZE, borderRadius: 8 },
  removeBtn: { position: 'absolute', top: -6, right: -6 },
  poolBadge: {
    position: 'absolute',
    bottom: 4,
    left: 4,
    backgroundColor: '#10B981',
    borderRadius: 10,
    width: 16,
    height: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Modal
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.55)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    borderTopWidth: 1,
    padding: 20,
    maxHeight: '80%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  modalTitle: { fontSize: 17, fontWeight: '700' },
  sectionTitle: { fontSize: 11, fontWeight: '700', letterSpacing: 0.8, marginBottom: 10 },
  poolGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginBottom: 8 },
  poolThumbWrap: {
    position: 'relative',
    borderRadius: 10,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  poolThumbSelected: { borderColor: '#10B981' },
  poolThumb: { width: 90, height: 90, borderRadius: 8 },
  selectedOverlay: {
    position: 'absolute',
    top: 4,
    right: 4,
    backgroundColor: 'rgba(255,255,255,0.85)',
    borderRadius: 12,
  },
  emptyPool: { fontSize: 13, textAlign: 'center', marginVertical: 12 },
  hint: { fontSize: 11, marginBottom: 12 },
  uploadRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: 10,
    padding: 14,
    marginBottom: 8,
  },
  uploadRowDisabled: { opacity: 0.5 },
  uploadText: { fontSize: 14 },
  doneBtn: {
    marginTop: 16,
    backgroundColor: '#10B981',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  doneBtnText: { color: '#fff', fontWeight: '700', fontSize: 16 },

  // Preview
  previewOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.9)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  previewImage: { width: '95%', height: '80%' },
  previewHint: { color: 'rgba(255,255,255,0.5)', marginTop: 12, fontSize: 13 },
});
