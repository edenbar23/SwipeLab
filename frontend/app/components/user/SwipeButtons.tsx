import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SwipeDirection } from '../../types';

interface SwipeButtonsProps {
  onSwipe: (direction: SwipeDirection) => void;
  onToggleReference: () => void;
  showReference: boolean;
}

export default function SwipeButtons({
  onSwipe,
  onToggleReference,
  showReference,
}: SwipeButtonsProps) {
  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.referenceButton}
        onPress={onToggleReference}
      >
        <Ionicons name="images-outline" size={20} color="#374151" />
        <Text style={styles.referenceText}>
          {showReference ? 'Hide References' : 'Show References'}
        </Text>
      </TouchableOpacity>

      <View style={styles.buttonsGrid}>
        <TouchableOpacity
          style={[styles.button, styles.buttonSmall, styles.buttonDontKnow]}
          onPress={() => onSwipe('dont-know')}
        >
          <Ionicons name="chevron-up" size={24} color="#FFFFFF" />
        </TouchableOpacity>
      </View>

      <View style={styles.buttonRow}>
        <TouchableOpacity
          style={[styles.button, styles.buttonNo]}
          onPress={() => onSwipe('no')}
        >
          <Ionicons name="chevron-back" size={32} color="#FFFFFF" />
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.button, styles.buttonYes]}
          onPress={() => onSwipe('yes')}
        >
          <Ionicons name="chevron-forward" size={32} color="#FFFFFF" />
        </TouchableOpacity>
      </View>

      <View style={styles.buttonsGrid}>
        <TouchableOpacity
          style={[styles.button, styles.buttonSmall, styles.buttonTrash]}
          onPress={() => onSwipe('trash')}
        >
          <Ionicons name="chevron-down" size={24} color="#FFFFFF" />
        </TouchableOpacity>
      </View>

      <Text style={styles.helpText}>
        Swipe or tap buttons to answer
      </Text>
      <Text style={styles.helpText}>
        ← No | Yes → | ↑ Don&apos;t Know | ↓ Trash
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },
  referenceButton: {
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
  referenceText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
  },
  buttonsGrid: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 16,
    marginBottom: 8,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 16,
  },
  button: {
    borderRadius: 50,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 4,
  },
  buttonNo: {
    width: 60,
    height: 60,
    backgroundColor: '#EF4444',
  },
  buttonYes: {
    width: 60,
    height: 60,
    backgroundColor: '#10B981',
  },
  buttonSmall: {
    width: 30,
    height: 30,
  },
  buttonDontKnow: {
    backgroundColor: '#FBBF24',
  },
  buttonTrash: {
    backgroundColor: '#9CA3AF',
  },
  helpText: {
    textAlign: 'center',
    fontSize: 12,
    color: '#6B7280',
    marginTop: 4,
  },
});