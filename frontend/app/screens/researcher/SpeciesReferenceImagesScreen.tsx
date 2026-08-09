import React, { useState } from "react";
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  ActivityIndicator,
  FlatList,
  Image,
  Alert,
  Modal,
  Platform,
} from "react-native";
import { useRoute, useNavigation, RouteProp } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import * as ImagePicker from 'expo-image-picker';
import { Ionicons } from "@expo/vector-icons";
import ScreenHeaderLayout from "@/components/layout/ScreenHeaderLayout/ScreenHeaderLayout";
import { useThemeStore } from "@/stores/themeStore";
import { Colors } from "../../../constants/theme";
import AuthenticatedImage from "@/components/ui/AuthenticatedImage";
import { useSpeciesPoolImages, useDeleteSpeciesRefImage, useProfile } from "@/api/queries";
import { useAuthStore } from "@/stores/authStore";
import { apiFetch } from "@/api/apiFetch";
import { API_ENDPOINTS } from "@/api/apiEndpoints";
import { researcherStackParamList } from "@/navigation/researcherStack.types";


import taxonomyImg from "../../../assets/images/taxonomy.png";
import { queryClient } from "@/queryClient";

type SpeciesRefImagesRouteProp = RouteProp<researcherStackParamList, 'SpeciesReferenceImages'>;
type NavigationProp = NativeStackNavigationProp<researcherStackParamList, 'SpeciesReferenceImages'>;

const MAX_IMAGES = 10;

