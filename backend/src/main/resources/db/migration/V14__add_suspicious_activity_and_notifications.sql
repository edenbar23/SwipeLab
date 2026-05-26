-- ============================================================
-- V14 – Suspicious Activity Tracking & Admin Notifications
-- Issue: Malicious Labeling Rework
-- ============================================================

-- ── 1. Suspicious activity audit log ─────────────────────────
-- Stores every STRIKE / WARNING / BAN event emitted by
-- FraudDetectionService. Never physically deleted — audit only.
CREATE TABLE IF NOT EXISTS suspicious_activity_records (
    id               BIGSERIAL    PRIMARY KEY,
    username         VARCHAR(255) NOT NULL,
    reason           VARCHAR(512) NOT NULL,
    response_time_ms BIGINT,
    task_id          BIGINT,
    severity         VARCHAR(50)  NOT NULL,   -- matches WarningLevel enum
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sar_username   ON suspicious_activity_records(username);
CREATE INDEX IF NOT EXISTS idx_sar_severity   ON suspicious_activity_records(severity);
CREATE INDEX IF NOT EXISTS idx_sar_created_at ON suspicious_activity_records(created_at);

-- ── 2. Admin notifications ────────────────────────────────────
-- One row per admin-visible event (warning / ban / recovery).
-- is_read is toggled by the admin via the dashboard.
CREATE TABLE IF NOT EXISTS admin_notifications (
    id              BIGSERIAL    PRIMARY KEY,
    type            VARCHAR(50)  NOT NULL,    -- matches NotificationType enum
    severity        VARCHAR(20)  NOT NULL,    -- matches NotificationSeverity enum
    title           VARCHAR(255) NOT NULL,
    message         TEXT         NOT NULL,
    target_username VARCHAR(255),
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_an_is_read        ON admin_notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_an_type           ON admin_notifications(type);
CREATE INDEX IF NOT EXISTS idx_an_target_username ON admin_notifications(target_username);
CREATE INDEX IF NOT EXISTS idx_an_created_at     ON admin_notifications(created_at);

-- ── 3. User entity — new suspicious-activity columns ─────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS warning_count            INTEGER   NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS strike_count             INTEGER   NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_warning_at          TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS consecutive_correct_golds INTEGER  NOT NULL DEFAULT 0;

-- WARNED is stored as a plain VARCHAR via @Enumerated(STRING),
-- so no ALTER TYPE is required for the status column.
