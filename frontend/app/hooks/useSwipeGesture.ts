import { useRef } from 'react';
import { Animated, Dimensions, PanResponder } from 'react-native';
import { SwipeDirection } from '../types';

const SCREEN_WIDTH = Dimensions.get('window').width;
const SWIPE_THRESHOLD = 120;

export const useSwipeGesture = (onSwipe: (direction: SwipeDirection) => void) => {
  const position = useRef(new Animated.ValueXY()).current;

  const rotate = position.x.interpolate({
    inputRange: [-SCREEN_WIDTH / 2, 0, SCREEN_WIDTH / 2],
    outputRange: ['-10deg', '0deg', '10deg'],
    extrapolate: 'clamp',
  });

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

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onPanResponderMove: (_, gesture) => {
        position.setValue({ x: gesture.dx, y: gesture.dy });
      },
      onPanResponderRelease: (_, gesture) => {
        if (Math.abs(gesture.dx) > Math.abs(gesture.dy)) {
          if (gesture.dx > SWIPE_THRESHOLD) {
            forceSwipe('yes');
          } else if (gesture.dx < -SWIPE_THRESHOLD) {
            forceSwipe('no');
          } else {
            resetPosition();
          }
        } else {
          if (gesture.dy < -SWIPE_THRESHOLD) {
            forceSwipe('dont-know');
          } else if (gesture.dy > SWIPE_THRESHOLD) {
            forceSwipe('trash');
          } else {
            resetPosition();
          }
        }
      },
    })
  ).current;

  return {
    position,
    rotate,
    panResponder,
    forceSwipe,
  };
};