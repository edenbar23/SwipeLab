-- V7: Add Gamification Challenges & Badges Schema

-- 1. badge_definition: Reusable badge templates
CREATE TABLE badge_definition (
    id UUID PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. challenge_definition: Templates for distinct challenges
CREATE TABLE challenge_definition (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    metric_type VARCHAR(50) NOT NULL,
    aggregation_type VARCHAR(50) NOT NULL,
    target_value INTEGER NOT NULL,
    time_window_type VARCHAR(50) NOT NULL,
    badge_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    available_from TIMESTAMP,
    available_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_challenge_badge FOREIGN KEY (badge_id) REFERENCES badge_definition(id)
);

CREATE INDEX idx_challenge_def_active ON challenge_definition(active);
CREATE INDEX idx_challenge_def_available ON challenge_definition(available_from, available_until);

-- 3. user_challenge: Challenge instance per user per window
CREATE TABLE user_challenge (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    challenge_definition_id UUID NOT NULL,
    current_progress INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_challenge_user FOREIGN KEY (username) REFERENCES users(username),
    CONSTRAINT fk_user_challenge_def FOREIGN KEY (challenge_definition_id) REFERENCES challenge_definition(id),
    CONSTRAINT uq_user_challenge_window UNIQUE (username, challenge_definition_id, window_start)
);

CREATE INDEX idx_user_challenge_username ON user_challenge(username);
CREATE INDEX idx_user_challenge_def_id ON user_challenge(challenge_definition_id);
CREATE INDEX idx_user_challenge_window_start ON user_challenge(window_start);

-- 4. user_challenge_distinct: Tracking distinct values for a challenge
CREATE TABLE user_challenge_distinct (
    user_challenge_id UUID NOT NULL,
    distinct_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_challenge_id, distinct_value),
    CONSTRAINT fk_distinct_challenge FOREIGN KEY (user_challenge_id) REFERENCES user_challenge(id) ON DELETE CASCADE
);

-- 5. badge_award: Actual badge earnings history
CREATE TABLE badge_award (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    badge_id UUID NOT NULL,
    challenge_definition_id UUID NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    awarded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_award_user FOREIGN KEY (username) REFERENCES users(username),
    CONSTRAINT fk_award_badge FOREIGN KEY (badge_id) REFERENCES badge_definition(id),
    CONSTRAINT fk_award_challenge FOREIGN KEY (challenge_definition_id) REFERENCES challenge_definition(id),
    CONSTRAINT uq_user_award_window UNIQUE (username, challenge_definition_id, window_start)
);

CREATE INDEX idx_badge_award_username ON badge_award(username);
CREATE INDEX idx_badge_award_badge_id ON badge_award(badge_id);
CREATE INDEX idx_badge_award_window_start ON badge_award(window_start);

-- Optional Clean Up of V2 Legacy Entities if we don't want to maintain them
-- Uncomment if V2 badges logic should be discarded entirely.
-- DROP TABLE IF EXISTS user_badges;
-- DROP TABLE IF EXISTS badges;
