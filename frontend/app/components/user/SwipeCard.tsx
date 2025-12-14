import React, { forwardRef, useImperativeHandle, useRef } from 'react';
import {
  Animated,
  Dimensions,
  Image,
  PanResponder,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SwipeDirection } from '../../types';

const SCREEN_WIDTH = Dimensions.get('window').width;
const SWIPE_THRESHOLD = 120;

interface SwipeCardProps {
  question: string;
  imageUrl: string | null;
  onSwipe: (direction: SwipeDirection) => void;
}

export interface SwipeCardHandle {
  swipeCard: (direction: SwipeDirection) => void;
}

const SwipeCard = forwardRef<SwipeCardHandle, SwipeCardProps>(
  ({ question, imageUrl, onSwipe }, ref) => {
    const position = useRef(new Animated.ValueXY()).current;

    const rotate = position.x.interpolate({
      inputRange: [-SCREEN_WIDTH / 2, 0, SCREEN_WIDTH / 2],
      outputRange: ['-10deg', '0deg', '10deg'],
      extrapolate: 'clamp',
    });

    const panResponder = useRef(
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onPanResponderMove: (_, gesture) => {
          position.setValue({ x: gesture.dx, y: gesture.dy });
        },
        onPanResponderRelease: (_, gesture) => {
          if (Math.abs(gesture.dx) > Math.abs(gesture.dy)) {
            if (gesture.dx > SWIPE_THRESHOLD) forceSwipe('yes');
            else if (gesture.dx < -SWIPE_THRESHOLD) forceSwipe('no');
            else resetPosition();
          } else {
            if (gesture.dy < -SWIPE_THRESHOLD) forceSwipe('dont-know');
            else if (gesture.dy > SWIPE_THRESHOLD) forceSwipe('trash');
            else resetPosition();
          }
        },
      })
    ).current;

    const forceSwipe = (direction: SwipeDirection) => {
      const x = direction === 'yes' ? SCREEN_WIDTH : direction === 'no' ? -SCREEN_WIDTH : 0;
      const y = direction === 'dont-know' ? -SCREEN_WIDTH : direction === 'trash' ? SCREEN_WIDTH : 0;

      Animated.timing(position, {
        toValue: { x, y },
        duration: 250,
        useNativeDriver: false,
      }).start(() => {
        position.setValue({ x: 0, y: 0 });
        onSwipe(direction);
      });
    };

    const resetPosition = () => {
      Animated.spring(position, {
        toValue: { x: 0, y: 0 },
        useNativeDriver: false,
      }).start();
    };

    useImperativeHandle(ref, () => ({
      swipeCard: forceSwipe,
    }));

    const getCardStyle = () => ({
      ...styles.card,
      transform: [
        { translateX: position.x },
        { translateY: position.y },
        { rotate },
      ],
    });

    return (
      <View style={styles.container}>
        <Text style={styles.question}>{question}</Text>
        <Animated.View style={getCardStyle()} {...panResponder.panHandlers}>
          <View style={styles.imageContainer}>
            {imageUrl ? (
              <Image source={{ uri: imageUrl }} style={styles.image} />
            ) : (
              <View style={styles.placeholder}>
                <Text style={styles.placeholderText}>📷</Text>
              </View>
            )}
          </View>
        </Animated.View>
      </View>
    );
  }
);

// ✅ Add display name to fix ESLint warning
SwipeCard.displayName = 'SwipeCard';

export default SwipeCard;

const styles = StyleSheet.create({
  container: { width: '100%' },
  question: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#1F2937',
    textAlign: 'center',
    marginBottom: 12,
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
  },
  imageContainer: {
    aspectRatio: 1,
    width: '100%',
    borderRadius: 16,
    overflow: 'hidden',
    backgroundColor: '#E5E7EB',
  },
  image: { width: '100%', height: '100%' },
  placeholder: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  placeholderText: { fontSize: 60 },
});
