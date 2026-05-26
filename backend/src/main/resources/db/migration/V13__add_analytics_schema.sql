-- V13__add_analytics_schema.sql
-- Creates the analytics star-schema tables used by the analytics module.
-- All tables use CREATE TABLE IF NOT EXISTS so this migration is idempotent
-- and safe to run against databases where Hibernate ddl-auto:update already
-- bootstrapped the schema (e.g. dev/mock environments).

-- ─── classification_facts ────────────────────────────────────────────────────
-- Denormalised event log; one row per classification event.
-- Populated asynchronously by AnalyticsEventListener on every ClassificationSubmittedEvent.
CREATE TABLE IF NOT EXISTS classification_facts (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    classification_id   BIGINT,
    task_id             BIGINT       NOT NULL,
    image_id            BIGINT       NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    species             VARCHAR(255),
    is_correct          BOOLEAN,
    is_expert           BOOLEAN,
    credibility_at_time DOUBLE PRECISION,
    response_time_ms    BIGINT,
    consensus_score     DOUBLE PRECISION,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    "day"                 DATE         NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cf_task_id ON classification_facts (task_id);
CREATE INDEX IF NOT EXISTS idx_cf_user_id ON classification_facts (user_id);
CREATE INDEX IF NOT EXISTS idx_cf_day     ON classification_facts ("day");

-- ─── user_daily_stats ────────────────────────────────────────────────────────
-- Upserted on every classification; one row per (user, day).
CREATE TABLE IF NOT EXISTS user_daily_stats (
    id       BIGSERIAL    PRIMARY KEY,
    user_id  VARCHAR(255) NOT NULL,
    "day"      DATE         NOT NULL,
    total    INTEGER      NOT NULL DEFAULT 0,
    correct  INTEGER      NOT NULL DEFAULT 0,
    accuracy DOUBLE PRECISION      DEFAULT 0.0,
    CONSTRAINT uq_uds_user_day UNIQUE (user_id, "day")
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_uds_user_day ON user_daily_stats (user_id, "day");

-- ─── task_daily_stats ────────────────────────────────────────────────────────
-- Upserted on every classification; one row per (task, day).
CREATE TABLE IF NOT EXISTS task_daily_stats (
    id                BIGSERIAL PRIMARY KEY,
    task_id           BIGINT    NOT NULL,
    "day"               DATE      NOT NULL,
    classifications   INTEGER   NOT NULL DEFAULT 0,
    completed_images  INTEGER   NOT NULL DEFAULT 0,
    consensus_reached INTEGER   NOT NULL DEFAULT 0,
    CONSTRAINT uq_tds_task_day UNIQUE (task_id, "day")
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tds_task_day ON task_daily_stats (task_id, "day");

-- ─── task_species_stats ──────────────────────────────────────────────────────
-- Cumulative per (task, species) counters for the task analytics endpoint.
CREATE TABLE IF NOT EXISTS task_species_stats (
    id                   BIGSERIAL    PRIMARY KEY,
    task_id              BIGINT       NOT NULL,
    species              VARCHAR(255) NOT NULL,
    classification_count INTEGER      NOT NULL DEFAULT 0,
    agreement_rate       DOUBLE PRECISION      DEFAULT 0.0,
    true_positive        INTEGER      NOT NULL DEFAULT 0,
    false_positive       INTEGER      NOT NULL DEFAULT 0,
    false_negative       INTEGER      NOT NULL DEFAULT 0,
    true_negative        INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_tss_task_species UNIQUE (task_id, species)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tss_task_species ON task_species_stats (task_id, species);

-- ─── user_ranking ────────────────────────────────────────────────────────────
-- Updated by a future scheduled job; one row per (period, user).
-- Periods: DAILY, WEEKLY, MONTHLY, ALL_TIME
CREATE TABLE IF NOT EXISTS user_ranking (
    id         BIGSERIAL    PRIMARY KEY,
    period     VARCHAR(50)  NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    rank_val   INTEGER,
    accuracy   DOUBLE PRECISION,
    percentile INTEGER,
    CONSTRAINT uq_ur_period_user UNIQUE (period, user_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ur_period_user ON user_ranking (period, user_id);
