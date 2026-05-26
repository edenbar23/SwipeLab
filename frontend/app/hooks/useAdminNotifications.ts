import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '../api/apiFetch';
import { API_ENDPOINTS } from '../api/apiEndpoints';
import { AdminNotification, AdminNotificationPage } from '../types/fraudTypes';

const ADMIN_NOTIF_KEYS = {
  all: ['admin', 'notifications'] as const,
  unreadCount: ['admin', 'notifications', 'unread-count'] as const,
};

// ── Unread badge count ────────────────────────────────────────────────────────

/** Polls every 60 s so the bell badge stays fresh without hammering the server. */
export const useNotificationUnreadCount = () => {
  return useQuery<{ unreadCount: number }>({
    queryKey: ADMIN_NOTIF_KEYS.unreadCount,
    queryFn: async () => {
      const res = await apiFetch(API_ENDPOINTS.ADMIN.NOTIFICATIONS_UNREAD_COUNT);
      if (!res.ok) throw new Error('Failed to fetch unread count');
      return res.json();
    },
    refetchInterval: 60_000, // 60 s
    staleTime: 30_000,
  });
};

// ── Notification list ─────────────────────────────────────────────────────────

export const useAdminNotifications = (params?: {
  isRead?: boolean;
  type?: string;
  severity?: string;
  page?: number;
  size?: number;
}) => {
  const query = new URLSearchParams();
  if (params?.isRead !== undefined) query.set('isRead', String(params.isRead));
  if (params?.type) query.set('type', params.type);
  if (params?.severity) query.set('severity', params.severity);
  query.set('page', String(params?.page ?? 0));
  query.set('size', String(params?.size ?? 20));

  return useQuery<AdminNotificationPage>({
    queryKey: [...ADMIN_NOTIF_KEYS.all, params],
    queryFn: async () => {
      const res = await apiFetch(`${API_ENDPOINTS.ADMIN.NOTIFICATIONS}?${query.toString()}`);
      if (!res.ok) throw new Error('Failed to fetch notifications');
      return res.json();
    },
    staleTime: 30_000,
  });
};

// ── Mark as read ─────────────────────────────────────────────────────────────

export const useMarkNotificationRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      const res = await apiFetch(API_ENDPOINTS.ADMIN.NOTIFICATION_READ(id), { method: 'PATCH' });
      if (!res.ok) throw new Error('Failed to mark as read');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_NOTIF_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ADMIN_NOTIF_KEYS.unreadCount });
    },
  });
};

// ── Mark all as read ─────────────────────────────────────────────────────────

export const useMarkAllNotificationsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const res = await apiFetch(API_ENDPOINTS.ADMIN.NOTIFICATIONS_READ_ALL, { method: 'PATCH' });
      if (!res.ok) throw new Error('Failed to mark all as read');
      return res.json() as Promise<{ updated: number }>;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_NOTIF_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ADMIN_NOTIF_KEYS.unreadCount });
    },
  });
};