export default function SpeciesReferenceImagesScreen() {
  const route = useRoute<SpeciesRefImagesRouteProp>();
  const navigation = useNavigation<NavigationProp>();
  const { speciesId, speciesLabel } = route.params;

  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  const { data: poolImagesMap, isLoading } = useSpeciesPoolImages([speciesId]);
  const deleteMutation = useDeleteSpeciesRefImage();
  const { data: userProfile } = useProfile();
  const currentUsername = userProfile?.username;
  const isSuperAdmin = useAuthStore(state => state.isSuperAdmin);
  
  const [uploading, setUploading] = useState(false);
  const [previewUri, setPreviewUri] = useState<string | null>(null);

  const images = poolImagesMap?.[speciesId] ?? [];
  const atMax = images.length >= MAX_IMAGES;


  // ── Delete ─────────────────────────────────────────────────────────────────

  const handleDelete = (id: number) => {
    const message = "Are you sure you want to delete this image? It will be removed from the pool (but preserved on existing tasks).";
    
    if (Platform.OS === 'web') {
      if (window.confirm(message)) {
        deleteMutation.mutate(id, {
          onError: (err: any) => alert(err.message || "Failed to delete image")
        });
      }
      return;
    }

    Alert.alert(
      "Delete Reference Image",
      message,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: () => deleteMutation.mutate(id, {
            onError: (err: any) => Alert.alert("Error", err.message || "Failed to delete image")
          }),
        },
      ]
    );
  };

  // ── Upload ─────────────────────────────────────────────────────────────────

  const handleUpload = async () => {
    if (atMax) {
      Alert.alert('Limit reached', `Maximum ${MAX_IMAGES} reference images allowed per species in the pool.`);
      return;
    }

    if (Platform.OS === 'web') {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/jpeg,image/png,image/webp,image/gif,image/bmp';
      input.multiple = true;
      input.onchange = async (e: Event) => {
        const files = (e.target as HTMLInputElement).files;
        if (!files) return;
        await performUpload(Array.from(files).slice(0, MAX_IMAGES - images.length));
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
        selectionLimit: MAX_IMAGES - images.length,
        quality: 1,
      });
      if (!result.canceled) {
        await performUpload(result.assets);
      }
    }
  };

  const performUpload = async (files: any[]) => {
    if (files.length === 0) return;
    setUploading(true);
    try {
      const fd = new FormData();
      for (const file of files) {
        if (Platform.OS === 'web') {
          fd.append('files', file);
        } else {
          fd.append('files', {
            uri: file.uri,
            name: file.fileName || 'ref.jpg',
            type: 'image/jpeg',
          } as any);
        }
      }

      const res = await apiFetch(API_ENDPOINTS.SPECIES.REF_IMAGES(speciesId), {
        method: 'POST',
        body: fd,
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Upload failed');
      }

      queryClient.invalidateQueries({ queryKey: ['species', 'pool'] });
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setUploading(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  const isWeb = Platform.OS === 'web';
  
  const renderItem = ({ item }: { item: any }) => {
    const canDelete = isSuperAdmin || (currentUsername && item.uploadedBy === currentUsername);
    
    return (
      <View style={[
        styles.imageCard, 
        { 
          backgroundColor: themeColors.card, 
          borderColor: themeColors.border,
          maxWidth: isWeb ? '23.5%' : '48%' 
        }
      ]}>
        <TouchableOpacity 
          style={styles.imageWrap}
          onPress={() => setPreviewUri(item.imageUrl)}
        >
          <AuthenticatedImage 
            uri={item.thumbnailUrl}
            style={styles.thumbnail} 
            resizeMode="cover"
          />
          <View style={styles.zoomOverlay}>
            <Ionicons name="search" size={24} color="white" />
          </View>
        </TouchableOpacity>
        <View style={styles.cardActions}>
          <Text style={[styles.uploaderText, { color: themeColors.textSecondary }]} numberOfLines={2}>
            By: {item.uploadedBy}
          </Text>
          {canDelete && (
            <TouchableOpacity 
              style={styles.deleteBtn}
              onPress={() => handleDelete(item.id)}
              disabled={deleteMutation.isPending}
            >
              <Ionicons name="trash-outline" size={20} color="#ef4444" />
            </TouchableOpacity>
          )}
        </View>
      </View>
    );
  };

  return (
    <ScreenHeaderLayout
      leftIcon={taxonomyImg}
      leftTitle={`References: ${speciesLabel}`}
      onLeftPress={() => navigation.goBack()}
    >
      <View style={[styles.container, { backgroundColor: themeColors.background }]}>
        
        {/* Header Action */}
        <View style={[styles.actionHeader, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
          <View>
            <Text style={[styles.actionTitle, { color: themeColors.text }]}>
              Pool Images
            </Text>
            <Text style={[styles.actionSubtitle, { color: themeColors.textSecondary }]}>
              {images.length} / {MAX_IMAGES} uploaded
            </Text>
          </View>
          <TouchableOpacity 
            style={[styles.uploadBtn, (atMax || uploading) && { opacity: 0.5 }]}
            onPress={handleUpload}
            disabled={atMax || uploading}
          >
            {uploading ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <>
                <Ionicons name="cloud-upload-outline" size={18} color="#fff" />
                <Text style={styles.uploadBtnText}>Upload New</Text>
              </>
            )}
          </TouchableOpacity>
        </View>

        {/* List */}
        {isLoading ? (
          <View style={styles.centerContainer}>
            <ActivityIndicator size="large" color="#10B981" />
          </View>
        ) : images.length === 0 ? (
          <View style={styles.centerContainer}>
            <Ionicons name="images-outline" size={48} color={themeColors.textSecondary} />
            <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>
              No reference images exist for this species.
            </Text>
            <Text style={[styles.emptySubtext, { color: themeColors.textSecondary }]}>
              Upload images to build the reference pool.
            </Text>
          </View>
        ) : (
          <FlatList
            style={{ width: '100%', flex: 1 }}
            data={images}
            keyExtractor={(item) => String(item.id)}
            renderItem={renderItem}
            numColumns={isWeb ? 4 : 2}
            contentContainerStyle={styles.listContainer}
            columnWrapperStyle={styles.columnWrapper}
            showsVerticalScrollIndicator={false}
          />
        )}

        {/* Full Image Preview Modal */}
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
              <AuthenticatedImage 
                uri={previewUri}
                style={styles.previewImage} 
                resizeMode="contain" 
              />
              <TouchableOpacity 
                style={styles.closePreviewBtn}
                onPress={() => setPreviewUri(null)}
              >
                <Ionicons name="close-circle" size={32} color="#fff" />
              </TouchableOpacity>
            </TouchableOpacity>
          </Modal>
        )}

      </View>
    </ScreenHeaderLayout>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center' },
  centerContainer: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60 },
  emptyText: { marginTop: 16, fontSize: 16, fontWeight: '500' },
  emptySubtext: { marginTop: 8, fontSize: 14 },
  
  actionHeader: {
    width: '100%',
    maxWidth: 800,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    marginVertical: 16,
    borderRadius: 12,
    borderWidth: 1,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  actionTitle: { fontSize: 18, fontWeight: '700' },
  actionSubtitle: { fontSize: 14, marginTop: 2 },
  uploadBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#10B981',
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 8,
    gap: 6,
  },
  uploadBtnText: { color: '#fff', fontWeight: '600', fontSize: 14 },

  listContainer: {
    width: '100%',
    maxWidth: 800,
    alignSelf: 'center',
    paddingHorizontal: 16,
    paddingBottom: 40,
  },
  columnWrapper: {
    gap: 16,
    marginBottom: 16,
  },
  imageCard: {
    flex: 1,
    borderRadius: 12,
    borderWidth: 1,
    overflow: 'hidden',
  },
  imageWrap: {
    width: '100%',
    aspectRatio: 1,
    position: 'relative',
    backgroundColor: '#f1f5f9',
  },
  thumbnail: {
    width: '100%',
    height: '100%',
  },
  zoomOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.3)',
    alignItems: 'center',
    justifyContent: 'center',
    opacity: 0,
  },
  cardActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 8,
  },
  uploaderText: {
    fontSize: 11,
    flexShrink: 1,
    marginRight: 4,
  },
  deleteBtn: {
    padding: 4,
  },

  previewOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.92)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  previewImage: {
    width: '100%',
    height: '100%',
  },
  closePreviewBtn: {
    position: 'absolute',
    top: 40,
    right: 20,
    padding: 10,
  },
});
