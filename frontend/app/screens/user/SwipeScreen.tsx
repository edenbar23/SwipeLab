import React, { useEffect, useRef, useState } from 'react';
import { Platform, StyleSheet, View } from 'react-native';
import ReferenceGallery from '../../components/user/ReferenceGallery';
import SwipeButtons from '../../components/user/SwipeButtons';
import SwipeCard, { SwipeCardHandle } from '../../components/user/SwipeCard';
import useResponsive from '../../hooks/useResponsive';
import { SwipeDirection } from '../../types';

export default function SwipeScreen() {
  const [showReference, setShowReference] = useState(false);
  const [currentQuestion] = useState('Is this a ...');
  const { isPhone, isDesktop } = useResponsive();
  const size = isDesktop ? 200 : isPhone ? 300 : 250;

  const cardRef = useRef<SwipeCardHandle>(null);

  const handleSwipe = (direction: SwipeDirection) => {
    console.log(`Swiped: ${direction}`);
    // TODO: handle swipe and load next question
  };

  // Keyboard arrow handling (Web only)
  useEffect(() => {
    if (Platform.OS !== 'web') return;

    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowUp':
          cardRef.current?.swipeCard('dont-know');
          handleSwipe('dont-know');
          break;
        case 'ArrowDown':
          cardRef.current?.swipeCard('trash');
          handleSwipe('trash');
          break;
        case 'ArrowLeft':
          cardRef.current?.swipeCard('no');
          handleSwipe('no');
          break;
        case 'ArrowRight':
          cardRef.current?.swipeCard('yes');
          handleSwipe('yes');
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <View style={styles.container}>
      <View style={[styles.cardSection, { maxWidth: size }]}>
        <SwipeCard
          ref={cardRef}
          question={currentQuestion}
          imageUrl={null}
          onSwipe={handleSwipe}
        />
      </View>

      <View style={styles.buttonSection}>
        {showReference ? (
          <ReferenceGallery
            images={['https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
    'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
    'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
    'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
 

            ]}
            onClose={() => setShowReference(false)}
          />
        ) : (
          <SwipeButtons
            onSwipe={(direction) => {
              cardRef.current?.swipeCard(direction);
              handleSwipe(direction);
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
    backgroundColor: '#daddffff',
    paddingHorizontal: 16,
    paddingTop: 8,
    alignItems: 'center',
  },
  cardSection: {
    width: '100%',
    marginBottom: 16,
  },
  buttonSection: {
    width: '100%',
    maxWidth: 300,
  },
});
