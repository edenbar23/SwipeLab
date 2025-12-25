-- BADGES TABLE
CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(500),
    criteria_json TEXT, -- JSON string defining how to earn it
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- USER BADGES (Junction)
CREATE TABLE user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    badge_id BIGINT NOT NULL,
    awarded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_badge_user FOREIGN KEY (user_id) REFERENCES users(username),
    CONSTRAINT fk_user_badge_badge FOREIGN KEY (badge_id) REFERENCES badges(id),
    UNIQUE(user_id, badge_id)
);

-- LEADERBOARDS (Snapshot/Cache Table)
-- In a real system this might be a Redis cache or a View, but for now a table is fine
CREATE TABLE leaderboards (
    id BIGSERIAL PRIMARY KEY,
    period VARCHAR(50) NOT NULL, -- 'WEEKLY', 'MONTHLY', 'ALL_TIME'
    user_id VARCHAR(255) NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    rank INTEGER NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_leaderboard_user FOREIGN KEY (user_id) REFERENCES users(username)
);

CREATE INDEX idx_leaderboard_period_rank ON leaderboards(period, rank);
