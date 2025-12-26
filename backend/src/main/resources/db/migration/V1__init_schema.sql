-- USERS TABLE
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
    
    -- Credibility & Stats
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    credibility_score DOUBLE PRECISION DEFAULT 0.0,
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

-- LABELS TABLE (Species/Categories)
CREATE TABLE labels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    common_name VARCHAR(255),
    description TEXT
);

-- TASKS TABLE
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    deadline TIMESTAMP,
    min_classifications_per_image INTEGER DEFAULT 3,
    consensus_threshold DOUBLE PRECISION DEFAULT 80.0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES users(username)
);

-- IMAGES TABLE
CREATE TABLE images (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    caption TEXT,
    experiment_id BIGINT,
    priority INTEGER DEFAULT 0,
    is_gold_standard BOOLEAN NOT NULL DEFAULT FALSE,
    correct_label_id BIGINT,
    task_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_image_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_image_correct_label FOREIGN KEY (correct_label_id) REFERENCES labels(id)
);

-- CLASSIFICATIONS TABLE
CREATE TABLE classifications (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    image_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_classification_user FOREIGN KEY (user_id) REFERENCES users(username),
    CONSTRAINT fk_classification_image FOREIGN KEY (image_id) REFERENCES images(id),
    CONSTRAINT fk_classification_label FOREIGN KEY (label_id) REFERENCES labels(id)
);

-- ASSOCIATION TABLES FOR TASKS
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
