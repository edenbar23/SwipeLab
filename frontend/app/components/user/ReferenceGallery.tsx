import { Ionicons } from '@expo/vector-icons';
import React, { useState } from 'react';
import {
  Dimensions,
  Image,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { PinchGestureHandler } from 'react-native-gesture-handler';
import Animated, { useAnimatedStyle, useSharedValue } from 'react-native-reanimated';

interface ReferenceGalleryProps {
  images: string[];
  onClose: () => void;
}

export default function ReferenceGallery({ images, onClose }: ReferenceGalleryProps) {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const scale = useSharedValue(1);
  const screenWidth = Dimensions.get('window').width;
  const isDesktop = Platform.OS === 'web' && screenWidth > 500;

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const onPinchEvent = (event: any) => {
    scale.value = event.nativeEvent.scale;
  };

  const displayedImages = images.length > 0 ? images : [];

  // width for horizontal scroll images
  const imageWidth = isDesktop ? 140 : Math.min(screenWidth / 2 - 20, 150);

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.closeButton} onPress={onClose}>
        <Ionicons name="images-outline" size={20} color="#374151" />
        <Text style={styles.closeText}>Hide References</Text>
      </TouchableOpacity>

      {displayedImages.length > 0 ? (
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={true}
          style={[styles.scrollContainer, isDesktop && { maxWidth: 300 }]}
          contentContainerStyle={{ paddingHorizontal: 8 }}
        >
          {displayedImages.map((img, index) => (
            <TouchableOpacity
              key={index}
              onPress={() => setSelectedImage(img)}
              style={{ marginRight: index !== displayedImages.length - 1 ? 12 : 0 }}
            >
              <View style={[styles.imageContainer, { width: imageWidth }]}>
                <Image source={{ uri: img }} style={styles.image} resizeMode="cover" />
              </View>
            </TouchableOpacity>
          ))}
        </ScrollView>
      ) : (
        <Text style={{ textAlign: 'center', color: '#555' }}>No reference images available</Text>
      )}

      {/* Full-screen modal */}
      <Modal visible={!!selectedImage} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <TouchableOpacity
            style={styles.closeModalButton}
            onPress={() => {
              scale.value = 1;
              setSelectedImage(null);
            }}
          >
            <Ionicons name="close-circle-outline" size={36} color="#FFF" />
          </TouchableOpacity>

          {selectedImage && (
            <PinchGestureHandler onGestureEvent={onPinchEvent}>
              <View
                style={{
                  width: isDesktop ? 400 : '90%',
                  height: isDesktop ? 400 : '70%',
                  justifyContent: 'center',
                  alignItems: 'center',
                }}
              >
                <Animated.Image
                  key={selectedImage} // ensures re-render on change
                  source={{ uri: selectedImage }}
                  style={[{ width: '100%', height: '100%', borderRadius: 16 }, animatedStyle]}
                  resizeMode="contain"
                />
              </View>
            </PinchGestureHandler>
          )}
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {},
  closeButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F3F4F6',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 25,
    marginBottom: 12,
    gap: 8,
  },
  closeText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
  },
  scrollContainer: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  imageContainer: {
    aspectRatio: 1,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#E5E7EB',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.95)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  closeModalButton: {
    position: 'absolute',
    top: 40,
    right: 20,
    zIndex: 10,
  },
});
