CREATE TABLE user_expert_agreements (
    id BIGSERIAL PRIMARY KEY,
    user_username VARCHAR(255) NOT NULL REFERENCES users(username),
    expert_username VARCHAR(255) NOT NULL REFERENCES users(username),
    cohen_kappa DOUBLE PRECISION NOT NULL,
    total_compared INTEGER NOT NULL,
    last_calculated TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_expert UNIQUE (user_username, expert_username)
);

-- Store Fleiss' Kappa scores (multi-user agreement per image)
CREATE TABLE image_consensus (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL REFERENCES images(id),
    fleiss_kappa DOUBLE PRECISION,
    num_raters INTEGER NOT NULL,
    last_calculated TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_image_consensus UNIQUE (image_id)
);

-- Store individual user's Fleiss scores (aggregated across all images)
ALTER TABLE users ADD COLUMN fleiss_kappa_avg DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE users ADD COLUMN cohen_kappa_avg DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE users ADD COLUMN total_classifications INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN credibility_last_updated TIMESTAMP;

CREATE INDEX idx_user_expert_agreements_user ON user_expert_agreements(user_username);
CREATE INDEX idx_image_consensus_image ON image_consensus(image_id);