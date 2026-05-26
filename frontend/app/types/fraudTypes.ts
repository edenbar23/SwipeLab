// Fraud detection warning returned inline with classification submit response
export interface ClassificationWarning {
  level: 'WARNING_1' | 'WARNING_2';
  message: string;
  strikeCount: number;
  strikesUntilBan: number;
}

// Admin notification record
export interface AdminNotification {
  id: number;
  type: 'SUSPICIOUS_ACTIVITY' | 'USER_WARNED' | 'USER_BANNED' | 'USER_RECOVERED' | 'SYSTEM_ALERT';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  title: string;
  message: string;
  targetUsername: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface AdminNotificationPage {
  content: AdminNotification[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
