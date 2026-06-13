-- ─── USERS TABLE ─────────────────────────────────────────────────────────────
CREATE TABLE users (
    username VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(255),
    profile_image_url VARCHAR(500),

    -- Authentication
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    refresh_token_hash VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    verification_token_expiry TIMESTAMP,
    reset_password_token VARCHAR(255),
    reset_token_expiry TIMESTAMP,

    -- Account Status
    active BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_login TIMESTAMP,

    -- Role & Status (enum stored as VARCHAR)
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',

    -- Credibility & Stats
    credibility_score DOUBLE PRECISION DEFAULT 50.0,
    agreement_with_experts DOUBLE PRECISION DEFAULT 0.0,
    majority_agreement_score DOUBLE PRECISION DEFAULT 0.0,
    total_classifications INTEGER DEFAULT 0,
    correct_gold_classifications INTEGER DEFAULT 0,
    total_gold_classifications INTEGER DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_id ON users(provider_id);

-- ─── LABELS TABLE (Species/Categories) ───────────────────────────────────────
CREATE TABLE labels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    common_name VARCHAR(255),
    description TEXT
);

-- ─── TASKS TABLE ─────────────────────────────────────────────────────────────
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    -- Both a technical and display name (V8 originally added title, included here for clean slate)
    name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled',
    description TEXT,
    query_species VARCHAR(255),
    question VARCHAR(500),
    deadline TIMESTAMP,
    min_classifications_per_image INTEGER DEFAULT 3,
    consensus_threshold DOUBLE PRECISION DEFAULT 80.0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    source_system VARCHAR(50) NOT NULL DEFAULT 'INTERNAL',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES users(username)
);

-- ─── IMAGES TABLE ────────────────────────────────────────────────────────────
-- image_url is nullable: internal crops use src_path (base64) instead of URLs.
CREATE TABLE images (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(500),           -- nullable; external images only
    src_path TEXT,                    -- base64 or local path for internal crops
    thumbnail_url VARCHAR(500),
    caption TEXT,
    parent_image_id BIGINT,
    external_box_id BIGINT,           -- FK to Stardbi source (unique per task)
    experiment_id BIGINT,
    priority INTEGER DEFAULT 0,
    task_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_image_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT uk_external_box_task UNIQUE (external_box_id, task_id)
);

-- ─── CLASSIFICATIONS TABLE ───────────────────────────────────────────────────
-- Redesigned from original: uses username + enum response instead of user_id + label FK.
CREATE TABLE classifications (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    user_role VARCHAR(50),
    task_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    query_species VARCHAR(255),
    user_response VARCHAR(50) NOT NULL,   -- YES | NO | DONT_KNOW | TRASH
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_classification_image FOREIGN KEY (image_id) REFERENCES images(id)
);

-- ─── TASK ASSOCIATION TABLES ─────────────────────────────────────────────────
CREATE TABLE task_experiments (
    task_id BIGINT NOT NULL,
    experiment_id BIGINT,
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);

CREATE TABLE task_recipient_groups (
    task_id BIGINT NOT NULL,
    recipient_group_id BIGINT,
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);

CREATE TABLE task_target_species (
    task_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, label_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (label_id) REFERENCES labels(id)
);

-- ─── GAMIFICATION TABLE ──────────────────────────────────────────────────────
-- One row per user. yes_tag_count added by V11.
CREATE TABLE gamification (
    username VARCHAR(255) PRIMARY KEY,
    start_streak TIMESTAMP,
    end_streak TIMESTAMP,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    score BIGINT NOT NULL DEFAULT 0,
    badge TEXT,                           -- comma-separated badge names cache
    rank_level VARCHAR(50) DEFAULT 'UNRANKED'
);

-- ─── RECIPIENT USERS TABLE ───────────────────────────────────────────────────
CREATE TABLE recipient_users (
    username VARCHAR(255) PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ─── RECIPIENT GROUPS TABLE ──────────────────────────────────────────────────
CREATE TABLE recipient_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_recipient_group UNIQUE (created_by, name)
);

-- ─── RECIPIENT GROUP ↔ USERS JOIN TABLE ──────────────────────────────────────
CREATE TABLE recipient_group_users (
    group_id BIGINT       NOT NULL REFERENCES recipient_groups(id) ON DELETE CASCADE,
    username VARCHAR(255) NOT NULL REFERENCES recipient_users(username) ON DELETE CASCADE,
    PRIMARY KEY (group_id, username)
);
