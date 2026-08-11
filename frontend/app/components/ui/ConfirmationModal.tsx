import React from 'react';
import { Modal, View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useThemeStore } from '@/stores/themeStore';
import { Colors } from '../../../constants/theme';
import { Ionicons } from '@expo/vector-icons';

interface ConfirmationModalProps {
  visible: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmationModal({
  visible,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
}: ConfirmationModalProps) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  if (!visible) return null;

  return (
    <Modal transparent visible={visible} animationType="fade" onRequestClose={onCancel}>
      <View style={styles.overlay}>
        <View style={[styles.card, { backgroundColor: isDark ? themeColors.card : '#FFF', borderColor: isDark ? themeColors.border : '#E5E7EB' }]}>
          
          <View style={styles.header}>
            <Ionicons name="warning-outline" size={24} color="#EF4444" style={{ marginRight: 8 }} />
            <Text style={[styles.title, { color: themeColors.text }]}>{title}</Text>
          </View>
          
          <Text style={[styles.message, { color: themeColors.textSecondary }]}>
            {message}
          </Text>
          
          <View style={styles.actions}>
            <TouchableOpacity style={[styles.btn, styles.cancelBtn, { borderColor: isDark ? themeColors.border : '#D1D5DB' }]} onPress={onCancel}>
              <Text style={[styles.cancelBtnText, { color: themeColors.text }]}>{cancelText}</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={[styles.btn, styles.confirmBtn]} onPress={onConfirm}>
              <Text style={styles.confirmBtnText}>{confirmText}</Text>
            </TouchableOpacity>
          </View>

        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  card: {
    width: '100%',
    maxWidth: 400,
    borderRadius: 16,
    borderWidth: 1,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 10,
    elevation: 8,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  message: {
    fontSize: 15,
    lineHeight: 22,
    marginBottom: 24,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
  },
  btn: {
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancelBtn: {
    backgroundColor: 'transparent',
    borderWidth: 1,
  },
  confirmBtn: {
    backgroundColor: '#EF4444',
  },
  cancelBtnText: {
    fontSize: 15,
    fontWeight: '600',
  },
  confirmBtnText: {
    fontSize: 15,
    fontWeight: '600',
    color: '#FFF',
  },
});
