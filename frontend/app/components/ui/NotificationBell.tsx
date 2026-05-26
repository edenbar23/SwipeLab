import React, { useRef, useState } from 'react';
import {
  Animated,
  FlatList,
  Modal,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import {
  useAdminNotifications,
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotificationUnreadCount,
} from '../../hooks/useAdminNotifications';
import { AdminNotification } from '../../types/fraudTypes';

const SEVERITY_COLORS: Record<string, string> = {
  INFO: '#3b82f6',
  WARNING: '#f59e0b',
  CRITICAL: '#ef4444',
};

/**
 * Bell icon with unread badge count. Tapping opens a dropdown panel
 * listing recent notifications with severity colour coding.
 *
 * Intended to be placed in the researcher / super-admin toolbar header.
 */
export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const { data: countData } = useNotificationUnreadCount();
  const unreadCount = countData?.unreadCount ?? 0;

  const { data: page, isLoading } = useAdminNotifications(
    open ? { page: 0, size: 15 } : undefined
  );
  const markRead = useMarkNotificationRead();
  const markAll = useMarkAllNotificationsRead();

  const onBellPress = () => {
    // Brief scale pulse for tactile feedback
    Animated.sequence([
      Animated.timing(scaleAnim, { toValue: 1.2, duration: 100, useNativeDriver: true }),
      Animated.timing(scaleAnim, { toValue: 1, duration: 100, useNativeDriver: true }),
    ]).start();
    setOpen(true);
  };

  const handleMarkRead = (id: number) => {
    markRead.mutate(id);
  };

  const handleMarkAllRead = () => {
    markAll.mutate();
  };

  const severityIcon = (severity: AdminNotification['severity']): any => {
    if (severity === 'CRITICAL') return 'alert-circle';
    if (severity === 'WARNING') return 'warning';
    return 'information-circle';
  };

  const renderItem = ({ item }: { item: AdminNotification }) => {
    const color = SEVERITY_COLORS[item.severity] ?? '#6b7280';
    return (
      <TouchableOpacity
        style={[styles.notifRow, !item.isRead && styles.notifRowUnread]}
        onPress={() => handleMarkRead(item.id)}
        accessibilityLabel={`Notification: ${item.title}`}
        activeOpacity={0.75}
      >
        <Ionicons name={severityIcon(item.severity)} size={20} color={color} style={styles.notifIcon} />
        <View style={styles.notifText}>
          <Text style={styles.notifTitle} numberOfLines={1}>{item.title}</Text>
          <Text style={styles.notifMsg} numberOfLines={2}>{item.message}</Text>
          {item.targetUsername && (
            <Text style={styles.notifUser}>@{item.targetUsername}</Text>
          )}
        </View>
        {!item.isRead && <View style={[styles.unreadDot, { backgroundColor: color }]} />}
      </TouchableOpacity>
    );
  };

  return (
    <>
      {/* Bell button */}
      <TouchableOpacity
        onPress={onBellPress}
        style={styles.bellButton}
        accessibilityLabel={`Notifications, ${unreadCount} unread`}
        accessibilityRole="button"
        activeOpacity={0.75}
      >
        <Animated.View style={{ transform: [{ scale: scaleAnim }] }}>
          <Ionicons name="notifications" size={24} color="#fff" />
          {unreadCount > 0 && (
            <View style={styles.badge} accessibilityLabel={`${unreadCount} unread notifications`}>
              <Text style={styles.badgeText}>{unreadCount > 99 ? '99+' : unreadCount}</Text>
            </View>
          )}
        </Animated.View>
      </TouchableOpacity>

      {/* Dropdown panel */}
      <Modal
        visible={open}
        transparent
        animationType="fade"
        onRequestClose={() => setOpen(false)}
        accessibilityViewIsModal
      >
        <TouchableOpacity
          style={styles.backdrop}
          onPress={() => setOpen(false)}
          activeOpacity={1}
          accessibilityLabel="Close notifications"
        />
        <View style={styles.panel}>
          {/* Panel header */}
          <View style={styles.panelHeader}>
            <Text style={styles.panelTitle}>🔔 Notifications</Text>
            <TouchableOpacity
              onPress={handleMarkAllRead}
              disabled={markAll.isPending || unreadCount === 0}
              style={styles.markAllBtn}
            >
              <Text style={[styles.markAllText, unreadCount === 0 && { opacity: 0.4 }]}>
                Mark all read
              </Text>
            </TouchableOpacity>
          </View>

          {/* List */}
          {isLoading ? (
            <Text style={styles.emptyText}>Loading…</Text>
          ) : !page?.content?.length ? (
            <Text style={styles.emptyText}>No notifications yet.</Text>
          ) : (
            <FlatList
              data={page.content}
              keyExtractor={(item) => String(item.id)}
              renderItem={renderItem}
              style={styles.list}
              showsVerticalScrollIndicator={false}
            />
          )}
        </View>
      </Modal>
    </>
  );
}

const IS_WEB = Platform.OS === 'web';

const styles = StyleSheet.create({
  bellButton: {
    padding: 6,
    position: 'relative',
  },
  badge: {
    position: 'absolute',
    top: -4,
    right: -4,
    backgroundColor: '#ef4444',
    borderRadius: 8,
    minWidth: 16,
    height: 16,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 3,
  },
  badgeText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '700',
  },

  // ── Modal overlay ─────────────────────────────────────────────────────────
  backdrop: {
    flex: 1,
  },
  panel: {
    position: 'absolute',
    top: IS_WEB ? 56 : 80,
    right: 12,
    width: IS_WEB ? 360 : 300,
    maxHeight: 480,
    backgroundColor: '#1e2533',
    borderRadius: 14,
    shadowColor: '#000',
    shadowOpacity: 0.4,
    shadowRadius: 20,
    elevation: 12,
    overflow: 'hidden',
  },
  panelHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#2d3748',
  },
  panelTitle: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 15,
  },
  markAllBtn: {
    paddingVertical: 4,
    paddingHorizontal: 8,
  },
  markAllText: {
    color: '#60a5fa',
    fontSize: 12,
    fontWeight: '600',
  },
  list: {
    flexGrow: 0,
  },
  emptyText: {
    color: '#9ca3af',
    textAlign: 'center',
    padding: 24,
    fontSize: 13,
  },

  // ── Notification row ──────────────────────────────────────────────────────
  notifRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#2d3748',
  },
  notifRowUnread: {
    backgroundColor: '#1a2236',
  },
  notifIcon: {
    marginRight: 10,
    marginTop: 2,
  },
  notifText: {
    flex: 1,
  },
  notifTitle: {
    color: '#f9fafb',
    fontWeight: '600',
    fontSize: 13,
    marginBottom: 2,
  },
  notifMsg: {
    color: '#9ca3af',
    fontSize: 12,
    lineHeight: 17,
  },
  notifUser: {
    color: '#60a5fa',
    fontSize: 11,
    marginTop: 3,
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginLeft: 8,
    marginTop: 6,
  },
});
