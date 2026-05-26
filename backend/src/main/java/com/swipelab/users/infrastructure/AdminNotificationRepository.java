package com.swipelab.users.infrastructure;

import com.swipelab.users.domain.AdminNotification;
import com.swipelab.users.domain.NotificationSeverity;
import com.swipelab.users.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    /** Paginated list for the admin dashboard, filterable by read state. */
    Page<AdminNotification> findByIsReadOrderByCreatedAtDesc(Boolean isRead, Pageable pageable);

    /** All notifications paginated — no read filter. */
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Paginated by type. */
    Page<AdminNotification> findByTypeOrderByCreatedAtDesc(NotificationType type, Pageable pageable);

    /** Paginated by severity. */
    Page<AdminNotification> findBySeverityOrderByCreatedAtDesc(NotificationSeverity severity, Pageable pageable);

    /** Badge counter — only unread notifications. */
    long countByIsRead(Boolean isRead);

    /** Mark all as read in a single UPDATE to avoid N+1 queries. */
    @Modifying
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    int markAllAsRead();
}
